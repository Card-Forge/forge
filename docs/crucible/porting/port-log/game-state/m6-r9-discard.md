# Port Log — Game State: Discard targets and modes (R9)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `discardeffect.go` (`discardEffect`,
  `discardChoice`, `discardValidResolvable`), `defined.go` (`definedPlayers` `TriggeredTarget`), `trigger.go`
  (`damageSourceObjects`), `valid.go` (`ChosenColor`)
- **Java:** `forge-game/src/main/java/forge/game/ability/effects/DiscardEffect.java:119-294`,
  `SpellAbilityEffect.java:313-350` (`getPlayers`), `SpellAbilityEffect.java:917-946` (`discard`)

## Who discards

| Shape                                    | Discarders                         | Java                               |
| ---------------------------------------- | ---------------------------------- | ---------------------------------- |
| `ValidTgts$` (`TargetMin$`/`TargetMax$`) | the ability's target players       | `getTargetPlayers`, line 125       |
| `Defined$` only                          | `definedPlayers`, default `You`    | `getPlayers(false, "Defined", sa)` |
| `Mode$ RevealTgtChoose`                  | `Defined$` first, else the targets | `getDefinedPlayersOrTargeted`      |

`targetedOrDefinedPlayers` (`defined.go`) is the existing `getTargetPlayers` mirror; `resolveTargets` (`targeting.go`)
already chose the targets and read `TargetMin$`/`TargetMax$` before the ability resolves. 171 smoke refusals named
`ValidTgts$` (Mind Rot, Thoughtseize, Hymn to Tourach, ...).

## What each discards

| `Mode$`                      | Lines | Cards                                                        | Chooser                             |
| ---------------------------- | ----- | ------------------------------------------------------------ | ----------------------------------- |
| `TgtChoose`                  | 813   | `min(NumCards$, valid)` of the hand matching `DiscardValid$` | the discarder                       |
| `Hand`                       | 110   | the whole hand                                               | none                                |
| `RevealYouChoose`            | 82    | as `TgtChoose`                                               | the activator                       |
| `Random`                     | 59    | `randomSample` over the valid hand cards                     | none (game RNG)                     |
| `Defined`                    | 25    | `DefinedCards$` (`definedCards`)                             | none                                |
| `RevealDiscardAll`           | 13    | every valid hand card                                        | none                                |
| `LookYouChoose`, `YouChoose` | 7, 2  | as `TgtChoose`                                               | the activator                       |
| `RevealTgtChoose`            | 1     | as `TgtChoose`                                               | the first target (Rakdos Augermage) |

Counts are real `(AB|SP|DB)$ Discard` lines (1,112 total). Bounds for the choose family are
`DiscardEffect.java:258-259`: `Optional$` or `AnyNumber$` lowers the minimum to 0, `AnyNumber$` raises the maximum to
every valid card. `RevealNumber$` first has the discarder pick that many hand cards (`ChooseCardsForEffect`, clamped to
the hand as `PlayerControllerHuman.java:1804-1811` clamps), and the chooser picks only among those. `Optional$` on
`Hand`, `Random` and `Defined` asks the discarder (`ConfirmEffect`). `NumCards$` defaults to 1 and is clamped to the
hand only when present (lines 177-181), so `Random` with none draws one card.

Reveal and Look change nothing: the engine has no hidden zone (the same reading as `revealhandeffect.go`).

## Controller decisions

No new `PlayerController` method. Reason: every choice fits an existing contract.

| Decision                                                                      | Method                 | Why                                                                                        |
| ----------------------------------------------------------------------------- | ---------------------- | ------------------------------------------------------------------------------------------ |
| `TgtChoose` with no `DiscardValid$`/`Optional$`/`AnyNumber$`/`RevealNumber$`  | `ChooseCardsToDiscard` | the discarder's whole hand, exact count: its contract; keeps existing scripts and fixtures |
| every other pick (`YouChoose` family, filtered, optional, any number, reveal) | `ChooseCardsForEffect` | lo..hi over an offered pool, re-checked by `checkChoice` (GO-7)                            |
| `Optional$` confirm                                                           | `ConfirmEffect`        | Java's `confirmAction` for an `Optional$` param                                            |
| graveyard order of two or more cards                                          | `OrderCardsForZone`    | `GameActionUtil.orderCardsByTheirOwners` on every path                                     |

