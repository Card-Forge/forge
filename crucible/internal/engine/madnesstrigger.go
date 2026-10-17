// Madness's trigger: "When you do [discard it into exile], cast it for its
// madness cost or put it into your graveyard" (CR 702.35a), the Mode$ Exiled
// trigger keyword.Expand writes for K:Madness (CardFactoryUtil.java:
// 1386-1420), fired by the discard replacement (replacementdiscard.go).

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// checkMadnessTriggers fires card's own Mode$ Exiled triggers that the exile
// its Madness replacement just made satisfies: TriggerExiled.performTest
// admits a Madness trigger only when the exile's cause is the same Madness
// keyword's ability (TriggerExiled.java:87-94), here the keyword line both
// were expanded from. ValidCard$ Card.Self is the card itself, now in exile.
// No other Mode$ Exiled trigger fires from here: the corpus's few others
// (Soulherder's Origin$ Battlefield) are not this event.
func (g *Game) checkMadnessTriggers(controller PlayerController, card CardID, keywordLine string) {
	c := g.Card(card)
	if c.Def == nil || keywordLine == "" || keyword.Parse(keywordLine).Name != "Madness" {
		return
	}
	var matches []Ability
	for face := range c.triggerFaces {
		for _, t := range face.Triggers {
			if !strings.EqualFold(t.Name, "Exiled") || t.Keyword != keywordLine {
				continue
			}
			if v, ok := t.Param("ValidCard"); ok && !Matches(g, c, valid.Parse(v), c.Controller(), card) {
				continue
			}
			if sub, api, optional, ok := triggerEffectAPI(g, c, face.Amounts, t); ok {
				matches = append(matches, Ability{API: api, Source: card, Controller: c.Controller(), Params: sub, Amounts: face.Amounts, Optional: optional,
					triggered: face.objects(triggeredObjects{card: card}), staticTrigger: isStaticTrigger(t)})
			}
		}
	}
	g.pushTriggeredAbilities(controller, matches)
}
