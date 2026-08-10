# ForgeAI ML Strategy

**Status:** Provisional / Accepted for implementation planning
**Date:** 2026-08-08
**Revision:** 9 — as revision 8, plus the post-CONFIRMATION sequence: ORDER attribution before decomposition, the modern DAMAGE_ASSIGNMENT information barrier, and the zero-unsupported gate ahead of RandomLegalPolicy
**Scope:** Initial ForgeRL 1v1 research environment

## Purpose

This document records the current machine-learning architecture decisions for ForgeAI.

These decisions are based on:

* FRL-00 Forge baseline results,
* independent architecture reviews from multiple model families,
* published MTG / collectible-card-game RL research,
* and Forge-specific architectural constraints.

The objective is to prevent the ForgeRL environment from being designed around a learning algorithm before the relevant empirical questions have been answered.

---

# 1. Core Decision

## Do not commit ForgeAI to PPO

PPO is retained as an important baseline, but it does **not** own the architecture.

The learning algorithm must remain replaceable.

Current candidate families include:

* masked PPO,
* R2D2 / replay-based value learning,
* IMPALA / V-trace,
* replay-capable actor-critic,
* future policy/value-guided search.

The production learner will be selected empirically.

---

# 2. Algorithm-Neutral ForgeRL Contract

ForgeRL must expose an algorithm-independent environment.

Conceptually:

```text
Forge
  ↓
Player-perspective Observation
  ↓
DecisionRequest
  ↓
LegalCandidate[]
  ↓
Agent
  ↓
ChosenCandidate
  ↓
Forge
```

The following types must not contain PPO-, Q-learning-, or neural-network-specific assumptions:

```text
Observation
DecisionRequest
LegalCandidate
Trajectory
EpisodeResult
HistoryEvent
```

The same interface must support:

```text
RandomLegalPolicy
Forge heuristic teacher
Behavior Cloning
PPO
R2D2
IMPALA / V-trace
other actor-critic methods
```

## 2.1 Scope Limit of the Contract

This contract is a **stepping** interface.

Search is a **branching** consumer: it additionally requires state copying, rollback, and information-set-safe resampling.

Those capabilities are deliberately **not** part of the contract above and are deferred with Section 11.

The contract must therefore not be described as "search-ready" until a branching capability is specified separately.

---

# 3. Action Representation and Decision Decomposition

Do not create one enormous global Magic action ID space.

Forge decisions are heterogeneous and may differ substantially in combinatorial structure.

The core abstraction remains:

```text
DecisionRequest
    ↓
LegalCandidate[]
    ↓
ChosenCandidate
```

However, `LegalCandidate[]` refers to the legal alternatives for **one atomic agent decision**, not necessarily all complete outcomes of one Forge `PlayerController` callback.

A single Forge callback may therefore produce multiple sequential `DecisionRequest`s.

This distinction is architectural and must be established before the ML environment is considered complete.

## 3.1 Enumerate vs. Decompose

Each Forge decision family must explicitly choose one of two representations:

```text
ENUMERATE
    Forge callback
    → complete legal candidates
    → one agent decision

DECOMPOSE
    Forge callback
    → sequence of smaller legal decisions
    → final composed Forge result
```

Initial policy:

| Decision family                      | Initial representation                                        |
| ------------------------------------ | ------------------------------------------------------------- |
| `PRIORITY_ACTION`                    | Enumerate legal top-level actions                             |
| `TARGET` — single target             | Enumerate                                                     |
| `MODE` — small legal set             | Enumerate                                                     |
| `X_VALUE` — bounded small range      | Enumerate where practical                                     |
| `MULLIGAN` — keep/redraw             | Enumerate (binary)                                            |
| `MULLIGAN` — London bottoming        | Sequential `CARD_SELECTION` (max 35 subsets; enumerable, but sequential for identity reuse) |
| `CONFIRMATION`                       | Enumerate                                                     |
| `CARD_SELECTION` — small bounded set | Enumerate or sequential selection                             |
| `ATTACK`                             | Decompose into sequential set construction                    |
| `BLOCK`                              | Decompose into blocker/attacker assignments                   |
| `ORDER` — see 18.2                   | Attribution pending; almost certainly not one family          |
| `PAYMENT`                            | Decompose where complete payment enumeration is combinatorial |
| multi-target choices                 | Autoregressive/sequential selection                           |
| large subset choices                 | Sequential set construction                                   |

This table is an initial architectural policy, not a complete inventory of Forge callbacks.

Every actual `PlayerController` decision type must eventually be classified explicitly.

### Rationale: generalization, not current infeasibility

The initial decision decomposition policy is not based only on the candidate counts observed in the current two-deck slice.

FRL-00.5 found relatively small observed structures in the benchmark workloads:

```text
ATTACK
eligible attackers:
max 6 proactive
max 8 reactive

BLOCK
eligible blockers:
max 4

ORDER
items:
max 3 proactive
max 4 reactive
```

For this restricted slice, complete enumeration of some of these outcomes could be computationally feasible.

Nevertheless, the initial ForgeRL design retains sequential decomposition for:

```text
ATTACK
BLOCK
ORDER
PAYMENT where needed
```

because the architecture must remain stable as board sizes and card pools grow.

The primary rationale is therefore:

> **generalization across variable and potentially much larger decision structures, rather than evidence that enumeration is already infeasible in the initial benchmark.**

For example:

```text
8 eligible attackers
→ at most 256 subsets

15 eligible attackers
→ 32,768 subsets
```

A policy trained to score incremental choices such as:

```text
ADD_ATTACKER(card)
DONE
```

can reuse the same action semantics across both situations.

A policy whose candidate vocabulary consists of complete attacker subsets would instead encounter rapidly changing, largely unseen compound candidates as board size grows.

Therefore decomposition is selected where it provides:

```text
stable action semantics
bounded candidate-set growth
better transfer across board sizes
reusable card/entity representations
```

even if enumeration would have been affordable in the current controlled slice.

This remains an empirical architectural decision and may be revisited for specific bounded decision families where whole-action enumeration proves materially better.

## 3.2 Attacker Selection

Do not enumerate all attacker subsets.

Instead expose something conceptually equivalent to:

```text
ATTACK_SELECTION

legal candidates:
    eligible attacker A
    eligible attacker B
    eligible attacker C
    DONE
```

After choosing an attacker, ForgeRL updates the partial declaration and produces the next request.

Example:

```text
[A, B, C, DONE]
    ↓ choose A

[B, C, DONE]
    ↓ choose C

[B, DONE]
    ↓ DONE
```

The resulting composed Forge declaration is:

```text
attackers = [A, C]
```

Legality must be recomputed after each partial choice.

## 3.3 Blocker Assignment

Do not enumerate the complete Cartesian space of blocking assignments.

Use sequential construction such as:

```text
CHOOSE_BLOCKER
    ↓
CHOOSE_ATTACKER_FOR_BLOCKER
    ↓
repeat
    ↓
DONE
```

Where ordering or multiple-block rules require additional choices, expose those as subsequent typed requests.

## 3.4 Ordering

Do not enumerate permutations.

Construct an ordering autoregressively:

```text
remaining = [A, B, C]

ORDER_NEXT
legal = [A, B, C]

→ choose B

remaining = [A, C]

ORDER_NEXT
legal = [A, C]

→ choose A

remaining = [C]

forced → C
```

## 3.5 Payment

Mana/payment choices are strategically relevant and must not automatically be collapsed into one opaque Forge heuristic.

Complete payment combinations may be combinatorial.

The initial environment should therefore support incremental payment decisions where necessary, for example:

```text
PAYMENT_SOURCE
→ choose mana source

MANA_ABILITY / COLOR
→ choose relevant production mode

repeat until cost is satisfied

CONFIRM_PAYMENT
```

Forge's existing auto-payment behavior may later be offered as an explicit candidate such as:

```text
AUTO_PAY
```

but it must not silently replace strategically relevant payment decisions.

The decomposition must preserve the possibility that mana-source selection changes future available interaction.

## 3.6 Atomic Candidate Contract

After decomposition, every neural/agent-facing step should have a manageable dynamic legal candidate set:

```text
History / Observation
        ↓
DecisionRequest
        ↓
LegalCandidate[1..N]
        ↓
candidate scorer
```

Long-term model shape:

```text
state/history encoder → h

legal candidate A → encoder → eA
legal candidate B → encoder → eB
legal candidate C → encoder → eC

                 ↓

score(h, candidate)
```

The model scores only the candidates legal at that atomic step.

The resulting score may later represent:

```text
policy logit
Q value
search prior
```

without changing the Forge decision semantics.

## 3.7 Synthetic Decisions

A `DecisionRequest` does not need to correspond 1:1 to a Forge callback.

ForgeRL is allowed to synthesize sub-decisions when this is necessary to avoid combinatorial candidate enumeration.

Therefore:

```text
Forge callback count
≠
Agent decision count
```

