# Port Log — Game State: `ReplaceDyingDefined$` and `ExcessSVar$` (R9e)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `replacedying.go` (`checkReplaceDying`,
  `replaceDying`, `filterDefinedCards`, `replaceDyingDef`), `excessdamage.go` (`excessDamageValue`, `damageable`,
  `defaultExcessSVar`, `bindExcessSVar`, `checkExcessSVar`, `matchesWithTargetedBy`, `chainTargeted`), `trigger.go`
  (`damageEntry.Lethal`), `combatdamage.go` (`dealPermanentDamage` records it), `damageresolveeffect.go`
  (`rememberDamaged` moved here), callers in `dealdamageeffect.go`, `damagealleffect.go`, `fighteffect.go`,
  `pumpeffect.go`
- **Java:** `SpellAbilityEffect.java:548-560` (`addForgetOnMovedTrigger`), `:653-689` (`createEffect`), `:691-756`
  (`replaceDying`); `DamageDealEffect.java:269-272`; `DamageAllEffect.java:106`; `DamageResolveEffect.java:28-30`;
  `FightEffect.java:60-62`, `:177`; `PumpEffect.java:506`; `GameAction.java:2738-2767`, `:2787-2794`;
  `CardDamageTable.java:113-155`; `Card.java:6127-6151`, `:6325-6327`; `CardProperty.java:271-277`;
  `SpellAbility.java:1691-1704` (`getSVarFallback`), `:2112-2118` (`isTargeting`); `AbilityUtils.java:68`, `:319-326`,
  `:400-423`

## `ReplaceDyingDefined$`: an effect card with a built Moved replacement

`replaceDying` runs after the effect's damage or pump. The cards `ReplaceDyingDefined$` names now are remembered by a
new effect card in the activator's Command zone, which carries one replacement:

```text
Event$ Moved | ValidLKI$ Card.IsRemembered | Origin$ Battlefield | Destination$ Graveyard
  -> DB$ ChangeZone | Defined$ ReplacedCard | Origin$ Battlefield | Destination$ Exile
```

| Piece       | Does                                                                                     | Why                                                                                     |
| ----------- | ---------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| Definition  | `replaceDyingDef` builds the tree as `compile.Ability` literals; nothing parsed          | PORT-2; Java parses the strings at `:728-740`. Same precedent as `monarchEffectDef`     |
| Applying    | The existing Moved dispatch `movedGraveyardDestination` reads it from the Command zone   | No new replacement path; SBA, Destroy, Sacrifice all reach it through `moveToGraveyard` |
| Lifetime    | `effectUntilEndOfTurn`, exiled at the next cleanup                                       | `game.getEndOfTurn().addUntil(exileEffect)`, `:752`                                     |
| Forget      | `forgetOnMoved` = Battlefield: a remembered card leaving is forgotten, empty effect goes | `addForgetOnMovedTrigger(eff, "Battlefield")`; `effectCardsSeeMove`                     |
| No cards    | No effect                                                                                | `:712-715`; Disintegrate at a player                                                    |
| `.<valid>`  | `Remembered.Creature`, `ThisTargetedCard.Creature`: base through `definedCards`, filter  | `getDefinedCards`' `incR` split, each alternative prefixed `Card.` (`:319-326`)         |
| `Condition` | `ReplaceDyingCondition$ Kicked`: no effect unless kicked                                 | `:693-701`; any other value refused, Java would install the effect regardless           |
| SVars       | The effect's face carries the ability's amounts                                          | `createEffect`'s `eff.setSVars(sa.getSVars())`                                          |

No new `Game` state and no new controller decision: the effect card is an ordinary arena card (`IsEffect`,
`effectLife`), which `Game.Clone` already copies; its definition is immutable once built.

Where each effect calls it, matching Java's position relative to its early returns:

| Effect          | Call site                                                                                             | Corpus lines |
| --------------- | ----------------------------------------------------------------------------------------------------- | ------------ |
| `DealDamage`    | After the batch, `RememberDamaged$` and triggers; under `DamageMap$` before the later `DamageResolve` | 44           |
| `DamageAll`     | After the sweep and `RememberDamaged$`; also with `NumDmg$` 0                                         | 6            |
| `DamageResolve` | After the map is dealt and `RememberDamaged$`                                                         | 1            |
| `Fight`         | After the fight's damage; no effect with fewer than two fighters (`:60-62`)                           | 4            |
| `Pump`          | At the end; also on Go's "nothing to pump" return, which Java does not have                           | 4            |

