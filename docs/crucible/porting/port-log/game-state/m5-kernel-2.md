# Port Log — Game State: M5 rules-kernel leftovers (batch I)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Previous:** [`m5-kernel.md`](m5-kernel.md)
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `match.go`, `setstateeffect.go`, `stack.go`,
  `triggerpack.go`, `scryeffect.go`, `surveileffect.go`, `amountheads.go`

## Flip (CR 709)

Java: `Card.changeCardState("Flip")` (`Card.java:716-745`), `getFaceupCardStateName` (`:4326`). `SetState Mode$ Flip`
resolves. No ADR: a flip card reuses the transform swap (`Card.frontDef` holds the unflipped `Def`), so no new state
model exists to decide on.

| Case | Result |
| --- | --- |
| flip card, on its first flip | `Def` becomes the flipped face; no timestamp, no `Transformed` |
| already flipped | `false`, nothing happens (CR 709.4, one-way) |
| face-down, copying, or card without a flipped face | `flipped` flag only, `Def` unchanged (`Card.java:727-741`) |
| leaves the battlefield | `turnFrontFaceUp` restores the card and clears `flipped` |
| fixture `\|Flipped` (`GameState.java:351,1349`) | dump writes it for `InFlippedState`; load calls `Game.Flip` |

`PrintedDef` returns the unflipped definition for a flipped card, so the dump names the paper card, as Java does. Not
ported: a flip card turned face up from face down does not show its flipped face (no face-down flip card exists in the
corpus). Scenarios `flip-akki-lavarunner-flips-into-tok-tok-after-damaging-the-opponent`,
`flip-akki-lavarunner-blocked-stays-unflipped`, `flip-loaded-flipped-tok-tok-deals-one-extra-damage-to-the-opponent`;
`flip_test.go`.

## Match series and the starting player

Java: `Match.isMatchOver`/`getGamesWonBy`/`getWinner` (`Match.java:125-160`), `GameAction.determineFirstTurnPlayer`
(`GameAction.java:2385-2447`). `engine.Match` holds outcomes only, no `Game` (GO-2): seats are `PlayerID`s, `Record`,
`GamesWon`, `IsOver`, `Winner`, `LastLoser` (first seat that did not win the last game; the first seat after a draw).
`DetermineFirstTurnPlayer(g, controller, m, puzzle)` follows Java's order:

| Rule | Starting player |
| --- | --- |
| Puzzle | first seat, no question (the game type is a parameter: a `Game` does not record it) |
| Archenemy | the player with a non-empty `SchemeDeck` (`Player.isArchenemy`), no question |
| Power Play | owner of one in the Command zone; several owners: seat order shuffled, `Collections.shuffle` |
| game one | `ChooseStartingPlayer` asked of the coin-flip winner (`g.rand`), `isFirstGame` true |
| later | asked of `Match.LastLoser`, `isFirstGame` false |

Power Play deviation: Java shuffles a `HashSet` copy, whose order has no meaning; seat order replaces it. Tests:
`match_test.go` (a fixture cannot express a series).

## Ongoing scheme replacements in the Command zone

`R:` lines with `ActiveZones$ Command` already reach Command-zone scheme cards (`replacementZones` walks Battlefield and
Command). Scenarios `scheme-ongoing-nothing-can-stop-me-now-prevents-1-of-combat-damage`,
`scheme-ongoing-nothing-can-stop-me-now-does-not-protect-its-opponent` pin Nothing Can Stop Me Now.

## Ascend, instants and sorceries

`Game.ascendAtResolution` (`stack.go`, `AbilityUtils.resolvePreAbilities`, `AbilityUtils.java:1338`): a resolving
non-permanent spell whose card has Ascend gives its controller the blessing at ten permanents, before its amounts are
read; a fizzled spell never gets there. `Count$Blessing.<with>.<without>` (`AbilityUtils.java:2269`) resolves for plain
numbers. Scenarios `ascend-sorcery-resolving-with-ten-permanents-draws-three`,
`ascend-sorcery-resolving-with-nine-permanents-draws-two`. Sneak's declare-blockers-only timing stays unported: no Sneak
cast exists.

## Scry and Surveil trigger params

| Param | Result |
| --- | --- |
| `Scry ToBottom$` | fires only if the scry put at least one card on the bottom |
| `Surveil FirstTime$` | fires only on the player's first surveil this turn (`Player.surveilThisTurn`) |
| `TurnFaceUp ValidCause$` | matched against the causing ability; a special action has none, so it never matches |
| `ScryNum`/`ScryBottom` | `TriggerCount$` reads cards put anywhere / on the bottom (`triggerCounts.scryNum`) |
| empty library | Scry and Surveil still count and fire (`GameAction.java:2605-2655`, `Player.java:1083`) |

Tests: `scrytriggerparams_test.go`, `triggerpack_test.go`.
