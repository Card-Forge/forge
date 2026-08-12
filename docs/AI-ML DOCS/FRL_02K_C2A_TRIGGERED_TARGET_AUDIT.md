# FRL-02K-C2A - Triggered TARGET Provider Seam Audit

Status: `FRL_02K_C2A_FINAL_VALIDATED`. The current C2A-R verification ran against code/test HEAD
`1d7216a93a8` (`fix: drop malformed direct trigger cycles`) and passed the focused C2A selector 94/94 with 0 failures,
0 errors, and 0 skips; the separate R2 API selector `TriggeredTargetIntegrityExceptionApiTest` 1/1; the exact
retained/API/confirmation/ownership selector 21/21 (20/20 when the separate ownership audit is excluded); the
canonical native ownership workload 1/1 with exit 0; and the broad reactor 743 total with 737 passed, 0 failures,
0 errors, and 6 configured skips. The broad reactor exited 0 in 719 s (about 11:59): `forge-ai` 20/20,
`forge-game` 16/16, and `forge-gui-desktop` 707 total with 701 passed and 6 skips. Package and validate/checkstyle
are current after 1d: the package command exited 0, the validate command exited 0 with 0 Checkstyle violations,
and the combined verification took about 55 s; no per-command duration is assigned. The current document diff check
is clean. The earlier 84-test aggregate in the previous audit snapshot is historical only, not a current selector
count. This final C2A-R refresh updates only this audit document.

Audit date: 2026-08-12

Repository: `chrismaghuhn/forgeAI`

Audit worktree: `C:\forgeAI-triggered-target-c2a`

Branch: `frl/02k-c2a-triggered-target-provider-seam`

Previous quality-review starting HEAD: `92384c6865d27c23df671de538b20feb1f58b2f0`
(`fix: close C2A child traversal and parent cycles`); that historical checkpoint was 28 commits ahead of
`origin/master`.

P2 follow-up starting HEAD before the retained tests: `b9b7e6da3dbaaca2a323795071d551c3e3097bf0`
(`docs: record FRL-02K-C2A latest review evidence`); branch ahead count was 32 relative to `origin/master`.

Current C2A-R verification HEAD before this audit-only update: `1d7216a93a8`
(`fix: drop malformed direct trigger cycles`). The current code/test verification is complete before this final
document-only commit; no production source or test file is changed by this document update.

Base: `3851fdf3825` (`origin/master`, `FRL-02K-C2: audit triggered target ownership`)

This is the current C2A-R validation checkpoint for the committed C2A seam. It documents the exact admitted Blood
Operative ETB target shape, the measured native/external ownership boundary, and the focused, retained/API/
confirmation, canonical native, broad reactor, package, and configured-validation results current at this code HEAD.
It does not generalize triggered TARGET or add a CONFIRMATION boundary.

Evidence labels:

- `[BESTAETIGT]` - directly established by current source or a completed focused gate.
- `[STARKES INDIZ]` - reproducible evidence retained from a completed checkpoint, with a narrower boundary than a broad validation.
- `[UNKLAERT]` - not established by this worktree or its completed gates.
- `[BLOCKER]` - must remain closed before the relevant scope can be widened.

## 1. Checkpoint and decision

[BESTAETIGT] At the start of the previous quality review, `git status --short --branch` was clean. The requested branch
was `frl/02k-c2a-triggered-target-provider-seam...origin/master [ahead 28]` at
`92384c6865d27c23df671de538b20feb1f58b2f0`; `3851fdf3825` is `origin/master`. That review added bounded
production and regression-test corrections in `cbdf110cb13e8500cd9b332a03da67fe05fb4cc6`.

[BESTAETIGT] The P2 follow-up started from clean HEAD `b9b7e6da3dbaaca2a323795071d551c3e3097bf0` at branch
ahead 32, added only retained coordinator-seam tests, and produced code/test HEAD
`c44c80c6d9f67f1f480ca246a8bf804805fdd6c7` at branch ahead 33. This audit-document update is the separate
documentation correction.

The decision is deliberately narrow:

```text
Blood Operative ETB TARGET: admitted and operational for the exact profile below
global triggered TARGET: not admitted by this coordinator
Blood CONFIRMATION: not implemented
global CONFIRMATION: not implemented
```

The existing C2/C2R material remains evidence, not a claim that C2A is a universal target or confirmation adapter.

