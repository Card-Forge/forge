// Mana: CR 106.1, a resolving (not activated) mana burst -- 263 real
// (SP|DB)$ Mana lines (24 SP$, the ritual-spell shape -- Dark Ritual and its
// kin -- 239 DB$, a sub-ability chain), distinct from A:AB$ Mana's own 2,134
// real lines (a permanent's own printed mana ability, ActivateManaAbility,
// activatemanaability.go). Produced$ dispatches identically to that file's
// own producedManaColor/parseComboColors -- reused outright rather than
// re-derived, since a resolving Mana line's own Produced$ vocabulary is the
// identical CR 605.1 shape an activated one already reads. Amount$ resolves
// through resolveNamedAmount (amount.go) the same as every other effect's
// own numeric param, default 1 (AbilityUtils.calculateAmount's own
// `sa.hasParam("Amount") ? ... : 1`). Snow mana (colorless-and-Snow /
// colored-and-Snow, ActivateManaAbility's own dual AddSnowColorless/AddSnow
// branches) is not ported: a resolving ritual-shaped Mana line's own host is
// essentially never itself a Snow permanent in the real corpus this port has
// read, a narrow simplification rather than a silently dropped real shape.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/ManaEffect.java's
// resolve, trimmed hard: that file also handles isComboMana's own
// specifyManaCombo multi-symbol-at-once choice (a different controller
// decision from the single-color-per-target ChooseManaColor this file
// reuses), isSpecialMana's own EnchantedManaCost/EachColoredManaSymbol/
// DoubleManaInPool/LastNotedType/EachColorAmong reference vocabulary, and
// Chooser$'s own delegated-decider override -- 0 real (SP|DB)$ Mana lines
// name Chooser$ at all, so nothing here is silently ignored by skipping it.

package engine

//enginelint:allow id card game player ability defined condition control amount manaability replaceeffect

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/mana"
)

// manaUnresolvedParams names ManaEffect's own params this port does not
// evaluate. Every one fails the whole line loudly (PORT-8/GO-7):
// RestrictValid$ (20) -- a mana-pool spending restriction this port's own
// Pool has no tag mechanism for, manaAbilityAllowedParams' own identical
// gap (activatemanaability.go); Each$ (1) -- isComboMana's own
// per-iteration-color-count shape, a further branch past the single-choice
// dispatch this file resolves; CombatMana$ (1) -- unclear semantics on a
// resolving line, not worth guessing at; Optional$ (1) -- an interactive
// "would you like to add mana" confirm, the identical gap Sacrifice's/
// Discard's/Mill's own Optional$ already document; Condition$/
// ConditionDefined$ (0/6) -- condition.go's own subAbilityConditionMet
// would otherwise silently no-op a card naming either, dealDamageEffect's
// own identical reasoning.
var manaUnresolvedParams = [...]string{
	"RestrictValid", "Each", "CombatMana", "Optional",
	"Condition"}

// manaEffect resolves Mode$/DB$/SP$ Mana. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type manaEffect struct{}

func (manaEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range manaUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Mana: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	produced, ok := a.Params.Param("Produced")
	if !ok {
		return fmt.Errorf("engine: Mana: Produced$ missing")
	}

	amountParam, ok := a.Params.Param("Amount")
	if !ok {
		amountParam = "1"
	}
	amount, ok := resolveNamedAmount(g, a.Amounts, source, amountParam)
	if !ok {
		return fmt.Errorf("engine: Mana: Amount$ %q is not resolvable", amountParam)
	}
	if amount <= 0 {
		return nil
	}

	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Mana: %w", err)
	}

	// The host made the mana (ManaEffect's AbilityManaPart source), ADR-0042.
	src := g.manaSourceOf(a.Source)
	for _, pid := range players {
		pool := &g.Player(pid).ManaPool
		switch {
		case produced == "Any":
			color := controller.ChooseManaColor(g, pid, a.Source, mana.AllColors)
			if color.Count() != 1 {
				return fmt.Errorf("engine: Mana: ChooseManaColor did not answer with exactly one color")
			}
			pool.addProduced(producedMana{color: color, amount: amount}, src)
		case strings.HasPrefix(produced, "Combo "):
			options, comboOK := parseComboColors(produced)
			if !comboOK {
				return fmt.Errorf("engine: Mana: Produced$ %q not resolvable yet", produced)
			}
			color := controller.ChooseManaColor(g, pid, a.Source, options)
			if color.Count() != 1 || !options.Has(color) {
				return fmt.Errorf("engine: Mana: ChooseManaColor did not answer with one color from the offered set")
			}
			pool.addProduced(producedMana{color: color, amount: amount}, src)
		default:
			color, colorless, colorOK := producedManaColor(produced)
			if !colorOK {
				return fmt.Errorf("engine: Mana: Produced$ %q not resolvable yet", produced)
			}
			pool.addProduced(producedMana{color: color, colorless: colorless, amount: amount}, src)
		}
	}
	return nil
}