## Order of events

Java collects every player's cards into `discardedMap` first and discards after (line 291), so a later player's choice
sees the earlier hands as they were; the port does the same (`discardPick`). `discardedMap` is a `HashMap<Player, ...>`
and `Player.hashCode` is its id (`GameEntity.java:407`), so Java walks it in ascending id: the picks are sorted by
`PlayerID`. Per player: each card still in a hand (`Card.canBeDiscardedBy`, `Card.java:6940-6946`) moves to the
graveyard and fires `Discarded`; `RememberDiscarded$` remembers it (`Player.java:1445-1447`). Then `DiscardedAll` runs
once per player. `RememberDiscardingPlayers$` remembers every player that reached `discardedMap` — a `Hand` discarder
with an empty hand included, a choose-mode discarder who picked nothing excluded (lines 266-268).

`discardCards` stays the cost-side helper (`Discard<N/Card>`).

## `DiscardValid$` that would read false

`filterValid` reads an unported property as false for every card, which would discard nothing silently.
`discardValidResolvable` checks the corpus's `DiscardValid$` values before anything moves:

| Property                                      | Handling                                                                                                                                                                          |
| --------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ChosenColor` (Addle, Persecute, Crosis)      | now a `valid.go` case: `CardStateProperty.java:58-59`, the source's chosen color; refused when more than one color is chosen, since `Memory` keeps a set and Java reads the first |
| `SharesColorWith <defined>` (Mind Extraction) | refused: no evaluator for the suffixed form                                                                                                                                       |
| a comparison operand (`cmcEQX`)               | refused when `compareOperand` cannot resolve it                                                                                                                                   |

Every other property the 99 `DiscardValid$` lines name (`nonLand`, `NamedCard`, `IsRemembered`, `ChosenCard`,
`ChosenType`, `sharesNameWith`, type names, colors) already has a case.

## `Defined$ TriggeredTarget` for a player

`TriggerDamageDone.setTriggeringObjects` records the damaged object as `AbilityKey.Target`
(`TriggerDamageDone.java:115`). `damageSourceObjects` now records it (`CardEntity` or `PlayerEntity`), and
`definedPlayers` answers `TriggeredTarget` with it when it is a player, as `AbilityUtils.getDefinedPlayers`' `Triggered`
branch does. 50 `Discard` lines read it (Abyssal Specter, Hypnotic Specter, Barbed Shocker); `definedCards`' existing
`TriggeredTarget` now also answers a damaged creature.

A DamageDone trigger whose target restriction is `TargetsWithDefinedController$ TriggeredTarget` (`targeting.go`) now
finds its candidates and goes on the stack. Ink-Eyes, Zombie Cannibal, Skullsnatcher and the other `ChangeZone` cards
play ([`m6-r9-pump-kw.md`](m6-r9-pump-kw.md)); Somnophore fails in `Effect` `Duration$`.

## Not ported

| Param                       | Lines | Reason                                                                                                                     |
| --------------------------- | ----- | -------------------------------------------------------------------------------------------------------------------------- |
| `UnlessType$`               | 15    | `chooseCardsToDiscardUnlessType`: N cards, or fewer if one has the type; `ChooseCardsForEffect`'s lo..hi cannot express it |
| `RememberDiscardingPlayer$` | 0     | never read by Java; rejected rather than ignored                                                                           |
| `Defined$ X & Y`            | 3     | `getPlayers` splits on `" & "`; `definedPlayers` does not                                                                  |
| `Player.canDiscardBy`       | —     | `StaticAbilityCantDiscard`: no `Mode$ CantDiscard` static is ported, so nothing can make it false                          |

## Tests

`discardmodes_test.go` (every mode, targets, owner order, `Optional$`, `AnyNumber$`, `RevealNumber$`, picks before
discards, refused shapes), `discardeffect_test.go`, and scenarios
`discard-duress-caster-picks-a-noncreature-nonland-card`, `discard-amnesia-target-discards-every-nonland-card`,
`discard-addle-caster-picks-a-card-of-the-chosen-color`, `discard-hypnotic-specter-damaged-player-discards-at-random`.
