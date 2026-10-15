# Port Log — Game State: PutCounter `TargetMin$`/`TargetMax$` and `DividedAsYouChoose$` (R9)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `putcountereffect.go`, `dividedtargets.go`
  (`capDividedChoice`, `divideAmongTargets`, `checkDivision`, `Ability.dividedShareOf`), `ability.go`
  (`Ability.divided`, `chainTarget.Divided`, `dividedShare`), `targeting.go` (`resolveTargets`, `targetChoiceFor`,
  `clonedChainTargets`), `chaintargets.go`, `subability.go`, `copyspellabilityeffect.go` (`chooseCopyTargets`),
  `control.go` (`PlayerController.DivideAmongTargets`, `ScriptedController.QueueDivision`);
  [`internal/smoke`](../../../../../crucible/internal/smoke) `controller.go`;
  [`internal/fixture`](../../../../../crucible/internal/fixture) `actions.go` (`queue divide`, `queue targets none`)
- **Java:** `CountersPutEffect.java:170-573` (`resolvePerType`; `:244` targets, `:460-461` divided share), `:575-661`;
  `SpellAbilityEffect.java:376-383` (`getDefinedEntitiesOrTargeted`, Defined$ first);
  `PlayerControllerHuman.java:2486-2527` (division announced after the targets); `SpellAbility.java:1822-1846`,
  `:2182-2188` (`dividedValue`); `TargetChoices.java:51`, `:71-82`, `:153-171` (`dividedMap`);
  `TargetRestrictions.java:370-376`; `InputSelectTargets.java:438-439`; `TargetSelection.java:84`, `:166-174`

## What `TargetMin$` refused

`PutCounter` listed `TargetMin$`/`TargetMax$` among its unresolved params, so every "up to N" or "each of N" line
failed. `crucible card-smoke -all` refused 138 cards with `PutCounter: TargetMin$ not resolvable yet` (86 at `resolve`,
29 at `activate`, 23 at `turns`). Shapes, as found by dropping the two keys and reading the next refusal:

| Shape                                                       | Cards | Status                                                                                                                                                                                                              |
| ----------------------------------------------------------- | ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Each target gets `CounterNum$` (Gird for Battle, Basri Ket) | 100   | Resolved: the targets were already chosen by `resolveTargets`                                                                                                                                                       |
| `DividedAsYouChoose$` over the line's own targets           | 28    | Resolved (below); 25 play, Bounty of the Hunt stops at `RemovePhase$`, Glint Weaver at its `GainLife` amount, Lathiel at `DividedUpTo$`                                                                             |
| Another refused param or amount on the line or its chain    | 9     | Argothian Uprooting, Kaya the Inexorable (`Animate: Triggers$`), Elspeth Resplendent (`CounterTypes$`), Orcish Catapult (`DividedRandomly$`), Jenova, The Caves of Androzani, Liege of the Tangle, Olinda, Turf War |
| Sky Deck (an Attraction)                                    | 1     | `placed` now                                                                                                                                                                                                        |

## Each target gets `CounterNum$`

`CountersPutEffect.resolvePerType` walks `getDefinedEntitiesOrTargeted` (`:244`) and puts `counterAmount` on each
(`:540`). The targets are chosen as the ability goes on the stack: `targetChoiceFor` already reads
`TargetMin$`/`TargetMax$` (named SVars included: Hapatra's `TargetMax$ OneEach`, Chong and Lily's `X`), and
`TargetMin$ 0` with no candidate pushes the ability with none (R9 batch 2). An "up to" choice of none puts nothing; the
`SubAbility$` chain still runs.

`Defined$` beside `ValidTgts$` is read first (`definedFirst`, `SpellAbilityEffect.java:379`), each `" & "` part through
`definedCounterTargets`: Drillworks Mole's `Defined$ Self & Targeted`. The effect read the targets whenever `ValidTgts$`
was named; no card that played before changed (whole-corpus diff, below).

`PutCounter` now fails on `Ability.targetsErr`: a target shape `resolveTargets` could not ask about (or a bad division)
used to resolve over no targets, silently.

## `DividedAsYouChoose$`: the division is announced with the targets

CR 601.2d: the player announces the division right after choosing targets, each target getting at least one. Java's
human controller does this in `chooseTargetsFor` (`PlayerControllerHuman.java:2486-2527`) and stores it in
`TargetChoices.dividedMap`; `CountersPutEffect` reads each target's share at resolution (`:460-461`), `CounterNum$`
unread. Crucible had nowhere to carry a division, so the shape was refused (here and in `DealDamage`).

