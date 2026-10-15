// Dividing an amount among targets: CR 601.2d -- "if the spell requires the
// player to divide or distribute an effect (such as damage or counters) as
// they choose among any number of targets, the player announces the
// division," right after choosing the targets. Each target must receive at
// least one.
//
// Ported from forge-gui's PlayerControllerHuman.chooseTargetsFor
// (PlayerControllerHuman.java:2486-2527), which allocates as soon as the
// targets are chosen, and SpellAbility's dividedValue/TargetChoices.dividedMap
// (SpellAbility.java:1822-1846, 2182-2188; TargetChoices.java:51, 153-171),
// which carry the allocation to resolution. An effect reads one target's
// share back through dividedShareOf.

package engine

//enginelint:allow id game ability control amount targeting

import "fmt"

// isDivided reports whether a names DividedAsYouChoose$
// (SpellAbility.isDividedAsYouChoose, SpellAbility.java:1822-1824).
func (a *Ability) isDivided() bool {
	if a.Params == nil {
		return false
	}
	_, ok := a.Params.Param("DividedAsYouChoose")
	return ok
}

// dividedTotal is SpellAbility.dividedValue: DividedAsYouChoose$ evaluated
// as the targets are chosen (clearTargets, SpellAbility.java:2182-2188), the
// amount divideAmongTargets splits. ok is false for a value this port cannot
// evaluate.
func (g *Game) dividedTotal(a *Ability) (int, bool, error) {
	raw, ok := a.Params.Param("DividedAsYouChoose")
	if !ok {
		return 0, false, nil
	}
	defer g.inAbilityContext(a)()
	n, ok := resolveNamedAmount(g, a.Amounts, g.Card(a.Source), raw)
	if !ok {
		return 0, true, fmt.Errorf("engine: DividedAsYouChoose$ %q is not resolvable", raw)
	}
	return n, true, nil
}

// capDividedChoice bounds choice by a's DividedAsYouChoose$ total: a target
// past the amount could get nothing, so InputSelectTargets stops offering
// targets once there are as many as the amount (InputSelectTargets.java:
// 438-439), and a total of 0 asks for none (TargetRestrictions.
// isMinTargetsChosen, TargetRestrictions.java:370-376). An unreadable total
// fails the ability when it resolves, targetChoice.err's own deferral.
func (g *Game) capDividedChoice(a *Ability, choice targetChoice) targetChoice {
	total, divided, err := g.dividedTotal(a)
	if !divided {
		return choice
	}
	if err != nil {
		return targetChoice{min: choice.min, max: choice.max, err: err}
	}
	if _, ok := a.Params.Param("DividedUpTo"); ok {
		// PlayerControllerHuman.java:2492-2494 asks how much of the total to
		// divide first: a decision this port does not ask.
		return targetChoice{min: choice.min, max: choice.max, err: fmt.Errorf("engine: DividedUpTo$ not resolvable yet")}
	}
	choice.max = min(choice.max, max(total, 0))
	choice.min = min(choice.min, choice.max)
	return choice
}

// divideAmongTargets announces a's division of its DividedAsYouChoose$ total
// among chosen (CR 601.2d), returning one share per target. The forced
// answers are taken without asking, PlayerControllerHuman.java:2495-2504's
// own: one target gets it all; as many targets as the amount get one each.
// Otherwise decider is asked (DivideAmongTargets) and the answer checked:
// one share per target, each at least one, adding up to the total. More
// targets than the amount is no legal division (PlayerControllerHuman.java:
// 2505-2506). No target chosen divides nothing (the `size > 0` guard,
// PlayerControllerHuman.java:2491).
func (g *Game) divideAmongTargets(controller PlayerController, decider PlayerID, a *Ability, chosen []EntityID) ([]dividedShare, error) {
	total, divided, err := g.dividedTotal(a)
	if !divided || len(chosen) == 0 {
		return nil, err
	}
	if err != nil {
		return nil, err
	}
	if len(chosen) > total {
		return nil, fmt.Errorf("engine: DividedAsYouChoose$: %d targets cannot each get one of %d", len(chosen), total)
	}
	amounts := make([]int, len(chosen))
	switch {
	case len(chosen) == 1:
		amounts[0] = total
	case len(chosen) == total:
		for i := range amounts {
			amounts[i] = 1
		}
	default:
		amounts = controller.DivideAmongTargets(g, decider, a.Source, chosen, total)
		if err := checkDivision(amounts, len(chosen), total); err != nil {
			return nil, err
		}
	}
	shares := make([]dividedShare, len(chosen))
	for i, e := range chosen {
		shares[i] = dividedShare{target: e, amount: amounts[i]}
	}
	return shares, nil
}

