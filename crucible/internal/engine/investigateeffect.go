package engine

//enginelint:allow ability card condition control defined effecthelpers game id parts player token zone

// investigateEffect is InvestigateEffect.java: Num$ times (default 1), each
// target or Defined$ player (default You) creates a Clue (c_a_clue_draw),
// ChangesZoneAll firing once per round. Optional$ (a per-player "may")
// fails closed; InvestigatedThisTurn is not tracked, since nothing in this
// port reads it.
type investigateEffect struct{}

func (investigateEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "Investigate", "Optional"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	num, err := optionalAmount(g, a, "Investigate", "Num", 1)
	if err != nil {
		return err
	}
	clue, err := tokenScript(g, "c_a_clue_draw")
	if err != nil {
		return err
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return err
	}
	players = g.inAPNAPOrder(players)
	for i := 0; i < num; i++ {
		var created []CardID
		for _, p := range players {
			if g.Player(p).Lost {
				continue
			}
			created = append(created, g.createToken(controller, tokenSpec{Def: clue, Owner: p}))
			if hasParam(a, "RememberInvestigatingPlayers") {
				source.Memory.Remember(PlayerEntity(p))
			}
		}
		g.checkChangesZoneAllTriggers(controller, created, None, Battlefield)
	}
	return nil
}
