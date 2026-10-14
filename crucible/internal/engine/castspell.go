// Casting a spell: CR 601. Three shapes: nothing left to decide once a
// permanent (no target) or an Aura (one target) is chosen, and an Instant or
// Sorcery, whose own A:SP$ ability line can name modes and targets of its
// own (ADR-0018).

package engine

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/cost"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// castableAsPermanent reports whether c is a non-Aura permanent spell
// CastSpell's own no-target branch can cast: a creature, artifact,
// enchantment, planeswalker or Battle. Ported from CardState.java's
// getBasicSpells, which routes a permanent, non-Aura card to SpellPermanent
// -- an Aura routes to getAuraSpell() instead (castAura, below) since it
// needs a target chosen at cast time (CR 601.2c) that this shape has none
// of, and an Instant or Sorcery routes to castInstantOrSorcery (below)
// instead, whose own A:SP$ ability line may need either. A land is never a
// spell at all (CR 305.1) and is correctly excluded by not appearing in this
// list rather than by a special case.
func castableAsPermanent(c *Card) bool {
	t := c.Type()
	if t.HasSubtype("Aura") {
		return false
	}
	return t.Has(cardtype.Creature) || t.Has(cardtype.Artifact) || t.Has(cardtype.Enchantment) ||
		t.Has(cardtype.Planeswalker) || t.Has(cardtype.Battle)
}

// castableAsInstantOrSorcery reports whether c is an Instant or a Sorcery --
// CastSpell's own third branch (castInstantOrSorcery, below).
func castableAsInstantOrSorcery(c *Card) bool {
	t := c.Type()
	return t.Has(cardtype.Instant) || t.Has(cardtype.Sorcery)
}

// CastSpell is CR 601: pay the cost, then the spell becomes an object on the
// stack (CR 405.2, 601.2i) -- resolving is a separate step, ResolveStack.
// Timing (CR 307.1/601.3a, ADR-0019 Decision point 5) is canActSorcerySpeed
// for everything except an Instant: a permanent, an Aura and a Sorcery all
// need the active player, a main phase and an empty stack the same way
// PlayLand's own CR 305.3 check does; an Instant has none of those
// restrictions and may be cast whenever something asks (PassPriority,
// priority.go).
//
// A card outside pid's hand is castable only under a Layer 8 MayPlay$
// grant (mayPlayOption, game.go), which can also waive the mana cost
// (MayPlayWithoutManaCost$) or the sorcery-speed timing
// (MayPlayWithFlash$). A grant choice this port cannot make is recorded
// for TakePendingError and declines the cast.
//
// mayPlayOption runs before the timing check below, since its own
// WithFlash result feeds that check: an ambiguous-grant error (two live
// grants disagreeing, or a cost-changing grant beside a normal hand cast,
// mayplay_test.go's TestMayPlayHandChoiceFailsClosed) can fire even when
// the attempt would also have failed on timing alone. Both outcomes are
// "declined, not a bug" (GO-7); only the reason surfaced can differ, and
// only when a card the corpus never combines this way is scripted to.
//
// An Instant or Sorcery's own A:SP$ line adds its printed "cast only..."
// restrictions on top of that (timingRestrictionsMet, activateability.go:
// ActivationPhases$, PlayerTurn$, OpponentTurn$, ActivationFirstCombat$,
// ActivationAfterBlockers$), checked the same way for any cast from hand or
// a MayPlay$ grant. An effect that casts during its own resolution skips the
// sorcery-speed timing (CR 608.2g) but not these: Play offers such a spell
// only while they hold (playOptionsOf), Java's "extra timing restrictions
// still apply" (AbilityUtils.java:2944).
//
// Reports whether the spell was cast. false covers every legal-but-declined
// case: wrong timing, the card is neither in pid's hand nor granted,
// castableAsPermanent says no, or the cost could not be paid -- the same
// "declined by the rules, not a bug" contract PayManaCost and PlayLand
// already carry. A
// failed cost payment leaves the pool exactly as PayManaCost already
// guarantees, and the card never leaves hand.
//
// A successful cast fires SpellCast (ADR-0013's own schema has named this
// kind since M4, with nothing to emit it until now), then checks CR 603's
// own "whenever a player casts a spell" trigger (checkSpellCastTriggers,
// trigger.go) -- fired at cast time, not on resolution, the same place
// Java's own checkTriggerEffects call sits.
//
// A Room card casts as its left half (DoorLeft); CastRoomDoor picks either.
func (g *Game) CastSpell(pid PlayerID, card CardID, controller PlayerController) bool {
	return g.castFromHand(pid, card, DoorLeft, controller)
}

// CastRoomDoor is CastSpell for a Room card cast as its half d (CR 709.3,
// Card.setSplitStateToPlayAbility): the spell has that half's
// characteristics and cost, and that door unlocks as the Room enters
// (permanentEffect). Reports false for a card that is not a Room or for any
// d besides DoorLeft/DoorRight -- applyAction (priority.go) hands this an
// unvalidated controller answer, and doorView (room.go) indexes
// printed.Faces[d] directly, so an out-of-range d must be refused here
// rather than reaching that index (GO-7: a controller's wrong answer errors,
// never panics).
func (g *Game) CastRoomDoor(pid PlayerID, card CardID, d Door, controller PlayerController) bool {
	if d != DoorLeft && d != DoorRight {
		return false
	}
	if !isRoomDef(g.Card(card).Def) {
		return false
	}
	return g.castFromHand(pid, card, d, controller)
}

