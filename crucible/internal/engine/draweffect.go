// Draw: M6's own first script-driven effect, CR 120.3.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/DrawEffect.java's
// resolve, trimmed to the two params this port can resolve today.

package engine

//enginelint:allow id card game player ability turn defined amount control condition

import "fmt"

// drawEffect resolves Mode$/DB$ Draw. Two of Java's params are handled:
// NumCards$ (default 1, Java's own `sa.hasParam("NumCards") ? ... : 1`),
// through resolveNamedAmount (amount.go) -- a plain integer or a named SVar
// this face defines, the identical literal-or-reference resolution a
// continuous effect's own numeric params already get (ptParam,
// continuous.go); a "*"-shaped amount or a head resolveAmount does not evaluate still
// fails, resolveAmount's own contract -- and Defined$ (definedPlayers,
// defined.go). Condition$/ConditionPresent$/ConditionCompare$/
// ConditionCheckSVar$/ConditionSVarCompare$/ConditionDefined$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does -- this file had no call to it at all until
// found missing (rules review on the LosePerpetual/ADR-0023 landing), so
// every one of the corpus's own 747 real SVar-defined Draw lines carrying a
// Condition*$ param silently drew anyway.
//
// Upto$ (the player chooses 0 to NumCards$), OptionalDecider$ (the drawing
// player confirms; the value names no one, DrawEffect.java:67) and
// RememberDrawn$ are resolved; Reveal$ is informational and ignored.
//
// SubAbility$ was never in this list -- Draw never blocked it -- but had
// nowhere to go until resolveSubAbility (subability.go, effect.go's own
// Registry.Resolve) landed: Rousing Read's own "draw two cards, then
// discard a card" (DB$ Draw | ... | SubAbility$ DBDiscard, chaining into
// DB$ Discard) is the shape that made this real rather than theoretical.
// 170 of the corpus's own 747 real SVar-defined Draw lines naming
// SubAbility$ chain to an already-built leaf ability (no further
// SubAbility$ of its own) and resolve end to end.
type drawEffect struct{}

func (drawEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	source := g.Card(a.Source)
	n := 1
	if v, ok := a.Params.Param("NumCards"); ok {
		parsed, ok := resolveNamedAmount(g, a.Amounts, source, v)
		if !ok {
			return fmt.Errorf("engine: Draw: NumCards$ %q is not resolvable", v)
		}
		n = parsed
	}
	// Reveal$ (DrawEffect.java:91-93) only shows the drawn cards: the engine is
	// omniscient and no trigger reads a reveal, so it changes nothing here.
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	// DrawEffect.resolve's getTargetPlayers(sa): ValidTgts$ targets, else
	// Defined$, which defaults to You (the Cycling keyword's own Draw has none).
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return err
	}
	_, upto := a.Params.Param("Upto")
	_, decider := a.Params.Param("OptionalDecider")
	optional := decider || upto
	_, remember := a.Params.Param("RememberDrawn")
	// A player named twice draws once, for the sum (DrawEffect.java:75-79).
	var order []PlayerID
	times := map[PlayerID]int{}
	for _, pid := range players {
		if times[pid] == 0 {
			order = append(order, pid)
		}
		times[pid]++
	}
	for _, pid := range order {
		if g.Player(pid).Lost {
			continue // DrawEffect.java:70-72: !p.isInGame()
		}
		num := n * times[pid]
		// Optional, not up to: a draw the player cannot make in full is skipped.
		if optional && !upto && g.cantDrawAmount(pid, num) {
			continue
		}
		if optional && !controller.ConfirmEffect(g, pid, a.Source) {
			continue
		}
		if upto {
			for num > 0 && g.cantDrawAmount(pid, num) {
				num--
			}
			if num <= 0 {
				continue
			}
			num = controller.ChooseNumber(g, pid, a.Source, 0, num)
		}
		if remember {
			var drawn []CardID
			outer := g.drawRecord
			g.drawRecord = &drawn
			g.DrawCards(pid, num, controller)
			g.drawRecord = outer
			for _, id := range drawn {
				source.Memory.Remember(CardEntity(id))
			}
			continue
		}
		g.DrawCards(pid, num, controller)
	}
	return nil
}