// checkDivision rejects a DivideAmongTargets answer that is not n shares of
// at least one each adding up to total (GO-7).
func checkDivision(amounts []int, n, total int) error {
	if len(amounts) != n {
		return fmt.Errorf("engine: DividedAsYouChoose$: %d shares for %d targets", len(amounts), n)
	}
	sum := 0
	for _, v := range amounts {
		if v < 1 {
			return fmt.Errorf("engine: DividedAsYouChoose$: share %d is below one", v)
		}
		sum += v
	}
	if sum != total {
		return fmt.Errorf("engine: DividedAsYouChoose$: shares add up to %d, want %d", sum, total)
	}
	return nil
}

// perTargetAmounts is the amount a damage effect gives each of targets, the
// line DamageDealEffect.java:248 and DamagePreventEffect.java:105 share:
// `sa.usesTargeting() && sa.isDividedAsYouChoose() ? sa.getDividedValue(o)
// : amount`. Every share is read before anything is dealt or prevented, so a
// target with none fails the line before it acts (GO-7): Java's Integer
// unboxing there would throw. api names the effect in the errors.
//
// Refused by name, each with no real corpus line:
//
//   - a divided line without ValidTgts$: Java's usesTargeting is false and
//     every entity takes the whole amount, not a division -- a reading this
//     port does not guess at;
//   - a divided line with Radiance$: Java's loop variable keeps the last
//     target's share and the radiant cards take that
//     (DamageDealEffect.java:248 then :263-267, DamagePreventEffect.java:105
//     then :115-119), a quirk of statement order no rule backs.
func (a *Ability) perTargetAmounts(api string, targets []EntityID, amount int) ([]int, error) {
	out := make([]int, len(targets))
	if !a.isDivided() {
		for i := range out {
			out[i] = amount
		}
		return out, nil
	}
	if _, ok := a.Params.Param("ValidTgts"); !ok {
		return nil, fmt.Errorf("engine: %s: DividedAsYouChoose$ without its own targets not resolvable yet", api)
	}
	if _, ok := a.Params.Param("Radiance"); ok || a.radiance != "" {
		return nil, fmt.Errorf("engine: %s: DividedAsYouChoose$ with Radiance$ not resolvable yet", api)
	}
	for i, e := range targets {
		share, ok := a.dividedShareOf(e)
		if !ok {
			return nil, fmt.Errorf("engine: %s: DividedAsYouChoose$: no division was announced for %s", api, describeEntity(e))
		}
		out[i] = share
	}
	return out, nil
}

// describeEntity names e for an error: "card N" or "player N".
func describeEntity(e EntityID) string {
	if id, ok := e.AsCard(); ok {
		return fmt.Sprintf("card %d", id)
	}
	id, _ := e.AsPlayer()
	return fmt.Sprintf("player %d", id)
}

// dividedShareOf is SpellAbility.getDividedValue(target): what target was
// allocated as a's targets were chosen. false when nothing was allocated to
// it -- it was not a target then.
func (a *Ability) dividedShareOf(target EntityID) (int, bool) {
	for _, s := range a.divided {
		if s.target == target {
			return s.amount, true
		}
	}
	return 0, false
}
