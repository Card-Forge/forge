// RingTemptsYou: the Ring tempts you (CR 701.54), "The Ring" Command-zone
// effect card whose abilities grow with each temptation, the Ring-bearer
// designation and the Mode$ RingTemptsYou trigger.

package engine

//enginelint:allow id zone card game player ability additional condition control parts valid trigger effecthelpers becomemonarcheffect earthbendeffect

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// ringTemptsYouEffect is RingTemptsYouEffect.java: the activator's Ring
// tempts them. Their "The Ring" effect card is made on first use
// (Player.createTheRing), the temptation count goes up and the Ring gains
// that level's abilities (Player.setRingLevel), the activator chooses a
// creature they control as their Ring-bearer (they may keep the same one),
// and Mode$ RingTemptsYou triggers run.
//
// ConditionDefined$ is rejected: subAbilityConditionMet reads a line naming
// it as unmet, which would silently skip the effect. A Mode$ RingTemptsYou
// trigger in play whose Execute$ carries a Cost$ is rejected too, before
// anything happens: this port's trigger resolution does not ask for or pay
// a triggered ability's own cost, so resolving one would hand out its
// effect for free.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/RingTemptsYouEffect.java's
// resolve.
type ringTemptsYouEffect struct{}

func (ringTemptsYouEffect) Resolve(g *Game, a *Ability, c PlayerController) error {
	if err := rejectParams(a, "RingTemptsYou", "ConditionDefined"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	p := a.Controller
	if err := g.ringTemptsYouTriggersResolvable(p); err != nil {
		return fmt.Errorf("engine: RingTemptsYou: %w", err)
	}
	g.temptWithRing(p, g.Player(p).ringTempted+1)

	candidates := g.ringBearerCandidates(p)
	bearer := NoCard
	switch len(candidates) {
	case 0:
	case 1:
		bearer = candidates[0]
	default:
		chosen := c.ChooseCardsForEffect(g, p, a.Source, candidates, 1, 1)
		if err := checkChoice(chosen, candidates, 1, 1); err != nil {
			return fmt.Errorf("engine: RingTemptsYou: %w", err)
		}
		bearer = chosen[0]
	}
	// Player.setRingBearer(null) returns early: with no creature to choose,
	// the previous Ring-bearer, if any, stays.
	if bearer != NoCard {
		g.Player(p).ringBearer = bearer
	}
	g.checkRingTemptsYouTriggers(c, p, bearer)
	return nil
}

// RingTemptedYou is how many times the Ring has tempted p
// (Player.getNumRingTemptedYou), the count its level follows.
func (g *Game) RingTemptedYou(p PlayerID) int { return g.Player(p).ringTempted }

// RingBearer is p's Ring-bearer (Player.getRingBearer), NoCard while p has
// none: never chosen, or the creature left the battlefield or came under
// another player's control (CR 701.54a).
func (g *Game) RingBearer(p PlayerID) CardID {
	id := g.Player(p).ringBearer
	if id == NoCard {
		return NoCard
	}
	if c := g.Card(id); c.Zone != Battlefield || c.Controller() != p {
		return NoCard
	}
	return id
}

// SetRingTemptedYou restores a temptation count as a saved game state has
// it -- GameState.applyPlayer's numringtemptedyou: the count, and, when it is
// above zero, p's "The Ring" card with the abilities of every level up to it.
// No trigger runs and no Ring-bearer is chosen.
func (g *Game) SetRingTemptedYou(p PlayerID, n int) {
	if n <= 0 {
		g.Player(p).ringTempted = 0
		return
	}
	g.temptWithRing(p, n)
}

// SetRingBearer makes id p's Ring-bearer as a saved game state has it
// (GameState's IsRingBearer card annotation), with no trigger run.
func (g *Game) SetRingBearer(p PlayerID, id CardID) { g.Player(p).ringBearer = id }

// temptWithRing is the Ring half of the effect: p's Ring is made on first
// use (Player.createTheRing), the count becomes n and the Ring's definition
// carries the abilities of every level up to n (Player.setRingLevel, called
// once per temptation, adds the one level's abilities each time; a fresh
// definition holding levels 1..min(n, 4) is the same set). The card itself
// is kept, so its timestamp -- the one its Layer 4 static applies at -- is
// the Ring's own from when it was made.
func (g *Game) temptWithRing(p PlayerID, n int) {
	pl := g.Player(p)
	pl.ringTempted = n
	def := ringDef(n)
	if pl.theRing == NoCard {
		pl.theRing = g.putDesignationCard(NoCard, p, func() *compile.Card { return def })
		return
	}
	g.Card(pl.theRing).Def = def
}

// ringBearerCandidates is Player.getCreaturesInPlay: every creature on the
// battlefield p controls, in seat and then zone order.
func (g *Game) ringBearerCandidates(p PlayerID) []CardID {
	var out []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if c := g.Card(id); c.Controller() == p && c.Type().Has(cardtype.Creature) {
				out = append(out, id)
			}
		}
	}
	return out
}

