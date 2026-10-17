// Event$ Moved replacements that act BEFORE a card enters the battlefield:
// the ones whose ReplaceWith$ chain moves the card itself (Lake of the Dead's
// "sacrifice a Swamp instead, if you do put it onto the battlefield, if you
// don't put it into its owner's graveyard", Containment Priest's "exile it
// instead", Captive Audience's "enters under the control of an opponent of
// your choice") and the "as it enters" ones that run first and let the move
// go on (Lich's ReplacementResult$ Updated LoseLife).
//
// Java runs ReplacementHandler.run(Moved) in GameAction.changeZone before the
// card leaves its zone; a Replaced result returns at once (GameAction.java:
// 334-365), so no zone change, no table entry and no ETB trigger happen for
// the entry that was replaced. The ChangeZone ability in the chain moves the
// card through a second, full changeZone, and ReplacementEffect.hasRun keeps
// the same replacement from applying to that nested move
// (ReplacementHandler.java:137, :227-228). Here every site that puts a card
// onto the battlefield (permanentEffect, attachEffect, playLandNow,
// moveByEffect) calls entryReplaced before Game.Move; runReplacements keeps
// the applied line in Game.replacing for the length of the chain, which is
// that hasRun.
//
// Layer order is ReplacementLayer.java:9-13: Control, then Copy ("enters as a
// copy", applyCopyReplacements in entersascopy.go), then Other. The Copy layer
// runs here too, before the move: a Clone entering under Containment Priest
// copies first and is then exiled as the copy, and the entering card's copy
// effect is dropped when the entry does not land (CR 400.7).

package engine

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// entryReplaced runs the pre-entry Moved replacements for moved about to
// enter the battlefield under entering from origin. It reports whether one
// replaced the entry: the caller then does not move the card, run the enter
// replacements or fire ETB triggers, because the replacement's own chain
// moved it (or did not) through a nested entry.
func (g *Game) entryReplaced(controller PlayerController, moved CardID, origin ZoneType, entering PlayerID) bool {
	card := g.Card(moved)
	if card.Def == nil {
		return false
	}
	// Java matches the entry on the card as it would exist on the battlefield,
	// controlled by whoever it enters under.
	savedController := card.controller
	card.controller = entering
	replaced := g.runEntryLayer(controller, card, origin, entering, "Control")
	if !replaced {
		// The Copy layer: the card takes its copied definition before the
		// move (CR 614.12), so the Other layer below and the move itself read
		// the copy, as ReplacementHandler.run's later layers do.
		g.refreshLayers()
		g.applyCopyReplacements(controller, moved, origin, entering)
		replaced = g.runEntryLayer(controller, card, origin, entering, "Other")
	}
	if replaced && card.Zone != Battlefield {
		// The entry did not land: a copy effect and the counters it was to
		// enter with go with the object that never entered (CR 400.7), not
		// onto the card in its new zone.
		if len(card.copies) > 0 {
			card.setCopies(nil)
		}
		card.pendingEnter, card.devoured = nil, nil
	}
	if replaced && card.Zone != Battlefield {
		// A card the chain left outside the battlefield is not under the
		// entering player's control (CR 108.4a); Move resets it for a spell
		// leaving the stack and for the battlefield, not for a card in hand.
		if card.Zone != Stack {
			card.controller = savedController
		}
	} else if !replaced {
		card.controller = savedController
	}
	return replaced
}

// runEntryLayer runs one ReplacementLayer's pre-entry Moved replacements for
// card's entry (a runReplacements walk) and reports whether one replaced it.
func (g *Game) runEntryLayer(controller PlayerController, card *Card, origin ZoneType, entering PlayerID, layer string) bool {
	return g.runReplacements(controller, entering, func() []replacementCandidate {
		return g.entryCandidates(controller, card, origin, layer)
	}) == replacementReplaced
}

