# AI performance, phase 1: low-risk work elimination

Phase 0 built the measurement layer (see [AI-Performance-Measurement.md](AI-Performance-Measurement.md)).
This is phase 1 of the performance plan: the changes that remove demonstrably repeated work without
changing the action space the AI explores, plus one correctness fix that had to land before any
further concurrency work.

**Nothing here changes how the AI plays.** Every item reaches the same answer by doing less, and each
one is paired with a test that compares it against the code it replaced on a real game state.

## What changed

| # | Change | Where | What it removes |
|---|---|---|---|
| 1 | Reuse the root game score | `SpellAbilityPicker`, `GameSimulator` | One full state evaluation — and the combat-lookahead game copy inside it — per simulation branch |
| 2 | Bounded target counting | `TargetRestrictions.hasAtLeastCandidates` | Building a full candidate list to answer "are there at least N?" |
| 3 | Per-decision comparator facts | `ComputerUtilAbility.SortFacts`, `AiController.sortCandidates` | Re-deriving cost, priority and creature evaluation on every one of `O(n log n)` comparisons |
| 4 | Reuse one structural cost adjustment | `ComputerUtilCost.canPayCost` | The second `CostAdjustment.adjust` scan of every battlefield, stack and command permanent per feasibility check |
| 5 | Serial forced-attacker loop | `AiAttackController.declareAttackers` | A common-pool fan-out that made the declared attack depend on thread scheduling |
| 6 | Pooled evaluation worker | `AiEvaluationExecutor` | One OS thread created and destroyed per priority decision |

### 1. Reuse the root game score

`SpellAbilityPicker` evaluates the unchanged original game once, and then every `GameSimulator` it
constructs evaluated it again. That evaluation is not cheap: before combat damage,
`GameStateEvaluator` copies the whole game to look ahead at the coming combat.

`GameSimulator` now takes an optional baseline. `Score` is two final ints, so the value is the same
number reached by less work — but only while the original game has not changed in a way the evaluator
can see. With assertions on, which is how Surefire runs, the supplied value is checked against a
fresh evaluation for **every** branch, so a stale baseline fails loudly instead of quietly changing a
decision. The four-argument constructor still evaluates for itself and is what `OnePlaySafetyChecker`
and the existing tests use.

**The reuse is scoped to the branches of one candidate, not to a whole decision, and that is an
empirical result rather than a preference.** The plan proposed passing the picker's root score down
to every branch, conditional on a shadow test proving the baseline invariant. That shadow test was
written first, and it failed: on a fixture with a full board and recursive simulation, the score
evaluated at the top of `chooseSpellAbilityToPlay` no longer matched a fresh evaluation by the time
the branches ran. Working out whether each candidate can be played and paid for touches state the
evaluator reads, so a baseline taken before candidate generation does not survive to the branches.
The plan's stated fallback is to narrow the scope or abandon the change; narrowing it to one
candidate's branches — where the first branch still evaluates exactly where it always did — passes,
including on that fixture.

### 2. Bounded target counting

`getNumCandidates` is `O(P + C)` plus a list allocation even when the caller only wants to know
whether one candidate exists. `hasAtLeastCandidates(sa, required)` walks the same sources in the same
order, applies the same predicates, and stops as soon as the answer is known.

It reproduces `getNumCandidates` rather than correcting it — including the double count its own TODO
notes — because callers observe only the boolean. The one thing it never skips is
`applyTargetTextChanges`, which mutates `validTgts` between the player and card passes and which
later readers depend on having run.

Migrated callers (all threshold-only): `ComputerUtilAbility.isFullyTargetable`,
`AiController.canPlaySa`, `SpellAbilityAi.doTrigger`, `CharmEffect`. Callers that need the members or
their order still use `getAllCandidates`.

### 3. Per-decision comparator facts

`saComparator` derives the same handful of values from an ability on every comparison, and
`getSpellAbilityPriority` walks the host card's triggers and static abilities to do it. Nothing in a
sort mutates the game, so those values cannot change while the sort runs.

`AiController.sortCandidates` now creates one `SortFacts` and shares it between both ordering passes —
the general comparator pass and the creature-spell pass — so a creature's evaluation is paid for once
per decision instead of once per comparison. The facts are dropped when the ordering finishes.

