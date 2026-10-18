// Attack legality: CR 508.1c-d's "obey as many requirements as possible"
// check on a declaration of attackers (ADR-0024 Decision 1).
//
// Ported from forge-game/src/main/java/forge/game/combat/AttackConstraints.java
// (getLegalAttackers, collectLegalAttackers, getSortedFilteredRequirements,
// countViolations), AttackRequirement.java (constructor, countViolations,
// getSortedRequirements), CombatUtil.validateAttackers (CombatUtil.java:82)
// and StaticAbilityMustAttack.entitiesMustAttack.
//
// Java's AttackConstraints also carries restrictions this port has no
// source for: AttackRestriction's types (NEED_TWO_OTHERS, NOT_ALONE,
// ONLY_ALONE, NEED_BLACK_OR_GREEN, NEED_GREATER_POWER -- "can't attack
// alone" keywords and Mode$ CantAttack statics), GlobalAttackRestrictions'
// maximum attacker counts and the requirements of Mode$ AttackRequirement
// and Mode$ PlayerMustAttack. Attack costs (Propaganda, Mode$
// CantAttackUnless, attackcost.go) are ported: legalAttack skips a costed
// pair. With the rest absent, getLegalAttackers' search collapses
// to its unrestricted path: every creature with a requirement attacks the
// defender it has the most requirements toward, and the search's branches
// that only a restriction or a maximum reaches (the ONLY_ALONE loop,
// isLimited's "try both with and without", predicate reservations) are not
// ported. A static of the two unported requirement modes in play is an
// error rather than a requirement silently not counted (GO-7).

package engine

