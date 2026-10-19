// Choosing targets: CR 601.2c (a spell) / 603.3b (a triggered ability) --
// "choices... including targets... are made" the moment the ability is put
// on the stack. This port has exactly one place that happens today,
// pushTriggeredAbilities (trigger.go), so resolveTargets is called from
// there rather than from a separate step of its own; a future cast path for
// an Instant/Sorcery naming its own top-level ValidTgts$ (not built --
// CastSpell, castspell.go, only casts a permanent or an Aura today) would
// call it from wherever that lands too.

package engine

import (
	"fmt"
	"slices"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// Radiance$ ("and each other permanent that shares a color with it") is not
// a targeting shape: the extra permanents are never announced, so choosing
// targets treats a Radiance$ line like any other ValidTgts$ line. The
// derived set is computed when the effect resolves (radianceCards, below).
// TargetsForEachPlayer$ (one target per player) reads as
// TargetsWithDifferentControllers$ plus a fizzle-time check that no target
// changed controller (trimTargetSet, recordForEachControllers).
// TargetUnique$ excludes what the ancestors of a SubAbility$ link targeted
// (SpellAbility.getUniqueTargets, SpellAbility.java:2034-2044): the link
// resolveChainTargets asks about carries that list (Ability.uniqueTargets).
// A root or Charm mode has no ancestors, so there it excludes nothing.

// resolveTargets is CR 601.2c/603.3b's own "choose targets," and reports
// whether a is still eligible to be pushed onto the stack at all. true with
// a.Targets left nil means there was nothing to target in the first place --
// no ValidTgts$ named, or TargetMin$ 0 with no candidate (asked nothing).
// false means CR 603.3c's own real rule ("if the
// ability requires a target and there are no legal targets, it doesn't go on
// the stack"). A target shape this port cannot offer (a TargetType$ other
// than Spell, an unresolvable TargetMin$/TargetMax$, a Defined$ it cannot
// read) is not that: the ability is pushed with no targets and fails when it
// resolves (Ability.targetsErr), so the refusal is seen (GO-7).
//
// TargetMin$/TargetMax$ (1/1 when neither is named, TargetRestrictions.
// java's own getOrDefault) resolve through resolveNamedAmount exactly as
// every other numeric param already does. targetCandidates (below) always
// evaluates ValidTgts$ against both players and cards and unions whatever
// matches -- a real corpus spec regularly names both ("Any", CR 115's own
// "any target"; `Player,Planeswalker`, written out explicitly, 273 real
// lines corpus-wide), so there is no single "shape" to settle up front the
// way an earlier version of this function tried to.
func (g *Game) resolveTargets(controller PlayerController, a *Ability) bool {
	choice, named, ok := g.targetChoiceFor(a)
	if !named {
		return true
	}
	if !ok {
		return false
	}
	if choice.err != nil {
		// Pushed with no targets, failing when it resolves: pushing has no
		// error path of its own (GO-7), the same deferral Ability.modesErr
		// makes for a Charm.
		a.targetsErr = choice.err
		return true
	}
	// TargetingPlayer$ names who picks the targets (SpellAbility.setupTargets);
	// targetChoiceFor resolved it, and one it could not resolve is choice.err.
	decider := choice.targeter
	a.targeter = decider
	if len(choice.candidates) == 0 || choice.max <= 0 && choice.min <= 0 {
		// TargetMin$ 0 and no candidate, or no target allowed at all (a
		// DividedAsYouChoose$ total of 0, capDividedChoice): nothing to ask
		// (TargetSelection.java:84, 117-120).
		return true
	}
	chosen, err := g.chooseTargets(controller, a, choice)
	if err != nil {
		a.targetsErr = err
		return true
	}
	a.Targets = g.trimTargetSet(a, chosen)
	a.recordForEachControllers(g)
	if cards := len(a.Targets); cards < len(chosen) && cards < choice.min {
		// The answer broke a pairwise restriction and fewer than the minimum
		// survive: no legal set was chosen, CR 603.3c.
		a.Targets = nil
		return false
	}
	// CR 601.2d: the division is announced right after the targets.
	divided, err := g.divideAmongTargets(controller, decider, a, a.Targets)
	if err != nil {
		a.targetsErr = err
		return true
	}
	a.divided = divided
	return true
}

// chooseTargets is TargetSelection.chooseTargets' choice among choice's
// candidates for a, whose targets are being chosen for the first time
// (numTargets == null, TargetSelection.java:123): with TargetsAtRandom$
// (TargetRestrictions.java:202-204) the game picks them at random
// (randomTargets) and nobody is asked; otherwise choice.targeter is asked.
func (g *Game) chooseTargets(controller PlayerController, a *Ability, choice targetChoice) ([]EntityID, error) {
	if _, random := a.Params.Param("TargetsAtRandom"); random {
		return g.randomTargets(a, choice)
	}
	return controller.ChooseTargets(g, choice.targeter, choice.candidates, choice.min, choice.max), nil
}

// randomTargets is TargetSelection.java:123-137: num targets drawn one at a
// time from the candidates in getAllCandidates' order, each removed once
// drawn (Aggregates.random over a List: no draw for a single candidate,
// randomIndex). num is TargetMin$, or with RandomNumTargets$
// (TargetRestrictions.java:205-207) Aggregates.randomInt between TargetMin$
// (at least 1) and the lesser of TargetMax$ and the candidate count. A
// random count whose lower bound exceeds its upper one makes Java's nextInt
// throw; here it is an error. Java's AI picks such targets its own way per
// API (DamageDealAi.java:466, ControlGainAi.java:96); this is the rules'
// "chosen at random", the same for every controller.
func (g *Game) randomTargets(a *Ability, choice targetChoice) ([]EntityID, error) {
	candidates := append([]EntityID(nil), choice.candidates...)
	num := choice.min
	if _, randomNum := a.Params.Param("RandomNumTargets"); randomNum {
		top, bot := min(len(candidates), choice.max), max(choice.min, 1)
		if top < bot {
			return nil, fmt.Errorf("RandomNumTargets$ between %d and %d not resolvable", bot, top)
		}
		num = g.randomInt(bot, top)
	}
	var out []EntityID
	for range num {
		i := g.randomIndex(len(candidates))
		if i < 0 {
			break
		}
		out = append(out, candidates[i])
		candidates = slices.Delete(candidates, i, i+1)
	}
	return out, nil
}

// inAbilityContext makes a the ability in context (Game.resolving) until the
// returned func restores the previous one. Java hands the SpellAbility being
// put on the stack to every amount and valid string read while its targets are
// chosen (TargetSelection, SpellAbility.getMaxTargets/getMinTargets and
// dividedValue all pass sa to AbilityUtils.calculateAmount), so its triggering
// objects, paid lists and targets so far are readable then, not only while it
// resolves: Case of the Market Melee's TargetMax$ TriggerObjectsAttackers$Amount.
// A SubAbility$ link is its own context, as Java's sa is the link.
func (g *Game) inAbilityContext(a *Ability) func() {
	prev := g.resolving
	g.resolving = a
	return func() { g.resolving = prev }
}

// hasSameControllerRestriction reports whether a names
// TargetsWithSameController$ (TargetRestrictions.java:184-185). Two halves
// of SpellAbility.canTarget's pairwise check are ported: the candidate
// pre-filter (withSameControllerPartner) and the fizzle-time check
// (targetStillLegal). The single ChooseTargets answer is not re-validated,
// the same trust every other ChooseTargets answer gets; a set spanning two
// controllers makes every card target illegal at resolution, so it fizzles.
func hasSameControllerRestriction(a *Ability) bool {
	if a.Params == nil {
		return false
	}
	_, ok := a.Params.Param("TargetsWithSameController")
	return ok
}

// trimTargetSet applies the pairwise restrictions SpellAbility.canTarget
// asks of each further target (SpellAbility.java:1486-1534) to the answer a
// controller gave, in order: a target that breaks one against the targets
// already kept is dropped. Java asks per candidate as targets are picked;
// this port asks the controller once (ChooseTargets), so the same rule runs
// over the whole answer. Ported: TargetsWithDifferentControllers,
// TargetsWithDifferentCMC, TargetsWithDifferentNames,
// TargetsWithEqualToughness, TargetsWithSameCardType and
// MaxTotalTargetCMC/MaxTotalTargetPower, TargetsWithSameCreatureType and
// TargetsWithoutSameCreatureType (sharesCreatureType), and
// TargetsForEachPlayer, which asks what TargetsWithDifferentControllers does.
func (g *Game) trimTargetSet(a *Ability, chosen []EntityID) []EntityID {
	p := a.Params
	if p == nil {
		return chosen
	}
	defer g.inAbilityContext(a)()
	has := func(key string) bool { _, ok := p.Param(key); return ok }
	diffCtl, diffCMC, diffNames := has("TargetsWithDifferentControllers") || has("TargetsForEachPlayer"), has("TargetsWithDifferentCMC"), has("TargetsWithDifferentNames")
	eqTough, sameType := has("TargetsWithEqualToughness"), has("TargetsWithSameCardType")
	sameCreature, noCreature := has("TargetsWithSameCreatureType"), has("TargetsWithoutSameCreatureType")
	host := g.Card(a.Source)
	limit := func(key string) (int, bool) {
		raw, ok := p.Param(key)
		if !ok {
			return 0, false
		}
		return resolveNamedAmount(g, a.Amounts, host, raw)
	}
	maxCMC, hasMaxCMC := limit("MaxTotalTargetCMC")
	maxPower, hasMaxPower := limit("MaxTotalTargetPower")
	if !diffCtl && !diffCMC && !diffNames && !eqTough && !sameType && !sameCreature && !noCreature && !hasMaxCMC && !hasMaxPower {
		return chosen
	}
	var kept []EntityID
	var keptCards []*Card
	cmcSum, powerSum := 0, 0
	for _, e := range chosen {
		id, isCard := e.AsCard()
		if !isCard {
			kept = append(kept, e)
			continue
		}
		c := g.Card(id)
		legal := true
		for _, o := range keptCards {
			switch {
			case diffCtl && o.Controller() == c.Controller(),
				diffCMC && o.CMC() == c.CMC(),
				diffNames && o.Def != nil && c.Def != nil && o.Name() == c.Name(),
				eqTough && !sameToughness(o, c),
				sameType && !sharesCardType(o, c),
				sameCreature && !g.sharesCreatureType(o, c),
				noCreature && g.sharesCreatureType(o, c):
				legal = false
			}
		}
		pw, _ := c.Power()
		if hasMaxCMC && cmcSum+c.CMC() > maxCMC || hasMaxPower && powerSum+pw > maxPower {
			legal = false
		}
		if !legal {
			continue
		}
		kept = append(kept, e)
		keptCards = append(keptCards, c)
		cmcSum += c.CMC()
		powerSum += pw
	}
	return kept
}

// sharesCardType is Card.sharesCardTypeWith: a core type in common.
func sharesCardType(a, b *Card) bool {
	for _, t := range a.Type().CoreTypes() {
		if b.Type().Has(t) {
			return true
		}
	}
	return false
}

// sharesCreatureType is Card.sharesCreatureTypeWith (CardType.java:704-735):
// both are creatures or Kindred and have a creature type in common. A card with
// Changeling carries every creature type already (applyChangelings), which is
// what Java's "all creature types" flag stands for.
func (g *Game) sharesCreatureType(a, b *Card) bool {
	reg := g.db.Types()
	if reg == nil {
		return false
	}
	mine := a.Type().CreatureTypes(reg)
	theirs := b.Type().CreatureTypes(reg)
	for _, t := range mine {
		if slices.Contains(theirs, t) {
			return true
		}
	}
	return false
}

// targetingPlayerOf is who chooses a's targets (SpellAbility.setupTargets): the
// controller, or the one player TargetingPlayer$ names. A spec naming no player
// or several is an error, the activator's pick among them being a decision this
// port does not ask (GO-7).
func (g *Game) targetingPlayerOf(a *Ability) (PlayerID, error) {
	spec, ok := a.Params.Param("TargetingPlayer")
	if !ok {
		return a.Controller, nil
	}
	players, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs())
	if err != nil {
		return NoPlayer, fmt.Errorf("engine: TargetingPlayer$: %w", err)
	}
	if len(players) != 1 {
		return NoPlayer, fmt.Errorf("engine: TargetingPlayer$ %q names %d players, not resolvable yet", spec, len(players))
	}
	return players[0], nil
}

