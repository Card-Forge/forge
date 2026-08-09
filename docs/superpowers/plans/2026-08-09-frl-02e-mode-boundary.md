# FRL-02E MODE Boundary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a callback-local, completion-safe neutral MODE boundary for Forge's ordinary single-mode Charm slice.

**Architecture:** The provider consumes Forge's already-filtered callback `possible` list, maps live modes to stable original ordinals, and performs detached-branch TARGET plus shared request-free X/PAYMENT checks. Applying a candidate returns a revalidated live mode to the existing callback; `CharmEffect.chainAbilities` remains the only attachment authority.

**Tech Stack:** Java 17, Forge game engine, TestNG, Maven, GitHub Actions-compatible diagnostics.

---

### Task 1: Define MODE request data

**Files:**
- Modify: `forge-game/src/main/java/forge/game/decision/DecisionType.java`
- Modify: `forge-game/src/main/java/forge/game/decision/DecisionRequest.java`
- Modify: `forge-game/src/main/java/forge/game/decision/LegalCandidate.java`
- Create: `forge-game/src/main/java/forge/game/decision/ModeDecisionContext.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/ModeDecisionProviderTest.java`

- [ ] Write a failing test that constructs an ordinary Izzet Charm callback request and expects `DecisionType.MODE`, stable `MODE|0..2` keys, original ordinals, choosing-player metadata, and private live mode application identity.
- [ ] Run `mvn -pl forge-gui-desktop -am -Dtest=ModeDecisionProviderTest -Dsurefire.failIfNoSpecifiedTests=false test` and verify compilation/test failure because MODE types do not exist.
- [ ] Add `MODE`, the MODE-specific immutable context, request invariants/getter, and `LegalCandidate.mode(...)` public fields plus package-private live `AbilitySub` access.
- [ ] Rerun the focused test and verify the data-model assertions pass.

### Task 2: Implement callback-local generation and shape classification

**Files:**
- Create: `forge-game/src/main/java/forge/game/decision/ModeDecisionProvider.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/ModeDecisionProviderTest.java`

- [ ] Add failing tests proving generation uses only the supplied callback list, retains original ordinals after filtering, reports one candidate as forced, rejects stale/unmapped callback objects, and rejects copied, trigger, optional, Chooser, dynamic/multi/repeat, ModeCost, Spree, Tiered, and Pawprint states.
- [ ] Run the focused test and verify failures identify missing generation/status behavior.
- [ ] Implement `generateModeRequest(root, possible, min, num, allowRepeat, choosingPlayer, continuation)` with explicit `DECISION`, `INVALID_MODE`, `NOT_APPLICABLE`, and `UNSUPPORTED` results. Allocate request id and continuation index only after the complete supported candidate set is known.
- [ ] Rerun the focused test and verify shape, identity, forced, and unsupported tests pass.

### Task 3: Add detached branch-local TARGET completion

**Files:**
- Modify: `forge-game/src/main/java/forge/game/decision/TargetDecisionProvider.java`
- Modify only if required by a failing purity test: `forge-game/src/main/java/forge/game/spellability/TargetRestrictions.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/ModeDecisionProviderTest.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/TargetDecisionProviderTest.java`

- [ ] Add the mandatory failing regression: root sub-ability is null, detached candidate requires a mandatory target, no legal target exists, and generation must exclude that mode.
- [ ] Add failing tests for a detached legal target branch, nested candidate sub-chain, unresolved `TargetingPlayer`, coupled/divided/random targeting, hidden candidate safety, and unchanged root sub-ability/TargetChoices/request counters.
- [ ] Run the two focused test classes and verify the detached illegal-target regression fails because current assessment starts at the root.
- [ ] Implement `assessBranchCompletion(candidateMode, rootActivatingPlayer)` starting at the supplied mode and traversing only its sub-chain, reusing existing target prototype and completion safety logic without cost or request generation.
- [ ] If the tests demonstrate target-restriction mutation, add the smallest no-write current-state enumeration helper in `TargetRestrictions`; otherwise leave that file unchanged.
- [ ] Rerun both focused test classes and verify all branch-local and existing TARGET tests pass.

### Task 4: Add request-free shared X/PAYMENT support checks

**Files:**
- Modify: `forge-game/src/main/java/forge/game/decision/XDecisionProvider.java`
- Modify: `forge-game/src/main/java/forge/game/decision/ModeDecisionProvider.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/ModeDecisionProviderTest.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/XDecisionProviderTest.java`

