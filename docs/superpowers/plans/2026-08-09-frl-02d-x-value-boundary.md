# FRL-02D X Value Boundary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a completion-safe neutral player-selected X boundary with a proven finite domain and no AI heuristics or arbitrary cap.

**Architecture:** Introduce an `XDecisionProvider` that starts from Forge announcement bounds, obtains a complete cost-independent fixed mana capacity and a domain-wide reduction allowance, assesses every candidate with a pure specific-X feasibility query, and preflights only chooser-deterministic X-independent targets. Requests reuse the selected priority action continuation and apply only after full revalidation.

**Tech Stack:** Java 17, Forge game engine, TestNG, Maven multi-module reactor.

---

### Task 1: Specific-X feasibility and complete capacity

**Files:**
- Modify: `forge-game/src/main/java/forge/game/decision/PriorityCostFeasibility.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/PriorityCostFeasibilityTest.java`

- [ ] Add failing tests for `assessPaymentAtX`, off-color capacity, fixed multi-output capacity, unsupported incomplete inventory, root-over-sub cost authority, and no mutation of root/mana abilities/pool.
- [ ] Run the focused class and verify failures are caused by missing specific-X/capacity APIs.
- [ ] Add an immutable capacity assessment and `assessPaymentAtX(Player, SpellAbility, int)` using the existing shadow payment search.
- [ ] Remove probe fallback mutation of root and live mana abilities; require prepared root state and use pure/copy-local mana ability inspection.
- [ ] Re-run the focused class and retain green output.

### Task 2: Domain-wide cost-reduction allowance

**Files:**
- Modify: `forge-game/src/main/java/forge/game/cost/CostAdjustment.java`
- Modify: `forge-game/src/main/java/forge/game/cost/CostAdjustmentPreview.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/cost/CostAdjustmentPreviewTest.java`

- [ ] Add failing tests proving one Electromancer reports allowance one independent of probed X, fixed increases add no allowance, and multiple/variable reductions remain choice-required or unsupported.
- [ ] Run the focused class and verify the allowance API is absent.
- [ ] Extend preview results with a conservative `maximumGenericReductionAllowance` computed by CostAdjustment authority from the full fixed reduction amount, not the candidate-applied reduction.
- [ ] Re-run the focused class.

### Task 3: Pure target completion preflight

**Files:**
- Modify: `forge-game/src/main/java/forge/game/decision/TargetDecisionProvider.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/TargetDecisionProviderTest.java`

- [ ] Add failing tests for a normal deterministic activating-player target, impossible mandatory target, and unresolved `TargetingPlayer` chooser.
- [ ] Assert the preflight leaves targets, request IDs, continuation indices, and cost state untouched.
- [ ] Implement a pure tri-state completion assessment using existing target legality helpers without calling `generateTargetRequest` or cost reassessment.
- [ ] Re-run the focused class.

### Task 4: X request model and provider

**Files:**
- Modify: `forge-game/src/main/java/forge/game/decision/DecisionType.java`
- Modify: `forge-game/src/main/java/forge/game/decision/DecisionRequest.java`
- Modify: `forge-game/src/main/java/forge/game/decision/LegalCandidate.java`
- Create: `forge-game/src/main/java/forge/game/decision/XDecisionContext.java`
- Create: `forge-game/src/main/java/forge/game/decision/XDecisionProvider.java`
- Create: `forge-gui-desktop/src/test/java/forge/game/decision/XDecisionProviderTest.java`

- [ ] Add failing tests for ordinary X, Invoke the Firemind ordering/state, X=0, multiple/forced values, XMin/XMax/AnnounceMax, off-color generic-X capacity, fixed reduction/increase, retention in ManaCostBeingPaid, stale apply, semantic ordering, continuation, hidden-info differential, non-mana X, unsafe domain, adjustment choice, target/mode dependencies, and absence of AI helpers.
- [ ] Run the focused class and verify missing X model/provider failures.
- [ ] Implement `X_VALUE`, request-local `X|N` candidates, immutable context, structured generation statuses/reasons, complete domain generation, and apply-time recomputation.
- [ ] Re-run the focused class until green without weakening assertions.

### Task 5: Raw callback and neutral diagnostics

**Files:**
- Modify: `forge-game/src/main/java/forge/game/player/PlaySpellAbility.java`
- Modify: `forge-game/src/main/java/forge/game/decision/PriorityActionDiagnostics.java`
- Modify: `forge-ai/src/main/java/forge/ai/PlayerControllerAi.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/PriorityActionDiagnosticsTest.java`

- [ ] Add failing tests proving raw callbacks do not consume subdecision indices and only supported generated requests do.
- [ ] Add separate raw-X and neutral-X diagnostic events with compact fields.
- [ ] Hook diagnostics immediately before Forge's existing X controller callback without changing the returned Human/AI value.
- [ ] Remove the old AI raw-X-as-downstream-subdecision accounting.
- [ ] Re-run diagnostics and existing controller-focused tests.

### Task 6: Focused verification and controlled benchmarks

**Files:**
- Create: `docs/AI-ML DOCS/FRL_02D_X_VALUE_REPORT.md`

- [ ] Run all focused X, feasibility, adjustment, target, payment, continuation, and diagnostics tests.
- [ ] Run the `forge-gui-desktop` package build with dependencies.
- [ ] Run 10 seeded Dead and Alive vs Air Forces games and 10 seeded Izzet Guild Kit vs Dimir Guild Kit games using the accepted benchmark path.
- [ ] Record raw requests, neutral/forced/strategic/unsupported counts, candidate distributions, generation latency, and honest zeroes where AI preselection avoids neutral requests.
- [ ] Document exact commands, results, limitations, and the retained FRL-01A adjustment-choice dependency.

### Task 7: Review and publish

**Files:**
- Review every file changed by Tasks 1-6.

- [ ] Run `git diff --check`, inspect `git diff --stat` and the full diff, and verify no unrelated changes.
- [ ] Run the complete focused verification again from the final tree.
- [ ] Stage only FRL-02D files and commit with a scoped message.
- [ ] Push `frl/02d-x-value-boundary` to origin.
- [ ] Open a Draft PR targeting `master` with the implementation summary, test evidence, benchmark results, and unsupported boundaries.
