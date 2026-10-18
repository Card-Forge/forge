package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// The amount heads over the turn and game records (amountrecords.go): what
// left a graveyard, how often a player scried or surveiled, drew last turn or
// cast this game, the turn and end-step counts, the mana a cast spent and the
// "most of one kind" counts.

// gainedNow resolves "gain X life" off a host already on p's battlefield, in
// whatever phase the game is in, and answers the life gained.
func gainedNow(t *testing.T, g *engine.Game, p engine.PlayerID, x string, svars ...string) (int, error) {
	t.Helper()
	before := g.Player(p).Life
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ GainLife | Defined$ You | LifeAmount$ X",
		append([]string{"X", x}, svars...)...)
	return g.Player(p).Life - before, err
}

// advanceToTurn walks the game through bookkeeping phases until turn n
// begins (its untap step), every cleanup on the way rolling the turn records
// over.
func advanceToTurn(t *testing.T, g *engine.Game, n int) {
	t.Helper()
	for i := 0; g.Turn() < n; i++ {
		if i > 100 {
			t.Fatalf("turn %d never began (at turn %d)", n, g.Turn())
		}
		g.AdvancePhase(engine.NewScriptedController())
	}
}

// Count$StartingPlayer.<then>.<else> (AbilityUtils.java:2280-2282) reads the
// game's first-turn player: a turn-1 SetTurnState and StartTurn set it,
// SetStartingPlayer injects it, and unset it is unresolved.
func TestCountStartingPlayerReadsTheFirstTurnsPlayer(t *testing.T) {
	t.Parallel()

	if got := gainedFrom(t, nil, "Count$StartingPlayer.5.2"); got != 5 {
		t.Errorf("starting player gained %d, want 5", got)
	}
	notFirst := func(g *engine.Game, _, other engine.PlayerID) { g.SetStartingPlayer(other) }
	if got := gainedFrom(t, notFirst, "Count$StartingPlayer.5.2"); got != 2 {
		t.Errorf("non-starting player gained %d, want 2", got)
	}
	unset := func(g *engine.Game, _, _ engine.PlayerID) { g.SetStartingPlayer(engine.NoPlayer) }
	if err := gainErr(t, unset, "Count$StartingPlayer.5.2"); err == nil || !strings.Contains(err.Error(), "not resolvable") {
		t.Errorf("unset starting player: err = %v, want not resolvable", err)
	}

	g, p, other := newTwoPlayerGame(t)
	g.StartTurn(other, engine.NewScriptedController())
	if got, err := gainedNow(t, g, p, "Count$StartingPlayer.5.2"); err != nil || got != 2 {
		t.Errorf("after StartTurn(other): gained %d, err %v, want 2", got, err)
	}
}

// The scalar record heads.
func TestRecordCountHeads(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		x     string
		setup func(g *engine.Game, p, other engine.PlayerID)
		want  int
	}{
		{"Count$TotalTurns", func(g *engine.Game, p, _ engine.PlayerID) { g.SetTurnState(4, p, engine.Main1) }, 4},
		// The host the line resolves from was itself cast.
		{"Count$YouCastThisGame", nil, 1},
		{"Count$YouScryThisTurn", func(g *engine.Game, p, _ engine.PlayerID) {
			for range 2 {
				if _, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ Scry | ScryNum$ 1"); err != nil {
					t.Fatalf("Scry: %v", err)
				}
			}
		}, 2},
		{"Count$YouSurveilThisTurn", func(g *engine.Game, p, _ engine.PlayerID) {
			if _, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ Surveil | Amount$ 1"); err != nil {
				t.Fatalf("Surveil: %v", err)
			}
		}, 1},
		{"Count$LifeYourTeamGainedThisTurn", func(g *engine.Game, p, _ engine.PlayerID) { g.Player(p).LifeGainedThisTurn = 3 }, 3},
		{"Count$MaxOppDamageThisTurn", func(g *engine.Game, p, _ engine.PlayerID) {
			if _, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ DealDamage | Defined$ Opponent | NumDmg$ 3"); err != nil {
				t.Fatalf("DealDamage: %v", err)
			}
		}, 3},
		{"Count$MaxCombatDamageThisTurn", nil, 0},
		{"Count$MostCardName Creature.YouCtrl", func(g *engine.Game, p, _ engine.PlayerID) {
			addN(t, g, p, engine.Battlefield, 3, "Creature Elf")
		}, 3},
		{"Count$MostCardNameGraveyard Card.YouOwn", func(g *engine.Game, p, _ engine.PlayerID) {
			addN(t, g, p, engine.Graveyard, 2, "Creature Elf")
		}, 2},
		{"Count$MostCardName Creature.OppCtrl", nil, 0},
		{"Count$LeftGraveyardThisTurn Card.YouOwn", func(g *engine.Game, p, _ engine.PlayerID) {
			ids := addN(t, g, p, engine.Graveyard, 3, "Creature Elf")
			g.Move(ids[0], engine.Hand, p)
			g.Move(ids[1], engine.Exile, p)
		}, 2},
		{"Count$LeftGraveyardThisTurn Card.OppOwn", func(g *engine.Game, p, _ engine.PlayerID) {
			ids := addN(t, g, p, engine.Graveyard, 1, "Creature Elf")
			g.Move(ids[0], engine.Hand, p)
		}, 0},
		// The host was cast paying one G (castETBChain).
		{"Count$EachSpentToCast.G", nil, 1},
		{"Count$EachSpentToCast.B", nil, 0},
		{"Count$EachSpentToCast.C", nil, 0},
		{"CastSA>Count$Adamant_1.Green.4.0", nil, 4},
		{"CastSA>Count$Adamant_2.Green.4.0", nil, 0},
		{"CastSA>Count$Adamant_1.Colorless.0.6", nil, 6},
		{"Count$IsPrime Y.1.0", nil, 1},
		{"Count$ValidAll Card.YouOwn", func(g *engine.Game, p, _ engine.PlayerID) {
			addN(t, g, p, engine.Graveyard, 2, "Creature Elf")
			addN(t, g, p, engine.Hand, 1, "Creature Elf")
		}, 4},
	} {
		t.Run(tc.x, func(t *testing.T) {
			t.Parallel()
			if got := gainedFrom(t, tc.setup, tc.x, "Y", "Number$5"); got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
		})
	}
}

