# FRL-02L1C Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the exact `COPY_SPELL_RESOLVE_FIRST_ORDER` profile from the approved FRL-02L1C design. The profile owns only the exact copied-spell batch produced by `CopySpellAbilityEffect`, records a deterministic public ORDER contract, preserves the existing L1 contract, and proves the canonical real-engine path without absorbing TARGET ownership.

**Architecture:** One resolver-independent `OrderProfileRouter` classifies the callback shape before strict admission. Exact L1 remains owned by the existing coordinator and provider; exact L1C has its own typed value objects, provider, controller-local counters, and coordinator. `RESOLVE_FIRST` is translated by one pure reversal helper. Native and external L1C lifecycles share the typed request/trace contract but never share resolver state or IDs. BC eligibility is persisted as typed request metadata in wholly versioned `DECISION_TRACE_V3` traces; L1-only traces remain byte-compatible `DECISION_TRACE_V2`.

**Tech Stack:** Java in the Forge multi-module Maven build; `forge-game` decision contracts and engine boundaries; `forge-ai` controller routing; `forge-gui-desktop` TestNG/Surefire unit, integration, and fresh-child-JVM audit tests; deterministic trace and properties-file assertions.

---

## Authority and scope gate

The implementation worker must start from commit `d018e87c1429cbe2c36178d587625b66e54acea0` in the isolated planning/implementation worktree and read the complete authority document:

    docs/superpowers/specs/2026-08-13-frl-02l1c-copy-spell-resolve-first-order-design.md

That document is the only authority for implementation scope. The current source and test files named below are inspection anchors and regression contracts; they must not be used to broaden the profile beyond the design. The protected checkout `C:\forgeAI` is read-only for this work. The plan author has verified the authority commit and the complete 1,008-line design document from the isolated checkout; an implementation worker must repeat the commit and clean-worktree checks before changing code.

This plan does not authorize implementation in the planning step. It is limited to the files and tests in the responsibility map below. It does not authorize changes to copied-spell TARGET ownership, a generic ORDER framework, legacy rules/decks, RNG/state/continuation behavior, or external publication.

## Locked semantic contracts

### Exact profile and lifecycle

`COPY_SPELL_RESOLVE_FIRST_ORDER` owns only the exact copied-spell batch produced by `CopySpellAbilityEffect`. Its semantic sequence is:

    ORDER -> per-copy TARGET setup -> MagicStack insertion -> resolution

ORDER means which copied spell resolves first. It is not damage assignment, target selection, arbitrary `SpellAbility` permutation, copied activated-ability ordering, or a generic list-order API. The callback-supplied list is the complete legal domain.

The L1C coordinator ends when it returns the valid native insertion list. It does not call `chooseTargetsFor`, `setupTargets`, `setupNewTargets`, a target provider, `MagicStack.add`, or resolution code.

### Closed router ownership

The router classifies before strict L1C admission and without consulting resolver presence:

* `L1_EXACT`: the existing exact simultaneous-trigger predicate succeeds.
* `COPY_SPELL_FAMILY_INTENT`: `active != null`, `active.size() >= 2`, and every entry is non-null, `entry.isSpell()`, `entry.isCopied()`, `entry.getHost() != null`, and `entry.getHost().isCopiedSpell()`. This pre-classifier does not inspect `SpellApiBased`, source lineage, API, players, visibility, `getCastSA()`, targets, or resolver presence.
* `UNOWNED_OTHER`: every other shape, including null input, n=0/n=1, null entries, copied non-spells, mixed shapes that do not satisfy the family predicate, and unrelated `SpellApiBased` input. Inspection exceptions fail to `UNOWNED_OTHER`.

Only family intent proceeds to strict admission. Family intent plus strict admission success is exact L1C; family intent plus strict admission failure is `MALFORMED_L1C_INTENT`; no family intent is `UNOWNED_OTHER`.

With an active L1C resolver, `MALFORMED_L1C_INTENT` fails closed without native fallback. Without an L1C resolver it follows native compatibility. `UNOWNED_OTHER` never consults the L1C resolver. An active L1 resolver cannot claim L1C, and an active L1C resolver cannot hard-fail L1 or unowned input.

### Provider and identity isolation

Do not create `OrderDecisionIdAuthority.java`. Keep the existing L1 provider-local `nextRequestId` and `nextOrderSessionId` counters unchanged. Give L1C its own provider-local monotonic counters. Do not require globally unique `DecisionRequest` IDs. Public/session correlation is `(profile, sessionId, stepIndex)`; persisted trace joins use `traceRequestIndex` plus profile/stage.

At L1C session creation, capture an immutable native snapshot, session-local ordinals `itemId = 1..n`, public projections, and a private `IdentityHashMap` from each native copied spell to its item. Duplicate public projections remain distinct candidates. Repeated native identity is `SESSION_INTEGRITY_FAILURE`. Native identity, `equals`, public text, IDs, targets, RNG, and wall-clock values are never used as public identity.

### Public projection and request validation

The public L1C projection contains only:

* session-local ordinal `itemId`;
* visible original-source name;
* `ApiType`;
* typed `COPIED_SPELL` marker.

It must not expose copied-host `CardSelectionCard`, `SpellAbility`, `SpellApiBased`, native `Card`, `CardLKI`, `GameObject`, native IDs, copied provenance, target objects, descriptions, stack text, hidden information, or arbitrary string output.

`DecisionRequest` and `LegalCandidate` must be profile-discriminated. L1 requests contain only `SimultaneousTriggerOrderContext` and `SimultaneousTriggerOrderItem`; L1C requests contain only the new L1C context/item types. Both use `DecisionType.ORDER`, `OrderDirection.RESOLVE_FIRST`, typed `SELECT_RESOLVE_FIRST` candidates, a zero-based step index, the original item count, and the stable callback receiver player ID. No one-candidate request is emitted; successful sessions emit exactly n-1 requests and force the final item internally.

### Trace and BC contract

