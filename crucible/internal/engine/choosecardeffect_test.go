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

// etbChainDef builds a 1/1 creature whose "when CARDNAME enters" trigger
// runs trig, with any further name/value SVar pairs available for trig's
// own SubAbility$ chain -- the shape a Memory-writing effect and the
// Defined$ Remembered/ChosenCard/ChosenPlayer reader after it need.
func etbChainDef(t *testing.T, name, trig string, svars ...string) *compile.Card {
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
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ Trig",
	}
	raw.Faces[0].SVars.Set("Trig", trig)
	for i := 0; i+1 < len(svars); i += 2 {
		raw.Faces[0].SVars.Set(svars[i], svars[i+1])
	}
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// castETBChain casts def for p on controller c and resolves the stack.
func castETBChain(t *testing.T, g *engine.Game, p engine.PlayerID, def *compile.Card, c *engine.ScriptedController) (engine.CardID, error) {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	creature := g.NewCard(def, p, engine.Hand)
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting a creature with exactly enough mana")
	}
	return creature, g.ResolveStack(engine.NewRegistry(), c)
}

// newTwoPlayerGame is the two-player Main1 setup every test in this pack
// starts from; one player would end the game at the first state-based
// action check (CR 104.2a) before a SubAbility$ ever resolves.
func newTwoPlayerGame(t *testing.T) (*engine.Game, engine.PlayerID, engine.PlayerID) {
	t.Helper()
	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	return g, p, other
}

// TestChooseCardEffectChosenCardFeedsDefined proves the write/read pair
// this pack exists for: ChooseCard records the pick on the host's Memory,
// and a SubAbility$ naming Defined$ ChosenCard (defined.go) acts on exactly
// that card -- the dominant "choose a creature, then do X to it" chain.
func TestChooseCardEffectChosenCardFeedsDefined(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	bystander := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{victim})
	def := etbChainDef(t, "Test ChooseCard Destroy",
		"DB$ ChooseCard | Defined$ You | Choices$ Creature.OppCtrl | Mandatory$ True | SubAbility$ DBDestroy",
		"DBDestroy", "DB$ Destroy | Defined$ ChosenCard")
	host, err := castETBChain(t, g, p, def, c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(victim).Zone; z != engine.Graveyard {
		t.Errorf("chosen creature zone = %v, want Graveyard", z)
	}
	if z := g.Card(bystander).Zone; z != engine.Battlefield {
		t.Errorf("unchosen creature zone = %v, want Battlefield", z)
	}
	if got := g.Card(host).Memory.Chosen(); len(got) != 1 || got[0] != victim {
		t.Errorf("host Memory.Chosen = %v, want [%v]", got, victim)
	}
}

// TestChooseCardEffectRememberAndImprintChosen proves the trailing Memory
// block: RememberChosen$ and ImprintChosen$ copy the pick into the other
// two lists.
func TestChooseCardEffectRememberAndImprintChosen(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	pick := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{pick})
	def := etbChainDef(t, "Test ChooseCard Remember",
		"DB$ ChooseCard | Choices$ Creature.OppCtrl | Mandatory$ True | RememberChosen$ True | ImprintChosen$ True")
	host, err := castETBChain(t, g, p, def, c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	m := g.Card(host).Memory
	if got := m.Remembered(); len(got) != 1 || got[0] != engine.CardEntity(pick) {
		t.Errorf("Remembered = %v, want [%v]", got, engine.CardEntity(pick))
	}
	if got := m.Imprinted(); len(got) != 1 || got[0] != pick {
		t.Errorf("Imprinted = %v, want [%v]", got, pick)
	}
}

// TestChooseCardEffectRejectsUnofferedAnswer proves a controller answer
// outside the offered pool is an error, not silently accepted (GO-7).
func TestChooseCardEffectRejectsUnofferedAnswer(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{mine})
	def := etbChainDef(t, "Test ChooseCard Bad", "DB$ ChooseCard | Choices$ Creature.OppCtrl | Mandatory$ True")
	if _, err := castETBChain(t, g, p, def, c); err == nil {
		t.Fatal("ResolveStack succeeded, want an error for a card that was not offered")
	}
}

// TestChooseCardEffectRejectsUnresolvedParam proves
// chooseCardUnresolvedParams' fail-loud contract (PORT-8/GO-7).
func TestChooseCardEffectRejectsUnresolvedParam(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	def := etbChainDef(t, "Test ChooseCard Each", "DB$ ChooseCard | Choices$ Creature.OppCtrl | ChooseEach$ Elf & Goblin")
	if _, err := castETBChain(t, g, p, def, c); err == nil {
		t.Fatal("ResolveStack succeeded, want an error for unresolved ChooseEach$")
	}
}

