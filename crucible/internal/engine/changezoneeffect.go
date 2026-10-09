package engine

//enginelint:allow id card game player ability defined condition control amount parts zone effecthelpers zonemove scheduledaction replacement

import (
	"fmt"
	"strings"
)

// changeZoneUnresolvedParams are ChangeZoneEffect.java's params this port
// cannot honour yet, grouped by the mechanism each needs: an entering-state
// modifier (Transformed$, FaceDown$, AttachedTo$ ...; WithCountersType$ and
// WithCountersAmount$ resolve, changeZoneEnter),
// combat insertion (Attacking$/Blocking$), a delayed or duration-scoped
// follow-up (AtEOT$, Duration$, LeaveBattlefield$, StaticEffect$), an
// alternative zone prompt (DestinationAlternative$, OriginAlternative$), a
// non-default search loop (AtRandom$, Different*$, WithTotal*$, Exactly$,
// Reorder$), a different chooser (Chooser$), or an LKI copy in Memory
// (RememberLKI$).
var changeZoneUnresolvedParams = [...]string{
	"Duration", "Transformed", "WithNotedCounters",
	"AttachedTo", "AttachedToPlayer", "AttachAfter", "FaceDown", "ExileFaceDown",
	"Attacking", "Blocking", "LeaveBattlefield", "AtEOT", "StaticEffect",
	"DestinationAlternative", "LibraryPositionAlternative", "OriginAlternative",
	"RememberLKI", "Champion", "Foretold", "ForetoldCost", "WithMayLook", "TrackDiscarded",
	"RandomOrder", "Chooser", "AtRandom", "DifferentNames", "DifferentCMC", "DifferentPower",
	"ShareLandType", "WithTotalCMC", "WithTotalPower", "WithTotalCardTypes",
	"ShuffleChangedPile", "Reorder", "Exactly", "RememberSearched",
	"AlreadyRevealed", "ImprintLast", "TargetsWithDefinedController", "Unearth",
	"Condition", "SorcerySpeed", "Ultimate", "ModeCost",
	// ExiledWithEffectSource$ marks the effect card's own source as the
	// exiler instead of the host (SpellAbilityEffect.java:1092-1094).
	"ExiledWithEffectSource",
}

// anyZoneOrigin is what Origin$ All covers: every zone a card this port moves
// can be in. Command is left out, which only an effect card exiling itself
// may name.
var anyZoneOrigin = [...]ZoneType{Hand, Library, Graveyard, Battlefield, Exile, Stack, Sideboard}

// changeZoneEffect is ChangeZoneEffect.java, the corpus's single most
// common API (5,576 real (AB|DB)$ lines). Java splits it on
// SpellAbility.isHidden -- Hidden$, or an Origin$ naming a hidden zone
// (Library, Hand, Sideboard) -- into changeHiddenOriginResolve (search a zone, choose,
// move) and changeKnownOriginResolve (move already-identified cards); this
// port keeps that split.
type changeZoneEffect struct{}

func (changeZoneEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range changeZoneUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: ChangeZone: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	destName, _ := a.Params.Param("Destination")
	dest, err := changeZoneDestination(destName)
	if err != nil {
		return fmt.Errorf("engine: ChangeZone: %w", err)
	}
	var origin []ZoneType
	anyOrigin := false
	if raw, ok := a.Params.Param("Origin"); ok && raw == "All" {
		// Origin$ All: the card is wherever it is (a replaced card still on
		// the stack, in hand or in a graveyard). It names no hidden zone, so
		// it does not make the ability hidden by itself.
		origin, anyOrigin = anyZoneOrigin[:], true
	} else if ok {
		origin, err = parseZoneList(raw)
		if err != nil {
			return fmt.Errorf("engine: ChangeZone: Origin$: %w", err)
		}
		for _, z := range origin {
			// Command only for an effect card exiling itself -- the corpus's
			// 180-odd "Origin$ Command | Destination$ Exile" lines
			// (changeZoneKnown's IsEffect branch).
			if z == Command && dest == Exile {
				continue
			}
			// Stack only for a spell exiling itself ("Exile CARDNAME", Mnemonic
			// Betrayal): the loop below refuses any other spell.
			if z != Battlefield && z != Graveyard && z != Hand && z != Library && z != Exile && z != Sideboard && z != Stack {
				return fmt.Errorf("engine: ChangeZone: Origin$ %v not resolvable yet", z)
			}
		}
	}
	newController, err := changeZoneGainControl(g, a)
	if err != nil {
		return err
	}
	// SpellAbility.isHidden: Hidden$, or an Origin$ naming a hidden zone --
	// Library, Hand, Sideboard (ZoneType.java:15-23).
	_, hidden := a.Params.Param("Hidden")
	if hidden || (!anyOrigin && (zoneIn(Library, origin) || zoneIn(Hand, origin) || zoneIn(Sideboard, origin))) {
		return changeZoneHidden(g, a, controller, source, origin, dest, newController)
	}
	return changeZoneKnown(g, a, controller, source, origin, dest, newController)
}

