package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// isBecomeMonstrousTrigger reports whether t is CR 701.31's "becomes
// monstrous" shape, Mode$ BecomeMonstrous (TriggerBecomeMonstrous.java). Every
// one of the 19 corpus lines names only ValidCard$ Card.Self.
func isBecomeMonstrousTrigger(t *compile.Ability) bool {
	return strings.EqualFold(t.Name, "BecomeMonstrous")
}

// checkMonstrousTriggers fires TriggerBecomeMonstrous.performTest for the card
// that just became monstrous (CountersPutEffect.java:543-548), over the same
// battlefield walk checkExertedTriggers makes.
func (g *Game) checkMonstrousTriggers(controller PlayerController, card CardID) {
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
					if !isBecomeMonstrousTrigger(t) {
						continue
					}
					if validCard, ok := t.Param("ValidCard"); ok && !Matches(g, c, valid.Parse(validCard), h.Controller(), host) {
						continue
					}
					if sub, api, optional, ok := triggerEffectAPI(g, h, face.Amounts, t); ok {
						matches = append(matches, Ability{API: api, Source: host, Controller: h.Controller(), Params: sub, Amounts: face.Amounts, Optional: optional, triggered: face.objects(triggeredObjects{}), staticTrigger: isStaticTrigger(t)})
					}
				}
			}
		}
	}
	g.pushTriggeredAbilities(controller, matches)
}
