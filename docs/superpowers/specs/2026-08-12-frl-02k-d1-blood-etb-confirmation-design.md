# FRL-02K-D1 Blood Operative ETB CONFIRMATION Design Checkpoint

Status: DESIGN_APPROVED

## Checkpoint

- Repository: `chrismaghuhn/forgeAI`
- Base: `c83a1e2b1209d1bfa9f671a5d3acc885133dc2cb`
- Worktree: `C:\forgeAI-blood-confirmation-d1`
- Branch: `frl/02k-d1-blood-etb-confirmation`
- Pre-D audit: `FRL_02K_D_PASS`

The protected `C:\forgeAI` checkout and the existing C2A and D worktrees are
out of scope. The initial fresh-worktree focused baseline timed out before a
test summary and is therefore unverified; it is not treated as a pass or a
failure of D1.

## 1. Objective and non-goals

D1 adds exactly one explicit confirmation profile for the already-targeted
Blood Operative ETB effect:

```text
Blood Operative ETB
  -> C2A selects Target A
  -> D1 asks [ACCEPT, DECLINE]
  -> ACCEPT resolves the existing ChangeZone effect with A
  -> DECLINE skips the effect
```

D1 does not add generic CONFIRMATION, a second TARGET decision, Blood Surveil
`PayLife<3>` support, a global callback adapter, new card-script behavior,
Lazav/Cipher/Flip/Bid/Replacement/Static-Application support, ORDER,
DAMAGE_ASSIGNMENT, ObservationEncoder, HistoryEvent, BC redesign, or RL.

## 2. Selected architecture

### 2.1 Provider boundary

Extend the controller-local `ConfirmationDecisionProvider` with a second
strictly admitted profile. Keep Gelectrode B1 behavior source-compatible and
semantically unchanged. The provider remains the only confirmation request
owner; no Blood-specific provider and no generic optional-trigger predicate are
introduced.

The profiles are:

```text
GELECTRODE_SPELL_CAST_UNTAP_SELF
BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD
```

Add `ConfirmationEventType.CHANGES_ZONE` for Blood. The trace stage is derived
from the typed profile (`GELECTRODE_CONFIRMATION` or
`BLOOD_ETB_CONFIRMATION`); `WrappedAbility.resolve` does not inspect card
names or Blood semantics.

### 2.2 Common semantic basis

If source inspection confirms the extraction remains small, add a stateless
`BloodOperativeEtbProfile` helper containing only common semantic checks and
normalization:

```text
Blood source/provenance and visibility
intrinsic, non-static ChangesZone trigger
Origin=Any, Destination=Battlefield
ValidCard=Card.Self, OptionalDecider=You
Execute=TrigChangeZone, no SpawningAbility
ChangeZone Graveyard -> Exile, ValidTgts=Card
one-card non-random free live effect
no Optional, TargetingPlayer, subability, or additional ability
chooser/decider/activator/source-controller agreement
```

The helper owns no target enumeration, target application, resolver calls,
trace state, stack transitions, AI calls, or ownership policy. If extracting
it would enlarge or destabilize C2A, retain narrow duplicated checks and record
that tradeoff in the audit.

C2A and D1 retain different live-target gates:

```text
C2A: TargetChoices is empty before Target A generation.
D1:  TargetChoices contains exactly one live Card, the stored Target A.
```

### 2.3 WrappedAbility orchestration

`WrappedAbility.resolve` remains a generic lifecycle adapter:

```text
generate
  -> capture/request-trace
  -> choose
  -> apply + live-context integrity validation
  -> diagnostics result
  -> terminal DECISION_TRACE_V2 result
  -> continue/return
```

It may select the trace stage from the typed request context, but it may not
contain Blood card, zone, or profile predicates. `PlayerControllerAi` remains
unchanged except for a narrowly justified observation seam, if one is proven
necessary by the real production-route test.

## 3. Public Target-A context and integrity

Extend `ConfirmationDecisionContext` with a nullable immutable
`CardSelectionCard targetPublicIdentity` and a nullable event-player value.

```text
Gelectrode: targetPublicIdentity == null
Blood:      targetPublicIdentity == public projection of live Target A
```

The Blood request also exposes the existing profile, `CHANGES_ZONE` event,
public source identity, optional event-player ID, and decider-player ID. The
projection is created only when exactly one live target exists, it is a Card,
it is public to the decider, and its value can be safely projected. Its public
identity is the established `(cardId, gameTimestamp)` value inside
`CardSelectionCard`; no raw `Card`, `CardLKI`, `TargetChoices`, `SpellAbility`,
`Game`, `Zone`, or JVM identity is exported.

