// The history of the turn: every spell cast, by whom and from where, every
// zone change, every counter placement and every attacker.
// Count$ThisTurnCast_<valid> (about 100 corpus uses: storm-like counts, Weftwalking's
// "first spell each turn") counts the entries whose card matches <valid> as
// it was when cast (MagicStack.getSpellsCastThisTurn, an LKI list).
// Count$ThisTurnEntered_<zone>[_from_<zone>]_<valid>, Count$CountersAddedThisTurn,
// Count$CreaturesAttackedThisTurn and Count$LifeYouGainedThisTurn read the
// others (AbilityUtils.java:2805, :2819, :2517, :2360); the zone changes out of
// the battlefield are also Game.leftBattlefieldThisTurn, which
// Count$LeftBattlefieldThisTurn, Void and Revolt read (:2537, :2698, :2268); the
// moves out of a graveyard are Game.leftGraveyardThisTurn (:2543). All live on the Game
// or its players, are copied by Game.Clone and end at cleanup (GO-2).

package engine

import (
	"slices"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
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
	g.Player(pid).spellsCastThisGame++
	c := g.Card(card)
	g.castThisTurn = append(g.castThisTurn, castRecord{card: card, controller: pid, from: c.castFrom, stamp: c.zoneStamp})
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
	if from == Graveyard {
		// Game.addLeftGraveyardThisTurn(lastKnownInfo), GameAction.java:550-552:
		// the card as it last was in the graveyard, whatever zone it went to.
		g.leftGraveyardThisTurn = append(g.leftGraveyardThisTurn, *before)
	}
	snap := *before
	if to == Battlefield {
		snap = *c
	}
	g.enteredThisTurn = append(g.enteredThisTurn, zoneEntry{card: snap, from: from, to: to})
}

// recordAttacked adds attacker to its controller's attackers of the turn
// (Player.addCreaturesAttackedThisTurn), and the player it attacks directly,
// once, to the players attacked this turn (the map's Player keys) and this
// combat (attackedPlayersThisCombat, Player.java:1839-1841); the entity it
// attacks goes on the card's own record (CardDamageHistory.
// setCreatureAttackedThisCombat, CardDamageHistory.java:62-64). Called after
// the declaration's targets are set.
func (g *Game) recordAttacked(attacker CardID) {
	c := g.Card(attacker)
	target, declared := g.combat.AttackTargets[attacker]
	if declared {
		c.attackedThisTurn = append(c.attackedThisTurn, target)
	}
	p := g.Player(c.Controller())
	p.attackedThisTurn = append(p.attackedThisTurn, attacker)
	pid, ok := target.AsPlayer()
	if !ok {
		return
	}
	if !slices.Contains(p.attackedPlayersThisTurn, pid) {
		p.attackedPlayersThisTurn = append(p.attackedPlayersThisTurn, pid)
	}
	if !slices.Contains(p.attackedPlayersThisCombat, pid) {
		p.attackedPlayersThisCombat = append(p.attackedPlayersThisCombat, pid)
	}
}

// recordCountersAdded logs a placement (Game.addCounterAddedThisTurn,
// Game.java:1244-1253): putter is the player who placed the counters
// (addCardCounters' placer), and a placement with no placer or no counters is
// ignored, as Java ignores a null putter.
func (g *Game) recordCountersAdded(putter PlayerID, id CardID, ct CounterType, n int) {
	if putter == NoPlayer || n <= 0 {
		return
	}
	g.countersAddedThisTurn = append(g.countersAddedThisTurn, counterAddition{
		counter: ct, putter: putter, card: *g.Card(id), n: n,
	})
}

// recordCountersRemoved logs n counters of kind ct removed from target
// (Game.addCounterRemovedThisTurn, Game.java:1306-1311): a card as it is just
// after the removal, Java's LKI copy, or a player.
func (g *Game) recordCountersRemoved(target EntityID, ct CounterType, n int) {
	if n <= 0 {
		return
	}
	r := counterRemoval{counter: ct, player: NoPlayer, n: n}
	if pid, ok := target.AsPlayer(); ok {
		r.player = pid
	} else if id, ok := target.AsCard(); ok && id != NoCard && int(id) < len(g.cards) {
		r.card = *g.Card(id)
	} else {
		return
	}
	g.countersRemoved = append(g.countersRemoved, r)
}

// countersRemovedCount is Count$CountersRemovedThisTurn <type> <valid>
// (AbilityUtils.java:2839-2844, Game.getCounterRemovedThisTurn :1313-1321):
// the counters of the type removed this turn from the cards and players that
// match the valid string from the source controller's point of view. Java
// splits the head on spaces and reads the third part whole, so text is the
// head as written. A player matches only a player alternative (Player.isValid
// is false for a Card one), a card only a card one; a player spec this port
// cannot read is unresolved (GO-7).
func (g *Game) countersRemovedCount(sourceController PlayerID, source CardID, text string) (int, bool) {
	parts := strings.Split(text, " ")
	if len(parts) < 3 || sourceController == NoPlayer {
		return 0, false
	}
	kind, spec := CounterType(strings.ToUpper(parts[1])), parts[2]
	parsed := valid.Parse(spec)
	namesPlayers := false
	for _, alt := range strings.Split(spec, ",") {
		base, _, _ := strings.Cut(alt, ".")
		if _, ok := matchesPlayerBase(sourceController, sourceController, base); ok {
			namesPlayers = true
		}
	}
	n := 0
	for i := range g.countersRemoved {
		r := &g.countersRemoved[i]
		if r.counter != kind {
			continue
		}
		if r.player == NoPlayer {
			if Matches(g, &r.card, parsed, sourceController, source) {
				n += r.n
			}
			continue
		}
		if !namesPlayers {
			continue
		}
		matched, ok := matchesPlayerSpec(g, r.player, sourceController, source, spec)
		if !ok {
			return 0, false
		}
		if matched {
			n += r.n
		}
	}
	return n, true
}

