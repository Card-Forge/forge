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

**Reading the redirect: no dispatch point exists to convert.** This port has one shared `PlayerController` for the whole
game — `Game.Step`/`Game.Run` (`driver.go`) and every effect take the same controller value, and `control.go`'s own doc
comment already says so: "a single ScriptedController answers for every player." The deciding `PlayerID` passed to a
`PlayerController` method identifies whose decision it is, not which object answers it; there is no per-seat controller
map or seam a redirect could route through. This matches Java's own mechanism more closely than the original wording
here implied: `ControlPlayerEffect.java:27-44` and `Player.addController`/`removeController`/ `getController`
(`Player.java:2517-2566`) never change who acts, targets, or owns the zones an ability touches — they change which brain
(`PlayerController`) is _built for the same slave player_ (`LobbyPlayerAi.createMindSlaveController` and
`LobbyPlayerHuman`, both construct a controller for the slave, not the master). The `PlayerID` passed everywhere in this
port stays the slave, exactly as Java's own activating player, `Defined$ You`, and hand/library ownership do.

So the redirect is read-only engine state consulted by name at the two real rules readers Java's own engine has, not a
routed dispatch: `Game.ControllingPlayer(pid) PlayerID` / `Game.IsControlled(pid) bool`, read by Learn
(`Player.java:3906` excludes Sideboard Lessons for a controlled player) and by Wish-family zone changes
(`ChangeZoneEffect.java:989` excludes `Sideboard` as an origin for a controlled player, live in this port's
`changezoneeffect.go` — 24 real `Origin$ Sideboard` corpus lines). A future harness or `PlayerController` implementation
wanting to route a different AI per seat reads `ControllingPlayer` itself to pick a brain; the engine performs no such
routing on its behalf.

**Combat$'s own pair of hooks.** Secret of Bloodbending's Combat$ True line needs a grant keyed to begin of combat
(`PhaseHandler.java:301`) and a revoke keyed to end of combat (`PhaseHandler.endCombat`, `PhaseHandler.java:1262`). This
is a second pair beyond the cleanup pair `delayedTrigger` already gates on. `scheduledAction` gains these as two more
gating cases. They are checked at `turn.go`'s own CombatBegin step and at `g.endCombat()` (`turn.go:466`) — the same way
the cleanup pair is checked at `activateCleanupDelayedTriggers` and `delayedTriggersOnNextTurn`.

**Ordering, both pairs.** Revokes fire before grants at the same boundary (`PhaseHandler.java:515-518`'s own comment,
"do this first for ControlPlayer") — Cruel Entertainment's mutual pair depends on it. This port's own turn-boundary call
order already does this (`delayedTriggersOnNextTurn` before `activateCleanupDelayedTriggers`, `turn.go:92-93`); the
combat pair's own call sites must preserve the identical revoke-before-grant order. A revoke scheduled while the current
boundary's own actions run must not itself fire at that same boundary (snapshot the list before running it,
`Phase.excute`'s own precedent). A grant is skipped if its controlling player has already left the game.

## Consequences

**Good:** unblocks the 11 real `ControlPlayer` lines. The scheduling piece is a genuinely small, additive extension of
an existing pattern, not a new subsystem. The stack-based redirect state reproduces CR 800.4b's own revert-to-previous
behavior for free. Reading the redirect needs no engine-wide audit: only two rules readers (Learn, Wish-family Sideboard
origin) consult it, matching how narrow Java's own two readers are.

**Bad:** every future effect asking "who decides for player X" must remember X stays the acting player throughout — only
a read that specifically needs to know _whose brain_ answers (today: none inside the engine) would consult
`ControllingPlayer`/`IsControlled`; a rules reader that should exclude something for a controlled player (a third case
beyond Learn and Wish, if the corpus ever needs one) and forgets to check `IsControlled` will silently diverge from Java
the same way Learn and Wish would have without this ADR.

**Neutral:** `scheduledAction` and `delayedTrigger` share gating logic but not a struct; a future third scheduled-thing
either grows a third parallel list (acceptable at this scale) or motivates unifying the gating logic into a shared
helper both structs embed — not decided here, revisit if a third case appears.

## Related

ADR-0013 (event schema), ADR-0019 Decision 5 (controller answers), `delayedtrigger.go`,
`docs/crucible/porting/port-log/game-state/effects-batch-b.md` (`ControlPlayer`'s prior deferred row, superseded by this
ADR unblocking it).
