package engine

//enginelint:allow card game ability defined control amount replaceeffect id

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// manaReflectedAllowedParams names every key ManaReflected resolves or can
// ignore; any other fails the line (PORT-8, GO-7). Valid$ (25 corpus lines)
// feeds the Produce/Is walks, not resolved; Produced$ (1, Omnath's
// "Combo") is isComboMana's specifyManaCombo split; RestrictValid$ tags
// mana this port's Pool cannot tag; IsPresent$/Condition* (1 each) are
// restrictions with no gate here. SpellDescription$/StackDescription$
// describe the ability to a human. No subAbilityConditionMet call below,
// unlike other effects: Condition$/ConditionDefined$ hold no key here, so a
// line naming either already fails the param check rather than silently
// reading as "always met" -- the allow-list stands in for the usual gate.
var manaReflectedAllowedParams = map[string]bool{
	"db": true, "colorortype": true, "reflectproperty": true, "defined": true, "amount": true,
	"subability": true, "spelldescription": true, "stackdescription": true,
}

// manaReflectedEffect is ManaReflectedEffect.java: add one mana of a type
// another source produced or could produce (CR 106.7). Only the
// ReflectProperty$ Produced shape resolves -- 22 of 47 corpus lines, every
// one the DB$ of a Static$ True Mode$ TapsForMana trigger or chained under
// one (Mana Flare, Heartbeat of Spring, Zendikar Resurgent, Overabundance):
// "that player adds one mana of any type that land produced". The colors
// are the triggering mana (CardUtil.java:295-307, AbilityKey.Produced,
// recorded by checkTapsForManaTriggers, trigger.go, as the mana after
// replacement).
//
// ReflectProperty$ Produce and Is are refused. Both are A:AB$ mana
// abilities (Reflecting Pool, Exotic Orchard, Fellwar Stone, Meteor
// Crater) that belong in ActivateManaAbility, not a Registry resolve, and
// Produce's recursive walk carries a Forge bug: CardUtil.java:345 passes
// the outer ability as the nested frame's abMana, so a reflected
// ManaReflected ability's Valid$ resolves against the reflecting card
// (forge-java-defects.md). Not reachable while Produce is refused.
//
// Every production in this port is one type -- a basic land's one color,
// a mana ability's single Produced$ symbol or chosen color -- so the
// reflected set holds at most one type and generatedReflectedMana's
// chooseColor/chooseColorAllowColorless menu (ManaReflectedEffect.java:99,
// :108) is never reached. That menu has no counterpart here --
// ChooseManaColor has no colorless member -- so a production of two types
// would need one first.
//
// No ProduceMana replacement applies: ReplaceProduceMana's default ValidSA$
// "Activated.hasTapCost+ManaAbility" never matches a triggered root, and
// AbilityManaPart.tapsForMana returns at once for the same root, so no
// TapsForMana trigger fires off this mana either. The mana is snow when the
// reflecting host is (Mana.isSnow reads the source card).
type manaReflectedEffect struct{}

func (manaReflectedEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	for _, p := range a.Params.Params {
		if !manaReflectedAllowedParams[strings.ToLower(p.Key)] {
			return fmt.Errorf("engine: ManaReflected: %s$ not resolvable yet", p.Key)
		}
	}
	if property, _ := a.Params.Param("ReflectProperty"); property != "Produced" {
		return fmt.Errorf("engine: ManaReflected: ReflectProperty$ %q not resolvable yet", property)
	}
	colorOrType, _ := a.Params.Param("ColorOrType")
	if colorOrType != "Color" && colorOrType != "Type" {
		return fmt.Errorf("engine: ManaReflected: ColorOrType$ %q not resolvable", colorOrType)
	}
	// Java reads the root ability's triggering Produced and throws on a
	// null one; a line reached outside Mode$ TapsForMana has none.
	// checkTapsForManaTriggers (trigger.go) is the only writer of
	// a.triggered, and always sets activator to the tapping player -- unlike
	// produced.amount, which a ProduceMana replacement can legitimately
	// zero out (a land whose mana was replaced away still fired the
	// trigger), activator is never NoPlayer for a real call, so it is the
	// reliable "was this ever pushed through the trigger" signal.
	if a.triggered.activator == NoPlayer {
		return fmt.Errorf("engine: ManaReflected: ReflectProperty$ Produced: the trigger recorded no produced mana")
	}
	source := g.Card(a.Source)
	amount := 1
	if amountText, ok := a.Params.Param("Amount"); ok {
		n, resolved := resolveNamedAmount(g, a.Amounts, source, amountText)
		if !resolved {
			return fmt.Errorf("engine: ManaReflected: Amount$ %q is not resolvable", amountText)
		}
		amount = n
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ManaReflected: %w", err)
	}

	m, ok := reflectedProducedMana(a.triggered.produced, colorOrType == "Type")
	if !ok || amount <= 0 {
		// No reflectable type: generatedReflectedMana answers "0", which
		// produceMana adds as zero colorless mana.
		return nil
	}
	m.amount = amount
	m.snow = source.Type().HasSupertype(cardtype.Snow)
	for _, pid := range players {
		g.addProducedMana(pid, m, g.manaSourceOf(a.Source))
	}
	return nil
}

// reflectedProducedMana is getReflectableManaColors' Produced branch
// (CardUtil.java:295-307) over this port's one-type production: produced's
// color, or colorless when withColorless (ColorOrType$ Type) and the mana
// was colorless. ok is false when nothing is reflectable -- no mana
// produced, or colorless mana under ColorOrType$ Color.
func reflectedProducedMana(produced producedMana, withColorless bool) (producedMana, bool) {
	switch {
	case produced.amount <= 0:
		return producedMana{}, false
	case produced.colorless:
		return producedMana{colorless: true}, withColorless
	}
	return producedMana{color: produced.color}, true
}
