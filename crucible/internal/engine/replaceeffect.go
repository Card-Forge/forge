// The Replace* effects: ReplaceWith$ abilities that edit the event being
// replaced -- its damage, life, counter or token amount, its target, the
// mana it produces -- instead of substituting a different event.

package engine

//enginelint:allow id card game player ability defined amount control parts effecthelpers effect replacement manaability condition combat

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// replacementResult is ReplacementResult: what running a replacement did to
// its event.
type replacementResult uint8

const (
	// replacementNotReplaced leaves the event as it was.
	replacementNotReplaced replacementResult = iota
	// replacementUpdated lets the event happen with the edited values.
	replacementUpdated
	// replacementReplaced means the event does not happen at all.
	replacementReplaced
)

// replacementEvent is the event a Replace* effect edits: the
// OriginalParams map ReplacementHandler hands the ReplaceWith$ ability
// (SpellAbility.getReplacingObject), as a struct. amountName is the
// AbilityKey the event's number travels under -- DamageAmount (DamageDone),
// LifeGained (GainLife), CounterNum (AddCounter), TokenNum (CreateToken) --
// the name ReplaceEffect's VarName$ and ReplaceCount$ must use to reach it.
type replacementEvent struct {
	result     replacementResult
	amountName string
	amount     int

	// affected is the damaged object (DamageDone's Affected).
	affected EntityID
	// redirect and redirectAmount are the split-off half ReplaceSplitDamage
	// adds: redirectAmount of the damage goes to redirect instead.
	redirect       EntityID
	redirectAmount int

	// counterType is AddCounter's counter kind.
	counterType CounterType

	// mana is ProduceMana's produced mana.
	mana producedMana

	// card is Moved's Affected: the card entering the battlefield, what
	// Defined$ ReplacedCard names (entersascopy.go). NoCard for every other
	// event.
	card CardID

	// player and defendingPlayer are DeclareBlocker's two replacing objects
	// (ReplaceDeclareBlocker.setReplacingObjects): AbilityKey.Player, who
	// declares the blocks (Defined$ ReplacedPlayer), and AbilityKey.Affected
	// re-keyed as DefendingPlayer, whose creatures block (Defined$
	// ReplacedDefendingPlayer). NoPlayer for every other event, which is how
	// Camouflage (camouflageeffect.go) tells it is not resolving as one.
	player          PlayerID
	defendingPlayer PlayerID
	// blocks is DeclareBlocker's combat: every Block declared so far this
	// step, earlier defenders' included, which the ReplaceWith$ ability adds
	// its own to in place of the normal declaration (ADR-0035).
	blocks []Block
}

// producedMana is one production of mana as this port's mana abilities make
// it: amount units of one color, or of colorless ({C}), snow or not.
type producedMana struct {
	color     mana.Colors
	colorless bool
	snow      bool
	amount    int
}

// replaceEffectFor is the effect behind a Replace* API, for the
// replacement dispatch (replacement.go), which runs a ReplaceWith$ ability
// on the spot without a Registry. ok false for any other API.
func replaceEffectFor(api APIType) (Effect, bool) {
	switch api {
	case APIReplaceEffect:
		return replaceEffectEffect{}, true
	case APIReplaceDamage:
		return replaceDamageEffect{}, true
	case APIReplaceSplitDamage:
		return replaceSplitDamageEffect{}, true
	case APIReplaceToken:
		return replaceTokenEffect{}, true
	case APIReplaceCounter:
		return replaceCounterEffect{}, true
	case APIReplaceMana:
		return replaceManaEffect{}, true
	}
	return nil, false
}

// errNotReplacing is a Replace* effect resolved outside a replacement.
// ReplaceEffect.java dereferences the missing OriginalParams map there; the
// other five return early (`if (!sa.isReplacementAbility()) return;`).
func errNotReplacing(api string) error {
	return fmt.Errorf("engine: %s: not resolving as a replacement", api)
}

// replaceEffectEffect is ReplaceEffect.java's default "amount" VarType$:
// VarName$ names the event's number (replacementEvent.amountName) and
// VarValue$ is its new value -- a literal, a ReplaceCount$ expression over
// the old value (doubled, plus one, ...), or any amount the host resolves.
// The Card/Player/GameEntity/Map/CardSet/PlanarDice VarType$ values, which
// swap the event's object rather than its number, are rejected.
type replaceEffectEffect struct{}

