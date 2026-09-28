# Effects: Subgame lands

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `subgameeffect.go` (effect, subgame build and
  start), `mulligan.go` (`startingLife`, `drawOpeningHand`), `defined.go` (`Defined$ Player.<property>`), `valid.go`
  (player property `IsRemembered`); [`internal/fixture`](../../../../../crucible/internal/fixture) — `actions.go`
  (`queue cardchoice`)
- **Scenario:** `crucible/testdata/scenarios/subgame-enter-the-dungeon-winner-searches`

Batch file for `Subgame`, CR 720. Supersedes `Subgame`'s deferred row in `effects-batch-b.md` (closed file, left as
written). No ADR: the subgame is a second `*Game` driven by the existing turn driver (`Game.Run`, ADR-0026); no new
engine state, no new `PlayerController` method.

---

## Subgame lands, a nested game through the turn driver

Ported from `forge-game/src/main/java/forge/game/ability/effects/SubgameEffect.java`'s `resolve`, `createSubGame`,
`setCardsInZone`, `prepareAllZonesSubgame`, with `GameAction.startGame` (`GameAction.java:2322-2382`) as `startSubgame`.
Corpus: 3 real `SP$ Subgame` lines — Shahrazad, Enter the Dungeon, The Countdown Is at One — all naming
`RememberPlayers$`, two naming `StartingLife$`.

| Step               | Go                                                                                                           | Java                                                      |
| ------------------ | ------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------- |
| Seats              | main-game players not `Lost`, seat order; subgame seat `i` answers for `seats[i]`                            | `maingame.getPlayers()` (`:34-36`)                        |
| New game           | `NewGame(g.db, g.rand, names)`, `registry` = main's                                                          | `new Game(players, rules, match, maingame, startingLife)` |
| Starting life      | `StartingLife$`, else `startingLife` (20, CR 103.3)                                                          | `Game.java:347-351`, `RegisteredPlayer.java:25`           |
| Library            | copy of each main-library card (`NewCard(card.Def)`), same order                                             | `setCardsInZone(Library)` (`:96`)                         |
| Sideboard          | copies of owned cards in Hand, Battlefield, Graveyard, Exile, Stack, Sideboard, Ante, Merged, phased-out too | `getCardsInOwnedBy(outsideZones)` (`:100-102`, CR 720.4)  |
| Variant decks      | Attraction/Contraption copied                                                                                | `:111-121`                                                |
| Skip               | tokens, copied spells, effect cards                                                                          | `isToken() \|\| isCopiedSpell()` (`:45`)                  |
| Shuffle            | per seat: library, then each variant deck                                                                    | `:126-130`                                                |
| First player       | `rand.Int32n(seats)` decider, `ChooseStartingPlayer(sub, decider, true)`                                     | `determineFirstTurnPlayer(null)` (`GameAction.java:2384`) |
| Hands, mulligans   | `drawOpeningHand` (no second shuffle), `PerformMulligans`                                                    | `drawCards(startingHandSize)`, `MulliganService`          |
| Play               | `sub.StartTurn(first)`, `sub.Run(g.registry, c, subgameTurnCap)`                                             | `startFirstTurn`, `mainGameLoop`                          |
| Outcome            | `Player.Won` on the subgame; none won = draw, every seat `NotWin`                                            | `outcome.isWinner` (`:153-172`)                           |
| `RememberPlayers$` | `Win` / `NotWin` main players remembered on host                                                             | `:174-181`                                                |
| After              | each seat's main Library shuffled, then each existing variant deck                                           | `player.shuffle(sa)`, deck shuffles (`:240-244`)          |

**Nested `Game.Run` works as-is.** The subgame is a separate `*Game` value; `subgame.Run` inside the main game's
`Resolve` is an ordinary call, not re-entry into the main game's own `priorityRound`/`Step`. Nothing in the driver keys
off shared state: the controller takes `*Game` per call, so the same `PlayerController` answers both games; an error
inside the subgame (`TestSubgameErrorInsideTheSubgamePropagates`) returns through `Resolve` wrapped `engine: Subgame:`.
Main game untouched until the subgame ends: its libraries hold the same `CardID`s afterwards, only reshuffled.

