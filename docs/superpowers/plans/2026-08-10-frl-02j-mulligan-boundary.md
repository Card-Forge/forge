# FRL-02J Mulligan Boundary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose Forge's ordinary 1v1 Constructed London KEEP/REDRAW callback and ordered bottom-card callback through neutral, stale-safe decision contracts without taking ownership of Forge mulligan state.

**Architecture:** Add a dedicated `DecisionType.MULLIGAN` for KEEP/REDRAW and a parent `MulliganSession` keyed by game and acting player. Reuse the existing sequential `CARD_SELECTION` primitive through a `MULLIGAN_BOTTOM` adapter, generalizing its source metadata to allow an absent `SpellAbility` without introducing a fake source. Add a narrow service observer for forced KEEP/unsupported empty-hand diagnostics and wrap existing AI callbacks fail-open.

**Tech Stack:** Java 17, Maven, TestNG, Forge game/AI modules, existing value-identity and decision-provider primitives.

---

### Task 1: Establish the branch and baseline

**Files:**
- No production files.
- Preserve untracked `FRL-02J-prompt-clean.md`.

- [x] **Step 1: Verify the approved checkpoint**

Run:

```powershell
git status
git branch --show-current
git rev-parse HEAD
git rev-parse origin/master
git diff
git diff --check
```

Expected: branch `master` before branching, both hashes `5ad875a9ec08b6d056ae780584b9b93a8e2c4c94`, no tracked diff, and only the known untracked prompt.

- [x] **Step 2: Create the feature branch**

Run:

```powershell
git switch -c frl/02j-mulligan-boundary 5ad875a9ec08b6d056ae780584b9b93a8e2c4c94
```

Expected: current branch `frl/02j-mulligan-boundary` at the approved SHA.

### Task 2: Add the atomic MULLIGAN contract test-first

**Files:**
- Create: `forge-gui-desktop/src/test/java/forge/game/decision/MulliganDecisionProviderTest.java`
- Create: `forge-game/src/main/java/forge/game/decision/MulliganCandidateKind.java`
- Create: `forge-game/src/main/java/forge/game/decision/MulliganContext.java`
- Create: `forge-game/src/main/java/forge/game/decision/MulliganSession.java`
- Create: `forge-game/src/main/java/forge/game/decision/MulliganDecisionProvider.java`
- Modify: `forge-game/src/main/java/forge/game/decision/DecisionType.java`
- Modify: `forge-game/src/main/java/forge/game/decision/DecisionRequest.java`
- Modify: `forge-game/src/main/java/forge/game/decision/LegalCandidate.java`

- [ ] **Step 1: Write failing TestNG cases**

Cover: two semantic candidates, separate acting/starting player IDs, zero cards-to-return, one outstanding request, KEEP terminalization, REDRAW round completion without generating a future callback, exact hand identity snapshot, stale stage/hand/starting-player rejection, and explicit unsupported empty-hand state.

- [ ] **Step 2: Run the focused test and verify the expected compile failure**

Run:

```powershell
mvn -pl forge-gui-desktop -am -Dtest=MulliganDecisionProviderTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: failure because the new MULLIGAN types and provider do not yet exist.

- [ ] **Step 3: Implement the minimal value-only contract**

Add `KEEP` and `REDRAW` candidate kinds with semantic keys `MULLIGAN|KEEP` and `MULLIGAN|REDRAW`. Keep `MulliganContext` free of `Player`, `Game`, `Card`, `PlayerZone`, `CardCollection`, and `SpellAbility`. Key session storage by game ID and acting-player ID; never use a single global current session.

- [ ] **Step 4: Run the focused test and verify green**

Run the same Maven command. Expected: all `MulliganDecisionProviderTest` methods pass.

### Task 3: Generalize CARD_SELECTION source metadata without a fake SpellAbility

**Files:**
- Create: `forge-game/src/main/java/forge/game/decision/CardSelectionAdapter.java`
- Modify: `forge-game/src/main/java/forge/game/decision/CardSelectionContext.java`
- Modify: `forge-game/src/main/java/forge/game/decision/CardSelectionSession.java`
- Modify: `forge-game/src/main/java/forge/game/decision/CardSelectionDecisionProvider.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/CardSelectionDecisionProviderTest.java`

- [ ] **Step 1: Add failing source-absent and adapter-identity tests**

Add TestNG cases proving a callback-backed selection session can start with `source == null`, exposes absent source-card values, reports `selectionAdapter=MULLIGAN_BOTTOM`, and still preserves existing source-backed discard behavior.

- [ ] **Step 2: Run the focused CARD_SELECTION tests and verify red**

Run:

```powershell
mvn -pl forge-gui-desktop -am -Dtest=CardSelectionDecisionProviderTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: the new source-absent tests fail at the current mandatory `SpellAbility` check or host-card dereference; existing tests must continue to compile and run.

- [ ] **Step 3: Implement explicit nullable source semantics**

Make `SpellAbility` optional only for the new callback adapter. Preserve source card ID/timestamp for spell-backed selections. Add an explicit adapter value with only `DISCARD` and `MULLIGAN_BOTTOM`. Keep `decisionSequenceId` and `actionSubdecisionIndex` null for Mulligan bottom sessions.

- [ ] **Step 4: Run both new and existing CARD_SELECTION tests**

Run:

```powershell
mvn -pl forge-gui-desktop -am -Dtest=CardSelectionDecisionProviderTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: all existing FRL-02F tests and the new source-absent regressions pass.

### Task 4: Implement the MULLIGAN_BOTTOM adapter test-first

**Files:**
- Create: `forge-game/src/main/java/forge/game/decision/MulliganBottomAdapter.java`
- Create: `forge-gui-desktop/src/test/java/forge/game/decision/MulliganBottomAdapterTest.java`

- [ ] **Step 1: Write failing adapter tests**

Use real Forge `Game`, `Player`, and hand cards. Cover exact `min=max=cardsToReturn`, 7-to-6 candidate shrinkage, zero-card completion, callback-supplied hand authority, duplicate-name identity, exact count validation, stale timestamp rejection, no live zone mutation, and completion order `[C,A]` remaining `[C,A]`.

- [ ] **Step 2: Run the adapter tests and verify red**

Run:

```powershell
mvn -pl forge-gui-desktop -am -Dtest=MulliganBottomAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: failure because `MulliganBottomAdapter` does not yet exist.

- [ ] **Step 3: Implement the adapter**

Snapshot only the exact callback `CardCollectionView`. Use `(cardId, gameTimestamp)` for membership and live-card resolution. Generate deterministic candidate ordering but never reorder `selectedIdentities` or the completed result. Reject impossible counts before session creation and reject all domain/identity/duplicate errors as mapping failures.

- [ ] **Step 4: Run the adapter tests and verify green**

Run the same Maven command. Expected: all adapter tests pass with no hand/library mutation.

### Task 5: Add forced KEEP and empty-hand observer behavior

**Files:**
- Create: `forge-game/src/main/java/forge/game/decision/MulliganDecisionDiagnostics.java`
- Modify: `forge-game/src/main/java/forge/game/mulligan/MulliganService.java`
- Create: `forge-gui-desktop/src/test/java/forge/game/decision/MulliganDiagnosticsIntegrationTest.java`

- [ ] **Step 1: Write failing service-observer tests**

Cover: `canMulligan == false` records forced KEEP without invoking the controller; an empty-hand callback produces an explicit unsupported state; the original Forge result remains unchanged; observer state is isolated by game and player.

- [ ] **Step 2: Run the integration test and verify red**

Run:

```powershell
mvn -pl forge-gui-desktop -am -Dtest=MulliganDiagnosticsIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: failure because the observer and diagnostics do not yet exist.

- [ ] **Step 3: Add the narrow observer seam**

Observe only the forced branch and lifecycle state. Do not alter London accounting, redraw, tuck, shuffle, stages, or controller invocation. Reject empty-hand neutral admission with `UNSUPPORTED_EMPTY_HAND_MULLIGAN` or `ENGINE_MULLIGAN_TERMINATION_MISMATCH`, then fail open to Forge's unchanged controller behavior.

- [ ] **Step 4: Add AI callback diagnostics**

Wrap `PlayerControllerAi.mulliganKeepHand` and `tuckCardsViaMulligan` without changing their returned boolean or ordered `CardCollectionView`. Map AI results to semantic candidates, validate tuck results, and record `MULLIGAN_STATE/MAPPING_FAILED` on invalid mapping.

- [ ] **Step 5: Run integration tests and verify green**

Run the same Maven command. Expected: forced, empty-hand, AI mapping, fail-open, and two-player isolation tests pass.

### Task 6: Update the FRL-02J report and verification evidence

**Files:**
- Modify: `docs/AI-ML DOCS/FRL_02J_MULLIGAN_REPORT.md`

- [ ] **Step 1: Write the failing report assertions as test/evidence checklist**

Record the supported v0 contract, Forge sequential timing gap, unsupported empty-hand edge, source-absent CARD_SELECTION generalization, forced KEEP observer, hidden-information contract, and exact metrics names.

- [ ] **Step 2: Update the report after implementation evidence exists**

Include exact changed files, test totals, unsupported-state counts, and benchmark results. Do not claim official simultaneous-mulligan equivalence.

### Task 7: Run complete verification and prepare review delivery

**Files:**
- No additional source scope.

- [ ] **Step 1: Run focused decision tests**

```powershell
mvn -pl forge-gui-desktop -am -Dtest=MulliganDecisionProviderTest,MulliganBottomAdapterTest,MulliganDiagnosticsIntegrationTest,CardSelectionDecisionProviderTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 2: Run relevant AI tests**

```powershell
mvn -pl forge-ai -am test
```

- [ ] **Step 3: Run the decision suite and package build**

```powershell
mvn -pl forge-game,forge-ai,forge-gui,forge-gui-desktop -am test
mvn -pl forge-gui-desktop -am package -DskipTests
```

- [ ] **Step 4: Run checkstyle and repository checks**

```powershell
mvn -pl forge-gui-desktop -am checkstyle:check
git diff --check
git status --short
```

Confirm `FRL-02J-prompt-clean.md` is still untracked and absent from staged files.

- [ ] **Step 5: Run the fixed-seed benchmark**

Run the two approved 10-game matchup sets without changing decks, AI policy, or mulligan mode. Record raw callbacks, strategic/forced events, tuck counts, mapping failures, unsupported states, and latency percentiles.

- [ ] **Step 6: Commit only intended files**

```powershell
git add forge-game forge-ai forge-gui-desktop/src/test docs/AI-ML DOCS/FRL_02J_MULLIGAN_REPORT.md docs/superpowers/plans/2026-08-10-frl-02j-mulligan-boundary.md
git diff --cached --name-only
git commit -m "feat: add Forge mulligan decision boundary"
```

Verify the untracked prompt is not staged.

- [ ] **Step 7: Push and open a Draft PR**

```powershell
git push -u origin frl/02j-mulligan-boundary
```

Open a Draft PR only; do not mark ready or merge. Report branch, exact commit, PR URL, changed files, tests, benchmark, and unsupported counts.
