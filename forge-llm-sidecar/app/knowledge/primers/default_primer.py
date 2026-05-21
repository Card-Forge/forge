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
        "of the format.\n\n"
        "Return a JSON object EXACTLY matching this skeleton — keep every "
        "field at the type shown (lists stay lists, objects stay objects, "
        "strings stay strings). strategy_type must be lowercase.\n\n"
        "{\n"
        '  "strategy_type": "aggro"|"tempo"|"midrange"|"control"|"combo"|"ramp",\n'
        '  "overview": "2-4 sentence summary",\n'
        '  "overview_evidence": {"confidence": 0.3, "kind": "inferred", "evidence_span": ""},\n'
        '  "win_conditions": ["..."],\n'
        '  "win_conditions_evidence": [],\n'
        '  "mulligan": {\n'
        '    "keep_criteria": ["..."],\n'
        '    "mulligan_criteria": ["..."],\n'
        '    "examples": []\n'
        "  },\n"
        '  "game_plan": {\n'
        '    "early_game": ["..."],\n'
        '    "mid_game": ["..."],\n'
        '    "late_game": ["..."]\n'
        "  },\n"
        '  "key_cards": [\n'
        '    {"name": "Card Name", "role": "what it does", "notes": "how to use"}\n'
        "  ],\n"
        '  "sequencing_tips": ["..."],\n'
        '  "sequencing_tips_evidence": [],\n'
        '  "matchups": [\n'
        '    {"opponent_archetype": "name", "advice": "...", "watch_for": ["..."]}\n'
        "  ],\n"
        '  "common_threats": ["..."],\n'
        '  "common_threats_evidence": []\n'
        "}\n\n"
        "Every key_cards entry MUST be an object with name/role/notes (not a "
        "bare string). Every matchups entry MUST be an object. win_conditions, "
        "sequencing_tips, common_threats MUST be arrays of strings, NOT "
        "single strings."
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
