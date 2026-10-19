# Port Log — Game State: ChangeZone `Duration$ UntilHostLeavesPlay`, `Chooser$`, `OriginAlternative$` (R9)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `changezoneeffect.go` (`changeZoneDuration`,
  `untilReturns`, `armUntilHostLeaves`, `changeZoneChooser`, `hiddenChooser`, `alternativeOrigin`, `changeZoneKnown`,
  `changeZoneHidden`), `controlcommands.go` (`commandReturnExiled`, `cardCommand.Zone`, `returnUntilCards`),
  `zonemove.go` (`moveByEffect`), `action.go` (state-based pass), `game.go` (`Game.pendingReturns`, `Clone`)
- **Java:** `ChangeZoneEffect.java:448-461`, `:530-566`, `:865`, `:910-1100`, `:1286-1539`;
  `SpellAbilityEffect.java:833-915` (`changeZoneUntilCommand`), `:954-1034` (`addUntilCommand`), `:1036-1064`
  (`checkValidDuration`); `CardZoneTable.java:100-141`; `Card.java:2179-2191` (`cleanupExiledWith`);
  `GameAction.java:769-781`

## What refused

`crucible card-smoke -all -workers 8` refused 263 cards on these three params: `Duration$` 100, `Chooser$` 118,
`OriginAlternative$` 45. The top-causes table shows 97, 92 and 44: it counts the `resolve` stage only, not refusals at
`turns` or `activate`. Corpus `ChangeZone` lines by shape:

| Param | Lines | Shape |
| --- | --- | --- |
| `Duration$` | 112 | All `UntilHostLeavesPlay`. 104 `Origin$ Battlefield` → `Exile` (O-Ring: Banishing Light, Banisher Priest); 8 `Origin$ Hand` (Kitesail Freebooter, Brain Maggot) |
| `Chooser$` | 245 | Almost all hidden: `DefinedPlayer$ Targeted \| Chooser$ You` ("target opponent reveals their hand, you choose"); 5 known-origin lines |
| `OriginAlternative$` | 62 | All `Origin$ Library \| OriginAlternative$ Graveyard` (or `Graveyard,Hand`): "search your library and/or graveyard for a card named ..." |

## `Duration$ UntilHostLeavesPlay`

Java (`changeZoneUntilCommand`): after the moves, a `GameCommand` goes on the host's leaves-play list; when it runs,
every card of the zone table still the same object (`equalsWithGameTimestamp`) goes back to the zone it came from, with
no cause, under its owner's control (`moveToPlay(c, c.getController())`, an exiled card's controller is its owner). The
returns fire one `ChangesZoneAll` batch (`Game.untilHostLeavesPlayTriggerList`).

| Step | Java | Here |
| --- | --- | --- |
| Gate | `checkValidDuration`: host neither in play nor on the stack → nothing | Same, before the hidden/known split |
| Record | `hostCard.addUntilLeavesBattlefield(non-token moved cards)` | One `commandReturnExiled` per moved non-token card: target, its new `zoneStamp`, origin zone (`Zone`) |
| Arm | `host.addLeavesPlayCommand(gc)` | On `Card.leavesPlayCmds` |
| Host not in play | `gc.run()` at once (`:910-911`, host exiled itself or is a spell) | Queued and run at once |
| Same-object check | `untilCards.contains(c)` and `equalsWithGameTimestamp` | `zoneStamp` unchanged: any later move restamps, and `cleanupExiledWith` only fires on a move |
| Return | `game.getAction().moveTo(origin, card, 0, null, ...)` | `moveByEffect(controller, card, origin, 0, NoPlayer, false)`, then `checkChangesZoneAllTriggers` per zone pair |

**Design call: the return waits for a controller.** Java runs the move inside the host's own zone change. Here the
leaves-play hook runs inside `Game.Move` (`effectCardsSeeMove`), which has no `PlayerController`, and a battlefield
return needs one: `entryReplaced`, `enterBattlefieldReplacements` and `checkETBTriggers` ask questions. So
`commandReturnExiled` is data-only: running it appends to `Game.pendingReturns`, and `returnUntilCards` carries the
queue out at the first point holding a controller.

| Drain point | Covers | Why there |
| --- | --- | --- |
| End of `moveByEffect` | Host moved by an effect (bounce, flicker, destroy-all) | The effect's next step already sees the card back, as in Java: Ephemerate on Brutal Cathar re-targets it |
| Each state-based pass | Host dying to damage, sacrificed as a cost, `Game.Move` | Before any player gets priority; counts as a performed action, so the pass repeats |
| `armUntilHostLeaves` | Host not in play right after the moves | Java's `gc.run()` at `:911` |

Precedent: the lose-control command already leaves its zone correction to the next state-based pass
(`controlcommands.go`). Nothing a player can do happens between the hook and either drain point; only the stack order of
the host's own leaves trigger against the returned card's enter trigger can differ from Java.

New state: `cardCommand.Zone` (the return zone), `commandReturnExiled`, `Game.pendingReturns` (empty between steps;
`Game.Clone` copies it). `ReturnAbility$`/`ReturnValid$` (Lagrella, the Magpie's delayed "return under your control"
trigger, `:878-899`) were not in the refused list because `Duration$` masked them; now refused by name. Any `Duration$`
other than `UntilHostLeavesPlay` stays refused, naming the value.

## `Chooser$`

