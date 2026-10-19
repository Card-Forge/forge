# Port Log — Game State: CR 305.7, a set land subtype removes the land's abilities

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `card.go` (`printedTraitsRemoved`, `traitDef`,
  `traitFaces`, `liveTraitFaces`), `typemod.go` (`TypeEffect.RemoveLandTypes`, `removesLandAbilities`), `dependency.go`
  (`staticExists`)

Java: `Card.hasRemoveIntrinsic` (`Card.java:3416-3422`) is true while any Layer 4 change carries `RemoveLandTypes`
(`CardChangedType.isRemoveLandTypes`): Blood Moon, Magus of the Moon, Evil Presence, an Animate with `RemoveLandTypes$`.
`CardState.LandTraitChanges` (`CardState.java:555-610`) then clears, in Layer 4, every spell ability, trigger,
replacement effect, static ability and keyword accumulated so far — the printed ones and Layer 3's text-gained ones —
and adds the basic land type's mana ability. Layer 6 grants come after it and still apply.

| Trait | Go reader | Under CR 305.7 |
| --- | --- | --- |
| Static abilities | `continuousStatics` and every static scan, through `traitFaces`/`traitDef` | Printed and text-gained lines gone |
| Replacement effects | Every watcher scan in `replacement.go`, `gameloss.go`, `regeneration.go`, ... | Gone |
| Triggers | `triggerFaces` | Printed faces gone; grant rows and `traitGrants` kept |
| Activated and mana abilities | `abilityAt` | Printed indices not found; granted indices unchanged |
| Keywords | `KeywordLines` | Printed lines gone; `KeywordMod` grants kept |
| Basic land type mana ability | `TapLandForMana` (from the subtype) | The new subtype's |

`TypeEffect.RemoveLandTypes` is set by `layerTypeChange` (statics) and `buildAnimateCharacteristics` (Animate). SVar
amounts are not abilities and stay readable (`valid.go`'s amount lookup reads `Def` directly).

CR 613.8a's existence test reads the same flag (`staticExists`): Urborg, Tomb of Yawgmoth depends on Blood Moon, which
removes its ability, so Blood Moon applies first whatever the timestamps and Urborg's static is skipped once its host
has lost it (Java's `applyContinuousAbilityBefore` returning null).

Entering lands (CR 614.12): `enterBattlefieldReplacements` (`entersascopy.go`) calls `refreshLayers` (`action.go`: the
state-based-action pass's layer sequence without its control corrections) before any entry replacement runs, so the card
already on the battlefield is the permanent as it would exist there: under Blood Moon Gruul Guildgate and Temple Garden
are Mountains without their "enters tapped" replacements (`ReplacementHandler.getReplacementList`'s LKI look-ahead,
`ReplacementHandler.java:79-98`, which this replaces). The moving card's own copy replacements and enter-tapped
replacements read `traitDef`/`traitFaces`.

`RemoveAllAbilities$` uses the same hiding (`abilityRemoval`,
[`layers.md`](layers.md#cr-613-6-carried-sets-and-removeallabilities)).

Tests: `landabilityremoval_test.go`, `enterstappedunless_test.go`, scenarios
`blood-moon-removes-a-shock-lands-replacement-on-entering` (Crystal Vein's printed mana ability, Darksteel Citadel's
printed keyword, Urborg in both timestamp orders, Chromatic Lantern's granted mana ability surviving and offered by
`ActivateAbility`'s `manaChoices`).
