# ADR-0030 — ControlPlayer: a Scheduled Controller Redirect

- **Status:** Accepted
- **Date:** 2026-09-27
- **Deciders:** Crucible session (M6 porter round)

## Context

`ControlPlayer` (11 corpus lines, Mindslaver's own shape: "target player's decisions are made by another player's
controller for their next turn") is deferred (`effects-batch-b.md`) on two missing pieces:

1. **A per-player controller redirect.** `control.go`'s own doc comment: `PlayerController` methods take the deciding
   player as an explicit `PlayerID` rather than binding to one seat — there is no seam a control-changing effect could
   redirect. `Player.addController`/`removeController` (`Player.java`) route every decision through a different player's
   controller object for a duration; Crucible has no equivalent field.
2. **A phase-boundary schedule, both ends.** `ControlPlayerEffect.java:34-45` schedules the grant at a future phase
   boundary (`game.getBeginOfCombat()`/`getCleanup()`, chosen by `Combat$`) and the revoke at the one after
   (`getEndOfCombat()`/`getCleanup()`), via `addUntil` — a one-shot closure fired when that phase is reached, not an
   immediate effect.

Crucible already has a phase-boundary-scheduled mechanism close in shape: `delayedtrigger.go`'s `delayedTrigger` carries
`AtCleanup bool` (held inactive until this turn's cleanup, `Java's getCleanup().addUntil`) and `ForPlayer PlayerID`
(held inactive until that player's next turn), activated by `activateCleanupDelayedTriggers`/
`delayedTriggersOnNextTurn` (`delayedtrigger.go`). It is shaped for a **scripted triggered ability**
(`Trigger *compile.Ability`, an `Execute$` line) — `ControlPlayer`'s own scheduled action is not a script the card
triggers, it is the engine itself toggling a redirect.

## Decision Drivers

- GO-2: no package-level state; the redirect and its schedule live on `*Game`/`*Player`.
- GO-9: identity by `PlayerID`, never a bound controller object.
- PORT-2: no script re-interpretation — this is engine state, not a compiled ability.
- Reuse over duplication: the delayed-trigger machinery's phase-boundary gating (`AtCleanup`/`ForPlayer`) is the right
  shape; duplicating a second phase-boundary scheduler for one API is waste.
- Corpus-first: 11 lines, one dominant shape (grant at a fixed future boundary, revoke at the next one, `Combat$`
  choosing which pair of boundaries) — no reordering or stacking of multiple simultaneous redirects observed.

## Considered Options

**Redirect state:**

1. `Player.ControlledBy PlayerID` (`NoPlayer` = self), a single active redirect.
2. A per-player stack of `(timestamp, controller)` pairs, mirroring Java's own timestamp-keyed `addController`, to
   support nested/overlapping grants.

**Scheduling:**

1. Extend `delayedTrigger` with an optional non-script action (a typed enum + target fields) alongside its existing
   `Trigger *compile.Ability`, reusing `AtCleanup`/`ForPlayer` gating and the same activation call sites.
2. A separate, parallel `scheduledAction` list on `*Game`, gated by the same two phase-boundary conditions, evaluated at
   the same call sites as delayed triggers but through its own small switch, not through `compile.Ability`/
   `Registry.Resolve` at all.
3. A `Game` field per grant (`Game.pendingControlGrants []...`), checked inline in the turn/phase driver rather than
   through the delayed-trigger list.

## Decision

**Redirect state: Option 2.** A per-player stack of `(Timestamp int64, Controller PlayerID)`. Costs one slice field on
`Player`; nesting is cheap to support now and matches Java's own model (CR 800.4b: control reverts to the
second-most-recent grant when the most recent one's duration ends, not necessarily to the player themself) rather than
assuming grants never overlap, which 11 real lines don't prove either way.

**Scheduling: Option 2.** A new `scheduledAction` list on `*Game`, data-only (`Kind`, `Target`, `Controller PlayerID`,
plus the same `AtCleanup`/`ForPlayer` gating fields `delayedTrigger` already has), evaluated at the identical
phase-boundary call sites (`activateCleanupDelayedTriggers`, `delayedTriggersOnNextTurn`) via its own switch over
`Kind`. Kept parallel to, not merged into, `delayedTrigger`: `delayedTrigger` carries a `compile.Ability` and resolves
through `Registry.Resolve`; a scheduled controller grant/revoke is never a script line, so forcing it through the same
struct means either a nil `Trigger` field on every delayed trigger (wasted, and a nil-check every reader needs to add)
or a synthetic `compile.Ability` faked into existence to appease a struct field this action never uses. Reusing the
_gating pattern_, not the _struct_, keeps both readable and keeps `Registry.Resolve` as the only place a
`compile.Ability` is interpreted (PORT-2).

**Reading the redirect.** Every `PlayerController` call site that currently resolves "whose controller answers this
decision" from the deciding `PlayerID` directly instead resolves it from `Player.ControlledBy`'s top of stack, falling
back to the player's own seat when empty. This is additive at each of the ~40 existing call sites, not a signature
change — `PlayerController` methods keep taking an explicit `PlayerID`; only which `PlayerID`'s answers are asked
changes, at the one place decisions are dispatched, not at every caller.

## Consequences

**Good:** unblocks the 11 real `ControlPlayer` lines. The scheduling piece is a genuinely small, additive extension of
an existing pattern, not a new subsystem. The stack-based redirect state reproduces CR 800.4b's own revert-to-previous
behavior for free.

**Bad:** every future effect that asks "who decides for player X" must remember to resolve `ControlledBy` rather than
assume `X` decides for itself; a caller that reads `PlayerID` directly and dispatches to that seat's own fixed
controller bypasses the redirect silently. The implementing commit must audit and convert the dispatch point(s), not
just add the field.

**Neutral:** `scheduledAction` and `delayedTrigger` share gating logic but not a struct; a future third scheduled-thing
either grows a third parallel list (acceptable at this scale) or motivates unifying the gating logic into a shared
helper both structs embed — not decided here, revisit if a third case appears.

## Related

ADR-0013 (event schema), ADR-0019 Decision 5 (controller answers), `delayedtrigger.go`,
`docs/crucible/porting/port-log/game-state/effects-batch-b.md` (`ControlPlayer`'s prior deferred row, superseded by this
ADR unblocking it).
