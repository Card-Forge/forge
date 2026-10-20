// FlipOntoBattlefield: 2 real (AB|DB|SP)$ FlipOntoBattlefield lines --
// chaos_orb.txt and falling_star.txt, the two Un-set "throw the physical
// card at the table" gonzo cards CR does not otherwise describe. Both name
// no param this port does not read; only AllowRandom$ (0 real lines) is
// rejected outright, defensively.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/FlipOntoBattlefieldEffect.java's
// resolve. The controller picks a battlefield permanent as the "landing
// spot" (chooseCardsForEffect, no bounding-box system exists, Java's own
// TODO at :33 says the same); flipNeighbor then finds one neighbor of that
// spot -- an attachment half the time if the spot carries one, else the
// spot's own left neighbor in its controller's battlefield order, matching
// Java's own getNeighboringCard(tgtLoc, -1). getNeighboringCard(tgtLoc, +1)
// (java:39) is not ported: it never draws (the attachment lottery is gated
// on direction<0) and its result is always discarded (getNeighboringCard
// never returns nil, so java:43's own "lhsNeighbor != null" is always true
// and the rhsNeighbor branch at java:45-47 never runs) -- computing it
// would change nothing this port can observe.
//
// The rest is a straight RNG replay, in Java's own draw order, through
// pkg/javarand (ADR-0010): a 15% chance the card never flips at all (no
// remember, no further draws); otherwise one more draw picks a times-flipped
// count this port also drops (nothing downstream reads TimesFlipped --
// neither chaos_orb.txt's nor falling_star.txt's own SubAbility$ chain names
// a Condition*SVar$ on it -- but the draw itself still has to happen, to
// keep the RNG stream in the same place Java's own would be); then a last
// draw decides whether the flip lands on both the spot and its neighbor, on
// one of the two, or on neither. Aggregates.random(List, count)'s own
// reservoir sampling never actually draws for this file's own randChoices,
// which never grows past 2 elements against a count of 1 or 2 -- verified
// by hand from Aggregates.java's own reservoir loop, `i <= count` always
// true across a 2-element list -- so the two-card hit is a plain slice, not
// a sampled one.
//
// Forge bug (PORT-8): FlipOntoBattlefieldEffect.java:109's own
// getNeighboringCard filter, `c.isPlaneswalker() || c.isArtifact() ||
// (c.isEnchantment() && !c.isAura())`, re-tests the landing spot itself
// (`c`) instead of the candidate under test (`card`) on its own third
// clause. Entering the branch at all only requires ONE of the three OR'd
// conditions on `c`; when a planeswalker or artifact spot enters it without
// also being a non-Aura enchantment, `c`'s own enchantment clause evaluates
// false there and the return degenerates to the correct
// `card.isPlaneswalker() || card.isArtifact()` -- no bug in that case. Only
// a non-Aura-enchantment landing spot makes its own third clause true
// unconditionally, and the whole return degenerates to "true": every
// permanent on that spot's controller's battlefield becomes a valid
// neighbor candidate. Not reproduced: this port rejects a non-Aura
// enchantment landing spot with an error instead of silently sweeping the
// entire battlefield for a card that names no such thing. Chaos Orb, a
// plain artifact, is unaffected by the rejection and resolves normally when
// chosen as the landing spot.

package engine

//enginelint:allow id card game player ability condition control valid zone parts cardtype

import (
	"fmt"
	"slices"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// flipOntoBattlefieldUnresolvedParams names FlipOntoBattlefieldEffect.java's
// own params this port does not evaluate. AllowRandom$ (0 real lines) feeds
// chooseCardsForEffect's own isOptional argument, which can return an empty
// pick even with permanents on the battlefield -- a shape with no real
// corpus line to confirm against, rejected rather than guessed at.
var flipOntoBattlefieldUnresolvedParams = [...]string{"AllowRandom"}

type flipOntoBattlefieldEffect struct{}

func (flipOntoBattlefieldEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range flipOntoBattlefieldUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: FlipOntoBattlefield: %s$ not resolvable yet", key)
		}
	}
	host := g.Card(a.Source)
	if !subAbilityConditionMet(g, host, a.Amounts, a.Params) {
		return nil
	}

	var options []CardID
	for _, pid := range g.Players() {
		options = append(options, g.Zone(Battlefield, pid).Cards()...)
	}
	if len(options) == 0 {
		return fmt.Errorf("engine: FlipOntoBattlefield: no permanent on the battlefield to flip onto")
	}
	picked := controller.ChooseCardsForEffect(g, a.Controller, a.Source, options, 1, 1)
	if len(picked) != 1 {
		return fmt.Errorf("engine: FlipOntoBattlefield: controller chose %d cards, want 1", len(picked))
	}
	tgtLoc := g.Card(picked[0])

	candidates, attachments, err := flipCandidates(g, tgtLoc)
	if err != nil {
		return err
	}
	neighbor := flipNeighbor(g, tgtLoc, candidates, attachments)

	// randChoices: FlipOntoBattlefieldEffect.java:41-47 builds an
	// FCollection (unique elements), adding tgtLoc then neighbor -- a no-op
	// when getNeighboringCard fell back to tgtLoc itself, java's own
	// "lhsNeighbor != null" branch above's own doc comment.
	randChoices := []CardID{tgtLoc.ID}
	if neighbor != tgtLoc.ID {
		randChoices = append(randChoices, neighbor)
	}

	const (
		chanceToFlip        = 0.85
		chanceToHitTwoCards = 0.20
		maxFlipTimes        = 2
	)

	// chanceToHit = min(1, 0.45 + 0.07 * rowSize) in float32 (java:41-42, #12183):
	// rowSize counts tgtLoc's controller's permanents sharing a core type with
	// tgtLoc, itself included. Variables, not constants, so Go rounds each step
	// to float32 as Java does.
	rowSize := 0
	for _, pid := range g.Players() {
		for _, cid := range g.Zone(Battlefield, pid).Cards() {
			card := g.Card(cid)
			if card.Controller() == tgtLoc.Controller() && sharesCoreType(card.Type(), tgtLoc.Type()) {
				rowSize++
			}
		}
	}
	base, perCard := float32(0.45), float32(0.07)
	chanceToHit := min(float32(1), base+perCard*float32(rowSize))

	if g.rand.Float32() > chanceToFlip {
		// Did not turn over even once: no effect at all (java:52-55).
		return nil
	}
	g.rand.Int32n(maxFlipTimes) // flippedTimes: drawn to match Java's stream; nothing downstream reads it (see doc comment above).

	var hit []CardID
	switch outcome := g.rand.Float32(); {
	case outcome <= chanceToHitTwoCards:
		hit = randChoices
	case outcome <= chanceToHit:
		hit = []CardID{randChoices[g.randomIndex(len(randChoices))]}
	}

	for _, id := range hit {
		host.Memory.Remember(CardEntity(id))
	}
	return nil
}

