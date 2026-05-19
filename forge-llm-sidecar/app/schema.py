"""Request/response models and the LangGraph state schema."""

from __future__ import annotations

from typing import TypedDict

from pydantic import BaseModel, Field

# v2 adds the piloting advice block to the response (see PilotingAdvice).
# v3 adds structured ActionScore with personality weighting.
SCHEMA_VERSION = 3


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
    # AI personality profile. Sent so the LLM can factor it into action scoring.
    personality: dict[str, str | int | float | bool] = Field(default_factory=dict)


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