[BESTAETIGT] The current route retains R1's resolver-active cyclic-parent fail-closed protection. Historical commit
`8a3980f123b` added the resolver-null production-order regression fix; current commit `1d7216a93a8` adds the direct
malformed-cycle `playTrigger` fix. The malformed Forge cyclic graph itself is not claimed to be natively executable,
and either no-stack safety disposition is not an external ownership transfer.

[BESTAETIGT] R2 removed the coarse public `TriggeredTargetIntegrityException.Status` type, field, and getter. The
`Reason` value is the sole machine-readable classification; the separate 1/1 API selector is recorded in the evidence
matrix.

[BESTAETIGT] R3 synchronized the central status authorities in `FRL_02K_CONFIRMATION_AUDIT.md` and `ML_STRATEGY.md`
to this exact C2A boundary. Those documents point to this audit's evidence matrix and exact-command record and add no
separate test result.

## 2. Exact production API and ownership split

[BESTAETIGT] The ownership is split at the controller/provider/coordinator boundaries, not through a global
provider or global request counter.

| Owner | Exact responsibility |
|---|---|
| `PlayerController` | Owns one `private final TargetDecisionProvider targetDecisionProvider` and one nullable `TargetDecisionProvider.Resolver targetDecisionResolver`; exposes final get/set accessors. The resolver is the per-controller external ownership switch. |
| `TargetDecisionProvider` | Enumerates Forge-legal public target candidates, creates immutable request values, and applies one selected candidate through the live Forge `TargetChoices`. Its `private long nextRequestId` is monotonic only within that provider instance; independent providers restart their local sequence. Request IDs are not globally unique. |
| `TargetDecisionProvider.Generation` | Returns `DECISION`, `COMPLETE`, or `INVALID_TARGETING`. Only `DECISION` carries a request. `COMPLETE` means the current target group is complete after Forge mutation/reassessment; `INVALID_TARGETING` means Forge cannot supply the mandatory target state. |
| `TriggeredTargetDecisionCoordinator` | Stateless admission/orchestration boundary. It has no mutable per-game fields. It checks the exact profile, rejects active continuations, calls the provider on the underlying live ability, routes native versus external ownership, and records the terminal V2 provenance. |
| `PlayerControllerAi` | Thin routing only: `prepareSingleSa` passes the wrapped ability, wrapper decider, controller-local provider, and nullable resolver to the coordinator; it invokes the native adapter only for the coordinator's native statuses and otherwise keeps the existing Forge path. |

[BESTAETIGT] `TargetDecisionProvider.apply(request, candidate)` is the authoritative Forge legality/mutation/completion
boundary. It requires a live `TARGET` request and a member candidate, rechecks current Forge legality, adds the
selected object to the live `TargetChoices`, and generates the next request or `COMPLETE`. The coordinator does not
duplicate legality or write a second target structure. For an externally owned target, a successful
`provider.apply(request, candidate)` must return a `COMPLETE` generation and leave exactly one live target identity
for this slice. For this admitted profile the initial target list is empty and the minimum is one, so an initial
`COMPLETE` generation is impossible; if observed on the admitted empty initial state, it is an integrity failure
rather than an external success. Native ownership is separate: a native callback can succeed by mapping its
post-callback identity to the immutable teacher request; it does not call `provider.apply` and does not require
completion regeneration.

The relevant source is `forge-game/src/main/java/forge/game/player/PlayerController.java`,
`forge-game/src/main/java/forge/game/decision/TargetDecisionProvider.java`,
`forge-game/src/main/java/forge/game/decision/TriggeredTargetDecisionCoordinator.java`, and
`forge-ai/src/main/java/forge/ai/PlayerControllerAi.java`. The ownership introduction is retained in commit
`7616455fe07`; the coordinator and routing are retained in `9b5367dcea8` and `19cbe984fd4`.

## 3. Exact Blood admission profile

[BESTAETIGT] The exact admitted profile identifier is
`BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD_TARGET`. Admission is a semantic profile check. The source and trigger
provenance must be public and ordinary:

- Source is `Blood Operative`, in `CardStateName.Original`, not cloned, not face-down, and visible to both chooser and decider.
- The trigger is intrinsic, non-static, `ChangesZone`, has no spawning ability, and is not copied. The `WrappedAbility` and its live underlying ability are also intrinsic and non-copied.
- The chooser, wrapper decider, live activating player, and source controller are the same Forge player/seat. No alternate targeting player is accepted.

The trigger definition is normalized from the original and runtime maps. Its semantic keys are exactly:

