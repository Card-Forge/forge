// Scry: CR 701.19, ScryEffect.java + GameAction.scry -- the first
// script-driven effect that asks the resolving player to reorder cards
// rather than a plain choose-N-of-a-set decision (ChooseCardsToDiscard's
// own shape, discardeffect.go). 340 of the corpus's 415 real
// (AB|DB)$ Scry lines that also name Defined$ You/Opponent/Player/
// Player.Opponent or no Defined$ at all -- AbilityUtils.getDefinedPlayers's
// own `changedDef = (def == null) ? "You" : ...` default, unlike every
// other M6 effect so far, where an absent Defined$ is a real error -- and
// carry no other unresolved param, resolve; 8 of them name Planeswalker$
// too, no longer blocked (CR 606.3's own loyalty-ability restriction is a
// cost-side gate, activateability.go, never a restriction on how the
// effect it pays for resolves).
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/ScryEffect.java's
// resolve and forge-game/src/main/java/forge/game/GameAction.java's own
// scry; Mode$ Scry fires per scrying player (checkScryTriggers,
// triggerpack.go). The CR 614 Scry replacement type is skipped: 0 real corpus
// lines name it.

package engine

//enginelint:allow id card game player ability defined amount condition control zone

import (
	"fmt"
)

// SubAbility$ no longer blocks: resolveSubAbility (subability.go) chains it
// through Registry.Resolve (effect.go) once this effect's own body
// finishes, whether or not subAbilityConditionMet let it run at all. 31 of
// the corpus's own 57 real SVar-defined Scry lines naming SubAbility$
// chain to an already-built leaf ability and resolve end to end.
var scryUnresolvedParams = [...]string{
	"ValidTgts", "TargetMin", "TargetMax", "Optional",
}

type scryEffect struct{}

func (scryEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range scryUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Scry: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	scryNum, ok := a.Params.Param("ScryNum")
	if !ok {
		scryNum = "1"
	}
	num, ok := resolveNamedAmount(g, a.Amounts, source, scryNum)
	if !ok {
		return fmt.Errorf("engine: Scry: ScryNum$ %q is not resolvable", scryNum)
	}
	// CR 701.22b: a player instructed to scry 0 does not scry at all.
	if num <= 0 {
		return nil
	}

	defined, ok := a.Params.Param("Defined")
	if !ok {
		defined = "You"
	}
	players, err := definedPlayers(g, a.Controller, a.Source, defined, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Scry: %w", err)
	}

	for _, pid := range players {
		// Event$ Scry: Kenessos' "scry that many plus one", Eligeth's "draw
		// that many cards instead".
		pidNum, replaced := g.playerAmountReplaced(controller, "Scry", "Num", pid, num)
		if replaced || pidNum <= 0 {
			continue
		}
		lib := g.Zone(Library, pid).Cards()
		n := pidNum
		if n > len(lib) {
			n = len(lib)
		}
		// An empty library still scries (GameAction.java:2605-2615 keeps the
		// player, arrangeForScry gets an empty list) and the Scry trigger
		// fires with ScryNum 0 (:2652-2655). Nothing is asked of the
		// controller: there is nothing to arrange.
		var toTop, toBottom []CardID
		if n > 0 {
			topN := append([]CardID(nil), lib[:n]...)
			toTop, toBottom = controller.ArrangeForScry(g, pid, topN)
		}
		for i := len(toTop) - 1; i >= 0; i-- {
			g.MoveToLibraryTop(toTop[i], pid)
		}
		for _, id := range toBottom {
			g.Move(id, Library, pid)
		}
		g.checkScryTriggers(controller, pid, len(toTop)+len(toBottom), len(toBottom))
		// GameAction.java:2684: p.incScryThisTurn(), after the trigger.
		g.Player(pid).scryThisTurn++
	}
	return nil
}
