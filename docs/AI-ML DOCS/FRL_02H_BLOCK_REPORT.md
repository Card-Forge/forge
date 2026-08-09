# FRL-02H BLOCK Declaration and Sequential Assignment Boundary

Status: IMPLEMENTED on `frl/02h-block-boundary`

Base checkpoint: `64ffeb8e630f22d4dc05d4c64a40991b3de6a3ed`

Architecture authority: `docs/AI-ML DOCS/ML_STRATEGY.md`

Prerequisite blocker: NONE

This milestone adds a neutral, callback-local BLOCK boundary for the proven
constraint-free v0 slice. It does not replace Human or AI blocking behavior.
The existing Forge callback remains authoritative; the neutral adapter captures
and replays an unchanged AI declaration for diagnostics, and an explicit
transactional application method is available for a later controller bridge.

## 1. Exact Forge BLOCK trace

The relevant normal path is:

```text
COMBAT_DECLARE_BLOCKERS
  -> removeAbsentCombatants()
  -> freeze stack
  -> declareBlockersTurnBasedAction()
       for each active defending player:
         whoDeclaresBlockers = p.getDeclaresBlockers() or p
         if p is actually attacked and CombatUtil.canBlock(p, combat):
           ReplacementType.DeclareBlocker
           if NotReplaced:
             whoDeclaresBlockers.getController().declareBlockers(p, combat)
         payRequiredBlockCosts
         remove unpaid block assignments
         remove assignments invalidated by group restrictions
         BlockersDeclared event
  -> orderBlockersForDamageAssignment()
  -> orderAttackersForDamageAssignment()
  -> removeAbsentCombatants()
  -> unblocked-attacker triggers
  -> combat/stack continuation
```

The controller chain for the normal callback is:

```text
PhaseHandler
  -> PhaseHandler.declareBlockersTurnBasedAction
  -> PlayerController.declareBlockers
  -> PlayerControllerAi.declareBlockers
  -> AiController.declareBlockers
  -> AiBlockController
```

For Human control the chain reaches `PlayerControllerHuman.declareBlockers`
and `InputBlock`. The replacement branch is checked before the controller
callback. FRL-02H places its diagnostic seam only inside
`ReplacementResult.NotReplaced`, directly around the unchanged controller call.
A replacement-owned declaration therefore produces no neutral BLOCK request.

The phases have distinct ownership:

| Area | Owner |
|---|---|
| Which blocker blocks which attacker | BLOCK boundary / player declaration |
| Pair legality and aggregate declaration legality | Forge `CombatUtil` |
| Required block-cost payment and unpaid removal | PhaseHandler |
| Post-declaration group-restriction cleanup | PhaseHandler |
| `BlockersDeclared`, block triggers, unblocked triggers | Forge |
| Blocker/attacker damage-assignment order | future ORDER / Forge order callbacks |
| Combat damage | future boundary / Forge |

`PhaseHandler` does not universally call `CombatUtil.validateBlocks` after an
arbitrary controller callback. FRL-02H therefore retains it in the neutral
admission proof and in final application.

## 2. Mutation audit

Human `InputBlock` is intentionally not the neutral model. It incrementally
mutates the live `Combat`:

| Human operation | Live effect | Reversible | UI/event effect |
|---|---|---:|---|
| Select blocker | `Combat.addBlocker(attacker, blocker)` | Yes | `CardView` updates, `UiEventBlockerAssigned`, combat-changed event |
| Toggle existing assignment | `Combat.removeBlockAssignment` | Yes | blocker-assignment UI event and combat-changed event |
| Right-click/remove | `Combat.removeFromCombat` | Yes in the GUI sense, but broad | blocker-assignment UI event and view update |
| Done | no new assignment; `validateBlocks(combat, defender)` gates acceptance | n/a | returns to Forge |

The Human UI therefore carries provisional choices in the live combat object.
AI has a different but equally live mutation path: `AiBlockController` can
clear, add, remove, reassign, gang-block, chump-block, and satisfy its own
requirements. `ComputerUtilCombat` and related evaluation code are heuristics,
not legality authorities.

The neutral path has a strict boundary:

```text
CHOOSE_BLOCKER / CHOOSE_ATTACKER
  -> session-local identities and assignments only

completed session
  -> explicit final application
  -> one transactional set of Combat.addBlocker calls
```

No synthetic request calls `addBlocker`, `removeBlockAssignment`,
`undoBlockingAssignment`, or `removeFromCombat`. No card view, combat event,
cost payment, trigger, order, or damage operation occurs during construction.

