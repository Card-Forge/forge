# Port Log — Game State: X and enters-with-counters

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `entercounters.go`, `amountheads.go` (`xPaid`),
  `effect.go` (`Registry.resolve`), `castspell.go` (`castX`), `game.go` (`xctx`)
- **Decision:** [ADR-0038](../../../adr/0038-keyword-expansion.md) for `K:etbCounter`

## `Count$xPaid`

`AbilityUtils.xCount` (`AbilityUtils.java:1624-1637`, `:1881`) reads the root ability's announced X
(`SpellAbility.getXManaCostPaid`) when it has one, else the host card's own (`Card.getXManaCostPaid`: its cast ability's
X, 0 when never cast). 927 corpus files read it: Blaze's `NumDmg$ X`, every `K:etbCounter:...:X`, Walking Ballista,
Hangarback Walker.

| Java | Go |
| --- | --- |
| `root.getXManaCostPaid() != null` | `Game.xctx`, set by `Registry.resolve` from `Ability.xManaCostPaid`/`hasXManaCostPaid` and restored afterwards |
| `getRootAbility()` | `resolveSubAbility` copies the X onto the child, so every ability of a chain reads its root's |
| `c.getXManaCostPaid()` (`getCastSA().getXManaCostPaid`) | `Card.castX`, set when the spell is cast, cleared by `Game.Move` when the card leaves the stack or battlefield |
| `ChangeX` rewrites the cast ability's X | `changeXEffect` also writes `Card.castX` of a spell |

A triggered ability of a permanent carries no X of its own, so it reads `castX`: an "enters" trigger of an X spell sees
the X it was cast with. `Game.xctx` also records the resolving ability's source, and `xPaid` reads it only for that
card: a card an X spell puts onto the battlefield (Genesis Wave) reads its own `castX` (0, it was not cast), as Java's
replacement ability, which has no X, falls back to the card's (`TestXPaidOfAnEnteringCardIsNotTheResolvingSpellsX`). A
static ability or trigger condition outside any resolving ability reads `castX` too.

**X before targets.** CR 601.2b announces X before the targets of 601.2c, so Repeal's `ValidTgts$ ...cmcEQX` sees the
chosen X. `castInstantOrSorcery` calls `announceX` first (`ChoosePayX` over `castCost`, the cost with kicker and cost
modifications), `Game.preX` carries the value, and `payManaCostX` takes it instead of asking again. While a spell is
being cast (`Game.castPending`), `xPaid` for it reads `preX`, and is unresolved (matches nothing, GO-7) until one is
announced. Auras and activated abilities with an X in their cost still announce it when they pay.

Tests: `xpaid_test.go` (Blaze), `entercounters_test.go` (X counters), `xannounced_test.go`.

## Enters with counters

CR 122.6 / 614.1c: "enters with N counters" is a replacement of the entry. Java builds it as an
`Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield` replacement whose ability is
`DB$ PutCounter | ETB$ True` (the counters go into the replaced event's counter table). Two sources write it:

- `K:etbCounter:<type>:<amount>[:<extra params>|no Condition[:<description>]]` (477 cards):
  `CardFactoryUtil.makeEtbCounter` (`:545`). `keyword.Expand` builds the replacement and a `KWEtbCounter<n>` SVar; the
  extra params ride on the replacement (`CheckSVar$ WasKicked`, `Revolt$ True`, `ValidCard$ Card.Self+escaped`, a later
  `ValidCard$` replaces `Card.Self` as in Java's map). An `EACH` type (`CounterTypes$`) stays inert.
- `K:ETBReplacement:<layer>:<SVar>:...` (415 cards, `CardFactoryUtil.java:2045`, `compile.go` `etbReplacement`), whose
  SVar is `DB$ PutCounter | ETB$ True | Defined$ ReplacedCard` for Grumgully, the Generous and 28 more. Every layer
  compiles now, not only Copy: that put the SVars of the Other layer under `apiscan -check -api`, which found two dead
  `ListTitle$` params, fixed upstream ([`upstream-patches.md`](../../upstream-patches.md#pending-upstream-fixes)).

`applyEnterCounters` (`entercounters.go`) runs from `enterBattlefieldReplacements`, after the Copy layer and before
"enters tapped" and every ETB trigger, for each of the four entry sites (a resolved permanent spell or Aura, a land
play, `ChangeZone`). Every matching replacement applies, not only the first: counters from separate effects add up. Java
puts them all into one counter table and applies the `AddCounter` replacements once, so the amounts of one kind are
summed first and `countersReplaced` runs once per kind (Hardened Scales adds one for the lot). `eachReplacement` reads
only the live faces (`liveFaces`), so a front face entering does not pick up its back face's `K:etbCounter`.

| Shape | Result |
| --- | --- |
| `Defined$ Self` on moved's own line | counters on moved |
| `Defined$ ReplacedCard` | counters on moved, from another permanent's line (`ValidCard$ Creature.YouCtrl+Other`, ...) |
| `Defined$ Self` on another permanent | none: its own "self" is not what entered |
| any other `Defined$`, an extra `PutCounter` param, an extra replacement param (`Optional$`), an amount that does not resolve | pending error (ADR-0020 decision 4, GO-7), never an entry without the counters |

A replacement whose condition does not resolve (`CheckSVar$` over an amount `resolveAmount` lacks, `ValidCard$` with an
unported property such as `escaped`) does not apply: the permanent enters without the counters. That is the existing
"skip what cannot be evaluated" rule for replacement conditions, not an error.

Not covered: tokens (`Token` creates them without `enterBattlefieldReplacements`), `CounterTypes$`, a replacement the
player may decline. Tests: `entercounters_test.go` (Big Mother Mouser enters 0/0 with two counters and survives,
Broodguard Elite with X = 3, Grumgully adding one to Grizzly Bears).

### Bloodthirst

`Bloodthirst$ True` on a replacement or trigger is `Player.hasBloodthirst`: an opponent has been dealt damage this turn.
`Player.DamageReceivedThisTurn` (reset each turn; `dealPlayerDamage` adds the damage left after prevention and
replacement, as `GameEntity.damageReceivedThisTurn` records it) feeds the flag in `triggerCommonRequirementsMet`
(`opponentWasDamaged`) and `Count$BloodthirstAmount` (`bloodthirstAmount`, the opponents' damage together). A line this
file cannot evaluate past the flags `triggerCommonRequirementsMet` reads still records a pending error.
