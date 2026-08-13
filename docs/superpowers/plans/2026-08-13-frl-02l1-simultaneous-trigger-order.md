# FRL-02L1 Exact SIMULTANEOUS_TRIGGER_ORDER Implementation Plan

> For agentic workers: REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Implement the exact ForgeRL L1 SIMULTANEOUS_TRIGGER_ORDER decision profile with a deterministic public projection, controller-local native/external ownership, one native teacher callback, incremental n-1 external decisions, centralized LIFO translation, terminal V2 trace records, and a 19/19 exact-profile canonical acceptance gate. The raw shared callback surface remains 20, with one separately attributed player-owned copied-spell callback outside L1.

**Architecture:** MagicStack remains behaviorally unchanged. PlayerControllerAi remains a thin router and passes the existing AiController.orderPlaySa method as the native callback. A controller-local SimultaneousTriggerOrderDecisionProvider owns resolver state and deterministic counters. A typed SimultaneousTriggerOrderDecisionCoordinator owns exact admission, immutable session snapshots, request generation, native/external lifecycle, integrity enforcement, and the single semantic/native ordering translation. Public requests expose only value DTOs and typed enums; native Forge objects remain private coordinator state.

**Tech Stack:** Java 17, Maven reactor, TestNG, Forge game/AI modules, existing DecisionRequest/LegalCandidate/DeterminismTrace V2 APIs, PowerShell on Windows.

---

## Scope guard and implementation map

The approved design is the sole source of truth:

docs/superpowers/specs/2026-08-13-frl-02l1-simultaneous-trigger-order-design.md

The implementation must stay on the exact profile. It must not add generic
permutation ordering, generic DecisionType.ORDER behavior, combat ordering,
damage assignment, Surveil ordering, a MagicStack behavior change, text or
engine-object public fields, or ActionContinuation semantics.

Files to create:

- forge-game/src/main/java/forge/game/decision/OrderDirection.java
  - exact semantic direction value RESOLVE_FIRST.
- forge-game/src/main/java/forge/game/decision/OrderCandidateKind.java
  - exact candidate kind SELECT_RESOLVE_FIRST.
- forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderProfile.java
  - exact profile value SIMULTANEOUS_TRIGGER_ORDER.
- forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderItem.java
  - immutable value-only item projection with stable session-local ordinal,
    CardSelectionCard, TriggerType, and ApiType.
- forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderContext.java
  - immutable request context without remaining-item duplication or
    ActionContinuation.
- forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderDecisionProvider.java
  - controller-local resolver and deterministic request/session counters.
- forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderDecisionCoordinator.java
  - exact admission, lifecycle orchestration, trace ownership, integrity
    checks, and centralized semantic/native translation.
- forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderIntegrityException.java
  - sanitized hard-failure type/reason for malformed snapshots and duplicate
    native identities.
- forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderAuditDiagnostics.java
  - disabled-by-default, value-only canonical acceptance counters used by the
    dedicated audit test; it must not alter gameplay, RNG, or trace behavior.
- forge-gui-desktop/src/test/java/forge/game/decision/SimultaneousTriggerOrderPublicApiTest.java
  - public-contract/reflection and immutable DTO tests.
- forge-gui-desktop/src/test/java/forge/game/decision/SimultaneousTriggerOrderTraceTest.java
  - ORDER validator, terminal result, and trace lifecycle tests.
- forge-gui-desktop/src/test/java/forge/game/decision/SimultaneousTriggerOrderCoordinatorTest.java
  - pure translation, admission, native/external lifecycle, and failure tests.
- forge-gui-desktop/src/test/java/forge/view/FRL02L1SimultaneousTriggerOrderAuditTest.java
  - fresh-JVM canonical 116-call/19-session/26-request acceptance test plus
    explicit raw-20 and outside-L1 counters.

Files to modify:

- forge-game/src/main/java/forge/game/decision/DecisionType.java
  - add only ORDER.
- forge-game/src/main/java/forge/game/decision/LegalCandidate.java
  - add typed ORDER candidate state/factory/getters while retaining the
    value-only public surface.
