# Port Log — Game State: Turn, Stack, Combat

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)

Turn structure, the stack, mulligans, combat, block legality.

## Turn structure

`turn.go` is `PhaseHandler.java` (1,324 LOC), reduced to what does not need the stack, triggers or `SpellAbility`:
`Turn`, `ActivePlayer` and `ActivePhase` live on `Game` now (`StartTurn`, `AdvancePhase`, `SetTurnState`), and four
steps — Untap, Draw, CombatEnd and Cleanup — have real bodies (below). Every other step (`onPhaseBegin`'s Upkeep, Main,
the rest of combat, End of Turn) still just changes `ActivePhase` and nothing else, because casting, blocking and firing
a trigger all need machinery this port has not reached; CombatEnd's own action (CR 511.3, below) needed none of that,
just resetting state this port already has. `AdvancePhase` walks through the rest as bookkeeping only, until each one's
turn comes.

**Cleanup discards to hand size (CR 514.1) before clearing damage (CR 514.2).** `cleanupStep(controller)` gained the
`controller` parameter and a new `DiscardToHandSize` decision (`PlayerController`, control.go) once the active player's
own hand could actually need asking about: `Game.Zone(Hand, g.activePlayer).Len() > MaxHandSize` (a new constant, 7 — CR
103.4's default, used unconditionally since nothing this port can grant a modified or unlimited hand size yet, the same
continuous-effect gap the rest of the layer system has) is what decides whether to ask at all, the same "nothing
meaningful to decide" reasoning every other decision point in this port uses for an empty or already-satisfied set.
Unlike the damage clear right after it, CR 514.1 is scoped to the active player only — Forge's own `CLEANUP` case reads
`playerTurn`'s hand, not every player's, which is why `cleanupStep` checks just the one zone before its existing
every-player damage loop runs. The discarded cards go to their owner's graveyard the same way `resolveLegendRule` and
every "destroy" state-based action already move things — `g.Move(id, Graveyard, g.Card(id).Owner)`, not a new pattern.

Priority (`mainLoopStep`) is not here either, on purpose. With no stack and no `PlayerController` method that can cast
anything, asking a player "do you have a legal action" always answers no — building that loop today would be a stub
standing in for a decision no one can make yet, not a real one deferred. It lands with the stack.

Two rules came along because Draw needed them to mean something real rather than silently doing nothing:

- **CR 103.7a** — the first player skips the draw step of their own first turn in a two-player game.
  `turn == 1 && len(Players()) == 2`, the same condition Java's `isSkippingPhase` uses.
- **CR 704.5b** — an attempted draw with nothing to draw loses the game. `Player.DrewFromEmptyLibrary` records the
  attempt (a one-shot flag, cleared the moment `CheckStateBasedActions` reads it, matching Java's
  `triedToDrawFromEmptyLibrary`), checked first among the loss conditions per Java's own order — its comment cites
  Lich's Mirror, a card not ported, so today the order changes nothing observable.

`CheckStateBasedActions` runs after every phase entry, not just when a card script asks: `beginPhase` calls it right
after the step's own action, the same pairing `onPhaseBegin`/`checkStateBasedEffects` make at the top of `mainLoopStep`
(CR 704.3, "whenever a player would get priority").

Turn order skips a player who has lost (`nextPlayerAfter`), which is CR 800-something's "a player who has left the game
is skipped when play passes to them" — needed the moment a 3+ player game outlives its first loser, which
`CheckStateBasedActions` already supports.

**The top of the library is index 0** of the zone's order — a design decision, not a Java fact reproduced: nothing
established a convention before this, so `drawStep` set one. A fixture author writing `humanlibrary=Top;Next;...` names
it left to right, top to bottom, and `Load` already builds cards in that order, so drawing `Cards()[0]` and returning a
mulligan's tuck to the zone's end (bottom) both fall out of the existing `Zone`/`Move` behaviour with no new API.

Java's extra-turn stack is modeled (`nextActivePlayer`/`addExtraTurn`, `turn.go`, fed by `AddTurn` and `SkipTurn`), and
so are its extra-phase stack and skipped phases (`Game.extraPhases`/`consumeSkip`, fed by `AddPhase` and `SkipPhase`).
Not modeled, and each is a real rule some card will eventually need: topsy-turvy phase order (a handful of effects
reverse it), and CR 502.3's "this permanent doesn't untap" effects.

---

## Stack

`stack.go` is `forge-game/src/main/java/forge/game/zone/MagicStack.java` (1,025 LOC), cut down to CR 405's container and
CR 405.5/608's resolve loop. `PushAbility`, `StackLen`, `StackTop` and `ResolveStack` are new methods on `Game`, not a
new type: `Ability` already said "one resolvable ability on the stack" when `effect.go` landed it, so the stack itself
is `[]Ability`, last element on top. `ResolveStack` pops the top, dispatches it through the `Registry` the caller
supplies, emits `AbilityResolved`, then runs `CheckStateBasedActions` before popping what is now on top -- the same
pairing `beginPhase` already runs after a turn-based action.

`Ability` (and `APIType` with it) moved out of `effect.go` into a new `ability.go` to make `Game.stack []Ability` legal.
`Effect.Resolve(g *Game, a *Ability) error` already made the `effect` group depend on `game`; adding an `Ability` field
to `Game` itself would have made `game` depend on `effect` right back — a cycle `enginelint` is built to catch, not a
hole it missed. Splitting the vocabulary (`APIType`, `Ability`) from the dispatch machinery that resolves one against a
`*Game` (`Effect`, `Registry`) put the former below both `game` and `effect`, the same position `id.go` already holds
relative to everything else.

