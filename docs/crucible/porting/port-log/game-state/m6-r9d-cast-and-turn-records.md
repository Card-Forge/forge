# Cast and turn records behind amounts (R9d)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `replacement.go` (`damageReplacement`),
  `ability.go`/`defined.go` (`ReplacedTarget`), `castrecord.go` (`leftBattlefieldThisTurn`, `isVoid`, `hasRevolt`),
  `stack.go` (`noteCrime`), `player.go` (`descended`, `committedCrimes`), `memory.go` (`rememberedNumbers`),
  `putcountereffect.go` (`RememberAmount$`), `manapay.go`/`castspell.go` (`xAnnounced.snow`, `Card.castSnow`,
  `escapeCost`), `optionalcost.go` (Bargain), `foretell.go` (`cardIsForetold`), `card.go` (`castOptional.foretold`,
  `escaped`), `condition.go`, `amountheads.go`, `amountbranches.go`, `valid.go`, `animate*.go`
- **Java:** `AbilityUtils.calculateAmount` `ReplaceCount$` (`:663-671`), `getDefinedPlayers` "Replaced" (`:1080-1106`),
  `xCount` (`:1708-1710` Bargain, `:1896-1908` CastTotalManaSpent, `:1991-1999` RememberedNumber, `:2044-2047` Foretold,
  `:2058-2060` Escaped, `:2268-2291` Revolt/CommittedCrime, `:2336-2338` YouDescended, `:2537-2542`
  LeftBattlefieldThisTurn, `:2698-2701` Void, `:2782-2787` Random); `ReplaceDamage.setReplacingObjects`
  (`ReplaceDamage.java:143-146`); `Game.isVoid` (`Game.java:1432-1435`), `addLeftBattlefieldThisTurn`
  (`GameAction.java:539-540`); `Player.hasRevolt` (`Player.java:2095-2097`), `descend` (`:2102-2104`), `commitCrime`
  (`:3962-3968`), `onCleanupPhase` (`:2491`, `:2501`); `MagicStack.add` (`:449-451`), `commitCrimeCheck` (`:1034-1054`);
  `Card.rememberedObjects` (`Card.java:194`), `GameEntityCounterTable.java:181-184`;
  `GameActionUtil.getOptionalCostValues` (`:479-481`), Escape (`:145-169`), Foretell (`:205-225`); `Card.isForetold`
  (`Card.java:6717-6727`); `SpellAbilityCondition.java:109-123`, `:279-280`; `CardProperty.java:1751-1755` (`escaped`);
  `PlayerProperty.java:83-84`

## What refused

BEFORE `card-smoke -all -workers 8`: 76 cards refused on an unresolved amount
(`"<Api>: <Param>$ "_" is not resolvable"`). The R9d shapes among them, by the head that failed:

| Shape | Cards | Cards (before) |
| --- | --- | --- |
| `ReplaceCount$DamageAmount` in a non-Replace `ReplaceWith$` chain | 7 | Angel of Suffering, Awe Strike, Ria Ivor, Szadek, The Mindskinner, Undead Alchemist, Polukranos |
| Cast options (`Bargained`, `Foretold`, `Escaped`, `Madness`, `Emerged`, `AltCost`) | 8 | Candy Grapple, Stonesplitter Bolt, Starnheim Unleashed, Polukranos, From Under the Floorboards, Avacyn's Judgment, Adipose Offspring, Bringer of Green Zenith's Twilight |
| `Count$Devoured <valid>` | 4 | Marrow Chomper, Skullmulcher, Tar Fiend, Voracious Dragon |
| Per-turn records (`Void`, `LeftBattlefieldThisTurn`, descend, crime) | 7 | Plasma Bolt, Tragic Trajectory, Kutzil's Flanker, That's Rough Buddy, Molten Collapse, ... |
| `Count$Random.<min>.<max>` | 2 | Ashuza's Breath, Rainbow Knights |
| `Count$RememberedNumber` (after PutCounter `RememberAmount$`) | 5 | Auron, Dimension X Pizzasaur, Etched Slith, Lockjaw, Wishing Well |
| `Count$CastTotalManaSpent <valid>` | 6 | Graven Lore, Search for Glory, Tundra Fumarole (Snow); Bat Colony, Cataclysmic Prospecting, Spoils |

Polukranos needs both its first and second row: 38 distinct cards.

## What landed

