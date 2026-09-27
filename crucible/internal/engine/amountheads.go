// Amount heads: the Count$ and PlayerCount$ measurements resolveAmount
// (amount.go) evaluates -- each a direct read of state this port already
// models (a player's life, a zone's cards, a permanent's counters, a card's
// printed mana cost), dispatched on the exact head name xCount itself would
// reach for it. Every head here was checked against xCount's own if-chain
// for an earlier `contains`/`startsWith` branch that would catch it first;
// none does.
//
// Ported from forge-game/src/main/java/forge/game/ability/AbilityUtils.java's
// xCount (:1566), playerXCount (:3288) and playerXProperty (:3420), and
// forge-game/src/main/java/forge/game/staticability/StaticAbilityDevotion.java.

package engine

import (
	"math"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// countValue is xCount for a Count$ body already read at load (expr.Count):
// the Valid family (validFamilyValue, amountpaid.go), then the exact heads
// below. Any other head -- Party, YourTurns, xPaid and the rest of xCount's
// own two hundred-odd branches -- reports false (GO-7).
//
//   - YourLifeTotal: the controller's life (Player.getLife);
//     OppGreatestLifeTotal, the highest among its opponents.
//   - YouDrewThisTurn: cards the controller drew this turn
//     (Player.getNumDrawnThisTurn; Player.CardsDrawnThisTurn here).
//   - NumInAllHands: every card in every hand -- no earlier xCount branch
//     matches it, so it reaches getCardListForXCount, whose only matching
//     qualifier is "InAllHands" (game.getCardsIn(Hand)).
//   - Domain/DomainActivePlayer: basic land types among the controller's (or
//     the active player's) lands (domainCount).
//   - Devotion.<Color>/DevotionDual.<Color>.<Color>: devotionCount.
//   - Chroma[.<Color>]/ChromaInGrave[.<Color>]/ChromaSource[.<Color>]:
//     chromaCount.
//   - CardCounters.<TYPE>|ALL: counters on source itself (c.getCounters /
//     getNumAllCounters) -- the host, since calculateAmount hands xCount the
//     card the amount is read for.
//   - ChosenNumber: source's own chosen number, 0 when none was chosen
//     (`i == null ? 0 : i`).
func countValue(g *Game, sourceController PlayerID, source CardID, count expr.Count) (int, bool) {
	if expr.IsValidHead(count.Head) {
		return validFamilyValue(g, sourceController, source, count)
	}
	switch count.Head {
	case "CardCounters":
		if source == NoCard || len(count.Parameters) == 0 {
			return 0, false
		}
		c := g.Card(source)
		if count.Parameters[0] == "ALL" {
			return c.Counters.Total(), true
		}
		return c.Counters.Count(CounterType(strings.ToUpper(count.Parameters[0]))), true
	case "ChosenNumber":
		if source == NoCard {
			return 0, false
		}
		n, _ := g.Card(source).Memory.ChosenNumber()
		return n, true
	case "ChromaSource":
		if source == NoCard {
			return 0, false
		}
		return chromaCount([]CardID{source}, g, count.Parameters)
	}
	if sourceController == NoPlayer {
		return 0, false
	}
	switch count.Head {
	case "YourLifeTotal":
		return g.Player(sourceController).Life, true
	case "OppGreatestLifeTotal":
		// Player.getOpponentsGreatestLifeTotal: Aggregates.max seeds with
		// Integer.MIN_VALUE, which is what no opponent at all reads as.
		n := math.MinInt32
		for _, pid := range g.Players() {
			if pid != sourceController && !g.Player(pid).Lost {
				n = max(n, g.Player(pid).Life)
			}
		}
		return n, true
	case "YouDrewThisTurn":
		return g.Player(sourceController).CardsDrawnThisTurn, true
	case "NumInAllHands":
		n := 0
		for _, pid := range g.Players() {
			n += len(g.Zone(Hand, pid).Cards())
		}
		return n, true
	case "Domain":
		return domainCount(g, sourceController), true
	case "DomainActivePlayer":
		return domainCount(g, g.ActivePlayer()), true
	case "Devotion", "DevotionDual":
		return devotionCount(g, sourceController, count)
	case "Chroma":
		return chromaCount(battlefieldControlledBy(g, sourceController), g, count.Parameters)
	case "ChromaInGrave":
		return chromaCount(g.Zone(Graveyard, sourceController).Cards(), g, count.Parameters)
	}
	return 0, false
}

// battlefieldControlledBy is Player.getCardsIn(Battlefield) filtered to what
// pid actually controls: g.Zone(Battlefield, pid) is keyed by owner, not
// controller (zone.go), so a stolen permanent sits in its owner's zone while
// GainControl$/ExchangeControl$ move who controls it. Every xCount head that
// reads "permanents pid controls" -- Domain, Devotion, Chroma among them --
// has to walk every player's own battlefield and filter on Controller,
// never trust the zone's owner key alone.
func battlefieldControlledBy(g *Game, pid PlayerID) []CardID {
	var out []CardID
	for _, owner := range g.Players() {
		for _, id := range g.Zone(Battlefield, owner).Cards() {
			if g.Card(id).Controller() == pid {
				out = append(out, id)
			}
		}
	}
	return out
}

// domainCount is xCount's own Count$Domain: how many of the five basic land
// types appear among pid's lands (Player.getLandsInPlay, filtered by
// CardLists.getType's own hasStringType per type). Reads each land's current
// type line, Layer 4 folded in (Card.Type).
func domainCount(g *Game, pid PlayerID) int {
	// MagicColor.Constant.BASIC_LANDS, in its own order.
	basicLandTypes := [...]string{"Plains", "Island", "Swamp", "Mountain", "Forest"}
	var seen [len(basicLandTypes)]bool
	for _, id := range battlefieldControlledBy(g, pid) {
		t := g.Card(id).Type()
		if !t.Has(cardtype.Land) {
			continue
		}
		for i, basic := range basicLandTypes {
			if t.HasStringType(basic) {
				seen[i] = true
			}
		}
	}
	n := 0
	for _, s := range seen {
		if s {
			n++
		}
	}
	return n
}

// devotionCount is xCount's own Count$Devotion/DevotionDual: every mana
// symbol of the named color(s) in the mana costs of permanents pid controls
// (ManaCostShard.isColor -- a symbol counts once if any of its colors is in
// the mask, so a hybrid symbol counts once even toward a dual devotion),
// plus Player.getDevotionMod (devotionMod). A color written as "Chosen..."
// (the host's own chosen color) or one ManaAtom.fromName would not read as a
// color at all reports false.
func devotionCount(g *Game, pid PlayerID, count expr.Count) (int, bool) {
	want := 2
	if count.Head == "Devotion" {
		want = 1
	}
	if len(count.Parameters) < want {
		return 0, false
	}
	var mask mana.Colors
	for _, name := range count.Parameters[:want] {
		c, ok := manaAtomColor(name)
		if !ok {
			return 0, false
		}
		mask |= c
	}
	mod, ok := devotionMod(g, pid)
	if !ok {
		return 0, false
	}
	return shardsOfColor(g, battlefieldControlledBy(g, pid), mask) + mod, true
}

// chromaCount is xCount's own Count$Chroma (CardLists.getTotalChroma): the
// mana symbols of the named color -- all five when none is named, ManaAtom's
// own ALL_MANA_COLORS -- in the mana costs of cards.
func chromaCount(cards []CardID, g *Game, params []string) (int, bool) {
	mask := mana.AllColors
	if len(params) > 0 {
		c, ok := manaAtomColor(params[0])
		if !ok {
			return 0, false
		}
		mask = c
	}
	return shardsOfColor(g, cards, mask), true
}

// shardsOfColor counts the symbols among cards' own printed mana costs
// whose colors intersect mask -- Card.getManaCost's own shard iteration,
// generic mana excluded (it is no shard).
func shardsOfColor(g *Game, cards []CardID, mask mana.Colors) int {
	n := 0
	for _, id := range cards {
		c := g.Card(id)
		if c.Def == nil {
			continue
		}
		for _, s := range c.Def.Faces[0].ManaCost.Shards() {
			if s.Colors().HasAny(mask) {
				n++
			}
		}
	}
	return n
}

// manaAtomColor is ManaAtom.fromName for the five colors: one or two mana
// letters, or a full color name in any case. Colorless (ManaAtom.COLORLESS,
// a mana type with no mana.Colors bit) and anything unrecognized (fromName's
// own "generic" 0) report false.
func manaAtomColor(name string) (mana.Colors, bool) {
	if len(name) == 1 || len(name) == 2 {
		var out mana.Colors
		for i := 0; i < len(name); i++ {
			c, ok := mana.ColorFromLetter(strings.ToUpper(name[i : i+1])[0])
			if !ok {
				return 0, false
			}
			out |= c
		}
		return out, true
	}
	switch strings.ToLower(name) {
	case "white":
		return mana.White, true
	case "blue":
		return mana.Blue, true
	case "black":
		return mana.Black, true
	case "red":
		return mana.Red, true
	case "green":
		return mana.Green, true
	}
	return 0, false
}

// devotionMod is StaticAbilityDevotion.getDevotionMod: the sum of Value$
// (default 1) over every active Mode$ Devotion static ability whose own
// ValidPlayer$ matches pid -- Altar of the Pantheon's "your devotion to each
// color ... is increased by one," the one real corpus line. A line carrying
// any param past Mode$/ValidPlayer$/Value$/Description$ (checkConditions'
// own Condition$ family), a non-integer Value$, or a ValidPlayer$ that
// matchesPlayerSpec cannot evaluate makes the whole count unresolvable
// rather than silently dropping its contribution (GO-7).
func devotionMod(g *Game, pid PlayerID) (int, bool) {
	mod := 0
	for _, owner := range g.Players() {
		for _, host := range g.traitHosts(owner) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.Def.Faces {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, "Devotion") {
						continue
					}
					for _, p := range s.Params {
						switch strings.ToLower(p.Key) {
						case "mode", "validplayer", "value", "description":
						default:
							return 0, false
						}
					}
					if spec, ok := s.Param("ValidPlayer"); ok {
						matched, ok := matchesPlayerSpec(g, pid, h.Controller(), host, spec)
						if !ok {
							return 0, false
						}
						if !matched {
							continue
						}
					}
					v := 1
					if raw, ok := s.Param("Value"); ok {
						n, err := strconv.Atoi(raw)
						if err != nil {
							return 0, false
						}
						v = n
					}
					mod += v
				}
			}
		}
	}
	return mod, true
}

