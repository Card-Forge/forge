package engine

//enginelint:allow card game ability condition control amount valid

import "fmt"

// branchEffect is BranchEffect.java: BranchConditionSVar$ is compared with
// BranchConditionSVarCompare$ (default GE1) and TrueSubAbility$ or
// FalseSubAbility$ resolves accordingly (either may be absent). 100 real
// lines.
type branchEffect struct{}

func (branchEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	svar, _ := a.Params.Param("BranchConditionSVar")
	left, ok := resolveNamedAmount(g, a.Amounts, source, svar)
	if !ok {
		return fmt.Errorf("engine: Branch: BranchConditionSVar$ %q not resolvable yet", svar)
	}
	compare, ok := a.Params.Param("BranchConditionSVarCompare")
	if !ok {
		compare = "GE1"
	}
	if len(compare) < 3 {
		return fmt.Errorf("engine: Branch: BranchConditionSVarCompare$ %q malformed", compare)
	}
	right, ok := resolveNamedAmount(g, a.Amounts, source, compare[2:])
	if !ok {
		return fmt.Errorf("engine: Branch: BranchConditionSVarCompare$ %q not resolvable yet", compare)
	}
	key := "FalseSubAbility"
	if compareOp(left, compare[:2], right) {
		key = "TrueSubAbility"
	}
	return g.resolveAdditionalKey(a, controller, key)
}
