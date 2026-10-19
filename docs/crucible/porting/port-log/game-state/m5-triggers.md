# Port Log — Game State: M5 Trigger Batch B

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)

Four trigger gaps closed: `ChangesController` past the plain card trigger, Command-zone schemes as trait hosts, the
`Triggered<Key>.<filter>` player form, and four more modes (`Scry`, `Surveil`, `Transformed`, `TurnFaceUp`).

## `ChangesController`: `TriggerController$`, delayed mode, runChangeControllerCommands

| Java | Go |
| --- | --- |
| `TriggerHandler.java:494-497` `TriggerController$` sets activating player | `checkChangesControllerTriggers` (`trigger.go`) resolves it, first player wins |
| `TriggerChangesController.setTriggeringObjects`: `Card`, `OriginalController` | `triggeredObjects.card` + `.originalController` (`ability.go`) |
| `AbilityUtils.java:1022-1027` `TriggeredOriginalController` is the player | `definedPlayers` case `TriggeredOriginalController` (`defined.go`) |
| `DelayedTriggerEffect` with `Mode$ ChangesController` (4 real lines) | `delayedTriggerEffect` accepts the mode; `delayedChangesControllerMatches` fires it |
| `ThisTurn$` on the delayed line | existing `delayedTrigger.ThisTurn` (`delayedTriggersOnNextTurn`), no new code |
| `GameAction.java:1008-1022` unpair, `runChangeControllerCommands` | [`m5-control.md`](m5-control.md) |

`TriggerController$` and `ThisTurn$` never appear on a `T:` card line together: `TriggerController$` is Khârn the
Betrayer's (`TriggeredOriginalController`, the player who lost control draws), `ThisTurn$` is Stolen Uniform's delayed
line. A `T:` line naming `ThisTurn$` still skips. A `TriggerController$` Defined that fails to resolve skips the line
(GO-7).

Delayed trigger semantics (`TriggerChangesController.performTest` run against the registered trigger):

- `ValidCard$ Card.IsTriggerRemembered`: card is in `RememberObjects$` (Ray of Command, Magus of the Unseen, Stolen
  Uniform).
- Any other `ValidCard$` is a valid string against the delayed trigger's host (`Card.StrictlySelf`, Seraph and Krovikan
  Vampire). `StrictlySelf` now reads as `Self` (identity), same simplification `StrictlyOther` already carries
  (`valid.go`).
- `ValidOriginalController$` is a player spec relative to the trigger's controller.
- The trigger fires once and is removed; the card is recorded as `TriggeredCard`.
- `IsPresent$` (Stolen Uniform, `Card.IsTriggerRemembered+AttachedTo ...`) is read by `delayedPresentMatches`, see
  [`m5-control.md`](m5-control.md), which also covers `LoseControl$`, the change-controller command lists and Soulbond
  unpairing.

## Command-zone schemes are trait hosts

`traitHosts` (`game.go`) walked battlefield permanents plus Command-zone effect cards. It now also walks Command-zone
cards of type Scheme (`Card.isCommandTraitHost`): all 21 ongoing schemes carry `EffectZone$ Command`/
`TriggerZones$ Command` on every printed line, so no per-caller zone gate is needed. Planes and phenomena are not
walked: every planar mode has its own `planeswalkTriggerZones` walk, and widening would double-fire them.

Reached through this: `Attacks`, `ChangesZone`, `SpellCast`, `Abandoned`, `DamageDoneOnce`-family triggers and
`Continuous` (`layerHostZoneActive` honours `EffectZone$ Command`), `CantBeCast`, `ReduceCost`, `UntapOtherPlayer`
statics. `Mode$ Phase` and `SetInMotion` walk Command directly and are unchanged.

Still unreached: an ongoing scheme's `R:` replacement with `ActiveZones$ Command` (Nothing Can Stop Me Now); the
replacement walks are not `traitHosts` callers.

## `Triggered<Key>.<filter>` players

`AbilityUtils.getDefinedPlayers` splits `Defined$` at the first `.`; for a `Triggered...` base the tail is a comma list
of restrictions, each prefixed `Player.` (`AbilityUtils.java:1186-1196`). `definedPlayers` now does the same
(`triggeredPlayersFiltered`, `defined.go`). Three supporting pieces:

- `TriggeredAttackingPlayer` (`AbilityKey.AttackingPlayer`, `TriggerAttackersDeclared.java:98`): new
  `triggeredObjects.attackingPlayer`, recorded by both AttackersDeclared modes. Before this the key was unrecorded, so
  the bare form errored too.
- `matchesPlayerSpec` splits an alternative's property on `+` (`GameObject` `incR[1].split("\\+")`) through
  `matchesPlayerProperties`.
- `PlayerProperty.java:297-310` `controls<Type>[_<cmp><n>]` (`controlsMatches`): battlefield cards matching the valid
  string, default at least one; a non-literal comparator operand is unrecognized.
- Card property `attacking <Defined>` (`CardProperty.java:1529-1535`): the attacker's defender must satisfy the player
  spec. Only a player defender against a player spec is read: `valid` cannot depend on `defined` (enginelint cycle) and
  no real line names a card there.

Curse of Vitality, Curse of Chains-style lines:
`TriggeredAttackingPlayer.Opponent+controlsCreature.attacking Player.EnchantedBy` keeps the attacker only while it is an
opponent of the ability controller with a creature attacking the enchanted player.

## Scry, Surveil, Transformed, TurnFaceUp

`triggerpack.go`, through `scanTriggers` (`triggerscan.go`). Corpus lines: `TurnFaceUp` 126, `Transformed` 35, `Scry`
24, `Surveil` 16.

| Mode | Fires from | Params evaluated | Lines left unfired |
| --- | --- | --- | --- |
| `Scry` | `scryEffect` per scrying player (`GameAction.java:2655`) | `ValidPlayer$` | `ToBottom$` (1) |
| `Surveil` | `surveilEffect` per player (`Player.java:1088`) | `ValidPlayer$` | `FirstTime$` (1) |
| `Transformed` | `SetState` `Mode$ Transform`, after `Game.transform` | `ValidCard$` | none |
| `TurnFaceUp` | `SetState` `Mode$ TurnFaceUp` | `ValidCard$` | `ValidCause$` (2) |

A line naming any param outside the table's evaluated set plus the general ones (`effectEventTriggerParams`) is not
fired (GO-7). Scry's `ScryNum`/`ScryBottom` triggering counts are not recorded (Elvish Mariner's `X`, Celeborn's `X`
stay unresolved). Face-down casting (morph) and `Card.turnFaceUp` from play are not ported, so `SetState` is the only
real `TurnFaceUp` site. `Surveil` fires even when the library is empty (`Player.java:1083` is outside the surveil
block); `Scry` does not (`n == 0` skips the player, a deviation recorded here: Java gates only on `cause != null`).

## Tests

Fixtures (`testdata/scenarios/`): `trigger-ongoing-scheme-*` (2), `trigger-curse-of-vitality-*` (2), `trigger-kharn-*`
(2), `trigger-scry-*` (2). New actions verbs `queue scry`/`queue surveil` (`fixture/actions.go`). Module tests:
`triggerdefined_test.go` (3-player Curse of Vitality), `triggerchangescontroller_test.go` (delayed mode),
`triggerpack_test.go` (the four modes).
