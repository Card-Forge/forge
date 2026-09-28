# Port Log — Game State: M6 Effects: MustBlock and Combat Declaration Legality

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `attackconstraints.go`, `blockvalidation.go`,
  `mustblockeffect.go`, `attack.go`, `block.go`, `combat.go`
- **Decision:** [ADR-0024](../../../adr/0024-combat-declaration-legality.md) — Java's two validators, an illegal
  declaration is an error

## Combat declarations are validated

CR 508.1c-d/509.1b-c: a declaration obeys every restriction and as many requirements as possible. Java checks both sides
differently; this port reproduces each as it is (PORT-7, ADR-0024 Decision 1).

| Side      | Go                                                     | Java                                                                                                                                                |
| --------- | ------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| Attackers | `validateAttackers`, `attackconstraints.go`            | `CombatUtil.validateAttackers` (`CombatUtil.java:82`), `AttackConstraints.getLegalAttackers` (`AttackConstraints.java:77-160`), `AttackRequirement` |
| Blockers  | `validateBlocks`, `blockvalidation.go`                 | `CombatUtil.validateBlocks` (`:637`), `findFreeBlockers` (`:604`), `mustBlockAnAttacker` (`:745`), `attackerLureSatisfied`                          |
| Pairings  | `checkBlockPairing`, `block.go`                        | `InputBlock.onCardSelected`'s per-click `CombatUtil.canBlock(attacker, blocker, combat)` (`InputBlock.java:151`)                                    |
| Failure   | `*IllegalDeclarationError{Rule, Cards, Reason}`, no-op | Human re-prompted (`PhaseHandler.java:547-550`, `InputBlock.java:101-109`)                                                                          |

`DeclareCombatAttackers`/`DeclareCombatBlockers` now return `(…, error)`. Nothing is applied before validation passes:
attack targets are chosen into a local map, tapping, exert and triggers follow (Java order,
`PhaseHandler.java:545-556`). The `PlayerController` methods are unchanged (ADR-0024 Decision 4). `fixture.RunActions`
returns the error with its `actions.log` line.

**Removed** (ADR-0024 Decision 2): the goad auto-add in `DeclareCombatAttackers`, and `block.go`'s silent drop of a
`CanBlock`-rejected pairing and of a Menace attacker's lone blocker (`menaceLegal`). Reason: which creature to add or
which block to drop is the controller's choice; a fixture could not tell a legal declaration from a repaired one.

### Attack side

`attackRequirement` per creature the active player controls (tapped ones too, Java's `getCreaturesInPlay`: an
unavoidable violation counts in the declared and the best attack alike, so it cancels):

| Requirement source                            | Counted as                                                                              |
| --------------------------------------------- | --------------------------------------------------------------------------------------- |
| Goad (CR 701.15b)                             | one toward every possible defender per distinct goader (`AttackRequirement.java:36-38`) |
| `S:Mode$ MustAttack`, no `MustAttack$`        | one toward every possible defender                                                      |
| `S:Mode$ MustAttack \| MustAttack$ <defined>` | one toward each named entity, minus the active player and what it controls (CR 506.2)   |

Violations = sum of a creature's counts minus the count toward the defender it attacks. Best attack =
`collectLegalAttackers`' unrestricted path (below) or nobody attacking, whichever violates fewer; the declaration may
violate no more. The error names every creature the declaration leaves worse off than the best attack does.

