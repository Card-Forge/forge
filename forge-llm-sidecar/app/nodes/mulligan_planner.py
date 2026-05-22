"""Mulligan and rolling early-game planning node."""

from __future__ import annotations

import logging

from app.early_plan import build_early_game_plan, filter_actions_to_legal, merge_plan_actions
from app.schema import GraphState

log = logging.getLogger(__name__)


async def mulligan_planner_node(state: GraphState) -> GraphState:
    """Add a keep/mulligan decision and rolling early-game plan.

    The implementation is deterministic and fail-soft. It uses the resolved
    piloting guide and current game state prepared by game_advisor. Future
    iterations can add an LLM pass over this deterministic scaffold without
    changing the response contract.
    """
    if not state.get("hand") and (state.get("decision_type") or "") != "mulligan":
        return state
    try:
        plan = build_early_game_plan(state)
    except Exception as exc:  # pragma: no cover - defensive fail-soft path
        log.warning("mulligan_planner: failed to build plan: %s", exc)
        return state
    out: GraphState = {**state}
    out["early_game_plan"] = {k: v for k, v in plan.items() if k != "action_adjustments"}
    merged_actions = merge_plan_actions(state.get("actions") or [], plan.get("action_adjustments") or [])
    out["actions"] = filter_actions_to_legal(state, merged_actions)
    if (state.get("decision_type") or "") == "mulligan":
        decision = plan.get("decision") or "keep"
        out["mulligan_advice"] = (
            "Keep: " + str(plan.get("keep_reason") or "")
            if decision == "keep"
            else "Mulligan: " + str(plan.get("mulligan_reason") or plan.get("reasoning") or "")
        )
    return out
