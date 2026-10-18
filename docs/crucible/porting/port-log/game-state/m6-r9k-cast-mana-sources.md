# Cast mana sources, prepare, Siege modes, random and secret choices, activation cost triggers, discard replacements (R9k)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Decision:** [ADR-0042](../../../adr/0042-mana-pool-unit-sources.md) — the mana pool records each unit's source card
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `mana.go`, `manaability.go`,
  `activatemanaability.go`, `manaeffect.go`, `manareflectedeffect.go`, `manapay.go`, `castspell.go`, `amountheads.go`,
  `prepare.go` (new), `alterattributeeffect.go`, `castoptions.go`, `token.go`, `valid.go`, `replacemententry.go`,
  `genericchoiceeffect.go`, `memory.go`, `choosecardeffect.go`, `chooseplayereffect.go`, `choosecoloreffect.go`,
  `choosetypeeffect.go`, `choosenumbereffect.go`, `namecardeffect.go`, `defined.go`, `activateability.go`,
  `triggerscan.go`, `replacementdiscard.go`, `game.go`, `card.go`
- **Java:** `AbilityUtils.java:1896-1908`; `ManaPool.java:228-250`; `AlterAttributeEffect.java:65-90`;
  `CopyPermanentEffect.java:276-313`; `Card.java:5845-5848`, `:6676-6694`; `CardProperty.java:116-117`, `:1861`;
  `CardStateProperty.java:79-80`; `GameAction.java:171-172`, `:1912-1932`; `ChooseGenericEffect.java:86-126`;
  `ChooseCardEffect.java:225-228`, `:275-288`; `ChoosePlayerEffect.java:47-58`; `Card.java:2127-2129`, `:2214-2216`;
  `ChooseColorEffect.java:46-87`; `ChooseNumberEffect.java:65-66`; `ChooseCardNameEffect.java:62-78`;
  `AbilityUtils.java:287-303`; `PlaySpellAbility.java:682`, `:727`; `MagicStack.java:124-171`, `:303-365`;
  `ReplaceMoved.java:79-90`, `:120-124`; `ChangeZoneEffect.java:1097`, `:1195-1199`, `:1259`

## What refused

BEFORE `card-smoke -all -workers 8 -list`: 30,691 plays, 2,925 refused, 0 hung, 91.3% of 33,616 judged.

| Gap                                                 | Cards | Before                                                                               |
| --------------------------------------------------- | ----- | ------------------------------------------------------------------------------------ |
| `Count$CastTotalManaSpent <valid>` past `Snow`      | 3     | Refused: the pool kept no source card                                                |
| Prepare (`AlterAttribute Attributes$ Prepared`)     | 40    | Refused: the as-enters chain and the attribute were not ported                       |
| Siege modes (`GenericChoice SetChosenMode$`)        | 15    | Refused in the entry chain                                                           |
| `Random$`/`AtRandom$`/`Secretly$`/`ColorsFrom$`/... | ~60   | Refused by each Choose effect                                                        |
| Activated-ability cost triggers                     | all   | Pushed during payment, under the ability                                             |
| Discard replacements other than Madness             | ~6    | Refused when they applied (Loxodon Smiter family, Library of Leng, Nephalia Academy) |

## What landed