This distinction must remain explicit in profiling, trajectories, replay and evaluation.

Every synthetic decision must still compose into exactly one legal Forge result without changing Magic rules.

## 3.8 Implemented Boundary Status

The initial policy in 3.1 has now been tested against the engine. Status as of FRL-02J:

| DecisionType | Milestone | Realised representation | Status |
|---|---|---|---|
| `PRIORITY_ACTION` | FRL-01A | Enumerated top-level actions | SUPPORTED, known `COST_ADJUSTMENT_CHOICE_REQUIRED` gap |
| — action continuation | FRL-02A | `decision_sequence_id` + `subdecision_index` correlation, not a request family | SUPPORTED |
| `TARGET` | FRL-02B | Sequential multi-target selection with legal DONE | SUPPORTED for cards, players, stack spells |
| `PAYMENT` | FRL-02C | Sequential source/floating-mana selection | **PARTIAL** — variable output and several mana semantics fail closed |
| `X_VALUE` | FRL-02D | Enumerated finite `X|N` domain | SUPPORTED within a provable capacity model |
| `MODE` | FRL-02E | Enumerated from Forge's own `possible` list, original ordinals | SUPPORTED for ordinary single-mode Charm slice |
| `CARD_SELECTION` | FRL-02F | Sequential, stable `(cardId, gameTimestamp)` identities | SUPPORTED for resolution-time own-hand discard |
| `ATTACK` | FRL-02G | `ADD_ATTACKER \| ... \| DONE` | SUPPORTED for constraint-free 1v1 slice |
| `BLOCK` | FRL-02H | Two-stage `CHOOSE_BLOCKER` → `CHOOSE_ATTACKER_FOR_BLOCKER` | SUPPORTED for independent-pair Player-only slice |
| `MULLIGAN` | FRL-02J | KEEP/REDRAW plus `MULLIGAN_BOTTOM` card selection | SUPPORTED for ordinary 1v1 Constructed London mulligan |
| `ORDER` | 18.2 | attribution pending — see below | OPEN, aggregate |
| `DAMAGE_ASSIGNMENT` | — | see 3.9 | OPEN |

The decomposition policy in 3.1 held for `ATTACK`, `BLOCK` and `PAYMENT`. It was **incomplete** for `MULLIGAN`: the London bottoming choice is a second, separate decision that the original one-line classification did not describe.

Every implemented boundary follows the same discipline: Forge remains the legality authority, unsupported states are structured and explicit rather than approximated, and the neutral layer does not mutate live game state during candidate construction.

`PAYMENT` is the load-bearing exception. It is `PARTIAL`, it is the second-highest-volume family, and its unsupported share is measured rather than hypothetical: the reactive matchup produced 848 raw callbacks, 225 atomic requests and 192 explicit `VARIABLE_MANA_OUTPUT` states — roughly 22.6% of raw payment callbacks carrying a known unsupported feature.

`ORDER` is listed as one row only because the engine exposes it as one callback. It is an aggregate of several unrelated decision families and must be attributed before it is classified. See 18.2.

## 3.9 DAMAGE_ASSIGNMENT and the Information Barrier

The rules changed underneath this boundary. Since Foundations (November 2024) there is no damage assignment order: when an attacking creature is blocked by several creatures, its controller divides the combat damage freely among them at the start of the combat damage step, lethal damage no longer constrains the division, and trample remains the sole exception in requiring lethal damage to every blocker before trampling over. Players make no combat-damage decision during the declare blockers step, and there is no priority window between assignment and damage.

### This is not hidden information

The distinction matters for where the problem lands in the architecture.

```text
hidden information
    a state exists and one player cannot observe it
    -> belief-state inference

damage assignment
    the state does not exist yet
    -> a private, not-yet-made future choice
```

The defending player casts combat tricks without knowing how the damage will be divided. That is uncertainty about an **opponent's future choice**, not uncertainty about a concealed card. It is an opponent-modelling problem, not an inference problem over hidden zones, and it should not be modelled as a belief state.

### Two interface rules

```text
no assignment value may enter the opponent's observation
before the choice is made

no artificial opponent observation or response step may be
introduced between assignment and damage
```

The second is the easier mistake to make. A decomposed assignment boundary naturally produces intermediate steps, and exposing them to the opponent would invent a response window the rules do not grant — and would silently make the defender stronger than Magic allows.

### Open question before implementation

FRL-02H documented the live Forge combat trace, and it places these calls in the declare-blockers path:

```text
-> orderBlockersForDamageAssignment()
-> orderAttackersForDamageAssignment()
```

Under post-Foundations rules no such decision belongs there. Either the methods are vestigial, or they no longer fire, or this Forge version has not adopted the change.

> Before `DAMAGE_ASSIGNMENT` is implemented, the ORDER attribution audit must establish which of the three is true. The boundary has to be designed against the model the engine actually runs, not against the model the rules describe.

FRL-02H already admits the one-attacker/many-blockers case in the v0 slice, so this boundary is live rather than theoretical.

---

# 4. Forced Decisions and Automatic Resolution

Automatic resolution applies to an **atomic ForgeRL decision**, not necessarily an original Forge callback.

After any required decomposition:

```text
exactly 1 legal candidate
→ execute automatically
→ record HistoryEvent
→ no policy inference
→ no policy-loss training sample
```

The event must remain in history because it may carry strategically relevant information for later decisions.

If two or more legal candidates exist:

```text
→ agent decision
→ trajectory decision step
```

Important:

`PASS` is not automatically trivial.

```text
PASS only legal action
→ forced

PASS + one or more alternative legal actions
→ genuine strategic decision
```

This is especially important for reactive Magic strategies where declining to act while retaining mana or information is part of the policy.

---

# 5. History and Memory

ForgeRL must support complete legal action-observation history from the beginning.

The trajectory/interface must therefore support sequence learning and recurrent state.

However:

**A recurrent neural network is not mandatory for every initial model.**

Initial experiments should be able to compare:

```text
feed-forward
vs.
GRU/LSTM
```

The expensive architectural commitment is therefore:

```text
history-capable interface: YES
```

not:

```text
specific recurrent model: FIXED
```

Transformers are deferred until evidence shows that GRU/LSTM or explicit state features are insufficient.

---

# 6. Behavior Cloning

Behavior Cloning from Forge's existing heuristic AI is the default initialization strategy.

Pipeline:

```text
Forge heuristic
      ↓
same Observation
same DecisionRequest
same LegalCandidates
      ↓
Trajectory Dataset
      ↓
Behavior Cloning
```

BC is expected to reduce the amount of expensive online exploration required to learn basic Magic behavior.

However, BC is an initialization strategy, not the final objective.

At least one scratch/random-initialization control should remain in experiments so that the value of BC can be measured.

Do not permanently anchor the policy to the Forge teacher.

If imitation loss is retained during RL, its influence should be configurable and normally decay.

Before collecting teacher data, verify that Forge AI decisions do not depend on information that is unavailable in the recorded player-perspective observation.

---

# 7. Reward and Episode Outcome Semantics

The primary strategic reward remains:

```text
win  = +1
draw =  0
loss = -1
```

However, ForgeRL must distinguish three different termination classes.

## 7.1 Normal game result

```text
WIN
LOSS
RULES_DRAW
```

These are strategic outcomes and produce normal learning targets.

## 7.2 Environment / engine failure

Examples:

```text
engine hang
worker crash
invalid transition
bridge failure
corrupted state
decision timeout caused by infrastructure
```

These are engineering failures.

They must be reported as:

```text
INVALID / TRUNCATED
```

and must not silently become losses.

### This requirement binds the match harness

FRL-02K0 found the inverse failure in practice. A diagnostic exception escaped into the game loop; the simulator caught it and, in a `finally` block, ended the game as a draw; `Player.onGameOver` then marked both unresolved players as winners.

The engine failure did not become a loss. It became a **result** — which is equally prohibited and considerably harder to notice, because nothing turns red.

Therefore:

> Any code path that can end a game must classify the termination. A `finally` block that assigns an outcome without classifying why violates this section regardless of which outcome it assigns.

This applies to the match harness and the simulator entry point, not only to the ForgeRL environment wrapper.

**Status: closed.** The simulator no longer constructs a singular winner from a malformed outcome. The case is reported explicitly:

```text
INVALID_OUTCOME MULTIPLE_WINNERS [0,1]
```

The underlying Forge defect — a singular getter resolving a plural, unordered winner source — is no longer a ForgeAI blocker. It remains recorded as an upstream contribution candidate in Section 20.4.

## 7.3 Game-limit termination

Examples:

```text
turn cap
match clock
Forge simulation clock limit
```

These must **not** automatically be grouped with engine failures.

Long games may correlate strongly with particular archetypes, especially control.

Therefore every capped game must record:

```text
limit_type
turn
elapsed_time
current game state summary
```

and the evaluation harness must report the cap rate separately by deck/archetype.

