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

## Known defects it surfaces

Compiling the corpus found five cards whose `SubAbility$` names an SVar that does not exist. Four are fixed; the fifth,
`typhoid_mary_fractured`, is exempted by name in the corpus test until its author decides what the missing `DBCharm`
should be. All five are in [`../card-script-defects.md`](../card-script-defects.md).