```text
Mode=ChangesZone
Origin=Any
Destination=Battlefield
ValidCard=Card.Self
OptionalDecider=You
Execute=TrigChangeZone
```

The source's static `TrigChangeZone` SVar is normalized to the exact effect shape:

```text
DB=ChangeZone
Origin=Graveyard
Destination=Exile
ValidTgts=Card
```

The live underlying ability must be `ApiType.ChangeZone`, with Forge-normalized Graveyard target legality and a
single card target:

```text
TgtZone=Graveyard
TargetMin=1
TargetMax=1
initial TargetChoices: empty
sub/additional abilities: absent
pay cost: free
random target: false
random target count: false
```

The underlying ability must not carry `Optional`, `TargetingPlayer`, or a resolved targeting player. Optionality is
owned by the trigger's `OptionalDecider=You`; it is not duplicated on the underlying ChangeZone ability. The target
is therefore `Graveyard -> Exile`, `Card`, min=max=1, with no underlying optional flag. Target selection is
explicitly non-random: both Forge `TargetRestrictions.isRandomTarget()` and
`TargetRestrictions.isRandomNumTargets()` must be false.

[BESTAETIGT] `TriggerDescription`, `TgtPrompt`, and `ValidTgtsDesc` are presentation text and are removed from
normalization. Changing them does not change admission. Unknown semantic keys do change admission: an unknown
original trigger parameter, unknown live parameter, runtime effect mismatch, copied/generated/spawned provenance,
cloned source, non-empty initial targets, target-bound mismatch, or chooser mismatch is rejected as
`UNSUPPORTED_TARGETED_TRIGGER` rather than guessed through.

The admission and bound tests are in
`forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetDecisionCoordinatorTest.java`; the public
profile fixture and external ownership test are in
`forge-gui-desktop/src/test/java/forge/view/FRL02KTriggeredTargetExternalOwnershipAuditTest.java`.

## 4. Runtime, native, and external ownership

### 4.1 Native path: nullable resolver is the Forge-preserving path

[BESTAETIGT] With a null controller resolver, the coordinator performs one provider generation for an admitted Blood
trigger. Only a `DECISION` generation enters `NATIVE_WITH_TEACHER_CAPTURE`: that generation supplies the immutable
teacher request/candidate view and the `List.copyOf` pre-target snapshot, records one `DECISION_TRACE_V2` request,
and then invokes the existing native Forge-AI target adapter exactly once. `completeNative` compares the
post-callback target list by object identity, requires exactly one newly added object, and maps that identity to
exactly one target candidate from the captured request. A false native result or any missing identity
mapping is `MAPPING_FAILED`; it is not converted into an external choice.

For the admitted empty initial state, `INVALID_TARGETING` is the native no-stack result. It is returned as the
native preparation outcome without a `DecisionRequest`, teacher capture, or trace capture; it does not become an
external request. `COMPLETE` on that state would contradict the empty initial target list and minimum-one profile
requirement, so it is an integrity failure (`TARGET_APPLICATION_INCOMPLETE`), not a native or external success.

This keeps the existing `brains.doTrigger` behavior and no-stack Blood route. It is teacher capture/diagnostic
mapping, not external policy ownership.

### 4.2 External path: resolver owns the target once

[BESTAETIGT] With a non-null controller resolver, only the admitted Blood profile can become `PREPARED`:

| Runtime shape | C2A result |
|---|---|
| No trigger or non-targeted ability | `NOT_APPLICABLE`; leave the normal native Forge path available. |
| Targeted but unsupported profile | Fail closed with `UNSUPPORTED_TARGETED_TRIGGER`; no resolver call and no Forge-AI fallback. |
| Zero legal Graveyard candidates | Provider returns `INVALID_TARGETING`; no impossible external request is exposed and no stack entry is created. The null-resolver path preserves Forge's native no-stack result. |
| One legal candidate | Request is forced; coordinator applies the sole candidate exactly once and records engine-forced terminal history without a policy callback. |
| Many legal candidates | Resolver is called once with the exact request; the selected candidate must be a member `TARGET_CARD` in that request, and provider `apply` is called exactly once. |

The external route never invokes the Forge-AI target callback, so external target A cannot be silently replaced by a
Forge-AI target. The applied live `TargetChoices` remains authoritative through the later optional trigger decision
and effect resolution. The later confirmation-time temporary evaluation is not a second C2A request. If the stored
target becomes illegal at stack time, Forge fizzles/clears the stale target according to its normal legality path;
C2A does not retarget or select a replacement.

