# FRL-02K-C2A Triggered TARGET Provider Seam Implementation Plan

> For agentic workers: REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

Goal: add a production, controller-local TARGET ownership seam for the exact
Blood Operative ETB exile-from-graveyard card-target profile, with native Forge
compatibility when no resolver is configured and fail-closed external ownership
when a resolver is configured.

Architecture: PlayerController owns one non-static TargetDecisionProvider and
one nullable non-static TargetDecisionProvider.Resolver. A stateless
TriggeredTargetDecisionCoordinator performs profile admission, applicability
classification, native request-local teacher mapping, external 0/1/many
orchestration, and integrity enforcement. TargetDecisionProvider remains the
generic Forge legality/candidate/application oracle; PlayerControllerAi only
routes the existing callback and stack flow.

Tech Stack: Java 17, Maven reactor, TestNG, Forge game/AI modules, existing
DECISION_TRACE_V2 and DeterminismTrace APIs, PowerShell on Windows.

---

## Scope guard and file map

The implementation starts from branch
frl/02k-c2a-triggered-target-provider-seam at correction commit
19ea8bd8a8884aece27e029820a03ecebe2c41be, whose parent is the implementation
plan commit 5f221714f972f9a94ba0a46e079d8981b68b3acb and whose grandparent is
the approved design specification commit 24006a3d67bcefb8197daba28ce7fd8ee942e1f9,
in worktree C:\forgeAI-triggered-target-c2a.

Files to create:

- forge-game/src/main/java/forge/game/decision/TriggeredTargetIntegrityException.java
  - sanitized reason enum and fail-closed exception without host/card/target
    names.
- forge-game/src/main/java/forge/game/decision/TriggeredTargetDecisionCoordinator.java
  - stateless classification, exact Blood admission, Generation orchestration,
    native immutable-snapshot mapping, resolver integrity, and trace provenance.
- forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetDecisionCoordinatorTest.java
  - focused exact-profile, native/external, adversarial, isolation, and stack
    preparation tests.
- forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetContinuationChildMain.java
  - fresh-JVM continuation-gate fixture.
- forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetContinuationProcessTest.java
  - parent test that launches the continuation child with diagnostics enabled.
- forge-gui-desktop/src/test/java/forge/view/FRL02KTriggeredTargetExternalOwnershipAuditTest.java
  - focused external ownership workload and separate native control run.
- docs/AI-ML DOCS/FRL_02K_C2A_TRIGGERED_TARGET_AUDIT.md
  - measured C2A evidence and final narrow milestone status.

Files to modify:

- forge-game/src/main/java/forge/game/decision/TargetDecisionProvider.java
  - add only the generic Resolver functional interface; keep provider state and
    Forge legality/application unchanged.
- forge-game/src/main/java/forge/game/player/PlayerController.java
  - add controller-local provider/resolver fields and accessors.
- forge-game/src/main/java/forge/game/decision/DecisionTraceTrainingValidator.java
  - permit external CHOSEN for TARGET or existing CONFIRMATION only.
- forge-ai/src/main/java/forge/ai/PlayerControllerAi.java
  - thin coordinator routing, one native callback adapter for testability, and
    no changes to native confirmation Target B logic.
- forge-gui-desktop/src/test/java/forge/game/decision/TargetDecisionProviderTest.java
  - add provider-local request-ID isolation coverage.
- forge-gui-desktop/src/test/java/forge/game/decision/DeterminismTraceV2Test.java
  - add external TARGET CHOSEN validator coverage and retain BC exclusion.

Files explicitly out of scope:

- forge-gui/src/main/java/forge/... and non-AI controllers;
- forge-gui-desktop/src/test/java/forge/gamesimulationtests/util/PlayerControllerForTests.java;
- forge-gui-desktop/src/test/java/forge/view/FRL02KTriggeredTargetOwnershipAuditTest.java,
  except for running it as a regression;
- forge-gui/src/main/resources/forge/cardsfolder/b/blood_operative.txt;
- Lazav, CONFIRMATION, PAYMENT, ORDER, DAMAGE_ASSIGNMENT, observation/history,
  Encode, or any generic triggered-target profile beyond the admitted Blood
  profile.

The existing static diagnostic-only provider in PriorityActionDiagnostics is
not reused by C2A. The C2A provider used by controllers is the new
controller-local instance; no new static provider or resolver is introduced.

## Public and package API contract

Add the generic resolver contract to TargetDecisionProvider without storing a
resolver in that class:

    @FunctionalInterface
    public interface Resolver {
        LegalCandidate resolve(DecisionRequest request);
    }

Add these controller-local members to PlayerController:

    private final TargetDecisionProvider targetDecisionProvider =
            new TargetDecisionProvider();
    private TargetDecisionProvider.Resolver targetDecisionResolver;

    public final TargetDecisionProvider getTargetDecisionProvider()
    public final TargetDecisionProvider.Resolver getTargetDecisionResolver()
    public final void setTargetDecisionResolver(
            TargetDecisionProvider.Resolver resolver)

The provider field is final, non-static, and constructed once per controller.
The resolver field is nullable, non-static, and never copied into a static
diagnostic object.