The exact strategic treatment of a cap:

```text
DRAW
TRUNCATION
other explicitly defined protocol result
```

must be decided before RL evaluation begins and used consistently.

It must never be changed silently between algorithms.

## 7.4 Prohibited initial objectives

Do not initially optimize directly for:

```text
life total
damage dealt
card advantage
board value
creature power
```

because these can conflict with winning Magic.

Potential-based shaping remains an experimental arm, not the baseline.

Auxiliary prediction losses may be added later without changing the true environment reward.

---

# 8. Trajectory Format

Trajectory data must be designed for both on-policy and off-policy algorithms.

The logical schema defined here is separate from its physical storage encoding; see Section 19.6.

At minimum preserve:

```text
episode_id
step
seed

environment_version
observation_schema_version
action_schema_version

player
seat
deck_id
opponent_deck_id

own_policy_id
opponent_policy_id

observation
history_boundary
decision_type
legal_candidates
chosen_candidate

reward
terminal
truncated
termination_reason
```

Where available also preserve:

```text
behavior_log_probability
policy_version
recurrent_state_boundary / burn-in metadata
```

`behavior_log_probability` should be supported by the schema from the beginning, but may be absent for policies that do not provide a meaningful probability distribution, such as deterministic Forge heuristic decisions.

## 8.1 Callback / sub-decision traceability

Trajectory metadata must distinguish the Forge callback from synthetic agent decisions.

Additionally preserve:

```text
forge_callback_type
forge_callback_id

decision_sequence_id
subdecision_index

candidate_count
forced
```

This allows one Forge callback to generate:

```text
0 policy decisions
1 policy decision
N policy decisions
```

while remaining traceable back to the original engine interaction.

## 8.2 Decision Trace V2 and the Labeled-Sample Contract

The decision trace and the trajectory format converge on the same artifact: an observation, a legal candidate set, and a chosen action label. `DECISION_TRACE_V2` makes that contract explicit rather than implying it from a single flat record.

Records are split by lifecycle stage, because a "selected candidate" field cannot be populated at request-generation time:

```text
DECISION_TRACE_V2|REQUEST
    DecisionType, context/adapter/stage, step, forced
    legalCandidates with unique semantic keys
    no selection label

DECISION_TRACE_V2|RESULT
    references the preceding request
    terminal state
```

Terminal states are distinct rather than collapsed into one failure sentinel:

```text
CHOSEN             a candidate was selected
FORCED             exactly one legal candidate; no policy information
UNOBSERVED         no unambiguous teacher mapping seam exists
ENGINE_ROLLBACK    the action was legitimately cancelled by Forge
MAPPING_FAILED     mapping was attempted and no legal candidate matched
TRACE_INCOMPLETE   the trace ended mid-decision
```

The distinction matters for data-quality monitoring as much as for training: a rising unlabeled rate that cannot separate legitimate cancellation from a truncated trace would repeat the failure mode of Section 14.11.

### Behavior-cloning validity

A record is a valid labeled policy sample only if:

```text
RESULT == CHOSEN
and forced == false
and selectedCandidate is a member of legalCandidates
```

`FORCED` is excluded by construction, consistent with Section 4. This is not a formality: forced decisions carry no policy information, and including them inflates any reported top-1 accuracy with trivially correct predictions. Published collectible-card-game work found several percentage points of real improvement from filtering such records out of imitation data.

Everything else — `UNOBSERVED`, `ENGINE_ROLLBACK`, `MAPPING_FAILED`, `TRACE_INCOMPLETE` — is a diagnostic observation and never training data.

`DecisionTraceTrainingValidator` enforces the rule mechanically rather than leaving it to the consumer.

### Status and the remaining gap

```text
DECISION_TRACE_V2 format          CLOSED
teacher-label coverage            PARTIAL by decision family
```

The format is training-capable. Coverage is not yet complete: `TARGET` and `PAYMENT` in particular can still terminate as `UNOBSERVED` where no unambiguous mapping seam exists between the Forge teacher's action and a neutral candidate.

> Before the first behavior-cloning data collection, per-`DecisionType` label coverage must be measured and reported. A family with low `CHOSEN` share contributes observations but no supervision, and its absence from the training signal must be a known quantity rather than a discovery made after training.

---

# 9. Self-Play

Latest-policy-only self-play is acceptable as a diagnostic experiment but not as the planned production training distribution.

Long-term minimum:

```text
current policy
+
historical checkpoints
+
Forge heuristic anchor
```

Later, if required:

```text
prioritized opponent sampling
PFSP
dedicated exploiters
league training
```

Maintain a cross-play/payoff matrix between historical policies.

A policy improving only against its current opponent is not sufficient evidence of progress.

---

# 10. Robustness / Exploitability

Headline self-play win rate is insufficient.

ForgeAI should eventually measure robustness using:

```text
fixed evaluation opponents
historical checkpoint performance
cross-play matrix
held-out matchups
```

and later an approximate best-response / exploiter test.

This helps distinguish:

```text
strong policy
```

from:

```text
policy overfitted to its training population
```

---

# 11. Search

Naive MCTS over Forge's real `Game` state is prohibited for agent decision making because the underlying state contains hidden information.

Possible future search must respect the player's information set.

Candidates include:

```text
determinization
ISMCTS
belief-state search
ReBeL-inspired public-belief search
```

Search is not part of the initial ForgeRL implementation.

Before introducing it, Forge game-copy correctness and performance must be benchmarked.

Local tactical search may be evaluated before global imperfect-information search.

---

# 12. CFR / ReBeL / MuZero

## CFR / Deep CFR

Theoretically relevant for two-player imperfect-information games.

Not selected as the initial full-Forge learner because the required game-tree traversal is likely impractical at Forge scale.

## ReBeL

Interesting long-term direction for policy/value learning plus imperfect-information search.

Not an initial implementation target.

## MuZero / learned dynamics

Not currently justified.

Forge already provides exact game dynamics.

A learned world model should only be reconsidered if profiling later proves that exact Forge rollouts are the dominant bottleneck and a learned surrogate can provide materially cheaper, sufficiently accurate search rollouts.

---

# 13. Current Open Algorithm Questions

The production online learner remains intentionally undecided.

There are two independent performance hypotheses.

## 13.1 Experience Scarcity Hypothesis

Question:

> Are Forge interactions expensive enough that long-lived experience reuse materially improves strength per Forge interaction?

Primary challenger family:

```text
R2D2 / replay learning
or
replay-capable actor-critic
```

Primary measurements:

```text
strength / Forge callback
strength / atomic agent decision
strength / wall-clock
```

## 13.2 Actor Throughput Hypothesis

Question:

> Can multiple independent Forge JVM actors generate useful fresh experience efficiently enough that asynchronous actor/learner execution materially improves wall-clock learning?

Primary challenger:

```text
IMPALA / V-trace
```

Primary measurements:

```text
aggregate Forge callbacks/sec
aggregate atomic decisions/sec
worker scaling efficiency
actor idle time
policy lag
wall-clock learning
```

These hypotheses are independent.

Possible outcome:

```text
experience expensive
AND
process scaling good
```

is entirely valid.

In that case, the architecture should investigate a replay-capable distributed actor-critic rather than choosing between replay and asynchronous actors as mutually exclusive concepts.

---

# 14. FRL-00.5 Measurement Contract

FRL-00.5 occurs before the final decision-decomposition policy is complete.

Therefore it must not report a single ambiguous metric called:

```text
agent decisions / second
```

as if that value already had fixed semantics.

Instead measure the layers separately.

## 14.1 Forge-native callback throughput

Measure:

```text
PlayerController callbacks / game
PlayerController callbacks / second

latency by callback type
p50
p95
p99
```

This metric is independent of ForgeRL's future decomposition choices.

## 14.2 Candidate-structure distribution

For every callback where legal alternatives can be inspected safely, measure:

```text
candidate count

mean
median
p90
p95
p99
max
```

grouped by decision family.

Especially inspect:

```text
ATTACK
BLOCK
PAYMENT
ORDER
TARGET
CARD_SELECTION
PRIORITY_ACTION
```

The purpose is to identify where direct enumeration is feasible and where decomposition is required.

If complete legal outcomes cannot be enumerated without combinatorial expansion, report that fact rather than generating them purely for benchmarking.

## 14.3 Forced-callback opportunity

Where possible, measure callbacks with:

```text
exactly 1 legal result
```

separately.

Do not assume that callback count equals future policy-step count.

## 14.4 Projected atomic decision throughput

Only after a decomposition policy is proposed may FRL-00.5 estimate:

```text
projected atomic agent decisions/game
projected atomic agent decisions/sec
```

Such numbers must be explicitly labeled:

```text
PROJECTED
```

until the real DecisionRequest environment exists.

## 14.5 Process scaling

Still measure:

```text
1 JVM
2 JVMs
4 JVMs
8 JVMs where practical
```