// castFromHand is CastSpell's hand and timing gates.
func (g *Game) castFromHand(pid PlayerID, card CardID, d Door, controller PlayerController) bool {
	c := g.Card(card)
	fromHand := c.Controller() == pid && c.Zone == Hand
	options := g.castOptions(pid, card, fromHand)
	var opts castOpts
	var way castOption
	if len(options) == 0 {
		// Flashback (CR 702.34a), foretell and Beam me up: the owner may cast
		// the card from their graveyard or exile for a keyword's cost.
		cost, ok := g.flashbackCost(pid, c)
		opts.fromKeyword = ok // Flashback and Beam me up exile the spell as it leaves the stack
		if !ok {
			cost, ok = g.foretellCost(pid, c)
			opts.foretold = ok
		}
		if !ok {
			// Escape (CR 702.138): its mana instead of the mana cost, its other
			// parts (exiling other graveyard cards) on top; the spell is not
			// exiled as it leaves the stack.
			cost, opts.extra, ok = g.escapeCost(pid, c)
			opts.hasExtra, opts.escape = ok, ok
		}
		if !ok {
			cost, opts.extra, ok = g.beamMeUpCost(pid, c)
			if !ok {
				return false
			}
			opts.hasExtra = true
			opts.fromKeyword = true
		}
		opts.altCost, opts.hasAltCost = cost, true
	} else {
		// Only the options playable under their own timing are offered. A
		// MayFlashCost keyword or a CastWithFlash line needing the targets or X
		// (flashNeedsInfo) keeps the cast open; the gate below settles it.
		_, _, flashByCost := mayFlashCost(c)
		_, _, flashByBehold := mayFlashBehold(c)
		flashByCost = flashByCost || flashByBehold
		var timed []castOption
		for _, o := range options {
			if c.Type().Has(cardtype.Instant) || o.flash || flashByCost || g.castsWithFlash(pid, card) ||
				g.canActSorcerySpeed(pid) || g.castsWithFlashNeedsInfo(pid, card) {
				timed = append(timed, o)
			}
		}
		var ok bool
		if way, ok = g.chooseCastOption(controller, pid, card, timed); !ok {
			return false
		}
		opts.withoutManaCost, opts.anyType, opts.anyColor = way.withoutMana, way.anyType, way.anyColor
		opts.snowAnyColor = way.snowAnyColor
		opts.sneak = way.sneak
		if way.hasAlt {
			opts.altCost, opts.hasAltCost = way.alt, true
		}
		if way.raise != "" {
			// MayPlay's RaiseCost$ was checked payable-in-shape when the grant was
			// made (applyOneContinuousMayPlay); its mana joins the cost, the rest
			// is the way's extra cost.
			uc, ok := parseUnlessCost(way.raise)
			if !ok {
				return false
			}
			if uc.hasMana {
				opts.raiseMana, opts.hasRaiseMana = uc.mana, true
			}
			if len(uc.parsed.Parts) > 0 {
				opts.extra, opts.hasExtra = uc, true
			}
		}
	}
	if c.Zone == Exile && c.IsFaceDown() {
		// A face-down exiled card (Heist) is cast face up; a cast that does
		// not happen leaves it as it was.
		blank := c.Def
		c.turnFaceUp()
		defer func() {
			if now := g.Card(card); now.Zone == Exile {
				now.faceUpDef, now.Def = now.Def, blank
			}
		}()
	}
	if !c.Type().Has(cardtype.Instant) && !way.flash && !g.castsWithFlash(pid, card) && !g.canActSorcerySpeed(pid) {
		// Out of timing: a CastWithFlash line for the chosen targets or X
		// decides later (castsWithFlashChosen), else MayFlashCost's optional cost
		// (GameActionUtil.java:514-517, instant speed once paid) if the caster
		// pays it.
		extra, text, ok := mayFlashCost(c)
		behold, beholdText, beholdOK := mayFlashBehold(c)
		switch {
		case g.castsWithFlashNeedsInfo(pid, card):
			opts.flashNeedsInfo = true
		case ok && controller.ConfirmPayCost(g, pid, cost.Parse(text), card):
			opts.flashCost, opts.hasFlashCost = extra, true
		case beholdOK && !opts.hasExtra && g.unlessPayable(pid, card, behold) &&
			controller.ConfirmPayCost(g, pid, cost.Parse(beholdText), card):
			// A non-mana MayFlashCost (Molten Exhale's Behold<1/Dragon>) is an
			// additional cost like any other the way the spell is cast adds.
			opts.extra, opts.hasExtra = behold, true
		default:
			return false
		}
	}
	if castableAsInstantOrSorcery(c) {
		if spell := firstSpellAbility(c); spell != nil && (!g.timingRestrictionsMet(pid, spell) || !g.otherRestrictionsMet(c, spell)) {
			return false
		}
	}
	opts.door = d
	if !g.castSpell(controller, pid, card, opts) {
		return false
	}
	if opts.fromKeyword {
		g.Card(card).flashbackCast = true
	}
	g.noteMayPlayUse(way)
	if way.replaceExile {
		g.Card(card).graveyardToExile = true
	}
	if way.sacAtCleanup {
		g.sacrificeAtCleanup(pid, card)
	}
	return true
}

// mayFlashCost is the plain mana cost of c's MayFlashCost keyword, with its
// script text (what ConfirmPayCost shows). ok is false without the keyword or
// when its cost is not plain mana (Behold<1/Dragon>, Molten Exhale): such a
// cost is not offered, never half-paid (GO-7).
func mayFlashCost(c *Card) (mc mana.Cost, text string, ok bool) {
	for _, line := range c.KeywordLines() {
		k := keyword.Parse(line)
		if k.Name != "MayFlashCost" || k.Details == "" {
			continue
		}
		if !cost.Parse(k.Details).IsPureMana() {
			return mana.Cost{}, "", false
		}
		mc, err := mana.Parse(k.Details)
		if err != nil || mc.CountX() > 0 {
			return mana.Cost{}, "", false
		}
		return mc, k.Details, true
	}
	return mana.Cost{}, "", false
}

// mayFlashBehold is the MayFlashCost keyword of c when its cost is a Behold
// and nothing else (Molten Exhale's Behold<1/Dragon>): the cost, with its
// script text for ConfirmPayCost. Any other non-mana shape is not offered
// (GO-7).
func mayFlashBehold(c *Card) (uc unlessCost, text string, ok bool) {
	for _, line := range c.KeywordLines() {
		k := keyword.Parse(line)
		if k.Name != "MayFlashCost" || k.Details == "" {
			continue
		}
		uc, parsed := parseUnlessCost(k.Details)
		if !parsed || !uc.revealBattlefield || uc.hasMana {
			return unlessCost{}, "", false
		}
		return uc, k.Details, true
	}
	return unlessCost{}, "", false
}

// withExtraMana is base plus extra, one more cost paid with it.
func withExtraMana(base, extra mana.Cost) mana.Cost {
	if base.IsNoCost() {
		return base
	}
	return mana.FromShards(append(append([]mana.Shard(nil), base.Shards()...), extra.Shards()...), base.Generic()+extra.Generic())
}

// sacrificeAtCleanup is MayFlashSac's trigger (CardFactoryUtil.java:2025-2038):
// a delayed trigger, registered as the spell is cast, that sacrifices the
// permanent it becomes at the beginning of the next cleanup step. Java
// builds it from strings; this port builds the same tree (PORT-2):
//
//	DB$ DelayedTrigger | Mode$ Phase | Phase$ Cleanup | RememberObjects$ Self
//	  Execute: DB$ SacrificeAll | Defined$ DelayTriggerRememberedLKI
func (g *Game) sacrificeAtCleanup(pid PlayerID, card CardID) {
	execute := &compile.Ability{Record: compile.SubAbility, Name: "SacrificeAll", Params: []vocab.Param{
		{Key: "DB", Value: "SacrificeAll"}, {Key: "Defined", Value: "DelayTriggerRememberedLKI"},
	}}
	trigger := &compile.Ability{Record: compile.SubAbility, Name: "DelayedTrigger", Params: []vocab.Param{
		{Key: "DB", Value: "DelayedTrigger"}, {Key: "Mode", Value: "Phase"}, {Key: "Phase", Value: "Cleanup"},
	}, Subs: []compile.SubRef{{Key: "Execute", Ability: execute}}}
	g.delayed = append(g.delayed, delayedTrigger{
		Trigger: trigger, Host: card, Controller: pid, Remembered: []EntityID{CardEntity(card)},
	})
}

