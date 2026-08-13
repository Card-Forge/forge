# FRL-02L — Live ORDER Attribution Audit

**Status:** `FRL_02L_PASS`
**Date:** 2026-08-13
**Authoritative checkpoint:** `ec52000158448811bafb76763c3117f6e2101f75`
**Branch:** `frl/02l-order-attribution-audit`
**Worktree:** `C:\forgeAI-order-audit`

## Executive verdict

Forge does contain live, modern, agent-relevant ordering decisions in the controlled v0 slice. The corrected authority distinguishes three semantic areas:

1. `SIMULTANEOUS_TRIGGER_ORDER`: a pure permutation of two or more abilities controlled by the same player before stack insertion.
2. `COPY_SPELL_RESOLVE_FIRST_ORDER`: the proven Replicate/`CopySpellAbilityEffect` copied-spell resolve-first ordering seam; discovered in the controlled workload but not implemented.
3. `SURVEIL_PARTITION_PLUS_ORDER`: a Surveil partition decision followed, when two or more cards remain on top, by a relative top-library order.

The first profile is the current L1 implementation slice. The copied-spell
profile requires its own design checkpoint and is not admitted by
`SIMULTANEOUS_TRIGGER_ORDER`. The Surveil profile is not a pure permutation
callback and must remain decomposed from the card-partition decision. No
generic `ORDER` adapter or provider is implemented here.

The raw shared callback surface is not the semantic denominator:

```text
raw multi-item orderSimultaneousSa callbacks = 20
SIMULTANEOUS_TRIGGER_ORDER sessions = 19
COPY_SPELL_RESOLVE_FIRST_ORDER-like sessions = 1
```

`FRL-02L1R2_COPY_SPELL_ORDER_OWNERSHIP_AUDIT.md` corrected the original
callback-wide inference after runtime type and Human-controller ownership
inspection. The original raw measurement remains historical evidence; only its
semantic attribution is corrected.

`FRL-02I legacy combat order != FRL-02L live ORDER attribution.` The old blocker/attacker damage-assignment callbacks remain excluded from v0. Modern direct combat damage distribution remains `DAMAGE_ASSIGNMENT`, not `ORDER`.

## 1. Checkpoint

The primary checkout `C:\forgeAI` was synchronized and clean before the worktree was created. The following were verified after `git fetch origin`:

```text
HEAD          = ec52000158448811bafb76763c3117f6e2101f75
origin/master = ec52000158448811bafb76763c3117f6e2101f75
git diff --stat = empty
git diff        = empty
git diff --check = clean
```

No primary or retained worktree was modified.

## 2. Branch and isolated worktree

```text
worktree: C:\forgeAI-order-audit
branch:   frl/02l-order-attribution-audit
HEAD:     ec52000158448811bafb76763c3117f6e2101f75
origin/master: ec52000158448811bafb76763c3117f6e2101f75
merge-base:   ec52000158448811bafb76763c3117f6e2101f75
```

The audit worktree was created from the synchronized checkpoint. Temporary runtime diagnostics and generated output were worker-local and are not part of the final production change.

## 3. Changed files

Final source/document changes are limited to:

```text
docs/AI-ML DOCS/FRL_02L_ORDER_ATTRIBUTION_AUDIT.md
docs/AI-ML DOCS/ML_STRATEGY.md
docs/superpowers/plans/2026-08-13-frl-02l-order-attribution-audit.md
```

No card script, game-rule implementation, player decision implementation, or ForgeRL provider was changed.

## 4. Authority read before attribution

The audit read the current architecture authority in `ML_STRATEGY.md`, the FRL-02K confirmation audit, the FRL-02K-D1 Blood Operative audit, and the available historical combat/ORDER references. The existing strategy explicitly treated aggregate `ORDER` counts as unattributed and required a separate audit before `DAMAGE_ASSIGNMENT`.

The audit preserves the earlier FRL-02I conclusion instead of rewriting it.

## 5. Source ORDER inventory

The complete controller-facing and game-semantic ordering inventory is:

```text
PlayerController.orderSimultaneousSa
PlayerController.orderAndPlaySimultaneousSa
PlayerController.orderBlockers
PlayerController.orderBlocker
PlayerController.orderAttackers
PlayerController.arrangeForScry
PlayerController.arrangeForSurveil
PlayerController.orderMoveToZoneList
PlayerController.orderCosts
MagicStack simultaneous-trigger queue/APNAP preparation
TriggerHandler simultaneous-trigger collection
TriggerWaiting ordered trigger collection
GameActionUtil.orderCardsByTheirOwners
ReorderZoneEffect
RearrangeTopOfLibraryEffect
DigEffect / DigUntilEffect / ChangeZoneEffect / ChangeZoneAllEffect
DiscardEffect / DestroyEffect / ConniveEffect graveyard ordering callers
Combat legacy damage-order methods
Combat.assignCombatDamage
Zone.reorder / human hand reordering
Zone.sort and deterministic engine canonical sorting
Human replacement-effect selection
ReverseTurnOrderEffect
```

Incidental comparator sorts used only to produce a canonical engine collection order are included under `DETERMINISTIC_INTERNAL_ORDER`; they are not treated as player policy seams.

## 6. PlayerController ordering APIs

`forge.game.player.PlayerController` declares the ordering surfaces at the following semantic boundaries:

