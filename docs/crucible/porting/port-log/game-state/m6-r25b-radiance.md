# M6 R25b: `Radiance$`

"Target X and each other permanent that shares a color with it." 10 real cards script `Radiance$ True` on a targeted
`DealDamage`, `Pump`, `Untap`, `Destroy`, `Protection` or `PreventDamage` line (Cleansing Beam, Brightflame, Bathe in
Light, Incite Hysteria, Leave No Trace, Rally the Righteous, Surge of Zeal, Wojek Embermage/Apothecary/Siren, ...).

## Java

`CardUtil.getRadiance(sa)` (`CardUtil.java:149-182`). Not a targeting rule: the extra permanents are never announced, so
`targetChoiceFor` treats a `Radiance$` line like any other `ValidTgts$` line. Each of the six effects above calls it at
resolution (`DamageDealEffect.java:195`, `PumpEffect.java:489`, `UntapEffect.java:53`, `DestroyEffect.java:54`,
`ProtectEffect.java:141`, `DamagePreventEffect.java:102`) and applies itself to the result too. Rules read from the
source:

| Rule                                                               | Java                                        |
| ------------------------------------------------------------------ | ------------------------------------------- |
| Reads the targeting ability: `sa` itself or the nearest ancestor   | `getSATargetingCard`, `CardUtil.java:150`   |
| Only that ability's `Radiance$` counts; a sub-ability needs none   | `CardUtil.java:151`                         |
| Card targets only; a player target yields nothing                  | `getTargetCards`, `CardUtil.java:159`       |
| Colors of all targets combined; battlefield cards of each color    | `getColoredCardsInPlay`, `:170-174`         |
| Excludes targets, keeps `ValidTgts$` matches for the host's player | `c.isValid(valid, controller, ...)`, `:175` |

`isValid`, not a targeting check: hexproof, shroud and protection do not stop a radiance permanent.

## Go

| Piece                            | Where                                                                                      |
| -------------------------------- | ------------------------------------------------------------------------------------------ |
| `targetUnresolvedParams` removed | `targeting.go` (the list held only `Radiance`)                                             |
| `Ability.radiance`               | the ancestor's `ValidTgts$`, passed to a `SubAbility$` child by `resolveSubAbility`        |
| `Ability.radianceSpec`           | own `ValidTgts$` when `a` has card targets and names `Radiance$`, else the inherited value |
| `Game.radianceCards`             | WUBRG, then battlefield order; one entry per permanent; computed at resolution             |
| Six effects call `radianceCards` | one line each; `Radiance` dropped from `Pump`, `Protection`, `PreventDamage` reject lists  |

Rally the Righteous: the `Untap` targets, its `Pump` child has `Defined$ ParentTarget` and no `ValidTgts$`; the child
inherits the parent's spec and `parentTargets`, as `getSATargetingCard` walks up.

A phased-out permanent is skipped: `Player.getColoredCardsInPlay` reads `getCardsIn(Battlefield)`, which filters them
(`PlayerZoneBattlefield.java:75`). Divergence from Java: none known. `DamageDealEffect` skips a radiance permanent that
left the battlefield (`isInPlay`); so does the Go loop.

## Tests

Scenarios `radiance-cleansing-beam-damages-every-creature-sharing-a-color`,
`radiance-rally-the-righteous-untaps-through-the-sub-ability`,
`radiance-leave-no-trace-destroys-enchantments-sharing-a-color`; module tests in `radiance_test.go` (Pump, DealDamage,
Protection, PreventDamage, player target). `TestLoseLifeEffectValidTgtsRadianceIgnoresPlayerTargets` replaces the old
"skips silently" test.

## Still refused

Brightflame: its `DBGainLife` reads `Count$TotalDamageDoneByThisTurn` (`AbilityUtils.java:2400`,
`Card.getTotalDamageDoneBy`), an unported count. `card-smoke` shows `GainLife: LifeAmount$ "_" is not resolvable`, a
loud refusal after the damage resolves.
