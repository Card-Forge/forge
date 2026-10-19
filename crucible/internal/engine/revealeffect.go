package engine

//enginelint:allow id card game ability defined condition control amount parts zone effecthelpers

import "fmt"

// revealUnresolvedParams are RevealEffect.java's params this port cannot
// honour yet: Random$ (Aggregates.random), OptionalDecider$ and
// BecomeStartingPlayer$ (1 line each).
var revealUnresolvedParams = [...]string{
	"Random", "OptionalDecider", "BecomeStartingPlayer",
	"Condition"}

// revealEffect is RevealEffect.java: each player (Defined$/ValidTgts$,
// default You) with a non-empty hand reveals RevealDefined$'s cards,
// every hand card matching RevealAllValid$, or -- the default -- NumCards$
// (default 1) cards they choose from their hand cards matching
// RevealValid$; AnyNumber$ and Optional$ lower the minimum to zero. The
// engine is omniscient, so revealing changes no state by itself; what this
// port keeps is RememberRevealed$ (43 of 68 real lines), which the
// following SubAbility$ reads back as Defined$ Remembered.
type revealEffect struct{}

func (revealEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range revealUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Reveal: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	cnt := 1
	if raw, ok := a.Params.Param("NumCards"); ok {
		n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
		if !ok {
			return fmt.Errorf("engine: Reveal: NumCards$ %q not resolvable yet", raw)
		}
		cnt = n
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Reveal: %w", err)
	}
	_, anyNumber := a.Params.Param("AnyNumber")
	_, optional := a.Params.Param("Optional")
	_, remember := a.Params.Param("RememberRevealed")

	for _, pid := range players {
		hand := g.Zone(Hand, pid).Cards()
		if len(hand) == 0 {
			continue
		}
		var revealed []CardID
		if defined, ok := a.Params.Param("RevealDefined"); ok {
			revealed, err = definedCards(source, defined, a.refs())
			if err != nil {
				return fmt.Errorf("engine: Reveal: RevealDefined$: %w", err)
			}
		} else if all, ok := a.Params.Param("RevealAllValid"); ok {
			revealed = filterValid(g, hand, all, pid, a.Source)
		} else {
			options := hand
			if rv, ok := a.Params.Param("RevealValid"); ok {
				options = filterValid(g, hand, rv, pid, a.Source)
			}
			if len(options) == 0 {
				continue
			}
			hi := cnt
			if hi > len(options) {
				hi = len(options)
			}
			lo := hi
			if anyNumber {
				hi, lo = len(options), 0
			} else if optional {
				lo = 0
			}
			revealed = controller.ChooseCardsForEffect(g, pid, a.Source, options, lo, hi)
			if err := checkChoice(revealed, options, lo, hi); err != nil {
				return fmt.Errorf("engine: Reveal: %w", err)
			}
		}
		if remember {
			for _, cid := range revealed {
				source.Memory.Remember(CardEntity(cid))
			}
		}
	}
	return nil
}
