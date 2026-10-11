// Mode$ LifeLost (CR 119.3, TriggerLifeLost.performTest): the trigger a life
// loss -- a LoseLife effect, damage, a payment of life -- fires after
// Player.loseLife has reduced the total. FirstTime$ is the first loss this
// turn, LifeAmount$ compares the amount of this one, ValidPlayer$ names who
// lost. ValidCause$ (the ability that caused it) and the batched Mode$
// LifeLostAll are not matched: a trigger naming either does not fire (GO-7).

package engine

import (
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

func isLifeLostTrigger(t *compile.Ability) bool {
	return strings.EqualFold(t.Name, "LifeLost")
}

// noteLifeLost records that pid lost amount life and fires the LifeLost
// triggers it raises. Called once the life total has been reduced.
func (g *Game) noteLifeLost(controller PlayerController, pid PlayerID, amount int) {
	if amount <= 0 {
		return
	}
	p := g.Player(pid)
	first := p.LifeLostThisTurn == 0
	p.LifeLostThisTurn += amount

	var matches []Ability
	for _, owner := range g.Players() {
		for _, z := range phaseTriggerZones {
			for _, host := range g.Zone(z, owner).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for face := range h.triggerFaces {
					for _, t := range face.Triggers {
						if !isLifeLostTrigger(t) || hasAnyParam(t, "ValidCause") {
							continue
						}
						if !phaseTriggerZoneMatches(h, t, z) {
							continue
						}
						spec, ok := t.Param("ValidPlayer")
						if !ok {
							continue
						}
						matched, recognized := matchesPlayerSpec(g, pid, h.Controller(), host, spec)
						if !recognized || !matched {
							continue
						}
						if _, ok := t.Param("FirstTime"); ok && !first {
							continue
						}
						if cmp, ok := t.Param("LifeAmount"); ok {
							operand, err := strconv.Atoi(cmp[min(2, len(cmp)):])
							if err != nil || len(cmp) < 3 || !compareOp(amount, cmp[:2], operand) {
								continue
							}
						}
						if sub, api, optional, ok := triggerEffectAPI(g, h, face.Amounts, t); ok {
							matches = append(matches, Ability{API: api, Source: host, Controller: h.Controller(), Params: sub, Amounts: face.Amounts, Optional: optional,
								triggered: face.objects(triggeredObjects{player: pid, counts: triggerCounts{life: amount, set: countLife}}), staticTrigger: isStaticTrigger(t)})
						}
					}
				}
			}
		}
	}
	g.pushTriggeredAbilities(controller, matches)
}