Add `DecisionTraceTeacherLabelEligibility` with at least `NOT_APPLICABLE`, `BC_ELIGIBLE`, and `BC_EXCLUDED_PUBLIC_SYMMETRY`. `DecisionTraceRequestRecord` carries the typed eligibility and the closed profile discriminator. The L1C coordinator computes eligibility at request creation from the captured public projections using exactly `(visibleSourceName, effectApi, kind)` after removing `itemId`. It must not be reconstructed from `RESOLVE_FIRST|<itemId>` or candidate-set hashes.

`DecisionTraceTrainingValidator.isBCPolicySample(...)` requires `BC_ELIGIBLE` in addition to the current native-completed, mapping-attempted, `CHOSEN`, non-forced, and selected-key checks. Missing or malformed L1C eligibility fails closed to BC=false. Native C records remain history; symmetric requests are excluded only from BC.

Legacy deserialization is explicitly profile-aware. An existing `DECISION_TRACE_V2` L1 ORDER REQUEST with no persisted eligibility field is normalized in memory to `BC_ELIGIBLE`, preserving the historical L1 teacher semantics; a V2 non-ORDER request is normalized to `NOT_APPLICABLE`. This compatibility default applies only to exact L1 V2 records. It must never make a V3 L1C request eligible when its typed eligibility is missing, unknown, or malformed: a V3 L1C request fails closed to BC=false.

L1-only files remain byte-compatible `DECISION_TRACE_V2`. Any file containing an L1C request is wholly `DECISION_TRACE_V3`; V2 and V3 records are never mixed. The V3 REQUEST retains all V2 fields and appends the typed profile and eligibility. The paired RESULT preserves the selected-key and lifecycle flags. `DeterminismTrace` buffers structured request/result records in memory until `finish()`, determines whether any L1C request occurred, and only then serializes the complete file as V2 or V3. It must never emit a V2 prefix before a later L1C request is known.

### TARGET and diagnostics boundary

Do not modify `CopySpellAbilityEffect.java`, `forge-game/src/main/java/forge/game/zone/MagicStack.java`, `PlayerControllerHuman.java`, `AiController.java`, or the existing target engine path unless source verification proves the design impossible. The required engine route remains:

    PlayerControllerAi -> CopySpellAbilityEffect -> orderAndPlaySimultaneousSa
    -> per-copy TARGET setup -> MagicStack -> resolution

Diagnostics are value-only and disabled by default. Raw callback accounting occurs once at the router boundary. The versioned diagnostic contract uses explicit `raw.*`, `l1.*`, and `l1c.*` namespaces; legacy V2 keys are not silently redefined.

## Exact file-responsibility map

This map is the implementation boundary. Every file is listed with its planned responsibility before task decomposition.

### New production files

* `forge-game/src/main/java/forge/game/decision/OrderProfileRouter.java` — resolver-independent raw accounting and closed L1/L1C/unowned classification; the only ownership dispatcher.
* `forge-game/src/main/java/forge/game/decision/OrderResolutionTranslation.java` — pure copied-list reversal for semantic resolve-first versus native insertion order, shared by L1 and L1C.
* `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderProfile.java` — the one exact L1C profile value `COPY_SPELL_RESOLVE_FIRST_ORDER`.
* `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderItemKind.java` — the typed public marker `COPIED_SPELL`; no stringly typed kind.
* `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderSourceProjection.java` — immutable visible original-source name value, with no native object or copied-host identity.
* `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderItem.java` — immutable public item containing only ordinal, source projection, API value, and typed kind.
* `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderContext.java` — immutable L1C ORDER context containing profile, `RESOLVE_FIRST`, session ID, step index, original count, and callback receiver player ID.
* `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderDecisionProvider.java` — L1C resolver capture and provider-local request/session counters.
* `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderDecisionCoordinator.java` — strict admission, identity snapshot, native/external lifecycle, request construction, trace completion, and native insertion-list return.
* `forge-game/src/main/java/forge/game/decision/DecisionTraceTeacherLabelEligibility.java` — typed request-scoped BC eligibility enum.

### Modified production files

* `forge-ai/src/main/java/forge/ai/PlayerControllerAi.java` — route `orderSimultaneousSa` through the thin dispatcher and preserve `orderAndPlaySimultaneousSa` target/stack loop.
* `forge-game/src/main/java/forge/game/player/PlayerController.java` — expose only the separate L1C provider/resolver surface; retain the existing L1 accessors and abstract callbacks.
* `forge-game/src/main/java/forge/game/decision/DecisionRequest.java` — add a typed L1C ORDER context slot and profile-discriminated validation without weakening L1 validation.
* `forge-game/src/main/java/forge/game/decision/LegalCandidate.java` — add typed L1C candidate payload/factory while preserving the existing L1 payload/factory.
* `forge-game/src/main/java/forge/game/decision/DecisionTraceRequestRecord.java` — persist the closed typed profile discriminator and `DecisionTraceTeacherLabelEligibility`.
* `forge-game/src/main/java/forge/game/decision/DecisionTraceTrainingValidator.java` — require `BC_ELIGIBLE` for BC samples and fail closed for missing/malformed L1C metadata.
* `forge-game/src/main/java/forge/game/decision/DeterminismTrace.java` — buffer structured REQUEST/RESULT records until `finish()`, select V2/V3 for the complete file, serialize V3 REQUEST metadata, and retain existing RESULT fields.
* `forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderDecisionCoordinator.java` — minimal L1-only changes for shared reversal/accounting/trace eligibility; no L1C admission or resolver ownership.
* `forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderAuditDiagnostics.java` — explicitly version and namespace raw/L1/L1C counters without repurposing historical V2 names.

### New verification files

