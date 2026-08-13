# FRL-02L1R — SpellApiBased ORDER Reconciliation Audit

Status: `FRL_02L1_PARTIAL` before this audit

Audit checkpoint: `3cd5c311f7e508d4533d6951c873c7570b39f708`

Authoritative base: `9200349284b3489a6a349de378c773bdfa2f6efc`

Branch: `frl/02l1-simultaneous-trigger-order`

Scope: read-only reconciliation and attribution. This document does not add
SpellApiBased admission, change the L1 canonical gate, or change production
behavior.

## 1. Executive verdict

The one rejected multi-item `orderSimultaneousSa` invocation is not an
unadmitted member of `SIMULTANEOUS_TRIGGER_ORDER`. It is a copy-spell batch
created by the `Replicate` path of `Pyromatics` while a `CopySpellAbilityEffect`
is resolving. The two entries passed to the callback are copied
`forge.game.ability.SpellApiBased` `DealDamage` spells, not pending triggered
abilities. The enclosing Replicate trigger is a `WrappedAbility`, but the
entries in the rejected list are not wrappers and are not triggers.

The exact observed pair consists of two copies of the same spell, with the same
damage API, the same visible source card, the same activating/effective
ordering player, and the same `Dimir Guildmage` target choice in the callback
snapshot. Forge reuses the
simultaneous-ordering controller helper for this batch, but the copy effect does
not expose a player ordering decision. The native callback returns the input
order unchanged, and the two copies are observationally interchangeable in
this state.

Therefore the exact rejected invocation is `ENGINE_OWNED`, not a second
player-owned ORDER profile. The previous `20` strategic-session attribution was
too broad because it counted every multi-item invocation of the shared callback
surface without runtime entry-type attribution. The corrected current-workload
attribution is:

```text
SIMULTANEOUS_TRIGGER_ORDER: 19 sessions, 26 ORDER requests
engine-owned copy-spell callback reuse: 1 session, 0 ORDER requests
```

No L1 production change or L1 spec amendment is required by this audit. A
separate authority correction must be reviewed before changing any acceptance
counts or declaring the L1 gate green. This audit does not make that correction
itself.

## 2. Checkpoint and audit boundary

The required checkpoint was established before analysis:

```text
HEAD   = 3cd5c311f7e508d4533d6951c873c7570b39f708
branch = frl/02l1-simultaneous-trigger-order
status = clean
```

The authoritative base is:

```text
9200349284b3489a6a349de378c773bdfa2f6efc
```

The audit used the controlled canonical workload only:

```text
Izzet Guild Kit
vs
Dimir Guild Kit
10 games
seed 20260810
```

The only repository change authorized by this task is this audit document.
Temporary detail instrumentation used for the one-call attribution was removed
before the final diff. No production admission boundary, `MagicStack`, native
AI ordering implementation, canonical test, L1 spec, or implementation plan
was changed.

## 3. Exact reproduction

The focused canonical run reproduced the existing expected gate failure rather
than changing it:

```text
raw orderSimultaneousSa callbacks       116
n=1                                      96
n=2                                      14
n=3                                       5
n=4                                       1
multi-item callback invocations         20
admitted strategic sessions              19
ORDER requests                           26
candidate size 2                         19
candidate size 3                          6
candidate size 4                          1
unsupported native fallbacks             1
```

The rejected invocation is:

```text
global callback ordinal                 94
canonical game index                     9
game id                                  9
turn                                     21
phase                                    UPKEEP
choosing player                          Ai(1)-Izzet Guild Kit
active list size                         2
```

`Game` IDs start at one for the ten-game `SimulateMatch` loop, so runtime
`gameId=9` is canonical Game 9. The first detail block was captured with the
diagnostics-only recorder; the second control block came from the deterministic
control run and did not alter the conclusion.

### 3.1 Runtime entry classification

The following is an engine-internal audit record. Native spell-ability IDs and
card IDs are private evidence only and must not be exposed in an observation or
`DecisionRequest`.

