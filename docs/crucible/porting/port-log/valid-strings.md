# Port: Valid Strings

- **Java source:** `forge-game/src/main/java/forge/game/card/CardProperty.java` (2,135, the numeric-comparison branch
  ported), `forge/util/Expressions.java`, and the callers that split the param — `CardTraitBase.java:261`,
  `CardLists.java:189`
- **Go target:** `crucible/internal/valid`
- **Status:** Parsing done — M3 slice C. Evaluation started in `internal/engine` (M5): `Matches` covers the
  `Card.isValid` control flow in full; `ChosenCard`/`ChosenCardStrict`/`nonChosenCard`, `IsRemembered` and `IsImprinted`
  (membership in `source`'s own `Memory` lists); `EnchantedBy`/`EquippedBy`/`AttachedBy`/`FortifiedBy` (membership in
  `c`'s own `Attachments`), bare form only; `inZone`/`inRealZone` (`c`'s own `Zone`); `attacking`/`blocking` (the
  current `Combat`'s `Attackers`/`Blocks`), bare form only; `HasCounters` and `counters_<op><n>_<type>` (a second,
  independent numeric comparison over `Card.Counters`, plain-integer operand only); `enchanted`/`equipped` (any
  attachment of the matching subtype) and `modified` (CR 707.9's own three-way OR over the two plus `HasCounters`, the
  Aura leg filtered to the modified card's own controller); `RememberedPlayerCtrl`/`RememberedPlayerOwn` (membership in
  `source`'s own `Memory`, by player rather than by card) and `ActivePlayerCtrl` (`c`'s controller against
  `Game.ActivePlayer`); `Historic`/`Outlaw`/`Party` (pure `CardType` checks — Legendary/Artifact/Saga, and a
  Creature-or-Kindred with a fixed creature-type membership); controller/owner relative to `sourceController`
  (`YouCtrl`, `YouDontCtrl`, `OppCtrl`, `YouOwn`, `YouDontOwn`, `OppOwn`); identity relative to `source` (`Self`,
  `Other`, `StrictlyOther`); the five colors plus `Colorless`/`MultiColor`; a keyword check under three spellings
  (`with`/`without`/`hasKeyword`); `tapped`/`untapped`; the numeric comparisons (`power`, `basePower`, `toughness`,
  `baseToughness`, `cmc`, `totalPT`, `numColors`, `numTypes`, crossed with `LT`/`LE`/`EQ`/`GE`/`GT`/`NE`/`M2`) for an
  integer or source-SVar operand; the generic `non<Type>` fallback (`CardStateProperty`'s own chain); and the bare
  type/supertype/subtype fallthrough every property chain shares. The other ~880 property names are M5-M6, corpus
  frequency order (`tools/vocabscan -kind validProperty`) — a rough figure, not a precisely tracked count (the "Numeric
  comparisons" section below already explains why a batch like that one does not move it by a countable amount)

## What it does

Turns `Creature.Green+attacking,Land.YouCtrl` into alternatives, each a base with properties. `,` is OR, `+` is AND, and
the properties do not distribute: `Instant.YouCtrl,Sorcery` is "(an instant you control) or (any sorcery)".

49,615 valid strings in the corpus, under every valid-typed param key and inside `Count$Valid` heads.

## Nothing is trimmed, on purpose

`CardTraitBase` splits the param with a plain `split(",")` and `Card.isValid` splits the alternative with
`split("\\.", 2)`. Neither trims, so `ValidTgts$ Player, Planeswalker` really does produce an alternative whose base is
`" Planeswalker"` — and `CardType.hasStringType` rejects it, because no subtype carries a leading space,
`StringUtils.capitalize` leaves one alone, and the enum lookups are exact.

Reproducing that exactly is what found the defect on two cards
([`../card-script-defects.md`](../card-script-defects.md)). The vocabulary scanner, which trims because it is only
counting, could not see it. **A parser that tidies its input cannot find the bugs that tidying would fix.**

The corpus test therefore checks two things: every valid string writes back byte for byte, and no base is padded with
space.

## Numeric comparisons

`powerGE1` is a field, an operator and an operand in one token, and the split is not derivable from the field name. Java
hardcodes an offset per field:

| Field           | Written            | Operand offset |
| --------------- | ------------------ | -------------: |
| `power`         | `powerGE1`         |              7 |
| `basePower`     | `basePowerEQ2`     |             11 |
| `toughness`     | `toughnessLE3`     |             11 |
| `baseToughness` | `baseToughnessEQ1` |             15 |
| `cmc`           | `cmcGE7`           |              5 |
| `totalPT`       | `totalPT_GE5`      |             10 |
| `numColors`     | `numColorsEQ2`     |             11 |
| `numTypes`      | `numTypesGE3`      |             10 |

`totalPT` is the one that looks wrong and is not: the corpus writes it with a trailing underscore, so its operand starts
two characters after the field name rather than none.

The operator is found by **containment**, not position: `Expressions.compare` takes the whole property string and asks
whether it contains `LT`, then `LE`, `EQ`, `GE`, `GT`, `NE`, `M2`, in that order. The order is the rule for a token that
could match twice, so it is reproduced rather than tidied into a lookup.

`M2` is modulo 2, and the corpus uses it — `cmcM20` and `cmcM21`, Gyruda and Obosh. `NE` is defined by Java and used by
no card.

Operands stay text. A number, `X`, `Chosen`, an SVar name, or a whole `Count$` expression appears there, and resolving
any of them needs a game.

## Evaluation lands in `internal/engine`

`engine.Matches` (`valid.go`) is `Card.isValid` plus the one branch of `Card.hasProperty` that matters for negation, not
`CardProperty.cardHasProperty` itself yet — that 2,135-line switch is corpus-frequency work, same as `effect.go`'s
`Registry` (ADR-0011), and starts with three names: `YouCtrl`, `OppCtrl`, `Self`. A fourth case, a bare
type/supertype/subtype word used as either a `Base` or a `Property`, reaches
[`cardtype.Line.HasStringType`](../../../../crucible/internal/cardtype/cardtype.go) — the Go port of
`CardType.hasStringType`, which both `Card.isValid`'s own default case and every property chain's final fallthrough call
in Java.

**A `!` on the base negates the whole alternative, not just the base.** `Card.isValid` reads as an AND-chain — the base,
then every property in order — where any failing check returns early with a shared `testFailed` flag set once from the
base's own leading `!`; only if everything passes does it return `!testFailed`. `!Creature.YouCtrl` is therefore "not (a
creature you control)" — true for an opponent's creature and for any non-creature you control alike — not "a
non-creature you control". `valid.Base.Negated` already carried the flag; `altMatches` is what reproduces the
short-circuit. A `!` on a property is the simple case: `Card.hasProperty`'s own wrapper just inverts `cardHasProperty`'s
result for that one property (`valid.Property.Negated`), independent of the base's flag.

**`YouCtrl`/`OppCtrl` read `Card.Controller` directly, not Java's LKI-derived controller.**
`CardProperty.cardHasProperty` compares against `game.getChangeZoneLKIInfo(card).getController()`, last-known
information for a card whose own zone change might be mid-resolution. This port has no LKI tracking (`game-state.md`'s
"Not ported yet"), so it reads the current `Controller` — correct except for the one moment a card's own leaving is what
a property is trying to describe, the same category of gap `Move`'s missing LKI already is.

**`OppCtrl` has no team system to be aware of.** Java's is `controller.getOpponents().contains(sourceController)`; this
port reads it as "controlled by anyone other than `sourceController`" — right for every game this port can play today
(two players, or free-for-all with no teams), wrong only once a team variant exists to disagree with it.

`Matches` takes `sourceController PlayerID, source CardID` (plus, since the memory-based batch below, a `*Game`) rather
than an `*Ability`: tying it to `Ability` specifically would assume every valid-string check happens during ability
resolution, which CR 704.5's still-unbuilt Aura `Enchant`-restriction check will not (its source is the Aura itself, not
anything on a stack — `game-state.md`'s "State-based actions" section has the full citation caveat: Java's own comments
do not cleanly single-letter this rule).

