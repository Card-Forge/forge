package engine

//enginelint:allow card game ability defined condition control effecthelpers room valid zone id

import (
	"fmt"
	"slices"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// unlockDoorEffect is UnlockDoorEffect.java for its two script-written
// modes (CR 709.5): Mode$ Unlock unlocks a locked door of each Room -- the
// activator picks which when both are locked -- and Mode$ LockOrUnlock
// locks or unlocks one of its doors: with none locked it locks one, with
// both locked it unlocks one, and with one locked the activator picks
// either door and that door changes. The Rooms are the targets (or
// Defined$ cards), or with Choices$ one Room the activator picks among the
// battlefield's permanents matching it.
//
// The default Mode$ ThisDoor is rejected: no script line uses it. It is
// Java's synthesized unlock special action (CardFactoryUtil.java:124),
// which this port runs as Game.UnlockDoor/ActionUnlockDoor (room.go)
// without an ability, and the cast-half unlock (GameAction.java:571), run
// by permanentEffect.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/UnlockDoorEffect.java's resolve.
type unlockDoorEffect struct{}

func (unlockDoorEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	mode, ok := a.Params.Param("Mode")
	if !ok {
		mode = "ThisDoor"
	}
	if mode != "Unlock" && mode != "LockOrUnlock" {
		return fmt.Errorf("engine: UnlockDoor: Mode$ %s not resolvable yet", mode)
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	rooms, err := unlockDoorRooms(g, a, controller)
	if err != nil {
		return err
	}
	for _, id := range rooms {
		room := g.Card(id)
		if !room.IsRoomPermanent() {
			continue
		}
		locked := room.LockedDoors()
		if mode == "Unlock" {
			d, ok, err := chooseRoomDoor(g, a, controller, id, locked)
			if err != nil {
				return err
			}
			if ok {
				g.unlockDoor(controller, id, a.Controller, d)
			}
			continue
		}
		options := []Door{DoorLeft, DoorRight}
		switch len(locked) {
		case 0:
			options = room.UnlockedDoors()
		case 2:
			options = locked
		}
		d, ok, err := chooseRoomDoor(g, a, controller, id, options)
		if err != nil {
			return err
		}
		if !ok {
			continue
		}
		if slices.Contains(locked, d) {
			g.unlockDoor(controller, id, a.Controller, d)
		} else {
			g.lockDoor(id, d)
		}
	}
	return nil
}

// unlockDoorRooms is the Rooms the effect acts on: with Choices$, the one
// the activator picks among every battlefield permanent matching it
// (chooseSingleEntityForEffect, mandatory; none when nothing matches),
// else the targets or Defined$ cards (getTargetCards).
func unlockDoorRooms(g *Game, a *Ability, controller PlayerController) ([]CardID, error) {
	spec, ok := a.Params.Param("Choices")
	if !ok {
		rooms, err := targetedOrDefinedCards(g.Card(a.Source), a.Params, a.refs())
		if err != nil {
			return nil, fmt.Errorf("engine: UnlockDoor: %w", err)
		}
		return rooms, nil
	}
	parsed := valid.Parse(spec)
	var choices []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if Matches(g, g.Card(id), parsed, a.Controller, a.Source) {
				choices = append(choices, id)
			}
		}
	}
	if len(choices) == 0 {
		return nil, nil
	}
	picked := controller.ChooseCardsForEffect(g, a.Controller, a.Source, choices, 1, 1)
	if err := checkChoice(picked, choices, 1, 1); err != nil {
		return nil, fmt.Errorf("engine: UnlockDoor: %w", err)
	}
	return picked, nil
}

// chooseRoomDoor is chooseSingleCardState over options: nothing to pick
// from is no door (Java's null, the Room skipped), one is taken without
// asking (PlayerControllerHuman.chooseSingleCardState), two ask the
// activator.
func chooseRoomDoor(g *Game, a *Ability, controller PlayerController, room CardID, options []Door) (Door, bool, error) {
	switch len(options) {
	case 0:
		return 0, false, nil
	case 1:
		return options[0], true, nil
	}
	d := controller.ChooseRoomDoor(g, a.Controller, room, options)
	if !slices.Contains(options, d) {
		return 0, false, fmt.Errorf("engine: UnlockDoor: controller chose door %s, not one of %v", d, options)
	}
	return d, true, nil
}