// recordActivation is MagicStack.addAbilityActivatedThisTurn (MagicStack.java:
// 983-987) for an activated ability as it goes on the stack (:310) or a mana
// ability as it resolves (:234), neither a copy nor a trigger: the ability and
// a last-known copy of its host. before is the host as it stood before the
// costs were paid. A cost that moved the host (exiled from a graveyard,
// sacrificed) leaves Java's ability on the old card object, which still reads
// the zone it left (Zone.remove does not reset it, Zone.java:162-167), so the
// copy is before; otherwise it is the host now, its costs paid.
func (g *Game) recordActivation(a *Ability, before *Card) {
	host := *g.Card(a.Source)
	if before != nil && before.zoneStamp != host.zoneStamp {
		host = *before
	}
	g.activatedThisTurn = append(g.activatedThisTurn, activationRecord{ability: *a, host: host})
}

// thisTurnActivatedCount is Count$ThisTurnActivated_<valid> (AbilityUtils.java:
// 2805-2816, CardUtil.getThisTurnActivated :139-143): the abilities activated
// this turn that match the SpellAbility valid string from the source
// controller's point of view, each read with its host as last known. text is
// the head as written; Java takes the part after the first underscore up to
// the next. The abilities still paying their costs (Game.costPaymentStack,
// :2810-2814) are not counted: only a Type$ Ability ReduceCost reads this
// head while a cost is paid, and costmod.go applies Type$ Spell lines alone.
func (g *Game) thisTurnActivatedCount(sourceController PlayerID, source CardID, text string) (int, bool) {
	parts := strings.Split(text, "_")
	if len(parts) < 2 || parts[1] == "" || source == NoCard || sourceController == NoPlayer {
		return 0, false
	}
	host := g.Card(source)
	n := 0
	for i := range g.activatedThisTurn {
		r := &g.activatedThisTurn[i]
		matched, recognized := g.spellAbilityMatchesAs(&r.ability, &r.host, parts[1], host, sourceController, nil)
		if !recognized {
			return 0, false
		}
		if matched {
			n++
		}
	}
	return n, true
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
	return g.enteredCount(g.enteredThisTurn, sourceController, source, text)
}

// enteredCount is thisTurnEnteredCount over entries: this turn's zone changes,
// or last turn's for Count$LastTurnEntered_ (CardUtil.getLastTurnEntered,
// Zone.getCardsAddedLastTurn).
func (g *Game) enteredCount(entries []zoneEntry, sourceController PlayerID, source CardID, text string) (int, bool) {
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
	for i := range entries {
		e := &entries[i]
		if e.to == dest && (!hasOrigin || e.from == origin) && Matches(g, &e.card, parsed, sourceController, source) {
			n++
		}
	}
	return n, true
}

// leftBattlefieldThisTurn calls each with every card that left the battlefield
// this turn, as last known there, in the order it left: Game.getLeftBattlefieldThisTurn
// (Game.java:295-303, filled by GameAction.changeZone with the LKI copy,
// GameAction.java:539-540, cleared at cleanup, Game.java:1228). The turn's zone
// changes already hold that copy (recordEntered), so the list is read off them
// rather than kept twice. each returns false to stop.
func (g *Game) leftBattlefieldThisTurn(each func(lki *Card) bool) {
	for i := range g.enteredThisTurn {
		e := &g.enteredThisTurn[i]
		if e.from == Battlefield && !each(&e.card) {
			return
		}
	}
}

// leftBattlefieldCount is Count$LeftBattlefieldThisTurn <valid>
// (AbilityUtils.java:2537-2542): the cards that left the battlefield this turn
// matching the valid string from the source controller's point of view, each
// judged as it last was there ("creatures that left the battlefield under
// your control": Creature.YouCtrl reads the LKI's controller).
func (g *Game) leftBattlefieldCount(sourceController PlayerID, source CardID, spec string) (int, bool) {
	if spec == "" {
		return 0, false
	}
	parsed := valid.Parse(spec)
	n := 0
	g.leftBattlefieldThisTurn(func(lki *Card) bool {
		if Matches(g, lki, parsed, sourceController, source) {
			n++
		}
		return true
	})
	return n, true
}

// isVoid is Game.isVoid (Game.java:1432-1435): a nonland permanent left the
// battlefield this turn, or a spell was warped this turn. This port casts no
// spell for its Warp cost (castspell.go's permanentEffect: no Warp cast mode;
// continuous.go reads a Spell.Warp grant as never used), so the second half
// is false for every spell this game cast -- what the cast record says, not a
// guess about one.
func (g *Game) isVoid() bool {
	found := false
	g.leftBattlefieldThisTurn(func(lki *Card) bool {
		found = !lki.Type().Has(cardtype.Land)
		return !found
	})
	return found
}

// hasRevolt is Player.hasRevolt (Player.java:2095-2097): a permanent pid
// controlled left the battlefield this turn.
func (g *Game) hasRevolt(pid PlayerID) bool {
	found := false
	g.leftBattlefieldThisTurn(func(lki *Card) bool {
		found = lki.Controller() == pid
		return !found
	})
	return found
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