| Piece                                    | Java                                                                 | Engine                                                                                                                                                                                                                                                                                                             |
| ---------------------------------------- | -------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Unit sources (ADR-0042)                  | `ManaPool.java:228-250`                                              | `Pool.units` beside the counters, one per unit in add order; a payment takes each decremented bucket's oldest units (`takeSpent`). Mana abilities, basic lands, rituals and ManaReflected record the card and its timestamp before costs move it (`manaSourceOf`)                                                  |
| `CastTotalManaSpent <valid>`             | `AbilityUtils.java:1899-1907`                                        | `payManaCostX` returns the spent units' sources; `castPaidSources` on the spell; the count matches each source as the same object, else its LKI (a sacrificed Treasure, tapped), else as it is                                                                                                                     |
| Prepare                                  | `AlterAttributeEffect.java:65-90`                                    | `prepare.go`: a token copy of the prepare face in the activator's exile (`preparedFrom`), linked from the creature (`prepared`); its controller may cast it (`castOptions`); casting it or leaving the battlefield unprepares; a waiting copy matches no valid string and survives 704.5d; `IsPrepared`/`prepared` |
| Enters prepared                          | `GameAction.java:171-172` (same object onto battlefield)             | `entersPrepared` admits `AlterAttribute Attributes$ Prepared` (Self) to the entry chain: the link survives `Move`                                                                                                                                                                                                  |
| Siege modes                              | `ChooseGenericEffect.java:120-122`, `CardProperty.java:116-117`      | `SetChosenMode$` records the choice's `SpellDescription$` (`Memory.SetChosenMode`); `Card.ChosenMode<X>`; `LockInText$` accepted; a `Pump` with only a description is a mode-only choice the entry chain runs                                                                                                      |
| Random picks                             | `Aggregates.random`                                                  | ChooseCard's `AtRandom$`: reservoir over the pool, `Amount$` cards. GenericChoice's `AtRandom$`: reservoir over the choices. ChoosePlayer's `Random$`. ChooseColor's `Random$`: WUBRG list, picks removed. ChooseNumber's `Random$`: `randomInt`                                                                   |
| Secret picks                             | `Card.java:2127-2129`, `:2214-2216`, `ChooseCardEffect.java:275-288` | `Secretly$` on ChooseCard, GenericChoice, ChoosePlayer and ChooseType: the same state as an open choice, only the display differs                                                                                                                                                                                  |
| `ColorsFrom$`, `ChooseFromDefinedCards$` | `ChooseColorEffect.java:46-52`, `ChooseCardNameEffect.java:62-78`    | `definedCardsInGame` resolves `Valid <spec>`/`Valid<Zone> <spec>` (`AbilityUtils.java:287-303`); colors among the cards (none: nothing chosen); names of the cards passing `ValidCards$`, split halves too, sorted                                                                                                 |
| Activation cost triggers                 | `PlaySpellAbility.java:682`, `MagicStack.java:124-171`               | `ActivateAbility` holds the triggers its costs fire (the cast's `castExits` list) once the mana is paid, pushes the ability, then one batch: held, AbilityCast, Cycled                                                                                                                                             |
| Discard replacements                     | `ReplaceMoved.java:79-90`                                            | `discardReplaced` runs the CR 616 walk (`runReplacements`, Optional$ asked); Madness as before, any other line whose chain moves the replaced card runs that chain                                                                                                                                                 |

## Design decisions

| Decision                                                     | Why                                                                                                                                                                                        |
| ------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Pool keeps counters and adds a unit list (ADR-0042)          | Every tested payment rule stays; an empty pool clones with no allocation                                                                                                                   |
| Source read as the same object, else LKI                     | Java's `Mana` points at the battlefield `Card` object; the stamp taken before the mana ability's costs tells a sacrificed Treasure from the card now in the graveyard                      |
| Prepare as a creature-copy link, not an Effect card          | The effect's three parts (MayPlay$ static, static cast trigger, leaves-play command) are each one line on the link; no new command-zone card kind                                          |
| Prepared copy is a token in exile matching no valid string   | `CopyPermanentEffect.getProtoType` makes a token; `Card.isValid` filters it (`Card.java:5845-5848`); 704.5d exempts it (`GameAction.java:1922`)                                            |
| Entry replaced after enters-prepared keeps the copy castable | Java only unprepares through a leaves-play command (forge-java-defects.md); reproduced                                                                                                     |
| ChangeZone `AtRandom$` stays refused                         | Java sorts the fetch list by `CardView.toString` (zone text, translated name, Java card id) before the draw (`ChangeZoneEffect.java:1097`); the same RNG draw would pick another card here |
| ChooseType `AtRandom$` over `Type$` lists stays refused      | Java's subtype lists iterate HashSets (`CardType.java:871-873`)                                                                                                                            |
| Activation hold begins after the mana is paid                | No later cost part fails, so the hold always ends; an enclosing hold is set aside and put back                                                                                             |
| Library of Leng / Nephalia Academy shuffle before topdecking | Java does (`ChangeZoneEffect.java:1259`, scripts lack `NoShuffle$`); reproduced and reported (card-script-defects.md)                                                                      |

## Refused (GO-7)

| Shape                                                                                                      | Error                                                             |
| ---------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| `ChangeZone AtRandom$` (Make a Wish, Call from the Grave, ...)                                             | `ChangeZone: AtRandom$ not resolvable yet`                        |
| `ChooseType AtRandom$` over a `Type$` list; `TypesFromDefined$`; `MostPrevalentInDefinedZone$`             | the effect's own error                                            |
| `ChooseNumber Secretly$` (Highest$/Lowest$ over a `HashMap<Player>`)                                       | `ChooseNumber: Secretly$ not resolvable yet`                      |
| `NameCard AtRandom$` over the whole DB, or beside `ChooseFromDefinedCards$`                                | `NameCard: AtRandom$ ...`                                         |
| `GenericChoice AtRandom$ Urza`; `NumRandomChoices$`; `Guess$`                                              | `GenericChoice: ... not resolvable yet`                           |
| ChooseColor `Choices$` with `Random$` or `Exclude$` (Java's `Arrays.asList` throws, forge-java-defects.md) | `ChooseColor: Choices$ with Random$ or Exclude$ not resolvable`   |
| `Defined$ ValidAll ...`                                                                                    | `Defined$ ... not resolvable yet`                                 |
| Archangel of Strife (choices note cards for players); AlterAttribute other than Prepared as it enters      | `as-it-enters replacement ReplaceWith$ <Api> not resolvable yet`  |
| Wilt-Leaf Liege's chain (`DefinedPlayer$ ReplacedPlayer`, unrecorded for a Moved event)                    | `Defined$ "ReplacedPlayer" outside a replacement that records it` |

Not done (deferred, no shared cause with this row): the remaining as-enters chains (Reveal, RemoveCounter, Discard,
Sacrifice+Animate, SetState, Attach, Draft, ChangeZone imprint, RollDice+Animate); Codie's `Defined$ Valid ...` on
AlterAttribute (only ChooseColor and NameCard read `definedCardsInGame` yet); `ReplacedCard$CastTotalManaSpent` (Coin of
Mastery). No engine action enumerator offers exile casts; the prepared copy is castable through `CastSpell` like the
Airbend/Heist grants.

## Tests

Module: `castmanasources_test.go`, `prepare_test.go`, `siegemodes_test.go`, `randomchoices_test.go`,
`casttimetriggers_test.go` (activation), `discardreplacement_test.go`. Scenarios:
`activation-sacrifice-cost-dies-trigger-resolves-before-the-ability`,
`discard-replacement-loxodon-smiter-enters-when-an-opponent-makes-you-discard`,
`cast-mana-sources-bat-colony-counts-mana-from-caves`.

## Measured

AFTER: 30,803 plays, 2,810 refused, 1 hung, 91.6% of 33,614 judged. No plays → refused. 112 refused → plays, each by its
before-reason:

| Before-reason                                                                                             | Cards  |
| --------------------------------------------------------------------------------------------------------- | ------ |
| As-enters `AlterAttribute` (enters prepared)                                                              | 40     |
| `AlterAttribute` attribute `Prepared` elsewhere                                                           | 15     |
| ChooseCard `AtRandom$`                                                                                    | 15     |
| As-enters `GenericChoice` (Siege modes)                                                                   | 13     |
| ChoosePlayer `Random$`                                                                                    | 7      |
| `CastTotalManaSpent` Cave/Desert/Treasure (2 Branch)                                                      | 5      |
| ChooseNumber `Random$`                                                                                    | 4      |
| ChoosePlayer `Secretly$`, ChooseColor `Random$`                                                           | 3, 3   |
| ChooseCard `Secretly$`                                                                                    | 2      |
| ChooseType/GenericChoice `Secretly$`, GenericChoice `AtRandom$`, `ColorsFrom$`, `ChooseFromDefinedCards$` | 1 each |

2 refused → placed (Karona, False God Avatar; Nephalia: ChooseCard `AtRandom$`). Frontier Siege and Struggle for Project
Purity now enter and refuse later (Frontier Siege on its Khans trigger's `Mana` `Produced$`). 1 refused → hung: Face to
Face, whose best-of-three Rock, Paper, Scissors repeats while the probe's controller makes the same pick for both
players, a tie every round. Java's `RepeatEffect` has no cap without `MaxRepeat$` (`RepeatEffect.java:30-53`), so the
loop is faithful to identical picks; the real AI picks at random (`AILogic$ Random`).
