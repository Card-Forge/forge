# FRL-02L1 Exact SIMULTANEOUS_TRIGGER_ORDER Design Checkpoint

Status: DESIGN_APPROVED
Implementation status: FRL_02L1_PASS

## Checkpoint

- Repository: chrismaghuhn/forgeAI
- Base: 9200349284b3489a6a349de378c773bdfa2f6efc
- Protected checkout: C:\forgeAI (out of scope for edits)
- Worktree: C:\forgeAI-order-l1
- Branch: frl/02l1-simultaneous-trigger-order
- Baseline: mvn -pl forge-gui-desktop -am test
- Baseline result: 727 tests, 0 failures, 0 errors, 6 skipped

The protected checkout remains clean and at the locked base. This design and
the later implementation are isolated to the fresh worktree above.

The approved design gate is:

~~~text
P0 = 0
P1 = 0
Approach 1 = approved
19/19 exact SIMULTANEOUS_TRIGGER_ORDER sessions admitted = required
26 exact L1 ORDER requests = required
raw multi-item callback surface = 20
one separately attributed copied-spell callback remains outside L1
~~~

## 1. Objective and non-goals

FRL-02L1 adds one exact decision profile for engine-generated simultaneous
trigger ordering:

~~~text
SIMULTANEOUS_TRIGGER_ORDER
direction = RESOLVE_FIRST
~~~

It exposes the existing native Forge ordering decision through a typed,
controller-local request boundary. A native full permutation is captured once
and decomposed into sequential n-1 teacher-labelled requests. An external
resolver chooses the same semantic RESOLVE_FIRST candidates one request at a
time. The final native insertion order is reconstructed privately through one
central LIFO translation.

This milestone does not add:

- SURVEIL_PARTITION_PLUS_ORDER or any Surveil partition semantics;
- COPY_SPELL_RESOLVE_FIRST_ORDER; the exact Replicate/`CopySpellAbilityEffect`
  seam is discovered by FRL-02L1R2 but requires a separate design checkpoint;
- generic permutation or generic ORDER provider APIs;
- combatant ORDER, damage assignment, DAMAGE_ASSIGNMENT, or combat-rule
  activation;
- changes to MagicStack grouping, APNAP handling, or stack insertion;
- descriptions, stack text, raw trigger objects, SpellAbility, Card, CardLKI,
  GameObject, Java identity, or engine state in the public ML contract;
- ActionContinuation, decisionSequenceId, or subdecisionIndex support for this
  stack-preparation flow;
- a global/static resolver, a generic callback adapter, or an AI heuristic that
  manufactures a candidate domain.

Existing B1 confirmation, C2A triggered-target, and D1 Blood confirmation
boundaries remain intact. ORDER here is not DAMAGE_ASSIGNMENT.

## 2. Authoritative production seam

The production call path is fixed:

~~~text
MagicStack
  -> orderAndPlaySimultaneousSa(activePlayerSAs)
  -> orderSimultaneousSa(activePlayerSAs)
  -> existing native stack insertion
~~~

MagicStack remains unchanged. Its existing grouping semantics are the source of
truth. The PlayerControllerAi route remains a thin router: it delegates
admission and decision ownership to the typed coordinator, then preserves the
existing target handling, copied ability handling, and stack insertion path.

The exact family is:

~~~text
SimultaneousTriggerOrderDecisionCoordinator
SimultaneousTriggerOrderDecisionProvider
SimultaneousTriggerOrderContext
SimultaneousTriggerOrderItem
OrderCandidateKind
OrderDirection
~~~

Add DecisionType.ORDER only for this exact request profile. No generic
permutation provider is introduced.

## 3. Exact admission boundary

Admission is a complete, fail-closed projection of the one native input
snapshot. It either admits the entire strategic session or admits nothing.
There is no per-entry native fallback after a partial projection.

### 3.1 Effective ordering player

Define and use one named function/invariant:

~~~text
effectiveOrderingPlayer(entry)
~~~

Its semantics must mirror the grouping expression already used by MagicStack:

~~~text
activatingPlayer = entry.getActivatingPlayer()
if activatingPlayer == null:
    activatingPlayer = entry.getHostCard().getController()
