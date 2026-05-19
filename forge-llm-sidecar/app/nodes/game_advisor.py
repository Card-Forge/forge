"""The game_advisor LangGraph node.

One graph node, one LLM call per trigger:

1. **Deck recognition** — guess which archetype the *human opponent* is
   playing from observed human plays only.
2. **Piloting guidance** — resolve the AI's own guide deterministically and
   return lightweight guide-derived advice without another model call.

Recognition deliberately does not receive the AI deck, guide, hand, board, or
graveyard, so it cannot flip-flop into identifying the AI's own archetype.
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

_RECOGNITION_SYSTEM_PROMPT = (
    "You are an expert Magic: The Gathering deck-recognition analyst. "
    "Identify only the human opponent's deck from observed human plays. "
    "You will not receive the AI player's deck or guidance. "
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


def _format_personality(personality: dict | None) -> str:
    """Render the AI's personality profile as compact prompt text."""
    if not personality:
        return "(no personality profile provided)"
    parts = []
    for k, v in personality.items():
        parts.append(f"{k}={v}")
    return "AI personality: " + ", ".join(parts) if parts else "(no traits)"


def _build_recognition_prompt(state: GraphState) -> str:
    """Build a recognition-only prompt with no AI deck or piloting context."""
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
        f"Human opponent's observed plays so far (chronological):\n"
        f"{_format_observations(state.get('observations', []))}\n\n"
        "Identify the human opponent's most likely archetype. Use only the "
        "observed plays above as evidence. Do not infer from turn player, AI "
        "deck identity, AI guidance, AI hand, AI battlefield, or AI graveyard.\n"
        "Respond with exactly these keys:\n"
        '  "archetype": string (the human opponent deck name),\n'
        '  "confidence": number between 0 and 1,\n'
        '  "reasoning": string (one or two sentences based only on observed plays),\n'
        '  "alternatives": array of up to 3 other plausible human opponent archetype names.\n'
        "Prefer an opponent archetype name from the list above; only invent a name "
        "if none fit, and lower confidence if you do.\n"
        f"Valid human opponent archetype names: {json.dumps(names)}"
    )


def _guide_list(items: list[str], fallback: str) -> str:
    return "; ".join(items[:2]) if items else fallback


