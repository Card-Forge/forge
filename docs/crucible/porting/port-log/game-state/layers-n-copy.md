# Port Log — Game State: Layer 1 copy gaps (batch N)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`entersascopy.go`](../../../../../crucible/internal/engine/entersascopy.go),
  [`replacemententry.go`](../../../../../crucible/internal/engine/replacemententry.go),
  [`entercounters.go`](../../../../../crucible/internal/engine/entercounters.go),
  [`cloneeffect.go`](../../../../../crucible/internal/engine/cloneeffect.go)
- **Builds on:** [`layer1-enters-as-copy.md`](layer1-enters-as-copy.md), [`effects-clone.md`](effects-clone.md),
  [`m5-replacement-3.md`](m5-replacement-3.md)

## The Copy layer runs before the move

`ReplacementLayer` order is `CantHappen, Control, Copy, Transform, Other` (`ReplacementLayer.java:9-13`).
`entryReplaced` now runs Control, then `applyCopyReplacements`, then Other, all before `Game.Move`. The old post-move
call is gone from `enterBattlefieldReplacements`.

| Consequence | Why |
| --- | --- |
| A Clone under Containment Priest copies first; the Priest reads the copy | Other layer sees the copied `Def`; a noncreature copy enters |
| An entry the Other layer replaces away drops the copy effect and any pending counters | CR 400.7; `entryReplaced` clears both when the card did not land |
| A card entering from a graveyard is still in it while `Choices$` runs | Java's last graveyard state; the "moved out already" divergence closed |
| A copied planeswalker or battle gets its printed loyalty or defense in `Move` | `Move` reads `BaseLoyalty` from the copied `Def` |
| `cloneEffect` accepts a target outside the battlefield only if it is `a.replacedCard()` | The entering card is still in hand, on the stack or in a graveyard |
| `Choices$` no longer needs the by-hand "not the entering card" filter | Nothing on the battlefield is the entrant yet |

`AdditionalAbility` children (`resolveAdditional`, `FlipCoin` branches) now carry `replacing`; Molten Sentry's Clone
read an unset `replacedCard()` before, which only worked because its target was the host on the battlefield.

## CR 616.1: several copy replacements

`applyCopyReplacements` keeps its own loop (`gen` identity, `ConfirmEffect` for `Optional$`) and asks
`chooseReplacement` when more than one candidate applies: the player the card enters under is the decider,
`PlayerController.ChooseReplacementEffect` the hook, candidates own face first, then watchers in player order.

Scenarios `copy-order-metamorph-beside-essence-of-the-wild-own-copy-first` / `-essence-first`: the copied Ichor
Wellspring's draw trigger fires only when Metamorph's own replacement went first. Module test
`TestEntersAsCopyAmongSeveralCopyReplacementsTheEnteringPlayerChooses` checks the resulting definitions.

## ETB$ counters in an entry chain