* `forge-gui-desktop/src/test/java/forge/game/decision/CopySpellResolveFirstOrderPublicApiTest.java` — reflection and value-contract tests for profile/context/item/source projection/kind/resolver.
* `forge-gui-desktop/src/test/java/forge/game/decision/CopySpellResolveFirstOrderTraceTest.java` — request lifecycle, trace joins, native/external labels, and trace-incomplete checks.
* `forge-gui-desktop/src/test/java/forge/game/decision/DecisionTraceV3Test.java` — V2 byte compatibility, wholly V3 C-bearing files, V3 REQUEST metadata, and fail-closed parsing.
* `forge-gui-desktop/src/test/java/forge/game/decision/CopySpellResolveFirstOrderCoordinatorTest.java` — n=2/n=3/n=4 native/external sessions and all coordinator failure semantics.
* `forge-gui-desktop/src/test/java/forge/game/decision/OrderProfileRouterTest.java` — pure pre-classifier, malformed versus unowned ownership, resolver isolation, raw accounting, and provider counters.
* `forge-gui-desktop/src/test/java/forge/game/decision/CopySpellResolveFirstOrderEngineIntegrationTest.java` — real `CopySpellAbilityEffect` to target setup, stack, and resolution route.
* `forge-gui-desktop/src/test/java/forge/view/FRL02L1CCopySpellResolveFirstOrderAuditTest.java` — fresh-child canonical 10-game audit, split diagnostics, V3 trace, and audit-on/off hash equality.

### Existing files explicitly inspected but not changed by this milestone

* `forge-game/src/main/java/forge/game/ability/effects/CopySpellAbilityEffect.java` — source of the exact copied-spell batch and the call into `orderAndPlaySimultaneousSa`.
* `forge-game/src/main/java/forge/game/card/CardFactory.java` — source of copied-host lineage and `getCastSA()` identity evidence.
* `forge-game/src/main/java/forge/game/zone/MagicStack.java` — actual stack insertion path and add-first LIFO semantics.
* `forge-game/src/main/java/forge/game/player/PlayerControllerHuman.java` — native UI ordering remains unchanged.
* `forge-ai/src/main/java/forge/ai/AI.java` and `AiController.java` — native teacher callback and target behavior remain outside L1C ownership.
* `forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderDecisionProvider.java` — existing L1 provider and its local counters remain unchanged.
* `forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderProfile.java` — existing L1 profile remains exact and separate.
* `forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderIntegrityException.java` — existing reasons are reused; no new result kind is invented.

`forge-game/src/main/java/forge/game/zone/MagicStack.java` is the correct source path. The obsolete `forge-game/.../stack/MagicStack.java` path is not used.

`OrderDecisionIdAuthority.java` is explicitly not created.

## Implementation tasks

### Task 1 — Verify the authority checkpoint and establish an isolated baseline

Anchors: repository HEAD; `docs/superpowers/specs/2026-08-13-frl-02l1c-copy-spell-resolve-first-order-design.md`; current L1 tests under `forge-gui-desktop/src/test/java/forge/game/decision` and `forge-gui-desktop/src/test/java/forge/view`.

- [ ] Confirm `git rev-parse HEAD` is `d018e87c1429cbe2c36178d587625b66e54acea0`.
- [ ] Confirm the worktree contains no unrelated changes and that `C:\forgeAI` remains clean and on its protected checkout.
- [ ] Read the complete design document and record that its expected implementation inventory and non-goals are the only scope authority.
- [ ] Run the current focused L1 baseline from the isolated checkout:

      mvn -pl forge-gui-desktop -am '-Dtest=SimultaneousTriggerOrderPublicApiTest,SimultaneousTriggerOrderTraceTest,SimultaneousTriggerOrderCoordinatorTest,SimultaneousTriggerOrderEngineIntegrationTest,FRL02L1SimultaneousTriggerOrderAuditTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

- [ ] Preserve the baseline evidence: `BUILD SUCCESS`; 21 tests; 0 failures; 0 errors; 0 skipped; the canonical L1 audit child JVM exits 0.
- [ ] Stop before implementation if the baseline, authority hash, or protected-checkout cleanliness differs; do not repair unrelated failures as part of this milestone.

Expected result: the isolated worktree is at the exact authority commit, the protected checkout is untouched, and the existing L1 acceptance is a recorded green baseline.

### Task 2 — Add failing public-contract tests before production types

Anchors: new `forge-gui-desktop/src/test/java/forge/game/decision/CopySpellResolveFirstOrderPublicApiTest.java`; design sections Minimum public item projection and Duplicate-looking items and native identity.

- [ ] Add reflection tests for `CopySpellResolveFirstOrderProfile`, `CopySpellResolveFirstOrderContext`, `CopySpellResolveFirstOrderItem`, `CopySpellResolveFirstOrderSourceProjection`, `CopySpellResolveFirstOrderItemKind`, and the L1C resolver surface.
- [ ] Assert the only public item values are session-local `itemId`, visible original-source name, `ApiType`, and typed `COPIED_SPELL`.
- [ ] Assert no public method/field/type exposes `CardSelectionCard`, `SpellAbility`, `SpellApiBased`, `Card`, `CardLKI`, `GameObject`, native IDs, copied-host IDs, raw provenance, target objects, descriptions, stack text, or arbitrary string output.
- [ ] Add a duplicate-looking pair assertion: equal visible source/API/kind values, distinct ordinal IDs, and distinct private native identities.
- [ ] Assert the copied host is not passed through unchanged as `CardSelectionCard`.
- [ ] Run the focused test while the types are absent to establish the expected red contract:

      mvn -pl forge-gui-desktop -am '-Dtest=CopySpellResolveFirstOrderPublicApiTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

Expected result: the new test class fails only because the planned L1C types do not yet exist; no production or existing test file is modified in this planning step.

### Task 3 — Implement the immutable typed L1C profile, source projection, item, context, and kind

Anchors: new production files in `forge-game/src/main/java/forge/game/decision`; public fields defined in the design; callback receiver ownership from `PlayerControllerAi`.

- [ ] Create `CopySpellResolveFirstOrderProfile.java` with only `COPY_SPELL_RESOLVE_FIRST_ORDER`.
- [ ] Create `CopySpellResolveFirstOrderItemKind.java` with only `COPIED_SPELL`.
- [ ] Create an immutable `CopySpellResolveFirstOrderSourceProjection` that captures only the visible original-source name after visibility admission and rejects null/unstable values.
- [ ] Create an immutable `CopySpellResolveFirstOrderItem` with ordinal, source projection, non-null `ApiType`, and the typed kind; keep native objects out of the object graph and serialization.
- [ ] Create an immutable `CopySpellResolveFirstOrderContext` with profile, `RESOLVE_FIRST`, session ID, zero-based step index, original item count, and callback receiver player ID; reject continuation/native/hidden state.
- [ ] Implement deterministic value equality for public projections while keeping item ordinals distinct.
- [ ] Run the public API test:

      mvn -pl forge-gui-desktop -am '-Dtest=CopySpellResolveFirstOrderPublicApiTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

