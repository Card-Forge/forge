// "Enters as a copy": CR 614.1c/707.2 copy effects that replace the event
// of a card entering the battlefield -- ReplacementHandler's Copy layer
// (ReplacementLayer.Copy, CR 616.1c) run for Event$ Moved, applied before
// any other replacement looks at the card (CR 614.12).

package engine

//enginelint:allow id zone card game valid ability control effect replacement replaceeffect subability

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// enterBattlefieldReplacements runs the replacements of moved's entry onto
// the battlefield from origin, in ReplacementHandler's layer order: the
// Copy layer first (applyCopyReplacements), then "enters tapped"
// (checkMovedReplacement, an Other-layer shape). Every site that puts a
// card onto the battlefield calls it right after Game.Move and before the
// ETB triggers: CR 614.12 has an entering permanent's replacements look at
// it as it would exist on the battlefield, so a Vesuva entering as a copy
// of a tapland enters tapped, and a Clone entering as a copy of a creature
// with an ETB trigger has that trigger when checkETBTriggers looks.
func (g *Game) enterBattlefieldReplacements(controller PlayerController, moved CardID, origin ZoneType) {
	g.applyCopyReplacements(controller, moved, origin)
	g.checkMovedReplacement(moved, origin)
}

// copyReplacement is one Copy-layer replacement that applies to an entry:
// its host, the replacement, and the host face's SVar amounts.
type copyReplacement struct {
	host    CardID
	r       *compile.Ability
	amounts map[string]expr.Amount
}

// applyCopyReplacements is ReplacementHandler.run's Copy layer for moved's
// entry. Each applicable replacement applies once (Java's hasRun set), and
// after each the candidates are gathered again against moved's new
// characteristics: a Body Double entering as a copy of a Clone card in a
// graveyard now carries Clone's own "enters as a copy" replacement, which
// has not run yet and applies next (CR 614.12, 707.2). The loop ends
// because the set of replacements a game can reach is finite and none
// applies twice.
//
// Anything this port cannot resolve records a pending error (ADR-0020
// decision 4, GO-7) and stops before acting: several candidates at once,
// whose order CR 616.1 lets the affected player choose (no PlayerController
// decision for that exists yet); an unported param or ReplaceWith$ shape.
func (g *Game) applyCopyReplacements(controller PlayerController, moved CardID, origin ZoneType) {
	var applied []copyReplacement
	for {
		cands, err := g.copyReplacementCandidates(moved, origin, applied)
		if err != nil {
			g.recordPendingError(err)
			return
		}
		if len(cands) == 0 {
			return
		}
		if len(cands) > 1 {
			g.recordPendingError(fmt.Errorf(
				"engine: %d copy replacements apply to %q entering the battlefield: CR 616.1's choice among them not resolvable yet",
				len(cands), g.Card(moved).Def.Name))
			return
		}
		c := cands[0]
		applied = append(applied, c)
		if err := g.runCopyReplacement(controller, moved, c); err != nil {
			g.recordPendingError(err)
			return
		}
	}
}

// copyReplacementCandidates is getReplacementList for the Copy layer of
// moved's entry: moved's own current face, then every other active trait
// host (the battlefield's permanents, effect cards in the Command zone), in
// player order. Only a face-up card's current face (Faces[0]) is read, not
// every face: a battle entering front face up must not be offered its back
// face's replacement (Invasion of Amonkhet's Lazotep Convert).
func (g *Game) copyReplacementCandidates(moved CardID, origin ZoneType, applied []copyReplacement) ([]copyReplacement, error) {
	var out []copyReplacement
	consider := func(h *Card, self bool) error {
		if h.Def == nil {
			return nil
		}
		face := &h.Def.Faces[0]
		for _, r := range face.Replacements {
			c := copyReplacement{host: h.ID, r: r, amounts: face.Amounts}
			if wasApplied(applied, c) {
				continue
			}
			ok, err := g.copyReplacementApplies(h, self, r, face.Amounts, moved, origin)
			if err != nil {
				return err
			}
			if ok {
				out = append(out, c)
			}
		}
		return nil
	}
	if err := consider(g.Card(moved), true); err != nil {
		return nil, err
	}
	for _, pid := range g.Players() {
		for _, id := range g.traitHosts(pid) {
			if id == moved {
				continue
			}
			if err := consider(g.Card(id), false); err != nil {
				return nil, err
			}
		}
	}
	return out, nil
}

func wasApplied(applied []copyReplacement, c copyReplacement) bool {
	for _, a := range applied {
		if a.host == c.host && a.r == c.r {
			return true
		}
	}
	return false
}

// copyReplacementParams are the params a Copy-layer Moved replacement may
// carry and this port reads: ReplaceMoved.canReplace's zone and card checks,
// ReplacementEffect.requirementsCheck's (replacementRequirementsCheck), the
// handler's Optional$, and the purely descriptive rest.
var copyReplacementParams = [...]string{
	"event", "validcard", "destination", "origin", "replacementresult", "layer", "optional", "activezones",
	"description", "replacewith", "secondary", "checksvar", "svarcompare", "ispresent", "playerturn",
}

