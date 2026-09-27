# Effects: ChaosEnsues lands (ADR-0029)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `chaosensueseffect.go`, `trigger.go`
  (`checkChaosEnsuesTriggers`, `checkChaosEnsuesOnTriggers`)
- **Decision:** [ADR-0029](../../../adr/0029-planechase-active-plane-state.md)
- **Sibling:** [`effects-planeswalk.md`](effects-planeswalk.md) — `activePlane`, `planechaseActive`, the planar host
  walk

Batch file for `ChaosEnsues`, the second of the four Planechase APIs ADR-0029 unblocks. Supersedes `ChaosEnsues`'s
deferred row in `effects-batch-b.md` (closed file, left as written).

---

## ChaosEnsues lands

CR 311.7's "chaos ensues". Ported from `forge-game/src/main/java/forge/game/ability/effects/ChaosEnsuesEffect.java`'s
`resolve`. No new engine state: reads `planechaseActive` only; `Clone` untouched.

| Step            | Go                                                                                          | Java                                     |
| --------------- | ------------------------------------------------------------------------------------------- | ---------------------------------------- |
| Planechase gate | `!g.planechaseActive` → no-op, before `Defined$` is read                                    | `getActivePlanes() == null` (`:33-35`)   |
| No `Defined$`   | `checkChaosEnsuesTriggers(c, activator)`                                                    | `runTrigger(ChaosEnsues, {Player})`      |
| `Defined$`      | `chaosEnsuesAffected`; none → no-op; one → `checkChaosEnsuesOnTriggers(c, activator, card)` | affected list, `Affected` key (`:41-57`) |
| Errors          | `TakePendingError` after the run (a `Static$ True` chaos ability's failure)                 | —                                        |

Activator is `a.Controller` (`sa.getActivatingPlayer()`).

### `Mode$ ChaosEnsues` lands

`TriggerChaosEnsues.performTest` is not the `Planeswalked*` shape: no `ValidCard$`; `ValidPlayer$` against
`AbilityKey.Player`; an `AbilityKey.Affected` gate. Own walk, sharing only `planeswalkTriggerZones` and
`phaseTriggerZoneMatches`.

| Entry point                                                       | Use                                                                                                                                                     |
| ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `(g *Game) checkChaosEnsuesTriggers(controller, player PlayerID)` | No `Affected`. Every player's `Battlefield` + whole `Command` zone, `TriggerZones$` gated. The planar die's chaos face (`RollPlanarDice`) calls this    |
| `(g *Game) checkChaosEnsuesOnTriggers(controller, player, card)`  | `Affected` = `card`. Only `card`'s chaos abilities, from any zone, `TriggerZones$` not checked. Caller must first confirm `hasChaosEnsuesTrigger(card)` |
| `hasChaosEnsuesTrigger(c *Card) bool`, `isChaosEnsuesTrigger(t)`  | "has a chaos ability" test (`ChaosEnsuesEffect.java:43-44`)                                                                                             |
| `(g *Game) chaosEnsuesTriggerMatches(player, affected, static)`   | Shared collector; `affected == NoCard` = no `Affected` key                                                                                              |

Both entry points resolve `Static$ True` lines inline through `resolveStaticTriggers` (ADR-0020) first, then push the
rest through `pushTriggeredAbilities`; a static failure is left pending — callers finish with `g.TakePendingError()`.
Neither returns an error itself.

| performTest piece      | Port                                                                                                                                                                                      |
| ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ValidPlayer$`         | `matchesPlayerSpec` against `player`; unrecognized spec never fires (`checkTapsForManaTriggers`' `Activator$` rule). 0 real lines                                                         |
| `Affected` (`:36-50`)  | One card, walked alone. Java widens that card's chaos triggers' active zones by its current zone, runs, restores (`ChaosEnsuesEffect.java:45-49,61-70`) — skipping the zone gate is equal |
| Triggering objects     | `triggeredObjects{player: player}` on every match: Java's `setTriggeringObjectsFrom(runParams, AbilityKey.Player)`, readable as `Defined$ TriggeredPlayer` (`defined.go`)                 |
| `OptionalDecider$ You` | 6 real lines; `triggerEffectAPI`'s generic `triggerIsOptional`                                                                                                                            |
| Controller             | host's `Controller()` — the plane's owner for a plane in its owner's `Command` zone, the planar controller CR 311.7 names                                                                 |

Differs from the `Planeswalked*` siblings on triggering objects: those record none (Java sets only `AbilityKey.Cards`,
unread). Here Java sets `AbilityKey.Player`, which Crucible already reads. No real chaos-ability body reads it today
(158 lines checked); recorded anyway for parity.

Corpus: 158 real `Mode$ ChaosEnsues` lines, all `TriggerZones$ Command`, all on Plane cards; 6 `OptionalDecider$ You`;
none `ValidPlayer$`, none `Static$`. All reach this walk; their `Execute$` bodies resolve or fail by their own APIs.

### Corpus: 11 real `DB$ ChaosEnsues` lines

| Shape                                                                          | Lines | Status                                                                                                                         |
| ------------------------------------------------------------------------------ | ----: | ------------------------------------------------------------------------------------------------------------------------------ |
| Bare / `SpellDescription$`: Missy, Oteclán, Norn's Seedcore, 5× Path of the …  |     8 | resolve; no-op outside Planechase (Missy, the Paths' vote in a normal game)                                                    |
| Bare, `ReplaceWith$` of `Event$ Planeswalk`: Fixed Point in Time, P-No Way Out |     2 | resolve, but unreachable today: `Planeswalk` rejects any live `Event$ Planeswalk` replacement (`effects-planeswalk.md`)        |
| `Defined$ Remembered \| SubAbility$`: The Fertile Lands of Saulvinia           |     1 | resolves; unreachable today: its parent `DB$ DigUntil \| DigZone$ PlanarDeck` is rejected (`diguntileffect.go:12`, `DigZone$`) |

Norn's Seedcore's own chaos ability dies the same way: its `DigUntil` has `DigZone$ PlanarDeck`, rejected before its
`DB$ Planeswalk | Defined$ Remembered | DontPlaneswalkAway$ True` (itself rejected, ADR-0029) is reached. The
`ChaosEnsues` run and the triggered ability's own failure are independent: `ChaosEnsues` fires the trigger correctly.

**Rejected: `Defined$` naming more than one distinct card with a chaos ability.** `TriggerChaosEnsues.performTest`'s
`Iterable` branch (`forge-game/src/main/java/forge/game/trigger/TriggerChaosEnsues.java:43-48`) passes only when
**every** affected card equals the host, so two distinct planes fire neither. CR 311.7 defines chaos ensuing only "for a
particular object"; no real line reaches the case (Saulvinia remembers `DigUntil`'s single found plane, then
`Cleanup`s). Suspected Forge defect — "any affected card is the host" was likely meant — but unverifiable without a real
card, so fail closed rather than reproduce a silent no-op or guess the intent. Error:
`engine: ChaosEnsues: Defined$ <spec> naming more than one plane not resolvable yet`. The same card listed twice (Java
adds it once per chaos trigger, `:48`) is one card, fires normally.

**Error: a `Defined$` spelling `definedCards` does not resolve** (`engine: Defined$ "<spec>" not resolvable yet`), as
every `Defined$` reader. Checked after the Planechase gate: outside Planechase nothing is read, as Java.

### Not ported

| Gap              | Why                                                                                                                                                                                                                 |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Scenario fixture | `GameState.java` has no `PlanarDeck` zone key (`ZONES`, `GameState.java:53`); a Crucible-only key would break the Java oracle's reading of the fixture (`effects-planeswalk.md`, same reason). Module tests instead |

Tests: `planar_chaos_test.go`.