## Color reaches `Matches` through a second chain, `CardStateProperty`

The five colors, `Colorless`/`MultiColor` and the generic `non<Type>` fallback are not in `CardProperty.cardHasProperty`
at all — Java's own switch has no `case "White"`. `CardProperty`'s final `else` calls
`card.getCurrentState().hasProperty(property, ...)`, `CardStateProperty.hasProperty`, the second of the four property
chains (`CardProperty`, `CardStateProperty`, `PlayerProperty`, `SpellAbilityProperty`), and that is where color lives —
confirmed by grepping `CardProperty.java` for a bare color-name case and finding none, then finding all of them in
`CardStateProperty.java` instead. `colorMatches` (`valid.go`) is that chain's own color branch, ported as a small
function of its own rather than folded into `propertyMatches`'s switch, since it has real internal structure (a shared
`mustHave`/`non`-prefix computation across all five names) `propertyMatches`'s existing three cases do not.

**Card color needed a real source: `Card.Colors()`, from `compile.Face`'s new `ManaCost`/`Colors`/`HasColors`.** Neither
existed before this — `compile.Face` carried Type/Power/Toughness/Loyalty/Defense/Keywords through from `carddb.Face`
but not the mana cost or a color override, because nothing before `colorMatches` needed a card's color at all.
`Card.Colors()` is exactly `carddb.Face.dumpColors()`'s own "override, else derive from cost" logic — not a new
derivation invented for this, but a second caller of one M2's P1 gate already verified byte-identical to Forge's own
dump across the whole corpus. `internal/mana.Colors`, the bitmask type both share, already existed too
(`internal/mana`'s own doc comment, ported from `MagicColor`/`ColorSet` for M1's mana-cost work) — this is its first use
outside cost parsing.

**`colorMatches` exact-matches rather than reproducing Java's `Contains`/prefix-stripped form, on purpose.** The one
suffix Java folds into its color branch, `Source` (`WhiteSource`, `nonRedSource`, `ColorlessSource`), is stripped first
by `propertyMatches` and answered by `sourceColorMatches` (`valid.go`): the candidate's own colors, except colorless
while a `Mode$ ColorlessDamageSource` static's `ValidCard$` matches it (`colorlessDamageSource`, Java's
`StaticAbilityColorlessDamageSource.colorlessDamageSource`; `ghostly_flame.txt` is the corpus's only such static). Only
battlefield hosts are scanned, same reach as `ignoreLegendRule`. Ghostly Flame's stack clauses (`Spell.Red+inZoneStack`)
never match: `baseMatches` has no `Spell` case, so a spell on the stack keeps its printed color here; `ChooseSource`
refuses that shape rather than answer it. Any other suffixed or compound name falls through to `propertyMatches`'s own
type-name fallthrough — false for every card, the honest "not implemented" answer, rather than a plausible-looking wrong
one.

