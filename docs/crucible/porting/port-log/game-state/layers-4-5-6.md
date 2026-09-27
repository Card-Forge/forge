# Port Log — Game State: Layers 4, 5 and 6 past a literal token list

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine/continuouslayers.go`](../../../../../crucible/internal/engine/continuouslayers.go),
  `applyContinuousType`/`Color`/`Keyword` in [`continuous.go`](../../../../../crucible/internal/engine/continuous.go)
- **Java:** `forge-game/.../staticability/StaticAbility.java` (`checkConditions`, `zonesCheck`),
  `StaticAbilityContinuous.java` (`getAffectedCards`, `applyContinuousAbility`'s TYPE/COLOR/ABILITIES branches,
  `getColorsFromParam`)

Type-, color- and keyword-changing `Mode$ Continuous` lines, from "a fixed token list matched by `Affected$`" to Java's
own gate, affected set and runtime tokens.

## Layers 4, 5 and 6: gate, affected set and runtime tokens

### What Layers 4/5/6 read, past a literal token list

Three mechanisms, ported into `continuouslayers.go`, decide whether and how a real `S:Mode$ Continuous` line applies:

| Mechanism                                                                                                           | Java                                                                       |
| ------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| `AffectedDefined$ Self`/`Enchanted`/`Equipped`/`AttachedBy Self`: upstream's shape for Auras, Equipment, self-buffs | `getAffectedCards`, `StaticAbilityContinuous.java:1048-1050,1066-1067`     |
| `IsPresent$`, `CheckSVar$` chain, `EffectZone$`, `TopCardOfLibraryIs$` (`layerStaticApplies`)                       | `StaticAbility.checkConditions`/`zonesCheck`, `StaticAbility.java:337-512` |
| Runtime tokens (`ChosenType`, `ChosenColor`, `AllColors`, `HostCardUID`, ...), `Remove*Types$`, `RemoveKeyword$`    | `StaticAbilityContinuous.java:167-460, 706-748`                            |

Every host-side value these tokens read already exists: `Memory` (`memory.go`) holds the chosen color, type, number,
player, names and even/odd, and the imprinted list. `*cardtype.Registry` is also already reachable, see below.

### `*cardtype.Registry`: no new injection, the DB already carries it

The registry rides on `*compile.DB` (`DB.Types()`, set by `LoadDB`, `WithTypes` for tests), the one per-process
immutable dependency `NewGame` already takes (GO-2). Animate's `subtypeCategoryDrop` (`animate.go`) and ChooseType read
it there. This change reuses that seam: no `Game` field, nothing new for `Game.Clone` to copy (`db` is shared by pointer
already).

Reason for not adding a separate `Game.types`: the vocabulary is what the corpus was parsed against. A second injection
point lets a game run cards compiled against one `TypeLists.txt` with another's category membership, a mismatch nothing
would detect.

A game whose DB has no vocabulary (`NewDB` without `WithTypes`, most engine tests) cannot evaluate
`ImprintedCreatureType`/`AllBasicLandType`/`AllNonBasicLandType` or a category flag (`RemoveLandTypes$`,
`RemoveCreatureTypes$`, `RemoveArtifactTypes$`, `RemoveEnchantmentTypes$`); such a line does nothing, never half of
itself.

### Model

`continuous.go` keeps the three appliers; each now reads one line through three helpers in `continuouslayers.go`:

| Helper                                          | Java                            | Does                                                                                                                                                                             |
| ----------------------------------------------- | ------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `layerStaticApplies`                            | `StaticAbility.checkConditions` | host zone (`EffectZone$`, effect card = Command only, default battlefield; skipped for CDA), `Condition$`, `IsPresent$`, `TopCardOfLibraryIs$`, `CheckSVar$`..`CheckFourthSVar$` |
| `layerAffectedCards`                            | `getAffectedCards`              | CDA = host (unless in `ExcludeZone$`); else `AffectedDefined$` (phased-in), else `AffectedZone$` zones, else battlefield; then `Affected$`                                       |
| `layerTypeChange`/`ColorChange`/`KeywordChange` | TYPE/COLOR/ABILITIES branches   | the effect record, runtime tokens resolved                                                                                                                                       |

Kept Java quirks (PORT-7), each visible to a card:

- `CheckSecondSVar$` is read only when `CheckSVar$` is present (`StaticAbility.java:470-472`). No real line writes one
  without the other.
- `CharacteristicDefining$` is tested for presence, not value: the one `CharacteristicDefining$ False` line is a CDA.
- An unchosen `ChosenType`/`ChosenColor`-bearing token is dropped, not the whole line (`removeIf`).
- `SetColor$ ChosenColor` with nothing chosen adds no color effect at all, not an overwrite to colorless.
- `Remove*Types$` flags apply only when `AddType$` is absent or kept a token (`StaticAbilityContinuous.java:425-426`).
- `RemoveType$ ChosenType` is never substituted, only dropped when unchosen: Java substitutes `AddType$` alone. No real
  `S:` line writes `RemoveType$ ChosenType` today.

`HostCardUID`/`HostCardControllerUID`/`ChosenPlayerUID` become this port's `CardID`/`PlayerID` numbers. Java writes its
own ids there. Neither engine's `CardUID_`/`PlayerUID_` valid property is read by this port yet, so the text only has to
be stable within a game.

Off-battlefield cards: an `AffectedZone$` line (`Stack` 71, `Graveyard` 27, `Hand` 16, ...) writes `TypeMod`/
`ColorMod`/`KeywordMod` onto cards `Move` never clears, so each applier also clears every off-battlefield card
(`forEachOffBattlefieldCard`) before rebuilding. Phased-out permanents are skipped: their Pump and Animate records must
survive until they phase in.

### Counts

Counting rule: every `S:` line under `forge-gui/res/cardsfolder` with `Mode$ Continuous`; a line resolves for a layer
when every param that layer's applier reads is a shape the port evaluates (whether a type was actually chosen is runtime
state, not shape). A `CheckSVar$`/`PresentCompare$` operand counts as resolvable when it is an integer or an SVar
`resolveAmount` (`amount.go`) computes: `Count$Valid<zones> <spec>` with no operator or distinct-property suffix.

| Lines                                                    | Total | Resolved |
| -------------------------------------------------------- | ----- | -------- |
| `AddType$`/`RemoveType$`                                 | 284   | 254      |
| Type flags without `AddType$`/`RemoveType$`              | 10    | 2        |
| `AddColor$`/`SetColor$`                                  | 61    | 60       |
| `AddKeyword$`                                            | 1,875 | 1,710    |
| `RemoveKeyword$`/`RemoveAllAbilities$`, no `AddKeyword$` | 61    | 56       |

Most of the resolved total names `AffectedDefined$ Self`/`Enchanted`/`Equipped`/`AttachedBy Self` -- upstream's current
shape for Auras, Equipment and self-buffs -- and reaches its target through `layerAffectedCards`. The unresolved
remainder is the "Still not resolved" table below.

### Still not resolved

| Shape                                                                 | Lines (type/color/kw)    | Why                                                                                                                                                                                                                               |
| --------------------------------------------------------------------- | ------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `CheckSVar$`/`PresentCompare$` naming an amount outside `Count$Valid` | 26 / 0 / 90              | `Count$Devotion*` (22), `Count$ThisTurnCast_*` (15), `Imprinted$Valid` (9), `PlayerCountOpponents$*`, `Count$CardCounters`, ...: the general `AbilityUtils.calculateAmount` port, shared by every layer, not a Layer 4-6 question |
| `EffectZone$` without `Battlefield` (Command, Graveyard, Stack)       | 4 / 1 / 40               | `traitHosts` (`game.go`) walks the battlefield and effect cards only; emblems, planes and graveyard/stack-functioning statics need their own host walk                                                                            |
| `AddAllCreatureTypes$`                                                | 8 (type)                 | Java's `CardType.allCreatureTypes` flag; `cardtype.Line` has none, and materialising ~300 subtypes on every `Type()` fold is the alternative                                                                                      |
| `CardManaCost` in a keyword                                           | 18 (kw)                  | needs `ManaCost.getShortString`, not ported; a guessed format would be wrong the day a reader of Escape/Scavenge costs lands                                                                                                      |
| `SharedKeywordsZone$`, `FromDraftNotes$`, `CantHaveKeyword$`          | 8, 1, 5                  | `CardFactoryUtil.sharedKeywords`; draft notes (no draft); a grant blocker later timestamps must respect                                                                                                                           |
| `Condition$ Blessing`/`EnduringStory`/`Monarch`                       | 4 + 3 + 1 (kw)           | no player state for any (`continuousConditionMet`)                                                                                                                                                                                |
| CDA hosts off the battlefield                                         | 9 each, counted resolved | a CDA functions in every zone; only battlefield hosts are walked, so "this spell has flash" and "is every color" in hand do not apply yet                                                                                         |
| "Loses all abilities" beyond keywords                                 | —                        | `RemoveAllAbilities$` removes keywords only; the affected card's own triggers, activated abilities and statics keep working                                                                                                       |

`forge-gui/res/cardsfolder/p/phyrexian_adapter.txt:6` writes `AddType$ Food,Blood,Clue,Treasure,Powerstone`: a comma
list where every other line uses `" & "`. Java's `CardType.add` stores it as one subtype named with the commas, and so
does this port (PORT-8, reported, not compensated).
