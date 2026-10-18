# Port Log — Game State: battle defenders, trigger batches for damage and zone changes, ChangeZone into combat, loud CheckSVar (R9l)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `combat.go` (`defenderPlayerOf`,
  `defenderPlayerByAttacker`), `attack.go` (`defenderOf`, `eligibleAttackTargets`), `cantattack.go`, `valid.go`
  (`DefenderCtrl*`, `attackingSuffixMatches`), `effect.go` (`resolveEffectBatched`), `combatdamage.go`, `action.go`,
  `trigger.go`, `changezoneeffect.go` (`changeZoneJoinCombat`, `changeZoneLeftCombat`), `changecombatantseffect.go`
  (`addToCombatAttackingAs`, `addToCombatBlockingAs`), `pumpeffect.go` (`pumpMultiplier`), `presentcheck.go`,
  `amountheads.go`, `phase.go` (`phaseConstNames`), `continuous.go`, `defined.go`
- **Java:** `Combat.java:289-306`, `:452-463`, `:956-970`; `CombatUtil.java:58-76`, `:515-538`;
  `PhaseHandler.java:656-675`; `MagicStack.java:599`, `:855-880`; `TriggerHandler.java:250-269`;
  `SpellAbilityEffect.java:758-831`; `ChangeZoneEffect.java:675`, `:703`, `:1317-1330`, `:1391`, `:1400`;
  `PumpEffect.java:305-310`, `:468-480`, `:489-496`; `CardTraitBase.java:470-488`; `AbilityUtils.java:2511-2528`;
  `PhaseType.java:89-127`; `CardProperty.java:198-213`, `:1506-1549`; `Card.java:7066-7068`

## A battle is defended by its protector

