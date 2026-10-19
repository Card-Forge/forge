# ADR-0019 — Interactive Priority (CR 117)

- **Status:** Accepted
- **Date:** 2026-09-25
- **Deciders:** `mc@archlab.pl`

## Context

`ResolveStack` (`stack.go:47-86`) plays out CR 117 for the one case this port has ever reached: no `PlayerController`
method lets a player respond to anything on the stack, so every priority pass is a pass in succession and the top item
always resolves next, with nothing new arriving on top of it in the meantime (`stack.go:55-63`). ADR-0018 built real
Instant/Sorcery casting on top of that same assumption and explicitly deferred interactive priority to a later ADR
(`0018-instant-sorcery-spell-object.md:31-36`) rather than risk an unlandable ADR bundling both.

This blocks:

- `Play` (330 corpus lines) and `CopySpellAbility` (255 lines) — ADR-0018 unblocked their dominant shapes, but the
  research doc's own table (`effects-play-copyspellability.md:14-18`) is explicit that a real response window is the
  next thing both need past their dominant shape.
- Most of the ~57 real remaining `PlayerController` gaps: `chooseSpellAbilityToPlay`/`playChosenSpellAbility` (the ask
  loop itself), `chooseTargetsFor`/`chooseNewTargetsFor` for a response, `payManaCost` as a response, and everything
  else in the casting infrastructure that exists only to serve a real priority window.
- `MustBlock`'s own deferred question (`effects-batches.md:696`): whether to trust, re-prompt, or error on a
  controller's answer that turns out to violate a legality check discovered after the fact. This ADR decides that
  question generally (Decision, point 5) — `MustBlock`'s own ADR applies it rather than re-deciding it.