- forge-game/src/main/java/forge/game/decision/DecisionRequest.java
  - add the ORDER context and exact profile/direction/candidate validation.
- forge-game/src/main/java/forge/game/decision/DecisionTraceResultKind.java
  - add INVALID_EXTERNAL_CANDIDATE and NATIVE_CALLBACK_FAILURE.
- forge-game/src/main/java/forge/game/decision/DecisionTraceTrainingValidator.java
  - accept ORDER external CHOSEN false/false and the two terminal ORDER
    failure kinds exactly as specified.
- forge-game/src/main/java/forge/game/decision/DeterminismTrace.java
  - expose terminal methods for those two failure kinds.
- forge-game/src/main/java/forge/game/player/PlayerController.java
  - own one provider and expose its resolver/provider accessors.
- forge-ai/src/main/java/forge/ai/PlayerControllerAi.java
  - route orderSimultaneousSa through the coordinator while preserving the
    existing native callback and stack insertion code.
- forge-gui-desktop/src/test/java/forge/game/decision/DecisionPublicApiReflectionTest.java
  - if this test is located in the game test source set in the checkout, add
    the new DTO/context types to its forbidden-engine-type coverage; otherwise
    extend the corresponding existing public API test in the desktop module.

Files explicitly out of scope:

- forge-game/src/main/java/forge/game/zone/MagicStack.java;
- Forge card scripts, forge-gui controllers, and unrelated decision profiles;
- AiController.orderPlaySa itself;
- TargetDecisionProvider, confirmation/target behavior, and existing native
  heuristics except for calling the existing fallback exactly as before;
- any combat ORDER, DAMAGE_ASSIGNMENT, or Surveil implementation;
- any raw SpellAbility, WrappedAbility, Card, CardLKI, GameObject, trigger,
  stack text, description, Java identity, or ActionContinuation in the public
  ORDER contract.

## Required API and invariants

Keep these names and meanings consistent across the plan, tests, and code:

~~~java
public enum OrderDirection {
    RESOLVE_FIRST
}

public enum OrderCandidateKind {
    SELECT_RESOLVE_FIRST
}

public enum SimultaneousTriggerOrderProfile {
    SIMULTANEOUS_TRIGGER_ORDER
}
~~~

SimultaneousTriggerOrderItem contains only:

~~~java
long itemId();                         // session-local deterministic ordinal
CardSelectionCard source();            // visible face-up source projection
TriggerType triggerType();
ApiType effectApi();
~~~

The item ID is allocated once from the immutable initial snapshot and is
stable through every step. Two distinct native entries may have identical
source, triggerType, and effectApi; they receive different IDs. The same
native entry identity appearing twice is a hard
SESSION_INTEGRITY_FAILURE.

SimultaneousTriggerOrderContext contains:

~~~text
profile = SIMULTANEOUS_TRIGGER_ORDER
direction = RESOLVE_FIRST
orderSessionId                  // deterministic controller-local counter
stepIndex
originalItemCount
choosingPlayerId
decisionSequenceId = null
subdecisionIndex = null
~~~

It does not contain remainingItems; DecisionRequest.candidates is the
authoritative complete remaining set. It does not contain an ActionContinuation,
and it does not create one.

Every admitted request has:

~~~text
DecisionType.ORDER
profile SIMULTANEOUS_TRIGGER_ORDER
direction RESOLVE_FIRST
forced false
exactly one SELECT_RESOLVE_FIRST candidate per remaining item
candidate count == remaining item count >= 2
semanticKey == RESOLVE_FIRST|<itemId>
~~~

The public request never exposes a native ability, trigger, card, text, or
identity. The resolver receives DecisionRequest and returns a typed
LegalCandidate; the coordinator validates membership against the current
request before applying it.

effectiveOrderingPlayer(entry) must mirror MagicStack exactly:

~~~text
p = entry.getActivatingPlayer()
if p == null: p = entry.getHostCard().getController()
~~~

