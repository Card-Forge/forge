# Port Log — Game State: Mana and Casting

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)

Mana pool and payment, land plays, casting spells and Auras.

## Mana pool and payment

`mana.go`'s `Pool` (CR 106.4, one per `Player`) and its `Pay` method (CR 601.2h/601.2i) are M5 item 28's mana-payment
slice — the plan's own "budget the most time here" warning is about the full version, and this is deliberately not that:
`Pay` itself handles a cost's `Generic` amount plus its six "pure" shards (`ShardW`/`U`/`B`/`R`/`G`/`C`) and nothing
else, the same "plain-integer operand" discipline `valid.go`'s `compareMatches` already applies to numeric comparisons.
`Game.PayManaCost` (`manapay.go`) layers eight harder cases on top without touching `Pay` directly: `{X}` (CR 601.2b)
asks `ChoosePayX` for the value of X exactly once, before anything else in the cost resolves, and folds it into
`Generic` as `x * cost.CountX()` — every `X` symbol the cost carries stands for the same announced value, not one value
each (CR 107.3f), so a cost with two `{X}` symbols owes twice the chosen amount, and `ChoosePayX` is asked once
regardless of how many `{X}` symbols there are. A negative answer is not re-checked against anything downstream —
`PayManaCost` itself reports failure before the shard loop or `Pool.Pay` ever run, since CR 601.2b restricts X to a
non-negative integer and there is no meaningful `Pay`-level failure to delegate that to. Then a two-colour hybrid shard
(`{W/U}`) asks `ChooseHybridManaColor` which colour to pay with, substitutes the plain shard for the answer, and hands
the result to `Pay` unchanged; a monocoloured hybrid (`{2/W}`) asks `ChoosePayMonocoloredHybrid` whether to pay with
colour or with the shard's own `CMC` (2) worth of generic instead — `true` substitutes the plain colour shard the same
way the two-colour case does, `false` adds the shard's `CMC` onto the cost's `Generic` amount instead of adding a shard
at all; a colourless hybrid (`{C/W}`) asks `ChoosePayColorlessHybrid` the same true/false shape, but `false` substitutes
`mana.ShardC` for the symbol instead of touching `Generic` — its other side is a specific mana type, not an amount, so
it is resolved the same way the two-colour case's colour choice is, not the way the monocoloured case's generic choice
is; a single-colour Phyrexian shard (`{W/P}`, CR 118.4) asks `ChoosePayPhyrexian` the same true/false shape, but `false`
adds 2 to a running `life` total instead of touching `resolved` or `Generic` at all; a hybrid Phyrexian shard
(`{B/G/P}`) asks `ChoosePayHybridPhyrexian` a genuinely three-way question — its return type is `mana.Colors`, not
`bool`, since there are two colours to offer plus life, and returning the zero `mana.Colors` is how the controller picks
life over either one, the same "reuse the type, encode the third option in its zero value" shape
`ChooseHybridManaColor`'s own two-colour return already established, just extended one option further. Once every shard
is resolved, `PayManaCost` asks `ChoosePayGeneric` once per unit of the cost's `Generic` amount still owed (CR 106.6:
"any type of mana, including colorless mana, can be used to pay a generic mana cost") — a plain `mana.Shard` answer (one
of `ShardW`/`U`/`B`/`R`/`G`/`C`), appended to the resolved shards the same as every other case, then handed to `Pay`
with `Generic` itself reduced to zero. Each unit is its own call, not one combined answer for the whole amount — the
same "one decision per shard" granularity `ChooseHybridManaColor` already uses per hybrid symbol, not batched across a
whole cost. `PayManaCost` only deducts `life` from `Player.Life` directly, after `Pool.Pay` reports success, so a
payment that fails on an unrelated shard never costs life for a Phyrexian shard (either kind) it already resolved.
Paying life this way fires `LifeChanged` with `Source: NoCard` (no card causes it — `PayManaCost` takes no card
parameter today) and `Amount` as the negative life lost, the same wiring discipline `CounterChanged` got when a real
mutator needed it ([`## Events, wired`](targeting-and-chaining.md#events-wired)). A snow (`{S}`, CR 106.3a) shard asks
`ChoosePaySnow` which color of floating snow mana pays it — unlike `{X}`, each `{S}` symbol in a cost is its own
independent question (CR 106.3a puts no "announced once" language on it the way CR 601.2b does for X), so a cost with
two `{S}` symbols asks twice and can take two different colors' snow mana. The answer is collected into a separate
`snow []mana.Shard` slice, not `resolved`, because a snow requirement can only be paid from `Pool`'s own snow bucket for
that color, never the plain one — folding it into `resolved` the way every other shard is would let `Pool.Pay`'s
plain-pip matching spend ordinary mana for it, which is not legal. All eight ask before `Pool.PayWithSnow` (`mana.go`)
ever sees the cost; `Pay` itself (now `PayWithSnow` called with no snow shards) is unaware any hybrid, Phyrexian, `{X}`,
`{S}` or controller-chosen generic shard exists — it always receives an already-resolved shard list, a `Generic` of
zero, and an empty snow slice when called from here.

**Nothing casts a spell yet, and `Pay` does not need one to be worth building.** `turn.go`'s own doc comment already
says why the priority loop isn't wired in: no `PlayerController` method can cast or activate anything, so `Pay` has no
real caller today beyond its own tests — the same position `DeclareCombatAttackers`/`AssignCombatDamage` were in before
anything glued a full combat together, and CR 106/601.2h is exactly as self-contained a rules chapter as CR 508-510 was.
What is not deferrable is CR 500.4: mana already empties between every phase and step regardless of whether anything is
being cast, so `emptyManaPools` is a real, unconditional consumer of `Pool` from the moment `beginPhase` exists — not a
hypothetical one waiting on a future effect, the gap every other "build state ahead of its writer" call this port has
made (`Memory`, before anything in `effect.go`'s empty `Registry` could write to it) had to weigh instead.

**`emptyManaPools` runs at the top of `beginPhase`, not a separate `onPhaseEnd`.** Java's `PhaseHandler.onPhaseEnd`
clears every player's pool once per transition, right before the next phase's `onPhaseBegin` runs; this port's phase
walk has no separate "ending" hook (`beginPhase`'s own comment: `AdvancePhase` "walks through them as bookkeeping
only... until each one's turn comes"), so the one hook that already fires on every transition is where CR 500.4 lands —
same cadence, same effect, just attached to whichever half of the transition this port actually implemented. Mana burn
(losing life for mana left unspent) is not reproduced: it left the rules in 2010, before anything this port's corpus
targets, so there is no parity to keep with a rule no card in scope was ever printed under.

**`Pay`'s own fixed generic order only fires for a caller that reaches it directly, bypassing `PayManaCost`.** CR
106.6/601.2h give the paying player free choice of which floating mana covers a generic cost; `Pay` itself still spends
colorless first, then white/blue/black/red/green, a deterministic tie-break rather than a decision, for exactly the
callers that were already calling `Pay` before `PayManaCost` existed (its own tests, `mana_test.go`). Every real answer
to that choice now goes through `ChoosePayGeneric` instead (above) — `PayManaCost` never leaves a nonzero `Generic` for
`Pay` to guess about.

**`PayManaCost` now has real `TEST-5` fixture coverage, not just unit tests.** Two gaps blocked writing one:
`setup.state`'s own `manapool=` key (Java's `GameState` format) parsed but never applied to `Player.ManaPool`
(`internal/fixture/load.go`'s `Unapplied` list carried it since before `Pool` existed), and `actions.log` had no verb to
call `PayManaCost` at all. Both are fixed: `applyManaPool` reads `manapool=`'s space-separated color letters (`"W W U"`,
`MagicColor.Color`'s own short names, not a mana cost's `"2W"` shorthand) into the pool the same
`Pool.Add`/`AddColorless` every unit test already uses, and `paymanacost <player> <cost>` (`queue paygeneric <shard>`
for its own generic answers) calls `Game.PayManaCost` directly — the same "callable ahead of a full turn" position
`DeclareCombatAttackers` was in before combat glued together (`manapay.go`'s own doc comment). `Pool.Breakdown`
(`mana.go`) is the new exported reader both the fixture harness's own `compareGames` and any future caller need to
compare two pools' full contents rather than just `Total`. `PersistentMana:` stays `Unapplied`: `Pool` tracks no
persistence, and CR 500.4's own emptying applies to every kind of floating mana this port has. Every hybrid and
Phyrexian shard has its own verb too now (`queue hybridmanacolor`, `queue paymonocoloredhybrid`,
`queue paycolorlesshybrid`, `queue payphyrexian`, `queue payhybridphyrexian` -- `game-state-fixture.md`'s own verb
table), each mirroring its `ScriptedController` method's argument shape exactly -- a bare color letter or a bool, and
`payhybridphyrexian`'s own third answer written as the literal word `life` rather than an empty value, the same "decline
explicitly" convention `queue attackers none`/`queue blocks none` already use. Five fixtures exercise six of the seven
resolved shapes end to end: `mana-payment-pays-colored-and-generic`, `mana-payment-fails-atomically`,
`mana-payment-hybrid-color-choice`, `mana-payment-monocolored-hybrid-generic` and `mana-payment-hybrid-phyrexian-life`.

**`{X}` turned out not to need a real caster after all.** Earlier passes over this section assumed CR 601.2b's "the
player announces X" belonged to the missing casting flow (M6) and left it unresolved alongside snow, which turned out to
need no caster either (below). Revisiting `{X}`: `PayManaCost` is already called standalone, ahead of any cast (the same
position every other shape here is in), and CR 601.2b's announcement is itself just one more decision `PlayerController`
can be asked before the rest of the cost resolves — no different in kind from `ChoosePayGeneric` asking which mana
covers a generic unit. `ChoosePayX(g, decider, cost) int` is that decision, asked once per cost regardless of how many
`{X}` symbols it carries, and its answer times `cost.CountX()` is added to `Generic` before the shard loop runs at all.
`queue payx <n>` (`game-state-fixture.md`) is the verb; `mana-payment-resolves-x` is the fixture, paying `{X}{R}` with
X=3 from a pool of one red and three white.

**`Pool.Add` has a real (non-test) caller now: `TapLandForMana` (`manaability.go`), CR 305.6's intrinsic land ability.**
Every fixture above preloads the pool through `manapool=` directly; nothing in the engine had ever put mana there
itself. The blocking question — how Forge derives a basic land's "T: Add [color]" ability, since
`forge-gui/res/cardsfolder/p/plains.txt` carries no `A:` line at all, only `Oracle:({T}: Add {W}.)` — turned out to live
in a file the previous search had not checked: `CardState.java`'s `getLandTraitChanges`/`getLandManaForColor` walks
`MagicColor.Color.values()`, and for each one whose `getBasicLandType()` the card's current type line has as a subtype
(`hasSubtype("Plains")` for white, and so on for the other four), synthesizes
`AB$ Mana | Cost$ T | Produced$ <color> | Secondary$ True | ...` on the fly rather than reading it from script text.
That is a fixed CR 305.6 mapping, not script content a compiled `AST` carries, so this port keys off `cardtype.Line`'s
own subtypes the same way `enchantSpec`/`resolveWorldRule` already read a type line directly
([`## State-based actions`](state-based-actions.md#state-based-actions), above) instead of waiting on M6's effect
dispatch. `basicLandType` holds the five-entry map (`Plains`→White, `Island`→Blue, `Swamp`→Black, `Mountain`→Red,
`Forest`→Green); `TapLandForMana(pid, land, color)` checks control, zone, tapped state and the matching subtype, then
taps and calls `Pool.Add` in the same call, since CR 605.3 gives a mana ability no stack to wait on. It reports `bool`,
the same "declined by the rules, not a bug" contract `PayManaCost` already carries — a dual-typed land (a Snow-Covered
Plains Island) keeps two separate intrinsic abilities, but tapping is one shared cost, so activating either one leaves
the other unavailable. Whether the mana produced is snow (CR 106.3a) is read off the land's own Snow supertype
(`cardtype.Snow`) at the very end, once tapping is known to succeed —
`forge-gui/res/cardsfolder/s/snow_covered_plains.txt` writes `Types:Basic Snow Land Plains`, the identical no-`A:`-line
shape a plain Plains has, differing only in that one supertype, so nothing about the ability itself changes, only which
of `Pool`'s two buckets for that color receives it ([`## Mana pool and payment`](#mana-pool-and-payment), above).
`tapformana <player> <id> <color>` is `actions.log`'s own verb for it (`game-state-fixture.md`), and
`mana-payment-tap-land-for-mana` is the first fixture where the paid mana comes from a card instead of `manapool=`.

**Snow turned out to need a bigger change than `{X}` did, but still no caster.** The earlier assumption was that snow
needed "a `Pool` redesign for snow-provenance" — true as far as it goes, but the redesign is a fixed, bounded one, not
an open-ended one: `Pool` (`mana.go`) gained a second bucket per color (`snowWhite`, `snowBlue`, ...) alongside the six
plain ones, disjoint rather than a subset count layered on the plain total, so spending never has to reconcile which
specific unit of a color was snow after the fact. `Pool.AddSnow`/`AddSnowColorless` mirror `Add`/`AddColorless` exactly;
`Pool.SnowBreakdown` mirrors `Breakdown`'s own shape for the snow half alone, and `Breakdown` itself now sums both
buckets per color (CR 106.3a: snow mana is still that color), so an existing caller reading total mana of a color is
unaffected by snow's existence. The harder part was where snow mana can substitute for plain: a same-color pip or a
generic unit accepts snow-tagged mana the same as plain (CR 106.3a again — snow is a type of that color, not a different
one), so `Pool.Pay`'s own shard loop and generic loop both fall back to the snow bucket once the plain one is empty; a
snow (`{S}`) requirement is the one thing plain mana cannot cover, so it needs its own consumption path that never
touches a plain bucket. `Pool.PayWithSnow(cost, snow []mana.Shard)` is that path — `Pay` itself is now `PayWithSnow`
called with `snow` nil, so its signature and every existing caller are unchanged. `ChoosePaySnow(g, decider) mana.Shard`
is `PayManaCost`'s own decision, asked once per `{S}` symbol independently (unlike `{X}`, CR 106.3a puts no "announced
once" language on `{S}`, so two `{S}` symbols in one cost can take two different colors' snow mana) — its answers
collect into a `snow` slice kept separate from `resolved` for exactly the reason above: folding a snow answer into
`resolved` would let the ordinary plain-pip matching spend non-snow mana for it. `queue paysnow <shard>`
(`game-state-fixture.md`) is the verb; `mana-payment-resolves-snow` is the fixture — a real Snow-Covered Plains tapped
for snow white (`TapLandForMana`'s own Snow-supertype check, above), then spent paying a bare `{S}` cost.
`setup.state`'s own `manapool=` cannot express snow mana at all: `GameState.java`'s own
`processManaPool`/`updateManaPool` iterate `ManaAtom.MANATYPES`
(`forge-core/src/main/java/forge/card/mana/ManaAtom.java` — white/blue/black/red/green/colorless, no snow entry), so the
Java oracle's own dump format has nowhere to write a snow flag either; this is not a deviation this port introduces, it
is the format's own limit, and `queue paysnow` (a `ScriptedController` answer, `game-state-fixture.md`'s existing
Crucible-only category) plus `tapformana` on a real snow land are the only way a fixture gets snow mana into a pool
today.

---

## Playing a land is not casting a spell

`Game.PlayLand` (`land.go`) is CR 305: no cost, no stack (CR 305.1) — the card moves straight from hand to the
battlefield. It is the first thing in this port that gets a card from a player's hand onto the battlefield through a
real game action rather than `setup.state` placing it there directly, which is why it took this long to reach even
though nothing about it needed the M6 effect-dispatch machinery `manaability.go`'s own precedent already established
mana abilities and basic-land-type checks do not need: CR 305 is a fixed rule with no script content to interpret, the
same category `TapLandForMana` and `enchantSpec`/`resolveWorldRule` are already in.

Timing is CR 305.3's own gate ("any time they could cast a sorcery"), collapsed to what this port can check without an
interactive priority system: `pid` is the active player, `ActivePhase` is `Main1` or `Main2`, and the stack is empty.
That last check is never false today — nothing pushes an ability yet outside `stack.go`'s own tests — but is checked
anyway, on the same "should not have to change again once casting exists to make it meaningful" reasoning
`ResolveStack`'s own doc comment already gives for building the resolve loop ahead of a real pusher. `mayPlay` alternate
zones, `CantBeCast` static abilities and every other `canPlayLand` condition Java checks beyond these three plus "in
hand" plus "is a land" are M5-M6 gaps this port does not have the machinery for yet (a quality-matching static-ability
engine, mostly) and are not checked, the same "a rule this port has not implemented simply never fires" position
`CheckStateBasedActions`'s own doc comment already states for its own gaps.

CR 305.2's one-land-per-turn limit is `Player.LandsPlayed` (`player.go`), a genuinely new field with a real caller —
`engine.Player` had no lands-played count at all before this, even though `setup.state`'s own `landsplayed=`/
`landsplayedlastturn=` keys have existed and parsed successfully since before `PlayLand` did, landing in `Unapplied` for
lack of anywhere to put them (the same position `manapool=` was in before `TapLandForMana`). `maxLandPlays` is Java's
own `getMaxLandPlays()` default of 1 with no `adjustLandPlays` term added — nothing in this port grants an extra land
play yet, so there is nothing to add. `cleanupStep` (`turn.go`) now rolls `LandsPlayed` into `LandsPlayedLastTurn` and
resets it to zero for every player, not just the active one — `Game.onCleanupPhase` in Java loops every registered
player the same way, the same "every player, not just the active one" scope CR 500.4's own mana-pool emptying already
has in this port. `LandsPlayedLastTurn` has no reader yet (a replacement effect keyed on "if you've played a land this
turn" would be one), but resets alongside `LandsPlayed` regardless, since nothing about resetting per-turn state should
wait on a reader existing before it starts happening correctly.

`playland <player> <id>` (`game-state-fixture.md`) is the verb, `id` from `Loaded.CardByFixtureID` the same as
`tapformana`. `Game.PlayLand`'s `bool` return is not asserted, the same "declined by the rules, not a fixture error"
convention `paymanacost`/`tapformana` already established. `land-played-then-tapped-for-mana` is the fixture: a Plains
drawn into hand at setup, played, then tapped for its own intrinsic mana the same turn and spent paying a `{W}` cost —
CR 302.6's summoning-sickness restriction is a creature's own tap-ability gate, not a land's mana ability, so
`TapLandForMana` correctly has no `SummonSick` check to get in the way of playing and tapping the same land in one turn.

`compareGames` (`scenario_test.go`) gained `LandsPlayed`/`LandsPlayedLastTurn` alongside `ManaPool`'s own two
comparisons — a field `Load` now applies has to be a field the scenario harness actually checks, or a fixture naming it
would silently assert nothing (the exact gap the fixture-level `Dump` audit that found `ManaPool`'s own missing
write-back caught, `game-state-fixture.md`'s own section on it).

---

## Casting a spell needed the stack for real, for the first time

`Game.CastSpell` (`castspell.go`) is CR 601, trimmed to the two shapes with nothing left to decide once a target (an
Aura) or nothing (every other permanent) is chosen. `PlayLand`'s own precedent — a fixed CR rule needs none of the M6
effect-dispatch machinery its neighbors in this file keep deferring to — turned out to reach further than land-playing
alone: casting a permanent and resolving it into a battlefield permanent is _also_ a fixed rule, not a card-script
effect, once `APIPermanentCreature`/`APIPermanentNoncreature` (`ability.go`'s generated constants) and `effect.go`'s own
`Effect`/`Registry` dispatch (built at M4, holding zero implementations since — `CLAUDE.md`'s own M4 status line) are
read together: Java's `SpellPermanent` constructs one or the other API depending on `cardstate.getType().isCreature()`,
but never through `AbilityFactory.getAbility`'s script-string dispatch the way a real effect implementation would — the
same "hardcoded, not corpus-script-driven" shape `TapLandForMana`'s own `CardState.java` precedent already established
for a different API entirely.

`castableAsPermanent` is `CardState.java`'s own `getBasicSpells` routing, read directly: a creature, artifact,
enchantment, planeswalker or Battle, and not an Aura. An Aura routes to `getAuraSpell()` in Java (`castAura`,
[`## Aura targeting is a spell's own second cast-time decision`](#aura-targeting-is-a-spells-own-second-cast-time-decision),
below) since it needs a target chosen at cast time (CR 601.2c) that this no-decision branch has none of. A land is never
a spell at all (CR 305.1) and needs no special case: it is simply absent from the list `castableAsPermanent` checks, the
same "excluded by not appearing" shape `PlayLand`'s own doc comment already uses for the reverse case (a non-land
declined by `PlayLand`). Instant and sorcery route to a `SpellAbility` this port does not build yet (they resolve into a
script effect, not "become a permanent") and are excluded the same way.

Timing is `PlayLand`'s own CR 305.3 check, copied rather than shared: active player, a main phase, an empty stack. CR
601.3a gives permanent spells the identical sorcery-speed default lands have (CR 307.5), so the two checks read
identically today — they diverge the moment a real Flash-granting effect exists, which is exactly why `CastSpell` keeps
its own copy instead of factoring out a helper for a coincidence that will not stay one.

**`permanentEffect` is the first `Effect` implementation with a real (non-test) caller.** `effect.go`'s own doc comment
already predicted its shape — "Implementations are stateless shared values" — and `PermanentEffect.java`'s own `resolve`
confirms it: strip `Dash`/`Blitz`/`Warp`/`Sneak` (alternate-cast-mode keywords this port cannot grant a spell), and what
is left is `game.getAction().moveToPlay` plus `table.triggerChangesZoneAll` (CR 603 firing) —
`Game.Move(a.Source, Battlefield, a.Controller)` followed by `checkETBTriggers(a.Source)` here
(`## Trigger firing found its first mode`, below). Java gives `PermanentCreatureEffect` its own subclass only to
override `getStackDescription` (display text for the stack, showing power/toughness); this port has no stack-description
system at all, so one value answers for both `APIPermanentCreature` and `APIPermanentNoncreature` — `NewRegistry()`
registers it twice. `Move`'s own ETB logic (loyalty/defense grants, `SummonSick = true`) already existed and needed no
change: a permanent entering the battlefield by resolving off the stack is not a special case of entering, so casting a
planeswalker or Battle spell grants loyalty/defense correctly for free
(`cast-a-planeswalker-spell-resolves-to-battlefield`, `cast-a-battle-spell-reaches-the-stack`).

`ResolveStack` (`stack.go`) had zero non-test callers before this — its own doc comment said as much ("casting has no
cost-payment or targeting to drive it"). `CastSpell` pushing a real `Ability` and `NewRegistry` giving `ResolveStack`
something to dispatch to is what makes that sentence no longer true, for the one shape it can reach. `resolvestack`
(`game-state-fixture.md`) is the fixture verb, and — unlike every bool-returning verb in this file — it does not swallow
its error: `Registry.Resolve`'s own `ErrUnimplemented` is a real gap (GO-7's "a bad card fails its game"), not a
declined decision, so a fixture naming an API this port cannot resolve yet fails loud instead of silently doing nothing.

`castspell <player> <id>` is the verb for `CastSpell` itself, `id` from `Loaded.CardByFixtureID` the same as every other
card-naming verb; its `bool` return is not asserted, the same convention `paymanacost`/`tapformana`/`playland` already
established. `cast-a-creature-spell-resolves-to-battlefield` is the fixture: two Forests tapped for a real Grizzly
Bears' `{1}{G}` cost, cast, then resolved onto the battlefield — the mana comes from real lands tapped after reaching
Main1, not `setup.state`'s own `manapool=`, because `emptyManaPools` (CR 500.4) clears any preloaded pool on the very
first `startturn`/`advance` a scenario runs, the same trap `mana-payment-tap-land-for-mana`'s own fixtures already route
around by tapping mid-scenario rather than preloading.

---

## Aura targeting is a spell's own second cast-time decision

`castAura` (`castspell.go`) is `CastSpell`'s own branch for an Aura, `CardState.java`'s `getAuraSpell()` read alongside
`AttachEffect.java`'s own `resolve`. Java builds an Aura's cast-time ability as
`SP$ Attach | ValidTgts$ Card.CanBeEnchantedBy,Player.CanBeEnchantedBy` — a generic `Attach` spell whose own
target-choosing machinery (`TargetSelection`, interactive targeting) this port does not have. What survives the trim is
CR 601.2c's own requirement stripped to its essentials: a target is chosen before the cost is paid, from whatever the
Aura's own `Enchant` restriction (`enchantSpec`,
[`## The World rule needed no new field...`](state-based-actions.md#the-world-rule-needed-no-new-field-only-the-one-every-zone-change-already-stamps)'s
neighbor section, above — the same helper `cleanupDanglingAttachments` already uses) actually allows.

`enchantTargets` (castspell.go) builds that eligible set: every battlefield permanent, across every player, `Matches`
([`## State-based actions`](state-based-actions.md#state-based-actions)) accepts against the Aura's own parsed spec,
`self` (`Matches`'s own `source` parameter) being the Aura's own id — the identical convention
`cleanupDanglingAttachments` already established for re-checking an attached Aura's restriction after the fact. Two
things decline the cast outright, both CR 601.2c's own "a spell requiring a target with none legal is illegal to cast":
`enchantSpec` finding nothing checkable at all (an `Enchant Player`/`Enchant Opponent` Aura — `enchantSpec`'s own doc
comment already names this gap, since `AttachedTo` has no representation for "attached to a player"), or a checkable
spec matching zero battlefield permanents. A lone eligible target is assigned automatically, `assignAttackTargets`'s own
"nothing meaningful to decide" reasoning; more than one asks `ChooseEnchantTarget`, the new twentieth `PlayerController`
method ([`## Controller`](../game-state.md#controller)).

`Ability` (`ability.go`) gained a `Target CardID` field for this — its own doc comment had already reserved the shape
("once casting or targeting exists to fill them") before this landed. `attachEffect` reads it back at resolution: `Move`
to the battlefield, `Attach` to `a.Target`, then `checkETBTriggers` the same as `permanentEffect`. CR 608.2b's own
fizzle check — re-validating the target is still legal right before resolving — is ported now
([`## Instant and Sorcery spells reach the stack for real`](#instant-and-sorcery-spells-reach-the-stack-for-real),
ADR-0018): a trigger resolving above an Aura on the stack can remove its target (a `SpellCast` trigger destroying,
exiling or bouncing it), and `targetsStillLegal` (`targeting.go`) catches it right before `attachEffect` would run.

`castspell`'s own DSL verb needed no change — `CastSpell` already branches internally — but `queue enchanttarget <id>`
(`game-state-fixture.md`) is new, `queue legendarykeep`'s own "pick one id from a list" shape.
`cast-an-aura-spell- attaches-to-chosen-target` is the fixture: a real Pacifism (`{1}{W}`, `K:Enchant:Creature`) cast at
a lone Grizzly Bears on the battlefield, assigned automatically, attached at resolution.

---

## Instant and Sorcery spells reach the stack for real

ADR-0018. `CastSpell` (`castspell.go`) gains a third branch, `castInstantOrSorcery`, for a card `castableAsPermanent`
and the Aura branch both decline: it finds the card's own `A:SP$` line (`Def.Faces[0].Abilities`,
`Record == compile.Spell` — the identical field `ActivateAbility` already reads for its own `A:AB$`/`A:T$` lines),
chooses modes through `chooseCharmModes` if it names `APICharm`, chooses targets through `resolveTargets` — the same
function `pushTriggeredAbilities` already calls for a triggered ability, its own doc comment's "a future cast path...
would call it from wherever that lands too" now true — pays the cost, then pushes
`Ability{API, Params, Amounts, Targets}` the same as `castAura` does for `APIAttach`. Unlike `castAura`,
`castInstantOrSorcery` cannot reuse `pushTriggeredAbilities` wholesale: cost payment has to sit between mode/target
selection and the push, and `pushTriggeredAbilities`' own all-in-one shape (modes, targets, push, in one call) has no
room for that — its own caller, an already-paid triggered or activated ability, never needs it.

`Ability` gained an `ID StackItemID` field (`id.go`), assigned by `PushAbility` from a new `Game.nextStackItemID`
monotonic counter, on `Card.Timestamp`'s own "never reused within a game" terms. Nothing reads it yet —
`CopySpellAbility`'s own `Defined$ TriggeredSpellAbility` shape is the first real consumer, still deferred
(`effects-play-copyspellability.md`) — but every stack push, cast or triggered, now carries one.

`ResolveStack` (`stack.go`) gained two things after popping the top ability and before/after dispatching it:

- **`targetsStillLegal`** (`targeting.go`), CR 608.2b's fizzle check: every chosen target re-checked on its own
  ([`## CR 608.2b`](targeting-and-chaining.md#cr-6082b-every-target-re-checked-at-resolution)). An Aura's own single
  `Target` keeps its own check, `auraTargetStillLegal`, which re-runs `enchantTargets`'s two checks (`Matches` against
  the `Enchant` spec, `cardCantBeTargetedBy`).
- **`moveResolvedSpellToGraveyard`** (`stack.go`), CR 608.2m's own "then it's put into its owner's graveyard," run after
  dispatch (fizzled or resolved) whenever the ability's own `Source` card is still in the `Stack` zone —
  `permanentEffect`/`attachEffect` already move their own source to the battlefield as part of what they resolve into,
  so this is a no-op for both. It is the only place an Instant or Sorcery's own source ever leaves the stack, since
  `destroyEffect`/`drawEffect`/... never touch their own host card. `checkMovedReplacement` (`replacement.go`) is not
  called here — it resolves CR 614.1's "enters the battlefield tapped" replacement specifically, wired only at the three
  battlefield-entry call sites that already use it. `ReplaceGraveyard$` (CR 614's own "goes to exile instead of a
  graveyard" redirect) has no resolver in this port and always ends up in the graveyard regardless.

Three scenarios: `cast-an-instant-destroy-spell-through-the-stack-to-graveyard` (a real Terror, `{1}{B}`, destroying a
targeted creature — the fixture this section is proven by) plus `TestCastSpellInstantResolvesThroughStackToGraveyard`/
`TestCastSpellSorceryWithNoTargetResolvesThroughStackToGraveyard` (`castspell_test.go`) at module level.

Not ported: interactive priority (a later ADR, ADR-0018's own scope explicitly excludes it — neither `Play` nor
`CopySpellAbility`'s dominant shape needs it, `effects-play-copyspellability.md`); a general `ValidTgts$` fizzle check
(above); `ReplaceGraveyard$` (above).

---

## CR 115's own "any target" lands, and targetCandidates stops picking one pool

`targetCandidates` (`targeting.go`) used to pick one pool -- every player, or every battlefield card -- by trying
`matchesPlayerSpec` once on the whole spec and branching on whether it was player-shaped at all. That was never Java's
own shape: `TargetRestrictions.getAllCandidates` (`forge-game/.../spellability/TargetRestrictions.java`) unconditionally
tries `game.getPlayers()` and `game.getCardsIn(zone)` for every real `ValidTgts$` string, and lets
`Player.isValid`/`Card.isValid` filter each pool on their own -- an ordinary card-shaped spec ("Creature.YouCtrl")
simply matches zero players the same way an ordinary player-shaped one ("Opponent") matches zero cards. `"Any"` (CR
115's own "any target") is not a structural special case on top of that: `Player.isValid`/`Card.isValid` both
special-case the literal string, unconditionally true for a player, `creature || planeswalker || Battle` for a card
(`matchesPlayerBase`'s own new `"Any"` case, `baseMatches`'s existing one, `valid.go`). `targetCandidates` now unions
both pools unconditionally too, matching Java's real structure -- not an `"Any"`-only patch: the corpus writes the same
mixed shape out explicitly too (`Player,Planeswalker`, 273 real lines corpus-wide), which the old either/or design could
never have answered correctly even before `"Any"` motivated fixing it. No existing caller regressed: every
already-ported `ValidTgts$` shape (`Destroy`, `Tap`, ...) is purely one-pool in the real corpus, so the added pool
always contributes zero candidates for them.

`dealDamageEffect` (`dealdamageeffect.go`) is the first real consumer: `ValidTgts$` (2,068 real corpus lines; 615 of
them `ValidTgts$ Any` alone -- Lightning Bolt, Shock, the corpus's own dominant shape) reads `Ability.Targets` directly
and dispatches per entity -- `dealPermanentDamage` for a card (CR 120.3c's own loyalty-counter branch, CR 121.5's own
defense-counter branch, both already shared with combat damage), `dealPlayerDamage` for a player -- a single loop with
an `AsCard`/`AsPlayer` check, `DamageDealEffect.java`'s own identical shape (a single `instanceof`-checked loop over
`getTargetEntities`, not two separate `getTargetCards`/`getTargetPlayers` passes, those existing only for
`getStackDescription`). `dealDamageTargets` (below `Resolve`) skips a card target that already left the battlefield
between targeting and resolution -- `DamageDealEffect.java`'s own per-target liveness check, which Java runs inside the
effect on top of the shared CR 608.2b check -- while every other target in the same resolution still takes its damage.
No such check exists for a player target in Java's own loop, so none was added here.

`DividedAsYouChoose$` (Forked Bolt's own uneven-split shape, 74 real lines) is explicitly not ported: Java records the
allocation at target-choosing time (`SpellAbility.addDividedAllocation`), a decision this port's own
`resolveTargets`/`ChooseTargets` has nowhere to carry, so a line naming it fails loudly rather than dealing the
ability's own full `NumDmg$` to every target chosen.

`fixture/actions.go`'s own `queue targets` verb gained player-name support (`resolveTargetEntities`,
`resolveAttackTarget`'s own comma-separated-list sibling) -- a real target list can now name a seated player, not just a
card's `Loaded.CardByFixtureID`. `deal-damage-any-target-lightning-bolt-at-a-planeswalker` is the fixture: a real
Lightning Bolt cast at an opponent's Narset, Parter of Veils, `Counters:LOYALTY` dropping from 5 to 2. Module tests
(`dealdamagetargets_test.go`) cover a creature, a planeswalker (loyalty, and dying at zero to the state-based action), a
Battle, a player, the identical path through `ActivateAbility`, the already-left-the-battlefield skip, and
`DividedAsYouChoose$` failing loudly.

Closes the M5 "Not ported yet" row for non-combat damage to a planeswalker or a Battle
(`docs/crucible/porting/port-log/game-state.md`).
