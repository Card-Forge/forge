// Trigger modes whose only tests are a few params against the event: the
// Cycled, AbilityCast, SpellAbilityCast, SpellCastOrCopy and AttackerUnblocked
// families. Each fires from one place (activating an ability, casting a spell,
// declaring blockers) through scanTriggers.

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// scanTriggers walks every trigger of the given modes on every card in a zone
// that can hold one (phaseTriggerZones), in seat then zone order, calling test
// on each whose TriggerZones$ admits the host's zone and that names no
// ActivationLimit$/ResolvedLimit$ (no per-trigger counter enforces them,
// checkPlayerActionTriggers' reasoning). test returns the triggering objects
// and whether the trigger fires.
func (g *Game) scanTriggers(modes []string, test func(h *Card, face triggerFace, t *compile.Ability) (triggeredObjects, bool)) []Ability {
	var matches []Ability
	for _, pid := range g.Players() {
		for _, z := range phaseTriggerZones {
			for _, host := range g.Zone(z, pid).Cards() {
				matches = g.appendTriggerMatches(matches, g.Card(host), z, modes, test)
			}
		}
	}
	return matches
}

// appendTriggerMatches is scanTriggers' per-host body, also used for a host
// that sits on the stack (a spell's own "when you cast this spell").
func (g *Game) appendTriggerMatches(matches []Ability, h *Card, zone ZoneType, modes []string, test func(h *Card, face triggerFace, t *compile.Ability) (triggeredObjects, bool)) []Ability {
	if h.Def == nil {
		return matches
	}
	for face := range h.triggerFaces {
		for _, t := range face.Triggers {
			if !modeIn(t, modes) || !phaseTriggerZoneMatches(h, t, zone) {
				continue
			}
			objs, ok := test(h, face, t)
			if !ok {
				continue
			}
			if sub, api, optional, ok := triggerEffectAPI(g, h, face.Amounts, t); ok {
				matches = append(matches, Ability{API: api, Source: h.ID, Controller: h.Controller(), Params: sub, Amounts: face.Amounts, Optional: optional,
					triggered: face.objects(objs), staticTrigger: isStaticTrigger(t)})
			}
		}
	}
	return matches
}

// modeIn reports whether t's Mode$ is one of modes.
func modeIn(t *compile.Ability, modes []string) bool {
	for _, m := range modes {
		if strings.EqualFold(t.Name, m) {
			return true
		}
	}
	return false
}

// triggerCardMatches is matchesValidParam("ValidCard", c) for a trigger on h:
// true when the trigger names no ValidCard$.
func (g *Game) triggerCardMatches(h *Card, t *compile.Ability, key string, c *Card) bool {
	spec, ok := t.Param(key)
	return !ok || Matches(g, c, valid.Parse(spec), h.Controller(), h.ID)
}

// triggerPlayerMatches is matchesValidParam(key, player): true when the
// trigger names no such param; a spec matchesPlayerSpec cannot read does not
// match (GO-7).
func (g *Game) triggerPlayerMatches(h *Card, t *compile.Ability, key string, pid PlayerID) bool {
	spec, ok := t.Param(key)
	if !ok {
		return true
	}
	matched, recognized := matchesPlayerSpec(g, pid, h.Controller(), h.ID, spec)
	return recognized && matched
}

var abilityCastModes = []string{"AbilityCast", "SpellAbilityCast"}

// abilityCastParams are the params a cast/activation trigger may carry that
// this port evaluates; a line naming another is not fired (GO-7).
var abilityCastParams = map[string]bool{
	"mode": true, "execute": true, "triggerdescription": true, "triggerzones": true, "optionaldecider": true,
	"secondary": true, "activationlimit": true, "resolvedlimit": true, "validcard": true,
	"validactivatingplayer": true, "validsa": true,
}

// checkAbilityCastTriggers is MagicStack.addAbility's AbilityCast and
// SpellAbilityCast runs (MagicStack.java:235-237, :350) for the activated
// ability a that was just put on the stack: ValidCard$ against the ability's
// source card, ValidActivatingPlayer$ against its controller, ValidSA$ against
// the ability (spellAbilityMatches). TargetsValid$ and ValidSAonCard$ lines
// are not fired.
func (g *Game) checkAbilityCastTriggers(controller PlayerController, a *Ability) {
	source := g.Card(a.Source)
	matches := g.scanTriggers(abilityCastModes, func(h *Card, face triggerFace, t *compile.Ability) (triggeredObjects, bool) {
		if !paramsResolvable(t, abilityCastParams) || !g.triggerCardMatches(h, t, "ValidCard", source) ||
			!g.triggerPlayerMatches(h, t, "ValidActivatingPlayer", a.Controller) {
			return triggeredObjects{}, false
		}
		if spec, ok := t.Param("ValidSA"); ok {
			if m, known := g.spellAbilityMatches(a, spec, h, h.Controller(), face.Amounts); !known || !m {
				return triggeredObjects{}, false
			}
		}
		return triggeredObjects{card: a.Source}, true
	})
	g.pushTriggeredAbilities(controller, matches)
}

// cycledParams are the params a Cycled trigger may carry that are evaluated.
var cycledParams = map[string]bool{
	"mode": true, "execute": true, "triggerdescription": true, "triggerzones": true, "optionaldecider": true,
	"secondary": true, "validcard": true, "validplayer": true, "firsttime": true, "checksvar": true, "svarcompare": true,
}