import (
	"fmt"
	"sort"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// attackRequirement is AttackRequirement.java for one creature: how many
// requirements it has toward each possible defender (defenderSpecific,
// insertion-ordered like Java's LinkedHashMap).
type attackRequirement struct {
	attacker  CardID
	defenders []EntityID
	counts    []int
}

// add is defenderSpecific.merge(e, n, Integer::sum).
func (r *attackRequirement) add(e EntityID, n int) {
	for i, d := range r.defenders {
		if d == e {
			r.counts[i] += n
			return
		}
	}
	r.defenders = append(r.defenders, e)
	r.counts = append(r.counts, n)
}

// count is defenderSpecific.getOrDefault(e, 0).
func (r *attackRequirement) count(e EntityID) int {
	for i, d := range r.defenders {
		if d == e {
			return r.counts[i]
		}
	}
	return 0
}

// violations is AttackRequirement.countViolations: every requirement it
// has, less those toward the defender it attacks (ok false: it stays home).
// Java's causesToAttack half (Mode$ AttackRequirement) is not ported.
func (r *attackRequirement) violations(defender EntityID, attacking bool) int {
	total := 0
	for _, n := range r.counts {
		total += n
	}
	if total == 0 {
		// hasRequirement() is false: no value above zero.
		return 0
	}
	if attacking {
		total -= r.count(defender)
	}
	return total
}

// newAttackRequirement is AttackRequirement's constructor: one requirement
// toward every possible defender per distinct goading player (CR 701.15b,
// AttackRequirement.java:36-38 -- Card.getGoaded is a deduplicating
// PlayerCollection) and per "attacks each combat if able" Mode$ MustAttack
// static, one toward the named entity per Mode$ MustAttack static naming
// one (MustAttack$), then every entry naming a player no longer in the game
// or a card that is no longer an opposing planeswalker or a battle removed.
func (g *Game) newAttackRequirement(attacker CardID, possibleDefenders []EntityID) (attackRequirement, error) {
	r := attackRequirement{attacker: attacker}
	anything := len(g.Card(attacker).goaders())
	mustAttack, err := g.entitiesMustAttack(attacker)
	if err != nil {
		return r, err
	}
	for _, e := range mustAttack {
		if e == CardEntity(attacker) {
			anything++
		} else {
			r.add(e, 1)
		}
	}
	for _, d := range possibleDefenders {
		r.add(d, anything)
	}
	var defenders []EntityID
	var counts []int
	for i, e := range r.defenders {
		if g.attackRequirementEntityGone(attacker, e) {
			continue
		}
		defenders = append(defenders, e)
		counts = append(counts, r.counts[i])
	}
	r.defenders, r.counts = defenders, counts
	return r, nil
}

// attackRequirementEntityGone is AttackRequirement's removal test: a player
// who lost, or a card that is not on the battlefield, whose controller
// lost, or that is neither a battle nor controlled by an opponent of
// attacker's controller.
func (g *Game) attackRequirementEntityGone(attacker CardID, e EntityID) bool {
	if pid, ok := e.AsPlayer(); ok {
		return g.Player(pid).Lost
	}
	cid, ok := e.AsCard()
	if !ok {
		return true
	}
	c := g.Card(cid)
	if c.Zone != Battlefield || g.Player(c.Controller()).Lost {
		return true
	}
	return !c.Type().Has(cardtype.Battle) && c.Controller() == g.Card(attacker).Controller()
}

// goaders is Card.getGoaded: the distinct players goading c, in goad order.
func (c *Card) goaders() []PlayerID {
	var out []PlayerID
	for _, gd := range c.goadedBy {
		if !containsPlayer(out, gd.By) {
			out = append(out, gd.By)
		}
	}
	return out
}

// entitiesMustAttack is StaticAbilityMustAttack.entitiesMustAttack: for
// every Mode$ MustAttack static in play whose ValidCreature$ matches
// attacker, the entities its MustAttack$ names -- less any the active
// player controls or is (CR 506.2) -- or attacker itself when it names
// none ("attacks each combat if able"). Hosts are traitHosts: the
// battlefield and effect cards in the Command zone, where an effect's
// "target creature attacks this turn if able" lives (DB$ Effect |
// StaticAbilities$ MustAttack).
func (g *Game) entitiesMustAttack(attacker CardID) ([]EntityID, error) {
	var out []EntityID
	err := g.eachCombatStatic("MustAttack", func(h *Card, _ *compile.Face, s *compile.Ability) error {
		if !staticValidMatches(g, h, s, "ValidCreature", attacker) {
			return nil
		}
		spec, ok := s.Param("MustAttack")
		if !ok {
			out = append(out, CardEntity(attacker))
			return nil
		}
		defs, err := definedEntities(g, h.Controller(), h, spec, abilityRefs{})
		if err != nil {
			return fmt.Errorf("engine: Mode$ MustAttack: MustAttack$: %w", err)
		}
		for _, e := range defs {
			if pid, ok := e.AsPlayer(); ok && pid == g.activePlayer {
				continue
			}
			if cid, ok := e.AsCard(); ok && g.Card(cid).Controller() == g.activePlayer {
				continue
			}
			out = append(out, e)
		}
		return nil
	})
	return out, err
}

// attackConstraints is AttackConstraints.java over the active player's
// creatures: every one's requirements, in battlefield order.
type attackConstraints struct {
	requirements []attackRequirement
}

// newAttackConstraints is AttackConstraints' constructor. Every creature
// the active player controls is a possible attacker, tapped or not, the
// way Java's getCreaturesInPlay is: an unavoidable violation (a tapped
// creature that must attack) counts toward the declared attack and the best
// possible one alike, so it cancels out.
func (g *Game) newAttackConstraints() (attackConstraints, error) {
	if err := g.rejectUnportedAttackRequirements(); err != nil {
		return attackConstraints{}, err
	}
	defenders := g.eligibleAttackTargets()
	var ac attackConstraints
	for _, id := range g.creaturesInPlay(g.activePlayer) {
		r, err := g.newAttackRequirement(id, defenders)
		if err != nil {
			return attackConstraints{}, err
		}
		ac.requirements = append(ac.requirements, r)
	}
	return ac, nil
}

// rejectUnportedAttackRequirements errors on a Mode$ AttackRequirement or
// Mode$ PlayerMustAttack static in play: both add requirements Java's
// validator counts (AttackRequirement's causesToAttack, AttackConstraints'
// playerRequirements) that this port does not model, so validating without
// them would accept declarations Java rejects.
func (g *Game) rejectUnportedAttackRequirements() error {
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.traitFaces() {
				for _, s := range face.Statics {
					if strings.EqualFold(s.Name, "AttackRequirement") || strings.EqualFold(s.Name, "PlayerMustAttack") {
						return fmt.Errorf("engine: Mode$ %s static not resolvable yet", s.Name)
					}
				}
			}
		}
	}
	return nil
}

