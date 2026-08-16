# Forge headless bridge

The `bridge` command runs one Forge AI player under an external game
coordinator. It is intended to be launched by software such as DeepScry rather
than used as an interactive game.

The coordinator sends newline-delimited JSON-RPC 2.0 requests and public game
events on standard input. Standard output is reserved for Forge's JSON-RPC
responses and decisions, while diagnostics go to standard error. The `--log`
file records both protocol directions as JSON Lines.

```sh
./headless.sh bridge -d decks/simple_bolt.dck decks/simple_bolt.dck \
  --seat 1 --seed 42 --log /tmp/forge-bridge.jsonl
```

Run `./headless.sh bridge --help` for the complete command-line contract. Deck
files are ordered by seat, and `--seat` selects the one-based seat controlled
by Forge AI. The seed initializes Forge's current shared random-number stream;
the bridge protocol will separate controller and mirrored-engine randomness in
a subsequent protocol version.

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

Targeted remote casts are split across the same callback boundary used by the
engines. An `announce_cast` action selects and reserves the legal Forge spell;
a later `choose` action with `choice_kind: "spell_targets"` supplies player or
battlefield targets. `BridgeController` validates the referenced card and
target count, applies the targets, and only then delegates cost payment and
casting to Forge. Untargeted casts remain one complete `cast` action.
