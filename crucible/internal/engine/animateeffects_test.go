package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// advanceToCleanup walks g to its own cleanup step, where "until end of
// turn" effects end.
func advanceToCleanup(g *engine.Game, c engine.PlayerController) {
	for g.ActivePhase() != engine.Cleanup {
		g.AdvancePhase(c)
	}
}

// TestAnimateEffectLandBecomesCreatureUntilEndOfTurn proves the dominant
// "manland" shape: a land targeted by Animate becomes a 3/3 red Elemental
// creature with haste this turn -- Layers 4, 5, 6 and 7b together -- and is
// a plain land again after cleanup.
func TestAnimateEffectLandBecomesCreatureUntilEndOfTurn(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	land := g.NewCard(landDef(t, "Test Mountain", "Land Mountain"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(land)})
	def := etbChainDef(t, "Test Animator",
		"DB$ Animate | ValidTgts$ Land | Power$ 3 | Toughness$ 3 | Types$ Creature,Elemental | Colors$ Red | OverwriteColors$ True | Keywords$ Haste")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	l := g.Card(land)
	if !l.Type().Has(cardtype.Creature) || !l.Type().HasSubtype("Elemental") || !l.Type().Has(cardtype.Land) {
		t.Errorf("animated land type = %v, want Land Creature with Elemental", l.Type())
	}
	pw, okP := l.Power()
	tg, okT := l.Toughness()
	if !okP || !okT || pw != 3 || tg != 3 {
		t.Errorf("animated land = %d/%d (ok %v/%v), want 3/3", pw, tg, okP, okT)
	}
	if l.Colors() != mana.Red || !l.HasKeyword("Haste") {
		t.Errorf("animated land colors = %v haste = %v, want red with haste", l.Colors(), l.HasKeyword("Haste"))
	}
	advanceToCleanup(g, c)
	if l.Type().Has(cardtype.Creature) || l.HasKeyword("Haste") {
		t.Errorf("land still animated after cleanup: type %v", l.Type())
	}
}

