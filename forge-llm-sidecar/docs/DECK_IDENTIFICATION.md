# Deck Identification — How the AI Sidecar Works

## Architecture Overview

The AI sidecar is a **separate Python HTTP service** (`forge-llm-sidecar/`) that the Forge Java AI calls over HTTP. It exists because the AI uses LangGraph (a Python library), and keeping it as a standalone process avoids embedding Python in the JVM.

```
Forge AI (Java)                    Sidecar (Python)
────────────                       ───────────────
DeckRecognitionObserver  ──POST──▶ FastAPI
(subscribes to game events)        │
                                 LangGraph graph
                                 └─ deck_recognition node
                                      └─ LLM call -> JSON response
```

### Triggering (Java side: `DeckRecognitionObserver.java`)

- Subscribes to Forge's **Guava EventBus** for game events
- Records opponent plays: **spells cast** and **lands played** (ignores the AI's own plays)
- Re-runs recognition on **every opponent action** and **every turn boundary** (so a pass is also captured)
- Uses **latest-wins coalescing**: if a recognition is already in flight, only one rerun is queued — intermediate states are skipped but the final state is always evaluated
- Waits until **turn 2** and at least **one observation** before making a guess
- Writes the guess to the game log only; **never influences** the heuristic AI's decisions

### The Recognition Flow (Sidecar)

1. **Resolve format** — Uses the format Forge reports; if it's generic ("Constructed"), calls Scryfall to detect the format from the AI's own decklist. Cached per game.
2. **Load candidate archetypes** — Pulls the metagame for that format (from committed JSON files, scraped weekly by CI) and merges in curated archetype details (strategy, signature cards, tells).
3. **Build prompt** — Assembles a structured prompt with: format, current turn, candidate archetypes with metagame shares, and the opponent's chronological plays.
4. **Call LLM** — Queries a local LLM (an OpenAI-compatible server such as llama.cpp) in JSON mode.
5. **Parse & clamp** — Returns archetype, confidence (clamped to [0, 1]), reasoning, and up to 3 alternatives. If the LLM names an archetype outside the known set, confidence is capped at 0.4.

---

## The AI's Thought Process (Prompt)

The LLM receives a prompt structured like this:

```
Game format: Modern
Current turn: 4

Archetypes in the current metagame:
- Izzet Murktide [U, R] — 12.3% of the current metagame: Aggro deck that uses cards like
    signature cards: Murktide Navigator, Thoughtseize, Lightning Bolt, Opt
    tells: plays multiple instants, sacrifices creatures for tempo
- Rakdos Midrange [B, R] — 8.7% of the current metagame: Midrange deck that controls the
    signature cards: Bloodtithe Harvester, Gut Shot, Dreadmourn Witch
    tells: discards opponent's cards, plays midrange creatures
- ... (more archetypes with their metagame share percentages)

Opponent's observed plays so far (chronological):
- turn 1: land Sworn Companion (cmc 2, W, Creature)
- turn 2: spell Opt (cmc 1, U, Instant)
- turn 3: spell Thoughtseize (cmc 1, B, Instant)
- turn 4: spell Lightning Bolt (cmc 1, R, Instant)
```

The system prompt tells the model:
> "You are an expert Magic: The Gathering analyst. You identify which deck archetype an opponent is playing from the cards they have revealed. Always answer with a single JSON object and nothing else."

Then it's asked to:
1. Pick the **single most likely archetype** from the provided list
2. Give a **confidence** (0–1)
3. Provide **reasoning** (1–2 sentences)
4. Suggest up to **3 alternatives**

The LLM's reasoning is essentially pattern-matching: *"The opponent played Sworn Companion on turn 1, followed by Opt, Thoughtseize, and Lightning Bolt — these are all signature cards and tells for Izzet Murktide, which is also the most popular archetype at 12.3%."*

The metagame percentages act as a **prior** — the prompt instructs the model to prefer more popular archetypes when evidence is ambiguous.

---

## Key Design Decisions

- **Read-only / fail-soft**: The sidecar never affects gameplay. If the LLM server is down, Scryfall is down, or the sidecar itself is offline, the AI just plays normally.
- **No runtime scraping**: Metagame data is scraped weekly by CI and committed as JSON. The sidecar has no network dependencies at request time.
- **Extensible graph**: The LangGraph currently has one node, but the `GraphState` TypedDict is designed to let future nodes be added (play advisor, threat assessment, etc.) without changing the HTTP contract.
