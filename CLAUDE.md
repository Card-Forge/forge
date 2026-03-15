# Forge RL Research

Fork of [Forge](https://github.com/Card-Forge/forge) MTG engine with reinforcement learning infrastructure. All RL work lives on the `rl-research` branch.

## Repo Layout

```
forge-core/             # Card rules, static data, core game types
forge-game/             # Game engine (turns, phases, combat, stack)
forge-ai/               # Built-in rule-based AI (training opponent)
forge-gui/              # GUI abstractions, FModel initialization
forge-gui-desktop/      # Swing desktop client
forge-research/         # Java: gRPC server, RL player controller, observations
forge-research-python/  # Python: PPO training, gym env, gRPC client
```

## Building

```bash
# Full compile (required first time, -am builds all dependencies)
mvn compile -pl forge-research -am

# Classpath file (needed for shell scripts)
cd forge-research && mvn dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
```

## Python Setup

```bash
cd forge-research-python
python -m venv .venv && source .venv/bin/activate
pip install -e .
pip install torch gymnasium grpcio grpcio-tools protobuf tensorboard
```

Regenerate proto stubs: `cd forge-research-python && ./generate_proto.sh`

## Training

```bash
cd forge-research-python && source .venv/bin/activate
python train_ppo.py \
  --run-name run_001_mono_red \
  --deck-a src/main/resources/decks/mono_red_pingers.dck \
  --deck-b src/main/resources/decks/caw_gates.dck \
  --num-envs 8
```

This auto-starts 8 Java gRPC servers with lazy loading + `-Xmx128m` heap cap, trains player 0 (deck_a) vs Forge AI on deck_b, and kills servers on exit.

Key flags: `--no-reward-shaping`, `--swap-decks`, `--opponent-model <path>`, `--total-timesteps N`

Monitor: `tensorboard --logdir runs` — watch `charts/win_rate`, `charts/SPS` (~50 expected)

## Available Decks (`forge-research/src/main/resources/decks/`)

- `mono_red_pingers.dck` — Best trained so far (~73% vs AI caw_gates)
- `caw_gates.dck` — Control/midrange
- `ramunap_red.dck` — Mono red aggro
- `bg_constrictor.dck` — BG midrange

## GUI Play

```bash
./forge-research/play_hotseat.sh              # Control both sides
./forge-research/play_hotseat_logged.sh       # Same but logs all decisions as JSON
./forge-research/play_vs_onnx.sh model.onnx   # Play against exported model
```

## Architecture (Java)

- **`ForgeResearchServer`** — gRPC server, one game at a time. Accepts deck paths as args for lazy loading.
- **`RlPlayerController`** — Core RL integration. Overrides every decision method (mulligan, spells, targeting, attackers, blockers, sacrifice, discard, etc.). Builds legal actions, queries Python agent via gRPC, executes choice.
- **`ObservationBuilder`** — Flat float array encoding game state (life, board, hand, graveyard, mana, phase/turn).
- **`CardRegistry`** — Card name to integer ID mapping.

## Architecture (Python)

- **`forge_rl/env.py`** — Gymnasium env wrapping one gRPC connection. Action masking via `info["action_mask"]`.
- **`train_ppo.py`** — CleanRL-style PPO with `CategoricalMasked` for action masking. Shared MLP with policy + value heads. `RewardShaping` wrapper adds life delta + board advantage signals.

## Key Design Notes

- **Lazy loading**: `FModel.initialize(null, null, true)` + deck preloading = ~50MB/server instead of ~225MB
- **Heap cap**: `-Xmx128m` prevents swap thrashing (SPS cliff from 50 to 7 without it)
- **Targeting works**: `rlChooseTargets()` uses `DecisionType.CHOOSE_ENTITY` — confirmed via debug logging
- **8GB MacBook Air**: 8 envs is practical max. Server logs at `runs/<name>/server_logs/`

## Current State

- Mono red plateaus at ~73% win rate. Not a targeting issue. Likely needs larger network, better obs, or different algo.
- Caw gates topped out at ~52% with PPO. May need MCTS.
- All training runs were killed before this handoff. No processes running.