// flashDecided is the decision a CastWithFlash line needing info deferred
// (opts.flashNeedsInfo): the spell, with its targets and the X announced so far,
// is castable at this timing. Always true when nothing was deferred.
func (g *Game) flashDecided(pid PlayerID, card CardID, opts castOpts, spell Ability) bool {
	if !opts.flashNeedsInfo {
		return true
	}
	if g.hasPreX {
		spell.xManaCostPaid, spell.hasXManaCostPaid = g.preX, true
	}
	return g.castsWithFlashChosen(pid, card, &spell)
}

// beamMeUpCost is Beam me up's graveyard cast (GameActionUtil.java:176-180):
// c's keyword cost plus returning a creature you control that can be beamed
// up. ok is false when pid does not own c in the graveyard, the keyword is
// absent or its cost is not plain mana, or no such creature exists.
func (g *Game) beamMeUpCost(pid PlayerID, c *Card) (mana.Cost, unlessCost, bool) {
	if c.Zone != Graveyard || c.Owner != pid || c.Def == nil {
		return mana.Cost{}, unlessCost{}, false
	}
	for _, line := range c.KeywordLines() {
		k := keyword.Parse(line)
		if k.Name != "Beam me up" || k.Details == "" {
			continue
		}
		if parsed := cost.Parse(k.Details); !parsed.IsPureMana() {
			return mana.Cost{}, unlessCost{}, false
		}
		mc, err := mana.Parse(k.Details)
		if err != nil || mc.CountX() > 0 {
			return mana.Cost{}, unlessCost{}, false
		}
		extra, ok := parseUnlessCost("Return<1/Creature.YouCtrl+canBeBeamedUp/creature you control>")
		if !ok || !g.unlessPayable(pid, c.ID, extra) {
			return mana.Cost{}, unlessCost{}, false
		}
		return mc, extra, true
	}
	return mana.Cost{}, unlessCost{}, false
}

// escapeCost is c's Escape keyword when pid may cast it from the graveyard
// (GameActionUtil.java:145-169, CR 702.138a): pid owns c and it is there. The
// keyword's cost replaces the mana cost (copyWithManaCostReplaced): its mana
// is what is paid instead, its other parts -- exiling other cards from the
// graveyard -- are paid with it. A cost parseUnlessCost does not read
// (Nethergoyf's ExileFromGrave<X/...>, Lunar Hatchling's Exile<1/Land>) or
// one whose parts pid cannot pay is not offered (GO-7).
func (g *Game) escapeCost(pid PlayerID, c *Card) (mana.Cost, unlessCost, bool) {
	if c.Zone != Graveyard || c.Owner != pid || c.Def == nil {
		return mana.Cost{}, unlessCost{}, false
	}
	for _, line := range c.KeywordLines() {
		k := keyword.Parse(line)
		if k.Name != "Escape" || k.Details == "" {
			continue
		}
		uc, ok := parseUnlessCost(k.Details)
		if !ok || !uc.hasMana {
			return mana.Cost{}, unlessCost{}, false
		}
		mc := uc.mana
		uc.hasMana, uc.mana, uc.manaTokens = false, mana.Cost{}, nil
		if !g.unlessPayable(pid, c.ID, uc) {
			return mana.Cost{}, unlessCost{}, false
		}
		return mc, uc, true
	}
	return mana.Cost{}, unlessCost{}, false
}

// flashbackCost is the mana cost of c's Flashback keyword when pid may cast it
// from the graveyard: pid owns it and it is there. A Flashback whose cost is
// not a plain mana cost (Sac<..>, PayLife<..>, a non-mana part) is not offered
// (GO-7).
func (g *Game) flashbackCost(pid PlayerID, c *Card) (mana.Cost, bool) {
	if c.Zone != Graveyard || c.Owner != pid || c.Def == nil {
		return mana.Cost{}, false
	}
	for _, line := range c.KeywordLines() {
		k := keyword.Parse(line)
		if k.Name != "Flashback" {
			continue
		}
		text := k.Details
		if text == "" {
			// A bare Flashback (granted by AddKeyword$) is cast for the card's
			// own mana cost (GameActionUtil.getGraveyardSpellByKeyword).
			if c.Def.Faces[0].ManaCost.IsNoCost() {
				return mana.Cost{}, false
			}
			return c.Def.Faces[0].ManaCost, true
		}
		if parsed := cost.Parse(text); !parsed.IsPureMana() {
			return mana.Cost{}, false
		}
		mc, err := mana.Parse(text)
		if err != nil || mc.CountX() > 0 {
			return mana.Cost{}, false
		}
		return mc, true
	}
	return mana.Cost{}, false
}

