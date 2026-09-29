# ADR-0032 — Meld: Two Cards, One Permanent

- **Status:** Accepted
- **Date:** 2026-09-27
- **Deciders:** Crucible session (M6 porter round)

## Context

`Meld` (CR 712, 7 corpus lines) is deferred (`effects-batch-b.md`): "two cards become one permanent" — a `SplitMeld`
card's own back face is the melded result (`Brisela, Voice of Nightmares` is Gisela's own back face), entered by
`MeldEffect.java` exiling both named cards, transforming the primary to its `CardStateName.Meld` state
(`primary.changeToState`), and marking the secondary as folded into it (`primary.setMeldedWith(secondary)`,
`PlayerZoneBattlefield.addToMelded(secondary)`) before moving the primary onto the battlefield. CR 712.4c: if the melded
permanent ever leaves the battlefield, it splits back into its two original cards, each going to its owner's appropriate
zone.

`SplitMeld` is one of Crucible's own recognized `SplitType` values (`carddb/card.go`, the same enum family as
`SplitTransform`), and `compile.Card` compiles every face of every card with no exemption (M3). A meld card's back face
compiles exactly like a transform DFC's back face — `compile.Card.Faces[1]` already holds it. Crucible already has an
established, repeated pattern for "swap `Card.Def` to a different compiled face, remember the old one to swap back":
`setstateeffect.go` (`Mode$ Transform`: `c.frontDef, c.Def = front, back`), `cloneeffect.go` (`c.uncopiedDef`),
`facedown.go` (`c.faceUpDef`). **The face-swap half of `Meld` is not novel** — it reuses this precedent directly.

**What is novel:** a second, distinct `Card` object (the secondary) that stops being its own permanent and instead rides
along inside the primary's identity while melded, then un-folds back into two cards the moment either leaves the
battlefield (CR 712.4c). Crucible's `Card` model has no "this card is folded into that other card" relationship at all
today.

## Decision Drivers

- GO-9: identity by `CardID` — the secondary keeps its own `CardID` throughout; nothing re-numbers or merges IDs.
- Reuse over invention: the face-swap uses the existing `Def`-swap precedent exactly as `SetState`/`Clone` do; only the
  two-card pairing is new state.
- PORT-2: no new card-state is parsed at runtime — the meld face is already a compiled `Faces[1]`, resolved at load.
- CR 712.4c parity: splitting back on any zone change away from the battlefield is not optional cleanup, it is the rule
  — both halves must return to real zones, not vanish or leave one half stranded.

## Considered Options

**Where the secondary "lives" while melded:**

1. Move the secondary to `Exile` and give it a `MeldedWith CardID` back-pointer, treating its battlefield presence as
   purely notional (only the primary's card object is ever really "on the battlefield").
2. Keep the secondary's own `Card` object's `Zone` field reading `Battlefield` (matching Java's own
   `PlayerZoneBattlefield.addToMelded`), but hold it out of the zone's card collection entirely — Java's own
   `addToMelded` never calls the zone's normal `add`, only a separate `meldedCards` list `getCards()` never reads, so
   nothing needs a `Melded` flag checked at each enumeration site the way `IsDesignationCard` needs one for
   `BecomeMonarch`'s synthetic card.

**Splitting back apart (CR 712.4c):**

1. A duration-tracked effect (like `Game.pumps`), checked at cleanup.
2. A zone-move hook: whichever of `Game.Move`'s call sites already handles a card leaving `Battlefield` also checks
   `c.MeldedWith`/`c.Melded`, and if set, reverts the primary's `Def` (`frontDef` swap-back, `SetState`'s own precedent)
   and moves the secondary to its own owner's appropriate zone in the same call.

## Decision

**Secondary's location: Option 2, corrected to match Java's actual mechanism.** `PlayerZoneBattlefield.addToMelded`
(`PlayerZoneBattlefield.java:45-49`) does not add the secondary to the zone's own card collection at all: it removes the
card from its old zone's collection, points `Card.setZone(this)` at the battlefield zone object directly, and adds it
only to a separate `meldedCards` list `getCards()`/`contains()`/`size()` never read. So the secondary's `Zone` field
reads `Battlefield` (matching Java, not a fiction moved to `Exile`), carrying a new `Melded bool` field, but it is a
member of no `Zone`'s card set at all — not present-and-skipped, simply absent. Every walk that enumerates "the cards
actually on the battlefield" (`Game.Zone(Battlefield, pid).Cards()` and its ~84 real call sites,
`CardsIncludingPhasedOut`, the fixture dump) needs no added check at any of them: the secondary was never in the set
they read, the identical "invisible without a per-site skip" property `IsDesignationCard` reaches only with an explicit
check at each site. The primary gets `MeldedWith CardID` (`NoCard` = not melded) naming the secondary.

**Face swap: reuses `SetState`'s existing precedent**, no new mechanism. The primary's `Def` swaps to its own `Faces[1]`
(the meld face), `frontDef` holding the original for the revert.

**Splitting apart: Option 2.** Reverting is not duration-tracked — it is triggered by the same zone-move code paths that
already exist for a card leaving `Battlefield` (both `Game.Move` and `Game.MoveToLibraryTop`, not `Move` alone), at the
point a melded primary's `Zone` changes away from `Battlefield`. That code checks `MeldedWith`; if set, it un-swaps the
primary's `Def` (`frontDef` restore) and moves the secondary (currently `Melded`, in no zone's own collection) to its
owner's zone matching wherever the primary is now headed (CR 712.4c: both components go to the zone the event would have
sent a single permanent to — e.g. both to their owners' graveyards on a destroy, or the secondary follows the primary on
top of the library). The secondary's own `Melded` flag clears in the same step.

**Corpus scope.** `MeldEffect.java`'s own `Tapped$` param and its exile-then-check-both-still-there gate (tokens and
copies are explicitly excluded, `c.isToken() || c.getCloneOrigin() != null`) are the dominant real shape; anything past
that is rejected as unresolved per the usual PORT-8/GO-7 discipline, decided at implementation time against the real 7
corpus lines.

## Consequences

**Good:** unblocks `Meld`'s 7 corpus lines by composing two things this port already has (the `Def`-swap precedent, a
synthetic-card-skip precedent) rather than inventing a third card-representation mechanism. `compile.Card`'s
already-compiled meld face means no new compile-time work.

**Bad:** the secondary being absent from its own zone's card collection is a sharper edge than a checked flag would be —
any future code that reaches a melded secondary by means other than `MeldedWith`/`Melded` (a raw `CardID` held
elsewhere, say) will find it reporting `Zone == Battlefield` while nothing that walks `Zone(Battlefield, ...).Cards()`
ever surfaces it, a real trap for a reader who assumes those two facts agree the way they do for every other card. The
zone-move hooks touch shared, heavily-called paths (`Game.Move`, `Game.MoveToLibraryTop`); the implementing commit must
keep the added check cheap (an early return for the overwhelmingly common non-melded case) and additive, not restructure
either function.

**Neutral:** `Melded`/`MeldedWith` are new `Card` fields, plain values, carried by `Game.Clone` for free like every
other such field.

## Related

ADR-0009 (arena/ID identity), `setstateeffect.go`/`cloneeffect.go`/`facedown.go` (the reused `Def`-swap precedent),
`becomemonarcheffect.go`'s `IsDesignationCard` (the reused synthetic-card-skip precedent),
`docs/crucible/porting/port-log/game-state/effects-batch-b.md` (`Meld`'s prior deferred row, superseded here).
