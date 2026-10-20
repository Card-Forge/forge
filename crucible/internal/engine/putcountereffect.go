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

//enginelint:allow id card game player ability event defined amount condition parts control resolvechoice attackconstraints continuouslayers effecthelpers valid zone

import (
	"fmt"
	"slices"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/valid"
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
// Monstrosity$/Adapt$/Support$/PowerUp$/Exhaust$ -- each its own
// further keyword-ability mechanic buildSpellAbility itself rewrites into
// this shape from a simpler keyword line; EachFromSource$/PutOnEachOther$/
// PutOnDefined$/ChooseDifferent$/EachExistingCounter$/UniqueType$/
// CounterTypePerDefined$/CounterNumPerDefined$/OnlyNewKind$/
// RandomType$ -- each its own further per-target
// mechanic; CounterTypes$ (17, plural) -- a different multi-type shape,
// not this one; ForColor$/SharedKeywords$/SharedKeywordsDefined$/
// SharedKeywordsZone$/SharedRestrictions$/TriggeredCounterMap$/
// CounterMapValues$/SpecifyCounter$/RememberCards$/RemovePhase$/
// Optional$/UpTo$/UpToMin$ -- each its own further mechanic. Placer$ resolves
// (putCounterPlacer).
//
// Condition$ values conditionValueRead does not name -- SpellAbilityCondition's
// own shapes subAbilityConditionMet does not cover.
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
	"DividedRandomly",
	"EachFromSource",
	"ForColor", "SharedKeywords", "SharedKeywordsDefined", "SharedKeywordsZone",
	"SharedRestrictions", "TriggeredCounterMap", "CounterMapValues", "SpecifyCounter",
	"RemovePhase",
}

// putCounterEffect resolves Mode$/DB$/AB$ PutCounter for the Defined$
// Self/Enchanted/Equipped/You shape -- no target. ConditionPresent$/
// ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$ are resolved
// through subAbilityConditionMet (condition.go) the identical way
// DealDamage's/GainLife's/Pump's own do.
type putCounterEffect struct{}