// castOpts is how an effect's "cast it" differs from casting it normally
// (PlayEffect.java's tgtSA adjustments before playSaFromPlayEffect).
type castOpts struct {
	// withoutManaCost is WithoutManaCost$ (SpellAbility.copyWithNoManaCost):
	// no mana is paid, and an X in the cost is 0, so ChoosePayX is never
	// asked (CR 107.3b).
	withoutManaCost bool
	// door is the half a Room card is cast as; ignored for anything else.
	door Door
	// altCost, when hasAltCost, is paid instead of the printed mana cost (a
	// flashback cost, CR 702.34a).
	altCost    mana.Cost
	hasAltCost bool
	// kickers are the kicker costs chosen (kicker1, kicker2 bits), paid on top
	// of the cost (CR 601.2b, 702.33a).
	kickers uint8
	// optional are the Mode$ OptionalCost and Teamwork costs chosen
	// (optionalcost.go): their mana joins the cost, their other parts are paid
	// once it is.
	optional optionalChoice
	// extra, when hasExtra, is a cost the way the spell is cast adds to the
	// printed one: Beam me up's returned creature.
	extra    unlessCost
	hasExtra bool
	// flashCost, when hasFlashCost, is MayFlashCost's mana, paid on top of the
	// cost for casting out of timing.
	flashCost    mana.Cost
	hasFlashCost bool
	// flashNeedsInfo marks a cast out of timing that only a CastWithFlash line
	// naming IsTargeting or XCost allows: castsWithFlashChosen decides once the
	// targets and X are chosen.
	flashNeedsInfo bool
	// anyType is MayPlayIgnoreType$: mana of any type pays the cost.
	anyType bool
	// anyColor is MayPlayIgnoreColor$: mana of any type pays a colored part of
	// the cost, but a {C} part still needs colorless mana.
	anyColor bool
	// snowAnyColor is MayPlaySnowIgnoreColor$: snow mana pays a colored part
	// of the cost whatever its color.
	snowAnyColor bool
	// raiseMana, when hasRaiseMana, is the mana part of MayPlay's RaiseCost$,
	// paid on top of the cost (its other parts ride extra).
	raiseMana    mana.Cost
	hasRaiseMana bool
	// fromKeyword marks a cast from a graveyard or exile through a keyword's
	// own cost (Flashback, Foretell, Beam me up), exiled as it leaves the stack.
	fromKeyword bool
	// sneak is a cast for the Sneak cost (sneak.go): altCost is its mana, and
	// an unblocked attacker returns to hand once the mana is paid.
	sneak bool
	// foretold is a cast from exile for the card's Foretell cost
	// (AlternativeCost.Foretold, GameActionUtil.java:205-225): altCost is that
	// cost, and the cast records it (castOptional.foretold).
	foretold bool
	// escape is a cast from the graveyard for the card's Escape cost
	// (AlternativeCost.Escape, GameActionUtil.java:145-169): altCost is its
	// mana, extra its other parts, and the cast records it
	// (castOptional.escaped).
	escape bool
}

// castSpell is CastSpell past its timing and hand gates: the three spell
// shapes (Aura, Instant/Sorcery, other permanent), each choosing, paying,
// then putting the card on the stack. It is the one cast path, from any
// zone: CastSpell calls it for a card in hand at sorcery speed, and an
// effect that casts during its resolution (Play, playeffect.go; Discover's
// castWithoutPaying) calls it with timing ignored (CR 608.2g). Reports false
// for every legal-but-declined case the three branches name.
func (g *Game) castSpell(controller PlayerController, pid PlayerID, card CardID, opts castOpts) bool {
	if g.cantBeCast(pid, card) || (opts.sneak && !g.sneakPayable(pid, card)) {
		return false
	}
	// Kicker is chosen as the spell is announced; the card carries the choice
	// (Card.kicker) from the stack onto the battlefield, and a cast that fails
	// forgets it.
	g.Card(card).castFrom = g.Card(card).Zone
	g.Card(card).castBy = pid
	g.Card(card).wasCast = true
	// The OptionalCost statics come first in Java's list, the keyword costs
	// after (GameActionUtil.java:442-523); Teamwork is one of those, Kicker is
	// chooseKicker's.
	optional, err := g.chooseOptionalCosts(controller, pid, card)
	if err != nil {
		g.recordPendingError(err)
		g.Card(card).wasCast = false
		return false
	}
	opts.optional = optional
	g.Card(card).castOpt = castOptional{kinds: optional.kinds, foretold: opts.foretold, escaped: opts.escape}
	opts.kickers = g.chooseKicker(controller, pid, g.Card(card))
	g.Card(card).kicker = opts.kickers
	g.Card(card).sneaked = opts.sneak
	if !g.castSpellChosen(controller, pid, card, opts) {
		g.Card(card).kicker = 0
		g.Card(card).castOpt = castOptional{}
		g.Card(card).wasCast = false
		g.Card(card).sneaked, g.Card(card).sneakDefender = false, NoEntity
		return false
	}
	return true
}

// castSpellChosen is castSpell once the kicker choice is made.
func (g *Game) castSpellChosen(controller PlayerController, pid PlayerID, card CardID, opts castOpts) bool {
	g.castPending = card
	g.holdingCastExits = true
	defer func() {
		g.castPending, g.preX, g.hasPreX = NoCard, 0, false
		// Every graveyard exit a cast makes comes after its payment succeeded,
		// so checkSpellCastTriggers has always taken castExits by now.
		g.holdingCastExits, g.castExits = false, nil
	}()
	c := g.Card(card)
	if c.Type().HasSubtype("Aura") {
		return g.castAura(pid, card, c, controller, opts)
	}
	if castableAsInstantOrSorcery(c) {
		return g.castInstantOrSorcery(pid, card, c, controller, opts)
	}
	if !castableAsPermanent(c) {
		return false
	}
	if opts.flashNeedsInfo && (!g.announceX(pid, c, controller, opts) ||
		!g.flashDecided(pid, card, opts, Ability{Source: card, Controller: pid, spell: true})) {
		return false
	}
	extra, hasExtra, ok := g.castExtraCost(pid, c, firstSpellAbility(c), opts)
	if !ok || !g.optionalPartsPayable(pid, card, opts.optional) {
		return false
	}
	room := isRoomDef(c.Def)
	if room {
		c.castAsDoor(opts.door)
	}
	x, paid := g.payCastCost(pid, c, controller, opts)
	if !paid {
		if room {
			c.undoCastAsDoor()
		}
		return false
	}
	if hasExtra {
		g.payUnlessParts(controller, &Ability{Source: card, Controller: pid}, pid, extra)
	}
	if opts.sneak && !g.paySneakReturn(controller, pid, card) {
		return false
	}
	if !g.payOptionalParts(controller, pid, card, opts.optional) {
		return false
	}
	g.putSpellOnStack(card, pid)
	api := APIPermanentNoncreature
	if c.Type().Has(cardtype.Creature) {
		api = APIPermanentCreature
	}
	cast := Ability{API: api, Source: card, Controller: pid, spell: true}
	x.setOn(&cast)
	g.Card(card).castX = x.value
	g.PushAbility(cast)
	g.sink.Emit(Event{Kind: SpellCast, Phase: g.activePhase, Active: g.activePlayer, Actor: pid, Turn: uint16(g.turn), Source: card})
	g.recordSpellCast(pid, card)
	g.checkSpellCastTriggers(controller, card, pid, g.castExitMatches())
	return true
}

// castCost is the mana cost pid pays to cast c under opts: the printed (or
// alternative) cost with the chosen kicker costs added, then every cost
// modification applied (spellCost).
func (g *Game) castCost(pid PlayerID, c *Card, opts castOpts) (mana.Cost, bool) {
	base := c.Def.Faces[0].ManaCost
	switch {
	case opts.withoutManaCost:
		// A free cast still pays the optional costs chosen (CR 118.9d).
		base = mana.Cost{}
	case opts.hasAltCost:
		base = opts.altCost
	}
	base = withKicker(c, opts.kickers, base)
	base = opts.optional.withMana(base)
	if opts.hasFlashCost {
		base = withExtraMana(base, opts.flashCost)
	}
	if opts.hasRaiseMana {
		base = withExtraMana(base, opts.raiseMana)
	}
	total, ok := g.spellCost(pid, c.ID, base)
	total = opts.optional.reduced(total)
	switch {
	case opts.anyType:
		total = anyTypeCost(total)
	case opts.anyColor:
		total = anyColorCost(total)
	}
	return total, ok
}