The existing B1 `triggeringPlayerId` meaning is retained for Gelectrode: it is
the public `AbilityKey.Activator` player from the SpellCast event and remains
non-null. Blood does not inherit that B1 assumption. The current
`TriggerChangesZone.setTriggeringObjects` runtime projection copies
`AbilityKey.Card`/`CardLKI`, not `AbilityKey.Activator`, and no runtime-proven
Blood event player is required by D1. Therefore the context carries
`triggeringPlayerId == null` for Blood; it must not use a sentinel or duplicate
the decider ID. The nullable value is event-specific and is not part of Blood
admission.

The request-local A/B/C invariant is:

```text
A = C2A's authoritative live target before D1
B = native AI-only temporary evaluation, never an agent candidate
C = target consumed by the existing ChangeZone effect
```

Successful terminalization requires the projected A value to still match
exactly one live target by runtime identity/correlation and public value. D1
does not re-run target legality or choose a replacement.

## 4. Ownership and fail-closed matrix

The resolver determines ownership only after exact admission:

| Condition | `resolver == null` | `resolver != null` |
|---|---|---|
| Exact Blood profile, valid A | One D1 request; native `confirmTrigger` once; validate restored A | One D1 request; resolver once; native callback and B zero |
| Unsupported/hidden/non-exact/copy/stale-before-request | No D1 request; preserve existing native Forge path | Sanitized hard failure before resolver/native fallback |
| Active `ActionContinuation` | Never create a D1 request; preserve existing native boundary behavior | Reject before resolver or native callback |
| Native A restoration/integrity failure | No second callback; `MAPPING_FAILED` with native/mapping `true/true`; no `CHOSEN` | Not applicable |
| External null/foreign/stale/wrong-kind/throwing resolver | Not applicable | Fail closed; no native fallback and no `MAPPING_FAILED` classification |

For external ownership, the live A identity is rechecked after resolver
selection and before applying the result. For native ownership, the Boolean is
mapped exactly once, but `recordNativeMappedResult(ACCEPT/DECLINE)` is delayed
until the callback has returned and A has been restored and verified. An
integrity failure never triggers a second native callback. A native Boolean
that cannot be safely associated with the original A is a native request/result
mapping failure: it records `MAPPING_FAILED` with `nativeCallbackCompleted=true`
and `mappingAttempted=true`, never a `CHOSEN` or an external `false/false`
result. External resolver/application failures remain non-native and may
finalize as `TRACE_INCOMPLETE` under the existing V2 contract.

### 4.1 Independent TARGET/CONFIRMATION ownership matrix

C2A Target A ownership and D1 confirmation ownership are independent
controller-local seams. The production route must preserve all four
combinations:

| TARGET resolver | CONFIRMATION resolver | Required ownership and observations |
|---|---|---|
| `null` | `null` | Forge AI owns A; native confirmation owns yes/no; target and native confirmation callbacks each occur once on the supported path. |
| `external` | `null` | External policy owns A once; native confirmation owns yes/no; temporary B may exist during native confirmation but A must be restored and consumed. |
| `null` | `external` | Forge AI owns A once; external policy owns ACCEPT/DECLINE once; native confirmation and temporary B are absent. |
| `external` | `external` | External policy owns A once and ACCEPT/DECLINE once; native target callback is zero; native confirmation callback is zero; B is absent; ACCEPT consumes C==A. |

No combination may create two Target-A decisions, a second TARGET request, or a
confirmation-time retarget.

## 5. Candidates, resolution, and traces

Every admitted Blood request contains exactly:

```text
DecisionType.CONFIRMATION
forced = false
candidates = [ACCEPT, DECLINE]
ActionContinuation = null
decisionSequenceId = null
subdecisionIndex = null
```

`ACCEPT` lets Forge run the existing `playSpellAbilityNoStack` path. `DECLINE`
returns before effect resolution. No second TARGET request, target application,
retarget, or native target selection is allowed.

Use `DECISION_TRACE_V2` without schema changes:

```text
native teacher:  CHOSEN, nativeCallbackCompleted=true,  mappingAttempted=true
external owner:  CHOSEN, nativeCallbackCompleted=false, mappingAttempted=false
```

The existing BC rule remains unchanged: only valid non-forced native
`true/true` history is eligible. Invalid external runs may remain
`TRACE_INCOMPLETE`; they must not be reclassified as native mapping failures.

`UnsupportedConfirmationDecisionException` keeps its sanitized status/reason
API, but its message is changed from the stale B1-only wording to a
profile-neutral confirmation unsupported/integrity message. No card name,
target name, resolver exception text, or raw object string is included.