~~~

For every admitted entry:

~~~text
effectiveOrderingPlayer(entry) == choosingPlayer
~~~

All entries in the callback must have the same effective ordering player. L1
must not maintain a second, subtly different owner definition.

### 3.2 v0 shape

The exact L1 profile admits only the strategic v0 shape proven by source
inspection and the canonical workload:

- input is a non-null list with n >= 2 entries after the forced pre-admission
  path has been handled;
- each entry is a supported native WrappedAbility trigger entry;
- the wrapped trigger is a supported non-static simultaneous trigger;
- the source is face-up and publicly projectable to the choosing player;
- the source projection is a CardSelectionCard;
- TriggerType and underlying ApiType are available as enum/value fields;
- the effective ordering player is available and agrees for every entry;
- no active continuation, hidden source, unsupported trigger shape, or
  unprojectable value is present.

The WrappedAbility restriction is an exact v0 admission condition only. If
canonical source inspection or the locked workload identifies a real strategic
v0 group with another entry type, silently filtering it is forbidden. The
FRL-02L1R2 audit has identified exactly one such group: the copied-spell
resolve-first seam. It is recorded as a separate player-owned semantic profile,
not as a missing L1 admission; the 19/19 L1 gate measures only the exact
trigger profile.

### 3.3 Public duplicate policy and private integrity

The public projection does not identify a native entry. The request-local item
ID does.

These two distinct native entries are valid even when their public values are
identical:

~~~text
entry #17 -> source X, trigger Y, api Z
entry #18 -> source X, trigger Y, api Z
~~~

They receive different item IDs and both remain legal candidates. This is the
required duplicate-looking-trigger case.

The following is an integrity failure:

~~~text
the same native SpellAbility/entry identity appears twice in one snapshot
~~~

The native identity guard is private and is never serialized or exposed to an
agent. Descriptions and stack text are not used for identity or duplicate
detection.

Native identity validation is a separate integrity phase, not ordinary
unsupported admission. A null entry, a repeated native entry identity, or any
other malformed snapshot identity invariant is:

~~~text
SESSION_INTEGRITY_FAILURE
resolver callback = 0
native orderPlaySa callback = 0
no L1 request and no stack insertion
~~~

This hard-fails regardless of resolver ownership. It must not enter the native
fallback path. Unsupported projection, hidden source, and unsupported future
shape remain `UNSUPPORTED_ADMISSION` and retain the ownership matrix below.

### 3.4 Admission ownership matrix

The resolver is consulted only after exact admission:

| Admission | Resolver | Ownership and result |
|---|---|---|
| succeeds | null | native teacher ownership; one native callback; L1 trace requests are emitted |
| succeeds | present | external ownership; resolver is captured once for the whole session; native callback is zero |
| unsupported admission | null | zero L1 requests; invoke existing native `orderPlaySa` exactly as before and return its native result |
| unsupported admission | present | sanitized hard failure; resolver callback is zero; native callback is zero; no stack insertion |
| session integrity failure | either | sanitized hard failure; resolver callback is zero; native ordering callback is zero; no stack insertion |

Admission failure is not MAPPING_FAILED. MAPPING_FAILED is reserved for a
native callback that completed but could not be mapped to the admitted
snapshot. External resolver failures are external hard failures and never
fall back to native ownership.

The resolver-null fallback is a literal delegation to the existing native
`orderPlaySa` path. It preserves that path's supplied-list mutation, RNG
consumption, heuristic ordering, returned list identity/order, and exception
behavior. L1 must not substitute the original input or a normalized copy.

### 3.5 Cardinality and forced pre-admission path

Cardinality is handled before strategic L1 admission. A singleton is not an
unsupported strategic projection.

~~~text
null list
  malformed input
  resolver == null -> invoke existing native orderPlaySa exactly as before;
                     preserve its baseline behavior, including any baseline
                     exception or input handling
  resolver != null -> sanitized hard failure; resolver/native callbacks = 0;
                     no L1 request and no stack insertion

n = 0
  no strategic session
  0 ORDER requests
  resolver != null -> resolver = 0, native callback = 0, return empty result
  resolver == null -> invoke existing native orderPlaySa exactly as today and
                     return its native result