| API | Meaning | Final classification |
|---|---|---|
| `orderSimultaneousSa(List<SpellAbility>)` | Permute same-player simultaneous stack abilities | `LIVE_AGENT_ORDER` when `n >= 2`; `FORCED_ORDER` otherwise |
| `orderAndPlaySimultaneousSa(List<SpellAbility>)` | Order, then execute/insert abilities | `DUPLICATE_ENGINE_SURFACE` / compound execution |
| `orderBlockers(Card, CardCollection)` | Legacy blocker damage order | `LEGACY_COMBAT_ORDER` |
| `orderBlocker(Card, Card, CardCollection)` | Legacy incremental blocker order | `LEGACY_COMBAT_ORDER` |
| `orderAttackers(Card, CardCollection)` | Legacy attacker damage order | `LEGACY_COMBAT_ORDER` |
| `arrangeForScry(CardCollection)` | Scry partition plus top-card order | `UNREACHED_IN_V0_CARD_POOL`; future `PARTITION_PLUS_ORDER` |
| `arrangeForSurveil(CardCollection)` | Surveil partition plus retained-top order | `LIVE_AGENT_ORDER` subdecision within `SURVEIL_PARTITION_PLUS_ORDER` |
| `orderMoveToZoneList(CardCollectionView, ZoneType, SpellAbility)` | Caller-dependent zone permutation, sometimes after selection | `UNSUPPORTED_ORDER` in the aggregate; split by caller/profile |
| `orderCosts(List<CostPart>)` | Payment-cost order | `OTHER_DECISION_TYPE` (`PAYMENT`) |

The callback names are not sufficient to establish agent ownership. The caller and destination semantics are required.

## 7. AI ordering APIs

`PlayerControllerAi` delegates simultaneous ability ordering to `AiController.orderPlaySa`. That routine categorizes abilities by API and builds a native order using discard, draw, counters, evolve, pump, and token groups. It is heuristic policy code, not a legal-candidate generator.

The AI implementations have these semantics:

* `orderSimultaneousSa` returns a permutation for the observed calls, but `AiController.orderPlaySa` filters the supplied list in place before rebuilding the result. A future teacher capture must snapshot the input before invoking native AI.
* `arrangeForSurveil` partitions cards using the native scry heuristic and then calls `CardLists.shuffle(toTop)`. That native shuffle is a runtime AI policy fact and consumes Forge RNG; the audit instrumentation itself did not consume RNG.
* `arrangeForScry` has the same partition-plus-native-shuffle shape, but was not reached by the current two-deck pool.
* `orderMoveToZoneList` only reorders a graveyard list for the Volrath's Shapeshifter branch or applies deterministic library heuristics. Neither branch was relevant to the selected v0 decks.
* `orderCosts` returns the original list.
* The legacy combat order methods delegate to `AiBlockController` but are gated off by the controlled rules.

## 8. Human ordering APIs

`PlayerControllerHuman` provides the UI counterparts:

* `orderSimultaneousSa` prompts only when the list has at least two items and its `needPrompt` logic finds a meaningful distinction, a target, or an `OrderDuplicates` trigger. It maps saved decisions by descriptions and `indexOf`, which is not a safe semantic identity for duplicate-looking abilities.
* `orderAndPlaySimultaneousSa` iterates the chosen list in reverse before stack insertion so the GUI's `ResolveFirst` ordering matches LIFO resolution.
* `arrangeForScry` and `arrangeForSurveil` separately select the cards sent away and order the remaining top cards.
* `orderMoveToZoneList` prompts only for destinations/callers that permit it. Graveyard ordering is gated by the graveyard-order preference and `Game.isGraveyardOrdered`; explicit `ReorderZone` bypasses that preference.
* `orderCosts` prompts only under full-control cost-order mode and with at least two costs.
* `orderBlockers`, `orderBlocker`, and `orderAttackers` are the human legacy combat paths.

The human UI is not the canonical AI-vs-AI runtime, but it confirms which source seams are intended to be player-owned when the corresponding rules/effect profile is reached.

## 9. Simultaneous-trigger route

The live route is:

```text
TriggerHandler creates a WrappedAbility
    -> MagicStack.addSimultaneousStackEntry
    -> MagicStack.addAllTriggeredAbilitiesToStack
    -> chooseOrderOfSimultaneousStackEntry
    -> controller.orderAndPlaySimultaneousSa
    -> controller.orderSimultaneousSa
    -> returned items are inserted into MagicStack
```

`TriggerHandler` explicitly notes that non-static triggers are ordered later in `MagicStack`. `MagicStack` does not reorder arbitrary existing stack objects; it only collects pending simultaneous entries before stack insertion.

## 10. APNAP findings

`MagicStack.addAllTriggeredAbilitiesToStack` obtains players in turn order and performs two passes: non-`AbilityTriggered` entries, then `AbilityTriggered` entries. For each active player, `chooseOrderOfSimultaneousStackEntry` filters by trigger class and by activating player, falling back to the host-card controller when no activating player is present.

Therefore:

* APNAP/turn-order grouping is engine-owned.
* Only entries controlled by the same player are passed to one ordering callback.
* A player cannot permute another player's entries through this seam.
* The callback does not own cross-player APNAP ordering.

## 11. Simultaneous-trigger strategic semantics