[BESTAETIGT] A `RuntimeException` from `resolver.resolve(request)` is sanitized to
`INVALID_EXTERNAL_CANDIDATE`. It never falls back to the native Forge-AI callback and is never reclassified as
`MAPPING_FAILED`. The already-open `DECISION_TRACE_V2` request has no selected candidate; normal finalization leaves
the trace open for `TRACE_INCOMPLETE`. Resolver exception text and other private details are not exported.

[BESTAETIGT] The P2 retained coordinator tests now cover the external orchestration cardinalities without changing
the provider/coordinator ownership split: zero legal candidates return `NO_STACK` from `INVALID_TARGETING` without
resolver, native callback, request, or result capture; one forced candidate uses the request's `isForced` flag,
calls provider `apply` exactly once, requires `COMPLETE`, leaves exactly one live target, and records external
engine-forced `FORCED` provenance; many strategic candidates sanitize a provider-`apply` exception to
`INVALID_EXTERNAL_CANDIDATE`, leave the trace `TRACE_INCOMPLETE`, and do not create a `MAPPING_FAILED` result.

### 4.3 Final quality-review fail-closed hardening

[BESTAETIGT] At current C2A-R verification HEAD `1d7216a93a8`, a non-null controller resolver makes cyclic parent
chains fail closed before provider, resolver, native, chooser, order, or stack paths. The boundary throws the
sanitized `UNSUPPORTED_PROFILE` result; direct coordinator preparation and direct `playTrigger` use the same cycle
gate. With `resolver == null`, ordinary unsupported profiles preserve the native Forge fallback. A malformed cyclic
ability is a narrow safety disposition: `orderAndPlaySimultaneousSa` drops the actual unclassifiable ordered output
before generic stack insertion, while direct `playTrigger` returns no-stack before `brains.doTrigger`. Neither route
is native execution or external ownership transfer, and the malformed Forge cyclic graph is not made natively
executable.

[BESTAETIGT] R4's supported external production route remains covered through
`PlayerControllerAi.orderAndPlaySimultaneousSa`: `externalOwnershipThroughOrderAndPlayUsesLiveTargetBeforeResolution`
invokes the resolver once before stack insertion, keeps the native target callback at zero, places the selected target
on the live underlying ability before resolution, resolves the queued trigger without resolver re-entry, and verifies
the `DECISION_TRACE_V2` external `CHOSEN` result. The resolver-null regressions are named
`cyclicAbilitySubOrderedOutputIsDroppedWithoutResolver` and
`cyclicWrappedAbilityDirectPlayTriggerDropsUnclassifiableRouteWithoutResolver`: the first reaches ordering with an
actual malformed cycle and drops the unclassifiable output before generic stack insertion; the second returns
no-stack before `brains.doTrigger`. Both keep provider/resolver/native/chooser callbacks at zero. This is a narrow
malformed-graph no-stack safety disposition, not a safe substitute for target selection, not native execution, and
not an external ownership transfer.

[BESTAETIGT] The same boundary carries a `triggeredAncestor` context through generic child edges. A targeted
non-`AbilitySub` additional child, including an `AbilityApiBased` child with `TargetRestrictions`, is rejected with
`UNSUPPORTED_PROFILE` before resolver/provider/native/chooser/order/stack routes when a resolver is active. A
standalone non-trigger remains `NOT_APPLICABLE`, and the live ability inside an admitted `WrappedAbility` is not
preflighted independently as a non-wrapper.

[BESTAETIGT] The child-edge context now propagates only after the cycle-safe root admission has established that
the root is an actual trigger root. An ordinary copied spell/ability with a targeted child therefore keeps native
order/stack ownership under a configured resolver, with no C2A resolver/provider/native target route. Exact,
wrapped, and non-wrapped actual trigger roots retain fail-closed rejection for targeted descendants.

[BESTAETIGT] At the same coordinator boundary, unexpected provider generation failures for an admitted Blood
capture and provider application failures in the forced path are sanitized to `TARGET_APPLICATION_INCOMPLETE`;
already-sanitized `TriggeredTargetIntegrityException` values are preserved. The separate normal `INVALID_TARGETING`
result and the external strategic-apply `INVALID_EXTERNAL_CANDIDATE` mapping remain unchanged. The regressions
assert that host/reason text is not exposed, no stack push or resolver/native fallback occurs, and no
`MAPPING_FAILED` trace entry is manufactured.

## 5. Trace and integrity contract

