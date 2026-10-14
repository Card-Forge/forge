// Operator-aware amounts: the heads whose arithmetic suffix Java applies
// inside the measurement rather than after it -- a card list measured by
// handlePaid (Remembered$, Sacrificed$, Targeted$, ...) and a player list
// measured by playerXCount/playerXProperty (PlayerCount<hType>$,
// TargetedPlayer$, TriggeredPlayer$, ...). Where the operator lands is
// observable: playerXCount answers 0 for no players whatever the operator,
// sums each player's already-operated value, and handlePaid operates each
// card's value before summing (PORT-7).
//
// Ported from forge-game/src/main/java/forge/game/ability/AbilityUtils.java's
// calculateAmount (:456-500, :572-636, :672-719), playerXCount (:3355-3487)
// and playerXProperty (:3489-3701).

package engine

import (
	"slices"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// opAwareValue evaluates amt when its head is a card or player list, the
// operator applied where Java applies it. handled is false for every other
// head, which resolveAmountDepth then evaluates with the operator after.
func opAwareValue(g *Game, amounts map[string]expr.Amount, sourceController PlayerID, source CardID, amt expr.Amount, depth int) (n int, ok, handled bool) {
	if amt.Count != nil || amt.Head == "" {
		return 0, false, false
	}
	head := amt.Head
	if hType, isPlayerCount := strings.CutPrefix(head, "PlayerCount"); isPlayerCount {
		players, ok := g.playerCountPlayers(amounts, sourceController, source, hType, depth)
		if !ok {
			return 0, false, true
		}
		n, ok := playerXCount(g, amounts, sourceController, source, players, amt.Body, amt.Op, depth)
		return n, ok, true
	}
	if cards, ok, isList := g.listHeadCards(sourceController, source, head); isList && !isAbilityPlayerHead(head) {
		if !ok {
			return 0, false, true
		}
		if len(cards) == 0 && (head == "Sacrificed" || head == "Exiled" || head == "Discarded") {
			// A cost list nothing was paid into is Java's null paid list
			// (SpellAbility.getPaidList): the amount is 0, the operator unread
			// (AbilityUtils.java:715-729).
			return 0, true, true
		}
		n, ok := handlePaid(g, amounts, sourceController, source, cards, amt.Body, amt.Op, depth)
		return n, ok, true
	}
	if !isAbilityPlayerHead(head) {
		return 0, false, false
	}
	a := g.resolving
	if a == nil || source == NoCard || a.Source != source {
		// An ability-context head read outside its own ability's resolution
		// (a static, a cost being checked): not resolvable here (GO-7).
		return 0, false, true
	}
	refs := a.refs()
	switch {
	case strings.HasPrefix(head, "TargetedPlayer"):
		players, ok := targetingPlayers(refs)
		if !ok {
			return 0, false, true
		}
		n, ok := playerXCount(g, amounts, sourceController, source, players, amt.Body, amt.Op, depth)
		return n, ok, true
	case strings.HasPrefix(head, "ThisTargetedPlayer"):
		var players []PlayerID
		for _, e := range refs.targets {
			if pid, isPlayer := e.AsPlayer(); isPlayer && !slices.Contains(players, pid) {
				players = append(players, pid)
			}
		}
		n, ok := playerXCount(g, amounts, sourceController, source, players, amt.Body, amt.Op, depth)
		return n, ok, true
	case strings.HasPrefix(head, "TargetedController"), strings.HasPrefix(head, "TriggeredPlayers"), head == "TriggeredCardController":
		defined := head
		if rest, isPlural := strings.CutPrefix(head, "TriggeredPlayers"); isPlural {
			defined = "Triggered" + rest
		} else if strings.HasPrefix(head, "TargetedController") {
			defined = "TargetedController"
		}
		players, err := definedPlayers(g, a.Controller, source, defined, refs)
		if err != nil {
			return 0, false, true
		}
		n, ok := playerXCount(g, amounts, sourceController, source, players, amt.Body, amt.Op, depth)
		return n, ok, true
	}
	// TriggeredPlayer/TriggeredTarget/TriggeredDefendingPlayer/
	// TriggeredActivator: one triggering object, playerXProperty when it is a
	// player and 0 when it is not (AbilityUtils.java:631-636).
	key := triggeredPlayerKey(head)
	players, err := definedPlayers(g, a.Controller, source, key, refs)
	if err != nil {
		return 0, false, true
	}
	if len(players) == 0 {
		return 0, true, true
	}
	n, ok = playerXProperty(g, amounts, sourceController, source, players[0], amt.Body, amt.Op, depth)
	return n, ok, true
}

// isAbilityPlayerHead reports the calculateAmount heads that read players off
// the resolving ability (AbilityUtils.java:574-636), each checked by
// startsWith in Java's order -- before the card lists, so TriggeredPlayer is
// never read as a Triggered<Key> card list.
func isAbilityPlayerHead(head string) bool {
	for _, p := range [...]string{"TargetedPlayer", "ThisTargetedPlayer", "TargetedController", "TriggeredPlayer",
		"TriggeredTarget", "TriggeredDefendingPlayer", "TriggeredActivator"} {
		if strings.HasPrefix(head, p) {
			return true
		}
	}
	return head == "TriggeredCardController"
}

// triggeredPlayerKey is the Defined$ spelling of a single-object Triggered
// head: its own name trimmed to the AbilityKey Java reads.
func triggeredPlayerKey(head string) string {
	for _, k := range [...]string{"TriggeredDefendingPlayer", "TriggeredActivator", "TriggeredPlayer", "TriggeredTarget"} {
		if strings.HasPrefix(head, k) {
			return k
		}
	}
	return head
}

// targetingPlayers is SpellAbility.getSATargetingPlayer's target players
// (SpellAbility.java:2020-2032): the ability's own player targets, else those
// of the nearest ancestor targeting a player. No targeting ancestor at all is
// Java's null ability, no players. The engine sees only the nearest ancestor
// that chose targets; when that one targets no player a further one might,
// so that case is unresolved (GO-7).
func targetingPlayers(refs abilityRefs) ([]PlayerID, bool) {
	if own := playersIn(refs.targets); len(own) > 0 {
		return own, true
	}
	if len(refs.parentTargets) == 0 {
		return nil, !refs.ancestorTargeting
	}
	parent := playersIn(refs.parentTargets)
	return parent, len(parent) > 0
}

func playersIn(entities []EntityID) []PlayerID {
	var out []PlayerID
	for _, e := range entities {
		if pid, ok := e.AsPlayer(); ok && !slices.Contains(out, pid) {
			out = append(out, pid)
		}
	}
	return out
}

// playerCountPlayers is calculateAmount's PlayerCount<hType>$ player set
// (AbilityUtils.java:456-500). Players and Opponents are the players still in
// the game (Game.getPlayers); RegisteredOpponents counts those who have left
// too; YourTeam is the player alone, this port seating no teams. An hType Java
// answers 0 for is unresolved here, never a silent 0 (GO-7). amounts and depth
// resolve a Property<p> operand naming an SVar (playerPropertyOperand).
func (g *Game) playerCountPlayers(amounts map[string]expr.Amount, controller PlayerID, source CardID, hType string, depth int) ([]PlayerID, bool) {
	inGame := func(keep func(PlayerID) bool) []PlayerID {
		var out []PlayerID
		for _, pid := range g.Players() {
			if !g.Player(pid).Lost && keep(pid) {
				out = append(out, pid)
			}
		}
		return out
	}
	switch {
	case hType == "" || hType == "Players":
		return inGame(func(PlayerID) bool { return true }), true
	case hType == "YourTeam" || strings.HasPrefix(hType, "PropertyYou"):
		if controller == NoPlayer {
			return nil, false
		}
		return []PlayerID{controller}, true
	case hType == "Opponents" || hType == "Other":
		return inGame(func(pid PlayerID) bool { return pid != controller }), true
	case hType == "RegisteredOpponents":
		var out []PlayerID
		for _, pid := range g.Players() {
			if pid != controller {
				out = append(out, pid)
			}
		}
		return out, true
	case hType == "NonActive":
		return inGame(func(pid PlayerID) bool { return pid != g.ActivePlayer() }), true
	case hType == "HasLost":
		var out []PlayerID
		for _, pid := range g.Players() {
			if g.Player(pid).Lost {
				out = append(out, pid)
			}
		}
		return out, true
	case strings.HasPrefix(hType, "Remembered"):
		if source == NoCard {
			return nil, false
		}
		switch hType {
		case "Remembered", "RememberedController", "RememberedOwner":
		default:
			return nil, false
		}
		var out []PlayerID
		for _, pid := range rememberedPlayers(g, hType, g.Card(source).Memory.Remembered(), true) {
			if !slices.Contains(out, pid) {
				out = append(out, pid)
			}
		}
		return out, true
	case strings.HasPrefix(hType, "Property"):
		prop := strings.TrimPrefix(hType, "Property")
		var out []PlayerID
		for _, pid := range g.playersInTurnOrder() {
			matched, known := matchesPlayerPropertyIn(g, pid, controller, source, prop, amounts, depth)
			if !known {
				return nil, false
			}
			if matched {
				out = append(out, pid)
			}
		}
		return out, true
	case strings.HasPrefix(hType, "Defined"):
		var refs abilityRefs
		if a := g.resolving; a != nil && source != NoCard && a.Source == source {
			refs = a.refs()
		}
		players, err := definedPlayers(g, controller, source, strings.TrimPrefix(hType, "Defined"), refs)
		return players, err == nil
	}
	return nil, false
}

// playerXCount is AbilityUtils.playerXCount over players, body the measure
// and op its operator. No players is 0, the operator not applied. Highest/
// Lowest under an operator applies it per player and again to the result
// (Java's own double application); no corpus line exercises that, so it is
// refused rather than reproduced blind.
func playerXCount(g *Game, amounts map[string]expr.Amount, controller PlayerID, source CardID, players []PlayerID, body string, op *expr.Op, depth int) (int, bool) {
	if len(players) == 0 {
		return 0, true
	}
	math := func(n int) (int, bool) {
		if op == nil {
			return n, true
		}
		return applyOperator(g, amounts, controller, source, n, op, depth)
	}
	switch {
	case strings.HasPrefix(body, "TotalCommanderCastFromCommandZone"):
		// No commander is ever cast from a Command zone here (no Commander
		// game setup), which is not the same as having counted none.
		return 0, false
	case strings.HasPrefix(body, "Highest"), strings.HasPrefix(body, "Lowest"):
		if op != nil {
			return 0, false
		}
		highest := strings.HasPrefix(body, "Highest")
		prop := strings.Replace(body, "Lowest", "", 1)
		n := 99999
		if highest {
			prop, n = strings.Replace(body, "Highest", "", 1), 0
		}
		for _, pid := range players {
			v, ok := playerXProperty(g, amounts, controller, source, pid, prop, nil, depth)
			if !ok {
				return 0, false
			}
			if highest && v > n || !highest && v < n {
				n = v
			}
		}
		return n, true
	case strings.HasPrefix(body, "TiedForHighestLife"), strings.HasPrefix(body, "TiedForLowestLife"):
		highest := strings.HasPrefix(body, "TiedForHighestLife")
		best := g.Player(players[0]).Life
		for _, pid := range players[1:] {
			if l := g.Player(pid).Life; highest && l > best || !highest && l < best {
				best = l
			}
		}
		n := 0
		for _, pid := range players {
			if g.Player(pid).Life == best {
				n++
			}
		}
		return math(n)
	case body == "Amount":
		return math(len(players))
	case strings.HasPrefix(body, "HasProperty"):
		prop := strings.TrimPrefix(body, "HasProperty")
		n := 0
		for _, pid := range players {
			matched, known := matchesPlayerPropertyIn(g, pid, controller, source, prop, amounts, depth)
			if !known {
				return 0, false
			}
			if matched {
				n++
			}
		}
		return math(n)
	case strings.HasPrefix(body, "Condition"):
		return conditionPlayerCount(g, amounts, controller, source, players, body, op, depth)
	case strings.Contains(body, "DamageThisTurn"):
		n := 0
		for _, pid := range players {
			n += g.Player(pid).DamageReceivedThisTurn
		}
		return math(n)
	}
	total := 0
	for _, pid := range players {
		v, ok := playerXProperty(g, amounts, controller, source, pid, body, op, depth)
		if !ok {
			return 0, false
		}
		total += v
	}
	return total, true
}

// conditionPlayerCount is playerXCount's Condition branch
// (AbilityUtils.java:3447-3468): how many players' property compares true
// against an amount, `Condition<op><amount> <property>` (bare Condition is
// GE1). An amount written per player (RelativePlayerUID) is not resolved.
func conditionPlayerCount(g *Game, amounts map[string]expr.Amount, controller PlayerID, source CardID, players []PlayerID, body string, op *expr.Op, depth int) (int, bool) {
	head, prop, ok := strings.Cut(body, " ")
	if !ok {
		return 0, false
	}
	comparator, calc := "GE", "1"
	if head != "Condition" {
		if len(head) < 11 {
			return 0, false
		}
		comparator, calc = head[9:11], head[11:]
	}
	if amt, named := amounts[strings.ToLower(calc)]; named && strings.Contains(amt.Text, "RelativePlayerUID") {
		return 0, false
	}
	y, ok := branchAmount(g, amounts, controller, source, calc, depth)
	if !ok {
		return 0, false
	}
	n := 0
	for _, pid := range players {
		x, ok := playerXProperty(g, amounts, controller, source, pid, prop, nil, depth)
		if !ok {
			return 0, false
		}
		if javaCompare(x, comparator, y) {
			n++
		}
	}
	if op == nil {
		return n, true
	}
	return applyOperator(g, amounts, controller, source, n, op, depth)
}

// playerPropertyNames are playerXProperty's `value.contains` checks
// (AbilityUtils.java:3542-3663), in Java's order: a property an earlier name is
// contained in is caught by the earlier one, as Java catches it.
var playerPropertyNames = [...]string{
	"NumPowerSurgeLands", "DomainPlayer", "CardsInHand", "CardsInLibrary", "CardsInGraveyard", "LandsInGraveyard",
	"CardsInPlay", "CreaturesInPlay", "StartingLife", "StartingLibrarySize", "LifeTotal", "LifeLostThisTurn",
	"LifeLostLastTurn", "LifeGainedThisTurn", "LifeGainedByTeamThisTurn", "LifeStartedThisTurnWith", "Speed",
	"SVarAmount", "Counters", "TopOfLibraryCMC", "LandsPlayed", "SpellsCastThisTurn", "CardsDrawn",
	"CardsDiscardedThisTurn", "ExploredThisTurn", "AttackersDeclared", "DamageToOppsThisTurn",
	"NonCombatDamageDealtThisTurn",
}

// playerPropertyMeasure is the measure of the first playerPropertyNames entry
// value contains, found false when none does. ok is false for a property this
// port keeps no state for: StartingLibrarySize,
// LifeLostLastTurn, LifeGainedByTeamThisTurn, LifeStartedThisTurnWith,
// SVarAmount, ExploredThisTurn, DamageToOppsThisTurn.
func playerPropertyMeasure(g *Game, pid PlayerID, value string, sq []string) (n int, ok, found bool) {
	name := ""
	for _, candidate := range playerPropertyNames {
		if strings.Contains(value, candidate) {
			name = candidate
			break
		}
	}
	p := g.Player(pid)
	switch name {
	case "":
		return 0, false, false
	case "NumPowerSurgeLands":
		return p.numPowerSurgeLands, true, true
	case "DomainPlayer":
		return domainCount(g, pid), true, true
	case "CardsInHand":
		return len(g.Zone(Hand, pid).Cards()), true, true
	case "CardsInLibrary":
		return len(g.Zone(Library, pid).Cards()), true, true
	case "CardsInGraveyard":
		return len(g.Zone(Graveyard, pid).Cards()), true, true
	case "LandsInGraveyard":
		return len(filterValid(g, g.Zone(Graveyard, pid).Cards(), "Land", pid, NoCard)), true, true
	case "CardsInPlay":
		return len(g.Zone(Battlefield, pid).Cards()), true, true
	case "CreaturesInPlay":
		return len(filterValid(g, g.Zone(Battlefield, pid).Cards(), "Creature", pid, NoCard)), true, true
	case "StartingLife":
		return startingLife, true, true
	case "LifeTotal":
		return p.Life, true, true
	case "LifeLostThisTurn":
		return p.LifeLostThisTurn, true, true
	case "LifeGainedThisTurn":
		return p.LifeGainedThisTurn, true, true
	case "Speed":
		return p.Speed, true, true
	case "Counters":
		if len(sq) < 2 {
			return 0, false, true
		}
		if sq[1] == "ALL" {
			return p.Counters.Total(), true, true
		}
		return p.Counters.Count(CounterType(strings.ToUpper(sq[1]))), true, true
	case "TopOfLibraryCMC":
		if lib := g.Zone(Library, pid).Cards(); len(lib) > 0 {
			return g.Card(lib[0]).CMC(), true, true
		}
		return 0, true, true
	case "LandsPlayed":
		return p.LandsPlayed, true, true
	case "SpellsCastThisTurn":
		return p.SpellsCastThisTurn, true, true
	case "CardsDrawn":
		return p.CardsDrawnThisTurn, true, true
	case "CardsDiscardedThisTurn":
		return len(p.discardedThisTurn), true, true
	case "AttackersDeclared":
		return len(p.attackedThisTurn), true, true
	case "NonCombatDamageDealtThisTurn":
		// getAssignedDamage - getAssignedCombatDamage: the noncombat damage the
		// player was dealt this turn.
		return p.damageThisTurn[0], true, true
	}
	return 0, false, true
}

// playerXProperty is AbilityUtils.playerXProperty for pid, body the property
// and op its operator, applied to the property's value. A valid string's
// "You" is pid itself. A property Java would answer 0 for, or one this port
// keeps no state for, is unresolved (GO-7).
func playerXProperty(g *Game, amounts map[string]expr.Amount, controller PlayerID, source CardID, pid PlayerID, body string, op *expr.Op, depth int) (int, bool) {
	math := func(n int) (int, bool) {
		if op == nil {
			return n, true
		}
		return applyOperator(g, amounts, controller, source, n, op, depth)
	}
	if spec, ok := strings.CutPrefix(body, "Valid "); ok {
		return math(countValid(g, []ZoneType{Battlefield}, valid.Parse(spec), pid, source))
	}
	if strings.HasPrefix(body, "Valid") {
		zoneText, spec, ok := strings.Cut(body, " ")
		if !ok {
			return 0, false
		}
		zones, ok := validCountZones(zoneText)
		if !ok {
			return 0, false
		}
		return math(countValid(g, zones, valid.Parse(spec), pid, source))
	}
	if strings.HasPrefix(body, "ThisTurnEntered") {
		n, ok := g.thisTurnEnteredCount(pid, source, body)
		if !ok {
			return 0, false
		}
		return math(n)
	}
	if strings.HasPrefix(body, "Sacrificed") {
		n, ok := g.sacrificedThisTurnCount(pid, source, body)
		if !ok {
			return 0, false
		}
		return math(n)
	}
	sq := strings.Split(body, ".")
	value := sq[0]
	if n, ok, found := playerPropertyMeasure(g, pid, value, sq); found {
		if !ok {
			return 0, false
		}
		return math(n)
	}
	switch value {
	case "RingTemptedYou":
		return math(g.Player(pid).ringTempted)
	case "OpponentsAttackedThisTurn":
		// AbilityUtils.java:3665-3668: Player.getAttackedPlayersMyTurn.
		return math(len(g.Player(pid).attackedPlayersThisTurn))
	case "OpponentsAttackedThisCombat":
		return math(g.attackedOpponentsThisCombat(pid))
	case "AttractionsVisitedThisTurn":
		// AbilityUtils.java:3683: Player.getAttractionsVisitedThisTurn. The
		// engine never visits Attractions (RollDice's ToVisitYourAttractions
		// is refused), so no visit has been counted: always 0.
		return math(0)
	}
	return 0, false
}
