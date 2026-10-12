package smoke

import (
	"strings"
	"sync"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/valid"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// targetSpec is one comma alternative of a ValidTgts$ the card names, with the
// zone its targets live in.
type targetSpec struct {
	alt  string
	zone engine.ZoneType
	// spell is TargetType$ Spell: the target is a spell on the stack, so the
	// card is cast rather than placed.
	spell bool
}

// tailorKey keys the candidate cache: the spec and zone, and whether the
// candidate belongs to the probe player (YouCtrl, YouOwn) or the opponent.
type tailorKey struct {
	spec targetSpec
	mine bool
}

// tailor finds, for a ValidTgts$ no generic board satisfies, a corpus card the
// spec accepts. Candidates are cached across cards: "Creature.Legendary" is
// searched for once.
type tailor struct {
	found sync.Map // tailorKey -> string
}

// targetSpecs lists the ValidTgts$ alternatives of def's spell and activated
// abilities and their sub-abilities. Player-only alternatives are left out:
// both players are always present.
func targetSpecs(def *compile.Card) []targetSpec {
	var out []targetSpec
	seen := map[targetSpec]bool{}
	visited := map[*compile.Ability]bool{}
	var walk func(a *compile.Ability)
	walk = func(a *compile.Ability) {
		if a == nil || visited[a] {
			return
		}
		visited[a] = true
		if v, ok := a.Param("ValidTgts"); ok {
			zone := targetZone(a)
			tt, _ := a.Param("TargetType")
			spell := strings.HasPrefix(tt, "Spell")
			for _, alt := range strings.Split(v, ",") {
				alt = strings.TrimSpace(alt)
				switch {
				case alt == "", alt == "Any", alt == "You", strings.HasPrefix(alt, "Player"), strings.HasPrefix(alt, "Opponent"):
					continue
				}
				ts := targetSpec{alt: alt, zone: zone, spell: spell}
				if !seen[ts] {
					seen[ts] = true
					out = append(out, ts)
				}
			}
		}
		for _, s := range a.Subs {
			walk(s.Ability)
		}
	}
	for _, a := range def.Faces[0].Abilities {
		walk(a)
	}
	// An Aura's host is an Enchant keyword spec the generic board may not
	// satisfy ("Creature.Black"). A spec naming a zone is searched there, and
	// the fixed board already has a creature card in each graveyard.
	for _, line := range def.Faces[0].Keywords {
		k := keyword.Parse(line)
		if k.Name != "Enchant" {
			continue
		}
		spec, _, _ := strings.Cut(k.Details, ":")
		if spec == "" || spec == "Player" || spec == "Opponent" || strings.Contains(spec, "inZone") {
			continue
		}
		if ts := (targetSpec{alt: spec, zone: engine.Battlefield}); !seen[ts] {
			seen[ts] = true
			out = append(out, ts)
		}
	}
	return out
}

// targetZone is the zone a's targets are chosen from: TgtZone$ or, for a
// ChangeZone, Origin$; the battlefield otherwise.
func targetZone(a *compile.Ability) engine.ZoneType {
	for _, key := range [...]string{"TgtZone", "Origin"} {
		v, ok := a.Param(key)
		switch {
		case !ok:
		case strings.Contains(v, "Graveyard"):
			return engine.Graveyard
		case strings.Contains(v, "Exile"):
			return engine.Exile
		case strings.Contains(v, "Hand"):
			return engine.Hand
		}
	}
	return engine.Battlefield
}

// addTailored puts, for each target spec of def, a card the spec accepts on
// each side that has one, in the zone the spec targets.
func (p *Prober) addTailored(g *engine.Game, pid, opp engine.PlayerID, def *compile.Card) {
	p.addWornAura(g, pid, opp, def)
	for _, ts := range targetSpecs(def) {
		if ts.spell {
			continue
		}
		for _, mine := range [...]bool{true, false} {
			name := p.candidate(ts, mine)
			if name == "" {
				continue
			}
			owner := opp
			if mine {
				owner = pid
			}
			if d, ok := p.db.Card(name); ok {
				g.NewCard(d, owner, ts.zone)
			}
		}
	}
}

// wornAura is the Aura addWornAura attaches to a creature on each side.
const wornAura = "Pacifism"

// addWornAura gives each side a creature wearing an Aura when def's Enchant
// spec asks for one ("Creature.EnchantedBy Aura.Other", Daybreak Coronet; or
// "Creature.modified", Lion Umbra): the
// tailor judges candidates in hand, where no card is enchanted, so it cannot
// find such a host.
func (p *Prober) addWornAura(g *engine.Game, pid, opp engine.PlayerID, def *compile.Card) {
	wanted := false
	for _, line := range def.Faces[0].Keywords {
		k := keyword.Parse(line)
		// "Creature.modified" (Lion Umbra) is also satisfied by a worn Aura.
		wanted = wanted || (k.Name == "Enchant" &&
			(strings.Contains(k.Details, "EnchantedBy") || strings.Contains(k.Details, "modified")))
	}
	auraDef, ok1 := p.db.Card(wornAura)
	bearsDef, ok2 := p.db.Card(bears)
	if !wanted || !ok1 || !ok2 {
		return
	}
	for _, who := range [...]engine.PlayerID{pid, opp} {
		host := g.NewCard(bearsDef, who, engine.Battlefield)
		g.Attach(g.NewCard(auraDef, who, engine.Battlefield), host)
	}
}

// stackTailored casts, for each TargetType$ Spell target spec of def, a card
// the spec accepts, leaving it on the stack. The probe player casts it: a
// permanent spell can only be cast at sorcery speed.
func (p *Prober) stackTailored(g *engine.Game, pid engine.PlayerID, def *compile.Card, ctl engine.PlayerController) {
	for _, ts := range targetSpecs(def) {
		if !ts.spell {
			continue
		}
		name := p.candidate(ts, true)
		d, ok := p.db.Card(name)
		if !ok {
			continue
		}
		g.CastSpell(pid, g.NewCard(d, pid, engine.Hand), ctl)
		return
	}
}

// candidate is the corpus card with the fewest abilities that ts accepts for
// the given side, or "" for none. It is judged in a scratch game with the card
// in hand, so a spec that reads game state (attacking, entered this turn)
// finds nothing.
func (p *Prober) candidate(ts targetSpec, mine bool) string {
	key := tailorKey{ts, mine}
	if v, ok := p.tailor.found.Load(key); ok {
		return v.(string)
	}
	name := p.search(ts, mine)
	p.tailor.found.Store(key, name)
	return name
}

func (p *Prober) search(ts targetSpec, mine bool) string {
	landDef, ok := p.db.Card(land)
	if !ok {
		return ""
	}
	g := engine.NewGame(p.db, javarand.New(1), []string{"a", "b"})
	pid, opp := g.Players()[0], g.Players()[1]
	owner := opp
	if mine {
		owner = pid
	}
	src := g.NewCard(landDef, pid, engine.Hand)
	spec := valid.Parse(ts.alt)
	best, bestScore := "", 1<<30
	for _, n := range p.db.Names() {
		d, ok := p.db.Card(n)
		if !ok || (ts.spell && d.Faces[0].Type.Has(cardtype.Land)) {
			continue
		}
		id := g.NewCard(d, owner, engine.Hand)
		if !engine.Matches(g, g.Card(id), spec, pid, src) {
			continue
		}
		if score := len(d.Faces[0].Abilities); score < bestScore {
			best, bestScore = n, score
			if score == 0 {
				break
			}
		}
	}
	return best
}
