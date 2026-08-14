# FRL-02L2A SURVEIL_PARTITION Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Implement the approved FRL-02L2A Surveil graveyard-membership boundary as a typed CARD_SELECTION contract with deterministic native teacher capture, no L2B ownership, and no change to native Surveil gameplay semantics.

**Architecture:** Add a Surveil-specific typed decision family under the existing CARD_SELECTION DecisionType. A PlayerController-owned provider allocates controller-local sessions, a coordinator captures and validates the existing composite arrangeForSurveil callback exactly once, and the post-callback native partition is materialized into one binary membership trace request/result at a time. Native snapshot order stays private for identity and L2B continuity; policy traversal uses canonical chooser-visible projection order.

**Tech Stack:** Java 17, Maven multi-module build, TestNG, existing Forge Pair/CardCollection types, existing DecisionRequest/LegalCandidate envelope, existing DECISION_TRACE_V3 serialization, and the current native Human/AI PlayerController implementations.

---

## Authority and locked scope

The implementation must start from the approved design authority:

- Design: docs/superpowers/specs/2026-08-14-frl-02l2a-surveil-partition-design.md
- Repository: C:\forgeAI
- Engine owner: Player.surveil receiver
- Native callback: PlayerController.arrangeForSurveil
- Decision type: DecisionType.CARD_SELECTION
- Profile: SURVEIL_PARTITION
- Candidate kinds: CLASSIFY_GRAVEYARD and CLASSIFY_RETAIN
- Transitional ownership: L2A_TEACHER_CAPTURE_ONLY_UNTIL_L2B
- Trace: existing DECISION_TRACE_V3, no new trace version

Do not modify:

- DecisionType
- CardSelectionAdapter
- CardSelectionCard
- CardSelectionSession
- CardSelectionDecisionProvider
- PlayerController.arrangeForSurveil signature
- PlayerControllerHuman.arrangeForSurveil behavior
- PlayerControllerAi.arrangeForSurveil behavior
- SurveilAi
- ComputerUtil.scryWillMoveCardToBottomOfLibrary
- card scripts
- L2B retained-top ORDER
- any external Surveil resolver slot
- any new generic partition or continuation framework

The implementation must preserve these engine guarantees:

~~~text
capture admission failure -> native callback exactly once on the existing path
valid native capture -> native callback exactly once, then sequential trace materialization
native mapping failure -> original native Pair returned unchanged
native callback exception -> original exception rethrown
zone movement/replacements/events/triggers -> existing Player.surveil and GameAction path
~~~

Work in the requested checkout and preserve the existing branch boundary. Creating a branch, committing, pushing, or opening a PR requires separate authorization; this plan itself performs none of those actions.

## Future file map

New production files:

- forge-game/src/main/java/forge/game/decision/SurveilPartitionProfile.java
- forge-game/src/main/java/forge/game/decision/SurveilPartitionCandidateKind.java
- forge-game/src/main/java/forge/game/decision/SurveilPartitionCard.java
- forge-game/src/main/java/forge/game/decision/SurveilPartitionContext.java
- forge-game/src/main/java/forge/game/decision/SurveilPartitionItemId.java
- forge-game/src/main/java/forge/game/decision/SurveilPartitionSession.java
- forge-game/src/main/java/forge/game/decision/SurveilPartitionDecisionProvider.java
- forge-game/src/main/java/forge/game/decision/SurveilPartitionDecisionCoordinator.java
- forge-game/src/main/java/forge/game/decision/SurveilPartitionDiagnostics.java (process-local counter aggregation and opt-in audit-properties writer; no session/ID ownership)

Modified production files:

- forge-game/src/main/java/forge/game/player/Player.java
- forge-game/src/main/java/forge/game/player/PlayerController.java
- forge-game/src/main/java/forge/game/decision/DecisionRequest.java
- forge-game/src/main/java/forge/game/decision/LegalCandidate.java
- forge-game/src/main/java/forge/game/decision/DecisionTraceRequestRecord.java
- forge-game/src/main/java/forge/game/decision/DeterminismTrace.java
- forge-game/src/main/java/forge/game/decision/DecisionTraceTrainingValidator.java

New test files:

- forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionDecisionProviderTest.java
- forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionItemIdTest.java
- forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionSessionTest.java
- forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionDecisionEnvelopeTest.java
- forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionDecisionCoordinatorTest.java
- forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionPublicApiTest.java
- forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionTraceTest.java
- forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionEngineIntegrationTest.java
- forge-gui-desktop/src/test/java/forge/view/FRL02L2ASurveilPartitionAuditTest.java

No existing test file needs modification. Existing CardSelection, DecisionTrace V2/V3, L1, and L1C tests remain regression coverage.

---

### Task 1: Add the typed Surveil public model and pure deterministic item-ID helper

**Files:**
- Create: forge-game/src/main/java/forge/game/decision/SurveilPartitionProfile.java
- Create: forge-game/src/main/java/forge/game/decision/SurveilPartitionCandidateKind.java
- Create: forge-game/src/main/java/forge/game/decision/SurveilPartitionCard.java
- Create: forge-game/src/main/java/forge/game/decision/SurveilPartitionContext.java
- Create: forge-game/src/main/java/forge/game/decision/SurveilPartitionItemId.java
- Create: forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionPublicApiTest.java
- Create: forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionItemIdTest.java

- [ ] Step 1: Write the public-boundary tests first.

Cover the exact v0 public shape:

~~~java
@Test
public void publicCardContainsOnlyOpaqueItemIdAndVisibleName() {
    assertEquals(publicFields(SurveilPartitionCard.class),
            Set.of("getItemId", "getVisibleName"));
    assertFalse(hasFieldOfType(SurveilPartitionCard.class,
            Card.class, CardView.class, CardLKI.class));
    assertFalse(hasFieldNamed(SurveilPartitionCard.class,
            "cardId", "gameTimestamp", "ownerId", "controllerId", "zone"));
}

@Test
public void contextContainsExactlyTheApprovedPublicFields() {
    assertEquals(publicFields(SurveilPartitionContext.class),
            Set.of("getProfile", "getSurveilSessionId", "getDecisionStepIndex",
                    "getChoosingPlayerId", "getOriginalItemCount",
                    "getVisibleItems", "getCurrentItemId"));
    assertFalse(hasFieldNamed(SurveilPartitionContext.class,
            "gameId", "nativeSnapshot", "nativeIdentityMap", "selectedLabels",
            "remainingLabels", "retainedNativeOrder"));
}

@Test
public void duplicateLookingCardsRemainDistinct() {
    final SurveilPartitionCard first = card(1L, "Island");
    final SurveilPartitionCard second = card(2L, "Island");

    assertNotEquals(first.getItemId(), second.getItemId());
    assertEquals(first.getVisibleName(), second.getVisibleName());
}
~~~

Use the existing DecisionPublicApiReflectionTest style. The reflection test must inspect public fields and methods without requiring native Card objects to cross the boundary.

- [ ] Step 2: Run the focused test and verify the new types are absent.

Run from C:\forgeAI:

~~~powershell
mvn -pl forge-gui-desktop -am "-Dtest=SurveilPartitionPublicApiTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
~~~

Expected: FAIL because the new Surveil types do not yet exist. Do not alter generic CardSelection classes to make this test compile.

- [ ] Step 3: Implement the profile, candidate-kind, card, and context types.

