// Player properties that read the turn's records, combat and zone counts:
// the slice of PlayerProperty.playerHasProperty matchesPlayerProperty
// (valid.go) falls through to -- LostLifeThisTurn, the wasDealt...ThisTurn
// family, Defending, the attack-history properties, HasCardsIn<Zone>, the
// withMore/withAtLeast/hasMore/hasFewer/withMost/withLowest comparisons,
// TappedLandForManaThisTurn, CardsInHandAtBeginningOfTurn and life<op><SVar>
// -- plus the per-player records they and the playerXProperty measures
// (OpponentsAttackedThisTurn/ThisCombat, SacrificedThisTurn,
// NumPowerSurgeLands) read. Every record lives on the Game or its players,
// is copied by Game.Clone and ends at cleanup (GO-2).
//
// Ported from forge-game/src/main/java/forge/game/player/PlayerProperty.java
// (:66-70, :140-203, :311-439, :444-476) and AbilityUtils.playerXProperty
// (:3523-3544, :3665-3673).

package engine

import (
	"slices"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// recordPlayerDamage logs amount of damage source dealt target, after
// prevention and replacement (CardDamageHistory.registerDamage ignores
// damage of zero or less).
func (g *Game) recordPlayerDamage(source CardID, target PlayerID, amount int, combat bool) {
	if amount <= 0 {
		return
	}
	g.playerDamageThisTurn = append(g.playerDamageThisTurn,
		playerDamage{source: *g.Card(source), target: target, amount: amount, combat: combat})
}

// recordSacrificed is Player.addSacrificedThisTurn: a last-known copy of id,
// taken while it is still on the battlefield, added to pid's sacrifices of
// the turn.
func (g *Game) recordSacrificed(pid PlayerID, id CardID) {
	g.sacrificedThisTurn = append(g.sacrificedThisTurn, sacrificeRecord{player: pid, card: *g.Card(id)})
}

// noteTapsForMana is AbilityManaPart.tapsForMana's
// setTappedLandForManaThisTurn: a tap mana ability of a land, activated by
// pid. Callers call it only for an ability with a tap cost.
func (g *Game) noteTapsForMana(card CardID, pid PlayerID) {
	if g.Card(card).Type().Has(cardtype.Land) {
		g.Player(pid).tappedLandForManaThisTurn = true
	}
}

// notePowerSurgeLands is PhaseHandler.java:190-191: as pid's turn begins,
// before its untap step, the untapped lands pid controls.
func (g *Game) notePowerSurgeLands(pid PlayerID) {
	n := 0
	for _, id := range g.Zone(Battlefield, pid).Cards() {
		if c := g.Card(id); !c.Tapped && c.Type().Has(cardtype.Land) {
			n++
		}
	}
	g.Player(pid).numPowerSurgeLands = n
}

// cleanupPlayerRecords is the slice of Player.onCleanupPhase these records
// need (Player.java:2481-2489, :2514-2517): the hand size the next turn
// starts with, the player's sacrifices and land-for-mana flag cleared, and,
// for the player whose turn is ending, the attacked players rotated into last
// turn's. Called once per player after the cleanup discard, and by
// RestartGame.
func (g *Game) cleanupPlayerRecords(pid PlayerID) {
	p := g.Player(pid)
	p.cardsInHandStartedTurn = len(g.Zone(Hand, pid).Cards())
	var kept []sacrificeRecord
	for _, s := range g.sacrificedThisTurn {
		if s.player != pid {
			kept = append(kept, s)
		}
	}
	g.sacrificedThisTurn = kept
	p.tappedLandForManaThisTurn = false
	if pid == g.activePlayer {
		p.attackedPlayersLastTurn = p.attackedPlayersThisTurn
		p.attackedPlayersThisTurn = nil
	}
}

// attackedOpponentsThisCombat is Combat.getAttackedOpponents(pid).size()
// (Combat.java:199-209): for the attacking player (the active player,
// PhaseHandler.java:300's new Combat(playerTurn)), the players some attacker
// attacks directly -- a planeswalker or battle attacked is not its controller
// here. Zero for anyone else, and when nothing attacks.
func (g *Game) attackedOpponentsThisCombat(pid PlayerID) int {
	if pid != g.activePlayer {
		return 0
	}
	n := 0
	for _, other := range g.Players() {
		if g.attackedDirectly(other) {
			n++
		}
	}
	return n
}

// attackedDirectly is Combat.getAttackersAndDefenders().values().contains(pid)
// (PlayerProperty.java:185-188): some attacker's defender is pid itself.
func (g *Game) attackedDirectly(pid PlayerID) bool {
	for _, id := range g.combat.Attackers {
		if g.combat.AttackTargets[id] == PlayerEntity(pid) {
			return true
		}
	}
	return false
}

// sacrificedThisTurnCount is playerXProperty's SacrificedThisTurn[ <valid>]
// (AbilityUtils.java:3523-3532): pid's sacrifices of the turn, filtered by
// the valid string after the first space when there is one, matched with pid
// as the controller it is relative to. Every other Sacrificed... measure
// (SacrificedPermanentTypesThisTurn) is unresolved.
func (g *Game) sacrificedThisTurnCount(pid PlayerID, source CardID, body string) (int, bool) {
	head, spec, filtered := strings.Cut(body, " ")
	if head != "SacrificedThisTurn" {
		return 0, false
	}
	var parsed valid.Spec
	if filtered {
		parsed = valid.Parse(spec)
	}
	n := 0
	for i := range g.sacrificedThisTurn {
		s := &g.sacrificedThisTurn[i]
		if s.player == pid && (!filtered || Matches(g, &s.card, parsed, pid, source)) {
			n++
		}
	}
	return n, true
}

// playerPropertyOperand is calculateAmount(source, operand) for a property's
// comparison operand: an integer, or an SVar name. With amounts (a
// PlayerCount amount's own table) the name resolves there, depth counting the
// hop; without, it resolves against the source card's SVars (compareOperand).
func playerPropertyOperand(g *Game, amounts map[string]expr.Amount, host PlayerID, source CardID, operand string, depth int) (int, bool) {
	if amounts == nil {
		return compareOperand(g, operand, host, source)
	}
	if n, err := strconv.Atoi(operand); err == nil {
		return n, true
	}
	return namedAmount(g, amounts, host, source, operand, depth)
}

// playerProperty is matchesPlayerPropertyIn's (valid.go) fallthrough: the
// properties below, in PlayerProperty.playerHasProperty's own order where a
// prefix overlaps. ok is false for a property it does not know, and for a
// known one whose operand or shape does not resolve (GO-7).
func playerProperty(g *Game, candidate, host PlayerID, source CardID, property string, amounts map[string]expr.Amount, depth int) (matched, ok bool) {
	p := g.Player(candidate)
	switch property {
	case "OpponentToActive":
		// :66-70; no teams here, so every other player is an opponent.
		return candidate != g.ActivePlayer(), true
	case "Defending":
		return g.attackedDirectly(candidate), true
	case "TappedLandForManaThisTurn":
		return p.tappedLandForManaThisTurn, true
	case "CardsInHandAtBeginningOfTurn":
		return p.cardsInHandStartedTurn > 0, true
	case "castSpellThisTurn":
		return p.SpellsCastThisTurn > 0, true
	case "attackedWithCreaturesThisTurn":
		return len(p.attackedThisTurn) > 0, true
	case "attackedYouTheirCurrentTurn":
		return slices.Contains(p.attackedPlayersThisTurn, host), true
	case "attackedYouTheirLastTurn":
		return slices.Contains(p.attackedPlayersLastTurn, host), true
	case "BeenAttackedThisCombat":
		// :477-483: some registered player's attackedPlayersThisCombat names
		// candidate -- who was declared attacked, not who is still attacked.
		for _, pid := range g.Players() {
			if slices.Contains(g.Player(pid).attackedPlayersThisCombat, candidate) {
				return true, true
			}
		}
		return false, true
	case "withLowestLife":
		// :423-438: nobody in the game has less life; a tie includes all.
		for _, pid := range g.Players() {
			if !g.Player(pid).Lost && g.Player(pid).Life < p.Life {
				return false, true
			}
		}
		return true, true
	}
	switch {
	case strings.HasPrefix(property, "wasDealtCombatDamageThisCombatBy "),
		strings.HasPrefix(property, "wasDealtDamageThisGameBy "):
		// :112-139: per-combat and per-game damage records are not kept.
		return false, false
	case strings.HasPrefix(property, "wasDealt"):
		return wasDealtThisTurn(g, candidate, host, source, property, amounts, depth)
	case strings.HasPrefix(property, "LostLifeThisTurn"):
		return compareProperty(g, p.LifeLostThisTurn, property, "LostLifeThisTurn", amounts, host, source, depth)
	case strings.HasPrefix(property, "HasCardsIn"):
		return hasCardsIn(g, candidate, host, source, property[len("HasCardsIn"):], amounts, depth)
	case len(property) > 6 && strings.HasPrefix(property, "life"):
		// :266-272: the operand after "life<op>" is an SVar here (a literal is
		// matchesPlayerPropertyIn's own case); Expressions.compare reads the
		// operator out of the whole property.
		n, known := playerPropertyOperand(g, amounts, host, source, property[6:], depth)
		if !known {
			return false, false
		}
		return javaCompare(p.Life, property, n), true
	}
	return comparePlayers(g, candidate, host, property)
}

// compareProperty is the "<name>[ <op><amount>]" shape (LostLifeThisTurn,
// PlayerProperty.java:189-202): value against the operand after the space,
// at least 1 when there is none.
func compareProperty(g *Game, value int, property, name string, amounts map[string]expr.Amount, host PlayerID, source CardID, depth int) (matched, ok bool) {
	if property == name {
		return value >= 1, true
	}
	rest, spaced := strings.CutPrefix(property, name+" ")
	if !spaced || len(rest) < 3 {
		return false, false
	}
	n, known := playerPropertyOperand(g, amounts, host, source, rest[2:], depth)
	if !known {
		return false, false
	}
	return javaCompare(value, rest[:2], n), true
}

// wasDealtThisTurn is PlayerProperty.java:140-172 for the ThisTurn forms
// ("LastTurn" is matchesPlayerPropertyIn's own case): damage dealt to the
// candidate this turn -- all, combat ("CombatDamage") or noncombat
// ("NonCombat") -- compared with "<op><amount>" when the property is a
// "ThisTurnBy" form that writes one, else at least 1.
//
//   - wasDealt...ThisTurn: whether any such damage was dealt.
//   - wasDealt...ThisTurnBy <valid> [<op><n>]: how many distinct sources
//     matching valid, as each was when it dealt the damage, dealt such damage
//     (Game.getDamageDoneThisTurn's list size, one entry per source's history).
//     The valid string is matched with the candidate as its controller, as
//     Java passes the player there.
//   - wasDealt...ThisTurnBySource [<op><n>]: the amount of such damage the
//     source card dealt the candidate, or with "SourceTimes" how many times.
func wasDealtThisTurn(g *Game, candidate, host PlayerID, source CardID, property string, amounts map[string]expr.Amount, depth int) (matched, ok bool) {
	if strings.Contains(property, "LastTurn") {
		// A LastTurn form matchesPlayerPropertyIn does not read (a suffix after
		// LastTurn): Java ignores it, which is not reproduced blind.
		return false, false
	}
	if !strings.Contains(property, "ThisTurn") {
		return false, false
	}
	kind := func(playerDamage) bool { return true }
	switch {
	case strings.Contains(property, "NonCombat"):
		kind = func(d playerDamage) bool { return !d.combat }
	case strings.Contains(property, "CombatDamage"):
		kind = func(d playerDamage) bool { return d.combat }
	}
	comp, right := "GE", 1
	validCard := ""
	bySource := strings.Contains(property, "BySource")
	if strings.Contains(property, "ThisTurnBy") {
		props := strings.Split(property, " ")
		idx := 2
		if bySource {
			idx = 1
		} else {
			if len(props) < 2 {
				return false, false
			}
			validCard = props[1]
		}
		if len(props) > idx {
			if len(props[idx]) < 3 {
				return false, false
			}
			n, known := playerPropertyOperand(g, amounts, host, source, props[idx][2:], depth)
			if !known {
				return false, false
			}
			comp, right = props[idx][:2], n
		}
	}
	result := 0
	switch {
	case bySource:
		if source == NoCard {
			return false, false
		}
		times := strings.Contains(property, "SourceTimes")
		for _, d := range g.playerDamageThisTurn {
			if d.target == candidate && d.source.ID == source && kind(d) {
				if times {
					result++
				} else {
					result += d.amount
				}
			}
		}
	case validCard == "":
		for _, d := range g.playerDamageThisTurn {
			if d.target == candidate && kind(d) {
				result = 1
				break
			}
		}
	default:
		spec := valid.Parse(validCard)
		var sources []CardID
		for i := range g.playerDamageThisTurn {
			d := &g.playerDamageThisTurn[i]
			if d.target != candidate || !kind(*d) || slices.Contains(sources, d.source.ID) {
				continue
			}
			if Matches(g, &d.source, spec, candidate, source) {
				sources = append(sources, d.source.ID)
			}
		}
		result = len(sources)
	}
	return javaCompare(result, comp, right), true
}

// hasCardsIn is PlayerProperty.java:311-318's HasCardsIn<Zone>_<valid>_<op><n>:
// how many of the candidate's cards in the zone match valid, from host's
// point of view, against the operand. Java's split("_") with no third part
// throws; here it is unresolved.
func hasCardsIn(g *Game, candidate, host PlayerID, source CardID, rest string, amounts map[string]expr.Amount, depth int) (matched, ok bool) {
	parts := strings.Split(rest, "_")
	if len(parts) < 3 || len(parts[2]) < 3 {
		return false, false
	}
	zone, known := zoneByLooseName(parts[0])
	if !known {
		return false, false
	}
	n, known := playerPropertyOperand(g, amounts, host, source, parts[2][2:], depth)
	if !known {
		return false, false
	}
	spec := valid.Parse(parts[1])
	count := 0
	for _, id := range g.Zone(zone, candidate).Cards() {
		if Matches(g, g.Card(id), spec, host, source) {
			count++
		}
	}
	return javaCompare(count, parts[2], n), true
}

// typeCount is CardLists.getType(player.getCardsIn(zone), cardType).size():
// pid's cards in the zone whose type line has the type (CardType.hasStringType).
func typeCount(g *Game, pid PlayerID, zone ZoneType, cardType string) int {
	n := 0
	for _, id := range g.Zone(zone, pid).Cards() {
		if g.Card(id).Type().HasStringType(cardType) {
			n++
		}
	}
	return n
}

// comparePlayers is PlayerProperty.java:319-439: the candidate against the
// player the property names ("...ThanActive" the active player, anything else
// host) or against every player in the game. A shape Java would throw on, or
// answer true for without comparing anything (an unknown withMost/withLowest
// kind), is unresolved.
func comparePlayers(g *Game, candidate, host PlayerID, property string) (matched, ok bool) {
	versus := func(word string) PlayerID {
		if word == "Active" {
			return g.ActivePlayer()
		}
		return host
	}
	switch {
	case strings.HasPrefix(property, "withMore"):
		// "withMore<Type>sThan<You|Active>": strictly more of the type on the
		// battlefield.
		head, who, found := strings.Cut(property, "sThan")
		if !found {
			return false, false
		}
		cardType := head[len("withMore"):]
		return typeCount(g, candidate, Battlefield, cardType) > typeCount(g, versus(who), Battlefield, cardType), true
	case strings.HasPrefix(property, "withAtLeast"):
		// "withAtLeast<d>More<Type>sThan<You|Active>": at least d more, d one
		// digit (Java's substring(11, 12)).
		_, after, found := strings.Cut(property, "More")
		head, who, thanFound := strings.Cut(after, "sThan")
		if !found || !thanFound || len(property) < 12 {
			return false, false
		}
		d, err := strconv.Atoi(property[11:12])
		if err != nil {
			return false, false
		}
		return typeCount(g, candidate, Battlefield, head) >= typeCount(g, versus(who), Battlefield, head)+d, true
	case strings.HasPrefix(property, "hasMore"):
		// "hasMore<Life|CardsInHand>[Than<Active>]": strictly more.
		_, who, _ := strings.Cut(property, "Than")
		other := versus(who)
		switch rest := property[len("hasMore"):]; {
		case strings.HasPrefix(rest, "Life"):
			return g.Player(candidate).Life > g.Player(other).Life, true
		case strings.HasPrefix(rest, "CardsInHand"):
			return len(g.Zone(Hand, candidate).Cards()) > len(g.Zone(Hand, other).Cards()), true
		}
		return false, false
	case strings.HasPrefix(property, "hasFewer"):
		// "hasFewer<Type>sIn<Play|Yard>Than<You|Active>": strictly fewer; only
		// "CreaturesInYard" reads the graveyard, as in Java.
		head, _, found := strings.Cut(property, "sIn")
		_, who, thanFound := strings.Cut(property, "Than")
		if !found || !thanFound {
			return false, false
		}
		zone := Battlefield
		if strings.HasPrefix(property[len("hasFewer"):], "CreaturesInYard") {
			zone = Graveyard
		}
		cardType := head[len("hasFewer"):]
		return typeCount(g, candidate, zone, cardType) < typeCount(g, versus(who), zone, cardType), true
	case strings.HasPrefix(property, "withMost"):
		return withMost(g, candidate, property[len("withMost"):])
	}
	return false, false
}

// withMost is PlayerProperty.java:353-422's withMost<kind> past Life
// (matchesPlayerPropertyIn's own case): PermanentInPlay, the only player with
// the most permanents; CardsInHand, the first player in seat order holding
// strictly more cards than every one before it (so a tie goes to the earlier
// seat, as Java's strict ">" scan gives it); Type<T>[Only], every player tied
// for the most permanents of type T, or with Only the one player who has
// strictly the most.
func withMost(g *Game, candidate PlayerID, kind string) (matched, ok bool) {
	players := g.inGamePlayers()
	switch {
	case kind == "PermanentInPlay":
		most := mostPlayers(players, func(pid PlayerID) int { return len(g.Zone(Battlefield, pid).Cards()) })
		return len(most) == 1 && most[0] == candidate, true
	case kind == "CardsInHand":
		largest, with := 0, NoPlayer
		for _, pid := range players {
			if n := len(g.Zone(Hand, pid).Cards()); n > largest {
				largest, with = n, pid
			}
		}
		return candidate == with, true
	case strings.HasPrefix(kind, "Type"):
		cardType, only := strings.CutSuffix(kind[len("Type"):], "Only")
		most := mostPlayers(players, func(pid PlayerID) int { return typeCount(g, pid, Battlefield, cardType) })
		if only && len(most) != 1 {
			return false, true
		}
		return slices.Contains(most, candidate), true
	}
	return false, false
}

// mostPlayers is withMost's tally: the players whose count is the largest.
// Java starts from 0 and adds every player equal to the running maximum, so
// with every count 0 every player is "with the most".
func mostPlayers(players []PlayerID, count func(PlayerID) int) []PlayerID {
	best := 0
	var most []PlayerID
	for _, pid := range players {
		n := count(pid)
		if n > best {
			best, most = n, nil
		}
		if n == best {
			most = append(most, pid)
		}
	}
	return most
}

// inGamePlayers is Game.getPlayers: the players still in the game, in seat
// order.
func (g *Game) inGamePlayers() []PlayerID {
	var out []PlayerID
	for _, pid := range g.Players() {
		if !g.Player(pid).Lost {
			out = append(out, pid)
		}
	}
	return out
}
