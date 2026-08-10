# FRL-02K-A3 Optional Trigger Event Projection Audit Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Determine whether any observed normal optional no-cost trigger has a minimum sufficient public event projection without exposing opaque Forge runtime objects or hidden information.

**Architecture:** Reproduce the A2 callback set, cluster it by semantic trigger shape, focus on the dominant intrinsic no-cost shape, and use only test/audit evidence to classify decision relevance, provenance, identity, visibility, and neutrality. No production confirmation adapter or DTO is introduced.

**Tech Stack:** Java/TestNG, Maven, Forge `CardView` visibility APIs, `ForgeStateFingerprint`, `DeterminismAuditRandom`, existing A2 attribution evidence, and GitHub Draft PR workflow.

---

### Task 1: Establish the merged A3 checkpoint

**Files:** No production files.

- [ ] **Step 1: Verify the merged checkpoint**

Run:

```powershell
git status
git branch --show-current
git rev-parse HEAD
git rev-parse origin/master
git merge-base HEAD origin/master
git diff --check
```

Expected: clean `frl/02k-a3-trigger-event-projection`, with `HEAD`, `origin/master`, and the merge base equal to `62ea04e8dd2c0f374208a4ecaeba66d5d423422f`.

- [ ] **Step 2: Confirm protected worktrees are unchanged**

Run `git -C C:\forgeAI status --short --branch` and `git -C C:\forgeAI-determinism-gate status --short --branch`. Do not write, reset, clean, stash, or switch either worktree.

### Task 2: Reproduce and cluster the A2 occurrence set

**Files:** Test-only audit evidence if a reproducibility assertion is needed.

- [ ] **Step 1: Reuse the existing A2 attribution path**

Locate the existing JDI probe and workload command. Do not change Forge-AI behavior. Run `Izzet Guild Kit` versus `Dimir Guild Kit`, seed `20260810`, ten games, in a fresh JVM.

- [ ] **Step 2: Preserve the complete callback record**

Retain game/turn/phase, source and visibility, mode, definition candidate, provenance, decider/active player, cost class, AbilityKey/runtime categories, native result, and continuation state. Require exactly 26 callbacks; stop and explain any divergence.

- [ ] **Step 3: Produce semantic shape counts**

Cluster all occurrences, including Gelectrode `SpellCast -> Untap`, Blood Operative zone-change and surveil-cost triggers, Lazav clone, Nightveil/Tibor damage-play triggers, and any additional measured shape. Counts must sum to 26.

### Task 3: Trace and analyze the dominant candidate

**Files:** Test-only audit evidence.

- [ ] **Step 1: Select the measured dominant shape**

Choose the most frequent normal, non-static, intrinsic, optional, no-cost, public-source shape. Use Gelectrode only if the rerun confirms it is dominant.

- [ ] **Step 2: Trace Forge lifecycle**

Document card script, `TriggerHandler` matching and `runParams`, `WrappedAbility` construction, copied objects, `WrappedAbility.resolve`, the native callback, and effect resolution. Separate engine qualification data from policy context.

- [ ] **Step 3: Classify every observed AbilityKey**

Use exactly one of `REQUIRED_FOR_POLICY_CONTEXT`, `REQUIRED_FOR_ENGINE_ONLY`, `DERIVABLE_FROM_PUBLIC_CONTEXT`, `REDUNDANT`, `HIDDEN_OR_UNSAFE`, or `UNKNOWN`. Do not expose raw `CardLKI`, `SpellAbility`, targets, or collections.

### Task 4: Prove public sufficiency, visibility, and neutrality

**Files:** Modify only `forge-gui-desktop/src/test/java/forge/ai/ability/FRL02KConfirmationAuditTest.java`.

- [ ] **Step 1: Use a fixed decider perspective**

Keep `deciderViewer` fixed while varying public `triggeringPlayer` values. Check source visibility only against `deciderViewer`; assert distinct public events produce distinct conceptual contexts.

- [ ] **Step 2: Test opaque-object invariance**

