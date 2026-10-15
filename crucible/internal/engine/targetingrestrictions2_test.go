package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// recordingTargets is a ScriptedController that remembers who was asked to
// choose targets and from which candidates.
type recordingTargets struct {
	*engine.ScriptedController
	decider    engine.PlayerID
	candidates []engine.EntityID
}

func (r *recordingTargets) ChooseTargets(g *engine.Game, decider engine.PlayerID, valid []engine.EntityID, lo, hi int) []engine.EntityID {
	r.decider, r.candidates = decider, slices.Clone(valid)
	return r.ScriptedController.ChooseTargets(g, decider, valid, lo, hi)
}

func entities(ids ...engine.CardID) []engine.EntityID {
	out := make([]engine.EntityID, len(ids))
	for i, id := range ids {
		out[i] = engine.CardEntity(id)
	}
	return out
}

// restrictionGame is a game over the real card DB (its creature-type registry
// is what TargetsWithSameCreatureType reads).
func restrictionGame(t *testing.T) (*engine.Game, engine.PlayerID, engine.PlayerID) {
	t.Helper()
	return newTwoPlayerGameOn(t, scenarioDB(t))
}

// TargetsWithSameCreatureType$ and TargetsWithoutSameCreatureType$
// (SpellAbility.java:1559-1573, Secret Tunnel and Rivals' Duel): the two chosen
// creatures must share a creature type, or must share none. A Changeling has
// every type.
func TestTargetsWithSharedCreatureTypeRestrictions(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name   string
		param  string
		second string // type line of the second creature
		kw     string
		want   bool
	}{
		{"same type, both Elves", "TargetsWithSameCreatureType", "Creature Elf", "", true},
		{"same type, one without", "TargetsWithSameCreatureType", "Creature", "", false},
		{"same type, a Changeling", "TargetsWithSameCreatureType", "Creature", "K:Changeling", true},
		{"no shared type, both Elves", "TargetsWithoutSameCreatureType", "Creature Elf", "", false},
		{"no shared type, one without", "TargetsWithoutSameCreatureType", "Creature", "", true},
		{"no shared type, a Changeling", "TargetsWithoutSameCreatureType", "Creature", "K:Changeling", false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := restrictionGame(t)
			first := g.NewCard(scriptDef(t, "First Elf", "Creature Elf"), p, engine.Battlefield)
			var lines []string
			if tc.kw != "" {
				lines = append(lines, tc.kw)
			}
			second := g.NewCard(scriptDef(t, "Second", tc.second, lines...), p, engine.Battlefield)
			engine.CheckStateBasedActions(g, engine.NewScriptedController())
			spell := g.NewCard(scriptDef(t, "Pair Spell", "Sorcery",
				"A:SP$ Pump | ValidTgts$ Creature | "+tc.param+"$ True | TargetMin$ 2 | TargetMax$ 2 | NumAtt$ 1"), p, engine.Hand)
			c := engine.NewScriptedController()
			c.QueueTargets(entities(first, second))

			if got := g.CastSpell(p, spell, c); got != tc.want {
				t.Errorf("CastSpell = %v, want %v", got, tc.want)
			}
		})
	}
}

