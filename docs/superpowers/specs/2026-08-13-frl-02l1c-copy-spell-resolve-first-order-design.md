# FRL-02L1C — Exact `COPY_SPELL_RESOLVE_FIRST_ORDER` Design Checkpoint

Status: design-only checkpoint

Date: 2026-08-13

Revision: 1 — review closure for BC persistence and router ownership

Base checkpoint: `93e89a98a4866bca7c3794576a6df60dbd6693ae`

Branch: `frl/02l1c-copy-spell-resolve-first-order`

Worktree: `C:\forgeAI-order-l1c`

This document defines the architecture boundary for FRL-02L1C. It does not
implement the profile, modify the existing L1 authority documents, or create
an implementation plan. The protected checkout at `C:\forgeAI` remains outside
the change scope.

## 1. Checkpoint and scope

FRL-02L1 is the already-accepted simultaneous-trigger ORDER profile. Its
semantic contract is player-owned ordering of an admitted trigger batch. It
remains PASS and is not generalized by this checkpoint.

FRL-02L1C is a second, disjoint profile:

`COPY_SPELL_RESOLVE_FIRST_ORDER`

It owns only the exact copied-spell batch produced by the current
`CopySpellAbilityEffect` path. The choice means “which copied spell resolves
first.” It does not mean damage assignment, target selection, spell-mode
selection, or arbitrary permutation of arbitrary `SpellAbility` objects.

FRL-02L2, `SURVEIL_PARTITION_PLUS_ORDER`, remains OPEN. `ORDER_V0_COMPLETE`
must remain false during this milestone.

The required focused baseline was run from the fresh worktree with the exact
command requested by the checkpoint:

`mvn -pl forge-gui-desktop -am '-Dtest=SimultaneousTriggerOrderPublicApiTest,SimultaneousTriggerOrderTraceTest,SimultaneousTriggerOrderCoordinatorTest,SimultaneousTriggerOrderEngineIntegrationTest,FRL02L1SimultaneousTriggerOrderAuditTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`

Result: `BUILD SUCCESS`; the TestNG suite reported 21 tests, 0 failures, 0
errors, and 0 skipped. The canonical L1 audit subprocess also exited 0.

The separately supplied `NoClassDefFoundError` for
`forge.game.decision.DiagnosticOutputPaths` was not reproduced by that exact
Maven run. The class is present in the worktree source and in
`forge-game\target\classes` after the Reactor compile. The evidence is
consistent with a direct `forge.view.Main` launch using a class path that does
not contain the `forge-game` output, not with a failing FRL-02L1 baseline. It
does not authorize a source change in this design-only milestone.

## 2. Source and caller inventory

The following inventory is the semantic boundary used by this design.

| Source or caller | Observed responsibility | FRL-02L1C classification |
| --- | --- | --- |
| `forge-game/src/main/java/forge/game/ability/effects/CopySpellAbilityEffect.java` | Creates a batch of copied abilities and calls `orderAndPlaySimultaneousSa(copies)` after the copy list is complete. | Exact L1C seam. |
| `forge-game/src/main/java/forge/game/card/CardFactory.java` | The production copy factory used by the effect. For a copied spell it creates a copied host, marks it as a copied spell, retains the original in `getCopiedPermanent()`, copies the ability, marks the ability copied, and sets `getCastSA()` to the copied spell. | Required provenance evidence for admission. |
| `forge-ai/src/main/java/forge/ai/PlayerControllerAi.java` | Current AI entry point for `orderSimultaneousSa`; it delegates to the existing L1 coordinator and then to the native AI ordering callback. | Add one thin profile dispatcher here; keep the existing L1 coordinator narrow. |
| `forge-game/src/main/java/forge/game/player/PlayerController.java` | Controller-owned registration surface for decision providers/resolvers. | Add a separate L1C resolver/provider slot; do not reuse resolver presence or counters across profiles. |
| `forge-game/src/main/java/forge/game/zone/MagicStack.java` | Inserts entries with add-first LIFO behavior and later resolves the top entry. | Existing downstream behavior; no change required. |
| `forge-ai/src/main/java/forge/ai/AiController.java` | Native AI ordering callback. The observed exact DealDamage pair returns its native identity/insertion ordering without using hidden opponent data or RNG on that path. | Native teacher for L1C, subject to public-symmetry BC filtering. |
| `forge-gui/src/main/java/forge/player/PlayerControllerHuman.java` | Native human ordering UI and existing reverse insertion handling. | Preserve the human/native path; do not introduce the external L1C resolver into the UI path in this milestone. |
| `forge-game/src/main/java/forge/game/ability/effects/RunChaosEffect.java` | Creates wrapped/trigger-style entries. | Not L1C; preserve the existing trigger or unsupported classification. |
| `forge-game/src/main/java/forge/game/decision/TriggeredTargetDecisionCoordinator.java` and `TargetDecisionProvider` | Existing narrow external TARGET boundary for a different wrapped-trigger profile. | Not the copied-spell ORDER boundary. Do not absorb copied-spell TARGET into L1C. |

The production source inventory contains one production caller of
`CardFactory.copySpellAbilityAndPossiblyHost`: the copied-spell effect path.
That is evidence for a narrow current seam, not permission to admit every
future copied ability. A future copy producer requires a fresh admission
audit.

The exact controlled canonical observation is two copied, non-trigger,
non-wrapper, `SpellApiBased` `DealDamage` spells from `Pyromatics Replicate`.
Both have `Pyromatics` as the visible source and initially carry the same
`Dimir Guildmage` target at the ORDER snapshot. They are duplicate-looking
public items but distinct native copies.

## 3. Exact semantic profile

