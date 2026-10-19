# ADR-0039 — Layer 3 Word Substitution: Rewrite the Compiled Definition, Never the Script

- **Status:** Accepted
- **Date:** 2026-10-07
- **Deciders:** Crucible session (M5/M6 layers batch O)

## Context

CR 612 text-changing effects replace a color word or a basic land / creature type word in a permanent's or spell's text.
The corpus has 17 `ChangeText` lines (Mind Bend, Magical Hack, Alter Reality, Artificial Evolution, New Blood, ...), 2
`ExchangeTextBox` (Exchange of Words, Deadpool, Trading Card) and 1 static `ChangeColorWordsTo$` (Swirl the Mists).
ADR-0023 point 5 deferred them to "their own decision".

Java (`Card.addChangedTextColorWord/TypeWord`, `CardChangedWords`, `AbilityUtils.getReplacedText`):

- Two timestamp-keyed word tables per card; `CardChangedWords.refreshCache` folds them into one flat map in timestamp
  order, chaining `a->b` then `b->c` into `a->c`; an empty row clears everything older.
- Every read re-runs a regex over the intrinsic trait's param strings and SVar bodies (`CardTraitBase.changeText`,
  `AbilityUtils.getSVar`): `\b(non)?<orig>` with no trailing `\b`, color words in both lower and capitalized form, type
  words as written.
- A type-word change also rewrites the real subtype (`WordChangedType.applyChanges`); a color-word change leaves the
  real color alone.
- `ExchangeTextBox` copies one card's intrinsic non-keyword traits and intrinsic keywords onto another at a timestamp.

Crucible compiles script text once (PORT-2). Params are still `Key$ Value` text on `compile.Ability`, typed on demand
(`compile.ParseParams`), so a rewrite of values reaches every typed reader. Layer 1 copies and `GainTextOf$` already
swap `Card.Def` for a per-game composite that every later layer folds over (`layers-text-and-rules.md` (git
`624d87dba16`)).

## Decision Drivers

- PORT-2: no script parsing during a game.
- Reuse the `Def` swap: all ~80 trait scans already read `Def` / `traitFaces`.
- GO-2/3: no package state, no mutex; `Game.Clone` must not share mutable memo tables.
- CR 400.7: a text change ends with the object.

## Considered Options

1. **Overlay with rewrite at read, as Java.** Rejected: every param read on a text-changed card would run the rewrite;
   the 80 scan sites would each need to ask the overlay (the cost ADR-0023 accepted for grants, avoided here because
   `Def` already carries it).
2. **Rewrite the compiled `Def` once per (base definition, word map), swap it in as Layer 3.** Chosen.
3. **Re-parse a rewritten script text.** Rejected: PORT-2, and `compile` has no entry point for one card at run time.

## Decision

Option 2. A `Game` holds the word changes as records (`ChangeText` resolutions: card, timestamp, color or type pair,
permanent or until cleanup; `ExchangeTextBox` resolutions: both cards, a trait snapshot, timestamp) and
`ChangeColorWordsTo$` statics re-derive theirs each pass. `applyContinuousText` folds a card's rows in timestamp order
into one map (`CardChangedWords` semantics), then `rewriteDef` builds a copy of the card's `Def` with every ability,
trigger, static and replacement param value, every `K:` keyword line a keyword is allowed to change, and each type
line's subtype rewritten, and swaps it in like `GainTextOf$`. The result is memoised per game on (base `Def`, folded
map); `Game.Clone` starts with an empty memo.

Rewrite rules, kept from Java: left `\b` only; optional `non` prefix kept; no match within 100 characters after `named`;
color words replaced lower and capitalized, the `Any` source skipping the destination color; type words replaced as
written; a value equal to an SVar or sub-ability name, and `TokenScript`, `NewName`, `DefinedName`, `ChooseFromList`,
`AddAbility`, descriptions: untouched. `Face.Amounts` (compiled SVar amounts) is rewritten the way
`AbilityUtils.calculateAmount` does at read time (`AbilityUtils.java:440`): the text after the first `$` goes through
the word map and the amount is parsed again (`rewriteAmounts`); a `Number$` amount is read unrewritten (`:447-448`).
Table keys stay as written, since an inline amount is keyed by its own text.

A spell on the stack resolves from its `Ability`, built at cast time; `Registry.Resolve` rewrites a spell's `Params`
when its card carries a map (Java does the same at `MagicStack.java:580`).

## Consequences

**Good:** every `ChangeText` card, both `ExchangeTextBox` cards and Swirl the Mists resolve through one stage; trait
scans need no change; an unchanged card pays nothing.

**Bad:** a text-changed card allocates one `compile.Card` copy per distinct map and game; the SVar-amount gap above is
silent until a card hits it. When an exchange ends, older word changes on both cards return, while Java leaves them
cleared (a Forge bug, reported in the port log); Crucible disagrees with the oracle on that one sequence.

**Neutral:** `ChangeText` ends at cleanup unless `Duration$ Permanent`, as Java: any other `Duration$` value also means
cleanup.

## Related

ADR-0007, ADR-0023, ADR-0025, `layers-o-text.md` (git `624d87dba16`)
