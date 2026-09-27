// Casting a spell: CR 601. Three shapes: nothing left to decide once a
// permanent (no target) or an Aura (one target) is chosen, and an Instant or
// Sorcery, whose own A:SP$ ability line can name modes and targets of its
// own (ADR-0018).

package engine

import (
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
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
func (g *Game) CastSpell(pid PlayerID, card CardID, controller PlayerController) bool {
	c := g.Card(card)
	fromHand := c.Controller() == pid && c.Zone == Hand
	play, granted, err := g.mayPlayOption(pid, card, fromHand)
	if err != nil {
		g.recordPendingError(err)
		return false
	}
	if !fromHand && !granted {
		return false
	}
	if !c.Type().Has(cardtype.Instant) && !play.WithFlash && !g.canActSorcerySpeed(pid) {
		return false
	}
	return g.castSpell(controller, pid, card, castOpts{withoutManaCost: play.WithoutManaCost})
}

// castOpts is how an effect's "cast it" differs from casting it normally
// (PlayEffect.java's tgtSA adjustments before playSaFromPlayEffect).
type castOpts struct {
	// withoutManaCost is WithoutManaCost$ (SpellAbility.copyWithNoManaCost):
	// no mana is paid, and an X in the cost is 0, so ChoosePayX is never
	// asked (CR 107.3b).
	withoutManaCost bool
}

// castSpell is CastSpell past its timing and hand gates: the three spell
// shapes (Aura, Instant/Sorcery, other permanent), each choosing, paying,
// then putting the card on the stack. It is the one cast path, from any
// zone: CastSpell calls it for a card in hand at sorcery speed, and an
// effect that casts during its resolution (Play, playeffect.go; Discover's
// castWithoutPaying) calls it with timing ignored (CR 608.2g). Reports false
// for every legal-but-declined case the three branches name.
func (g *Game) castSpell(controller PlayerController, pid PlayerID, card CardID, opts castOpts) bool {
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
	x, paid := g.payCastCost(pid, c, controller, opts)
	if !paid {
		return false
	}
	g.putSpellOnStack(card, pid)
	api := APIPermanentNoncreature
	if c.Type().Has(cardtype.Creature) {
		api = APIPermanentCreature
	}
	cast := Ability{API: api, Source: card, Controller: pid, spell: true}
	x.setOn(&cast)
	g.PushAbility(cast)
	g.sink.Emit(Event{Kind: SpellCast, Phase: g.activePhase, Active: g.activePlayer, Actor: pid, Turn: uint16(g.turn), Source: card})
	g.Player(pid).SpellsCastThisTurn++
	g.checkSpellCastTriggers(controller, card, pid)
	return true
}

// payCastCost pays c's printed mana cost for pid (CR 601.2g-h), or nothing
// under opts.withoutManaCost, and reports the X it announced for the caller
// to record on the spell (xAnnounced.setOn, manapay.go). A WithoutManaCost$
// cast announces none: its cost has no X part left, and
// PlaySpellAbility.announceValuesLikeX leaves xManaCostPaid null then.
func (g *Game) payCastCost(pid PlayerID, c *Card, controller PlayerController, opts castOpts) (xAnnounced, bool) {
	if opts.withoutManaCost {
		return xAnnounced{}, true
	}
	return g.payManaCostX(pid, c.Def.Faces[0].ManaCost, controller)
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
// Controller$ You over an opponent's exiled card) is the opponent.
func (g *Game) putSpellOnStack(card CardID, pid PlayerID) {
	g.Move(card, Stack, pid)
	g.Card(card).controller = pid
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
	spec, ok := enchantSpec(c)
	if !ok {
		return false
	}
	eligible := g.enchantTargets(spec, pid, card)
	if len(eligible) == 0 {
		return false
	}
	target := eligible[0]
	if len(eligible) > 1 {
		target = controller.ChooseEnchantTarget(g, pid, card, eligible)
	}
	x, paid := g.payCastCost(pid, c, controller, opts)
	if !paid {
		return false
	}
	g.putSpellOnStack(card, pid)
	cast := Ability{API: APIAttach, Source: card, Controller: pid, Target: target, spell: true}
	x.setOn(&cast)
	g.PushAbility(cast)
	g.sink.Emit(Event{Kind: SpellCast, Phase: g.activePhase, Active: g.activePlayer, Actor: pid, Turn: uint16(g.turn), Source: card})
	g.Player(pid).SpellsCastThisTurn++
	g.checkSpellCastTriggers(controller, card, pid)
	tgts := []EntityID{CardEntity(target)}
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
	a := Ability{API: apiType, Source: card, Controller: pid, Params: spellAbility, Amounts: c.Def.Faces[0].Amounts, spell: true}
	if a.API == APICharm {
		modesOK, err := g.chooseCharmModes(controller, &a)
		if err != nil {
			a.modesErr = err
		} else if !modesOK {
			return false
		}
	}
	if !g.resolveTargets(controller, &a) {
		return false
	}
	x, paid := g.payCastCost(pid, c, controller, opts)
	if !paid {
		return false
	}
	x.setOn(&a)
	g.putSpellOnStack(card, pid)
	g.PushAbility(a)
	g.sink.Emit(Event{Kind: SpellCast, Phase: g.activePhase, Active: g.activePlayer, Actor: pid, Turn: uint16(g.turn), Source: card})
	g.Player(pid).SpellsCastThisTurn++
	g.checkSpellCastTriggers(controller, card, pid)
	tgts := allTargetsOf(a)
	matches := g.checkBecomesTargetTriggers(tgts, false, pid)
	matches = append(matches, g.checkWardTriggers(tgts, card, pid)...)
	g.pushTriggeredAbilities(controller, matches)
	return true
}

// allTargetsOf gathers a's own top-level Targets plus every one of a
// Charm's chosen modes' own Targets (Ability.Modes, ability.go) --
// checkBecomesTargetTriggers/checkWardTriggers otherwise never see a modal
// spell's own chosen target, since chooseCharmModes stores each mode's
// targets on Ability.Modes[i], not on a.Targets itself.
func allTargetsOf(a Ability) []EntityID {
	targets := append([]EntityID(nil), a.Targets...)
	for _, mode := range a.Modes {
		targets = append(targets, mode.Targets...)
	}
	return targets
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

// enchantTargets is every battlefield permanent, across every player, that
// spec (self's own Enchant restriction, enchantSpec) matches, and that does
// not refuse self outright (cardCantBeTargetedBy, staticability.go -- CR
// 702.11b/702.16e/702.18a's own Hexproof/Protection/Shroud gate, a separate
// question from the card-type restriction spec itself checks) -- CR
// 601.2c's legal-target set for casting self as an Aura. Matches' own
// source parameter is self, the same "the enchantment's own id, not the
// host's" convention cleanupDanglingAttachments (action.go) already uses
// when re-checking an attached Aura's own restriction after the fact.
func (g *Game) enchantTargets(spec valid.Spec, controller PlayerID, self CardID) []CardID {
	var eligible []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if Matches(g, g.Card(id), spec, controller, self) && !cardCantBeTargetedBy(g, g.Card(id), controller, self) {
				eligible = append(eligible, id)
			}
		}
	}
	return eligible
}

// permanentEffect is CR 608.2m/608.3g's own resolution for a permanent
// spell, trimmed to what this port can support: no Dash/Blitz/Warp/Sneak
// alternate-cast-mode handling (PermanentEffect.java's own resolve checks
// host.wasCast()/isDash()/isBlitz()/isWarp()/isSneak(), none of which this
// port can grant a spell). What is left, ported directly, is the whole of
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
	copyBecomesToken(g.Card(a.Source))
	g.Move(a.Source, Battlefield, a.Controller)
	g.enterBattlefieldReplacements(controller, a.Source, origin)
	g.checkETBTriggers(controller, a.Source, origin)
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
	origin := g.Card(a.Source).Zone
	copyBecomesToken(g.Card(a.Source))
	g.Move(a.Source, Battlefield, a.Controller)
	g.Attach(a.Source, a.Target)
	g.enterBattlefieldReplacements(controller, a.Source, origin)
	g.checkETBTriggers(controller, a.Source, origin)
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