The profile is named `COPY_SPELL_RESOLVE_FIRST_ORDER` and uses the shared
decision type `ORDER`, shared direction `RESOLVE_FIRST`, and shared candidate
kind `SELECT_RESOLVE_FIRST`. Those shared enum values are safe because they
describe the already existing decision meaning. The profile and context types
remain distinct from the L1 trigger types.

The profile owns this exact sequence:

`COPY_SPELL_RESOLVE_FIRST_ORDER`

`ORDER: choose copied spell that resolves first`

`per-copy TARGET setup`

`MagicStack insertion`

`resolution`

The callback-supplied list is the complete legal candidate domain. The policy
must choose within that list. It may not inspect the game to add candidates,
remove candidates, infer a target-based order, or reinterpret a copy as a
damage-assignment decision.

The canonical profile is player-owned only at the ordering boundary. Target
selection remains a later engine operation for each native copy. The profile
does not claim ownership of arbitrary `SpellAbility` ordering, trigger
ordering, copied activated abilities, stack target choices, or downstream
resolution effects.

## 4. Exact admission boundary

Admission is fail-closed and all-or-nothing. A batch is admitted to L1C only
when every condition below holds:

1. The list is non-null and contains at least two entries.
2. Every entry is non-null and the list contains no repeated native identity.
   Identity, not `equals`, defines this check.
3. Every entry is a `SpellApiBased` runtime shape, is a spell, is copied, is
   not a trigger, and is not a `WrappedAbility`.
4. Every entry has a non-null host; the host is marked as a copied spell; the
   host has a non-null `getCopiedPermanent()` source; and the copied host's
   `getCastSA()` is the same native entry. This is the typed copy-factory
   lineage check, not a class-name-only heuristic.
5. Every entry has a non-null effect API. The current factory path produces a
   common API for one copy batch; mixed APIs are rejected as a future shape,
   rather than silently generalized here.
6. Every entry has a non-null activating player, every copied host has a
   non-null controller and owner, and every effective choosing player is the
   exact callback receiver. Mixed or missing ownership fails closed.
7. All entries point to the same original source by native source identity and
   therefore to one copy batch. The original source is face-up and publicly
   projectable to the chooser.
8. No active `ActionContinuation` is present. L1C does not fabricate a
   continuation or make a continuation-bearing session public.
9. The source projection and API value can be captured without an exception,
   hidden information, a native object reference, or an unstable textual
   representation.

The current `CopySpellAbilityEffect`/`CardFactory` path is the only evidence
for item 4 and item 7. The implementation must not replace those checks with
“is copied” alone. A copied non-spell, a copied ability, a copied wrapper, a
non-copied `SpellApiBased` entry, a trigger batch, a mixed trigger/copy batch,
a mixed-player batch, a hidden source, a null API, or a missing provenance
marker is not an admitted L1C session.

### Pure pre-classification and closed ownership predicate

The router performs a pure, resolver-independent pre-classification before
running the strict L1C admission. It has exactly these classes:

* `L1_EXACT`: the existing exact L1 trigger predicate succeeds. L1 owns this
  batch and the L1C resolver is irrelevant.
* `COPY_SPELL_FAMILY_INTENT`: `active != null`, `active.size() >= 2`, and
  every entry is non-null, `isSpell()`, `isCopied()`, has a non-null host, and
  that host is marked as a copied-spell game piece. This predicate does not
  inspect `SpellApiBased`, source lineage, API, players, visibility,
  `getCastSA()`, targets, or resolver presence.
* `UNOWNED_OTHER`: every other shape, including null input, n=0/n=1, a null
  entry, a copied non-spell, a mixed trigger/copy batch that does not satisfy
  the family predicate, and unrelated `SpellApiBased` input. If inspection
  throws, classification is `UNOWNED_OTHER`; the router never lets an
  exception or an unknown shape claim L1C ownership.

The pre-classifier is the complete ownership boundary. It is pure, does not
mutate the list, and has no resolver-dependent branch.

Only a batch classified as `COPY_SPELL_FAMILY_INTENT` proceeds to the strict
admission above:

* family intent plus strict admission success -> exact L1C;
* family intent plus strict admission failure -> `MALFORMED_L1C_INTENT`; and
* no family intent -> `UNOWNED_OTHER`.

`MALFORMED_L1C_INTENT` with an active L1C resolver is a hard fail-closed
admission failure with no native fallback. Without an L1C resolver it follows
the existing native compatibility path. `UNOWNED_OTHER` never consults the
L1C resolver and therefore cannot be changed by its presence. This makes the
decision `MALFORMED_L1C_INTENT` versus `UNOWNED_OTHER` a closed predicate,
not an implementation-time judgment.

## 5. Choosing-player authority

The choosing player is the player whose controller receives the
`orderSimultaneousSa` callback. For the AI route, this is the `Player` already
owned by `PlayerControllerAi`; it is not inferred from the first item, source
owner, target controller, or AI heuristic.

Admission requires, for every entry:

* `entry.getActivatingPlayer()` is non-null and is the callback receiver;
* `entry.getHost()` is non-null and `entry.getHost().getController()` is the
  same callback receiver; and
* the host owner/controller values required by the copied-spell factory are
  non-null.

The context records the callback receiver's stable player ID. A missing or
mixed player is a hard admission failure. There is no L1-style fallback to a
player selected from an entry because doing so would let a malformed copy
batch cross an ownership boundary.

## 6. Minimum public item projection

The public item is a new immutable value-only DTO. It is not
`CardSelectionCard` constructed from the copied host.

The proposed exact fields are:

| Field | Meaning | Why it is admitted |
| --- | --- | --- |
| `itemId: long` | Session-local initial-snapshot ordinal `1, 2, ..., n`. | The resolver needs distinct request-local identities even when copies look identical. It is deterministic and carries no native identity. |
| `visibleSourceName: String` | Public name of `host.getCopiedPermanent()` at the ORDER snapshot. | It is useful to a player-facing policy and is only captured after visibility admission. It is not the copied host's hidden/temporary identity. |
| `effectApi: ApiType` | Public effect category, for example `DealDamage`. | It is a stable value enum already used by the existing decision API and is useful for effect-aware policy/teacher analysis. |
| `kind: CopySpellResolveFirstOrderItemKind` | Typed value `COPIED_SPELL`. | Keeps the profile discriminated without exposing `SpellApiBased` or a native ability object. |

The projection deliberately omits target state. The same initial target does
not make the copies the same item, and later TARGET setup is a separate
engine-owned phase.

The projection must not expose `SpellAbility`, `SpellApiBased`, a native
`Card`, `CardLKI`, `GameObject`, Java identity, native spell-ability ID,
copied-host ID, raw copy provenance, ability description, stack text, hidden
information, or arbitrary `toString()` output.

`CardSelectionCard` is unsafe unchanged for this seam. Constructing it from a
copied host before stack insertion can expose the copied host's Forge card ID
or game timestamp, which is not a player-known identity at the ORDER snapshot.
Using the original source's `CardSelectionCard` would also add native identity
that is unnecessary for this decision. The new projection therefore keeps the
visible name only and uses `itemId` for request-local identity.

The projection is immutable, has deterministic value equality for public
comparison, and never owns the native object. The coordinator retains the
private native mapping separately.

## 7. Duplicate-looking items and native identity

The canonical pair is:

* the same visible source;
* the same `ApiType`;
* the same initial target; and
* two distinct copied native spell identities.

The two items remain two legal candidates. Public text is not an identity
mechanism.

At session creation the coordinator captures:

* the immutable initial list;
* the public item for each initial ordinal; and
* a private `IdentityHashMap` from each native copied spell to its item.

The same native spell object appearing twice is an immediate
`SESSION_INTEGRITY_FAILURE`, even if all public fields are equal. A returned
candidate is resolved through its `itemId` and the current remaining set; it is
never matched by `equals`, source text, API, target, UUID, wall clock,
`nanoTime`, `identityHashCode`, or RNG.

The item IDs remain stable for the session while the request's candidate list
shrinks. The current remaining list is always emitted in initial-ordinal order
so that request hashes and trace joins are deterministic.

## 8. Routing architecture with the existing L1 PASS profile

### Recommended shape: one thin top-level dispatcher

`PlayerControllerAi.orderSimultaneousSa` should enter one narrow
`OrderProfileRouter`. The router records the raw invocation exactly once,
classifies the immutable input shape once, and delegates to exactly one
profile-specific coordinator:

* exact `SIMULTANEOUS_TRIGGER_ORDER` -> existing L1 coordinator;
* exact `COPY_SPELL_RESOLVE_FIRST_ORDER` -> new L1C coordinator;
* malformed copy-intended -> L1C admission/failure policy;
* otherwise -> existing unowned/native route.

The existing L1 coordinator remains responsible only for its wrapped,
non-static, visible trigger profile. Its admission rules, request semantics,
resolver behavior, and native translation are not broadened to understand
copied spells. The L1C coordinator remains responsible only for its exact
copied-spell profile.

Raw callback instrumentation moves to the common router entry (or to a
shared common entry used by all AI callers) so the same callback cannot be
counted by both coordinators. No coordinator may invoke the other, call the
native callback twice, insert a partial list, or emit duplicate trace/request
records.

The router is a dispatcher, not a generic permutation framework. It has a
closed set of profile classifications and a fail-closed default.

### Resolver isolation

The L1 provider/resolver and L1C provider/resolver are separate. A resolver
registered for L1 cannot claim a copied-spell session, and an L1C resolver
cannot make a trigger or unrelated batch hard-fail. Resolver presence is
checked only after the corresponding exact profile has been selected.

The human path continues to use its existing native UI ordering. This
checkpoint does not make the human controller a second external-policy
surface. The AI path remains the route used for ForgeRL external decisions and
the native AI teacher.

## 9. Provider, resolver, and deterministic ID ownership

Add a profile-specific
`CopySpellResolveFirstOrderDecisionProvider`. It owns the L1C resolver and
captures that resolver once when a session begins. A resolver setter change
after session start does not change the active session's callback source.

Do not reuse the L1 trigger provider object or a global resolver. To preserve
controller-local uniqueness without coupling the profiles. The existing L1
provider keeps its current `nextRequestId` and `nextOrderSessionId` counters
unchanged. The new L1C provider owns separate counters with the same
controller-local, monotonic semantics. There is deliberately no shared
`OrderDecisionIdAuthority`: no current consumer requires global
`DecisionRequest`-ID uniqueness, and introducing one would add L1 churn
without improving trace correctness.

IDs must not use time, random state, UUIDs, static global counters, or native
object identity. Public/session correlation is `(profile, sessionId,
stepIndex)`. `DeterminismTrace` keeps its existing global per-game trace
request index, so persisted joins use `traceRequestIndex` plus profile/stage;
they do not assume that a provider-local `DecisionRequest` ID is globally
unique.

## 10. Request and context contract

The implementation adds typed siblings rather than replacing the existing L1
types:

* `CopySpellResolveFirstOrderProfile` with the one value
  `COPY_SPELL_RESOLVE_FIRST_ORDER`;
* `CopySpellResolveFirstOrderContext`;
* `CopySpellResolveFirstOrderItem`; and
* `CopySpellResolveFirstOrderSourceProjection`.

`DecisionRequest` and `LegalCandidate` may be extended with a second typed
ORDER payload. Their validation must be profile-discriminated:

* L1 requests contain only `SimultaneousTriggerOrderContext` and
  `SimultaneousTriggerOrderItem`;
* L1C requests contain only the L1C context and item; and
* neither path accepts a generic arbitrary permutation item.

