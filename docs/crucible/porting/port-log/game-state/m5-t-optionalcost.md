# M5 batch T1: optional additional costs (`Mode$ OptionalCost`, Teamwork)

## Optional costs as a spell is cast

Java has no `StaticAbilityOptionalCost` class: `GameActionUtil.getOptionalCostValues` (`GameActionUtil.java:409-535`)
builds the list for the spell being cast, `PlaySpellAbility.chooseOptionalAdditionalCosts` (`:140-154`) lets the caster
pick, and `GameActionUtil.addOptionalCosts` (`:537-569`) adds each pick to the spell's cost and records its
`OptionalCost` kind on the cast `SpellAbility`.

| Piece          | Port                                                                                                                                                                                                                                                                                                                                                                       |
| -------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| List           | `optionalCostOptions` (`optionalcost.go`): `Mode$ OptionalCost` lines of the card and of every static-ability host (`staticHostsWith`), conditions held, `ValidCard$`/`ValidSA$`/`Activator$` matched; then `K:Teamwork:N` (`CostTeamwork`, tap creatures with total power N). A card on the battlefield has none (`:420`). Statics first, keywords after, as Java's list. |
| Choice         | `chooseOptionalCosts`, called by `castSpell` before `chooseKicker`: one `ConfirmPayCost` per option whose non-mana part is payable (`unlessPayable`), the question Kicker asks. No new `PlayerController` method. A mana part is not pre-checked: an unpayable pick fails the cast, as an unpayable kicker does.                                                           |
| Mana           | `castOpts.optional`: its mana joins the cost in `castCost` (after Kicker), so a free cast still pays it (CR 118.9d, `castOpts.paysNoMana`). `Waterbend<N>` lets the caster tap artifacts and creatures for up to N generic of the whole cost after Convoke/Delve (`waterbendCast`, `CostAdjustment.java:329-348`).                                                         |
| `ReduceColor$` | Defilers: `optionalChoice.reduced` takes one shard per paid line off the cost after `spellCost` (`CostAdjustment.java:238-242`), in `ManaCostBeingPaid.decreaseShard`'s order: the plain colored shard, a two-color hybrid, `{2/C}`, a colorless hybrid, a Phyrexian one. Never generic.                                                                                   |
| Other parts    | Checked before the mana (`optionalPartsPayable`), paid after it in all three cast branches, Aura included (`payOptionalParts`, through `payUnlessParts`). Reveal/Behold now pick the revealed cards (`ChooseCardsForEffect`), Teamwork the creatures to tap.                                                                                                               |
| Record         | `Card.castOpt` (`castOptional`): the kinds (`optGeneric`, `optTeamwork`; a `ReduceColor$` line records no kind a reader asks about) and the `Collected` and `Revealed` paid lists. Set as the spell is cast, cleared with `kicker` by a failed cast and by `Game.Move`, deep-copied by `Game.Clone`.                                                                       |

Readers:

| Reader                                                              | Port                                                                                                                                                                                                                                                                                                            |
| ------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Count$OptionalGenericCostPaid.A.B`, `Count$Teamwork.A.B`           | `optionalBranch` over `Game.spellOptional`: the record while the card is being cast (`castPending`, so Pyrrhic Strike's `CharmNum$` and Bite Down on Crime's `ReduceCost` read it) or on the stack. A permanent's own abilities read nothing (`AbilityUtils.java:1700-1706` reads the ability's own root).      |
| `Condition$ OptionalCost`, `ConditionOptionalPaid$`                 | `conditionValueMet` (`SpellAbilityCondition.java:118-128`, `:283-284`; default True). `rejectUnreadCondition` replaces the blanket `Condition$` refusal in Destroy, GainLife, ChangeZoneAll, Effect, Fog and ControlPlayer, which therefore also read `Condition$ Kicked` now.                                  |
| `CastSA>` amounts                                                   | `castSAValue`: `Count$OptionalGenericCostPaid`, `Count$Teamwork`, `Collected$`, `Revealed$` against `Card.castOpt` (Katara, Spotlight Falcon, Vitu-Ghazi Inspector). A card not cast reads 0, as Java's unswitched context does (`AbilityUtils.java:4036-4040`, `:729`). Other `CastSA>` heads stay unresolved. |
| `Collected$`, `Revealed$`, `Defined$ Collected`, `CastSA>Collected` | `contextHeads` and `definedCards`. `collectEvidence` returns what it exiled.                                                                                                                                                                                                                                    |
| `Count$Presence_<Type>.A.B`                                         | `presenceCount` (`AbilityUtils.java:2180-2192`): cast, and a revealed card of the type or a controlled permanent of the type. Java counts the battlefield as the amount is read, not as the spell was cast (a Forge defect, `forge-java-defects.md`); reproduced.                                               |
| `ReduceCost` `Relative$`                                            | Read on the spell's own line only (`costModStatus`): the static's host is then the spell the amount is read against. On any other host the line stays unresolvable.                                                                                                                                             |

Refused (the cast is declined, the reason left for `TakePendingError`, GO-7):

| Card                  | Shape                                 | Reason                                                                                                                                                                                                    |
| --------------------- | ------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Dragon's Fire         | `RevealOrChoose<1/Dragon>`            | `parseUnlessCost` has no such part                                                                                                                                                                        |
| Celestial Reunion     | `ChooseCreatureType<1> Behold<2/...>` | `ChooseCreatureType` cost part not ported                                                                                                                                                                 |
| Redemptor Dreadnought | `ExileFromGrave<1/Creature>`          | `CostExile` links the exiled card to the spell, and the permanent keeps the link (`GameAction.java:480-481`, CR 607.2q); `exiledWith` is stamp-keyed and the link breaks on the stack-to-battlefield move |

Not ported:

- Inline `CheckSVar$`/`ConditionCheckSVar$` expressions (`Count$Presence_Dragon.1.0`, `CastSA>Count$...` written in the
  param rather than an SVar): Draconic Roar, Foul-Tongue Invocation, Orator of Ojutai, Scaleguard Sentinels
  (`etbCounter`), Dragonlord's Prerogative (`R:` line), Graven Archfiend. `checkSVarMatches` looks the value up as an
  SVar name and reads the condition as unmet. Needs `compile.addInlineAmounts` extended past the four Layer 7 keys, a
  golden AST regeneration.
- `CastWithFlash` `ValidSA$ Spell.Teamwork` (Quantum Reduction): `Spell.Teamwork` stays in `saUnresolvedPrefixes`; the
  out-of-timing gate has no "flash only if Teamwork is chosen" branch. `Card.Self+Teamwork` (Team Tactics, Cruel
  Alliance) and `Taps`'s `Teamwork$` (Agent Maria Hill) are not read either.
- `CollectEvidence` as an activation cost (Cryptex's `T CollectEvidence<3>`): `cost.ActivationShape` has no such part.
- Territorial Strike's `Duration$ Perpetual` pump of `RevealedCards`; Dragon's Fire's `Revealed$CardPower`.
- A spell copy does not carry the record (nor `kicker`), CR 707.10.

Tests: `optionalcost_test.go`. Fixtures: `optional-cost-cinder-strike-blight-paid-deals-four`,
`optional-cost-cinder-strike-declined-deals-two`.
