package engine

//enginelint:allow id card game ability defined condition control amount parts additional effecthelpers unlesscost

import "fmt"

// genericChoiceUnresolvedParams are ChooseGenericEffect.java's params this
// port cannot honour yet: NumRandomChoices$ (a random subset first), Guess$,
// and the batched damage/zone tables (DamageMap$, ChangeZoneTable$).
// SetChosenMode$ records the pick on the host (ChooseGenericEffect.java:
// 120-122); LockInText$ is display text the statics also carry; Secretly$
// only gathers the picks into one notification (:116-119, :143-145).
// AtRandom$ True picks with Aggregates.random(choices, ChoiceAmount$) (:86-89);
// AtRandom$ Urza re-draws targeting choices with no candidates (:91-100) and
// is refused.
var genericChoiceUnresolvedParams = [...]string{
	"NumRandomChoices", "Guess",
	"DamageMap", "ChangeZoneTable",
	"Condition", "Ultimate",
}

// genericChoiceEffect is ChooseGenericEffect.java: each Defined$/ValidTgts$
// player (default You) picks ChoiceAmount$ (default 1) of the Choices$
// abilities (ChooseAbilitiesForEffect) and each chosen one resolves in
// order. TempRemember$ swaps the chooser in as the host's only remembered
// player for the duration, as Java's own oldRem swap does.
//
// Java first drops choices whose UnlessCost$ the chooser cannot pay
// (ChooseGenericEffect.java:69-80: Cost.canPay for the chooser, never
// UnlessPayer$ or UnlessSwitched$; a mana part always passes,
// CostPartMana.java:118-120), falling back to FallbackAbility$ when none
// remain (:128-137). A chosen ability then pays or not through its own
// UnlessCost$ as it resolves.
type genericChoiceEffect struct{}

func (genericChoiceEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range genericChoiceUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: GenericChoice: %s$ not resolvable yet", key)
		}
	}
	random, atRandom := a.Params.Param("AtRandom")
	if atRandom && random != "True" {
		return fmt.Errorf("engine: GenericChoice: AtRandom$ %s not resolvable yet", random)
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	choices := additionalAbilities(a.Params, "Choices")
	unless := make([]unlessCost, len(choices))
	hasUnless := make([]bool, len(choices))
	for i, c := range choices {
		raw, ok := c.Ability.Param("UnlessCost")
		if !ok {
			continue
		}
		uc, ok := parseUnlessCost(raw)
		if !ok {
			return fmt.Errorf("engine: GenericChoice: choice %s UnlessCost$ %q not resolvable yet", c.SVar, raw)
		}
		// Mana is never filtered; unlessPayable reads the other parts only.
		unless[i], hasUnless[i] = uc, true
	}
	amountRaw, ok := a.Params.Param("ChoiceAmount")
	if !ok {
		amountRaw = "1"
	}
	wanted, ok := resolveNamedAmount(g, a.Amounts, source, amountRaw)
	if !ok {
		return fmt.Errorf("engine: GenericChoice: ChoiceAmount$ %q not resolvable yet", amountRaw)
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: GenericChoice: %w", err)
	}
	_, tempRem := a.Params.Param("TempRemember")
	_, setMode := a.Params.Param("SetChosenMode")
	for _, p := range players {
		// avail indexes the choices p may take; picks index into it.
		var avail []int
		var names []string
		for i, c := range choices {
			if hasUnless[i] && !g.unlessPayable(p, a.Source, unless[i]) {
				continue
			}
			avail = append(avail, i)
			names = append(names, c.SVar)
		}
		amount := min(wanted, len(avail))
		var picked []int
		if len(avail) > 0 {
			if atRandom {
				picked = g.randomSample(len(avail), amount)
			} else {
				picked = controller.ChooseAbilitiesForEffect(g, p, a.Source, names, amount)
				all := make([]int, len(avail))
				for i := range all {
					all[i] = i
				}
				if err := checkChoice(picked, all, amount, amount); err != nil {
					return fmt.Errorf("engine: GenericChoice: %w", err)
				}
			}
			for k, i := range picked {
				picked[k] = avail[i]
			}
		}
		if len(picked) == 0 {
			// ChooseGenericEffect.java:128-137: nothing chosen resolves the
			// FallbackAbility$, and does nothing without one.
			if err := g.resolveAdditionalKey(a, controller, "FallbackAbility"); err != nil {
				return err
			}
			continue
		}
		var oldRem []EntityID
		if tempRem {
			oldRem = swapRememberedPlayer(&source.Memory, p)
		}
		for _, i := range picked {
			if setMode {
				// The chosen ability's description is the mode
				// (chosenSA.getDescription(), its SpellDescription$).
				desc, _ := choices[i].Ability.Param("SpellDescription")
				g.Card(a.Source).Memory.SetChosenMode(desc)
			}
			if err := g.resolveAdditional(a, controller, choices[i]); err != nil {
				return err
			}
		}
		if tempRem {
			restoreRememberedPlayers(&source.Memory, p, oldRem)
		}
	}
	return nil
}
