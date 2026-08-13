# FRL-02L1R2 — Copy-Spell ORDER Ownership and Target-Semantics Audit

Status: source-complete attribution audit; no production or test changes

Audit checkout:

- repository: `C:\\forgeAI-order-l1`
- branch: `frl/02l1-simultaneous-trigger-order`
- required HEAD: `3cd5c311f7e508d4533d6951c873c7570b39f708`
- authoritative base: `9200349284b3489a6a349de378c773bdfa2f6efc`
- protected checkout: `C:\\forgeAI` (not used for this audit)

This document resolves the remaining ownership and target-lifecycle question
for the exact multi-item callback observed in Game 9, callback ordinal 94:
two copied Pyromatics `SpellApiBased` `DealDamage` abilities produced by
`Replicate` / `CopySpellAbilityEffect`.

The existing L1R document remains unchanged. Its historical conclusion is
reviewed near the end of this document rather than silently rewritten.

## Scope and recorded runtime shape

The exact observed entries were:

```text
count = 2
isTrigger = false
WrappedAbility = false
isSpell = true
isCopied = true
ApiType = DealDamage
source = Pyromatics
current target = the same Dimir Guildmage for both entries at the ORDER snapshot
```

The raw canonical distribution remains:

```text
raw orderSimultaneousSa callbacks = 116
n=1 = 96
n=2 = 14
n=3 = 5
n=4 = 1
raw multi-item callbacks = 20
```

The existing exact L1 trigger profile remains unchanged at 19 sessions and 26
requests. This audit does not alter that profile, its admission rules, or its
canonical test.

## 0. Checkpoint verification

The checkpoint was verified in the intended worktree before source review:

```text
git status
git diff --stat
git diff
git diff --check
git rev-parse HEAD
git branch --show-current
```

Result:

```text
HEAD   = 3cd5c311f7e508d4533d6951c873c7570b39f708
branch = frl/02l1-simultaneous-trigger-order
```

The only pre-existing uncommitted path was:

```text
docs/AI-ML DOCS/FRL_02L1R_SPELL_API_BASED_ORDER_RECONCILIATION.md
```

That file is the authorized preceding L1R audit and was preserved. No
production or test change was present, and the protected `C:\\forgeAI`
checkout was not reset, checked out, or modified.

## 1. Human-controller ownership

### 1.1 Complete `orderSimultaneousSa` path

The relevant complete implementation is
`forge-gui/src/main/java/forge/player/PlayerControllerHuman.java:2257-2356`.

For a list with fewer than two items the method returns the input immediately.
For the exact two-item callback, the method continues and evaluates:

```java
boolean needPrompt = !activePlayerSAs.get(0).isTrigger();
```

Because the first copied Pyromatics entry has `isTrigger == false`,
`needPrompt` is true at
`forge-gui/src/main/java/forge/player/PlayerControllerHuman.java:2262-2266`.
The later `usesTargeting()` check at lines 2277-2281 can also require a prompt,
but it is not needed to establish the result for this callback.

The method therefore does not take the no-prompt return at lines 2288-2289.
It proceeds through the remembered-order lookup and reaches the GUI order call
at lines 2328-2334. The first-prompt form passes:

```text
Select order for simultaneous abilities
Resolve first
```

The exact English values are defined by
`forge-gui/res/languages/en-US.properties:1540-1543`:

```text
lblSelectOrderForSimultaneousAbilities=Select order for simultaneous abilities
lblResolveFirst=Resolve first
```

The saved-order form uses `Reorder simultaneous abilities` with the same
`Resolve first` semantic label. Both forms return an ordered list of
`SpellAbilityView` objects, which the method maps back to the corresponding
native abilities using its private `spellViewCache` at lines 2314-2343.

This is source-level evidence that the Human controller deliberately treats a
non-trigger multi-item list as an orderable player interaction. It is not a
trigger-only helper.

### 1.2 Non-identity order, default, and cancellation behavior

The player can choose a non-identity permutation. `DualListBox` presents the
source list and appends selected entries to the destination list in the order
selected (`forge-gui-desktop/src/main/java/forge/gui/DualListBox.java:76-100`).
For two entries, selecting the second entry first produces the non-identity
order.

