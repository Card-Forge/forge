# Port Log — Game State: M6 Effects: ControlPlayer

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `controlplayereffect.go`, `scheduledaction.go`,
  `player.go` (`controlledBy`, `controlGrant`), `game.go` (`scheduled`, `Clone`), `turn.go` (three hooks),
  `becomemonarcheffect.go` (`onPlayersLost`), `learneffect.go`, `changezoneeffect.go`
- **Java:** `ControlPlayerEffect.java:27-47`, `Player.java:2517-2566`, `Phase.java:92-146` (`addUntil`, `excute`),
  `PhaseHandler.java:301,515-518,1261-1263,1296-1322`, `Game.java:1014-1017`
- **ADR:** [ADR-0030](../../../adr/0030-controlplayer-scheduled-control-redirect.md)

Supersedes the `ControlPlayer` row of `effects-batch-b.md`'s deferred table; that file is closed, so the row stays as
written there.

## ControlPlayer lands

CR 800.4b: Mindslaver's "you control target player during that player's next turn." `ControlPlayerEffect.java` changes
nothing as it resolves: it adds a command to a future phase boundary that grants control, and the grant schedules its
own revoke at the boundary after. This port queues the same pair as data (ADR-0030).

### What the redirect is

Java's `addController` builds a new `PlayerController` (brain) for the **same** slave player; it never changes who acts,
targets or owns zones. This port has one shared `PlayerController` (`control.go`), so there is no brain to swap. The
redirect is read-only state; every `PlayerID` the engine passes stays the slave.

