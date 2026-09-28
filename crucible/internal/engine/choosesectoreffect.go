package engine

//enginelint:allow id ability game control effecthelpers card condition parts subability

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// chooseSectorEffect is ChooseSectorEffect.java: the host's controller -- not
// the activator, card.getController() at ChooseSectorEffect.java:12 -- picks
// one of Space Beleren's three sectors, and the host records it
// (Card.setChosenSector). AILogic$ is an AI hint and Ultimate$ feeds only
// AchievementTracker.java:23, so neither changes how this resolves.
//
// Only the write is ported: the Creature.ChosenSector/DifferentSector reads
// (CardProperty.java:119-126) and the per-creature sector that
// CR 704.5u's state-based action assigns (GameAction.java:1801-1828) do not
// exist yet (port-log effects-choosesector.md). Recording a sector nothing
// reads back would silently turn a resolved ability into a no-op -- correct
// per this port's own valid-string fallthrough, but indistinguishable from a
// real effect in telemetry and in a game log, the exact harm GO-7 exists to
// prevent (rules-review finding on the merged commit; effects-batch-c.md's
// own prior deferral reasoning already said so). So the chain is rejected
// loudly instead, the same as any other unresolved shape, whenever the
// SubAbility$ this API is chained from would read the sector it just wrote.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/ChooseSectorEffect.java's resolve.
type chooseSectorEffect struct{}

// chooseSectorReadUnbuilt reports whether sub's own ValidCards$/ValidTgts$
// names the Creature.ChosenSector/DifferentSector property this port's
// valid.go cannot match yet (falls through to false for every card, silently
// -- see the doc comment above). Checked before choosing so this API's own
// answer is never asked, let alone recorded, for a chain that cannot use it.
func chooseSectorReadUnbuilt(sub *compile.Ability) bool {
	for _, key := range [...]string{"ValidCards", "ValidTgts"} {
		if v, ok := sub.Param(key); ok &&
			(strings.Contains(v, "ChosenSector") || strings.Contains(v, "DifferentSector")) {
			return true
		}
	}
	return false
}

func (chooseSectorEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "ChooseSector", "Condition"); err != nil {
		return err
	}
	if sub, ok := findSubAbility(a.Params); ok && chooseSectorReadUnbuilt(sub.Ability) {
		return fmt.Errorf("engine: ChooseSector: SubAbility$ %s reads Creature.ChosenSector/DifferentSector, not resolvable yet (read side not ported)", sub.SVar)
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
