# Port Log — Game State: M5 batch C, replacement effects

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `replacementchoice.go`, `replacement.go`,
  `control.go`

Siblings: [`replacement.md`](replacement.md) (the first replacement shapes), this file (CR 616 ordering, the next
`Event$` values, `Moved` destinations).

## CR 616: the affected player chooses

Java: `ReplacementHandler.run` (`ReplacementHandler.java:169-290`). Per `ReplacementLayer`: collect the candidates
(`getReplacementList`), let the decider pick one (`chooseSingleReplacementEffect`), mark it `hasRun`, apply it, and on
an `Updated` result run the event again without it; `Replaced` ends the event; `NotReplaced` (declined, nothing to do)
tries the others. The decider is the affected player, or the affected permanent's controller.

`runReplacements` (`replacementchoice.go`) is that loop. A dispatch hands it a `collect` closure that lists the
candidates in play order (players, zones, cards, faces, lines: `eachReplacementRule`) and, per candidate, an `apply`
closure that edits the event the dispatch owns and returns Replaced, Updated or NotReplaced. `collect` runs again after
every applied effect, so a line gated on the amount (`DamageAmount$`) sees the updated one.

| Dispatch | Event$ | Decider |
| --- | --- | --- |
| `drawReplaced` | `Draw` | the drawing player |
| `gainLifeReplaced` | `GainLife` | the player gaining |
| `damageReplaced` (`damageReplacement`) | `DamageDone` | the damaged card's controller |
| `damageReplacedPlayer` (`damageReplacement`) | `DamageDone` | the damaged player |

`PlayerController.ChooseReplacementEffect(g, decider, options []ReplacementOption) int` is the hook, with its own caller
here and nowhere else. `ScriptedController` answers from `QueueReplacementEffect(i)`; the fixture verb is
`queue replacement <index>`, the index into the candidates in play order. Like Java's human controller it does not ask
for a lone candidate, nor for several that read the same (one permanent, one `Description$`; two copies of a card are
two permanents and are asked). A controller answering out of range records a pending error and gets the first candidate
(GO-7).

Two Furnaces of Rath compound (2, 4, 8). Rhox Faithmender (twice) and Angel of Vitality (plus 1) turn a gain of 6 into
13 or 14 depending on the order.

| Scenario | Proves |
| --- | --- |
| `replacement-616-gain-life-doubling-then-plus-one` | index 0 applies Faithmender first: 6 gives 13 |
| `replacement-616-gain-life-plus-one-then-doubling` | index 1 applies Angel first: 6 gives 14 |
| `replacement-616-damage-doubling-then-plus-one` | Furnace then Thor: 2 gives 5 |
| `replacement-616-damage-plus-one-then-doubling` | Thor then Furnace: 2 gives 6 |
| `replacement-616-two-furnaces-quadruple-damage` | two identical shapes compound, one question asked |

Not ported: Java's `ReplacementLayer` order (CantHappen, Control, Copy, Transform, Other) as one loop per layer. The
Copy layer (`applyCopyReplacements`) and the `Prevent$ True` / `Layer$ CantHappen` shapes run in their own functions
before these four, which is Java's order; a `Draw`/`GainLife`/`DamageDone` line with another `Layer$` is not resolved. A
`ReplaceWith$` the dispatch cannot resolve is a candidate whose `apply` does nothing, so with two or more live
candidates the player can be asked about one that then declines. `Draw`'s `ReplaceWith$ DB$ Draw` draws directly rather
than raising a new `Draw` event, so a second draw replacement does not see the replacement's own cards (Java does, with
the first marked `hasRun`).

## Event$ values past the first shapes

Real R: line counts from the corpus (1,717 lines). Already resolved elsewhere: `AddCounter`, `CreateToken`,
`ProduceMana`, `DeclareBlocker` (`replacement.go`), `GameLoss`/`GameWin` `CantHappen` (`gameloss.go`), `Destroy` with
`Regeneration$` (`regeneration.go`). The unresolved rest, in count order, and what `replacementevents.go` ports:

| Event$ | Lines | Shape ported | Java |
| --- | --- | --- | --- |
| `Counter` | 118 | `Layer$ CantHappen`, `ValidCard$`, `ValidSA$ Spell[.props]` (117) | `CounterEffect.removeFromStack`, `ReplaceCounter` |
| `BeginPhase` | 21 | `Skip$ True` with `Phase$`, optional `ValidPlayer$`, `Hellbent$` (20) | `PhaseHandler.advanceToNextPhase` |
| `LifeReduced` | 8 | `ReplaceEffect` on `Amount` (limit, double), `CantHappen`, `Result$` | `Player.loseLife`, `ReplaceLifeReduced` |
| `LoseMana` | 5 | `ReplaceMana` `ReplaceType$` converts the pool instead of emptying it | `ManaPool.clearPool` |
| `BeginTurn` | 5 | `Skip$ True` with `ExtraTurn$ True` (4) | `PhaseHandler.getNextActivePlayer` |

Details that are not obvious from the Java:

- **Counter hosts.** A line with no `ActiveZones$` applies from any zone (`TriggerReplacementBase.zonesCheck`), which is
  how a spell's own "can't be countered" works from the stack; `counterCantHappen` always includes the spell itself as a
  host. `ValidSA$ Spell.Creature+YouCtrl` reads its properties off the spell's card (`YouCtrl` is the host's
  controller). `CounterEffect` still refuses a `CantBeCountered` static. Guile's `ReplaceWith$` line resolves
  ([`m5-replacement-2.md`](m5-replacement-2.md)).