// recordForEachControllers keeps each chosen card's controller under
// TargetsForEachPlayer$ (TargetChoices.add's cardControllers), for the
// fizzle-time check targetStillLegal makes.
func (a *Ability) recordForEachControllers(g *Game) {
	if a.Params == nil {
		return
	}
	if _, ok := a.Params.Param("TargetsForEachPlayer"); !ok {
		return
	}
	a.forEachControllers = map[CardID]PlayerID{}
	for _, e := range a.Targets {
		if id, ok := e.AsCard(); ok {
			a.forEachControllers[id] = g.Card(id).Controller()
		}
	}
}

// candidateRestrictionsMet is the per-card half of SpellAbility.canTarget
// (SpellAbility.java:1418-1481) the other candidate filters leave out:
//   - TargetsWithSharedCardType$ names cards (TargetsWithSharedTypes$ narrowing
//     the types) the target must share a card type with;
//   - TargetsWithControllerProperty$ cmcLECardsInGraveyard / powerLECardsInGraveyard
//     compares the target to its own controller's graveyard;
//   - TargetsWithRelatedProperty$ LEPower / LECMC compares it to the first card
//     an ancestor ability targeted (none: nothing qualifies);
//   - TargetingPlayerControls$ needs the targeting player to control it.
//
// err is a shape this port cannot read, which makes the ability untargetable
// rather than guessed at.
func (g *Game) candidateRestrictionsMet(a *Ability, c *Card, targeter PlayerID) (bool, error) {
	p := a.Params
	if def, ok := p.Param("TargetsWithSharedCardType"); ok {
		cards, err := definedCards(g.Card(a.Source), def, a.refs())
		if err != nil {
			return false, err
		}
		for _, id := range cards {
			other := g.Card(id)
			if types, ok := p.Param("TargetsWithSharedTypes"); ok {
				shared := false
				for _, t := range strings.Split(types, ",") {
					if c.Type().HasStringType(t) && other.Type().HasStringType(t) {
						shared = true
						break
					}
				}
				if !shared {
					return false, nil
				}
			} else if !sharesCardType(c, other) {
				return false, nil
			}
		}
	}
	if prop, ok := p.Param("TargetsWithControllerProperty"); ok {
		graveyard := g.Zone(Graveyard, c.Controller()).Len()
		pw, _ := c.Power()
		switch {
		case prop == "cmcLECardsInGraveyard" && c.CMC() > graveyard,
			prop == "powerLECardsInGraveyard" && pw > graveyard:
			return false, nil
		}
	}
	if related, ok := p.Param("TargetsWithRelatedProperty"); ok {
		var parent *Card
		for _, e := range a.refs().parentTargets {
			if id, isCard := e.AsCard(); isCard {
				parent = g.Card(id)
				break
			}
		}
		if parent == nil {
			return false, nil
		}
		pw, _ := c.Power()
		ppw, _ := parent.Power()
		switch {
		case related == "LEPower" && pw > ppw, related == "LECMC" && c.CMC() > parent.CMC():
			return false, nil
		}
	}
	if _, ok := p.Param("TargetingPlayerControls"); ok && c.Controller() != targeter {
		return false, nil
	}
	return true, nil
}

func sameToughness(a, b *Card) bool {
	ta, _ := a.Toughness()
	tb, _ := b.Toughness()
	return ta == tb
}

// withSameControllerPartner is CardLists.getTargetableCards' own
// TargetsWithSameController$ pre-filter (CardLists.java:201-217): before
// any target is chosen, for an ability needing at least two, a card whose
// controller controls no other candidate card cannot be part of a legal
// set and is not offered. Player candidates pass through untouched.
func (g *Game) withSameControllerPartner(candidates []EntityID) []EntityID {
	perController := map[PlayerID]int{}
	for _, e := range candidates {
		if id, ok := e.AsCard(); ok {
			perController[g.Card(id).Controller()]++
		}
	}
	out := make([]EntityID, 0, len(candidates))
	for _, e := range candidates {
		if id, ok := e.AsCard(); ok && perController[g.Card(id).Controller()] < 2 {
			continue
		}
		out = append(out, e)
	}
	return out
}

// targetChoice is one targeting part's CR 601.2c question: the legal
// candidates and how many of them to choose. err is set, with no
// candidates, when the legal set holds something this port cannot offer as
// a target (stackAbilityCandidates): the question cannot be asked
// faithfully, and narrowing it silently would be a wrong guess.
type targetChoice struct {
	candidates []EntityID
	min, max   int
	err        error
	// targeter is who chooses (targetingPlayerOf), resolved once while the
	// candidates were filtered; NoPlayer alongside err.
	targeter PlayerID
}

