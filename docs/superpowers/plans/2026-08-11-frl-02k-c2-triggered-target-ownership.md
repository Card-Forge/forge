# FRL-02K-C2 Triggered TARGET Ownership Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task with verification checkpoints.

**Goal:** Establish the current Forge lifecycle and ownership of Blood Operative's stored target A, classify reuse of the existing TARGET provider, and record audit-only evidence without adding Blood production support.

**Architecture:** Keep Forge's existing legality and target-storage paths authoritative. Add opt-in, neutral diagnostics at trigger construction, target preparation, stack insertion, confirmation, and effect resolution; use stable public projections and a trace-local diagnostic token only. Keep all strategic TARGET and CONFIRMATION production behavior unchanged.

**Tech Stack:** Java 17, Maven reactor, Forge game/AI modules, TestNG, existing DECISION_TRACE_V2 and deterministic audit utilities, Markdown architecture reports.

---

### Task 1: Lock the clean C2 checkpoint and source map

**Files:**
- Read: forge-game/src/main/java/forge/game/trigger/TriggerHandler.java
- Read: forge-game/src/main/java/forge/game/zone/MagicStack.java
- Read: forge-game/src/main/java/forge/game/spellability/SpellAbility.java
- Read: forge-ai/src/main/java/forge/ai/PlayerControllerAi.java
- Read: forge-ai/src/main/java/forge/ai/ability/ChangeZoneAi.java
- Read: forge-gui/src/main/java/forge/player/PlayerControllerHuman.java

- [ ] Verify HEAD, origin/master, merge-base, branch, status, and git diff --check.
- [ ] Record the Blood script, trigger construction, Human target path, AI target path, stack target validation, and WrappedAbility.resolve path in the audit report.
- [ ] Do not change Forge behavior while building the source map.

### Task 2: Add the failing C2 lifecycle audit test

**Files:**
- Create: forge-gui-desktop/src/test/java/forge/view/FRL02KTriggeredTargetOwnershipAuditTest.java

- [ ] Add a fresh-JVM canonical reactive workload test using the existing ChildJvmSupport pattern and forge.triggeredTarget.auditFile.
- [ ] Assert the required Blood sequence, target counts, chooser, null continuation, stack-time ordering, A/B/C projections, and audit-on/off deterministic trace equality.
- [ ] Assert the canonical locks: 26 raw confirmTrigger, 17 Gelectrode, 5 other no-cost, 1 cost-bearing, 3 provenance-untrusted, two Blood occurrences, one A/B match and one A/B divergence.
- [ ] Add provider fixture assertions for 0, 1, and multiple legal Blood-shaped graveyard targets, candidate completeness, public visibility, and null continuation.
- [ ] Run the focused test before adding diagnostics; it must fail because the C2 audit output does not yet exist.

### Task 3: Implement opt-in neutral lifecycle diagnostics

**Files:**
- Create: forge-game/src/main/java/forge/game/decision/TriggeredTargetAuditDiagnostics.java
- Modify: forge-game/src/main/java/forge/game/trigger/TriggerHandler.java
- Modify: forge-game/src/main/java/forge/game/zone/MagicStack.java
- Modify: forge-game/src/main/java/forge/game/spellability/SpellAbility.java
- Modify: forge-ai/src/main/java/forge/ai/PlayerControllerAi.java
- Modify: forge-game/src/main/java/forge/game/trigger/WrappedAbility.java
- Modify: forge-game/src/main/java/forge/game/ability/effects/ChangeZoneEffect.java

- [ ] Gate all diagnostics behind forge.triggeredTarget.auditFile; the disabled path must perform no projection, RNG draw, state mutation, or AI invocation.
- [ ] Use an engine-local identity map only for lifecycle bookkeeping; export a monotonically allocated trace-local token, never Java identity, hash code, Trigger.getId(), PID, timestamp, or raw Forge objects.
- [ ] Record trigger construction/queue, Human/common SpellAbility.setupTargets completion, AI prepareSingleSa completion, stack insertion before/after, resolution/confirmation, and ChangeZone effect target projections.
- [ ] Record caller/selector labels and target counts before/after the target-selection call so the first 0-to-1 transition is attributable to the existing caller path.
- [ ] Preserve public visibility checks and typed target projections; hidden target identity must be represented as HIDDEN or fail closed.
- [ ] Ensure audit code catches its own runtime failures and cannot alter gameplay.

### Task 4: Make the test green and classify provider reuse

**Files:**
- Modify: forge-gui-desktop/src/test/java/forge/view/FRL02KTriggeredTargetOwnershipAuditTest.java
- Modify: docs/AI-ML DOCS/FRL_02K_CONFIRMATION_AUDIT.md

- [ ] Run the focused C2 tests and fix only diagnostics/test assertions, never production decision semantics.
- [ ] Verify TargetDecisionProvider accepts the underlying triggered SpellAbility, enumerates Forge-legal cards, applies through TargetChoices, supports null continuation, forces one candidate, rejects zero mandatory candidates, and exposes multiple public candidates in deterministic semantic-key order.
- [ ] Classify wrapper-vs-underlying ability handling, chooser/activating-player assumptions, MustTarget, visibility, teacher mapping, and correlation limits.
- [ ] Document the exact call graph, stage order, target ownership matrix, provider matrix, state/RNG neutrality, 0/1/many behavior, Human/AI parity, and the next smallest milestone. Do not add a production TARGET seam or Blood CONFIRMATION support.

### Task 5: Re-run regression and broad validation

**Files:**
- Read: all changed files

- [ ] Re-run the focused C2 test and the existing B1/C/C1 tests.
- [ ] Run mvn -pl forge-gui-desktop -am test and record exact tests, failures, errors, skipped, and the known port-55556 rule.
- [ ] Run mvn -pl forge-gui-desktop -am -DskipTests package.
- [ ] Run mvn -pl forge-gui-desktop -am -DskipTests validate.
- [ ] Run git diff --check, inspect the final diff, and stop after the report and Draft PR if all gates are satisfied.

### Task 6: Publish only the audit result

**Files:**
- Read: final diff and report

- [ ] Commit only the C2 audit, focused tests, diagnostics, report, and this plan.
- [ ] Push the dedicated C2 branch and create a Draft PR titled FRL-02K-C2: audit triggered target ownership only after fresh verification.
- [ ] Do not mark ready, merge, or implement triggered TARGET ownership, Blood CONFIRMATION, Lazav, PAYMENT, ORDER, or DAMAGE_ASSIGNMENT.