The `Auto` action is a default/shortcut that moves all remaining entries while
preserving the current source order
(`DualListBox.java:461-501`). Thus identity order is available, but it is not
the only available order.

The normal dialog does not expose a separate cancel button. `GuiChoose.order`
returns the ordered list from the dialog at
`forge-gui-desktop/src/main/java/forge/gui/GuiChoose.java:239-261`.
The dialog is configured with `DO_NOTHING_ON_CLOSE` at line 248, so ordinary
window close/Escape is not a normal cancel path. The controller nevertheless
has a defensive `chosen == null` fallback at
`PlayerControllerHuman.java:2336-2339`, which returns the original list. That
fallback applies to a custom GUI result or dialog/execution failure, not to a
normal user-selected cancellation action.

### 1.3 Remembered and macro orders

Remembered/saved orders can replay a prior non-identity order automatically.
The Human controller checks its `orderedSALookup` and `rememberedKeys` at
`PlayerControllerHuman.java:2306-2312`. A remembered index order is returned
before opening the ordinary selection dialog.

The macro path is backed by
`forge-gui/src/main/java/forge/player/RecordActionsMacroSystem.java:147-165`.
It validates that the recorded descriptions form a complete multiset and can
therefore consume a prior non-identity `StackOrderAction` without asking the
player again. The action stores descriptions rather than native object IDs
(`forge-game/src/main/java/forge/game/decision/StackOrderAction.java:5-20`).

There is an important duplicate-description limitation: replay resolves a
description to the first remaining matching ability in
`PlayerControllerHuman.orderSpellAbilitiesByDescription`,
`PlayerControllerHuman.java:2372-2385`. The GUI itself retains a private
view-to-native mapping and can distinguish duplicate-looking entries, but a
description-only macro can be ambiguous for identical copies. That limitation
does not remove the Human player's original ability to choose a non-identity
order.

### 1.4 Authority of the returned Human list

`PlayerControllerHuman.orderAndPlaySimultaneousSa` calls
`orderSimultaneousSa` and then consumes the returned list at
`PlayerControllerHuman.java:2389-2413`. It iterates the returned semantic list
in reverse, performs copied-spell target setup for each item, and adds each
item to `MagicStack`.

The returned order is therefore authoritative for later stack insertion. It is
not merely diagnostic metadata or an ignored hint.

### 1.5 Human answers

For the exact rejected shape:

1. `needPrompt` becomes true: **YES**.
2. The code reaches `getGui().order(...)`: **YES**.
3. The semantic instruction is **“Select order for simultaneous abilities”**
   with the destination label **“Resolve first”**.
4. A non-identity permutation is possible: **YES**.
5. Identity/default behavior is possible through the initial order or `Auto`;
   the normal dialog has no explicit cancel button. A defensive null/failure
   fallback returns the original order.
6. Remembered and macro/saved orders can automatically replay a prior
   non-identity order, subject to the description-based duplicate limitation.
7. The returned order is authoritative for subsequent insertion: **YES**.
8. The same helper is intended to cover both simultaneous triggers and
   non-trigger simultaneous abilities/spell copies: **YES**. Normal trigger
   collection reaches the helper through `MagicStack`, while
   `CopySpellAbilityEffect` calls the controller helper directly.

The Human evidence is incompatible with classifying this exact callback as
engine-owned merely because the entries were created internally by a copy
effect.

## 2. Human/AI semantic comparison

| Property | Human controller | AI controller |
|---|---|---|
| callback reached | `orderAndPlaySimultaneousSa` calls `orderSimultaneousSa`; exact non-trigger list prompts | `orderAndPlaySimultaneousSa` calls `orderSimultaneousSa`; exact non-trigger list reaches the coordinator/native fallback |
| player/order UI | Yes: `getGui().order(...)` | No visible UI; AI chooses through `AiController.orderPlaySa(...)` |
| ordering heuristic | Human-selected, remembered, or macro order | API-category heuristic; exact `DealDamage` pair remains in input order |
| identity order possible | Yes | Yes; exact pair returns identity |
| non-identity order possible | Yes, by selecting entries in another order | Yes for other API combinations; the helper is not identity-only in general |
| `RESOLVE_FIRST` semantic | Explicit GUI label `Resolve first` | Same semantic seam, but native returned list is consumed as insertion order by the AI loop |
| final returned list used for insertion | Yes; Human reverses semantic list before LIFO insertion | Yes; AI iterates its native insertion list forward before LIFO insertion |

