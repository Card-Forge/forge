package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// Random and secret choices (R9k): each random pick draws the game's Java
// RNG exactly as Aggregates.random does, so a seeded game replays the
// oracle's pick. Every game here is seeded 1 and draws nothing before the
// effect, so the expected pick is a fresh javarand.New(1)'s.

// reservoir is Aggregates.random(Iterable, count)'s draw over indices 0..n-1.
func reservoir(r *javarand.Rand, n, count int) []int {
	var out []int
	for i := 1; i <= n; i++ {
		if i <= count {
			out = append(out, i-1)
			continue
		}
		if j := int(r.Int32n(int32(i))); j < count {
			out[j] = i - 1
		}
	}
	return out
}

// ChoosePlayer's Random$ picks Aggregates.random over Choices$
// (ChoosePlayerEffect.java:47-48); Secretly$ asks and records like an open
// choice (Card.setSecretChosenPlayer writes the same slot).
func TestChoosePlayerRandomAndSecretly(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	host, err := castETBChain(t, g, p, etbChainDef(t, "Random Chooser",
		"DB$ ChoosePlayer | Defined$ You | Choices$ Player | Random$ True"), engine.NewScriptedController())
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if want := g.Players()[javarand.New(1).Int32n(2)]; g.Card(host).Memory.ChosenPlayer() != want {
		t.Errorf("chosen player = %v, want %v", g.Card(host).Memory.ChosenPlayer(), want)
	}

	g2, p2, other2 := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueuePlayerChoice(other2)
	host2, err := castETBChain(t, g2, p2, etbChainDef(t, "Secret Chooser",
		"DB$ ChoosePlayer | Defined$ You | Choices$ Opponent | Secretly$ True"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g2.Card(host2).Memory.ChosenPlayer(); got != other2 {
		t.Errorf("secretly chosen player = %v, want the opponent", got)
	}
}

// ChooseCard's AtRandom$ samples Amount$ cards from the pool in scan order
// (ChooseCardEffect.java:225-228).
func TestChooseCardAtRandomSamplesThePool(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	var pool []engine.CardID
	for range 3 {
		pool = append(pool, g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield))
	}
	host, err := castETBChain(t, g, p, etbChainDef(t, "Random Picker",
		"DB$ ChooseCard | Choices$ Creature.OppCtrl | Amount$ 2 | AtRandom$ True | Secretly$ True"), engine.NewScriptedController())
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	var want []engine.CardID
	for _, i := range reservoir(javarand.New(1), len(pool), 2) {
		want = append(want, pool[i])
	}
	got := g.Card(host).Memory.Chosen()
	if len(got) != 2 || got[0] != want[0] || got[1] != want[1] {
		t.Errorf("chosen = %v, want %v", got, want)
	}
}

// GenericChoice's AtRandom$ True picks ChoiceAmount$ of the choices with
// Aggregates.random (ChooseGenericEffect.java:86-89) and resolves them.
func TestGenericChoiceAtRandomResolvesTheDrawnChoice(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	gains := []int{1, 10, 100}
	if _, err := castETBChain(t, g, p, etbChainDef(t, "Random Choice",
		"DB$ GenericChoice | Defined$ You | AtRandom$ True | Secretly$ True | Choices$ DBA,DBB,DBC",
		"DBA", "DB$ GainLife | Defined$ You | LifeAmount$ 1",
		"DBB", "DB$ GainLife | Defined$ You | LifeAmount$ 10",
		"DBC", "DB$ GainLife | Defined$ You | LifeAmount$ 100"), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	want := 20 + gains[reservoir(javarand.New(1), 3, 1)[0]]
	if got := g.Player(p).Life; got != want {
		t.Errorf("life = %d, want %d", got, want)
	}
}

// A hidden-zone ChangeZone's AtRandom$ draws over a fetch list Java first
// sorts by CardView.toString (ChangeZoneEffect.java:1097), an order this port
// cannot reproduce: refused rather than drawing a different card.
func TestChangeZoneAtRandomIsRefused(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	for range 3 {
		g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)
	}
	if _, err := castETBChain(t, g, p, etbChainDef(t, "Random Wish",
		"DB$ ChangeZone | Origin$ Graveyard | Destination$ Hand | ChangeType$ Card.YouCtrl | ChangeNum$ 2 | Hidden$ True | AtRandom$ True"),
		engine.NewScriptedController()); err == nil {
		t.Error("AtRandom$ resolved, want it refused")
	}
}

// ChooseNumber's Random$ is nextInt(max-min+1)+min (ChooseNumberEffect.java:
// 65-66); bounds that leave no number are an error, where Java throws.
func TestChooseNumberRandom(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	host, err := castETBChain(t, g, p, etbChainDef(t, "Random Number",
		"DB$ ChooseNumber | Defined$ You | Min$ 1 | Max$ 6 | Random$ True"), engine.NewScriptedController())
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	got, ok := g.Card(host).Memory.ChosenNumber()
	if want := int(javarand.New(1).Int32n(6)) + 1; !ok || got != want {
		t.Errorf("chosen number = %d (%v), want %d", got, ok, want)
	}

	g2, p2, _ := newTwoPlayerGame(t)
	if _, err := castETBChain(t, g2, p2, etbChainDef(t, "Empty Range",
		"DB$ ChooseNumber | Defined$ You | Min$ 3 | Max$ 1 | Random$ True"), engine.NewScriptedController()); err == nil {
		t.Error("Random$ over an empty range resolved, want an error")
	}
}

// ChooseColor's Random$ draws the minimum count, each pick off the WUBRG
// list (ChooseColorEffect.java:80-87).
func TestChooseColorRandomTwoColors(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	host, err := castETBChain(t, g, p, etbChainDef(t, "Random Pair",
		"DB$ ChooseColor | Defined$ You | TwoColors$ True | Random$ True"), engine.NewScriptedController())
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	r := javarand.New(1)
	left := []mana.Colors{mana.White, mana.Blue, mana.Black, mana.Red, mana.Green}
	var want mana.Colors
	for range 2 {
		i := int(r.Int32n(int32(len(left))))
		want |= left[i]
		left = append(left[:i], left[i+1:]...)
	}
	if got := g.Card(host).Memory.ChosenColors(); got != want {
		t.Errorf("chosen colors = %v, want %v", got, want)
	}
}

// ColorsFrom$ offers only the colors among the defined cards -- Desert
// Cenote's "ValidHand Card.YouOwn" -- and with none the effect ends unasked
// (ChooseColorEffect.java:46-52).
func TestChooseColorColorsFromTheHand(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Hand)
	g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueColorChoice(mana.Red | mana.Green)
	line := "DB$ ChooseColor | Defined$ You | TwoColors$ True | UpTo$ True | ColorsFrom$ ValidHand Card.YouOwn"
	host, err := castETBChain(t, g, p, etbChainDef(t, "Hand Colors", line), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(host).Memory.ChosenColors(); got != mana.Red|mana.Green {
		t.Errorf("chosen colors = %v, want red and green", got)
	}

	g2, p2, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	c2 := engine.NewScriptedController()
	c2.QueueColorChoice(mana.White)
	host2, err := castETBChain(t, g2, p2, etbChainDef(t, "Hand Colors", line), c2)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g2.Card(host2).Memory.ChosenColors(); got != 0 {
		t.Errorf("chosen colors from an empty hand = %v, want none", got)
	}

	g3, p3, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	if _, err := castETBChain(t, g3, p3, etbChainDef(t, "Bad Zone",
		"DB$ ChooseColor | Defined$ You | ColorsFrom$ ValidNowhere Card"), engine.NewScriptedController()); err == nil {
		t.Error("ColorsFrom$ over an unknown zone resolved, want an error")
	}
}

// ChooseType's Secretly$ records the type like an open choice
// (Card.setSecretChosenType): A Killer Among Us.
func TestChooseTypeSecretly(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueOption(1)
	host, err := castETBChain(t, g, p, etbChainDef(t, "Secret Type",
		"DB$ ChooseType | Type$ Creature | ValidTypes$ Human,Merfolk,Goblin | Secretly$ True"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(host).Memory.ChosenType(false); got != "Merfolk" {
		t.Errorf("chosen type = %q, want Merfolk", got)
	}
}

// NameCard's ChooseFromDefinedCards$ offers the defined cards' names passing
// ValidCards$, sorted (ChooseCardNameEffect.java:62-78): Alhammarret's
// nonland names from a hand.
func TestNameCardChooseFromDefinedCards(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Lightning Bolt"), p, engine.Hand)
	g.NewCard(corpusCard(t, "Forest"), p, engine.Hand)
	g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueOption(1)
	host, err := castETBChain(t, g, p, etbChainDef(t, "Name From Hand",
		"DB$ NameCard | Defined$ You | ValidCards$ Card.nonLand | ChooseFromDefinedCards$ ValidHand Card.YouOwn"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(host).Memory.NamedCards(); len(got) != 1 || got[0] != "Lightning Bolt" {
		t.Errorf("named = %v, want Lightning Bolt (the second of the sorted nonland names)", got)
	}
}
