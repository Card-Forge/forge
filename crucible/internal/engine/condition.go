// SpellAbilityCondition's own Condition$ family, trimmed to the two real
// corpus shapes M6's own callers need -- a zone-presence count
// (ConditionPresent$/ConditionCompare$) and a named-SVar comparison
// (ConditionCheckSVar$/ConditionSVarCompare$). Shared, so neither owns it,
// the identical "shared, so neither" reason amount.go/defined.go each sit
// apart from their own first callers: checkland-style replacement effects
// (replacement.go's own tapAbilityResolvesTap) and dealDamageEffect
// (dealdamageeffect.go) both gate an ability's own resolution on it, a
// distinct question from CardTraitBase's own meetsCommonRequirements a
// trigger checks before firing at all (triggerCommonRequirementsMet,
// trigger.go) -- the two families share param shapes (isPresentMatches/
// checkSVarMatches, trigger.go, generalized to take either family's own key
// names) but never their own vocabulary: Condition$ itself (Threshold,
// Metalcraft, Delirium, Hellbent, Revolt, Kicked, Surge, Bargain, ...) is
// SpellAbilityCondition's OWN separate switch (setConditions,
// SpellAbilityCondition.java), built here only for the values
// conditionValueRead names (Kicked, OptionalCost, Bargain, Foretold) -- any
// other skips via subAbilityUnresolvedParams below, the identical GO-7
// contract.

package engine

