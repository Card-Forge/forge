# Effects: RollPlanarDice lands (ADR-0029)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `rollplanardiceeffect.go`, `player.go`
  (`planarDie`), `becomemonarcheffect.go` (`IsDesignationCard`)
- **Decision:** [ADR-0029](../../../adr/0029-planechase-active-plane-state.md)
- **Siblings:** [`effects-planeswalk.md`](effects-planeswalk.md) (`activePlane`, `planechaseActive`, the planar host
  walk), [`effects-chaosensues.md`](effects-chaosensues.md) (`Mode$ ChaosEnsues`)

Batch file for `RollPlanarDice`, the fourth of the Planechase APIs ADR-0029 unblocks. Supersedes `RollPlanarDice`'s
deferred row in `effects-batch-d.md` (closed file, left as written).

---

## RollPlanarDice lands

CR 901.9's planar die roll. Ported from
`forge-game/src/main/java/forge/game/ability/effects/RollPlanarDiceEffect.java`'s `resolve` with
`forge-game/src/main/java/forge/game/PlanarDice.java`'s `roll` inlined. Roller = `a.Controller`
(`sa.getActivatingPlayer()`).

| Step               | Go                                                                                         | Java                                                        |
| ------------------ | ------------------------------------------------------------------------------------------ | ----------------------------------------------------------- |
| `SpecialAction$`   | rejected up front                                                                          | `incPlanarDiceSpecialActionThisTurn` (`:28-30`)             |
| Planechase gate    | `!g.planechaseActive` → no-op                                                              | `getActivePlanes() == null` → return (`:25-27`)             |
| Refusals           | `planarDieRollResolvable`, before the draw (below)                                         | —                                                           |
| Roll               | `g.rand.Int32n(6)`: 0 Planeswalk, 1 Chaos, 2-5 Blank. No `+1`: the index names a face      | `MyRandom.getRandom().nextInt(6)` (`PlanarDice.java:44-53`) |
| Planar Dice card   | roller's made on first roll (`putDesignationCard`), reused after                           | made per player at game start (`Player.java:3259-3287`)     |
| `Mode$ PlanarDice` | `planarDiceTriggerMatches`, `Result` = face                                                | `runTrigger(PlanarDice)` (`:73-75`)                         |
| `RolledDie`/`Once` | refused before the roll                                                                    | `runTrigger(RolledDie)` ×rolls, `RolledDieOnce` (`:77-88`)  |
| Chaos face         | `chaosEnsuesTriggerMatches(roller, NoCard, …)`, the walk behind `checkChaosEnsuesTriggers` | `runTrigger(ChaosEnsues, {Player})` (`:90-93`)              |
| Errors             | `TakePendingError` after the run                                                           | —                                                           |

**The Planeswalk face does not planeswalk here.** `PlanarDice.roll` only reports the face. The walk is the roller's
"Planar Dice" effect card's own trigger,
`Mode$ PlanarDice | Result$ Planeswalk | TriggerZones$ Command | ValidPlayer$ You | Secondary$ True` →
`DB$ Planeswalk | Cause$ PlanarDie` (`Player.java:3270-3275`): a triggered ability on the stack like any other roll
trigger, resolved by the landed `Planeswalk` port (`Cause$` accepted there).

**Both trigger runs push as one batch.** Java puts every trigger one roll fires into one simultaneous batch, stacked
APNAP (`MagicStack.addAllTriggeredAbilitiesToStack`). Calling each run's own `check…Triggers` entry point run by run
would push all `PlanarDice` matches, then all `ChaosEnsues` matches: a nonactive player's "whenever you roll" would sit
under the active player's chaos ability instead of over it. So `runPlanarDieTriggers` resolves `Static$ True` lines
inline in run order (ADR-0020), then pushes both runs' other matches in one `pushTriggeredAbilities` call
(`TestPlanarDieTriggersStackInOneAPNAPBatch`). Reads `chaosEnsuesTriggerMatches` directly rather than calling
`checkChaosEnsuesTriggers`; `trigger.go` itself is unchanged.

### Refused before the roll

Each refusal happens after the Planechase gate (outside Planechase nothing is read, as Java) and before `Int32n`, so a
refused roll consumes nothing from the game's stream (`TestRollPlanarDiceRefusesUnportedShapes` rolls again with the
blocker gone and lands the stream's first draw).

