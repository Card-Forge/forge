# FRL-02E MODE Boundary Design

## Scope

FRL-02E adds an algorithm-neutral `MODE` decision boundary at Forge's existing
`PlayerController.chooseModeForAbility` callback. It supports only ordinary, noncopied,
nontrigger, nonoptional Charm abilities whose actual chooser is the activating player,
whose literal/default mode count is exactly one, which cannot repeat modes, and whose
modes do not alter payment cost.

Invoke the Firemind and Izzet Charm are the primary real-card fixtures. Reinforcement
learning, multi-mode decomposition, CONFIRMATION, CARD_SELECTION, new TARGET or PAYMENT
capabilities, search, transport, and Forge AI legality are outside this milestone.

## Callback authority

`CharmEffect.makeChoices` already clears the previous modal chain, filters the live
`Choices`, handles Entwine, Optional confirmation and Random engine selection, resolves
the actual chooser, and computes `min`, `num`, and `canRepeat` before calling
`chooseModeForAbility`. `ModeDecisionProvider` therefore accepts that exact callback
state:

```text
generateModeRequest(root, possible, min, num, allowRepeat, choosingPlayer, continuation)
```

It never calls `CharmEffect.makePossibleOptions`. Each supplied live `AbilitySub` is
mapped by identity to its original zero-based position in
`root.getAdditionalAbilityList("Choices")`. Candidate keys are `MODE|N`, so filtering
does not renumber semantic identity.

## Candidate and context

`DecisionType.MODE` requests contain public, immutable candidate data: request-local
candidate id, original mode ordinal, `MODE|N`, public description, and whether the mode
uses targeting. The live `AbilitySub` is retained package-privately for application and
is never serialized.

`ModeDecisionContext` retains the live root and callback-local possible list privately,
while exposing choosing-player id, activating-player id, numeric selection constraints,
continuation identity, and subdecision index. Exactly one candidate is forced; two or
more are strategic.

## Completion safety

TARGET completion is branch-local. `TargetDecisionProvider.assessBranchCompletion`
starts at the detached supplied mode, walks only that mode's sub-chain, derives the
normal future targeting player from the root activating player, and never climbs to the
root chain. It creates no request, consumes no identifiers, mutates no `TargetChoices`,
and performs no cost preview. Existing coupled, divided, random, unresolved targeting
player, and hidden-target safety rules remain authoritative.

For MODE-before-X, `XDecisionProvider.assessFutureXPaymentDomain` reuses the accepted
FRL-02D finite-domain and payment checks without requiring an attached mode, performing
TARGET checks, creating a request, allocating identifiers, consuming a continuation
index, or mutating X. Candidate-specific target completion remains separate. For
X-before-MODE, the already announced X remains untouched and ordinary root payment
support is assessed from current state.

PAYMENT support is shared from the accepted feasibility machinery. Mode-dependent costs,
Spree, Tiered, Pawprint, and unsupported adjustment/payment states reject the MODE state;
FRL-02E adds no payment capability.

## Application

`ModeDecisionProvider.apply` validates request membership, callback shape, chooser,
original ordinal membership in the callback state, and selected-branch completion. It
returns the current live `AbilitySub`; it does not attach anything to the root. The
controller returns the selected singleton list to Forge, and `CharmEffect.chainAbilities`
remains the sole authority for copying, final `CharmOrder`, stack description, and
attachment.

## Diagnostics and continuation

The authoritative callback records three distinct outcomes:

```text
MODE_CALLBACK  raw Forge callback; no subdecision index
MODE           supported neutral request; consumes one subdecision index
MODE_STATE     unsupported or invalid non-request; no subdecision index
```

The legacy `PlayerControllerAi.recordDownstreamCallback(MODE, ...)` call is removed to
prevent duplicate accounting. Existing Human and AI selection behavior is unchanged.
Generation records elapsed nanoseconds, candidate count, rule/legality probes, and
downstream-completion probes.

## Unsupported boundary

The provider rejects copied, trigger, optional, external-chooser, nonliteral/dynamic or
multi-mode, repeating, ModeCost, Spree, Tiered, Pawprint, unresolved targeting-player,
unprovable target-completion, and unsupported shared X/payment states. Entwine and actual
Random selection remain Forge-owned and never reach a neutral MODE request.
