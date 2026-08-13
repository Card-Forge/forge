# Forge headless bridge

The `bridge` mode exposes a Forge AI seat over newline-delimited JSON-RPC 2.0. Standard output is reserved for protocol messages; diagnostics go to standard error. The `--log` file records both protocol directions as JSONL.

```sh
./headless.sh bridge -d decks/simple_bolt.dck decks/simple_bolt.dck \
  --seat 1 --seed 42 --log /tmp/forge-bridge.jsonl
```

The Java process initiates `hello`. Its controller then accepts `game_start`, `decision`, and `shutdown` requests plus protocol event notifications. Task B creates a deterministic mirrored-game shell and delegates priority and mulligan choices to Forge's real `PlayerControllerAi`. Applying arbitrary opponent actions to Forge's full rules engine is the explicit integration boundary for the next bridge phase.