// targetChoiceFor builds a's targetChoice from its ValidTgts$/TargetType$/
// TargetMin$/TargetMax$, the scan resolveTargets asks the controller over
// and a copy's new targets are chosen from (CR 707.10c, copySpell,
// copyspellabilityeffect.go). named is false when a names no ValidTgts$ at
// all; ok is false when it does but has no legal candidate while TargetMin$
// is above 0. A shape this port cannot offer is ok with choice.err set.
// TargetMin$ 0 with no candidate is ok with an empty pool.
func (g *Game) targetChoiceFor(a *Ability) (choice targetChoice, named, ok bool) {
	defer g.inAbilityContext(a)()
	validTgts, named := a.Params.Param("ValidTgts")
	if !named && a.API == APIEarthbend {
		// EarthbendEffect.buildSpellAbility sets the target restriction
		// itself; no script line names it.
		validTgts, named = "Land.YouCtrl", true
	}
	support, isSupport := a.Params.Param("Support")
	isSupport = isSupport && !named && a.API == APIPutCounter
	if isSupport {
		// CountersPutEffect.buildSpellAbility (:779-800): up to N target
		// creatures, other than the host when it is a permanent.
		validTgts, named = "Creature", true
		if g.Card(a.Source).Type().IsPermanent() {
			validTgts = "Creature.Other"
		}
	}
	if !named {
		return targetChoice{}, false, true
	}
	source := g.Card(a.Source)
	minStr, ok := a.Params.Param("TargetMin")
	if !ok {
		minStr = "1"
	}
	maxStr, ok := a.Params.Param("TargetMax")
	if !ok {
		maxStr = "1"
	}
	if isSupport {
		minStr, maxStr = "0", support
	}
	// An amount this port cannot resolve is not "no legal target": the ability
	// is pushed and fails when it resolves, naming the param (GO-7). Java
	// always has an answer here (calculateAmount).
	targetMin, ok := resolveNamedAmount(g, a.Amounts, source, minStr)
	if !ok {
		return targetChoice{err: fmt.Errorf("TargetMin$ %q not resolvable yet", minStr)}, true, true
	}
	targetMax, ok := resolveNamedAmount(g, a.Amounts, source, maxStr)
	if !ok {
		return targetChoice{min: targetMin, err: fmt.Errorf("TargetMax$ %q not resolvable yet", maxStr)}, true, true
	}

	var candidates []EntityID
	targetType, hasTargetType := a.Params.Param("TargetType")
	switch {
	case hasTargetType && (a.API == APIChangeTargets || targetType != "Spell"):
		// TargetType$ read as SpellAbility.isValid's full restriction over
		// every stack item (stackAbilityCandidates): ChangeTargets (23 of its
		// 44 corpus lines name a shape past "Spell"), and any API naming more
		// than the literal Spell (Stifle's Activated,Triggered). A spell is
		// offered as its card, an activated or triggered ability as its stack
		// item (ADR-0041); a stack holding no such item asks the plain
		// question (no legal target: CR 603.3c), as Java does.
		var err error
		candidates, err = g.stackAbilityCandidates(a)
		if err != nil {
			return targetChoice{min: targetMin, max: targetMax, err: err}, true, true
		}
	case hasTargetType:
		// TargetType$ Spell names spells on the stack (CR 115.1a); a spell
		// here is a card in the Stack zone.
		candidates = g.stackSpellCandidates(a.Controller, a.Source, validTgts)
	case a.API == APICopySpellAbility:
		// CopySpellAbilityEffect.buildSpellAbility sets the target zone to
		// the stack whether or not TargetType$ is named
		// (CopySpellAbilityEffect.java:28-33): Mischievous Quanar's
		// ValidTgts$ Instant,Sorcery names spells, not battlefield cards.
		candidates = g.stackSpellCandidates(a.Controller, a.Source, validTgts)
	default:
		zones := []ZoneType{Battlefield}
		if a.API == APIPlay {
			// Scoped to Play alone (Pass the Torch's own TrigPlay, 27 real
			// corpus DB$ Play | TgtZone$ Graveyard lines): every other API
			// reaching this branch (ChangeZone, Pump, Effect, PutCounter,
			// MakeCard, Clone, RepeatEach, ...) has its own Resolve written
			// against a battlefield target only, unaudited against a
			// graveyard/exile one -- widening TgtZone$ reading to all of
			// them here would make roughly 190 more corpus lines newly
			// reachable with no review of what each Resolve does with a
			// non-battlefield target (GO-7). charmeffect.go's own
			// canOfferMode also stays correct only because of this scoping:
			// it probes with targetCandidates (battlefield-only) to decide
			// whether a Charm mode not named Play (e.g. Trystan's Command's
			// ChangeZone mode) can be offered at all, and that probe must
			// keep agreeing with what actually resolves the mode's targets.
			if raw, ok := a.Params.Param("TgtZone"); ok {
				parsed, err := parseZoneList(raw)
				if err != nil {
					return targetChoice{min: targetMin, max: targetMax, err: fmt.Errorf("TgtZone$: %w", err)}, true, true
				}
				if zoneIn(Stack, parsed) {
					// No real Play line names the stack, and Play's Resolve
					// is unaudited against a spell target: fail closed
					// rather than guess. (ChangeZone's Origin$ Stack is
					// scanned below, zoneListCandidates.)
					return targetChoice{min: targetMin, max: targetMax,
						err: fmt.Errorf("TgtZone$ %q: Stack not resolvable through this path yet", raw)}, true, true
				}
				zones = parsed
			}
		}
		if a.API == APIChangeZone || a.API == APIChangeZoneAll {
			// AbilityFactory.adjustChangeZoneTarget (ChangeZoneEffect.java:47,
			// buildSpellAbility): a ChangeZone that does not target players
			// targets cards in its Origin$ zones, not the battlefield by default
			// (Raise Dead: Origin$ Graveyard | ValidTgts$ Creature.YouCtrl).
			if raw, ok := a.Params.Param("Origin"); ok && !specCanTargetPlayer(validTgts) {
				parsed, err := parseZoneList(raw)
				if err != nil {
					return targetChoice{min: targetMin, max: targetMax, err: fmt.Errorf("ChangeZone Origin$: %w", err)}, true, true
				}
				for _, z := range parsed {
					// Stack for ChangeZone alone: changeZoneKnown removes a
					// targeted spell from the stack (ChangeZoneEffect.java:
					// 577-579); ChangeZoneAll's Resolve does not.
					if z != Battlefield && z != Graveyard && z != Exile && (z != Stack || a.API != APIChangeZone) {
						return targetChoice{min: targetMin, max: targetMax,
							err: fmt.Errorf("ChangeZone Origin$ %q: targeting a card in %v not resolvable yet", raw, z)}, true, true
					}
				}
				zones = parsed
			}
		}
		if zoneIn(Stack, zones) {
			// A ChangeZone over Origin$ Stack (Unsubstantiate, Venser): a spell
			// is targeted as its card on the stack (changeZoneKnown removes the
			// spell), the zones scanned in Origin$ order (Game.getCardsIn).
			candidates = g.zoneListCandidates(a, validTgts, zones)
		} else {
			candidates = g.targetCandidatesInZones(a.Controller, a.Source, validTgts, zones, askOf(a))
		}
	}
	if def, ok := a.Params.Param("TargetsWithDefinedController"); ok {
		// SpellAbility.canTarget (SpellAbility.java:1410-1416): a card
		// target's controller must be one of the players Defined names.
		players, err := definedPlayers(g, a.Controller, a.Source, def, a.refs())
		if err != nil {
			// Java has no failure here (getDefinedPlayers answers a list); an
			// answer this port cannot give fails the ability when it resolves
			// rather than leaving it off the stack unseen (GO-7).
			return targetChoice{min: targetMin, max: targetMax, err: fmt.Errorf("TargetsWithDefinedController$: %w", err)}, true, true
		}
		kept := candidates[:0:0]
		for _, e := range candidates {
			if id, isCard := e.AsCard(); isCard && !slices.Contains(players, g.Card(id).Controller()) {
				continue
			}
			kept = append(kept, e)
		}
		candidates = kept
	}
	if _, ok := a.Params.Param("TargetUnique"); ok && len(a.uniqueTargets) > 0 {
		// SpellAbility.canTarget (SpellAbility.java:1401): a TargetUnique$
		// link may not choose what an ancestor already targets
		// (getUniqueTargets, SpellAbility.java:2034-2044).
		kept := candidates[:0:0]
		for _, e := range candidates {
			if !slices.Contains(a.uniqueTargets, e) {
				kept = append(kept, e)
			}
		}
		candidates = kept
	}
	targeter, err := g.targetingPlayerOf(a)
	if err != nil {
		// A TargetingPlayer$ this port cannot name one player for: the ability
		// is pushed with no targets and fails when it resolves (GO-7), never
		// left off the stack as if it had no legal target.
		return targetChoice{min: targetMin, max: targetMax, err: err}, true, true
	}
	kept := candidates[:0:0]
	for _, e := range candidates {
		if id, isCard := e.AsCard(); isCard {
			met, err := g.candidateRestrictionsMet(a, g.Card(id), targeter)
			if err != nil {
				return targetChoice{min: targetMin, max: targetMax, err: err}, true, true
			}
			if !met {
				continue
			}
		}
		kept = append(kept, e)
	}
	candidates = kept
	if targetMin >= 2 && hasSameControllerRestriction(a) {
		candidates = g.withSameControllerPartner(candidates)
	}
	if len(candidates) == 0 && targetMin > 0 {
		return targetChoice{}, true, false
	}
	// An "up to N" choice (TargetMin$ 0) with nothing to choose still goes on:
	// TargetSelection.chooseTargets cancels only when there are neither enough
	// candidates nor enough targets (TargetSelection.java:82, 113-120), and a
	// minimum of 0 is always enough (Gaea's Blessing over an empty graveyard).
	return g.capDividedChoice(a, targetChoice{candidates: candidates, min: targetMin, max: targetMax, targeter: targeter}), true, true
}

