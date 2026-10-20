package engine

//enginelint:allow ability animate card condition control defined effecthelpers game

import (
	"fmt"
	"strings"
)

// debuffEffect is DebuffEffect.java: each targeted or Defined$ card on the
// battlefield loses Keywords$ until end of turn (or for good with Duration$
// Permanent). The "Protection from <color>" special cases -- splitting
// "Protection from each color" and the Ward Auras' Protection: form -- and
// AllSuffixKeywords$ walk removal are not built: a line whose card could hit
// them fails closed rather than removing the wrong thing.
type debuffEffect struct{}

func (debuffEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "Debuff", "AllSuffixKeywords"); err != nil {
		return err
	}
	permanent, until, err := animateDuration(a, "Debuff")
	if err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	raw, _ := a.Params.Param("Keywords")
	var kws []string
	if raw != "" {
		kws = strings.Split(raw, " & ")
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Debuff: %w", err)
	}
	for _, kw := range kws {
		if !strings.HasPrefix(kw, "Protection from ") {
			continue
		}
		for _, id := range cards {
			for _, line := range g.Card(id).KeywordLines() {
				if strings.HasPrefix(line, "Protection:") || line == "Protection from each color" {
					return fmt.Errorf("engine: Debuff: removing %q from a card with %q not resolvable yet", kw, line)
				}
			}
		}
	}
	return g.animateCards(a, animateRecord{Permanent: permanent, until: until, RemoveKeywords: kws}, cards)
}