The general CR 608.2b fizzle check (ADR-0018 Decision point 3, narrowed to an Aura's own `Target`) stays out of scope
here too: responses remove the premise ADR-0018 scoped it under ("no way to make a chosen target illegal between casting
and resolving yet"), but a general per-entity re-check (Java's `SpellAbility.canTarget(entity, fizzleCheck=true)`) is
its own unit, not bundled into this ADR (Consequences).

## Decision Drivers

- GO-2: no package-level mutable state — priority state (`pPlayerPriority`, `pFirstPriority` in Java) lives on `*Game`,
  scoped per game, the same as every other turn-structure field `turn.go` already carries.
- GO-8: no `any` — a controller's priority answer is a typed action, not a `map[string]string` or an interface grab bag.
- TEST-1: existing module tests and all 300+ scenario fixtures, `expect.events` included, must stay byte-identical with
  the default (empty-queue) answer — priority is additive, not a behavior change for every game that never queues a
  response.
- PORT-2: no new runtime interpretation — a response is still `Ability{API, Params, ...}` pushed through the existing
  `Registry`, the same as every other cast path.

## Considered Options

1. **One `PlayerController` method returning a typed pass-or-act result, asked in a loop.** Mirrors Java's
   `chooseSpellAbilityToPlay`/`playChosenSpellAbility` pair (`PlayerController.java:278-279`) collapsed into one round
   trip: ask, get back either "pass" or a chosen action, apply it if not a pass, ask the same player again until they
   pass. Matches this port's existing one-question-at-a-time `PlayerController` shape (every other decision method is
   already this shape) and needs no new concept beyond a zero-value-means-pass action struct.
2. **Two methods, split ask/act like Java.** Rejected: Java's split exists because `chooseSpellAbilityToPlay` also
   drives the human GUI's legal-action highlighting, a concern this port has none of (PORT-6, no UI). A single method
   removes a round trip with no loss — `ScriptedController`'s queue already answers "what, if anything, to do" in one
   shot for every other decision point.
3. **Model priority as its own state machine object, separate from `ResolveStack`.** Rejected: Java's `pPlayerPriority`/
   `pFirstPriority`/`givePriorityToPlayer` are three fields on `PhaseHandler`, not a separate type — replicating that as
   fields on `*Game` alongside the stack (`stack.go`, `turn.go`) keeps the same locality every other turn-structure
   field already has (GO-2's own reasoning), and a separate object would need its own way back into `*Game` for the
   stack, SBAs, and triggers it has to drive.

Option 1 chosen.

## Decision

1. **`PlayerController` gains one method**, `TakeAction(g *Game, pid PlayerID) Action` — the "priority" ask, named for
   the CR 117 concept, not Java's method name. `Action` is a typed struct (GO-8): a zero value means pass. `Action`'s
   fields carry exactly what a response can be today — cast a spell (`CastSpell`'s own path, Decision point 5's split
   makes an Instant legal here) or activate an ability (`ActivateAbility`'s own path) — reusing those two functions'
   existing signatures rather than inventing a third cast path. `ScriptedController` keys its queue **per player**
   (`[MaxPlayers][]Action`-shaped, indexed by `PlayerID` like `Game.players` already is — not a `map`, GO-12) rather
   than one shared FIFO: a single shared queue and "empty means pass" contradict each other the moment more than one
   player acts in a round. CR 117.3c lets a player keep priority and be asked again immediately, so a fixture queuing
   `[A's sorcery, B's Bolt]` in one FIFO would have A's own second ask (still A's turn to act, per 117.3c) pop B's entry
   by mistake. A per-player queue makes `QueueAction(pid, ...)` name who the answer is for, the same way a fixture
   already names a player for every other multi-player decision. `ScriptedController.QueueAction`/`TakeAction` use the
   existing `QueueX`/`ChooseX` naming, but not the existing exhaustion behavior: every other queued decision panics on
   empty (`scriptExhausted`, `control.go`'s own struct comment — "a queue running dry mid-game is a fixture-authoring
   mistake"), because each is asked a bounded, known number of times a fixture author can count. `TakeAction` is asked
   an unbounded number of times per priority round — CR 117.3c lets a player act repeatedly, and every round ends in a
   pass — so "nothing left queued for this player" is the overwhelmingly common, correct answer, not a mistake. An empty
   per-player queue answers pass for `TakeAction` specifically, a deliberate, documented exception to the panic
   convention, not an instance of it.
2. **A new public entry, `Game.PassPriority(reg *Registry, controller PlayerController) error`**, standalone — nothing
   existing calls it. It is not wired into `beginPhase` (`turn.go:189`): `beginPhase` takes no `*Registry`, and wiring
   it there would resolve phase triggers through the priority loop, changing every existing scenario's `expect.events`.
   Wiring the loop into the turn structure is a later commit; this one only adds the loop itself, callable directly by a
   test, a fixture verb, or (eventually) `AdvancePhase`'s own caller. The byte-identical invariant (Decision Drivers)
   holds by construction, not by a phase-body check.
   - Priority state is local to the call, not a new `Game` field: a `holder PlayerID` starting at the active player, and
     a consecutive-pass count. A one-field `Game.priorityPlayer` cannot by itself detect "priority has gone all the way
     around" — the loop needs to know how many players in a row passed, which a single current-holder field does not
     carry. Counting locally needs nothing persisted on `*Game` between calls, since the whole round runs inside one
     `PassPriority` call (CR 117.3c's "keeps priority" is loop-local too: an action resets the counter and the holder to
     the actor, it never has to survive past this call the way turn/phase state does).
   - Loop body, in order: `CheckStateBasedActions` (already the only pre-ask step this port's trigger design needs —
     `pushTriggeredAbilities` fires inline at trigger-detection sites, not from a waiting queue a priority ask would
     have to drain, unlike Java's `addAllTriggeredAbilitiesToStack`). Then ask `holder.TakeAction`. A non-pass answer is
     applied by calling `CastSpell`/`ActivateAbility` directly; a `false` return means the queued action failed a
     legality check `PassPriority` itself did not pre-derive (Decision, point 5) and becomes an error naming the player,
     the card and, for an activated ability, its index (GO-7: a bad queued action fails its own game, not the batch). On
     success, `holder` and the pass count reset to the actor (CR 117.3c — the caster keeps priority). A pass advances
     `holder` to the next live player (`nextPlayerAfter`, already skips a player who has lost) and increments the pass
     count; once it reaches the number of live players (recomputed each time — a state-based action inside the loop can
     remove one mid-round), CR 117.4 resolves **only the object on top of the stack**, not the whole stack —
     `resolveTop` (Decision, point 3) — then CR 117.3b gives priority back to the active player (or the next live player
     after them, if the active player has since lost, `PhaseHandler.java:1131-1135`) with the pass count reset to zero,
     and the loop continues. If the stack was already empty when the pass count reached the player count, the step ends
     and `PassPriority` returns. The loop's own guard is `!g.over`, checked going into each iteration — a state-based
     action can end the game mid-round.
   - No nested loops: a response only pushes and returns to the outer loop — the loop itself is the only place
     `CastSpell`/`ActivateAbility` get called from a priority ask. Both keep their existing push-only, gate-then-push
     shape (Decision, point 5) and stay callable directly the way every existing test and `actions.go` verb already
     calls them, unchanged.
3. **`ResolveStack`'s loop body is extracted into `resolveTop`** — pop, the existing fizzle check, dispatch, emit
   `AbilityResolved`, move a resolved spell's own source to the graveyard, then `CheckStateBasedActions`: exactly one
   pass through what is currently `ResolveStack`'s `for` body (`stack.go:70-83`). `ResolveStack` itself becomes
   `for len(g.stack) > 0 && !g.over { resolveTop(...) }` — byte-identical behavior, same signature, same callers,
   verified by the existing test suite passing unchanged. `PassPriority` calls `resolveTop` once per full pass-around
   (Decision, point 2), never `ResolveStack`: CR 117.4 only ever resolves the top object, then hands priority back
   before the next one — calling the to-empty `ResolveStack` from inside the loop would resolve everything currently on
   the stack in one pass-around with no response window between items, which is CR 117.3b's whole point to prevent.
4. **Which steps grant priority** (`Game.givesPriority(phase PhaseType, controller PlayerController) bool`, ported from
   `onPhaseBegin`'s own per-phase sets, `PhaseHandler.java:240-451`): not `Untap` (CR 502.4). `Cleanup` grants it only
   when `CheckStateBasedActions` finds something to do or the stack is non-empty (CR 514.3a) — `givesPriority` runs that
   check itself for `Cleanup` (it can only be known by actually running it, not cached), rather than `beginPhase`'s own
   Cleanup body carrying a separate repeat-check; `PassPriority` is not wired into `beginPhase` yet (Decision, point 2),
   so there is only one caller to check it today. Combat steps with no attackers/no damage to assign
   (`COMBAT_DECLARE_ATTACKERS` with zero attackers, a first-strike-damage step with no first strikers) do not grant it
   either in Java — this port's own Combat is thin enough that this rule is stated now and wired in once each of those
   steps has a real body to guard, not implemented in `givesPriority` itself yet. Every other step/phase grants it
   unconditionally.
5. **`CastSpell` and `ActivateAbility` gain a CR 307.1 timing check in place of their current blanket gate**, and a
   controller's chosen action that fails it is an error, not a silent skip or a re-prompt. Both functions today reject
   any cast/activate unless `pid == g.activePlayer`, the phase is `Main1`/`Main2`, and the stack is empty
   (`castspell.go:59-61`, `activateability.go:215-217`) — correct for a permanent, an Aura, or a Sorcery (CR 307.1
   itself), but it also blocks an Instant or an instant-speed activated ability from ever being cast as a response,
   which is the entire point of this ADR. The gate splits by object, checked inline where each function already returns
   `false` today:
   - Permanent, Aura, Sorcery: keep exactly the existing check (CR 307.1 — your turn, a main phase, empty stack).
   - Instant: no timing restriction (CR 307.1 already excludes Instants) — legal whenever `TakeAction`'s caller has
     priority to be asked at all, which `PassPriority`'s own loop already only does for a live player during a
     priority-granting step (Decision, point 4).
   - Activated ability: legal at instant speed unless its own top `A:AB$`/`A:AR$` line — `abilities[index]`, the exact
     `compile.Ability` `ActivateAbility` is asked to activate, never a `SubAbility$`/`DB$` chained off it or a spell's
     own `SP$` line — carries `SorcerySpeed$ true` or `Planeswalker$` (a loyalty ability), both read via
     `compile.Ability.Param(key)`, generic across every `APIType`'s param struct since the check runs before dispatch.
     `ActivateAbility` already reads `Planeswalker$` today (`activateability.go`'s `isLoyaltyAbility`/
     `LoyaltyAbilityActivated` once-per-turn check) but does not yet tie it to sorcery timing (CR 606.3) — this closes
     that gap for the top-line case. `destroyeffect.go:38`, `milleffect.go:31`, `sacrificeeffect.go:44`,
     `sacrificealleffect.go:25` and others each flag their own `SorcerySpeed$` param as "a cost-restriction flag with no
     cost-payment site to enforce it yet" on a `DB$`/`SP$` line those files compile, not necessarily the same occurrence
     this gate reads — a `SorcerySpeed$` written on a Sorcery's own `SP$` line (already sorcery speed by card type, so
     the param would be redundant there) or a chained sub-ability (which never gates casting/activating at all) stays
     exactly as unenforced as before this ADR. Only the shape `activateability.go`'s own gate reads —
     `SorcerySpeed$`/`Planeswalker$` on an activated ability's own top line — gets a real enforcement site here
     (Consequences).
   - The oracle (`ScriptedController`, or any future real controller) is never offered an action it cannot legally take
     in Java — `canCastTiming` only ever gates what appears in the legal-action set a controller chooses from, never
     re-validates after the fact (`SpellAbility.java:2596-2613`). This port has no legal-action enumeration
     (`TakeAction` just asks "what, if anything" and trusts the answer, the same stance every other `PlayerController`
     method already documents). `CastSpell`/`ActivateAbility` themselves keep their existing contract — `false` for
     every declined-by-the-rules case, timing included, the identical signature every current caller (every test,
     `actions.go`'s verbs) already relies on. `PassPriority` is the one caller that cannot treat a `false` here as an
     ordinary decline: the action came from a controller being asked what to do with priority, not from a test calling a
     cast function speculatively, so any `false` `PassPriority` gets back from applying a non-pass answer becomes an
     error naming the player, the card, and (for an activated ability) its index — no separate legality-checking logic
     duplicated on `PassPriority`'s own side, just turning the existing gate's answer into a hard stop where the caller
     is a controller's decision (GO-7: a bad queued action fails its own game, not the batch). This is the general rule
     `MustBlock`'s own ADR (Context) applies rather than re-decides: a controller's answer is trusted up to the legality
     checks this port can cheaply run inline; a check that would require re-deriving the full legal-action set (Java's
     own approach) is deferred, not silently skipped.
   - Existing tests asserting an Instant is rejected outside `Main1`/`Main2` (if any) are a deliberate CR 307.1 fix, not
     a regression — named as such in the implementing commit.
6. **No new event.** A pass is not telemetry-worthy on its own (ADR-0013's own volume argument — a pass happens far more
   often than every event this port already emits combined) and every existing `expect.events` fixture file must stay
   byte-identical for the empty-queue default (Decision Drivers). `AbilityActivated`/`AbilityResolved` already cover a
   response's own push/resolve.

**Explicitly out of scope:** mana abilities (CR 605, which do not use the stack and do not pass priority the normal
way); split second; a real `AIController` (M7); `MagicStack.undoStack`; `Play` and `CopySpellAbility` themselves (this
ADR only removes their remaining blocker); the general CR 608.2b fizzle check past an Aura's own `Target`
(Consequences); Flash — a non-Instant permanent carrying the `Flash` keyword (parsed, `keyword/defined.go:90`) should be
castable at instant speed too, but `canActSorcerySpeed`'s split only ever checks `cardtype.Instant`, so a Flash
permanent queued as a response fails CR 307.1's check exactly like any other permanent. A real gap, not a silent wrong
answer — `PassPriority` still errors rather than allowing it, so nothing plays out incorrectly, it is simply not yet
possible to cast a Flash permanent as a response.

## Consequences

**Good:** `Play`, `CopySpellAbility`, and the casting-infra bucket of `PlayerController`'s remaining gaps all unblock.
`ResolveStack` keeps its exact signature and to-empty behavior for every existing caller — only its body moves into
`resolveTop` (Decision, point 3), verified by the existing test suite passing unchanged. `PassPriority` is purely
additive: nothing existing calls it, so every current test and scenario stays byte-identical by construction.
`SorcerySpeed$`'s and `Planeswalker$`'s own top-line-on-an-activated-ability shape (Decision, point 5) gets its first
real timing enforcement as part of giving `CastSpell`/`ActivateAbility` a real check.

**Bad:** the general CR 608.2b fizzle check becomes load-bearing rather than a documented but currently-unreachable gap
— a response resolving above a targeted spell can now actually remove its target before this port has a general re-check
for it (only the Aura shape has one, ADR-0018 Decision point 3). Named as the next real unit once this lands, not solved
here: Java's own mechanism is a per-entity `canTarget(entity, fizzleCheck=true)` (`SpellAbility.java:1591`), not a
recompute-and-intersect of the candidate list — the shape that already broke `TestRemoveFromGameSpellOnStack` once
(ADR-0018 Decision point 3) and must not be reused for the general case either.

**Neutral:** `Game` gains no new field — the priority round's state (holder, pass count) is local to one `PassPriority`
call, not persisted turn-structure state. `PlayerController` gains one method, implemented by `ScriptedController` and
any future controller the same way every other method already is. `CastSpell` and `ActivateAbility`'s existing callers
that only ever exercise the main-phase, empty-stack, active-player case see no behavior change. One existing test did
not: `TestActivateAbilityDeclinesOutsideMainPhaseWithEmptyStack` asserted that a plain, unmarked activated ability
declines outside a main phase — exactly the blanket-gate behavior this ADR replaces. It is renamed and split into
`TestActivateAbilityInstantSpeedByDefault` (the same ability now activates during combat, correctly) and
`TestActivateAbilityDeclinesSorcerySpeedOutsideMainPhase` (an otherwise-identical ability marked `SorcerySpeed$` still
declines) — a deliberate CR 307.1 fix, not a regression, named as such in the implementing commit.

## Related

ADR-0027 (per-target CR 608.2b check), ADR-0026 (turn driver — wires this loop into the turn structure), ADR-0018
(instant/sorcery spell object — this ADR removes the priority gap it deferred), ADR-0013 (event schema — no new event
kind), ADR-0017 (`Registry` dispatch — a response still resolves through it, unchanged),
`effects-play-copyspellability.md`, `effects-batches.md:696` (`MustBlock`'s own deferred question, answered generally
here), [04-adr-process](../guidelines/04-adr-process.md)
