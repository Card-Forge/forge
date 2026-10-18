package engine

import (
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// regenerate is CR 701.16's own regeneration action, applied whenever a
// permanent that would be destroyed has a way to regenerate instead
// (Card.canRegenerate's own two real sources): a regeneration shield --
// Card.RegenShields, granted by AB$/DB$ Regenerate (regenerateeffect.go) --
// or a permanent's own always-on "if this would be destroyed, regenerate
// it" replacement (ApiType.Regeneration -- RegenerationEffect.java --
// reached only through R:Event$ Destroy | Regeneration$ True | ReplaceWith$
// <SVar>, e.g. mossbridge_troll.txt:5-6). It reports whether the
// destruction was replaced.
//
// cardCantRegenerate is checked first, ahead of both sources: Mode$
// CantRegenerate (knight_of_the_holy_nimbus.txt's/clergy_of_the_holy_nimbus
// .txt's own "{N}: CARDNAME can't be regenerated this turn," an opponent-only
// activated ability) blocks a shield exactly as it blocks the static
// replacement -- Card.canRegenerate's own first check in Java,
// `getGame().getStaticEffects().getCantRegenerateList()`, applied before
// either source is even asked.
//
// Past that, the static replacement is checked before a shield: it costs
// nothing, and Java's own GameAction.destroy offers every applicable
// ReplacementEffect through the identical CR 616 "which one applies" choice
// a shield's own replacement instance is generated into -- checking it first
// here simply means a permanent carrying both this static text AND an
// active shield spends neither at once and keeps the shield for a later
// destruction, the more useful of the two orders a corpus with no card
// combining both today cannot tell apart empirically either way.
func (g *Game) regenerate(controller PlayerController, id CardID) bool {
	if cardCantRegenerate(g, id) {
		return false
	}
	c := g.Card(id)
	if destroyReplacedByRegeneration(g, controller, c) {
		return true
	}
	if c.RegenShields <= 0 {
		return false
	}
	c.RegenShields--
	g.regenerateBody(controller, id)
	return true
}

// cardCantRegenerate reports whether any Mode$ CantRegenerate static
// ability currently in play names id -- cantBlockBy's own walk
// (staticability.go, Game.traitHosts) applied to a different Mode$: every
// battlefield permanent and Command-zone effect card is a possible host,
// the identical source Java's own getCantRegenerateList draws from (an
// effect card's own StaticAbilities$ trait, effecteffect.go, is the only
// real corpus source -- 2 of the corpus's real AB$ Effect|StaticAbilities$
// lines, knight_of_the_holy_nimbus.txt's/clergy_of_the_holy_nimbus.txt's own
// opponent-only "{N}: CARDNAME can't be regenerated this turn"). Any param
// past ValidCard$ (0 real lines carry one) is not read: both real lines are
// bare past it.
func cardCantRegenerate(g *Game, id CardID) bool {
	target := g.Card(id)
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.traitFaces() {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, "CantRegenerate") {
						continue
					}
					validCard, ok := s.Param("ValidCard")
					if !ok {
						continue
					}
					if Matches(g, target, valid.Parse(validCard), h.Controller(), host) {
						return true
					}
				}
			}
		}
	}
	return false
}

// regenerateBody is CR 701.16's own three-part action -- RegenerationEffect
// .resolve's own healDamage/tap/removeFromCombat trio -- shared by both of
// regenerate's own sources: a spent shield (above) and regenerationEffect
// (regenerationeffect.go), which the static replacement below runs.
func (g *Game) regenerateBody(controller PlayerController, id CardID) {
	c := g.Card(id)
	c.Damage.Heal()
	// RegenerationEffect.java:41: Card.addRegeneratedThisTurn, read by
	// Count$RegeneratedThisTurn (Spiny Starfish).
	c.regeneratedThisTurn++
	if !c.Tapped {
		c.Tapped = true
		g.checkTapsTriggers(controller, id, c.Controller(), false)
	}
	g.removeFromCombat(id)
}