If an opaque field is proven `ENGINE_ONLY`, compare public-equivalent decisions with different internal representations and require identical projections. If no safe fixture exists, record `NOT_PROVEN`.

- [ ] **Step 3: Test hidden-information rejection**

Use Forge `CardView.canBeShownTo` and `canFaceDownBeShownTo` for hidden-zone, face-down, hidden-LKI, hidden-source, and mixed hidden collections. Fail closed without identity export.

- [ ] **Step 4: Test state and RNG neutrality**

Compare `ForgeStateFingerprint` before/after supported and rejected projections and assert zero `DeterminismAuditRandom` draws. Exceptions become `UNSUPPORTED`.

### Task 5: Refine identity, provenance, and occurrence semantics

**Files:** Test-only evidence and the A3 report section.

- [ ] **Step 1: Trace all three `intrinsic == false` occurrences**

Record construction path, spawning ability, copy/grant/generation evidence, and stable visible definition mapping. Classify each as `TRULY_GENERATED_UNSTABLE`, `COPIED`, `GRANTED`, `DERIVED_BUT_STABLY_ATTRIBUTABLE`, or `OTHER`; do not admit by coverage pressure.

- [ ] **Step 2: Keep identity layers separate**

Use canonical rules identity, card state, trigger discriminator, mode, and normalized semantic parameters for conceptual semantic identity. Keep set/printing, runtime ID, timestamp, and `Trigger.getId()` diagnostic/provenance-only.

- [ ] **Step 3: Verify occurrence ordering**

Retain a deterministic monotonic trace-local occurrence index and prove same-seed fresh JVM ordering for the controlled fixture without PID, time, randomness, object identity, or `Trigger.getId()`.

### Task 6: Reclassify, decide, and document

**Files:** `docs/AI-ML DOCS/FRL_02K_CONFIRMATION_AUDIT.md`; `ML_STRATEGY.md` only if a new accepted fact requires it.

- [ ] **Step 1: Reclassify all 26 occurrences**

Compare A2 and A3 buckets; report strict A2 admitted, A3 semantically projectable, and remaining blockers. Preserve the total of 26 when the workload is unchanged.

- [ ] **Step 2: State admission and exclusion predicates**

Require the engine-owned `WrappedAbility.resolve` seam, optional normal non-static lifecycle, trusted provenance, stable definition/occurrence identity, public typed decision-relevant objects, no nonzero cost, no hidden data, and absent `ActionContinuation`. Exclude mandatory, static, delayed, generated/copied/granted, helper, and unsupported provenance paths.

- [ ] **Step 3: Review generalization and trace compatibility**

Use player/seat identities rather than `SELF`/`OPPONENT`. Confirm `[ACCEPT, DECLINE]` remains compatible with `DECISION_TRACE_V2`; do not introduce V3.

- [ ] **Step 4: Select exactly one A3 verdict**

Choose only `IMPLEMENT_GELECTRODE_OPTIONAL_TRIGGER_SLICE`, `IMPLEMENT_OPTIONAL_TRIGGER_EVENT_V0`, `NO_SAFE_V0_YET`, or `DEFER_CONFIRMATION_AND_PROCEED_TO_OTHER_GAPS` based on evidence.

### Task 7: Verify and publish the audit-only milestone

**Files:** Audit report and test-only files only.

- [ ] **Step 1: Run required gates**

Run focused A1/A2/A3 tests, the expanded decision/determinism selection, `FullGameCollectorNeutralityTest`, `WorkerIsolationSmokeTest`, package, configured `validate`/Checkstyle, and `git diff --check`. Keep K0/A2 labels separate from post-A3 totals.

- [ ] **Step 2: Prove production scope is empty**

Run `git diff --name-only` and inspect all `src/main` paths. The production-source list must be empty.

- [ ] **Step 3: Commit and push audit-only changes**

Use intentional commits, push `frl/02k-a3-trigger-event-projection`, and open `FRL-02K-A3: audit optional trigger event projection` as Draft. Do not mark Ready, merge, or implement production CONFIRMATION.
