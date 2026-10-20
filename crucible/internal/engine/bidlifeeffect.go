package engine

//enginelint:allow control game ability effecthelpers card condition amount id defined player parts

import (
	"fmt"
	"math"
)

// bidLifeEffect is BidLifeEffect.java: bidding starts at StartBidding$
// (default 0; Any lets the activator announce it). The bidders -- the
// activator and OtherBidder$, or every player in turn order starting with
// the activator -- are asked in rounds whether to top the bid; a player who
// does raises it by 1-9 and becomes the winner. Bidding ends after a round
// where nobody tops it. The host records the final bid as its chosen number
// and remembers the winner while BidSubAbility$ resolves, then forgets
// everything.
type bidLifeEffect struct{}

func (bidLifeEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "BidLife"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	bid := 0
	if raw, ok := a.Params.Param("StartBidding"); ok {
		if raw == "Any" {
			bid = controller.ChooseNumber(g, a.Controller, a.Source, 0, math.MaxInt32)
			if bid < 0 {
				return fmt.Errorf("engine: BidLife: starting bid %d out of range", bid)
			}
		} else {
			n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
			if !ok {
				return fmt.Errorf("engine: BidLife: StartBidding$ %q not resolvable", raw)
			}
			bid = n
		}
	}
	var bidders []PlayerID
	if raw, ok := a.Params.Param("OtherBidder"); ok {
		others, err := definedPlayers(g, a.Controller, a.Source, raw, a.refs())
		if err != nil {
			return fmt.Errorf("engine: BidLife: %w", err)
		}
		bidders = append([]PlayerID{a.Controller}, others...)
	} else {
		for _, p := range g.playersFrom(a.Controller) {
			if !g.Player(p).Lost {
				bidders = append(bidders, p)
			}
		}
	}
	winner := a.Controller
	for more := true; more; {
		more = false
		for _, p := range bidders {
			if !controller.ConfirmEffect(g, p, a.Source) {
				continue
			}
			raise := controller.ChooseNumber(g, p, a.Source, 1, 9)
			if raise < 1 || raise > 9 {
				return fmt.Errorf("engine: BidLife: raise %d out of range", raise)
			}
			bid += raise
			winner = p
			more = true
		}
	}
	source.Memory.SetChosenNumber(bid)
	source.Memory.Remember(PlayerEntity(winner))
	if err := g.resolveAdditionalKey(a, controller, "BidSubAbility"); err != nil {
		return err
	}
	source.Memory.ClearRemembered()
	return nil
}
