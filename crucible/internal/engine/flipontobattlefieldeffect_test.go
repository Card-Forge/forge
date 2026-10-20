package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// battleDef builds just enough of a *compile.Card for Card.Type() to answer
// "is this a Battle" -- flipCandidates' own last real branch
// (sharesCoreType) is reached only by a battlefield permanent core type none
// of Creature/Land/Planeswalker/Artifact/Enchantment names, and Battle is
// the only one left.
func battleDef(t *testing.T, name string) *compile.Card {
	t.Helper()
	def := &compile.Card{Name: name}
	def.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), "Battle Siege")
	def.Faces[0].Defense = "5"
	return def
}

// newSeededTwoPlayerGame is newTwoPlayerGame with a chosen RNG seed --
// FlipOntoBattlefield's own outcome depends on the exact draw sequence
// (pkg/javarand, ADR-0010), so its tests need to pick a seed that lands on
// each branch rather than the fixed seed 1 every other pack test shares.
func newSeededTwoPlayerGame(t *testing.T, seed int64) (*engine.Game, engine.PlayerID, engine.PlayerID) {
	t.Helper()
	g := engine.NewGame(nil, javarand.New(seed), []string{"a", "b"})
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	return g, p, other
}

// remembered reads back host's Memory.Remembered() as CardIDs, skipping
// anything that is not a card (nothing FlipOntoBattlefield ever remembers
// is), for assertions that do not care about EntityID's own wrapping.
func remembered(t *testing.T, g *engine.Game, host engine.CardID) []engine.CardID {
	t.Helper()
	var out []engine.CardID
	for _, e := range g.Card(host).Memory.Remembered() {
		id, ok := e.AsCard()
		if !ok {
			t.Fatalf("remembered a non-card entity %v", e)
		}
		out = append(out, id)
	}
	return out
}

// TestFlipOntoBattlefieldRejectsAllowRandom proves AllowRandom$ fails loudly
// (PORT-8/GO-7): 0 real corpus lines confirm its shape, so this port does
// not guess at chooseCardsForEffect's own isOptional contract for it.
func TestFlipOntoBattlefieldRejectsAllowRandom(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := engine.NewScriptedController() // no QueueCardChoice: the reject fires before any choice is asked for.
	_, err := resolveNow(t, g, p, c, nil, "DB$ FlipOntoBattlefield | AllowRandom$ True")
	if err == nil || !strings.Contains(err.Error(), "AllowRandom$ not resolvable yet") {
		t.Fatalf("err = %v, want it to reject AllowRandom$", err)
	}
}

// TestFlipOntoBattlefieldRejectsEmptyBattlefield is Falling Star's own real
// shape: a sorcery, not yet a permanent, resolving against a battlefield
// that may hold nothing at all -- FlipOntoBattlefieldEffect.java:36's own
// tgtBox.getFirst() would NPE there; this port fails closed with an error
// instead.
func TestFlipOntoBattlefieldRejectsEmptyBattlefield(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	def := etbChainDef(t, "Test Flip Empty", "DB$ FlipOntoBattlefield")
	host := g.NewCard(def, p, engine.Hand)
	face := def.Faces[0]
	sub := face.Triggers[0].Subs[0]
	api, ok := engine.APIByName(sub.Ability.Name)
	if !ok {
		t.Fatalf("unknown API %q", sub.Ability.Name)
	}
	g.PushAbility(engine.Ability{API: api, Source: host, Controller: p, Params: sub.Ability, Amounts: face.Amounts})
	err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController())
	if err == nil || !strings.Contains(err.Error(), "no permanent on the battlefield") {
		t.Fatalf("err = %v, want it to reject an empty battlefield", err)
	}
}

