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
turns, with `smoke.Controller` answering every decision.

| Outcome        | Meaning                                                                                      |
| -------------- | -------------------------------------------------------------------------------------------- |
| `plays`        | No error surfaced on that path                                                               |
| `refused`      | The engine returned an error or left a pending one (GO-7): the loud half, and the M6 backlog |
| `panicked`     | An engine invariant breach, recovered at the probe boundary. Always a bug                    |
| `hung`         | No result within `-timeout` (default 20 s). The engine loops on this card. Always a bug      |
| `inconclusive` | The card could not be cast on the fixed board (no spell to counter, a cost the board lacks)  |

The refusal causes are grouped by the first line of the error with card names, numbers and ids blanked, so one missing
param counts once however many cards carry it. That table, ordered by cards blocked, is the backlog.

## What the number means

**An upper bound.** "Plays" is the absence of a loud refusal. The probe cannot see what the engine does silently wrong
(the silent GO-7 gaps listed in `game-state.md`), what a card does on a board other than the fixed one, or a card whose
ability needs a situation (a combat, a graveyard of a given shape) the three turns never create. 1,710 cards are
inconclusive for the last reason and are left out of the percentage.

Controller policy is part of the measure: the first legal option, the smallest sensible count, attack with everything,
never block, and "yes" to at most 40 optional questions. The cap exists because a "you may repeat" effect (Ad Nauseam,
Pain's Reward, Dance with Calamity) answered yes forever loops by the controller's fault, not the engine's.

## Results

Measured at the commit that added the probe, on the full corpus (34,078 cards):

| Outcome      |  Cards |
| ------------ | -----: |
| plays        | 27,967 |
| refused      |  4,401 |
| panicked     |      0 |
| hung         |      0 |
| inconclusive |  1,710 |

86.4% of the 32,368 judged cards play. On the 8,668 distinct cards of the 505 quest preconstructed decks under
`forge-gui/res/quest/precons` (a proxy, not a metagame: the real gauntlet decklists are not supplied,
[`meta-gauntlet.md`](../../research/meta-gauntlet.md)) it is 88.9% of 8,345 judged.

Largest refusal causes over the full corpus (cards blocked):

| Cards | Refusal                                           |
| ----: | ------------------------------------------------- |
|   169 | `Discard`: `ValidTgts$` not resolvable            |
|    94 | `Pump`: `KW$` value not resolvable                |
|    91 | `ChangeZone`: `Duration$` not resolvable          |
|    85 | `ChangeZone`: `Chooser$` not resolvable           |
|    82 | `DealDamage`: `DamageSource$` not resolvable      |
|    72 | `PutCounter`: `TargetMin$` not resolvable         |
|    56 | `Discard`: `Mode$` value not resolvable           |
|    54 | `DealDamage`: `NumDmg$` amount not resolvable     |
|    50 | `Draw`: `NumCards$` amount not resolvable         |
|    44 | `Token`: `TokenAmount$` amount not resolvable     |
|    44 | triggered `AB$`: `Cost$` not resolvable           |
|    43 | `ChangeZone`: `OriginAlternative$` not resolvable |

Run `crucible card-smoke -all` for the current table; these counts move with every port.

## Engine bug the first run found

Four cards hung the engine (Vodalian War Machine, Herald of Vengeance, Summoning Trap, Spider-Man 2099; Visions of
Phyrexia and its Alchemy twin hung with them): a `Mode$ Phase | Phase$ Cleanup | Static$ True` trigger.
`checkPhaseTriggers` did not mark the match `staticTrigger`, so it went on the stack, CR 514.3a gave priority and began
another Cleanup, which fired the trigger again, forever. It now resolves inline like the ChangesZone and TapsForMana
static triggers (ADR-0020). `TestStaticCleanupTriggerDoesNotRepeatTheCleanupStep` pins it. Other trigger modes still do
not set `staticTrigger` when a line carries `Static$ True`.

## Not done

- The gauntlet itself: no decklists, so no "cards of the gauntlet that play" figure. `-decks gauntlets/modern` takes the
  real list when it exists.
- Counterspells and other cards that need a spell on the stack are inconclusive; the board has none.
- The replay-parity harness (ADR-0010 L3): `RecordingPlayerController` over the 110 `PlayerController` methods in
  `oracle-java` and a `ReplayController` here. Separate work; see `00-master-implementation-plan-in-progress.md`
  item 32.

Tests: `internal/smoke/probe_test.go`.