[BESTAETIGT] C2A uses `DECISION_TRACE_V2` only. No V3 schema or new BC sample rule was introduced.

| Path | `nativeCallbackCompleted` | `mappingAttempted` | Terminal history | BC policy sample |
|---|---:|---:|---|---:|
| Native target mapped | `true` | `true` | `CHOSEN` | Eligible only when not forced and otherwise valid. |
| External target selected | `false` | `false` | `CHOSEN` | No. This is valid external history, not native behavior-cloning data. |
| Forced target | Engine-forced or native `true/true` | See path | `FORCED` | No; forced history is excluded. |
| Native callback could not map | `true` | `true` | `MAPPING_FAILED` | No; native mapping failure only. |
| Invalid external candidate | `false` | `false` | Sanitized to no selected candidate and `TRACE_INCOMPLETE` after finalization | No; it is not mislabeled as `MAPPING_FAILED`. |

`DecisionTraceTrainingValidator` preserves the existing rule: a BC sample is a valid, non-forced `CHOSEN` result
with a legal selected semantic key and native/mapping flags `true/true`. External TARGET history is valid only with
`false/false`, and forced results remain non-BC. Duplicate terminals, unknown request references, missing
terminals, illegal selected keys, and duplicate semantic candidate keys remain rejected.

[BESTAETIGT] The continuation guard runs before provider generation and before any resolver or native callback. The
current C2A-R fresh-JVM child checks both resolver modes and produces these exact outputs:

Null-resolver run:

```text
reason=UNSUPPORTED_ACTION_CONTINUATION
provider_requests=0
resolver_present=false
resolver_calls=0
native_calls=0
```

External-resolver run:

```text
reason=UNSUPPORTED_ACTION_CONTINUATION
provider_requests=0
resolver_present=true
resolver_calls=0
native_calls=0
```

The guard therefore does not invent a target request or attach a triggered target to a priority continuation. R1's
native-null cycle preservation and routing correction retain this fail-closed continuation boundary for both modes.
Evidence is in `forge-game/src/main/java/forge/game/decision/DeterminismTrace.java`,
`forge-game/src/main/java/forge/game/decision/DecisionTraceTrainingValidator.java`,
`forge-gui-desktop/src/test/java/forge/game/decision/DeterminismTraceV2Test.java`, and
`forge-gui-desktop/src/test/java/forge/game/decision/TriggeredTargetContinuationProcessTest.java`.

## 6. Evidence matrix

The local XML reports under `forge-gui-desktop/target/surefire-reports/junitreports/` are generated focused-gate
artifacts, not a replacement for source or commit history. They contain no exported hidden host/card state; this
audit likewise records only the public profile and the two named public fixture choices required for ownership
comparison.

