# ADR-0033 — RestartGame: Resetting the Running Game Mid-Resolution

- **Status:** Accepted
- **Date:** 2026-09-27
- **Deciders:** Crucible session (M6 porter round)

## Context

`RestartGame` (Karn Liberated's ultimate, 1 real corpus line) is deferred (`effects-batch-d.md`).
`RestartGameEffect.java` (read in full) does not spawn a new game — it mutates the **current** `Game`/`Player` state
almost entirely in place, while still inside the resolution of the ability that caused it: every player's zones (except
a `RestrictFromZone$` carve-out) shuffle back into a fresh library, the stack resets, all phase-boundary delayed
commands clear, monarch/initiative/daytime clear, `TriggerType.ChangesZone`/`Shuffled` are suppressed during the reset
(so the reset itself does not fire a wave of "a card changed zones" triggers), and the phase handler restarts with the
activator as the new active player. Java's own comment: "The rest is handled by phaseHandler" — the normal turn loop
simply continues afterward, now operating on the reset state.

This is not the same shape as `Subgame` (`Subgame` builds an unrelated `*Game` and drives it with the existing
`Game.Run`, ADR-0026 — no ADR needed there, since a second, independent `*Game` value has nothing to conflict with).
`RestartGame` alters the very `*Game` whose turn driver (`Game.Step`/`Game.Run`, ADR-0026) is, at the moment this effect
resolves, mid-priority-round, mid-resolution, inside `resolveTop`. ADR-0026 already names one narrower version of this
exact risk as an open gap (Decision 5): `EndTurn`/`EndCombatPhase` moving the phase non-driven during resolution is
"named as a gap, not silently right" — the driver's own bookkeeping (what step it thinks it's in, a Cleanup repeat's
pending state-based result) is not proven consistent after a phase jump mid-round. `RestartGame` is a far larger version
of the identical hazard: it resets essentially every field ADR-0026's driver reads.

## Decision Drivers

- ADR-0026's own driver contract: `Step`/`Run` assume the phase/turn state they read is the state their own prior call
  left it in. `RestartGame` breaks that assumption deliberately, mid-round, the same way (and further than) the
  already-acknowledged `EndTurn`/`EndCombatPhase` gap.
- GO-7: a card causing a reset must not leave the engine in a state a future call panics on; an inconsistency must
  surface as an error, not a crash three calls later.
- PORT-8: Java's own reset is a deliberate design (Karn's ultimate is meant to work this way) — reproduce it, not work
  around the resolution-in-progress hazard by declining to reset something Java resets.
- Corpus-first: 1 real line. Decide the general shape; do not build every zone/field Java resets if the real line's own
  reachability does not need it (verify at implementation time).

## Considered Options

1. **Reset immediately, inline, exactly where `RestartGameEffect.java` does — mid-`Resolve`, before returning.**
   Rejected outright as unexamined: the ability being resolved (`RestartGame` itself), the stack frame that pushed it,
   and any `SubAbility$` chain after it all reference state (`a.Source`, the stack's own remaining items) that the reset
   may have just invalidated — the same class of problem ADR-0026 Decision 5 flagged, now for a reset that touches the
   stack itself.
2. **Defer the reset to a safe boundary Crucible's driver already returns control at** (the end of the current
   `resolveTop` call, propagated up through `Game.Step`/`Game.Run` as a signal the driver checks before starting its
   next step), rather than mutating state inline mid-`Resolve`.
3. **Reset inline, but make `RestartGame`'s own `Resolve` the last thing anything reads from the old state**: perform
   the reset as the literal last statement of `Resolve` (matching Java's own structure, which does exactly this —
   nothing in `RestartGameEffect.java` reads game state after the reset section begins), and audit the call chain above
   it (`resolveTop`, any `SubAbility$` continuation) to confirm none of them dereference the resolved ability's own
   `Source`/stack position after `Resolve` returns for this specific API.

## Decision

**Option 3.** `RestartGameEffect`'s resolve is ported as Java structures it: the reset is unconditionally the last thing
this effect does, matching that "nothing reads state after" is already true in the source it is ported from.
`RestartGame` explicitly **rejects `SubAbility$`** (PORT-8/GO-7) rather than assume a chained ability can safely run
against reset state — Java's own real corpus line does not chain one, and Java's own resolve gives no indication a chain
after this reset would be meaningful (every zone a chained ability might reference has just been rebuilt).
`resolveTop`'s own post-`Resolve` bookkeeping (event emission, moving to the next stack item) is audited during
implementation to confirm it reads only the values it captured before calling `Resolve`, not fresh state afterward; any
read of post-reset state found there is fixed to use the pre-reset capture, logged in the implementing commit, not
silently left as a latent bug.

**What resets, scoped to the real corpus line.** All of: per-player zone shuffle into a fresh library (the
`RestrictFromZone$`/`RestrictFromValid$` carve-out, since Karn Liberated's own line uses it —
`RestrictFromZone$ Command | RestrictFromValid$ Card.YouCtrl+attractionType` keeps a controlled Attraction out of the
reshuffle), stack reset, phase-boundary delayed-command clears, monarch/initiative/daytime clear, and the phase handler
restarting with the activator as active player. `TriggerType.ChangesZone`/`Shuffled` suppression during the reset reuses
whatever trigger-suppression mechanism this port already has for a bulk zone move that must not fire per-card triggers
(check `Shuffle`/`MoveAll`-family effects for precedent before adding a new one).

**Not built:** anything Java resets that has no real corpus reader today (commander-specific stat resets, ring/blessing
state if unreached by the real line) — rejected as unresolved per the usual discipline, not silently skipped.

## Consequences

**Good:** unblocks `RestartGame`'s 1 corpus line without reopening or generalizing ADR-0026's driver contract — the fix
is local to this one effect's own resolve, not a change to how `Step`/`Run` work for every other card.

**Bad:** `RestartGame` becomes the second effect (after `EndTurn`/`EndCombatPhase`) whose resolve leaves the turn
driver's bookkeeping in a state ADR-0026 does not fully guarantee consistency for. A third such effect should prompt
generalizing this into ADR-0026 itself rather than a third one-off audit.

**Neutral:** rejecting `SubAbility$` is a real corpus-safe simplification today (0 real lines chain one), not a
permanent limitation — a future corpus card needing it reopens this specific question, not the whole ADR.

## Related

ADR-0026 (turn driver, Decision 5's phase-jump gap — the precedent this generalizes),
`docs/crucible/porting/port-log/game-state/effects-batch-d.md` (`RestartGame`'s prior deferred row, superseded here).
