# Effects: Planeswalk lands (ADR-0029)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `planeswalkeffect.go`, `trigger.go`
  (`checkPlaneswalkedFromTriggers`, `checkPlaneswalkedToTriggers`), `game.go` (`activePlane`, `planechaseActive`)
- **Decision:** [ADR-0029](../../../adr/0029-planechase-active-plane-state.md)

Batch file for `Planeswalk`, the first of the four Planechase APIs ADR-0029 unblocks. Supersedes `Planeswalk`'s deferred
rows in `effects-batch-a.md`, `effects-batch-b.md` and `effects-batch-d.md` (closed files, left as written).

---

## Planeswalk lands

CR 901's planeswalk. Ported from `forge-game/src/main/java/forge/game/ability/effects/PlaneswalkEffect.java`'s
`resolve`, with `Player.planeswalk`/`planeswalkTo`/`leaveCurrentPlane` (`Player.java:2641-2679`) inlined. Resolution
order is Java's:

| Step                   | Go                                                                                                                | Java                                                         |
| ---------------------- | ----------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------ |
| Planechase gate        | `!g.planechaseActive` → no-op                                                                                     | `game.getActivePlanes() == null` → return (`:23`)            |
| `Optional$`            | `controller.ConfirmEffect(activator)`; declined → no-op                                                           | `confirmAction` (`:27-30`)                                   |
| Planeswalk replacement | any live `Event$ Planeswalk` replacement (`eachReplacement`) → error, nothing moved                               | `ReplacementType.Planeswalk` run (`:32-37`)                  |
| Leave                  | every non-lost player in seat order: `PlaneswalkedFrom` fires, then their plane → its owner's `PlanarDeck` bottom | `for p : game.getPlayers() p.leaveCurrentPlane()` (`:39-43`) |
| Walk                   | top of activator's `PlanarDeck` → activator's `Command`; `activePlane` set; `PlaneswalkedTo` fires                | `activator.planeswalk(sa)` (`:47`)                           |

Top of a `PlanarDeck` is index 0 and `Game.Move` appends to the bottom, the `Library` convention (`MoveToLibraryTop`'s
doc comment); Java's `getZone(PlanarDeck).get(0)` and `moveTo(ZoneType.PlanarDeck, plane, -1)` match.

### Engine state (ADR-0029)