Expected result: `BUILD SUCCESS`, all public API/projection tests pass, and reflection confirms that no native or hidden Forge object crosses the L1C boundary.

### Task 4 — Extend `DecisionRequest` and `LegalCandidate` with profile-discriminated L1C ORDER validation

Anchors: `forge-game/src/main/java/forge/game/decision/DecisionRequest.java` constructors and ORDER validation around the current `SimultaneousTriggerOrderContext` slot; `forge-game/src/main/java/forge/game/decision/LegalCandidate.java` existing `order(...)` factory around the current L1 candidate payload.

- [ ] Add a separate L1C context slot and accessor; do not replace or widen the existing L1 slot.
- [ ] Add a separate typed L1C candidate payload/factory; retain the existing `getOrderKind()/getOrderItem()` behavior for L1.
- [ ] Validate that L1 requests contain only L1 context/items and L1 profile; L1C requests contain only L1C context/items and `COPY_SPELL_RESOLVE_FIRST_ORDER`.
- [ ] Enforce `DecisionType.ORDER`, `RESOLVE_FIRST`, at least two candidates, zero-based step, original count, choosing-player ID, exact remaining-set membership, deterministic ordinal ordering, and semantic key `RESOLVE_FIRST|<itemId>`.
- [ ] Reject generic or cross-profile ORDER candidates, wrong profile, wrong kind, stale session, stale request, foreign item, duplicate semantic keys, and one-candidate public requests.
- [ ] Add regression assertions that every existing L1 request and candidate validates exactly as before.
- [ ] Run the contract and current L1 tests together:

      mvn -pl forge-gui-desktop -am '-Dtest=CopySpellResolveFirstOrderPublicApiTest,SimultaneousTriggerOrderPublicApiTest,SimultaneousTriggerOrderCoordinatorTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

Expected result: `BUILD SUCCESS`; L1 request construction remains green, L1C requests accept only the typed profile, and no generic permutation item is accepted.

### Task 5 — Add the separate L1C provider and controller-local counters

Anchors: new `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderDecisionProvider.java`; `forge-game/src/main/java/forge/game/player/PlayerController.java`; existing `SimultaneousTriggerOrderDecisionProvider` counter methods.

- [ ] Add a profile-specific resolver interface/handle whose resolver receives only the typed L1C `DecisionRequest` and returns a typed L1C `LegalCandidate`.
- [ ] Add L1C-local `nextRequestId` and `nextOrderSessionId` counters with the same deterministic monotonic semantics as L1, initialized per provider/controller and never static/global.
- [ ] Capture the resolver once at L1C session start; prove a setter replacement during an active session does not change the callback source.
- [ ] Add only the L1C provider accessor/setter needed by the AI controller; leave the existing L1 provider accessor, counter fields, and sequence unchanged.
- [ ] Correlate public/session data as `(profile, sessionId, stepIndex)` and document that provider-local request IDs are not globally unique; keep trace joins on `traceRequestIndex` plus profile/stage.
- [ ] Add the provider counter-isolation test to `CopySpellResolveFirstOrderPublicApiTest`: create fresh L1 and L1C providers, verify both start at 1 and advance independently, and verify no `OrderDecisionIdAuthority.java` exists.
- [ ] Assert no native identity, UUID, wall-clock, RNG, or shared global counter participates in either provider ID.
- [ ] Run the provider-focused test:

      mvn -pl forge-gui-desktop -am '-Dtest=CopySpellResolveFirstOrderPublicApiTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

Expected result: `BUILD SUCCESS`; the separate L1C provider compiles and its public API/counter tests pass. L1 and L1C counters are independent, deterministic, controller-local, and resolver capture is specified for the later coordinator session.

### Task 6 — Implement strict all-or-nothing L1C admission and private identity capture

Anchors: new `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderDecisionCoordinator.java`; `forge-game/src/main/java/forge/game/ability/effects/CopySpellAbilityEffect.java`; `forge-game/src/main/java/forge/game/card/CardFactory.java`; visibility/source APIs inspected in the existing Forge model.

- [ ] Capture the callback receiver as the choosing player; require every entry’s activating player and copied-host controller to be non-null and exactly that receiver, with non-null owner/controller values required by the copy factory.
- [ ] Require non-null list and at least two entries; reject null entries and repeated native identities immediately as `SESSION_INTEGRITY_FAILURE`.
- [ ] Require each entry to be the exact accepted runtime shape: `SpellApiBased`, spell, copied, non-trigger, non-`WrappedAbility`, host non-null and marked copied-spell.
- [ ] Require copied-host lineage: non-null `getCopiedPermanent()`, copied host `getCastSA()` is the same native entry, one original source by native identity, and one non-null common effect API.
- [ ] Require the original source to be face-up and publicly projectable to the callback receiver; capture only the visible source name and API value. Reject hidden/unprojectable source and capture exceptions without leaking native data.
- [ ] Reject mixed source identities, mixed APIs, null API, copied non-spells, copied abilities, wrappers, non-copied spells, mixed choosing players, null ownership, and any active `ActionContinuation`. Do not invent a continuation.
- [ ] Build the immutable initial ordinal list and private `IdentityHashMap` only after all admission checks that can mutate/throw have completed; never use the copied host as a public card selection object.
- [ ] Keep malformed family intent classified as `MALFORMED_L1C_INTENT`. With an active L1C resolver, terminalize through the existing fail-closed unsupported-admission reason rather than native fallback; without a resolver, preserve native compatibility. Do not add a new result enum merely for the router classification.
- [ ] Test the admission matrix in `CopySpellResolveFirstOrderCoordinatorTest`, including null list, n=0, n=1, null entry, copied non-spell, mixed trigger/copy, mixed players, null ownership, hidden source, null API, exact n=2, duplicate public projection, and same native identity twice.
- [ ] Run the admission-focused command:

      mvn -pl forge-gui-desktop -am '-Dtest=CopySpellResolveFirstOrderCoordinatorTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

