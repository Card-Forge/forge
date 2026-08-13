# FRL-02K-D1 - Blood Operative ETB confirmation audit

Audit date: 2026-08-13

Repository: `chrismaghuhn/forgeAI`

Audit worktree: `C:\forgeAI-blood-confirmation-d1`

Branch: `frl/02k-d1-blood-etb-confirmation`

Protected checkpoint: `c83a1e2b1209d1bfa9f671a5d3acc885133dc2cb`

Authority: [D1 design specification](../superpowers/specs/2026-08-12-frl-02k-d1-blood-etb-confirmation-design.md) and [D1 implementation plan](../superpowers/plans/2026-08-12-frl-02k-d1-blood-etb-confirmation.md).

## 1. Scope and decision boundary

This audit covers only the exact Blood Operative ETB `CONFIRMATION` slice:

```text
source:          Blood Operative
event:           ChangesZone -> Battlefield
trigger:         TrigChangeZone
effect:          ChangeZone Graveyard -> Exile
target:          exactly one existing public/projectable Card A
confirmation:    [ACCEPT, DECLINE]
cost:            free
triggeringPlayerId: null
trace label:     BLOOD_ETB_CONFIRMATION
```

It does not generalize `CONFIRMATION`, change `WrappedAbility` into a Blood-specific owner, alter card scripts, or change the existing B1 Gelectrode, C2A target, PAYMENT, Lazav, Cipher, or Flip semantics.

The exact profile is admitted only after the ownership-neutral common semantic validator in `BloodOperativeEtbProfile`. C2A and D1 then apply their own target cardinality rules: C2A requires an empty initial target set, while D1 requires exactly one captured public target A.

## 2. Ownership and fail-closed contract

The implementation preserves the two resolver seams independently.

| Target resolver | Confirmation resolver | D1/C2A ownership result |
|---|---|---|
| null | null | Forge owns target A; native Forge owns yes/no. |
| external | null | Policy owns target A; native confirmation may create temporary B; live A must be restored before terminalization. |
| null | external | Forge owns target A; policy owns ACCEPT/DECLINE; no native B is allowed. |
| external | external | Policy owns each seam once; native target and native confirmation callbacks are both zero; B is absent; ACCEPT preserves C == A. |

The `ConfirmationDecisionProvider` behavior is:

- `resolver == null`: unsupported, hidden, non-exact, stale-before-request, and copied cases create no D1 request; the existing native Forge route remains available.
- `resolver != null`: unsupported, hidden, non-exact, copied, and wrong-profile cases hard-fail; there is no Forge-AI fallback.
- An active `ActionContinuation` is rejected before resolver or native callback and never creates a D1 request.
- External resolver null, foreign, throwing, stale, or otherwise invalid results are mapped to the sanitized `INVALID_EXTERNAL_CANDIDATE` failure; native Forge is not consulted.

The public context contains only `CardSelectionCard` projections. The provider retains a private request-local raw `Card` identity guard so a projected value cannot be substituted for the exact live target object. This raw reference is not exported through the request, context, diagnostics, or trace.

For Blood, `ConfirmationDecisionContext.getTriggeringPlayerId()` is explicitly nullable and is `null`. No `AbilityKey.Activator` is required or invented for the ChangesZone event. The decider remains separately represented by `deciderPlayerId`.

## 3. Lifecycle and terminal-result integrity

The implemented order is:

```text
generate
-> capture request and diagnostics
-> choose by the captured owner
-> apply and validate live context
-> record diagnostics result
-> terminalize DECISION_TRACE_V2 result
-> continue or return
```

The provider requires D1 to have a captured choice before `apply`. It rechecks the live source projection, ChangesZone event, nullable Blood triggering player, decider, public target projection, exact raw target identity, zone, face-up/public state, and target cardinality. Full Forge legality is owned by generation-time admission; apply does not duplicate that common semantic validator.

Native confirmation calls the Forge boolean once. A temporary B may exist internally, but a successful terminal result is recorded only after the original A has been verified live. If A restoration fails after the native boolean was observed, the wrapper records `MAPPING_FAILED` with `nativeCallbackCompleted=true` and `mappingAttempted=true`, then raises the controlled native mapping exception. It never records `CHOSEN` for that state.

External confirmation calls the confirmation resolver once. It skips the native confirmation callback and therefore cannot create B. Before apply, the provider again requires live target choices to contain exactly the captured A. An integrity failure is non-native and does not use the native mapping-failure classification.

`DECLINE` returns without applying the Blood effect. `ACCEPT` continues the existing stack path. The wrapper records diagnostics before the terminal trace result, so an integrity failure cannot leave a terminal `CHOSEN` record behind.

## 4. Semantic admission and provenance

`BloodOperativeEtbProfile.validateCommonSemanticProfile` checks the exact source identity and original state, intrinsic/non-copied provenance, intrinsic non-static ChangesZone trigger, exact trigger parameters, exact static Blood effect definition, exact live ChangeZone API, free cost, Graveyard-only target zone, one target minimum/maximum, and absence of optional/targeting-player/sub-ability/additional-ability drift.

