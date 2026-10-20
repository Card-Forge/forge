package engine

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// additionalAbilities returns parent's compiled references under key
// (case-insensitive), in script order -- SpellAbility.getAdditionalAbility
// for a single key, getAdditionalAbilityList for Choices$.
func additionalAbilities(parent *compile.Ability, key string) []compile.SubRef {
	var out []compile.SubRef
	for _, sub := range parent.Subs {
		if strings.EqualFold(sub.Key, key) {
			out = append(out, sub)
		}
	}
	return out
}

// resolveAdditional is AbilityUtils.resolve on one AdditionalAbility: sub
// becomes a child Ability sharing parent's source, controller and targets
// (resolveSubAbility's own construction, subability.go) and resolves
// through the Registry currently driving the stack, SubAbility$ chain and
// UnlessCost$ included.
func (g *Game) resolveAdditional(parent *Ability, controller PlayerController, sub compile.SubRef) error {
	api, ok := APIByName(sub.Ability.Name)
	if !ok {
		return fmt.Errorf("engine: %s$ %s: unrecognized API %q", sub.Key, sub.SVar, sub.Ability.Name)
	}
	if g.registry == nil {
		return fmt.Errorf("engine: %s$ %s: no Registry is resolving", sub.Key, sub.SVar)
	}
	child := Ability{
		API:        api,
		Source:     parent.Source,
		Controller: parent.Controller,
		Target:     parent.Target,
		Targets:    parent.Targets,
		// SpellAbility.getParentTargetingCard walks up from an additional
		// ability through the ability that holds it.
		parentTargets:     parentTargetsOf(parent),
		ancestorTargeting: parent.ancestorTargeting || linkUsesTargeting(parent.Params),
		paid:              parent.paid,
		activationIndex:   parent.activationIndex,
		Params:            sub.Ability,
		Amounts:           parent.Amounts,
		rootSpell:         parent.spell || parent.rootSpell,
		// The root's targeted spells and abilities as last seen
		// (getAllTargetChoices starts at getRootAbility), for a Branch or roll
		// result under a counterspell.
		stackTargets:      parent.stackTargets,
		TriggerRemembered: parent.TriggerRemembered,
		triggered:         parent.triggered,
		hostTransforms:    parent.hostTransforms,
		hasHostTransforms: parent.hasHostTransforms,
		damageMap:         parent.damageMap,
		// An AdditionalAbility of a replacement's ReplaceWith$ chain (Molten
		// Sentry's FlipCoin branches) still replaces the same event.
		replacing: parent.replacing,
	}
	r := g.registry
	err := r.Resolve(g, &child, controller)
	g.registry = r
	return err
}

// resolveAdditionalKey resolves parent's single AdditionalAbility under key,
// doing nothing when the script names none.
func (g *Game) resolveAdditionalKey(parent *Ability, controller PlayerController, key string) error {
	subs := additionalAbilities(parent.Params, key)
	if len(subs) == 0 {
		return nil
	}
	return g.resolveAdditional(parent, controller, subs[0])
}
