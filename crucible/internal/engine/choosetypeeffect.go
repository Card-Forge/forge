package engine

//enginelint:allow control game ability effecthelpers card condition defined parts

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// chooseTypeEffect is ChooseTypeEffect.java: each target player (the
// activator by default) chooses a type from Type$'s list -- every core type
// for Card, every creature/basic land/nonbasic land/land/planeswalker type
// for the others -- or from ValidTypes$ when given, less InvalidTypes$. The
// host records it (setChosenType, or setChosenType2 with ChooseType2$).
// AtRandom$ picks with Aggregates.random, only over an explicit ValidTypes$
// list: Java's subtype lists are HashSet iterations (CardType.Constant,
// CardType.java:871-873) whose order this port does not reproduce. Subtype
// lists come from the game DB's type vocabulary and are offered sorted.
// Secretly$ writes the same chosen-type slot without updating the view
// (Card.setSecretChosenType, Card.java:2214-2216).
type chooseTypeEffect struct{}

func (chooseTypeEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "ChooseType", "TypesFromDefined", "MostPrevalentInDefinedZone",
		"Note", "ChooseNoted"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	kind, _ := a.Params.Param("Type")
	var types []string
	if raw, ok := a.Params.Param("ValidTypes"); ok {
		types = strings.Split(raw, ",")
	} else {
		if hasParam(a, "AtRandom") {
			return fmt.Errorf("engine: ChooseType: AtRandom$ over Type$ %q not resolvable (no stable order)", kind)
		}
		var err error
		types, err = chooseTypeOptions(g, kind)
		if err != nil {
			return err
		}
	}
	if raw, ok := a.Params.Param("InvalidTypes"); ok {
		drop := strings.Split(raw, ",")
		kept := types[:0:0]
		for _, t := range types {
			if !containsString(drop, t) {
				kept = append(kept, t)
			}
		}
		types = kept
	}
	if len(types) == 0 {
		return fmt.Errorf("engine: ChooseType: no types to choose from")
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ChooseType: %w", err)
	}
	_, second := a.Params.Param("ChooseType2")
	for _, p := range players {
		var i int
		if hasParam(a, "AtRandom") {
			i = g.randomIndex(len(types))
		} else {
			i = controller.ChooseOption(g, p, a.Source, types)
			if i < 0 || i >= len(types) {
				return fmt.Errorf("engine: ChooseType: choice %d out of range", i)
			}
		}
		source.Memory.SetChosenType(types[i], second)
	}
	return nil
}

// chooseTypeOptions is ChooseTypeEffect's Type$ switch for the lists it
// builds itself.
func chooseTypeOptions(g *Game, kind string) ([]string, error) {
	if kind == "Card" {
		out := make([]string, 0, 15)
		for t := cardtype.Kindred; t <= cardtype.Vanguard; t++ {
			out = append(out, t.String())
		}
		return out, nil
	}
	reg := g.db.Types()
	if reg == nil {
		return nil, fmt.Errorf("engine: ChooseType: Type$ %q needs the type vocabulary", kind)
	}
	switch kind {
	case "Creature":
		return reg.Members(cardtype.CategoryCreature), nil
	case "Basic Land":
		return reg.Members(cardtype.CategoryBasic), nil
	case "Nonbasic Land":
		return reg.Members(cardtype.CategoryLand), nil
	case "Land":
		return append(reg.Members(cardtype.CategoryBasic), reg.Members(cardtype.CategoryLand)...), nil
	case "Planeswalker":
		return reg.Members(cardtype.CategoryPlaneswalker), nil
	}
	return nil, fmt.Errorf("engine: ChooseType: Type$ %q not resolvable yet", kind)
}
