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
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// targetUnresolvedParams names ValidTgts$'s own structural siblings this
// port does not parse: Radiance$ (4 real corpus lines -- "and each other
// permanent that shares a color with it," a second, derived candidate set
// no single ValidTgts$ evaluation produces), TargetsForEachPlayer$/
// TargetsWithDefinedController$/TargetUnique$ (0 real lines each) -- every
// one its own further mechanic. A line naming any of these is treated the
// same as CR 603.3c's own "no legal targets" case (below) rather than
// erroring: both mean the ability does not do anything, and this port has
// no way to tell the difference from outside without building the shape
// (PORT-8/GO-7's "skip rather than guess," folded into the identical bucket
// a genuinely empty candidate set already uses since the two are
// observationally identical).
var targetUnresolvedParams = [...]string{
	"Radiance", "TargetsForEachPlayer", "TargetsWithDefinedController", "TargetUnique",
}

// resolveTargets is CR 601.2c/603.3b's own "choose targets," and reports
// whether a is still eligible to be pushed onto the stack at all. true with
// a.Targets left nil means there was nothing to target in the first place --
// no ValidTgts$ named. false means either CR 603.3c's own real rule ("if
// the ability requires a target and there are no legal targets, it doesn't
// go on the stack") or a target shape targetUnresolvedParams names above --
// the two are folded into the same outcome rather than given an error
// return, because a caller cannot act on "an ability the game decided not
// to put on the stack" any differently than "an ability this port cannot
// parse the targeting for": either way, nothing happens, and GO-7's own
// "fail one game, not the batch" reasoning does not apply to a card that
// was never going to do anything this port can tell.
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
	a.Targets = controller.ChooseTargets(g, a.Controller, choice.candidates, choice.min, choice.max)
	return true
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
}

// targetChoiceFor builds a's targetChoice from its ValidTgts$/TargetType$/
// TargetMin$/TargetMax$, the scan resolveTargets asks the controller over
// and a copy's new targets are chosen from (CR 707.10c, copySpell,
// copyspellabilityeffect.go). named is false when a names no ValidTgts$ at
// all; ok is false when it does but has no legal candidate or a shape
// targetUnresolvedParams names -- resolveTargets' own two outcomes.
func (g *Game) targetChoiceFor(a *Ability) (choice targetChoice, named, ok bool) {
	validTgts, named := a.Params.Param("ValidTgts")
	if !named && a.API == APIEarthbend {
		// EarthbendEffect.buildSpellAbility sets the target restriction
		// itself; no script line names it.
		validTgts, named = "Land.YouCtrl", true
	}
	if !named {
		return targetChoice{}, false, true
	}
	for _, key := range targetUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return targetChoice{}, true, false
		}
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
	targetMin, ok := resolveNamedAmount(g, a.Amounts, source, minStr)
	if !ok {
		return targetChoice{}, true, false
	}
	targetMax, ok := resolveNamedAmount(g, a.Amounts, source, maxStr)
	if !ok {
		return targetChoice{}, true, false
	}

	var candidates []EntityID
	targetType, hasTargetType := a.Params.Param("TargetType")
	switch {
	case hasTargetType && a.API == APIChangeTargets:
		// ChangeTargets alone reads TargetType$ as SpellAbility.isValid's
		// full restriction (stackAbilityCandidates): 23 of its 44 corpus
		// lines name a shape past the literal "Spell". Every other API keeps
		// the literal-only branch below, since its own Resolve was vetted
		// against spell targets only.
		var err error
		candidates, err = g.stackAbilityCandidates(a, targetType, validTgts)
		if err != nil {
			return targetChoice{min: targetMin, max: targetMax, err: err}, true, true
		}
	case hasTargetType:
		// TargetType$ Spell names spells on the stack (CR 115.1a); a spell
		// here is a card in the Stack zone. Activated/Triggered abilities
		// on the stack are not targetable objects in this port.
		if targetType != "Spell" {
			return targetChoice{}, true, false
		}
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
					// Zone(Stack, pid) is never the real Stack zone (kept
					// under Zone(Stack, NoPlayer), game.go): scanning it
					// per player would silently find nothing, and
					// allocating an empty per-player Stack zone as a side
					// effect (Game.Zone's own lazy-create) besides. No real
					// Play line names it -- fail closed rather than guess.
					return targetChoice{min: targetMin, max: targetMax,
						err: fmt.Errorf("TgtZone$ %q: Stack not resolvable through this path yet", raw)}, true, true
				}
				zones = parsed
			}
		}
		candidates = g.targetCandidatesInZones(a.Controller, a.Source, validTgts, zones)
	}
	if targetMin >= 2 && hasSameControllerRestriction(a) {
		candidates = g.withSameControllerPartner(candidates)
	}
	if len(candidates) == 0 {
		return targetChoice{}, true, false
	}
	return targetChoice{candidates: candidates, min: targetMin, max: targetMax}, true, true
}

