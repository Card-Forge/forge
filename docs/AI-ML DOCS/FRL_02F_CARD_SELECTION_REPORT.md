# FRL-02F CARD_SELECTION / Discard Selection Boundary Report

## Status

```text
FRL-02F architecture: APPROVED
Prerequisite blocker: NONE
Implementation: COMPLETE FOR CONTROLLED SLICE
```

Implementation branch: `frl/02f-card-selection-boundary`

Base: `d06ebc79852bfcc05e466d6d3a593524689bad1a`

This milestone adds a neutral, mutation-free `DecisionType.CARD_SELECTION` primitive and one deliberately narrow
resolution-time Discard adapter. It does not implement reinforcement learning, change Forge AI choices, or turn
all card-choice APIs into one abstraction.

## Exact controlled Forge path

The real Izzet Charm script supplies three modes. Its draw mode is:

```text
Draw 2
-> SubAbility DBDiscard
-> Defined You
-> NumCards 2
-> Mode TgtChoose
```

The runtime path remains:

```text
cast Izzet Charm
-> MODE chooses draw/discard
-> PAYMENT
-> spell placed on stack
-> action announcement ends; PriorityActionDiagnostics.endAction() removes ActionContinuation
-> opponents receive priority
-> spell resolves
-> DrawEffect draws two
-> DBDiscard resolves
-> DiscardEffect derives discarder, chooser, hand, validCards, visibleToChooser, min and max
-> chooseCardsToDiscardFrom(...)
-> controller returns one complete CardCollection
-> diagnostic-only neutral sequential replay
-> GameActionUtil.orderCardsByTheirOwners(...)
-> Player.discard(...)
-> canBeDiscardedBy(...) is checked and normal movement/triggers occur
```

`chooseCardsToDiscardFrom` returning the collection is the authoritative player choice. Forge's later
`Player.discard` remains the final legality and movement authority.

## Controlled adapter capability

The generic primitive can represent distinct chooser/affected players, visible supersets, duplicate names, and
`min = 0` / `DONE`. The v0 Discard adapter claims support only when the actual callback has:

```text
Mode = TgtChoose
chooser == affected/discarding player
every valid identity in that player's current hand
no RevealNumber dependency
no UnlessType path
every callback-valid identity present in visibleToChooser
```

This is the own-hand Izzet Charm shape. `YouChoose`, `RevealYouChoose`, `LookYouChoose`, `RevealTgtChoose`, and
other modes remain explicit future adapter capabilities even where the primitive could model their data.

Discard modes that do not invoke this callback remain Forge-owned: `Random` is engine selection; `Defined`,
`Hand`, and `RevealDiscardAll` are forced/engine-derived collections; `UnlessType` uses its separate callback;
and optional confirmations outside this callback remain future `CONFIRMATION` work.

## Sequential decision contract

One callback creates one provider-local `CardSelectionSession` containing immutable callback snapshots and
session-local selected state:

```text
selectionSessionId
gameId
chooser
affected player
source ability identity
callback-valid stable identities
visible-to-chooser public identities
min / max
selected identities
```

Each request has a normal `requestId`, `selectionSessionId`, and zero-based `selectionStepIndex`. Candidates are:

```text
SELECT_CARD | cardId | gameTimestamp
DONE
```

Public visible-card data contains the stable identity, visible name, zone, and relevant owner/controller IDs.
Names, Java object identity, and list positions are never exported as identity. Candidates are deterministically
sorted by the neutral semantic key, so same-named copies remain distinct.

Generation follows:

```text
selectedCount < min       -> remaining SELECT_CARD only
min <= count < max        -> remaining SELECT_CARD plus DONE
selectedCount == max      -> COMPLETE immediately
```

Thus exact-two selection has no subset enumeration and no trailing DONE request. One legal candidate is forced;
`DONE` plus a selectable card is strategic. `min = 0, max = 0` completes immediately, while an empty
`min = 0, max > 0` domain produces one forced DONE request.

## Completion and mutation safety

Applying `SELECT_CARD` only appends a stable identity to private session state. It does not remove a card from
hand, move zones, modify the source ability or targets, add remembered objects, or fire events/triggers. At
completion, exact live identities are resolved into a new `CardCollection`. The diagnostic adapter never replaces
the controller collection: it returns no gameplay value, and `DiscardEffect` continues with the original result.