All admitted entries must resolve to the choosing player. The v0 admission
also requires a non-static WrappedAbility, a non-null visible face-up source,
and a complete public projection. A continuation-active call is unsupported
for this profile.

## Task 1: Verify checkpoint and write public-contract RED tests

Files: create/modify the public API test files listed above.

- [ ] Confirm C:\forgeAI is clean and remains on the requested base, and
      confirm the isolated branch is frl/02l1-simultaneous-trigger-order.
- [ ] Add tests that require DecisionType.ORDER, the three exact enums, and
      the item/context constructors/getters.
- [ ] Assert item IDs are positive/session-local ordinals and that two
      duplicate-looking items remain distinct by ID while their public values
      may otherwise match.
- [ ] Assert the context exposes the two null continuation fields and no
      remaining-item duplicate field.
- [ ] Extend public reflection coverage so the new public types cannot expose
      SpellAbility, WrappedAbility, Card, Game, GameObject, trigger,
      text/description, Java identity, or ActionContinuation.
- [ ] Add request/candidate construction assertions for one
      SELECT_RESOLVE_FIRST candidate per item, unique semantic keys, and
      ORDER-only context validation.

Run the narrow test before adding production types and record the expected
compile failure:

~~~powershell
mvn -pl forge-gui-desktop -am -Dtest=SimultaneousTriggerOrderPublicApiTest '-Dsurefire.failIfNoSpecifiedTests=false' test
~~~

Do not weaken the test to make the pre-implementation run pass.

## Task 2: Write trace/validator RED tests before trace implementation

Files: create SimultaneousTriggerOrderTraceTest.java, then modify the trace
validator tests only as needed.

- [ ] Add an external ORDER CHOSEN record with false/false; require
      isHistoryValid() == true, isBCPolicySample() == false, and a valid
      selected semantic key.
- [ ] Add a native ORDER CHOSEN record with true/true; require valid history
      and BC policy sample.
- [ ] Add INVALID_EXTERNAL_CANDIDATE with empty selected key and false/false;
      require valid history and no BC sample.
- [ ] Add NATIVE_CALLBACK_FAILURE with empty selected key and false/false;
      require valid history and no BC sample.
- [ ] Preserve MAPPING_FAILED as empty key, true/true, valid history, and no
      BC sample.
- [ ] Require every emitted request to have exactly one terminal result and
      reject TRACE_INCOMPLETE for the new failure paths.

Run the focused tests and observe RED due to the absent result kinds and ORDER
allowlist:

~~~powershell
mvn -pl forge-gui-desktop -am -Dtest=SimultaneousTriggerOrderTraceTest '-Dsurefire.failIfNoSpecifiedTests=false' test
~~~

## Task 3: Write pure LIFO translation RED tests

Files: SimultaneousTriggerOrderCoordinatorTest.java.

- [ ] Test toSemanticResolveFirst and toNativeInsertion for all canonical sizes
      n=2, n=3, and n=4.
- [ ] Test the exact relationship native insertion -> semantic resolve-first
      and the round trip:

~~~text
toNativeInsertion(toSemanticResolveFirst(nativeOrder)) == nativeOrder
~~~

- [ ] Include duplicate-looking projections backed by distinct native entry
      identities; translation must use private snapshot identity/ordinal, not
      public text or public-value equality.
- [ ] Test that translation is the only place where reversal occurs; the
      coordinator must not expose a second ad hoc reverse operation.

The test may use a package-private test representation or coordinator helper,
but public APIs must remain value-only.

## Task 4: Write admission, ownership, lifecycle, and failure RED tests

Files: SimultaneousTriggerOrderCoordinatorTest.java and supporting fixtures.

- [ ] Cover null input, n=0, and n=1 separately. With no resolver, require
      literal delegation to the supplied native callback; with a resolver,
      require zero resolver/native callbacks for empty/singleton inputs and
      unchanged empty/sole return behavior.
- [ ] Cover n>=2 admitted v0 groups with same effective ordering player.
- [ ] Cover an activator-null entry whose host controller determines the
      effective player, matching the MagicStack rule.