// entryCandidates lists the Event$ Moved lines of the given layer that apply
// to moved's entry and are resolvable here: moved's own (its faces and its
// perpetual grants, from whatever zone it is entering from), then every
// other trait host in play.
func (g *Game) entryCandidates(controller PlayerController, moved *Card, origin ZoneType, layer string) []replacementCandidate {
	var out []replacementCandidate
	consider := func(h *Card, z ZoneType, self bool, amounts map[string]expr.Amount, r *compile.Ability) {
		sub, updated, ok := g.entryShape(r, moved, origin, h, z, self, amounts, layer)
		if !ok {
			return
		}
		out = append(out, replacementCandidate{host: h, rule: r, apply: func() replacementResult {
			result := replacementReplaced
			if updated {
				result = replacementUpdated
			}
			if err := g.runReplacementChain(controller, h, amounts, sub, &replacementEvent{result: result, card: moved.ID}); err != nil {
				g.recordPendingError(err)
			}
			return result
		}})
	}
	for _, face := range moved.traitFaces() {
		for _, r := range face.Replacements {
			consider(moved, moved.Zone, true, face.Amounts, r)
		}
	}
	for _, grant := range moved.grants {
		for _, r := range grant.replacements {
			consider(moved, moved.Zone, true, grant.amounts, r)
		}
	}
	g.eachReplacementRule(func(h *Card, z ZoneType, amounts map[string]expr.Amount, r *compile.Ability) {
		if h.ID != moved.ID {
			consider(h, z, false, amounts, r)
		}
	})
	return out
}

// entryShape is whether r is a pre-entry Event$ Moved line this port runs for
// moved's entry, and its ReplaceWith$ ability. updated is the "as it enters"
// shape (ReplacementResult$ Updated): the ability runs and the entry
// continues. Otherwise the ability's chain must move the replaced card
// itself (a ChangeZone with Defined$ ReplacedCard), the shape that stands in
// for the entry.
func (g *Game) entryShape(r *compile.Ability, moved *Card, origin ZoneType, h *Card, hz ZoneType, self bool, amounts map[string]expr.Amount, layer string) (sub *compile.Ability, updated, ok bool) {
	if !strings.EqualFold(r.Name, "Moved") || !replacementZoneMatches(r, "Destination", Battlefield) || !replacementZoneMatches(r, "Origin", origin) {
		return nil, false, false
	}
	// A FoundSearchingLibrary$ line applies only to a card a library search
	// found (foundSearchReplaced): no entry this function sees carries that run
	// param (ReplaceMoved.java:92-95).
	if _, found := r.Param("FoundSearchingLibrary"); found {
		return nil, false, false
	}
	lineLayer, _ := r.Param("Layer")
	if lineLayer == "" {
		lineLayer = "Other"
	}
	if !strings.EqualFold(lineLayer, layer) {
		return nil, false, false
	}
	sub = replaceWithSub(r)
	if sub == nil {
		return nil, false, false
	}
	// unhandled is an "as it enters" line no entry path runs: it is matched
	// like the rest below and, when it applies, refused loudly rather than
	// skipped (GO-7).
	unhandled := false
	if v, has := r.Param("ReplacementResult"); has && strings.EqualFold(v, "Updated") {
		updated = true
		if !entryUpdatedShape(sub) {
			if enterShapeHandledAfterMove(sub) {
				return nil, false, false
			}
			unhandled = true
		}
	} else if !chainMovesReplacedCard(sub) {
		return nil, false, false
	}
	// A self line reads off the card's battlefield-bound copy whatever
	// ActiveZones$ says (getReplacementList's affectedLKI).
	if !self && !hostInActiveZones(h, r, hz) {
		return nil, false, false
	}
	validCard, has := r.Param("ValidCard")
	if !has || !Matches(g, moved, valid.Parse(validCard), h.Controller(), h.ID) {
		return nil, false, false
	}
	if !replacementRequirementsCheck(g, h, amounts, r) {
		return nil, false, false
	}
	if unhandled {
		g.recordPendingError(fmt.Errorf("engine: %q: as-it-enters replacement ReplaceWith$ %s not resolvable yet", h.Def.Name, sub.Name))
		return nil, false, false
	}
	return sub, updated, true
}

