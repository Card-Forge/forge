# FRL-02C Payment Boundary Design

**Status:** Approved

**Authority:** `docs/AI-ML DOCS/ML_STRATEGY.md` and the FRL-02C milestone contract

## Goal

Add the smallest algorithm-neutral `PAYMENT` decision boundary for the accepted ForgeRL slice. The boundary exposes strategically meaningful, sequential resource choices while Forge remains authoritative for costs, payment state, mana-ability activation, mana production, consumption, completion, cancellation, and rollback.

FRL-02C does not implement reinforcement learning, complete-payment enumeration, a replacement mana engine, MODE, X_VALUE, TARGET changes, or general non-mana payment decomposition.

## Authoritative Forge Path

`PlaySpellAbility` announces type, mode, X-like values, and targets before constructing `CostPayment` from the live root action. `CostPayment.payCost` runs `CostAdjustment.adjust`, orders the adjusted cost parts, and calls `CostPart.payAsDecided` with the actual `CostDecisionMakerBase` player.

For mana, `CostPartMana.payAsDecided` calls that payer's `PlayerController.payManaCost`. `PlaySpellAbility.payManaCost` constructs the live `ManaCostBeingPaid`, retains announced X, applies final mana-cost adjustments, and invokes `PlayerController.applyManaToCost`. This callback is the PAYMENT session entry and supplies the actual payer and live remaining cost; it is not necessarily the location of every atomic payment choice.

The human path enumerates playable Forge mana abilities, activates one through `PlaySpellAbility.playSpellAbility`, resolves real mana production in `ManaEffect`, and applies produced mana with `ManaPool.payManaFromAbility`. Floating mana is removed through `ManaPool.tryPayCostWithMana`. The AI path performs comparable mutations but selects resources with `ComputerUtilMana`; those heuristics are not a legality oracle.

If payment fails or is cancelled, failure propagates through `CostPayment` to `GameActionUtil.rollbackAbility`, `CostPayment.refundPayment`, and `ManaRefundService`. FRL-02C adds no rollback implementation.

## Neutral Session and Atomic Stages

`PaymentDecisionProvider` receives the live `ManaCostBeingPaid`, root cost-bearing `SpellAbility`, actual payer, active mana conversion matrix, and optional `ActionContinuation` at `applyManaToCost`.

The supported stage is `PAYMENT_SOURCE`, whose candidates are:

- one specific compatible floating `Mana` resource; or
- one playable, supported fixed-output mana ability.

Selecting a fixed-output source executes the real Forge mana ability. A fixed multi-output bundle such as `Produced$ U B` remains one source activation. Forge produces the complete bundle and `ManaPool.payManaFromAbility` consumes whichever produced mana can pay the live remaining cost.

Variable-output abilities are supported only if their real controller choice inside `ManaEffect.resolve` can be intercepted as `PAYMENT_MANA_OUTPUT` without controller redesign. The output request must be generated from Forge's actual offered colors or combo at that callback. Human express choice and AI color heuristics may not select the output. If the real callback cannot be intercepted neutrally and narrowly, variable-output sources are rejected with a structured unsupported reason.

Completion is a provider status, not a synthetic `DONE` candidate. Impossible supported states return an explicit invalid status. There is no generic cancel or AUTO_PAY candidate.

## Candidate Legality and Identity

Candidate generation uses Forge state and rule operations only: payer-controlled battlefield cards, `Card.getManaAbilities`, alternative-cost expansion where supported, `SpellAbility.canPlay`, mana-ability compatibility, live `ManaCostBeingPaid`, mana restrictions, and the payer's live `ManaPool`. It must not call `ComputerUtilMana`, `AiCostDecision`, or other AI evaluators.

Every exported candidate has a deterministic request-local id and semantic key. Source candidates use Forge card id, game timestamp, stable ability position, and fixed production semantics. Floating candidates retain a private reference to the exact live `Mana` object and identify it with Forge-visible provenance and a deterministic occurrence index. Java identity, `toString`, and global SpellAbility construction ids are not policy semantics.

Application re-generates or equivalently revalidates the candidate against the live remaining cost, pool membership, payer, source controller, source timestamp, tap state, ability availability, restrictions, and output semantics. A stale candidate fails before mutation.

## Floating Mana Equivalence

Compatibility is evaluated per live `Mana` object, not per color. Same-color mana is not merged when source provenance, restrictions, spending triggers/effects, snow status, persistence, combat status, or other Forge-visible semantics differ.

Candidates may be collapsed only when equivalence is demonstrated from all relevant Forge state. When equivalence cannot be proven, the resources remain distinct. If a relevant distinction cannot be represented safely, generation fails explicitly rather than silently selecting the first mana of a color.