import (
	"fmt"
	"slices"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// subAbilityUnresolvedParams names SpellAbilityCondition's own remaining
// params past ConditionPresent$/ConditionCompare$/ConditionDefined$/
// ConditionCheckSVar$/ConditionSVarCompare$: Condition$ itself (Threshold,
// Metalcraft, ... -- its own separate flag switch, distinct from
// CardTraitBase's identically-named but differently-evaluated Condition$
// gate a trigger checks), ConditionActivationLimit$/ConditionGameTypes$/
// ConditionSorcerySpeed$/ConditionTargetValidTargeting$/ConditionTargetsSingleTarget$
// (each its own further mechanic this port tracks nothing for). A line naming any of these -- a
// Condition$ value conditionValueRead does not name -- records a pending
// error and reads false, so the game that reached it fails instead of
// silently skipping the ability (GO-7). ConditionZone$ is not among them:
// isPresentMatches reads it as ConditionPresent$'s zone list
// (SpellAbilityCondition.java:184-185, :354).
var subAbilityUnresolvedParams = [...]string{
	"Condition",
	"ConditionActivationLimit",
	"ConditionGameTypes", "ConditionSorcerySpeed", "ConditionTargetValidTargeting",
	"ConditionTargetsSingleTarget",
}

// subAbilityConditionMet reports whether a's own SpellAbilityCondition gate
// permits it to resolve: ConditionPlayerTurn$ (the host's controller is --
// or, for "False", is not -- the active player; Java asks the activator,
// which is the host's controller for every ability this port resolves),
// ConditionPhases$ (the current phase is in parsePhaseRange's set),
// ConditionFirstCombat$ (Game.isFirstCombat), ConditionPresent$/ConditionCompare$ (a zone-scan
// count against Battlefield, isPresentMatches' own default when
// ConditionZone$ is absent, which it always is for every real line this
// resolves), ConditionPresent2$/ConditionCompare2$/ConditionDefined2$ beside
// a first one, ConditionPlayerContains$ (playerContainsMet),
// ConditionLifeTotal$ (conditionLifeTotalMet), ConditionManaSpent$/
// ConditionManaNotSpent$ (conditionManaSpentMet) and
// ConditionCheckSVar$/ConditionSVarCompare$ (a named-SVar
// comparison), each absent meaning met -- SpellAbilityCondition.areMet's own
// "every unset condition passes" contract. Neither host nor amounts is a's
// own: both belong to the ability's HOST card (the land carrying a checkland
// SVar, the permanent resolving DealDamage), matching every other
// ability-condition check in this port (ptParam, continuousConditionMet,
// triggerCommonRequirementsMet) reading its own host rather than the
// sub-ability's.
func subAbilityConditionMet(g *Game, host *Card, amounts map[string]expr.Amount, a *compile.Ability) bool {
	for _, key := range subAbilityUnresolvedParams {
		v, ok := a.Param(key)
		if !ok {
			continue
		}
		if key == "Condition" && conditionValueRead(v) {
			if conditionValueMet(g, host, a, v) {
				continue
			}
			return false
		}
		// A condition this port cannot evaluate is not an unmet one: the
		// ability fails instead of silently skipping (GO-7).
		g.recordPendingError(fmt.Errorf("engine: %s$ %q not resolvable yet", key, v))
		return false
	}
	if turn, ok := a.Param("ConditionPlayerTurn"); ok {
		mine := g.ActivePlayer() == host.Controller()
		if (turn == "False") == mine {
			return false
		}
	}
	if _, ok := a.Param("ConditionFirstCombat"); ok && !g.isFirstCombat() {
		return false
	}
	if phases, ok := a.Param("ConditionPhases"); ok {
		set, recognized := parsePhaseRange(phases)
		if !recognized || !set.has(g.activePhase) {
			return false
		}
	}
	if !isPresentMatches(g, host, amounts, a, "ConditionPresent", "ConditionCompare", "ConditionDefined", "ConditionZone", "ConditionPlayer") {
		return false
	}
	// SpellAbilityCondition.java:169-174, :376-402: the second presence check,
	// read only beside a first one, over the same ConditionZone$.
	if _, ok := a.Param("ConditionPresent"); ok &&
		!isPresentMatches(g, host, amounts, a, "ConditionPresent2", "ConditionCompare2", "ConditionDefined2", "ConditionZone", "ConditionPlayer") {
		return false
	}
	if !playerContainsMet(g, host, a) || !conditionLifeTotalMet(g, host, amounts, a) || !conditionManaSpentMet(g, host, a) ||
		!conditionColorsMet(g, host, a) {
		return false
	}
	return conditionSVarMet(g, host, amounts, a)
}

// conditionSVarMet is SpellAbilityCondition's ConditionCheckSVar$ block
// (SpellAbilityCondition.java:221-237, :492-507): the SVar against
// ConditionSVarCompare$, or -- OrOtherConditionSVarCompare$ -- a second
// comparison of OrConditionCheckSVar$ (the same SVar when absent) that also
// lets it pass. Variable Solutions' "If X is 1 ... If X is 4 or more, ... do
// all of the above".
func conditionSVarMet(g *Game, host *Card, amounts map[string]expr.Amount, a *compile.Ability) bool {
	if _, ok := a.Param("ConditionCheckSVar"); !ok {
		return true
	}
	first := checkSVarMatches(g, host, amounts, a, "ConditionCheckSVar", "ConditionSVarCompare", "")
	compare2, ok := a.Param("OrOtherConditionSVarCompare")
	if !ok || first {
		return first
	}
	check2, ok := a.Param("OrConditionCheckSVar")
	if !ok {
		check2, _ = a.Param("ConditionCheckSVar")
	}
	left, ok := resolveNamedAmount(g, amounts, host, check2)
	if !ok {
		g.recordPendingError(fmt.Errorf("engine: OrConditionCheckSVar$ %q is not resolvable", check2))
		return false
	}
	if len(compare2) < 3 {
		g.recordPendingError(fmt.Errorf("engine: OrOtherConditionSVarCompare$ %q is not a comparison", compare2))
		return false
	}
	right, ok := resolveNamedAmount(g, amounts, host, compare2[2:])
	if !ok {
		g.recordPendingError(fmt.Errorf("engine: OrOtherConditionSVarCompare$ %q is not resolvable", compare2))
		return false
	}
	return compareOp(left, compare2[:2], right)
}

// conditionRefs is the refs of host's ability in context, the sa Java hands
// AbilityUtils.getDefinedPlayers for a condition; none outside it.
func conditionRefs(g *Game, host *Card) abilityRefs {
	if r := g.resolving; r != nil && r.Source == host.ID {
		return r.refs()
	}
	return abilityRefs{}
}

// playerContainsMet is ConditionPlayerContains$ over ConditionPlayerDefined$
// (SpellAbilityCondition.java:404-413): every player Contains names is among
// those Defined names, and Contains names at least one. ConditionPlayerDefined$
// alone checks nothing, as in Java. A Defined$ this port cannot read is a
// pending error (GO-7).
func playerContainsMet(g *Game, host *Card, a *compile.Ability) bool {
	contains, ok := a.Param("ConditionPlayerContains")
	if !ok {
		return true
	}
	refs := conditionRefs(g, host)
	var list []PlayerID
	if defined, ok := a.Param("ConditionPlayerDefined"); ok {
		ps, err := definedPlayers(g, host.Controller(), host.ID, defined, refs)
		if err != nil {
			g.recordPendingError(fmt.Errorf("engine: ConditionPlayerDefined$: %w", err))
			return false
		}
		list = ps
	}
	want, err := definedPlayers(g, host.Controller(), host.ID, contains, refs)
	if err != nil {
		g.recordPendingError(fmt.Errorf("engine: ConditionPlayerContains$: %w", err))
		return false
	}
	if len(want) == 0 {
		return false
	}
	for _, p := range want {
		if !slices.Contains(list, p) {
			return false
		}
	}
	return true
}

// conditionLifeTotalMet is ConditionLifeTotal$/ConditionLifeAmount$
// (SpellAbilityCondition.java:415-429): the life of the first player the
// Defined$ names against the amount (GE1 by default), whose right side is a
// number or "X", the host's SVar X -- Java parses anything else as an
// integer. No player, an unreadable Defined$ or right side is a pending error
// (GO-7): Java throws on the first two.
func conditionLifeTotalMet(g *Game, host *Card, amounts map[string]expr.Amount, a *compile.Ability) bool {
	defined, ok := a.Param("ConditionLifeTotal")
	if !ok {
		return true
	}
	players, err := definedPlayers(g, host.Controller(), host.ID, defined, conditionRefs(g, host))
	if err != nil || len(players) == 0 {
		g.recordPendingError(fmt.Errorf("engine: ConditionLifeTotal$ %q names no player", defined))
		return false
	}
	compare, ok := a.Param("ConditionLifeAmount")
	if !ok {
		compare = "GE1"
	}
	if len(compare) < 3 {
		g.recordPendingError(fmt.Errorf("engine: ConditionLifeAmount$ %q is not a comparison", compare))
		return false
	}
	rhs := compare[2:]
	right, err := strconv.Atoi(rhs)
	if rhs == "X" {
		var ok bool
		right, ok = resolveNamedAmount(g, amounts, host, "X")
		if !ok {
			err = fmt.Errorf("X")
		} else {
			err = nil
		}
	}
	if err != nil {
		g.recordPendingError(fmt.Errorf("engine: ConditionLifeAmount$ %q is not resolvable", compare))
		return false
	}
	return javaCompare(g.Player(players[0]).Life, compare, right)
}

// conditionColorsMet is ConditionNoDifferentColors$ (SpellAbilityCondition.
// java:286-298: the cards the Defined$ names all have the first one's colors,
// and there is a first one -- Dead Ringers' "unless either one is a color the
// other isn't") and ConditionChosenColor$ (:342-346, Card.hasChosenColor: the
// host chose that color). An unreadable Defined$ or a value that is no color
// is a pending error (GO-7).
func conditionColorsMet(g *Game, host *Card, a *compile.Ability) bool {
	if defined, ok := a.Param("ConditionNoDifferentColors"); ok {
		cards, err := definedCards(host, defined, conditionRefs(g, host))
		if err != nil {
			g.recordPendingError(fmt.Errorf("engine: ConditionNoDifferentColors$: %w", err))
			return false
		}
		if len(cards) == 0 {
			return false
		}
		first := g.Card(cards[0]).Colors()
		for _, id := range cards[1:] {
			if g.Card(id).Colors() != first {
				return false
			}
		}
	}
	if v, ok := a.Param("ConditionChosenColor"); ok {
		color, ok := manaAtomColor(v)
		if !ok || color.Count() != 1 {
			g.recordPendingError(fmt.Errorf("engine: ConditionChosenColor$ %q not resolvable", v))
			return false
		}
		if !host.Memory.ChosenColors().HasAny(color) {
			return false
		}
	}
	return true
}

// conditionManaSpentMet is ConditionManaSpent$/ConditionManaNotSpent$
// (SpellAbilityCondition.java:476-490): the host's cast spent every color the
// space-separated names give (ColorSet.fromNames), or not every one. Spent
// fails for a host that was not cast, NotSpent passes. A name that is no
// color is a pending error (GO-7).
func conditionManaSpentMet(g *Game, host *Card, a *compile.Ability) bool {
	for _, key := range [...]string{"ConditionManaSpent", "ConditionManaNotSpent"} {
		v, ok := a.Param(key)
		if !ok || v == "" {
			continue
		}
		var colors mana.Colors
		for _, name := range strings.Fields(v) {
			c, ok := manaAtomColor(name)
			if !ok {
				g.recordPendingError(fmt.Errorf("engine: %s$ %q not resolvable", key, v))
				return false
			}
			colors |= c
		}
		if (key == "ConditionManaSpent") != castSpentAllColors(host, colors) {
			return false
		}
	}
	return true
}

// conditionValueMet is SpellAbilityCondition's Condition$ for the values this
// port reads (conditionValueRead): the Kicked family, the activator's Threshold,
// Metalcraft, Delirium, Hellbent, Revolt, Blessing and EnduringStory, AltCost,
// and OptionalCost --
// the spell was cast with a Mode$ OptionalCost static's Generic cost paid,
// or with ConditionOptionalPaid$ False was not (SpellAbilityCondition.java:
// 118-128, :283-284; the flag defaults to true and parses as
// Boolean.parseBoolean). Any other value is false here (GO-7).
func conditionValueMet(g *Game, host *Card, a *compile.Ability, value string) bool {
	switch value {
	case "OptionalCost":
		want := true
		if v, ok := a.Param("ConditionOptionalPaid"); ok {
			want = strings.EqualFold(v, "true")
		}
		return g.spellOptional(host.ID).has(optGeneric) == want
	case "Bargain":
		// SpellAbilityCondition.java:109-110, :279: the spell was bargained.
		return g.spellOptional(host.ID).has(optBargain)
	case "Foretold":
		// SpellAbilityCondition.java:122-123, :280: the spell was cast for its
		// foretell cost.
		return g.spellOptional(host.ID).foretold
	case "AltCost":
		// :119-120, :278: the spell's own Mode$ AlternativeCost was paid.
		return g.spellOptional(host.ID).has(optAltCost)
	case "Threshold", "Metalcraft", "Delirium", "Hellbent", "Revolt", "Blessing", "EnduringStory":
		// :266-272: the activator's own state, the host's controller here.
		you := host.Controller()
		switch value {
		case "Threshold":
			return len(g.Zone(Graveyard, you).Cards()) >= 7
		case "Metalcraft":
			return battlefieldArtifactCount(g, you) >= 3
		case "Delirium":
			return graveyardCoreTypeCount(g, you) >= 4
		case "Hellbent":
			return len(g.Zone(Hand, you).Cards()) == 0
		case "Revolt":
			return g.hasRevolt(you)
		case "Blessing":
			return g.Player(you).Blessing
		default:
			return g.Player(you).EnduringStory
		}
	}
	return kickedConditionMet(host, value)
}

// conditionValueRead reports whether conditionValueMet evaluates value.
func conditionValueRead(value string) bool {
	switch value {
	case "Kicked", "Kicked 1", "Kicked 2", "OptionalCost", "Bargain", "Foretold", "AltCost",
		"Threshold", "Metalcraft", "Delirium", "Hellbent", "Revolt", "Blessing", "EnduringStory":
		return true
	}
	return false
}

// rejectUnreadCondition is the error an effect returns for a Condition$ it
// would otherwise skip silently: nil when the param is absent or names a value
// subAbilityConditionMet evaluates, which the effect then gates on.
func rejectUnreadCondition(a *Ability, api string) error {
	if v, ok := a.Params.Param("Condition"); ok && !conditionValueRead(v) {
		return fmt.Errorf("engine: %s: Condition$ %q not resolvable yet", api, v)
	}
	return nil
}

// kickedConditionMet is SpellAbilityCondition's Condition$ Kicked / Kicked 1 /
// Kicked 2 (SpellAbilityCondition.java:97-104,274-276): the host was cast with a
// kicker paid. Any other Condition$ value is false here (GO-7).
func kickedConditionMet(host *Card, value string) bool {
	switch value {
	case "Kicked":
		return host.kickerMagnitude() > 0
	case "Kicked 1":
		return host.kicker&kicker1 != 0
	case "Kicked 2":
		return host.kicker&kicker2 != 0
	}
	return false
}
