// TakeInitiative: the initiative designation (CR 725), its "The
// Initiative" Command-zone effect card, and the Mode$ TakesInitiative and
// Mode$ DamageDoneOnceByController triggers it runs on.

package engine

//enginelint:allow id zone card game player ability defined condition control parts valid trigger effecteffect effecthelpers earthbendeffect becomemonarcheffect

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
)

// takeInitiativeEffect is TakeInitiativeEffect.java: each targeted or
// Defined$ player (default the activator) still in the game takes the
// initiative (GameAction.takeInitiative) -- even one who already has it,
// which still ventures into Undercity.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/TakeInitiativeEffect.java's
// resolve.
type takeInitiativeEffect struct{}

func (takeInitiativeEffect) Resolve(g *Game, a *Ability, c PlayerController) error {
	if err := rejectParams(a, "TakeInitiative", "ConditionDefined"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: TakeInitiative: %w", err)
	}
	for _, pid := range players {
		if g.Player(pid).Lost {
			continue
		}
		g.takeInitiative(c, pid)
	}
	return nil
}

// Initiative is the player who has the initiative (CR 725), NoPlayer while
// nobody does.
func (g *Game) Initiative() PlayerID { return g.initiative }

// SetInitiative gives p the initiative as a restored game state would
// have it: the designation and p's "The Initiative" card, with no trigger
// run -- SetMonarch's counterpart. NoPlayer clears it.
func (g *Game) SetInitiative(p PlayerID) {
	if g.initiative != NoPlayer {
		g.removeDesignationCard(g.Player(g.initiative).initiativeEffect)
	}
	g.initiative = p
	if p != NoPlayer {
		pl := g.Player(p)
		pl.initiativeEffect = g.putDesignationCard(pl.initiativeEffect, p, initiativeEffectDef)
	}
}

// takeInitiative is GameAction.takeInitiative (GameAction.java:2557-2579):
// a player who does not have the initiative takes it -- the previous
// holder's card leaves, p's enters -- and Mode$ TakesInitiative triggers
// run either way (CR 725.2: you can take the initiative even if you
// already have it).
//
// A player who has lost is Java's hasLost branch: the next player in turn
// order takes the initiative, and then, with no return after that call,
// the lost player takes it too (GameAction.java:2568-2573). Reproduced as
// written, a Forge defect (forge-java-defects.md) this port does not
// correct: reached only from onPlayersLost when the active player loses in
// the same state-based-action pass as the holder. The recursion ends, as
// Java's does, at the holder (still in the game until processed) or at a
// player who has not lost.
func (g *Game) takeInitiative(c PlayerController, p PlayerID) {
	previous := g.initiative
	if p == NoPlayer {
		return
	}
	if p != previous {
		if previous != NoPlayer {
			g.removeDesignationCard(g.Player(previous).initiativeEffect)
		}
		if g.Player(p).Lost {
			g.takeInitiative(c, g.nextInGameAfter(p))
		}
		g.initiative = p
		pl := g.Player(p)
		pl.initiativeEffect = g.putDesignationCard(pl.initiativeEffect, p, initiativeEffectDef)
	}
	g.pushPlayerTriggers(c, p, g.playerActionTriggerMatches(p, nil, "TakesInitiative"))
}