This is a real Magic-rules ordering decision when two or more abilities controlled by one player are pending together. The returned permutation changes the order in which the abilities are inserted into the LIFO stack, and therefore can change resolution order and state.

The AI's native category ordering is evidence that Forge treats the family as policy-bearing rather than as a canonical sort. The runtime observed seven non-identity permutations in the raw 20 multi-item calls. R2 establishes that the raw family contains 19 simultaneous-trigger sessions and one separate player-owned copied-spell session.

## 12. Canonical runtime counts

Workload:

```text
Izzet Guild Kit vs Dimir Guild Kit
10 games
seed 20260810
```

The opt-in audit recorder wrote sanitized request-local indices only. It did not write card names, raw `Card`, `SpellAbility`, `CardLKI`, or hidden zone objects.

| Surface | Calls | n=0 | n=1 | n=2 | n>=3 | Acting players |
|---|---:|---:|---:|---:|---:|---|
| `orderSimultaneousSa` | 116 | 0 | 96 | 14 | 6 | player 0: 48; player 1: 68 |
| `orderAndPlaySimultaneousSa` | 116 | 0 | 96 | 14 | 6 | player 0: 48; player 1: 68 |
| `arrangeForSurveil` | 16 | 0 | 6 | 10 | 0 | player 1: 16 |
| `orderMoveToZoneList` | 16 | 0 | 0 | 8 | 8 | player 0: 16 |
| `arrangeForScry` | 0 | 0 | 0 | 0 | 0 | none |
| `orderBlockers` | 0 | 0 | 0 | 0 | 0 | none |
| `orderBlocker` | 0 | 0 | 0 | 0 | 0 | none |
| `orderAttackers` | 0 | 0 | 0 | 0 | 0 | none |
| `orderCosts` | 0 | 0 | 0 | 0 | 0 | none |

The two `orderAndPlaySimultaneousSa` rows are not additional decisions; they are the compound execution wrapper around the pure ordering callback.

## 13. Item-count distribution

For `orderSimultaneousSa`, the 20 raw multi-item calls were distributed as follows:

```text
n=2: 14 calls
n=3: 5 calls
n=4: 1 call
```

The corrected semantic attribution is:

```text
raw multi-item callbacks = 20

SIMULTANEOUS_TRIGGER_ORDER:
  n=2: 13 sessions
  n=3: 5 sessions
  n=4: 1 session
  sessions: 19

COPY_SPELL_RESOLVE_FIRST_ORDER-like:
  n=2: 1 session
  sessions: 1
  status: DISCOVERED / NOT_IMPLEMENTED
```

For the exact L1 profile, the sequential request distribution is candidate
size 2 = 19, candidate size 3 = 6, candidate size 4 = 1, for 26 requests.

## 13.1 FRL-02L1R2 authority correction

The original FRL-02L audit treated the 20 raw multi-item
`orderSimultaneousSa` callbacks as 20 strategic sessions for one profile.
FRL-02L1R initially separated the non-trigger shape from the trigger profile
but incorrectly classified its ownership as engine-owned. FRL-02L1R2 resolved
the Human-controller ownership dispute: the exact Replicate/Pyromatics callback
is player-owned, but it belongs to the separate
`COPY_SPELL_RESOLVE_FIRST_ORDER`-like semantic family.

The corrected relationship is:

```text
orderSimultaneousSa = heterogeneous controller surface
  -> SIMULTANEOUS_TRIGGER_ORDER (19 canonical sessions, current L1)
  -> COPY_SPELL_RESOLVE_FIRST_ORDER-like (1 canonical session, open)
```

The one copied-spell callback is not a missing L1 admission and must not be
called engine-owned. It remains on native fallback until its separately
authorized profile is designed and implemented.

Among those calls, player 0 received 15 and player 1 received 5. Seven returned permutations differed from the captured input order:

```text
n=2, player 0: 3 reverse permutations
n=3, player 0: 1 non-identity permutation
n=2, player 1: 1 reverse permutation
n=3, player 1: 1 non-identity permutation
n=4, player 0: 1 non-identity permutation
```

All 116 returned lists were valid request-local permutations: no observed invalid item, duplicate, omission, or result-size mismatch. The native AI mutated the supplied input list in 11 calls, all at multi-item sizes; this is why a future boundary must capture the pre-callback snapshot.

## 14. Current two-deck reachability

The current card-pool files contain triggered abilities capable of producing simultaneous stack entries, and the runtime directly observed same-player groups of two to four. This is source-plus-runtime proof of v0 reachability; no inference from a zero aggregate count was used.

The current deck scripts also contain Surveil profiles, including Surveil 1 and Surveil 2. The selected pool contains no Scry, `ReorderZone`, or `RearrangeTopOfLibrary` card profile, and no selected card carries `NeedsOrderedGraveyard`.

## 15. Targeted fixture evidence

No focused fixture was required for simultaneous triggers or Surveil: both source reachability and direct canonical runtime calls answered the ownership question. Adding a synthetic trigger or enabling legacy combat rules would have changed the controlled slice and was intentionally avoided.

The source-reachable but unobserved Scry/explicit-zone families remain future-pool profiles, not evidence for enabling legacy ORDER.

## 16. Library ordering surfaces

`RearrangeTopOfLibraryEffect` obtains a fixed top-card list and delegates to `orderMoveToZoneList` before moving the cards one at a time. This is a true future `ORDER` profile when the card/effect is reached.

