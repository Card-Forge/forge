// Costs a spell's controller pays partly with permanents or cards instead of
// mana: Convoke (CR 702.51), Improvise (702.126) and Delve (702.66).
// CostAdjustment.adjust (CostAdjustment.java) reduces the mana to pay for each;
// here the controller picks the permanents or cards, the reduced cost is paid,
// and the taps or exiles happen only once the mana was.

//enginelint:allow id zone card game control manapay taptype parts leftto

package engine

import (
	"slices"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// costAssist is what paying a reduced cost will tap and exile.
type costAssist struct {
	tap, exile []CardID
}

// assistCost reduces total by what the caster chooses to convoke, improvise and
// delve (each only if c has the keyword and there is something to reduce),
// returning the cost still to pay and what to tap and exile once it is paid.
// A pick that is not a subset of the candidates, or that pays more than the
// cost holds, is declined whole: the spell is cast without that assist.
func (g *Game) assistCost(controller PlayerController, pid PlayerID, c *Card, total mana.Cost) (mana.Cost, costAssist) {
	var assist costAssist
	if total.IsNoCost() {
		return total, assist
	}
	generic, shards := total.Generic(), append([]mana.Shard(nil), total.Shards()...)

	if c.HasKeyword("Delve") && generic > 0 {
		grave := g.Zone(Graveyard, pid).Cards()
		if len(grave) > 0 {
			picks := controller.ChooseCardsForEffect(g, pid, c.ID, grave, 0, min(generic, len(grave)))
			if isSubset(picks, grave) && len(picks) <= generic {
				generic -= len(picks)
				assist.exile = picks
			}
		}
	}
	if c.HasKeyword("Convoke") {
		var candidates []CardID
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if p := g.Card(id); p.Type().Has(cardtype.Creature) && !p.Tapped && id != c.ID {
				candidates = append(candidates, id)
			}
		}
		if len(candidates) > 0 && (generic > 0 || len(shards) > 0) {
			picks := controller.ChooseCardsForEffect(g, pid, c.ID, candidates, 0, len(candidates))
			if g2, s2, ok := convokePay(g, picks, candidates, generic, shards); ok {
				generic, shards = g2, s2
				assist.tap = append(assist.tap, picks...)
			}
		}
	}
	if c.HasKeyword("Improvise") && generic > 0 {
		var candidates []CardID
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if p := g.Card(id); p.Type().Has(cardtype.Artifact) && !p.Tapped && !slices.Contains(assist.tap, id) {
				candidates = append(candidates, id)
			}
		}
		if len(candidates) > 0 {
			picks := controller.ChooseCardsForEffect(g, pid, c.ID, candidates, 0, min(generic, len(candidates)))
			if isSubset(picks, candidates) && len(picks) <= generic {
				generic -= len(picks)
				assist.tap = append(assist.tap, picks...)
			}
		}
	}
	return mana.FromShards(shards, generic), assist
}

// convokePay applies one tapped creature each: it pays one of the cost's
// colored shards when it shares that color, else one generic. ok is false when
// the pick is not a subset of the candidates or a creature has nothing to pay.
func convokePay(g *Game, picks, candidates []CardID, generic int, shards []mana.Shard) (int, []mana.Shard, bool) {
	if !isSubset(picks, candidates) {
		return 0, nil, false
	}
	shards = append([]mana.Shard(nil), shards...)
	for _, id := range picks {
		colors := g.Card(id).Colors()
		paid := false
		for i, sh := range shards {
			if sh.Colors() != 0 && sh.Colors()&colors != 0 && sh.Colors()&(sh.Colors()-1) == 0 {
				shards = slices.Delete(shards, i, i+1)
				paid = true
				break
			}
		}
		if !paid {
			if generic == 0 {
				return 0, nil, false
			}
			generic--
		}
	}
	return generic, shards, true
}

// settle taps and exiles what the paid cost used. A delved card leaves its
// graveyard: its graveyard-origin triggers wait for the spell's SpellCast
// batch (noteGraveyardExit): settle runs only inside castSpellChosen, which
// holds them.
func (a costAssist) settle(g *Game, controller PlayerController) {
	tapChosenPermanents(g, controller, a.tap)
	for _, id := range a.exile {
		c := g.Card(id)
		from := c.Zone
		g.Move(id, Exile, c.Owner)
		g.noteGraveyardExit(id, from)
	}
}
