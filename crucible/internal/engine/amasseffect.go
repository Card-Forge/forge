package engine

//enginelint:allow ability animate card condition control defined effecthelpers event game id parts token zone

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// amassEffect is AmassEffect.java (CR 701.47): the first target or Defined$
// player (default You) creates a 0/0 black <Type> Army when they control no
// Army, then puts Num$ (default 1) +1/+1 counters on an Army they control,
// which becomes a <Type> in addition to its other types if it is not one.
//
// Java builds the Army from b_0_0_army and rewrites its creature types and
// name; every real Type$ (Goblin, Orc, Sliver, Zombie) has its own script
// (b_0_0_<type>_army) with exactly that result, so this port uses it and
// fails closed on a Type$ without one. The "becomes a <Type>" command-zone
// effect lasts as long as the Army stays, the Permanent animateRecord
// clearAnimates drops when it leaves.
type amassEffect struct{}

func (amassEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "Amass"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return err
	}
	players = g.inAPNAPOrder(players)
	if len(players) == 0 {
		return nil
	}
	amasser := players[0]
	num, err := optionalAmount(g, a, "Amass", "Num", 1)
	if err != nil {
		return err
	}
	kind, ok := a.Params.Param("Type")
	if !ok {
		return fmt.Errorf("engine: Amass: no Type$")
	}

	if len(armies(g, amasser)) == 0 {
		def, err := tokenScript(g, "b_0_0_"+strings.ToLower(kind)+"_army")
		if err != nil {
			return err
		}
		id := g.createToken(controller, tokenSpec{Def: def, Owner: amasser})
		g.checkChangesZoneAllTriggers(controller, []CardID{id}, None, Battlefield)
	}
	candidates := armies(g, amasser)
	if len(candidates) == 0 {
		return nil
	}
	army := candidates[0]
	if len(candidates) > 1 {
		chosen := controller.ChooseCardsForEffect(g, amasser, a.Source, candidates, 1, 1)
		if err := checkChoice(chosen, candidates, 1, 1); err != nil {
			return fmt.Errorf("engine: Amass: %w", err)
		}
		army = chosen[0]
	}
	if hasParam(a, "RememberAmass") {
		source.Memory.Remember(CardEntity(army))
	}
	// The amassing player places (AmassEffect.java:52, :102).
	g.addCardCounters(controller, amasser, a.Source, army, P1P1, num)
	if !g.Card(army).Type().HasSubtype(kind) {
		g.timestamp++
		g.addAnimate(animateRecord{
			Card: army, Timestamp: g.timestamp, Permanent: true,
			Types: TypeEffect{AddTypes: cardtype.ParseToken(kind)},
		})
	}
	return nil
}

// armies is every Army p controls.
func armies(g *Game, p PlayerID) []CardID {
	var out []CardID
	for _, id := range g.Zone(Battlefield, p).Cards() {
		if g.Card(id).Type().HasSubtype("Army") {
			out = append(out, id)
		}
	}
	return out
}