| Piece                                      | Does                                                                                                                                                                                                                                                                                                                     | Why                                                                                    |
| ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------- |
| `capDividedChoice`                         | `TargetMax$` capped at the `DividedAsYouChoose$` total, `TargetMin$` at the cap; a total of 0 asks for no target                                                                                                                                                                                                         | `InputSelectTargets.java:438-439`, `TargetRestrictions.java:370-376`                   |
| `divideAmongTargets`                       | Right after `ChooseTargets`: one target takes all, as many targets as the amount get one each, otherwise `DivideAmongTargets` is asked                                                                                                                                                                                   | `PlayerControllerHuman.java:2495-2526`; total evaluated at targeting (`:2182-2188`)    |
| `checkDivision`                            | One share per target, each at least one, summing to the total; else `targetsErr`, the spell fails at resolution and places nothing                                                                                                                                                                                       | GO-7; Java's GUI enforces the same (`assignGenericAmount`, `atLeastOne`)               |
| `Ability.divided`, `chainTarget.Divided`   | Shares keyed by target, never by position; replaced, never written in place                                                                                                                                                                                                                                              | `dropIllegalTargets` rebuilds `Targets`; `Game.Clone` and `copySpell` share the slices |
| `resolveChainTargets`, `resolveSubAbility` | A link announces its own division; its child `Ability` carries it (`chainTargetsFor`). A link whose targets or division could not be asked records its own error (`chainTarget.Err`, the child's `targetsErr`) and the root's (`failLinkTargets`), so the whole ability fails in `Registry.resolve` before the root acts | Java walks every targeting link (`chooseTargetsFor`'s loop)                            |

A target removed under CR 608.2b takes its share with it (`TargetChoices.java:71-82`): the one left keeps its own share,
not the whole amount. Charm modes go through `resolveTargets` too (Abzan Charm).

The mechanism is not `PutCounter`'s own: any ability naming `DividedAsYouChoose$` now announces its division.
`DealDamage` and `PreventDamage` read `dividedShareOf` per target
([`m6-r9-dealdamage-divided.md`](m6-r9-dealdamage-divided.md)).

### New controller decision

`PlayerController.DivideAmongTargets(g, decider, source, targets, amount) []int`, Java's `assignGenericAmount` with
`atLeastOne`. No existing decision fits: `ChooseNumber` asks one number, and asking it per target would be a decision
loop Java does not have. `ScriptedController.QueueDivision`, fixture verb `queue divide <n>,<n>`. The smoke controller
gives every target one and the rest to the first.

## Refused, by name

| Shape                                                                | Reason                                                                                                                                                |
| -------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| `DividedUpTo$` (Lathiel, 1 refused card)                             | A further "how much of it" `chooseNumber` before dividing (`PlayerControllerHuman.java:2492`)                                                         |
| `DividedAsYouChoose$` with `Defined$`, or with no `ValidTgts$`       | Untargeted, Java divides card by card at resolution (`CountersPutEffect.java:470-483`, `Choices$`)                                                    |
| New targets for a divided copy (`MayChooseTarget$`, copier says yes) | Java reassigns the original's division values to the new targets (`PlayerControllerHuman.java:2549`); a copy keeping its targets keeps their division |
| `DividedRandomly$`, `RemovePhase$`, `CounterTypes$`                  | Unchanged: each its own mechanic                                                                                                                      |

## Smoke probe

`crucible card-smoke -all -workers 8`, before → after:

| Status       | Before | After  |
| ------------ | ------ | ------ |
| plays        | 29,610 | 29,735 |
| refused      | 3,911  | 3,785  |
| placed       | 359    | 360    |
| inconclusive | 198    | 198    |
| judged play  | 88.3%  | 88.7%  |

All 138 changed cards are the refused ones: 125 refused → plays, 12 refused for another reason (above), 1 refused →
placed (Sky Deck; judged 33,521 → 33,520). No card went plays → refused.

A temporary whole-corpus run (reverted) errored any `PutCounter` whose targets could not be read or that resolved over
no target. No `TargetMin$`/`TargetMax$` value was unreadable. 16 of the 125 resolved over no target, each a legal "up
to" choice with no candidate on the probe's board: Arni Slays the Troll, Chong and Lily, Cleopatra, Crawl from the
Cellar, Cryogen Relic, Huatli, Katsumasa, Kaya Geist Hunter, Kenku Artificer (and A-), River Heralds' Boon, Spoils of
War, Sweet-Gum Recluse, The Caves of Androzani, Tyvar Kell (and A-). Their counters are unexercised by the probe, not
refused.

## Tests

`countersamongtargets_test.go`: each of up to two targets, "up to one" choosing none with its chain still running,
`Defined$ Self & Targeted`, an unreadable `Defined$` part, Hapatra's `TargetsForEachPlayer$` with a named `TargetMax$`,
an asked division, both forced divisions, every bad answer (sum, a zero share, share count, more targets than the
amount), a dropped target's share, a link's own division and its bad answer (under a `PutCounter` root and a `GainLife`
one), a total of 0, `DividedUpTo$`, an unreadable total, `Defined$`/untargeted division, a link that could not be asked
(`DividedUpTo$`, an unreadable total) under a `GainLife` root, a pushed ability with no division, and a divided copy
keeping its targets and choosing new ones. Scenarios `putcounter-gird-for-battle-counters-each-of-two-targets`,
`putcounter-cost-of-brilliance-up-to-one-link-chooses-no-creature`,
`putcounter-defend-the-celestus-divides-three-counters-two-and-one`,
`putcounter-defend-the-celestus-bolted-target-takes-its-share-with-it`,
`putcounter-abzan-charm-mode-puts-both-counters-on-its-one-target`.
