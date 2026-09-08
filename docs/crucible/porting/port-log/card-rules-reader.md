# Port: CardRules.Reader

- **Java source:** `forge-core/src/main/java/forge/card/CardRules.java` (943, of which `Reader` is roughly the last
  third), `CardFace.java` (279), `CardSplitType.java` (49)
- **Go target:** `crucible/internal/carddb` — not written yet; this note is the specification it is written from
- **Status:** In progress — M2, slice A of D

## What it does

Turns one card script into a `CardRules`: an array of seven `CardFace` values plus card-level metadata (split mode, AI
hints, deck hints, meld partner, tokens the card can create).

The parser is one `switch` on the **first character** of the key, then a string comparison against the whole key. A key
matching no case is ignored without complaint, which is how two malformed lines have survived in the corpus
([`../dsl/01-card-script-grammar.md`](../dsl/01-card-script-grammar.md)).

Ability lines — `A:`, `K:`, `T:`, `S:`, `R:`, and most `SVar:` bodies — are stored as **strings**. Nothing about them is
interpreted here; that is M3's job (PORT-2). This unit's entire responsibility is structure: which face, which key,
which raw value.

## The state machine, which is the whole difficulty

Everything else is a field assignment. Faces are not.

| Line                    | Effect on state                                                             |
| ----------------------- | --------------------------------------------------------------------------- |
| `Name:`                 | Constructs the face at the current index. A face does not exist before this |
| `ALTERNATE`             | Current index becomes 1. No colon, no value                                 |
| `SPECIALIZE:<COLOUR>`   | Current index becomes 2-6 for `WHITE BLUE BLACK RED GREEN`                  |
| `Variant:<name>:<line>` | Re-parses the remainder onto a named variant face of the current face       |
| `CopyFaceFrom:<name>`   | Records a placeholder for the current index, resolved after the full load   |
| `AlternateMode:<mode>`  | Records how the faces relate. **Does not change the current index**         |

`Variant:` is recursion, not iteration: it calls `parseLine` again with a different target face, so a variant line can
carry any key the parser understands.

`CopyFaceFrom:` is the only cross-card dependency in the whole reader. The face is filled in from another card after
every script has been read, which means a single-card parse cannot be complete on its own.

## Deviations from Java, planned

Nothing is implemented yet. These are decisions this note exists to fix before code makes them by accident.

| Java                                                           | Go                                                                                                                                                                     |
| -------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Unknown key is silently ignored                                | Error naming the file and line. The two known offenders are listed in an allowlist with their card names, so the corpus still loads and no third one gets in unnoticed |
| `switch` on first character, then string compare               | One map from key to handler. The first-character dispatch is a 2011 micro-optimisation and it is what let `ODeckHints:` slip past                                      |
| `case 'F':` falls through into `case 'H':` — a missing `break` | Not reproduced. It is unobservable, because no key is both `FlavorName` and `HandLifeModifier`, but it is a bug rather than a rule (PORT-7)                            |
| Seven-element array with `null` holes                          | `[7]Face` with an `IsPresent` flag, so an absent face is not a nil dereference waiting to happen (GO-11)                                                               |
| `SVar:` with no value throws `IllegalArgumentException`        | `error`, naming the card. A card script is data (GO-7)                                                                                                                 |
| Reader is a mutable object reused across cards via `reset()`   | A function per script, returning a value. Nothing is reused, so nothing leaks between cards (GO-2)                                                                     |

## Null decisions

| Java                                                              | Go                                                   |
| ----------------------------------------------------------------- | ---------------------------------------------------- |
| `value` is `null` when the line has no colon                      | `(string, bool)`; only `ALTERNATE` produces it       |
| `faces[i]` is `null` until a `Name:` line                         | `Face.IsPresent bool`                                |
| `placeholderFaces` is `null` when the card has no `CopyFaceFrom:` | Empty map, because the caller iterates it either way |

## Open questions

- **Whether to reproduce the ignore-unknown-keys behaviour.** Erroring is the P2 vocabulary gate's whole premise, and
  Forge tolerating a typo is not a licence to. The allowlist keeps the two known defects loading while making a third
  one loud, which is the same shape as `internal/cardtype`'s `UnknownTypes`.
- **What the P1 dumper does with a placeholder face.** The empty diff is defined over `CardRules`, and a `CopyFaceFrom:`
  face is not resolved until every card is read, so the dump has to happen after the full corpus load on both sides.