| entry | runtime class | wrapper | trigger | spell | copied | API | host/source | copied original | target | players |
|---|---|---:|---:|---:|---:|---|---|---|---|---|
| 0 | `forge.game.ability.SpellApiBased` | no | no | yes | yes | `DealDamage` | `Pyromatics` (host id 123; native SA id 25810) | `Pyromatics` (copied permanent id 31) | `Dimir Guildmage` (card id 116) | activating, effective ordering, and host controller all `Ai(1)-Izzet Guild Kit` |
| 1 | `forge.game.ability.SpellApiBased` | no | no | yes | yes | `DealDamage` | `Pyromatics` (host id 124; native SA id 25815) | `Pyromatics` (copied permanent id 31) | `Dimir Guildmage` (card id 116) | activating, effective ordering, and host controller all `Ai(1)-Izzet Guild Kit` |

Additional runtime facts:

```text
trigger field                         null for both entries
original ability class/API/host      null for both entries
sourceVisibleToChooser               true for both entries
native result order                   same as input order
```

The `Dimir Guildmage` target and native IDs are included to establish the exact
engine state. They are not proposed public identity fields. The source card is
visible to the chooser, but visibility alone does not turn this reused helper
call into a player-owned ordering decision.

## 4. Complete caller-chain trace

The exact path is:

```text
Pyromatics
  -> Replicate keyword generated by CardFactoryUtil
  -> SpellCast trigger with CopySpellAbility overriding ability
  -> WrappedAbility.resolve
  -> CopySpellAbilityEffect.resolve
  -> CardFactory.copySpellAbilityAndPossiblyHost
       twice, producing copied SpellApiBased DealDamage spells
  -> controller.getController().orderAndPlaySimultaneousSa(copies)
  -> PlayerControllerAi.orderSimultaneousSa
  -> SimultaneousTriggerOrderDecisionCoordinator.order
       rejects NON_WRAPPED_ENTRY
  -> resolver-null native fallback
  -> AiController.orderPlaySa
  -> PlayerControllerAi.orderAndPlaySimultaneousSa
  -> copied-spell target preparation, if requested
  -> MagicStack.add for each returned copy
  -> MagicStack.push / stack.addFirst
  -> MagicStack.resolveStack / AbilityUtils.resolve
```

The relevant source seams are:

```text
forge-game/src/main/java/forge/game/card/CardFactoryUtil.java
  dynamic Replicate trigger and CopySpellAbility definition

forge-game/src/main/java/forge/game/ability/effects/CopySpellAbilityEffect.java
  resolve(...), copies construction, orderAndPlaySimultaneousSa(copies)

forge-game/src/main/java/forge/game/card/CardFactory.java
  copySpellAbilityAndPossiblyHost(...)
  copySpellHost(...)

forge-ai/src/main/java/forge/ai/PlayerControllerAi.java
  orderSimultaneousSa(...)
  orderAndPlaySimultaneousSa(...)

forge-ai/src/main/java/forge/ai/AiController.java
  orderPlaySa(...)

forge-game/src/main/java/forge/game/zone/MagicStack.java
  push(...), resolveStack(...)
```

### 4.1 Answers to the caller-chain questions

1. `CopySpellAbilityEffect.resolve()` calls
   `orderAndPlaySimultaneousSa(copies)` for the newly created copy list.

2. `SpellApiBased` is present because the copied object is the copied
   `SpellAbility` for `Pyromatics`'s `DealDamage` spell API. The copy factory
   marks it copied and creates a copied spell host with
   `GamePieceType.COPIED_SPELL`.

3. This is not the ordinary simultaneous-trigger engine path. Ordinary pending
   triggers are collected by `MagicStack.addAllTriggeredAbilitiesToStack()` and
   grouped by `chooseOrderOfSimultaneousStackEntry()` using the active player
   and the activating-player/host-controller fallback. The rejected call never
   enters that APNAP pending-trigger collection. It merely reuses
   `orderAndPlaySimultaneousSa()` and, transitively, the same controller
   callback.

4. The Forge semantic is copy-spell resolution for `Replicate`: the Replicate
   machinery creates one copy for each paid replicate amount and permits new
   target preparation through the copy effect. This is a copy-spell stack
   insertion batch, not a batch of simultaneous triggered abilities.

5. In the reproduced pair, changing the order does not change the game state
   transition or subsequent ORDER choices. Both entries are one `DealDamage`
   effect from the same visible `Pyromatics` source, both deal one damage, and
   both carry the same `Dimir Guildmage` target choice in the callback
   snapshot. The later `MayChooseTarget` preparation is a separate target
   seam, and the two entries are observationally interchangeable for this
   exact state.

