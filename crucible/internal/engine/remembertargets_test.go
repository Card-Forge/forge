package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// pushChainAbility puts the Execute$ ability of an etbChainDef card hosted on
// p's battlefield onto the stack with targets, without resolving it, and
// returns the host.
func pushChainAbility(t *testing.T, g *engine.Game, p engine.PlayerID, targets []engine.EntityID, trig string, svars ...string) engine.CardID {
	t.Helper()
	def := etbChainDef(t, "Test Remember", trig, svars...)
	host := g.NewCard(def, p, engine.Battlefield)
	face := def.Faces[0]
	for _, sub := range face.Triggers[0].Subs {
		if !strings.EqualFold(sub.Key, "Execute") {
			continue
		}
		api, ok := engine.APIByName(sub.Ability.Name)
		if !ok {
			t.Fatalf("unknown API %q", sub.Ability.Name)
		}
		g.PushAbility(engine.Ability{API: api, Source: host, Controller: p, Params: sub.Ability, Amounts: face.Amounts, Targets: targets})
		return host
	}
	t.Fatal("no Execute$")
	return engine.NoCard
}

// TestRememberTargetsSylvanSmiteDealsItsCreaturesPower proves
// AbilityUtils.handleRemembering (MagicStack.java:646) on the real card: the
// root PutCounter's target is remembered before it resolves, so the chained
// DealDamage's DamageSource$ Remembered and Y = Remembered$CardPower read the
// creature -- 4 damage after its counter, not the 0 a missed remember gives.
func TestRememberTargetsSylvanSmiteDealsItsCreaturesPower(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetStartingPlayer(other)
	mine := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "2", "5"), other, engine.Battlefield)
	smite := g.NewCard(corpusCard(t, "Sylvan Smite"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(theirs)})
	c.QueuePayGeneric(mana.ShardG)
	g.Player(p).ManaPool.Add(mana.Green, 2)
	if !g.CastSpell(p, smite, c) {
		t.Fatal("CastSpell(Sylvan Smite) failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if got := g.Card(theirs).Damage.Marked; got != 4 {
		t.Errorf("damage on the opponent's creature = %d, want 4 (the 3/3 plus its counter)", got)
	}
	if got := g.Card(smite).Memory.Remembered(); len(got) != 0 {
		t.Errorf("remembered after DBCleanup = %v, want none", got)
	}
}

// TestRememberTargetsCloudshiftReturnsTheCreature proves the flicker shape
// (Cloudshift and 40-odd ChangeZone lines): the exiled target is remembered and
// DBReturn's Defined$ Remembered brings it back.
func TestRememberTargetsCloudshiftReturnsTheCreature(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	shift := g.NewCard(corpusCard(t, "Cloudshift"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(mine)})
	g.Player(p).ManaPool.Add(mana.White, 1)
	if !g.CastSpell(p, shift, c) {
		t.Fatal("CastSpell(Cloudshift) failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if z := g.Card(mine).Zone; z != engine.Battlefield {
		t.Errorf("creature in %v, want back on the battlefield", z)
	}
}

// TestRememberTargetsAddsAndForgets proves the list handling: the targets are
// added after what the host already remembers, and ForgetOtherTargets$
// clears the list first (AbilityUtils.java:1520-1524).
func TestRememberTargetsAddsAndForgets(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		name, line string
		keepOld    bool
	}{
		{"adds", "DB$ Pump | ValidTgts$ Creature | RememberTargets$ True", true},
		{"forgets", "DB$ Pump | ValidTgts$ Creature | RememberTargets$ True | ForgetOtherTargets$ True", false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			old := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
			target := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
			host := pushChainAbility(t, g, p, []engine.EntityID{engine.CardEntity(target)}, tc.line)
			g.Card(host).Memory.Remember(engine.CardEntity(old))
			if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
				t.Fatal(err)
			}
			want := []engine.EntityID{engine.CardEntity(target)}
			if tc.keepOld {
				want = []engine.EntityID{engine.CardEntity(old), engine.CardEntity(target)}
			}
			if got := g.Card(host).Memory.Remembered(); !slices.Equal(got, want) {
				t.Errorf("remembered = %v, want %v", got, want)
			}
		})
	}
}

// TestRememberTargetsKeepsOnlyLegalTargets proves the order Java runs the two
// steps in: hasFizzled removes an illegal target (MagicStack.java:779) before
// handleRemembering reads the rest (:646).
func TestRememberTargetsKeepsOnlyLegalTargets(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	gone := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	kept := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	host := pushChainAbility(t, g, p, []engine.EntityID{engine.CardEntity(gone), engine.CardEntity(kept)},
		"DB$ Pump | ValidTgts$ Creature | TargetMax$ 2 | RememberTargets$ True")
	g.Move(gone, engine.Graveyard, other)
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatal(err)
	}
	if got, want := g.Card(host).Memory.Remembered(), []engine.EntityID{engine.CardEntity(kept)}; !slices.Equal(got, want) {
		t.Errorf("remembered = %v, want %v", got, want)
	}
}

// TestRememberTargetsWithoutTargetingRemembersNothing proves the
// usesTargeting half of AbilityUtils.java:1520: a RememberTargets$ line that
// names no ValidTgts$ (Choking Vines' sibling shapes) leaves the list alone.
func TestRememberTargetsWithoutTargetingRemembersNothing(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	host, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ Pump | Defined$ Self | RememberTargets$ True")
	if err != nil {
		t.Fatal(err)
	}
	if got := g.Card(host).Memory.Remembered(); len(got) != 0 {
		t.Errorf("remembered = %v, want none", got)
	}
}

// TestRememberTargetsRefusesWhatItCannotRemember proves each refusal comes
// before anything is remembered or resolved (GO-7): a sub-ability's
// RememberTargets$ (a Forge script defect, Volcanic Offering), a targeted
// ability on the stack, IncludeAllComponentCards$ and RememberCostMana$.
func TestRememberTargetsRefusesWhatItCannotRemember(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		name, line, want string
		svars            []string
		stackTarget      bool
	}{
		{"sub-ability", "DB$ Pump | Defined$ Self | NumAtt$ +1 | SubAbility$ DBX", "never reaches AbilityUtils.handleRemembering",
			[]string{"DBX", "DB$ Pump | ValidTgts$ Creature | RememberTargets$ True"}, false},
		{"stack item", "DB$ Pump | ValidTgts$ Card | RememberTargets$ True", "naming an ability on the stack", nil, true},
		{"components", "DB$ Pump | ValidTgts$ Creature | RememberTargets$ True | IncludeAllComponentCards$ True", "IncludeAllComponentCards$ not resolvable yet", nil, false},
		{"cost mana", "DB$ Pump | Defined$ Self | NumAtt$ +1 | RememberCostMana$ True", "RememberCostMana$ not resolvable yet", nil, false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			creature := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
			target := engine.CardEntity(creature)
			if tc.stackTarget {
				// The first item pushed is stack item 1, under the one tested.
				pushStackAbility(t, g, other, creature)
				target = engine.StackItemEntity(1)
			}
			host := pushChainAbility(t, g, p, []engine.EntityID{target}, tc.line, tc.svars...)
			err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController())
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Fatalf("err = %v, want %q", err, tc.want)
			}
			if got := g.Card(host).Memory.Remembered(); len(got) != 0 {
				t.Errorf("remembered = %v, want none before the refusal", got)
			}
			if p, _ := g.Card(host).Power(); tc.name == "cost mana" && p != 1 {
				t.Errorf("host power = %d, want 1: the refusal came after the pump", p)
			}
		})
	}
}