// Count$IsPrime <amount>.<then>.<else> (AbilityUtils.java:1616-1622): Guava's
// IntMath.isPrime, which throws on a negative number -- unresolved here.
func TestCountIsPrime(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		y    string
		want int
	}{{"Number$2", 1}, {"Number$4", 0}, {"Number$1", 0}, {"Number$0", 0}, {"Number$13", 1}, {"Number$25", 0}} {
		if got := gainedFrom(t, nil, "Count$IsPrime Y.7.3", "Y", tc.y); got != tc.want*4+3 {
			t.Errorf("IsPrime %s: gained %d, want %d", tc.y, got, tc.want*4+3)
		}
	}
	for _, x := range []string{"Count$IsPrime Y.1", "Count$IsPrime Missing.1.0"} {
		if err := gainErr(t, nil, x, "Y", "Number$5"); err == nil {
			t.Errorf("%s resolved, want an error", x)
		}
	}
	if err := gainErr(t, nil, "Count$IsPrime Y.1.0", "Y", "Count$YourLifeTotal/Minus.30"); err == nil {
		t.Error("a negative IsPrime amount resolved, want an error")
	}
}

// Malformed record heads stay errors naming the value (GO-7).
func TestRecordHeadsRefuseMalformedShapes(t *testing.T) {
	t.Parallel()

	for _, x := range []string{
		"Count$LeftGraveyardThisTurn",
		"Count$MostCardName",
		"Count$MostCardNameNowhere Card",
		"Count$EachSpentToCast",
		"Count$EachSpentToCast.Purple",
		"Count$MostProminentCreatureType",
		"CastSA>Count$Adamant_X.Green.1.0",
		"CastSA>Count$Adamant.Purple.1.0",
		"CastSA>Count$Adamant_1.Green.one.0",
	} {
		t.Run(x, func(t *testing.T) {
			t.Parallel()
			if err := gainErr(t, nil, x); err == nil || !strings.Contains(err.Error(), "not resolvable") {
				t.Errorf("err = %v, want LifeAmount$ not resolvable", err)
			}
		})
	}
}

// Count$MostProminentCreatureType <valid> (CardFactoryUtil.java:318-346): the
// most matching battlefield cards sharing one creature type, which needs the
// DB's subtype vocabulary.
func TestCountMostProminentCreatureType(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	for _, name := range []string{"Llanowar Elves", "Elvish Mystic", "Grizzly Bears"} {
		g.NewCard(corpusCard(t, name), p, engine.Battlefield)
	}
	// The host the line resolves from is an Elf too.
	if got, err := gainedNow(t, g, p, "Count$MostProminentCreatureType Creature.YouCtrl"); err != nil || got != 3 {
		t.Errorf("gained %d, err %v, want 3 (three Elves, one Bear)", got, err)
	}
}