- [ ] Reject mixed effective players and non-wrapper/static/hidden-source or
      incomplete-projection groups as UNSUPPORTED_ADMISSION.
- [ ] Assert unsupported + resolver-null invokes the existing native callback
      exactly as supplied, preserving the same input object, mutation, result,
      RNG/heuristic behavior, and exception behavior; it does not emit L1
      requests.
- [ ] Assert unsupported + resolver-present is sanitized hard failure with
      resolver/native counts zero and no stack insertion.
- [ ] Assert duplicate native identity is SESSION_INTEGRITY_FAILURE, never
      native fallback even when resolver is null, with resolver/native counts
      zero and no insertion.
- [ ] Assert deterministic orderSessionId and request IDs advance by local
      counters only; no UUID/time/RNG source is permitted.
- [ ] Assert a resolver is captured once at session start and subsequent provider
      mutation cannot change ownership mid-session.

## Task 5: Add typed public DTOs/enums and make public-contract tests GREEN

Files:

- forge-game/src/main/java/forge/game/decision/OrderDirection.java
- forge-game/src/main/java/forge/game/decision/OrderCandidateKind.java
- forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderProfile.java
- forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderItem.java
- forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderContext.java

- [ ] Implement immutable constructors and defensive copies where lists are
      present. Use existing CardSelectionCard, TriggerType, and ApiType value
      types; do not retain raw native entry references in these public DTOs.
- [ ] Validate positive IDs, non-null public values, non-negative step/counts,
      deterministic session ID, and null continuation identifiers.
- [ ] Implement stable value equality/hash behavior without using Java object
      identity as public identity. The item ID is the session-local identity.
- [ ] Keep package-private construction where it prevents callers from
      manufacturing an invalid engine snapshot; retain public read access only
      to the approved values.

Run the public API test and the existing reflection suite; both must pass
before proceeding:

~~~powershell
mvn -pl forge-gui-desktop -am -Dtest=SimultaneousTriggerOrderPublicApiTest,DecisionPublicApiReflectionTest '-Dsurefire.failIfNoSpecifiedTests=false' test
~~~

## Task 6: Add ORDER request/candidate and terminal trace support

Files:

- forge-game/src/main/java/forge/game/decision/DecisionType.java
- forge-game/src/main/java/forge/game/decision/LegalCandidate.java
- forge-game/src/main/java/forge/game/decision/DecisionRequest.java
- forge-game/src/main/java/forge/game/decision/DecisionTraceResultKind.java
- forge-game/src/main/java/forge/game/decision/DecisionTraceTrainingValidator.java
- forge-game/src/main/java/forge/game/decision/DeterminismTrace.java

- [ ] Add DecisionType.ORDER without changing generic decision semantics.
- [ ] Add typed ORDER fields to LegalCandidate, a package-safe factory for
      SELECT_RESOLVE_FIRST, and getters returning only the approved kind/item.
      Reject mixing ORDER fields with other candidate families.
- [ ] Add ORDER context storage/getter and validation to DecisionRequest.
      Require exact profile/direction, no continuation, non-null choosing player
      ID, unique semantic keys, candidate count equal to the remaining snapshot,
      and at least two candidates. Reject ORDER context on other decision types.
- [ ] Add the two exact terminal result kinds.
- [ ] Update DecisionTraceTrainingValidator so ORDER external CHOSEN
      (false/false) is valid history but not BC; ORDER native CHOSEN
      (true/true) is valid history and BC; both new failure kinds are valid
      history with empty key and false/false; MAPPING_FAILED retains its
      existing true/true meaning.
- [ ] Add RequestHandle.recordInvalidExternalCandidate() and
      recordNativeCallbackFailure() using one terminal complete(...) call.
      They must not create TRACE_INCOMPLETE or close any subsequent request.

Run the trace RED suite again and make it GREEN, then run existing V2 trace
and validator tests:

~~~powershell
mvn -pl forge-gui-desktop -am -Dtest=SimultaneousTriggerOrderTraceTest,DeterminismTraceV2Test '-Dsurefire.failIfNoSpecifiedTests=false' test
~~~

