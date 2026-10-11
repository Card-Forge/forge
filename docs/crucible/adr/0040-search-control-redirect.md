# ADR-0040 — Search Control: a Per-Effect Decision Redirect for Library Searches

- **Status:** Accepted
- **Date:** 2026-10-08
- **Deciders:** Crucible session (M5 batch S)

## Context

`ControlOpponentsSearchingLibrary$ You` (Opposition Agent, 1 corpus line) hands an opponent's library search to the
Agent's controller. Java:

- `StaticAbilityContinuous.java:531-534` stores `getDefinedPlayers(...).getFirst()` in the affected player's
  timestamp-keyed `controlledWhileSearching` map (`Player.java:2575-2587`, newest timestamp wins);
  `StaticEffect.java:198` removes it when the effect ends.
- `ChangeZoneEffect.java:1070-1076` (and `ChooseCardEffect.java:243-251` for `QuasiLibrarySearch$`): when the searching
  player is the decider, `player.addController(controlTimestamp, searchControlPlayer.getValue())` pushes a **real
  control grant** keyed by the static's timestamp; `:1268-1269` pops it after the choice. For that window
  `getController()` is the Agent controller's brain, which sees the library and picks the card.

ADR-0030 fixed the principle (a redirect changes whose brain answers, never who acts) and built the grant stack
`Player.controlledBy` read by `Game.ControllingPlayer`. ADR-0036 fixed the shape for a static that names a player: a
`PlayerID` on `RulesEffect`, rebuilt every pass. Neither covers a redirect that exists only for the length of one
effect's choice.

## Decision Drivers

- ADR-0030: `PlayerID` stays the acting player; the engine owns one `PlayerController`, so there is no brain to swap.
- GO-2: no package-level state; `applyContinuousRules` already rebuilds `RulesMod` each pass.
- Parity: Java picks the grant by **timestamp**, so a Mindslaver grant newer than the Agent still wins during the
  search.
- PORT-8: a param that never reaches its effect is a bug, not a skip.

## Considered Options

State:

1. **`RulesEffect.SearchControl PlayerID`**, newest `Timestamp` wins, rebuilt each pass (ADR-0036's shape).
2. A timestamp-keyed map on `Player` with add and remove, as Java. Rejected: the rebuild pass re-derives the value, so a
   map needs a removal hook Crucible does not otherwise have.

Window:

1. **Push a `controlGrant` for the search, keyed by the static's timestamp, pop it after the choice.** Reuses ADR-0030's
   stack; `ControllingPlayer` and `IsControlled` answer correctly inside the window with no new reader.
2. A separate `Game.searchController` field set around the choice. Rejected: a second redirect store; every reader of
   `ControllingPlayer` would have to learn about it.
3. A `searcher` parameter on `PlayerController.ChooseCardsForEffect`. Rejected: every controller and fixture changes
   signature for a redirect no scripted test needs (ADR-0036's reason).

## Decision

State option 1, window option 1. `rulesEffect` reads `ControlOpponentsSearchingLibrary$` through `definedPlayers`;
`Game.SearchController(pid)` returns the controller and the static's timestamp. `changeZoneHidden` pushes the grant
after the fetch list is built (so `isControlled`'s Wish exclusion, read before, is unaffected, `:987-991`) and pops it
after the choice, only when the library was searched: `Library` in `Origin$`, no `NoLooking$`, the decider passes
`canSearchLibraryWith`, or `Searched$`. The grant is inserted in timestamp order, not appended, so `controlledBy[len-1]`
stays the newest grant and a later Mindslaver grant outranks it. `ChooseCard`'s `QuasiLibrarySearch$` takes the same
grant (`m5-t-replace-search.md`).

## Consequences

**Good:** Opposition Agent's control half stops being a silent skip; the redirect composes with ControlPlayer grants by
timestamp exactly as Java's `TreeMap.lastEntry` does; no interface break. **Bad:** a harness that wants the Agent's
controller to choose must read `Game.ControllingPlayer` inside `ChooseCardsForEffect`; the engine does not reroute for
it. The Agent's `FoundSearchingLibrary$` replacement (exile what the opponent finds) is a separate Moved replacement and
is not part of this decision (`m5-t-replace-search.md`). **Neutral:** the grant consumes no `Game.timestamp`; it borrows
the static's.

## Related

ADR-0030, ADR-0036, [`m5-s-redirects.md`](../porting/port-log/game-state/m5-s-redirects.md)