Expected result: `BUILD SUCCESS`; the exact factory-produced copied-spell batch is admitted, malformed family intent is represented for later router ownership, unowned input is not handled here, and repeated native identity is `SESSION_INTEGRITY_FAILURE`.

### Task 7 — Centralize the pure RESOLVE_FIRST reversal utility

Anchors: new `forge-game/src/main/java/forge/game/decision/OrderResolutionTranslation.java`; current private reversal helper in `forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderDecisionCoordinator.java`; `forge-game/src/main/java/forge/game/decision/OrderDirection.java`.

- [ ] Implement one pure helper that copies a non-null list and reverses object references without sorting, deduplicating, RNG, native identity, or public equality.
- [ ] Use it for semantic resolve-first sequence to native insertion order and for the inverse direction; verify reversal is its own inverse.
- [ ] Refactor the existing L1 coordinator to use the helper while preserving its request ordering, native callback contract, and local counters.
- [ ] Add tests for n=2, n=3, n=4 and duplicate-looking public items with distinct native identities; include null-input rejection and input-list immutability.
- [ ] Run the translation and coordinator tests now that the L1C coordinator exists:

      mvn -pl forge-gui-desktop -am '-Dtest=CopySpellResolveFirstOrderCoordinatorTest,SimultaneousTriggerOrderCoordinatorTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

Expected result: `BUILD SUCCESS`; [A,B] maps to [B,A], [A,B,C] to [C,B,A], [A,B,C,D] to [D,C,B,A], and both L1C admission and existing L1 coordinator tests remain green.

### Task 8 — Implement the pure family-intent pre-classifier and complete router ownership wiring

Anchors: new `forge-game/src/main/java/forge/game/decision/OrderProfileRouter.java`; `forge-ai/src/main/java/forge/ai/PlayerControllerAi.java` method `orderSimultaneousSa`; existing L1 admission predicate `SimultaneousTriggerOrderDecisionCoordinator.isSimultaneousTriggerProfileCandidate`; `forge-game/src/main/java/forge/game/player/PlayerController.java` provider accessors; L1C strict-admission result from Task 6.

- [ ] Keep pre-classification resolver-independent and expose only the pre-admission classes `L1_EXACT`, `COPY_SPELL_FAMILY_INTENT`, and `UNOWNED_OTHER`. Produce `MALFORMED_L1C_INTENT` only after family intent reaches strict admission and fails.
- [ ] Extract or expose the existing L1 exact predicate as a side-effect-free method without broadening its admission rules.
- [ ] Implement the family pre-classifier exactly as `active != null`, size at least 2, and every entry non-null, spell, copied, host non-null, and host copied-spell. Catch inspection exceptions and return `UNOWNED_OTHER`.
- [ ] Prove by tests that the pre-classifier does not inspect `SpellApiBased`, source lineage, API, players, visibility, `getCastSA()`, targets, or resolver presence.
- [ ] Route the AI callback through one dispatcher that records raw callback accounting once, selects one owner, and never lets L1 and L1C call each other or the native callback twice.
- [ ] Route exact L1 to the existing L1 coordinator, family intent to L1C strict admission, malformed active-resolver input to fail-closed L1C admission, malformed no-resolver input to existing native compatibility, and unowned input to existing compatibility without consulting L1C.
- [ ] Keep `orderAndPlaySimultaneousSa` unchanged in meaning: it consumes the returned insertion list and performs per-copy TARGET setup before each stack insertion.
- [ ] Test the complete shape matrix in `OrderProfileRouterTest`: null list, n=0, n=1, null entry, copied non-spell, non-copied `SpellApiBased`, trigger/`WrappedAbility`, mixed trigger/copy, mixed players, null ownership, hidden source, null API, exact L1, exact family, malformed family, unrelated input, and inspection exception.
- [ ] Test resolver isolation with both providers installed: L1 input reaches only L1; exact L1C reaches only L1C; malformed family with active L1C fails closed; unowned input never consults L1C; L1 resolver presence never claims C.
- [ ] Test raw accounting once and assert no duplicate request/trace records or list mutation before successful completion.
- [ ] Run the router and current L1 suites:

      mvn -pl forge-gui-desktop -am '-Dtest=OrderProfileRouterTest,SimultaneousTriggerOrderPublicApiTest,SimultaneousTriggerOrderTraceTest,SimultaneousTriggerOrderCoordinatorTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

Expected result: `BUILD SUCCESS`; every pre-admission shape has one deterministic classification, post-admission malformed ownership is explicit, resolver isolation is proven, and raw accounting increments once.





### Task 9 — Implement native and external L1C coordinator lifecycles

Anchors: new `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderDecisionCoordinator.java`; `CopySpellResolveFirstOrderDecisionProvider`; existing `DecisionTrace` request-handle lifecycle; existing `SimultaneousTriggerOrderIntegrityException.Reason` values.