func (replaceEffectEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	ev := a.replacing
	if ev == nil {
		return errNotReplacing("ReplaceEffect")
	}
	if err := rejectParams(a, "ReplaceEffect", "VarKey"); err != nil {
		return err
	}
	if !subAbilityConditionMet(g, g.Card(a.Source), a.Amounts, a.Params) {
		return nil
	}
	if t, ok := a.Params.Param("VarType"); ok && t != "amount" {
		return fmt.Errorf("engine: ReplaceEffect: VarType$ %q not resolvable yet", t)
	}
	varName, _ := a.Params.Param("VarName")
	if !strings.EqualFold(varName, ev.amountName) {
		return fmt.Errorf("engine: ReplaceEffect: VarName$ %q not resolvable yet for this event", varName)
	}
	varValue, ok := a.Params.Param("VarValue")
	if !ok {
		return fmt.Errorf("engine: ReplaceEffect: no VarValue$")
	}
	n, ok := replacingAmount(g, a, varValue, ev)
	if !ok {
		return fmt.Errorf("engine: ReplaceEffect: VarValue$ %q is not resolvable", varValue)
	}
	ev.amount = max(n, 0)
	ev.result = replacementUpdated
	return nil
}

// replacingAmount is AbilityUtils.calculateAmount for a ReplaceWith$
// ability: ReplaceCount$<amountName> reads the event's own current number
// (resolveReplaceCountAmount), anything else the host resolves as usual.
func replacingAmount(g *Game, a *Ability, value string, ev *replacementEvent) (int, bool) {
	source := g.Card(a.Source)
	if n, ok := resolveReplaceCountAmount(g, a.Amounts, source, value, ev.amount, ev.amountName); ok {
		return n, true
	}
	return resolveNamedAmount(g, a.Amounts, source, value)
}

// replaceDamageEffect is ReplaceDamageEffect.java: prevent Amount$ (default
// 1) of the damage. An Amount$ naming a Number$ SVar is a depleting shield:
// what is left is written back to the host, and an effect card whose shield
// is spent is exiled. The prevented amount is recorded as the host's
// PreventedDamage SVar. Nothing left means the damage is replaced
// outright.
type replaceDamageEffect struct{}

func (replaceDamageEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	ev := a.replacing
	if ev == nil {
		return nil
	}
	if err := rejectParams(a, "ReplaceDamage", "DivideShield"); err != nil {
		return err
	}
	if !subAbilityConditionMet(g, g.Card(a.Source), a.Amounts, a.Params) {
		return nil
	}
	if ev.amountName != "DamageAmount" {
		return fmt.Errorf("engine: ReplaceDamage: not replacing damage")
	}
	value, ok := a.Params.Param("Amount")
	if !ok {
		value = "1"
	}
	prevent, shield, ok := shieldAmount(g, a, value)
	if !ok {
		return fmt.Errorf("engine: ReplaceDamage: Amount$ %q is not resolvable", value)
	}
	dmg := ev.amount
	host := g.Card(a.Source)
	if prevent > 0 {
		n := min(dmg, prevent)
		dmg -= n
		prevent -= n
		if shield {
			if host.IsEffect && prevent <= 0 {
				g.exileEffect(host.ID)
			} else {
				setRuntimeSVar(host, value, prevent)
			}
		}
		setRuntimeSVar(host, "PreventedDamage", n)
	}
	ev.amount = dmg
	if dmg <= 0 {
		ev.amount = 0
		ev.result = replacementReplaced
		return nil
	}
	ev.result = replacementUpdated
	return nil
}

// shieldAmount resolves a prevention amount, and reports whether it is a
// depleting shield: a non-numeric value naming a Number$ SVar, either as
// the script wrote it or as an earlier use of the shield left it.
func shieldAmount(g *Game, a *Ability, value string) (n int, shield, ok bool) {
	if n, err := strconv.Atoi(value); err == nil {
		return n, false, true
	}
	host := g.Card(a.Source)
	if n, ok := host.svars[strings.ToLower(value)]; ok {
		return n, true, true
	}
	if amt, ok := a.Amounts[strings.ToLower(value)]; ok && amt.Kind == expr.Expression && strings.EqualFold(amt.Head, "Number") {
		n, err := strconv.Atoi(amt.Body)
		return n, err == nil, err == nil
	}
	n, ok = resolveNamedAmount(g, a.Amounts, host, value)
	return n, false, ok
}

// setRuntimeSVar is Card.setSVar(name, "Number$" + n): a runtime value that
// shadows the script's SVar of that name (resolveNamedAmount).
func setRuntimeSVar(c *Card, name string, n int) {
	if c.svars == nil {
		c.svars = make(map[string]int)
	}
	c.svars[strings.ToLower(name)] = n
}

// replaceSplitDamageEffect is ReplaceSplitDamageEffect.java: VarName$
// (default 1) of the damage is dealt to DamageTarget$ instead of the
// original target, which keeps the rest. An effect card whose amount is
// spent is exiled ("the next 1 damage"). Nothing left for the original
// target means its half is replaced outright.
type replaceSplitDamageEffect struct{}

