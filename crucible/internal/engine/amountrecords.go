// Amount heads over the turn and game records: what left a graveyard this
// turn, how often a player scried, drew last turn or cast this game, how many
// turns and end steps have passed, the mana a cast spent by color, and the two
// "most of one kind" counts over the battlefield. Each reads a record Java
// keeps on Game, PhaseHandler or Player, kept here on the same owner (game.go,
// player.go) and copied by Game.Clone.
//
// Ported from forge-game/src/main/java/forge/game/ability/AbilityUtils.java's
// xCount (:2147-2161 EachSpentToCast, :2219-2221 FinishedEndOfTurnsThisTurn,
// :2312-2314 YouDrewLastTurn, :2328-2330 YouScryThisTurn, :2340-2342
// YouCastThisGame, :2377-2379 LifeYourTeamGainedThisTurn, :2543-2547
// LeftGraveyardThisTurn, :2754-2756 TotalTurns, :2846-2863 MostCardName and
// MostProminentCreatureType) and CardFactoryUtil.getMostProminentCreatureTypeSize
// (CardFactoryUtil.java:318-346).

package engine

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// recordCountValue is xCount for the record heads above. handled is false for
// every other head, which countValue then dispatches; ok is false for a head
// whose record or argument this port cannot read (GO-7).
func (g *Game) recordCountValue(sourceController PlayerID, source CardID, count expr.Count) (n int, ok, handled bool) {
	switch {
	case count.Head == "LeftGraveyardThisTurn":
		n, ok = g.leftGraveyardCount(sourceController, source, count.Argument)
		return n, ok, true
	case count.Head == "TotalTurns":
		// PhaseHandler.getTurn: the game's turn number.
		return g.turn, true, true
	case count.Head == "FinishedEndOfTurnsThisTurn":
		// The end steps begun this turn, less the one in progress.
		n = g.endOfTurnsThisTurn
		if g.activePhase == EndOfTurn {
			n--
		}
		return n, true, true
	case count.Head == "EachSpentToCast":
		n, ok = g.eachSpentToCast(source, count)
		return n, ok, true
	case strings.HasPrefix(count.Head, "MostCardName"):
		n, ok = g.mostCardName(sourceController, source, count)
		return n, ok, true
	case count.Head == "MostProminentCreatureType":
		n, ok = g.mostProminentCreatureType(sourceController, source, count.Argument)
		return n, ok, true
	case strings.HasPrefix(count.Head, "RememberedWithSharedCardType"):
		n, ok = g.rememberedSharedCardType(source)
		return n, ok, true
	case strings.HasPrefix(count.Head, "LastTurnEntered_"):
		if sourceController == NoPlayer {
			return 0, false, true
		}
		n, ok = g.enteredCount(g.enteredLastTurn, sourceController, source, countText(count))
		return n, ok, true
	case count.Head == "MaxCombatDamageThisTurn":
		// Player.getMaxAssignedCombatDamage (Player.java:829-831): the most
		// combat damage any registered player was dealt this turn.
		for _, pid := range g.Players() {
			n = max(n, g.Player(pid).damageThisTurn[1])
		}
		return n, true, true
	case count.Head == "MaxOppDamageThisTurn":
		// Player.getMaxOpponentAssignedDamage (Player.java:822-824): the most
		// damage one registered opponent was dealt this turn.
		if sourceController == NoPlayer {
			return 0, false, true
		}
		for _, pid := range g.Players() {
			if pid != sourceController {
				n = max(n, g.Player(pid).DamageReceivedThisTurn)
			}
		}
		return n, true, true
	}
	var read func(p *Player) int
	switch {
	case count.Head == "YouScryThisTurn":
		read = func(p *Player) int { return p.scryThisTurn }
	case count.Head == "YouSurveilThisTurn":
		// AbilityUtils.java:2332-2334: Player.getSurveilThisTurn.
		read = func(p *Player) int { return p.surveilThisTurn }
	case count.Head == "YouDrewLastTurn":
		read = func(p *Player) int { return p.drawnLastTurn }
	case count.Head == "YouCastThisGame":
		read = func(p *Player) int { return p.spellsCastThisGame }
	case strings.Contains(count.Head, "LifeYourTeamGainedThisTurn"):
		// Player.getLifeGainedByTeamThisTurn: this port seats no teams, so the
		// team is the player alone and its gain is the player's own
		// (Player.java:470-478 adds each gain to every teammate's count).
		read = func(p *Player) int { return p.LifeGainedThisTurn }
	default:
		return 0, false, false
	}
	if sourceController == NoPlayer {
		return 0, false, true
	}
	return read(g.Player(sourceController)), true, true
}

