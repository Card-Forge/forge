# Commander-cast counts and the triggering attacker list (R9c)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `castrecord.go` (`commanderCasts`),
  `amountheads.go` (`Count$` heads), `amountplayers.go` (`playerXCount`), `trigger.go` (`attackersDeclaredObjects`,
  `checkBlocksTriggers`), `ability.go` (`triggeredObjects.attackers`), `defined.go` (`TriggeredAttackers`),
  `amountcontext.go` (`TriggerObjectsAttackers`)
- **Java:** `Player.getTotalCommanderCast` / `getCommanderCast` (`Player.java:2875-2900`), the increment
  `MagicStack.java:346-349`, commander tax `CostAdjustment.java:57-61`, `Player.addCommander` (`:2817-2826`),
  `createCommanderEffect` (`:3226-3276`), `GameState.java:1377` (`|IsCommander`); `AbilityUtils.xCount` (`:2363-2369`),
  `playerXCount` (`:3366-3372`), `calculateAmount`'s `TriggerObjects` list (`:697-701`);
  `TriggerAttackersDeclared.setTriggeringObjects` (`:79-98`), `TriggerBlocks.java:71`, `PhaseHandler.java:628-644`,
  `:771`

## What refused

`card-smoke -all` refused 12 cards on a commander-cast count (`Count$TotalCommanderCastFromCommandZone`,
`TargetedPlayer$TotalCommanderCastFromCommandZone`) and 18 on the attacker list a trigger never recorded: 8 on
`Defined$ TriggeredAttackers` ("`Defined$ "_" not resolvable yet`"), 10 on a `TriggerObjectsAttackers$` amount.

| Key | Cards | Modes (corpus) |
| --- | --- | --- |
| `Count$TotalCommanderCastFromCommandZone` | 11 | Spell-cast triggers (the Storm cycle), ETB (Jirina Kudro, The Swarmlord, Jeska), activated, statics |
| `TargetedPlayer$TotalCommander...` | 1 | Commander's Insight |
| `Defined$ TriggeredAttackers` | 8 | `AttackersDeclared` (Lightmine Field, Vrestin, Tyvar), `AttackersDeclaredOneTarget` (Yuriko, ...) |
| `TriggerObjectsAttackers$<measure>` | 10 | `AttackersDeclared` (The Ur-Dragon, Varina, Radha, Witch-king), with `Amount`, `CardPower`, ... |

## Commander casts: 0, the answer for a game without commanders

Java counts per player how many times they cast one of their own commanders from the Command zone
(`MagicStack.java:346-349`: `source.isCommander()`, cast from `ZoneType.Command`, `owner == activator`); commander tax
reads the same record. `Card.isCommander` is set only by `Player.addCommander`, which the Commander game setup and the
`GameState` card option `|IsCommander` call. This port has neither: the fixture loader reports `|IsCommander` unapplied
(`TestLoadReportsIsCommanderUnapplied`), the engine `Card` has no commander field, and nothing seats a deck's
`[Commander]` section (`internal/deck` parses it; its one consumer, `card-smoke`, reads card names only,
`cmd/crucible/smoke.go:101`). So no card in an engine game is a commander and every record is empty. The count is 0,
which is Java's own answer in a game without commanders, not a guess.

| Decision | Why |
| --- | --- |
| No per-player counter, no `IsCommander` designation | An increment needs a commander, and a commander cast from the Command zone needs the Commander Effect (`createCommanderEffect`: the `MayPlay$` grant from Command, the CR 903.9 move replacement, and with it commander tax). Without them an increment is unreachable code, and a designation without the effect plays a game Java never plays (no tax) |
| One reader, `Game.commanderCasts`, answering 0 | Its doc names the invariant and what replaces it: a Commander setup must swap in the record it keeps. `Count$`, `PlayerCount...$`/`TargetedPlayer$` (`playerXCount`) and `Count$CommanderCastFromCommandZone` (Opal Palace) all read it |
| `playerXCount` sums per player, then applies the operator | `AbilityUtils.java:3366-3372`: `doXMath(totCast, m)`, so `PlayerCountPlayers$TotalCommander.../Plus.3` is 3 |
| `IsCommander` valid property unchanged | `DamageSource$` (`dealdamageeffect.go:223`) and Play's `Valid$` (`playeffect.go:80`) refuse it by name, as before. Elsewhere `Matches` has no `IsCommander` case: it falls through to the type check (`valid.go:722`) and reads false, which in a game without commanders is Java's answer too. Not touched by this row |

## Attackers: recorded where Java's mode sets them

