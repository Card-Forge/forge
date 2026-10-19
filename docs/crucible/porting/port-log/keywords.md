# Port: Keywords

- **Java source:** `forge-game/src/main/java/forge/game/keyword/Keyword.java` (354, its enum and `getKeywordDetails`),
  the 27 `KeywordInstance` subclasses beside it
- **Go target:** `crucible/internal/keyword`
- **Status:** Parsing and the definition table done — M3 slice F. Expansion into triggers, statics and abilities is
  `keyword.Expand` (ADR-0038), below. `engine.Card.HasKeyword` (M5) is the first engine consumer, and only ever asks "is
  the bare word present" — it does not expand a keyword into what it grants

## What it does

Reads a `K:` line into a head, its details, and the enum entry that says how the details are shaped. `Flying` carries
nothing, `Ward:2` an amount, `Dash:4 R W` a cost, `Awaken:3:4 U` both.

18,248 `K:` lines in the corpus.

`compile.Face.Keywords` carries the raw lines through from `carddb.Face.Keywords` unchanged (the removed port log's "Lethal
and deathtouch damage" section), the same "carry the printed text, interpret it downstream" split
`Type`/`Power`/`Toughness`/`Loyalty` already use. `engine.Card.HasKeyword(name)` is the interpreter: it calls `Parse` on
each line and compares `.Name`, so a keyword written with arguments (`"Ward:2"`) is still found by its bare head
(`"Ward"`).

## The head is not "everything before the first colon"

`Keyword.getKeywordDetails` has three cases, and the middle one is the reason:

| Line | Case |
| --- | --- |
| `Ward:2` | A `:` splits head from details at the first colon |
| `First strike` | No colon, so the **whole line** is tried as a name |
| `Bands with other Dragon` | The whole line fails, so the first word is tried |
| `Flying` | No colon, no space: the line is the name |

Cutting at the first colon gets the first and last right and the middle two wrong. `smartValueOf` is an exact
case-insensitive match on the display name, so the space case is not a prefix rule.

A `:Flavor` suffix, with the space that follows it, is cut from the details before they are read. It is display text,
and leaving it in would make `Ward:2:Flavor Protective Ward` an amount keyword whose amount is not a number.

## What the corpus actually writes

| What | Distinct | Uses |
| --- | ---: | ---: |
| Keywords the enum defines | 199 | 18,062 |
| Pseudo-keywords the card factory handles | 11 | 1,297 |
| Rules text on a `K:` line | 36 | 144 |

The enum has 203 constants, so four are defined and unused.

The eleven pseudo-keywords are `etbCounter` (475), `ETBReplacement` (405), `Chapter` (236), `Class` (76),
`AlternateAdditionalCost` (42), `MayEffectFromOpeningHand` (30), `Visit` (21), `DeckLimit` (5),
`MayEffectFromOpeningDeck` (4), `MustBeBlockedByAll` (2) and `Prize` (1). They are the vocabulary a compiler owes on top
of the enum, and they are in the golden by name.

The rules-text lines are counted, not listed. `CARDNAME must be blocked if able.` is a keyword only in the sense that
Forge stores it on a `K:` line; it is card text, it would move the golden every time a card is added, and Java resolves
it to `UNDEFINED` and falls back to a simple keyword holding the original string. **An unresolved head is not an error**
— this is the one place in the script language where that is by design.

## The argument shape is the class

The enum names a `KeywordInstance` subclass per keyword, and that class is the contract: `Ward:2` and `Hexproof:White`
are written identically and mean different things. The table records it.

| Shape | Keywords | Example |
| --- | ---: | --- |
| `Simple` | 88 | `Flying` |
| `Cost` | 54 | `Dash:4 R W` |
| `Amount` | 27 | `Absorb:1` |
| `Special` | 23 | `Ward`, `Protection`, `Kicker` |
| `Type` | 5 | `Champion:Elf` |
| `CostAndAmount` | 3 | `Awaken:3:4 U` |
| `CostAndType` | 2 | |

`Special` is the seventeen classes with a parser of their own plus the six that share one; their details stay text until
the keyword is expanded.

## Deviations from Java

| Java | Go |
| --- | --- |
| `getInstance` reflects a class per keyword and parses eagerly | A table row with a `Kind`. No reflection in the engine (GO-4), and the shape is data |
| An unresolved head silently becomes a `SimpleKeyword` | `Entry` is nil and `Kind()` is `Unknown`, so a caller can tell text from vocabulary |
| `KeywordWithAmount.parse` throws on a non-numeric amount | The corpus test checks it instead: every `Amount` keyword's argument is a number or `X` |

## Not ported yet

| Java | When |
| --- | --- |
| `CardFactoryUtil.setupKeywordedAbilities` — expansion into traits | M3, after the effect registry exists |
| The 23 bespoke `KeywordInstance` parsers | M5, with the keywords they implement |
| Reminder text formatting | Never. Display only (PORT-6) |

## Expansion (ADR-0038)

`keyword.Expand(Keyword) (Expansion, bool)` (`expand.go`) ports the `CardFactoryUtil` branches that build a trait from
script text. It returns the `A:`/`T:`/`S:`/`R:` lines and the SVars they name, minus the display params (`PrecostDesc$`,
`CostDesc$`, `SpellDescription$`, `TriggerDescription$`). `compileFace` calls it for each printed `K:` line after the
card's own lines (`CardFactory.getCard` runs `setupKeywordedAbilities` after the face's abilities), stores the SVars per
keyword line (`KWProwess<n>`, so two keywords never collide) and tags each synthesized `compile.Ability` with `Keyword`,
the line it came from. `ok == false` leaves the keyword inert: no template, or details this port does not translate
(GO-7).

