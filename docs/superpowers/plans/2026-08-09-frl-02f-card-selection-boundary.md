# FRL-02F CARD_SELECTION Boundary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a completion-safe neutral sequential card-selection primitive and a diagnostic-only Izzet Charm discard adapter without changing Forge decisions or game mutation timing.

**Architecture:** `CardSelectionSession` owns immutable callback authority plus session-local selections. `CardSelectionDecisionProvider` generates and applies atomic CARD_SELECTION requests, while `DiscardEffect` gates the narrow v0 shape and replays the unchanged controller result into separate discard diagnostics.

**Tech Stack:** Java 17, Forge game engine, JUnit 5, Maven, CSV diagnostics.

---

### Task 1: Neutral request model and exact identity

**Files:**
- Modify: `forge-game/src/main/java/forge/game/decision/DecisionType.java`
- Modify: `forge-game/src/main/java/forge/game/decision/DecisionRequest.java`
- Modify: `forge-game/src/main/java/forge/game/decision/LegalCandidate.java`
- Create: `forge-game/src/main/java/forge/game/decision/CardSelectionCandidateKind.java`
- Create: `forge-game/src/main/java/forge/game/decision/CardSelectionCard.java`
- Create: `forge-game/src/main/java/forge/game/decision/CardSelectionContext.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/CardSelectionDecisionProviderTest.java`

- [ ] Write tests that require CARD_SELECTION-only context, SELECT_CARD/DONE candidates, `(cardId,gameTimestamp)` keys, deterministic ordering, duplicate-name separation, safe visible fields, and wrong-request ownership rejection.
- [ ] Run `mvn -pl forge-gui-desktop -am -Dtest=CardSelectionDecisionProviderTest -Dsurefire.failIfNoSpecifiedTests=false test` and confirm compilation/test failure because the model does not exist.
- [ ] Add the minimal typed request/candidate/context model. Add `CARD_SELECTION` at the end of `DecisionType`; do not reuse TARGET fields for selection semantics.
- [ ] Rerun the focused test and require it to pass before extending behavior.

### Task 2: Mutation-free sequential session

**Files:**
- Create: `forge-game/src/main/java/forge/game/decision/CardSelectionSession.java`
- Create: `forge-game/src/main/java/forge/game/decision/CardSelectionDecisionProvider.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/CardSelectionDecisionProviderTest.java`

- [ ] Add failing tests for exact-two requests, selected-card removal, automatic max completion, DONE legality, immediate zero/zero completion, empty optional forced DONE, impossible minimum, selected-count corruption, request ownership, and forcedness.
- [ ] Add failing tests proving the first selection leaves the live hand unchanged and completion returns exact live cards only.
- [ ] Add failing tests for a moved/re-entered card with the same ID but a new timestamp, a moved-out card, and another same-name copy; all must fail without substitution.
- [ ] Add failing tests proving `validCards` snapshot membership is authority and visible supersets never become candidates; hidden selectable identities return `UNSUPPORTED_HIDDEN_CARD_SELECTION`.
- [ ] Implement session creation, generation, apply, completion, deterministic semantic-key ordering, and explicit status/reason values with no live-game mutation and no `DiscardValid` recomputation.
- [ ] Rerun the provider test after each behavior and preserve red-green evidence.

### Task 3: Scoped session identity and optional continuation

**Files:**
- Modify: `forge-game/src/main/java/forge/game/decision/CardSelectionSession.java`
- Modify: `forge-game/src/main/java/forge/game/decision/CardSelectionContext.java`
- Modify: `forge-game/src/main/java/forge/game/decision/CardSelectionDecisionProvider.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/CardSelectionDecisionProviderTest.java`

- [ ] Add failing tests that session creation consumes no continuation index, each generated request increments the session step, a real continuation allocates one action subdecision per request, and null continuation leaves action identity null.
- [ ] Implement provider/game-local session IDs and step indices. Store optional continuation metadata only on generated requests; do not modify `ActionContinuation`.
- [ ] Rerun the focused test and confirm resolution-style sessions group steps without an action sequence.