// TestFlipOntoBattlefieldRejectsNonAuraEnchantmentLocation is the PORT-8
// guard: FlipOntoBattlefieldEffect.java:109's own neighbor filter always
// matches every permanent once the chosen landing spot is a non-Aura
// enchantment (see flipontobattlefieldeffect.go's own doc comment) --
// rejected rather than reproduced. A planeswalker or artifact spot does not
// trigger the bug and is not rejected (TestFlipOntoBattlefieldArtifactLocationResolves,
// below).
func TestFlipOntoBattlefieldRejectsNonAuraEnchantmentLocation(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	loc := g.NewCard(worldDef(t, "Test World Enchantment"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{loc})
	_, err := resolveNow(t, g, p, c, nil, "DB$ FlipOntoBattlefield")
	if err == nil || !strings.Contains(err.Error(), "non-Aura enchantment") {
		t.Fatalf("err = %v, want it to reject a non-Aura enchantment location", err)
	}
}

// TestFlipOntoBattlefieldArtifactLocationResolves proves the PORT-8 rejection
// above stays narrow: FlipOntoBattlefieldEffect.java:109's own bug only fires
// for a non-Aura-enchantment landing spot (see flipontobattlefieldeffect.go's
// own doc comment for why); a plain artifact -- Chaos Orb's own real shape,
// choosing itself or another artifact as the landing spot -- resolves
// normally. Seed 2048's own NOFLIP draw; only resolving without error
// matters.
func TestFlipOntoBattlefieldArtifactLocationResolves(t *testing.T) {
	t.Parallel()

	g, p, _ := newSeededTwoPlayerGame(t, 2048)
	loc := g.NewCard(equipmentDef(t), p, engine.Battlefield) // Artifact Equipment, unattached.
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{loc})
	if _, err := resolveNow(t, g, p, c, nil, "DB$ FlipOntoBattlefield"); err != nil {
		t.Fatalf("resolve: %v", err)
	}
}

// TestFlipOntoBattlefieldNeverFlips is seed 2048's own draw: the first
// Float32() draw exceeds chanceToFlip, so nothing is remembered and no
// further draw happens (FlipOntoBattlefieldEffect.java:51-55) -- the lone
// creature landing spot (no neighbor) still has to resolve without error
// first.
func TestFlipOntoBattlefieldNeverFlips(t *testing.T) {
	t.Parallel()

	g, p, _ := newSeededTwoPlayerGame(t, 2048)
	loc := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{loc})
	host, err := resolveNow(t, g, p, c, nil, "DB$ FlipOntoBattlefield")
	if err != nil {
		t.Fatalf("resolve: %v", err)
	}
	if got := remembered(t, g, host); len(got) != 0 {
		t.Errorf("remembered = %v, want none (a miss should not flip at all)", got)
	}
}

// TestFlipOntoBattlefieldHitsBothCards is seed 3: the flip happens and lands
// on both the chosen spot and its one same-type battlefield neighbor.
func TestFlipOntoBattlefieldHitsBothCards(t *testing.T) {
	t.Parallel()

	g, p, _ := newSeededTwoPlayerGame(t, 3)
	loc := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	neighbor := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{loc})
	host, err := resolveNow(t, g, p, c, nil, "DB$ FlipOntoBattlefield")
	if err != nil {
		t.Fatalf("resolve: %v", err)
	}
	got := remembered(t, g, host)
	want := []engine.CardID{loc, neighbor}
	if len(got) != len(want) || got[0] != want[0] || got[1] != want[1] {
		t.Errorf("remembered = %v, want %v", got, want)
	}
}

// TestFlipOntoBattlefieldHitsNeighborOnly is seed 16: the flip lands on one
// card, and it is the neighbor rather than the chosen spot -- proving both
// the random pick between the two (randomIndex, random.go) and
// FlipOntoBattlefieldEffect.java:125-129's own "leftmost" quirk this port
// preserves (flipNeighbor's own doc comment): the chosen spot sits first in
// its controller's battlefield order, has no left neighbor, and so lands on
// its right neighbor instead of on itself.
func TestFlipOntoBattlefieldHitsNeighborOnly(t *testing.T) {
	t.Parallel()

	g, p, _ := newSeededTwoPlayerGame(t, 16)
	loc := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	neighbor := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{loc})
	host, err := resolveNow(t, g, p, c, nil, "DB$ FlipOntoBattlefield")
	if err != nil {
		t.Fatalf("resolve: %v", err)
	}
	got := remembered(t, g, host)
	if len(got) != 1 || got[0] != neighbor {
		t.Errorf("remembered = %v, want [%d] (the neighbor, not the chosen spot %d)", got, neighbor, loc)
	}
}

