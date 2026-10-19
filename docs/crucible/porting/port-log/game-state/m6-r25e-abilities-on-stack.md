# Port Log — Game State: abilities on the stack as targets (R25e)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Decision:** [ADR-0041](../../../adr/0041-stack-items-as-target-entities.md)
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `id.go` (`StackItemEntity`, `AsStackItem`),
  `targeting.go` (`stackAbilityCandidates`, `canTargetStackItem`, `stackItemHostMatches`, `stackItemMatches`,
  `hostCardProperty`, `targetStillLegal`, `recordStackTargets`, `stackTargetOf`), `countereffect.go` (`counterAbility`,
  `removeStackItem`), `replacementevents.go` (`counterCantHappen`, `counterReplaced`, `stackValidSA`),
  `changetargetseffect.go`, `copyspellabilityeffect.go`, `defined.go` (`TargetedController`), `stack.go` (`isCrime`),
  `ability.go` (`Ability.stackTargets`), `subability.go`; [`internal/fixture`](../../../../../crucible/internal/fixture)
  (`stack:N` target token); [`internal/smoke`](../../../../../crucible/internal/smoke) (`stackTailored`, `stackAbility`)
- **Java:** `SpellAbility.java:2046-2110` (`canTargetSpellAbility`), `:2193-2261` (`isValid`), `:1383-1597`
  (`canTarget`), `:1779-1781` (`canBeTargetedBy`); `SpellAbilityProperty.java:213-247`, `:319-320`;
  `TargetSelection.java:288-331`; `MagicStack.java:734-782` (`hasFizzled`), `:1034-1054` (`commitCrimeCheck`);
  `CounterEffect.java:61-117`, `:247-308`; `ReplaceCounter.java:46-57`; `AbilityUtils.java:956-969`, `:1264-1278`

## An ability on the stack is a target

A spell is still targeted as its card on the stack (R9i). An activated or triggered ability is targeted as its stack
item, `StackItemEntity(item.ID)`: a third `EntityID` kind (bit 30), so `ChooseTargets`, `Ability.Targets` and the fizzle
check carry it unchanged. `AsCard` excludes it, so every card or player reader skips it.

| Piece | Does | Java |
| --- | --- | --- |
| `stackAbilityCandidates` | Every stack item, top first, the targeting ability can target; a spell as its card, an ability as its stack item | `TargetSelection.java:288-303` |
| `canTargetStackItem` | Not the ability itself; host matches ValidTgts$; item matches TargetType$; one of its targets matches TargetValidTargeting$ | `SpellAbility.java:2046-2110` |
| `stackItemMatches` | `Activated` and `Triggered` read the item's `causeKind` (a trigger `pushTriggeredAbilities` pushed, else activated); `Ability` any non-spell | `SpellAbility.java:2224-2233` |
| `hostCardProperty` | A TargetType$ property the ability does not have is read off its host card: type and supertype words, colors, `MultiColor`, `YouDontCtrl`, each `non` form; any other errors | `SpellAbilityProperty.java:319-320` |
| `stackItemHostMatches` | An ability of an Effect card is matched through its effect source in Java, which is not recorded: a bare `Card` alternative still answers, any other spec errors | `SpellAbility.java:2098-2107` |
| `targetStillLegal` | A stack-item target is illegal once its item left the stack or `canTargetStackItem` fails; a spell target under TargetType$ is re-checked the same way | `MagicStack.java:753-755` |
| `recordStackTargets` | Before resolution, each targeted ability's controller and host are kept on the targeting ability, read once a Counter removed it | target object kept in `TargetChoices` |
| `isCrime` | Targeting an ability an opponent controls is a crime | `MagicStack.java:1041-1044` |
| `definedPlayers` | `TargetedController` adds each targeted ability's controller (Deny the Witch, Ayesha Tanaka's UnlessPayer$) | `AbilityUtils.java:956-962` |

## Counter, ChangeTargets, CopySpellAbility

