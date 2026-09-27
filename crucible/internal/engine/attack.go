// Declaring attackers: CR 508.1, the first step of combat this port
// reaches.

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/cost"
)

// Attackers returns the creatures currently attacking, if any.
func (g *Game) Attackers() []CardID { return g.combat.Attackers }

// DeclareCombatAttackers is CR 508.1: the active player chooses which of their
// eligible creatures attack. A creature is eligible if it is untapped and
// either has no summoning sickness or has haste (CR 302.6, CR 508.1a) --
// nothing this port can grant a creature the ability to attack tapped, so
// that half of 508.1a is not a case that can arise yet.
//
// The steps run in PhaseHandler.declareAttackersTurnBasedAction's order
// (PhaseHandler.java:545-556): the controller's answer and each attacker's
// defender (CR 508.1a-b, chooseAttackTargets) first, then the legality check
// (CR 508.1c-d, validateAttackers in attackconstraints.go), and only then
// does anything happen to the game -- declared attackers tap unless they
// have vigilance (CR 508.1f), exert (CR 508.1g) and trigger. A declaration
// that fails the check is an *IllegalDeclarationError and changes nothing
// (ADR-0024): Java re-prompts its human, this port's controller is expected
// to answer legally, and nothing is repaired on its behalf -- a goaded
// creature left home is not added back, since which creature obeys a
// requirement is the controller's choice, not the engine's (ADR-0024,
// Considered Options 1).
//
// If no creature is eligible, the controller is not asked at all: there is
// nothing meaningful to decide, the same reasoning a mulligan offer with no
// legal targets would have no question to ask either. Java's
// CombatUtil.canAttack(playerTurn) guard skips the whole declaration,
// validation included, the same way (PhaseHandler.java:537). This also keeps
// every existing scenario that walks through the DeclareAttackers phase
// without ever creating a creature from needing to queue an empty answer.
//
// Not wired into AdvancePhase's automatic walk through the phases
// (turn.go): PerformMulligans is the precedent for a real M5 mechanic a
// scenario calls explicitly (actions.log's own `declareattackers` verb)
// rather than one the turn structure invokes unconditionally.
// checkAttacksTriggers (trigger.go) runs once per declared attacker, after
// tapping and target assignment both landed -- CR 508.3's own "whenever ~
// attacks" trigger fires off the attack as declared, not off a
// still-provisional one. checkAttackersDeclaredTrigger (trigger.go) runs
// once after that loop, for CR 508.1's own "whenever a player attacks"
// trigger.
func (g *Game) DeclareCombatAttackers(controller PlayerController) ([]CardID, error) {
	eligible := g.eligibleAttackers()
	if len(eligible) == 0 {
		return nil, nil
	}

	attackers := controller.DeclareCombatAttackers(g, g.activePlayer, eligible)
	for i, id := range attackers {
		if !containsCard(eligible, id) {
			return nil, illegal("CR 508.1a", "not an eligible attacker", id)
		}
		if containsCard(attackers[:i], id) {
			return nil, illegal("CR 508.1a", "declared as an attacker twice", id)
		}
	}
	targets, err := g.chooseAttackTargets(controller, attackers)
	if err != nil {
		return nil, err
	}
	if err := g.validateAttackers(attackers, targets); err != nil {
		return nil, err
	}

	g.combat.Attackers = attackers
	g.combat.AttackTargets = targets
	for _, id := range attackers {
		if !g.Card(id).HasKeyword("Vigilance") {
			g.Card(id).Tapped = true
			g.checkTapsTriggers(controller, id, g.Card(id).Controller(), true)
		}
	}
	g.exertDeclaredAttackers(controller, attackers)
	for _, id := range attackers {
		g.Card(id).AttacksThisTurn++
		g.checkAttacksTriggers(controller, id)
	}
	g.checkAttackersDeclaredOneTargetTrigger(controller)
	g.checkAttackersDeclaredTrigger(controller)
	return attackers, nil
}

// eligibleAttackers is every creature the active player controls that CR
// 508.1a lets attack (canAttackAtAll), in battlefield order.
func (g *Game) eligibleAttackers() []CardID {
	var eligible []CardID
	for _, id := range g.Zone(Battlefield, g.activePlayer).Cards() {
		if g.canAttackAtAll(id) {
			eligible = append(eligible, id)
		}
	}
	return eligible
}