`ComputerUtilAbility.saEvaluator` is unchanged and still derives everything on demand, so any other
caller behaves exactly as before. Suspicious comparator asymmetries were deliberately **not** "fixed"
here; the parity test requires byte-identical ordered output.

### 4. Reuse one structural cost adjustment

`ComputerUtilCost.canPayCost` reaches both the mana check and the additional-cost check, and each ran
`CostAdjustment.adjust` over the same cost — the source carried a TODO saying as much. The mana check
now reports the adjustment it derived, and the additional-cost check takes that result.

The two are not automatically the same, which is why this is guarded rather than unconditional. While
it works out the mana cost, `calculateManaCost` temporarily points the host card's "cast from" at the
zone it is currently in, and the adjustment reads that: commander tax is charged from it, and a
static ability's `AffectedZone` requirement is tested against it for a card that has been cast. Where
the adjustment can see that difference — a commander, or a card that has been cast — the reuse is
declined and the second adjustment is performed for itself. It is also declined when the ability
announces `NumTimes`, because the mana check can rewrite the host's SVar in between.

With assertions on, every reuse is shadow-checked against adjusting a second time.

### 5. Serial forced-attacker loop

The must-attack loop in `declareAttackers` fanned out one `CompletableFuture` per attacker onto the
common pool. The tasks read shared combat and requirement objects and declared attackers into the
live `Combat`, so which creatures ended up attacking — and in what order — depended on how the pool
interleaved them; a task still running when the aggregate future timed out could declare an attacker
after the method had moved on.

It now runs serially in attacker order. The work is a handful of requirement lookups per attacker, so
ordering it costs little, and the declaration is reproducible. This is the prerequisite the plan sets
for any later concurrency work: a behaviour-preserving parallel rewrite has to start from a pure
computation over an immutable snapshot, not from this.

### 6. Pooled evaluation worker

The watchdog boundary — run the candidate loop on another thread, wait with a timeout — is worth
keeping. Creating an OS thread per priority decision to get it is not. `AiEvaluationExecutor` hands
out the same semantics over a pool that keeps idle workers for a minute.

**This is a shared pool, not the per-controller single worker the plan sketched**, and the reason is
worth recording. Decisions are not as serial as they look: every simulated game copy builds its own
players and therefore its own controllers, and the AI in a copy takes priority while an outer
decision is still on the stack. A single worker owned by one controller would deadlock the nested
decision behind the outer one, and a worker owned by each controller would leave a parked thread
behind for every game copy. A pool sized by actual concurrency does neither.

`Thread.stop()` is still the last resort, unchanged, because the evaluation loop only honours
cancellation between abilities. A run that ignores cancellation keeps its thread and simply never
returns to the pool — exactly what thread-per-decision did — so one stuck evaluation cannot stall
later decisions.

## New counters

Added to `PerfCounter`, so the JSON report and the JFR events show whether each fast path is engaging:

| Counter | Meaning |
|---|---|
| `baselineScoreReuses` | Branches that took the caller's baseline instead of evaluating one |
| `targetThresholdQueries` | `hasAtLeastCandidates` calls |
| `targetCandidatesVisited` | Entities those traversals examined before stopping — compare against `targetCandidatesMaterialized` |
| `sortFactsComputed` / `sortFactHits` | Comparator facts derived versus served from the per-decision cache |
| `costAdjustmentReuses` | Feasibility checks that adjusted the cost once instead of twice |
| `evalWorkersAbandoned` | Evaluations that ignored cancellation and cost their worker |

## What phase 1 did *not* include

- **"Use no-allocation traversal where the result is not retained"** (plan §3.4). It appears in the
  plan's low-risk list but has no phase 1 row in the roadmap, and §4.1 makes it conditional on an
  allocation profile selecting the call sites. Converting an aggregate zone query is only sound after
  auditing each consumer for snapshot, indexing and mutation assumptions, which is per-call-site work
  that belongs with the phase 2 allocation pass.