// changeZoneEnter reads WithCountersType$ (a comma list, each kind added) and
// WithCountersAmount$ (default 1): the counters a permanent entering the
// battlefield gets (ChangeZoneEffect.java:614). "Any" is a choice this port does
// not make, and counters on a destination other than the battlefield are not
// applied, so either is an error rather than a silent omission (GO-7).
func changeZoneEnter(g *Game, a *Ability, dest ZoneType) ([]enterCounters, error) {
	raw, ok := a.Params.Param("WithCountersType")
	if !ok {
		return nil, nil
	}
	if dest != Battlefield {
		return nil, fmt.Errorf("engine: ChangeZone: WithCountersType$ with Destination$ %v not resolvable yet", dest)
	}
	n := 1
	if amount, ok := a.Params.Param("WithCountersAmount"); ok {
		v, ok := resolveNamedAmount(g, a.Amounts, g.Card(a.Source), amount)
		if !ok {
			return nil, fmt.Errorf("engine: ChangeZone: WithCountersAmount$ %q is not resolvable", amount)
		}
		n = v
	}
	var out []enterCounters
	for _, kind := range strings.Split(raw, ",") {
		kind = strings.TrimSpace(kind)
		if kind == "" || strings.EqualFold(kind, "Any") {
			return nil, fmt.Errorf("engine: ChangeZone: WithCountersType$ %q not resolvable yet", raw)
		}
		out = append(out, enterCounters{kind: CounterType(strings.ToUpper(kind)), n: n})
	}
	return out, nil
}

// changeZoneGainControl reads GainControl$: "True" is the activator,
// anything else a Defined$ player spec whose first player takes control.
func changeZoneGainControl(g *Game, a *Ability) (PlayerID, error) {
	raw, ok := a.Params.Param("GainControl")
	if !ok {
		return NoPlayer, nil
	}
	if strings.EqualFold(raw, "True") {
		return a.Controller, nil
	}
	players, err := definedPlayers(g, a.Controller, a.Source, raw, a.refs())
	if err != nil {
		return NoPlayer, fmt.Errorf("engine: ChangeZone: GainControl$: %w", err)
	}
	if len(players) == 0 {
		return NoPlayer, nil
	}
	return players[0], nil
}

// changeZoneMemory applies RememberChanged$/ForgetChanged$/Imprint$ for a
// card that actually moved.
func changeZoneMemory(a *Ability, source *Card, id CardID) {
	if _, ok := a.Params.Param("RememberChanged"); ok {
		source.Memory.Remember(CardEntity(id))
	}
	if _, ok := a.Params.Param("ForgetChanged"); ok {
		source.Memory.Forget(CardEntity(id))
	}
	if _, ok := a.Params.Param("Imprint"); ok {
		source.Memory.Imprint(id)
	}
}

// changeZonePreMemory applies Unimprint$/ForgetOtherRemembered$, which Java
// runs before any card moves.
func changeZonePreMemory(a *Ability, source *Card) {
	if _, ok := a.Params.Param("Unimprint"); ok {
		source.Memory.ClearImprinted()
	}
	if _, ok := a.Params.Param("ForgetOtherRemembered"); ok {
		source.Memory.ClearRemembered()
	}
}

