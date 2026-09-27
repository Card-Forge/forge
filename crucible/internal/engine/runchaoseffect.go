package engine

//enginelint:allow id card game ability condition control trigger defined

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/expr"
)

// runChaosEffect is RunChaosEffect.java: each of the cards' chaos abilities
// (CR 311.7's Mode$ ChaosEnsues lines) triggers for the activator -- Pools
// of Becoming's "each of the revealed cards' {CHAOS} abilities triggers".
// Chaos does not ensue: no Mode$ ChaosEnsues run happens, so no other
// card's chaos ability fires, not even the active plane's, and nothing is
// recorded as TriggeredPlayer (Java never calls setTriggeringObjects on the
// copies).
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/RunChaosEffect.java's
// resolve (:18-39): getTargetCards (targets, else Defined$, deduplicated),
// each card's chaos triggers in order, the trigger's ability copied for the
// activator (:23), then orderAndPlaySimultaneousSa (:38): every copy goes
// on the stack, resolving after the rest of this ability's chain, the way
// pushTriggeredAbilities puts simultaneous triggers there (its doc comment:
// no ordering decision exists yet, so they keep the order found). There is no Planechase gate
// (ChaosEnsuesEffect.java:33 has one; this resolve does not) and no
// TriggerZones$ gate: the cards are normally still in the planar deck.
//
// OptionalDecider$ and Cost$ on a chaos trigger are rejected: :27 and :30
// set the optional flag on the RunChaos ability itself rather than on the
// copy, so whether the copy is optional depends on whether an earlier
// ordinary run of that trigger already flagged its shared stored ability
// (forge-java-defects.md).
type runChaosEffect struct{}

// runChaosTriggerParams are the chaos-trigger params this port reads or can
// ignore: the corpus's 158 real Mode$ ChaosEnsues lines use these alone
// (6 add OptionalDecider$, rejected). TriggerZones$ is not a gate here
// (RunChaosEffect.java never checks it); TriggerDescription$ and
// Secondary$ are text.
var runChaosTriggerParams = [...]string{"Mode", "TriggerZones", "Execute", "TriggerDescription", "Secondary"}

func (runChaosEffect) Resolve(g *Game, a *Ability, c PlayerController) error {
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return err
	}
	// Collect every copy before pushing any: a rejected chaos trigger
	// fails the whole resolve before anything reaches the stack (PORT-8).
	var copies []Ability
	seen := make(map[CardID]bool, len(cards))
	for _, id := range cards {
		if seen[id] {
			continue
		}
		seen[id] = true
		h := g.Card(id)
		if h.Def == nil {
			continue
		}
		for _, face := range h.Def.Faces {
			for _, t := range face.Triggers {
				if !isChaosEnsuesTrigger(t) {
					continue
				}
				cp, err := runChaosCopy(t, id, a.Controller, face.Amounts)
				if err != nil {
					return fmt.Errorf("engine: RunChaos: %s: %w", h.Def.Name, err)
				}
				copies = append(copies, cp)
			}
		}
	}
	g.pushTriggeredAbilities(c, copies)
	return nil
}

// runChaosCopy is RunChaosEffect.java:23's
// t.ensureAbility().copy(activator): t's Execute$ ability, hosted by host,
// controlled by activator. A param outside runChaosTriggerParams is an
// error, as is an Execute$ naming no API this port knows.
func runChaosCopy(t *compile.Ability, host CardID, activator PlayerID, amounts map[string]expr.Amount) (Ability, error) {
	for _, p := range t.Params {
		if !runChaosTriggerParam(p.Key) {
			return Ability{}, fmt.Errorf("chaos trigger %s$ not resolvable yet", p.Key)
		}
	}
	for _, sub := range t.Subs {
		if !strings.EqualFold(sub.Key, "Execute") {
			continue
		}
		api, ok := APIByName(sub.Ability.Name)
		if !ok {
			return Ability{}, fmt.Errorf("chaos trigger Execute$ API %q not resolvable yet", sub.Ability.Name)
		}
		return Ability{API: api, Source: host, Controller: activator, Params: sub.Ability, Amounts: amounts}, nil
	}
	return Ability{}, fmt.Errorf("chaos trigger without Execute$ not resolvable yet")
}

func runChaosTriggerParam(key string) bool {
	for _, k := range runChaosTriggerParams {
		if strings.EqualFold(k, key) {
			return true
		}
	}
	return false
}
