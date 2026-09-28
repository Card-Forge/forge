package engine

//enginelint:allow ability animate card condition control delayedtrigger effecthelpers event game id parts trigger valid zone

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// earthbendEffect is EarthbendEffect.java: the target land the activator
// controls becomes a 0/0 creature with haste that is still a land, gets Num$
// (default 1) +1/+1 counters, and gains two delayed triggers -- when it dies
// or is exiled, return it to the battlefield tapped under the activator's
// control. Then the activator's ElementalBend trigger runs.
//
// Java builds the target restriction itself (buildSpellAbility,
// Land.YouCtrl): no script line names ValidTgts$, so resolveTargets supplies
// it (resolveTargets, targeting.go). The animation has no duration, the
// Permanent animateRecord. A target no longer a land the activator controls
// is illegal on resolution (CR 608.2b) and the ability does nothing.
type earthbendEffect struct{}

func (earthbendEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "Earthbend", "Condition", "ConditionDefined"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	num, err := optionalAmount(g, a, "Earthbend", "Num", 1)
	if err != nil {
		return err
	}
	var lands []CardID
	for _, e := range a.Targets {
		id, ok := e.AsCard()
		if !ok {
			continue
		}
		c := g.Card(id)
		if c.Zone == Battlefield && c.Controller() == a.Controller && c.Type().Has(cardtype.Land) {
			lands = append(lands, id)
		}
	}
	if len(lands) == 0 {
		return nil
	}
	for _, id := range lands {
		g.timestamp++
		g.addAnimate(animateRecord{
			Card: id, Timestamp: g.timestamp, Permanent: true,
			Types:       TypeEffect{AddTypes: cardtype.ParseToken("Creature")},
			AddKeywords: []string{"Haste"},
			HasPower:    true, HasToughness: true,
		})
		g.Card(id).Counters.Add(P1P1, num)
		emitCounterChanged(g.sink, a.Source, CardEntity(id), P1P1, num)
		for _, zone := range [...]ZoneType{Graveyard, Exile} {
			g.delayed = append(g.delayed, delayedTrigger{
				Trigger: earthbendReturnTrigger(zone), Host: a.Source, Controller: a.Controller,
				Amounts: a.Amounts, Remembered: []EntityID{CardEntity(id)},
			})
		}
	}
	g.checkElementalBendTriggers(controller, a.Controller, "Earthbend")
	return nil
}

// earthbendReturnTrigger is EarthbendEffect.buildTrigger's trigger for zone:
// Mode$ ChangesZone to the graveyard or Mode$ Exiled, from the battlefield,
// of the remembered card, executing "DB$ ChangeZone | Defined$
// DelayTriggerRemembered | Origin$ <zone> | Destination$ Battlefield |
// Tapped$ True | GainControl$ You". Java parses both strings; this port
// builds the same tree directly, once per registration.
func earthbendReturnTrigger(zone ZoneType) *compile.Ability {
	origin := "Graveyard"
	trig := &compile.Ability{Name: "ChangesZone", Params: []vocab.Param{
		{Key: "Mode", Value: "ChangesZone"}, {Key: "ValidCard", Value: "Card.IsTriggerRemembered"},
		{Key: "Origin", Value: "Battlefield"}, {Key: "Destination", Value: "Graveyard"},
	}}
	if zone == Exile {
		origin = "Exile"
		trig = &compile.Ability{Name: "Exiled", Params: []vocab.Param{
			{Key: "Mode", Value: "Exiled"}, {Key: "Origin", Value: "Battlefield"},
			{Key: "ValidCard", Value: "Card.IsTriggerRemembered"},
		}}
	}
	exec := &compile.Ability{Name: "ChangeZone", Params: []vocab.Param{
		{Key: "Defined", Value: "DelayTriggerRemembered"}, {Key: "Origin", Value: origin},
		{Key: "Destination", Value: "Battlefield"}, {Key: "Tapped", Value: "True"}, {Key: "GainControl", Value: "You"},
	}}
	trig.Subs = []compile.SubRef{{Key: "Execute", Ability: exec}}
	return trig
}

// checkElementalBendTriggers is Player.triggerElementalBend: bender's
// Mode$ ElementalBend triggers, then Mode$ <kind> ones. Java also records
// kind in elementalBendThisTurn for hasAllElementBend ("all four this
// turn"); Firebend and Waterbend are not ported, so four is unreachable here
// and the set is not kept.
func (g *Game) checkElementalBendTriggers(controller PlayerController, bender PlayerID, kind string) {
	g.checkPlayerActionTriggers(controller, bender, "ElementalBend", kind)
}

// checkPlayerActionTriggers runs every trigger of the given modes whose only
// test is ValidPlayer$ against actor (TriggerElementalbend,
// TriggerDiscover's player half). ActivationLimit$/ResolvedLimit$ lines are
// skipped: no per-trigger counter enforces them (checkLifeGainedTriggers'
// reasoning, trigger.go).
func (g *Game) checkPlayerActionTriggers(controller PlayerController, actor PlayerID, modes ...string) {
	g.pushTriggeredAbilities(controller, g.playerActionTriggerMatches(actor, nil, modes...))
}

// playerActionTriggerMatches is checkPlayerActionTriggers' walk, returning
// the matches instead of pushing them. gate, when not nil, is a further
// mode-specific test a line must pass (Mode$ BecomeMonarch's BeginTurn$).
func (g *Game) playerActionTriggerMatches(actor PlayerID, gate func(h *Card, t *compile.Ability) bool, modes ...string) []Ability {
	var matches []Ability
	for _, mode := range modes {
		for _, pid := range g.Players() {
			for _, z := range phaseTriggerZones {
				for _, host := range g.Zone(z, pid).Cards() {
					h := g.Card(host)
					if h.Def == nil {
						continue
					}
					for face := range h.triggerFaces {
						for _, t := range face.Triggers {
							if !strings.EqualFold(t.Name, mode) || !phaseTriggerZoneMatches(h, t, z) {
								continue
							}
							if hasAnyParam(t, "ActivationLimit", "ResolvedLimit") {
								continue
							}
							if vp, ok := t.Param("ValidPlayer"); ok {
								matched, recognized := matchesPlayerSpec(g, actor, h.Controller(), host, vp)
								if !recognized || !matched {
									continue
								}
							}
							if gate != nil && !gate(h, t) {
								continue
							}
							if sub, api, optional, ok := triggerEffectAPI(g, h, face.Amounts, t); ok {
								matches = append(matches, Ability{API: api, Source: host, Controller: h.Controller(), Params: sub, Amounts: face.Amounts, Optional: optional, triggered: face.objects(triggeredObjects{})})
							}
						}
					}
				}
			}
		}
	}
	return matches
}