n = 1
  no strategic session
  0 ORDER requests
  resolver != null -> resolver = 0, native callback = 0, return the sole entry
                     unchanged
  resolver == null -> invoke existing native orderPlaySa exactly as today and
                     return its native singleton result unchanged

n >= 2
  perform private identity validation and strategic L1 admission
~~~

The resolver is never consulted for n <= 1. The native path for n <= 1 is
still called exactly where the current controller path calls
`AiController.orderPlaySa`; L1 does not replace, normalize, or short-circuit
that native behavior for resolver-null ownership. For n >= 2, only
`UNSUPPORTED_ADMISSION` may use that same native fallback. A
`SESSION_INTEGRITY_FAILURE` never does.

## 4. Public contract

### 4.1 SimultaneousTriggerOrderItem

The public item is value-only:

~~~text
itemId: session-local deterministic ordinal assigned once from the immutable
        initial snapshot
source: CardSelectionCard
triggerType: TriggerType
effectApi: ApiType
~~~

The contract contains no description, stack description, trigger object,
SpellAbility, WrappedAbility, Card, CardLKI, GameObject, Java identity, or
private native handle. CardSelectionCard is used only when the visible face-up
source can be safely projected.

itemId is assigned once before step 0 and remains stable in every later
remaining set in the session. It is unique within the session snapshot and is
the only public identity for an item. Two items may have the same source,
trigger type, and API.

### 4.2 SimultaneousTriggerOrderContext

The context contains session metadata only:

~~~text
profile = SIMULTANEOUS_TRIGGER_ORDER
direction = RESOLVE_FIRST
orderSessionId
stepIndex
originalItemCount
choosingPlayerId
decisionSequenceId = null
subdecisionIndex = null
~~~

The context does not duplicate the remaining set. DecisionRequest.candidates
is the complete and authoritative current remaining set. No state in which a
context list and candidate list disagree is representable.

orderSessionId is opaque, controller-local correlation for one ORDER session.
It is not an ActionContinuation, not a gameplay identity, and not a cross-game
identifier. It must be deterministic: use a controller/game-local monotonic
deterministic counter or existing deterministic request-ID infrastructure.
UUIDs, wall-clock time, System.nanoTime, identityHashCode, and RNG are
forbidden.

### 4.3 ORDER request invariant

For a remaining item count n >= 2, every request satisfies:

~~~text
DecisionType.ORDER
profile = SIMULTANEOUS_TRIGGER_ORDER
direction = RESOLVE_FIRST
forced = false
exactly one SELECT_RESOLVE_FIRST candidate per remaining item
candidate count == remaining item count >= 2
semanticKey = RESOLVE_FIRST|<itemId>
ActionContinuation = null
decisionSequenceId = null
subdecisionIndex = null
~~~

For n <= 1, the forced pre-admission path emits zero ORDER requests. The final
remaining item in an admitted session is forced internally and is never
exposed as a one-candidate request.

Candidates are serialized in deterministic item order. The set, cardinality,
item IDs, candidate kinds, and semantic keys are all validated on every
response.

## 5. Session lifecycle and ownership capture

The coordinator creates one session from one admitted native snapshot:

~~~text
snapshot -> public items -> deterministic orderSessionId -> step 0
~~~

The selected resolver/provider is captured exactly once. A later controller
setter mutation cannot change ownership for the active session. The session
state is controller/request/session-local and non-static.

### 5.1 External owner

For n items, external ownership performs exactly n-1 requests:

~~~text
remaining = all items
while remaining.size >= 2:
    request current remaining set
    validate one SELECT_RESOLVE_FIRST item from remaining
    emit CHOSEN for that step
    remove selected item
force the final remaining item internally
translate semantic order once to native insertion order
~~~

The resolver is called once per request and never for the forced final item.
The native AI callback is zero. No native stack insertion occurs until all
external choices have validated and the full semantic order has been
translated.

Any null, foreign, previously selected, wrong-kind, stale, duplicate, or
throwing external result is a sanitized hard failure. If the current request
has already been emitted, it is terminalized exactly once as:

~~~text
INVALID_EXTERNAL_CANDIDATE
nativeCallbackCompleted = false
mappingAttempted = false
~~~

There is no native fallback, no partial stack insertion, and no
`TRACE_INCOMPLETE` result for this path.

### 5.2 Native owner

Native ownership uses one full-order callback:

~~~text
snapshot [A,B,C,D]
REQUEST step 0 [A,B,C,D]

native orderPlaySa exactly once
validate the complete native permutation
convert once to semantic RESOLVE_FIRST

RESULT step 0
REQUEST step 1 [remaining]
RESULT step 1
REQUEST step 2 [remaining]
RESULT step 2
force final item
return original native insertion order
~~~

The native list is captured before the existing native operation can mutate
its supplied list. The native callback is invoked exactly once for the
admitted n >= 2 session. The full permutation is validated for exact size,
membership, multiplicity, and private native identity before any later teacher
label is published.

The trace lifecycle is intentionally incremental. Request step 0 exists before
the native callback. Only after the complete permutation validates is step 0
terminalized and step 1 created. Later requests are created one at a time from
their exact remaining sets. They are not prebuilt before the native result is
known.

If `orderPlaySa` throws before returning a native permutation, the active step
0 is terminalized exactly once as:

~~~text
NATIVE_CALLBACK_FAILURE
nativeCallbackCompleted = false
mappingAttempted = false
~~~

The failure is sanitized, no later request is created, no native fallback or
stack insertion occurs, and the result is not `MAPPING_FAILED`.

If the native permutation is invalid, only the active step 0 is closed as:

~~~text
MAPPING_FAILED
nativeCallbackCompleted = true
mappingAttempted = true
~~~

No step 1/step 2 request, teacher label, retry, sort, fallback, or stack
insertion is allowed. No part of an invalid native permutation is published as
a CHOSEN label.

### 5.3 Terminal request contract

Once a `REQUEST` record exists, that request receives exactly one terminal
result. L1 must never intentionally leave an emitted ORDER request as
`TRACE_INCOMPLETE`.

The implementation must extend the typed `DECISION_TRACE_V2` result
representation with the terminal ORDER result kinds
`INVALID_EXTERNAL_CANDIDATE` and `NATIVE_CALLBACK_FAILURE`. They are not
encoded as generic `TRACE_INCOMPLETE` records:

| Terminal result | Meaning | Native callback | Mapping attempted |
|---|---|---:|---:|
| `CHOSEN` | valid native or external selection | owner-dependent | owner-dependent |
| `MAPPING_FAILED` | native callback returned, then full permutation mapping failed | `true` | `true` |
| `INVALID_EXTERNAL_CANDIDATE` | external resolver returned invalid data or threw | `false` | `false` |
| `NATIVE_CALLBACK_FAILURE` | native callback threw before returning a permutation | `false` | `false` |

`INVALID_EXTERNAL_CANDIDATE` and `NATIVE_CALLBACK_FAILURE` are terminal
failures, not incomplete traces and not teacher labels. Their public status,
exception, and trace reason are sanitized. `MAPPING_FAILED` is used only when
the native callback completed normally and the returned full permutation could
not be mapped to the immutable session snapshot. The training validator must
exclude all three failure kinds from teacher-label admission.

## 6. Central LIFO translation

The only native/semantic order conversion is a pure, centrally tested pair of
functions:

~~~text
toSemanticResolveFirst(nativeInsertionOrder)
toNativeInsertion(semanticResolveFirstOrder)
~~~

Native Forge stack insertion is LIFO. Therefore the semantic first-to-resolve
list is the reverse of the native insertion list, and the inverse operation is
also a reverse. No caller performs an ad hoc reverse.

The mandatory roundtrip lock is:

~~~text
toNativeInsertion(
    toSemanticResolveFirst(nativeOrder)
)
== nativeOrder
~~~

for n=2, n=3, and n=4, including duplicate-looking public projections whose
private native identities are different. The translation never relies on
descriptions, source names, Java identity, or candidate text.

The existing stack insertion route receives the reconstructed native order only
after the session has completed successfully. MagicStack behavior and LIFO
semantics remain unchanged.

## 7. Trace and diagnostics contract

