package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// These tests cover ChooseCard's QuasiLibrarySearch$ (ChooseCardEffect.java:
// 243-269) and the sharesNameWith valid property its corpus lines filter by
// (CardProperty.java:818-870).

const quasiSearch = "DB$ ChooseCard | ChoiceZone$ Library | QuasiLibrarySearch$ You | Choices$ Card.YouOwn | ImprintChosen$ True"

func TestQuasiLibrarySearchPicksOneCardOrNone(t *testing.T) {
	t.Parallel()

	t.Run("pick", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		lib := libraryCards(t, g, p, 2)
		c := engine.NewScriptedController()
		c.QueueCardChoice([]engine.CardID{lib[1]})
		host := resolveLine(t, g, p, c, quasiSearch+" | SubAbility$ DBMove",
			"DBMove", "DB$ ChangeZone | Defined$ Imprinted | Origin$ Library | Destination$ Hand | Searched$ True")
		if z := g.Card(lib[1]).Zone; z != engine.Hand {
			t.Errorf("picked card zone = %v, want Hand", z)
		}
		if got := g.Card(host).Memory.Imprinted(); len(got) != 1 || got[0] != lib[1] {
			t.Errorf("imprinted = %v, want [%v]", got, lib[1])
		}
	})

	t.Run("decline keeps the old memory", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		lib := libraryCards(t, g, p, 1)
		bear := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
		c := engine.NewScriptedController()
		c.QueueCardChoice([]engine.CardID{bear})
		c.QueueCardChoice(nil)
		host := resolveLine(t, g, p, c, "DB$ ChooseCard | Choices$ Creature | Mandatory$ True | SubAbility$ DBSearch",
			"DBSearch", quasiSearch)
		// ChooseCardEffect.java:262-264 returns before setChosenCards: the
		// first pick stays the host's chosen card.
		if got := g.Card(host).Memory.Chosen(); len(got) != 1 || got[0] != bear {
			t.Errorf("chosen = %v, want [%v]", got, bear)
		}
		if got := g.Card(host).Memory.Imprinted(); len(got) != 0 {
			t.Errorf("imprinted = %v, want none", got)
		}
		if z := g.Card(lib[0]).Zone; z != engine.Library {
			t.Errorf("library card zone = %v, want Library", z)
		}
	})

	t.Run("empty pool asks nothing", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		rec := &offerRecorder{ScriptedController: engine.NewScriptedController()}
		def := etbChainDef(t, "Test Quasi", quasiSearch+" | Mandatory$ True")
		host := g.NewCard(def, p, engine.Battlefield)
		sub := def.Faces[0].Triggers[0].Subs[0].Ability
		g.PushAbility(engine.Ability{API: engine.APIChooseCard, Source: host, Controller: p, Params: sub})
		if err := g.ResolveStack(engine.NewRegistry(), rec); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if len(rec.offers) != 0 {
			t.Errorf("offers = %v, want none", rec.offers)
		}
		if got := g.Card(host).Memory.Imprinted(); len(got) != 0 {
			t.Errorf("imprinted = %v, want none", got)
		}
	})
}

// TestQuasiLibrarySearchRefusals: a QuasiLibrarySearch$ naming no player
// (Java's get(0) throws), and a search the activator makes while another
// player controls their searches (Opposition Agent): Java's decline path
// never pops that control grant (ChooseCardEffect.java:262-269), so the shape
// is refused before anything is asked.
func TestQuasiLibrarySearchRefusals(t *testing.T) {
	t.Parallel()

	t.Run("names no player", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, strings.Replace(quasiSearch, "$ You", "$ ChosenPlayer", 1))
		if err == nil || !strings.Contains(err.Error(), "names no player") {
			t.Errorf("err = %v, want one naming no player", err)
		}
	})

	t.Run("unresolvable player", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, strings.Replace(quasiSearch, "$ You", "$ TriggeredPlayer", 1))
		if err == nil || !strings.Contains(err.Error(), "QuasiLibrarySearch$") {
			t.Errorf("err = %v, want one naming QuasiLibrarySearch$", err)
		}
	})

	t.Run("under search control", func(t *testing.T) {
		t.Parallel()
		g, p, other := newTwoPlayerGame(t)
		g.NewCard(oppositionAgentLikeDef(t), other, engine.Battlefield)
		engine.CheckStateBasedActions(g, engine.NewScriptedController())
		libraryCards(t, g, p, 1)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, quasiSearch)
		if err == nil || !strings.Contains(err.Error(), "ControlOpponentsSearchingLibrary$") {
			t.Errorf("err = %v, want the search-control refusal", err)
		}
		if got := g.ControllingPlayer(p); got != engine.NoPlayer {
			t.Errorf("controller after the refusal = %v, want none", got)
		}
	})
}