using Forge-native callback throughput and games/hour.

This is sufficient to evaluate whether multi-process actor scaling is promising before the full RL interface exists.

## 14.6 Resource, stability and search-readiness metrics

Also measure:

```text
cold JVM startup
warm game/reset time

games/hour

RAM per JVM
CPU utilization
GC behavior

timeout/crash/invalid rate
```

Also measure `GameCopier` latency where practical for future search evaluation.

The benchmark must distinguish proactive from reactive workloads where possible.

## 14.7 FRL-00.5 Process-Scaling Result

FRL-00.5 measured the following approximate aggregate Forge-native callback throughput:

```text
1 worker     267 callbacks/s
2 workers    388 callbacks/s
4 workers    542 callbacks/s
6 workers    548 callbacks/s

6 workers,
ActiveProcessorCount=1
              571 callbacks/s

6 workers,
ActiveProcessorCount=2
              584 callbacks/s

8 workers    489 callbacks/s
```

The correct interpretation is:

```text
multi-process scaling = MODERATE
```

not linear or near-linear.

The practical scaling knee on the benchmark machine is approximately:

```text
4-6 JVM workers
```

Six workers produce roughly twice the throughput of one worker, not six times the throughput.

Eight workers already reduce aggregate throughput.

Therefore ForgeAI should not assume that adding actors will continue increasing environment throughput proportionally.

The measured result supports a small asynchronous actor pool, but does not by itself establish distributed actor throughput as the dominant ML optimization target.

## 14.8 JVM Processor Visibility

FRL-00.5 found only a modest throughput change from constraining JVM processor visibility.

At six workers:

```text
Default
~548 callbacks/s
~1.15 GB mean RSS/worker

ActiveProcessorCount=1
~571 callbacks/s
~510 MB mean RSS/worker

ActiveProcessorCount=2
~584 callbacks/s
~1.38 GB mean RSS/worker
```

The primary architectural value of:

```text
-XX:ActiveProcessorCount=1
```

is therefore currently **memory/resource isolation**, not a major throughput improvement.

It may leave substantially more host RAM available for:

```text
replay
learner process
IPC buffers
dataset caching
```

while sacrificing little Forge throughput relative to the highest measured configuration.

No value is yet designated as the production default.

The measurement must be repeated with the real ForgeRL controller and learner workload before fixing JVM worker settings.

Note that the observed RSS values are not monotonic in processor visibility. This should be re-checked before any value is treated as a resource guarantee.

## 14.9 FRL-00.5 Trajectory-Size Measurements Are Diagnostics Only

The FRL-00.5 compressed callback-size measurements must be labeled:

```text
CALLBACK METADATA ONLY
NOT A REPLAY BUFFER ESTIMATE
```

because they do not contain:

```text
Observation
LegalCandidate identities/features
history/event representation
recurrent sequence context
```

No replay-capacity conclusion may be drawn from those byte counts.

## 14.10 AI-Controller Benchmark Coverage Bias

Any benchmark driven by Forge's own heuristic controller measures **that controller's decision path**, not the environment's decision surface.

Two independent observations establish this:

```text
FRL-00.5
93-98% of callbacks exposed no candidate count,
dominated by PRIORITY_ACTION
(79% of baseline, 65% of reactive callbacks)

FRL-02D
raw X callbacks     0 / 0
neutral X requests  0 / 0
across 2 x 10 benchmark games,
because the Forge AI preselects the value,
while focused fixtures show the same boundary
generating candidates at p50 ~29 ms
```

The rule is therefore:

> **Counts obtained through the Forge-AI controller path are a lower bound on decision-surface coverage. They are controller-path-specific and must never be read as environment properties.**

Explicitly prohibited inference:

```text
0 callbacks observed
→ "this DecisionType does not occur in the environment"
```

The only supported reading is:

```text
0 callbacks observed
→ "this DecisionType was not exercised by the Forge
   controller path in this slice"
```

Every reported coverage or frequency figure must be labeled with the policy that drove the run.

Only a `RandomLegalPolicy`-driven or otherwise policy-driven ForgeRL run produces coverage and frequency numbers usable for throughput estimation.

### Accumulated zero-coverage observations

The effect was first recorded in FRL-02A, which found that the Forge AI frequently preselects targets and X values inside its priority heuristic, so an AI trace does not expose all decisions a future external controller must make. Subsequent milestones produced repeated instances:

```text
FRL-02D  X_VALUE          0 / 0   in both 10-game matchups
FRL-02E  MODE             0 / 0   in the proactive matchup
FRL-02F  CARD_SELECTION   0       discard callbacks in the proactive matchup
```

None of these is evidence that the decision type is rare in Magic. Each is evidence that the Forge heuristic controller did not reach it in that slice.

The proactive matchup in particular has now produced zero coverage for three separate decision types. A benchmark slice can be adequate for throughput and inadequate for coverage at the same time; the two properties must be reported separately.

## 14.11 Determinism: the FRL-02K0 Finding

An apparent single-milestone score deviation triggered a full determinism audit. The audit found something different and worse than the deviation it was chasing.

### There was no outlier

Replaying every milestone twice from its own merge commit:

| Milestone | Retained report | Replay A | Replay B |
|---|---:|---:|---:|
| FRL-02C | 5-5 | 5-5 | 3-7 |
| FRL-02D | 5-5 | 3-7 | 3-7 |
| FRL-02E | 3-7 | 5-5 | 5-5 |
| FRL-02F | 5-5 | 5-5 | 5-5 |
| FRL-02G | 3-7 | 3-7 | 5-5 |

The reported score was unstable at **every** milestone, and same-commit repeats disagree with each other. The earlier reading of FRL-02E as "the deviation" was an artifact of five samples of a process-dependent value.

Meanwhile the underlying traces were identical everywhere. Gameplay hash, RNG hash, and a 4,018-record common priority-teacher projection matched at every milestone, and every adjacent comparison C-D, D-E, E-F, F-G returned first divergence `-1`.

### Root cause

```text
PriorityActionDiagnostics.capture
  -> PriorityActionProvider.generatePriorityRequest
  -> COST_ADJUSTMENT_CHOICE_REQUIRED
  -> UnsupportedPriorityActionException escapes the diagnostic boundary
  -> game loop aborts
  -> simulator finally-block: setGameOver(Draw)
  -> Player.onGameOver marks both unresolved players as winners
  -> GameOutcome stores both in an identity-keyed HashMap
  -> the winner readout returns the first entry, which is process-dependent
```

Two of ten games in the reactive matchup aborted mid-game at every milestone since FRL-01A. The score instability was never a determinism problem in the game; it was an invalid-state readout that **masked a 20% abort rate** for five consecutive milestones.

The original hypothesis — probe-side RNG consumption — was wrong.

### Neutrality has three categories, not two

State neutrality and RNG neutrality were both necessary and together still insufficient. Neutral priority previews advanced the global static `SpellAbility` identifier counter: neither game state nor randomness, but process-wide.

> A neutral boundary must not perturb **any** global process state: game state, random number generation, or global counters and identifier sequences.

The fix gives audit-created abilities request-local negative identifiers, restores live ability activators in a `finally` block, and preserves full `getAllPossibleAbilities` semantics including Forge alternative costs. A regression asserts that the global ability-ID sequence advances by exactly one between two sentinel allocations.

All nine decision families are now verified state-neutral and RNG-neutral with zero draws consumed:

```text
PRIORITY_ACTION  TARGET  PAYMENT  X_VALUE  MODE
CARD_SELECTION   ATTACK  BLOCK    MULLIGAN
```

### The detection was luck, and that is the argument for the gate

Five score samples happened to contain one differing value. Had they agreed, nothing would have been investigated and two of ten games would still be aborting silently.

Final score is not a determinism detector: two runs can diverge on turn three and still finish with the same record. The permanent gates replace the lucky observation with a guaranteed one:

```text
same seed, twice          -> identical gameplay / RNG / decision hash
diagnostics OFF vs ON     -> identical gameplay / RNG hash and draw count
per-family probe check    -> unchanged FORGE_STATE_V1, zero RNG draws
ambiguous outcomes        -> rejected, never resolved from unordered iteration
```

Traces are SHA-256 over UTF-8 canonical records at three levels — decision, gameplay state fingerprint, and RNG draw — with a first-divergence reduction so any mismatch resolves to a last-equal/first-different index.

### Collector self-neutrality

The gate initially proved that priority and mulligan diagnostics are neutral — using a trace collector that was itself active in every run. That is a measurement-apparatus problem: an instrument cannot validate itself.

The gap was narrower than it looks. Diagnostics OFF-versus-ON remained valid (the collector was present on both sides), and the root cause stands independently because the aborts appear in pre-collector reports. What was *not* proven was that the shipping build behaves like the instrumented one — which is exactly the claim a benchmark needs.

It is now proven at full-game scale. Four child-JVM runs:

```text
collector OFF-A    collector OFF-B
collector ON-A     collector ON-B
```