// specCanTargetPlayer is TargetRestrictions.canTgtPlayer for a ValidTgts$
// spec: some comma alternative starts with "Player", "Opponent" or "Any"
// (TargetRestrictions.java:454-461 tests exactly those three prefixes, so "You"
// alone does not count).
func specCanTargetPlayer(spec string) bool {
	for _, alt := range strings.Split(spec, ",") {
		if strings.HasPrefix(alt, "Player") || strings.HasPrefix(alt, "Opponent") || strings.HasPrefix(alt, "Any") {
			return true
		}
	}
	return false
}

// targetCandidates is targetCandidatesInZones scoped to the battlefield --
// CR's own implicit "target creature" scope, and every real corpus
// ValidTgts$ line's default when it names no TgtZone$ of its own.
func (g *Game) targetCandidates(controller PlayerID, source CardID, spec string, kind string) []EntityID {
	return g.targetCandidatesInZones(controller, source, spec, []ZoneType{Battlefield}, targetAsk{kind: kind})
}

// targetCandidatesInZones is the union of spec evaluated against every
// player still in the game (matchesPlayerSpec, valid.go; a player who has
// lost is never a legal target, the identical exclusion definedPlayers's own
// `if (!p.isInGame())` reading already makes) AND against every card in any
// player's zones (Matches, valid.go) -- targetChoiceFor's own TgtZone$ read,
// scoped to Play alone (27 real DB$ Play | TgtZone$ Graveyard lines, Pass
// the Torch's TrigPlay among them), Battlefield-only otherwise
// (targetCandidates, above, and targetChoiceFor's own API check). Rejecting
// TgtZone$ elsewhere (playeffect.go's own Defined$+TgtZone$ case,
// airbendeffect.go, animate.go's now-removed Execute$ check) is each of
// those APIs' own separate targeting shape, not this one; every other API
// reaching this general path (ChangeZone, Pump, Effect, PutCounter,
// MakeCard, Clone, RepeatEach, ...) still ignores a TgtZone$ it may carry,
// unaudited against a non-battlefield target -- widening past Play needs
// that audit first (targetChoiceFor's own comment has the reasoning).
//
// Both pools are always tried, never one or the other picked by a spec's own
// shape: Java's own TargetRestrictions.getAllCandidates (CR 115's own "any
// target" candidate collection) does the identical thing, unconditionally
// probing game.getPlayers() and game.getCardsIn(zone) for every ValidTgts$
// spec -- an ordinary card-shaped spec ("Creature.YouCtrl") simply matches
// zero players the same way an ordinary player-shaped one ("Opponent")
// matches zero cards, filtering happening entirely inside matchesPlayerSpec/
// Matches rather than by picking a pool up front. This port tried the
// "pick one pool by a trial match" shortcut first; it is unsound for any
// spec whose comma-separated alternatives mix a player-shaped and a
// card-shaped one -- CR 115's own "Any" (matchesPlayerBase's own "Any" case)
// is the single-token example, but the corpus also writes it out explicitly
// (`Player,Planeswalker`, 273 real lines corpus-wide) -- so the union is not
// an "Any"-only special case, it is the general, correct shape. Players are
// probed once regardless of how many card zones are named: TgtZone$ names
// where a card candidate may sit, never a player-shaped alternative's own
// scope, so naming two zones does not double the player half.
func (g *Game) targetCandidatesInZones(controller PlayerID, source CardID, spec string, zones []ZoneType, ask targetAsk) []EntityID {
	candidates := g.playerCandidates(controller, source, spec, ask)
	parsed := valid.Parse(spec)
	for _, zone := range zones {
		for _, pid := range g.Players() {
			candidates = g.appendCardCandidates(candidates, g.Zone(zone, pid).Cards(), parsed, controller, source, ask)
		}
	}
	return candidates
}

// playerCandidates is getAllCandidates' player half: every player still in
// the game spec matches and who can be targeted.
func (g *Game) playerCandidates(controller PlayerID, source CardID, spec string, ask targetAsk) []EntityID {
	var candidates []EntityID
	for _, pid := range g.Players() {
		if g.Player(pid).Lost {
			continue
		}
		if matched, _ := matchesPlayerSpec(g, pid, controller, source, spec); matched && !playerCantBeTargetedBy(g, pid, controller, source, ask) {
			candidates = append(candidates, PlayerEntity(pid))
		}
	}
	return candidates
}

// appendCardCandidates appends every card of ids that matches parsed and can
// be targeted (getAllCandidates' sa.canTarget(c) over one zone's cards).
func (g *Game) appendCardCandidates(candidates []EntityID, ids []CardID, parsed valid.Spec, controller PlayerID, source CardID, ask targetAsk) []EntityID {
	for _, id := range ids {
		c := g.Card(id)
		if Matches(g, c, parsed, controller, source) && !cardCantBeTargetedBy(g, c, controller, source, ask) {
			candidates = append(candidates, CardEntity(id))
		}
	}
	return candidates
}

// zoneListCandidates is TargetRestrictions.getAllCandidates for a ChangeZone
// whose Origin$ zones include the stack (AbilityFactory.adjustChangeZoneTarget
// sets the target zones to Origin$): the players first, then each zone's cards
// in Origin$ order (Game.getCardsIn(List<ZoneType>)), the stack's being the
// card of every spell on it (stackSpellCards). A spell is never its own target
// (CR 115.5, SpellAbility.java:1391-1395): the one being cast is not on the
// stack yet while its targets are chosen.
func (g *Game) zoneListCandidates(a *Ability, spec string, zones []ZoneType) []EntityID {
	ask := askOf(a)
	candidates := g.playerCandidates(a.Controller, a.Source, spec, ask)
	parsed := valid.Parse(spec)
	for _, zone := range zones {
		if zone == Stack {
			// Stack alone is TargetSelection.chooseCardFromStack, which walks
			// the stack top first (TargetSelection.java:97-101, :298); in a
			// list it is the Stack zone's cards, oldest first (Zone.add
			// appends, Zone.java:141-146).
			cards := g.stackSpellCards(len(zones) == 1)
			if a.spell {
				cards = slices.DeleteFunc(cards, func(id CardID) bool { return id == a.Source })
			}
			candidates = g.appendCardCandidates(candidates, cards, parsed, a.Controller, a.Source, ask)
			continue
		}
		for _, pid := range g.Players() {
			candidates = g.appendCardCandidates(candidates, g.Zone(zone, pid).Cards(), parsed, a.Controller, a.Source, ask)
		}
	}
	return candidates
}

