# Port Log — Game State: `RememberTargets$` at resolution, counter-removal and activation records, cleanup resets, `GainControl$ Player.isMonarch` (R9o)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `stack.go` (`rememberTargets`), `effect.go`,
  `subability.go` (`rememberTargetsLine`), `ability.go` (`stackRoot`), `castrecord.go` (`recordCountersRemoved`,
  `countersRemovedCount`, `recordActivation`, `thisTurnActivatedCount`), `amountrecords.go` (`activatedThisGame`,
  `RegeneratedThisTurn`), `amountplayers.go` (`DamageToOppsThisTurn`), `saspec.go` (`spellAbilityMatchesAs`), `game.go`,
  `card.go`, `turn.go`, `statictrigger.go`, `activateability.go`, `activatemanaability.go`, `manaability.go`,
  `regeneration.go`, `removecountereffect.go`, `combatdamage.go`, `replacement.go`, `continuous.go`, `defined.go`,
  `becomesblockedeffect.go`, `controlspelleffect.go`
- **Java:** `MagicStack.java:617-647`, `:734-782`, `:983-990`, `:232-235`, `:310`, `:968`; `AbilityUtils.java:933-935`,
  `:1517-1554`, `:1848-1850`, `:2130-2131`, `:2805-2816`, `:2839-2844`, `:3657-3659`; `Game.java:114`, `:1226-1242`,
  `:1301-1321`; `Card.java:1910-1932`, `:3516-3523`, `:7273-7275`; `Player.java:815-817`, `:899`;
  `ActivationTable.java:23-62`; `Trigger.java:605-609`; `TriggerHandler.java:539`; `PhaseHandler.java:415-418`;
  `StaticAbilityContinuous.java:597-600`; `RegenerationEffect.java:41`; `CostPayment.java:136-166`;
  `WrappedAbility.java:64-67`, `:368`; `Zone.java:162-167`

## `RememberTargets$`

`AbilityUtils.handleRemembering` runs once per resolution, in `MagicStack.resolveStack` (`:646`), on the popped ability
alone, after `hasFizzled` dropped the illegal targets (`:779`) and before `AbilityUtils.resolve`. A `WrappedAbility`
forwards `hasParam`/`usesTargeting` to the trigger's `Execute$` (`WrappedAbility.java:64-67`, `:368`), so a triggered
root counts. Go: `resolveTop` marks the popped item `stackRoot` and calls `rememberTargets` right after
`targetsStillLegal`, before Ascend and `Registry.Resolve`.

