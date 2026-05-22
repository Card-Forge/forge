"""Combo strategist node for the AI's own deck.

Runs after game_advisor has resolved the AI piloting guide and initial action
scores. The node self-gates to decks with a guide-level combo_profile, computes
deterministic combo readiness, then asks the strategist model to choose the
current line. If the model call fails, the deterministic plan is still applied.
"""

from __future__ import annotations

import json
import logging

from app.combo import analyze_combo_state, merge_combo_adjustments
from app.config import CONFIG
from app.llm_client import LLMError, generate_json
from app.schema import GraphState

log = logging.getLogger(__name__)

_SYSTEM_PROMPT = (
    "You are a Pro Tour-level Magic: The Gathering combo pilot. Given a combo "
    "profile, deterministic readiness analysis, and current public game state, "
    "choose the best current combo line for the AI's own deck. Be conservative "
    "against open countermagic unless the deterministic analysis says the AI is "
    "under pressure. Always answer with a single JSON object and nothing else."
)


def _prompt(state: GraphState, profile: dict, deterministic: dict) -> str:
    return (
        f"AI deck: {state.get('own_archetype') or 'Unknown'}\n"
        f"Turn: {state.get('turn', 0)} Phase: {state.get('phase') or '?'}\n"
        f"AI hand: {', '.join(state.get('hand') or []) or '(empty)'}\n"
        f"AI battlefield: {', '.join(state.get('own_board') or []) or '(empty)'}\n"
        f"AI graveyard: {', '.join(state.get('your_graveyard') or []) or '(empty)'}\n"
        f"AI available mana symbols: {', '.join(state.get('available_mana') or []) or '(none)'}\n"
        f"Opponent archetype: {state.get('archetype') or 'Unknown'}\n"
        f"Opponent battlefield: {', '.join(state.get('opponent_board') or []) or '(empty)'}\n"
        f"Opponent open mana sources: {json.dumps(state.get('opp_untapped_sources') or [])}\n"
        f"Opponent inferred hand: {json.dumps(state.get('opponent_hand') or [])}\n\n"
        f"COMBO PROFILE\n{json.dumps(profile, indent=2)}\n\n"
        f"DETERMINISTIC ANALYSIS\n{json.dumps(deterministic, indent=2)}\n\n"
        "Return exactly these keys:\n"
        '  "line_name": string,\n'
        '  "go_for_it_now": boolean,\n'
        '  "readiness_score": number from 0 to 100,\n'
        '  "needed_cards": [string],\n'
        '  "needed_mana": string,\n'
        '  "sequence": [string],\n'
        '  "protection_plan": string,\n'
        '  "risk_assessment": string,\n'
        '  "action_adjustments": [{"action_type": "PLAY_SPELL"|"PLAY_LAND"|"PASS", '
        '"target": string, "targets": null, "percentage": number, "reasoning": string}]\n'
        "Only name cards that are in the AI hand, battlefield, graveyard, or the combo profile."
    )


def _coerce_adjustments(raw: object) -> list[dict]:
    if not isinstance(raw, list):
        return []
    out: list[dict] = []
    for item in raw:
        if not isinstance(item, dict):
            continue
        action_type = str(item.get("action_type") or "").strip()
        if action_type not in {"PLAY_SPELL", "PLAY_LAND", "PASS"}:
            continue
        try:
            pct = max(0.0, min(100.0, float(item.get("percentage") or 0.0)))
        except (TypeError, ValueError):
            pct = 0.0
        out.append(
            {
                "action_type": action_type,
                "target": str(item.get("target") or ""),
                "targets": item.get("targets") if isinstance(item.get("targets"), list) else None,
                "percentage": pct,
                "reasoning": str(item.get("reasoning") or ""),
            }
        )
    return out


def _merge_plan(deterministic: dict, llm_result: dict | None) -> dict:
    plan = {k: v for k, v in deterministic.items() if k != "bucket_state"}
    if not isinstance(llm_result, dict):
        return plan
    for key in (
        "line_name",
        "go_for_it_now",
        "readiness_score",
        "needed_cards",
        "needed_mana",
        "sequence",
        "protection_plan",
        "risk_assessment",
    ):
        if key in llm_result:
            plan[key] = llm_result[key]
    if "needed_cards" in plan and "missing_pieces" not in plan:
        plan["missing_pieces"] = plan["needed_cards"]
    llm_adjustments = _coerce_adjustments(llm_result.get("action_adjustments"))
    if llm_adjustments:
        plan["action_adjustments"] = llm_adjustments
    return plan


async def combo_strategist_node(state: GraphState) -> GraphState:
    guide = state.get("piloting_guide") or {}
    combo_profile = guide.get("combo_profile") if isinstance(guide, dict) else None
    if not isinstance(combo_profile, dict):
        return state
    if (guide.get("strategy_type") or "").lower() != "combo":
        return state

    deterministic = analyze_combo_state(state, combo_profile)
    llm_result: dict | None = None
    try:
        llm_result = await generate_json(
            _prompt(state, combo_profile, deterministic),
            system=_SYSTEM_PROMPT,
            model=CONFIG.strategist_model_name,
            temperature=0.2,
        )
    except LLMError as exc:
        log.warning("combo_strategist: LLM call failed (%s); using deterministic plan", exc)

    plan = _merge_plan(deterministic, llm_result)
    adjustments = plan.get("action_adjustments") or []
    out: GraphState = {**state}
    out["combo_plan"] = plan
    out["actions"] = merge_combo_adjustments(state.get("actions") or [], adjustments)
    return out
