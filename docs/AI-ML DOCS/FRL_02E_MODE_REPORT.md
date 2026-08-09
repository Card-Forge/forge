# FRL-02E MODE Decision Boundary Report

## Status

```text
FRL-02E architecture: APPROVED
Prerequisite blocker: NONE
Implementation: COMPLETE FOR CONTROLLED SLICE
```

Implementation branch: `frl/02e-mode-boundary`

Base: `c3d5eebae0765cbc4784febf9c4da425f50e63c2`

This milestone adds a neutral `DecisionType.MODE` boundary for ordinary, activating-player-selected,
single-mode Charm operations. It does not implement reinforcement learning or alter Forge's Human/AI choice.

## Authoritative callback boundary

`CharmEffect.makeChoices` remains the Forge state machine and legality authority. It clears the prior modal
sub-chain, calls `makePossibleOptions`, handles Entwine, Optional confirmation, Random, and Chooser, calculates
`min`, `num`, and repetition, then supplies the exact live callback state:

```text
PriorityActionDiagnostics.recordModeCallback(root, choices, min, num, canRepeat, chooser)
chooser.getController().chooseModeForAbility(root, choices, min, num, canRepeat)
CharmEffect.chainAbilities(root, chosen)
```

`ModeDecisionProvider` consumes that callback-supplied `choices` list. It never calls
`CharmEffect.makePossibleOptions` and never uses Forge AI as a legality oracle. Each supplied live `AbilitySub`
is mapped by membership in `root.getAdditionalAbilityList("Choices")` to its original zero-based ordinal.
Filtering therefore preserves semantic keys such as `MODE|0` and `MODE|2`; it never renumbers them.

The raw callback and neutral boundary are deliberately separate:

```text
MODE_CALLBACK  raw Forge callback; no subdecision index
MODE           supported neutral DecisionRequest; consumes one subdecision index
MODE_STATE     unsupported/not-applicable/invalid diagnostic; no subdecision index
```

The previous AI-only generic MODE recorder was removed from `PlayerControllerAi`. Existing AI and Human mode
selection behavior is otherwise unchanged.

## Decision contract

The supported request is:

```text
DecisionRequest(MODE)
  ModeDecisionContext
    choosing_player_id
    activating_player_id
    min = 1
    max = 1
    allow_repeat = false
    decision_sequence_id
    subdecision_index

  LegalCandidate
    semantic_key = MODE|<original ordinal>
    mode_ordinal
    public mode description
    targeting present anywhere in the mode branch
    private request-local live AbilitySub
```

One candidate is forced; two or more are strategic. A forced request remains a real atomic outcome but requires
no policy inference. No mutable Forge object is serialized.

## Completion safety

### Detached TARGET branch

`TargetDecisionProvider.assessBranchCompletion(candidateMode, rootActivatingPlayer)` starts at the supplied
detached mode and walks only that mode's sub-chain. It does not climb to `getRootAbility`, attach anything to the
root, mutate `TargetChoices`, generate a request, allocate an ID/index, or revisit costs.

For a branch without `TargetingPlayer`, it uses the root activating player, matching normal `setupTargets` default
semantics. An unresolved explicit `TargetingPlayer`, divided/random targeting, hidden targets, and coupled target
sets without a completion proof are unsupported. Mandatory target groups with no legal target are removed.

The MODE-specific enumeration uses the current callback-prepared Forge restrictions through `canTarget` and does
not call `TargetRestrictions.getAllCandidates`, avoiding a second `applyTargetTextChanges` mutation. The normal
TARGET request path retains its existing behavior.

The central regression has `root.subAbility == null` while a detached Izzet Charm damage mode has no mandatory
creature target. The mode is excluded and only `MODE|2` remains forced.

### X and PAYMENT

For MODE-before-X cards, MODE calls only the request-free
`XDecisionProvider.assessFutureXPaymentDomain(root, payer)`. It does not call `generateXRequest`, attach a mode,
run TARGET completion, change X, allocate a request, or allocate a continuation index. It proves only that the
shared root X/payment domain belongs to the accepted FRL-02D support slice.

For explicit X-before-MODE cards, the already announced root X remains authoritative. The Confront the Past
fixture sets X before MODE and verifies it is unchanged after generation.

