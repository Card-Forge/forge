# FRL-02C Payment Boundary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and measure a sequential, neutral PAYMENT boundary for ordinary Forge mana payment without using Forge AI as the legality oracle.

**Architecture:** `PaymentDecisionProvider` consumes the real `ManaCostBeingPaid` received at `applyManaToCost`, emits deterministic resource-level requests, and applies chosen resources through `ManaPool` or Forge's normal mana-ability execution. Fixed-output, tap-only sources and semantically distinct floating mana are supported; real variable-output callbacks and complex mechanics fail explicitly when they cannot be intercepted without redesign.

**Tech Stack:** Java 17, Forge game engine, TestNG, Maven, existing `AITest` live-game fixtures, optional CSV diagnostics, `SimulateMatch` controlled benchmarks.

---

## File Map

- Create `forge-game/src/main/java/forge/game/decision/PaymentCandidateKind.java`: public atomic PAYMENT operation vocabulary.
- Create `forge-game/src/main/java/forge/game/decision/PaymentStage.java`: source-stage metadata without adding another DecisionType.
- Create `forge-game/src/main/java/forge/game/decision/PaymentDecisionContext.java`: immutable public context plus package-private live references.
- Create `forge-game/src/main/java/forge/game/decision/PaymentDecisionProvider.java`: neutral generation, validation, and Forge-owned application.
- Create `forge-game/src/main/java/forge/game/decision/UnsupportedPaymentDecisionException.java`: structured loud failure.
- Modify `DecisionType.java`, `DecisionRequest.java`, and `LegalCandidate.java`: add PAYMENT while preserving PRIORITY_ACTION and TARGET contracts.
- Modify `ManaPool.java`: add exact-instance membership and payment operations so same-color provenance is preserved despite `Mana.equals`.
- Modify `PriorityActionDiagnostics.java`, `ManaPool.java`, and `ComputerUtilMana.java` only as needed for diagnostic observation of real payment operations.
- Create `PaymentDecisionProviderTest.java` and extend `PriorityActionDiagnosticsTest.java`: focused real-state tests.
- Create `docs/AI-ML DOCS/FRL-02C_PAYMENT_BOUNDARY.md`: final architecture, classification, metrics, limitations, and benchmark report.

### Task 1: PAYMENT Request Contract

**Files:**
- Modify: `forge-game/src/main/java/forge/game/decision/DecisionType.java`
- Modify: `forge-game/src/main/java/forge/game/decision/DecisionRequest.java`
- Modify: `forge-game/src/main/java/forge/game/decision/LegalCandidate.java`
- Create: `forge-game/src/main/java/forge/game/decision/PaymentCandidateKind.java`
- Create: `forge-game/src/main/java/forge/game/decision/PaymentStage.java`
- Create: `forge-game/src/main/java/forge/game/decision/PaymentDecisionContext.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/PaymentDecisionProviderTest.java`

- [ ] **Step 1: Write a failing contract test**

Create a TestNG test that constructs a live Lightning Bolt payment context and asserts:

```java
assertEquals(request.getDecisionType(), DecisionType.PAYMENT);
assertEquals(request.getPaymentContext().getPaymentStage(), PaymentStage.SOURCE);
assertEquals(request.getPaymentContext().getPayerId(), payer.getId());
assertEquals(request.getPaymentContext().getRemainingCostSummary(), "{R}");
assertEquals(request.getCandidates().get(0).getPaymentKind(), PaymentCandidateKind.ACTIVATE_MANA_SOURCE);
```

- [ ] **Step 2: Verify RED**

Run:

```text
mvn -pl forge-gui-desktop -Dtest=forge.game.decision.PaymentDecisionProviderTest test
```

Expected: compilation fails because PAYMENT contract types do not exist.

- [ ] **Step 3: Add the minimal immutable contract**

Add `PAYMENT` to `DecisionType`; add a PAYMENT-only context slot and validation to `DecisionRequest`; add PAYMENT candidate fields and package-private live references to `LegalCandidate`. Define:

```java
public enum PaymentCandidateKind {
    USE_FLOATING_MANA,
    ACTIVATE_MANA_SOURCE
}

public enum PaymentStage {
    SOURCE
}
```

`PaymentDecisionContext` exports payer id, remaining-cost summary, stage, nullable sequence id, nullable subdecision index, and forced-safe summary fields. It privately retains payer, root ability, live remaining cost, conversion matrix, and continuation for application.

- [ ] **Step 4: Verify GREEN and regression compatibility**

Run the focused test and `DecisionRequest`/TARGET tests. Expected: all selected tests pass and existing request validation remains unchanged for PRIORITY_ACTION and TARGET.

### Task 2: Exact Floating-Mana Resource Operations

**Files:**
- Modify: `forge-game/src/main/java/forge/game/mana/ManaPool.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/PaymentDecisionProviderTest.java`

- [ ] **Step 1: Write failing exact-identity tests**

Create two blue `Mana` objects whose `Mana.equals` can compare equal but whose source cards differ. Assert that exact membership finds each object independently and that paying the second removes the second source's mana rather than the first.

