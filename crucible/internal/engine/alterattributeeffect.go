package engine

//enginelint:allow game ability control effecthelpers card condition defined parts id animate

import (
	"fmt"
	"strings"
)

// alterAttributeEffect is AlterAttributeEffect.java: each Defined$ or
// targeted card gains (Activate$ True, the default) or loses each of the
// comma-separated Attributes$ -- Suspected (CR 701.60: menace and "can't
// block" while suspected, never suspected twice), Solved, Harnessed and
// Plotted, each a flag on the card, and Prepared (prepare.go: an exiled copy
// of its prepare spell its controller may cast; never "altered", so never
// remembered, AlterAttributeEffect.java:65-90). Optional$ lets the activator
// decline first. Saddled (reads the saddle cost's tapped creatures) and
// Commander are not resolved, nor is Suspected while a CantBeSuspected
// static is out.
type alterAttributeEffect struct{}

func (alterAttributeEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "AlterAttribute", "IncludeAllComponentCards"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	raw, _ := a.Params.Param("Attributes")
	attrs := strings.Split(raw, ",")
	for i, attr := range attrs {
		attr = strings.TrimSpace(attr)
		switch attr {
		case "Suspect", "Suspected", "Solve", "Solved", "Harnessed", "Plotted", "Prepared":
		default:
			return fmt.Errorf("engine: AlterAttribute: attribute %q not resolvable yet", attr)
		}
		attrs[i] = attr
	}
	activate := true
	if v, ok := a.Params.Param("Activate"); ok {
		activate = strings.EqualFold(v, "true")
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: AlterAttribute: %w", err)
	}
	if hasParam(a, "Optional") && !controller.ConfirmEffect(g, a.Controller, a.Source) {
		return nil
	}
	_, remember := a.Params.Param("RememberAltered")
	for _, id := range cards {
		for _, attr := range attrs {
			// Re-read: prepare's NewCard may have grown the arena.
			c := g.Card(id)
			altered := true
			switch attr {
			case "Prepared":
				altered = false
				if activate {
					g.prepare(id, a.Controller)
				} else {
					g.unprepare(id)
				}
			case "Suspect", "Suspected":
				if activate && battlefieldStaticNames(g, "CantBeSuspected") {
					return fmt.Errorf("engine: AlterAttribute: CantBeSuspected statics not resolvable yet")
				}
				g.setSuspected(id, activate)
			case "Solve", "Solved":
				c.Solved = activate
			case "Harnessed":
				c.Harnessed = activate
			case "Plotted":
				c.Plotted = activate
			}
			if altered && remember {
				g.Card(a.Source).Memory.Remember(CardEntity(id))
			}
		}
	}
	return nil
}

// setSuspected is Card.setSuspected: becoming suspected adds menace at a
// fresh timestamp (the suspected static's Layer 6 grant) and "can't block"
// (CanBlock reads Suspected); a card already suspected stays as it is
// (CR 701.60d). Losing it removes both.
func (g *Game) setSuspected(id CardID, on bool) {
	c := g.Card(id)
	if on {
		if c.Suspected {
			return
		}
		c.Suspected = true
		g.timestamp++
		c.suspectedTS = g.timestamp
		g.addAnimate(animateRecord{Card: id, Timestamp: c.suspectedTS, Permanent: true, AddKeywords: []string{"Menace"}})
		return
	}
	if !c.Suspected {
		return
	}
	c.Suspected = false
	kept := g.animates[:0]
	for _, r := range g.animates {
		if r.Card != id || r.Timestamp != c.suspectedTS {
			kept = append(kept, r)
		}
	}
	g.animates = kept
}
