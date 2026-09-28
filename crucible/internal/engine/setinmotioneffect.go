// SetInMotion: Archenemy's CR 904 "set a scheme in motion" -- the top card
// of a scheme deck (or, with Again$, the scheme a Mode$ SetInMotion trigger
// just named) moves face up into the Command zone and Mode$ SetInMotion
// triggers fire. The game action itself (Player.setSchemeInMotion) is also
// the archenemy's turn-based action at the start of their precombat main
// phase (PhaseHandler.java:278-280, archenemyMain1), and its counterpart,
// the scheme state-based action that returns a finished non-ongoing scheme
// to the bottom of its deck (GameAction.java:1745-1752), lives here too.
//
// No Archenemy variant gate: SetInMotionEffect.java checks none, and the
// turn-based action's own gate is Player.isArchenemy, "the SchemeDeck is not
// empty" (Player.java:271-273) -- zone state this port already has, so
// unlike Planechase's planechaseActive (ADR-0029) nothing new is stored.

package engine

//enginelint:allow card game ability amount condition control defined effecthelpers zone parts id replacement trigger valid statictrigger

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// setInMotionEffect is SetInMotionEffect.java: the host's controller sets a
// scheme in motion RepeatNum$ times (default once) -- the top of their
// scheme deck, or with Again$ the root ability's triggering scheme
// (AbilityKey.Scheme) once more.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/SetInMotionEffect.java's
// resolve, with Player.setSchemeInMotion (Player.java:275-292) as
// setSchemeInMotion below.
//
// The two real lines: My Laughter Echoes' `DB$ SetInMotion | Again$ True |
// ConditionDefined$ Remembered | ConditionPresent$ Card` resolves, its
// ConditionDefined$ evaluated by definedPresentConditionMet (below).
// Plots That Span Centuries' `DB$ SetInMotion | RepeatNum$ 3` is the
// ReplaceWith$ of an Event$ SetInMotion replacement, which
// setSchemeInMotion refuses (setInMotionReplacement), so RepeatNum$
// resolves here but no real line reaches it yet.
type setInMotionEffect struct{}

func (setInMotionEffect) Resolve(g *Game, a *Ability, c PlayerController) error {
	source := g.Card(a.Source)
	met, err := definedPresentConditionMet(g, source, a, "SetInMotion")
	if err != nil || !met {
		return err
	}
	// Java reads source.getController(), not the activator.
	player := source.Controller()
	repeats, err := optionalAmount(g, a, "SetInMotion", "RepeatNum", 1)
	if err != nil {
		return err
	}
	_, again := a.Params.Param("Again")
	for range repeats {
		scheme := NoCard
		if again {
			// sa.getRootAbility().getTriggeringObject(AbilityKey.Scheme):
			// resolveSubAbility copies the trigger's triggered objects down
			// the chain, so a's own are the root's. Java would throw on a
			// null scheme; refuse instead (GO-7).
			scheme = a.triggered.scheme
			if scheme == NoCard {
				return fmt.Errorf("engine: SetInMotion: Again$ outside a Mode$ SetInMotion trigger: no triggering scheme")
			}
		}
		if err := g.setSchemeInMotion(c, player, scheme); err != nil {
			return err
		}
	}
	return nil
}