| Shape                                                  | Real lines                                                                   | Why refused                                                                                                                                      |
| ------------------------------------------------------ | ---------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `SpecialAction$`                                       | 0 card lines; the planar die's own `ST$ RollPlanarDice` (`Player.java:3277`) | bumps `PhaseHandler`'s per-turn `planarDiceSpecialActionThisTurn`, read only by that action's `Cost$ X`; the special action itself is not ported |
| live `Event$ RollPlanarDice` replacement               | 1 (Ichor Elixir, "roll that many plus one and ignore one")                   | `ReplacementType.RollPlanarDice` and `PlayerController.choosePDRollToIgnore` deferred (ADR-0029)                                                 |
| live `Event$ PlanarDiceResult` replacement             | 1 (Chaotic Aether's effect, blank → chaos)                                   | `ReplacementType.PlanarDiceResult` deferred (ADR-0029)                                                                                           |
| live `Mode$ RolledDie` / `Mode$ RolledDieOnce` trigger | 31 (23 / 8)                                                                  | engine-wide unported trigger modes; Java runs both on every planar roll, so rolling without them would drop them silently                        |

Replacements found through `eachReplacement` (Battlefield + Command, `ActiveZones$`/effect-card gated). A `RolledDie`
line counts as live from `TriggerZones$` (Battlefield when none; effect cards Command only) across Battlefield, Command,
Graveyard, Exile, Hand (`rolledDieZones`; real lines: 28 Battlefield, 1 Command, 2 Graveyard). Presence refuses whether
or not the line's own conditions would hold — none is ported far enough to ask.

**PORT-7 note for whoever ports `RolledDie`:** `PlanarDice.roll` runs `RolledDie` with `Result` = `0` and
`RolledDieOnce` with `Result` = `[0]` whatever face came up (`PlanarDice.java:81`, `:87`), `Sides` = 6, no `Number`.
Parity needs that reproduced: a `ValidResult$ LE3` line fires on every planar roll, `ValidResult$ 6`/`Highest` never.
`TriggerRolledDie.performTest`'s `Number$` branch unboxes a missing `AbilityKey.Number` (`TriggerRolledDie.java:57-60`)
and would throw on a planar roll; 0 real `Mode$ RolledDie … Number$` lines, so unreached.

### Corpus

| Shape                                                                            | Lines | Status                                                          |
| -------------------------------------------------------------------------------- | ----: | --------------------------------------------------------------- |
| `AB$ RollPlanarDice \| Cost$ T \| SorcerySpeed$ True` (Fractured Powerstone)     |     1 | resolves; a no-op outside Planechase, its usual real path       |
| `ST$ RollPlanarDice \| … \| SpecialAction$ True` (synthetic, `Player.java:3277`) |     — | rejected, `SpecialAction$` (above); no Crucible card carries it |

### Not ported

| Gap                                                                  | Why                                                                                                                                                                                                                                      |
| -------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Planar die's special action (CR 901.9, `{X}`: roll, X = times taken) | Needs a Command-zone special action plus `PhaseHandler`'s per-turn counter; no card script reaches it                                                                                                                                    |
| `Player.roll()` / `numRollsThisTurn`                                 | Crucible keeps no per-turn roll count (`RollDice` neither); its reader, `Count$YouRollThisTurn` (1 real line), is not resolvable                                                                                                         |
| `GameEventRollDie`, `notifyOfValue`                                  | Sound and UI message; no Crucible event kind                                                                                                                                                                                             |
| Planar controller follows the turn (`PhaseHandler.java:850-857`)     | Java hands every active plane to the new active player each turn; Crucible's plane stays in the Command zone of whoever walked to it. Decides whose "you" a plane's `Mode$ PlanarDice`/`ChaosEnsues` line means on another player's turn |
| Scenario fixture                                                     | `GameState.java` has no `PlanarDeck` zone key (`ZONES`, `GameState.java:53`), as `effects-planeswalk.md`. Module tests instead                                                                                                           |

## Mode$ PlanarDice and the Planar Dice card land

`TriggerPlanarDice.performTest`: `ValidPlayer$` against `AbilityKey.Player` (the roller), `Result$` against the face
(`PlanarDice.smartValueOf`, case-insensitive). Own walk in `rollplanardiceeffect.go`, same host set as its siblings.

| Piece              | Port                                                                                                                                  |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------- |
| Hosts              | `planeswalkTriggerZones` (every player's Battlefield + whole Command zone), `TriggerZones$` gated by `phaseTriggerZoneMatches`        |
| `ValidPlayer$`     | `matchesPlayerSpec` against the roller; unrecognized spec never fires (`checkChaosEnsuesTriggers`' rule)                              |
| `Result$`          | face name, case-insensitive; a name that is no face is an error (`smartValueOf` throws), recorded pending, returned by the effect     |
| `Static$ True`     | resolved inline through `resolveStaticTriggers` (ADR-0020); 0 real lines                                                              |
| Triggering objects | `triggeredObjects{player: roller}` — `setTriggeringObjectsFrom(runParams, AbilityKey.Player)`, readable as `Defined$ TriggeredPlayer` |
| Controller         | host's `Controller()`                                                                                                                 |

Corpus: 5 real `Mode$ PlanarDice` lines, all `TriggerZones$ Command` on planes: Pompeii (`Result$ Blank`), The Drum
Mining Facility (`ValidPlayer$ You`), Stairs to Infinity, Ten Wizards Mountain, P No Way Out (neither). All reach this
walk; their `Execute$` bodies resolve or fail by their own APIs.

**Planar Dice card (new `Player` state).** `Player.planarDie CardID`, `NoCard` until the player first rolls. Built by
`planarDieDef` with `designationDef`/`designationTrigger` (the `monarchEffectDef` pattern: trees built directly, nothing
parsed at runtime, PORT-2), placed by `putDesignationCard` (effect card, permanent, roller's Command zone).

| Choice                                          | Reason                                                                                                                                                                                                                                                        |
| ----------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| A real host card, not a bare pushed ability     | The walk trigger's stack item needs a source; Java's is this card. A synthetic ability with the rolling card (Fractured Powerstone) as source would name the wrong object on the stack and skip `Mode$ PlanarDice` matching                                   |
| `Player` field, not a Command-zone scan by name | Same shape as `monarchEffect`/`initiativeEffect`/`theRing`; ADR-0029's "no new `Game` field" untouched. Value field, so `Clone`'s player copy carries it (`TestClonePlanarDie`)                                                                               |
| Made at first roll, roller only                 | No match setup in Crucible (same gap as `initPlane`, `effects-planeswalk.md`). Only the roller's card can fire on a roll — its trigger is `ValidPlayer$ You` against the host's controller — so an unmade card for anyone else changes nothing a roll can see |
| Skipped by fixture dump                         | `IsDesignationCard` names it: Java makes it for every Planechase player, so no fixture line carries it                                                                                                                                                        |
| Trigger only, no special action                 | The card's `ST$ RollPlanarDice` special action is not ported (above)                                                                                                                                                                                          |

Tests: `planar_die_test.go`.