## Task 7: Add controller-local provider and coordinator, then make lifecycle tests GREEN

Files:

- forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderDecisionProvider.java
- forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderIntegrityException.java
- forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderDecisionCoordinator.java
- forge-game/src/main/java/forge/game/player/PlayerController.java

Provider contract:

~~~java
@FunctionalInterface
public interface Resolver {
    LegalCandidate resolve(DecisionRequest request);
}

public void setResolver(Resolver resolver);
public Resolver getResolver();
public boolean hasResolver();
~~~

The provider captures the resolver once per generated strategic session and
keeps deterministic controller-local request/session counters. Counters must
not use static state, UUIDs, wall clock, identity hashes, or RNG.

Coordinator contract:

~~~java
@FunctionalInterface
public interface NativeOrderer {
    List<SpellAbility> order(List<SpellAbility> input);
}

public List<SpellAbility> order(
        List<SpellAbility> active,
        Player chooser,
        SimultaneousTriggerOrderDecisionProvider provider,
        NativeOrderer nativeOrderer);
~~~

- [ ] Implement a private immutable initial snapshot containing each native
      entry and its projected item, with an IdentityHashMap duplicate guard.
- [ ] Implement exact effectiveOrderingPlayer fallback semantics and v0
      admission; keep unsupported admission distinct from integrity failure.
- [ ] Implement null/empty/singleton pre-admission exactly as the Spec.
- [ ] Delegate unsupported resolver-null calls directly to the supplied native
      orderer with the original list. Do not return a copied original input.
- [ ] On resolver-present unsupported admission, hard-fail sanitized with no
      resolver/native callback and no stack insertion path.
- [ ] On integrity failure, hard-fail regardless of resolver ownership and never
      use native fallback.
- [ ] Build the step-0 request from the complete immutable snapshot before the
      one native callback. Native callback exceptions terminalize step 0 as
      NATIVE_CALLBACK_FAILURE (false/false).
- [ ] Validate the complete native permutation before publishing any teacher
      labels. If invalid, terminalize only step 0 as MAPPING_FAILED
      (true/true), create no subsequent requests, and do not insert a result.
- [ ] Translate the native insertion order to semantic RESOLVE_FIRST once
      through the central pure helper; complete step 0 and only then create
      step 1 through n-2. Do not prebuild future remaining sets.
- [ ] Implement external mode as exactly n-1 requests. Validate returned
      candidate kind, semantic key, item ID, and current request membership.
      Invalid/stale/foreign/null/throwing resolver outcomes terminalize the
      active request as INVALID_EXTERNAL_CANDIDATE (false/false) and never
      fall back.
- [ ] Ensure every emitted request receives exactly one terminal record. The
      final remaining item is forced and has no request.
- [ ] Use a single toSemanticResolveFirst(...) / toNativeInsertion(...)
      implementation for all conversion; add package-level round-trip tests.

Run the coordinator tests and the public API/trace tests together before
integrating the AI callback:

~~~powershell
mvn -pl forge-gui-desktop -am -Dtest=SimultaneousTriggerOrderCoordinatorTest,SimultaneousTriggerOrderPublicApiTest,SimultaneousTriggerOrderTraceTest '-Dsurefire.failIfNoSpecifiedTests=false' test
~~~

## Task 8: Route PlayerControllerAi without changing MagicStack

File: forge-ai/src/main/java/forge/ai/PlayerControllerAi.java.

- [ ] Replace only the current orderSimultaneousSa body with the coordinator
      call, passing the current player, the controller-local provider, and
      getAi()::orderPlaySa as the existing native callback.
- [ ] Keep MagicStack.java unchanged and keep orderAndPlaySimultaneousSa
      target/copy/insertion behavior unchanged.
- [ ] Preserve the resolver-null native path in ownership and callback semantics:
      the coordinator must invoke AiController.orderPlaySa for unsupported/no-
      policy calls, including its input mutation, RNG, heuristic result, and
      exception behavior.
- [ ] Do not route CONFIRMATION, TARGET, PAYMENT, combat order, or any
      unrelated AI callback through the new coordinator.