| Piece                                          | Shape                                                                                                                                                                                                                                               |
| ---------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Game.activePlane CardID`                      | `ActivePlane()`, `SetActivePlane(id)` (setup/fixture injection, moves nothing). `NoCard` = none. Planar controller = `Card(ActivePlane()).ZoneOwner`; no `Player` field                                                                             |
| `Game.planechaseActive bool`                   | `PlanechaseActive()`. Set by `NewCard` the first time setup creates a card in any `PlanarDeck`, never unset: "any `PlanarDeck` non-empty after setup" without a finalize call, since `NewCard` is setup-only and real play only `Move`s cards there |
| `Clone`                                        | copies both (value fields); `TestClonePlanechaseState`                                                                                                                                                                                              |
| Unexported fields, exported accessors/injector | engine convention (`monarch`/`Monarch()`/`SetMonarch`); the ADR's `Game.ActivePlane`/`Game.PlanechaseActive` names are the accessors                                                                                                                |

### `Mode$ PlaneswalkedFrom` and `Mode$ PlaneswalkedTo` land

`TriggerPlaneswalkedFrom`/`TriggerPlaneswalkedTo.performTest` are identical: `ValidCard$` against `AbilityKey.Cards`,
nothing else. One shared walk, `planeswalkTriggerMatches` (`trigger.go`):

- **Hosts:** every player's `Battlefield` and whole `Command` zone. A face-up plane is an ordinary `Command` card, not
  an effect card, so `traitHosts` misses it. `TriggerZones$` gates each line like `Mode$ Phase`
  (`phaseTriggerZoneMatches`; effect cards `Command` only). A plane in the `PlanarDeck` never fires
  (`TestPlanesInThePlanarDeckDoNotTrigger`).
- **`ValidCard$`:** matched against the card list, true if any card matches (`anyPlaneMatches`,
  `CardTraitBase.matchesValid`'s `Iterable` branch). Empty list matches no `ValidCard$` line; a line without one fires
  regardless — Java runs `PlaneswalkedFrom` once per player, plane or not (`Player.java:2670-2672`),
  `TestPlaneswalkedFromWithoutValidCardFiresOncePerPlayer`.
- **Timing:** `PlaneswalkedFrom` checked before the plane moves, so a `TriggerZones$ Command` "planeswalk away from
  CARDNAME" still sees it there; `PlaneswalkedTo` after arrival.
- **`Static$ True`** (5 of 72 real lines, all on effect cards a plane or phenomenon made — Chaotic Aether's
  `TPWAway: ... ExileSelf | Static$ True`): resolved inline through `resolveStaticTriggers` (ADR-0020) before the other
  matches are pushed, the `checkTapsForManaTriggers` shape. A static line that fails fails the planeswalk
  (`TestPlaneswalkStopsOnAStaticTriggerError`).
- **Triggering objects:** none recorded. Java sets only `AbilityKey.Cards`; no ported `Defined$` reads `TriggeredCards`,
  and no real `Mode$ Planeswalked*` body reads it either.

Corpus: 72 real `Mode$ Planeswalked*` lines (62 `PlaneswalkedTo`, 10 `PlaneswalkedFrom`); `ValidCard$ Card.Self` (53),
`Plane.Self` (13), `Plane` (1), none (5). All reach this walk; their `Execute$` bodies resolve or fail by their own
APIs.

### Corpus: 28 of 30 real `DB$ Planeswalk` lines resolve

| Shape                                                                   | Lines | Status                                                                                                 |
| ----------------------------------------------------------------------- | ----: | ------------------------------------------------------------------------------------------------------ |
| Bare, `SubAbility$`, `SpellDescription$`, `ConditionCheckSVar$`         |    26 | resolve; mostly a phenomenon's "(Then planeswalk away from this phenomenon.)"                          |
| `Optional$ True` (TARDIS, Start the TARDIS)                             |     2 | resolve through `ConfirmEffect`; a no-op outside Planechase, as Java                                   |
| `Defined$ Remembered` (Spatial Merging)                                 |     1 | **rejected**: walks to two planes at once                                                              |
| `Defined$ Remembered \| DontPlaneswalkAway$ True` (Norn's Seedcore)     |     1 | **rejected**: new plane joins the old one                                                              |
| `Cause$ PlanarDie` (synthetic, `Player.java:3272`; 0 card-script lines) |     — | resolves: `Cause$` only feeds the replacement's `AbilityKey.Cause`, and that path fails closed (below) |

**Rejected: `Defined$`, `DontPlaneswalkAway$`.** Both need more than one concurrent active plane (Java's
`currentPlanes`/`activePlanes` lists grow past one). ADR-0029 chose a single `activePlane` on the premise "none found";
Spatial Merging and Norn's Seedcore are two real cards that do need it. Kept the ADR's shape (binding); porting them
needs a superseding ADR widening `activePlane` to a slice. Error:
`engine: Planeswalk: Defined$ not resolvable yet (needs concurrent active planes, ADR-0029)`.

**Rejected: a live `Event$ Planeswalk` replacement** (Susan Foreman, P No Way Out, Fixed Point in Time's effect).
`ReplacementType.Planeswalk` is not ported (ADR-0029). Its presence alone refuses the planeswalk, whether or not its own
conditions (`IsPresent$`, `ValidCause$`) would hold — fail-closed over planeswalking as if it were absent. Checked after
the Planechase gate and `Optional$`, as Java: Susan Foreman in a normal game changes nothing.

**Error: activator's planar deck empty after leaving.** Java's `getZone(PlanarDeck).get(0)` throws
`IndexOutOfBoundsException` (`Player.java:2642`); refused before anything moves (GO-7). The activator's own leaving
plane counts: it goes to the bottom first and is walked straight back to
(`TestPlaneswalkDeckRefilledByTheLeavingPlane`).

### Not ported

| Gap                                                         | Why                                                                                                                                                                                                                            |
| ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Game-start `initPlane` (`Player.java:2693-2711`, CR 901.5)  | Match setup, not an effect; Crucible has no match setup. `SetActivePlane` injects a starting plane                                                                                                                             |
| CR 901.6/901.10 planar controller or owner leaving the game | `Game.onPlayerLost` (`Game.java:899-986`); a lost player's plane is not moved. Leave loop skips lost players like `game.getPlayers()`                                                                                          |
| `planeswalkedToThisTurn`                                    | No real reader among ported shapes                                                                                                                                                                                             |
| Plane counters across a trip to the planar deck             | `Move` keeps `Counters` off-battlefield; Java builds a new card object. A revisited Mount Keralia would keep old pressure counters                                                                                             |
| Scenario fixture                                            | `GameState.java` has no `PlanarDeck` zone key (`ZONES`, `GameState.java:53`, only `command` among the non-standard zones); a Crucible-only key would break the Java oracle's reading of the same fixture. Module tests instead |

### Forge defect found (PORT-8)

`forge-gui/res/cardsfolder/m/mount_keralia.txt:11` — `SVar:KeraliaX:TriggeredCard$CardCounters.PRESSURE` reads
`AbilityKey.Card` (`AbilityUtils.java:700-703`), but its `Mode$ PlaneswalkedFrom` trigger sets only `AbilityKey.Cards`
(`TriggerPlaneswalkedFrom.setTriggeringObjects`). `new CardCollection((Card) null)` adds nothing (`FCollection.add`,
null → false), so X is 0: the eruption deals no damage. Fix: `TriggerObjectsCards$CardCounters.PRESSURE`
(`AbilityUtils.java:694-697`, reads the `Cards` collection as it stood at trigger time, counters included). Crucible
records no `Card` triggering object for these modes, matching Java; not compensated. Row in `forge-java-defects.md`.

Tests: `planechase_test.go`.
