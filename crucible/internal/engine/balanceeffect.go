package engine

//enginelint:allow ability card condition control discardeffect effecthelpers game id sacrificeeffect valid zone

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// balanceEffect is BalanceEffect.java: every player, in turn order, with
// more Valid$ (default Card) cards in Zone$ (Battlefield, or Hand) than the
// player with the fewest, sacrifices -- or, for a hand, discards -- the
// difference, chosen by themselves. Discards happen together after every
// choice; sacrifices as each player chooses.
type balanceEffect struct{}

func (balanceEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "Balance"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	spec, ok := a.Params.Param("Valid")
	if !ok {
		spec = "Card"
	}
	zone := Battlefield
	if raw, ok := a.Params.Param("Zone"); ok {
		z, ok := ZoneByName(raw)
		if !ok || (z != Battlefield && z != Hand) {
			return fmt.Errorf("engine: Balance: Zone$ %q not resolvable yet", raw)
		}
		zone = z
	}
	parsed := valid.Parse(spec)
	players := g.playersInAPNAPOrder()
	counts := make([][]CardID, len(players))
	least := -1
	for i, p := range players {
		for _, id := range g.Zone(zone, p).Cards() {
			if Matches(g, g.Card(id), parsed, a.Controller, a.Source) {
				counts[i] = append(counts[i], id)
			}
		}
		if least < 0 || len(counts[i]) < least {
			least = len(counts[i])
		}
	}
	discards := make([][]CardID, len(players))
	for i, p := range players {
		n := len(counts[i]) - least
		if n == 0 {
			continue
		}
		if zone == Hand {
			chosen := controller.ChooseCardsToDiscard(g, p, counts[i], n)
			if err := checkChoice(chosen, counts[i], n, n); err != nil {
				return fmt.Errorf("engine: Balance: %w", err)
			}
			discards[i] = chosen
			continue
		}
		chosen := controller.ChoosePermanentsToSacrifice(g, p, counts[i], n)
		if err := checkChoice(chosen, counts[i], n, n); err != nil {
			return fmt.Errorf("engine: Balance: %w", err)
		}
		sacrificeCards(g, controller, a, chosen)
	}
	for i, p := range players {
		if len(discards[i]) > 0 {
			discardCards(g, controller, discards[i], p)
		}
	}
	return nil
}
