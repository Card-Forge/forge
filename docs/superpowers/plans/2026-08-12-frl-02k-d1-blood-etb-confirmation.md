# FRL-02K-D1 — Exact Blood Operative ETB CONFIRMATION Slice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the exact Blood Operative ChangesZone -> Battlefield confirmation slice on top of the existing controller-local ConfirmationDecisionProvider, while preserving C2A Target-A ownership, native Forge behavior outside the admitted profile, hidden-information safety, continuation fail-closed behavior, and DECISION_TRACE_V2 provenance.

**Architecture:** Extend the existing provider with one explicit Blood confirmation profile and a nullable public Target-A projection. Extract only the ownership-neutral common Blood semantic admission checks into a small stateless helper shared by C2A and D1; keep C2A's empty-initial-target invariant and D1's exactly-one-public-target invariant in their respective boundaries. WrappedAbility.resolve remains the engine-owned orchestration seam and performs apply plus live-A integrity validation before diagnostics and terminal trace recording.

**Tech Stack:** Java 17, Maven reactor, TestNG, Forge game/AI modules, existing CardSelectionCard, controller-local target and confirmation seams, DeterminismTrace/DECISION_TRACE_V2, PowerShell on Windows.

---

## Scope guard and authoritative inputs

The implementation runs only in the isolated worktree:

~~~text
worktree: C:\forgeAI-blood-confirmation-d1
branch:    frl/02k-d1-blood-etb-confirmation
base:      c83a1e2b1209d1bfa9f671a5d3acc885133dc2cb
~~~

The protected checkout C:\forgeAI and the existing C2A worktrees are read-only reference checkouts for this task. Do not switch the working directory to one of them, rewrite their branches, or present their state as D1 progress.

The approved design is:

~~~text
docs/superpowers/specs/2026-08-12-frl-02k-d1-blood-etb-confirmation-design.md
~~~

The design was patched for the three P1 findings and three P2 findings, then committed as 77a731215a1. The implementation must preserve these decisions:

- resolver == null plus unsupported/hidden/non-exact/stale-before-request means no D1 request and the existing native Forge path remains available.
- resolver != null plus unsupported/hidden/non-exact/stale-before-request means a sanitized hard failure with no native fallback.
- an active ActionContinuation never creates a D1 request; external ownership rejects before resolver or native confirmation callback.
- Blood has triggeringPlayerId == null; no AbilityKey.Activator requirement, sentinel, or decider duplication is allowed for Blood.
- C2A owns Target A; D1 owns only [ACCEPT, DECLINE].
- terminal trace order is request -> choose -> apply/integrity -> diagnostics result -> terminal trace -> continue/return.
- native A-restoration failure is MAPPING_FAILED with true/true; an external resolver/application failure is not a native mapping failure.

## File map

Expected production files to create:

- forge-game/src/main/java/forge/game/decision/BloodOperativeEtbProfile.java
  - stateless, ownership-neutral common Blood semantic validation and canonical normalized parameter maps shared by C2A and D1.

Expected production files to modify:

- forge-game/src/main/java/forge/game/decision/ConfirmationDecisionProvider.java
  - explicit Blood profile admission, immutable public Target-A capture, resolver ownership snapshot, exact candidate validation, and A-integrity validation before applying ACCEPT/DECLINE.
- forge-game/src/main/java/forge/game/decision/ConfirmationDecisionContext.java
  - nullable event-player identity and nullable typed Target-A projection.
- forge-game/src/main/java/forge/game/decision/ConfirmationTriggerProfile.java
  - BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD plus its trace label.
- forge-game/src/main/java/forge/game/decision/ConfirmationEventType.java
  - CHANGES_ZONE.
- forge-game/src/main/java/forge/game/decision/TriggeredTargetDecisionCoordinator.java
  - consume the common Blood semantic validator while retaining C2A-specific empty-initial-target and target-provider ownership rules.
- forge-game/src/main/java/forge/game/trigger/WrappedAbility.java
  - profile-aware trace label, apply-before-terminal-trace ordering, native mapping failure handling, and external no-fallback behavior.
- forge-game/src/main/java/forge/game/decision/ConfirmationDiagnostics.java
  - profile-neutral wording and sanitized typed profile CSV field.
- forge-game/src/main/java/forge/game/decision/UnsupportedConfirmationDecisionException.java
  - profile-neutral sanitized message contract.

Expected focused test files to create:

- forge-gui-desktop/src/test/java/forge/game/decision/BloodOperativeConfirmationDecisionProviderTest.java
  - direct provider, context, admission, candidate, native, external, and adversarial tests.
