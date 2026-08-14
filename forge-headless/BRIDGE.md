# Forge headless bridge

The `bridge` mode exposes a Forge AI seat over newline-delimited JSON-RPC 2.0. Standard output is reserved for protocol messages; diagnostics go to standard error. The `--log` file records both protocol directions as JSONL.

```sh
./headless.sh bridge -d decks/simple_bolt.dck decks/simple_bolt.dck \
  --seat 1 --seed 42 --log /tmp/forge-bridge.jsonl
```

The Java process initiates `hello`. Its controller then accepts `game_start`,
`decision`, and `shutdown` requests plus protocol event notifications. Full-game
mode runs Forge's real game loop: the Forge seat delegates main-phase actions
and combat declarations to `PlayerControllerAi`, while the remote seat exactly
replays matched actions from the authoritative DeepScry game.

The paired `test_decks/simple_multi_blocker.dck` coverage scenario uses Hill
Giant and Gray Ogre to reach Forge's multi-blocker combat-damage callback. The
bridge fails the scenario if that callback is not observed. DeepScry's harness
also verifies its two corresponding SMART damage-assignment sub-choices. The
scenario keeps one Hill Giant per battlefield because occurrence-index card
references are not stable object identities after multiple same-name objects
change zones.
