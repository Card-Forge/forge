# Effects: RingTemptsYou

The Ring tempts you (CR 701.54): a per-player Command-zone effect card whose abilities grow with each temptation, the
Ring-bearer designation and the `Mode$ RingTemptsYou` trigger. Batch file for one API (DOC-12, `port-effect` step 5).

---

## RingTemptsYou lands

`RingTemptsYou` (49 corpus lines) resolves: the activator's Ring tempts them. `ringtemptsyoueffect.go`, ported from
`RingTemptsYouEffect.java` and `Player.createTheRing`/`setRingLevel`/`setRingBearer` (`Player.java:3289-3351`,
`:1777-1789`).

| Step | Port |
| --- | --- |
| The Ring | Made on first temptation (`createTheRing`), an `IsEffect` card in the activator's Command zone, kept for the rest of the game |
| Count and level | `Player.ringTempted` +1; the Ring's `Def` becomes `ringDef(n)`, the abilities of levels 1..min(n, 4) |
| Ring-bearer | Candidates: every battlefield creature the activator controls, seat then zone order (`getCreaturesInPlay`). None: no pick, the previous bearer stays (`setRingBearer(null)` returns early). One: taken, no question. More: `ChooseCardsForEffect(lo=1, hi=1)`, checked |
| Trigger | `Mode$ RingTemptsYou` runs with the activator as `TriggeredPlayer` |

**The Ring follows the Monarch precedent, not a bare field.** `BecomeMonarch`/`TakeInitiative` build a Go-side
`IsEffect` Command-zone card (`designationDef`, `putDesignationCard`, `effects-monarch-initiative-venture.md`); the Ring
reuses both. Reason: its level abilities are real statics and triggers that the existing `traitHosts` walks (layers,
`cantBlockBy`, attack/block/damage triggers) already read off an effect card in the Command zone -- a status field would
need its own hook in each of those walks.

**One definition per level, rebuilt, not appended.** Java's `setRingLevel(n)` adds only level n's abilities, once per
temptation, so after n temptations the Ring holds levels 1..min(n, 4). `temptWithRing` swaps the card's `Def` for a
fresh `ringDef(n)` holding that same set. Built as trees (PORT-2), no string parsed at runtime. The card is kept, so its
timestamp -- the one the Layer 4 static applies at -- stays the Ring's own. Nothing in the engine keys state on a
`*compile.Ability` pointer, so a new definition resets nothing.

| Level | Java string (`Player.java`) | Resolved by |
| ---: | --- | --- |
| 1 | `Mode$ Continuous \| Affected$ Card.YouCtrl+IsRingbearer \| AddType$ Legendary` | `applyContinuousType` (Layer 4) |
| 1 | `Mode$ CantBlockBy \| ValidAttacker$ Card.YouCtrl+IsRingbearer \| ValidBlockerRelative$ Creature.powerGTX`, X = `Count$CardPower` | `cantBlockBy` + `blockerRelativeMatches` (new) |
| 2 | `Mode$ Attacks \| ValidCard$ Card.YouCtrl+IsRingbearer` -> `Draw \| Defined$ You \| NumCards$ 1`, then `Discard \| NumCards$ 1 \| Mode$ TgtChoose` | `checkAttacksTriggers`, `SubAbility$` chain |
| 3 | `Mode$ AttackerBlockedByCreature \| ValidBlocker$ Creature` -> `DelayedTrigger \| Phase$ EndCombat \| RememberObjects$ TriggeredBlockerLKICopy` | `checkAttackerBlockedByCreatureTriggers` records the blocker (new) |
| 3 | delayed `Execute` -> `SacrificeAll \| Defined$ DelayTriggerRememberedLKI` | `delayedPhaseTriggerMatches`, `sacrificeAllEffect` |
| 4 | `Mode$ DamageDone \| ValidTarget$ Player \| CombatDamage$ True` -> `LoseLife \| Defined$ Opponent \| LifeAmount$ 3` | `checkDamageDoneTriggersToPlayer` |

Each level has a module test driving real combat (`ringtemptation_test.go`).

### Shared engine pieces

