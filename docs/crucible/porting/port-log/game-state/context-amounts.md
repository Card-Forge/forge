# Port Log — Game State: ability-context amounts

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `amountcontext.go`, `amount.go`
  (`expressionValue`), `effect.go` (`Game.resolving`), `chosencosts.go` (`Ability.paid`), `trigger.go` (`card` of an
  enters trigger)

## `Targeted$`, `TriggeredCard$`, `Remembered$` and the rest

`AbilityUtils.calculateAmount` (`AbilityUtils.java:640-715`) reads an SVar written `<Object>$<measure>` by building a
list of cards from the resolving ability and measuring it (`handlePaid`). About 1,100 corpus uses: `Remembered$` (~500),
`Targeted$` (~330), `TriggeredCard$` (~220), `Sacrificed$` (~130), `Imprinted$`, `TriggeredAttacker$`,
`TriggeredBlocker$`, `Exiled$`, `Discarded$`.

`Registry.resolve` sets `Game.resolving` to the ability it runs and restores the previous one afterwards, so a
sub-ability reads its own (the chain copies targets, triggering objects and paid cards). `opAwareValue`
(`amountplayers.go`) sends the list heads to `listHeadCards` and measures them with `handlePaid` (`amountpaid.go`,
[`m6-r9-amounts.md`](m6-r9-amounts.md)); the heads in `contextHeads` are unresolved (never 0) outside a resolution:

| Head                                | Cards                                                                                                          |
| ----------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| `Targeted`                          | the card targets of the ability                                                                                |
| `TriggeredCard`                     | the card the trigger recorded: the one that entered (`checkETBTriggers`), died, was discarded, tapped for mana |
| `TriggeredAttacker`/`Blocker`       | the attacker or blocker an attack or block trigger recorded                                                    |
| `TriggeredSource`                   | the card source a damage trigger recorded (none for a player source)                                           |
| `TriggerRemembered`                 | the cards a delayed or reflexive trigger remembered                                                            |
| `Remembered`, `Imprinted`           | the host's `Memory` lists                                                                                      |
| `Sacrificed`, `Exiled`, `Discarded` | the cards the cost used up (`Ability.paid`)                                                                    |

`Remembered`/`Imprinted` and `Valid <spec>` read the card the amount is evaluated for (`AbilityUtils.java:512-555`), so
a trigger or layer check that runs while another ability resolves reads its own host. The ability-carried lists
(targets, trigger, paid cards) are read only when that card is the resolving ability's own source; any other evaluation
stays unresolved. A paid list is unresolved unless the cost that paid for the ability recorded it
(`paidLists.recorded`): activation costs record `Sac`/`Exile`/`Discard` picks and the self parts; a spell's additional
cost and a triggered `AB$` line's `Cost$` record what they sacrificed and discarded (`payUnlessPartsInto`); an unless
cost does not. A recorded list nothing was paid into reads 0, the operator unread (Java's null paid list).

A card that left the battlefield (a sacrificed, exiled or dying creature) is read from its last-known information
(`Game.LKI`), which is what Java's copy of it holds. `TriggeredCard` reads the snapshot of its last battlefield
departure, never one taken when the trigger was recorded (Java's `runParams`): a discarded card that was once on the
battlefield, or a dies-trigger card that returned before the trigger resolves (persist), reads the wrong state. The
measure is `Amount` (how many), `Valid <spec>` (how many match, from the host), or a per-card measure summed or folded
with `Greatest`/`Least`/`Different`: `CardPower`, `CardToughness`, `CardManaCost`, `CardCounters.<TYPE>`,
`CardNumColors`, `CardSumPT` (`liveCardMeasure`; inside a layer pass, `Game.layering`, a battlefield permanent's power
and toughness are refused because Layer 7 is being rebuilt). Anything else is unresolved. `resolveNamedAmount` also
strips a leading sign (`NumAtt$ +X`) as `calculateAmount` does, which Pump's `+X` needed. The `LKI` spellings
(`RememberedLKI$`, `ImprintedLKI$`) read a card that left the battlefield from its last-known information.

Not resolved: `Exiled$`/`Revealed$` lists that no cost or effect records yet, `AllTargeted$`, `TriggerObjects*$`,
`TriggeredSpellAbility$CardManaCostLKI`, `ThisTargeted$`, `Convoked$`, `Emerged$`, `Crewed$`. The player-list heads
(`TriggeredPlayer$`, `TargetedPlayer$`, `PlayerCount<hType>$`) are [`m6-r9-amounts.md`](m6-r9-amounts.md)'s. Tests:
`contextamounts_test.go`, `chosencosts_test.go`, `amountexpressions_test.go`.

## `TriggerCount$<Key>`

`TriggerCount$DamageAmount` (236 corpus lines), `LifeAmount` (40), `Amount` (32), the storm count: the integer the
trigger that put the ability on the stack recorded (`AbilityUtils.java:638`). `triggeredObjects.counts`
(`triggerCounts`, a typed struct with a set bitmask, never a map) is filled by the modes that record a key:
`DamageDone`, `DamageDoneOnce` and `DamageDealtOnce` (`DamageAmount`), `LifeGained` (`LifeAmount`, also `Player`),
`CounterAddedOnce` (`Amount`), `SpellCast` (`CurrentStormCount`, every spell cast this turn including this one; Storm
subtracts one). `Game.triggerCount` reads it for the resolving ability's own source and returns unresolved for a key the
mode did not record, not 0. Not recorded: `LifeAmount` of `LifeLost` (the mode is not ported), `Amount` of
`DiscardedAll`/`ChangesZoneAll`, `Result` of rolled dice, `ScryNum`. `Count$CardManaCost` (the source's own mana value)
is a count head too. Tests: `triggercount_test.go`.
