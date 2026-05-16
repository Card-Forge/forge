"""The LangGraph agent graph.

Currently a single node: START -> deck_recognition -> END. Additional nodes
(play advisor, threat assessment, ...) can be added with ``add_node`` /
``add_edge`` without changing the HTTP contract, because :class:`GraphState`
is a superset TypedDict.
"""

from __future__ import annotations

import functools

from langgraph.graph import END, START, StateGraph

from app.nodes.deck_recognition import deck_recognition_node
from app.schema import GraphState

_DECK_RECOGNITION = "deck_recognition"


@functools.lru_cache(maxsize=1)
def get_graph():
    """Build and compile the graph once (cached for the process lifetime)."""
    builder = StateGraph(GraphState)
    builder.add_node(_DECK_RECOGNITION, deck_recognition_node)
    builder.add_edge(START, _DECK_RECOGNITION)
    builder.add_edge(_DECK_RECOGNITION, END)
    return builder.compile()
