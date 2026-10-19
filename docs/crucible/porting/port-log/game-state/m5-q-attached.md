# Port Log — Game State: M5 batch Q, Attached and pay-life replacements

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `triggerpack.go`, `replacementfaces.go`,
  `replacementcost.go`, `replacemententry.go`, `amountheads.go`

Follows [`m5-control-2.md`](m5-control-2.md) and [`m5-replacement-3.md`](m5-replacement-3.md).

## Mode$ Attached

`checkAttachedTriggers` (`triggerpack.go`) is `TriggerAttached`: `Card.attachToEntity` fires it after the
`Event$ Attached` replacements (`Card.java:3949-3953`). `Game.attachTo` (card host) and the new `Game.attachToPlayer`
(an "Enchant player" Aura, `castspell.go`) call it; the fixture loader's `Game.Attach` does not.

| Param | Java | Go |
| --- | --- | --- |
| `ValidSource$` | attachment, host card as source | `triggerCardMatches` |
| `ValidTarget$` | card or player, host card as source | `attachTargetMatches`: `Matches` for a card, `matchesPlayerSpec` for a player |
| `TargetRelativeToSource$` | target, the attachment as the source card | `attachTargetRelative`: same, with `Game.relativeAmounts` set to the trigger host SVars |
| triggering `Source` | `AbilityKey.AttachSource` | `triggeredObjects.source`, so `LoseControl$ UntilSourceUnattached` registers |
| triggering `Target` | `AbilityKey.AttachTarget` | `triggeredObjects.target`; `Defined$ TriggeredTarget` / `TriggeredTargetLKICopy` |

`TargetRelativeToSource$ ...cmcLEX` (Eriette, the Beguiler) is `AbilityUtils.calculateAmount(source, "X", trigger)`: the
SVar is read off the trigger (Eriette: `Count$CardManaCost`), the amount off the source card (the Aura).
`compareOperand` reads `Game.relativeAmounts` after `Game.relativeFace` (the `CantBlockBy` equivalent).

Corpus: Siona, Enormous Energy Blade and Eriette resolve. Assimilation Aegis' `Clone` with `Duration$ UntilUnattached`
and `CloneTarget$ TriggeredTargetLKICopy` is not ported (Layer 1 duration), and Dinosaur Headdress' `Static$ True`
Attached trigger needs Craft.

| Scenario | Proves |
| --- | --- |
| `attached-enormous-energy-blade-taps-the-equipped-creature` | equip fires it, `TriggeredTargetLKICopy` is the host |
| `attached-siona-makes-a-token-when-an-aura-attaches-to-your-creature` | Aura resolution attaches and fires it |
| `attached-siona-ignores-an-aura-on-an-opposing-creature` | `ValidTarget$ Creature.YouCtrl` fails |
| `attached-siona-ignores-an-aura-attached-to-a-player` | player host fires it, a card spec does not match |
| `attached-eriette-the-beguiler-steals-a-permanent-no-pricier-than-the-aura` | `cmcLEX` with X = the Aura's mana value, control until unattached |
| `attached-eriette-the-beguiler-leaves-a-pricier-permanent-alone` | `cmcLEX` fails |

## Event$ Attached: Psychic Paper

The replacement already ran (`attachedReplaced`); what was missing was a way to pick a card name out of the whole
database. `queue optionname <text>` answers `ChooseOption` by value.
`replacement-attached-psychic-paper-names-the-equipped-creature` runs `NameCard` then `ChooseType` as the Paper
attaches; GameState text names a card by its paper name, so `TestPsychicPaperRenamesTheEquippedCreature` reads the new
name (`SetName$ ChosenName`) and type (`AddType$ ChosenType`) off the game. Paleontologist's Pick-Axe's back face,
Dinosaur Headdress, needs Craft and `ExiledWith`: not ported.

## Moved to the battlefield: pay any amount of life

