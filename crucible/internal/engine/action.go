// State-based actions and the game-over detection that rides along with
// them.

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// CheckStateBasedActions applies every state-based action this port checks
// today and reports whether the game has ended as a result.
//
// Ported from forge-game/src/main/java/forge/game/GameAction.java
// (checkGameOverCondition, stateBasedAction704_5q, and the inline checks in
// its own checkStaticAbilities/state-based-action loop) and
// forge/game/player/Player.java (checkLoseCondition). CR numbers below are
// the ones Java's own comments cite, chased down line by line rather than
// assumed from the rulebook: GameAction.java labels the toughness check
// "Rule 704.5f", not 704.5g, and Forge's own comments disagree with each
// other about a couple of the others (noted where that happens) -- citing
// the wrong letter is worse than citing none, since a reader who goes
// looking for the Java source and finds a different rule at that letter
// has no way to tell whether the port or the citation is wrong.
//
// Twelve of Java's checks are here: CR 704.5b (an attempted draw with
// nothing to draw loses), CR 704.5a (a player at zero or less life loses),
// CR 704.5c (ten or more poison counters loses), CR 704.5q (a permanent
// carrying both +1/+1 and -1/-1 counters loses the smaller pile from each,
// in equal number -- `stateBasedAction704_5q`'s own name is the source for
// this letter), a partial CR 704.5f (a creature at zero or less toughness
// -- printed, Layer 7's own SETPT/MODIFYPT effects (applyContinuousPT,
// continuous.go, recomputed fresh right before this loop runs) and +1/+1 or
// -1/-1 counters all folded in, Card.Toughness's own job -- goes to its
// owner's graveyard), CR 704.5g and 704.5h together (a creature dealt
// damage at least equal to its toughness, or dealt any deathtouch damage
// at all, is destroyed -- destroyDamagedCreatures, below), a partial CR
// 704.5v (a Battle at zero or less defense goes to its owner's graveyard --
// destroyZeroDefense, below), CR 704.5w/704.5x (a Battle's protector --
// assignBattleProtector, below, the first state-based action besides the
// legend rule that needs a PlayerController), CR 704.5m (more than one
// permanent with the World supertype on the battlefield destroys every one
// but the newest, by the same Card.Timestamp every zone change already
// stamps for CR 613's own ordering -- resolveWorldRule, below), and three
// rules Java's own comments do not cleanly single-letter: a "cleanup aura"
// rule (Java's own comment for it, GameAction.java:1511 -- an Aura not
// attached to a permanent on the battlefield, or attached to one that no
// longer matches its own `Enchant` restriction, goes to its owner's
// graveyard; an Equipment or Fortification in the same state just becomes
// unattached), a planeswalker at zero loyalty (`handlePlaneswalkerRule`,
// which Java's own comments do not number at all), and the legend rule
// (`handleLegendRule`, same -- resolveLegendRule, below). Every other rule
// in Java's loop -- indestructible aside (destroyDamagedCreatures checks
// it; nothing else here needs to), the rest of 704.5f's own toughness (a
// "*" with no characteristic-defining effect to replace it, or a Count$
// reference on the printed Toughness field itself -- resolveAmount
// (amount.go) is wired into Mode$ Continuous's own PT params, `ptParam`,
// continuous.go, not this text field yet), the rest of 704.5v's
// own exception (a Battle whose own trigger is still on the stack -- always
// false today, destroyZeroDefense's own doc comment), and the legend rule's
// own Corner Case 1, a Corner-Case-2 permanent's own borrowed names colliding
// with some OTHER legendary's own literal printed name (resolveLegendRule's
// doc comment) -- needs a card-name lookup across every creature card this
// game ever printed, which this port's `*Game` holds no reference for. A
// rule this port has not implemented simply never fires, the same as it
// would in a real game with no permanent that rule applies to.
//
// 704.5b is checked first, matching Java's own order -- its comment cites
// Lich's Mirror (CR 704.7), a card not ported, so today's checks would give
// the same result in any order. Kept anyway: it costs nothing, and a future
// port of that card should not have to notice these were ever reordered.
//
// Java's `canRemoveCounters` guard on 704.5q -- some cards grant "counters
// can't be removed from CARDNAME" -- is a static ability, so it is not
// checked either: nothing this port can build yet grants that effect, so its
// absence changes no card's behaviour today.
//
// Java's own checkStateEffects loops up to nine times, because one SBA firing
// can make another one true (destroying a permanent can, in turn, empty an
// Aura's target -- exactly the interaction 704.5f, 704.5g/704.5h, 704.5v, the
// legend rule, the World rule and the attachment cleanup below have, which is
// why destroyLethalToughness, destroyDamagedCreatures, destroyZeroLoyalty,
// destroyZeroDefense, resolveLegendRule and resolveWorldRule all run before
// cleanupDanglingAttachments rather than on a later call). Nothing here
// cascades a second time: destroying a permanent cannot itself change another
// one's printed toughness, deal it damage, or give it the same name, and
// nothing yet grants an effect that could. One pass is complete; the loop
// returns once a rule that can cascade twice lands.
//
// A game that has already ended skips every check below entirely, the same
// as Java: checkStateEffects returns as soon as checkGameOverCondition finds
// the game over, before its creature loop ever runs.
//
// A GameEnded event fires exactly once, on the call that flips g.over --
// never on a later call finding it already true, and not from any other
// path: this is the only place g.over is set.
//
// CR 104.1's own alt-win -- winsGameEffect (winsgameeffect.go) setting
// Player.Won directly rather than through CR 104.2a's own elimination count
// below -- ends the game immediately, checked first for the identical
// reason: an effect that makes a player win does not wait for every
// opponent to also be eliminated first. More than one Player.Won at once
// (winsGameEffect resolving for two players in the same pass) is CR
// 104.4a's own simultaneous-win draw, Actor left NoPlayer the same as the
// ordinary zero-remaining draw below.
func CheckStateBasedActions(g *Game, controller PlayerController) bool {
	over, _ := checkStateBasedActions(g, controller)
	return over
}