// copyReplacementApplies is modeCheck, zonesCheck, requirementsCheck and
// ReplaceMoved.canReplace for one replacement r of host h against moved's
// entry. self is h being moved itself, whose replacements Java reads off its
// battlefield-bound LKI copy regardless of ActiveZones$ (getReplacementList's
// affectedLKI). An error means r would apply but this port cannot run it.
func (g *Game) copyReplacementApplies(h *Card, self bool, r *compile.Ability, amounts map[string]expr.Amount, moved CardID, origin ZoneType) (bool, error) {
	if !strings.EqualFold(r.Name, "Moved") {
		return false, nil
	}
	if layer, _ := r.Param("Layer"); !strings.EqualFold(layer, "Copy") {
		return false, nil
	}
	if !replacementZoneMatches(r, "Destination", Battlefield) || !replacementZoneMatches(r, "Origin", origin) {
		return false, nil
	}
	if !self && !hostInActiveZones(h, r, h.Zone) {
		return false, nil
	}
	validCard, ok := r.Param("ValidCard")
	if !ok || !Matches(g, g.Card(moved), valid.Parse(validCard), h.Controller(), h.ID) {
		return false, nil
	}
	if !replacementRequirementsCheck(g, h, amounts, r) {
		return false, nil
	}
	if err := copyReplacementResolvable(h, r); err != nil {
		return false, err
	}
	return true, nil
}

// copyReplacementResolvable reports why an applicable Copy-layer
// replacement r cannot run here, nil when it can: its params, a host that
// is an effect card, and its ReplaceWith$ chain.
func copyReplacementResolvable(h *Card, r *compile.Ability) error {
	for _, p := range r.Params {
		known := false
		for _, k := range copyReplacementParams {
			known = known || strings.EqualFold(p.Key, k)
		}
		if !known {
			return fmt.Errorf("engine: %q: copy replacement param %s$ not resolvable yet", h.Def.Name, p.Key)
		}
	}
	if h.IsEffect {
		// Mystic Reflection: "the next time one or more creatures enter",
		// a batch this port's one-at-a-time entries cannot tell apart, and
		// an effect card whose own ChangesZoneAll exile ends it.
		return fmt.Errorf("engine: %q: a copy replacement on an effect card not resolvable yet", h.Def.Name)
	}
	var with *compile.Ability
	for _, sub := range r.Subs {
		if strings.EqualFold(sub.Key, "ReplaceWith") {
			with = sub.Ability
		}
	}
	if with == nil || with.Name != "Clone" {
		name := "none"
		if with != nil {
			name = with.Name
		}
		return fmt.Errorf("engine: %q: copy replacement with ReplaceWith$ %s not resolvable yet", h.Def.Name, name)
	}
	for a := with; a != nil; a = subAbilityOf(a) {
		if _, ok := a.Param("ValidTgts"); ok {
			return fmt.Errorf("engine: %q: copy replacement ability %s with ValidTgts$ not resolvable yet", h.Def.Name, a.Name)
		}
		if _, ok := a.Param("ReplacementEffects"); ok && a.Name == "Effect" {
			// Spark Double, Moritte of the Frost: an effect whose own
			// replacement adds counters to this same entry, which in this
			// port has already happened by the time the effect exists.
			return fmt.Errorf("engine: %q: an Effect replacing the entry it is part of not resolvable yet", h.Def.Name)
		}
	}
	return nil
}

// subAbilityOf is a's chained SubAbility$, nil when it has none.
func subAbilityOf(a *compile.Ability) *compile.Ability {
	if sub, ok := findSubAbility(a); ok {
		return sub.Ability
	}
	return nil
}

// runCopyReplacement is executeReplacement for one Copy-layer replacement:
// an Optional$ one asks the entering card's controller first (the
// decider, ReplacementHandler.run), then its ReplaceWith$ Clone chain
// resolves on the spot, the host's controller activating it, with moved as
// the replacing object (Defined$ ReplacedCard). Declining, or a Clone that
// finds nothing to copy, leaves moved entering as itself.
func (g *Game) runCopyReplacement(controller PlayerController, moved CardID, c copyReplacement) error {
	h := g.Card(c.host)
	if _, ok := c.r.Param("Optional"); ok && !controller.ConfirmEffect(g, g.Card(moved).Controller(), c.host) {
		return nil
	}
	if g.registry == nil {
		return fmt.Errorf("engine: %q: copy replacement: no Registry on this Game", h.Def.Name)
	}
	var with *compile.Ability
	for _, sub := range c.r.Subs {
		if strings.EqualFold(sub.Key, "ReplaceWith") {
			with = sub.Ability
		}
	}
	a := Ability{
		API: APIClone, Source: c.host, Controller: h.Controller(), Params: with, Amounts: c.amounts,
		replacing: &replacementEvent{result: replacementUpdated, card: moved},
	}
	if err := g.registry.Resolve(g, &a, controller); err != nil {
		return fmt.Errorf("engine: %q: copy replacement: %w", h.Def.Name, err)
	}
	return nil
}