The coordinator exposes the following routing contract:

    public enum Classification {
        NOT_APPLICABLE,
        ADMITTED,
        UNSUPPORTED_TARGETED_TRIGGER
    }

    public enum PreparationStatus {
        NATIVE,
        NATIVE_WITH_TEACHER_CAPTURE,
        PREPARED,
        NO_STACK
    }

    public Classification classify(SpellAbility queuedAbility)

    public Preparation prepare(
            SpellAbility queuedAbility,
            TargetDecisionProvider provider,
            TargetDecisionProvider.Resolver resolver)

    public boolean completeNative(Preparation preparation,
            boolean nativeResult)

    public void enforceExternalTargetBoundary(
            SpellAbility queuedAbility,
            TargetDecisionProvider.Resolver resolver)

Preparation exposes only status and boolean routing facts needed by
PlayerControllerAi. Its live SpellAbility, immutable target snapshot,
DecisionRequest, and trace handle remain private implementation state. No
public Preparation getter returns SpellAbility, WrappedAbility, Game, Card,
GameObject, TargetChoices, or ActionContinuation.

Generation handling is always API-exact:

    TargetDecisionProvider.Generation generation =
            provider.generateTargetRequest(underlying, chooser, null);

    switch (generation.getStatus()) {
    case INVALID_TARGETING:
        ...
    case DECISION:
        DecisionRequest request = generation.getRequest();
        ...
    case COMPLETE:
        ...
    }

The coordinator never calls provider.generateTargetRequest a second time for
native mapping and never generates a second externally visible policy request
after resolver selection. TargetDecisionProvider.apply may perform its existing
internal completion generation.

## Task 1: Reconfirm the implementation checkpoint

Files: none.

- [ ] Verify the requested primary checkpoint and isolated worktree before
      touching implementation files.

    $primaryStatus = @(git -C C:\forgeAI status --short)
    if ($primaryStatus.Count -ne 0) { throw 'Primary C:\forgeAI worktree is not clean' }
    git -C C:\forgeAI rev-parse HEAD
    git -C C:\forgeAI rev-parse origin/master
    git -C C:\forgeAI merge-base HEAD origin/master

    $status = @(git -C C:\forgeAI-triggered-target-c2a status --short)
    if ($status.Count -ne 0) { throw 'C2A worktree is not clean' }
    git -C C:\forgeAI-triggered-target-c2a branch --show-current
    git -C C:\forgeAI-triggered-target-c2a rev-parse HEAD
    git -C C:\forgeAI-triggered-target-c2a rev-parse HEAD^
    git -C C:\forgeAI-triggered-target-c2a rev-parse HEAD^^
    git -C C:\forgeAI-triggered-target-c2a rev-parse origin/master
    git -C C:\forgeAI-triggered-target-c2a merge-base HEAD origin/master

  Expected primary output: HEAD, origin/master, and merge-base are
  3851fdf3825e394af82717508e34177f903c864d; primary status is empty.
  Expected C2A output: status is empty; the branch is
  frl/02k-c2a-triggered-target-provider-seam; HEAD is
  19ea8bd8a8884aece27e029820a03ecebe2c41be; HEAD^ is
  5f221714f972f9a94ba0a46e079d8981b68b3acb; HEAD^^ is
  24006a3d67bcefb8197daba28ce7fd8ee942e1f9; and origin/master and
  merge-base are
  3851fdf3825e394af82717508e34177f903c864d.

- [ ] Confirm the approved design commit and plan file are present without any
      production changes.

    git -C C:\forgeAI-triggered-target-c2a show --stat --oneline 24006a3d67b
    git -C C:\forgeAI-triggered-target-c2a diff --name-only 24006a3d67b..HEAD
    git -C C:\forgeAI-triggered-target-c2a diff --check origin/master...HEAD

  Expected output: the design commit is present; the second command prints
  exactly one path,
  docs/superpowers/plans/2026-08-11-frl-02k-c2a-triggered-target-provider-seam.md;
  and the ranged diff check prints no output and exits zero before
  implementation starts.

## Task 2: Write RED tests for the public seams and trace validator

Files:

- Create: forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetDecisionCoordinatorTest.java
- Modify: forge-gui-desktop/src/test/java/forge/game/decision/DeterminismTraceV2Test.java
- Modify: forge-gui-desktop/src/test/java/forge/game/decision/TargetDecisionProviderTest.java

- [ ] Add a first coordinator test that compiles against the planned API and
      asserts the exact resolver-null/external distinction.

  Use the existing AITest fixture style, create a Blood Operative on the
  battlefield, create two opposing cards in the graveyard, obtain the native
  ChangesZone trigger, and build the live WrappedAbility from the trigger's
  ensured ability. The first test must call:

    final TriggeredTargetDecisionCoordinator.Preparation nativePreparation =
            coordinator.prepare(wrapper, provider, null);
    assertEquals(nativePreparation.getStatus(),
            TriggeredTargetDecisionCoordinator.PreparationStatus
                    .NATIVE_WITH_TEACHER_CAPTURE);

    final TriggeredTargetDecisionCoordinator.Preparation externalPreparation =
            coordinator.prepare(wrapper, provider,
                    request -> request.getCandidates().get(1));
    assertEquals(externalPreparation.getStatus(),
            TriggeredTargetDecisionCoordinator.PreparationStatus.PREPARED);

  The external preparation must leave exactly one target on the underlying
  ability and must not invoke a resolver for the one-candidate fixture. Use a
  two-candidate fixture for the resolver-count assertion.

- [ ] Add an external TARGET trace test to DeterminismTraceV2Test using the
      existing package-local DecisionRequest construction helpers.

  Record a TARGET request, call recordExternalChosenResult with a legal
  candidate, assert isHistoryValid returns true, and assert
  isBCPolicySample returns false. Also create an external TARGET result with
  nativeCallbackCompleted=true or mappingAttempted=true and assert it remains
  invalid. Do not change the existing CONFIRMATION assertions.

