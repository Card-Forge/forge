package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// TestCanBlockFlyingAttackerNeedsFlyingOrReach proves the Flying keyword's
// own CantBlockBy synthesis (cantBlockByKeywords, staticability.go),
// mirroring CardFactoryUtil.java's own "Mode$ CantBlockBy | ValidAttacker$
// Creature.Self | ValidBlocker$ Creature.withoutFlying+withoutReach": a
// creature with neither flying nor reach cannot block a flying attacker,
// one with either can.
func TestCanBlockFlyingAttackerNeedsFlyingOrReach(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	attacker := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Flying"), a, engine.Battlefield)
	ground := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	flyer := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Flying"), b, engine.Battlefield)
	reacher := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Reach"), b, engine.Battlefield)

	if g.CanBlock(attacker, ground) {
		t.Error("CanBlock(flying attacker, grounded blocker) = true, want false")
	}
	if !g.CanBlock(attacker, flyer) {
		t.Error("CanBlock(flying attacker, flying blocker) = false, want true")
	}
	if !g.CanBlock(attacker, reacher) {
		t.Error("CanBlock(flying attacker, reach blocker) = false, want true")
	}
}

// TestCanBlockOrdinaryAttacker is the sanity check: two plain creatures with
// no relevant keyword or static ability block each other legally, the same
// answer CanBlock's own base rule (untapped creature) already gave before
// CantBlockBy existed.
func TestCanBlockOrdinaryAttacker(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)

	if !g.CanBlock(attacker, blocker) {
		t.Error("CanBlock(plain attacker, plain blocker) = false, want true")
	}
}

// TestCanBlockFearAttacker proves the Fear keyword's own CantBlockBy
// synthesis ("ValidBlocker$ Creature.nonArtifact+nonBlack"): an artifact
// creature can still block a Fear attacker (the restriction only catches
// blockers that are both non-artifact AND non-black), a plain nonartifact
// creature cannot.
func TestCanBlockFearAttacker(t *testing.T) {
	t.Parallel()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\nGolem\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	attacker := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Fear"), a, engine.Battlefield)

	artifactDef := &compile.Card{Name: "Test Artifact Creature"}
	artifactDef.Faces[0].Type = cardtype.Parse(reg, "Artifact Creature Golem")
	artifactDef.Faces[0].Power, artifactDef.Faces[0].Toughness = "2", "2"
	artifactBlocker := g.NewCard(artifactDef, b, engine.Battlefield)

	plainBlocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)

	if !g.CanBlock(attacker, artifactBlocker) {
		t.Error("CanBlock(fear attacker, artifact blocker) = false, want true (nonArtifact fails, so Fear does not restrict it)")
	}
	if g.CanBlock(attacker, plainBlocker) {
		t.Error("CanBlock(fear attacker, plain nonartifact blocker) = true, want false")
	}
}

// TestCanBlockIntimidateAttacker proves the Intimidate keyword's own
// CantBlockBy synthesis ("ValidBlocker$ Creature.nonArtifact+!SharesColorWith",
// CardFactoryUtil.java:3940) end to end: an artifact creature can still block
// (nonArtifact fails, same as Fear), a nonartifact creature sharing the
// attacker's own color can still block (!SharesColorWith fails), and a
// nonartifact creature of a different color cannot -- the new SharesColorWith
// property (valid.go) exercised by a real caller for the first time.
func TestCanBlockIntimidateAttacker(t *testing.T) {
	t.Parallel()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\nGolem\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]

	attackerDef := &compile.Card{Name: "Test Intimidate Attacker"}
	attackerDef.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	attackerDef.Faces[0].Power, attackerDef.Faces[0].Toughness = "2", "2"
	attackerDef.Faces[0].Keywords = []string{"Intimidate"}
	attackerDef.Faces[0].ManaCost = mana.MustParse("R")
	attacker := g.NewCard(attackerDef, a, engine.Battlefield)

	artifactDef := &compile.Card{Name: "Test Artifact Blocker"}
	artifactDef.Faces[0].Type = cardtype.Parse(reg, "Artifact Creature Golem")
	artifactDef.Faces[0].Power, artifactDef.Faces[0].Toughness = "2", "2"
	artifactBlocker := g.NewCard(artifactDef, b, engine.Battlefield)

	sameColorDef := &compile.Card{Name: "Test Same Color Blocker"}
	sameColorDef.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	sameColorDef.Faces[0].Power, sameColorDef.Faces[0].Toughness = "2", "2"
	sameColorDef.Faces[0].ManaCost = mana.MustParse("R")
	sameColorBlocker := g.NewCard(sameColorDef, b, engine.Battlefield)

	offColorDef := &compile.Card{Name: "Test Off Color Blocker"}
	offColorDef.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	offColorDef.Faces[0].Power, offColorDef.Faces[0].Toughness = "2", "2"
	offColorDef.Faces[0].ManaCost = mana.MustParse("U")
	offColorBlocker := g.NewCard(offColorDef, b, engine.Battlefield)

	if !g.CanBlock(attacker, artifactBlocker) {
		t.Error("CanBlock(intimidate attacker, artifact blocker) = false, want true (nonArtifact fails, so Intimidate does not restrict it)")
	}
	if !g.CanBlock(attacker, sameColorBlocker) {
		t.Error("CanBlock(intimidate attacker, same-color blocker) = false, want true (SharesColorWith holds, so !SharesColorWith fails)")
	}
	if g.CanBlock(attacker, offColorBlocker) {
		t.Error("CanBlock(intimidate attacker, off-color blocker) = true, want false")
	}
}