| Piece                                       | Was                                | Now (Java)                                                                                    |
| ------------------------------------------- | ---------------------------------- | --------------------------------------------------------------------------------------------- |
| `defenderOf` (blockers, `DefendingPlayer`)  | battle's controller                | protector, `Combat.getDefenderPlayerByAttacker` (`Combat.java:297-299`)                       |
| `eligibleAttackTargets`                     | battles opponents control          | battles whose protector is an opponent, wherever they sit (`CombatUtil.java:66-73`)           |
| `DefenderCtrl`                              | battle's controller                | `getDefendingPlayerRelatedTo` → protector (`CardProperty.java:198-213`)                       |
| `cantattack.go`, `defenderPlayerByAttacker` | own copies                         | one helper, `defenderPlayerOf` in `combat.go` (valid may reference combat, not attack)        |
| Battle with no protector                    | `g.Card(NoCard)` panic on a lookup | not attackable (Java's `getProtectingPlayer().isOpponentOf` would throw); `NoPlayer` defender |

A Siege its caster controls is attacked by that caster; `combat-attack-and-damage-a-battle` attacked a battle the
attacker itself protected (Java-illegal) and is rewritten to the legal shape. `TestAssignBattleProtectorNotAskedWhile…`
needed an unprotected battle under attack: now three players, the protector leaves the game.

The battle scenario exposed a second bug: `gatherStatics` read every face, so an untransformed Invasion of Belenon
pumped its controller's creatures with its back face's Belenon War Anthem. Statics now read `liveFaces` only, as
triggers already did (`Card.getStaticAbilities` reads `currentState`, `Card.java:7066-7068`).

## One APNAP pass per damage step, effect and SBA death batch

R9j's batch (`beginTriggerBatch`) now also wraps:

| Event                                                         | Modes in one pass                                                    |
| ------------------------------------------------------------- | -------------------------------------------------------------------- |
| `dealCombatDamageStep`                                        | DamageDone per exchange, ExcessDamage\*, DamageDoneOnce\*, DamageAll |
| one effect body (`resolveEffectBatched`, every Registry path) | each moved card's ChangesZone, then ChangesZoneAll, damage, …        |
| `destroyLethalToughness`, `destroyDamagedCreatures`           | dies triggers, then ChangesZoneAll                                   |

Java holds every trigger of a resolving ability on a frozen stack (`TriggerHandler.java:264`, `MagicStack.java:599`) and
orders the whole list per player (`:855-880`). No existing scenario moved. Remaining divergence: Java's list also spans
the SBA deaths that follow combat damage; here they are the next pass (R9j's per-event decision).

## ChangeZone `Attacking$` / `Blocking$`

`addToCombat` (`SpellAbilityEffect.java:758-831`) via `addToCombatAttackingAs`/`addToCombatBlockingAs`, with the
controller the card enters under (`GainControl$` first, `ChangeZoneEffect.java:1322-1330`). Joined before the move on
both paths: Java's search does so (`:1391` vs `:1400`); its known-origin move joins after `moveTo` (`:703`), but its
zone-change triggers wait until the effect ends, so "enters attacking" watchers (Arni Metalbrow) see it attacking either
way. This port tests them as the card moves, so joining first gives the same answer; the defender is asked before the
move instead of after. A move that leaves the card off the battlefield takes it out of combat again. `TriggeredDefender`
(Kaalia) and `DefendingPlayer` (Zara) resolve since R9n ([`m6-r9n-checksvar-heads.md`](m6-r9n-checksvar-heads.md)).

## Smaller pieces

| Piece                                                      | Does                                                                                                                            |
| ---------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| Pump `NumAtt$`/`NumDef$` `Double`/`Triple`                 | +stat ×1/×2 per target (`PumpEffect.java:468-480`), current stat, not the LKI copy's; `Radiance$` beside it and PumpAll refused |
| `checkSVarMatches`                                         | unresolvable side, operand-less compare, `CheckSecondSVar$` record a pending error (GO-7)                                       |
| `Count$AttackersDeclared` (Raid)                           | distinct creatures in the controller's `attackedThisTurn` (`AbilityUtils.java:2511-2528`), by CardID                            |
| Phase names                                                | Java constant names too (`COMBAT_BEGIN`, `PhaseType.java:98`); no corpus line uses them                                         |
| `attackingAlone`/`Same`/`YouOrYourPW[LKI]`, other suffixes | `CardProperty.java:1506-1549`; Alone counts live attackers plus the card itself                                                 |
| `DefenderCtrlForRemembered`                                | first remembered card's defending player (Holga, Mercadia's Downfall)                                                           |
| `attackingBattle`                                          | refused loudly: Forge bug below                                                                                                 |

## Forge defects

| Where                    | Defect                                                                                                                                  |
| ------------------------ | --------------------------------------------------------------------------------------------------------------------------------------- |
| `CardProperty.java:1522` | `attackingBattle` reads `combat.getDefenderByAttacker(source)`, not `card`: Rampaging Geoderm asks whether the Geoderm attacks a battle |
| `CombatUtil.java:70`     | `battle.getProtectingPlayer().isOpponentOf(...)` throws on a battle with no protector                                                   |

## Engine findings left open

| Where                                                 | Finding                                                                             |
| ----------------------------------------------------- | ----------------------------------------------------------------------------------- |
| `card.go` `traitFaces` (≈55 static/replacement scans) | Still reads a DFC's back face; only continuous statics and triggers use `liveFaces` |
| `defined.go` `TriggeredDefendingPlayer`               | Reads the current combat, not the trigger-time value Java stores                    |

## Smoke probe

`card-smoke -all -workers 8 -list`: plays 30,807 → 30,770, refused 2,807 → 2,848, placed 376 → 372, inconclusive 88,
hung 0, judged 91.6% → 91.5%. Per-card lists: scratchpad `af84-smoke-before.txt`, `af84-smoke-after.txt`, CheckSVar
classification `af84-svar-flips.txt`, final diff `af84-smoke-diff.txt`.

| Flip             | Cards | Why                                                                                                                                                                                                                      |
| ---------------- | ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| refused → plays  | 32    | Pump Double (19: Bulk Up, Overclocked Electromancer, Unleash Fury, …); ChangeZone `Attacking$` (11: Preeminent Captain, Kinscaer Sentry, Shadowfax, …); `Count$AttackersDeclared` (Protean Raider, Wedding Announcement) |
| plays → refused  | 69    | 66 CheckSVar heads this port does not resolve (below); Raid now true reaches Brazen Cannonade's `Effect Duration$` and Perforating Artist's GenericChoice `UnlessCost$`; Rampaging Geoderm `attackingBattle`             |
| placed → refused | 4     | CheckSVar: Mine Is the Only Truth, Necropotence Avatar, Unyaro, You Cannot Hide from Me                                                                                                                                  |

Every CheckSVar flip is a head Java resolves and this port does not: `Count$Adamant…`/`CastSA>Count$…` (Catharsis,
Drowner of Truth, Wumpus Aberration, Desecrate Reality, Deceit, Emptiness, Vibrance, Wistfulness, Plumb the Forbidden),
`Count$StartingPlayer` (7 Smite/Desert Cenote cards), `Count$LeftGraveyardThisTurn` (7), `Count$MostCardName` (3),
`Count$YouScryThisTurn` (2), `…$LifeLostLastTurn` (3), `…$HighestLifeLostThisTurn` (2), `Count$ThisTurnActivated_` (2),
`…$HasPropertyisMonarch` (2), `Count$ValidAll` (Mangara, Tomik), `Count$CountersRemovedThisTurn` (2), and single heads
(`Count$EachSpentToCast`, `Count$YourTurns`, `Count$TotalTurns`, `Count$YouCastThisGame`, `Count$YouDrewLastTurn`,
`Count$IsPrime`, `Count$PromisedGift`, `Count$Compare`, `Doubles`, `Count$ActivatedThisGame`,
`SpellTargeted$CardManaCostLKI`, `Targeted$CastTotalManaSpent`, `…$Speed`, …). Before, each read false and the card
"played" with its condition never met.

## Decisions

| Decision                                                      | Why                                                                                     |
| ------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| Batch per effect body, not per whole resolution               | Smallest event Java treats as simultaneous that covers every ChangesZoneAll site; nests |
| Join combat before the move on the known path too             | Trigger answers match Java's deferred check; only the prompt order differs              |
| `attackingBattle` refused, not mirrored                       | PORT-8: neither compensate nor knowingly reproduce; refusal is visible                  |
| Port `Count$AttackersDeclared` here                           | Largest CheckSVar flip cluster (≈45 Raid cards), combat state already kept              |
| No new engine state, no new `PlayerController` method, no ADR | —                                                                                       |

## Tests

`triggerbatchdamagezone_test.go` (damage step, effect zone change, SBA deaths; each fails with batching off),
`changezoneentersattacking_test.go`, `pumpdoublepower_test.go`, `attackingproperties_test.go`, `checksvarloud_test.go`
(CheckSVar branches, `Count$AttackersDeclared`, phase constant names), `attacktarget_test.go` (protector eligibility).
Scenarios `battle-protector-blocks-the-attack-on-the-siege`,
`changezone-preeminent-captain-puts-a-soldier-in-attacking`, `pump-double-overclocked-electromancer-doubles-its-power`;
`combat-attack-and-damage-a-battle` corrected.