// paysNoMana reports a free cast (WithoutManaCost$) that adds no mana either:
// no kicker, MayFlashCost, RaiseCost$ or optional cost with mana (CR 118.9d).
func (opts castOpts) paysNoMana() bool {
	return opts.withoutManaCost && opts.kickers == 0 && !opts.hasFlashCost && !opts.hasRaiseMana && !opts.optional.hasMana
}

// announceX is CR 601.2b: a spell with {X} in its cost has its X announced
// before its targets are chosen, so a restriction on them can name it
// (Repeal's cmcEQX). payManaCostX takes the announced value instead of asking
// again. False when the caster declines to announce one.
func (g *Game) announceX(pid PlayerID, c *Card, controller PlayerController, opts castOpts) bool {
	return g.announceXFor(pid, c, controller, opts, nil)
}

// announceXFor is announceX for the spell line, whose own Cost$ may name X in
// a part past mana (`Discard<X/Card>`, `PayLife<X>`): PlaySpellAbility.
// announceValuesLikeX (PlaySpellAbility.java:766-782) asks once when the raw
// cost has an X anywhere and the card's SVar X is Count$xPaid (or absent).
// The controller's answer is held to what the non-mana parts can pay, as
// PlayerControllerHuman.announceRequirements bounds it (:376-381); the mana X,
// if there is one, shares the value.
func (g *Game) announceXFor(pid PlayerID, c *Card, controller PlayerController, opts castOpts, line *compile.Ability) bool {
	if opts.paysNoMana() {
		return true
	}
	total, ok := g.castCost(pid, c, opts)
	if !ok {
		return true
	}
	partsText := ""
	if line != nil {
		if text, has := line.Param("Cost"); has && costNamesX(text) && xIsPaid(c) {
			partsText = text
		}
	}
	if total.CountX() == 0 && partsText == "" {
		return true
	}
	n := controller.ChoosePayX(g, pid, total)
	if n < 0 {
		return false
	}
	if partsText != "" {
		n = min(n, g.nonManaXMax(pid, c.ID, partsText))
	}
	g.preX, g.hasPreX = n, true
	return true
}

// xIsPaid reports whether c's SVar X is Count$xPaid or absent: the only X that
// is announced (PlaySpellAbility.java:768-771). Any other X (a count of
// targets, of lands) is computed, never asked.
func xIsPaid(c *Card) bool {
	amt, ok := c.Def.Faces[0].Amounts["x"]
	return !ok || (amt.Kind == expr.Expression && amt.Head == "Count" && strings.EqualFold(amt.Body, "xPaid"))
}

// payCastCost pays c's printed mana cost for pid (CR 601.2g-h), or nothing
// under opts.withoutManaCost, and reports the X it announced for the caller
// to record on the spell (xAnnounced.setOn, manapay.go). A WithoutManaCost$
// cast announces none: its cost has no X part left, and
// PlaySpellAbility.announceValuesLikeX leaves xManaCostPaid null then.
func (g *Game) payCastCost(pid PlayerID, c *Card, controller PlayerController, opts castOpts) (xAnnounced, bool) {
	// The X is announced here, so from the return on Count$xPaid reads it.
	defer func() { g.castPending = NoCard }()
	c.castManaSpent, c.castPaid, c.castSnow = 0, [5]int{}, 0
	if opts.paysNoMana() {
		return xAnnounced{}, true
	}
	total, ok := g.castCost(pid, c, opts)
	if !ok {
		return xAnnounced{}, false
	}
	total, assist := g.assistCost(controller, pid, c, total)
	total, waterbent := g.waterbendCast(controller, pid, c.ID, total, opts.optional.waterbendN)
	g.snowAnyColor = opts.snowAnyColor
	x, paid := g.payManaCostX(pid, total, controller)
	g.snowAnyColor = false
	if paid {
		assist.settle(g, controller)
		tapChosenPermanents(g, controller, waterbent)
		// SpellAbility.getTotalManaSpent: one per mana paid. Phyrexian shards
		// paid with life are counted too, a small overcount no card reads.
		c.castManaSpent = len(total.Shards()) + total.Generic() + x.value*total.CountX()
		// The mana another player paid (assist) is not in x.paid: Converge
		// and Adamant read only the caster's.
		c.castPaid, c.castSnow = x.paid, x.snow
	}
	return x, paid
}

// setOn records x on a as the X its cost was paid with; announced false
// leaves Java's null.
func (x xAnnounced) setOn(a *Ability) {
	a.xManaCostPaid, a.hasXManaCostPaid = x.value, x.announced
}

// putSpellOnStack moves card to the stack under pid and makes pid its
// controller: CR 110.2's "the player who cast it", MagicStack.add's
// source.setController(activator). Move alone keeps whatever controller the
// card had, which for an opponent's card cast by an effect (Play's
// Controller$ You over an opponent's exiled card) is the opponent. A spell
// cast from a graveyard (flashback, escape, a Play grant) leaves it: its
// graveyard-origin triggers wait in castExits for the SpellCast batch.
func (g *Game) putSpellOnStack(card CardID, pid PlayerID) {
	g.Card(card).castLast = g.castSnapshotNow()
	from := g.Card(card).Zone
	g.Move(card, Stack, pid)
	g.Card(card).controller = pid
	g.noteGraveyardExit(card, from)
}

