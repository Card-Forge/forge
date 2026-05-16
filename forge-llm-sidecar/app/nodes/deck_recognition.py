"""The deck_recognition LangGraph node.

Given the game format and the opponent's observed plays, ask the local LLM to
identify the most likely deck archetype, scored against the current metagame.
"""
from __future__ import annotations

import json
import logging

from app.config import CONFIG
from app.knowledge import format_detect, loader, metagame
from app.ollama_client import OllamaError, generate_json
from app.schema import GraphState

log = logging.getLogger(__name__)

# Resolved metagame slug per game_id, so Scryfall format detection runs at most
# once per game rather than on every (per-action) recognition call.
_resolved_format: dict[str, str] = {}

_SYSTEM_PROMPT = (
    "You are an expert Magic: The Gathering analyst. You identify which deck "
    "archetype an opponent is playing from the cards they have revealed. "
    "Always answer with a single JSON object and nothing else."
)


def _format_archetypes(archetypes: list[dict]) -> str:
    lines = []
    for a in archetypes:
        colors = "/".join(a.get("colors", [])) or "?"
        sig = ", ".join(a.get("signature_cards", [])) or "(none listed)"
        tells = "; ".join(a.get("tells", [])) or "(none listed)"
        share = a.get("meta_share")
        share_str = f" — {share:.1f}% of the current metagame" if share else ""
        lines.append(
            f"- {a.get('name', '?')} [{colors}]{share_str}: {a.get('strategy', '')}\n"
            f"    signature cards: {sig}\n"
            f"    tells: {tells}"
        )
    return "\n".join(lines)


def _format_observations(observations: list[dict]) -> str:
    if not observations:
        return "(no plays observed yet)"
    lines = []
    for o in observations:
        colors = "/".join(o.get("colors", [])) or "C"
        types = " ".join(o.get("types", [])) or "?"
        lines.append(
            f"- turn {o.get('turn', '?')}: {o.get('event', '?')} "
            f"{o.get('card', '?')} (cmc {o.get('cmc', 0)}, {colors}, {types})"
        )
    return "\n".join(lines)


def _build_prompt(state: GraphState) -> str:
    archetypes = state.get("candidate_archetypes", [])
    names = [a.get("name", "?") for a in archetypes]
    has_shares = any(a.get("meta_share") for a in archetypes)
    meta_note = (
        "The percentages show each archetype's share of the CURRENT metagame; "
        "all else being equal, prefer more popular archetypes.\n"
        if has_shares
        else ""
    )
    fmt = state.get("resolved_format") or state.get("format", "Unknown")
    return (
        f"Game format: {fmt}\n"
        f"Current turn: {state.get('turn', 0)}\n\n"
        f"Archetypes in the current metagame:\n{_format_archetypes(archetypes)}\n\n"
        f"{meta_note}"
        f"Opponent's observed plays so far (chronological):\n"
        f"{_format_observations(state.get('observations', []))}\n\n"
        "Identify the single most likely archetype the opponent is playing.\n"
        "Prefer a name from the archetype list above; only invent a name "
        "if none fit, and lower your confidence if you do.\n"
        "Respond with a JSON object with exactly these keys:\n"
        '  "archetype": string (the deck name),\n'
        '  "confidence": number between 0 and 1,\n'
        '  "reasoning": string (one or two sentences),\n'
        '  "alternatives": array of up to 3 other plausible archetype names.\n'
        f"Valid archetype names: {json.dumps(names)}"
    )


async def _resolve_meta_slug(state: GraphState) -> str:
    """Decide which metagame to score against.

    Uses the format Forge reported; if that is generic ("Constructed"), detects
    the precise format from the AI's own deck via Scryfall. The result is
    cached per game so detection runs at most once per game.
    """
    game_id = state.get("game_id", "")
    if game_id in _resolved_format:
        return _resolved_format[game_id]

    slug = metagame.resolve_meta_format(state.get("format", ""))
    if not slug:
        slug = await format_detect.infer_format(state.get("deck_cards", []))
    if not slug:
        slug = CONFIG.default_meta_format

    if game_id:
        _resolved_format[game_id] = slug
    log.info("deck_recognition: game %s -> metagame '%s'", game_id, slug)
    return slug


async def deck_recognition_node(state: GraphState) -> GraphState:
    """LangGraph node: produce an archetype guess from observations.

    Candidate archetypes come from the current metagame for the detected
    format, enriched with curated card details and falling back to the curated
    knowledge base when no metagame data is available.
    """
    slug = await _resolve_meta_slug(state)
    state["resolved_format"] = slug

    curated = loader.get_archetypes(slug) or loader.get_archetypes(state.get("format", ""))
    live = metagame.get_metagame(slug) if CONFIG.metagame_enable else []
    if live:
        archetypes = loader.merge_with_curated(live, curated)
    else:
        archetypes = curated
    state["candidate_archetypes"] = archetypes
    known_names = {a.get("name", "") for a in archetypes}

    try:
        result = await generate_json(_build_prompt(state), system=_SYSTEM_PROMPT)
    except OllamaError as exc:
        log.warning("deck_recognition: model call failed: %s", exc)
        return {
            **state,
            "archetype": "Unknown",
            "confidence": 0.0,
            "reasoning": f"Model unavailable: {exc}",
            "alternatives": [],
        }

    archetype = str(result.get("archetype", "Unknown")).strip() or "Unknown"
    try:
        confidence = float(result.get("confidence", 0.0))
    except (TypeError, ValueError):
        confidence = 0.0
    confidence = max(0.0, min(1.0, confidence))

    # An archetype outside the curated KB is kept but its confidence is capped,
    # since the model is guessing beyond what we can corroborate.
    if archetype not in known_names and archetype != "Unknown":
        confidence = min(confidence, 0.4)

    alternatives = result.get("alternatives", [])
    if not isinstance(alternatives, list):
        alternatives = []
    alternatives = [str(x) for x in alternatives][:3]

    return {
        **state,
        "archetype": archetype,
        "confidence": confidence,
        "reasoning": str(result.get("reasoning", "")).strip(),
        "alternatives": alternatives,
    }