// countViolations is AttackConstraints.countViolations: the requirements
// attack (attacker -> defender) leaves unmet, summed over every possible
// attacker. Restrictions (Java's -1) are not checked here: an attack that
// breaks one never reaches this -- DeclareCombatAttackers rejects an
// ineligible attacker or an unoffered defender before validating, and
// legalAttack only builds attacks from eligible pairs.
func (ac attackConstraints) countViolations(attack map[CardID]EntityID) int {
	n := 0
	for i := range ac.requirements {
		r := &ac.requirements[i]
		d, attacking := attack[r.attacker]
		n += r.violations(d, attacking)
	}
	return n
}

// attackOption is AttackConstraints.Attack: one creature attacking one
// defender, and how many of its requirements that meets.
type attackOption struct {
	attacker     CardID
	defender     EntityID
	requirements int
}

// sortedFilteredRequirements is getSortedFilteredRequirements: every
// (creature, defender) pair the creature can legally attack, each
// creature's pairs in ascending requirement order (getSortedRequirements,
// a stable sort), then the whole list stably sorted descending
// (Comparator.reverseOrder over a TimSort). canAttack(attacker, defender)
// is the restriction side this port has: eligible, and not a goader while
// another player can be attacked (goadTargets).
func (g *Game) sortedFilteredRequirements(ac attackConstraints) []attackOption {
	targets := g.eligibleAttackTargets()
	var out []attackOption
	for _, r := range ac.requirements {
		if !g.canAttackAtAll(r.attacker) {
			continue
		}
		allowed := g.goadTargets(r.attacker, targets)
		var own []attackOption
		for i, d := range r.defenders {
			if containsEntity(allowed, d) {
				own = append(own, attackOption{attacker: r.attacker, defender: d, requirements: r.counts[i]})
			}
		}
		sort.SliceStable(own, func(i, j int) bool { return own[i].requirements < own[j].requirements })
		out = append(out, own...)
	}
	sort.SliceStable(out, func(i, j int) bool { return out[i].requirements > out[j].requirements })
	return out
}

// legalAttack is collectLegalAttackers' unrestricted path (the file's doc
// comment has why no other path is reachable): walk the sorted options,
// skip one that meets no requirement or whose pair has an attack cost
// (AttackConstraints.java:195-197, "has to pay a cost: skip!" -- a zero
// cost too, getAttackCost being non-null), otherwise send its creature at
// its defender and drop the creature's remaining options. A costed pair is
// skipped alone: the creature's other options stay, as Java removes only
// that Attack from reqs.
func (g *Game) legalAttack(options []attackOption) (map[CardID]EntityID, error) {
	attack := map[CardID]EntityID{}
	for _, o := range options {
		if _, done := attack[o.attacker]; done || o.requirements == 0 {
			continue
		}
		_, costed, err := g.attackCost(o.attacker, o.defender)
		if err != nil {
			return nil, err
		}
		if costed {
			continue
		}
		attack[o.attacker] = o.defender
	}
	return attack, nil
}

// attackValidation is a combat's AttackConstraints as Combat.initConstraints
// builds it, once, before anything taps: the requirements and the sorted
// options. PhaseHandler.declareAttackersTurnBasedAction re-validates against
// the same constraints after an attacker is dropped for an unpaid attack cost
// (PhaseHandler.java:585), while the declared attackers sit provisionally
// tapped; rebuilding them then would read every one of those as unable to
// attack (canAttackAtAll's untapped test).
type attackValidation struct {
	ac      attackConstraints
	options []attackOption
}

// newAttackValidation builds the constraints of the current board.
func (g *Game) newAttackValidation() (attackValidation, error) {
	ac, err := g.newAttackConstraints()
	if err != nil {
		return attackValidation{}, err
	}
	return attackValidation{ac: ac, options: g.sortedFilteredRequirements(ac)}, nil
}

