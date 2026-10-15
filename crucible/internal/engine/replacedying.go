// ReplaceDyingDefined$: "if that creature would die this turn, exile it
// instead" -- an effect card in the activator's Command zone carrying an
// Event$ Moved replacement for the cards it remembers, until end of turn.
// DealDamage, DamageAll, DamageResolve, Fight and Pump install it after
// their own resolution.
//
// Ported from forge-game/src/main/java/forge/game/ability/SpellAbilityEffect.java's
// replaceDying (:691-756), createEffect (:653-689) and addForgetOnMovedTrigger
// (:548-560); AbilityUtils.getDefinedCards' ".<valid>" filter
// (AbilityUtils.java:68, :319-326).

package engine

//enginelint:allow id zone card game ability defined condition valid effecteffect effecthelpers

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// replaceDyingUnresolvedParams are replaceDying's params this port does not
// build, each refused by name before the effect acts (GO-7):
// ReplaceDyingValid$ and ReplaceDyingZone$ have no corpus line.
var replaceDyingUnresolvedParams = [...]string{"ReplaceDyingValid", "ReplaceDyingZone"}

// checkReplaceDying refuses, before the effect acts, a ReplaceDying shape
// replaceDying cannot build: an unported param, a ReplaceDyingCondition$
// other than Kicked (SpellAbilityEffect.java:693-701 reads Kicked alone; any
// other value would install the effect unconditionally, a guess), or a
// ReplaceDyingDefined$ spelling definedCards does not read.
func checkReplaceDying(g *Game, a *Ability, api string) error {
	if err := rejectParams(a, api, replaceDyingUnresolvedParams[:]...); err != nil {
		return err
	}
	spec, ok := a.Params.Param("ReplaceDyingDefined")
	if !ok {
		return nil
	}
	if cond, ok := a.Params.Param("ReplaceDyingCondition"); ok && cond != "Kicked" {
		return fmt.Errorf("engine: %s: ReplaceDyingCondition$ %q not resolvable yet", api, cond)
	}
	base, _, _ := strings.Cut(spec, ".")
	if _, err := definedCards(g.Card(a.Source), base, a.refs()); err != nil {
		return fmt.Errorf("engine: %s: ReplaceDyingDefined$: %w", api, err)
	}
	return nil
}

// replaceDying is SpellAbilityEffect.replaceDying (:691-756): the cards
// ReplaceDyingDefined$ names now -- definedCards on the part before the first
// ".", the rest a valid filter each card must match (Remembered.Creature,
// ThisTargetedCard.Creature) -- are remembered by a new effect card whose
// Moved replacement sends any of them going from the battlefield to a
// graveyard to exile instead. No card, no effect (:712-715);
// ReplaceDyingCondition$ Kicked creates none unless the spell was kicked.
//
// The effect ends at the next cleanup step (game.getEndOfTurn().addUntil,
// effectUntilEndOfTurn) and forgets a remembered card leaving the battlefield
// (addForgetOnMovedTrigger(eff, "Battlefield"), effectCardsSeeMove), exiling
// itself once it remembers none. checkReplaceDying must have passed.
func (g *Game) replaceDying(a *Ability, api string) error {
	spec, ok := a.Params.Param("ReplaceDyingDefined")
	if !ok {
		return nil
	}
	source := g.Card(a.Source)
	if _, ok := a.Params.Param("ReplaceDyingCondition"); ok && !kickedConditionMet(source, "Kicked") {
		return nil
	}
	base, filter, filtered := strings.Cut(spec, ".")
	cards, err := definedCards(source, base, a.refs())
	if err != nil {
		return fmt.Errorf("engine: %s: ReplaceDyingDefined$: %w", api, err)
	}
	if filtered && len(cards) > 0 {
		cards = filterDefinedCards(g, a, cards, filter)
	}
	if len(cards) == 0 {
		return nil
	}
	def := replaceDyingDef(effectHostName(source)+"'s Effect", a)
	id := g.NewCard(def, a.Controller, Command)
	eff := g.Card(id)
	eff.IsEffect = true
	eff.effectLife = effectLifetime{
		duration:      effectUntilEndOfTurn,
		player:        a.Controller,
		host:          a.Source,
		forgetOnMoved: zoneMask(1) << Battlefield,
	}
	for _, c := range cards {
		eff.Memory.Remember(CardEntity(c))
	}
	return nil
}

// filterDefinedCards is getDefinedCards' trailing filter (AbilityUtils.java
// :319-326): each comma alternative of filter prefixed with "Card.", matched
// for the ability's controller and host.
func filterDefinedCards(g *Game, a *Ability, cards []CardID, filter string) []CardID {
	alts := strings.Split(filter, ",")
	for i, alt := range alts {
		alts[i] = "Card." + alt
	}
	spec := valid.Parse(strings.Join(alts, ","))
	var out []CardID
	for _, id := range cards {
		if Matches(g, g.Card(id), spec, a.Controller, a.Source) {
			out = append(out, id)
		}
	}
	return out
}

// replaceDyingDef is the effect card replaceDying creates: named name, with
// the ability's SVars (createEffect's eff.setSVars(sa.getSVars())) and the one
// replacement Java parses from strings at :728-740. This port builds the same
// tree directly, as monarchEffectDef does, so nothing is parsed at runtime
// (PORT-2):
//
//	Event$ Moved | ValidLKI$ Card.IsRemembered | Origin$ Battlefield | Destination$ Graveyard
//	  -> DB$ ChangeZone | Defined$ ReplacedCard | Origin$ Battlefield | Destination$ Exile
//
// Java's re.setLayer(ReplacementLayer.Other) is not a param; the Moved
// dispatch reads no layer for this event. ReplaceDyingExiledWith$ adds
// ExiledWithEffectSource$ True to the ChangeZone (:732-734): the exiled card
// is exiled with the card whose ability made the effect, not with the effect
// (Gut, Fanatical Priestess, whose specialized faces copy "a creature card
// exiled with this creature"); the Moved dispatch reads it
// (movedGraveyardDestination).
func replaceDyingDef(name string, a *Ability) *compile.Card {
	exile := &compile.Ability{Record: compile.SubAbility, Name: "ChangeZone", Params: []vocab.Param{
		{Key: "DB", Value: "ChangeZone"}, {Key: "Defined", Value: "ReplacedCard"},
		{Key: "Origin", Value: "Battlefield"}, {Key: "Destination", Value: "Exile"},
	}}
	if hasParam(a, "ReplaceDyingExiledWith") {
		exile.Params = append(exile.Params, vocab.Param{Key: "ExiledWithEffectSource", Value: "True"})
	}
	moved := &compile.Ability{Record: compile.Replacement, Name: "Moved", Params: []vocab.Param{
		{Key: "Event", Value: "Moved"}, {Key: "ValidLKI", Value: "Card.IsRemembered"},
		{Key: "Origin", Value: "Battlefield"}, {Key: "Destination", Value: "Graveyard"},
		{Key: "Description", Value: "If that permanent would die this turn, exile it instead."},
	}, Subs: []compile.SubRef{{Key: "ReplaceWith", Ability: exile}}}
	def := &compile.Card{Name: name}
	face := &def.Faces[0]
	face.Name = name
	face.Amounts = a.Amounts
	face.Replacements = []*compile.Ability{moved}
	return def
}
