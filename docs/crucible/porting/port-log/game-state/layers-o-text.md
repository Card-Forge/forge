# Port Log — Game State: Layers batch O, Layer 3 word substitution and granted-body rewrites

- **Parent:** [`game-state.md`](../game-state.md)
- **Decision:** [ADR-0039](../../../adr/0039-layer3-word-substitution.md)
- **Go:** `textrewrite.go` (pure rewrite), `textwords.go` (records, stage, grant substitution), `changetexteffect.go`,
  `exchangetextboxeffect.go`, `continuouslayers.go` (`AddAllCreatureTypes$`, `CardManaCost` keyword),
  `mana.Cost.ShortString`
- **Java:** `ChangeTextEffect.java:25-130`, `TextBoxExchangeEffect.java:39-173`, `CardChangedWords.java:79-104`,
  `AbilityUtils.java:3013-3099`, `CardTraitBase.java:691-712`, `CardUtil.java:53-76`, `Card.java:4888-4897,5404-5500`,
  `StaticAbilityContinuous.java:660-675,735-741,777-784,842-847`, `ManaCost.java:294`

## `ChangeText`, `ExchangeTextBox`, `ChangeColorWordsTo$`

| Piece | Where |
| --- | --- |
| Word records | `Game.textWords` (one per card, kind, pair, timestamp, permanent); `Game.textBoxes` (one per exchanged card) |
| Fold | `foldWords`: rows in timestamp order, `a->b` then `b->c` chains to `a->c`, a clear row empties the table |
| Rewrite | `rewriteDef`: params, sub-abilities, modifiable keyword lines, type line; memoised per game in `Game.textMemo` |
| Stage | `applyTextWords`, last step of `applyContinuousText`; swaps `Def` through `setTextChange`, so Layers 4-8 fold on it |
| Spells on the stack | `Registry.resolve` calls `textChangedSpell`: the ability built at cast is rewritten with the card's folded map |
| End | cleanup drops non-`Permanent`; `Move` drops the card's records (CR 400.7); host-bound exchanges end with the host |
| Choices | `ChooseColors` (new color excludes the original), `ChooseOption` over the DB's basic / creature type lists |

Regex semantics kept (PORT-7): left `\b` only, `non` prefix kept, nothing replaced within 100 characters after `named`,
color words lower case then as written, `Any` skipping its destination, type words case sensitive, a value equal to a
sub-ability or SVar name untouched, `LockInText$` locks a trait. Any `Duration$` other than `Permanent` ends at cleanup
(`ChangeTextEffect.java:29`). A type-word change really changes the subtype (`WordChangedType`); a color word never
changes the real color. An exchange clears every earlier word row of both cards (`Card.java:4895-4896`).

Not resolved:

- `ExchangeTextBox` `Duration$` other than none, `AsLongAsInPlay`, `UntilHostLeavesPlay` is an error.
- Word changes are keyed per card, not per exchanged text box: a change made after an exchange rewrites the exchanged
  text, as Java.
- A spell's change reaches only `Params` (not its costs or target restrictions beyond what a param carries).

**Forge bugs (PORT-8), not worked around:** `TextBoxExchangeEffect.java:74-76` removes the exchanged traits at the end
but leaves the two `addEmpty` clear rows `Card.java:4895-4896` wrote, so word changes made before an `AsLongAsInPlay`
exchange stay wiped after it ends (the source comment is a question). This port's ending exchange drops its own rows, so
earlier word changes return: that differs from the Java oracle on this one sequence and is the item to review with the
upstream fix. The exchanged text is the other card's current, already rewritten definition, which is Java's
`changeTextIntrinsic` baking. `AbilityUtils.java:3090` builds its regex from the unescaped word with no right boundary,
so `Red` also rewrites the front of `Reduce`. Reproduced (PORT-7), as parity depends on it.

## Granted-static gaps

| Gap | State |
| --- | --- |
| `CardManaCost` / `ConvertedManaCost` in a granted body | Lands: `costSubstitutedGrant`, per receiving card; abilities take either token, statics only the second |
| Layer 3 text change on a granted body | Lands for free: a grant's SVars are read through the host's rewritten `Def`; the grant row itself is not rewritten |
| A granted static's Layer 4/5 effects | Not done |
| `AddSVar$` | Lands for free: the granted body reads the granting face's amounts and sub-abilities |

Java applies a static granted in Layer 6 to Layers 4 and 5 from the next pass. This port rebuilds grants after Layers
4-5, so the effect would need a second settle step; no real shape depends on it yet beyond a handful of Auras that also
grant a type. `AddSVar$` (about 40 real lines) writes `Card.changedSVars` in Java; here SVars are compiled amounts and
sub-abilities of the granting face, which a grant already carries, so no per-card SVar table exists
(`layersaddsvar_test.go`).

## Layers 4, 5 and 6

| Row | State |
| --- | --- |
| `AddAllCreatureTypes$` (8) | Lands: `layerTypeChange` adds every creature type of the DB vocabulary via `Game.allCreatureTypes`, shared with Changeling |
| `CardManaCost` in a keyword (2 real lines) | Lands: `mana.Cost.ShortString` (`ManaCost.getShortString`), `layerKeywordsFor`; `CardManaCost` wins over `ConvertedManaCost` |
| `SharedKeywordsZone$`, `CantHaveKeyword$` | Lands: `layerSharedKeywords` (`CardFactoryUtil.getSharedKeywords`), `KeywordEffect.CantHave` stripped in `KeywordMod.fold` |
| "Loses all abilities" past keywords | Already resolved before this batch (`abilityRemoval`, `printedTraitsRemoved`, `removeallabilities_test.go`); the row is stale |
| `CheckSVar$` amount outside `Count$Valid` | Not done: the general `calculateAmount` port |

Tests: `textwords_test.go`, `textboxwords_test.go`, `textrewrite_test.go`, `layerscostgrants_test.go`,
`internal/mana/shortstring_test.go`. No scenario fixture: the state dump holds zones, life and counters only, never
text, types or keywords, so a fixture cannot observe these outcomes.