The L1C request fields are:

* `DecisionType.ORDER`;
* profile `COPY_SPELL_RESOLVE_FIRST_ORDER`;
* direction `RESOLVE_FIRST`;
* `sessionId` from the shared controller-local authority;
* zero-based `stepIndex`;
* `originalItemCount`;
* stable `choosingPlayerId`; and
* no continuation ID or hidden/native state.

Each emitted request has `SELECT_RESOLVE_FIRST` candidates for the exact
remaining set, in deterministic initial-ordinal order. A request is emitted
only while at least two candidates remain. Therefore there are exactly `n - 1`
requests for a successful external or native session, no one-candidate
request, and no synthetic request for the final item. `forced` is false for
every emitted ORDER request; the last item is forced internally and is not a
public policy choice.

The candidate semantic key remains request-local and typed, for example
`RESOLVE_FIRST|<itemId>`. The profile validation must reject a candidate from a
different profile, session, request, or stale remaining set.

## 11. Native lifecycle

The native path is used when the exact L1C batch is admitted and no L1C
resolver is installed. It is the existing AI teacher callback path, with the
following ordering guarantees:

1. Capture the immutable native input snapshot, public projection, and private
   identity map before native code can mutate or return a list.
2. Create and record step-zero's ORDER request. It describes the full set and
   is not a policy label yet.
3. Call the native ordering callback exactly once.
4. Validate a non-null, full permutation of the original native identities:
   exact size, no null, no foreign object, no omission, and no duplicate.
5. Only after complete validation, translate the native insertion list to
   semantic resolve-first order by one pure reversal and complete the trace
   requests for the selected sequence.
6. Return the original valid native insertion list unchanged. The downstream
   `orderAndPlaySimultaneousSa` loop remains responsible for target setup and
   stack insertion.

The native callback's returned list is the insertion order. Because
`MagicStack.add()` is add-first LIFO, the semantic resolve-first sequence is
the reverse of that returned list. No second native callback, heuristic
fallback, partial insertion, or target setup is allowed inside the
coordinator.

If the native callback throws, step zero is terminalized as
`NATIVE_CALLBACK_FAILURE`; the coordinator does not retry or fall back. If
the callback returns an invalid permutation, step zero is terminalized as
`MAPPING_FAILED` with native completion and mapping-attempt flags retained in
the trace, but no candidate label and no stack mutation. Both are hard
failures.

## 12. External lifecycle

The external path is used when the exact L1C batch is admitted and the L1C
resolver is present. The resolver is captured once before the first request.

For each step while at least two items remain:

1. Build an immutable request from the exact current remaining set.
2. Record one trace request with `forced=false`.
3. Ask the captured resolver for one candidate.
4. Validate that the returned candidate is non-null, has the L1C profile,
   `SELECT_RESOLVE_FIRST` kind, matching semantic key, current `itemId`, and
   membership in this request's remaining set.
5. Record `CHOSEN` with external flags (`nativeCompleted=false`,
   `mappingAttempted=false`), remove that item, and continue.

The final remaining item is appended to the semantic sequence internally. The
sequence is reversed once into native insertion order and returned. Native
ordering is not called at all on a successful external session.

A null, throwing, stale, foreign, duplicate, wrong-kind, wrong-profile, or
cross-session resolver result terminalizes the active request as
`INVALID_EXTERNAL_CANDIDATE`. There is no native fallback after an external
failure and no partial stack insertion. A resolver setter change during the
session is ignored because the session captured its resolver at start.

Every emitted request receives exactly one terminal result. The successful
path and all intentional failure paths must produce zero
`TRACE_INCOMPLETE` records.

## 13. Failure semantics

The profile reuses the existing failure vocabulary where the meaning is
already exact:

| Condition | Result | Native fallback? | Stack mutation? |
| --- | --- | --- | --- |
| Same native object twice or malformed initial identity snapshot | `SESSION_INTEGRITY_FAILURE` | No | No |
| `MALFORMED_L1C_INTENT` with active L1C resolver | Typed fail-closed admission failure | No | No |
| Native callback throws | `NATIVE_CALLBACK_FAILURE` | No | No |
| Native result is not an identity permutation | `MAPPING_FAILED` | No | No |
| External result invalid/stale/foreign/wrong profile | `INVALID_EXTERNAL_CANDIDATE` | No | No |
| Exact L1C batch without resolver | Native teacher path | N/A | Only downstream after valid return |
| Unowned batch with no profile owner | Existing native compatibility path | Yes, by existing ownership | Existing downstream behavior |

The pre-classifier maps null lists, empty/singleton lists, null entries, and
other shapes outside the family predicate to `UNOWNED_OTHER`. They do not
create an L1C ORDER request and must not call an L1C resolver; the existing
controller boundary handles their native compatibility behavior. The router
never turns a null or unknown input into an invented candidate domain.

The engine-boundary exception is fail-closed. The coordinator never converts a
failure into a fabricated `ActionContinuation`, synthetic candidate, or
heuristic order.

## 14. `RESOLVE_FIRST` translation

The reversal is a pure, central semantic utility shared by L1 and L1C where
needed, without making either coordinator depend on the other's profile class.

For a semantic sequence `[A, B]` where A resolves first, native insertion is
`[B, A]`. For `[A, B, C]`, insertion is `[C, B, A]`. For `[A, B, C, D]`, it is
`[D, C, B, A]`. The reverse operation is its own inverse.

The implementation must have one tested helper for both directions. It must
copy the input, reject null inputs, preserve object references, and not sort,
deduplicate, consult RNG, or use public projection equality. Tests must cover
n=2, n=3, n=4, and duplicate-looking public items with distinct native
identities.

## 15. ORDER/TARGET coexistence and downstream identity

The exact lifecycle is:

`ORDER` -> `per-copy TARGET setup` -> `MagicStack` insertion -> `resolution`

L1C ends when it returns the native insertion list. It does not call
`chooseTargetsFor`, `setupTargets`, `setupNewTargets`, or any target provider.
The existing `PlayerControllerAi.orderAndPlaySimultaneousSa` loop continues to
set up each copy's targets against that copy's native `SpellAbility` before
adding it to the stack. The human path remains unchanged as well.

At the current boundary, the public target context identifies target entities,
target count/group, and choosing player, but it does not carry the L1C
session-local copy item or copied-spell source projection. The copied nontrigger
TARGET path is currently engine-owned and bypasses the external wrapped-trigger
target coordinator. Internally, the native `SpellAbility`/host association is
enough for Forge to target the correct copy because target setup is performed
sequentially on the returned native objects.

The explicit design verdict is:

`TARGET_NOT_YET_OWNED_BUT_FUTURE_CORRELATION_REQUIRED`

This is not a blocker for L1C because L1C does not claim copied-spell TARGET.
If a later milestone externalizes that TARGET decision, it must introduce a
typed, player-safe correlation from the current copy item to the target
request before claiming that boundary. It must not reuse copied-host IDs or
infer correlation from source text. No TARGET change is part of this
checkpoint.

## 16. Native teacher and BC policy

The native AI path is history-valid when it returns a valid native permutation
and its downstream behavior is deterministic. For the canonical Pyromatics
pair, however, the public ORDER request is symmetric: removing `itemId`, both
items have the same visible source, API, and typed copied-spell kind. The
native callback's identity/insertion ordering is therefore not a uniquely
justified public policy label at that request.

The teacher classification is:

`SAFE_WITH_REQUEST_LOCAL_IDENTITY`

The policy is option B:

* retain native L1C `CHOSEN` records as normal history with
  `nativeCompleted=true` and `mappingAttempted=true`;
* exclude a request from offline BC admission when two or more remaining
  candidates have identical public projections after removing `itemId`;
* do not collapse permutations, rewrite the history, or mark the request
  forced; and
* allow a successful native L1C request with a unique public projection to be
  a BC sample.

The symmetry key is exactly the tuple
`(visibleSourceName, effectApi, kind)`. Initial target, native host ID,
native ability identity, and raw provenance are not part of that key. The
canonical Pyromatics/DealDamage pair is therefore history-valid but has
`BC sample = false`. A request is forced only when exactly one legal candidate
remains; because one-candidate ORDER requests are not emitted, all recorded
L1C ORDER requests have `forced=false`.

This policy changes only the offline BC sample filter for the C profile. It
does not alter online resolver behavior, native list order, game state, RNG,
trace history, or target setup.

### Persisted BC-eligibility contract

The public-symmetry decision is a request property and must be persisted; it
must not be reconstructed later from `RESOLVE_FIRST|<itemId>` semantic keys or
from the candidate-set hash. The current `DecisionTraceRequestRecord` stores
only semantic candidate strings, so the existing `DECISION_TRACE_V2` shape
cannot represent the L1C decision reproducibly.

Add the typed value enum
`DecisionTraceTeacherLabelEligibility` with at least:

* `NOT_APPLICABLE` for non-ORDER requests;
* `BC_ELIGIBLE` when a successful native CHOSEN result may be admitted to BC;
  and
* `BC_EXCLUDED_PUBLIC_SYMMETRY` when the request-local public projections are
  symmetric after removing `itemId`.

`DecisionTraceRequestRecord` carries this field. The L1C coordinator computes
it once from the captured public item projections at request creation. The
existing L1 coordinator supplies `BC_ELIGIBLE` for its exact ORDER requests,
preserving the current native-teacher behavior and V2 output. External
results still remain non-BC through their existing lifecycle flags. The
training validator's BC predicate requires `BC_ELIGIBLE` in addition to the
existing native-completed, mapping-attempted, CHOSEN, non-forced checks. A
missing or malformed L1C eligibility value is never treated as eligible.

This is a new persisted trace version, not an extension with ambiguous
optional V2 columns:

* L1-only traces with no L1C request remain byte-compatible
  `DECISION_TRACE_V2` traces.
* Any trace containing an L1C request is written wholly as
  `DECISION_TRACE_V3`; V2 and V3 records are never mixed in one file.
  `DeterminismTrace` therefore decides the per-trace version before writing
  the file (or buffers structured records until `finish()`); it must not emit
  an early V2 prefix and later append a V3 request.
* The V3 REQUEST record retains the V2 fields and appends the typed profile
  and `DecisionTraceTeacherLabelEligibility` value. The paired RESULT record
  retains the current selected-key and lifecycle flags; the eligibility is
  request-scoped and is joined by `traceRequestIndex`.
* The summary records `decisionTraceVersion=DECISION_TRACE_V3` for a C-bearing
  trace. Offline consumers must require the V3 request metadata for L1C BC
  admission and must fail closed to BC=false if it is absent.

Thus a stored canonical pair contains both the normal REQUEST/RESULT history
and an explicit `BC_EXCLUDED_PUBLIC_SYMMETRY` label. The offline validator can
reproduce the Pyromatics decision without access to Forge objects or the
original public projections.

## 17. Hidden-information and determinism analysis

Admission reads the original copied source only after checking that it is
face-up and visible to the chooser. A hidden or unprojectable source fails
admission. The copied host is not used as a public source because its
temporary/native identity is not player-known at this point.

The public request contains no current target, target object, stack identity,
native ID, copied provenance, hidden card state, continuation, or arbitrary
engine description. The only identity available to the policy is the
session-local ordinal, which is assigned from the callback list and therefore
does not expose a Forge identity.

