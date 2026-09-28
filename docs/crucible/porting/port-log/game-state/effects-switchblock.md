# Effects: SwitchBlock

`SwitchBlockEffect.java`'s two real corpus lines (`general_jarkeld.txt`, `sorrows_path.txt`) each need pieces outside
the effect before either can activate, target or resolve. Each lands as a shared primitive first.

## Combat and activation pieces SwitchBlock needs

| Piece                                                      | Where                                                                   | Java                                                                             | Why SwitchBlock needs it                                                                                                                            |
| ---------------------------------------------------------- | ----------------------------------------------------------------------- | -------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| `blocked` valid property                                   | `valid.go` `propertyMatches`                                            | `CardProperty.java:1591-1592`                                                    | Jarkeld's `ValidTgts$ Creature.attacking+blocked` matched nothing: the tap was paid, then no target, ability never pushed                           |
| `ActivationPhases$` enforced                               | `activateability.go`, `activatemanaability.go` `inActivationPhases`     | `SpellAbilityRestriction.java:131-132,294-297`                                   | Jarkeld is "declare blockers step only"; unenforced it could switch blocks after first-strike damage, changing outcomes                             |
| `TargetsWithSameController$`                               | `targeting.go` `withSameControllerPartner`, `targetStillLegal`          | `CardLists.java:201-217`, `SpellAbility.java:1543-1549`                          | Sorrow's Path's two targets must share a controller; 35 corpus files named it, all silently ignored                                                 |
| Per-turn blocked-by history                                | `card.go` `Card.blockedByThisTurn`, `block.go` `recordBlockedBy`        | `Card.java:112,1658-1666`, `PhaseHandler.java:804-805`, `BlockEffect.java:60-61` | Sorrow's Path's `DefinedAttacker$ Valid Creature.blockedByValidThisTurn Targeted` reads it                                                          |
| Attacker stays blocked when its last blocker leaves combat | `combat.go` `removeFromCombat`                                          | `Combat.java:602-639` (band's blocked flag never cleared)                        | Jarkeld can target a `ForcedBlocked` attacker with no blocker; after the trade the other attacker has none either and must stay blocked (CR 509.1h) |
| Summoning sickness only on creatures                       | `card.go` `Card.isSick`, `activateability.go`, `activatemanaability.go` | `Card.java:3651-3653`                                                            | Sorrow's Path is a Land: played this turn, its {T} was refused (CR 302.6 restricts creatures only)                                                  |

- `blocked` is exact-matched: `combat.isBlocked(card)`, an attacker with a blocker or one an effect made blocked
  (`Combat.ForcedBlocked`). `blockedBySource*`/`blockedThisTurn`/`blockedValidThisTurn` are distinct Java branches, stay
  unported and fall through to "matches nothing".
- `ActivationPhases$` is a general activation restriction, not SwitchBlock's own: every `AB$ Activated` (non-`Mana`)
  line naming it (150 corpus files) now activates only in its phase set through `ActivateAbility`. `ActivateManaAbility`
  gained the identical `inActivationPhases` check too, and `activationphases` is now admitted by
  `manaAbilityAllowedParams`; the sole real `AB$ Mana` line naming it, `mana_cache.txt`'s `Upkeep->Main2`, still cannot
  activate end to end, because that same line also names `Activator$`/`PlayerTurn$`, neither admitted there (rules
  review on the merged commit; those two remain a separate, open gap). Parsed by `parsePhaseRange`
  (`PhaseType.parseRange`) against the current step; all 17 distinct corpus values parse. An unreadable value declines
  the activation (GO-7) rather than dropping the restriction.
- `destroyalleffect.go`, `damagealleffect.go`, `removecountereffect.go`, `untapalleffect.go` still reject
  `ActivationPhases$` at resolve. Now enforced at activation, those rejects are stale; left for their own owners to
  lift.
- `TargetsWithSameController$`: two of Java's three halves. Candidate pre-filter (`targetChoiceFor`, min targets >= 2):
  a card whose controller controls no other candidate is not offered. Fizzle check (`targetStillLegal`): a card target
  whose controller differs from any other chosen card target is illegal; every target is checked against the full chosen
  list before any is dropped (`MagicStack.hasFizzled` removes after its loop), so a split pair fizzles whole. Not
  ported: re-validating the one `ChooseTargets` answer (Java checks each click); no other `ChooseTargets` answer is
  re-validated either, and a split answer fizzles at resolution anyway. A copy's retargeting
  (`copyspellabilityeffect.go`) gets the pre-filter through `targetChoiceFor`.
- Blocked-by history: new engine state, a `[]CardID` per `Card` (Java's `List<Card>` of LKI copies, compared by id). Per
  turn, not per combat, so it cannot live on `Combat`. Written only at Java's two sites: `DeclareCombatBlockers` and the
  Block effect. Cleared in `cleanupStep` next to `AttacksThisTurn` (`Card.onCleanupPhase`, `Card.java:7152`) and on
  leaving the battlefield (`Game.Move`/`MoveToLibraryTop`), where Java's card becomes a new object. Copied by
  `Game.Clone`; a nil slice copies without allocating. Not added to the LKI snapshot's deep copy: nothing reads a
  snapshot's history, and `Move` replaces the live slice with nil rather than mutating it, so no aliasing.
- `Card.isSick`: `ActivateAbility`/`ActivateManaAbility` refused any `{T}` cost while `SummonSick`, creature or not.
  Java's `Card.isSick` requires a creature; a nonbasic land's mana ability the turn it was played was refused too.
  General fix.
- `removeFromCombat`: a blocker leaving combat now puts each attacker it leaves with no blocker into
  `Combat.ForcedBlocked`, which `isBlocked` and combat damage already read as "blocked, no blocker" (no damage to the
  player unless trample). A general fix, CR 506.4/509.1h: every caller (`RemoveFromCombat`, `ChangeCombatants`,
  `GainControl`, phasing, regeneration, `RemoveFromGame`) had let such an attacker go unblocked, where Java's band stays
  blocked. `Game.Move` still leaves combat untouched (`game-state.md`, "Not ported yet").

| Test                                                                   | Proves                                                                                 |
| ---------------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| `TestMatchesBlocked`                                                   | Blocked attacker matches, unblocked attacker and the blocker do not                    |
| `TestActivateAbilityActivationPhasesRestrictsTiming`                   | Declines in Main1 and Combat Damage, cost unpaid; activates in step                    |
| `TestActivateAbilityActivationPhasesRange`                             | `A->B` range inclusive both ends                                                       |
| `TestActivateAbilityActivationPhasesUnreadableDeclines`                | Unreadable phase list declines (GO-7)                                                  |
| `TestActivateManaAbilityActivationPhasesRestrictsTiming`               | `ActivateManaAbility` checks `ActivationPhases$` too, the identical gate               |
| `TestTargetsWithSameControllerDropsLoneCandidate`                      | Three players: lone creature of third player never offered; pair pumped                |
| `TestTargetsWithSameControllerMixedAnswerFizzles`                      | Split answer fizzles whole at resolution                                               |
| `TestTargetsWithSameControllerFizzlesOnControlChange`                  | Control change on stack makes both targets illegal                                     |
| `TestRemoveFromCombatEffectBlockerLeavesAttackerBlocked`               | `RemoveFromCombat` on the only blocker: attacker still blocked, deals no player damage |
| `TestSummoningSicknessSparesNonCreatureTapAbility`                     | A Land entered this turn pays {T} for a stack ability and a mana ability               |
| `combat-blocker-removed-from-combat-attacker-stays-blocked` (scenario) | Labyrinth of Skophos removes the only blocker; attacker stays blocked, no damage       |

## SwitchBlock lands

`switchblockeffect.go`, from `SwitchBlockEffect.java`. Both real corpus lines resolve through the real activation path
(`ActivateAbility`, targeting, stack):

| Line                  | Branch                                  | `DefinedAttacker$`                               | `DefinedBlocker$`                    |
| --------------------- | --------------------------------------- | ------------------------------------------------ | ------------------------------------ |
| `general_jarkeld.txt` | `isTargetingAttacker` true, `:63-115`   | `Targeted`                                       | `Valid Creature.blockingTargeted`    |
| `sorrows_path.txt`    | `isTargetingAttacker` false, `:116-164` | `Valid Creature.blockedByValidThisTurn Targeted` | `Targeted`, plus `RemoveFromCombat$` |

- Resolution order is Java's: resolve both defined lists, keep only attacking/blocking ones, stop if either is empty,
  then the branch. Each branch is planned (`switchPlan`: who leaves combat, which blocks come back, in Java's order)
  before anything moves, so the PORT-8 guards below can fail the line with nothing changed.
- Fizzles, all "nothing changes, no error": one attacker (Jarkeld) or one blocker (Sorrow's Path) left after targets are
  re-checked; a blocker that could not block the other attacker (`CanBlock`, `CombatUtil.canBlock(attacker, blocker)`);
  for Sorrow's Path, either blocker already blocking more than one attacker (`canBlockAdditional()+1`, and nothing in
  this port grants an additional block).
- The two `Valid ...` defined forms need the ability's targets, which `Matches` does not take; both are answered in
  `switchBlockDefined` over the battlefield in seat then zone order (`phasesValidCards`' precedent). Any other
  `DefinedAttacker$`/`DefinedBlocker$` value is an error.
- `RemoveFromCombat$` (Sorrow's Path) fires `AttackerBlockedByCreature` then `Blocks` after each re-added block
  (`SwitchBlockEffect.runTriggers`), through `checkAttackerBlockedByCreatureTriggers`/`checkBlocksTriggers`.
- `Condition$` rejected (`rejectParams`), as `blockEffect` does.

Known gaps, general to `ActivateAbility`, not SwitchBlock's own (pinned by `TestSwitchBlockSorrowsPathRealCard`):

| Gap                                                                                                                                        | Effect on these two cards                                                                                                                       |
| ------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| A trigger fired by paying the cost (`checkTapsTriggers`) is pushed during payment, under the ability; Java (CR 603.3) puts it on top       | Sorrow's Path's own Taps trigger (2 damage to you and each creature you control) resolves after the switch here, before it in Java              |
| Cost is paid and cost triggers fire before targets are chosen; `targetChoiceFor` refuses only zero candidates, not fewer than `TargetMin$` | With one legal target, Jarkeld taps and Sorrow's Path taps and deals its damage for nothing; Java refuses the activation (`TargetMin$ 2` unmet) |

No Java counterpart, by design:

| Java                                                                                         | Why nothing to port                                                                                               |
| -------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `orderBlockersForDamageAssignment`/`orderAttackersForDamageAssignment`, `unregisterAttacker` | Re-cache Java's stored damage order. `AssignCombatDamage` (`control.go`) asks at damage time from `Combat.Blocks` |
| `getBandOfAttacker(attacker1) == getBandOfAttacker(attacker2)`                               | No banding: every attacker its own band, so the check is `attacker1 == attacker2`; two distinct targets never are |
| `GameEventCombatChanged`                                                                     | View event, no ADR-0013 counterpart                                                                               |

**PORT-8 guards.** Three places where `SwitchBlockEffect.java` does something the card text does not
(`forge-java-defects.md`). Copying Java would copy the bug, following the text would fix it silently; each is an `error`
before anything moves, and only when the divergent case is actually reached:

| Guard                              | Divergent case                                                                                                                                                      |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `checkStrayBlocks` (Jarkeld)       | A switching blocker also blocks a third attacker; `removeFromCombat` (`:88`) drops that block too                                                                   |
| `checkStrayBlocks` (Sorrow's Path) | A targeted blocker blocks an attacker no blocked-by record names (a block an earlier switch added, `:97,:103,:151,:157` never record)                               |
| `checkReblockTriggerKeys`          | A re-block would test a `Mode$ Blocks` trigger naming `ValidBlocked$`; Java passes the attacker as `Attacker` (`:22-25`), `TriggerBlocks.java:61` reads `Attackers` |

`checkReblockTriggerKeys` tests `ValidBlocked$` against the new attacker (`Block.Attacker`, the key Java's bug actually
nulls), independent of `ValidCard$` (`TriggerBlocks.java:58`'s own, unrelated check against the blocker): rules review
on the merged commit found the first landed version substituted `ValidCard$` against the blocker instead, over-broad
(errors on any `ValidCard$ Card.Self` trigger regardless of whether `ValidBlocked$` matches) and under-broad (a
`ValidBlocked$` trigger with no `ValidCard$` sailed through unguarded). Fixed before push;
`TestSwitchBlockValidBlockedGuardTestsTheNewAttackerNotValidCard`/`...GuardAppliesWithoutValidCard` pin both directions.

| Test                                                              | Proves                                                                                                             |
| ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `TestSwitchBlockJarkeldTradesBlockers`                            | Jarkeld's branch: blockers trade attackers, scan order                                                             |
| `TestSwitchBlockJarkeldMovesGangBlock`                            | Both blockers of one attacker move together                                                                        |
| `TestSwitchBlockJarkeldIllegalBlockChangesNothing`                | Flying attacker, non-reach blocker: no switch                                                                      |
| `TestSwitchBlockJarkeldOneTargetLeftChangesNothing`               | One target gone before resolution: no switch                                                                       |
| `TestSwitchBlockJarkeldOutsideDeclareBlockersDeclines`            | `ActivationPhases$` holds in the combat damage step                                                                |
| `TestSwitchBlockJarkeldStrayBlockFailsClosed`                     | Stray block guard, blocks unchanged                                                                                |
| `TestSwitchBlockSorrowsPathTradesAttackers`                       | Sorrow's Path's branch via blocked-by history                                                                      |
| `TestSwitchBlockSorrowsPathReblockFiresBlocks`                    | `RemoveFromCombat$` re-block fires `Mode$ Blocks` again                                                            |
| `TestSwitchBlockSorrowsPathValidBlockedTriggerFailsClosed`        | Trigger key guard, blocks unchanged                                                                                |
| `TestSwitchBlockValidBlockedGuardTestsTheNewAttackerNotValidCard` | `ValidCard$ Card.Self` alone (no matching `ValidBlocked$`) does not trip the guard                                 |
| `TestSwitchBlockValidBlockedGuardAppliesWithoutValidCard`         | `ValidBlocked$` alone, no `ValidCard$`, still trips the guard                                                      |
| `TestSwitchBlockSorrowsPathIllegalBlockChangesNothing`            | Blocker that cannot block the other's attacker: no switch                                                          |
| `TestSwitchBlockSorrowsPathOneBlockerLeftChangesNothing`          | One target gone before resolution: no switch                                                                       |
| `TestSwitchBlockSorrowsPathSecondSwitchFailsClosed`               | Unrecorded-block guard on a second switch, blocks unchanged                                                        |
| `TestSwitchBlockRejectsUnknownDefined`                            | Defined spellings outside the two lines are errors                                                                 |
| `TestSwitchBlockRejectsCondition`                                 | `Condition$` rejected                                                                                              |
| `TestSwitchBlockSorrowsPathRealCard`                              | Printed Sorrow's Path (Land, Taps trigger): activates the turn it entered, switches, deals its 2; pins stack order |
| `switchblock-general-jarkeld-trades-blockers` (scenario)          | Whole engine: activate in declare blockers, combat damage follows the new blocks                                   |

Supersedes the `SwitchBlock` row of `effects-abandon-switchblock-choosesector.md`'s deferred table and
`effects-batch-c.md`'s; both files are closed, so the rows stay as written there.
