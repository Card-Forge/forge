# FRL-02D X Value Boundary Design

## Scope

FRL-02D adds an algorithm-neutral `X_VALUE` decision boundary at Forge's existing
`PlayerController.announceRequirements(..., "X")` callback. It does not change Human or AI selection,
implement MODE, expand PAYMENT, or introduce learning/search behavior.

## Forge ordering

The boundary runs exactly where Forge announces X. Ordinary abilities execute type announcements before X.
Charm abilities with explicit `Announce$ X` execute X before mode selection; other Charms, including Invoke the
Firemind, select their mode before X. The provider never moves X relative to MODE or TARGET.

## Request applicability

Only a callback that represents a genuine player-selected X may produce a request. Derived X, copied abilities,
and wrappers are not applicable and produce no request. A fully understood state with no completion-safe value is
`INVALID_X`. Capability gaps are `UNSUPPORTED` with a structured reason.

## Candidate domain

Forge's `AbilityUtils.getAnnouncementBounds` supplies the raw minimum and maximum. The provider supports exactly
one mana cost part containing X and rejects non-mana X, multiple mana parts, unresolved pre-X modes, X-dependent
targeting, hidden-information-sensitive bounds, incomplete mana inventories, and unsupported adjustments.

The finite-domain proof uses:

```text
S = cost-independent maximum mana capacity from every fully understood FRL-02C-compatible resource
R = conservative maximum generic reduction allowance across the entire supported X domain
k = number of X shards in the root mana cost

upper = min(rawMax, floor((S + R) / k))
```

`S` is never derived from PAYMENT candidates for `rawMin` or another specific cost. It includes floating mana and
the maximum fixed bundle from each unique supported source even when that mana cannot advance the current colored
cost. If any potentially available resource is not completely modeled, the request is unsupported because the
domain could otherwise omit legal X values.

`R` is supplied by `CostAdjustment` as a domain-wide conservative allowance. It is not the reduction applied to
one previewed candidate. A fixed numeric reduction contributes its full numeric amount; choice-dependent,
variable, multiple, or otherwise unbounded reductions are unsupported.

Every integer in the proven interval is assessed individually. `PAYABLE` values become candidates in numeric order
with semantic keys `X|N`; `UNPAYABLE` values are omitted. Any `UNSUPPORTED` assessment makes the whole request
unsupported rather than silently incomplete. Search does not stop at the first unpayable value.

## Specific-X feasibility purity

`PriorityCostFeasibility.assessPaymentAtX(payer, root, x)` passes X only to
`CostAdjustment.preview(cost, root, payer, effect, x, root.getXColor())`. It never sets live root X or activating
player state, mutates live mana abilities, activates a source, or writes the mana pool. Mana availability is
represented by immutable shadow records constructed from already prepared state and pure source checks.

## Target completion

The X provider performs a pure target-completion preflight only when the future targeting chooser is already
deterministic. A normal target group without `TargetingPlayer` uses the activating player. If Forge must later ask a
controller to select the targeting player, X generation is unsupported. The preflight does not generate a TARGET
request, allocate IDs, consume continuation indices, mutate `TargetChoices`, or re-preview costs.

## Continuation and apply

Only a generated neutral X request consumes the next continuation subdecision index. Raw callbacks, derived/copy/
wrapper cases, support probes, and invalid or unsupported states do not. Applying a candidate recomputes bounds,
capacity, adjustment support, target completion, and the complete payable domain without allocating another index.
If `X|N` is absent, apply fails before mutation. A valid candidate is applied only through
`root.setXManaCostPaid(N)`.

## Diagnostics

Raw Forge X callbacks and neutral X requests are separate events. Request diagnostics include sequence/subdecision,
chooser, raw bounds, candidate count/range, forced state, status, unsupported reason, and generation time without
dumping complete candidate lists.