| Gate / checkpoint | Completed outcome | Evidence and qualification |
|---|---|---|
| Previous quality-review starting checkpoint | `[BESTAETIGT]` clean requested branch/worktree; checkpoint HEAD `92384c6865d27c23df671de538b20feb1f58b2f0`; base `3851fdf3825`; branch ahead 28 | Historical checkpoint from the preceding quality review |
| P2 follow-up starting checkpoint | `[BESTAETIGT]` clean HEAD `b9b7e6da3dbaaca2a323795071d551c3e3097bf0`; branch ahead 32 | `git status --short --branch`, `git rev-parse HEAD`, and `git rev-list --left-right --count origin/master...HEAD` |
| Current C2A-R verification anchor | `[BESTAETIGT]` code/test HEAD `1d7216a93a8` | Current code/test verification includes the resolver-null regressions `cyclicAbilitySubOrderedOutputIsDroppedWithoutResolver` and `cyclicWrappedAbilityDirectPlayTriggerDropsUnclassifiableRouteWithoutResolver`; this final refresh changes only this audit document |
| Quality-review RED baseline | `[STARKES INDIZ]` historical prior baseline 20 total; 17 passed, 3 failed, 0 errors, 0 skips | Test-only RED run for the preceding review; its three failures were corrected before the prior latest review |
| Latest-review RED/GREEN gate | `[STARKES INDIZ]` historical RED 50 total; 47 passed, 3 failed, 0 errors, 0 skips; GREEN 50/50 with 0 failures/errors/skips | Test-only RED/GREEN gate for the preceding review; not the current P2 follow-up selector |
| P2 retained external orchestration tests | `[BESTAETIGT]` 3/3 | `TriggeredTargetDecisionCoordinatorTest`: external zero-target `NO_STACK`, forced one-target provider completion/engine-forced provenance, and strategic many-target provider-apply sanitization; no production code changed |
| Provider/API historical aggregate | `[STARKES INDIZ] 32/32` | `TargetDecisionProviderTest` 27/27 + `FRL02KTriggeredTargetProviderAuditTest` 3/3 + `DecisionPublicApiReflectionTest` 2/2 in the retained JUnit reports from the earlier gate; current A/B/R2 selectors are recorded below |
| Task 5 coordinator checkpoint | `[BESTAETIGT] 15/17` before Task 6; two request failures were explicitly deferred | Retained Task 5 gate outcome; the later correction/orchestration commits are `c2779afa449`, `9b5367dcea8`, and `0f85ab32582` |
| Task 6 / Task 10 coordinator | `[STARKES INDIZ]` historical checkpoint 28/28 | `TriggeredTargetDecisionCoordinatorTest`; includes native 0/1/many and five native mapping-failure tests: callback false, zero new targets, multiple new targets, foreign target, and the duplicate-target setup that reaches the multiple-new-target guard. Forge rejects the duplicate live identity; this case does not construct an ambiguous identity mapping |
| Task 6 focused gate | `[STARKES INDIZ]` historical checkpoint 26/26 after Task 8; pre-Task 8 was 25/26 with one known validator RED | Completed post-Task 8 focused gate; no broad reactor/build result is inferred |
| Task 8 validator/continuation historical gate | `[STARKES INDIZ] 10/10` = V2 validator/trace 9/9 plus fresh-JVM continuation 1/1 | Historical Task 8 result; the current continuation coverage is included in A below and exact current child outputs are in section 5 |
| External ownership and boundary regressions | `[BESTAETIGT] 23/23` | `FRL02KTriggeredTargetExternalOwnershipAuditTest`; includes native/external ownership, five invalid-candidate cases, throwing-resolver sanitization, resolver-active cyclic-parent fail-closed protection, the resolver-null `cyclicAbilitySubOrderedOutputIsDroppedWithoutResolver` and `cyclicWrappedAbilityDirectPlayTriggerDropsUnclassifiableRouteWithoutResolver` no-stack regressions, ordinary copied targeted-child native order/stack ownership, copied/non-wrapped/Charm/nested-child rejection, four additional-child routes (`TrueSubAbility`, `FalseSubAbility`, `FallbackAbility`, and a non-`Choices` additional list), direct preparation/`playTrigger` cycle rejection, the targeted non-`AbilitySub` child fixture, and the R4 production `orderAndPlaySimultaneousSa` route. All resolver/native/chooser/order/stack fallback counters remain zero on the fail-closed and malformed-cycle routes. |
| Throwing-resolver focused gate | `[BESTAETIGT] 1/1` | `throwingResolverFailsClosedWithoutNativeFallbackOrMappingFailure`; `RuntimeException` is sanitized to `INVALID_EXTERNAL_CANDIDATE`, with no native fallback and no `MAPPING_FAILED` reclassification |
| Current focused C2A suite (A) | `[BESTAETIGT] 94/94` | At current C2A-R verification code/test HEAD `1d7216a93a8`, the exact selector ran `TriggeredTargetDecisionCoordinatorTest` 33/33, `TriggeredTargetContinuationProcessTest` 2/2, `FRL02KTriggeredTargetExternalOwnershipAuditTest` 23/23, `DeterminismTraceV2Test` 9/9, and `TargetDecisionProviderTest` 27/27; 0 failures, 0 errors, 0 skips. The focused report total is `94`, passed `94`, failed `0`, skipped `0`, including both resolver-null malformed-cycle no-stack regressions. |
| R2 integrity-exception API selector | `[BESTAETIGT] 1/1` | `TriggeredTargetIntegrityExceptionApiTest` verifies that the coarse `Status` type, field, and getter are absent and that `Reason` is the only machine-readable classification; 0 failures, 0 errors, 0 skips. |
| Current retained/API/confirmation subtotal | `[BESTAETIGT] 20/20` | The retained/API/confirmation classes in B ran `FRL02KTriggeredTargetProviderAuditTest` 3/3, `DecisionPublicApiReflectionTest` 2/2, `PriorityActionDiagnosticsTest` 11/11, and `forge.ai.ability.FRL02KConfirmationAuditTest` 4/4; 0 failures, 0 errors, 0 skips. |
| Current exact B selector including ownership | `[BESTAETIGT] 21/21` | The requested B selector ran the retained/API/confirmation subtotal plus `FRL02KTriggeredTargetOwnershipAuditTest` 1/1; total `21`, passed `21`, failed `0`, skipped `0`. |
| Current canonical native ownership workload (C) | `[BESTAETIGT] 1/1` | Fresh child-JVM `forge.view.Main sim` workload: `Izzet Guild Kit` vs `Dimir Guild Kit`, 10 games, seed `20260810`, once with the public triggered-target audit file and once without it. The native-only fixture asserts two Blood occurrences, exact lifecycle/A-B ordering, both effects accepted, one stored A target matching temporary B and one differing, typed public projections with no raw engine/localized data, `action_continuation=false`, `state_neutral=true`, `rng_delta=0` for every row, and identical audit/control determinism trees. The workload exited `0`; neither child timed out. |
| Current broad reactor tests (D) | `[BESTAETIGT] 743 total; 737 passed; 0 failures; 0 errors; 6 skips` | Exact `mvn -pl forge-gui-desktop -am test` exited 0 in `719 s` (about `11:59`) after the 1d direct malformed-cycle fix. Module reports: `forge-ai` 20/20, `forge-game` 16/16, and `forge-gui-desktop` 707 total with 701 passed and 6 skips; reactor total is 743/737/0/0/6. The malformed-cycle dispositions are no-stack safety routes, not native execution or external ownership transfer. All six skips are configured `NetworkPlayIntegrationTest` methods requiring `-Drun.stress.tests=true`: `analyzeLog`, `runComprehensiveDeltaSyncTest`, `runQuickDeltaSyncTest`, `testConfigurableParallel`, `testUnifiedHarnessLocalMode`, and `testConfigurableSequential`. |
| Current package (E) | `[BESTAETIGT]` exit 0 at current code/test HEAD `1d7216a93a8` | `mvn -pl forge-gui-desktop -am -DskipTests package` exited 0 after 1d. Package and validate were recorded as a combined verification of about 55 s; no per-command duration is assigned. |
| Current validate/checkstyle (F) | `[BESTAETIGT]` exit 0; 0 Checkstyle violations | `mvn -pl forge-gui-desktop -am validate` exited 0 after 1d with 0 Checkstyle violations. The current document `git diff --check` is clean. |
| Current canonical fixture identity | `[BESTAETIGT]` | C used `Izzet Guild Kit` vs `Dimir Guild Kit`, 10 games, seed `20260810`; the fixture is native-only because the test child process configures audit/trace properties and does not install an external target resolver. |

