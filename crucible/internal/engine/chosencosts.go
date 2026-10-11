// The chosen-card parts of an activation cost: Sac<N/Type>, Exile<N/Type>,
// ExileFromGrave<N/Type> and Discard<N/Type>, where the payer picks N cards
// among those matching a type (CostSacrifice, CostExile, CostDiscard past
// their CARDNAME shapes). The picks are made before anything is paid, so a
// refused or illegal one costs nothing (CR 601.2h's ordering is free), then
// committed after the mana is.

//enginelint:allow id zone card game valid ability control discardeffect sacrificeeffect exile taptype exilefromgrave

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cost"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// chosenCosts is the cards picked for each chosen-card part.
type chosenCosts struct {
	sac, exile, exileGrave, discard []CardID
}

// costCandidates is the cards of zone pid owns or controls that spec (a Cost
// type: OR alternatives separated by ";") matches from source's point of view,
// source itself left out of a hand pick, and out of a battlefield pick when a
// self part of the same cost (SelfSac, SelfExile, SelfReturn) already moves
// it.
func (g *Game) costCandidates(pid PlayerID, source CardID, selfMoves bool, zone ZoneType, spec string) []CardID {
	parsed := valid.Parse(strings.ReplaceAll(spec, ";", ","))
	var out []CardID
	for _, id := range g.Zone(zone, pid).Cards() {
		c := g.Card(id)
		if zone == Battlefield && (c.IsPhasedOut() || selfMoves && id == source) || zone == Hand && id == source {
			continue
		}
		if Matches(g, c, parsed, pid, source) {
			out = append(out, id)
		}
	}
	return out
}

// chooseCostCards checks every chosen-card part of shape can be paid and has the
// controller pick its cards. false when one cannot, or when the pick is not N
// distinct candidates, or when two parts picked the same card.
func (g *Game) chooseCostCards(controller PlayerController, pid PlayerID, source CardID, shape cost.ActivationShape) (chosenCosts, bool) {
	var chosen chosenCosts
	selfMoves := shape.SelfSac || shape.SelfExile || shape.SelfReturn
	pick := func(n int, spec string, zone ZoneType, choose func([]CardID) []CardID) ([]CardID, bool) {
		if n == 0 {
			return nil, true
		}
		candidates := g.costCandidates(pid, source, selfMoves, zone, spec)
		if len(candidates) < n {
			return nil, false
		}
		picked := choose(candidates)
		if len(picked) != n || !isSubset(picked, candidates) || hasDuplicate(picked) {
			return nil, false
		}
		return picked, true
	}
	var ok bool
	if chosen.sac, ok = pick(shape.SacTypeN, shape.SacTypeSpec, Battlefield, func(c []CardID) []CardID {
		var allowed []CardID
		for _, id := range c {
			if !g.cantSacrifice(g.Card(id), false, nil) { // Card.canBeSacrificedBy
				allowed = append(allowed, id)
			}
		}
		if len(allowed) < shape.SacTypeN {
			return nil
		}
		return controller.ChoosePermanentsToSacrifice(g, pid, allowed, shape.SacTypeN)
	}); !ok {
		return chosenCosts{}, false
	}
	if chosen.exile, ok = pick(shape.ExileTypeN, shape.ExileTypeSpec, Battlefield, func(c []CardID) []CardID {
		return controller.ChooseCardsForEffect(g, pid, source, c, shape.ExileTypeN, shape.ExileTypeN)
	}); !ok {
		return chosenCosts{}, false
	}
	if chosen.exileGrave, ok = pick(shape.ExileGraveN, shape.ExileGraveSpec, Graveyard, func(c []CardID) []CardID {
		return controller.ChooseCardsForEffect(g, pid, source, c, shape.ExileGraveN, shape.ExileGraveN)
	}); !ok {
		return chosenCosts{}, false
	}
	if chosen.discard, ok = pick(shape.DiscardTypeN, shape.DiscardTypeSpec, Hand, func(c []CardID) []CardID {
		return controller.ChooseCardsToDiscard(g, pid, c, shape.DiscardTypeN)
	}); !ok {
		return chosenCosts{}, false
	}
	return chosen, !overlaps(chosen.sac, chosen.exile)
}

// payCostCards commits the picks and records them on a, the ability they paid
// for.
func (g *Game) payCostCards(controller PlayerController, pid PlayerID, chosen chosenCosts, a *Ability) {
	// A pick that left its zone since it was made (an earlier part of the same
	// cost moved it) is skipped by the move and so is not paid, nor recorded.
	in := func(zone ZoneType, ids []CardID) []CardID {
		var out []CardID
		for _, id := range ids {
			if g.Card(id).Zone == zone {
				out = append(out, id)
			}
		}
		return out
	}
	if sac := in(Battlefield, chosen.sac); len(sac) > 0 {
		sacrificeCardsFor(g, controller, a, sac, false)
		a.paid.sacrificed = append(a.paid.sacrificed, sac...)
	}
	if exile := in(Battlefield, chosen.exile); len(exile) > 0 {
		exileCards(g, controller, exile)
		a.paid.exiled = append(a.paid.exiled, exile...)
	}
	for _, id := range in(Graveyard, chosen.exileGrave) {
		exileFromGraveyard(g, controller, id)
		a.paid.exiled = append(a.paid.exiled, id)
	}
	if discard := in(Hand, chosen.discard); len(discard) > 0 {
		discardCards(g, controller, discard, pid)
		a.paid.discarded = append(a.paid.discarded, discard...)
	}
}

func hasDuplicate(ids []CardID) bool {
	for i, id := range ids {
		for _, other := range ids[:i] {
			if id == other {
				return true
			}
		}
	}
	return false
}

func overlaps(a, b []CardID) bool {
	for _, id := range a {
		for _, other := range b {
			if id == other {
				return true
			}
		}
	}
	return false
}
