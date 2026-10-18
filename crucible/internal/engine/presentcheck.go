// IsPresent$, PresentDefined$ and CheckSVar$ conditions: CardTraitBase's own
// meetsCommonRequirements blocks, shared by triggers, static abilities, activation
// restrictions and sub-ability conditions.

package engine

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// isPresentMatches is CardTraitBase's own IsPresent$/PresentCompare$/
// PresentDefined$/PresentZone$/PresentPlayer$ block (isKey absent from t
// entirely is a pass, the identical "no restriction" contract every other
// optional gate in this port already has). PresentZone$ defaults to
// Battlefield, a comma list otherwise (ZoneByName per entry, an
// unrecognized name a pending error, GO-7); PresentPlayer$ is "You"
// (host's own controller only), anything else -- "Any", the corpus's own
// overwhelming default, or absent -- every player, Java's own three
// additive You/Opponent/Allies blocks collapsed to the one partition they
// produce for a single-valued param. definedKey (PresentDefined$/
// ConditionDefined$) names the objects counted instead of a zone scan
// (definedEntities, against the ability resolving from host when there is
// one, which is where targets, the triggering card and paid costs live); a
// spelling it cannot resolve fails the ability that is resolving (GO-7). An LKI spelling counts a
// card that left the battlefield by its last-known state.
func isPresentMatches(g *Game, host *Card, amounts map[string]expr.Amount, t *compile.Ability, isKey, compareKey, definedKey, zoneKey, playerKey string) bool {
	spec, ok := t.Param(isKey)
	if !ok {
		return true
	}
	if strings.Contains(spec, "IsTriggerRemembered") {
		// Card.IsTriggerRemembered names the delayed trigger's own
		// RememberObjects$ list (CardProperty.java:163), which this scan
		// cannot see: delayedPresentMatches (delayedtrigger.go) is the
		// check for a delayed trigger and runs where that list is known.
		return true
	}
	if defined, ok := t.Param(definedKey); ok {
		return definedPresentMatches(g, host, amounts, t, spec, definedKey, defined, compareKey)
	}
	var zones []ZoneType
	if zoneList, ok := t.Param(zoneKey); ok {
		for _, name := range strings.Split(zoneList, ",") {
			z, ok := ZoneByName(name)
			if !ok {
				g.recordPendingError(fmt.Errorf("engine: %s$ %q: zone %q not resolvable", zoneKey, zoneList, name))
				return false
			}
			zones = append(zones, z)
		}
	} else {
		zones = []ZoneType{Battlefield}
	}
	onlyYou := false
	if player, ok := t.Param(playerKey); ok {
		onlyYou = strings.EqualFold(player, "You")
	}
	var candidates []CardID
	for _, pid := range g.Players() {
		if onlyYou && pid != host.Controller() {
			continue
		}
		for _, z := range zones {
			candidates = append(candidates, g.Zone(z, pid).Cards()...)
		}
	}
	parsed := valid.Parse(spec)
	n := 0
	for _, id := range candidates {
		if Matches(g, g.Card(id), parsed, host.Controller(), host.ID) {
			n++
		}
	}
	return presentCountMatches(g, host, amounts, t, compareKey, n)
}

// definedPresentMatches is the PresentDefined$/ConditionDefined$ branch of
// isPresentMatches: the candidates are the objects defined names, not a zone
// scan -- SpellAbilityCondition.java:350-351's
// AbilityUtils.getDefinedObjects(host, defined, sa), whose restriction filter
// (GameObjectPredicates.restriction, :365) counts a player too when the spec
// names players (3 real lines: ConditionPresent$ Player). Pass the Torch's
// "if you do" (ConditionDefined$ Remembered | ConditionPresent$ Card after
// RememberPlayed$) is the shape.
func definedPresentMatches(g *Game, host *Card, amounts map[string]expr.Amount, t *compile.Ability, spec, definedKey, defined, compareKey string) bool {
	var refs abilityRefs
	own := false
	if a := g.resolving; a != nil && a.Source == host.ID {
		refs, own = a.refs(), true
	}
	var objects []EntityID
	var err error
	if defined == "TriggeredCard" || defined == "TriggeredCardLKICopy" {
		// The card the trigger recorded; only a condition reads it for now
		// (every other Defined$ reader still refuses it).
		if refs.triggered.card == NoCard {
			err = fmt.Errorf("the trigger recorded no card")
		} else {
			objects = []EntityID{CardEntity(refs.triggered.card)}
		}
	} else {
		objects, err = definedEntities(g, host.Controller(), host, defined, refs)
	}
	if err != nil {
		if own {
			// A condition the port cannot read is not an unmet one: the
			// ability fails instead of silently skipping (GO-7).
			g.recordPendingError(fmt.Errorf("engine: %s$ %q: %w", definedKey, defined, err))
		}
		return false
	}
	lki := strings.HasSuffix(defined, "LKI") || strings.HasSuffix(defined, "LKICopy")
	parsed := valid.Parse(spec)
	n := 0
	for _, e := range objects {
		if id, ok := e.AsCard(); ok {
			c := g.Card(id)
			if snap := g.LKI(id); lki && c.Zone != Battlefield && snap != nil {
				c = snap
			}
			if Matches(g, c, parsed, host.Controller(), host.ID) {
				n++
			}
			continue
		}
		if pid, ok := e.AsPlayer(); ok {
			if matched, recognized := matchesPlayerSpec(g, pid, host.Controller(), host.ID, spec); recognized && matched {
				n++
			}
		}
	}
	return presentCountMatches(g, host, amounts, t, compareKey, n)
}

