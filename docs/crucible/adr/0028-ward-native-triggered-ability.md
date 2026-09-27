# ADR-0028 — Ward: A Natively Constructed Triggered Ability, Not a Compiled Script

- **Status:** Accepted
- **Date:** 2026-09-27
- **Deciders:** `mc@archlab.pl`

## Context

CR 702.21a: Ward (`K:Ward:<cost>`) is a triggered ability every warded permanent carries: "Whenever this permanent
becomes the target of a spell or ability an opponent controls, counter that spell or ability unless its controller pays
[cost]." Java synthesizes it the identical way it synthesizes Hexproof/Shroud/Protection's own `CantTarget` statics
(`CardFactoryUtil.java`'s own keyword-dispatch): a `Mode$ BecomesTarget` trigger whose `Execute$` is
`DB$ Counter | Defined$ TriggeredSourceSA | UnlessCost$ <cost> | UnlessPayer$ TriggeredSourceSAController`.

Ward is not a targeting restriction (ADR-0027's own scope) — a warded permanent is targeted successfully; the trigger
fires only after. This is why `cardCantBeTargetedBy`/`playerCantBeTargetedBy` (staticability.go) never read it, and why
it needed its own decision rather than folding into that pack.

Corpus (`K:Ward:...` printed, `AddKeyword$ Ward:...` granted, both):

| Cost shape                                                                              | Lines  |
| --------------------------------------------------------------------------------------- | ------ |
| Mana (`Ward:1`/`2`/`3`/`4`/`8`)                                                         | ~210   |
| `PayLife<N>`                                                                            | ~26    |
| `Discard<...>`                                                                          | ~17    |
| `Sac<...>`                                                                              | ~8     |
| `Ward:X`, `Waterbend<...>`, `CollectEvidence<...>`, `Blight<...>`, `AddCounterYou<...>` | 1 each |

No multi-cost (`Ward:cost1:cost2`) line exists in the corpus today, so `DB$ GenericChoice`'s own multi-cost synthesis
never applies.

This port already has every piece Ward's own resolved shape composes from, built for other reasons:

- `checkBecomesTargetTriggers` (trigger.go) fires whenever an ability's targets are chosen, from five call sites
  (`castAura`, `castInstantOrSorcery`, `pushTriggeredAbilities`, `changetargetseffect.go`, `copyspellabilityeffect.go`).
- `resolveUnlessCost` (effect.go) already runs the "ask a payer, counter/skip on the answer" gate for a pure-mana
  `UnlessCost$` and an explicit `UnlessPayer$`.
- `counterEffect` (countereffect.go) already counters whatever a resolved ability's own `Targets` names, checking
  `CantBeCountered`/a Counter replacement first.
- `designationTrigger` (becomemonarcheffect.go) already constructs a `*compile.Ability` natively in Go — no script text,
  no SVar, no card in `cardsfolder` behind it — for the Monarch/Initiative/Ring designations' own triggers, each built
  fresh with `[]vocab.Param` literals at the moment it is needed.

Ward's own trigger needs the identical shape `designationTrigger` already gives Monarch/Initiative, not a second
mechanism: a `*compile.Ability` built from the Ward line's own argument (the cost text, already written in
`UnlessCost$`'s own cost-part grammar — `PayLife<2>`, `Discard<1/Card>`, ... — the exact syntax `cost.Parse` already
reads), pushed as a real stack item so it goes through priority, can be responded to, and is subject to CR 608.2b's own
re-check the same as anything else on the stack.

## Decision Drivers

- PORT-2: no script text parsed at runtime. Ward's own cost argument is not script text to re-parse into effect behavior
  — it is a value (a mana amount, a cost-part string) plugged into a Go-literal `*compile.Ability`, the identical thing
  `designationTrigger` already does for Monarch's turn-based trigger.
- CR 702.21a/g: Ward is a real triggered ability. It goes on the stack, resolves through normal priority, and (CR
  702.21g) a permanent with two or more Ward instances fires each independently — `protectionEach`'s own "check every
  line, not just the first" precedent (ADR-0027's implementing pack) applies identically.
- GO-2: no package-level state. Ward triggers are built fresh per firing, not cached on any global.
- GO-7: a Ward cost shape this port cannot pay for must fail closed (skipped, not guessed), the identical contract
  `resolveUnlessCost` already gives every other `UnlessCost$` line.

## Considered Options

1. **Precompile one `compile.Ability` template per distinct Ward cost at `carddb.DB` load, keyed by cost text, on the
   injected `*carddb.DB`.** Rejected for this pack: Ward's cost set is not closed the way a card's own script vocabulary
   is — a `PayLife<N>`/`Discard<N/Type>` line's exact argument varies per card, so the DB would need one template per
   distinct argument string, discovered by scanning the whole corpus at load for a feature this pack only resolves the
   mana shape of. Revisit once a non-mana shape is in scope; the mana shape needs no such table at all, since the amount
   is the only variable and `designationTrigger`'s own per-call construction already handles a variable argument for
   free.
2. **A hardcoded runtime check inside `cardCantBeTargetedBy`-shaped code, counting/charging inline at the moment of
   targeting.** Rejected: CR-wrong. Ward is a triggered ability, not a static check — charging inline would skip the
   trigger's own place on the stack, meaning nothing could respond to it, Stifle-shaped effects could never counter it,
   and it would resolve out of order with every other `BecomesTarget` trigger the same targeting event raises.
3. **Build the trigger's `Execute$` (`DB$ Counter | ...`) natively with `designationTrigger`'s own machinery, fire it
   the moment `checkBecomesTargetTriggers` sees a Ward-carrying entity as one of the new targets, and push the built
   `Ability` through the identical `pushTriggeredAbilities`/`PushAbility`/`resolveTop` path every other triggered
   ability already takes.** Chosen.

## Decision

1. **`checkWardTriggers` (trigger.go) runs alongside `checkBecomesTargetTriggers`**, at the two call sites that name an
   actual spell card (`castAura`, `castInstantOrSorcery`) — the two places this port casts something with a chosen
   target today. For each newly-targeted `CardEntity` whose card is on the battlefield and carries one or more `Ward`
   lines (`KeywordLines`, printed and continuously granted alike — Layer 6 grants Ward the identical way it grants
   Hexproof/Shroud/Protection, ADR-0027's own pack), each line fires independently (CR 702.21g) when `sourceController`
   is an opponent of the warded card's controller (`matchesPlayerSpec`, the same "Opponent" check
   `cardCantBeTargetedBy`'s own Hexproof gate already makes) and TriggerZones$'s own Battlefield restriction holds.
2. **Scope for this pack is the mana-cost shape only** (`cost.Parse(details).IsPureMana()` plus `mana.Parse` with no
   `X`, the identical pre-check `resolveUnlessCost` runs at resolution — checked again here, before pushing, so a cost
   this port cannot pay for is never pushed rather than pushed and then erroring at resolution). `PayLife`, `Discard`,
   `Sac`, `Ward:X` and every Alchemy/rebalanced shape (`Waterbend`, `CollectEvidence`, `Blight`, `AddCounterYou`) are
   skipped (GO-7), logged in `game-state.md`'s Not ported yet with their own corpus counts.
3. **Scope for this pack is a targeted spell only**, not a targeted activated or triggered ability. An ability has no
   `EntityID` (id.go: a card or a player) to hand `counterEffect` as the thing to counter — the identical gap
   `stackAbilityCandidates` (targeting.go) already names for `ChangeTargets`/`Counter`. `ChangeTargets`'s and
   `CopySpellAbility`'s own re-targeting call sites are out of scope for the identical reason plus one more: neither
   names the spell whose targeting just changed to the two call sites Ward reads from.
4. **The built `Ability` has `API: APICounter`, `Controller` the targeting spell's own controller, `Source` the warded
   card, and `Targets` a single `CardEntity` naming the targeting spell.** `Params` is a `*compile.Ability` built with
   `designationTrigger`'s own `[]vocab.Param` literal shape: `DB$ Counter`, `TargetType$ Spell`,
   `UnlessCost$ <the Ward line's own cost text>`, `UnlessPayer$ You` — "You" resolves to `Controller`
   (`definedPlayers`'s own already-tested case), which is set to the attacker rather than the warded player specifically
   so the existing string resolves to the correct payer without a new `TriggeredSourceSAController` case. `Targets` is
   set directly rather than through `Defined$ TriggeredSourceSA` (which `counterEffect` does not read at all, and which
   nothing in this port resolves yet) — a runtime-constructed `Ability` needs no `Defined$` indirection to reach a value
   already sitting in Go, and `counterEffect` already reads `a.Targets` directly.
5. **Pushed through `pushTriggeredAbilities`**, the same call every other triggered ability's own match list goes
   through: `resolveTargets` is a no-op for it (no `ValidTgts$` named, so `Targets` set above survives untouched),
   `PushAbility` stamps it (CR 400.7's own zoneStamp, ADR-0027) and appends it to the real stack, and `resolveTop`'s own
   `targetsStillLegal` re-check (ADR-0027) means a spell that already left the stack by the time Ward resolves is
   handled by the identical fizzle path every other stale target already is, no new code needed.
   `checkBecomes TargetTriggers` firing again for the Ward-Counter's own `Targets` (the spell it names) is harmless:
   nothing watches a card on the Stack for `BecomesTarget` today, and nothing in the corpus needs to.

## Consequences

**Good:** the dominant real shape (~252 of ~262 real corpus lines, mana plus mana-granted) resolves through the
identical pipeline used to build `RingTemptsYou`'s own triggers; a targeted spell against a Warded permanent now goes
through real priority (a Ward trigger can be responded to, an uncounterable spell still resolves, two Ward instances
fire twice).

**Bad:** an activated or triggered ability targeting a Warded permanent still does not raise it (Not ported yet);
neither does `ChangeTargets`/`CopySpellAbility` retargeting one onto a Warded permanent.

**Neutral:** the non-mana cost shapes reach the identical `resolveUnlessCost` gate every other `UnlessCost$` line
already sits behind — the day that gate resolves `PayLife`/`Discard`/`Sac`, Ward's own pre-check (Decision point 2)
widens for free, no change needed here.

## Related

ADR-0018 (stack/casting), ADR-0019 (priority), ADR-0026 (turn driver), ADR-0027 (per-target fizzle check,
`Protection Each`'s "check every line" precedent this reuses for CR 702.21g), `becomemonarcheffect.go`'s
`designationTrigger` (the native-construction precedent), [04-adr-process](../guidelines/04-adr-process.md)