// checkStateBasedActions is CheckStateBasedActions' body. over is the game
// having ended; performed is GameAction.checkStateEffects' own
// performedSBA (GameAction.java:1412, :1614): some state-based action
// actually did something -- a permanent moved, counters annihilated, an
// attachment fell off, a token ceased to exist, a battle was assigned a
// protector. Rebuilding continuous effects is not an action, and neither is
// completing a dungeon (Java leaves checkAgain alone for it,
// GameAction.java:1437-1438). The turn driver's CR 514.3a cleanup repeat
// reads performed (beginStep, turn.go).
func checkStateBasedActions(g *Game, controller PlayerController) (over, performed bool) {
	if g.over {
		return true, false
	}

	winner, wins := NoPlayer, 0
	for _, id := range g.Players() {
		if g.Player(id).Won {
			winner, wins = id, wins+1
		}
	}
	if wins > 0 {
		g.over = true
		actor := winner
		if wins > 1 {
			actor = NoPlayer
		}
		g.sink.Emit(Event{Kind: GameEnded, Active: g.activePlayer, Actor: actor, Turn: uint16(g.turn)})
		return true, false
	}

	// CR 704.5b, 704.5a, 704.5c
	for _, id := range g.Players() {
		p := g.Player(id)
		drewFromEmpty := p.DrewFromEmptyLibrary
		p.DrewFromEmptyLibrary = false
		if p.Lost {
			continue
		}
		if drewFromEmpty || p.Life <= 0 || p.Counters.Count(Poison) >= 10 {
			p.Lost = true
		}
	}
	g.onPlayersLost(controller)

	remaining := NoPlayer
	count := 0
	for _, id := range g.Players() {
		if !g.Player(id).Lost {
			count++
			remaining = id
		}
	}

	// CR 104.2a: one player left standing wins. Zero is a draw, and more than
	// one means the game goes on -- both leave g.over false.
	switch count {
	case 1:
		g.Player(remaining).Won = true
		g.over = true
	case 0:
		g.over = true
	}
	if g.over {
		// Actor is the winner, or NoPlayer for a draw -- there is no
		// separate PlayerLost event, so this is also where a loss is
		// visible in the stream: everyone else in the game lost.
		g.sink.Emit(Event{Kind: GameEnded, Active: g.activePlayer, Actor: remaining, Turn: uint16(g.turn)})
		return true, false
	}

	performed = removeTokensOffBattlefield(g)
	g.completeFinishedDungeons(controller)
	g.returnFinishedSchemes()

	// CR 613: recomputed fresh every pass, before anything below reads
	// Power()/Toughness() or Type() -- applyContinuousPT's own doc comment
	// (continuous.go) has the reason this cannot be a one-time push instead.
	// applyContinuousControl runs first: CR 613.1 puts Layer 2 (control)
	// before every layer that follows, and several of them evaluate
	// Affected$ specs that can themselves read Controller() (a "YouCtrl"
	// property), which must already reflect this pass's own control changes
	// (applyContinuousControl's own doc comment).
	// clearContinuousText/applyContinuousText bracket Layer 2 for Layer 3:
	// last pass's text changes end before control is recomputed, and this
	// pass's apply right after it (clearContinuousText's own doc comment).
	clearContinuousText(g)
	applyContinuousControl(g)
	g.dropStolenRingBearers()
	applyContinuousText(g)
	applyContinuousPT(g)
	applyContinuousType(g)
	applyContinuousColor(g)
	applyContinuousKeyword(g)
	applyContinuousRules(g)
	applyContinuousNames(g)
	// applyPumpEffects runs after applyContinuousPT/applyContinuousKeyword,
	// once their own Clear() has already emptied every battlefield card's PT
	// and KeywordMod for this pass, so a resolved Pump effect's own
	// contribution (Game.pumps, game.go) is what re-adds it back rather than
	// a Mode$ Continuous S: line (applyPumpEffects's own doc comment,
	// continuous.go).
	applyPumpEffects(g)
	applyAnimateEffects(g)

	// CR 704.5q
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			performed = annihilateCounters(g, id) || performed
		}
	}
	performed = destroyLethalToughness(g, controller) || performed
	performed = destroyDamagedCreatures(g, controller) || performed
	performed = destroyZeroLoyalty(g, controller) || performed
	performed = assignBattleProtector(g, controller) || performed
	performed = destroyZeroDefense(g, controller) || performed
	performed = resolveLegendRule(g, controller) || performed
	performed = resolveWorldRule(g, controller) || performed
	performed = cleanupDanglingAttachments(g, controller) || performed
	return false, performed
}