// isRingBearer is the IsRingbearer card property (CardProperty.java,
// Card.isRingBearer): c is some player's Ring-bearer.
func (g *Game) isRingBearer(c *Card) bool {
	for _, pid := range g.Players() {
		if c.ID != NoCard && g.RingBearer(pid) == c.ID {
			return true
		}
	}
	return false
}

// loseRingBearer is RingTemptsYouEffect's loseCommand, run as id leaves
// the battlefield (addLeavesPlayCommand) or another player gains control of
// it (addChangeControllerCommand): it stops being a Ring-bearer (CR
// 701.54a) and stays that way even if control comes back.
func (g *Game) loseRingBearer(id CardID) {
	for i := range g.players {
		if g.players[i].ringBearer == id {
			g.players[i].ringBearer = NoCard
		}
	}
}

// isRingCard reports whether id is some player's "The Ring" card, state a
// fixture writes as numringtemptedyou= rather than as a Command-zone card.
func (g *Game) isRingCard(id CardID) bool {
	for i := range g.players {
		if g.players[i].theRing == id {
			return true
		}
	}
	return false
}

// checkRingTemptsYouTriggers runs Mode$ RingTemptsYou (TriggerRingTemptsYou)
// for p: ValidPlayer$ against p, ValidCard$ against the Ring-bearer just
// chosen -- no creature chosen matches nothing, as Java's matchesValid on
// null. The triggers record p as TriggeredPlayer.
func (g *Game) checkRingTemptsYouTriggers(c PlayerController, p PlayerID, bearer CardID) {
	g.pushPlayerTriggers(c, p, g.playerActionTriggerMatches(p, func(h *Card, t *compile.Ability) bool {
		spec, ok := t.Param("ValidCard")
		if !ok {
			return true
		}
		return bearer != NoCard && Matches(g, g.Card(bearer), valid.Parse(spec), h.Controller(), h.ID)
	}, "RingTemptsYou"))
}

// ringTemptsYouTriggersResolvable is the effect's pre-check: an error when
// a Mode$ RingTemptsYou trigger that could fire for p (host in a zone its
// TriggerZones$ names, ValidPlayer$ matching p) runs an Execute$ carrying a
// Cost$ (Call of the Ring's PayLife<2>, Sauron's Discard<1/Hand>).
func (g *Game) ringTemptsYouTriggersResolvable(p PlayerID) error {
	for _, pid := range g.Players() {
		for _, z := range phaseTriggerZones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for face := range h.triggerFaces {
					for _, t := range face.Triggers {
						if !strings.EqualFold(t.Name, "RingTemptsYou") || !phaseTriggerZoneMatches(h, t, z) {
							continue
						}
						if vp, ok := t.Param("ValidPlayer"); ok {
							if matched, recognized := matchesPlayerSpec(g, p, h.Controller(), host, vp); recognized && !matched {
								continue
							}
						}
						if vc, ok := t.Param("ValidCard"); ok && !g.anyRingBearerCandidateMatches(p, h, vc) {
							continue
						}
						for _, sub := range additionalAbilities(t, "Execute") {
							if _, ok := sub.Ability.Param("Cost"); ok {
								return fmt.Errorf("%s's RingTemptsYou trigger: Execute$ with Cost$ not resolvable yet", h.Def.Name)
							}
						}
					}
				}
			}
		}
	}
	return nil
}

// anyRingBearerCandidateMatches reports whether spec, read from host,
// matches any creature p could choose as Ring-bearer. A ValidCard$ trigger
// none of them matches cannot fire on this temptation -- with no creature
// the trigger's Card is null, whoever the kept bearer is (an opponent's Call of the Ring:
// Creature.YouCtrl is read against its controller). Level 1 only adds
// Legendary, so the candidates are the same after the level changes.
func (g *Game) anyRingBearerCandidateMatches(p PlayerID, host *Card, spec string) bool {
	parsed := valid.Parse(spec)
	for _, id := range g.ringBearerCandidates(p) {
		if Matches(g, g.Card(id), parsed, host.Controller(), host.ID) {
			return true
		}
	}
	return false
}

// dropStolenRingBearers is controllerChangeZoneCorrection's Ring half for
// a control change no effect made -- a Layer 2 static
// (GameAction.checkStaticAbilities runs the correction for every
// battlefield card whose controller is no longer its zone's player): a
// Ring-bearer another player now controls stops being one for good (CR
// 701.54a). CheckStateBasedActions runs it right after
// applyContinuousControl.
func (g *Game) dropStolenRingBearers() {
	for i := range g.players {
		pl := &g.players[i]
		if pl.ringBearer != NoCard && g.Card(pl.ringBearer).Controller() != pl.ID {
			pl.ringBearer = NoCard
		}
	}
}