// destroyReplacedByRegeneration reports whether host carries a resolvable
// "if this would be destroyed, regenerate it" replacement (ApiType
// .Regeneration's own one real corpus shape, mossbridge_troll.txt:5-6,
// knight_of_the_holy_nimbus.txt:6-7, clergy_of_the_holy_nimbus.txt:6-7 --
// 3 of 3 real DB$ Regeneration lines, every one naming ValidCard$ Card.Self,
// so host is always both the replacement's own carrier and the card the
// replacement is about) and, when it does, runs it.
//
// The match itself is hand-run against host's own compiled Replacements,
// the identical "recognize the one shape, run it by hand" precedent
// drawReplaced's/gainLifeReplaced's own applyDrawReplacement/
// applyGainLifeReplacement already have for a different Event$
// (replacement.go): the call sites that decide a destruction (destroyEffect,
// destroyAllEffect, destroyDamagedCreatures) run outside any
// Registry.Resolve call, so Game.registry (game.go) cannot be relied on to
// be set yet -- a fresh game's very first state-based-action check, before
// any ability has ever resolved, is exactly the case that would panic on a
// nil Registry. Once matched, the ReplaceWith$ ability itself runs through
// the real registered regenerationEffect (regenerationeffect.go) directly --
// called as a value, not through *Registry.Resolve, for the identical
// nil-Game.registry reason -- rather than a second hand-rolled copy of its
// body: the registered API and the one this file's own real corpus lines
// run are the same code. regenerationEffect.Resolve's own error (an
// unrecognized Defined$) means the whole replacement is not run, exactly
// like an unrecognized shape reported false here would be -- GO-7's refuse-
// rather-than-guess contract, not a reason to fall back to a shield anyway.
func destroyReplacedByRegeneration(g *Game, controller PlayerController, host *Card) bool {
	if host.Def == nil {
		return false
	}
	for _, face := range host.traitFaces() {
		for _, r := range face.Replacements {
			if !regenerationReplacementMatches(g, r, host, face.Amounts) {
				continue
			}
			for _, sub := range r.Subs {
				if !strings.EqualFold(sub.Key, "ReplaceWith") || !strings.EqualFold(sub.Ability.Name, "Regeneration") {
					continue
				}
				if _, ok := sub.Ability.Param("SubAbility"); ok {
					// No chaining support here (PORT-8/GO-7) -- 0 real lines
					// name one.
					return false
				}
				child := Ability{
					API:        APIRegeneration,
					Source:     host.ID,
					Controller: host.Controller(),
					Params:     sub.Ability,
					Amounts:    face.Amounts,
				}
				return (regenerationEffect{}).Resolve(g, &child, controller) == nil
			}
		}
	}
	return false
}

// regenerationReplacementMatches is destroyReplacedByRegeneration's own
// shared half: Event$ Destroy, Regeneration$ True, host in one of r's own
// ActiveZones$ (hostInActiveZones, replacement.go -- always Battlefield in
// every real line and at every real call site, since regenerate is only
// ever asked about a permanent already there), ValidCard$ matched against
// host itself the identical way every other replacement match in this
// package already is, and replacementRequirementsCheck (replacement.go).
// Any param besides the ones the corpus's own 3 real lines carry (Event,
// ActiveZones, ValidCard, Regeneration, ReplaceWith, Description) skips the
// whole line rather than guessing (GO-7) -- there are none today, so this
// allow-list is exhaustive against the real corpus, not aspirational.
func regenerationReplacementMatches(g *Game, r *compile.Ability, host *Card, amounts map[string]expr.Amount) bool {
	if !strings.EqualFold(r.Name, "Destroy") {
		return false
	}
	regen, ok := r.Param("Regeneration")
	if !ok || !strings.EqualFold(regen, "True") {
		return false
	}
	for _, p := range r.Params {
		switch strings.ToLower(p.Key) {
		case "event", "regeneration", "description", "validcard", "activezones", "replacewith", "secondary":
		default:
			return false
		}
	}
	if !hostInActiveZones(host, r, host.Zone) {
		return false
	}
	validCard, ok := r.Param("ValidCard")
	if !ok {
		return false
	}
	if !Matches(g, host, valid.Parse(validCard), host.Controller(), host.ID) {
		return false
	}
	return replacementRequirementsCheck(g, host, amounts, r)
}
