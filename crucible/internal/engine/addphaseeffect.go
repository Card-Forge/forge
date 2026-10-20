package engine

//enginelint:allow ability card condition control effecthelpers game phase

import (
	"fmt"
	"strings"
)

// addPhaseEffect is AddPhaseEffect.java (CR 500.8): NumPhases$ (default 1)
// times, the ExtraPhase$ -- Beginning (untap, upkeep, draw), Combat (the
// six combat steps) or one named phase -- then FollowedBy$ is inserted
// after AfterPhase$ (default the current phase), the most recent insertion
// running first (PhaseHandler.addExtraPhase). ExtraPhaseDelayedTrigger$ and
// BeforeFirstPostCombatMainEnd$ fail closed.
type addPhaseEffect struct{}

func (addPhaseEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "AddPhase", "ExtraPhaseDelayedTrigger", "BeforeFirstPostCombatMainEnd"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	after := g.activePhase
	if raw, ok := a.Params.Param("AfterPhase"); ok {
		p, ok := phaseNameFold(strings.TrimSpace(raw))
		if !ok {
			return fmt.Errorf("engine: AddPhase: AfterPhase$ %q not resolvable", raw)
		}
		after = p
	}
	extra, ok := a.Params.Param("ExtraPhase")
	if !ok {
		return fmt.Errorf("engine: AddPhase: no ExtraPhase$")
	}
	var list []PhaseType
	switch strings.TrimSpace(extra) {
	case "Beginning":
		list = []PhaseType{Untap, Upkeep, Draw}
	case "Combat":
		list = []PhaseType{CombatBegin, DeclareAttackers, DeclareBlockers, FirstStrikeDamage, CombatDamage, CombatEnd}
	default:
		p, ok := phaseNameFold(strings.TrimSpace(extra))
		if !ok {
			return fmt.Errorf("engine: AddPhase: ExtraPhase$ %q not resolvable", extra)
		}
		list = []PhaseType{p}
	}
	if raw, ok := a.Params.Param("FollowedBy"); ok {
		p, ok := phaseNameFold(strings.TrimSpace(raw))
		if !ok {
			return fmt.Errorf("engine: AddPhase: FollowedBy$ %q not resolvable", raw)
		}
		list = append(list, p)
	}
	num, err := optionalAmount(g, a, "AddPhase", "NumPhases", 1)
	if err != nil {
		return err
	}
	natural := PhaseType((int(after) + 1) % numPhaseTypes)
	for n := num; n > 0; n-- {
		g.addExtraPhase(after, list, natural)
	}
	return nil
}

// addExtraPhase is PhaseHandler.addExtraPhase: each extra phase is followed
// by the next in list, the last by whatever already followed after (a
// previous insertion's first phase) or else next; after is then followed by
// list's first phase.
func (g *Game) addExtraPhase(after PhaseType, list []PhaseType, next PhaseType) {
	for i, extra := range list {
		if i < len(list)-1 {
			g.extraPhases[extra] = append(g.extraPhases[extra], list[i+1])
			continue
		}
		if st := g.extraPhases[after]; len(st) > 0 {
			g.extraPhases[extra] = append(g.extraPhases[extra], st[len(st)-1])
			g.extraPhases[after] = st[:len(st)-1]
		} else {
			g.extraPhases[extra] = append(g.extraPhases[extra], next)
		}
	}
	g.extraPhases[after] = append(g.extraPhases[after], list[0])
}
