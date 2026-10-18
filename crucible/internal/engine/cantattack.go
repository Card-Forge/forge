// Mode$ CantAttack and the Defender keyword
// (StaticAbilityCantAttackBlock.cantAttack/applyCantAttackAbility), with
// Mode$ CanAttackDefender ("can attack as though it didn't have defender").

//enginelint:allow id zone card game valid player staticability zonemove attack combat

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

var (
	cantAttackParams = map[string]bool{
		"mode": true, "validcard": true, "target": true, "unlessdefender": true, "effectzone": true,
		"condition": true, "phases": true, "playerturn": true,
		"description": true, "secondary": true, "spelldescription": true, "stackdescription": true,
	}
	canAttackDefenderParams = map[string]bool{
		"mode": true, "validcard": true, "validattacked": true, "effectzone": true,
		"condition": true, "phases": true, "playerturn": true,
		"description": true, "secondary": true, "spelldescription": true, "stackdescription": true,
	}
)

// attackableTargets keeps the targets of eligible that attacker may attack
// (CombatUtil.canAttack(attacker, defender)'s cantAttack half).
func (g *Game) attackableTargets(attacker CardID, eligible []EntityID) []EntityID {
	out := make([]EntityID, 0, len(eligible))
	for _, t := range eligible {
		if !g.cantAttack(attacker, t) {
			out = append(out, t)
		}
	}
	return out
}

// cantAttack is StaticAbilityCantAttackBlock.cantAttack for one defender: the
// Defender keyword (a synthesized CantAttack static in Java, lifted by a
// CanAttackDefender static), or a Mode$ CantAttack static naming the attacker
// and the defender. The keyword texts and detention are canAttackAtAll's.
func (g *Game) cantAttack(attacker CardID, target EntityID) bool {
	c := g.Card(attacker)
	if c.HasKeyword("Defender") && !g.canAttackDefender(c, target) {
		return true
	}
	for _, p := range g.Players() {
		for _, host := range g.traitHosts(p) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.traitFaces() {
				for _, s := range face.Statics {
					if strings.EqualFold(s.Name, "CantAttack") && g.cantAttackApplies(c, target, h, s) {
						return true
					}
				}
			}
		}
	}
	return false
}

func (g *Game) cantAttackApplies(c *Card, target EntityID, host *Card, s *compile.Ability) bool {
	if !paramsResolvable(s, cantAttackParams) || !g.staticConditionsMet(host, s) {
		return false
	}
	if v, ok := s.Param("ValidCard"); ok && !Matches(g, c, valid.Parse(v), host.Controller(), host.ID) {
		return false
	}
	if v, ok := s.Param("Target"); ok && !g.attackTargetMatches(target, v, host) {
		return false
	}
	if v, ok := s.Param("UnlessDefender"); ok {
		defender, ok := g.defenderPlayerOf(target)
		if !ok {
			return false
		}
		// "Player." + property is what Player.hasProperty reads; an
		// unrecognized property leaves the line unapplied (GO-7).
		matched, recognized := matchesPlayerSpec(g, defender, host.Controller(), host.ID, "Player."+v)
		if !recognized || matched {
			return false
		}
	}
	return true
}

// attackTargetMatches is matchesValidParam("Target"/"ValidAttacked", target)
// for a comma list of alternatives: a player target matches a player
// alternative (matchesPlayerSpec), a planeswalker or battle a card
// alternative (Matches). An alternative this port cannot recognize does not
// match.
func (g *Game) attackTargetMatches(target EntityID, spec string, host *Card) bool {
	if pid, ok := target.AsPlayer(); ok {
		matched, recognized := matchesPlayerSpec(g, pid, host.Controller(), host.ID, spec)
		return recognized && matched
	}
	cid, ok := target.AsCard()
	if !ok {
		return false
	}
	for _, alt := range strings.Split(spec, ",") {
		base, _, _ := strings.Cut(alt, ".")
		if _, isPlayerBase := matchesPlayerBase(NoPlayer, NoPlayer, base); isPlayerBase {
			continue
		}
		if Matches(g, g.Card(cid), valid.Parse(alt), host.Controller(), host.ID) {
			return true
		}
	}
	return false
}

// canAttackDefender is StaticAbilityCantAttackBlock.canAttackDefender: some
// CanAttackDefender static names the creature (ValidCard$) and the defender
// (ValidAttacked$), so it attacks as though it had no defender.
func (g *Game) canAttackDefender(c *Card, target EntityID) bool {
	for _, p := range g.Players() {
		for _, host := range g.traitHosts(p) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.traitFaces() {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, "CanAttackDefender") || !paramsResolvable(s, canAttackDefenderParams) || !g.staticConditionsMet(h, s) {
						continue
					}
					if v, ok := s.Param("ValidCard"); ok && !Matches(g, c, valid.Parse(v), h.Controller(), h.ID) {
						continue
					}
					if v, ok := s.Param("ValidAttacked"); ok && !g.attackTargetMatches(target, v, h) {
						continue
					}
					return true
				}
			}
		}
	}
	return false
}

var cantBlockParams = map[string]bool{
	"mode": true, "validcard": true, "effectzone": true,
	"condition": true, "phases": true, "playerturn": true,
	"description": true, "secondary": true, "spelldescription": true, "stackdescription": true,
}

// cantBlock is StaticAbilityCantAttackBlock.cantBlock for the statics:
// some Mode$ CantBlock static names the blocker (ValidCard$ matches it; an
// absent ValidCard$ names every creature). Hosts are the blocker itself and
// the static-ability source zones. Detention, suspicion and the keyword texts
// are canBlockAtAll's own. A line with an unlisted param (IsPresent$,
// CheckSVar$ ...) is not applied (GO-7).
func (g *Game) cantBlock(blocker CardID) bool {
	b := g.Card(blocker)
	for _, host := range g.staticHostsWith(blocker) {
		h := g.Card(host)
		if h.Def == nil {
			continue
		}
		for _, face := range h.traitFaces() {
			for _, s := range face.Statics {
				if !strings.EqualFold(s.Name, "CantBlock") || !paramsResolvable(s, cantBlockParams) || !g.staticConditionsMet(h, s) {
					continue
				}
				if v, ok := s.Param("ValidCard"); !ok || Matches(g, b, valid.Parse(v), h.Controller(), h.ID) {
					return true
				}
			}
		}
	}
	return false
}