### 2.1 AI call path

The complete AI controller path is
`forge-ai/src/main/java/forge/ai/PlayerControllerAi.java:1362-1408`.
Its `orderSimultaneousSa` delegates to
`SimultaneousTriggerOrderDecisionCoordinator.order(...)` and then to
`getAi()::orderPlaySa` when no external resolver handles the list
(`PlayerControllerAi.java:1362-1365`). The exact non-trigger copied-spell
batch is rejected by the current trigger-only admission rule, so it reaches
the native AI order helper for the current run.

`AiController.orderPlaySa` is implemented at
`forge-ai/src/main/java/forge/ai/AiController.java:2235-2300`. It filters and
rebuilds the list by API categories. `DealDamage` does not match the category
filters in the exact pair, so the native result preserves identity for this
case. The source also permits non-identity category ordering for other API
combinations; identity is not the semantic definition of the callback.

The helper mutates the supplied list while filtering. A future external
boundary must snapshot request-local native identity before invoking it and
validate/map the returned permutation afterward. The exact path contains no
random operation, and the observed return is a full permutation of the input
items. The discard-related branch consults the AI player's own hand; no hidden
opponent information is needed for this exact DealDamage pair.

### 2.2 AI classification

The AI helper is **an AI implementation of the same player-owned ordering
seam**, not proof that the seam is an internal engine-only reorder. Its exact
identity result is an output of the current heuristic, not evidence that the
Human order is unavailable or non-authoritative.

For a future exact copy-spell profile, the native AI callback is best classified
as `SAFE_WITH_REQUEST_LOCAL_IDENTITY`: it is deterministic for this pair and
does not use hidden information in the observed branch, but it mutates native
lists and must be bounded by request-local identity and full-permutation
validation.

## 3. Complete `CopySpellAbilityEffect` lifecycle

The exact source order is:

```text
Pyromatics card definition
  -> Replicate spell-cast trigger
  -> TriggerHandler creates WrappedAbility
  -> WrappedAbility.resolve
  -> CopySpellAbilityEffect.resolve
  -> copy construction
  -> orderAndPlaySimultaneousSa(copies)
  -> orderSimultaneousSa(copies)
  -> per-copy target setup during controller insertion loop
  -> MagicStack insertion
  -> later stack resolution
```

### 3.1 Card and Replicate construction

`forge-game/src/main/java/forge/game/card/CardFactoryUtil.java:1713-1722`
defines Replicate as a spell-cast trigger whose overriding ability is
`CopySpellAbilityEffect` with `MayChooseTarget$ True`. The source card is
`forge-gui/res/cardsfolder/p/pyromatics.txt:1-6`:

```text
K:Replicate:1 R
A:SP$ DealDamage | ValidTgts$ Any | NumDmg$ 1
```

The card's replicate copy may choose new targets, matching the copy flags
observed at runtime.

### 3.2 Trigger wrapper and copy-effect resolution

`forge-game/src/main/java/forge/game/trigger/TriggerHandler.java:457-532`
obtains and copies the overriding ability, wraps it in `WrappedAbility`, and
queues the wrapper through the stack path. The wrapper's resolution at
`forge-game/src/main/java/forge/game/trigger/WrappedAbility.java:409-502`
calls `playSpellAbilityNoStack` at line 495. The wrapper is the Replicate
effect, not one of the copied Pyromatics damage abilities.

`PlaySpellAbility` resolves the no-stack ability through
`forge-game/src/main/java/forge/game/player/PlaySpellAbility.java:575-580`
and `:673-729`; the no-stack branch calls `AbilityUtils.resolve` rather than
inserting the wrapper as a normal stack item. `AbilityUtils` dispatches the
ability API at
`forge-game/src/main/java/forge/game/ability/AbilityUtils.java:1297-1329`
and `:1375-1399`.

