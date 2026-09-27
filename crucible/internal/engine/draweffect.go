// Draw: M6's own first script-driven effect, CR 120.3.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/DrawEffect.java's
// resolve, trimmed to the two params this port can resolve today.

package engine

//enginelint:allow id card game player ability turn defined amount control

import "fmt"

// drawEffect resolves Mode$/DB$ Draw. Two of Java's params are handled:
// NumCards$ (default 1, Java's own `sa.hasParam("NumCards") ? ... : 1`),
// through resolveNamedAmount (amount.go) -- a plain integer or a named SVar
// this face defines, the identical literal-or-reference resolution a
// continuous effect's own numeric params already get (ptParam,
// continuous.go); a "*"-shaped amount or a head resolveAmount does not evaluate still
// fails, resolveAmount's own contract -- and Defined$ (definedPlayers,
// defined.go).
//
// Not ported: Upto (a player chooses how many, 0 to NumCards$), the optional
// draw's own confirmation prompt (OptionalDecider$), Reveal, and
// RememberDrawn -- none of PlayerController's methods this port has yet
// covers a numeric or reveal choice (control.go's own "90 of 110 methods"
// gap), so a script needing one of these fails loudly (below) rather than
// drawing the wrong number silently.
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
	n := 1
	if v, ok := a.Params.Param("NumCards"); ok {
		parsed, ok := resolveNamedAmount(g, a.Amounts, g.Card(a.Source), v)
		if !ok {
			return fmt.Errorf("engine: Draw: NumCards$ %q is not resolvable", v)
		}
		n = parsed
	}
	for _, key := range []string{"Upto", "OptionalDecider", "Reveal", "RememberDrawn"} {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Draw: %s$ not resolvable yet", key)
		}
	}
	defined, _ := a.Params.Param("Defined")
	players, err := definedPlayers(g, a.Controller, a.Source, defined, a.refs())
	if err != nil {
		return err
	}
	for _, pid := range players {
		g.DrawCards(pid, n, controller)
	}
	return nil
}
