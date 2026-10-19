# Port Log — Game State: M5 batch M, layers and misc kernel

- **Parent:** [`game-state.md`](../game-state.md)
- **Go:** `continuous.go` (`applyOneContinuousTraits`, `applyContinuousNames`, `continuousConditionMet`), `card.go`
  (`traitGrant`, `grantFaces`, `Name`), `entersascopy.go`, `action.go` (`resolveLegendRule`), `event.go`
  (`counterDetailFor`)
- **Java:** `StaticAbilityContinuous.java:326-363,771-858` (trait grants), `:647-655` (`SetName$`),
  `GameAction.java:1120-1172` (`checkStaticAbilities`), `:2006-2066` (`handleLegendRule`), `CardDb.java:1079`
  (`isNonLegendaryCreatureName`)

## `AddStaticAbility$` and `AddReplacementEffect$` grants

Real lines: 54 `AddStaticAbility$` (Rune of Flight/Speed/Might/..., Walking Sarcophagus, Gastal Raider, Kabira
Vindicator, Tsagan Raider-Warlord), 9 `AddReplacementEffect$` (Bewitching Leechcraft, Hedron-Field Purists, Pulmonic
Sliver, Master Chef, Cloudsteel Kirin, ...).

| Piece | Where |
| --- | --- |
| The SVars were already compiled (`continuousGrantKeys`); the grant row gains `statics`, `replacements` | `traitGrant`, `applyOneContinuousTraits` |
| Every walk over a host's statics or replacements reads `traitFaces`; it appends one pseudo-face per grant | `Card.grantFaces` (one seam, ~20 walks) |
| `RemoveAllAbilities$` takes granted statics and replacements granted earlier, `RemoveNonManaAbilities$` too | `removeTraits` |
| A granted static applies in the layer that granted it (and so does what it grants), then in every later one | `abilitiesLayerOps`, `continuousStatics` |
| Grants are cleared at the start of every pass, so Layers 2-5 never read last pass's | `applyContinuousLayers` |

Granted statics enter `continuousStatics` with `face: -1` and an index running across the host's grants.

Not done: a granted static's Layer 4/5 effects (the grant happens in Layer 6, Java applies them from the next pass),
granted statics read by the few walks that read `Def.Faces` directly instead of `traitFaces`, `AddSVar$` (changed SVars
a granted trait reads), the `ConvertedManaCost`/`CardManaCost` token rewrite in a granted body, and Layer 3 text changes
applied to the granted body (`AbilityUtils.getSVar` rewrites intrinsic bodies).

## `SetName$` (Layer 3)

`Card.changedName` is rebuilt by `applyContinuousNames` beside `HasNonLegendaryCreatureNames`, because Java keeps both
in one `changedCardNames` table: an overwrite resets the flag, `AddNames$` sets it. `ChosenName` is the host's last
`NameCard` pick; no pick writes nothing. `Card.Name()` is `Card.getName`; `sharesName`, `NamedCard` and
`NamedByRememberedPlayer`, the legend rule and the different-names targeting check read it. Other `Def.Name` reads
(about 60, messages and fixture dumps) keep the printed name.

Not done: `ChangeColorWordsTo$` (Swirl the Mists, 1 line). It needs the `ChangeText` mechanism (a `changedTextColors`
table and a word rewrite over every trait script), estimated 250+ lines with the regex semantics, and shares its work
with the deferred `ChangeText`/`ExchangeTextBox`.

## Legend rule Corner Case 1

`Game.isNonLegendaryCreatureName` reads `g.db` (the injected `*compile.DB`, GO-2). A name group whose name is a creature
card's non-legendary printed name also takes every `HasNonLegendaryCreatureNames` permanent. Removed cards are collected
and moved once all groups have been asked (Java's `noRegCreats`). `DB.Card` indexes front faces only, so a back face's
name is not found.

## `CounterChanged` for any `CounterType`

`emitCounterChanged` now takes `*Game`. A kind outside the nine named constants is interned per game from
`CounterDetailOpenBase` (`1<<16`) in first-use order (`Game.counterKinds`, copied by `Game.Clone`); `Game.CounterTypeOf`
decodes a Detail. The named values are unchanged, so stored shards still read.

## Layer 1: copy replacements that are not `Clone`

`copyReplacementResolvable` accepts any `ReplaceWith$` whose API the registry has, and `runCopyReplacement` runs it with
the API it names. Primal Clay (`GenericChoice` of three `Clone | Defined$ Self` modes) and Molten Sentry (`FlipCoin`)
enter as their chosen shape; Living Lore's `ChangeZone` exiles on entry. A copy of the permanent itself
(`Defined$ Self`, `copyEffect.ofSelf`) is not a new generation of its own entry replacements, so Primal Clay's
replacement does not re-apply after its mold.

| Shape | State |
| --- | --- |
| Primal Clay family, Molten Sentry, Living Lore | resolve; scenarios `primal-clay-*`, tests `entersasnonclone_test.go` |
| The Mimeoplasm | still an error: its chain `PutCounter` with `ETB$` (counters as part of the entry) is refused before the copy |
| CR 616.1 choice among copy replacements | still an error (`PlayerController` has no replacement-order decision on this path) |
| Mystic Reflection, `Clone`'s rejected params | unchanged |

Living Lore's `SetPower$ X` over `Remembered$CardManaCost` needs the remembered-amount CDA (`layer7a-cda-amounts.md`).

Scenario verb `queue abilitychoice <i>[,...]` (`QueueAbilityChoice`) answers `ChooseAbilitiesForEffect`.

## `Condition$ Monarch`

`continuousConditionMet` resolves `Monarch` against `Game.Monarch()` (2 real lines on `Mode$ Continuous`: Dawnglade
Regent, Entourage of Trest; Queen Mother Ramonda is a `CantAttack` static, which reads `unresolvedStaticConditions`, not
this). `EnduringStory` stays open: it is a player flag set by the `Storied` keyword's `Always` trigger
(`CardFactoryUtil.java:1790`), which has no port.

Tests: `staticgrants_test.go`, `setname_test.go`, `entersasnonclone_test.go`, `putcountereffect_test.go`.
