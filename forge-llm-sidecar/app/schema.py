"""Request/response models and the LangGraph state schema."""

from __future__ import annotations

from typing import TypedDict

from pydantic import BaseModel, Field

# v2 adds the piloting advice block to the response (see PilotingAdvice).
# v3 adds structured ActionScore with personality weighting.
# v4 adds board-aware advice: RoleAssessment (Who's the Beatdown), hand
# valuations, opponent-hand inference, target priorities, and richer state on
# the request (phase, available_mana).
SCHEMA_VERSION = 4


class Observation(BaseModel):
    """A single observed opponent play."""

    turn: int
    event: str  # "spell" | "land" | "permanent" | "graveyard"
    card: str
    cmc: int = 0
    colors: list[str] = Field(default_factory=list)
    types: list[str] = Field(default_factory=list)


class ActionScore(BaseModel):
    """A scored action recommendation from the LLM piloting advice."""

    action_type: str = (
        ""  # PLAY_SPELL | PLAY_LAND | ATTACK | BLOCK | ACTIVATE_ABILITY | MULLIGAN | PASS
    )
    target: str | None = None  # primary target (card name, "all_attackers", etc.)
    targets: list[str] | None = None  # secondary / multiple targets
    percentage: float = 0.0  # 0.0–100.0 — how good this action is
    reasoning: str = ""


class RecognitionRequest(BaseModel):
    # Identifies the calling client/adapter (e.g. "forge"). Any MTG client can
    # be an adapter by producing this request shape — see docs/ADAPTERS.md.
    client: str = "unknown"
    game_id: str
    format: str
    opponent_seat: int = 0
    turn: int = 0
    observations: list[Observation] = Field(default_factory=list)
    # The AI's own deck (card names). Forge knows this exactly; used to detect
    # the precise Constructed format and to identify the AI's own archetype.
    deck_cards: list[str] = Field(default_factory=list)
    # Live game state for piloting advice. All optional: clients that do not
    # send them still get archetype-level advice from the guide.
    hand: list[str] = Field(default_factory=list)
    own_board: list[str] = Field(default_factory=list)
    opponent_board: list[str] = Field(default_factory=list)
    your_graveyard: list[str] = Field(default_factory=list)
    opponent_graveyard: list[str] = Field(default_factory=list)
    life_totals: dict[str, int] = Field(default_factory=dict)
    # Current game phase (e.g. "MAIN1", "COMBAT_DECLARE_ATTACKERS"). Optional —
    # board-aware advice uses it to pick phase-appropriate actions.
    phase: str = ""
    # Mana available to the AI right now, in WUBRGC order or as a list of color
    # strings. Used to filter actions to ones the AI can actually pay for.
    available_mana: list[str] = Field(default_factory=list)
    # AI personality profile. Sent so the LLM can factor it into action scoring.
    personality: dict[str, str | int | float | bool] = Field(default_factory=dict)


class RoleAssessment(BaseModel):
    """Who's the Beatdown? Mike Flores's role-assignment doctrine.

    Role is matchup- and state-dependent and can flip mid-game; the wrong
    role assignment is a known losing pattern. The sidecar consults the
    natural role of each deck's strategy, the current board pressure, card
    advantage, and clock, and decides who should be the aggressor right now.
    """

    ai_role: str = "contested"  # "beatdown" | "control" | "contested"
    opponent_role: str = "contested"
    winning_side: str = "even"  # "ai" | "human" | "even"
    margin: float = 0.0  # 0-1, how decisively the winning side is ahead
    role_flipped: bool = False  # true when current state overrode the deck's natural role
    reasoning: str = ""  # one or two sentences citing life/board/card-advantage/clock


class HandValuation(BaseModel):
    """Per-card score for cards currently in the AI's hand."""

    card: str
    value: float = 0.0  # 0-100, higher = more valuable to keep / prioritize playing
    role: str = "filler"  # "threat" | "answer" | "ramp" | "card_draw" | "win_con" | "land" | "filler"
    reasoning: str = ""