Use the existing DECISION_TRACE_V2 lifecycle with a typed ORDER stage:

~~~text
stage = SIMULTANEOUS_TRIGGER_ORDER
profile = SIMULTANEOUS_TRIGGER_ORDER
direction = RESOLVE_FIRST
~~~

For native teacher selections:

~~~text
CHOSEN
nativeCallbackCompleted = true
mappingAttempted = true
~~~

For external selections:

~~~text
CHOSEN
nativeCallbackCompleted = false
mappingAttempted = false
~~~

For an external invalidity after a request exists, record the terminal
`INVALID_EXTERNAL_CANDIDATE` result with `false/false`. For a native callback
that throws before returning, record the terminal `NATIVE_CALLBACK_FAILURE`
result with `false/false`. Neither path may be represented as
`TRACE_INCOMPLETE`.

### 7.1 DecisionTraceTrainingValidator contract

The existing structural validator must treat ORDER history explicitly. Extend
its external `CHOSEN` allowlist from `CONFIRMATION`/`TARGET` to include
`DecisionType.ORDER`, without making any other decision type external by
default.

For an ORDER request, `request.forced` is always false. The exact validator
contract is:

| Result | `isHistoryValid` | `isBCPolicySample` | Required conditions |
|---|---:|---:|---|
| `ORDER + CHOSEN + false/false` | `true` | `false` | selected semantic key is contained in the request candidates |
| `ORDER + CHOSEN + true/true` | `true` | `true` | selected semantic key is contained in the request candidates |
| `INVALID_EXTERNAL_CANDIDATE` | `true` | `false` | selected key is empty; native and mapping flags are both false |
| `NATIVE_CALLBACK_FAILURE` | `true` | `false` | selected key is empty; native and mapping flags are both false |
| `MAPPING_FAILED` | `true` | `false` | existing empty-key, `true/true` semantics remain unchanged |

For both ORDER `CHOSEN` rows, the selected key must be legal and the request
must not be forced. `ORDER + CHOSEN + false/false` is valid external history
but is never a BC policy sample. `ORDER + CHOSEN + true/true` is valid native
teacher history and is a BC policy sample. The two terminal failure kinds are
structurally valid terminal history records so `validateRecords()` accepts a
complete trace with no `TRACE_INCOMPLETE` record, but neither is a training
label. Every request still requires exactly one terminal result.

The forced final item has no request and no synthetic CHOSEN result. The session
correlation uses orderSessionId; ActionContinuation, decisionSequenceId, and
subdecisionIndex remain absent/null.

All unsupported and integrity failures use sanitized status/reason values.
Raw resolver exception text, card/ability object strings, descriptions, native
lists, and private identities must not enter public requests, traces, or
diagnostics.

## 8. Preserved integration boundaries

The implementation must preserve these existing boundaries:

- C2A triggered-target ownership remains independent of ORDER ownership;
- B1 and D1 confirmation traces, provenance, and fail-closed behavior remain
  unchanged;
- target handling in orderAndPlaySimultaneousSa remains exactly once per
  ability and is not re-run by the ORDER coordinator;
- arrangeForSurveil remains native and emits zero L1 ORDER requests;
- legacy combat ordering remains unactivated and unchanged;
- assignCombatDamage remains untouched;
- no MagicStack grouping or APNAP behavior is moved into ML/provider code;
- no global decision provider or static session state is introduced;
- state, RNG, hashes, and deterministic replay are unchanged except for the
  explicitly observable deterministic decision trace/request data.

## 9. Failure, trace, and integration test matrix

The implementation plan must turn every row below into a focused test or an
explicit retained-test assertion. A test is not an acceptance substitute when
the production route is not exercised.

### 9.1 Public contract and pure functions

