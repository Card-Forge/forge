package engine

//enginelint:allow card game ability defined condition control parts

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/mana"
)

// chooseColorUnresolvedParams are ChooseColorEffect.java's params this port
// cannot honour yet; a bare CheckSVar$/SVarCompare$ is the activation gate
// activationLimitsMet's sibling otherRestrictionsMet enforces.
var chooseColorUnresolvedParams = [...]string{
	"Condition", "InstantSpeed", "Forecast",
}

// chooseColorEffect is ChooseColorEffect.java: each chooser (Defined$,
// default You) picks colors out of all five, or Choices$'s restricted list,
// or the colors among ColorsFrom$'s cards (none: the effect ends,
// ChooseColorEffect.java:46-52), minus Exclude$. The count is one, exactly
// two for TwoColors$, one or more for OrColors$, and UpTo$ lowers the minimum
// to zero -- Java's own cntMin/cntMax pair. Random$ draws the minimum count
// with Aggregates.random over the list in WUBRG order, each pick off the list
// (:80-87). A colorless answer ends the effect without recording anything, as
// Java's own early return does. 122 real lines.
type chooseColorEffect struct{}

func (chooseColorEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range chooseColorUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: ChooseColor: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	options := mana.AllColors
	_, random := a.Params.Param("Random")
	if raw, ok := a.Params.Param("Choices"); ok {
		if _, exclude := a.Params.Param("Exclude"); random || exclude {
			// Java's Choices$ list is an Arrays.asList, which neither Exclude$'s
			// remove nor the random pick's can shrink: both throw
			// UnsupportedOperationException (ChooseColorEffect.java:44, :55, :84,
			// forge-java-defects.md). No card combines them.
			return fmt.Errorf("engine: ChooseColor: Choices$ with Random$ or Exclude$ not resolvable")
		}
		c, err := parseColorNames(raw)
		if err != nil {
			return fmt.Errorf("engine: ChooseColor: Choices$: %w", err)
		}
		options = c
	}
	if raw, ok := a.Params.Param("ColorsFrom"); ok {
		cards, err := definedCardsInGame(g, a.Controller, source, raw, a.refs())
		if err != nil {
			return fmt.Errorf("engine: ChooseColor: ColorsFrom$: %w", err)
		}
		// CardUtil.getColorsFromCards: the union of their colors.
		var from mana.Colors
		for _, id := range cards {
			from |= g.Card(id).Colors()
		}
		if from.IsColorless() {
			return nil
		}
		options = from
	}
	if raw, ok := a.Params.Param("Exclude"); ok {
		c, err := parseColorNames(raw)
		if err != nil {
			return fmt.Errorf("engine: ChooseColor: Exclude$: %w", err)
		}
		options &^= c
	}
	_, upTo := a.Params.Param("UpTo")
	_, two := a.Params.Param("TwoColors")
	_, or := a.Params.Param("OrColors")
	lo, hi := 1, 1
	switch {
	case upTo:
		lo = 0
	case two:
		lo = 2
	}
	switch {
	case two:
		hi = 2
	case or:
		hi = options.Count()
	}

	choosers, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ChooseColor: %w", err)
	}
	for _, pid := range choosers {
		var chosen mana.Colors
		if random {
			chosen = g.randomColors(options, lo)
		} else {
			chosen = controller.ChooseColors(g, pid, a.Source, options, lo, hi)
			if chosen&^options != 0 || chosen.Count() < lo || chosen.Count() > hi {
				return fmt.Errorf("engine: ChooseColor: controller chose %v, want %d to %d of %v", chosen, lo, hi, options)
			}
		}
		if chosen.IsColorless() {
			return nil
		}
		source.Memory.SetChosenColors(chosen)
	}
	return nil
}

// randomColors is ChooseColorEffect's Random$ loop (ChooseColorEffect.java:
// 80-87): n draws, each Aggregates.random over the colors left in options in
// WUBRG order (MagicColor.Constant.ONLY_COLORS), the pick removed.
func (g *Game) randomColors(options mana.Colors, n int) mana.Colors {
	var left []mana.Colors
	for _, c := range [...]mana.Colors{mana.White, mana.Blue, mana.Black, mana.Red, mana.Green} {
		if options.HasAny(c) {
			left = append(left, c)
		}
	}
	var chosen mana.Colors
	for range n {
		i := g.randomIndex(len(left))
		if i < 0 {
			break
		}
		chosen |= left[i]
		left = append(left[:i], left[i+1:]...)
	}
	return chosen
}

// parseColorNames reads a comma-separated list of MagicColor's own
// lowercase color names, what Choices$ and Exclude$ spell ("white,blue").
func parseColorNames(raw string) (mana.Colors, error) {
	var out mana.Colors
	for _, name := range strings.Split(raw, ",") {
		switch strings.ToLower(strings.TrimSpace(name)) {
		case "white":
			out |= mana.White
		case "blue":
			out |= mana.Blue
		case "black":
			out |= mana.Black
		case "red":
			out |= mana.Red
		case "green":
			out |= mana.Green
		default:
			return 0, fmt.Errorf("color %q not resolvable yet", name)
		}
	}
	return out, nil
}
