// "Enters with counters": the Event$ Moved replacements that put counters on a
// permanent as it enters the battlefield (CR 614.1c, CR 122.6), written as a
// ReplaceWith$ ability `DB$ PutCounter | ETB$ True`. CardFactoryUtil's
// makeEtbCounter builds the same shape for `K:etbCounter:<type>:<amount>`
// (keyword.Expand, ADR-0038).
//
// Java's PutCounterEffect with ETB$ puts the counters into the replaced
// event's counter table, which the entering permanent receives as it enters;
// here they are added right after the move, before any "enters" trigger looks
// at the permanent (enterBattlefieldReplacements), through countersReplaced so
// an AddCounter replacement (Hardened Scales) still changes them.

//enginelint:allow id zone card game valid ability replacement replaceeffect amount putcountereffect control event parts effecthelpers defined

package engine

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// applyEnterCounters applies every counter-adding Moved replacement that
// matches moved's entry onto the battlefield from origin: moved's own
// ("CARDNAME enters with N counters", Defined$ Self) and another permanent's
// ("each creature you control enters with an additional counter", Defined$
// ReplacedCard). Unlike the tap shape, each of them applies, since counters
// from separate effects add up (CR 616.1's choice only orders them).
//
// A line this port cannot evaluate records a pending error rather than
// entering without the counters (ADR-0020 decision 4, GO-7): a PutCounter with
// a param past Defined$/CounterType$/CounterNum$/ETB$, a Defined$ other than
// Self or ReplacedCard, an amount that does not resolve.
func (g *Game) applyEnterCounters(controller PlayerController, moved CardID, origin ZoneType) {
	movedCard := g.Card(moved)
	// Java puts every ETB$ counter into one counter table and runs the
	// AddCounter replacements on it once, so the amounts of one kind are summed
	// first (Hardened Scales adds one for the lot, not one per line).
	var piles []enterPile
	addPile := func(p enterPile) {
		for i := range piles {
			if piles[i].ct == p.ct {
				piles[i].n += p.n
				return
			}
		}
		piles = append(piles, p)
	}
	// Replacements whose ReplaceWith$ chain goes on past the PutCounter (an
	// Effect card's "enters with an additional counter", then exiles itself:
	// Spark Double, Moritte of the Frost). They resolve after the walk, since
	// the chain may exile the very card the walk is standing on.
	type chain struct {
		host    *Card
		amounts map[string]expr.Amount
		sub     *compile.Ability
	}
	var chains []chain
	g.eachReplacement("Moved", func(h *Card, amounts map[string]expr.Amount, r *compile.Ability) bool {
		sub := replaceWithSub(r)
		if sub == nil || !strings.EqualFold(sub.Name, "PutCounter") {
			return false
		}
		if _, ok := sub.Param("ETB"); !ok {
			return false
		}
		if !replacementZoneMatches(r, "Destination", Battlefield) || !replacementZoneMatches(r, "Origin", origin) {
			return false
		}
		validCard, ok := r.Param("ValidCard")
		if !ok {
			// ReplaceMoved matches any card with no ValidCard$; no corpus line
			// writes that for counters, so it is not guessed at.
			g.recordPendingError(fmt.Errorf("engine: %q: ETB$ PutCounter replacement without ValidCard$ not resolvable yet", h.Def.Name))
			return false
		}
		if !Matches(g, movedCard, valid.Parse(validCard), h.Controller(), h.ID) {
			return false
		}
		if !replacementRequirementsCheck(g, h, amounts, r) {
			return false
		}
		if !onlyParams(r, "validcard", "destination", "origin", "replacementresult", "layer",
			// the flags triggerCommonRequirementsMet evaluates
			"bloodthirst", "metalcraft", "delirium", "threshold", "hellbent", "fatefulhour", "lifetotal", "lifeamount", "revolt",
			"ispresent2", "presentcompare2", "presentzone2", "presentplayer2", "presentdefined2",
			"adamant", "manaspent", "mananotspent", "checkdefinedplayer", "definedplayercompare", "enduringstory") {
			// Optional$ and the like: counters it may decline are not put
			// unconditionally.
			g.recordPendingError(fmt.Errorf("engine: %q: ETB$ PutCounter replacement: a param past ValidCard$/Destination$/Origin$ not resolvable yet", h.Def.Name))
			return false
		}
		if chainNext(sub) != nil {
			chains = append(chains, chain{host: h, amounts: amounts, sub: sub})
			return false
		}
		ct, n, err := g.enterCounterAmount(moved, h, amounts, sub)
		if err != nil {
			g.recordPendingError(err)
			return false
		}
		if n <= 0 {
			return false
		}
		addPile(enterPile{ct: ct, n: n, source: h.ID, placer: h.Controller()})
		return false
	})
	for _, c := range chains {
		if err := g.runEnterChain(controller, moved, c.host, c.amounts, c.sub); err != nil {
			g.recordPendingError(err)
		}
	}
	// What an entry replacement's own chain put on the card (putEnterCounters:
	// a Copy-layer chain before the move, an effect card's chain just above)
	// joins the same table.
	for _, p := range movedCard.pendingEnter {
		addPile(p)
	}
	movedCard.pendingEnter = nil
	for _, p := range piles {
		if n := g.countersReplaced(controller, p.placer, CardEntity(moved), p.ct, p.n); n > 0 {
			g.addCardCounters(controller, p.placer, p.source, moved, p.ct, n)
		}
	}
}

