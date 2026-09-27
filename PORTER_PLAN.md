# Porter plan: ChaosEnsues (ADR-0029, 2 of 4)

Scratch file; deleted in the final commit.

| Step | What                                                                                                 |
| ---- | ---------------------------------------------------------------------------------------------------- |
| 1    | `trigger.go`: `checkChaosEnsuesTriggers` for `Mode$ ChaosEnsues` (`TriggerChaosEnsues.performTest`)  |
| 2    | `chaosensueseffect.go`: Planechase gate, default path, `Defined$` path (zone-widened chaos triggers) |
| 3    | Registry regen, enginelint, tests in `planechase_test.go`-sibling `chaosensues_test.go`              |
| 4    | Docs: `effects-chaosensues.md`, `game-state.md` index row, counts in `CLAUDE.md` and plan            |
| 5    | `gates.sh full`, commit, delete this file                                                            |