// castAura is CastSpell's own Aura branch (CardState.java's getAuraSpell,
// the "SP$ Attach" ability it builds, and AttachEffect.java's own resolve).
// CR 601.2c puts choosing a target before paying the cost, the one thing
// that makes an Aura's cast different from castableAsPermanent's own
// no-decision case: a target is picked here, carried on the pushed Ability
// (Target, ability.go) to wherever attachEffect (below) reads it back at
// resolution.
//
// Reports false for every legal-but-declined case castableAsPermanent's own
// CastSpell branch already has, plus two more: enchantSpec finds nothing
// checkable (an "Enchant Player"/"Enchant Opponent" Aura, its own doc
// comment's gap -- this port cannot tell a legal host from an illegal one
// without a card-type spec to check), or the battlefield has no legal host
// at all (CR 601.2c: a spell requiring a target that has none is illegal to
// cast, not one cast with nothing to point at).
//
// checkBecomesTargetTriggers (trigger.go) runs after the push too: an Aura's
// own attach target is CR 115's "becomes the target of a spell" exactly as
// much as a triggered ability's chosen target is (pushTriggeredAbilities's
// own identical call, trigger.go) -- Illusionary Servant's real "when
// CARDNAME becomes the target of a spell or ability, sacrifice it" fires
// off an opponent Auraing it exactly as much as off a triggered ability
// targeting it, and this is the only cast-time path this port has today
// that could ever reach it (a targeted Instant/Sorcery is not built yet,
// checkBecomesTargetTriggers' own doc comment).
func (g *Game) castAura(pid PlayerID, card CardID, c *Card, controller PlayerController, opts castOpts) bool {
	if kind, ok := enchantPlayerSpec(c); ok {
		return g.castPlayerAura(pid, card, c, controller, opts, kind)
	}
	spec, ok := enchantSpec(c)
	if !ok {
		return false
	}
	eligible := g.enchantTargets(spec, pid, card, true)
	if len(eligible) == 0 {
		return false
	}
	target := eligible[0]
	if len(eligible) > 1 {
		target = controller.ChooseEnchantTarget(g, pid, card, eligible)
	}
	if opts.flashNeedsInfo && (!g.announceX(pid, c, controller, opts) ||
		!g.flashDecided(pid, card, opts, Ability{Source: card, Controller: pid, Target: target, spell: true})) {
		return false
	}
	if !g.optionalPartsPayable(pid, card, opts.optional) {
		return false
	}
	x, paid := g.payCastCost(pid, c, controller, opts)
	if !paid {
		return false
	}
	if !g.payOptionalParts(controller, pid, card, opts.optional) {
		return false
	}
	g.putSpellOnStack(card, pid)
	cast := Ability{API: APIAttach, Source: card, Controller: pid, Target: target, spell: true}
	x.setOn(&cast)
	g.Card(card).castX = x.value
	g.PushAbility(cast)
	g.sink.Emit(Event{Kind: SpellCast, Phase: g.activePhase, Active: g.activePlayer, Actor: pid, Turn: uint16(g.turn), Source: card})
	g.recordSpellCast(pid, card)
	g.checkSpellCastTriggers(controller, card, pid, g.castExitMatches())
	tgts := []EntityID{CardEntity(target)}
	matches := g.checkBecomesTargetTriggers(tgts, true, pid)
	matches = append(matches, g.checkWardTriggers(tgts, card, pid)...)
	g.pushTriggeredAbilities(controller, matches)
	return true
}

// castPlayerAura is castAura for an Aura that enchants a player (CR 303.4h,
// "Enchant player"/"Enchant opponent": the Curses). The player is its target
// (CR 601.2c), carried on Ability.Targets like any other player target, so
// hexproof, shroud, protection and "can't be the target" statics refuse it
// the way they refuse a spell aimed at that player, and the spell fizzles if
// the player is no longer a legal target when it resolves. The choice is one
// ChooseTargets question over the legal players, skipped when only one is
// left. Java's own check is GameEntity.cantBeEnchantedByMsg -- isValid of the
// Enchant keyword's type ("Player" or "Opponent") against the player.
func (g *Game) castPlayerAura(pid PlayerID, card CardID, c *Card, controller PlayerController, opts castOpts, kind string) bool {
	var eligible []EntityID
	for _, cand := range g.Players() {
		if g.Player(cand).Lost {
			continue
		}
		if matched, _ := matchesPlayerSpec(g, cand, pid, card, kind); !matched {
			continue
		}
		if playerCantBeTargetedBy(g, cand, pid, card, targetAsk{kind: causeSpell, enchant: kind, casting: true}) {
			continue
		}
		eligible = append(eligible, PlayerEntity(cand))
	}
	if len(eligible) == 0 {
		return false
	}
	target := eligible[0]
	if len(eligible) > 1 {
		chosen := controller.ChooseTargets(g, pid, eligible, 1, 1)
		if len(chosen) != 1 {
			return false
		}
		target = chosen[0]
	}
	if opts.flashNeedsInfo && (!g.announceX(pid, c, controller, opts) ||
		!g.flashDecided(pid, card, opts, Ability{Source: card, Controller: pid, Targets: []EntityID{target}, spell: true})) {
		return false
	}
	if !g.optionalPartsPayable(pid, card, opts.optional) {
		return false
	}
	x, paid := g.payCastCost(pid, c, controller, opts)
	if !paid {
		return false
	}
	if !g.payOptionalParts(controller, pid, card, opts.optional) {
		return false
	}
	g.putSpellOnStack(card, pid)
	cast := Ability{API: APIAttach, Source: card, Controller: pid, Targets: []EntityID{target}, spell: true}
	x.setOn(&cast)
	g.Card(card).castX = x.value
	g.PushAbility(cast)
	g.sink.Emit(Event{Kind: SpellCast, Phase: g.activePhase, Active: g.activePlayer, Actor: pid, Turn: uint16(g.turn), Source: card})
	g.recordSpellCast(pid, card)
	g.checkSpellCastTriggers(controller, card, pid, g.castExitMatches())
	tgts := []EntityID{target}
	matches := g.checkBecomesTargetTriggers(tgts, true, pid)
	matches = append(matches, g.checkWardTriggers(tgts, card, pid)...)
	g.pushTriggeredAbilities(controller, matches)
	return true
}