// definedPresentConditionMet is subAbilityConditionMet (condition.go) for an
// ability that may name ConditionDefined$: SpellAbilityCondition.areMet's
// getIsPresent branch with getPresentDefined set
// (SpellAbilityCondition.java:348-373) counts the ConditionPresent$ matches
// among the ConditionDefined$ objects instead of a zone scan.
// subAbilityConditionMet's own isPresentMatches treats any ConditionDefined$
// as never met, so a line naming it is evaluated here instead: Defined$
// through definedCards (cards only; every real ConditionPresent$ beside
// ConditionDefined$ on a SetInMotion line is `Card`), ConditionCompare$
// defaulting to GE1. Any other Condition-family key beside ConditionDefined$
// is refused rather than half-checked (GO-7). Without ConditionDefined$ this
// is subAbilityConditionMet unchanged.
func definedPresentConditionMet(g *Game, host *Card, a *Ability, api string) (bool, error) {
	defined, ok := a.Params.Param("ConditionDefined")
	if !ok {
		return subAbilityConditionMet(g, host, a.Amounts, a.Params), nil
	}
	for _, p := range a.Params.Params {
		switch strings.ToLower(p.Key) {
		case "conditiondefined", "conditionpresent", "conditioncompare":
			continue
		}
		if strings.HasPrefix(strings.ToLower(p.Key), "condition") {
			return false, fmt.Errorf("engine: %s: %s$ beside ConditionDefined$ not resolvable yet", api, p.Key)
		}
	}
	present, ok := a.Params.Param("ConditionPresent")
	if !ok {
		return false, fmt.Errorf("engine: %s: ConditionDefined$ without ConditionPresent$ not resolvable yet", api)
	}
	cards, err := definedCards(host, defined, a.refs())
	if err != nil {
		return false, fmt.Errorf("engine: %s: ConditionDefined$: %w", api, err)
	}
	spec := valid.Parse(present)
	n := 0
	for _, id := range cards {
		if Matches(g, g.Card(id), spec, host.Controller(), host.ID) {
			n++
		}
	}
	compare, ok := a.Params.Param("ConditionCompare")
	if !ok {
		compare = "GE1"
	}
	if len(compare) < 3 {
		return false, fmt.Errorf("engine: %s: ConditionCompare$ %q not resolvable", api, compare)
	}
	right, ok := resolveNamedAmount(g, a.Amounts, host, compare[2:])
	if !ok {
		return false, fmt.Errorf("engine: %s: ConditionCompare$ %q not resolvable", api, compare)
	}
	return compareOp(n, compare[:2], right), nil
}

// setSchemeInMotion is Player.setSchemeInMotion (Player.java:275-292):
// player sets scheme in motion -- the top card of player's SchemeDeck when
// scheme is NoCard (setSchemeInMotion(cause)'s getZone(SchemeDeck).get(0)).
// The scheme moves to its owner's Command zone (moveToCommand, which
// suppresses Mode$ ChangesZone; Move fires no trigger itself), then Mode$
// SetInMotion runs with it as AbilityKey.Scheme. A Static$ True trigger's
// error surfaces here.
//
// A live Event$ SetInMotion replacement refuses the whole action before
// anything moves (setInMotionReplacement), and an empty SchemeDeck is an
// error where Java's get(0) would throw.
func (g *Game) setSchemeInMotion(c PlayerController, player PlayerID, scheme CardID) error {
	if name, ok := g.setInMotionReplacement(); ok {
		return fmt.Errorf("engine: SetInMotion: %q's Event$ SetInMotion replacement not resolvable yet", name)
	}
	if scheme == NoCard {
		deck := g.Zone(SchemeDeck, player).Cards()
		if len(deck) == 0 {
			return fmt.Errorf("engine: SetInMotion: scheme deck is empty")
		}
		scheme = deck[0]
	}
	g.Move(scheme, Command, g.Card(scheme).Owner)
	g.checkSetInMotionTriggers(c, scheme)
	return g.TakePendingError()
}

// setInMotionReplacement names the host of a live Event$ SetInMotion
// replacement, if any. ReplacementType.SetInMotion (ReplaceSetInMotion.java)
// is not ported: its two sources are Plots That Span Centuries' effect card
// (a ReplaceWith$ chaining SubAbility$, which runReplaceWith refuses) and
// AddTurn's NoSchemes$ CantHappen effect (addturneffect.go rejects
// NoSchemes$). Its mere presence refuses the action rather than setting a
// scheme in motion as if it were not there -- planeswalkReplacement's shape.
func (g *Game) setInMotionReplacement() (string, bool) {
	name, found := "", false
	g.eachReplacement("SetInMotion", func(h *Card, _ map[string]expr.Amount, _ *compile.Ability) bool {
		name, found = h.Def.Name, true
		return true
	})
	return name, found
}

