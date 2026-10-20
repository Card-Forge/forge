package engine

//enginelint:allow ability card condition control defined effecthelpers game phase player turn

import (
	"fmt"
	"strings"
)

// skipPhaseEffect is SkipPhaseEffect.java: each target or Defined$ player
// (default You) skips their next Phase$/Step$ (PhaseType.parseRange --
// "Main" is both main phases, BeginCombat the whole combat phase). Duration$
// EndOfTurn skips every such phase for the rest of the turn; NextThisTurn
// skips the next one and lapses at end of turn. Start$ (wait for an
// upkeep) fails closed.
type skipPhaseEffect struct{}

func (skipPhaseEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "SkipPhase", "Start"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	name, ok := a.Params.Param("Phase")
	if !ok {
		name, ok = a.Params.Param("Step")
	}
	if !ok {
		return fmt.Errorf("engine: SkipPhase: no Phase$ or Step$")
	}
	phases, ok := parsePhaseRange(name)
	if !ok {
		return fmt.Errorf("engine: SkipPhase: phase %q not resolvable", name)
	}
	rec := skipPhase{Phases: phases}
	if d, ok := a.Params.Param("Duration"); ok {
		switch {
		case d == "NextThisTurn":
			rec.UntilCleanup = true
		case strings.EqualFold(d, "EndOfTurn"):
			rec.Each, rec.UntilCleanup = true, true
		default:
			return fmt.Errorf("engine: SkipPhase: Duration$ %q not resolvable yet", d)
		}
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return err
	}
	for _, p := range g.inAPNAPOrder(players) {
		r := rec
		r.Player = p
		g.skips = append(g.skips, r)
	}
	return nil
}