Use these exact public semantics:

~~~java
public enum SurveilPartitionProfile {
    SURVEIL_PARTITION
}

public enum SurveilPartitionCandidateKind {
    CLASSIFY_GRAVEYARD,
    CLASSIFY_RETAIN
}

public final class SurveilPartitionCard {
    private final long itemId;
    private final String visibleName;

    public long getItemId() { return itemId; }
    public String getVisibleName() { return visibleName; }
}

public final class SurveilPartitionContext {
    private final SurveilPartitionProfile profile;
    private final long surveilSessionId;
    private final int decisionStepIndex;
    private final int choosingPlayerId;
    private final int originalItemCount;
    private final List<SurveilPartitionCard> visibleItems;
    private final long currentItemId;

    public SurveilPartitionProfile getProfile() { return profile; }
    public long getSurveilSessionId() { return surveilSessionId; }
    public int getDecisionStepIndex() { return decisionStepIndex; }
    public int getChoosingPlayerId() { return choosingPlayerId; }
    public int getOriginalItemCount() { return originalItemCount; }
    public List<SurveilPartitionCard> getVisibleItems() { return visibleItems; }
    public long getCurrentItemId() { return currentItemId; }
}
~~~

Make constructors package-private so only the provider/coordinator can create the public values. All lists must be immutable copies. Do not add gameId, native objects, CardView, CardLKI, card IDs, timestamps, zone data, original library positions, AI state, or retained order.

- [ ] Step 4: Implement the pure deterministic itemId helper.

Create the package-private helper `SurveilPartitionItemId` with no session, Card, provider, or native dependencies. It accepts only the canonical rank integer. Never derive the public token from native snapshot position, native Card identity, card ID, game timestamp, session ID, wall-clock time, or RNG.

Use the fixed canonical rank input 1..N and this deterministic bijective 64-bit mixer:

~~~java
final class SurveilPartitionItemId {
    private SurveilPartitionItemId() {
    }