// foundSearchReplaced runs the Event$ Moved lines naming FoundSearchingLibrary$
// for id, a card a library search found, about to move from its zone to dest:
// changeHiddenOriginResolve passes FoundSearchingLibrary with every such move
// (ChangeZoneEffect.java:1308) and ReplaceMoved.canReplace admits the line only
// then (ReplaceMoved.java:92-98). Opposition Agent's "they exile each card they
// find" is the one corpus line. It reports whether a line replaced the move;
// its ReplaceWith$ chain moved the card itself (a ChangeZone of Defined$
// ReplacedCard), and the move it replaced does not happen.
//
// The CR 616 walk is the card controller's (ReplacementHandler's decider for a
// card), over these lines only: every other Moved line applies to whichever
// move then happens, the chain's own or the original, so the walk order
// against them is not modelled. A line with a param this port does not read,
// or a chain that does not move the replaced card, records a pending error
// and does not apply (GO-7).
func (g *Game) foundSearchReplaced(controller PlayerController, id CardID, dest ZoneType) bool {
	card := g.Card(id)
	return g.runReplacements(controller, card.Controller(), func() []replacementCandidate {
		var out []replacementCandidate
		g.eachReplacementRule(func(h *Card, z ZoneType, amounts map[string]expr.Amount, r *compile.Ability) {
			if !strings.EqualFold(r.Name, "Moved") || !hostInActiveZones(h, r, z) {
				return
			}
			if _, found := r.Param("FoundSearchingLibrary"); !found {
				return
			}
			if !replacementZoneMatches(r, "Origin", card.Zone) || !replacementZoneMatches(r, "Destination", dest) {
				return
			}
			sub := replaceWithSub(r)
			if layer, _ := r.Param("Layer"); (layer != "" && !strings.EqualFold(layer, "Other")) || sub == nil || !chainMovesReplacedCard(sub) ||
				!onlyParams(r, "validcard", "foundsearchinglibrary", "origin", "destination", "layer", "optional", "optionaldecider") {
				g.recordPendingError(fmt.Errorf("engine: %q: Event$ Moved FoundSearchingLibrary$: shape not resolvable yet", h.Def.Name))
				return
			}
			if v, ok := r.Param("ValidCard"); ok && !Matches(g, card, valid.Parse(v), h.Controller(), h.ID) {
				return
			}
			if !replacementRequirementsCheck(g, h, amounts, r) {
				return
			}
			out = append(out, replacementCandidate{host: h, rule: r, apply: func() replacementResult {
				from := card.Zone
				if err := g.runReplacementChain(controller, h, amounts, sub, &replacementEvent{result: replacementReplaced, card: id}); err != nil {
					g.recordPendingError(err)
					if card.Zone == from {
						// The chain failed before it moved the card: the search's
						// own move still happens.
						return replacementNotReplaced
					}
				}
				return replacementReplaced
			}})
		})
		return out
	}) == replacementReplaced
}

// chainMovesReplacedCard reports whether the ReplaceWith$ chain starting at
// sub contains a ChangeZone of Defined$ ReplacedCard: the ability that puts
// the replaced card somewhere in place of the entry.
func chainMovesReplacedCard(sub *compile.Ability) bool {
	for a := sub; a != nil; a = chainNext(a) {
		if strings.EqualFold(a.Name, "ChangeZone") {
			if d, ok := a.Param("Defined"); ok && strings.EqualFold(d, "ReplacedCard") {
				return true
			}
		}
	}
	return false
}

// chainNext is a's chained SubAbility$, nil when it has none.
func chainNext(a *compile.Ability) *compile.Ability {
	for _, sub := range a.Subs {
		if strings.EqualFold(sub.Key, "SubAbility") {
			return sub.Ability
		}
	}
	return nil
}

// entryUpdatedShape is the set of "as it enters" abilities that run before
// the move: a plain life loss (Lich), the StoreSVar that remembers the life
// paid as the card enters (Phyrexian Processor, Minion of the Wastes, Nameless
// Race: payReplacementLifeX pays its Cost$), Devour's sacrifice, whose
// chain's ETB$ counters wait on the card for applyEnterCounters (devour.go),
// and a chain of entryChainAPIs ("as CARDNAME enters, choose ...").
func entryUpdatedShape(sub *compile.Ability) bool {
	if strings.EqualFold(sub.Name, "StoreSVar") {
		if cost, ok := sub.Param("Cost"); ok {
			return cost == replacementPayLifeX
		}
	}
	if strings.EqualFold(sub.Name, "PutCounter") {
		// A chain headed by its counters is applyEnterCounters', after the move.
		return false
	}
	return strings.EqualFold(sub.Name, "LoseLife") || isDevourChain(sub) || entryChainRunsBeforeMove(sub, 0)
}