| API | Ability target |
| --- | --- |
| Counter | `counterAbility`: RememberForCounter$ remembers the host; gone item skipped; Counter replacement checked with the host as Affected and the item's kind as ValidSA$ (Spider-Punk); item removed by ID, no card moves; DestroyPermanent$ destroys the host; RememberCountered$ the host |
| Counter | Ward on an ability now goes through `counterAbility`, so a Counter replacement applies to it as well |
| Counter | `TargetValidTargeting$` is no longer rejected: targeting reads it, and the fizzle check re-reads it |
| Counter | A ReplaceWith$ Counter line matching an ability records a pending error and replaces nothing: no corpus ReplaceWith$ line names an ability |
| ChangeTargets | Stack-item targets name their item directly; the part's candidates exclude the item itself by entity |
| CopySpellAbility | Still refused: `TargetType$` past `Spell` and a `Defined$ Targeted` ability error. `copySpell` builds a new spell card; a copied ability keeps its host (CR 707.10), not built |

## Decisions

| Decision | Why |
| --- | --- |
| Third `EntityID` kind, not a separate target list (ADR-0041) | One target answer through `ChooseTargets`, Charm modes, links, retargeting and copying; Disallow asks one question over spells and abilities |
| `Ability.stackTargets`, recorded at the CR 608.2b check | Java keeps the SpellAbility object; the engine keeps no item past the stack. Recorded once per resolution, nil when nothing is a stack item, shared by Clone |
| `Activated` = `causeKind` activated, not only `activated` | Same vocabulary as ValidSA$/ValidCause$ statics (`game.go`); every non-spell item not pushed as a trigger is an activated ability |
| No new `PlayerController` method | Targets are `ChooseTargets` over `[]EntityID` |
| Fixture `stack:N` | StackItemIDs are assigned in push order from 1, so a fixture can name one before it exists |
| Smoke probe: an opponent's Jayemdae Tome activation on the stack | Stifle and its kin were inconclusive only because the probe never had an ability on the stack; the probe player's own for a `YouCtrl` form |

## Smoke probe

`crucible card-smoke -all -workers 8 -list`, before → after:

| Status | Before | After |
| --- | --- | --- |
| plays | 30,637 | 30,656 |
| refused | 2,938 | 2,926 |
| placed | 373 | 373 |
| inconclusive | 130 | 123 |
| judged play | 91.2% | 91.3% |

| Flip | Cards | Which |
| --- | --- | --- |
| inconclusive → plays | 8 | Probe tailors an activated ability: Bind (two corpus entries), Interdict, Repudiate, Rust, Squelch, Stifle, Trickbind |
| refused → plays | 11 | Counter's `TargetValidTargeting$` read by targeting: Avoid Fate, Confound, Hindering Light, Intervene, Keep Safe, Outwit, Psychic Rebuttal, Rebuff the Wicked, Turn Aside; host-card properties: Louisoix's Sacrifice, Tale's End |
| refused → inconclusive | 1 | Consign to Memory: its property now reads, and the probe has no colorless spell or trigger to target |
| plays → refused | 0 | |

Still inconclusive for the probe board, not the engine: Reroute (the Tome's ability has no target), Teferi's Response
and Not of This World (no ability targeting a permanent of theirs), Consign to Memory.

## Not done

| Shape | Cards | Blocker |
| --- | --- | --- |
| Copy an activated or triggered ability | Strionic Resonator, Lithoform Engine, Kirol, Echo, Gogo, Tawnos, Return the Favor, Weaver of Harmony, Adric, ... | A copy keeping its host (CR 707.10) and Mode$ SpellAbilityCopy triggers; refused loudly |
| Chef's Kiss | Chef's Kiss | ControlSpell `RememberTargets$`, unrelated to ability targets |
| Emissary of Grudges, Ertai Resurrected | | Their own refusals (`ChoosePlayer Secretly$`, `Destroy RememberLKI$`) |
| A ReplaceWith$ Counter line on an ability | | None in the corpus; pending error |

## Tests

`abilitiesonstack_test.go` (Stifle on an activated and a triggered ability, Bind refuses a trigger, a second Bind
fizzles, Deny the Witch drains after the counter, Trickbind locks the host, Teferi's Response destroys the source and
needs an ability targeting your land, Consign to Memory's host color, Spider-Punk stops the counter, crime, Reroute
moves a ping, Strionic Resonator refused, unknown property and effect-card host errors), `retargeting_test.go`
(`TestChangeTargetsRetargetsAnAbilityOnTheStack`), `spellcontrol_test.go`
(`TestControlSpellTargetValidTargetingFailsClosed`), `internal/smoke` `TestProbeTailorsAnAbilityOnTheStack`. Scenarios
`stifle-counters-an-activated-ability-pyromancer-ping`, `bind-counters-an-activated-ability-and-draws`,
`deny-the-witch-counters-a-ping-and-drains-its-controller`.
