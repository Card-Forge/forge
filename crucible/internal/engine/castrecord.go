// The history of the turn: every spell cast, by whom and from where, every
// zone change, every counter placement and every attacker.
// Count$ThisTurnCast_<valid> (about 100 corpus uses: storm-like counts, Weftwalking's
// "first spell each turn") counts the entries whose card matches <valid> as
// it was when cast (MagicStack.getSpellsCastThisTurn, an LKI list).
// Count$ThisTurnEntered_<zone>[_from_<zone>]_<valid>, Count$CountersAddedThisTurn,
// Count$CreaturesAttackedThisTurn and Count$LifeYouGainedThisTurn read the
// others (AbilityUtils.java:2805, :2819, :2517, :2360). All live on the Game
// or its players, are copied by Game.Clone and end at cleanup (GO-2).

package engine

import (
	"slices"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// commanderCasts is Player.getTotalCommanderCast (Player.java:2894-2900) for
// any player, and Player.getCommanderCast(card) (:2875-2877) for any card: how
// many times the player cast a commander of theirs from the Command zone, the
// record MagicStack.java:346-349 keeps as the spell is cast and commander tax
// reads (CostAdjustment.java:57-61). The increment needs Card.isCommander,
// which only Player.addCommander sets -- the Commander game setup and the
// GameState |IsCommander card option (GameState.java:1377), neither ported:
// the fixture loader reports |IsCommander unapplied. So no card here is a
// commander, every player's record is empty, and the answer is 0 -- Java's
// own for a game without commanders, not a guess. A Commander setup (the
// designation, the Commander Effect's cast permission, replacement and tax,
// Player.createCommanderEffect) must replace this with the record it keeps.
func (*Game) commanderCasts() int { return 0 }

// recordSpellCast counts card as cast by pid and adds it to the turn's
// history. Called with the card already on the stack: from is where the cast
// started, kept by the cast path in Card.castFrom.
func (g *Game) recordSpellCast(pid PlayerID, card CardID) {
	g.Player(pid).SpellsCastThisTurn++
	g.castThisTurn = append(g.castThisTurn, castRecord{card: card, controller: pid, from: g.Card(card).castFrom})
}

// thisTurnCastCount is Count$ThisTurnCast_<spec>: the spells cast so far this
// turn whose card, read as its caster controlled it, matches spec from
// source's controller's point of view.
func (g *Game) thisTurnCastCount(source CardID, spec string) (int, bool) {
	if source == NoCard {
		return 0, false
	}
	parsed := valid.Parse(strings.TrimSpace(spec))
	host := g.Card(source)
	n := 0
	for _, rec := range g.castThisTurn {
		view := *g.Card(rec.card)
		view.controller = rec.controller
		if Matches(g, &view, parsed, host.Controller(), source) {
			n++
		}
	}
	return n, true
}

// recordEntered logs c's move from from to to (Zone.add, Zone.saveLKI). before
// is c as it stood just before the move; a battlefield entry logs the card as
// it is now, any other move as it was. A move within one zone logs nothing.
func (g *Game) recordEntered(c, before *Card, from, to ZoneType) {
	if from == to {
		return
	}
	snap := *before
	if to == Battlefield {
		snap = *c
	}
	g.enteredThisTurn = append(g.enteredThisTurn, zoneEntry{card: snap, from: from, to: to})
}

// recordAttacked adds attacker to its controller's attackers of the turn
// (Player.addCreaturesAttackedThisTurn), and the player it attacks directly,
// once, to the players attacked this turn (the map's Player keys). Called
// after the declaration's targets are set.
func (g *Game) recordAttacked(attacker CardID) {
	p := g.Player(g.Card(attacker).Controller())
	p.attackedThisTurn = append(p.attackedThisTurn, attacker)
	if pid, ok := g.combat.AttackTargets[attacker].AsPlayer(); ok && !slices.Contains(p.attackedPlayersThisTurn, pid) {
		p.attackedPlayersThisTurn = append(p.attackedPlayersThisTurn, pid)
	}
}

// recordCountersAdded logs a placement (Game.addCounterAddedThisTurn,
// Game.java:1244-1253): putter is the controller of the source that put the
// counters, and a placement with no source (a turn-based action) or none
// counters is ignored, as Java ignores a null putter.
func (g *Game) recordCountersAdded(source, id CardID, ct CounterType, n int) {
	if source == NoCard || n <= 0 {
		return
	}
	g.countersAddedThisTurn = append(g.countersAddedThisTurn, counterAddition{
		counter: ct, putter: g.Card(source).Controller(), card: *g.Card(id), n: n,
	})
}

// zoneByLooseName is ZoneType.smartValueOf: one zone name, in any case.
func zoneByLooseName(name string) (ZoneType, bool) {
	if name == "" {
		return None, false
	}
	return ZoneByName(strings.ToUpper(name[:1]) + strings.ToLower(name[1:]))
}

// thisTurnEnteredCount is Count$ThisTurnEntered_<Dest>[_from_<Origin>]_<valid>
// (AbilityUtils.java:2805-2878): the zone changes of the turn into Dest (out of
// Origin when named) whose card, as last known, matches the valid string from
// the source controller's point of view. text is the whole head with its
// parameters, as the script wrote it. Java splits at the first five
// underscores, so a valid string that holds one is cut short; and an
// unreadable zone (smartValueOf's null) or a shape with a `$` is unresolved.
func (g *Game) thisTurnEnteredCount(sourceController PlayerID, source CardID, text string) (int, bool) {
	if strings.Contains(text, "$") {
		return 0, false
	}
	parts := strings.SplitN(text, "_", 5)
	if len(parts) < 3 {
		return 0, false
	}
	dest, ok := zoneByLooseName(parts[1])
	if !ok {
		return 0, false
	}
	spec, origin, hasOrigin := parts[2], None, false
	if parts[2] == "from" {
		if len(parts) < 5 {
			return 0, false
		}
		if origin, ok = zoneByLooseName(parts[3]); !ok {
			return 0, false
		}
		spec, hasOrigin = parts[4], true
	}
	parsed := valid.Parse(spec)
	n := 0
	for i := range g.enteredThisTurn {
		e := &g.enteredThisTurn[i]
		if e.to == dest && (!hasOrigin || e.from == origin) && Matches(g, &e.card, parsed, sourceController, source) {
			n++
		}
	}
	return n, true
}

// creaturesAttackedCount is Count$CreaturesAttackedThisTurn <valid>
// (AbilityUtils.java:2517-2521): the creatures the source controller attacked
// with this turn that match the valid string, one per declaration.
func (g *Game) creaturesAttackedCount(sourceController PlayerID, source CardID, spec string) (int, bool) {
	if spec == "" {
		return 0, false
	}
	parsed := valid.Parse(spec)
	n := 0
	for _, id := range g.Player(sourceController).attackedThisTurn {
		if Matches(g, g.Card(id), parsed, sourceController, source) {
			n++
		}
	}
	return n, true
}

// countersAddedCount is Count$CountersAddedThisTurn <type> <players> <valid>
// (Game.getCounterAddedThisTurn, Game.java:1256-1277): the counters of the
// type ("Any" is every type) that putters matching the player string put on
// cards matching the valid string, the cards read as they were when the
// counters went on.
func (g *Game) countersAddedCount(sourceController PlayerID, source CardID, argument string) (int, bool) {
	args := strings.Split(argument, " ")
	if len(args) != 3 {
		return 0, false
	}
	parsed := valid.Parse(args[2])
	n := 0
	for i := range g.countersAddedThisTurn {
		a := &g.countersAddedThisTurn[i]
		if args[0] != "Any" && !strings.EqualFold(args[0], string(a.counter)) {
			continue
		}
		who, ok := matchesPlayerSpec(g, a.putter, sourceController, source, args[1])
		if !ok {
			return 0, false
		}
		if who && Matches(g, &a.card, parsed, sourceController, source) {
			n += a.n
		}
	}
	return n, true
}

// unlockedDoorNames is Player.getUnlockedDoors: the name of every unlocked door
// of the Rooms pid controls, left door first within a Room.
func (g *Game) unlockedDoorNames(pid PlayerID) []string {
	var names []string
	for _, id := range g.Zone(Battlefield, pid).Cards() {
		c := g.Card(id)
		for _, d := range c.UnlockedDoors() {
			names = append(names, c.roomDef.Faces[d].Name)
		}
	}
	return names
}
