# Port: Card Smoke Probe

- **Java source:** none. New code: Forge has no counterpart, and the Java oracle cannot say which cards the Go engine
  plays
- **Go target:** `crucible/internal/smoke`, `crucible/cmd/crucible/smoke.go` (`crucible card-smoke`)
- **Status:** Measurement done on the whole corpus and on a proxy deck set. The replay-parity harness (ADR-0010 L3) is
  not started

## What it does

ADR-0011 defines "done" for M6 as a command that exits zero over the gauntlet. `corpus-coverage` only answered "does the
database have the card". `card-smoke` answers "does the engine play it": for each card it builds a fixed board, puts the
card in hand, casts it (or plays it as a land), resolves the stack, activates each activated ability once and runs three
turns, with `smoke.Controller` answering every decision. A card that cannot be cast is retried on up to six more board
variants, in order: an opposing instant on the stack, an opposing creature spell on the stack, a rich board (an
artifact, an enchantment, an equipment, a flier, a planeswalker, one land of each basic type, a tapped creature, a
graveyard of seven cards, a card in exile, a hand of three), the rich board with each of the two spells, and the rich
board in the declare blockers step with an attacking and a blocking creature. A card whose spell names
`ActivationPhases$`, `PlayerTurn$`, `OpponentTurn$` or `CheckSVar$` is then tried timed (`internal/smoke/timing.go`): on
the plain, rich and tailored boards, the game is played with `Game.Step` from the untap step of the opponent's turn
(turn 2) or its own (turn

1. up to each step that grants priority, the active player attacking with everything and nobody blocking, and the card
   is cast there; which step the restriction accepts is the engine's answer, not the probe's. The first variant that
   casts the card decides the outcome.

Any card still not cast is then tried on staged boards (`internal/smoke/staged.go`), whose game state is played through
the engine first: an Aura cast, a creature killed and another cast, two spells or a flashback spell left on the stack, a
creature dealt 1 damage, tokens, a +1/+1 counter, an opponent's face-down creature, and six combat moments with the
defender blocking with everything or not at all.

| Outcome        | Meaning                                                                                                                                                                                                                                                         |
| -------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `plays`        | No error surfaced on that path                                                                                                                                                                                                                                  |
| `refused`      | The engine returned an error or left a pending one (GO-7): the loud half, and the M6 backlog                                                                                                                                                                    |
| `panicked`     | An engine invariant breach, recovered at the probe boundary. Always a bug                                                                                                                                                                                       |
| `hung`         | No result within `-timeout` (default 20 s). The engine loops on this card. Always a bug                                                                                                                                                                         |
| `placed`       | A Command-zone type (plane, scheme, Vanguard, conspiracy, phenomenon): put in the zone, three turns run, statics and triggers live. Abilities that need the game mode (set in motion, chaos, planeswalk) are not exercised, so it is counted apart from `plays` |
| `inconclusive` | The card could not be cast on any board variant (an extra cost, an X or a target the variants lack)                                                                                                                                                             |

The refusal causes are grouped by the first line of the error with card names, numbers and ids blanked, so one missing
param counts once however many cards carry it. That table, ordered by cards blocked, is the backlog.

## What the number means

**An upper bound.** "Plays" is the absence of a loud refusal. The probe cannot see what the engine does silently wrong
(the silent GO-7 gaps listed in `game-state.md`), what a card does on a board other than the fixed one, or a card whose
ability needs a situation (a combat, a graveyard of a given shape) the three turns never create. The inconclusive cards
are left out of the percentage, as are the `placed` ones.

Controller policy is part of the measure: the first legal option, the smallest sensible count, attack with everything,
never block, and "yes" to at most 40 optional questions. The cap exists because a "you may repeat" effect (Ad Nauseam,
Pain's Reward, Dance with Calamity) answered yes forever loops by the controller's fault, not the engine's.

## Results

Measured on the full corpus (34,078 cards):

| Outcome      |  Cards |
| ------------ | -----: |
| plays        | 32,265 |
| refused      |  1,306 |
| panicked     |      0 |
| hung         |      0 |
| placed       |    434 |
| inconclusive |     73 |

96.1% of the 33,571 judged cards play. On the 8,668 distinct cards of the 505 quest preconstructed decks under
`forge-gui/res/quest/precons` (a proxy, not a metagame: the real gauntlet decklists are not supplied,
[`meta-gauntlet.md`](../../research/meta-gauntlet.md)) it is 97.1% of 8,654 judged.

Largest refusal causes over the full corpus (cards blocked):

| Cards | Refusal                                      |
| ----: | -------------------------------------------- |
|    34 | `Charm: Defined$ not resolvable`             |
|    19 | `ChangeZone: DifferentNames$ not resolvable` |
|    16 | `RepeatEach: DamageMap$ not resolvable`      |
|    15 | `Counter: Defined$ not resolvable`           |
|    15 | `DelayedTrigger: Mode$ not resolvable`       |
|    14 | `ChangeZone: AtRandom$ not resolvable`       |

Run `crucible card-smoke -all` for the current table; these counts move with every port.

## Engine bug the first run found

Four cards hung the engine (Vodalian War Machine, Herald of Vengeance, Summoning Trap, Spider-Man 2099; Visions of
Phyrexia and its Alchemy twin hung with them): a `Mode$ Phase | Phase$ Cleanup | Static$ True` trigger.
`checkPhaseTriggers` did not mark the match `staticTrigger`, so it went on the stack, CR 514.3a gave priority and began
another Cleanup, which fired the trigger again, forever. It now resolves inline like the ChangesZone and TapsForMana
static triggers (ADR-0020). `TestStaticCleanupTriggerDoesNotRepeatTheCleanupStep` pins it. Every other trigger mode sets
`staticTrigger` too now (`TestStaticTriggerResolvesInlineForEveryMode`; the skips in BecomeMonarch, Abandoned and
LandPlayed are gone).

## Not done

- The gauntlet itself: no decklists, so no "cards of the gauntlet that play" figure. `-decks gauntlets/modern` takes the
  real list when it exists.
- The inconclusive cards. Past the variants (plain board, a spell on the stack, the rich board, combat) the probe builds
  a tailored one: for each `ValidTgts$` alternative the card names it places a corpus card the spec accepts
  (`internal/smoke/tailor.go`, fewest abilities first; judged with the card in hand, so a spec that reads game state
  finds none until a staged board plays it), or casts one for `TargetType$ Spell`. What remains is engine work that
  fails silently today, grouped in open plan R25d-R25k. The probe's mana pool holds 20 of each colour: the controller
  pays generic mana with the colour floating most, so a cost of twelve generic needs twelve of one.
- Command-zone cards are counted as `placed`, which proves nothing about their one-shot abilities. A sticker sheet is
  refused by the probe itself (open plan R15), not by the engine, and that line goes when stickers are ported.
- The replay-parity harness (ADR-0010 L3): `RecordingPlayerController` over the 110 `PlayerController` methods in
  `oracle-java` and a `ReplayController` here. Separate work; open plan, Exit gates, P5.

Tests: `internal/smoke/probe_test.go`.
