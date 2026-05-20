"""Pydantic models for deck *piloting* guidance.

A piloting guide tells the LLM how to play a given deck archetype: how to
mulligan, what the game plan is, the win conditions, key cards, matchup notes,
and meta threats to watch for. Guides live as JSON under
``app/knowledge/piloting/`` and are validated against :class:`PilotingGuide`
both when the offline builder writes them and when the loader reads them.

Schema v2 (current) adds provenance, per-field evidence, legal-era markers,
and a deck-list hash so a guide that references a banned card stays useful but
flagged. v1 guides keep loading: the loader fills FieldEvidence with inferred
defaults and leaves provenance/legal_era empty.
"""

from __future__ import annotations

from enum import Enum
from typing import Literal, Optional

from pydantic import BaseModel, Field

PILOTING_SCHEMA_VERSION = 2


class StrategyType(str, Enum):
    """Generic strategy buckets, used to pick a fallback guide."""

    AGGRO = "aggro"
    TEMPO = "tempo"
    MIDRANGE = "midrange"
    CONTROL = "control"
    COMBO = "combo"
    RAMP = "ramp"


class EvidenceKind(str, Enum):
    """Whether a field came verbatim from a primer or was inferred."""

    EXPLICIT = "explicit"
    INFERRED = "inferred"


class FieldEvidence(BaseModel):
    """Per-field provenance + confidence.

    Attached as a sibling field on text-bearing entries (e.g. ``overview`` has
    ``overview_evidence``). The ``source_index`` points into
    :attr:`PilotingGuide.provenance`.
    """

    confidence: float = 0.5
    kind: EvidenceKind = EvidenceKind.INFERRED
    source_index: int = 0
    evidence_span: str = ""


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


class Provenance(BaseModel):
    """Where a piece of guidance came from."""

    publisher: str = ""
    author: str = ""
    source_url: str = ""
    publish_date: str = ""
    fetched_at: str = ""
    http_status: int = 0
    used_for_fields: list[str] = Field(default_factory=list)


class LegalEra(BaseModel):
    """The B&R window this guide was written against."""

    br_window_start: str = ""
    br_window_end: str = ""
    set_legality_snapshot: list[str] = Field(default_factory=list)


class StalenessFlags(BaseModel):
    """Computed at load time. Never persisted — derived from banlist events."""

    banned_cards_referenced: list[str] = Field(default_factory=list)
    unbanned_cards_missing: list[str] = Field(default_factory=list)
    format_rotation_event: bool = False
    out_of_meta: bool = False
    age_days: int = 0
    notes: list[str] = Field(default_factory=list)


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

    # v2 additions
    provenance: list[Provenance] = Field(default_factory=list)
    legal_era: LegalEra = Field(default_factory=LegalEra)
    decklist_hash: Optional[str] = None
    overview_evidence: Optional[FieldEvidence] = None
    win_conditions_evidence: list[FieldEvidence] = Field(default_factory=list)
    sequencing_tips_evidence: list[FieldEvidence] = Field(default_factory=list)
    common_threats_evidence: list[FieldEvidence] = Field(default_factory=list)

    # Computed at load time by the piloting loader; not persisted to disk.
    stale_flags: StalenessFlags = Field(default_factory=StalenessFlags)


def upgrade_v1_payload(raw: dict) -> dict:
    """Patch a v1 guide dict so it round-trips through the v2 model.

    Mutates ``raw`` in place and returns it. Idempotent — calling on a v2 guide
    leaves it alone. The loader runs this before model_validate so existing
    files keep loading without on-disk migration.
    """
    if not isinstance(raw, dict):
        return raw
    meta = raw.get("metadata") or {}
    version = meta.get("schema_version", 1) if isinstance(meta, dict) else 1
    if version >= PILOTING_SCHEMA_VERSION:
        return raw

    raw.setdefault("provenance", [])
    raw.setdefault("legal_era", {})
    raw.setdefault("decklist_hash", None)
    raw.setdefault("win_conditions_evidence", [])
    raw.setdefault("sequencing_tips_evidence", [])
    raw.setdefault("common_threats_evidence", [])
    raw.setdefault("overview_evidence", None)
    # Stamp it as v2 in the in-memory copy; we do NOT rewrite the file on disk.
    if isinstance(meta, dict):
        meta["schema_version"] = PILOTING_SCHEMA_VERSION
        raw["metadata"] = meta
    return raw
