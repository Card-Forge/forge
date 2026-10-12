package engine

//enginelint:allow id card game player ability defined condition control amount parts zone effecthelpers zonemove scheduledaction replacement controlcommands

import (
	"fmt"
	"strings"
)

// changeZoneUnresolvedParams are ChangeZoneEffect.java's params this port
// cannot honour yet, grouped by the mechanism each needs: an entering-state
// modifier (Transformed$, FaceDown$, AttachedTo$ ...; WithCountersType$ and
// WithCountersAmount$ resolve, changeZoneEnter),
// combat insertion (Attacking$/Blocking$), a delayed follow-up (AtEOT$,
// LeaveBattlefield$, StaticEffect$; Duration$ UntilHostLeavesPlay resolves,
// changeZoneDuration, but not its ReturnAbility$/ReturnValid$ delayed
// trigger), an alternative destination prompt (DestinationAlternative$), a
// non-default search loop (AtRandom$, Different*$, WithTotal*$, Exactly$,
// Reorder$), or an LKI copy in Memory (RememberLKI$). Chooser$ and
// OriginAlternative$ resolve (changeZoneHidden, changeZoneKnown).
// AlreadyRevealed$ is read only to skip the DelayedReveal the decider is shown
// while choosing (ChangeZoneEffect.java:1044), a display this port never
// makes, so it is accepted and changes nothing.
var changeZoneUnresolvedParams = [...]string{
	"Transformed", "WithNotedCounters", "ReturnAbility", "ReturnValid",
	"AttachedTo", "AttachedToPlayer", "AttachAfter", "FaceDown", "ExileFaceDown",
	"Attacking", "Blocking", "LeaveBattlefield", "AtEOT", "StaticEffect",
	"DestinationAlternative", "LibraryPositionAlternative",
	"RememberLKI", "Champion", "Foretold", "ForetoldCost", "WithMayLook", "TrackDiscarded",
	"RandomOrder", "AtRandom", "DifferentNames", "DifferentCMC", "DifferentPower",
	"ShareLandType", "WithTotalCMC", "WithTotalPower", "WithTotalCardTypes",
	"ShuffleChangedPile", "Reorder", "Exactly", "RememberSearched",
	"ImprintLast", "Unearth",
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
	until, err := changeZoneDuration(a)
	if err != nil {
		return err
	}
	// checkValidDuration (ChangeZoneEffect.java:450,
	// SpellAbilityEffect.java:1044-1048): a host that is neither in play nor
	// on the stack any more -- a Banisher Priest killed in response to its own
	// trigger -- makes the whole effect do nothing.
	if until && source.Zone != Battlefield && source.Zone != Stack {
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
		return changeZoneHidden(g, a, controller, source, origin, dest, newController, until)
	}
	// OriginAlternative$ is read by changeHiddenOriginResolve only
	// (ChangeZoneEffect.java:939); every corpus line names Origin$ Library, so
	// a known-origin one would be a script the Java ignores it on.
	if raw, ok := a.Params.Param("OriginAlternative"); ok {
		return fmt.Errorf("engine: ChangeZone: OriginAlternative$ %q on a known-origin move not resolvable", raw)
	}
	return changeZoneKnown(g, a, controller, source, origin, dest, newController, until)
}

// changeZoneDuration reads Duration$. UntilHostLeavesPlay, the only value
// the corpus's ChangeZone lines name (112 of 112), is changeZoneUntilCommand
// (SpellAbilityEffect.java:833-915): the moved cards come back when the host
// leaves the battlefield. Any other value is not resolvable yet (GO-7).
func changeZoneDuration(a *Ability) (bool, error) {
	d, ok := a.Params.Param("Duration")
	if !ok {
		return false, nil
	}
	if d != "UntilHostLeavesPlay" {
		return false, fmt.Errorf("engine: ChangeZone: Duration$ %q not resolvable yet", d)
	}
	return true, nil
}

