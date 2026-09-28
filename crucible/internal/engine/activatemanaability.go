// Activating a mana ability past a basic land's own intrinsic one: CR 605.3,
// the corpus's own real A:AB$ Mana lines a card actually prints (rocks,
// dorks, Treasures, ...) -- TapLandForMana (manaability.go) is CR 305.6's
// synthesized version of the identical rule for the one case that needs no
// script line at all.
//
// A mana ability resolves immediately, with no stack (CR 605.3a) -- unlike
// ActivateAbility (activateability.go), this port's own CR 601.3a
// sorcery-speed simplification does not apply here at all. TapLandForMana
// already carries no phase restriction of its own -- paying for something is
// the caller's own job (a fixture, or PayManaCost's own eventual caller),
// not a turn-structure decision this port's own missing priority window
// would otherwise gate -- and ActivateManaAbility keeps the identical
// contract for every other permanent's own printed mana ability.

package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/cost"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// manaAbilityAllowedParams names every Key$ ActivateManaAbility actually
// reads or can safely ignore -- SpellDescription$ describes the ability to a
// human and this port never renders it. Any other key present fails the
// whole line loudly rather than producing the wrong mana or ignoring a real
// restriction (PORT-8/GO-7): RestrictValid$ (53 real lines clearing every
// other gate here) is a mana-pool spending restriction this port's own Pool
// has no way to tag mana with; SubAbility$ (28) chains a further ability
// this immediate, no-stack resolution has nowhere to route through
// Registry.Resolve; IsPresent$/ConditionCheckSVar$/ConditionSVarCompare$/
// PresentCompare$/CheckSVar$/SVarCompare$/OpponentTurn$ (26 combined) are an
// "activate only if..." restriction this port has no general resolving-
// ability gate for (distinct from subAbilityConditionMet's own Condition-
// prefixed pair, which a *resolving* Mana ability never carries in the real
// corpus); TriggersWhenSpent$/AddsKeywords$/AddsKeywordsValid$/
// AddsKeywordsUntil$ (16 combined) tag the mana itself with a further effect
// this port's own Pool cannot carry; AILogic$/AINoRecursiveCheck$/
// PrecostDesc$/Activation$ (13 combined) are AI hinting or a further
// "Activate only as..." cost-description gate, neither read here.
// Planeswalker$ (CR 606.3's own loyalty-ability marker, SpellAbility.
// isPwAbility's own bare hasParam check) and Ultimate$ (purely descriptive,
// never itself a restriction) are both admitted: 21 real A:AB$ Mana lines
// name the first, every one paired with AddCounter<...>/SubCounter<...>.
// ActivationPhases$ is admitted too -- ActivateManaAbility checks it before
// this gate the identical way ActivateAbility does (inActivationPhases,
// activateability.go) -- but the sole real corpus line naming it,
// mana_cache.txt's "Upkeep->Main2", stays unreachable regardless: it also
// names Activator$/PlayerTurn$, neither admitted here (Activator$ is CR
// 606's own "any player" grant and PlayerTurn$ its own "only during their
// turn" restriction, both restrictions this function has no general
// resolving-ability gate for, the identical reasoning as the
// IsPresent$/ConditionCheckSVar$/... group above).
var manaAbilityAllowedParams = map[string]bool{
	"ab": true, "cost": true, "spelldescription": true, "produced": true, "amount": true,
	"planeswalker": true, "ultimate": true, "activationzone": true, "activationphases": true,
}

// manaAbilityParamsResolvable reports whether a is nothing but the params
// manaAbilityAllowedParams admits.
func manaAbilityParamsResolvable(a *compile.Ability) bool {
	for _, p := range a.Params {
		if !manaAbilityAllowedParams[strings.ToLower(p.Key)] {
			return false
		}
	}
	return true
}