// presentCountMatches compares n, the count of present objects, against
// compareKey (GE1 when absent), its right side a literal or a named SVar. A
// compare string too short to hold an operator and an operand, or a right
// side resolveNamedAmount cannot resolve, records a pending error and reads
// false (GO-7): Java evaluates both (CardTraitBase.java:425-431).
func presentCountMatches(g *Game, host *Card, amounts map[string]expr.Amount, t *compile.Ability, compareKey string, n int) bool {
	compare, ok := t.Param(compareKey)
	if !ok {
		compare = "GE1"
	}
	if len(compare) < 3 {
		g.recordPendingError(fmt.Errorf("engine: %s$ %q is not a comparison", compareKey, compare))
		return false
	}
	right, ok := resolveNamedAmount(g, amounts, host, compare[2:])
	if !ok {
		g.recordPendingError(fmt.Errorf("engine: %s$ %q is not resolvable", compareKey, compare))
		return false
	}
	return compareOp(n, compare[:2], right)
}

// checkSVarMatches is CardTraitBase's own CheckSVar$/SVarCompare$ block,
// both sides resolved through resolveNamedAmount (ptParam's own shape,
// continuous.go, factored out once this needed the identical
// literal-or-named-SVar resolution against a *Card rather than a
// *compile.Ability's own param). checkKey/compareKey/secondKey are the three
// param names this exact shape uses under two different names in the real
// corpus -- CardTraitBase's own CheckSVar$/SVarCompare$/CheckSecondSVar$
// (triggerCommonRequirementsMet, below) and SpellAbilityCondition's own
// ConditionCheckSVar$/ConditionSVarCompare$/OrOtherConditionSVarCompare$
// (subAbilityConditionMet, condition.go) -- generalized once the second
// caller needed the identical logic under its own param names.
//
// A condition this port cannot evaluate is not an unmet one: a second SVar
// (secondKey, not ported), a compare string too short to hold an operator and
// an operand, or a side resolveNamedAmount cannot resolve records a pending
// error and reads false, so the game that reached it fails instead of
// silently skipping the ability (GO-7). Java evaluates every one of these
// (CardTraitBase.java:470-488).
func checkSVarMatches(g *Game, host *Card, amounts map[string]expr.Amount, t *compile.Ability, checkKey, compareKey, secondKey string) bool {
	checkSVar, ok := t.Param(checkKey)
	if !ok {
		return true
	}
	if second, ok := t.Param(secondKey); ok {
		g.recordPendingError(fmt.Errorf("engine: %s$ %q with %s$ %q not resolvable yet", checkKey, checkSVar, secondKey, second))
		return false
	}
	left, ok := resolveNamedAmount(g, amounts, host, checkSVar)
	if !ok {
		g.recordPendingError(fmt.Errorf("engine: %s$ %q is not resolvable", checkKey, checkSVar))
		return false
	}
	compare, ok := t.Param(compareKey)
	if !ok {
		compare = "GE1"
	}
	if len(compare) < 3 {
		g.recordPendingError(fmt.Errorf("engine: %s$ %q is not a comparison", compareKey, compare))
		return false
	}
	right, ok := resolveNamedAmount(g, amounts, host, compare[2:])
	if !ok {
		g.recordPendingError(fmt.Errorf("engine: %s$ %q is not resolvable", compareKey, compare))
		return false
	}
	return compareOp(left, compare[:2], right)
}
