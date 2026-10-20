package engine

//enginelint:allow ability animate card condition control defined effecthelpers game parts

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// protectionEffect is ProtectEffect.java: each targeted or Defined$ card on
// the battlefield gains protection until end of turn (or for good with
// Duration$ Permanent) -- "Protection from <color>" for a color,
// "Protection:<type>" for a card type (CardType.isACardType). Gains$ is a
// comma list, ChosenColor (the host's chosen colors), or Choice: the
// activator picks one of Choices$ (AnyColor expands to the five colors,
// CardType to every card type). Choser$ and Gains$ Defined... fail
// closed. Radiance$ adds the permanents sharing a color with a target.
type protectionEffect struct{}

func (protectionEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "Protection", "Choser", "Condition"); err != nil {
		return err
	}
	permanent, until, err := animateDuration(a, "Protection")
	if err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	gains, err := protectionGains(g, a, controller, "Protection")
	if err != nil || len(gains) == 0 {
		return err
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Protection: %w", err)
	}
	// Radiance$ (ProtectEffect.java:141).
	cards = append(cards, g.radianceCards(a)...)
	if err := g.animateCards(a, animateRecord{Permanent: permanent, until: until, AddKeywords: protectionKeywords(gains, true)}, cards); err != nil {
		return err
	}
	return nil
}

// protectionGains is the Gains$ half both Protect effects share: nil (and
// no error) when a Choice finds nothing to choose.
func protectionGains(g *Game, a *Ability, controller PlayerController, api string) ([]string, error) {
	raw, ok := a.Params.Param("Gains")
	if !ok {
		return nil, fmt.Errorf("engine: %s: no Gains$", api)
	}
	switch {
	case strings.Contains(raw, "Choice"):
		choices := protectionChoices(a)
		if len(choices) == 0 {
			return nil, nil
		}
		i := controller.ChooseProtectionType(g, a.Controller, a.Source, choices)
		if i < 0 || i >= len(choices) {
			return nil, fmt.Errorf("engine: %s: protection choice %d out of %d", api, i, len(choices))
		}
		return []string{choices[i]}, nil
	case raw == "ChosenColor":
		chosen := g.Card(a.Source).Memory.ChosenColors()
		var out []string
		for i, c := range [...]mana.Colors{mana.White, mana.Blue, mana.Black, mana.Red, mana.Green} {
			if chosen.Has(c) {
				out = append(out, [...]string{"white", "blue", "black", "red", "green"}[i])
			}
		}
		return out, nil
	case strings.HasPrefix(raw, "Defined"), raw == "TargetedCardColor":
		return nil, fmt.Errorf("engine: %s: Gains$ %q not resolvable yet", api, raw)
	default:
		return strings.Split(raw, ","), nil
	}
}

// protectionChoices is ProtectEffect.getProtectionList for Gains$ Choice.
func protectionChoices(a *Ability) []string {
	choices, _ := a.Params.Param("Choices")
	var out []string
	if strings.Contains(choices, "AnyColor") {
		out = append(out, "white", "blue", "black", "red", "green")
		choices = strings.ReplaceAll(strings.ReplaceAll(choices, "AnyColor,", ""), "AnyColor", "")
	} else if strings.Contains(choices, "CardType") {
		choices = strings.Join(cardtype.CoreTypeNames(), ",")
	}
	for _, c := range strings.Split(choices, ",") {
		if c != "" {
			out = append(out, c)
		}
	}
	return out
}

// protectionKeywords turns gains into keyword lines. cardTypes is
// ProtectEffect's isACardType split into "Protection:<type>";
// ProtectAllEffect writes "Protection from" for everything.
func protectionKeywords(gains []string, cardTypes bool) []string {
	var out []string
	for _, gain := range gains {
		if _, isType := cardtype.CoreTypeFromName(gain); isType && cardTypes {
			out = append(out, "Protection:"+gain)
		} else {
			out = append(out, "Protection from "+gain)
		}
	}
	return out
}