// TestCanBlockIslandwalkAttacker proves Landwalk's own CantBlockBy synthesis
// (landwalkType/matchesValidDefender, staticability.go): a creature with
// K:Landwalk:Island is unblockable exactly while its defender controls an
// Island, and freely blockable before one enters -- CardFactoryUtil.java's
// own "Mode$ CantBlockBy | ValidAttacker$ Creature.Self | ValidDefender$
// Player.controlsIsland" end to end.
func TestCanBlockIslandwalkAttacker(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	attacker := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Landwalk:Island"), a, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)

	if !g.CanBlock(attacker, blocker) {
		t.Error("CanBlock(islandwalk attacker, blocker) = false, want true -- defender controls no Island yet")
	}

	g.NewCard(landDef(t, "Island", "Basic Land Island"), b, engine.Battlefield)

	if g.CanBlock(attacker, blocker) {
		t.Error("CanBlock(islandwalk attacker, blocker) = true, want false -- defender now controls an Island")
	}
}

// TestCanBlockIslandwalkAttackerIgnoresAttackersOwnIsland proves
// matchesValidDefender checks the BLOCKER's own controller, not the
// attacker's: an Island under the attacker's control (not the defender's)
// must not trigger the restriction.
func TestCanBlockIslandwalkAttackerIgnoresAttackersOwnIsland(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	attacker := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Landwalk:Island"), a, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	g.NewCard(landDef(t, "Island", "Basic Land Island"), a, engine.Battlefield)

	if !g.CanBlock(attacker, blocker) {
		t.Error("CanBlock(islandwalk attacker, blocker) = false, want true -- the attacker's own Island must not restrict the defender's blocker")
	}
}

// TestCanBlockProtectionFromColorAttacker proves Protection's own
// natural-language color form ("K:Protection from red," 154 of roughly 219
// real corpus lines) end to end: a red blocker cannot block, a blue one can.
func TestCanBlockProtectionFromColorAttacker(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	attacker := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Protection from red"), a, engine.Battlefield)
	redBlocker := g.NewCard(creatureDefManaCost(t, "R"), b, engine.Battlefield)
	blueBlocker := g.NewCard(creatureDefManaCost(t, "U"), b, engine.Battlefield)

	if g.CanBlock(attacker, redBlocker) {
		t.Error("CanBlock(protection-from-red attacker, red blocker) = true, want false")
	}
	if !g.CanBlock(attacker, blueBlocker) {
		t.Error("CanBlock(protection-from-red attacker, blue blocker) = false, want true")
	}
}

// TestCanBlockProtectionFromArtifactAttacker proves Protection's other real
// shape, the colon-structured form ("K:Protection:Artifact," 65 real
// lines): an artifact blocker cannot block, a plain one can.
func TestCanBlockProtectionFromArtifactAttacker(t *testing.T) {
	t.Parallel()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\nGolem\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	attacker := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Protection:Artifact"), a, engine.Battlefield)

	artifactDef := &compile.Card{Name: "Test Artifact Blocker"}
	artifactDef.Faces[0].Type = cardtype.Parse(reg, "Artifact Creature Golem")
	artifactDef.Faces[0].Power, artifactDef.Faces[0].Toughness = "2", "2"
	artifactBlocker := g.NewCard(artifactDef, b, engine.Battlefield)
	plainBlocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)

	if g.CanBlock(attacker, artifactBlocker) {
		t.Error("CanBlock(protection-from-artifact attacker, artifact blocker) = true, want false")
	}
	if !g.CanBlock(attacker, plainBlocker) {
		t.Error("CanBlock(protection-from-artifact attacker, plain blocker) = false, want true")
	}
}

// TestCanBlockProtectionFromEverythingAttacker proves the unconditional
// shape (Java's own getProtectionValid returns an empty validSource,
// CardFactoryUtil then omits ValidBlocker$ entirely): unblockable by
// anything.
func TestCanBlockProtectionFromEverythingAttacker(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	attacker := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Protection from everything"), a, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)

	if g.CanBlock(attacker, blocker) {
		t.Error("CanBlock(protection-from-everything attacker, any blocker) = true, want false")
	}
}

