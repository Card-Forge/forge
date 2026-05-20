"""Pydantic models for deck *piloting* guidance.

A piloting guide tells the LLM how to play a given deck archetype: how to
mulligan, what the game plan is, the win conditions, key cards, matchup notes,
and meta threats to watch for. Guides live as JSON under
``app/knowledge/piloting/`` and are validated against :class:`PilotingGuide`
both when the offline builder writes them and when the loader reads them.
"""

from __future__ import annotations

from enum import Enum
from typing import Literal

from pydantic import BaseModel, Field

PILOTING_SCHEMA_VERSION = 1


class StrategyType(str, Enum):
    """Generic strategy buckets, used to pick a fallback guide."""

    AGGRO = "aggro"
    TEMPO = "tempo"
    MIDRANGE = "midrange"
    CONTROL = "control"
    COMBO = "combo"
    RAMP = "ramp"


class MulliganHandExample(BaseModel):
    """A worked example of a keep/mulligan decision."""

    hand: list[str] = Field(default_factory=list)
    decision: Literal["keep", "mulligan"]
    reason: str = ""


class MulliganGuidance(BaseModel):
    """When to keep an opening hand and when to ship it back."""

    keep_criteria: list[str] = Field(default_factory=list)
    mulligan_criteria: list[str] = Field(default_factory=list)
    examples: list[MulliganHandExample] = Field(default_factory=list)


class GamePlan(BaseModel):
    """Turn-phase priorities: what to do early, mid, and late game."""

    early_game: list[str] = Field(default_factory=list)
    mid_game: list[str] = Field(default_factory=list)
    late_game: list[str] = Field(default_factory=list)


class KeyCard(BaseModel):
    """A card that matters to the game plan and how to use it."""

    name: str
    role: str = ""
    notes: str = ""


class MatchupNote(BaseModel):
    """How to play against a specific opponent archetype."""

    opponent_archetype: str
    advice: str = ""
    watch_for: list[str] = Field(default_factory=list)


class GuideMetadata(BaseModel):
    """Provenance for a guide (who/what generated it, when)."""

    source: str = ""
    source_url: str = ""
    generated_at: str = ""
    model: str = ""
    schema_version: int = PILOTING_SCHEMA_VERSION


class PilotingGuide(BaseModel):
    """Full piloting guidance for one deck archetype (or generic strategy)."""

    archetype: str
    format: str = ""
    strategy_type: StrategyType
    overview: str = ""
    win_conditions: list[str] = Field(default_factory=list)
    mulligan: MulliganGuidance = Field(default_factory=MulliganGuidance)
    game_plan: GamePlan = Field(default_factory=GamePlan)
    key_cards: list[KeyCard] = Field(default_factory=list)
    sequencing_tips: list[str] = Field(default_factory=list)
    matchups: list[MatchupNote] = Field(default_factory=list)
    common_threats: list[str] = Field(default_factory=list)
    metadata: GuideMetadata = Field(default_factory=GuideMetadata)
