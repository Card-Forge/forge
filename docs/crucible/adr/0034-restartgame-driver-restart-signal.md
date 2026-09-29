# ADR-0034 — RestartGame: a Driver Restart Signal, Not an Inline Reset

- **Status:** Accepted
- **Date:** 2026-09-28
- **Deciders:** Crucible session (M6 porter round)
- **Supersedes:** ADR-0033 in full. That ADR's own Option 3 (reset inline, as the last statement of `Resolve`,
  `SubAbility$` rejected) does not fit the real corpus line and understates the hazard; see Context.

## Context

`RestartGame` (Karn Liberated's ultimate) has exactly one real corpus line, `karn_liberated.txt:7`:

```text
A:AB$ RestartGame | Cost$ SubCounter<14/LOYALTY> | Planeswalker$ True | Ultimate$ True | RestrictFromZone$ Exile |
RestrictFromValid$ Card.!ExiledWithSource,Spell,Card.Aura | SubAbility$ ReturnFromExile | ...
SVar:ReturnFromExile:DB$ ChangeZoneAll | ChangeType$ Permanent.nonAura+ExiledWithSource | Origin$ Exile |
Destination$ Battlefield | GainControl$ True
```

ADR-0033 described a different carve-out (`RestrictFromZone$ Command`) and claimed the real line chains no
`SubAbility$`. Both are wrong: the carve-out is Exile, not Command, and the chained `ReturnFromExile` is the card's
whole point — everything Karn exiled with itself comes back under the activator's control. Rejecting `SubAbility$`, as
ADR-0033's Option 3 decided, would register the API while resolving nothing the real card actually does.

The deeper problem is what "the rest is handled by phaseHandler" (`RestartGameEffect.java:109`, ADR-0033's own Context)
actually means. It does not mean the normal turn loop continues on reset state. Java leaves the loop entirely:

1. `RestartGameEffect.java:105` sets `game.setAge(GameStage.RestartedByKarn)`.
2. After `resolveStack()` returns, `mainLoopStep` (`PhaseHandler.java:1150`) sees that age, clears the phase, fires
   `GameEventGameRestarted`, and returns.
3. `mainGameLoop` (`:1034`) exits on the same check.
4. `GameAction.startGame`'s own `do { ... } while (age == RestartedByKarn)` loop (`GameAction.java:2326-2381`) runs
   again: pre-opening-hand actions, drawing opening hands, mulligans, opening-hand actions, `TriggerType.NewGame`
   triggers, then `startFirstTurn(activator)`.

So Java's real order is: reset, then `ReturnFromExile` resolves against the reset state (the stack round finishes), then
the driver exits, then a fresh opening-hand/mulligan/first-turn sequence runs before play resumes. This port's
`Game.Step`/`Game.Run` (ADR-0026) assume the phase/turn state they read is the state their own prior call left it in;
mulligans and opening hands are explicitly out of scope for the driver, run by the caller before `StartTurn` (ADR-0026,
"Explicitly out of scope"). `RestartGame` needs the caller to run that same setup again, mid-game — a second, larger
instance of the "mid-round phase jump" class ADR-0026 Decision 5 already named as a gap for `EndTurn`/`EndCombatPhase`,
and the "third such case" ADR-0033 itself said should motivate a real fix rather than a third one-off audit.

**A genuine prerequisite is missing too.** `ExiledWithSource` — which card exiled which — has no counterpart in this
port. `ExiledWithSource` appears only in reject lists today (`cloneeffect.go:526`, `playeffect.go:64-69`); `valid.go`
has no case for it. Java sets it on every exile-by-host path (`SpellAbilityEffect.java:1090-1112`,
`PlaySpellAbility.java:534-535`, `CostAdjustment.java:274-275`, `Card.java:1571-1572`) and reads it back as
`card.getExiledWith().equalsWithGameTimestamp(host)` (`CardProperty.java:397-411`). Without it, `ReturnFromExile`'s own
`ChangeType$ Permanent.nonAura+ExiledWithSource` filter matches nothing, and the card does nothing at all even once the
reset itself works.

## Decision Drivers

- PORT-8: reproduce Java's real behavior for the one real corpus line, not a shape convenient to this port's current
  driver contract.
- ADR-0026's own contract: `Step`/`Run` must not silently continue on state whose invariants (turn count, whose hand was
  already dealt, whether mulligans already happened) the reset broke.
- GO-7: a caller that does not know to re-run setup after a restart must get an unmistakable signal, not a driver that
  quietly limps on with a half-reset game.
- Corpus-first: one real line. Build what it needs (the Exile carve-out, `ExiledWithSource`, the driver restart signal);
  reject anything Java resets that this line's own reachability does not exercise (commander-specific stats,
  ring/blessing state).

## Considered Options

1. **ADR-0033's Option 3: reset inline as the last statement of `Resolve`, reject `SubAbility$`.** Rejected: the real
   line chains `SubAbility$ ReturnFromExile`, so this would ship an API that never does what its one real user needs.