// TestAnimateEffectRemoveCardTypesThenAdd proves CardChangedType's order:
// RemoveCardTypes$ strips the card types before Types$ adds, so "becomes an
// artifact" leaves an artifact and nothing else; Duration$ Permanent keeps
// it past cleanup.
func TestAnimateEffectRemoveCardTypesThenAdd(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	def := etbChainDef(t, "Test Becomes Artifact",
		"DB$ Animate | Defined$ Self | Types$ Artifact | RemoveCardTypes$ True | Duration$ Permanent")
	host, err := castETBChain(t, g, p, def, c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	advanceToCleanup(g, c)
	typ := g.Card(host).Type()
	if !typ.Has(cardtype.Artifact) || typ.Has(cardtype.Creature) {
		t.Errorf("host type = %v, want an artifact that is no longer a creature", typ)
	}
}

// TestAnimateEffectRemoveCreatureTypesNeedsVocabulary proves the category
// removal reads the DB's subtype vocabulary: with it, Elf goes; without it
// (a DB built by NewDB), the line fails closed rather than guessing.
func TestAnimateEffectRemoveCreatureTypesNeedsVocabulary(t *testing.T) {
	t.Parallel()

	line := "DB$ Animate | Defined$ Self | Types$ Spirit | RemoveCreatureTypes$ True"

	g, p, _ := newTwoPlayerGame(t)
	_, err := castETBChain(t, g, p, etbChainDef(t, "Test Spirit No Vocab", line), engine.NewScriptedController())
	if err == nil || !strings.Contains(err.Error(), "vocabulary") {
		t.Errorf("without a vocabulary: error = %v, want a vocabulary error", err)
	}

	db := compile.NewDB(nil).WithTypes(attachmentTypeRegistry(t))
	g2 := engine.NewGame(db, javarand.New(1), []string{"a", "b"})
	p2 := g2.Players()[0]
	g2.SetTurnState(1, p2, engine.Main1)
	g2.Player(p2).Life, g2.Player(g2.Players()[1]).Life = 20, 20
	host, err := castETBChain(t, g2, p2, etbChainDef(t, "Test Spirit", line), engine.NewScriptedController())
	if err != nil {
		t.Fatalf("with a vocabulary: ResolveStack: %v", err)
	}
	typ := g2.Card(host).Type()
	if typ.HasSubtype("Elf") || !typ.HasSubtype("Spirit") {
		t.Errorf("host type = %v, want Spirit and no Elf", typ)
	}
}

// TestAnimateAllEffectAffectsEveryMatch proves ValidCards$ over the whole
// battlefield.
func TestAnimateAllEffectAffectsEveryMatch(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	mine := g.NewCard(landDef(t, "Test Forest", "Land Forest"), p, engine.Battlefield)
	theirs := g.NewCard(landDef(t, "Test Forest", "Land Forest"), other, engine.Battlefield)
	def := etbChainDef(t, "Test Awaken All",
		"DB$ AnimateAll | ValidCards$ Land | Power$ 2 | Toughness$ 2 | Types$ Creature")
	if _, err := castETBChain(t, g, p, def, engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	for _, id := range []engine.CardID{mine, theirs} {
		if !g.Card(id).Type().Has(cardtype.Creature) {
			t.Errorf("land %v not animated", id)
		}
	}
}

// TestAnimateAllEffectValidTgtsScopesToThePlayer proves a targeting AnimateAll
// reads only the targeted player's cards (AnimateAllEffect.java:136-140).
func TestAnimateAllEffectValidTgtsScopesToThePlayer(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	mine := g.NewCard(landDef(t, "Test Forest", "Land Forest"), p, engine.Battlefield)
	theirs := g.NewCard(landDef(t, "Test Forest", "Land Forest"), other, engine.Battlefield)
	def := etbChainDef(t, "Test Awaken Theirs",
		"DB$ AnimateAll | ValidTgts$ Player | ValidCards$ Land | Power$ 2 | Toughness$ 2 | Types$ Creature")
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !g.Card(theirs).Type().Has(cardtype.Creature) {
		t.Error("the targeted player's land was not animated")
	}
	if g.Card(mine).Type().Has(cardtype.Creature) {
		t.Error("the other player's land was animated")
	}
}

// TestDebuffEffectRemovesKeywordUntilEndOfTurn proves Debuff takes a
// printed keyword away for the turn.
func TestDebuffEffectRemovesKeywordUntilEndOfTurn(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	flier := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Flying"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(flier)})
	def := etbChainDef(t, "Test Grounder", "DB$ Debuff | ValidTgts$ Creature.OppCtrl | Keywords$ Flying")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.Card(flier).HasKeyword("Flying") {
		t.Fatal("creature still flies after Debuff")
	}
	advanceToCleanup(g, c)
	if !g.Card(flier).HasKeyword("Flying") {
		t.Error("creature does not fly again after cleanup")
	}
}

// TestProtectionEffectChoiceGrantsProtection proves Gains$ Choice with
// Choices$ AnyColor asks the activator and grants "Protection from
// <color>"; a card type becomes "Protection:<type>".
func TestProtectionEffectChoiceGrantsProtection(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueProtectionChoice(3) // red: white, blue, black, red, green
	def := etbChainDef(t, "Test Protector",
		"DB$ Protection | Defined$ Self | Gains$ Choice | Choices$ AnyColor | SubAbility$ DBType",
		"DBType", "DB$ Protection | Defined$ Self | Gains$ Artifact")
	host, err := castETBChain(t, g, p, def, c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	lines := strings.Join(g.Card(host).KeywordLines(), "|")
	if !strings.Contains(lines, "Protection from red") || !strings.Contains(lines, "Protection:Artifact") {
		t.Errorf("host keywords = %q, want Protection from red and Protection:Artifact", lines)
	}
}

// TestProtectionAllEffectCoversValidCards proves every ValidCards$ match
// gains the protection.
func TestProtectionAllEffectCoversValidCards(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	ally := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	def := etbChainDef(t, "Test Shield All", "DB$ ProtectionAll | ValidCards$ Creature.YouCtrl | Gains$ black")
	if _, err := castETBChain(t, g, p, def, engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !strings.Contains(strings.Join(g.Card(ally).KeywordLines(), "|"), "Protection from black") {
		t.Error("ally lacks Protection from black")
	}
}