compared over channels that do not depend on the collector:

```text
ReferenceGameplay projection
priority projection
RNG trace and draw count
final Forge state
outcome
```

All four runs were identical on every channel. This is no longer a local attach/snapshot/hash assertion.

The observer effect is not eliminated — the reference hasher is itself an observer. It is reduced to something small enough that its neutrality is verifiable by reading it rather than by measuring it. That is the achievable goal.

### Reproducibility status

Production checkpoint: PR #11, head `676c941`, merge `c8835a22`.

| Matchup | Seed | Runs | Result | Hashes |
|---|---|---:|---:|---|
| Dead and Alive vs Air Forces | 20260809 | 4 (OFF A/B, ON A/B), 40 games | 7-3 | all identical |
| Izzet Guild Kit vs Dimir Guild Kit | 20260810 | 4 (OFF A/B, ON A/B), 40 games | 3-7 | all identical |
| V2 cohort repeats | — | 40 games | — | 0 canonical trace differences |

The proactive cohort reproduces its pre-fix callback and request counts exactly, confirming that it never contained an aborted game. Only the reactive cohort was contaminated.

## 14.12 Worker Output Isolation

Single-process determinism does not imply multi-process integrity. Diagnostic sinks write to configured paths; six workers pointed at the same paths would interleave or overwrite, and the result would look like nondeterminism.

The resolution is a shared output namespace owned by the launcher, not per-class process-ID logic:

```text
<outputRoot>/<runId>/worker-000/
    priority.csv
    mulligan.csv
    determinism/

<outputRoot>/<runId>/worker-001/
    ...
```

Path resolution order, with fail-fast on partial configuration:

```text
explicit per-sink path
  >  derived outputRoot / runId / workerId
  >  disabled
```

A full worker namespace enables all three sinks together.

### Two-JVM smoke result

```text
exit codes              0 / 0
path collisions         0
parse errors            0
process interleaving    0

GAMEPLAY_TRACE_V1       byte-identical
RNG_TRACE_V1            byte-identical
DECISION_TRACE_V2       byte-identical
PRIORITY_REFERENCE_V1   identical
```

Byte-identical traces across two simultaneous JVMs at the same seed prove isolation and per-process determinism together — a stronger property than absence of collision, and the one the multi-process benchmark actually depends on.

The child-JVM gates were initially Windows-specific through a hardcoded `java.exe`; they are now portable across Windows, Linux and macOS, so they can run permanently rather than in an environment-specific profile.

**Status: gate satisfied ahead of the multi-process REAL ForgeRL benchmark.**

---

# 15. Decision After FRL-00.5

FRL-00.5 materially reduced uncertainty but did not yet select the production learner.

Measured findings:

```text
Forge native execution:
faster than initially feared

multi-process scaling:
useful but moderate

future atomic agent-step rate:
unknown

replay value:
unknown

priority-action candidate structure:
unknown
```

The prior assumption that Forge would make PPO obviously unaffordable is therefore rejected.

PPO remains a fully viable baseline and potential production learner.

FRL-00.5 does **not** show that replay is mandatory.

It also does **not** show that asynchronous actor scaling is sufficient to prefer IMPALA/V-trace.

Current status:

```text
PPO
    viable

R2D2 / replay
    still a serious challenger

IMPALA / V-trace
    potentially useful,
    but actor scaling is only moderate

replay-capable actor-critic
    interesting combined hypothesis,
    not selected
```

The next algorithm decision must wait for measurements from the real atomic ForgeRL interface.

## 15.1 Throughput Interpretation

Using the highest native callback throughput measured by FRL-00.5:

```text
~584 callbacks/s
```

ten million native callbacks correspond to only several hours of aggregate Forge execution.

This is substantially faster than the pre-benchmark expectation.

However:

```text
Forge callback
!=
atomic ML decision
```

and existing callback measurements include Forge's current AI/controller behavior rather than the future external-policy path.

Therefore callback throughput must not be used directly as an RL sample-throughput estimate.

In particular, the project must still measure:

```text
atomic DecisionRequests/sec
observation encoding cost
legal-candidate encoding cost
IPC cost
policy inference cost
learner contention
```

before deciding whether PPO's on-policy sample use is economically unacceptable.

This list is expanded into a per-layer, per-DecisionType cost model in Section 15.4.

## 15.2 Current Scaling Classification

For the decision matrix, FRL-00.5 currently places local execution closer to:

```text
interaction cost:
UNKNOWN / apparently moderate

process scaling:
MODERATE
```

rather than confidently assigning ForgeAI to:

```text
expensive interaction
+
good scaling
```

The algorithm matrix in Section 15.3 remains useful, but the final quadrant assignment is deferred until atomic ForgeRL measurements exist.

## 15.3 Reference Decision Matrix

Interpret the throughput benchmark as a two-axis matrix.

```text
                         PROCESS SCALING
                    poor                good

EXPERIENCE   cheap     PPO/simple       PPO or V-trace
COST                  baseline likely  depending actor bottleneck

             costly    Replay priority  Distributed replay /
                                     replay-capable actor-critic
```

More explicitly:

### Forge interaction cheap + poor process scaling

PPO remains a strong default.

Do not add distributed complexity without evidence.

### Forge interaction cheap + good process scaling

PPO remains viable.

If synchronous rollout collection wastes throughput, test:

```text
PPO vs IMPALA/V-trace
```

### Forge interaction expensive + poor process scaling

Experience reuse becomes the primary concern.

Test:

```text
PPO vs R2D2/replay
```

or another replay-capable learner.

### Forge interaction expensive + good process scaling

Both hypotheses matter.

Prefer testing an architecture that supports:

```text
asynchronous actors
+
experience replay
+
stochastic policy where appropriate
```

A replay-capable actor-critic becomes especially interesting.

PPO remains the reference baseline.

## 15.4 Atomic Decision Cost Model

The cost of one atomic agent decision is not a single number. It must be decomposed and each layer measured separately:

```text
Forge transition / callback latency
candidate generation latency
candidate encoding latency
observation encoding latency
IPC
policy inference
candidate application
```

`candidate generation` is a distinct cost class from `candidate encoding`, and the measured spread across decision types justifies separating them:

```text
FRL-02C PAYMENT request generation
p50 274.8 us / p95 616 us / p99 829.8 us

FRL-02D X_VALUE request generation
p50 29.066 ms
```

Two orders of magnitude. X is expensive because it runs full payment-feasibility probes per candidate; PAYMENT is not. A single aggregate "generation latency" would hide exactly the variation that matters.

Therefore measure, per DecisionType:

```text
generation latency p50 / p95 / p99
generations per game
candidates per request
feasibility / rule probes per request
feasibility / rule probes per candidate where meaningful
```

### Latency alone is not a throughput statement

A decision type's contribution to throughput is:

```text
generation cost x frequency
```

Section 14.10 establishes that frequency measured through the Forge-AI controller path is a lower bound. X_VALUE is the extreme case: p50 ~29 ms generation at an AI-path frequency of zero.

Reports must therefore give **both**:

```text
per-request latency
aggregate ms per game
```

and the aggregate is only valid under a policy-driven run.

### Generation-cost gate

A DecisionType whose generation p50 exceeds a defined multiple of the Forge transition p50 must be flagged before it enters the atomic decision path, together with the dominant probe class responsible.

The purpose is to name the cost at the boundary where it is introduced, rather than discovering an unexplained aggregate at the REAL ForgeRL benchmark and having to attribute it afterwards.

### Measured generation latency by decision type

Revalidated on the fixed head, reactive cohort (Izzet/Dimir, seed 20260810, ten games, diagnostics ON):

| DecisionType | Generation p50 | p95 | p99 | Native callback p50 | Generation / native at p50 |
|---|---:|---:|---:|---:|---:|
| `MULLIGAN` keep/redraw | below timer resolution | — | — | 27.1 us | — |
| `CARD_SELECTION` | 42.8 us | 136.0 us | 896.8 us | 63.2 us | 68% |
| `MULLIGAN_BOTTOM` | 45.7 us | 64.8 us | 64.8 us | 165.3 us | 28% |
| `ATTACK` | 70.0 us | 208.4 us | 343.5 us | 7.677 ms | 0.9% |
| `TARGET` | 187.6 us | 447.9 us | 447.9 us | not available | — |
| `BLOCK` | 260.7 us | 829.1 us | 1.395 ms | 4.647 ms | 5.6% |
| `PAYMENT` | 370.9 us | 933.6 us | 1.395 ms | not available | — |
| `MODE` | 1.037 ms | 59.778 ms | 59.778 ms | not available | — |
| `PRIORITY_ACTION` | 1.266 ms | 9.937 ms | **127.270 ms** | 3.320 ms | 38% |
| `X_VALUE` (focused fixture) | 31.56 ms | 40.26 ms | 48.98 ms | n/a | — |