## 3. Forge legality authorities

The implementation uses Forge's own primitives as follows:

- `CombatUtil.canBlock(attacker, blocker, combat)` builds the initial pair
  matrix. It includes pair evasion, defender relation, capacity/global state,
  and live assignment-sensitive checks.
- `CombatUtil.canBlock(attacker, blocker)` is used only by the diagnostic
  replay session after the captured pair domain has already been admitted.
- `CombatUtil.validateBlocks(combat, defender)` is the aggregate declaration
  oracle. Before any supported request, the live combat is blocker-free and
  `validateBlocks(combat, defender) == null` must hold.
- `CombatUtil.mustBlockAnAttacker` identifies lure, must-block, and related
  coupled requirements. It is not reimplemented as a solver.
- `CombatUtil.canBlockMoreCreatures` exposes blocker capacity. v0 requires
  `canBlockAdditional() == 0` and `canBlockAny() == false` for every
  pair-bearing blocker.
- `CombatUtil.canAttackerBeBlockedWithAmount` and
  `getMinNumBlockersForAttacker` are backed by
  `StaticAbilityCantAttackBlock.getMinMaxBlocker`. v0 admits exactly
  `(1, Integer.MAX_VALUE)` for every attacker.
- `StaticAbilityMustBlock` is used for precise known-mechanic rejection, while
  `validateBlocks(empty)` remains the primary fail-closed proof.
- `StaticAbilityBlockRestrict.blockRestrictNum(defender)` must return
  `Integer.MAX_VALUE`.
- `CombatUtil.getBlockCost(game, blocker, attacker)` is checked for every
  otherwise legal pair before the empty-declaration proof. A cost-bearing pair
  rejects the complete supported callback domain.

## 4. Blocking cardinality model

Forge's `Combat` is a multimap-like model:

```text
one attacker -> many blockers       supported in v0
one blocker  -> many attackers      unsupported in v0
```

`Combat.getBlockers(attacker)` returns a fresh collection for that attacker;
`Combat.getAttackersBlockedBy(blocker)` returns a fresh reverse collection.
`getAllBlockers()` deduplicates a blocker used on multiple attackers, which is
why diagnostic mapping iterates each attacker's `getBlockers(attacker)` list.

Any pair-bearing blocker with additional-blocker capacity or unlimited capacity
is rejected as `UNSUPPORTED_MULTI_BLOCKER_ASSIGNMENT`, rather than silently
dropping legal assignments.

## 5. Constraint-free admission

The exact v0 admission gates are:

```text
normal controller callback after DeclareBlocker replacement is NotReplaced
active game has exactly two players
whoDeclaresBlockers == defendingPlayer
actual attacked targets are exactly one entity, the defending Player
attacker declaration is nonempty and exact
no pre-existing live blocker assignment
no multi-card attacking band

for every pair-bearing blocker:
    canBlockAdditional() == 0
    canBlockAny() == false
    no blocker group restriction

for every attacker:
    getMinMaxBlocker(attacker, defender) == (1, Integer.MAX_VALUE)

blockRestrictNum(defender) == Integer.MAX_VALUE
all legal pairs have getBlockCost(...) == null
validateBlocks(empty combat, defender) == null
```

The cost domain is checked before the empty `validateBlocks` oracle. Known
Must-Block/Lure/blocks-each-combat keywords remain explicit guards for precise
diagnostic reasons, but the keyword list is not the primary legality proof.
Individual pair evasion such as Flying, Reach, Shadow, and `CantBlockBy` is
not a callback-wide rejection; Forge's pair check simply omits that pair.

The actual target calculation deliberately does not use
`combat.getDefenders().size()`. That collection contains possible attack
targets initialized by `Combat.initConstraints()`. The session instead derives
distinct `combat.getDefenderByAttacker(attacker)` values from the current
attackers. An unattacked planeswalker or Battle therefore does not invalidate a
Player-only declaration.

## 6. Sequential decomposition

The approved representation is:

```text
DecisionType.BLOCK

CHOOSE_BLOCKER
  candidates = remaining pair-bearing blocker identities + DONE

CHOOSE_ATTACKER_FOR_BLOCKER
  candidates = currently revalidated captured attackers for the pending blocker

select attacker
  -> commit one session-local blocker -> attacker assignment
  -> remove that blocker from the remaining blocker domain
  -> return to CHOOSE_BLOCKER
```

