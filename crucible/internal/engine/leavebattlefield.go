// LeaveBattlefield$ (SpellAbilityEffect.addLeaveBattlefieldReplacement,
// SpellAbilityEffect.java:618-650): "if it would leave the battlefield, exile it
// instead of putting it anywhere else", for the card an Animate, Pump or
// ChangeZone just changed. Java makes an effect card in the Command zone that
// remembers the card and carries an Event$ Moved replacement for it, and has it
// forget the card once it leaves the battlefield (addExileOnMovedTrigger); this
// builds the same tree directly (PORT-2). The replacement is read by
// movedDestination.

//enginelint:allow id zone card game ability effecteffect effecthelpers replacedying

package engine

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
)

// leaveBattlefieldZone reads LeaveBattlefield$: the zone the card goes to
// instead. Only Exile has a corpus line; another zone is refused (GO-7).
func leaveBattlefieldZone(a *Ability, api string) (zone string, has bool, err error) {
	zone, has = a.Params.Param("LeaveBattlefield")
	if has && zone != "Exile" {
		return "", true, fmt.Errorf("engine: %s: LeaveBattlefield$ %q not resolvable yet", api, zone)
	}
	return zone, has, nil
}

// addLeaveBattlefieldReplacement installs the effect card for card, which must
// be on the battlefield; a card that has left already has nothing to replace.
func (g *Game) addLeaveBattlefieldReplacement(a *Ability, card CardID, zone string) {
	if g.Card(card).Zone != Battlefield {
		return
	}
	source := g.Card(a.Source)
	name := effectHostName(source) + "'s Effect"
	send := &compile.Ability{Record: compile.SubAbility, Name: "ChangeZone", Params: []vocab.Param{
		{Key: "DB", Value: "ChangeZone"}, {Key: "Defined", Value: "ReplacedCard"},
		{Key: "Origin", Value: "Battlefield"}, {Key: "Destination", Value: zone},
	}}
	moved := &compile.Ability{Record: compile.Replacement, Name: "Moved", Params: []vocab.Param{
		{Key: "Event", Value: "Moved"}, {Key: "ValidCard", Value: "Card.IsRemembered"},
		{Key: "Origin", Value: "Battlefield"}, {Key: "ExcludeDestination", Value: zone},
		{Key: "Description", Value: "If it would leave the battlefield, exile it instead of putting it anywhere else."},
	}, Subs: []compile.SubRef{{Key: "ReplaceWith", Ability: send}}}
	def := &compile.Card{Name: name}
	face := &def.Faces[0]
	face.Name = name
	face.Amounts = a.Amounts
	face.Replacements = []*compile.Ability{moved}
	id := g.NewCard(def, a.Controller, Command)
	eff := g.Card(id)
	eff.IsEffect = true
	eff.effectLife = effectLifetime{duration: effectPermanent, player: a.Controller, host: a.Source, forgetOnMoved: zoneMask(1) << Battlefield}
	eff.Memory.Remember(CardEntity(card))
}

// leaveBattlefieldFor installs LeaveBattlefield$'s replacement for each of
// cards still on the battlefield, nothing for a line without the param.
func (g *Game) leaveBattlefieldFor(a *Ability, api string, cards []CardID) error {
	zone, has, err := leaveBattlefieldZone(a, api)
	if err != nil || !has {
		return err
	}
	for _, id := range cards {
		g.addLeaveBattlefieldReplacement(a, id, zone)
	}
	return nil
}