```java
assertTrue(pool.containsManaInstance(first));
assertTrue(pool.containsManaInstance(second));
assertTrue(pool.tryPayCostWithManaInstance(spell, remaining, second, spell.getPayingMana()));
assertTrue(pool.containsManaInstance(first));
assertFalse(pool.containsManaInstance(second));
```

- [ ] **Step 2: Verify RED**

Run the focused test. Expected: compilation fails because exact-instance operations are absent.

- [ ] **Step 3: Implement exact-instance removal inside `ManaPool`**

Scan only the mana object's color lane, compare with `==`, remove through that lane's iterator, call the existing `ManaCostBeingPaid.payMana`, update the payer's recorded mana list, view, and mana-pool event consistently with existing removal. Return false without mutation if the instance is absent or no longer needed.

- [ ] **Step 4: Verify GREEN**

Run the focused test and `ManaCostBeingPaidTest`/`ManaRefundServiceTest`. Expected: exact source provenance is preserved and existing refund behavior passes.

### Task 3: Neutral Candidate Generation

**Files:**
- Create: `forge-game/src/main/java/forge/game/decision/PaymentDecisionProvider.java`
- Create: `forge-game/src/main/java/forge/game/decision/UnsupportedPaymentDecisionException.java`
- Modify: `forge-game/src/main/java/forge/game/decision/LegalCandidate.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/PaymentDecisionProviderTest.java`

- [ ] **Step 1: Write failing tests for fixed source counts**

Use real Mountain/Island/Swamp and spell fixtures to assert one legal source is forced, two distinct sources are strategic, tapped sources disappear, and deterministic regeneration produces identical semantic key order.

- [ ] **Step 2: Verify RED**

Run the provider test. Expected: failure because generation is absent.

- [ ] **Step 3: Implement fixed-output source discovery**

For each payer-controlled battlefield card, inspect its real mana abilities in deterministic card-id/timestamp order. Require a single `CostTap`, one mana part, playable live ability, and fixed production tokens. Reject relevant `Any`, `Combo`, `Chosen`, `Special`, reflected, replacement-dependent, multiple-part, non-tap, or reusable complex production with `UnsupportedPaymentDecisionException` rather than omitting a legal alternative.

Use `ManaCostBeingPaid.getPaymentVariants`, mana restrictions, and `SpellAbility.allowsPayingWithShard` to prove that at least one produced mana unit can advance the live cost. Do not call any class in `forge.ai`.

- [ ] **Step 4: Write failing floating-mana tests**

Assert that one compatible resource is forced; two same-color resources with different source provenance remain two candidates; incompatible restricted mana is excluded; and exact equivalent objects from the same source are collapsed only when their complete Forge-visible semantics match.

- [ ] **Step 5: Verify RED**

Run the focused test. Expected: floating candidate assertions fail.

- [ ] **Step 6: Implement floating candidates per concrete resource**

Filter each live mana by spell-level restrictions, shard-level variants, source/color allowance, snow, and live pool membership. Build semantic keys from operation kind, color, source card id/timestamp, restriction strings, snow, persistent/combat state, spending effects, and deterministic equivalent occurrence semantics. Keep the exact representative private.

- [ ] **Step 7: Add continuation, payer, hidden-state, and unsupported tests**

Assert actual payer can differ from ability activator, continuation ids are reused, subdecision increments once per generated request, uncorrelated sessions remain null, opponent hand changes do not alter requests, Birds of Paradise is explicitly unsupported, Phyrexian mana is unsupported, and no production source imports or calls AI helpers.

- [ ] **Step 8: Verify GREEN**

Run focused tests. Expected: supported candidates are stable and unsupported paths fail with structured reasons.

### Task 4: Forge-Owned Candidate Application

**Files:**
- Modify: `forge-game/src/main/java/forge/game/decision/PaymentDecisionProvider.java`
- Modify: `forge-game/src/main/java/forge/game/decision/PaymentDecisionContext.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/PaymentDecisionProviderTest.java`

- [ ] **Step 1: Write failing floating application tests**

Apply a floating candidate and assert exact pool removal, live remaining-cost mutation, paying-mana tracking, next request generation, and COMPLETE when the last shard is paid.

- [ ] **Step 2: Verify RED**

Run the focused test. Expected: apply operation is absent.

- [ ] **Step 3: Implement floating application through `ManaPool`**

Validate request membership and regenerate current candidate semantics. Call only the exact-instance `ManaPool` operation; never directly remove mana or decrement a cost from the provider. Generate the next request from the mutated live state.

- [ ] **Step 4: Write failing real-source application tests**

Apply Mountain to Lightning Bolt and Dimir Aqueduct to a `{U}{B}` payment. Assert Forge taps the source, resolves its real mana ability, records the paying ability, consumes the fixed bundle as one activation, and does not re-offer the used source.

- [ ] **Step 5: Verify RED**

Run the focused test. Expected: source application is absent.

- [ ] **Step 6: Implement source application through Forge**