// canAttackAtAll is the part of CombatUtil.canAttack(attacker, defender)
// that does not depend on the defender, for the sources this port has: a
// creature, untapped, not summoning sick unless it has haste, without
// StaticAbilityCantAttackBlock.cantAttack's own two keyword lines
// ("CARDNAME can't attack." and "CARDNAME can't attack or block.",
// StaticAbilityCantAttackBlock.java:41, printed or hidden), not detained
// (CR 701.35).
func (g *Game) canAttackAtAll(id CardID) bool {
	c := g.Card(id)
	if !c.Type().Has(cardtype.Creature) || c.Tapped {
		return false
	}
	if c.hasKeywordText("CARDNAME can't attack.") || c.hasKeywordText("CARDNAME can't attack or block.") {
		return false
	}
	if c.SummonSick && !c.HasKeyword("Haste") {
		return false
	}
	return !c.isDetained()
}

// exertDeclaredAttackers is CR 508.1c: after tapping, before target
// assignment (PhaseHandler.declareAttackersTurnBasedAction, right after
// attackers are provisionally tapped -- CombatUtil.getOptionalAttackCostCreatures
// filters the same way optionalAttackCostExert (below) does). Every
// attacker carrying a real S:Mode$ OptionalAttackCost | Cost$
// Exert<1/CARDNAME> static ability (Ahn-Crop Crasher) is offered; if none
// are, the controller is not asked at all, the same "nothing meaningful to
// decide" reasoning DeclareCombatAttackers itself already uses for an empty
// eligible set.
func (g *Game) exertDeclaredAttackers(controller PlayerController, attackers []CardID) {
	var eligible []CardID
	for _, id := range attackers {
		if _, ok := optionalAttackCostExert(g.Card(id)); ok {
			eligible = append(eligible, id)
		}
	}
	if len(eligible) == 0 {
		return
	}
	chosen := controller.ExertAttackers(g, g.activePlayer, eligible)
	for _, id := range chosen {
		c := g.Card(id)
		c.Exerted = true
		g.checkExertedTriggers(controller, id)
		g.resolveOptionalAttackCostPayoff(controller, id)
	}
}

// optionalAttackCostExert is exertDeclaredAttackers' own eligibility check:
// c's own S:Mode$ OptionalAttackCost static ability, if its own Cost$ names
// Exert<1/CARDNAME|NICKNAME> (cost.ActivationShape's own SelfExert,
// activateability.go's identical reuse for the unrelated activated-ability
// shape) -- every one of the corpus's 28 real OptionalAttackCost lines,
// this port's only supported OptionalAttackCost cost shape.
func optionalAttackCostExert(c *Card) (*compile.Ability, bool) {
	if c.Def == nil {
		return nil, false
	}
	for _, face := range c.Def.Faces {
		for _, s := range face.Statics {
			if !strings.EqualFold(s.Name, "OptionalAttackCost") {
				continue
			}
			costText, ok := s.Param("Cost")
			if !ok {
				continue
			}
			if shape, ok := cost.Parse(costText).ActivationShape(); ok && shape.SelfExert {
				return s, true
			}
		}
	}
	return nil, false
}

// resolveOptionalAttackCostPayoff runs id's own OptionalAttackCost static
// ability's own Trigger$ sub-ability (compile.go's own "trigger" subAbilityKeys
// entry resolves it at compile time) -- CR 508.1c's "when you do" payoff,
// 23 of the corpus's 28 real OptionalAttackCost lines (5 name none at all:
// resolute_survivors.txt's own payoff is an ordinary T:Mode$ Exerted line
// instead, which checkExertedTriggers (exertcost.go), already called before
// this, already covers). Pushed the identical way checkExertedTriggers'
// own matches are: through pushTriggeredAbilities, for CR 601.2c/603.3b's
// own "choices are made the moment it's put on the stack" targeting and
// APNAP-consistent push, even though a lone exerting player has no APNAP
// order to disagree with.
func (g *Game) resolveOptionalAttackCostPayoff(controller PlayerController, id CardID) {
	s, ok := optionalAttackCostExert(g.Card(id))
	if !ok {
		return
	}
	for _, sub := range s.Subs {
		if !strings.EqualFold(sub.Key, "Trigger") {
			continue
		}
		api, ok := APIByName(sub.Ability.Name)
		if !ok {
			return
		}
		c := g.Card(id)
		payoff := Ability{API: api, Source: id, Controller: c.Controller(), Params: sub.Ability, Amounts: c.Def.Faces[0].Amounts}
		g.pushTriggeredAbilities(controller, []Ability{payoff})
		return
	}
}