// checkAttackers is CombatUtil.validateAttackers (CombatUtil.java:82):
// the declared attack may leave no more requirements unmet than the best
// attack getLegalAttackers finds -- the one collectLegalAttackers builds, or
// nobody attacking, whichever violates fewer. On failure the error names
// every creature the declaration leaves with more unmet requirements than
// the best attack does. Attack costs are read live, as Java's getAttackCost
// call inside collectLegalAttackers is.
func (g *Game) checkAttackers(v attackValidation, attackers []CardID, targets map[CardID]EntityID) error {
	ac := v.ac
	declared := make(map[CardID]EntityID, len(attackers))
	for _, id := range attackers {
		declared[id] = targets[id]
	}
	best, err := g.legalAttack(v.options)
	if err != nil {
		return err
	}
	bestN := ac.countViolations(best)
	if empty := ac.countViolations(nil); empty < bestN {
		best, bestN = nil, empty
	}
	if ac.countViolations(declared) <= bestN {
		return nil
	}
	var cards []CardID
	for _, r := range ac.requirements {
		d, attacking := declared[r.attacker]
		bd, bestAttacking := best[r.attacker]
		if r.violations(d, attacking) > r.violations(bd, bestAttacking) {
			cards = append(cards, r.attacker)
		}
	}
	return illegal("CR 508.1d", "attack requirements left unmet that could have been met", cards...)
}

// eachCombatStatic calls f for every static ability of the given Mode$ on a
// traitHost (battlefield permanents and Command-zone effect cards) whose
// conditions hold, in player then host order, stopping at f's first error.
// A param staticCombatParams does not allow, or a Condition$ value
// continuousConditionMet does not know, is an error: the static would
// otherwise be silently dropped or applied without its restriction (GO-7).
func (g *Game) eachCombatStatic(mode string, f func(h *Card, face *compile.Face, s *compile.Ability) error) error {
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for fi := range h.traitFaces() {
				face := &h.traitFaces()[fi]
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, mode) {
						continue
					}
					ok, err := combatStaticConditionsMet(g, h, face, s)
					if err != nil {
						return err
					}
					if !ok {
						continue
					}
					if err := f(h, face, s); err != nil {
						return err
					}
				}
			}
		}
	}
	return nil
}

// staticCombatParams are the params a combat-requirement static may carry
// and this port resolves: the mode, what it applies to, the MustAttack$
// target, its descriptions, AffectedZone$ Battlefield (where every creature
// it could apply to already is), Condition$ (continuousConditionMet's
// values) and IsPresent$ with its PresentCompare$/PresentZone$/
// PresentPlayer$ (isPresentMatches), and MinMaxBlocker's Min$/Max$.
var staticCombatParams = [...]string{
	"Mode", "ValidCreature", "ValidCard", "MustAttack", "Description", "Secondary", "AffectedZone",
	"Condition", "IsPresent", "PresentCompare", "PresentZone", "PresentPlayer", "Min", "Max",
}

// combatStaticConditionsMet is StaticAbility.checkConditions for the
// combat-requirement modes: every param allowed (staticCombatParams),
// Condition$ one continuousConditionMet knows, then both gates evaluated.
func combatStaticConditionsMet(g *Game, h *Card, face *compile.Face, s *compile.Ability) (bool, error) {
	for _, p := range s.Params {
		known := false
		for _, k := range staticCombatParams {
			if strings.EqualFold(p.Key, k) {
				known = true
				break
			}
		}
		if !known {
			return false, fmt.Errorf("engine: Mode$ %s: %s$ not resolvable yet", s.Name, p.Key)
		}
	}
	if z, ok := s.Param("AffectedZone"); ok && z != "Battlefield" {
		return false, fmt.Errorf("engine: Mode$ %s: AffectedZone$ %q not resolvable yet", s.Name, z)
	}
	if c, ok := s.Param("Condition"); ok && !knownStaticCondition(c) {
		return false, fmt.Errorf("engine: Mode$ %s: Condition$ %q not resolvable yet", s.Name, c)
	}
	if !continuousConditionMet(g, h, s) {
		return false, nil
	}
	return isPresentMatches(g, h, face.Amounts, s, "IsPresent", "PresentCompare", "PresentDefined", "PresentZone", "PresentPlayer"), nil
}

// knownStaticCondition reports whether continuousConditionMet evaluates c
// rather than reading it as unmet.
func knownStaticCondition(c string) bool {
	switch c {
	case "PlayerTurn", "NotPlayerTurn", "Threshold", "Hellbent", "Metalcraft", "Delirium", "FatefulHour",
		"Blessing", "Monarch", "EnduringStory":
		return true
	}
	return false
}

// staticValidMatches is StaticAbility.matchesValidParam(key, card): an
// absent param matches every card.
func staticValidMatches(g *Game, h *Card, s *compile.Ability, key string, card CardID) bool {
	spec, ok := s.Param(key)
	if !ok {
		return true
	}
	return Matches(g, g.Card(card), valid.Parse(spec), h.Controller(), h.ID)
}
