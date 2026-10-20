package engine

//enginelint:allow id ability game control effecthelpers card condition parts subability

import (
	"fmt"
)

// chooseSectorEffect is ChooseSectorEffect.java: the host's controller -- not
// the activator, card.getController() at ChooseSectorEffect.java:12 -- picks
// one of Space Beleren's three sectors, and the host records it
// (Card.setChosenSector). AILogic$ is an AI hint and Ultimate$ feeds only
// AchievementTracker.java:23, so neither changes how this resolves.
//
// The read side is ported: Creature.ChosenSector and DifferentSector are
// valid.go properties, and CR 704.5u's per-creature sector is assigned by
// assignSectors (action.go), so the chained PutCounterAll/DestroyAll resolve.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/ChooseSectorEffect.java's resolve.
type chooseSectorEffect struct{}

func (chooseSectorEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "ChooseSector"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	// PlayerController.chooseSector's own fixed list and order
	// (PlayerController.java:255), built per call: a package-level slice
	// would be shared mutable state (GO-2).
	options := []string{"Alpha", "Beta", "Gamma"}
	i := controller.ChooseSector(g, source.Controller(), NoCard, options)
	if i < 0 || i >= len(options) {
		return fmt.Errorf("engine: ChooseSector: choice %d out of range", i)
	}
	source.Memory.SetChosenSector(options[i])
	return nil
}