Attackers remain reusable by multiple independent blockers. The two-stage form
avoids exporting a blocker-by-attacker Cartesian candidate list at every
decision. A blocker candidate is offered only when its captured matrix row has
at least one currently valid pair.

## 7. DONE legality

DONE exists only at `CHOOSE_BLOCKER`. Selecting a blocker means that blocker
will block something, so the attacker stage contains no DONE or generic
CANCEL. If every pending pair becomes stale, the provider returns
`STALE_BLOCK_DECLARATION` with no empty attacker request.

Within the admitted slice, the Forge empty-declaration oracle proves that
blocking nothing is legal. The remaining explicit gates remove all known
dimensions that could make a previously independent partial subset illegal:
Must-Block/Lure, finite min/max counts, global limits, group requirements,
block costs, blocker multi-assignment capacity, and multi-card bands. Therefore
DONE is legal at every `CHOOSE_BLOCKER` stage, including when it is the sole
candidate and is forced.

## 8. Pair-legality strategy

The initial matrix is captured while the live combat has no assignment from
this declaration:

```text
for blocker B and attacker A:
    CombatUtil.canBlock(A, B, combat)
```

At each generation and application, the exact captured identities and the
admission gates are revalidated, and the same captured pairs are checked again.
The live combat remains blocker-free during neutral construction. In the
admitted slice, no local pair can change another pair's legality because all
capacity, count, requirement, group, cost, and band coupling has been excluded.
The final live `validateBlocks` call remains a safety check rather than a
substitute for the admission proof.

## 9. No-partial-mutation model

`BlockDeclarationSession` contains private Forge references only inside the
session/provider/application layer and stores public/session decision state as:

```text
blockSessionId
gameId
captured attacker id + gameTimestamp + defender id
captured band identity and attacker cardinality/shape
captured blocker id + gameTimestamp
captured neutral blocker -> attacker pair matrix
selected neutral assignments
pending blocker identity
active request id
blockStepIndex
completed/terminal flag
```

The public `BlockDeclarationCard`, `BlockDeclarationAssignment`,
`BlockDeclarationContext`, and BLOCK fields on `LegalCandidate` expose neutral
values only. The reflection regression rejects public Forge `Card`, `Player`,
`Combat`, `GameEntity`, and `SpellAbility` exposure.

## 10. Final application and rollback

`applyCompletedToCombat` is explicit and transactional:

```text
require terminal session and empty live blocker assignments
full stale/admission revalidation
resolve exact live objects by id + timestamp and object identity
add exactly the selected pairs
verify each pair registered
recheck getBlockCost for every applied pair
validateBlocks(combat, defender)
return COMPLETE
```

If pair registration, the post-add cost check, or final validation fails, the
provider removes only the pairs recorded by this application, in reverse order,
using `Combat.removeBlockAssignment(attacker, blocker)`. It then verifies that
none of those pairs remains. It never uses `removeFromCombat` as generic
rollback and never pays a block cost. The post-add cost regression uses a
context-dependent `Creature.blocking` validity condition and confirms that the
live combat is empty after rollback.

## 11. Block-cost boundary

Block costs are not part of the neutral BLOCK provider. Forge owns
`CombatUtil.payRequiredBlockCosts` after the normal controller callback. A
cost-bearing pair is not removed from the candidate set: it rejects the whole
supported callback domain as `UNSUPPORTED_BLOCK_COST`, because paying it may be
a real strategic legal choice.

The final application repeats `getBlockCost` after all selected pairs are
present. This protects against context-sensitive costs that do not appear in
the blocker-free admission state.

## 12. Requirement, Menace, and Lure boundary

The empty Forge validation oracle is required before any supported request.
Manual known guards preserve precise reasons for blocker must-block lists,
`blocksEachCombatIfAble`, lure-like attacker keywords, and
`MustBeBlockedBy`/`MustBeBlockedByAll` forms. There is no general requirement
solver.

`getMinMaxBlocker` starts with `(1, Integer.MAX_VALUE)`; Menace or any other
minimum/maximum effect changes that value and rejects the session as
`ATTACKER_BLOCK_COUNT_RESTRICTION`. No temporary first blocker is exported for
a Menace/min-two declaration. Group completion and Lure completion are also
outside v0.

## 13. Replacement-effect handling

`ReplacementType.DeclareBlocker` is evaluated by PhaseHandler before the real
controller callback. The BLOCK diagnostic capture is created only when the
replacement result is `NotReplaced`. A replacement-owned declaration therefore
does not get a synthetic neutral player request and does not get reinterpreted
as a normal BLOCK session.