| Keyword | Lines (corpus) | Expands to |
| --- | ---: | --- |
| Equip | 650 | `AB$ Attach \| Cost$ <cost> \| ValidTgts$ Creature.YouCtrl \| SorcerySpeed$ True` |
| Cycling | 306 | `AB$ Draw \| Cost$ <cost> Discard<1/CARDNAME> \| ActivationZone$ Hand` |
| TypeCycling | 106 | `AB$ ChangeZone \| Cost$ <cost> Discard<1/CARDNAME> \| ActivationZone$ Hand \| Origin$ Library \| Destination$ Hand \| ChangeType$ <type>` |
| Crew | 192 | `AB$ Animate \| Cost$ tapXType<Any/Creature.Other+withTotalPowerGEN> \| Defined$ Self \| Types$ Artifact,Creature` |
| Affinity | 77 | `S:Mode$ ReduceCost \| ValidCard$ Card.Self \| Type$ Spell \| Amount$ KWAffinity<n> \| EffectZone$ All`, the amount `Count$Valid <type>.YouCtrl` in `Face.Amounts` |
| Prowess | 104 | `T:Mode$ SpellCast` (noncreature, yours) running `DB$ Pump +1/+1` on Self |
| Exalted | 35 | `T:Mode$ Attacks` (alone, a creature you control) pumping `TriggeredAttackerLKICopy` +1/+1 |
| Annihilator | 14 | `T:Mode$ Attacks` running `DB$ Sacrifice \| Defined$ TriggeredDefendingPlayer \| SacValid$ Permanent \| Amount$ N` |
| Bushido | 37 | `T:Mode$ Blocks` and `T:Mode$ AttackerBlocked`, both pumping Self +N/+N |
| Afterlife | 11 | `T:Mode$ ChangesZone` (dies) making N `wb_1_1_spirit_flying` tokens |
| Persist, Undying | 24, 22 | `T:Mode$ ChangesZone` (dies with no -1/-1 resp. +1/+1 counter) returning `TriggeredNewCardLKICopy` with one |
| etbCounter | 477 | `R:Event$ Moved \| ValidCard$ Card.Self \| Destination$ Battlefield \| ReplaceWith$ KWEtbCounter<n>` running `DB$ PutCounter \| Defined$ Self \| ETB$ True`; extra params ride on the replacement |
| Fabricate | 16 | `T:Mode$ ChangesZone` (enters) running `DB$ Token \| TokenScript$ c_1_1_a_servo \| UnlessCost$ AddCounter<N/P1P1> \| UnlessPayer$ You` |
| Extort | 18 | `T:Mode$ SpellCast` (yours) running `AB$ LoseLife \| Cost$ WB \| Defined$ Player.Opponent`, then `DB$ GainLife \| LifeAmount$ AFLifeLost` |
| Echo | 52 | `T:Mode$ Phase` (your upkeep, `IsPresent$ Card.Self+cameUnderControlSinceLastUpkeep`) running `DB$ Sacrifice \| Echo$ <cost>` |
| Cumulative upkeep | 80 | `T:Mode$ Phase` (your upkeep) running `DB$ Sacrifice \| CumulativeUpkeep$ <cost>` |
| Renown | 20 | `T:Mode$ DamageDone` (combat damage to a player, `IsPresent$ Card.Self+!IsRenowned`) running `DB$ PutCounter`; PutCounter then sets the creature renowned |
| Chapter | 236 | one `T:Mode$ CounterAdded \| CounterType$ LORE \| CounterAmount$ EQ<i>` per chapter running that chapter's SVar |
| Bloodthirst | 23 | `R:Event$ Moved` (enters, `Bloodthirst$ True`) running `DB$ PutCounter \| ETB$ True` for N; `Bloodthirst X` counts the damage (`Count$BloodthirstAmount`) |
| Modular | 24 | enters with N +1/+1 counters (as `etbCounter`), and `T:Mode$ ChangesZone` (dies, `OptionalDecider$ You`) putting `TriggeredCard$CardCounters.P1P1` counters on target `Artifact.Creature` |
| Battle cry | 13 | `T:Mode$ Attacks` (Self) running `DB$ PumpAll \| ValidCards$ Creature.attacking+Other \| NumAtt$ 1` |
| Dethrone | 9 | `T:Mode$ Attacks` with `Attacked$ Player.withMostLife` putting a +1/+1 counter on Self |
| Flanking | 30 | `T:Mode$ AttackerBlockedByCreature` (`ValidBlocker$ Creature.withoutFlanking`) pumping `TriggeredBlockerLKICopy` -1/-1 |
| Afflict | 10 | `T:Mode$ AttackerBlocked` making `TriggeredDefendingPlayer` lose N life |
| Soulshift | 26 | `T:Mode$ ChangesZone` (dies, `OptionalDecider$ You`) returning target `Spirit.YouOwn+cmcLEN` from the graveyard to hand |
| Mentor | 19 | `T:Mode$ Attacks` putting a +1/+1 counter on target `Creature.attacking+powerLTKWPower<n>`, the amount `Count$CardPower` |
| Training | 11 | `T:Mode$ Attacks` with `IsPresent$ Creature.attacking+Other+powerGTKWPower<n>` putting a +1/+1 counter on Self |
| Evolve | 23 | `T:Mode$ ChangesZone` (`Creature.YouCtrl+Other`, `Condition$ Evolve`) putting a +1/+1 counter on Self |
| Cascade | 43 | `T:Mode$ SpellCast` (`ValidCard$ Card.Self`, `TriggerZones$ Stack`) running `DB$ DigUntil` (nonland, `cmcLT` the spell's `Count$CardManaCost`), then `DB$ Play` (free, optional), `DB$ ChangeZoneAll` (the rest to the bottom, random order), `DB$ Cleanup` |
| Storm | 37 | `T:Mode$ SpellCast` (self, from the stack) running `DB$ CopySpellAbility \| Amount$ TriggerCount$CurrentStormCount/Minus.1 \| MayChooseTarget$ True` |
| Exploit | 25 | `T:Mode$ ChangesZone` (enters) running `DB$ Sacrifice \| SacValid$ Creature \| Optional$ True`; Sacrifice fires `Mode$ Exploited` (`exploit.go`) for a keyword-tagged ability |

Equip's `ReduceCost$` and `AlternateCost$` extras are not read by any ability this port resolves, so those Equip lines
(about 25) stay inert; an `ActivationLimit$` extra is carried as written. The engine side: `attachEffect` resolves an
activated `AB$ Attach` (`attachActivated`, `castspell.go`) by attaching the source to its first creature target unless
protection refuses; `Draw` defaults `Defined$` to You as `getTargetPlayers` does; Layer 7 reads
`AffectedDefined$ Equipped/Enchanted`. Granted keywords (`AddKeyword$ Prowess`) do not expand yet: they
need ADR-0023's trait overlay. A bare `Flashback` (no cost) casts for the card's own mana cost. Persist and Undying
needed `ChangeZone`'s `WithCountersType$`/`WithCountersAmount$` (the permanent enters with the counters before its enter
replacements, `moveByEffect`'s `enterCounters`) and `Defined$ TriggeredNewCard[LKICopy]` (the card a dies or enters
trigger recorded); Annihilator needed `Defined$ TriggeredDefendingPlayer`. Tests: `keywordexpansion_test.go`, scenario
`equip-bonesplitter-attaches-to-a-creature-at-sorcery-speed`. Golden: `TestCorpusAST` changed for the 1,055 cards
carrying these keywords.

