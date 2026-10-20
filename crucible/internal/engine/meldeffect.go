package engine

//enginelint:allow id card game zone ability condition control effecthelpers zonemove valid changecombatantseffect combat exile phase

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// meldEffect is MeldEffect.java (CR 712, ADR-0032): the ability's host
// (Primary$) and a permanent its activator both controls and owns, named
// Secondary$ and of type SecondaryType$ (default Creature), are exiled; if
// both are then in exile, still named Primary$ and Secondary$, and neither is
// a token, they meld: the host returns to the battlefield under the
// activator's control as its meld face (the card's own ALTERNATE face), tapped
// with Tapped$ and attacking with Attacking$ True, representing both cards.
// The other card goes to no zone's card list (PlayerZoneBattlefield.
// addToMelded); leaving the battlefield splits them apart again (Game.unmeld,
// CR 712.4c).
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/
// MeldEffect.java's resolve. Name$ is the meld face's name for the ability's
// description only: nothing in MeldEffect.java or MeldAi.java reads it.
type meldEffect struct{}

func (meldEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "Meld", "Blocking"); err != nil {
		return err
	}
	if attacking, ok := a.Params.Param("Attacking"); ok && attacking != "True" {
		return fmt.Errorf("engine: Meld: Attacking$ %q not resolvable yet", attacking)
	}
	primName, ok := a.Params.Param("Primary")
	if !ok {
		return fmt.Errorf("engine: Meld: no Primary$")
	}
	secName, ok := a.Params.Param("Secondary")
	if !ok {
		return fmt.Errorf("engine: Meld: no Secondary$")
	}
	if battlefieldStaticMode(g, "CantExile") {
		return fmt.Errorf("engine: Meld: CantExile statics not resolvable yet")
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	// Every corpus Meld gates on its host being on the battlefield: an
	// ability's ConditionPresent$ (checked above) or its trigger's
	// IsPresent$, which Java checks again as the trigger resolves (CR
	// 603.4). This port does not recheck a trigger's condition, so a host
	// that left in response would be exiled from wherever it went and melded.
	if source.Zone != Battlefield {
		return fmt.Errorf("engine: Meld: host off the battlefield (no CR 603.4 recheck) not resolvable yet")
	}
	secType, ok := a.Params.Param("SecondaryType")
	if !ok {
		secType = "Creature"
	}
	var field []CardID
	for _, id := range g.Zone(Battlefield, a.Controller).Cards() {
		c := g.Card(id)
		if c.Owner == a.Controller && sharesName(c, secName) && c.Type().HasStringType(secType) {
			field = append(field, id)
		}
	}
	if len(field) == 0 {
		return nil
	}
	picked := controller.ChooseCardsForEffect(g, a.Controller, a.Source, field, 1, 1)
	if err := checkChoice(picked, field, 1, 1); err != nil {
		return fmt.Errorf("engine: Meld: %w", err)
	}
	primary, secondary := a.Source, picked[0]

	// GameAction.exile on both, the host first: each exile fires its own
	// leave-the-battlefield triggers, the pair its ChangesZoneAll triggers.
	exileCards(g, controller, []CardID{primary, secondary})
	// GameAction.java:424-429: a permanent leaving the battlefield leaves
	// combat. Game.Move does not (combat.go's removeFromCombat), and an
	// attacking card melded away must not deal combat damage as though it
	// were still a permanent.
	g.removeFromCombat(primary)
	g.removeFromCombat(secondary)

	p, s := g.Card(primary), g.Card(secondary)
	if p.Zone != Exile || s.Zone != Exile {
		return nil
	}
	// The cards have their own names in exile (a copy effect ended as each
	// left the battlefield), and neither may be a token.
	if !sharesName(p, primName) || !sharesName(s, secName) || p.IsToken || s.IsToken {
		return nil
	}
	front := p.Def
	if front == nil || front.SplitType != carddb.SplitMeld {
		return fmt.Errorf("engine: Meld: %q has no meld face", primName)
	}

	// Card.changeToState(CardStateName.Meld): the meld face, the front face
	// kept for Game.Move's turnFrontFaceUp -- setstateeffect.go's transform
	// precedent.
	back := &compile.Card{Filename: front.Filename, Name: front.Faces[carddb.FaceAlternate].Name, SplitType: front.SplitType}
	back.Faces[0] = front.Faces[carddb.FaceAlternate]
	p.frontDef, p.Def = front, back
	p.MeldedWith = secondary

	// PlayerZoneBattlefield.addToMelded: the other card leaves exile for the
	// activator's battlefield, but joins none of its cards.
	g.Zone(Exile, s.ZoneOwner).remove(secondary)
	s.Zone, s.ZoneOwner = Battlefield, a.Controller
	s.Melded = true

	g.moveByEffect(controller, primary, Battlefield, 0, a.Controller, hasParam(a, "Tapped"))
	if !hasParam(a, "Attacking") {
		return nil
	}
	// SpellAbilityEffect.addToCombat (SpellAbilityEffect.java:758-793).
	if !g.activePhase.IsCombat() || p.Zone != Battlefield || !p.Type().Has(cardtype.Creature) ||
		p.Controller() != g.activePlayer {
		return nil
	}
	if err := g.attackByEffect(controller, a.Controller, primary); err != nil {
		return fmt.Errorf("engine: Meld: %w", err)
	}
	return nil
}