- [ ] Add a provider-local request-ID test to TargetDecisionProviderTest with
      two fresh providers and an ability fixture.

  The test must assert that each fresh provider's first request ID is zero,
  that the first provider's second request is one, and that equal IDs across
  providers are accepted. It must not assert global uniqueness.

- [ ] Run the focused RED command before adding production classes.

    mvn -pl forge-gui-desktop -am '-Dtest=TriggeredTargetDecisionCoordinatorTest,DeterminismTraceV2Test,TargetDecisionProviderTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

  Expected result: compilation fails because the coordinator API and
  controller/provider additions do not yet exist; the validator-only test
  also identifies the current TARGET external-CHOSEN rejection. Preserve the
  failure output in the implementation notes; do not weaken the assertions.

- [ ] Commit only the RED tests and test changes.

    git add forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetDecisionCoordinatorTest.java forge-gui-desktop/src/test/java/forge/game/decision/DeterminismTraceV2Test.java forge-gui-desktop/src/test/java/forge/game/decision/TargetDecisionProviderTest.java
    git diff --cached --check
    git commit -m "test: define C2A triggered target seam contract"

## Task 3: Expand RED coverage for exact admission and fail-closed classification

File:

- Modify: forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetDecisionCoordinatorTest.java

- [ ] Add exact-profile admission tests with the actual Blood script.

  Use separate tests named:

    exactBloodEtbProfileIsAdmitted
    triggerDescriptionAndTargetPromptDoNotAffectAdmission
    liveChangeZoneMismatchRejectsStaticHit
    runtimeRewriteRejectsStaticDefinition
    underlyingOptionalParamRejectsDuplicatedOptionality
    nonEmptyInitialTargetsFailBeforeGeneration
    chooserMustMatchDeciderActivatorAndSourceController
    copiedWrapperAndClonedSourceAreSeparateProvenanceFailures

  Assert ADMITTED only when the source is Original, public to the chooser, not
  cloned, the wrapper is intrinsic and not copied, the trigger is intrinsic,
  normal, non-static, non-generated, not a SpawningAbility, and all normalized
  static/live parameters match. Assert that the wrapper decider,
  underlying activating player, and source controller are the same Forge seat.

  Mutate only the test fixture's runtime objects or copied trigger data. Do not
  edit forge-gui/src/main/resources/forge/cardsfolder/b/blood_operative.txt.

- [ ] Add classification tests for resolver-on and resolver-off ownership.

  Use these exact test names:

    nonTargetedTriggerIsNotApplicableAndNativeWithResolver
    unsupportedTargetedProfileFailsClosedOnlyWithResolver
    copiedGeneratedSpawningAndTargetingPlayerCasesNeverFallbackExternally
    unsupportedTargetedProfileRemainsNativeWithoutResolver
    nonWrappedTargetedTriggerFailsClosedWhenExternalOwnershipIsActive

  For resolver-on unsupported targeted cases assert
  TriggeredTargetIntegrityException.Reason.UNSUPPORTED_TARGETED_TRIGGER, zero
  resolver calls, zero native callback calls, and no stack insertion. For
  resolver-null cases assert the native route status and no exception.

- [ ] Add the continuation gate test contract to the parent test, with the
      actual process test implemented in Task 8.

  The test name is:

    activeContinuationFailsBeforeProviderResolverOrNativeCallback

  It must assert the child result reason is
  UNSUPPORTED_ACTION_CONTINUATION and the child counters are provider=0,
  resolver=0, native=0.

- [ ] Run the coordinator test class to record the expanded RED state.

    mvn -pl forge-gui-desktop -am '-Dtest=TriggeredTargetDecisionCoordinatorTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

  Expected result: the test compiles only after the public exception and
  coordinator types are introduced; until then it fails at compilation.

- [ ] Commit the additional RED admission tests.

    git add forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetDecisionCoordinatorTest.java
    git diff --cached --check
    git commit -m "test: cover C2A Blood admission and ownership gates"

## Task 4: Implement the generic resolver contract, controller ownership, and exception

Files:

- Modify: forge-game/src/main/java/forge/game/decision/TargetDecisionProvider.java
- Modify: forge-game/src/main/java/forge/game/player/PlayerController.java
- Create: forge-game/src/main/java/forge/game/decision/TriggeredTargetIntegrityException.java

- [ ] Add only the generic nested Resolver interface to TargetDecisionProvider.

  Add the functional interface shown in the API contract. Do not add a resolver
  field, setter, resolver call, Blood check, or external ownership branch to
  TargetDecisionProvider. Keep nextRequestId private, non-static, and
  incremented only where DecisionRequest is created.

- [ ] Add controller-local TargetDecisionProvider and nullable resolver state.

  Construct the provider as a final instance field in PlayerController,
  initialize the resolver to null, and expose only the three methods in the
  API contract. Do not add static accessors or a provider singleton. Existing
  ConfirmationDecisionProvider construction and behavior remain unchanged.