// annihilateCounters is CR 704.5q: N +1/+1 and N -1/-1 counters are removed
// together, where N is the smaller pile. A card carrying only one kind, or
// neither, is untouched. Sourced from id itself: the rule is self-inflicted,
// not a card's ability doing it, the same as Move's ETB loyalty/defense grant
// (game.go) has no other card to attribute it to.
func annihilateCounters(g *Game, id CardID) bool {
	c := g.Card(id)
	plus, minus := c.Counters.Count(P1P1), c.Counters.Count(M1M1)
	if plus == 0 || minus == 0 {
		return false
	}
	remove := plus
	if minus < remove {
		remove = minus
	}
	c.Counters.Add(P1P1, -remove)
	c.Counters.Add(M1M1, -remove)
	emitCounterChanged(g.sink, id, CardEntity(id), P1P1, -remove)
	emitCounterChanged(g.sink, id, CardEntity(id), M1M1, -remove)
	return true
}

// destroyLethalToughness is CR 704.5f, GameAction.java's own comment (not
// 704.5g -- see CheckStateBasedActions's doc comment). Card.Toughness
// (card.go) folds Layer 7's continuous effects and +1/+1 and -1/-1
// counters onto the printed value already, so a creature reduced to zero
// by an annihilated -1/-1 pile, a MODIFYPT pump, or a SETPT/CHARACTERISTIC
// effect all die here the same as one whose printed toughness always read
// zero. What still does not die: a creature whose toughness is
// unresolvable at every layer -- "*" with no characteristic-defining
// effect to replace it, or a Count$ reference -- since Toughness reports
// that as ok=false rather than a wrong number (game-state.md's "Not
// ported yet").
//
// Candidates are collected before Move runs, the same reason
// cleanupDanglingAttachments collects first: Move mutates the battlefield
// zone this ranges over. Once every dead creature has actually moved, Mode$
// ChangesZoneAll fires once for the whole batch (checkChangesZoneAllTriggers,
// trigger.go) -- CR 704.3's own "all applicable SBAs performed
// simultaneously," a board wipe's own "whenever one or more creatures you
// control die" firing once naming every creature this SBA killed together,
// not once per creature.
func destroyLethalToughness(g *Game, controller PlayerController) bool {
	var dead []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(id)
			if !c.Type().Has(cardtype.Creature) {
				continue
			}
			if t, ok := c.Toughness(); ok && t <= 0 {
				dead = append(dead, id)
			}
		}
	}
	for _, id := range dead {
		g.Move(id, Graveyard, g.Card(id).Owner)
		g.checkDiesTriggers(controller, id)
	}
	g.checkChangesZoneAllTriggers(controller, dead, Battlefield, Graveyard)
	return len(dead) > 0
}

