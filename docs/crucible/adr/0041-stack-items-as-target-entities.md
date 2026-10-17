# ADR-0041 — An Ability on the Stack Is a Target Entity

- **Status:** Accepted
- **Date:** 2026-10-17
- **Deciders:** Crucible session (M6 R25e)

## Context

CR 115.1d-e: activated and triggered abilities on the stack are targets of "counter target activated ability" (Stifle,
Bind, Trickbind, Squelch, Interdict, Rust, Disallow, Deny the Witch, Voidslime, ...), of "change the target of target
spell or ability" (Willbender, Bolt Bend, Reroute, Spellskite) and of "copy target activated or triggered ability"
(Strionic Resonator, Lithoform Engine). Java keeps the targeted `SpellAbility` itself in `TargetChoices`
(`TargetSelection.java:288-331`, `chooseCardFromStack`) and re-checks it at fizzle time by stack-instance identity
(`MagicStack.java:753-755`).

ADR-0009 types `EntityID` as "card or player, for targeting". A spell is targeted as its card on the stack
(`CardEntity`, R9i `m6-r9i-stack-targets.md`), which works because a spell has a card in the Stack zone. An activated or
triggered ability has no card of its own: its `Source` is its host, which may also have a spell, another ability or a
permanent in play. `stackAbilityCandidates` (`targeting.go`) therefore errors whenever such an ability would be a legal
target, so 66 corpus cards naming `TargetType$ Activated`/`Triggered`/`Ability`/`SpellAbility` either never cast or fail
when they resolve.

## Decision Drivers

- GO-9: identity by integer handle, compared with `==`; no pointer into `Game.stack`.
- `ChooseTargets`, `Ability.Targets`, `Memory`, `Event.Target` all carry `EntityID`; a target kind outside it would need
  a parallel list through every one of them.
- `StackItemID` (ADR-0018) already names one stack object for its whole life and is never reused.
- `Game.Clone` and `TestCloneAllocationsStayBounded`: no new per-ability state.

## Considered Options

1. **Third `EntityID` kind tagging a `StackItemID`** (bit 30; bit 31 stays the player tag). Spells stay `CardEntity`.
2. A separate `Ability.StackTargets []StackItemID` beside `Targets`. Rejected: `ChooseTargets`, `trimTargetSet`,
   `dropIllegalTargets`, Charm modes, `chainTargets` and the copy/retarget paths would each need a second list and a
   second answer; Disallow's "spell, activated ability or triggered ability" is one choice over a mixed pool.
3. Every stack target as a stack item, spells included (Java's model). Rejected: Counter, ChangeZone `Origin$ Stack`,
   CopySpellAbility, ControlSpell and the `Defined$ Targeted` card readers already read a spell as its card; moving them
   all is a larger change with no card that needs it.

## Decision

Option 1. `StackItemEntity(StackItemID)` and `EntityID.AsStackItem`; `AsCard` excludes the new tag, so every existing
card or player reader skips a stack item rather than misreading it. An ability on the stack is offered as
`StackItemEntity(item.ID)`; a spell keeps `CardEntity(item.Source)`. A stack-item target is legal at resolution while
its item is still on the stack and still passes the targeting ability's `TargetType$`/`TargetValidTargeting$`/
`ValidTgts$` check (`SpellAbility.canTargetSpellAbility`, `SpellAbility.java:2046-2110`).

## Consequences

**Good:** one target list, one controller decision, no new `Ability` or `Game` field; Clone untouched. **Bad:** card
counts are capped at 2^30 per game, and every reader that turns "not a card" into "a player" must check `AsPlayer`'s
`ok`. **Neutral:** a stack-item target names nothing once its item leaves the stack; Java keeps the object as last-known
information, so an effect needing an ability that already left reads nothing, as Java's
`getInstanceMatchingSpellAbilityID` null-check does for Counter.

## Related

ADR-0009, ADR-0018, ADR-0027, ADR-0028,
[`m6-r25e-abilities-on-stack.md`](../porting/port-log/game-state/m6-r25e-abilities-on-stack.md)
