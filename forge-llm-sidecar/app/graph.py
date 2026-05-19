"""The LangGraph agent graph.

A single node: START -> game_advisor -> END. The ``game_advisor`` node does
opponent deck recognition *and* own-deck piloting advice in one LLM call —
these are merged (rather than split into two nodes) to keep per-action latency
low; see ``docs/EXTENDING.md``. Further nodes can still be added with
``add_node`` / ``add_edge`` without changing the HTTP contract, because
:class:`GraphState` is a superset TypedDict.
"""

from __future__ import annotations

import functools

from langgraph.graph import END, START, StateGraph

from app.nodes.game_advisor import game_advisor_node
from app.schema import GraphState

_GAME_ADVISOR = "game_advisor"


@functools.lru_cache(maxsize=1)
def get_graph():
    """Build and compile the graph once (cached for the process lifetime)."""
    builder = StateGraph(GraphState)
    builder.add_node(_GAME_ADVISOR, game_advisor_node)
    builder.add_edge(START, _GAME_ADVISOR)
    builder.add_edge(_GAME_ADVISOR, END)
    return builder.compile()
