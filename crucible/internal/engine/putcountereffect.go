// PutCounter: CR 121.1, the corpus's own second-largest resolvable slice
// after Pump -- 993 of the corpus's 3,165 real (AB|DB)$ PutCounter lines
// that name a single literal CounterType$, Defined$ Self/Enchanted/
// Equipped/You, and carry no other unresolved param -- 1 of them naming
// Planeswalker$ too, no longer blocked (below): CR 606.3's own
// loyalty-ability restriction is a cost-side gate (ActivateAbility/
// ActivateManaAbility, activateability.go), never a restriction on how the
// effect it pays for actually resolves.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/CountersPutEffect.java's
// resolve/resolvePerType, trimmed hard: that file is 800 lines wide with
// Bolster/Monstrosity/Adapt/Support/Choices/DividedRandomly/PutOnEachOther/
// PutOnDefined/EachFromSource/the ETB counter-table replacement path and
// more, each its own further mechanic this port does not have anywhere to
// route through yet -- every one of them fails this line loudly rather than
// silently dropping half of what it asked for (PORT-8/GO-7).

package engine

//enginelint:allow id card game player ability event defined amount condition parts control

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/keyword"
)

// putCounterUnresolvedParams names CountersPutEffect's own params past
// CounterType$/CounterNum$/Defined$ this port does not evaluate. Every one
// fails the whole line loudly. TargetMin$/TargetMax$ are not among them:
// resolveTargets (targeting.go) already asks for that many targets, and
// each one chosen gets CounterNum$ counters. ETB$
// (154) -- CR 614's own counters-added-simultaneously replacement table
// (GameEntityCounterTable), the identical batching risk ChangesZoneAll's
// own gap already documents (Not ported yet); Choices$ and its own
// ChoiceTitle$/ChoiceAmount$/MinChoiceAmount$/ChoicesDesc$/ChoiceZone$/
// ChoiceOptional$ (46 combined) -- an interactive multi-card choice this
// port's own PlayerController has no hook for; DividedRandomly$/SplitAmount$
// -- each its own distribution mechanic (DividedAsYouChoose$ over the
// line's own targets resolves: divideAmongTargets, dividedtargets.go);
// Monstrosity$/Adapt$/Bolster$/Support$/PowerUp$/Exhaust$ -- each its own
// further keyword-ability mechanic buildSpellAbility itself rewrites into
// this shape from a simpler keyword line; EachFromSource$/PutOnEachOther$/
// PutOnDefined$/ChooseDifferent$/EachExistingCounter$/UniqueType$/
// CounterTypePerDefined$/CounterNumPerDefined$/OnlyNewKind$/
// SkipReceiveCounters$/RandomType$ -- each its own further per-target
// mechanic; CounterTypes$ (17, plural) -- a different multi-type shape,
// not this one; ForColor$/SharedKeywords$/SharedKeywordsDefined$/
// SharedKeywordsZone$/SharedRestrictions$/TriggeredCounterMap$/
// CounterMapValues$/SpecifyCounter$/Placer$/RememberCards$/RemovePhase$/
// Optional$/UpTo$/UpToMin$ -- each its own further mechanic.
//
// Condition$ itself and ConditionDefined$/ConditionZone$/
// ConditionPlayerTurn$/ConditionActivationLimit$/ConditionPresent2$/
// ConditionCompare2$ -- SpellAbilityCondition's own shapes
// subAbilityConditionMet does not cover, the identical Pump/GainLife-shaped
// gap.
//
// SubAbility$ no longer blocks: resolveSubAbility (subability.go) chains it
// through Registry.Resolve (effect.go) once this effect's own body
// finishes, whether or not subAbilityConditionMet let it run at all --
// well_rested.txt's own real PutCounter-chaining-into-GainLife shape is
// why. 56 of the corpus's own 623 real SVar-defined PutCounter lines
// naming SubAbility$ chain to an already-built leaf ability and resolve
// end to end.
var putCounterUnresolvedParams = [...]string{
	"ETB",
	"Choices", "ChoiceTitle", "ChoiceAmount", "MinChoiceAmount", "ChoicesDesc", "ChoiceZone", "ChoiceOptional",
	"DividedRandomly", "SplitAmount",
	"Monstrosity", "Adapt", "Bolster", "Support", "PowerUp", "Exhaust",
	"EachFromSource", "PutOnEachOther", "PutOnDefined", "ChooseDifferent", "EachExistingCounter", "UniqueType",
	"CounterTypePerDefined", "CounterNumPerDefined", "OnlyNewKind", "SkipReceiveCounters", "RandomType",
	"CounterTypes", "ForColor", "SharedKeywords", "SharedKeywordsDefined", "SharedKeywordsZone",
	"SharedRestrictions", "TriggeredCounterMap", "CounterMapValues", "SpecifyCounter", "Placer",
	"RememberCards", "RemovePhase", "Optional", "UpTo", "UpToMin",
	"Condition", "ConditionZone", "ConditionPlayerTurn", "ConditionActivationLimit",
	"ConditionPresent2", "ConditionCompare2",
}

