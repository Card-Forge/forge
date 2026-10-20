// Untap: CR 701.22, Tap's own mirror image at the opposite end of the
// identical Card.Tapped field -- 431 of the corpus's real (AB|DB)$ Untap
// lines once UntapUpTo$/UntapExactly$ (18 combined, below) are set aside:
// 228 name Defined$ and 163 name ValidTgts$ -- SpellAbilityEffect.
// getTargetCards(sa)'s own either/or contract, targetedOrDefinedCards
// (defined.go), Defined$ the dominant real shape here rather than
// ValidTgts$'s own dominance on Destroy/Tap.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/UntapEffect.java's
// resolve (the plain branch; untapChoose, below, is not). Not ported:
// TriggerType.UntapAll -- checkTapsTriggers' own mirror event
// (checkUntapsTriggers, trigger.go) already fires per card below, the
// identical "per-card mode already covers the real corpus" reasoning
// TapAll's own tapEffect doc comment gives for skipping the batch mode.

package engine

//enginelint:allow id card game ability defined condition control zone

import "fmt"

// untapUnresolvedParams names UntapEffect's own params this port does not
// evaluate. Every one fails the whole line loudly (PORT-8/GO-7): UntapUpTo$/
// UntapExactly$/UntapType$/Amount$ (16/2/18/18) -- untapChoose's own
// "choose up to N battlefield permanents matching a spec" interactive
// shape, this port's own PlayerController has no hook for it (the identical
// gap tapUnresolvedParams' own CardChoices$ has, no ChoosePermanentsToUntap
// method exists yet); Condition$/ConditionDefined$ (1/0) -- condition.go's
// own subAbilityConditionMet would otherwise silently no-op a card naming
// either, dealDamageEffect's own identical reasoning; ETB$ (6) -- Tap's own
// "enters tapped" replacement idiom (tapAbilityResolvesTap, replacement.go)
// has no untap-side mirror built, so an "enters the battlefield untapped"
// override line is not recognized anywhere in this port and must fail
// loudly here rather than silently untap-without-a-trigger the wrong
// permanent.
var untapUnresolvedParams = [...]string{
	"UntapUpTo", "UntapExactly", "UntapType", "Amount", "ETB",
}

// untapEffect resolves Mode$/DB$/AB$ Untap for the plain ValidTgts$/
// Defined$ shape -- UntapUpTo$/UntapExactly$'s own interactive "choose"
// variant fails loudly above rather than resolving here. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type untapEffect struct{}

func (untapEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range untapUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Untap: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Untap: %w", err)
	}
	// Radiance$ (UntapEffect.java:53).
	cards = append(cards, g.radianceCards(a)...)
	for _, id := range cards {
		c := g.Card(id)
		// A phased-out permanent is skipped (UntapEffect.java:57).
		if c.Zone != Battlefield || c.IsPhasedOut() || !c.Tapped {
			continue
		}
		g.runUntapCommands(controller, id)
		c.Tapped = false
		g.checkUntapsTriggers(controller, id)
	}
	return nil
}
