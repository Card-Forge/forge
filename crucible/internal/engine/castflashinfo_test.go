package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// withStatic appends the static line to def's first face.
func withStatic(t *testing.T, def *compile.Card, line string) *compile.Card {
	t.Helper()
	def.Faces[0].Statics = append(def.Faces[0].Statics, scriptDef(t, "x", "Enchantment", line).Faces[0].Statics...)
	return def
}

// A CastWithFlash line whose ValidSA$ names XCost lets the cast begin out of
// timing and is decided once X is announced (StaticAbilityCastWithFlash
// .anyWithFlashNeedsInfo, SpellAbilityProperty.java:33-36).
func TestCastWithFlashXCostIsCheckedOnceXIsAnnounced(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name string
		x    int
		want bool
	}{
		{"X at the limit", 3, true},
		{"X over the limit", 4, false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGame(t)
			def := withStatic(t, creatureDefManaCost(t, "X G"),
				"S:Mode$ CastWithFlash | ValidCard$ Card.Self | ValidSA$ Spell.XCostLE3 | EffectZone$ All | Caster$ You")
			card := g.NewCard(def, p, engine.Hand)
			g.SetTurnState(1, other, engine.Main1)
			g.Player(p).ManaPool.Add(mana.Green, 6)
			c := engine.NewScriptedController()
			c.QueuePayX(tc.x)
			queueXPayGeneric(c, mana.ShardG, tc.x)

			if got := g.CastSpell(p, card, c); got != tc.want {
				t.Fatalf("CastSpell with X=%d on the opponent's turn = %v, want %v", tc.x, got, tc.want)
			}
			if !tc.want && g.Player(p).ManaPool.Total() != 6 {
				t.Errorf("a refused cast spent mana: pool = %d, want 6", g.Player(p).ManaPool.Total())
			}
		})
	}
}

// An XCost line decides a permanent spell too, and an Aura, whose targets are
// chosen before X.
func TestCastWithFlashXCostWithoutAnXSymbolReadsZero(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	def := withStatic(t, creatureDefCost(t, "No X", "G"),
		"S:Mode$ CastWithFlash | ValidCard$ Card.Self | ValidSA$ Spell.XCostEQ0 | EffectZone$ All | Caster$ You")
	card := g.NewCard(def, p, engine.Hand)
	g.SetTurnState(1, other, engine.Main1)
	g.Player(p).ManaPool.Add(mana.Green, 1)
	if !g.CastSpell(p, card, engine.NewScriptedController()) {
		t.Fatal("a spell with no X announces none, which reads as 0: CastSpell = false, want true")
	}
}

// IsTargeting Valid <spec>: the flash is granted when a target matches
// (SpellAbilityProperty.java:234-244, Flash Photography).
func TestCastWithFlashIsTargetingReadsTheChosenTargets(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name   string
		mine   bool
		wantOK bool
	}{
		{"targets a permanent you control", true, true},
		{"targets an opponent's permanent", false, false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGame(t)
			own := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
			theirs := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
			def := withStatic(t, scriptDef(t, "Test Photo", "Sorcery", "A:SP$ Pump | ValidTgts$ Creature | NumAtt$ 1"),
				"S:Mode$ CastWithFlash | ValidCard$ Card.Self | ValidSA$ Spell.IsTargeting Valid Permanent.YouCtrl | EffectZone$ All | Caster$ You")
			def.Faces[0].ManaCost = mana.MustParse("G")
			card := g.NewCard(def, p, engine.Hand)
			g.SetTurnState(1, other, engine.Main1)
			g.Player(p).ManaPool.Add(mana.Green, 1)
			target := own
			if !tc.mine {
				target = theirs
			}
			c := engine.NewScriptedController()
			c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})

			if got := g.CastSpell(p, card, c); got != tc.wantOK {
				t.Fatalf("CastSpell = %v, want %v", got, tc.wantOK)
			}
		})
	}
}

// MayFlashCost (GameActionUtil.java:514-517): out of timing, the spell is cast
// for its cost plus the keyword's mana if the caster agrees; in timing nothing is
// asked.
func TestMayFlashCostIsAnOptionalAdditionalCost(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name    string
		confirm bool
		pool    int
		want    bool
		left    int
	}{
		{"paid", true, 3, true, 0},
		{"declined", false, 3, false, 3},
		{"cannot afford it", true, 2, false, 2},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGame(t)
			def := creatureDefCost(t, "Rout Like", "G")
			def.Faces[0].Keywords = append(def.Faces[0].Keywords, "MayFlashCost:2")
			card := g.NewCard(def, p, engine.Hand)
			g.SetTurnState(1, other, engine.Main1)
			g.Player(p).ManaPool.Add(mana.Green, tc.pool)
			c := engine.NewScriptedController()
			c.QueueConfirmPayCost(tc.confirm)
			queueXPayGeneric(c, mana.ShardG, 2)

			if got := g.CastSpell(p, card, c); got != tc.want {
				t.Fatalf("CastSpell = %v, want %v", got, tc.want)
			}
			if got := g.Player(p).ManaPool.Total(); got != tc.left {
				t.Errorf("mana left = %d, want %d", got, tc.left)
			}
		})
	}
}

