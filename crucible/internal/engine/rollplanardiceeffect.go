// RollPlanarDice: CR 901.9's planar die roll, the Mode$ PlanarDice trigger
// and each player's "Planar Dice" Command-zone effect card.

package engine

//enginelint:allow id card game player ability condition control zone trigger replacement effecthelpers statictrigger valid becomemonarcheffect

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
	"github.com/jczastkiewicz/crucible/internal/expr"
)

// planarDieFace is PlanarDice.java's enum: the three faces of the planar
// die, in its declaration order.
type planarDieFace uint8

const (
	planarDiePlaneswalk planarDieFace = iota
	planarDieChaos
	planarDieBlank
)

// String is the face's PlanarDice.java enum name, what Result$ spells.
func (f planarDieFace) String() string {
	switch f {
	case planarDiePlaneswalk:
		return "Planeswalk"
	case planarDieChaos:
		return "Chaos"
	default:
		return "Blank"
	}
}

// planarDieFaceByName is PlanarDice.smartValueOf: the face name, compared
// ignoring case and surrounding space. Java throws for any other name.
func planarDieFaceByName(name string) (planarDieFace, bool) {
	for _, f := range [...]planarDieFace{planarDiePlaneswalk, planarDieChaos, planarDieBlank} {
		if strings.EqualFold(strings.TrimSpace(name), f.String()) {
			return f, true
		}
	}
	return 0, false
}

// rollPlanarDiceEffect is RollPlanarDiceEffect.java: the activator rolls
// the planar die once (PlanarDice.roll). Nothing happens outside a
// Planechase game. Every face runs Mode$ PlanarDice; the planeswalk itself
// is not done here but by the roller's "Planar Dice" effect card's own
// Result$ Planeswalk trigger (Player.createPlanechaseEffects,
// Player.java:3259-3287), so it uses the stack like every other roll
// trigger. A Chaos face also runs Mode$ ChaosEnsues for the roller.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/RollPlanarDiceEffect.java's
// resolve with forge-game/src/main/java/forge/game/PlanarDice.java's roll
// inlined. Refused before the die is rolled, so nothing is consumed from
// the game's random stream: SpecialAction$ (the planar die's own special
// action, which bumps a per-turn counter this port does not keep), any live
// Event$ RollPlanarDice or Event$ PlanarDiceResult replacement (not ported,
// ADR-0029), and any live Mode$ RolledDie or RolledDieOnce trigger (not
// ported, though PlanarDice.roll runs both).
type rollPlanarDiceEffect struct{}

func (rollPlanarDiceEffect) Resolve(g *Game, a *Ability, c PlayerController) error {
	if err := rejectParams(a, "RollPlanarDice", "SpecialAction"); err != nil {
		return err
	}
	if !subAbilityConditionMet(g, g.Card(a.Source), a.Amounts, a.Params) {
		return nil
	}
	if !g.planechaseActive {
		// Not a Planechase game: nothing happens (RollPlanarDiceEffect.java:25).
		return nil
	}
	if err := g.planarDieRollResolvable(); err != nil {
		return err
	}
	roller := a.Controller
	face := g.rollPlanarDie()
	pl := g.Player(roller)
	pl.planarDie = g.putDesignationCard(pl.planarDie, roller, planarDieDef)
	g.runPlanarDieTriggers(c, roller, face)
	return g.TakePendingError()
}

// rollPlanarDie is PlanarDice.roll's single roll (PlanarDice.java:44-53):
// Java's raw nextInt(6) on the game's stream, 0 the planeswalker symbol, 1
// chaos, 2 through 5 blank. Unlike a numbered die (rollDiceFor) no 1 is
// added: the index names a face, not a number.
func (g *Game) rollPlanarDie() planarDieFace {
	switch g.rand.Int32n(6) {
	case 0:
		return planarDiePlaneswalk
	case 1:
		return planarDieChaos
	default:
		return planarDieBlank
	}
}