// checkCycledTriggers is Player.addCycled (Player.java:3861): the card cycled
// by activating its Cycling or TypeCycling ability, now in its owner's
// graveyard. FirstTime$ is the player's first cycling this turn.
func (g *Game) checkCycledTriggers(controller PlayerController, card CardID, pid PlayerID, first bool) {
	cycled := g.Card(card)
	matches := g.scanTriggers([]string{"Cycled"}, func(h *Card, _ triggerFace, t *compile.Ability) (triggeredObjects, bool) {
		if !paramsResolvable(t, cycledParams) || !g.triggerCardMatches(h, t, "ValidCard", cycled) ||
			!g.triggerPlayerMatches(h, t, "ValidPlayer", pid) {
			return triggeredObjects{}, false
		}
		if _, ok := t.Param("FirstTime"); ok && !first {
			return triggeredObjects{}, false
		}
		return triggeredObjects{card: card, player: pid}, true
	})
	g.pushTriggeredAbilities(controller, matches)
}

var unblockedParams = map[string]bool{
	"mode": true, "execute": true, "triggerdescription": true, "triggerzones": true, "optionaldecider": true,
	"secondary": true, "validcard": true, "validdefender": true, "validdefenders": true, "validattackingplayer": true,
}

// checkUnblockedTriggers is Combat.fireTriggersForUnblockedAttackers
// (Combat.java:673-703), run once the blockers are declared: AttackerUnblocked
// for every attacker nobody blocked (ValidCard$ the attacker, ValidDefender$
// what it attacks), then AttackerUnblockedOnce for the combat if any attacker
// was unblocked (ValidAttackingPlayer$ the active player, ValidDefenders$ the
// entities those attackers attack, one for each). A CR 509.1h forced-blocked
// attacker counts as blocked.
func (g *Game) checkUnblockedTriggers(controller PlayerController) {
	var unblocked []CardID
	for _, id := range g.combat.Attackers {
		if !g.combat.isBlocked(id) {
			unblocked = append(unblocked, id)
		}
	}
	if len(unblocked) == 0 {
		return
	}
	var matches []Ability
	for _, id := range unblocked {
		attacker := g.Card(id)
		defender := g.combat.AttackTargets[id]
		matches = append(matches, g.scanTriggers([]string{"AttackerUnblocked"}, func(h *Card, _ triggerFace, t *compile.Ability) (triggeredObjects, bool) {
			if !paramsResolvable(t, unblockedParams) || !g.triggerCardMatches(h, t, "ValidCard", attacker) {
				return triggeredObjects{}, false
			}
			if spec, ok := t.Param("ValidDefender"); ok && !attackedTargetMatches(g, h, []EntityID{defender}, spec) {
				return triggeredObjects{}, false
			}
			return triggeredObjects{attacker: id}, true
		})...)
	}
	var defenders []EntityID
	for _, id := range unblocked {
		defenders = append(defenders, g.combat.AttackTargets[id])
	}
	matches = append(matches, g.scanTriggers([]string{"AttackerUnblockedOnce"}, func(h *Card, _ triggerFace, t *compile.Ability) (triggeredObjects, bool) {
		if !paramsResolvable(t, unblockedParams) || !g.triggerPlayerMatches(h, t, "ValidAttackingPlayer", g.activePlayer) {
			return triggeredObjects{}, false
		}
		if spec, ok := t.Param("ValidDefenders"); ok && !attackedTargetMatches(g, h, defenders, spec) {
			return triggeredObjects{}, false
		}
		return triggeredObjects{}, true
	})...)
	g.pushTriggeredAbilities(controller, matches)
}

var discardedAllParams = map[string]bool{
	"mode": true, "execute": true, "triggerdescription": true, "triggerzones": true, "optionaldecider": true,
	"secondary": true, "validplayer": true, "validcard": true, "activationlimit": true, "firsttime": true,
	"gameactivationlimit": true,
}

// checkDiscardedAllTriggers is the DiscardedAll run after a batch of discards
// (SpellAbilityEffect.java:943, Player.java:3938, CostDiscard.java:246),
// TriggerDiscardedAll.performTest: ValidPlayer$ the discarder, ValidCard$
// narrowing the batch (the trigger's cards, and its amount, are the matching
// ones), FirstTime$ that none of them matching ValidCard$ was discarded
// earlier this turn. ValidCause$ and Random$ lines are not fired.
func (g *Game) checkDiscardedAllTriggers(controller PlayerController, pid PlayerID, discarded []CardID) {
	pl := g.Player(pid)
	before := pl.discardedThisTurn
	pl.discardedThisTurn = append(append([]CardID(nil), before...), discarded...)
	if len(discarded) == 0 {
		return
	}
	matches := g.scanTriggers([]string{"DiscardedAll"}, func(h *Card, _ triggerFace, t *compile.Ability) (triggeredObjects, bool) {
		if !paramsResolvable(t, discardedAllParams) || !g.triggerPlayerMatches(h, t, "ValidPlayer", pid) {
			return triggeredObjects{}, false
		}
		var cards []CardID
		for _, id := range discarded {
			if g.triggerCardMatches(h, t, "ValidCard", g.Card(id)) {
				cards = append(cards, id)
			}
		}
		if len(cards) == 0 {
			return triggeredObjects{}, false
		}
		if _, ok := t.Param("FirstTime"); ok {
			for _, id := range before {
				if g.triggerCardMatches(h, t, "ValidCard", g.Card(id)) {
					return triggeredObjects{}, false
				}
			}
		}
		return triggeredObjects{player: pid, cards: cards, counts: triggerCounts{amount: len(cards), set: countAmount}}, true
	})
	g.pushTriggeredAbilities(controller, matches)
}