// castInstantOrSorcery is CastSpell's own Instant/Sorcery branch (ADR-0018):
// CR 601.2b/601.2c's own "choose modes, then targets" for the one real
// A:SP$ ability line the card carries (Def.Faces[0].Abilities, the identical
// field ActivateAbility already reads for its own A:AB$/A:T$ lines,
// activateability.go), reusing chooseCharmModes and resolveTargets the same
// way pushTriggeredAbilities already does for a triggered ability
// (trigger.go). Cost payment sits between the two and PushAbility here,
// which pushTriggeredAbilities' own all-in-one shape has no room for -- its
// own caller, an already-paid triggered or activated ability, never needs
// it.
//
// Reports false for every legal-but-declined case CastSpell's own other two
// branches already have: no A:SP$ line this port's compiler gave the card
// (PORT-8, not this port's job to guess one), a Charm whose modes the
// controller declined (chooseCharmModes' own false, the identical
// "ability never goes on the stack" case pushTriggeredAbilities already
// treats as a skip), no legal target (CR 601.2c), or the cost could not be
// paid.
func (g *Game) castInstantOrSorcery(pid PlayerID, card CardID, c *Card, controller PlayerController, opts castOpts) bool {
	spellAbility := firstSpellAbility(c)
	if spellAbility == nil {
		return false
	}
	apiType, ok := APIByName(spellAbility.Name)
	if !ok {
		return false
	}
	a := Ability{API: apiType, Source: card, Controller: pid, Params: spellAbility, Amounts: c.Def.Faces[0].Amounts, spell: true, casting: true}
	if !g.announceXFor(pid, c, controller, opts, spellAbility) {
		return false
	}
	if a.API == APICharm {
		modesOK, err := g.chooseCharmModes(controller, &a)
		if err != nil {
			a.modesErr = err
		} else if !modesOK {
			return false
		}
	}
	if !g.resolveTargets(controller, &a) || !g.resolveChainTargets(controller, &a) {
		return false
	}
	// Targets are chosen: from here the spell is "on the stack" for an
	// EffectZone$ Stack CantTarget line (targetAsk.casting).
	a.casting = false
	for i := range a.Modes {
		a.Modes[i].casting = false
	}
	if !g.flashDecided(pid, card, opts, a) {
		return false
	}
	extra, hasExtra, ok := g.castExtraCost(pid, c, spellAbility, opts)
	if !ok || !g.optionalPartsPayable(pid, card, opts.optional) {
		return false
	}
	x, paid := g.payCastCost(pid, c, controller, opts)
	if !paid {
		return false
	}
	if !x.announced && g.hasPreX {
		// An X announced for a cost part past mana (announceXFor) has no mana
		// shard to carry it; the spell still records it for Count$xPaid.
		x = xAnnounced{value: g.preX, announced: true}
	}
	if hasExtra {
		// The spell's additional cost is its own paid list: Sacrificed$ and
		// Discarded$ read it as the spell resolves (SpellAbility.getPaidList).
		g.payUnlessPartsInto(controller, &a, pid, extra, &a.paid)
	}
	a.paid.recorded = true
	if opts.sneak && !g.paySneakReturn(controller, pid, card) {
		return false
	}
	if !g.payOptionalParts(controller, pid, card, opts.optional) {
		return false
	}
	x.setOn(&a)
	g.putSpellOnStack(card, pid)
	g.Card(card).castX = x.value
	g.PushAbility(a)
	g.sink.Emit(Event{Kind: SpellCast, Phase: g.activePhase, Active: g.activePlayer, Actor: pid, Turn: uint16(g.turn), Source: card})
	g.recordSpellCast(pid, card)
	g.checkSpellCastTriggers(controller, card, pid, g.castExitMatches())
	tgts := allTargetsOf(a)
	matches := g.checkBecomesTargetTriggers(tgts, false, pid)
	matches = append(matches, g.checkWardTriggers(tgts, card, pid)...)
	g.pushTriggeredAbilities(controller, matches)
	return true
}

// spellAdditionalCost reads the A:SP$ line's Cost$ as the spell's additional
// cost (CR 118.8): its mana is the card's own (paid with the rest), its other
// parts are paid once the mana is. Forge writes the printed mana cost into Cost$
// beside the additional parts, so a line whose mana differs from the card's (an
// alternative or cleave cost) is not one this port reads, and neither is a shape
// parseUnlessCost does not: ok false means the spell is not cast, never cast
// free of the cost (GO-7). has is false for a line with no Cost$.
func (g *Game) spellAdditionalCost(pid PlayerID, c *Card, line *compile.Ability) (extra unlessCost, has, ok bool) {
	costText, hasCost := line.Param("Cost")
	if !hasCost {
		return unlessCost{}, false, true
	}
	uc, parsed := parseUnlessCostX(costText, g.preX, g.hasPreX)
	if !parsed || !g.unlessPayable(pid, c.ID, uc) || (uc.hasMana && !uc.mana.Equal(c.Def.Faces[0].ManaCost)) {
		return unlessCost{}, false, false
	}
	return uc, true, true
}

// castExtraCost is the additional cost casting c under opts pays beyond its
// mana: the A:SP$ line's own (spellAdditionalCost, line may be nil) or the one
// the way it is cast adds. ok is false when either cannot be paid, or both
// exist (no corpus card has both, and paying two is not modelled).
func (g *Game) castExtraCost(pid PlayerID, c *Card, line *compile.Ability, opts castOpts) (extra unlessCost, has, ok bool) {
	if line != nil {
		if extra, has, ok = g.spellAdditionalCost(pid, c, line); !ok {
			return unlessCost{}, false, false
		}
	}
	if !opts.hasExtra {
		return extra, has, true
	}
	if has || !g.unlessPayable(pid, c.ID, opts.extra) {
		return unlessCost{}, false, false
	}
	return opts.extra, true, true
}

// firstSpellAbility is c's first A:SP$ line (Def.Faces[0].Abilities), or
// nil when it has none -- the one spell castInstantOrSorcery casts.
func firstSpellAbility(c *Card) *compile.Ability {
	for _, ab := range c.Def.Faces[0].Abilities {
		if ab.Record == compile.Spell {
			return ab
		}
	}
	return nil
}

// enchantTargets is every battlefield permanent -- and, when the Enchant type
// names a zone (enchantsOutsidePlay), every graveyard card -- across every
// player, that spec (self's own Enchant restriction, enchantSpec) matches, and that does
// not refuse self outright (cardCantBeTargetedBy, staticability.go -- CR
// 702.11b/702.16e/702.18a's own Hexproof/Protection/Shroud gate, a separate
// question from the card-type restriction spec itself checks) -- CR
// 601.2c's legal-target set for casting self as an Aura. Matches' own
// source parameter is self, the same "the enchantment's own id, not the
// host's" convention cleanupDanglingAttachments (action.go) already uses
// when re-checking an attached Aura's own restriction after the fact.
func (g *Game) enchantTargets(spec valid.Spec, controller PlayerID, self CardID, casting bool) []CardID {
	var eligible []CardID
	ask := targetAsk{kind: causeSpell, enchant: spec.String(), casting: casting}
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if Matches(g, g.Card(id), spec, controller, self) && !cardCantBeTargetedBy(g, g.Card(id), controller, self, ask) {
				eligible = append(eligible, id)
			}
		}
	}
	if !enchantsOutsidePlay(spec) {
		return eligible
	}
	// The Aura spell is "TgtZone$ Battlefield,Graveyard" (CardState.java:665)
	// and a host outside play is legal only when the Enchant type names a zone
	// (Card.java:6986): Animate Dead's "Creature.inZoneGraveyard". A spec
	// naming any other zone matches nothing in either searched zone.
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Graveyard, pid).Cards() {
			if id != self && Matches(g, g.Card(id), spec, controller, self) && !cardCantBeTargetedBy(g, g.Card(id), controller, self, ask) {
				eligible = append(eligible, id)
			}
		}
	}
	return eligible
}

