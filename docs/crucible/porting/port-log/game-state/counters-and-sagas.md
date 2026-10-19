# Port Log — Game State: counter triggers, Sagas and Renown

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `counteradded.go`, `saga.go`,
  `putcountereffect.go` (`Renown`), `action.go` (`sacrificeCompletedSagas`), `turn.go` (Main1)
- **Decision:** [ADR-0038](../../../adr/0038-keyword-expansion.md) for `K:Chapter` and `K:Renown`

## `Mode$ CounterAdded` and `Mode$ CounterAddedOnce`

`Card.addCounter` (`Card.java:1780-1822`) runs `CounterAdded` once per counter, with `CounterAmount` the card's new
total, then `CounterAddedOnce` once for the placement. Every effect that puts counters on a card now goes through
`Game.addCardCounters` (`counteradded.go`): the counters, the `CounterChanged` event, then the triggers. Wired through
it: `PutCounter`, `PutCounterAll`, `MultiplyCounter`, `MoveCounter` (its destination), `Amass`, `Explore`, `Connive`,
`Endure`, `Earthbend`, `Blight`, the counters a permanent enters with (`applyEnterCounters`), a Saga's lore counters,
Fabricate's and Cumulative upkeep's. Not wired, so they fire nothing: loyalty and defense counters a planeswalker or
battle enters with (`Game.Move`), the `AddCounter<N/Type>` activation cost, `MakeCard`, token creation counters,
`Proliferate` and `TimeTravel` (`Counters.Add` directly).

A trigger fires for hosts on the battlefield (`traitHosts`). Params read: `ValidCard$`, `ValidSource$` (the placing
player, `AbilityKey.Source`, `Card.java:1805`; [R9f](m6-r9f-trigger-findings.md)), `CounterType$`, and on `CounterAdded`
`CounterAmount$ <op><n>` (`Expressions.compare` against the new total); the general gates (`TriggerZones$`,
`IsPresent$`, `OptionalDecider$`, phase params). A trigger naming any other param (`ValidPlayer$`, `FirstTime$`,
`ActivationLimit$`, `ValidSpellAbility$`, `ValidMode$`, `ValidEntity$`) is skipped, not fired without its condition
(GO-7). `Defined$ TriggeredCard` reads the card the counters went on.

## Sagas (CR 714)

| Rule | Go |
| --- | --- |
| enters with a lore counter (`CardState.getSagaRep`) | `applySagaCounter`, from `enterBattlefieldReplacements`, for a Saga type without `Read ahead`; through `countersReplaced` |
| a lore counter at the precombat main (`PhaseHandler.java:283`) | `sagaLoreCounters` in `beginStep`'s Main1 case, for each Saga the active player controls with a `Chapter` keyword |
| chapter N triggers when the counters reach N | `keyword.Expand`'s `Chapter:<N>:<SVar>,...`: one `Mode$ CounterAdded \| CounterType$ LORE \| CounterAmount$ EQ<i>` trigger per chapter |
| sacrifice when lore counters reach the last chapter and none of its chapter abilities is on the stack | `sacrificeCompletedSagas`, a state-based action; `hasChapterOnStack` scans the stack for an ability tagged `Chapter` |

`compileFace` tags the ability a synthesized trigger or replacement runs with the keyword line it came from
(`Ability.Keyword` on the `Execute$`/`ReplaceWith$` sub-ability, as Java's `SpellAbility.isKeyword` sees it), which is
how a chapter on the stack is told from any other ability of the Saga, and how `PutCounter` knows Renown's counters are
Renown's. A state-based sacrifice has no ability behind it, so `sacrificeCards` takes a nil `Params`.

Chapters are limited by the effects they run, not by this: a chapter that discards a hand (`Discard` with `Mode$ Hand`)
or names an effect the registry lacks fails the way any such ability does. Not ported: `Read ahead` (a Saga with it gets
no lore counter and never advances), chapters of a Saga that is a copy gaining the keyword, and `Mode$ CounterRemoved`
(Vanishing and Suspend).

Tests: `sagas_test.go` (chapters I-III and the sacrifice, the sacrifice waiting for chapter III to leave the stack,
`CounterAdded` per counter and `CounterAddedOnce` per placement).

## Renown

`K:Renown:<N>` expands to a `Mode$ DamageDone` trigger (`ValidSource$ Card.Self`, `ValidTarget$ Player`,
`CombatDamage$ True`, `IsPresent$ Card.Self+!IsRenowned`) running `DB$ PutCounter`. `CountersPutEffect.java:553` sets
the creature renowned after a `PutCounter` whose ability is the Renown keyword's, whether or not the counters were
prevented: `putCounterEffect` sets `Card.renowned` for `a.Params.Keyword == "Renown"`, `Game.Move` clears it when the
card leaves the battlefield, and `IsRenowned` is a valid property. The fixture format's `|Renowned` annotation is still
unapplied. Test: `TestRenownTriggersOnlyOnce`.
