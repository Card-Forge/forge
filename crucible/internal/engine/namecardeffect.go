package engine

//enginelint:allow game ability control effecthelpers card condition parts defined player amount

import (
	"fmt"
	"sort"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// nameCardEffect is ChooseCardNameEffect.java: each target player (the
// activator by default) names a card. The names on offer are
// ChooseFromList$'s (less the host's earlier picks with ExcludeChosen$), or
// every card in the game's DB whose front face passes ValidCards$ (default
// Card) under CardFacePredicates.valid. The host adds the name to its named
// cards (addNamedCard) and the player records it as their named card.
// AtRandom$ picks with Aggregates.random from a ChooseFromList$ list;
// AtRandom$ over the whole DB (a stream collector's random draw) is not
// resolved. ChooseFromDefinedCards$ offers the names of those cards passing
// ValidCards$ (a split card both halves), sorted (ChooseCardNameEffect.java:
// 62-78).
type nameCardEffect struct{}

func (nameCardEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "NameCard"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	_, random := a.Params.Param("AtRandom")
	var names []string
	if raw, ok := a.Params.Param("ChooseFromDefinedCards"); ok {
		if random {
			// Java's defined branch ignores AtRandom$ for the pick yet skips
			// the player's named card for it (:136-138); no card combines them.
			return fmt.Errorf("engine: NameCard: AtRandom$ with ChooseFromDefinedCards$ not resolvable yet")
		}
		var err error
		if names, err = definedCardNames(g, a, source, raw); err != nil {
			return err
		}
	} else if raw, ok := a.Params.Param("ChooseFromList"); ok {
		_, exclude := a.Params.Param("ExcludeChosen")
		for _, n := range strings.Split(raw, ",") {
			n = strings.ReplaceAll(n, ";", ",")
			if exclude && containsString(source.Memory.NamedCards(), n) {
				continue
			}
			names = append(names, n)
		}
	} else {
		if random {
			return fmt.Errorf("engine: NameCard: AtRandom$ over every card not resolvable yet")
		}
		spec, ok := a.Params.Param("ValidCards")
		if !ok {
			spec = "Card"
		}
		var err error
		names, err = nameableCards(g, a, source, spec)
		if err != nil {
			return err
		}
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: NameCard: %w", err)
	}
	for _, p := range players {
		if g.Player(p).Lost {
			continue
		}
		chosen := ""
		switch {
		case len(names) == 0:
		case random:
			chosen = names[g.randomIndex(len(names))]
		default:
			i := controller.ChooseOption(g, p, a.Source, names)
			if i < 0 || i >= len(names) {
				return fmt.Errorf("engine: NameCard: choice %d out of range", i)
			}
			chosen = names[i]
		}
		if chosen != "" {
			source.Memory.AddNamedCard(chosen)
		}
		if !random {
			g.Player(p).NamedCard = chosen
		}
	}
	return nil
}

// definedCardNames is ChooseCardNameEffect's ChooseFromDefinedCards$ list
// (ChooseCardNameEffect.java:63-77): the defined cards passing ValidCards$
// (default Card, the host's controller as You), each card's main face name
// once and a split card's other half too, sorted by name (CardFace.compareTo).
func definedCardNames(g *Game, a *Ability, source *Card, defined string) ([]string, error) {
	cards, err := definedCardsInGame(g, a.Controller, source, defined, a.refs())
	if err != nil {
		return nil, fmt.Errorf("engine: NameCard: ChooseFromDefinedCards$: %w", err)
	}
	spec, ok := a.Params.Param("ValidCards")
	if !ok {
		spec = "Card"
	}
	var names []string
	for _, id := range filterValid(g, cards, spec, source.Controller(), source.ID) {
		def := g.Card(id).Def
		if def == nil {
			continue
		}
		if !containsString(names, def.Faces[0].Name) {
			names = append(names, def.Faces[0].Name)
			if def.SplitType == carddb.SplitSplit && def.Faces[1].Name != "" && !containsString(names, def.Faces[1].Name) {
				names = append(names, def.Faces[1].Name)
			}
		}
	}
	sort.Strings(names)
	return names, nil
}

// nameableCards lists the DB's card names whose front face passes spec
// (CardFacePredicates.ValidPredicate), in the DB's order. A cmcEQ operand
// that is not a number is resolved first, the substitution Java makes.
func nameableCards(g *Game, a *Ability, source *Card, spec string) ([]string, error) {
	if g.db.Len() == 0 {
		return nil, fmt.Errorf("engine: NameCard: no card database to name from")
	}
	if strings.Contains(spec, "ManaCost=") {
		return nil, fmt.Errorf("engine: NameCard: ValidCards$ %q not resolvable yet", spec)
	}
	if before, operand, ok := strings.Cut(spec, "cmcEQ"); ok {
		if _, err := strconv.Atoi(operand); err != nil {
			n, ok := resolveNamedAmount(g, a.Amounts, source, operand)
			if !ok {
				return nil, fmt.Errorf("engine: NameCard: cmcEQ%s not resolvable", operand)
			}
			spec = before + "cmcEQ" + strconv.Itoa(n)
		}
	}
	var out []string
	for _, name := range g.db.Names() {
		c, _ := g.db.Card(name)
		ok, err := faceValid(&c.Faces[0], spec)
		if err != nil {
			return nil, err
		}
		if ok {
			out = append(out, name)
		}
	}
	return out, nil
}

// faceValid is CardFacePredicates.ValidPredicate.test: the base is Card,
// Permanent (not an instant or sorcery) or a type word; each "+" property
// is cmcEQ<n>, non<prop>, or a type word.
func faceValid(f *compile.Face, spec string) (bool, error) {
	base, props, _ := strings.Cut(spec, ".")
	switch base {
	case "Card":
	case "Permanent":
		if f.Type.HasStringType("Instant") || f.Type.HasStringType("Sorcery") {
			return false, nil
		}
	default:
		if !f.Type.HasStringType(base) {
			return false, nil
		}
	}
	if props == "" {
		return true, nil
	}
	for _, m := range strings.Split(props, "+") {
		if rest, ok := strings.CutPrefix(m, "cmcEQ"); ok {
			n, err := strconv.Atoi(rest)
			if err != nil {
				return false, fmt.Errorf("engine: NameCard: %q not resolvable", m)
			}
			if f.ManaCost.CMC() != n {
				return false, nil
			}
			continue
		}
		if !faceHasProperty(f, m) {
			return false, nil
		}
	}
	return true, nil
}

func faceHasProperty(f *compile.Face, v string) bool {
	if rest, ok := strings.CutPrefix(v, "non"); ok {
		return !faceHasProperty(f, rest)
	}
	return f.Type.HasStringType(v)
}
