# M5 batch G: targeting restrictions, player Protection, Ward residue

## `Mode$ CantTarget`

`cantTargetStatic` (staticability.go) takes a `targetAsk` instead of a bare ability kind: `kind`, the asking ability's
root line (`root`; a Charm mode asks through its Charm, `Ability.charmRoot`), an Aura's Enchant text (`enchant`) and
`casting`.

| Param | Port |
| --- | --- |
| `SourceCanOnlyTarget$` | `targetAsk.onlyTargets` (`StaticAbilityCantTarget.java:108-129`): every targeting part under the root (a Charm's every `Choices` chain, else the root's `SubAbility$` chain) names a `ValidTgts$` that contains the word, no comma, no `non`+word. Wall of Shadows (1 line). An asker with no known root never qualifies. |
| `EffectZone$ Stack` | live only on the spell being cast (`targetAsk.casting`, set by `castInstantOrSorcery`, `castAura`, `castPlayerAura`; cleared once targets are chosen). `cantTargetHost` skips the host-zone test (`staticOtherConditionsMet`) because the card is still in hand Go-side. Enthralling Hold, Dream Leash (2 lines). Copies and resolution re-checks never set it. |

Fixtures: `cant-target-only-walls-*` (4), `cant-target-stack-zone-*` (3).

Corpus counts for the other `canTarget` params (root `A:`/`SP$` lines and `DB$` lines together): `TargetUnique` 131,
`TargetsForEachPlayer` 68, `TargetsWithSameController` 35, `TargetsWithDefinedController` 70 (`Triggered*` 58,
`ParentTarget` 11), `MaxTotalTargetCMC` 14, `TargetsWithDifferentControllers` 8, `TargetsWithSameCardType` 5,
`TargetingPlayerControls` 5, `TargetsWithSameCreatureType` 4, `TargetsWithDifferentCMC` 3,
`TargetsWithControllerProperty` 3, the rest 1-2 each.

- `TargetUnique$` removed from `targetUnresolvedParams`: `SpellAbility.getUniqueTargets` collects only ancestors'
  targets (`SpellAbility.java:2040-2050`), so on a root or Charm mode it excludes nothing. Its lines on `SubAbility$`
  links are enforced by `resolveChainTargets` ([`m6-r9-damagesource.md`](m6-r9-damagesource.md)).
- `TargetsWithDefinedController$` is a candidate filter in `targetChoiceFor` (card targets whose controller is among
  `definedPlayers`). `ParentTarget` forms are sub-ability only and stay unreachable.
- `trimTargetSet` drops answers breaking `TargetsWithDifferentControllers`, `DifferentCMC`, `DifferentNames`,
  `EqualToughness`, `SameCardType`, `MaxTotalTargetCMC`, `MaxTotalTargetPower`, in answer order (Java asks per
  candidate; this port asks once). Fewer than `TargetMin` kept: not cast (CR 603.3c). Fixtures
  `cant-target-different-controllers-*`, `cant-target-max-total-cmc-*`.
- The remaining `canTarget` params are in [`m5-targeting-2.md`](m5-targeting-2.md). No fixture for
  `TargetsWithDefinedController$` (every real root line is a trigger's `Execute$` needing a trigger card).

## Player Protection

The Chosen forms already worked: `layerSubstituteKeyword` rewrites `ChosenName` to `Card.named<Name>` and `ChosenType`
to the chosen type before a Player's `KeywordMod` sees it (the old "skipped by `keywordTokens`" note covered only Pump
`KW$`). Fixtures need `NamedCard:` and `ChosenType:`, now loaded and dumped by `internal/fixture`
(`GameState.java:379-390`, `:1418`).

`protectionEach` now reads `Protection:Player...` as
`Card.ControlledBy <characteristic>,Emblem.ControlledBy <characteristic>` (`Protection.java:13-17`, which falls through
to the shared `Card.`/`Emblem.` wrap, `:63-65`). `matchesPlayerProperty` gained `PlayerUID_<n>` and
`OpponentOf <You|PlayerUID_n>` (`PlayerProperty.java:38-49`). The change reaches targeting, Aura attachment and
CantBlockBy alike. Fixtures: `protection-runed-halo-*` (2), `protection-serras-emissary-*` (2),
`protection-absolute-virtue-*` (2).

Pump `DefinedKW$` and its remainder are in [`m5-targeting-2.md`](m5-targeting-2.md); True-Name Nemesis has no fixture
(no `GameState` key sets a chosen player).

## Ward residue (`UnlessCost$`)

Corpus counts: `Draw<N/Player.targetedBy>` 2, `Discard<1/Hand>` 2 (Perplex, Miss Highwater), `DefinedCost_*` 5,
`RemoveAnyCounter` 2 as an `UnlessCost$` (41 as a cost), `PutCardToLibFromGrave` 5, `Mode$ CollectEvidence` triggers 2.

- `Draw<N/Player.targetedBy>` draws only for players among the owning ability's targets (`unlessCost.withTargets`,
  called from `resolveUnlessCost`). A cost no ability owns (Ward, replacements) still names every matching player. No
  fixture: both cards need a full ValidTgts/UnlessSwitched chain.
- `Discard<N/Hand>` discards the whole hand and is always payable (`CostDiscard.java:143-151`). Perplex itself is
  unreachable: its `UnlessCost$` has no `UnlessPayer$` (default `TargetedController`, `resolveUnlessCost`). No fixture.
- `DefinedCost_*`, `RemoveAnyCounter`, `PutCardToLibFromGrave`, the `CollectEvidence` trigger and the `UnlessPayer$`
  default are in [`m5-targeting-2.md`](m5-targeting-2.md).

## Observed, outside this batch

Activating an ability with no legal target still pays its cost: `ActivateAbility` pays mana and taps before
`pushTriggeredAbilities` runs `resolveTargets` (`activateability.go:355-370`), against CR 602.2b. Spells check targets
first.