Focused regressions prove the hand is unchanged after the first and second synthetic choices. A separate real
`DiscardEffect` resolution test proves Forge still moves exactly the controller-selected count through its normal
discard path.

## Identity and stale revalidation

Stable identity is exactly `(cardId, gameTimestamp)`. Every atomic generation/application verifies:

```text
same game, chooser and affected player still in game
selected count does not exceed max
each original callback-valid id+timestamp still exists in affected player's current hand
identity remains in the original callback-valid snapshot
identity belonged to the original visibility contract
identity was not already selected
```

The lookup scans the affected player's current hand for exact ID and timestamp. It does not rely on ID-only
`Game.getCardState`, recalculate `DiscardValid`, or substitute by name. A real hand -> graveyard -> hand move with
the same card ID and a new timestamp is rejected as `LIVE_STATE_CHANGED`; another same-named copy is never used.
The original callback `validCards` remains selection legality authority. Forge rechecks final discard legality.

## Hidden-information boundary

Two snapshots remain separate:

```text
selectable identities = callback validCards
visible context        = visibleToChooser
```

Visible-but-invalid cards are context only and never candidates. If any selectable identity is absent from the
visibility snapshot, session creation returns `UNSUPPORTED_HIDDEN_CARD_SELECTION` and exports no request or card
features. The v0 own-hand adapter therefore cannot expose hidden opponent hand identities.

## Resolution-time identity

`selectionSessionId` is provider/game-local grouping identity, not a globally unique callback ID. Persisted rows
carry `process_id`, `selection_game_id`, and `selection_session_id`; steps add `selection_step_index`.

Session creation consumes no ActionContinuation index. If a real continuation is supplied to the generic
primitive, each generated request records its existing `decision_sequence_id` and consumes one genuine
subdecision index. Izzet Charm resolves with no active continuation, so both action fields remain null while its
selection session and step indexes remain populated. `ActionContinuation` itself is unchanged, and resolution is
not reconnected to the earlier cast sequence.

## AI diagnostic replay

The diagnostic path is deliberately observational:

```text
snapshot callback arguments
-> record CARD_SELECTION_DISCARD_CALLBACK
-> invoke the unchanged controller
-> receive its complete CardCollection
-> validate bounds, membership, uniqueness and visibility
-> sort the returned set by neutral semantic key for diagnostic sequentialization only
-> replay through the neutral session
-> record CARD_SELECTION steps or CARD_SELECTION_STATE
-> leave the original controller collection untouched
```

The AI result never creates or filters legal candidates. Mapping order is neither AI policy semantics nor Forge
movement `ORDER`. Mapping failures are caught and recorded fail-open; they cannot reject or alter normal Forge
resolution.

New resolution-time events are separated by event type and `selection_adapter = DISCARD`:

```text
CARD_SELECTION_DISCARD_CALLBACK
CARD_SELECTION
CARD_SELECTION_STATE
```

The existing cost-time `DOWNSTREAM / CARD_SELECTION` recorder in `chooseCardsForCost` is unchanged, retains its
ActionContinuation behavior, and is excluded from Discard metrics.

## Unsupported boundary

```text
all Discard modes except the narrow own-hand TgtChoose adapter
hidden identity-addressable selection
RevealNumber and UnlessType dependent paths
random/engine-selected discard as CARD_SELECTION
chooseCardsForCost migration
chooseCardsForEffect and related generic callbacks
ORDER, CONFIRMATION, MULLIGAN, combat decomposition
partial live-game application
AI heuristic legality
full observation/belief system
search, game copying, RL, models or network transport
```

No prerequisite blocker was encountered.

## Verification

The complete focused FRL decision suite passed after the final implementation changes:

```text
163 tests, 0 failures, 0 errors, 0 skipped
```

The dedicated final Forge discard-path regression also passed (`5` integration tests total). A second run with
diagnostics genuinely enabled recorded an intentional `INVALID_CONTROLLER_RESULT / RESULT_CARD_NOT_VALID`, then
completed normal `DiscardEffect` resolution and its hand/graveyard assertions unchanged. The package build passed:

```text
mvn -pl forge-gui-desktop -am -DskipTests package
BUILD SUCCESS
```