- forge-gui-desktop/src/test/java/forge/game/decision/BloodConfirmationOwnershipMatrixTest.java
  - real route coverage for the independent TARGET/CONFIRMATION 2x2 matrix, fizzle, continuation, and terminal trace semantics.

Expected focused test files to modify:

- forge-gui-desktop/src/test/java/forge/game/decision/GelectrodeConfirmationDecisionProviderTest.java
  - retain B1 non-null triggeringPlayerId, target-free context, and sanitized exception regressions.
- forge-gui-desktop/src/test/java/forge/game/decision/DeterminismTraceV2Test.java
  - retain external CONFIRMATION CHOSEN false/false, native mapping failure, and BC true/true eligibility rules.
- forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetDecisionCoordinatorTest.java
  - retain C2A exact Blood target tests and add common-profile parity assertions where the helper is shared.

Expected audit/document files to create or modify only after production tests and the production verification gate pass:

- docs/AI-ML DOCS/FRL_02K_D1_BLOOD_CONFIRMATION_AUDIT.md
- docs/AI-ML DOCS/FRL_02K_CONFIRMATION_AUDIT.md
- docs/AI-ML DOCS/ML_STRATEGY.md

Files explicitly out of scope:

- forge-gui/src/main/resources/forge/cardsfolder/b/blood_operative.txt and every card script/resource file.
- A new Blood-specific provider, generic CONFIRMATION adapter, generic boolean API, or public raw Forge-object projection.
- PayLife<3> / Blood Surveil PAYMENT behavior.
- Lazav, Cipher, Flip, Bid, Replacement, Static, ORDER, or DAMAGE_ASSIGNMENT semantics.
- ObservationEncoder, HistoryEvent, global learning code, or a new trace schema.
- Changes to PlayerControllerAi.confirmTrigger's existing temporary-B mechanism unless a focused regression proves the D1 boundary cannot be enforced without a minimal routing change.

## Public and internal API contract

Extend the explicit enums without introducing a generic profile registry:

~~~java
public enum ConfirmationTriggerProfile {
    GELECTRODE_SPELL_CAST_UNTAP_SELF("GELECTRODE_CONFIRMATION"),
    BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD("BLOOD_OPERATIVE_CONFIRMATION");

    private final String traceLabel;

    ConfirmationTriggerProfile(final String traceLabel0) {
        traceLabel = traceLabel0;
    }

    public String getTraceLabel() {
        return traceLabel;
    }
}
~~~

Add CHANGES_ZONE to ConfirmationEventType and keep SPELL_CAST unchanged. Extend ConfirmationDecisionContext with:

~~~java
private final CardSelectionCard targetPublicIdentity;
private final Integer triggeringPlayerId;

CardSelectionCard getTargetPublicIdentity();
Integer getTriggeringPlayerId();
~~~

The constructor remains package-private. sourcePublicIdentity remains required. targetPublicIdentity is null for Gelectrode and is exactly one public graveyard Card projection for Blood. triggeringPlayerId remains the Gelectrode AbilityKey.Activator player ID and is null for Blood. No context field may hold a Card, GameObject, SpellAbility, WrappedAbility, raw triggering-object map, request occurrence counter, or continuation object.

Use these controlled provider statuses for post-request failures in addition to the existing admission statuses:

~~~java
INVALID_EXTERNAL_CANDIDATE,
NATIVE_MAPPING_FAILED,
TARGET_A_INTEGRITY_FAILURE
~~~

The existing UnsupportedConfirmationDecisionException remains the controlled exception type. Its message must contain only the fixed profile-neutral prefix, the enum status name, and a bounded reason token. Resolver exception text, card/target names, object toString output, and stack details are never copied into the message or reason. A native NATIVE_MAPPING_FAILED status is the only confirmation-provider status that the wrapper maps to trace MAPPING_FAILED; an external invalid candidate or live-A mismatch leaves the request non-native and lets the existing trace finalization classify it as TRACE_INCOMPLETE.

The common helper is package-private and stateless. Its result carries only a controlled reason token:

~~~java
final class BloodOperativeEtbProfile {
    enum Failure {
        NULL_INPUT, SOURCE_IDENTITY, SOURCE_STATE, SOURCE_PROVENANCE,
        TRIGGER_DEFINITION, STATIC_EFFECT_DEFINITION, LIVE_EFFECT_DEFINITION,
        TARGETING_SHAPE
    }

    static Validation validateCommonSemanticProfile(WrappedAbility wrapper);

    static final class Validation {
        boolean isAdmitted();
        Failure getFailure();
    }
}
~~~