`ConfirmationDiagnostics` is generalized in wording from B1-only to supported
confirmation profiles and gains a sanitized `profile` field containing the
typed enum name. Any other fields remain opt-in, value-only, sanitized,
worker-local, state-neutral, and RNG-neutral; diagnostics do not participate
in correctness.

## 6. Test matrix

Add focused production tests for:

1. Common/profile admission: exact Blood shape, every specified trigger/live
   mismatch, provenance/copy/clone/visibility/chooser/cost/ability rejection,
   and Blood Surveil PAYMENT exclusion.
2. Context safety: exactly one public graveyard A, immutable projection,
   identity/zone match, no raw engine values, hidden/untrusted/zero/multiple
   target fail-closed behavior, and null continuation metadata.
3. Native ownership: one request, one native callback, exact Boolean mapping,
   A-B-A restoration, post-callback A integrity, native `true/true` trace,
   and controlled integrity failures without retry.
4. External ownership: ACCEPT and DECLINE, exactly one resolver, zero native
   callback/B evaluation, A preservation, C==A on ACCEPT, no effect on
   DECLINE, no second TARGET request, and external `false/false` trace.
5. Adversarial resolver: null, foreign, stale, wrong confirmation kind, and
   throwing results; all sanitized, fail-closed, no Forge-AI fallback, and no
   `MAPPING_FAILED` classification.
6. Fizzle and production route: illegal A fizzles before confirmation with no
   resolver call/retarget, plus the real
   `PlayerControllerAi.orderAndPlaySimultaneousSa` -> C2A -> stack -> D1 ->
   ChangeZone external route.
7. Retained gates: B1 Gelectrode, C2A target ownership/provider/external
   ownership, trace validator, diagnostics, canonical workload locks, and
   worker/provider isolation.

The canonical native workload remains Izzet Guild Kit versus Dimir Guild Kit,
10 games, seed `20260810`. The external workload is Blood-focused only and
does not claim Blood agent completeness.

## 7. File map

Expected production files:

```text
forge-game/src/main/java/forge/game/decision/ConfirmationDecisionProvider.java
forge-game/src/main/java/forge/game/decision/ConfirmationDecisionContext.java
forge-game/src/main/java/forge/game/decision/ConfirmationTriggerProfile.java
forge-game/src/main/java/forge/game/decision/ConfirmationEventType.java
forge-game/src/main/java/forge/game/trigger/WrappedAbility.java
forge-game/src/main/java/forge/game/decision/ConfirmationDiagnostics.java
forge-game/src/main/java/forge/game/decision/UnsupportedConfirmationDecisionException.java
possibly forge-game/src/main/java/forge/game/decision/BloodOperativeEtbProfile.java
```

Expected focused tests/docs:

```text
forge-gui-desktop/src/test/java/forge/game/decision/
    BloodOperativeConfirmationDecisionProviderTest.java
forge-gui-desktop/src/test/java/forge/view/
    FRL02KD1BloodConfirmationExternalOwnershipAuditTest.java
docs/AI-ML DOCS/FRL_02K_D1_BLOOD_CONFIRMATION_AUDIT.md
docs/AI-ML DOCS/FRL_02K_CONFIRMATION_AUDIT.md
docs/AI-ML DOCS/ML_STRATEGY.md
```

No `PlayerControllerAi` policy logic, card script, ObservationEncoder,
HistoryEvent, unrelated decision provider, or global callback adapter is in
scope.

`DecisionTraceTrainingValidator` is a retained regression authority, not an
expected production change: its current V2 rules already accept external
`CONFIRMATION` `CHOSEN` history with `false/false` while keeping BC eligibility
strictly native `true/true`.

## 8. Self-review

```text
Review resolutions:
P1-1 trace terminalization follows apply/integrity validation: FIXED
P1-2 Blood has no invented triggering player: FIXED
P1-3 explicit 2x2 ownership matrix: FIXED
P2-1 native integrity failure is V2 MAPPING_FAILED true/true: FIXED
P2-2 profile-neutral sanitized exception message is mandatory: FIXED
P2-3 typed ConfirmationDiagnostics.profile field is mandatory: FIXED

P0: 0
P1: 0
P2: 0 design defects
```

The only open verification note is the fresh-worktree baseline timeout; it is
an evidence gap to be resolved by the implementation test gates, not an
unresolved architecture issue. The design has no generic CONFIRMATION escape
route, no ownership logic in the shared semantic helper, no Target-A
reselection path, and no trace/schema expansion.