Run existing PlayerControllerAi/decision regression suites and the focused
integration workload:

~~~powershell
mvn -pl forge-gui-desktop -am -Dtest=SimulateMatchDeterminismTest,FRL02KTriggeredTargetProviderAuditTest,GelectrodeConfirmationDecisionProviderTest '-Dsurefire.failIfNoSpecifiedTests=false' test
~~~

## Task 9: Add deterministic profile-separated audit counters and the 19/19 RED-to-GREEN gate

Files:

- forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderAuditDiagnostics.java
- forge-gui-desktop/src/test/java/forge/view/FRL02L1SimultaneousTriggerOrderAuditTest.java

- [ ] Add an opt-in property such as
      forge.simultaneousTriggerOrder.auditFile; disabled behavior must have
      no file I/O and no gameplay/RNG changes.
- [ ] Record only sanitized value counters: total raw calls, cardinality
      buckets, raw multi-item callbacks, exact
      `SIMULTANEOUS_TRIGGER_ORDER` profile sessions, admitted exact-profile
      sessions, non-L1 multi-item callbacks, request candidate-size buckets,
      forced count, L1 unsupported fallbacks, outside-L1 native fallbacks,
      integrity failures, trace-incomplete count, and terminal failure counts.
      Do not record card names, stack text, Java identity, or engine object
      serialization.
- [ ] Launch the existing deterministic Forge headless workload in a fresh
      JVM, enable only the audit property and decision trace output, and parse
      the resulting value-only files.
- [ ] Assert exactly:
~~~text
orderSimultaneousSa total = 116
n=1 = 96
n=2 = 14
n=3 = 5
n=4 = 1
raw multi-item callbacks = 20
SIMULTANEOUS_TRIGGER_ORDER profile sessions = 19
admitted SIMULTANEOUS_TRIGGER_ORDER sessions = 19
non-L1 player-owned copy-spell callbacks = 1
ORDER requests = 26
candidate size 2 = 19
candidate size 3 = 6
candidate size 4 = 1
forced requests = 0
l1 unsupported fallback = 0
outside-L1 native fallback = 1
~~~
- [ ] Assert that no Surveil/combat/DAMAGE_ASSIGNMENT workload is admitted by
      this exact profile and that the known copied-spell callback is classified
      outside L1 rather than engine-owned.
- [ ] Keep the acceptance failure explicit: any admitted count below 19 exact
      L1 sessions is `FRL_02L1_PARTIAL`, not a pass. The outside-L1 callback is
      not an L1 completeness failure.

Run the focused audit command. On Windows quote Maven properties containing
equal signs:

~~~powershell
mvn -pl forge-gui-desktop -am -Dtest=FRL02L1SimultaneousTriggerOrderAuditTest '-Dsurefire.failIfNoSpecifiedTests=false' test
~~~

## Task 10: Documentation, self-review, and verification

Files: the approved Spec, this plan, implementation files, and tests.

- [ ] Change the Spec verdict to DESIGN_APPROVED with P0 = 0, P1 = 0;
      keep implementation status separate from design approval.
- [ ] Run a plan-to-Spec checklist covering every mandatory section:
      exact admission/effective player, projection, duplicate semantics,
      cardinality, native fallback ownership, session ID determinism, native
      one-call lifecycle, incremental trace, external n-1, RESOLVE_FIRST
      conversion/round trip, terminal failure kinds, validator semantics,
      no ActionContinuation, canonical raw-20 versus exact-19/26 lock, and
      scope exclusions.
- [ ] Scan the plan for unresolved placeholders and remove any accidental
      matches before committing. The command excludes its own scan expression:

~~~powershell
$hits = rg -n "TODO|TBD|FIXME|<placeholder>|TBC" docs/superpowers/plans/2026-08-13-frl-02l1-simultaneous-trigger-order.md
$hits | Where-Object { $_ -notmatch 'rg -n' }
~~~