// stackSpellCards is the card of every spell on the stack still in the Stack
// zone, top first or oldest first. Each card appears once.
func (g *Game) stackSpellCards(topFirst bool) []CardID {
	var out []CardID
	n := len(g.stack)
	for i := 0; i < n; i++ {
		item := &g.stack[i]
		if topFirst {
			item = &g.stack[n-1-i]
		}
		if !item.spell || item.Source == NoCard || int(item.Source) >= len(g.cards) || g.Card(item.Source).Zone != Stack || slices.Contains(out, item.Source) {
			continue
		}
		out = append(out, item.Source)
	}
	return out
}

// stackSpellCandidates is every spell on the stack -- a card in the Stack
// zone, top first -- matching spec.
func (g *Game) stackSpellCandidates(controller PlayerID, source CardID, spec string) []EntityID {
	parsed := valid.Parse(spec)
	var candidates []EntityID
	for i := len(g.stack) - 1; i >= 0; i-- {
		id := g.stack[i].Source
		c := g.Card(id)
		if c.Zone != Stack || containsEntity(candidates, CardEntity(id)) {
			continue
		}
		if Matches(g, c, parsed, controller, source) {
			candidates = append(candidates, CardEntity(id))
		}
	}
	return candidates
}

// stackAbilityCandidates is TargetSelection.chooseCardFromStack's offer
// (TargetSelection.java:288-303): every stack item, top first, the targeting
// ability a can target (canTargetStackItem). A spell is offered as its card on
// the stack (CardEntity), the convention Counter and CopySpellAbility already
// read back through spellItemOf; an activated or triggered ability as its
// stack item (StackItemEntity, ADR-0041).
func (g *Game) stackAbilityCandidates(a *Ability) ([]EntityID, error) {
	var candidates []EntityID
	for i := len(g.stack) - 1; i >= 0; i-- {
		item := &g.stack[i]
		ok, err := g.canTargetStackItem(a, item)
		if err != nil {
			return nil, err
		}
		if ok {
			candidates = append(candidates, stackTargetEntity(item))
		}
	}
	return candidates, nil
}

// stackTargetEntity is how item is named as a target: a spell as its card, an
// ability as its stack item (ADR-0041).
func stackTargetEntity(item *Ability) EntityID {
	if item.spell {
		return CardEntity(item.Source)
	}
	return StackItemEntity(item.ID)
}

// canTargetStackItem is SpellAbility.canTargetSpellAbility
// (SpellAbility.java:2046-2110) for the targeting ability a and the stack
// item it would target, asked both as targets are chosen and at resolution
// (MagicStack.java:753-755): item is not a itself, matches one of
// TargetType$'s comma-separated alternatives (stackItemMatches), one of its
// targets matches TargetValidTargeting$ when a names one, and its host card
// matches ValidTgts$ (stackItemHostMatches).
func (g *Game) canTargetStackItem(a, item *Ability) (bool, error) {
	if item.Source == NoCard || int(item.Source) >= len(g.cards) {
		return false, nil
	}
	if a.ID != NoStackItem && item.ID == a.ID {
		return false, nil
	}
	// ValidTgts$ first: a property below can raise an error for an item this
	// would have ruled out anyway. An ability naming none was handed its
	// targets some other way (a test, a Ward stand-in) and has no host
	// restriction to check.
	if validTgts, ok := a.Params.Param("ValidTgts"); ok {
		matched, err := g.stackItemHostMatches(a, item, validTgts)
		if err != nil || !matched {
			return false, err
		}
	}
	if targetType, ok := a.Params.Param("TargetType"); ok {
		matched, err := g.stackItemMatches(item, targetType, a)
		if err != nil || !matched {
			return false, err
		}
	}
	if tvt, ok := a.Params.Param("TargetValidTargeting"); ok {
		matched, err := g.stackItemTargetsMatch(item, tvt, a)
		if err != nil || !matched {
			return false, err
		}
	}
	return true, nil
}

// stackItemHostMatches is canTargetSpellAbility's host check
// (SpellAbility.java:2098-2109): item's host card matches ValidTgts$. The host
// of an ability an Effect card carries (a delayed or effect trigger) is that
// effect's source card in Java, which this port does not record: a ValidTgts$
// with a bare Card alternative is still answered (any source card matches it),
// any other is an error rather than a guess about the source.
func (g *Game) stackItemHostMatches(a, item *Ability, validTgts string) (bool, error) {
	spec := valid.Parse(validTgts)
	host := g.Card(item.Source)
	if host.IsEffect && !item.spell {
		for _, alt := range spec.Alternatives {
			if alt.Base.Name == "Card" && !alt.Base.Negated && len(alt.Properties) == 0 {
				return true, nil
			}
		}
		return false, fmt.Errorf("ValidTgts$ %q: an ability of an effect card, whose effect source is not recorded, not resolvable yet", validTgts)
	}
	return Matches(g, host, spec, a.Controller, a.Source), nil
}

// stackItemMatches is SpellAbility.isValid (SpellAbility.java:2193-2261)
// over restriction's comma-separated alternatives, for item judged by
// ability a: the kind before the first "." (Spell, SpellAbility, Ability,
// Activated, Triggered, Instant, Sorcery), then every "+"-joined property
// after it (stackItemHasProperty). Activated and Triggered are the item's
// causeKind (game.go): a trigger pushTriggeredAbilities put on the stack, or
// any other non-spell item. A kind or property this port does not read is an
// error, not a false (GO-7).
func (g *Game) stackItemMatches(item *Ability, restriction string, a *Ability) (bool, error) {
	for _, alt := range strings.Split(restriction, ",") {
		head, rest, hasRest := strings.Cut(alt, ".")
		var kind bool
		switch head {
		case "Spell":
			kind = item.spell
		case "SpellAbility":
			kind = true
		case "Ability":
			kind = !item.spell
		case "Activated":
			kind = item.causeKind() == causeActivated
		case "Triggered":
			kind = item.causeKind() == causeTriggered
		case "Instant":
			kind = g.Card(item.Source).Type().Has(cardtype.Instant)
		case "Sorcery":
			kind = g.Card(item.Source).Type().Has(cardtype.Sorcery)
		default:
			return false, fmt.Errorf("TargetType$ %q not resolvable yet", alt)
		}
		if !kind {
			continue
		}
		matched := true
		if hasRest {
			for _, prop := range strings.Split(rest, "+") {
				ok, err := g.stackItemHasProperty(item, prop, a)
				if err != nil {
					return false, err
				}
				if !ok {
					matched = false
					break
				}
			}
		}
		if matched {
			return true, nil
		}
	}
	return false, nil
}

// stackItemHasProperty is SpellAbilityProperty.hasProperty
// (SpellAbilityProperty.java:213-245) at the properties ChangeTargets'
// TargetType$ lines name: singleTarget (exactly one target, the same object
// twice counting twice), numTargets <op><n> (distinct objects), IsTargeting
// Self/You (a's host card, a's controller), YouCtrl/OppCtrl.
func (g *Game) stackItemHasProperty(item *Ability, prop string, a *Ability) (bool, error) {
	switch prop {
	case "YouCtrl":
		return item.Controller == a.Controller, nil
	case "OppCtrl":
		return item.Controller != a.Controller, nil
	case "singleTarget":
		targets, err := stackItemTargets(item)
		return len(targets) == 1, err
	}
	name, arg, _ := strings.Cut(prop, " ")
	switch name {
	case "numTargets":
		targets, err := stackItemTargets(item)
		if err != nil {
			return false, err
		}
		var distinct []EntityID
		for _, t := range targets {
			if !containsEntity(distinct, t) {
				distinct = append(distinct, t)
			}
		}
		if len(arg) < 3 {
			return false, fmt.Errorf("TargetType$ property %q not resolvable yet", prop)
		}
		n, err := strconv.Atoi(arg[2:])
		if err != nil {
			return false, fmt.Errorf("TargetType$ property %q not resolvable yet", prop)
		}
		return compareOp(len(distinct), arg[:2], n), nil
	case "IsTargeting":
		var want EntityID
		switch arg {
		case "Self":
			want = CardEntity(a.Source)
		case "You":
			want = PlayerEntity(a.Controller)
		default:
			return false, fmt.Errorf("TargetType$ property %q not resolvable yet", prop)
		}
		targets, err := stackItemTargets(item)
		return containsEntity(targets, want), err
	}
	if hostCardProperty(prop) {
		// SpellAbilityProperty.java:319-320: a property the ability itself
		// does not have is asked of its host card (Consign to Memory's
		// Spell.Colorless, Tale's End's Spell.Legendary, Nimble
		// Obstructionist's Activated.YouDontCtrl).
		return Matches(g, g.Card(item.Source), valid.Parse("Card."+prop), a.Controller, a.Source), nil
	}
	return false, fmt.Errorf("TargetType$ property %q not resolvable yet", prop)
}