// changeZoneKnown is changeKnownOriginResolve: the targeted or Defined$
// cards (default Self) still in an Origin$ zone move to Destination$.
// Optional$ asks per card; ShuffleNonMandatory$ asks once up front; several
// cards heading into a library are ordered by their owners first (CR
// 401.4) unless Shuffle$ True will shuffle them anyway.
func changeZoneKnown(g *Game, a *Ability, controller PlayerController, source *Card, origin []ZoneType, dest ZoneType, newController PlayerID) error {
	enter, err := changeZoneEnter(g, a, dest)
	if err != nil {
		return err
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ChangeZone: %w", err)
	}
	libPos, err := libraryPosition(g, a, source)
	if err != nil {
		return fmt.Errorf("engine: ChangeZone: %w", err)
	}
	changeZonePreMemory(a, source)
	shuffleParam, _ := a.Params.Param("Shuffle")
	shuffle := shuffleParam == "True"
	if _, ok := a.Params.Param("ShuffleNonMandatory"); ok && !controller.ConfirmEffect(g, a.Controller, a.Source) {
		return nil
	}
	if dest == Library && len(cards) > 1 && !shuffle {
		cards, err = g.orderCardsByTheirOwners(controller, cards, dest)
		if err != nil {
			return fmt.Errorf("engine: ChangeZone: %w", err)
		}
	}
	for _, id := range cards {
		if c := g.Card(id); c.Zone == Stack && zoneIn(Stack, origin) && id != a.Source && (a.replacing == nil || a.replacing.card != id) {
			return fmt.Errorf("engine: ChangeZone: Origin$ Stack for a spell other than its own host not resolvable yet")
		}
		if c := g.Card(id); c.Zone == Command && zoneIn(Command, origin) && !c.IsEffect {
			return fmt.Errorf("engine: ChangeZone: Origin$ Command for a card that is not an effect not resolvable yet")
		}
	}
	_, optional := a.Params.Param("Optional")
	_, tapped := a.Params.Param("Tapped")
	moved := map[ZoneType][]CardID{}
	var movedOrigins []ZoneType
	for _, id := range cards {
		c := g.Card(id)
		if len(origin) > 0 && !zoneIn(c.Zone, origin) {
			continue
		}
		if c.IsEffect && c.Zone == Command {
			// GameAction.changeZone's immutable branch
			// (GameAction.java:100-106): an effect card sent to exile just
			// leaves the Command zone, with no zone-change event or trigger.
			g.exileEffect(id)
			continue
		}
		// A phased-out permanent is not moved (ChangeZoneEffect.java:557,
		// ADR-0021 decision 3).
		if c.IsPhasedOut() {
			continue
		}
		if optional && !controller.ConfirmEffect(g, a.Controller, a.Source) {
			continue
		}
		from := c.Zone
		melded := g.moveByEffect(controller, id, dest, libPos, newController, tapped, enter...)
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
		changeZoneMemory(a, source, id)
	}
	for _, from := range movedOrigins {
		g.checkChangesZoneAllTriggers(controller, moved[from], from, dest)
	}
	if dest == Library && shuffle {
		var owners []PlayerID
		for _, id := range cards {
			owners = appendUniquePlayer(owners, g.Card(id).Owner)
		}
		if len(owners) == 0 {
			owners = []PlayerID{a.Controller}
		}
		for _, pid := range owners {
			g.Shuffle(Library, pid)
		}
	}
	return nil
}

// hiddenChoice is one fetcher's pick, held until every fetcher has chosen --
// Java's HiddenOriginChoices, which moves nothing until all players decide.
type hiddenChoice struct {
	player PlayerID
	chosen []CardID
	// shuffleMandatory is this fetcher's own flag (ChangeZoneEffect.java:999,
	// declared per fetcher): a blocked search turns it to
	// !ShuffleNonMandatory$.
	shuffleMandatory bool
	// found is HiddenOriginChoices.searchedLibrary, the FoundSearchingLibrary
	// run param every move of this fetcher's cards carries
	// (ChangeZoneEffect.java:1277, :1308): the library was really searched --
	// not blocked, not NoLooking$, and not a Searched$ ability, which Java
	// turns back to false once its grant is made (:1090-1092).
	found bool
}