func (replaceSplitDamageEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	ev := a.replacing
	if ev == nil {
		return nil
	}
	if err := rejectParams(a, "ReplaceSplitDamage", "DivideShield"); err != nil {
		return err
	}
	if !subAbilityConditionMet(g, g.Card(a.Source), a.Amounts, a.Params) {
		return nil
	}
	if ev.amountName != "DamageAmount" {
		return fmt.Errorf("engine: ReplaceSplitDamage: not replacing damage")
	}
	value, ok := a.Params.Param("VarName")
	if !ok {
		value = "1"
	}
	prevent, _, ok := shieldAmount(g, a, value)
	if !ok {
		return fmt.Errorf("engine: ReplaceSplitDamage: VarName$ %q is not resolvable", value)
	}
	spec, ok := a.Params.Param("DamageTarget")
	if !ok {
		return fmt.Errorf("engine: ReplaceSplitDamage: no DamageTarget$")
	}
	host := g.Card(a.Source)
	targets, err := definedEntities(g, a.Controller, host, spec, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ReplaceSplitDamage: DamageTarget$: %w", err)
	}
	dmg := ev.amount
	if prevent > 0 && len(targets) > 0 {
		n := min(dmg, prevent)
		dmg -= n
		prevent -= n
		if host.IsEffect && prevent <= 0 {
			g.exileEffect(host.ID)
		} else if _, err := strconv.Atoi(value); err != nil {
			setRuntimeSVar(host, value, prevent)
		}
		ev.redirect, ev.redirectAmount = targets[0], n
	}
	ev.amount = max(dmg, 0)
	if dmg <= 0 {
		ev.result = replacementReplaced
		return nil
	}
	ev.result = replacementUpdated
	return nil
}

// replaceTokenEffect is ReplaceTokenEffect.java's Type$ Amount: the number
// of tokens the matched creation makes is put through Amount$ (default
// Twice) -- Doubling Season's "twice that many". The dispatch checks
// ValidToken$ before this runs. Type$ AddToken/ReplaceToken/
// ReplaceController, which change which tokens are made or who controls
// them, are rejected.
type replaceTokenEffect struct{}

func (replaceTokenEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	ev := a.replacing
	if ev == nil {
		return errNotReplacing("ReplaceToken")
	}
	if !subAbilityConditionMet(g, g.Card(a.Source), a.Amounts, a.Params) {
		return nil
	}
	if ev.amountName != "TokenNum" {
		return fmt.Errorf("engine: ReplaceToken: not replacing token creation")
	}
	if t, _ := a.Params.Param("Type"); t != "Amount" {
		return fmt.Errorf("engine: ReplaceToken: Type$ %q not resolvable yet", t)
	}
	op, ok := a.Params.Param("Amount")
	if !ok {
		op = "Twice"
	}
	n, ok := doXMath(ev.amount, op)
	if !ok {
		return fmt.Errorf("engine: ReplaceToken: Amount$ %q not resolvable yet", op)
	}
	ev.amount = max(n, 0)
	ev.result = replacementUpdated
	return nil
}

// doXMath is AbilityUtils.doXMath for the operators a bare Amount$ names:
// Twice, Thrice, HalfUp, HalfDown, and Plus.N/Minus.N with a literal N.
func doXMath(n int, op string) (int, bool) {
	name, operand, _ := strings.Cut(op, ".")
	switch name {
	case "Twice":
		return n * 2, true
	case "Thrice":
		return n * 3, true
	case "HalfUp":
		return (n + 1) / 2, true
	case "HalfDown":
		return n / 2, true
	case "Plus", "Minus":
		k, err := strconv.Atoi(operand)
		if err != nil {
			return 0, false
		}
		if name == "Minus" {
			k = -k
		}
		return n + k, true
	}
	return 0, false
}

// replaceCounterEffect is ReplaceCounterEffect.java for one placement: the
// counters being put become Amount$, usually a ReplaceCount$CounterNum
// expression over the original number ("that many plus one", "twice that
// many"). ValidCounterType$ limits it to one kind. ChooseCounter$ only
// matters when counters come from several sources at once, which this
// port's single-source placements never do. ValidSource$ is rejected.
type replaceCounterEffect struct{}

func (replaceCounterEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	ev := a.replacing
	if ev == nil {
		return nil
	}
	if err := rejectParams(a, "ReplaceCounter", "ValidSource"); err != nil {
		return err
	}
	if !subAbilityConditionMet(g, g.Card(a.Source), a.Amounts, a.Params) {
		return nil
	}
	if ev.amountName != "CounterNum" {
		return fmt.Errorf("engine: ReplaceCounter: not replacing counters")
	}
	value, ok := a.Params.Param("Amount")
	if !ok {
		return fmt.Errorf("engine: ReplaceCounter: no Amount$")
	}
	if ct, ok := a.Params.Param("ValidCounterType"); ok && !strings.EqualFold(ct, string(ev.counterType)) {
		return nil
	}
	n, ok := replacingAmount(g, a, value, ev)
	if !ok {
		return fmt.Errorf("engine: ReplaceCounter: Amount$ %q is not resolvable", value)
	}
	ev.amount = max(n, 0)
	ev.result = replacementUpdated
	return nil
}