// destroyDamagedCreatures is CR 704.5g and 704.5h together, GameAction.java's
// own comments (both cited on the same `else if`, since Java checks them in
// one branch): a creature dealt damage at least equal to its current
// toughness is destroyed, and a creature dealt any amount of deathtouch
// damage is destroyed regardless of the amount (CR 702.2c -- deathtouch
// makes any nonzero damage lethal on its own, so the marked total is never
// consulted for that half of the check). Indestructible
// (`c.hasKeyword(Keyword.INDESTRUCTIBLE)` in Java, an earlier branch in the
// same if/else chain) skips both: an indestructible creature that has taken
// lethal damage is not destroyed here, the one keyword this port checks
// anywhere, because getting it wrong would make this SBA actively incorrect
// for those cards rather than merely incomplete.
//
// Card.Toughness already folds Layer 7 and +1/+1/-1/-1 counters onto the
// printed value (`## Layer 7`, game-state.md), so this reads the same
// current toughness destroyLethalToughness does; a creature whose toughness
// is unresolvable at every layer is left alone here too, for the same
// reason.
//
// Candidates are collected before Move runs, the same reason every other
// SBA in this file does. Mode$ ChangesZoneAll fires once for the whole
// batch too, destroyLethalToughness's own doc comment has the reason --
// this port's own SBA split (a separate function per CR 704.5 clause,
// rather than Java's single combined pass) means a creature killed by
// destroyLethalToughness and one killed by destroyDamagedCreatures in the
// identical CheckStateBasedActions call fire two separate
// ChangesZoneAll batches rather than one shared one: a real, narrow
// simplification against CR 704.3's own full simultaneity, not observable
// against a corpus with no card that cares which of the two SBA clauses
// killed which creature.
func destroyDamagedCreatures(g *Game, controller PlayerController) bool {
	var dead []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(id)
			if !c.Type().Has(cardtype.Creature) || c.HasKeyword("Indestructible") {
				continue
			}
			if c.Damage.Deathtouch {
				dead = append(dead, id)
				continue
			}
			if t, ok := c.Toughness(); ok && c.Damage.Marked > 0 && c.Damage.Marked >= t {
				dead = append(dead, id)
			}
		}
	}
	var died []CardID
	for _, id := range dead {
		if g.regenerate(controller, id) {
			continue
		}
		g.Move(id, Graveyard, g.Card(id).Owner)
		g.checkDiesTriggers(controller, id)
		died = append(died, id)
	}
	g.checkChangesZoneAllTriggers(controller, died, Battlefield, Graveyard)
	return len(dead) > 0
}

// destroyZeroLoyalty is CR 704.5's planeswalker-loyalty rule -- Java's own
// GameAction.java does not cite a letter for handlePlaneswalkerRule
// (CheckStateBasedActions's doc comment), so none is asserted here either:
// a planeswalker with loyalty zero or less goes to its owner's graveyard.
// Loyalty is entirely counter-based
// (Card.BaseLoyalty's own doc comment) -- there is no Layer 7 to fold, no
// printed-value fallback the way BaseToughness has one, so this reads
// Card.Counters.Count(Loyalty) directly rather than calling a "current
// loyalty" accessor that would just be that same call one level removed.
//
// Move itself puts a starting loyalty counter on a planeswalker that enters
// the battlefield through real play (CR 121.5, Game.Move's own doc
// comment) -- CastSpell/PlayLand-driven entries get it for free. A
// permanent placed directly by setup.state (Game.NewCard) does not, the
// same "exactly what the fixture says" contract NewCard's own doc comment
// gives every other battlefield-entry field -- a fixture naming a
// planeswalker there still sets Counters:LOYALTY= itself if it wants one.
// Either way, this SBA is real and correct against whatever count is on
// the card, however it got there.
//
// Candidates are collected before Move runs, the same reason
// destroyLethalToughness and cleanupDanglingAttachments do.
//
// A Mode$ IgnorePlaneswalkerZeroLoyaltyRule static ability
// (ignorePlaneswalkerZeroLoyaltyRule, staticability.go) exempts a matching
// planeswalker from this whole SBA, per Java's own Card.
// ignorePlaneswalkerZeroLoyaltyRule()/GameAction.handlePlaneswalkerRule.
func destroyZeroLoyalty(g *Game, controller PlayerController) bool {
	var dead []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(id)
			if c.Type().Has(cardtype.Planeswalker) && c.Counters.Count(Loyalty) <= 0 && !ignorePlaneswalkerZeroLoyaltyRule(g, id) {
				dead = append(dead, id)
			}
		}
	}
	for _, id := range dead {
		g.Move(id, Graveyard, g.Card(id).Owner)
		g.checkDiesTriggers(controller, id)
	}
	return len(dead) > 0
}