| Record | Java | Engine |
| --- | --- | --- |
| `ReplaceCount$` in a chain | `calculateAmount` `:663-671` | `damageReplacement`'s non-Replace branch passes `replaceCountAmounts`, as `playerAmountReplaced` already did: "that much damage" for Mill, GainLife, Token… |
| `Defined$ ReplacedTarget` | `ReplaceDamage.java:145`, `:1104-1106` | `abilityRefs.replacedTarget` = the DamageDone event's `affected`; a player names itself, a creature nobody; outside a replacement an error |
| Left the battlefield this turn | `Game.leftBattlefieldThisTurn` (LKI copies) | Read off `Game.enteredThisTurn` (`from == Battlefield`), whose snapshot is already the pre-move card: no second list. Heads `LeftBattlefieldThisTurn`, `Void`, `Revolt` |
| Descend count | `Player.descended` (int) | `Player.descended`, beside the exported `DescendedThisTurn` bool (kept: tests and other porters read it); `Count$YouDescendedThisTurn` |
| Crimes | `MagicStack.add` → `commitCrimeCheck` | `noteCrime` in `PushAbility`, the one stack funnel, over `allTargetsOf` (modes, chain links); `Count$CommittedCrimeThisTurn`, player property `committedCrimeThisTurn` |
| Remembered integers | `rememberedObjects` `LinkedHashSet<Object>` | `Memory.rememberedNumbers`, an ordered set (equal integers collapse); `RememberedSize` counts them, `ClearRemembered` clears them |
| PutCounter `RememberAmount$` | `GameEntityCounterTable.java:181-184` | The total after the AddCounter replacements (Hardened Scales' extra counter in it), remembered when above 0. With `ETB$` an error |
| Snow mana a cast spent | `getPayingMana` filtered by source `Snow` | `xAnnounced.snow`, the drop in the pool's snow buckets across the payment → `Card.castSnow`; `CastTotalManaSpent Snow` |
| Bargain | `OptionalCost.Bargain`, `:479-481` | A keyword option in `optionalCostOptions` (`Sac<1/Artifact;Enchantment;Card.token/…>`), kind `optBargain`; `Count$Bargained`, `Condition$ Bargain` |
| Foretold | `Card.isForetold`, `AlternativeCost.Foretold` | `castOptional.foretold` set by the foretell-cost cast; `cardIsForetold` (in exile the card's own flag); `Count$Foretold`, `Condition$ Foretold` |
| Escape | `AlternativeCost.Escape`, `:145-169` | `escapeCost`: the owner casts from the graveyard, keyword mana instead of the mana cost, its `ExileFromGrave` part as the extra cost; `castOptional.escaped`; `Count$Escaped`, property `escaped` |
| Animate `Condition$` | `AbilityUtils.resolve` → `metConditions` | `Animate`/`AnimateAll` read `Condition$` through `rejectUnreadCondition` + `subAbilityConditionMet` instead of refusing it (Brave the Wilds' `Condition$ Bargain`) |

## Design decisions

| Decision | Why |
| --- | --- |
| No new left-battlefield list | `recordEntered` already logs every zone change with the card as it last was; a second list would be the same data kept twice |
| Void's warp clause reads false | No Warp cast exists here (`continuous.go` already reads a `Spell.Warp` grant as never used), so no spell this game cast was warped: the record, not a guess |
| `Madness`, `Convoked` stay refused | Each asks about the card's own keyword (Madness, Convoke), which this port does not perform. Reading "not cast that way" would turn the card to `plays` while its own text is dead. Emerge, Devour and `Mode$ AlternativeCost` are performed since R9d's second batch ([`m6-r9d-cast-leftovers.md`](m6-r9d-cast-leftovers.md)) |
| Bargain and Escape ported as casts, not only as reads | Both fit an existing seam (an optional cost; Beam me up's alternative cost plus extra parts), so the record is written by the path Java writes it on. Before, Bargain was never offered and Escape never castable, both silently |
| `CastTotalManaSpent <valid>` only for `Snow` | `Pool` counts mana by color and snow-ness, never by source card. Cave, Desert, Treasure need a per-unit source the pool does not keep (a Pool redesign, out of scope) |
| Crimes counted in `PushAbility` | Every spell, activated, triggered ability and copy reaches the stack there, as every one reaches `MagicStack.add`, which runs the check for copies too |

## Limits

| Limit | Effect |
| --- | --- |
| `payWithSnow` spends plain mana before snow mana for a plain pip or a generic unit | `CastTotalManaSpent Snow` sees the engine's payment order, not a player's choice to spend snow mana first |
| `RememberAmount$` sums the counters after replacement; Java's `totalValues()` also counts a placement an AddCounter replacement fully replaced | Differs only for a `Replaced` AddCounter replacement; no R9d card meets one |
| Escape with an X in its exile count (Nethergoyf) is not offered | the X of a non-mana exile part is not announced; the card stays uncastable from the graveyard, never half-paid |

## New engine state

| State | Why | Clone / cleanup |
| --- | --- | --- |
| `Player.descended int` | Java's count; `YouDescendedThisTurn` reads a number | Copied by value; reset at cleanup and RestartGame |
| `Player.committedCrimes int` | `Player.committedCrimeThisTurn` | Copied by value; reset at cleanup and RestartGame |
| `Memory.rememberedNumbers` | Integers in the remembered set; `EntityID` cannot carry one | `Memory.clone` clones the set; `ClearRemembered` clears |
| `Card.castSnow int`, `xAnnounced.snow int` | Snow mana the cast spent | Copied by value; cleared with `castManaSpent` |
| `castOptional.foretold`, `castOptional.escaped` | The cast SpellAbility's alternative cost | Copied by value; cleared with `castOpt` on `Move` |
| `optBargain`, `castOpts.foretold`, `castOpts.escape` | The Bargain kind; how `castFromHand` chose to cast | Per cast |
| `abilityRefs.replacedTarget` | `Defined$ ReplacedTarget` of a DamageDone replacement | Per resolution |

No new `PlayerController` decision: Bargain asks `ConfirmPayCost` and `ChoosePermanentsToSacrifice`, Escape's exile
`ChooseCardsForEffect`, the questions those costs already ask elsewhere.

## Not resolved, by name

| Shape | Cards | Reason |
| --- | --- | --- |
| `Count$Madness` | 2 | No Madness discard-to-exile cast |
| `Count$CastTotalManaSpent Cave/Desert/Treasure` | 3 | The pool keeps no mana source |

## Smoke probe

`crucible card-smoke -all -workers 8`, before → after:

| Status | Before | After |
| --- | --- | --- |
| plays | 30,542 | 30,572 |
| refused | 2,970 | 2,941 |
| placed | 372 | 372 |
| inconclusive | 194 | 193 |
| judged play | 91.1% | 91.2% |

29 refused → plays: Angel of Suffering, Auron, Awe Strike, Candy Grapple, Crumbling Sanctuary, Dimension X Pizzasaur,
Farsight Ritual, Graven Lore, Haunting Voyage, Immortal Coil, Kutzil's Flanker, Lockjaw, Molten Collapse, Oko the
Ringleader, Plasma Bolt, Polukranos, Ria Ivor, Search for Glory, Starnheim Unleashed, Stonesplitter Bolt, Szadek, That's
Rough Buddy, The Mindskinner, The Mycotyrant, Thunderous Debut, Tragic Trajectory, Tundra Fumarole, Undead Alchemist,
Wishing Well. Brave the Wilds went inconclusive → plays (its `TargetMin$ Count$Bargained.1.0` now reads). No plays →
refused. Etched Slith changed only its reason: its reflexive trigger now fires and stops at `RemoveCounter: ValidTgts$`.

## Tests

`castandturnrecords_test.go`: an Angel of Suffering-shaped damage-to-mill chain, `ReplacedTarget` on a creature and
outside a replacement; `LeftBattlefieldThisTurn`, `Void` and `Revolt` both ways; the descend count past tokens and
instants and its cleanup reset; crimes against an opponent, their creature, their graveyard card, and none against
yourself; `RememberedNumber` and `RememberedSize`, the set's collapse, Hardened Scales' replaced total; snow mana spent;
Bargain paid, declined, not offered, and gating Brave the Wilds' Animate; Escape castable only with enough fodder and
recorded for `escaped` and `Count$Escaped`; a foretold cast against one from hand. Scenarios
`amount-void-plasma-bolt-deals-three-after-a-creature-died`,
`amount-replace-count-angel-of-suffering-mills-twice-the-damage`, `amount-bargained-candy-grapple-gives-minus-five`,
`amount-left-battlefield-thats-rough-buddy-puts-two-counters`,
`amount-remembered-number-pizzasaur-counters-reflex-destroy`.
