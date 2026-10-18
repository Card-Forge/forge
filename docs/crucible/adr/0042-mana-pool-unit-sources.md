# ADR-0042 — The Mana Pool Records Each Unit's Source Card

- **Status:** Accepted
- **Date:** 2026-10-18
- **Deciders:** Crucible session (M6 R9k)

## Context

`Count$CastTotalManaSpent <valid>` counts the mana spent casting a card whose source card matches a valid string
(`AbilityUtils.java:1899-1907`: `getPayingMana().stream().map(Mana::getSourceCard)` filtered by
`CardPredicates.restriction`). Three corpus cards read it past `Snow`: Bat Colony (`Cave`), Cataclysmic Prospecting
(`Desert`), Spoils of the Hunt (`Treasure`). All three refuse today (`Token: TokenAmount$`, `Pump: NumAtt$` not
resolvable).

`Pool` (`internal/engine/mana.go`) is twelve counters: five colors and `{C}`, each split plain/snow. It knows no source
card, so only `Snow` is answerable (the snow buckets, `xAnnounced.snow`). Java's `ManaPool.floatingMana` is an
`ArrayListMultimap<Byte, Mana>`, each `Mana` holding its `sourceCard`; `tryPayCostWithColor` spends the first unit of a
color in the list (`ManaPool.java:228-250`), so within one color the oldest unit pays first.

A Treasure pays by sacrificing itself: by the time the spell's count is read, its source card has left the battlefield.
Java's `Mana` still points at the battlefield `Card` object, whose types are those it had there.

## Decision Drivers

- GO-9: source by `CardID`, not pointer. GO-12: the order units were added is the order they pay (Java's list order).
- `Game.Clone` and `TestCloneAllocationsStayBounded`: an empty pool (the common state between steps) must cost no
  allocation to clone.
- Every existing payment rule (plain before snow, `ChoosePayGeneric`'s type per generic unit, `payWithSnow`'s
  all-or-nothing) stays exactly as tested.
- Smoke probe, fixtures and tests add mana with no source (`Pool.Add`); they must keep working.

## Considered Options

1. **An ordered unit list beside the counters.** `Pool.units []poolUnit{bucket, source CardID, stamp}`, one per unit, in
   add order; the counters stay the payment authority. A successful payment takes, for each bucket it decremented, that
   many oldest units of the bucket and returns their sources.
2. Replace the counters with the unit list. Rejected: `payWithSnow`, `convertTo`, `Breakdown`, `merge` and the fixture
   dump all read counts; rewriting them over a list changes every tested payment path for no card that needs it.
3. Record only the last mana ability's source per bucket (`lastSource [12]CardID`). Rejected: two Caves and a Mountain
   floating red-red-red pay with whichever was last, not the units actually spent; wrong as soon as two sources of one
   color float together.
4. Count at production time: tag a unit "matches valid X" when added. Rejected: the valid string is the spell's, not
   known when the mana is made, and one pool unit can pay any spell.

## Decision

Option 1. Every unit added through a mana ability or a land's intrinsic ability carries its source card and that card's
zone-change timestamp; `Pool.Add` and the other source-less adders record `NoCard`. `payManaCostX` returns the spent
units' sources (`xAnnounced.sources`), which `payCastCost` stores on the spell (`Card.castPaidSources`, cleared with
`castManaSpent`). `Count$CastTotalManaSpent <valid>` counts those whose source matches: the card itself while its
timestamp is unchanged, else its last-known battlefield snapshot (`Game.LKI`), else the card as it is now. `Empty` drops
the list; `Clone` copies it only when non-empty; `convertTo` and `merge` keep units with their bucket rewritten.

## Consequences

**Good:** `Count$CastTotalManaSpent` answers any valid string, and a later reader of per-unit sources (mana restrictions
such as `RestrictValid$`, `ManaFrom$` AI hints) has the data. A pool that is empty costs nothing extra to clone.

**Bad:** every mana addition from an ability appends to a slice, one allocation per growth step per step's mana, and
every payment walks the list. Within one color Crucible spends plain mana before snow mana, while Java's list mixes both
in add order, so a cast paying from a mixed plain/snow color can attribute a unit to a different source than Java would.
Mana another player pays through Assist records no source, as `xAnnounced.paid` already excludes it.

**Neutral:** fixtures carry no sources (the `GameState` format has none); a fixture-loaded pool pays as `NoCard` units,
which no valid string matches.

## Related

ADR-0009, ADR-0005, [`m6-r9k-cast-mana-sources.md`](../porting/port-log/game-state/m6-r9k-cast-mana-sources.md)
