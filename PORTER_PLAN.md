# Porter plan: RunChaos (ADR-0029, 3rd of 4 Planechase APIs)

Scratch file, deleted in the final commit.

| Step | What                                                                                                               |
| ---- | ------------------------------------------------------------------------------------------------------------------ |
| 1    | `runchaoseffect.go`: targets-then-Defined$ cards (deduped), each card's `Mode$ ChaosEnsues` lines, activator-owned |
| 2    | Push via `pushTriggeredAbilities` (no ordering hook, documented gap); no Planechase gate (Java has none)           |
| 3    | Reject: trigger `Cost$`/`TriggerController$`/`Static$`/non-`You` `OptionalDecider$`, missing `Execute$`            |
| 4    | Forge defect row: `RunChaosEffect.java:27,30` sets optional on the wrong SA; `:25` decider never null              |
| 5    | Tests `planar_runchaos_test.go`; docs `effects-runchaos.md`, index row, counts 174 -> 175; gates; commit           |

No new `Game` field, no new controller method, no `trigger.go` edit planned.