    static long opaqueItemId(final int canonicalRank) {
        if (canonicalRank < 1) {
            throw new IllegalArgumentException("canonicalRank must be positive");
        }
        long z = 0x9E3779B97F4A7C15L ^ (long) canonicalRank;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
~~~

Keep the helper package-private and do not expose it as a public policy API. The mixer is deterministic and invertible as an integer transform, not a cryptographic rank-hider. In this contract, `opaque` means equality-only: the policy adapter must not sort by it, expose its magnitude, or include it in the observation feature vector.

- [ ] Step 5: Add pure-helper tests.

Add these test methods with the stated assertions:

- `opaqueItemIdIsDeterministicForTheSameCanonicalRank`: call the helper twice for each rank in 1..4 and assert identical results.
- `opaqueItemIdIsDistinctForTheCanonicalRanksUsedByOneSession`: call the helper for ranks 1..4 and assert all four equality tokens differ.
- `opaqueItemIdRejectsNonPositiveCanonicalRank`: assert rank 0 and a negative rank throw IllegalArgumentException.
- `opaqueItemIdIgnoresNativeSessionAndCardInputs`: assert the helper has no public API and that equal rank input produces the same token regardless of test-only native/session values that are not passed to it.

Canonical ordering, private tie-break behavior, and equivalent-session stability are tested only after session admission exists in Task 3.

- [ ] Step 6: Run the focused tests.

Run:

~~~powershell
mvn -pl forge-gui-desktop -am "-Dtest=SurveilPartitionPublicApiTest,SurveilPartitionItemIdTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
~~~

Expected: all public-model tests pass after their implementation is present. Do not create a commit as part of plan authoring.

---

### Task 2: Extend the shared request envelope without broadening generic CARD_SELECTION

**Files:**
- Modify: forge-game/src/main/java/forge/game/decision/DecisionRequest.java
- Modify: forge-game/src/main/java/forge/game/decision/LegalCandidate.java
- Create: forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionDecisionEnvelopeTest.java

- [ ] Step 1: Write the failing envelope tests.

Add these test methods with the stated assertions:

- `surveilRequestUsesCardSelectionAndTypedContext`: assert DecisionType.CARD_SELECTION and exactly one SurveilPartitionContext with the approved public fields.
- `surveilCandidatesUseOnlyTheirTypedPayload`: assert each candidate exposes one approved kind and one public item projection, with no generic subset payload.
- `legacyCardSelectionContextRulesRemainUnchanged`: run the existing generic CARD_SELECTION construction/validation cases and assert their prior acceptance and rejection behavior.
- `surveilRequestRejectsWrongProfileCandidateStepOrItem`: submit mismatched profile, step, item, and semantic-key candidates and assert rejection.
- `genericCardSelectionDoesNotAcceptSurveilNativeFields`: construct a generic request and assert native snapshot identity and Surveil-private fields cannot be attached through the generic envelope.

- [ ] Step 2: Run the envelope tests to establish the failing boundary.

Run from C:\\forgeAI:

~~~powershell
mvn -pl forge-gui-desktop -am "-Dtest=SurveilPartitionDecisionEnvelopeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
~~~

Expected: FAIL because the typed Surveil candidate payload and mutually exclusive context do not yet exist. Do not alter generic CardSelection classes to make this test compile.

- [ ] Step 3: Implement the typed Surveil payload in LegalCandidate.

Add these private fields, accessors, and factory while preserving every existing field and constructor meaning:

~~~java
private final SurveilPartitionCandidateKind surveilPartitionCandidateKind;
private final SurveilPartitionCard surveilPartitionCard;

public SurveilPartitionCandidateKind getSurveilPartitionCandidateKind();
public SurveilPartitionCard getSurveilPartitionCard();

public static LegalCandidate surveilPartition(
        final int candidateId,
        final SurveilPartitionCandidateKind kind,
        final SurveilPartitionCard item) {
    final String operation = kind == SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD
            ? "CLASSIFY_GRAVEYARD" : "CLASSIFY_RETAIN";
    return new LegalCandidate(candidateId, DecisionType.CARD_SELECTION,
            "SURVEIL_PARTITION|" + operation + "|" + item.getItemId(),
            kind, item);
}
~~~

The constructor must set every unrelated typed payload to null. Do not add native Card, CardView, CardLKI, card ID, timestamp, zone, owner, controller, or SpellAbility to the Surveil payload.

- [ ] Step 4: Add the mutually exclusive Surveil context to DecisionRequest.

Add a SurveilPartitionContext field and a constructor overload with this exact input shape:

~~~java
DecisionRequest(final long requestId, final DecisionType decisionType,
        final List<LegalCandidate> candidates,
        final SurveilPartitionContext surveilPartitionContext)
~~~

Validate all of the following for the Surveil overload:

- DecisionType is CARD_SELECTION;
- exactly one of CardSelectionContext and SurveilPartitionContext is present;
- Surveil context profile is SURVEIL_PARTITION;
- candidates contain exactly two legal candidates;
- both candidates reference the same currentItemId;
- one candidate is CLASSIFY_GRAVEYARD and the other is CLASSIFY_RETAIN;
- semantic keys exactly match the typed item and operation;
- no ORDER, target, payment, continuation, or legacy CardSelection payload is attached;
- neither candidate is forced.

Do not alter the existing generic CARD_SELECTION constructor or CardSelectionSession validation logic except to enforce context mutual exclusion.

- [ ] Step 5: Run envelope and regression tests.

Run:

~~~powershell
mvn -pl forge-gui-desktop -am "-Dtest=SurveilPartitionDecisionEnvelopeTest,CardSelectionDecisionProviderTest,DecisionPublicApiReflectionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
~~~

Expected: Surveil envelope tests pass and existing generic CARD_SELECTION/public API tests remain green.

---

### Task 3: Implement the private snapshot, canonical traversal, binary provider, and session state

**Files:**
- Create: forge-game/src/main/java/forge/game/decision/SurveilPartitionSession.java
- Create: forge-game/src/main/java/forge/game/decision/SurveilPartitionDecisionProvider.java
- Create: forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionSessionTest.java
- Create: forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionDecisionProviderTest.java

- [ ] Step 1: Write session/provider tests for canonical identity and the complete binary domain.

Cover these exact cases:

Add these test methods with the stated assertions:

- `nativeSnapshotPermutationDoesNotChangeCanonicalPublicOrderOrItemIds`: reverse two equivalent native input lists and assert equal ordered visibleName/itemId pairs.
- `privateTieBreakStabilizesExactPublicTiesWithoutExposingTieData`: use equal visibleName values with distinct private stable tuples, assert deterministic order, and assert neither tuple field appears in the public projection or semantic key.
- `itemIdsAreStableAcrossFreshEquivalentSessions`: create two fresh sessions from equal public projections and equal private stable tuples, then assert equal ordered item IDs.
- `itemIdIsNotDerivedFromNativeSnapshotPosition`: permute native positions while keeping each card's private stable tuple fixed, then assert each public item ID follows canonical rank rather than input position.
- `closedSessionIsRemovedAndCannotBeReused`: close an admitted session, assert active registry size returns to zero, and assert the closed session cannot create or apply another request.
- `newSessionAfterCloseUsesTheSameControllerProviderWithoutRegistryGrowth`: admit, close, and admit again through one provider; assert the second session has a higher controller-local ID and active registry size is one rather than two.
- `zeroSnapshotCreatesNoPublicRequest`: admit N=0 and assert that no request is produced.
- `everyNonEmptyRequestHasExactlyGraveyardAndRetainCandidates`: for N=1, N=2, and N=4 assert exactly two candidates with the two approved kinds.
- `nOneIsNotForced`: for N=1 assert `forced == false` and both candidates are present.
- `nTwoAndNFourUseCanonicalProjectionOrder`: assert that each request cursor follows the canonical visibleName order for N=2 and N=4.
- `everySubsetMapsToExactlyOneMembershipVector`: enumerate all binary label vectors in the test harness and assert each maps to exactly one completed session without creating click-order or DONE records.
- `duplicateNativeObjectIsCaptureIntegrityFailure`: admit the same native Card reference twice and assert admission failure before a public request.
- `duplicatePrivateStableIdentityIsCaptureIntegrityFailure`: admit distinct native objects with the same private stable tuple and assert admission failure.
- `wrongProfileWrongStepForeignOrAlreadyClassifiedCandidateIsRejected`: submit each invalid candidate variant and assert rejection without advancing the session.
- `publicLabelsDoNotUseItemIdOrPrivateNativeIdentity`: assert public candidate keys contain only the approved operation and item equality token, with no native identity or private tie-break data.

The provider tests must assert that there is no DONE candidate, no remaining-card candidate set, no one-candidate request, and no subset enumeration. N=0 is the only no-request case; every N>0 request has exactly two candidates.

- [ ] Step 2: Run the provider tests to establish the failing boundary.

Run:

~~~powershell
mvn -pl forge-gui-desktop -am "-Dtest=SurveilPartitionSessionTest,SurveilPartitionDecisionProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
~~~

Expected: FAIL because the private session/provider types, canonical traversal, and binary request generation are not implemented.

- [ ] Step 3: Implement private SurveilItem/session admission.

Keep native state private inside the package-private session implementation. The session must retain:

~~~text
controller-local surveilSessionId
gameId for private drift validation
choosingPlayerId
immutable native topN snapshot in native order
private native ordinal
opaque itemId
SurveilPartitionCard projection
private IdentityHashMap<Card, SurveilItem>
private stable (cardId, gameTimestamp) identity
canonical policy item list
per-item GRAVEYARD/RETAIN labels
retained native list after mapping
current step and one-outstanding state
closed state and terminal close reason
~~~

Expose one package-private session lookup for the already validated native membership vector:

~~~java
SurveilPartitionCandidateKind nativeMembershipKindAt(int canonicalStep);
~~~

It returns the private GRAVEYARD/RETAIN label for that canonical step and exposes no Card or native identity.

Admission rules:

- reject null topN, null chooser, null cards, duplicate native object identity, or duplicate private stable identity;
- verify each card is chooser-visible before constructing the public projection;
- capture the native list before invoking arrangeForSurveil;
- use IdentityHashMap<Card, SurveilItem> for result mapping;
- create public projections with only itemId and visibleName;
- sort policy items by visibleName String.compareTo, then private (cardId, gameTimestamp);
- assign item IDs from the fixed opaqueItemId(canonicalRank) function;
- expose canonical order through visibleItems, never native order;
- never include private tie-break fields in context, candidate keys, trace records, or diagnostics.

The native snapshot order remains available only for exact native pair validation and later L2B continuation.

The canonical rank is assigned only after that sort. For exact public ties, the private `(cardId, gameTimestamp)` tuple determines which duplicate receives the earlier canonical rank and therefore which deterministic equality handle it receives. This is an internal mapping dependency, not a public feature or rank-hiding guarantee. Two equivalent sessions with the same visibleName sequence and the same private stable tuples must produce the same ordered itemId values even when native topN input order is reversed.

- [ ] Step 4: Implement provider-owned session and request allocation.

The PlayerController-owned provider must own:

~~~java
private long nextSurveilSessionId = 1L;
private long nextRequestId = 1L;
private final Map<Long, SurveilPartitionSession> activeSessions = new HashMap<>();
~~~

The provider must expose these exact package-private operations:

~~~java
long nextSurveilSessionId();
long nextRequestId();
SurveilPartitionSession admit(Player chooser, List<Card> privateSnapshot);
DecisionRequest createMembershipRequest(SurveilPartitionSession session);
void applyMembershipCandidate(SurveilPartitionSession session, LegalCandidate candidate);
boolean isComplete(SurveilPartitionSession session);
void closeSession(SurveilPartitionSession session);
int activeSessionCount();
~~~

Use these method names and signatures so the coordinator and tests share one unambiguous boundary. `admit` receives only the coordinator-owned immutable `privateSnapshot`; it must never receive, retain, mutate, or pass through the original mutable `topN` collection. The original collection belongs exclusively to the native callback. Session IDs and request IDs are controller-local counters; they must not be static or global. Session IDs are correlation metadata and never enter candidate semantic keys.

createMembershipRequest must:

- use the current canonical item only;
- emit exactly CLASSIFY_GRAVEYARD and CLASSIFY_RETAIN for that item;
- set DecisionType.CARD_SELECTION;
- attach exactly one SurveilPartitionContext;
- set forced to false by virtue of exactly two candidates;
- reject creation if the session is stale, complete, or already has an open request.

applyMembershipCandidate must verify:

~~~text
candidate belongs to this request
candidate profile is SURVEIL_PARTITION
candidate itemId equals currentItemId
candidate kind is CLASSIFY_GRAVEYARD or CLASSIFY_RETAIN
candidate semantic key is exact
candidate has not already been applied
session/player/game identity is stable
~~~

It then records one label and advances the step. It must not move cards or mutate Forge zones.

`closeSession` must remove the exact registered session by identity, mark it closed, and make later request creation/application fail as stale. `activeSessionCount` is package-private test/diagnostic visibility only; it must not expose native objects or become an ID allocator. A successful terminal session, a mapping failure, and a callback exception all call `closeSession` when the session was registered. Capture admission failure registers nothing. No L2B handoff exists in this milestone; a future approved L2B seam may transfer ownership before closing instead.

- [ ] Step 5: Add public-symmetry grouping to the session.

Define the symmetry key from all policy-visible card fields except itemId, session ID, player ID, step, and current-item cursor. In v0 the key is exactly visibleName.

For each group:

~~~text
all GRAVEYARD -> no conflict
all RETAIN -> no conflict
mixed GRAVEYARD/RETAIN -> symmetryConflict = true
~~~

Store the conflict as private session metadata for teacher eligibility and sanitized diagnostics. Do not collapse duplicate-looking items and do not use the private tie-break to clear the conflict.

- [ ] Step 6: Run provider/session tests.

Run:

~~~powershell
mvn -pl forge-gui-desktop -am "-Dtest=SurveilPartitionSessionTest,SurveilPartitionDecisionProviderTest,SurveilPartitionDecisionEnvelopeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
~~~

Expected: all session/provider/envelope tests pass, including registry removal and post-close session reuse. Existing CardSelection tests must remain unchanged and passing.

---

### Task 4: Make PlayerController the durable owner and add capture-only native coordination

**Files:**
- Create: forge-game/src/main/java/forge/game/decision/SurveilPartitionDecisionCoordinator.java
- Create: forge-game/src/main/java/forge/game/decision/SurveilPartitionDiagnostics.java
- Modify: forge-game/src/main/java/forge/game/player/PlayerController.java
- Modify: forge-game/src/main/java/forge/game/player/Player.java
- Create: forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionDecisionCoordinatorTest.java
- Create: forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionEngineIntegrationTest.java

- [ ] Step 1: Write callback-count and failure-isolation tests.

Use a callback counter and verify:

Add these test methods with the stated assertions:

- `validNativeCaptureCallsArrangeForSurveilExactlyOnce`: count callback invocations and assert one invocation plus the original native Pair object.
- `nativeCallbackReceivesOriginalMutableTopNInstance`: capture the callback argument and assert `assertSame(originalTopN, callbackArgument)`; assert the callback can observe the native Human mutation on that same collection.
- `nullTopNStillCallsNativeExactlyOnce`: pass null topN, assert no session is registered, assert `nativeArrange.apply(null)` occurs exactly once, and preserve the native callback result or exception unchanged.
- `nullCardEntryIsClassifiedAsCaptureAdmissionFailure`: pass a mutable topN containing one null element, assert the private snapshot preserves that null entry, admission fails before session registration, and nativeArrange still receives the original topN once.
- `captureAdmissionFailureCallsNativeExactlyOnceAndCreatesNoL2ARequest`: use an inadmissible snapshot, assert one native invocation, no membership handle, and unchanged native gameplay result.
- `nativePairMappingFailureReturnsOriginalPairAndCreatesNoMembershipRows`: return a malformed partition, assert the same Pair object and zero membership request/result rows.
- `nativeCallbackFailureRethrowsWithoutSecondInvocationOrTraceRows`: throw from the callback, assert the same exception instance, one invocation, and no membership rows.
- `terminalCapturePathsRemoveRegisteredSessions`: run successful mapping, mapping failure, and callback-throw paths and assert the controller provider reports activeSessionCount() == 0 after each.
- `nullEmptyHumanSideIsNormalizedForValidationButOriginalPairIsReturned`: validate a null/empty human side as the approved empty side while returning the original Pair unchanged.
- `retainedOrderIsIgnoredForMembershipButReturnedUnchanged`: assign labels from the graveyard side only and assert retained order is preserved byte-for-byte as the native result.

The engine integration tests must observe that the coordinator returns before existing GameAction movement begins, and that Player.surveil continues to own moveToGraveyard, moveToLibrary, replacement handling, setSurveilled, events, and triggers.

- [ ] Step 2: Run the coordinator tests to establish the failing behavior.

Run:

~~~powershell
mvn -pl forge-gui-desktop -am "-Dtest=SurveilPartitionDecisionCoordinatorTest,SurveilPartitionEngineIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
~~~

Expected: FAIL because the coordinator, controller ownership, and Player.surveil integration do not yet exist.

- [ ] Step 3: Add PlayerController-owned provider/coordinator fields.

Add imports and fields beside the existing controller-local providers:

~~~java
private final SurveilPartitionDecisionProvider surveilPartitionDecisionProvider =
        new SurveilPartitionDecisionProvider();
private final SurveilPartitionDecisionCoordinator surveilPartitionDecisionCoordinator =
        new SurveilPartitionDecisionCoordinator(surveilPartitionDecisionProvider);
~~~

Add final accessors:

~~~java
public final SurveilPartitionDecisionProvider getSurveilPartitionDecisionProvider() {
    return surveilPartitionDecisionProvider;
}

public final SurveilPartitionDecisionCoordinator getSurveilPartitionDecisionCoordinator() {
    return surveilPartitionDecisionCoordinator;
}
~~~

Do not add a Resolver field, setter, or external ownership path. Do not change arrangeForSurveil or any Human/AI method.

- [ ] Step 4: Implement the coordinator callback boundary.

Expose this coordinator entry point:

~~~java
public Pair<CardCollection, CardCollection> captureNativeSurveil(
        final Player chooser,
        final CardCollection topN,
        final Function<CardCollection, Pair<CardCollection, CardCollection>> nativeArrange)
~~~

The method must:

1. require a non-null nativeArrange function and retain the exact `topN` reference as `originalTopN`;
2. if `originalTopN == null`, record capture admission failure, do not register a session, and invoke `nativeArrange.apply(null)` exactly once;
3. otherwise create an immutable private snapshot with `Collections.unmodifiableList(new ArrayList<>(originalTopN))`; this copy must preserve null entries;
4. call `provider.admit(chooser, privateSnapshot)` only after the non-null snapshot exists; admission/session identity is derived exclusively from that snapshot and the provider registers a session only after admission succeeds;
5. validate chooser, cards, visibility, identity, and stable tuples against the private snapshot;
6. on capture-only admission failure, record sanitized diagnostics and invoke nativeArrange exactly once with `originalTopN`;
7. compute canonical policy traversal from public projections without changing `originalTopN`;
8. invoke `nativeArrange.apply(originalTopN)` exactly once; the callback never receives the private snapshot, a reconstructed collection, or a second collection instance;
9. normalize null left/right lists as empty only for validation;
10. validate exact native object identity, no null entries, no duplicates, no overlap, no omission, no foreign card, and exact cardinality;
11. map only graveyard membership to per-item labels;
12. materialize trace rows only after complete mapping succeeds;
13. return the original Pair object unchanged;
14. rethrow native exceptions unchanged;
15. never call nativeArrange a second time.

The callback boundary must be equivalent to this ownership sequence:

~~~java
final CardCollection originalTopN = topN;
if (originalTopN == null) {
    diagnostics.recordCaptureAdmissionFailure("NULL_TOP_N");
    return nativeArrange.apply(null);
}
final List<Card> privateSnapshot = Collections.unmodifiableList(new ArrayList<>(originalTopN));
// admission and canonical projection use privateSnapshot only; null entries remain visible to admission
final SurveilPartitionSession session = provider.admit(chooser, privateSnapshot);
// an admission rejection takes the capture-only fallback before nativePair assignment
final Pair<CardCollection, CardCollection> nativePair = nativeArrange.apply(originalTopN);
// validation maps nativePair against privateSnapshot by object identity
~~~

The unmodifiable `ArrayList` copy is private capture authority only. It must never be passed to `arrangeForSurveil`; the native callback must receive the original mutable `CardCollection` instance. A null topN is handled before copying, and a null element remains in the copy so admission can classify it as capture failure rather than the copy operation throwing first.

The coordinator must close the registered parent session on every terminal path:

~~~text
topN == null or admission failure -> no registered session; native callback once
native callback throws after registration -> provider.closeSession(session); rethrow unchanged
native pair mapping failure -> provider.closeSession(session); return original pair unchanged
successful mapping and complete trace materialization -> provider.closeSession(session)
future L2B handoff -> not implemented in L2A; later seam may transfer before close
~~~

Use IdentityHashMap for native result validation. Do not compare cards with equals, names, IDs, timestamps, or projections.

- [ ] Step 5: Integrate at the existing Player.surveil line.

Replace only the direct callback expression in Player.surveil. Preserve the original mutable collection reference and pass it through the coordinator:

~~~java
final CardCollection originalTopN = topN;
final Pair<CardCollection, CardCollection> lists =
        getController().getSurveilPartitionDecisionCoordinator().captureNativeSurveil(
                this,
                originalTopN,
                getController()::arrangeForSurveil);
~~~

Keep the code after this line unchanged. In particular, retain:

~~~text
lists.getLeft()/getRight()
moveToGraveyard
setSurveilled
RememberMoved
Collections.reverse(toTop)
moveToLibrary
RememberKept
GameEventSurveil
TriggerType.Surveil
~~~

The integration must remain immediately after topN is captured and before any zone movement. The callback method reference receives exactly `originalTopN`; it must not be changed to `privateSnapshot`, an immutable replacement, or a reconstructed `CardCollection`.

- [ ] Step 6: Implement sanitized Surveil diagnostics.

SurveilPartitionDiagnostics must expose only counters/histograms for:

~~~text
raw arrangeForSurveil invocations
capture admission failures by reason
non-empty sessions
effective N buckets 0, 1, 2, >=3
native callback count/failures
valid partition mappings
mapping failures by reason
graveyard and retained cardinalities
binary request count
steps per session
candidate count and forced count
external attempts
trace incomplete count
public symmetry conflicts
teacher eligibility counts
~~~

Do not emit card names, item IDs, card IDs, timestamps, native objects, private zones, retained order, RNG state, AI thresholds, or player hand/battlefield data. No new Decision-Trace version is introduced; the separate diagnostics schema V1 below is permitted.

The runtime diagnostics contract must define these exact opt-in properties and schema constant:

~~~java
static final String AUDIT_ENABLED_PROPERTY = "forge.surveil.partition.audit.enabled";
static final String AUDIT_OUTPUT_PROPERTY = "forge.surveil.partition.audit.output";
static final String AUDIT_SCHEMA = "FRL02L2A_SURVEIL_AUDIT_V1";
void recordCaptureAdmissionFailure(String reason);
~~~

With `AUDIT_ENABLED_PROPERTY=true`, the diagnostics collector writes one UTF-8 Java-properties artifact to the path in `AUDIT_OUTPUT_PROPERTY` at normal child-JVM shutdown. With the property absent or false, it writes no audit artifact. The audit schema is separate from DECISION_TRACE_V3; it does not add or change a decision-trace version.

The collector is process-local aggregation only: all controller-owned coordinators publish counters to the one enabled collector so both player seats contribute to one workload artifact. It owns no provider, session, session ID, request ID, Card, or native object. Provider/coordinator/session ownership remains controller-local exactly as specified above. A fresh child JVM supplies the run isolation.

- [ ] Step 7: Run coordinator and engine tests.

Run:

~~~powershell
mvn -pl forge-gui-desktop -am "-Dtest=SurveilPartitionDecisionCoordinatorTest,SurveilPartitionEngineIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
~~~

Expected: valid capture, fallback, one-callback, no-pre-callback-handle, original-pair, and engine-boundary tests pass.

---

### Task 5: Add Surveil to DECISION_TRACE_V3 and implement post-callback trace materialization

**Files:**
- Modify: forge-game/src/main/java/forge/game/decision/DecisionTraceRequestRecord.java
- Modify: forge-game/src/main/java/forge/game/decision/DeterminismTrace.java
- Modify: forge-game/src/main/java/forge/game/decision/DecisionTraceTrainingValidator.java
- Modify: forge-game/src/main/java/forge/game/decision/SurveilPartitionDecisionCoordinator.java
- Create: forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionTraceTest.java

- [ ] Step 1: Write trace lifecycle tests first.

Cover:

Add these test methods with the stated assertions:

- `successfulNativeCaptureMaterializesNRequestsAfterCallbackInCanonicalOrder`: assert no request before callback completion, then N sequential request/result pairs in canonical order.
- `traceMaterializationUsesTheProviderStateMachine`: assert every post-callback item calls the existing createMembershipRequest/applyMembershipCandidate pair and no trace-only request generator exists.
- `onlyOneMembershipRequestHandleIsOpenAtAnyTime`: instrument trace handles and assert the maximum simultaneous open count is one.
- `mappingFailureCreatesNoMembershipRequestOrChosenResult`: inject a mapping failure and assert no partial membership rows exist.
- `nativeCallbackCompletedMeansParentCallbackForEveryMappedMembershipResult`: assert every mapped result has `nativeCallbackCompleted == true` because the shared parent callback completed once.
- `surveilTraceForcesV3WithoutChangingV2Interpretation`: assert Surveil-bearing traces serialize as V3 and historical non-Surveil V2 interpretation remains unchanged.
- `mixedPublicSymmetryLabelsAreDiagnosticOnlyUntilParity`: assert a mixed visibleName symmetry group records a diagnostic conflict but the current L2A result remains NOT_APPLICABLE, never BC_ELIGIBLE or BC_EXCLUDED_PUBLIC_SYMMETRY.
- `identicalPublicLabelsRemainDiagnosticNeutral`: assert equal labels in a public symmetry group do not create a symmetry conflict and the current L2A result remains NOT_APPLICABLE.
- `nativeHumanObservationParityIsAlsoNotApplicable`: assert the current native Human path remains NOT_APPLICABLE because no separately approved observation-parity milestone has run.
- `aiObservationParityRemainsNotApplicable`: assert the current native AI path remains NOT_APPLICABLE because observation parity is not proven.

- [ ] Step 2: Run the trace tests to establish the failing behavior.

Run:

~~~powershell
mvn -pl forge-gui-desktop -am "-Dtest=SurveilPartitionTraceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
~~~

Expected: FAIL because the profile enum, V3 routing, native Surveil eligibility path, and post-callback materialization are not implemented.

- [ ] Step 3: Add the SURVEIL_PARTITION trace profile.

Extend DecisionTraceRequestRecord.Profile with exactly:

~~~java
SURVEIL_PARTITION
~~~

Add an exact isSurveilPartitionRequest helper. V2 parsing must continue to map unknown/non-profiled historical requests to existing behavior; do not infer Surveil from an old stage string.

Update DeterminismTrace.decisionTraceVersion so any Surveil-bearing trace selects DECISION_TRACE_V3, just as an L1C-bearing trace does. Do not add DECISION_TRACE_V4 or any other version.

- [ ] Step 4: Materialize native membership records only after mapping.

In SurveilPartitionDecisionCoordinator, after the native Pair has passed full validation and the private membership vector is available:

~~~java
try {
    for (int step = 0; step < session.getCanonicalItems().size(); step++) {
        final DecisionRequest request = provider.createMembershipRequest(session);
        final SurveilPartitionCandidateKind expectedKind = session.nativeMembershipKindAt(step);
        final LegalCandidate chosen = request.getCandidates().stream()
                .filter(candidate -> candidate.getSurveilPartitionCandidateKind() == expectedKind)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("native membership candidate is not legal"));
        final DeterminismTrace.RequestHandle handle = DeterminismTrace.recordRequest(
                chooser.getGame(),
                chooser.getId(),
                request,
                "SURVEIL_PARTITION",
                step,
                DecisionTraceRequestRecord.Profile.SURVEIL_PARTITION,
                eligibilityFor(session, nativeOwner, step));
        handle.recordNativeMappedResult(chosen);
        provider.applyMembershipCandidate(session, chosen);
    }
} finally {
    provider.closeSession(session);
}
~~~

The following order is mandatory:

~~~text
no request handle before native callback
native callback completes
Pair validates completely
membership vector exists
provider.createMembershipRequest(session) creates one request
one native-mapped CHOSEN result recorded
provider.applyMembershipCandidate(session, chosen) closes/advances the same state machine
request closes
next item begins
~~~

The callback-count flag on every CHOSEN result is true because the shared parent arrangeForSurveil callback completed. It does not represent a new callback for that item. On mapping or callback failure, do not create partial request/result rows.

- [ ] Step 5: Implement teacher eligibility and symmetry precedence.

Use a pure eligibility decision with this precedence:

~~~text
invalid mapping -> no BC
observation parity not proven -> NOT_APPLICABLE
public symmetry group has mixed membership labels after a separately approved parity gate -> BC_EXCLUDED_PUBLIC_SYMMETRY
otherwise after a separately approved parity gate -> BC_ELIGIBLE
~~~

For the current L2A milestone, observation parity is not proven for either native owner. Native AI and native Human therefore remain NOT_APPLICABLE even when the native membership mapping is exact. A mixed public-symmetry group is diagnostic-only in this milestone; do not emit BC_ELIGIBLE or BC_EXCLUDED_PUBLIC_SYMMETRY. A future separately approved observation-parity milestone may enable the two positive outcomes without changing the L2A membership representation or trace version.

Do not add a new eligibility enum.

- [ ] Step 6: Update the training validator without broadening unrelated decisions.

DecisionTraceTrainingValidator must:

- accept a Surveil native-mapped CHOSEN result as history-valid when the candidate is legal and both nativeCallbackCompleted and mappingAttempted are true;
- require profile SURVEIL_PARTITION and explicit BC_ELIGIBLE metadata for a Surveil BC sample, while the current L2A coordinator emits no BC_ELIGIBLE metadata;
- reject NOT_APPLICABLE and BC_EXCLUDED_PUBLIC_SYMMETRY as BC samples;
- preserve all existing L1, L1C, V2, and non-Surveil behavior;
- add no new result kind and no new trace version.

No capture-only MAPPING_FAILED or NATIVE_CALLBACK_FAILURE row is created because no membership request exists at failure time.

- [ ] Step 7: Run trace and regression tests.

Run:

~~~powershell
mvn -pl forge-gui-desktop -am "-Dtest=SurveilPartitionTraceTest,DecisionTraceV2Test,DecisionTraceV3Test,DeterminismTraceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
~~~

Expected: Surveil traces serialize as V3, successful membership rows close sequentially, mapping failures produce no partial membership rows, and all existing trace tests remain green.

---

### Task 6: Complete public API, native mapping, replacement, and failure coverage

**Files:**
- Modify: forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionPublicApiTest.java
- Modify: forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionDecisionCoordinatorTest.java
- Modify: forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionEngineIntegrationTest.java
- Modify: forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionTraceTest.java

- [ ] Step 1: Add identity and visibility tests.

Test all admission cases:

~~~text
N=0, N=1, N=2, N=3, N=4
null card
same native Card object twice
distinct native objects with duplicate private stable tuple
foreign/stale native result card
chooser visibility failure
stable Player.surveil receiver authority
game/player/snapshot drift
~~~

Each capture-only admission failure must leave native ownership active and must produce no public Surveil request or Teacher/BC row.

- [ ] Step 2: Add complete native Pair validation tests.

Test:

~~~text
valid complete partition
valid null empty Human side
null Pair
null entry
foreign result card
omitted result card
duplicate result card
same card in both sides
wrong total cardinality
stale/replaced result object
callback throw
callback count remains exactly one
retained permutation does not change L2A label
~~~

The valid result path must return the original Pair and preserve retained list order for the existing engine. The L2A mapping must compare only native object identity for membership.

- [ ] Step 3: Add binary request and external-boundary tests.

Test the typed future ownership contract without installing a resolver:

~~~text
CLASSIFY_GRAVEYARD for current item
CLASSIFY_RETAIN for current item
stale candidate
foreign candidate
already-classified candidate
wrong profile
wrong stage
wrong request ID
wrong step
null external result
external resolver exception
~~~

The L2A-only coordinator must reject external ownership admission before any external request path and must never call arrangeForSurveil a second time.

- [ ] Step 4: Add context and hidden-information tests.

Assert that public requests do not expose:

~~~text
Card
CardView
CardLKI
SpellAbility
Player
Game
cardId
gameTimestamp
native object identity
original native library position
owner/controller IDs
zone object
AI state
RNG
retained order
~~~

Assert that visibleItems are canonical projection order and that itemId is used only for exact item/candidate identity. Do not add itemId to any policy feature vector or symmetry key.

- [ ] Step 5: Add symmetry and eligibility tests.

Test the exact groups:

~~~text
Island A -> GRAVEYARD, Island B -> GRAVEYARD
    no symmetry conflict

Island A -> RETAIN, Island B -> RETAIN
    no symmetry conflict

Island A -> GRAVEYARD, Island B -> RETAIN
    symmetry conflict
    diagnostic-only in current L2A
    NOT_APPLICABLE in current L2A
    BC_EXCLUDED_PUBLIC_SYMMETRY only after a separately approved parity gate
~~~

Also test that native Human and native AI retained shuffle/order never changes the L2A label or symmetry diagnostic, and that current L2A emits neither BC_ELIGIBLE nor BC_EXCLUDED_PUBLIC_SYMMETRY for either owner.

---

### Task 7: Run the canonical controlled workload and the full focused suite

**Files:**
- Create: forge-gui-desktop/src/test/java/forge/view/FRL02L2ASurveilPartitionAuditTest.java
- Use without modification: forge-gui-desktop/src/test/java/forge/view/ChildJvmSupport.java
- Modify only the implementation/test files listed above during execution.
- Do not modify card scripts or unrelated diagnostics.

- [ ] Step 1: Run the complete focused Surveil suite.

Run from C:\forgeAI:

~~~powershell
mvn -pl forge-gui-desktop -am "-Dtest=SurveilPartitionItemIdTest,SurveilPartitionDecisionEnvelopeTest,SurveilPartitionSessionTest,SurveilPartitionDecisionProviderTest,SurveilPartitionDecisionCoordinatorTest,SurveilPartitionPublicApiTest,SurveilPartitionTraceTest,SurveilPartitionEngineIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
~~~

Expected: all focused Surveil implementation tests pass, including pure ID determinism, envelope validation, no pre-callback request handles, canonical order, current fail-closed eligibility, V3 routing, native Pair mapping, and engine neutrality. The fresh-JVM audit harness is run separately in Step 3 after its test file is created.

- [ ] Step 2: Run the existing decision regression suite.

Run:

~~~powershell
mvn -pl forge-gui-desktop -am "-Dtest=CardSelectionDecisionProviderTest,DecisionPublicApiReflectionTest,DecisionTraceV2Test,DecisionTraceV3Test,DeterminismTraceTest,SimultaneousTriggerOrderPublicApiTest,CopySpellResolveFirstOrderPublicApiTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
~~~

Expected: existing generic CARD_SELECTION, L1, L1C, and trace tests remain green.

- [ ] Step 3: Run the canonical Izzet-vs-Dimir acceptance workload.

Create `FRL02L2ASurveilPartitionAuditTest.java` in this task. Reuse the existing test-only `ChildJvmSupport.java` to resolve the current Java executable and launch the workload directly through `ProcessBuilder`; do not invoke a shell or reuse the parent test JVM. Run one audit-enabled and one audit-disabled fresh child with identical logical inputs and compare their determinism trace trees for exact equality. The two children must use disjoint output roots:

~~~text
run/
  audit/
    audit.properties
    trace/
    console.log
  control/
    trace/
    console.log
~~~

The audit test must define this repository-root helper, construct both child commands from concrete run-local paths, and keep the shared workload arguments identical:

~~~java
private static Path repositoryRoot() {
    final Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    return workingDirectory.getFileName().toString().equals("forge-gui-desktop")
            ? workingDirectory.getParent() : workingDirectory;
}

final Path run = Files.createTempDirectory("frl02l2a-audit-");
final Path auditRoot = run.resolve("audit");
final Path controlRoot = run.resolve("control");
Files.createDirectories(auditRoot);
Files.createDirectories(controlRoot);
final Path auditOutput = auditRoot.resolve("audit.properties");
final Path auditTrace = auditRoot.resolve("trace");
final Path auditConsole = auditRoot.resolve("console.log");
final Path controlTrace = controlRoot.resolve("trace");
final Path controlConsole = controlRoot.resolve("console.log");
final List<String> commonWorkloadArgs = List.of(
        "-cp", System.getProperty("java.class.path"),
        "forge.view.Main", "sim", "-d", "Izzet Guild Kit", "Dimir Guild Kit",
        "-n", "10", "-s", "20260810", "-q");
final List<String> auditCommand = new ArrayList<>();
auditCommand.add(ChildJvmSupport.javaExecutable().toString());
auditCommand.add("-Dforge.surveil.partition.audit.enabled=true");
auditCommand.add("-Dforge.surveil.partition.audit.output=" + auditOutput);
auditCommand.add("-Dforge.determinism.traceDir=" + auditTrace);
auditCommand.add("-Dforge.determinism.auditRandom=true");
auditCommand.addAll(commonWorkloadArgs);
final List<String> controlCommand = new ArrayList<>();
controlCommand.add(ChildJvmSupport.javaExecutable().toString());
controlCommand.add("-Dforge.determinism.traceDir=" + controlTrace);
controlCommand.add("-Dforge.determinism.auditRandom=true");
controlCommand.addAll(commonWorkloadArgs);
~~~

Launch each command with the same working directory and these exact timeout/retention rules:

~~~java
private static int runChild(
        final List<String> command,
        final Path consoleOutput) throws Exception {
    final Process child = new ProcessBuilder(command)
            .directory(repositoryRoot().resolve("forge-gui").toFile())
            .redirectErrorStream(true)
            .redirectOutput(consoleOutput.toFile())
            .start();
    if (!child.waitFor(300, TimeUnit.SECONDS)) {
        child.destroyForcibly();
        throw new AssertionError("FRL-02L2A child timed out; see " + consoleOutput);
    }
    assertEquals(0, child.exitValue());
    return child.exitValue();
}

boolean passed = false;
try {
    runChild(auditCommand, auditConsole);
    runChild(controlCommand, controlConsole);
    assertEquals(hashTraceTree(auditTrace), hashTraceTree(controlTrace));
    // Parse auditOutput as UTF-8 Java properties and execute every exact key/value assertion specified below.
    passed = true;
} finally {
    if (passed) {
        try (Stream<Path> paths = Files.walk(run)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        }
    } else {
        System.err.println("FRL-02L2A audit artifacts retained at " + run);
    }
}
~~~

`runChild` must be called once for each fresh JVM. The audit and control commands must use the same decks, game count, seed, classpath, working directory, and `-Dforge.determinism.auditRandom=true`; they must use different trace paths. Only the audit command receives the two Surveil audit properties. The audit child must produce exactly one UTF-8 Java-properties artifact whose schema property is `FRL02L2A_SURVEIL_AUDIT_V1`. On timeout, non-zero exit, failed assertion, malformed output, or trace mismatch, retain and print the complete run tree; delete it only after the complete test passes.

Define `hashTraceTree` in the audit test so the comparison is independent of filesystem enumeration order and includes both relative paths and file bytes:

~~~java
private static String hashTraceTree(final Path root) throws Exception {
    final MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (Stream<Path> paths = Files.walk(root)) {
        final List<Path> files = paths
                .filter(Files::isRegularFile)
                .sorted(Comparator.comparing(path -> root.relativize(path).toString()))
                .toList();
        for (final Path file : files) {
            digest.update(root.relativize(file).toString().replace('\\', '/').getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(Files.readAllBytes(file));
            digest.update((byte) 0);
        }
    }
    return HexFormat.of().formatHex(digest.digest());
}
~~~

The artifact must contain exactly these required keys:

~~~text
schema
profile
workload_first_deck
workload_second_deck
games
seed
raw_arrange_for_surveil_invocations
capture_admission_failures
non_empty_sessions
n_bucket_0
n_bucket_1
n_bucket_2
n_bucket_ge3
native_callback_invocations
native_callback_failures
valid_partition_mappings
mapping_failures
membership_request_count
membership_result_count
candidate_count
forced_request_count
external_attempts
trace_incomplete_count
public_symmetry_conflicts
teacher_eligibility_not_applicable_count
teacher_eligibility_bc_eligible_count
teacher_eligibility_bc_excluded_public_symmetry_count
n2_graveyard_0_retained_2
n2_graveyard_1_retained_1
n2_graveyard_2_retained_0
~~~

Use the existing controlled workload:

~~~text
Izzet Guild Kit vs Dimir Guild Kit
10 games
seed 20260810
~~~

Record and assert:

~~~text
schema = FRL02L2A_SURVEIL_AUDIT_V1
profile = SURVEIL_PARTITION
workload_first_deck = Izzet Guild Kit
workload_second_deck = Dimir Guild Kit
games = 10
seed = 20260810
non_empty_sessions = 16
n_bucket_0 = 0
n_bucket_1 = 6
n_bucket_2 = 10
n_bucket_ge3 = 0
raw_arrange_for_surveil_invocations = 16
capture_admission_failures = 0
native_callback_invocations = 16
native_callback_failures = 0
valid_partition_mappings = 16
mapping_failures = 0
membership_request_count = 26
membership_result_count = 26
candidate_count = 52
forced_request_count = 0
trace_incomplete_count = 0
external_attempts = 0
teacher_eligibility_not_applicable_count = 26
teacher_eligibility_bc_eligible_count = 0
teacher_eligibility_bc_excluded_public_symmetry_count = 0
n2_graveyard_0_retained_2 = 5
n2_graveyard_1_retained_1 = 2
n2_graveyard_2_retained_0 = 3
~~~

Parse the properties file and assert every required key is present, the listed values match, and no forbidden raw Card/object/native fields occur in the artifact. `public_symmetry_conflicts` is required and must be a non-negative integer, but its workload value is diagnostic evidence rather than a hard-coded reachability target. Do not lock an unproven N=1 retained/graveyard distribution. Verify binary request counts from diagnostics without making them a separate canonical gate.

Run the newly created audit test with:

~~~powershell
mvn -pl forge-gui-desktop -am "-Dtest=FRL02L2ASurveilPartitionAuditTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
~~~

Expected: both fresh children exit 0, the audit properties assertions pass, and audit-enabled versus audit-disabled determinism trees are byte-for-byte equal.

- [ ] Step 4: Run the full module build after focused tests.

Run:

~~~powershell
mvn -pl forge-gui-desktop -am test
~~~

Expected: the required modules compile and their tests pass. If an unrelated pre-existing environment or port-binding flake occurs, preserve the exact failure output and distinguish it from Surveil failures.

---

### Task 8: Final implementation verification and handoff

**Files:**
- No new files beyond the approved implementation map.
- No card scripts.
- No unrelated production or test files.

- [ ] Step 1: Inspect the final changed-file set.

Run:

~~~powershell
git status
git diff --stat
git diff --check
~~~

Expected changed paths are limited to the approved production and test files. No DecisionType change, no generic CardSelection change, no PlayerControllerHuman/Ai change, and no L2B implementation may appear.

- [ ] Step 2: Verify the prohibited-data boundary.

Run a repository search over the Surveil implementation:

~~~powershell
rg -n "getCardId|getGameTimestamp|CardView|CardLKI|ownerId|controllerId|ZoneType|RNG|shuffle|arrangeForSurveil" forge-game/src/main/java/forge/game/decision/SurveilPartition* forge-game/src/main/java/forge/game/player/Player.java
~~~

Review every hit manually. Private native validation and the single Player.surveil callback are allowed; public projection, semantic keys, diagnostics, and trace fields must not contain those values. Native AI shuffle must remain in its existing implementation and must not be called by the new coordinator.

- [ ] Step 3: Verify trace semantics from serialized output.

Check that:

~~~text
all Surveil-bearing traces use DECISION_TRACE_V3
each request has exactly two legal candidates
each request is non-forced
request order follows canonical visible projection order
nativeCallbackCompleted=true means the shared parent callback completed
each successful request has exactly one CHOSEN result
mapping/callback failures have no partial membership request/result rows
current L2A emits NOT_APPLICABLE for native Human and native AI
current L2A records mixed public symmetry as diagnostic-only
BC_EXCLUDED_PUBLIC_SYMMETRY appears only after a separately approved parity gate
BC_ELIGIBLE is never emitted by current L2A
no V2 Surveil compatibility is fabricated
~~~

- [ ] Step 4: Re-run the exact final verification commands.

Run:

~~~powershell
git status
git diff --stat
git diff
git diff --check
~~~

Expected: no whitespace errors, no unapproved files, and no implementation of L2B or external ownership.

- [ ] Step 5: Handoff without push or PR.

Report:

~~~text
FRL-02L2A implementation complete
focused tests:
regression tests:
canonical workload:
trace version:
native callback count:
mapping failures:
trace incomplete:
external attempts:
changed files:
commit:
push:
PR:
~~~

Do not push or open a PR as part of this plan. A commit is a separate integration decision after the implementation and verification review.

## Spec coverage self-review

| Approved design requirement | Plan coverage |
| --- | --- |
| CARD_SELECTION, no new DecisionType | Tasks 2 and 7 |
| Binary GRAVEYARD/RETAIN domain | Tasks 2 and 3 |
| No synthetic click ordering | Tasks 3, 5, and 6 |
| Canonical chooser-visible traversal | Tasks 1, 3, 6, and 7 |
| Native order private | Tasks 1, 3, 4, and 6 |
| Deterministic opaque item IDs | Tasks 1 and 3 |
| Public symmetry/BC contract | Tasks 3, 5, and 6 |
| Observation parity fail-closed | Task 5 |
| One native callback | Task 4 |
| Post-callback sequential trace materialization | Task 5 |
| Capture-only failure isolation | Tasks 4 and 6 |
| PlayerController provider/coordinator ownership | Task 4 |
| Exact public context | Tasks 1, 2, and 3 |
| Native/public identity separation | Tasks 1, 2, 3, 4, and 6 |
| L2A to L2B identity continuity | Tasks 3, 4, and 6 |
| Replacement boundary | Task 4 and Task 6 |
| DECISION_TRACE_V3, no new version | Task 5 |
| Diagnostics | Tasks 4 and 7 |
| Active-session close/remove lifecycle | Tasks 3, 4, 5, and 6 |
| Null-safe snapshot and original mutable callback argument | Task 4 |
| One provider request/apply state machine for capture and future ownership | Tasks 3 and 5 |
| Fresh-JVM deterministic audit harness parity | Task 7 |
| Canonical 16/6/10 acceptance | Task 7 |
| No L2B or external resolver | Authority section and Tasks 4-8 |
| No card-script changes | Authority section and Tasks 7-8 |

## Plan self-review

- Placeholder scan: no unresolved placeholder token or unspecified implementation step is required.
- Type consistency: SurveilPartitionProfile, SurveilPartitionCandidateKind, SurveilPartitionCard, SurveilPartitionContext, SurveilPartitionItemId, SurveilPartitionSession, SurveilPartitionDecisionEnvelopeTest, and provider/coordinator ownership are used consistently across all tasks.
- Failure consistency: capture-only failures create diagnostics only; successful native mapping creates post-callback sequential CHOSEN rows; future external failures remain fail-closed.
- Identity consistency: native snapshot order is private; canonical projection order drives trace order; the invertible item-ID mixer derives equality handles only from canonical rank; duplicate-card assignment may indirectly follow the private tie-break, which is never public or a policy feature; L2B reuses the same item IDs.
- Trace consistency: every successful membership request has two candidates, is non-forced, uses V3 profile metadata, records nativeCallbackCompleted against the shared parent callback, and current L2A emits NOT_APPLICABLE for both native owners.
- Scope consistency: the plan changes no generic CardSelection semantics, no callback signatures, no native AI/Human behavior, no card scripts, and no retained-top ORDER.

Plan authoring itself does not execute any task above.
