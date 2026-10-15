# Port Log — Game State: layers between chain links, `ReplaceDyingExiledWith$`, excess-damage record (R9h)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `action.go` (`refreshLayersBetweenLinks`, the
  `staticsHeld` hold in `checkStateBasedActionsPass`), `subability.go` (call site), `game.go` (`staticsHeld`),
  `replacedying.go` (`replaceDyingDef`), `replacement.go` (`movedPost.exiledWith`, `movedGraveyardDestination`,
  `moveToGraveyard`), `excessdamage.go` (`batchExcess`, `excessSoFar`, `triggerExcessDamage`, `combatDamageMatches`,
  `validTargetMatches`), `combatdamage.go` (`dealPermanentDamage` sets the flag), `trigger.go`
  (`checkDamageTableTriggers` calls it), `damage.go` (`Damage.Heal`), `healdamageeffect.go`, `regeneration.go`,
  `valid.go` (`wasDealtExcessDamageThisTurn`, `TriggeredTarget`)
- **Java:** `AbilityUtils.java:1360-1377` (`resolveSubAbilities`); `GameAction.java:1096-1248` (`checkStaticAbilities`),
  `:1588-1618` (the SBA hold), `:2723-2808` (`dealDamage`); `PumpEffect.java:57-63`; `SpellAbilityEffect.java:691-756`
  (`replaceDying`), `:1087-1117` (`handleExiledWith`); `Card.java:6181-6205`, `:7233-7237`, `:7285`;
  `CardDamageTable.java:113-163`; `TriggerExcessDamage.java:50-69`; `TriggerExcessDamageAll.java:18-54`;
  `CardProperty.java:1164-1167`, `:2045-2063`

## Layers rebuilt before each sub-ability

`AbilityUtils.resolveSubAbilities` calls `checkStaticAbilities()` before it resolves the next link
(`AbilityUtils.java:1366-1367`, "Equip an untapped creature with Sword of the Paruns then cast Deadshot on it"). Java's
Pump writes its boost straight onto the card (`PumpEffect.java:61`, `addPTBoost`), so the next link reads it either way.
This port keeps a resolved Pump as a `pumpRecord` applied in Layer 7c, so only the rebuild puts the boost on the card
before the next link reads it: Rhino's Rampage's 5/1 pumped to 6/1 deals 6, not 5.

| Piece       | Go                                                                                   | Why                                                                                     |
| ----------- | ------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------- |
| Call site   | `resolveSubAbility`, once the sub is found, before `Registry.Resolve` of the child   | Java refreshes only when a sub-ability exists (`:1361-1364`)                            |
| Rebuild     | `refreshLayersBetweenLinks` → `refreshLayers` (all eight layers, no SBA)             | Reuses the existing pass; no parallel mechanism                                         |
| Context     | `g.resolving` and `g.xctx` cleared for the rebuild, restored after                   | Statics evaluate their own amounts, not the parent's targets or X                       |
| Re-entry    | Skipped while `g.layering > 0`                                                       | A layer pass can reach a chain                                                          |
| SBA hold    | Skipped while `g.staticsHeld > 0`, set for the whole of `checkStateBasedActionsPass` | Java's `setHoldCheckingStaticAbilities` around the SBA's destruction (`:1588-1618`)     |
| Left to SBA | Control-zone correction, static commands, unpairing, Always/Immediate triggers       | Java's `checkStaticAbilities` does them too (`:1203-1248`); the next SBA pass does here |

**New engine state:** `Game.staticsHeld int`, transient like `layering`: zero between calls, so `Game.Clone` leaves it
zero. Java's hold is narrower (destroy/sacrifice only); here it covers the whole pass, which also runs the layers
itself.

## `ReplaceDyingExiledWith$` (Gut, Fanatical Priestess)

`replaceDying` adds `ExiledWithEffectSource$ True` to the replacing ChangeZone when the ability names
`ReplaceDyingExiledWith$` (`SpellAbilityEffect.java:732-734`). `handleExiledWith` then marks the exiled card as exiled
with the effect's source instead of the effect (`:1092-1094`): `Card.getEffectSource` is the host of the ability that
made the effect (`Card.java:7233-7237`), here Gut. Gut's specialized faces copy `Defined$ ExiledWith`.