// hostCardProperty reports whether prop is a card property propertyMatches
// (valid.go) answers for the host-card fallback of stackItemHasProperty: a
// core type or supertype word or a color word, each with its non- form,
// MultiColor or YouDontCtrl. Any other is left to the caller's error, since a
// property Matches has no case for reads false for every card, silently.
func hostCardProperty(prop string) bool {
	word := strings.TrimPrefix(prop, "non")
	if _, ok := cardtype.CoreTypeFromName(word); ok {
		return true
	}
	if _, ok := cardtype.SupertypeFromName(word); ok {
		return true
	}
	if _, ok := colorFromName(word); ok || word == "Colorless" {
		return true
	}
	return prop == "MultiColor" || prop == "YouDontCtrl"
}

// stackItemTargetsMatch is canTargetSpellAbility's TargetValidTargeting$
// check (SpellAbility.java:2085-2108): some target of item matches spec,
// judged with a's controller and host.
func (g *Game) stackItemTargetsMatch(item *Ability, spec string, a *Ability) (bool, error) {
	targets, err := stackItemTargets(item)
	if err != nil {
		return false, err
	}
	parsed := valid.Parse(spec)
	for _, t := range targets {
		if g.entityMatches(t, spec, parsed, a.Controller, a.Source) {
			return true, nil
		}
	}
	return false, nil
}

// entityMatches is GameObject.isValid for a card or a player target: spec
// through Matches for a card, matchesPlayerSpec for a player.
func (g *Game) entityMatches(e EntityID, spec string, parsed valid.Spec, controller PlayerID, source CardID) bool {
	if pid, ok := e.AsPlayer(); ok {
		matched, _ := matchesPlayerSpec(g, pid, controller, source, spec)
		return matched
	}
	if id, ok := e.AsCard(); ok {
		return Matches(g, g.Card(id), parsed, controller, source)
	}
	return false
}

// stackItemTargets is SpellAbility.getAllTargetChoices for item: an Aura's
// cast-time Target, its own Targets, then each Charm mode's. A SubAbility$
// naming its own ValidTgts$ keeps its targets on Ability.chainTargets
// (chaintargets.go), which ChangeTargets does not rewrite yet, so asking for
// such an item is an error.
func stackItemTargets(item *Ability) ([]EntityID, error) {
	if subChainTargets(item.Params) {
		return nil, fmt.Errorf("TargetType$: targets of a spell whose SubAbility$ targets not resolvable yet")
	}
	var targets []EntityID
	if item.Target != NoCard {
		targets = append(targets, CardEntity(item.Target))
	}
	targets = append(targets, item.Targets...)
	for _, m := range item.Modes {
		if subChainTargets(m.Params) {
			return nil, fmt.Errorf("TargetType$: targets of a spell whose SubAbility$ targets not resolvable yet")
		}
		targets = append(targets, m.Targets...)
	}
	return targets, nil
}

// subChainTargets reports whether any SubAbility$ below p names its own
// ValidTgts$.
func subChainTargets(p *compile.Ability) bool {
	for p != nil {
		sub, ok := findSubAbility(p)
		if !ok {
			return false
		}
		if _, ok := sub.Ability.Param("ValidTgts"); ok {
			return true
		}
		p = sub.Ability
	}
	return false
}

// targetsStillLegal is CR 608.2b, checked by resolveTop (stack.go) right
// before an ability would resolve: MagicStack.hasFizzled
// (MagicStack.java:704-752). Every chosen target is re-checked on its own
// (targetStillLegal); an illegal one is removed from a's Targets, or its
// Charm mode's, so the effect never sees it (MagicStack.java:748-750). The
// ability fizzles -- reports false -- when at least one target was chosen
// and none is left, unless it or a chosen mode names CantFizzle$. An
// Aura's own single cast-time Target (castAura, castspell.go) keeps its own
// check (auraTargetStillLegal).
//
// Per entity, never by recomputing the candidate scan and intersecting:
// TestRemoveFromGameSpellOnStack (pack3shapes_test.go) targets a spell on
// the stack through a plain ValidTgts$ Card that targetCandidates' own
// battlefield scan would never list.
func (g *Game) targetsStillLegal(a *Ability) bool {
	if !g.dropIllegalTargets(a) {
		return false
	}
	g.recordStackTargets(a)
	if a.API != APIAttach || a.Target == NoCard {
		return true
	}
	return g.auraTargetStillLegal(a)
}

// recordStackTargets keeps, on a and each chosen Charm mode, what every
// ability on the stack it still targets is (Ability.stackTargets), before a
// resolves and possibly removes one. Modes were copied by dropIllegalTargets,
// so writing them reaches a alone.
func (g *Game) recordStackTargets(a *Ability) {
	a.stackTargets = g.stackTargetRefs(a)
	for i := range a.Modes {
		a.Modes[i].stackTargets = g.stackTargetRefs(&a.Modes[i])
	}
}

// stackTargetRefs is the stackTargetRef of every stack-item target of a and
// its SubAbility$ links still on the stack; nil, allocating nothing, when
// there is none -- it runs before every resolution.
func (g *Game) stackTargetRefs(a *Ability) []stackTargetRef {
	out := g.appendStackTargetRefs(nil, a.Targets)
	for _, ct := range a.chainTargets {
		out = g.appendStackTargetRefs(out, ct.Targets)
	}
	return out
}

// appendStackTargetRefs appends to out the stackTargetRef of each stack-item
// entity of targets still on the stack, and of each card target that is a
// spell on the stack (stackTargetRef's spell), in target order: Java's
// getAllTargetChoices order, which SpellTargeted$ reads the first of.
func (g *Game) appendStackTargetRefs(out []stackTargetRef, targets []EntityID) []stackTargetRef {
	for _, e := range targets {
		if ref, ok := g.stackTargetOf(e, nil); ok {
			if item, onStack := g.stackItem(ref.item); onStack {
				ref.targets = allTargetsOf(*item)
			}
			out = append(out, ref)
			continue
		}
		if ref, ok := g.spellTargetRef(e); ok {
			out = append(out, ref)
		}
	}
	return out
}

// spellTargetRef is the stackTargetRef of e when e is a card on the stack as
// a spell: its item, controller, cast record and own targets as they are now.
func (g *Game) spellTargetRef(e EntityID) (stackTargetRef, bool) {
	id, ok := e.AsCard()
	if !ok || g.Card(id).Zone != Stack {
		return stackTargetRef{}, false
	}
	itemID, ok := g.spellItemOf(id)
	if !ok {
		return stackTargetRef{}, false
	}
	item, _ := g.stackItem(itemID)
	c := g.Card(id)
	return stackTargetRef{item: itemID, controller: item.Controller, host: id, spell: true, stamp: c.zoneStamp,
		cast: spellCastRecord{wasCast: c.wasCast, x: c.castX, manaSpent: c.castManaSpent, snow: c.castSnow,
			sources: c.castPaidSources},
		targets: allTargetsOf(*item)}, true
}

// stackTargetOf is the ability on the stack e names: read off the stack while
// it is there, else off recorded, the targeting ability's record of it
// (Ability.stackTargets). false when e is not a stack item or neither knows
// it.
func (g *Game) stackTargetOf(e EntityID, recorded []stackTargetRef) (stackTargetRef, bool) {
	id, ok := e.AsStackItem()
	if !ok {
		return stackTargetRef{}, false
	}
	if item, ok := g.stackItem(id); ok {
		return stackTargetRef{item: id, controller: item.Controller, host: item.Source}, true
	}
	for _, r := range recorded {
		if r.item == id {
			return r, true
		}
	}
	return stackTargetRef{}, false
}