// replaceManaEffect is ReplaceManaEffect.java: the mana being produced
// becomes ReplaceMana$ (a single mana symbol, or Any: a color the activator
// chooses), has every unit's type replaced by ReplaceType$ (colored and
// colorless alike), has its colored units -- or only ReplaceOnly$'s --
// recolored by ReplaceColor$ (Chosen: the host's chosen color), or is
// multiplied by ReplaceAmount$.
type replaceManaEffect struct{}

func (replaceManaEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	ev := a.replacing
	if ev == nil {
		return nil
	}
	if !subAbilityConditionMet(g, g.Card(a.Source), a.Amounts, a.Params) {
		return nil
	}
	if ev.amountName != "Mana" {
		return fmt.Errorf("engine: ReplaceMana: not replacing mana")
	}
	m := ev.mana
	switch {
	case hasParam(a, "ReplaceMana"):
		v, _ := a.Params.Param("ReplaceMana")
		c, colorless, err := replacementManaType(g, a, controller, v)
		if err != nil {
			return err
		}
		m = producedMana{color: c, colorless: colorless, snow: m.snow, amount: 1}
	case hasParam(a, "ReplaceType"):
		v, _ := a.Params.Param("ReplaceType")
		c, colorless, err := replacementManaType(g, a, controller, v)
		if err != nil {
			return err
		}
		m.color, m.colorless = c, colorless
	case hasParam(a, "ReplaceColor"):
		v, _ := a.Params.Param("ReplaceColor")
		var c mana.Colors
		colorless := false
		if v == "Chosen" {
			chosen := g.Card(a.Source).Memory.ChosenColors()
			if chosen.Count() != 1 {
				// Java keeps the literal "Chosen" when nothing was chosen,
				// which names no mana at all.
				return fmt.Errorf("engine: ReplaceMana: ReplaceColor$ Chosen with no chosen color")
			}
			c = chosen
		} else {
			var err error
			if c, colorless, err = manaTypeByName(v); err != nil {
				return err
			}
		}
		if m.colorless {
			// ReplaceColor$ recolors WUBRG units only; {C} stays.
			break
		}
		if only, ok := a.Params.Param("ReplaceOnly"); ok {
			oc, ocless, err := manaTypeByName(only)
			if err != nil || ocless || oc != m.color {
				break
			}
		}
		m.color, m.colorless = c, colorless
	case hasParam(a, "ReplaceAmount"):
		v, _ := a.Params.Param("ReplaceAmount")
		k, err := strconv.Atoi(v)
		if err != nil {
			return fmt.Errorf("engine: ReplaceMana: ReplaceAmount$ %q is not a number", v)
		}
		m.amount *= k
	default:
		return fmt.Errorf("engine: ReplaceMana: no ReplaceMana$/ReplaceType$/ReplaceColor$/ReplaceAmount$")
	}
	ev.mana = m
	ev.result = replacementUpdated
	return nil
}

// replacementManaType reads a single mana type -- a letter, a color word, or
// Any (a color the activator chooses, WUBRG).
func replacementManaType(g *Game, a *Ability, controller PlayerController, v string) (mana.Colors, bool, error) {
	if v != "Any" {
		return manaTypeByName(v)
	}
	if controller == nil {
		return 0, false, fmt.Errorf("engine: ReplaceMana: Any needs a controller to choose")
	}
	c := controller.ChooseManaColor(g, a.Controller, a.Source, mana.AllColors)
	if c.Count() != 1 {
		return 0, false, fmt.Errorf("engine: ReplaceMana: ChooseManaColor did not answer with exactly one color")
	}
	return c, false, nil
}

// manaTypeByName is MagicColor.toShortString: a WUBRGC letter or a color
// word.
func manaTypeByName(v string) (mana.Colors, bool, error) {
	switch strings.ToLower(v) {
	case "white":
		v = "W"
	case "blue":
		v = "U"
	case "black":
		v = "B"
	case "red":
		v = "R"
	case "green":
		v = "G"
	case "colorless":
		v = "C"
	}
	c, colorless, ok := producedManaColor(v)
	if !ok {
		return 0, false, fmt.Errorf("engine: ReplaceMana: mana type %q not resolvable yet", v)
	}
	return c, colorless, nil
}
