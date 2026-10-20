// Activating an ability: CR 602, trimmed to the corpus's own dominant cost
// shapes -- mana, an optional Tap-self token, an optional self-sacrifice
// token (Sac<1/CARDNAME>), an optional self-exile token (Exile<1/CARDNAME>),
// an optional self-return token (Return<1/CARDNAME>, "return this permanent
// to its owner's hand"), an optional self-exert token (Exert<1/CARDNAME>,
// CR 701.42a), an optional "discard N cards of your choice" (Discard<N/Card>),
// an optional "pay N life" (PayLife<N>), an optional "pay N energy counters"
// (PayEnergy<N>), an optional "tap N untapped permanents of a type"
// (tapXType<N/Type>), an optional "return N permanents of a type you
// control" (Return<N/Type>), an optional "put N counters of a kind on this
// permanent" (AddCounter<N/Type>), an optional "remove N counters of a
// kind from this permanent" (SubCounter<N/Type>), an optional
// self-exile-from-the-graveyard (ExileFromGrave<1/CARDNAME>), an optional
// self-discard (Discard<1/CARDNAME>, CR 702.28's own Cycling and its own
// kin) and an optional self-exile-from-hand (ExileFromHand<1/CARDNAME>), in
// any combination (cost.Cost.ActivationShape, internal/cost) -- the only
// primitives this port has payment machinery for. An ability naming
// ActivationZone$ Graveyard or Hand activates from that zone rather than the
// battlefield -- CR 602.2's own generalization past a permanent already on
// the battlefield, ported past its own two dominant real destinations (230
// real lines name Graveyard, 97 name Hand; Command's 57 stays unbuilt): the
// source's own owner, not controller (CR 109.5, a card outside the
// battlefield has no controller), may activate it while it sits in that
// zone rather than Battlefield, and only that zone's own self-reference
// primitive (plus mana) may pay for it -- every other primitive above
// assumes a permanent already on the battlefield, and 0 real corpus lines
// combine one with ActivationZone$ Graveyard or Hand, or combine one
// zone's own self-reference primitive with the other's. A Planeswalker$
// ability -- CR 606.3's own
// loyalty ability, real corpus lines pairing AddCounter/SubCounter with it
// 996 times out of 999 -- may activate at most once per turn regardless of
// which of those two shapes its own cost uses, including AddCounter<0/...>'s
// own real "+0" shape (Card.LoyaltyAbilityActivated, card.go); the raised
// limit a StaticAbilityNumLoyaltyAct effect grants is not built (PORT-8/
// GO-7, 0 real corpus lines this port cannot already resolve some other way
// depend on it). Java's own entry
// point (Player.playSpellAbility, by way of PlayerControllerHuman/AI's own
// input loop) is a real priority-window action, and this port has one now:
// PassPriority (priority.go, ADR-0019). Timing is CR 307.1/canActSorcerySpeed
// (priority.go): instant speed by default, sorcery speed (active player, a
// main phase, an empty stack) only for a loyalty ability (Planeswalker$,
// CR 606.3's own separate restriction) or one explicitly marked
// SorcerySpeed$ -- both read generically off the compiled Ability's own
// params before dispatch.

package engine

