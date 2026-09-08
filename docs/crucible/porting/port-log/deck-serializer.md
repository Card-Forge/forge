# Port: Deck serializer

- **Java source:** `forge-core/src/main/java/forge/deck/DeckSection.java` (49 of it), `Deck.java` (`loadDeckSections`),
  `CardPool.java` (`processCardList`), `forge/util/FileSection.java` (`parseSections`, `parse`), and the name indexing
  in `forge/card/CardDb.java`
- **Go target:** `crucible/internal/deck`, plus the lookup in `crucible/cmd/crucible`
- **Status:** Done — M2

## What it does

Reads a `.dck` file into sections of card entries. A decklist is what scopes the port: ADR-0011 defines card support by
the decks being simulated, so this is the file that says which cards have to work.

Nothing here resolves a card. Matching a name to the database is the caller's job, because the rules for it belong to
`CardDb` rather than to the file format.

## Deviations from Java

| Java                                                             | Go                                                                                                              |
| ---------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| `Deck` holds a `CardPool`, which resolves names against `CardDb` | `Deck` holds entries as written. Resolution needs the database, and the format does not                         |
| A non-section header is skipped silently                         | Recorded in `UnknownSections`, so a `[quest]` block answers "why is that card missing" without opening the file |
| `Parse` throws on nothing                                        | Returns no error at all, for the same reason: nothing in the format can fail                                    |

## Quirks reproduced rather than fixed (PORT-7)

Each of these is a case where being stricter than Forge would refuse a decklist Forge opens.

| Behaviour                                                | Where it comes from                                                                                               |
| -------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| A bare `4` on its own line is a card **named** "4"       | `((\d+)\s+)?(.*?)` needs whitespace after the digits, so with none they fall into the name group                  |
| A metadata line with no `=` is a key with an empty value | `FileSection.parse` does `v.length > 1 ? v[1] : ""`. 1,945 decklists have a multi-line `Description`              |
| Section headers and metadata keys match without case     | `smartValueOf` compares with `compareToIgnoreCase`; the metadata map is a `TreeMap` with `CASE_INSENSITIVE_ORDER` |

## Name lookup, which is `CardDb`'s and not this package's

The coverage command reproduces three rules from `CardDb`'s indexes, because a decklist that Forge opens must not be one
Crucible calls unsupported:

- **Case is ignored.** Every name map there is built with `String.CASE_INSENSITIVE_ORDER`, so `Knight Of The Reliquary`
  finds the card.
- **Accents are folded.** `CardDb` also indexes `StringUtils.stripAccents(name)`, so `Lim-Dul's Vault` finds
  `Lim-Dûl's Vault`. Go's standard library has no equivalent, so `carddb.NormalizeName` folds the sixteen accented runes
  the corpus actually uses, measured rather than guessed (GO-14).
- **A split card answers to three names.** `CardRules.getName` joins the faces of a `Split` card with `" // "`, and each
  face is addressable on its own.

## Open questions

- **Ligatures are not folded.** A decklist writing `AEther Adept` finds nothing, because the corpus spells it
  `Aether Adept` and `stripAccents` leaves `AE` alone — so Forge misses it too. Left alone deliberately: reproducing a
  miss is fidelity, and inventing a match is not.
- **`Edition` and `Extra` are parsed and unused.** Crucible plays cards, not printings, so the set code only matters if
  a future gauntlet pins one.
