// Radiation: CR 121.13 -- 22 real (AB|DB)$ Radiation lines, all naming
// Num$; poisonEffect's own mirror shape, a positive value adds radiation
// counters and a negative one removes them. Defined$ (18) and ValidTgts$
// (4) both resolve through targetedOrDefinedPlayers (defined.go), default
// "You".
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/RadiationEffect.java's
// resolve. CR 704.5u's own upkeep dice-roll-or-lose-life consequence of
// carrying radiation counters is not ported -- PhaseHandler's own Upkeep
// step body needs building first (game-state.md's "Not ported yet") -- but
// that is a separate CR paragraph from this one: CR 121.13 itself is just
// "a player who is given radiation counters gets them," the whole of what
// this file resolves, the identical scope a PutCounter line gets independent
// of whatever static ability a keyword counter's own kind might imply
// elsewhere.

package engine

//enginelint:allow id card game player ability defined condition control amount parts event

import "fmt"

// radiationUnresolvedParams names RadiationEffect's own params this port
// does not evaluate. Every one fails the whole line loudly (PORT-8/GO-7):
// TriggeredCard$ (1) -- a trigger-context Defined$ value definedPlayers has
// no case for (defined.go's own doc comment); Condition$/ConditionDefined$
// (0/0, defensive) -- condition.go's own subAbilityConditionMet would
// otherwise silently no-op a card naming either without ConditionPresent$
// alongside it, dealDamageEffect's own identical reasoning.
var radiationUnresolvedParams = [...]string{
	"TriggeredCard"}

// radiationEffect resolves Mode$/DB$/AB$ Radiation. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type radiationEffect struct{}

func (radiationEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range radiationUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Radiation: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	numParam, ok := a.Params.Param("Num")
	if !ok {
		numParam = "0"
	}
	amount, ok := resolveNamedAmount(g, a.Amounts, source, numParam)
	if !ok {
		return fmt.Errorf("engine: Radiation: Num$ %q is not resolvable", numParam)
	}
	if amount == 0 {
		return nil
	}

	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Radiation: %w", err)
	}

	for _, pid := range players {
		p := g.Player(pid)
		// Player.addCounter: GameEntity.addCounter refuses a placement a
		// Mode$ CantPutCounter static forbids (Player.canReceiveCounters).
		if amount > 0 && g.cantPutCounter(PlayerEntity(pid), Radiation) {
			continue
		}
		p.Counters.Add(Radiation, amount)
		emitCounterChanged(g, a.Source, PlayerEntity(pid), Radiation, amount)
	}
	return nil
}