// planarDieRollResolvable names what, in play, this port cannot roll the
// planar die past, nil when nothing does. A replacement or trigger that
// could not apply anyway (its own conditions false) still refuses: none of
// them is ported far enough to ask.
func (g *Game) planarDieRollResolvable() error {
	for _, event := range [...]string{"RollPlanarDice", "PlanarDiceResult"} {
		name, found := "", false
		g.eachReplacement(event, func(h *Card, _ map[string]expr.Amount, _ *compile.Ability) bool {
			name, found = h.Def.Name, true
			return true
		})
		if found {
			return fmt.Errorf("engine: RollPlanarDice: %q's Event$ %s replacement not resolvable yet", name, event)
		}
	}
	if name, mode, found := g.rolledDieTriggerHost(); found {
		return fmt.Errorf("engine: RollPlanarDice: %q's Mode$ %s trigger not resolvable yet", name, mode)
	}
	return nil
}

// rolledDieZones is where a Mode$ RolledDie/RolledDieOnce host can be: 28
// of the corpus's 31 real lines are Battlefield (one by default), 1
// Command, 2 Graveyard.
var rolledDieZones = [...]ZoneType{Battlefield, Command, Graveyard, Exile, Hand}

// rolledDieTriggerHost finds a card whose Mode$ RolledDie or RolledDieOnce
// line is live where the card is: PlanarDice.roll runs both after
// Mode$ PlanarDice (PlanarDice.java:77-88), neither is ported, and firing
// neither would silently drop them.
func (g *Game) rolledDieTriggerHost() (name, mode string, found bool) {
	for _, pid := range g.Players() {
		for _, z := range rolledDieZones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for _, face := range h.Def.Faces {
					for _, t := range face.Triggers {
						if !strings.EqualFold(t.Name, "RolledDie") && !strings.EqualFold(t.Name, "RolledDieOnce") {
							continue
						}
						if rolledDieTriggerLiveIn(h, t, z) {
							return h.Def.Name, t.Name, true
						}
					}
				}
			}
		}
	}
	return "", "", false
}

// rolledDieTriggerLiveIn reports whether t can fire from zone: an effect
// card's from Command, any other card's from its TriggerZones$, Battlefield
// when it names none (Trigger's default).
func rolledDieTriggerLiveIn(h *Card, t *compile.Ability, zone ZoneType) bool {
	if h.IsEffect {
		return zone == Command
	}
	if _, ok := t.Param("TriggerZones"); !ok {
		return zone == Battlefield
	}
	return phaseTriggerZoneMatches(h, t, zone)
}

// runPlanarDieTriggers is PlanarDice.roll's trigger runs for face
// (PlanarDice.java:73-93): Mode$ PlanarDice, then on a Chaos face
// Mode$ ChaosEnsues with the roller as AbilityKey.Player. The RolledDie and
// RolledDieOnce runs between them are refused before the roll
// (planarDieRollResolvable).
//
// Static$ True lines resolve inline, in run order, through
// resolveStaticTriggers (ADR-0020). The other matches of both runs are
// pushed together, not run by run: Java collects every trigger a run fires
// as one simultaneous batch, put on the stack in APNAP order
// (MagicStack.addAllTriggeredAbilitiesToStack), so a nonactive player's
// "whenever you roll the planar die" does not go under the active player's
// chaos ability. The ChaosEnsues half is checkChaosEnsuesTriggers' own
// walk, chaosEnsuesTriggerMatches.
func (g *Game) runPlanarDieTriggers(c PlayerController, roller PlayerID, face planarDieFace) {
	g.resolveStaticTriggers(c, g.planarDiceTriggerMatches(roller, face, true))
	pending := g.planarDiceTriggerMatches(roller, face, false)
	if face == planarDieChaos {
		g.resolveStaticTriggers(c, g.chaosEnsuesTriggerMatches(roller, NoCard, true))
		pending = append(pending, g.chaosEnsuesTriggerMatches(roller, NoCard, false)...)
	}
	g.pushTriggeredAbilities(c, pending)
}