## 14. Turn-based identity

BLOCK is a turn-based combat declaration, not a child of PRIORITY_ACTION. Each
session uses:

```text
blockSessionId
blockStepIndex
blockStage
```

Diagnostic correlation is `process_id + game_id + block_session_id +
block_step_index`. No `ActionContinuation`, `decisionSequenceId`, or
`actionSubdecisionIndex` is added for ordinary BLOCK requests.

Every session permits at most one outstanding request. A second generation
returns `STALE_BLOCK_DECLARATION / REQUEST_OUTSTANDING` without allocating a
new step. Old request IDs cannot be applied. DONE consumes the request,
snapshots the completed assignment set, and terminalizes the session. Further
generation returns `COMPLETE` with no request.

## 15. AI diagnostic replay

The diagnostic flow is:

```text
capture supported callback before unchanged AI
invoke existing AI unchanged
read final per-attacker blocker assignments
map exact id + timestamp identities and captured pair membership
reject unknown, duplicate, wrong-defender, or multi-attacker blocker use
sort blocker semantic key, then attacker semantic key
replay in a fresh neutral session
select DONE
compare completed assignment SET with the actual Forge assignment SET
```

The mapping iterates `combat.getBlockers(attacker)` for each attacker so that
many blockers on one attacker are preserved. It deliberately does not use the
deduplicating `getAllBlockers()` as the assignment source. A blocker assigned
to multiple attackers is `MAPPING_FAILED`/`BLOCK_STATE`, and diagnostics fail
open without changing the original live Combat. AI valuation, life danger,
trade evaluation, damage prediction, and combat-damage ordering are not
candidate legality.

## 16. Controlled-deck audit

The four controlled precon files were statically audited and the requested
benchmarks were run against the packaged artifact:

| Deck | Relevant observed mechanics | v0 result |
|---|---|---|
| Dead and Alive | Flying cards; Fallen Askari has `CantBlock` and therefore no admitted pair | individual pair/domain filtering |
| Air Forces | Flying cards; Cloud Djinn has `CantBlockBy` for non-flying attackers | Forge pair legality |
| Izzet Guild Kit | ordinary creature combat; no BLOCK-coupling script found | naturally admitted |
| Dimir Guild Kit | ordinary creature combat; no BLOCK-coupling script found | naturally admitted |

Across these four decks the audit found no Menace/minimum blocker effect,
attacker maximum blocker effect, Lure/must-block effect, block-cost
`CantBlockUnless`, global `BlockRestrict`, multi-block capacity, Banding or
multi-card attacking band, or planeswalker/Battle card. Flying and Cloud
Djinn's individual restriction remain supported through Forge pair checks;
Fallen Askari is simply not an eligible blocker. The runtime supported all
observed BLOCK callbacks in both 10-game runs.

## 17. Unsupported boundary

The implementation fails closed for:

```text
multiple actual defending entities
attacked planeswalker or Battle
external declaring player / replacement-owned declaration
pre-mutated Combat
exact attacker or blocker identity/timestamp/shape drift
multi-card attacking bands
global blocker limits
attacker min/max blocker restrictions
blocker group restrictions
Must-Block, Lure, blocks-each-combat, or MustBeBlockedBy coupling
block costs
one blocker capable of blocking multiple attackers
no currently admitted pair
final stale/cost/validate/apply failure
ORDER, combat damage, triggers, payment, multiplayer, search, and game copying
```

The scope deliberately stops before a general Must-Block/Lure solver,
group-completion search, multi-blocker-capacity solver, block-cost payment
integration, ORDER, or combat-damage implementation.

## 18. Performance and diagnostics

The diagnostic CSV now has 55 columns and records:

```text
BLOCK_CALLBACK  raw Forge controller callback
BLOCK           one synthetic neutral request
BLOCK_STATE     unsupported, stale, mapping, or application diagnostic
```

The selection columns include game/session/step identity, stage, candidate
count, selected assignment count, remaining blocker count, initial eligible
blocker count, shrinkage, forced status, reason/status, generation time, and
native callback time. Native AI callback timing remains separate from neutral
request generation.

Percentiles use the seeded 10-game CSV rows; generation is reported in ns.

