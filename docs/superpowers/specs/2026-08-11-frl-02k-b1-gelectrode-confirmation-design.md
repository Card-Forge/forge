# FRL-02K-B1 Gelectrode Confirmation Design

Status: approved after B1 design review

## Scope

B1 adds the first production confirmation boundary for exactly the proven
`Gelectrode` optional `SpellCast -> Untap Self` trigger. It does not add a
generic optional-trigger predicate or adapters for Lazav, Blood Operative,
Cipher-derived triggers, costs, `confirmAction`, binary choices, payments,
replacement effects, static applications, ORDER, or DAMAGE_ASSIGNMENT.

The canonical reactive workload remains a hard gate: 17 admitted Gelectrode
occurrences out of 26 total trigger callbacks. The other 9 callbacks remain
rejected or deferred.

## Engine seam and ownership

The only production seam is `WrappedAbility.resolve`, immediately around the
existing native `decider.getController().confirmTrigger(this)` call.
`PlayerControllerAi.confirmTrigger` is not globally instrumented. Direct helper
calls, including the Contraption helper's direct `confirmTrigger(new
WrappedAbility(...))`, therefore cannot enter the B1 seam.

The existing provider pattern is reused through a narrow
`ConfirmationDecisionProvider` attached to the player controller. The provider
owns request generation and candidate application. It keeps the existing
provider-local monotonic `requestId` pattern. It may optionally receive an
explicit external/test resolver; without one, the teacher path calls the
native callback exactly once and maps its boolean result.

There is no new global ownership/mode framework. Native compatibility is the
default provider behavior. An explicitly installed resolver owns only admitted
B1 requests; unsupported profiles do not silently fall back to native in that
case and fail closed observably.

## Admission profile

The only profile is `GELECTRODE_SPELL_CAST_UNTAP_SELF`. Admission requires all
of the following semantic facts:

- canonical card/rules identity is `Gelectrode` in the original card state;
- the trigger is intrinsic, normal, non-static, and optional at the wrapper;
- `Mode=SpellCast`;
- `ValidCard=Instant,Sorcery`;
- `ValidActivatingPlayer=You`;
- `OptionalDecider=You`;
- `TriggerZones=Battlefield`;
- `Execute=TrigUntap`;
- the normalized `TrigUntap` ability is exactly `DB=Untap` and `Defined=Self`;
- there is no nonzero trigger cost;
- the source is visible to the decider; and
- no active `ActionContinuation` is present.

Description text, localization, `Trigger.getId()`, Java identity, raw
triggering objects, and opaque collections are not semantic admission inputs.
Card name alone is insufficient; the full normalized signature and trusted
provenance are required.

## Request and context

`DecisionType.CONFIRMATION` has one typed immutable context,
`ConfirmationDecisionContext`, with these fields:

- `ConfirmationTriggerProfile profile`;
- `ConfirmationEventType event` (`SPELL_CAST`);
- existing typed `CardSelectionCard sourcePublicIdentity` for the visible
  public card instance;
- `int triggeringPlayerId`; and
- `int deciderPlayerId`.

The existing `CardSelectionCard` identity convention is deliberately reused:
`cardId + gameTimestamp` is allowed only inside that typed public-card value.
It is runtime/entity correlation, not trigger semantics or candidate
semantics. No new project-wide identity scheme is introduced.

The context contains no raw `Card`, `CardLKI`, `SpellAbility`, `Trigger`,
`WrappedAbility`, `Player`, `GameEntity`, raw collection, localized
description, `requestId`, `occurrenceIndex`, runtime trigger ID, or timestamp
outside the established typed public-card identity. No agent-facing
`occurrenceIndex` is allocated. If trace internals need correlation, the
existing trace-local request index remains separate from the request context.

## Candidates and application

The provider generates exactly two candidates in stable order:

1. `ACCEPT`
2. `DECLINE`

Their semantic keys are exactly `ACCEPT` and `DECLINE`. The request is never
forced. Candidate application validates the request type, exact context/profile,
request ownership, and candidate membership. Unknown, stale, cross-request,
or cross-profile candidates fail closed and never default to acceptance.

- `ACCEPT` maps to `true`, then the existing `TrigUntap` path executes once.
- `DECLINE` maps to `false`, then resolution returns without `TrigUntap`.

Mandatory triggers produce no request. Cost-bearing and unsupported optional
triggers produce no generic confirmation request. Native compatibility outside
the explicitly installed external resolver remains unchanged.

## Trace and neutrality

Admitted requests use the existing `DECISION_TRACE_V2` REQUEST/RESULT path.
The request records `CONFIRMATION`, `[ACCEPT, DECLINE]`, and `forced=false`.
The result is one legal `CHOSEN` candidate. No raw Forge values enter the
trace.

Generation, admission, identity projection, and candidate construction must
leave the Forge state fingerprint unchanged and consume zero audited gameplay
RNG draws. The ordinary Gelectrode path must have no `ActionContinuation`.

## Observation gate

`GameEventSpellAbilityCast` and view-based stack/log surfaces exist, but the
repository has no proven ForgeRL agent observation/history contract that
correlates the concrete public cast to the later confirmation seam. The B1
gate is therefore:

`OBSERVATION_HISTORY_GAP`

B1 does not add a raw cast `Card`, `CardLKI`, or `SpellAbility` to the context.
Any future public cast-history extension is a separate architecture decision.

## Test gates

Focused tests cover both candidate paths, mandatory/cost/unsupported rejection,
Lazav/Blood Operative/Cipher exclusion, helper exclusion, hidden information,
candidate validation/order/forced state, ActionContinuation absence, state/RNG
neutrality, trace completion, native teacher mapping, and exactly-once behavior.
The controlled Izzet Guild Kit vs Dimir Guild Kit run with seed `20260810` and
10 games must prove exactly 17 admitted and 9 rejected/deferred out of 26.