A single semantically distinct compatible floating resource is forced or bookkeeping. Two or more distinct compatible resources produce a strategic PAYMENT request. Purely forced internal compatibility checks, bundle application, and completion checks do not allocate a continuation subdecision.

## Payer and Continuation

`payer_id` comes from the player whose controller received `applyManaToCost`, not from priority ownership or assumed action ownership. The cost-bearing root action and payer are stored separately.

When an `ActionContinuation` exists, every generated atomic PAYMENT request reuses its `decision_sequence_id` and allocates exactly one new `subdecision_index`. Engine bookkeeping and diagnostic-only callbacks do not increment it. Payments without an originating priority action remain explicitly uncorrelated.

## Cost Correctness

The session consumes the live `ManaCostBeingPaid` produced after Forge has processed the current mode, X, targets, optional or alternate costs, and payment-time adjustment. It never rebuilds payment from a target sub-ability or substitutes `AbilitySub Cost.Zero` for the root cost.

`CostAdjustment.preview` may be used for diagnostics and consistency checks against the live root action. It does not execute payment and cannot replace the live remaining-cost object.

## Operation Classification

The initial classification is:

| Operation | Classification | Reason |
| --- | --- | --- |
| cost adjustment, cost ordering, cost-stack push/pop | `ENGINE_BOOKKEEPING` | Forge owns payment preparation |
| compatibility checks, remaining-cost updates, completion checks | `ENGINE_BOOKKEEPING` | no independent resource choice |
| consume one proven-equivalent compatible floating resource | `ENGINE_BOOKKEEPING` or `FORCED_PAYMENT_ACTION` | no meaningful alternative |
| choose among distinct compatible floating resources | `STRATEGIC_PAYMENT_DECISION` | provenance or spending semantics can affect the result |
| activate the only supported legal fixed-output source | `FORCED_PAYMENT_ACTION` | meaningful atomic operation with one candidate |
| choose among multiple supported fixed-output sources | `STRATEGIC_PAYMENT_DECISION` | future available resources differ |
| tap-only activation cost of a selected source | `FORCED_PAYMENT_ACTION` | Forge applies the forced source cost internally |
| choose a variable mana output at the real Forge callback | `STRATEGIC_PAYMENT_DECISION` | supported only with a neutral callback interception |
| apply produced fixed bundle with `payManaFromAbility` | `ENGINE_BOOKKEEPING` | source choice already determined the bundle |
| strategic sacrifice, discard, exile, counter, or tap selection | `UNSUPPORTED` | requires another decision family |
| Phyrexian life versus mana and deferred complex mechanics | `UNSUPPORTED` | outside controlled v0 |
| human/AI auto-pay | `UNSUPPORTED` | delegates to AI strategy |
| cancel and rollback | `ENGINE_BOOKKEEPING` | existing Forge rollback remains authoritative |

## Supported and Unsupported Scope

Supported v0 is ordinary mana payment from compatible floating mana and tap-only, single-use, fixed-output mana sources with no strategic activation cost beyond tapping. Fixed multi-output bundles are supported as one activation.

Variable-output sources are conditional on the real `ManaEffect` choice interception described above. Strategic non-mana costs, Phyrexian payment, Convoke, Delve, Improvise, Assist, Offering, Emerge, Waterbend, dynamic Special mana, snow/provenance cases that cannot be represented, multi-use or non-tap engines, controller-dependent cost order, and complex replacement effects fail loudly unless focused evidence proves a smaller safe subset.

## Diagnostics and Benchmarks

Diagnostics remain optional and information-safe. They record sequence id, subdecision index, payment stage, payer id, candidate count, forced status, remaining generic/colored summary, provider status, unsupported reason, and generation time. They do not serialize internal mana objects or hidden identities.

The existing AI path may be observed diagnostically. A reliable AI-selected source may be checked for membership in the neutral request, but AI choices never create or filter legal candidates.

The controlled benchmark runs at least ten games each for Dead and Alive versus Air Forces and Izzet Guild Kit versus Dimir Guild Kit when runtime permits. It reports raw Forge PAYMENT callbacks separately from generated atomic requests, forced and strategic counts, request candidates, source-choice diversity, unsupported operations, and generation latency percentiles.

## Testing Strategy

Tests use real Forge game state and cards where possible. They cover exact live-mana identity, fixed one-source and multi-source payments, fixed bundles, sequential partial payment, completion and invalid states, stale candidates, deterministic ordering, continuation identity, root cost, target-modified live state, payer identity, hidden-information invariance, unsupported mechanics, and the absence of AI legality dependencies.

Every production behavior is introduced test-first. Focused tests run before the broader ForgeRL regression suite and controlled match benchmarks.