Candidate-specific TARGET completion stays separate. Non-X roots use `PriorityCostFeasibility` only as
side-effect-free shared-cost/payability evidence; this is not claimed as proof of every FRL-02C implementation
detail and adds no PAYMENT capability. ModeCost, Spree, Tiered, Pawprint, and other per-mode cost semantics are
unsupported.

## Apply and Forge chaining

`ModeDecisionProvider.apply` validates request ownership, revalidates supported shape, chooser, original ordinal,
current branch completion, shared X/payment support, and semantic key. A stale selection is rejected; no substitute
or ordinal clamping occurs.

Apply returns the current live `AbilitySub` and does not attach it. The existing controller integration returns the
selected list to `CharmEffect`, and `CharmEffect.chainAbilities` remains the sole authority that sorts, copies,
assigns final `CharmOrder`, sets stack-description behavior, and appends the clone to the root.

## Ordering fixtures

```text
Invoke the Firemind
PRIORITY_ACTION -> optional/additional costs -> MODE -> X_VALUE -> optional TARGET -> PAYMENT

Confront the Past (explicit Announce$ X)
PRIORITY_ACTION -> optional/additional costs -> X_VALUE -> MODE -> TARGET -> PAYMENT
```

The implementation does not impose a global MODE/X/TARGET/PAYMENT order. MODE exists only at Forge's actual
`chooseModeForAbility` callback.

## Supported slice

```text
noncopied
nontrigger
nonoptional
ordinary Charm
actual chooser == activating player
literal/default CharmNum = 1
min = max = 1
CanRepeatModes = false
no ModeCost
no Spree/Tiered/Pawprint
no unresolved TargetingPlayer
provable target completion
shared supported root cost
```

Primary real-card fixtures are Invoke the Firemind, Izzet Charm, and Confront the Past for explicit X-before-MODE.

## Explicit capability boundary

| Operation | Classification |
|---|---|
| Ordinary single mode, supported shape | `PLAYER_MODE_DECISION` |
| One legal ordinary mode | `FORCED_MODE` |
| Entwine | `ENGINE_SELECTED_MODE_SET`; no MODE callback/request |
| Actual Random branch | `ENGINE_SELECTED_MODE`; no MODE callback/request |
| Copied modal spell | `NOT_APPLICABLE` |
| Optional before unresolved confirmation | `UNSUPPORTED` |
| External/opponent Chooser | `UNSUPPORTED` |
| Trigger modal choice | `UNSUPPORTED` |
| CharmNum greater than one or dynamic/X | `UNSUPPORTED` |
| MinCharmNum not one | `UNSUPPORTED` |
| CanRepeatModes | `UNSUPPORTED` |
| ModeCost, Spree, Tiered, Pawprint | `UNSUPPORTED` |
| Unresolved TargetingPlayer | `UNSUPPORTED_MODE_TARGET_COMPLETION` |
| Coupled/divided/random/hidden target completion without proof | `UNSUPPORTED_MODE_TARGET_COMPLETION` |
| Future X/payment domain outside FRL-02D | `MODE_X_PAYMENT_DOMAIN` |
| Shared non-X payment feasibility outside controlled support | `MODE_PAYMENT_SUPPORT` |

No subset/multiset enumeration, multi-step MODE solver, confirmation, card selection, target search, payment
extension, combat decomposition, or AI mode scoring was added.

## Verification

The focused decision suite passed:

```text
142 tests, 0 failures, 0 errors, 0 skipped
```

It includes the MODE fixtures plus the complete existing ActionContinuation, PRIORITY_ACTION, TARGET, X_VALUE,
PAYMENT, cost-feasibility, and diagnostics suites. The package build also passed:

```text
mvn -pl forge-gui-desktop -am -DskipTests package
BUILD SUCCESS
```

MODE coverage includes original ordinals after filtering, deterministic keys, forced/strategic requests, detached
mandatory-target failure with a null root sub-chain, Invoke MODE-before-X, Confront X-before-MODE, continuation
identity, public/private candidate fields, Forge-owned chaining, stale ordinal and stale target rejection,
hidden-hand differential, purity, chooser/optional/random/Entwine/copy boundaries, multi/repeat rejection, and
Spree/Pawprint/ModeCost rejection.