| Mode | Recorded `Attackers` | Java |
| --- | --- | --- |
| `AttackersDeclared` | every declared attacker, narrowed by `ValidAttackers$`, declaration order | `TriggerAttackersDeclared.java:82-91`, `PhaseHandler:641` |
| `AttackersDeclaredOneTarget` | the one defender's attackers, narrowed the same way | same class, `PhaseHandler.java:633` |
| `Blocks` | the attacker the blocker blocks (each blocker blocks one here, `checkBlocksTriggers`) | `TriggerBlocks.java:71`, `PhaseHandler.java:771` |
| Readers | `Defined$ TriggeredAttackers` (`definedCards`, so `DefinedCards$`, `RememberObjects$` too); `TriggerObjectsAttackers$<measure>` through `handlePaid` | `AbilityUtils.java:697-701` |

| Decision | Why |
| --- | --- |
| `attackersRecorded` flag beside the list | A recorded list may be empty (`ValidAttackersAmount$ EQ0`) and reads 0; an unrecorded key stays the loud "the trigger recorded no attackers" / unresolved amount (GO-7). Java's `TriggerObjects` reader defaults a missing key to an empty list, which this port does not guess at |
| A fresh slice per trigger | `slices.Clone` of combat's list: the recorded list never aliases `Combat.Attackers`, and is never edited in place, so `Game.Clone`'s shallow copy of a stacked `Ability` stays safe |
| `TriggerObjectsAttackers$` reads last-known information | Java's list holds the battlefield `Card` object, which a move off the battlefield replaces with a copy (`GameAction.java:207`), so a dead attacker's `CardPower` is its last power |
| `AttackedTarget` recorded beside it | [`m6-r9g-attack-triggers.md`](m6-r9g-attack-triggers.md) |

No new `Game`/`Player` state, no `PlayerController` method, no `Game.Clone` change: `attackers`/`attackersRecorded` are
`triggeredObjects` fields, copied by value with the `Ability`.

## Related

`Mode$ BlockersDeclared`/`AttackerBlockedOnce`, the
`TriggeredAttackers`/`NotDefinedTriggeredAttackers`/`attacking TriggeredAttackedTarget` valid properties and amounts
read while targets are chosen (Tide of War, Jin Sakai, Squall, Case of the Market Melee):
[`m6-r9g-attack-triggers.md`](m6-r9g-attack-triggers.md).

## Smoke probe

`crucible card-smoke -all -workers 8`, before → after:

| Status | Before | After |
| --- | --- | --- |
| plays | 30,513 | 30,542 |
| refused | 2,999 | 2,970 |
| placed | 372 | 372 |
| inconclusive | 194 | 194 |
| judged | 33,512 | 33,512 |
| judged play | 91.1% | 91.1% |

29 refused → plays, no plays → refused:

| Gap closed | Cards |
| --- | --- |
| Commander casts | 11: Captain Vargus Wrath, Commander's Insight, Echo Storm, Empyrial Storm, Fury Storm, Genesis Storm, Hatut Zeraze Strike Force, Jeska Thrice Reborn, Jirina Kudro, Jyoti Moag Ancient, The Swarmlord |
| `Defined$` | 8: Frenzied Gorespawn, Frontier Warmonger, Jiang Yanggu Alone, Jin Sakai Ghost of Tsushima, Lightmine Field, Tyvar the Bellicose, Vrestin Menoptra Leader, Yuriko Blade of the Mighty |
| Attacker amount | 10: Choco Seeker of Paradise, Fangorn Tree Shepherd, Grand Warlord Radha, Rabble Rousing, Screaming Swarm, The Earth King, The Ur-Dragon, The Ur-Sphinx, Varina Lich Queen, Witch-king Sky Scourge |
| Still refused | Skull Storm: past the count, now `RepeatEach: RepeatPlayers$: engine: Defined$ "_" not resolvable yet` |

## Tests

`triggerattackers_test.go` (`package engine_test`): `AttackersDeclared` records only the attackers (counters on them,
none on the creature at home, `TriggerObjectsAttackers$Amount`); `ValidAttackers$` narrowing under `Amount`, `/Twice`,
`CardPower`, `GreatestCardPower`, `Valid`; a recorded empty list reads 0 with its operator;
`AttackersDeclaredOneTarget`; `Blocks` names the blocked attacker; an unrecorded list stays an error through both
readers. `commandercasts_test.go`: `Count$`, `Count$CommanderCastFromCommandZone`, `PlayerCountPlayers$` (operator after
the sum) and Commander's Insight's `TargetedPlayer$` chain. `TestLoadReportsIsCommanderUnapplied` (`internal/fixture`)
pins the invariant the 0 rests on. Scenarios: `trigger-attackers-declared-lightmine-field-damages-each-attacker`,
`trigger-attackers-declared-vrestin-counters-only-the-attacking-insects`,
`commander-casts-jirina-kudro-makes-no-tokens-without-a-commander`.