`CopySpellAbilityEffect.resolve` is
`forge-game/src/main/java/forge/game/ability/effects/CopySpellAbilityEffect.java:65-210`.
For ordinary Replicate, it constructs every copy first at lines 153-169. Each
copy is made with
`CardFactory.copySpellAbilityAndPossiblyHost`, and
`copy.setMayChooseNewTargets(true)` is applied at lines 159-161. The complete
copy list is then passed at line 204 to:

```java
controller.getController().orderAndPlaySimultaneousSa(copies);
```

There is no target prompt between construction of the individual copies and
this order callback.

### 3.3 Copy flags and target state

`CardFactory.copySpellAbilityAndPossiblyHost` at
`forge-game/src/main/java/forge/game/card/CardFactory.java:127-166` copies a
spell host into a new copied-spell object and marks the copied ability with
`setCopied(true)` at line 146. `SpellAbility.copy` at
`forge-game/src/main/java/forge/game/spellability/SpellAbility.java:1243-1317`
creates a new native ability and copies the existing `TargetChoices` at
lines 1304-1307, while resetting `mayChooseNewTargets` before the copy-effect
code turns it back on.

Therefore the recorded two-item list arrives at ORDER with copied target state
already present, but with target replacement still pending.

## 4. Target interaction after ordering

### 4.1 Actual per-controller order

Human insertion is
`PlayerControllerHuman.orderAndPlaySimultaneousSa:2389-2413`:

```text
returned semantic list = [first-to-resolve, second-to-resolve, ...]
iterate returned list from last to first
  move copied host to Stack
  if may choose new targets: setupNewTargets(player)
  add to MagicStack
```

AI insertion is
`PlayerControllerAi.orderAndPlaySimultaneousSa:1372-1408`:

```text
returned AI list = native insertion order
iterate returned list from first to last
  move copied host to Stack
  if may choose new targets: setupTargets()
  add to MagicStack
```

The AI path explicitly documents that `setupNewTargets` is not implemented
for AI and would break the AI, so it uses fresh `setupTargets` and restores
the previous targets when the flag is false (`PlayerControllerAi.java:1395-1402`).
Human uses `setupNewTargets`, whose implementation at
`PlayerControllerHuman.java:2506-2521` can retain the old target or choose new
targets.

The two controller loops therefore differ in native list convention, but both
use the order result to determine which copied ability is processed and added
at each insertion step.

The first copied host is moved to the Stack and added before the next copied
ability's target setup begins. Consequently, the subsequent target prompt can
observe the changed stack state, even though the exact Pyromatics `Any` target
enumeration does not include Stack objects.

### 4.2 Target setup is after ORDER

The target methods confirm the ordering is not inferred from names:

- `SpellAbility.setupNewTargets` is
  `forge-game/src/main/java/forge/game/spellability/SpellAbility.java:2138-2157`.
  It invokes the controller's `chooseNewTargetsFor` for the current copied
  ability and restores the previous target if no replacement is chosen.
- `SpellAbility.setupTargets` is
  `SpellAbility.java:2159-2205`. It clears the old targets and asks the
  controller to choose fresh legal targets.
- Human target choice is routed through
  `PlayerControllerHuman.java:2506-2521`.
- AI fresh target choice is routed through
  `PlayerControllerAi.java:1560-1568`; AI's `chooseNewTargetsFor` returns
  null because that capability is not implemented.

Consequently, for both controller paths, ORDER returns before target setup for
the copied spells. Target setup is performed in the returned-order-dependent
insertion loop, once per copy, before that copy is pushed to the stack.

### 4.3 Exact Pyromatics target domain

Pyromatics uses `ValidTgts$ Any`. The relevant target restrictions are
`forge-game/src/main/java/forge/game/card/TargetRestrictions.java:130-149`
and `:454-486`, with candidate enumeration at `:568-592`. For this card, the
target domain includes players and battlefield cards/creatures. It does not
include the Stack merely because a copied spell host is being inserted.

For the exact two-copy callback:

- each copy can independently receive fresh target setup: **YES**;
- target setup follows the returned-order-dependent insertion sequence: **YES**;
- the returned order determines which native copy receives the first and
  subsequent target prompt: **YES**;
