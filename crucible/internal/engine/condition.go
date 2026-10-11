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
// SpellAbilityCondition.java), not built here -- a card naming it skips via
// subAbilityUnresolvedParams below, the identical GO-7 contract.

package engine

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/expr"
)

// subAbilityUnresolvedParams names SpellAbilityCondition's own remaining
// params past ConditionPresent$/ConditionCompare$/ConditionDefined$/
// ConditionCheckSVar$/ConditionSVarCompare$: Condition$ itself (Threshold,
// Metalcraft, ... -- its own separate flag switch, distinct from
// CardTraitBase's identically-named but differently-evaluated Condition$
// gate a trigger checks), ConditionPresent2$/ConditionCompare2$ (a second,
// independent zone-presence check no real corpus line combines with the
// shapes built here), ConditionZone$ (present on 0 real DB$ Tap/DealDamage
// lines carrying a resolved shape, so the isPresentMatches default,
// Battlefield alone, is never actually overridden -- naming it anyway means
// a shape this function was not built to check), ConditionPlayerTurn$/
// ConditionPhases$/ConditionActivationLimit$/ConditionGameTypes$/
// ConditionSorcerySpeed$/ConditionChosenColor$/ConditionSorcerySpeed$/
// ConditionSpeed$/ConditionSpeed$/ConditionLifeTotal$/ConditionLifeAmount$/
// ConditionManaSpent$/ConditionManaNotSpent$/ConditionPlayerDefined$/
// ConditionPlayerContains$/ConditionNoDifferentColors$/
// ConditionTargetValidTargeting$/ConditionTargetsSingleTarget$/
// OrOtherConditionSVarCompare$/OrConditionCheckSVar$ (each its own further
// mechanic this port tracks nothing for). A line naming any of these skips
// entirely rather than resolving the params it does recognize and guessing
// wrong about the rest (PORT-8/GO-7).
var subAbilityUnresolvedParams = [...]string{
	"Condition", "ConditionPresent2", "ConditionCompare2", "ConditionZone",
	"ConditionActivationLimit",
	"ConditionGameTypes", "ConditionSorcerySpeed", "ConditionChosenColor",
	"ConditionLifeTotal", "ConditionLifeAmount", "ConditionManaSpent",
	"ConditionManaNotSpent", "ConditionPlayerDefined", "ConditionPlayerContains",
	"ConditionNoDifferentColors", "ConditionTargetValidTargeting",
	"ConditionTargetsSingleTarget", "OrOtherConditionSVarCompare",
	"OrConditionCheckSVar",
}

// subAbilityConditionMet reports whether a's own SpellAbilityCondition gate
// permits it to resolve: ConditionPlayerTurn$ (the host's controller is --
// or, for "False", is not -- the active player; Java asks the activator,
// which is the host's controller for every ability this port resolves),
// ConditionPhases$ (the current phase is in parsePhaseRange's set),
// ConditionFirstCombat$ (Game.isFirstCombat), ConditionPresent$/ConditionCompare$ (a zone-scan
// count against Battlefield, isPresentMatches' own default when
// ConditionZone$ is absent, which it always is for every real line this
// resolves) and ConditionCheckSVar$/ConditionSVarCompare$ (a named-SVar
// comparison), both absent meaning met -- SpellAbilityCondition.areMet's own
// "every unset condition passes" contract. Neither host nor amounts is a's
// own: both belong to the ability's HOST card (the land carrying a checkland
// SVar, the permanent resolving DealDamage), matching every other
// ability-condition check in this port (ptParam, continuousConditionMet,
// triggerCommonRequirementsMet) reading its own host rather than the
// sub-ability's.
func subAbilityConditionMet(g *Game, host *Card, amounts map[string]expr.Amount, a *compile.Ability) bool {
	for _, key := range subAbilityUnresolvedParams {
		if v, ok := a.Param(key); ok {
			if key == "Condition" && conditionValueMet(g, host, a, v) {
				continue
			}
			return false
		}
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
	return checkSVarMatches(g, host, amounts, a, "ConditionCheckSVar", "ConditionSVarCompare", "OrOtherConditionSVarCompare")
}

// conditionValueMet is SpellAbilityCondition's Condition$ for the values this
// port reads (conditionValueRead): the Kicked family, and OptionalCost --
// the spell was cast with a Mode$ OptionalCost static's Generic cost paid,
// or with ConditionOptionalPaid$ False was not (SpellAbilityCondition.java:
// 118-128, :283-284; the flag defaults to true and parses as
// Boolean.parseBoolean). Any other value is false here (GO-7).
func conditionValueMet(g *Game, host *Card, a *compile.Ability, value string) bool {
	if value == "OptionalCost" {
		want := true
		if v, ok := a.Param("ConditionOptionalPaid"); ok {
			want = strings.EqualFold(v, "true")
		}
		return g.spellOptional(host.ID).has(optGeneric) == want
	}
	return kickedConditionMet(host, value)
}

// conditionValueRead reports whether conditionValueMet evaluates value.
func conditionValueRead(value string) bool {
	switch value {
	case "Kicked", "Kicked 1", "Kicked 2", "OptionalCost":
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