`payReplacementLifeX` (`replacementcost.go`) pays `Cost$ Mandatory PayLife<X>` inside `runReplacementChain`, the only
`Cost$` a `ReplaceWith$` ability carries (3 corpus lines). The controller chooses 0 up to the life total
(`ChooseNumber`), capped by `XMax$` (Nameless Race: `SVar:Limit:SVar$Active/Plus.Buried`), none while a static forbids
paying life. The life leaves before the ability resolves, X is the ability's `xManaCostPaid` (`Count$xPaid`),
`StoreSVar` keeps it as `LifePaidOnETB`, and `costPaid` stops `Registry.payTriggeredCost` from asking again. A number
outside the range is an error and takes no life. `entryUpdatedShape` accepts the `StoreSVar` shape of
`ReplacementResult$ Updated`.

| Scenario | Proves |
| --- | --- |
| `replacement-moved-minion-of-the-wastes-pays-five-life-and-is-a-five-five` | 20 to 15 life, the CDA 5/5 survives |
| `replacement-moved-minion-of-the-wastes-paying-no-life-dies-as-a-zero-zero` | X 0: 0/0 dies |
| `replacement-moved-nameless-race-pays-up-to-the-white-permanents-limit` | `XMax$ Limit` = 2 (permanent + graveyard card) |
| `replacement-moved-nameless-race-refuses-paying-over-the-limit` | 3 against limit 1 is an error, no life paid |

Phyrexian Processor shares the shape; its token ability reads `LifePaidOnETB` and has no scenario of its own. Module
test: `replacementpaylife_test.go` (range, `XMax$`, a `Cost$` of another shape).

## Count$ResolvedThisTurn

`AbilityUtils.java:1843`: `SpellAbility.getResolvedThisTurn`, the host's count of resolutions of that ability this turn.
`Registry.resolve` now counts every non-Spell ability resolution (`AbilityUtils.resolve` counts sub-abilities too,
`:1315`), not only triggers; `countValue` reads the count of `Game.resolving.Params`. Sephiroth, Fabled SOLDIER's
`DBTransform` (`ConditionCheckSVar$ X`, `EQ4`) transforms on the fourth death trigger and gets his emblem.
`TestResolvedThisTurnTransformsSephirothOnTheFourthDeath` (the emblem is a Command card GameState text cannot name) and
`count-resolved-this-turn-sephiroth-stays-after-three-deaths`.

## Fixture verbs and helpers

| Change | Where |
| --- | --- |
| `queue optionname <text>`, `queue numberchoice <n>` | `internal/fixture/actions.go` |
| `ScriptedController.option` is `[]optionAnswer`; `QueueOptionNamed` | `control.go` (`QueueOption(i)` keeps its signature) |
| `Game.relativeAmounts`, `triggeredObjects.target` | `game.go`, `ability.go` |
| `Game.attachToPlayer` | `replacementfaces.go` |

## Not landed

- Animate `Duration$ AsLongAsControl` (`Abilities$`, `TargetsForEachPlayer$`), Charm modes and copies keeping their
  parent's targets, a card's own `Origin$ Graveyard` triggers, graveyard exits and battlefield-to-library moves outside
  `moveByEffect`, `Static$ True` ChangesZone triggers on the remaining paths: unchanged.
- `Event$ RollDice` `DicePTExchanges` (Vedalken Squirrel-Whacker): unchanged.
- `Draw` lines skipped by `drawReplacementMatches`: `ValidCause$` (only Unpredictable Cyclone, a cycling cause the draw
  does not carry), `FirstExtraCardDrawnThisTurn$`, the `Optional$ Prevent$` line: unchanged.
- `TurnFaceUp` abilities of Gift of Doom (`DB$ Attach | Choices$`) and Vesuvan Shapeshifter (`Duration$ UntilFacedown`),
  Ludevic: unchanged.
- Assimilation Aegis (see above), Dinosaur Headdress.