// TestChooseCardEffectRememberedFeedsDefined proves definedCards' new
// "Remembered" case (defined.go): a card remembered by one link is what a
// later Defined$ Remembered acts on.
func TestChooseCardEffectRememberedFeedsDefined(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{victim})
	def := etbChainDef(t, "Test ChooseCard Remembered",
		"DB$ ChooseCard | Choices$ Creature.OppCtrl | Mandatory$ True | RememberChosen$ True | SubAbility$ DBTap",
		"DBTap", "DB$ Tap | Defined$ Remembered")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !g.Card(victim).Tapped {
		t.Error("remembered creature Tapped = false, want true")
	}
}

// TestChooseCardEffectRememberedControllerFeedsDefinedPlayer proves
// definedPlayers' "RememberedController" (rememberedPlayers, defined.go): a
// remembered card stands for its controller.
func TestChooseCardEffectRememberedControllerFeedsDefinedPlayer(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	pick := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{pick})
	def := etbChainDef(t, "Test ChooseCard RememberedController",
		"DB$ ChooseCard | Choices$ Creature.OppCtrl | Mandatory$ True | RememberChosen$ True | SubAbility$ DBLose",
		"DBLose", "DB$ LoseLife | Defined$ RememberedController | LifeAmount$ 2")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 18 {
		t.Errorf("controller life = %d, want 18", got)
	}
}

// TestChooseCardEffectForgetChosen proves ForgetChosen$ removes a card an
// earlier link remembered (Memory.Forget).
func TestChooseCardEffectForgetChosen(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	pick := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{pick})
	c.QueueCardChoice([]engine.CardID{pick})
	def := etbChainDef(t, "Test ChooseCard Forget",
		"DB$ ChooseCard | Choices$ Creature.OppCtrl | Mandatory$ True | RememberChosen$ True | SubAbility$ DBForget",
		"DBForget", "DB$ ChooseCard | Choices$ Creature.OppCtrl | Mandatory$ True | ForgetChosen$ True")
	host, err := castETBChain(t, g, p, def, c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(host).Memory.Remembered(); len(got) != 0 {
		t.Errorf("Remembered = %v, want empty after ForgetChosen$", got)
	}
}

// TestChooseCardEffectChoiceZoneAndControlledByChooser proves ChoiceZone$
// widens the pool past the battlefield and ControlledByPlayer$ Chooser
// narrows it to the chooser's own cards; the pick is then Imprinted and
// read back through Defined$ Imprinted.
func TestChooseCardEffectChoiceZoneAndControlledByChooser(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{mine})
	def := etbChainDef(t, "Test ChooseCard Zone",
		"DB$ ChooseCard | Choices$ Creature.Other | ChoiceZone$ Battlefield,Hand | ControlledByPlayer$ Chooser | Mandatory$ True | ImprintChosen$ True | SubAbility$ DBTap",
		"DBTap", "DB$ Tap | Defined$ Imprinted")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !g.Card(mine).Tapped {
		t.Error("imprinted creature Tapped = false, want true")
	}
}

// TestChooseCardEffectDefinedCardsAndOptional proves DefinedCards$ replaces
// the zone pool outright, and that without Mandatory$ an empty pick is legal.
func TestChooseCardEffectDefinedCardsAndOptional(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueCardChoice(nil)
	host, err := castETBChain(t, g, p, etbChainDef(t, "Test ChooseCard Defined", "DB$ ChooseCard | DefinedCards$ Self"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(host).Memory.Chosen(); len(got) != 0 {
		t.Errorf("Chosen = %v, want empty", got)
	}
}

// TestChooseCardEffectRejectsUnknownZoneAndController proves an unknown
// ChoiceZone$ and a non-Chooser ControlledByPlayer$ both fail closed.
func TestChooseCardEffectRejectsUnknownZoneAndController(t *testing.T) {
	t.Parallel()

	for _, line := range []string{
		"DB$ ChooseCard | Choices$ Card | ChoiceZone$ Nowhere",
		"DB$ ChooseCard | Choices$ Creature | ControlledByPlayer$ Left",
		"DB$ ChooseCard | Choices$ Creature | Amount$ Bogus",
		"DB$ ChooseCard | Choices$ Creature | MinAmount$ many",
		"DB$ ChooseCard | DefinedCards$ TriggeredTarget",
	} {
		g, p, other := newTwoPlayerGame(t)
		g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
		c := engine.NewScriptedController()
		if _, err := castETBChain(t, g, p, etbChainDef(t, "Test ChooseCard Reject", line), c); err == nil {
			t.Errorf("%q: ResolveStack succeeded, want an error", line)
		}
	}
}