`ReplaceDyingDefined$` is checked before the effect acts (`checkReplaceDying`): the base spelling must be one
`definedCards` reads. `ReplaceDyingExiledWith$` (Gut, Fanatical Priestess: `ExiledWithEffectSource$` on the ChangeZone,
outside `movedGraveyardDestination`'s allow-list), `ReplaceDyingValid$` and `ReplaceDyingZone$` (no corpus line) are
refused by name.

## Silent misses closed on the way

Before this batch these lines played, ignoring a param:

| Effect          | Param read nowhere                            | Cards                                                                                                          |
| --------------- | --------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| `Pump`          | `ReplaceDyingDefined$`                        | Bleed Dry, Gnashing of Teeth, Necrotic Wound, Ob Nixilis's Cruelty                                             |
| `DamageAll`     | `ReplaceDyingDefined$` and `RememberDamaged$` | Anger of the Gods, Crush the Weak, Draconic Intervention, Gallifrey Falls, Yamabushi's Storm, Underworld Fires |
| `DamageResolve` | `RememberDamaged$`                            | Serpentine Spike (also refused for `ReplaceDyingDefined$`)                                                     |

`DamageAll`'s `RememberDamaged$` has 11 corpus lines. Besides the six above, Reckless Blaze (plays: its Effect's
`RememberObjects$ Remembered` now holds the damaged creatures, so its "dies this turn, add {R}" trigger can match),
Flamebreak (still refused, at its Effect's `Remembered.Creature`), Aggravate and Chaos Balor (refused at `DamageAll`'s
`ValidTgts$`, and Chaos Balor's line is not reached by the probe). The smoke probe cannot see this change: those cards
played before and still do.

`rememberDamaged` moved from `dealdamageeffect.go` to `damageresolveeffect.go` so `DamageAll` and `DamageResolve` can
call it without a group cycle (`dealdamageeffect` already depends on `damageresolveeffect`).

## `ExcessSVar$`: the batch's excess bound for the chain

`GameAction.dealDamage` snapshots each damaged permanent's `getExcessDamageValue(false)` before its first damage of the
batch (`:2738-2752`); `triggerExcessDamage` sums what the batch dealt it, subtracts the snapshot (1 when it has been
dealt deathtouch damage), and adds the positive results of the permanents matching `ExcessSVarCondition$` into the
cause's SVar, 0 included (`CardDamageTable.java:113-155`).

| Piece          | Go                                                                                                                     |
| -------------- | ---------------------------------------------------------------------------------------------------------------------- |
| Snapshot       | `damageEntry.Lethal`, set by `dealPermanentDamage` right before marking; the first entry per card is used              |
| Sum            | Per card across every source of the table, in first-damaged order                                                      |
| Not damageable | A card none of creature, planeswalker, battle adds nothing (`Card.java:6325-6327` deals it 0)                          |
| Condition      | Matched against the damaged permanent, not the source (`:137`); the opposite of `ExcessDamageCondition$`               |
| `targetedBy`   | `matchesWithTargetedBy`: the root's `isTargeting` over the chain, read as own targets, `parentTargets`, `chainTargets` |
| Binding        | `withAmount` on `a.Amounts`; `resolveSubAbility` hands the map down, Java's `getSVarFallback` parent read              |

Callers: `DealDamage` (22 lines) and `Fight` (2: Rhino's Rampage, The Last Agni Kai). Under `DamageMap$` it is refused:
Java's cause there is the later `DamageResolve`, so the `DealDamage` link's SVar is never set.

**Unset reads 0 (PORT-7).** Java sets the SVar only once `dealDamage` runs for the ability. An ability returning before
that (no damage source left, `OptionalDecider$` declined, an unmet condition, a fight with one fighter) leaves it unset,
and `calculateAmount` reads an unset SVar as 0 (`AbilityUtils.java:415-423`). `defaultExcessSVar` binds 0 first unless
the card defines the name itself, so Windswift Slice whose biter left makes no token instead of failing its
`TokenAmount$ Excess`. A card's own SVar of that name survives the early return, as Java's card fallback does; a
`dealDamage` that ran, even over an empty table, overwrites it.

`checkExcessSVar` refuses, before the effect acts, `ExcessSVar$` under `DamageMap$` and an `ExcessSVarCondition$` naming
`!targetedBy` (no corpus line). An ancestor above the nearest targeting one is not carried, so its targets read false
for `targetedBy`; no `ExcessSVarCondition$` line sits under two targeting ancestors.

## Smoke probe

`crucible card-smoke -all -workers 8`, before → after:

| Status       | Before | After  |
| ------------ | ------ | ------ |
| plays        | 30,572 | 30,630 |
| refused      | 2,941  | 2,883  |
| placed       | 372    | 372    |
| inconclusive | 193    | 193    |
| judged play  | 91.2%  | 91.4%  |

| Flip                    | Cards | Cause                                                                                                                                                                                                                                                      |
| ----------------------- | ----- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| refused → plays         | 58    | 37 `DealDamage` `ReplaceDyingDefined$` (The War Doctor at `turns`), 16 `ExcessSVar$`, Mawloc and Unnatural Aggression (`Fight` `ReplaceDying`), Rhino's Rampage and The Last Agni Kai (`Fight` `ExcessSVarCondition$`), Serpentine Spike (`DamageResolve`) |
| refused, reason changed | 9     | Each now stops at a later refusal (below)                                                                                                                                                                                                                  |
| plays → refused         | 0     |                                                                                                                                                                                                                                                            |

| Card                     | Now stops at                                                                                                                      |
| ------------------------ | --------------------------------------------------------------------------------------------------------------------------------- |
| Bolg of the North        | `DealDamage: NumDmg$ "_" is not resolvable` (`X` = `Spawner>Sacrificed$CardPower`, the ImmediateTrigger spawner's cost-paid list) |
| Brazen Boarding          | `MakeCard: Filter$ not resolvable yet`                                                                                            |
| Burn from Within         | `Debuff: Defined$ "_" not resolvable yet`                                                                                         |
| Gut, Fanatical Priestess | `Fight: ReplaceDyingExiledWith$ not resolvable yet`                                                                               |
| Mephit's Enthusiasm      | `Effect: Boon$ not resolvable yet`                                                                                                |
| Narset's Rebuke          | `Mana: Produced$ "_" not resolvable yet`                                                                                          |
| Ravenous Pursuit         | `Pump: Duration$ "_" not resolvable yet`                                                                                          |
| Red Sun's Zenith         | `ChangeZone: Defined$ "_" not resolvable yet`                                                                                     |
| Unleash the Inferno      | `ImmediateTrigger: RememberSVarAmount$ not resolvable yet`                                                                        |

## Not ported

| Shape                                                                   | Reason                                                       |
| ----------------------------------------------------------------------- | ------------------------------------------------------------ |
| `ReplaceDyingExiledWith$` (Gut, Fanatical Priestess)                    | `ExiledWithEffectSource$` on the replacing ChangeZone        |
| `ReplaceDyingCondition$` other than `Kicked`                            | Java ignores the value and installs the effect; no such line |
| `ExcessSVar$` under `DamageMap$`                                        | The SVar belongs to the later `DamageResolve`                |
| `ExcessSVarCondition$` naming `!targetedBy`                             | No corpus line                                               |
| `ExcessDamage`/`ExcessDamageAll` trigger modes, `Damage.ExcessThisTurn` | Not this batch's; `ExcessThisTurn` is still never set        |

Found while testing, not this batch's: a `Pump`'s P/T change is not visible to a later link of the same chain until the
layers next run (`applyContinuousLayers` runs at state-based checks, not between links). Rhino's Rampage, among the
cards that now play, therefore fights with the unpumped power: a 5/1 pumped to 6/1 dealt a 4/2 5 damage (3 excess) where
Java's `getNetPower`, reading the pump at once, deals 6 (4 excess). A silent wrong answer, not a refusal.

## Tests

`exileinsteadofdying_test.go` (Lava Coil before the SBA and after, a survivor destroyed later the same turn and one
destroyed next turn, a player target and the `.Creature` filter, `RememberDamaged$` with Cleanup, prevented damage
remembering nothing, Anger of the Gods with an indestructible creature, `DamageAll` at 0 damage, Bleed Dry, X=0 Necrotic
Wound, a Fight's target, a Fight with no target, Serpentine Spike's `DamageResolve`, Burn the Accursed under
`DamageMap$`, unkicked Scorching Lava, every refused shape), `excessdamage_test.go` (excess past lethal and past marked
damage, `ConditionCheckSVar$`, planeswalker and player, a deathtouch biter, the unset read, a card's own SVar,
`Card.targetedBy` and a Radiance$ creature a later link targets, a condition on the damaged card, Rhino's Rampage's
fight, a fight with one fighter, `!targetedBy`). Scenarios `replace-dying-lava-coil-exiles-the-creature-it-kills`,
`replace-dying-lava-coil-indestructible-survives-the-turn`,
`replace-dying-lava-coil-survivor-bolted-the-same-turn-is-exiled`,
`replace-dying-anger-of-the-gods-exiles-every-creature-it-kills`,
`excess-svar-lacerate-flesh-makes-a-blood-token-per-excess-point`.
