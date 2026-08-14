# FRL-02L2A - SURVEIL_PARTITION Design

## Status

~~~text
Milestone: FRL-02L2A - SURVEIL_PARTITION
Task type: architecture/design only
Status: DESIGN_APPROVED_FOR_IMPLEMENTATION_PLAN
Implementation: not part of this milestone
Implementation plan: docs/superpowers/plans/2026-08-14-frl-02l2a-surveil-partition-implementation.md
Production/test behavior: unchanged by this milestone
~~~

This document is the implementation-authoritative boundary for the Surveil
graveyard-partition decision. It does not implement the decision, add a
provider/resolver/coordinator/context/candidate, change a trace version, or
change a card script.

The approved decomposition is:

~~~text
look at the actual top-N snapshot
    -> L2A: choose the graveyard subset
    -> derive the retained complement
    -> L2B: order retained cards TOP_FIRST when at least two remain
    -> engine performs zone movement, replacements, events and triggers
~~~

L2A owns only the first player choice. L2B remains a separate downstream
milestone.

## Authority checkpoint

The design was prepared from the requested C:\forgeAI checkout after the
required clean-checkpoint commands.

| Authority | Value |
| --- | --- |
| Repository | chrismaghuhn/forgeAI |
| Branch | master |
| Starting HEAD | 91d9372e856a0de3f7ce0d709e7dea8b1fb7dc57 |
| Remote state | master was up to date with origin/master |
| Starting worktree | clean |
| Starting diff | empty |
| Starting git diff --check | clean |
| Final HEAD for this design-only task | unchanged; no commit is made |

Recent commits at the checkpoint:

~~~text
91d9372e856 FRL-02L1C: add copied-spell resolve-first ORDER profile
93e89a98a48 FRL-02L1: implement exact simultaneous trigger order (#22)
9200349284b FRL-02L: audit live ORDER decision surfaces (#21)
ec520001584 FRL-02K-D1: add Blood Operative ETB confirmation
~~~

## Runtime and source evidence

### Established audit evidence

The existing FRL-02L audit reports a live Surveil callback in the canonical
workload:

~~~text
Izzet Guild Kit vs Dimir Guild Kit
10 games
seed 20260810

arrangeForSurveil callbacks: 16
input N=1: 6
input N=2: 10

for the N=2 calls:
retained/graveyard 2/0: 5
retained/graveyard 1/1: 2
retained/graveyard 0/2: 3
~~~

The audit classifies the callback as partition plus retained-top order, not as
one generic permutation. It reports an exact partition union for all 16 calls,
while native AI shuffles the retained top list. See
docs/AI-ML DOCS/FRL_02L_ORDER_ATTRIBUTION_AUDIT.md:318-452.

Those counts are prior controlled-run evidence and future implementation
acceptance targets. This design-only task does not rerun or alter that
workload.

### Current engine boundary

The current source path is:

~~~text
SurveilEffect.resolve
  -> Player.surveil
  -> StaticAbilitySurveilNum.surveilNumMod
  -> Player.getTopXCardsFromLibrary
  -> PlayerController.arrangeForSurveil
  -> returned retained/graveyard pair
  -> GameAction.moveToGraveyard
  -> reverse retained list
  -> GameAction.moveToLibrary at position 0
  -> GameEventSurveil
  -> TriggerType.Surveil
~~~

Primary source evidence:

* forge-game/src/main/java/forge/game/ability/effects/SurveilEffect.java:31-55
  computes the spell amount, handles the unrelated optional confirmation, and
  calls Player.surveil.
* forge-game/src/main/java/forge/game/player/Player.java:1052-1097
  applies StaticAbilitySurveilNum, captures topN, invokes the composite callback,
  moves graveyard cards first, reverses the retained list, moves retained cards,
  fires GameEventSurveil, and runs the Surveil trigger.
* forge-game/src/main/java/forge/game/player/Player.java:1600-1613
  shows that the actual snapshot is the first min(library size, amount) cards.
* forge-game/src/main/java/forge/game/staticability/StaticAbilitySurveilNum.java:10-31
  confirms that static Surveil modifiers are applied before the snapshot.
* forge-game/src/main/java/forge/game/player/PlayerController.java:235-236
  exposes one composite arrangeForSurveil(CardCollection) callback.
* forge-ai/src/main/java/forge/ai/PlayerControllerAi.java:592-612
  partitions with the native Scry heuristic and shuffles retained cards.
* forge-gui/src/main/java/forge/player/PlayerControllerHuman.java:1048-1086
  selects the graveyard subset and then orders retained cards in one UI callback.
  The Human path uses null for an empty side of the returned pair.
* forge-game/src/main/java/forge/game/GameAction.java:843-890 and :308-364
  show that later moves use the generic zone-change path and ReplacementType.Moved.

The preferred policy boundary is immediately after an immutable copy of topN
exists and immediately before the one native arrangeForSurveil call. At that
point no card has changed zone and no Surveil event or trigger has fired.

## Problem statement

Forge's callback returns two outputs that represent two decisions:

~~~text
left  = retained cards, in native top-first order
right = cards selected for the graveyard
~~~

L2A is the set-valued choice:

~~~text
G subset of LOOKED_AT
R = LOOKED_AT - G
~~~

The order of R is not part of the L2A action. The callback currently hides that
distinction, so an exact L2A boundary must capture the full callback once, map
only graveyard membership to the L2A teacher label, and keep retained order
private for native continuation. It must not call the native callback once for
partition capture and again for retained ordering.

## Exact semantic ownership

### L2A owns

* the complete actual looked-at top-N snapshot;
* the choosing player identity;
* the legal partition domain;
* the selected graveyard subset G;
* the deterministic binary membership classification of every looked-at item;
* the private derivation of the retained complement R.

### L2A does not own

* retained-card order or permutation;
* TOP_FIRST semantics;
* GameAction.moveToGraveyard;
* GameAction.moveToLibrary;
* replacement resolution;
* Card.setSurveilled;
* GameEventSurveil;
* TriggerType.Surveil;
* Surveil count calculation or static modifiers;
* the optional CONFIRMATION in SurveilEffect;
* the later Surveil-triggered PAYMENT decision;
* any generic partition, ORDER, or continuation framework.

The semantic L2A result is GRAVEYARD_SUBSET. The retained complement is derived
state for native processing and a future L2B child stage.

## Non-goals

FRL-02L2A does not implement:

~~~text
SURVEIL_RETAINED_TOP_ORDER
Scry
generic partition framework
generic subset combinatorics
generic library ordering
DAMAGE_ASSIGNMENT
PAYMENT
RandomLegalPolicy
trajectory generation
Behavior Cloning
RL
~~~

It does not alter SurveilAi, ComputerUtil.scryWillMoveCardToBottomOfLibrary,
the native retained-card shuffle, the Human UI, card scripts, or normal Forge
movement behavior.

## Decision representation

### Decision type

The decision type is:

~~~text
DecisionType.CARD_SELECTION
~~~

No PARTITION, SURVEIL, or ORDER value is introduced.

This is an exact semantic fit because each request asks the player to classify
one looked-at card as belonging to G or to the retained complement R. The
request is a card-selection request even though the terminal result is later
consumed as a set partition.

The current generic CARD_SELECTION implementation is not reused unchanged.
CardSelectionSession revalidates only the affected player's current hand
(CardSelectionSession.java:107-127), and CardSelectionCard exposes
(cardId, gameTimestamp) publicly (CardSelectionCard.java:10-73). Those
contracts are correct for the existing hand-selection adapters but not for a
private revealed library snapshot.

### Exact profile/adaptor

The future exact profile is:

~~~text
SurveilPartitionProfile.SURVEIL_PARTITION
~~~

It is a typed Surveil-specific sibling of the current Discard and
MULLIGAN_BOTTOM adapters. It does not add a value to the generic
CardSelectionAdapter enum and does not broaden the generic
CardSelectionSession.

DecisionRequest and LegalCandidate may carry a second typed CARD_SELECTION
payload for this profile, in the same pattern that current ORDER requests have
separate L1 and L1C typed payloads. A Surveil request contains exactly one
SurveilPartitionContext; a legacy Discard or MULLIGAN_BOTTOM request continues
to contain only the existing CardSelectionContext.

### Binary membership classification versus one subset candidate

| Alternative | Semantics | Candidate growth | Identity/trace behavior | Decision |
| --- | --- | --- | --- | --- |
| Deterministic binary classification | For each item in canonical policy traversal: GRAVEYARD or RETAIN | Exactly 2 per item request | Final native subset maps directly to one label per item; no path permutation | Chosen |
| Repeated SELECT_FOR_GRAVEYARD plus DONE | Select any remaining graveyard item, then finish | At most remaining + 1 per request | Same subset has multiple action paths; native capture cannot recover click order | Rejected |
| One subset candidate | One candidate represents all of G | 2^N candidates | Large request/hash and duplicate complexity | Rejected |
| Generic permutation/order | Treat original top-N as one ordered list | Wrong semantic family | Retained order contaminates L2A | Rejected |

The chosen decomposition represents every subset exactly once as a vector of
per-item membership labels without enumerating subsets. It does not reconstruct
an interaction order from the native callback. The canonical public-projection
traversal is only a serialization protocol for the membership vector; it is not
a player action preference or a native snapshot-position feature.

## Candidate semantics

The future typed candidate kind is:

~~~text
SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD
SurveilPartitionCandidateKind.CLASSIFY_RETAIN
~~~

Exact semantic keys are:

~~~text
SURVEIL_PARTITION|CLASSIFY_GRAVEYARD|<itemId>
SURVEIL_PARTITION|CLASSIFY_RETAIN|<itemId>
~~~

itemId is the session-local public identity of the current item. It is not a
Forge card ID, native timestamp, or original-library-position field. It is
opaque only in the contract sense: the deterministic numeric token is an
equality handle, not a cryptographic rank-hider. The implementation must not
sort candidates by itemId, expose its magnitude as a feature, or use it to
recover snapshot order.
The private snapshot ordinal is separate and is used only for native identity
validation and L2B continuity. Canonical public-projection order defines the
membership-trace sequence.

The meaning is directional and exhaustive:

~~~text
CLASSIFY_GRAVEYARD(itemId)
    this looked-at card belongs to the final intended Surveil graveyard subset

CLASSIFY_RETAIN(itemId)
    this looked-at card belongs to the retained complement
~~~

Each request contains exactly one current item and exactly two legal candidates:
CLASSIFY_GRAVEYARD and CLASSIFY_RETAIN. The request domain is complete for that
binary membership action and contains no visible-but-invalid context items.
There is no DONE candidate and no public request that represents a partially
selected subset. After the last item is classified, the provider finalizes
internally and derives G and its retained complement R.

The native callback supplies G directly. Native capture maps every captured item
to one of the two labels in canonical policy order through the private native
identity map; it never invents a native selection sequence. The resulting trace
is a membership projection, not a replay of Human click order.

## Cardinality and forcedness

The provider uses the actual snapshot size, not the printed spell amount.

| Actual snapshot size | Contract |
| ---: | --- |
| N = 0 | Current Player.surveil skips the callback. A direct empty future seam finalizes with no request. |
| N = 1 | Emit two candidates for the one item: CLASSIFY_GRAVEYARD and CLASSIFY_RETAIN. It is not forced. |
| N = 2 | Emit two candidates for item 1 and then two for item 2. Retained cardinality may be 0, 1, or 2; L2B applies only when retained cardinality is 2. |
| N >= 3 | Apply the same cardinality-independent binary contract; no subset enumeration. |

Result cases:

~~~text
graveyard = none
    choose CLASSIFY_RETAIN for every item

graveyard = one or some but not all
    choose CLASSIFY_GRAVEYARD for members of G
    choose CLASSIFY_RETAIN for the complement

graveyard = all
    choose CLASSIFY_GRAVEYARD for every item
    finalize internally after the last item
~~~

Every emitted request has exactly two candidates and DecisionRequest.isForced()
is false. The fixed request sequence is not a sequence of player choices; only
the per-item GRAVEYARD versus RETAIN candidate is a choice.

## Effective Surveil count

SurveilEffect's amount is not the L2A domain authority. Player.surveil adds
StaticAbilitySurveilNum.surveilNumMod(this), and getTopXCardsFromLibrary
truncates to the current library size. The L2A session captures:

~~~text
effectiveN = immutable lookedAtSnapshot.size()
~~~

It does not expose or reconstruct the printed amount, the static modifier, or
the spell text. A zero effective result or a short library is governed only by
the actual snapshot.

## Public information model

The cards are private to opponents but intentionally visible to the player
performing Surveil. policy-visible is not the same as globally public.

### Public SurveilPartitionCard projection

The v0 projection is deliberately minimal:

| Field | Classification | Contract |
| --- | --- | --- |
| itemId: long | REQUIRED | Session-local deterministic equality handle allocated from canonical rank by the fixed invertible mixer; its numeric value is not an ordinal, position, secret, or policy feature. |
| visibleName: String | REQUIRED | Chooser-visible display name captured at session creation. |
| visibleOracleName: String | OPTIONAL | Later observation-contract extension; not emitted by L2A v0 unless separately approved. |
| visibleTypeLine, visibleColors, visibleManaCost, visibleRulesText | OPTIONAL | Legitimate chooser-visible feature candidates for a later observation milestone; not inferred from native AI and not silently added here. |
| native card ID or game timestamp | FORBIDDEN | Forge identity/provenance, not player identity. |
| Card, CardView, CardLKI, Java object identity | FORBIDDEN | No native object crosses the public boundary. |
| owner/controller ID, zone object, original library position | FORBIDDEN | Not needed for L2A and can expose unrelated engine identity/order. |
| AI evaluation, thresholds, hand/battlefield objects, mana estimates, RNG | FORBIDDEN | Native heuristic inputs are not automatically public policy inputs. |

The implementation constructs value scalars only after a chooser-visibility
check. It must not expose CardView merely because the UI can display one.
Full card-face feature parity is a prerequisite for any stronger BC claim.

### Duplicate-looking cards

Two Island cards, or any two cards with equal public fields, remain two legal
items. Their projections may be equal except for itemId. A policy must never
collapse them by name, oracle identity, printing identity, equals, or hashCode.

### Exact public SurveilPartitionContext fields

The future typed context exposes exactly these fields:

| Field | Classification | Contract |
| --- | --- | --- |
| profile | REQUIRED | Always `SURVEIL_PARTITION`. |
| surveilSessionId | REQUIRED metadata | Controller-local parent correlation only; not a candidate key or policy feature. |
| decisionStepIndex | REQUIRED metadata | Fixed binary-classification cursor `0..N-1`; it is protocol metadata, not a reconstructed player-choice sequence. |
| choosingPlayerId | REQUIRED metadata | The stable `Player.surveil` receiver identity. |
| originalItemCount | REQUIRED metadata | The effective immutable snapshot size `N`. |
| visibleItems | REQUIRED | Immutable chooser-visible projections for every looked-at item in canonical policy traversal order; no native object, original position, or hidden field. |
| currentItemId | REQUIRED | The one item being classified by this request; it must match exactly one entry in `visibleItems`. |

There are no other public context fields in v0. In particular, `gameId`, private
snapshot ordinals, selected labels, remaining labels, native identity maps,
callback results, and retained native order remain session-private. The
`visibleItems` collection has the canonical public projection order defined
above, never the native library order. The private snapshot order remains only
for native identity/L2B continuity.

## Private native snapshot and identity

At session creation, the future coordinator/provider captures:

~~~text
immutable ordered native topN snapshot
private snapshot ordinal = 1..N
opaque request-local public itemId per item
one immutable public projection per item
private IdentityHashMap<Card, SurveilItem>
private itemId -> SurveilItem map
private native identity and stable timestamp checks for stale validation
~~~

The same native Card object twice is a SESSION_INTEGRITY_FAILURE. Two distinct
native objects with the same private stable (cardId, gameTimestamp) identity are
also rejected as a malformed snapshot. Result mapping uses native object
identity and never substitutes a same-named or same-ID object.

Initial list order assigns private native-snapshot ordinals only. It is not an
L2A action preference and is never exposed as original position. Public itemIds
are deterministic equality handles derived from canonical rank by the fixed
invertible mixer; they are not secret rank-hiders, sorted, numerically
interpreted, or used as model features. A policy-facing adapter may use an
itemId only for exact current-item lookup and candidate identity; it must omit
itemId from the observation feature vector. Policy traversal uses the separate
canonical projection order defined below.
Retained order remains private and belongs to L2B/native processing.

### Canonical policy traversal

The native snapshot order and the policy membership traversal are different
contracts:

~~~text
native topN order
    private native identity validation and L2B continuity only

policy traversal order
    canonical order of the chooser-visible projection
    independent of native topN order
~~~

For v0, the canonical policy key is the chooser-visible `visibleName`, compared
by locale-independent Java `String.compareTo` semantics (UTF-16 code-unit
lexicographic order; no locale folding or case folding). Later approved visible
fields may extend that public comparator only as part of the corresponding
observation contract. The provider never uses the native snapshot order as the
primary policy traversal key.

If two items have exactly the same policy-visible projection, they form one
public symmetry group. A private deterministic tie-break chooses an internal
iteration order for that group. The tie-break is the private stable
`(cardId, gameTimestamp)` tuple in lexicographic order; duplicate tuples are
rejected during admission. It is not serialized, is not a candidate key, is not
a diagnostic field, and is not exposed as a policy feature. The symmetry BC
rule below governs whether differing labels in that group may be used for
training.

Equivalent sessions with the same public projections and private stable tuples
allocate the same itemIds because the helper consumes canonical rank only. The
assignment of canonical rank to an exact public tie is indirectly determined
by the private `(cardId, gameTimestamp)` tie-break; that private dependency is
never exposed or treated as policy semantics. Canonical-traversal equivalence
is evaluated by the ordered public projection tuple and stable-tie mapping, not
by itemId magnitude. Within an exact symmetry group, only the symmetry
contract—not the private tie-break—has policy meaning.

## Choosing-player authority

The authority is the Player instance on which Player.surveil executes. It is
the player whose library produced topN and whose controller owns
arrangeForSurveil.

The session captures:

~~~text
gameId
choosingPlayerId = Player.surveil receiver ID
controller-local sessionId
~~~

It does not infer authority from Card.getOwner() or Card.getController().
Generation/application revalidates game and player stability, the captured
native objects in the expected library snapshot, and the one-outstanding-request
invariant.

### Provider, coordinator, and session-ID lifetime

The durable owner is the `PlayerController` instance for the choosing player.
The future controller owns exactly one long-lived
`SurveilPartitionDecisionProvider` and one long-lived
`SurveilPartitionDecisionCoordinator` for that controller instance. This is a
provider holder and orchestration seam; it is not a new `arrangeForSurveil`
resolver slot, and the abstract `PlayerController.arrangeForSurveil` signature
does not change.

The provider, not the coordinator and not a static utility, owns the
controller-local `nextSurveilSessionId` counter and the private parent-session
registry. The coordinator receives the allocated session ID, performs one
capture/mapping operation, and closes or hands off that session; it never
allocates global IDs. A parent session can remain in that controller-local
registry only until native capture/mapping is complete or the future shared L2B
seam consumes it. There is no static map and no global ID authority.

Current L2A always invokes the provider's close/remove operation after complete
post-callback trace materialization, after a native mapping failure, or after a
native callback exception when a session was registered. Capture admission
failure registers no session. No L2B transfer occurs in this milestone; a
future approved L2B seam may transfer ownership before closing instead. A
closed session is removed from the active registry and cannot create or apply
later requests.

`Player.surveil` remains the narrow engine integration point: it obtains the
controller-owned coordinator, passes the `Player.surveil` receiver as authority,
captures an immutable validation snapshot, and supplies the existing mutable
`topN` collection to `arrangeForSurveil` as the one native callback. `PlayerController` changes
only to hold and expose the provider/coordinator; it does not change Human/AI
callback behavior or add external ownership in L2A.

## Session lifecycle

### Capture-only native lifecycle

~~~text
retain the exact original mutable topN collection for the native callback
capture an immutable copy of that collection for private validation authority
  -> validate native identity and chooser visibility
  -> assign private snapshot ordinals, opaque itemIds, and public projections
  -> compute canonical policy traversal from public projections
  -> create parent Surveil session
  -> open no CARD_SELECTION RequestHandle
  -> call native arrangeForSurveil exactly once
  -> validate the complete native pair
  -> map the pair to one GRAVEYARD/RETAIN label per item
  -> for each item in canonical policy order:
       open one trace-only membership RequestHandle
       record its native-mapped CHOSEN result immediately
       close the handle before the next item
  -> derive R = snapshot - G
  -> return the original native pair unchanged
  -> keep exact retained native order private for native/L2B continuation
~~~

Capture-only does not open a public or resolver-owned membership request before
the native callback. The N post-callback handles are trace materialization
records, not additional gameplay callbacks and not policy prompts. If the
native callback throws or pair mapping fails, no membership RequestHandle or
CHOSEN row is materialized; only the capture diagnostic is recorded and the
native compatibility behavior is preserved.

### Future external-ownership lifecycle

After the shared L2B seam authorizes external ownership, the same canonical
policy traversal is used interactively:

~~~text
capture/validate parent session
  -> open one current-item RequestHandle
  -> await CLASSIFY_GRAVEYARD or CLASSIFY_RETAIN
  -> close it and advance one step
  -> repeat until every item is classified
  -> derive G and R
~~~

The one-outstanding-request invariant therefore holds in both planes: capture
has no open request during native execution and opens/closes one trace handle
at a time afterward; external ownership has at most one live request while it
waits for policy input.

The private session owns sessionId, gameId, choosingPlayerId, selectionStepIndex,
originalItemCount, private snapshot ordinals, canonical policy traversal,
itemId-to-label state, private native identity, and the retained native list
after the callback.

No ActionContinuation, priority-action sequence, wall-clock value, UUID, random
value, or generic continuation state is created. Surveil is a resolution-time
boundary and the current engine provides no valid priority continuation.

### Fail-closed profile intent

The future profile-intent boundary is deliberately narrow:

~~~text
EXACT_SURVEIL_PARTITION
    active Surveil partition session
    + complete looked-at snapshot
    + stable choosing Player.surveil authority

MALFORMED_SURVEIL_PARTITION_INTENT
    Surveil-shaped input with a missing, incomplete, stale, or inconsistent
    session/snapshot/chooser contract

UNOWNED_OTHER
    any input that is not an exact Surveil partition request
~~~

Only EXACT_SURVEIL_PARTITION may be owned by a future typed provider. Malformed
Surveil-shaped input fails closed; it is not repaired into a generic
CARD_SELECTION request. UNOWNED_OTHER remains on the native compatibility path
when such a path exists. L2A creates no classifier in this milestone.

## Native callback ownership and transition

### Transitional verdict

~~~text
L2A_TEACHER_CAPTURE_ONLY_UNTIL_L2B
~~~

L2A can be implemented independently as native teacher/history capture around
the existing composite callback. It cannot yet be exposed as a standalone
external policy resolver without a downstream retained-order seam.

The reason is concrete:

* PlayerController has no Surveil partition resolver slot;
* arrangeForSurveil owns partition and retained order together;
* external L2A followed by a second native arrangeForSurveil would rerun the
  partition choice, prompt Human twice, and consume a second AI shuffle/RNG path;
* calling native first and replacing its graveyard subset would contradict the
  completed composite decision.

The L2A-only implementation therefore supports native capture/history and keeps
the current native composite callback as the sole gameplay owner. It does not
install an external resolver. RandomLegalPolicy cannot own this partition until
the L2B/shared engine seam exists.

### One-call invariant

For every non-empty Surveil operation:

~~~text
native arrangeForSurveil callback count = exactly one on the pre-existing path
native arrangeForSurveil callback count = zero only when the pre-existing path skips it
native arrangeForSurveil callback count = never more than one
~~~

The future Player.surveil integration wraps the single controller call at the
post-snapshot/pre-movement boundary. It does not change the signatures of
PlayerController.arrangeForSurveil, PlayerControllerAi.arrangeForSurveil, or
PlayerControllerHuman.arrangeForSurveil.

The callback argument is an identity-sensitive engine contract:

~~~text
privateSnapshot = immutable copy used for admission and result validation
native arrangeForSurveil argument = exact original mutable topN instance
not the privateSnapshot, an immutable replacement, or a reconstructed collection
~~~

Native Human may mutate that original collection in place. The coordinator must
therefore preserve the original reference while keeping the validation copy
private.

### Native Human path

The coordinator creates the private parent session and canonical policy
traversal but opens no CARD_SELECTION RequestHandle. It calls the existing Human
callback exactly once, validates the returned partition, and extracts only
graveyard membership for one binary membership label per captured item. It does
not reconstruct the order in which the Human selected cards. Only after the
complete pair is valid does it materialize one trace-only membership request and
immediate native-mapped result at a time. The returned retained order is copied
into private session state and returned to the engine unchanged.

The source uses null for an empty toTop or toGrave side. The logical validation
contract is:

~~~text
pair itself must be non-null
null left  == empty retained list
null right == empty graveyard list
normalized validation lists are both non-null
~~~

This preserves the existing Human and test-controller convention while still
validating a complete two-way partition. The UI is not split into two callbacks
and is not prompted twice.

The native subset-derived binary trace is history-valid for Human, but current
L2A emits NOT_APPLICABLE for BC because observation parity has not been proven.
It represents the exact membership action, not an observed UI click-order
sequence. A separately approved observation-parity milestone is required before
either BC_ELIGIBLE or BC_EXCLUDED_PUBLIC_SYMMETRY may be emitted.

### Native AI path

The coordinator calls the existing AI callback exactly once. It uses only
graveyard membership for the binary labels and ignores retained list order. The
existing ComputerUtil heuristic, low-library branch, and CardLists.shuffle
remain unchanged. The coordinator never calls the heuristic itself and never
re-shuffles retained cards.

The AI result is history-valid when the returned partition is complete, but the
retained-order RNG cannot contaminate the L2A label. The binary membership trace
is still NOT_APPLICABLE for BC until observation parity is separately proven.

### Native result validation

Normalize empty left/right sides as above and validate:

~~~text
pair exists
every native result entry is non-null
every result card is one of the exact captured native objects
no result card appears twice
no card appears in both normalized sides
every captured item appears exactly once across both sides
result cardinality equals snapshot cardinality
no omission
no foreign card
no stale/replaced object
~~~

Retained order is not compared for L2A correctness. A valid partition with any
retained permutation is the same L2A action.

A malformed native result records the capture diagnostic MAPPING_FAILED, creates
no membership RequestHandle or CHOSEN result, and returns the original pair
unchanged so the boundary cannot repair or silently alter normal Forge behavior.
A native callback exception records the capture diagnostic
NATIVE_CALLBACK_FAILURE, creates no membership RequestHandle, and rethrows the
original exception; the callback is never called again.

### Future L2B handoff

The future shared seam carries one private parent session containing:

~~~text
same choosing player
same parent surveilSessionId
same immutable original snapshot
exact graveyard item set
exact retained native identity list
retained public itemIds unchanged from the parent Surveil session
~~~

L2B may then order the retained complement with child stage
SURVEIL_RETAINED_TOP_ORDER. It must not reconstruct the complement from names,
resnapshot the library, use public native IDs, use wall-clock values, remap
itemIds, or call arrangeForSurveil again. Every retained item keeps the exact
same parent Surveil itemId assigned by L2A.

## Failure matrix

The boundary has two different failure planes. Capture-only failures suppress
instrumentation, never native gameplay. Fail-closed ownership applies only when
a future external Surveil profile has explicitly been admitted at the shared
L2B seam.

### L2A capture-only plane

| Condition | Capture result | Public request/trace | Engine behavior |
| --- | --- | --- | --- |
| null topN or unavailable chooser authority | UNSUPPORTED_ADMISSION; capture skipped | No L2A session or teacher trace | Invoke the original native callback exactly once if the pre-existing path would invoke it; otherwise preserve the existing skip. |
| Empty actual snapshot | terminal/no capture session | No request | Current Player.surveil skips the callback. |
| Null card in initial snapshot | SESSION_INTEGRITY_FAILURE; capture skipped | No L2A session or teacher trace | Pass the original snapshot to the native path unchanged. |
| Same native object twice | SESSION_INTEGRITY_FAILURE; capture skipped | No L2A session or teacher trace | Native gameplay remains the sole owner; no repair. |
| Duplicate private stable identity | SESSION_INTEGRITY_FAILURE; capture skipped | No L2A session or teacher trace | Native gameplay remains the sole owner; no repair. |
| Card not visible to chooser | UNSUPPORTED_ADMISSION; capture skipped | No projection or teacher trace | Native gameplay remains the sole owner; never export hidden identity. |
| Native pair is null | MAPPING_FAILED after one callback | No membership RequestHandle or CHOSEN row; diagnostic only | Return the original null pair unchanged; do not repair engine behavior. |
| Foreign/stale result card | MAPPING_FAILED after one callback | No membership RequestHandle or CHOSEN row; diagnostic only | Return the original pair unchanged. |
| Omitted, duplicate, overlapping, or wrong-cardinality result | MAPPING_FAILED after one callback | No membership RequestHandle or CHOSEN row; diagnostic only | Return the original pair unchanged. |
| Native callback throws | NATIVE_CALLBACK_FAILURE | No membership RequestHandle or CHOSEN row; no second callback | Rethrow the original exception; do not alter native failure behavior. |

For every capture-only admission failure before the callback, there is no
`SurveilPartitionContext`, no owned external request, and no Teacher/BC row.
The native `arrangeForSurveil` call remains the exact original gameplay call.

### Future external-ownership plane

| Condition | Owned result | Public request/trace | Engine behavior |
| --- | --- | --- | --- |
| Session game/player/snapshot drift | STALE_SESSION | Close the owned request without a chosen label | Fail closed; do not substitute a native or neutral action. |
| Wrong request/step/profile, foreign/removed/already-classified candidate | INVALID_EXTERNAL_CANDIDATE | Terminal invalid result | Fail closed; no native fallback and no second callback. |
| External resolver returns null or throws | INVALID_EXTERNAL_CANDIDATE | Terminal invalid result | Fail closed; no native fallback and no contradictory continuation. |

Existing generic CARD_SELECTION failure concepts may be reused only where their
meaning is exact. The Surveil boundary must not report a hand-specific reason
for a library snapshot. `SESSION_INTEGRITY_FAILURE`, `UNSUPPORTED_ADMISSION`,
and `MAPPING_FAILED` in the first table are capture diagnostics, not permission
to interrupt the native engine path.

## Replacement-effect boundary

L2A records the player's intended choice of which looked-at cards are to be put
into the graveyard by Surveil. It does not claim that each selected card
definitely ends in the Graveyard.

There is no Surveil-specific replacement boundary here; the later GameAction
moves enter the generic ReplacementType.Moved path.
Replacement effects, prevention, zone-change copies, final zones,
setSurveilled, remembered objects, events, and trigger counts remain
engine-owned. The L2A label is never inferred from post-replacement zones.

## Trace contract

### Version and request records

The existing DECISION_TRACE_V3 machinery is sufficient. No new trace version is
introduced. Current V2/V3 behavior for L1, L1C, and non-Surveil decisions is
unchanged.

Future external requests and post-callback capture-materialization records carry
the same Surveil fields:

~~~text
decisionType = CARD_SELECTION
adapterOrStage = SURVEIL_PARTITION
profile = SURVEIL_PARTITION
teacherLabelEligibility = explicit typed value
decisionStepIndex = fixed membership-classification step 0..N-1
currentItemId = the one item classified by this request
legalCandidates = exactly CLASSIFY_GRAVEYARD and CLASSIFY_RETAIN for currentItemId
candidateSetHash = existing deterministic candidate-set hash
forced = false for every emitted request
~~~

V2 parsing must not infer Surveil from an old stage string. No historical
Surveil partition trace exists, so V2 compatibility is not fabricated.

DeterminismTrace already serializes typed profile and eligibility fields in V3.
The future implementation extends the closed profile enum and V3 routing, and
makes a Surveil-bearing trace V3 exactly as L1C does. It does not create a new
trace version.

Candidate keys contain only profile, operation, and request-local itemId. They
contain no card name, native ID, timestamp, object identity, library position,
wall-clock value, or RNG value.

For a valid native capture, the trace contains one post-callback membership
request per snapshot item and one chosen membership key per item. These handles
are materialized and closed sequentially in canonical policy order. The fixed
step order is a serialization of the final set, not a reconstructed Human
selection order. A capture-only admission, callback, or mapping failure emits
no owned CARD_SELECTION request or Teacher row; it records only the capture
failure in the Surveil diagnostic namespace and leaves native gameplay
untouched.

### Result records

For a successful membership projection, the existing result record is
sufficient:

~~~text
CHOSEN:
    legal selected membership key for currentItemId
    nativeCallbackCompleted = true
    mappingAttempted = true

The `nativeCallbackCompleted` flag on every post-callback CHOSEN row refers to
the one shared parent native callback. It does not mean that a new native
callback ran between this membership request and result. Capture-only
`MAPPING_FAILED` and `NATIVE_CALLBACK_FAILURE` remain existing lifecycle
vocabulary in the Surveil diagnostic namespace, but create no membership result
row because no membership handle is opened until mapping succeeds.

TRACE_INCOMPLETE:
    only for a future external request that remains open at finalization
~~~

Canonical acceptance requires no TRACE_INCOMPLETE rows. Retained order needs no
L2A result field because it is not an L2A label. A native membership projection
does not claim that the corresponding UI clicks occurred in trace step order.

### L2A trace and private session correlation

Offline trace joins use the existing trace request index plus profile/stage and
step. The in-memory L2A-to-L2B join uses:

~~~text
(parent surveilSessionId, choosingPlayerId, stage)
~~~

The parent ID is controller/provider-local, not a global ID authority. It is
not encoded into a candidate key and does not expose native identity. A future
diagnostic stream may carry a sanitized parent ID separately; the public
decision trace does not need a new version. L2B reuses every retained item's
exact parent `itemId`; there is no deterministic remapping option.

## Teacher and BC contract

Eligibility is request/result-scoped and fail-closed.

| Owner/result | History status | BC status |
| --- | --- | --- |
| Native Human, exact complete membership projection | History-valid at the membership-action boundary; not a click-order trace | NOT_APPLICABLE in current L2A; no BC outcome until a separately approved observation-parity milestone. |
| Native AI, exact complete membership projection | History-valid at the membership-action boundary | NOT_APPLICABLE in current L2A; no BC outcome until a separately approved observation-parity milestone. |
| Future external binary policy result | History-valid if exact validation succeeds | Not a native teacher; NOT_APPLICABLE. |
| Invalid native/external result | Invalid or mapping-failed | Never BC. |

The label is never inferred from candidate key, graveyard count, final zones, or
retained order. Duplicate-looking items remain distinct.

### Public symmetry contract

Define a public symmetry group by equality of every policy-visible card field
except `itemId`, session metadata, and the current-item cursor. In v0 this is
the exact `visibleName` value; future approved projection fields join the group
key only when they join the observation contract.

For each symmetry group H, compare the mapped membership labels:

~~~text
all items in H -> GRAVEYARD
    no symmetry conflict

all items in H -> RETAIN
    no symmetry conflict

some items in H -> GRAVEYARD and some -> RETAIN
    symmetry conflict
    diagnostic only in current L2A
    BC_EXCLUDED_PUBLIC_SYMMETRY only after a separately approved parity gate
~~~

The last case is history-valid as a native membership projection, but a policy
that cannot observe itemId cannot reproduce which publicly identical copy got
which label. The private tie-break does not resolve this for policy purposes.
If observation parity is not proven, the final eligibility remains
`NOT_APPLICABLE` and the symmetry conflict is recorded as a separate
diagnostic fact; `BC_EXCLUDED_PUBLIC_SYMMETRY` is never used to mask missing
observation parity. Current L2A emits neither BC_ELIGIBLE nor
BC_EXCLUDED_PUBLIC_SYMMETRY. If all publicly identical copies receive the same
label, there is no symmetry exclusion.

The existing DecisionTraceTeacherLabelEligibility values are sufficient for the
future parity-approved gate: BC_ELIGIBLE is positive, NOT_APPLICABLE is the
current fail-closed result for both native owners, and
BC_EXCLUDED_PUBLIC_SYMMETRY handles only the future proven public-symmetry
conflict above. No new enum value is created in L2A.

## Observation parity

The result is:

~~~text
PUBLIC_OBSERVATION_PARITY_NOT_YET_PROVEN
~~~

ComputerUtil.scryWillMoveCardToBottomOfLibrary reads more than the revealed
card. Current source uses AI profile thresholds, the player's all cards, hand
and battlefield, land/creature counts, mana abilities, CMC, creature
evaluation, available mana, and other chooser-owned state. The L2A v0 request
contains the item projection and session metadata, not that complete
observation. Deterministic native code is not enough to prove public parity.

Therefore native AI partition is history-valid when mapping is exact, but is not
a BC sample yet. The retained AI shuffle is never part of the L2A label. This
design does not expand into a general observation model merely to force a
positive result.

## Diagnostics

The future implementation uses a Surveil-specific diagnostic namespace. It
does not reuse Discard, MULLIGAN_BOTTOM, or generic ORDER counters with a
different meaning.

Required sanitized metrics:

~~~text
raw arrangeForSurveil invocations
non-empty Surveil partition sessions
effective N buckets: 0, 1, 2, >=3
native callback count and failures
valid native partition mappings
mapping failures by exact reason
graveyard-cardinality histogram
retained-cardinality histogram
binary membership request count
steps per session
candidate count and forced count
external attempts (must be zero for L2A-only)
trace incomplete count
public-symmetry conflict count
teacher eligibility counts
~~~

The stream must not include card names, card IDs, timestamps, native objects,
hidden zones, retained order, RNG values, or AI private state. The approved
implementation uses this separate process-local audit namespace:

~~~text
forge.surveil.partition.audit.enabled
forge.surveil.partition.audit.output
FRL02L2A_SURVEIL_AUDIT_V1
~~~

The first property enables emission, the second supplies one UTF-8 Java-
properties output path, and the third is the artifact's `schema` value. This
audit schema is not a decision-trace version; DECISION_TRACE_V3 remains the
only Surveil trace version. The diagnostics collector owns no provider, session,
or ID state, and a fresh child JVM isolates each workload. This design task
creates no runtime artifact.

## Canonical acceptance

Primary workload:

~~~text
Izzet Guild Kit vs Dimir Guild Kit
10 games
seed 20260810
~~~

Future implementation targets:

| Metric | Target |
| --- | ---: |
| Surveil partition sessions/callbacks | 16 |
| N=1 sessions | 6 |
| N=2 sessions | 10 |
| N>=3 sessions in this workload | 0; not a design blocker |
| Valid native partition mappings | 16 |
| Mapping failures | 0 |
| Trace incomplete | 0 |
| External L2A resolver attempts | 0 in this transitional milestone |
| Native callback count for non-empty sessions | exactly 16 |

For the ten N=2 sessions:

~~~text
retained/graveyard 2/0 = 5
retained/graveyard 1/1 = 2
retained/graveyard 0/2 = 3
~~~

The six N=1 graveyard cardinalities and derived binary request count must be
measured by implementation acceptance; they are not separate locked gates.

Focused fixtures may use existing Curate, Watcher in the Mist, or Found Footage.
Fangkeeper's Familiar is not preferred because Surveil 3 is mixed with
unrelated modal behavior. No card script is modified.

## Required future test matrix

### Admission and identity

~~~text
N=0, N=1, N=2, N=3, N=4
duplicate-looking cards
same native Card object twice
distinct objects with duplicate private stable identity
null card
foreign card
stale/replaced card
chooser visibility failure
stable choosing-player authority
native snapshot permutations produce the same canonical policy traversal
canonical traversal uses visibleName ordering, not native snapshot position
private symmetry tie-break is not public or serialized
~~~

### Partition choices

~~~text
classify one item as GRAVEYARD
classify one item as RETAIN
select none for graveyard
select one for graveyard
select some for graveyard
select all for graveyard
every non-empty request has exactly two candidates
finalize internally after the last item; no DONE request
~~~

### Native result validation

~~~text
valid complete partition
valid null empty side from the Human path
null pair
foreign result card
omitted card
duplicate result card
same card in both sides
wrong total cardinality
stale/replaced result object
native callback throw
native callback count remains one
~~~

### External lifecycle, only after the shared L2B seam authorizes it

~~~text
legal CLASSIFY_GRAVEYARD candidate
legal CLASSIFY_RETAIN candidate
stale candidate
foreign candidate
already-classified candidate
wrong profile or stage
wrong request or step
null return
resolver throw
~~~

L2A-only instead proves that an external resolver is not admitted and cannot
trigger a second native callback.

### Public API and hidden information

~~~text
no public Card/CardView/CardLKI/SpellAbility/Player/Game return type
no cardId or gameTimestamp in public projection or semantic key
duplicate-looking items remain distinct by itemId
itemId is request-local and not a policy feature
visibleItems use canonical projection order, never native snapshot order
foreign/stale public candidates fail closed
~~~

### Trace

~~~text
Surveil request is CARD_SELECTION with exact SURVEIL_PARTITION profile
V3 serialization is selected for a Surveil-bearing trace
old V2 traces remain unchanged
complete candidate set is recorded at every step
candidate-set hash is deterministic
all emitted requests are non-forced
native mapping flags are exact
capture-only opens no membership RequestHandle before native callback
successful capture materializes one request/result pair at a time after mapping
nativeCallbackCompleted means the shared parent callback completed
mapping/callback failure materializes no membership request/result rows
AI or Human parity failure cannot become BC_ELIGIBLE
mixed labels in one public-symmetry group are diagnostic-only in current L2A and become BC_EXCLUDED_PUBLIC_SYMMETRY only when a separate parity gate is proven
identical labels in one public-symmetry group do not trigger symmetry exclusion
no TRACE_INCOMPLETE on canonical run
parent session/stage correlation is exact
~~~

### Engine neutrality and ownership

~~~text
partition boundary is before any zone movement
native callback is exactly once on the pre-existing non-empty path and never twice
all capture-only membership trace handles are post-callback and sequential
original returned pair remains the engine input
retained order is not in the L2A label
replacement effects remain engine-owned
remembered objects/events/triggers remain engine-owned
native AI retained shuffle and RNG sequence are unchanged
~~~

## Exact file-responsibility inventory

This is a future implementation inventory only. None of these production or
test files is changed by the current design task.

### New production files

| File | Responsibility |
| --- | --- |
| forge-game/src/main/java/forge/game/decision/SurveilPartitionProfile.java | One exact profile value: SURVEIL_PARTITION. |
| forge-game/src/main/java/forge/game/decision/SurveilPartitionCandidateKind.java | Exact CLASSIFY_GRAVEYARD and CLASSIFY_RETAIN operations. |
| forge-game/src/main/java/forge/game/decision/SurveilPartitionCard.java | Immutable value-only projection containing opaque itemId and v0 chooser-visible visibleName. |
| forge-game/src/main/java/forge/game/decision/SurveilPartitionContext.java | Public value-only context with exactly profile, controller-local session metadata, fixed step metadata, choosing player, original count, complete visibleItems, and currentItemId; no native object or continuation. |
| forge-game/src/main/java/forge/game/decision/SurveilPartitionItemId.java | Package-private pure canonical-rank mixer; deterministic equality handles only, with no native/session input. |
| forge-game/src/main/java/forge/game/decision/SurveilPartitionSession.java | Private callback-local state, native snapshot identity maps, canonical policy traversal and per-item labels, stale checks, and retained native continuation state. |
| forge-game/src/main/java/forge/game/decision/SurveilPartitionDecisionProvider.java | Generates complete binary CARD_SELECTION requests, applies owned typed membership candidates, and maps native subsets without mutating Forge state. It owns the controller-local session-ID counter and parent-session registry. |
| forge-game/src/main/java/forge/game/decision/SurveilPartitionDecisionCoordinator.java | Captures the pre-callback snapshot, wraps one native callback, validates the composite pair, maps only graveyard membership, and preserves native compatibility. |
| forge-game/src/main/java/forge/game/decision/SurveilPartitionDiagnostics.java | Process-local sanitized counter aggregation and opt-in FRL02L2A_SURVEIL_AUDIT_V1 properties output; no provider/session/ID ownership and no card/native/RNG export. |

### Modified production files

| File | Minimal reason |
| --- | --- |
| forge-game/src/main/java/forge/game/player/Player.java | Route the one arrangeForSurveil call through the coordinator at the existing post-snapshot/pre-movement line; return the original pair unchanged. |
| forge-game/src/main/java/forge/game/player/PlayerController.java | Hold and expose one long-lived Surveil provider and coordinator per controller instance; no resolver slot and no arrangeForSurveil signature or Human/AI behavior change. |
| forge-game/src/main/java/forge/game/decision/DecisionRequest.java | Add one mutually exclusive typed Surveil CARD_SELECTION context and validation; retain legacy CardSelectionContext rules. |
| forge-game/src/main/java/forge/game/decision/LegalCandidate.java | Add typed Surveil candidate payload, factory, accessors, and profile-specific semantic-key validation without changing existing meanings. |
| forge-game/src/main/java/forge/game/decision/DecisionTraceRequestRecord.java | Add the closed SURVEIL_PARTITION profile and exact profile/stage helpers; do not infer it from V2. |
| forge-game/src/main/java/forge/game/decision/DeterminismTrace.java | Treat a Surveil typed profile as V3-bearing and serialize existing profile/eligibility fields; no new version. |
| forge-game/src/main/java/forge/game/decision/DecisionTraceTrainingValidator.java | Permit exact native Surveil lifecycle and require explicit BC_ELIGIBLE metadata for Surveil BC samples; fail closed otherwise. |

No DecisionType value, generic CardSelectionAdapter value, generic
CardSelectionCard field, generic CardSelectionSession rule, or existing trace
result enum value is changed.

### New test files

| File | Responsibility |
| --- | --- |
| forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionItemIdTest.java | Pure canonical-rank item-ID determinism and equality-token contract. |
| forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionDecisionProviderTest.java | Cardinality-independent binary generation/application, canonical traversal independent of native order, exact two-candidate domains, ownership, stale state, duplicate-looking items, opaque IDs, and no DONE path. |
| forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionSessionTest.java | Canonical rank assignment, private tie-break stability, equivalent-session item-ID equality, and snapshot-order independence. |
| forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionDecisionEnvelopeTest.java | Typed LegalCandidate and DecisionRequest validation without broadening generic CARD_SELECTION. |
| forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionDecisionCoordinatorTest.java | Human/AI/native mapping, null-side normalization, complete validation, capture-only fallback, post-callback materialization, callback count, failures, and retained-order isolation. |
| forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionPublicApiTest.java | Reflection and projection tests proving exact context fields, canonical visibleItems order, no native identity/hidden-object leakage, and distinct duplicate-looking IDs. |
| forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionTraceTest.java | V3 membership-projection serialization, sequential post-callback request/result flags, parent callback semantics, hashes, V2 preservation, symmetry/BC behavior, and no synthetic click-order claim. |
| forge-gui-desktop/src/test/java/forge/game/decision/SurveilPartitionEngineIntegrationTest.java | Pre-movement boundary, capture-admission fallback, no request before native callback, replacement ownership, unchanged engine pair consumption, no double callback, and RNG neutrality. |
| forge-gui-desktop/src/test/java/forge/view/FRL02L2ASurveilPartitionAuditTest.java | Canonical Izzet-vs-Dimir workload metrics and exact 16/6/10 reachability targets. |

### Modified test files

~~~text
None required by the chosen design.
~~~

Existing CardSelectionDecisionProviderTest, DecisionPublicApiReflectionTest,
DecisionTraceV2Test, DecisionTraceV3Test, L1, and L1C tests remain regressions
and are not rewritten to make Surveil fit an older contract.

### Inspected but unchanged files

~~~text
forge-game/src/main/java/forge/game/ability/effects/SurveilEffect.java
forge-game/src/main/java/forge/game/staticability/StaticAbilitySurveilNum.java
forge-ai/src/main/java/forge/ai/PlayerControllerAi.java
forge-gui/src/main/java/forge/player/PlayerControllerHuman.java
forge-ai/src/main/java/forge/ai/ability/SurveilAi.java
forge-ai/src/main/java/forge/ai/ComputerUtil.java
forge-game/src/main/java/forge/game/GameAction.java
forge-game/src/main/java/forge/game/decision/DecisionType.java
forge-game/src/main/java/forge/game/decision/CardSelectionAdapter.java
forge-game/src/main/java/forge/game/decision/CardSelectionContext.java
forge-game/src/main/java/forge/game/decision/CardSelectionCard.java
forge-game/src/main/java/forge/game/decision/CardSelectionSession.java
forge-game/src/main/java/forge/game/decision/CardSelectionDecisionProvider.java
forge-game/src/main/java/forge/game/decision/DiscardCardSelectionAdapter.java
forge-game/src/main/java/forge/game/decision/MulliganBottomAdapter.java
forge-game/src/main/java/forge/game/decision/DecisionTraceResultRecord.java
forge-game/src/main/java/forge/game/decision/DecisionTraceResultKind.java
forge-game/src/main/java/forge/game/decision/DecisionTraceTeacherLabelEligibility.java
forge-game/src/main/java/forge/game/decision/OrderProfileRouter.java
forge-game/src/main/java/forge/game/decision/SimultaneousTriggerOrderDecisionCoordinator.java
forge-game/src/main/java/forge/game/decision/CopySpellResolveFirstOrderDecisionCoordinator.java
forge-gui-desktop/src/test/java/forge/game/decision/CardSelectionDecisionProviderTest.java
forge-gui-desktop/src/test/java/forge/game/decision/DecisionTraceV3Test.java
~~~

The existing CardSelection implementation is therefore reused only as contract
evidence and as a separate legacy decision family, not as a generic Surveil
implementation.

## Architecture alternatives

### Alternative A - extend the existing CARD_SELECTION family

Add a Surveil adapter value and generalize CardSelectionSession/CardSelectionCard
to revealed library snapshots. This looks small at enum level, but requires
non-hand live lookup, a second identity scheme, a second projection, and a
second semantic operation. The current provider uses native ID/timestamp keys
and hardcodes hand revalidation. It would either leak identity or force legacy
Discard/MULLIGAN semantics to depend on Surveil. Rejected.

### Alternative B - Surveil-specific provider/coordinator under CARD_SELECTION

Keep the correct binary CARD_SELECTION family while giving Surveil its own
typed context, public projection, private snapshot, candidate operations,
integrity checks, native mapper, controller-owned provider lifetime, and future
L2B handoff. Change the shared request/trace envelope only through mutually
exclusive typed payloads. This is the smallest exact contract. Chosen.

### Alternative C - nested L2A/L2B contracts at the composite engine seam

This is the correct future shape for external policy ownership: one parent
Surveil operation, a partition child stage, and a retained-order child stage
with one native continuation. It cannot be delivered as L2A-only because the
current callback owns both decisions. Introducing it now would make L2A
operationally depend on L2B. Deferred to the shared-seam milestone.

### Alternative D - one complete subset candidate

Represent the set-valued choice with up to 2^N subset candidates. This has no
semantic benefit over repeated classification and makes candidate identity,
trace size, and duplicate handling scale with the power set. Rejected.

## Chosen architecture

~~~text
Alternative B now:
Surveil-specific typed CARD_SELECTION provider/coordinator
    + one narrow Player.surveil observation wrapper
    + canonical visible-projection traversal independent of native topN order
    + native teacher/history capture only
    + post-callback sequential membership-trace materialization
    + existing DECISION_TRACE_V3 envelope
    + private parent session for future L2B

Alternative C later:
shared engine seam for external L2A plus retained-order L2B
~~~

The architecture separates:

~~~text
public item identity       -> request-local itemId
private native mapping     -> IdentityHashMap<Card, SurveilItem>
legal action               -> CLASSIFY_GRAVEYARD or CLASSIFY_RETAIN
semantic result            -> graveyard subset
native compatibility       -> original composite pair, one callback
retained order             -> private L2B/native continuation only
~~~

It preserves the established FRL style: narrow exact profile, typed
context/candidates, controller-local/session-local identity, complete legal
domains, fail-closed owned input, native compatibility for unowned paths, and
deterministic trace semantics.

## Rejected alternatives

~~~text
DecisionType.PARTITION       rejected: CARD_SELECTION represents the binary membership choice exactly
DecisionType.SURVEIL         rejected: effect family is not the decision semantics
DecisionType.ORDER           rejected: retained ordering is L2B, not L2A
2^N subset candidates        rejected: power-set explosion
generic CardSelection reuse rejected: hand-only lookup and native identity leakage
SELECT_FOR_GRAVEYARD + DONE rejected: same subset has multiple paths and native callback has no click order
two native callbacks         rejected: Human double prompt and AI/RNG divergence
L2A resolver before L2B      rejected: no exact retained-order continuation exists
AI deterministic rewrite     rejected: changes native policy/RNG and is out of scope
~~~

## Open risks and mitigations

| Risk | Mitigation/decision |
| --- | --- |
| Human returns a null empty side | Normalize the side for validation and return the original pair unchanged. |
| Human mutates the callback collection | Capture an immutable native snapshot before invoking it. |
| AI retained shuffle contaminates teacher | Label only graveyard membership; never inspect/serialize retained order. |
| AI observation parity is incomplete | Use NOT_APPLICABLE until a separate observation proof exists. |
| Native object replaced between snapshot and result | Use private identity mapping and reject stale/foreign objects. |
| Duplicate-looking cards collapse | Use itemId and private identity; never match by name/equality. |
| External resolver added too early | Do not add a resolver slot in L2A; require the L2B seam. |
| Replacement redirects a move | Define L2A as intended membership; leave resulting zones to engine. |
| V2/V3 trace ambiguity | Emit new Surveil traces as V3 with explicit profile; do not infer from V2. |
| Candidate leaks native identity | Semantic keys contain only profile, operation, and itemId. |
| Projection grows silently | Treat additional chooser-visible fields as a separately reviewed observation change. |
| Request sequence leaks native snapshot order | Traverse by canonical visible projection order; keep native order private for identity/L2B only. |
| Duplicate-looking cards receive conflicting labels | Record the public-symmetry group as diagnostic-only in current L2A; use BC_EXCLUDED_PUBLIC_SYMMETRY only after a separately approved parity gate. |
| Capture trace opens requests before native result exists | Open no handle before callback; materialize one post-callback request/result pair at a time. |
| Result flag is misread as N native callbacks | Define nativeCallbackCompleted as the shared parent-callback lifecycle flag. |

## Implementation readiness verdict

~~~text
FRL-02L2A design is ready for design review.
~~~

The teacher-capture portion is implementable after design approval without
changing card scripts or implementing L2B. External L2A policy ownership is not
implementation-ready by itself and remains gated by the future shared engine
seam.

Implementation remains separate from this design milestone. The approved
execution contract is recorded in
docs/superpowers/plans/2026-08-14-frl-02l2a-surveil-partition-implementation.md.

## Final review record

The final design review confirmed all items below with no remaining P1, P2-HIGH,
P2, or P3 findings:

~~~text
the CARD_SELECTION plus typed Surveil sibling contract
the bounded binary GRAVEYARD/RETAIN decomposition
the canonical policy traversal independent of native snapshot order
the minimal public projection and forbidden identity fields
the exact public SurveilPartitionContext fields
the public-symmetry BC exclusion contract
the PlayerController-owned provider/coordinator/session-ID lifetime
the one-native-callback teacher mapping
the post-callback sequential membership-trace materialization
the L2A_TEACHER_CAPTURE_ONLY_UNTIL_L2B verdict
the exact Player.surveil integration point
the V3/no-new-version trace decision
the file-responsibility inventory
~~~

~~~text
FRL_02L2A_DESIGN_APPROVED_FOR_IMPLEMENTATION_PLAN
~~~
