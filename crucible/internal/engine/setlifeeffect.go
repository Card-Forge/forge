// SetLife: CR 119.4, M6's own next script-driven effect -- 39 real
// (AB|DB)$ SetLife lines, 31 naming Defined$ and 5 naming ValidTgts$
// instead -- SpellAbilityEffect.getTargetPlayers(sa)'s own either/or
// contract, targetedOrDefinedPlayers (defined.go), millEffect's own first
// caller (milleffect.go) reused wholesale here, "You" the identical default
// when neither is present.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/LifeSetEffect.java's
// resolve (the plain branch; Redistribute$'s own multi-player life-swap
// shape, below, is not) and Player.setLife's own CR 119.5 dispatch: a life
// total set ABOVE its current value is a life GAIN (gainLifeEffect's own
// gainLifePrevented/gainLifeReplaced/checkLifeGainedTriggers machinery,
// gainlifeeffect.go, reused directly rather than re-derived -- Player.
// setLife's own Java body IS exactly `newLife > life ? gainLife(...) :
// newLife < life ? loseLife(...) : no-op`, so this file has nothing of its
// own to port for either direction); set BELOW is a life LOSS
// (loseLifeEffect's own plain `Life -= amount`, no prevention machinery
// built, loselifeeffect.go's own identical gap); set to the SAME value is
// no event at all, CR 119.5's own explicit carve-out.

package engine

//enginelint:allow id card game player ability defined condition control amount event

import "fmt"

// setLifeUnresolvedParams names LifeSetEffect's own params this port does
// not evaluate. Every one fails the whole line loudly (PORT-8/GO-7):
// Redistribute$ (2) -- getDistribution's own multi-player life-total-swap
// solver, a further mechanic entirely; PlayerChoices$/ChoicePrompt$/
// ChoiceAmount$ (2/2/2) -- an interactive "choose which player(s)" pick,
// this port's own PlayerController has no hook for it; Ultimate$ (1) -- a
// planeswalker-ultimate-specific flag, its own further mechanic;
// Condition$/ConditionDefined$ (0/1) -- condition.go's own
// subAbilityConditionMet would otherwise silently no-op a card naming
// either, dealDamageEffect's own identical reasoning.
var setLifeUnresolvedParams = [...]string{
	"Redistribute", "PlayerChoices", "ChoicePrompt", "ChoiceAmount",
	"Ultimate"}

// setLifeEffect resolves Mode$/DB$/AB$ SetLife for the plain, non-
// Redistribute$ shape. ConditionPresent$/ConditionCompare$/
// ConditionCheckSVar$/ConditionSVarCompare$ are resolved through
// subAbilityConditionMet (condition.go), the identical way every other M6
// effect's own does.
type setLifeEffect struct{}

func (setLifeEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range setLifeUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: SetLife: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	lifeAmount, ok := a.Params.Param("LifeAmount")
	if !ok {
		return fmt.Errorf("engine: SetLife: LifeAmount$ missing")
	}
	newLife, ok := resolveNamedAmount(g, a.Amounts, source, lifeAmount)
	if !ok {
		return fmt.Errorf("engine: SetLife: LifeAmount$ %q is not resolvable", lifeAmount)
	}

	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: SetLife: %w", err)
	}

	for _, pid := range players {
		setPlayerLife(g, controller, a.Source, pid, newLife)
	}
	return nil
}

// setPlayerLife is Player.setLife's own CR 119.5 dispatch -- above zero
// cards more than its current life is a GAIN (gainLifeEffect's own
// gainLifePrevented/gainLifeReplaced/checkLifeGainedTriggers machinery,
// gainlifeeffect.go, reused directly), below is a LOSS (loseLifeEffect's
// own plain subtract, no prevention machinery built, loselifeeffect.go's
// own identical gap), and the same value is no event at all. Shared with
// exchangeLifeEffect (exchangelifeeffect.go), CR 119.10's own "the loser of
// the exchange has their life SET to the winner's post-loss total" reduced
// to the identical two-branch dispatch.
func setPlayerLife(g *Game, controller PlayerController, source CardID, pid PlayerID, newLife int) {
	p := g.Player(pid)
	switch {
	case newLife > p.Life:
		g.gainLife(controller, pid, newLife-p.Life, source, nil)
	case newLife < p.Life:
		lost := p.Life - newLife
		p.Life = newLife
		g.sink.Emit(Event{Kind: LifeChanged, Source: source, Target: PlayerEntity(pid), Amount: -int32(lost)})
	}
}
