# Port Log — Game State: DealDamage `DamageSource$` (R9)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `dealdamageeffect.go` (`damageSources`,
  `dealDamageTargets`, `recordCardDamage`, `excessDamageValue`, `rememberDamaged`), `damageresolveeffect.go`
  (`dealPendingDamage`), `defined.go` (`ParentTarget`, `TriggeredSource`, `ThisTargetedController`,
  `ImprintedController`/`ImprintedOwner`), `amountcontext.go` (`ParentTargeted$`), `ability.go`
  (`Ability.ancestorTargeting`, `Ability.uniqueTargets`), `subability.go` (`linkUsesTargeting`), `chaintargets.go`,
  `targeting.go` (`targetChoiceFor`'s `TargetUnique$`)
- **Java:** `DamageDealEffect.java:131-302`; `GameAction.java:2723-2808` (`dealDamage`, `RememberDamaged$` at
  `:2787-2794`); `AbilityUtils.java:65-329` (`getDefinedCards`), `:688-693` (`ParentTargeted$`), `:755-769`
  (`getDefinedEntities`), `:976-982` (`ThisTargetedController`); `SpellAbilityEffect.java:374-383`
  (`getTargetEntities`); `SpellAbility.java:1401`, `:1995-2044` (`getParentTargetingCard`, `getUniqueTargets`);
  `Card.java:6127-6151` (`getExcessDamageValue`); `CardUtil.java:149-153` (`getRadiance`); `MagicStack.java:734-782`

## What `DamageSource$` refused

104 smoke-probe cards were refused with `DealDamage: DamageSource$ not resolvable yet` (89 at `resolve`, 14 at `turns`,
1 at `activate`). Values on their lines, and corpus-wide over all 190 real `DealDamage` lines naming it:

| Value                                                                                                           | Refused cards | Corpus lines | Status                             |
| --------------------------------------------------------------------------------------------------------------- | ------------- | ------------ | ---------------------------------- |
| `ParentTarget` (Rabid Bite, the "bite" shape)                                                                   | 70            | 88           | Resolved                           |
| `Targeted` (Justice Strike, Stalking Yeti)                                                                      | 15            | 26           | Resolved                           |
| `TriggeredCard`/`...LKICopy`, `TriggeredAttacker...`                                                            | 8             | 31           | Resolved through `definedCards`    |
| `Enchanted`, `Imprinted`, `Remembered`, `ChosenCard`, `Self`, `ThisTargetedCard`, `DelayTriggerRemembered[LKI]` | 9             | 18           | Resolved                           |
| `TriggeredSource`                                                                                               | 1             | 4            | Resolved (new `definedCards` case) |
| `Valid <spec>`                                                                                                  | 2             | 4            | Resolved (battlefield scan)        |
| `EffectSource`, `OriginalHost`, `TriggeredTarget[LKICopy]`, `Spawner>...`                                       | 0             | 20           | Error naming the value             |

## Every source deals its own damage

`DamageDealEffect.resolve` resolves the sources first (`:135-138`: none left, no damage), then the targets, then puts
every source's damage to every target in one `CardDamageTable` and deals it as one batch (`:217-271`).
`dealDamageEffect` does the same into a `pendingDamage`: the `DamageMap$` chain's shared one, or a local one dealt by
`dealPendingDamage` (also `DamageResolve`'s loop now). `dealPermanentDamage`/`dealPlayerDamage` read lifelink, infect,
wither and every `ValidSource$` prevention off the source `CardID` they get, and deathtouch is read per source, so a
bite has the biter's keywords, not the spell's. A pair named twice sums, as the Guava table does.

| Piece               | Does                                                                                               | Why                                                                                     |
| ------------------- | -------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| `damageSources`     | `DamageSource$` through `definedCards`; `Valid <spec>` scans the battlefield; absent is the host   | `getDefinedCards`, `:287-303` for `Valid`                                               |
| `dealDamageTargets` | Targets if `ValidTgts$`, else each `" & "` part of `Defined$` (default `Self`), players then cards | `getTargetEntities`; a card `Defined$` (Swooping Pteranodon) was silently dealt nothing |
| liveness            | A card target off the battlefield or phased out is skipped, `Defined$` ones included               | `:253-256`; no timestamp check for a `Defined$` card (not stamped here)                 |
| Radiance            | Applies on the `Defined$` path too                                                                 | `getRadiance` walks to the targeting ancestor (`CardUtil.java:150`)                     |

The source is read as it is now, not as an LKI copy (`sourceLKI`, `:218`): a battlefield source is the same either way,
and a `ParentTarget`/`Targeted` source that left was already dropped under CR 608.2b. `ReplacementType.AssignDealDamage`
(`:140-143`) is not run, as before for the host.

## An empty `ParentTarget` is not always an error

Rabid Bite whose biter is removed in response: `dropIllegalTargets` empties the `Pump` root's targets, the link's target
is still legal, and Java's `getParentTargetingCard` finds no ancestor targeting a card, so no damage
(`MagicStack.java:744-779`). `definedCards` refused any empty `parentTargets`, since "the ancestor chose none" and "this
port carries no ancestor" looked the same. `Ability.ancestorTargeting` (set by `resolveSubAbility` and
`resolveChainTargets`: some ancestor names `ValidTgts$`) tells them apart: with it, an empty `ParentTarget` and an empty
`ParentTargeted$` are Java's empty answer; without it, still an error. This also unblocked three `Attach`
`Object$ ParentTarget` cards (Barret, Raubahn, Thorin).

## `ParentTargeted$` amounts

`ParentTargeted$CardPower` (Rabid Bite's `X`) is a context head now (`abilityListIDs`, `AbilityUtils.java:688-693`): the
card entries of `Ability.parentTargets`. Inline in `NumDmg$` (Bite Down on Crime, Cosmic Hunger, Throw from the Saddle)
it stays unresolved: `resolveNamedAmount` reads SVar names, not inline expressions (R9's amounts batch).

## `TargetUnique$` on chain links

Fall of the Hammer's "another target creature" is `TargetUnique$` on the `DealDamage` link: `canTarget` rejects what any
ancestor targets (`SpellAbility.java:1401`, `getUniqueTargets` `:2034-2044`). `targeting.go` skipped it on the reasoning
that links were never targeted separately; `chaintargets.go` targets them now, so a biter could bite itself.
`resolveChainTargets` hands each link every target chosen before it (`Ability.uniqueTargets`, rebuilt, never appended in
place), and `targetChoiceFor` drops those candidates. A root has no ancestors: no change there.

## Params `DealDamage` read silently, now ported or refused

Unblocking 104 cards exposed params no Go code read. Each is ported or refused by name now (PORT-8/GO-7):

| Param                                                            | Lines  | Status                                                                                |
| ---------------------------------------------------------------- | ------ | ------------------------------------------------------------------------------------- |
| `ExcessDamage$`/`ExcessDamageCondition$`                         | 6/1    | Ported: lethal to the card, the rest to the first entity named (`internalDamageDeal`) |
| `RememberDamaged$`                                               | 34     | Ported: the host remembers what its own damage reached; refused under `DamageMap$`    |
| `OptionalDecider$`                                               | 5      | Ported: `ConfirmEffect` by the first player named (`:180-185`)                        |
| `ReplaceDyingDefined$`/`ReplaceDyingCondition$`                  | 44/1   | Ported (R9e, [`m6-r9e-replace-dying-excess.md`](m6-r9e-replace-dying-excess.md))      |
| `ExcessSVar$`/`ExcessSVarCondition$`                             | 22/7   | Ported (R9e); refused under `DamageMap$`                                              |
| `RelativeTarget$`                                                | 2      | Refused: `Defined$` re-read per source (`:243-245`)                                   |
| `DivideEvenly$`, `DividerOnResolution$`                          | 1/1    | Refused: an allocation this port does not carry                                       |
| `CardChoices$`/`PlayerChoices$`/`Random$`                        | 4/4/3  | Refused: a choice outside targets (`:150-176`)                                        |
| `RememberAmount$`, `RememberRedirectedExcess$`, `NoRedirection$` | 1 each | Refused                                                                               |
| `ValidTgts$` naming `NotDefined<X>`                              | 1      | Refused: `Matches` has no ability context and would read false (Special Move)         |

A fully prevented entity is not remembered: Java's `runReplaceDamage` removes it from the table
(`ReplacementHandler.java:526-527`). One reduced to 0 by an `Updated` replacement stays in Java's table and is not
remembered here.

## Smoke probe

`crucible card-smoke -all -workers 8`, before → after:

| Status       | Before | After  |
| ------------ | ------ | ------ |
| plays        | 29,521 | 29,610 |
| refused      | 4,001  | 3,911  |
| inconclusive | 198    | 198    |
| judged play  | 88.1%  | 88.3%  |

| Flip                    | Cards | Cause                                                                                                                                           |
| ----------------------- | ----- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| refused → plays         | 129   | 87 `DamageSource$`; 42 more: `TriggeredSource` (Aurification, Dread, ...), card `Defined$`, `ParentTarget`/`ParentTargeted$` after zero targets |
| refused, reason changed | 42    | 17 `DamageSource$` cards now stop at a refused param or an unresolved amount; 25 reach a newly refused param or `Defined$` first                |
| plays → refused         | 40    | 34 `ReplaceDyingDefined$`, 4 `ExcessSVar$`: silent misses made loud; Shellshock (below); Monstrous Step (below)                                 |
| refused → placed        | 1     | Nature Demands an Offering (judged 33,522 → 33,521)                                                                                             |

- Shellshock: `RememberDamaged$` now fills the host's list, and its `Token` link's `TokenAmount$ Y`
  (`Remembered$Valid Creature`) refuses over a non-empty list; it used to read 0 over the empty one and make no Mutagen.
- Monstrous Step: its `MustBlock` link names `TargetUnique$`, so the probe can no longer pick the pumped creature as its
  own blocker; the forced blocker is real, and the probe's block declarer does not satisfy block requirements (the lure
  pumps' cause in `m6-r9-pump-kw.md`).

Every `DealDamage` resolution that dealt nothing was listed once with a temporary error (a whole-corpus run, then
reverted): 9 cards, each a Java-empty answer -- a target sacrificed as the cost (Fiery Conclusion, Final Flare, Fodder
Cannon, Elite Headhunter), `TargetMin$ 0` with none chosen (Batroc the Leaper, Fight with Fire), or a choice the probe
board never made (`ChosenCard`: Purging Scythe, Trial of Agony; `ChosenPlayer`: Saskia the Unyielding). None of the
`DamageSource$` bite cards.

## Not ported

| Shape                                                                    | Reason                                               |
| ------------------------------------------------------------------------ | ---------------------------------------------------- |
| `DamageSource$ EffectSource`/`OriginalHost`/`TriggeredTarget`/`Spawner>` | No `definedCards` case; error names the value        |
| `DamageSource$ Valid Card.IsCommander+YouCtrl` (1 line)                  | `Matches` has no `IsCommander` case; refused by name |
| Source LKI                                                               | Source read as it is now (above)                     |
| `AssignDealDamage` replacements                                          | Not run for any source                               |
| Inline `NumDmg$ ParentTargeted$CardPower`, `Enchanted$CardPower`         | Amount resolver gaps, R9's amounts batch             |

Found while testing, not this batch's to fix: protection's damage half (CR 702.16e) is not modeled anywhere in the
engine. A 3/3 blue biter deals full damage to a creature with `Protection from blue` (combat damage likewise); only
protection's targeting, blocking and attaching halves are read (`staticability.go`). The source-based test uses a
`ValidSource$` prevention replacement instead.

## Tests

`damagesource_test.go` (bite with the biter's lifelink and as the `DamageDealt` source, `ValidSource$` prevention
against the source, an infect target damaging itself, no parent target left, no targeting ancestor, `Valid` sources,
empty source list, Stalking Yeti's shape, card `Defined$` and the `Self` default, a `Defined$` card off the battlefield,
`TriggeredSource`, `RememberDamaged$`, `ExcessDamage$` on a creature, planeswalker, artifact, with a deathtouch source
and an unmet condition, `OptionalDecider$`, a `TargetUnique$` link, Self-Destruct's `DamageMap$` chain,
`ImprintedController`, every refused param), `dealdamageeffect_test.go`, `radiance_test.go`. Scenarios
`damage-source-rabid-bite-nighthawk-deals-deathtouch-lifelink-damage`,
`damage-source-rabid-bite-bolted-biter-deals-no-damage`,
`damage-source-justice-strike-deathtouch-recluse-destroys-itself`,
`damage-source-ram-through-trample-excess-hits-the-controller`.
