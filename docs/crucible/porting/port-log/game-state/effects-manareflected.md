# Port Log — Game State: M6 Effects: ManaReflected

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)

## ManaReflected: Produced lands, Produce and Is stay deferred

`ManaReflected`'s dominant shape, `ReflectProperty$ Produced` (22 of 47 corpus lines), now resolves
(`manareflectedeffect.go`), on top of ADR-0020's static-trigger primitive: `checkTapsForManaTriggers` (trigger.go)
records the triggering mana on `Ability.triggered.produced`, and `manaReflectedEffect.Resolve` reflects it into every
`Defined$`/targeted player's pool, honoring `ColorOrType$`/`Amount$`. `ReflectProperty$ Produce` and `Is` (25 lines)
stay `ErrUnimplemented` -- both are `A:AB$` mana abilities that belong in `ActivateManaAbility`, not a `Registry`
resolve, and `Produce`'s own recursive walk carries a Forge bug (`CardUtil.java:345`, below) not yet worth reproducing
while it's unreachable.

`a.triggered.activator == NoPlayer` is the "was this ever pushed through `checkTapsForManaTriggers`" guard: `produced`
(`ability.go`) is a value whose zero `amount` a `ProduceMana` replacement can legitimately produce (a land whose mana
was replaced away still fired the trigger), so it can't itself signal "wrong context" -- `activator` can, since
`checkTapsForManaTriggers` always sets it to the tapping player and nothing else writes `triggeredObjects`.

`ActivateAbility` (`activateability.go`) now also refuses `Name == "ManaReflected"` (previously only `"Mana"`), so
Reflecting Pool's `A:AB$ ManaReflected` line correctly never reaches the stack.

Java read directly: `ManaReflectedEffect.java:32-147`, `CardUtil.java:234-367` (`getReflectableManaColors`,
`canProduce`), `AbilityManaPart.java:200-226` (`produceMana`, `tapsForMana`), `AbilityManaPart.java:601-613` and
`SpellAbility.java:295-309` (`canProduce`), `TriggerTapsForMana.java:67-88`, `TriggerHandler.java:300-309` and
`:522-526` (static triggers).

### Corpus shapes

| `ReflectProperty$` | Lines | Where | Colors from |
| --- | ---: | --- | --- |
| `Produced` | 22 | `DB$` under a `Mode$ TapsForMana \| Static$ True` trigger, every one | The triggering mana, `AbilityKey.Produced` (`CardUtil.java:295-307`) |
| `Produce` | 15 | `A:AB$` mana abilities (Reflecting Pool, Exotic Orchard, Fellwar Stone) | What `Valid$` cards' abilities could produce (`CardUtil.java:308-345`) |
| `Is` | 10 | 7 `A:AB$`; Omnath's `DB$` (`Produced$ Combo`); Tazri/Katilda `AddAbility$` grants | `Valid$` cards' colors (`CardUtil.java:283-294`) |

Of the 22 `Produced` lines, 20 are the trigger's own `Execute$` and 2 are chained under it (Overabundance's
`SubAbility$`, Sasaya's `RepeatEach`). `Defined$`: `You` 9, `TriggeredActivator` 12, `TriggeredCardController` 1.

**Registering it now would register a `Resolve` that handles zero lines.** The `A:AB$` lines are mana abilities, so they
belong in `ActivateManaAbility` (no stack), not `Registry.Resolve`. Every `DB$` line the registry can reach is
`Produced` or Omnath's `Produced$ Combo` (`specifyManaCombo`, a multi-color split decision this port lacks). Both would
be rejected.

### What the engine lacks

In place since ADR-0020 ([`static-triggers.md`](static-triggers.md)): `Static$ True` `TapsForMana` triggers resolve
immediately, `Game` owns its registry, and `triggeredObjects` carries the produced mana, activator and card.

| Gap | Where | Blocks |
| --- | --- | --- |
| Colorless as a choice | `ColorOrType$ Type` adds colorless (`CardUtil.java:256`, `:303-305`, `:362-364`). `ChooseManaColor` (`control.go:387`) answers a `mana.Colors`, which has no colorless member; Java asks `chooseColorAllowColorless` (`ManaReflectedEffect.java:99`, `:108`) | `Type` lines |
| `ManaReflected` as a mana ability | `ActivateAbility` refuses only `Name == "Mana"` (`activateability.go:233`); registering `ManaReflected` would put Reflecting Pool on the stack. `ActivateManaAbility` accepts only `Mana` (`activatemanaability.go:244`) | `A:AB$` lines |
| `Valid$ Defined.*` names | `definedCards` (`defined.go`) lacks `ExiledWith`, `ValidGraveyard ...`, and the cost-paid `Sacrificed`/`Untapped` lists | 5 lines |

