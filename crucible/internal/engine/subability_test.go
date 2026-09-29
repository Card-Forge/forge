package engine_test

import (
	"errors"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// etbSubAbilityTriggerDefParams builds a *compile.Card whose own "when
// CARDNAME enters" trigger runs execute (a full "DB$ <API> | ..." line) and
// defines every name->body pair in svars as an additional SVar the compiled
// ability tree can reference -- the shared shape every test in this file
// needs, spanning several already-built effects' own APIs rather than one
// effect's own etbXTriggerDefParams (draweffect_test.go, ...).
func etbSubAbilityTriggerDefParams(t *testing.T, name, execute string, svars map[string]string) *compile.Card {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "1", "1"
	raw.Faces[0].ManaCost = mana.MustParse("G")
	raw.Faces[0].Triggers = []string{
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigMain",
	}
	raw.Faces[0].SVars.Set("TrigMain", execute)
	for svarName, body := range svars {
		raw.Faces[0].SVars.Set(svarName, body)
	}

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBSubAbility casts def for p on controller c and resolves the stack.
// c is passed in, rather than built fresh, because several cases here need
// to queue a discard choice or a chosen target before this runs.
func castETBSubAbility(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card, c *engine.ScriptedController) error {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

// TestSubAbilityChainDrawThenDiscardResolvesBoth proves the real corpus
// shape Rousing Read's own trigger compiles to (rousing_read.txt): "draw
// two cards, then discard a card" -- DB$ Draw | Defined$ You | NumCards$ 2 |
// SubAbility$ DBDiscard, chaining into
// DB$ Discard | Defined$ You | NumCards$ 1 | Mode$ TgtChoose. Draw never
// blocked SubAbility$ (draweffect.go never named it); this is the first
// proof that chaining actually reaches a second, different, already-built
// effect rather than the reference just sitting unused in compile.Ability.Subs.
func TestSubAbilityChainDrawThenDiscardResolvesBoth(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	first := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)
	second := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)

	def := etbSubAbilityTriggerDefParams(t, "Test Rousing Read",
		"DB$ Draw | Defined$ You | NumCards$ 2 | SubAbility$ DBDiscard",
		map[string]string{"DBDiscard": "DB$ Discard | Defined$ You | NumCards$ 1 | Mode$ TgtChoose"})

	c := engine.NewScriptedController()
	c.QueueDiscardChoice([]engine.CardID{first})

	if err := castETBSubAbility(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(first).Zone != engine.Graveyard {
		t.Errorf("first card zone = %v, want Graveyard -- drawn then discarded by the chained SubAbility$", g.Card(first).Zone)
	}
	if g.Card(second).Zone != engine.Hand {
		t.Errorf("second card zone = %v, want Hand -- drawn, kept", g.Card(second).Zone)
	}
}

// TestSubAbilityChainRunsRegardlessOfParentCondition proves
// resolveSubAbility's own central claim: a SubAbility$ chains whether or
// not the parent's own subAbilityConditionMet gate let the parent's own
// body run at all -- AbilityUtils.resolveApiAbility's own unconditional
// "sa.resolve(); resolveSubAbilities(sa, game)" pairing, the real behavior
// Sphinx Sovereign's own if/else split across a LoseLife and a chained
// GainLife needs (ConditionDefined$ Self, unresolved here -- game-state.md's
// "Not ported yet" -- so this uses the zone-scan Condition family instead,
// ConditionPresent$/ConditionCompare$, which IS resolved, to prove the
// identical guarantee without depending on that gap). The parent's own
// ConditionCompare$ GE2 needs two creatures the caster controls; only the
// one just-cast creature exists, so the parent's own GainLife body must
// NOT run -- but the chained LoseLife carries no Condition$ of its own, so
// it must run regardless.
func TestSubAbilityChainRunsRegardlessOfParentCondition(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbSubAbilityTriggerDefParams(t, "Test Condition Independent",
		"DB$ GainLife | Defined$ You | LifeAmount$ 5 | ConditionPresent$ Creature.YouCtrl | ConditionCompare$ GE2 | SubAbility$ DBLoseLifeAlways",
		map[string]string{"DBLoseLifeAlways": "DB$ LoseLife | Defined$ You | LifeAmount$ 3"})

	c := engine.NewScriptedController()
	if err := castETBSubAbility(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 17 {
		t.Errorf("p's life = %d, want 17 -- GainLife's own unmet condition must skip its body (still 20), "+
			"but the chained LoseLife must still run (20-3=17) regardless", g.Player(p).Life)
	}
}

// TestSubAbilityChainPropagatesTargetsToSub proves a chained ability reads
// the SAME Targets the parent's own ValidTgts$ resolved -- LoseLife
// (targeting.go's own first real consumer) targets a player, and the
// chained GainLife's own Defined$ Targeted (definedPlayers's own
// "Targeted" case, defined.go) must act on that identical player, not the
// caster and not nobody.
func TestSubAbilityChainPropagatesTargetsToSub(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	def := etbSubAbilityTriggerDefParams(t, "Test Targets Propagate",
		"DB$ LoseLife | ValidTgts$ Player | LifeAmount$ 2 | SubAbility$ DBGainLifeTargeted",
		map[string]string{"DBGainLifeTargeted": "DB$ GainLife | Defined$ Targeted | LifeAmount$ 4"})

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if err := castETBSubAbility(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(p).Life != 20 {
		t.Errorf("p's life = %d, want 20 -- neither node targets p", g.Player(p).Life)
	}
	if g.Player(other).Life != 22 {
		t.Errorf("other's life = %d, want 22 -- LoseLife drains 2 (20-2=18), "+
			"the chained GainLife's own Defined$ Targeted must read the SAME target and grant 4 (18+4=22)", g.Player(other).Life)
	}
}

// TestSubAbilityChainDepthThreeChainsAllThree proves recursion past one
// level: resolveSubAbility recurses through Registry.Resolve itself, so a
// chain's own further SubAbility$ (LoseLife -> GainLife -> Draw here) keeps
// going without any caller needing a loop -- 1,172 real corpus lines
// chain exactly this deep (subability.go's own doc comment), a synthetic
// composition of three independently-real per-level shapes rather than one
// single real card (LoseLife's own real depth-3 corpus lines that chain
// this way all hit some other unrelated gap -- an unresolved SVar amount, a
// Defined$ Targeted with nothing targeted -- before reaching the third
// level cleanly).
func TestSubAbilityChainDepthThreeChainsAllThree(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	top := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)

	def := etbSubAbilityTriggerDefParams(t, "Test Depth Three",
		"DB$ LoseLife | Defined$ Player.Opponent | LifeAmount$ 1 | SubAbility$ DBGainLife2",
		map[string]string{
			"DBGainLife2": "DB$ GainLife | Defined$ You | LifeAmount$ 2 | SubAbility$ DBDraw2",
			"DBDraw2":     "DB$ Draw | Defined$ You | NumCards$ 1",
		})

	c := engine.NewScriptedController()
	if err := castETBSubAbility(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Player(other).Life != 19 {
		t.Errorf("other's life = %d, want 19 -- the first node (LoseLife) must run", g.Player(other).Life)
	}
	if g.Player(p).Life != 22 {
		t.Errorf("p's life = %d, want 22 -- the second node (GainLife) must run", g.Player(p).Life)
	}
	if g.Card(top).Zone != engine.Hand {
		t.Errorf("library card zone = %v, want Hand -- the third node (Draw) must run", g.Card(top).Zone)
	}
}

// TestSubAbilityChainUnimplementedAPIErrors proves a chain reaching an API
// this port has not built an Effect for yet fails with ErrUnimplemented
// naming it -- Registry.Resolve's own existing contract for a top-level
// ability, extended for free once resolveSubAbility recurses back through
// Registry.Resolve itself (effect.go's own doc comment): DB$ Draw |
// Defined$ You | NumCards$ 3 | SubAbility$ DBMutate, chaining into DB$
// Mutate (an API this port has not built yet). Draw's own
// three cards must
// already be in hand: CR's own sequential resolution means the parts of an
// ability already executed stay executed even when a later part fails,
// the identical reasoning Rousing Read's own chain already exercises for
// the success path.
func TestSubAbilityChainUnimplementedAPIErrors(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	for i := 0; i < 3; i++ {
		g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)
	}

	def := etbSubAbilityTriggerDefParams(t, "Test Riverwise Augur",
		"DB$ Draw | Defined$ You | NumCards$ 3 | SubAbility$ DBMutate",
		map[string]string{"DBMutate": "DB$ Mutate"})

	c := engine.NewScriptedController()
	err := castETBSubAbility(t, g, p, def, c)
	if err == nil {
		t.Fatal("ResolveStack: got nil error, want one naming Mutate")
	}
	if !errors.Is(err, engine.ErrUnimplemented) {
		t.Errorf("ResolveStack error = %q, want it to wrap ErrUnimplemented", err.Error())
	}
	if !strings.Contains(err.Error(), "Mutate") {
		t.Errorf("ResolveStack error = %q, want it to name Mutate", err.Error())
	}
	if len(g.Zone(engine.Hand, p).Cards()) != 3 {
		t.Errorf("p's hand size = %d, want 3 -- Draw's own body must already have run before the chain hit Mutate",
			len(g.Zone(engine.Hand, p).Cards()))
	}
}

// TestSubAbilityChainUnrecognizedAPIErrors proves a SubAbility$ SVar body
// whose own leading value ApiType.java has no constant for is a real error
// naming it, not a silently dropped chain -- not reachable against the
// real corpus today (ApiType.java's own generated vocabulary, ability.go,
// and the apiscan/vocabscan gates, M3, already require every real API
// string to resolve), but a card cannot be trusted not to be the first
// (PORT-8), and compile.Compile itself never validates an API name against
// any vocabulary (that check is APIByName's own job, at resolve time).
func TestSubAbilityChainUnrecognizedAPIErrors(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	top := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)

	def := etbSubAbilityTriggerDefParams(t, "Test Unrecognized API",
		"DB$ Draw | Defined$ You | NumCards$ 1 | SubAbility$ DBBogus",
		map[string]string{"DBBogus": "DB$ NotARealAPI"})

	c := engine.NewScriptedController()
	err := castETBSubAbility(t, g, p, def, c)
	if err == nil {
		t.Fatal("ResolveStack: got nil error, want one naming NotARealAPI")
	}
	if !strings.Contains(err.Error(), "NotARealAPI") {
		t.Errorf("ResolveStack error = %q, want it to name NotARealAPI", err.Error())
	}
	if g.Card(top).Zone != engine.Hand {
		t.Errorf("library card zone = %v, want Hand -- Draw's own body must already have run", g.Card(top).Zone)
	}
}
