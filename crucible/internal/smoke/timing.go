package smoke

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
)

// moment is a point of a turn a timed variant plays the game up to before the
// card is cast: whose turn it is and the step.
type moment struct {
	// opponents is the opponent's turn rather than the probe player's.
	opponents bool
	step      engine.PhaseType
}

func (m moment) String() string {
	whose := "own"
	if m.opponents {
		whose = "opponent's"
	}
	return fmt.Sprintf("the %s %v", whose, m.step)
}

// timingKeys are the spell restrictions that tie a cast to a moment of the
// turn (SpellAbilityRestriction.checkTimingRestrictions, and CheckSVar$, which
// the "cast only if you've been attacked this step" and "after their upkeep"
// spells read). A spell with none of them is never tried timed.
var timingKeys = [...]string{"ActivationPhases", "PlayerTurn", "OpponentTurn", "CheckSVar"}

// walkSteps are the steps a timed variant tries, in turn order: every step
// that grants priority. The first-strike damage step is left out, as the fixed
// board has no first strike to give it one.
var walkSteps = [...]engine.PhaseType{
	engine.Upkeep, engine.Draw, engine.Main1, engine.CombatBegin, engine.DeclareAttackers,
	engine.DeclareBlockers, engine.CombatDamage, engine.CombatEnd, engine.Main2, engine.EndOfTurn,
}

// timedBoards are the boards a timed variant is tried on: plain, rich (an
// artifact, an enchantment, a planeswalker to attack ...) and tailored (a
// Wall for Glyph of Reincarnation's target).
var timedBoards = [...]variant{{}, {rich: true}, {tailor: true}}

// timedVariants is, for a card whose spell names a timing restriction, one
// variant per board, turn and step. Which of them the engine accepts is the
// engine's own answer (timingRestrictionsMet): the probe does not read
// ActivationPhases$ itself, it only narrows the turns PlayerTurn$ and
// OpponentTurn$ already rule out.
func timedVariants(def *compile.Card) []variant {
	spell := firstSpell(def)
	if spell == nil {
		return nil
	}
	timed := false
	for _, k := range timingKeys {
		_, ok := spell.Param(k)
		timed = timed || ok
	}
	if !timed {
		return nil
	}
	turns := []bool{true, false}
	if _, ok := spell.Param("PlayerTurn"); ok {
		turns = []bool{false}
	}
	if _, ok := spell.Param("OpponentTurn"); ok {
		turns = []bool{true}
	}
	var out []variant
	for _, board := range timedBoards {
		for _, opponents := range turns {
			for _, step := range walkSteps {
				v := board
				v.timed, v.at = true, moment{opponents: opponents, step: step}
				out = append(out, v)
			}
		}
	}
	return out
}

// firstSpell is the card's first SP$ line, the one CastSpell casts.
func firstSpell(def *compile.Card) *compile.Ability {
	for _, ab := range def.Faces[0].Abilities {
		if ab.Record == compile.Spell {
			return ab
		}
	}
	return nil
}

// walk plays the game from the untap step of the moment's turn up to its step
// through Game.Step, the turn driver: the upkeep is counted, the active player
// attacks with everything (Controller), the defender does not block, and
// combat damage is dealt, as in a real turn. The opponent's turn is turn 2, as
// if the probe player had gone first. An error, or a step the turn skips (no
// attackers, no declare blockers step), means the moment cannot be reached.
func (p *Prober) walk(g *engine.Game, ctl *Controller, pid, opp engine.PlayerID, m moment) error {
	active, turn := pid, 1
	if m.opponents {
		active, turn = opp, 2
	}
	g.SetTurnState(turn, active, engine.Untap)
	g.SetStartingPlayer(pid)
	for g.ActivePhase() != m.step {
		if g.Over() || g.Turn() != turn || g.ActivePhase() > m.step {
			return fmt.Errorf("%v could not be reached", m)
		}
		if err := g.Step(p.registry, ctl); err != nil {
			return fmt.Errorf("the walk to %v failed: %w", m, err)
		}
	}
	return nil
}
