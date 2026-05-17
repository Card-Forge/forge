# Client adapters

The sidecar is **client-agnostic**. It knows nothing about Forge specifically —
it accepts a normalized `RecognitionRequest` describing an opponent's plays and
returns an archetype guess. Anything that can produce that request is an
**adapter**.

```
┌──────────────┐   RecognitionRequest    ┌─────────────────────────┐
│  MTG client  │ ──────(HTTP/JSON)─────▶  │  sidecar (this service) │
│  + adapter   │ ◀─────RecognitionResponse│  LangGraph + LLM        │
└──────────────┘                          └─────────────────────────┘
```

An adapter has two jobs:

1. **Observe** a game on its client — the opponent's cast spells, lands, etc.
2. **Speak the contract** — POST `/recognize` with a `RecognitionRequest` and do
   something with the `RecognitionResponse`.

The wire contract is defined in [API.md](API.md) and versioned via
`schema_version`. Every request carries a `client` field identifying the
adapter (e.g. `"forge"`), so the sidecar can log and, in future, special-case
adapters.

## Reference adapter: Forge

The Forge adapter lives in the `forge.ai.llm` package of the `forge-ai` module
(see its `package-info.java`). It subscribes to Forge's game event bus, records
the opponent's public plays, and calls the sidecar on every opponent action.
It is the only adapter that is implemented and supported today.

## Writing another adapter

Any client can integrate by implementing the two jobs above. Feasibility
depends entirely on what the client exposes:

| Client | How an adapter would observe | Notes |
|---|---|---|
| **Forge** | Game event bus (in-process) | ✅ Implemented. |
| **XMage** | Open-source Java engine — hook the game model | Feasible; similar to Forge. |
| **MTG Arena** | Tail the client's `Player.log` (as deck trackers do) | Observe-only; the result is a human-facing overlay, not an AI input. |
| **MTGO / closed clients** | No plugin or log API | Not supported — no legitimate integration path. |

When adding an adapter:

- Set the `client` field to a stable identifier for your client.
- Populate `deck_cards` if you know the controlled player's decklist — it makes
  format detection reliable from turn one. Omit it otherwise; the sidecar will
  fall back to detecting the format from observed cards.
- Treat the sidecar as optional. Calls should be asynchronous and fail-soft so
  a missing or slow sidecar never disrupts the game.

The sidecar itself needs **no changes** to support a new adapter — that is the
point of the normalized contract.