6. The player's controller is the same player that cast the source spell, but
   no player ordering choice is requested by `CopySpellAbilityEffect`. The
   callback is an implementation-reuse seam, not evidence of strategic
   ownership. The exact call is engine-owned/forced.

## 5. Semantic ownership

### 5.1 Existing `SIMULTANEOUS_TRIGGER_ORDER` comparison

The existing L1 profile requires all of the following:

```text
pending simultaneous triggered abilities
TriggerType meaningful for every item
same-player MagicStack/APNAP grouping
player-owned resolve-first ordering choice
safe public projection of each triggered entry
```

The rejected entries fail the first two conditions:

```text
isTrigger()     = false for both entries
TriggerType     = null for both entries
```

The enclosing Replicate trigger is a `WrappedAbility`, but that wrapper is the
caller being resolved. It is not either item in the list passed to
`orderSimultaneousSa`. Treating the enclosing wrapper as proof that the list
contains triggers would conflate caller provenance with candidate semantics.

The exact call therefore does not belong to
`SIMULTANEOUS_TRIGGER_ORDER`. In particular, the existing `TriggerType` field
cannot be populated truthfully for these entries.

### 5.2 Distinct profile assessment

No distinct player-owned ORDER profile is established by this evidence. The
observed shape is a homogeneous copy batch whose two items are equivalent in
the reproduced state, and `CopySpellAbilityEffect` does not ask the player to
order them. A name such as `COPY_SPELL_ORDER` must not be adopted from the Java
type or callback name alone.

This does not claim that every future copy-spell batch is always engine-owned.
A future batch containing semantically different copies, distinct targets, or a
source effect that explicitly delegates ordering would require a separate
source-and-runtime audit. That future question is outside this reconciliation.

### 5.3 Engine ownership

The exact call is engine-owned because:

* the list is constructed internally by `CopySpellAbilityEffect`;
* the effect loops over the selected source spell and creates the copies before
  calling the shared ordering helper;
* there is no ordering request, candidate-domain construction, or public choice
  at this seam;
* the two observed entries are semantically interchangeable; and
* the callback is reused for execution convenience rather than reached through
  the normal pending-trigger/APNAP path.

The native fallback is consequently the correct behavior for this call under
the current fail-closed L1 boundary. Widening admission would publish an
engine-internal execution batch as an agent decision.

## 6. LIFO and resolution-order proof

The exact native snapshot and native result were:

```text
input/result [0] = copied Pyromatics host id 123, native SA id 25810
input/result [1] = copied Pyromatics host id 124, native SA id 25815
```

`AiController.orderPlaySa()` returns the same order here because both entries
have `ApiType.DealDamage`, which is not one of its native category filters. The
controller then iterates the returned list in order. `MagicStack.push()` calls
`stack.addFirst(si)`, and `MagicStack.resolveStack()` resolves `peekAbility()`.

The resulting stack behavior is therefore:

```text
controller adds copy 123 first
  -> copy 123 is below the next entry
controller adds copy 124 second
  -> copy 124 is on top

resolution order: copy 124, then copy 123
```

This proves the mechanical insertion-to-resolution reversal for this path. It
does not prove a player-owned `RESOLVE_FIRST` decision. The observed result is
the native order used by an engine-owned copy batch, not a selected semantic
candidate.

Target preparation is also a separate seam. In
`PlayerControllerAi.orderAndPlaySimultaneousSa()`, `sa.setupTargets()` runs
after `orderSimultaneousSa()` returns and before each copied spell is inserted
on the stack. `SpellAbility.setupTargets()` delegates to the activating
player's `chooseTargetsFor(...)`. That target decision is not an ORDER
candidate and must not be folded into the rejected-session attribution.

## 7. Native teacher behavior

For this exact invocation, `AiController.orderPlaySa()` behaves as follows:

* the list has two entries, so the multi-item path is entered;
* the method filters for `Discard`, `Draw`, `PutCounter`, `PutCounterAll`,
  `evolve`, `Token`, `Pump`, and `PumpAll` categories;
* neither `DealDamage` entry matches any category;
* no candidate is removed from the input list;
* the returned result preserves the input order; and
* no random operation occurs in this path.

The method does inspect the controlled AI player's own hand for a mandatory
discard condition, but that condition does not alter this `DealDamage` pair.
It does not use hidden opponent information for this callback. The output is
deterministic for the fixed state/RNG context observed, and a private
request-local native identity could map the returned objects if this were a
real policy decision.