// flipCandidates is getNeighboringCard's own per-card filter
// (FlipOntoBattlefieldEffect.java:101-117), applied across tgtLoc's
// controller's whole battlefield: candidates is cardsOTB, attachments is the
// side list of candidates attached specifically to tgtLoc (java:100/103-104).
func flipCandidates(g *Game, tgtLoc *Card) (candidates, attachments []CardID, err error) {
	tgtType := tgtLoc.Type()
	isCreature := tgtType.Has(cardtype.Creature)
	isPW := tgtType.Has(cardtype.Planeswalker)
	isArtifact := tgtType.Has(cardtype.Artifact)
	isNonAuraEnchantment := tgtType.Has(cardtype.Enchantment) && !tgtType.HasSubtype("Aura")
	isLand := tgtType.Has(cardtype.Land)
	attachedHost, tgtAttached := tgtLoc.AttachedTo()

	if !isCreature && isNonAuraEnchantment {
		return nil, nil, fmt.Errorf("engine: FlipOntoBattlefield: chosen location %s is a non-Aura enchantment: forge-game/src/main/java/forge/game/ability/effects/FlipOntoBattlefieldEffect.java:109's own neighbor filter always matches every permanent for that shape (PORT-8), not reproduced", tgtLoc.Def.Name)
	}

	tgtController := tgtLoc.Controller()
	for _, pid := range g.Players() {
		for _, cid := range g.Zone(Battlefield, pid).Cards() {
			card := g.Card(cid)
			if card.Controller() != tgtController {
				continue
			}
			to, isAttached := card.AttachedTo()
			include := false
			switch {
			case isAttached && to == tgtLoc.ID:
				attachments = append(attachments, cid)
				include = true
			case isCreature:
				include = card.Type().Has(cardtype.Creature)
			case isPW || isArtifact:
				include = card.Type().Has(cardtype.Planeswalker) || card.Type().Has(cardtype.Artifact)
			case isLand:
				include = card.Type().Has(cardtype.Land)
			case tgtAttached:
				// tgtLoc is itself attached to something (an Aura or Equipment
				// as the landing spot): match siblings attached to the same
				// host (java:112-113's own first disjunct). The second
				// disjunct, `c.equals(card.getAttachedTo())`, can never fire
				// here -- it asks whether card is attached to tgtLoc, which the
				// first case above already caught and returned early for -- so
				// it is not ported.
				include = isAttached && to == attachedHost
			default:
				include = sharesCoreType(card.Type(), tgtType)
			}
			if include {
				candidates = append(candidates, cid)
			}
		}
	}
	return candidates, attachments, nil
}

// flipNeighbor is getNeighboringCard(tgtLoc, -1): half the time, when tgtLoc
// carries an attachment, the flip lands on one of its attachments instead of
// a battlefield neighbor. Otherwise it is tgtLoc's own left neighbor in
// candidates' order -- or, when tgtLoc sits at index 0 and a right neighbor
// exists, that right neighbor instead: java:125-129's own "leftmost" quirk,
// preserved because parity with the shipped card depends on it (PORT-7), not
// a bug like flipCandidates' own rejected shape above.
func flipNeighbor(g *Game, tgtLoc *Card, candidates, attachments []CardID) CardID {
	const hitAttachment = 0.50
	if len(attachments) > 0 && g.rand.Float32() <= hitAttachment {
		return attachments[g.randomIndex(len(attachments))]
	}
	loc := slices.Index(candidates, tgtLoc.ID)
	if loc < 0 {
		// flipCandidates always matches tgtLoc against its own filter (every
		// branch there tests tgtLoc's own type against itself trivially),
		// so tgtLoc is always present in candidates -- an engine invariant,
		// not something a card script can cause (GO-7).
		panic("engine: FlipOntoBattlefield: chosen location missing from its own candidate list")
	}
	if loc > 0 {
		return candidates[loc-1]
	}
	if loc < len(candidates)-1 {
		return candidates[loc+1]
	}
	return tgtLoc.ID
}

// sharesCoreType is Card.sharesCardTypeWith: true when a and b share any one
// core type (CardType.java's own sharesCardTypeWith, core types only --
// supertypes and subtypes do not count).
func sharesCoreType(a, b cardtype.Line) bool {
	for _, t := range a.CoreTypes() {
		if b.Has(t) {
			return true
		}
	}
	return false
}