// targetCandidates is targetCandidatesInZones scoped to the battlefield --
// CR's own implicit "target creature" scope, and every real corpus
// ValidTgts$ line's default when it names no TgtZone$ of its own.
func (g *Game) targetCandidates(controller PlayerID, source CardID, spec string) []EntityID {
	return g.targetCandidatesInZones(controller, source, spec, []ZoneType{Battlefield})
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
func (g *Game) targetCandidatesInZones(controller PlayerID, source CardID, spec string, zones []ZoneType) []EntityID {
	var candidates []EntityID
	for _, pid := range g.Players() {
		if g.Player(pid).Lost {
			continue
		}
		if matched, _ := matchesPlayerSpec(g, pid, controller, source, spec); matched && !playerCantBeTargetedBy(g, pid, controller, source) {
			candidates = append(candidates, PlayerEntity(pid))
		}
	}

	parsed := valid.Parse(spec)
	for _, zone := range zones {
		for _, pid := range g.Players() {
			for _, id := range g.Zone(zone, pid).Cards() {
				c := g.Card(id)
				if Matches(g, c, parsed, controller, source) && !cardCantBeTargetedBy(g, c, controller, source) {
					candidates = append(candidates, CardEntity(id))
				}
			}
		}
	}
	return candidates
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

// stackAbilityCandidates is SpellAbility.canTargetSpellAbility
// (SpellAbility.java:2059-2122) run over every stack item, top first, for a
// ChangeTargets ability a: the item matches one of TargetType$'s
// comma-separated alternatives (stackItemMatches), one of its targets
// matches TargetValidTargeting$ when a names one, and its host card matches
// ValidTgts$. A spell is offered as its card on the stack (CardEntity), the
// convention Counter and CopySpellAbility already read back through
// spellItemOf.
//
// An activated or triggered ability on the stack has no EntityID (id.go: a
// card or a player), so one that would be a legal target is an error rather
// than a candidate left out: offering the spells alone would be a narrower
// question than Java asks. Ability has no activated/triggered kind, so
// "Activated" and "Triggered" each match every non-spell item; the answer
// they change is only which items raise that error.
func (g *Game) stackAbilityCandidates(a *Ability, targetType, validTgts string) ([]EntityID, error) {
	validSpec := valid.Parse(validTgts)
	tvt, hasTVT := a.Params.Param("TargetValidTargeting")
	var candidates []EntityID
	for i := len(g.stack) - 1; i >= 0; i-- {
		item := &g.stack[i]
		if item.Source == NoCard || int(item.Source) >= len(g.cards) {
			continue
		}
		// ValidTgts$ first: a property below can raise an error for an
		// item this would have ruled out anyway.
		if !Matches(g, g.Card(item.Source), validSpec, a.Controller, a.Source) {
			continue
		}
		matched, err := g.stackItemMatches(item, targetType, a)
		if err != nil {
			return nil, err
		}
		if !matched {
			continue
		}
		if hasTVT {
			matched, err := g.stackItemTargetsMatch(item, tvt, a)
			if err != nil {
				return nil, err
			}
			if !matched {
				continue
			}
		}
		if !item.spell {
			return nil, fmt.Errorf("engine: ChangeTargets: TargetType$ %q: targeting an ability on the stack not resolvable yet", targetType)
		}
		candidates = append(candidates, CardEntity(item.Source))
	}
	return candidates, nil
}

// stackItemMatches is SpellAbility.isValid (SpellAbility.java:2206-2270)
// over restriction's comma-separated alternatives, for item judged by
// ChangeTargets ability a: the kind before the first "." (Spell,
// SpellAbility, Ability/Activated/Triggered, Instant, Sorcery), then every
// "+"-joined property after it (stackItemHasProperty). A kind or property
// this port does not read is an error, not a false (GO-7).
func (g *Game) stackItemMatches(item *Ability, restriction string, a *Ability) (bool, error) {
	for _, alt := range strings.Split(restriction, ",") {
		head, rest, hasRest := strings.Cut(alt, ".")
		var kind bool
		switch head {
		case "Spell":
			kind = item.spell
		case "SpellAbility":
			kind = true
		case "Ability", "Activated", "Triggered":
			kind = !item.spell
		case "Instant":
			kind = g.Card(item.Source).Type().Has(cardtype.Instant)
		case "Sorcery":
			kind = g.Card(item.Source).Type().Has(cardtype.Sorcery)
		default:
			return false, fmt.Errorf("engine: ChangeTargets: TargetType$ %q not resolvable yet", alt)
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
			return false, fmt.Errorf("engine: ChangeTargets: TargetType$ property %q not resolvable yet", prop)
		}
		n, err := strconv.Atoi(arg[2:])
		if err != nil {
			return false, fmt.Errorf("engine: ChangeTargets: TargetType$ property %q not resolvable yet", prop)
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
			return false, fmt.Errorf("engine: ChangeTargets: TargetType$ property %q not resolvable yet", prop)
		}
		targets, err := stackItemTargets(item)
		return containsEntity(targets, want), err
	}
	return false, fmt.Errorf("engine: ChangeTargets: TargetType$ property %q not resolvable yet", prop)
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
// naming its own ValidTgts$ is not targeted separately in this port
// (resolveSubAbility, subability.go), so Java's count for such an item is
// unknown here and asking is an error.
func stackItemTargets(item *Ability) ([]EntityID, error) {
	if subChainTargets(item.Params) {
		return nil, fmt.Errorf("engine: ChangeTargets: targets of a spell whose SubAbility$ targets not resolvable yet")
	}
	var targets []EntityID
	if item.Target != NoCard {
		targets = append(targets, CardEntity(item.Target))
	}
	targets = append(targets, item.Targets...)
	for _, m := range item.Modes {
		if subChainTargets(m.Params) {
			return nil, fmt.Errorf("engine: ChangeTargets: targets of a spell whose SubAbility$ targets not resolvable yet")
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
	if a.API != APIAttach || a.Target == NoCard {
		return true
	}
	return g.auraTargetStillLegal(a)
}

// dropIllegalTargets removes every target of a, and of each chosen Charm
// mode (a Charm's modes are its sub-abilities in Java, which hasFizzled
// recurses into), that is no longer legal. It reports false -- the ability
// fizzles -- when at least one target was chosen and none is left, unless
// the ability or a chosen mode carries CantFizzle$.
func (g *Game) dropIllegalTargets(a *Ability) bool {
	chosen, kept := 0, 0
	cantFizzle := hasCantFizzle(a)
	a.Targets = g.withoutIllegal(a, a.Targets, &chosen, &kept)
	for i := range a.Modes {
		m := &a.Modes[i]
		m.Targets = g.withoutIllegal(m, m.Targets, &chosen, &kept)
		cantFizzle = cantFizzle || hasCantFizzle(m)
	}
	return chosen == 0 || kept > 0 || cantFizzle
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
//   - anything else (an ability on the stack, ChangeTargets) is kept.
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
	spec, hasSpec := targetSpec(owner)
	if pid, ok := e.AsPlayer(); ok {
		if g.Player(pid).Lost {
			return false
		}
		if playerCantBeTargetedBy(g, pid, owner.Controller, owner.Source) {
			return false
		}
		if !hasSpec {
			return true
		}
		matched, _ := matchesPlayerSpec(g, pid, owner.Controller, owner.Source, spec)
		return matched
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
	if cardCantBeTargetedBy(g, c, owner.Controller, owner.Source) {
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
	for i := range a.Modes {
		m := &a.Modes[i]
		m.targetStamps = g.restamp(m.targetStamps, m.Targets, NoCard)
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
	if target.Zone != Battlefield || target.IsPhasedOut() {
		return false
	}
	// CR 400.7: a host that left and came back is a new object.
	if stamp, ok := a.stampOf(a.Target); ok && stamp != target.zoneStamp {
		return false
	}
	if cardCantBeTargetedBy(g, target, a.Controller, a.Source) {
		return false
	}
	spec, ok := enchantSpec(c)
	if !ok {
		return true
	}
	return Matches(g, target, spec, a.Controller, a.Source)
}
