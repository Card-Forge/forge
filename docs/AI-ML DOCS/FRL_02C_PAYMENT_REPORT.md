# FRL-02C Payment Decomposition Report

## 1. Repository state

- Base: `bf5a15b6abc4eded25ecbe30cef1d43a2d93ec71` (merged PR #3 / FRL-02B)
- Branch: `frl/02c-payment-boundary`
- Head: recorded after the implementation commit
- Working tree: required to be clean before publication

## 2. Forge payment architecture

The traced Forge path is:

```text
CostPayment.payCost
  -> CostAdjustment.adjust(root live action/cost)
  -> CostPart order
  -> CostPartMana.payAsDecided(actual payer)
  -> payer.getController().payManaCost(...)
  -> PlaySpellAbility.payManaCost
  -> live ManaCostBeingPaid (including announced X and final adjustment)
  -> payer controller applyManaToCost(...)
  -> floating Mana or real mana SpellAbility
  -> CostPayment for the mana ability activation cost
  -> ManaEffect.resolve / AbilityManaPart.produceMana
  -> ManaPool.payManaFromAbility
  -> ManaCostBeingPaid.payMana
  -> CostPayment completion
```

The human path enters `InputPayManaOfCostPayment`, discovers live Forge mana abilities, and calls
`PlaySpellAbility.playSpellAbility`. Variable production is chosen later in `ManaEffect.resolve` through controller
`chooseColor` / `specifyManaCombo`; the earlier human "express mana choice" is a convenience heuristic, not a neutral
decision boundary. The AI path uses `ComputerUtilMana`; it is observed diagnostically but never imported or called by
the neutral provider.

Non-mana `CostPart` instances execute in Forge's cost-part order through `CostPayment`. Tap, sacrifice, discard,
life, counter, exile, and similar selection semantics are not reimplemented here.

## 3. Raw callback classification

The two final ten-game benchmark files contain these observed operations:

| Callback / operation | Count | Classification | Reason |
|---|---:|---|---|
| controller `payManaCost` / `applyManaToCost` entry | 1,637 | `ENGINE_BOOKKEEPING` | Opens/re-enters a payment session but is not itself an atomic resource choice. |
| one-candidate neutral request | 126 | `FORCED_PAYMENT_ACTION` | Exactly one semantically distinct legal resource after neutral filtering. |
| multi-candidate neutral request | 529 | `STRATEGIC_PAYMENT_DECISION` | Two or more distinct live Forge resources remain legal. |
| variable mana output reached by diagnostic AI play | 192 | `UNSUPPORTED` | The real choice occurs in `ManaEffect.resolve`; FRL-02C does not let AI/Human express-choice choose it for an agent. |

Across those 2,484 classified observations this is 65.90% engine/session bookkeeping, 5.07% forced, 21.30%
strategic, and 7.73% unsupported. These percentages describe diagnostic operations, not RL throughput.

## 4. Atomic PAYMENT model

```text
DecisionRequest(PAYMENT)
  context:
    payment_stage = SOURCE
    remaining_cost_summary
    payer_id
    decision_sequence_id? / subdecision_index?
  candidates:
    USE_FLOATING_MANA(exact private Mana reference, public stable semantic key)
    ACTIVATE_MANA_SOURCE(Forge card id + game timestamp + ability index)
```

There is no complete-payment-combination enumeration. Generation examines only the live pool and local battlefield
mana abilities. Candidate IDs are request-local indexes after deterministic semantic-key sorting. Object identity is
kept private and is never exported as policy semantics.

`PAYMENT_MANA_OUTPUT` remains an evidence-based future stage: fixed outputs are supported as one source action;
variable outputs return structured `VARIABLE_MANA_OUTPUT` instead of invoking Human/AI choice code.

## 5. Example sequences

```text
Lightning Bolt, one Mountain
PRIORITY_ACTION -> PAYMENT [Mountain] (forced) -> Forge activates/taps -> COMPLETE

Lightning Bolt, two Mountains
PRIORITY_ACTION -> PAYMENT [Mountain#A, Mountain#B] -> choose #B -> COMPLETE

fixed U B production
PAYMENT [Dimir Aqueduct] -> one activation -> Forge produces the fixed U+B bundle
  -> ManaPool pays every currently useful unit -> next PAYMENT or COMPLETE

Dark Banishing, three Swamps
PRIORITY_ACTION -> TARGET -> PAYMENT [Swamp#A, #B, #C]
  -> choose #A -> PAYMENT [#B, #C] -> choose #B -> PAYMENT [#C] -> COMPLETE
```

The focused tests exercise all four forms with real Forge cards/state; the fixed bundle is never split into two
source activations.

## 6. Payer semantics

`CostPartMana.payAsDecided` passes its concrete payer to that player's controller. The request therefore records
`payer_id` from this callback argument, not priority player or action owner. The continuation retains the originating
action independently. A focused test uses a non-priority player as payer; payment without an action continuation
retains null sequence/subdecision metadata.

## 7. Forge mutation path

For a source candidate, `apply` first regenerates legal prototypes from the same live cost/payer/action and matches
kind plus stable semantic key. It then calls `PlaySpellAbility.playSpellAbility`; Forge pays the activation cost,
taps the source, resolves `ManaEffect`, and produces real `Mana`. `ManaPool.payManaFromAbility` applies useful
production to the live `ManaCostBeingPaid`.

For floating mana, `ManaPool.tryPayCostWithManaInstance` validates restrictions, shard compatibility, and exact
instance presence, emits the normal pool event, and calls `ManaCostBeingPaid.payMana`. This avoids the existing
same-color/`Mana.equals` first-match behavior without adding a parallel pool or cost engine.

## 8. Forced vs strategic

Across both matchups: 126/655 atomic requests (19.24%) were forced and 529/655 (80.76%) were strategic. This split
is calculated only over generated, supported atomic requests. Unsupported variable-output observations are separate.

## 9. Candidate distribution

Across 655 supported requests:

| Metric | Candidates |
|---|---:|
| Mean | 3.226 |
| p50 | 3 |
| p95 | 7 |
| Max | 11 |

## 10. Callback compression

| Matchup (10 games) | Raw callbacks | Atomic requests | Forced | Strategic | Unsupported states |
|---|---:|---:|---:|---:|---:|
| Dead and Alive vs Air Forces | 789 | 430 | 64 | 366 | 0 |
| Izzet Guild Kit vs Dimir Guild Kit | 848 | 225 | 62 | 163 | 192 |
| Total | 1,637 | 655 | 126 | 529 | 192 |

The 236 action sequences that generated supported payment requests averaged 2.775 atomic requests per paid action.
The first matchup averaged 3.071 (430/140), and the Guild matchup 2.344 (225/96). Raw callbacks and atomic decisions
are therefore demonstrably not one-to-one.

## 11. Cost correctness

The provider consumes Forge's live `ManaCostBeingPaid`, created after optional/alternate cost, mode, X, targets, and
the final `CostAdjustment.adjust` call. It never regenerates printed card cost. Context normalizes the passed ability
to `getRootAbility`; a test passes an `AbilitySub` with `Cost.Zero` and verifies that the remaining root `{2}{B}` cost
and root action remain authoritative. Another test mutates the live remaining requirement before generation and
observes that updated requirement. MODE and X selection are not implemented; hidden/unresolved choices remain a
controlled-slice dependency.

## 12. Information safety

Candidate generation inspects only the payer's battlefield, payer's mana pool, public action/cost state, and public
source characteristics. It exports no opponent hand/library objects, AI estimates, or mutable `Mana`/`SpellAbility`
references. A differential test changes an irrelevant opponent hidden hand card and obtains identical keys/order.
Remaining risk is a future restriction/replacement whose legality cause is itself hidden; such semantics must be
added as structured unsupported rather than exposed.

## 13. Cancellation / rollback

No `CANCEL_PAYMENT` candidate was invented. Inability to continue returns `INVALID_PAYMENT`; unsupported semantics
return `UNSUPPORTED`; a stale selection throws before mutation. Intentional Human cancellation remains owned by
`InputPayManaOfCostPayment`. Announcement failure continues through `CostPayment.refundPayment`,
`GameActionUtil.rollbackAbility`, and `ManaRefundService`. ForgeRL has no rollback engine.

## 14. Unsupported payment mechanics

Structured provider reasons currently cover variable mana output, complex/non-tap source activation costs,
multiple mana parts, Phyrexian mana, and snow provenance. Strategic non-mana selections, Convoke, Delve, Improvise,
Assist, Offering, Emerge, Waterbend, dynamic `Special` mana, controller-dependent cost order, and replacement-driven
complex payment remain outside v0 unless a smaller mechanism is separately proven. `AUTO_PAY` is absent because the
Human button delegates to `ComputerUtilMana`.

## 15. Tests

Commands:

```text
mvn -pl forge-gui-desktop -am "-Dtest=forge.game.decision.PaymentDecisionProviderTest" \
  "-Dsurefire.failIfNoSpecifiedTests=false" test
# 23 tests, 0 failures/errors/skips

mvn -pl forge-gui-desktop -am \
  "-Dtest=forge.game.decision.*Test,forge.game.cost.CostAdjustmentPreviewTest,forge.game.mana.ManaRefundServiceTest" \
  "-Dsurefire.failIfNoSpecifiedTests=false" test
# 95 tests, 0 failures/errors/skips

mvn -pl forge-gui-desktop -am -DskipTests package
# BUILD SUCCESS
```

The focused suite covers exact floating identity, one/two sources, forced/strategic status, deterministic ordering,
fixed bundles, variable-output unsupported, tapped/consumed exclusion, partial regeneration, completion/invalid,
stale and foreign candidate rejection, continuation/payer/root/live cost, hidden information, and Phyrexian failure.
Compilation also enforces the module boundary: `forge-game` cannot depend on the downstream `forge-ai` module.

## 16. Controlled benchmark

Both accepted matchups completed ten seeded games through the packaged Forge artifact:

```text
Dead and Alive vs Air Forces: seed 20260809, result 7-3
Izzet Guild Kit vs Dimir Guild Kit: seed 20260810, result 5-5
```

The Guild run logged the pre-existing FRL-01A partial feasibility exceptions for `Invoke the Firemind` and
`Direct Current` (`COST_ADJUSTMENT_CHOICE_REQUIRED`) while completing all games. Payment metrics are the table in
section 10. Raw CSVs were retained outside the repository in the OS temporary directory.

## 17. Performance

Supported request generation over both benchmarks:

| Percentile | Time |
|---|---:|
| p50 | 133.4 us |
| p95 | 312.6 us |
| p99 | 477.3 us |

Generation performs no game copy, complete-combination enumeration, search, AI evaluation, or observation
serialization.

## 18. Continuation invariant

All 655 generated benchmark PAYMENT requests were correlated and all had the same sequence as their originating
priority action. There were zero uncorrelated generated requests. All 192 unsupported variable-output state records
retained their action sequence but intentionally received no subdecision index because no agent request was generated.

The API still truthfully supports uncorrelated payment when called outside a priority announcement; its sequence and
subdecision fields remain null rather than inventing a parent.

## 19. ML implication

Within these decks, supported PAYMENT is not mostly forced: 80.76% of generated requests had multiple distinct
resources. It is therefore likely to be a material fraction of policy inference steps in the controlled slice.
However, 192 Guild observations reached unsupported variable output, so no claim is made for the complete accepted
slice or for broader Magic. Raw callback volume substantially overstates atomic step volume.

## 20. Next DecisionRequest recommendation

`X_VALUE` is the single recommended next family. The accepted Guild benchmark directly exposed live X/cost-preview
dependencies (`Invoke the Firemind`) and X is required before those actions can enter payment with a fully determined
live remaining cost. This report does not implement it.

FRL-02C: PARTIAL