- [ ] Create TriggeredTargetIntegrityException with sanitized reasons.

  Define this public enum inside the exception:

    UNSUPPORTED_TARGETED_TRIGGER,
    UNSUPPORTED_PROFILE,
    LIVE_EFFECT_MISMATCH,
    NON_EMPTY_INITIAL_TARGETS,
    INVALID_EXTERNAL_CANDIDATE,
    TARGET_APPLICATION_INCOMPLETE,
    MAPPING_FAILED,
    UNSUPPORTED_ACTION_CONTINUATION

  Store the Reason in a private final field, expose getReason(), and construct
  the superclass message from reason.name() only. Do not accept a SpellAbility,
  Card, GameObject, candidate serialization, or exception cause in the public
  constructor. Do not use UnsupportedTargetDecisionException at the
  coordinator boundary.

- [ ] Run provider/API tests after this minimal implementation.

    mvn -pl forge-gui-desktop -am '-Dtest=TargetDecisionProviderTest,DecisionPublicApiReflectionTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

  Expected result: provider-local ID and public API tests pass; coordinator
  tests still fail because the coordinator has not been implemented.

- [ ] Commit the generic ownership/API layer.

    git add forge-game/src/main/java/forge/game/decision/TargetDecisionProvider.java forge-game/src/main/java/forge/game/player/PlayerController.java forge-game/src/main/java/forge/game/decision/TriggeredTargetIntegrityException.java
    git diff --cached --check
    git commit -m "feat: add controller-local target decision ownership"

## Task 5: Implement coordinator classification and exact Blood admission

File:

- Create: forge-game/src/main/java/forge/game/decision/TriggeredTargetDecisionCoordinator.java

- [ ] Add the stateless enums, Preparation status, and private request-local
      state holder.

  Preparation must retain private references to the exact live underlying
  SpellAbility, DecisionRequest, immutable before-target snapshot, and
  DeterminismTrace.RequestHandle. Public methods expose only status and
  native-teacher completion eligibility. Use List.copyOf for the target
  snapshot and compare target objects by identity during mapping; never retain
  or return a live TargetChoices object.

- [ ] Implement NOT_APPLICABLE and unsupported targeted classification.

  Return NOT_APPLICABLE for null/no-trigger/non-targeted queued abilities. Treat
  any triggered ability that uses targeting as an agent-required targeted family
  when external ownership is active, including copied, generated, non-intrinsic,
  static, SpawningAbility, TargetingPlayer, chooser-mismatch, and unknown
  profiles. For a non-WrappedAbility targeted trigger with resolver null,
  return the unchanged NATIVE route.

  Do not classify a copied or generated targeted trigger as NOT_APPLICABLE.
  Implement enforceExternalTargetBoundary so PlayerControllerAi can guard
  copied-trigger branches before their existing native setup path.

- [ ] Implement source/provenance admission using Forge semantics.

  Require the source card name Blood Operative, Original state, public source
  visibility to wrapper.getDecider(), and !source.isCloned(). Require a trigger
  with mode ChangesZone, !isStatic(), isIntrinsic(), no spawning ability, and
  the wrapper/underlying intrinsic and copied flags required by the spec.
  Reject copied wrapper provenance separately from cloned source provenance.
  Compare chooser/activating/controller with Player.equals or the repository's
  same-seat semantics, never JVM identity.

- [ ] Implement normalized static trigger checks.

  Build a semantic projection from trigger.getOriginalMapParams(), remove only
  TriggerDescription and other explicitly nonsemantic runtime decoration, and
  require exactly:

    Mode=ChangesZone
    Origin=Any
    Destination=Battlefield
    ValidCard=Card.Self
    OptionalDecider=You
    Execute=TrigChangeZone

  Reject unknown semantic keys. Read the static TrigChangeZone SVar through
  AbilityFactory.getMapParams, remove TgtPrompt from the semantic projection,
  and require DB=ChangeZone, Origin=Graveyard, Destination=Exile,
  ValidTgts=Card. Do not compare raw parameter maps.

- [ ] Implement independent live SpellAbility validation.

  Validate wrapper.getWrappedAbility() itself, with API ChangeZone, target
  zone Graveyard, min/max one, no random targeting, no TargetingPlayer, no
  subability, no additional ability/list, a free cost, and no underlying
  Optional parameter. Require usesTargeting and an empty initial target list.
  If the static definition matches but the live effect does not, return the
  specific live mismatch admission reason and never continue to generation.

  The coordinator must not call canTarget, getAllCandidates,
  CardUtil.getValidCardsToTarget, legalTargetPrototypes, or any other provider
  legality helper. This task is profile admission only.

- [ ] Add the action-continuation precondition after exact Blood admission and
      before any provider generation.

  If PriorityActionDiagnostics.hasActiveActionContinuation() is true, throw
  TriggeredTargetIntegrityException with
  UNSUPPORTED_ACTION_CONTINUATION. This check applies to the admitted Blood
  profile even when resolver is null. The provider must receive null
  continuation on the supported path.

- [ ] Run the admission tests.

    mvn -pl forge-gui-desktop -am '-Dtest=TriggeredTargetDecisionCoordinatorTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

  Expected result: exact admission, resolver-null fallback, unsupported
  targeted hard-fail, copied/generated classification, and initial-target
  tests pass; orchestration tests remain red until Task 6.

- [ ] Commit the coordinator admission layer.

    git add forge-game/src/main/java/forge/game/decision/TriggeredTargetDecisionCoordinator.java
    git diff --cached --check
    git commit -m "feat: admit exact Blood triggered target profile"

## Task 6: Implement Generation orchestration, external apply, and native mapping

File:

- Modify: forge-game/src/main/java/forge/game/decision/TriggeredTargetDecisionCoordinator.java

