# FRL-02D — X_VALUE Announcement and Neutral Decision Boundary

**Base:** `master @ 115762217f84e13041cc4ed315dc58e8b95abae8`

**Branch:** `frl/02d-x-value-boundary`

**Scope:** neutral player-announced mana-X decisions only; no reinforcement learning, MODE, new PAYMENT capability, or AI X policy.

## Result

FRL-02D establishes `DecisionType.X_VALUE` and a completion-safe, deterministic candidate list for the fixed-output
mana slice. Forge remains authoritative for announcement bounds, cost adjustment, target legality, payment legality,
and applying the chosen X. An understood state with no legal X is `INVALID_X`. An X request exists exactly when Forge
reaches the player X announcement boundary; a pre-existing `XManaCostPaid` value does not suppress that callback or
the neutral request. Derived X and copied/wrapper abilities that Forge never announces produce no neutral request.

## Forge ordering and Invoke the Firemind

`PlaySpellAbility` performs optional/additional-cost preparation and then preserves Forge's real ordering. Explicit
Charm `Announce$ X` can occur before mode choice. Ordinary generic mana X follows `announceType`, then
`announceValuesLikeX`, restrictions, targets, and payment.

`Invoke the Firemind` has no explicit `Announce$ X`. Its script is a Charm with Draw and Damage choices, therefore its
live order is:

```text
priority action -> MODE -> X -> optional TARGET -> CostAdjustment -> ManaCostBeingPaid -> PAYMENT
```

The selected X is applied only with `SpellAbility.setXManaCostPaid`. Specific-X adjustment uses
`CostAdjustment.preview(cost, root, payer, effect, X, root.getXColor())`; Forge then carries that X into
`ManaCostBeingPaid`.

## Candidate-domain proof

The provider begins with `AbilityUtils.getAnnouncementBounds(ability, "X")`. It never enumerates to
`Integer.MAX_VALUE` and has no arbitrary cap.

For one supported mana cost containing `k` X shards, it computes:

```text
S = all potentially available fixed-output mana units plus floating mana
R = conservative maximum generic reduction allowance across the supported X domain
upper = min(rawMax, floor((S + R) / k))
```

`S` is cost-independent. It includes off-color sources that cannot advance the colored base cost at X=0 but can pay
generic X later. Alternative mana activations, production replacements, variable output, any mana ability carrying
an `Amount` parameter, nontrivial mana sub-abilities, and complex source costs make the inventory incomplete and therefore return
`UNSUPPORTED_FINITE_DOMAIN`.

`Amount` is a separate Forge production-multiplicity input and is not encoded by `AbilityManaPart.getOrigProduced()`.
The v0 shadow inventory therefore rejects every playable `Amount`-bearing mana ability as
`DYNAMIC_MANA_PRODUCTION` before constructing static bundles. This applies to both capacity and specific-X payment;
it prevents sources such as a multi-counter Everflowing Chalice from being silently modeled as one mana.

`R` scans the public cost-adjustment authority for every potentially relevant fixed generic reducer, not merely the
reduction applied at `rawMin`. It overestimates safely by including currently inactive fixed reducers. Any reducer
whose domain-wide maximum is not provable with the fixed model publishes no allowance and makes the X domain
unsupported. Addition saturates rather than overflowing.

Every integer in the proven finite interval is then independently filtered through side-effect-free
`PriorityCostFeasibility.assessPaymentAtX`. No monotonicity shortcut is used.

## Purity and completion safety

Specific-X feasibility uses the root action even when called with an `AbilitySub` whose cost is `Cost.Zero`. It never
sets X or an activating player on the live root, never sets an activating player on live mana abilities, never
activates sources, and never writes the mana pool. Unprepared abilities and mana abilities are copied for probing.

The pure target preflight neither calls `generateTargetRequest` nor allocates request IDs/subdecision indices,
re-previews cost, or mutates `TargetChoices`. It runs only when the future target chooser is already deterministic.
Unresolved `TargetingPlayer`, X-dependent target completion, coupled target rules without a pure completion oracle,
random targeting, and divided allocation are unsupported. An understood impossible mandatory target yields
`INVALID_X`.

The announcement chooser is stored independently from the activating/payment player. Payment capacity and
specific-X feasibility use the root activating player; candidate context records the actual controller player at the
Forge announcement callback.

## Atomic model and revalidation

Candidates are ordered numerically and identified as `X|0`, `X|1`, and so on. Exactly one candidate is forced; two or
more are strategic. X=0 remains a real forced outcome when it is the only candidate.

Applying a candidate recomputes applicability, Forge bounds, target completion, capacity, reduction allowance, and
specific-X payment. A stale candidate fails before `setXManaCostPaid`; it is never clamped.

Generated X requests reuse the priority action's `ActionContinuation`. Raw Forge callbacks, derived/copy/wrapper
cases, unsupported states, and feasibility probes do not consume a subdecision index.

