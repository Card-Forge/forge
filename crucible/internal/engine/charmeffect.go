// Charm: a modal ability (CR 700.2). Its modes are chosen as the ability
// is put on the stack, each chosen mode's targets with them, and resolving
// the Charm resolves the chosen modes in printed order.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/
// CharmEffect.java (makeChoices, makePossibleOptions, chainAbilities).
// Java chains the chosen modes onto the Charm as sub-abilities; this port
// keeps them on Ability.Modes instead, since a compiled Ability is
// immutable (PORT-2).

package engine

//enginelint:allow ability amount card control game

import (
	"fmt"
	"sort"
	"strings"
)

// charmUnresolvedParams are CharmEffect params this port does not model:
// ChoiceRestriction$ (Card.getChosenModes' once-per-turn/-game bookkeeping),
// CanRepeatModes$, Optional$, Chooser$ (an opponent chooses), Random$
// Compare, Defined$ (never read by CharmEffect; its six lines lean on a
// mode reading it). ActivationLimit$/GameActivationLimit$ are enforced at
// activation (activationLimitsMet, activateability.go).
var charmUnresolvedParams = [...]string{
	"ChoiceRestriction", "CanRepeatModes", "Optional", "Chooser", "RandomCompare",
	"RandomCompareSVar", "Defined",
}

// chooseCharmModes is makeChoices, run from pushTriggeredAbilities before
// the Charm goes on the stack. It fills a.Modes and reports whether the
// Charm may go on the stack: false when fewer modes are legal than
// MinCharmNum$ demands, or -- for a trigger -- when no mode was chosen
// ("trigger without chosen modes are removed from stack"). An activated
// Charm reaches here through the same push, and Java lets it keep zero
// modes when MinCharmNum$ allows; this port treats both the same, since
// neither can resolve to anything.
func (g *Game) chooseCharmModes(controller PlayerController, a *Ability) (bool, error) {
	for _, key := range charmUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return false, fmt.Errorf("engine: Charm: %s$ not resolvable yet", key)
		}
	}
	host := g.Card(a.Source)

	var options []Ability
	var names []string
	for _, sub := range a.Params.Subs {
		if !strings.EqualFold(sub.Key, "Choices") {
			continue
		}
		api, ok := APIByName(sub.Ability.Name)
		if !ok {
			return false, fmt.Errorf("engine: Charm: mode %s: unrecognized API %q", sub.SVar, sub.Ability.Name)
		}
		mode := Ability{
			API: api, Source: a.Source, Controller: a.Controller, Params: sub.Ability,
			Amounts: a.Amounts, TriggerRemembered: a.TriggerRemembered, triggered: a.triggered,
			charmRoot: a.Params, casting: a.casting,
		}
		if !g.modeHasLegalTargets(&mode) {
			continue
		}
		options = append(options, mode)
		names = append(names, sub.SVar)
	}

	num := 1
	if raw, ok := a.Params.Param("CharmNum"); ok {
		n, ok := resolveNamedAmount(g, a.Amounts, host, raw)
		if !ok {
			return false, fmt.Errorf("engine: Charm: CharmNum$ %q is not resolvable", raw)
		}
		num = n
	}
	minModes := num
	if raw, ok := a.Params.Param("MinCharmNum"); ok {
		n, ok := resolveNamedAmount(g, a.Amounts, host, raw)
		if !ok {
			return false, fmt.Errorf("engine: Charm: MinCharmNum$ %q is not resolvable", raw)
		}
		minModes = n
	}
	if minModes > len(options) {
		return false, nil
	}
	if num > len(options) {
		num = len(options)
	}

	var chosen []int
	if random, ok := a.Params.Param("Random"); ok {
		if random != "True" {
			return false, fmt.Errorf("engine: Charm: Random$ %q not resolvable yet", random)
		}
		chosen = g.randomSample(len(options), num)
	} else {
		chosen = controller.ChooseModesForAbility(g, a.Controller, a.Source, names, minModes, num)
		if err := checkModeChoice(chosen, len(options), minModes, num); err != nil {
			return false, err
		}
	}
	sort.Ints(chosen)

	// chainAbilities appends each chosen mode, its own SubAbility$ chain with
	// it, to the Charm's tail (CharmEffect.java:293-317), and setupTargets
	// then walks the whole chain (SpellAbility.java:2141-2171): each mode's
	// targets, then its own links', before the next mode's. A link naming its
	// own ValidTgts$ (Inscription of Abundance's fight) is chosen here, not
	// handed the mode's targets.
	a.Modes = nil
	for _, i := range chosen {
		mode := options[i]
		if !g.resolveTargets(controller, &mode) || !g.resolveChainTargets(controller, &mode) {
			return false, nil
		}
		a.Modes = append(a.Modes, mode)
	}
	return len(a.Modes) > 0, nil
}

// modeHasLegalTargets is makePossibleOptions' CR 603.3c filter: a mode
// that targets, needs at least one target and has no candidate cannot be
// chosen.
func (g *Game) modeHasLegalTargets(mode *Ability) bool {
	if _, ok := mode.Params.Param("ValidTgts"); !ok {
		return true
	}
	minStr, ok := mode.Params.Param("TargetMin")
	if !ok {
		minStr = "1"
	}
	if targetMin, ok := resolveNamedAmount(g, mode.Amounts, g.Card(mode.Source), minStr); ok && targetMin == 0 {
		return true
	}
	// The scan casting itself uses (targetChoiceFor): a mode targeting the
	// stack (Insidious Will's ChangeTargets and Counter modes) is judged over
	// the stack, a ChangeZone mode over its Origin$ zones. A targetChoice.err
	// (an unresolvable TargetMin$ among them) keeps the mode, so the error
	// surfaces when it resolves instead of the mode silently vanishing.
	_, _, ok = g.targetChoiceFor(mode)
	return ok
}

// checkModeChoice rejects a controller answer that is not lo..hi distinct
// indices into n options.
func checkModeChoice(chosen []int, n, lo, hi int) error {
	if len(chosen) < lo || len(chosen) > hi {
		return fmt.Errorf("engine: Charm: chose %d modes, want %d..%d", len(chosen), lo, hi)
	}
	seen := make([]bool, n)
	for _, i := range chosen {
		if i < 0 || i >= n || seen[i] {
			return fmt.Errorf("engine: Charm: mode index %d is not a distinct choice among %d", i, n)
		}
		seen[i] = true
	}
	return nil
}

// charmEffect resolves the modes chooseCharmModes picked, in printed
// order, each through the Registry driving the stack -- its own
// SubAbility$ chain and UnlessCost$ included.
type charmEffect struct{}

func (charmEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if a.modesErr != nil {
		return a.modesErr
	}
	if a.Modes == nil {
		return fmt.Errorf("engine: Charm: no modes were chosen when it was put on the stack")
	}
	for _, mode := range a.Modes {
		m := mode
		m.hostTransforms, m.hasHostTransforms = a.hostTransforms, a.hasHostTransforms
		m.damageMap = a.damageMap
		r := g.registry
		err := r.Resolve(g, &m, controller)
		g.registry = r
		if err != nil {
			return err
		}
	}
	return nil
}