| Piece                      | Java                                                             | Go                                                                                         |
| -------------------------- | ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| Grant stack                | `Player.controlledBy`, `TreeMap<Long, Pair<Player, Controller>>` | `Player.controlledBy []controlGrant{Timestamp, Controller}`, oldest first                  |
| Newest grant decides       | `getControllingPlayer` → `lastEntry()`                           | `Game.ControllingPlayer(pid)`, `NoPlayer` when none                                        |
| Controlled by someone else | `isControlled`: controller non-null and not self                 | `Game.IsControlled(pid)`                                                                   |
| Revoke                     | `removeController(ts)`                                           | `removeControlGrant`: removes that timestamp only, so CR 800.4b's next-most-recent decides |
| Controller leaves the game | `Game.onPlayerLost` → `removeController(p)` for every player     | `releaseControlBy`, called from `onPlayersLost`                                            |
| Timestamp                  | `game.getNextTimestamp()`                                        | `g.timestamp++`, `uint64` (`Game.timestamp`'s own type)                                    |

### Scheduling

`scheduledAction{Kind, At, ForPlayer, Target, Controller, UntilEndOfCombat, Timestamp}` on `Game.scheduled`. `At` is the
Java `Phase` the command was added to; `ForPlayer` its per-player list (`NoPlayer` = the unkeyed `until` list). Not
merged into `delayedTrigger`: that carries a `compile.Ability` resolved through `Registry.Resolve` (PORT-2).

| Boundary              | Java call                                                                 | Go call site                                       | Holds                                 |
| --------------------- | ------------------------------------------------------------------------- | -------------------------------------------------- | ------------------------------------- |
| `boundaryCleanup`     | `getCleanup().executeUntil()` then `executeUntil(playerTurn)`, `:515-518` | `advanceStep`, turn wrap, after `nextActivePlayer` | turn grant (keyed), revokes (unkeyed) |
| `boundaryBeginCombat` | `getBeginOfCombat().executeUntil(playerTurn)`, `:301`                     | `beginStep` `CombatBegin`, both modes              | `Combat$` grant (keyed)               |
| `boundaryEndCombat`   | `getEndOfCombat().executeUntil()`, `endCombat` `:1262`                    | `Game.endCombat` itself                            | `Combat$` revoke (unkeyed)            |

`endCombat` hosts the revoke, not the `CombatEnd` step, so `EndCombatPhase` and `EndTurn` (which call `endCombat`
directly, as Java's `endCombatPhaseByEffect` does) revoke too.

`runScheduledActions` runs the unkeyed batch, then the batch keyed to the active player — Java's "do this first for
ControlPlayer" order. Each batch is taken out of the list before any of it runs (`Phase.excute` copies first), so a
grant's own revoke waits for the next boundary. Cruel Entertainment's mutual pair depends on both: as the second turn
begins, the first revoke runs, then the second grant, whose revoke must not fire at once.

The delayed-trigger pair at the same boundary runs keyed before unkeyed (`delayedTriggersOnNextTurn`, then
`activateCleanupDelayedTriggers`): harmless there, both only flip flags, so `runScheduledActions` keeps its own order
rather than riding theirs.

A grant is skipped when its controller has left the game (`ControlPlayerEffect.java:36`). At the turn boundary an action
keyed to a lost player is dropped: Java runs it from `handleMultiplayerEffects` (CR 800.4m) over a player nobody can
observe.

### Shapes

| Param                          | Corpus | Resolution                                                                                                 |
| ------------------------------ | ------ | ---------------------------------------------------------------------------------------------------------- |
| `ValidTgts$ Player`/`Opponent` | 9      | one grant per targeted player (`targetedOrDefinedPlayers`)                                                 |
| `Defined$`                     | 2      | `definedPlayers`; `Targeted` reads the parent's targets                                                    |
| `Controller$`                  | 2      | first of `definedPlayers`, default `You` (`AbilityUtils.java:920`); empty → error, Java's `.get(0)` throws |
| `Combat$ True`                 | 1      | grant at the target's next beginning of combat, revoke at end of combat                                    |

Every real line's own `ControlPlayer` shape resolves except the rejected ones below. Whole card, from the real script:

| Card                                          | End to end  | Why                                                                                                                            |
| --------------------------------------------- | ----------- | ------------------------------------------------------------------------------------------------------------------------------ |
| Mindslaver                                    | yes         | `TestMindslaverControlsTheTargetFromTheRealScript`, scenario below                                                             |
| Sorin Markov (-7)                             | yes         | `TestSorinMarkovUltimateControlsTheTarget`                                                                                     |
| Worst Fears                                   | no          | grant scheduled, then `DBChange`'s `Origin$ Stack` errors in `changezoneeffect.go` (pre-existing gap)                          |
| Emrakul, Urza, Cosmic Cube, Dominion Bracelet | not checked | host mechanism (SpellCast trigger, `GenericChoice AtRandom$`, `ImmediateTrigger`, granted ability) is its own effect's concern |

Tests: `playercontrol_test.go`.

### Rejected with an error before acting

| Param                                                   | Line                                 | Reason                                                                                                       |
| ------------------------------------------------------- | ------------------------------------ | ------------------------------------------------------------------------------------------------------------ |
| `Condition$ OptionalCost`, `ConditionOptionalPaid$`     | Secret of Bloodbending (both halves) | no optional-cost record; `subAbilityConditionMet` would skip silently. `Combat$` itself is ported and tested |
| `TargetUnique$`                                         | Cruel Entertainment                  | `targeting.go` does not enforce it; its head `ChoosePlayer` never reaches the stack anyway                   |
| `Controller$ ParentTarget`, `Controller$ Player.Chosen` | Cruel Entertainment                  | `definedPlayers` has no case; errors through its default branch                                              |

Cruel Entertainment also needs a sub-ability's own `ValidTgts$` chosen (`subability.go`: not targeted separately). Its
mutual shape is tested through two `Defined$`/`Controller$` lines instead
(`TestControlPlayerMutualPairRevokesBeforeGranting`).

## Readers: a controlled player cannot reach outside the game

Java has exactly two rules readers of the redirect; both now read `IsControlled`.

| Reader | Java                                                        | Go                                                                 |
| ------ | ----------------------------------------------------------- | ------------------------------------------------------------------ |
| Learn  | `Player.java:3906`, no sideboard Lessons                    | `learneffect.go`: hand cards only                                  |
| Wish   | `ChangeZoneEffect.java:987-991`, `origin.remove(Sideboard)` | `changeZoneHidden`: per-fetcher copy of `Origin$` less `Sideboard` |

The Wish reader needed `Origin$ Sideboard` itself, which `changezoneeffect.go` rejected. Now:

| Change                           | Java                                                                          | Reason                                                                           |
| -------------------------------- | ----------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| `Sideboard` allowed in `Origin$` | `ZoneType.listValueOf`                                                        | the 26 real `Origin$ ...Sideboard` lines (24 cards)                              |
| `Sideboard` routes hidden        | `SpellAbility.isHidden:2644-2650`; `Sideboard(true, ...)`, `ZoneType.java:23` | a line without `Hidden$` still searches and chooses                              |
| Exclusion per fetcher, on a copy | `:919-991` order: target substitution, `isInGame`, `Optional$`, then removal  | outer `Origin$` still drives the library-shuffle checks and the next fetcher     |
| `Defined$` fetch ignores it      | `:1000-1011` reads `getDefinedCards`, not `origin`                            | parity: `Defined$ ChosenCard` from Sideboard still moves for a controlled player |

20 of those 26 lines now resolve (`ChangeZone` was registered already; API count unchanged). The other 6 stay rejected:
`Exactly$` 2, `Chooser$` 2, `AtRandom$` 1, `ConditionDefined$` 1. Still silently ignored, pre-existing: `Reveal$`, the
`fetchList.sort()` at `:1097`. A resolving line is not a working card: the six sorcery Wishes (Burning, Cunning, Death,
Glittering, Golden, Living) chain `SubAbility$ DBChange | Origin$ Stack | Destination$ Exile`, which
`changezoneeffect.go` still rejects, so each fails loudly after its fetch; the other 18 cards carry no such sub-ability.
Tests: `controlledoutsidegame_test.go`; scenario `controlplayer-mindslaved-player-cannot-wish-from-sideboard` (real
Mindslaver, then ai's Coax from the Blind Eternities on the controlled turn searches Exile only).

## Not ported

| Piece                                 | Reason                                                                                                                                                                                                                                                                                                                        |
| ------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `GameEventPlayerControl`              | UI event only (`Player.java:2545`); ADR-0013 has no kind for it and ADR-0030 adds none                                                                                                                                                                                                                                        |
| `clearController` on game over        | `Game.java:583`: nothing reads control after the game ends                                                                                                                                                                                                                                                                    |
| `clearController` on restart          | `RestartGameEffect.java:74` belongs to that effect's port: clear every `Player.controlledBy`, and drop `Game.scheduled` entries at `boundaryCleanup`/`boundaryEndCombat` (`:49`, `:51` clear those `Phase` lists). `:47-51` leaves `getBeginOfCombat()`'s list alone, so a pending `Combat$` grant survives a restart in Java |
| Control state in `GameState` fixtures | Forge's `GameState` format has no key for it; the scenario observes it through the Wish instead                                                                                                                                                                                                                               |
| Per-seat brain routing                | engine does none (ADR-0030); a harness reads `Game.ControllingPlayer`                                                                                                                                                                                                                                                         |