import (
	"slices"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cost"
	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// ActivateAbility is CR 602.2: pay index's own Cost$, then the ability
// becomes an object on the stack (CR 602.2g/405.2) -- resolving is
// ResolveStack's own separate step, identical to a cast spell. index
// selects among card's own compiled `A:` lines (Card.Def.Faces[0].
// Abilities, compile.go) by position, the same "caller already knows the
// card's own script" contract a fixture author already has for everything
// else this port drives by index rather than by name.
//
// Reports whether the ability was activated. false covers every
// legal-but-declined case CastSpell's own bool return already covers (wrong
// timing, not pid's own permanent, not on the battlefield, the cost could
// not be paid) plus three more specific to this shape: index does not name
// an Activated-record line at all (a card's own `A:SP$` line, an Instant's
// or Adventure's own spell half, shares the same Abilities slice -- compile.
// go's own doc comment on why both `A:` kinds land there together), the
// line's own API is "Mana" (CR 605.3a's own no-stack immediate resolution,
// a wholly different mechanism this port only has for a basic land's own
// intrinsic ability, TapLandForMana, manaability.go -- extending it to an
// arbitrary permanent's own printed mana ability is not this shape), or the
// line's own Cost$ has no ActivationShape (internal/cost) -- a chosen or
// SVar-sized Sac<.../Exile<.../Return<.../Exert<...>, a Discard<...> past the literal
// "N/Card" shape, a PayLife<...>/PayEnergy<...> past a literal positive
// integer (PayLife<X>/PayEnergy<X> and their own kin, an amount this port
// has no resolver to plug in here), a tapXType<...>/Return<...> naming a
// non-literal or non-positive N, an AddCounter<...>/SubCounter<...> naming a
// non-literal or negative N or a target past the self-reference shape (a
// chosen permanent, "OriginalHost," a type list), or an Untap/Mandatory/
// XMin token, each its own further payment primitive this port does not
// have, PORT-8/GO-7's "skip the whole line" applied to the cost itself
// rather than to the ability's own other params, an ActivationZone$ this
// port does not build (Command/Exile/Stack -- Graveyard and Hand are the
// only non-Battlefield zones supported), a Graveyard-zone ability naming
// any primitive but ExileFromGrave, a Hand-zone ability naming any
// primitive but Discard's or ExileFromHand's own self-reference shape, or a
// Battlefield-zone ability naming ExileFromGrave/Discard-self/
// ExileFromHand, a Planeswalker$ ability
// already activated once this turn (CR 606.3, Card.LoyaltyAbilityActivated),
// a Discard component the activating player's own hand
// cannot actually pay (fewer cards in hand than DiscardN), a PayLife
// component the activating player's own life cannot actually pay (CR 119.4:
// a life payment can never bring the payer below 0), a PayEnergy component
// the activating player's own energy-counter count cannot actually pay (CR
// 122.5's identical "never below 0" shape, checked the identical way), a
// SubCounter component the source's own count of that counter kind cannot
// actually pay (CostRemoveCounter.java's own "source.getCounters(cntrs) -
// amount >= 0" -- CR 121.5's identical "can't remove more than there are"
// shape; AddCounter has no such floor, "put 0 counters" trivially always
// pays, CostPutCounter.java's own "getAbilityAmount == 0" early return), a
// tapXType component whose own type spec this port cannot evaluate
// (tapTypeResolvable, taptype.go) or whose own candidate count -- the
// activating player's own untapped, type-matched battlefield permanents,
// the source itself excluded when the same cost also taps it through a
// separate plain T token -- falls short of TapTypeN, or a Return<N/Type>
// component whose own candidate count (returnTypeCandidates, returncost.go
// -- every one of the activating player's own type-matched battlefield
// permanents, tapped state irrelevant and the source never excluded, CR
// 602 places no such restriction on this primitive the way it does on
// tapXType) falls short of ReturnTypeN.
//
// Every feasibility check runs before anything is committed: a Tap-self
// cost checks CR 602.5b/302.6 first (already tapped, or summoning-sick
// without haste, DeclareCombatAttackers' own identical SummonSick/Haste
// check, attack.go, reused rather than re-derived), a Discard component
// checks the hand actually holds DiscardN cards, a PayLife component checks
// the player's own current life is at least PayLifeN, a PayEnergy
// component checks the player's own current Energy counter count is at
// least PayEnergyN, a SubCounter component checks the source's own current
// count of that counter kind is at least SubCounterN, a tapXType component
// checks its own candidate count
// (tapTypeCandidates, taptype.go) is at least TapTypeN, and a Return<N/Type>
// component checks its own candidate count (returnTypeCandidates,
// returncost.go) is at least ReturnTypeN. The mana half is paid through
// PayManaCost exactly as CastSpell's own is; only once that succeeds does
// the tap itself actually
// happen (Card.Tapped set, checkTapsTriggers fired), then a self-sac cost
// actually sacrifices the card (sacrificeCards, sacrificeeffect.go, reused
// wholesale -- CR 701.20's own "dies" trigger, RememberSacrificed$, and the
// batched Mode$ ChangesZoneAll firing all come free, exactly as they already
// do for Sacrifice's own "Self" branch), then a self-exile cost actually
// exiles the card (exileCards, exile.go, sacrificeCards's own sibling --
// CR 603.6d's own "leaves the battlefield" trigger and the identical batched
// Mode$ ChangesZoneAll both fire the same way), then a self-return cost
// actually returns the card to hand (returnCards, returncost.go,
// exileCards's own sibling at the identical destination-only difference),
// then a self-exert cost marks the card Exerted (Card.Exerted, card.go) and
// fires CR 701.42a's own trigger (checkExertedTriggers, exertcost.go) --
// unlike Sac/Exile/Return, exerting moves nothing and pays off nothing here
// at all: CR 701.42b's own "doesn't untap next turn" cost is entirely
// deferred to the exerting player's own next untapStep (turn.go), then a
// Discard component asks ChooseCardsToDiscard for exactly DiscardN
// cards and discards them
// (discardCards, discardeffect.go, reused wholesale the identical way), then
// a PayLife component subtracts PayLifeN from Player.Life and emits the
// identical LifeChanged event loseLifeEffect's own does (Player.payLife
// routes through the same loseLife machinery Java's own LifeLoseEffect
// uses, so this port's own single event kind for "life total changed"
// covers both causes) -- Mode$ LifeLost/LifeLostAll still fires no trigger
// check here, the identical omission loseLifeEffect's own doc comment
// already justifies (0 real T:Mode$ LifeLost/LifeLostAll lines corpus-wide)
// -- then a PayEnergy component subtracts PayEnergyN from the player's own
// Energy counter (Player.Counters, counters.go) and emits the identical
// CounterChanged event putCounterEffect's own does (Player.payEnergy routes
// through Player.loseEnergy/subtractCounter in Java, an ordinary counter
// removal with no trigger of its own -- unlike PayLife, Forge's own
// TriggerType has no PayEnergy mode at all to even skip), then a tapXType
// component asks ChoosePermanentsToTap for exactly TapTypeN of the
// candidates already found feasible and taps them (tapChosenPermanents,
// taptype.go, firing checkTapsTriggers per card the identical way a
// Tap-self cost's own single tap already does), then a Return<N/Type>
// component asks ChoosePermanentsToReturn for exactly ReturnTypeN of the
// candidates already found feasible and returns them to hand (returnCards,
// returncost.go, reused wholesale the identical way the self-return branch
// above already reuses it), then an AddCounter component puts AddCounterN
// counters of AddCounterType on the source (Card.Counters, counters.go) and
// emits the identical CounterChanged event putCounterEffect's own does, then
// a SubCounter component removes SubCounterN counters the identical way with
// a negative delta, then a SelfExileFromGrave component exiles the source
// from the graveyard (exileFromGraveyard, exilefromgrave.go), then a
// SelfDiscard component discards the source (discardCards, discardeffect.go
// -- Mode$ Discarded fires the identical way any other discard does, unlike
// SelfExileFromGrave/SelfExileFromHand's own no-trigger exile), then a
// SelfExileFromHand component exiles the source from the hand
// (exileFromHand, exilefromgrave.go) -- neither exile-from-a-non-battlefield-
// zone branch fires a trigger check at all, unlike the battlefield SelfExile
// branch above: CR 603.6d's own "leaves the battlefield" family does not
// apply to a card that was never on the battlefield -- CR 602.2g's own
// "costs are paid together" is
// approximated here as "check every cost for feasibility first, then commit
// each one, mana first, tap second, sacrifice third, exile fourth, return
// fifth, exert sixth, discard seventh, life eighth, energy ninth,
// tap-by-type tenth, return-by-type eleventh, add-counter twelfth,
// remove-counter thirteenth, exile-from-graveyard fourteenth, self-discard
// fifteenth, exile-from-hand last," so a failed mana payment never
// leaves the permanent tapped, sacrificed, exiled, returned, exerted, the
// player short a card, short life, short energy, wrongly gaining or missing
// counters, or another permanent
// wrongly tapped or returned for nothing, and a Tap-self cost never taps a
// permanent that has already left the battlefield. CR 601.2h's own "costs
// may be paid in any order" makes this ordering a free choice, not an
// approximation of a specific one Java's own CostPayment (a part-by-part,
// player-cancellable payment loop this port does not build) would make
// instead. A Planeswalker$ ability marks Card.LoyaltyAbilityActivated last,
// after every other part of its own cost has already committed -- CR 606.3
// restricts the whole ability regardless of which shape paid for it, so a
// "+0" AddCounter<0/...> ability sets this the identical way a "-N" one
// does.
//
// A successful activation pushes through pushTriggeredAbilities
// (trigger.go) with card's own controller as the sole entry -- resolving
// ValidTgts$ (targeting.go) and firing CR 115's own "becomes the target"
// check the identical way a triggered ability's own push already does,
// APNAP ordering a harmless no-op over the one player activating. No
// "activates an ability" trigger mode exists to check afterward
// (CR 603's own remaining gap, game-state.md); this port has none built to
// fire.
func (g *Game) ActivateAbility(pid PlayerID, card CardID, index int, controller PlayerController) bool {
	c := g.Card(card)
	if c.isDetained() {
		return false
	}
	ability, _, ok := c.abilityAt(index)
	if !ok {
		return false
	}
	// A:AB$ ManaReflected (Reflecting Pool, Exotic Orchard) is a mana
	// ability too, same as Mana -- never on the stack, ActivateManaAbility's
	// job, not this one's, even though the Produce walk it would need there
	// isn't ported yet either.
	if ability.Record != compile.Activated || ability.Name == "Mana" || ability.Name == "ManaReflected" {
		return false
	}
	_, isLoyaltyAbility := ability.Param("Planeswalker")
	if g.cantBeActivated(pid, c, false, isLoyaltyAbility) {
		return false
	}
	_, sorcerySpeed := ability.Param("SorcerySpeed")
	// A Mode$ CastWithFlash static with ValidSA$ Activated.Equip or
	// Activated.Loyalty lifts the sorcery-speed timing (SpellAbility.withFlash).
	if (isLoyaltyAbility || sorcerySpeed) && !g.canActSorcerySpeed(pid) && !g.activatesWithFlash(pid, card, ability) {
		return false
	}
	if !g.timingRestrictionsMet(pid, ability) || !g.activationLimitsMet(c, index, ability) || !g.otherRestrictionsMet(c, ability) {
		return false
	}
	fromGraveyard, fromHand := false, false
	switch zone, _ := ability.Param("ActivationZone"); zone {
	case "", "Battlefield":
		if !g.activatorValid(pid, c, ability) || c.Zone != Battlefield {
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
	case "Command":
		// An effect card's own ability (an emblem's): its controller activates
		// it from the Command zone.
		if !c.IsEffect || c.Zone != Command || c.Controller() != pid {
			return false
		}
	default:
		return false
	}
	if isLoyaltyAbility && c.LoyaltyAbilityActivated {
		return false
	}
	costText, ok := ability.Param("Cost")
	if !ok {
		return false
	}
	parsed := cost.Parse(costText)
	shape, ok := parsed.ActivationShape()
	if !ok {
		return false
	}
	// CostPutCounter.canPay: the source must be able to receive the counters
	// (Card.canReceiveCounters: not phased out, no Mode$ CantPutCounter).
	if shape.AddCounterType != "" && (c.IsPhasedOut() || g.cantPutCounter(CardEntity(card), CounterType(strings.ToUpper(shape.AddCounterType)))) {
		return false
	}
	nonBattlefield := fromGraveyard || fromHand
	if nonBattlefield && (shape.Tap || shape.SelfSac || shape.SelfExile || shape.SelfReturn || shape.SelfExert ||
		shape.DiscardN > 0 || shape.PayLifeN > 0 || shape.PayEnergyN > 0 || shape.TapTypeN > 0 || shape.ReturnTypeN > 0 ||
		shape.AddCounterType != "" || shape.SubCounterType != "" ||
		shape.SacTypeN > 0 || shape.ExileTypeN > 0 || shape.ExileGraveN > 0 || shape.DiscardTypeN > 0) {
		return false
	}
	if !fromGraveyard && shape.SelfExileFromGrave {
		return false
	}
	if !fromHand && (shape.SelfDiscard || shape.SelfExileFromHand) {
		return false
	}
	if fromGraveyard && (shape.SelfDiscard || shape.SelfExileFromHand) {
		return false
	}
	if fromHand && shape.SelfExileFromGrave {
		return false
	}
	if shape.Tap && (c.Tapped || c.isSick()) {
		return false
	}
	hand := g.Zone(Hand, pid).Cards()
	if shape.DiscardN > len(hand) {
		return false
	}
	if shape.SelfSac && g.cantSacrifice(c, false, nil) {
		return false
	}
	if shape.PayLifeN > g.Player(pid).Life || (shape.PayLifeN > 0 && g.cantPayLife(pid, false, causeActivated)) {
		return false
	}
	if shape.PayEnergyN > g.Player(pid).Counters.Count(Energy) {
		return false
	}
	if shape.SubCounterType != "" && shape.SubCounterN > c.Counters.Count(CounterType(strings.ToUpper(shape.SubCounterType))) {
		return false
	}
	// CostCollectEvidence.canPay (:42-48): the graveyard's total mana value.
	if shape.CollectEvidenceN > 0 && graveyardManaValue(g, g.Zone(Graveyard, pid).Cards()) < shape.CollectEvidenceN {
		return false
	}
	var tapCandidates, tapChosen []CardID
	if shape.TapTypeN > 0 {
		if !tapTypeResolvable(shape.TapTypeSpec) {
			return false
		}
		tapCandidates = tapTypeCandidates(g, pid, card, shape.Tap, shape.TapTypeSpec)
		if len(tapCandidates) < shape.TapTypeN {
			return false
		}
		if shape.TapTypeTotalPower > 0 {
			// Crew N: the controller picks any number of the candidates whose
			// total power reaches N (CostTapType's withTotalPowerGE); the pick
			// is made before anything is paid, so an illegal one costs nothing.
			if totalPower(g, tapCandidates) < shape.TapTypeTotalPower {
				return false
			}
			tapChosen = controller.ChooseCardsForEffect(g, pid, card, tapCandidates, 1, len(tapCandidates))
			if !isSubset(tapChosen, tapCandidates) || totalPower(g, tapChosen) < shape.TapTypeTotalPower {
				return false
			}
		}
	}
	var returnCandidates []CardID
	if shape.ReturnTypeN > 0 {
		returnCandidates = returnTypeCandidates(g, pid, card, shape.ReturnTypeSpec)
		if len(returnCandidates) < shape.ReturnTypeN {
			return false
		}
	}
	chosen, ok := g.chooseCostCards(controller, pid, card, shape)
	if !ok {
		return false
	}
	manaCost, err := mana.Parse(strings.Join(parsed.Mana, " "))
	if err != nil {
		return false
	}
	if _, ok := ability.Param("PowerUp"); ok && c.Def != nil && c.zoneEntered && c.zoneEntryTurn == g.turn {
		// CostAdjustment.java:225-228: a Power-up ability costs the host's own
		// mana cost less on the turn the host entered.
		manaCost = subtractManaCost(manaCost, c.Def.Faces[0].ManaCost)
	}
	apiType, ok := APIByName(ability.Name)
	if !ok {
		return false
	}
	activated := Ability{
		API: apiType, Source: card, Controller: pid,
		Params: ability, Amounts: c.abilityAmounts(ability), costPaid: true, activated: true,
	}
	// CR 602.2b / 601.2c: modes and targets are chosen before any cost is paid
	// (PlaySpellAbility.java:675-683 runs setupTargets ahead of CostPayment), so
	// an ability with no legal target costs nothing. Feasibility of every cost
	// part, and the cost cards' own choice, come first, as canPlay does.
	if !g.chooseAbilityTargets(controller, &activated) {
		return false
	}
	// The host before any cost moves it, for the activation record.
	before := *c
	x, paid := g.payManaCostX(pid, manaCost, controller)
	if !paid {
		return false
	}
	// PlaySpellAbility.java:682: the stack is frozen while the costs are paid,
	// so a trigger a cost fires (a sacrificed creature's dies trigger, a
	// discarded card's) waits for the ability and goes on the stack above it
	// (MagicStack.addAndUnfreeze, :132-171). The cast's hold (holdCastTriggers)
	// is that waiting list, begun once the mana is paid: no later part fails,
	// so the hold always ends below. An enclosing hold is set aside and put
	// back before the ability is pushed.
	outerHold, outerExits := g.holdingCastExits, g.castExits
	g.holdingCastExits, g.castExits = true, nil
	x.setOn(&activated)
	activated.paid.recorded = true
	activated.paid.mana, activated.paid.manaRecorded = x.sources, true
	if shape.Tap {
		c.Tapped = true
		g.checkTapsTriggers(controller, card, pid, false)
	}
	if shape.SelfSac {
		if g.Card(card).Zone == Battlefield {
			activated.paid.sacrificed = append(activated.paid.sacrificed, card)
		}
		sacrificeCardsFor(g, controller, &activated, []CardID{card}, false)
	}
	if shape.SelfExile {
		if g.Card(card).Zone == Battlefield {
			activated.paid.exiled = append(activated.paid.exiled, card)
		}
		exileCards(g, controller, []CardID{card})
	}
	if shape.SelfReturn {
		returnCards(g, controller, []CardID{card})
	}
	if shape.SelfExert {
		c.Exerted = true
		g.checkExertedTriggers(controller, card)
	}
	if shape.DiscardN > 0 {
		chosen := controller.ChooseCardsToDiscard(g, pid, hand, shape.DiscardN)
		activated.paid.discarded = append(activated.paid.discarded, chosen...)
		discardCards(g, controller, chosen, pid)
	}
	if shape.PayLifeN > 0 {
		g.Player(pid).Life -= shape.PayLifeN
		g.sink.Emit(Event{Kind: LifeChanged, Source: card, Target: PlayerEntity(pid), Amount: -int32(shape.PayLifeN)})
		g.noteLifeLost(controller, pid, shape.PayLifeN)
	}
	if shape.PayEnergyN > 0 {
		g.Player(pid).Counters.Add(Energy, -shape.PayEnergyN)
		emitCounterChanged(g, card, PlayerEntity(pid), Energy, -shape.PayEnergyN)
	}
	if shape.TapTypeTotalPower > 0 {
		tapChosenPermanents(g, controller, tapChosen)
	} else if shape.TapTypeN > 0 {
		chosen := controller.ChoosePermanentsToTap(g, pid, tapCandidates, shape.TapTypeN)
		tapChosenPermanents(g, controller, chosen)
	}
	if shape.ReturnTypeN > 0 {
		chosen := controller.ChoosePermanentsToReturn(g, pid, returnCandidates, shape.ReturnTypeN)
		returnCards(g, controller, chosen)
	}
	g.payCostCards(controller, pid, chosen, &activated)
	if shape.AddCounterType != "" {
		ct := CounterType(strings.ToUpper(shape.AddCounterType))
		c.Counters.Add(ct, shape.AddCounterN)
		emitCounterChanged(g, card, CardEntity(card), ct, shape.AddCounterN)
	}
	if shape.SubCounterType != "" {
		ct := CounterType(strings.ToUpper(shape.SubCounterType))
		c.Counters.Add(ct, -shape.SubCounterN)
		emitCounterChanged(g, card, CardEntity(card), ct, -shape.SubCounterN)
	}
	if shape.SelfExileFromGrave {
		activated.paid.exiled = append(activated.paid.exiled, card)
		exileFromGraveyard(g, controller, card)
	}
	if shape.SelfDiscard {
		activated.paid.discarded = append(activated.paid.discarded, card)
		discardCards(g, controller, []CardID{card}, pid)
	}
	if shape.SelfExileFromHand {
		activated.paid.exiled = append(activated.paid.exiled, card)
		exileFromHand(g, card)
	}
	if shape.CollectEvidenceN > 0 {
		// CostCollectEvidence's paymentOrder is 15 (:36-39), the sacrifices'
		// and exiles' own: it is paid with them, after the counters.
		g.collectEvidence(controller, pid, card, shape.CollectEvidenceN)
	}
	if isLoyaltyAbility {
		c.LoyaltyAbilityActivated = true
	}
	c.activations.note(index)
	g.recordActivation(&activated, &before)
	held := g.castExitMatches()
	g.holdingCastExits, g.castExits = outerHold, outerExits
	g.pushTriggeredAbilities(controller, []Ability{activated})
	// One batch, as unfreezeStack runs every waiting trigger at once: the
	// costs' triggers in firing order, then AbilityCast, then Cycled
	// (MagicStack.java:354-365).
	held = append(held, g.abilityCastMatches(&activated)...)
	if isCycling(ability) {
		pl := g.Player(pid)
		pl.CyclingThisTurn++
		held = append(held, g.cycledMatches(card, pid, pl.CyclingThisTurn == 1)...)
	}
	g.pushTriggeredAbilities(controller, held)
	return true
}

// activatorValid is SpellAbilityRestriction.checkActivatorRestrictions
// (SpellAbilityRestriction.java:331-346): pid may activate the A: line when
// Activator$ (default "You") matches them against the permanent's controller.
// A spec matchesPlayerSpec does not recognize refuses (GO-7).
func (g *Game) activatorValid(pid PlayerID, c *Card, ability *compile.Ability) bool {
	// IgnoreEffectCost$'s ability belongs to every player its static affects.
	if ability.Name == "InternalIgnoreEffect" {
		return g.ignoreActivatorValid(pid, c, ability)
	}
	spec, ok := ability.Param("Activator")
	if !ok {
		return c.Controller() == pid
	}
	matched, recognized := matchesPlayerSpec(g, pid, c.Controller(), c.ID, spec)
	return recognized && matched
}

// otherRestrictionsMet is SpellAbilityRestriction.checkOtherRestrictions'
// player-state and board checks for an A: or SP$ line of c
// (SpellAbilityRestriction.java:361-437,474-486): Activation$ (Threshold,
// Metalcraft, Delirium, Hellbent, Blessing, Solved), ActivationGameTypes$,
// ClassLevel$, IsPresent$ with PresentCompare$/
// PresentZone$, LifeTotal$ with LifeAmount$, and one CheckSVar$ with
// SVarCompare$. PresentPlayer$, IsPresent2$ and CheckSecondSVar$ are not read
// here, as Java's restriction does not read them. An Activation$ value this port
// does not know refuses (GO-7).
func (g *Game) otherRestrictionsMet(c *Card, ability *compile.Ability) bool {
	amounts := c.abilityAmounts(ability)
	you := c.Controller()
	if act, ok := ability.Param("Activation"); ok {
		var has bool
		switch act {
		case "Threshold":
			has = len(g.Zone(Graveyard, you).Cards()) >= 7
		case "Metalcraft":
			has = battlefieldArtifactCount(g, you) >= 3
		case "Delirium":
			has = graveyardCoreTypeCount(g, you) >= 4
		case "Hellbent":
			has = len(g.Zone(Hand, you).Cards()) == 0
		case "Blessing":
			has = g.Player(you).Blessing
		case "Solved":
			has = c.Solved
		default:
			return false
		}
		if !has {
			return false
		}
	}
	// ActivationGameTypes$ (SpellAbilityRestriction.java:526): some listed
	// variant must be applied to the game. A Game here is a plain constructed
	// game with none, so a line naming only Commander-family variants never
	// activates.
	if _, ok := ability.Param("ActivationGameTypes"); ok {
		return false
	}
	// ClassLevel$ compares the host's Class level, and no Class level is
	// tracked: refuse rather than ignore the restriction (GO-7).
	if _, ok := ability.Param("ClassLevel"); ok {
		return false
	}
	return isPresentMatches(g, c, amounts, ability, "IsPresent", "PresentCompare", "PresentDefined", "PresentZone", "") &&
		lifeTotalMatches(g, c, amounts, ability) &&
		checkSVarMatches(g, c, amounts, ability, "CheckSVar", "SVarCompare", "")
}

// activationLimitsMet is SpellAbilityRestriction.canPlay's
// ActivationLimit$/GameActivationLimit$ half (SpellAbilityRestriction.java:
// 583-598): fewer activations of the A: line at index this turn (this game)
// than the limit. The limit is an amount, X through the card's own SVars. One
// that cannot be resolved refuses the activation (GO-7).
func (g *Game) activationLimitsMet(c *Card, index int, ability *compile.Ability) bool {
	turn, game := c.activations.of(index)
	if _, ok := ability.Param("Exhaust"); ok && game >= 1 {
		// SpellAbilityRestriction.java:465-471: an Exhaust or Power-up ability
		// is limited to one activation a game (StaticAbilityAdditionalActivations
		// raises it; no corpus card does yet).
		return false
	}
	if _, ok := ability.Param("PowerUp"); ok && game >= 1 {
		return false
	}
	for _, k := range [...]struct {
		key   string
		count int
	}{{"ActivationLimit", turn}, {"GameActivationLimit", game}} {
		raw, ok := ability.Param(k.key)
		if !ok {
			continue
		}
		limit, ok := resolveNamedAmount(g, c.abilityAmounts(ability), c, raw)
		if !ok || k.count >= limit {
			return false
		}
	}
	return true
}

// timingRestrictionsMet is SpellAbilityRestriction.checkTimingRestrictions
// (SpellAbilityRestriction.java:282-315) for pid activating or casting an
// ability, shared by activated abilities, mana abilities and A:SP$ spells:
//
//   - PlayerTurn$ needs pid to be the active player; OpponentTurn$ needs
//     someone else to be (Player.isOpponentOf collapses to "not pid" with no
//     teams, the same reading matchesPlayerBase already makes).
//   - ActivationPhases$ needs the current step in the named set
//     (PhaseType.parseRange -- parsePhaseRange, phase.go). A value
//     parsePhaseRange cannot read refuses the attempt rather than ignoring
//     the restriction (GO-7).
//   - ActivationFirstCombat$ needs the turn's first combat: Java compares
//     the combats begun so far with 1 inside a combat step and 0 outside one
//     (PhaseHandler.inCombat is true from beginning of combat to end of combat).
//   - ActivationAfterBlockers$ (CR 506.7f) fails once the declare blockers
//     step was skipped for lack of attackers.
//
// None of the keys' values is read, as Java does not read them. Sneak's
// declare-blockers-only timing (SpellAbilityRestriction.java:312-316) is
// checked where the Sneak way to cast is offered (sneakAvailable, sneak.go).
// InstantSpeed$ is read nowhere either: Java reads it for a mana ability being
// activated while another cost is paid (SpellAbilityRestriction.java:564) --
// this port never interleaves a mana ability with a payment -- and as
// "has flash" for a SorcerySpeed$ line no real card pairs it with.
func (g *Game) timingRestrictionsMet(pid PlayerID, ability *compile.Ability) bool {
	if _, ok := ability.Param("PlayerTurn"); ok && pid != g.activePlayer {
		return false
	}
	if _, ok := ability.Param("OpponentTurn"); ok && pid == g.activePlayer {
		return false
	}
	if phases, ok := ability.Param("ActivationPhases"); ok {
		set, ok := parsePhaseRange(phases)
		if !ok || !set.has(g.activePhase) {
			return false
		}
	}
	if _, ok := ability.Param("ActivationFirstCombat"); ok {
		allowed := 0
		if g.activePhase.IsCombat() {
			allowed = 1
		}
		if g.combatsThisTurn > allowed {
			return false
		}
	}
	if _, ok := ability.Param("ActivationAfterBlockers"); ok && g.skipDamageSteps {
		return false
	}
	return true
}

// isCycling is SpellAbility.isCycling: the ability was built from a Cycling or
// TypeCycling keyword.
func isCycling(ability *compile.Ability) bool {
	name := keyword.Parse(ability.Keyword).Name
	return name == "Cycling" || name == "TypeCycling"
}

// subtractManaCost is ManaCostBeingPaid.subtractManaCost: each shard of sub
// that cost still holds comes off it, any other shard takes its mana value off
// the generic part, then sub's generic part does too (never below 0). An X
// shard of sub drops one X of cost.
func subtractManaCost(cost, sub mana.Cost) mana.Cost {
	shards := slices.Clone(cost.Shards())
	generic := cost.Generic()
	for _, sh := range sub.Shards() {
		if i := slices.Index(shards, sh); i >= 0 {
			shards = slices.Delete(shards, i, i+1)
		} else if !sh.IsX() {
			generic = max(0, generic-sh.CMC())
		}
	}
	return mana.FromShards(shards, max(0, generic-sub.Generic()))
}
