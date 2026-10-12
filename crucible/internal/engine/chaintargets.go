// Targets of a SubAbility$ chain: CR 601.2c chooses every target of a spell
// or ability as it is put on the stack, the sub-abilities' included.
//
// Ported from the loop in forge-game's PlayerControllerHuman/HumanPlay
// target selection, which walks sa.getSubAbility() and runs TargetSelection
// for each link that usesTargeting().

package engine

//enginelint:allow id game ability control subability targeting

import "github.com/jczastkiewicz/crucible/internal/carddb/compile"

// resolveChainTargets chooses the targets of every sub-ability of a that
// names its own ValidTgts$ (Stolen Uniform's "target Equipment"), in chain
// order after a's own, and stores them on a. It reports whether a can still
// be put on the stack: false when a link has no legal target, as for a's
// own (resolveTargets). A link this port cannot ask about records the error
// to fail the ability when it resolves, as resolveTargets does.
func (g *Game) resolveChainTargets(controller PlayerController, a *Ability) bool {
	cur := a.Params
	// What Defined$ ParentTarget and TargetsWithDefinedController$ name for a
	// link: the targets of the nearest ancestor that chose some
	// (SpellAbility.getParentTargetingCard), a's own first.
	parents := parentTargetsOf(a)
	for cur != nil {
		sub, ok := findSubAbility(cur)
		if !ok {
			return true
		}
		cur = sub.Ability
		api, ok := APIByName(sub.Ability.Name)
		if !ok {
			return true
		}
		// The link carries its root's trigger context and casting state, as
		// Java's sub-ability reads them off getRootAbility: a Charm mode's
		// link is asked about as its mode is.
		link := Ability{API: api, Source: a.Source, Controller: a.Controller, Params: sub.Ability, Amounts: a.Amounts,
			TriggerRemembered: a.TriggerRemembered, triggered: a.triggered, casting: a.casting, charmRoot: a.charmRoot,
			parentTargets: parents}
		choice, named, ok := g.targetChoiceFor(&link)
		if !named {
			continue
		}
		if !ok {
			return false
		}
		if choice.err != nil {
			a.targetsErr = choice.err
			return true
		}
		// TargetingPlayer$ on the link names who chooses (Magus of the Arena).
		decider, err := g.targetingPlayerOf(&link)
		if err != nil {
			return false
		}
		chosen := controller.ChooseTargets(g, decider, choice.candidates, choice.min, choice.max)
		a.chainTargets = append(a.chainTargets, chainTarget{Params: sub.Ability, Targets: chosen})
		if len(chosen) > 0 {
			parents = chosen
		}
	}
	return true
}

// chooseAbilityTargets is the "choose modes and targets" step of putting a on
// the stack, ahead of any cost (PlaySpellAbility.java:675-683: announceType,
// announceValuesLikeX, setupTargets, then CostPayment). It marks a so
// pushTriggeredAbilities does not choose again, and reports whether a can still
// be put on the stack: false for a Charm the controller declined or a missing
// legal target (CR 601.2c).
func (g *Game) chooseAbilityTargets(controller PlayerController, a *Ability) bool {
	if a.API == APICharm {
		ok, err := g.chooseCharmModes(controller, a)
		if err != nil {
			a.modesErr = err
		} else if !ok {
			return false
		}
	}
	if !g.resolveTargets(controller, a) || !g.resolveChainTargets(controller, a) {
		return false
	}
	a.targetsChosen = true
	return true
}

// chainTargetsFor is the targets chosen for the sub-ability link params, and
// whether any were. A Charm mode carries its own links' (chooseCharmModes), a
// copy its original's (copySpell); a link no one chose targets for (one
// naming no ValidTgts$) keeps its parent's.
func (a *Ability) chainTargetsFor(link *compile.Ability) ([]EntityID, bool) {
	for _, c := range a.chainTargets {
		if c.Params == link {
			return c.Targets, true
		}
	}
	return nil, false
}