Source inspection confirms the MODE provider contains no `SpellApiToAi`, `ComputerUtil`, `AILogic`,
`makePossibleOptions`, `generateTargetRequest`, or `generateXRequest` call.

## Controlled benchmarks

Both requested packaged-artifact runs completed:

```text
Dead and Alive vs Air Forces: 10 games, seed 20260809, result 7-3
Izzet Guild Kit vs Dimir Guild Kit: 10 games, seed 20260810, result 3-7
```

| Matchup | Raw MODE callbacks | Neutral MODE requests | Forced | Strategic | Unsupported MODE states |
|---|---:|---:|---:|---:|---:|
| Dead and Alive vs Air Forces | 0 | 0 | 0 | 0 | 0 |
| Izzet Guild Kit vs Dimir Guild Kit | 2 | 1 | 1 | 0 | 1 |

The Guild run naturally observed Izzet Charm: one callback-supplied mode remained, producing forced `MODE|2`.
It also observed Invoke the Firemind with two raw modes; its live shared future X/payment domain was outside the
accepted slice and produced `MODE_STATE / MODE_X_PAYMENT_DOMAIN`, not an unsafe request. AI was not modified to
manufacture callbacks.

The CSV continuation evidence is:

```text
Izzet Charm: PRIORITY_ACTION 0 -> MODE_CALLBACK no index -> MODE 1 -> PAYMENT 2
Invoke:      PRIORITY_ACTION 0 -> MODE_CALLBACK no index -> MODE_STATE no index -> PAYMENT 1
```

Raw CSVs are outside the repository:

```text
C:\Users\chris\AppData\Local\Temp\frl02e-dead-air-20260809.csv
C:\Users\chris\AppData\Local\Temp\frl02e-izzet-dimir-20260810.csv
```

## MODE generation performance

The focused test covers forced one-candidate, strategic two-candidate, and strategic three-candidate requests.
After 20 warmups, 180 generations of the three-candidate fixture measured:

| Metric | Value |
|---|---:|
| generation p50 | 0.683 ms |
| generation p95 | 1.111 ms |
| generation p99 | 1.351 ms |
| candidate mean | 2.0 |
| candidate p50 | 2 |
| candidate p95 | 3 |
| candidate max | 3 |
| rule/legality probes per request | 2, 3, 4 |
| downstream completion probes per request | 2, 3, 4 |

The benchmark's sole supported request had one candidate, two rule probes, two downstream probes, and 2.128 ms
generation time. The unsupported Invoke state had three rule probes, three downstream probes, and 5.850 ms
generation time. These are MODE candidate-generation measurements, not Forge callback latency, candidate encoding
latency, policy inference latency, or ForgeRL throughput.

## Files changed

```text
forge-ai/src/main/java/forge/ai/PlayerControllerAi.java
forge-game/src/main/java/forge/game/ability/effects/CharmEffect.java
forge-game/src/main/java/forge/game/decision/DecisionRequest.java
forge-game/src/main/java/forge/game/decision/DecisionType.java
forge-game/src/main/java/forge/game/decision/LegalCandidate.java
forge-game/src/main/java/forge/game/decision/ModeDecisionContext.java
forge-game/src/main/java/forge/game/decision/ModeDecisionProvider.java
forge-game/src/main/java/forge/game/decision/PriorityActionDiagnostics.java
forge-game/src/main/java/forge/game/decision/TargetDecisionProvider.java
forge-game/src/main/java/forge/game/decision/XDecisionProvider.java
forge-gui-desktop/src/test/java/forge/game/decision/ModeDecisionProviderTest.java
forge-gui-desktop/src/test/java/forge/game/decision/PriorityActionDiagnosticsTest.java
docs/AI-ML DOCS/FRL_02E_MODE_REPORT.md
docs/superpowers/plans/2026-08-09-frl-02e-mode-boundary.md
docs/superpowers/specs/2026-08-09-frl-02e-mode-boundary-design.md
```

`ActionContinuation.java`, `TargetRestrictions.java`, `ML_STRATEGY.md`, and all forbidden capability areas are
unchanged.
