# Port: AbilityFactory

- **Java source:** `forge-game/src/main/java/forge/game/ability/AbilityFactory.java` (getAbility, getSubAbility,
  additionalAbilityKeys), `forge-core/src/main/java/forge/util/FileSection.java` (parseToMap)
- **Go target:** `crucible/internal/carddb/compile`
- **Status:** Structure done — M3 slice A. Param values are still text; typing them is the generated layer

## What it does

Turns an ability line into a node: what kind of record it is, which API it names, its params, and its sub-abilities
resolved into direct references.

`SubAbility$ DBFoo` means "resolve `SVar:DBFoo` next", so a card is a linked list of effects. Java walks that list by
name every time a `Card` is constructed; Crucible resolves it once at load, which is ADR-0007's whole point.

## The four reference shapes

All four are AbilityFactory's, and the API gate on the last two is not decoration — `Choices$` on any other API is a
valid string, and resolving it as a list of SVar names would fail on cards that are correct.

| Shape                         | Resolves to      | Gate                                                                               |
| ----------------------------- | ---------------- | ---------------------------------------------------------------------------------- |
| `SubAbility$ X`               | One SVar         | None                                                                               |
| 29 additional-ability keys    | One SVar each    | None. Java's `additionalAbilityKeys`, plus `SubAbility` and `PreventionSubAbility` |
| `Choices$ A,B,C`              | A list           | API is `Charm`, `GenericChoice`, `AssignGroup`, `VillainousChoice` or `Vote`       |
| `ResultSubAbilities$ 1:A,2:B` | `key:svar` pairs | API is `RollDice`                                                                  |

## Param maps are maps, not lists

`FileSection.parseToMap` reads params into a `TreeMap` with `String.CASE_INSENSITIVE_ORDER`. Two consequences, both
load-bearing and both measured in the corpus:

| Consequence                              | Corpus                                                 |
| ---------------------------------------- | ------------------------------------------------------ |
| A repeated key keeps only its last value | 29 lines on 24 cards repeat a key                      |
| Key spelling ignores case                | 5 keys have two spellings, including one `SubABility$` |

Reading the params as a list instead chains a sub-ability Forge never chains — `the_eagles_are_coming` writes
`SubAbility$` twice, and only the second exists — and misses one it does.

Order is kept anyway, because Java's TreeMap loses it and nothing should depend on that.

## Deviations from Java

| Java                                                            | Go                                                                                                                    |
| --------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| An unresolved `SubAbility$` prints to stdout and returns `null` | `error` naming the card and the reference. A chain that ends early is a defect, not a state (PORT-8)                  |
| A cycle would recurse until the stack ends                      | `ErrCycle`, detected by the set of SVars on the current chain                                                         |
| `AbilityRecordType` covers `AB`, `SP`, `ST`, `DB`               | Seven records: those four plus `RE`, and `Mode$` split into `Trigger` and `StaticEffect` by the line carrying it      |
| Resolution happens per `Card` construction                      | Once per script at load (ADR-0007). A game builds ~120 cards, and a million-game run would repeat the work 10^8 times |

## Not ported yet

| Java                                                                             | When       |
| -------------------------------------------------------------------------------- | ---------- |
| Typed params. Values are text here                                               | M3 slice C |
| Costs, valid strings, count expressions -- the values themselves                 | M3 slice D |
| Keyword expansion (`CardFactoryUtil.setupKeywordedAbilities`)                    | M3         |
| SVars naming statics, triggers or replacements (`StaticAbilities$`, `Triggers$`) | M4         |
| Functional variants -- `Variant:` faces have their own lines                     | M3 slice C |

## Golden AST diff

`compile.WriteCanonical` writes a compiled card as deterministic, indented text; `compile.Fingerprint` hashes it.
`testdata/ast.golden` carries one `filename<TAB>hash` line per card, and `testdata/ast-shapes.golden` carries the full
text of six cards chosen to cover every record type and every way one ability names another.

Two files rather than one because they answer different questions. The hash golden says **which** of 33,689 cards
changed, which is the only thing a 34,000-line diff can usefully say. The shapes golden says **what** a change looks
like, which is what a reviewer actually needs.

Regenerate with `-update`, and read the diff before committing it. A golden updated without being read converts an
unexplained change into an approved one, which is worse than having no golden.

## `ReplaceWith$` and three more keys the list misses

`AbilityFactory.additionalAbilityKeys` is not the whole set of params whose value is an SVar holding an ability. Four
more are resolved by the handler or the effect that needs them, and reading only the list leaves them unfollowed:

| Key                              | Resolved by                   | Corpus uses |
| -------------------------------- | ----------------------------- | ----------: |
| `ReplaceWith`                    | `ReplacementHandler.java:843` |       1,581 |
| `ExtraTurnDelayedTriggerExecute` | `AddTurnEffect.java:50`       |           7 |
| `ExtraPhaseDelayedTriggerExcute` | `AddPhaseEffect.java:72`      |           4 |
| `Else`                           | `RollDiceEffect.java:474`     |           0 |

The misspelling in `ExtraPhaseDelayedTriggerExcute` is Forge's own and is load-bearing: the param map is keyed on it, so
correcting it in the Go port would stop the key matching.

`ReplaceWith` is the one that mattered. Every replacement effect in the corpus names its ability that way, so before
this the compiled AST stopped at the replacement line for 1,276 cards, and `TestCorpusCompiles` could not have found a
dangling `ReplaceWith$` -- the same defect class as the five dangling `SubAbility$` references it did find. The corpus
has none today, but it was not being checked.

The golden AST diff is how this surfaced: `leader_super_genius` showed `param ReplaceWith$ "DBDraw"` with no `sub` line
under it.

## What type is a param?

ADR-0007 says parameters become typed structs per API — `DealDamageParams` holding an `expr.Amount` and a `valid.Spec`,
not two strings. Nothing in Forge declares those types, so they have to be recovered, and `tools/apiscan -kinds` is the
measurement. Two independent signals, because neither is sufficient:

| Signal             | Settles                                                                             | Cannot settle                                      |
| ------------------ | ----------------------------------------------------------------------------------- | -------------------------------------------------- |
| The Java call site | `calculateAmount` → amount, `ZoneType.smartValueOf` → zone, `getValidCards` → valid | A key with no distinctive consumer                 |
| The written values | Every value `True`/`False` → flag; an expression head → amount; spaces → prose      | Zone vs valid string vs SVar name: all bare tokens |

The type belongs to the **(API, key) pair**, not the key: `AttachedTo` is a valid string on some effects and an object
selector on others, and one type for both would be wrong for both. Over the 6,684 pairs the corpus writes:

| Evidence                    | Pairs | Share |
| --------------------------- | ----: | ----: |
| Typed by the API's own Java |   337 |    5% |
| Typed by shared code        | 1,237 |   19% |
| Flag, from its values alone | 1,201 |   18% |
| No evidence                 | 3,909 |   58% |

Per-API attribution earns its keep on exactly the ambiguous pairs: `AttachedTo` resolves to `defined` on `CopyPermanent`
and `Token`, and stays split on `ChangeZone`, where that effect genuinely uses it both ways.

**So the generator cannot type everything, and should not pretend to.** A pair with no evidence gets a `string` field,
which is what Java has anyway; the win is that the 2,775 with evidence get a real type, and GO-8's actual requirement —
no `any`, no `map[string]string` — is met by the struct either way.

The signals also disagree on real keys, and that is the finding worth keeping rather than averaging away:

```text
AttachedTo   valid:9,defined:11
Choices      valid:40,defined:2
NumCards     amount:24,int:2
```

`AttachedTo` is a valid string on some effects and an object selector on others. A per-key type would be wrong for one
of them, which is why the type belongs to the (API, key) pair and not to the key — the same reason the vocabulary itself
had to be attributed per effect.

`tools/apiscan/testdata/param-kinds.golden` pins all 6,684 rows — API, key, uses, the API's own evidence, the shared
evidence, the written shapes — so an upstream refactor that moves a `getParam` call becomes a diff someone reads rather
than a silent change in a generated field's type.

## An unknown key is not an error

Java accepts any key. `AbilityFactory` builds the map, the effect asks for what it knows, and a key nobody asks for is
neither rejected nor logged. The Go compile matches that — refusing an unknown key would reject cards Forge plays — so
the check lives outside the compiler, in `tools/apiscan -check`, which fails the build on a key no Java code reads
anywhere.

Two Java behaviours the port depends on, both found by that gate:

| Behaviour                                                     | Where                                    |
| ------------------------------------------------------------- | ---------------------------------------- |
| `matchesValidParam` returns **true** when the param is absent | `CardTraitBase.java:259-265`             |
| An absent param takes the effect's default, not a failure     | e.g. `getParamOrDefault` on every effect |

The first is the one that bites: a missing `Valid…` key does not narrow a restriction, it removes it.