// producedManaColor reads Produced$'s own literal single-symbol shape -- one
// of WUBRG, or C for colorless -- 1,005 of the corpus's own 2,156 real
// A:AB$ Mana lines, the dominant shape past "Any" (CR 605.3b's own "choose a
// color," resolved separately in ActivateManaAbility through
// ChooseManaColor) and "Combo <letters>" (parseComboColors, below, a
// restricted-set version of the identical choice). "Chosen" (a color picked
// earlier in the same resolution, an SVar-like reference this port does not
// follow) stays unresolved.
func producedManaColor(produced string) (color mana.Colors, colorless bool, ok bool) {
	switch produced {
	case "W":
		return mana.White, false, true
	case "U":
		return mana.Blue, false, true
	case "B":
		return mana.Black, false, true
	case "R":
		return mana.Red, false, true
	case "G":
		return mana.Green, false, true
	case "C":
		return 0, true, true
	}
	return 0, false, false
}

// parseComboColors reads Produced$'s own "Combo <letters>" shape -- CR
// 605.3b's own restricted-choice version of "Any," a dual/tri-land's real
// "Add W or U"/"Add G, U, or R" -- into the set of colors offered. Every
// token past "Combo" must be a single literal WUBRG letter (a repeat is
// harmless -- options is a set, ORing the same bit twice changes nothing);
// anything else -- "Combo Any"/"Combo AnyDifferent" (CR 605.3b's own
// "in any combination of colors," a per-unit independent choice this
// single-color-per-activation dispatch does not model), "ColorIdentity"
// (Commander's own color-identity set, a format concept this port does not
// track), a "Chosen" token (producedManaColor's own identical unresolved
// reference) -- fails the whole match rather than guessing a subset
// (PORT-8/GO-7).
func parseComboColors(produced string) (mana.Colors, bool) {
	tokens := strings.Fields(produced)
	if len(tokens) < 2 || tokens[0] != "Combo" {
		return 0, false
	}
	var options mana.Colors
	for _, tok := range tokens[1:] {
		if len(tok) != 1 {
			return 0, false
		}
		color, ok := mana.ColorFromLetter(tok[0])
		if !ok {
			return 0, false
		}
		options |= color
	}
	return options, true
}

