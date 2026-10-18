// Sub-ability chaining: AbilityUtils.resolveApiAbility's own
// resolveSubAbilities call, ported. This port's own most-cited gap after
// targeting -- SubAbility$ appears on 16,022 real corpus lines (12% of the
// whole corpus), and every M6 effect landed so far names it in its own
// "not resolved" list.
//
// Ported from forge-game/src/main/java/forge/game/ability/AbilityUtils.java
// (resolveApiAbility, resolveSubAbilities).

package engine

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// resolveSubAbility chains the one SubAbility$ a compiled ability names, if
// it names one at all -- resolveApiAbility's own unconditional
// resolveSubAbilities(sa, game) call, made right after sa.resolve() in
// Registry.Resolve (effect.go), so a sub-ability runs whether or not the
// parent's own Condition$ gate let the parent's own body run:
// subAbilityConditionMet (condition.go) is each effect's own internal check
// on ITS OWN Params, read fresh off the child Ability built here, not
// something this function evaluates on the parent's behalf. Sphinx
// Sovereign is the real corpus card this behavior is load-bearing for: "you
// gain 3 life if CARDNAME is untapped. Otherwise, each opponent loses 3
// life" is one DB$ LoseLife (Condition$ Card.tapped) with a
// SubAbility$ DB$ GainLife (the identical Condition$, negated via
// ConditionCompare$ EQ0) -- untapped means the parent's own condition fails
// and its body is a no-op, but the chain still has to reach the sub for the
// gain-life half to ever happen.
//
// Only the literal `SubAbility$` key auto-chains this way (compile.go's own
// subAbilityKeys comment): `PreventionSubAbility$` and every "additional
// ability" key (WinSubAbility$, ChooseSubAbility$, Choices$, ...) name a
// sub-ability an effect fetches and resolves explicitly through its own Go
// code once that effect exists (FlipCoinEffect.java, ChoosePlayerEffect.java,
// ...) -- none of those effects are built yet, so this is the only chaining
// shape this port has a caller for today.
//
// Recurses through r.Resolve rather than r[api].Resolve directly, so a
// sub-ability's own further SubAbility$ (16,022 real lines chain to a depth
// of 2 or more 5,556 times, up to 13 deep once) keeps chaining without this
// function needing a loop -- the compiled Ability tree (compile.Ability.Subs)
// already holds the whole chain, one reference at a time.
//
// A sub-ability naming its own ValidTgts$ (891 of 16,022 real referenced
// lines, 5.6%) is targeted separately: chaintargets.go chooses each link's
// targets at cast and trigger time, so the child built here carries its own
// Target/Targets rather than the parent's. The CR 608.2b re-check at
// resolution covers them (dropIllegalTargets).
func (r *Registry) resolveSubAbility(g *Game, parent *Ability, controller PlayerController) error {
	if parent.Params == nil {
		return nil
	}
	sub, ok := findSubAbility(parent.Params)
	if !ok {
		return nil
	}
	api, ok := APIByName(sub.Ability.Name)
	if !ok {
		// Not reachable against the real corpus today -- ApiType.java's own
		// generated vocabulary (ability.go) and the apiscan/vocabscan gates
		// (M3) already require every real API string to resolve -- but a
		// card cannot be trusted not to be the first (PORT-8), and a
		// sub-ability silently dropped after the parent's own body already
		// ran would be a wrong guess, not a no-op (GO-7).
		return fmt.Errorf("engine: SubAbility$ %s: unrecognized API %q", sub.SVar, sub.Ability.Name)
	}
	child := Ability{
		API:               api,
		Source:            parent.Source,
		Controller:        parent.Controller,
		Target:            parent.Target,
		Targets:           parent.Targets,
		parentTargets:     parentTargetsOf(parent),
		ancestorTargeting: parent.ancestorTargeting || linkUsesTargeting(parent.Params),
		radiance:          parent.radianceSpec(),
		Params:            sub.Ability,
		Amounts:           parent.Amounts,
		TriggerRemembered: parent.TriggerRemembered,
		triggered:         parent.triggered,
		hostTransforms:    parent.hostTransforms,
		hasHostTransforms: parent.hasHostTransforms,
		damageMap:         parent.damageMap,
		// SpellAbility.getRootAbility: a sub-ability reads its root's X.
		xManaCostPaid:    parent.xManaCostPaid,
		hasXManaCostPaid: parent.hasXManaCostPaid,
		paid:             parent.paid,
		// The parent's targeted abilities on the stack as last seen (ADR-0041),
		// for a link read after the parent countered one.
		stackTargets: parent.stackTargets,
		// ReplacementHandler.executeReplacement sets the replacing objects
		// on every ability of the ReplaceWith$ chain, not the head alone.
		replacing: parent.replacing,
	}
	child.chainTargets = parent.chainTargets
	if ct, ok := parent.chainTargetsFor(sub.Ability); ok {
		child.Targets, child.Target, child.divided, child.targetsErr = ct.Targets, NoCard, ct.Divided, ct.Err
		for _, e := range ct.Targets {
			if id, ok := e.AsCard(); ok {
				child.Target = id
				break
			}
		}
	}
	// AbilityUtils.java:1366-1367: the statics are rechecked before each
	// sub-ability, so the link reads what the previous one changed.
	if g != nil {
		g.refreshLayersBetweenLinks()
	}
	return r.Resolve(g, &child, controller)
}

// findSubAbility returns the one `SubAbility$` reference a's own Subs carry,
// if any -- mergeParams (compile.go) already collapses a repeated key to one
// value, so at most one exists.
func findSubAbility(a *compile.Ability) (compile.SubRef, bool) {
	for _, sub := range a.Subs {
		if strings.EqualFold(sub.Key, "SubAbility") {
			return sub, true
		}
	}
	return compile.SubRef{}, false
}

// linkUsesTargeting is SpellAbility.usesTargeting for a compiled link: it names
// ValidTgts$.
func linkUsesTargeting(p *compile.Ability) bool {
	if p == nil {
		return false
	}
	_, ok := p.Param("ValidTgts")
	return ok
}

// rememberTargetsLine is handleRemembering's own test (AbilityUtils.java:1520):
// the line names RememberTargets$ and targets.
func rememberTargetsLine(p *compile.Ability) bool {
	if p == nil {
		return false
	}
	_, ok := p.Param("RememberTargets")
	return ok && linkUsesTargeting(p)
}

// parentTargetsOf is what Defined$ ParentTarget names for a child of parent:
// parent's own targets, or the ones its own ParentTarget names when it chose
// none (SpellAbility.getParentTargetingCard walks up to the first ancestor
// that targets).
func parentTargetsOf(parent *Ability) []EntityID {
	if len(parent.Targets) > 0 {
		return parent.Targets
	}
	return parent.parentTargets
}
