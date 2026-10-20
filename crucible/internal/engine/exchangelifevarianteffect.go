package engine

//enginelint:allow ability control game effecthelpers card condition defined zone player setlifeeffect animate id

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// exchangeLifeVariantEffect is LifeExchangeVariantEffect.java: the host,
// only while it is a creature on the battlefield, exchanges its Mode$
// Power or Toughness with the first target (or Defined$) player's life
// total. The player loses or gains the difference -- skipped entirely when
// the gain would be prevented (canGainLife) -- and the host's power or
// toughness is set to that player's old life total, a Layer 7b change with
// no end (addNewPT with no until-command).
type exchangeLifeVariantEffect struct{}

func (exchangeLifeVariantEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "ExchangeLifeVariant"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	mode, _ := a.Params.Param("Mode")
	if mode != "Power" && mode != "Toughness" {
		return fmt.Errorf("engine: ExchangeLifeVariant: Mode$ %q not resolvable", mode)
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ExchangeLifeVariant: %w", err)
	}
	if len(players) == 0 || source.Zone != Battlefield || !source.Type().Has(cardtype.Creature) {
		return nil
	}
	p := players[0]
	life := g.Player(p).Life
	num, ok := source.Power()
	if mode == "Toughness" {
		num, ok = source.Toughness()
	}
	if !ok {
		return fmt.Errorf("engine: ExchangeLifeVariant: host %s is not resolvable", mode)
	}
	if num > life && g.gainLifePrevented(p) {
		return nil
	}
	setPlayerLife(g, controller, a.Source, p, num)
	r := animateRecord{Permanent: true}
	if mode == "Power" {
		r.Power, r.HasPower = life, true
	} else {
		r.Toughness, r.HasToughness = life, true
	}
	return g.animateCards(a, r, []CardID{a.Source})
}