// ActivateManaAbility is CR 605.3: pay index's own Cost$, then add
// Produced$'s own mana to the pool at once, no stack involved. index selects
// among card's own compiled `A:` lines by position, the identical
// "caller already knows the card's own script" contract ActivateAbility
// already has.
//
// Reports whether the mana was produced. false covers not pid's own
// permanent, not on the battlefield, index not naming an Activated API
// "Mana" line at all, the current step outside ActivationPhases$'s own set
// (inActivationPhases, activateability.go's own identical gate, reused
// outright and checked first the identical way -- 1 real corpus A:AB$ Mana
// line names it, mana_cache.txt's "Upkeep->Main2", already unreachable
// regardless for an unrelated reason: it also names Activator$/PlayerTurn$,
// neither in manaAbilityAllowedParams), a param past manaAbilityAllowedParams, a Cost$ past
// ActivationShape (internal/cost, ActivateAbility's own identical gate,
// reused outright -- ActivateAbility itself refuses API "Mana" and this
// function refuses anything else, so the two never overlap) -- including a
// Discard, PayLife or Return component (both self-reference and chosen-
// type-count shapes), all of which this function declines outright rather
// than silently skipping (PORT-8/GO-7): 0 real corpus A:AB$ Mana lines name
// Discard<...> or PayLife<...> at all, and the sole real line naming
// Return<1/CARDNAME> is already unreachable for an unrelated reason
// (SorcerySpeed$, not in manaAbilityAllowedParams), so ActivateAbility's own
// Discard/PayLife/Return payment (activateability.go) has nothing real to
// reuse here, and letting any of those shapes through unhandled would mean
// claiming the cost was paid in full while never actually discarding,
// losing life, or moving any card -- a Tap-self cost declined by CR
// 602.5b/302.6 (SummonSick/Haste,
// DeclareCombatAttackers' own gate, reused), a PayEnergy component the
// activating player's own Energy counter count cannot actually pay (CR
// 122.5, ActivateAbility's own identical never-below-0 check), a Produced$
// past producedManaColor's own literal shape, "Any" or parseComboColors' own
// literal-letters-only "Combo" shape, ChooseManaColor answering with
// anything but exactly one color from the set it was offered -- not
// re-checked by the interface itself (ChooseManaColor's own doc comment,
// control.go), so this is where that trust ends rather than at
// [Pool.Add]'s own panic -- an Amount$ that does not resolve to a positive
// integer (resolveNamedAmount, amount.go -- pumpAmount's own identical
// plain-integer-or-SVar reading), or an unaffordable mana half of the cost.
//
// PayEnergy, SelfExile, SelfExert, tapXType and Add/SubCounter are not
// declined the way Discard/PayLife/Return are: 4 real corpus A:AB$ Mana
// lines name
// PayEnergy<...> (aether_hub.txt's/servant_of_the_conduit.txt's/
// solar_transformer.txt's own real "T, Pay one energy counter: Add one mana
// of any color" among them), 1 names Exile<1/CARDNAME> (mirrored_lotus.txt's
// own real "T, Exile CARDNAME: Add three mana of any one color"), 1 names
// Exert<1/CARDNAME> with no other unresolved param (a second real line
// combining it also names AddsKeywords$/AddsKeywordsValid$/
// AddsKeywordsUntil$, already outside manaAbilityAllowedParams for an
// unrelated reason, so it stays unreachable regardless of this landing),
// 22 more name tapXType<...> (birchlore_rangers.txt's own real "Tap two
// untapped Elves you control: Add one mana of any color" among them), and 27
// of the 83 real corpus lines naming AddCounter<...>/SubCounter<...> resolve
// (3 AddCounter, 24 SubCounter -- a "[+N]: Add ..." loyalty ability among the
// first, a mana rock's own charge-counter-powered
// "T, Remove a charge counter from CARDNAME: Add one mana of any color"
// shape the corpus's own dominant real one for the second); the remaining
// 56 name a param outside manaAbilityAllowedParams (SubAbility$/AILogic$/
// RestrictValid$/CostDesc$ among the most common), an unresolvable
// AddCounter/SubCounter shape (a non-literal N -- SubCounter<X/...>'s own
// "remove that many counters" storage-counter-battery shape, X1+, or a
// target past the self-reference pair), or a Produced$ past
// producedManaColor's/parseComboColors' own literal shape (a plain
// space-separated multi-color line, "Add {R}{G}" written as
// Produced$ R G rather than Combo R G -- the identical literal-shape gap
// every other Produced$ line already has), each the identical PORT-8/GO-7
// gap ActivateAbility's own doc comment already names. So unlike
// Discard/PayLife/Return this function pays each the identical way
// ActivateAbility does (Player.Counters, counters.go, subtracted and a
// CounterChanged event emitted for PayEnergy; exileCards, exile.go, for
// SelfExile; Card.Exerted set and checkExertedTriggers fired, exertcost.go,
// for SelfExert; ChoosePermanentsToTap/tapChosenPermanents, taptype.go, for
// tapXType; Card.Counters, counters.go, adjusted and a CounterChanged event
// emitted, for Add/SubCounter) rather than refusing a shape the corpus
// actually needs. A tapXType component whose own type spec is unresolvable
// (tapTypeResolvable, taptype.go) or whose own candidate count falls short
// of TapTypeN declines the identical way ActivateAbility's own does, and a
// SubCounter component whose own counter count falls short of SubCounterN
// declines the identical way too. A Planeswalker$ mana ability -- CR 606.3's
// own loyalty-ability restriction, real corpus lines pairing it with
// AddCounter/SubCounter exactly as often here as in ActivateAbility's own
// non-mana lines -- may activate at most once per turn the identical way
// (Card.LoyaltyAbilityActivated, checked and set here too). ActivationZone$
// Graveyard/Hand both apply here too -- the identical owner-not-controller,
// zone-swapped gate and the identical "only that zone's own self-reference
// primitive plus mana" shape restriction ActivateAbility's own doc comment
// already covers -- for the one real corpus line naming Graveyard (a mana
// rock's own "1, Exile CARDNAME from your graveyard: Add one mana of any
// color") and the two naming Hand (both "Exile CARDNAME from your hand: Add
// {R}/{G}", SelfDiscard's own real corpus payoff here is 0 lines, so this
// function declines it the identical way it already declines DiscardN/
// PayLife/Return).
//
// Payment order matches ActivateAbility's own: mana first, tap second,
// self-sac third, exile fourth, exert sixth, energy ninth, tap-by-type
// tenth, add-counter twelfth, remove-counter thirteenth, exile-from-graveyard
// fourteenth, exile-from-hand last -- SelfDiscard is declined outright
// above, so it never reaches this ordering (activateability.go's own
// doc comment has the CR 601.2h reasoning), reusing sacrificeCards
// (sacrificeeffect.go), exileCards (exile.go) and tapChosenPermanents
// (taptype.go) the identical way. A Tap-self cost also
// fires CR 603's own "taps for mana" trigger (checkTapsForManaTriggers,
// trigger.go) alongside the ordinary "becomes tapped" one -- TapLandForMana's
// own pairing, ported here rather than duplicated, and skipped when the cost
// has no Tap component at all (a Treasure-style pure self-sac cost taps
// nothing, so nothing "becomes tapped to produce mana"); a tapXType
// component's own tapped permanents fire only the ordinary "becomes tapped"
// trigger, never "taps for mana" -- they did not themselves produce the
// mana, the source card alone did. A SelfExert component pays off nothing
// here either -- CR 701.42b's own "doesn't untap next turn" cost is
// entirely deferred to the exerting player's own next untapStep (turn.go),
// the identical deferral activateability.go's own doc comment already
// documents for SelfExert. A Planeswalker$ ability marks
// Card.LoyaltyAbilityActivated last, after the mana is already in the pool
// -- activateability.go's own identical ordering, since CR 606.3 restricts
// the whole ability only once it has actually happened.
func (g *Game) ActivateManaAbility(pid PlayerID, card CardID, index int, controller PlayerController) bool {
	c := g.Card(card)
	if c.isDetained() {
		return false
	}
	abilities := c.Def.Faces[0].Abilities
	if index < 0 || index >= len(abilities) {
		return false
	}
	ability := abilities[index]
	if ability.Record != compile.Activated || ability.Name != "Mana" {
		return false
	}
	if !g.inActivationPhases(ability) {
		return false
	}
	if !manaAbilityParamsResolvable(ability) {
		return false
	}
	fromGraveyard, fromHand := false, false
	switch zone, _ := ability.Param("ActivationZone"); zone {
	case "", "Battlefield":
		if c.Controller() != pid || c.Zone != Battlefield {
			return false
		}
	case "Graveyard":
		if c.Owner != pid || c.Zone != Graveyard {
			return false
		}
		fromGraveyard = true
	case "Hand":
		if c.Owner != pid || c.Zone != Hand {
			return false
		}
		fromHand = true
	default:
		return false
	}
	_, isLoyaltyAbility := ability.Param("Planeswalker")
	if isLoyaltyAbility && c.LoyaltyAbilityActivated {
		return false
	}
	costText, ok := ability.Param("Cost")
	if !ok {
		return false
	}
	parsed := cost.Parse(costText)
	shape, ok := parsed.ActivationShape()
	if !ok || shape.DiscardN > 0 || shape.PayLifeN > 0 || shape.SelfReturn || shape.ReturnTypeN > 0 || shape.SelfDiscard {
		return false
	}
	nonBattlefield := fromGraveyard || fromHand
	if nonBattlefield && (shape.Tap || shape.SelfSac || shape.SelfExile || shape.SelfExert ||
		shape.PayEnergyN > 0 || shape.TapTypeN > 0 || shape.AddCounterType != "" || shape.SubCounterType != "") {
		return false
	}
	if !fromGraveyard && shape.SelfExileFromGrave {
		return false
	}
	if !fromHand && shape.SelfExileFromHand {
		return false
	}
	if fromGraveyard && shape.SelfExileFromHand {
		return false
	}
	if fromHand && shape.SelfExileFromGrave {
		return false
	}
	if shape.Tap && (c.Tapped || c.isSick()) {
		return false
	}
	if shape.PayEnergyN > g.Player(pid).Counters.Count(Energy) {
		return false
	}
	if shape.SubCounterType != "" && shape.SubCounterN > c.Counters.Count(CounterType(strings.ToUpper(shape.SubCounterType))) {
		return false
	}
	var tapCandidates []CardID
	if shape.TapTypeN > 0 {
		if !tapTypeResolvable(shape.TapTypeSpec) {
			return false
		}
		tapCandidates = tapTypeCandidates(g, pid, card, shape.Tap, shape.TapTypeSpec)
		if len(tapCandidates) < shape.TapTypeN {
			return false
		}
	}
	produced, ok := ability.Param("Produced")
	if !ok {
		return false
	}
	var color mana.Colors
	var colorless bool
	switch {
	case produced == "Any":
		color = controller.ChooseManaColor(g, pid, card, mana.AllColors)
		if color.Count() != 1 {
			return false
		}
	case strings.HasPrefix(produced, "Combo "):
		options, comboOK := parseComboColors(produced)
		if !comboOK {
			return false
		}
		color = controller.ChooseManaColor(g, pid, card, options)
		if color.Count() != 1 || !options.Has(color) {
			return false
		}
	default:
		color, colorless, ok = producedManaColor(produced)
		if !ok {
			return false
		}
	}
	amount := 1
	if amountText, hasAmount := ability.Param("Amount"); hasAmount {
		amount, ok = resolveNamedAmount(g, c.Def.Faces[0].Amounts, c, amountText)
		if !ok || amount <= 0 {
			return false
		}
	}
	costMana, err := mana.Parse(strings.Join(parsed.Mana, " "))
	if err != nil {
		return false
	}
	if !g.PayManaCost(pid, costMana, controller) {
		return false
	}
	if shape.Tap {
		c.Tapped = true
		g.checkTapsTriggers(controller, card, pid, false)
	}
	if shape.SelfSac {
		sacrificeCards(g, controller, &Ability{Source: card, Controller: pid, Params: ability}, []CardID{card})
	}
	if shape.SelfExile {
		exileCards(g, controller, []CardID{card})
	}
	if shape.SelfExert {
		c.Exerted = true
		g.checkExertedTriggers(controller, card)
	}
	if shape.PayEnergyN > 0 {
		g.Player(pid).Counters.Add(Energy, -shape.PayEnergyN)
		emitCounterChanged(g.sink, card, PlayerEntity(pid), Energy, -shape.PayEnergyN)
	}
	if shape.TapTypeN > 0 {
		chosen := controller.ChoosePermanentsToTap(g, pid, tapCandidates, shape.TapTypeN)
		tapChosenPermanents(g, controller, chosen)
	}
	if shape.AddCounterType != "" {
		ct := CounterType(strings.ToUpper(shape.AddCounterType))
		c.Counters.Add(ct, shape.AddCounterN)
		emitCounterChanged(g.sink, card, CardEntity(card), ct, shape.AddCounterN)
	}
	if shape.SubCounterType != "" {
		ct := CounterType(strings.ToUpper(shape.SubCounterType))
		c.Counters.Add(ct, -shape.SubCounterN)
		emitCounterChanged(g.sink, card, CardEntity(card), ct, -shape.SubCounterN)
	}
	if shape.SelfExileFromGrave {
		exileFromGraveyard(g, card)
	}
	if shape.SelfExileFromHand {
		exileFromHand(g, card)
	}
	if isLoyaltyAbility {
		c.LoyaltyAbilityActivated = true
	}

	made := g.manaReplaced(controller, pid, card, producedMana{
		color: color, colorless: colorless, snow: c.Type().HasSupertype(cardtype.Snow), amount: amount,
	})
	g.addProducedMana(pid, made)

	if shape.Tap {
		g.checkTapsForManaTriggers(controller, card, pid, made)
	}
	return true
}
