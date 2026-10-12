package engine

//enginelint:allow id zone card game ability control valid trigger statictrigger

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// hauntParams are the keys the one Haunt line the corpus can reach carries
// (CardFactoryUtil.java:1262, synthesized by compile/haunt.go): the record key
// and the target. Anything else fails the line (GO-7).
var hauntParams = map[string]bool{"db": true, "validtgts": true, "tgtprompt": true}

// hauntEffect is HauntEffect.java's targeted branch (CR 702.55a): the haunter,
// still the graveyard object its trigger saw and not a token, is exiled
// haunting the target creature (Card.haunting, read by HauntedBy).
//
// Java's untargeted branch undoes a haunt, run by the two Static$ triggers
// CardFactoryUtil.java:1213-1237 adds; this port pins both objects' zone
// stamps in the mark instead (hauntMark, card.go) and synthesizes neither
// trigger, so an untargeted Haunt line is refused rather than read as a
// no-op.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/HauntEffect.java's resolve.
type hauntEffect struct{}

func (hauntEffect) Resolve(g *Game, a *Ability, c PlayerController) error {
	for _, p := range a.Params.Params {
		if !hauntParams[strings.ToLower(p.Key)] {
			return fmt.Errorf("engine: Haunt: %s$ not resolvable yet", p.Key)
		}
	}
	if _, ok := a.Params.Param("ValidTgts"); !ok {
		return fmt.Errorf("engine: Haunt: an untargeted Haunt (unhaunt) is not resolvable: a haunt ends by object identity (hauntMark)")
	}
	host := g.Card(a.Source)
	// HauntEffect.java:25: no token, and still the object in the graveyard
	// the trigger fired for (equalsWithGameTimestamp).
	if host.IsToken || host.Zone != Graveyard || (a.triggered.cardStamp != 0 && host.zoneStamp != a.triggered.cardStamp) {
		return nil
	}
	var target CardID
	for _, e := range a.Targets {
		if id, ok := e.AsCard(); ok {
			target = id
			break
		}
	}
	if target == NoCard {
		return fmt.Errorf("engine: Haunt: no creature target chosen")
	}
	g.moveByEffect(c, a.Source, Exile, 0, NoPlayer, false)
	if host.Zone != Exile {
		return nil
	}
	host.haunting = hauntMark{self: host.zoneStamp, card: target, stamp: g.Card(target).zoneStamp}
	return nil
}

// Haunting is the creature id haunts (CR 702.55b), NoCard when it haunts
// none: id is still the exiled object Haunt made it and the creature is
// still the battlefield object it haunted.
func (g *Game) Haunting(id CardID) CardID {
	h := g.Card(id)
	m := h.haunting
	if m.card == NoCard {
		return NoCard
	}
	if t := g.Card(m.card); t.Zone != Battlefield || !h.haunts(t) {
		return NoCard
	}
	return m.card
}

// checkSpellToGraveyardTriggers is TriggerChangesZone's Fizzle$ test
// (TriggerChangesZone.java:151-158) for a spell card the stack just put into
// its owner's graveyard after resolving or fizzling (MagicStack.java:715-721,
// the one move that carries AbilityKey.Fizzle): card's own Origin$ Stack,
// Destination$ Graveyard triggers whose Fizzle$ agrees with fizzled. A spell
// countered or moved any other way carries no Fizzle key, so a Fizzle$ line
// never fires for it in Java either. The Haunt keyword's spell trigger
// (CardFactoryUtil.java:1255-1258) is the corpus's only Fizzle$ line.
func (g *Game) checkSpellToGraveyardTriggers(controller PlayerController, card CardID, fizzled bool) {
	c := g.Card(card)
	if c.Zone != Graveyard || c.Def == nil {
		return
	}
	var matches []Ability
	for face := range c.triggerFaces {
		for _, t := range face.Triggers {
			if !isSpellToGraveyardTrigger(t, fizzled) {
				continue
			}
			if spec, ok := t.Param("ValidCard"); !ok || !Matches(g, c, valid.Parse(spec), c.Controller(), card) {
				continue
			}
			if sub, api, optional, ok := triggerEffectAPI(g, c, face.Amounts, t); ok {
				matches = append(matches, Ability{API: api, Source: card, Controller: c.Controller(), Params: sub, Amounts: face.Amounts, Optional: optional,
					triggered: face.objects(triggeredObjects{card: card, cardStamp: c.zoneStamp}), staticTrigger: isStaticTrigger(t)})
			}
		}
	}
	g.pushTriggeredAbilities(controller, matches)
}

// isSpellToGraveyardTrigger reports whether t is a Mode$ ChangesZone line
// from the stack to a graveyard naming Fizzle$, and its Fizzle$ matches
// fizzled ("True" == Fizzle$ is the fizzled case). A line naming any other
// ChangesZone param this port does not evaluate is not one (GO-7).
func isSpellToGraveyardTrigger(t *compile.Ability, fizzled bool) bool {
	if !strings.EqualFold(t.Name, "ChangesZone") || !hasZone(t, "Origin", "Stack") || !hasZoneOrAny(t, "Destination", Graveyard) {
		return false
	}
	v, ok := t.Param("Fizzle")
	if !ok || (v == "True") != fizzled {
		return false
	}
	for _, key := range changesZoneUnresolvedParams {
		if _, has := t.Param(key); has && key != "Fizzle" {
			return false
		}
	}
	return true
}
