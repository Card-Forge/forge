// Discard replacements: the Event$ Moved lines with Discard$ True, which
// ReplaceMoved admits only for a move a discard makes (ReplaceMoved.java:
// 79-83; Player.discard puts AbilityKey.Discard and EffectOnly into the move's
// run params, Player.java:1431-1433). Madness's "if you discard this card,
// discard it into exile" (CardFactoryUtil.java:2345-2362) is the shape that
// resolves; its exile fires the keyword's own Mode$ Exiled trigger ("when you
// do, cast it for its madness cost or put it into your graveyard",
// CardFactoryUtil.java:1386-1420). The card is still discarded: the
// Discarded triggers run after the move either way (Player.java:1448-1452).

package engine

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// discardToGraveyard is Player.discard's move (Player.java:1424-1455): id
// leaves its owner's hand for the graveyard, through a discard replacement
// when one applies (discardReplaced), else through the graveyard's own Moved
// replacements (moveToGraveyard). The caller fires the Discarded triggers.
func (g *Game) discardToGraveyard(controller PlayerController, id CardID, cause discardCause) {
	if g.discardReplaced(controller, id, cause) {
		return
	}
	g.moveToGraveyard(controller, id)
}

// discardLine is one Discard$ True line that applies to a discard: its host,
// the face's amounts and the line.
type discardLine struct {
	host    *Card
	amounts map[string]expr.Amount
	rule    *compile.Ability
}

// discardReplaced applies the Discard$ True line that applies to id's
// discard, reporting whether one did. The madness shape -- ReplaceWith$ a
// ChangeZone of Defined$ ReplacedCard to Exile, nothing else on the line --
// exiles the card and fires the madness trigger. Any other line that applies
// (Loxodon Smiter's "put it onto the battlefield instead", Library of Leng's
// "you may put it on top of your library") is a pending error, and so is a
// discard line together with a graveyard replacement that would also apply
// (Rest in Peace): CR 616.1 has the affected player order them, which this
// does not model.
func (g *Game) discardReplaced(controller PlayerController, id CardID, cause discardCause) bool {
	c := g.Card(id)
	var lines []discardLine
	consider := func(h *Card, amounts map[string]expr.Amount, r *compile.Ability) bool {
		if v, ok := r.Param("Discard"); !ok || !strings.EqualFold(v, "True") {
			return false
		}
		if !replacementZoneMatches(r, "Origin", c.Zone) || !replacementZoneMatches(r, "Destination", Graveyard) {
			return false
		}
		if v, ok := r.Param("ValidCard"); ok && !Matches(g, c, valid.Parse(v), h.Controller(), h.ID) {
			return false
		}
		applies, err := discardCauseMatches(r, h, cause)
		if err != nil {
			g.recordPendingError(fmt.Errorf("engine: %q: Event$ Moved Discard$: %w", h.Def.Name, err))
			return false
		}
		if !applies || !replacementRequirementsCheck(g, h, amounts, r) {
			return false
		}
		lines = append(lines, discardLine{host: h, amounts: amounts, rule: r})
		return false
	}
	// The discarded card's own lines, live in its hand (ActiveZones$ Hand:
	// Madness, Loxodon Smiter), which eachReplacement's zones do not cover;
	// then every battlefield and command-zone host's (Library of Leng).
	if c.Def != nil {
		for _, face := range c.liveTraitFaces() {
			for _, r := range face.Replacements {
				if strings.EqualFold(r.Name, "Moved") && hostInActiveZones(c, r, c.Zone) {
					consider(c, face.Amounts, r)
				}
			}
		}
	}
	g.eachReplacement("Moved", consider)
	if len(lines) == 0 {
		return false
	}
	line := lines[0]
	if len(lines) > 1 {
		g.recordPendingError(fmt.Errorf("engine: %q: two discard replacements apply to one discard: CR 616.1 order not resolvable yet", c.Def.Name))
		return false
	}
	if !madnessExileShape(line.rule) {
		g.recordPendingError(fmt.Errorf("engine: %q: Event$ Moved Discard$ shape not resolvable yet", line.host.Def.Name))
		return false
	}
	if dest, _ := g.movedGraveyardDestination(id); dest != Graveyard {
		g.recordPendingError(fmt.Errorf("engine: %q: a discard replacement and a graveyard replacement both apply: CR 616.1 order not resolvable yet", c.Def.Name))
		return false
	}
	g.Move(id, Exile, c.Owner)
	g.checkMadnessTriggers(controller, id, line.rule.Keyword)
	return true
}

// discardCauseMatches is ReplaceMoved's EffectOnly$ and ValidCause$ for a
// discard: EffectOnly$ admits only a discard an effect made (:85-90), and the
// one ValidCause$ the corpus writes on these lines, SpellAbility.OppCtrl,
// only one an opponent of the host's controller's ability made. An unknown
// cause, or another ValidCause$, is an error.
func discardCauseMatches(r *compile.Ability, h *Card, cause discardCause) (bool, error) {
	_, effectOnly := r.Param("EffectOnly")
	validCause, hasCause := r.Param("ValidCause")
	if !effectOnly && !hasCause {
		return true, nil
	}
	if !cause.known {
		return false, fmt.Errorf("the discard's cause is not known here")
	}
	if effectOnly && !cause.effect {
		return false, nil
	}
	if hasCause {
		if validCause != "SpellAbility.OppCtrl" {
			return false, fmt.Errorf("ValidCause$ %q not resolvable yet", validCause)
		}
		if !cause.effect || cause.controller == NoPlayer || cause.controller == h.Controller() {
			return false, nil
		}
	}
	return true, nil
}

// madnessExileShape reports whether r is Madness's replacement as
// keyword.Expand writes it: ActiveZones$ Hand, ValidCard$ Card.Self,
// Discard$ True, ReplaceWith$ a ChangeZone of Defined$ ReplacedCard to Exile.
func madnessExileShape(r *compile.Ability) bool {
	if !onlyParams(r, "validcard", "discard") {
		return false
	}
	sub := replaceWithSub(r)
	if sub == nil || !strings.EqualFold(sub.Name, "ChangeZone") || !onlyKeys(sub, "DB", "Hidden", "Origin", "Destination", "Defined") {
		return false
	}
	to, _ := sub.Param("Destination")
	defined, _ := sub.Param("Defined")
	return strings.EqualFold(to, "Exile") && defined == "ReplacedCard"
}
