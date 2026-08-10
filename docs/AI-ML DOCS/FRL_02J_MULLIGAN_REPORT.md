# FRL-02J MULLIGAN Boundary Report

## Milestone status

```text
FRL-02J MULLIGAN:
PASS (controlled Forge-London callback slice)

Official simultaneous mulligan timing:
ENGINE GAP / DEFERRED
```

Branch: `frl/02j-mulligan-boundary`

Approved base:

```text
master @ 5ad875a9ec08b6d056ae780584b9b93a8e2c4c94
```

This milestone implements the ordinary two-player Constructed London callback boundary. It does not implement
RL/PPO/R2D2/IMPALA, `RandomLegalPolicy`, general pregame handling, Commander/multiplayer expansion, starting-player
choice, `Backup Plan`, `Serum Powder`, legacy mulligan rules, `CONFIRMATION`, `ORDER`, combat damage, shuffle/library
mechanics, or an official simultaneous-mulligan rewrite.

## Forge authority and callback path

Forge remains authoritative for all gameplay mutation:

```text
MulliganService
-> canMulligan()
-> PlayerController.mulliganKeepHand(firstPlayer, cardsToReturn)
-> existing AI boolean
-> diagnostic-only KEEP/REDRAW capture and replay
-> original boolean returned unchanged
-> Forge performs mulligan/redraw progression
-> London draws seven
-> PlayerController.tuckCardsViaMulligan(callbackHand, cardsToReturn)
-> diagnostic-only MULLIGAN_BOTTOM replay
-> original AI CardCollection returned unchanged
-> London moves returned cards to library
```

The neutral layer never moves cards, shuffles, changes library order, changes timestamps, increments the mulligan
count, or changes `GameStage`.

## KEEP / REDRAW contract

`DecisionType.MULLIGAN` exposes exactly two semantic alternatives:

```text
MULLIGAN|KEEP
MULLIGAN|REDRAW
```

The public `MulliganContext` contains:

```text
gameId
mulliganSessionId
mulliganRoundIndex
mulliganStepIndex
actingPlayerId
startingPlayerId
cardsToReturn
handSize
stage = KEEP_OR_REDRAW
own hand CardSelectionCard values
```

It contains no opponent hand, opponent library order, future draws, hidden AI evaluation, or live Forge object
reference. `startingPlayerId` is captured independently from `actingPlayerId`; it is the `firstPlayer` argument passed
by `MulliganService`.

The provider admits only London, two-player Constructed, ordinary Mulligan-stage callbacks. The active Forge rule is
read at callback time and is not changed by the feature. Non-London, multiplayer/Commander, malformed return counts,
known `Serum Powder`, and known `Backup Plan` states fail closed without a neutral policy request.

When Forge bypasses the controller because `canMulligan() == false`, the observer records a forced KEEP callback.
No `MULLIGAN` policy event is inferred and the controller is not called.

An empty hand is explicitly unsupported as `UNSUPPORTED_EMPTY_HAND_MULLIGAN`. The Forge `LondonMulligan` empty-hand
termination edge is not repaired in FRL-02J and is not exported as a supported REDRAW domain.

## Session lifecycle and stale safety

There is one parent `MulliganSession` per `(gameId, actingPlayerId)` in the diagnostic provider. It spans all real
Forge callbacks for that player:

```text
round 0 KEEP / REDRAW
REDRAW -> await Forge redraw and tuck callback
round 1 KEEP / REDRAW
...
KEEP -> terminal
```

The two players' sessions are independent and can interleave. A session has at most one outstanding KEEP/REDRAW
request. A second generation returns `REQUEST_OUTSTANDING` without allocating a new step. REDRAW consumes the current
request and waits for Forge to supply the next real hand; it never predicts or mutates the next round. KEEP terminalizes
the parent session, and old requests cannot be reapplied. Registries are cleaned when the mulligan process/game ends.

Every request/application checks the same game, acting player, starting player, session, round, stage, return count, and
exact current hand identity snapshot. Card identity is `(cardId, gameTimestamp)`; a same-name card or a new timestamp is
stale.

## MULLIGAN_BOTTOM and CARD_SELECTION reuse

Bottom cards use `DecisionType.CARD_SELECTION` through `CardSelectionAdapter.MULLIGAN_BOTTOM`. No
`SELECT_BOTTOM_CARD`, `DecisionType.ORDER`, fake `SpellAbility`, or `ActionContinuation` is created.

The callback `hand` argument is the authoritative selectable domain. The adapter uses `min = cardsToReturn` and
`max = cardsToReturn`, so `DONE` cannot appear before the exact count. A zero-count callback completes without a policy
request.

The generic source semantics are explicit:

```text
DISCARD:
    source card identity remains available
MULLIGAN_BOTTOM:
    source absent
    sourceCardId = null
    sourceCardTimestamp = null
```