### 6.1 C2A-R current verification commands

The following focused and broad commands ran from `C:\forgeAI-triggered-target-c2a` against current code/test HEAD
`1d7216a93a8`:

```text
mvn -pl forge-gui-desktop -am '-Dtest=TriggeredTargetDecisionCoordinatorTest,TriggeredTargetContinuationProcessTest,FRL02KTriggeredTargetExternalOwnershipAuditTest,DeterminismTraceV2Test,TargetDecisionProviderTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
mvn -pl forge-gui-desktop -am '-Dtest=FRL02KTriggeredTargetProviderAuditTest,FRL02KTriggeredTargetOwnershipAuditTest,DecisionPublicApiReflectionTest,PriorityActionDiagnosticsTest,FRL02KConfirmationAuditTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
mvn -pl forge-gui-desktop -am '-Dtest=FRL02KTriggeredTargetOwnershipAuditTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
mvn -pl forge-gui-desktop -am test
git diff --check
git status --short --branch
git rev-parse HEAD
```

The separate R2 selector was `TriggeredTargetIntegrityExceptionApiTest` 1/1. Recorded outcomes were, in order:
A 94/94, 0 failures, 0 errors, 0 skips; R2 1/1; B 21/21, 0 failures, 0 errors, 0 skips; C 1/1 with exit 0;
D reactor 743 total, 737 passed, 0 failures, 0 errors, and 6 configured skips. The broad D run exited 0 in 719 s
(about 11:59); the resolver-null malformed-cycle no-stack regressions are included in the current focused/broad
code/test anchor;
the six skips are the configured `NetworkPlayIntegrationTest` cases listed in the evidence matrix. No failure, error,
or timeout is recorded for these current gates.

