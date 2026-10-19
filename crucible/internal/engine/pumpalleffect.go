// PumpAll: PumpEffect's own blanket sibling -- CR 611 applied to a
// valid-string-matched set rather than a single Defined$/targeted card, the
// corpus's own real "anthem spell" shape (Overrun, ...). 833 real
// (AB|DB)$ PumpAll lines, 818 of them naming no Defined$ and no ValidTgts$ at
// all -- Java's own `!sa.usesTargeting() && !sa.hasParam("Defined")` branch,
// this port's entire real scope since neither targeting nor most Defined$
// shapes exist -- and within that (plus the 3 real Defined$ You/Opponent
// lines resolveDefinedPlayers already covers), 668 of 833 resolve, 26 of
// them naming Planeswalker$/Ultimate$ (below) -- CR 606.3's own
// loyalty-ability marker and its own purely-descriptive sibling, neither a
// restriction on how the effect they cost-gate actually resolves.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/PumpAllEffect.java's
// resolve/applyPumpAll.

package engine

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// pumpAllUnresolvedParams names PumpAllEffect.resolve's own params past
// NumAtt$/NumDef$/KW$/Duration$/PumpZone$/ValidCards$/Defined$ this port
// does not evaluate. Every one fails the whole line loudly rather than
// applying half of it and guessing at the rest (PORT-8/GO-7):
// Condition$/ConditionDefined$/ConditionZone$/ConditionPlayerTurn$/
// ConditionManaSpent$/ConditionManaNotSpent$ (4/5/3/1/4/0) --
// SpellAbilityCondition's own shapes subAbilityConditionMet does not cover,
// the identical Pump-shaped gap; ValidTgts$ (12) -- a real target past a
// blanket ValidCards$ match, this port's own targeting gap; RememberPumped$
// (8) -- Card.Memory has no writer wired to a
// blanket multi-card grant; SharedKeywordsZone$/SharedRestrictions$ (4/4) --
// CardFactoryUtil.sharedKeywords' own zone scan, a further mechanic;
// ModeCost$ (3) and Exhaust$ (4) -- each its own further activation
// mechanic; AtEOT$ (0 real PumpAll lines, kept for symmetry with Pump's own
// list and in case a future corpus update adds one) --
// registerDelayedTrigger, a new trigger this effect would silently fail to
// create.
//
// SubAbility$ no longer blocks: resolveSubAbility (subability.go) chains it
// through Registry.Resolve (effect.go) once this effect's own body
// finishes, whether or not subAbilityConditionMet let it run at all. 6 of
// the corpus's own 75 real SVar-defined PumpAll lines naming SubAbility$
// chain to an already-built leaf ability and resolve end to end.
//
// UnlessCost$/UnlessPayer$ no longer block either: resolveUnlessCost
// (effect.go) gates the whole ability before Registry.Resolve ever reaches
// it. 0 of the corpus's own 3 real PumpAll lines naming UnlessCost$ resolve,
// though: rhystic_shield.txt's own real "get +0/+2... unless any player
// pays {2}" is the one clearing resolveUnlessCost's own pure-mana-cost/
// resolvable-payer filter, and it is a top-level A:SP$ PumpAll line on an
// Instant -- CastSpell does not cast an instant or sorcery at all
// (castspell.go's own doc comment); the other 2 name UnlessPayer$
// RememberedController and a non-mana UnlessCost$ (Mandatory PayEnergy<X>),
// neither resolvable here regardless.
var pumpAllUnresolvedParams = [...]string{
	"Condition", "ConditionZone", "ConditionPlayerTurn",
	"ConditionManaSpent", "ConditionManaNotSpent", "ValidTgts",
	"RememberPumped", "SharedKeywordsZone", "SharedRestrictions",
	"ModeCost", "Exhaust", "AtEOT",
}

// pumpAllEffect resolves Mode$/DB$/AB$ PumpAll for the blanket shape --
// every card ValidCards$ matches across PumpZone$'s own zones (default
// Battlefield alone), every player's own unless Defined$ narrows it to
// specific players (definedPlayers, defined.go). ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go), the identical way Pump's
// own do.
type pumpAllEffect struct{}

func (pumpAllEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	for _, key := range pumpAllUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: PumpAll: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	permanent, untilNextTurn := false, false
	if d, ok := a.Params.Param("Duration"); ok {
		switch d {
		case "Permanent":
			permanent = true
		case "UntilYourNextTurn":
			// SpellAbilityEffect.addUntilCommand (:980-981), as Pump's.
			permanent, untilNextTurn = true, true
		default:
			return fmt.Errorf("engine: PumpAll: Duration$ %q not resolvable yet", d)
		}
	}

	power, err := pumpAmount(g, "PumpAll", a, source, "NumAtt")
	if err != nil {
		return err
	}
	toughness, err := pumpAmount(g, "PumpAll", a, source, "NumDef")
	if err != nil {
		return err
	}
	grant, err := pumpKeywords("PumpAll", a.Params)
	if err != nil {
		return err
	}
	if power == 0 && toughness == 0 && grant.empty() {
		return nil
	}

	validCards, ok := a.Params.Param("ValidCards")
	if !ok {
		return fmt.Errorf("engine: PumpAll: ValidCards$ missing")
	}
	spec := valid.Parse(validCards)

	var players []PlayerID
	if defined, ok := a.Params.Param("Defined"); ok {
		players, err = definedPlayers(g, a.Controller, a.Source, defined, a.refs())
		if err != nil {
			return fmt.Errorf("engine: PumpAll: %w", err)
		}
	} else {
		players = g.Players()
	}

	g.timestamp++
	timestamp := g.timestamp
	for _, pid := range players {
		for _, z := range pumpAllZones(a.Params) {
			for _, cid := range g.Zone(z, pid).Cards() {
				if !Matches(g, g.Card(cid), spec, source.Controller(), a.Source) {
					continue
				}
				g.pumps = append(g.pumps, pumpRecord{
					Card: cid, Timestamp: timestamp, Power: power, Toughness: toughness,
					Keywords: grant.keywords, Hidden: grant.hidden, Switched: grant.switched, Permanent: permanent,
					HasUntil: untilNextTurn, UntilTurnOf: a.Controller,
				})
			}
		}
	}
	return nil
}

// pumpAllZones reads PumpZone$ as the comma list of zones to scan --
// Battlefield alone when absent, ZoneType.listValueOf's own Java default.
// An unrecognized zone name is skipped rather than failing the whole line:
// the same looseness hasZone (trigger.go) already has for a zone list, and
// every real PumpZone$ value in the corpus is already one of ZoneType's own
// names.
func pumpAllZones(a *compile.Ability) []ZoneType {
	v, ok := a.Param("PumpZone")
	if !ok {
		return []ZoneType{Battlefield}
	}
	var zones []ZoneType
	for _, name := range strings.Split(v, ",") {
		if z, ok := ZoneByName(name); ok {
			zones = append(zones, z)
		}
	}
	return zones
}