- [ ] For an external session, capture the resolver once, emit each request from the exact current remaining set in initial ordinal order, require at least two remaining items, and use `forced=false` for every public ORDER request.
- [ ] For external n=2, n=3, and n=4, assert exactly n-1 requests, exact candidate sets at each step, one chosen item per request, one internal final item, a single pure reversal, no native callback, no partial insertion, and one terminal result per request.
- [ ] Validate external results by profile, ORDER direction, `SELECT_RESOLVE_FIRST` kind, semantic key, current `itemId`, session/request/step, and membership in the current remaining set.
- [ ] Add explicit external failures for null, throwing, stale, foreign, duplicate, wrong-profile, wrong-kind, wrong-session, wrong-step, and removed-candidate results; terminalize as `INVALID_EXTERNAL_CANDIDATE` with no native fallback.
- [ ] For a native session, record the full step-zero request before calling the native callback exactly once, retain the resolver-free native teacher path, and validate exact size, no null, no foreign identity, no omission, and no duplicate identity.
- [ ] On valid native n=2/n=3/n=4 permutations, set native-completed and mapping-attempted flags, translate the native insertion list by the central reversal, complete the semantic request sequence, and return the original valid native insertion list unchanged.
- [ ] On native callback throw, terminalize step zero as `NATIVE_CALLBACK_FAILURE`; on null/invalid/foreign/omitted/duplicated permutation, terminalize as `MAPPING_FAILED`. Do not retry, fall back, fabricate a candidate, mutate the stack, or return a partial list.
- [ ] Ensure same native identity twice fails before native callback and returns `SESSION_INTEGRITY_FAILURE`.
- [ ] Ensure every successful and intentional failure path has exactly one terminal RESULT and zero intentional `TRACE_INCOMPLETE` records.
- [ ] Add resolver replacement during-session coverage proving that the captured external resolver remains active while the provider setter changes.
- [ ] Run the lifecycle suite:

      mvn -pl forge-gui-desktop -am '-Dtest=CopySpellResolveFirstOrderCoordinatorTest,CopySpellResolveFirstOrderTraceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

Expected result: `BUILD SUCCESS`; external and native n=2/n=3/n=4 sessions satisfy the exact request counts and lifecycle flags, invalid candidates/permutations fail with the required reasons, and no fallback or partial stack mutation occurs.

### Task 10 — Persist typed BC eligibility and wholly versioned DECISION_TRACE_V3 records

Anchors: new `forge-game/src/main/java/forge/game/decision/DecisionTraceTeacherLabelEligibility.java`; `forge-game/src/main/java/forge/game/decision/DecisionTraceRequestRecord.java`; `forge-game/src/main/java/forge/game/decision/DecisionTraceTrainingValidator.java`; `forge-game/src/main/java/forge/game/decision/DeterminismTrace.java`; existing RESULT serialization and request-handle methods.

- [ ] Add the typed enum values `NOT_APPLICABLE`, `BC_ELIGIBLE`, and `BC_EXCLUDED_PUBLIC_SYMMETRY`. Keep the enum value request-scoped and independent of candidate semantic keys.
- [ ] Add the typed profile discriminator and eligibility field to `DecisionTraceRequestRecord`, preserving all current V2 fields and getters used by offline consumers.
- [ ] Make exact L1 ORDER requests supply `BC_ELIGIBLE` in the in-memory record while keeping L1-only serialized bytes and field ordering byte-compatible with `DECISION_TRACE_V2`.
- [ ] Define the legacy deserializer rule in the implementation: an existing V2 exact L1 ORDER REQUEST with no eligibility field becomes in-memory `BC_ELIGIBLE`; a V2 non-ORDER request becomes `NOT_APPLICABLE`; this default is never applied to V3 L1C.
- [ ] Make the L1C coordinator compute symmetry from captured public projections at request creation using only `(visibleSourceName, effectApi, kind)` after removing `itemId`. Any two or more equal projection keys produce `BC_EXCLUDED_PUBLIC_SYMMETRY`; otherwise produce `BC_ELIGIBLE`.
- [ ] Make `DeterminismTrace` retain structured REQUEST/RESULT records in memory until `finish()`, track whether an L1C request occurred, and serialize the entire file as V2 or V3 only after that decision. No request/result bytes may be written before the version is known.
- [ ] Serialize V3 REQUEST records with the existing V2 fields plus typed L1C profile and eligibility. Keep paired RESULT selected-key and lifecycle flags unchanged; join eligibility through `traceRequestIndex`.
- [ ] Record the C-bearing summary as `decisionTraceVersion=DECISION_TRACE_V3`. Parse missing, unknown, or malformed eligibility on a V3 L1C REQUEST as ineligible rather than throwing into BC.
- [ ] Update `DecisionTraceTrainingValidator.isBCPolicySample` to require `BC_ELIGIBLE` in addition to native-completed, mapping-attempted, `CHOSEN`, non-forced, and selected-key membership. An external L1C RESULT remains non-BC through existing lifecycle flags.
- [ ] Add trace tests for the canonical symmetric duplicate pair: native completion and mapping attempted remain true, the request persists `BC_EXCLUDED_PUBLIC_SYMMETRY`, and BC is false.
- [ ] Add a non-symmetric native test that persists `BC_ELIGIBLE` and remains BC-eligible when all existing lifecycle conditions hold.
- [ ] Add a legacy V2 deserialization test that reads an existing L1-only V2 REQUEST without the new field, normalizes it to `BC_ELIGIBLE`, and preserves its historical BC result and exact serialized bytes.
- [ ] Add a V3 L1C missing/unknown/malformed-eligibility test that fails closed to BC=false; do not use that fail-closed rule for legacy V2 L1.
- [ ] Add the temporal version-selection test: record an L1 REQUEST, then an L1C REQUEST, then call `finish()`; assert the complete file is V3, contains no V2 prefix, and contains no mixed record encoding.
- [ ] Run the trace/validator suite:

      mvn -pl forge-gui-desktop -am '-Dtest=DecisionTraceV3Test,CopySpellResolveFirstOrderTraceTest,SimultaneousTriggerOrderTraceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

Expected result: `BUILD SUCCESS`; symmetric C native history is persisted but excluded from BC, non-symmetric C native history remains eligible, missing metadata fails closed, C-bearing files are wholly V3, and L1-only files remain byte-compatible V2.

### Task 11 — Split raw, L1, and L1C diagnostics into an explicit versioned namespace

Anchors: `forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderAuditDiagnostics.java`; existing `forge-gui-desktop/src/test/java/forge/view/FRL02L1SimultaneousTriggerOrderAuditTest.java`; new `forge-gui-desktop/src/test/java/forge/view/FRL02L1CCopySpellResolveFirstOrderAuditTest.java`.

