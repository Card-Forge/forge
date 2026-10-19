# Port Log — Game State: losing and winning

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `gameloss.go`, `action.go`
  (`checkStateBasedActions`), `losesgameeffect.go`, `winsgameeffect.go`, `game.go` (`NewGame`)

## Can't lose and can't win

`Player.loseConditionMet(reason)` (`Player.java:1974`) asks the replacement handler whether a `GameLoss` event is
stopped (`ReplaceGameLoss.canReplace`: `ValidPlayer$`, `ValidLoseReason$`) and sets the loss only when it is not;
`Player.cantWin` is the same `cantHappenCheck` over `GameWin`. `Game.loseConditionMet` and `Game.cantWin` port both for
the `Layer$ CantHappen` shape (about 40 corpus lines: Platinum Angel, Abyssal Persecutor, Phyrexian Unlife, Lich's Tomb,
Soul Echo, Transcendence). The host must be in an `ActiveZones$` zone of Battlefield or Command, and a line naming a
param outside the allow-list is skipped (GO-7).

| Caller | Reason |
| --- | --- |
| state-based action, attempted draw from empty library | `Milled` (704.5b) |
| state-based action, life 0 or less | `LifeReachedZero` (704.5a) |
| state-based action, ten poison counters | `Poisoned` (704.5c) |
| `LosesGame` effect | `SpellEffect` |
| `WinsGame` effect, SBA winner check | `cantWin` (`altWinBySpellEffect`, `hasWon`) |

The SBA tries the three conditions in `checkLoseCondition`'s order and stops at the first that takes effect. The last
player left standing still wins when a permanent says they can't win: `checkGameOverCondition` counts players with no
outcome, not winners.

`Game.Concede(pid)` is `Player.concede` ("No cantLose checks - just lose"): `Player.Conceded` marks it, and
`gameEventCantHappen` never applies to a conceded player. The fixture verb is `concede <player>`.

Not ported: the `ReplaceWith$` GameLoss lines (Lich's Mirror, Exquisite Archangel, ...: 7 corpus lines), which need a
controller decision. While one covers the player the loss is not applied and a pending error is recorded;
`Mode$ InfectDamage` (Phyrexian Unlife's second line) is inert, so its life-0 damage-as-poison half does not happen.

## Starting life

`NewGame` gives every seat `startingLife` (20, CR 103.3). Before this a bare `NewGame` started at 0 life and every test
or fixture set 20 itself.

Scenarios: `cant-lose-platinum-angel-...`, `cant-lose-phyrexian-unlife-...` (two), `cant-win-abyssal-persecutor-...`,
`sba-negative-life-mid-resolution-...`, `sba-lifelink-and-damage-simultaneous-survive`,
`sba-draw-more-than-library-draws-all-then-loses`, `sba-poison-eleven-loses`. Go tests: `gameloss_test.go`.