| Piece | Where | Why |
| --- | --- | --- |
| `IsRingbearer` card property | `valid.go` | `CardProperty.java:160`. True when some player's `Game.RingBearer` is the card |
| `ValidBlockerRelative$` in `cantBlockBy` | `staticability.go` | `applyCantBlockBy` ignored it, which would make the Ring-bearer unblockable by everything. Resolved shape: `Creature.powerGTX` with X = `Count$CardPower`, X the attacker's power (`matchesValidParam(..., blocker, attacker)`) -- `skulkBlocks`' comparison |
| Unresolved `ValidBlockerRelative$`: static skipped | `staticability.go` | Space Beleren's `Creature.DifferentSector` (1 line) no longer applies unconditionally; skipped, the `matchesValidDefender` contract. `ValidAttackerRelative$` (Ironclaw Curse) left as it was: out of this port's scope |
| `triggeredObjects.blocker` | `ability.go`, `trigger.go` | `TriggerAttackerBlockedByCreature.setTriggeringObjects`' Blocker, recorded by `checkAttackerBlockedByCreatureTriggers` |
| `Defined$ TriggeredBlocker`/`TriggeredBlockerLKICopy` | `defined.go` | Read by the level-3 `RememberObjects$`; an error when the trigger recorded no blocker (GO-7). A `CardID` is stable across zone changes, so the LKI spelling names the same card |
| `Mode$ RingTemptsYou` (9 corpus lines) | `checkRingTemptsYouTriggers` | `playerActionTriggerMatches` with a gate: `ValidPlayer$` against the tempted player, `ValidCard$` against the bearer just chosen -- none chosen matches nothing (`matchesValid(null)`). All four `phaseTriggerZones`, so Ringwraiths' `TriggerZones$ Graveyard` fires |

### New engine state

| State | Java | Why |
| --- | --- | --- |
| `Player.theRing` | `Player.theRing` | The one Ring card, `monarchEffect`'s pattern |
| `Player.ringTempted` | `Player.numRingTemptedYou` | The count the level follows; `Game.RingTemptedYou` |
| `Player.ringBearer` | `Player.ringBearer` + `Card.ringbearer` | One field instead of Java's two copies of one fact. `Game.RingBearer` also answers `NoCard` off the battlefield or under another controller |

All three are values `Game.Clone` copies with the `Player` struct (`TestRingStateSurvivesClone`); no new allocation.

**Losing the Ring-bearer (CR 701.54a).** Java registers a leaves-play and a change-controller `GameCommand` on the
bearer. Here `loseRingBearer` runs from `Move`/`MoveToLibraryTop` leaving the battlefield and from `changeControllerAt`
on a real controller change -- the `controllerChangeZoneCorrection` point, where the port already drops a creature from
combat. A Layer 2 static control change reaches no `changeControllerAt`; `dropStolenRingBearers`, run by
`CheckStateBasedActions` right after `applyContinuousControl`, clears a bearer another player now controls -- Java's
`checkStaticAbilities` running `controllerChangeZoneCorrection` (`GameAction.java:1203-1207`). Once cleared it stays
cleared when control comes back, as in Java.

**No new `PlayerController` method.** Java's `chooseSingleEntityForEffect` over creatures is `ChooseCardsForEffect` with
`lo = hi = 1`, `Play`'s pattern.

**Fixture keys.** `numringtemptedyou=` and `|IsRingBearer` are Java `GameState` keys, now loaded and dumped
(`game-state-fixture.md`). Scenarios: `ring-tempts-you-nazgul-becomes-ring-bearer` (Nazgûl's ETB tempts, the only
creature becomes the bearer, its own `RingTemptsYou` trigger counters each Wraith) and
`ring-bearer-cant-be-blocked-by-greater-power` (level 1 in combat).

### Rejected (`error` before acting)

| Shape | Lines | Reason |
| --- | ---: | --- |
| `ConditionDefined$` | 1 | `subAbilityConditionMet` reads it as unmet and would skip the temptation silently (`BecomeMonarch`'s reason) |

A `Mode$ RingTemptsYou` trigger whose `Execute$` has `Cost$` (Call of the Ring's `PayLife<2>`, Sauron's
`Discard<1/Hand>`) pays it as it resolves, like every triggered `AB$`
([`## Triggered \`AB$\` costs`](mana-and-casting.md#triggered-ab-costs)).

**Not ported, not reached by the corpus shape:** Java's set code on the Ring (`createTheRing(setCode)`, image only);
`RestartGame`'s Ring reset (`RestartGameEffect.java:70-72`) -- `RestartGame` is unported.

**No Forge defect found (PORT-8).**