// AttackTarget returns what attacker is attacking -- a player, or a
// planeswalker/battle that player controls.
func (g *Game) AttackTarget(attacker CardID) EntityID { return g.combat.AttackTargets[attacker] }

// chooseAttackTargets is CR 508.1b: for each declared attacker, what it's
// attacking. Every attacker shares the same eligible set, narrowed per
// attacker only by goad's restriction half (goadTargets, CR 701.15b --
// CombatUtil.canAttack's own goad branch, CombatUtil.java:215-229). A lone
// option -- the ordinary two-player game with no planeswalker or battle on
// the other side -- is assigned automatically, the same "nothing
// meaningful to decide" reasoning DeclareCombatAttackers uses for an empty
// eligible list; more than one asks the controller per attacker
// (ChooseAttackTarget). An answer outside the options breaks a restriction
// -- Java's countViolations returns -1 for it -- and is an
// *IllegalDeclarationError. Nothing is written to the game: the caller
// commits the map once the whole declaration passed validation.
func (g *Game) chooseAttackTargets(controller PlayerController, attackers []CardID) (map[CardID]EntityID, error) {
	eligible := g.eligibleAttackTargets()
	targets := make(map[CardID]EntityID, len(attackers))
	for _, id := range attackers {
		options := g.goadTargets(id, eligible)
		if len(options) == 1 {
			targets[id] = options[0]
			continue
		}
		target := controller.ChooseAttackTarget(g, g.activePlayer, id, options)
		if !containsEntity(options, target) {
			return nil, illegal("CR 508.1b", "cannot attack the chosen defender", id)
		}
		targets[id] = target
	}
	return targets, nil
}

// eligibleAttackTargets is every opponent still in the game, plus every
// planeswalker or battle any of them controls (CR 506.4c).
func (g *Game) eligibleAttackTargets() []EntityID {
	var eligible []EntityID
	for _, pid := range g.Players() {
		if pid == g.activePlayer || g.Player(pid).Lost {
			continue
		}
		eligible = append(eligible, PlayerEntity(pid))
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			t := g.Card(id).Type()
			if t.Has(cardtype.Planeswalker) || t.Has(cardtype.Battle) {
				eligible = append(eligible, CardEntity(id))
			}
		}
	}
	return eligible
}

// defenderOf is the player defending against attacker: the player it's
// attacking directly, or the controller of the planeswalker/battle it's
// attacking (CR 802.4a's "attacking him/her or a planeswalker/battle he/she
// controls" is what makes that player the one who can block it).
//
// Called per attacker, not once for the whole combat: a two-player game, or
// a multiplayer game where the active player sent every attacker at a
// single opponent, gets the same defender back every time, but a combat
// split across more than one defending player at once (CR 506.4) does not,
// and DeclareCombatBlockers (block.go) groups by the result rather than
// assuming one answer for every attacker.
func (g *Game) defenderOf(attacker CardID) PlayerID {
	target := g.combat.AttackTargets[attacker]
	if pid, ok := target.AsPlayer(); ok {
		return pid
	}
	cid, _ := target.AsCard()
	return g.Card(cid).Controller()
}

// attackersOf is every creature currently attacking target directly -- a
// player or, more often the reason this exists, a planeswalker or battle
// (assignBattleProtector, action.go, CR 704.5w's "no attacking creatures
// currently attacking that battle"). Nil, not an error, when nothing is.
func (g *Game) attackersOf(target EntityID) []CardID {
	var attackers []CardID
	for _, id := range g.combat.Attackers {
		if g.combat.AttackTargets[id] == target {
			attackers = append(attackers, id)
		}
	}
	return attackers
}