- stack state differs after each insertion: **YES**;
- the legal `Any` target set changes solely because the other copied spell was
  inserted: **NO** for this exact card, because insertion changes the Stack,
  not the players/battlefield candidate domain;
- a copied spell resolves only after both copies have been inserted: **YES**.

The absence of a target-domain change between insertions does not make ORDER
engine-owned. It only limits one possible source of divergence for this exact
target filter.

### 4.4 Can identical copies diverge after ORDER?

**YES.** They are identical at the recorded ORDER snapshot in the observed
public fields and initially carry the same Dimir Guildmage target, but after
ORDER each native copied spell independently passes through target setup. The
player can retain the old target for one copy and choose a different legal
player or battlefield target for the other, or choose different new targets
for both. The target decisions are attached to the individual returned native
items.

The final copied stack entries can consequently have different target state.
Their relative stack/resolution order is also controlled by the chosen order.
The copy objects are not resolved during the target-setup loop, so any state
change caused by damage occurs later; nevertheless, the resolved target/order
pair can affect later game state and outcome. In particular, damage to one
target may matter before damage to the next target, and target legality at
resolution can depend on state changed by earlier resolution.

For the exact `Any` domain, the intermediate insertion itself does not remove
or add a legal battlefield/player target. The strategic seam comes from the
player-owned resolve order plus the independent target choices and subsequent
stack resolution, not from stack objects becoming legal `Any` targets.

## 5. Magic semantics represented by Forge

The callback is semantically ordering a batch of simultaneously created
spell-copy stack entries before those entries are inserted and resolved. It is
not merely an execution-time sort:

1. the Human UI explicitly asks for the order;
2. the returned order is used to select the insertion sequence;
3. target replacement/setup happens separately for each copied spell during
   that insertion sequence;
4. `MagicStack.add` pushes entries with `addFirst` at
   `forge-game/src/main/java/forge/game/zone/MagicStack.java:523-562`, and
   `resolveStack` resolves `peekFirst` at `:564-621`.

The precise Forge semantic is therefore:

```text
order simultaneously created copied-spell stack entries
with the requested list expressed as resolve-first order,
then perform each copy's target setup and stack insertion.
```

The UI wording is a Forge implementation of a genuine player-facing order
choice. The Java callback name alone would not establish this, but the Human
control flow and the authoritative use of its result do.

## 6. Strategic relevance test

### 6.1 Exact observed state

Are the two copies strategically interchangeable in the exact recorded ORDER
snapshot?

**YES, at the snapshot only.** They have the same recorded public source/API,
copy flags, and current target. Swapping their private native identities does
not expose a different value in the observed callback payload.

### 6.2 Decision seam in general

Can the player's returned order affect later target choices, stack order,
resolution order, legal choices, or game outcome?

**YES.** Target setup is later and per copied ability; the final stack order
and resolution order are determined by the returned order; and distinct target
choices can create different target/resolution state. The fact that this
particular callback begins with duplicate-looking values does not remove the
player-owned seam.

The two questions are therefore not conflated: snapshot equivalence is a fact
about the current values, while ownership and later semantic consequences are
facts about the callback and lifecycle.

## 7. Forced/equivalent candidate analysis

The existing ForgeRL rule in
`forge-game/src/main/java/forge/game/decision/DecisionRequest.java:279-281`
defines a forced request as one with exactly one legal candidate. A two-item
order has two legal permutations, even when the entries currently display the
same public values.

This audit does not invent an additional rule that treats equivalent-looking
candidates as forced. Under the current rule, a future exact two-copy
profile would have one sequential order request (`n - 1 = 1`) if it represents
the legal permutation explicitly.

There is a separate architectural question about quotienting semantically
equivalent permutations into one equivalence class and emitting zero policy
requests. That question is **OPEN**, is not needed to establish ownership, and
is not adopted here. Adopting it would require separate authority because it
would change the meaning of the existing forced-choice rule.

Accordingly, the audit classification is not “engine-owned because the exact
pair looks equivalent.” It is a player-owned seam with an exact instance whose
current public values are equivalent at the snapshot.