The helper validates only shared semantics: Blood Operative original/public provenance shape, intrinsic/non-static/non-copied/no-spawning trigger, exact ChangesZone trigger map, exact TrigChangeZone static ChangeZone map, live ApiType.ChangeZone, Graveyard-to-Exile ValidTgts=Card, one-target restriction shape, free cost, no Optional, no TargetingPlayer, and no sub/additional abilities. It does not decide chooser/decider/controller ownership, C2A empty targets, D1 Target-A projection, or resolver behavior.

## Task 1: Reconfirm the checkpoint and establish a usable baseline

Files: none.

- [ ] Verify that the protected checkout is clean and still at the required post-D base. Verify that the isolated branch is at the design-patch commit and has no unstaged files before adding the plan.

~~~powershell
$expected = 'c83a1e2b1209d1bfa9f671a5d3acc885133dc2cb'
$primary = @(git -C C:\forgeAI status --short)
if ($primary.Count -ne 0) { throw 'protected C:\forgeAI is not clean' }
if ((git -C C:\forgeAI rev-parse HEAD) -ne $expected) { throw 'protected HEAD drifted' }
if ((git -C C:\forgeAI rev-parse origin/master) -ne $expected) { throw 'origin/master drifted' }
if ((git -C C:\forgeAI merge-base HEAD origin/master) -ne $expected) { throw 'protected merge-base drifted' }

$isolated = @(git -C C:\forgeAI-blood-confirmation-d1 status --short)
if ($isolated.Count -ne 0) { throw 'D1 worktree is not clean before implementation' }
git -C C:\forgeAI-blood-confirmation-d1 branch --show-current
git -C C:\forgeAI-blood-confirmation-d1 rev-parse HEAD
git -C C:\forgeAI-blood-confirmation-d1 rev-parse origin/master
git -C C:\forgeAI-blood-confirmation-d1 merge-base HEAD origin/master
~~~

  Expected result: protected checkout is clean at the expected base; the D1 branch is frl/02k-d1-blood-etb-confirmation, and its merge-base is the same base. The plan file itself is the first intentional worktree change after this check.

- [ ] Confirm the mandatory D checkpoint in the authoritative document:

~~~powershell
rg -n "FRL_02K_D_PASS|FRL-02K-D|c83a1e2b1209d1bfa9f671a5d3acc885133dc2cb" "C:\forgeAI-blood-confirmation-d1\docs\AI-ML DOCS\FRL_02K_D_CONFIRMATION_REMAINDER_AUDIT.md"
~~~

  Expected result: the document declares FRL_02K_D_PASS and records the post-D checkpoint. If that declaration is absent, stop before production implementation.

- [ ] Repeat the focused baseline in split selectors because the earlier combined invocation exceeded the command wrapper timeout without producing a test summary. Use the quoted Surefire property so Maven treats it as a property rather than a lifecycle phase:

~~~powershell
mvn -pl forge-gui-desktop -am '-Dtest=GelectrodeConfirmationDecisionProviderTest,FRL02KConfirmationAuditTest,DeterminismTraceV2Test' '-Dsurefire.failIfNoSpecifiedTests=false' test
mvn -pl forge-gui-desktop -am '-Dtest=TriggeredTargetDecisionCoordinatorTest,FRL02KTriggeredTargetProviderAuditTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
mvn -pl forge-gui-desktop -am '-Dtest=FRL02KTriggeredTargetOwnershipAuditTest,FRL02KTriggeredTargetExternalOwnershipAuditTest,FRL02KChangesZoneProjectionAuditTest,PriorityActionDiagnosticsTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
~~~

  Expected result: each split command finishes with an explicit Maven/Surefire result. A timeout is recorded as UNVERIFIED, not as PASS or FAIL; split the timed-out selector again until it has a definitive result. A real pre-existing failure is a baseline blocker and must be recorded before any D1 production claim. Do not start production changes on an unclassified baseline.

- [ ] Commit the plan-only artifact after its self-review as docs: add FRL-02K-D1 implementation plan; no implementation file belongs in that commit.

## Task 2: Write RED tests for the public Blood confirmation contract

Files:

- Create forge-gui-desktop/src/test/java/forge/game/decision/BloodOperativeConfirmationDecisionProviderTest.java.
- Modify forge-gui-desktop/src/test/java/forge/game/decision/GelectrodeConfirmationDecisionProviderTest.java.

- [ ] Add a real Forge fixture that creates Blood Operative on the battlefield, finds the intrinsic ChangesZone trigger, creates its wrapped TrigChangeZone ability, and places exactly one legal Card A in the Graveyard. Keep the fixture local to the test class or use a test-only helper; do not expose it through production classes.

- [ ] Add the first RED test for exact request generation. It must expect:

