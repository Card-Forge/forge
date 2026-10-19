package engine

//enginelint:allow id card game ability defined condition control parts zone effecthelpers valid zonemove

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// repeatEachUnresolvedParams are RepeatEachEffect.java's params this port
// cannot honour yet: the stack-object and card-type loops
// (RepeatSpellAbilities$, RepeatTypesFrom$), a delayed per-player loop
// (NextTurnForEachPlayer$), vote amounts (AmountFromVotes$), the batched
// damage/life tables (DamageMap$, LoseLifeMap$), a
// chooser-ordered loop (ChooseOrder$), and a non-APNAP start (StartingWith$).
var repeatEachUnresolvedParams = [...]string{
	"RepeatSpellAbilities", "RepeatTypesFrom", "NextTurnForEachPlayer", "AmountFromVotes",
	"DamageMap", "LoseLifeMap", "ChooseOrder", "StartingWith",
	"Condition", "Ultimate",
}

// repeatEachEffect is RepeatEachEffect.java: RepeatSubAbility$ resolves
// once per item, the item swapped into the host's Memory for the duration
// so the sub-ability reads it as Remembered (or Imprinted, UseImprinted$).
// Items are RepeatCards$ (cards in Zone$, default Battlefield, matching it,
// relative to the host's controller), DefinedCards$, RepeatTargeted$ (the
// ability's own targets) and RepeatPlayers$ (a Defined$ player list; the
// host's other remembered players are set aside while each resolves, and
// RepeatOptionalForEachPlayer$ lets each player decline). 288 real lines.
type repeatEachEffect struct{}

func (repeatEachEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range repeatEachUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: RepeatEach: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	if _, ok := a.Params.Param("Optional"); ok {
		if _, prompt := a.Params.Param("OptionPrompt"); prompt && !controller.ConfirmEffect(g, a.Controller, a.Source) {
			return nil
		}
	}
	if _, ok := a.Params.Param("ChangeZoneTable"); ok {
		// RepeatEachEffect.java:84-86, :202-205: one CardZoneTable for the whole
		// loop; the batched ChangesZoneAll fires when it ends.
		defer g.openZoneBatch(controller)()
	}
	_, useImprinted := a.Params.Param("UseImprinted")
	m := &source.Memory

	var cards []CardID
	hasCards := false
	if spec, ok := a.Params.Param("RepeatCards"); ok {
		hasCards = true
		zones := []ZoneType{Battlefield}
		if raw, ok := a.Params.Param("Zone"); ok {
			var err error
			zones, err = parseZoneList(raw)
			if err != nil {
				return fmt.Errorf("engine: RepeatEach: Zone$: %w", err)
			}
		}
		parsed := valid.Parse(spec)
		for _, z := range zones {
			for _, pid := range g.Players() {
				for _, id := range g.Zone(z, pid).Cards() {
					if Matches(g, g.Card(id), parsed, source.Controller(), a.Source) {
						cards = append(cards, id)
					}
				}
			}
		}
	} else if spec, ok := a.Params.Param("DefinedCards"); ok {
		hasCards = true
		var err error
		cards, err = definedCards(source, spec, a.refs())
		if err != nil {
			return fmt.Errorf("engine: RepeatEach: DefinedCards$: %w", err)
		}
	}
	if _, ok := a.Params.Param("ClearRemembered"); ok {
		m.ClearRemembered()
	}
	if hasCards {
		for _, id := range cards {
			if useImprinted {
				m.Imprint(id)
			} else {
				m.Remember(CardEntity(id))
			}
			if err := g.resolveAdditionalKey(a, controller, "RepeatSubAbility"); err != nil {
				return err
			}
			if useImprinted {
				m.forgetImprinted(id)
			} else {
				m.Forget(CardEntity(id))
			}
		}
	}
	if _, ok := a.Params.Param("RepeatTargeted"); ok {
		for _, e := range a.Targets {
			m.Remember(e)
			if err := g.resolveAdditionalKey(a, controller, "RepeatSubAbility"); err != nil {
				return err
			}
			m.Forget(e)
		}
	}
	if spec, ok := a.Params.Param("RepeatPlayers"); ok {
		players, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs())
		if err != nil {
			return fmt.Errorf("engine: RepeatEach: RepeatPlayers$: %w", err)
		}
		if _, ok := a.Params.Param("ClearRememberedBeforeLoop"); ok {
			m.ClearRemembered()
		}
		_, optional := a.Params.Param("RepeatOptionalForEachPlayer")
		for _, p := range players {
			if optional && !controller.ConfirmEffect(g, p, a.Source) {
				continue
			}
			old := swapRememberedPlayer(m, p)
			if err := g.resolveAdditionalKey(a, controller, "RepeatSubAbility"); err != nil {
				return err
			}
			restoreRememberedPlayers(m, p, old)
		}
	}
	return nil
}
