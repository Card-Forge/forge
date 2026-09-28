// AdvanceCrank: Unfinity's Contraption "CRANK!" counter (CR 725, the
// Contraption subtype's own rules) -- clock_of_doooooooooooom.txt's own
// "AB$ AdvanceCrank | Cost$ 4 T" is the one real corpus line.

package engine

//enginelint:allow ability card control defined effecthelpers game id parts player trigger valid zone

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// advanceCrankEffect is AdvanceCrankEffect.java: for each Defined$-or-
// targeted player (default the activator, getDefinedPlayersOrTargeted),
// advance that player's own CRANK! counter to the next sprocket (1, 2 or 3,
// wrapping) and let them choose any number of that sprocket's Contraptions
// to crank.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/
// AdvanceCrankEffect.java's resolve, and Player.java:4014-4023's own
// advanceCrankCounter. Not ported: Player.setCrankCounter's own
// contraptionSprocketEffect (Player.java:4004-4038), a Command-zone "sprocket
// dial" display card with no rules effect of its own -- an overlay-text-only
// UI artifact this port has no reason to model.
type advanceCrankEffect struct{}

func (advanceCrankEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return err
	}
	for _, pid := range players {
		g.advanceCrankCounter(controller, pid, a.Source)
	}
	return nil
}

// advanceCrankCounter is Player.advanceCrankCounter, ported: move p's own
// CRANK! counter to the next sprocket (1, 2, 3, then back to 1 --
// crankCounter % 3 + 1, an exact port of Java's own modular arithmetic), find
// every Contraption p controls dialed to that sprocket
// (CardPredicates.isContraptionOnSprocket -- Card.Sprocket is 0 for every
// non-Contraption and every Contraption not yet assembled, so a bare
// Sprocket-equality scan already excludes them), let p's controller choose
// any number of them (getController().chooseContraptionsToCrank ->
// ChooseCardsForEffect's own existing lo 0/hi len(options) shape, no new
// PlayerController method needed), then fire CR's own Mode$ CrankContraption
// trigger once per chosen Contraption.
func (g *Game) advanceCrankCounter(controller PlayerController, pid PlayerID, source CardID) {
	p := g.Player(pid)
	p.CrankCounter = p.CrankCounter%3 + 1
	sprocket := p.CrankCounter

	var contraptions []CardID
	for _, ownerID := range g.Players() {
		for _, id := range g.Zone(Battlefield, ownerID).Cards() {
			card := g.Card(id)
			if card.Controller() == pid && card.Sprocket == sprocket {
				contraptions = append(contraptions, id)
			}
		}
	}
	if len(contraptions) == 0 {
		return
	}
	// source is the AdvanceCrank ability's own host (e.g. Clock of
	// DOOOOOOOOOOOOM!) rather than a synthetic "Contraption Sprockets"
	// display card (Player.java:4024-4038, not built here -- this file's
	// own doc comment): ChooseCardsForEffect only reports it back to the
	// controller for context, and every real corpus AdvanceCrank$ line has
	// exactly one host to name.
	chosen := controller.ChooseCardsForEffect(g, pid, source, contraptions, 0, len(contraptions))
	for _, id := range chosen {
		g.checkCrankContraptionTriggers(controller, id)
	}
}

// isCrankContraptionTrigger reports whether t is CR's own Mode$
// CrankContraption shape (TriggerCrankContraption.java). Every real corpus
// T:Mode$ CrankContraption line names only ValidCard$ Card.Self, so no
// further param is read here.
func isCrankContraptionTrigger(t *compile.Ability) bool {
	return strings.EqualFold(t.Name, "CrankContraption")
}

// checkCrankContraptionTriggers fires CR's own "whenever you crank
// CARDNAME" trigger for cranked -- checkExertedTriggers' own exact
// structural sibling (exertcost.go): a single unified battlefield walk,
// though every real corpus line is ValidCard$ Card.Self so only cranked's
// own ability ever actually matches.
func (g *Game) checkCrankContraptionTriggers(controller PlayerController, cranked CardID) {
	var matches []Ability
	c := g.Card(cranked)
	for _, pid := range g.Players() {
		for _, host := range g.Zone(Battlefield, pid).Cards() {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for face := range h.triggerFaces {
				for _, t := range face.Triggers {
					if !isCrankContraptionTrigger(t) {
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