| Matchup | Raw callbacks | Supported / unsupported | Synthetic requests | Forced / strategic | Eligible blockers mean / p50 / p95 / max | Attackers per blocker mean / p50 / p95 / max | Candidates/request mean / p50 / p95 / max | Steps/callback mean / p50 / p95 / max | Generation p50 / p95 / p99 ns | Mapping/apply failures |
|---|---:|---:|---:|---:|---|---|---|---|---|---:|
| Dead and Alive vs Air Forces | 37 | 37 / 0 | 65 | 18 / 47 | 1.054 / 1 / 2 / 2 | 1.571 / 2 / 2 / 2 | 1.754 / 2 / 2 / 3 | 1.757 / 1 / 3 / 5 | 141100 / 287600 / 1104200 | 0 |
| Izzet Guild Kit vs Dimir Guild Kit | 23 | 23 / 0 | 57 | 22 / 35 | 1.348 / 1 / 3 / 4 | 1.471 / 1 / 3 / 3 | 1.789 / 2 / 3 / 5 | 2.478 / 3 / 5 / 5 | 180400 / 405500 / 1970000 | 0 |

The corresponding native callback p50/p95/p99 values were 2279800 / 7875200 /
12818100 ns for Dead/Air and 3425100 / 10842900 / 11897800 ns for
Izzet/Dimir. These are reported separately and are not neutral generation
latency. Raw CSVs are retained outside tracked source files under
`C:\forgeAI\target\`.

## 19. Expected and actual files

The implementation files are:

```text
forge-game/src/main/java/forge/game/decision/DecisionType.java
forge-game/src/main/java/forge/game/decision/DecisionRequest.java
forge-game/src/main/java/forge/game/decision/LegalCandidate.java
forge-game/src/main/java/forge/game/decision/BlockDeclarationAdapter.java
forge-game/src/main/java/forge/game/decision/BlockDeclarationAssignment.java
forge-game/src/main/java/forge/game/decision/BlockDeclarationCandidateKind.java
forge-game/src/main/java/forge/game/decision/BlockDeclarationCard.java
forge-game/src/main/java/forge/game/decision/BlockDeclarationContext.java
forge-game/src/main/java/forge/game/decision/BlockDeclarationDecisionProvider.java
forge-game/src/main/java/forge/game/decision/BlockDeclarationSession.java
forge-game/src/main/java/forge/game/decision/BlockDeclarationStage.java
forge-game/src/main/java/forge/game/decision/PriorityActionDiagnostics.java
forge-game/src/main/java/forge/game/phase/PhaseHandler.java
forge-ai/pom.xml
```

The regression tests are:

```text
forge-game/src/test/java/forge/game/decision/BlockDeclarationPublicApiTest.java
forge-game/src/test/java/forge/game/decision/BlockDeclarationProviderContractTest.java
forge-ai/src/test/java/forge/game/decision/BlockDeclarationIntegrationTest.java
```

No Combat, CombatUtil, static-ability, AI heuristic, Human/InputBlock,
ActionContinuation, ORDER, damage, confirmation, mulligan, ML/RL, network, or
game-copying implementation was modified.

## 20. Recommendation

Keep FRL-02H v0 exactly as implemented: a fail-closed, two-stage neutral
BLOCK boundary for the admitted 1v1 Player-only, independent-pair slice, with
Forge `validateBlocks(empty)` as the admission oracle and a transactional final
application seam. Use the diagnostic AI replay to measure coverage and
identity correctness. The next expansion prerequisite should be a separately
approved coupled-block requirement boundary; do not broaden this provider by
adding an implicit Must-Block/Lure or payment solver.

## Verification

```text
mvn -pl forge-game -am -Dtest=BlockDeclarationPublicApiTest,BlockDeclarationProviderContractTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
3 tests, 0 failures, 0 errors, 0 skipped

mvn -pl forge-ai -am -Dtest=BlockDeclarationIntegrationTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
20 tests, 0 failures, 0 errors, 0 skipped

mvn -pl forge-game -am test
6 tests, 0 failures, 0 errors, 0 skipped

mvn -pl forge-ai -am test
6 inherited Forge Game tests + 20 BLOCK integration tests, all passing

mvn -pl forge-gui-desktop -am -DskipTests package
BUILD SUCCESS; packaged JAR produced

git diff --check
PASS
```

Benchmark commands used the packaged JAR from the `forge-gui` resource
directory with `-n 10`, seeds `20260809` and `20260810`, and separate
`forge.priority.metricsFile` outputs. All twenty games completed. The Izzet /
Dimir run also printed pre-existing unrelated unsupported priority-action
warnings for `Invoke the Firemind` and `Direct Current`; no BLOCK diagnostic
failure altered the results.
