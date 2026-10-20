package engine

//enginelint:allow control ability game effecthelpers card condition defined zone token parts event id

import "fmt"

// endureEffect is EndureEffect.java: for each Defined$ (default Self) or
// targeted card, in the order its owners arrange them, its controller
// either puts Num$ (default 1) +1/+1 counters on it -- only while it is on
// the battlefield, and only if they confirm -- or creates a Num$/Num$ white
// Spirit token (w_x_x_spirit). Counters go on first; the tokens are made
// together afterwards (makeTokenTable). Num$ below one does nothing.
type endureEffect struct{}

func (endureEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "Endure"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	n, err := optionalAmount(g, a, "Endure", "Num", 1)
	if err != nil {
		return err
	}
	if n < 1 {
		return nil
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Endure: %w", err)
	}
	cards, err = g.orderCardsByTheirOwners(controller, cards, Battlefield)
	if err != nil {
		return fmt.Errorf("engine: Endure: %w", err)
	}
	var specs []tokenSpec
	for _, id := range cards {
		c := g.Card(id)
		pl := c.Controller()
		if c.Zone == Battlefield && controller.ConfirmEffect(g, pl, a.Source) {
			// The enduring permanent's controller places (EndureEffect.java:63, :74).
			g.addCardCounters(controller, pl, a.Source, id, P1P1, n)
			continue
		}
		def, err := tokenScript(g, "w_x_x_spirit")
		if err != nil {
			return fmt.Errorf("engine: Endure: %w", err)
		}
		specs = append(specs, tokenSpec{Def: def, Owner: pl, Power: n, Toughness: n, HasPower: true, HasToughness: true})
	}
	var created []CardID
	for _, spec := range specs {
		created = append(created, g.createToken(controller, spec))
	}
	if len(created) > 0 {
		g.checkChangesZoneAllTriggers(controller, created, None, Battlefield)
	}
	return nil
}
