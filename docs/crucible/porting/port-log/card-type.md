# Port: CardType

- **Java source:** `forge-core/src/main/java/forge/card/CardType.java` (1,083, of which the static half is ported), its
  `Constant` and `Helper.parseTypes` inner classes, and the loader in
  `forge-gui/src/main/java/forge/model/FModel.java:336` (`loadDynamicGamedata`)
- **Data:** `forge-gui/res/lists/TypeLists.txt` (576 lines, 10 sections)
- **Go target:** `crucible/internal/cardtype`
- **Status:** Done for static card data — M1, P0 gate. Type-changing effects are M4

## What it does

Turns the `Types:` line of a card script into a value: a set of supertypes, a set of core types, and an ordered list of
subtypes. `Legendary Creature Elf Warrior` becomes two sets and two strings, so the engine answers "is this a creature"
with a bit test instead of a string comparison, millions of times per batch.

Subtypes are open-ended — every creature type ever printed — so they stay strings, checked against a vocabulary loaded
from `TypeLists.txt`. That vocabulary is also what makes `Time Lord` one subtype rather than two.

Both that file and the card scripts are upstream inputs, so `crucible-go.yml` triggers on them and a sync that adds a
type is caught by the corpus golden on the sync itself (see [`mana-cost.md`](mana-cost.md)).

## Deviations from Java

| Deviation                                                                    | Reason                                                                                                                                                                                                          |
| ---------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| The vocabulary is a `*Registry` passed in, not `CardType.Constant`'s statics | `Constant` is a mutable static guarded by a `LOADED` flag — the exact singleton shape GO-2 bans. One immutable registry is shared by every game instead                                                         |
| `" - "` is a separator, not a word                                           | Java splits on spaces and lets the dash become a subtype, which survives only because the next step deletes subtypes that make no sense. Treating it as a separator is what makes `Parse(l.String())` equal `l` |
| Multiword subtypes match longest-first                                       | Java takes the first `startsWith` hit from a `HashSet`, so the result depends on hash order. Iteration order is load-bearing here (GO-12)                                                                       |
| `Parse` returns no error                                                     | No type line can fail. Nothing is dropped either: `CardType.parse` adds each word with `add()`, which never calls `sanisfySubtypes`, so Forge's own database holds `Contraption` and `Killbot` as written       |
| Core and supertypes are bitmasks; subtypes stay an ordered slice             | Java uses `EnumSet` (iterates in declaration order) and `LinkedHashSet` (iterates in insertion order). Bitmask plus slice reproduces both, and `Line` stays copyable                                            |
| `Line` is immutable, and `Parse` is the only constructor                     | Java's `CardType` is mutable, with `add`, `remove`, `clear` and a `calculatedType` cache invalidated on every write. Static card data never changes after load (PORT-2)                                         |

## Not ported yet

| Java                                                                          | When                                                           |
| ----------------------------------------------------------------------------- | -------------------------------------------------------------- |
| `allCreatureTypes`, `excludedCreatureSubtypes`, and the `(All except X)` form | Changeling and type-changing effects, M4-M6                    |
| `getTypeWithChanges`, `ICardChangedType`, `CardChangedType`                   | The continuous-effect layer system, M5                         |
| `add`, `remove`, `setCreatureTypes`, `clear`                                  | Same — mutation only happens to a card in play                 |
| `isOutlaw`, `isParty` and their `OUTLAW_TYPES` / `PARTY_TYPES` sets           | The effects that ask, M6                                       |
| `getSortedSubTypes`, `compareTo`, `toGamePieceType`                           | Deck editor and non-constructed formats; out of scope (PORT-6) |

## Null decisions

| Java                                                          | Go                                                                      |
| ------------------------------------------------------------- | ----------------------------------------------------------------------- |
| `CoreType.getEnum` / `Supertype.getEnum` return `null`        | `(CoreType, bool)` and `(Supertype, bool)`                              |
| `getMultiwordType` returns `null` when no multiword type fits | `(string, bool)`                                                        |
| A missing registry                                            | Panic, not an error: no card script can cause it, so it is a bug (GO-7) |

## Open questions

- **Twenty-four subtypes in the corpus are not in `TypeLists.txt`** — `Killbot`, `Clamfolk`, `Cow`, `Omenpath`,
  `Sivitri`, `B.O.B.` and the rest, pinned in `internal/cardtype/testdata/unknown-types.golden`. Forge keeps them on the
  card anyway: they are absent from the vocabulary, not from the type line. What the vocabulary decides is which
  subtypes survive a type-changing effect, which is M4's problem, not the database's.
- **`Dungeon Master` parses as the core type `Dungeon` plus the unknown word `Master`.** Java does the same, because the
  multiword list has no entry for it. Pinned rather than fixed (PORT-7).
- **A `-` in a script's type line is a subtype.** `gandalf_shadows_foe` writes the printed form,
  `Legendary Creature - Avatar Wizard`, and Forge stores a literal `-` subtype for it, printing
  `Legendary Creature - - Avatar Wizard`. That is what the P1 diff demanded, and it costs the round-trip property
  `Parse(l.String()) == l`, which this parser was never required to have.