// TestCanBlockSkulkAttacker proves K:Skulk (CR 702.118a, "can't be blocked
// by creatures with greater power"): skulkBlocks (staticability.go) reads
// both cards' own Power() -- already the full continuous-effect-folded
// value, not the printed one -- so a strictly-greater-power blocker cannot
// block, but an equal or lesser power one can.
func TestCanBlockSkulkAttacker(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	attacker := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Skulk"), a, engine.Battlefield)
	strongerBlocker := g.NewCard(creatureDefPT(t, "3", "3"), b, engine.Battlefield)
	equalBlocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	weakerBlocker := g.NewCard(creatureDefPT(t, "1", "1"), b, engine.Battlefield)

	if g.CanBlock(attacker, strongerBlocker) {
		t.Error("CanBlock(skulk attacker, power-3 blocker) = true, want false -- greater power")
	}
	if !g.CanBlock(attacker, equalBlocker) {
		t.Error("CanBlock(skulk attacker, power-2 blocker) = false, want true -- equal power, not greater")
	}
	if !g.CanBlock(attacker, weakerBlocker) {
		t.Error("CanBlock(skulk attacker, power-1 blocker) = false, want true -- lesser power")
	}
}

// TestCanBlockSkulkAppliesContinuousPowerBoost proves skulkBlocks reads
// Power() (card.go), the layer-folded value, not a printed one: an anthem
// boosting the blocker's own power past the attacker's makes it unable to
// block, even though its printed power alone would not be.
func TestCanBlockSkulkAppliesContinuousPowerBoost(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.Player(a).Life, g.Player(b).Life = 20, 20
	attacker := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Skulk"), a, engine.Battlefield)
	g.NewCard(continuousDef(t, "Test Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 2"), b, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "1", "1"), b, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if g.CanBlock(attacker, blocker) {
		t.Error("CanBlock(skulk attacker, anthem-boosted power-3 blocker) = true, want false")
	}
}

// cantBlockByAuraDef builds a *compile.Card for an Aura carrying a real,
// literal S:Mode$ CantBlockBy line written on the Aura itself rather than
// synthesized from a keyword -- ValidAttacker$ Creature.EnchantedBy, the
// second most common real shape after Creature.Self (the removed port log's "Block
// legality" section has the corpus count) -- compiled through the real
// pipeline the same reason etbTriggerCreatureDef (trigger_test.go) is.
func cantBlockByAuraDef(t *testing.T) *compile.Card {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: "Test Unblockable Aura"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Test Unblockable Aura"
	raw.Faces[0].Type = cardtype.Parse(reg, "Enchantment Aura")
	raw.Faces[0].Statics = []string{
		"Mode$ CantBlockBy | ValidAttacker$ Creature.EnchantedBy | Secondary$ True",
	}
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}
	return c
}

// TestCanBlockRealCantBlockByLineOnAnotherHost proves cantBlockBy
// (staticability.go) walks every battlefield permanent as a possible host,
// not just the attacker's own card: the Aura's own static ability, not a
// keyword the attacker carries, is what makes it unblockable here.
func TestCanBlockRealCantBlockByLineOnAnotherHost(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	aura := g.NewCard(cantBlockByAuraDef(t), a, engine.Battlefield)
	g.Attach(aura, attacker)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)

	if g.CanBlock(attacker, blocker) {
		t.Error("CanBlock(enchanted-unblockable attacker, ordinary blocker) = true, want false")
	}

	unenchanted := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	if !g.CanBlock(unenchanted, blocker) {
		t.Error("CanBlock(plain attacker, ordinary blocker) = false, want true -- only the enchanted creature is restricted")
	}
}

// TestDeclareCombatBlockersRejectsIllegalCantBlockByPairing proves
// DeclareCombatBlockers (block.go) checks the controller's answer against
// CanBlock: a grounded creature declared blocking a flier is an illegal
// declaration (CR 509.1b, ADR-0024), an error with nothing applied, not a
// silently dropped pairing.
func TestDeclareCombatBlockersRejectsIllegalCantBlockByPairing(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Flying"), a, engine.Battlefield)
	ground := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, ac)

	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: ground, Attacker: attacker}})
	_, err := g.DeclareCombatBlockers(bc)
	ill := wantIllegal(t, err, "CR 509.1b")
	if len(ill.Cards) != 2 || ill.Cards[0] != ground || ill.Cards[1] != attacker {
		t.Errorf("cards = %v, want the blocker and the attacker", ill.Cards)
	}
	if len(g.Blocks()) != 0 {
		t.Errorf("Blocks() = %v, want none", g.Blocks())
	}

	// The legal answer -- no block -- goes through.
	bc.QueueBlocks(nil)
	if got := declareBlockers(t, g, bc); got != nil {
		t.Errorf("DeclareCombatBlockers() = %v, want nil", got)
	}
}
