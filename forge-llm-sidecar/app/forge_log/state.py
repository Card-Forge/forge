"""Game state accumulator for parsed Forge log events.

Processes a stream of events and maintains a running picture of the game.
Produces `RecognitionRequest` checkpoints at key moments (opponent actions,
turn boundaries) that can be sent to the sidecar.
"""

from __future__ import annotations

from dataclasses import dataclass, field

from app.forge_log.events import (
    CombatBlock,
    CombatDeclareAttackers,
    DamageEvent,
    Discard,
    DrawCard,
    Event,
    GameOutcome,
    LandPlayed,
    LifeChange,
    MulliganEvent,
    PhaseChanged,
    SpellCast,
    StackResolve,
    TurnBegan,
    ZoneChange,
)
from app.schema import Observation, RecognitionRequest


@dataclass
class PlayerState:
    """Tracked state for a single player."""

    name: str
    life: int = 20
    hand_size: int = 7
    board: list[str] = field(default_factory=list)
    graveyard: list[str] = field(default_factory=list)
    # Opponent observations (spells cast, lands played)
    observations: list[Observation] = field(default_factory=list)
    # Cards played this game (for deck inference)
    played_cards: list[str] = field(default_factory=list)


@dataclass
class GameSessionState:
    """Accumulating state for a full game session.

    Processes events in order and can produce `RecognitionRequest` snapshots
    at any point.
    """

    game_id: str = "log-session"
    format: str = "Constructed"
    turn: int = 0
    phase: str = ""
    active_player: str = ""
    players: dict[str, PlayerState] = field(default_factory=dict)
    opponent_name: str = ""  # human player to analyze; set explicitly or inferred
    ai_name: str = ""
    finished: bool = False

    def _ensure_player(self, name: str) -> PlayerState:
        if name not in self.players:
            self.players[name] = PlayerState(name=name)
        return self.players[name]

    def process(self, event: Event) -> list[RecognitionRequest]:
        """Process a single event. Returns any new checkpoints (usually 0 or 1)."""
        match event.kind:
            case "mulligan":
                return self._on_mulligan(event)
            case "turn":
                return self._on_turn(event)
            case "phase":
                self._on_phase(event)
            case "spell":
                return self._on_spell(event)
            case "land":
                return self._on_land(event)
            case "life":
                return self._on_life(event)
            case "damage":
                self._on_damage(event)
            case "zone_change":
                self._on_zone_change(event)
            case "discard":
                self._on_discard(event)
            case "draw":
                self._on_draw(event)
            case "combat_attack":
                self._on_combat_attack(event)
            case "combat_block":
                self._on_combat_block(event)
            case "resolve":
                self._on_resolve(event)
            case "outcome":
                return self._on_outcome(event)
        return []

    # ── Event handlers ─────────────────────────────────────────────────────

    def _on_mulligan(self, event: MulliganEvent) -> list[RecognitionRequest]:
        ps = self._ensure_player(event.player)
        ps.hand_size = event.hand_size
        if event.kept:
            ps.hand_size = event.hand_size
        return []

    def _on_turn(self, event: TurnBegan) -> list[RecognitionRequest]:
        self.turn = event.turn_number
        self.active_player = event.player
        self._ensure_player(event.player)

        # If the AI player is known, infer the analyzed human player from the
        # first non-AI turn instead of blindly taking whoever started the game.
        if not self.opponent_name and event.player != self.ai_name:
            self.opponent_name = event.player

        # Turn boundary is a checkpoint
        return [self._build_checkpoint()]

    def _on_phase(self, event: PhaseChanged) -> None:
        self.phase = event.phase
        if event.player:
            self._ensure_player(event.player)

    def _on_spell(self, event: SpellCast) -> list[RecognitionRequest]:
        ps = self._ensure_player(event.player)
        ps.played_cards.append(event.card)

        if not self.opponent_name and event.player != self.ai_name:
            self.opponent_name = event.player

        # If this is the opponent, record as observation
        if event.player == self.opponent_name:
            ps.observations.append(
                Observation(
                    turn=self.turn,
                    event="spell",
                    card=event.card,
                    cmc=0,
                    colors=[],
                    types=[],
                )
            )
            return [self._build_checkpoint()]
        return []

    def _on_land(self, event: LandPlayed) -> list[RecognitionRequest]:
        ps = self._ensure_player(event.player)
        ps.played_cards.append(event.card)
        ps.board.append(event.card)

        if not self.opponent_name and event.player != self.ai_name:
            self.opponent_name = event.player

        if event.player == self.opponent_name:
            ps.observations.append(
                Observation(
                    turn=self.turn,
                    event="land",
                    card=event.card,
                    cmc=0,
                    colors=[],
                    types=["Land"],
                )
            )
            return [self._build_checkpoint()]
        return []

    def _on_life(self, event: LifeChange) -> list[RecognitionRequest]:
        ps = self._ensure_player(event.player)
        ps.life = event.new_value
        # Life reaching 0 or below is a game-ending event worth checkpointing
        if event.new_value <= 0:
            return [self._build_checkpoint()]
        return []

    def _on_damage(self, event: DamageEvent) -> None:
        # Damage to a player
        target_ps = self.players.get(event.target)
        if target_ps:
            # Life is tracked via LifeChange events; damage events are
            # supplementary.  We don't recalculate life here.
            pass

    def _on_zone_change(self, event: ZoneChange) -> None:
        if not event.card:
            return

        # Try to figure out which player this card belongs to
        # Heuristic: cards going to graveyard from battlefield were on board
        for ps in self.players.values():
            if event.from_zone == "Battlefield" and event.card in ps.board:
                ps.board.remove(event.card)
                if event.to_zone == "Graveyard":
                    ps.graveyard.append(event.card)
                break

    def _on_discard(self, event: Discard) -> None:
        ps = self._ensure_player(event.player)
        ps.graveyard.append(event.card)

    def _on_draw(self, event: DrawCard) -> None:
        ps = self._ensure_player(event.player)
        ps.hand_size += event.count

    def _on_combat_attack(self, event: CombatDeclareAttackers) -> None:
        # Track attackers
        pass

    def _on_combat_block(self, event: CombatBlock) -> None:
        # Track blockers
        pass

    def _on_resolve(self, event: StackResolve) -> None:
        # Resolution effects may modify board state
        # Already tracked via zone_change events
        pass

    def _on_outcome(self, event: GameOutcome) -> list[RecognitionRequest]:
        if event.player:
            self.finished = True
            return [self._build_checkpoint()]
        return []

    # ── Checkpoint generation ───────────────────────────────────────────────

    def _build_checkpoint(self) -> RecognitionRequest:
        """Build a RecognitionRequest from current state."""
        opp = self.players.get(self.opponent_name)
        ai = self.players.get(self.ai_name)

        life_totals = {}
        for name, ps in self.players.items():
            life_totals[name] = ps.life

        return RecognitionRequest(
            client="forge-log",
            game_id=self.game_id,
            format=self.format,
            opponent_seat=0,
            turn=self.turn,
            observations=opp.observations if opp else [],
            deck_cards=[],
            hand=[],
            own_board=ai.board if ai else [],
            opponent_board=opp.board if opp else [],
            your_graveyard=ai.graveyard if ai else [],
            opponent_graveyard=opp.graveyard if opp else [],
            life_totals=life_totals,
        )

    def snapshot(self) -> RecognitionRequest:
        """Build a checkpoint from the current state (not triggered by event)."""
        return self._build_checkpoint()