The generated current reports reconcile A and B through `forge-gui-desktop\target\surefire-reports\testng-results.xml`,
the R2 API report under `forge-game\target\surefire-reports\`, and the module TestNG reports used for D. Older
report files in the same report directories were not used to inflate or replace the current counts.

The current package command, run after code/test HEAD `1d7216a93a8`, was:

```text
mvn -pl forge-gui-desktop -am -DskipTests package
```

It exited 0. Package and validate were recorded as a combined verification of about 55 s; no per-command duration
is assigned.

The current validate command was run after code/test HEAD `1d7216a93a8` and exited 0 with 0 Checkstyle violations:

```text
mvn -pl forge-gui-desktop -am validate
```

The current document `git diff --check` is clean.

The current continuation child emits the two resolver-mode outputs recorded in section 5. The non-null child installs
a controller resolver that increments an `AtomicInteger` and returns `null`; the zero resolver count is therefore an
observed boundary result rather than a hard-coded fixture value.

The prior audit's 84-test A aggregate was a historical individual JUnit/TestNG snapshot and is not a current count.
The current A reports record `TriggeredTargetDecisionCoordinatorTest` 33/33,
`TriggeredTargetContinuationProcessTest` 2/2, `FRL02KTriggeredTargetExternalOwnershipAuditTest` 23/23,
`DeterminismTraceV2Test` 9/9, and `TargetDecisionProviderTest` 27/27; the current full named C2A selector is 94/94.
The separate R2 API selector is 1/1. B's retained/API/confirmation subtotal is 20/20, and the exact requested B
selector is 21/21 after adding the separate ownership audit 1/1. The coordinator-seam tests assert the external
zero/one/many orchestration evidence, the resolver-active cycle gate, the resolver-null malformed-cycle no-stack
regressions `cyclicAbilitySubOrderedOutputIsDroppedWithoutResolver` and
`cyclicWrappedAbilityDirectPlayTriggerDropsUnclassifiableRouteWithoutResolver`, and the R4 production
`orderAndPlaySimultaneousSa` route;
the external-ownership methods continue to assert the native/external choice and callback-count evidence. None invoke
the native Forge-AI callback on an externally owned route, and the malformed-cycle regressions do not transfer
ownership externally or claim native execution.

The canonical native workload command represented by C is:

```text
java [diagnostic properties] -cp ..\forge-gui-desktop\target\forge-gui-desktop-2.0.14-SNAPSHOT-jar-with-dependencies.jar forge.view.Main sim -d "Izzet Guild Kit" "Dimir Guild Kit" -n 10 -s 20260810 -q
```

The canonical native workload exited `0`.

## 7. Remaining scope

[BESTAETIGT] C2A supports only the exact Blood Operative ETB target profile in this document. The following remain
open and must not be inferred from the focused target gates:

- global triggered `TARGET` admission;
- Blood `CONFIRMATION` ownership;
- global `CONFIRMATION` ownership;
- any copied, granted, hidden, static, generated, alternate-chooser, multi-effect, or otherwise different Blood shape;
- the six explicitly skipped `NetworkPlayIntegrationTest` stress cases unless a separate run enables
  `-Drun.stress.tests=true`.

Blood is not agent-complete. Its ETB target is supported only within the exact profile and only as `TARGET`; the
later optional trigger decision remains outside this C2A boundary.

## Narrow status

```text
Blood Operative ETB TARGET: SUPPORTED (exact profile only)
global triggered TARGET: OPEN
Blood CONFIRMATION: OPEN
global CONFIRMATION: OPEN
Current C2A-R gates: FINAL_VALIDATED (code/test HEAD 1d7216a93a8; A 94/94; R2 API 1/1; B 21/21 exact selector with 20/20 retained/API/confirmation subtotal; C 1/1 canonical native workload, exit 0; D 743 total, 737 passed, 0 failures, 0 errors, 6 configured skips; resolver-null malformed-cycle no-stack regressions `cyclicAbilitySubOrderedOutputIsDroppedWithoutResolver` and `cyclicWrappedAbilityDirectPlayTriggerDropsUnclassifiableRouteWithoutResolver` included; package exit 0; validate/checkstyle exit 0 with 0 violations; combined package/validate verification about 55 s; current document diff check clean)
C2A-R focused/retained/canonical/broad: CURRENT PASS (the resolver-null malformed-cycle regressions are narrow safety dispositions: the ordered unclassifiable output is dropped before generic stack insertion and direct `playTrigger` returns no-stack before `brains.doTrigger`; neither is native execution or external ownership transfer; six configured NetworkPlayIntegrationTest skips remain accepted unless stress validation is explicitly requested)
FRL_02K_C2A_FINAL_VALIDATED
```