// planarDiceTriggerMatches collects the Mode$ PlanarDice lines
// (TriggerPlanarDice.performTest) that fire for roller rolling face,
// Static$ True ones only or the rest only, as static says. Hosts are
// planeswalkTriggerZones' -- every player's Battlefield and whole Command
// zone -- TriggerZones$ gating each line (phaseTriggerZoneMatches); all 5
// real lines are TriggerZones$ Command, on planes. ValidPlayer$ is matched
// against roller, an unrecognized spec never firing (checkChaosEnsuesTriggers'
// rule); Result$ against face. A Result$ naming no face is an error, as
// Java's smartValueOf throws. Every match records roller as TriggeredPlayer
// (setTriggeringObjectsFrom(runParams, AbilityKey.Player)).
func (g *Game) planarDiceTriggerMatches(roller PlayerID, face planarDieFace, static bool) []Ability {
	var matches []Ability
	objects := triggeredObjects{player: roller}
	for _, pid := range g.Players() {
		for _, z := range planeswalkTriggerZones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for _, cardFace := range h.Def.Faces {
					for _, t := range cardFace.Triggers {
						if !strings.EqualFold(t.Name, "PlanarDice") || isStaticTrigger(t) != static {
							continue
						}
						if !phaseTriggerZoneMatches(h, t, z) || !g.planarDiceTriggerTest(h, host, t, roller, face) {
							continue
						}
						if sub, api, optional, ok := triggerEffectAPI(g, h, cardFace.Amounts, t); ok {
							matches = append(matches, Ability{API: api, Source: host, Controller: h.Controller(), Params: sub, Amounts: cardFace.Amounts, Optional: optional, triggered: objects})
						}
					}
				}
			}
		}
	}
	return matches
}

// planarDiceTriggerTest is TriggerPlanarDice.performTest: ValidPlayer$
// against the roller, then Result$ against the face rolled.
func (g *Game) planarDiceTriggerTest(h *Card, host CardID, t *compile.Ability, roller PlayerID, face planarDieFace) bool {
	if spec, ok := t.Param("ValidPlayer"); ok {
		matched, recognized := matchesPlayerSpec(g, roller, h.Controller(), host, spec)
		if !recognized || !matched {
			return false
		}
	}
	want, ok := t.Param("Result")
	if !ok {
		return true
	}
	cond, known := planarDieFaceByName(want)
	if !known {
		g.recordPendingError(fmt.Errorf("engine: %q's Mode$ PlanarDice Result$ %q names no planar die face", h.Def.Name, want))
		return false
	}
	return cond == face
}

// planarDieDef is the card Player.createPlanechaseEffects builds
// (Player.java:3259-3287): "Planar Dice", with its one trigger. Java parses
// it from strings at game start; this port builds the same tree directly,
// the way monarchEffectDef does, so nothing is parsed at runtime (PORT-2):
//
//	Mode$ PlanarDice | Result$ Planeswalk | TriggerZones$ Command | ValidPlayer$ You | Secondary$ True
//	  -> DB$ Planeswalk | Cause$ PlanarDie
//
// Java's card also carries the planar die's special action (ST$
// RollPlanarDice | Cost$ X | SpecialAction$ True, CR 901.9's "roll the
// planar die" any number of times for {X}); that action is not ported, so
// the card here carries the trigger alone.
func planarDieDef() *compile.Card {
	walk := designationTrigger("PlanarDice", []vocab.Param{
		{Key: "Result", Value: "Planeswalk"}, {Key: "TriggerZones", Value: "Command"},
		{Key: "ValidPlayer", Value: "You"}, {Key: "Secondary", Value: "True"},
	}, "Planeswalk", []vocab.Param{{Key: "Cause", Value: "PlanarDie"}})
	return designationDef("Planar Dice", walk)
}
