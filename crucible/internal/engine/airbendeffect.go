package engine

//enginelint:allow ability card condition control defined earthbendeffect effecthelpers game id player zone zonemove

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// ExilePlayGrant is one "you may cast it from exile" permission a resolving
// effect gave a player for one card -- the Mode$ Continuous | MayPlay$ True
// static on the command-zone effect card AirbendEffect.java and
// HeistEffect.java create, with that effect's addForgetOnMovedTrigger and
// addForgetOnCastTrigger folded into Timestamp: the card's zone-change
// timestamp as it was exiled, so any later zone change (being cast
// included) ends the grant.
//
// CastSpell (castspell.go) casts from outside the hand only under a Layer 8
// MayPlay$ grant (mayPlayOption, game.go), and does not read these: using
// one needs its alternative {2} cost (AltGeneric) and any-type mana
// (AnyManaType), which castOpts cannot express yet. MayPlayFromExile is
// the query that path will ask.
type ExilePlayGrant struct {
	Card      CardID
	Timestamp uint64
	Player    PlayerID
	// AltGeneric is MayPlayAltManaCost$: the generic cost paid instead of
	// the card's mana cost, when HasAltCost.
	AltGeneric int
	HasAltCost bool
	// AnyManaType is MayPlayIgnoreType$: mana of any type can be spent.
	AnyManaType bool
	// NonLand is Affected$ Card.IsRemembered+nonLand: a land card is not
	// covered.
	NonLand bool
}

// MayPlayFromExile answers p's live grant for card, if any: card is still
// in exile with the timestamp it was exiled with, and a NonLand grant does
// not cover a land.
func (g *Game) MayPlayFromExile(p PlayerID, card CardID) (ExilePlayGrant, bool) {
	c := g.Card(card)
	for _, gr := range g.exileGrants {
		if gr.Card != card || gr.Player != p || c.Zone != Exile || c.Timestamp != gr.Timestamp {
			continue
		}
		if gr.NonLand && c.Type().Has(cardtype.Land) {
			continue
		}
		return gr, true
	}
	return ExilePlayGrant{}, false
}

// airbendEffect is AirbendEffect.java (CR 701.65): each target or Defined$
// card is exiled, and while it stays exiled its owner may cast it for {2}
// rather than its mana cost (ExilePlayGrant). A token gets no grant.
// ChangesZoneAll fires once for the batch, then, when anything moved, the
// activator's ElementalBend trigger (CR 701.65b, Player.triggerElementalBend).
//
// Only battlefield cards are exiled: every real line targets a permanent,
// except the one reaching the stack through TgtZone$ (a spell target), which
// fails closed, as does Airbend of a card already gone (Java's
// equalsWithGameTimestamp skip).
type airbendEffect struct{}

func (airbendEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "Airbend", "TgtZone", "Condition", "ConditionDefined"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Airbend: %w", err)
	}
	var moved []CardID
	for _, id := range cards {
		c := g.Card(id)
		if c.Zone != Battlefield {
			continue
		}
		melded := g.moveByEffect(controller, id, Exile, 0, NoPlayer, false)
		g.markExiledWith(id, a.Source)
		if melded != NoCard {
			g.markExiledWith(melded, a.Source)
		}
		if c.Zone != Exile {
			continue
		}
		moved = append(moved, id)
		if c.IsToken {
			continue
		}
		g.exileGrants = append(g.exileGrants, ExilePlayGrant{
			Card: id, Timestamp: c.Timestamp, Player: c.Owner,
			AltGeneric: 2, HasAltCost: true, NonLand: true,
		})
	}
	g.checkChangesZoneAllTriggers(controller, moved, Battlefield, Exile)
	if len(moved) > 0 {
		g.checkElementalBendTriggers(controller, a.Controller, "Airbend")
	}
	return nil
}
