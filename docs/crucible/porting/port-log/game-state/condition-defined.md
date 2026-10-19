# Port Log — Game State: `ConditionDefined$`

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `trigger.go` (`isPresentMatches`,
  `definedPresentMatches`), `defined.go` (`definedCards`), `ability.go` (`abilityRefs`, `parentTargets`), `condition.go`
  (`subAbilityConditionMet`)

## What it counts

`SpellAbilityCondition.areMet` (`SpellAbilityCondition.java:348-373`): with `ConditionPresent$` set and
`ConditionDefined$` naming objects, the candidates are `AbilityUtils.getDefinedObjects(host, defined, sa)` instead of a
zone scan; `ConditionPresent$` filters them and `ConditionCompare$` (default `GE1`) compares the count. About 1,200
corpus uses, 659 of them `Remembered`, 169 `Targeted`, 90 `ChosenCard`, 75 `Self`.

`isPresentMatches` sends every `PresentDefined$`/`ConditionDefined$`/`RepeatDefined$` to `definedPresentMatches`, which
resolves the name with `definedEntities` against the ability that is resolving when its source is the evaluated host
(`Game.resolving`; `a.refs()` carries targets, parent targets, the triggering card and the paid lists). Cards count by
`valid.Matches`, players by `matchesPlayerSpec`. An LKI spelling (`RememberedLKI`, `TriggeredCardLKICopy`) reads a card
that left the battlefield from `Game.LKI`.

| Name | Objects |
| --- | --- |
| `Self`, `Enchanted`, `Equipped`, `Imprinted`, `ChosenCard`, `Remembered[LKI]` | the host's own references (`definedCards`) |
| `Targeted`, `ThisTargetedCard` | the ability's targets |
| `ParentTarget` | the nearest ancestor ability's targets (`Ability.parentTargets`, set per sub-ability) |
| `Sacrificed` | the cards the activation cost sacrificed (`paidLists`) |
| `TriggeredCard[LKICopy]` | the card the trigger recorded; read only by a condition |
| `DelayTriggerRemembered[LKI]`, `TriggeredAttacker`/`Blocker`/`NewCard`, `ReplacedCard` | the existing `definedCards` cases |

## Fails closed

A name the port cannot resolve (`Collected`, `TriggeredTargetLKICopy`, `ExiledWith`, ...), a `ParentTarget` with no
ancestor that chose targets, a `Sacrificed` list no cost recorded, and a `TriggeredCard` outside a trigger record a
pending error naming `ConditionDefined$`: the ability fails (GO-7) instead of reading as unmet. Outside an ability
resolving from the same host the condition stays false without an error (a trigger or static check has no ability to
fail).

The per-effect "unresolved params" lists no longer name `ConditionDefined$` (`animate.go` keeps it: Animate does not
check `subAbilityConditionMet`); `SetInMotion`'s local evaluator is gone. Tests: `conditiondefined_test.go`, plus
`TestControlSpellPerplexingChimeraOffersARetarget` (`spellcontrol_test.go`).

Not resolved: `Collected` (9), `TriggeredTargetLKICopy` (4), `ReplacedSource` (3), `TriggeredSpellAbility` (2),
`TriggeredSourceLKICopy` (2), `TopOfLibrary` (2), `ExiledWith[Source]` (4). `ConditionZone$` and the `Condition$` values
`conditionValueRead` knows are read in `subAbilityConditionMet`; any other `Condition$` value is a pending error (R9n,
[`m6-r9n-checksvar-heads.md`](m6-r9n-checksvar-heads.md)).
