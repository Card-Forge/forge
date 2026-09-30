# RestartGame and ExiledWithSource (ADR-0034)

Karn Liberated's ultimate (`karn_liberated.txt:7`) and the per-card "exiled with" state its
`SubAbility$ ReturnFromExile` reads. Design: `docs/crucible/adr/0034-restartgame-driver-restart-signal.md`.

Supersedes the `RestartGame` row of `effects-batch-d.md`'s deferred table; that file is closed, so the row stays as
written there.

## ExiledWithSource lands

`Card.exiledWith` (`card.go`) is Java's `Card.exiledWith` (`Card.java:326`): which host object exiled the card.

| Piece                       | Where              | Java                                                                                        |
| --------------------------- | ------------------ | ------------------------------------------------------------------------------------------- |
| Mark cleared on zone entry  | `put`/`putFront`   | `Card.cleanupExiledWith` on every move but to the stack (`GameAction.java:576-579`)         |
| Mark set after exile move   | `markExiledWith`   | `SpellAbilityEffect.handleExiledWith` (`SpellAbilityEffect.java:1087-1116`)                 |
| Host object stamp           | `hostObjectStamp`  | `equalsWithGameTimestamp` against the ability's host object (`CardProperty.java:397-411`)   |
| `ExiledWithSource` property | `valid.go`         | `CardProperty.java:397-411`, exact name only                                                |
| Fixture `ExiledWith:<id>`   | `internal/fixture` | `GameState.java:407-410` (dump), `:771-781`/`:1398` (load)                                  |
| `Spell` valid base          | `baseMatches`      | `Card.isSpell` (`Card.java:5500-5502`): instant, sorcery, or an Aura off the battlefield    |
| Last battlefield stamp      | `battlefieldStamp` | the old object an ability keeps after its host moves (LKI copy keeps its `gameTimestamp`)   |
| Setting sites               | six effects        | `ChangeZone`, `ChangeZoneAll`, `Dig`, `DigUntil`, `Heist`, `Airbend` -- Java's own callers  |
| Reject lists lifted         | `playeffect.go`    | `Play`'s `Valid$ Card.ExiledWithSource`; `Clone`'s `cloneUnportedProperties` entry went too |

**Identity is the host object, not the `CardID` alone.** This port's IDs are stable across zone moves, so a plain
`CardID` match over-matches: Java compares `equalsWithGameTimestamp`, and a host that left the battlefield and came back
is a new object that exiled nothing. The mark stores the host's `zoneStamp` at exile time; the property compares it
against the stamp of the host object an ability of the host sees (`hostObjectStamp`, `game.go`):

| Host is in                          | Object an ability sees                       | Reason                                                           |
| ----------------------------------- | -------------------------------------------- | ---------------------------------------------------------------- |
| Battlefield, Stack, Command         | current object (`zoneStamp`)                 | Java's host object is the live one                               |
| elsewhere, left the battlefield     | last battlefield object (`battlefieldStamp`) | Java's ability keeps the old host object, not the moved copy     |
| elsewhere, never on the battlefield | current object, not listed                   | Java adds to `exiledCards` only for a host in play/stack/Command |

The second row is what makes Karn work: the restart shuffles Karn into its library before `ReturnFromExile` resolves,
and Java's `sa.getHostCard()` is still the battlefield-era Karn object whose `exiledCards` list and timestamp match.
`battlefieldStamp` is set as a card leaves the battlefield (`Move`, `MoveToLibraryTop`) because the `Game.lki` snapshot
is taken after `put` has already restamped the card, so its `zoneStamp` is the new zone's. Divergence: an `Ability`
carries no host object of its own, so an ability a moved card's new object activates from its graveyard or hand reads
the old battlefield object too; no real `ExiledWithSource` line is on such an ability (corpus grep of `ActivationZone$`/
`TriggerZones$` Graveyard/Hand/Exile over the 186 `ExiledWithSource` cards: one hit, Altar of the Wretched, whose
graveyard ability does not read it).

`listed` is Java's `exilingSource.addExiledCard(movedCard)` guard (`SpellAbilityEffect.java:1100-1104`): only a host in
play, on the stack or in the Command zone lists the card, and the property requires `source.hasExiledCard(card)` too.
`Game.SetExiledWith` (fixture loading) always lists, as `GameState`'s own `addExiledCard` does.

**Melded partner marked too.** `Move`/`MoveToLibraryTop` now return the melded permanent's other card when they unmeld
one along with the card the caller moved -- `game.go`'s own `unmeld` follows the primary card into the same zone, but
fired no effect-driven path of its own for a caller to hang a second `markExiledWith` on. `moveByEffect` (`zonemove.go`)
propagates that return; all six setting sites check it and mark the partner too when present, matching
`ChangeZoneEffect.java`'s own `handleExiledWith(meld, sa)` call alongside its primary one
(`TestChangeZoneExilingAMeldedPermanentMarksBothHalves`).