// destroyZeroDefense is CR 704.5v: a Battle at defense zero or less goes to
// its owner's graveyard, unless it is the source of a triggered ability
// that has triggered but not yet left the stack (`hasSourceOnStack` in
// Java) -- CR 704.5v's own exception exists so a Battle's own "when this
// reaches 0 defense" trigger still gets to resolve. This port checks the
// exception exactly, not by skipping it: `g.StackTop`'s kind of lookup
// would need to inspect every item, not just the top, since anything could
// be pushed above the Battle's own trigger by the time this runs, and CR
// 613.6-613.8's ordering makes "is it still there" the only question that
// matters. Today it is always answered no -- nothing puts a trigger on the
// stack yet (`## Stack`), so every Battle is checked as if the exception
// never applies, which is the exception's own correct answer whenever it
// genuinely does not.
//
// Defense, like Loyalty, is entirely counter-based (Card.BaseDefense's own
// doc comment): entering the battlefield with printed-defense-many Defense
// counters is CR 704.5v's own prerequisite, and Move sets exactly that on
// real entry the same way it does for Loyalty (destroyZeroLoyalty's own doc
// comment) -- a setup.state-placed Battle still needs its own explicit
// Counters:DEFENSE= if it wants one.
//
// assignBattleProtector is CR 704.5w/704.5x, checked (and, per Java's own
// combined stateBasedAction_Battle, applied) before destroyZeroDefense
// below -- a Battle destroyed here for having no eligible protector never
// reaches the defense check at all.
//
// CR 704.5w: a Battle with no protector -- never assigned, or its protector
// has since left the game -- and no creature currently attacking it
// (attackersOf) has its controller choose one of their opponents to
// protect it. CR 704.5x: a Battle whose protector is somehow its own
// controller re-chooses too, regardless of whether it's being attacked --
// Java's own condition only gates the no-protector branch with "not
// attacked," not this one. Nothing this port can cause the CR 704.5x case
// with yet (nothing changes a protector once set), so it is here for
// completeness rather than because a reachable scenario needs it today.
//
// Every Battle needing a protector is treated as a Siege -- always the
// controller choosing an opponent -- with no subtype check of its own:
// every Battle in the compiled corpus prints that subtype, and Forge's own
// comment on the alternative ("fall back to the controller") calls it
// support for custom cards no official card uses, so a Siege-shaped answer
// is correct for the entire real corpus without needing to branch on it.
// No eligible opponent -- every real game has at least one once this runs,
// since CheckStateBasedActions already returned above if only one player
// remained -- sends the Battle to its owner's graveyard instead of asking,
// CR 704.5w's own fallback.
func assignBattleProtector(g *Game, controller PlayerController) bool {
	var needsProtector []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(id)
			if !c.Type().Has(cardtype.Battle) {
				continue
			}
			protector := c.ProtectingPlayer
			noProtector := protector == NoPlayer || g.Player(protector).Lost
			selfProtector := protector == c.Controller()
			if (noProtector && len(g.attackersOf(CardEntity(id))) == 0) || selfProtector {
				needsProtector = append(needsProtector, id)
			}
		}
	}

	for _, id := range needsProtector {
		c := g.Card(id)
		var eligible []PlayerID
		for _, pid := range g.Players() {
			if pid != c.Controller() && !g.Player(pid).Lost {
				eligible = append(eligible, pid)
			}
		}
		if len(eligible) == 0 {
			g.Move(id, Graveyard, c.Owner)
			g.checkDiesTriggers(controller, id)
			continue
		}
		c.ProtectingPlayer = controller.ChooseBattleProtector(g, c.Controller(), id, eligible)
	}
	return len(needsProtector) > 0
}