`LoseLife` records the life lost (the amount asked for from each player still in the game: this port has no life-loss
replacement to reduce it) where `AFLifeLost` reads it (`LifeLoseEffect.java:60`): on the source card's SVars, since
Java's per-ability SVar has no home here. Fabricate's counters-or-Servos choice is the unless cost the way Java scripts
it; the payer is asked through `ConfirmPayCost`. Java scopes the value SVars of
Mentor and Training (`X`) to the ability; the expansion names them `KWPower<n>` so they never shadow the card's own `X`,
and `compileFace` refuses a synthesized SVar whose name the card already defines. A numeric valid property resolves a
value SVar operand through the source card (`compareOperand`, `valid-strings.md`). Training's own `Training$ True` param
is read by nothing in Forge and is dropped. `etbCounter` is a Forge pseudo-keyword (no `Keyword` enum value). Not expanded yet,
each for a named reason: Rampage (`Count$Valid Creature.blockingTriggeredAttacker` needs `blocking<Defined>` in
`CardProperty`) and Melee (`TriggeredPlayersDefenders$Amount`).
Tests: `keywordtriggers_test.go`.

### Flashback is casting infrastructure, not an expansion

Java's `Flashback` keyword builds only the exile replacement (`Event$ Moved | Origin$ Stack`); the alternative cost
lives in `GameActionUtil`. ADR-0038 puts it with the casting infrastructure, so it is native: `castFromHand`
(`castspell.go`) offers a graveyard card its owner could cast for the keyword's cost when that cost is a plain mana cost
(`flashbackCost`; a Sac, PayLife or other non-mana flashback cost is not offered), `castOpts.altCost` replaces the
printed cost in `payCastCost`, and `Card.flashbackCast` makes `Game.Move` exile the spell wherever it would leave the
stack (CR 702.34a: a resolved or countered spell, and a permanent entering the battlefield). Timing is the card's own
(an instant at instant speed). A granted flashback (Snapcaster Mage, `MayPlay` grants) is not offered. Test:
`TestFlashbackCastsFromTheGraveyardAndExiles`.

Crew needed the cost shape `tapXType<Any/Type+withTotalPowerGEN>` (`cost.ActivationShape.TapTypeTotalPower`): the
controller picks any number of the untapped candidates through `ChooseCardsForEffect`, and the activation is refused,
before anything is paid, unless their total power reaches N (`ActivateAbility`, `taptype.go`'s `totalPower`).
