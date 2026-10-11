# Port Log — Game State: M5 batch H, replacement remainder

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `replacementchoice.go`, `replacementfaces.go`,
  `replacementamounts.go`, `replacementevents.go`, `replacement.go`

Siblings: [`m5-replacement.md`](m5-replacement.md) (CR 616 ordering, first `Event$` values).

## Draw substitutes raise a new Draw event

Java: `Player.drawCards` runs the `Draw` replacement per card; a `ReplaceWith$ DB$ Draw` ability draws through the same
path, so another Draw replacement sees the substituted draw. `ReplacementHandler.run` keeps the applied effect in
`hasRun` for the length of its own resolution (`ReplacementHandler.java:227-274`).

| Go                                         | Role                                                                             |
| ------------------------------------------ | -------------------------------------------------------------------------------- |
| `Game.drawEvent` (`turn.go`)               | one draw: cantDraw, Prevent$, CR 616 walk, draw. `DrawCards`, Draw/GainLife subs |
| `Game.replacing` (`game.go`)               | `hasRun` set while a `ReplaceWith$` ability resolves; never cloned               |
| `runReplacements` (`replacementchoice.go`) | skips candidates in `Game.replacing`; pushes/pops around `apply`                 |

Why: Thought Reflection's own line must not recheck itself, but Notion Thief must see each substituted draw. Reason for
a `Game` field, not a parameter: the substitute ability resolves through nested `Registry.Resolve` calls.

| Scenario                                                           | Proves                                                 |
| ------------------------------------------------------------------ | ------------------------------------------------------ |
| `replacement-draw-616-thought-reflection-first-then-notion-thief`  | TR first: two new draws, each diverted by Notion Thief |
| `replacement-draw-616-notion-thief-first-skips-thought-reflection` | Notion Thief first: TR never sees the draw             |

## Optional$ replacements: `ConfirmReplacementEffect`

New `PlayerController.ConfirmReplacementEffect(g, decider, host, description)`, Java `confirmReplacementEffect`
(`ReplacementHandler.executeReplacement`). One caller: `confirmOptionalReplacement` in `runReplacements`.
`OptionalDecider$` resolves through `definedPlayers`. A declined candidate counts as applied (not offered again) and
changes nothing. A nil controller declines. Queue verb: `queue confirmreplacement <bool>`.

`BeginPhase` and `BeginTurn` now run through `runBeginReplacements` (CR 616 walk): `Skip$ True` or a `ReplaceWith$`
ability (Fasting's gain 2, Time Vault's untap) replaces the event. Draw's allow-list gained
`Optional$`/`OptionalDecider$` (Pursuit of Knowledge).

| Scenario                                                            | Proves                            |
| ------------------------------------------------------------------- | --------------------------------- |
| `replacement-optional-fasting-skips-the-draw-step-and-gains-life`   | confirmed: no draw step, +2 life  |
| `replacement-optional-fasting-declined-draws-a-card`                | declined: the draw happens        |
| `replacement-optional-time-vault-skips-the-turn-and-untaps`         | BeginTurn skip, substitute untaps |
| `replacement-optional-pursuit-of-knowledge-counter-instead-of-draw` | Draw replaced by a STUDY counter  |
| `replacement-optional-pursuit-of-knowledge-declined-draws`          | declined Draw replacement         |

## TurnFaceUp and Transform

