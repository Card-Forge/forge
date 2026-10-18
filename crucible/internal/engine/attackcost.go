// Attack costs (CR 508.1d, 508.1h-j): a Mode$ CantAttackUnless static --
// Propaganda, Ghostly Prison, Brainwash, Leviathan -- makes a creature unable
// to attack, or to attack a given defender, unless its controller pays a
// cost as attackers are declared.
//
// Ported from CombatUtil.getAttackCost (CombatUtil.java:287-305),
// StaticAbility.getAttackCost (StaticAbility.java:306-314),
// StaticAbilityCantAttackBlock.getAttackCost
// (StaticAbilityCantAttackBlock.java:335-369), CombatUtil.checkPropagandaEffects
// (CombatUtil.java:253-269) and the payment loop of
// PhaseHandler.declareAttackersTurnBasedAction (PhaseHandler.java:553-597).
// The cost is paid with the UnlessCost$ machinery (unlesscost.go): the same
// "pay this or not" question (ConfirmPayCost) and the same parts, which is
// what Java's payCostDuringAbilityResolve is for both.

//enginelint:allow id zone card game ability control unlesscost attackconstraints combat amount valid

package engine

import (
	"fmt"
	"slices"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// attackCostParams are the params a Mode$ CantAttackUnless line may carry and
// this port resolves: what it applies to (ValidCard$, Target$), the cost and
// RememberingAttacker$, the static conditions staticConditionsMet evaluates
// (EffectZone$, Condition$ through knownStaticCondition, IsPresent$ and its
// companions) and descriptions. Anything else is an error, never a tax
// silently not charged (GO-7).
var attackCostParams = map[string]bool{
	"mode": true, "validcard": true, "target": true, "cost": true, "rememberingattacker": true,
	"effectzone": true, "condition": true, "ispresent": true, "presentcompare": true, "presentzone": true,
	"presentplayer": true, "description": true, "triggerdescription": true, "secondary": true,
}

// attackCost is CombatUtil.getAttackCost(game, attacker, defender) with no
// optional-cost attackers (CombatUtil.java:271-272): every Mode$
// CantAttackUnless static on a trait host whose conditions hold, whose
// ValidCard$ names attacker and whose Target$ names defender adds its Cost$.
// The parts are joined into one cost text, Cost.add's sum (CombatUtil.java:
// 295). costed is false when no static applies -- Java's null; a static whose
// cost is zero still counts, as Java's non-null Cost does. OptionalAttackCost
// lines are exertDeclaredAttackers' own (attack.go).
func (g *Game) attackCost(attacker CardID, defender EntityID) (text string, costed bool, err error) {
	var parts []string
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.liveTraitFaces() {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, "CantAttackUnless") {
						continue
					}
					part, applies, err := g.staticAttackCost(h, &face, s, attacker, defender)
					if err != nil {
						return "", false, err
					}
					if applies {
						parts = append(parts, part)
					}
				}
			}
		}
	}
	return strings.Join(parts, " "), len(parts) > 0, nil
}

// staticAttackCost is StaticAbility.getAttackCost for one CantAttackUnless
// line: its conditions (checkConditions), then
// StaticAbilityCantAttackBlock.getAttackCost's ValidCard$ and Target$ tests
// (an absent param matches, matchesValidParam) and its cost text. A param
// attackCostParams does not list or a Condition$ continuousConditionMet does
// not know is an error.
func (g *Game) staticAttackCost(h *Card, face *compile.Face, s *compile.Ability, attacker CardID, defender EntityID) (string, bool, error) {
	for _, p := range s.Params {
		if !attackCostParams[strings.ToLower(p.Key)] {
			return "", false, fmt.Errorf("engine: Mode$ CantAttackUnless: %s$ not resolvable yet", p.Key)
		}
	}
	if c, ok := s.Param("Condition"); ok && !knownStaticCondition(c) {
		return "", false, fmt.Errorf("engine: Mode$ CantAttackUnless: Condition$ %q not resolvable yet", c)
	}
	if !g.staticConditionsMet(h, s) || !staticValidMatches(g, h, s, "ValidCard", attacker) {
		return "", false, nil
	}
	if v, ok := s.Param("Target"); ok && !g.attackTargetMatches(defender, v, h) {
		return "", false, nil
	}
	text, ok := s.Param("Cost")
	if !ok {
		return "", false, fmt.Errorf("engine: Mode$ CantAttackUnless without Cost$")
	}
	text, err := g.attackCostAmount(h, face, s, text, attacker)
	if err != nil {
		return "", false, err
	}
	return text, true, nil
}

