// Spell cost modification (CR 601.2f): Mode$ RaiseCost and Mode$ ReduceCost
// statics, CostAdjustment.getSpellCostChange (raise) and CostAdjustment.adjust
// (reduce) for Type$ Spell lines.

//enginelint:allow id zone card game valid player staticability amount parts zonemove

package engine

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// costModParams are the params a RaiseCost/ReduceCost line may carry that this
// port evaluates (Relative$ on the spell's own line only, costModStatus); any
// other (ValidTarget$, Color$, UpTo$,
// OnlyFirstSpell$, ValidSpell$, ForEachShard$, a keyword Amount$ ...) makes the
// line unresolvable and it is not applied (GO-7).
var costModParams = map[string]bool{
	"mode": true, "validcard": true, "activator": true, "type": true, "affectedzone": true,
	"amount": true, "cost": true, "minmana": true, "effectzone": true,
	"condition": true, "phases": true, "playerturn": true, "relative": true,
	"description": true, "secondary": true, "spelldescription": true, "stackdescription": true,
}

// spellCost is base, the printed (or alternative) mana cost of casting card as
// pid, after every RaiseCost static (their Cost$, default {1}, added Amount$
// times) and then every ReduceCost static (generic mana only, down to
// MinMana$ -- CostAdjustment.applyReduceCostAbility's cap on total reduction
// against the converted cost). Hosts are the battlefield and Command-zone
// statics and the card itself (EffectZone$ All). An X in the cost is
// untouched: only the generic part shrinks.
//
// paying is the cost actually paid (payCastCost), after the X is announced and
// the targets chosen, which a Relative$ amount may read; a ReduceCost Amount$
// that does not resolve then refuses the cast. The probe before the announce
// (announceXFor, which only asks whether an X is left) skips such a line: a
// reduction never touches the X.
func (g *Game) spellCost(pid PlayerID, card CardID, base mana.Cost, paying bool) (mana.Cost, bool) {
	if base.IsNoCost() {
		return base, true
	}
	c := g.Card(card)
	generic := base.Generic()
	shards := append([]mana.Shard(nil), base.Shards()...)

	type line struct {
		host *Card
		s    *compile.Ability
		face *compile.Face
	}
	var raises, reduces []line
	for _, host := range g.staticHostsWith(card) {
		h := g.Card(host)
		if h.Def == nil {
			continue
		}
		// The host's live faces only: a modal or transforming card's other face
		// is not its state, and its statics do not apply (Extus, Oriq
		// Overlord's back-face reduction while the creature is cast).
		live := h.liveTraitFaces()
		for i := range live {
			face := &live[i]
			for _, s := range face.Statics {
				switch {
				case strings.EqualFold(s.Name, "RaiseCost"):
					switch g.costModStatus(pid, c, h, s) {
					case costModApplies:
						raises = append(raises, line{h, s, face})
					case costModUnresolvable:
						// A raise this port cannot evaluate: casting for less
						// than Java would charge is wrong, so the cast is
						// refused (GO-7).
						return base, false
					}
				case strings.EqualFold(s.Name, "ReduceCost") && g.costModStatus(pid, c, h, s) == costModApplies:
					reduces = append(reduces, line{h, s, face})
				}
			}
		}
	}
	for _, r := range raises {
		count := 1
		if raw, ok := r.s.Param("Amount"); ok {
			n, ok := costModAmount(g, r.face, r.host, raw)
			if !ok {
				return base, false
			}
			count = n
		}
		text := "1"
		if raw, ok := r.s.Param("Cost"); ok {
			text = raw
		}
		add, err := mana.Parse(text)
		if err != nil || add.CountX() > 0 {
			return base, false
		}
		for range max(count, 0) {
			generic += add.Generic()
			shards = append(shards, add.Shards()...)
		}
	}
	cmc := mana.FromShards(shards, generic).CMC()
	reduced := 0
	for _, r := range reduces {
		raw, ok := r.s.Param("Amount")
		if !ok {
			// calculateAmount of no Amount$ is 0 (CostAdjustment.java:466-479).
			continue
		}
		value, ok := costModAmount(g, r.face, r.host, raw)
		if !ok && !paying {
			continue
		}
		if !ok {
			// Casting for more than Java would charge is as wrong as for less:
			// the cast is refused, and why is kept (GO-7).
			g.recordPendingError(fmt.Errorf("engine: ReduceCost: Amount$ %q is not resolvable", raw))
			return base, false
		}
		minMana := 0
		if raw, ok := r.s.Param("MinMana"); ok {
			minMana, _ = strconv.Atoi(raw)
		}
		if maxReduction := cmc - minMana - reduced; maxReduction > 0 {
			reduced += min(value, maxReduction)
		}
	}
	return mana.FromShards(shards, max(generic-reduced, 0)), true
}

// costModResult is how a RaiseCost/ReduceCost line stands against a cast.
type costModResult int

const (
	costModSkips costModResult = iota
	costModApplies
	// costModUnresolvable: the line names this card, caster and a spell, but
	// carries a param or condition this port does not evaluate.
	costModUnresolvable
)

// costModStatus is CostAdjustment.checkRequirement for a spell: Type$ is Spell,
// ValidCard$ matches the card, Activator$ the caster and AffectedZone$ the
// zone the card is in -- else the line skips. A line that passes those but has a
// param outside costModParams, or whose conditions do not hold, is applied only
// when everything is evaluable; with an unlisted param it is
// costModUnresolvable.
func (g *Game) costModStatus(pid PlayerID, c, host *Card, s *compile.Ability) costModResult {
	if t, ok := s.Param("Type"); !ok || t != "Spell" {
		return costModSkips
	}
	if v, ok := s.Param("ValidCard"); ok && !Matches(g, c, valid.Parse(v), host.Controller(), host.ID) {
		return costModSkips
	}
	if v, ok := s.Param("Activator"); ok {
		matched, recognized := matchesPlayerSpec(g, pid, host.Controller(), host.ID, v)
		if !recognized || !matched {
			return costModSkips
		}
	}
	if v, ok := s.Param("AffectedZone"); ok {
		zones, err := parseZoneList(v)
		if err != nil || !zoneIn(c.Zone, zones) {
			return costModSkips
		}
	}
	// Relative$ reads Amount$ against the spell being cast rather than the
	// static (CostAdjustment.java:155-157). On the spell's own line the host
	// is that spell, which is what the amount is read against here (Bite Down
	// on Crime's Count$OptionalGenericCostPaid); another host's is not read.
	if _, relative := s.Param("Relative"); relative && host.ID != c.ID {
		return costModUnresolvable
	}
	if !paramsResolvable(s, costModParams) {
		return costModUnresolvable
	}
	if !g.staticConditionsMet(host, s) {
		return costModSkips
	}
	return costModApplies
}

// costModAmount is a line's Amount$: a plain number, or an SVar of the host's
// face resolveNamedAmount evaluates; false when it cannot.
func costModAmount(g *Game, face *compile.Face, host *Card, raw string) (int, bool) {
	if n, err := strconv.Atoi(raw); err == nil {
		return n, true
	}
	return resolveNamedAmount(g, face.Amounts, host, raw)
}