That last mapping property is not sufficient to make this a teacher label. The
native callback is being used as an execution-order helper for an engine-owned
batch. The correct teacher classification for this exact case is:

```text
NO_POLICY_DECISION
```

In the decision matrix below this is recorded as `NOT_APPLICABLE`.

## 8. Public projection and information boundary

The existing L1 public projection cannot represent this call truthfully:

```text
source       = visible Pyromatics
TriggerType  = null
effectApi    = DealDamage
```

`TriggerType` is not semantically available. The copied-spell status is a
typed engine fact that could be considered in a future, separately approved
profile, but it does not repair the missing trigger semantics.

The two entries also have identical proposed value fields if the current
trigger-only projection is extended informally:

```text
source       = Pyromatics
trigger      = null
effectApi    = DealDamage
copied       = true       (internal observation only)
```

The private native identity and a session-local item ID could preserve the
distinction between the two copies. They must not be used to manufacture a
decision where the engine has not exposed one. If a future source audit proves
a distinct copy-spell profile, the minimum candidate projection should be
re-derived from that profile; a possible starting point would be a
session-local item ID plus visible source, `ApiType`, and an explicitly
justified copied-spell marker. Native objects, Java identity, stack text, raw
descriptions, and copied-object provenance must remain private unless a later
profile proves a public semantic need.

The internal target `Dimir Guildmage` is included in the audit only to prove
the symmetry of this exact pair. It is not a proposed item-identity field. No
hidden opponent information is proposed for publication.

## 9. Canonical-count reconciliation

The raw callback count remains unchanged:

```text
orderSimultaneousSa total = 116
singletons            = 96
multi-item callbacks  = 20
```

The semantic decomposition supported by the exact rejected-session trace is:

| classification | sessions | n=2 | n=3 | n=4 | L1 ORDER requests |
|---|---:|---:|---:|---:|---:|
| `SIMULTANEOUS_TRIGGER_ORDER` | 19 | 13 | 5 | 1 | 26 |
| engine-owned copy-spell callback reuse | 1 | 1 | 0 | 0 | 0 |
| raw multi-item callback total | 20 | 14 | 5 | 1 | 26 L1 requests plus one excluded callback |

The L1 request arithmetic is:

```text
13 x (2 - 1) = 13
 5 x (3 - 1) = 10
 1 x (4 - 1) =  3
----------------
               26
```

The L1 candidate-size distribution is:

```text
candidate size 2: 13 + 5 + 1 = 19
candidate size 3:  5 + 1     =  6
candidate size 4:  1         =  1
```

The rejected copy-spell callback is not an additional ORDER profile in this
audit, so the `other-profile` count is zero. It is one engine-owned callback
invocation with zero L1 requests. The old `20/27` gate must remain unchanged
until the authority correction is explicitly reviewed; this document does not
edit the gate or claim `FRL_02L1_PASS`.

## 10. Correction to the earlier FRL-02L attribution

The earlier [FRL-02L attribution audit](FRL_02L_ORDER_ATTRIBUTION_AUDIT.md)
correctly established that the normal MagicStack route can expose a genuine
same-player simultaneous-trigger ordering decision, and it correctly separated
Surveil ordering, combat ordering, and damage assignment. Its gap was narrower:

* the aggregate `orderSimultaneousSa` surface was classified as strategic for
  all `n >= 2`;
* the canonical report described 20 multi-item calls as strategic sessions;
* the previous opt-in recorder intentionally wrote only sanitized
  request-local indices and did not record runtime classes, card names, or
  copied-spell provenance; and
* no runtime attribution proved that all 20 multi-item lists contained pending
  trigger entries.

The same broad inference was repeated in the FRL-02L section of
`ML_STRATEGY.md`, which described 116 callbacks including 20 with two or more
entries. The evidence proved callback reachability and permutation-shaped
output, but not semantic homogeneity of every caller of the shared callback.

The correction is therefore an attribution correction, not a production
boundary correction:

```text
orderSimultaneousSa multi-item callback
!= automatically SIMULTANEOUS_TRIGGER_ORDER
```

The one proven exception is the Replicate copy-spell path documented here. The
existing L1 admission boundary was right to reject it.

## 11. FRL-02L1R reconciliation matrix