// TestParentTargetPlayers covers Defined$ ParentTarget naming players
// (AbilityUtils.java:947-951): the targeted player of the parent; a parent
// targeting only cards is refused, since Java would walk past it to a
// player-targeting grandparent (SpellAbility.java:2024-2032).
func TestParentTargetPlayers(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ GainLife | ValidTgts$ Player | LifeAmount$ 1 | SubAbility$ DBGain", "DBGain", "DB$ GainLife | Defined$ ParentTarget | LifeAmount$ 2"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 23 {
		t.Errorf("targeted player's life = %d, want 23", got)
	}

	bear := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(bear)},
		"DB$ Pump | ValidTgts$ Creature | NumAtt$ +1 | SubAbility$ DBGain", "DBGain", "DB$ GainLife | Defined$ ParentTarget | LifeAmount$ 2")
	if err == nil || !strings.Contains(err.Error(), "targets no player") {
		t.Errorf("err = %v, want the card-only parent refusal", err)
	}
}

// TestSharesNameWithForms covers CardProperty.java's sharesNameWith branches
// answered without the ability: the bare form (the source itself), the zone
// forms, and the Memory-backed Defined$ forms; the others match nothing.
func TestSharesNameWithForms(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	bearDef := scriptDef(t, "Bear", "Creature Bear")
	elfDef := scriptDef(t, "Elf", "Creature Elf")
	candidate := g.NewCard(bearDef, p, engine.Library)
	source := g.NewCard(bearDef, p, engine.Battlefield)
	elf := g.NewCard(elfDef, p, engine.Battlefield)
	g.NewCard(bearDef, other, engine.Graveyard)
	token := g.NewCard(elfDef, other, engine.Battlefield)
	g.Card(token).IsToken = true
	g.Card(elf).Memory.Remember(engine.CardEntity(source))
	g.Card(elf).Memory.Remember(engine.PlayerEntity(other))
	g.Card(source).Memory.Imprint(elf)

	for _, tc := range []struct {
		spec   string
		source engine.CardID
		want   bool
	}{
		{"Card.sharesNameWith", source, true},
		{"Card.sharesNameWith", elf, false},
		{"Card.sharesNameWith YourGraveyard", source, false},
		{"Card.sharesNameWith Graveyard", source, true},
		{"Card.sharesNameWith Battlefield", source, true},
		{"Card.sharesNameWith NonToken", source, true},
		{"Card.sharesNameWith Remembered", elf, true},
		{"Card.sharesNameWith Remembered", source, false},
		{"Card.sharesNameWith Imprinted", source, false},
		{"Card.sharesNameWith ThisTurnCast", source, false},
		{"Card.sharesNameWith Remembered", engine.NoCard, false},
	} {
		if got := engine.Matches(g, g.Card(candidate), valid.Parse(tc.spec), p, tc.source); got != tc.want {
			t.Errorf("%q from %v = %v, want %v", tc.spec, tc.source, got, tc.want)
		}
	}
	// The token Elf is skipped by NonToken; once the nontoken Elf is gone only
	// the token is left, and nothing matches. YourGraveyard reads the
	// source controller's graveyard: other's holds a Bear.
	elfInHand := g.NewCard(elfDef, p, engine.Hand)
	if !engine.Matches(g, g.Card(elfInHand), valid.Parse("Card.sharesNameWith NonToken"), p, source) {
		t.Error("NonToken: the nontoken Elf on the battlefield shares the name")
	}
	g.Move(elf, engine.Exile, p)
	if engine.Matches(g, g.Card(elfInHand), valid.Parse("Card.sharesNameWith NonToken"), p, source) {
		t.Error("NonToken: only an Elf token is left, want no match")
	}
	if !engine.Matches(g, g.Card(candidate), valid.Parse("Card.sharesNameWith YourGraveyard"), other, source) {
		t.Error("YourGraveyard: other's graveyard holds a Bear")
	}
}