def _local_piloting_advice(state: GraphState) -> dict:
    """Return guide-derived piloting advice with structured action scores."""
    guide = state.get("piloting_guide") or {}
    personality = state.get("personality") or {}
    turn = state.get("turn", 0)
    mulligan = guide.get("mulligan", {})
    game_plan = guide.get("game_plan", {})
    key_cards = guide.get("key_cards", [])
    strategy = guide.get("strategy_type", "").lower()

    # Determine if the AI is aggressive based on personality or strategy.
    is_aggro = personality.get("play_aggro") in (True, "true", "True") or strategy == "aggro"
    # Base action percentages derived from guide strategy & personality.
    base_actions: list[dict] = []

    if turn <= 0:
        keep = _guide_list(mulligan.get("keep_criteria", []), "hands that execute the deck plan")
        ship = _guide_list(mulligan.get("mulligan_criteria", []), "hands lacking early action")
        mulligan_text = f"Keep {keep}. Mulligan {ship}."
        base_actions = [
            {
                "action_type": "MULLIGAN",
                "target": "keep",
                "targets": None,
                "percentage": 70.0 if "Keep" in mulligan_text else 30.0,
                "reasoning": "Derived from piloting guide mulligan criteria.",
            }
        ]
        return {
            "recommended_play": "",
            "play_reasoning": "",
            "play_alternatives": [],
            "mulligan_advice": mulligan_text,
            "actions": base_actions,
        }

    phase = "early_game" if turn <= 3 else "mid_game" if turn <= 7 else "late_game"
    plan = _guide_list(game_plan.get(phase, []), guide.get("overview") or "advance the deck plan")
    alternatives = game_plan.get("mid_game" if phase == "early_game" else "late_game", [])[:2]
    key = ""
    if key_cards:
        names = [str(c.get("name", "")).strip() for c in key_cards if c.get("name")]
        if names:
            key = " Prioritize " + ", ".join(names[:2]) + " when the board state supports it."

    # Build structured actions.
    # PLAY_SPELL: always a primary action, boosted by aggro.
    spell_pct = 65.0 if is_aggro else 50.0
    base_actions.append(
        {
            "action_type": "PLAY_SPELL",
            "target": plan.split(";")[0].strip() if ";" in plan else plan.strip(),
            "targets": None,
            "percentage": spell_pct,
            "reasoning": f"{phase} priority from piloting guide.",
        }
    )

    # ATTACK: higher priority for aggro/tempo strategies.
    attack_pct = 50.0 if is_aggro else 25.0
    base_actions.append(
        {
            "action_type": "ATTACK",
            "target": "all_available",
            "targets": None,
            "percentage": attack_pct,
            "reasoning": "Based on deck strategy and board state.",
        }
    )

    # PLAY_LAND: present but medium priority.
    base_actions.append(
        {
            "action_type": "PLAY_LAND",
            "target": "",
            "targets": None,
            "percentage": 40.0,
            "reasoning": "Advance mana development.",
        }
    )

    # ACTIVATE_ABILITY: lower unless key cards mention activated abilities.
    activate_pct = 25.0
    base_actions.append(
        {
            "action_type": "ACTIVATE_ABILITY",
            "target": "",
            "targets": None,
            "percentage": activate_pct,
            "reasoning": "Use utility abilities when beneficial.",
        }
    )

    # BLOCK: lower for aggro, moderate otherwise.
    block_pct = 15.0 if is_aggro else 35.0
    base_actions.append(
        {
            "action_type": "BLOCK",
            "target": "",
            "targets": None,
            "percentage": block_pct,
            "reasoning": "Defensive consideration.",
        }
    )

    # PASS: lowest for aggro, moderate for control.
    pass_pct = 10.0 if is_aggro else 30.0
    base_actions.append(
        {
            "action_type": "PASS",
            "target": "",
            "targets": None,
            "percentage": pass_pct,
            "reasoning": "Hold up mana or pass when no good play exists.",
        }
    )

    return {
        "recommended_play": plan + key,
        "play_reasoning": "Derived from the resolved AI piloting guide; no extra LLM call.",
        "play_alternatives": [str(x) for x in alternatives],
        "mulligan_advice": "",
        "actions": base_actions,
    }


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
        "actions": [],
    }


async def game_advisor_node(state: GraphState) -> GraphState:
    """LangGraph node: opponent recognition plus local own-deck guidance.

    One LLM call recognizes the human opponent from observed human plays only.
    The AI's own archetype and piloting guide are resolved deterministically and
    converted into lightweight local advice without another model call.
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
        result = await generate_json(
            _build_recognition_prompt(state),
            system=_RECOGNITION_SYSTEM_PROMPT,
        )
    except LLMError as exc:
        log.warning("game_advisor: recognition model call failed: %s", exc)
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

    reasoning = str(result.get("reasoning", "")).strip()

    piloting_result = _local_piloting_advice(state)

    play_alternatives = piloting_result.get("play_alternatives", [])
    if not isinstance(play_alternatives, list):
        play_alternatives = []
    play_alternatives = [str(x) for x in play_alternatives][:3]

    raw_actions = piloting_result.get("actions", [])
    if not isinstance(raw_actions, list):
        raw_actions = []

    return {
        **state,
        "archetype": archetype,
        "confidence": confidence,
        "reasoning": reasoning,
        "alternatives": alternatives,
        "recommended_play": str(piloting_result.get("recommended_play", "")).strip(),
        "play_reasoning": str(piloting_result.get("play_reasoning", "")).strip(),
        "play_alternatives": play_alternatives,
        "mulligan_advice": str(piloting_result.get("mulligan_advice", "")).strip(),
        "actions": raw_actions,
    }
