"""Tests for the Forge game log adapter module."""

from __future__ import annotations

from app.forge_log.events import (
    CombatBlock,
    CombatDeclareAttackers,
    DamageEvent,
    Discard,
    DrawCard,
    GameOutcome,
    LandPlayed,
    LifeChange,
    MulliganEvent,
    PhaseChanged,
    SpellCast,
    TurnBegan,
    ZoneChange,
)
from app.forge_log.parser import parse_line, parse_lines
from app.forge_log.state import GameSessionState
from app.forge_log import ForgeLogAdapter


# ── Parser tests ─────────────────────────────────────────────────────────────


class TestParser:
    def test_mulligan_kept(self):
        ev = parse_line("Mulligan: Rogist has kept a hand of 7 cards")
        assert isinstance(ev, MulliganEvent)
        assert ev.player == "Rogist"
        assert ev.kept is True
        assert ev.hand_size == 7

    def test_mulligan_down(self):
        ev = parse_line("Mulligan: Atlin has mulliganed down to 6 cards.")
        assert isinstance(ev, MulliganEvent)
        assert ev.player == "Atlin"
        assert ev.kept is False
        assert ev.hand_size == 6

    def test_turn_began(self):
        ev = parse_line("Turn: Turn 7 (Atlin)")
        assert isinstance(ev, TurnBegan)
        assert ev.turn_number == 7
        assert ev.player == "Atlin"

    def test_phase_changed_with_player(self):
        ev = parse_line("Phase: Atlin's Declare Attackers Step")
        assert isinstance(ev, PhaseChanged)
        assert ev.player == "Atlin"
        assert ev.phase == "Declare Attackers Step"

    def test_phase_changed_generic(self):
        ev = parse_line("Phase: Cleanup step")
        assert isinstance(ev, PhaseChanged)
        assert ev.phase == "Cleanup step"

    def test_spell_cast_with_id(self):
        ev = parse_line("Add To Stack: Atlin cast Past in Flames (26)")
        assert isinstance(ev, SpellCast)
        assert ev.player == "Atlin"
        assert ev.card == "Past in Flames"
        assert ev.card_id == 26

    def test_spell_cast_no_id(self):
        ev = parse_line("Add To Stack: Rogist cast Metallic Rebuke")
        assert isinstance(ev, SpellCast)
        assert ev.player == "Rogist"
        assert ev.card == "Metallic Rebuke"

    def test_spell_cast_with_target(self):
        ev = parse_line(
            "Add To Stack: Rogist cast Metallic Rebuke targeting "
            "[Each instant and sorcery card in your graveyard]"
        )
        assert isinstance(ev, SpellCast)
        assert ev.player == "Rogist"
        assert ev.card == "Metallic Rebuke"
        assert "Each instant" in ev.target_info

    def test_land_played(self):
        ev = parse_line("Land: Atlin played Mountain (55)")
        assert isinstance(ev, LandPlayed)
        assert ev.player == "Atlin"
        assert ev.card == "Mountain"
        assert ev.card_id == 55

    def test_life_change(self):
        ev = parse_line("Life: Life: Rogist 8 > -4")
        assert isinstance(ev, LifeChange)
        assert ev.player == "Rogist"
        assert ev.old_value == 8
        assert ev.new_value == -4

    def test_damage_combat(self):
        ev = parse_line("Damage: Goblin Token (175) deals 1 combat damage to Rogist.")
        assert isinstance(ev, DamageEvent)
        assert ev.source == "Goblin Token"
        assert ev.source_id == 175
        assert ev.amount == 1
        assert ev.target == "Rogist"
        assert ev.is_combat is True

    def test_damage_non_combat(self):
        ev = parse_line("Damage: Ral, Monsoon Mage (57) deals 1 non-combat damage to Atlin.")
        assert isinstance(ev, DamageEvent)
        assert ev.is_combat is False
        assert ev.target == "Atlin"

    def test_damage_no_qualifier(self):
        ev = parse_line("Damage: Abrade (27) deals 3 damage to Fish Token (155).")
        assert isinstance(ev, DamageEvent)
        assert ev.source == "Abrade"
        assert ev.source_id == 27
        assert ev.amount == 3
        assert ev.target == "Fish Token (155)"
        assert ev.is_combat is True  # defaults to combat when no qualifier

    def test_combat_unblocked_no_prefix(self):
        ev = parse_line("Atlin didn't block Restless Anchorage (81).")
        assert isinstance(ev, CombatBlock)
        assert ev.player == "Atlin"
        assert len(ev.unblocked) == 1
        assert "Restless Anchorage" in ev.unblocked[0]

    def test_zone_change(self):
        ev = parse_line("Zone Change: Arid Mesa (2) was put into Graveyard from Battlefield.")
        assert isinstance(ev, ZoneChange)
        assert ev.card == "Arid Mesa"
        assert ev.card_id == 2
        assert ev.to_zone == "Graveyard"
        assert ev.from_zone == "Battlefield"

    def test_discard(self):
        ev = parse_line("Discard: Atlin discards Ruby Medallion (34).")
        assert isinstance(ev, Discard)
        assert ev.player == "Atlin"
        assert ev.card == "Ruby Medallion"
        assert ev.card_id == 34

    def test_combat_attackers(self):
        ev = parse_line(
            "Combat: Atlin assigned Goblin Token (160) and Ral, Monsoon Mage (57) to attack Rogist."
        )
        assert isinstance(ev, CombatDeclareAttackers)
        assert ev.player == "Atlin"
        assert "Goblin Token" in ev.attackers
        assert "Ral, Monsoon Mage" in ev.attackers
        assert ev.target == "Rogist"

    def test_combat_blockers(self):
        ev = parse_line("Combat: Rogist assigned Arcbound Ravager (92) to block Goblin Token (159).")
        assert isinstance(ev, CombatBlock)
        assert ev.player == "Rogist"
        assert len(ev.blockers) == 1

    def test_combat_unblocked(self):
        ev = parse_line("Combat: Rogist didn't block Goblin Token (160).")
        assert isinstance(ev, CombatBlock)
        assert ev.player == "Rogist"
        assert len(ev.unblocked) == 1

    def test_draw_card(self):
        ev = parse_line("Resolve Stack: Fiery Islet (129) - Rogist draws a card.")
        assert isinstance(ev, DrawCard)
        assert ev.player == "Rogist"

    def test_game_outcome_won(self):
        ev = parse_line("Game Outcome: Atlin has won because all opponents have lost")
        assert isinstance(ev, GameOutcome)
        assert ev.player == "Atlin"
        assert ev.won is True
        assert ev.lost is False

    def test_game_outcome_lost(self):
        ev = parse_line("Game Outcome: Rogist has lost because life total reached 0")
        assert isinstance(ev, GameOutcome)
        assert ev.player == "Rogist"
        assert ev.lost is True

    def test_unrecognized_line_returns_none(self):
        assert parse_line("this is not a forge log line") is None

    def test_empty_line_returns_none(self):
        assert parse_line("") is None

    def test_blank_line_returns_none(self):
        assert parse_line("   ") is None