The private identity map is engine-only and never serialized into a request,
trace candidate, BC feature, or public diagnostic. Request order, session IDs,
item IDs, and trace stage are deterministic and independent of RNG and wall
clock. Instrumentation must not affect the hash of an audit-on versus audit-off
run.

## 18. Diagnostics design

The current L1 diagnostics contract contains both raw callback counts and L1
profile counters, plus an old “non-L1” bucket that currently counts the
canonical copied-spell callback. Once L1C owns that callback, silently
repurposing those old fields would make the historical V2 contract ambiguous.

The implementation milestone must version the diagnostics contract and split
the namespaces, either in one explicitly versioned file or in separate raw,
L1, and L1C files. The recommended shape is one versioned file with explicit
`raw.*`, `l1.*`, and `l1c.*` namespaces. The old V2 key names remain a frozen
historical reference; they are not silently redefined.

The raw namespace retains:

* `orderSimultaneousSa.total = 116`;
* `n0 = 0`, `n1 = 96`, `n2 = 14`, `n3 = 5`, `n4 = 1`, `nOther = 0`;
* `rawMultiItemCallbacks = 20`.

The L1 namespace retains:

* `triggerSessions = 19`;
* `admittedTriggerSessions = 19`;
* `orderRequests = 26`;
* candidate sizes 2/3/4 = 19/6/1;
* forced requests = 0;
* L1 unsupported fallback = 0;
* mapping failures = 0; and
* trace incomplete = 0.

The L1C namespace adds:

* `copyProfileSessions = 1`;
* `admittedCopyProfileSessions = 1`;
* `inputSize2 = 1`;
* `orderRequests = 1`;
* `candidateSize2 = 1`;
* `forcedRequests = 0`;
* `nativeTeacherCallbacks = 1` when the canonical run has no external C
  resolver;
* `mappingFailures = 0`;
* `nativeCallbackFailures = 0`;
* `invalidExternalCandidates = 0`; and
* `traceIncomplete = 0`.

The canonical raw multi-item attribution is 19 L1 trigger sessions plus one
L1C copied-spell session. There is no canonical unowned multi-item callback.
The old `nonL1MultiItemCallbacks` and `outsideL1NativeFallbacks` keys must not
be reused as if they meant L1C. The exact version and key names are an
implementation acceptance contract, not a reason to change decision semantics.

Raw callback accounting happens once at the dispatcher boundary. Profile
counters happen only after classification/admission. Diagnostics remain
disabled by default, value-only, and unable to change engine control flow.

Decision-trace versioning is separate from the raw/profile audit counter
version: the canonical run containing the L1C request must produce
`DECISION_TRACE_V3`, while an L1-only control run may remain
`DECISION_TRACE_V2`. Audit instrumentation must not change either version or
the resulting trace hash.

## 19. Canonical acceptance lock

The implementation milestone must run the existing controlled workload in a
fresh child JVM with:

* decks `Izzet Guild Kit` and `Dimir Guild Kit`;
* 10 games;
* seed `20260810`; and
* audit enabled and disabled in separate runs.

The acceptance lock is:

| Metric | Required value |
| --- | ---: |
| Raw `orderSimultaneousSa.total` | 116 |
| Raw `n1` | 96 |
| Raw `n2` / `n3` / `n4` | 14 / 5 / 1 |
| Raw multi-item callbacks | 20 |
| L1 trigger sessions / admitted | 19 / 19 |
| L1 ORDER requests | 26 |
| L1 candidate sizes 2 / 3 / 4 | 19 / 6 / 1 |
| L1 fallback | 0 |
| L1C copy sessions / admitted | 1 / 1 |
| L1C input size 2 | 1 |
| L1C ORDER requests | 1 |
| L1C candidate size 2 | 1 |
| L1C forced requests | 0 |
| L1C native teacher callbacks | 1 in the native canonical run |
| Decision trace version | `DECISION_TRACE_V3` for the C-bearing run |
| Mapping failures | 0 |
| Trace incomplete | 0 |

The audit-on and audit-off deterministic trace tree hashes must be identical.
The raw semantic attribution is exactly 19 trigger ORDER sessions plus one
copied-spell ORDER session. The existing L1 acceptance is updated only as an
explicit versioned diagnostics-contract change during implementation; it is
not weakened or made to absorb L1C counters.

## 20. Implementation test matrix

No tests are added in this design milestone. The following tests are required
before L1C implementation can be accepted.

### Public API and projection

* Reflection/API test proves the new profile, context, item, source projection,
  candidate kind, and resolver expose only the approved value fields.
* The projection test rejects `Card`, `CardLKI`, `GameObject`,
  `SpellAbility`, `SpellApiBased`, Java identity, native spell-ability ID,
  copied-host ID, target object, description, and arbitrary string fields.
* A duplicate-looking canonical pair has distinct `itemId` values while all
  other public values are equal.
* `CardSelectionCard` is not used unchanged for the copied host.

### Coordinator, trace, and translation

* n=2, n=3, and n=4 external sessions emit exactly n-1 requests with exact
  remaining sets and one internal final item.
* Native n=2/n=3/n=4 sessions call the teacher once and validate the complete
  identity permutation.
* Reverse translation round-trips n=2/n=3/n=4 and duplicate-looking items.
* Same native identity twice is `SESSION_INTEGRITY_FAILURE`.
* Invalid native permutation, foreign object, omission, duplicate, null
  result, and native callback throw produce the correct terminal result and no
  fallback/partial insertion.
* Invalid, stale, foreign, duplicate, wrong-profile, and wrong-kind external
  candidates produce `INVALID_EXTERNAL_CANDIDATE` with no native callback.
* Resolver replacement during an active session does not change the captured
  resolver.
* Native records remain history-valid; symmetric native records are excluded
  from BC, while non-symmetric native records remain eligible.
