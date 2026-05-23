"""The LangGraph agent graph.

A linear chain: START -> deck_recognition -> game_advisor ->
mulligan_planner -> combo_strategist -> opponent_strategist -> END.

``deck_recognition`` does opponent deck recognition with one LLM call per
trigger. ``game_advisor`` then derives AI piloting advice locally from the
completed recognition result, so recognition stays grounded only in observed
human plays — it never sees the AI deck or game state. The later nodes self-gate
and fail soft, only enriching the state:
``mulligan_planner`` adds keep/mulligan and rolling early-game planning,
``combo_strategist`` refines action scores for combo decks, and
``opponent_strategist`` does deeper opponent reasoning (hand inference,
next-turn prediction, threat ranking).

Further nodes can still be added with ``add_node`` / ``add_edge`` without
changing the HTTP contract, because :class:`GraphState` is a superset TypedDict.
"""

from __future__ import annotations

import functools

from langgraph.graph import END, START, StateGraph

from app.nodes.combo_strategist import combo_strategist_node
from app.nodes.game_advisor import game_advisor_node, recognition_node
from app.nodes.mulligan_planner import mulligan_planner_node
from app.nodes.opponent_strategist import opponent_strategist_node
from app.schema import GraphState

_DECK_RECOGNITION = "deck_recognition"
_GAME_ADVISOR = "game_advisor"
_MULLIGAN_PLANNER = "mulligan_planner"
_COMBO_STRATEGIST = "combo_strategist"
_OPPONENT_STRATEGIST = "opponent_strategist"


@functools.lru_cache(maxsize=1)
def get_graph():
    """Build and compile the graph once (cached for the process lifetime).

    ``deck_recognition`` recognizes the opponent;
    ``game_advisor`` derives own-deck piloting from that completed result;
    ``mulligan_planner`` adds keep/mulligan and rolling early-turn planning;
    ``combo_strategist`` then refines action scores for AI combo decks with
    structured combo profiles;
    ``opponent_strategist`` then does the deeper Pro-Tour-style opponent
    reasoning (hand inference, next-turn prediction, threat ranking) when a
    profile exists for the recognized archetype. The strategist self-gates and
    fails soft, so it adds no risk to the recognition path.
    """
    builder = StateGraph(GraphState)
    builder.add_node(_DECK_RECOGNITION, recognition_node)
    builder.add_node(_GAME_ADVISOR, game_advisor_node)
    builder.add_node(_MULLIGAN_PLANNER, mulligan_planner_node)
    builder.add_node(_COMBO_STRATEGIST, combo_strategist_node)
    builder.add_node(_OPPONENT_STRATEGIST, opponent_strategist_node)
    builder.add_edge(START, _DECK_RECOGNITION)
    builder.add_edge(_DECK_RECOGNITION, _GAME_ADVISOR)
    builder.add_edge(_GAME_ADVISOR, _MULLIGAN_PLANNER)
    builder.add_edge(_MULLIGAN_PLANNER, _COMBO_STRATEGIST)
    builder.add_edge(_COMBO_STRATEGIST, _OPPONENT_STRATEGIST)
    builder.add_edge(_OPPONENT_STRATEGIST, END)
    return builder.compile()
