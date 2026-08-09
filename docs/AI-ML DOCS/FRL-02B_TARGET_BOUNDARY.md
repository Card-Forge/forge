# FRL-02B — TARGET Decision Boundary

## Authoritative Forge path

`PlaySpellAbility.playAbility` announces type and X-like values, performs its early restriction checks, and then
calls `SpellAbility.setupTargets()`. `setupTargets()` walks the live root ability and its sub-abilities. For each
targeting group it clears the current `TargetChoices`, resolves `TargetingPlayer` (or uses the activating player),
stores that player on the live `SpellAbility`, and invokes `targetingPlayer.getController().chooseTargetsFor(...)`.

FRL-02B observes that boundary immediately after Forge has assigned the targeting player. It neither replaces the
human controller nor the AI controller.

`TargetRestrictions.getAllCandidates(SpellAbility)` supplies the current card/player candidates. The provider uses
the same `CardUtil.getValidCardsToTarget` operation as Forge's human target input to remove cards already chosen in
the current group, and removes an already chosen stack `SpellAbility` by its live `TargetChoices` membership.
Before exposing a request it mirrors `TargetSelection`'s minimum-completion preflight: enough remaining candidates
must exist, and `TargetsWithDifferentControllers` / `TargetsForEachPlayer` must have enough
distinct card controllers. For an isolated group it also uses Forge's `StaticAbilityMustTarget` filter and its final
restriction predicate, so an active MustTarget obligation suppresses player targets even when filtering removed no
cards. With multiple target groups it matches the human controller by deferring that global check until Forge's final
validation. `SpellAbility` then remains the legality authority: `canTarget(GameObject)` re-evaluates card/player legality against the live
`TargetChoices`, and `canTargetSpellAbility(SpellAbility)` does the same for stack spells. `TargetChoices.add(...)`
is the only application operation used by the provider. Forge re-checks target legality while resolving the stack
object (`MagicStack.hasFizzled`).

## Neutral request contract

`TargetDecisionProvider` turns one live target group into one atomic `DecisionRequest(TARGET)`. It carries the
actual Forge targeting player, group index, current selected count, min/max, and an FRL-02A continuation only when
one already exists. Candidate ordering is deterministic by public semantic target kind, Forge entity/stack id, and
zone/context; Java reference identity is private to applying a candidate to that live request.

Supported controlled-slice candidates are cards, players, and stack spell objects. Multi-target groups are
decomposed one target at a time; the provider re-runs Forge legality after each `TargetChoices.add(...)`. `DONE`
appears only when Forge reports that the current minimum is complete and the maximum has not been reached. There is
no generic `CANCEL` candidate: Forge's target-controller cancellation contracts are not uniform.

An empty candidate set before the minimum is met produces `INVALID_TARGETING`, rather than a request, an automatic
selection, or an AI fallback. Required multi-target groups coupled through same-controller, creature/card type,
mana value, name, toughness, or total-CMC/power restrictions are explicitly unsupported until Forge offers a
side-effect-free completion oracle; the provider does not simulate choices or recreate those rules. Divided-target
allocation, hidden/unidentifiable card targets, and unknown target entity types fail with
`UnsupportedTargetDecisionException` rather than being silently omitted. Random-target groups are Forge-owned
randomness rather than player choices and are likewise explicitly unsupported.

## Information and downstream cost safety

Cards must be legally identifiable through Forge's `CardView` for the actual targeting player. A face-down card that
may be selected but whose identity may not be shown has an empty exported name. Unrelated hidden-zone changes do
not participate in target candidate generation.

After the provider completes a group, it runs the existing side-effect-free `PriorityCostFeasibility` preview against
the cost-bearing root ability and its activating player, never the `Cost.Zero` target sub-ability. A caller can then
discard an obsolete pre-target estimate. Forge remains authoritative and recalculates adjusted cost again in
`CostPayment` immediately before payment. FRL-02B does not add a PAYMENT decision.

The `setupTargets()` hook is intentionally diagnostic-only: it observes a live generic target operation without
replacing its human or AI controller. An external-controller callback/sink is deferred, so the provider is consumed
directly by the focused live-state tests in this milestone.
