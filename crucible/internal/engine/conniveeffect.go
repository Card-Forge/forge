// Connive: CR 702.164 -- 51 real (AB|DB)$ Connive lines, 12 naming
// ValidTgts$ and 7 Defined$, both through targetedOrDefinedCards
// (defined.go, default "Self"); ConniveNum$ (3, default 1) resolves through
// resolveNamedAmount. A composite of three already-built primitives rather
// than a new mechanic: Game.DrawCards (turn.go), ChooseCardsToDiscard/
// discardCards (control.go/discardeffect.go) and Counters.Add
// (counters.go) -- "draw a card, discard a card, if you discarded a
// nonland card put a +1/+1 counter on this creature" reduced to one call
// of each per conniver.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/ConniveEffect.java's
// resolve, trimmed hard: that file groups connivers by controller in APNAP
// turn order and lets the controller choose which of several connives to
// resolve first when more than one shares a controller -- this file
// resolves each targeted/defined card independently in target order
// instead, millEffect's own identical "Players()' own fixed seat order
// rather than turn order" simplification (defined.go's own doc comment on
// targetedOrDefinedPlayers) applied to a card list rather than a player
// one: which physical card's own draw-then-discard happens first never
// changes any of their own outcomes, since each only ever reads its own
// controller's hand. ReplacementType.Connive (a replacement effect keyed to
// this API specifically) is not ported -- no real corpus card defines one,
// so NotReplaced is the only real outcome regardless.

package engine

//enginelint:allow id card game player ability defined condition control amount parts event zone turn discardeffect

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// conniveUnresolvedParams names ConniveEffect's own params this port does
// not evaluate. Every one fails the whole line loudly (PORT-8/GO-7):
// Condition$/ConditionDefined$ (0/0,
// defensive) -- condition.go's own subAbilityConditionMet would otherwise
// silently no-op a card naming either without ConditionPresent$ alongside
// it, dealDamageEffect's own identical reasoning.
var conniveUnresolvedParams = [...]string{

	"Condition"}

// conniveEffect resolves Mode$/DB$/AB$ Connive. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way every
// other M6 effect's own does.
type conniveEffect struct{}

func (conniveEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range conniveUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Connive: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	numParam, ok := a.Params.Param("ConniveNum")
	if !ok {
		numParam = "1"
	}
	num, ok := resolveNamedAmount(g, a.Amounts, source, numParam)
	if !ok {
		return fmt.Errorf("engine: Connive: ConniveNum$ %q is not resolvable", numParam)
	}

	connivers, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Connive: %w", err)
	}

	for _, id := range connivers {
		pid := g.Card(id).Controller()
		g.DrawCards(pid, num, controller)

		hand := g.Zone(Hand, pid).Cards()
		amt := num
		if amt > len(hand) {
			amt = len(hand)
		}
		if amt == 0 {
			continue
		}
		chosen := controller.ChooseCardsToDiscard(g, pid, hand, amt)
		nonLands := 0
		for _, cid := range chosen {
			if !g.Card(cid).Type().Has(cardtype.Land) {
				nonLands++
			}
		}
		discardCards(g, controller, chosen, pid)

		if nonLands > 0 && g.Card(id).Zone == Battlefield {
			// The conniver's controller places (ConniveEffect.java:64, :99).
			g.addCardCounters(controller, pid, a.Source, id, P1P1, nonLands)
		}
	}
	return nil
}