func (putCounterEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, "PutCounter"); err != nil {
		return err
	}
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
	bolster, isBolster := a.Params.Param("Bolster")
	var counterType CounterType
	var counterNum string
	switch {
	case isMonstrosity:
		counterType, counterNum = P1P1, monstrosity
	case isAdapt:
		counterType, counterNum = P1P1, adapt
	case isSupport:
		counterType, counterNum = P1P1, "1"
	case isBolster:
		counterType, counterNum = P1P1, bolster
	default:
		// CounterTypes$, ExistingCounter and the per-object type choices pick the
		// kind per target (counterKindPlan); no single kind is read here.
		if !counterKindsPerObject(a.Params) {
			var err error
			if counterType, err = g.putCounterTypeChosen(controller, a, placer); err != nil {
				return err
			}
		}
		var ok bool
		if counterNum, ok = a.Params.Param("CounterNum"); !ok {
			counterNum = "1"
		}
	}
	plan := g.counterKindPlan(controller, a, counterType)
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
	if isBolster {
		// CountersPutEffect.java:750-756: Bolster rewrites the line into a
		// Choices$ over the controller's least-toughness creatures, and
		// SkipReceiveCounters$ lifts the canReceiveCounters filter (:215).
		choicesSpec, hasChoices = "Creature.leastToughnessControlledByYou", true
	}
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
		// counters unless a Mode$ CanAdapt static lets it adapt anyway,
		// Monstrosity one that is already monstrous.
		if isAdapt && g.Card(cid).Counters.Count(P1P1) > 0 && !g.anyWithAdapt(cid) || isMonstrosity && g.Card(cid).monstrous {
			continue
		}
		kinds, err := plan(CardEntity(cid), amounts[i])
		if err != nil {
			return err
		}
		for _, k := range kinds {
			n := g.countersReplaced(controller, placer, CardEntity(cid), k.kind, k.n)
			if n <= 0 {
				continue
			}
			total += n
			g.addCardCounters(controller, placer, a.Source, cid, k.kind, n)
			if remember {
				// CountersPutEffect.java:658-660: the cards that got counters.
				source.Memory.Remember(CardEntity(cid))
			}
			if a.evolve != NoCard && k.kind == P1P1 {
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
	}
	if keyword.Parse(a.Params.Keyword).Name == "Renown" {
		// CountersPutEffect.java:553: whatever the counters did.
		for _, cid := range cards {
			g.Card(cid).renowned = true
		}
	}
	for _, pid := range players {
		kinds, err := plan(PlayerEntity(pid), amount)
		if err != nil {
			return err
		}
		for _, k := range kinds {
			n := g.countersReplaced(controller, placer, PlayerEntity(pid), k.kind, k.n)
			if n <= 0 {
				continue
			}
			total += n
			if rememberPut {
				source.Memory.Remember(PlayerEntity(pid))
			}
			g.Player(pid).Counters.Add(k.kind, n)
			emitCounterChanged(g, a.Source, PlayerEntity(pid), k.kind, n)
		}
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
		// "Valid <spec>" and "Valid<Zone> <spec>" scan the game
		// (definedCardsInGame); every other form is definedCards'.
		cards, err := definedCardsInGame(g, controller, host, defined, refs)
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
	if hasParam(a, "ChoiceOptional") {
		// chooseCardsForEffect(..., optional, ...) (CountersPutEffect.java:241):
		// the chooser may pick none.
		lo = 0
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
	// SkipReceiveCounters$ lifts the canReceiveCounters filter
	// (CountersPutEffect.java:215), which Bolster's rewrite always sets (:756).
	_, skipReceive := a.Params.Param("SkipReceiveCounters")
	_, bolster := a.Params.Param("Bolster")
	var offer []CardID
	for _, cid := range all {
		if skipReceive || bolster || !g.cantPutCounter(CardEntity(cid), ct) {
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

// counterKindsPerObject reports whether the line chooses the counter kind for
// each object it puts counters on: CounterTypes$, CounterType$ ExistingCounter,
// and a CounterType$ list under CounterTypePerDefined$, UniqueType$ or
// ChooseDifferent$.
func counterKindsPerObject(a *compile.Ability) bool {
	if hasParamOn(a, "CounterTypes") || hasParamOn(a, "CounterTypePerDefined") || hasParamOn(a, "UniqueType") ||
		hasParamOn(a, "ChooseDifferent") {
		return true
	}
	raw, _ := a.Param("CounterType")
	return raw == "ExistingCounter"
}

// counterKind is one kind of counter an object is to get, and how many.
type counterKind struct {
	kind CounterType
	n    int
}

// counterKindPlan is CountersPutEffect.resolvePerType's per-object choice of
// kinds (:300-470): given the entity and the amount it is to get, the counters
// to place. Without a per-object shape it is the one counterType, amount times.
//
//   - CounterTypes$ lists the kinds, each getting the amount; ChosenFromList
//     names one the activator picks from TypeList$; SplitAmount$ lets them split
//     the amount (all but the last choose a number, the last takes the rest). An
//     EachType_ entry reads the counters in play and is not resolved.
//   - CounterType$ ExistingCounter picks among the kinds the object already has:
//     every one under EachExistingCounter$, the only one, or the activator's
//     choice. PutOnEachOther$ puts the picked kind on every other card its valid
//     string names, PutOnDefined$ on the cards Defined names (OnlyNewKind$:
//     those without it), and the object itself gets nothing.
//   - CounterTypePerDefined$/UniqueType$ choose a kind from the CounterType$
//     list for each object (UniqueType$: only one it has none of, RandomType$:
//     at random), CounterNumPerDefined$ reading the amount off the target card.
//
// UpTo$ asks the activator for the amount of a card (UpToMin$ down to).
func (g *Game) counterKindPlan(controller PlayerController, a *Ability, counterType CounterType) func(target EntityID, amount int) ([]counterKind, error) {
	source := g.Card(a.Source)
	placer, _ := putCounterPlacer(g, a)
	listChoice := func(list string, target EntityID) (CounterType, bool) {
		var choices []CounterType
		for _, s := range strings.Split(list, ",") {
			if s == "" {
				continue
			}
			ct := CounterType(strings.ToUpper(s))
			if hasParam(a, "UniqueType") && g.entityCounters(target).Count(ct) != 0 {
				continue
			}
			if !slices.Contains(choices, ct) {
				choices = append(choices, ct)
			}
		}
		if len(choices) == 0 {
			return "", false
		}
		if hasParam(a, "RandomType") {
			return choices[g.rand.Int32n(int32(len(choices)))], true
		}
		names := make([]string, len(choices))
		for i, c := range choices {
			names[i] = string(c)
		}
		pick := controller.ChooseOption(g, a.Controller, a.Source, names)
		if pick < 0 || pick >= len(choices) {
			pick = 0
		}
		return choices[pick], true
	}
	return func(target EntityID, amount int) ([]counterKind, error) {
		card, isCard := target.AsCard()
		if raw, ok := a.Params.Param("CounterNumPerDefined"); ok && isCard {
			n, ok := resolveNamedAmount(g, a.Amounts, g.Card(card), raw)
			if !ok {
				return nil, fmt.Errorf("engine: PutCounter: CounterNumPerDefined$ %q is not resolvable", raw)
			}
			amount = n
		}
		if hasParam(a, "UpTo") && isCard {
			lo := 0
			if raw, ok := a.Params.Param("UpToMin"); ok {
				if lo, ok = resolveNamedAmount(g, a.Amounts, source, raw); !ok {
					return nil, fmt.Errorf("engine: PutCounter: UpToMin$ %q is not resolvable", raw)
				}
			}
			if amount > lo {
				picked := controller.ChooseNumber(g, a.Controller, a.Source, lo, amount)
				if picked < lo || picked > amount {
					return nil, fmt.Errorf("engine: PutCounter: %d is not within %d..%d", picked, lo, amount)
				}
				amount = picked
			}
		}
		switch {
		case hasParam(a, "ChooseDifferent"):
			kinds, err := g.chooseDifferentKinds(controller, a, placer, amount)
			return kinds, err
		case hasParam(a, "CounterTypes"):
			list, _ := a.Params.Param("CounterTypes")
			var kinds []CounterType
			if strings.Contains(list, "ChosenFromList") {
				typeList, _ := a.Params.Param("TypeList")
				if ct, ok := listChoice(typeList, target); ok {
					kinds = append(kinds, ct)
				}
				list = strings.ReplaceAll(list, "ChosenFromList", "")
			}
			for _, word := range strings.Split(list, ",") {
				if word == "" {
					continue
				}
				if strings.Contains(word, "EachType") {
					return nil, fmt.Errorf("engine: PutCounter: CounterTypes$ %q not resolvable yet", word)
				}
				kinds = append(kinds, CounterType(strings.ToUpper(word)))
			}
			remaining := amount
			out := make([]counterKind, 0, len(kinds))
			for i, ct := range kinds {
				n := amount
				if hasParam(a, "SplitAmount") {
					if len(kinds)-i > 1 {
						n = controller.ChooseNumber(g, a.Controller, a.Source, 0, remaining)
						if n < 0 || n > remaining {
							return nil, fmt.Errorf("engine: PutCounter: %d is not within 0..%d", n, remaining)
						}
						if n == 0 {
							continue
						}
						remaining -= n
					} else {
						n = remaining
					}
				}
				out = append(out, counterKind{ct, n})
			}
			return out, nil
		case counterKindsPerObject(a.Params) && !hasParam(a, "CounterTypePerDefined") && !hasParam(a, "UniqueType"):
			// CounterType$ ExistingCounter.
			var choices []CounterType
			for _, ct := range g.entityCounters(target).Kinds() {
				if g.entityCounters(target).Count(ct) > 0 && (!g.cantPutCounter(target, ct) || hasParam(a, "PutOnEachOther")) {
					choices = append(choices, ct)
				}
			}
			if hasParam(a, "EachExistingCounter") {
				out := make([]counterKind, len(choices))
				for i, ct := range choices {
					out[i] = counterKind{ct, amount}
				}
				return out, nil
			}
			var chosen CounterType
			switch len(choices) {
			case 0:
				return nil, nil
			case 1:
				chosen = choices[0]
			default:
				names := make([]string, len(choices))
				for i, c := range choices {
					names[i] = string(c)
				}
				pick := controller.ChooseOption(g, a.Controller, a.Source, names)
				if pick < 0 || pick >= len(choices) {
					pick = 0
				}
				chosen = choices[pick]
			}
			if spec, ok := a.Params.Param("PutOnEachOther"); ok {
				parsed := valid.Parse(spec)
				for _, pid := range g.Players() {
					for _, other := range append([]CardID(nil), g.Zone(Battlefield, pid).Cards()...) {
						if CardEntity(other) == target || !Matches(g, g.Card(other), parsed, a.Controller, a.Source) {
							continue
						}
						g.putOneKind(controller, placer, a, other, chosen, amount)
					}
				}
				return nil, nil
			}
			if spec, ok := a.Params.Param("PutOnDefined"); ok {
				defs, err := definedCards(source, spec, a.refs())
				if err != nil {
					return nil, fmt.Errorf("engine: PutCounter: PutOnDefined$: %w", err)
				}
				for _, other := range defs {
					if !hasParam(a, "OnlyNewKind") || g.Card(other).Counters.Count(chosen) < 1 {
						g.putOneKind(controller, placer, a, other, chosen, amount)
					}
				}
				return nil, nil
			}
			return []counterKind{{chosen, amount}}, nil
		case hasParam(a, "CounterTypePerDefined") || hasParam(a, "UniqueType"):
			list, _ := a.Params.Param("CounterType")
			ct, ok := listChoice(list, target)
			if !ok {
				return nil, nil
			}
			return []counterKind{{ct, amount}}, nil
		}
		return []counterKind{{counterType, amount}}, nil
	}
}

// putOneKind puts amount counters of kind on card through the AddCounter
// replacements, as the per-object loop does for its own targets.
func (g *Game) putOneKind(controller PlayerController, placer PlayerID, a *Ability, card CardID, kind CounterType, amount int) {
	if n := g.countersReplaced(controller, placer, CardEntity(card), kind, amount); n > 0 {
		g.addCardCounters(controller, placer, a.Source, card, kind, n)
	}
}

// entityCounters is the counter table of a card or a player.
func (g *Game) entityCounters(e EntityID) *Counters {
	if id, ok := e.AsCard(); ok {
		return &g.Card(id).Counters
	}
	pid, _ := e.AsPlayer()
	return &g.Player(pid).Counters
}

// anyWithAdapt is StaticAbilityAdapt.anyWithAdapt: a Mode$ CanAdapt static in
// play whose ValidCard$ names card lets it adapt as though it had no +1/+1
// counters. A ValidSA$ on the static is not evaluated and reads as no match.
func (g *Game) anyWithAdapt(card CardID) bool {
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			for _, face := range h.traitFaces() {
				for _, s := range face.Statics {
					if strings.EqualFold(s.Name, "CanAdapt") && !hasParamOn(s, "ValidSA") &&
						g.staticConditionsMet(h, s) && staticValidMatches(g, h, s, "ValidCard", card) {
						return true
					}
				}
			}
		}
	}
	return false
}

// counterTypeList is a CounterType$ comma list as chooseTypeFromList reads it:
// each kind once, in order, empty entries dropped.
func counterTypeList(raw string) []CounterType {
	var out []CounterType
	for _, s := range strings.Split(raw, ",") {
		ct := CounterType(strings.ToUpper(s))
		if s != "" && !slices.Contains(out, ct) {
			out = append(out, ct)
		}
	}
	return out
}

// chooseCounterKind is the PlayerController.chooseCounterType half of
// CountersPutEffect.chooseTypeFromList (:690-716): decider picks one of
// options (the only one when there is just one).
func (g *Game) chooseCounterKind(controller PlayerController, decider PlayerID, source CardID, options []CounterType) CounterType {
	if len(options) == 1 {
		return options[0]
	}
	names := make([]string, len(options))
	for i, c := range options {
		names[i] = string(c)
	}
	pick := controller.ChooseOption(g, decider, source, names)
	if pick < 0 || pick >= len(options) {
		pick = 0
	}
	return options[pick]
}

// putCounterTypeChosen is CounterType$ for a line with one kind for all its
// objects: a literal name, or a comma list the placer picks from once, before
// any counter is put (CountersPutEffect.java:620-629). ExistingCounter and
// Any are not literal kinds and stay refused.
func (g *Game) putCounterTypeChosen(controller PlayerController, a *Ability, placer PlayerID) (CounterType, error) {
	raw, _ := a.Params.Param("CounterType")
	if !strings.Contains(raw, ",") {
		return putCounterType(a.Params)
	}
	options := counterTypeList(raw)
	if len(options) == 0 {
		return "", fmt.Errorf("engine: PutCounter: CounterType$ %q names no kind", raw)
	}
	return g.chooseCounterKind(controller, placer, a.Source, options), nil
}

// chooseDifferentKinds is ChooseDifferent$ N (:292-314): the placer picks N
// kinds of the CounterType$ list, one after another with each pick struck from
// the options, and every kind gets the amount.
func (g *Game) chooseDifferentKinds(controller PlayerController, a *Ability, placer PlayerID, amount int) ([]counterKind, error) {
	rawN, _ := a.Params.Param("ChooseDifferent")
	n, err := strconv.Atoi(strings.TrimSpace(rawN))
	if err != nil {
		return nil, fmt.Errorf("engine: PutCounter: ChooseDifferent$ %q is not a number", rawN)
	}
	list, _ := a.Params.Param("CounterType")
	options := counterTypeList(list)
	var out []counterKind
	for i := 0; i < n && len(options) > 0; i++ {
		ct := g.chooseCounterKind(controller, placer, a.Source, options)
		out = append(out, counterKind{ct, amount})
		options = slices.DeleteFunc(options, func(o CounterType) bool { return o == ct })
	}
	return out, nil
}
