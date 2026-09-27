# Porter plan: CR 613 Layers 4/5/6 past a literal token list

Scratch file for whoever resumes this branch; deleted in the final commit.

## Findings (orientation)

- `*cardtype.Registry` is already reachable from the engine: `compile.DB.Types()` (`WithTypes`, set by `LoadDB`), used
  by Animate's `subtypeCategoryDrop` and ChooseType. No new `Game` field needed; reuse that seam.
- Upstream scripts now write `AffectedDefined$ Self|Enchanted|Equipped|AttachedBy Self` (+ optional `Affected$`) for
  most Aura/Equipment/self lines. Every `applyOneContinuous*` skips `AffectedDefined$`, so real coverage is far below
  the port-log's 201/284, 54/61, 1556/1857.
- `IsPresent$`/`CheckSVar$`/`EffectZone$`/`TopCardOfLibraryIs$` are ignored today: a gated line applies unconditionally.
- Host memory (chosen type/color/number/player/name/even-odd, imprinted) already exists (`memory.go`).

## Steps (commit after each)

1. Shared gate + affected-set helpers in a new file (`continuouslayers.go`): StaticAbility.checkConditions (EffectZone,
   Condition, IsPresent, CheckSVar chain, TopCardOfLibraryIs; unresolved keys skip) and getAffectedCards
   (AffectedDefined four values, AffectedZone list/All, CharacteristicDefining self, Affected$ filter). Clear mods on
   every non-battlefield card too.
2. Layer 4: use helpers; ChosenType/ChosenType2/ImprintedCreatureType/AllBasicLandType/AllNonBasicLandType via host
   memory + `g.db.Types()`; Remove\*Types flags via TypeEffect fields/subtypeCategoryDrop. AddAllCreatureTypes deferred
   (needs `cardtype.Line` flag).
3. Layer 5: helpers; ChosenColor from host memory.
4. Layer 6: helpers; Chosen\* substitutions, AllColors/ColorsYouCtrl/YourBasic expansion, HostCardUID, CardManaCost/
   ConvertedManaCost/CardColors per affected card, RemoveKeyword$/RemoveAllAbilities$ keyword half.
5. Scenario fixture, port-log note `game-state/layers-4-5-6-dynamic.md`, game-state.md row + counts, gates full.