| Path | Java | Here |
| --- | --- | --- |
| Hidden (`:911-917`) | Activator picks one of the `Chooser$` players; none → each fetcher decides | `hiddenChooser`: one → it; several → `ChoosePlayerForEffect`, answer checked; none → `NoPlayer` |
| Hidden decider (`:919-923`) | `decider = chooser ?: player`, fixed **before** a `ValidTgts$` fetcher is swapped for the target | Same |
| Hidden uses of the decider | Optional$ and alt-zone confirms, `canSearchLibraryWith`, the pick | Same |
| Search control (`:1071`) | `ControlOpponentsSearchingLibrary$` grant only when `decider == player` | Same |
| Known (`:530-546`, `:566`) | First player named (`get(0)`); answers `Optional$`; orders library-bound cards | `changeZoneChooser`; `OrderCardsForZone(chooser)` replaces `orderCardsByTheirOwners`; empty → `error` |

The decider order changes 18 existing lines with `ValidTgts$ Player`/`Opponent`, a library or hand origin, and neither
`DefinedPlayer$` nor `Chooser$` (Bribery, Acquire, Extract, Denying Wind, Praetor's Grasp, Painful Memories, ...): the
activator now picks from the target's zone, which is what 17 of the cards' texts say. The engine asked the target
before. The 18th, Turtle Tracks ("any number of target players may each search their library"), is a script defect: with
no `DefinedPlayer$ Targeted` only the first target searches (`getFirstTargetedPlayer`, `:926`), and the activator picks.

`AlreadyRevealed$` (9 cards newly reached through `Chooser$`) only skips the `DelayedReveal` the decider is shown while
choosing (`:1044`); this port shows nothing, so the param is accepted and changes nothing.

## `OriginAlternative$`

Per fetcher, before `Optional$` (`:939-970`), all asked of the decider through `ConfirmEffect`:

1. "Search `Origin$`?" — no clears it.
2. While alternatives remain and more than one zone is still in play (`origin + alt != 1`): "search this one too?", yes
   adds it.
3. Nothing left → the remaining alternatives are searched.
4. A zone that is not hidden (the graveyard) holding a `ChangeType$` match of the player's makes the pick mandatory, for
   later fetchers too (Java's method-level `mandatory`).

So a declined library searches the graveyard with no second question. The per-fetcher zones flow into the fetch list and
into "if you search your library this way, shuffle" (`hiddenChoice.origin`, `:1522`). An `OriginAlternative$` on a
known-origin move, which Java ignores, is an `error` (no corpus line).

`AlternativeDecider$` belongs to `handleAltDest` (`DestinationAlternative$`, `:1694`), not to this prompt;
`DestinationAlternative$` stays refused.

## Forge defects found (PORT-8)

| Site | Defect | Here |
| --- | --- | --- |
| `ChangeZoneEffect.java:961-963`, `:1014-1016` | Declined library → `origin = [Graveyard]`, no `DefinedPlayer$` → every player's graveyard. Captain Kirk (`Land.Basic`), Kassandra, Shaun & Rebecca, Auditore Ambush can take an opponent's card | Reproduced (oracle parity) |
| `ChangeZoneEffect.java:532` | Known-origin `Chooser$` `get(0)` throws on an empty list; unreached by the corpus | `error` |

Rows in [`forge-java-defects.md`](../../forge-java-defects.md).

## Smoke probe

`crucible card-smoke -all -workers 8`, before → after this change:

| Status | Before | After |
| --- | --- | --- |
| plays | 29,308 | 29,521 |
| refused | 4,214 | 4,001 |
| inconclusive | 198 | 198 |
| judged play | 87.4% | 88.1% |

| Flip | Cards | Cause |
| --- | --- | --- |
| refused → plays | 213 | By old reason: `Duration$` 92 of 100, `Chooser$` 77 of 118 (7 also needed `AlreadyRevealed$`), `OriginAlternative$` 44 of 45 |
| refused, reason changed | 50 | `ChangeZone: ChangeNum$ "_"` 20, `SorcerySpeed$` 4, `PutCounter: TargetMin$` 3, `RememberLKI$` 2, `ChangeZone: Condition$` 2, `Chooser$: Defined$ "_"` 2 (Bane of Bala Ged, Clash of Elements), 17 single causes |
| plays → refused | 0 | |

## Not ported

| Shape | Lines | Reason |
| --- | --- | --- |
| `Duration$` other than `UntilHostLeavesPlay` on `ChangeZone` | 0 | None in the corpus; refused naming the value |
| `ReturnAbility$` / `ReturnValid$` | 1 | Lagrella's per-card delayed `ChangesZone` trigger with an LKI spawning ability |
| `ChangeZoneAll` `Duration$` | — | Separate effect, still refused there |
| Host flickered while its trigger waits (Ephemerate on Banisher Priest) | — | Gap: the ability sees the same `CardID` back on the battlefield and exiles until the new object leaves; CR 610.3 says the old object already left, so nothing should be exiled. An `Ability` carries no host-object stamp (`hostObjectStamp`, `game.go`) |

## Tests

`untilhostleaves_test.go` (return at the state-based pass, inside the same effect, nothing once the host is gone, a card
that moved on, a token, the host exiling itself, a hand card back to hand, refused durations), `searchdecider_test.go`
(Chooser$ on both paths, the activator deciding a targeted search, several and no choosers, the three
`OriginAlternative$` prompt outcomes, refusals). Scenarios
`exile-until-host-leaves-banishing-light-returns-the-bears-when-destroyed`,
`exile-until-host-leaves-banisher-priest-bolted-in-response-exiles-nothing`,
`exile-until-host-leaves-kitesail-freebooter-returns-the-card-to-hand-when-it-dies`,
`origin-alternative-ajanis-aid-skips-the-library-and-takes-ajani-from-the-graveyard`.