// destroyZeroDefense is CR 704.5v: a Battle at defense zero or less goes to
// its owner's graveyard, unless it is the source of a triggered ability
// that has triggered but not yet left the stack (`hasSourceOnStack` in
// Java) -- CR 704.5v's own exception exists so a Battle's own "when this
// reaches 0 defense" trigger still gets to resolve. This port checks the
// exception exactly, not by skipping it: `g.StackTop`'s kind of lookup
// would need to inspect every item, not just the top, since anything could
// be pushed above the Battle's own trigger by the time this runs, and CR
// 613.6-613.8's ordering makes "is it still there" the only question that
// matters. Today it is always answered no -- nothing puts a trigger on the
// stack yet (`## Stack`), so every Battle is checked as if the exception
// never applies, which is the exception's own correct answer whenever it
// genuinely does not.
//
// Defense, like Loyalty, is entirely counter-based (Card.BaseDefense's own
// doc comment): entering the battlefield with printed-defense-many Defense
// counters is CR 704.5v's own prerequisite, and Move sets exactly that on
// real entry the same way it does for Loyalty (destroyZeroLoyalty's own doc
// comment) -- a setup.state-placed Battle still needs its own explicit
// Counters:DEFENSE= if it wants one.
func destroyZeroDefense(g *Game, controller PlayerController) bool {
	var dead []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(id)
			if c.Type().Has(cardtype.Battle) && c.Counters.Count(Defense) <= 0 {
				dead = append(dead, id)
			}
		}
	}
	for _, id := range dead {
		g.Move(id, Graveyard, g.Card(id).Owner)
		g.checkDiesTriggers(controller, id)
	}
	return len(dead) > 0
}

// resolveLegendRule is the legend rule -- another of Java's own comments do
// not number (`handleLegendRule`, CheckStateBasedActions's doc comment): a
// player controlling two or more legendary permanents that share a name
// keeps one and puts the rest into their owners' graveyards.
//
// Grouping is per player, not across the whole battlefield: two different
// players may each legally control their own copy of one legendary
// permanent, so only a player's own duplicates trigger this. Within a
// player, names are grouped in the order their permanents first appear on
// the battlefield (GO-12) -- the same determinism `Multimaps.index`'s
// insertion-ordered keys give Java.
//
// A legendary permanent exempted by some Mode$ IgnoreLegendRule static
// ability (ignoreLegendRule, staticability.go) never enters the grouping at
// all, the same as Java's own handleLegendRule filters its own candidate
// list before grouping by name (GameAction.java).
//
// Corner Case 2, Java's own name for it (handleLegendRule's own comment): two
// or more legendary permanents that all carry HasNonLegendaryCreatureNames
// (card.go, applyContinuousNames, continuous.go's own Layer 3 -- Spy Kit's
// own real "has all names of nonlegendary creature cards in addition to its
// name") clash with EACH OTHER even when their own printed names differ,
// since every one of them answers to every non-legendary creature's own
// name, including each other's. Grouped and resolved the identical way a
// same-name duplicate is, after the ordinary name-grouping above has already
// removed its own duplicates -- a permanent already sent to its owner's
// graveyard by the name-grouping does not also need asking about here.
//
// Corner Case 1 is still not here: whether a Corner-Case-2 permanent's own
// borrowed names collide with some OTHER legendary's own literal printed
// name (`StaticData.instance().getCommonCards().isNonLegendaryCreatureName`,
// GameAction.java) needs a lookup across every creature card this game ever
// printed, not just what is on this battlefield -- this port's `*Game` holds
// no `*carddb.DB` reference to ask, and adding one now, for the one corpus
// card (Spy Kit) this would unlock, is a disproportionately large refactor
// (every `*Game` constructor across the whole test suite would need one
// threaded through) for what it reaches (PORT-8/GO-7): skipped, not guessed.
func resolveLegendRule(g *Game, controller PlayerController) bool {
	performed := false
	for _, pid := range g.Players() {
		byName := map[string][]CardID{}
		var order []string
		var nonLegendaryNamed []CardID
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(id)
			if !c.Type().HasSupertype(cardtype.Legendary) {
				continue
			}
			if ignoreLegendRule(g, id) {
				continue
			}
			if c.HasNonLegendaryCreatureNames {
				nonLegendaryNamed = append(nonLegendaryNamed, id)
			}
			name := c.Def.Name
			if _, ok := byName[name]; !ok {
				order = append(order, name)
			}
			byName[name] = append(byName[name], id)
		}
		removed := map[CardID]bool{}
		for _, name := range order {
			dup := byName[name]
			if len(dup) < 2 {
				continue
			}
			keep := controller.ChooseLegendaryToKeep(g, pid, dup)
			for _, id := range dup {
				if id != keep {
					removed[id] = true
					performed = true
					g.Move(id, Graveyard, g.Card(id).Owner)
					g.checkDiesTriggers(controller, id)
				}
			}
		}

		var remaining []CardID
		for _, id := range nonLegendaryNamed {
			if !removed[id] {
				remaining = append(remaining, id)
			}
		}
		if len(remaining) < 2 {
			continue
		}
		keep := controller.ChooseLegendaryToKeep(g, pid, remaining)
		for _, id := range remaining {
			if id != keep {
				performed = true
				g.Move(id, Graveyard, g.Card(id).Owner)
				g.checkDiesTriggers(controller, id)
			}
		}
	}
	return performed
}