The shared helper is intentionally small and ownership-neutral. It does not decide target ownership, confirmation ownership, lifecycle continuation, or resolver policy. D1 adds exactly-one public/projectable target A; C2A retains its empty-target requirement.

Blood Surveil remains a cost-bearing/procedural callback and is not admitted as D1. The canonical audit still observes it as PAYMENT/procedural workload behavior.

## 5. Diagnostics and deterministic trace

`ConfirmationDiagnostics` now emits a typed `profile` column. The profile is carried even for recognized-but-unsupported Blood generations, so an audit can distinguish Blood ETB from Gelectrode without relying on source-name heuristics. Diagnostics and ChangesZone audit writes are guarded so an audit I/O failure cannot change the Forge callback or game loop.

Admitted D1 requests use exactly `[ACCEPT, DECLINE]`, `forced=false`, and the trace label `BLOOD_ETB_CONFIRMATION`. External confirmation results carry `nativeCallbackCompleted=false` and `mappingAttempted=false`. Native results carry `true/true` only after the native mapping path has been evaluated.

## 6. Evidence matrix

All commands below were run from `C:\forgeAI-blood-confirmation-d1` with Maven's exact Surefire selector property quoted where needed. Every listed successful build also reported zero Checkstyle violations.

| Evidence | Result |
|---|---|
| `BloodOperativeConfirmationDecisionProviderTest` | 10 tests, 0 failures, 0 errors; exact profile, native/external ownership, captured-result guard, typed unsupported profile, sanitized null/throwing/foreign resolver failures, and live-integrity invalidation. |
| `BloodConfirmationOwnershipMatrixTest` plus C2A ownership selectors | 25 tests, 0 failures, 0 errors; all four target/confirmation ownership cells, A restoration, B absence under external confirmation, trace flags, and the post-stack illegal-A fizzle before confirmation. |
| Combined D1/B1/C2A/trace/diagnostics selector | 81 tests, 0 failures, 0 errors. |
| `FRL02KConfirmationAuditTest` | 4 tests, 0 failures, 0 errors. |
| `FRL02KChangesZoneProjectionAuditTest` | 1 test, 0 failures, 0 errors; isolated canonical ChangesZone workload. |
| `FRL02KRemainingConfirmationAuditTest` | 1 test, 0 failures, 0 errors; isolated canonical reactive/proactive workloads. |
| `PriorityActionDiagnosticsTest` and retained C2A diagnostics selectors | Green in the focused verification set; no diagnostics schema regression observed. |

The combined selector initially exposed a real B1 regression when the new D1 `apply-after-choose` guard was applied unconditionally. The correction was narrowed to the Blood ETB profile, after which the focused B1+D1 selector was green with 28 tests and the full combined selector was green with 79 tests. The later profile-aware correction produced 80 tests; the explicit post-stack fizzle regression brings the current selector to 81. This is recorded as resolved evidence, not suppressed.

## 7. Canonical workload invariants

The isolated retained workload audit completed with the following exact output:

```text
reactive callback total: 41
  confirmTrigger:       26
  confirmAction:          8
  chooseBinary:           2
  payCostToPreventEffect: 5

proactive callback total: 24
  payCostToPreventEffect: 24
```

Within the 26 reactive `confirmTrigger` rows:

```text
Gelectrode SpellCast -> Untap Self: 17 admitted
Blood Operative ETB ChangesZone:     2 admitted
Blood Operative Surveil:              1 unsupported cost/procedural row
other unsupported/provenance rows:    unchanged by the D1 slice
```

The audit distinguishes the two admitted profiles through the typed diagnostics column. The existing B1 Gelectrode count remains 17; D1 adds exactly two admitted Blood ETB confirmations. Payment rows remain payment rows. Lazav clone decisions remain caller-owned. Cipher-derived play and Stitch in Time flip/binary decisions remain outside D1.

## 8. Known limitations and accepted P2

- The full common Blood semantic validator is intentionally not rerun during `apply`. This is an accepted P2 design choice: generation is the Forge legality gate, while apply revalidates the live source/event/decider and exact target A needed to protect terminal result integrity. Re-running the entire legality validator at apply would duplicate Forge legality rather than strengthen the D1 ownership boundary.
- The baseline workload command has historically exceeded the interactive command window on this machine. Such a timeout is neither PASS nor FAIL. The final isolated workload runs above completed within the test's child-JVM timeout and are the authoritative workload evidence for this branch.
- Hosted CI, remote review, and live/private historical state are not claimed by this local audit.
- Global `CONFIRMATION` remains open. This audit does not make Blood agent-complete; it closes only the exact ETB confirmation slice described above.

## 9. Final gate

P0 = 0. P1 = 0. The exact D1 implementation, focused regressions, ownership matrix, canonical workloads, diagnostics, and deterministic trace gates are complete on the isolated branch. No merge is performed by this audit.

FRL_02K_D1_PASS

STOP: Do not generalize this result to global Blood confirmation or global `CONFIRMATION` without a new semantic profile, specification, audit, and approval gate.