- [ ] Implement native resolver-null preparation with exact Generation handling.

  For an admitted Blood wrapper and resolver null, call exactly once:

    TargetDecisionProvider.Generation generation =
            provider.generateTargetRequest(underlying, chooser, null);

  On INVALID_TARGETING return NATIVE without a request or trace handle. On
  DECISION, capture generation.getRequest(), create List.copyOf of the live
  target list before the callback, record a V2 request using acting seat
  chooser.getId(), stage TRIGGERED_TARGET, and step index zero, and return
  NATIVE_WITH_TEACHER_CAPTURE. An unexpected COMPLETE result while the
  admitted initial target list is empty is an integrity failure, not a native
  fallback.

- [ ] Implement external INVALID_TARGETING and forced branches.

  With a non-null resolver, INVALID_TARGETING returns NO_STACK without resolver
  invocation, target mutation, trace request creation, or stack preparation.
  A COMPLETE Generation on the admitted initial empty-target Blood ability is
  an integrity failure with TARGET_APPLICATION_INCOMPLETE; it must not invoke
  the resolver, push the stack, or become a native fallback.
  For a DECISION request with request.isForced() true, do not invoke the
  resolver. Apply request.getCandidates().get(0) exactly once through
  provider.apply(request, candidate), require returned Generation status
  COMPLETE, assert the underlying TargetChoices has exactly one target matching
  the candidate, call traceHandle.recordEngineForced(), and return PREPARED.

- [ ] Implement external strategic resolver validation and apply.

  For a non-forced DECISION request, invoke Resolver.resolve(request) exactly
  once. Reject null, a candidate not contained in request.getCandidates(), a
  candidate with the wrong target kind, and a candidate from another request
  with INVALID_EXTERNAL_CANDIDATE. Do not select a replacement and do not
  invoke Forge AI.

  Call provider.apply(request, selected) exactly once. Catch provider
  application rejection without copying its message and convert it to
  INVALID_EXTERNAL_CANDIDATE. If the returned Generation status is not
  COMPLETE, throw TARGET_APPLICATION_INCOMPLETE. Do not call
  recordMappingFailed for any external error. On success, assert exactly one
  live target, call recordExternalChosenResult(selected), and return PREPARED.

  The coordinator must not duplicate live canTarget, MustTarget, or candidate
  enumeration. TargetDecisionProvider.apply remains the sole authoritative
  live Forge legality and TargetChoices mutation boundary.

- [ ] Implement completeNative with immutable identity mapping.

  Require a NATIVE_WITH_TEACHER_CAPTURE preparation. If nativeResult is false,
  call traceHandle.recordMappingFailed() and throw the sanitized integrity
  exception with MAPPING_FAILED. If nativeResult is true, copy the live target
  list after the callback, remove only objects found by identity in the
  immutable before-snapshot, and require exactly one new object.

  Find candidates whose package-local LegalCandidate.getTarget() is the same
  object by identity. Require exactly one matching candidate. On success call
  recordNativeMappedResult(selected), return true, and never regenerate the
  request. On zero, multiple, foreign, missing, or non-unique mapping call
  recordMappingFailed() and throw MAPPING_FAILED.

- [ ] Run the complete coordinator unit suite.

    mvn -pl forge-gui-desktop -am '-Dtest=TriggeredTargetDecisionCoordinatorTest,DeterminismTraceV2Test' '-Dsurefire.failIfNoSpecifiedTests=false' test

  Expected result: external 0/1/many, forced, COMPLETE requirement, invalid
  resolver, native single-callback mapping, and trace tests pass.

- [ ] Commit the orchestration layer.

    git add forge-game/src/main/java/forge/game/decision/TriggeredTargetDecisionCoordinator.java
    git diff --cached --check
    git commit -m "feat: orchestrate native and external triggered targets"

## Task 7: Route PlayerControllerAi without moving decision semantics into AI

File:

- Modify: forge-ai/src/main/java/forge/ai/PlayerControllerAi.java

- [ ] Add one stateless coordinator field or one constructed coordinator
      instance with no resolver/provider state.

  The field may be an ordinary final instance because the coordinator carries
  no mutable request state. Do not store a resolver or TargetDecisionProvider
  in PlayerControllerAi.

- [ ] Add a protected native callback adapter used only by the stack-time
      target route.

    protected boolean invokeNativeTriggeredTarget(
            final SpellAbility underlying, final boolean mandatory) {
        return brains.doTrigger(underlying, mandatory);
    }

  Keep confirmTrigger's temporary Target B call directly native and unchanged;
  do not route confirmation through C2A.

- [ ] Route wrapped triggered abilities through the coordinator before the
      existing TargetingPlayer branch.

  For a WrappedAbility trigger, call coordinator.prepare with
  getTargetDecisionProvider() and getTargetDecisionResolver(). Switch only on
  PreparationStatus:

    NO_STACK       -> return false
    PREPARED       -> return true
    NATIVE         -> call invokeNativeTriggeredTarget once and return its result
    NATIVE_WITH_TEACHER_CAPTURE
                     -> call invokeNativeTriggeredTarget once on
                        wrapper.getWrappedAbility(), call completeNative once,
                        then return true

  Record the existing TriggeredTargetAuditDiagnostics target-preparation event
  around the native callback with the new coordinator selector path. Do not
  add Blood checks, candidate-count checks, resolver calls, or target mapping
  code to PlayerControllerAi.