// MayFlashCost: Behold<1/Elf> (Molten Exhale's Behold<1/Dragon>) is an additional
// cost paid by choosing a matching permanent you control or revealing a matching
// card in hand (CostBehold, a CostReveal over Hand and Battlefield). It is offered
// only when one exists and the caster agrees.
func TestMayFlashCostBeholdNeedsAMatchingCardAndConsent(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name    string
		zone    engine.ZoneType
		confirm bool
		want    bool
	}{
		{"elf on the battlefield", engine.Battlefield, true, true},
		{"elf in hand", engine.Hand, true, true},
		{"declined", engine.Battlefield, false, false},
		{"elf in the graveyard", engine.Graveyard, true, false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGame(t)
			def := creatureDefCost(t, "Exhale Like", "G")
			def.Faces[0].Keywords = append(def.Faces[0].Keywords, "MayFlashCost:Behold<1/Elf>")
			card := g.NewCard(def, p, engine.Hand)
			elf := g.NewCard(scriptDef(t, "Test Elf", "Creature Elf"), p, tc.zone)
			g.SetTurnState(1, other, engine.Main1)
			g.Player(p).ManaPool.Add(mana.Green, 1)
			c := engine.NewScriptedController()
			c.QueueConfirmPayCost(tc.confirm)
			// The beheld card is the cast's Revealed list (payChoiceParts).
			c.QueueCardChoice([]engine.CardID{elf})
			if got := g.CastSpell(p, card, c); got != tc.want {
				t.Fatalf("CastSpell = %v, want %v", got, tc.want)
			}
		})
	}
}

// MayFlashCost with a cost this port cannot pay is never offered: the spell
// stays uncastable out of timing.
func TestMayFlashCostWithANonManaCostIsNotOffered(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	def := creatureDefCost(t, "Exhale Like", "G")
	def.Faces[0].Keywords = append(def.Faces[0].Keywords, "MayFlashCost:Behold<1/Dragon>")
	card := g.NewCard(def, p, engine.Hand)
	g.SetTurnState(1, other, engine.Main1)
	g.Player(p).ManaPool.Add(mana.Green, 3)
	c := engine.NewScriptedController()
	c.QueueConfirmPayCost(true)
	if g.CastSpell(p, card, c) {
		t.Error("a MayFlashCost this port cannot pay was offered")
	}
}

// MayFlashSac: cast where a sorcery could not be cast, the permanent is
// sacrificed at the next cleanup step (CardFactoryUtil.java:2025-2038); cast
// where a sorcery could be, nothing is scheduled.
func TestMayFlashSacSacrificesAtCleanupOnlyWhenCastOutOfTiming(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name       string
		ownTurn    bool
		wantGone   bool
		wantOption bool
	}{
		{"opponent's turn", false, true, true},
		{"own main phase", true, false, true},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGame(t)
			libraryCards(t, g, p, 5)
			libraryCards(t, g, other, 5)
			def := creatureDefCost(t, "Climb Like", "G")
			def.Faces[0].Keywords = append(def.Faces[0].Keywords, "MayFlashSac")
			card := g.NewCard(def, p, engine.Hand)
			if tc.ownTurn {
				g.SetTurnState(1, p, engine.Main1)
			} else {
				g.SetTurnState(1, other, engine.Main1)
			}
			g.Player(p).ManaPool.Add(mana.Green, 1)
			c := engine.NewScriptedController()
			if !g.CastSpell(p, card, c) {
				t.Fatal("CastSpell = false, want true")
			}
			if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if z := g.Card(card).Zone; z != engine.Battlefield {
				t.Fatalf("zone after resolving = %v, want Battlefield", z)
			}
			advanceUntil(t, g, c, func() bool { return g.ActivePhase() == engine.Cleanup })
			advanceUntil(t, g, c, func() bool { return g.ActivePhase() == engine.Upkeep })
			gone := g.Card(card).Zone == engine.Graveyard
			if gone != tc.wantGone {
				t.Errorf("sacrificed after cleanup = %v, want %v (zone %v)", gone, tc.wantGone, g.Card(card).Zone)
			}
		})
	}
}
