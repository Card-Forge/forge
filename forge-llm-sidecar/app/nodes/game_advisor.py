"""The game_advisor LangGraph node.

A single node, a single LLM call, two concerns:

1. **Deck recognition** — guess which archetype the *opponent* is playing.
2. **Piloting advice** — recommend what the *AI* should play next, using a
   piloting guide for the AI's own deck.

These are deliberately merged into one prompt/call: the AI re-runs the graph on
every action, and a second LLM call would add noticeable lag. See
``docs/EXTENDING.md`` — latency wins over the "one concern per node" rule here.
"""

from __future__ import annotations

import json
import logging

from app.config import CONFIG
from app.knowledge import format_detect, loader, metagame, piloting
from app.knowledge.piloting_schema import StrategyType
from app.llm_client import LLMError, generate_json
from app.schema import GraphState

log = logging.getLogger(__name__)

# Per-game caches so per-action calls do not redo one-time work.
_resolved_format: dict[str, str] = {}
_own_archetype: dict[str, tuple[str | None, StrategyType]] = {}

_SYSTEM_PROMPT = (
    "You are an expert Magic: The Gathering analyst and coach. You identify the "
    "opponent's deck archetype and advise the AI on how to pilot its own deck. "
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


def _format_guide(guide: dict | None) -> str:
    """Render the AI's own piloting guide as compact prompt text."""
    if not guide:
        return "(no piloting guide available)"
    gp = guide.get("game_plan", {})
    mull = guide.get("mulligan", {})
    parts = [
        f"Your deck: {guide.get('archetype', '?')} "
        f"(strategy: {guide.get('strategy_type', '?')})",
        f"Overview: {guide.get('overview', '')}",
    ]
    if guide.get("win_conditions"):
        parts.append("Win conditions: " + "; ".join(guide["win_conditions"]))
    if mull.get("keep_criteria"):
        parts.append("Keep hands that: " + "; ".join(mull["keep_criteria"]))
    if mull.get("mulligan_criteria"):
        parts.append("Mulligan hands that: " + "; ".join(mull["mulligan_criteria"]))
    for phase in ("early_game", "mid_game", "late_game"):
        if gp.get(phase):
            parts.append(f"{phase.replace('_', ' ').title()}: " + "; ".join(gp[phase]))
    if guide.get("key_cards"):
        kc = "; ".join(f"{c.get('name')} ({c.get('role')})" for c in guide["key_cards"])
        parts.append("Key cards: " + kc)
    if guide.get("sequencing_tips"):
        parts.append("Sequencing tips: " + "; ".join(guide["sequencing_tips"]))
    if guide.get("matchups"):
        mu = "; ".join(
            f"vs {m.get('opponent_archetype')}: {m.get('advice')}" for m in guide["matchups"]
        )
        parts.append("Matchup notes: " + mu)
    if guide.get("common_threats"):
        parts.append("Threats to watch for: " + "; ".join(guide["common_threats"]))
    return "\n".join(parts)


def _format_zone(name: str, cards: list[str]) -> str:
    return f"{name}: {', '.join(cards)}" if cards else f"{name}: (empty/unknown)"


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
    turn = state.get("turn", 0)

    play_block = (
        '  "mulligan_advice": string — keep or mulligan the opening hand, and why,\n'
        if turn <= 0
        else (
            '  "recommended_play": string — the single best play for the AI now,\n'
            '  "play_reasoning": string — one or two sentences,\n'
            '  "play_alternatives": array of up to 3 other reasonable plays,\n'
        )
    )

    return (
        f"Game format: {fmt}\n"
        f"Current turn: {turn}\n\n"
        f"Archetypes in the current metagame:\n{_format_archetypes(archetypes)}\n\n"
        f"{meta_note}"
        f"Opponent's observed plays so far (chronological):\n"
        f"{_format_observations(state.get('observations', []))}\n\n"
        f"--- The AI's own deck guidance ---\n{_format_guide(state.get('piloting_guide'))}\n\n"
        f"--- Current board state ---\n"
        f"{_format_zone('Your hand', state.get('hand', []))}\n"
        f"{_format_zone('Your battlefield', state.get('own_board', []))}\n"
        f"{_format_zone('Opponent battlefield', state.get('opponent_board', []))}\n"
        f"{_format_zone('Your graveyard', state.get('your_graveyard', []))}\n"
        f"{_format_zone('Opponent graveyard', state.get('opponent_graveyard', []))}\n"
        f"Life totals: {state.get('life_totals', {}) or '(unknown)'}\n\n"
        "Do two things and respond with a single JSON object with exactly these keys:\n"
        "1. Identify the opponent's most likely archetype:\n"
        '  "archetype": string (the deck name),\n'
        '  "confidence": number between 0 and 1,\n'
        '  "reasoning": string (one or two sentences),\n'
        '  "alternatives": array of up to 3 other plausible archetype names,\n'
        "2. Advise the AI on piloting its own deck:\n"
        f"{play_block}"
        "Prefer an opponent archetype name from the list above; only invent a name "
        "if none fit, and lower confidence if you do. Base the piloting advice on "
        "the AI's own deck guidance and the current board state.\n"
        f"Valid opponent archetype names: {json.dumps(names)}"
    )


async def _resolve_meta_slug(state: GraphState) -> str:
    """Decide which metagame to score against (cached per game)."""
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
    log.info("game_advisor: game %s -> metagame '%s'", game_id, slug)
    return slug


def _resolve_own_archetype(state: GraphState, slug: str) -> tuple[str | None, StrategyType]:
    """Identify the AI's own archetype once per game (deterministic, no LLM)."""
    game_id = state.get("game_id", "")
    if game_id in _own_archetype:
        return _own_archetype[game_id]

    result = piloting.identify_own_archetype(state.get("deck_cards", []), slug)
    if game_id:
        _own_archetype[game_id] = result
    log.info("game_advisor: game %s -> own archetype %s", game_id, result[0])
    return result


def _fail_soft(state: GraphState, exc: Exception) -> GraphState:
    return {
        **state,
        "archetype": "Unknown",
        "confidence": 0.0,
        "reasoning": f"Model unavailable: {exc}",
        "alternatives": [],
        "recommended_play": "",
        "play_reasoning": "",
        "play_alternatives": [],
        "mulligan_advice": "",
    }


async def game_advisor_node(state: GraphState) -> GraphState:
    """LangGraph node: opponent recognition + own-deck piloting advice.

    One LLM call returns both. Candidate (opponent) archetypes come from the
    current metagame; the AI's own archetype is identified deterministically and
    drives which piloting guide is loaded.
    """
    slug = await _resolve_meta_slug(state)
    state["resolved_format"] = slug

    curated = loader.get_archetypes(slug) or loader.get_archetypes(state.get("format", ""))
    live = metagame.get_metagame(slug) if CONFIG.metagame_enable else []
    archetypes = loader.merge_with_curated(live, curated) if live else curated
    state["candidate_archetypes"] = archetypes
    known_names = {a.get("name", "") for a in archetypes}

    own_name, own_strategy = _resolve_own_archetype(state, slug)
    state["own_archetype"] = own_name
    guide = piloting.get_piloting_guide(own_name or "", slug, own_strategy)
    state["piloting_guide"] = guide.model_dump() if guide else None
    guide_source = ""
    if guide:
        is_specific = own_name and piloting.slugify(guide.archetype) == piloting.slugify(own_name)
        guide_source = (
            f"{slug}/{piloting.slugify(own_name)}"
            if is_specific
            else f"generic/{guide.strategy_type.value}"
        )
    state["guide_source"] = guide_source

    try:
        result = await generate_json(_build_prompt(state), system=_SYSTEM_PROMPT)
    except LLMError as exc:
        log.warning("game_advisor: model call failed: %s", exc)
        return _fail_soft(state, exc)

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

    play_alternatives = result.get("play_alternatives", [])
    if not isinstance(play_alternatives, list):
        play_alternatives = []
    play_alternatives = [str(x) for x in play_alternatives][:3]

    return {
        **state,
        "archetype": archetype,
        "confidence": confidence,
        "reasoning": str(result.get("reasoning", "")).strip(),
        "alternatives": alternatives,
        "recommended_play": str(result.get("recommended_play", "")).strip(),
        "play_reasoning": str(result.get("play_reasoning", "")).strip(),
        "play_alternatives": play_alternatives,
        "mulligan_advice": str(result.get("mulligan_advice", "")).strip(),
    }