A `0` reading for `MULLIGAN` keep/redraw means below timer resolution, not free.

`MODE` p95/p99 rest on two requests and are not a usable estimate.

### The generation-cost gate has fired: PRIORITY_ACTION

For combat the neutral layer is cheap relative to the controller it replaces — `ATTACK` generation is under 1% of the native callback, `BLOCK` under 6%. That result does not generalise.

`PRIORITY_ACTION` is the dominant family, at 5,120 of roughly 6,600 observations in the cohort, and it inverts the relationship at the tail:

```text
p50   generation 1.266 ms   vs native 3.320 ms     38%
p95   generation 9.937 ms   vs native 24.942 ms    40%
p99   generation 127.270 ms vs native 43.267 ms   294%
```

At the 99th percentile, generating the neutral candidate set costs roughly three times the Forge AI callback it is meant to replace. On the family that accounts for around three quarters of the decision surface, this is the case Section 15.4's gate exists for.

Required before the REAL ForgeRL benchmark:

```text
identify the dominant probe class in the PRIORITY_ACTION tail
report probes per request and per candidate for that family
decide whether the tail is a card-specific pathology or structural
```

`X_VALUE` remains a separate outlier for a known reason: a full payment-feasibility probe per candidate. Its architecture was not changed by the determinism gate.

## 15.5 Measured Callback-to-Request Ratios

Section 3.7 states that Forge callback count is not agent decision count. Both cohorts are now revalidated on the fixed head, and the direction is family-specific rather than uniform.

| Family | Proactive callbacks → requests | Ratio | Reactive callbacks → requests | Ratio |
|---|---:|---:|---:|---:|
| `PRIORITY_ACTION` | not separately reported | — | 5,120 → 5,001 | 0.977 |
| `TARGET` | not separately reported | — | 3 → 3 | 1.000 |
| `PAYMENT` | 789 → 430 | 0.545 | 1,292 → 303 | 0.235 |
| `MODE` | not separately reported | — | 7 → 2 | 0.286 |
| `CARD_SELECTION` | not separately reported | — | 34 → 37 | 1.088 |
| `ATTACK` | 122 → 239 | 1.959 | 126 → 221 | 1.754 |
| `BLOCK` | 37 → 65 | 1.757 | 21 → 61 | 2.905 |
| `MULLIGAN` keep/redraw | not separately reported | — | 24 → 24 | 1.000 |
| `MULLIGAN_BOTTOM` | not separately reported | — | 4 → 4 | 1.000 |

Combined `PAYMENT` across both cohorts:

```text
2,081 callbacks -> 733 requests     ratio 0.352
```

This supersedes the earlier combined figure of `1,637 -> 655` (ratio 0.40), which mixed a clean proactive run with a reactive run containing two aborted games.

`PAYMENT` **collapses**: Forge issues many fine-grained payment callbacks that the neutral layer groups into fewer atomic decisions, and the reactive collapse is steeper than the proactive one. Combat **expands**, by roughly a factor of two. `PRIORITY_ACTION`, the dominant family, is close to one-to-one.

Because `PRIORITY_ACTION` and `PAYMENT` together dominate volume, the aggregate atomic-decision rate is likely to land **at or below** the raw callback rate rather than above it. No projection assuming a single direction is valid.

### Measured forced share

Section 4 auto-resolves atomic decisions with exactly one legal candidate.

| Family | Proactive forced share | Reactive forced share |
|---|---:|---:|
| `PRIORITY_ACTION` | — | 27.2% |
| `PAYMENT` | 14.9% | 24.8% |
| `ATTACK` | 29.3% | 24.4% |
| `BLOCK` | 27.7% | 42.6% |
| `TARGET`, `MODE`, `CARD_SELECTION`, `MULLIGAN` | — | 0% |

Combined `PAYMENT` forced share is 18.96%.

FRL-00.5 estimated 73-85% forced, but over the 2-7% of callbacks whose candidate count was observable — a sample biased toward inherently forced types. The measured range over real atomic requests is roughly 0% to 43% depending on family, clustering near a quarter for the volume families.

The auto-resolution saving is real and considerably smaller than the original estimate. Cohorts must not be mixed when quoting these figures.

### Downstream callbacks per action

FRL-02A measured, per non-PASS priority action:

```text
proactive   mean 1.810   p50 1   p95 5   max 8
reactive    mean 2.046   p50 1   p95 5   max 7
```

All callback-free actions in the sample were land plays.

### Coverage caveat

119 unsupported `PRIORITY_ACTION` observations occurred in the ten-game reactive cohort, roughly 2.3% of priority decisions. They now fail as explicit diagnostic states rather than escaping into the game loop, but they remain a real coverage hole that a `RandomLegalPolicy` run will encounter.

---

# 16. Initial Benchmark Matchup Policy

The initial learning/evaluation slice must exercise both proactive and reactive play.

Do not select two purely proactive decks merely because they produce faster or easier learning.

The initial controlled benchmark should use:

```text
one proactive deck
vs.
one interactive/reactive deck
```

where practical.

The matchup should exercise at least some of:

```text
priority passing with alternatives
instant-speed interaction
mana held for responses
target selection
combat choices
stack interaction
resource reservation
```

The exact decks must be selected from known-good Forge-compatible decks and documented before the algorithm bake-off.

If the initial two-deck environment cannot exercise these behaviors, its results must be labeled as a limited proactive benchmark rather than evidence of general Magic gameplay competence.

## 16.1 FRL-00.5 Reactive-Workload Caveat

The FRL-00.5 reactive workload is useful evidence that interactive Magic produces more callbacks and higher tail latency:

```text
~33% more callbacks/game
higher p95/p99 callback latency
roughly half the games/hour
```

However, it must not be treated as an upper bound on realistic reactive/control workload.

The current Forge heuristic is itself limited in complex reactive play.

Therefore:

> **FRL-00.5 establishes that reactive workloads are measurably more expensive in the tested slice; it does not establish the maximum cost of strong reactive Magic play.**

Future model evaluation must retain a proactive-vs-reactive workload split rather than collapsing both into one throughput or strength number.

---

# 17. Engineering Timebox

Algorithm-neutral architecture is a means to enable experiments, not an objective by itself.

ForgeRL must avoid indefinitely postponing learning experiments in pursuit of complete abstraction.

Each engineering milestone should therefore have an explicit time budget before implementation begins.

If a milestone exceeds its time budget, reassess scope.

Acceptable responses include:

```text
reduce supported decision surface
restrict the initial deck/card pool
defer rare decision types
replace generic abstraction with a narrower v0 contract
record unsupported decision types explicitly
```

Do not silently expand the milestone indefinitely.

The initial ForgeRL v0 objective remains:

> Make one controlled, representative 1v1 matchup externally playable and measurable through the complete decision interface required by those decks.

It is not:

> Solve every possible Forge decision type before the first ML experiment.

Unsupported decisions outside the controlled v0 card/deck slice may remain explicitly unsupported until the environment expands.

The project should prefer:

```text
small complete experimental environment
```

over:

```text
large theoretically elegant but unfinished environment
```

---

# 18. Current Roadmap

```text
FRL-00
Vanilla Forge baseline
        ✅ PASS

        ↓

FRL-00.5
Forge-native throughput
+ decision-surface benchmark
        ✅ PASS

        ↓

FRL-01A
Priority Legal-Action Boundary
+ first real DecisionRequest
+ priority candidate measurements

        ↓

FRL-01B+ / FRL-02x
remaining algorithm-neutral
decision boundaries
        ✅ through CONFIRMATION

        ↓

ORDER Attribution Audit
separate live ORDER families
from legacy combat ordering

        ↓

modern DAMAGE_ASSIGNMENT
information barrier,
no premature leak

        ↓

Runtime Gap Audit
collect known PAYMENT gaps
and any new ones

        ↓

Gap Closure
primarily VARIABLE_MANA_OUTPUT
/ PAYMENT

        ↓

ZERO-UNSUPPORTED v0 gate

        ↓

RandomLegalPolicy
completes representative full games

        ↓

REAL ForgeRL benchmark

atomic agent decisions/sec
candidate generation latency by decision type
observation bytes/decision
candidate bytes/decision
trajectory bytes/decision
IPC latency
inference latency
learner contention

        ↓

Training-Safety / Issue Gate

        ↓

Forge heuristic trajectory collection

        ↓

Behavior Cloning

        ↓

Online RL bake-off

        ↓

Select learner using:
win rate
+ Forge interactions
+ wall-clock
+ stability
+ robustness

        ↓

Historical / league self-play

        ↓

Multi-deck/card generalization

        ↓

Optional information-safe search
```

## 18.1 FRL-01A — Priority Legal-Action Boundary

The first ForgeRL environment task should focus specifically on the dominant unresolved decision surface:

```text
PRIORITY_ACTION
```

FRL-00.5 observed that priority-related callbacks account for the majority of measured controller callbacks, while their general legal candidate set remains unavailable through the existing instrumentation.

