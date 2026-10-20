package engine

//enginelint:allow id card game player ability condition control amount parts event zone effecthelpers

import "fmt"

// proliferateEffect is CountersProliferateEffect.java: Amount$ times
// (default 1), the activator chooses any number of players and battlefield
// permanents that have a counter (ChooseEntitiesForEffect), and each chosen
// one gets one more of every kind of counter it already has (CR 701.34a).
// Players come first, then permanents, as Java's own list.addAll order.
//
// Not ported: the Proliferate replacement family and the Proliferate
// trigger mode (neither exists in replacement.go/trigger.go yet), and the
// per-counter replacement pass GameEntityCounterTable.replaceCounterEffect
// runs -- putCounterEffect adds counters the same direct way.
type proliferateEffect struct{}

func (proliferateEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	num := 1
	if raw, ok := a.Params.Param("Amount"); ok {
		n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
		if !ok {
			return fmt.Errorf("engine: Proliferate: Amount$ %q not resolvable yet", raw)
		}
		num = n
	}
	for i := 0; i < num; i++ {
		var options []EntityID
		for _, pid := range g.Players() {
			if len(g.Player(pid).Counters.Kinds()) > 0 {
				options = append(options, PlayerEntity(pid))
			}
		}
		for _, pid := range g.Players() {
			for _, cid := range g.Zone(Battlefield, pid).Cards() {
				if len(g.Card(cid).Counters.Kinds()) > 0 {
					options = append(options, CardEntity(cid))
				}
			}
		}
		chosen := controller.ChooseEntitiesForEffect(g, a.Controller, a.Source, options, 0, len(options))
		if err := checkChoice(chosen, options, 0, len(options)); err != nil {
			return fmt.Errorf("engine: Proliferate: %w", err)
		}
		for _, e := range chosen {
			var counters *Counters
			if pid, ok := e.AsPlayer(); ok {
				counters = &g.Player(pid).Counters
			} else {
				cid, _ := e.AsCard()
				counters = &g.Card(cid).Counters
			}
			for _, kind := range counters.Kinds() {
				counters.Add(kind, 1)
				emitCounterChanged(g, a.Source, e, kind, 1)
			}
		}
	}
	return nil
}