`ReorderZoneEffect` delegates non-random ordering to the same callback and uses engine RNG for random ordering. It is a future explicit zone-order profile.

The canonical workload produced no `Library` `orderMoveToZoneList` call. The current two-deck card scripts contain no `ReorderZone` or `RearrangeTopOfLibrary` API use. These surfaces are `SOURCE_REACHABLE` but `V0_CARD_POOL_UNREACHABLE`.

## 17. Top/bottom ordering surfaces

`PlayerController.orderMoveToZoneList` reverses the returned sequence when cards are moved to the top of a library because movement order and final top-to-bottom order are opposite. This is a representation detail that must not be confused with the strategic semantic order.

The source also contains fixed-membership top-library ordering in `RearrangeTopOfLibraryEffect` and caller-dependent `Dig`/`DigUntil` paths. Selection of which cards move is distinct from ordering the fixed selected/revealed set.

## 18. Scry findings

Scry is implemented as a partition-plus-order callback, not as a single permutation:

```text
choose which revealed cards go to bottom
then order the cards that remain on top
```

`arrangeForScry` was not reached in the controlled two-deck workload. Its native AI implementation partitions with the scry heuristic and shuffles the retained top list. It is `SOURCE_REACHABLE`, `RUNTIME_NOT_OBSERVED`, and `FUTURE_POOL_ONLY` for this v0 card pool.

## 19. Surveil findings

Surveil has the same decomposition, but it is live in v0:

```text
which cards move to graveyard?       CARD_SELECTION component
which cards remain on top?            complement of that selection
what is their relative top order?     ORDER component when at least two remain
```

Runtime distribution for `arrangeForSurveil`:

```text
input n=1: 6 calls (top/graveyard partition only)
input n=2: 10 calls
top/graveyard sizes 2/0: 5 calls
top/graveyard sizes 1/1: 2 calls
top/graveyard sizes 0/2: 3 calls
```

All five `2/0` calls exercised a real retained-top ordering domain. The native top order was `0;1` three times and `1;0` twice. The overall callback must not be flattened into one generic `ORDER` request; it is an exact `SURVEIL_PARTITION_PLUS_ORDER` profile.

Blood Operative's Surveil-triggered PayLife<3 path remains `PAYMENT`-owned and unrelated to this ordering result.

## 20. Stack-order findings

Forge supports ordering simultaneous pending abilities before stack insertion. It does not expose a callback that reorders arbitrary existing stack objects.

The native stack is LIFO (`MagicStack.push` uses `addFirst`). AI and human compound methods use opposite iteration directions so their controller-specific list conventions can produce their intended native behavior. A future agent contract must define the semantic direction explicitly, preferably “resolve first,” and privately translate to the native insertion convention.

## 21. Combat-order classification

The following are all `LEGACY_COMBAT_ORDER`:

```text
Combat.orderBlockersForDamageAssignment
Combat.addBlockerToDamageAssignmentOrder
Combat.orderAttackersForDamageAssignment
PlayerController.orderBlockers
PlayerController.orderBlocker
PlayerController.orderAttackers
```

`Combat` copies `GameRules.hasOrderCombatants()` into `legacyOrderCombatants` and only calls the controller order methods when that flag is enabled. The controlled Constructed `SimulateMatch` path creates `GameRules` without enabling `setOrderCombatants(true)`. The canonical run observed zero legacy combat-order callbacks.

## 22. Legacy FRL-02I disposition

FRL-02I remains historical evidence about old combat damage ordering. This FRL-02L audit does not revive it, enable it, or use it to manufacture observations.

```text
FRL-02I legacy combat order
!=
FRL-02L live ORDER attribution
```

No ORDER provider is created for blockers-before-damage or attackers-before-damage ordering.

## 23. Modern DAMAGE_ASSIGNMENT separation

`Combat.assignCombatDamage` is a separate callback. In the controlled rules path, `!legacyOrderCombatants` contributes `overrideOrder=true`, so modern direct damage distribution is owned by `assignCombatDamage`, not by the old order callbacks.

This milestone only establishes the separation. It does not audit candidate generation or implement `DAMAGE_ASSIGNMENT`.

## 24. Forced-order findings

An item count of zero or one does not create a policy decision:

```text
orderSimultaneousSa n=1: 96 observed calls, forced
arrangeForSurveil n=1: 6 calls, partition choice only; no relative ORDER
orderMoveToZoneList: callers guard multi-item calls; one item is forced
legacy combat order with <=1 item: forced and disabled in v0
```

Forced callbacks may be recorded for history in a future audit, but must not generate policy samples or an agent request.

## 25. Permutation explosion analysis

The observed simultaneous-trigger maximum was four, but the source contract is not bounded to four. The audit does not recommend flattening a size-`n` ordering into `n!` candidate objects.

Recommended future representation for a true pure order profile:

```text
ORDER_START
remaining = [A, B, C, ...]

SELECT_NEXT(A/B/C/...)
remaining -= selected

repeat until one item remains
last item = forced
```

For Surveil, this sequence must be nested after or coordinated with the separate partition/card-selection decision; it must not be represented as a generic permutation of the original top-N list.

## 26. Semantic identity and duplicate-item findings