| Piece    | Go                                                                                                          |
| -------- | ----------------------------------------------------------------------------------------------------------- |
| Build    | `replaceDyingDef` appends the param; `replaceDyingUnresolvedParams` keeps `ReplaceDyingValid$`/`Zone$`      |
| Dispatch | `movedGraveyardDestination` admits the key and records `effectLife.host` in `movedPost.exiledWith`          |
| Mark     | `moveToGraveyard` calls `markExiledWith` for the card and a melded partner after the move (exile only)      |
| Guard    | On a host that is not an effect card: pending error, replacement not applied (Java: null `getEffectSource`) |

No corpus `R:` line names `ExiledWithEffectSource$`; only `replaceDying` builds it. The plain shape (Lava Coil) still
marks nothing: Java marks it exiled with the effect card, which no reader looks at before the effect is exiled at
cleanup.

## Excess-damage record and the ExcessDamage trigger modes

`GameAction.dealDamage` runs `CardDamageTable.triggerExcessDamage` after the batch is dealt and before the
DamageDoneOnce family (`GameAction.java:2766-2767`, `:2803`). For each permanent dealt more than was lethal before the
batch it sets `hasBeenDealtExcessDamageThisTurn`, logs the excess, and runs Mode$ ExcessDamage; then Mode$
ExcessDamageAll once (`CardDamageTable.java:126-162`).

| Piece                          | Go                                                                                                                               |
| ------------------------------ | -------------------------------------------------------------------------------------------------------------------------------- |
| Per-card excess                | `batchExcess`, shared by `bindExcessSVar` and the triggers so the two cannot disagree                                            |
| `Damage.ExcessThisTurn`        | Set by `dealPermanentDamage` (`excessSoFar`) before the exchange's DamageDone check, and again by `triggerExcessDamage`          |
| Mode$ ExcessDamage             | Per damaged permanent: `ValidTarget$`, `CombatDamage$`; records `Target` and `DamageAmount` (the excess)                         |
| Mode$ ExcessDamageAll          | Once per batch: `CombatDamage$`, one damaged permanent matching `ValidTarget$`; `Targets` not recorded (no reader)               |
| Where                          | `checkDamageTableTriggers`, after lifelink, before DamageDoneOnce: every damage path (combat, DealDamage, DamageAll, Fight, ...) |
| Heal, regeneration             | `Damage.Heal` keeps the flag (`Card.healDamage`, `Card.java:6181-6185`); cleanup's `Clear` drops it (`:7285`)                    |
| `wasDealtExcessDamageThisTurn` | Valid property on the flag (`CardProperty.java:1164-1167`); it read false for every card before                                  |

**Why the flag is set per exchange.** Java tests a DamageDone trigger only once the batch is done (waiting triggers), so
Zangief's `ValidTarget$ Creature.wasDealtExcessDamageThisTurn` sees the excess that same damage dealt. This port checks
DamageDone inside `dealPermanentDamage`, per exchange. Setting the flag there, from the batch so far, gives the same
answer except when a later source of the same batch is what pushes an earlier-checked creature past lethal (two sources
damaging one creature simultaneously): Java's earlier source's trigger then sees the flag, this port's does not.

**Order.** Java iterates the lethal-damage snapshot as a `HashMap` (`GameAction.java:2738`), whose order no rule fixes;
this port uses first-damaged order.

**Not kept:** `Card.logExcessDamage`'s running total. Its one reader, `Count$ExcessDamageReceivedThisTurn`, is reached
only through Overclocked Electromancer's `CardTriggeredTarget$` amount head, which this port does not resolve (and the
card is refused earlier, at its Pump).

## `TriggeredTarget` valid property