`git diff --check` passed. Tests cover the real Izzet script, exact-two construction, duplicate names, no partial
mutation, no repeat selection, deterministic identities/order, forced and DONE semantics, invalid domains,
timestamp staleness, same-name non-substitution, chooser/affected separation, visible supersets, hidden rejection,
normal Forge discard movement, null resolution continuation, shared selection identity, legacy cost separation,
diagnostic fail-open behavior, and generation instrumentation.

## Controlled benchmarks

Both requested packaged-artifact runs completed without changing AI behavior:

```text
Dead and Alive vs Air Forces: 10 games, seed 20260809, result 7-3
Izzet Guild Kit vs Dimir Guild Kit: 10 games, seed 20260810, result 5-5
```

| Matchup | Raw discard callbacks | Supported | Unsupported | Atomic requests | Forced | Strategic | Mean steps/callback |
|---|---:|---:|---:|---:|---:|---:|---:|
| Dead and Alive vs Air Forces | 0 | 0 | 0 | 0 | 0 | 0 | n/a |
| Izzet Guild Kit vs Dimir Guild Kit | 6 | 6 | 0 | 7 | 0 | 7 | 1.167 |

For Izzet/Dimir, atomic steps per callback were p50 `1`, p95 `2`, max `2`. Candidate counts were mean `3.714`,
p50 `3`, p95 `6`, max `6`. Generation latency was p50 `55.4 us`, p95/p99 `1.2214 ms`. One session in game 4
was the exact-two pattern with candidate shrinkage `6 -> 5`; both rows had blank `decision_sequence_id` and
`subdecision_index`, a shared non-null `selection_session_id`, and steps `0, 1`. No `CARD_SELECTION_STATE`
mapping failure occurred. Neither run contained legacy cost-time `DOWNSTREAM / CARD_SELECTION` events.

Raw CSVs are outside the repository:

```text
C:\Users\chris\AppData\Local\Temp\frl02f-dead-air-final-20260809.csv
C:\Users\chris\AppData\Local\Temp\frl02f-izzet-dimir-final-20260810.csv
C:\Users\chris\AppData\Local\Temp\frl02f-enabled-integration.csv
```

## Focused generation measurement

After 20 warmups, 180 fresh exact-two request generations over a seven-card callback domain measured:

| Metric | Value |
|---|---:|
| candidate counts across steps | 7, 6 |
| candidate mean / p50 / p95 / max | 6.5 / 6 / 7 / 7 |
| atomic steps/callback | 2 |
| per-step shrinkage | 0, 1 |
| generation p50 | 23.9 us |
| generation p95 | 40.9 us |
| generation p99 | 88.0 us |

This measures only neutral candidate/request generation, separately from Forge callback latency and policy
inference.

## Files changed

```text
forge-game/src/main/java/forge/game/ability/effects/DiscardEffect.java
forge-game/src/main/java/forge/game/decision/CardSelectionCandidateKind.java
forge-game/src/main/java/forge/game/decision/CardSelectionCard.java
forge-game/src/main/java/forge/game/decision/CardSelectionContext.java
forge-game/src/main/java/forge/game/decision/CardSelectionDecisionProvider.java
forge-game/src/main/java/forge/game/decision/CardSelectionSession.java
forge-game/src/main/java/forge/game/decision/DecisionRequest.java
forge-game/src/main/java/forge/game/decision/DecisionType.java
forge-game/src/main/java/forge/game/decision/DiscardCardSelectionAdapter.java
forge-game/src/main/java/forge/game/decision/LegalCandidate.java
forge-game/src/main/java/forge/game/decision/PriorityActionDiagnostics.java
forge-gui-desktop/src/test/java/forge/game/decision/CardSelectionDecisionProviderTest.java
forge-gui-desktop/src/test/java/forge/game/decision/CardSelectionDiscardIntegrationTest.java
forge-gui-desktop/src/test/java/forge/game/decision/PriorityActionDiagnosticsTest.java
docs/AI-ML DOCS/FRL_02F_CARD_SELECTION_REPORT.md
docs/superpowers/plans/2026-08-09-frl-02f-card-selection-boundary.md
docs/superpowers/specs/2026-08-09-frl-02f-card-selection-boundary-design.md
```

`ActionContinuation.java`, `PlayerControllerAi`, `PlayerControllerHuman`, the Izzet Charm script, and all forbidden
capability areas are unchanged.