class OpponentHandGuess(BaseModel):
    """Inferred category of card the human likely still holds, based on
    observed plays minus what's been seen, weighted by archetype confidence."""

    category: str = ""  # "counterspell" | "removal" | "wrath" | "threat" | "combo_piece" | ...
    example_cards: list[str] = Field(default_factory=list)
    probability: float = 0.0  # 0-1
    reasoning: str = ""


class TargetPriority(BaseModel):
    """Ordered target preference for a spell/ability with selectable targets.

    ``spell`` is the AI's spell whose targets we are ranking; an empty string
    means generic priority that applies when no spell-specific entry matches.
    """

    spell: str = ""
    targets: list[str] = Field(default_factory=list)
    reasoning: str = ""


class PilotingAdvice(BaseModel):
    """Advice on how the AI should play its own deck this turn."""

    own_archetype: str = "Unknown"
    guide_source: str = ""  # which guide answered: "<format>/<slug>" or "generic/<strategy>"
    recommended_play: str = ""
    reasoning: str = ""
    alternatives: list[str] = Field(default_factory=list)
    mulligan_advice: str = ""  # populated on turn 0 instead of recommended_play
    actions: list[ActionScore] = Field(
        default_factory=list
    )  # structured action recommendations with percentages
    # Board-aware additions (v4):
    role: RoleAssessment | None = None
    hand_values: list[HandValuation] = Field(default_factory=list)
    opponent_hand: list[OpponentHandGuess] = Field(default_factory=list)
    target_priorities: list[TargetPriority] = Field(default_factory=list)


class TrainingExample(BaseModel):
    """A single training data point from a log analysis checkpoint."""

    game_id: str = ""
    turn: int = 0
    format: str = ""
    observations: list[dict] = Field(default_factory=list)
    deck_cards: list[str] = Field(default_factory=list)
    hand: list[str] = Field(default_factory=list)
    own_board: list[str] = Field(default_factory=list)
    opponent_board: list[str] = Field(default_factory=list)
    your_graveyard: list[str] = Field(default_factory=list)
    opponent_graveyard: list[str] = Field(default_factory=list)
    life_totals: dict[str, int] = Field(default_factory=dict)
    # LLM response at this checkpoint
    archetype: str = ""
    confidence: float = 0.0
    reasoning: str = ""
    alternatives: list[str] = Field(default_factory=list)


class RecognitionResponse(BaseModel):
    archetype: str
    confidence: float
    reasoning: str
    alternatives: list[str] = Field(default_factory=list)
    piloting: PilotingAdvice | None = None
    schema_version: int = SCHEMA_VERSION


# ---------------------------------------------------------------------------
# LangGraph state. Kept as a superset TypedDict so the HTTP contract and the
# graph state can evolve independently.
# ---------------------------------------------------------------------------
class GraphState(TypedDict, total=False):
    # inputs
    game_id: str
    format: str
    turn: int
    observations: list[dict]
    deck_cards: list[str]
    hand: list[str]
    own_board: list[str]
    opponent_board: list[str]
    your_graveyard: list[str]
    opponent_graveyard: list[str]
    life_totals: dict[str, int]
    phase: str
    available_mana: list[str]
    personality: dict[str, str | int | float | bool]
    # resolved by the game_advisor node
    resolved_format: str | None
    candidate_archetypes: list[dict]
    own_archetype: str | None
    piloting_guide: dict | None
    # opponent-recognition outputs of the game_advisor node
    archetype: str | None
    confidence: float | None
    reasoning: str | None
    alternatives: list[str]
    # piloting outputs of the game_advisor node
    recommended_play: str | None
    play_reasoning: str | None
    play_alternatives: list[str]
    mulligan_advice: str | None
    guide_source: str | None
    actions: list[dict]
    role: dict | None
    hand_values: list[dict]
    opponent_hand: list[dict]
    target_priorities: list[dict]