`CardProperty.java:2045-2063`: the card is the root ability's `AbilityKey.Target` triggering object. Read off the
ability in context (`g.resolving`), false outside one. It read false for every card before, so `!TriggeredTarget`
excluded nothing: Toralf, God of Fury's "other than that permanent" and Blade of Shared Souls' "another target creature"
could pick the triggering creature itself, and Kusari-Gama's `DamageAll | ValidCards$ Creature.!TriggeredTarget+...`
dealt the damaged blocker its damage again. Those three are the corpus's card-level uses (every letter directory,
`upcoming/`, `rebalanced/`); the `Player.TriggeredTarget` forms (Komainu Battle Armor, Marisi, Wrexial, Willie Lumpkin)
are player properties, a different matcher.

## `ExcessSVar$` under `DamageMap$`: stays refused

No corpus line: none of the 24 `ExcessSVar$` cards names `DamageMap$` (`cardsfolder` letter directories, `upcoming/`,
`rebalanced/`). Java's cause there is the later DamageResolve (`DamageResolveEffect.java:28` → `GameAction.java:2767`),
so the DealDamage link's SVar would never be set. `checkExcessSVar` keeps refusing it rather than building a shape no
card uses.

## Smoke probe

`crucible card-smoke -all -workers 8 -list`, before → after:

| Status       | Before | After  |
| ------------ | ------ | ------ |
| plays        | 30,624 | 30,626 |
| refused      | 2,889  | 2,887  |
| placed       | 372    | 372    |
| inconclusive | 193    | 193    |
| judged play  | 91.4%  | 91.4%  |

| Flip            | Card                     | Cause                                                                                                                                                                                   |
| --------------- | ------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| refused → plays | Gut, Fanatical Priestess | `Fight: ReplaceDyingExiledWith$ not resolvable yet` resolved                                                                                                                            |
| refused → plays | Blade of Shared Souls    | `TriggeredTarget` now excludes the attached Rebel token, the probe board's only creature, so the Clone trigger has no target and its `Duration$ UntilUnattached` refusal is not reached |
| plays → refused | none                     |                                                                                                                                                                                         |

The probe cannot see the rest: Rhino's Rampage, The Last Agni Kai and every other Pump-then-link chain played before and
still do, now with the pumped numbers; Aegar, the Freezing Flame, Fall of Cair Andros, Toralf, God of Fury, Magmatic
Galleon and Zangief, the Red Cyclone played because their triggers never fired, and still play.

## Not ported

| Shape                                                              | Reason                                                                 |
| ------------------------------------------------------------------ | ---------------------------------------------------------------------- |
| `ExcessSVar$` under `DamageMap$`                                   | No corpus line (above)                                                 |
| `Count$ExcessDamageReceivedThisTurn`, `CardTriggeredTarget$`       | Overclocked Electromancer only; refused earlier at Pump                |
| ExcessDamageAll's `Targets` (Defined$ TriggeredTargets)            | No corpus reader                                                       |
| Java's LKI damage-history excess flag (`CardDamageTable.java:130`) | Read by `Count$NumDamageThisTurn` (Rith, Liberated Primeval), unported |
| checkStaticAbilities' non-layer steps between links                | Left to the next SBA pass (above)                                      |

## Tests

`sublinklayers_test.go` (a Pump read by the next link's `Count$CardPower`; a replacement chain reached inside the SBA
pass still runs every link), `excessdamage_test.go` (Rhino's Rampage's +1/+0 now in the root: 6 damage, 4 excess),
`replacedyingexiledwith_test.go` (Gut's fight exiles with the host, `ExiledWithSource` reads it; Lava Coil marks
nothing; `ExiledWithEffectSource$` on a non-effect host fails), `excessdamagetriggers_test.go` (ExcessDamage with
`TriggerCount$DamageAmount`, no excess, `CombatDamage$ True`; ExcessDamageAll once for two creatures and not for the
controller's own; Zangief's DamageDone gate; the flag through a heal and gone after `Clear`; `TriggeredTarget`).
Scenarios `pump-chain-rhinos-rampage-fights-with-the-pumped-power`,
`replace-dying-gut-exiles-the-fought-creature-with-itself` (`ExiledWith:` compared: `compareZoneCards` checks it by host
name when `expect.state` names one), `excess-damage-all-magmatic-galleon-makes-a-treasure`.
