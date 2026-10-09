// Sneak (Keyword.SNEAK, Teenage Mutant Ninja Turtles): "You may cast this spell
// for [cost] if you also return an unblocked attacker you control to hand
// during the declare blockers step. [A permanent] enters tapped and
// attacking."
//
// Ported from forge-game/src/main/java/forge/game/card/CardFactoryUtil.java
// (the Sneak keyword branch, :3441-3467: the first spell copied with its mana
// cost replaced by "<cost> Return<1/Creature.attacking+unblocked/unblocked
// attacker>", instant speed, AlternativeCost.Sneak),
// SpellAbilityRestriction.checkTimingRestrictions (:312-316, the declare
// blockers step only) and PermanentEffect.resolve (:33-35, :55-63).

package engine

//enginelint:allow id zone card game control combat returncost effecthelpers phase player

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// sneakReturnSpec is the Return cost's type: an unblocked attacker the
// caster controls (CostReturn walks the payer's own battlefield).
const sneakReturnSpec = "Creature.attacking+unblocked"

// sneakCost is the mana cost of c's Sneak keyword. ok is false without the
// keyword, for a third field (Java's ValidAfterStack$, CardFactoryUtil.java:
// 3448-3450; no corpus line writes one) or for a cost that is not plain mana
// without X: such a Sneak is not offered, never cast for a guessed cost
// (GO-7).
func sneakCost(c *Card) (mana.Cost, bool) {
	for _, line := range c.KeywordLines() {
		k := keyword.Parse(line)
		if k.Name != "Sneak" {
			continue
		}
		if k.Details == "" || strings.Contains(k.Details, ":") {
			return mana.Cost{}, false
		}
		mc, err := mana.Parse(k.Details)
		if err != nil || mc.CountX() > 0 {
			return mana.Cost{}, false
		}
		return mc, true
	}
	return mana.Cost{}, false
}

// sneakAvailable is the Sneak cost pid may cast c from hand for now, offered
// beside the normal cast (castOptions): only during the declare blockers step
// (SpellAbilityRestriction.java:312-316), and only while pid controls an
// unblocked attacker to return. An Aura is never offered one: no corpus Aura
// has Sneak, and its cast path (castAura) does not pay the return.
func (g *Game) sneakAvailable(pid PlayerID, c *Card) (mana.Cost, bool) {
	if g.activePhase != DeclareBlockers || c.Type().HasSubtype("Aura") {
		return mana.Cost{}, false
	}
	mc, ok := sneakCost(c)
	if !ok || !g.sneakPayable(pid, c.ID) {
		return mana.Cost{}, false
	}
	return mc, true
}

// sneakReturnCandidates is every unblocked attacker pid controls, what the
// Sneak cost's Return part may pick.
func (g *Game) sneakReturnCandidates(pid PlayerID, card CardID) []CardID {
	return returnTypeCandidates(g, pid, card, sneakReturnSpec)
}

// sneakPayable reports whether pid can pay the Sneak cost's Return part.
func (g *Game) sneakPayable(pid PlayerID, card CardID) bool {
	return len(g.sneakReturnCandidates(pid, card)) > 0
}

// paySneakReturn pays the Sneak cost's Return part once the mana is paid
// (Cost.sort puts mana first): pid returns one unblocked attacker to its
// owner's hand. What it was attacking is kept on the spell's card for
// sneakJoinsCombat -- Java reads it back through the combat's last-known
// information of the returned card (sa.getPaidList("Returned"),
// Combat.getDefenderByAttacker). The creature leaves combat as it leaves the
// battlefield (GameAction.java:437-439); this port's Move does not do that on
// its own (combat.go's removeFromCombat). A controller answer that is not one
// candidate is a pending error and the cast fails (GO-7).
func (g *Game) paySneakReturn(controller PlayerController, pid PlayerID, card CardID) bool {
	candidates := g.sneakReturnCandidates(pid, card)
	chosen := controller.ChoosePermanentsToReturn(g, pid, candidates, 1)
	if err := checkChoice(chosen, candidates, 1, 1); err != nil {
		g.recordPendingError(fmt.Errorf("engine: Sneak: returned attacker: %w", err))
		return false
	}
	returned := chosen[0]
	g.Card(card).sneakDefender = g.combat.AttackTargets[returned]
	g.removeFromCombat(returned)
	returnCards(g, controller, chosen)
	return true
}

// sneakEntry reports whether the permanent spell source resolves as a
// sneaked one (PermanentEffect.java:33: cast for its Sneak cost), checked
// before anything moves. A sneaked creature whose recorded defender can no
// longer be attacked (a player who left the game, a planeswalker or battle
// gone from the battlefield) is an error: Java keeps it attacking a
// placeholder (Combat.removeFromCombat's CR 506.4c "<Nothing>" card), which
// this port has no model for.
func (g *Game) sneakEntry(source CardID) (bool, error) {
	c := g.Card(source)
	if !c.sneaked {
		return false, nil
	}
	if !c.Type().Has(cardtype.Creature) {
		return true, nil
	}
	if !g.sneakDefenderAttackable(c.sneakDefender) {
		return false, fmt.Errorf("engine: Sneak: %s's defender is no longer attackable; not resolvable yet", c.Def.Name)
	}
	return true, nil
}

// sneakDefenderAttackable reports whether d, a sneaked creature's recorded
// defender, can still be attacked: a player still in the game, or a
// planeswalker or battle still on the battlefield.
func (g *Game) sneakDefenderAttackable(d EntityID) bool {
	if pid, ok := d.AsPlayer(); ok {
		return !g.Player(pid).Lost
	}
	id, ok := d.AsCard()
	if !ok {
		return false
	}
	c := g.Card(id)
	return c.Zone == Battlefield && (c.Type().Has(cardtype.Planeswalker) || c.Type().Has(cardtype.Battle))
}

// sneakJoinsCombat is PermanentEffect.java:55-63: a sneaked creature now on
// the battlefield becomes an attacker of the defender its returned attacker
// was attacking, unblocked (its band's blocked flag set false). It was never
// declared as an attacker, so no "whenever ~ attacks" trigger fires (CR
// 508.4).
func (g *Game) sneakJoinsCombat(id CardID) {
	c := g.Card(id)
	if c.Zone != Battlefield || !c.Type().Has(cardtype.Creature) {
		return
	}
	g.combat.Attackers = append(g.combat.Attackers, id)
	if g.combat.AttackTargets == nil {
		g.combat.AttackTargets = make(map[CardID]EntityID)
	}
	g.combat.AttackTargets[id] = c.sneakDefender
	g.combat.blockDecided = append(g.combat.blockDecided, id)
}
