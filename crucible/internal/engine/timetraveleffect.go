package engine

//enginelint:allow game control ability effecthelpers card condition id zone parts event

import "fmt"

// timeTravelEffect is TimeTravelEffect.java: Amount$ (default 1) times, the
// activator picks any of the suspended cards they have in exile and the
// permanents they control with a time counter, and for each picked card
// adds or removes one time counter (chooseBinary AddOrRemove).
type timeTravelEffect struct{}

func (timeTravelEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "TimeTravel"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	times, err := optionalAmount(g, a, "TimeTravel", "Amount", 1)
	if err != nil {
		return err
	}
	for i := 0; i < times; i++ {
		var list []CardID
		for _, id := range g.Zone(Exile, a.Controller).Cards() {
			if g.Card(id).HasKeyword("Suspend") {
				list = append(list, id)
			}
		}
		for _, id := range g.Zone(Battlefield, a.Controller).Cards() {
			if g.Card(id).Counters.Count(Time) > 0 {
				list = append(list, id)
			}
		}
		if len(list) == 0 {
			continue
		}
		chosen := controller.ChooseCardsForEffect(g, a.Controller, a.Source, list, 0, len(list))
		if err := checkChoice(chosen, list, 0, len(list)); err != nil {
			return fmt.Errorf("engine: TimeTravel: %w", err)
		}
		for _, id := range chosen {
			c := g.Card(id)
			if controller.ChooseBinary(g, a.Controller, a.Source, AddOrRemove) {
				// Card.addCounter: no placement a CantPutCounter static forbids
				// (Card.canReceiveCounters).
				if g.cantPutCounter(CardEntity(id), Time) {
					continue
				}
				c.Counters.Add(Time, 1)
				emitCounterChanged(g, a.Source, CardEntity(id), Time, 1)
			} else if c.Counters.Count(Time) > 0 {
				c.Counters.Add(Time, -1)
				emitCounterChanged(g, a.Source, CardEntity(id), Time, -1)
			}
		}
	}
	return nil
}