- [ ] Run git diff --check and inspect the complete diff for scope drift.
- [ ] Run the focused suites from Tasks 2, 7, 8, and 9.
- [ ] Run the complete requested reactor suite:

~~~powershell
mvn -pl forge-gui-desktop -am test
~~~

- [ ] Record actual test counts, failures, errors, skips, and any timeout or
      unavailable runtime evidence; do not convert unknowns into passes.
- [ ] Commit the Spec status/plan separately from implementation if practical,
      then commit implementation/tests only after the green verification.
- [ ] Push the implementation branch and confirm PR #22 is still Draft. Do not
      merge, mark ready, or change the PR base.

## Plan self-check against the approved Spec

| Spec gate | Plan coverage |
|---|---|
| Exact profile only; no generic ORDER | Tasks 5-8 add typed ORDER only and reject unrelated contexts |
| effectiveOrderingPlayer mirrors MagicStack | Tasks 4 and 7 test activator then host-controller fallback |
| Value-only public projection | Tasks 1 and 5 reflection/DTO checks |
| Duplicate-looking entries valid; duplicate native identity hard-fails | Tasks 1, 3, 4, and 7 |
| Candidates are authoritative remaining set | Tasks 1, 6, and 7 request validation |
| Deterministic orderSessionId, no ActionContinuation | Tasks 1, 4, 5, and 7 |
| Resolver ownership/fallback matrix | Tasks 4 and 7 |
| Null/0/1 pre-admission | Task 4 and coordinator implementation |
| Native callback exactly once | Tasks 4, 7, and 8 |
| Native step-0 trace then incremental subsequent requests | Task 7 |
| Native mapping failure atomicity | Tasks 2, 4, and 7 |
| External n-1 and invalidity terminalization | Tasks 2, 4, and 7 |
| Central LIFO translation plus round trip | Task 3 and Task 7 |
| ORDER validator exact semantics | Task 2 and Task 6 |
| Canonical raw-20 / exact-19/19 / 26-request lock | Task 9 |
| MagicStack unchanged and scope exclusions | Tasks 8 and 10 |

No plan step silently substitutes native fallback for an excluded strategic
session, no future request is created before its remaining set exists, and no
implementation task broadens the public contract beyond the approved values.

## Expected completion state

The Spec is DESIGN_APPROVED; the plan is committed at the requested path; the
implementation is test-first and limited to the exact profile; focused and full
tests report their actual results; the canonical audit is 19/19 exact L1
sessions with 26 requests while retaining 20 raw multi-item callbacks and one
outside-L1 copied-spell callback; and PR #22 remains Draft.

## Implementation checkpoint — 2026-08-13

The corrected authority gate is complete for the exact
`SIMULTANEOUS_TRIGGER_ORDER` profile. The fresh-JVM workload confirms the raw
distribution exactly:

~~~text
orderSimultaneousSa total = 116
n=1 = 96
n=2 = 14
n=3 = 5
n=4 = 1
~~~

The raw workload and corrected exact-profile admission produce:

~~~text
raw multi-item callbacks = 20
SIMULTANEOUS_TRIGGER_ORDER profile sessions = 19
admitted SIMULTANEOUS_TRIGGER_ORDER sessions = 19
ORDER requests = 26
candidate size 2 = 19
candidate size 3 = 6
candidate size 4 = 1
l1 unsupported native fallbacks = 0
outside-L1 native fallbacks = 1
~~~

The one outside-L1 call contains a `SpellApiBased` non-trigger from the
separate copy-spell ordering path. FRL-02L1R2 proves that this is a player-owned
semantic seam, not an engine-owned callback and not a missing L1 admission. It
therefore remains on the resolver-null native fallback while the separate
profile is unimplemented. The corrected L1 gate measures 19 exact sessions and
26 requests; the copy-spell design checkpoint remains open.

The resulting milestone status is:

```text
FRL_02L1_PASS
ORDER_V0_COMPLETE = false
```

The next design checkpoint is:

```text
FRL-02L1C DESIGN_COPY_SPELL_RESOLVE_FIRST_ORDER
```

It is not part of this implementation plan and must not be started
automatically.