// entryChainAPIs are the abilities an "as CARDNAME enters" chain may be made
// of and still run before the card lands. K:ETBReplacement:Other makes the
// chain the replacement's overriding ability (CardFactoryUtil.java:515-543),
// which ReplacementHandler runs before the move; ADR-0022 runs it the same way,
// through the Registry on the entering card (runReplacementChain), so a choice
// is on the card's Memory (which Game.Move keeps) before its statics, ETB
// triggers and SBAs look: Cavern of Souls' ChooseType, Iona's ChooseColor,
// Pithing Needle's NameCard, Rick's GenericChoice of StoreSVars. The others
// act on other objects or on the entry's own counter table (PutCounter ETB$,
// putEnterCounters): B.O.B.'s tokens, Urborg Lhurgoyf's mill, Wood
// Elemental's sacrifice, Dungeon Master's die. An ability that would change
// the entering card itself (Animate, AlterAttribute, Tap, SetState) is not
// here: Game.Move rebuilds what such an effect writes, so it would be lost.
// A param an effect does not resolve is that effect's own error (ChooseColor's
// Random$ on Rainbow Knights), which runReplacementChain records (GO-7).
var entryChainAPIs = [...]string{
	"ChooseColor", "ChooseType", "ChoosePlayer", "ChooseNumber", "ChooseCard",
	"ChooseEvenOdd", "ChooseDirection", "NameCard", "GenericChoice", "StoreSVar", "Cleanup",
	"Mill", "Discard", "Draw", "Token", "Reveal", "RevealHand", "Sacrifice", "SacrificeAll",
	"RollDice", "ChangeZoneAll",
}

// entryChainRunsBeforeMove reports whether every ability reachable from a
// (its SubAbility$ chain and any other sub, a GenericChoice's choices) is an
// entryChainAPIs entry, a PutCounter into the entry's counter table (ETB$) or
// onto another card (Phylactery Lich's Defined$ ChosenCard), or a
// DelayedTrigger, whose Execute$ runs later and is not walked (Predatory
// Sludge). depth bounds a cyclic script.
func entryChainRunsBeforeMove(a *compile.Ability, depth int) bool {
	if depth > 32 {
		return false
	}
	delayed := strings.EqualFold(a.Name, "DelayedTrigger")
	ok := delayed
	if strings.EqualFold(a.Name, "PutCounter") {
		_, etb := a.Param("ETB")
		defined, hasDefined := a.Param("Defined")
		ok = etb || (hasDefined && !strings.EqualFold(defined, "Self") && !strings.HasPrefix(defined, "Replaced"))
	}
	for _, api := range entryChainAPIs {
		ok = ok || strings.EqualFold(a.Name, api)
	}
	if !ok {
		return false
	}
	for _, s := range a.Subs {
		if delayed && !strings.EqualFold(s.Key, "SubAbility") {
			continue
		}
		if s.Ability != nil && !entryChainRunsBeforeMove(s.Ability, depth+1) {
			return false
		}
	}
	return true
}

// enterShapeHandledAfterMove reports whether sub, an "as it enters" Moved
// line's ability, is one a path after the move runs: "enters tapped" or
// "untapped" and "it becomes day" (checkMovedReplacement,
// movedBattlefieldMatch's shapes) and "enters with counters" (PutCounter with
// ETB$, applyEnterCounters, which refuses its own unresolvable shapes). Every
// Tap line is left to checkMovedReplacement, whose own tapShapeRecognized
// decides it.
func enterShapeHandledAfterMove(sub *compile.Ability) bool {
	switch {
	case strings.EqualFold(sub.Name, "Tap"):
		return true
	case strings.EqualFold(sub.Name, "Untap"):
		defined, ok := sub.Param("Defined")
		return ok && strings.EqualFold(defined, "ReplacedCard") && onlyParamsOf(sub, "db", "defined", "etb")
	case strings.EqualFold(sub.Name, "DayTime"):
		return true
	case strings.EqualFold(sub.Name, "PutCounter"):
		_, etb := sub.Param("ETB")
		return etb
	}
	return false
}
