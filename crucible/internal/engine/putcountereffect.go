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

//enginelint:allow id card game player ability event defined amount condition parts control resolvechoice

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
// own gap already documents (Not ported yet); ChoiceOptional$ (2) -- Java
// passes it as isOptional with the minimum unchanged, which this port's
// ChooseCardsForEffect has no flag for; DividedRandomly$/SplitAmount$
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
// CounterMapValues$/SpecifyCounter$/RememberCards$/RemovePhase$/
// Optional$/UpTo$/UpToMin$ -- each its own further mechanic. Placer$ resolves
// (putCounterPlacer).
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
	"ChoiceOptional",
	"DividedRandomly", "SplitAmount",
	"Bolster",
	"EachFromSource", "PutOnEachOther", "PutOnDefined", "ChooseDifferent", "EachExistingCounter", "UniqueType",
	"CounterTypePerDefined", "CounterNumPerDefined", "OnlyNewKind", "SkipReceiveCounters", "RandomType",
	"CounterTypes", "ForColor", "SharedKeywords", "SharedKeywordsDefined", "SharedKeywordsZone",
	"SharedRestrictions", "TriggeredCounterMap", "CounterMapValues", "SpecifyCounter",
	"RemovePhase", "UpTo", "UpToMin",
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
	_, rememberAmount := a.Params.Param("RememberAmount")
	if enter && rememberAmount {
		// The entry's counters are put by applyEnterCounters with the rest of
		// its table, past the point this ability could remember their total.
		return fmt.Errorf("engine: PutCounter: RememberAmount$ with ETB$ not resolvable yet")
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	placer, err := putCounterPlacer(g, a)
	if err != nil {
		return err
	}
	if _, ok := a.Params.Param("Optional"); ok && enter {
		return fmt.Errorf("engine: PutCounter: Optional$ not resolvable yet")
	}
	if _, ok := a.Params.Param("Optional"); ok && !controller.ConfirmEffect(g, a.Controller, a.Source) {
		// CountersPutEffect.java:183-185: the activator may decline the lot.
		return nil
	}
	if enter {
		return g.putEnterCounters(a, source, placer)
	}

	// Monstrosity$, Adapt$ and Support$ rewrite the ability into +1/+1
	// counters (CountersPutEffect.buildSpellAbility, :740-800); Exhaust$ is an
	// activation limit (activationLimitsMet) and never read here.
	monstrosity, isMonstrosity := a.Params.Param("Monstrosity")
	adapt, isAdapt := a.Params.Param("Adapt")
	_, isSupport := a.Params.Param("Support")
	var counterType CounterType
	var counterNum string
	switch {
	case isMonstrosity:
		counterType, counterNum = P1P1, monstrosity
	case isAdapt:
		counterType, counterNum = P1P1, adapt
	case isSupport:
		counterType, counterNum = P1P1, "1"
	default:
		var err error
		if counterType, err = putCounterType(a.Params); err != nil {
			return err
		}
		var ok bool
		if counterNum, ok = a.Params.Param("CounterNum"); !ok {
			counterNum = "1"
		}
	}
	amount, ok := resolveNamedAmount(g, a.Amounts, source, counterNum)
	if !ok {
		return fmt.Errorf("engine: PutCounter: CounterNum$ %q is not resolvable", counterNum)
	}

	var cards []CardID
	var players []PlayerID
	defined, hasDefined := a.Params.Param("Defined")
	_, targeted := a.Params.Param("ValidTgts")
	targeted = targeted || isSupport
	divided := a.isDivided()
	choicesSpec, hasChoices := a.Params.Param("Choices")
	if divided && !hasChoices && (!targeted || hasDefined) {
		// Untargeted, the division is asked card by card as the effect
		// resolves (CountersPutEffect.java:470-483, Choices$-chosen cards);
		// with Defined$ the cards read are not the targets the division was
		// announced over.
		return fmt.Errorf("engine: PutCounter: DividedAsYouChoose$ without its own targets not resolvable yet")
	}
	var choiceShares []int
	switch {
	case hasChoices:
		// Choices$ replaces Defined$ and the targets
		// (CountersPutEffect.java:190 against :244).
		var err error
		if cards, choiceShares, err = g.putCounterChoice(controller, a, choicesSpec, counterType, amount, divided); err != nil {
			return err
		}
	case targeted && !hasDefined:
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
	default:
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
		if hasChoices && divided {
			amounts[i] = choiceShares[i]
			continue
		}
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
	// total is GameEntityCounterTable.totalValues once the AddCounter
	// replacements have edited the table in place (ReplaceCounterEffect's
	// setCount): the counters each object was to get, summed.
	total := 0
	// RememberCards$ remembers the cards that got counters (CountersPutEffect.
	// java:658-660) and RememberPut$ every entity that did, a player too
	// (GameEntityCounterTable.java:158-163).
	_, rememberCards := a.Params.Param("RememberCards")
	_, rememberPut := a.Params.Param("RememberPut")
	remember := rememberCards || rememberPut
	for i, cid := range cards {
		// CountersPutEffect.java:485-489: Adapt skips a creature that has +1/+1
		// counters (CanAdapt statics are not modelled), Monstrosity one that is
		// already monstrous.
		if isAdapt && g.Card(cid).Counters.Count(P1P1) > 0 || isMonstrosity && g.Card(cid).monstrous {
			continue
		}
		n := g.countersReplaced(controller, placer, CardEntity(cid), counterType, amounts[i])
		if n <= 0 {
			continue
		}
		total += n
		g.addCardCounters(controller, placer, a.Source, cid, counterType, n)
		if remember {
			// CountersPutEffect.java:658-660: the cards that got counters.
			source.Memory.Remember(CardEntity(cid))
		}
		if a.evolve != NoCard && counterType == P1P1 {
			// Card.java:1823: the evolve ability's +1/+1 counters made the
			// creature evolve (CR 702.100b).
			g.checkEvolvedTriggers(controller, cid)
		}
		if isMonstrosity {
			// :543-548.
			g.Card(cid).monstrous = true
			g.checkMonstrousTriggers(controller, cid)
		}
	}
	if keyword.Parse(a.Params.Keyword).Name == "Renown" {
		// CountersPutEffect.java:553: whatever the counters did.
		for _, cid := range cards {
			g.Card(cid).renowned = true
		}
	}
	for _, pid := range players {
		n := g.countersReplaced(controller, placer, PlayerEntity(pid), counterType, amount)
		if n <= 0 {
			continue
		}
		total += n
		if rememberPut {
			source.Memory.Remember(PlayerEntity(pid))
		}
		g.Player(pid).Counters.Add(counterType, n)
		emitCounterChanged(g, a.Source, PlayerEntity(pid), counterType, n)
	}
	if rememberAmount && total > 0 {
		// GameEntityCounterTable.java:181-184: the host remembers the total as
		// an Integer, once the counters are on (Etched Slith's "when you do",
		// read back by Count$RememberedNumber).
		g.Card(a.Source).Memory.RememberNumber(total)
	}
	return nil
}

// putCounterPlacer is who puts the counters (CountersPutEffect.java:583-586):
// the activator, or the first player Placer$ names. Java's `.get(0)` throws on
// an empty list; here that is an error before anything is placed (GO-7). The
// placer is what an AddCounter replacement's and a CounterAdded trigger's
// ValidSource$ read (Quicksilver Fountain, Alexios: "that player puts").
func putCounterPlacer(g *Game, a *Ability) (PlayerID, error) {
	spec, ok := a.Params.Param("Placer")
	if !ok {
		return a.Controller, nil
	}
	players, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs())
	if err != nil {
		return NoPlayer, fmt.Errorf("engine: PutCounter: Placer$: %w", err)
	}
	if len(players) == 0 {
		return NoPlayer, fmt.Errorf("engine: PutCounter: Placer$ %q names no player", spec)
	}
	return players[0], nil
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

// putCounterChoice is the Choices$ branch of CountersPutEffect.resolvePerType
// (:183-244, :470-483): Chooser$ picks MinChoiceAmount$..ChoiceAmount$ cards of
// ChoiceZone$ matching Choices$ that can receive the counters, once, before
// any counter is placed. With DividedAsYouChoose$ the activator then splits
// the amount card by card (chooseNumber 1..remaining), the last card taking
// what is left. shares is the amount each returned card gets.
func (g *Game) putCounterChoice(controller PlayerController, a *Ability, spec string, ct CounterType, amount int, divided bool) (cards []CardID, shares []int, err error) {
	chooser, err := choiceChooser(g, a, "PutCounter")
	if err != nil {
		return nil, nil, err
	}
	source := g.Card(a.Source)
	n := 1
	if raw, ok := a.Params.Param("ChoiceAmount"); ok {
		if n, ok = resolveNamedAmount(g, a.Amounts, source, raw); !ok {
			return nil, nil, fmt.Errorf("engine: PutCounter: ChoiceAmount$ %q is not resolvable", raw)
		}
	}
	lo := n
	if raw, ok := a.Params.Param("MinChoiceAmount"); ok {
		if lo, ok = resolveNamedAmount(g, a.Amounts, source, raw); !ok {
			return nil, nil, fmt.Errorf("engine: PutCounter: MinChoiceAmount$ %q is not resolvable", raw)
		}
	}
	if n <= 0 {
		return nil, nil, nil
	}
	zones, err := choiceZones(a, "PutCounter")
	if err != nil {
		return nil, nil, err
	}
	all, err := choiceCandidates(g, a, "PutCounter", zones, spec)
	if err != nil {
		return nil, nil, err
	}
	var offer []CardID
	for _, cid := range all {
		if !g.cantPutCounter(CardEntity(cid), ct) {
			offer = append(offer, cid)
		}
	}
	cards, err = askChoiceCards(g, controller, a, "PutCounter", chooser, offer, lo, n)
	if err != nil || len(cards) == 0 {
		return nil, nil, err
	}
	shares = make([]int, len(cards))
	remaining := amount
	for i := range cards {
		shares[i] = amount
		if !divided {
			continue
		}
		switch {
		case i == len(cards)-1 || remaining == 1:
			shares[i] = remaining
		case remaining <= 0:
			shares[i] = 0
		default:
			shares[i] = controller.ChooseNumber(g, a.Controller, a.Source, 1, remaining)
			if shares[i] < 1 || shares[i] > remaining {
				return nil, nil, fmt.Errorf("engine: PutCounter: division %d is not within 1..%d", shares[i], remaining)
			}
		}
		remaining -= shares[i]
	}
	return cards, shares, nil
}
