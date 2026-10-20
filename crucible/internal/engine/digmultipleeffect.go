package engine

//enginelint:allow control game ability effecthelpers card condition amount zone defined id player valid parts

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// digMultipleEffect is DigMultipleEffect.java: each Defined$ or targeted
// player looks at the top DigNum$ cards of their SourceZone$ (default
// Library) and, for each comma-separated ChangeValid$ category with a
// match, may pick one of those cards (chooseCardsForEffectMultiple asks
// once per category; this port walks the categories in written order,
// where Java's HashMap order is arbitrary). Picks go to DestinationZone$
// (default Hand; LibraryPosition$ for a library), the rest to
// DestinationZone2$ (default the library bottom), shuffled first with
// RestRandomOrder$. Without Optional$ the player must pick something.
type digMultipleEffect struct{}

func (digMultipleEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "DigMultiple", "ChooseAmount", "ChosenZone", "ChangeLater",
		"ExileFaceDown", "Imprint", "ForgetOtherRemembered", "Tapped"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	raw, ok := a.Params.Param("DigNum")
	if !ok {
		return fmt.Errorf("engine: DigMultiple: DigNum$ missing")
	}
	digNum, ok := resolveNamedAmount(g, a.Amounts, source, raw)
	if !ok {
		return fmt.Errorf("engine: DigMultiple: DigNum$ %q not resolvable", raw)
	}
	zoneParam := func(key string, def ZoneType) (ZoneType, error) {
		raw, ok := a.Params.Param(key)
		if !ok {
			return def, nil
		}
		z, ok := ZoneByName(raw)
		if !ok {
			return 0, fmt.Errorf("engine: DigMultiple: %s$ %q not resolvable", key, raw)
		}
		return z, nil
	}
	srcZone, err := zoneParam("SourceZone", Library)
	if err != nil {
		return err
	}
	dest1, err := zoneParam("DestinationZone", Hand)
	if err != nil {
		return err
	}
	dest2, err := zoneParam("DestinationZone2", Library)
	if err != nil {
		return err
	}
	libPos := func(key string) (int, error) {
		raw, ok := a.Params.Param(key)
		if !ok {
			return -1, nil
		}
		n, err := strconv.Atoi(raw)
		if err != nil || (n != 0 && n != -1) {
			return 0, fmt.Errorf("engine: DigMultiple: %s$ %q not resolvable", key, raw)
		}
		return n, nil
	}
	pos1, err := libPos("LibraryPosition")
	if err != nil {
		return err
	}
	pos2, err := libPos("LibraryPosition2")
	if err != nil {
		return err
	}
	changeValid, _ := a.Params.Param("ChangeValid")
	_, optional := a.Params.Param("Optional")
	_, remember := a.Params.Param("RememberChanged")
	_, random := a.Params.Param("RestRandomOrder")
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: DigMultiple: %w", err)
	}
	var moved, movedRest []CardID
	for _, p := range players {
		if g.Player(p).Lost {
			continue
		}
		zone := g.Zone(srcZone, p).Cards()
		n := digNum
		if n > len(zone) {
			n = len(zone)
		}
		top := append([]CardID(nil), zone[:n]...)
		if len(top) == 0 {
			continue
		}
		var chosen []CardID
		for _, spec := range strings.Split(changeValid, ",") {
			parsed := valid.Parse(spec)
			var list []CardID
			for _, id := range top {
				if !containsCard(chosen, id) && Matches(g, g.Card(id), parsed, source.Controller(), a.Source) {
					list = append(list, id)
				}
			}
			if len(list) == 0 {
				continue
			}
			pick := controller.ChooseCardsForEffect(g, p, a.Source, list, 0, 1)
			if err := checkChoice(pick, list, 0, 1); err != nil {
				return fmt.Errorf("engine: DigMultiple: %w", err)
			}
			chosen = append(chosen, pick...)
		}
		anyValid := len(chosen) > 0
		if !anyValid {
			for _, spec := range strings.Split(changeValid, ",") {
				if len(filterValid(g, top, spec, source.Controller(), a.Source)) > 0 {
					anyValid = true
				}
			}
			if anyValid && !optional {
				return fmt.Errorf("engine: DigMultiple: a card must be chosen")
			}
		}
		for _, id := range chosen {
			g.moveByEffect(controller, id, dest1, pos1, NoPlayer, false)
			moved = append(moved, id)
			if remember {
				source.Memory.Remember(CardEntity(id))
			}
		}
		rest := withoutCards(top, chosen)
		if dest2 == Library || dest2 == Graveyard {
			if random {
				g.rand.Shuffle(len(rest), func(i, j int) { rest[i], rest[j] = rest[j], rest[i] })
			}
			if pos2 != -1 {
				rest = reversedCards(rest)
			}
		}
		for _, id := range rest {
			g.moveByEffect(controller, id, dest2, pos2, NoPlayer, false)
			if dest2 != srcZone {
				movedRest = append(movedRest, id)
			}
		}
	}
	g.checkChangesZoneAllTriggers(controller, moved, srcZone, dest1)
	g.checkChangesZoneAllTriggers(controller, movedRest, srcZone, dest2)
	return nil
}
