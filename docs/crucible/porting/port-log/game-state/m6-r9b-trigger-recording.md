# Trigger modes record their triggering objects (R9b)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `trigger.go` (the per-mode `check...Triggers`,
  `damageDoneOnceObjects`), `delayedtrigger.go`, `triggerscan.go`, `exile.go`, `defined.go` (`TriggeredCardOwner`),
  `ability.go` (`triggeredObjects` field comments)
- **Java:** each `Trigger<Mode>.setTriggeringObjects` (`forge/game/trigger/`): `TriggerPhase` (`AbilityKey.Player`),
  `PhaseHandler.java:438` (`mapFromPlayer(playerTurn)`); `TriggerDrawn.java:113`; `TriggerTaps.java:94`;
  `TriggerUntaps.java:67`; `TriggerSacrificed.java:76`; `TriggerDamageDealtOnce.java:94-101`;
  `TriggerDamageDoneOnce.java:57-71`; `CardDamageTable.java:59-94`; `TriggerAttackerUnblockedOnce.java:68-70`,
  `Combat.java:698-701`; `TriggerBlocks.java:70-72`; `TriggerLandPlayed.java:57-59`; `TriggerBecomesTarget.java:83-86`;
  `TriggerChangesZoneAll.java:57-63`; `TriggerChangesZone.java:190-198`; `TriggerExiled.java:102-104`;
  `AbilityUtils.getDefinedPlayers` `:1013-1076` (the `Triggered...` branch, `...Owner` at `:1053-1059`)

## What refused