FRL-01A should determine and expose:

```text
all legal top-level actions for the acting player

PASS

playable land actions

castable spells

activatable abilities

other priority actions where applicable
```

from the correct player-perspective game state.

It must distinguish:

```text
PASS is the only legal choice
```

from:

```text
PASS while alternatives exist
```

The latter remains a genuine strategic policy decision.

FRL-01A must also decide, and document, which activations count as top-level priority alternatives. Mana abilities in particular are rarely meaningful as standalone priority choices; if they are exposed at top level, most priority windows will be classified as non-forced and the measured decision rate will be dominated by them. If they are hidden at top level, payment decomposition in Section 3.5 must reintroduce them.

FRL-01A should directly measure:

```text
priority requests/game

candidate-count:
mean
median
p90
p95
p99
max

forced priority requests

PASS with alternatives

priority DecisionRequests/sec

encoded bytes per priority candidate
```

This measurement is part of implementation, not a separate FRL-00.6 benchmark.

The purpose is simultaneously to:

1. establish the first real `DecisionRequest`,
2. solve the largest unresolved action-interface boundary,
3. measure the callback→agent-step transformation,
4. establish the first real ML-facing throughput number.

## 18.2 ORDER Attribution Audit

`ORDER` is a single engine callback covering several unrelated decision families. FRL-00.5 counted 591 and 419 calls across the two matchups with a median of one item and a maximum of three to four, but that aggregate cannot be attributed to a family and therefore cannot inform a representation decision.

Attribute separately at minimum:

```text
orderSimultaneousSa            simultaneous triggered abilities
scry / surveil ordering        partition followed by ordering
zone ordering
legacy combat ordering         orderBlockersForDamageAssignment
                               orderAttackersForDamageAssignment
```

Record per subtype, not in aggregate:

```text
call count per game
candidate count: mean / p50 / p95 / max
forced share
generation latency p50 / p95 / p99
```

Per-subtype counts are required because the subtypes grow differently. Four simultaneous triggers are twenty-four permutations. A scry three is a partition into keep and bottom followed by an ordering of the kept cards — structurally a different decision, not a longer one. An aggregate distribution cannot decide enumerate-versus-decompose for either.

The audit must also answer one specific question, because Section 3.9 depends on it:

> Does `orderBlockersForDamageAssignment` fire in the v0 slice, how often, and with how many items?

If it fires, "legacy combat ordering" is not legacy — it is the model this Forge version actually runs, and `DAMAGE_ASSIGNMENT` must be designed against that.

Only after attribution should the type be split, for example into `TRIGGER_ORDER`, `ZONE_ORDER`, and `PARTITION_PLUS_ORDER`. Deferring the aggregate as "legacy" would defer live strategic decisions along with the dead one: when several triggered abilities controlled by the same player go on the stack simultaneously, that player chooses their order, and the order can change the result.

## 18.3 Runtime Gap Audit and the Zero-Unsupported Gate

Purpose: establish that a random legal policy can complete games **entirely through the neutral interface**, with no silent recourse to the Forge heuristic.

Exit criterion for the defined v0 random-policy runs:

```text
0 agent-relevant UNSUPPORTED
0 Forge-AI fallbacks
0 MAPPING_FAILED
0 TRACE_INCOMPLETE
```

Deliberately **not** counted:

```text
UNOBSERVED       a teacher-label coverage problem for behavior
                 cloning, not a blocker for an external controller

engine-owned and forced operations
                 not policy decisions at all
```

Section 8.2 keeps these in separate terminal states precisely so that they can be counted separately here.

### The criterion must be enforced, not observed

A Forge-AI fallback in a random-policy run must **throw**. `MAPPING_FAILED` and `TRACE_INCOMPLETE` must be hard failures in that configuration, not rows someone counts afterwards.

A gate that depends on a person noticing entries in a CSV is the same construction that carried the abort in Section 14.11 through five consecutive milestones. The lesson there was not "look more carefully" — it was that a criterion which can be passed by inattention is not a criterion.

### Known largest gap

`PAYMENT`, not `DAMAGE_ASSIGNMENT`, is the current blocker. `DAMAGE_ASSIGNMENT` is a well-bounded new boundary; `PAYMENT` is a measured hole in an existing one, with roughly 22.6% of raw payment callbacks in the reactive matchup carrying a known unsupported feature. Gap Closure therefore precedes the gate, and `VARIABLE_MANA_OUTPUT` is its primary target.

---

# 19. Compute Provider and Training Portability

Compute infrastructure must remain subordinate to algorithm selection.

ForgeAI must not select a learning algorithm because it happens to fit Kaggle, Colab, a local GPU, or another specific compute provider.

## 19.1 Provider Neutrality

The ML system must support interchangeable training environments where practical:

```text
local CPU/GPU
Kaggle
Colab
rented cloud GPU
future dedicated training machine
```

Forge itself must not know which training provider is used.

The boundary remains:

```text
Forge
  ↓
versioned trajectories / replay data
  ↓
training system
  ↓
versioned checkpoint
  ↓
Forge evaluation
```

Provider-specific code belongs outside the Forge environment contract.

## 19.2 Compute Provider Must Not Bias the Algorithm Bake-off

Kaggle suitability is explicitly not an algorithm-selection criterion.

The primary online-RL bake-off must compare algorithms on the same compute basis.

For example:

```text
PPO
vs.
R2D2 / replay learner
vs.
V-trace / actor-critic
```

must not become:

```text
algorithm A runs locally
algorithm B runs on Kaggle
```

for the primary scientific comparison.

Otherwise measured differences combine:

```text
algorithm
+ hardware
+ actor topology
+ IPC
+ provider limits
+ runtime availability
```

and the result no longer isolates the learning method.

Therefore:

> The primary bake-off runs on a common compute basis.

Because Forge is CPU-bound and the actor/learner loop must stay intact for the online candidates, that common basis is the machine that runs the Forge workers.

External GPU providers may be used after the comparison for scaling, secondary experiments, pretraining, or workloads that are naturally provider-independent.

## 19.3 Appropriate Kaggle Workloads

Kaggle is particularly suitable for bounded GPU-centric jobs whose input already exists as a dataset.

Examples:

```text
Behavior Cloning
card-embedding training
representation pretraining
value pretraining
auxiliary supervised objectives
offline-RL experiments
large model ablations
Transformer experiments
```

Conceptually:

```text
versioned dataset
      ↓
GPU training job
      ↓
versioned checkpoint
```

This has no requirement for a low-latency connection to Forge.

## 19.4 Online RL Must Remain Online

Do not describe this workflow as R2D2:

```text
Forge workers
    ↓
static replay dump
    ↓
upload
    ↓
remote GPU trains for hours
    ↓
checkpoint downloaded later
```

Once the dataset is fixed for the duration of training and no meaningful actor ↔ learner feedback loop exists, this is an offline/batch learning workflow.

R2D2-style learning instead conceptually requires:

```text
Actors
   ↓
prioritized sequence replay
   ↓
Learner
   ↓
updated weights
   ↓
Actors
   ↓
new experience
   ↺
```

with policy/recurrent-state staleness controlled as part of the algorithm.

If static historical Forge data is trained remotely, classify the experiment as:

```text
Behavior Cloning
offline value pretraining
offline RL
```

as appropriate.

If offline RL is used, select a method designed for static-data distribution shift rather than silently treating ordinary online replay RL as offline RL.

## 19.5 Online PPO / V-trace / R2D2 and Remote Compute

Remote GPU use for online RL is allowed only when the full architecture preserves the intended actor/learner feedback cycle.

Possible architectures include:

```text
same machine:
Forge actors + learner

or

low-latency distributed system:
Forge actors
    ↕
remote learner
```

The feasibility of the second architecture depends on measured:

```text
network latency
network reliability
trajectory bandwidth
checkpoint/update frequency
actor policy lag
serialization overhead
```

Do not assume a notebook service is suitable merely because it provides a GPU.

## 19.6 Trajectory Storage Encoding

The logical trajectory schema defined in Section 8 is separate from its physical storage encoding.

Large training datasets should not default to one giant JSON-lines file.

The storage layer must support:

```text
versioned shards
partial dataset consumption
streaming/batched reads
compression
integrity checking
resume/retry
```

Candidate encodings include:

```text
Parquet shards
NPZ shards
or another explicitly benchmarked binary/columnar representation
```

The physical format remains an implementation choice and must be benchmarked against the actual observation/action representation.

Every shard must carry or reference:

```text
environment_version
observation_schema_version
action_schema_version
trajectory_format_version
```

Datasets must remain usable incrementally; partial uploads should not require the complete corpus to be regenerated.

## 19.7 Data Movement Is a Compute Constraint

Before adopting a remote GPU workflow, measure:

```text
compressed bytes / decision
compressed bytes / episode

dataset generation rate
local disk throughput
upload throughput
download throughput

dataset shard size
checkpoint size
```