~~~text
status = ADMITTED
request.decisionType = CONFIRMATION
request.isForced() = false
candidate semantic keys = [ACCEPT, DECLINE]
context.profile = BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD
context.event = CHANGES_ZONE
context.targetPublicIdentity = CardSelectionCard(A)
context.targetPublicIdentity.zone = Graveyard
context.triggeringPlayerId = null
context.deciderPlayerId = Forge decider seat
continuation metadata = absent/null
~~~

  Compare only value projections and typed enum names. Assert that the request context has no raw Forge engine object or occurrence metadata through the same reflection/value-safety style already used by the B1 tests. The current provider must fail this test with non-admission, proving RED before its implementation.

- [ ] Add fail-closed generation tests for zero targets, multiple targets, a non-Card target, a target outside Graveyard, a hidden target, a stale target identity, and a target that cannot be projected to the decider. Each case must assert request == null; resolver ownership is tested later at the wrapper boundary.

- [ ] Add the exact common-profile mutation table. For each mutation, assert D1 is not admitted and the existing C2A classifier remains unsupported for the same semantic defect:

  - wrong source name, non-original state, clone, copied wrapper, copied live ability, generated/non-intrinsic trigger, static trigger, spawning ability;
  - wrong trigger mode/origin/destination/valid-card/optional-decider/execute, unknown semantic trigger parameter, or missing TrigChangeZone SVar;
  - wrong static ChangeZone API/origin/destination/valid targets, unknown semantic static parameter, optional/targeting-player static contamination;
  - wrong live API, target zone, random targeting, min/max, non-free cost, optional live parameter, targeting player, sub-ability, or additional ability.

  Do not add a second legality oracle in the test. Mutate the actual Forge trigger or live ability used by the C2A fixture and assert the shared semantic boundary only.

- [ ] Update the B1 test to assert Integer.valueOf(player.getId()) for Gelectrode's triggering player and assertNull for its target projection. Existing B1 candidate order, native callback count, external callback count, and hidden-message assertions remain unchanged.

- [ ] Run the new and retained provider tests before production changes:

~~~powershell
mvn -pl forge-gui-desktop -am '-Dtest=BloodOperativeConfirmationDecisionProviderTest,GelectrodeConfirmationDecisionProviderTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
~~~

  Expected RED result: the new exact Blood request assertion fails because the current provider has only B1 admission, while the retained Gelectrode tests continue to show their baseline behavior. Record the exact failure; do not weaken the expected contract to make the old implementation pass.

## Task 3: Write RED tests for lifecycle, ownership, and trace ordering

Files:

- Create forge-gui-desktop/src/test/java/forge/game/decision/BloodConfirmationOwnershipMatrixTest.java.
- Modify forge-gui-desktop/src/test/java/forge/game/decision/DeterminismTraceV2Test.java.

- [ ] Add a counting controller and counting target/confirmation resolvers around the real PlayerControllerAi.orderAndPlaySimultaneousSa route. Keep separate counters for target resolver, native target callback, confirmation resolver, native confirmation callback, temporary B observation, and effect application. The fixture must retain the actual live Card A so the test can assert A-B-A restoration and C==A.

- [ ] Add the four independent ownership cases exactly as a table:

| TARGET resolver | CONFIRMATION resolver | Required observations |
|---|---|---|
| null | null | Forge owns A; native confirmation owns yes/no; one target callback and one native confirmation callback; D1 native CHOSEN true/true. |
| external | null | target resolver once; native confirmation once; temporary B may exist; A restored and consumed; D1 native CHOSEN true/true. |
| null | external | native target once; confirmation resolver once; native confirmation and B zero; D1 external CHOSEN false/false. |
| external | external | target resolver once; confirmation resolver once; both native callbacks zero; B absent; ACCEPT consumes C==A; D1 external CHOSEN false/false. |

  The current implementation must fail the D1 assertions, producing the RED lifecycle gate before modifying WrappedAbility.

- [ ] Add ACCEPT and DECLINE integration assertions. ACCEPT moves the existing A through the existing ChangeZone effect; DECLINE performs no effect. Neither route may create a second TARGET request or retarget.

- [ ] Add adversarial external resolver tests for null, foreign-request, stale-request, wrong-kind, and throwing results. Assert the controlled confirmation exception, bounded status/reason, no native callback, no native fallback, no effect, and no MAPPING_FAILED result. The trace may finalize as TRACE_INCOMPLETE at game trace finish.

- [ ] Add native A-integrity tests where the native controller returns a Boolean after clearing/restoring the wrong target or leaving A absent. Assert one native callback only, no CHOSEN, MAPPING_FAILED, nativeCallbackCompleted=true/mappingAttempted=true, and no retry.

