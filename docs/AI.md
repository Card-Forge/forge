# About Forge's Artificial Intelligence
The AI is *not* "trained". It uses basic rules and can be easy to overcome knowing its weaknesses.

The AI is:
- Best with Aggro and midrange decks
- Poor to Ok in control decks
- Pretty bad for most combo decks

The logic is mostly based on heuristics and split between effect APIs and all other ingame decisions. Sometimes there is hardcoded logic for single cards but that's usually not a healthy approach though it can be more justifiable for highly iconic cards.  
Defining general concepts of smart play can help improve the win rate much easier, e.g. the AI will always attack with creatures that it has temporarily gained control of until end of turn in order not to miss the opportunity and thus waste the control effect.

If you want to train a model for the AI, please do. We would love to see something like that implemented in Forge.

# AI Matches from Command Line
The AI can battle itself in the command line, allowing the tests to be performed on headless servers or on computers that have poor graphic performance, and when you just don't need to see the match. This can be useful if you want to script testing of decks, test a large tournament, or just bash 100's of games out to see how well a deck performs.

Please understand, the AI is still the AI, and it's limitations exist even against itself. Games can lag and become almost unbearably long when the AI has a lot to think about, and you can't see what's on the table for it to play against. It's best if you set up the tournament and walk away, you can analyze logs later, results are printed at the end.

## Syntax
`sim -d <deck1[.dck]> ... <deckX[.dck]> -D [path] -n [N] -f [F] -t [T] -p [P] -q`

- `sim` - "Simulation Mode" forces Forge to not start the GUI and automatically runs the AI matches in command line. Enables all other switches for simulation mode.
- `-d <deck1[.dck]> ... <deckX[.dck]>` - Space separated list of deck files, in `-f` game type path. (For example; If `-f` is set to Commander, decks from `<userdata>/decks/commander/` will be searched. If `-f` is not set then default is `<userdata>/decks/constructed/`.) Names must use quote marks when they contain spaces.
  - `deck1.dck` - Literal deck file name, when the value has ".dck" extension.
  - `deck` - A meta deck name of a deck file.
- `-D [path]` - [path] is absolute directory path to load decks from. (Overrides path for `-d`.)
- `-n [N]` - [N] number of games, just flat test the AI multiple times. Default is 1.
- `-m [M]` - [M] number of matches, best of [M] matches. (Overrides -n) Recommended 1, 3, or 5. Default is 1.
- `-f [F]` - Runs [F] format of game. Default is "constructed"
  - `Commander`
  - `Oathbreaker`
  - `TinyLeaders`
  - `Brawl`
  - `MomirBasic`
  - `Vanguard`
  - `MoJhoSto`
