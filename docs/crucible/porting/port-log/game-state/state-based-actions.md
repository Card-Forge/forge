# Port Log — Game State: State-Based Actions

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)

SBAs, loyalty, lethal damage, legend and world rules, `Move`.

## State-based actions

`CheckStateBasedActions` is `GameAction.checkGameOverCondition`, `Player.checkLoseCondition`, `stateBasedAction704_5q`,
`handlePlaneswalkerRule`, `stateBasedAction_Battle`, `handleLegendRule` and `handleWorldRule`, plus
`destroyLethalToughness`, `destroyDamagedCreatures` and `cleanupDanglingAttachments` for a slice of what `changeZone`
folds in elsewhere in Java ([`## Move carries what Java gets for free`](#move-carries-what-java-gets-for-free), below) —
the rules answerable without the full layer system: CR 704.5a (a player at zero or less life loses), CR 704.5c (ten or
more poison counters loses), CR 704.5q (a permanent carrying both +1/+1 and -1/-1 counters loses the smaller pile from
each, in equal number — five +1/+1 and two -1/-1 leaves three +1/+1 and none — `stateBasedAction704_5q`'s own name is
the source for this letter), CR 704.5f (a creature at zero or less toughness dies, Layer 7 and counters folded in —
`GameAction.java`'s own comment on this check, not 704.5g), CR 704.5g and 704.5h together (a creature dealt lethal
damage, or any deathtouch damage at all, dies — indestructible creatures excepted,
[`## Lethal and deathtouch damage`](#lethal-and-deathtouch-damage-and-the-one-keyword-this-port-checks), below), a
partial CR 704.5v (a Battle at zero or less defense dies, its own trigger-on-the-stack exception checked and always
false today — [`## Loyalty is not a layer`](#loyalty-is-not-a-layer)'s Battle paragraph, below), CR 704.5w/704.5x (a
Battle's protector — `assignBattleProtector`,
[`## Combat`](turn-stack-combat.md#combat-declaring-attackers-declaring-blockers-and-dealing-damage)'s own paragraph on
it, below), CR 704.5m (more than one permanent with the World supertype on the battlefield at once, across every player,
destroys every one but the newest by `Card.Timestamp` — `resolveWorldRule`, the same field
[`## Handles, not pointers`](../game-state.md#handles-not-pointers)'s own table above already stamps on every zone
change), and three rules Java's own comments do not number: a planeswalker at zero or less loyalty dies
(`handlePlaneswalkerRule`), the legend rule (`handleLegendRule` —
[`## The legend rule needed CheckStateBasedActions to take a controller`](#the-legend-rule-needed-checkstatebasedactions-to-take-a-controller),
below), and a "cleanup aura" rule (Java's own comment for it, `GameAction.java:1511` — an Aura not attached to a
permanent on the battlefield, or attached to one that no longer matches the Aura's own `Enchant` restriction (CR 303.4a,
`enchantSpec`, below), goes to its owner's graveyard; an Equipment or Fortification in the same state just becomes
unattached alongside it). Citing these against Java's own comments rather than the rulebook from memory is deliberate:
`GameAction.java` labels the toughness check 704.5f, not 704.5g, and disagrees with itself about the attachment rule
(one comment calls it 704.5q, the same letter `stateBasedAction704_5q`'s own name already claims for counter
annihilation) — a wrong citation is worse than none, so the attachment, loyalty and legend rules are not asserted a
specific sub-letter here. Every other SBA in Java's loop — lethal damage to a planeswalker or a Battle via its
loyalty/defense rather than a creature's toughness, the rest of 704.5f/704.5g's own toughness (`*` with no
characteristic-defining effect to replace it, or a `Count$` reference on the printed Toughness field itself —
`resolveAmount`, amount.go, is wired into `Mode$ Continuous`'s own PT params, not this text field yet), and the legend
rule's own Corner Case 1
([`## The legend rule needed CheckStateBasedActions to take a controller`](#the-legend-rule-needed-checkstatebasedactions-to-take-a-controller),
below, has the full account) — needs a card-name lookup across every creature card this game has ever printed, not just
what is on this battlefield, which this port's `*Game` holds no reference for. Damage dealt to a planeswalker or a
Battle, which CR 120.3c/121.5 removes as loyalty/defense counters rather than marking `Damage`, is wired too
(`dealPermanentDamage`,
[`## Combat`](turn-stack-combat.md#combat-declaring-attackers-declaring-blockers-and-dealing-damage)) — combat can
attack one directly, so `destroyZeroLoyalty`/`destroyZeroDefense` are exercised by real play as well as by tests that
remove counters directly. Only _non-combat_ damage to a planeswalker or Battle is still a gap: nothing that deals damage
outside combat exists yet (no `SpellAbility`, no activated ability), so a burn spell or an ability aimed at a
planeswalker's loyalty has nowhere to come from regardless of whether the target-side plumbing is ready. A rule this
port has not implemented simply never fires, the same as a real game with no permanent that rule ever applies to — it is
a coverage gap (ADR-0011), not a wrong answer.

CR 704.5q's own guard — some cards grant "counters can't be removed from CARDNAME" — is a static ability, so it is not
checked either: nothing this port can grant that effect yet, so its absence changes no card's behaviour today.

Java's own loop runs up to nine times, because one SBA firing can make another one true. `destroyLethalToughness`,
`destroyDamagedCreatures`, `destroyZeroLoyalty`, `assignBattleProtector`, `destroyZeroDefense` and `resolveLegendRule`
all run before `cleanupDanglingAttachments`, not after, for exactly that reason: a creature, planeswalker, Battle or
legendary permanent this pass destroys can leave an Aura dangling that the very same `CheckStateBasedActions` call has
to catch, the one real cascade among the ten rules here. `assignBattleProtector` itself sits before `destroyZeroDefense`
for a narrower version of the same reason — matching `stateBasedAction_Battle`'s own combined-function order, a Battle
destroyed for having no eligible protector
([`## Combat`](turn-stack-combat.md#combat-declaring-attackers-declaring-blockers-and-dealing-damage)'s own paragraph
has the reachability of that case) never reaches the defense check at all, rather than because assigning a protector
changes any card's Defense. Nothing else cascades a second time — destroying a permanent cannot itself change another
one's printed toughness, damage total, loyalty/defense count or name, and nothing yet grants an effect that could — so
one ordered pass is complete. A game that already ended skips every check below entirely, the same as Java:
`checkStateEffects` returns before its creature loop runs once `checkGameOverCondition` finds the game over. The loop
returns once a rule that can cascade twice lands — a card script writing to `Player.Life` or a permanent's counters
mid-check does not exist yet either.

`cleanupDanglingAttachments` needed `Card.Type()` to exist at all: `compile.Card` carried no printed characteristics
before this, only compiled ability lines, because nothing before this needed to go from a compiled card back to "what
type is it" (`## Compiled cards needed a name back` in `game-state-fixture.md` is the same shape of gap, for `Name`
instead of `Type`). `carddb.Face` already parses one (`Type cardtype.Line`, from the corpus's own `Type:` line);
`compile.Face` now carries it through unchanged, since it is a printed value `Compile` does not interpret, the same way
`Name` is copied rather than recomputed. `Card.Type()` returns the primary face's line and, for a `nil` `Def` (every
synthetic test card in this package), the zero `Line` — which matches no subtype, so a test card is never mistaken for
an Aura.

**`cleanupDanglingAttachments` now checks an Aura's own `Enchant` restriction, not just its host's presence.**
`enchantSpec` (`action.go`) reads the Aura's `K:Enchant:...` keyword (`internal/keyword`'s `Type`-kind parsing) and
turns the valid-string half of it into an `internal/valid.Spec` — `KeywordWithType.java`'s own
`"<validString>:<display text>"` split, reproduced with `strings.Cut` on the first remaining `:` in
`keyword.Keyword.Details` since Go's own `keyword.Parse` only cuts the head off once. `Matches` (`valid.go`) then checks
the host against that spec from the Aura's own controller and the Aura itself — exactly the call `Matches`'s own doc
comment already named as this rule's eventual caller, with no `Ability` in sight, before this landed. `"Player"` and
`"Opponent"` (`K:Enchant:Player`, `K:Enchant:Opponent` — Tenuous Truce, Archenemy, Overencumbered, Psychic Possession)
are Java's own literal forms for an Aura that enchants a player rather than a permanent; `enchantSpec` reports no
checkable spec for either rather than reading the bare word as a card-type restriction no permanent's type line could
ever contain, which would silently destroy every such Aura on the very next `CheckStateBasedActions` call. This port's
`AttachedTo` (`CardID`-only) has no representation for "attached to a player" at all, so a player-target Aura is a gap
this specific check does not close — a distinct, larger one from the type-restriction check it does close.

**Two different Java checks share this rule's "does the specific host refuse THIS specific Aura" question, a
card-property question `enchantSpec`'s own type-restriction check (above) is not, and this port keeps them in two
functions rather than one.** `hostRefusesAttach` (staticability.go) is
`GameEntity.cantBeAttachedMsg -> StaticAbilityCantAttach.cantAttach` (`GameEntity.java:270`) -- Protection's own
`CantAttach` half only, checked from `cleanupDanglingAttachments` (every ongoing SBA pass, CR 704.5m) in addition to,
not instead of, its own `Matches` call. `cardCantBeTargetedBy` (staticability.go,
[`## Hexproof, Shroud and Protection refuse a target`](targeting-and-chaining.md#hexproof-shroud-and-protection-refuse-a-target))
is `Card.canBeTargetedBy -> StaticAbilityCantTarget.cantTarget` -- Hexproof, Shroud and Protection's targeting half,
checked from `enchantTargets` (cast time, CR 601.2c) and `auraTargetStillLegal` (the CR 608.2b re-check) instead. Java's
own `cantBeEnchantedByMsg` (`GameEntity.java:292-304`) never re-checks Hexproof or Shroud for an already -attached Aura,
only the `Enchant` restriction itself -- a host gaining Hexproof or Shroud after the Aura attached does not make it fall
off, since the Aura was a legal target when it targeted the host and CR 704.5m never re-runs that check.
`hostRefusesAttach` checking only Protection is why: putting Hexproof or Shroud there, as one earlier version of this
port's merged `hostRefusesEnchant` did, would fall an already-attached Aura off the instant its host gained either -- a
bug this split fixed rather than reproduced (PORT-8 does not apply; no card script depended on the old behavior).

Protection is ported from `CardFactoryUtil.java`'s own Protection branch, which synthesizes a
`Mode$ CantAttach | Target$ Card.Self | ValidCard$ <valid>` line alongside `CantBlockBy`'s own `ValidBlocker$ <valid>`
-- the identical `valid` string `protectionEach`
([`## Block legality: CantBlockBy`](turn-stack-combat.md#block-legality-cantblockby)) already extracts for blocking,
matched against the Aura itself here rather than a candidate blocker, once per recognized Protection line (CR 702.16b: a
card with two or more applies each independently). Hexproof is ported from `CardFactoryUtil.java`'s own Hexproof branch
(`Mode$ CantTarget | ValidTarget$ Card.Self | Activator$ Opponent`, plus a `ValidSource$`/`ValidSA$` when the keyword
names a type): bare `K:Hexproof` (80 of 110 real lines) refuses any opponent's Aura unconditionally,
`Activator$ Opponent` collapsing to the identical `aura.Controller != h.Controller` check every other no-team-simplified
`Opponent` in this port already makes. A qualified form (`K:Hexproof:Black`, `K:Hexproof:Enchantment`, ..., 30 of 110
real lines) additionally requires the Aura itself to match a `ValidSource$` string -- `hexproofValidSource` builds that
string the same way `KeywordWithType.parse` (`Hexproof.java`'s own superclass) does: a bare color word (`Black`, 7 real
lines) gets `Card.` prepended before it reaches `Matches` (a bare color name is not itself a recognized `baseMatches`
case, valid.go), while a bare type word (`Enchantment`, 11 real lines) and an already-qualified `Card.<Property>` value
(`Card.MonoColor`, 5 real lines) both pass straight through unchanged -- `baseMatches`'s own ordinary type-check
fallthrough handles the first, `propertyMatches`'s own color/type dispatch the second. The ability-source shape
(`Triggered`/`Activated`, 2 real lines, "Hexproof from triggered/activated abilities") is refused rather than resolved:
Java's own branch would synthesize `ValidSA$` for these (`getTypeDescription().contains("abilities")`), and `Matches`
never evaluates a `SpellAbility` -- observable outside an Aura's own cast now that `cardCantBeTargetedBy` is called from
ordinary targeting too, logged in `game-state.md`'s Not ported yet.

---

## Loyalty is not a layer

`Card.BaseLoyalty` mirrors `BasePower`/`BaseToughness` — `compile.Face.Loyalty` carried through from `carddb.Face`'s
`InitialLoyalty`, resolved to an `int` only when it is a plain printed integer — but a planeswalker's loyalty does not
have a `Card.Loyalty()` counterpart the way power and toughness have `Power()`/`Toughness()`, because CR 121.5 does not
put loyalty through Layer 7 at all: a planeswalker's loyalty _is_ its `Loyalty` counter count (`counters.go`) from the
moment it enters the battlefield, full stop. `BaseLoyalty` only ever answers "how many counters would it enter with" —
`destroyZeroLoyalty` (CR 704.5, [`## State-based actions`](#state-based-actions) — Java's own comments do not number
this one) reads `Card.Counters.Count(Loyalty)` directly, not a computed accessor that would just be that same call one
level removed.

`Move` grants a planeswalker its printed starting loyalty as counters the moment it enters the battlefield (CR 121.5) —
the same "Java gets this for free by building a new Card object" gap `SummonSick` already closed for combat, closed here
for loyalty. `NewCard` still does not: fixture loading seats a battlefield permanent with exactly the counters the
fixture names, on purpose (`Move`'s own doc comment), so a scenario or a unit test isolating a state-based action still
sets `Loyalty` counters directly (`humancounters=LOYALTY=5`, or `Counters.Add` in a Go test) rather than relying on an
ETB grant that only fires for a card genuinely transitioning zones. `destroyZeroLoyalty` reads
`Card.Counters.Count(Loyalty)` either way, real and correct against whatever count is there, however it got there — the
same as `destroyLethalToughness` was real before `Power`/`Toughness` folded in Layer 7.

**A Battle's defense is the same shape, granted the same way.** `Card.BaseDefense`/`compile.Face.Defense`/
`carddb.Face.Defense` mirror `BaseLoyalty` exactly, `Move` grants it on entry the same way, and `destroyZeroDefense` (CR
704.5v, `GameAction.java`'s own comment) reads `Card.Counters.Count(Defense)` directly, the same story. One extra piece
of 704.5v is here too: Java's own version does not destroy a Battle at zero defense if it is the source of a trigger
that has fired but not yet left the stack, `hasSourceOnStack` in `GameAction.java`. That exception is checked, not
skipped — `destroyZeroDefense`'s own doc comment explains why it always reads false today (nothing puts a trigger on the
stack yet) rather than being silently dropped. CR 704.5w/704.5x, a Battle's protector assignment, is a separate
state-based action now too (`assignBattleProtector`,
[`## Combat`](turn-stack-combat.md#combat-declaring-attackers-declaring-blockers-and-dealing-damage)'s own paragraph on
it, below) — never a prerequisite for 704.5v's own defense check to be correct on its own terms, which is why the two
landed in different sessions without either one blocking on the other.

---

## Lethal and deathtouch damage, and the one keyword this port checks

`destroyDamagedCreatures` is CR 704.5g and 704.5h, Java's own comments on a single `else if` in `GameAction.java`'s
loop: a creature dealt damage at least equal to its current `Toughness()` dies, and a creature dealt any amount of
deathtouch damage dies regardless of the amount — `Card.Damage`'s own `Marked`/`Deathtouch` fields already existed for
exactly this ([`## The card's mutable parts`](../game-state.md#the-cards-mutable-parts)) but had no reader until now.

Java's own check has an earlier branch first: indestructible creatures skip both halves. This is the one keyword this
port reads anywhere — not because keywords in general are in scope, but because getting this one wrong would not be a
coverage gap, it would be an actively wrong answer: a creature this port destroys that a real game would not.
`Card.HasKeyword` (card.go) is the accessor, `compile.Face.Keywords` (carried through from `carddb.Face.Keywords`
unchanged, the same "carry the printed text" split `Type`/`Power`/`Toughness`/`Loyalty` already use) is what it reads,
and `internal/keyword`'s own `Parse(line).Name` does the actual matching — the head as written, so `"Ward:2"` is still
found by `"Ward"`. Expanding a keyword into what it actually grants (the triggers, statics and abilities behind it) is a
different, much larger job `keyword.go`'s own doc comment already says this package does not do; `HasKeyword` only ever
answers "is the bare word present."

`cleanupStep` (`turn.go`) is a partial CR 514.2, landing alongside this because a damage-based SBA that never clears the
damage it is checking would be testing a state that cannot occur in a real game past one turn. Clearing damage on every
permanent in the game (not just the active player's, unlike `untapStep`) is the only piece built here; discarding to
hand size (CR 514.1, `Player.HandSizeLimit`) and ending "until end of turn" effects (514.2's other half, `Game.pumps`)
land in later chunks
([`## M6's fifth effect: Pump, and duration tracking`](effects-m6-first.md#m6s-fifth-effect-pump-and-duration-tracking),
has the second).

---

## The legend rule needed `CheckStateBasedActions` to take a controller

`resolveLegendRule` is `handleLegendRule`: a player controlling two or more legendary permanents sharing a name keeps
one and puts the rest into their owners' graveyards. It is the first state-based action this port has that asks a player
a question rather than just reading game state, and that changed a signature every other SBA in this file already used
without one — `CheckStateBasedActions(g *Game)` became `CheckStateBasedActions(g *Game, controller PlayerController)`,
which rippled to every path that reaches it: `Game.StartTurn`, `Game.AdvancePhase`, `Game.beginPhase` (turn.go) and
`Game.ResolveStack` (stack.go) all gained the same parameter, and every call site — production and test alike — had to
pass one through. `ChooseLegendaryToKeep` (control.go) is the fifth `PlayerController` method, alongside a
`ScriptedController.QueueLegendaryToKeep` and a `queue legendarykeep <id>` `actions.log` verb
(`internal/fixture/actions.go`) for scenarios that need to script it.

This is the real fix, not a workaround: CR 704.3 says state-based actions are checked automatically, so a state-based
action that needs a decision has to have somewhere to get one from, and bolting a separate
`ResolveLegendRule(g, controller)` on as something a caller has to remember to invoke would not actually be "automatic"
the way the rule requires. Threading the controller through instead keeps every state-based action, decision-needing or
not, reachable the same way.

Grouping is per player, not across the whole battlefield: two different players may each legally control their own copy
of one legendary permanent, so only a player's own duplicates trigger the rule. Within a player, names are grouped in
the order their permanents first appear on the battlefield (GO-12) — the same determinism `Multimaps.index`'s
insertion-ordered keys give Java.

A legendary permanent exempted by its own `Mode$ IgnoreLegendRule` static ability (`ignoreLegendRule`,
`staticability.go`) is filtered out before grouping even starts, the same as Java's own `handleLegendRule` filters its
candidate list first (`GameAction.java`). Ported from
`StaticAbilityIgnoreLegendRule.ignoreLegendRule`/`applyIgnoreLegendRuleAbility`: every battlefield permanent is walked
as a possible host (Battlefield only, the same trim `cantBlockBy`'s own doc comment justifies), and a `ValidCard`-less
line (1 of the 11 real corpus lines, an unconditional "the legend rule doesn't apply") matches every card, exactly
Java's own `matchesValidParam` contract for an absent param. Two of the 11 carry `IsPresent$`/`PresentCompare$` (a "you
control exactly two of them" condition `StaticAbility.java`'s own generic `checkConditions` evaluates, no equivalent for
any static-ability mode in this port yet) — skipped rather than guessed at, the same safe default an unresolvable
`Toughness` leaves a creature alive under.

Java's own `handleLegendRule` carries two more corner cases past the ordinary same-name grouping above, its own names
for them (`GameAction.java`'s own comments, "Corner Case 1"/"Corner Case 2"). Corner Case 2 is built now; Corner Case 1
is not.

**Corner Case 2**: a permanent whose own `Card.hasNonLegendaryCreatureNames()` is true — Spy Kit's own real "has all
names of nonlegendary creature cards in addition to its name" — answers to every non-legendary creature's own printed
name, including every OTHER such permanent's, so two or more of them clash with each other even when their own printed
names differ. `Card.HasNonLegendaryCreatureNames` (card.go) is the port's own version of that flag — a plain bool, not
folded by timestamp the way `TypeMod`/`ColorMod`/`KeywordMod` are, since nothing else ever needs to know more than one
source's own contribution at once (two Spy Kits on the identical creature answer the identical "yes" a single one
would). `applyContinuousNames` (continuous.go) is the new Layer 3 continuous applier that sets it, recomputed fresh
every `CheckStateBasedActions` pass the same as every other layer's own applier — resolving the one real value
`AddNames$` takes corpus-wide, `AllNonLegendaryCreatureNames` (1 real line, Spy Kit's own).

Spy Kit's own real shape needed one thing no other applier's own `Affected$` dispatch had needed yet:
`AffectedDefined$ Equipped` — "the creature this Equipment currently equips," not a blanket battlefield-wide `Affected$`
match. Every other layer's own applier (`applyOneContinuousPT`/`Type`/`Color`/`Keyword`) refuses outright the instant
`AffectedDefined$` is present (0 real lines pair it with any of their own keys, their own doc comments' skip-lists), a
convention this applier could not reuse without making itself dead code against the one real corpus line it exists for.
`Card.AttachedTo()` (card.go) already answers the question directly — "what this card [the Equipment] is attached to" is
exactly Java's own `AbilityUtils.getDefinedCards(hostCard, "Equipped", ...)`, since this port already models Equipment
attachment the identical way an Aura's is (`Game.Attach`/`Unattach`,
[`## Handles, not pointers`](../game-state.md#handles-not-pointers)) — `Affected$`'s own valid-string still filters that
single card afterward, the identical two-step Java's own `getAffectedCards` does (`AffectedDefined$` first, `Affected$`
second, `StaticAbilityContinuous.java`).

`resolveLegendRule` (action.go) reads the flag after the ordinary name-grouping above has already run: every legendary
permanent carrying it, that the name-grouping has not already sent to its own owner's graveyard, is gathered into one
more group and resolved the identical way a same-name duplicate is (`ChooseLegendaryToKeep`, reused) — skipping anything
the name-grouping already removed is what keeps two same-named, both-flagged legendaries from being asked about twice.

**Corner Case 1** stays unbuilt: whether a Corner-Case-2 permanent's own borrowed names collide with some OTHER
legendary's own literal printed name (`StaticData.instance().getCommonCards().isNonLegendaryCreatureName`,
`GameAction.java`) needs a lookup across every creature card this game has ever printed, not just what is on this
battlefield today — a card-name-to-type index over the WHOLE corpus, not the handful of cards any one game ever touches.
This port's `*Game` holds no `*carddb.DB` reference to ask (GO-2's own guidance to inject one has never actually been
done for the engine package), and adding one now, only for this, would mean threading it through every `*Game`
constructor across the entire test suite — a disproportionately large refactor for the one corpus card (Spy Kit) it
would unlock, the identical "narrow correct slice over a large risky one" call the Registry-avoidance chunks
([`## Draw's own ReplaceWith$`](replacement.md#draws-own-replacewith-cr-616s-the-event-is-replaced)) already made for a
different wall (PORT-8).

---

## The World rule needed no new field, only the one every zone change already stamps

`resolveWorldRule` is `handleWorldRule` (CR 704.5m): at most one permanent with the World supertype may be on the
battlefield at once, across every player at once, not grouped per player the way the legend rule is above — the newest
one survives, and every other one goes to its owner's graveyard. It asks nobody anything, unlike the legend rule right
above it: Java's own version picks the newest by `getWorldTimestamp()`, a plain comparison, not a choice, so
`resolveWorldRule` takes no `PlayerController` at all.

The comparison it needs already existed: `Card.Timestamp`
([`## Handles, not pointers`](../game-state.md#handles-not-pointers)'s own table, above) is stamped on every zone change
for CR 613's own layer ordering, and a World permanent enters the battlefield through the same `Move`/`put` every other
permanent does, so there was no `getWorldTimestamp()`-equivalent field to add — the general `Timestamp` already answers
"which one is newest" without knowing anything about World in particular.

A tie for the newest timestamp destroys every tied permanent too, not just the older ones — Java's own
`toKeep.size() == 1` guard only spares the survivor when there is exactly one. `g.timestamp` increments on every single
`put`, so no two cards placed through the public API (`NewCard`, `Move`) ever actually share one; the tie branch is
reachable only by a test that sets `Card.Timestamp` directly, the same "kept for when it becomes reachable" position
`destroyZeroDefense`'s own stack-trigger exception is already in
([`## Loyalty is not a layer`](#loyalty-is-not-a-layer), below).

---

## Move carries what Java gets for free

`GameAction.changeZone` (2,897 LOC, most of it replacement effects and triggers this port has not reached) is not
ported. Two pieces of it are: the part that exists only because Go's cards do not work the way Java's do, and — since
"## Last-known-information lands," below — the one slice of `CardCopyService.getLKICopy()`'s own last-known-information
bookkeeping the real corpus's own dies triggers actually read.

Java rebuilds a `Card` as a new object on every zone change (`CardCopyService.copyCard`), so a field the new object does
not carry — tapped, damage, counters, summoning sickness — is simply gone, free of charge. ADR-0009 chose the opposite:
a `CardID` is stable for the card's whole life in the game, so the same struct that was tapped on the battlefield is
still tapped after `Move` if nothing clears it. `Move` now does that clearing explicitly: leaving the battlefield
freezes an LKI copy of the card (below) before clearing `Counters`, `Damage`, `PT`, `TypeMod`, `ColorMod`, `KeywordMod`,
`Tapped` and the card's own attachment; entering it sets `SummonSick`, since a freshly-arrived permanent has not been
under its controller's control since their last turn began (CR 302.6).

What it deliberately does not do: unattach whatever was attached _to_ the leaving card (an Equipment left behind when
its creature dies keeps pointing at a `CardID` no longer on the battlefield). That is CR 704.5's "cleanup aura" rule's
job, not `Move`'s — it is an SBA, checked continuously, not something a zone change fires inline — and
[`## State-based actions`](#state-based-actions) above is where it landed (`cleanupDanglingAttachments`). `Game.NewCard`
stays untouched by any of this: it is the arena-allocation primitive fixture loading uses to seat a board mid-game,
where a battlefield card's starting `Tapped`/`SummonSick` is exactly what the fixture says, not a rule this port applies
at construction time.