// TargetsForEachPlayer$ asks one target per player: two creatures of one
// controller cannot both be kept (TargetRestrictions.isForEachPlayer in
// SpellAbility.canTarget), and a target that changes controller before the
// spell resolves is illegal then (TargetChoices.forEachControllerChanged).
func TestTargetsForEachPlayerKeepsOneTargetPerController(t *testing.T) {
	t.Parallel()

	g, p, other := restrictionGame(t)
	mineA := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	mineB := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	spell := g.NewCard(scriptDef(t, "Each Player Spell", "Sorcery",
		"A:SP$ Destroy | ValidTgts$ Creature | TargetsForEachPlayer$ True | TargetMin$ 0 | TargetMax$ 2"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets(entities(mineA, mineB, theirs))
	if !g.CastSpell(p, spell, c) {
		t.Fatal("CastSpell = false, want true")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(mineA).Zone; z != engine.Graveyard {
		t.Errorf("first creature zone = %v, want Graveyard", z)
	}
	if z := g.Card(mineB).Zone; z != engine.Battlefield {
		t.Errorf("second creature of the same controller zone = %v, want Battlefield: it was dropped", z)
	}
	if z := g.Card(theirs).Zone; z != engine.Graveyard {
		t.Errorf("the other player's creature zone = %v, want Graveyard", z)
	}
}

func TestTargetsForEachPlayerFizzlesATargetThatChangedController(t *testing.T) {
	t.Parallel()

	g, p, other := restrictionGame(t)
	mine := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	destroy := g.NewCard(scriptDef(t, "Each Player Spell", "Sorcery",
		"A:SP$ Destroy | ValidTgts$ Creature | TargetsForEachPlayer$ True | TargetMin$ 0 | TargetMax$ 2"), p, engine.Hand)
	steal := g.NewCard(scriptDef(t, "Steal", "Instant",
		"A:SP$ GainControl | ValidTgts$ Creature.OppCtrl"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets(entities(mine, theirs))
	if !g.CastSpell(p, destroy, c) {
		t.Fatal("CastSpell of the sorcery = false, want true")
	}
	c.QueueTargets(entities(theirs))
	if !g.CastSpell(p, steal, c) {
		t.Fatal("CastSpell of the instant = false, want true")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(mine).Zone; z != engine.Graveyard {
		t.Errorf("unmoved target zone = %v, want Graveyard", z)
	}
	if z := g.Card(theirs).Zone; z != engine.Battlefield {
		t.Errorf("target that changed controller zone = %v, want Battlefield: it is no longer a legal target", z)
	}
}

// TargetingPlayer$ makes that player choose the targets, and
// TargetingPlayerControls$ limits them to cards that player controls (Preacher,
// Echo Chamber).
func TestTargetingPlayerChoosesAmongTheirOwnPermanents(t *testing.T) {
	t.Parallel()

	g, p, other := restrictionGame(t)
	mine := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	spell := g.NewCard(scriptDef(t, "Opponent Picks", "Sorcery",
		"A:SP$ Pump | ValidTgts$ Creature | TargetingPlayer$ Player.Opponent | TargetingPlayerControls$ True | NumAtt$ 1"), p, engine.Hand)
	c := &recordingTargets{ScriptedController: engine.NewScriptedController()}
	c.QueueTargets(entities(theirs))

	if !g.CastSpell(p, spell, c) {
		t.Fatal("CastSpell = false, want true")
	}
	if c.decider != other {
		t.Errorf("targets chosen by player %v, want the opponent %v", c.decider, other)
	}
	if !slices.Equal(c.candidates, entities(theirs)) {
		t.Errorf("candidates = %v, want only the opponent's creature (not %v)", c.candidates, mine)
	}
}

// A TargetingPlayer$ naming no player leaves nobody to choose: the spell goes
// on the stack with no targets and fails loudly when it resolves, never a pick
// by the activator and never a silent "no legal target" (GO-7). Java has no
// answer either: chooseSingleEntityForEffect returns null for an empty list
// and setupTargets dereferences it (SpellAbility.java:2153-2161).
func TestTargetingPlayerNamingNobodyFailsWhenItResolves(t *testing.T) {
	t.Parallel()

	g, p, _ := restrictionGame(t)
	mine := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	spell := g.NewCard(scriptDef(t, "Nobody Picks", "Sorcery",
		"A:SP$ Pump | ValidTgts$ Creature | TargetingPlayer$ Player.Bogus | NumAtt$ 1"), p, engine.Hand)
	c := engine.NewScriptedController()
	if !g.CastSpell(p, spell, c) {
		t.Fatal("CastSpell = false, want true: the spell goes on the stack and fails when it resolves")
	}
	err := g.ResolveStack(engine.NewRegistry(), c)
	if err == nil || !strings.Contains(err.Error(), "TargetingPlayer$") {
		t.Fatalf("ResolveStack = %v, want the TargetingPlayer$ error", err)
	}
	if got, _ := g.Card(mine).Power(); got != 1 {
		t.Errorf("power = %d, want 1: nothing was pumped", got)
	}
}

// TargetsWithControllerProperty$ cmcLECardsInGraveyard (Drown in the Loch): a
// target's mana value may not exceed the cards in its controller's graveyard.
func TestTargetsWithControllerPropertyComparesToTheTargetsGraveyard(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		prop string
		want []string
	}{
		{"cmcLECardsInGraveyard", []string{"small"}},
		{"powerLECardsInGraveyard", []string{"small", "big"}},
	} {
		t.Run(tc.prop, func(t *testing.T) {
			t.Parallel()

			g, p, _ := restrictionGame(t)
			small := g.NewCard(creatureDefManaCost(t, "1"), p, engine.Battlefield)
			big := g.NewCard(creatureDefManaCost(t, "3"), p, engine.Battlefield)
			g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)
			names := map[engine.CardID]string{small: "small", big: "big"}
			spell := g.NewCard(scriptDef(t, "Graveyard Spell", "Sorcery",
				"A:SP$ Destroy | ValidTgts$ Creature | TargetsWithControllerProperty$ "+tc.prop), p, engine.Hand)
			c := &recordingTargets{ScriptedController: engine.NewScriptedController()}
			c.QueueTargets(entities(small))
			if !g.CastSpell(p, spell, c) {
				t.Fatal("CastSpell = false, want true")
			}
			var got []string
			for _, e := range c.candidates {
				if id, ok := e.AsCard(); ok && names[id] != "" {
					got = append(got, names[id])
				}
			}
			slices.Sort(got)
			slices.Sort(tc.want)
			if !slices.Equal(got, tc.want) {
				t.Errorf("candidates = %v, want %v", got, tc.want)
			}
		})
	}
}

// TargetsWithSharedCardType$ names cards the target must share a card type with
// (TargetsWithSharedTypes$ narrowing which types count).
func TestTargetsWithSharedCardTypeFiltersCandidates(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name   string
		extra  string
		wantIn []string
	}{
		{"any shared card type", "", []string{"artifact creature", "creature"}},
		{"only listed types", " | TargetsWithSharedTypes$ Artifact", []string{"artifact creature"}},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := restrictionGame(t)
			ref := g.NewCard(scriptDef(t, "Reference", "Artifact Creature"), p, engine.Battlefield)
			both := g.NewCard(scriptDef(t, "Both", "Artifact Creature"), p, engine.Battlefield)
			creature := g.NewCard(scriptDef(t, "Creature", "Creature"), p, engine.Battlefield)
			land := g.NewCard(namedLand(t, "Some Land"), p, engine.Battlefield)
			names := map[engine.CardID]string{both: "artifact creature", creature: "creature", land: "land", ref: "ref"}
			spell := g.NewCard(scriptDef(t, "Shared Spell", "Sorcery",
				"A:SP$ Pump | ValidTgts$ Permanent | TargetsWithSharedCardType$ Remembered"+tc.extra+" | NumAtt$ 1"), p, engine.Hand)
			g.Card(spell).Memory.Remember(engine.CardEntity(ref))
			c := &recordingTargets{ScriptedController: engine.NewScriptedController()}
			c.QueueTargets(entities(both))
			if !g.CastSpell(p, spell, c) {
				t.Fatal("CastSpell = false, want true")
			}
			var got []string
			for _, e := range c.candidates {
				if id, ok := e.AsCard(); ok && names[id] != "" && id != ref {
					got = append(got, names[id])
				}
			}
			slices.Sort(got)
			if !slices.Equal(got, tc.wantIn) {
				t.Errorf("candidates sharing a type = %v, want %v", got, tc.wantIn)
			}
		})
	}
}

// TargetsWithRelatedProperty$ compares the target to what an ancestor ability
// targeted; a root ability has none, so nothing qualifies (SpellAbility.java:1450-1454).
func TestTargetsWithRelatedPropertyWithoutAnAncestorTargetsNothing(t *testing.T) {
	t.Parallel()

	g, p, _ := restrictionGame(t)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	spell := g.NewCard(scriptDef(t, "Related Spell", "Sorcery",
		"A:SP$ Pump | ValidTgts$ Creature | TargetsWithRelatedProperty$ LEPower | NumAtt$ 1"), p, engine.Hand)
	if g.CastSpell(p, spell, engine.NewScriptedController()) {
		t.Error("CastSpell = true, want false: no ancestor target to compare with")
	}
}

// TargetingPlayer$ on a SubAbility$ link (Magus of the Arena): the opponent
// chooses its target among their own creatures, and the link still resolves --
// the targeting player is not the controller the legality re-check falls back to.
func TestTargetingPlayerOnAChainLinkChoosesAndResolves(t *testing.T) {
	t.Parallel()

	g, p, other := restrictionGame(t)
	mine := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	libraryCards(t, g, p, 1)
	spell := g.NewCard(scriptDef(t, "Chain Spell", "Sorcery",
		"A:SP$ Draw | Defined$ You | NumCards$ 1 | SubAbility$ DBTap",
		"SVar:DBTap:DB$ Tap | ValidTgts$ Creature | TargetingPlayer$ Player.Opponent | TargetingPlayerControls$ True"), p, engine.Hand)
	c := &recordingTargets{ScriptedController: engine.NewScriptedController()}
	c.QueueTargets(entities(theirs))
	if !g.CastSpell(p, spell, c) {
		t.Fatal("CastSpell = false, want true")
	}
	if c.decider != other || !slices.Equal(c.candidates, entities(theirs)) {
		t.Errorf("chosen by %v among %v, want the opponent among only their creature (not %v)", c.decider, c.candidates, mine)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !g.Card(theirs).Tapped {
		t.Error("the opponent's chosen creature is not tapped: the link did not resolve")
	}
	if g.Card(mine).Tapped {
		t.Error("the controller's own creature was tapped")
	}
}

// Two cards of one controller targeted under the default UnlessPayer$
// (TargetedController) ask that player once: getDefinedPlayers collects a set.
func TestUnlessCostDefaultPayerIsAskedOncePerPlayer(t *testing.T) {
	t.Parallel()

	g, p, other := restrictionGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	g.Player(other).ManaPool.Add(mana.Green, 2)
	c := engine.NewScriptedController()
	c.QueueTargets(entities(a, b))
	c.QueueConfirmPayCost(true)
	queueXPayGeneric(c, mana.ShardG, 4)
	resolveLine(t, g, p, c, "DB$ Destroy | ValidTgts$ Creature.OppCtrl | TargetMin$ 2 | TargetMax$ 2 | UnlessCost$ 2")
	if got := g.Player(other).ManaPool.Total(); got != 0 {
		t.Errorf("mana left = %d, want 0: paid {2} once", got)
	}
	if g.Card(a).Zone != engine.Battlefield || g.Card(b).Zone != engine.Battlefield {
		t.Error("a creature was destroyed although the cost was paid")
	}
}
