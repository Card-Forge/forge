# Port Log — Game State: target questions fail loudly, the counter placer, the triggering card's LKI (R9f)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `targeting.go` (`targetChoiceFor`,
  `targetChoice.targeter`, `resolveTargets`), `chaintargets.go` (`failLinkTargets`), `effect.go` (`Registry.resolve`'s
  `targetsErr` check), `counteradded.go` (`addCardCounters`' placer, `counterAddedTriggerMatches`), `castrecord.go`
  (`recordCountersAdded`), `putcountereffect.go` (`putCounterPlacer`), `putcounteralleffect.go` (`putCounterAllPlacer`),
  `entercounters.go`, `ability.go` (`triggeredObjects.cardLKIController`/`cardLKIOwner`), `trigger.go` (`withCardLKI`,
  `changesZoneCardObjects`), `defined.go` (`cardController`, `cardOwner`), `exile.go`, `leftto.go`, `delayedtrigger.go`,
  every `addCardCounters` caller
- **Java:** `SpellAbility.java:2141-2164` (`setupTargets`); `Card.java:1761-1832` (`addCounterInternal`, `:1792`
  `addCounterAddedThisTurn`, `:1805` `AbilityKey.Source`); `Game.java:1244-1253`; `TriggerCounterAdded.java:57-98`;
  `TriggerCounterAddedOnce.java:58-94`; `CountersPutEffect.java:583-586`; `CountersPutAllEffect.java:65-92`;
  `PhaseHandler.java:286`; `CardFactoryUtil.java:545-583` (`makeEtbCounter`); `AbilityUtils.java:1013-1076`;
  `TriggerChangesZone.java:190-198`; `GameAction.java:600-612`, `:673`, `:933-953`; `Player.java:2276-2284`;
  `TriggerExiled.java:102-104`; `TriggerSacrificed.java:75-77`

## A target question this port cannot ask fails the ability loudly

`targetChoiceFor` answered "no legal target" (`ok` false) when `targetingPlayerOf` or the
`TargetsWithDefinedController$` `definedPlayers` call errored, so the ability never reached the stack and nothing said
why. `resolveTargets`' own `targetingPlayerOf` call (the R9b finding, `targeting.go:72-75`) ran the same lookup a second
time and its error branch was dead: the drop happened inside `targetChoiceFor` first.

| Piece                     | Does                                                                                                                                                  | Why                                                                                                      |
| ------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| `targetChoiceFor`         | Both errors become `targetChoice.err`: the ability is pushed with no targets (`Ability.targetsErr`) and fails when it resolves                        | GO-7; the deferral `choice.err` already had for a target with no `EntityID`                              |
| `targetChoice.targeter`   | The targeting player is resolved once, while the candidates are filtered; `resolveTargets` and `resolveChainTargets` read it                          | One answer per push; no second lookup that could disagree                                                |
| `Registry.resolve`        | Any ability carrying `targetsErr` fails as `engine: <API>: <err>` after the optional confirm, before a cost is paid; the 5 per-effect checks are gone | Only DealDamage, PreventDamage, PutCounter, ControlSpell and ChangeTargets read it; the rest did nothing |
| `failLinkTargets`         | A `SubAbility$` link's error is recorded on the root too, naming the link (`SubAbility$ DBPump (Pump): ...`); the first failure wins                  | The root fails before its own body acts, not after                                                       |
| Several targeting players | Still an error (`TargetingPlayer$ "Player" names 2 players`)                                                                                          | Java has the activator pick (`chooseSingleEntityForEffect`, `:2153-2154`), a decision this port lacks    |

## `CounterAdded`'s `ValidSource$` is the player who places

`addCardCounters` takes the placer, Java's `Player source` (`Card.java:1761`). It is matched against `ValidSource$` of
`CounterAdded`/`CounterAddedOnce` (`AbilityKey.Source`, `:1805`) through `matchesPlayerSpec`, and it keys the turn's
counter record (`recordCountersAdded`, `Game.java:1244-1253`). No placer matches nothing, as Java's null does. Every
corpus value of the param is `You`. A caller that runs the AddCounter replacements first (`countersReplaced`) gives them
the same player, so the replacement's `ValidSource$` and the trigger's agree:

| Caller                                                                 | Placer                                                                                                | Java                                                                                                                 |
| ---------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `PutCounter`                                                           | activator, or the first player `Placer$` names                                                        | `CountersPutEffect.java:583-586`                                                                                     |
| `PutCounterAll`                                                        | activator; each card's controller/owner for `Placer$ Controller`/`Owner`; else the first player named | `CountersPutAllEffect.java:65-84`                                                                                    |
| Saga, entering                                                         | the Saga's controller                                                                                 | `makeEtbCounter`'s PutCounter activator                                                                              |
| Saga, precombat main                                                   | the turn's player                                                                                     | `PhaseHandler.java:286`                                                                                              |
| `MultiplyCounter`, `MoveCounter`, `Earthbend`, Cumulative upkeep's AGE | activator                                                                                             | `CountersMultiplyEffect.java:43`, `CountersMoveEffect.java:86`, `EarthbendEffect.java:50`, `SacrificeEffect.java:54` |
| `Explore`, `Endure`, `Connive`                                         | the creature's controller                                                                             | `ExploreEffect.java:63`, `EndureEffect.java:63`, `ConniveEffect.java:64`                                             |
| `Amass`, `Blight`                                                      | the amassing / blighting player                                                                       | `AmassEffect.java:52`, `BlightEffect.java:45`                                                                        |
| An `AddCounter` unless cost                                            | the payer                                                                                             | `CostPutCounter.java:182`                                                                                            |
| Entry counters (`applyEnterCounters`)                                  | the pile's placer                                                                                     | the counter table's key                                                                                              |

The placer is matched, never recorded: `TriggerCounterAdded.java:97-98` and `TriggerCounterAddedOnce.java:92-94` keep
only `Card` and `Player` (and `Amount`), so a `Defined$ TriggeredSource` on such a trigger stays the "recorded no
source" error, as Java answers nothing there. A Saga's lore counters now enter the turn's record (Java records
`playerTurn`); before, a placement with no source card was dropped from it.

