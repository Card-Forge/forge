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
// the identical Pump-shaped gap; SharedKeywordsZone$/SharedRestrictions$ (4/4) --
// CardFactoryUtil.sharedKeywords' own zone scan, a further mechanic;
// ModeCost$ (3) and Exhaust$ (4) -- each its own further activation
// mechanic. AtEOT$ resolves (registerAtEOT) over every matched card.
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
	"SharedKeywordsZone", "SharedRestrictions",
	"ModeCost", "Exhaust",
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
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range pumpAllUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: PumpAll: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	permanent, untilNextTurn, perpetual := false, false, false
	until := ""
	if d, ok := a.Params.Param("Duration"); ok {
		switch d {
		case "Permanent":
			permanent = true
		case "Perpetual":
			permanent, perpetual = true, true
		case "UntilYourNextTurn":
			// SpellAbilityEffect.addUntilCommand (:980-981), as Pump's.
			permanent, untilNextTurn = true, true
		default:
			// PumpAllEffect.java:122, then :92's addUntilCommand per card.
			if err := durationRefused("PumpAll", d); err != nil {
				return err
			}
			if !g.checkValidDuration(a, d) {
				return nil
			}
			if !endsAtCleanup(d) {
				permanent, until = true, d
			}
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
	if perpetual && (len(grant.hidden) > 0 || grant.switched) {
		return fmt.Errorf("engine: PumpAll: Duration$ Perpetual with a HIDDEN keyword not resolvable yet")
	}
	pumpsNothing := power == 0 && toughness == 0 && grant.empty()
	if pumpsNothing && !hasParam(a, "AtEOT") && !hasParam(a, "RememberPumped") {
		return nil
	}

	validCards, ok := a.Params.Param("ValidCards")
	if !ok {
		return fmt.Errorf("engine: PumpAll: ValidCards$ missing")
	}
	spec := valid.Parse(validCards)

	var players []PlayerID
	if _, targeting := a.Params.Param("ValidTgts"); targeting {
		// PumpAllEffect.java:133-137: a targeting line pumps what the targeted
		// players have (getTargetPlayers).
		for _, e := range a.refs().targets {
			if pid, ok := e.AsPlayer(); ok {
				players = append(players, pid)
			}
		}
	} else if defined, ok := a.Params.Param("Defined"); ok {
		players, err = definedPlayers(g, a.Controller, a.Source, defined, a.refs())
		if err != nil {
			return fmt.Errorf("engine: PumpAll: %w", err)
		}
	} else {
		players = g.Players()
	}

	g.timestamp++
	timestamp := g.timestamp
	var matched []CardID
	for _, pid := range players {
		for _, z := range pumpAllZones(a.Params) {
			for _, cid := range g.Zone(z, pid).Cards() {
				if !Matches(g, g.Card(cid), spec, source.Controller(), a.Source) {
					continue
				}
				matched = append(matched, cid)
				if hasParam(a, "RememberPumped") {
					source.Memory.Remember(CardEntity(cid))
				}
				if pumpsNothing {
					continue
				}
				g.pumps = append(g.pumps, pumpRecord{
					Card: cid, Timestamp: timestamp, Power: power, Toughness: toughness,
					Keywords: grant.keywords, Hidden: grant.hidden, Switched: grant.switched, Permanent: permanent,
					HasUntil: untilNextTurn, UntilTurnOf: a.Controller, Perpetual: perpetual,
				})
				if until != "" {
					if err := g.addUntilCommand(a, until, cardCommand{Kind: commandEndPump, Target: cid, Timestamp: timestamp}); err != nil {
						return err
					}
				}
			}
		}
	}
	// PumpAllEffect.java:98-100: AtEOT$ remembers every pumped card.
	return registerAtEOT(g, a, matched)
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