Raw Java object identity is not an agent-visible semantic ID. `SpellAbility` and `Trigger` have internal IDs, and a trigger exposes a source-trigger ID, but descriptions and source-card text can repeat. Copied abilities can also have duplicate-looking descriptions.

The audit recorder used request-local ordinals plus private identity checks. A future projection should use:

```text
public request-local ordinal / semantic key
private native identity guard for exact mapping
```

The guard may use the native `SpellAbility`/`Trigger` identity and source-card identity privately, but those objects and hidden fields must not be serialized into ForgeRL observations. The human description/macro path is not sufficient by itself because it maps duplicates through text and `indexOf`.

## 27. Hidden-information findings

| Profile | Information classification | Finding |
|---|---|---|
| Simultaneous trigger order | `PUBLIC_ONLY` for the stack-facing semantic profile; `UNSAFE_WITH_CURRENT_PROJECTION` for implementation | Ordered entries are public stack decisions, but no approved ORDER projection exists. |
| Surveil retained-top order | `PLAYER_PRIVATE_SAFE` for the chooser; `UNSAFE_WITH_CURRENT_PROJECTION` | The chooser sees the top cards; the opponent must not receive their identities. |
| Explicit library/zone order | `MIXED_VISIBILITY` | Revealed-card and private-library callers share one native callback. |
| Legacy combat order | `PUBLIC_ONLY`, but excluded | Not a v0 agent boundary. |

No raw `Card`, `CardLKI`, `SpellAbility`, `GameObject`, hidden card name, or opponent-private object was exported by the audit.

## 28. Native teacher-label feasibility

| Profile | Native mapping result | Teacher classification |
|---|---|---|
| `SIMULTANEOUS_TRIGGER_ORDER` | 116/116 valid request-local permutations; native AI mutates input in 11 calls | `TEACHER_SAFE_WITH_REQUEST_LOCAL_IDENTITY` |
| `SURVEIL_PARTITION_PLUS_ORDER` | Partition union was exact in all 16 calls; retained top order is separate from partition, but native AI shuffles the retained top with RNG | `TEACHER_UNSAFE` until a policy-specific deterministic/native-label contract is proven; not a pure ORDER label today |
| `orderMoveToZoneList` aggregate | Caller-dependent selection/order and hidden-zone cases | `TEACHER_UNSAFE` as one aggregate family |
| legacy combat order | Disabled and out of scope | `NOT_AGENT_DECISION` |

The native callback is not invoked more than once for the same pure simultaneous group in the observed route. The compound wrapper invokes it once and then performs stack insertion. Teacher collection is not implemented here.

## 29. ActionContinuation findings

The simultaneous-trigger callback is engine-generated during stack preparation, not a child subdecision of the originating priority action. The callback receives no `ActionContinuation`, `decision_sequence_id`, or `subdecision_index`.

Surveil is a resolution-time callback reached through `SurveilEffect` and `Player.surveil`; it is not a priority-action callback. The existing native method receives no priority-action continuation metadata, and the `PhaseHandler` continuation scope ends around action announcement before later stack resolution. A future boundary must therefore use an independent resolution-context identity, must not synthesize or inherit an unrelated priority continuation, and may add correlation only if a native provenance seam later proves it.

## 30. State and RNG neutrality

The temporary recorder was opt-in, worker-local, sanitized, and limited to request-local identity indices and counts. It did not invoke AI helpers, mutate game objects, reorder live lists, or call random APIs.

Audit-on and audit-off canonical runs each produced 44 determinism files. SHA-256 comparison found:

```text
gameplay traces:       10/10 identical
RNG traces:            10/10 identical
RNG diagnostic traces: 10/10 identical
decision traces:       identical where present
summary properties:    10/10 identical
file-set mismatches:   0
```

The native Surveil AI shuffle consumes RNG as part of gameplay policy; that fact is recorded, not altered. The audit path itself is RNG-neutral.

## 31. Source/runtime reconciliation

| Family | Source status | Runtime status | v0 status | Future status |
|---|---|---|---|---|
| Simultaneous trigger order | `SOURCE_REACHABLE` | `RUNTIME_OBSERVED` | `V0_CARD_POOL_REACHABLE` | live |
| Surveil retained-top order | `SOURCE_REACHABLE` | `RUNTIME_OBSERVED` | `V0_CARD_POOL_REACHABLE` | live |
| Scry partition-plus-order | `SOURCE_REACHABLE` | not observed | `V0_CARD_POOL_UNREACHABLE` | `FUTURE_POOL_ONLY` |
| Explicit library/reorder-zone order | `SOURCE_REACHABLE` | not observed | `V0_CARD_POOL_UNREACHABLE` | `FUTURE_POOL_ONLY` |
| Graveyard order with relevant card | `SOURCE_REACHABLE` | callback observed, no relevant order choice | `V0_CARD_POOL_UNREACHABLE` | `FUTURE_POOL_ONLY` |
| Legacy combat order | source exists but rules-gated | not observed | excluded | legacy only |
| Modern damage distribution | source exists separately | not audited here | next milestone | `DAMAGE_ASSIGNMENT` |
| Cost order | source exists | not observed | `PAYMENT`/AI-owned | separate payment work |

Observed callback volume is not used as a proxy for strategic ownership.

## 32. Full ownership matrix

