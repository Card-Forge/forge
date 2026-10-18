// The two once-per-declaration "blockers" trigger modes: Mode$ BlockersDeclared
// ("whenever one or more creatures block", Tide of War) and Mode$
// AttackerBlockedOnce ("whenever one or more creatures you control become
// blocked", Big Spender, Hezrou, Neyith of the Dire Hunt). Each fires once for
// the whole declare-blockers step, unlike Blocks/AttackerBlocked, which fire
// per blocker or per attacker (trigger.go).
//
// Ported from TriggerBlockersDeclared.java, TriggerAttackerBlockedOnce.java and
// the runTrigger calls in PhaseHandler.declareBlockersTurnBasedAction
// (PhaseHandler.java:748-762, :816-820).

package engine

import (
	"slices"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// checkBlockersDeclaredTriggers is TriggerType.BlockersDeclared, run once when
// at least one blocker was declared (PhaseHandler.java:748-762).
// TriggerBlockersDeclared.performTest is unconditionally true: only the
// common requirements (triggerEffectAPI) and TriggerZones$ gate it. It records
// AbilityKey.Blockers, every declared blocker once, and AbilityKey.Attackers,
// every attacker one of them blocks, once each in blocker order
// (PhaseHandler.java:750-757), read by Defined$ TriggeredBlockers/
// TriggeredAttackers.
//
// blocks is the whole declaration, in the order the defenders declared it;
// Java's Combat.getAllBlockers walks its blockedBands multimap instead, which
// for a two-player game holds the same blockers in the same declaration order.
func (g *Game) checkBlockersDeclaredTriggers(controller PlayerController, blocks []Block) {
	if len(blocks) == 0 {
		return
	}
	var blockers, attackers []CardID
	for _, b := range blocks {
		if !slices.Contains(blockers, b.Blocker) {
			blockers = append(blockers, b.Blocker)
		}
	}
	for _, blocker := range blockers {
		for _, b := range blocks {
			if b.Blocker == blocker && !slices.Contains(attackers, b.Attacker) {
				attackers = append(attackers, b.Attacker)
			}
		}
	}
	objs := triggeredObjects{blockers: blockers, attackers: attackers, attackersRecorded: true}
	g.pushTriggeredAbilities(controller, g.onceTriggerMatches("BlockersDeclared", func(*Card, *compile.Ability) bool { return true }, objs))
}

// checkAttackerBlockedOnceTriggers is TriggerType.AttackerBlockedOnce, run once
// after every per-attacker AttackerBlocked trigger when at least one attacker
// has a blocker (PhaseHandler.java:778-820). The attackers are the combat's
// blocked attackers in combat.getAttackers() order, not block order.
// TriggerAttackerBlockedOnce.performTest matches ValidCard$ against that whole
// list: any one matching passes (CardTraitBase.matchesValid's Iterable
// branch), and an absent ValidCard$ passes. It records AbilityKey.Attackers.
func (g *Game) checkAttackerBlockedOnceTriggers(controller PlayerController, blocks []Block) {
	var blocked []CardID
	for _, a := range g.combat.Attackers {
		if slices.ContainsFunc(blocks, func(b Block) bool { return b.Attacker == a }) {
			blocked = append(blocked, a)
		}
	}
	g.runAttackerBlockedOnce(controller, blocked)
}

// runAttackerBlockedOnce runs Mode$ AttackerBlockedOnce with AbilityKey.Attackers
// blocked, nothing when it is empty: the declaration's blocked attackers
// (checkAttackerBlockedOnceTriggers), or the one attacker an effect's creature
// starts blocking (addToCombatBlocking, SpellAbilityEffect.java:808-812).
func (g *Game) runAttackerBlockedOnce(controller PlayerController, blocked []CardID) {
	if len(blocked) == 0 {
		return
	}
	match := func(h *Card, t *compile.Ability) bool {
		spec, ok := t.Param("ValidCard")
		if !ok {
			return true
		}
		parsed := valid.Parse(spec)
		return slices.ContainsFunc(blocked, func(id CardID) bool {
			return Matches(g, g.Card(id), parsed, h.Controller(), h.ID)
		})
	}
	objs := triggeredObjects{attackers: blocked, attackersRecorded: true}
	g.pushTriggeredAbilities(controller, g.onceTriggerMatches("AttackerBlockedOnce", match, objs))
}

// onceTriggerMatches walks every trigger of Mode$ mode the way the
// AttackersDeclared scan does (phaseTriggerZones, TriggerZones$ honoured), and
// returns an Ability for each one test accepts, carrying objs. objs' slices
// are shared by every match: none is ever edited in place.
func (g *Game) onceTriggerMatches(mode string, test func(*Card, *compile.Ability) bool, objs triggeredObjects) []Ability {
	var matches []Ability
	for _, pid := range g.Players() {
		for _, z := range phaseTriggerZones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for face := range h.triggerFaces {
					for _, t := range face.Triggers {
						if !strings.EqualFold(t.Name, mode) || !phaseTriggerZoneMatches(h, t, z) || !test(h, t) {
							continue
						}
						if sub, api, optional, ok := triggerEffectAPI(g, h, face.Amounts, t); ok {
							matches = append(matches, Ability{API: api, Source: host, Controller: h.Controller(), Params: sub, Amounts: face.Amounts,
								Optional: optional, triggered: face.objects(objs), staticTrigger: isStaticTrigger(t)})
						}
					}
				}
			}
		}
	}
	return matches
}