## Known defects it surfaces

Compiling the corpus found five cards whose `SubAbility$` names an SVar that does not exist. All five are fixed and
carried until upstream merges them, so the corpus compiles whole with no exemption. Each is in
[`../card-script-defects.md`](../card-script-defects.md) with the printed text it was checked against.

The param gate found thirteen more keys that no Java code reads, across seventeen cards, in the same file.

## The generated parameter structs

`tools/genparams` turns the evidence table into one struct per ability API — 187 of them, 4,674 fields — plus a fill
constructor each and a generated `ParseParams` switch. No reflection, no `init()` registry: the shape ADR-0008 describes
for effect dispatch, applied to parameters.

The rule that turns evidence into a field type lives in `fieldType`, in one place, and its order is deliberate:

1. **Unanimous values decide first.** "Every card ever written puts `True` or `False` here" outweighs a single call
   site.
2. **Then the API's own Java**, because a key means different things on different effects.
3. **Then the shared code**, which is right whenever the effect does not handle the key itself.
4. **Anything ambiguous or unevidenced stays a `string`.** A wrong type is worse than an untyped one, because it
   compiles.

| Field type    | Fields |
| ------------- | -----: |
| `string`      |  2,792 |
| `bool`        |    993 |
| `expr.Amount` |    254 |
| `valid.Spec`  |    194 |
| `int`         |     68 |

Two Java behaviours the generator reproduces rather than tidies. Keys are matched folded, because the param map is a
`TreeMap(CASE_INSENSITIVE_ORDER)` — the corpus writes both `UpTo$` and `Upto$`, and they are one parameter and one
field. A `bool` field is set by the key being **present**, not by its value being `True`, which is what `hasParam`
means: `Reveal$ False` turns the flag on. Reproducing that is the point (PORT-7).

`TestEveryAbilityGetsTypedParams` runs all 81,830 compiled abilities through the switch: 56,751 reach a generated struct
and the remaining 220 record names are trigger modes, static modes and replacement events, whose params the handlers
read rather than an effect. An API missing from the switch fails the test, so the generated file cannot silently fall
behind `ApiType`.

## `LoadDB` compiled before resolving `CopyFaceFrom:`

`compile.DB.LoadDB` walks the corpus, parsing and compiling each script inside the same pass. That fails any card with
`CopyFaceFrom:` — Bind // Liberate among them — because `carddb.ParseScript`'s own doc comment says the placeholder face
it leaves behind needs the whole corpus read first: `carddb.ResolvePlaceholders` is a second pass, over every parsed
card together, not a per-file step. `LoadDB` never took that second pass.

This is not a Forge defect (PORT-8) — it is Crucible's own, in code M3 shipped. Nothing had called `LoadDB` against the
real corpus before `internal/engine`'s scenario harness did (`porting/port-log/game-state-fixture.md`); the P1/P2 gates
that claim the whole corpus compiles clean go through `tools/carddump` and `compile_test.go`'s own `parseCorpus`, which
already call `ResolvePlaceholders` themselves. Fixed by splitting `LoadDB` into the same two passes: parse every file,
resolve placeholders once across all of them, then compile.

## Token scripts and the subtype vocabulary on `compile.DB`

`LoadTokenScripts` compiles every `res/tokenscripts` file, keyed by file name without `.txt` -- the name `TokenScript$`
writes, and Java's `TokenDb` key. The table sits beside `byName`, not in it: many token scripts share one printed name
("Soldier Token"), and nothing looks a token up by name. `DB.WithTokens` attaches it to a copy, so a DB stays read-only
once shared (ADR-0005); `DB.Token` on a nil DB reports nothing. `TestTokenScriptsCompile` compiles all 853 files, the
token half of the P2 gate.

`LoadDB` also keeps the `TypeLists.txt` registry it parsed the corpus with (`DB.Types`; `WithTypes` for a hand-built
DB). The engine's type-changing effects need it to remove a whole subtype category -- Animate's `RemoveCreatureTypes$`
asks "is this subtype a creature type," which only the vocabulary answers.

## Face names and split type on the compiled card

`compile.Face.Name` is each face's printed name and `compile.Card.SplitType` the card's `AlternateMode:`, copied from
`carddb` at compile time. Transform reads both: a transforming double-faced permanent turns to a one-face definition
built from its other face, which needs that face's own name. Neither is part of the AST fingerprint, which covers
abilities only. `DB.Card`, `DB.Len` and `DB.Names` answer "nothing" on a nil DB, like `DB.Token`.
