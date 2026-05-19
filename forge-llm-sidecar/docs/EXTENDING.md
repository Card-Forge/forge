# Extending the agent

The graph is built to grow. This guide walks through adding a second node,
using the planned **strategy classifier** (aggro / control / midrange / combo /
ramp / other) as the worked example.

## How the graph is structured

`app/graph.py` builds a `StateGraph` over `GraphState` (`app/schema.py`).
`GraphState` is a `TypedDict` with `total=False`, so it can carry keys that only
some nodes use. Today:

```
START ──▶ game_advisor ──▶ END
```

Adding a node is three steps: extend the state, write the node, wire it in.

## 1. Extend `GraphState`

In `app/schema.py`, add the keys the new node produces:

```python
class GraphState(TypedDict, total=False):
    ...
    # outputs of the strategy_classification node
    strategy: Optional[str]            # "aggro" | "control" | ...
    strategy_scores: dict              # {"aggro": 0.6, "control": 0.1, ...}
```

If the new node's result should be returned over HTTP, also add fields to
`RecognitionResponse` (and bump `SCHEMA_VERSION`) — but a node can equally be
internal-only.

## 2. Write the node

Create `app/nodes/strategy_classification.py`. A node is an `async` function
`GraphState -> GraphState`:

```python
from app.llm_client import LLMError, generate_json
from app.schema import GraphState

_SYSTEM = "You are an MTG strategist. Answer with a single JSON object."

async def strategy_classification_node(state: GraphState) -> GraphState:
    # The game_advisor node has already run, so its output is available:
    archetype = state.get("archetype", "Unknown")
    observations = state.get("observations", [])

    prompt = _build_prompt(archetype, observations)
    try:
        result = await generate_json(prompt, system=_SYSTEM)
    except LLMError:
        return {**state, "strategy": "unknown", "strategy_scores": {}}

    scores = _normalise(result.get("scores", {}))
    return {
        **state,
        "strategy": max(scores, key=scores.get) if scores else "unknown",
        "strategy_scores": scores,
    }
```

Reuse what already exists — `generate_json` for the LLM call, `loader` /
`metagame` for knowledge — rather than adding new infrastructure.

## 3. Wire it into the graph

In `app/graph.py`:

```python
from app.nodes.strategy_classification import strategy_classification_node

_STRATEGY = "strategy_classification"

@functools.lru_cache(maxsize=1)
def get_graph():
    builder = StateGraph(GraphState)
    builder.add_node(_GAME_ADVISOR, game_advisor_node)
    builder.add_node(_STRATEGY, strategy_classification_node)
    builder.add_edge(START, _GAME_ADVISOR)
    builder.add_edge(_GAME_ADVISOR, _STRATEGY)   # runs after the advisor
    builder.add_edge(_STRATEGY, END)
    return builder.compile()
```

Because nodes run in sequence and share `GraphState`, `strategy_classification`
sees everything `game_advisor` produced (`archetype`, `candidate_archetypes`,
`resolved_format`, …).

## 4. Surface the result

If the result goes back to Forge, add it to the `RecognitionResponse` mapping in
`app/main.py` and to the Java `RecognitionResult` record. If it is only consumed
by a later node, no API change is needed.

## Testing a new node

Follow `tests/test_graph.py`: stub `generate_json` so tests stay offline and
deterministic, invoke `get_graph().ainvoke(state)`, and assert on the new state
keys. Keep network-touching helpers (`format_detect`, `scraper`) stubbed.

## Design conventions

- **Fail-soft.** A node must always return a valid `GraphState`; never raise
  into the graph. Degrade to a neutral value on any error.
- **One LLM concern per node — unless latency says otherwise.** Focused prompts
  in separate nodes are the default. The `game_advisor` node deliberately
  breaks this rule: it does opponent recognition *and* piloting advice in one
  call, because the AI re-runs the graph on every action and a second LLM call
  would add noticeable lag. Merge concerns only when per-action latency
  justifies it.
- **Keep the HTTP contract stable.** Add response fields additively and bump
  `SCHEMA_VERSION`; the Java client tolerates unknown fields.
- **Cache expensive lookups.** See the per-game caches (`_resolved_format`,
  `_own_archetype` in `game_advisor.py`) for the pattern.
