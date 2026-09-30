package engine

//enginelint:allow id card game player ability defined condition control amount parts zone effecthelpers zonemove

import (
	"fmt"
	"strings"
)

// digUnresolvedParams are DigEffect.java's params this port cannot honour
// yet: a non-library or bottom-up dig (SourceZone$, FromBottom$), a
// different chooser (Choser$), the alternative selection loops
// (WithTotalCMC$, ForEachColorPair$, WithDifferentPowers$, RandomChange$),
// entering-state modifiers (FaceDown*$, WithCounters$, Attacking$), exile
// bookkeeping this port has no concept of (ExileFaceDown$,
// ExileWithCounters$, DefinedExiler$), and GainControl$'s Cybership reorder.
var digUnresolvedParams = [...]string{
	"SourceZone", "FromBottom", "Choser", "SetChosenPlayer", "WithTotalCMC",
	"ForEachColorPair", "WithDifferentPowers", "RandomChange", "GainControl",
	"FaceDown", "FaceDownPower", "FaceDownToughness", "FaceDownSetType",
	"WithCounters", "WithCountersAmount", "Attacking", "Blocking",
	"ExileFaceDown", "ExileWithCounters", "DefinedExiler", "StaticEffect",
	"Pawprint", "Boast", "UnlessResolveSubs",
	"Condition", "ConditionDefined", "SorcerySpeed", "PlayerTurn", "Ultimate", "ModeCost", "CheckSVar",
}

// digEffect is DigEffect.java: each Defined$/ValidTgts$ player (default
// You) looks at the top DigNum$ cards of their library; the activator
// chooses ChangeNum$ (default 1, or All, or Any) of those matching
// ChangeValid$ for DestinationZone$ (default Hand), and the rest go to
// DestinationZone2$ (default the library, LibraryPosition2$ default the
// bottom). Optional$ lets the chooser take none. Chosen cards are moved in
// reverse pick order (Java's own Collections.reverse), then re-ordered by
// their owners for a battlefield or library destination; the rest are
// ordered by the chooser (or by their owners into a graveyard) unless
// SkipReorder$ or RestRandomOrder$, which shuffles them with the game's own
// RNG. 813 real lines.
type digEffect struct{}

