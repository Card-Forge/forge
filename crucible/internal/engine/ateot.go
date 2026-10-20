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
// AtEOTCondition$ puts a ConditionCheckSVar$ on the trigger's body, read off
// the host's SVars when it fires. A location Java has no trigger body for fails
// closed.
func registerAtEOT(g *Game, a *Ability, cards []CardID) error {
	location, ok := a.Params.Param("AtEOT")
	if !ok || len(cards) == 0 {
		return nil
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
	if cond, ok := a.Params.Param("AtEOTCondition"); ok {
		// SpellAbilityEffect.java:486-489: the body gains ConditionCheckSVar$.
		body = append(body, vocab.Param{Key: "ConditionCheckSVar", Value: cond})
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

// addSelfTrigger is SpellAbilityEffect.addSelfTrigger (:498-526), the
// AtEOTTrig$ of Token and CopyPermanent: the new card itself gets a trigger,
// "at the beginning of the end step, sacrifice (exile) this permanent". The
// trigger lives on the card, so it goes with it at a zone change: a permanent
// animateRecord holds it (clearAnimates drops it), not the zone-surviving grant
// store. A "You_" prefix is the line's Player$ You, which TriggerPhase never
// reads (it tests ValidPlayer$ only), so the trigger fires at every end step
// exactly as Java's does (forge-java-defects.md).
func (g *Game) addSelfTrigger(a *Ability, location string, card CardID) error {
	if _, loc, ok := strings.Cut(location, "_"); ok {
		location = loc
	}
	var execute []vocab.Param
	api := ""
	switch location {
	case "Sacrifice":
		api, execute = "Sacrifice", []vocab.Param{{Key: "SacValid", Value: "Self"}}
	case "Exile":
		api, execute = "ChangeZone", []vocab.Param{{Key: "Origin", Value: "Battlefield"},
			{Key: "Destination", Value: "Exile"}, {Key: "Defined", Value: "Self"}}
	default:
		return fmt.Errorf("engine: %s: AtEOTTrig$ %q not resolvable yet", a.Params.Name, location)
	}
	trig := designationTrigger("Phase",
		[]vocab.Param{{Key: "Phase", Value: "End of Turn"}, {Key: "TriggerZones", Value: "Battlefield"}}, api, execute)
	g.timestamp++
	g.addAnimate(animateRecord{Card: card, Timestamp: g.timestamp, Permanent: true, triggers: []*compile.Ability{trig}})
	return nil
}
