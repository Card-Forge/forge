package engine

// package engine, not engine_test (TEST-2's "invariant not observable from
// outside" row): cardCantBeTargetedBy's own effect on targetCandidates --
// whether an illegal target is actually excluded from the offered
// candidate set, rather than merely dropped later by the CR 608.2b
// re-check -- has no observable trace in fixture.Dump's own state (a spell
// that never offered an illegal target and a spell that offered it and
// then fizzled both end up in the same graveyard with the same effect
// applied). code-review of this pack's own fixture scenarios found exactly
// this: every "on resolution" fixture still passes with the
// targetCandidates filter below deleted, since ScriptedController.
// ChooseTargets (control.go) never validates its answer against the
// candidates it was offered. This file calls targetCandidates directly to
// hold that filter to its own contract, the thing no scenario can.

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

func targetCandidatesTestGame(t *testing.T, players ...string) *Game {
	t.Helper()
	return NewGame(nil, javarand.New(1), players)
}

func targetCandidatesTestTypeRegistry(t *testing.T) *cardtype.Registry {
	t.Helper()
	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\nSalamander\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	return reg
}

func targetCandidatesTestCreature(t *testing.T, keywords ...string) *compile.Card {
	t.Helper()
	return targetCandidatesTestCreatureOfType(t, "Creature Elf", keywords...)
}

func targetCandidatesTestCreatureOfType(t *testing.T, typeLine string, keywords ...string) *compile.Card {
	t.Helper()
	def := &compile.Card{Name: "Test Creature"}
	def.Faces[0].Type = cardtype.Parse(targetCandidatesTestTypeRegistry(t), typeLine)
	def.Faces[0].Power, def.Faces[0].Toughness = "2", "2"
	def.Faces[0].Keywords = keywords
	return def
}

func hasCardCandidate(candidates []EntityID, id CardID) bool {
	for _, e := range candidates {
		if got, ok := e.AsCard(); ok && got == id {
			return true
		}
	}
	return false
}

func hasPlayerCandidate(candidates []EntityID, id PlayerID) bool {
	for _, e := range candidates {
		if got, ok := e.AsPlayer(); ok && got == id {
			return true
		}
	}
	return false
}

// TestTargetCandidatesHexproofExcludesOpponentIncludesController is CR
// 702.11b/e at the point a target is offered, not just at the CR 608.2b
// re-check every fixture scenario can reach instead.
func TestTargetCandidatesHexproofExcludesOpponentIncludesController(t *testing.T) {
	t.Parallel()

	g := targetCandidatesTestGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	target := g.NewCard(targetCandidatesTestCreature(t, "Hexproof"), p, Battlefield)
	src := g.NewCard(targetCandidatesTestCreature(t), opp, Hand)

	if got := g.targetCandidates(opp, src, "Creature"); hasCardCandidate(got, target) {
		t.Errorf("opponent's targetCandidates includes a Hexproof creature, want excluded")
	}
	if got := g.targetCandidates(p, src, "Creature"); !hasCardCandidate(got, target) {
		t.Errorf("controller's own targetCandidates excludes their own Hexproof creature, want included")
	}
}

// TestTargetCandidatesShroudExcludesEvenTheController is CR 702.18a: Shroud
// carries no Activator$ gate, unlike Hexproof.
func TestTargetCandidatesShroudExcludesEvenTheController(t *testing.T) {
	t.Parallel()

	g := targetCandidatesTestGame(t, "a")
	p := g.Players()[0]
	target := g.NewCard(targetCandidatesTestCreature(t, "Shroud"), p, Battlefield)
	src := g.NewCard(targetCandidatesTestCreature(t), p, Hand)

	if got := g.targetCandidates(p, src, "Creature"); hasCardCandidate(got, target) {
		t.Errorf("targetCandidates includes a Shroud creature for its own controller, want excluded")
	}
}

// TestTargetCandidatesProtectionExcludesMatchingColorIncludesOthers is CR
// 702.16e's targeting half: ValidSource$ matches the ability's own host
// card, not who controls it.
func TestTargetCandidatesProtectionExcludesMatchingColorIncludesOthers(t *testing.T) {
	t.Parallel()

	g := targetCandidatesTestGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	target := g.NewCard(targetCandidatesTestCreature(t, "Protection from red"), p, Battlefield)
	redSrc := targetCandidatesTestCreature(t)
	redSrc.Faces[0].ManaCost = mana.MustParse("R")
	red := g.NewCard(redSrc, opp, Hand)
	green := g.NewCard(targetCandidatesTestCreature(t), opp, Hand)

	if got := g.targetCandidates(opp, red, "Creature"); hasCardCandidate(got, target) {
		t.Errorf("targetCandidates includes a Protection-from-red creature against a red source, want excluded")
	}
	if got := g.targetCandidates(opp, green, "Creature"); !hasCardCandidate(got, target) {
		t.Errorf("targetCandidates excludes a Protection-from-red creature against a colorless source, want included")
	}
}