Revalidate payer, card id/timestamp, controller, tap state, ability position, costs, fixed output, restrictions, and playability. Invoke `PlaySpellAbility.playSpellAbility` with the payer's actual controller. After successful resolution, call `ManaPool.payManaFromAbility` exactly as `InputPayMana` does, then regenerate from the same live `ManaCostBeingPaid`.

- [ ] **Step 7: Write stale, partial, invalid, and root-cost tests**

Assert a tapped or removed source candidate is rejected before mutation; a used source is absent; multi-step `{2}{U}{B}` payment regenerates; impossible supported payment returns INVALID; `AbilitySub Cost.Zero` does not replace root state; and a post-target live remaining cost is preserved.

- [ ] **Step 8: Verify GREEN and differential behavior**

Run focused tests. Expected: every supported neutral candidate succeeds through Forge and all stale applications fail without state corruption.

### Task 5: Diagnostic Classification and Metrics

**Files:**
- Modify: `forge-game/src/main/java/forge/game/decision/PriorityActionDiagnostics.java`
- Modify: `forge-game/src/main/java/forge/game/mana/ManaPool.java`
- Modify: `forge-ai/src/main/java/forge/ai/ComputerUtilMana.java`
- Modify: `forge-gui-desktop/src/test/java/forge/game/decision/PriorityActionDiagnosticsTest.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/PaymentDecisionProviderTest.java`

- [ ] **Step 1: Write failing formatting and counter tests**

Assert PAYMENT records contain sequence, subdecision, payer, stage, candidate count, forced status, remaining summary, status, unsupported reason, and generation nanoseconds without card-object dumps.

- [ ] **Step 2: Verify RED**

Run diagnostics tests. Expected: PAYMENT formatting methods are absent.

- [ ] **Step 3: Add optional diagnostic observation**

When diagnostics are enabled and payment is real rather than a test probe, observe each initial floating-resource consumption before Forge chooses it and each source-selection iteration before AI heuristics choose a source. Generate the neutral request independently; then record whether an observable AI-selected source belongs to it. Unsupported generation is recorded and never changes AI behavior.

- [ ] **Step 4: Verify no AI legality dependency**

Add a static dependency test that reads `PaymentDecisionProvider.java` and fails if it references `ComputerUtilMana`, `AiCostDecision`, or `ComputerUtil`. Run focused tests and confirm diagnostics-disabled behavior remains unchanged.

### Task 6: Controlled Benchmarks and Architecture Report

**Files:**
- Create: `docs/AI-ML DOCS/FRL-02C_PAYMENT_BOUNDARY.md`
- Modify only if required: `forge-game/src/main/java/forge/game/decision/PriorityActionDiagnostics.java`

- [ ] **Step 1: Build the runnable desktop artifact**

Run:

```text
mvn -pl forge-gui-desktop -am -DskipTests package
```

Expected: exit code 0 and the desktop runnable artifact is produced.

- [ ] **Step 2: Run both accepted matchups**

Run ten games each, with optional PAYMENT diagnostics enabled, for:

```text
Dead and Alive vs Air Forces
Izzet Guild Kit vs Dimir Guild Kit
```

Use the exact `SimulateMatch` syntax printed by the built artifact. Retain raw diagnostic CSVs outside tracked source files.

- [ ] **Step 3: Calculate required metrics**

From diagnostic rows calculate raw callbacks, atomic requests, forced/strategic requests, requests per paid action, candidate mean/p50/p95/max, generation p50/p95/p99, operation-class percentages, and resource-choice diversity buckets. Keep unsupported and unmapped operations explicit.

- [ ] **Step 4: Write the final milestone report**

Document repository state, exact Forge path, callback classification table, atomic model, real sequences, payer semantics, mutation path, metrics, compression, cost correctness, information safety, cancellation, unsupported mechanics, exact test commands/counts, both benchmark results, performance, continuation invariants, controlled-slice ML implication, and exactly one next DecisionRequest recommendation.

### Task 7: Full Verification, Review, and Publication

**Files:** all FRL-02C changes.

- [ ] **Step 1: Run focused and existing ForgeRL tests**

Run focused PAYMENT tests, decision package tests, cost adjustment, feasibility, target, continuation, and mana refund tests. Expected: zero failures.

- [ ] **Step 2: Run the broader module suite and checks**

Run the established `forge-gui-desktop` test suite, package build, `git diff --check`, and inspect `git diff`/`git status`. Split suites by package only if the full command exceeds the execution limit, and report every split result.

- [ ] **Step 3: Review requirements line by line**

Compare the implementation and report against every Definition of Done item. If variable output is unsupported, mark FRL-02C PARTIAL only when an accepted controlled-slice mechanism requires it; otherwise report its explicit v0 limitation without overstating support.

- [ ] **Step 4: Request code review and address findings**

Review the diff against the design and milestone contract. Correct every critical or important finding with a failing regression test before changing production code.

- [ ] **Step 5: Commit, push, and open Draft PR**

Create one implementation/report commit or a small justified series after the design commit, push `frl/02c-payment-boundary`, and open a Draft PR against `master`. Do not merge and do not mark ready.
