// Branch amounts: the Count$ heads that pick one of two written values by a
// condition -- Count$Compare and the player-state family (Hellbent,
// Metalcraft, Morbid, wasCastFromGraveyard, ...) -- each branch itself an
// amount (a number or an SVar name, calculateAmount's own input).
//
// Ported from forge-game/src/main/java/forge/game/ability/AbilityUtils.java's
// xCount (:1608-1614 Compare, :2163-2178 wasCastFrom, :2205-2211
// InOwnMainPhase, :2256-2290 the player flags, :2346-2357 MaxSpeed and Night,
// :2693-2697 Morbid).

package engine

import (
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// branchCountValue is xCount for the branch heads. handled is false for every
// other head, which countValue then dispatches. A branch is chosen as Java
// chooses it and evaluated as calculateAmount would (branchAmount); a branch
// or a condition this port cannot evaluate leaves the whole amount unresolved
// (GO-7).
//
// The player is the source controller (xCount's own `player`), and every flag
// reads state the engine already keeps for the same flag's Condition$ use
// (continuousConditionMet, continuous.go); Adamant reads the spell's own paid
// mana (adamantValue). Not resolved: Bargain, Madness, Escaped, Emerged,
// Foretold, Freerunning, AltCost, PromisedGift (none of those cast records is
// kept), CommittedCrimeThisTurn, Revolt and Void (no per-turn record),
// StartingPlayer, and Random (an amount read twice must not roll twice).
func branchCountValue(g *Game, amounts map[string]expr.Amount, sourceController PlayerID, source CardID, count expr.Count, depth int) (n int, ok, handled bool) {
	branch := func(yes bool) (int, bool, bool) {
		if len(count.Parameters) < 2 {
			return 0, false, true
		}
		pick := count.Parameters[1]
		if yes {
			pick = count.Parameters[0]
		}
		n, ok := branchAmount(g, amounts, sourceController, source, pick, depth)
		return n, ok, true
	}
	if count.Head == "Compare" {
		c := count.Compare
		if c == nil {
			return 0, false, true
		}
		lhs, okL := branchAmount(g, amounts, sourceController, source, c.LHS, depth)
		rhs, okR := branchAmount(g, amounts, sourceController, source, c.RHS, depth)
		if !okL || !okR {
			return 0, false, true
		}
		pick := c.Else
		if javaCompare(lhs, c.Comparator, rhs) {
			pick = c.Then
		}
		n, ok := branchAmount(g, amounts, sourceController, source, pick, depth)
		return n, ok, true
	}
	if strings.HasPrefix(count.Head, "wasCastFrom") {
		matched, known := castFromMatches(g, sourceController, source, count.Head)
		if !known {
			return 0, false, true
		}
		return branch(matched)
	}
	if strings.HasSuffix(count.Head, "InOwnMainPhase") {
		// Integer.parseInt branches, not calculateAmount (AbilityUtils.java:2210).
		if sourceController == NoPlayer || source == NoCard || len(count.Parameters) < 2 {
			return 0, false, true
		}
		phase := g.ActivePhase()
		mine := (phase == Main1 || phase == Main2) && g.ActivePlayer() == sourceController &&
			(!strings.HasPrefix(count.Head, "IfCast") || g.Card(source).wasCast)
		pick := count.Parameters[1]
		if mine {
			pick = count.Parameters[0]
		}
		n, err := strconv.Atoi(pick)
		return n, err == nil, true
	}
	if strings.HasPrefix(count.Head, "Adamant") {
		n, ok := g.adamantValue(amounts, sourceController, source, count, depth)
		return n, ok, true
	}
	var flag func(PlayerID) bool
	switch count.Head {
	case "Hellbent":
		flag = func(p PlayerID) bool { return len(g.Zone(Hand, p).Cards()) == 0 }
	case "Metalcraft":
		flag = func(p PlayerID) bool { return battlefieldArtifactCount(g, p) >= 3 }
	case "Delirium":
		flag = func(p PlayerID) bool { return graveyardCoreTypeCount(g, p) >= 4 }
	case "Threshold":
		flag = func(p PlayerID) bool { return len(g.Zone(Graveyard, p).Cards()) >= 7 }
	case "FatefulHour":
		flag = func(p PlayerID) bool { return g.Player(p).Life <= 5 }
	case "Monarch":
		flag = func(p PlayerID) bool { return g.Monarch() == p }
	case "Initiative":
		flag = func(p PlayerID) bool { return g.Initiative() == p }
	case "Blessing":
		flag = func(p PlayerID) bool { return g.Player(p).Blessing }
	case "MaxSpeed":
		// Player.maxSpeed: speed == 4 (Player.java:1907).
		flag = func(p PlayerID) bool { return g.Player(p).Speed == 4 }
	case "Night":
		flag = func(PlayerID) bool { return g.DayTime() == Night }
	case "Landfall":
		// Player.hasLandfall (Player.java:2113): a land was added to the
		// player's battlefield this turn.
		flag = func(p PlayerID) bool {
			n, _ := g.thisTurnEnteredCount(p, source, "ThisTurnEntered_Battlefield_Land.YouCtrl")
			return n > 0
		}
	case "Morbid":
		// CardUtil.getThisTurnEntered(Graveyard, Battlefield, "Creature", ...).
		flag = func(p PlayerID) bool {
			n, _ := g.thisTurnEnteredCount(p, source, "ThisTurnEntered_Graveyard_from_Battlefield_Creature")
			return n > 0
		}
	default:
		return 0, false, false
	}
	if sourceController == NoPlayer {
		return 0, false, true
	}
	return branch(flag(sourceController))
}

// adamantValue is Count$Adamant[_N].<Color>.<then>.<else>
// (AbilityUtils.java:1795-1800): then when the mana paid for the root ability
// held at least N (3 by default) of the color. The root is read only for a
// spell resolving off the stack, whose paid mana is its cast's (castPaid); a
// triggered or activated root's own payment is not recorded, unresolved.
func (g *Game) adamantValue(amounts map[string]expr.Amount, sourceController PlayerID, source CardID, count expr.Count, depth int) (int, bool) {
	num := 3
	if rest, ok := strings.CutPrefix(count.Head, "Adamant_"); ok {
		n, err := strconv.Atoi(rest)
		if err != nil {
			return 0, false
		}
		num = n
	} else if count.Head != "Adamant" {
		return 0, false
	}
	a := g.resolving
	if len(count.Parameters) < 3 || a == nil || source == NoCard || a.Source != source || g.Card(source).Zone != Stack {
		return 0, false
	}
	color, ok := manaAtomColor(count.Parameters[0])
	if !ok || color.Count() != 1 {
		return 0, false
	}
	paid := 0
	for i, col := range [...]mana.Colors{mana.White, mana.Blue, mana.Black, mana.Red, mana.Green} {
		if col == color {
			paid = g.Card(source).castPaid[i]
		}
	}
	pick := count.Parameters[2]
	if paid >= num {
		pick = count.Parameters[1]
	}
	return branchAmount(g, amounts, sourceController, source, pick, depth)
}

// castFromMatches is Count$wasCastFrom[Your]<Zone>[ByYou]'s own test
// (AbilityUtils.java:2164-2176): the card was cast from that zone, by the
// player when ByYou, from the player's own zone when Your. known is false for
// a zone name ZoneByName does not know.
func castFromMatches(g *Game, player PlayerID, source CardID, head string) (matched, known bool) {
	if source == NoCard {
		return false, false
	}
	zone := strings.TrimPrefix(head, "wasCastFrom")
	your := strings.Contains(head, "Your")
	byYou := strings.Contains(head, "ByYou")
	if your {
		zone = strings.TrimPrefix(zone, "Your")
	}
	if byYou {
		zone, _, _ = strings.Cut(zone, "ByYou")
	}
	z, ok := ZoneByName(zone)
	if !ok {
		return false, false
	}
	c := g.Card(source)
	return c.wasCast && c.castFrom == z && (!byYou || c.castBy == player) && (!your || c.Owner == player), true
}

// branchAmount is calculateAmount for one written branch or operand: blank is
// 0 (calculateAmount's own first line), an integer is itself, anything else
// the SVar it names.
func branchAmount(g *Game, amounts map[string]expr.Amount, sourceController PlayerID, source CardID, s string, depth int) (int, bool) {
	if s == "" {
		return 0, true
	}
	if n, err := strconv.Atoi(s); err == nil {
		return n, true
	}
	return namedAmount(g, amounts, sourceController, source, s, depth)
}

// javaCompare is Expressions.compare (forge/util/Expressions.java:45-65): the
// operator is found by containment in the written comparator, in Java's order,
// so `GEMePlus` reads GE.
func javaCompare(left int, comparator string, right int) bool {
	for _, op := range [...]string{"LT", "LE", "EQ", "GE", "GT", "NE", "M2"} {
		if strings.Contains(comparator, op) {
			return compareOp(left, op, right)
		}
	}
	return false
}