2. **A restart signal `Step`/`Run` return on; the caller re-runs setup and starts the activator's turn.** Matches Java's
   own control flow (leave the loop, redo setup, re-enter at turn 1) instead of forcing it through a loop built to
   assume continuity.
3. **Recurse: `RestartGameEffect.Resolve` itself calls the setup functions and `Game.Run` again, inline.** Rejected:
   nests a second driver run inside `resolveTop`, inside the first driver run's own call stack — the identical "no
   nested loops" hazard ADR-0019 already ruled out for priority, now for the whole turn driver. A panic or a runaway
   recursion three levels down would be far harder to diagnose than a signal the top-level caller checks once.

## Decision

**Option 2.** `RestartGameEffect.Resolve` performs the reset as the last thing it does (this part of ADR-0033's Option 3
still holds — nothing in Java's own resolve reads state after its reset section begins, so ordering the reset last is
still correct), then sets `Game.Restarted bool` before returning. `SubAbility$` is **not** rejected — `ReturnFromExile`
resolves normally against the post-reset state in the same stack round, matching Java's own order
(`AbilityUtils.resolveApiAbility` calls `resolve()` then `resolveSubAbilities` in the same call).

`Game.Step`, `Game.Run` and `ResolveStack` check `Restarted` immediately after any resolve call and return at once if
set (`Run` the same way it already returns early when a capped game stops — `Over()` false is not the only reason to
stop; `Restarted` true is a second one, and the two are mutually exclusive in practice since a restarted game is not
over). The stack itself is empty after `ReturnFromExile` finishes (nothing else was pushed during the reset), so
returning does not strand a partially-resolved stack. `priorityRound`'s own loop checks it too, so a still-open priority
window does not continue asking the old active player for actions in the new game.

**The caller's contract, documented on `Game.Restarted` itself:** seeing it true means deal opening hands
(`DealOpeningHands`), run mulligans (`PerformMulligans`), clear the flag, and call `StartTurn` with the activator
(carried on `Game.RestartedBy PlayerID`, set alongside `Restarted`) before calling `Step`/`Run` again. `NewGame` trigger
firing is not built — 0 real corpus lines have no other way to fire on a restart specifically today, so it is rejected
as unresolved rather than silently skipped, the identical PORT-8 discipline as anywhere else a real Java step has no
Crucible counterpart yet.

**`ExiledWithSource` lands as part of this same effect, not a separate unit.** A `CardID` field on `Card` (`NoCard` =
not exiled-with-anything), set at every path this port already has that exiles a card by a named source (the reject-list
sites in `cloneeffect.go`/`playeffect.go` are the two that need it lifted out of their own unresolved-param list; any
exile path this port has beyond those gets it too, found during implementation), copied by `Game.Clone`. One new
`valid.go` property, exact match against the ability's own host, mirroring `CardProperty.java:397-411`'s host-timestamp
comparison as a plain `CardID` equality (this port's `CardID` is stable across zone moves, so no timestamp is needed the
way Java's is — verify at implementation time whether Karn moving to the library mid-resolution changes which `CardID`
`ReturnFromExile` should compare against, since Java compares by game-timestamped object identity and this port's
stable-ID model may already sidestep the ambiguity the Java porter's own research flagged as needing an oracle check).

**What resets, scoped to the real corpus line:** every player's zones (except the
`RestrictFromZone$ Exile | RestrictFromValid$ Card.!ExiledWithSource,Spell,Card.Aura` carve-out) shuffle into a fresh
library; the stack resets; delayed-command state clears (this port's `delayedTrigger` list today, and `scheduledAction`
too if ADR-0030 has landed by the time this is implemented — check `git log` for `ADR-0030`'s implementing commit rather
than assuming either way); monarch/initiative/daytime clear; trigger suppression during the reset itself reuses whatever
this port's `Shuffle`/`MoveAll`-family effects already use for a bulk move that must not fire per-card triggers.

## Consequences

**Good:** the one real corpus line resolves fully, including the return-from-exile half that is the card's actual
payoff. The restart signal generalizes ADR-0026 Decision 5's already-acknowledged phase-jump gap into a documented,
checked contract instead of a third silent one-off, as ADR-0033 itself anticipated might be needed.

**Bad:** every caller of `Step`/`Run` (fixtures, `cmd/crucible`, the eventual M8 runner) must learn to check
`Game.Restarted` and re-run setup, or a restarted game silently stops advancing with no error — this is a new contract
obligation on every driver caller, not just an internal engine concern. `ExiledWithSource` is one more field every
future exile-by-host effect must remember to set, the same tax every such per-card marker imposes.

**Neutral:** `Game.Restarted`/`RestartedBy` are plain fields, carried by `Clone` like any other. Commander-specific
resets and ring/blessing clearing stay unbuilt — rejected as unresolved, not silently dropped, since the one real corpus
line never reaches them.

## Related

ADR-0026 (turn driver, Decision 5's phase-jump gap — this generalizes it for one larger case, not a rewrite of ADR-0026
itself), ADR-0019 ("no nested loops" — the reason Option 3 above is rejected),
`docs/crucible/porting/port-log/game-state/effects-batch-d.md` (`RestartGame`'s original deferred row).
