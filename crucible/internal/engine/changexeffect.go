package engine

//enginelint:allow ability amount card condition control effecthelpers game id replaceeffect stack

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/expr"
)

// changeXEffect is ChangeXEffect.java: the value of X a spell or ability on
// the stack was cast or activated with becomes Value$ (Unbound
// Flourishing's "double the value of X", Glava's "the value of X becomes
// 5"). The mana already paid is untouched; only what later reads X sees the
// new value. A spell whose X was never announced (Java's null
// xManaCostPaid: no {X} in its cost, or cast without paying it) keeps
// none.
//
// Both corpus lines name Defined$ TriggeredSpellAbility, the stack item a
// Mode$ SpellCast trigger recorded (triggeredObjects.spellAbility,
// trigger.go). Java writes the new X onto the SpellAbility object and its
// host's cast SA (ChangeXEffect.java:22-29, the two differ only for a
// SpellAbilityStackInstance, which this port does not have: the stack item
// is the cast ability). A spell already gone from the stack gets nothing:
// Java would still write to its orphaned SpellAbility, which no later
// reader in this port could see.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/ChangeXEffect.java's
// resolve.
type changeXEffect struct{}

// changeXUnresolvedParams are shapes getTargetSpells reads that no ChangeX
// corpus line uses, each rejected before acting (PORT-8, GO-7): a targeted
// spell (ValidTgts$/TargetType$) and Condition$/ConditionDefined$, which
// subAbilityConditionMet reads as never met, silently.
var changeXUnresolvedParams = [...]string{"ValidTgts", "TargetType", "Condition", "ConditionDefined"}

func (changeXEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectParams(a, "ChangeX", changeXUnresolvedParams[:]...); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	defined, _ := a.Params.Param("Defined")
	if defined != "TriggeredSpellAbility" {
		// Parent, Targeted, Remembered and the other
		// getDefinedSpellAbilities shapes have no ChangeX corpus line.
		return fmt.Errorf("engine: ChangeX: Defined$ %q not resolvable yet", defined)
	}
	if a.triggered.spellAbility == NoStackItem {
		return fmt.Errorf("engine: ChangeX: Defined$ TriggeredSpellAbility: no triggering spell recorded")
	}
	item, ok := g.stackItem(a.triggered.spellAbility)
	if !ok {
		return nil
	}
	// Java computes Value$ once per spell, before its null check.
	value, err := changeXValue(g, a, source)
	if err != nil {
		return err
	}
	if item.hasXManaCostPaid {
		item.xManaCostPaid = value
	}
	return nil
}

// changeXValue is AbilityUtils.calculateAmount over Value$ for the shapes
// ChangeX's corpus writes: a literal (Glava's 5), a named SVar
// (resolveNamedAmount), or TriggeredSpellAbility>Count$xPaid with an
// optional doXMath operator (Unbound Flourishing's /Twice) -- the X the
// triggering spell announced, 0 when it announced none
// (AbilityUtils.java:1636-1637's root.getXManaCostPaid(), falling through
// to its final doXMath(0) at :1688). resolveAmount itself has no ability to
// read an xPaid from, so the context-switched head is read here. Any other
// shape is an error, never 0 (GO-7).
func changeXValue(g *Game, a *Ability, source *Card) (int, error) {
	raw, ok := a.Params.Param("Value")
	if !ok {
		return 0, fmt.Errorf("engine: ChangeX: no Value$")
	}
	amt := expr.Parse(raw)
	switch amt.Kind {
	case expr.Literal:
		return amt.Value, nil
	case expr.Reference:
		if n, ok := resolveNamedAmount(g, a.Amounts, source, amt.Name); ok {
			if amt.Negative {
				n = -n
			}
			return n, nil
		}
	case expr.Expression:
		if amt.Context == "TriggeredSpellAbility>" && amt.Head == "Count" && strings.EqualFold(amt.Body, "xPaid") {
			n := 0
			if item, ok := g.stackItem(a.triggered.spellAbility); ok && item.hasXManaCostPaid {
				n = item.xManaCostPaid
			}
			if amt.Op != nil {
				op := amt.Op.Name
				if amt.Op.Operand != "" {
					op += "." + amt.Op.Operand
				}
				if n, ok = doXMath(n, op); !ok {
					return 0, fmt.Errorf("engine: ChangeX: Value$ %q: operator not resolvable yet", raw)
				}
			}
			if amt.Negative {
				n = -n
			}
			return n, nil
		}
	}
	return 0, fmt.Errorf("engine: ChangeX: Value$ %q not resolvable yet", raw)
}

// XManaCostPaid is SpellAbility.getXManaCostPaid: the value of X announced
// for a's own mana cost, and whether one was announced at all -- false is
// Java's null, a cost with no X or one not paid (a WithoutManaCost$ cast).
func (a *Ability) XManaCostPaid() (int, bool) {
	return a.xManaCostPaid, a.hasXManaCostPaid
}