- [ ] Add fizzle and continuation tests. Moving A out of Graveyard before stack resolution must fizzle before D1 confirmation with no resolver call and no replacement target. An active ActionContinuation must never create a D1 request; with external confirmation ownership it must throw before resolver or native confirmation callback.

- [ ] Extend DeterminismTraceV2Test with a confirmation-specific regression: external CONFIRMATION CHOSEN remains valid only for false/false, native mapping remains true/true, and BC policy samples still require valid native true/true. Do not change production validator logic unless this regression demonstrates a real failure; the current validator already accepts external CONFIRMATION and TARGET CHOSEN with false/false.

- [ ] Run the RED lifecycle selectors:

~~~powershell
mvn -pl forge-gui-desktop -am '-Dtest=BloodConfirmationOwnershipMatrixTest,DeterminismTraceV2Test' '-Dsurefire.failIfNoSpecifiedTests=false' test
~~~

## Task 4: Implement explicit profile/context types and the shared common validator

Files:

- Create forge-game/src/main/java/forge/game/decision/BloodOperativeEtbProfile.java.
- Modify forge-game/src/main/java/forge/game/decision/ConfirmationTriggerProfile.java.
- Modify forge-game/src/main/java/forge/game/decision/ConfirmationEventType.java.
- Modify forge-game/src/main/java/forge/game/decision/ConfirmationDecisionContext.java.
- Modify forge-game/src/main/java/forge/game/decision/TriggeredTargetDecisionCoordinator.java.

- [ ] Add the typed Blood confirmation profile and trace label, plus CHANGES_ZONE. Keep the existing Gelectrode enum name and trace label stable so B1 trace and audit consumers do not drift.

- [ ] Change the context constructor and getter from primitive int to nullable Integer for the event player, add nullable targetPublicIdentity, and preserve CardSelectionCard value-only immutability. Update B1 construction to pass the Activator player ID and null target; leave Blood event-player null.

- [ ] Implement the helper's exact normalized maps from the C2A admission:

~~~text
trigger: Mode=ChangesZone, Origin=Any, Destination=Battlefield,
         ValidCard=Card.Self, OptionalDecider=You,
         Execute=TrigChangeZone
static:  DB=ChangeZone, Origin=Graveyard, Destination=Exile,
         ValidTgts=Card
live:    ApiType.ChangeZone, same semantic map, TgtZone=Graveyard,
         TargetMin=1, TargetMax=1
~~~

  Preserve the existing C2A allowance for decorative TriggerDescription, TgtPrompt, and ValidTgtsDesc fields only. Unknown semantic fields remain rejected. Check both original/live trigger maps where C2A currently does so.

- [ ] Move only common checks from TriggeredTargetDecisionCoordinator.admitBlood into the helper. Keep these C2A checks in the coordinator:

  - chooser/decider/live-activating-player/source-controller seat equality;
  - chooser and decider visibility perspectives;
  - TargetChoices initially empty;
  - C2A target-provider generation/application, native temporary target mapping, and C2A trace labels.

  Map helper failure tokens to the existing C2A controlled reason enum without changing the C2A public status contract.

- [ ] Run the existing C2A unit and ownership tests immediately after the helper extraction. Expected result: no C2A behavior change; all existing exact-profile, mismatch, native, external, fizzle, continuation, and trace assertions remain green before D1 provider work starts.

## Task 5: Implement the exact Blood provider admission and request lifecycle

Files:

- Modify forge-game/src/main/java/forge/game/decision/ConfirmationDecisionProvider.java.

- [ ] Generalize the provider's B1-only profile classifier into a fixed two-case classifier. Keep the initial hidden-source and active-continuation gates before request creation. Use the shared helper for Blood common semantics and retain B1's exact existing checks.

- [ ] For Blood, enforce only these ownership relations, using Forge seat equality as C2A does:

~~~text
decider == wrapper.getDecider()
decider == liveAbility.getActivatingPlayer()
decider == source.getController()
~~~

  Do not read or require AbilityKey.Activator for Blood. For Gelectrode keep the existing Activator Player and source-controller check unchanged.

- [ ] Capture D1 Target A only after exact common admission and before request creation. Read the live target list once, require exactly one Card, require the card to be in Graveyard and public/projectable to the decider, and create new CardSelectionCard(targetA). Store no live reference in the context. Zero, multiple, non-Card, hidden, stale-before-request, or unprojectable A returns unsupported generation with no request.

- [ ] Keep every admitted request exactly:

~~~text
DecisionType.CONFIRMATION
forced=false
candidates=[LegalCandidate.confirmation(0, ACCEPT),
            LegalCandidate.confirmation(1, DECLINE)]