Restriction sources this port does not have, so `getLegalAttackers`' search collapses to one path — every creature with
a requirement attacks its highest-count defender (stable reverse sort, Java's TimSort):

| Java piece not reachable here                                                                       | Why                                     |
| --------------------------------------------------------------------------------------------------- | --------------------------------------- |
| `AttackRestrictionType` (NEED_TWO_OTHERS, NOT_ALONE, ONLY_ALONE, NEED_BLACK_OR_GREEN, …)            | no "can't attack alone"/CantAttack yet  |
| `GlobalAttackRestrictions` max, `isLimited`'s with/without branches, predicate reservations         | no maximum-attackers source             |
| `getAttackCost` skip (Propaganda, `Mode$ CantAttackUnless`)                                         | no attack costs                         |
| `causesToAttack` (`Mode$ AttackRequirement`, 4 lines), `playerRequirements` (`PlayerMustAttack`, 2) | in play → `error`, not uncounted (GO-7) |

The goad restriction half (`CombatUtil.canAttack`'s goad branch, `CombatUtil.java:215-229`) stays `goadTargets`: the
options `ChooseAttackTarget` is offered. An answer outside them, an ineligible creature or a repeated one is an error
(`CR 508.1a`/`508.1b`), Java's `countViolations` -1.

### Block side

Java's local approximation, not CR 509.1c's maximum — `findFreeBlockers`' own
`TODO according to 509.1c, this should really check if the maximum possible is already fulfilled`
(`CombatUtil.java:602-603`), cited in `blockvalidation.go`. Checks, first failure wins, Java's order:

| Check                                                                                                  | Rule          |
| ------------------------------------------------------------------------------------------------------ | ------------- |
| Pairing: attacker not this defender's (CR 802.4a), blocker not offered, repeated pair, second attacker | 509.1a/802.4a |
| Pairing: `CanBlock` (CantBlockBy statics and keywords, `CARDNAME can't block.`, can't-block-alone)     | 509.1b        |
| `MustBlock` requirement a creature could meet ("must still block")                                     | 509.1c        |
| Lure keywords and `MustBlock` via `mustBlockAnAttacker`                                                | 509.1c        |
| `S:Mode$ MustBlock` ("blocks each combat if able")                                                     | 509.1c        |
| `CARDNAME can't block alone.`, `… unless at least two other creatures block.`, `… greater power`       | 509.1b        |
| Blocker count per attacker: Menace (min 2), `S:Mode$ MinMaxBlocker` `Min$ <n>\|All`, `Max$ <n>`        | 509.1b        |

Reproduced quirks (PORT-7): `validateBlocks`' free-blocker loop takes every free blocker able to help on its first pass
whatever `additionalBlockers` is, and consumes them for later creatures' checks; lure keyword lines match by prefix
(`hasStartOfKeyword`) in `attackerLureSatisfied` but exactly (`hasKeyword`) in `canBlock(attacker, blocker, combat)`.

Lure sources: printed `K:` lines (`All creatures able to block CARDNAME do so.` 11, `CARDNAME must be blocked if able.`
16, exactly-one 1, two-or-more 1, `MustBeBlockedBy`/`MustBeBlockedByAll` 5). Pump's `KW$ HIDDEN …` stays rejected
(`pumpeffect.go`), so granted lures do not reach here yet.

Absent Java sources, read as their default: block costs (`getBlockCost` null, no requirement excused),
`StaticAbilityBlockRestrict`'s per-player limit, `Mode$ CanBlockTapped`, and "can block an additional creature" —
`canBlockMoreCreatures` true only for a blocker blocking nothing. Known strictness gap: a creature Java lets block two
attackers (`CanBlockAny$`/`CanBlockAmount$`, 42 lines, unported in Pump and Continuous) is an error here.

## MustBlock lands

`mustblockeffect.go` ports `MustBlockEffect.java:24-97`. Corpus: 26 lines. Registered count 168.

The requirement lives on the blocker, as Java's `Card.mustBlockCards` (`MustBlockEffect.java:76`/`:79`): new state
`Card.mustBlock []mustBlockReq{Attacker, UntilEndOfCombat}`, read through `Card.MustBlockAttackers`. Lifetime:

| Ends                                        | Where                                 | Java                                          |
| ------------------------------------------- | ------------------------------------- | --------------------------------------------- |
| Cleanup, every entry                        | `endMustBlocks(false)`, `cleanupStep` | `Card.onCleanupPhase` → `clearMustBlockCards` |
| End of combat, `Duration$ UntilEndOfCombat` | `endMustBlocks(true)`, `endCombat`    | `addUntilCommand` → `EndOfCombat.addUntil`    |
| Leaving the battlefield                     | `Game.Move`                           | new object (CR 400.7)                         |

`Game.Clone` copies the slice (nil stays nil, `TestCloneAllocationsStayBounded` unchanged).

| Shape (lines)                                                 | Status                                                                         |
| ------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| Host is the attacker, `ValidTgts$` blocker (16)               | resolves; 2 name `ValidTgts$ Creature.DefenderCtrl`, now a `valid.go` property |
| `DefinedAttacker$ TriggeredAttacker[LKICopy]` (4)             | 3 resolve: `Mode$ Attacks` now records `triggered.attacker`                    |
| `BlockAllDefined$ True` (2), `Duration$ UntilEndOfCombat` (4) | resolve                                                                        |
| `Defined$ Valid …` / `DefinedAttacker$ Valid …` (2)           | `error`: `definedCards` has no valid-string form                               |
| `DefinedAttacker$ ParentTarget` / `Defined$ ParentTarget` (5) | `error`: a sub-ability is not targeted apart from its parent (`subability.go`) |
| `Choices$ … \| Chooser$ TriggeredDefendingPlayer` (1)         | `error`: `Choices$`/`Chooser$`/`ChoiceTitle$` rejected before acting           |
| Any other `Duration$`                                         | `error` (none in the corpus)                                                   |

A blocker no longer on the battlefield is skipped (Java's `equalsWithGameTimestamp`).

`DefenderCtrl` (`CardProperty.java:197-212`) is engine-wide, not MustBlock's alone: every `ValidTgts$`/`Valid*$` naming
it (47 corpus files, attack triggers targeting "creature defending player controls") now finds its targets instead of
none. Relative to the attacking source, or for an Aura, Equipment or Fortification source to what it is attached to
(`Combat.getDefendingPlayerRelatedTo`, `Combat.java:452-463`). `DefenderCtrlForRemembered` stays unmatched.

## Mode$ MustAttack and Mode$ MustBlock land

Both scanned on demand over `traitHosts` (battlefield and Command-zone effect cards), `eachCombatStatic`
(`attackconstraints.go`) — no materialized per-card state, since Java evaluates them per declaration too
(`StaticAbilityMustAttack.entitiesMustAttack`, `StaticAbilityMustBlock.blocksEachCombatIfAble`). Effect-card shapes
(`DB$ Effect | StaticAbilities$ MustAttack|MustBlock` with `ValidCreature$ Card.IsRemembered`, 18 and 14 lines) reach
them unchanged.

| Param                                                             | Handling                                                                  |
| ----------------------------------------------------------------- | ------------------------------------------------------------------------- |
| `ValidCreature$` (`ValidCard$` for MinMaxBlocker)                 | `Matches`, host as source; absent matches all                             |
| `MustAttack$`                                                     | `definedEntities`, host's controller, no refs; an unknown spelling errors |
| `Condition$`                                                      | `continuousConditionMet`'s values; any other → `error`                    |
| `IsPresent$`, `PresentCompare$`, `PresentZone$`, `PresentPlayer$` | `isPresentMatches`                                                        |
| `AffectedZone$ Battlefield`, `Description$`, `Secondary$`         | accepted, no effect; another `AffectedZone$` → `error`                    |
| anything else (`ValidPlayer$` 1 line, …)                          | `error` from the declaration                                              |

`MustAttack$` spellings `definedEntities` does not know (`CardOwner`, `EffectSource`, `RememberedPlayer`,
`EnchantedController`, …, 13 lines) error when the static applies. `Player.Other` (2 lines) and `Player.IsRemembered` (2
lines) resolve now: `definedPlayers`' generic `Player.<property>` fallthrough, added porting `Subgame`
(`effects-subgame.md`), reaches `definedEntities` the same way every other `definedPlayers` case already does.

## Tests and fixtures

- `combatlegality_test.go`: every rule above, error and legal case each. `declareAttackers`/`declareBlockers` helpers
  fail a test on error; about 225 call sites moved to them.
- `TestGoadForcesAttackAwayFromGoader` (`pack3effects_test.go`) split: `TestGoadedCreatureLeftHomeIsIllegal` (error) and
  the legal declaration naming the goaded creature.
- `TestDeclareCombatBlockersDropsSingleBlockerAgainstMenace` (`block_test.go`) →
  `TestDeclareCombatBlockersRejectsSingleBlockerAgainstMenace`, error then the legal no-block answer; same split for
  `TestDeclareCombatBlockersRejectsIllegalCantBlockByPairing` (`staticability_test.go`).
- Scenario `ring-bearer-cant-be-blocked-by-greater-power` queued an illegal Hill Giant block (CR 509.1b); it now queues
  the legal block only. The other 353 scenarios declare legally unchanged.
- New scenario `combat-attacks-and-blocks-each-combat-if-able` (Bloodrock Cyclops, Iron Golem).

## Forge defects found

`StaticAbilityCantAttackBlock.cantBlockBy(attacker, null)` is always false: `applyCantBlockByAbility` returns false for
a null blocker (`StaticAbilityCantAttackBlock.java:269`), so `CombatUtil.canBeBlocked`'s "Unblockable check"
(`CombatUtil.java:533`) never fires. Not ported; no validator outcome depends on it, since every requirement check also
asks the per-pair `canBlock(attacker, blocker)`. Row in [`forge-java-defects.md`](../../forge-java-defects.md).
