# Port Log — Game State: M6 Effects: Clone

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine/cloneeffect.go`](../../../../../crucible/internal/engine/cloneeffect.go)

Layer 1's first copy effect.

## Clone lands, Layer 1's first copy effect

`cloneEffect` (`CloneEffect.java`, with `CardFactory.getCloneStates` at `CardFactory.java:461-750`) makes a permanent a
copy of another card: CR 707.2, a Layer 1 copy effect (CR 613.2a). It resolves the "becomes a copy" shape (activated,
triggered and spell lines). The largest single shape, "enters as a copy", is a Copy-layer replacement that runs this
same effect: [`layer1-enters-as-copy.md`](layer1-enters-as-copy.md).

### Model: Layer 1 is the definition every other layer folds over

Java stores copy effects as `Card.clonedStates`, a `NavigableMap<timestamp, CardCloneStates>` (`Card.java:166`).
`getState` returns the latest entry's `CardState` in place of the card's own (`Card.java:517-526`). Every other
continuous effect lives on the `Card` (`changedCardTypes`, `changedCardKeywords`, `newPT`, ...) and applies over
whichever state is current. So Java's Layer 1 is "swap the base state", with Layers 2-7 layered on top.

This port already reads every characteristic through `Card.Def` (`Type`, `Colors`, `KeywordLines`, `BasePower`, the
trigger/ability/static/replacement scans, `Def.Faces[0].Amounts`), and `TypeMod`/`ColorMod`/`KeywordMod`/`PT`/
`ControlMod` fold over it. Face-down cards (`faceUpDef`) and transformed ones (`frontDef`) already swap `Def`. Layer 1
follows the same pattern, so no new layer system was needed:

| Piece                             | What it holds                                                               | Why                                                                                                                         |
| --------------------------------- | --------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `Card.copies []copyEffect`        | Copy effects in ascending timestamp; the last one's `def` is `Def`          | Java's `clonedStates`: a permanent and an until-end-of-turn copy coexist, the later wins, ending it reveals the earlier     |
| `Card.uncopiedDef`                | `Def` as it was before the first copy                                       | What `Def` returns to when the last copy ends. Doubles as `UncopiedDef()`, the name `GameState` writes                      |
| `copyEffect.def`                  | A per-game `*compile.Card` built once at resolution, never written again    | PORT-2: no script text is re-read; `faceDownDef` and the transformed back face already build a `*compile.Card` the same way |
| `copyEffect.life`                 | `effectLifetime` (duration, activator, host), reused from `effecteffect.go` | Same until-command vocabulary effect cards already expire by                                                                |
| `copyEffect.remembered/imprinted` | The copy's `Memory` as a `Duration$` copy began                             | CloneEffect's `unclone` command restores it (Death-Mask Duplicant)                                                          |

Layering is correct by construction: a +2/+2 pump, a Layer 4 type grant and +1/+1 counters applied before the copy still
apply over the copied values (`TestCloneLaterLayersApplyOverCopiedValues`), because they were never part of `Def`.

The built definition must never write into the shared one. `Face` is a value, but its `Keywords`, `Statics` and
`Amounts` alias the process-wide `*compile.Card`. `cloneChanges.apply` replaces each slice and map it edits, never
appends in place; `TestCloneExceptParamsChangeTheCopiedValues` and `TestCloneSetColorAndKeepName` check the copied
card's own definition is untouched. The `copies` slice is rebuilt on every change too, since a last-known-information
snapshot (`snap := *c` in `Game.Move`) shares its backing array.

### What a copy takes

`copiableValues` is CR 707.2 plus CR 707.3: the card's current `Def`, a copy's own when one applies, so a copy of a copy
copies what it copied. A face-down permanent's `Def` is already `faceDownDef`, which is `getCloneStates`' face-down
branch. A token's base power/toughness from its creating effect (`TokenPower$`) is copiable and baked into the copy's
`Power`/`Toughness`. `BasePower`/`BaseToughness` ignore a card's own override while a copy applies. `CopyPermanent`'s
`fromCard` now reads `copiableValues` too, since before this port nothing could be copied twice.

Which faces: `getCloneStates` copies all states of a flip, split, adventure/omen (`Secondary`) or prepare card, and only
the current state otherwise (`CardFactory.java:513-547`). For `Clone` (not `CopyPermanent`) that includes transforming,
modal and meld cards. `cloneDef` keeps every face for `SplitFlip`/`SplitSplit`/`SplitAdventure`/`SplitOmen`/
`SplitPrepare`. For everything else it keeps `Faces[0]` alone, the current face in this port's convention, and clears
`SplitType`.

### Resolution, `CloneEffect.resolve`

| Step          | Params                                                                                                                                                                                                                   |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Duration      | Absent: permanent. `UntilEndOfTurn` ends at cleanup, `UntilYourNextTurn` as the activator's turn begins, `UntilHostLeavesPlay` as the host leaves (no-op if the host is off the battlefield and stack)                   |
| Card to copy  | `Choices$` (activator picks, in `ChoiceZone$`, `ChoiceOptional$`), else first `Defined$`, else first targeted card, else `CopyFromChosenName$` (DB lookup of the host's named card)                                      |
| Confirm       | `Optional$` asks the host's controller (`host.getController()`, not the activator)                                                                                                                                       |
| Becoming copy | `CloneTarget$` (`definedCards` plus `Valid <spec>`), else the target when `Choices$` chose, else the host; `ExcludeChosen$`, `CloneZone$`                                                                                |
| Except        | `NewName$`, `KeepName$`, `AddColors$`, `SetColor$` (drops devoid and a CDA color), `NonLegendary$`, `AddTypes$`, `AddKeywords$` (`IfNew`), `SetPower$`/`SetToughness$` (drop a CDA P/T), `AddSVars$`, `GainThisAbility$` |
| After         | `IntoPlayTapped$` taps; host's own memory cleared unless `ImprintRememberedNoCleanup$`; `Duration$` snapshot; target's memory cleared; `RememberCloneOrigin$`                                                            |

`GainThisAbility$` (24 lines) keeps "this ability" on the copy: `cloneRoot` finds the trigger, activated ability or
replacement on the host whose compiled tree holds the resolving line, by pointer (`SpellAbility.getRootAbility`), and
appends that same `*compile.Ability` to each copied face. A delayed or immediate trigger's `Execute$` compiles inside
the ability that spawns it, so the search lands on the spawning root, as `getSpawningAbility` does (Aurora Shifter,
which also names `AddTriggers$`) (`CardFactory.java:667-682`). The host's current definition is searched first, then its
own and each copy's, so a copy that gained the ability finds it again. No new `Ability` field and no trigger-site change
were needed. A line whose root is not among the host's abilities (granted by another card) is an `error`.

`AddSVars$` carries only numeric SVars (`Face.Amounts`). An ability SVar it names in Java is text the gained ability
looks up at run time. Here that ability is already compiled with its sub-abilities embedded (PORT-2), so there is
nothing to carry.

Every target is checked and every definition built before any card changes, so a rejected shape fails the whole line.

A copy ends with its permanent's zone change: `Game.Move`/`MoveToLibraryTop` call `endCopiesOnLeave` before
`turnFaceUp`/`turnFrontFaceUp`, so a copied, transformed permanent lands showing its own front face. LKI keeps the copy
it died as. `endCopiesAtCleanup` (`cleanupStep`) and `endCopiesAtTurnStart` (turn start) expire the rest; `Game.Clone`
copies `copies`.

The fixture dumper and the scenario comparator name a card by `UncopiedDef().Name`, because `GameState.java` writes
`getPaperCard().getName()`, never a copy's name. `actions.log` gains `queue targets <id>[,...]`, since a triggered
ability's targets had no verb. Scenario `copy-effect-attacker-fights-as-copied-creature` runs Tilonalli's Skinshifter
through combat as a 3/3 copy of Hill Giant.

### Rejected

| Param or shape                                                                                                               | Why                                                                                                                       |
| ---------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| `GainTextAbilities$`, `GainTextOf$`                                                                                          | No `Clone` line writes them (only Volrath's Shapeshifter's static does); refused rather than guessed                      |
| `PumpKeywords$`, `PumpDuration$`                                                                                             | Layer 6 grant with its own until-command (`TokenEffectBase.addPumpUntil`)                                                 |
| `Embalm$`, `RemoveCost$`, `SetManaCost$`, `SetColorByManaCost$`                                                              | Embalmed state and mana-cost rewriting; `RemoveCost$` would also need the color frozen, since color derives from the cost |
| `RemoveCardTypes$`, `RemoveSubTypes$`, `SetCreatureTypes$`, `RemoveKeywords$`, `SetLoyalty$`                                 | Type-line rewriting (`CardType.sanisfySubtypes`), 3 lines                                                                 |
| `RemoveCreatureTypes$`                                                                                                       | `getCloneStates` never reads it (PORT-8, below)                                                                           |
| `Duration$` `UntilUnattached`, `UntilFacedown`, `UntilTargetedUntaps`, `UntilNextEndStep`                                    | Endings this port does not track                                                                                          |
| `Choices$`/`Valid` properties `token`, `NotDefinedTargeted`, `ExiledWithSource`, `ThisTurnEntered*`, non-literal comparisons | `Matches` has no case and reads them false silently (GO-7)                                                                |
| A copy target outside the battlefield, or face down                                                                          | Java's clone state there is hidden or dies with the next zone change; neither is modeled                                  |

### Divergences

| Where                           | Java                                                                | Here                                               |
| ------------------------------- | ------------------------------------------------------------------- | -------------------------------------------------- |
| Transforming a copied permanent | Flips `backside`; setState fails; the flip shows once the copy ends | `transform` refuses; no flip (`setstateeffect.go`) |
| Phased-out copy target          | Skipped                                                             | Phasing is not modeled                             |
| `GameEventCardStatsChanged`     | Fired per copy and unclone                                          | No event kind                                      |
| `setMarkedColors` (Spire)       | Copied                                                              | Marked colors are not modeled                      |

### Shapes not resolved

58 of the corpus's 180 `(AB|SP|DB)$ Clone` lines resolve as a spell, activated or triggered ability, and 50 more as a
Copy-layer replacement ([`layer1-enters-as-copy.md`](layer1-enters-as-copy.md); one more, Lazotep Convert's, is
unreachable). Blocked, by first blocker:

| Shape                                                                    | Lines | Blocker                                                                                                                                                                                                                                 |
| ------------------------------------------------------------------------ | ----- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Run as a Copy-layer replacement (`K:ETBReplacement:Copy`, `Layer$ Copy`) | 18    | [`layer1-enters-as-copy.md`](layer1-enters-as-copy.md#what-resolves)                                                                                                                                                                    |
| `R:` replacement with `ReplaceWith$` Clone, `Event$ Transform` (Ludevic) | 1     | `replacement.go` dispatches no `Transform` replacement                                                                                                                                                                                  |
| `Defined$`/`CloneTarget$` outside `definedCards`                         | 27    | `ParentTarget` (a sub-ability's own `ValidTgts$` is not targeted separately, `subability.go`), `TriggeredCardLKICopy`, `TriggeredTarget*`, `RememberedLKI`, `Sacrificed`, `TopOfLibrary`, `ExiledWith`, `Exiled`, `Remembered.Creature` |
| Unported valid properties                                                | 10    | `NotDefinedTargeted`, `token`, `ExiledWithSource`, `ThisTurnEntered*`, `cmcLEY`                                                                                                                                                         |
| Rejected params                                                          | 2     | `RemoveCardTypes$` (Taskmaster), `PumpKeywords$` (Loose in the Park)                                                                                                                                                                    |
| Unported durations                                                       | 3     | `UntilUnattached` 2, `UntilTargetedUntaps`                                                                                                                                                                                              |

`Defined$ Remembered` after `RememberLKI$` (Absorb Identity) copies the remembered card's current values, not its LKI
snapshot: `Memory` holds a `CardID`, and a card's copiable values off the battlefield are its printed ones, so this
differs only when the remembered permanent was itself a copy.

### Forge defects found (PORT-8)

| Site                                                                          | Defect                                                                                                                                                                                       | Crucible meanwhile                                  |
| ----------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------- |
| `forge-gui/res/cardsfolder/l/ludevic_necrogenius_olag_ludevics_hubris.txt:23` | `AddColors$ Blue & Black`; `CardFactory.java:497` splits on `,`, so `MagicColor.fromName("blue & black")` is 0: Olag gains no color                                                          | `error`: `AddColors$ "Blue & Black" names no color` |
| `forge-gui/res/cardsfolder/t/taskmaster_mercenary_mimic.txt:6`                | `RemoveCreatureTypes$ True` is never read by `getCloneStates` (`CardFactory.java:579-581` reads `RemoveCardTypes$`/`RemoveSubTypes$`): the copy keeps its creature types, against its Oracle | `RemoveCreatureTypes$` rejected                     |

Both are card-script defects, logged in [card-script-defects.md](../../card-script-defects.md).