// rememberedSharedCardType is Count$RememberedWithSharedCardType
// (AbilityUtils.java:2001-2020): the largest group the host's remembered cards
// make with one of them and every other one sharing a card type with it
// (CardType.sharesCardTypeWith: a core type in common), at least 1 -- Rowdy
// Crew's "if they share a card type".
func (g *Game) rememberedSharedCardType(source CardID) (int, bool) {
	if source == NoCard {
		return 0, false
	}
	var cards []CardID
	for _, e := range g.Card(source).Memory.Remembered() {
		if id, ok := e.AsCard(); ok {
			cards = append(cards, id)
		}
	}
	most := 1
	for _, a := range cards {
		n := 1
		ta := g.Card(a).Type()
		for _, b := range cards {
			if b == a {
				continue
			}
			for _, ct := range ta.CoreTypes() {
				if g.Card(b).Type().Has(ct) {
					n++
					break
				}
			}
		}
		most = max(most, n)
	}
	return most, true
}

// countText is the head of count with its parameters and argument as the
// script wrote them: ParseCount cut it at the first `.` and space, and the
// zone-entry heads read the string whole (AbilityUtils.java:2820).
func countText(count expr.Count) string {
	text := count.Head
	if len(count.Parameters) > 0 {
		text += "." + strings.Join(count.Parameters, ".")
	}
	if count.Argument != "" {
		text += " " + count.Argument
	}
	return text
}

// leftGraveyardCount is Count$LeftGraveyardThisTurn <valid>: the cards that
// left a graveyard this turn matching the valid string from the source
// controller's point of view, each as it last was there.
func (g *Game) leftGraveyardCount(sourceController PlayerID, source CardID, spec string) (int, bool) {
	if spec == "" {
		return 0, false
	}
	parsed := valid.Parse(spec)
	n := 0
	for i := range g.leftGraveyardThisTurn {
		if Matches(g, &g.leftGraveyardThisTurn[i], parsed, sourceController, source) {
			n++
		}
	}
	return n, true
}

// castSpent is how much mana of one kind a cast spent, by the name Java's
// MagicColor.toShortString reads: a color name or letter, or Colorless/C.
// false for any other name.
func castSpent(c *Card, kind string) (int, bool) {
	if strings.EqualFold(kind, "Colorless") || kind == "C" {
		return c.castColorless, true
	}
	color, ok := manaAtomColor(kind)
	if !ok || color.Count() != 1 {
		return 0, false
	}
	for i, col := range [...]mana.Colors{mana.White, mana.Blue, mana.Black, mana.Red, mana.Green} {
		if col == color {
			return c.castPaid[i], true
		}
	}
	return 0, false
}

// eachSpentToCast is Count$EachSpentToCast.<type>: the units of that mana the
// card's cast spent (getCastSA().getPayingMana, matched by Mana.toString), 0
// for a card that was not cast.
func (g *Game) eachSpentToCast(source CardID, count expr.Count) (int, bool) {
	if source == NoCard || len(count.Parameters) == 0 {
		return 0, false
	}
	c := g.Card(source)
	if !c.wasCast {
		return 0, true
	}
	return castSpent(c, count.Parameters[0])
}

// adamantMet is Count$Adamant[_N].<color>.<then>.<else>'s test
// (AbilityUtils.java:1795-1800): the cast spent at least N (3 by default) mana
// of the color. ok is false for an unreadable N or color.
func adamantMet(c *Card, count expr.Count) (met, ok bool) {
	num := 3
	if rest, cut := strings.CutPrefix(count.Head, "Adamant_"); cut {
		n, err := strconv.Atoi(rest)
		if err != nil {
			return false, false
		}
		num = n
	} else if count.Head != "Adamant" {
		return false, false
	}
	if len(count.Parameters) < 3 {
		return false, false
	}
	paid, ok := castSpent(c, count.Parameters[0])
	return paid >= num, ok
}

// castAdamant is CastSA>Count$Adamant...: the test against the host's own
// cast record (adjustTriggerContext swaps in getCastSA, whose root is itself),
// each branch a plain number -- the corpus writes no other. Catharsis'
// "if {W}{W} was spent to cast it" as it enters.
func (g *Game) castAdamant(source CardID, count expr.Count) (int, bool) {
	met, ok := adamantMet(g.Card(source), count)
	if !ok {
		return 0, false
	}
	pick := count.Parameters[2]
	if met {
		pick = count.Parameters[1]
	}
	n, err := strconv.Atoi(pick)
	return n, err == nil
}