`Placer$` resolves on both effects (it was refused by name): Alexios, Agitator Ant, Regna's Sanction and Tempt with
Glory play. A `Placer$` naming no player fails before anything is placed (Java's `.get(0)` throws).

## `TriggeredCardController`/`TriggeredCardOwner` read the object the mode stored

Java answers `getController()`/`getOwner()` of the `AbilityKey.Card` object (`AbilityUtils.java:1020-1030`,
`:1053-1059`). Which object that is depends on the mode:

| Mode                                                             | Stored `Card`                                                         | Go                                                              |
| ---------------------------------------------------------------- | --------------------------------------------------------------------- | --------------------------------------------------------------- |
| `ChangesZone`, `Origin$` exactly `Battlefield`                   | the CardLKI (`TriggerChangesZone.java:192-194`)                       | `changesZoneCardObjects` freezes the LKI's controller and owner |
| `ChangesZone`, any other `Origin$`                               | the moved card (`GameAction.java:602`), owner-controlled after `:673` | live read: the card's controller now                            |
| `Exiled` (delayed)                                               | the pre-move object (`GameAction.java:933-942`)                       | frozen                                                          |
| `Sacrificed`                                                     | the LKI copy (`Player.java:2276-2280`)                                | frozen: the sacrificer and the owner                            |
| `Taps`, `Drawn`, `Discarded`, `TapsForMana`, `CounterAdded`, ... | the live card                                                         | live read, unchanged                                            |

The frozen answer is two `PlayerID` fields on `triggeredObjects`, taken as the trigger fires; `g.LKI` is not read at
resolution, since a later battlefield exit overwrites it. A creature that died under a borrowed controller now names the
borrower (Massacre Wurm over a creature stolen from its owner); before, the owner.

## Decisions

| Decision                                                | Why                                                                                                                       |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| Defer the error to resolution, not `recordPendingError` | Targeting already defers `choice.err` that way; the ability is visible on the stack, and `Registry.resolve` names its API |
| New state: `triggeredObjects.cardLKIController`/`Owner` | Two values on a value struct; `Ability` and `Game.Clone` copy them with no allocation                                     |
| No new `PlayerController` method                        | The activator's pick among several targeting players stays unported: an error, not a guess                                |
| `addCardCounters` takes the placer, no new trigger key  | Java reads the placer only in `performTest`                                                                               |

## Forge defects and engine findings

| Where                                                                     | Finding                                                                                                                                                                                                                                                                                                   |
| ------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `SpellAbility.java:2153-2161`                                             | A `TargetingPlayer$` naming nobody: `chooseSingleEntityForEffect` returns null for an empty list (`PlayerControllerHuman.java:585-590`) and `targetingPlayer.getController()` throws. Reported, not reproduced                                                                                            |
| `exploit.go:57` (engine)                                                  | `Mode$ Exploited` records the exploited creature as `card`; Java's `Card` is the exploiter and `Exploited` the LKI (`SacrificeEffect.java:172-174`, `TriggerExploited.java:71`)                                                                                                                           |
| `combatdamage.go:341`, `zonemove.go:67`, `makecardeffect.go:102` (engine) | Wither/infect -1/-1 counters, counters a zone move carries in and `MakeCard`'s `WithCounter$` bypass `addCardCounters`: no `CounterAdded` trigger, no turn record                                                                                                                                         |
| `putcounteralleffect.go`, `multiplycountereffect.go` (engine)             | No `countersReplaced` call: AddCounter replacements (Hardened Scales) do not see these placements                                                                                                                                                                                                         |
| `targeting.go:392`, `:396`, `:418` (engine)                               | An unresolvable `TargetMin$`/`TargetMax$` and a `TargetType$` other than `Spell` still leave the ability off the stack silently                                                                                                                                                                           |
| `Card.Controller` (engine)                                                | A creature stolen by a Control Magic-style static still answers the thief from the graveyard until the layers next run: the `ControlMod` row outlives the move. A dies trigger resolving first reads the thief either way, so only an Act of Treason-style steal (cleared by `Move`) shows the LKI change |
| `targeting.go:1042` (engine)                                              | At fizzle time a chain link's `TargetingPlayer$` that no longer resolves falls back to the controller for `TargetingPlayerControls$`                                                                                                                                                                      |

## Smoke probe

`crucible card-smoke -all -workers 8 -list`, before → after:

| Status       | Before | After  |
| ------------ | ------ | ------ |
| plays        | 30,630 | 30,618 |
| refused      | 2,883  | 2,895  |
| placed       | 372    | 372    |
| inconclusive | 193    | 193    |
| judged play  | 91.4%  | 91.4%  |

| Flip                    | Cards | Which                                                                                                                                                                                                                                                               |
| ----------------------- | ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| plays → refused         | 10    | Resolved a ChangeZone with an `Origin$` naming `Stack` over no target, silently: Brutal Expulsion; Divide by Zero; A-Divide by Zero; Jeskai Revelation; Press the Enemy; Reprieve; Shell of the Last Kappa; Sink into Stupor; Unsubstantiate; Venser, Shaper Savant |
| plays → refused         | 6     | Left their trigger off the stack silently on a `TargetsWithDefinedController$` it cannot read: Firkraag, Gornog, Karazikar, Seifer (`TriggeredAttackedTarget`), Necrotic Plague (`TriggeredCardOpponent`), Power Struggle (`Player.OpponentOf TriggeredPlayer`)     |
| refused → plays         | 4     | `Placer$`: Agitator Ant; Alexios, Deimos of Kosmos; Regna's Sanction; Tempt with Glory                                                                                                                                                                              |
| refused, reason changed | 10    | Aether Gust, Bilbo's Gambit, Commit, Endless Detour, Subtlety, Sudden Setback, Swat Away, Talion's Throneguard now stop at the Stack target; Quicksilver Fountain at `Effect: ForgetCounter$`, Orzhov Advokist at `Effect: RememberObjects$`                        |

Every plays → refused card was a silent miss before: none of them did what its text says. The triggering-card LKI
changes no smoke outcome (the probe never steals a creature); the module tests and a scenario pin it.

## Tests

`targetingerrors_test.go` (several targeting players, an unrecorded `TargetingPlayer$` and
`TargetsWithDefinedController$`, a `ChangeZone` target zone not scanned, a link's error failing the root before it
acts), `targetingrestrictions2_test.go` (`TargetingPlayer$` naming nobody fails at resolution), `counterplacer_test.go`
(`ValidSource$ You` for each placer, `Placer$` on `PutCounter`, `PutCounterAll`'s activator/`Controller`/`Owner`/defined
placer, the unresolvable `Placer$` errors), `triggeredcardlki_test.go` (a stolen creature dying, exiled and sacrificed
names the borrower; an `Origin$`-less line names the owner). Scenarios
`trigger-dies-massacre-wurm-drains-the-stolen-creatures-last-controller`,
`trigger-counter-added-sigurd-sees-the-turn-based-lore-counter-its-controller-puts`,
`trigger-counter-added-alexios-placer-is-the-upkeep-player`.
