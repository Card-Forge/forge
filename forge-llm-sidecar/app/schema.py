"""Request/response models and the LangGraph state schema."""

from __future__ import annotations

from typing import TypedDict

from pydantic import BaseModel, Field

SCHEMA_VERSION = 1


class Observation(BaseModel):
    """A single observed opponent play."""

    turn: int
    event: str  # "spell" | "land" | "permanent" | "graveyard"
    card: str
    cmc: int = 0
    colors: list[str] = Field(default_factory=list)
    types: list[str] = Field(default_factory=list)


class RecognitionRequest(BaseModel):
    game_id: str
    format: str
    opponent_seat: int = 0
    turn: int = 0
    observations: list[Observation] = Field(default_factory=list)
    # The AI's own deck (card names). Forge knows this exactly; used to detect
    # the precise Constructed format when ``format`` is generic.
    deck_cards: list[str] = Field(default_factory=list)


class RecognitionResponse(BaseModel):
    archetype: str
    confidence: float
    reasoning: str
    alternatives: list[str] = Field(default_factory=list)
    schema_version: int = SCHEMA_VERSION


# ---------------------------------------------------------------------------
# LangGraph state. Kept as a superset TypedDict: extra keys are reserved for
# future nodes (play advisor, threat assessment, ...) so the HTTP contract and
# the graph state can evolve independently.
# ---------------------------------------------------------------------------
class GraphState(TypedDict, total=False):
    # inputs
    game_id: str
    format: str
    turn: int
    observations: list[dict]
    deck_cards: list[str]
    # resolved by the deck_recognition node
    resolved_format: str | None
    candidate_archetypes: list[dict]
    # outputs of the deck_recognition node
    archetype: str | None
    confidence: float | None
    reasoning: str | None
    alternatives: list[str]
