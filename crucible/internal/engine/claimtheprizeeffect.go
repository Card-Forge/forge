// ClaimThePrize: Unfinity's Attraction "claim the prize" (CR 725, the
// Attraction subtype's own rules), fired by an Attraction's own Visit
// ability once its luck-counter threshold is met -- pick_a_beeble.txt's own
// "DB$ ClaimThePrize | ConditionDefined$ Self | ConditionPresent$
// Card.Self+counters_GE6_LUCK" is the one real corpus line, and
// the_most_dangerous_gamer.txt's "T:Mode$ ClaimPrize | ValidCard$
// Attraction.YouCtrl" is the one real corpus trigger listener.

package engine

//enginelint:allow ability card condition control defined effecthelpers game id parts trigger valid zone

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// claimThePrizeEffect is ClaimThePrizeEffect.java: for each Defined$
// (default Self) card, run CR's own Mode$ ClaimPrize trigger
// (TriggerHandler.runTrigger(TriggerType.ClaimPrize, ...)) once.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/
// ClaimThePrizeEffect.java's resolve. Java's resolve reads only Defined$.
//
// ConditionDefined$ is rejected, not silently gated: the one real corpus
// line (pick_a_beeble.txt's "ConditionDefined$ Self | ConditionPresent$
// Card.Self+counters_GE6_LUCK") named it assuming subAbilityConditionMet
// would evaluate it, but isPresentMatches (trigger.go) treats any
// ConditionDefined$ as never met and returns false silently -- this effect
// would resolve to nil, quietly doing nothing, on the corpus's only real
// use. Discovered porting SetInMotion (effects-setinmotion.md); fixed here
// the way every other effect naming ConditionDefined$ already does (GO-7),
// rather than resolving to an invisible no-op.
type claimThePrizeEffect struct{}

// claimThePrizeUnresolvedParams names the one shape isPresentMatches cannot
// evaluate (see the doc comment above): rejected loudly before acting,
// never silently gated to "condition not met".
var claimThePrizeUnresolvedParams = [...]string{"ConditionDefined"}

func (claimThePrizeEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "ClaimThePrize", claimThePrizeUnresolvedParams[:]...); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return err
	}
	for _, id := range cards {
		g.checkClaimPrizeTriggers(controller, id)
	}
	return nil
}

// isClaimPrizeTrigger reports whether t is CR's own Mode$ ClaimPrize shape
// (TriggerClaimPrize.java). The one real corpus line
// (the_most_dangerous_gamer.txt) names only ValidCard$ and TriggerZones$
// Battlefield (phaseTriggerZoneMatches' own default when absent), so no
// further param is read here.
func isClaimPrizeTrigger(t *compile.Ability) bool {
	return strings.EqualFold(t.Name, "ClaimPrize")
}

// checkClaimPrizeTriggers fires CR's own "whenever you claim the prize of
// an Attraction" trigger for claimed -- checkExertedTriggers' own exact
// structural sibling (exertcost.go): a single unified battlefield walk
// covers both the claimed Attraction's own ClaimPrize ability (ValidCard$
// Card.Self, pick_a_beeble.txt's own K:Prize keyword, not itself compiled by
// this port yet) and another permanent's watching trigger (ValidCard$
// Attraction.YouCtrl, the_most_dangerous_gamer.txt) in the one pass.
func (g *Game) checkClaimPrizeTriggers(controller PlayerController, claimed CardID) {
	var matches []Ability
	c := g.Card(claimed)
	for _, pid := range g.Players() {
		for _, host := range g.Zone(Battlefield, pid).Cards() {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for face := range h.triggerFaces {
				for _, t := range face.Triggers {
					if !isClaimPrizeTrigger(t) {
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