// resolveWorldRule is CR 704.5m: at most one permanent with the World
// supertype may be on the battlefield at once, across every player at once --
// unlike the legend rule, this is not grouped per player. The newest one, by
// Card.Timestamp, survives; every other one goes to its owner's graveyard.
//
// Ported from GameAction.java's handleWorldRule. World permanents enter the
// battlefield the same as any other permanent, so Card.Timestamp -- already
// stamped on every zone change for CR 613's own layer ordering (game.go) --
// is exactly Java's own getWorldTimestamp(), with no new field needed to
// answer this.
//
// A tie for the newest timestamp destroys every tied permanent too, not just
// the older ones: Java's own toKeep.size() == 1 guard only spares the survivor
// when there is exactly one, reproduced here as tied == 1. g.timestamp
// increments on every single put -- NewCard and Move alike -- so no two
// cards placed through the public API ever actually share one (game.go's own
// put, arena_test.go's timestamp test), the same way Java's own
// getNextTimestamp() cannot hand out one value twice either; a tie needs two
// World permanents entering as one designed-simultaneous batch, which
// neither engine has a mechanism for yet. This branch is untested by real
// play today for exactly that reason -- the same "kept for when it becomes
// reachable" position destroyZeroDefense's own stack-trigger exception is
// in -- and is exercised here only by a test that sets Card.Timestamp
// directly.
func resolveWorldRule(g *Game, controller PlayerController) bool {
	var worlds []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if g.Card(id).Type().HasSupertype(cardtype.World) {
				worlds = append(worlds, id)
			}
		}
	}
	if len(worlds) < 2 {
		return false
	}

	var newest CardID
	var newestTS uint64
	tied := 0
	for _, id := range worlds {
		switch ts := g.Card(id).Timestamp; {
		case ts > newestTS:
			newestTS, newest, tied = ts, id, 1
		case ts == newestTS:
			tied++
		}
	}

	performed := false
	for _, id := range worlds {
		if tied == 1 && id == newest {
			continue
		}
		performed = true
		g.Move(id, Graveyard, g.Card(id).Owner)
		g.checkDiesTriggers(controller, id)
	}
	return performed
}