// TestFlipOntoBattlefieldFlipsButMissesEverything is seed 2: the card turns
// over at least once, but the final draw lands on neither the spot nor a
// neighbor, so nothing is remembered even though a flip happened.
func TestFlipOntoBattlefieldFlipsButMissesEverything(t *testing.T) {
	t.Parallel()

	g, p, _ := newSeededTwoPlayerGame(t, 2)
	loc := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{loc})
	host, err := resolveNow(t, g, p, c, nil, "DB$ FlipOntoBattlefield")
	if err != nil {
		t.Fatalf("resolve: %v", err)
	}
	if got := remembered(t, g, host); len(got) != 0 {
		t.Errorf("remembered = %v, want none", got)
	}
}

// TestFlipOntoBattlefieldAttachmentNeighbor is seed 4097: the chosen spot
// carries an Equipment, the attachment lottery hits, and the flip lands on
// both -- FlipOntoBattlefieldEffect.java:119-123's own attachment branch,
// checked ahead of the plain battlefield-order neighbor.
func TestFlipOntoBattlefieldAttachmentNeighbor(t *testing.T) {
	t.Parallel()

	g, p, _ := newSeededTwoPlayerGame(t, 4097)
	loc := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	equipment := g.NewCard(equipmentDef(t), p, engine.Battlefield)
	g.Attach(equipment, loc)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{loc})
	host, err := resolveNow(t, g, p, c, nil, "DB$ FlipOntoBattlefield")
	if err != nil {
		t.Fatalf("resolve: %v", err)
	}
	got := remembered(t, g, host)
	want := []engine.CardID{loc, equipment}
	if len(got) != len(want) || got[0] != want[0] || got[1] != want[1] {
		t.Errorf("remembered = %v, want %v", got, want)
	}
}

// TestFlipOntoBattlefieldLandNeighbor is seed 2048's own NOFLIP draw again,
// this time over two lands -- covering flipCandidates' own isLand branch,
// which the creature-shaped tests above never reach. Nothing is remembered
// either way (the same miss as TestFlipOntoBattlefieldNeverFlips), so this
// only has to resolve without error.
func TestFlipOntoBattlefieldLandNeighbor(t *testing.T) {
	t.Parallel()

	g, p, _ := newSeededTwoPlayerGame(t, 2048)
	loc := g.NewCard(landDef(t, "Test Land A", "Land"), p, engine.Battlefield)
	g.NewCard(landDef(t, "Test Land B", "Land"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{loc})
	if _, err := resolveNow(t, g, p, c, nil, "DB$ FlipOntoBattlefield"); err != nil {
		t.Fatalf("resolve: %v", err)
	}
}

// TestFlipOntoBattlefieldAttachmentLotteryMiss is seed 1 again, this time
// with the chosen spot carrying an attachment the lottery does not pick
// (attach=0.7309 > the 50% hitAttachment threshold): flipNeighbor falls
// through to the plain battlefield-order search, which still finds the
// attachment as tgtLoc's own index-adjacent candidate, but the final draw
// (idx=0) lands on the chosen spot itself rather than on it.
func TestFlipOntoBattlefieldAttachmentLotteryMiss(t *testing.T) {
	t.Parallel()

	g, p, _ := newSeededTwoPlayerGame(t, 1)
	loc := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.Attach(g.NewCard(equipmentDef(t), p, engine.Battlefield), loc)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{loc})
	host, err := resolveNow(t, g, p, c, nil, "DB$ FlipOntoBattlefield")
	if err != nil {
		t.Fatalf("resolve: %v", err)
	}
	got := remembered(t, g, host)
	if len(got) != 1 || got[0] != loc {
		t.Errorf("remembered = %v, want [%d] (the chosen spot itself)", got, loc)
	}
}

