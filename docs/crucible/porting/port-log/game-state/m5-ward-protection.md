# M5 batch D: Ward, `UnlessCost$` shapes, static restrictions, player Protection

## `UnlessCost$` and Ward cost shapes

`parseUnlessCost` (`unlesscost.go`) now reads these parts beside mana, `PayLife`, `Discard<N/Card>`, one `Sac`,
`Return`, `Reveal`, `DamageYou`, `PayEnergy`, `Draw<N/You>` and `AddCounter<N/Type>` on the source (those already
landed; the old "Not ported yet" counts for `Reveal` (22) and `AddCounter` (4) were stale).

| Part                                   | Payment                                                                                                                 |
| -------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `ExileFromGrave<N/Type>`               | payer picks N matching graveyard cards (`chosenUnlessCards`), `exileFromGraveyard`                                      |
| `tapXType<N/Type>`                     | `tapTypeCandidates`, `ChoosePermanentsToTap`, `tapChosenPermanents`                                                     |
| `AddCounter<N/Type/Spec>`, `Blight<N>` | payer picks a card matching `Spec` that may receive the counters; `Blight` is M1M1 on `Creature.YouCtrl` (`CostBlight`) |
| `AddCounterYou<N/Type>`                | the payer gets the counters through `countersReplaced`                                                                  |
| `CollectEvidence<N>`                   | payer exiles graveyard cards of total mana value at least N (`CostCollectEvidence`)                                     |
| `Waterbend<N>`                         | N generic mana; the payer may tap untapped artifacts and creatures for {1} each (`waterbendReduce`)                     |
| `Draw<N/Player...>`                    | every player the spec names that may draw (`unlessDrawers`; `Player.targetedBy` names every player)                     |

An illegal pick (not a subset, wrong count) falls back to the first options: a payer cannot back out of a payment
already half made; payability was checked first.

`expandUnlessCost` evaluates SVars before parsing (`AbilityUtils.calculateUnlessCost`, `PlaySpellAbility.payManaCost`):

| Text                                     | Meaning                                      |
| ---------------------------------------- | -------------------------------------------- |
| a bare SVar name other than X (`Y`, `Z`) | that much generic mana, or of `UnlessColor$` |
| mana symbol `X`                          | generic mana, the amount of SVar `X`         |
| `Name<X/...>` (`PayLife`, `PayEnergy`)   | the SVar's amount as the part's count        |

An SVar this port cannot evaluate (`Targeted$CardManaCost`, `TriggeredCard$CardManaCost`, `Count$ChosenNumber`) is an
error at resolution (GO-7). `Ward:X`-style Ward lines evaluate X against the warded card's own SVars when Ward's trigger
is built, and again when it resolves.

`Ward:A:B` (`Ward.parse` splits on `:`; 1 real line, `Discard<1/Card>:2`) is a choice of one cost
(`pickUnlessAlternative`, `ChooseOption`) among those whose non-mana parts are payable; none payable counters.

Not ported: the `CollectEvidence` trigger mode (no card's `Mode$ CollectEvidence` trigger fires when evidence is paid),
`Draw<N/Player.targetedBy>` limiting to the ability's own targets, an `UnlessCost$` naming `DefinedCost_*`,
`RemoveAnyCounter`, `PutCardToLibFromGrave`, `Discard<N/Hand>`.

## Ward on abilities, copies and retargets (ADR-0028, edited in place)

`checkWardTriggersForAbility` is `checkWardTriggers` for an activated or triggered ability. `Ability.wardItem` (a
`StackItemID`) is the thing Ward's `Counter` removes; `counterEffect` drops that stack item and moves no card. Call
sites: `pushTriggeredAbilities` (the push every activated and triggered ability takes, reading the pushed item's own ID
off the stack), `copySpell`'s copies (`copiedTargets.host`) and ChangeTargets' rewritten items (`retargeted`, spell or
ability by `Ability.spell`). Tests: `wardstack_test.go`; fixtures `ward-activated-ability-*`.

## Static restrictions

- `Mode$ CantPutCounter` now also refuses a counter placed straight on the entity: an `AddCounter<N/Type>` activation
  cost (`ActivateAbility`, `ActivateManaAbility`; `CostPutCounter.canPay`), `Poison`, `Radiation`, `TimeTravel`'s add
  and `Empower` (`GameEntity.addCounter`). Left unread: `MakeCard` and token/ETB counters (`etbCounter`),
  loyalty/defense on entering. Tests: `cantputcounterdirect_test.go`.
- `staticConditionsMet` evaluates `IsPresent$`/`IsPresent2$`/`CheckSVar$` through the trigger side's `isPresentMatches`
  and `checkSVarMatches` instead of skipping the line, for every static mode using it.
- `Mode$ CastWithFlash` reads `ValidSA$` through `spellAbilityMatches` (so `Spell.Hero`, `Activated.Equip`,
  `Activated.Loyalty` evaluate) and `ActivateAbility` asks `activatesWithFlash` before its sorcery-speed gate (`Equip`,
  loyalty abilities). `IsPresent$`/`CheckSVar$` lines now apply. Still skipped: `ValidSA$` naming `XCost`, `IsTargeting`
  (Java asks them after targets and X are chosen), `MayFlashCost`/`MayFlashSac`. Tests: `castflashshapes_test.go`.
- Not ported: `Mode$ CantTarget` with `SourceCanOnlyTarget$` (Wall of Shadows; needs the root ability's `ValidTgts$` at
  `cardCantBeTargetedBy`, whose callers pass no ability) and `EffectZone$ Stack` (Enthralling Hold; needs the spell
  being cast as a static host before it reaches the stack); `canTarget`'s multi-target params.

## Player Protection

`playerEnchantLegal` (action.go) reads `playerRefusesAttach`: a player's Protection lines refuse an attached Curse
(`PlayerFactoryUtil.java:42-52`), each line independently; the Curse falls off as a state-based action. Test:
`playerprotectionattach_test.go`. Not ported: `Protection:ChosenName`, `ChosenType` (skipped by `keywordTokens`) and the
`Player.Opponent` forms (`Protection.java:13-27` "ControlledBy").