// initiativeEffectDef is the card Player.createInitiativeEffect builds
// (Player.java:3494-3541): "The Initiative", its three triggers in Java's
// order, built as trees the way monarchEffectDef is (PORT-2):
//
//	Mode$ DamageDoneOnceByController | ValidSource$ Player | ValidTarget$ You | CombatDamage$ True | TriggerZones$ Command
//	  -> DB$ TakeInitiative | Defined$ TriggeredSource
//	Mode$ Phase | Phase$ Upkeep | TriggerZones$ Command | ValidPlayer$ You | Secondary$ True
//	  -> DB$ Venture | Dungeon$ Undercity
//	Mode$ TakesInitiative | ValidPlayer$ You | TriggerZones$ Command
//	  -> DB$ Venture | Dungeon$ Undercity
func initiativeEffectDef() *compile.Card {
	take := designationTrigger("DamageDoneOnceByController", []vocab.Param{
		{Key: "ValidSource", Value: "Player"}, {Key: "ValidTarget", Value: "You"},
		{Key: "CombatDamage", Value: "True"}, {Key: "TriggerZones", Value: "Command"},
	}, "TakeInitiative", []vocab.Param{{Key: "Defined", Value: "TriggeredSource"}})
	venture := []vocab.Param{{Key: "Dungeon", Value: "Undercity"}}
	upkeep := designationTrigger("Phase", []vocab.Param{
		{Key: "Phase", Value: "Upkeep"}, {Key: "TriggerZones", Value: "Command"},
		{Key: "ValidPlayer", Value: "You"}, {Key: "Secondary", Value: "True"},
	}, "Venture", venture)
	taken := designationTrigger("TakesInitiative", []vocab.Param{
		{Key: "ValidPlayer", Value: "You"}, {Key: "TriggerZones", Value: "Command"},
	}, "Venture", venture)
	return designationDef("The Initiative", take, upkeep, taken)
}

// checkDamageDoneOnceByControllerTriggers is Mode$
// DamageDoneOnceByController (TriggerDamageDoneOnceByController), off the
// same damage table DamageDoneOnce reads: for each damaged target, in
// first-seen order, once per distinct controller of the sources that dealt
// it damage, in first-seen order (CardDamageTable.java:77-104) --
// ValidTarget$ against the target, ValidSource$ against that controller,
// CombatDamage$ against isCombat. The controller is read now, at damage
// time (Java's table is keyed by LKI copies), and recorded as the
// trigger's Source (Defined$ TriggeredSource).
func (g *Game) checkDamageDoneOnceByControllerTriggers(c PlayerController, table damageTable, isCombat bool) {
	var targets []EntityID
	byTarget := map[EntityID][]PlayerID{}
	for _, e := range table {
		ctrls, seen := byTarget[e.Target]
		if !seen {
			targets = append(targets, e.Target)
		}
		ctrl := g.Card(e.Source).Controller()
		if !containsPlayer(ctrls, ctrl) {
			byTarget[e.Target] = append(ctrls, ctrl)
		}
	}
	var matches []Ability
	for _, target := range targets {
		for _, ctrl := range byTarget[target] {
			for _, pid := range g.Players() {
				for _, host := range g.traitHosts(pid) {
					h := g.Card(host)
					if h.Def == nil {
						continue
					}
					for face := range h.triggerFaces {
						for _, t := range face.Triggers {
							if !strings.EqualFold(t.Name, "DamageDoneOnceByController") {
								continue
							}
							if cd, ok := t.Param("CombatDamage"); ok && strings.EqualFold(cd, "True") != isCombat {
								continue
							}
							if spec, ok := t.Param("ValidTarget"); ok && !attackedTargetMatches(g, h, []EntityID{target}, spec) {
								continue
							}
							if spec, ok := t.Param("ValidSource"); ok {
								matched, recognized := matchesPlayerSpec(g, ctrl, h.Controller(), host, spec)
								if !recognized || !matched {
									continue
								}
							}
							if sub, api, optional, ok := triggerEffectAPI(g, h, face.Amounts, t); ok {
								matches = append(matches, Ability{API: api, Source: host, Controller: h.Controller(),
									Params: sub, Amounts: face.Amounts, Optional: optional,
									triggered: face.objects(triggeredObjects{source: PlayerEntity(ctrl), sourceController: ctrl})})
							}
						}
					}
				}
			}
		}
	}
	g.pushTriggeredAbilities(c, matches)
}

// containsPlayer reports whether list holds p.
func containsPlayer(list []PlayerID, p PlayerID) bool {
	for _, q := range list {
		if q == p {
			return true
		}
	}
	return false
}
