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
over). The reset empties the stack, so returning does not strand a partially-resolved one: all it can hold afterwards is
what `ReturnFromExile` itself pushed — a returned permanent's own enters trigger — which belongs to the restarted game
and resolves at its first priority window, where Java's trigger handler puts its waiting triggers on the stack too.
`priorityRound`'s own loop checks it too, so a still-open priority window does not continue asking the old active player
for actions in the new game.

**The caller's contract, documented on `Game.Restarted` itself:** seeing it true means calling `Game.ResumeAfterRestart`
before calling `Step`/`Run` again. It is Java's own restart loop body (`GameAction.java:2326-2381`): each live player
draws an opening hand from the library the reset already shuffled, mulligans (`PerformMulligans`) run starting with the
activator (carried on `Game.RestartedBy`, set alongside `Restarted`), the flag clears, and `StartTurn` begins the
activator's first turn. `DealOpeningHands` is not the entry point: it flips a coin, asks `ChooseStartingPlayer` and
shuffles again, three random-stream and controller calls Java's restart never makes, since its loop carries `first` over
as the activator (`GameAction.java:2380`). `Step`, `Run`, `PassPriority` and `ResolveStack` called while `Restarted` is
still true return an error. `NewGame` trigger firing is not built: every real `Mode$ NewGame` line sits on a
Command-zone card (`TriggerZones$ Command`), and the reset rejects any Command-zone card other than an effect or a
dungeon before acting, so no card that could carry one survives into the restarted game — the identical PORT-8
discipline as anywhere else a real Java step has no Crucible counterpart yet.

**`ExiledWithSource` lands as part of this same effect, not a separate unit.** A per-card mark on `Card` (zero value =
not exiled with anything), set at every path this port has that Java calls `handleExiledWith` from (`ChangeZone`,
`ChangeZoneAll`, `Dig`, `DigUntil`, `Heist`, `Airbend`), cleared on every zone entry, copied by `Game.Clone` with the
card. The reject-list sites in `cloneeffect.go`/`playeffect.go` lift the exact property. One new `valid.go` property
mirroring `CardProperty.java:397-411`'s `equalsWithGameTimestamp` comparison: the mark holds the host's `CardID` and the
host object's `zoneStamp`, and the property compares both against the host object an ability of the host sees. A plain
`CardID` equality is not enough even though this port's IDs are stable across zone moves: it would match a host that
left the battlefield and came back, a new object in Java that exiled nothing. The host object an ability sees is the
current one while the host is on the battlefield, the stack or in Command, and its last battlefield object once it has
left — Java's ability keeps its old host object, which is exactly what Karn's own `ReturnFromExile` compares against
after the reset has shuffled Karn into its library mid-resolution.

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
