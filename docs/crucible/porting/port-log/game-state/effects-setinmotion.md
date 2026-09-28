# Effects: SetInMotion lands

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `setinmotioneffect.go` (effect, game action,
  trigger walk, Main1 action, scheme SBA), `ability.go` (`triggeredObjects.scheme`), `turn.go` (`beginStep`'s `Main1`
  case), `action.go` (`checkStateBasedActions` calls `returnFinishedSchemes`)

Batch file for `SetInMotion`, CR 904 Archenemy. Supersedes `SetInMotion`'s deferred row in `effects-batch-c.md` (closed
file, left as written). No ADR: every piece mirrors one already built — `Abandon`'s zone move reversed
(`effects-abandon-switchblock-choosesector.md`), the `Mode$ Planeswalked*` Command-zone walk (`effects-planeswalk.md`),
the dungeon SBA (`completeFinishedDungeons`).

---

## SetInMotion lands, with Archenemy's Main1 action and scheme SBA

Ported from `forge-game/src/main/java/forge/game/ability/effects/SetInMotionEffect.java`'s `resolve`, with
`Player.setSchemeInMotion` (`Player.java:275-292`) as `Game.setSchemeInMotion`.

| Step           | Go                                                                                         | Java                                                                         |
| -------------- | ------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------- |
| Condition gate | `definedPresentConditionMet` (below)                                                       | `SpellAbilityCondition.areMet`                                               |
| Player         | host's controller                                                                          | `source.getController()` (`:18`), not the activator                          |
| Repeats        | `RepeatNum$` through `optionalAmount`, default 1                                           | `calculateAmount(RepeatNum)` (`:21-24`)                                      |
| Which scheme   | top of player's `SchemeDeck`, or with `Again$` the triggering scheme (`AbilityKey.Scheme`) | `getZone(SchemeDeck).get(0)` / root ability's triggering `Scheme` (`:27-31`) |
| Replacement    | any live `Event$ SetInMotion` replacement → error, nothing moved                           | `ReplacementType.SetInMotion` run (`Player.java:279-281`)                    |
| Move           | scheme → its owner's `Command` (`Move` fires no `ChangesZone` trigger)                     | `moveToCommand`, `ChangesZone` suppressed (`GameAction.java:942-947`)        |
| Trigger        | `checkSetInMotionTriggers(scheme)`; a `Static$` trigger's error returned                   | `runTrigger(TriggerType.SetInMotion, Scheme)` (`Player.java:289-291`)        |

Top of a `SchemeDeck` is index 0 and `Game.Move` appends to the bottom, the `PlanarDeck`/`Library` convention.

**No Archenemy variant gate.** `SetInMotionEffect.java` checks none, unlike `PlaneswalkEffect.java:23`'s
`getActivePlanes() == null`. The only gate anywhere is the Main1 action's `Player.isArchenemy`, which is
`getZone(SchemeDeck).size() > 0` (`Player.java:271-273`) — zone state the engine already has. No `Game` field, nothing
new for `Clone`.

**Errors (GO-7).** Empty `SchemeDeck` on the top-card path (Java's `get(0)` throws; checked on every `RepeatNum$`
repeat). `Again$` with no recorded triggering scheme (Java would dereference null). Unresolvable `RepeatNum$`.

### Rejected: a live `Event$ SetInMotion` replacement

`ReplaceSetInMotion.java` is not ported. Its presence alone refuses the action — `planeswalkReplacement`'s shape
(`effects-planeswalk.md`) — rather than setting a scheme in motion as if it were absent. Its two sources:

| Source                                                                                               | Why not ported                                                                                                         |
| ---------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| Plots That Span Centuries' effect card (`PlotPower`, `ReplaceWith$ PlotRep`)                         | `PlotRep` chains `SubAbility$ ExilePlot`; `runReplaceWith` (`replacement.go`) refuses a chained `ReplaceWith$`         |
| `AddTurnEffect.createCantSetSchemesInMotionEffect` (`AddTurnEffect.java:69-83`, `Layer$ CantHappen`) | Unreachable: `addturneffect.go` already rejects `NoSchemes$`, the only param that creates it (`PhaseHandler.java:888`) |

### `ConditionDefined$`: `definedPresentConditionMet`

My Laughter Echoes' `DB$ SetInMotion | Again$ True | ConditionDefined$ Remembered | ConditionPresent$ Card` needs
`ConditionDefined$`. `subAbilityConditionMet`'s `isPresentMatches` (`trigger.go`) treats any `ConditionDefined$` as
never met, silently; every other M6 effect rejects the key. Evaluated in the effect file instead, ported from
`SpellAbilityCondition.java:348-373`'s `getPresentDefined` branch: count `ConditionPresent$` matches among the
`ConditionDefined$` objects (`definedCards`, cards only), compare against `ConditionCompare$` (default `GE1`). Any other
`Condition*` key beside `ConditionDefined$` is refused rather than half-checked; without `ConditionDefined$` it is
`subAbilityConditionMet` unchanged. Kept local, not in `isPresentMatches`: that function's fail-closed answer is what
every other effect's rejection list is written against. Reusable by any effect that wants the same shape.

### Main1 turn-based action