- **Reproducing the PR #11366 and #11160 measurements on this revision** (plan roadmap, phase 1 P0).
  This is a measurement task, and it was not run here: the container this work was done in shares CPU
  with other tenants, which is the one thing §11.2 says a timing run must not do. The runbook below is
  what to run on a machine that can produce a trustworthy number. Note that #11366's result cannot be
  reproduced without first implementing the `CardState` trait cache, which is phase 2 P0.

## Reproduction runbook

Take a baseline and an optimised measurement from the same fixture corpus, on a quiet machine, with
pinned JVM flags. `forge bench` is the harness; see the measurement doc for its options.

```
# 1. correctness first: exact trace identity, both builds, same seed
forge bench -d deck-a.dck -D res/geneticaidecks -n 20 -s 7 -t -o before
#   (rebuild with the change)
forge bench -d deck-a.dck -D res/geneticaidecks -n 20 -s 7 -t -o after
diff before/trace.jsonl after/trace.jsonl        # must be empty

# 2. then timing, tracing off, fresh JVM per run, order randomised
forge bench -d deck-a.dck -D res/geneticaidecks -n 20 -w 2 -s 7 -o before-timing
forge bench -d deck-a.dck -D res/geneticaidecks -n 20 -w 2 -s 7 -o after-timing
```

Beyond "faster", the success criteria for these six changes are:

| Change | What the counters must show |
|---|---|
| Baseline reuse | `baselineScoreReuses` = `simulationBranches` − candidates evaluated; `scoreEvaluations` and `combatLookaheadCopies` down by the same amount; identical trace |
| Bounded targets | `targetCandidatesVisited` well below what `targetCandidatesMaterialized` was for the same fixture; same boolean everywhere |
| Sort facts | `sortFactHits` ≫ `sortFactsComputed`; byte-identical ordered candidate list |
| Cost adjustment | `costAdjustmentReuses` close to `canPayCostChecks`; same payable verdict |
| Forced attackers | Same declared attackers across repeated runs of one board |
| Pooled worker | Thread count tracks concurrent evaluations, not decision count; `evalWorkersAbandoned` zero |

## Parity evidence

The default pass criterion is exact trace identity, and it was checked against the merge base rather
than only asserted about. A throwaway harness played two fixed-seed scenarios to the end of a turn on
both builds with `PerfProbe` tracing on, and wrote the ordered decision trace plus the canonical
`GameStateDigest` of the final state:

| Scenario | Covers | Result |
|---|---|---|
| Conventional AI, full board (lands, burn, creature spell, artifact, aura, attackers and blockers) | Candidate ordering, heuristic verdicts, target thresholds, cost feasibility, attack and block declaration, RNG draws | 545 trace lines and the final digest **byte-identical** |
| Full-simulation AI, multi-branch targeting | Simulated branch scores per candidate, baseline reuse, RNG draws | 702 trace lines and the final digest **byte-identical** |

Two things surfaced while building that comparison and are worth recording:

- The simulation fixture originally gave the AI creatures before combat, which makes the evaluator
  copy the game to look ahead. That fixture fails on **master** too, with
  `GameSimulator`'s own "Game copy error" check: a recursively simulated game copies back with a
  creature missing on each side and the opponent two life adrift. It is a pre-existing game-copy
  inaccuracy in recursive simulation, not something this work introduced or fixes, and it is why the
  simulation scenario above leaves the AI's creatures out. It is worth its own investigation.
- Because that check compares a copy against the original's baseline, it is also a second shadow
  check on the reused baseline whenever simulation runs with assertions on.

## Tests

| Test | Covers |
|---|---|
| `forge.ai.AiPhase1OptimizationTest` | Bounded target counting against full counting at every threshold; ordered candidate list with and without facts; cost feasibility with and without adjustment reuse; repeated attack declarations on one board; worker reuse across a played turn |
| `forge.ai.simulation.BaselineScoreReuseTest` | Supplied baseline equals the evaluated one; picker reaches the same score with reuse engaged |
| `forge.ai.AiPerfInstrumentationTest` | (phase 0) Probing does not change the canonical game state; traces are reproducible |

Assertions are enabled under Surefire, so the two shadow checks — the simulation baseline and the
reused cost adjustment — run over the **whole** AI and simulation test suite, not only over the tests
above. Both are `assert`-only and cost nothing in a shipped build.