# ── State tracker tests ──────────────────────────────────────────────────────


class TestStateTracker:
    def test_turn_produces_checkpoint(self):
        state = GameSessionState(game_id="test", opponent_name="Atlin", ai_name="Rogist")
        events = parse_lines([
            "Turn: Turn 1 (Atlin)",
        ])
        cps = state.process(events[0])
        assert len(cps) == 1
        assert cps[0].turn == 1

    def test_opponent_spell_produces_checkpoint(self):
        state = GameSessionState(
            game_id="test",
            opponent_name="Atlin",
            ai_name="Rogist",
            turn=3,
        )
        events = parse_lines([
            "Add To Stack: Atlin cast Mountain (54)",
        ])
        cps = state.process(events[0])
        assert len(cps) == 1
        assert len(cps[0].observations) == 1
        assert cps[0].observations[0].card == "Mountain"

    def test_oppnent_land_produces_checkpoint(self):
        state = GameSessionState(
            game_id="test",
            opponent_name="Atlin",
            ai_name="Rogist",
            turn=3,
        )
        events = parse_lines([
            "Land: Atlin played Mountain (54)",
        ])
        cps = state.process(events[0])
        assert len(cps) == 1
        assert cps[0].observations[0].event == "land"
        assert "Mountain" in state.players["Atlin"].board

    def test_ai_spell_does_not_produce_checkpoint(self):
        state = GameSessionState(
            game_id="test",
            opponent_name="Atlin",
            ai_name="Rogist",
            turn=3,
        )
        events = parse_lines([
            "Add To Stack: Rogist cast Tormod's Crypt",
        ])
        cps = state.process(events[0])
        assert len(cps) == 0

    def test_life_change_updates(self):
        state = GameSessionState(
            game_id="test",
            opponent_name="Atlin",
            ai_name="Rogist",
        )
        state._ensure_player("Atlin")
        events = parse_lines([
            "Life: Life: Atlin 20 > 19",
        ])
        state.process(events[0])
        assert state.players["Atlin"].life == 19

    def test_zone_change_moves_card(self):
        state = GameSessionState(
            game_id="test",
            opponent_name="Atlin",
            ai_name="Rogist",
        )
        atlin = state._ensure_player("Atlin")
        atlin.board.append("Arid Mesa")
        events = parse_lines([
            "Zone Change: Arid Mesa (2) was put into Graveyard from Battlefield.",
        ])
        state.process(events[0])
        assert "Arid Mesa" not in atlin.board
        assert "Arid Mesa" in atlin.graveyard

    def test_discard_adds_to_graveyard(self):
        state = GameSessionState(
            game_id="test",
            opponent_name="Atlin",
            ai_name="Rogist",
        )
        events = parse_lines([
            "Discard: Atlin discards Ruby Medallion (34).",
        ])
        state.process(events[0])
        assert "Ruby Medallion" in state.players["Atlin"].graveyard

    def test_checkpoint_includes_life_totals(self):
        state = GameSessionState(
            game_id="test",
            opponent_name="Atlin",
            ai_name="Rogist",
            turn=5,
        )
        state._ensure_player("Atlin")
        state._ensure_player("Rogist")
        state.players["Atlin"].life = 17
        state.players["Rogist"].life = 19
        cp = state.snapshot()
        assert cp.life_totals["Atlin"] == 17
        assert cp.life_totals["Rogist"] == 19