// permanentEffect is CR 608.2m/608.3g's own resolution for a permanent
// spell, trimmed to what this port can support: Sneak's "enters tapped and
// attacking" (sneakEntry, sneakJoinsCombat, sneak.go) is ported; no
// Dash/Blitz/Warp alternate-cast-mode handling (PermanentEffect.java's own
// resolve checks host.wasCast()/isDash()/isBlitz()/isWarp(), none of which
// this port can grant a spell). What is left, ported directly, is the whole of
// what both APIPermanentCreature and APIPermanentNoncreature actually do:
// the card leaves the stack and becomes a permanent, CR 614.1's replacements
// of its entry ("enters as a copy", then "enters tapped") run against it
// before anything else sees the result (enterBattlefieldReplacements,
// entersascopy.go), and then CR 603.2's own ETB
// trigger check runs (checkETBTriggers, trigger.go) the same way Java's
// table.triggerChangesZoneAll does after every zone change. Java
// splits the two APIs only for getStackDescription's own display text
// (PermanentCreatureEffect overrides it to show P/T); this port has no
// stack-description system, so one stateless value answers for both.
//
//crucible:register PermanentCreature
//crucible:register PermanentNoncreature
type permanentEffect struct{}

func (permanentEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	origin := g.Card(a.Source).Zone
	door, castRoom := g.Card(a.Source).castRoomDoor()
	copyBecomesToken(g.Card(a.Source))
	sneak, err := g.sneakEntry(a.Source)
	if err != nil {
		return err
	}
	if g.entryReplaced(controller, a.Source, origin, a.Controller) {
		return nil
	}
	if sneak {
		// PermanentEffect.java:33-35: a sneaked permanent is tapped before it
		// moves, so it enters tapped.
		g.Card(a.Source).Tapped = true
	}
	g.Move(a.Source, Battlefield, a.Controller)
	g.enterBattlefieldReplacements(controller, a.Source, origin)
	if sneak {
		g.sneakJoinsCombat(a.Source)
	}
	if castRoom {
		// GameAction.java:571: the half the Room was cast as unlocks for
		// its caster as it enters, firing its "when you unlock this door".
		g.unlockDoor(controller, a.Source, a.Controller, door)
	}
	g.checkETBTriggers(controller, a.Source, origin)
	// A lone entry is a batch of one for Mode$ ChangesZoneAll (GameAction's
	// moveToPlay ends in table.triggerChangesZoneAll): Mystic Reflection's
	// effect ends here, once the one card entered as its copy.
	g.checkChangesZoneAllTriggers(controller, []CardID{a.Source}, origin, Battlefield)
	return nil
}

// attachEffect is CR 601.2c/608.2c's own resolution for an Aura: move it to
// the battlefield, then attach it to the target castAura chose at cast time
// (Ability.Target) -- Java's own AttachEffect.resolve does the two in the
// same order (moveToPlay before attachToEntity), though nothing here would
// change if they ran the other way since Move and Attach touch disjoint
// fields.
//
// CR 608.2b's re-check of the Aura's target runs before this, in
// targetsStillLegal (auraTargetStillLegal, targeting.go); an Aura whose host
// becomes illegal after it attaches falls off through the
// cleanupDanglingAttachments state-based action (action.go).
type attachEffect struct{}

func (attachEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if !a.spell {
		return g.attachActivated(a, controller)
	}
	origin := g.Card(a.Source).Zone
	copyBecomesToken(g.Card(a.Source))
	if g.entryReplaced(controller, a.Source, origin, a.Controller) {
		return nil
	}
	g.Move(a.Source, Battlefield, a.Controller)
	if a.Target == NoCard {
		// castPlayerAura: the Aura enchants the player it targeted.
		if pid, ok := firstPlayerTarget(a.Targets); ok {
			g.attachToPlayer(controller, a.Source, pid)
		}
	} else {
		g.attachTo(controller, a.Source, a.Target)
	}
	g.enterBattlefieldReplacements(controller, a.Source, origin)
	g.checkETBTriggers(controller, a.Source, origin)
	g.checkChangesZoneAllTriggers(controller, []CardID{a.Source}, origin, Battlefield)
	return nil
}

// firstPlayerTarget is the first player among targets.
func firstPlayerTarget(targets []EntityID) (PlayerID, bool) {
	for _, e := range targets {
		if pid, ok := e.AsPlayer(); ok {
			return pid, true
		}
	}
	return NoPlayer, false
}

// attachActivated is AttachEffect.resolve for an activated AB$ Attach (Equip,
// CR 702.6a): the source permanent attaches to the first card its targets
// name, moving off what it was attached to. Nothing is attached when the
// source has left the battlefield, the target is gone or is not a creature,
// or the host's protection refuses the attachment (CR 702.16c). Params
// choosing the attachment or the host without targeting (Object$, Choices$,
// Defined$, PlayerChoices$, Optional$, Chooser$) are not read: such a line is
// an error, never an attachment to the wrong thing (GO-7).
func (g *Game) attachActivated(a *Ability, controller PlayerController) error {
	if _, ok := a.Params.Param("Object"); ok {
		return g.attachObject(a, controller)
	}
	if _, targeted := a.Params.Param("ValidTgts"); !targeted {
		if _, ok := a.Params.Param("Defined"); ok {
			return g.attachDefined(a, controller)
		}
	}
	for _, key := range [...]string{"Choices", "Defined", "PlayerChoices", "Optional", "Chooser", "Move"} {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Attach: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if source.Zone != Battlefield || !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	for _, e := range a.Targets {
		host, ok := e.AsCard()
		if !ok || host == a.Source || g.Card(host).Zone != Battlefield || !g.Card(host).Type().Has(cardtype.Creature) {
			continue
		}
		if hostRefusesAttach(g, source, host) {
			return nil
		}
		g.attachTo(controller, a.Source, host)
		return nil
	}
	return nil
}

// copyBecomesToken is CR 111.11, GameAction.changeZone's first branch
// (GameAction.java:96-98): a copy of a permanent spell becomes a token as it
// resolves, so the Move onto the battlefield that follows is a token's, not
// a copy ceasing to exist (ceaseCopiedSpell, game.go). A no-op for a card
// that is not a copy.
func copyBecomesToken(c *Card) {
	if c.IsCopiedSpell && c.Zone == Stack {
		c.IsCopiedSpell, c.IsToken = false, true
	}
}

// castSnapshotNow records the battlefield and the graveyards, in seating order.
func (g *Game) castSnapshotNow() castSnapshot {
	snap := castSnapshot{set: true}
	for _, pid := range g.Players() {
		snap.battlefield = append(snap.battlefield, g.Zone(Battlefield, pid).Cards()...)
		snap.graveyard = append(snap.graveyard, g.Zone(Graveyard, pid).Cards()...)
	}
	return snap
}