`archenemyMain1`, called from `beginStep`'s `Main1` case before phase triggers: `PhaseHandler.java:278-280`, CR 904.4
("at the beginning of the archenemy's precombat main phase, that player sets the top card of their scheme deck in
motion"). Gated on the active player's `SchemeDeck` being non-empty. Runs in both driven and bookkeeping mode, like
`drawStep`; bookkeeping `beginStep` has no error path, so a failure goes to `recordPendingError` (`Game.Step` takes it).

### Scheme state-based action

`returnFinishedSchemes`, next to `completeFinishedDungeons` in `checkStateBasedActions`: `GameAction.java:1745-1752`, CR
704.6f. A `Scheme` without the `Ongoing` supertype in any `Command` zone, not the source of a stack item
(`hasSourceOnStack`), goes to the bottom of its owner's `SchemeDeck`. Not counted as a performed SBA: Java leaves
`checkAgain` unset for Command-zone cards (`GameAction.java:1436-1440`), and `completeFinishedDungeons` does the same.

Without the Main1 action nothing in a driven game sets a scheme in motion, so the trigger mode below would be
unreachable; without the SBA, Main1 drains the deck and piles finished schemes up in Command. Both land here for that
reason.

### Corpus: 1 of 2 real `DB$ SetInMotion` lines resolves

| Line                                                                                                | Status                                                                                                                            |
| --------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `my_laughter_echoes.txt:6`, `Again$ True \| ConditionDefined$ Remembered \| ConditionPresent$ Card` | resolves (`TestMyLaughterEchoesSetsTheSchemeInMotionAgain`)                                                                       |
| `plots_that_span_centuries.txt:7`, `RepeatNum$ 3`, the `ReplaceWith$` of `PlotPower`                | `RepeatNum$` resolves in the effect (`TestSetInMotionRepeatNum`); the line itself is reached only through the refused replacement |

## Mode$ SetInMotion lands

`checkSetInMotionTriggers`/`setInMotionTriggerMatches` (`setinmotioneffect.go`, kept out of `trigger.go` so parallel
trigger-mode ports do not collide there). Ported from `TriggerSetInMotion.performTest`: `ValidCard$` against the scheme,
nothing else.

- **Hosts:** every player's `Battlefield` and whole `Command` zone (`planeswalkTriggerZones`). A face-up scheme is a
  plain `Command` card, not an effect card, so `traitHosts` misses it. `TriggerZones$` gates each line
  (`phaseTriggerZoneMatches`); all 85 real lines name `Command`, so a scheme still in its `SchemeDeck` never fires.
- **Triggering object:** `AbilityKey.Scheme` (`setTriggeringObjectsFrom(runParams, AbilityKey.Scheme)`) recorded as
  `triggeredObjects.scheme`, read by `Again$`. `resolveSubAbility` copies `triggered` down the chain, so a sub-ability's
  own is the root's (`sa.getRootAbility()`). A value field; `Clone` copies it with the stack.
- **`Static$ True`** (0 real lines): resolved inline through `resolveStaticTriggers` (ADR-0020) before the other matches
  are pushed, as `checkPlaneswalkTriggers` does.
- **Shared gates:** `OptionalDecider$ You` (4 lines) and `CheckSVar$`/`SVarCompare$` (1) through `triggerEffectAPI`.

Corpus: 85 real `Mode$ SetInMotion` lines on 84 cards (81 `Scheme`, 3 `Ongoing Scheme`). `ValidCard$ Card.Self` 82, none
2, `!Ongoing` 1 (My Laughter Echoes). All reach this walk; their `Execute$` bodies resolve or fail by their own APIs.

### Not ported

| Gap                                                                                        | Why                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| ------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| CR 904.6 archenemy goes first (`GameAction.java:2395-2401`), scheme deck shuffled at start | Match setup, not an effect; Crucible has no match setup. Tests seat the `SchemeDeck` with `NewCard`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| A face-up scheme's other traits                                                            | `Mode$ Phase` fires from `Command` (`phaseTriggerZones`, `trigger.go:1861`; 17 real lines on Ongoing schemes, `TestOngoingSchemePhaseTriggerFiresFromCommand`). Every walk built on `traitHosts` (battlefield plus effect cards) skips a plain `Command` scheme: its other trigger modes (`Attacks`, `AttackersDeclared`, `ChangesZone`, `SpellCast`, `BecomesTarget`, `DamageDoneOnce`, `AttackerUnblocked`, `Abandoned` on `Card.Self` — 14 real lines on Ongoing schemes) and its `S:` static abilities (`Continuous` 5, `CantBeCast`, `ReduceCost`, `UntapOtherPlayer`) do not apply yet. Newly reachable now that Main1 puts Ongoing schemes face up; widening `traitHosts` to face-up schemes touches every one of its ~35 callers, left for its own change |
| `ReplacementType.SetInMotion`                                                              | Above; fail-closed                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Scenario fixture                                                                           | `GameState.java`'s `ZONES` (`GameState.java:46-54`) has no `SchemeDeck` key; a Crucible-only key would break the Java oracle's reading of the same fixture. Module tests instead (`archenemy_test.go`), the planeswalk precedent                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |

### Forge defects found (PORT-8)

`forge-gui/res/cardsfolder/t/tooth_claw_and_tail.txt:4` and
`forge-gui/res/cardsfolder/y/your_will_is_not_your_own.txt:4` —
`T:Mode$ SetInMotion | Execute$ DarkEffect | TriggerZones$ Command` with no `ValidCard$ Card.Self`, though both read
"When you set this scheme in motion". `TriggerSetInMotion.performTest` passes a missing `ValidCard$`, so each fires for
any scheme set in motion while it is still face up (a `RepeatNum$` resolution sets several in motion with no state-based
check between). Crucible fires them the same way; not compensated. Rows in `card-script-defects.md`.

Tests: `archenemy_test.go`.
