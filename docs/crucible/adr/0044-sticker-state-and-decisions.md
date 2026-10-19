# ADR-0044 — Stickers: Compiled Sheets, Per-Card Applied List, Two Controller Decisions

- **Status:** Accepted
- **Date:** 2026-10-19
- **Deciders:** Crucible session (R15)

## Context

36 corpus cards run `PutSticker`, 48 sheet cards (`Types:Stickers`, `K:StickerSheet:STK1,...`) hold 480 stickers, and 7
cards read `Mode$ StickerPlaced`. Java (`forge-game/.../card/sticker/`, `PutStickerEffect.java`):

- A sheet is a card in `ZoneType.StickerSheets`, owned by a player (3 random sheets from the deck's `Stickers` section
  at game start, `Player.java:3047-3053`). Each sticker is an SVar of the sheet, parsed on demand (`Sticker.parse`).
- A placed sticker is an `AppliedSticker` (sticker, timestamp, name position) on the host `Card.stickers`; the host
  re-applies PT, name and granted traits from it (`Card.java:4634-4698`). It survives a move to a public zone and is
  dropped by a move to a hidden one (`GameAction.java:268-270`).
- Two controller decisions: `chooseSticker` (pick one or none among the affordable, unused stickers,
  `PlayerController.java:256`) and `chooseStickerNamePosition` (`:260`).

Constraints: the Layer 4/5/6/7 passes rebuild their output every state-based pass (`traitGrants` and P/T are cleared per
pass, ADR-0023); a sticker is permanent state, so a one-shot write would vanish at the next pass. PORT-2 forbids parsing
sticker SVars at resolve time.

## Decision Drivers

- PORT-2: compile once at load into typed values.
- ADR-0023 / ADR-0039: continuous output is rebuilt per pass from stored sources.
- GO-9: identity by `CardID`; a sticker is `(sheet CardID, slot)`, which is Java's identity
  (`StickerSheet.java:268-271`).
- No generic controller method can answer "one of these stickers, or none": `ChooseOption` has no "none" answer and a
  `ConfirmEffect` first adds a decision Java does not make.

## Considered Options

1. Compile each sheet's slots into `compile.Face.Stickers`; store `Card.stickers []AppliedSticker`; each rebuild pass
   re-applies them at their timestamp; two new `PlayerController` methods.
2. Parse sheet SVars when a sticker is chosen. Rejected: PORT-2.
3. Map the choices onto `ChooseOption` plus `ConfirmEffect`. Rejected: an extra decision, and "none" is not expressible.

## Decision

Option 1. `compile.Sticker{Slot, Kind, Tickets, Word, Text, Power, Toughness, Keywords, Abilities, Triggers, Statics}`
on `Face.Stickers`, in `K:StickerSheet:` order; a missing slot SVar or an unknown `Kind$` is skipped silently as in
`StickerSheet.java:294-299`. `ZoneType.StickerSheets` follows Java's declaration order (after `ContraptionDeck`).
`Card.stickers` holds `AppliedSticker{Sheet, Slot, Timestamp, NamePos}`; `put` clears it on a move to a hidden zone.
`PlayerController` gains `ChooseSticker(g, decider, source, target, options []StickerOption, optional) int` (the index,
or -1 for none) and `ChooseStickerNamePosition(g, decider, target, option) int`. Tickets are paid at resolution by
subtracting from the owner's `TICKET` counters (`PutStickerEffect.java:124-126`), not as a cost.

## Consequences

**Good:** the sheets are ordinary compiled data; the rebuild passes stay pure functions of stored state; the Java
identity rule (a sticker used once across public zones) is a scan by `(Sheet, Slot)`. **Bad:** every full
`PlayerController` implementation gains two methods (`ScriptedController`, the smoke controller, one test controller).
**Neutral:** the game-start pick of 3 sheets uses the game's injected `javarand` (R15e).

## Related

ADR-0023, ADR-0039, ADR-0043