### Task 4: Narrow Izzet Discard adapter

**Files:**
- Modify: `forge-game/src/main/java/forge/game/ability/effects/DiscardEffect.java`
- Create: `forge-game/src/main/java/forge/game/decision/DiscardCardSelectionAdapter.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/CardSelectionDiscardIntegrationTest.java`

- [ ] Add failing real-card tests for Izzet Charm `Draw 2 -> Discard 2`, the exact adapter capability predicate, null ActionContinuation, unchanged hand after synthetic step one, completed two-card collection, and normal Forge discard application only after callback completion.
- [ ] Add failing rejection tests for chooser/affected mismatch, Reveal/Look/YouChoose modes, RevealNumber, UnlessType, hidden selectable identity, and unresolved confirmation shapes. Verify the generic primitive still models chooser/affected separation independently.
- [ ] Implement the narrow capability gate and callback snapshot lifecycle immediately before `chooseCardsToDiscardFrom` without changing ordering or discard code.
- [ ] Rerun integration tests and confirm unsupported shapes continue through the original Forge callback without neutral replay claims.

### Task 5: Diagnostic-only AI result replay

**Files:**
- Modify: `forge-game/src/main/java/forge/game/decision/PriorityActionDiagnostics.java`
- Modify: `forge-game/src/main/java/forge/game/ability/effects/DiscardEffect.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/PriorityActionDiagnosticsTest.java`
- Test: `forge-gui-desktop/src/test/java/forge/game/decision/CardSelectionDiscardIntegrationTest.java`

- [ ] Add failing tests for separate `CARD_SELECTION_DISCARD_CALLBACK`, `CARD_SELECTION`, and `CARD_SELECTION_STATE` records containing adapter, scoped session identity, step index, forcedness, candidates, generation time, and shrinkage.
- [ ] Add failing tests proving successful replay returns the original controller collection object/value unchanged and mapping failure records state but never changes or rejects the Forge result.
- [ ] Add a failing regression showing legacy `DOWNSTREAM/CARD_SELECTION` cost events are not counted as discard callbacks or sessions.
- [ ] Implement raw callback recording, deterministic selected-set replay by semantic key, fail-open diagnostic mapping, and separate aggregation fields. Do not change `chooseCardsForCost`.
- [ ] Rerun focused diagnostics and integration tests.

### Task 6: Full validation, benchmarks, and report

**Files:**
- Create: `docs/AI-ML DOCS/FRL_02F_CARD_SELECTION_REPORT.md`

- [ ] Run the new CARD_SELECTION test classes and record test counts and duration.
- [ ] Run the complete focused `forge.game.decision` suite with `-Dsurefire.failIfNoSpecifiedTests=false` and record results.
- [ ] Run the repository package build with the established skip flags only where prior milestone builds use them; record the exact command and exit status.
- [ ] Run `git diff --check` and require no output.
- [ ] Run ten seeded `Dead and Alive` versus `Air Forces` games and ten seeded `Izzet Guild Kit` versus `Dimir Guild Kit` games where practical, using separate diagnostic output files.
- [ ] Compute raw discard callbacks, supported/unsupported sessions, atomic requests, forced/strategic counts, candidate count mean/p50/p95/max, generation p50/p95/p99, steps/callback mean/p50/p95/max, and per-step shrinkage without mixing cost CARD_SELECTION events.
- [ ] Write the report with confirmed evidence, unsupported boundaries, benchmark limitations, and no RL claims.

### Task 7: Review and publish

**Files:**
- Review all FRL-02F files above.

- [ ] Compare the final diff line-by-line with the approved design and mandatory refinements.
- [ ] Request an independent code review against base `d06ebc79852bfcc05e466d6d3a593524689bad1a`; fix all Critical and Important findings and rerun affected verification.
- [ ] Run fresh final focused tests, package build, `git diff --check`, and `git status --short` before claiming completion.
- [ ] Stage only FRL-02F files and commit with a scoped message.
- [ ] Push `frl/02f-card-selection-boundary` and open a Draft PR targeting `master` with validation and benchmark evidence. Do not merge or mark ready.
