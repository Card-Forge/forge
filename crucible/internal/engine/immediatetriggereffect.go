package engine

//enginelint:allow ability additional card condition control defined effecthelpers game id trigger

import (
	"fmt"
	"strings"
)

// immediateTriggerEffect is ImmediateTriggerEffect.java: a reflexive
// triggered ability (CR 603.12) -- TriggerAmount$ (default 1) copies of
// Execute$, each put on the stack as a triggered ability of this ability's
// controller, remembering RememberObjects$ (one object per copy with
// RememberEach$). Java registers it as a delayed trigger that fires at the
// next trigger check; this port pushes it at once, which lands it on the
// stack above everything the resolving ability left there, the same
// resolution order. OptionalDecider$ You makes it a "may".
// RememberSVarAmount$ remembers an Integer and fails closed, as do Static$
// and the triggering-object copies.
type immediateTriggerEffect struct{}

func (immediateTriggerEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "ImmediateTrigger", "RememberSVarAmount", "Static",
		"AfterReplacement", "RememberDiscarded", "Condition", "ConditionDefined"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	amount, err := optionalAmount(g, a, "ImmediateTrigger", "TriggerAmount", 1)
	if err != nil || amount <= 0 {
		return err
	}
	execs := additionalAbilities(a.Params, "Execute")
	if len(execs) == 0 {
		return fmt.Errorf("engine: ImmediateTrigger: no Execute$")
	}
	api, ok := APIByName(execs[0].Ability.Name)
	if !ok {
		return fmt.Errorf("engine: ImmediateTrigger: Execute$ %s: unrecognized API %q", execs[0].SVar, execs[0].Ability.Name)
	}
	optional, ok := triggerIsOptional(a.Params)
	if !ok {
		return fmt.Errorf("engine: ImmediateTrigger: OptionalDecider$ not resolvable yet")
	}
	var remember []EntityID
	if raw, ok := a.Params.Param("RememberObjects"); ok {
		for _, def := range strings.Split(raw, " & ") {
			es, err := definedEntities(g, a.Controller, source, def, a.refs())
			if err != nil {
				return fmt.Errorf("engine: ImmediateTrigger: %w", err)
			}
			remember = append(remember, es...)
		}
	}
	each := hasParam(a, "RememberEach")
	if each && len(remember) < amount {
		return fmt.Errorf("engine: ImmediateTrigger: RememberEach$ has %d objects for %d triggers", len(remember), amount)
	}
	var abilities []Ability
	for i := 0; i < amount; i++ {
		r := remember
		if each {
			r = remember[i : i+1]
		}
		abilities = append(abilities, Ability{
			API: api, Source: a.Source, Controller: a.Controller, Params: execs[0].Ability,
			Amounts: a.Amounts, Optional: optional, TriggerRemembered: r,
		})
	}
	g.pushTriggeredAbilities(controller, abilities)
	return nil
}