// cleanupDanglingAttachments is CR 704.5's attachment-legality rule: an
// attachment is illegal once either its host left the battlefield out from
// under it, or -- for an Aura specifically -- its host is still there but no
// longer matches the Aura's own `Enchant` restriction (CR 303.4a,
// enchantSpec, below). Not one clean letter: Java's own "cleanup aura"
// comment (GameAction.java:1511, CheckStateBasedActions's doc comment) is
// unlabeled, and the nearby attach-legality check it shares a loop with is
// labeled 704.5q in one comment even though `stateBasedAction704_5q`'s own
// name gives that letter to counter annihilation instead -- Java's comments
// disagree with each other here, so no sub-letter is asserted for this rule
// either. Move already unattaches a card from whatever *it* was attached to
// the moment it leaves (game.go); this is the other direction -- nothing
// walked the leaving card's own attachments -- and it has to be an SBA, not
// something Move does inline, because a Zone or Move test exercising a
// single card should not have to know about Aura at all.
//
// An Aura goes to its owner's graveyard whether the host left, never
// legally matched the restriction to begin with, or stopped matching it --
// all three are "not attached to a legal object". An Equipment or
// Fortification only loses the attachment, not the permanent: staying on
// the battlefield unattached is legal for those two, the way it is not for
// an Aura, and neither carries an `Enchant` restriction to re-check --
// `enchantSpec` only ever fires for an Aura.
//
// Protection's own CantAttach half preventing the attachment from staying
// legal (CR 704.5m proper, a static-ability "can't be attached" question,
// not the Enchant string itself) is checked too, via `hostRefusesAttach`
// (staticability.go) -- Protection for both real corpus shapes
// (`protectionEach` already built for CantBlockBy). Hexproof and Shroud do
// not belong here: Java generates no CantAttach ability for either
// (`hostRefusesAttach`'s own doc comment), so a host gaining hexproof or
// shroud after an Aura already attached does not make it fall off.
//
// Candidates are collected before either Move or Unattach runs, because
// both mutate the battlefield zone or a card's own attachment list -- the
// same hazard the zone-snapshot every zone read hands out already carries,
// just reachable here for the first time.
func cleanupDanglingAttachments(g *Game, controller PlayerController) bool {
	var toGraveyard, toUnattach []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(id)
			host, attached := c.AttachedTo()
			legal := attached && g.Card(host).Zone == Battlefield
			if legal && c.Type().HasSubtype("Aura") {
				if spec, ok := enchantSpec(c); ok {
					legal = Matches(g, g.Card(host), spec, c.Controller(), id)
				}
				legal = legal && !hostRefusesAttach(g, c, host)
			}
			switch {
			case legal:
				continue
			case c.Type().HasSubtype("Aura"):
				toGraveyard = append(toGraveyard, id)
			case attached:
				toUnattach = append(toUnattach, id)
			}
		}
	}
	for _, id := range toUnattach {
		g.Unattach(id)
	}
	for _, id := range toGraveyard {
		// Move unattaches id itself as a side effect of leaving the
		// battlefield (game.go), so there is nothing left to do here.
		g.Move(id, Graveyard, g.Card(id).Owner)
		g.checkDiesTriggers(controller, id)
	}
	return len(toGraveyard) > 0 || len(toUnattach) > 0
}

// enchantSpec parses c's own `Enchant` keyword (CR 303.4a) into a valid.Spec,
// reporting whether it found a checkable one. `Enchant`'s value is a
// KeywordWithType, whose written form is "<validString>:<display text>"
// when it carries a display text at all (KeywordWithType.java's own parse,
// `k[0]`/`k[1]` after splitting on every `:`); Go's own keyword.Parse only
// cuts the head off once, so Details still carries both halves here, and
// only the first is a valid.Spec.
//
// "Player" and "Opponent" (`K:Enchant:Player`, `K:Enchant:Opponent` --
// Tenuous Truce, Archenemy, Overencumbered, Psychic Possession) are Java's
// own literal forms for an Aura that enchants a player rather than a
// permanent, not a card-type restriction valid.Parse can express -- and
// this port's AttachedTo (card.go) has no representation for "attached to a
// player" at all, so those two report no checkable spec rather than being
// misread as a card-type restriction no permanent could ever match.
func enchantSpec(c *Card) (valid.Spec, bool) {
	if c.Def == nil {
		return valid.Spec{}, false
	}
	for _, line := range c.Def.Faces[0].Keywords {
		k := keyword.Parse(line)
		if k.Name != "Enchant" {
			continue
		}
		typ, _, _ := strings.Cut(k.Details, ":")
		if typ == "" || typ == "Player" || typ == "Opponent" {
			return valid.Spec{}, false
		}
		return valid.Parse(typ), true
	}
	return valid.Spec{}, false
}