## 8. `RESOLVE_FIRST` validation

### 8.1 Human mapping

For a Human semantic return:

```text
returned order [A, B]
  -> Human orderAndPlaySimultaneousSa iterates B, then A
  -> B is inserted first; A is inserted second
  -> MagicStack.add pushes each new entry at the front
  -> A is at top of stack
  -> A resolves first, then B
```

Thus the Human returned list is correctly interpreted as resolve-first order.

### 8.2 AI mapping

For the AI controller:

```text
AI returned list [A, B]
  -> AI orderAndPlaySimultaneousSa iterates A, then B
  -> A is inserted first; B is inserted second
  -> MagicStack.add pushes B to the front
  -> B is at top of stack
  -> B resolves first, then A
```

The AI callback's native list is therefore insertion order, not the same list
orientation that the Human GUI labels resolve-first. The existing external
trigger coordinator accounts for semantic/native reversal when it maps an
external resolve-first decision back to native order
(`SimultaneousTriggerOrderDecisionCoordinator.java:32-43` and `:218-250`).

The conclusion is:

```text
RESOLVE_FIRST is valid for a future copy-spell profile: YES
```

It is valid only if the future adapter preserves the controller-specific
native insertion convention and performs the same explicit reversal/mapping
discipline. No profile or adapter is implemented by this audit.

## 9. Candidate identity and minimum public projection

Because the callback is proven player-owned, a future profile must distinguish
two native copies even when their public values are equal. A safe minimum
projection is:

```text
session-local itemId
visible copied-spell source/card projection
ApiType
copied-spell marker, if needed to distinguish the admitted family
```

The `itemId` is the public candidate identity for the request. The native
`SpellAbility`/copied host mapping remains private and request-local. The
existing `SpellAbilityView` mechanism demonstrates the same separation: it
keeps a private view-to-native map at
`forge-gui/src/main/java/forge/player/SpellAbilityView.java:20-58`, while
`TrackableObject` supplies a view identity.

Two copies may therefore have identical public values and still require
separate candidate identities because a permutation must refer to two distinct
items and later target setup must be applied to the correct native copy. A
session-local item ID plus private native mapping is sufficient; no native
object identity needs to cross the public boundary.

The future projection must not expose `SpellAbility`, `SpellApiBased`, a native
Card object, Java identity, native spell-ability ID, stack description,
arbitrary `toString()` output, hidden data, or raw copied-object provenance.
The current target should not be added to the ORDER projection merely because
it is visible in diagnostics; target selection is a separate downstream
`TARGET` seam and is not required to express the order candidate.

## 10. Native AI teacher safety

For a future exact copy-spell profile, the native AI callback is:

```text
SAFE_WITH_REQUEST_LOCAL_IDENTITY
```

Evidence:

- it sees the supplied ability list and the AI player's own visible game
  state; the exact `DealDamage` branch does not require hidden opponent data;
- it does not use randomness in the inspected ordering algorithm;
- it can return non-identity orders for other API groups, so it is a real
  ordering heuristic rather than a fixed identity helper;
- it preserves the supplied item set as an order/permutation for valid input;
- it mutates/filter-rebuilds the supplied list, so the boundary must snapshot
  request-local identity before invocation and validate the returned identity
  permutation;
- it expresses the same order seam that Human exposes with `Resolve first`,
  subject to the AI native insertion-list orientation.

This classification is scoped to the future exact profile and does not
authorize reusing the current trigger-only profile for copied spells.

## 11. Canonical-count scenarios

No count is changed by this audit. The following scenarios are the supported
reconciliations of the existing raw observation.

### Scenario A — separate player-owned copy-spell profile

The raw decomposition would be:

```text
raw multi-item callbacks = 20

SIMULTANEOUS_TRIGGER_ORDER:
  sessions = 19
  requests = 26

future copy-spell ORDER profile:
  sessions = 1
  requests = 1  # current sequential n-1 encoding for the observed n=2 batch

engine-owned excluded:
  sessions = 0
```

The `1` request is a future-profile design projection, not a change to the
current L1 gate or canonical test. If a future workload contains other
copy-spell batches, their request count must be derived from their admitted
item counts rather than assumed from this one observation.

