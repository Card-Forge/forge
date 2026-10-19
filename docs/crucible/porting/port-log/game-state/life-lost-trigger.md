# Port Log — Game State: Mode$ LifeLost trigger

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `lifelost.go` (`noteLifeLost`), `player.go`
  (`LifeLostThisTurn`)

Java: `GameAction`/`Player.loseLife` → `TriggerType.LifeLost`; `Player.lifeLostThisTurn` backs `FirstTime$`.

`noteLifeLost(controller, pid, amount)` runs after life actually dropped, so a `CantLoseLife` prevention or a zero
amount fires nothing. It adds to `Player.LifeLostThisTurn` (reset in `turn.go` at the start of each turn), then fires
every `Mode$ LifeLost` trigger whose `ValidPlayer$`, `FirstTime$` and `LifeAmount$` hold. One damage or LoseLife event
is one trigger firing, not one per point.

| Caller | Source of the loss |
| --- | --- |
| `loselifeeffect.go` | `LoseLife` effect |
| `combatdamage.go` | combat damage to player |
| `activateability.go` (`PayLife` cost) | life paid as a cost |

Not ported: `ValidCause$` and `ResolvedLimit$` on a `LifeLost` line (skipped, not applied); `LifeLostAll` (the batch
mode); loss from non-combat damage paths other than the ones above. Test: `lifelost_test.go` (Vengeful Warchief gets one
counter after losing 2 then 3 life in one turn).