// mostCardName is Count$MostCardName[<zones>] <valid>[,<valid>...]: the size
// of the largest group of same-named cards among those matching, in the named
// zones (every player's) or the battlefield. Java returns the count without
// doXMath (AbilityUtils.java:2856); no corpus line writes an operator after
// it, so the caller's operator never meets that difference.
func (g *Game) mostCardName(sourceController PlayerID, source CardID, count expr.Count) (int, bool) {
	if count.Argument == "" {
		return 0, false
	}
	zones := []ZoneType{Battlefield}
	if suffix := strings.TrimPrefix(count.Head, "MostCardName"); suffix != "" {
		zones = nil
		for _, name := range strings.Split(suffix, ",") {
			z, ok := ZoneByName(name)
			if !ok {
				return 0, false
			}
			zones = append(zones, z)
		}
	}
	parsed := valid.Parse(count.Argument)
	var names []string
	var sizes []int
	most := 0
	for _, z := range zones {
		for _, pid := range g.Players() {
			for _, id := range g.Zone(z, pid).Cards() {
				c := g.Card(id)
				if !Matches(g, c, parsed, sourceController, source) {
					continue
				}
				i := indexOfString(names, c.Name())
				if i < 0 {
					names, sizes, i = append(names, c.Name()), append(sizes, 0), len(names)
				}
				sizes[i]++
				most = max(most, sizes[i])
			}
		}
	}
	return most, true
}

// mostProminentCreatureType is Count$MostProminentCreatureType <valid>: the
// largest number of matching battlefield cards that share one creature type.
// A changeling's type line holds every creature type here, so it counts
// toward each, the same total Java's separate allCreatureType tally adds.
// Without the DB's subtype vocabulary a creature type cannot be told from any
// other subtype: unresolved.
func (g *Game) mostProminentCreatureType(sourceController PlayerID, source CardID, spec string) (int, bool) {
	reg := g.db.Types()
	if spec == "" || reg == nil {
		return 0, false
	}
	parsed := valid.Parse(spec)
	var types []string
	var sizes []int
	most := 0
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			c := g.Card(id)
			if !Matches(g, c, parsed, sourceController, source) {
				continue
			}
			for _, t := range creatureTypesOf(c.Type(), reg) {
				i := indexOfString(types, t)
				if i < 0 {
					types, sizes, i = append(types, t), append(sizes, 0), len(types)
				}
				sizes[i]++
				most = max(most, sizes[i])
			}
		}
	}
	return most, true
}

// creatureTypesOf is CardType.getCreatureTypes: the line's creature types,
// each once.
func creatureTypesOf(line cardtype.Line, reg *cardtype.Registry) []string {
	var out []string
	for _, t := range line.CreatureTypes(reg) {
		if indexOfString(out, t) < 0 {
			out = append(out, t)
		}
	}
	return out
}

// cardsInGame is Game.getCardsInGame (Game.java:761-789): each player's
// graveyard, hand, library, battlefield (phased-out permanents included,
// getCards(false)), exile and Command zone, then the stack.
func (g *Game) cardsInGame() []CardID {
	var out []CardID
	for _, pid := range g.Players() {
		out = append(out, g.Zone(Graveyard, pid).Cards()...)
		out = append(out, g.Zone(Hand, pid).Cards()...)
		out = append(out, g.Zone(Library, pid).Cards()...)
		out = append(out, g.Zone(Battlefield, pid).CardsIncludingPhasedOut()...)
		out = append(out, g.Zone(Exile, pid).Cards()...)
		out = append(out, g.Zone(Command, pid).Cards()...)
	}
	for _, pid := range g.Players() {
		out = append(out, g.Zone(Stack, pid).Cards()...)
	}
	return out
}

// indexOfString is the position of s in list, -1 when absent.
func indexOfString(list []string, s string) int {
	for i, v := range list {
		if v == s {
			return i
		}
	}
	return -1
}

// isPrimeValue is Count$IsPrime <amount>.<then>.<else> (AbilityUtils.java:
// 1616-1622): then when the amount is a prime number (Guava's IntMath.isPrime,
// which throws on a negative number -- unresolved here, GO-7).
func isPrimeValue(n int) (prime, ok bool) {
	if n < 0 {
		return false, false
	}
	if n < 2 {
		return false, true
	}
	for d := 2; d*d <= n; d++ {
		if n%d == 0 {
			return false, true
		}
	}
	return true, true
}

