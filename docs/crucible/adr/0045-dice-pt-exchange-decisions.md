# ADR-0045 — Dice P/T Exchange: Per-Card Set-P/T List, Two Controller Decisions

- **Status:** Accepted
- **Date:** 2026-10-19
- **Deciders:** Crucible session (R16)

## Context

One corpus card, Vedalken Squirrel-Whacker, replaces a roll of a d6 with an exchange: after the dice are rolled, each
Squirrel in the set may swap one roll with the creature's power or toughness (`RollDiceEffect.java:233-262`). The script
reaches it as `R:Event$ RollDice | ValidSides$ 6 | ReplaceWith$ SwapRoll`, `SwapRoll` =
`DB$ ReplaceEffect | VarName$ DicePTExchanges | VarType$ CardSet | VarValue$ Self`. `DicePTExchanges` is not a script
param: it is the `Set<Card>` `rollDiceForPlayer` creates (`:140`) and `ReplaceEffect` fills
(`ReplaceEffect.java:52-57`).

The swap phase makes three decisions: which roll (`chooseRollToSwap`, `PlayerController.java:279`, one or none), which
Squirrel (`chooseSingleEntityForEffect`, exists), which stat (`chooseRollSwapValue`, `:280`, Power or Toughness, or
none, which re-asks the roll). The effect is `Card.addNewPT` (`Card.java:4390-4398`): a Layer 7b set-P/T at a fresh
timestamp, permanent, cleared when the card changes zone. No effect card exists.

Constraints: Layer 7 output is rebuilt every state-based pass (ADR-0023), so a one-shot write would vanish;
`ChooseOption` has no "none" answer and `ConfirmEffect` first would add a decision Java does not make (the ADR-0044
argument).

## Decision Drivers

- Same persistence rule as stickers (ADR-0044): state on the card, effect rebuilt per pass.
- Replay parity: the decisions Java asks are the decisions Crucible asks, in the same order.
- GO-9: dice are values, not entities; a roll is an index into the rolled list.

## Considered Options

1. `Card.newPTs []newPT{Timestamp, Power, Toughness, HasPower, HasToughness}`, re-added as `LayerSetPT` effects each
   pass beside `stickerPT`, cleared on any zone change; two new `PlayerController` methods.
2. Reuse `animateRecord` with `Duration$ Permanent`. Rejected: it carries types, keywords and abilities Java does not
   touch here, and its timestamp rule differs (`Animate` in the same chain must beat the swap, which both orders give,
   but an Animate record is dropped by `Animate`'s own `RemoveAllAbilities` bookkeeping).
3. Answer both decisions with `ChooseOption`. Rejected: "none" is not expressible (a refused stat re-asks the roll).

## Decision

Option 1. `PlayerController` gains `ChooseRollToSwap(g, decider, source, rolls []int) int` (index, -1 for none) and
`ChooseRollSwapValue(g, decider, source, roll, power, toughness int) int` (0 Power, 1 Toughness, -1 none). The Squirrel
is picked with `ChooseCardsForEffect` (1 of the set). `replacementEvent` gains `exchanges []CardID`; `ReplaceEffect`
accepts `VarType$ CardSet` with `VarName$ DicePTExchanges` and `VarValue$ Self`, nothing else. The swap runs in
`rollDiceFor` after the modifier, before the total, ResultSVar and result sub-abilities. The set keeps insertion order
(Java's `HashSet` order is not reproducible).

## Consequences

**Good:** the rebuild passes stay pure over stored state; the same list serves any later `addNewPT` caller. **Bad:**
every full `PlayerController` gains two methods (`ScriptedController`, the smoke controller, one test controller).
**Neutral:** the smoke controller swaps nothing (-1), so the probe sees the roll unchanged.

## Related

ADR-0023, ADR-0044
