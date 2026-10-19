# Port Log — Game State: DealDamage and PreventDamage `DividedAsYouChoose$` (R9)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `dealdamageeffect.go`,
  `preventdamageeffect.go`, `dividedtargets.go` (`Ability.perTargetAmounts`, `describeEntity`)
- **Java:** `DamageDealEffect.java:131-273` (`:248` the divided share, `:252-256` the in-play check, `:263-267`
  Radiance), `:275-302` (`internalDamageDeal`); `DamagePreventEffect.java:78-120` (`:105` the divided share, `:115-119`
  Radiance); `SpellAbilityEffect.java:374-383` (`getTargetEntities`); `TargetChoices.java:71-82` (a removed target's
  share goes with it)

## What `DividedAsYouChoose$` refused

R9 batch 5 ([`m6-r9-putcounter-targetmin.md`](m6-r9-putcounter-targetmin.md)) announces the division with the targets
(CR 601.2d, `divideAmongTargets`) for any ability; `DealDamage` and `PreventDamage` still listed `DividedAsYouChoose$`
among their unresolved params. `crucible card-smoke -all` refused 56 cards on it:

| Refusal | Cards | Where |
| --- | --- | --- |
| `DealDamage: DividedAsYouChoose$ not resolvable` | 51 | 48 `resolve`; Iron Fist and Mogg Mob at `activate`; Magmatic Core at `turns` |
| `PreventDamage: DividedAsYouChoose$ not resolvable` | 5 | Angel of Salvation, Embolden, Pollen Remedy, Remedy (`resolve`); Serra's Hymn (`activate`) |

One corpus shape for both: `ValidTgts$` + `TargetMin$`/`TargetMax$` +
`DividedAsYouChoose$ <the same amount as NumDmg$/Amount$>`. A per-letter scan of `cardsfolder` finds no divided
`DealDamage`/`PreventDamage` line without `ValidTgts$`, none with `Radiance$`, `DamageSource$`, `Defined$`,
`ExcessDamage$`, `DamageMap$`, `DividedUpTo$` or `DivideEvenly$`; Fight with Fire's `Condition$ Kicked` stops at
`Condition$` first (not one of the 56).

## Each target takes its own share

`DamageDealEffect.java:248`: `dmg = (sa.usesTargeting() && sa.isDividedAsYouChoose()) ? sa.getDividedValue(o) : dmg`,
inside the per-source, per-target loop. `DamagePreventEffect.java:105` is the same line for the shield amount.
`Ability.perTargetAmounts` is that line for both effects:

| Piece | Does | Why |
| --- | --- | --- |
| `perTargetAmounts` | One amount per target, read before anything is dealt or shielded: the share when divided, else the amount | A target with no share fails the line before it acts (GO-7); Java's `Integer` unboxing would throw |
| Player targets | Read by `PlayerEntity` like a card's: Arc Lightning's "2 to a creature, 1 to a player" | `getDividedValue(GameObject)`; `PutCounter` only ever read card shares |
| `DamageSource$` | Each source deals each target that target's share | `:217-268`, the share read per source |
| `ExcessDamage$` | The excess is the share's: a 1/1 given 3 takes 1, its controller 2 | `internalDamageDeal` gets the share as `dmg` |
| Dropped or gone target | Its share is lost, never redistributed; the others keep theirs | `dropIllegalTargets` (CR 608.2b, `TargetChoices.java:71-82`); `:252-256` in-play skip |
| `Ability.targetsErr` read | Both effects now fail on a target shape or division `resolveTargets` could not ask (`DividedUpTo$`, a bad answer, an unreadable total) | Pushed with no targets, the line would otherwise deal or prevent nothing, silently |

`NumDmg$`/`Amount$` is still evaluated (`:145`, `:80`) and still fails loudly when unreadable; for a divided line it is
otherwise unread. Reading `targetsErr` on every line, not only divided ones, flipped no card that played before
(whole-corpus diff, below).

## Refused, by name

| Shape | Reason |
| --- | --- |
| `DividedAsYouChoose$` without `ValidTgts$` | Java's `usesTargeting()` is false and every entity takes the whole amount, no division. No corpus line; this port does not guess at the reading |
| `DividedAsYouChoose$` with `Radiance$` | Java's loop variable keeps the last target's share and the radiant cards take that (`:248` then `:263-267`; `DamagePreventEffect.java:105` then `:115-119`). No corpus line |
| `DividedUpTo$`, `DividedRandomly$`, `DivideEvenly$`, `DividerOnResolution$` | Unchanged: each its own decision or mechanic |

The Radiance leak is a latent Java quirk (statement order, no rule behind it), not reproduced: no card reaches it.

## Smoke probe

`crucible card-smoke -all -workers 8`, before → after:

| Status | Before | After |
| --- | --- | --- |
| plays | 29,735 | 29,786 |
| refused | 3,785 | 3,734 |
| placed | 360 | 360 |
| inconclusive | 198 | 198 |
| judged play | 88.7% | 88.9% |

All 56 changed cards are the refused ones; no card went plays → refused. 51 refused → plays (46 `DealDamage`, all 5
`PreventDamage`), an upper bound: a `TargetMin$ 0` line the probe resolves over no target never reaches a division. The
division path itself is pinned by the module tests and scenarios below, not by the probe. 5 stop at a later refusal:

| Card | Next refusal |
| --- | --- |
| Avacyn's Judgment, Impact Resonance, Shatterskull Smashing | The divided total is unreadable (`Count$Madness`, `Count$SingleMaxDamageThisTurn`, `Count$Compare`): the `NumDmg$` amount family, open plan R9 |
| Ghired's Belligerence | `CopyPermanent: Populate$` |
| Ravenous Gigantotherium | `EachDamage: NumDmg$` (`Count$CardPower` over `DefinedDamagers$ Targeted`) |

## Tests

`damagedivided_test.go`: a 2/1 split between a creature and a player, one target taking it all, a dropped target's share
going with it, two `DamageSource$` cards each dealing each share, `ExcessDamage$` over a share, a bad division dealing
nothing, `DividedUpTo$`, no announced division, untargeted and Radiance refusals; `PreventDamage` shields of each share
and its bad-division and missing-share failures. Scenarios
`dealdamage-arc-lightning-divides-two-to-a-creature-one-to-a-player`,
`dealdamage-arc-lightning-bounced-target-takes-its-share-with-it`,
`dealdamage-electrolyze-one-to-each-target-then-draws`, `preventdamage-remedy-divided-shield-partly-prevents-a-bolt`.
