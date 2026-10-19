# Port Log — Game State: M6 Effects: Targeted Pump, Default GainLife, Discarded Card

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)

## Targeted Pump, GainLife's default player, Megrim's discarded card

Three shapes the turn driver's scenarios (ADR-0026) hit first. Each blocked a common card, not an edge case.

| Shape | Corpus | Go | Java |
| --- | --- | --- | --- |
| `Pump` with `ValidTgts$` (Giant Growth) | 2,555 of 5,034 `(SP\|AB\|DB)$ Pump` lines; 2,318 name no unresolved param | `pumpEffect.Resolve` via `targetedOrDefinedCards` (`defined.go`) | `PumpEffect.resolve`, `getCardsfromTargets` |
| `GainLife` with no `Defined$` (Nourish) | 796 of 1,804 `(SP\|AB\|DB)$ GainLife` lines; 774 name no unresolved param | `gainLifePlayers` (`gainlifeeffect.go`) | `LifeGainEffect.resolve`; `SpellAbilityEffect.java:326-345` |
| `Defined$ TriggeredCardController` on `Mode$ Discarded` (Megrim) | every Discarded watcher naming the discarder | `checkDiscardedTriggers` (`trigger.go`) records `triggered.card` | `TriggerDiscarded.java:72-74` |

**Pump.** Targets come from `Ability.Targets`, already chosen at cast/activation (ADR-0018). A target that phased out
(CR 702.26e) or left `PumpZone$` (Battlefield by default) since it was chosen is skipped, the same two checks Java's own
loop runs. Java's `tgtPlayers` loop grants a player keywords only; this port has no player keyword record, so a `KW$`
line with a player target fails loudly (GO-7). A P/T-only line ignores player targets, as `applyPump(player)` does.

**GainLife.** `getTargetPlayersWithDuplicates(true, "Defined", sa)`: `Defined$` first, else the chosen player targets,
else `getParamOrDefault("Defined", "You")` (`SpellAbilityEffect.java:339`) — the activator. A player no longer in the
game gains nothing (`!p.isInGame()`). The shared `definedPlayers("")` stays an error: other effects call it too, and
defaulting there would change them without reading their Java.

**Discarded.** Both halves of `checkDiscardedTriggers` — the discarded card's own trigger and every watcher's — record
the discarded card as `AbilityKey.Card`, Java's only triggering object for the mode. `TriggeredCardController` then
reads that card's controller.

Fixtures: `priority-giant-growth-response-saves-creature-from-bolt`, `gainlife-without-defined-goes-to-the-caster`,
`megrim-damages-the-player-who-discarded`; `TestStepRepeatsCleanupWhenItGrantsPriority` uses Megrim's own trigger line.