// dropIllegalTargets removes every target of a, and of each chosen Charm
// mode (a Charm's modes are its sub-abilities in Java, which hasFizzled
// recurses into), that is no longer legal. It reports false -- the ability
// fizzles -- when at least one target was chosen and none is left, unless
// the ability or a chosen mode carries CantFizzle$.
func (g *Game) dropIllegalTargets(a *Ability) bool {
	chosen, kept := 0, 0
	cantFizzle := hasCantFizzle(a)
	// Java removes a part's illegal targets only after its sub-abilities were
	// checked (MagicStack.java:773-779), so a link's ParentTarget reads the
	// full chosen list.
	parents := parentTargetsOf(a)
	a.Targets = g.withoutIllegal(a, a.Targets, &chosen, &kept)
	cantFizzle = g.dropIllegalChainTargets(a, parents, &chosen, &kept) || cantFizzle
	// A chosen mode is its own sub-ability chain in Java (CharmEffect's
	// chainAbilities), links included, so hasFizzled walks into it too. The
	// slice is copied before it is written: a stack item copied by value
	// (copySpell, Game.Clone) shares it.
	if len(a.Modes) > 0 {
		a.Modes = append([]Ability(nil), a.Modes...)
	}
	for i := range a.Modes {
		m := &a.Modes[i]
		modeParents := parentTargetsOf(m)
		m.Targets = g.withoutIllegal(m, m.Targets, &chosen, &kept)
		cantFizzle = g.dropIllegalChainTargets(m, modeParents, &chosen, &kept) || hasCantFizzle(m) || cantFizzle
	}
	return chosen == 0 || kept > 0 || cantFizzle
}

// dropIllegalChainTargets is CR 608.2b over owner's SubAbility$ chain:
// hasFizzled recurses into sa.getSubAbility() with the one running fizzle
// flag, so a link's legal target keeps the whole ability from fizzling, and
// its illegal ones are removed (MagicStack.java:745-751). parents are what
// Defined$ ParentTarget names for the first link (owner's targets as chosen);
// each later link reads the nearest earlier one that chose targets, as
// resolveChainTargets asked them. It reports whether a link carries
// CantFizzle$.
func (g *Game) dropIllegalChainTargets(owner *Ability, parents []EntityID, chosen, kept *int) bool {
	cantFizzle := false
	owner.chainTargets = clonedChainTargets(owner.chainTargets)
	ancestorTargeting := owner.ancestorTargeting || linkUsesTargeting(owner.Params)
	for i := range owner.chainTargets {
		ct := &owner.chainTargets[i]
		// The link reads its root's triggering objects, as Java's sub-ability
		// does through getRootAbility (resolveChainTargets' own link), and its
		// parent's targets for TargetsWithSharedCardType$ ParentTarget and the
		// like (Legerdemain, Power Struggle).
		link := Ability{Source: owner.Source, Controller: owner.Controller, Params: ct.Params, targetStamps: ct.Stamps,
			Amounts: owner.Amounts, TriggerRemembered: owner.TriggerRemembered, triggered: owner.triggered, targeter: ct.Targeter,
			parentTargets: parents, ancestorTargeting: ancestorTargeting}
		if api, ok := APIByName(ct.Params.Name); ok {
			link.API = api
		}
		link.Targets = ct.Targets
		chosenHere := ct.Targets
		ct.Targets = g.withoutIllegal(&link, ct.Targets, chosen, kept)
		cantFizzle = cantFizzle || hasCantFizzle(&link)
		if len(chosenHere) > 0 {
			parents = chosenHere
		}
		ancestorTargeting = ancestorTargeting || linkUsesTargeting(ct.Params)
	}
	return cantFizzle
}

// hasCantFizzle reports whether a names CantFizzle$ (Gilded Drake's
// "cannot be countered by rules", MagicStack.java:736-740).
func hasCantFizzle(a *Ability) bool {
	if a.Params == nil {
		return false
	}
	_, ok := a.Params.Param("CantFizzle")
	return ok
}

// withoutIllegal is targets less every one owner can no longer target,
// counting what it saw and what it kept. The slice is rebuilt only when
// something is dropped.
func (g *Game) withoutIllegal(owner *Ability, targets []EntityID, chosen, kept *int) []EntityID {
	*chosen += len(targets)
	drop := 0
	for _, e := range targets {
		if !g.targetStillLegal(owner, e) {
			drop++
		}
	}
	*kept += len(targets) - drop
	if drop == 0 {
		return targets
	}
	out := make([]EntityID, 0, len(targets)-drop)
	for _, e := range targets {
		if g.targetStillLegal(owner, e) {
			out = append(out, e)
		}
	}
	return out
}

// targetStillLegal is SpellAbility.canTarget(entity, fizzleCheck=true)
// (SpellAbility.java:1398-1609) cut to the checks this port runs when a
// target is chosen, so a target is never held to a rule it was not chosen
// under:
//
//   - a card target is the same object it was (its zoneStamp, recorded by
//     stampTargets, has not changed -- CR 400.7), is not phased out
//     (Card.canBeTargetedBy, Card.java:6829-6831, CR 702.26b), still
//     matches owner's ValidTgts$, and -- under TargetsWithSameController$
//     -- shares its controller with every other chosen card target;
//   - a player target has not left the game (Player.canBeTargetedBy,
//     Player.java:1033-1043) and still matches owner's ValidTgts$;
//   - an ability on the stack (ADR-0041) is still there and owner can still
//     target it (MagicStack.java:753-755: canTarget reaches
//     canTargetSpellAbility through SpellAbility.canBeTargetedBy).
//
// Hexproof, shroud and protection (StaticAbilityCantTarget,
// cardCantBeTargetedBy/playerCantBeTargetedBy) are checked identically to
// targetCandidates (Java's own SpellAbility.canTarget runs
// entity.canBeTargetedBy(this) at both call sites regardless of
// fizzleCheck, no asymmetry). Ward is not a "can't be targeted" check at
// all -- it is CR 702.21a's own triggered ability, fired once a target is
// chosen rather than filtering candidates (checkWardTriggers, trigger.go,
// ADR-0028), so it has no place in this function.
func (g *Game) targetStillLegal(owner *Ability, e EntityID) bool {
	// canTarget runs with owner as its sa, the ability in context
	// (inAbilityContext): Ordruun Mentor's Creature.attacking
	// TriggeredAttackedTarget is re-read off the trigger it was chosen under.
	defer g.inAbilityContext(owner)()
	spec, hasSpec := targetSpec(owner)
	if pid, ok := e.AsPlayer(); ok {
		if g.Player(pid).Lost {
			return false
		}
		if playerCantBeTargetedBy(g, pid, owner.Controller, owner.Source, askOf(owner)) {
			return false
		}
		if !hasSpec {
			return true
		}
		matched, _ := matchesPlayerSpec(g, pid, owner.Controller, owner.Source, spec)
		return matched
	}
	if sid, ok := e.AsStackItem(); ok {
		item, onStack := g.stackItem(sid)
		if !onStack || owner.Params == nil {
			return false
		}
		legal, err := g.canTargetStackItem(owner, item)
		if err != nil {
			g.recordPendingError(err)
			return false
		}
		return legal
	}
	id, ok := e.AsCard()
	if !ok {
		return true
	}
	c := g.Card(id)
	if stamp, ok := owner.stampOf(id); ok && stamp != c.zoneStamp {
		return false
	}
	if c.IsPhasedOut() {
		return false
	}
	if cardCantBeTargetedBy(g, c, owner.Controller, owner.Source, askOf(owner)) {
		return false
	}
	if hasSameControllerRestriction(owner) {
		// SpellAbility.java:1543-1549 runs at fizzle time too: a target
		// whose controller no longer matches every other chosen card
		// target is illegal. owner.Targets is still the full chosen list
		// here (withoutIllegal rebuilds it only afterwards), matching
		// MagicStack.hasFizzled's remove-after-the-loop order.
		for _, other := range owner.Targets {
			if oid, ok := other.AsCard(); ok && oid != id && g.Card(oid).Controller() != c.Controller() {
				return false
			}
		}
	}
	if owner.Params != nil {
		// SpellAbility.canTarget runs at fizzle time too: the per-card
		// restrictions, and (TargetsForEachPlayer$) a target that changed
		// controller since it was chosen (TargetChoices.forEachControllerChanged).
		targeter := owner.targeter
		if targeter == NoPlayer {
			// Recorded as the targets were chosen (resolveTargets,
			// chainTarget.Targeter); an ability that never went through that
			// (a test resolving one directly) asks its TargetingPlayer$ now.
			// One that no longer names a player makes the target illegal and
			// says why, never the controller's guess (GO-7): Java's stored
			// targeting player has no such gap.
			var err error
			if targeter, err = g.targetingPlayerOf(owner); err != nil {
				g.recordPendingError(err)
				return false
			}
		}
		met, err := g.candidateRestrictionsMet(owner, c, targeter)
		if err != nil {
			g.recordPendingError(err)
			return false
		}
		if !met {
			return false
		}
		if was, recorded := owner.forEachControllers[id]; recorded && was != c.Controller() {
			return false
		}
		if _, stackTargeting := owner.Params.Param("TargetType"); stackTargeting && c.Zone == Stack {
			// A spell is a SpellAbility target in Java, re-checked by
			// canTargetSpellAbility like an ability (MagicStack.java:753-755):
			// TargetValidTargeting$ reads its targets as they are now.
			if sid, ok := g.spellItemOf(id); ok {
				item, _ := g.stackItem(sid)
				legal, err := g.canTargetStackItem(owner, item)
				if err != nil {
					g.recordPendingError(err)
					return false
				}
				if !legal {
					return false
				}
			}
		}
	}
	if !hasSpec {
		return true
	}
	return Matches(g, c, valid.Parse(spec), owner.Controller, owner.Source)
}