## Unsupported and non-applicable classifications

Unsupported capability boundaries are:

- unresolved mode whose choice precedes X;
- non-mana X or unsupported/multiple X mana structure;
- nonnumeric/dynamic `XMax` or `AnnounceMax` in the information-safe v0 slice;
- incomplete fixed mana inventory, including alternative activation, ProduceMana replacement, or mana-source
  `Amount` multiplicity;
- unprovable domain-wide reduction allowance;
- cost-adjustment choice or unsupported adjustment;
- unknown payment payer;
- unresolved target chooser, X-dependent target completion, or target completion without a pure oracle;
- unsupported specific-X payment feasibility.

Derived X and copied/wrapper abilities that Forge never announces are `NOT_APPLICABLE`, not unsupported. A value
already stored in `XManaCostPaid` is replaced if Forge reaches another real player-X callback; explicit Charm
announcement ordering remains authoritative through Forge's local `needX=false`, so no second callback is generated.
Cancellation remains Forge's authoritative rollback behavior and is not an X candidate.

## Tests

The focused regression command covers X, cost feasibility, cost-adjustment preview, TARGET, PAYMENT, and diagnostics:

```text
mvn -pl forge-gui-desktop -am \
  -Dtest=forge.game.decision.XDecisionProviderTest,forge.game.decision.PriorityCostFeasibilityTest,forge.game.cost.CostAdjustmentPreviewTest,forge.game.decision.TargetDecisionProviderTest,forge.game.decision.PriorityActionDiagnosticsTest,forge.game.decision.PaymentDecisionProviderTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: 120 tests passed, 0 failures, 0 errors. Packaging with
`mvn -pl forge-gui-desktop -am -DskipTests package` succeeded.

The fixtures cover X=0, forced and strategic X, `XMin`, `XMax`, `AnnounceMax`, Invoke's selected modes, insufficient
mana, fixed reduction/increase, announced-X propagation, root-over-sub authority, derived/copied X, stale rejection,
deterministic identity, continuation identity, hidden-hand differential, non-mana X, unsafe unbounded inventory,
adjustment choice, pure target completion, unresolved target chooser, dynamic `Amount` production, pre-existing X
replacement, copy/wrapper suppression, explicit Charm `needX` callback ordering, and the absence of any forge-ai
dependency from the forge-game X provider.

The key off-color fixture uses two Islands, one Mountain, and five Forests for Invoke. It proves `S=8` and exports
exactly `X=0..5`; deriving capacity from an X=0 PAYMENT request would have incorrectly stopped at three resources.

The real Everflowing Chalice regression gives the source five charge counters. Capacity returns
`UNSUPPORTED / DYNAMIC_MANA_PRODUCTION`, `assessPaymentAtX(..., 5)` returns `UNSUPPORTED` rather than `UNPAYABLE`,
and the X provider returns `UNSUPPORTED_FINITE_DOMAIN` with no truncated request.

## Controlled benchmarks

Both accepted matchups completed through the packaged artifact:

```text
Dead and Alive vs Air Forces: 10 games, seed 20260809, result 7-3
Izzet Guild Kit vs Dimir Guild Kit: 10 games, seed 20260810, result 5-5
```

| Matchup | Raw X callbacks | Neutral X requests | Forced | Strategic | Unsupported X states |
|---|---:|---:|---:|---:|---:|
| Dead and Alive vs Air Forces | 0 | 0 | 0 | 0 | 0 |
| Izzet Guild Kit vs Dimir Guild Kit | 0 | 0 | 0 | 0 | 0 |

AI preselection meant neither run reached Forge's generic player X callback. The zero is reported directly; AI was
not changed to manufacture callbacks. The Guild run retained the known FRL-01A
`COST_ADJUSTMENT_CHOICE_REQUIRED` diagnostics for Invoke the Firemind and Direct Current.

## Focused X metrics

Real neutral Invoke fixtures produced candidate counts `[1, 3, 6, 3]`:

| Metric | Value |
|---|---:|
| candidate mean | 3.25 |
| candidate p50 | 3 |
| candidate p95 | 6 |
| candidate max | 6 |
| forced | 25% |
| strategic | 75% |
| minimum candidate value | 0 |
| maximum candidate value | 5 |

After 20 warmups, 180 generations of the six-candidate off-color fixture measured p50 29.066 ms, p95 41.422 ms,
and p99 49.227 ms. These are request-generation measurements, not RL throughput. The cost is expected because each
integer receives a complete supported payment search. The observed maximum X list (6) is below FRL-02C PAYMENT's
observed maximum candidate list (11); no bucketing or compression is introduced.

## Milestone status

The neutral X boundary is complete for the declared fixed-output controlled slice. Broader dynamic bounds,
non-mana X, mode-dependent X legality, target-search-dependent X, and adjustment choices remain explicit later
capabilities.

**FRL-02D: PASS (controlled slice)**
