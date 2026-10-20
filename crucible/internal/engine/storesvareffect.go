package engine

//enginelint:allow ability amount card condition control effecthelpers game

import (
	"fmt"
	"strconv"
	"strings"
)

// storeSVarEffect is StoreSVarEffect.java: computes a number and stores it
// as the host's SVar$ (Card.setSVar(key, "Number$N")), which later reads
// of that SVar name see (resolveNamedAmount). Type$ Number parses
// Expression$; Calculate resolves it as an amount (calculateAmount);
// CountSVar is xCount on "SVar$<Expression$>" -- an SVar with at most the
// Plus/Minus/Twice suffix; AdditiveForEach adds Expression$ to the SVar's
// current value. Type$ Count/Targeted/Triggered and SVar$ EachPlayer read
// shapes this port has no evaluator for and fail closed. Java also copies
// the value onto the resolving ability chain's own SVars; the host's copy
// is the one every later read in this port goes through.
type storeSVarEffect struct{}

func (storeSVarEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "StoreSVar"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	key, _ := a.Params.Param("SVar")
	kind, _ := a.Params.Param("Type")
	expression, _ := a.Params.Param("Expression")
	if key == "" || kind == "" || expression == "" {
		return fmt.Errorf("engine: StoreSVar: SVar$, Type$ and Expression$ are required")
	}
	if key == "EachPlayer" {
		return fmt.Errorf("engine: StoreSVar: SVar$ EachPlayer not resolvable yet")
	}
	var value int
	var ok bool
	switch {
	case kind == "Number":
		n, err := strconv.Atoi(strings.TrimSpace(expression))
		value, ok = n, err == nil
	case kind == "Calculate":
		value, ok = resolveNamedAmount(g, a.Amounts, source, expression)
	case kind == "CountSVar":
		value, ok = countSVar(g, a, source, expression)
	case strings.HasPrefix(kind, "AdditiveForEach"):
		current, _ := resolveNamedAmount(g, a.Amounts, source, key)
		var add int
		add, ok = resolveNamedAmount(g, a.Amounts, source, expression)
		value = current + add
	default:
		return fmt.Errorf("engine: StoreSVar: Type$ %q not resolvable yet", kind)
	}
	if !ok {
		return fmt.Errorf("engine: StoreSVar: Expression$ %q is not resolvable", expression)
	}
	if source.svars == nil {
		source.svars = make(map[string]int)
	}
	source.svars[strings.ToLower(key)] = value
	return nil
}

// countSVar is xCount(source, "SVar$" + expression): the named SVar, then
// an optional Plus.N/Minus.N/Twice suffix (doXMath).
func countSVar(g *Game, a *Ability, host *Card, expression string) (int, bool) {
	name, op, hasOp := strings.Cut(expression, "/")
	v, ok := resolveNamedAmount(g, a.Amounts, host, name)
	if !ok || !hasOp {
		return v, ok
	}
	opName, operand, _ := strings.Cut(op, ".")
	switch opName {
	case "Twice":
		return v * 2, true
	case "Plus", "Minus":
		n, ok := resolveNamedAmount(g, a.Amounts, host, operand)
		if !ok {
			return 0, false
		}
		if opName == "Plus" {
			return v + n, true
		}
		return v - n, true
	}
	return 0, false
}