`PutCounter` with `ETB$` while `a.replacedCard() != NoCard` is `putEnterCounters`: the amount is read now (The
Mimeoplasm's `Remembered$CardPower` before `DBCleanup`) and the counters wait in `Card.pendingEnter`.
`applyEnterCounters` merges them into its per-kind piles after the move, so one `AddCounter` replacement (Hardened
Scales) sees the whole pile once. `Defined$` is `Self`, `ReplacedCard` or `ReplacedNewCard`, optionally with a
`.<valid>` suffix (`AbilityUtils.getDefinedCards`' `incR`). `CounterType$ EachFromSource` with `EachFromSource$` puts
one pile per kind the named cards hold (Dominion Saboteur), `CounterNum$` overriding the count. Outside an entry `ETB$`
is still refused.

| Card | Shape | Scenario |
| --- | --- | --- |
| Altered Ego | `P1P1`, `CounterNum$ X` | module test (`xPaid` needs a payment fixture) |
| Undercover Operative | `ConditionDefined$ Remembered`, `RememberCloneOrigin$` | `undercover-operative-copies-your-creature-and-enters-with-a-shield-counter`, `undercover-operative-copies-an-opponents-creature-without-a-counter` |
| Dominion Saboteur | `EachFromSource$ Remembered` | module test |
| The Mimeoplasm | `ChooseCard`, `ChangeZoneAll`, `PutCounter`, `Clone` | `the-mimeoplasm-enters-as-a-copy-with-counters-equal-to-the-other-power` |

`actions.log` gains `queue cardorder <id>[,...]` (`QueueCardOrder`): the two exiled cards of The Mimeoplasm are ordered
by `orderCardsByTheirOwners`.

## Effect-hosted replacements

The `Effect` refusals in `copyReplacementResolvable` are gone. Spark Double and Moritte of the Frost create an effect
card whose `Moved` replacement (Other layer) carries a chain `PutCounter, PutCounter, ChangeZone Self Command to Exile`.
`applyEnterCounters` runs a counter replacement whose chain goes past the first `PutCounter` through the Registry
(`runEnterChain`) after its walk of the replacements, so exiling the effect card never edits what is being iterated.
Scenarios `spark-double-enters-as-a-copy-with-an-additional-counter`,
`moritte-of-the-frost-enters-as-a-legendary-copy-with-two-counters`.

Mystic Reflection's effect card hosts a Copy-layer replacement; `Pump` gains `ImprintCards$`/`ForgetImprinted$`
(`PumpEffect.java:431-437`). The effect ends on its own `Mode$ ChangesZoneAll` trigger, which fires once for a batch:
two cards entering through one `ChangeZoneAll` both copy and the effect ends after the batch
(`TestEffectHostedCopyReplacementCopiesTheWholeBatchOnce`). A lone entry is a batch of one, so `permanentEffect`,
`attachEffect` and `PlayLand` now call `checkChangesZoneAllTriggers` after the ETB check; `moveByEffect` callers and
`Play` already fire their own. Scenario `mystic-reflection-next-creature-enters-as-a-copy-of-the-chosen`.

## Clone's rejected params

Corpus lines naming each (real `(AB|SP|DB)$ Clone` lines, same-line):

| Param | Lines | Cards | Outcome |
| --- | ---: | --- | --- |
| `RemoveCardTypes$` (+ `RemoveSubTypes$` under it) | 3 | Imposter Mech, Machine God's Effigy, Taskmaster | ported (`CardFactory.java:579-581`); Taskmaster still errors on `RemoveCreatureTypes$`, which Java never reads (PORT-8) |
| `PumpKeywords$`/`PumpDuration$` | 2 | Loose in the Park, The Fourteenth Doctor | ported (`CloneEffect.java:153-156`, `tokenPumpKeywords`); Loose in the Park still needs `Draft` and `ExiledWith` |
| `Embalm$`/`RemoveCost$` | 1 | Vizier of Many Faces | ported (`CardFactory.java:555`, `:613`): a card that is not an embalmed token takes the plain copy (Embalm is not ported) |
| `SetManaCost$`, `SetColorByManaCost$`, `SetCreatureTypes$`, `RemoveKeywords$`, `SetLoyalty$`, `GainText*` | 0 | — | kept rejected, no corpus line |

`RemoveCardTypes$` clears the core types and keeps supertypes; `RemoveSubTypes$`, read only under it, then drops every
subtype, since no core type is left to allow one.

## Condition$ EnduringStory

`Player.EnduringStory` is set by `assignEnduringStories` (state-based actions, like `assignBlessings`): a `Storied`
permanent on the battlefield whose controller has three or more `Permanent.YouCtrl+Historic` gives them the story.
`continuousConditionMet` reads it (Fili the Pathfinder, Thorin Oakenshield: 3 lines). Java's `RestartGameEffect` resets
only the blessing, so the story survives a restart here too (`RestartGameEffect.java:73`). Tests:
`enduringstory_test.go`. A dump shows no power or toughness, so no scenario can show the +1/+1.

The other Layer 4/5/6 rows of the "Still not resolved" table are not `Condition$` values or need more than tracked state
(a general `calculateAmount`, a host walk for `EffectZone$`, `ManaCost.getShortString`): untouched.

## Divergences and remaining

| Where | Java | Here |
| --- | --- | --- |
| Cards entering together | One counter table, one copy decision per batch | One entry at a time; a batch-hosted effect (Mystic Reflection) lives until the batch's `ChangesZoneAll` |
| Effect left in the Command zone | Moritte's `ValidCard$ Creature.IsRemembered` never matches a noncreature copy, so its effect lingers | Same: nothing exiles it |
| `ChangesZoneAll` trigger order | ETB and batch triggers collected together, then ordered | ETB triggers are pushed first, the batch ones second |