The relevant cost is not only GPU time.

A remote GPU that waits for dataset transfer may provide less wall-clock value than a slower local learner.

Dataset-transfer cost must therefore be included in:

```text
strength / wall-clock
```

when comparing practical training systems.

## 19.8 Checkpoint Compatibility

Every model checkpoint must contain compatibility metadata.

At minimum:

```text
checkpoint_format_version

environment_version

observation_schema_version
action_schema_version

model_architecture_id
algorithm_id

training_dataset_id where applicable
```

Additionally, every checkpoint must carry enough provenance to be placed in an experiment and on a sample-efficiency curve:

```text
seed
code_version / git_commit
training_step

forge_callbacks_consumed
atomic_decisions_consumed
```

Without `seed` and `code_version`, a checkpoint cannot be traced back to its bake-off cell.

Without the consumed-interaction counters, the metrics defined in Section 13.1:

```text
strength / Forge callback
strength / atomic agent decision
```

cannot be reconstructed from checkpoints alone.

Before inference or evaluation, the runtime must verify compatibility.

A checkpoint trained against:

```text
observation_schema_version = X
action_schema_version = Y
```

must not silently run against incompatible schema versions.

The default behavior on incompatibility is:

```text
FAIL LOUDLY
```

not implicit conversion.

Explicit migration adapters may be added later where justified.

## 19.9 Provider Capabilities Are Runtime Configuration

Provider properties such as:

```text
GPU model
GPU quota
TPU availability
maximum session duration
idle timeout
concurrent session limit
storage/output limit
network policy
```

are operational facts that may change over time.

They must not be encoded as permanent ForgeAI architectural assumptions.

Before a substantial remote training run:

```text
query / verify current provider capabilities
record them in experiment metadata
```

Experiment metadata should record where practical:

```text
provider
accelerator model
runtime image
framework version
CUDA/runtime version
allocated memory
training wall-clock
```

This makes provider-specific performance results reproducible and prevents temporary service limits from becoming permanent design assumptions.

### Inference determinism

Deterministic reproducible evaluation covers the environment; it does not automatically cover the network.

Identical weights do not produce bit-identical scores across:

```text
different GPU models
GPU vs CPU
different cuDNN/kernel algorithm selection
TF32 / mixed-precision settings
different reduction orders
```

Where an atomic decision is effectively an `argmax` over closely scored legal candidates, such differences can flip a single choice and therefore produce a different game.

Evaluation runs must therefore pin and record:

```text
inference device
numeric precision / TF32 setting
deterministic-kernel settings
framework and CUDA/runtime version
```

An evaluation result is only reproducible together with the inference configuration that produced it.

Where the environment seed is fixed but the inference configuration is not, the run must not be reported as deterministic.

## 19.10 Role of Kaggle in the Current Roadmap

Current intended role:

```text
FRL environment generation
        → primarily local Forge CPU workers

Algorithm bake-off
        → common compute basis

BC / supervised GPU workloads
        → Kaggle is a valid optional backend

Offline datasets
        → Kaggle is a valid optional backend

Online RL
        → use Kaggle only if the intended online
          actor/learner topology can actually be preserved

Final production algorithm
        → selected by empirical learning performance,
          not Kaggle compatibility
```

Kaggle is therefore:

> an opportunistic compute resource, not a dependency and not an architectural constraint.

---

# 20. Modified Upstream Forge Surface

The FRL work is not an isolated module. New decision code is contained in one package:

```text
forge-game/src/main/java/forge/game/decision/
```

but several **existing, upstream-maintained** Forge files have been modified. Known so far:

```text
forge-game/src/main/java/forge/game/phase/PhaseHandler.java     FRL-02H
forge-ai/src/main/java/forge/ai/PlayerControllerAi.java         FRL-02D
forge-game/.../CostAdjustment.java                              FRL-02D
forge-ai/pom.xml                                                FRL-02H
```

This list is incomplete and must be reconstructed and then maintained.

## 20.1 Why this is tracked

The fork has not yet been synchronised with `Card-Forge/forge`. Until it is, the merge cost of these modifications is invisible but accruing. `CostAdjustment` is the most exposed: cost-reduction logic is routinely touched upstream by new card implementations.

Absence of a git merge conflict to date is **not** evidence of low merge cost. The FRL-02D merge was conflict-free only because master had moved in documentation alone.

## 20.2 Requirement

Every FRL report must list the existing Forge files it modified, separately from files it added.

The cumulative list is the input to a later, deliberate decision about extracting a dedicated module. That decision is explicitly **not** made here, and must not be taken mid-milestone.

A useful trend indicator:

```text
git diff --name-only <merge-base with upstream>..HEAD -- "*.java"
```

## 20.3 Test-suite state

Green at the production checkpoint (PR #11, merge `c8835a22`):

```text
baseline diagnostics                 11 / 11
baseline decision regression        220 / 220

focused X / V2 / projection          32 / 32
Java executable resolver              3 / 3
focused mulligan lifecycle            6 / 6
outcome integrity                     3 / 3

collector OFF/OFF/ON/ON      1 test, 4 full games   PASS
worker isolation             1 test, 2 live JVMs    PASS

final expanded gate regression      286 / 286
failures 0, errors 0, skipped 0

package: BUILD SUCCESS
Checkstyle: 0 violations
git diff --check: clean
```

The earlier figure of 283 predates the three portability resolver tests and was corrected to 286 in the final report.

The previously known-red state — seven `PriorityActionDiagnostics` failures from a 55-versus-54 diagnostic column count — was repaired by tightening the stale assertions to the production contract rather than relaxing them.

That episode is worth recording rather than closing quietly. While the suite was red, it could not have distinguished a genuine regression from the known breakage. The determinism defect in Section 14.11 was live throughout that period and was found by a coincidence in reported scores, not by the test suite.

> A known-red suite is acceptable only while it is recorded, attributed, and short-lived. It removes the signal that would catch the next defect, including the one already present.

## 20.4 Upstream Contribution Candidates

Some defects found here are Forge defects, not ForgeAI needs. Contributing them upstream removes fork debt instead of adding it.

Current candidate:

```text
GameOutcome resolves a singular winner from a plural,
unordered, identity-keyed source.

The result is process-dependent even when the game state
is fully determined.
```

This is not the same claim as "more than one winner is invalid" — Forge supports multiplayer and team formats where several players legitimately win together. The defect is that the answer is unstable, not that it is plural.

Two separable changes follow, and the first carries no semantic risk:

```text
A  make the winner collection insertion-ordered
   -> the existing getter becomes deterministic
   -> no API change, no caller affected
   -> requires confirming that insertion order is itself deterministic

B  expose a 0 / 1 / n winner API
   -> callers decide explicitly
```

ForgeAI is already unblocked by its own simulator-side classification, so this is contribution work rather than remediation.

---

# Architectural Commitment

We commit now to:

1. player-perspective observations,
2. typed hierarchical legal decisions,
3. dynamic legal candidate representation,
4. explicit enumerate-vs-decompose classification per decision family,
5. synthetic sub-decisions where enumeration would be combinatorial, with `Forge callback count ≠ agent decision count` made explicit everywhere,
6. history-capable observations/trajectories,
7. algorithm-neutral worker and trajectory interfaces,
8. separation of normal results, engine failures and game-limit terminations,
9. deterministic reproducible evaluation, including a pinned and recorded inference configuration,
10. explicit robustness measurement,
11. a proactive-vs-reactive initial benchmark matchup,
12. explicit time budgets per engineering milestone,
13. compute-provider neutrality, with the algorithm chosen before the provider,
14. a single common compute basis for the primary algorithm bake-off,
15. sharded, versioned, compressed and resumable trajectory storage,
16. checkpoint compatibility metadata with fail-loud validation on schema mismatch,
17. labelling every coverage and frequency figure with the policy that drove the run, and never reading a Forge-AI-path count as an environment property,
18. a per-layer, per-DecisionType atomic decision cost model that separates candidate generation from candidate encoding,
19. RNG neutrality of neutral boundaries, in addition to state neutrality,
20. per-report listing of modified existing Forge files, separately from added files,
21. neutrality of boundaries against all global process state — game state, randomness, and global counters or identifier sequences,
22. classification of every game termination at whichever layer ends the game, including the match harness,
23. a launcher-owned worker output namespace, with no per-class process-ID logic in diagnostic sinks,
24. a mechanically enforced labeled-sample contract, with forced and unlabeled decisions excluded from policy training data.

We explicitly **do not commit yet** to:

* PPO,
* R2D2,
* IMPALA/V-trace,
* a specific recurrent architecture,
* reward shaping,
* the strategic treatment of a game-limit cap,
* the physical trajectory storage encoding (Parquet vs. NPZ vs. other),
* a specific external compute provider,
* global search,
* CFR,
* ReBeL,
* MuZero.

Those decisions remain empirical gates.