targetPublicIdentity=A projection for Blood, null for Gelectrode
triggeringPlayerId=null for Blood, Activator ID for Gelectrode
~~~

  No target candidate, target context, continuation field, sequence ID, or subdecision index is added to the request.

- [ ] Snapshot resolver ownership at generation. choose must use the captured resolver for this active request, invoke it exactly once for external ownership, and never invoke the native Boolean supplier on the external path. Null, foreign, stale, wrong-kind, and throwing external results become INVALID_EXTERNAL_CANDIDATE with a fixed reason token and invalidate the active request; they never select a replacement or fall back to Forge AI.

- [ ] Preserve B1 native Boolean mapping exactly once. For Blood native choice, the supplied confirmTrigger callback may perform its existing internal B evaluation. D1 must not call the callback a second time.

- [ ] Add profile-specific apply validation:

  - B1 keeps its existing source/profile/decider validation and boolean semantics.
  - Blood validates profile/event/source/decider and re-reads the live target list immediately before applying. It must contain exactly one live Card whose identity and public projection match the stored A. It must not rerun target legality and must not choose a replacement.
  - A mismatch throws TARGET_A_INTEGRITY_FAILURE for external ownership and NATIVE_MAPPING_FAILED for native ownership. In either case invalidate the active request so no retry can create a second callback.

- [ ] Add provider-level ownership/result access needed by the wrapper to use the request's captured owner, not a mutable resolver lookup after apply. Keep the API value-only and request-local; do not expose the resolver or live engine objects.

- [ ] Run the direct provider tests. Expected result: Task 2's exact Blood request, context, admission, and candidate tests become green; integration tests that require wrapper lifecycle remain red until Task 6.

## Task 6: Implement wrapper lifecycle, trace ordering, diagnostics, and failure mapping

Files:

- Modify forge-game/src/main/java/forge/game/trigger/WrappedAbility.java.
- Modify forge-game/src/main/java/forge/game/decision/ConfirmationDiagnostics.java.
- Modify forge-game/src/main/java/forge/game/decision/UnsupportedConfirmationDecisionException.java.

- [ ] Replace the hard-coded B1 trace stage with the admitted context profile's fixed trace label. Unsupported generation still creates no D1 request and no trace request.

- [ ] Implement the exact admitted lifecycle in this order:

~~~text
provider.generate
-> ConfirmationDiagnostics.capture
-> DeterminismTrace.recordRequest
-> provider.choose
-> provider.apply + live Target-A integrity validation
-> ConfirmationDiagnostics.recordResult
-> ChangesZone/TriggeredTarget diagnostics result
-> traceHandle.recordNativeMappedResult or recordExternalChosenResult
-> continue/return or existing effect resolution
~~~

  recordNativeMappedResult and recordExternalChosenResult must not appear before provider.apply returns successfully. ACCEPT then continues to the existing no-stack ChangeZone effect, while DECLINE returns before effect execution.

- [ ] On native NATIVE_MAPPING_FAILED, call traceHandle.recordMappingFailed exactly once after the callback has returned, then propagate the sanitized controlled exception. Do not call recordNativeMappedResult, do not record CHOSEN, and do not retry the native callback. On external invalid candidate or A-integrity failure, do not call recordMappingFailed; leave the request non-native for the existing TRACE_INCOMPLETE finalization path.

- [ ] Keep the resolver-null fallback for unsupported/non-exact profiles. When a resolver exists, throw UnsupportedConfirmationDecisionException before any native fallback. For an active continuation, the external branch must throw before resolver or native confirmation callback; the native branch must still create no D1 request and preserve the existing native boundary.

- [ ] Generalize ConfirmationDiagnostics wording from B1-only and add a sanitized typed profile column. Store the profile in Generation/capture metadata when known; use an empty safe value when the profile cannot be identified before a hidden/continuation gate. Keep source/mode/execute fields public-value-only and make diagnostics failure non-functional.

- [ ] Change the exception message to a profile-neutral fixed form such as Confirmation decision unsupported or integrity-failed: STATUS / REASON. Preserve getStatus() and getReason(), restrict reasons to fixed tokens, and assert that hidden names and resolver exception messages never appear.

- [ ] Run the wrapper integration tests and trace validator tests. Expected result: the full native/external Blood matrix, ACCEPT/DECLINE, A-B-A restoration, native mapping failure, no-B external path, sanitized failures, and trace ordering become green; retained B1 tests remain green.

## Task 7: Complete the independent TARGET/CONFIRMATION integration matrix

Files:

- Modify forge-gui-desktop/src/test/java/forge/game/decision/BloodConfirmationOwnershipMatrixTest.java.
- Modify forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetDecisionCoordinatorTest.java only for shared common-profile parity/regression coverage.

