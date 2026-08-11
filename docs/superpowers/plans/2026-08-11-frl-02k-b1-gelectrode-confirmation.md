# FRL-02K-B1 Gelectrode Confirmation Implementation Plan

## Goal

Implement and verify only the production Gelectrode optional
`SpellCast -> Untap Self` confirmation slice from the approved A3 evidence.
Preserve native Forge behavior outside the admitted profile and keep global
`CONFIRMATION` open.

## Baseline and boundaries

- Worktree: `C:\forgeAI-confirmation-b1`.
- Base: exact `origin/master` merge `86894c502bf1f7b6f0c736507506b7347b83db2e`.
- Do not modify `C:\forgeAI`, `C:\forgeAI-confirmation`, or
  `C:\forgeAI-determinism-gate`.
- Do not add generic optional-trigger confirmation or any other decision family.
- Keep the observation result explicit as `OBSERVATION_HISTORY_GAP`; do not
  add raw cast-card engine objects to the context.

## Step 1: establish the failing focused tests first

Add focused tests under `forge-gui-desktop/src/test/java/forge/game/decision`
and the existing trigger fixture package. The tests must reference the narrow
provider/context seam and initially fail because B1 production types and seam
behavior do not yet exist.

Cover:

1. `DecisionType.CONFIRMATION` and the exact candidate order `[ACCEPT,
   DECLINE]`.
2. Context immutability and absence of raw Forge objects, request IDs, and
   agent-facing occurrence indices.
3. Gelectrode profile admission with the full normalized trigger signature.
4. Gelectrode ACCEPT and DECLINE through real `WrappedAbility.resolve` fixtures.
5. Mandatory trigger creates zero confirmation requests.
6. Cost-bearing optional trigger creates no generic confirmation request.
7. Lazav, Blood Operative, and Cipher-derived triggers are rejected for their
   exact profile/provenance reasons.
8. Direct helper `confirmTrigger` cannot enter the B1 resolve seam.
9. Hidden source/hidden object/hidden LKI/opaque collection fails closed without
   exporting hidden data.
10. Wrong type, unknown, stale, cross-request, and cross-profile candidates are
    rejected.
11. Requests are never forced; ordinary admitted requests have no
    `ActionContinuation`.
12. Projection and candidate generation are state-neutral and RNG-neutral.
13. `DECISION_TRACE_V2` emits one complete REQUEST and one legal CHOSEN RESULT.
14. Native teacher `true`/`false` maps exactly to ACCEPT/DECLINE with one native
    callback and no mapping failure.
15. ACCEPT resolves one untap effect; DECLINE resolves none.
16. The canonical reactive classifier remains exactly 17 admitted and 9
    rejected/deferred out of 26.

Run the smallest focused test command and capture the expected red result
before writing production implementation.

## Step 2: add the typed decision surface

Modify only the decision primitives:

- add `DecisionType.CONFIRMATION`;
- add `ConfirmationCandidateKind` with only `ACCEPT` and `DECLINE`;
- extend `LegalCandidate` with a narrow typed confirmation factory/getter and
  exact semantic keys;
- extend `DecisionRequest` with a `ConfirmationDecisionContext` slot and the
  same type/context invariants used by existing decision families;
- add `ConfirmationTriggerProfile` with only
  `GELECTRODE_SPELL_CAST_UNTAP_SELF`;
- add `ConfirmationEventType` with `SPELL_CAST`; and
- add immutable `ConfirmationDecisionContext` using the existing
  `CardSelectionCard` public identity convention.

Do not change unrelated `DecisionType` values or candidate vocabularies.

## Step 3: implement the narrow provider

Add `ConfirmationDecisionProvider` in `forge.game.decision` following the
existing provider-local request-ID pattern:

- `generate(WrappedAbility, Player)` performs strict, read-only Gelectrode
  admission and returns an explicit unsupported status without fabricating a
  request;
- `apply(DecisionRequest, LegalCandidate, WrappedAbility)` validates ownership,
  type, exact context/profile, candidate membership, and stale requests;
- an optional per-controller resolver may choose a candidate for external/test
  ownership; no global mode framework is introduced;
- the default path remains native teacher compatibility; and
- unsupported external ownership is observable and fail-closed rather than
  silently falling back to `confirmTrigger`.

Admission must compare the normalized original trigger parameters and parsed
`TrigUntap` ability map, require trusted intrinsic provenance, public source
visibility, zero cost, optional/non-static lifecycle, and no active
ActionContinuation. It must not inspect or export hidden trigger objects.

Add the smallest read-only diagnostic accessor needed to assert that ordinary
trigger resolution has no active `ActionContinuation`; do not connect ordinary
triggers to priority continuation state.

## Step 4: connect only `WrappedAbility.resolve`

At the existing decider callback:

1. Ask the controller-owned narrow provider to assess/generate.
2. For unsupported native compatibility, preserve the original native callback.
3. For an admitted request, emit one V2 REQUEST.
4. Use the optional external resolver, or invoke native `confirmTrigger` once
   as teacher, never both.
5. Map the choice to one exact candidate and apply it once.
6. Emit one V2 RESULT for that candidate.
7. Execute `playSpellAbilityNoStack(sa, false)` only for ACCEPT.

Do not alter `PlayerControllerAi.confirmTrigger`, `confirmAction`, payment,
binary, bid, replacement, static, ORDER, or DAMAGE_ASSIGNMENT paths.

## Step 5: run focused and canonical workload gates

Run:

- all FRL-01/FRL-02 focused tests;
- `FRL02KConfirmationAuditTest` and `TriggerLifeGateTest`;
- all new B1 focused tests;
- `FullGameCollectorNeutralityTest`;
- `WorkerIsolationSmokeTest`;
- the broad decision/determinism reactor suite; and
- the canonical reactive workload, fresh JVM, Izzet Guild Kit vs Dimir Guild
  Kit, seed `20260810`, 10 games.

Record raw callbacks, admitted/rejected counts, ACCEPT/DECLINE counts, helper
admissions, mandatory requests, continuation violations, and trace results.
Stop if actual admission differs from 17/26. Run the proactive Dead and Alive
vs Air Forces workload, seed `20260809`, 10 games, and record the current
trigger count without assuming it changes.

Compare native teacher behavior with tracing/provider instrumentation enabled
and disabled where practical: winner, turn count, score/result, decision
sequence, and RNG audit must remain equal for equal choices.

## Step 6: final verification and documentation

Run exact focused/broad totals, package, validate/checkstyle, and
`git diff --check`. Update `FRL_02K_CONFIRMATION_AUDIT.md` with B1 evidence,
including the observation gap and the remaining 9 blockers. Update
`ML_STRATEGY.md` only if every B1 gate passes; retain `global CONFIRMATION:
OPEN`.

## Step 7: commit and draft PR only

Create small coherent commits, push branch
`frl/02k-b1-gelectrode-confirmation`, and open the draft PR
`FRL-02K-B1: add Gelectrode confirmation decision slice`.

Do not mark ready, merge, start another confirmation profile, ORDER, or
DAMAGE_ASSIGNMENT. Stop for architecture review.
