// Exerting a permanent as an activation cost: Exert<1/CARDNAME|NICKNAME>
// (CR 701.42a) -- SelfSac/SelfExile/SelfReturn's own fourth self-reference
// sibling (ActivationShape.SelfExert, internal/cost). Every real corpus
// Exert<...> cost line names the self-reference shape; no chosen-type
// sibling exists the way Sac/Exile/Return each have one (0 real
// Exert<N/Type> lines for N > 1).
//
// Ported from forge-game/src/main/java/forge/game/card/Card.java's own
// exert(Player) -- unlike Sac/Exile/Return, exerting does not move the
// permanent at all, so this needs none of exileCards'/returnCards' own
// zone-change machinery (Move, g.LKI, the batched Mode$ ChangesZoneAll
// trigger): Card.Exerted (card.go) is set, CR 701.42a's own trigger fires
// (checkExertedTriggers, below), and the actual cost -- CR 701.42b's "it
// doesn't untap during your next untap step" -- is entirely deferred, paid
// off at the exerting player's own next untapStep (turn.go), not here.

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// isExertedTrigger reports whether t is CR 701.42a's own "becomes exerted"
// shape: Mode$ Exerted (TriggerExerted.java). Every one of the 5 real corpus
// T:Mode$ Exerted lines names only ValidCard$, so unlike isTapsTrigger's own
// checkTapsTriggers (trigger.go) this dispatch reads no further param --
// FirstTime$/Teamwork$/ValidCause$/ValidPlayer$/Attacker$, real params on
// the unrelated Mode$ Taps, name nothing here because 0 real Mode$ Exerted
// lines carry any of them.
func isExertedTrigger(t *compile.Ability) bool {
	return strings.EqualFold(t.Name, "Exerted")
}

// checkExertedTriggers fires CR 701.42a's own trigger -- TriggerExerted.
// performTest, ported -- once for the exerted card itself. Unlike Sac/Exile/
// Return's own checkXTriggers, this needs no separate own/other split and no
// g.LKI lookback at all: the card stays on the battlefield throughout
// (exerting is not a zone change), so the identical single unified
// battlefield walk checkTapsTriggers (trigger.go) already uses covers both
// "this card's own Exerted ability" (ValidCard$ Card.Self) and "another
// permanent watching a creature you control become exerted" (every one of
// the 5 real corpus lines' ValidCard$ Creature.YouCtrl shape) in the one
// pass.
func (g *Game) checkExertedTriggers(controller PlayerController, card CardID) {
	var matches []Ability
	c := g.Card(card)
	for _, pid := range g.Players() {
		for _, host := range g.Zone(Battlefield, pid).Cards() {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for face := range h.triggerFaces {
				for _, t := range face.Triggers {
					if !isExertedTrigger(t) {
						continue
					}
					if validCard, ok := t.Param("ValidCard"); ok && !Matches(g, c, valid.Parse(validCard), h.Controller(), host) {
						continue
					}
					if sub, api, optional, ok := triggerEffectAPI(g, h, face.Amounts, t); ok {
						matches = append(matches, Ability{API: api, Source: host, Controller: h.Controller(), Params: sub, Amounts: face.Amounts, Optional: optional, triggered: face.objects(triggeredObjects{})})
					}
				}
			}
		}
	}
	g.pushTriggeredAbilities(controller, matches)
}