// TestFlipOntoBattlefieldTrueLeftNeighbor covers flipNeighbor's own
// `loc > 0` branch: a real left neighbor, rather than the "leftmost" quirk
// the other tests above exercise, since the chosen spot here sits second
// in its controller's battlefield order. Seed 2048's own NOFLIP draw again;
// only resolving without error matters.
func TestFlipOntoBattlefieldTrueLeftNeighbor(t *testing.T) {
	t.Parallel()

	g, p, _ := newSeededTwoPlayerGame(t, 2048)
	g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	loc := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{loc})
	if _, err := resolveNow(t, g, p, c, nil, "DB$ FlipOntoBattlefield"); err != nil {
		t.Fatalf("resolve: %v", err)
	}
}

// TestFlipOntoBattlefieldBattleFallback covers flipCandidates' own final
// fallback, Card.sharesCardTypeWith -- reached only when the chosen spot is
// none of Creature/Land/attached-to-something/planeswalker/artifact/
// non-Aura-enchantment, which among real battlefield permanents leaves only
// Battle. Seed 2048's own NOFLIP draw again; only resolving without error
// matters.
func TestFlipOntoBattlefieldBattleFallback(t *testing.T) {
	t.Parallel()

	g, p, other := newSeededTwoPlayerGame(t, 2048)
	loc := g.NewCard(battleDef(t, "Test Battle A"), p, engine.Battlefield)
	g.Card(loc).ProtectingPlayer = other
	b := g.NewCard(battleDef(t, "Test Battle B"), p, engine.Battlefield)
	g.Card(b).ProtectingPlayer = other
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{loc})
	if _, err := resolveNow(t, g, p, c, nil, "DB$ FlipOntoBattlefield"); err != nil {
		t.Fatalf("resolve: %v", err)
	}
}

// TestFlipOntoBattlefieldAttachedLocationSibling covers flipCandidates' own
// last real branch: the chosen spot is itself attached to something (an Aura
// enchanting a creature), so candidates narrow to its siblings -- another
// Aura attached to the identical host -- rather than to the host's own
// battlefield neighbors. Seed 2048's own NOFLIP draw again; only resolving
// without error matters here.
func TestFlipOntoBattlefieldAttachedLocationSibling(t *testing.T) {
	t.Parallel()

	g, p, _ := newSeededTwoPlayerGame(t, 2048)
	creatureHost := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	loc := g.NewCard(auraDef(t), p, engine.Battlefield)
	g.Attach(loc, creatureHost)
	sibling := g.NewCard(auraDef(t), p, engine.Battlefield)
	g.Attach(sibling, creatureHost)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{loc})
	if _, err := resolveNow(t, g, p, c, nil, "DB$ FlipOntoBattlefield"); err != nil {
		t.Fatalf("resolve: %v", err)
	}
}

// TestFlipOntoBattlefieldHitChanceGrowsWithRowSize is seed 8: the hit draw is
// 0.5969, which a flat 0.70 would turn into a one-card hit. The location and the
// host creature give min(1, 0.45 + 0.07*2) = 0.59, so the flip misses
// (FlipOntoBattlefieldEffect.java:41-42, #12183).
func TestFlipOntoBattlefieldHitChanceGrowsWithRowSize(t *testing.T) {
	t.Parallel()

	g, p, _ := newSeededTwoPlayerGame(t, 8)
	loc := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{loc})
	host, err := resolveNow(t, g, p, c, nil, "DB$ FlipOntoBattlefield")
	if err != nil {
		t.Fatalf("resolve: %v", err)
	}
	if got := remembered(t, g, host); len(got) != 0 {
		t.Errorf("remembered = %v, want none (hit chance is 0.59 for a row of two)", got)
	}
}
