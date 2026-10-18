package engine

//enginelint:allow control game ability effecthelpers card condition defined zone id valid trigger combat blockeffect phase

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// changeCombatantsEffect is ChangeCombatantsEffect.java through
// SpellAbilityEffect.addToCombat with Attacking$ True (CR 506.3b): during
// combat, each Defined$ or targeted creature the attacking player controls
// becomes an attacker, attacking the defender the activator picks among
// every eligible one -- or, if it is already attacking, is redirected to
// it. Optional$ asks the activator per creature first. A defender set other
// than True (Attacking$ TargetedPlayer, ...) is not resolved.
type changeCombatantsEffect struct{}

func (changeCombatantsEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "ChangeCombatants", "Condition", "TargetsWithDefinedController", "Blocking"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	attacking, ok := a.Params.Param("Attacking")
	if !ok {
		return nil
	}
	if attacking != "True" {
		return fmt.Errorf("engine: ChangeCombatants: Attacking$ %q not resolvable yet", attacking)
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ChangeCombatants: %w", err)
	}
	for _, id := range cards {
		c := g.Card(id)
		if !g.activePhase.IsCombat() || c.Zone != Battlefield || !c.Type().Has(cardtype.Creature) {
			continue
		}
		if c.Controller() != g.activePlayer {
			continue
		}
		if hasParam(a, "Optional") && !controller.ConfirmEffect(g, a.Controller, a.Source) {
			continue
		}
		if err := g.attackByEffect(controller, a.Controller, id); err != nil {
			return fmt.Errorf("engine: ChangeCombatants: %w", err)
		}
	}
	return nil
}

// attackByEffect is SpellAbilityEffect.addToCombat's Attacking$ True branch
// (SpellAbilityEffect.java:767-793, CR 506.3b) once its caller has checked
// that id is a creature on the battlefield controlled by the attacking
// player during combat: decider picks among every eligible defender and id
// attacks it -- or, already attacking another one, is redirected.
func (g *Game) attackByEffect(controller PlayerController, decider PlayerID, id CardID) error {
	return g.attackAmong(controller, decider, id, g.eligibleAttackTargets())
}

// attackAmong is addToCombat's attacking half over the defenders defs
// (SpellAbilityEffect.java:779-793): decider picks one and id attacks it,
// unless it already does. Its band is set unblocked (setBlocked(false),
// :791), so a creature put into combat after blockers were declared is an
// unblocked attacker (Combat.isUnblocked). No defender is no change.
func (g *Game) attackAmong(controller PlayerController, decider PlayerID, id CardID, defs []EntityID) error {
	if len(defs) == 0 {
		return nil
	}
	defender := controller.ChooseAttackTarget(g, decider, id, defs)
	if err := checkChoice([]EntityID{defender}, defs, 1, 1); err != nil {
		return err
	}
	if g.combat.isAttacking(id) && g.combat.AttackTargets[id] == defender {
		return nil
	}
	g.removeFromCombat(id)
	g.combat.Attackers = append(g.combat.Attackers, id)
	if g.combat.AttackTargets == nil {
		g.combat.AttackTargets = make(map[CardID]EntityID)
	}
	g.combat.AttackTargets[id] = defender
	g.combat.blockDecided = append(g.combat.blockDecided, id)
	return nil
}

// addToCombatAttacking is SpellAbilityEffect.addToCombat's attacking half
// (SpellAbilityEffect.java:758-793) for the creature id an effect of a just
// put onto the battlefield, read from param (TokenAttacking$): only a
// creature, only during combat, only one the attacking player controls. "True"
// offers every defender (combat.getDefenders); anything else is the
// " & "-joined Defined$ entities it names (TriggeredAttackedTarget,
// Remembered, ...). ForEach$ with a Defined$ value reads it off the created
// card instead of the host, not resolvable yet.
func (g *Game) addToCombatAttacking(controller PlayerController, a *Ability, id CardID, param string) error {
	c := g.Card(id)
	if c.Zone != Battlefield {
		return nil
	}
	return g.addToCombatAttackingAs(controller, a, id, param, c.Controller())
}