- `-t [T]` - for Tournament Mode, [T] for type of tournament.
  - `Bracket` - See wikipedia for [Bracket Tournament](https://en.wikipedia.org/wiki/Bracket_(tournament))
  - `RoundRobin` - See wikipedia for [Round Robin Tournaments](https://en.wikipedia.org/wiki/Round-robin_tournament)
  - `Swiss` - See wikipedia for [Swiss Pairing Tournaments](https://en.wikipedia.org/wiki/Swiss-system_tournament)
- `-p [P]` - [P] number of players paired, only used in tournament mode. Default is 2.
- `-q` - Quiet Mode, only prints the result not the entire log.
- `-c [S]` - Clock flag, maximum time of [S] seconds before calling the match a draw. Default is 120.

## Examples
In Windows, if you use the EXE file as described below, the simulation runs in the background and output is sent to the forge log file only. If you want to have output to the console, please use the `java -jar` evocation of forge.

To simulate a basic three games of two decks (deck1 and deck2 must be meta deck names of decks in `<userdata>\decks\constructed\`):
- Windows/Linux/MacOS: `java -jar forge.jar sim -d deck1 deck2 -n 3`
- Windows: `.\forge.exe sim -d deck1 deck2 -n 3`

To simulate a single 3-player Commander game (deck1, deck2, and deck3 must be meta deck names of decks in `<userdata>\decks\commander\`):
- Windows/Linux/MacOS: `java -jar forge.jar sim -d deck1 deck2 deck3 -f commander`
- Windows: `.\forge.exe sim -d deck1 deck2 deck3 -f commander`

To simulate a swiss tournament; best of three, all decks in a directory, 3 player pairings:
- Windows/Linux/MacOS: `java -jar forge.jar sim -D /path/to/DecksFolder/ -m 3 -t Swiss -p 3`
- Windows: `.\forge.exe sim -D C:\DecksFolder\ -m 3 -t Swiss -p 3`

***

Each game ends with an announcement of the winner, and the current status of the match. 

# LLM Sidecar (optional AI assist)

This build can optionally consult an external **LLM sidecar** — a standalone
Python service in [`forge-llm-sidecar/`](../forge-llm-sidecar/) — to recognize
the opponent's deck and advise the heuristic AI on how to play its own. It is
**off by default**, **fully fail-soft**, and changes nothing about a stock build
when it is not enabled or the sidecar is unreachable. The heuristic AI described
above remains the decision-maker; the sidecar only *biases* its existing scoring.

## How it fits together

Forge (JVM) talks to the sidecar over local HTTP. All of the Java glue lives in
`forge-ai/src/main/java/forge/ai/llm/`:

| Class | Role |
|---|---|
| `DeckRecognitionManager` | Attached from `LobbyPlayerAi` when an AI player is created. Self-gating and fail-soft: probes `GET /health` once and attaches nothing if the feature is off or the sidecar is down. |
| `DeckRecognitionObserver` | Subscribes to the game's Guava `EventBus`, records the opponent's public plays, and re-runs recognition on every opponent action and turn boundary. Uses latest-wins coalescing so a slow LLM call never queues up. |
| `DeckRecognitionClient` | Async HTTP client (`HttpURLConnection`, so it also works on Android). Every transport/parse error is swallowed and surfaced as an empty result. |
| `RecognitionRequest` / `RecognitionResult` | The snake_case JSON wire contract (see the sidecar's `docs/ADAPTERS.md`). The request carries observations plus optional live state (hand, boards, graveyards, life) for piloting advice. |
| `SidecarInfluence` | Holds the latest sidecar response and applies it — with personality weighting — at the AI's decision points. This is the bridge that lets the sidecar actually affect play. |
| `SidecarStatusBus` | Process-wide pub/sub fired around blocking sidecar calls; `forge-gui-desktop`'s `CMatchUI` subscribes to show a transient "AI is thinking…" indicator. |

The sidecar runs a LangGraph chain (`game_advisor → mulligan_planner →
combo_strategist → opponent_strategist`) and exposes `/recognize`,
`/mulligan-plan`, `/identify-own-archetype`, and more. See
[`forge-llm-sidecar/README.md`](../forge-llm-sidecar/README.md) and its `docs/`.

## Two layers, gated separately

1. **Recognition** — the observer fires `/recognize` and writes the deck guess
   to the game log. Controlled by `DECK_RECOGNITION_ENABLE` (or
   `-Dforge.ai.deckRecognition=true`) and `DECK_RECOGNITION_SIDECAR_URL`.
2. **Influence** — `SidecarInfluence` feeds the response back into AI decisions
   (piloting, mulligan, discard, targeting, combat role). Each `/recognize`
   result reaches it via `AiController.onSidecarResult(...)`. With influence off,
   the guess is **log-only** and play is unchanged.

These are configured through `AiProps` (set per AI profile `.ai` file, or via a
matching system property). Key properties:

- `SIDECAR_INFLUENCE_ENABLE` / `SIDECAR_INFLUENCE_WEIGHT` (0 = advisory only,
  100 = force legal sidecar choices) — the master switch.
- `SIDECAR_BIAS_*` — per-action max boost (spell, land, attack, block, ability, pass).
- `SIDECAR_ROLE_ASSESSMENT_ENABLE`, `SIDECAR_HAND_VALUATION_ENABLE`,
  `SIDECAR_OPPONENT_INFERENCE_ENABLE`, `SIDECAR_TARGETING_ENABLE`,
  `SIDECAR_MULLIGAN_ENABLE`, `SIDECAR_DISCARD_ENABLE` — toggle individual uses.
- `SIDECAR_WAIT_MS` and per-phase `SIDECAR_WAIT_MS_{MULLIGAN,COMBAT,PRIORITY,CRITICAL}`
  — how long a decision will block waiting for an in-flight LLM call.

## Running it

The repo's `run-forge.sh` wires the desktop build to a sidecar. Point it at a
local or remote (e.g. Tailscale) sidecar with `FORGE_SIDECAR_URL`:

```sh
FORGE_SIDECAR_URL=http://localhost:18970 ./run-forge.sh
```

Note: the on/off **toggle** is the *"Enable AI Deck Recognition"* checkbox under
Settings → Preferences, not a system property — `GamePlayerUtil` overwrites
`-Dforge.ai.deckRecognition` from that UI preference at game start. Only the
sidecar *URL* property is honored from the command line.

## Self-play mode

`Main.java` adds a `selfplay` launch mode (alongside `sim`/`parse`) backed by
`forge.view.SelfPlayRunner`. It drives AI-vs-AI games to exercise and tune the
sidecar (the sidecar's `/selfplay/reflect` endpoint summarizes the results into
learnings). Like `sim`, it runs headless from the command line.
