# Port Log — Game State: M5 batch T3, Sneak and Charm/copy link targets

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `sneak.go`, `castoptions.go`, `castspell.go`,
  `combat.go`, `block.go`, `valid.go`, `charmeffect.go`, `chaintargets.go`, `targeting.go`, `copyspellabilityeffect.go`,
  `ability.go`, `trigger.go`

Follows [`m5-kernel.md`](m5-kernel.md) (Sneak's timing row) and [`m5-control-2.md`](m5-control-2.md) (targets of a
sub-ability).

## Sneak (`sneak.go`)

`K:Sneak:<cost>` on 27 corpus cards (TMNT: the Turtles, Shredder, Karai, the `*_technique` sorceries). Java builds a
second spell (`CardFactoryUtil.java:3441-3467`): the first spell copied with its mana cost replaced by
`<cost> Return<1/Creature.attacking+unblocked/unblocked attacker>`, instant speed, `AlternativeCost.Sneak`.

| Piece | Java | Go |
| --- | --- | --- |
| Offer: hand only, declare blockers step, instant speed | `SpellAbilityRestriction.java:312-316` | `sneakAvailable`, one `castOption{sneak, hasAlt, flash}` beside the normal cast |
| Choice between normal cast and Sneak | separate `SpellAbility` | `chooseCastOption` (`ChooseOption`); a creature out of timing has only the Sneak option |
| Pay: mana first, then the return (`Cost.sort`) | `CostPartMana`, `CostReturn` | `payCastCost`, then `paySneakReturn` (`ChoosePermanentsToReturn`, `checkChoice`) |
| Returned attacker leaves combat | `GameAction.java:437-439` | `removeFromCombat` in `paySneakReturn`; `Move` itself still does not (`combat.go:84-94`) |
| Defender of the returned attacker | `getPaidList("Returned")` + combat LKI | `Card.sneakDefender`, read from `AttackTargets` before the return |
| Enters tapped | `PermanentEffect.java:33-35` | `permanentEffect` taps before `Move`, after `entryReplaced` |
| Creature attacks that defender, band unblocked | `PermanentEffect.java:55-63` | `sneakJoinsCombat`, before ETB triggers; no Attacks trigger (CR 508.4) |
| `Card.sneaked` | `CardProperty.java:1799-1806` | `Card.sneaked`, set by `castSpell`, cleared with `kicker` (failed cast, leaving stack/play) |
| `unblocked` | `CardProperty.java:1677-1680`, `Combat.java:941` | `Combat.isUnblocked` over the new `Combat.blockDecided` |

**New state.** `Card.sneaked`, `Card.sneakDefender` (values, copied by `Game.Clone`'s card copy). `Combat.blockDecided`:
Java's `AttackingBand.isBlocked` is a tri-state `Boolean`, null until `declareBlockersTurnBasedAction` sets it. Go's
`isBlocked` had no null; without it `unblocked` would hold during declare attackers. `DeclareCombatBlockers` marks every
attacker, `sneakJoinsCombat` its creature; `Combat.clone` copies it. Reason for a new field over reusing `activePhase`:
an attacker added after blockers (Sneak) is decided, one added another way (none ported) is not.

**Refused (GO-7).**

| Shape | Why |
| --- | --- |
| Third keyword field (`ValidAfterStack$`), X or non-mana cost | not offered; 0 corpus lines |
| Sneak on an Aura | not offered: `castAura` pays no return; 0 corpus Auras |
| Sneak granted off the hand (Ninja Teen's graveyard grant) | not offered: only `fromHand`; Ninja Teen's `K:Class` and `MayPlay ValidSA$ Spell.Sneak` refuse anyway |
| Sneaked creature whose defender left (player lost, PW/battle gone) | resolution error before it moves: Java keeps it attacking the CR 506.4c `<Nothing>` card, unmodelled |
| Return answer naming no unblocked attacker | pending error, cast fails (mana already paid, as for every post-mana cost part) |

Deviation: a copy of a sneaked permanent spell (`copySpell`) is a new card with `sneaked` false; Java's copied
`SpellAbility` keeps `AlternativeCost.Sneak`. No corpus card copies a creature spell during declare blockers.

Deviation: under a `MayPlay$` grant off the hand (Leonardo, Sewer Samurai's graveyard cast), Java would also offer the
grant's Sneak spell; here Sneak is offered from the hand only.

Not reached: The Last Ronin's Technique reads `Count$ValidStack Card.Self+sneaked`; no `ValidStack` amount head exists,
so its `Branch` fails on that amount, not on Sneak.

Scenarios: `sneak-leonardo-big-brother-enters-tapped-and-attacking-unblocked`,
`sneak-leonardo-leader-in-blue-sneaked-trigger-pumps-the-team` (`Card.Self+sneaked`),
`sneak-donatellos-technique-sorcery-at-instant-speed-in-declare-blockers`,
`driver-sneak-cast-in-declare-blockers-priority-attacks` (the turn driver's priority round). Module tests:
`sneakcasting_test.go` (wrong step either side, blocked attacker, unread keyword shapes, instant choice, bad return
answer, defender gone, planeswalker defender, noncreature, Clone).

## Charm modes and copies: their links' own targets

Java chains each chosen mode, with its own `SubAbility$` chain, onto the Charm's tail (`CharmEffect.java:293-317`), so
`setupTargets` (`SpellAbility.java:2141-2171`), `hasFizzled` (`MagicStack.java:745-751`), `getAllTargetChoices`
(`MagicStack.java:410`) and a copy's `setupNewTargets` (`SpellAbility.java:2120-2139`) all walk mode links too.

| Site | Go |
| --- | --- |
| Choosing (cast, trigger, activate) | `chooseCharmModes`: `resolveTargets` then `resolveChainTargets` per mode, in chain order |
| Link context | `resolveChainTargets`' link carries `triggered`, `TriggerRemembered`, `casting`, `charmRoot` of its root |
| CR 608.2b re-check | `dropIllegalChainTargets` for the head and each mode |
| Stamps | `stampChainTargets` for the head and each mode |
| BecomesTarget / Ward | `allTargetsOf` (moved to `ability.go`) adds links; `pushTriggeredAbilities` now uses it too |
| Copy | `copySpell` deep-copies links; `MayChooseTarget$` asks per link (`chooseCopyChainTargets`) |

Aliasing fix: `copySpell`'s `cp := orig` and `Game.Clone`'s shallow stack copy share `Modes` and `chainTargets` arrays;
`stampTargets` wrote stamps through them into the original. `stampTargets` and `dropIllegalTargets` now copy before
writing (`clonedChainTargets`), so Clone stays allocation-free for them.

Pre-existing gap closed on the way: a triggered or activated Charm's mode targets never reached BecomesTarget/Ward
(`trigger.go` read `Targets` only).

Still refused: `ChangeTargets` on an item whose head or mode has link targets (`stackItemTargets`).

Scenario: `charm-inscription-of-abundance-fight-mode-link-chooses-its-own-target` (new fixture verb `queue modechoice`).
Module tests: `charmlinktargets_test.go`.