- [ ] Guard copied targeted triggers in orderAndPlaySimultaneousSa before the
      existing copied branch can call setupTargets.

  Call coordinator.enforceExternalTargetBoundary for every queued trigger when
  a resolver is configured. Non-targeted copied triggers remain on their
  existing path. Copied/generated/TargetingPlayer targeted triggers throw
  before any native setup or stack insertion. With resolver null, preserve the
  existing copied-trigger behavior exactly.

- [ ] Keep the existing MagicStack/ComputerUtil stack and no-stack calls.

  The coordinator returns only preparation status; PlayerControllerAi retains
  the existing ComputerUtil.playStack and ComputerUtil.playNoStack calls. Do
  not add retargeting after stack insertion and do not modify confirmTrigger.

- [ ] Add a counting PlayerControllerAi test subclass in
      TriggeredTargetDecisionCoordinatorTest.

  Override invokeNativeTriggeredTarget to increment an AtomicInteger and
  delegate to a supplied native result. Install the subclass as the Forge
  player controller, configure getTargetDecisionResolver, and assert external
  multi-target preparation leaves native count at zero while the resolver count
  is one. In the native control case assert native count is one and resolver
  count is zero.

- [ ] Run the focused routing and existing triggered-target tests.

    mvn -pl forge-gui-desktop -am '-Dtest=TriggeredTargetDecisionCoordinatorTest,FRL02KTriggeredTargetProviderAuditTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

  Expected result: external routing calls the resolver exactly once, invokes
  the native adapter zero times for A, and retains native zero-target behavior.

- [ ] Commit the AI routing layer.

    git add forge-ai/src/main/java/forge/ai/PlayerControllerAi.java forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetDecisionCoordinatorTest.java
    git diff --cached --check
    git commit -m "feat: route triggered targets through C2A coordinator"

## Task 8: Implement the narrow V2 validator change and continuation child test

Files:

- Modify: forge-game/src/main/java/forge/game/decision/DecisionTraceTrainingValidator.java
- Create: forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetContinuationChildMain.java
- Create: forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetContinuationProcessTest.java

- [ ] Change only the externalChosen decision-type condition.

  Replace the current CONFIRMATION-only condition with the explicit expression:

    final boolean externalChosen =
            (request.getDecisionType() == DecisionType.CONFIRMATION
                    || request.getDecisionType() == DecisionType.TARGET)
            && !result.isNativeCallbackCompleted()
            && !result.isMappingAttempted();

  Leave forced, MAPPING_FAILED, TRACE_INCOMPLETE, and isBCPolicySample
  validation unchanged.

- [ ] Implement the child JVM fixture before loading PriorityActionDiagnostics.

  The child main must set forge.priority.metricsFile to a temporary CSV path
  before first referencing PriorityActionDiagnostics, initialize the AITest
  model, create the same Blood wrapper fixture, open a real single-action
  continuation through PriorityActionDiagnostics.beginAction, and invoke the
  coordinator with resolver null. It must print only controlled fields:

    reason=UNSUPPORTED_ACTION_CONTINUATION
    provider_requests=0
    resolver_calls=0
    native_calls=0

  The child must call PriorityActionDiagnostics.endAction in a finally block.
  It must exit nonzero if the coordinator does not fail before generation or
  if any counter changes.

- [ ] Implement the parent process test using the repository's child-JVM
      classpath convention.

  Use System.getProperty("java.class.path"), ChildJvmSupport.javaExecutable(),
  ProcessBuilder, a temporary directory, a 120-second timeout, and assert exit
  code zero plus the four exact output lines. Do not attempt to toggle the
  static diagnostic property inside an already initialized test JVM.

- [ ] Run the validator and fresh-JVM tests.

    mvn -pl forge-gui-desktop -am '-Dtest=DeterminismTraceV2Test,TriggeredTargetContinuationProcessTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

  Expected result: external TARGET CHOSEN validates, BC remains false, and the
  continuation child proves provider/resolver/native counts are all zero.

- [ ] Commit the trace and continuation changes.

    git add forge-game/src/main/java/forge/game/decision/DecisionTraceTrainingValidator.java forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetContinuationChildMain.java forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetContinuationProcessTest.java
    git diff --cached --check
    git commit -m "feat: record external TARGET provenance in V2"

## Task 9: Add the focused external ownership workload and ownership-difference proof

File:

- Create: forge-gui-desktop/src/test/java/forge/view/FRL02KTriggeredTargetExternalOwnershipAuditTest.java

- [ ] Build a deterministic Blood fixture with two legal graveyard cards and
      one controller-local resolver.

  The resolver selects the candidate with the highest request-local semantic
  ordering key in the external run and increments an AtomicInteger. The test
  must assert the resolver receives one exact TARGET request, sees no
  continuation metadata, and returns an object from that request's candidate
  list.

- [ ] Run a native control fixture separately with the same game seed and
      initial state.

  Install a counting controller whose native adapter delegates to Forge AI,
  capture the one target selected by native Forge as A_native, and assert one
  native callback. Do not reuse the same live Game instance for the external
  run; clone the fixture setup by construction so each run has independent
  provider and resolver state.

- [ ] Run the external fixture without invoking Forge AI for Target A.

  Assert resolver count one, native callback count zero, one applied target,
  one existing stack preparation, and external trace flags
  nativeCallbackCompleted=false and mappingAttempted=false. Where the fixture
  provides two distinct candidates, require A_external != A_native. The
  external run must never call Forge AI merely to discover its preference.