### Scenario B — engine-owned alternative

The prior alternative was:

```text
SIMULTANEOUS_TRIGGER_ORDER:
  sessions = 19
  requests = 26

engine-owned excluded:
  callbacks = 1
```

This remains a useful description of the prior L1R accounting, but the Human
`getGui().order(...)` evidence contradicts its ownership premise for the exact
callback. It is not the recommended classification.

### Scenario C — player-owned but equivalent/forced instance

The exact pair is player-owned and has two legal permutations. Under the
existing forced rule, that means:

```text
player-owned callback reachability = 1 session
policy requests under explicit permutation encoding = 1
```

An implementation that instead emits zero requests would be adopting
equivalence-class collapsing, not applying the existing “exactly one legal
candidate” rule. That remains an open architectural decision and is not
adopted in this audit. It must not be relabeled engine-owned.

## 12. Audit of the previous L1R claim

The previous document is
`docs/AI-ML DOCS/FRL_02L1R_SPELL_API_BASED_ORDER_RECONCILIATION.md`.
Its conclusions divide into supported facts and conclusions that require
correction:

| Previous L1R conclusion | Classification | Reason |
|---|---|---|
| The exact callback shape is two non-trigger, non-`WrappedAbility`, copied `SpellApiBased` `DealDamage` entries | SUPPORTED | The runtime shape is unchanged and remains outside the current trigger admission rule |
| The existing `SIMULTANEOUS_TRIGGER_ORDER` profile rejects the entries because they are not triggers/wrappers | SUPPORTED | `SimultaneousTriggerOrderDecisionCoordinator.admit` and `admissionRejection` still reject this shape |
| The copy effect constructs the copies and calls the shared controller helper | SUPPORTED | `CopySpellAbilityEffect.resolve:153-204` confirms this caller chain |
| Target setup occurs after the order callback | SUPPORTED | Both Human and AI insertion loops perform target setup after `orderSimultaneousSa` returns |
| `AiController.orderPlaySa` returns identity for this exact DealDamage pair | SUPPORTED | No inspected category filter matches `DealDamage` in this pair |
| The callback is engine-owned because `CopySpellAbilityEffect` internally constructs the list | CONTRADICTED | Human explicitly prompts non-trigger lists and uses the result for stack insertion |
| The copy effect exposes no player ordering decision | CONTRADICTED | `needPrompt = !first.isTrigger()` is true and reaches `getGui().order(...)` |
| There is no player-owned `RESOLVE_FIRST` seam for this callback | CONTRADICTED | Human labels the dialog `Resolve first` and permits non-identity order |
| The exact pair is equivalent at the observed ORDER snapshot | SUPPORTED, NEEDS QUALIFICATION | Snapshot public values are equal, but this does not establish engine ownership; later target setup and stack order remain player-owned semantics |
| The exact pair should be excluded as an engine-owned callback with zero policy requests | CONTRADICTED | Ownership is player-owned; zero-request equivalence collapsing would require separate authority |
| Current L1 trigger sessions/requests remain 19/26 and the old 20/27 gate must not be changed by this audit | SUPPORTED | The copied entries remain outside the current trigger profile; no gate change is authorized |
| No new profile should be implemented in the L1R audit | SUPPORTED | This R2 audit likewise implements no profile |

The prior audit therefore requires a correction, but the correction is
targeted: the current trigger profile and its 19/26 authority remain intact;
the exact copied-spell callback must no longer be labeled engine-owned or
`NO_POLICY_DECISION` solely on the prior premise.

## FRL-02L1R2 OWNERSHIP MATRIX

Exact runtime shape:
two non-trigger, non-`WrappedAbility`, copied Pyromatics `SpellApiBased`
`DealDamage` entries (`isSpell=true`, `isCopied=true`), both initially targeting
the same Dimir Guildmage

Human controller prompts:
YES

Human semantic label:
`Select order for simultaneous abilities` / `Resolve first`

Human can choose non-identity order:
YES

AI callback represents same semantic seam:
YES

Player-owned:
YES

Exact observed pair equivalent before target setup:
YES

