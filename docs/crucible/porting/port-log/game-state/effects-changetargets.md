# Port Log — Game State: M6 Effects: ChangeTargets

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `changetargetseffect.go`, `targeting.go`
- **Builds on:** [ADR-0018](../../../adr/0018-instant-sorcery-spell-object.md) stack-item identity (`Ability.ID`,
  `stackItem`, `spellItemOf`) and [`effects-play-copyspellability-v3.md`](effects-play-copyspellability-v3.md)'s
  `targetChoiceFor`

## ChangeTargets lands

`changetargetseffect.go` ports `ChangeTargetsEffect.java:48-210` (CR 115.7). Corpus: 44 lines. Targeted
Instants/Sorceries reach the stack through `castInstantOrSorcery`, `Play` and `copySpell`, each an `Ability` whose
`Targets` (and each Charm mode's `Targets`) this effect rewrites in place. Registered count 165.

Two halves, split by who reads `TargetType$`:

| Half | Who | What it needs |
| --- | --- | --- |
| Choosing the spell (push) | ChangeTargets ability's own targeting | `TargetType$` as `SpellAbility.isValid` over stack items: 31 of 44 lines name it, 23 past the literal `Spell` |
| Rewriting its targets (res) | the effect | the chosen spell's targeting parts, their legal candidates now (`targetChoiceFor`), and the chooser's new picks |

### Engine pieces

Each extends an existing piece. No new `PlayerController` method, no new `EntityID` kind.

| Piece | Where | Why | Java |
| --- | --- | --- | --- |
| `targetChoiceFor` gate: `TargetType$` + `APIChangeTargets` → `stackAbilityCandidates` | `targeting.go` | Every other API keeps the literal-`Spell` branch: its own `Resolve` was vetted against spell targets only. Generalizing for all would start pushing e.g. Wyll's Reversal's `SP$ Pump \| TargetType$ SpellAbility.numTargets GE1` with stack targets `Pump` never checked | `SpellAbility.canTargetSpellAbility` (`SpellAbility.java:2059-2122`) |
| `stackItemMatches`, `stackItemHasProperty` | `targeting.go` | `TargetType$` kinds and properties over a stack item (table below). Unknown kind, `!` negation or property → `error`. Separate from `validSAMatches` (`playeffect.go`), which judges a card's play options, not stack items | `SpellAbility.isValid` (`:2206-2270`), `SpellAbilityProperty.hasProperty` (`:213-245`) |
| `stackItemTargetsMatch`, `entityMatches` | `targeting.go` | `TargetValidTargeting$` (Muck Drubb, Rebound); `TargetRestriction$` on a card or player | `canTargetSpellAbility` (`:2085-2108`), `GameObjectPredicates.restriction` |
| `stackItemTargets`, `subChainTargets` | `targeting.go` | `getAllTargetChoices`: Aura `Target`, `Targets`, each mode's `Targets`. A `SubAbility$` naming its own `ValidTgts$` is never targeted separately (`resolveSubAbility`, `subability.go`), so Java's count is unknown here → `error` | `SpellAbility.getAllTargetChoices` |
| `targetChoice.err`, `Ability.targetsErr` | `targeting.go`, `ability.go` | Legal target with no `EntityID` (below): pushed with no targets, fails when it resolves. Pushing has no error path (GO-7); `modesErr`'s deferral | — |
| `chooseCopyTargets` returns `choice.err` | `copyspellabilityeffect.go` | A copy of a ChangeTargets spell choosing new targets hits the same refusal | — |
| `enginelint.json`: `targeting` may reference `subability` | `internal/engine/enginelint.json` | `subChainTargets` walks `findSubAbility` | — |
| Stack item `Modes` copied before a write; every rewritten `Targets` a fresh slice | `retargetParts`, `chooseNewTargets`, `changeSingleTarget` | `Game.Clone` copies `stack` shallowly (`game.go`: `append([]Ability(nil), g.stack...)`): `Targets` and `Modes` backing arrays are shared with the clone. Regression: `TestChangeTargetsRetargetsEachCharmMode` | — |
| `BecomesTarget` fired after every named spell is rewritten | `Resolve` | `checkBecomesTargetTriggers` pushes; a push can move the `g.stack` array a `stackItem` pointer names. Java only collects them in the loop (`runTrigger`), so order is unchanged | `ChangeTargetsEffect.java:178-208` |

**Why no stack-item `EntityID`.** `id.go` defines `EntityID` as a card or a player (ADR-0009's 4-byte handle); a third
kind changes that contract (needs an ADR first, ADRP-4), every `AsCard` caller would need auditing, and neither
`ScriptedController` answers nor the fixture `targets` verb can name one. So an activated or triggered ability on the
stack that would be a legal target is an `error`, not a candidate left out: offering spells alone is a narrower question
than Java asks. `Ability` carries no activated/triggered kind, so `Activated` and `Triggered` each admit every non-spell
item; that only changes which items raise the error.

### `TargetType$` over a stack item

| Kind / property | Reads |
| --- | --- |
| `Spell` | `Ability.spell` |
| `SpellAbility` | anything |
| `Ability`/`Activated`/`Triggered` | not a spell (then the no-`EntityID` error) |
| `Instant`, `Sorcery` | host card's type |
| `singleTarget` | exactly one target across all parts, the same object twice counting twice |
| `numTargets <op><n>` | distinct targets, literal `n`, `compareOp` |
| `IsTargeting Self`/`You` | the ChangeTargets host card / its controller among the targets |
| `YouCtrl`, `OppCtrl` | stack item's controller vs the ChangeTargets controller |

Then `ValidTgts$` against the item's host card (`Card,Emblem` matches every card; `Spell` never does, `baseMatches`).

### Resolution

| Shape | Resolved as | Java |
| --- | --- | --- |
| Spells: `ValidTgts$` / `Defined$ Targeted` | `a.Targets` card entities → `spellItemOf`; gone from the stack → skipped | `getTargetSpells`, `:57-61` |
| Spells: `Defined$ TriggeredSpellAbility` | `triggered.spellAbility` (Mode$ SpellCast) | `getDefinedSpellAbilities` |
| `Optional$` | `ConfirmEffect` per spell | `:67-69` |
| Targeting parts | the spell if it names `ValidTgts$` (Earthbend built in), then each Charm mode that does | sub-instance chain |
| Default (no `DefinedMagnet$`) | per part: `ChooseTargets` over the part's candidates (`targetChoiceFor` for the spell's controller, spell itself excluded), `TargetRestriction$` narrowing, exactly the old count; fewer candidates → part unchanged; answer outside the offer → `error`. Keeping a target is allowed | `:162-175`, `chooseNewTargetsFor` |
| `TargetRestriction$` "Other" | read against the part's first card target, else the ChangeTargets host | `:166-169` |
| `DefinedMagnet$` alone | each part that could target the magnet targets it alone | `:148-161` |
| `ChangeSingleTarget$` + `DefinedMagnet$` | one (part, target) pair — only one → no question (`PlayerControllerHuman.chooseTarget`), else `ChooseTargets` over the targets; replaced by the magnet (removed, magnet appended) unless already targeted or not targetable. No target at all → resolution ends (PORT-7: Java `return`s) | `:76-106` |
| New targets | `checkBecomesTargetTriggers(..., isSpellSource false, spell's controller)`, only objects new to their part | `:178-192` |

Inherited, not changed: `isSpellSource` false as `castInstantOrSorcery` passes it for the same spell's cast-time
targets, so a `ValidSource$ Spell` watcher misses both (`becomesTargetSourceMatches`, `trigger.go`; recorded in
`effects-play-copyspellability-v3.md`). `BecomesTargetOnce` has no trigger mode in this port, at cast time or here.

### Rejected with an `error` (PORT-8, GO-7)

| Param / shape | Lines | Why |
| --- | --- | --- |
| `Chooser$` | Sudden Substitution, Psychic Battle | another player chooses |
| `RandomTarget$`, `RandomTargetRestriction$` | Chef's Kiss, Grip of Chaos | random retarget; both also need an unbuilt `Defined$`/trigger mode |
| `ModeCost$` | Return the Favor | Spree cost; `Charm` charges none |
| `ConditionTargetValidTargeting$`, `ConditionTargetsSingleTarget$` | Meddle, Quicksilver Dragon | `subAbilityConditionMet` reads them as never met, silently |
| `ConditionPlayerDefined$`, `ConditionPlayerContains$` | Emissary of Grudges | same |
| `TargetsWithControllerProperty$` | 0 | `canTargetSpellAbility` filter not read |
| `Defined$` `TriggeredSourceSA`/`Remembered`/`ValidStack`/`SourceFirstSpell` | Captured by the Consulate, Psychic Battle, Chef's Kiss, Boltbender, Lightning Storm | `getDefinedSpellAbilities` shapes not built |
| `ChangeSingleTarget$` without `DefinedMagnet$` | 0 | Java's `getDefinedCardsOrTargeted` falls back to the ability's own targets (else `Self`); no line relies on it |
| Pair pick over a target two parts share | — | needs the (part, target) pair chosen; `PlayerController` has no such decision |
| Aura spell's attach target | — | `Ability.Target`, chosen through `ChooseEnchantTarget`; not rewritten here |
| Spell whose `SubAbility$` names `ValidTgts$`; `DividedAsYouChoose$` part | — | sub-ability targets not modelled; a new target would take over the old one's share (`ChangeTargetsEffect.java:103`), not rewritten |
| Legal target is an activated/triggered ability | `SpellAbility.*`, `Spell,Activated,Triggered`, `Activated.*` lines, when one is on the stack | no `EntityID` (above) |

`UnlessCost$` (Divert) fails in `resolveUnlessCost`: no `UnlessPayer$`, whose Java default `TargetedController` is not
resolved there.

### Reachability

End to end today: a `Mode$ SpellCast` trigger whose `Execute$` is ChangeTargets, fired by `CastSpell`
(`TestChangeTargetsRetargetsASpellWithASingleTarget`, `retargeting_test.go`). No real card reaches it yet: `CastSpell`
and `ActivateAbility` require an empty stack (no priority window), Speedball's trigger needs `TargetsValid$`, Perplexing
Chimera's and Commandeer's chains need `ControlSpell`, Wyll's Reversal's parent `Pump` names a `TargetType$` its own
targeting does not read, Captured by the Consulate is broken upstream (below). No effect-driven cast route is known
either: a `Play`/`Discover` cast during a resolution finds nothing left under it to retarget. So no scenario fixture:
nothing a `setup.state`/`actions.log` can drive reaches the effect.

Registered anyway, unlike `Planeswalk` (`effects-batch-a.md`), whose every line resolves to Java's own no-op outside a
Planechase game: ChangeTargets does real, tested work on real stack items, and goes live as soon as a priority window
(or one of the trigger/parent gaps above) lands, with no change here.

### Charm modes that target the stack

`modeHasLegalTargets` (`charmeffect.go`, `makePossibleOptions`' CR 603.3c filter) judges a mode naming `TargetType$`, or
a `CopySpellAbility` mode, by `targetChoiceFor`'s stack scan instead of the battlefield scan. Reason: over an empty
stack the battlefield scan offered Insidious Will's/Untimely Malfunction's ChangeTargets mode (and Return the Favor's
two), mode indices then disagreed with Java's option list, and picking it made the whole Charm silently not cast
(`resolveTargets` false). A `targetChoice.err` keeps the mode, so its error surfaces when it resolves. Regression:
`TestCharmOffersAStackTargetingModeOnlyWithACandidate`.

**Forge defect found.** `captured_by_the_consulate.txt:8`: `Defined$ TriggeredSourceSA` under a `Mode$ SpellCast`
trigger, which never records `SourceSA` (`TriggerSpellAbilityCastOrCopy.setTriggeringObjects`,
`TriggerSpellAbilityCastOrCopy.java:232-251`; `MagicStack.java:377-394` passes only `Activator`, `SpellAbility`,
`CurrentStormCount`, `CurrentCastSpells`). The retarget never happens. Logged in
[`card-script-defects.md`](../../card-script-defects.md). The line is fixed upstream
([#12193](https://github.com/Card-Forge/forge/pull/12193): `Defined$ TriggeredSpellAbility`).