| Surface / callers | Ordered object | v0 reachable? | Observed | n distribution | Strategic? | Forced? | Rules/engine owner | Classification | Visibility | Native mapping | Teacher | Continuation | Future type | Before v0 gate? | Blocker |
|---|---|---:|---:|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `PlayerController.orderSimultaneousSa`; AI `orderPlaySa`; Human implementation | heterogeneous copied/trigger stack entries | yes | 116 raw | 0/96/14/6 | profile-dependent | profile-dependent | shared controller surface; stack engine | `MULTI_PROFILE_CONTROLLER_SURFACE` | profile-specific | valid permutation; AI input mutation | profile-specific | absent | profile-specific | attribution required | not one denominator |
| exact wrapped non-static trigger route | `WrappedAbility` stack entries | yes | 19 | n=2:13, n=3:5, n=4:1 | yes | n<=1 | same-player CR 603.3b order; stack engine | `SIMULTANEOUS_TRIGGER_ORDER` | public stack profile | valid permutation; AI input mutation | safe with request-local ID | absent | `ORDER` / sequential | current L1 | 19/19 gate |
| `CopySpellAbilityEffect` -> `orderAndPlaySimultaneousSa` | copied `SpellApiBased` spell entries | yes | 1 | n=2:1 | yes | n<=1 under explicit encoding | copy effect/controller seam; stack engine | `COPY_SPELL_RESOLVE_FIRST_ORDER` | public copied-spell seam | private native mapping required | safe with request-local ID | absent | `ORDER` / sequential | future design | not implemented |
| `orderAndPlaySimultaneousSa` AI/Human | ordered abilities plus execution | yes | 116 wrapper calls | 0/96/14/6 | no separate seam | n<=1 | controller execution | `DUPLICATE_ENGINE_SURFACE` | same as child | no returned result | not separate | absent | none | no | compound wrapper |
| `TriggerHandler.runSingleTriggerInternal` and `addSimultaneousStackEntry` | pending wrapped abilities | yes | route source | n/a | no; queueing | empty queue | trigger/stack engine | `ENGINE_OWNED_ORDER` | engine internal | no policy result | not a decision | absent | none | no | none |
| `TriggerWaiting.setTriggers` | trigger collection order | yes | route source | n/a | no | n/a | linked insertion order | `DETERMINISTIC_INTERNAL_ORDER` | engine internal | no policy result | not a decision | absent | none | no | none |
| `MagicStack.addAllTriggeredAbilitiesToStack` / `chooseOrder...` | per-player pending groups | yes | 116 groups | same as child | APNAP no; child order yes | empty/one group | APNAP/activator filtering | `ENGINE_OWNED_ORDER` plus child delegation | public group membership | exact filtered input | child only | absent | child `ORDER` | yes through child | no |
| `MagicStack.push` / direct stack add | stack instances | yes | child route | LIFO | no | n/a | engine LIFO | `DETERMINISTIC_INTERNAL_ORDER` | public stack | no policy result | not a decision | absent | none | no | none |
| `PlayerController.arrangeForSurveil`; `Player.surveil`; Surveil effects | partitioned top cards | yes | 16 | 0/6/10/0 | selection + order | n=1 order forced | chooser owns partition/order | `LIVE_AGENT_ORDER` subprofile | chooser-private | exact partition; top permutation | safe with request-local ID | absent | `SURVEIL_PARTITION_PLUS_ORDER` | yes | no decomposed seam |
| `PlayerController.arrangeForScry`; `GameAction.scry`; Scry effects | partitioned top cards | no | 0 | source n>=1 | future selection + order | n=1 order forced | chooser owns partition/order | `UNREACHED_IN_V0_CARD_POOL` | chooser-private | native partition/shuffle | future only | absent | `PARTITION_PLUS_ORDER` | no | no current card |
| `orderMoveToZoneList`; `ReorderZoneEffect`; `RearrangeTop...` | fixed zone card list | no for true profile | 16 graveyard plumbing | observed 0/0/8/8 | future only for explicit order | n<=1 | caller plus chooser | `UNSUPPORTED_ORDER` / future profile | mixed | valid in fixed-list branch; aggregate unsafe | aggregate unsafe | absent | exact zone `ORDER` | no | no current explicit effect |
| `GameActionUtil.orderCardsByTheirOwners` | cards grouped by owner/controller | source yes; v0 order profile no | caller route | per-owner n>1 | APNAP no; delegate may | n<=1 | APNAP/timestamp engine | `ENGINE_OWNED_ORDER` | mixed caller | grouped output | not a decision | absent | caller-specific | no | no |
| `Dig`/`DigUntil`/`ChangeZone`/`Discard`/`Destroy`/`Connive` callers | selected/revealed cards | callback observed | 16 graveyard calls | 2/3+ only | selection/order compound; no current strategic order | n<=1 | effect + destination rules | `OTHER_DECISION_TYPE` / future zone order | mixed | caller-specific | unsafe aggregate | absent | selection plus exact order | no | no relevant v0 graveyard card |
| `Combat.orderBlockersForDamageAssignment` / `orderBlockers` | blockers | rules-gated | 0 | n/a | no in v0 | <=1 | legacy rules | `LEGACY_COMBAT_ORDER` | public | native AI/UI | not agent | absent | none | no | `orderCombatants=false` |
| `Combat.addBlockerToDamageAssignmentOrder` / `orderBlocker` | incremental blocker list | rules-gated | 0 | n/a | no in v0 | first blocker forced | legacy rules | `LEGACY_COMBAT_ORDER` | public | incremental native/UI | not agent | absent | none | no | historical FRL-02I |
| `Combat.orderAttackersForDamageAssignment` / `orderAttackers` | attackers | rules-gated | 0 | n/a | no in v0 | <=1 | legacy rules | `LEGACY_COMBAT_ORDER` | public | native AI/UI | not agent | absent | none | no | historical FRL-02I |
| `Combat.assignCombatDamage` | damage distribution map | yes as separate future surface | not audited | source-dependent | yes, but not ORDER | n/a | modern combat damage | `DAMAGE_ASSIGNMENT_NOT_ORDER` | public combat state | separate callback | next audit | separate | `DAMAGE_ASSIGNMENT` | next milestone | implementation not in FRL-02L |
| `CostPayment.orderCosts`; AI/Human `orderCosts` | cost parts | source yes | 0 | n/a | payment-owned | n<=1 | payment engine/full control | `OTHER_DECISION_TYPE` | public cost state | AI identity / GUI order | payment-specific | absent | `PAYMENT` | no | outside scope |
| `Zone.reorder`; `reorderHand`; protocol/UI | hand cards | UI source | 0 | n/a | no | n/a | presentation/client | `PRESENTATION_ONLY_ORDER` | player UI | UI identity | not agent | absent | none | no | `UI_ORDER_HAND=false` in v0 |
| `Zone.sort`, timestamp/card comparator sorts, canonical collection sorting | engine collections | yes internally | n/a | n/a | no | n/a | deterministic engine | `DETERMINISTIC_INTERNAL_ORDER` | internal | canonical comparator | not a decision | absent | none | no | none |
| Human `chooseSingleReplacementEffect` | replacement effects | source yes | not canonical AI | n/a | replacement choice | n<=1 | replacement engine/controller | `OTHER_DECISION_TYPE` | caller-dependent | native selection | replacement-specific | absent | `REPLACEMENT` | no | outside scope |
| `ReverseTurnOrderEffect` | player turn sequence | source yes | not runtime | n/a | rules effect, not permutation policy | n/a | engine rule/effect | `OTHER_DECISION_TYPE` | public | deterministic effect | not a decision | absent | none | no | outside scope |