// runEnterChain resolves one counter replacement's whole ReplaceWith$ chain
// through the Registry, host's controller activating it and moved the
// replacing card: the PutCounters in it deposit on moved (putEnterCounters)
// and a ChangeZone of the host out of the Command zone ends an effect card.
func (g *Game) runEnterChain(controller PlayerController, moved CardID, host *Card, amounts map[string]expr.Amount, sub *compile.Ability) error {
	api, ok := APIByName(sub.Name)
	if !ok || g.registry == nil {
		return fmt.Errorf("engine: %q: ETB$ %s chain has no registered effect", host.Def.Name, sub.Name)
	}
	name := host.Def.Name
	a := Ability{
		API: api, Source: host.ID, Controller: host.Controller(), Params: sub, Amounts: amounts,
		replacing: &replacementEvent{result: replacementUpdated, card: moved},
	}
	if err := g.registry.Resolve(g, &a, controller); err != nil {
		return fmt.Errorf("engine: %q: ETB$ counter chain: %w", name, err)
	}
	return nil
}

// putEnterCounters is a PutCounter with ETB$ resolving inside a Moved
// replacement's ReplaceWith$ chain (a Copy-layer chain: Altered Ego, Undercover
// Operative, The Mimeoplasm, Dominion Saboteur, Spark Double's effect): the
// counters go into the replaced event's counter table
// (CountersPutEffect.java:365, :439, :536), which the permanent receives as it
// enters. The card is not on the battlefield yet, so they wait on it
// (Card.pendingEnter) for applyEnterCounters, which adds them through the
// AddCounter replacements with the entry's other counters.
//
// The amount is read now, while the chain's remembered cards are still there
// (The Mimeoplasm's Remembered$CardPower, before its DBCleanup). Defined$
// names the entering card -- Self for its own line, ReplacedCard or
// ReplacedNewCard for a watcher's, optionally filtered by a ".<valid>" suffix
// (AbilityUtils.getDefinedCards' incR) -- and anything else is an error
// rather than counters put on a card that is not entering (GO-7).
//
// CounterType$ EachFromSource with EachFromSource$ is "the same number and
// kinds of counters as" the named cards (Dominion Saboteur): one pile per
// kind each of them has, CounterNum$ overriding the count. placer is the
// PutCounter's placer (putCounterPlacer), each pile's table key.
func (g *Game) putEnterCounters(a *Ability, source *Card, placer PlayerID) error {
	moved := a.replacedCard()
	defined := "Self"
	if d, ok := a.Params.Param("Defined"); ok {
		defined = d
	}
	base, filter, _ := strings.Cut(defined, ".")
	switch base {
	case "Self":
		if source.ID != moved {
			return fmt.Errorf("engine: %q: PutCounter ETB$ Defined$ Self on a card other than the entering one not resolvable yet", source.Def.Name)
		}
	case "ReplacedCard", "ReplacedNewCard":
	default:
		return fmt.Errorf("engine: %q: PutCounter ETB$ Defined$ %q not resolvable yet", source.Def.Name, defined)
	}
	card := g.Card(moved)
	if filter != "" && !Matches(g, card, valid.Parse(filter), a.Controller, a.Source) {
		return nil
	}
	put := func(ct CounterType, n int) {
		if n <= 0 {
			return
		}
		card.pendingEnter = append(append([]enterPile(nil), card.pendingEnter...),
			enterPile{ct: ct, n: n, source: a.Source, placer: placer})
	}
	counterNum, hasNum := a.Params.Param("CounterNum")
	n := 0
	if hasNum || !hasParam(a, "EachFromSource") {
		if !hasNum {
			counterNum = "1"
		}
		var ok bool
		if n, ok = resolveNamedAmount(g, a.Amounts, source, counterNum); !ok {
			return fmt.Errorf("engine: PutCounter: CounterNum$ %q is not resolvable", counterNum)
		}
	}
	if raw, ok := a.Params.Param("EachFromSource"); ok {
		cards, err := definedCards(source, raw, a.refs())
		if err != nil {
			return fmt.Errorf("engine: PutCounter: %w", err)
		}
		for _, id := range cards {
			c := g.Card(id)
			for _, ct := range c.Counters.Kinds() {
				if hasNum {
					put(ct, n)
				} else {
					put(ct, c.Counters.Count(ct))
				}
			}
		}
		return nil
	}
	ct, err := putCounterType(a.Params)
	if err != nil {
		return err
	}
	put(ct, n)
	return nil
}

// enterCounterAmount reads one ETB$ PutCounter's counter kind and amount for
// moved, 0 amount for a line that puts nothing on it (a watcher's own "Self").
func (g *Game) enterCounterAmount(moved CardID, host *Card, amounts map[string]expr.Amount, sub *compile.Ability) (CounterType, int, error) {
	name := host.Def.Name
	if !onlyKeys(sub, "DB", "Defined", "CounterType", "CounterNum", "ETB", "SpellDescription") {
		return "", 0, fmt.Errorf("engine: %q: ETB$ PutCounter: a param past Defined$/CounterType$/CounterNum$ not resolvable yet", name)
	}
	defined, _ := sub.Param("Defined")
	switch {
	case defined == "Self" && host.ID == moved, defined == "ReplacedCard":
	case defined == "Self":
		// A watcher's own "Self" is the watcher, not what entered.
		return "", 0, nil
	default:
		return "", 0, fmt.Errorf("engine: %q: ETB$ PutCounter: Defined$ %q not resolvable yet", name, defined)
	}
	counterType, err := putCounterType(sub)
	if err != nil {
		return "", 0, fmt.Errorf("engine: %q: %w", name, err)
	}
	counterNum, ok := sub.Param("CounterNum")
	if !ok {
		counterNum = "1"
	}
	amount, ok := resolveNamedAmount(g, amounts, host, counterNum)
	if !ok {
		return "", 0, fmt.Errorf("engine: %q: ETB$ PutCounter: CounterNum$ %q is not resolvable", name, counterNum)
	}
	return counterType, amount, nil
}
