// Mode$ CounterAdded and Mode$ CounterAddedOnce: the triggers a counter put on
// a card sets off (Card.addCounter, Card.java:1780-1822). Every effect that puts
// counters on a card goes through addCardCounters, so a Saga's chapters, "whenever
// one or more +1/+1 counters are put on" and the rest see every placement.

//enginelint:allow id zone card game valid ability control event amount parts trigger statictrigger

package engine

import (
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// addCardCounters puts n counters of ct on card id, emits the event, then
// fires the triggers: Mode$ CounterAdded once per counter, with the new total
// as CounterAmount, and Mode$ CounterAddedOnce once for the whole placement.
// placer is the player putting them, Card.addCounterInternal's own
// `Player source` (Card.java:1761): what the triggers' ValidSource$ and the
// turn's counter record read (AbilityKey.Source, Card.java:1805;
// Game.addCounterAddedThisTurn, Card.java:1792). A caller that runs the
// AddCounter replacements first (countersReplaced) gives them the same
// player, so the replacement and the trigger agree on who placed. source is the card the counters came from, for
// the event (NoCard for a turn-based action). n <= 0 does nothing.
func (g *Game) addCardCounters(controller PlayerController, placer PlayerID, source CardID, id CardID, ct CounterType, n int) {
	if n <= 0 {
		return
	}
	c := g.Card(id)
	old := c.Counters.Count(ct)
	// Card.addCounterInternal (Card.java:1760): a Mode$ MaxCounter static
	// caps the new total, CR 704.5r's Dream counters.
	if limit, capped := g.maxCounter(id, ct); capped {
		n = min(n, limit-old)
		if n <= 0 {
			return
		}
	}
	c.Counters.Add(ct, n)
	g.recordCountersAdded(placer, id, ct, n)
	emitCounterChanged(g, source, CardEntity(id), ct, n)
	var matches []Ability
	for i := 1; i <= n; i++ {
		matches = g.counterAddedMatches(matches, "CounterAdded", placer, id, ct, old+i)
	}
	matches = g.counterAddedMatches(matches, "CounterAddedOnce", placer, id, ct, n)
	g.pushTriggeredAbilities(controller, matches)
}

// counterAddedParams are the params this port reads on each mode; a trigger
// naming any other is skipped rather than fired without its condition (GO-7).
// The general ones (TriggerZones$, IsPresent$, OptionalDecider$, ...) are read
// by triggerEffectAPI.
var counterAddedParams = map[string][]string{
	"CounterAdded":     {"ValidCard", "ValidSource", "CounterType", "CounterAmount"},
	"CounterAddedOnce": {"ValidCard", "ValidSource", "CounterType"},
}

// counterAddedMatches appends every CounterAdded or CounterAddedOnce trigger
// (mode) that placer's placement of amount counters (the new total for
// CounterAdded) on card id fires. The placer is matched, never recorded:
// TriggerCounterAdded.java:97-98 and TriggerCounterAddedOnce.java:92-94 keep
// only Card and Player (and Amount) as triggering objects.
func (g *Game) counterAddedMatches(matches []Ability, mode string, placer PlayerID, id CardID, ct CounterType, amount int) []Ability {
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for face := range h.triggerFaces {
				for _, t := range face.Triggers {
					if !strings.EqualFold(t.Name, mode) || !counterAddedTriggerReadable(t, mode) {
						continue
					}
					if !g.counterAddedTriggerMatches(t, h, host, placer, id, ct, amount) {
						continue
					}
					objects := triggeredObjects{card: id}
					if mode == "CounterAddedOnce" {
						objects.counts = triggerCounts{amount: amount, set: countAmount}
					}
					if sub, api, optional, ok := triggerEffectAPI(g, h, face.Amounts, t); ok {
						matches = append(matches, Ability{API: api, Source: host, Controller: h.Controller(), Params: sub, Amounts: face.Amounts, Optional: optional,
							triggered: face.objects(objects), staticTrigger: isStaticTrigger(t)})
					}
				}
			}
		}
	}
	return matches
}

// counterAddedTriggerReadable reports whether every param of t is one this
// port evaluates for mode or one the general trigger gates read.
func counterAddedTriggerReadable(t *compile.Ability, mode string) bool {
	for _, p := range t.Params {
		switch strings.ToLower(p.Key) {
		case "mode", "execute", "triggerdescription", "secondary", "triggerzones", "chapter",
			"optionaldecider", "ispresent", "presentcompare", "presentzone", "presentplayer",
			"checksvar", "svarcompare", "playerturn", "phase", "opponentturn", "notplayerturn":
			continue
		}
		known := false
		for _, k := range counterAddedParams[mode] {
			known = known || strings.EqualFold(p.Key, k)
		}
		if !known {
			return false
		}
	}
	return true
}

// counterAddedTriggerMatches is TriggerCounterAdded.performTest
// (TriggerCounterAdded.java:57-90) and TriggerCounterAddedOnce's
// (TriggerCounterAddedOnce.java:58-89) over the params counterAddedParams
// lists. ValidSource$ is matched against the placer, AbilityKey.Source, a
// Player (Card.java:1805): every corpus value is "You". No placer (Java's
// null source) matches nothing, as matchesValidParam fails on null; a player
// spec matchesPlayerSpec cannot read leaves the trigger unfired rather than
// guessed.
func (g *Game) counterAddedTriggerMatches(t *compile.Ability, h *Card, host CardID, placer PlayerID, id CardID, ct CounterType, amount int) bool {
	if vc, ok := t.Param("ValidCard"); ok && !Matches(g, g.Card(id), valid.Parse(vc), h.Controller(), host) {
		return false
	}
	if vs, ok := t.Param("ValidSource"); ok {
		if placer == NoPlayer {
			return false
		}
		if matched, recognized := matchesPlayerSpec(g, placer, h.Controller(), host, vs); !recognized || !matched {
			return false
		}
	}
	if want, ok := t.Param("CounterType"); ok && !strings.EqualFold(want, string(ct)) {
		return false
	}
	if cmp, ok := t.Param("CounterAmount"); ok {
		// "EQ2": Expressions.compare of the new total against the operand.
		if len(cmp) < 3 {
			return false
		}
		operand, err := strconv.Atoi(cmp[2:])
		if err != nil || !compareOp(amount, cmp[:2], operand) {
			return false
		}
	}
	return true
}