// The records a cleanup rolls over: life lost and cards drawn become last
// turn's, the scries and the graveyard departures of the turn are forgotten,
// this turn's zone changes become last turn's, and the end steps begun count
// again from zero.
func TestTurnRecordsRollOverAtCleanup(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	// An empty library still scries, with nothing to arrange.
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ Scry | ScryNum$ 1"); err != nil {
		t.Fatalf("Scry: %v", err)
	}
	addN(t, g, other, engine.Library, 2, "Creature Elf")
	addN(t, g, p, engine.Library, 2, "Creature Elf")
	g.Player(p).LifeLostThisTurn = 4
	g.Player(other).LifeLostThisTurn = 6
	g.Player(p).CardsDrawnThisTurn = 2
	grave := addN(t, g, p, engine.Graveyard, 1, "Creature Elf")
	g.Move(grave[0], engine.Hand, p)
	hand := addN(t, g, p, engine.Hand, 1, "Creature Elf")
	g.Move(hand[0], engine.Battlefield, p)

	for g.ActivePhase() != engine.EndOfTurn {
		g.AdvancePhase(engine.NewScriptedController())
	}
	if got, err := gainedNow(t, g, p, "Count$FinishedEndOfTurnsThisTurn"); err != nil || got != 0 {
		t.Errorf("in the end step: gained %d, err %v, want 0", got, err)
	}
	// PhaseHandler.java:415-418 zeroes the count as cleanup begins.
	g.AdvancePhase(engine.NewScriptedController())
	if got, err := gainedNow(t, g, p, "Count$FinishedEndOfTurnsThisTurn"); err != nil || got != 0 {
		t.Errorf("in cleanup: gained %d, err %v, want 0", got, err)
	}

	advanceToTurn(t, g, 2)
	for _, tc := range []struct {
		x    string
		want int
	}{
		{"PlayerCountPropertyYou$LifeLostLastTurn", 4},
		{"PlayerCountOpponents$LifeLostLastTurn", 6},
		{"Count$YouDrewLastTurn", 2},
		{"Count$YouScryThisTurn", 0},
		{"Count$LeftGraveyardThisTurn Card.YouOwn", 0},
		{"Count$LastTurnEntered_Battlefield_Creature.YouCtrl", 1},
		{"Count$LastTurnEntered_Hand_from_Graveyard_Card.YouOwn", 1},
		{"Count$ThisTurnEntered_Battlefield_Creature.YouCtrl", 0},
		{"Count$TotalTurns", 2},
		{"Count$FinishedEndOfTurnsThisTurn", 0},
	} {
		if got, err := gainedNow(t, g, p, tc.x); err != nil || got != tc.want {
			t.Errorf("%s on turn 2: gained %d, err %v, want %d", tc.x, got, err, tc.want)
		}
	}
	if got := g.Player(p).LifeLostThisTurn; got != 0 {
		t.Errorf("LifeLostThisTurn = %d after cleanup, want 0", got)
	}
}

// A clone carries the records: a starting player, last turn's spells and the
// graveyard departures read the same off the copy.
func TestCloneCarriesTheTurnRecords(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetStartingPlayer(other)
	ids := addN(t, g, p, engine.Graveyard, 2, "Creature Elf")
	g.Move(ids[0], engine.Hand, p)
	c := g.Clone()
	if got, err := gainedNow(t, c, p, "Count$StartingPlayer.5.2"); err != nil || got != 2 {
		t.Errorf("clone StartingPlayer: gained %d, err %v, want 2", got, err)
	}
	if got, err := gainedNow(t, c, p, "Count$LeftGraveyardThisTurn Card.YouOwn"); err != nil || got != 1 {
		t.Errorf("clone LeftGraveyardThisTurn: gained %d, err %v, want 1", got, err)
	}
}

// Count$ValidAll <valid> (AbilityUtils.java:1951-1956) counts every card in
// the game: Mangara, the Diplomat's attackers of you, read off the combat.
func TestCountValidAllCountsTheAttackersOfYou(t *testing.T) {
	t.Parallel()

	g, p, _, _ := attackingCombat(t)
	if got, err := gainedNow(t, g, p, "Count$ValidAll Creature.attackingYouOrYourPWLKI"); err != nil || got != 1 {
		t.Errorf("gained %d, err %v, want 1 (one creature attacks p)", got, err)
	}
}

// Count$RememberedWithSharedCardType (AbilityUtils.java:2001-2020): the
// largest group of the host's remembered cards sharing a card type with one
// of them, at least 1 -- Rowdy Crew's "if they share a card type".
func TestCountRememberedWithSharedCardType(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name      string
		remembers []string
		want      int
	}{
		{"two creatures and an artifact", []string{"Creature Elf", "Creature Elf", "Artifact"}, 2},
		{"nothing remembered", nil, 1},
		{"an artifact creature joins both", []string{"Creature Elf", "Artifact", "Artifact Creature Elf"}, 3},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			def := etbChainDef(t, "Test Remembers", "DB$ GainLife | Defined$ You | LifeAmount$ X", "X", "Count$RememberedWithSharedCardType")
			host := g.NewCard(def, p, engine.Battlefield)
			for _, typeLine := range tc.remembers {
				g.Card(host).Memory.Remember(engine.CardEntity(addN(t, g, p, engine.Graveyard, 1, typeLine)[0]))
			}
			exec := def.Faces[0].Triggers[0].Subs[0].Ability
			g.PushAbility(engine.Ability{API: engine.APIGainLife, Source: host, Controller: p, Params: exec, Amounts: def.Faces[0].Amounts})
			before := g.Player(p).Life
			if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Player(p).Life - before; got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
		})
	}
}

// Targeted$ChromaSource is each card's own colored mana symbols
// (AbilityUtils.java:2703-2720 with the card as the list).
func TestChromaSourceCountsACardsColoredSymbols(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	target := g.NewCard(amountDef(t, "Pips", "Creature Elf", "2 G G", "2", "2", nil), other, engine.Battlefield)
	before := g.Player(p).Life
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(target)},
		"DB$ GainLife | Defined$ You | LifeAmount$ X", "X", "Targeted$ChromaSource"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life - before; got != 2 {
		t.Errorf("gained %d, want 2 (G G)", got)
	}
}
