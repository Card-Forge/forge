// Poison: CR 121.13 -- 35 real (AB|DB)$ Poison lines, all naming Num$; a
// positive value adds poison counters, a negative one removes them (CR
// 704.5g's own 10-or-more loss already checked by CheckStateBasedActions,
// action.go, unconditionally for whatever put the counters there). Defined$
// (29) and ValidTgts$ (5) both resolve through targetedOrDefinedPlayers
// (defined.go), default "You" -- PoisonEffect.java's own getTargetPlayers(sa)
// with no definedParam override.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/PoisonEffect.java's
// resolve. Counters.Add's own clamp-at-zero contract (counters.go) means a
// removal past the current count is a no-op rather than a negative pile,
// the identical reasoning removeCounterEffect's own doc comment gives.

package engine

//enginelint:allow id card game player ability defined condition control amount parts event

import "fmt"

// poisonUnresolvedParams names PoisonEffect's own params this port does not
// evaluate. Every one fails the whole line loudly (PORT-8/GO-7): Ultimate$/
// Planeswalker$ (1/1) -- a planeswalker-ultimate-specific flag and CR
// 606.3's own loyalty-ability marker, neither read by this file;
// Condition$/ConditionDefined$ (0/0, defensive) -- condition.go's own
// subAbilityConditionMet would otherwise silently no-op a card naming
// either without ConditionPresent$ alongside it, dealDamageEffect's own
// identical reasoning.
var poisonUnresolvedParams = [...]string{
	"Ultimate", "Planeswalker"}

// poisonEffect resolves Mode$/DB$/AB$ Poison. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type poisonEffect struct{}

func (poisonEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range poisonUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Poison: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	numParam, ok := a.Params.Param("Num")
	if !ok {
		return fmt.Errorf("engine: Poison: Num$ missing")
	}
	amount, ok := resolveNamedAmount(g, a.Amounts, source, numParam)
	if !ok {
		return fmt.Errorf("engine: Poison: Num$ %q is not resolvable", numParam)
	}
	if amount == 0 {
		return nil
	}

	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Poison: %w", err)
	}

	for _, pid := range players {
		p := g.Player(pid)
		// Player.addCounter: GameEntity.addCounter refuses a placement a
		// Mode$ CantPutCounter static forbids (Player.canReceiveCounters).
		if amount > 0 && g.cantPutCounter(PlayerEntity(pid), Poison) {
			continue
		}
		p.Counters.Add(Poison, amount)
		emitCounterChanged(g, a.Source, PlayerEntity(pid), Poison, amount)
	}
	return nil
}
