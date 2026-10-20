// ExchangeLife: CR 119.10, M6's own next script-driven effect -- 6 real
// (AB|DB)$ ExchangeLife lines, every one naming ValidTgts$ (a two-target
// "two target players exchange life totals" shape, or a one-target
// "exchange life totals with target opponent/player" shape against the
// ability's own activator) -- SpellAbilityEffect.getTargetPlayers(sa)'s own
// contract, targetedOrDefinedPlayers (defined.go), millEffect's own first
// caller (milleffect.go) reused wholesale here.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/LifeExchangeEffect.java's
// resolve: whichever of the two players has MORE life loses the
// difference, the other gains it -- CR 119.10's own net effect of an
// exchange, reusing setLifeEffect's own gain/loss halves directly (Player.
// setLife's own CR 119.5 dispatch, setlifeeffect.go's own doc comment has
// the reason a life change this port ports at all always splits into
// exactly those two shapes) rather than a third, redundant copy of the
// identical gainLifePrevented/gainLifeReplaced/checkLifeGainedTriggers
// sequence.

package engine

//enginelint:allow id card game player ability defined condition control setlifeeffect

import "fmt"

// exchangeLifeUnresolvedParams names LifeExchangeEffect's own params this
// port does not evaluate. Every one fails the whole line loudly
// (PORT-8/GO-7): RememberOwnLoss$/RememberDifference$ (1/1) -- Java's own
// `source.addRemembered(lost)`/`addRemembered(p1.getLife() - p2.getLife())`
// remember a raw integer, not an EntityID Memory.Remember (memory.go) has
// anywhere to put, removeCounterEffect's own identical RememberAmount$ gap
// (removecountereffect.go); Condition$/ConditionDefined$ (0/0, listed
// defensively) -- condition.go's own subAbilityConditionMet would
// otherwise silently no-op a card naming either, dealDamageEffect's own
// identical reasoning.
var exchangeLifeUnresolvedParams = [...]string{
	"RememberOwnLoss", "RememberDifference"}

// exchangeLifeEffect resolves Mode$/DB$/AB$ ExchangeLife. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type exchangeLifeEffect struct{}

func (exchangeLifeEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range exchangeLifeUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: ExchangeLife: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: ExchangeLife: %w", err)
	}

	var p1, p2 PlayerID
	switch len(players) {
	case 0:
		return nil
	case 1:
		p1, p2 = a.Controller, players[0]
	default:
		p1, p2 = players[0], players[1]
	}
	if p1 == p2 {
		return nil
	}

	life1, life2 := g.Player(p1).Life, g.Player(p2).Life
	diff := life1 - life2
	loser, gainer := p1, p2
	if diff < 0 {
		diff = -diff
		loser, gainer = p2, p1
	}
	if diff == 0 {
		return nil
	}

	setPlayerLife(g, controller, a.Source, loser, g.Player(loser).Life-diff)
	setPlayerLife(g, controller, a.Source, gainer, g.Player(gainer).Life+diff)
	return nil
}