Most of `MagicStack.java` is still not here: `undoStack` (nothing to undo without an interactive priority pass to undo
it during) stays unbuilt. `addSimultaneousStackEntry` (CR 603.3b's "your own simultaneous triggers, in an order you
choose," plus APNAP order between different players' own triggers) is ported as `pushTriggeredAbilities` (trigger.go):
every one of the ten trigger-check functions collects its own matches into a `[]Ability` first (its "own" and "other"
halves feeding the same slice, where it has both) and calls `pushTriggeredAbilities` once at the end, rather than
calling `PushAbility` the instant each match is found. `pushTriggeredAbilities` walks `playersInAPNAPOrder` (trigger.go
— `Game.ActivePlayer()` first, then `nextPlayerAfter`, turn.go's own seating-order/skip-a-lost-player rule, until every
seated player has appeared once) and pushes each player's whole group in that order. Because the stack is LIFO and
`MagicStack.addAllTriggeredAbilitiesToStack` itself pushes the active player's group first (so it ends up on the
bottom), the group pushed LAST — the player immediately before the active player in turn order — is the one on top,
resolving FIRST; the active player's own group resolves last, ported faithfully from
`MagicStack.chooseOrderOfSimultaneousStackEntry`'s own player-iteration order. `Player.orderAndPlaySimultaneousSa`
itself — a player choosing the order among more than one of their OWN simultaneous triggers — is not ported: this port
has no `PlayerController` method for that choice ([`## Controller`](../game-state.md#controller)'s own "90 of 110
methods" gap), so a single player's own multiple matches stay in the deterministic order the caller found them (GO-12),
the same simplification every other real-choice gap already makes. This was caught and fixed via
`TestPlayLandPushesETBTriggersInAPNAPOrder` (trigger*test.go): before `pushTriggeredAbilities`, `otherETBTriggerMatches`
meant one card entering the battlefield could push both its own trigger and another permanent's own trigger watching for
it, off the same event, in a fixed order (the entered card's own trigger first, then every other battlefield permanent's
own, in `Players()`/zone-iteration order) rather than CR 603.3b's APNAP one — every real fixture and test before this
had at most one trigger fire per event, so the wrong order was never actually exercised until this test built one on
purpose. `freezeStack`/`unfreezeStack`, though, exist to serve a second ability arriving on top of one still resolving
-- and that much can happen now: `checkETBTriggers`/`checkDiesTriggers` (trigger.go) push from inside
`permanentEffect`/`attachEffect`/`PlayLand`/every graveyard-bound SBA's own resolution, so `ResolveStack`'s loop (below)
finds a second entry on top the moment the first one finishes, not from a second caller racing the first. Nothing needs
freezing because nothing is interactive: this port has no `PlayerController` method that lets anyone respond between one
ability resolving and the next being found on top ([`## Controller`](../game-state.md#controller)), so there is never a
window for a \_third* ability to arrive while the second is still open either. `effect.go`'s `Registry` no longer holds
zero implementations -- `permanentEffect` and `attachEffect` (castspell.go) are `CastSpell`'s own resolutions, and
`ResolveStack` has real (non-test) callers through both.

`ResolveStack`'s loop body is now `resolveTop` (below, ADR-0019): pop, fizzle check, dispatch, emit, move a resolved
spell's own source to the graveyard, `CheckStateBasedActions`. `ResolveStack` itself is `resolveTop` called to empty,
unchanged for every existing caller.

`AdvancePhase`/`beginPhase` never call `ResolveStack` or `PassPriority`: they are the bookkeeping walk, and a phase
trigger they push stays on the stack until a caller resolves it. Fixtures do that with `resolvestack` or `passpriority`
(`game-state-fixture.md`). The turn driver (below, ADR-0026) is the one path that opens priority rounds per step.

---

## Interactive priority: CR 117 lands

ADR-0019. Until now, `ResolveStack`'s own doc comment stated the honest limit: "no `PlayerController` method lets a
player respond to anything on the stack, so every priority pass is a pass in succession." `PassPriority` (`priority.go`)
is the real CR 117 loop that limit describes the absence of: each live player, starting with the active player, is asked
a new `PlayerController` method, `TakeAction`, in turn order; a pass moves to the next live player; a non-pass action
applies it and returns priority to the actor (CR 117.3c — Java's own `pFirstPriority`/`pPlayerPriority` reset,
`PhaseHandler.java:1079-1082`). Once everyone in a row has passed, `resolveTop` resolves the one object on top of the
stack (CR 117.4 — not the whole stack, which would remove the response window between items) and a fresh round starts
with the active player (CR 117.3b).

Priority state — who currently holds it, how many passes in a row — is local to one `PassPriority` call, not a new
`Game` field: the whole round runs inside one call, so nothing needs to survive past it the way turn/phase state does.
Java's `pFirstPriority` and `pPlayerPriority` are two fields serving two CR-distinct roles (the pass-counter anchor and
who gets priority after a resolution); this port collapses them into a `holder` local and a pass count because it has no
human GUI needing them tracked separately.

`TakeAction`'s zero value, `ActionPass`, is a pass — Java's own `chooseSpellAbilityToPlay` returning `null`
(`PlayerController.java:278`). `ScriptedController` keys its queue per player (`actionQueue []*[]Action`, indexed by
`PlayerID` like `Game.players` already is, GO-12 — not a `map`) rather than one shared FIFO: CR 117.3c lets one player
be asked twice in a row, which a single shared FIFO cannot express without a fixture author writing an explicit pass for
every intermediate ask. An empty per-player queue answers pass — the one `ScriptedController` decision that defaults
instead of panicking on empty (`control.go`'s own struct comment covers why every other one panics): CR 117.3c means
`TakeAction` is asked an unbounded number of times per round, and "nothing left queued for this player" is the ordinary
way every round ends, not a fixture mistake.

`CastSpell` and `ActivateAbility` traded their blanket main-phase/empty-stack/active-player gate for CR 307.1's real
split: a permanent, an Aura or a Sorcery still need it; an Instant never did (CR 307.1 excludes it); an activated
ability is instant speed unless its own top `A:AB$`/`A:AR$` line carries `SorcerySpeed$` or is a loyalty ability
(`Planeswalker$`), both read generically off the compiled `Ability`'s own params before dispatch
(`compile.Ability.Param`) — the same param written on a different line (a chained `SubAbility$`, or a Sorcery's own
`SP$` line where it would be redundant) stays exactly as unenforced as before this ADR; only the activated-ability
top-line shape gets a real enforcement site here. Both functions keep their existing `bool` contract unchanged — `false`
still means declined, the identical signature every existing caller (every test, `actions.go`'s verbs) already relies
on. `PassPriority` is the one caller that cannot treat that `false` as an ordinary decline, since the action came from a
controller answering a priority ask rather than a test calling a cast function speculatively: it turns a `false` there
into an error naming the player, the card and (for an activate) its ability index (GO-7).

A response can make a spell's target illegal before it resolves; CR 608.2b's per-target re-check handles it
([`## CR 608.2b`](targeting-and-chaining.md#cr-6082b-every-target-re-checked-at-resolution)).

---

## Turn driver: ADR-0026

`Game.Step`/`Game.Run` (`driver.go`) play whole steps through `PassPriority`'s loop, `priorityRound` (`priority.go`).
Ported from `PhaseHandler.mainGameLoop`/`mainLoopStep` (`PhaseHandler.java:1032-1160`). `AdvancePhase` stays
bookkeeping; both walks share `advanceStep`/`beginStep` (`turn.go`), and only `driven=true` adds the parts below.

| Step              | Driven turn-based action (before phase triggers)                   | Priority                                                                            | Java                               |
| ----------------- | ------------------------------------------------------------------ | ----------------------------------------------------------------------------------- | ---------------------------------- |
| Untap             | unchanged                                                          | never                                                                               | `PhaseHandler.java:251`            |
| DeclareAttackers  | `DeclareCombatAttackers`                                           | always, zero attackers included                                                     | `:305-312`                         |
| DeclareBlockers   | `DeclareCombatBlockers`; skipped with no attackers                 | always                                                                              | `:228-233`, `:314-319`             |
| FirstStrikeDamage | damage if `combatDamageAssigned(true)`; skipped with no attackers  | only if damage is assigned                                                          | `:321-332`                         |
| CombatDamage      | damage if `combatDamageAssigned(false)`; skipped with no attackers | only if damage is assigned                                                          | `:334-344`                         |
| Cleanup           | unchanged                                                          | only if its SBA check did something or the stack is non-empty; then another Cleanup | `:419-428`, `:446-450`, `:156-158` |
| every other step  | unchanged                                                          | always                                                                              | —                                  |

`drivenSkips` also skips turn 1's draw step in a two-player game (CR 103.7a, `:221-222`); `advance` still begins it and
`drawStep` draws nothing. A skipped step is not begun at all: no `PhaseBegan`, no phase triggers — Java's `skipped` path
(`:244-246`, `:436-441`), the same treatment `consumeSkip` gives a SkipPhase-skipped step. Cleanup's "SBA did something"
is `checkStateBasedActions`' `performed` result (`action.go`), Java's `performedSBA` (`GameAction.java:1412`, `:1614`):
each SBA helper reports whether it acted; rebuilding continuous effects and completing a dungeon do not count.
`Game.skipDamageSteps` carries the "nobody attacked" answer from DeclareBlockers to the two damage steps, as Java's
field of the same name does, and `Clone` copies it. `combatDamageAssigned` (`combatdamage.go`) is
`Combat.assignCombatDamage`'s return (`Combat.java:919-925`) computed before damage is dealt.

`Step` takes the current step's own priority window as played: `StartTurn` begins Untap, which grants none, so
`StartTurn` then `Run` plays a whole game. `Run` stops after turn `maxTurns`'s Cleanup with `Over()` false — M8 decides
what an unfinished game counts as. Java has no cap.

Three `Action` kinds make a driven step payable, since mana pools empty on every step (CR 500.4): `ActionPlayLand`
(`PlayLand`, CR 116.3), `ActionTapForMana` (`TapLandForMana` with `Action.Color`) and `ActionManaAbility`
(`ActivateManaAbility`). Each keeps priority, like a cast. A `Color` naming other than exactly one color is the
controller's error, not `TapLandForMana`'s invariant panic.

A queued `Action` is spent on the first priority round that asks its player, Upkeep included. A fixture that wants an
action in Main1 steps to Upkeep first (`step` from Untap on turn 1), then queues. Fixtures: every
`testdata/scenarios/driver-*`; Go tests in `turndriver_test.go` for what state cannot show (the Cleanup repeat).

Gaps, each in `game-state.md`'s `Not ported yet`: a Cleanup that `EndTurn` begins mid-round does not repeat (its SBA
result is discarded); `dealsInStep` reads keywords held now, not Java's "dealt first-strike damage" set
(`Combat.java:906-917`); no cap on actions within one priority round (M7, where Java put its own guard,
`PhaseHandler.java:1102-1105`).

---

## Mulligans

`PerformMulligans` (`mulligan.go`) is `MulliganService` plus `LondonMulligan` — one rule, not the five-class strategy
hierarchy Java has. London is the only one modern paper Magic has used since 2019 and the only one the constructed
corpus Crucible targets exercises; Original, Paris and Vancouver are the rules it replaced, and Houston is a
Forge-specific casual variant. Porting a hierarchy for four rules nothing in scope calls is exactly the speculative work
CLAUDE.md rules out (PORT-6) — a straight function reads better than an interface with one real implementation.

It calls the two `PlayerController` methods M4 built and never used: `MulliganKeepHand` and `TuckCardsViaMulligan`.
Building the controller interface ahead of its callers, the way `ChooseStartingPlayer` was already sitting there unused,
is exactly what paid off here.

Two real rules came with it:

- **CR 103.4** — a game with more than two players gives every player one free mulligan. Heads-up London gives none: the
  very first mulligan already costs a card.
- **The last-offered mulligan can cost more than a fresh hand holds.** `LondonMulligan.canMulligan`'s bound
  (`tuckCardsDuringMulligan() <= maxHandSize`) reads the mulligan count from _before_ the mulligan it is gating, one
  step behind what that mulligan will actually cost once taken — Java's own code, not a port artifact. The practical
  effect is the last offered mulligan can ask a seven-card hand to tuck eight. `mulligan()` clamps the tuck count to the
  hand's actual size before asking `TuckCardsViaMulligan` for it — a defensive floor, not a rules change: tucking
  everything and tucking "everything, and then some" both leave an empty hand.

`Player.shuffle` needed `OrderedSet` to support reordering at all, which it could not: `Add` only appends, and nothing
before this needed to put a zone's cards in anything but insertion order. `OrderedSet.Swap(i, j)` is the addition —
exchanges two positions and keeps the lookup index in step — and `Game.Shuffle` drives it with `javarand.Rand.Shuffle`,
the same `Collections.shuffle(list, MyRandom.getRandom())` call Java's `Player.shuffle` makes, so a shuffled library
replays identically from the same seed (pkg/javarand's own P0 gate covers the algorithm; this is the first caller that
exercises it against a real zone).

**`DealOpeningHands` (`mulligan.go`) is the flow that was missing: `GameAction.startGame`'s pre-mulligan half,** with
every `Match`-level part trimmed. It decides who plays first (CR 103.2's coin flip — `g.rand`'s own
`Int32n(len(players))`, `Aggregates.random`'s algorithm for a `List` source, the exact call Java's own
`Aggregates.random(game.getPlayers())` makes), shuffles every library, and deals each player `startingHandSize` cards.
It does not call `PerformMulligans` itself, and does not start the first turn either — both stay separate, explicit
calls a caller makes with the returned first player, the same way `DeclareCombatAttackers` and `DealCombatDamage` stayed
separate functions rather than one that "plays a combat": `PerformMulligans` already has its own test suite built
against hands dealt directly (no RNG in the loop to predict), and coupling the two would break that.

**This port has no `Match`, so `isFirstGame` is always `true`.** Java's `determineFirstTurnPlayer` only reaches the coin
flip when there is no previous game's loser to name (`lastGameOutcome == null`); every other branch — Puzzle, Archenemy,
Power Play — is a format variant nothing in the corpus this port targets uses. A future `Match` that plays more than one
game and needs "the loser of the last one goes first" is new work for `DealOpeningHands` to grow into, not a shortcut
taken here.

---

## Combat: declaring attackers, declaring blockers, and dealing damage

`combat.go`/`attack.go`/`block.go`/`combatdamage.go` are CR 506-510, cut down to CR 508.1's declare-attackers step (now
including 508.1d's attack-target choice), CR 509.1's declare-blockers step, and CR 510.1-510.4's combat damage — both
the first-strike sub-step and the regular one — plus CR 702.19's trample. This is the whole of Combat this port has
reached; not every keyword or restriction is (below). `Combat` (combat.go) is the game's own combat state, currently
`Attackers []CardID`, `AttackTargets map[CardID]EntityID` and `Blocks []Block`; it is a new `Game` field
(`combat Combat`), cloned and cleared the same way `Card.PT`/the stack already are, and split into its own file for the
same reason `ability.go`/`layer.go`/`pt.go` are: `Game` needs the `Combat` type for its own field, and
`Game.DeclareCombatAttackers` (attack.go)/`Game.DeclareCombatBlockers` (block.go)/`Game.DealCombatDamage`/
`Game.DealFirstStrikeDamage` (combatdamage.go) need `*Game` — one of them has to sit below the other in the dependency
graph, or `enginelint` catches the cycle the same way it already has three times this milestone. `control.go` needed to
move into the `combat` group's own allow-list too, once `PlayerController.DeclareCombatBlockers` had to name `Block` in
its signature; `combatdamage`'s own group needed `parts` added to its allow-list, the first combat file to touch
`Card.Damage`; `block`/`combatdamage` both needed `attack` added once they started calling `defenderOf` (below).

`Game.DeclareCombatAttackers` computes eligibility itself — untapped, and either no summoning sickness or haste (CR
302.6) — rather than trusting the caller, the same "the game decides what is legal, the controller only decides among
what is offered" split `PerformMulligans` already uses for `MulliganKeepHand`. A declared attacker taps unless it has
vigilance (CR 508.1f) — `Card.HasKeyword`'s second real caller, after `destroyDamagedCreatures`'s `Indestructible`
check.

**Named `DeclareCombatAttackers`/`DeclareCombatBlockers`, not `DeclareAttackers`/`DeclareBlockers`.** `PhaseType`
already has constants with both of those exact names (phase.go) — Go allows a method and a package-level constant to
share a name, since methods live under their receiver's own namespace, but `enginelint`'s plain-identifier matching does
not tell the two apart, and neither would a reader skimming for one and finding the other. `enginelint` caught this
twice, once per method: `attack.go`'s `DeclareAttackers` first, and `block.go`'s `DeclareBlockers` the same way when it
landed. The method, the `PlayerController` interface method and the `ScriptedController` implementation are all renamed
in both cases; the phase constants, the actual CR 508/509 steps these methods are one piece of, keep their own names
unchanged.

**Blocking does not tap the blocker.** CR 508.1f taps an attacker; CR 509 has no equivalent step for a blocker, so
`Game.DeclareCombatBlockers` never touches `Card.Tapped`. Gang blocking (CR 509.1c) is unrestricted on the attacker side
— `Block` is a flat `[]Block` of `{Blocker, Attacker}` pairs, and more than one pair naming the same `Attacker` is
ordinary, not a case the code has to special-case.

**Flying/reach, Fear, Horsemanship, Intimidate, Landwalk, Protection, Skulk, Menace, and every real
`S:Mode$ CantBlockBy` line are checked.** `Game.DeclareCombatBlockers`'s own eligibility computation is still only
"untapped creature the defending player controls" — `CantBlockBy` is a property of one attacker/blocker pair, not of a
creature in isolation, so it is checked afterward instead, via `CanBlock`, against the controller's own answer
([`## Block legality: CantBlockBy`](#block-legality-cantblockby) has the full account). Menace is a per-attacker blocker
count checked on the whole declaration (`validateBlocks`), not a `CanBlock` one, for the same reason Forge itself does
not run it through the static-ability engine either. An illegal answer is an `*IllegalDeclarationError`, never a dropped
pairing ([`effects-mustblock.md`](effects-mustblock.md)). One exception: a defender an `Event$ DeclareBlocker`
replacement (Camouflage) applies to is not asked at all, and the blocks the replacement declares are repaired by Java's
steady-state loop, not validated ([`effects-camouflage.md`](effects-camouflage.md), ADR-0035).

**Not wired into `AdvancePhase`'s automatic walk through the phases.** `PerformMulligans` is the standing precedent for
a real M5 mechanic a scenario calls explicitly (`actions.log`'s own `declareattackers`/`declareblockers` verbs) rather
than one the turn structure invokes on every entry to that phase — the same "stub standing in for a decision no one can
make yet" reasoning `turn.go`'s own comment already gives for keeping `ResolveStack` out of `beginPhase`. Most games
reaching the DeclareAttackers phase attack with nothing at all; auto-wiring would mean every such phase entry pays the
cost of asking a question with an empty answer set almost every time.

**No eligible creature means the controller is never asked.** The same reasoning applies on both sides:
`Game.DeclareCombatAttackers` skips the question when the active player has nothing eligible, and
`Game.DeclareCombatBlockers` skips it both when there are no attackers at all and when the defending player has nothing
untapped to block with — there is nothing meaningful to decide, so nothing is queued for it. This is also what lets
every existing scenario and test that walks through combat without ever creating a creature keep working without queuing
an attackers or blocks answer it was never going to need.

**Attack targets: CR 508.1d generalized "who's defending" into "what's being attacked."** `assignAttackTargets`
(attack.go), called from inside `DeclareCombatAttackers` right after attackers are chosen and tapped, gives every
declared attacker an `EntityID` target — a player, or a planeswalker/battle that player controls
(`eligibleAttackTargets`). CR 508.1d makes "which creatures attack" and "what each attacks" one combined announcement,
not two sequential decisions, which is why this lives inside `DeclareCombatAttackers` rather than as its own
`actions.log` verb: `Game.DeclareCombatAttackers`'s own public signature and return value (`[]CardID`, which creatures
attacked) don't change at all, only a new side effect and, sometimes, a new controller call get added.

A lone eligible target — any two-player game with no planeswalker or battle on the other side, the case every existing
scenario before this one was — is assigned automatically, without ever calling `ChooseAttackTarget`. This is the load-
bearing backward-compatibility property: every fixture and test written before attack targets existed keeps passing
unmodified, because none of them gives an opponent a second thing to be attacked, so the ask branch never fires and
`ScriptedController.attackTargets` never has to hold anything. More than one eligible target — a planeswalker/battle
present, or (multiplayer) more than one living opponent — asks `ChooseAttackTarget` once per attacker, trusted the same
way `ChooseLegendaryToKeep`'s answer is.

\*\*`defenderOf` (attack.go) is what `DeclareCombatBlockers` resolves an attacker's own `AttackTarget` to the player who
can legally block it (CR 802.4a): itself, if the target is a player; the target's controller, if it's a planeswalker or
battle. `dealCombatDamageStep` (combatdamage.go) never needed it at all — it walks `g.combat.Attackers` and
`g.combat.Blocks` directly, resolving each attacker/blocker pair on its own terms, so it never assumed one defender to
begin with. `DeclareCombatBlockers` used to: it called `defenderOf` once, for `g.combat.Attackers[0]`, and asked only
that one player to declare blocks for every attacker. It now calls `defenderOf` per attacker, groups by the result, and
asks each distinct defender in turn — only about the attacker(s) actually attacking them, offering only their own
eligible creatures (CR 506.4's "each defending player" read per defender rather than assumed singular). Defenders are
asked in the order their first attacker appears in `g.combat.Attackers`, so the sequence is deterministic across a run
(GO-12) — the same reasoning `resolveLegendRule`'s own `order` slice exists for. A defender with no eligible creature is
skipped, not asked with an empty list, matching `DeclareCombatAttackers`'s own "nothing meaningful to decide" reasoning
for an inactive player. The scenario fixture `combat-split-across-two-defending-players` (`game-state-fixture.md`) is
the first in the corpus to seat three players and exercise it end to end.

**Attacking a planeswalker or battle changes how combat damage lands, not who deals it.** `dealAttackTargetDamage`
(combatdamage.go) is the dispatcher every "damage past the last blocker" call site (unblocked, trample overflow) now
goes through, in place of always calling `dealPlayerDamage`: a player target still reduces `Life`, but a
planeswalker/battle target goes to `dealPermanentDamage` instead. `dealPermanentDamage` (renamed from
`dealCreatureDamage`, which it still does everything of) removes loyalty or defense counters for a planeswalker or
battle target (CR 120.3c, 121.5) in addition to — not instead of — marking `Card.Damage` if the target is also a
creature, the same independent-checks shape Forge's own `Card.addDamageAfterPrevention` uses for a card that's more than
one type at once. No new state-based-action work was needed: `destroyZeroLoyalty`/`destroyZeroDefense`
([`## State-based actions`](state-based-actions.md#state-based-actions)) already existed and already read
`Counters.Count(Loyalty/Defense)`, so removing counters via combat damage is all it took to make them fire for real
instead of only in tests that added counters by hand.

**Combat damage is the first thing that actually deals damage.** Every earlier state-based action reading
`Card.Damage.Marked`/`Deathtouch` (`destroyDamagedCreatures`) only ever saw what a test had marked directly —
"`Damage.Mark`'s only callers are tests" (below, "Not ported yet") stops being true here. `dealCombatDamageStep`
(combatdamage.go), the shared body behind both `DealFirstStrikeDamage` and `DealCombatDamage`, computes every attacker's
exchange one at a time rather than computing all amounts first and applying them together: CR 510.2 makes a single
step's damage simultaneous, but nothing this port has built triggers off damage being dealt or reads a life total
mid-step, so the two orders are indistinguishable to anything that can currently observe them. An unblocked attacker
deals its power to whatever it's attacking (`dealAttackTargetDamage`, below). A single blocker exchanges full power for
full power automatically; a gang-blocked attacker (more than one live `Block` naming it) asks its controller to divide
its power via `AssignCombatDamage` (CR 510.1c) — trusted the same way `ChooseLegendaryToKeep`'s answer is, including the
"lethal before moving on" ordering constraint CR 510.1c itself imposes. `DamageDealt` and `LifeChanged` both wire here
for the first time (below, "Events, wired"), each attributed to `Source` (the dealing card) and flagged `FlagCombat`,
plus `FlagDeathtouch` when the source has that keyword.

**First strike (CR 510.4) is one function asked twice, not two functions.** `dealsInStep(c, firstStrike)` is the whole
of it: a creature with "First Strike" acts only when `firstStrike` is true, "Double Strike" acts either way, everything
else only when it's false. `DealFirstStrikeDamage` and `DealCombatDamage` are that same body called with `true` and
`false` — a fixture with no first striker at all can still call `DealFirstStrikeDamage` and get a real no-op back
(nobody's `dealsInStep` returns true), the same "the game decides what is legal, ask anyway" reasoning
`DeclareCombatAttackers` already applies to an empty eligible list. The two steps are separate `actions.log` verbs
(`firststrikedamage`, `combatdamage`, `game-state-fixture.md`), not one call that internally loops twice, because a real
state-based-action check has to happen between them — a first-strike kill has to be dead before the regular step asks
whether it still deals or receives anything — and that check already happens for free: `beginPhase` runs
`CheckStateBasedActions` on every phase entry ([`## Turn structure`](#turn-structure), `turn.go:275`), so a scenario
that `advance`s from `FirstStrikeDamage` into `CombatDamage` between the two verbs gets the kill applied without a new
verb invented just for it.

**A creature that left the battlefield between the two steps deals nothing and receives nothing.** This was a
documented, genuinely unreachable gap before first strike existed — nothing could kill a creature between
`DeclareCombatBlockers` and combat damage. First strike makes it reachable: `alive` (`Card.Zone == Battlefield`) gates
every attacker at the top of `dealCombatDamageStep`, and a dead attacker's blockers are filtered out before either side
of its exchange runs, so a creature killed by a first-strike blow neither swings again in the regular step nor gets hit
by something that's no longer there to hit it. `wasUnblocked` (`dealAttackerDamage`'s own parameter) tracks whether an
attacker was ever blocked at all, separately from whether it currently has zero live blockers — CR 510.1c treats "never
blocked" (hits the player) and "blocked, but every blocker has since died" (hits nobody, no trample) as different
outcomes that happen to look the same by the time only `len(liveBlockers) == 0` is left to check.

**Trample (CR 702.19) changes only how an attacker's own power splits, not who decides.** Against a single live blocker,
`lethalDamage` computes the minimum this port can assign it — the game deciding, since no decision was being asked in
that case anyway — and the rest goes to the player; against a gang-blocked attacker, whatever the controller's
`AssignCombatDamage` answer leaves unassigned across all its named blockers goes to the player instead of being wasted
(a non-trampler's own unassigned remainder is still wasted, unchanged from before trample existed). An unresolvable
toughness (`Toughness`'s own `*`/`Count$` gap) makes `lethalDamage` unable to compute lethal at all; its caller treats
that as "not trampling this blocker" — full power assigned to it, nothing guessed at — the same conservative default
`Toughness`'s own `ok`-false already gets everywhere else in this port, not a new one invented for trample. CR 702.19e's
"every blocker gone by the time damage is assigned" case — reachable the same way the paragraph above is, a trampler's
blocker dying to first strike — sends the attacker's full power to the player.

**A Battle's protector (CR 704.5w/704.5x) is a state-based action, not part of declaring attackers, even though it gates
whether one can be attacked without asking.** `assignBattleProtector` (action.go) runs in `CheckStateBasedActions`, not
here, because CR 704.5w fires independent of combat entirely — a freshly-played Battle needs a protector chosen before
anyone ever attacks it. `attackersOf` (attack.go) is what lets it ask CR 704.5w's own question ("is anyone currently
attacking this Battle") without duplicating `Combat.AttackTargets`' own bookkeeping: a reverse lookup by `EntityID`
rather than a new field, general enough that a player target works with it too even though only a Battle needs to ask
today. Only the Siege shape is implemented (every Battle in the compiled corpus prints that subtype; action.go's own doc
comment has the reasoning), and CR 704.5w's "no eligible opponent, destroy the Battle instead" fallback is real code
with no test behind it — provably unreachable as long as `CheckStateBasedActions`'s own win-condition check keeps
running first (its own doc comment works through why), kept anyway because Forge keeps it too, for the same "not
reachable given today's engine, but a fully specified rule" reason (Forge's own comment: "unless range of influence gets
implemented").

**`endCombat` (turn.go) is CR 511.3, wired as the End of Combat step's body.** `beginPhase`'s switch picked up a fourth
case ([`## Turn structure`](#turn-structure)): the other bookkeeping-only steps (Upkeep, Main1, the rest) stay empty
because they need the stack, triggers or `SpellAbility` to do anything, but CombatEnd's real action — every creature and
planeswalker/battle stops being attacking/blocking — needs none of that, just resetting `g.combat` to its zero value the
same way Java's `PhaseHandler.endCombat` sets its `Combat` field to `null`. Before this landed, nothing cleared
`Combat.Attackers`/`AttackTargets`/`Blocks` between combats at all: `DeclareCombatAttackers` and `DeclareCombatBlockers`
only overwrite `g.combat` on the branch where something is actually declared, and both return early without touching it
when nothing is eligible (the same "nothing meaningful to decide" shortcut that makes them cheap to call
unconditionally) — a real combat's data would have silently survived into a later turn that never attacked with
anything, latent because no fixture or test happened to play two turns of combat in the same game before this one.

Not here yet: block legality beyond "untapped creature the defending player controls" — Flying/reach, menace,
protection, "must be blocked by" — waits on the general static-ability engine, above.

---

## Block legality: CantBlockBy

`block.go`'s own doc comment had named the gap precisely: in Forge, CR 509.1b's restrictions — flying/reach, Fear,
Horsemanship, "can't be blocked except by," every other one — all run through one general mechanism,
`StaticAbilityCantAttackBlock.cantBlockBy`/`applyCantBlockByAbility`, not a keyword-specific check. `cantBlockBy`
(`staticability.go`) ports that mechanism; `CanBlock` (`block.go`) is the new public predicate combining it with the
existing "untapped creature you control" base rule.

Flying's own restriction turns out not to be a literal `S:` line at all for the overwhelming majority of the 3,276 real
corpus cards that carry it: `CardFactoryUtil.java:3910-3913` synthesizes
`Mode$ CantBlockBy | ValidAttacker$ Creature.Self | ValidBlocker$ Creature.withoutFlying+withoutReach` from the bare
`Flying` keyword at `CardState`-build time, the identical mechanism `Fear` (`:3906-3909`, 40 cards), `Horsemanship`
(`:3932-3935`, 29 cards) and `Intimidate` (`:3939-3942`, 23 cards, below) use. `cantBlockByKeywords`
(`staticability.go`) reproduces that synthesis for those four keywords, keyed off `Card.HasKeyword` rather than a
literal compiled `S:` line — nothing in `internal/carddb/compile` performs Java's own keyword-expansion step, so this
port's own version of it has to live here instead, one call site rather than a compile-time rewrite. `Menace` (408
cards) is not among them: Forge itself does not run Menace through this engine either — `getMinMaxBlocker` hardcodes
`attacker.hasKeyword(Keyword.MENACE)` directly, a minimum-blocker-_count_ rule a per-pair `CantBlockBy` check cannot
express. `minMaxBlockers` (`blockvalidation.go`) reproduces that same hardcoding, with `Mode$ MinMaxBlocker`'s own
counts on top, and `validateBlocks` checks every attacker's blocker count on the whole declaration: a Menace attacker
declared blocked by one creature makes the declaration an `*IllegalDeclarationError` (CR 702.111b makes the whole
attempt illegal to declare, not partially legal; [`effects-mustblock.md`](effects-mustblock.md)).

`Intimidate` (23 cards) is now among `cantBlockByKeywords`, once `SharesColorWith` had somewhere real to live: its own
`ValidBlocker$ Creature.nonArtifact+!SharesColorWith` needed a `SharesColorWith` property `propertyMatches` (`valid.go`)
did not evaluate — before this landed, the generic `non<Type>` fallthrough it would otherwise reach read
"SharesColorWith" as a nonexistent type (`false`), which the leading `!` then negated to `true`, an actively wrong
"matches everything" rather than an absent property, which is why it stayed out until the property itself was built
rather than shipped wrong. `SharesColorWith`'s bare form (`CardProperty.java`: `card.sharesColorWith(source)`, `source`
the ability's own host) is `c.Colors().HasAny(sc.Colors())` — `mana.Colors.HasAny` already existed, so the colorless
check Java makes explicit on `card` falls out for free (`HasAny(0)` is always false regardless of which side is
colorless). A suffixed form (`SharesColorWith MostProminentColor`, `SharesColorWithOther <restriction>`, and the rest —
21 of 26 literal corpus occurrences, none of them Intimidate's own keyword-synthesized use) reads a game-wide or
remembered-list comparison this port has no evaluator for and is not matched by the exact-equality case, the same "false
for every card" fallthrough any other unimplemented property gets.

Landwalk (131 cards, `K:Landwalk:<Type>`) is also now covered, but could not join `cantBlockByKeywords`'s own fixed
table the way the other four keywords did: its own restriction, `ValidDefender$ Player.controls<Type>`, has a `<Type>`
that is the keyword's OWN argument (`K:Landwalk:Island`'s own "Island", `Landwalk.java`/`KeywordWithType.getValidType`)
— a different value per card, not a name every card carrying the keyword shares. `landwalkType` (`staticability.go`)
reads it directly off the keyword line instead, `enchantSpec`'s own precedent
([`## Casting a spell`](mana-and-casting.md#casting-a-spell-needed-the-stack-for-real-for-the-first-time)) for reading a
keyword's argument rather than a fixed string: `keyword.Parse(line).Args()[0]`, exactly `KeywordWithType.type`.
`ValidDefender` itself needed a new `matchesValidDefender` (staticability.go): a `Player`, not a `Card`, the identical
reason `matchesActivatingPlayer` (trigger.go) exists for `SpellCast`'s own `ValidActivatingPlayer` —
`You`/`Opponent`/`Player` are the same three bare values (4, 1 and 1 of the 8 real literal `ValidDefender$` lines),
checked against the BLOCKER's controller (`stAb.matchesValidParam("ValidDefender", blocker.getController())`) rather
than a trigger's activator. A `"Player.controls<Type>"` value — Landwalk's own entire restriction — asks whether the
blocker's controller controls at least one battlefield permanent `valid.Parse(type)` matches (`PlayerProperty.java`'s
own `"controls"` branch, `property.substring(8)`, no comparator suffix: every real corpus use of this shape is the bare
"at least one" default); `controllerControlsType` (staticability.go) is a plain battlefield scan, the same pairing
(`host.Controller`/`host.ID`) every other staticability.go check already passes to `Matches`.
`Player.Condition`/`Card.Self` (2 of the 8 real literal lines) never match, the same skip-rather-than-fire contract
every other unresolved param in this port gets. The Landwalk ignore-check (`StaticAbilityIgnoreLandwalk.java`) stays
unported: zero real corpus `S:Mode$ IgnoreLandwalk` lines exist, so nothing here can ever need to consult it.

Protection is also now covered, `protectionEach` (staticability.go) built the same per-card way `landwalkType` is:
Java's own `keyword.startsWith("Protection")` branch (`CardFactoryUtil.java:3953-3963`) calls
`Protection.getProtectionValid(keyword, false)` (`damage=false`, the block-legality call, distinct from the
damage-prevention one this port never makes) to build `ValidBlocker$`, and that builder's own output differs per card (a
color, a type, a subtype) the identical reason Landwalk's `<Type>` does. Two real corpus shapes, both resolved: the
natural-language form (`K:Protection from red`, 154 of roughly 219 real lines) — `protectionColorValid` maps each of the
five colors, `"colorless"` and `"everything"` to the exact `"Card.<X>,Emblem.<X>"` string
`Protection.getProtectionValid`'s own final wrap produces (`Emblem` never matches anything in this port, `baseMatches`'s
own hardcoded `false` for it, since nothing here creates one yet — harmless, `Card.<X>` is the half that ever does); and
the colon-structured form (`K:Protection:Artifact`, 65 lines) — `keyword.Parse` already splits `Details` at the first
colon (`Name="Protection"`, `Details="Artifact"` or `"from red"`, `keyword.go`'s own doc comment on the space-vs-colon
split), and Java's own early-return path for this branch (`Protection.java:26`) turns out to never need the
`Card.`/`Emblem.` wrap at all in the real corpus: every real characteristic here is either a bare type/subtype word
(`Artifact`, `Vampire`, `Dragon`, ...), which `baseMatches`'s own default type-fallthrough already resolves the
identical way Java's own `getType().hasStringType(incR[0])` does, or already dot-qualified (`Card.MultiColor`,
`Card.cmcGE3`), which `Matches` already resolves whole. `"protection from everything"` (1 real line) is Java's own
empty-`validSource` case — `CardFactoryUtil` then omits `ValidBlocker$` entirely, an unconditional CantBlockBy
`applyCantBlockBy`'s own `hasValidBlocker=false` contract already gives for free.

Skulk (15 cards, `K:Skulk`, CR 702.118a's "can't be blocked by creatures with greater power") is covered too, but not
through `cantBlockByKeywords`' fixed-string table or a `protectionEach`/`landwalkType`-shaped per-card-argument reader:
its own `ValidBlocker$ Creature.powerGTX` names a `Compare` property whose `X` operand is neither a fixed string nor a
per-card script value. `CardFactoryUtil.java`'s own Skulk branch hardcodes `st.setSVar("X", "Count$CardPower")` directly
on the synthesized `StaticAbility`, and `CardProperty.java`'s own `power`-comparison branch always resolves that operand
against `source` — the ability's own host, always the attacker itself since `ValidAttacker$` is hardcoded to
`Creature.Self`. So `X` is always "the attacker's own power," a constant semantic rather than a `Count$`/SVar question
at all: `skulkBlocks` (staticability.go) reads it directly as `host.Power()` (already the full Layer-7-folded value, not
the printed one) and compares it to the candidate blocker's own `Power()`, the identical direct-comparison shape
`menaceLegal` already uses for Menace rather than routing through `Matches`/`compareMatches`. `compareMatches`'s own
non-numeric-operand gap stays real for every OTHER `Compare` property a script writes a genuine dynamic SVar for — Skulk
just never was one of those once the hardcoding was read precisely (PORT-8). This was the one real `CantBlockBy` gap the
block-legality section named; block legality has none left.

`cantBlockBy` walks every card on the battlefield as a possible host of a real `S:` line, not just the attacker's own:
Java's own `cantBlockBy` walks every card in `ZoneType.STATIC_ABILITIES_SOURCE_ZONES` (Battlefield, Graveyard, Exile,
Command, Stack), and a corpus frequency scan of the 364 real `^S:Mode$ CantBlockBy` lines confirms why —
`ValidAttacker$ Creature.Self`/`Card.Self` is only 235 of them; `Creature.EnchantedBy`/`EquippedBy` (34) is an Aura or
Equipment granting its own host "can't be blocked," living on a different permanent than the one it restricts.
Graveyard/Exile/Command/Stack are not walked — no real corpus line needs a source there. Within one host, `ValidBlocker`
needs no manual comma-splitting despite Java's own `.split(",")` loop: `valid.Parse` already treats a comma as OR
between `Spec.Alternatives`, so the raw param string passes straight to `Matches` unchanged, the same as any other
multi-alternative valid string this port already handles whole
([`## Trigger firing`](triggers.md#trigger-firing-entering-dying-attacking-blocking-dealing-damage-being-discarded-becoming-tapped-tapping-for-mana-casting-a-spell-the-beginning-of-a-step-or-phase-a-player-attacking-drawing-a-card-and-watching-another-permanent),
for `ValidCard$ Cleric.Other,Card.Self`). A `ValidBlocker`-less line (an unconditional "can't be blocked," 5 of the 364)
is treated as matching once `ValidAttacker` does, exactly Java's own `if (stAb.hasParam("ValidBlocker"))` skip.

Not ported from `applyCantBlockByAbility`: the "Dragon Hunter" reach exception (a `ValidBlocker` alternative containing
"withoutReach" is undone if a separate `CanBlockIfReach` static grants that specific blocker effective reach against
that specific attacker — 1 real corpus card); `ValidAttackerRelative` (Ironclaw Curse, 1 card). `ValidBlockerRelative`
itself now resolves one shape (`Creature.powerGTX` with X `Count$CardPower`, the Ring's own level-1 ability); any other
shape is unrecognized and `cantBlockBy` skips the static rather than erroring (`CanBlock` returns a bare `bool`, no
error channel), which for Space Beleren's `Creature.DifferentSector` static (1 card) means every block is now allowed
rather than none.

The one architectural wrinkle: `valid.go`'s own "attacking"/"blocking" bare-form properties used to call
`g.Attackers()`/`g.Blocks()` (`attack.go`/`block.go`), which made the `valid` `enginelint` group depend on `attack` and
`block`. `cantBlockBy` needs the reverse — `block` calling into code that calls `Matches` — so those two reads were
changed to `g.combat.Attackers`/`g.combat.Blocks` directly (same package, same field, zero behavior change): `combat.go`
already held the type those two accessors were just wrapping, so `valid` only ever needed the `combat` group, not
`attack`/`block` themselves. `enginelint.json`'s `valid` allow-list dropped both; a new `staticability` group (needing
`valid`) sits between them, and `block` gained it.

`DeclareCombatBlockers` (`block.go`) could not fold this into its existing `eligible` list the way "untapped" already
is: `eligible` is computed once per defending player and shared across every attacker assigned to them, but
`CantBlockBy` is a property of one attacker/blocker _pair_, not of a creature in isolation — the same blocker can be
legal against one attacker and illegal against another in the same combat. `CanBlock` is checked instead after the
controller answers, and an illegal pairing is dropped rather than committed — the one place a controller's own answer is
re-checked, unlike every other `Choose*`/`Declare*` method (`control.go`'s own doc comment,
[`## Combat`](#combat-declaring-attackers-declaring-blockers-and-dealing-damage), above). Menace's own `menaceLegal`
runs as a second filter on top, once `CanBlock` has already narrowed a defender's answer to individually legal pairs.

---

## CR 508.1c: exerting an attacker as it attacks

`compile.go`'s own `subAbilityKeys` gains a new entry, `"trigger"` -- `S:Mode$ OptionalAttackCost`'s own `Trigger$`
param (`StaticAbilityCantAttackBlock.java`'s own `getAttackCost`/`getSSTrigger`), not `T:`'s own `Trigger$` (that
grammar has no param of this name; the two share a param key by coincidence, not by CR mechanic). 23 of the corpus's 28
real `Cost$ Exert<1/CARDNAME>` `OptionalAttackCost` lines name one (`ahn_crop_crasher.txt`, `gust_walker.txt`); the
other 5 (`resolute_survivors.txt` and four more) name none at all -- their own payoff, if any, is an ordinary
`T:Mode$ Exerted` line instead, `checkExertedTriggers`' (`exertcost.go`) own shape already covering it. Compiling
`Trigger$` into `ability.Subs` regenerated 23 golden AST hashes (`testdata/ast.golden`), the exact 23 cards carrying one
-- verified by count before landing.

`PlayerController` gains its 47th method, `ExertAttackers` (`control.go`) -- CR 508.1c's own "you may exert this as it
attacks," a subset answer over every declared attacker carrying a real `OptionalAttackCost` static naming
`Cost$ Exert<1/CARDNAME>` (`optionalAttackCostExert`, `attack.go`, reusing `cost.ActivationShape`'s own `SelfExert` flag
-- `activateability.go`'s identical reuse for the unrelated activated-ability cost shape). Not offered at all when no
declared attacker carries one, the same "nothing meaningful to decide" reasoning `DeclareCombatAttackers` itself already
uses for an empty eligible set.

`exertDeclaredAttackers` (`attack.go`) runs right after tapping, before target assignment -- `PhaseHandler.java`'s own
ordering (`declareAttackersTurnBasedAction`, right after attackers are provisionally tapped). Each chosen attacker: sets
`Card.Exerted`, calls `checkExertedTriggers` (the identical `T:Mode$ Exerted` walk the cost-based `Exert<...>` shape
already uses -- `Card.exert(Player)` fires the identical trigger type in Java regardless of what caused it), then
`resolveOptionalAttackCostPayoff` finds and pushes the static's own `Trigger$` sub-ability if one exists, through
`pushTriggeredAbilities` -- CR 601.2c/603.3b's own "choices are made the moment it's put on the stack," the identical
push every other trigger already goes through.

`ScriptedController` gains `QueueExertAttackers`/`ExertAttackers`, on `QueueAttackers`'s own shape (a nil or empty
answer declines every offer, still consuming the queue slot). `fixture/actions.go` gains
`queue exertattackers [<id>,...]`, `queue attackers`'s own sibling. `cr-508-1c-exert-attacker-runs-payoff` is the
fixture: a real Gust Walker attacks and taps -- `GameState`'s own dump format (`dump.go`) has no
`Exerted`/granted-keyword/PT field to assert against, so the exert flag and the Pump/Flying payoff are proven at module
level instead (`TestDeclareCombatAttackersExertsAndRunsPayoff`, `attack_test.go`).

## Ward: a natively constructed triggered ability

CR 702.21a (ADR-0028): `checkWardTriggers` (trigger.go) runs alongside `checkBecomesTargetTriggers`, at the two call
sites that name an actual spell (`castAura`, `castInstantOrSorcery`). Each of a newly-targeted battlefield card's own
`Ward` lines fires independently (CR 702.21g, `protectionEach`'s own "check every line" precedent, ADR-0027) when the
spell's own controller is an opponent of the warded card's (`matchesPlayerSpec`'s "Opponent" base, the identical check
`cardCantBeTargetedBy`'s own Hexproof gate already makes).

Scoped to the mana-cost shape only (~252 of ~262 real corpus lines, `K:Ward:`/`AddKeyword$ Ward:` combined):
`cost.Parse(details).IsPureMana()` plus `mana.Parse` with no `X`, the identical pre-check `resolveUnlessCost`
(effect.go) runs at resolution -- checked again here, before pushing, so a shape this port cannot pay for is never
pushed rather than pushed and then erroring. `PayLife`/`Discard`/`Sac`/`Ward:X`/Alchemy shapes are skipped (GO-7),
logged in `game-state.md`'s Not ported yet.

No new `compile.Ability`-from-script-text machinery: the built `Ability` has `API: APICounter`, `Controller` the warded
card's own controller (CR 603.3a: a triggered ability's controller is its source's controller), `Source` the warded
card, and `wardCounters` (`ability.go`) a single `EntityID` naming the targeting spell -- Forge's own
`Defined$ TriggeredSourceSA` stand-in, kept off `Targets` on purpose: a real `Targets` entry would run the spell through
`pushTriggeredAbilities`'s own post-push `checkBecomesTargetTriggers` scan (wrongly marking it `BecameTargetThisTurn`
and letting an unrelated watcher's `ValidTarget$` fire against a stack card) and through `resolveTop`'s own CR 608.2b
`targetsStillLegal` re-check (`cardCantBeTargetedBy` has no business running against a spell). `counterEffect`
(`countereffect.go`) reads `wardCounters` alongside `Targets` instead, and its own `c.Zone != Stack` check is the
identical "left the stack already" answer Forge's own `getInstanceMatchingSpellAbilityID` null-check gives. `Params` is
a `*compile.Ability` built with `designationTrigger`'s own (`becomemonarcheffect.go`) `[]vocab.Param` literal shape --
`DB$ Counter`, `TargetType$ Spell`, `UnlessCost$ <the Ward line's own cost text>`,
`UnlessPayer$ TriggeredSourceController` -- resolved through `definedPlayers`'s existing
`TriggeredSource`/`TriggeredSourceController` case (`defined.go`) against
`Ability.triggered.source`/`.sourceController`, set to the targeting spell's `CardEntity` and its controller
respectively, so the payer is the attacker even though `Controller` above now names the warded player.

`checkWardTriggers`' own matches are collected into the same slice as `checkBecomesTargetTriggers`'s own matches from
the identical targeting event (`allTargetsOf(a)`, `castspell.go`, which also gathers a Charm's chosen modes' own
targets), then pushed together through one `pushTriggeredAbilities` call -- CR 603.3b's APNAP batching, so Ward's own
trigger shares a batch with every other BecomesTarget trigger the same spell caused instead of always resolving first in
a batch of its own. `resolveTargets` is a no-op for the pushed `Ability` (no `ValidTgts$` named, and `Targets` itself is
empty for Ward), and Ward's own `wardCounters` reference survives untouched. `PushAbility` stamps it (CR 400.7's own
zoneStamp, ADR-0027) and appends it to the real stack; `resolveTop`'s own `targetsStillLegal` re-check is a no-op too
(empty `Targets`), so a spell that already left the stack by the time Ward resolves is caught by `counterEffect`'s own
zone check instead, the identical answer Forge's own reference lookup gives.

Scoped out for this pack (ADR-0028, logged in `game-state.md`'s Not ported yet): an activated or triggered ability
targeting a Warded permanent (no `EntityID` for `counterEffect` to counter by) and `ChangeTargets`/`CopySpellAbility`
retargeting one onto a Warded permanent (neither call site names the spell).

Fixtures: `ward-refuses-payment-counters-the-spell` (ai declines the {2}, Bolt is countered, Tomakul Honor Guard takes
no damage), `ward-payment-lets-the-spell-resolve` (ai pays, Bolt resolves and kills the 3/1),
`ward-does-not-trigger-for-controllers-own-spell` (human's own Bolt at human's own Warded creature never raises the
trigger at all). `fixture/actions.go` gains `queue confirmpaycost <bool>` (`ScriptedController.QueueConfirmPayCost`),
this port's first fixture-reachable `UnlessCost$` pay-or-not prompt. `TestCastSpellWardEachInstanceFiresIndependently`/
`TestCastSpellWardDoesNotFireForANonManaCost` (`ward_test.go`) prove CR 702.21g and the mana-only scope directly through
`Game.StackLen()`, since two Ward triggers and a skipped one both dump identically otherwise.
