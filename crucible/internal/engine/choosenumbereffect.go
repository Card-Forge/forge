package engine

//enginelint:allow card game ability defined condition control amount parts

import "fmt"

// chooseNumberUnresolvedParams are ChooseNumberEffect.java's params this
// port cannot honour yet: the whole Secretly$ branch (KeepSecret$,
// Highest$/Lowest$/NotLowest$, Matched/UnmatchedAbility$,
// Guesser$/GuessCorrect$/GuessWrong$ -- AdditionalAbility dispatch over a
// HashMap<Player, Integer> whose iteration order orders the winners),
// RemoveChoices$ (reads remembered Integers, which Memory cannot hold), and
// RememberChosen$ (in Java it only ever adds the Secretly$ map's Integer
// values, again not an EntityID). Random$ is MyRandom's nextInt(max-min+1)
// + min (ChooseNumberEffect.java:65-66).
var chooseNumberUnresolvedParams = [...]string{
	"Secretly", "KeepSecret", "Highest", "Lowest", "NotLowest",
	"MatchedAbility", "UnmatchedAbility", "Guesser", "GuessCorrect",
	"GuessWrong", "RememberHighest", "RemoveChoices", "RememberChosen",
	"ChooseNumberSubAbility"}

// chooseNumberEffect is ChooseNumberEffect.java's open (non-secret) shape:
// each chooser (Defined$, default You) picks an integer between Min$
// (default 0) and Max$ (default 99) and the host records it
// (Memory.SetChosenNumber). ChooseAnyNumber$ is Java's announceRequirements
// path, the identical [lo, hi] contract, so it shares this one decision.
type chooseNumberEffect struct{}

func (chooseNumberEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range chooseNumberUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: ChooseNumber: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	lo, err := chooseNumberBound(g, a, source, "Min", "0")
	if err != nil {
		return err
	}
	hi, err := chooseNumberBound(g, a, source, "Max", "99")
	if err != nil {
		return err
	}
	choosers, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ChooseNumber: %w", err)
	}
	_, random := a.Params.Param("Random")
	if random && hi < lo {
		// Random.nextInt throws on a non-positive bound (GO-7: an error, not
		// a panic).
		return fmt.Errorf("engine: ChooseNumber: Random$ between %d and %d", lo, hi)
	}
	for _, pid := range choosers {
		var n int
		if random {
			n = g.randomInt(lo, hi)
		} else {
			n = controller.ChooseNumber(g, pid, a.Source, lo, hi)
			if n < lo || n > hi {
				return fmt.Errorf("engine: ChooseNumber: controller chose %d, want between %d and %d", n, lo, hi)
			}
		}
		source.Memory.SetChosenNumber(n)
	}
	return nil
}

func chooseNumberBound(g *Game, a *Ability, source *Card, key, def string) (int, error) {
	raw, ok := a.Params.Param(key)
	if !ok {
		raw = def
	}
	n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
	if !ok {
		return 0, fmt.Errorf("engine: ChooseNumber: %s$ %q not resolvable yet", key, raw)
	}
	return n, nil
}
