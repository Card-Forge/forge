package engine

//enginelint:allow game ability control effecthelpers card condition id zone valid parts

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// gainControlVariantEffect is ControlGainVariantEffect.java: every
// AllValid$ permanent may change controller, all under one timestamp, per
// ChangeController$:
//
//   - CardOwner: each goes to its owner.
//   - NextPlayerInChosenDirection: each player gains what the next player
//     in the host's chosen direction controls.
//   - ChooseNextPlayerInChosenDirection: starting with the activator and
//     going round in the chosen direction, each player chooses one
//     permanent the next player controls and gains it.
//   - ChooseFromPlayerToTheirRight: the activator chooses, for each player,
//     one permanent the player to their right controls for them to gain.
//   - Random: each goes to a player picked at random (Aggregates.random).
//
// Players are walked starting with the activator (Collections.rotate).
type gainControlVariantEffect struct{}

func (gainControlVariantEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "GainControlVariant"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	mode, _ := a.Params.Param("ChangeController")
	spec, ok := a.Params.Param("AllValid")
	if !ok {
		return fmt.Errorf("engine: GainControlVariant: AllValid$ missing")
	}
	parsed := valid.Parse(spec)
	var cards []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if Matches(g, g.Card(id), parsed, source.Controller(), a.Source) {
				cards = append(cards, id)
			}
		}
	}
	players := g.playersFrom(a.Controller)
	controlledBy := func(p PlayerID) []CardID {
		var out []CardID
		for _, id := range cards {
			if g.Card(id).Controller() == p {
				out = append(out, id)
			}
		}
		return out
	}
	choose := func(decider PlayerID, options []CardID) (CardID, bool, error) {
		if len(options) == 0 {
			return NoCard, false, nil
		}
		chosen := controller.ChooseCardsForEffect(g, decider, a.Source, options, 0, 1)
		if err := checkChoice(chosen, options, 0, 1); err != nil {
			return NoCard, false, fmt.Errorf("engine: GainControlVariant: %w", err)
		}
		if len(chosen) == 0 {
			return NoCard, false, nil
		}
		return chosen[0], true, nil
	}
	type grant struct {
		to   PlayerID
		card CardID
	}
	var grants []grant
	direction := source.Memory.ChosenDirection()
	switch mode {
	case "CardOwner":
		for _, id := range cards {
			grants = append(grants, grant{g.Card(id).Owner, id})
		}
	case "Random":
		for _, id := range cards {
			grants = append(grants, grant{players[g.randomIndex(len(players))], id})
		}
	case "NextPlayerInChosenDirection":
		if direction == "" {
			return nil
		}
		for _, p := range players {
			for _, id := range controlledBy(g.nextPlayerInDirection(p, direction == "Right")) {
				grants = append(grants, grant{p, id})
			}
		}
	case "ChooseNextPlayerInChosenDirection":
		if direction == "" {
			return nil
		}
		p := a.Controller
		for {
			next := g.nextPlayerInDirection(p, direction == "Right")
			id, ok, err := choose(p, controlledBy(next))
			if err != nil {
				return err
			}
			if ok {
				grants = append(grants, grant{p, id})
			}
			p = next
			if p == a.Controller {
				break
			}
		}
	case "ChooseFromPlayerToTheirRight":
		for _, p := range players {
			id, ok, err := choose(a.Controller, controlledBy(g.nextPlayerInDirection(p, true)))
			if err != nil {
				return err
			}
			if ok {
				grants = append(grants, grant{p, id})
			}
		}
	default:
		return fmt.Errorf("engine: GainControlVariant: ChangeController$ %q not resolvable", mode)
	}
	g.timestamp++
	ts := g.timestamp
	for _, gr := range grants {
		if g.Card(gr.card).Zone != Battlefield {
			continue
		}
		g.changeControllerAt(controller, gr.card, gr.to, ts)
	}
	return nil
}

// playersFrom is the game's players rotated so first leads
// (Collections.rotate on the player list), lost players included the way
// Java's getPlayers is.
func (g *Game) playersFrom(first PlayerID) []PlayerID {
	ids := g.Players()
	for i, id := range ids {
		if id == first {
			return append(append([]PlayerID(nil), ids[i:]...), ids[:i]...)
		}
	}
	return append([]PlayerID(nil), ids...)
}
