# Port Log — Game State: turn-history properties and staged probe variants (R25f)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `validhistory.go` (new, `valid` group),
  `valid.go` (the hook), `block.go` (`recordBlockedBy`), `card.go`, `player.go`, `playerproperty.go`
  (`cleanupPlayerRecords`), `castrecord.go` (`recordSpellCast`), `game.go` (`castRecord.stamp`, `Move`,
  `MoveToLibraryTop`, `Clone`), `turn.go` (`cleanupStep`), `damage.go` (`Damage.Heal`), `cloneeffect.go` (`cloneSpec`);
  [`internal/smoke`](../../../../../crucible/internal/smoke) (`staged.go` new, `probe.go`, `controller.go`)

Java: `CardProperty.java:924-931,951-954,969-986,1144-1147,1156-1159,1177-1180,1199-1210,1613-1646,1929-1949`,
`Card.java:116,1657-1665,2056-2058,6104-6106,6181-6185`, `GameEntity.java:56,377-392`, `CardDamageHistory.java:256-281`,
`PhaseHandler.java:804-805`, `BlockEffect.java:60-61`, `Player.java:130,2514-2519`, `GameState.java:1218-1223`.

## The cards and what they lacked

The open-plan row counted 72 cards; the baseline of this work had 123 inconclusive cards, of which 32 target or test
game state a placed board does not have. The rest are cost shapes (R25g, R25h), X-bound targets, unported properties
(`MonoColor`, `NotedColor`, `greatestPower`, `BorderColorSilver`, `YourTeamCtrl`) and colorless or ability stack targets
(R25m). Two causes stacked for most of the 32: the probe never played the state, and the engine read the property as a
type, so it matched nothing even where the state existed (`propertyMatches`' fallback, a silent GO-7 miss).

| State                                 | Cards                                                                                                                                                                         | Property                                                                    |
| ------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| Dealt or was dealt damage this turn   | Avenging Arrow, Executioner's Swing, Crushing Pain, Fatal Blow, Mirrodin Avenged, Needle Drop, You Are Already Dead, Treacherous Greed (a `Sac` cost), Reciprocate (to "you") | `dealtDamageThisTurn`, `wasDealtDamageThisTurn`, `dealtDamageToYouThisTurn` |
| Blocked, or was blocked by, this turn | Sizzling Barrage, Glyph of Delusion, Time to Reflect, You Cannot Pass!                                                                                                        | `blockedThisTurn`, `blockedValidThisTurn`, `blockedByValidThisTurn`         |
| Combat in progress                    | Aang's Defense (its own blocker), Glyph of Destruction, Mirror Strike, Surge of Righteousness, Confront the Assault (`IsPresent$`)                                            | `blocking`, `attacking`, `unblocked`, `attackingYou` (already read)         |
| Entered this turn, or since your last | Cradle to Grave, Premature Burial, Grim Return, Reenact the Crime                                                                                                             | `ThisTurnEntered`, `EnteredSinceYourLastTurn`, `ThisTurnEnteredFrom_`       |
| An Aura attached                      | Aura Graft, Enchantment Alteration, Cut the Earthly Bond, Venomous Vines                                                                                                      | `AttachedTo`, `enchanted` (already read)                                    |
| Spell history                         | Laquatus's Disdain (cast from a graveyard), Second Guess (second spell this turn)                                                                                             | `wasCastFromGraveyard`, `SecondSpellCastThisTurn`                           |
| A token, a +1/+1 counter, a face-down | For the Common Good, Mutant's Prey, Break Open                                                                                                                                | `token`, `counters_GE1_P1P1`, `faceDown` (already read)                     |
| Castable from its graveyard only      | Haakon, Stromgald Scourge                                                                                                                                                     | not a property: a graveyard `MayPlay$` static (Gaps)                        |

## Engine: the turn-history properties

`validhistory.go`'s `turnHistoryMatches` answers them; `propertyMatches` asks it after its own switch and before the
keyword, color and type fallbacks, so no existing case is shadowed.

| Property                                             | Go                                                                                                                                         | Java                                                                        |
| ---------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------- |
| `ThisTurnEntered`, `ThisTurnEntered[Your]<Zone>`     | `zoneEntered` and `zoneEntryTurn` the current turn; the zone form also needs the card in the source controller's `<Zone>`, owned when Your | `CardProperty.java:969-986`, `Card.enteredThisTurn` (`Card.java:2056-2058`) |
| `EnteredSinceYourLastTurn`                           | `zoneEntryTurn` past the source controller's new `Player.lastTurnNr`                                                                       | `CardProperty.java:951-954`, `Player.java:2514-2519`                        |
| `dealtDamageThisTurn`                                | `Card.damageDealtThisTurn` above 0                                                                                                         | `:1177-1180`, `Card.getTotalDamageDoneBy` (`Card.java:6104-6106`)           |
| `wasDealtDamageThisTurn`                             | `Damage.Sources` not empty                                                                                                                 | `:1156-1159`, `GameEntity.getAssignedDamage` (`GameEntity.java:377-392`)    |
| `dealtDamageToYouThisTurn`                           | an entry of `Game.playerDamageThisTurn` with this object (same `CardID`, same `zoneStamp`) as source and the source controller as target   | `:1144-1147`, `CardDamageHistory.getDamageDoneThisTurn` (`:256-281`)        |
| `blockedThisTurn`                                    | the new `Card.blockedThisTurn` not empty                                                                                                   | `:1199-1202`                                                                |
| `blockedByThisTurn`, `gotBlockedThisTurn`            | `Card.blockedByThisTurn` not empty                                                                                                         | `:1207-1210`, `:1615-1616`                                                  |
| `blockedValidThisTurn X`, `blockedByValidThisTurn X` | a card of the list matches `X`, read with the card's controller as source controller                                                       | `:1617-1646`, restriction half                                              |
| `wasCastFrom[Your\|Their]<Zone>[ByYou]`              | `wasCast`, `castFrom` the zone, the card's owner the zone's player for Your/Their, `castBy` for ByYou                                      | `:1929-1949`                                                                |
| `SecondSpellCastThisTurn`                            | `Game.castThisTurn[1]` is this object: same `CardID`, and the new `castRecord.stamp` equals the card's `zoneStamp`                         | `:924-931`, `equalsWithGameTimestamp`                                       |

New state, each copied by `Game.Clone` (nil slices stay nil, `TestCloneAllocationsStayBounded` unaffected):

| State                  | Why                                                                                                                                                                                                        |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Card.blockedThisTurn` | `Card.getBlockedThisTurn`, the blocker's mirror of `blockedByThisTurn`; written by the same `recordBlockedBy` (Java writes both at `PhaseHandler.java:804-805`, `BlockEffect.java:60-61`), cleared with it |
| `Player.lastTurnNr`    | `Player.getLastTurnNr`, set where Java sets it, as the player's own turn is cleaned up (`cleanupPlayerRecords`)                                                                                            |
| `castRecord.stamp`     | the object identity Java's `equalsWithGameTimestamp` compares, so a second spell that resolved into a permanent is no longer that spell                                                                    |

No controller method was added.

`Damage.Heal` (a regeneration) now keeps `Damage.Sources`: `Card.healDamage` (`Card.java:6181-6185`) clears the marked
damage and the assigned-damage map, never `damageReceivedThisTurn`, which `DamagedBy` and `wasDealtDamageThisTurn` read.
It cleared them before, so a regenerated creature lost its `DamagedBy` record.

`cloneSpec` accepted only `ThisTurnEnteredFrom_<Zone>` among the `ThisTurnEntered*` names; it now refuses only one that
names no zone (`thisTurnEnteredKnown`). Sakashima's Protégé (`Choices$ Permanent.ThisTurnEntered+Other`) plays.

`wasCastFrom...` is read by about 170 corpus lines, most of them ETB conditions ("if you cast it from your hand"); all
of them matched nothing before. Scion of Vitu-Ghazi's trigger now fires and its `Populate$` is refused (below).

## Probe: staged variants

`internal/smoke/staged.go` appends `stagedVariants` after every existing variant, tried for any card still not cast, so
no card an earlier variant cast changes. Each plays its state through the engine; nothing is poked into a field.

| Variant                      | Played                                                                                                 | For                                                                   |
| ---------------------------- | ------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------- |
| `stageAura`                  | Pacifism cast and resolved                                                                             | `Aura.AttachedTo ...`, `enchanted`                                    |
| `stageEntered`               | Murder cast at the first creature offered, then Grizzly Bears cast and resolved                        | `ThisTurnEntered`, `EnteredSinceYourLastTurn`, `ThisTurnEnteredFrom_` |
| `stageSpells`                | two Lightning Bolts cast, left on the stack                                                            | `SecondSpellCastThisTurn`                                             |
| `stageFlashback`             | Think Twice cast from the graveyard for its Flashback cost, left on the stack                          | `wasCastFromGraveyard`                                                |
| `stageDamaged`               | Electrickery (1 damage to a creature the probe player does not control)                                | `wasDealtDamageThisTurn` on a creature that lived                     |
| `stageToken`, `stageCounter` | Raise the Alarm; Battlegrowth on the first creature offered                                            | `token`, `counters_GE1_P1P1`                                          |
| `stageFaceDown`              | the opponent's turn walked to its first main phase, Soul Summons cast by the opponent                  | `faceDown+OppCtrl`                                                    |
| Six combat moments           | a turn walked (`walk`, `timing.go`) to its declare blockers step or second main phase, blocking or not | the combat and damage rows above                                      |

The combat moments: own declare blockers step with blocks, own second main phase with and without blocks, the opponent's
declare blockers step and second main phase, each with and without blocks. A blocking defender uses the new
`Controller.blockEvery`: every creature it can block with blocks, the attackers taken in turn, so a Wall, a Zombie or a
legendary creature placed for the card blocks too. `blockAll`, the existing combat variant's policy, is unchanged.

Before the combat walk the state-free part of each target spec is placed on both sides (`stateSpecs`,
`addStateTailored`): the spec with its state properties stripped (`Creature.Black+attacking` searches `Creature.Black`),
and the nested spec of a `blockedValidThisTurn`/`blockedByValidThisTurn` (`Zombie`, `Creature.Legendary`). A spec left
with nothing a bear lacks, or a nested `Targeted`/`ParentTarget`, places nothing.

A staged spell that cannot be cast or resolved makes the variant inconclusive with that reason: it says nothing about
the card. Once the card is cast, any engine refusal is the outcome as for every other variant.

## Whole corpus, before and after

`crucible card-smoke -all -workers 8 -list`, same corpus:

| Outcome      | Before | After |
| ------------ | -----: | ----: |
| plays        |  30656 | 30686 |
| refused      |   2926 |  2931 |
| inconclusive |    123 |    88 |
| judged       |  33582 | 33617 |
| % plays      |   91.3 |  91.3 |

37 cards changed.

| Change                      | Cards                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| inconclusive -> plays (30)  | Aang's Defense, Avenging Arrow, Break Open, Confront the Assault, Cradle to Grave, Crushing Pain, Cut the Earthly Bond, Enchantment Alteration, Even the Odds, Executioner's Swing, Fatal Blow, Grim Return, Laquatus's Disdain, Lay Down Arms, Mirrodin Avenged, Mirror Strike, Molder, Mutant's Prey, Needle Drop, Premature Burial, Reciprocate, Reenact the Crime, Second Guess, Sizzling Barrage, Surge of Righteousness, Time to Reflect, Treacherous Greed, Venomous Vines, You Are Already Dead, You Cannot Pass! |
| inconclusive -> refused (5) | see below                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| plays -> refused (1)        | Scion of Vitu-Ghazi: `CopyPermanent: Populate$ not resolvable yet`. Its "if you cast it from your hand" trigger (`ValidCard$ Card.wasCastFromYourHandByYou+Self`) never fired before                                                                                                                                                                                                                                                                                                                                      |
| refused -> plays (1)        | Sakashima's Protégé: `Choices$ Permanent.ThisTurnEntered+Other` was refused by `cloneSpec`                                                                                                                                                                                                                                                                                                                                                                                                                                |

Even the Odds, Lay Down Arms and Molder are not game-state cards: a staged board happens to give them what they need
(more creatures on the opponent's side; a token of mana value 0; Pacifism at mana value 2 for an X of 2).

| Card                              | Refusal, at resolution                                                |
| --------------------------------- | --------------------------------------------------------------------- |
| Aura Graft                        | `Attach: Move$ with Object$ not resolvable yet` (its `ReEnchant` sub) |
| For the Common Good               | `PumpAll: Duration$ "_" not resolvable yet`                           |
| Glyph of Destruction              | `Pump: AtEOT$ not resolvable yet`                                     |
| Role Reversal, Shifting Loyalties | `ExchangeControl: TargetsWithSameCardType$ not resolvable yet`        |

## Gaps

- Glyph of Delusion stays inconclusive: its sub-ability targets `Creature.blockedByValidThisTurn ParentTarget`. Java
  falls back to `AbilityUtils.getDefinedCards` for a `Defined$` name (`CardProperty.java:1626-1630`, `:1641-1645`); the
  valid evaluator is not given the ability, so `Targeted` and `ParentTarget` match nothing (SwitchBlock reads its own
  `blockedByValidThisTurn Targeted`, `switchblockeffect.go`).
- Haakon, Stromgald Scourge stays inconclusive: its `MayPlay$` static has `EffectZone$ Graveyard`, and graveyard statics
  grant no cast permission; a probe variant would only show it once they do.
- `Card.damageDealtThisTurn` is not cleared when a permanent leaves the battlefield; Java's new object has a new
  `CardDamageHistory`. A creature flickered after dealing damage still reads `dealtDamageThisTurn`.
- The fixture loader places setup cards with `NewCard`, so none has entered its zone; Java's `GameState` moves
  battlefield cards into play (`GameState.java:1218-1223`), setting their turn in zone to the current turn. A scenario
  reading `ThisTurnEntered` on a setup card would disagree with the oracle. No scenario does.
- `ThisTurnEntered[Your]<Zone>` reads the card's current zone, not the zone's added-this-turn list: a card that entered
  the zone this turn, left and came back is the same answer either way, a card that left for good is out of scope (it is
  no longer in the zone to be targeted).

## Tests

Module tests in `turnhistoryproperties_test.go`: after a blocked combat the attacker and blocker read every combat
history property, an unblocked attacker `dealtDamageToYouThisTurn` for its defender only, `blockedValidThisTurn` reads
the nested spec, the `Targeted` fallback matches nothing, a regeneration keeps the damage record, cleanup clears all of
it and a clone keeps its own; `blockedThisTurn` ends with the object; `ThisTurnEntered` for a cast creature and a
creature put into a graveyard, not for a placed one, its zone and `Your` forms, cleared next turn;
`EnteredSinceYourLastTurn` across the turn boundary for both players; `wasCastFrom` from a graveyard (Flashback) and a
hand, the Your, Their and ByYou forms, kept onto the battlefield; `SecondSpellCastThisTurn` only while the second spell
is that object. `copyeffects_test.go`'s Clone refusal case now names `ThisTurnEnteredNowhere`. Probe:
`gamestatevariants_test.go` (23 cards that now play, three that are refused, Glyph of Delusion inconclusive). Scenarios
`valid-property-sizzling-barrage-hits-the-creature-that-blocked` and
`valid-property-coal-stoker-cast-from-hand-adds-mana`.