# ── Full adapter tests ───────────────────────────────────────────────────────

_SAMPLE_LOG = """\
Turn: Turn 3 (Atlin)

Add To Stack: Atlin cast Mountain

Land: Atlin played Mountain (54)

Life: Life: Atlin 20 > 19

Discard: Atlin discards Ruby Medallion (34).

Zone Change: Arid Mesa (2) was put into Graveyard from Battlefield.

Turn: Turn 4 (Rogist)

Land: Rogist played Island (110)

Add To Stack: Rogist cast Tormod's Crypt

Life: Life: Rogist 20 > 19

Game Outcome: Atlin has won because all opponents have lost
"""


class TestForgeLogAdapter:
    def test_parse_returns_checkpoints(self):
        adapter = ForgeLogAdapter(game_id="test", format="Standard")
        adapter.set_opponent("Atlin")
        adapter.set_ai_player("Rogist")
        cps = adapter.parse(_SAMPLE_LOG)
        assert len(cps) >= 2

    def test_parse_identifies_turns(self):
        adapter = ForgeLogAdapter(game_id="test", format="Standard")
        adapter.set_opponent("Atlin")
        adapter.set_ai_player("Rogist")
        cps = adapter.parse(_SAMPLE_LOG)
        turns = [cp.turn for cp in cps]
        assert 3 in turns
        assert 4 in turns

    def test_parse_tracks_observations(self):
        adapter = ForgeLogAdapter(game_id="test", format="Standard")
        adapter.set_opponent("Atlin")
        adapter.set_ai_player("Rogist")
        cps = adapter.parse(_SAMPLE_LOG)
        final = cps[-1]
        cards = [o.card for o in final.observations]
        assert "Mountain" in cards

    def test_parse_tracks_graveyard(self):
        adapter = ForgeLogAdapter(game_id="test", format="Standard")
        adapter.set_opponent("Atlin")
        adapter.set_ai_player("Rogist")
        cps = adapter.parse(_SAMPLE_LOG)
        final = cps[-1]
        assert "Ruby Medallion" in final.opponent_graveyard

    def test_parse_tracks_life(self):
        adapter = ForgeLogAdapter(game_id="test", format="Standard")
        adapter.set_opponent("Atlin")
        adapter.set_ai_player("Rogist")
        cps = adapter.parse(_SAMPLE_LOG)
        final = cps[-1]
        assert final.life_totals["Atlin"] == 19
        assert final.life_totals["Rogist"] == 19

    def test_parse_events_returns_raw_events(self):
        adapter = ForgeLogAdapter()
        events = adapter.parse_events(_SAMPLE_LOG)
        assert len(events) > 0
        kinds = {ev.kind for ev in events}
        assert "turn" in kinds
        assert "spell" in kinds