**The mark survives onto the stack.** `put` (`game.go`) clears `exiledWith` on every zone entry but the Stack
(`GameAction.java:576-579`'s own `if (!zoneTo.is(Stack))` guard on `cleanupExiledWith`): a card cast or activated
straight out of the exile a host put it in keeps the mark while its own spell or ability is on the stack
(`TestExiledWithSourceSurvivesOntoTheStack`). `putFront` needs no matching guard -- it only ever moves a card onto a
library's top, never the stack.

**Rejected, before acting.**

| Shape                             | Where                  | Reason                                                                      |
| --------------------------------- | ---------------------- | --------------------------------------------------------------------------- |
| `ExiledWithEffectSource$` (3)     | `ChangeZone`           | marks the effect card's own source instead (`SpellAbilityEffect.java:1092`) |
| `ExiledWithSourceLKI` (15)        | `Play` (`playSpecGap`) | reads the exile zone's cards-added-this-turn LKI list, not ported           |
| `ExiledWithEffectSource` property | `Play` (`playSpecGap`) | effect-card source comparison, not ported                                   |

Not set: cost exiles (`CostExile`, `CostExileFromStack`, `CostBeholdExile`, `CostForage`, `CostCollectEvidence` all call
`handleExiledWith`) and the casting-time sites (`PlaySpellAbility.java:534-535`, `CostAdjustment.java:274-275`, craft
`Card.java:1571-1572`). This port's exile costs are self-exile only (`exile.go`, `exilefromgrave.go`), so the mark Java
sets there names the card itself; a later `ExiledWithSource` check by that same card reads false here. Casting-time
sites have no port to hang the mark on (no Adventure/craft casting).

`Spell` as a card base read false for every card before (`baseMatches`' old coverage-gap case); it now follows
`Card.isSpell` for every card valid string `Matches` evaluates, not only RestartGame's carve-out. The corpus's `Spell.`
valid strings mostly sit on ability-side params (`Valid$ Spell.Creature` on mana and cost restrictions), which a
different evaluator reads; on a card, Java's own `Card.isValid` gives the same answer this now does.

## RestartGame lands (ADR-0034)

`restartgameeffect.go`, `RestartGameEffect.java`. One real corpus line, `karn_liberated.txt:7`:
`RestrictFromZone$ Exile | RestrictFromValid$ Card.!ExiledWithSource,Spell,Card.Aura | SubAbility$ ReturnFromExile`.

**Resolution order** is Java's, per player in seat order (`game.getPlayers`, lost players skipped):

1. Player reset: life to `startingLife` (`setStartingLife`), player counters, spells cast, lands played this and last
   turn, cards drawn, descended, ventured, life-gain count, turns to skip, completed dungeons, Ring temptation count and
   bearer (`RestartGameEffect.java:61-74`).
2. New library, in `restartZones` order (`:29-30`): cards the player controls on the battlefield (phased out included),
   then library, graveyard, hand and exile, skipping the `RestrictFromZone$` zone; then that zone's cards matching
   `RestrictFromValid$` (default `Card`) with the player as the valid string's controller (`:76-80`).
3. Each to the top of its owner's library (`moveToLibrary(c, 0)`, `:90-94`), intensity reset; a card already there is
   moved to the top without a zone change. Then the player's library shuffles (`:98`).

Game-wide, before the loop: delayed triggers, extra phases and turns, the stack, monarch, initiative, day/night, combat,
prevention shields, skipped phases, exile play grants, the previous-turn record; every Command-zone card leaves for
`None` (`exileEffect`) -- effect, designation (monarch, initiative, the Ring) and dungeon cards alike (`:39-57`, `:88`).
After: turn order unreversed, turn 0, `activePlayer` the activator, `Game.restarted`/`restartedBy` set (`:104-107`).
`SubAbility$ ReturnFromExile` then resolves against that state in the same resolution.

**No triggers fire during the reset.** Java suppresses `ChangesZone` and `Shuffled` (`:42-44`, `:101-102`). This port
moves through `Game.Move`/`Game.Shuffle`, which fire none (only `moveByEffect` does); every effect card is gone before
any card moves, so no effect card's own zone watch (`effectCardsSeeMove`) sees one either. No replacement effect runs on
the moves; Java's `moveToLibrary` does consult `ReplaceMoved`.

**Driver restart signal.** Java leaves its game loop (`GameStage.RestartedByKarn`, `PhaseHandler.java:1034`,
`:1150-1155`) and reruns `GameAction.startGame`'s loop body (`GameAction.java:2326-2381`). Here:

| Entry point                             | On a restart during it                              | Called while restarted    |
| --------------------------------------- | --------------------------------------------------- | ------------------------- |
| `resolveTop`                            | returns right after `AbilityResolved`, no SBA check | --                        |
| `priorityRound`/`PassPriority`          | ends the round                                      | `errRestartPending`       |
| `Step`/`Run`                            | return nil, `Over()` false                          | `errRestartPending`       |
| `ResolveStack`                          | stops, rest of the stack kept                       | `errRestartPending`       |
| `ResumeAfterRestart` (new)              | --                                                  | the resume; else an error |
| Subgame driver, fixture `resumerestart` | resume and run on                                   | --                        |

`ResumeAfterRestart` (`driver.go`) is Java's loop body for a restart: each live player draws an opening hand
(`drawOpeningHand`, no shuffle), London mulligans from the activator, flag cleared, `StartTurn(activator)`. Not
`DealOpeningHands`: that flips a coin, asks `ChooseStartingPlayer` and shuffles again, three random-stream and
controller calls Java's restart never makes (`first` carries over as the activator, `GameAction.java:2380`). Skipping
SBA between the reset and the new opening hands is Java's order too: its next `checkStateEffects` is after mulligans
(`:2366`), so a token shuffled into a library can be drawn and then ceases to exist.

Anything `ReturnFromExile` put on the stack -- a returned permanent's own enters trigger -- waits through the restart
and resolves in the new game's first priority window (the activator's upkeep). Java instead collects it in the trigger
handler's waiting list and puts it on the stack at that same first priority check.

**Rejected, before anything changes (PORT-8, GO-7):**

| Shape                                          | Reason                                                                                                   |
| ---------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| A card in any Stack zone                       | `restartZones` has no Stack; `MagicStack.reset` (`MagicStack.java:97-109`) clears entries, not the cards |
| A Command-zone card neither effect nor dungeon | commander to library and `Player.initVariantsZones` rebuild, neither ported                              |
| Planechase game, any card in a variant deck    | `initVariantsZones` (`Player.java:2918`) rebuilds planar/scheme/attraction/contraption decks             |
| `RestrictFromZone$` outside `restartZones`     | 0 real lines; Java would still add that zone's matching cards to the library                             |

`NewGame` triggers are not built: every real one (5 lines) has `TriggerZones$ Command` on a vanguard/conspiracy-style
card, which the Command-zone rejection already refuses, so `ResumeAfterRestart` skipping the trigger drops nothing. The
Stack rejection is unreachable for Karn's line (a loyalty ability needs an empty stack to activate); a card left on the
Stack zone would be a Java gap, not modelled. `Ultimate$` is not rejected: it feeds only `AchievementTracker`
(`AchievementTracker.java:23`).

Not reset, no counterpart here: commander stats, city's blessing, `runPreOpeningHandActions`/`runOpeningHandActions`
(not ported for a normal game start either), `GameEventGameRestarted` (no event kind; the new game's `TurnBegan` turn 1
follows).

**`ControlPlayer` integration (ADR-0030), added on merge.** `resetPlayerForRestart` clears `p.controlledBy` per player
(`p.clearController()`, `RestartGameEffect.java:74`) and `resetForRestart` drops `g.scheduled` entries at the cleanup
and end-of-combat boundaries -- but not the begin-of-combat one, reproducing a real Forge bug (`forge-java-defects.md`):
`RestartGameEffect.java:47-51` clears five phase command lists, never `game.getBeginOfCombat()`'s, so a pending
`Combat$` grant survives a restart in Java too.
`TestRestartGameClearsActiveControlGrant`/`TestRestartGamePreservesAPendingBeginCombatGrant` pin both halves.

Karn's +4 (`Chooser$ Targeted`) is still rejected by `ChangeZone`'s own unresolved-param list; the -3 exiles and marks.

**Tests.** `restartgame_test.go` (real Karn from the corpus: the whole ultimate, both seatings, an earlier Karn's
exiles, every driver entry point, a returned permanent's trigger in the new game, the no-carve-out default, each
rejection, an ability still waiting under the resolving `RestartGame` discarded with the old game rather than resolved,
and a delayed "next upkeep" trigger made before the restart never firing in the restarted game's first upkeep),
`exiledwithsource_test.go`, `internal/fixture/exiledwith_test.go`, and the scenario
`testdata/scenarios/restartgame-karn-ultimate-returns-what-karn-exiled` (`resumerestart` through `RunActions`).