// attackCostAmount is StaticAbilityCantAttackBlock.java:345-360: a Cost$
// naming an SVar of the host (Sphere of Safety's X, Myr Prototype's Y, War
// Tax's XChosen) is that many generic mana, evaluated on the host; with
// RememberingAttacker$ the host remembers the attacker while it is (Nils,
// Discipline Enforcer's Remembered$CardCounters.ALL). Any other text is the
// cost as written.
//
// Java appends an X shard to a name starting with "X" ("keep X shards") and
// then pays it as 0 (checkPropagandaEffects sets the fake ability's X SVar to
// "0", CombatUtil.java:266), so the shard is left out here: the amount paid
// is the same. Java's removeRemembered also forgets an attacker the host
// remembered before the evaluation; Forget reproduces that (PORT-7).
func (g *Game) attackCostAmount(h *Card, face *compile.Face, s *compile.Ability, text string, attacker CardID) (string, error) {
	name := strings.ToLower(text)
	_, isSVar := face.Amounts[name]
	if _, own := h.svars[name]; !isSVar && !own {
		return text, nil
	}
	_, remember := s.Param("RememberingAttacker")
	if remember {
		h.Memory.Remember(CardEntity(attacker))
	}
	n, ok := resolveNamedAmount(g, face.Amounts, h, text)
	if remember {
		h.Memory.Forget(CardEntity(attacker))
	}
	if !ok {
		return "", fmt.Errorf("engine: Mode$ CantAttackUnless: Cost$ %s: SVar not resolvable yet", text)
	}
	return strconv.Itoa(n), nil
}

// parsedAttackCost is attackCost read into the payable form, nil when the pair
// has no cost. A cost the UnlessCost$ parts do not cover is an error.
func (g *Game) parsedAttackCost(attacker CardID, defender EntityID) (*unlessCost, error) {
	text, costed, err := g.attackCost(attacker, defender)
	if err != nil || !costed {
		return nil, err
	}
	uc, ok := parseUnlessCost(text)
	if !ok {
		return nil, fmt.Errorf("engine: Mode$ CantAttackUnless: Cost$ %q not resolvable yet", text)
	}
	return &uc, nil
}

// checkAttackCosts reads every declared attacker's cost before anything
// changes, so a cost this port cannot pay fails the declaration with the game
// untouched. payAttackCosts reads each again when it is paid: an earlier
// payment can change a later amount (sacrificing an enchantment lowers Sphere
// of Safety's X), as Java's per-attacker getAttackCost call sees.
func (g *Game) checkAttackCosts(attackers []CardID, targets map[CardID]EntityID) error {
	for _, id := range attackers {
		if _, err := g.parsedAttackCost(id, targets[id]); err != nil {
			return err
		}
	}
	return nil
}

// payAttackCosts is PhaseHandler.java:576-597 over the committed combat, the
// declared attackers already tapped (provisional, the ones without
// vigilance): each attacker in turn pays its cost (checkPropagandaEffects --
// its controller is asked ConfirmPayCost, then pays, the payment ability's
// source being the attacker, as Java's EmptySa(attacker) is). One that is not
// paid leaves combat and untaps, and the remaining attack is validated again
// against v, the constraints built before tapping. If it fails, every
// provisionally tapped creature untaps, the combat is restored to prev and the
// error is returned: Java re-prompts the declaration there, which ADR-0024
// makes the caller's job, and what was already paid stays paid, as in Java
// (the TODO at PhaseHandler.java:577). Attackers that left the battlefield
// while paying leave combat (Combat.removeAbsentCombatants). It returns the
// attackers left.
func (g *Game) payAttackCosts(controller PlayerController, v attackValidation, provisional []CardID, prev Combat) ([]CardID, error) {
	for _, id := range slices.Clone(g.combat.Attackers) {
		if !containsCard(g.combat.Attackers, id) || g.Card(id).Zone != Battlefield {
			continue
		}
		uc, err := g.parsedAttackCost(id, g.combat.AttackTargets[id])
		if err != nil {
			return nil, err
		}
		if uc == nil || g.payAttackCost(controller, id, *uc) {
			continue
		}
		g.removeFromCombat(id)
		if containsCard(provisional, id) {
			g.Card(id).Tapped = false
		}
		if err := g.recheckAttackers(v); err != nil {
			for _, p := range provisional {
				g.Card(p).Tapped = false
			}
			g.combat = prev
			return nil, err
		}
	}
	for _, id := range slices.Clone(g.combat.Attackers) {
		if g.Card(id).Zone != Battlefield {
			g.removeFromCombat(id)
		}
	}
	return g.combat.Attackers, nil
}

// recheckAttackers is the re-validation after an unpaid attacker left
// (PhaseHandler.java:585): the requirements against v, then the alone
// restrictions countViolations' -1 covers.
func (g *Game) recheckAttackers(v attackValidation) error {
	attackers := g.combat.Attackers
	if err := g.checkAttackers(v, attackers, g.combat.AttackTargets); err != nil {
		return err
	}
	return g.attackAloneViolation(attackers)
}

// payAttackCost is checkPropagandaEffects' payCombatCost for one attacker
// (PlayerControllerHuman.java:1814-1819, PlaySpellAbility.java:156-191): a
// non-mana part that cannot be paid fails it before anyone is asked (the
// canPay early bail, :187-191); otherwise its controller is asked, a zero
// cost included, then pays every part.
func (g *Game) payAttackCost(controller PlayerController, attacker CardID, uc unlessCost) bool {
	pid := g.Card(attacker).Controller()
	if !g.unlessPayable(pid, attacker, uc) {
		return false
	}
	a := &Ability{Source: attacker, Controller: pid}
	return controller.ConfirmPayCost(g, pid, uc.parsed, attacker) && g.payUnlessCost(controller, a, pid, uc)
}