* The canonical duplicate-looking request persists
  `BC_EXCLUDED_PUBLIC_SYMMETRY` in `DECISION_TRACE_V3`; the validator returns
  BC=false even when the result has native-completed and mapping-attempted
  flags.
* A C-bearing trace missing the typed eligibility metadata fails closed to
  BC=false, and an L1-only trace remains valid under `DECISION_TRACE_V2`.
* All intentional paths have exactly one terminal result and zero intentional
  `TRACE_INCOMPLETE`.

### Admission and routing

The failure matrix must explicitly cover:

* null list;
* n=0;
* n=1;
* exact canonical admitted n=2 copy batch;
* distinct copied items with identical public projection;
* same native identity twice;
* non-copied `SpellApiBased` list;
* copied but non-spell shape;
* trigger `WrappedAbility` list;
* mixed trigger/copy list;
* mixed choosing players;
* null activating/controller ownership;
* hidden or unprojectable source;
* null `ApiType`;
* invalid native permutation;
* native callback throw;
* invalid external candidate;
* stale external candidate;
* resolver change mid-session; and
* duplicate-looking copy identity mapping.

Routing tests must prove all of the following in one process:

* L1-only input uses only the L1 provider/resolver;
* L1C-only input uses only the L1C provider/resolver;
* a mixed batch is not admitted to either profile;
* the pure family-intent predicate deterministically separates
  `MALFORMED_L1C_INTENT` from `UNOWNED_OTHER` before resolver lookup;
* resolver presence in one profile does not hard-fail another profile;
* raw callback accounting occurs once;
* no coordinator calls the native callback twice;
* no duplicate requests/traces are emitted; and
* no list is inserted or mutated before successful completion.

### Real engine integration

Synthetic coordinator tests are not sufficient. The integration suite must
exercise:

`PlayerControllerAi -> CopySpellAbilityEffect -> orderAndPlaySimultaneousSa`
`-> per-copy target setup -> MagicStack -> resolution`.

It must cover external n=2 and n=3, native n=2, external/native equivalence,
target setup exactly once per copy, no duplicate TARGET request, correct
native copy association, and the target-correlation guard that prevents a
future external TARGET implementation from guessing between duplicate-looking
copies.

The existing L1 public API, trace, coordinator, engine integration, canonical
audit, current L1 regression, and determinism audit remain mandatory. The
canonical audit must validate audit-on/off trace equality as well as the
versioned split diagnostics.

## 21. Alternatives considered

### Alternative A — top-level profile dispatcher (recommended)

**Files/types affected:** `PlayerControllerAi`, `PlayerController`, a narrow
`OrderProfileRouter`, the new L1C profile/context/item/source projection,
provider, coordinator, pure translation helper, typed
`DecisionRequest`/`LegalCandidate` extensions,
`DecisionTraceTeacherLabelEligibility`, trace validator, and versioned
diagnostics.

**Semantic clarity:** Highest. One entry point assigns each callback to one
exact semantic owner. L1 and L1C coordinators stay monomorphic.

**L1 churn:** Low. The existing L1 admission logic remains unchanged; only the
raw counter location, shared translation/ID extraction, and one AI entry point
are touched.

**Failure isolation:** Highest. A resolver is consulted only for its exact
profile. Malformed C-shaped batches cannot fall through to L1, while unrelated
batches do not fail because an L1C resolver is present.

**Diagnostics:** Clean split between raw callbacks, L1, L1C, and unowned
fallbacks. Raw accounting cannot double-count, and the V3 trace stores the
typed BC eligibility instead of requiring projection reconstruction.

**Future L2:** A future profile can add one explicit classifier branch and its
own coordinator without making L1 or L1C understand it. This is a routing
boundary, not a generic order abstraction.

**Maintenance cost:** One extra dispatcher and profile-specific types, offset
by local invariants and straightforward tests.

### Alternative B — delegate from the existing L1 coordinator

**Files/types affected:** Mostly the current
`SimultaneousTriggerOrderDecisionCoordinator`, plus the new L1C coordinator,
provider, types, diagnostics, and `PlayerController` resolver surface.

**Semantic clarity:** Moderate. The class currently named and tested as the
L1 trigger coordinator would become a heterogeneous dispatcher for triggers,
copied spells, and future unsupported shapes.

**L1 churn:** Initially lower in `PlayerControllerAi`, but higher inside the
L1 coordinator and its admission/failure tests.

**Failure isolation:** Lower. L1 resolver presence, L1 diagnostics, and
`UNSUPPORTED_ADMISSION` behavior could accidentally leak into L1C. The current
“unsupported with resolver” behavior would need profile-aware exceptions in a
class whose public meaning is L1.

**Diagnostics:** More coupling. Raw counters and profile counters would be
split inside a class that also owns L1 semantics, increasing the chance that
old `nonL1` fields are silently reused.

**Future L2:** Poor trajectory. Each new profile would add another branch to a
class whose tests assume simultaneous trigger semantics, encouraging a generic
permutation framework by accretion.

**Maintenance cost:** Lower file count but higher semantic coupling and
regression risk. It is viable for a one-off patch, but not the exact ownership
boundary required here.

### Alternative C — shared generic ORDER strategy framework

This would introduce a generic permutation engine, generic item interface,
profile registry, and generalized resolver lifecycle. It could reduce some
boilerplate but would broaden the public API before a second independent
profile has proven the common semantics. It would make callback domains,
target ownership, failure routing, and BC labels easier to accidentally
generalize. It is rejected as speculative abstraction for this milestone.

## 22. Recommended architecture

Choose Alternative A:

1. Keep L1's exact coordinator and public types semantically unchanged.
2. Add one thin AI entry router that records raw invocation once and selects
   exactly one profile owner using the closed family-intent predicate.
3. Add a separate L1C provider/resolver with its own deterministic counters;
   leave the existing L1 provider counters unchanged.