// untilReturns builds changeZoneUntilCommand's records for the cards moved
// out of from: one commandReturnExiled per card that really changed zones,
// stamped with the object it became (equalsWithGameTimestamp,
// SpellAbilityEffect.java:875). A token is left out: it ceases to exist once
// exiled and never returns (SpellAbilityEffect.java:840-841). Cards are kept
// wherever the move put them, the way Java's CardZoneTable holds them: a
// redirected one is recorded at its actual zone, not filtered.
func (g *Game) untilReturns(cmds []cardCommand, host CardID, from ZoneType, ids []CardID) []cardCommand {
	for _, id := range ids {
		c := g.Card(id)
		if c.IsToken || c.Zone == from {
			continue
		}
		cmds = append(cmds, cardCommand{Kind: commandReturnExiled, Target: id, Timestamp: c.zoneStamp, Host: host, Zone: from})
	}
	return cmds
}

// armUntilHostLeaves is the end of changeZoneUntilCommand
// (SpellAbilityEffect.java:909-914): the return goes on the host's
// leaves-play list (addUntilCommand's UntilHostLeavesPlay branch, :999-1000),
// or, when the host is no longer in play -- it exiled itself, or it is a
// spell still on the stack -- runs at once.
func (g *Game) armUntilHostLeaves(controller PlayerController, host CardID, cmds []cardCommand) {
	if len(cmds) == 0 {
		return
	}
	h := g.Card(host)
	if h.Zone != Battlefield {
		g.pendingReturns = append(g.pendingReturns, cmds...)
		g.returnUntilCards(controller)
		return
	}
	h.leavesPlayCmds = append(h.leavesPlayCmds, cmds...)
}

// changeZoneChooser reads Chooser$ for a known-origin move
// (ChangeZoneEffect.java:530-533): the first player it names, else the
// activator. Java's get(0) throws on an empty list; here that is an error.
func changeZoneChooser(g *Game, a *Ability) (PlayerID, bool, error) {
	spec, ok := a.Params.Param("Chooser")
	if !ok {
		return a.Controller, false, nil
	}
	players, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs())
	if err != nil {
		return NoPlayer, false, fmt.Errorf("engine: ChangeZone: Chooser$: %w", err)
	}
	if len(players) == 0 {
		return NoPlayer, false, fmt.Errorf("engine: ChangeZone: Chooser$ %q names no player", spec)
	}
	return players[0], true, nil
}

// hiddenChooser reads Chooser$ for a search (ChangeZoneEffect.java:911-917):
// the activator picks one of the players it names when it names several, and
// NoPlayer -- each fetcher decides for themselves -- when it names none.
func hiddenChooser(g *Game, a *Ability, controller PlayerController) (PlayerID, error) {
	spec, ok := a.Params.Param("Chooser")
	if !ok {
		return NoPlayer, nil
	}
	players, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs())
	if err != nil {
		return NoPlayer, fmt.Errorf("engine: ChangeZone: Chooser$: %w", err)
	}
	switch len(players) {
	case 0:
		return NoPlayer, nil
	case 1:
		return players[0], nil
	}
	pick := controller.ChoosePlayerForEffect(g, a.Controller, a.Source, players)
	for _, p := range players {
		if p == pick {
			return pick, nil
		}
	}
	return NoPlayer, fmt.Errorf("engine: ChangeZone: Chooser$ answer %v is not one of %v", pick, players)
}