## 33. True live ORDER profiles

```text
SIMULTANEOUS_TRIGGER_ORDER
status: LIVE_AGENT_ORDER
v0 reachable: yes
minimum strategic n: 2
observed strategic n: 2..4
visibility: public stack semantic profile; current projection unsafe
native teacher mapping: safe with request-local identity
recommended representation: sequential SELECT_NEXT, final item forced
```

```text
SURVEIL_RETAINED_TOP_ORDER
status: LIVE_AGENT_ORDER subdecision
v0 reachable: yes
minimum strategic n: 2 retained top cards
observed retained-top n: 2 in 5 calls
visibility: chooser-private
native teacher mapping: safe only after partition/order decomposition and request-local identity
recommended representation: partition/card-selection first, then sequential order of retained top cards
```

```text
COPY_SPELL_RESOLVE_FIRST_ORDER
status: DISCOVERED / NOT_IMPLEMENTED
v0 reachable: yes, exact Replicate/Pyromatics path
minimum strategic n: 2 copied spells
observed sessions: 1
visibility: public copied-spell ordering seam; exact projection requires a separate design
native teacher mapping: safe only with request-local identity
recommended representation: separate sequential RESOLVE_FIRST profile before per-copy TARGET setup
```

## 34. Controlled-v0 ORDER semantic inventory

The corrected controlled-v0 inventory contains three distinct player-owned
semantic areas, with different implementation status:

```text
SIMULTANEOUS_TRIGGER_ORDER
  status: current L1 implementation profile

COPY_SPELL_RESOLVE_FIRST_ORDER
  status: DISCOVERED / OPEN / NOT IMPLEMENTED

SURVEIL_PARTITION_PLUS_ORDER (its retained-top ORDER subdecision)
  status: OPEN / NOT IMPLEMENTED
```

Only `SIMULTANEOUS_TRIGGER_ORDER` is covered by the current L1 acceptance
gate. No legacy combat order, generic stack reorder, cost order, hand UI order,
or aggregate zone callback is admitted by this audit.

## 35. Future-pool-only ORDER profiles

The following remain future-pool-only:

```text
SCRY_PARTITION_PLUS_ORDER
EXPLICIT_REORDER_ZONE
REARRANGE_TOP_OF_LIBRARY
RELEVANT_GRAVEYARD_ORDER
MULTI-CARD LIBRARY/BATTLEFIELD/EXILE ORDER from fixed-membership effects
```

They are not reasons to enable a generic ORDER provider now.

## 36. Recommended representation

Do not flatten permutations into `n!` candidate objects. Define the future request in terms of a public, sanitized semantic item projection and a sequential remaining-set contract. Use a private request-local identity guard to validate that the native callback result is a permutation of the exact input.

For simultaneous triggers, define whether the selected item means “resolve next” or “insert next” and translate once at the native boundary. For Surveil, keep partition/card selection separate from retained-top ordering.

## 37. Focused tests