- [ ] Exercise all four resolver combinations through the real controller route rather than directly invoking provider methods. Record, per case:

~~~text
target resolver calls
native target callbacks
confirmation resolver calls
native confirmation callbacks
temporary B observations
Target-A identity before/after confirmation
effect-consumed Card identity
DECISION_TRACE_V2 result kind/native/mapping flags
~~~

- [ ] Assert exactly one Target-A decision and no confirmation-time retarget in every supported combination. For TARGET external plus CONFIRMATION native, explicitly allow B internally but require A restoration before terminal trace and C==A. For either external confirmation case, assert B is absent.

- [ ] Assert resolver-null unsupported behavior for a non-exact Blood trigger preserves the native route and emits no D1 request. Assert resolver-non-null unsupported behavior throws before both resolver/native confirmation paths.

- [ ] Assert normal fizzle after A becomes illegal before resolution occurs before confirmation. No D1 resolver, no native confirmation callback, no second TARGET request, and no replacement target are permitted.

- [ ] Assert active continuation rejection in both target and confirmation seams without leaking continuation metadata into a request or diagnostics row.

- [ ] Run the complete focused D1/C2A matrix:

~~~powershell
mvn -pl forge-gui-desktop -am '-Dtest=BloodOperativeConfirmationDecisionProviderTest,BloodConfirmationOwnershipMatrixTest,TriggeredTargetDecisionCoordinatorTest,GelectrodeConfirmationDecisionProviderTest,DeterminismTraceV2Test' '-Dsurefire.failIfNoSpecifiedTests=false' test
~~~

## Task 8: Retained gates, diagnostics safety, and production workload evidence

Files:

- Create docs/AI-ML DOCS/FRL_02K_D1_BLOOD_CONFIRMATION_AUDIT.md.
- Modify docs/AI-ML DOCS/FRL_02K_CONFIRMATION_AUDIT.md.
- Modify docs/AI-ML DOCS/ML_STRATEGY.md only after the production gate passes.

- [ ] Run the focused audit selectors that cover the retained seams:

~~~powershell
mvn -pl forge-gui-desktop -am '-Dtest=FRL02KConfirmationAuditTest,FRL02KTriggeredTargetProviderAuditTest,FRL02KTriggeredTargetOwnershipAuditTest,FRL02KTriggeredTargetExternalOwnershipAuditTest,FRL02KChangesZoneProjectionAuditTest,PriorityActionDiagnosticsTest,DeterminismTraceV2Test,BloodOperativeConfirmationDecisionProviderTest,BloodConfirmationOwnershipMatrixTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
~~~

  If the combined selector exceeds the wrapper limit, split it into unit, provider, and fresh-JVM audit invocations and record every exit/result. Do not convert a timeout into a pass.

- [ ] Run the canonical native workload in a fresh JVM with the established SimulateMatch command shape: Izzet Guild Kit versus Dimir Guild Kit, ten games, seed 20260810, diagnostics enabled and disabled as the existing determinism gate requires. Use the built jar path printed by the package output and keep generated CSV/trace artifacts under target or a temporary directory, never in tracked source. Recompute current source/runtime counts rather than copying historical values; retain historical locks only when the current run proves them.

- [ ] Run the focused external Blood workload only through the real target and confirmation resolver seams. Report request/callback/effect/trace counts, ACCEPT/DECLINE counts, and A/B/C identity checks. Do not infer broad Blood agent completeness from this focused workload.

- [ ] Write the D1 audit with the attached task's required evidence sections, explicitly separating confirmed facts, measured indicators, hypotheses, blockers, baseline timeouts, and unverified external state. Include:

  - checkpoint and branch/base evidence;
  - exact common/C2A/D1 admission predicates;
  - public context and nullable Blood event-player contract;
  - resolver-null versus resolver-non-null fail-closed behavior;
  - TARGET/CONFIRMATION 2x2 matrix;
  - native A-B-A and external A-preservation lifecycle;
  - terminal trace ordering and result flags;
  - diagnostics profile field and raw-object safety;
  - focused tests, fresh-JVM workload commands, exact counts, and limitations;
  - final FRL_02K_D1_PASS, FRL_02K_D1_PARTIAL, or FRL_02K_D1_FAIL verdict, followed by the required final STOP boundary.

- [ ] Update the master confirmation audit to record B1 plus D1 as the two explicit production confirmation profiles, preserve C2A TARGET as a separate seam, and state that Blood Surveil PAYMENT and all other listed profiles are unchanged. Update ML_STRATEGY.md only with the measured narrow D1 status; do not claim generic CONFIRMATION coverage or agent completion.

## Task 9: Full verification and scope audit

Files: no new source files; use the completed implementation and docs.