| Param | Does |
| --- | --- |
| `RememberTargets$` | root line with `ValidTgts$`: its legal targets join the host's remembered list, in order |
| `ForgetOtherTargets$` | clears the list first |
| `IncludeAllComponentCards$` | refused (merged/melded parts; one corpus line, You're in Command) |
| `RememberCostMana$` | refused (Ice Cauldron, Jeweled Amulet: a mana string no reader resolves) |
| a stack-item target | refused (Java remembers the `SpellAbility`; no `Remembered` reader here sees one) |

A `RememberTargets$` line with `ValidTgts$` resolving off the stack root (sub-ability, Charm mode, `Choices$`) is an
error in `Registry.resolve`: Java never remembers its targets, so the chain reading `Remembered` acts on nothing. Two
corpus lines, both Forge script defects (below). The per-effect `RememberTargets` rejections in `BecomesBlocked` and
`ControlSpell` are gone. 101 corpus lines carry the param on a root (ChangeZone flicker/exile-until, Pump, Destroy,
PutCounter, DealDamage, ...); most played silently wrong before (Cloudshift never returned the creature, Journey to
Nowhere never gave it back, Sylvan Smite dealt 0).

## Records added

Each mirrors its Java owner, is copied by `Game.Clone` (nil when empty) and resets where Java resets it.

| Record | Java | Set | Cleared | Read by |
| --- | --- | --- | --- | --- |
| `Game.countersRemoved` | `Game.countersRemovedThisTurn` (`:114`, `:1306-1311`) | `emitCounterChanged` with delta < 0: card LKI just after the removal, or the player | cleanup (`Game.java:1230`) | `Count$CountersRemovedThisTurn` |
| `Game.activatedThisTurn` | `MagicStack.thisTurnActivated` (`:983-990`) | `ActivateAbility` (`:310`), `ActivateManaAbility`, `TapLandForMana` (`:232-235`): ability + host LKI | next turn starts (`onNextTurn`, `:968`) | `Count$ThisTurnActivated_<valid>` |
| `Card.regeneratedThisTurn` | `Card.regeneratedThisTurn` (`:3516-3523`) | `regenerateBody`, both regeneration paths (`RegenerationEffect.java:41`) | cleanup (`Card.java:7275`), zone change | `Count$RegeneratedThisTurn` |
| static trigger `trigActs` | `Trigger.triggerRun` (`TriggerHandler.java:539`) | after a `Static$ True` trigger resolved (a stacked one: as pushed) | as before | `Count$ActivatedThisGame`, limits |

`emitCounterChanged` is the single choke point: every counter change in the engine emits there after changing the
counters. Three sites emitted the requested amount rather than what was removed; they now emit Java's delta
(`Card.java:1910-1911`): `removeCounters` ("remove five" from three removes three, from none emits nothing), damage to a
planeswalker's loyalty or a battle's defense, and a `ReplaceCounter` removal.

The activation host snapshot is the card after its costs, unless a cost moved it (`zoneStamp` changed): then the card
before the costs. Java keeps the ability on the old card object, whose zone field still names the zone it left
(`Zone.remove` does not reset it), so Laboratory Drudge's `inZoneGraveyard` reads true for a card exiled from the
graveyard as its cost.

## Heads

| Head | Reads |
| --- | --- |
| `Count$CountersRemovedThisTurn <type> <valid>` | `countersRemoved`: a card entry by `Matches` on its LKI, a player entry by `matchesPlayerSpec` only when the spec has a player base (Java's `Player.isValid` is false for `Card…`) |
| `Count$ThisTurnActivated_<valid>` | `activatedThisTurn` through `spellAbilityMatchesAs` with the LKI host; Java's part after the first `_` |
| `Count$ActivatedThisGame` | the resolving root's game count (`ActivationTable.getOriginal`): trigger → `trigActs`, activated → `activations`; a sub-ability is unresolved |
| `Count$RegeneratedThisTurn` | the host's count |
| `<player>$DamageToOppsThisTurn` | sum of `DamageReceivedThisTurn` over the player's opponents still in the game (`Player.java:815-817`) |
| `Defined$ TargetedOrController` | `Targeted` players, then `TargetedController` (`AbilityUtils.java:933-935`) |

`Game.costPaymentStack` (`AbilityUtils.java:2810-2814`) is not modelled: the ability paying its costs is not counted.
Only a `Type$ Ability` ReduceCost reads this head mid-payment (Professor Hojo, Tezzeret: `LE1`), and `costmod.go`
applies `Type$ Spell` lines alone. The `Mode$ AlternativeCost` readers (Bruenor, Gavi, Forge Anew, Advancing the Spirit,
`ValidSA$ Activated.…`) are not applied either: `altcost.go` ports the self-hosted `Spell.Self` shape only.

## Cleanup resets, Layer 2 monarch

`upkeepsThisTurn` and `combatsThisTurn` are zeroed as cleanup begins (`PhaseHandler.java:415-416`), beside
`endOfTurnsThisTurn` (`:418`). The turn-start reset stays: after a cleanup it changes nothing, and a turn this port
enters without one (a state set mid-turn) starts from none.

`applyOneContinuousControl` reads `GainControl$` through `definedPlayers`, first player gains control
(`StaticAbilityContinuous.java:597-600`): `You` as before, `Player.isMonarch` (Fealty to the Realm) follows
`Game.monarch`, nobody when there is none. A spec `definedPlayers` cannot read is a pending error (it was a silent
skip).

## Smoke probe

`card-smoke -all -workers 8 -list`, same binary flags before and after:

| Measure | Before | After |
| --- | --- | --- |
| plays | 30,906 | 30,917 |
| refused | 2,712 | 2,701 |
| placed | 379 | 379 |
| inconclusive | 81 | 81 |
| hung | 0 | 0 |
| judged play | 91.9% | 92.0% |

Per-card lists: scratchpad `a250-smoke-before.txt`, `a250-smoke-after.txt`, diff `a250-smoke-diff.txt`.

| Flip | Cards | Why |
| --- | --- | --- |
| refused → plays | Choking Vines | `RememberTargets$` resolves |
| refused → plays | Churning Reservoir, Izzet Generatorium | `Count$CountersRemovedThisTurn` |
| refused → plays | Kiora of Salt and Sand, Laboratory Drudge, The Chain Veil | `Count$ThisTurnActivated_` |
| refused → plays | Telekinesis | `Count$ActivatedThisGame` on a static trigger |
| refused → plays | Antagonism, Spiny Starfish | `DamageToOppsThisTurn`, `Count$RegeneratedThisTurn` |
| refused → plays | Acorn Catapult, Chain of Plasma, Rhystic Lightning, Rakdos's Return, Pulse of the Forge | `Defined$ TargetedOrController`; Pulse's `Defined$ Parent` return is still refused when its condition holds (deferred) |
| cause changed | Chain Lightning | past `TargetedOrController` `UnlessPayer$`, now CopySpellAbility's `Defined$` |
| plays → refused | Ice Cauldron, Jeweled Amulet | `RememberCostMana$` loud; it was silently ignored |
| plays → refused | Volcanic Offering | its sub-ability `RememberTargets$` never reaches Java's remember (script defect); it destroyed one land |
| cause changed | Chef's Kiss | past `RememberTargets$`, now `TargetValidTargeting$` |

Without a flip (they already "played"): the 100 root `RememberTargets$` cards now remember their targets (Cloudshift,
Ephemerate, Journey to Nowhere, Sylvan Smite, ...; some still stop at another refusal), and Fealty to the Realm's
control applies. Pulse of the Forge's "plays" is a probe artifact: the probe's line never meets its condition, whose
`Defined$ Parent` return is still refused. Blaster Hulk's `Amount$ Count$CountersRemovedThisTurn ENERGY You` is an
inline amount `costModAmount` does not parse (only an SVar name), so its reduction is still skipped.

## Deferred, with the reason

| Head / shape | Cards | Why not here |
| --- | --- | --- |
| `Count$TotalManaSpent <valid>` on an activated ability | Forsworn Paladin | an activation's paying mana is not recorded |
| `SpellTargeted$CardManaCostLKI`, `Targeted$CastTotalManaSpent`, `TargetedByTarget$` | Reject Imperfection, Sound the Trumpets, Unravel, Bane's Contingency | targeted-spell LKI heads |
| `CastSA>Count$xPaid` for `Sac<X/…>` | Plumb the Forbidden | a cost's X is not the cast's `castX` |
| `Count$SearchedLibrary` | Archive Trap | ChangeZone's library search (`ChangeZoneEffect.java:1067-1089`) records nothing and fires no `SearchedLibrary` mode |
| `PlaneswalkedToThisTurn` | Unyaro | planechase record |
| `PromisedGift`, `Doubles`, `RepeatCheckSVar$` | Coiling Rebirth, Celebr-8000, Mana Clash, Sword of D&D | Gift casting, RollDice doubles, Repeat's own gate |
| inline compare operand, `NonTargetedController`, `HighestSpeed` | Spikeshell Harrier | needs a compile change for one card |
| `TriggeredAttackedTargetAndYou` in a trigger's `CheckSVar$` | Undercellar Sweep | Java passes the `Trigger` as ctb (`CardTraitBase.java:472`); `getDefinedPlayers`' `Triggered` branch needs a `SpellAbility` (`AbilityUtils.java:1013`), so what Java answers there is unclear |
| `Defined$ Parent` on ChangeZone `Origin$ Stack` | Pulse of the Forge (the probe's line never meets the condition) | returning the resolving spell from the stack (`getDefinedSpellAbilities`, `AbilityUtils.java:1232-1233`); refused when reached |
| `Game.costPaymentStack` | Professor Hojo, Tezzeret (Type$ Ability ReduceCost) | ability ReduceCost lines are not applied at all |

## Decisions

| Decision | Why |
| --- | --- |
| One remember site in `resolveTop`, not per effect | Java has one (`MagicStack.java:646`); per-effect reads would differ on declined optional triggers |
| Refuse a non-root `RememberTargets$` instead of mirroring Java's no-op | the card does not do what its text says in either engine; PORT-8 asks for the defect to surface |
| Record removals in `emitCounterChanged` | every change already emits there, after it happened; one site, as `Card.subtractCounter` is |
| Host snapshot before costs only when a cost moved the host | Java's old card object freezes at the move; costs that do not move it show on it |
| No `costPaymentStack` | no reader this port applies mid-payment; documented, not guessed |
| No new `PlayerController` method, no ADR | every piece mirrors a Java record or reads existing state |

## Forge defects (PORT-8)

| Where | Defect |
| --- | --- |
| `forge-gui/res/cardsfolder/v/volcanic_offering.txt:5` | `DBDestroyLand`'s `RememberTargets$` is on a sub-ability; `handleRemembering` only reads the root (`MagicStack.java:646`), so the opponent-chosen land is never destroyed |
| `forge-gui/res/cardsfolder/u/urza_academy_headmaster.txt` (`Destroy11U`) | a `GenericChoice` choice's `RememberTargets$` never runs, so "return each card put into a graveyard this way" returns nothing |
| `forge-gui/res/cardsfolder/l/lobotomy.txt:5` | `ForgetOtherTargets$` on the `ChooseCard` sub-ability is read nowhere (harmless: nothing remembers targets there) |

## Tests

`remembertargets_test.go` (Sylvan Smite and Cloudshift on the corpus card, add and forget, an illegal target dropped
first, no targeting, each refusal), `turnactivationrecords_test.go` (counter removals on cards and players with the
clamp, damaged loyalty, activations with the graveyard-cost LKI and next-turn clear, `ActivatedThisGame` for an
activated ability and a static trigger, `DamageToOppsThisTurn`, `RegeneratedThisTurn`, the upkeep reset at cleanup),
`targetedorcontroller_test.go`, `layer2control_test.go` (Fealty following the monarch, an unreadable spec loud).
Scenarios `remember-targets-journey-to-nowhere-returns-the-bears-when-destroyed`,
`checksvar-the-chain-veil-drains-only-after-a-turn-without-a-loyalty-activation`,
`layer2-fealty-to-the-realm-the-monarch-controls-the-enchanted-creature`.