// castManaConditionsMet is CardTraitBase.meetsCommonRequirements' three
// conditions on the mana the host's cast spent (CardTraitBase.java:355-375,
// :492-507): Adamant$ (three or more of the color, or of any one color for
// "Any"), ManaSpent$ (every color the value names was spent) and
// ManaNotSpent$ (not every one was). The first two fail for a host that was
// not cast (getCastSA null); ManaNotSpent$ passes for one. A value naming no
// color records a pending error and reads false (GO-7).
func castManaConditionsMet(g *Game, host *Card, t *compile.Ability) bool {
	if v, ok := t.Param("Adamant"); ok {
		if !host.wasCast {
			return false
		}
		if v == "Any" {
			any3 := false
			for _, n := range host.castPaid {
				any3 = any3 || n >= 3
			}
			if !any3 {
				return false
			}
		} else {
			n, ok := castSpent(host, v)
			if !ok {
				g.recordPendingError(fmt.Errorf("engine: %q: Adamant$ %q not resolvable", host.Def.Name, v))
				return false
			}
			if n < 3 {
				return false
			}
		}
	}
	for _, key := range [...]string{"ManaSpent", "ManaNotSpent"} {
		v, ok := t.Param(key)
		if !ok {
			continue
		}
		colors, ok := manaAtomColor(v)
		if !ok {
			g.recordPendingError(fmt.Errorf("engine: %q: %s$ %q not resolvable", host.Def.Name, key, v))
			return false
		}
		if (key == "ManaSpent") != castSpentAllColors(host, colors) {
			return false
		}
	}
	return true
}

// castSpentAllColors is getCastSA().getPayingColors().hasAllColors(colors): c
// was cast and its cast spent mana of every color in colors. False for a card
// that was not cast (no cast SpellAbility).
func castSpentAllColors(c *Card, colors mana.Colors) bool {
	if !c.wasCast {
		return false
	}
	for i, col := range [...]mana.Colors{mana.White, mana.Blue, mana.Black, mana.Red, mana.Green} {
		if colors.HasAny(col) && c.castPaid[i] == 0 {
			return false
		}
	}
	return true
}

// checkDefinedPlayerMatches is CardTraitBase.meetsCommonRequirements'
// CheckDefinedPlayer$/DefinedPlayerCompare$ (CardTraitBase.java:458-468): how
// many players the Defined$ names, compared (GE1 by default) -- "if you're
// the monarch" is CheckDefinedPlayer$ You.isMonarch. A Defined$ or compare
// this port cannot read records a pending error and reads false (GO-7).
func checkDefinedPlayerMatches(g *Game, host *Card, amounts map[string]expr.Amount, t *compile.Ability) bool {
	defined, ok := t.Param("CheckDefinedPlayer")
	if !ok {
		return true
	}
	var refs abilityRefs
	if a := g.resolving; a != nil && a.Source == host.ID {
		refs = a.refs()
	}
	players, err := definedPlayers(g, host.Controller(), host.ID, defined, refs)
	if err != nil {
		g.recordPendingError(fmt.Errorf("engine: CheckDefinedPlayer$: %w", err))
		return false
	}
	compare, ok := t.Param("DefinedPlayerCompare")
	if !ok {
		compare = "GE1"
	}
	if len(compare) < 3 {
		g.recordPendingError(fmt.Errorf("engine: DefinedPlayerCompare$ %q is not a comparison", compare))
		return false
	}
	right, ok := resolveNamedAmount(g, amounts, host, compare[2:])
	if !ok {
		g.recordPendingError(fmt.Errorf("engine: DefinedPlayerCompare$ %q is not resolvable", compare))
		return false
	}
	return compareOp(len(players), compare[:2], right)
}

// werewolfConditionsMet is CardTraitBase.meetsCommonRequirements'
// WerewolfTransformCondition$ and WerewolfUntransformCondition$
// (CardTraitBase.java:509-520) over the spells cast last turn
// (Game.castLastTurn): a transform needs no spell cast last turn, an
// untransform needs some player to have cast two or more. Each spell counts
// for the player who cast it, the controller its stack object had.
func (g *Game) werewolfConditionsMet(transform, untransform bool) bool {
	if transform && len(g.castLastTurn) > 0 {
		return false
	}
	if !untransform {
		return true
	}
	for _, pid := range g.Players() {
		n := 0
		for _, rec := range g.castLastTurn {
			if rec.controller == pid {
				n++
			}
		}
		if n > 1 {
			return true
		}
	}
	return false
}