// archenemyMain1 is PhaseHandler.java:278-280: as the active player's
// precombat main phase begins, an archenemy (Player.isArchenemy, a
// non-empty SchemeDeck) sets the top scheme in motion (CR 904.4). beginStep
// has no error path in bookkeeping mode, so a failure is left for
// TakePendingError (Game.Step takes it).
func (g *Game) archenemyMain1(c PlayerController) {
	if g.Zone(SchemeDeck, g.activePlayer).Len() == 0 {
		return
	}
	if err := g.setSchemeInMotion(c, g.activePlayer, NoCard); err != nil {
		g.recordPendingError(err)
	}
}

// returnFinishedSchemes is GameAction.stateBasedAction_Scheme
// (GameAction.java:1745-1752), CR 704.6f: a face-up non-ongoing scheme in
// the Command zone that is not the source of an ability on the stack goes
// to the bottom of its owner's SchemeDeck. Like completeFinishedDungeons it
// does not count as a performed state-based action (Java leaves checkAgain
// unset for Command-zone cards).
func (g *Game) returnFinishedSchemes() {
	for _, pid := range g.Players() {
		for _, id := range append([]CardID(nil), g.Zone(Command, pid).Cards()...) {
			t := g.Card(id).Type()
			if !t.Has(cardtype.Scheme) || t.HasSupertype(cardtype.Ongoing) || g.hasSourceOnStack(id) {
				continue
			}
			g.Move(id, SchemeDeck, g.Card(id).Owner)
		}
	}
}

// checkSetInMotionTriggers is Mode$ SetInMotion
// (TriggerSetInMotion.performTest), CR 904's "when you set this scheme in
// motion": ValidCard$ against the scheme, nothing else. Static$ True lines
// (0 real lines) resolve inline first through resolveStaticTriggers
// (ADR-0020); the rest are pushed. Every match records scheme as
// AbilityKey.Scheme (setTriggeringObjectsFrom(runParams, AbilityKey.Scheme)).
func (g *Game) checkSetInMotionTriggers(controller PlayerController, scheme CardID) {
	g.resolveStaticTriggers(controller, g.setInMotionTriggerMatches(scheme, true))
	g.pushTriggeredAbilities(controller, g.setInMotionTriggerMatches(scheme, false))
}

// setInMotionTriggerMatches walks planeswalkTriggerZones (every player's
// Battlefield and whole Command zone): a face-up scheme is a plain Command
// card, not an effect card, so traitHosts would miss it. TriggerZones$
// gates each line (phaseTriggerZoneMatches); all 85 real lines name
// Command.
func (g *Game) setInMotionTriggerMatches(scheme CardID, static bool) []Ability {
	objects := triggeredObjects{scheme: scheme}
	sc := g.Card(scheme)
	var matches []Ability
	for _, pid := range g.Players() {
		for _, z := range planeswalkTriggerZones {
			for _, host := range g.Zone(z, pid).Cards() {
				h := g.Card(host)
				if h.Def == nil {
					continue
				}
				for face := range h.triggerFaces {
					for _, t := range face.Triggers {
						if !isSetInMotionTrigger(t) || isStaticTrigger(t) != static || !phaseTriggerZoneMatches(h, t, z) {
							continue
						}
						if spec, ok := t.Param("ValidCard"); ok && !Matches(g, sc, valid.Parse(spec), h.Controller(), host) {
							continue
						}
						if sub, api, optional, ok := triggerEffectAPI(g, h, face.Amounts, t); ok {
							matches = append(matches, Ability{API: api, Source: host, Controller: h.Controller(), Params: sub, Amounts: face.Amounts, Optional: optional, triggered: face.objects(objects)})
						}
					}
				}
			}
		}
	}
	return matches
}

// isSetInMotionTrigger reports whether t is Mode$ SetInMotion.
func isSetInMotionTrigger(t *compile.Ability) bool {
	return strings.EqualFold(t.Name, "SetInMotion")
}
