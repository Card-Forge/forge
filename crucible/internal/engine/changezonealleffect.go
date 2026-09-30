package engine

//enginelint:allow id card game ability defined condition control parts zone effecthelpers zonemove

import "fmt"

// changeZoneAllUnresolvedParams are ChangeZoneAllEffect.java's params this
// port cannot honour yet (same mechanisms as changeZoneUnresolvedParams).
var changeZoneAllUnresolvedParams = [...]string{
	"Duration", "ExileFaceDown", "FaceDown", "FaceDownPower", "FaceDownToughness", "FaceDownSetType",
	"WithCountersType", "StaticEffect", "RememberLKI", "TypeLimit", "AtEOT", "OptionQuestion",
	"Hidden", "ChangeNum", "PowerUp", "Pawprint", "Random",
	"Condition", "ConditionDefined", "SorcerySpeed", "PlayerTurn", "Ultimate", "CheckSVar", "SVarCompare",
}

// changeZoneAllEffect is ChangeZoneAllEffect.java: every card in the
// Origin$ zones matching ChangeType$ moves to Destination$. With neither
// ValidTgts$ nor Defined$ (or with UseAllOriginZones$) the zones of every
// player are swept; otherwise only the targeted/Defined$ players' own.
// Several cards into a library are ordered by DefinedPlayer$ (default You)
// unless RandomOrder$ shuffles them (the game's own RNG, the same stream
// library shuffles draw from) or Shuffle$ will shuffle the library anyway;
// other destinations are ordered by their owners (CR 613.7m).
type changeZoneAllEffect struct{}

func (changeZoneAllEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range changeZoneAllUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: ChangeZoneAll: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	destName, _ := a.Params.Param("Destination")
	dest, err := changeZoneDestination(destName)
	if err != nil {
		return fmt.Errorf("engine: ChangeZoneAll: %w", err)
	}
	originRaw, _ := a.Params.Param("Origin")
	origin, err := parseZoneList(originRaw)
	if err != nil {
		return fmt.Errorf("engine: ChangeZoneAll: Origin$: %w", err)
	}
	_, hasTgts := a.Params.Param("ValidTgts")
	_, hasDefined := a.Params.Param("Defined")
	_, allZones := a.Params.Param("UseAllOriginZones")
	players := g.Players()
	if (hasTgts || hasDefined) && !allZones {
		players, err = targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
		if err != nil {
			return fmt.Errorf("engine: ChangeZoneAll: %w", err)
		}
	}
	var cards []CardID
	for _, z := range origin {
		for _, pid := range players {
			cards = append(cards, g.Zone(z, pid).Cards()...)
		}
	}
	if _, ok := a.Params.Param("Optional"); ok && !controller.ConfirmEffect(g, a.Controller, a.Source) {
		return nil
	}
	if changeType, ok := a.Params.Param("ChangeType"); ok {
		cards = filterValid(g, cards, changeType, a.Controller, a.Source)
	}
	if _, ok := a.Params.Param("ForgetOtherRemembered"); ok {
		source.Memory.ClearRemembered()
	}
	libPos := 0
	if dest == Library {
		libPos, err = libraryPosition(g, a, source)
		if err != nil {
			return fmt.Errorf("engine: ChangeZoneAll: %w", err)
		}
	}
	_, random := a.Params.Param("RandomOrder")
	_, shuffle := a.Params.Param("Shuffle")
	if !random && !shuffle {
		if dest == Library && len(cards) >= 2 {
			decider := a.Controller
			if spec, ok := a.Params.Param("DefinedPlayer"); ok {
				ps, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs())
				if err != nil || len(ps) == 0 {
					return fmt.Errorf("engine: ChangeZoneAll: DefinedPlayer$ %q not resolvable yet", spec)
				}
				decider = ps[0]
			}
			ordered := controller.OrderCardsForZone(g, decider, cards, dest)
			if err := checkChoice(ordered, cards, len(cards), len(cards)); err != nil {
				return fmt.Errorf("engine: ChangeZoneAll: %w", err)
			}
			cards = ordered
		} else {
			cards, err = g.orderCardsByTheirOwners(controller, cards, dest)
			if err != nil {
				return fmt.Errorf("engine: ChangeZoneAll: %w", err)
			}
		}
	}
	if dest == Library && random {
		cards = append([]CardID(nil), cards...)
		g.rand.Shuffle(len(cards), func(i, j int) { cards[i], cards[j] = cards[j], cards[i] })
	}

	newController := NoPlayer
	if _, ok := a.Params.Param("GainControl"); ok {
		newController = a.Controller
	}
	_, tapped := a.Params.Param("Tapped")
	moved := map[ZoneType][]CardID{}
	var movedOrigins []ZoneType
	for _, id := range cards {
		from := g.Card(id).Zone
		melded := g.moveByEffect(controller, id, dest, libPos, newController, tapped)
		if dest == Exile {
			g.markExiledWith(id, a.Source)
			if melded != NoCard {
				g.markExiledWith(melded, a.Source)
			}
		}
		if _, seen := moved[from]; !seen {
			movedOrigins = append(movedOrigins, from)
		}
		moved[from] = append(moved[from], id)
		if remember, ok := a.Params.Param("RememberChanged"); ok {
			if remember == "True" || filterValid(g, []CardID{id}, remember, a.Controller, a.Source) != nil {
				source.Memory.Remember(CardEntity(id))
			}
		}
		if _, ok := a.Params.Param("ForgetChanged"); ok {
			source.Memory.Forget(CardEntity(id))
		}
		if _, ok := a.Params.Param("Imprint"); ok {
			source.Memory.Imprint(id)
		}
	}
	for _, from := range movedOrigins {
		g.checkChangesZoneAllTriggers(controller, moved[from], from, dest)
	}
	if shuffle {
		for _, pid := range players {
			g.Shuffle(Library, pid)
		}
	}
	return nil
}