// ringDef is the card Player.createTheRing and setRingLevel build: "The
// Ring", with the abilities of every level up to level (at most four). Java
// parses each from a string as the level is reached; this port builds the
// same trees directly (PORT-2), the way monarchEffectDef does:
//
//	1: Mode$ Continuous | EffectZone$ Command | Affected$ Card.YouCtrl+IsRingbearer | AddType$ Legendary
//	   Mode$ CantBlockBy | EffectZone$ Command | ValidAttacker$ Card.YouCtrl+IsRingbearer | ValidBlockerRelative$ Creature.powerGTX
//	     with X = Count$CardPower
//	2: Mode$ Attacks | ValidCard$ Card.YouCtrl+IsRingbearer | TriggerZones$ Command
//	     -> DB$ Draw | Defined$ You | NumCards$ 1, then DB$ Discard | Defined$ You | NumCards$ 1 | Mode$ TgtChoose
//	3: Mode$ AttackerBlockedByCreature | ValidCard$ Card.YouCtrl+IsRingbearer | ValidBlocker$ Creature | TriggerZones$ Command
//	     -> DB$ DelayedTrigger | Mode$ Phase | Phase$ EndCombat | RememberObjects$ TriggeredBlockerLKICopy
//	          Execute: DB$ SacrificeAll | Defined$ DelayTriggerRememberedLKI
//	4: Mode$ DamageDone | ValidSource$ Card.YouCtrl+IsRingbearer | ValidTarget$ Player | CombatDamage$ True | TriggerZones$ Command
//	     -> DB$ LoseLife | Defined$ Opponent | LifeAmount$ 3
//
// (Player.java:3305-3351.)
func ringDef(level int) *compile.Card {
	def := designationDef("The Ring")
	face := &def.Faces[0]
	const bearer = "Card.YouCtrl+IsRingbearer"
	if level >= 1 {
		face.Statics = append(face.Statics,
			&compile.Ability{Record: compile.Static, Name: "Continuous", Params: []vocab.Param{
				{Key: "Mode", Value: "Continuous"}, {Key: "EffectZone", Value: "Command"},
				{Key: "Affected", Value: bearer}, {Key: "AddType", Value: "Legendary"},
			}},
			&compile.Ability{Record: compile.Static, Name: "CantBlockBy", Params: []vocab.Param{
				{Key: "Mode", Value: "CantBlockBy"}, {Key: "EffectZone", Value: "Command"},
				{Key: "ValidAttacker", Value: bearer}, {Key: "ValidBlockerRelative", Value: "Creature.powerGTX"},
			}})
		face.Amounts = map[string]expr.Amount{"x": expr.Parse("Count$CardPower")}
	}
	if level >= 2 {
		draw := designationTrigger("Attacks", []vocab.Param{
			{Key: "ValidCard", Value: bearer}, {Key: "TriggerZones", Value: "Command"},
		}, "Draw", []vocab.Param{{Key: "Defined", Value: "You"}, {Key: "NumCards", Value: "1"}})
		exec := draw.Subs[0].Ability
		exec.Subs = []compile.SubRef{{Key: "SubAbility", Ability: &compile.Ability{
			Record: compile.SubAbility, Name: "Discard", Params: []vocab.Param{
				{Key: "DB", Value: "Discard"}, {Key: "Defined", Value: "You"},
				{Key: "NumCards", Value: "1"}, {Key: "Mode", Value: "TgtChoose"},
			}}}}
		face.Triggers = append(face.Triggers, draw)
	}
	if level >= 3 {
		blocked := designationTrigger("AttackerBlockedByCreature", []vocab.Param{
			{Key: "ValidCard", Value: bearer}, {Key: "ValidBlocker", Value: "Creature"},
			{Key: "TriggerZones", Value: "Command"},
		}, "DelayedTrigger", []vocab.Param{
			{Key: "Mode", Value: "Phase"}, {Key: "Phase", Value: "EndCombat"},
			{Key: "RememberObjects", Value: "TriggeredBlockerLKICopy"},
		})
		exec := blocked.Subs[0].Ability
		exec.Subs = []compile.SubRef{{Key: "Execute", Ability: &compile.Ability{
			Record: compile.SubAbility, Name: "SacrificeAll", Params: []vocab.Param{
				{Key: "DB", Value: "SacrificeAll"}, {Key: "Defined", Value: "DelayTriggerRememberedLKI"},
			}}}}
		face.Triggers = append(face.Triggers, blocked)
	}
	if level >= 4 {
		face.Triggers = append(face.Triggers, designationTrigger("DamageDone", []vocab.Param{
			{Key: "ValidSource", Value: bearer}, {Key: "ValidTarget", Value: "Player"},
			{Key: "CombatDamage", Value: "True"}, {Key: "TriggerZones", Value: "Command"},
		}, "LoseLife", []vocab.Param{{Key: "Defined", Value: "Opponent"}, {Key: "LifeAmount", Value: "3"}}))
	}
	return def
}