- [ ] Retain one raw counter at the dispatcher boundary and ensure it increments once per `orderSimultaneousSa` callback before profile admission.
- [ ] Emit the explicit diagnostics version `FRL_02L1_ORDER_AUDIT_V3` with separate `raw.`, `l1.`, and `l1c.` property namespaces. Do not silently redefine or reinterpret historical V2 keys.
- [ ] Preserve raw canonical values: `raw.orderSimultaneousSa.total=116`, `raw.orderSimultaneousSa.n0=0`, `raw.orderSimultaneousSa.n1=96`, `raw.orderSimultaneousSa.n2=14`, `raw.orderSimultaneousSa.n3=5`, `raw.orderSimultaneousSa.n4=1`, `raw.orderSimultaneousSa.nOther=0`, and `raw.rawMultiItemCallbacks=20`.
- [ ] Preserve L1 values in the new namespace: trigger sessions/admitted `19/19`, ORDER requests `26`, candidate sizes 2/3/4 `19/6/1`, forced `0`, unsupported fallback `0`, mapping failures `0`, and trace incomplete `0`.
- [ ] Add L1C values: copy sessions/admitted `1/1`, input size2 `1`, ORDER requests `1`, candidate size2 `1`, forced `0`, native teacher callbacks `1` in the native canonical run, mapping failures `0`, native callback failures `0`, invalid external candidates `0`, and trace incomplete `0`.
- [ ] Keep the old historical non-L1 and outside-L1 fields from being reused as L1C meanings; raw attribution must be 19 L1 trigger sessions plus one L1C copied-spell session.
- [ ] Keep diagnostics disabled by default and prove enabling them cannot change request order, session IDs, item IDs, trace version, or trace tree hash.
- [ ] Update the existing L1 audit test only for the explicitly versioned namespace/contract; create the L1C audit test without weakening retained L1 assertions.
- [ ] Run the diagnostics-focused tests:

      mvn -pl forge-gui-desktop -am '-Dtest=FRL02L1SimultaneousTriggerOrderAuditTest,FRL02L1CCopySpellResolveFirstOrderAuditTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

Expected result: `BUILD SUCCESS`; diagnostics have one raw namespace and independent L1/L1C namespaces, the locked counts are exact, and no legacy key is silently repurposed.

### Task 12 — Prove the real engine route and canonical acceptance without TARGET absorption

Anchors: `forge-ai/src/main/java/forge/ai/PlayerControllerAi.java` methods `orderSimultaneousSa` and `orderAndPlaySimultaneousSa`; `forge-game/src/main/java/forge/game/ability/effects/CopySpellAbilityEffect.java`; `forge-game/src/main/java/forge/game/zone/MagicStack.java` method `orderAndPlaySimultaneousSa`; new integration and audit tests.

- [ ] Add a real-engine integration test that reaches `PlayerControllerAi -> CopySpellAbilityEffect -> orderAndPlaySimultaneousSa -> per-copy TARGET setup -> MagicStack -> resolution` rather than only constructing synthetic coordinator objects.
- [ ] Exercise external n=2 and n=3 L1C sessions and native n=2; assert external/native semantic equivalence for the same valid ordering while preserving the native insertion list contract.
- [ ] Assert per-copy TARGET setup occurs exactly once for each native copy after ORDER and before its `MagicStack` insertion; assert no duplicate TARGET request, no L1C target resolver call, and correct native copy-to-target association.
- [ ] Add the future-correlation guard: duplicate-looking copies must not permit a target implementation to guess by source text, copied-host ID, target text, or list position. The test records the current verdict `TARGET_NOT_YET_OWNED_BUT_FUTURE_CORRELATION_REQUIRED`.
- [ ] Verify that `CopySpellAbilityEffect.java`, `forge-game/src/main/java/forge/game/zone/MagicStack.java`, `PlayerControllerHuman.java`, `AiController.java`, and the existing TARGET path have no semantic changes.
- [ ] Run the real-engine integration suite:

      mvn -pl forge-gui-desktop -am '-Dtest=CopySpellResolveFirstOrderEngineIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

Expected result: `BUILD SUCCESS`; the actual engine path preserves `ORDER -> per-copy TARGET setup -> MagicStack insertion -> resolution`, with no TARGET ownership absorbed by L1C.

### Task 13 — Run canonical acceptance, retain L1 regression, and prepare the implementation handoff

Anchors: new `forge-gui-desktop/src/test/java/forge/view/FRL02L1CCopySpellResolveFirstOrderAuditTest.java`; existing `FRL02L1SimultaneousTriggerOrderAuditTest`; all focused L1C tests and the full scoped diff.

- [ ] Run the retained focused L1 regression command exactly:

      mvn -pl forge-gui-desktop -am '-Dtest=SimultaneousTriggerOrderPublicApiTest,SimultaneousTriggerOrderTraceTest,SimultaneousTriggerOrderCoordinatorTest,SimultaneousTriggerOrderEngineIntegrationTest,FRL02L1SimultaneousTriggerOrderAuditTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

- [ ] Require L1 retained acceptance of 19/19 sessions, 26 requests, candidate sizes 2/3/4 = 19/6/1, fallback 0, 26 requests total, and no regression in the existing deterministic trace.
- [ ] Run the L1C canonical fresh-child-JVM workload with decks `Izzet Guild Kit` and `Dimir Guild Kit`, 10 games, seed `20260810`, once with diagnostics enabled and once disabled. Use the same child-JVM construction and classpath pattern as `FRL02L1SimultaneousTriggerOrderAuditTest`.
- [ ] Require L1C canonical acceptance of 1/1 session, input size2=1, 1 ORDER request, candidate size2=1, forced=0, native teacher callback=1 in the native run, mapping failures=0, native callback failures=0, invalid external candidates=0, and trace incomplete=0.
- [ ] Require the raw canonical values 116 total, n1=96, n2/n3/n4=14/5/1, raw multi-item callbacks=20, with attribution exactly 19 L1 trigger sessions plus one L1C copied-spell session.
- [ ] Require the C-bearing run to contain `decisionTraceVersion=DECISION_TRACE_V3` and the L1-only control run to remain `DECISION_TRACE_V2`.
- [ ] Compare audit-on and audit-off deterministic trace tree hashes and require exact equality.
- [ ] Run the complete scoped verification command after the individual suites are green:

      mvn -pl forge-gui-desktop -am '-Dtest=CopySpellResolveFirstOrderPublicApiTest,CopySpellResolveFirstOrderTraceTest,DecisionTraceV3Test,CopySpellResolveFirstOrderCoordinatorTest,OrderProfileRouterTest,CopySpellResolveFirstOrderEngineIntegrationTest,FRL02L1CCopySpellResolveFirstOrderAuditTest,SimultaneousTriggerOrderPublicApiTest,SimultaneousTriggerOrderTraceTest,SimultaneousTriggerOrderCoordinatorTest,SimultaneousTriggerOrderEngineIntegrationTest,FRL02L1SimultaneousTriggerOrderAuditTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

