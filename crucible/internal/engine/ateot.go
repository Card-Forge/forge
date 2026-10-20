package engine

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
)

// registerAtEOT is SpellAbilityEffect.registerDelayedTrigger (:403-:480), the
// AtEOT$ of Token, CopyPermanent, Animate and their kin: one delayed trigger
// remembering cards, firing at the next end step ("Your" prefix: only your
// own, ValidPlayer$ You), the end of combat ("Combat" suffix) or the next
// upkeep ("Upkeep" suffix), that moves or destroys what it remembered. Java
// builds it from strings; this port builds the same tree (PORT-2):
//
//	Mode$ Phase | Phase$ End Of Turn | ValidPlayer$ You (Your only)
//	  Execute: DB$ ChangeZone | Defined$ DelayTriggerRememberedLKI | ...
//
// AtEOTCondition$ (one real line) fails closed, as does a location Java has
// no trigger body for.
func registerAtEOT(g *Game, a *Ability, cards []CardID) error {
	location, ok := a.Params.Param("AtEOT")
	if !ok || len(cards) == 0 {
		return nil
	}
	if hasParam(a, "AtEOTCondition") {
		return fmt.Errorf("engine: %s: AtEOTCondition$ not resolvable yet", a.Params.Name)
	}
	your := strings.HasPrefix(location, "Your")
	combat := strings.HasSuffix(location, "Combat")
	upkeep := strings.HasSuffix(location, "Upkeep")
	location = strings.TrimPrefix(location, "Your")
	location = strings.TrimSuffix(strings.TrimSuffix(location, "Combat"), "Upkeep")

	var body []vocab.Param
	switch location {
	case "Hand":
		body = []vocab.Param{{Key: "DB", Value: "ChangeZone"}, {Key: "Defined", Value: "DelayTriggerRememberedLKI"},
			{Key: "Origin", Value: "Battlefield"}, {Key: "Destination", Value: "Hand"}}
	case "Library":
		body = []vocab.Param{{Key: "DB", Value: "ChangeZone"}, {Key: "Defined", Value: "DelayTriggerRememberedLKI"},
			{Key: "Origin", Value: "Battlefield"}, {Key: "Destination", Value: "Library"}, {Key: "Shuffle", Value: "True"}}
	case "SacrificeCtrl":
		body = []vocab.Param{{Key: "DB", Value: "SacrificeAll"}, {Key: "Defined", Value: "DelayTriggerRememberedLKI"}}
	case "Sacrifice":
		body = []vocab.Param{{Key: "DB", Value: "SacrificeAll"}, {Key: "Defined", Value: "DelayTriggerRememberedLKI"},
			{Key: "Controller", Value: "You"}}
	case "Exile":
		body = []vocab.Param{{Key: "DB", Value: "ChangeZone"}, {Key: "Defined", Value: "DelayTriggerRememberedLKI"},
			{Key: "Origin", Value: "Battlefield"}, {Key: "Destination", Value: "Exile"}}
	case "Destroy":
		body = []vocab.Param{{Key: "DB", Value: "Destroy"}, {Key: "Defined", Value: "DelayTriggerRememberedLKI"}}
	default:
		return fmt.Errorf("engine: %s: AtEOT$ %q not resolvable yet", a.Params.Name, location)
	}
	phase := "End Of Turn"
	switch {
	case combat:
		phase = "EndCombat"
	case upkeep:
		phase = "Upkeep"
	}
	trigParams := []vocab.Param{{Key: "DB", Value: "DelayedTrigger"}, {Key: "Mode", Value: "Phase"}, {Key: "Phase", Value: phase}}
	if your {
		trigParams = append(trigParams, vocab.Param{Key: "ValidPlayer", Value: "You"})
	}
	execute := &compile.Ability{Record: compile.SubAbility, Name: body[0].Value, Params: body}
	d := delayedTrigger{
		Trigger: &compile.Ability{Record: compile.SubAbility, Name: "DelayedTrigger", Params: trigParams,
			Subs: []compile.SubRef{{Key: "Execute", Ability: execute}}},
		Host: a.Source, Controller: a.Controller, Amounts: a.Amounts, HostTransforms: g.Card(a.Source).Transforms,
	}
	for _, c := range cards {
		d.Remembered = append(d.Remembered, CardEntity(c))
	}
	g.delayed = append(g.delayed, d)
	return nil
}