| Area | Required evidence |
|---|---|
| request shape | ORDER, exact profile, RESOLVE_FIRST, forced=false, null continuation fields |
| cardinality | n=2 gives one request; n=3 gives two; n=4 gives three; n<=1 gives zero |
| candidate authority | every request candidate list is exactly the current remaining set; no duplicated context list |
| key stability | session-local item IDs are assigned once from the immutable initial snapshot and remain stable across every step; semantic keys, candidate order, and request/session serialization are deterministic |
| session IDs | repeated equivalent runs produce equivalent deterministic orderSessionId values; no time/UUID/RNG source |
| duplicate-looking entries | distinct native identities with equal public projection are both valid and receive distinct item IDs |
| duplicate native identity | same native entry identity twice is SESSION_INTEGRITY_FAILURE |
| public API | reflection rejects raw engine/private types and accepts only the approved value contract |
| LIFO | pure native/semantic roundtrip passes for n=2,3,4 and duplicate-looking entries |

### 9.2 Admission and fail-closed behavior

| Input/failure | Resolver absent | Resolver present |
|---|---|---|
| null list | zero L1 requests; invoke existing native orderPlaySa exactly as before | sanitized failure before resolver/native |
| null entry or malformed snapshot identity | SESSION_INTEGRITY_FAILURE; native ordering callback zero | SESSION_INTEGRITY_FAILURE; resolver/native ordering callbacks zero |
| n = 0 | zero ORDER requests; invoke existing native orderPlaySa exactly as today | zero ORDER requests; resolver/native callbacks zero; return empty result |
| n = 1 | zero ORDER requests; invoke existing native orderPlaySa exactly as today and return its singleton result | zero ORDER requests; resolver/native callbacks zero; return sole entry unchanged |
| non-wrapper/non-trigger entry | zero L1 requests; native preserved | sanitized failure; no insertion |
| null host/source/API/trigger | zero L1 requests; native preserved | sanitized failure; no insertion |
| hidden or face-down source | zero L1 requests; native preserved | sanitized failure; no insertion |
| mixed effective ordering players | zero L1 requests; native preserved | sanitized failure; no insertion |
| unsupported trigger/API shape | zero L1 requests; native preserved | sanitized failure; no insertion |
| active continuation | zero L1 requests; existing native behavior | reject before resolver/native |
| canonical projection omission | milestone is PARTIAL, never hidden by native fallback; unsupported path invokes existing native orderPlaySa | milestone is PARTIAL, hard failure |

Admission failure is never mislabeled MAPPING_FAILED.

### 9.3 External session lifecycle

| Case | Required evidence |
|---|---|
| n=2 | resolver called once; one request; final item forced; native callback zero |
| n=3 | resolver called twice; remaining sets are exact; native callback zero |
| n=4 | resolver called three times; no request is created before its set exists |
| ownership capture | mutating the controller resolver after step 0 does not change the captured resolver |
| invalid choice | null, foreign, prior, duplicate, wrong-kind, stale, or throwing result terminalizes the active request as INVALID_EXTERNAL_CANDIDATE with false/false; no fallback or insertion |
| trace | every accepted external step is CHOSEN with false/false; invalid steps are terminal INVALID_EXTERNAL_CANDIDATE; no TRACE_INCOMPLETE or forced final result |
| completion | one complete semantic order is translated once and only then inserted |

### 9.4 Native session lifecycle

| Case | Required evidence |
|---|---|
| one callback | orderPlaySa is called exactly once for each admitted strategic session |
| snapshot isolation | native mutation does not corrupt the public snapshot or item IDs |
| trace timing | only step 0 exists before native callback; later requests are created after full validation |
| valid teacher | full permutation maps to RESOLVE_FIRST; each emitted step is true/true |
| invalid duplicate | only active step 0 becomes MAPPING_FAILED true/true; no later requests or insertion |
| invalid omission/foreign/size | same atomic MAPPING_FAILED behavior; no retry/sort/fallback |
| callback throws | only active step 0 becomes NATIVE_CALLBACK_FAILURE false/false; no later requests, fallback, or insertion |
| native result | returned native insertion order is exactly the original valid native order |

### 9.5 Real engine-route integration

| Route | Required evidence |
|---|---|
| two-item external stack route | selected resolve-first item is observed resolving first under real MagicStack LIFO insertion |
| three-item external route | exact sequence, two resolver calls, zero native callback, one final insertion |
| native/external equivalence | same semantic order produces the same native insertion and observable effect order |
| target coexistence | C2A target handling remains once per ability; no duplicate TARGET request or retarget |
| confirmation coexistence | B1/D1 ownership, provenance, and trace rules remain unchanged |
| Surveil | arrangeForSurveil remains native; zero L1 ORDER requests |
| combat | legacy combat order remains off; assignCombatDamage is not touched |