The final verification records the existing FRL-02K0 determinism, B1 Gelectrode confirmation, C2A Blood TARGET, D1 Blood CONFIRMATION, `DecisionTraceV2`, `PriorityActionDiagnostics`, `TriggeredTargetDecisionCoordinator`, and `ConfirmationDecisionProvider` locks. The temporary order recorder was compile-checked and exercised only in the isolated worktree; it was removed before the final commit.

## 38. Canonical workload

The instrumented canonical run exited successfully after ten games. Results were:

```text
1 Dimir win, turn 10
2 Izzet win, turn 10
3 Dimir win, turn 15
4 Dimir win, turn 11
5 Izzet win, turn 8
6 Dimir win, turn 16
7 Izzet win, turn 9
8 Dimir win, turn 11
9 Dimir win, turn 19
10 Dimir win, turn 9
```

The same outcomes and deterministic traces were produced with the audit path disabled.

## 39. Broad tests

The required full reactor test command completed successfully:

```text
mvn -pl forge-gui-desktop -am test
BUILD SUCCESS
Tests run: 727
Failures: 0
Errors: 0
Skipped: 6
all six reactor modules: SUCCESS
checkstyle: 0 violations in every module
```

An earlier 10-minute execution-budget timeout occurred before the same command was rerun with a sufficient outer allowance. It is retained as an execution note, not as a test result. The completed rerun is the authoritative result.

## 40. Package / validate / checkstyle

This section is filled with the exact local results for:

```text
mvn -pl forge-gui-desktop -am -DskipTests package
mvn -pl forge-gui-desktop -am validate
git diff --check
```

Results:

```text
mvn -pl forge-gui-desktop -am -DskipTests package: BUILD SUCCESS
  checkstyle: 0 violations in every module
  forge.exe and forge-gui-desktop-2.0.14-SNAPSHOT-jar-with-dependencies.jar assembled
mvn -pl forge-gui-desktop -am validate: BUILD SUCCESS
git diff --check: exit 0
```

Focused retained-boundary locks also completed successfully: 89 decision tests; FRL02KConfirmationAuditTest 4; FRL02KChangesZoneProjectionAuditTest 1; FRL02KRemainingConfirmationAuditTest 1; FRL02KTriggeredTargetExternalOwnershipAuditTest 23; FRL02KTriggeredTargetOwnershipAuditTest 1; SimulateMatchDeterminismTest 3. Each had zero failures, errors, and skips.

## 41. P0 / P1 / P2

```text
P0: none.

P1 resolved by this audit:
- live same-player simultaneous-trigger ordering was identified and measured;
- the raw 20-callback surface was separated from the exact 19-session L1
  trigger profile;
- FRL-02L1R2 corrected the one copied-spell callback from `ENGINE_OWNED` to a
  separate player-owned, not-yet-implemented semantic profile;
- live Surveil retained-top ordering was separated from card partition;
- legacy combat order was proven rules-gated and separated from modern damage distribution;
- native input mutation was captured as a future teacher-mapping constraint.

P2:
- future-pool Scry and explicit zone-order profiles remain deferred;
- duplicate-looking human description mappings require request-local identity in a future seam;
- native Surveil retained-top ordering is RNG-driven in the current AI and is not a trustworthy BC teacher label until a deterministic/policy-specific label contract is proven;
- no generic ORDER provider exists yet, by design.
```

Independent architecture review: P0 none; P1 none. The exact profile split, APNAP ownership, legacy combat exclusion, and continuation/teacher caveats are supported by the source/runtime evidence. Independent evidence review: P0 none; P1 none; P2 only the bounded-sample caveat. The audit-on/off neutrality claim is scoped to the controlled 10-game run and its 44-file per-run traces. The six skipped full-suite tests are the `forge.net.NetworkPlayIntegrationTest` stress tests, explicitly skipped unless `-Drun.stress.tests=true` is supplied.

No P0/P1 attribution issue remains unresolved for the milestone. The P2 items are explicit future boundaries, not hidden blockers.

## 42. ORDER_DECISION

```text
ORDER_DECISION:
V0_ORDER_REQUIRED
```

The corrected controlled-v0 semantic inventory is:

```text
SIMULTANEOUS_TRIGGER_ORDER                 PASS (`FRL_02L1_PASS`)
COPY_SPELL_RESOLVE_FIRST_ORDER             DISCOVERED / OPEN / NOT IMPLEMENTED
SURVEIL_PARTITION_PLUS_ORDER               OPEN / NOT IMPLEMENTED
ORDER_V0_COMPLETE                          false
```

## 43. Next milestone and Draft PR

```text
NEXT:
FRL-02L1
IMPLEMENT_SIMULTANEOUS_TRIGGER_ORDER
```

`FRL-02L1` is the recommended first exact production slice because it is a pure same-player stack permutation with direct canonical reachability. It must not become a generic permutation adapter. After the L1 gate, the next design checkpoint is `FRL-02L1C DESIGN_COPY_SPELL_RESOLVE_FIRST_ORDER`; `FRL-02L2 SURVEIL_PARTITION_PLUS_ORDER` remains separate and is not silently folded into the first slice.

The branch is intended for a Draft PR titled:

```text
FRL-02L: audit live ORDER decision surfaces
```

The PR remains Draft and must not be merged or marked ready. No ORDER implementation or `DAMAGE_ASSIGNMENT` implementation begins in this milestone.

## 44. STOP

STOP
