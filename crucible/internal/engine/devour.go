// Devour (CR 702.82): as a permanent with devour enters, its controller may
// sacrifice any number of other permanents of the devour type, and it enters
// with N +1/+1 counters for each. keyword.Expand writes the keyword as
// CardFactoryUtil does (an Event$ Moved | ReplacementResult$ Updated line whose
// ReplaceWith$ chain is Sacrifice, PutCounter ETB$, Cleanup); the chain runs
// before the move (replacemententry.go's entryUpdatedShape), so the devourer
// is not yet on the battlefield while its food dies, and the counters join the
// entry's counter table (putEnterCounters). Ported from SacrificeEffect.java's
// `sa.isKeyword(Keyword.DEVOUR)` branch (:162-169), Card.addDevoured
// (Card.java:1189-1194), AbilityUtils.xCount's Devoured head (:2194-2197),
// CardProperty's HasDevoured (:1823-1826) and TriggerDevoured.

package engine

//enginelint:allow id card game ability valid control trigger triggerscan staticability amount

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// isDevourAbility reports whether a is the sacrifice Devour's keyword
// synthesized (SpellAbility.isKeyword(Keyword.DEVOUR)).
func isDevourAbility(a *Ability) bool {
	return a.Params != nil && isDevourChain(a.Params)
}

// isDevourChain reports whether sub, a ReplaceWith$ ability, is the head of
// the chain keyword.Expand writes for Devour: its Sacrifice.
func isDevourChain(sub *compile.Ability) bool {
	return sub.Keyword != "" && strings.EqualFold(sub.Name, "Sacrifice") && keyword.Parse(sub.Keyword).Name == "Devour"
}

// noteDevoured is SacrificeEffect.java:164-168 for one permanent a Devour
// sacrifice of devourer's put into the graveyard: devourer adds it to its
// devoured cards, and Mode$ Devoured triggers run with it.
func (g *Game) noteDevoured(controller PlayerController, devourer, food CardID) {
	c := g.Card(devourer)
	c.devoured = append(append([]CardID(nil), c.devoured...), food)
	g.checkDevouredTriggers(controller, food)
}

// devouredCards is c's devoured permanents, each as it last was on the
// battlefield.
func (g *Game) devouredCards(c *Card) []*Card {
	return g.cardsOf(c.devoured, true)
}

// devouredCount is Count$Devoured <valid> (AbilityUtils.java:2194-2197): the
// source's devoured permanents matching the valid string, from the source
// controller's side. An empty valid string is unresolved, as Java's split
// would fail on it.
func (g *Game) devouredCount(sourceController PlayerID, source CardID, spec string) (int, bool) {
	if source == NoCard || spec == "" {
		return 0, false
	}
	parsed := valid.Parse(spec)
	n := 0
	for _, food := range g.devouredCards(g.Card(source)) {
		if Matches(g, food, parsed, sourceController, source) {
			n++
		}
	}
	return n, true
}

// devouredTriggerParams are the params a Mode$ Devoured trigger may carry
// (TriggerDevoured.performTest reads ValidDevoured$).
var devouredTriggerParams = map[string]bool{
	"mode": true, "execute": true, "triggerdescription": true, "triggerzones": true, "optionaldecider": true,
	"secondary": true, "activationlimit": true, "validdevoured": true,
}

// checkDevouredTriggers runs Mode$ Devoured for food (TriggerDevoured.java:
// 56-68): ValidDevoured$ against the devoured permanent as it last was, which
// is TriggeredDevoured.
func (g *Game) checkDevouredTriggers(controller PlayerController, food CardID) {
	lki := g.Card(food)
	if snap := g.LKI(food); snap != nil {
		lki = snap
	}
	matches := g.scanTriggers([]string{"Devoured"}, func(h *Card, face triggerFace, t *compile.Ability) (triggeredObjects, bool) {
		if !paramsResolvable(t, devouredTriggerParams) || !g.triggerCardMatches(h, t, "ValidDevoured", lki) {
			return triggeredObjects{}, false
		}
		return face.objects(triggeredObjects{devoured: food}), true
	})
	g.pushTriggeredAbilities(controller, matches)
}
