# Port Log — Game State: Layers

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)

Continuous effects: Layer 0 printed P/T and Layers 2, 4-8.

## Layer 0: printed power and toughness

`destroyLethalToughness` needed the same kind of characteristic `Card.Type()` already carries, for power and toughness
instead of the type line: `compile.Face` now also copies `Power`/`Toughness` through from `carddb.Face` unchanged,
printed text, not a number — either can be `*`, `1+*` or a `Count$` reference (`carddb.Face`'s own doc comment), which
is exactly why they stay text at this layer too. `Card.BasePower`/`BaseToughness` resolve that text to an `int` only
when it is a plain integer (`strconv.Atoi`), reporting `false` otherwise rather than a wrong number or a panic — the
same "coverage gap, not a wrong answer" contract `Type()` already keeps.

"Base" is Java's own word (`getBasePower`/`getBaseToughness`) for the printed value, CR 613's Layer 0 — before a
characteristic-defining ability (Layer 7a), a setting effect (7b), a modifying effect (7c) or a counter (CR 613.4, after
Layer 7) has applied.

---

## Layer 7, Layer 4, Layer 5, Layer 6, Layer 8 and Layer 2: `PT`/`TypeMod`/`ColorMod`/`KeywordMod`/`RulesMod`/`ControlMod`, `Card.Power`/`Toughness`/`Type`/`Colors`/`HasKeyword`/`Controller`, and the first real Continuous callers

`layer.go` is `forge.game.staticability.StaticAbilityLayer`: the ten-value enum, Forge's own order, 7a/7b/7c split and
Layer 8 (Forge's own rule-changing bookkeeping, no CR number) included, even though only 7b/7c have a real caller today.
`pt.go`'s `PT` is a card's set of `PTEffect`s — one continuous effect's power/toughness contribution, a layer, a
timestamp, and (`HasPower`/`HasToughness`, below) which dimension it actually sets.

`Card.Power`/`Toughness` (`card.go`) is what actually applies CR 613.4's ordering: sort `PT`'s effects by layer then
timestamp, then fold — `LayerCharacteristic` and `LayerSetPT` each replace the running value, `LayerModifyPT` adds to
it, and +1/+1/-1/-1 counters (`Card.Counters`, already built) apply last, after every layer. A `LayerCharacteristic`
effect can turn an unresolvable base (`*`, `BasePower`'s own `ok=false`) into a resolvable one — a
characteristic-defining ability's entire purpose — so `foldPT` starts from `(base, baseOK)` rather than requiring
`baseOK` up front. `destroyLethalToughness` (CR 704.5f,
[`## State-based actions`](state-based-actions.md#state-based-actions)) reads `Toughness()` instead of
`BaseToughness()`, so a creature a `LayerModifyPT` pump or an annihilated -1/-1 pile actually reduces to zero dies here
too, not only one whose printed toughness always read zero.

`PTEffect` gained `HasPower`/`HasToughness` the moment a real caller needed them: a `LayerSetPT`/`LayerCharacteristic`
effect naming only one dimension (68 real corpus `SetPower$`-only lines, 9 `SetToughness$`-only) must leave the other
exactly as it was, not reset it to zero the way the original zero-value `Power`/`Toughness` `int` fields alone would
have — `foldPT`'s own `pick` function now returns `(value, hasThisDimension)`, overwriting only when `has` is true.
`LayerModifyPT` needs neither flag: adding zero to a dimension an effect does not mention is already a no-op. Every
existing `pt_test.go` literal that builds a `LayerSetPT`/`LayerCharacteristic` `PTEffect` needed both flags added to
keep testing what it already did; `TestPowerToughnessSetPTPartialLeavesOtherDimensionAlone` is the new case that
motivated the change.

`applyContinuousPT` (`continuous.go`) is the first real (non-test) `PT.Add` caller: `Mode$ Continuous` lines carrying
`AddPower$`/`AddToughness$`/`SetPower$`/`SetToughness$`, matched against every battlefield permanent via a blanket
`Affected$` valid-string — the anthem shape (Glorious Anthem, and 2,192 of 2,426 real corpus `S:Mode$ Continuous` lines
carrying one of those four keys). Ported from `StaticAbilityContinuous.applyContinuousAbility`/`getAffectedCards`.

Recomputed from scratch on every `CheckStateBasedActions` call, not pushed once when a source or an affected creature
enters: Java's own `applyContinuousAbility` runs fresh from `GameAction.checkStateEffects` every state-based-action pass
for exactly this reason — an anthem has to reach a creature that enters after it, and stop the instant the anthem itself
leaves, neither of which a one-time push at either card's own entry could give it
(`TestApplyContinuousPTRecomputesWhenSourceLeaves`, continuous_test.go, proves the second half). Every battlefield
card's own `PT.effects` is cleared before the rebuild; safe today because nothing else this port can build yet ever adds
a `PTEffect` of its own (a real "+3/+3 until end of turn" pump would need its own duration-scoped bucket this clear
would not touch, [`## Not ported yet`](../game-state.md#not-ported-yet)) — recomputing everything from
`Mode$ Continuous` statics alone is exactly correct until one exists, not an approximation that happens to work today.

Not resolved, each for a specific reason: `Condition$` (116 of 2,426 real lines) — a generic runtime gate
(`StaticAbility.java`'s own `checkConditions`, no equivalent for any static-ability mode in this port yet);
`AffectedDefined$`/`AffectedZone$` (0 and 24) — a targeted or `Remembered`-driven affected set, not a blanket
valid-string match. A non-numeric `AddPower$`/`AddToughness$`/`SetPower$`/`SetToughness$` naming a named SVar now
resolves through `resolveAmount` (amount.go, below) when that SVar's own body is one of the shapes it evaluates; per
missing dimension when it is not, not per whole line.

**`compile.Face.Amounts`, `resolveAmount` and Layer 7a (`CharacteristicDefining$`).** `AbilityUtils.calculateAmount`
itself is 300-some lines dispatching on eighty-some expression heads — not a port this slice attempts whole — but one
family of it, `Count$Valid[<Zone>[,<Zone>...]] <spec>` (`CardLists.getValidCardCount` against a zone,
`AbilityUtils.xCount`'s own "count valid cards on the battlefield"/"count valid cards in any specified zone/s"
branches), is exactly what `Matches` (valid.go) and this port's own `Zone` (zone.go) already evaluate everything else
with — 2,804 of the corpus's 6,186 real `Count$` expressions (45%), 2,537 of those with no operator suffix, is worth its
own evaluator slice for that reason alone.

The compiler carries the raw text a `SVar:X:Count$...` line holds; nothing before this needed to read one that was not
itself an ability (`compile.Ability`'s own `Subs`/`SubRef` mechanism resolves an ability-shaped SVar,
`Execute$ TrigDraw` and the like, but a plain-value reference like `AddPower$ X` left `"X"` as inert text with no
connection to its own SVar body). `compileAmounts` (compile.go) closes that: every SVar a face defines that is NOT
itself an ability — its own head, up to the first `$`, does not fold to a `recordKeys` entry
(`DB`/`AB`/`SP`/`ST`/`RE`/`Mode`/`Event`) — is parsed once, via `internal/expr.Parse`, into
`Face.Amounts map[string]expr.Amount`, keyed by name folded to lower case (`SVars.Get`'s own case-insensitive contract).
`expr.Parse` never fails (its own doc comment), so every non-ability SVar gets an entry even when its own body is not a
shape `resolveAmount` can use yet — the identical "record what recognizing it needs, evaluator decides whether it can"
split `Ability.Params` already has. This does not touch `WriteCanonical`/`Fingerprint` (canonical.go) at all — the
golden AST walks only `Abilities`/`Triggers`/`Statics`/`Replacements` — so `TestCorpusAST` needed no regeneration.

`resolveAmount`/`resolveAmountDepth` (amount.go, `internal/engine`) evaluate an `expr.Amount` to an int: a `Literal`
resolves directly; a `Reference` resolves the named SVar in turn (runtime SVar first, `maxAmountDepth` bounding the
chain); an `Expression` dispatches on its head. For the Valid family, `validCountZones` maps the head's own zone suffix
(empty is Battlefield, Java's own default; `,`-joined for the 40-some real lines naming more than one zone) to this
port's own `ZoneType` values, and `countValid` counts every card in every one of those zones, every player's own,
`Matches` accepts -- the identical `sourceController`/`source` pairing every other valid-string check passes.
Single-zone coverage: bare `Valid` (1,973 of 2,804), `ValidGraveyard` (471), `ValidHand` (253), `ValidLibrary` (63),
`ValidExile` (43) -- the whole family bar `ValidAll`/`ValidSelf` (8 real lines, neither an actual zone name). doXMath
suffixes, the heads past the Valid family and the `$`-suffixed properties below are
[`layer7a-cda-amounts.md`](layer7a-cda-amounts.md#layer-7a-amounts-the-real-corpus-vocabulary-and-what-resolves)'s.

A `Count$Valid<Zone> <spec>` argument can itself carry a further `$`: Tarmogoyf's own toughness SVar is
`Count$ValidGraveyard Card$CardTypes`. `xCount` (`AbilityUtils.java`) cuts the whole "head argument" string on the FIRST
`$` (`paidparts = l[0].split("\\$", 2)`) before it ever reaches `CardLists.getValidCards` — so the actual valid string
passed to it is only `Card` (Forge's own universal base, matching every object), and `CardTypes` is a completely
separate operator (`handlePaid`, `AbilityUtils.java:3719`, `countCardTypesFromList`): count the DISTINCT card types
among whatever `Card` matched, not the matches themselves. `expr.ParseCount` now splits this apart into a new
`DistinctProperty` field rather than feeding `Card$CardTypes` whole into `valid.Parse` as one base name — which would
have parsed without error (`valid.Parse` never fails, its own doc comment) into a single-alternative spec with base name
`"Card$CardTypes"`, an unrecognized base that itself matches nothing, but count.Valid would then hold `Card` correctly
once split, which DOES match every real object — silently returning a real but WRONG number (a plain match count, not a
distinct-type count) rather than failing to resolve at all. `resolveAmount` measures the properties `handlePaid` defines
that this port can evaluate (`paidMeasure`, amountpaid.go: `CardTypes`, `Colors`, `Amount`, and
`Greatest`/`Least`/`Different`/sum of `CardManaCost` or `CardCounters.<TYPE>`) and refuses every other one (GO-7) rather
than resolve `Valid` alone; [`layer7a-cda-amounts.md`](layer7a-cda-amounts.md#refused-rather-than-guessed-go-7) has the
list.

This was caught after Batch B had already shipped and merged, not during it: writing
`TestApplyContinuousCharacteristicDefiningSkipsDistinctPropertyCount` (continuous_test.go) — a Tarmogoyf-shaped CDA, one
matching graveyard card — found `Power()` resolving to `(1, true)` (one card matches the universal `Card` base) rather
than failing to resolve, tracing back to `Count$ValidGraveyard Card$CardTypes` never having been checked against a real
corpus SVar shaped exactly like this before. `TestParseCountDistinctProperty` (`internal/expr`) is the narrower
unit-level proof.

`ptParam` (continuous.go) is `resolveAmount`'s own real caller: a plain integer resolves exactly as it always did, and
failing that the value is looked up in `amounts` by name and handed to `resolveAmount`.

`applyOneContinuousPT` now branches on `CharacteristicDefining$` before its own general
Affected$-matched
path. `applyOneCharacteristicDefiningPT` reads only `SetPower$`/`SetToughness$`. A CDA always SETS the
base value it defines (CR 613.3) and never adds to one.

No real corpus line pairs `CharacteristicDefining$` with `AddPower$` or `AddToughness$`.

The result applies to the host card alone, at `LayerCharacteristic` (7a). `StaticAbilityContinuous.getAffectedCards`'s
own CharacteristicDefining branch hardcodes the affected set to a collection holding only the host card, regardless of
any `Affected$` a real corpus line also happens to carry (revenant.txt's own redundant `Affected$ Card.Self`) — so no
`Affected$` param is read for this shape at all.

`ExcludeZone$` is not resolved: one real line, among 264 `CharacteristicDefining$ True` cards, is not a shape worth a
separate zone check for, and `applyContinuousPT`'s own battlefield-only walk already means host is on the one zone this
port could check anyway.

`LayerCharacteristic` itself needed no new folding work. `PTEffect`'s own Layer/HasPower/HasToughness machinery (this
section's own earlier paragraphs) had already carried it as a first-class layer since M5's own Layer 7b/7c work landed,
unused as a real caller until now.

One real fixture needed fixing because of this, not writing: `equipment-falls-off-without-destroying`'s own Grizzly
Bears carried 2 marked damage on a printed 2/2, lethal only because Sword of Body and Mind's real
`Mode$ Continuous | AddPower$ 2 | AddToughness$ 2` was not applying yet. Equipped, it is actually a 4/4 — 2 damage was
never lethal to it in a rules-correct game, only in a port that had not built this yet. The fixture's own expected
outcome was accidentally right for the wrong reason; `setup.state` now marks 4 damage, actually lethal on a 4/4, so the
scenario still demonstrates what it was written for (`cleanupDanglingAttachments`'s Equipment-vs-Aura distinction)
without depending on a gap this port no longer has.

**Not here: CR 613.6-613.8's dependency reordering.** Java sorts effects within a layer by timestamp and then
re-evaluates whether an unapplied effect has become dependent on or independent of another as each one resolves
(`GameAction.checkStaticAbilities`'s `findStaticAbilityToApply`, 1,099-line `StaticAbilityContinuous.java`). Real
`PTEffect`s exist now, but nothing in the real corpus subset this slice resolves puts two effects on one card that could
actually disagree about order (an anthem and an equipment bonus stack additively regardless of which applied first) —
`foldPT`'s plain timestamp sort remains a real port of CR 613.7's tiebreak, not yet a stand-in for 613.8's harder case.

`PT.Clear()` runs from `Move` the moment a card leaves the battlefield, the same list `Counters`, `Damage` and `Tapped`
already clear there: a continuous effect that only applied on the battlefield does not survive the trip. That clear was
redundant with `applyContinuousPT`'s own rebuild-from-scratch for anything `Mode$ Continuous` produces (a card that has
left is not on the battlefield to be walked as a host or an affected card on the next pass either way) until an
event-driven "+3/+3 until end of turn" pump actually existed: `pumpEffect`
([`## M6's fifth effect: Pump, and duration tracking`](effects-m6-first.md#m6s-fifth-effect-pump-and-duration-tracking))
is not itself a `Mode$ Continuous` static, so its own record (`Game.pumps`) needs `Move`'s own clear to drop it the
moment its target leaves the battlefield too — `clearPumps`, called from the identical branch.

`Player.Counters` is new here, the same type `Card.Counters` already uses: poison is the only player-level counter any
rule reads today, but nothing about "a count that is never stored at zero" is specific to what holds it. `Game.Clone`
deep-copies it for the same reason it already deep-copies a card's — sharing the underlying map would let the AI's
lookahead poison the real game.

**Layer 4, `applyContinuousType` (`continuous.go`), and `cardtype.Line`'s new `ParseToken`/`Union`/`Without`.**
`AddType$`/`RemoveType$` are the next slice of `Mode$ Continuous` after Layer 7b/7c, and needed a real gap in
`cardtype.Line` closed first: nothing exported it a way to combine or subtract two type lines, because every existing
caller only ever needed to _read_ a printed line, never build a new one at runtime. `ParseToken` is `Parse`'s own
per-word classification (core type, then supertype, then subtype fallthrough) pulled out for a single already-split word
— no `*cardtype.Registry` needed, unlike `Parse` itself, because multiword lookahead is the only thing `Parse` uses a
`Registry` for and `AddType$`/`RemoveType$` values are already split on `" & "` into individual type names by the
compiled script. That absence is deliberate, not an oversight worked around: this port still injects no
`*cardtype.Registry`/`*carddb.DB` into the engine (`CLAUDE.md`'s own GO-2), so a Layer 4 effect built to need one would
have nothing to call. `Union` and `Without` are CR 613.4's own add/remove directions on `Line` itself.

`TypeMod` (`typemod.go`) is `PT`'s own structure, copied for Layer 4: a `[]TypeEffect` (`Timestamp`, `AddTypes`,
`RemoveTypes`), a `foldType` that sorts by `Timestamp` and folds each effect's `Union` then `Without` into the running
line — CR 613.7's tiebreak, the identical simplification `foldPT`'s own doc comment already makes for 613.8's harder
dependency-reordering case. `Card.Type()` now folds `TypeMod` over the printed `Def.Faces[0].Type` the same way
`Card.Power`/`Toughness` already fold `PT` over `BasePower`/`BaseToughness`. `Move` calls `TypeMod.Clear()` on leaving
the battlefield, next to `PT.Clear()`; `Game.Clone` deep-copies it, next to `PT`'s own clone.

`applyOneContinuousType` skips a whole line, not just the part it cannot resolve, the moment it carries anything past a
plain literal `AddType$`/`RemoveType$` token list: `ChosenType$`/`ChosenType2$`/`ImprintedCreatureType$`/
`AllBasicLandType$`/`AllNonBasicLandType$` as a token (29 of 256 real `AddType$` lines) need a runtime value this port
has no evaluator for; `AddAllCreatureTypes$` (8) needs the full creature-type enum, which needs the `Registry` this port
does not inject; and `RemoveSuperTypes$`/`RemoveCardTypes$`/`RemoveSubTypes$`/`RemoveLandTypes$`/
`RemoveCreatureTypes$`/`RemoveArtifactTypes$`/`RemoveEnchantmentTypes$` (62 of 284 real `AddType$`/`RemoveType$` lines)
are a bulk "wipe this whole category first" flag, most often paired with `AddType$` in a real "Enchanted creature is a
Turtle" shape (`StaticAbilityContinuous.java:425-448`) — applying `AddType$` alone without the wipe the line also asks
for would leave a card with both its old and new creature types, an answer actively worse than skipping the line
outright. 201 of 284 real `AddType$`/`RemoveType$` lines carry none of the above and resolve.

Intimidate's own `CantBlockBy` synthesis
([`## Block legality: CantBlockBy`](turn-stack-combat.md#block-legality-cantblockby)) is `SharesColorWith`'s real reason
for existing in `valid.go`, not this Layer 4 slice — the two landed together but are otherwise unrelated.

**Layer 5, `applyContinuousColor` (`continuous.go`), and `ColorMod`.** `AddColor$`/`SetColor$` are Layer 4's own
sibling, the next slice of `Mode$ Continuous` once a real caller needed `Card.Colors()` to fold something too.
`ColorMod`/`ColorEffect` (`colormod.go`) copy `TypeMod`'s own structure, with one difference `AddType$`/`RemoveType$`
did not need: `SetColor$` (Java's own `overwriteColors`) replaces the running color set outright rather than unioning
into it, so `ColorEffect` carries one `Overwrite bool` instead of two separate `cardtype.Line` fields, and `foldColor`
branches on it per effect in `Timestamp` order — `SetPower$`/`AddPower$`'s own `LayerSetPT`/`LayerModifyPT` split,
collapsed to a bool since Layer 5 has no third sub-layer to distinguish. `colorFromName` (valid.go's own `colorMatches`,
pulled out so both share it rather than duplicating the five-color switch) maps the bare color words; `colorTokens`
(continuous.go) adds the two fixed tokens Java's own `getColorsFromParam` special-cases (`"All"` → `mana.AllColors`,
`"Colorless"` → no color at all, both real corpus shapes) and skips the whole line the instant `"ChosenColor"` appears
anywhere in the `" & "`-split list (7 of 61 real lines) — a runtime value (`Card.getChosenColors()`) this port has no
evaluator for, `typeTokens`'s own "whole line, not partial" choice applied identically here. 54 of 61 real
`AddColor$`/`SetColor$` lines carry none of it and resolve. `Card.Colors()` folds `ColorMod` over the printed
`Colors:`-override-or-mana-cost base the same way `Type()` folds `TypeMod`; `Move`/`Game.Clone` treat `ColorMod`
identically to `TypeMod`/`PT`.

**Layer 6, `applyContinuousKeyword` (`continuous.go`), and `KeywordMod`.** `AddKeyword$` is the single largest real
slice of all four layers this port resolves (1,556 of 1,857 real lines, ahead even of Layer 7's own 2,192 of 2,426) --
an equipment or Aura granting Flying/Trample/Menace/Ward, the single most common continuous shape in the whole corpus.
`KeywordMod`/`KeywordEffect` (`keywordmod.go`) are simpler than `TypeMod`/`ColorMod`: `HasKeyword` (card.go) only ever
asks membership ("is this keyword present"), never "what is the current value" the way `Power`/`Type`/`Colors` do, so
there is no fold order to resolve at all -- two continuous effects both granting a keyword never disagree about
anything, so `KeywordEffect` carries no `Layer`/`Overwrite` distinction, just `Timestamp` (unused today, kept for the
same reason `TypeEffect`/`ColorEffect` keep theirs: a future remove direction will need it) and `AddKeywords []string`,
each entry a whole keyword line verbatim -- exactly what a real `K:` line would carry ("Ward:2", "First Strike",
"Protection:..."), so `HasKeyword`'s own `keyword.Parse(line).Name` reads a granted one the identical way it already
reads a printed one. `HasKeyword` gaining this fold reaches every existing call site for free: `cantBlockByKeywords`
(staticability.go) and combat's own First Strike/Double Strike/Deathtouch/Trample reads (`dealsInStep`,
`dealAttackerDamage`, combatdamage.go) all start seeing a continuously-granted keyword without changing a line of their
own code --`TestApplyContinuousKeywordGrantedFlyingAffectsCanBlock` (continuous_test.go) proves the reach past a bare
`HasKeyword` check into real block legality.

`keywordTokens` (continuous.go) splits `AddKeyword$` on `" & "` the identical way `typeTokens`/`colorTokens` do, and
skips the whole line -- not just the bad token -- the instant a dynamic-value marker (`StaticAbilityContinuous.java`'s
own `removeIf` lambda: `ChosenColor`, `ChosenType`, `ChosenNumber`, `ChosenPlayer`, `ChosenName`, `ChosenEvenOdd`,
`AllColors`/`allColors`, `CommanderColorID`, `ColorsYouCtrl`/`colorsYouCtrl`, `YourBasic` -- 42 of 1,857 real lines)
appears as a SUBSTRING anywhere within any one token, checked with `strings.Contains` rather than exact-token equality:
a real corpus token often embeds the marker as a qualifier inside a larger one
(`"Protection:Card.ChosenColor:chosenColor"` is one token, not "ChosenColor" standing alone), the identical reason
Java's own check is `input.contains(...)`, not `input.equals(...)`. `RemoveKeyword$`/ `RemoveAllAbilities$` (5 of 1,561
real `AddKeyword$` lines carrying no dynamic marker) and `SharedKeywords$`/ `FromDraftNotes$` (a
game-wide/remembered-list/draft-note keyword source rather than a fixed token list) each skip the whole line too, at the
outer `applyOneContinuousKeyword` level rather than inside `keywordTokens`, since they are static-ability PARAMS, not
tokens inside the `AddKeyword$` value itself -- applying the add half of a real "gains X, loses Y" line without the
remove half `applyOneContinuousType`'s own "becomes a Turtle" paragraph already explains why not to.

**Layer 8, `applyContinuousRules`, and `RulesMod` -- this port's first player-facing continuous effect.** Every layer
above lives on `Card`; `RulesMod`/`RulesEffect` (rulesmod.go) live on `Player` instead, matched through `Affected$`
against `matchesPlayerSpec` (valid.go) rather than `Matches`.

`StaticAbilityContinuous.getAffectedPlayers`'s own `Player.isValid` call is the identical `Affected$`-splits-then-
`Player.isValid`-each-candidate shape `getAffectedCards` already has for cards, just never needed a player-shaped target
before this.

`SetMaxHandSize$` (43 real lines) and `RaiseMaxHandSize$` (8) fold onto `HandSizeLimit` (player.go). A `SetMaxHandSize$`
effect REPLACES the running limit, and Java's own `unlimitedHandSize` flag with it -- `p.setUnlimitedHandSize(false)`
runs inside the same branch as `p.setMaxHandSize(max)`, so an ordinary numeric line always clears a previous
`Unlimited`. A `RaiseMaxHandSize$` effect ADDS to it instead. Both fold in `Timestamp` order -- `foldPT`'s own combine
convention (card.go), reused for the one other layer this port's own effects can conflict within. Java's own iteration
order for two `SetMaxHandSize$` effects active on the same player at once is not pinned down anywhere citable, so
`Timestamp` is this port's own deliberate choice here, not a rediscovery of Java's.

`AdjustLandPlays$` (27) folds onto `LandPlayLimit` (player.go) more simply: `Player.getMaxLandPlays`'s own unconditional
sum (every adjustment counts, regardless of order) and `Player.getMaxLandPlaysInfinite`'s own "any one active effect
makes it unlimited" OR. Neither has a "replace" form, so nothing here is order-dependent the way hand size is.

`"Unlimited"` (Java's own literal sentinel for `setUnlimitedHandSize(true)`/`addMaxLandPlaysInfinite`) is checked before
falling to the existing `ptParam` for the numeric case: `ptParam` itself would just report `"Unlimited"` unresolvable
(neither a plain integer nor an SVar name), correct on its own terms, but `rulesEffect` (continuous.go) needs to tell
"no maximum" apart from "could not resolve this" before it ever calls `ptParam` at all.

`HandSizeLimit`/`LandPlayLimit` both take the printed default as a parameter (`turn.go`'s own `MaxHandSize`, `land.go`'s
own `maxLandPlays`) rather than reading either constant directly. `player`'s own `enginelint` group would otherwise need
`turn` and `land` in its allow-list, and `turn`/`land` already allow `player` -- a real cycle, not the one-way
dependency every other cross-group reference in this port has been. `cleanupStep` (turn.go) and `PlayLand` (land.go) --
both of which already had a doc comment naming this exact gap, written well before this landed -- now call them instead
of comparing against the bare constants.

75 of the corpus's 78 real `SetMaxHandSize$`/`RaiseMaxHandSize$`/`AdjustLandPlays$` lines resolve. 2 carry a qualified
`Affected$` value `matchesPlayerSpec` cannot resolve (`Player.NotedForGreenAnchor`, `Player.Chosen`). 1 more --
Delirium's own "each opponent's maximum hand size is seven minus..." -- carries `Condition$`, which no static-ability
mode this port checks has an evaluator for.

Not attempted here: `MayLookAt$`/`MayPlay$` (88/181 real lines corpus-wide, the layer's own largest real params by far)
need a cast-time zone-eligibility permission `CastSpell`'s own hand-only check (castspell.go) has nowhere to consult
yet. `AddHiddenKeyword$` (19) has 8 real distinct values ("must be blocked if able," "can't attack alone," "doesn't
untap," ...), each its own separate block/attack/untap-step rule this port's combat/turn model has no hook for, none
sharing enough machinery to be worth building as one slice the way `SetMaxHandSize$`/`AdjustLandPlays$` did.
`ControlOpponentsSearchingLibrary$`, `ControlVote$`, `AdditionalVote$`, `AdditionalOptionalVote$`,
`AdditionalVillainousChoice$`, `DeclaresAttackers$` and `DeclaresBlockers$` (0-3 real lines each) are multiplayer/vote
mechanics this port has no concept of at all.

`TestApplyContinuousRulesSetsUnlimitedHandSize`, `TestApplyContinuousRulesSetsFixedHandSize`,
`TestApplyContinuousRulesRaisesHandSize`, `TestApplyContinuousRulesAdjustsLandPlays`,
`TestApplyContinuousRulesGrantsUnlimitedLandPlays`, `TestApplyContinuousRulesAffectedOpponentSkipsTheHostsOwnController`
and `TestApplyContinuousRulesSkipsLineWithCondition` (continuous_test.go) prove the fold mechanism directly;
`TestCleanupDoesNotDiscardWithUnlimitedHandSize` (turn_test.go) and
`TestPlayLandSucceedsPastTheDefaultLimitWithAdjustLandPlays`/`TestPlayLandSucceedsRepeatedlyWithUnlimitedLandPlays`
(land_test.go) prove the two real callers actually read it. The first attempt at the land-play tests used a
single-player game, `TestPlayLandMovesCardToBattlefieldAndCountsIt`'s own precedent -- and failed, because
`CheckStateBasedActions`'s own win-condition check (`remaining == 1` --> `Won = true`, `action.go`) returns before
`applyContinuousPT`/.../`applyContinuousRules` ever run in a one-player game, something every earlier single-player
`land_test.go` case had simply never called `CheckStateBasedActions` at all to notice. A real, self-caught ordering bug
in the test, not the implementation -- fixed by using a two-player game with both players' `Life` set, this section's
own established convention for any test that calls `CheckStateBasedActions` directly.

**Layer 2, `applyContinuousControl`, and `ControlMod` -- this port's first controller-change mechanism.**
`Card.Controller` stops being a plain field, set once in `NewCard` and never mutated anywhere else this port had built
until now, and becomes `Card.Controller()` (card.go): a fold over a new `ControlMod`/`ControlEffect` (controlmod.go) the
identical "highest `Timestamp` wins" shape `Card.Power`/`Toughness` already use for `PT`.

Ported from `Card.java`'s own `tempControllers` (a `NavigableMap<Long, Player>`) and `getController()`: the
highest-timestamp entry wins, if any exist, else the base falls through. Java's own `getController()` carries an extra
guard -- a temp-controller only wins if its own timestamp beats `controllerTimestamp`, the stamp on Java's own
`setController` (an explicit "gain control permanently" one-shot effect,
[`## Not ported yet`](../game-state.md#not-ported-yet)) -- which collapses away here: this port builds no equivalent of
`setController` yet, so there is no base-controller timestamp for a temp-controller to ever lose to, and any
`ControlEffect` unconditionally outranks the base.

`applyContinuousControl`/`applyOneContinuousControl` (continuous.go) resolve `GainControl$ You`:
`StaticAbilityContinuous.java`'s own CONTROL branch calls
`AbilityUtils.getDefinedPlayers(hostCard, params.get("GainControl"), stAb).get(0)`, a "defined player" lookup rather
than the `Affected$`-for-players membership test `RulesMod`'s own dispatch is, since `GainControl$` names WHO gains
control instead of describing a set to test candidates against -- `getDefinedPlayers`'s own `"You"` case is
`players.add(player)`, and `player` is `card.getController()` whenever `sa` is not a `SpellAbility` (every real
`Mode$ Continuous` static here), i.e. the effect's own host, `host.Controller()` below.

Corpus-frequency research here first had to separate two unrelated mechanics sharing one param name: a naive
`grep -rn "GainControl\$"` finds 58 real lines, but only 44 of them are `S:Mode$ Continuous` lines -- the other 14 are
`DB$ ChangeZone | ... | GainControl$ True` (Restoration Angel, Rise from the Grave) or
`DB$ Dig | ... | GainControl$ True`, `ChangeZoneEffect`'s own one-shot "put onto the battlefield under your control"
effect, entirely unrelated to this layer and part of M6's own remaining 202 script effects. Of the 44 real
`Mode$ Continuous` lines, 43 name `GainControl$ You`; the last, `GainControl$ Player.isMonarch`, is a qualified
`getDefinedPlayers` form (the `else` branch's own `game.getPlayersInTurnOrder()` filtered by
`PlayerPredicates.restriction`) this port has no monarch mechanic to filter by, so the whole line is skipped
(PORT-8/GO-7) rather than guessing "the controller" and being wrong the instant any game actually changes hands.
`Affected$` on the 44 real lines is overwhelmingly `Card.EnchantedBy`/`Permanent.EnchantedBy`/`Creature.EnchantedBy` (42
of 44, Control Magic's own shape -- the Aura's own host), needing nothing new: the identical `Matches`-driven
valid-string match `applyOneContinuousPT`'s own `Affected$` already does.

`applyContinuousControl` runs FIRST among the six appliers (`CheckStateBasedActions`, action.go), ahead of
`applyContinuousPT`/`Type`/`Color`/`Keyword`/`Rules`: CR 613.1 puts the control layer before every one of them, and
concretely, several of their own `Affected$` specs can themselves read `Controller()` (a `"YouCtrl"` property) -- a
stale value there would evaluate an anthem's own `Affected$ Creature.YouCtrl` against last pass's controller, not this
one's, the moment a `GainControl$` effect and an anthem effect are both in play at once.
`TestApplyContinuousControlRunsBeforeKeywordSoYouCtrlSeesTheNewController` (continuous_test.go) proves the ordering
directly, not just the individual fold.

Converting `Card.Controller` from a field to a method meant converting every read of it, roughly eighty call sites
across `staticability.go`, `valid.go`, `manaability.go`, `draweffect.go` (an `Ability.Controller` read, a different
field on a different type, left alone), `action.go`, `stack.go` (also `Ability.Controller`, left alone), `land.go`,
`trigger.go` (both `Card.Controller` reads AND `Ability.Controller` reads share the file; only the former needed
converting -- `pushTriggeredAbilities`'s own `a.Controller` is the pushed ability's controller, not a card's),
`combatdamage.go`, `continuous.go`, `attack.go`, `castspell.go`, plus `internal/fixture`'s own `dump.go`/`load_test.go`
(a different package, needing the same field-to-method rename since it reads a card's controller for its own dump/load
round-trip check). Two engine tests (`action_test.go`) that directly wrote `g.Card(x).Controller = b` to fake "someone
already controls this" for `hostRefusesEnchant`/`cleanupDanglingAttachments` coverage could not keep doing that: a
direct field write has nothing left to write to, and even if it did, `applyContinuousControl`'s own
clear-and-rebuild-every-pass contract would wipe it the instant `CheckStateBasedActions` runs. Both now build a real
`GainControl$ You | Affected$ Card.IsRemembered` permanent and `Memory.Remember` the target instead -- the real
mechanism exercising the exact behavior the test wants, rather than a field poke standing in for it.

`ControlEffect`'s own field is named `Controller`, not `Player`: `enginelint` walks every identifier in the package,
including a struct field's own name and a struct-literal key, and cannot tell a field named `Player` apart from a
reference to the `Player` type itself (`declaredIn["Player"]` resolves to `player.go` either way) -- `Player` would have
been flagged as `parts` illegally referencing `player`, a real false positive in the tool's own identifier scan, not a
real dependency (`topLevelNames`, tools/enginelint/lint.go, already excludes methods from this map for the same reason;
a struct field is not a method, so it is not excluded). `Controller` avoids the collision outright: nothing top-level in
the package is named that (only the _method_ `Card.Controller()` is, and methods are excluded).

`TestApplyContinuousControlGrantsControlOfEnchantedCreature`,
`TestApplyContinuousControlLeavesUnenchantedCreaturesAlone`,
`TestApplyContinuousControlSkipsUnresolvedGainControlValue`, `TestApplyContinuousControlSkipsLineWithCondition` and
`TestApplyContinuousControlRunsBeforeKeywordSoYouCtrlSeesTheNewController` (continuous_test.go) prove the fold mechanism
directly; `TestCheckStateBasedActionsAuraGoesToOwnersGraveyard` and
`TestCheckStateBasedActionsAuraGoesToGraveyardWhenEnchantPropertyStopsMatching` (action_test.go, rewritten as above)
prove a real caller reads it.

Not resolved: the qualified `GainControl$ Player.isMonarch` (1 of 44, above). Layer 1 (copy effects) and Layer 3
(`GainTextOf$`) stay untouched -- [`## Not ported yet`](../game-state.md#not-ported-yet), has the reasons.

### `Condition$`: the one gate all six appliers share

Every one of the six appliers above (`applyOneContinuousPT`/`Type`/`Color`/`Keyword`/`Rules`/`Control`) used to skip a
line outright the instant it carried a `Condition$` param at all, each one's own doc comment naming this the same
missing piece. `continuousConditionMet` (continuous.go) closes most of it: `StaticAbility.checkConditions`'s own
`Condition$` switch (StaticAbility.java), called from all six in place of the blanket skip, evaluated fresh every
`CheckStateBasedActions` pass the same as everything else Layer 4-8/2 fold, since CR 613 gives a continuous effect no
memory of its own last evaluation.

The corpus's own 317 real `S:Mode$ Continuous | Condition$` lines, tallied directly off `S:` lines carrying
`Mode$ Continuous` (a plain corpus grep for bare `Condition$` also catches values on `T:`/`A:` lines Trigger.java's own
`meetsRequirementsOnTriggeredObjects` and SpellAbilityCondition.java handle separately -- a different switch on the same
param name, not this port's problem here):

| Value           | Real lines | Resolved | Player state read                                                             |
| --------------- | ---------: | :------: | ----------------------------------------------------------------------------- |
| `PlayerTurn`    |        141 |   yes    | `Game.ActivePlayer() == host.Controller()`                                    |
| `Threshold`     |         61 |   yes    | `len(Zone(Graveyard, controller).Cards()) >= 7` (`Player.hasThreshold`)       |
| `MaxSpeed`      |         40 |    no    | Alchemy's own speed counter -- tracked nowhere in this port                   |
| `Delirium`      |         23 |   yes    | four-plus distinct core types unioned across the graveyard (below)            |
| `Metalcraft`    |         18 |   yes    | three-plus battlefield permanents whose `Type()` carries Artifact             |
| `Blessing`      |          9 |    no    | City's Blessing (ten-plus permanents, sticky) -- no such flag on `Player` yet |
| `NotPlayerTurn` |          8 |   yes    | the inverse of `PlayerTurn`                                                   |
| `Hellbent`      |          8 |   yes    | `len(Zone(Hand, controller).Cards()) == 0` (`Player.hasHellbent`)             |
| `EnduringStory` |          4 |    no    | a Saga's own lore-counter/chapter state -- Sagas are not ported               |
| `FatefulHour`   |          3 |   yes    | `Player.Life <= 5`                                                            |
| `Monarch`       |          2 |    no    | no monarch mechanic (same gap Layer 2's own qualified value has, above)       |

262 of 317 resolve. The four that do not (55 lines) are each its own untracked mechanic, so (the identical "cannot
evaluate, so do not apply" rule an unresolved `Affected$` value already has, GO-7) the whole line is skipped, same as
before this slice existed -- `continuousConditionMet`'s own `default` case, which also catches any value the real corpus
does not carry today.

`Delirium` is `AbilityUtils.countCardTypesFromList(graveyard, false)` (`graveyardCoreTypeCount`): every graveyard card's
own _current_ (Layer-4-folded) `Type()` unioned into one running `cardtype.Line` via `Union` -- reusing item 27's own
type-folding machinery rather than re-deriving a card's type from its printed face -- then `len(.CoreTypes())` against
4, core types only (not supertypes, not subtypes), matching `CardType.CoreType`'s own enum exactly. `Metalcraft` is
`battlefieldArtifactCount`: the same `Type()` read, `.Has(cardtype.Artifact)`, over the controller's own battlefield.

Winter, Misanthropic Guide's `Condition$ Delirium | Affected$ Opponent | SetMaxHandSize$ Y` (Layer 8) applies: `Y` is
`Number$7/Minus.X` over `X`, `Count$ValidGraveyard Card.YouOwn$CardTypes`, both of which `resolveAmount` evaluates
(`TestAmountDrivesWinterMaximumHandSize`).

`TestApplyContinuousPTSkipsConditionParam` moved from `Condition$ PlayerTurn` (now resolvable, and coincidentally false
in a fresh test game with no active player set -- `NoPlayer` matches no real `PlayerID`) to `Condition$ MaxSpeed` to
keep proving what its name claims: an unresolvable value skips the line regardless of game state.
`TestApplyContinuousRulesSkipsLineWithCondition`/`TestApplyContinuousControlSkipsLineWithCondition` moved the same way.
Ten new tests (`TestApplyContinuousPTAppliesWhen*ConditionMet`/`SkipsWhen*ConditionNotMet`, continuous_test.go) prove
each resolvable value both ways, driven through `g.StartTurn` (`PlayerTurn`) or a matching zone/type/life setup
(`Threshold`/`Hellbent`/`Metalcraft`/`Delirium`/`FatefulHour`) -- `Rules`/`Control` reuse the identical shared function,
so are not re-proven per value there.