- **BeginPhase before one-shot skips.** `advanceStep` checks `beginPhaseSkipped` before `consumeSkip`, so a static skip
  leaves a `SkipPhase` effect unspent. Java lets the player choose; a skip is a skip either way. Fasting's `Optional$`
  line asks the player ([`m5-replacement-2.md`](m5-replacement-2.md)).
- **BeginTurn.** Java's `isExtraTurn` is "the extra-turn stack is not empty after the pop"; the bottom entry is the
  normal turn. Time Vault's optional skip asks the player (`m5-replacement-2.md`).
- **LifeReduced** runs from `LoseLife` and from damage to a player (`isDamage`); infect damage is poison, not a loss.
  `Result$ LT1` compares life minus the loss. Worship's `LimitMax.Difference` needed `LimitMax`/`LimitMin` in
  `resolveReplaceCountAmount` and an inline `ReplaceCount$` in `VarValue$` (Bloodletter). The player property
  `lifeGE<n>` (a literal n) joined `matchesPlayerProperty`. `Monarch$ True` in the common requirements is now the host
  controller being the monarch (it rejected every line before), which Archon of Coronation's `CantHappen` needs.
- **LoseMana** runs when `emptyManaPools` finds a non-empty pool: `Pool.convertTo` moves every unit to the named type,
  snow kept as snow.
- `eachReplacementRule` walks `liveTraitFaces`, so a transformed or modal card's other face is not a host.

| Scenario | Proves |
| --- | --- |
| `replacement-counter-kavu-chameleon-cannot-be-countered` | self line from the stack |
| `replacement-counter-leyline-of-lifeforce-protects-a-creature-spell` | battlefield line, no `YouCtrl` |
| `replacement-counter-allosaurus-shepherd-protects-only-its-controllers-spells` | `YouCtrl` scopes to the host controller |
| `replacement-begin-phase-necropotence-skips-the-draw-step` | `Skip$ True`, `ValidPlayer$ You` |
| `replacement-begin-phase-eon-hub-skips-every-upkeep` | no `ValidPlayer$`, every player |
| `replacement-begin-turn-stranglehold-skips-an-opponents-extra-turn` | extra turn skipped, normal order resumes |
| `replacement-life-reduced-worship-leaves-one-life` | `Result$`, `lifeGE1`, `LimitMax.Difference` |
| `replacement-life-reduced-bloodletter-doubles-an-opponents-life-loss` | `PlayerTurn$`, inline `ReplaceCount$` |
| `replacement-lose-mana-horizon-stone-keeps-unspent-mana-as-colorless` | pool converted, not emptied |

Not ported: `RollDice` (4), `Attached` (3), and the rest at one line each; `Destroy` without `Regeneration$`,
`TurnFaceUp`, `Transform`, `Scry`/`Mill`/`DrawCards` and the `Optional$` lines are in
[`m5-replacement-2.md`](m5-replacement-2.md). `LifeReduced` lines whose `ReplaceWith$` is not a `ReplaceEffect`
(Enduring Angel's Transform) record a pending error when they apply.

## Event$ Moved to the graveyard: library destinations and chains

Java: `ReplaceMoved`, `ReplacementHandler.executeReplacement`. A `ReplaceWith$` ChangeZone of a Moved replacement is
played at once, without the stack (the "buffered" branch is `DamageDone` only), and its `SubAbility$` chain follows it.

`moveToGraveyard(controller, id)` now performs the move itself and runs the chain, so its roughly 20 callers keep their
shape; it gained the `controller` they all had in scope (a chain's token or extra turn may ask a decision).
`movedGraveyardDestination` returns the zone and a `movedPost`:

| Param on the ChangeZone | Effect |
| --- | --- |
| `Destination$` | `Exile`, `Hand` or `Library` |
| `LibraryPosition$` | `0` (top, the default) or `-1` (bottom); other values are a pending error |
| `Shuffle$ True` | the owner's library is shuffled with the game's RNG after the card goes in |
| `Reveal$` | accepted, no effect (hidden information is not tracked) |
| `SubAbility$` | the chain resolves through the Registry from the host, as its controller's own |

All 91 real `Destination$ Graveyard` lines now resolve: Library 8 (Gravebane Zombie top, Nissa's Chosen and Wheel of Sun
and Moon bottom, Darksteel and Blightsteel Colossus, Progenitus, Legacy Weapon, Nexus of Fate shuffled), chains 4
(Kalitas' Zombie token, Ugin's Nexus' extra turn, Firestorm Phoenix' effect, Necromancer's Magemark). The dies, exiled
and returned triggers key off the zone the card ended in, so a library-bound creature does not die. Token and copy
vanishing (CR 704.5d) is `Game.Move`'s and landed earlier.

| Scenario | Proves |
| --- | --- |
| `replacement-moved-gravebane-zombie-dies-on-top-of-its-owners-library` | top of the library, not the graveyard |
| `replacement-moved-nissas-chosen-dies-on-the-bottom-of-its-owners-library` | `LibraryPosition$ -1` |
| `replacement-moved-progenitus-is-shuffled-into-the-library` | `Shuffle$ True` |
| `replacement-moved-kalitas-exiles-and-creates-a-zombie` | `SubAbility$` chain after the exile |

Not ported: a `Destination$` other than Graveyard or the battlefield's tapped shapes on `Event$ Moved` (the 973-line
family is almost all `Destination$ Battlefield`, resolved as "enters tapped" only), a `ChangeZone` replacement of an
`Origin$ Stack` move into a zone other than Graveyard (Glimpse the Cosmos' `ValidLKI$ Card.CastSa` line), and
`ChangesZoneAll` batch triggers, which still name the graveyard when a card was exiled instead.