// addToCombatAttackingAs is addToCombatAttacking for a card about to enter
// the battlefield under ctrl (ChangeZone's Attacking$, changeZoneJoinCombat):
// Java's addToCombat checks neither the card's zone nor anything but the
// controller it has been given (SpellAbilityEffect.java:761, :768).
func (g *Game) addToCombatAttackingAs(controller PlayerController, a *Ability, id CardID, param string, ctrl PlayerID) error {
	raw, ok := a.Params.Param(param)
	if !ok {
		return nil
	}
	if !g.activePhase.IsCombat() || !g.Card(id).Type().Has(cardtype.Creature) || ctrl != g.activePlayer {
		return nil
	}
	if strings.EqualFold(raw, "True") {
		return g.attackByEffect(controller, a.Controller, id)
	}
	if hasParam(a, "ForEach") {
		return fmt.Errorf("%s$ %q with ForEach$ not resolvable yet", param, raw)
	}
	var defs []EntityID
	for _, part := range strings.Split(raw, " & ") {
		es, err := definedEntities(g, a.Controller, g.Card(a.Source), strings.TrimSpace(part), a.refs())
		if err != nil {
			return fmt.Errorf("%s$: %w", param, err)
		}
		for _, e := range es {
			if !containsEntity(defs, e) {
				defs = append(defs, e)
			}
		}
	}
	return g.attackAmong(controller, a.Controller, id, defs)
}

// addToCombatBlocking is SpellAbilityEffect.addToCombat's blocking half
// (SpellAbilityEffect.java:795-829) for the creature id an effect of a just
// put onto the battlefield, read from param (TokenBlocking$): only a creature,
// only during combat (:761). The attacker is the first card the param's
// Defined$ names from the host (TriggeredAttacker, Targeted, Imprinted); id
// blocks it when id's controller is that attacker's defending player
// (Combat.getDefenderPlayerByAttacker, :797). Nothing checks the block's
// legality -- Java calls neither canBlock nor validateBlocks -- and the pair is
// not recorded as blocked this turn (only declareBlockersTurnBasedAction and
// BlockEffect record that). The new blocker goes last in the attacker's
// damage assignment order (Combat.addBlockerToDamageAssignmentOrder without
// legacy ordering), which is g.combat.Blocks' order.
//
// Triggers, in Java's order and as one batch (beginTriggerBatch):
// AttackerBlockedByCreature for the pair, AttackerBlockedOnce with this
// attacker alone (:808-812), and -- only for an attacker that was not blocked
// before -- AttackerBlocked with every blocker it now has (:815-823), which
// also decides its blocked status (setBlocked). Blocks and BlockersDeclared do
// not run. Brimaz's own token blocks an attacker Brimaz already blocks, so
// only the first two run for it.
//
// An attacker that is no longer attacking is skipped: Java's
// getDefenderPlayerByAttacker returns null there and :797's .equals throws
// (a Forge defect, PORT-8), where the rule (CR 506.4) is that a creature
// removed from combat cannot be blocked. A block before blockers are declared
// is refused: DeclareCombatBlockers would replace it, and no corpus line
// reaches it (every TokenBlocking$ resolves in or after the declare blockers
// step).
func (g *Game) addToCombatBlocking(controller PlayerController, a *Ability, id CardID, param string) error {
	c := g.Card(id)
	if c.Zone != Battlefield {
		return nil
	}
	return g.addToCombatBlockingAs(controller, a, id, param, c.Controller())
}

// addToCombatBlockingAs is addToCombatBlocking for a card about to enter the
// battlefield under ctrl (ChangeZone's Blocking$, changeZoneJoinCombat).
func (g *Game) addToCombatBlockingAs(controller PlayerController, a *Ability, id CardID, param string, ctrl PlayerID) error {
	raw, ok := a.Params.Param(param)
	if !ok {
		return nil
	}
	if !g.activePhase.IsCombat() || !g.Card(id).Type().Has(cardtype.Creature) {
		return nil
	}
	cards, err := definedCards(g.Card(a.Source), raw, a.refs())
	if err != nil {
		return fmt.Errorf("%s$: %w", param, err)
	}
	if len(cards) == 0 {
		return nil
	}
	attacker := cards[0]
	defender, attacking := g.defenderPlayerByAttacker(attacker)
	if !attacking || defender != ctrl {
		return nil
	}
	if g.activePhase < DeclareBlockers {
		return fmt.Errorf("%s$ before blockers are declared not resolvable yet", param)
	}
	wasBlocked := g.combat.isBlocked(attacker)
	pair := Block{Blocker: id, Attacker: attacker}
	g.combat.Blocks = append(g.combat.Blocks, pair)
	g.beginTriggerBatch()
	defer g.endTriggerBatch(controller)
	g.checkAttackerBlockedByCreatureTriggers(controller, pair)
	g.runAttackerBlockedOnce(controller, []CardID{attacker})
	if !wasBlocked {
		g.checkAttackerBlockedTriggers(controller, attacker, g.blockersOf(attacker))
		if !containsCard(g.combat.blockDecided, attacker) {
			g.combat.blockDecided = append(g.combat.blockDecided, attacker)
		}
	}
	return nil
}
