# Port: GameState fixture format

- **Java source:** `forge-game/src/main/java/forge/game/GameState.java` (1,432 LOC) — `parse`/`parseLine`,
  `processCardsForZone`, `applyGameOnThread`, `toString`, and `PhaseType.smartValueOf`
- **Go target:** `crucible/internal/fixture`
- **Status:** Text parse, `Load` and `Dump` done for a core slice (M4); `RunActions` and the scenario harness (M5) run
  against the real corpus, including `lost=`/`won=`/`over=` so a scenario can assert a game actually ended. The per-card
  annotation long tail, tokens, and most remaining `Player` fields stay text-only; see "Not ported yet"

## What it does

Reads a fixture's `key=value` lines into a `State` (`Parse`), builds an `*engine.Game` from one (`Load`), and writes a
`*Game` back out (`Dump` to a `State`, `Write` to text). TEST-5 adopts this format verbatim so the same fixture runs
against the Java oracle — the whole reason to use Forge's own reader rather than inventing one.

The split mirrors `internal/carddb`'s own two stages (PORT-2): `Parse` keeps zone contents as raw
`Name|Set:X|Tapped:True;...` text, because turning that into `Card`s needs `compile.DB` and a `*Game` to allocate into.
`Load` is that second stage. `Dump` and `Write` are its inverse, and together the four make the P3 exit gate an actual
test: `TestDumpRoundTripsThroughParse` runs a fixture through `Parse → Load → Dump → Write → Parse → Load` and checks
every field the second `Loaded` produces against the first.

## `Loaded` no longer carries turn state

`Turn`, `ActivePlayer` and `ActivePhase` used to live on `Loaded` rather than `engine.Game`, because M5's turn/phase
loop had not landed and guessing its shape early was exactly the kind of speculative design CLAUDE.md rules out.
`turn.go`'s `StartTurn`/`AdvancePhase` gave them a real home (`porting/port-log/game-state.md`'s "turn structure"
section), so `Load` now calls `Game.SetTurnState` and `Dump` reads `Game.Turn`/`ActivePlayer`/`ActivePhase` back —
`SetTurnState` is `Load`'s tool for this the same way `devAdvanceToPhase` is Java's: a state injection, not a step
`AdvancePhase` would actually run.

`Loaded` keeps only what still has no `Game` equivalent: `ActivePhaseAdvance`/`PhaseAdvanced`, a fixture-only "advance
once more after setup" directive that is parsed but not yet applied, and `Unapplied`.

One consequence of the move: `Game` has no "phase is unset" state of its own — a real game always has some active phase
once turn state is initialised. `Dump` only writes `activephase=` when `ActivePlayer` is also set, so a fixture that
names no active player does not gain a phantom `activephase=Untap` line it never wrote; one that does gain an explicit
phase on its first dump, stable from then on.

## Load builds the game itself

Java's `GameState.applyToGame` runs against a `Game` a `Match` already built, with its players already seated —
`GameState` only fills in state on players that exist. Crucible has no `Match` yet, so `Load` seats the players itself,
one per `Named` slot, in slot order (human, ai, p2..p9) — the same order every fixture already writes in. This is a
structural difference from Java (PORT-1), not a data one: the seating order a fixture implies is unchanged.

## Two-pass resolution

`AttachedTo:`, `RememberedCards:` and `Imprinting:` can name a card declared later in the same fixture — Rancor
attaching to a creature written after it in the file has to work. `Load`'s `loader` type collects these as it creates
cards and resolves them once every card in the fixture exists (`resolveRefs`), the same two-pass shape
`GameState.applyGameOnThread` uses (`idToCard`, `cardToAttachId`, ...).

The three lists are ordered slices, not maps keyed by card, because two auras naming the same host must attach in the
order the fixture wrote them: that order is what breaks a tie between their continuous effects when both share a
timestamp (GO-12). `RememberedCards:`/`Imprinting:` have no such cross-card ordering concern — each card's `Memory` is
independent — but the slices are ordered anyway, once discipline was already needed for attachments.

## `Id:` is the card's own handle

Java hands out arbitrary per-fixture integers and only writes one when something else references that card
(`cardsReferencedByID`). Go always writes `Id:<CardID>` and always uses the handle the card already has: there is
nothing to gain from a fixture-chosen numbering scheme `Load` would have to remember separately, and
`Parse(Write(Dump(x)))` does not need to reproduce `x`'s own numbers to prove the round trip works — only that the
references resolve to the same cards again, which a fresh handle does just as well as an old one.

## Compiled cards needed a name back

`Dump` writing a card's name needs to ask `Def.Name`, and `compile.Card` did not carry one: it holds `Filename` (a
snake_case file stem, not a printed name) and the compiled `Faces`, because nothing before this needed to go from a
compiled card back to the string that named it. Added `Name string`, set from the primary face during `Compile` — a
one-line, backward-looking fix rather than new scope, and it does not touch `WriteCanonical`/`Fingerprint`, so the
golden AST diff (M3's P2 gate) is unaffected.

`compile.NewDB` is the other small addition this needed: `LoadDB` is the only prior constructor and always walks a real
corpus directory, which is wrong for a test that wants three known cards and nothing else. `NewDB` builds a `*DB` from
an already-compiled `map[string]*Card`.

Nothing had actually called `LoadDB` against the real corpus before the scenario harness below did, and it turned out
not to work: it compiled each script inside the same pass that parsed it, which fails any `CopyFaceFrom:` card (Bind //
Liberate among them) since that placeholder only resolves once the whole corpus has been read. Fixed in `LoadDB` itself
— `porting/port-log/ability-factory.md`'s own section on it, since the bug was there, not here.

## `Dump` dropped `ManaPool` entirely

`Load`'s `applyManaPool` has read `manapool=` into `Player.ManaPool` since before `TapLandForMana` existed, but `Dump`
never wrote a `PlayerState.ManaPool` back from it, and `Write` had no `manapool=` line to emit even if something had —
every other player field `Load` applies (`Life`, `Counters`, `Lost`, `Won`) had a matching pair on the way out;
`ManaPool` had neither. `TestDumpRoundTripsThroughParse` — the test that is P3's own exit gate, whose whole job is
"checks every field the second `Loaded` produces against the first" — never caught it because its own fixture text never
set `manapool=`, so both sides compared empty against empty.

Fixed with the same shape `dumpCounters` already has: `dumpManaPool` (`dump.go`) inverts `Pool.Breakdown` into
`manapool=`'s own space-separated letters, and `Write` (`fixture.go`) gained the missing `if p.ManaPool != "" {...}`
line every other non-empty field already had. Snow and plain mana dump identically — `Breakdown`, not `SnowBreakdown` —
the same limit `manapool=`'s own read side already has (`## Mana pool and payment`, `game-state.md`: `GameState.java`'s
own `ManaAtom.MANATYPES` has no snow entry either, so there was never a snow token for `dumpManaPool` to write in the
first place). `TestDumpRoundTripsThroughParse`'s own fixture now sets `humanmanapool=W W U` so this exact gap cannot
reopen silently; `TestDumpAndWriteRoundTripManaPool` is the narrower, Lost/Won/Over-shaped test for it on its own.