`Static$ True` `TapsForMana` triggers are already a gap without this API: 56 of 72 corpus `Mode$ TapsForMana` lines are
`Static$ True`, and 34 of them execute `DB$ Mana`. Those are stacked today. Other modes skip `Static$` lines outright
(`checkLandPlayedTriggers`, `isBecomesTargetTrigger`); `TapsForMana` does not.

**The reflection walk is buildable, with one caveat.** A card's producible colors are its basic land subtypes through
`Type()` (`basicLandType`, `manaability.go:21`), plus each `Def.Faces[0].Abilities` mana line's `Produced$` through
`producedManaColor`/`parseComboColors`, plus the `SubAbility$` chain (`SpellAbility.java:303-309`) and trigger
`Execute$` abilities (`CardUtil.java:311-314`). That is the same ability model `ActivateManaAbility` assumes.
`Produced$` shapes it cannot read (`Chosen`, `Combo Any`, `ColorIdentity`, `Special`) and `Condition*` params
(`metConditions`, `SpellAbility.java:297`) fail closed with an `error`. `Is` needs only `Card.Colors()` (`card.go:404`).

Caveat: no continuous layer reads `AddAbility$`, so Chromatic Lantern's "lands you control have {T}: add one mana of any
color" is silently not applied, and `AddType$ AllBasicLandType` (Prismatic Omen) is silently skipped
(`applyOneContinuousType`, `continuous.go:293`). A walk over printed abilities would then under-report colors without an
error. Until Layer 6 grants exist, the walk has to find a battlefield static with `AddAbility$` or a skipped `AddType$`
whose `Affected$` matches a reflected card, and fail closed. `ActivateManaAbility` has the same blind spot, but there it
errs toward "cannot activate"; here it gives a smaller color set.

### Smallest real design, in order

1. **Triggered mana abilities (CR 605.1b).** In place (ADR-0020, [`static-triggers.md`](static-triggers.md)), and
   `Produced` (22 lines) now resolves over the recorded `produced` mana -- done.
2. **Colorless choice.** A `PlayerController` decision offering colors plus colorless, mirroring
   `chooseColorAllowColorless`. `manaReflectedEffect` reflects colorless mana already when the trigger produced it
   (`ColorOrType$ Type`); this step is about a _choice_ among colors when the reflected set holds more than one, which
   no real corpus line needs yet since every production in this port is one type. Not built until one does.
3. **Activated route.** `ActivateManaAbility` branches on `Name == "ManaReflected"`; today it declines (only `"Mana"` is
   recognized), matching `ActivateAbility`'s own refusal above. `Produce` walk and `Is` colors as above, with the
   parents set (`CardUtil.java:275`, `:329-331`) so mutually reflecting lands terminate. Unblocks 25 `A:AB$`/`DB$`
   lines, less the 5 needing new `Defined$` names.

**Forge bug (PORT-8, tracked in [`forge-java-defects.md`](../../forge-java-defects.md)).** `CardUtil.java:345`: the
recursive `getReflectableManaColors(sa, ab, colors, parents)` passes the outer `sa` as `abMana`, so the nested frame's
`card` (`:243`) is the reflecting card's host, not the reflected ability's. The nested `Valid$ Defined.*` then resolves
against the wrong card (`:265`), and so does the valid-string source (`:271`). Reflecting Pool reflecting Pit of
Offerings (`Valid$ Defined.ExiledWith`) reads cards exiled with Reflecting Pool, so misses the colors Pit could produce
(CR 106.7). Correct only for the top frame.

**Still deferred, `ManaReflected`'s own `Produce`/`Is` shapes (25 of 47 lines).** Both are `A:AB$` mana abilities
belonging in `ActivateManaAbility`, not this `Registry` resolve; `Produce`'s own recursive walk additionally carries the
`CardUtil.java:345` Forge bug above, not reproduced while unreachable.
