# Porter plan: Planeswalk (ADR-0029)

Scratch file for whoever resumes this branch. Deleted in the final commit.

## Assigned

- `Planeswalk` (`PlaneswalkEffect.java`), 32 corpus `DB$ Planeswalk` lines.
- Plumbing per ADR-0029: `Game.activePlane CardID`, `Game.planechaseActive bool`, trigger modes `PlaneswalkedFrom` /
  `PlaneswalkedTo`.

## Approach

1. Engine state (game.go + Clone): `activePlane`, `planechaseActive` (set by `NewCard` when a card is created into a
   `PlanarDeck` zone = "PlanarDeck non-empty after setup"). Accessors `ActivePlane`, `SetActivePlane`,
   `PlanechaseActive`.
2. Trigger modes in trigger.go: `checkPlaneswalkedFromTriggers` / `checkPlaneswalkedToTriggers`, walking Battlefield +
   Command hosts, zone-gated like Mode$ Phase; `Static$ True` lines resolved inline via `resolveStaticTriggers`
   (ADR-0020), others pushed through `pushTriggeredAbilities`.
3. `planeswalkeffect.go`: not-Planechase no-op; `Optional$` via `ConfirmEffect`; live `Event$ Planeswalk` replacement ->
   error; leave current plane (every player, From triggers before the move, plane to owner's PlanarDeck bottom); top of
   activator's PlanarDeck -> activator's Command, set active, To triggers. Reject `Defined$` and `DontPlaneswalkAway$`
   (both need concurrent planes, which the single-field state cannot hold).
4. Tests (`planechase_test.go`), coverage >= 90%.
5. Docs: `effects-planeswalk.md`, index row, trigger-modes note.
6. Gates full, commit.

## Status

- [x] 1 state
- [x] 2 trigger modes
- [x] 3 effect
- [x] 4 tests
- [x] 5 docs
- [x] 6 gates
