package engine

//enginelint:allow card game ability condition control amount trigger presentcheck

import "fmt"

// repeatUnresolvedParams: RepeatDefined$ swaps the battlefield for a
// Defined$ card list in the RepeatPresent$ count, and RepeatOptionalDecider$
// hands the "again?" question to another player.
var repeatUnresolvedParams = [...]string{
	"RepeatDefined", "RepeatOptionalDecider",
	"Condition", "Ultimate",
}

// repeatEffect is RepeatEffect.java: RepeatSubAbility$ resolves at least
// once, then again while the repeat conditions hold -- RepeatPresent$/
// RepeatCompare$ over the battlefield, RepeatCheckSVar$/RepeatSVarCompare$,
// and RepeatOptional$ asking the activator (ConfirmEffect) -- stopping at
// MaxRepeat$ or when the game ends. 37 real lines.
type repeatEffect struct{}

func (repeatEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range repeatUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Repeat: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	// RepeatCheckSVar$ is first read after the first repetition
	// (RepeatEffect.java:39-52, checkRepeatConditions), by which time the
	// repeated chain has usually stored it (StoreSVar, Mana Clash and Sword
	// of Dungeons & Dragons): it is not read up front. One that does not
	// resolve then is a pending error (checkSVarMatches).
	maxRepeat := -1
	if raw, ok := a.Params.Param("MaxRepeat"); ok {
		n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
		if !ok {
			return fmt.Errorf("engine: Repeat: MaxRepeat$ %q not resolvable yet", raw)
		}
		if n == 0 {
			return nil
		}
		maxRepeat = n
	}
	for count := 1; ; count++ {
		if err := g.resolveAdditionalKey(a, controller, "RepeatSubAbility"); err != nil {
			return err
		}
		if maxRepeat >= 0 && count >= maxRepeat {
			return nil
		}
		if g.over ||
			!isPresentMatches(g, source, a.Amounts, a.Params, "RepeatPresent", "RepeatCompare", "RepeatDefined", "", "") ||
			!checkSVarMatches(g, source, a.Amounts, a.Params, "RepeatCheckSVar", "RepeatSVarCompare", "") {
			return nil
		}
		if _, ok := a.Params.Param("RepeatOptional"); ok && !controller.ConfirmEffect(g, a.Controller, a.Source) {
			return nil
		}
	}
}