func (digEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range digUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Dig: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	digRaw, _ := a.Params.Param("DigNum")
	digNum, ok := resolveNamedAmount(g, a.Amounts, source, digRaw)
	if !ok {
		return fmt.Errorf("engine: Dig: DigNum$ %q not resolvable yet", digRaw)
	}
	dest1, err := digZoneParam(a, "DestinationZone", Hand)
	if err != nil {
		return err
	}
	dest2, err := digZoneParam(a, "DestinationZone2", Library)
	if err != nil {
		return err
	}
	libPos1, err := digLibraryPos(a, "LibraryPosition")
	if err != nil {
		return err
	}
	libPos2, err := digLibraryPos(a, "LibraryPosition2")
	if err != nil {
		return err
	}
	changeValid, _ := a.Params.Param("ChangeValid")
	if strings.Contains(changeValid, "ChosenType") {
		return fmt.Errorf("engine: Dig: ChangeValid$ %q names ChosenType, not resolvable yet", changeValid)
	}
	changeNum, changeAll, anyNumber := 1, false, false
	if raw, ok := a.Params.Param("ChangeNum"); ok {
		switch {
		case strings.EqualFold(raw, "All"):
			changeAll = true
		case strings.EqualFold(raw, "Any"):
			anyNumber = true
		default:
			n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
			if !ok {
				return fmt.Errorf("engine: Dig: ChangeNum$ %q not resolvable yet", raw)
			}
			changeNum = n
		}
	}
	_, optional := a.Params.Param("Optional")
	_, mayBeSkipped := a.Params.Param("PromptToSkipOptionalAbility")
	_, remZone1 := a.Params.Param("RememberChanged")
	remZone2 := false
	if raw, ok := a.Params.Param("RememberMovedToZone"); ok {
		remZone1 = remZone1 || strings.Contains(raw, "1")
		remZone2 = strings.Contains(raw, "2")
	}
	_, tapped := a.Params.Param("Tapped")

	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Dig: %w", err)
	}
	var moved1, moved2 []CardID
	for _, p := range players {
		if g.Player(p).Lost {
			continue
		}
		library := g.Zone(Library, p).Cards()
		n := digNum
		if n > len(library) {
			n = len(library)
		}
		top := append([]CardID(nil), library[:n]...)
		if len(top) == 0 {
			continue
		}
		hasRevealed := true
		if _, ok := a.Params.Param("RevealOptional"); ok {
			hasRevealed = controller.ConfirmEffect(g, p, a.Source)
		}
		if _, ok := a.Params.Param("RememberRevealed"); ok && hasRevealed {
			for _, id := range top {
				source.Memory.Remember(CardEntity(id))
			}
		}
		if _, ok := a.Params.Param("ImprintRevealed"); ok && hasRevealed {
			for _, id := range top {
				source.Memory.Imprint(id)
			}
		}
		valid := top
		if changeValid != "" {
			valid = filterValid(g, top, changeValid, source.Controller(), a.Source)
		}
		if optional && mayBeSkipped && len(valid) > 0 && !controller.ConfirmEffect(g, p, a.Source) {
			return nil
		}
		var chosen []CardID
		switch {
		case changeAll:
			chosen = append(chosen, valid...)
		case len(valid) > 0:
			hi := changeNum
			if anyNumber || hi > len(valid) {
				hi = len(valid)
			}
			lo := hi
			if anyNumber || optional {
				lo = 0
			}
			if hi > 0 {
				chosen = controller.ChooseCardsForEffect(g, a.Controller, a.Source, valid, lo, hi)
				if err := checkChoice(chosen, valid, lo, hi); err != nil {
					return fmt.Errorf("engine: Dig: %w", err)
				}
			}
		}
		if _, ok := a.Params.Param("ForgetOtherRemembered"); ok {
			source.Memory.ClearRemembered()
		}
		chosen = reversedCards(chosen)
		if dest1 == Battlefield || dest1 == Library {
			chosen, err = g.orderCardsByTheirOwners(controller, chosen, dest1)
			if err != nil {
				return fmt.Errorf("engine: Dig: %w", err)
			}
		}
		for _, id := range chosen {
			melded := g.moveByEffect(controller, id, dest1, libPos1, NoPlayer, tapped)
			if dest1 == Exile {
				g.markExiledWith(id, a.Source)
				if melded != NoCard {
					g.markExiledWith(melded, a.Source)
				}
			}
			moved1 = append(moved1, id)
			if _, ok := a.Params.Param("Imprint"); ok {
				source.Memory.Imprint(id)
			}
			if remZone1 {
				source.Memory.Remember(CardEntity(id))
			}
		}
		rest := withoutCards(top, chosen)
		if len(rest) == 0 {
			continue
		}
		if _, ok := a.Params.Param("DestZone2Optional"); ok && !controller.ConfirmEffect(g, p, a.Source) {
			continue
		}
		if dest2 == Library || dest2 == Graveyard {
			_, random := a.Params.Param("RestRandomOrder")
			_, skip := a.Params.Param("SkipReorder")
			switch {
			case random:
				g.rand.Shuffle(len(rest), func(i, j int) { rest[i], rest[j] = rest[j], rest[i] })
			case !skip && len(rest) > 1 && dest2 == Graveyard:
				rest, err = g.orderCardsByTheirOwners(controller, rest, dest2)
				if err != nil {
					return fmt.Errorf("engine: Dig: %w", err)
				}
			case !skip && len(rest) > 1:
				ordered := controller.OrderCardsForZone(g, a.Controller, rest, dest2)
				if err := checkChoice(ordered, rest, len(rest), len(rest)); err != nil {
					return fmt.Errorf("engine: Dig: %w", err)
				}
				rest = ordered
			}
		}
		for _, id := range rest {
			melded := g.moveByEffect(controller, id, dest2, libPos2, NoPlayer, false)
			if dest2 == Exile {
				g.markExiledWith(id, a.Source)
				if melded != NoCard {
					g.markExiledWith(melded, a.Source)
				}
			}
			moved2 = append(moved2, id)
			if remZone2 {
				source.Memory.Remember(CardEntity(id))
			}
		}
	}
	g.checkChangesZoneAllTriggers(controller, moved1, Library, dest1)
	g.checkChangesZoneAllTriggers(controller, moved2, Library, dest2)
	return nil
}

func digZoneParam(a *Ability, key string, def ZoneType) (ZoneType, error) {
	raw, ok := a.Params.Param(key)
	if !ok {
		return def, nil
	}
	z, err := changeZoneDestination(raw)
	if err != nil {
		return 0, fmt.Errorf("engine: Dig: %s$: %w", key, err)
	}
	return z, nil
}

// digLibraryPos reads a literal LibraryPosition$/LibraryPosition2$ (Java's
// Integer.parseInt), default the bottom; only top and bottom are movable.
func digLibraryPos(a *Ability, key string) (int, error) {
	raw, ok := a.Params.Param(key)
	if !ok {
		return libraryBottom, nil
	}
	switch strings.TrimSpace(raw) {
	case "0":
		return 0, nil
	case "-1":
		return libraryBottom, nil
	}
	return 0, fmt.Errorf("engine: Dig: %s$ %q not resolvable yet", key, raw)
}