### 9.6 Canonical acceptance lock

Using the locked canonical workload (Izzet Guild Kit versus Dimir Guild Kit,
10 games, seed 20260810), the production audit must establish:

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
~~~

The milestone is `FRL_02L1_PASS` only if all 19 exact
`SIMULTANEOUS_TRIGGER_ORDER` sessions are admitted under the exact projection
and all request/trace/ordering invariants hold. The raw 20th multi-item
callback is the separately proven player-owned copied-spell seam and does not
count against L1 completeness. Native fallback for that outside-L1 callback is
not an L1-profile unsupported fallback.

The corrected acceptance gate was completed on the locked fresh-JVM workload:

```text
raw multi-item callbacks = 20
SIMULTANEOUS_TRIGGER_ORDER sessions = 19
admitted SIMULTANEOUS_TRIGGER_ORDER sessions = 19
ORDER requests = 26
candidate size 2/3/4 = 19/6/1
forced requests = 0
l1 unsupported fallback = 0
outside-L1 native fallback = 1
mapping failures = 0
trace incomplete = 0
```

### 9.7 Retained regression suite

Run the focused new tests and the existing decision/engine suites covering:

- DecisionTraceV2 and DecisionTraceTrainingValidator;
- PriorityActionProvider and priority action diagnostics;
- B1 Gelectrode confirmation;
- C2A triggered target;
- D1 Blood ETB confirmation;
- existing TriggeredTargetDecisionCoordinator and ConfirmationDecisionProvider
  tests;
- SimulateMatchDeterminismTest and the canonical audit harness;
- the full forge-gui-desktop Maven module with dependencies.

No implementation claim is complete until the fresh verification run reports its
actual result. Timeouts and unavailable external/runtime evidence remain
unverified, not passes.

## 10. Expected implementation map

The implementation plan should stay narrow and test-first. Expected changes are
limited to:

- typed ORDER DTOs/enums and exact DecisionRequest/LegalCandidate validation;
- explicit null-list, n=0, n=1, and n>=2 pre-admission routing;
- separate SESSION_INTEGRITY_FAILURE handling that never falls back to native;
- controller-local coordinator/provider seam and resolver capture;
- PlayerControllerAi thin routing integration;
- trace/validator support for the typed ORDER stage, external ORDER
  `CHOSEN false/false`, and terminal
  INVALID_EXTERNAL_CANDIDATE/NATIVE_CALLBACK_FAILURE result kinds;
- the centralized pure LIFO translation;
- focused unit, public-API, failure, trace, and real-engine integration tests;
- canonical audit assertions and documentation of the raw-20 versus exact-19
  profile lock.

MagicStack should not require a behavioral change. Any proposed production file
outside this map requires a new design checkpoint and explicit scope review.

## 11. Self-review and acceptance verdict

### P0/P1 review

~~~text
P0: none identified
P1: none; review gate closed
~~~

The revision preserves fail-closed ownership, public-contract safety, native
one-call capture, deterministic correlation, central LIFO semantics, and
canonical completeness while making cardinality, integrity, and terminal trace
ownership explicit.

### Deferred P2 items

- the discovered `COPY_SPELL_RESOLVE_FIRST_ORDER` profile, which requires its
  own design checkpoint;
- a future profile for Surveil partition-plus-order;
- any generic ORDER/permutation abstraction beyond this exact profile;
- cross-session/global correlation or ActionContinuation semantics;
- combat ordering or damage assignment.

The next design checkpoint after this exact L1 profile is:

```text
FRL-02L1C DESIGN_COPY_SPELL_RESOLVE_FIRST_ORDER
```

It must remain narrow to the proven Replicate/`CopySpellAbilityEffect`
semantic family and must not be implemented as part of FRL-02L1.

### Design verdict

~~~text
DESIGN_APPROVED
FRL_02L1_PASS
ORDER_V0_COMPLETE = false
~~~

The implementation plan follows this approved checkpoint and must preserve the
exact acceptance locks above while implementing with tests first.
