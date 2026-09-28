// Phasing (CR 702.26): the operation that phases a permanent in or out, the
// untap step's own phasing, the PhaseIn/PhaseOut/PhaseOutAll trigger modes
// and the CantPhaseIn/CantPhaseOut statics (ADR-0021).

package engine

//enginelint:allow id zone card game player event combat valid trigger control parts effecteffect ability

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// phasing is one phasing operation in progress: a Phases resolution or one
// untap step's phasing. Java holds every PhaseIn and PhaseOut trigger it
// fires (runTrigger(..., true), Card.java:5633, :5669) and collects them only
// once the operation is over, against the trigger set that was active
// before it began -- which is how "whenever CARDNAME phases out" sees its
// own, now phased-out, host (Card.phasedOutSelf). hosts is that set,
// snapshotted before the first card phases; events are the phasings in the
// order they happened.
type phasing struct {
	hosts  []CardID
	events []phaseEvent
}

// phaseEvent is one card phasing, in (true) or out.
type phaseEvent struct {
	card CardID
	in   bool
}

// beginPhasing snapshots the trigger hosts for a new phasing operation.
func (g *Game) beginPhasing() *phasing {
	b := &phasing{}
	for _, pid := range g.Players() {
		b.hosts = append(b.hosts, g.traitHosts(pid)...)
	}
	return b
}

// phase is Card.phase(fromUntapStep, direct) (Card.java:5589-5621): flip
// id's phasing, then phase its attachments with it -- indirectly, CR
// 702.26g -- and emit Phased. Phasing is never a zone change: no Move, no
// ZoneChanged, no ETB or LTB trigger (CR 702.26d, ADR-0021 decision 4), and
// the card keeps its place in the battlefield's order (GO-12).
//
// An attachment that is phased in and cannot phase out stays (Card.java:
// 5605-5607); otherwise one whose phasing matches id's before the flip
// follows it (Card.java:5608-5610).
func (g *Game) phase(b *phasing, id CardID, fromUntapStep, direct bool) {
	c := g.Card(id)
	phasingIn := c.IsPhasedOut()
	if !g.switchPhaseState(b, id, fromUntapStep) {
		return
	}
	if !phasingIn {
		c.directlyPhasedOut = direct
	}
	for _, eq := range append([]CardID(nil), c.Attachments()...) {
		e := g.Card(eq)
		if !e.IsPhasedOut() && g.cantPhase(eq, "CantPhaseOut") {
			continue
		}
		if e.IsPhasedOut() == phasingIn {
			g.phase(b, eq, fromUntapStep, false)
		}
	}
	detail := PhaseDetailOut
	if phasingIn {
		detail = PhaseDetailIn
	}
	g.sink.Emit(Event{Kind: Phased, Phase: g.activePhase, Active: g.activePlayer, Actor: c.Controller(), Turn: uint16(g.turn), Source: id, Detail: uint32(detail)})
}

// switchPhaseState is Card.switchPhaseState (Card.java:5623-5675). A
// permanent phased out with WontPhaseInNormal$ does not phase in on an
// untap step. Phasing out records the controller whose untap step phases
// it back in (Card.java:5645) and takes it out of combat (Card.java:
// 5647-5650). Phasing in unattaches it from a card no longer on the
// battlefield (CR 702.26g, Card.java:5652-5665).
//
// Not modeled, each for want of the state it acts on: runPhaseOutCommands
// (CR 702.26f -- no "until it phases out" duration exists in this port),
// clearEncodedCards (cipher) and soulbond pairing, and Combat.saveLKI.
func (g *Game) switchPhaseState(b *phasing, id CardID, fromUntapStep bool) bool {
	c := g.Card(id)
	if c.IsPhasedOut() && fromUntapStep && c.wontPhaseInNormal {
		return false
	}
	if !c.IsPhasedOut() {
		g.setPhasedOut(id, c.Controller())
		g.removeFromCombat(id)
		b.events = append(b.events, phaseEvent{card: id})
		return true
	}
	g.setPhasedOut(id, NoPlayer)
	if host, ok := c.AttachedTo(); ok && g.Card(host).Zone != Battlefield {
		g.Unattach(id)
	}
	b.events = append(b.events, phaseEvent{card: id, in: true})
	return true
}

// endPhasing finishes a phasing operation: ForgetOnPhasedIn$ effect cards
// forget what phased in, then the triggers fire -- PhaseOutAll first, which
// Java runs at once (PhasesEffect.java:123, Untap.java:241), then each held
// PhaseOut and PhaseIn event in the order it happened. phasedOutAll is the
// batch PhaseOutAll reports, empty for none.
//
// A PhaseOut event is matched against the hosts active before the
// operation; a PhaseIn event against those plus every card that phased in,
// Card.switchPhaseState's own registerActiveTrigger (Card.java:5668).
// ValidCard$ is read now, after the phasing: "Card.phasedOutSelf" is how the
// corpus writes a permanent's own "whenever CARDNAME phases out".
func (g *Game) endPhasing(controller PlayerController, b *phasing, phasedOutAll []CardID) {
	inHosts := b.hosts
	for _, ev := range b.events {
		if !ev.in {
			continue
		}
		g.effectCardsSeePhaseIn(ev.card)
		if !containsCard(inHosts, ev.card) {
			if len(inHosts) == len(b.hosts) {
				inHosts = append([]CardID(nil), b.hosts...)
			}
			inHosts = append(inHosts, ev.card)
		}
	}
	var matches []Ability
	if len(phasedOutAll) > 0 {
		matches = g.phaseTriggerMatches(b.hosts, "PhaseOutAll", phasedOutAll)
	}
	for _, ev := range b.events {
		if ev.in {
			matches = append(matches, g.phaseTriggerMatches(inHosts, "PhaseIn", []CardID{ev.card})...)
		} else {
			matches = append(matches, g.phaseTriggerMatches(b.hosts, "PhaseOut", []CardID{ev.card})...)
		}
	}
	g.pushTriggeredAbilities(controller, matches)
}