- [ ] Add failing tests for Invoke's MODE-before-X flow, Confront the Past's existing-X flow, unsupported shared future-X domain, unchanged X, unchanged request/continuation identity, and separation of candidate TARGET from shared X payment support.
- [ ] Run the focused MODE/X tests and verify failure because the only current X API creates a request and rejects unresolved modes.
- [ ] Extract `assessFutureXPaymentDomain(root, payer)` from the accepted X finite-domain/payment logic. It must skip unresolved-mode and TARGET checks and return support status/candidate-domain evidence without request allocation or X mutation.
- [ ] Reuse current payment feasibility for nonfuture-X roots; reject unsupported rather than copying PAYMENT rules or adding capability.
- [ ] Rerun MODE/X tests and verify ordering, purity, and shared-root checks pass.

### Task 5: Revalidate and return the live mode

**Files:**
- Modify: `forge-game/src/main/java/forge/game/decision/ModeDecisionProvider.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/ModeDecisionProviderTest.java`

- [ ] Add failing tests for stale callback membership, chooser changes, target completion changes, unsupported downstream changes, no ordinal clamping/substitution, and selected Izzet/Invoke mode producing the exact normal `CharmEffect.chainAbilities` clone after the provider returns it.
- [ ] Run the focused test and verify stale/application assertions fail.
- [ ] Implement `apply(request, candidate)` to recompute callback-local support from the context's current possible list, recheck the chosen original ordinal and branch/shared completion, and return the current live `AbilitySub` without attaching it.
- [ ] Rerun the focused test and verify application and stale rejection pass.

### Task 6: Replace MODE diagnostic accounting and add metrics

**Files:**
- Modify: `forge-game/src/main/java/forge/game/ability/effects/CharmEffect.java`
- Modify: `forge-game/src/main/java/forge/game/decision/PriorityActionDiagnostics.java`
- Modify: `forge-ai/src/main/java/forge/ai/PlayerControllerAi.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/PriorityActionDiagnosticsTest.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/ModeDecisionProviderTest.java`

- [ ] Add failing diagnostics tests for `MODE_CALLBACK` without subdecision index, supported `MODE` with exactly one index, `MODE_STATE` without an index, raw/neutral count separation, generation time, candidate count, rule probes, and downstream probes.
- [ ] Add failing controller-path tests proving Entwine and actual Random do not invoke `chooseModeForAbility`, while ordinary modes do once.
- [ ] Run focused diagnostics/MODE tests and verify missing MODE diagnostic behavior.
- [ ] Record at the authoritative `CharmEffect` callback immediately before `chooseModeForAbility`, remove the legacy AI `recordDownstreamCallback(MODE, ...)`, and extend CSV formatting without altering mode selection.
- [ ] Rerun focused tests and verify diagnostics and controller behavior pass.

### Task 7: Complete fixture, purity, hidden-information, and performance coverage

**Files:**
- Modify: `forge-gui-desktop/src/test/java/forge/game/decision/ModeDecisionProviderTest.java`

- [ ] Add real-card tests for Invoke, Izzet Charm, deterministic ordering, semantic keys, forced/strategic states, MODE-to-X and X-to-MODE ordering evidence, continuation identity, hidden-hand differential, no Forge AI imports, and generation purity.
- [ ] Add a warmup and measured generation fixture that reports p50/p95/p99, candidate mean/p50/p95/max, and rule/downstream probe distributions.
- [ ] Run the focused MODE test and verify all functional and measurement assertions pass.

### Task 8: Verify, benchmark, document, and publish

**Files:**
- Create: `docs/AI-ML DOCS/FRL_02E_MODE_REPORT.md`

- [ ] Run the focused FRL decision suite covering priority, TARGET, PAYMENT, X_VALUE, MODE, continuation, cost preview, and diagnostics; record exact pass/failure totals.
- [ ] Run `mvn -pl forge-gui-desktop -am -DskipTests package` and record the exit result.
- [ ] Run ten seeded `Dead and Alive vs. Air Forces` games and ten seeded `Izzet Guild Kit vs. Dimir Guild Kit` games with MODE diagnostics enabled; record raw callbacks, neutral requests, forced, strategic, and unsupported states.
- [ ] Write `FRL_02E_MODE_REPORT.md` with architecture, support boundary, tests, package evidence, benchmark counts, generation metrics, and honest zero-observation interpretation.
- [ ] Run `git diff --check`, inspect `git status` and the complete diff, then rerun the focused suite after documentation changes.
- [ ] Stage only FRL-02E files, commit with a scoped message, push `frl/02e-mode-boundary`, and open a Draft PR against `master` without merging or marking ready.