// putCounterEffect resolves Mode$/DB$/AB$ PutCounter for the Defined$
// Self/Enchanted/Equipped/You shape -- no target. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go) the identical way
// DealDamage's/GainLife's/Pump's own do.
type putCounterEffect struct{}

func (putCounterEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	// ETB$ inside a replacement of a card's entry puts the counters into the
	// entry itself (putEnterCounters); anywhere else it is still the counter
	// table this port does not batch.
	_, etb := a.Params.Param("ETB")
	enter := etb && a.replacedCard() != NoCard
	for _, key := range putCounterUnresolvedParams {
		if enter && (key == "ETB" || key == "EachFromSource") {
			continue
		}
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: PutCounter: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	if enter {
		return g.putEnterCounters(a, source)
	}

	counterType, err := putCounterType(a.Params)
	if err != nil {
		return err
	}

	counterNum, ok := a.Params.Param("CounterNum")
	if !ok {
		counterNum = "1"
	}
	amount, ok := resolveNamedAmount(g, a.Amounts, source, counterNum)
	if !ok {
		return fmt.Errorf("engine: PutCounter: CounterNum$ %q is not resolvable", counterNum)
	}

	var cards []CardID
	var players []PlayerID
	defined, hasDefined := a.Params.Param("Defined")
	_, targeted := a.Params.Param("ValidTgts")
	divided := a.isDivided()
	if divided && (!targeted || hasDefined) {
		// Untargeted, the division is asked card by card as the effect
		// resolves (CountersPutEffect.java:470-483, Choices$-chosen cards);
		// with Defined$ the cards read are not the targets the division was
		// announced over.
		return fmt.Errorf("engine: PutCounter: DividedAsYouChoose$ without its own targets not resolvable yet")
	}
	if a.targetsErr != nil {
		// The targets, or their division, could not be asked for as the
		// ability went on the stack (resolveTargets).
		return fmt.Errorf("engine: PutCounter: %w", a.targetsErr)
	}
	if targeted && !hasDefined {
		// getDefinedEntitiesOrTargeted (SpellAbilityEffect.java:376-383):
		// the chosen targets, a card or a player each -- every one of them,
		// "each of up to N target creatures" (TargetMin$/TargetMax$) getting
		// CounterNum$ apiece (CountersPutEffect.java:279-570). An "up to"
		// choice of none puts nothing and the SubAbility$ chain still runs.
		for _, e := range a.Targets {
			if id, ok := e.AsCard(); ok {
				cards = append(cards, id)
			} else if pid, ok := e.AsPlayer(); ok {
				players = append(players, pid)
			}
		}
	} else {
		// Defined$ wins over the targets when both are named (definedFirst,
		// SpellAbilityEffect.java:379): Drillworks Mole's "Self & Targeted".
		// No Defined$ is the host (AbilityUtils.getDefinedEntities' "default
		// to Self", AbilityUtils.java:1220): Murktide Regent's "put a +1/+1
		// counter on CARDNAME".
		if !hasDefined {
			defined = "Self"
		}
		for _, part := range strings.Split(defined, " & ") {
			c, p, err := definedCounterTargets(g, a.Controller, source, part, a.refs())
			if err != nil {
				return fmt.Errorf("engine: PutCounter: %w", err)
			}
			cards = append(cards, c...)
			players = append(players, p...)
		}
	}

	// Each card's amount, worked out before any counter is placed so a
	// missing share fails the line before it acts (GO-7).
	amounts := make([]int, len(cards))
	for i, cid := range cards {
		amounts[i] = amount
		if !divided {
			continue
		}
		// A targeted DividedAsYouChoose$ line puts on each target the share
		// announced as it was chosen, CounterNum$ unread
		// (CountersPutEffect.java:460-461).
		share, ok := a.dividedShareOf(CardEntity(cid))
		if !ok {
			return fmt.Errorf("engine: PutCounter: DividedAsYouChoose$: no division was announced for target %d", cid)
		}
		amounts[i] = share
	}
	for i, cid := range cards {
		n := g.countersReplaced(controller, a.Controller, CardEntity(cid), counterType, amounts[i])
		if n <= 0 {
			continue
		}
		g.addCardCounters(controller, a.Source, cid, counterType, n)
	}
	if keyword.Parse(a.Params.Keyword).Name == "Renown" {
		// CountersPutEffect.java:553: whatever the counters did.
		for _, cid := range cards {
			g.Card(cid).renowned = true
		}
	}
	for _, pid := range players {
		n := g.countersReplaced(controller, a.Controller, PlayerEntity(pid), counterType, amount)
		if n <= 0 {
			continue
		}
		g.Player(pid).Counters.Add(counterType, n)
		emitCounterChanged(g, a.Source, PlayerEntity(pid), counterType, n)
	}
	return nil
}

// putCounterType reads CounterType$ as a single literal counter type name,
// uppercased -- CounterEnumType.getType's own canonicalization
// (`name.toUpperCase(Locale.ROOT)`), so a corpus card writing "Stun" and
// another writing "STUN" (both real, 74 and 23 lines respectively) land on
// the identical CounterType key rather than silently tracking two different
// kinds of counter on the same card. This port's own CounterType has no
// keyword-counter/custom-type split Java's own dual-namespace
// CounterKeywordType/CounterCustomType maintains (a keyword counter like
// "Flying" stays keyed by its own title-case string in Java, not
// uppercased) -- nothing downstream reads a counter's own kind to grant the
// keyword it names yet, so internal consistency on this port's own side is
// all uppercasing has to buy today, not exact parity with Java's naming.
//
// A comma-separated list (22 real lines, an interactive choice among
// several types), "ExistingCounter" (8, EachExistingCounter's own
// hard-coded literal, its own further mechanic) and "Any"
// (CounterType.getType's own case-insensitive nil sentinel, "counters of
// any kind" rather than a concrete kind to add) all fail loudly rather than
// guessing which one to add.
func putCounterType(a *compile.Ability) (CounterType, error) {
	raw, ok := a.Param("CounterType")
	if !ok {
		return "", fmt.Errorf("engine: PutCounter: CounterType$ missing")
	}
	if strings.Contains(raw, ",") || raw == "ExistingCounter" || strings.EqualFold(raw, "Any") {
		return "", fmt.Errorf("engine: PutCounter: CounterType$ %q not resolvable yet", raw)
	}
	return CounterType(strings.ToUpper(raw)), nil
}

// definedCounterTargets resolves Defined$ to what receives the counters --
// a player (You/Opponent/Player.Opponent, definedPlayers) or a card
// (everything else, definedCards) -- CountersPutEffect.resolvePerType's own
// dispatch is on the resolved GameEntity's own runtime type
// (`obj instanceof Player`/`obj instanceof Card`), not on CounterType$
// itself: a player-only counter kind (energy, poison, ...) is only ever
// reached because Defined$ itself names a player, the identical shape this
// dispatch reproduces.
func definedCounterTargets(g *Game, controller PlayerID, host *Card, defined string, refs abilityRefs) ([]CardID, []PlayerID, error) {
	switch defined {
	case "You", "Opponent", "Player.Opponent":
		players, err := definedPlayers(g, controller, host.ID, defined, refs)
		return nil, players, err
	default:
		cards, err := definedCards(host, defined, refs)
		return cards, nil, err
	}
}