```text
FRL-02L1R RECONCILIATION

Rejected runtime shape:
2 x forge.game.ability.SpellApiBased copied Pyromatics DealDamage spells;
isTrigger=false, isWrapper=false, isSpell=true, isCopied=true, TriggerType=null.

Exact caller path:
Pyromatics Replicate generated trigger -> WrappedAbility.resolve ->
CopySpellAbilityEffect.resolve -> copySpellAbilityAndPossiblyHost (twice) ->
orderAndPlaySimultaneousSa(copies) -> PlayerControllerAi coordinator/native
ordering -> stack insertion.

Exact card/effect:
Pyromatics Replicate; two copied one-damage DealDamage effects targeting the
same Dimir Guildmage.

Player-owned strategic choice: NO

Same semantic as SIMULTANEOUS_TRIGGER_ORDER: NO

RESOLVE_FIRST semantic valid: NO as an agent-facing policy decision;
the mechanical LIFO reversal is present but is engine execution behavior.

Existing public projection sufficient: NO; TriggerType is null and there is no
player-owned decision to project.

TriggerType semantically valid: NO

Safe minimal alternative projection exists: NOT NEEDED

Native teacher: NOT_APPLICABLE (NO_POLICY_DECISION)

Correct classification: ENGINE_OWNED

Correct L1 canonical sessions: 19

Correct L1 canonical requests: 26

Additional v0 ORDER profile required: NO for this observed call

FRL-02L1 implementation change required: NO

FRL-02L1 spec amendment required: NO

FRL-02L1 audit correction required: YES
```

## 12. Recommendation

### Recommendation C — Exclude as engine-owned

Exclude this exact Replicate copy-spell invocation from
`SIMULTANEOUS_TRIGGER_ORDER`. Do not add `SpellApiBased` to the existing
admission check and do not create a generic copy-spell ORDER adapter.

The next approved step should be a small authority-only follow-up, for example
`FRL-02L1R-AUTHORITY-CORRECTION`, which would:

1. record this runtime attribution beside the original FRL-02L audit;
2. change the authoritative L1-local expectation from 20 sessions / 27
   requests to 19 sessions / 26 requests only after explicit review;
3. preserve a separate statement that the raw callback surface had 20
   multi-item invocations; and
4. rerun the focused canonical gate after the authority change.

That follow-up is not performed here. The existing test and gate remain as-is,
and the implementation remains fail-closed for the rejected entry.

## 13. Explicit non-goals

This audit does not:

* widen admission beyond `WrappedAbility`;
* add `SpellApiBased` support;
* change `MagicStack` or `AiController.orderPlaySa`;
* change expected canonical counts or acceptance tests;
* implement a `COPY_SPELL_ORDER` profile;
* implement Surveil ORDER, combat ORDER, or `DAMAGE_ASSIGNMENT`;
* alter TARGET, CONFIRMATION, or PAYMENT;
* change the L1 spec or implementation plan;
* expose hidden information, native objects, descriptions, or Java identity;
* mark `FRL-02L1` PASS;
* mark PR #22 Ready, merge PR #22, or otherwise change its Draft state.

## 14. Evidence and verification

Evidence used:

```text
checkpoint status/diff/diff-check/rev-parse/branch verification: PASS
exact canonical reproduction: 116 callbacks, 20 multi-item, 19 admitted
detail trace: callback ordinal 94, Game 9, two SpellApiBased copies
caller source trace: CopySpellAbilityEffect -> controller helper
copy source trace: CardFactory copy host and setCopied behavior
stack source trace: MagicStack.addFirst -> resolveStack.peekAbility
target source trace: setupTargets runs after the ORDER callback
native source trace: DealDamage entries bypass all orderPlaySa filters
```

The focused Maven run was:

```text
mvn -pl forge-gui-desktop -am '-Dtest=FRL02L1SimultaneousTriggerOrderAuditTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

It failed only at the still-authoritative old canonical expectation (`expected
[20] but found [19]`). The deterministic control run completed and produced no
hash mismatch. No 15-minute full-reactor rerun was needed.

Temporary detail instrumentation was disabled by default, used only to capture
the exact internal object attribution, and removed before this final
documentation-only diff. The final repository change is this audit document.

## 15. Final audit status

```text
FRL-02L1R = COMPLETE
classification = ENGINE_OWNED
recommendation = C
FRL_02L1_PASS = not claimed
FRL_02L1 status = PASS_ELIGIBLE_AFTER_AUTHORITY_CORRECTION
production changes = none
```