4. Add typed L1C value DTOs and profile-discriminated request validation.
5. Capture private native identity with `IdentityHashMap`; use only
   session-local ordinals publicly.
6. Use one pure reverse translation for semantic resolve-first versus native
   insertion order.
7. Persist request-scoped BC eligibility in `DECISION_TRACE_V3` for any
   C-bearing trace; keep L1-only traces on V2.
8. Keep `CopySpellAbilityEffect`, `MagicStack`, target setup, and the human
   controller behavior unchanged.
9. Keep native C records as history, exclude symmetric requests from BC, and
   retain non-symmetric native labels.
10. Version/split diagnostics explicitly and lock the canonical values above.

This architecture is the smallest one that closes the three required risks:
router separation, ORDER/TARGET identity, and the symmetric Pyromatics teacher
label.

## 23. Expected implementation files (not created)

The later implementation milestone is expected to touch only the following
semantic areas. This is an expected-file inventory, not an implementation plan;
none of these files is changed by this checkpoint.

* `forge-ai/src/main/java/forge/ai/PlayerControllerAi.java`
* `forge-game/src/main/java/forge/game/player/PlayerController.java`
* `forge-game/src/main/java/forge/game/decision/OrderResolutionTranslation.java`
* `forge-game/src/main/java/forge/game/decision/OrderProfileRouter.java`
* `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderProfile.java`
* `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderSourceProjection.java`
* `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderItem.java`
* `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderContext.java`
* `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderDecisionProvider.java`
* `forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderDecisionCoordinator.java`
* `forge-game/src/main/java/forge/game/decision/DecisionRequest.java`
* `forge-game/src/main/java/forge/game/decision/LegalCandidate.java`
* `forge-game/src/main/java/forge/game/decision/DecisionTraceRequestRecord.java`
* `forge-game/src/main/java/forge/game/decision/DecisionTraceTeacherLabelEligibility.java`
* `forge-game/src/main/java/forge/game/decision/DecisionTraceTrainingValidator.java`
* `forge-game/src/main/java/forge/game/decision/DeterminismTrace.java`
* `forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderAuditDiagnostics.java`
* `forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderDecisionCoordinator.java`
* `forge-gui-desktop/src/test/java/forge/game/decision/CopySpellResolveFirstOrderPublicApiTest.java`
* `forge-gui-desktop/src/test/java/forge/game/decision/CopySpellResolveFirstOrderTraceTest.java`
* `forge-gui-desktop/src/test/java/forge/game/decision/DecisionTraceV3Test.java`
* `forge-gui-desktop/src/test/java/forge/game/decision/CopySpellResolveFirstOrderCoordinatorTest.java`
* `forge-gui-desktop/src/test/java/forge/game/decision/CopySpellResolveFirstOrderEngineIntegrationTest.java`
* `forge-gui-desktop/src/test/java/forge/view/FRL02L1CCopySpellResolveFirstOrderAuditTest.java`

`CopySpellAbilityEffect.java`, `MagicStack.java`,
`PlayerControllerHuman.java`, `AiController.java`, and the current TARGET
engine path are not expected to change for this profile boundary.

## 24. Non-goals

This checkpoint does not:

* implement L1C production code or tests;
* create an implementation plan;
* change `CopySpellAbilityEffect` behavior;
* change `MagicStack` insertion/resolution;
* externalize copied-spell TARGET;
* introduce cross-decision target correlation now;
* change `PlayerControllerHuman` UI behavior;
* generalize to arbitrary copied abilities, triggers, or permutations;
* assign damage or choose targets during ORDER;
* enable legacy rules/decks to manufacture observations;
* alter RNG, game state, continuation, or stack timing;
* collapse duplicate-looking candidates;
* make `ORDER_V0_COMPLETE` true; or
* push, open a PR, or modify the protected checkout.

## 25. P0/P1 review

P0 findings: 0.

P1 findings: 0.

P2 findings: 0.

The previously risky points are resolved at design level:

* copied-host `CardSelectionCard` leakage is avoided with a minimal source
  projection;
* duplicate-looking copies use ordinal plus private native identity;
* L1 and L1C resolver/diagnostic ownership is separated at one router;
* native reverse translation is explicit and centrally tested;
* ORDER remains separate from TARGET;
* downstream TARGET is explicitly marked as not yet externally owned but
  requiring typed correlation before any future externalization; and
* the canonical symmetric Pyromatics native choice is history-valid but not a
  BC label for that request;
* `BC_EXCLUDED_PUBLIC_SYMMETRY` is persisted in `DECISION_TRACE_V3` and is
  required by the offline validator; and
* `COPY_SPELL_FAMILY_INTENT` is a pure, resolver-independent predicate whose
  failure routes to `UNOWNED_OTHER`, while strict-admission failure after a
  positive family intent is `MALFORMED_L1C_INTENT`.

The review's ID-isolation point is also closed: L1 retains its existing
provider-local counters, L1C gets separate provider-local counters, and
`(profile, sessionId, stepIndex)` is the public/session correlation. The
caller inventory records the actual `forge-game/src/main/java/forge/game/zone/MagicStack.java`
path.

No contradiction in the current accepted L1 implementation or canonical
checkpoint blocks this design. The historical conclusion that the copied-spell
path was merely engine-owned is superseded by the later R2 ownership evidence
and by the already-established distinction between the L1 trigger profile and
this new player-owned C profile; no authority document needs editing to make
the design coherent.

## 26. Design verdict

`DESIGN_APPROVED`

FRL-02L1 remains PASS. FRL-02L1C is approved for a later implementation
milestone with the exact admission, routing, projection, identity, TARGET,
teacher/BC persistence, diagnostics, and acceptance locks defined above.

The next milestone must be implementation-only after review of this artifact.
No implementation is part of FRL-02L1C design.