// canSearchLibraryWith is Player.canSearchLibraryWith (Player.java:3749): a
// decider with the CantSearchLibrary keyword (Leonin Arbiter) cannot search at
// all, and one with "Spells and abilities you control can't cause you to
// search your library." cannot search their own library with an ability they
// control.
func (g *Game) canSearchLibraryWith(decider, target, activator PlayerID) bool {
	p := g.Player(decider)
	if p.HasKeyword("CantSearchLibrary") {
		return false
	}
	return target != activator || !p.HasKeyword("Spells and abilities you control can't cause you to search your library.")
}

// changeZoneHidden is changeHiddenOriginResolve: each fetcher (DefinedPlayer$,
// default You; the first targeted player when ValidTgts$ names one and
// DefinedPlayer$ is absent) searches the Origin$ zones for up to ChangeNum$
// (default 1) cards matching ChangeType$ and they move to Destination$. A
// fetcher another player controls does not search Sideboard (the Wish rule,
// ChangeZoneEffect.java:987-991).
// Defined$ skips the search and takes the named cards in order;
// ChooseFromDefined$ offers them as the choice instead. A library that was
// searched is shuffled afterwards unless NoShuffle$ or Shuffle$ False --
// before the move when the destination is the library itself, so a fetched
// card placed on top stays there.
func changeZoneHidden(g *Game, a *Ability, controller PlayerController, source *Card, origin []ZoneType, dest ZoneType, newController PlayerID) error {
	enter, err := changeZoneEnter(g, a, dest)
	if err != nil {
		return err
	}
	fetchSpec, hasDefinedPlayer := a.Params.Param("DefinedPlayer")
	if !hasDefinedPlayer {
		fetchSpec = "You"
	}
	fetchers, err := definedPlayers(g, a.Controller, a.Source, fetchSpec, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ChangeZone: DefinedPlayer$: %w", err)
	}
	_, usesTargeting := a.Params.Param("ValidTgts")
	chooseFrom, chooseFromDefined := a.Params.Param("ChooseFromDefined")
	definedSpec, hasDefined := a.Params.Param("Defined")
	defined := hasDefined || chooseFromDefined
	changeType, _ := a.Params.Param("ChangeType")
	if strings.HasPrefix(changeType, "EACH") {
		return fmt.Errorf("engine: ChangeZone: ChangeType$ %q not resolvable yet", changeType)
	}
	_, mandatory := a.Params.Param("Mandatory")
	_, optional := a.Params.Param("Optional")
	_, noShuffle := a.Params.Param("NoShuffle")
	_, searchedParam := a.Params.Param("Searched")
	shuffleParam, _ := a.Params.Param("Shuffle")
	shuffleMandatory := !noShuffle && shuffleParam != "False"
	libPos, err := libraryPosition(g, a, source)
	if err != nil {
		return fmt.Errorf("engine: ChangeZone: %w", err)
	}

	var picks []hiddenChoice
	for _, player := range fetchers {
		if usesTargeting && !hasDefinedPlayer {
			player = NoPlayer
			for _, e := range a.Targets {
				if pid, ok := e.AsPlayer(); ok {
					player = pid
					break
				}
			}
			if player == NoPlayer {
				continue
			}
		}
		if g.Player(player).Lost {
			continue
		}
		if optional && !controller.ConfirmEffect(g, player, a.Source) {
			continue
		}
		// ChangeZoneEffect.java:987-991: a Wish fetcher another player
		// controls cannot search outside the game (CR 800.4b, ADR-0030).
		// A copy, so the next fetcher and the shuffle checks below still
		// see the whole Origin$.
		searched := origin
		if g.IsControlled(player) && zoneIn(Sideboard, origin) {
			searched = nil
			for _, z := range origin {
				if z != Sideboard {
					searched = append(searched, z)
				}
			}
		}
		changeNum := 1
		if raw, ok := a.Params.Param("ChangeNum"); ok {
			n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
			if !ok {
				return fmt.Errorf("engine: ChangeZone: ChangeNum$ %q not resolvable yet", raw)
			}
			changeNum = n
		}

		var fetchList []CardID
		fetcherShuffle, searchedLibrary, blocked := shuffleMandatory, false, false
		switch {
		case defined:
			spec := definedSpec
			if chooseFromDefined {
				spec = chooseFrom
			}
			fetchList, err = definedCards(source, spec, a.refs())
			if err != nil {
				return fmt.Errorf("engine: ChangeZone: %w", err)
			}
			if _, ok := a.Params.Param("ChangeNum"); !ok {
				changeNum = len(fetchList)
			}
		case !zoneIn(Library, searched) && !zoneIn(Hand, searched) && !hasDefinedPlayer:
			for _, z := range searched {
				for _, pid := range g.Players() {
					fetchList = append(fetchList, g.Zone(z, pid).Cards()...)
				}
			}
		default:
			// ChangeZoneEffect.java:1018-1039: a library search the decider may
			// not make (CantSearchLibrary) offers nothing from the library, and
			// the shuffle then follows ShuffleNonMandatory$.
			_, noLooking := a.Params.Param("NoLooking")
			if zoneIn(Library, searched) && !noLooking {
				searchedLibrary = true
				if !g.canSearchLibraryWith(player, player, a.Controller) {
					blocked = true
					searchedLibrary = false
					_, nonMandatory := a.Params.Param("ShuffleNonMandatory")
					fetcherShuffle = !nonMandatory && !noShuffle && shuffleParam != "False"
				}
			}
			for _, z := range searched {
				if z == Library && blocked {
					continue
				}
				fetchList = append(fetchList, g.Zone(z, player).Cards()...)
			}
		}
		if !defined && changeType != "" {
			fetchList = filterValid(g, fetchList, changeType, a.Controller, a.Source)
		}

		changeZonePreMemory(a, source)

		var chosen []CardID
		if defined && !chooseFromDefined {
			n := changeNum
			if n > len(fetchList) {
				n = len(fetchList)
			}
			chosen = append(chosen, fetchList[:n]...)
		} else {
			hi := changeNum
			if hi > len(fetchList) {
				hi = len(fetchList)
			}
			lo := 0
			if mandatory {
				lo = hi
			}
			if hi > 0 {
				// ChangeZoneEffect.java:1070-1076, 1268: a searched library is
				// searched under the control grant ControlOpponentsSearching-
				// Library$ gives (ADR-0040), popped after the pick. Searched$
				// counts as a search for the grant (:1067-1069).
				var end func()
				if searchedLibrary || searchedParam {
					end = g.beginSearchControl(player)
				}
				chosen = controller.ChooseCardsForEffect(g, player, a.Source, fetchList, lo, hi)
				if end != nil {
					end()
				}
				if err := checkChoice(chosen, fetchList, lo, hi); err != nil {
					return fmt.Errorf("engine: ChangeZone: %w", err)
				}
			}
		}
		if zoneIn(Library, origin) && dest == Library && fetcherShuffle {
			g.Shuffle(Library, player)
		}
		picks = append(picks, hiddenChoice{player: player, chosen: chosen, shuffleMandatory: fetcherShuffle,
			found: searchedLibrary && !searchedParam})
	}

	_, tapped := a.Params.Param("Tapped")
	for _, pick := range picks {
		moved := map[ZoneType][]CardID{}
		var movedOrigins []ZoneType
		for _, id := range pick.chosen {
			from := g.Card(id).Zone
			if pick.found && g.foundSearchReplaced(controller, id, dest) {
				// GameAction.changeZone returns the card a Moved replacement
				// sent elsewhere (GameAction.java:341-368), and Java goes on with
				// it: ExiledWith for an exiling search, then RememberChanged$ and
				// Imprint$ (ChangeZoneEffect.java:1416-1507). The replacement's own
				// move fired its zone-change triggers.
				if dest == Exile {
					g.markExiledWith(id, a.Source)
				}
				changeZoneMemory(a, source, id)
				continue
			}
			melded := g.moveByEffect(controller, id, dest, libPos, newController, tapped, enter...)
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
			changeZoneMemory(a, source, id)
		}
		for _, from := range movedOrigins {
			g.checkChangesZoneAllTriggers(controller, moved[from], from, dest)
		}
		if (zoneIn(Library, origin) && dest != Library && !defined && pick.shuffleMandatory) || shuffleParam == "True" {
			g.Shuffle(Library, pick.player)
		}
	}
	return nil
}

// appendUniquePlayer appends pid unless ps already holds it, keeping first-
// seen order (Java's FCollection set semantics).
func appendUniquePlayer(ps []PlayerID, pid PlayerID) []PlayerID {
	for _, p := range ps {
		if p == pid {
			return ps
		}
	}
	return append(ps, pid)
}
