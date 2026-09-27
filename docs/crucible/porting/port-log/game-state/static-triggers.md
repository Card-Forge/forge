# Port Log — Game State: Static Triggers

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `statictrigger.go`, `trigger.go`
  (`checkTapsForManaTriggers`), `game.go` (`registry`, `pendingErr`, `TakePendingError`)
- **Decision:** [ADR-0020](../../../adr/0020-static-triggers-resolve-immediately.md)

## Static triggers resolve at their trigger site

A `T:` line with `Static$ True` never uses the stack. Wired for `Mode$ TapsForMana` (CR 605.1b triggered mana ability:
Wild Growth, Utopia Sprawl, Fertile Ground) and `Mode$ PlaneswalkedFrom`/`PlaneswalkedTo`
([`effects-planeswalk.md`](effects-planeswalk.md#mode-planeswalkedfrom-and-mode-planeswalkedto-land)). Other modes that
skip `Static$` lines today (`trigger.go` `checkAbandonedTriggers`, `checkLandPlayedTriggers`, `isBecomesTargetTrigger`)
keep skipping until each ports its effect (ADR-0020 decision 3).

Java read directly: `TriggerHandler.java:243-262` (`runTrigger`: `TapsForMana`/`ManaAdded` never wait, `:257`),
`:300-309` (`runWaitingTrigger`: statics first), `:522-527` (`playTrigger` instead of `addSimultaneousStackEntry`),
`Trigger.java:579-581` (`isStatic` = param present), `TriggerTapsForMana.java:59-88`, `AbilityManaPart.java:155-228`
(`produceMana`, `tapsForMana`), `ManaEffect.java:170-193`, `AbilityUtils.java:1010-1035` (`Triggered...Controller`),
`PlayerControllerAi.java:1377-1382` (`playTrigger` → `playNoStack`).

### What happens

| Step                         | Go                                                                                                                  | Java                                                                         |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| Mana produced, replacements  | `TapLandForMana`/`ActivateManaAbility` keep `manaReplaced`'s result, add it, pass it to `checkTapsForManaTriggers`  | `produceMana` returns `afterReplace` (`AbilityManaPart.java:174`, `:211`)    |
| Static matches first         | `tapsForManaMatches(..., static=true)` → `resolveStaticTriggers`                                                    | `runWaitingTrigger` static loop (`TriggerHandler.java:300-309`)              |
| Then the rest                | `tapsForManaMatches(..., static=false)` → `pushTriggeredAbilities`, collected after statics resolved                | Non-static `canRunTrigger` evaluated after statics ran                       |
| Resolution                   | `g.registry.Resolve` — `Optional$`, `UnlessCost$`, `SubAbility$`, `ErrUnimplemented` as for a stacked ability       | `playTrigger` → `playNoStack`                                                |
| Triggering objects, all hits | `triggeredObjects{card, activator, produced}`; `Defined$ TriggeredCardController`, `TriggeredActivator` now resolve | `setTriggeringObjectsFrom(Card, Produced, Activator)` (`...ForMana.java:88`) |

Static resolution emits no `AbilityActivated`/`AbilityResolved` and runs no state-based-action check: `playNoStack` has
neither, and the event stream must not show a resolution with no matching activation. Order is host-walk order, not
APNAP: Java walks `activeTriggers` in order for statics.

### New engine state

| Field                | Why                                                                                                                                                                                                                         |
| -------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Game.registry`      | Set by `NewGame` to `NewRegistry()`; was set lazily in `Registry.Resolve`. Mana entry points take no `*Registry`, so a static trigger there had nothing to resolve through. Shared by `Clone` like `db` (stateless effects) |
| `Game.pendingErr`    | A trigger site has no error return (`TapLandForMana`/`ActivateManaAbility` return bool, `Move` nothing). First error kept; copied by `Clone`                                                                                |
| `triggeredObjects.*` | `card`, `activator`, `produced` (a `producedMana` value, so `Clone`'s stack `append` copies it with no aliasing). `produced` has no reader yet; `ManaReflected`'s `ReflectProperty$ Produced` is its consumer               |

### Error path (GO-7)

`Game.TakePendingError` returns and clears the waiting error. Boundaries that take it:

| Boundary                              | When                                                                                  |
| ------------------------------------- | ------------------------------------------------------------------------------------- |
| `Registry.Resolve`                    | After the effect ran without error — covers an effect tapping for mana mid-resolution |
| `ResolveStack`                        | At entry, before anything resolves                                                    |
| `PassPriority`                        | At entry and after each applied action                                                |
| `fixture.RunActions`                  | After every `actions.log` line, so `tapformana` cannot swallow it                     |
| Any driver calling a bool entry point | Calls `TakePendingError` itself                                                       |

Refused, not guessed: a static match whose API is `Charm` or that names `ValidTgts$` records `... not resolvable yet`
(no static `TapsForMana` line in the corpus needs either; choosing targets would need `BecomesTarget` firing with no
stack item).

### Behavior changes

Stacked before, now immediate: the 29 `DB$ Mana` static `TapsForMana` lines. 18 `DB$ ManaReflected` lines still fail
(API unregistered), now at the tap's next boundary instead of at stack resolution. No scenario or engine test held any
of the 50 cards, so every existing fixture is unchanged. `resolveAdditional`'s `g.registry == nil` branch is unreachable
for a `NewGame` game; kept as a guard.

### Tests

`triggeredmanaability_test.go` (Wild Growth's `T:`/`SVar:` lines verbatim): mana arrives in the tap and pays `{G}{G}`
with an empty stack; no stack events; `TriggeredCardController` pays the land's controller, not the Aura's; fires from
`ActivateManaAbility`; non-static sibling still stacked; error reaches `TakePendingError`, `ResolveStack`, a clone; a
clone resolves statics. Scenario `static-mana-trigger-adds-mana-before-the-payment`: Forest + Wild Growth casts Grizzly
Bears off one tap.

Mutation check: replacing `resolveStaticTriggers` with `pushTriggeredAbilities` for the static matches in
`checkTapsForManaTriggers` fails all 7 module tests and the scenario (pool `[0 0 0 0 2 0]`, Bears still in hand).