`card-smoke -all` refused 161 cards on `Defined$ Triggered<Key>` naming a key the trigger mode never set ("the trigger
recorded no player/card/target/source/attacking player") and 10 more on a `Triggered<Key>$` amount over the same gap. By
the key and the mode of the trigger that put the failing ability on the stack (card `T:` lines and the SVar triggers of
emblems, effects and `AddTrigger$` grants alike):

| Key missing | Cards | Modes |
| --- | --- | --- |
| `Player` (`TriggeredPlayer`) | 129 | `Phase` (all but a handful), `Drawn` (Underworld Dreams, Fate Unraveler, Razorkin Needlehead, Ob Nixilis) |
| `Card` (`TriggeredCard...`) | 20 | `Taps` 10, `Drawn` 4, `Sacrificed` 3, delayed `ChangesZone` 2 (Burn Away, Searing Blood), `Untaps` 1 |
| `Target` (`TriggeredTarget`) | 5 | `DamageDoneOnce` |
| `Source` (`TriggeredSource...`) | 4 | `DamageDealtOnce` |
| `AttackingPlayer` | 3 | `DamageDoneOnce` (Contested Game Ball, Norn's Decree), `AttackerUnblockedOnce` (Coveted Jewel) |
| amount `TriggeredPlayer$` | 9 | `Phase`: `CardsInHand`, `LifeTotal`, Power Surge's `NumPowerSurgeLands` |
| amount `TriggeredTarget$LifeTotal` | 1 | `DamageDoneOnce` (Grievous Wound) |

## What landed

Each mode now records what Java's `setTriggeringObjects` keeps, read by `definedPlayers`/`definedCards` and the
`Triggered<Key>$` amount heads through the one `Ability.triggered` struct (no new reader):

| Mode | Recorded | Notes |
| --- | --- | --- |
| `Phase` | `player`: the active player | Not the trigger's controller: `PhaseHandler` passes `playerTurn`. Delayed `Phase` lines the same |
| `Drawn` | `card`: the drawn card; `player`: the drawer | |
| `Taps`, `Untaps` | `card` | |
| `Sacrificed` | `card` | The `CardID`; Java's `Card` is the battlefield LKI |
| `DamageDealtOnce` | `source`, `sourceController` as the trigger fires | `damageSourceObjects`' reading; `Targets` (a list) not recorded |
| `DamageDoneOnce` | `target`; `attackingPlayer` when the sources share one ctrl | See decision below; `Sources` (a list) not recorded |
| `AttackerUnblockedOnce` | `attackingPlayer`: the active player | `Defenders` (a list) not recorded |
| `Blocks` | `blocker` | `Attackers` (a list) not recorded |
| `LandPlayed` | `card` | |
| `BecomesTarget` | `target` | `Source`/`SourceSA` not recorded: the check gets no source card |
| `ChangesZoneAll` | `cards`: the batch the filter kept; `Amount`: their count | `TriggerCount$Amount`, `Defined$ TriggeredCards` |
| `ChangesZone` to Exile | `card` (own and watcher lines) | As the graveyard path already did |
| delayed `ChangesZone`/`Exiled` | `card`: the card that left | `delayedLeftBattlefieldMatches` |
| `Defined$ TriggeredCardOwner` | reader: the recorded card's owner | Java's `...Owner` branch; an unset card is an error, as for every `Triggered` key (Java: no player) |

A key a mode still does not record stays the loud "the trigger recorded no ..." error (GO-7); no reader answers an empty
list for an unset key.

## Decisions

| Decision | Why |
| --- | --- |
| `DamageDoneOnce`'s `AttackingPlayer` only with one source controller | Java takes the controller of the first entry of `Maps.newHashMap(...)` (`CardDamageTable.java:90`, `TriggerDamageDoneOnce.java:67-70`): hash order, not a game order. With one controller (every two-player combat hit on a player, every single-source effect) the answer is determined; with several it is not, so the key stays unrecorded |
| `Abandoned`'s `Scheme` not recorded | No reader names it (`Defined$ TriggeredScheme` has no case); recording it would be untested state |
| One `CardID` for Java's `Card` (LKI) and `NewCard` | The existing `triggeredObjects.card` convention (`definedCards`' `TriggeredCard...` case): a `CardID` is stable across the move. The LKI's controller and owner ride beside it (`cardLKIController`, [R9f](m6-r9f-trigger-findings.md)) |
| No new engine state, no new `PlayerController` method, no Clone change | Every key lands in fields `triggeredObjects` already had; `Ability` copies it by value |

## Forge defects and engine findings

| Where | Finding |
| --- | --- |
| `TriggerDamageDoneOnce.java:67-70` | `AttackingPlayer` is "the first" source's controller of a `HashMap`: undetermined when the damaged entity was hit by several controllers' sources in one table (multiplayer combat, an "each creature deals damage" effect). Reported, not reproduced (decision above) |

## Smoke probe

`crucible card-smoke -all -workers 8`, before → after:

| Status | Before | After |
| --- | --- | --- |
| plays | 30,347 | 30,513 |
| refused | 3,168 | 2,999 |
| placed | 369 | 372 |
| inconclusive | 194 | 194 |
| judged | 33,515 | 33,512 |
| judged play | 90.5% | 91.1% |

167 refused → plays: 154 of the 161 `Defined$` cards, the 10 amount cards, and 3 that refused on the same gap under a
`Defined$ "_" not resolvable yet` message (Guilty Conscience's `TriggeredSourceLKICopy` from `DamageDealtOnce`;
Urabrask, Heretic Praetor and Who's That Praetor? from `Phase`). 3 refused → placed (Eladamri, Lord of Leaves Avatar;
Hold the Perimeter; The Dining Car). No "the trigger recorded no ..." refusal remains. Four stay refused at a later
stage, one played and now refuses:

| Card | Now refused on |
| --- | --- |
| Alexios, Deimos of Kosmos | `PutCounter: Placer$` (`Placer$ TriggeredPlayer`; finding above) |
| Risky Move | `GainControl: NewController$ Player.Chosen` |
| Sands of Time | `TapAll: TapperController$` |
| Tangle Wire | `Tap: Tapper$` |
| Quicksilver Fountain | plays → refused, `PutCounter: Placer$`; was a silent drop at `TargetingPlayer$` |

## Tests

`triggeredobjects_test.go` (`package engine_test`): one test per recorded key, each arranged so the recorded object
differs from the trigger's controller — `Phase` on the opponent's upkeep (`Defined$` and `TriggeredPlayer$CardsInHand`),
a delayed `Phase` line, `Drawn` (`TriggeredPlayer`, `TriggeredCardOwner`), `Taps`/`Untaps`, `Sacrificed`,
`DamageDoneOnce` (`TriggeredTarget`, `TriggeredAttackingPlayer`), `DamageDealtOnce`, `AttackerUnblockedOnce`, `Blocks`,
`LandPlayed`, `BecomesTarget`, `ChangesZoneAll` (`TriggerCount$Amount`), the Exile path of `ChangesZone`, and the
unset-key error for `TriggeredCardOwner`. Scenarios:
`trigger-phase-sulfuric-vortex-damages-the-player-whose-upkeep-it-is`,
`trigger-drawn-underworld-dreams-damages-the-opponent-who-draws`,
`trigger-delayed-searing-blood-burns-the-dead-creatures-controller`.