// targetSpec is the ValidTgts$ a's targets were chosen against
// (targetChoiceFor's own reading, Earthbend's implicit one included).
func targetSpec(a *Ability) (string, bool) {
	if a.Params == nil {
		return "", false
	}
	if spec, ok := a.Params.Param("ValidTgts"); ok {
		return spec, true
	}
	if a.API == APIEarthbend {
		return "Land.YouCtrl", true
	}
	return "", false
}

// stampTargets records the zoneStamp of every card a and each of its Charm
// modes target -- an Aura's own Target included -- keeping the stamp
// already recorded for a card that was a target before. PushAbility calls
// it as a goes on the stack; ChangeTargets again after it rewrites an
// item's targets. Keeping old stamps is what makes a target that changed
// zones stay illegal (CR 400.7) when ChangeTargets rewrites a different
// target of the same item, or when CopySpellAbility's copy keeps the
// original's targets: Java's copy carries the original target objects with
// their gameTimestamp. Only a newly chosen card is stamped as it is now.
func (g *Game) stampTargets(a *Ability) {
	a.targetStamps = g.restamp(a.targetStamps, a.Targets, a.Target)
	g.stampChainTargets(a)
	// Copied before writing, as dropIllegalTargets does: a's Modes and links
	// may share their arrays with another stack item (copySpell, Game.Clone).
	if len(a.Modes) > 0 {
		a.Modes = append([]Ability(nil), a.Modes...)
	}
	for i := range a.Modes {
		m := &a.Modes[i]
		m.targetStamps = g.restamp(m.targetStamps, m.Targets, NoCard)
		g.stampChainTargets(m)
	}
}

// clonedChainTargets is cts with its own backing arrays, Targets and Stamps
// included, so writing one copy's links (stampTargets, dropIllegalTargets,
// a copy's new targets) never reaches another: a stack item copied by value
// (copySpell, Game.Clone) shares them otherwise. Nil stays nil.
func clonedChainTargets(cts []chainTarget) []chainTarget {
	if len(cts) == 0 {
		return nil
	}
	out := make([]chainTarget, len(cts))
	for i, ct := range cts {
		out[i] = chainTarget{
			Params:  ct.Params,
			Targets: append([]EntityID(nil), ct.Targets...),
			Stamps:  append([]targetStamp(nil), ct.Stamps...),
			// Never written in place (Ability.divided), so shared as is.
			Divided:  ct.Divided,
			Err:      ct.Err,
			Targeter: ct.Targeter,
		}
	}
	return out
}

// stampChainTargets is stampTargets for a's SubAbility$ links.
func (g *Game) stampChainTargets(a *Ability) {
	a.chainTargets = clonedChainTargets(a.chainTargets)
	for i := range a.chainTargets {
		ct := &a.chainTargets[i]
		ct.Stamps = g.restamp(ct.Stamps, ct.Targets, NoCard)
	}
}

// restamp is stampTargets for one target list plus an optional Aura
// target.
func (g *Game) restamp(old []targetStamp, targets []EntityID, aura CardID) []targetStamp {
	var out []targetStamp
	add := func(id CardID) {
		if id == NoCard || int(id) >= len(g.cards) {
			return
		}
		for _, s := range old {
			if s.card == id {
				out = append(out, s)
				return
			}
		}
		out = append(out, targetStamp{card: id, stamp: g.Card(id).zoneStamp})
	}
	for _, e := range targets {
		if id, ok := e.AsCard(); ok {
			add(id)
		}
	}
	add(aura)
	return out
}

// auraTargetStillLegal is targetsStillLegal's own Aura branch: a's Target
// (castAura, castspell.go) is still legal only if it is still on the
// battlefield, still matches self's own Enchant restriction, and still does
// not refuse self outright (cardCantBeTargetedBy, staticability.go -- the
// identical two checks enchantTargets already ran to build the candidate set
// this target was chosen from, castspell.go).
func (g *Game) auraTargetStillLegal(a *Ability) bool {
	c := g.Card(a.Source)
	target := g.Card(a.Target)
	// A phased-out host cannot be targeted (Card.canBeTargetedBy,
	// Card.java:6829-6831).
	if target.IsPhasedOut() {
		return false
	}
	// CR 400.7: a host that left and came back is a new object.
	if stamp, ok := a.stampOf(a.Target); ok && stamp != target.zoneStamp {
		return false
	}
	spec, ok := enchantSpec(c)
	// The graveyard is a legal host zone only for an Enchant type naming a zone
	// (enchantTargets).
	if target.Zone != Battlefield && (target.Zone != Graveyard || !ok || !enchantsOutsidePlay(spec)) {
		return false
	}
	ask := targetAsk{kind: causeSpell}
	if ok {
		ask.enchant = spec.String()
	}
	if cardCantBeTargetedBy(g, target, a.Controller, a.Source, ask) {
		return false
	}
	if !ok {
		return true
	}
	return Matches(g, target, spec, a.Controller, a.Source)
}

// radianceSpec is the ValidTgts$ of the ability whose targets Radiance$
// fans out from, or "" when none does: CardUtil.getRadiance reads
// SpellAbility.getSATargetingCard, a itself when it targets a card, else the
// nearest ancestor that does (CardUtil.java:149-153), and only a Radiance$
// line there counts -- a sub-ability needs no Radiance$ of its own. radiance
// carries the ancestor's answer (resolveSubAbility).
func (a *Ability) radianceSpec() string {
	if spec, ok := a.Params.Param("ValidTgts"); ok {
		for _, e := range a.Targets {
			if _, isCard := e.AsCard(); isCard {
				if _, rad := a.Params.Param("Radiance"); rad {
					return spec
				}
				return ""
			}
		}
	}
	return a.radiance
}

// radianceCards is CardUtil.getRadiance (CardUtil.java:149-182): every
// battlefield permanent, other than a target, that shares a color with some
// target and is valid for the targeting ability's ValidTgts$ -- evaluated for
// the host's controller, so hexproof and the like do not matter, the cards
// were never targeted. Colors are visited in WUBRG order, then each color's
// permanents in battlefield order (Game.getColoredCardsInPlay); a permanent
// of two shared colors appears once. Computed at resolution from the
// targets' current colors.
func (g *Game) radianceCards(a *Ability) []CardID {
	specStr := a.radianceSpec()
	if specStr == "" {
		return nil
	}
	targets := a.Targets
	if _, own := a.Params.Param("ValidTgts"); !own && len(targets) == 0 {
		targets = a.parentTargets
	}
	var shared mana.Colors
	isTarget := make(map[CardID]bool, len(targets))
	for _, e := range targets {
		if id, ok := e.AsCard(); ok {
			isTarget[id] = true
			shared |= g.Card(id).Colors()
		}
	}
	spec := valid.Parse(specStr)
	controller := g.Card(a.Source).Controller()
	var out []CardID
	seen := map[CardID]bool{}
	for _, color := range [...]mana.Colors{mana.White, mana.Blue, mana.Black, mana.Red, mana.Green} {
		if !shared.Has(color) {
			continue
		}
		for _, pid := range g.Players() {
			for _, cid := range g.Zone(Battlefield, pid).Cards() {
				c := g.Card(cid)
				// A phased-out permanent is not on the battlefield for
				// getCards (PlayerZoneBattlefield.java:75).
				if seen[cid] || isTarget[cid] || c.IsPhasedOut() || !c.Colors().Has(color) || !Matches(g, c, spec, controller, a.Source) {
					continue
				}
				seen[cid] = true
				out = append(out, cid)
			}
		}
	}
	return out
}