**The generic `non<Type>` fallback is `CardStateProperty`'s own tail, reached only after color already had first
refusal.** `nonBlack` and `nonLand` look identical in shape but mean different things — one is a color negation
(`colorMatches` claims it), the other is a type negation (`c.Type().HasStringType("Land")`, inverted). Checking color
first is what keeps `nonBlack` from ever reaching the type fallback and being asked whether "Black" is a recognized
subtype (it is not, so the type fallback would answer `true` for every card — the exact wrong-answer shape colorMatches
existing to intercept color names avoids).

## Ownership, identity and keyword properties: back in `CardProperty` proper

The second highest-frequency gap after color (`tools/vocabscan -kind validProperty`) turned out to already be in the
chain `Matches` was reading — `CardProperty.cardHasProperty` itself, just further down than `YouCtrl`/`OppCtrl`/`Self`
reach: `YouDontCtrl`, `YouOwn`/`YouDontOwn`/`OppOwn` sit right next to the three ported names, and
`Other`/`StrictlyOther` sit right next to `Self`. Corpus weight alone justified this batch — `Other` (2,068 occurrences)
and `YouOwn` (1,853) outrank every property but `Self` and `YouCtrl` themselves.

**`YouOwn`/`YouDontOwn`/`OppOwn` are `YouCtrl`/`YouDontCtrl`/`OppCtrl` against `Card.Owner` instead of
`Card.Controller`.** Distinct once something steals control (an Owner and Controller can differ, `## Cloning`'s own
table in game-state.md has the reason both fields exist), otherwise identical in shape and in every LKI/team
simplification `YouCtrl`/`OppCtrl` already carry (this doc's own "Evaluation lands in `internal/engine`" section).

**`Other`/`StrictlyOther` are `Self`'s negation, collapsed to one behavior.** Java's `Strictly` forms
(`equalsWithGameTimestamp`) exist to tell a card from a same-named copy of itself apart across a zone change this port
has no game-timestamp/LKI tracking for (game-state.md's "Not ported yet") — the same gap `YouCtrl`'s own LKI paragraph
already documents, reached from a different direction. `StrictlyOther` therefore reads exactly like `Other`:
`c.ID != source`.

**Keyword properties reach `Card.HasKeyword` under three spellings Forge's own corpus uses interchangeably:
`with<Keyword>`, `without<Keyword>`, `hasKeyword<Keyword>`.** This is a fully generic mechanism, not Flying-specific —
`HasKeyword`'s own exact-match-on-parsed-name behavior (`card.go`'s own doc comment) already handles every keyword the
corpus has a name for, so porting the three prefixes once covers all of them. `without` is checked before the shorter
`with` prefix on purpose: `without` itself starts with the four characters `with`, so checking `with` first and
stripping only four characters would leave `"outFlying"` where `"Flying"` belongs — the same ordering mistake Java's own
nested `if` (`property.startsWith("without") && ...` checked inside the outer `property.startsWith("with")`) avoids by
checking the longer, more specific prefix first.

**`tapped`/`untapped` read `Card.Tapped` directly** — the same battlefield-only field `Game.Move` already clears on
leaving it (game-state.md's "The card's mutable parts").

## Numeric comparisons land in `engine.Matches`

`internal/valid` already split `powerGE1` into `Compare{Field: "power", Operator: "GE", Operand: "1"}` at parse time
(this doc's own "Numeric comparisons" section, M3). `propertyMatches` (`valid.go`) checks `p.Compare` first, before its
name-based switch, and hands off to `compareMatches` — the corpus-frequency-first mechanism this is, rather than eight
one-off branches, is the same shape `effect.go`'s `Registry` grows in (ADR-0011): one field-to-value table
(`compareFieldValue`) and one operator table (`compareOp`, `Expressions.compare` ported operator for operator, including
`M2`'s modulo-2 equality) cover every field/operator combination at once.

**`Operand` is an integer or a value SVar of the source card.** Java resolves it with
`AbilityUtils.calculateAmount(source, operand)`. `compareOperand` reads a base-10 integer directly, otherwise looks the
name up in the amounts of the source card's live faces and resolves it with `resolveAmount` (`powerLTKWPower0` for
Mentor's synthesized `Count$CardPower`; a card's own `powerGTX` with `SVar:X:Count$...`). A name no face of the source
defines, no source at all (`NoCard`), or an amount that does not resolve matches nothing: the same "false for every
card" answer any other unimplemented property gives, not a wrong one. `Chosen` (`source.getChosenNumber()`) is not
resolved.

`valid.go` therefore references `amount.go` (the `enginelint.json` allow list), and `amount.go` already references
`valid.go` through the `Count$Valid` family: the two groups are mutually dependent, Java's own `CardProperty` →
`AbilityUtils.calculateAmount` → `Card.isValid` cycle, the kind ADR-0003 names as the reason the engine is one package.

**`basePower`/`baseToughness` measure Java's `getCurrentPower`/`getCurrentToughness` (`Card.java:4407,4450`), not
`getBasePower`/`getBaseToughness`.** The names are Java's own trap: `getCurrentPower` is base folded with Layer 7's
`LayerCharacteristic`/`LayerSetPT` sub-layers, counters and the power/toughness-switch keyword excluded, while
`getBasePower` is the printed value alone — this port's own `BasePower`/`BaseToughness` accessors. Reusing `Power`'s own
Layer-7-fold (`foldPT`) rather than reaching for `BasePower` was the fix: `Power`/`Toughness` now split into a new
`layer7Power`/`layer7Toughness` step (base folded with Layer 7, no counters yet) and the counters addition that used to
be inline in one function, so `compareFieldValue`'s `basePower`/`baseToughness` cases read the same intermediate value
Java's `getCurrentPower`/`getCurrentToughness` compute, and `power`/`toughness` (Java's `getNetPower`/`getNetToughness`)
keep reading the full `Power`/`Toughness`, counters included. Neither this port's `Power`/`Toughness` nor `getNetPower`
folds in "CARDNAME's power and toughness are switched" here, so a switched creature's numeric comparisons share the same
gap every other switch-blind read on `Card` already has (game-state.md's "Not ported yet").

**`cmc` needed a new accessor, `Card.CMC()`** — nothing before this read a card's own mana value; `mana.Cost.CMC()`
already existed (M1) and `compile.Face.ManaCost` already existed (this doc's own "Color reaches `Matches`" section, for
the same reason), so `Card.CMC()` is a one-line composition of the two, not a new derivation.

**`totalPT`, `numColors` and `numTypes` have no unresolvable form.** `totalPT` is full `Power`+`Toughness` (both can
still be individually unresolvable, propagated as `ok=false` the same way `BasePower`'s own `ok` does); `numColors` is
`Card.Colors().Count()` (already used by `MultiColor`) and `numTypes` is `len(Card.Type().CoreTypes())`, both always
resolvable since neither a color set nor a type line is ever a printed `"*"`.

## `Matches` gains a `*Game`, for the properties that read `source`'s own card

`ChosenCard`/`ChosenCardStrict`/`nonChosenCard`, `IsRemembered` and `IsImprinted` (1,413 + 97 + 72 + 69 + 27 = 1,678
occurrences, the single biggest remaining gap by corpus weight — `tools/vocabscan -kind validProperty`) all ask the same
question in Java: is the candidate card present in a list carried by `source` itself
(`source.getChosenCards()`/`isRemembered()`/`hasImprintedCard()`), not anything on the candidate `card` or
`sourceController`. `Card.Memory` (`memory.go`) already carries exactly those three lists — `Remembered`, `Imprinted`,
`Chosen`, written by `RememberChanged$`/`ImprintCards$`/`ChooseCard` — but `Matches` had no way to reach the _card_
behind `source CardID`, only the handle. `Matches`, `altMatches` and `propertyMatches` all gained a `*Game` parameter so
`propertyMatches` can call `g.Card(source)`; nothing else in the file needed it, and `baseMatches`/`compareMatches`
still do not take one. Zero non-test callers existed yet (`effect.go`'s `Registry` has not started calling `Matches`
during ability resolution), so this was the cheap moment to make the change — the same "thread it through now, not
later" call the port has made before rather than adding a narrower parameter it would outgrow on the very next batch
(`EnchantedBy`/`EquippedBy`/`AttachedBy`, corpus rank 2 by combined weight, also need to resolve an arbitrary card by ID
— an attachment, not `source` — the same capability this grants).

**`Game.Card` panics on `NoCard` (GO-7's own engine-invariant-breach case everywhere else it is called); a `Matches`
caller legitimately passes `NoCard` as `source` when there is no meaningful one at all** (`Matches`'s own doc comment:
the base/property checks that never needed a source, the majority of the file). `sourceCard` (`valid.go`) is the guard:
`ok` is false on `NoCard`, and all five properties read it as "does not match" rather than propagating a panic a caller
was never wrong to trigger — the same "unresolvable input is a false, not a crash" shape `BasePower`'s own `ok` already
has, applied to a missing source card instead of a missing printed value.

**`ChosenCardStrict` collapses to `ChosenCard`, the same way `StrictlyOther` collapses to `Other`.** Java's `Strict`
suffix additionally checks `equalsWithGameTimestamp` — telling a chosen card from a same-named copy of itself apart
across a zone change — which this port has no game-timestamp tracking for (`game-state.md`'s "Not ported yet", the same
gap this doc's own "Ownership, identity and keyword properties" section already names for `StrictlyOther`).
`nonChosenCard` is `ChosenCard`'s negation and its own named token in the corpus (27 occurrences) rather than a bare
`!ChosenCard` — Forge exposes both spellings, so both are ported, and `nonChosenCard` shares `sourceCard`'s
`NoCard`-is-false answer rather than flipping to true on a missing source: "not chosen" is exactly as unknowable as
"chosen" when there is nothing to have chosen it, and the honest-unknown answer is the same one every other gap in this
file gives.

**`IsRemembered` compares by `EntityID`, not `CardID`** — `Memory.Remembered` holds entities generally
(`RememberObjects$ ChosenCard & Player.IsRemembered` puts a player in the same list as a card, `memory.go`'s own doc
comment), so the candidate card's identity has to go through `CardEntity(c.ID)` before the membership check, where
`IsImprinted`/`ChosenCard` compare `CardID` directly against lists `Memory.Imprinted`/`Memory.Chosen` already type that
way.

## `EnchantedBy`/`EquippedBy`/`AttachedBy`/`FortifiedBy` are one check, not four

All four ask "is `source` attached to `c`", and in Java they already collapse to one check before they ever reach
`CardProperty`: `GameEntity.isEnchantedBy(c)`, `isEquippedBy(c)` and `isFortifiedBy(c)` each just call
`hasCardAttachment(c)`, which is `getAttachedCards().contains(c)` — `GameEntity.java`'s own comment on `isEnchantedBy`
even says so: "Even if c is no Aura it still counts". This port's `Attach`/`Unattach` (`game.go`) is one mechanism for
Auras, Equipment and Fortifications alike (`card.go`'s own doc comment on `Attachments`), so there was nothing left to
port per name — `propertyMatches` exact-matches all four to the same `containsCard(c.Attachments(), source)`.

**Exact match, not prefix match, on purpose.** A trailing restriction — `EnchantedBy Aura.YouCtrl` ("does `c` have an
Aura attached that `sourceController` controls"), `EquippedByTargeted`, `AttachedTo Creature.EnchantedBy` — needs either
a nested `valid.Spec` matched against each attachment or an ability's current targets, neither of which `Matches` has
yet. Corpus weight justified stopping at the bare form: 2,338 of 2,345 occurrences of the four names, negated or not,
are bare (`tools/vocabscan -kind validProperty`); a restricted name is simply not equal to any of the four cases, so it
falls through to the same "false for every card" answer any other unimplemented property already gives, rather than a
prefix match silently answering the wrong (unrestricted) question for a token that asked a narrower one.

**Direction is `source` attached to `c`, not the reverse** — `EnchantedBy` read from the Aura's own perspective (`c`
being the Aura, `source` being what it enchants) is a different property, `Enchanted`
(`property.startsWith("Enchanted")` without the `By`, testing `source.equals(card.getEntityAttachedTo())`), not ported
here: low corpus weight next to the `By` forms and not part of this batch's own membership check.

## `inZone`/`inRealZone` reuse `ZoneByName`, and collapse to the same check

`inZoneBattlefield`, `inRealZoneStack` and the rest of the family (204 bare occurrences across `inZone` and 16 across
`inRealZone`, `tools/vocabscan -kind validProperty`) name a zone by the same spelling `ZoneByName` (`zone.go`, M1)
already parses `Zone:` lines with — no new lookup, just `strings.TrimPrefix` to strip the right prefix length (`inZone`
is 6 characters, `inRealZone` is 10) before handing the rest to it.

**Both read `c.Zone` directly, and read it identically.** Java's two forms differ only in `inZone`'s LKI indirection:
`property.startsWith("inZone")` compares against `lki.getLastKnownZone()`, which "falls back" to the object's own
current zone once there is no better LKI on hand (the branch's own comment); `inRealZone` skips LKI and calls
`card.isInZone(realZone)` directly. This port has no LKI tracking (`YouCtrl`'s own doc comment already makes the same
simplification, this doc's "Evaluation lands in `internal/engine`" section), so `inZone` reads `c.Zone` the same way
`inRealZone` always did — the two property names end up as one Go case each, not because they were the same check in
Java, but because the difference between them is exactly the gap this port already has everywhere else LKI would matter.

**`ZoneByName`'s own doc comment had a small inaccuracy, fixed while reading it for this.** It called Java's
`ZoneType.smartValueOf` case-sensitive; the actual method trims and then compares with `compareToIgnoreCase` --
case-insensitive. The port's own matching stays exact regardless (every zone name in the corpus is already written in
the enum's own case, so case-insensitive matching would only let a typo'd zone name silently resolve to one it did not
ask for), but the comment's claim about what Java does was simply wrong, not a deliberate simplification -- worth
correcting on sight rather than carrying the wrong reason forward into a property that now depends on reading it
correctly.

## `attacking`/`blocking`, bare form: the current `Combat`, not LKI

`attacking` (626) and `blocking` (148) are the two highest-weight combat properties by a wide margin over every suffixed
form combined (`tools/vocabscan -kind validProperty`) — `card.isAttacking()`/`combat.isBlocking(card)` in Java,
`containsCard(g.Attackers(), c.ID)`/`isBlocking(g.Blocks(), c.ID)` here, reusing the exported accessors
`attack.go`/`block.go` already have rather than reaching into `Game.combat` directly.

**No nil check needed for "no combat in progress."** Java guards both branches with `combat == null`; this port's
`Combat` is a value, not a pointer, so there is no nil state to check — but a zero-valued `Combat` (no attack declared
yet, or no combat happening at all) has an empty `Attackers`/`Blocks` either way, and `containsCard`/`isBlocking`
already answer `false` on an empty list. The two cases collapse to the same answer without a separate check, the same
way `inZone` and `inRealZone` collapsed into each other one section up.

**Bare form only, same reasoning as `EnchantedBy`'s own batch.** `attacking`'s suffixed forms alone run to
`attackingYou`, `attackingAlone`, `attackingSame`, `attackingBattle`, `attackingYouOrYourPW`, and a generic
`attacking <DefinedGameEntity>`; `blocking`'s add `blockingSource`, `blockingCreatureYouCtrl`, and a generic
`blocking <DefinedCards>`. Each is a real, distinct check (an LKI comparison, a defined-entity lookup, a
count-across-the-battlefield), not one shared mechanism the way the numeric comparisons or `EnchantedBy`'s four names
were — porting them is individual work for another turn, not a natural extension of this one. An exact match on the two
bare names keeps every suffixed form a coverage gap rather than a silently-wrong bare-form answer.

## `counters_<op><n>_<type>` is a second numeric comparison, deliberately not the same one

`HasCounters` (61) and the `counters_` family (318 across every counter type, `tools/vocabscan -kind validProperty` —
`counters_GE1_P1P1` alone is 125) read `Card.Counters` (`counters.go`, M4): `HasCounters` is `Counters.Any()`, already
there; `counters_` is `CardProperty.java`'s own second numeric-comparison branch, syntactically close to `powerGE1`'s
family but a distinct one — Java's own comment on the branch spells it "syntax example: `counters_GE9_P1P1` or
`counters_LT12_TIME`".

**It does not reuse `internal/valid`'s `Compare`, on purpose.** `Compare`'s grammar (`compare.go`, M3) is keyed on a
closed set of field names each with their own fixed operand offset (`powerGE1`'s offset is 7, `cmcGE1`'s is 5, this
doc's own "Numeric comparisons" table) — there is no such table for `counters_`, because the field is always the same
one word, `counters`, and what varies is the _type name_ after a second underscore, not the offset before the operator.
`countersMatches` (`valid.go`) is its own split on `_` into exactly three parts instead: `counters`, an operator
immediately followed by its operand (`operatorPrefix`, a prefix search over the same seven operators `compareOp` already
switches on, copied locally since `internal/valid`'s own list is unexported and parses a different token shape), and a
`CounterType` name. `CounterType` is an open string (`counters.go`'s own doc comment), so the type name needs no lookup
table at all — `CounterType("P1P1")` is the whole conversion, unlike `ZoneByName` or `colorMatches`, which both have a
closed vocabulary to check against.

**The operand stays plain-integer only.** `counters_LTX_P1P1` is a separate shape from `compareMatches` and keeps its
own `strconv.Atoi`: the one corpus card that writes it reads as "no match" like every other non-numeric operand does.

**The four-part `ReceivedThisTurn` form never reaches the three-part split at all.** Java's own property spells that
segment with no underscore of its own — `countersReceivedThisTurn_GE1_P1P1_You`,
`splitProperty[0].endsWith("ReceivedThisTurn")` is how Java's code tells it apart from the plain form — so it does not
match `counters_` (with the underscore) either, and falls straight through to the same "false for every card" answer the
type-name fallthrough gives anything else unrecognized. This port has no per-turn counter-received tracking to answer it
with regardless (`game-state.md`'s "Not ported yet"); only one card in the corpus uses this form.

## `enchanted`/`equipped`/`modified`: subtype-filtered attachment checks, and CR 707.9

`enchanted` (47) and `equipped` (56) are `EnchantedBy`'s own batch turned around: not "is a specific card among my
attachments" but "is _anything_ of the right subtype among my attachments" — Java's
`GameEntity.isEnchanted()`/`isEquipped()` are each one `getAttachedCards().anyMatch(...)` against `Card::isAura`/
`Card::isEquipment`. This port has no bulk predicate to call the way Java's `CardLists`/`Iterable.anyMatch` do, so
`attachedByType` (`valid.go`) resolves each attachment through `g` and checks its own `Type().HasSubtype` directly — a
small loop rather than a missing library function. Neither name has a suffixed form in the corpus at all (every
occurrence of both is the bare word), unlike every other batch so far — nothing here is scoped down to "the common
case"; this is the whole property, for both names.

**`modified` (CR 707.9, 38 occurrences) composes the same pieces, plus `HasCounters`, with one twist.** Java's
`Card.isModified()` is `isEquipped() || hasCounters() || getEnchantedBy().anyMatch(isController(controller))` — the
third leg is not plain `enchanted`: it only counts an Aura that shares the _modified card's own controller_, a filter
`enchanted` itself does not apply. `isModified` (`valid.go`) reuses `attachedByType` for the Equipment leg and
`Counters.Any()` for the counter leg, but writes the Aura leg as its own loop rather than calling `attachedByType` with
a filter bolted on — the controller check is not a generic "does this attachment have subtype X" question, so it does
not belong inside the function that answers that one. A card enchanted by an opponent's Aura is `enchanted` but not
`modified`, and the test suite checks exactly that pair on the same card.

**`enchanting`/`equipping` (the reverse direction — "am I the Aura/Equipment currently attached to something") are not
ported: neither appears in the corpus at all.** A property with zero real usage to verify against is not a natural
extension of this batch, unlike `inRealZone`'s 16 occurrences next to `inZone`'s 204 — there is nothing here to be
confident is even right, so it stays a documented gap rather than a guess.

## `RememberedPlayerCtrl`/`RememberedPlayerOwn`/`ActivePlayerCtrl`: two more player-relative reads

`RememberedPlayerCtrl` (104) and `RememberedPlayerOwn` (6) are `IsRemembered`'s own shape (membership in `source`'s
`Memory`) with the field it compares turned from "is `c` itself remembered" into "is `c`'s controller/owner among the
_players_ `source` has remembered" — `Memory.Remembered` already holds entities generally, not just cards (`memory.go`'s
own doc comment: `RememberObjects$ ChosenCard & Player.IsRemembered` puts a player in the same list), so this reuses
`sourceCard`/`containsEntity` unchanged and just wraps `c.Controller`/`c.Owner` in `PlayerEntity` instead of `c.ID` in
`CardEntity`.

**Exact-matching the two names, not reproducing Java's own `endsWith("Ctrl")` ternary, on purpose.**
`CardProperty.java`'s branch picks the field with `property.endsWith("Ctrl") ? controller : card.getOwner()` — not a
name lookup, just "does the string end in these four letters." That means a `$GreatestCardManaCost` tail (2 occurrences
on `RememberedPlayerCtrl`, 1 on `RememberedPlayerOwn`) reads as an **owner** check in Java even when attached to the
name that says `Ctrl`, because the tail is what the string actually ends in — an accidental coupling between an
unrelated `Count$`-style suffix and which field gets read, not a deliberate rule. This port has no `Count$` resolution
to make that suffix mean anything regardless, so exact-matching the two bare names and letting anything else fall
through to a coverage gap is both simpler and avoids inheriting a coupling that exists only because of how Java happened
to write the check.

**`ActivePlayerCtrl` needed nothing new: `Game.ActivePlayer` already existed (`turn.go`, M4).**
`c.Controller == g.ActivePlayer()` is the whole implementation — the only reason this waited for a Matches batch at all
is that `propertyMatches` had no `*Game` to read it from before "`Matches` gains a `*Game`," above.

**`TargetedPlayerCtrl` and `TargetedPlayerOwn` (bare forms, about 125 corpus uses) read the resolving ability's player
targets** (`AbilityUtils.getDefinedPlayers(source, "TargetedPlayer", spellAbility)`): `Matches` has no ability argument,
so `Game.targetsPlayer` reads `Game.resolving` when its source is the spec's source card, and the property is false
outside one. The `$GreatestCardPower` tail forms (5) stay unported.

## `Historic`/`Outlaw`/`Party`: pure `CardType` checks, no `Def` this port did not already read

`Historic` (52) is `CardType.isHistoric` — Legendary, Artifact, or a Saga, three unrelated chains (a supertype, a core
type, a subtype) any one of which is enough — and `Outlaw` (16) / `Party` (1) are `CardType.isOutlaw`/`isParty`: a
Creature or Kindred card with at least one of a fixed creature-type set (Assassin/Mercenary/Pirate/Rogue/Warlock for
Outlaw, Cleric/Rogue/Warrior/Wizard for Party — `CardType.java`'s own `Constant.OUTLAW_TYPES`/`PARTY_TYPES`, not
derivable from the type grammar, just copied). None of the three needed anything beyond `Card.Type()` — the same
`cardtype.Line` accessors (`HasSupertype`, `Has`, `HasSubtype`) the bare type/supertype/subtype fallthrough already
calls. `isHistoric`/`isTribalMember` (`valid.go`) are free functions over `cardtype.Line` rather than methods on
`*Card`, since nothing about either check reads anything else on the card.

**`Worthy` is not ported: zero corpus occurrences.** Java's version is `cardState.isWorthy()`, real per-turn state
(Outlaws of Thunder Junction's own mechanic) this port does not track — but with nothing in the corpus to verify
against, that gap is moot rather than a decision.

## Deviations from Java

| Java                                                                   | Go                                                                                                                                                                                                                                                                                                                       |
| ---------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| The property string is re-parsed on every evaluation                   | Parsed once at load into `Spec` (ADR-0007)                                                                                                                                                                                                                                                                               |
| `!` is consumed by mutating the local `incR[0]`                        | `Negated bool` on both `Base` and `Property`, so the sign is not part of the name                                                                                                                                                                                                                                        |
| A comparison is recognised by a chain of `startsWith` in the evaluator | `Compare` on the property, filled at parse time                                                                                                                                                                                                                                                                          |
| Matching happens against a `Card` and a `Game`                         | ADR-0003 stays honoured either way: `internal/valid` only parses a `Spec` and never imports `internal/engine`; `engine.Matches` does take a `*Game` (for `source`'s own `Memory`, this doc's own "`Matches` gains a `*Game`" section) but that boundary is between the two packages, not inside `internal/engine` itself |
| Color is read via `card.getColor(cardState)`, LKI/state-aware          | `Card.Colors()`, current state only — the same gap every other characteristic accessor on `Card` already has                                                                                                                                                                                                             |

## Not ported yet

| Java                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | When       |
| -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| `CardProperty.cardHasProperty` — the great majority of its branches; ported so far: `ChosenCard`/`ChosenCardStrict`/`nonChosenCard`, `IsRemembered`, `IsImprinted`, `EnchantedBy`/`EquippedBy`/`AttachedBy`/`FortifiedBy` (bare form), `inZone`/`inRealZone`, `attacking`/`blocking` (bare form), `HasCounters`, `counters_<op><n>_<type>`, `enchanted`/`equipped`/`modified`, `RememberedPlayerCtrl`/`RememberedPlayerOwn`, `ActivePlayerCtrl`, `Historic`, `YouCtrl`/`YouDontCtrl`/`OppCtrl`, `YouOwn`/`YouDontOwn`/`OppOwn`, `Self`/`Other`/`StrictlyOther`, `with`/`without`/`hasKeyword`, `tapped`/`untapped`, the numeric comparisons (`engine.Matches`) | M5-M6      |
| The `$`-suffixed `RememberedPlayerCtrl`/`RememberedPlayerOwn`/`TargetedPlayerCtrl` forms — need `Count$` resolution this port does not have                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | M5-M6      |
| `enchanting`/`equipping` — the reverse direction, "is this card itself attached to something" filtered by its own subtype; zero corpus occurrences                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             | M5-M6      |
| `countersReceivedThisTurn_<op><n>_<type>_<player>` — per-turn counter-received tracking (`game.getCounterAddedThisTurn`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | M5-M6      |
| `attacking`/`blocking` suffixed forms — `attackingYou`/`attackingAlone`/`attackingSame`/`attackingBattle`/`attackingYouOrYourPW`/`attacking <DefinedGameEntity>`, `blockingSource`/`blockingCreatureYouCtrl`/`blocking <DefinedCards>`, and the rest                                                                                                                                                                                                                                                                                                                                                                                                           | M5-M6      |
| `EnchantedBy`/`EquippedBy`/`AttachedBy`/`AttachedTo` restricted forms — a nested `valid.Spec` matched against each attachment, or against an ability's current targets                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | M5-M6      |
| `Enchanted`/`EnchantedController`/`EnchantedPlayerCtrl` — the Aura's own perspective on what it enchants, the reverse direction from `EnchantedBy`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             | M5-M6      |
| `AbilityUtils.calculateAmount` for a numeric-comparison `Operand` that is not a plain integer — `X`, `Chosen`, an SVar name; needs an ability-context evaluator `internal/expr` does not have yet                                                                                                                                                                                                                                                                                                                                                                                                                                                              | M5-M6      |
| `CardStateProperty.hasProperty` — the rest of it: `AllColors`, `MonoColor`, `AnyChosenColor`, `EnemyColor`, `AssociatedWithChosenColor`, `Worthy`, `HasSVar`, and everything past it (color, `Colorless`, `MultiColor`, `ChosenColor` for a single chosen color, `Outlaw`/`Party` and the generic `non<Type>` fallback are ported)                                                                                                                                                                                                                                                                                                                             | M5-M6      |
| `SpellAbilityProperty` — the fourth property chain, untouched                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | M5-M6      |
| `PlayerProperty.playerHasProperty` (517) — no `Base`/`Property` this port evaluates targets a `Player` yet                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | M5-M6      |
| LKI-aware `YouCtrl`/`OppCtrl`, and a team-aware `OppCtrl`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | M5-M6      |
| Property heads as a closed vocabulary, for the P2 gate                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | M3 slice H |

The 1,256 distinct property tokens are inventoried in `internal/carddb/vocab`'s golden. Classifying them into families
belongs with the evaluator that implements them, not with the parser.

## Gating the property vocabulary

`valid.Parse` is total: it splits a string into a base and properties and never rejects one, because Java never rejects
one either. The chain ends at `CardProperty.java:2116-2119`:

```java
} else if (!card.getCurrentState().hasProperty(property, sourceController, source, spellAbility)) {
    return false;
}
return true;
```

So an unimplemented property is not ignored — it is **false for every card**, and the alternative carrying it matches
nothing. That makes an unaccounted property a silent targeting failure, which is why it needs a gate rather than a
review habit.

The accepted vocabulary is not a list anywhere. Acceptance is a union, and it has to mirror Java's fallthrough order
rather than test a flat set, because a name can be reachable two ways:

| Source                                                             | Residual, of 928 distinct properties |
| ------------------------------------------------------------------ | -----------------------------------: |
| `equals` and `startsWith` branches in the **four** property chains |                                   62 |
| plus subtypes, core types, supertypes, colours                     |                                    6 |
| plus the 203 keyword names                                         |                                    1 |

Three things the scrape has to get right, each of which produces false findings on its own:

- **Four chains.** `CardProperty`, `CardStateProperty`, `PlayerProperty`, `SpellAbilityProperty`.
- **`equals` and `startsWith` are not interchangeable.** `startsWith("AttachedTo")` accepts
  `AttachedTo Creature.YouCtrl`; read as exact it rejects 400 properties Forge handles.
- **`restriction` is a second receiver name**, for 12 of the branches.

One deliberate exclusion, and it is not a property vocabulary at all. `ManaReflected` reads its own `Valid$` form:
`CardUtil.java:261` tests `validCard.startsWith("Defined.")` and treats the rest as a defined name, so
`Defined.Sacrificed` never reaches `CardProperty`. Nine cards use it.

`TestEveryPropertyIsAccountedFor` holds at zero. The one defect it found is in
[card-script-defects.md](../card-script-defects.md).
