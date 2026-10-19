# ADR-0020 — Static Triggers Resolve Immediately; `Game` Owns Its Registry

- **Status:** Accepted
- **Date:** 2026-09-26
- **Deciders:** `mc@archlab.pl`

## Context

A trigger line carrying `Static$ True` never uses the stack in Forge. `TriggerHandler.runWaitingTrigger` runs every
static trigger first, before any other trigger of the same event (`TriggerHandler.java:300-309`), and
`runSingleTriggerInternal` resolves it on the spot through `playTrigger` instead of `addSimultaneousStackEntry`
(`TriggerHandler.java:522-527`). CR 605.1b is the rules basis for the mana half: a triggered mana ability ("whenever …
is tapped for mana, add …") resolves immediately and is never put on the stack.

The corpus has 246 `T:` lines with `Static$ True`: `ChangesZone` 125, `TapsForMana` 49, `SpellCast` 17, `Phase` 17,
`TurnBegin` 8, `DamageDone` 6, a tail. Of the 49 `TapsForMana` lines, the `Execute$` is `DB$ Mana` 29 times (Utopia
Sprawl, Wild Growth, Fertile Ground) and `DB$ ManaReflected` 18 times (Zendikar Resurgent, Mana Flare).

This port has no static-trigger path:

| Site                                                                                         | Today                                                                                         |
| -------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| `checkTapsForManaTriggers` (`trigger.go:1624`)                                               | Pushes `Static$ True` matches through `pushTriggeredAbilities` onto the stack, like any other |
| Other modes (`trigger.go:1421`, `:3017`, `:3193`)                                            | Skip a `Static$` line outright                                                                |
| `Game.registry` (`game.go:50`)                                                               | Set lazily, only inside `Registry.Resolve` (`effect.go:75`)                                   |
| `TapLandForMana` (`manaability.go:60`), `ActivateManaAbility` (`activatemanaability.go:234`) | Take no `*Registry`, so a trigger fired there has nothing to resolve through                  |

Stacking a mana trigger is wrong in a way no test catches: the mana arrives after the cost it was tapped for is already
paid or failed. Skipping the line is safe only because the effect is refused loudly elsewhere. Neither survives
`ManaReflected` (`effects-manareflected.md` (git `624d87dba16`), "Smallest real design", step 1, which names this ADR as
its prerequisite).

## Decision Drivers

- CR 605.1b and parity with `TriggerHandler.java:300-309`/`:522-527` (PORT-7).
- GO-7: a static trigger whose effect is unimplemented must still fail its game loudly, not be skipped.
- No `NewGame` or `ResolveStack` signature change for the 19 existing `NewGame` call sites and every test.
- TEST-1 byte-identity: no scenario or engine test puts a `Static$ True` `TapsForMana` card on the battlefield today
  (checked by name against all 50 such cards), so no `expect.events` moves.

## Considered Options

1. **Pass `*Registry` into every trigger-check function and every mana entry point.** Rejected: widens a dozen
   signatures and every caller for a value that is the same immutable table in every game.
2. **Keep stacking static triggers; resolve the stack inside `TapLandForMana`.** Rejected: resolves unrelated stacked
   items too, and still needs a registry there.
3. **`Game` owns its registry from construction; static matches resolve at the trigger site.** Chosen.

## Decision

1. **`NewGame` sets `Game.registry` to `NewRegistry()`.** `Registry` is a generated `[numAPITypes]Effect` array with no
   mutable state (`effect.go:40`, `registry_gen.go:14`), so sharing one per game is GO-2-clean — the same standing as
   the injected `*compile.DB`. `ResolveStack`/`PassPriority` keep their `reg` parameter; it resolves against the table
   passed, exactly as today.
2. **A `Static$ True` trigger resolves at its trigger site, before the non-static matches of the same event are pushed**
   (`TriggerHandler.java:300-309` ordering). It never touches the stack, never emits `AbilityActivated`, and is never
   offered a response (CR 605.1b). Resolution goes through `Registry.Resolve`, so `Optional$`/`UnlessCost$` and
   `ErrUnimplemented` behave exactly as for a stacked ability.
3. **Scope is every mode's `Static$ True` line, not only `TapsForMana`.** The modes that skip `Static$` today move to
   this path one at a time, each in the commit that ports its effect; until then they keep skipping. The contract fixed
   here is the ordering and the no-stack rule, not which modes are wired.
4. **An error from a static trigger is carried to the nearest boundary that already returns `error`** —
   `Registry.Resolve`, `ResolveStack`, `PassPriority`, or a mana entry point once it gains one. `Move` (95 call sites)
   does not change signature; the implementing commit records the pending error on `Game` and the boundary returns it
   (GO-7).
5. **`triggeredObjects` gains the fields a static mana trigger reads** — the produced mana after replacement
   (`AbilityManaPart.java:205`, `:224`), the activator and the tapped card — and `Game.Clone` covers them.

## Consequences

**Good:** unblocks `ManaReflected`'s dominant `Produced` shape (18 static lines plus 4 chained) and the 29 static
`DB$ Mana` lines, which include Modern-legal Utopia Sprawl and Wild Growth. Mana triggered abilities stop resolving
after the payment they were meant to fund.

**Bad:** a trigger site that fires during cost payment can now resolve arbitrary effects mid-payment. Only mana-adding
effects do so in the corpus's `TapsForMana` lines (plus one `DealDamage`, one `RepeatEach`), but the path is general.

**Neutral:** `ResolveStack`'s `reg` parameter and `Game.registry` can disagree if a caller passes a different table; no
caller does, and the parameter can be dropped in a later cleanup.

## Related

ADR-0017 (generated registry), ADR-0019 (priority — static triggers never enter it), `effects-manareflected.md` (git
`624d87dba16`), [04-adr-process](../guidelines/04-adr-process.md)