// TestTargetCandidatesProtectionChecksEveryLine is CR 702.16b: a source
// with two or more protection abilities has each apply independently.
// Mirran Crusader's own "Protection from black" then "Protection from
// green" is the real corpus shape (22 cards carry two or more Protection
// lines) -- protectionEach's own predecessor, protectionValid, reported
// only the first line it found, which this locks against regressing.
func TestTargetCandidatesProtectionChecksEveryLine(t *testing.T) {
	t.Parallel()

	g := targetCandidatesTestGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	target := g.NewCard(targetCandidatesTestCreature(t, "Protection from black", "Protection from green"), p, Battlefield)
	greenSrc := targetCandidatesTestCreature(t)
	greenSrc.Faces[0].ManaCost = mana.MustParse("G")
	green := g.NewCard(greenSrc, opp, Hand)

	if got := g.targetCandidates(opp, green, "Creature"); hasCardCandidate(got, target) {
		t.Errorf("targetCandidates includes a creature with Protection from green against a green source (second Protection line), want excluded")
	}
}

// TestTargetCandidatesPlayerHexproofExcludesOpponentIncludesSelf is CR
// 702.11b/e for a Player entity -- Leyline of Sanctity's own shape,
// PlayerFactoryUtil.java's `Activator$ Opponent` gate, checked directly
// against Player.KeywordMod rather than through a Layer 6 pass: a bare
// Add is enough here, and running one would only risk a Clear() wiping it
// (applyContinuousKeyword's own doc comment).
func TestTargetCandidatesPlayerHexproofExcludesOpponentIncludesSelf(t *testing.T) {
	t.Parallel()

	g := targetCandidatesTestGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	g.Player(p).KeywordMod.Add(KeywordEffect{AddKeywords: []string{"Hexproof"}})
	src := g.NewCard(targetCandidatesTestCreature(t), opp, Hand)

	if got := g.targetCandidates(opp, src, "Player"); hasPlayerCandidate(got, p) {
		t.Errorf("opponent's targetCandidates includes a Hexproof player, want excluded")
	}
	if got := g.targetCandidates(p, src, "Player"); !hasPlayerCandidate(got, p) {
		t.Errorf("controller's own targetCandidates excludes their own Hexproof player, want included")
	}
}

// TestTargetCandidatesPlayerShroudExcludesEvenTheController is CR 702.18a
// for a Player entity -- True Believer/Ivory Mask's own shape: no
// Activator$ gate, unlike Hexproof.
func TestTargetCandidatesPlayerShroudExcludesEvenTheController(t *testing.T) {
	t.Parallel()

	g := targetCandidatesTestGame(t, "a")
	p := g.Players()[0]
	g.Player(p).KeywordMod.Add(KeywordEffect{AddKeywords: []string{"Shroud"}})
	src := g.NewCard(targetCandidatesTestCreature(t), p, Hand)

	if got := g.targetCandidates(p, src, "Player"); hasPlayerCandidate(got, p) {
		t.Errorf("targetCandidates includes a Shroud player for its own controller, want excluded")
	}
}

// TestTargetCandidatesPlayerProtectionExcludesMatchingTypeIncludesOthers is
// CR 702.16e for a Player entity -- Gor Muldrak, Amphinologist's own
// `Protection:Salamander` shape, a plain colon-structured characteristic
// protectionEach resolves against Player.KeywordLines the same way it
// already does against a card's own. No real corpus card combines the
// Salamander subtype with a targeted ability, the reason
// gor-muldrak-protection-does-not-refuse-unrelated-bolt (fixture) only
// covers the non-matching half; this covers both.
func TestTargetCandidatesPlayerProtectionExcludesMatchingTypeIncludesOthers(t *testing.T) {
	t.Parallel()

	g := targetCandidatesTestGame(t, "a", "b")
	p, opp := g.Players()[0], g.Players()[1]
	g.Player(p).KeywordMod.Add(KeywordEffect{AddKeywords: []string{"Protection:Salamander"}})
	salamander := g.NewCard(targetCandidatesTestCreatureOfType(t, "Creature Salamander"), opp, Hand)
	elf := g.NewCard(targetCandidatesTestCreature(t), opp, Hand)

	if got := g.targetCandidates(opp, salamander, "Player"); hasPlayerCandidate(got, p) {
		t.Errorf("targetCandidates includes a Protection-from-Salamander player against a Salamander source, want excluded")
	}
	if got := g.targetCandidates(opp, elf, "Player"); !hasPlayerCandidate(got, p) {
		t.Errorf("targetCandidates excludes a Protection-from-Salamander player against a non-Salamander source, want included")
	}
}