- [ ] Add adversarial resolver cases to the workload class.

  Return null, a candidate from another request, a stale candidate after a
  zone change, a foreign candidate, and a candidate made illegal by live Forge
  state. Assert INVALID_EXTERNAL_CANDIDATE, no stack push, no Forge fallback,
  and no MAPPING_FAILED result for each case.

- [ ] Run the focused external workload and record its output.

    mvn -pl forge-gui-desktop -am '-Dtest=FRL02KTriggeredTargetExternalOwnershipAuditTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

  Expected result: the separate control/external ownership proof passes and the
  adversarial cases fail closed.

- [ ] Commit the focused workload test.

    git add forge-gui-desktop/src/test/java/forge/view/FRL02KTriggeredTargetExternalOwnershipAuditTest.java
    git diff --cached --check
    git commit -m "test: prove external Blood target ownership transfer"

## Task 10: Add native compatibility and lifecycle regression assertions

Files:

- Modify: forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetDecisionCoordinatorTest.java
- Run: forge-gui-desktop/src/test/java/forge/game/decision/FRL02KTriggeredTargetProviderAuditTest.java
- Run: forge-gui-desktop/src/test/java/forge/view/FRL02KTriggeredTargetOwnershipAuditTest.java

- [ ] Add native zero-target, forced one-target, and multi-target tests.

  Native zero candidates must return false/no-stack with no DecisionRequest
  capture. Native one candidate must call Forge AI once, map exactly one new
  target, record native FORCED provenance, and never invoke a resolver. Native
  two-plus candidates must call Forge AI once, map one existing request
  candidate, and record native CHOSEN provenance.

- [ ] Add Target A/B/C lifecycle assertions without changing confirmation.

  Assert external preparation stores Target A on the live underlying ability,
  the existing stack path is used, and the later native confirmTrigger path is
  not routed through the C2A coordinator. Preserve the existing test evidence
  that native A can equal or differ from temporary B and that effect target C
  comes from stack-time A.

- [ ] Add no-retarget and fizzle assertions.

  After external apply and stack preparation, mutate the game so the target is
  illegal before resolution. Assert normal Forge fizzle/no-effect behavior and
  no second TARGET request. Assert no new target replaces the original A.

- [ ] Run the existing C2 provider and ownership audit classes.

    mvn -pl forge-gui-desktop -am '-Dtest=FRL02KTriggeredTargetProviderAuditTest,FRL02KTriggeredTargetOwnershipAuditTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

  Expected result: existing native Blood counts, hidden-name constraints,
  state/RNG neutrality, A/B divergence, and A/C identity remain unchanged.

- [ ] Commit lifecycle/regression assertions.

    git add forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetDecisionCoordinatorTest.java
    git diff --cached --check
    git commit -m "test: lock C2A native lifecycle and fizzle behavior"

## Task 11: Add the C2A audit document and measured result matrix

File:

- Create: docs/AI-ML DOCS/FRL_02K_C2A_TRIGGERED_TARGET_AUDIT.md

- [ ] Document the exact production API and ownership split.

  Record controller-local provider/resolver lifetime, provider-local request
  IDs, stateless coordinator responsibilities, the Generation API status
  handling, and the rule that TargetDecisionProvider.apply owns live legality
  and completion.

- [ ] Document admission, runtime, trace, and integrity evidence.

  Include the exact Blood profile, static/live checks, trigger-level optional
  semantics, immutable native snapshot, action-continuation gate,
  resolver-on/off classification, native/external/forced V2 flags,
  native-only MAPPING_FAILED, and external invalid-candidate behavior.

- [ ] Record the workload commands and exact outcomes.

  Include focused coordinator/trace/continuation commands, the canonical
  native Izzet Guild Kit versus Dimir Guild Kit ten-game seed 20260810
  regression, the external focused workload, the retained B1/C/C1/C2 locks,
  and any unavailable evidence as explicitly unverified.

- [ ] End the document with the narrow status:

    Blood Operative ETB TARGET: SUPPORTED (exact profile only)
    global triggered TARGET: OPEN
    Blood CONFIRMATION: OPEN
    global CONFIRMATION: OPEN
    FRL_02K_C2A_<PASS|PARTIAL|FAIL>

- [ ] Commit the measured audit documentation.

    git add "docs/AI-ML DOCS/FRL_02K_C2A_TRIGGERED_TARGET_AUDIT.md"
    git diff --cached --check
    git commit -m "docs: record FRL-02K-C2A implementation evidence"

## Task 12: Focused, broad, build, checkstyle, and diff verification

Files: all implementation, test, and documentation files above.

- [ ] Run the full focused C2A decision suite.

    mvn -pl forge-gui-desktop -am '-Dtest=TriggeredTargetDecisionCoordinatorTest,TriggeredTargetContinuationProcessTest,FRL02KTriggeredTargetExternalOwnershipAuditTest,DeterminismTraceV2Test,TargetDecisionProviderTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

  Expected result: all selected tests pass with no skipped C2A class caused by
  an incorrect Surefire filter.

- [ ] Run the retained C2/C2R tests.

    mvn -pl forge-gui-desktop -am '-Dtest=FRL02KTriggeredTargetProviderAuditTest,FRL02KTriggeredTargetOwnershipAuditTest,DecisionPublicApiReflectionTest,PriorityActionDiagnosticsTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

  Expected result: existing audits pass without changes to native confirmation
  or diagnostic semantics.