- [ ] Inspect the final diff and assert only the design-listed production/test files plus the implementation plan are changed; assert no `OrderDecisionIdAuthority.java`, no TARGET-path change, no generic permutation type, no trace result-kind change, and no protected-checkout change.
- [ ] Report `FRL_02L1C_PASS` only when all exact counts, trace versions, hashes, failure matrices, L1 regression, and integration assertions are green.

Expected result: the canonical workload and all focused tests pass with the exact locked metrics, L1 remains accepted, C-bearing traces are V3, audit instrumentation is deterministic, and the implementation is ready for a separate plan-reviewed handoff.

## Required test matrix

The task decomposition above must not be shortened when implementing. The following cases are mandatory and must be visible in test names or assertion messages:

| Area | Required cases | Expected result |
| --- | --- | --- |
| Coordinator cardinality | external and native n=2, n=3, n=4 | exactly n-1 requests; final item internal; native callback once; no partial mutation |
| Public identity | duplicate public projections | distinct ordinal candidates; no collapse; private native identities distinct |
| Native identity | same native object twice | `SESSION_INTEGRITY_FAILURE` before callback |
| External candidates | invalid, stale, foreign, duplicate, wrong-profile, wrong-kind, wrong-session, wrong-step, removed item, null, throw | `INVALID_EXTERNAL_CANDIDATE`; no native fallback |
| Native permutations | null, wrong size, null entry, foreign object, omission, duplicate | `MAPPING_FAILED`; no fallback/stack insertion |
| Native callback | callback throw | `NATIVE_CALLBACK_FAILURE`; no retry/fallback |
| Router shapes | null list, n=0, n=1, null entry, copied non-spell, mixed trigger/copy, mixed players, null ownership, hidden source, null API | exact `UNOWNED_OTHER` or `MALFORMED_L1C_INTENT` according to the closed predicate; no accidental ownership |
| Router classes | exact L1, pure family intent, malformed family intent, unowned other | `L1_EXACT`, `COPY_SPELL_FAMILY_INTENT`, `MALFORMED_L1C_INTENT`, `UNOWNED_OTHER` |
| Resolver isolation | L1 resolver with C input; L1C resolver with L1/unowned input; resolver replacement mid-session | only exact profile resolver called; malformed owned C fails closed; unowned never consults C |
| Provider isolation | fresh L1 and L1C providers | local IDs both start at 1 and advance independently; no global authority |
| Trace eligibility | symmetric native request | V3 `BC_EXCLUDED_PUBLIC_SYMMETRY`; BC=false despite native completion/mapping |
| Trace eligibility | non-symmetric native request | V3 `BC_ELIGIBLE`; BC remains eligible when current flags pass |
| Trace fail closed | missing/unknown/malformed C eligibility | BC=false |
| Trace compatibility | L1-only file | byte-compatible V2; no V3 fields or mixed records |
| Legacy V2 BC compatibility | existing V2 exact L1 ORDER REQUEST without eligibility field | deserializes as `BC_ELIGIBLE`; historical BC semantics and serialized bytes remain unchanged |
| Trace versioning | file containing any L1C request | wholly V3; typed profile and eligibility in REQUEST; RESULT flags unchanged |
| Trace version timing | L1 REQUEST, then L1C REQUEST, then `finish()` | complete file is V3; no V2 prefix and no mixed encoding |
| Engine order | real CopySpellAbilityEffect route | ORDER, then per-copy TARGET, then MagicStack, then resolution |
| Canonical acceptance | Izzet Guild Kit vs Dimir Guild Kit, 10 games, seed 20260810 | raw 116; L1 19/19 and 26; L1C 1/1 and 1 request; V3 C trace; audit-on/off hash equality |

## Implementation handoff and review gate

Before implementation begins, the plan reviewer must confirm:

- [ ] The complete authority design at commit `d018e87c1429cbe2c36178d587625b66e54acea0` was read and is the only scope authority.
- [ ] The exact file-responsibility map is complete, and every task names repository paths, source anchors or method names, concrete commands, and expected outcomes.
- [ ] The router classification is pure and closed before strict admission; resolver presence does not influence pre-classification.
- [ ] L1 and L1C providers/counters are isolated; `OrderDecisionIdAuthority.java` is not created.
- [ ] Public projection and duplicate/native identity rules are explicit and fail closed.
- [ ] BC eligibility is persisted as typed request metadata; symmetry is not reconstructed from item IDs or hashes.
- [ ] V2/V3 trace compatibility and no-mixing rules are explicit.
- [ ] Existing V2 exact L1 ORDER REQUESTs without the new field deserialize as `BC_ELIGIBLE`; only missing/malformed V3 L1C eligibility fails closed to BC=false.
- [ ] `DeterminismTrace` buffers structured records until `finish()`, and the L1-then-L1C temporal test proves a wholly V3 file with no V2 prefix.
- [ ] TARGET remains engine-owned and the actual MagicStack path is `forge-game/src/main/java/forge/game/zone/MagicStack.java`.
- [ ] Canonical L1 and L1C acceptance values, audit-on/off equality, and all failure matrices are explicit.
- [ ] No production code, tests, push, PR, or follow-up feature work is performed as part of plan review.

The implementation worker must use `superpowers:subagent-driven-development` or `superpowers:executing-plans`, execute tasks in order, preserve the checkboxes, stop at any failed expected result, and return for review before changing scope.