Design calls:

| Decision                          | Why                                                                                                                                                                                                                                                                                 |
| --------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Shared `rand` pointer             | Java's `MyRandom` is one stream across both games: the subgame's shuffles and coin flip advance the sequence the main game continues from. Two `*Game` values on one goroutine for one call — not package-level state (GO-2)                                                        |
| Subgame sink `DiscardSink`        | Subgame `CardID`s index their own arena; an event naming one in the main stream would name the wrong card. Java's `GameEventSubgameStart/End` are UI refreshes                                                                                                                      |
| `subgameTurnCap = 1000`, an error | Java has no cap (`PhaseHandler.java:1032-1037`). The main driver stops only between its steps, and the whole subgame is one of them, so an unbounded subgame would hang a batch. A capped subgame is an error, not a guessed draw (GO-7). Cap clears a 250-card Battle of Wits deck |
| Seats by index                    | `NewGame` hands out `PlayerID`s 1..n in seat order; with a lost main player, subgame seat 2 answers for main seat 3 (`TestSubgameSeatsOnlyPlayersStillInTheGame`)                                                                                                                   |
| `startingLife` constant           | No `RegisteredPlayer`: the variants that raise it (Commander, Archenemy, Vanguard) have no setup here. Same reasoning as `startingHandSize`                                                                                                                                         |
| No `Game` field for "is subgame"  | The Sideboard-exit rejection is a post-run check of each Sideboard copy's `zoneStamp`, so `Game.Clone` is untouched                                                                                                                                                                 |

Rejected, each an `error` naming the shape:

| Shape                                               | When                                               | Reason                                                                                                                                                                                                                                                                          |
| --------------------------------------------------- | -------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Condition$`/`ConditionDefined$`                    | before acting                                      | `subAbilityConditionMet` would pass them silently                                                                                                                                                                                                                               |
| `RememberPlayers$` other than `Win`/`NotWin`        | before acting                                      | Java ignores it silently (`:174-181`); no corpus line                                                                                                                                                                                                                           |
| `StartingLife$` not an integer                      | before acting                                      | Java `Integer.parseInt` throws                                                                                                                                                                                                                                                  |
| Main-game `SchemeDeck`/`PlanarDeck` nonempty        | before acting                                      | `startGame` puts the archenemy first (`GameAction.java:2393-2400`) and turns a plane up (`:2358-2363`); this port's game start has neither                                                                                                                                      |
| Companion among outside cards                       | before playing                                     | `Player.assignCompanion` (`Player.java:3109`) checks deck restrictions and asks the controller; no companion setup here                                                                                                                                                         |
| Card taken from the subgame Sideboard (Wish, Learn) | after the subgame, before the main game is touched | CR 720.4a: `GameAction.java:815-824` moves the mapped main-game card to the `Subgame` zone, then library (`SubgameEffect.java:205-212`). Needs card identity across two arenas (`addMaingameCardMapping`); no real line reaches it except through a deck that also plays a Wish |
| Subgame still running at the turn cap               | after 1000                                         | See design calls                                                                                                                                                                                                                                                                |
| Starting player outside the subgame's seats         | at start                                           | controller answer, not a script shape; error, not panic                                                                                                                                                                                                                         |

Nothing to port: Vanguard avatars and Commanders (`initVariantsZonesSubgame`) — this port has neither, so the main
Command zone never holds one; the post-game commander move (`:214-238`) and `Subgame`-zone return (`:205-212`) always
find nothing. `Merged` is scanned, always empty (`Mutate` unported). Opening-hand actions (`runOpeningHandActions`) and
`Mode$ NewGame` have no port in the main game's start either; neither runs here. `Player.shuffle(sa)`'s `Mode$ Shuffled`
has no trigger mode in this port.

Real cards end to end:

| Card                    | Follow-up                                                                              | Status                                                                                                                                                                            |
| ----------------------- | -------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Enter the Dungeon       | `RepeatEach` → `ChangeZone` `DefinedPlayer$ Player.IsRemembered`                       | resolves; scenario `subgame-enter-the-dungeon-winner-searches`                                                                                                                    |
| The Countdown Is at One | `Effect` `RememberObjects$ Player.IsRemembered`, damage-doubling `ReplacementEffects$` | resolves (`TestSubgameCountdownChain`)                                                                                                                                            |
| Shahrazad               | `RepeatEach` → `LoseLife` `LifeAmount$ X`, `X:PlayerCountRemembered$LifeTotal/HalfUp`  | `Subgame` resolves; `LoseLife` errors `LifeAmount$ "X" is not resolvable` — `amount.go` resolves only `Count$Valid*`, the `PlayerCount*` family is a separate gap (43 real lines) |

Tests: `internal/engine/subgame_test.go`.

---

## `Defined$ Player.<property>` and the player `IsRemembered` property

Every real `Subgame` follow-up names its players `Player.IsRemembered` (153 real lines: 139 `Defined$`, 11
`DefinedPlayer$`, 3 `RememberObjects$`). Two pieces:

| Piece                          | Go                                                                            | Java                                                                             |
| ------------------------------ | ----------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| `Defined$ Player.<property>`   | `definedPlayers` default branch: every player filtered by `matchesPlayerSpec` | `getDefinedPlayers` fallthrough plus restriction (`AbilityUtils.java:1186-1198`) |
| Player property `IsRemembered` | `matchesPlayerProperty`: host's `Memory.Remembered` holds the player          | `PlayerProperty.java:209-212`, `source.isRemembered(player)`                     |

Supersedes the `NewController$ Player.IsRemembered` half of Sudden Substitution's row in `effects-controlspell.md`
(closed file, left as written): that `Defined$` now resolves; the card still fails closed on its `ConditionDefined$`.
Only a single property resolves. A comma list (`Player.A,B`) or a property `matchesPlayerProperty` does not recognize
(`Player.withMostLife`, `Player.Chosen`) stays `Defined$ "..." not resolvable yet`. `definedPlayers` joins `valid` in
its enginelint allow list for `matchesPlayerSpec`.

The new generic branch iterates players in seat order; a rules review caught that this and the pre-existing `"Player"`/
`"Opponent"` cases both ignored `ReverseTurnOrder` (Java's own fallthrough reads `game.getPlayersInTurnOrder()`,
`AbilityUtils.java:1188`). Fixed for all three: `Game.playersInTurnOrder()` (`game.go`) reverses `Players()` when
`turnOrderReversed` is set, used by all three `definedPlayers` cases now. Rotation to start from the active player, if
Java's own callers ever depend on that rather than just direction, is not modeled — no real corpus caller found needing
it.

`IsRemembered` becoming a recognized `matchesPlayerSpec` property reaches every one of that function's ~35 callers, not
only `Defined$` and the replacement side: trigger `ValidTarget$`/`ValidPlayer$` (`trigger.go`), `targeting.go`
(`ValidTgts$` naming a player property), `continuous.go`'s own affected-player filters, `delayedtrigger.go`, and every
other site that was silently skipping or erroring on `Player.IsRemembered` before. `MustAttack$ Player.Other` and
`MustAttack$ Player.IsRemembered` (4 real lines, `effects-mustblock.md`) resolve now for the identical reason:
`definedEntities` reaches `definedPlayers`' own generic branch the same way every other `Defined$` caller does. Only
`Defined$`/`DefinedPlayer$`/`RememberObjects$`, the replacement side and `MustAttack$` have tests here; the rest match
Java by construction (one shared `matchesPlayerSpec`), not by a test per call site.

Scenario harness: `queue cardchoice <id>[,...]` answers `ChooseCardsForEffect` (`game-state-fixture.md` grammar).