The existing discard adapter retains its behavior, including its set-oriented semantic replay. MULLIGAN_BOTTOM keeps
the controller's returned order:

```text
controller returns [C, A]
Forge receives [C, A]
```

Each synthetic step changes only session-local selected identities. Completion resolves those identities to a new
ordered `CardCollection` of the current live hand cards. Wrong count, duplicate, unknown, or stale controller cards
produce `MAPPING_FAILED`; diagnostics fail open and return the original AI collection.

## Diagnostics

The diagnostic CSV separates:

```text
MULLIGAN_CALLBACK
MULLIGAN
MULLIGAN_STATE
CARD_SELECTION_CALLBACK
CARD_SELECTION (selection_adapter=MULLIGAN_BOTTOM)
```

It records process/game/session/round/step IDs, acting and starting player IDs, return count, hand size, candidate
count, forced flag, selected action, status/reason, generation time, native callback time, selection adapter/session
and step, and selection counts. Native AI latency is measured separately from neutral request generation.

## Official timing caveat

The current official Comprehensive Rules timing declares mulligan choices in turn order and then executes all
mulligans simultaneously. Forge's `MulliganService` executes each REDRAW immediately inside its sequential per-player
callback loop. FRL-02J preserves that Forge behavior and therefore does not claim full official-information or timing
equivalence. The gap remains an engine-level follow-up:

[Magic: The Gathering Comprehensive Rules, current checked copy](https://media.wizards.com/2026/downloads/MagicCompRules%2020260807.txt)

The supported v0 assumption is limited to two-player Constructed London games with no intermediate pregame effect
that observes or reacts to the sequential redraw mutation.

## Verification

| Check | Result |
|---|---|
| FRL-02J/CardSelection focused tests | 42 passed, 0 failed |
| Complete `forge.game.decision` selection run | 218 executed; 211 passed; 7 pre-existing `PriorityActionDiagnosticsTest` failures |
| `mvn -pl forge-ai -am test` | 26 passed (6 forge-game, 20 forge-ai) |
| Package build | SUCCESS |
| Checkstyle in package/focused builds | 0 violations |
| `git diff --check` | clean |

The seven unrelated decision-suite failures are the approved-base mismatch where
`PriorityActionDiagnostics` emits 55 columns while its existing tests assert 54. FRL-02J does not alter that
adjacent diagnostics contract.

## Controlled benchmarks

Built JAR: `forge-gui-desktop/target/forge-gui-desktop-2.0.14-SNAPSHOT-jar-with-dependencies.jar`

| Matchup | Seed | Games | Result | Raw KEEP/REDRAW callbacks | Strategic KEEP / REDRAW | Raw tuck callbacks | Synthetic bottom requests |
|---|---:|---:|---|---:|---:|---:|---:|
| Dead and Alive vs Air Forces | 20260809 | 10 | Dead and Alive 7–3 | 23 | 20 / 3 | 3 | 3 |
| Izzet Guild Kit vs Dimir Guild Kit | 20260810 | 10 | Izzet 3–7 | 24 | 20 / 4 | 4 | 4 |

Aggregate:

```text
games:                         20
mulligan sessions:             40 (2.0/game)
rounds/sessions:               47 rounds / 40 sessions
KEEP/REDRAW requests:          47 (40 KEEP, 7 REDRAW)
forced engine KEEP events:     0
raw tuck callbacks:             7
MULLIGAN_BOTTOM requests:      7
cardsToReturn distribution:    1:7
forced bottom steps:            0
mapping failures:               0
stale failures:                0
unsupported states:            0
```

Neutral generation latency, nanoseconds, using callback/request rows from both runs:

```text
KEEP/REDRAW generation p50/p95/p99:  14,200 / 464,800 / 4,409,800
MULLIGAN_BOTTOM generation p50/p95/p99: 105,400 / 1,223,100 / 1,223,100
native callback latency p50/p95/p99:  21,200 / 179,900 / 9,073,500
```

The p99 values are the maximum for these small controlled samples. Metrics were written to:

```text
C:\Users\chris\AppData\Local\Temp\frl02j-dead-air-final2-20260810.csv
C:\Users\chris\AppData\Local\Temp\frl02j-izzet-dimir-final2-20260810.csv
```

## Changed files

Production changes add the neutral MULLIGAN DTO/provider/session/diagnostics path, the MULLIGAN_BOTTOM adapter,
explicit CARD_SELECTION adapter/source semantics, `DecisionType.MULLIGAN`, semantic KEEP/REDRAW candidates, and the
narrow Forge observer seam. Tests cover ordinary candidates, starting-player separation, AI boolean mapping,
session/interleaving/stale/empty/unsupported lifecycle, exact ordered bottom selection, mapping fail-open behavior,
discard regressions, and public API reflection.

`FRL-02J-prompt-clean.md` remains untracked and is intentionally not staged or committed.