Can copies diverge after ordering through target setup:
YES

Ordering can affect stack/resolution semantics:
YES

RESOLVE_FIRST valid:
YES

Existing SIMULTANEOUS_TRIGGER_ORDER profile:
DOES_NOT_APPLY

Separate player-owned ORDER profile indicated:
YES

Equivalent-candidate collapsing required:
NO (the zero-request equivalence alternative remains OPEN and requires a new
architectural decision)

Native teacher:
SAFE_WITH_REQUEST_LOCAL_IDENTITY

Correct classification:
PLAYER_OWNED_SEPARATE_PROFILE

Correct L1 trigger sessions:
19

Correct L1 trigger requests:
26

Additional v0 policy boundary:
YES

L1 production implementation change required:
NO

L1 spec amendment required:
NO

Original FRL-02L attribution correction required:
YES

L1R audit correction required:
YES

The “additional v0 policy boundary” is an authority/design finding for a
future milestone, not an implementation request in this audit. It does not
widen the current profile.

## 14. Recommendation

### Recommendation A — New exact player-owned copy-spell ORDER profile

The Human source proves genuine player ownership, so the recommended next
milestone is a separately authorized exact profile. This audit does not
implement it.

Proposed authority for a future milestone:

```text
proposed milestone ID: FRL-02L1C
proposed semantic profile: COPY_SPELL_RESOLVE_FIRST_ORDER
semantic direction: OrderDirection.RESOLVE_FIRST
minimum public projection: session-local itemId, visible copied-spell source,
  ApiType, copied-spell marker if required by admission
observed request decomposition: 1 session, 1 request for the exact n=2 batch
teacher: SAFE_WITH_REQUEST_LOCAL_IDENTITY
relationship to TARGET: separate; ORDER precedes per-copy target setup and
  must not absorb TARGET semantics
```

The profile must retain private native identity mapping, validate the full
permutation, and handle the Human/AI insertion orientation explicitly. It must
not expose native spell abilities or hidden provenance. Any decision to collapse
equivalent-looking permutations into zero policy requests requires separate
approval and is not part of this recommendation.

## 15. Scope boundary and status

This artifact is documentation-only. It does not:

- widen `SIMULTANEOUS_TRIGGER_ORDER`;
- admit `SpellApiBased` or copied non-trigger entries to L1;
- change `WrappedAbility` admission;
- alter 20/27, 19/26, or any canonical expectation;
- implement `COPY_SPELL_ORDER` or a generic ORDER provider;
- change `MagicStack`, `PlayerControllerHuman`, `PlayerControllerAi`,
  `AiController.orderPlaySa`, copy-spell target behavior, TARGET,
  CONFIRMATION, PAYMENT, Surveil ORDER, combat ORDER, or DAMAGE_ASSIGNMENT;
- edit the L1 spec, implementation plan, canonical test, ML strategy, prior
  L1R document, production code, or tests;
- mark FRL-02L1 PASS, mark PR #22 Ready, or merge PR #22.

FRL-02L1 remains `REMAINS_PARTIAL`: the existing trigger-only gate is
unchanged, while the copied-spell ownership authority now requires a separately
approved profile and correction of the prior attribution.

Original FRL-02L1R verdict: `PARTIALLY_OVERTURNED`. The prior shape,
trigger-profile exclusion, lifecycle ordering, and 19/26 preservation remain
valid; its engine-owned/no-player-choice/no-policy conclusion for this exact
callback does not.

## 16. Verification

Source/runtime work was completed through source tracing; no production
diagnostic or full-reactor run was needed for the ownership conclusion.

Verification performed after creating this artifact:

```text
git diff --check
rg -n "[ \t]+$" "docs/AI-ML DOCS/FRL_02L1R2_COPY_SPELL_ORDER_OWNERSHIP_AUDIT.md"
git status --short
git ls-files --others --exclude-standard
```

Expected/resulting scope:

```text
git diff --check = clean for tracked changes
trailing-whitespace search = no matches
untracked documentation = the pre-existing L1R audit plus this R2 audit
production changes = none
test changes = none
```

No full Maven reactor was run. No temporary diagnostics were added. The work
stops here for review.