// alternativeOrigin is the OriginAlternative$ prompt (ChangeZoneEffect.java:
// 939-970) for one fetcher: decider is asked whether to search Origin$ (no
// clears it), then about each alternative zone in turn while more than one
// zone is still in play; an empty result searches the alternatives left. A
// zone that is not hidden (CR 400.2, the graveyard) holding a ChangeType$
// match of player's makes the pick mandatory.
func alternativeOrigin(g *Game, a *Ability, controller PlayerController, decider, player PlayerID, origin []ZoneType, changeType string) ([]ZoneType, bool, error) {
	raw, _ := a.Params.Param("OriginAlternative")
	alt, err := parseZoneList(raw)
	if err != nil {
		return nil, false, fmt.Errorf("engine: ChangeZone: OriginAlternative$: %w", err)
	}
	var altFetch []CardID
	for _, z := range alt {
		altFetch = append(altFetch, g.Zone(z, player).Cards()...)
	}
	if changeType != "" {
		altFetch = filterValid(g, altFetch, changeType, a.Controller, a.Source)
	}
	out := append([]ZoneType(nil), origin...)
	if !controller.ConfirmEffect(g, decider, a.Source) {
		out = nil
	}
	for len(alt) > 0 && len(out)+len(alt) != 1 {
		if controller.ConfirmEffect(g, decider, a.Source) {
			out = append(out, alt[0])
		}
		alt = alt[1:]
	}
	if len(out) == 0 {
		out = alt
	}
	mandatory := false
	for _, z := range out {
		if z == Hand || z == Library || z == Sideboard {
			continue
		}
		for _, id := range altFetch {
			if g.Card(id).Zone == z {
				mandatory = true
			}
		}
	}
	return out, mandatory, nil
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
// 401.4) unless Shuffle$ True will shuffle them anyway. A Chooser$ answers
// Optional$ and orders those cards itself (ChangeZoneEffect.java:530-546,
// 566). With until (Duration$ UntilHostLeavesPlay) the moved cards return
// when the host leaves the battlefield (changeZoneUntilCommand, :865).
func changeZoneKnown(g *Game, a *Ability, controller PlayerController, source *Card, origin []ZoneType, dest ZoneType, newController PlayerID, until bool) error {
	enter, err := changeZoneEnter(g, a, dest)
	if err != nil {
		return err
	}
	chooser, hasChooser, err := changeZoneChooser(g, a)
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
		if hasChooser {
			ordered := controller.OrderCardsForZone(g, chooser, cards, dest)
			if err := checkChoice(ordered, cards, len(cards), len(cards)); err != nil {
				return fmt.Errorf("engine: ChangeZone: ordering cards into %v: %w", dest, err)
			}
			cards = ordered
		} else if cards, err = g.orderCardsByTheirOwners(controller, cards, dest); err != nil {
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
		if optional && !controller.ConfirmEffect(g, chooser, a.Source) {
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
	if until {
		var cmds []cardCommand
		for _, from := range movedOrigins {
			cmds = g.untilReturns(cmds, a.Source, from, moved[from])
		}
		g.armUntilHostLeaves(controller, a.Source, cmds)
	}
	if dest == Library && shuffle {
		owners, err := changeZoneShufflers(g, a, cards)
		if err != nil {
			return err
		}
		for _, pid := range owners {
			g.Shuffle(Library, pid)
		}
	}
	return nil
}

// changeZoneShufflers is whose library a Shuffle$ True move into a library
// shuffles (ChangeZoneEffect.java:873-890): the players
// TargetsWithDefinedController$ names when present -- "it does need to work
// even without Targets" (Gaea's Blessing's ParentTarget with no card chosen),
// no fallback when it names nobody -- else every targeted card's owner, else
// the activator. TargetsWithDefinedController$'s other reading, the targeting
// restriction (SpellAbility.canTarget, SpellAbility.java:1405-1416), is
// targeting.go's.
func changeZoneShufflers(g *Game, a *Ability, cards []CardID) ([]PlayerID, error) {
	if spec, ok := a.Params.Param("TargetsWithDefinedController"); ok {
		players, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs())
		if err != nil {
			return nil, fmt.Errorf("engine: ChangeZone: TargetsWithDefinedController$: %w", err)
		}
		// PlayerCollection: each player once, first-seen order.
		var unique []PlayerID
		for _, p := range players {
			unique = appendUniquePlayer(unique, p)
		}
		return unique, nil
	}
	var owners []PlayerID
	for _, id := range cards {
		owners = appendUniquePlayer(owners, g.Card(id).Owner)
	}
	if len(owners) == 0 {
		owners = []PlayerID{a.Controller}
	}
	return owners, nil
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
	// origin is HiddenOriginChoices.origin: the zones this fetcher searched,
	// which OriginAlternative$ can narrow or widen from Origin$ -- "if you
	// search your library this way, shuffle" reads it (:1522).
	origin []ZoneType
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
//
// The decider is the Chooser$ player, else the fetcher as DefinedPlayer$
// names it (ChangeZoneEffect.java:919-923) -- taken before a ValidTgts$
// fetcher is swapped for the targeted player, so a targeted search with
// neither DefinedPlayer$ nor Chooser$ is decided by the activator. The
// decider answers Optional$ and the OriginAlternative$ prompts, makes the
// pick, and is the one CantSearchLibrary is checked on; the
// ControlOpponentsSearchingLibrary$ grant applies only to a fetcher deciding
// their own search (:1071).
func changeZoneHidden(g *Game, a *Ability, controller PlayerController, source *Card, origin []ZoneType, dest ZoneType, newController PlayerID, until bool) error {
	enter, err := changeZoneEnter(g, a, dest)
	if err != nil {
		return err
	}
	chooser, err := hiddenChooser(g, a, controller)
	if err != nil {
		return err
	}
	_, hasAlt := a.Params.Param("OriginAlternative")
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
	for _, fetcher := range fetchers {
		decider := chooser
		if decider == NoPlayer {
			decider = fetcher
		}
		player := fetcher
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
		fetchOrigin := origin
		if hasAlt {
			var altMandatory bool
			fetchOrigin, altMandatory, err = alternativeOrigin(g, a, controller, decider, player, origin, changeType)
			if err != nil {
				return err
			}
			// Java's mandatory is the method's own flag: once an
			// alternative zone makes it so, later fetchers keep it.
			mandatory = mandatory || altMandatory
		}
		if optional && !controller.ConfirmEffect(g, decider, a.Source) {
			continue
		}
		// ChangeZoneEffect.java:987-991: a Wish fetcher another player
		// controls cannot search outside the game (CR 800.4b, ADR-0030).
		// A copy, so the next fetcher still sees the whole Origin$.
		searched := fetchOrigin
		if g.IsControlled(player) && zoneIn(Sideboard, fetchOrigin) {
			searched = nil
			for _, z := range fetchOrigin {
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
				if !g.canSearchLibraryWith(decider, player, a.Controller) {
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
				if (searchedLibrary || searchedParam) && decider == player {
					end = g.beginSearchControl(player)
				}
				chosen = controller.ChooseCardsForEffect(g, decider, a.Source, fetchList, lo, hi)
				if end != nil {
					end()
				}
				if err := checkChoice(chosen, fetchList, lo, hi); err != nil {
					return fmt.Errorf("engine: ChangeZone: %w", err)
				}
			}
		}
		if zoneIn(Library, searched) && dest == Library && fetcherShuffle {
			g.Shuffle(Library, player)
		}
		picks = append(picks, hiddenChoice{player: player, chosen: chosen, shuffleMandatory: fetcherShuffle,
			found: searchedLibrary && !searchedParam, origin: searched})
	}

	_, tapped := a.Params.Param("Tapped")
	// returns is changeZoneUntilCommand's table across every fetcher's
	// moves (ChangeZoneEffect.java:1538), armed once they are all done.
	var returns []cardCommand
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
				if until {
					returns = g.untilReturns(returns, a.Source, from, []CardID{id})
				}
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
			if until {
				returns = g.untilReturns(returns, a.Source, from, moved[from])
			}
		}
		if (zoneIn(Library, pick.origin) && dest != Library && !defined && pick.shuffleMandatory) || shuffleParam == "True" {
			g.Shuffle(Library, pick.player)
		}
	}
	g.armUntilHostLeaves(controller, a.Source, returns)
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