- [ ] Run the broad relevant Maven test reactor.

    mvn -pl forge-gui-desktop -am test

  If the command reaches the 60-second command limit, split it by module:

    mvn -pl forge-game -am test
    mvn -pl forge-ai -am test
    mvn -pl forge-gui-desktop -am test

  Record each exact exit status; do not report a broad pass when any split
  command remains failed or unverified.

- [ ] Run validation/build and Checkstyle.

    mvn -pl forge-gui-desktop -am validate
    mvn -pl forge-gui-desktop -am verify -DskipTests

  If verify exceeds the command limit, run:

    mvn -pl forge-game -am verify -DskipTests
    mvn -pl forge-ai -am verify -DskipTests
    mvn -pl forge-gui-desktop -am verify -DskipTests

  Record compiler, Checkstyle, and packaging outcomes separately.

- [ ] Run whitespace and scope checks.

    git diff --check origin/master...HEAD
    git diff --name-only origin/master...HEAD
    git status --short --branch

  Expected changed paths are limited to the implementation map and approved
  C2A documentation/specification. No Blood script or unrelated decision family
  may appear.

- [ ] Inspect the final diff for forbidden coordinator/provider duplication.

    rg -n "canTarget|getAllCandidates|CardUtil|getValidCardsToTarget|legalTargetPrototypes" forge-game/src/main/java/forge/game/decision/TriggeredTargetDecisionCoordinator.java
    rg -n "Blood Operative|BLOOD_OPERATIVE|TriggeredTargetDecisionCoordinator" forge-game/src/main/java/forge/game/decision/TargetDecisionProvider.java
    rg -n "brains\\.doTrigger|resolve\\(|getCandidates\\(\\)\\.size\\(\\)" forge-ai/src/main/java/forge/ai/PlayerControllerAi.java

  Expected output: the coordinator legality search is empty; the generic
  provider contains no Blood/coordinator semantics; PlayerControllerAi contains
  only the native callback adapter and status routing, not resolver/candidate
  policy logic.

- [ ] Run a final placeholder and trace-contract scan.

    rg -n "DecisionType\\.TARGET.*externalChosen|isBCPolicySample" docs forge-game/src/main/java forge-ai/src/main/java forge-gui-desktop/src/test/java

  Expected output: validator condition is explicitly TARGET-or-CONFIRMATION;
  isBCPolicySample remains unchanged. Review the C2A files manually for any
  unfinished implementation marker before committing.

- [ ] Commit only after all required evidence is recorded.

    git add forge-game forge-ai forge-gui-desktop docs
    git diff --cached --check
    git status --short
    git commit -m "feat: complete FRL-02K-C2A triggered target seam"

## Task 13: Review, draft PR, and handoff boundary

Files: final branch diff and audit document.

- [ ] Perform a final requirements review against
      docs/superpowers/specs/2026-08-11-frl-02k-c2a-triggered-target-provider-seam-design.md.

  Check every approved requirement: resolver-null native preservation,
  external unsupported hard fail, exact Blood admission, trigger-only
  optionality, empty initial targets, chooser seat equality, null continuation,
  immutable native snapshot, one native callback, 0/1/many external behavior,
  COMPLETE after apply, provider-owned legality, native-only MAPPING_FAILED,
  TARGET validator extension, BC exclusion, hidden-info boundary, controller
  isolation, A/B/C boundary, and no-retarget behavior.

- [ ] Use the requesting-code-review workflow on the completed branch and fix
      only findings within C2A scope. Re-run the affected focused test after
      each fix.

- [ ] Push branch frl/02k-c2a-triggered-target-provider-seam and open a Draft
      PR only after the final commit and verification succeed. Do not merge.

- [ ] Prepare the final report with exactly the requested sections 1 through
      47, including files, tests, build/checkstyle/diff evidence, P0/P1/P2
      status, narrow milestone verdict, Draft PR, and the final STOP boundary.

## Self-review checklist

Spec coverage:

- Section 1 boundary and the five approved design sections map to Tasks 4-7
  and Task 13.
- Applicability correction and resolver-dependent fail-closed behavior map to
  Tasks 3 and 5.
- Trigger-level OptionalDecider and non-optional live effect map to Task 5.
- Copied-trigger versus cloned-source provenance maps to Task 5.
- Generation API precision maps to the API contract and Task 6.
- Provider-owned live legality maps to Tasks 4 and 6.
- Immutable native capture and native-only MAPPING_FAILED map to Task 6.
- External TARGET V2 validator extension and BC exclusion map to Task 8.
- Continuation hard fail maps to Tasks 3 and 8.
- Native/external ownership-difference proof maps to Task 9.
- Provider/controller-local request IDs map to Tasks 2 and 4.
- Native canonical workload, focused external workload, regression locks,
  build/checkstyle/diff checks, documentation, and Draft PR map to Tasks 9-13.

Completeness scan:

- Every implementation task names exact files, methods/contracts, commands,
  and expected outcomes; no task delegates a required decision to a future
  placeholder.

Type consistency:

- Resolver always means TargetDecisionProvider.Resolver.
- Generation always means TargetDecisionProvider.Generation.
- Native mapping always consumes Preparation and boolean nativeResult.
- External application always consumes DecisionRequest and LegalCandidate,
  calls TargetDecisionProvider.apply exactly once, and requires Generation
  status COMPLETE.
- Trace methods use recordNativeMappedResult,
  recordExternalChosenResult, recordEngineForced, and recordMappingFailed only
  on their approved paths.
