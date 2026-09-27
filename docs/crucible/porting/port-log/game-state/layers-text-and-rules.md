# Port Log — Game State: Layer 3 text change and Layer 8's remainder

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine/continuous.go`](../../../../../crucible/internal/engine/continuous.go),
  [`internal/engine/card.go`](../../../../../crucible/internal/engine/card.go)
- **Java:** `StaticAbilityContinuous.java:577-640` (TEXT layer), `Card.java:128-279` (the `...ByText` tables),
  `AbilityUtils.java:116-124` (`TopOfGraveyard`)

CR 613.1c's text-changing layer for its one real `GainTextOf$` line, and the rest of this port's Layer 8 bucket
(`StaticAbilityLayer.RULES`: the static params no numbered CR 613 layer holds).

## Layer 3: `GainTextOf$` lands

### Corpus

| Layer 3 param (`StaticAbility.java:143`) | Real `S:Mode$ Continuous` lines | Status                                                                                  |
| ---------------------------------------- | ------------------------------- | --------------------------------------------------------------------------------------- |
| `GainTextOf$`                            | 1 (Volrath's Shapeshifter)      | Resolved here                                                                           |
| `AddNames$`                              | 1 (Spy Kit)                     | Resolved earlier, `applyContinuousNames`                                                |
| `SetName$`                               | 5                               | Not resolved: every line pairs it with `RemoveAllAbilities$`/`RemoveCreatureTypes$`/... |
| `ChangeColorWordsTo$`                    | 1 (Swirl the Mists)             | Not resolved: CR 612 word substitution, the `ChangeText` API's own mechanism            |
| `Incorporate$`, `ManaCost$`              | 0 (Animate-only, `Perpetual`)   | Not a static shape                                                                      |

The one line:

```text
S:Mode$ Continuous | AffectedDefined$ Self | EffectZone$ Battlefield | GainTextOf$ TopOfGraveyard.Creature | GainTextAbilities$ VolrathDiscard
```

### What "gains the text of" means in Java

`StaticAbilityContinuous.java:580-632` reads the source card's current state (`first.getCurrentStateName()`; a flipped
state only when both cards are flip cards, unreachable here) and writes seven Layer 3 tables at once: name, mana cost,
color, type, traits (spells, triggers, replacements, statics), keywords, base power/toughness. Traits go in with
`CardTraitChanges(..., e -> true)`: every trait of the permanent's own is removed. `GainTextAbilities$` SVars are
appended after the gained traits. Values are the source's printed (current-state) ones, not its layered ones: a card in
a graveyard has no continuous effects on it anyway.

### Model: Layer 3 swaps `Def`, like Layer 1

Every reader already goes through `Card.Def` and every later layer folds over it (the Clone port's "Layer 1 is the
definition every other layer folds over"). One composite `compile.Card` expresses all seven Java tables together, so
Layer 3 swaps `Def` too, one step above Layer 1:

| Piece                                                     | What it holds                                                          | Why                                                                                                                           |
| --------------------------------------------------------- | ---------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| `Card.text.base`                                          | `Def` under the change (Layer 1's result); nil when none applies       | What `Def` returns to each pass; `preTextDef()`                                                                               |
| `Card.text.def` + key (`from`, `owner`, `face`, `static`) | Last composite; source `Def`; static by position in the host's `Def`   | Cache: unchanged top reuses the composite. Position, not `*compile.Ability`: enginelint's `card` group may not name `Ability` |
| `textChangedDef`                                          | Source's face 0, ability slice copied then `GainTextAbilities$` added  | Faces 1+ blank, `SplitType` zero: a DFC on top must not make the permanent transform                                          |
| `compile`: `GainTextAbilities$` on a static               | Compiled `Subs` (split on `&`)                                         | PORT-2; golden AST changed for `volraths_shapeshifter` only                                                                   |
| `clearContinuousText` (before Layer 2)                    | Ends every change, whole arena                                         | Java clears every static effect before collecting statics; phased-out cards included                                          |
| `applyContinuousText` (after Layer 2, before 4)           | Applies each `GainTextOf$` static                                      | Controller decides whose graveyard; Layers 4-7 then see the gained statics unchanged                                          |
| `topOfGraveyard`                                          | Last card of the host controller's graveyard, `.Valid` filter optional | `AbilityUtils.getDefinedCards` "TopOfGraveyard" plus its `incR[1]` restriction                                                |

The composite's ability slice is copied before appending: source `Def`s are shared across every game in the process
(ADR-0007), so appending into spare capacity would be a cross-game data race.

A gained static applies in every layer after Layer 3 because the later appliers walk `h.Def.Faces[*].Statics`, now the
source's. That matches Java's `toAdd` list in `GameAction.checkStaticAbilities` (a text-gained static joins every layer
after the one that gained it). Gained triggers, replacements and activated abilities work the same way: every scan reads
`Def`. The appended ability's index is stable while the source stays on top, so `ActivateAbility(..., index)` reaches
it.

### Where the pre-text `Def` is read

CR 707.2 leaves text-changing effects out of copiable values, and a text change ends with its object (CR 400.7):

| Site                                     | Change                    | Reason                                                                              |
| ---------------------------------------- | ------------------------- | ----------------------------------------------------------------------------------- |
| `copiableValues`, `UncopiedDef`          | Read `preTextDef()`       | A Clone of the Shapeshifter copies the printed card; fixture name                   |
| `addCopy`, `manifest`                    | `clearTextChange()` first | Both capture `Def` to restore later; must capture the real one                      |
| `Move` / `MoveToLibraryTop` leaving play | `clearTextChange()`       | After the LKI snapshot, so a gained dies trigger still sees text                    |
| Any other `Def` writer between passes    | None                      | `clearTextChange` restores only if `Def` is still the composite: a newer write wins |

### Tests

`gaintext_test.go` drives the real card: full text taken (name, cost, color, type, keywords, P/T, only the gained
ability), non-creature top and empty graveyard, activating the gained `{2}: Discard` by index and reverting, leaving the
battlefield, a Clone of it, a gained lord static, a gained `*/*` CDA, `Game.Clone`. Scenarios
`layer3-gain-text-of-zero-toughness-creature-dies` (Swampless Nightmare text: 0/0, CR 704.5f) and
`layer3-gain-text-of-needs-creature-on-top`.

### Not resolved

- A second static reaching the same card from the gained text in the same pass (the top card itself a Shapeshifter):
  Java re-applies it via `toAdd`; the result is identical (later text replaces earlier wholesale), so nothing is lost.
- Any `GainTextOf$` shape other than `AffectedDefined$ Self` + `TopOfGraveyard[.Valid]`: none exists.

## Layer 8: `AddHiddenKeyword$` lands

Java: `StaticAbilityContinuous.java:322-323` (RULES layer) and `:751-752` → `Card.addHiddenExtrinsicKeywords`
(`Card.java:5225`). `Card.hasKeyword(String)` checks the hidden table first (`Card.java:4981`), `hasStartOfKeyword` too
(`:5257`). "Hidden" means: a whole keyword line that exact-text and prefix reads see, but that is not in the card's
keyword list, so nothing that removes, lists or counts keywords (`RemoveAllAbilities$`, `KeywordLines`) touches it. That
is the one difference from `AddKeyword$` (Layer 6, `KeywordMod`).

| Piece                                             | What it holds / does                                                   | Why                                                                          |
| ------------------------------------------------- | ---------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| `Card.hiddenKeywords []string`                    | This pass's grants                                                     | Java's `hiddenExtrinsicKeywords` table; `Game.Clone` deep-copies it          |
| `clearHiddenKeywords` (in `applyContinuousRules`) | Resets it on every card in the arena                                   | A card that left play or phased out keeps no grant nothing re-derives        |
| `applyOneContinuousHiddenKeyword`                 | `AffectedDefined$` Self/Enchanted/Equipped, else `Affected$` over play | The two real shapes: Aura/Equipment/self statics, Effect-card blanket lines  |
| `hasKeywordText` / `hasKeywordTextPrefix`         | Read hidden lines before printed/Layer 6 ones                          | Block legality and block requirements already read these two                 |
| `canAttackAtAll`                                  | Refuses "CARDNAME can't attack." / "CARDNAME can't attack or block."   | `StaticAbilityCantAttackBlock.java:41`; the grant would otherwise half-apply |

Only lines something reads are granted; a line naming any other keyword is skipped whole (GO-7):

| Keyword line (corpus: `S:` + Effect SVar)                 | Lines  | Status                                                            |
| --------------------------------------------------------- | ------ | ----------------------------------------------------------------- |
| `CARDNAME can't block.`                                   | 1 + 26 | Resolved                                                          |
| `All creatures able to block CARDNAME do so.`             | 8 + 0  | Resolved                                                          |
| `CARDNAME must be blocked if able.`                       | 4 + 0  | Resolved                                                          |
| `CARDNAME can't attack or block.`                         | 0 + 2  | Resolved (block and attack halves)                                |
| `This card doesn't untap during your next untap step.`    | 1 + 6  | Skipped: no untap-step hook reads it                              |
| `CARDNAME can't attack alone.` / `can only attack alone.` | 3 + 0  | Skipped: `attackconstraints.go` reads neither                     |
| `CARDNAME count as <name>.`                               | 2 + 0  | Skipped: graveyard-only name aliasing (`AffectedZone$ Graveyard`) |

41 of 53 real lines resolve (13 of 19 `S:` lines, 28 of 34 Effect-SVar lines), each still subject to its own `Affected$`
filter being a property `Matches` evaluates. A `Pump`/`Animate` `KW$ HIDDEN ...` token is a separate one-shot grant, not
this static, and stays rejected (`pumpeffect.go`).

No scenario fixture: every behavior here is a block/attack declaration being refused, and the harness has no verb that
expects an illegal declaration; `hiddenkeyword_test.go` covers it the way `combatlegality_test.go` covers printed lure
and can't-block keywords.

## Layer 8: vote and villainous-choice params land

Java: `StaticAbilityContinuous.java:536-553` writes four per-player tables (`Player.addControlVote`,
`addAdditionalVote`, `addAdditionalOptionalVote`, `addAdditionalVillainousChoices`); `VoteEffect.java:83-104` and
`VillainousChoiceEffect.java:22-28` read them. They are player-facing, so they ride `RulesMod` like `SetMaxHandSize$`:
four new `RulesEffect` fields filled by `rulesEffect`, folded by `RulesMod` methods.

| Param (real lines)                | Card                      | Fold                              | Reader                                                           |
| --------------------------------- | ------------------------- | --------------------------------- | ---------------------------------------------------------------- |
| `AdditionalVote$` (1)             | Brago's Representative    | Sum                               | Vote: each player votes `1 + sum` times                          |
| `AdditionalOptionalVote$` (3)     | Ballot Broker, Tivit, ... | Sum                               | Vote: `ChooseNumber(0..sum)` more votes, asked only if sum > 0   |
| `ControlVote$` (1, Effect SVar)   | Illusion of Choice        | Latest `Timestamp` across players | Vote: that player casts every ballot, counted as the voter's     |
| `AdditionalVillainousChoice$` (1) | The Valeyard              | Sum                               | VillainousChoice: the whole choice `1 + sum` times, then resolve |

`ChooseNumber` is asked only when an optional vote exists: Java always calls `chooseNumber(0, optionalVotes)`, but a
`[0, 0]` choice decides nothing and would make every scripted vote queue an extra answer (the "nothing meaningful to
decide" reasoning `DeclareCombatAttackers` already uses).

Replaces the old fail-closed checks (`battlefieldStaticNames`), which also missed `ControlVote$`: its one real line sits
on an Effect card in the Command zone, not the battlefield, so a controlled vote silently ran uncontrolled.

**Forge bug (PORT-8), not reproduced:** `VoteEffect.java:93-94` removes `realVoter` (the `ControlVote$` player) from a
`VotePlayer$ Other` ballot, not the player whose vote it is: under a controlled vote each player may vote for
themselves, and nobody may vote for the controller. That combination returns an error citing the line instead.

Not resolved (skipped, `applyOneContinuousRules`' doc comment): `ControlOpponentsSearchingLibrary$` (1 real line) — no
search effect hands its decisions to another controller; `DeclaresAttackers$`/`DeclaresBlockers$` (1 `S:` line, 5 Effect
SVars) — `DeclareCombatAttackers`/`DeclareCombatBlockers` ask the attacking/defending player only; `IgnoreEffectCost$`
(4) — a cost-paid exemption from another static, its own mechanic.

Tests: `extravotes_test.go` (each param from its real line; `ControlVote$` through a controller that records who is
asked; the `VotePlayer$ Other` refusal).
