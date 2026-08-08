# ForgeAI ML Strategy

**Status:** Provisional / Accepted for implementation planning
**Date:** 2026-08-08
**Revision:** 3 — decision decomposition, termination semantics, FRL-00.5 measurement contract, benchmark matchup policy, engineering timebox, compute-provider portability
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
| `MULLIGAN`                           | Enumerate                                                     |
| `CONFIRMATION`                       | Enumerate                                                     |
| `CARD_SELECTION` — small bounded set | Enumerate or sequential selection                             |
| `ATTACK`                             | Decompose into sequential set construction                    |
| `BLOCK`                              | Decompose into blocker/attacker assignments                   |
| `ORDER`                              | Decompose into sequential permutation construction            |
| `PAYMENT`                            | Decompose where complete payment enumeration is combinatorial |
| multi-target choices                 | Autoregressive/sequential selection                           |
| large subset choices                 | Sequential set construction                                   |

This table is an initial architectural policy, not a complete inventory of Forge callbacks.

Every actual `PlayerController` decision type must eventually be classified explicitly.

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

---

# 15. Decision After FRL-00.5

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
Throughput + decision-surface benchmark

        ↓

FRL-01+
Algorithm-neutral ForgeRL environment

        ↓

RandomLegalPolicy completes full games

        ↓

Forge heuristic trajectory collection

        ↓

Behavior Cloning

        ↓

Controlled online-RL bake-off

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
16. checkpoint compatibility metadata with fail-loud validation on schema mismatch.

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