- [ ] Run the requested broad reactor test and record the exact result:

~~~powershell
mvn -pl forge-gui-desktop -am test
~~~

  If the command exceeds the wrapper limit, run module/package suites in bounded splits and retain the unsplit command as UNVERIFIED only when it cannot produce a definitive result. Report pre-existing failures separately from D1 failures.

- [ ] Run package and validation gates:

~~~powershell
mvn -pl forge-gui-desktop -am -DskipTests package
mvn -pl forge-gui-desktop -am validate
~~~

  Record checkstyle output from the configured Maven lifecycle/plugin. If no checkstyle goal is configured, record that fact instead of claiming a checkstyle pass.

- [ ] Run repository hygiene and inspect the complete diff:

~~~powershell
git -C C:\forgeAI-blood-confirmation-d1 diff --check
git -C C:\forgeAI-blood-confirmation-d1 status --short --branch
git -C C:\forgeAI-blood-confirmation-d1 diff --stat origin/master...HEAD
~~~

  Expected git diff --check: no output and exit zero. Confirm that only the mapped production/test/docs files changed. Search the D1 diff for forbidden scope terms and manually inspect every hit so that test/report references do not hide production edits to card scripts, PAYMENT, ORDER, or unrelated profiles.

- [ ] Perform two independent reviews before publication. Each review must classify P0/P1/P2 findings and verify at minimum: ownership matrix, nullable Blood event-player semantics, common-helper scope, A integrity before trace, native mapping failure flags, external no-fallback behavior, continuation gate, typed diagnostics profile, C2A/B1 regressions, and exact task scope. Resolve every P0/P1; document accepted P2s in the audit.

## Task 10: Commit, push, and Draft PR handoff

Files: no additional implementation files.

- [ ] Re-run the focused D1 selectors after all documentation changes, then rerun git diff --check. Confirm the worktree contains no generated audit artifacts or temporary traces.

- [ ] Commit the verified implementation and documents with:

~~~powershell
git -C C:\forgeAI-blood-confirmation-d1 add forge-game/src/main/java/forge/game/decision forge-game/src/main/java/forge/game/trigger/WrappedAbility.java forge-gui-desktop/src/test/java/forge/game/decision "docs/AI-ML DOCS/FRL_02K_D1_BLOOD_CONFIRMATION_AUDIT.md" "docs/AI-ML DOCS/FRL_02K_CONFIRMATION_AUDIT.md" "docs/AI-ML DOCS/ML_STRATEGY.md"
git -C C:\forgeAI-blood-confirmation-d1 commit -m "FRL-02K-D1: add Blood ETB confirmation slice"
~~~

  Do not stage unrelated generated files. The final commit must contain the exact D1 slice, its tests, and evidence docs only.

- [ ] Push the branch and verify the remote branch points to the committed SHA:

~~~powershell
git -C C:\forgeAI-blood-confirmation-d1 push -u origin frl/02k-d1-blood-etb-confirmation
git -C C:\forgeAI-blood-confirmation-d1 rev-parse HEAD
git -C C:\forgeAI-blood-confirmation-d1 ls-remote --heads origin frl/02k-d1-blood-etb-confirmation
~~~

- [ ] Open the requested Draft PR with title:

~~~text
FRL-02K-D1: add Blood ETB confirmation slice
~~~

  Include the exact final verdict, commit SHA, focused/broad/package/validate results, baseline timeout classification, review findings, scope limits, and explicit STOP. Do not merge, mark ready, begin ORDER, or start a follow-up profile from this task.

## Final completion contract

The implementation is complete only when all of the following are evidenced:

- D1 exact Blood profile admits only public, non-copied, semantically exact ChangesZone -> Battlefield with one authoritative Graveyard Card A.
- B1 remains behaviorally unchanged and Gelectrode remains target-free.
- C2A retains empty initial targets and continues to own Target A.
- resolver-null preserves native Forge fallback for unsupported/non-exact profiles; resolver-non-null fails closed without native fallback.
- native D1 performs one request and one confirmTrigger, permits internal B, restores/verifies A, and records only post-apply CHOSEN true/true or native MAPPING_FAILED true/true.
- external D1 performs one resolver decision, zero native confirmation/B evaluation, revalidates A, and records CHOSEN false/false only after apply.
- ACCEPT consumes C==A, DECLINE consumes no effect, and fizzle never retargets.
- active continuations never create a D1 request and external ownership rejects before resolver/native callback.
- diagnostics expose only the typed profile and safe value projections.
- the final audit ends with exactly one of FRL_02K_D1_PASS, FRL_02K_D1_PARTIAL, or FRL_02K_D1_FAIL, then STOP.