// phaseTriggerMatches is TriggerPhaseIn/TriggerPhaseOut.performTest
// (ValidCard$ against the one card) and TriggerPhaseOutAll.performTest
// (ValidCards$ against the batch, any match) over hosts' own Mode$ mode
// triggers. A host has to be in a zone TriggerZones$ names, an effect card
// in the Command zone alone (phaseTriggerZoneMatches).
func (g *Game) phaseTriggerMatches(hosts []CardID, mode string, cards []CardID) []Ability {
	key := "ValidCard"
	if mode == "PhaseOutAll" {
		key = "ValidCards"
	}
	var matches []Ability
	for _, host := range hosts {
		h := g.Card(host)
		if h.Def == nil {
			continue
		}
		for face := range h.triggerFaces {
			for _, t := range face.Triggers {
				if !strings.EqualFold(t.Name, mode) || !phaseTriggerZoneMatches(h, t, h.Zone) {
					continue
				}
				if spec, ok := t.Param(key); ok && !anyCardMatches(g, cards, valid.Parse(spec), h.Controller(), host) {
					continue
				}
				if sub, api, optional, ok := triggerEffectAPI(g, h, face.Amounts, t); ok {
					matches = append(matches, Ability{API: api, Source: host, Controller: h.Controller(), Params: sub, Amounts: face.Amounts, Optional: optional, triggered: face.objects(triggeredObjects{})})
				}
			}
		}
	}
	return matches
}

// anyCardMatches reports whether any of cards satisfies spec.
func anyCardMatches(g *Game, cards []CardID, spec valid.Spec, sourceController PlayerID, source CardID) bool {
	for _, id := range cards {
		if Matches(g, g.Card(id), spec, sourceController, source) {
			return true
		}
	}
	return false
}

// effectCardsSeePhaseIn is ForgetOnPhasedIn$'s watch
// (SpellAbilityEffect.addForgetOnPhasedInTrigger, SpellAbilityEffect.java:
// 583-588): an effect card remembering a card that phases in forgets it,
// and is exiled once it remembers no card (getForgetSpellAbility's
// ConditionCompare$ EQ0) -- effectCardsSeeMove's own ForgetOnMoved$ shape.
func (g *Game) effectCardsSeePhaseIn(id CardID) {
	for _, eid := range g.effectCards() {
		e := g.Card(eid)
		if e.Zone != Command || !e.effectLife.forgetOnPhasedIn || !effectRemembers(e, id) {
			continue
		}
		e.Memory.Forget(CardEntity(id))
		if !effectRemembersAnyCard(e) {
			g.exileEffect(eid)
		}
	}
}

// cantPhase is StaticAbilityCantPhase.cantPhase (mode "CantPhaseIn" or
// "CantPhaseOut"): a Mode$ <mode> static whose host's traits are active and
// whose IsPresent$ condition holds (StaticAbility.checkConditions, the only
// condition the corpus's seven lines carry) names id in its ValidCard$ --
// absent, every card. ValidCard$ reads a phased-out card through Matches'
// own phasedOut prefix: "Card.phasedOutIsRemembered".
func (g *Game) cantPhase(id CardID, mode string) bool {
	c := g.Card(id)
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.Def.Faces {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, mode) {
						continue
					}
					if !isPresentMatches(g, h, face.Amounts, s, "IsPresent", "PresentCompare", "PresentDefined", "PresentZone", "PresentPlayer") {
						continue
					}
					spec, ok := s.Param("ValidCard")
					if !ok || Matches(g, c, valid.Parse(spec), h.Controller(), host) {
						return true
					}
				}
			}
		}
	}
	return false
}

// untapStepPhasing is Untap.doPhasing (Untap.java:198-249), the first thing
// the untap step does (CR 502.1): every permanent the active player phased
// out directly phases back in, and every permanent with K:Phasing they
// control phases out -- or, already phased out on its own, in. It reads the
// whole battlefield, phased-out permanents included (Untap.java:202,
// ADR-0021's untap opt-in), in seat then zone order. A static that stops the
// phasing (cantPhase) leaves that permanent where it is.
//
// CR 702.26h: an attachment with phasing whose host phases out this same
// step phases out with it, indirectly, not on its own.
func (g *Game) untapStepPhasing(controller PlayerController) {
	turn := g.activePlayer
	var list []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).CardsIncludingPhasedOut() {
			c := g.Card(id)
			if c.phasedOut == turn && c.directlyPhasedOut || c.HasKeyword("Phasing") && c.Controller() == turn {
				list = append(list, id)
			}
		}
	}
	var toPhase []CardID
	for _, id := range list {
		c := g.Card(id)
		if c.IsPhasedOut() && g.cantPhase(id, "CantPhaseIn") || !c.IsPhasedOut() && g.cantPhase(id, "CantPhaseOut") {
			continue
		}
		toPhase = append(toPhase, id)
	}
	if len(toPhase) == 0 {
		return
	}
	b := g.beginPhasing()
	var phasedOut []CardID
	for _, id := range toPhase {
		c := g.Card(id)
		switch {
		case c.IsPhasedOut() && c.directlyPhasedOut:
			g.phase(b, id, true, true)
		case c.HasKeyword("Phasing"):
			if host, ok := c.AttachedTo(); ok && containsCard(list, host) && !g.cantPhase(host, "CantPhaseOut") {
				continue
			}
			g.phase(b, id, true, true)
			phasedOut = append(phasedOut, id)
		}
	}
	g.endPhasing(controller, b, phasedOut)
}
