"""Default-primer synthesis when no editorial source resolves.

Uses only the deck name, format, signature cards, and colors as grounding,
plus the builder LLM's general MTG knowledge. Every output field is marked
``inferred`` with low confidence (<=0.4) so the runtime model knows it is
working from synthesized rather than sourced material.
"""

from __future__ import annotations

import datetime as dt
import logging

from app.knowledge import builder_llm

log = logging.getLogger(__name__)

_SYSTEM = (
    "You are an expert Magic: The Gathering coach writing a piloting guide "
    "for a deck archetype when no editorial primer is available. Ground every "
    "recommendation in the supplied signature cards and the named format. "
    "Never invent cards from other formats. Every text-bearing field must be "
    "marked 'inferred' with confidence <= 0.4 since you have no editorial "
    "source. Respond with a single JSON object."
)


def _prompt(archetype: str, fmt: str, signature_cards: list[str], colors: list[str]) -> str:
    sig = ", ".join(signature_cards) or "(none listed)"
    cols = "/".join(colors) or "unknown"
    return (
        f"Archetype: {archetype}\n"
        f"Format: {fmt}\n"
        f"Colors: {cols}\n"
        f"Signature cards: {sig}\n\n"
        "No editorial primer was found for this deck. Write a best-effort "
        "piloting guide using only the data above and your general knowledge "
        "of the format. Return JSON with these keys (same schema as the "
        "editorial extractor): strategy_type, overview, overview_evidence, "
        "win_conditions, win_conditions_evidence, mulligan, game_plan, "
        "key_cards, sequencing_tips, sequencing_tips_evidence, matchups, "
        "common_threats, common_threats_evidence. Mark every *_evidence "
        "entry as {\"confidence\": 0.3, \"kind\": \"inferred\", \"evidence_span\": \"\"}."
    )


def synthesize(
    archetype: str, fmt: str, signature_cards: list[str], colors: list[str]
) -> tuple[dict, dict]:
    """Return ``(raw_fields, provenance_entry)``.

    ``raw_fields`` is the LLM JSON output (no provenance/metadata yet);
    ``provenance_entry`` is the Provenance dict the orchestrator merges in.
    """
    raw = builder_llm.generate_guide_json(
        _prompt(archetype, fmt, signature_cards, colors), system=_SYSTEM
    )
    provenance = {
        "publisher": "builder_llm_default",
        "author": builder_llm.MODEL_NAME,
        "source_url": "",
        "publish_date": "",
        "fetched_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        "http_status": 0,
        "used_for_fields": ["*"],
    }
    return raw, provenance