// playerCountValue is calculateAmount's own PlayerCount<hType>$ dispatch
// into playerXCount, for the one shape a real CDA writes (Adamaro, First to
// Desire's `PlayerCountOpponents$HighestCardsInHand`) and its immediate
// siblings: hType Opponents or Players (the empty hType is Players too),
// body Highest<Property> or Lowest<Property>, Property CardsInHand or
// LifeTotal (playerXProperty's own `value.contains` checks, reached by these
// two names before any other branch). Highest starts from 0 and Lowest from
// 99999, Java's own seeds; no players at all is 0. Players who have left the
// game are not counted (Game.getPlayers holds only players still in it).
func playerCountValue(g *Game, sourceController PlayerID, hType, body string) (int, bool) {
	var players []PlayerID
	for _, pid := range g.Players() {
		if g.Player(pid).Lost {
			continue
		}
		switch hType {
		case "", "Players":
		case "Opponents":
			if pid == sourceController {
				continue
			}
		default:
			return 0, false
		}
		players = append(players, pid)
	}
	var highest bool
	var property string
	switch {
	case strings.HasPrefix(body, "Highest"):
		highest, property = true, strings.TrimPrefix(body, "Highest")
	case strings.HasPrefix(body, "Lowest"):
		property = strings.TrimPrefix(body, "Lowest")
	default:
		return 0, false
	}
	var value func(PlayerID) int
	switch property {
	case "CardsInHand":
		value = func(pid PlayerID) int { return len(g.Zone(Hand, pid).Cards()) }
	case "LifeTotal":
		value = func(pid PlayerID) int { return g.Player(pid).Life }
	default:
		return 0, false
	}
	if len(players) == 0 {
		return 0, true
	}
	n := 99999
	if highest {
		n = 0
	}
	for _, pid := range players {
		v := value(pid)
		if highest && v > n || !highest && v < n {
			n = v
		}
	}
	return n, true
}