`landsplayed=`/`landsplayedlastturn=` had the identical gap on the `Write` side alone: `Parse` has read both since
before `PlayLand` existed (`fixture.go`'s own `number(&p.LandsPlayed)` case), but `Write` had no line to emit either
one. Caught this time before a scenario ever needed the round trip, while adding `Player.LandsPlayed` itself
(`## Playing a land is not casting a spell`, `game-state.md`) rather than after — `TestDumpAndWriteRoundTripLandsPlayed`
is `TestDumpAndWriteRoundTripManaPool`'s own shape, for this pair.

## Scenarios: `actions.log` and the harness

`internal/engine`'s `TestScenarios` (`scenario_test.go`) is TEST-5's directory walk: `setup.state` and `expect.state`
are this package's `Parse`/`Load`, unchanged. What is new is `RunActions` (`actions.go`) — the "ordered, explicit
decisions" Plan Section 3.3 names but does not itself define a format for, because Java's own differential tooling
drives a real `PlayerController` from Java code and never needed a text vocabulary for it. This one is Crucible's own,
line-oriented the same way `setup.state` is:

```text
startturn <player>            Game.StartTurn(player, controller)
advance [n]                   Game.AdvancePhase(controller), n times (default 1)
dealopeninghands              DealOpeningHands(game, controller), starting player discarded
mulligan <firstplayer>        PerformMulligans(game, controller, firstplayer)
declareattackers              Game.DeclareCombatAttackers(controller)
declareblockers               Game.DeclareCombatBlockers(controller)
firststrikedamage             Game.DealFirstStrikeDamage(controller)
combatdamage                  Game.DealCombatDamage(controller)
queue keephand <bool>         ScriptedController.QueueKeepHand
queue tuck <id>[,<id>...]     ScriptedController.QueueTuck, ids from Loaded.CardByFixtureID
queue startingplayer <p>      ScriptedController.QueueStartingPlayer
queue startinghand <n>        ScriptedController.QueueStartingHand
queue legendarykeep <id>      ScriptedController.QueueLegendaryToKeep, id from Loaded.CardByFixtureID
queue enchanttarget <id>      ScriptedController.QueueEnchantTarget, id from Loaded.CardByFixtureID
queue targets <t>[,<t>...]    ScriptedController.QueueTargets, a triggered ability's/cast spell's/activated ability's targets -- each <t> a card's Loaded.CardByFixtureID or a seated player's name (resolveTargetEntities)
queue attackers [<id>,...]    ScriptedController.QueueAttackers, ids from Loaded.CardByFixtureID (no ids declines)
queue exertattackers [<id>,...]  ScriptedController.QueueExertAttackers, ids from Loaded.CardByFixtureID (no ids/"none" declines)
queue attacktarget <p>|<id>   ScriptedController.QueueAttackTarget, a player name or a planeswalker/battle's Loaded.CardByFixtureID
queue blocks [<b>=<a>,...]    ScriptedController.QueueBlocks, blocker=attacker pairs from Loaded.CardByFixtureID (no pairs declines)
queue damage <b>=<n>[,...]    ScriptedController.QueueDamageAssignment, blocker=amount pairs from Loaded.CardByFixtureID
queue discard <id>[,...]      ScriptedController.QueueDiscard, ids from Loaded.CardByFixtureID
queue cardchoice <id>[,...]   ScriptedController.QueueCardChoice, an effect's ChooseCardsForEffect pick (Clone's Choices$, among others), ids from Loaded.CardByFixtureID
queue battleprotector <p>     ScriptedController.QueueBattleProtector, a seated player's name
paymanacost <player> <cost>   Game.PayManaCost(player, cost, controller), cost is mana.Parse's own text
tapformana <player> <id> <color> Game.TapLandForMana(player, id, color), id from Loaded.CardByFixtureID
playland <player> <id>        Game.PlayLand(player, id), id from Loaded.CardByFixtureID
castspell <player> <id>       Game.CastSpell(player, id, controller), id from Loaded.CardByFixtureID
resolvestack                  Game.ResolveStack(NewRegistry(), controller), no arguments
passpriority                  Game.PassPriority(NewRegistry(), controller), one CR 117 round (ADR-0019), no arguments
step [n]                      Game.Step(NewRegistry(), controller), n times (default 1), one driven step each (ADR-0026)
run <turns>                   Game.Run(NewRegistry(), controller, turns), steps until game over or turn <turns> ends
queue action <p> pass         ScriptedController.QueueAction, p passes once (an empty queue passes too)
queue action <p> cast <id>    ScriptedController.QueueAction, p casts id when next given priority
queue action <p> activate <id> <n>  ScriptedController.QueueAction, p activates id's n'th ability (0-based)
queue action <p> playland <id>      ScriptedController.QueueAction, p plays land id (ADR-0026)
queue action <p> tapformana <id> <color>  ScriptedController.QueueAction, p taps basic land id for color (ADR-0026)
queue action <p> manaability <id> <n>     ScriptedController.QueueAction, p activates id's n'th mana ability (ADR-0026)
queue action <p> cast <id> <door>         ScriptedController.QueueAction, p casts Room id as its LeftSplit/RightSplit half
queue action <p> unlockdoor <id> <door>   ScriptedController.QueueAction, p unlocks Room id's LeftSplit/RightSplit door (special action)
queue roomdoor <door>                 ScriptedController.QueueRoomDoor, UnlockDoor's LeftSplit/RightSplit pick
queue paygeneric <shard>      ScriptedController.QueuePayGeneric, a bare shard symbol ("W", "C", ...)
queue payx <n>                 ScriptedController.QueuePayX, the value of X for a cost carrying one
queue paysnow <shard>          ScriptedController.QueuePaySnow, a bare shard symbol naming the color
queue hybridmanacolor <color> ScriptedController.QueueHybridManaColor, a bare color letter
queue paymonocoloredhybrid <bool>     ScriptedController.QueuePayMonocoloredHybrid
queue paycolorlesshybrid <bool>       ScriptedController.QueuePayColorlessHybrid
queue payphyrexian <bool>             ScriptedController.QueuePayPhyrexian
queue payhybridphyrexian <color|life> ScriptedController.QueuePayHybridPhyrexian, "life" for the zero mana.Colors answer
queue confirmeffect <bool>            ScriptedController.QueueConfirmEffect, an effect's yes/no prompt (Play's single option, CopySpellAbility's Optional$/MayChooseTarget$)
queue optionaltrigger <bool>          ScriptedController.QueueConfirmOptionalTrigger, an OptionalDecider$ trigger's "you may" (Swarm Intelligence's copy trigger)
```

`queue battleprotector` is a state-based action's own question, not tied to any combat verb:
`Game.CheckStateBasedActions` asks it whenever a Battle has no protector (or its protector has left the game) and
nothing is currently attacking it (`game-state.md`'s "Combat" section, `assignBattleProtector`). A fixture with a Battle
in `setup.state` needs one queued before the first `startturn`/`advance`/`combatdamage`-family action that would run a
state-based-action check, since that first check is what asks. The result is nameable in `expect.state` too —
`Protector:<player>`, Crucible-only the same way `lost=`/`won=`/`over=` are (no Java `GameState` key exists to diverge
from), compared by name the same way everything else two independently loaded games disagree on numerically is.
`testdata/scenarios/battle-protector-assigned-to-opponent` is the example.

`queue discard` is needed only when `advance` reaches `Cleanup` with the active player's hand over `MaxHandSize` (7,
`game-state.md`'s "Turn structure") — a hand already at or under that never asks. There is no `none` shortcut, the same
reasoning `queue damage` has none: `Game.cleanupStep` only asks when there is a nonzero, known count to discard
(`hand.Len() - MaxHandSize`), so declining entirely was never a legal answer to make room for.

`queue attacktarget` is needed only when a declared attacker has more than one eligible target -- a planeswalker or
battle present on the opponent's side, or (multiplayer) more than one living opponent -- one call per such attacker, in
the order `declareattackers` declared them. A lone eligible target (any two-player game with nothing else to attack, the
ordinary case) is assigned automatically without consuming a queue entry; an entry queued for a question that was never
asked is simply left unread, the same as any other over-queued answer (`ScriptedController` has no "everything was
consumed" check of its own).

A scenario with a first striker needs both `firststrikedamage` and `combatdamage`, with an `advance` between them: a
first-strike kill has to actually happen (`CheckStateBasedActions` runs on every phase entry, `game-state.md`'s "Turn
structure") before the regular step asks whether the dead creature still deals or receives anything, and `advance`ing
from the `FirstStrikeDamage` phase into `CombatDamage` is what runs that check — no separate verb exists just for it. A
scenario with nothing carrying "First Strike"/"Double Strike" can skip `firststrikedamage` entirely; calling it anyway
is a safe no-op.

`queue blocks`' pairs are `blocker=attacker`, both `Id:` numbers — `1=2` means the card with `Id:1` blocks the card with
`Id:2`; `1=3,2=3` is a gang block, two blockers on one attacker. `queue damage`'s pairs are `blocker=amount` and, unlike
`queue blocks`, keep the order written: that order is the order `AssignCombatDamage` divides a gang-blocked attacker's
damage in (CR 510.1c), so reordering the pairs would answer a different question. Any of an attacker's power left
unassigned across the pairs tramples over to the defending player if the attacker has trample (CR 702.19c), or is wasted
if not — both are computed after `queue damage`'s answer is applied, not part of what it names. There is no `none`
shortcut for `queue damage` — `Game.DealCombatDamage` only ever asks when an attacker has more than one blocker, so an
empty answer is never itself the legal one the way declining to attack or block is.

`dealopeninghands` had no verb at all until this pass, even though `DealOpeningHands` (CR 103.1-103.4) has been done
since before `mulligan` itself was: `mulligan`'s own fixture (`mulligan-tucks-a-card`) starts from a hand `setup.state`
already wrote directly, never from a shuffled library `DealOpeningHands` actually dealt. `queue startingplayer` answers
`ChooseStartingPlayer`'s own CR 103.2 coin flip the same way it already does for `mulligan`'s starting-player question;
the coin flip itself (which player `g.rand` happens to ask) stays unobservable either way, since a
`ScriptedController`'s queued answer does not depend on who asked. Its return value (who actually goes first) is
discarded, the same "the fixture already knows because it queued a fixed answer" reasoning `paymanacost`'s `bool` and
`TapLandForMana`'s `bool` are not asserted for either — a fixture that needs the value for a later
`mulligan`/`startturn` call just writes down the same player name it already queued.
`testdata/scenarios/opening-hand-dealt-with-a-clean-keep` is the example: two ten-card libraries of identical Mountains
(so the shuffle's own randomness is unobservable in the dealt hand's contents), a full deal, and both players keeping
without a single mulligan.

`Game.StartTurn`/`AdvancePhase` take `controller` because `CheckStateBasedActions` does now too — the legend rule needs
one (`game-state.md`'s "The legend rule needed `CheckStateBasedActions` to take a controller"), and every path that
reaches a state-based-action check had to gain the same parameter.

`paymanacost` is not tied to any turn/phase/combat verb either, and needs none run first: `Game.PayManaCost` reads and
spends `manapool=`'s own pool directly (`Player.ManaPool`), the same self-contained-rules-chapter reasoning
`manapay.go`'s own doc comment gives for building it before anything casts a spell. `queue paygeneric` is needed once
per unit of the cost's own generic amount, regardless of whether the payment is going to succeed — `PayManaCost` asks
before it ever checks the pool, so a cost with `{2}` asks twice even when the pool cannot cover either answer,
`testdata/scenarios/mana-payment-fails-atomically`'s own shape. Every hybrid and Phyrexian shape now has its own verb
too: `queue hybridmanacolor`, `queue paymonocoloredhybrid`, `queue paycolorlesshybrid` and `queue payphyrexian` mirror
their own `ScriptedController` method's argument shape (a bare color letter or a bool); `queue payhybridphyrexian` takes
either a color letter or the literal word `life` for the zero `mana.Colors` answer (CR 118.4's own three-way choice,
`ChoosePayHybridPhyrexian`'s own doc comment) — `resolveManaColor` (`actions.go`) is the one parser both
`hybridmanacolor` and `payhybridphyrexian` share, since a bare color letter is exactly `mana.ParseShard`'s own pure
shard, taken down to its `Colors()` half.

`tapformana` is the one action verb, not a `queue` kind: unlike every `ScriptedController` answer, there is no decision
here for a controller to make ahead of time — the player, the card and the color are all named directly in the line, the
same "self-contained rules chapter, no turn/phase/combat verb needed first" position `paymanacost` is already in. It
reuses `resolveManaColor` for its own `<color>` argument and `resolveCardIDs` (requiring exactly one id, the same check
`queue legendarykeep` already makes) for `<id>`. `Game.TapLandForMana`'s `bool` return is not asserted, the same
"declined by the rules, not a fixture error" convention `paymanacost` already established —
`testdata/scenarios/mana-payment-tap-land-for-mana` is the one fixture so far, and the first mana-payment fixture where
the paid mana comes from a real card (a corpus `Plains`) rather than `manapool=`.

`playland` is `tapformana`'s own shape — one action verb, no `queue`, `<id>` resolved the same way — for `Game.PlayLand`
(CR 305, `game-state.md`'s own section on it). It is the first verb that moves a card from hand to the battlefield
through a real game action rather than `setup.state` placing it there directly: every existing combat/mana fixture's
battlefield cards start on the battlefield already, and `mulligan-tucks-a-card` is the only other fixture that moves a
card between zones at all before this. `land-played-then-tapped-for-mana` is the fixture: a Plains starts in hand,
`playland` puts it on the battlefield, and `tapformana` taps it for mana the same turn — proving `TapLandForMana` has no
summoning-sickness check to get in the way, since CR 302.6 restricts a creature's own tap ability, not a land's mana
ability.

`castspell` and `resolvestack` are `Game.CastSpell`/`Game.ResolveStack` (CR 601, `game-state.md`'s "Casting a spell
needed the stack for real, for the first time"), the first verbs to drive the stack at all. `castspell <player> <id>`
takes the same `<id>`-resolved-through-`Loaded.CardByFixtureID` shape as `playland`; its `bool` return is not asserted,
the same "declined by the rules" convention every other bool-returning verb already carries. `resolvestack` is the one
exception to that convention in the whole file: it takes no arguments, and `RunActions` does not discard its `error` the
way it discards every bool — `Registry.Resolve`'s `ErrUnimplemented` is a real gap (GO-7), not a declined decision, so a
fixture that pops an API this port cannot yet resolve fails loudly instead of silently doing nothing.
`cast-a-creature-spell-resolves-to-battlefield` is the fixture: a real Grizzly Bears cast with mana tapped from two real
Forests (`manapool=` cannot be used here either, the same CR 500.4 reason `tapformana`'s own fixtures already worked
around — `emptyManaPools` wipes any preloaded pool the moment the first `startturn`/`advance` call runs `beginPhase`, so
the lands are tapped mid-scenario, after reaching `Main1`, not preloaded at `setup.state` time).

`passpriority` runs one `Game.PassPriority` round: every seated player is asked `TakeAction` in turn order, starting
with the active player, until the stack is empty and everyone has passed in succession. It returns `error` like
`resolvestack`, for the same reason, and also for a queued action `CastSpell`/`ActivateAbility` declines — ADR-0019
Decision point 5 makes that a hard stop, not a silent decline. `queue action` fills one player's own `TakeAction` queue.
Queues are per player (ADR-0019 Decision point 1), so the order of `queue action` lines between two players does not
matter. An empty queue answers pass. An explicit `queue action <p> pass` is for a player who passes now and acts later
in the same round: the active player is asked first, so without it their queued response would be spent on the first
ask. Targets still come from the shared `queue targets` FIFO, in cast order. Fixtures:
`priority-response-bolt-wins-the-race` (a response resolves first and wins the game before the spell under it resolves),
`priority-active-player-passes-then-responds` (an explicit pass, then a response) and
`priority-pyromancer-pings-in-response-to-bolt` (a non-active player's instant-speed activated ability as a response).

`step` and `run` are the turn driver (ADR-0026, `turn-stack-combat.md`): unlike `advance`, a driven step runs combat's
declarations and damage itself (so a scenario queues `attackers`/`blocks` answers instead of calling `declareattackers`)
and opens a priority round wherever the step grants one, so a phase trigger resolves before `step` returns. A queued
action is spent on the first round that asks its player — Upkeep included — so a scenario steps to the step it wants
before queueing: on turn 1, `step` from Untap stops in Upkeep and the next step is Main1 (the driver skips turn 1's draw
step, CR 103.7a). Fixtures: `testdata/scenarios/driver-*`.

`queue enchanttarget` answers `ChooseEnchantTarget` (`castspell.go`'s own Aura branch, CR 601.2c) the same
`queue legendarykeep` shape — a bare id, no color or bool vocabulary — needed only when an Aura's own `Enchant`
restriction matches more than one battlefield permanent; a lone match is assigned automatically, the same "nothing
meaningful to decide" convention `attacktarget`'s own lone-target case already has, so most Aura fixtures never queue
one at all. `cast-an-aura-spell-attaches-to-chosen-target` is the fixture: a real Pacifism cast at a lone Grizzly Bears
on the battlefield, `resolvestack` moving it there and attaching it in the same call. Casting a Battle cannot go all the
way through `resolvestack` the way the other four permanent types do: every Battle in the corpus carries its own ETB
trigger (`checkETBTriggers`, `trigger.go`, `game-state.md`'s own section on it), which `resolvestack` correctly surfaces
as `ErrUnimplemented` once the Battle itself has resolved — `TestScenarios` has no way to assert an expected
`RunActions` failure, so `cast-a-battle-spell-reaches-the-stack` stops one step earlier than its four siblings, at
`castspell` alone.

`queue payx` is a bare `strconv.Atoi`, the plainest parser of the whole file — `ChoosePayX`'s own answer is just an
`int`, no shard or color vocabulary involved. It is asked once per cost, not once per `{X}` symbol, so a cost with two
`{X}`s (CR 107.3f) still consumes exactly one `queue payx` line; `mana-payment-resolves-x` writes `queue payx 3` once
and three `queue paygeneric` lines after it (X's chosen value folds into the generic amount `queue paygeneric` already
knows how to spend), not three `queue payx` lines.

`queue paysnow` reuses `mana.ParseShard` the same way `queue paygeneric` does -- `ChoosePaySnow`'s own answer is a plain
color shard, not a "this is snow" flag, since by construction the answer is already only ever asked for a snow ({S})
symbol. Unlike `queue payx`, one `queue paysnow` line answers exactly one `{S}` symbol: CR 106.3a puts no "announced
once" language on snow the way CR 601.2b does for X, so a cost with two `{S}` symbols needs two `queue paysnow` lines
and can name two different colors. `setup.state` has no way to put snow mana in a pool directly -- `manapool=` only ever
produces plain mana (`## Mana pool and payment`, `game-state.md`) -- so every snow fixture needs a real snow land and
`tapformana` first; `mana-payment-resolves-snow` is the one fixture, a Snow-Covered Plains tapped for snow white, then
spent paying a bare `{S}` cost.

`Loaded.CardByFixtureID` is the other piece `RunActions` needed: the same `Id:` map `AttachedTo:`/`RememberedCards:`
resolution already builds internally, kept around after `Load` returns instead of discarded. A scenario naming a
specific card to tuck needs a handle that survives a mulligan's shuffle, and `Id:` — assigned once, at load time, never
touched again — is exactly that; the `CardID` a shuffle produces is not something a fixture author could predict.

The comparison itself does not go through `Dump`. `Dump`'s `Id:` is the card's own `CardID` (see above), and
`setup.state` (run through actions.log) and `expect.state` are two independently loaded games whose `CardID`s were never
going to agree by number. `compareGames` (`scenario_test.go`) compares the two `*engine.Game`s directly instead — zone
contents by name and position, `Tapped`/`SummonSick`/`Damage`/`Counters`/attachment/protector per card — which sidesteps
the numbering question entirely and reaches fields `Dump` cannot write down at all (see below). Protector is compared by
name too, the same reasoning `CardID` gets: two independently loaded games were never going to agree on raw `PlayerID`s
either, only on who they name.

**`Lost`, `Won` and `Over` have their own keys: `lost=`, `won=`, `over=`.** `GameState.java`'s own format has none of
these — a Java fixture is always a still-being-played snapshot, never one that asserts the game already ended — so this
is Crucible-only, the same category as `actions.log` itself. Without them an `expect.state` loaded fresh always reported
`Lost`/`Won`/`Over` `false` regardless of what a scenario intended, which is why `compareGames` used to skip comparing
them: comparing would have failed every scenario that legitimately ends the game. `<player>lost=true` and
`<player>won=true` sit next to the other per-player keys (`PlayerState.Lost`/`Won`); `over=true` is top-level
(`State.Over`), since a game ending is not itself a per-player fact even though CR 104.2a's loss/win bookkeeping is.
`testdata/scenarios/poison-loss` is the example: ten poison counters going in via `setup.state`, `startturn human`
running `CheckStateBasedActions` in `actions.log`, and `expect.state` writing `humanlost=true`, `aiwon=true`,
`over=true` down as the assertion.

**`monarch=<player>` and `initiative=<player>` are the designations, Crucible-only like `over=`.** Java's
`GameState.toString` writes the monarch's "The Monarch" (and the initiative holder's "The Initiative") effect card into
the command zone by name, and nothing can load it back: no card database holds it. `State.Monarch`/`State.Initiative`
write the designations instead. `Load` recreates each card through `Game.SetMonarch`/`SetInitiative` (no trigger runs);
`Dump` writes `monarch=` and `initiative=` and leaves every designation card (`Game.IsDesignationCard`) out of the
command zone; `compareGames` compares both by name. An unknown player is a `Load` error. Reason: the card is state the
designation implies, so writing both would let a fixture say two contradicting things.
`testdata/scenarios/monarch-draws-at-end-step-and-passes-by-combat-damage` is the example.

**`<player>numringtemptedyou=<n>` and `|IsRingBearer` are Java's own keys, applied.** `Load` follows
`GameState.setupPlayerState` (`GameState.java:1157`, `:1213-1220`): after the player's cards, `Game.SetRingTemptedYou`
sets the count and, when above zero, makes the player's "The Ring" card carrying every level up to it (no trigger runs).
`|IsRingBearer` (`GameState.java:1350-1352`) makes the card the Ring-bearer of the player whose zone holds it
(`Game.SetRingBearer`). `Dump` writes the count when nonzero (Java writes `=0` too; Crucible's `Write` omits zero the
way it omits `landsplayed=0`), `|IsRingBearer` on the bearer's battlefield entry, and leaves "The Ring" out of the
command zone (`Game.IsDesignationCard`) -- the count implies it. `compareGames` compares the count and the bearer's
battlefield position. `testdata/scenarios/ring-tempts-you-nazgul-becomes-ring-bearer` and
`ring-bearer-cant-be-blocked-by-greater-power` are the examples.

**`|UnlockedRoom:<LeftSplit|RightSplit>` is Java's own key, applied.** `Load` unlocks that door of a Room on the
battlefield through `Game.LoadUnlockedDoor`, no trigger (`GameState.java:1419`; Java suppresses every trigger during
setup, `:617`); off the battlefield it lands in `Unapplied`. `Dump` writes one per unlocked door, last in the entry,
left then right (`GameState.java:445-450`), and names a Room by its printed card (`Card.PrintedDef`), not its door view.
`compareGames` compares unlocked doors. `testdata/scenarios/room-lock-or-unlock-fires-that-doors-trigger` is the example
([`effects-unlockdoor.md`](game-state/effects-unlockdoor.md)).

**`Protector:` is `lost=`/`won=`/`over=`'s own pattern applied to a single card field.** `Card.ProtectingPlayer` (CR
704.5w, `game-state.md`'s "Combat") is Crucible state with no Java `GameState` key to diverge from at all — grep finds
nothing resembling one in `GameState.java`. `Load`'s case is `Owner:`'s own shape exactly (`playerSlot`, then
`ld.slotToID[slot]`), gated to Battlefield-only in `Dump` the same way `Owner:`/`Tapped`/`Damage` are, since `Move`
clears it on the same "left the battlefield" transition. Written only when set (`ProtectingPlayer != NoPlayer`), the
same "say nothing when there's nothing to say" discipline `Owner:` already follows for when owner equals controller.
`testdata/scenarios/battle-protector-assigned-to-opponent` exercises the whole path: `queue battleprotector`, a
`Counters:DEFENSE=` high enough to survive `destroyZeroDefense` until the state-based action that assigns a protector
runs (CR 704.5v's ETB gap means a Battle placed directly on the battlefield starts at zero defense otherwise), and
`expect.state` naming the result with `Protector:`.

`TestScenarios` loads the real corpus once per test binary run (`sync.Once`), not once per scenario — synthetic cards
would defeat the point of a format meant to run against the Java oracle too, and 33,913 cards is too much to pay for per
case. That first load costs real time (order a minute, cold); TEST-13 already prices L3 at "every commit," same as L1,
so this is the cost that entry was always going to have once scenarios existed to pay it.

**Combat's own fixtures cover what its Go unit tests already prove, at the whole-engine level CLAUDE.md's testing table
asks for.** Every combat mechanic — declaring attackers/blockers, first strike, trample, gang blocking, attacking a
planeswalker, the legend rule — landed with full `package engine_test` coverage, but none of it had a
`testdata/scenarios/*` directory until this pass added seven: `combat-attacker-unblocked`,
`combat-single-block-kills-attacker`, `combat-first-strike-prevents-return-damage`, `combat-trample-excess-to-player`,
`combat-gang-block-damage-assignment`, `combat-attack-a-planeswalker`, `legend-rule-keeps-one`. Real corpus cards
throughout — Silvercoat Lion; Silver Knight; Craw Giant; Craw Wurm; Narset, Parter of Veils; Isamaru, Hound of Konda —
not synthetic defs, since `TestScenarios` runs against the real corpus and a scenario naming a card the corpus doesn't
have is a scenario with a typo. Each card's non-combat text (Rampage on Craw Giant, an activated ability on Narset) is
inert here on purpose: nothing this port has built fires a trigger, evaluates a static ability or activates anything, so
a real card's full script is exactly as safe a source of "just the keyword/type/P-T this scenario needs" as a synthetic
one — safer, since it also proves the scenario would keep meaning what it says once those systems exist and start
reading the rest of that same script.

Getting a first-strike or gang-block scenario right needs the phase walk to be real, not shortcut: `declareattackers`
and `declareblockers` each run while `AdvancePhase` has actually put the game in the matching phase
(`Declare Attackers`, `Declare Blockers`), one `advance` apart, because nothing in either method reads `ActivePhase` to
enforce that itself (game-state.md's "Combat" section) — a scenario that called them back-to-back without advancing
would still "work" mechanically but would end up asserting a phase that never happened. The state-based-action check
between the first-strike and regular damage steps is the sharper version of the same discipline:
`combat-first-strike-prevents-return-damage` only gets the right answer because `advance`ing from `First Strike Damage`
into `Combat Damage` is what actually kills the lethally-struck blocker before `combatdamage` runs
(`CheckStateBasedActions` runs on every phase entry, `game-state.md`'s "Turn structure") — skipping that `advance` would
leave the blocker alive to hit back, a different (wrong) scenario the fixture format makes easy to write by accident if
the phase walk isn't respected.

**`combat-mixed-first-strike-gang-block`** covers a combination the original seven didn't: one gang-blocked attacker
with blockers on both sides of the first-strike line, not one fixture per keyword. Durkwood Boars (4/4, no keywords) is
blocked by Elvish Archers (2/1, First Strike) and Devoted Hero (1/2, no keywords) — `firststrikedamage` only lets
Archers act, and `combatdamage`'s own `AssignCombatDamage` call still has to see both blockers as live, since neither
has taken any damage yet at that point (Archers dealt damage in the earlier step; nothing has dealt any to it). Getting
`dealsInStep`'s per-creature check wrong in either direction — Archers firing twice, or `AssignCombatDamage` only being
offered the blocker without first strike — is exactly the class of bug a first-strike fixture and a gang-block fixture,
each exercised alone, cannot catch.

**`combat-deathtouch-kills-regardless-of-toughness`** is the first fixture to touch Deathtouch at all. Typhoid Rats
(1/1, Deathtouch) attacks; Durkwood Boars (4/4, no keywords) blocks. A single point of damage is lethal to the Boars
despite its 4 toughness — `destroyDamagedCreatures` reading the deathtouch flag `dealPermanentDamage` set on the mark
(`game-state.md`'s "Lethal and deathtouch damage" section), not the raw amount against toughness a Go unit test
isolating that one function already proves correctly in isolation but which no scenario had exercised end to end through
declare-attackers/declare-blockers/combat-damage/state-based-actions together.

**`combat-split-across-two-defending-players`** is the first fixture to seat three players. Grizzly Bears attacks ai,
Silvercoat Lion attacks p2 — one combat, two defending players (CR 506.4) — and each is blocked by only that defender's
own creature (Hill Giant, Durkwood Boars), proving `DeclareCombatBlockers`' per-defender grouping asks the right player
about the right attacker rather than assuming one shared defender the way it did before this fixture existed. Human's
library needed a real card, not an empty one: CR 103.8a's "the first player skips their first draw step" is
two-player-only, so unlike every other fixture here (which are all two-player and rely on that skip), this one's active
player draws for real on turn 1 — the fixture's own `humanhand=Hill Giant` is that draw, not a card placed directly in
hand.

**`cleanup-discards-to-hand-size`** is the same discipline applied to CR 514.1 rather than combat: nine real cards
(Mountain) in hand, twelve `advance`s from `Untap` to land exactly on `Cleanup` (`Untap` is phase 0, `Cleanup` is 12),
`queue discard` naming the two that should leave. The count matters here more than in most scenarios — one `advance`
short lands on `End of Turn` instead, where `cleanupStep` never runs at all and the queued discard is simply never read.

Five state-based actions `CheckStateBasedActions`'s own doc comment lists had no fixture at all:
`counters-annihilate-plus-minus` (CR 704.5q), `life-loss-at-zero` (CR 704.5a — `poison-loss` already covered 704.5c,
nothing covered 704.5a), `draw-from-empty-library-loses` (CR 704.5b, the same `advance`-three-times-from-ai's-Cleanup
shape `untap-and-draw` already uses to reach a real Draw step, but with no library at all),
`equipment-falls-off-without-destroying` (the "cleanup" rule's other half —
`aura-enchant-restriction-sends-illegal-aura-to-graveyard` already covers the Aura branch, nothing covered the
Equipment/Fortification one, which unattaches instead of dying) and `battle-zero-defense-destroyed` (CR 704.5v with no
`Counters:DEFENSE=` at all, `game-state.md`'s own CR 704.5v ETB gap). The last one needs `queue battleprotector` even
though the Battle is about to die the very next statement in the same pass — `assignBattleProtector` asks
unconditionally for any Battle with no protector yet, run before `destroyZeroDefense` gets a chance to send it to the
graveyard, so skipping the queued answer panics on an unread decision rather than skipping a question that was never
going to be asked.

Two more fixtures cover a corner case of a mechanic that already had a fixture, not a whole missing rule:
`combat-vigilance-attacker-stays-untapped` (CR 508.1f — every other combat fixture's attacker, Silvercoat Lion, has no
keywords, so none of them exercise `DeclareCombatAttackers`' own vigilance check) and
`legend-rule-three-copies-keeps-one` (CR 704.5j with three copies of Isamaru, Hound of Konda rather than
`legend-rule-keeps-one`'s two, proving `resolveLegendRule`'s destroy loop handles more than one "the other one" at
once).

Two more still cover combat damage itself, not a keyword or a rule around it: `combat-double-strike-deals-damage-twice`
(CR 702.4 — Raging Redcap, unblocked, hits for 1 in `firststrikedamage` and 1 again in `combatdamage`; every other
first-strike fixture's attacker has plain `First Strike`, which only ever deals damage once, so none of them reach
`dealsInStep`'s other branch) and `combat-attack-and-damage-a-battle` (CR 121.5 — `combat-attack-a-planeswalker`'s own
shape, but `dealPermanentDamage` removes `Defense` counters through a wholly separate `if t.Has(cardtype.Battle)` case,
not the `Loyalty` branch a planeswalker target already exercises; needs `queue battleprotector human` before
`startturn`, and specifically not `ai` — the Battle is `ai`-controlled here, and naming the controller itself as its own
protector leaves `assignBattleProtector`'s `selfProtector` case true, asking again on every later state-based-action
check instead of staying answered).

**Closing the P4 fixture-count floor (Plan Section 3.2's ≥300) added 305 more, all against real corpus cards, none
synthetic.** Every mechanic exercised was already proven by an existing fixture or Go unit test — this pass is corpus
_breadth_, the same reasoning `TestScenarios` runs against the real 33,913-card corpus at all rather than a synthetic
three-card `compile.DB`: a differential harness meant to run against the Java oracle needs real cards moving through it,
not just one representative example per rule. By category: single-block combat trades across ~140 distinct vanilla
creatures (`combat-<attacker>-attacks-<blocker>`, outcome — kills-attacker, kills-blocker, mutual trade, or neither —
computed from each pair's own printed power/toughness, no keyword involved); the same shape again for every creature
carrying a solo Vigilance, First Strike or Deathtouch keyword, each against a fresh corpus attacker or a fixed Devoted
Hero blocker (`combat-vigilance-*`, `combat-first-strike-*-attacks-devoted-hero`,
`combat-deathtouch-*-attacks-devoted-hero`); every solo-Trample creature in the corpus against that same Devoted Hero,
proving `lethalDamage`'s own toughness cap and trample-excess split across a real spread of power values
(`combat-trample-*-attacks-devoted-hero`); every mana-payment branch this port resolves but a fixture had not yet
reached — both sides of colourless hybrid, monocoloured hybrid, single-colour Phyrexian, and both named colours of a
hybrid Phyrexian shard (`mana-payment-colorless-hybrid-*`, `mana-payment-monocolored-hybrid-colored`,
`mana-payment-phyrexian-*`, `mana-payment-hybrid-phyrexian-color*`); `TapLandForMana` and `PlayLand` against every basic
land color and its snow-covered printing, not just Plains (`mana-payment-tap-<color>-for-mana`,
`mana-payment-resolves-snow-<color>`, `land-played-then-tapped-for-mana-<color>`); `CastSpell` against a non-Aura
permanent of every type this port can cast — artifact, enchantment, planeswalker, Battle, and a two-colour-cost creature
— not only the one creature example that landed with `castspell.go` itself
(`cast-an-artifact-spell-resolves-to-battlefield`, `cast-an-enchantment-spell-resolves-to-battlefield`,
`cast-a-planeswalker-spell-resolves-to-battlefield`, `cast-a-battle-spell-resolves-to-battlefield`,
`cast-a-two-color-creature-spell-resolves-to-battlefield`); a handful of state-based-action and win-condition corners
with no fixture yet — a planeswalker at zero loyalty (`planeswalker-zero-loyalty-destroyed`, `destroyZeroLoyalty`'s own
counterpart to `battle-zero-defense-destroyed`), three Worlds instead of two (`world-rule-three-copies-keeps-newest`), a
-1/-1-counter-only kill with no damage involved (`counters-minus-one-reduces-toughness-to-zero-destroyed`), an Aura
whose host dies mid-game rather than being illegal from the start (`aura-falls-off-when-host-dies-sent-to-graveyard`),
life below (not just at) zero, nine poison counters surviving where ten would not, and one player's elimination not
ending a three-player game (`life-loss-below-zero-also-loses`, `poison-nine-survives`,
`life-loss-eliminates-one-player-game-continues`); a second consecutive London mulligan, proving `londonTuckCount`'s
cost escalates rather than repeating (`mulligan-twice-tucks-cumulative`); and `PlayLand`'s own one-per-turn limit and
`cleanupStep`'s roll-forward, at the scenario level rather than only `land_test.go`'s unit level
(`land-play-limit-one-per-turn`, `cleanup-resets-lands-played-for-next-turn`). Two long-stale doc comments came out of
writing these: `destroyZeroLoyalty`/`destroyZeroDefense` (`action.go`) and two existing fixtures'
(`battle-protector-assigned-to-opponent`, `battle-zero-defense-destroyed`) own comments still said nothing granted a
planeswalker or Battle its starting counters, which stopped being true once `Move` gained that ETB handling
(`game-state.md`'s "Loyalty is not a layer" section) — fixed in place rather than left to mislead the next reader
(DOC-16).

## Deviations from Java

| Java                                                                                                         | Go                                                                                                                                                                                                                                                                                             |
| ------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `splitLine` throws on a blank line (`line.charAt(0)`)                                                        | A blank line is skipped. Reproducing a crash would make blank lines unwritable in Crucible's own fixtures for no benefit                                                                                                                                                                       |
| Unrecognised category prints to stderr and is dropped                                                        | Returned in `Unknown` (top-level keys) or `Loaded.Unapplied` (card annotations and player fields with no engine home), so a fixture silently testing nothing is a test failure instead of a log line                                                                                           |
| A malformed number (`humanlife=twenty`) throws and dies                                                      | Returned as an `error` naming the line, not a panic — a card script cannot cause this, but a typo in a hand-written fixture is exactly the kind of caller mistake `error` is for (GO-7)                                                                                                        |
| `PhaseType.smartValueOf` matches the script name or the enum constant name, case-insensitively, off one list | `phaseByName` tries `engine.PhaseByName` (script name, case-sensitive) first, then a second table of Java's enum constant names, because `engine.PhaseByName`'s contract is specifically the script vocabulary and `GameState.toString`'s own dump (bare `Enum#toString`) writes the other one |
| `RemoveSummoningSickness` is state `GameState` remembers                                                     | It is a one-time load directive. By the time `Load` returns, every card it applied to already has `SummonSick` false, and `Dump` writes that per card; re-emitting the directive would assert something `Dump` cannot actually know                                                            |
| `Id:` is arbitrary, chosen by whoever wrote the fixture                                                      | Always the card's own `CardID` (see above)                                                                                                                                                                                                                                                     |

## Pinned quirks (PORT-7)

- **`p10life` addresses player 1, not player 10.** `getPlayerState(key)` does
  `Integer.parseInt(String.valueOf(key.charAt(1)))` — one digit, always. A fixture that means player 10 cannot be
  written in this format, in either engine.
- **The first `=` splits, the rest of the value does not.** `humancounters=POISON=3` parses correctly by accident of
  this rule, not because counters get special handling.
- **Keys fold, values do not.** Both engines lowercase the key before matching and leave the value exactly as written.
- **`Tapped` and `SummonSick` match on prefix, no colon required.** `info.startsWith("Tapped")` accepts `Tapped`,
  `Tapped:True`, or in principle `TappedFoo` — reproduced because a fixture written either way has to load the same in
  both engines.

## Not ported yet

| Missing                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Lands                                  |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------- |
| Token cards (`t:`/`T:` entries) — need `TokenInfo`/`AbilityFactory`, neither built                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | M5-M6                                  |
| The rest of the per-card annotation grammar: `Renowned`, `Solved`, `Saddled`, `Suspected`, `Monstrous`, `PhasedOut`, `FaceDown`, `Transformed`/`Modal`/`Flipped`/`Meld`, `OnAdventure`, `IsCommander`, `EnchantingPlayer:`, `Ability:`, `ChosenColor:`/`ChosenType:`/`ChosenType2:`, `ChosenCards:`, `MergedCards:`, `NamedCard:`, `ExecuteScript:`, `ExiledWith:`, `Attacking`, `NoETBTrigs`, `Foretold`/`ForetoldThisTurn`, `IsToken`, `ClassLevel:`, `UnlockedRoom:` — each needs a mechanic or a type (`CardState`, `SpellAbility`, combat) this port has not reached | M5-M6, mechanic by mechanic            |
| Player-level `PersistentMana:`, `Speed:` — `engine.Player` has neither field yet. `NumRingTemptedYou:` is applied (`Game.SetRingTemptedYou`, since `RingTemptsYou`). `Counters:` is applied (`Player.Counters`, since M5's SBA work), `ManaPool:` (`Player.ManaPool`, `applyManaPool`, since M5's mana-payment work), and `LandsPlayed:`/`LandsPlayedLastTurn:` (`Player.LandsPlayed`/`LandsPlayedLastTurn`, since `PlayLand`) are all applied now                                                                                                                        | M5-M6, as each field lands on `Player` |
| `ability<key>=` string values are stored verbatim in `AbilityStrings`; nothing parses or resolves them (puzzle-mode precast targeting)                                                                                                                                                                                                                                                                                                                                                                                                                                    | Puzzle mode, if ever                   |
| `[metadata]` section (puzzle-mode name/description)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | Puzzle mode, if ever                   |
| `actions.log` verbs for casting anything an instant or sorcery needs — nothing downstream of `ScriptedController` can answer what one resolves into yet. Combat, mana payment (`paymanacost` and every `queue` kind `PayManaCost` can ask), the one mana ability this port has (`tapformana`), playing a land (`playland` — not casting a spell at all, CR 305.1) and casting/resolving a permanent spell including an Aura's own cast-time target (`castspell`/`resolvestack`/`queue enchanttarget`) all have verbs                                                      | M5-M6                                  |
| `expect.events` — the Plan's own fixture shape names it (Section 3.5) alongside `setup.state`/`actions.log`/`expect.state`, but `TestScenarios` (`internal/engine/scenario_test.go`) never reads a fourth file: `runScenario` loads only `setup.state` and `expect.state` and calls `compareGames`, which does not touch `Game`'s event sink at all. A fixture proving `LifeChanged`/`CounterChanged` actually fired (not just that life or a counter ended up at the right number) has nowhere to assert that yet                                                        | M5-M6                                  |
