# Port Log — Game State: M5 Layers (7d, 7b dependency)

- **Parent:** [`game-state.md`](../game-state.md)
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)
- **Builds on:** [`layers.md`](layers.md)

## Layer 7d: switch power and toughness

Forge has no layer value for it: `StaticAbilityLayer.SWITCHPT` is commented out (`StaticAbilityLayer.java:34`,
`GameAction.java:1255`). The switch is the hidden keyword `CARDNAME's power and toughness are switched`, granted by
`Pump`/`PumpAll` `KW$ HIDDEN ...`. `Card.getNetPower`/`getNetToughness` swap the whole unswitched total (current value,
temp boost, counters) when the keyword's amount is odd (`Card.java:4447-4458`). Two switches cancel.

| Go | Java |
| --- | --- |
| `pumpRecord.Switched`, `pumpKeywords` | `PumpEffect.java:51`; the one `HIDDEN` token resolved, others still refuse |
| `PT.AddSwitch`/`Switched` (parity) | `getAmountOfKeyword(...) % 2` |
| `Card.Power`/`Toughness` swap after counter | `getNetPower`; `unswitchedPower`/`unswitchedToughness` are the inputs |

Corpus: 30-odd `Pump`/`PumpAll` lines, all `HIDDEN` on the exact phrase; no `AddHiddenKeyword$` or `AddKeyword$` carries
it. `PT` carries the count (not `hiddenKeywords`, which `applyContinuousRules` clears after the P/T layer). `Pump` with
a switch and nothing else no longer returns early.

`layer7Power`/`layer7Toughness` are now Java's `getCurrentPower`/`getCurrentToughness`: Layers 7a/7b only. Layer 7c
(Java's temp boost) is `Card.modifyPT`, added in `unswitchedPower`. Before, `basePowerEQ3`-style properties saw 7c
modifiers.

Fixtures: `layer7d-about-face-switches-the-total-after-the-layer-7c-modifier`,
`layer7d-switched-attacker-deals-its-toughness-as-combat-damage`, `layer7d-two-switches-cancel`.

## Layer 7a-7c as three walks, 7b in dependency order

`applyContinuousPT` ran every static once in effectOrder, so a 7c line with an earlier timestamp evaluated its
`Affected$` before a later 7b line applied. `GameAction.checkStaticAbilities` (`:1120`) runs a layer over all statics
before the next, so now: 7a (CDAs, no search), 7b through `applyInDependencyOrder` with `setPTLayerOps`, 7c in
effectOrder (`MODIFYPT` is not in `CONTINUOUS_LAYERS_WITH_DEPENDENCY`, `StaticAbilityLayer.java:49`).
`PT.size`/`truncate` bracket the trial application. `applyOneContinuousPT` takes the sublayer and applies only its half;
a line with both Set and Add keys fixes its affected set at 7b (`staticAffected`, CR 613.6).

P/T lines now gate on `layerStaticApplies` (`IsPresent$`, `CheckSVar$`, `EffectZone$`, as Layers 4-6 do) instead of
`Condition$` alone: Andrios's `IsPresent$ Card.Self+attacking` and every Level Up line need it.

The fold stays timestamp-keyed (`foldPT`): Java keys `newPT` by the static's timestamp, so dependency changes which
cards an effect reaches, never the fold order. Observable only when an `Affected$` reads power or toughness: Andrios
(`basePowerEQ4+baseToughnessEQ3`) waits for Maha's `SetToughness$ 1`.

Fixtures: `layer-dependency-andrios-set-pt-waits-for-maha` (16 damage without the ordering, 4 with),
`layer-dependency-kormus-bell-waits-for-urborg` (Layer 4), `layer-timestamp-later-set-pt-effect-wins-*` (a pair, order
swapped).

## Not ported

| Gap | Why |
| --- | --- |
| Layer 1 ordering | `StaticAbility.generateLayer` never returns `COPY`: no static ever runs in Layer 1, so there is nothing to order |
| Layer 3 dependency | Only `GainTextOf$` (self-only) and `AddNames$` resolve; no pair of real lines can depend on each other, so it cannot be observed |
| 7c-before-7b order | Fixed, but no real corpus pair discriminates it; covered by `layer7c-applies-after-the-layer-7b-set` only for the 7b/7c order |
