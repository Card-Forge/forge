# FRL-02F CARD_SELECTION / Discard Selection Boundary Design

## Status and scope

Architecture approved on 2026-08-09. FRL-02F adds a generic, mutation-free sequential card-set primitive and adapts only the controlled resolution-time discard shape used by Izzet Charm.

The v0 adapter supports only `Mode=TgtChoose`, `chooser == playerDiscard`, selection from that player's own hand, no `RevealNumber`, no `UnlessType`, no earlier confirmation dependency, and callback `validCards` whose stable identities are all present in `visibleToChooser`. Other discard modes remain explicit future capability even when the primitive can model their data.

## Primitive

`CardSelectionSession` snapshots callback authority without changing Forge state:

- selectable identities come only from `validCards`;
- visible context comes only from `visibleToChooser`;
- stable card identity is `(cardId, gameTimestamp)`;
- chooser and affected player remain distinct fields;
- raw `min` and `max` are preserved;
- selected identities are session-local;
- `selectionSessionId` is provider/game-local, not globally unique;
- `selectionStepIndex` starts at zero and increments only for generated requests.

Generation follows:

```text
selectedCount < min       -> remaining SELECT_CARD candidates
min <= selectedCount < max -> remaining SELECT_CARD candidates + DONE
selectedCount == max       -> COMPLETE
```

`min=0,max=0` completes immediately. With no selectable cards and `min=0,max>0`, DONE is the single forced candidate. A required minimum that cannot be completed is an invalid state.

Every public candidate uses stable neutral fields and a deterministic semantic key. Card names and Java object identity never define identity. Duplicate named cards remain separate.

## Revalidation and completion

Before generation and application, every selected or remaining identity must still resolve to a card in the affected player's current hand with the same card ID and game timestamp. It must belong to the original callback-valid identity snapshot, remain permitted by the original visibility snapshot, and not already be selected. `Game.getCardState` may assist lookup but never replaces explicit timestamp and zone checks. No name fallback is allowed.

The provider does not rerun `DiscardValid` or reconstruct `DiscardEffect` legality. A side-effect-free `canBeDiscardedBy` check may only be an additional fail-closed guard. Forge's later `Player.discard` remains authoritative.

Applying SELECT_CARD changes session-local state only. Completion resolves the exact live selected identities into a new `CardCollection`; it does not move cards, change the hand, mutate the source ability, remember objects, or fire triggers.

## Visibility

Selectable identities and visible context are immutable, separate snapshots. Every identity-addressable selectable card must occur in the visibility snapshot or session creation returns `UNSUPPORTED_HIDDEN_CARD_SELECTION`. Visible but invalid cards are context only. Public descriptors never expose name or features for identities absent from the visibility snapshot.

## Continuation and session identity

Creating a session consumes no ActionContinuation index. Each generated request has its normal request ID and a session step index. If a real `ActionContinuation` is supplied when generating a request, only that generated request consumes an action subdecision index.

Resolution-time Izzet Charm has null `decisionSequenceId` and null action subdecision index, while its generated requests share a non-null provider/game-local selection session ID. Persisted uniqueness is the tuple `(processId, gameId, selectionSessionId)` or an equivalent scoped identity. Existing ActionContinuation semantics do not change.

## Narrow Discard adapter and AI diagnostics

`DiscardEffect` creates the session immediately before the existing controller callback for supported v0 shapes. It records a raw `CARD_SELECTION_DISCARD_CALLBACK`, invokes the unchanged controller, then validates and replays the returned set in deterministic semantic-key order through the neutral session. Replay records `CARD_SELECTION` steps or `CARD_SELECTION_STATE` failures with adapter `DISCARD`, session ID, and step index.

Replay never supplies legality, changes the AI result, or changes Forge behavior. Mapping failure is diagnostic-only and the original controller result continues to Forge. Forge still performs owner ordering and actual discard movement.

The existing `PlayerControllerAi.chooseCardsForCost -> DOWNSTREAM/CARD_SELECTION` recorder remains untouched and is never aggregated as a discard session.

## Verification

Focused tests cover exact-two sequential completion, DONE and forced semantics, callback-valid authority, duplicate names, timestamp-aware staleness, no partial mutation, visibility supersets and hidden rejection, chooser/affected separation in the primitive, null resolution continuation with session grouping, unchanged AI result on replay success or failure, and legacy cost/discard diagnostic separation.

Validation includes the focused FRL decision suite, package build, `git diff --check`, and ten seeded games for both controlled matchups where practical. The final report keeps callback, generation, synthetic-step, and native controller metrics separate.

## Explicitly unsupported

No ActionContinuation redesign, full discard taxonomy, `chooseCardsForEffect`, cost selection migration, CONFIRMATION, MULLIGAN, ORDER, combat, search, game copying, RL, model, Python, or network work is included.