Java runs both replacements after the face changed (`Card.turnFaceUp`, `Card.changeCardState`), so `ReplaceWith$` is an
"as it is turned face up" step on the card in its new state. `canBeTurnedFaceUp` is the `Layer$ CantHappen` gate
(`cantHappenCheck`). Ported: `Game.canBeTurnedFaceUp`, `Game.faceChangeReplaced`, `Game.runReplacementChain` (resolves a
`ReplaceWith$` ability and its `SubAbility$` chain through the Registry; `playSpellAbilityNoStack` resolves the chain,
unlike the old `runReplaceWithEffect` refusal, which stays for DeclareBlocker). `SetState` no longer refuses when a
Transform replacement is in play. The valid properties `faceDown`/`faceUp` were missing from `valid.go`, so
`Creature.faceDown` matched nothing (Ixidor's target).

| Scenario                                                               | Proves                                                                        |
| ---------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `replacement-turn-face-up-hooded-hydra-gets-five-counters`             | `ReplaceWith$ PutCounter` after the flip                                      |
| `replacement-turn-face-up-crowd-control-warden-counts-other-creatures` | `X` read after the flip                                                       |
| `TestTurnFaceUpCantHappenHoldsOnlyDuringTheHostControllersTurn`        | Karlov Watchdog shape (Go: a face-down card can't be named in `expect.state`) |

Not done: Transform has no scenario (the four real lines need Zenos, Sephiroth, Curse of Leeches or Ludevic on the
battlefield and a transform source), `Attached` (3) and `RollDice` (4) have no dispatch site, `TurnFaceUp` lines run
through the Registry and record a pending error if the effect refuses; Gift of Doom and Vesuvan Shapeshifter resolve
([`m5-t-replace-search.md`](m5-t-replace-search.md)).

## Scry, Mill, DrawCards

`playerAmountReplaced` (`replacementamounts.go`): one CR 616 walk per player before the action, number under
`Num`/`Number`. `ReplaceEffect` updates it (Kenessos, Bruvac, The Water Crystal, Quantum Riddler); any other
`ReplaceWith$` replaces the event (Eligeth's draw, Alms Collector). `replaceCountAmounts` pre-resolves `ReplaceCount$`
SVars and inline values to literals so an ordinary effect reads them. `Number$ GE2` gates a line. Callers: `scryEffect`,
`millEffect`, `Game.DrawCards`. `definedPlayers` now splits `A & B` (`getDefinedPlayers`).

| Scenario                                                | Proves                                |
| ------------------------------------------------------- | ------------------------------------- |
| `replacement-scry-kenessos-scries-one-more`             | scry 1 becomes 2                      |
| `replacement-scry-eligeth-draws-instead-of-scrying`     | event replaced, no scry decision      |
| `replacement-mill-bruvac-mills-twice-as-many`           | `Twice`                               |
| `replacement-mill-the-water-crystal-mills-four-more`    | `Plus.4`                              |
| `replacement-draw-cards-alms-collector-splits-the-draw` | `Number$ GE2`, `You & ReplacedPlayer` |
| `replacement-draw-cards-quantum-riddler-draws-one-more` | `CheckSVar$`, inline `ReplaceCount$`  |

## Destroy without Regeneration

`Game.destroyInstead`: a `ReplaceWith$` line (Harmonious/Crackling Emergence: sacrifice the Aura, the land gains
indestructible) replaces the destruction. Called before `regenerate` in `destroyEffect`, `destroyAllEffect` and the
lethal-damage SBA, so `NoRegen$` does not skip it (Java runs the replacement regardless).

Scenarios: `replacement-destroy-harmonious-emergence-is-sacrificed-instead`,
`replacement-destroy-crackling-emergence-is-sacrificed-instead`.

## Moved to the battlefield: tapped, untapped, day

`checkMovedReplacement` collects every resolvable line (own faces, grants, watchers) instead of stopping at the first.
Shapes: `DB$ Tap` (ETBTapped family), `DB$ Untap | Defined$ ReplacedCard` (Gond Gate, Spelunking, Wandering Minstrel,
Archelos: 6 lines), `DB$ DayTime` (10 lines). `DayTime$ Neither/Day/Night` is a common requirement now. One state kind
alone applies the first line (idempotent, as before, no controller question). Tap and untap together go through
`runReplacements`: the affected player picks the order and the last one applied wins (CR 616.1). A `DB$ Tap` whose
condition fails no longer untaps a card that entered tapped for another reason.

| Scenario                                                     | Proves                      |
| ------------------------------------------------------------ | --------------------------- |
| `replacement-moved-wandering-minstrel-untapped-applied-last` | choice 0: tap, then untap   |
| `replacement-moved-wandering-minstrel-tapped-applied-last`   | choice 1: untap, then tap   |
| `TestMovedDayTimeReplacementMakesItDayOnlyWhenNeither`       | `DayTime$ Neither`, `DoDay` |

Not done: `PayBeforeETB`/`SacBeforeETB` (9), `Exile` (Containment Priest, Primeval Spawn: the entry has happened by the
time `enterBattlefieldReplacements` runs), the "enters with counters" family (needs `PutCounter ETB$`),
`PayLife`/`LoseLife` (4), `Layer$ Control` (`DBChooseOpp`, 3), Amulet-style `ChooseP`/`ChooseCT`.

## ChangesZoneAll names the real zone

`checkChangesZoneAllTriggers` regroups a `Graveyard` batch by each card's actual zone (Exile, Library, Hand, Sideboard,
Command): Rest in Peace's exile fires an Exile batch, not a Graveyard one.

| Scenario                                                                    | Proves                    |
| --------------------------------------------------------------------------- | ------------------------- |
| `replacement-changes-zone-all-ghoulish-procession-sees-the-graveyard`       | graveyard batch fires     |
| `replacement-changes-zone-all-rest-in-peace-exile-is-not-a-graveyard-batch` | exile: no graveyard batch |

## Guile's Counter line

`Game.counterReplaced` (`replacementevents.go`): `Event$ Counter` with `ReplaceWith$` (`ValidSA$`, `ValidCard$`,
`ValidCause$ SpellAbility[.YouCtrl|.OppCtrl]`) takes the spell off the stack and runs the ability with the spell as the
replaced card. `ChangeZone` allows `Origin$ Stack` for that card. `counterCantHappen` no longer errors on such a line.

| Scenario                                                   | Proves                               |
| ---------------------------------------------------------- | ------------------------------------ |
| `replacement-counter-guile-exiles-the-spell-and-plays-it`  | exile, then Play without paying      |
| `replacement-counter-guile-exiles-the-spell-play-declined` | the spell stays in its owner's exile |
