package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Subgame (CR 720) plays a whole second game inside one resolution. These
// tests drive it with land-only libraries of at most seven cards, so the
// only decisions a subgame asks for are the starting player and one keep
// per seat: nobody can attack, and nobody ends a turn above hand size. A
// seat whose library is empty loses on its first draw (CR 704.5b), which is
// how every subgame here gets a winner.

// subgameLands puts n basic lands in p's library.
func subgameLands(t *testing.T, g *engine.Game, p engine.PlayerID, n int) []engine.CardID {
	t.Helper()
	ids := make([]engine.CardID, n)
	for i := range ids {
		ids[i] = g.NewCard(landDef(t, "Forest", "Basic Land Forest"), p, engine.Library)
	}
	return ids
}

// subgameController queues the start of one two-seat subgame: first goes
// first, both keep.
func subgameController(first engine.PlayerID) *engine.ScriptedController {
	c := engine.NewScriptedController()
	c.QueueStartingPlayer(first)
	c.QueueKeepHand(true)
	c.QueueKeepHand(true)
	return c
}

// subgameRemembered is host's remembered entities.
func subgameRemembered(g *engine.Game, host engine.CardID) []engine.EntityID {
	return g.Card(host).Memory.Remembered()
}

func sameIDs(a, b []engine.CardID) bool {
	if len(a) != len(b) {
		return false
	}
	seen := make(map[engine.CardID]bool, len(a))
	for _, id := range a {
		seen[id] = true
	}
	for _, id := range b {
		if !seen[id] {
			return false
		}
	}
	return true
}

// The subgame's winner is remembered, and the main game keeps its own
// cards: the subgame played copies (Card.fromPaperCard), so the main
// libraries hold the same cards afterwards, shuffled, and no life changed.
func TestSubgameWinnerRememberedAndMainGameUntouched(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	pLib := subgameLands(t, g, p, 3)
	hand := g.NewCard(landDef(t, "Island", "Basic Land Island"), other, engine.Hand)
	// A token has no paper card: it is not copied into the subgame
	// (setCardsInZone's isToken skip), and it stays where it is.
	token := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.Card(token).IsToken = true

	host := resolveLine(t, g, p, subgameController(p), "DB$ Subgame | RememberPlayers$ Win")

	if got, want := subgameRemembered(g, host), []engine.EntityID{engine.PlayerEntity(p)}; len(got) != 1 || got[0] != want[0] {
		t.Errorf("remembered = %v, want %v (p goes first and other decks on turn 2)", got, want)
	}
	if got := g.Zone(engine.Library, p).Cards(); !sameIDs(got, pLib) {
		t.Errorf("p library = %v, want the same cards %v", got, pLib)
	}
	if g.Zone(engine.Library, other).Len() != 0 || g.Card(hand).Zone != engine.Hand {
		t.Error("other's main-game zones changed")
	}
	if g.Card(token).Zone != engine.Battlefield {
		t.Error("the token moved")
	}
	if g.Player(p).Life != 20 || g.Player(other).Life != 20 || g.Over() {
		t.Error("the subgame's losses reached the main game")
	}
}

// NotWin remembers every player who did not win: here the loser only.
func TestSubgameNotWinRemembersTheLoser(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	subgameLands(t, g, other, 2)

	host := resolveLine(t, g, p, subgameController(other), "DB$ Subgame | RememberPlayers$ NotWin | StartingLife$ 5")

	if got := subgameRemembered(g, host); len(got) != 1 || got[0] != engine.PlayerEntity(p) {
		t.Errorf("remembered = %v, want [p]", got)
	}
}

// StartingLife$ 0 puts every seat at zero, so the first state-based check
// ends the subgame in a draw (CR 104.4a): nobody won, both are NotWin, and
// Win remembers nobody. The default (no StartingLife$) is CR 103.3's 20,
// which is why the other tests here get a winner at all.
func TestSubgameStartingLifeZeroIsADraw(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	subgameLands(t, g, p, 2)
	subgameLands(t, g, other, 2)
	host := resolveLine(t, g, p, subgameController(p), "DB$ Subgame | RememberPlayers$ NotWin | StartingLife$ 0")
	if got := subgameRemembered(g, host); len(got) != 2 || got[0] != engine.PlayerEntity(p) || got[1] != engine.PlayerEntity(other) {
		t.Errorf("NotWin remembered = %v, want [p other]", got)
	}

	g2, p2, _ := newTwoPlayerGame(t)
	host2 := resolveLine(t, g2, p2, subgameController(p2), "DB$ Subgame | RememberPlayers$ Win | StartingLife$ 0")
	if got := subgameRemembered(g2, host2); len(got) != 0 {
		t.Errorf("Win remembered = %v, want none", got)
	}
}

// Enter the Dungeon end to end: the subgame, then RepeatEach over the
// remembered winner, who searches their main-game library for two cards.
func TestSubgameEnterTheDungeonChain(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	lib := subgameLands(t, g, p, 3)
	c := subgameController(p)
	c.QueueCardChoice(lib[:2])

	resolveLine(t, g, p, c,
		"DB$ Subgame | RememberPlayers$ Win | StartingLife$ 5 | SubAbility$ DBRepeatEachPlayer",
		"DBRepeatEachPlayer", "DB$ RepeatEach | RepeatPlayers$ Remembered | ClearRememberedBeforeLoop$ True | RepeatSubAbility$ DBSearch",
		"DBSearch", "DB$ ChangeZone | Origin$ Library | Destination$ Hand | ChangeType$ Card | ChangeNum$ 2 | Mandatory$ True | DefinedPlayer$ Player.IsRemembered")

	for _, id := range lib[:2] {
		if g.Card(id).Zone != engine.Hand {
			t.Errorf("card %d in %v, want Hand", id, g.Card(id).Zone)
		}
	}
	if g.Card(lib[2]).Zone != engine.Library {
		t.Error("the third card left the library")
	}
}

// Shahrazad's chain, with its own X (PlayerCountRemembered$LifeTotal/HalfUp,
// which amount.go does not resolve) replaced by the number it would be: the
// non-winner at 19 loses 10.
func TestSubgameShahrazadChain(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	subgameLands(t, g, p, 1)
	g.Player(other).Life = 19

	resolveLine(t, g, p, subgameController(p),
		"DB$ Subgame | RememberPlayers$ NotWin | SubAbility$ DBRepeatEachPlayer",
		"DBRepeatEachPlayer", "DB$ RepeatEach | RepeatPlayers$ Remembered | ClearRememberedBeforeLoop$ True | RepeatSubAbility$ DBLoseLife",
		"DBLoseLife", "DB$ LoseLife | LifeAmount$ 10 | Defined$ Player.IsRemembered")

	if got := g.Player(other).Life; got != 9 {
		t.Errorf("other life = %d, want 9 (19 - 10)", got)
	}
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("p life = %d, want 20", got)
	}
}

// The Countdown Is at One end to end: the subgame at 1 life, then an
// Effect remembering the non-winner whose damage replacement doubles damage
// dealt to them for the rest of the main game.
func TestSubgameCountdownChain(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	subgameLands(t, g, p, 1)
	resolveLine(t, g, p, subgameController(p),
		"DB$ Subgame | RememberPlayers$ NotWin | StartingLife$ 1 | SubAbility$ DBEffect",
		"DBEffect", "DB$ Effect | RememberObjects$ Player.IsRemembered | ReplacementEffects$ DmgEvent | Duration$ Permanent",
		"DmgEvent", "Event$ DamageDone | ValidTarget$ Player.IsRemembered | ReplaceWith$ DmgTwice | Description$ Double.",
		"DmgTwice", "DB$ ReplaceEffect | VarName$ DamageAmount | VarValue$ X",
		"X", "ReplaceCount$DamageAmount/Twice")

	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ DealDamage | Defined$ Player | NumDmg$ 2")
	if got := g.Player(other).Life; got != 16 {
		t.Errorf("other life = %d, want 16 (2 doubled)", got)
	}
	if got := g.Player(p).Life; got != 18 {
		t.Errorf("p life = %d, want 18 (the winner takes 2)", got)
	}
}

// Defined$ Player.<property> is getDefinedPlayers' filtered fallthrough;
// a property the valid evaluator does not know, or a comma list of them,
// fails loudly.
func TestDefinedPlayerPropertyRejectsUnknownShapes(t *testing.T) {
	t.Parallel()

	for _, def := range []string{"Player.Bogus", "Player.IsRemembered,Active"} {
		g, p, _ := newTwoPlayerGame(t)
		wantSubgameError(t, subgameCast(t, g, p, engine.NewScriptedController(), "DB$ LoseLife | LifeAmount$ 1 | Defined$ "+def),
			`Defined$ "`+def+`" not resolvable yet`)
	}
}

// Only players still in the main game are seated (createSubGame's
// maingame.getPlayers()), and subgame seat i answers for the i-th of them:
// with b out, c is the subgame's second seat, and NotWin names c, not b.
func TestSubgameSeatsOnlyPlayersStillInTheGame(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b", "c")
	ps := g.Players()
	a, b, c := ps[0], ps[1], ps[2]
	g.SetTurnState(1, a, engine.Main1)
	g.Player(a).Life, g.Player(b).Life, g.Player(c).Life = 20, 0, 20
	g.Player(b).Lost = true
	subgameLands(t, g, a, 2)

	// The subgame seats a and c as its players 1 and 2: a goes first, and c
	// decks on turn 2.
	host := resolveLine(t, g, a, subgameController(engine.PlayerID(1)), "DB$ Subgame | RememberPlayers$ NotWin")
	if got := subgameRemembered(g, host); len(got) != 1 || got[0] != engine.PlayerEntity(c) {
		t.Errorf("remembered = %v, want [c]", got)
	}
}

// Attraction and Contraption decks are copied and shuffled with the rest
// (SubgameEffect.java:117-130); the main game's own copies stay put.
func TestSubgameCopiesVariantDecks(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	subgameLands(t, g, p, 1)
	var decks []engine.CardID
	for _, kind := range []engine.ZoneType{engine.AttractionDeck, engine.ContraptionDeck} {
		for i := 0; i < 2; i++ {
			decks = append(decks, g.NewCard(flowCardDef(t, "Widget", "Artifact", ""), p, kind))
		}
	}
	resolveLine(t, g, p, subgameController(p), "DB$ Subgame")
	for _, id := range decks {
		if z := g.Card(id).Zone; z != engine.AttractionDeck && z != engine.ContraptionDeck {
			t.Errorf("card %d left its deck for %v", id, z)
		}
	}
}

// subgameCast casts host (an ETB-chain creature costing G) and resolves its
// trigger, returning the resolution error: castETBChain for any
// PlayerController.
func subgameCast(t *testing.T, g *engine.Game, p engine.PlayerID, c engine.PlayerController, trig string) error {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	host := g.NewCard(etbChainDef(t, "Test Subgame", trig), p, engine.Hand)
	if !g.CastSpell(p, host, c) {
		t.Fatal("CastSpell failed")
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

func wantSubgameError(t *testing.T, err error, fragment string) {
	t.Helper()
	if err == nil || !strings.Contains(err.Error(), fragment) {
		t.Errorf("err = %v, want one containing %q", err, fragment)
	}
}

// Shapes the three real lines never reach fail before anything is built.
func TestSubgameRejectsUnresolvableShapes(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, line, want string
	}{
		{"condition", "DB$ Subgame | Condition$ Kicked", "Condition$ not resolvable yet"},
		{"remember", "DB$ Subgame | RememberPlayers$ Losers", `RememberPlayers$ "Losers" not resolvable yet`},
		{"life", "DB$ Subgame | StartingLife$ X", `StartingLife$ "X" is not an integer`},
	} {
		g, p, _ := newTwoPlayerGame(t)
		wantSubgameError(t, subgameCast(t, g, p, engine.NewScriptedController(), tc.line), tc.want)
	}
}

// A scheme or planar deck needs the game-start actions Archenemy and
// Planechase add (archenemy first, a plane face up), which this port's game
// start does not have.
func TestSubgameRejectsSchemeAndPlanarDecks(t *testing.T) {
	t.Parallel()

	for _, kind := range []engine.ZoneType{engine.SchemeDeck, engine.PlanarDeck} {
		g, p, other := newTwoPlayerGame(t)
		g.NewCard(flowCardDef(t, "Deck Card", "Artifact", ""), other, kind)
		wantSubgameError(t, subgameCast(t, g, p, engine.NewScriptedController(), "DB$ Subgame"), kind.String()+" not resolvable yet")
	}
}

// A Companion among a player's outside-the-game cards would be offered as
// the subgame's companion (Player.assignCompanion); not built.
func TestSubgameRejectsACompanionOutsideTheGame(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(scriptDef(t, "Test Companion", "Creature Elf",
		"K:Companion:Permanent.cmcLE2,Instant,Sorcery:Each permanent card in your starting deck has mana value 2 or less."), p, engine.Graveyard)
	wantSubgameError(t, subgameCast(t, g, p, engine.NewScriptedController(), "DB$ Subgame"), `companion "Test Companion"`)
}

// A card fetched from the subgame's Sideboard (here by Learn) stands for a
// main-game card CR 720.4a moves too; that cross-game move is not built, so
// the whole resolution fails once the subgame ends, before the main game
// is touched.
func TestSubgameRejectsACardTakenFromTheSideboard(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	learn := scriptDef(t, "Test Learn", "Instant", "A:SP$ Learn")
	learn.Faces[0].ManaCost = mana.MustParse("0")
	g.NewCard(learn, p, engine.Library)
	lesson := &compile.Card{Name: "Lesson"}
	lesson.Faces[0].Type = cardtype.Parse(lessonRegistry(t), "Sorcery Lesson")
	g.NewCard(lesson, p, engine.Hand)

	// Subgame arena: p's library copy (Test Learn) is card 1, p's hand
	// Lesson is card 2 in the subgame Sideboard. p goes first and casts
	// Learn in the first upkeep, fetching the Lesson.
	c := subgameController(engine.PlayerID(1))
	c.QueueAction(engine.PlayerID(1), engine.Action{Kind: engine.ActionCast, Card: 1})
	c.QueueCardChoice([]engine.CardID{2})
	wantSubgameError(t, subgameCast(t, g, p, c, "DB$ Subgame | RememberPlayers$ Win"), "Sideboard (CR 720.4a) not resolvable yet")
}

// A starting player who is not a subgame seat is an error, not a panic.
func TestSubgameRejectsAStartingPlayerOutsideTheSubgame(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueStartingPlayer(engine.PlayerID(9))
	wantSubgameError(t, subgameCast(t, g, p, c, "DB$ Subgame"), "starting player 9 is not a subgame seat")
}

// An error inside the subgame -- here a controller's illegal action in its
// first priority round -- fails the Subgame resolution with it.
func TestSubgameErrorInsideTheSubgamePropagates(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := subgameController(p)
	c.QueueAction(p, engine.Action{Kind: engine.ActionKind(99)})
	wantSubgameError(t, subgameCast(t, g, p, c, "DB$ Subgame"), "engine: Subgame: ")
}

// discardingController answers DiscardToHandSize itself, so a subgame can
// run long enough for hands to overflow.
type discardingController struct {
	*engine.ScriptedController
}

func (discardingController) DiscardToHandSize(_ *engine.Game, _ engine.PlayerID, hand []engine.CardID, n int) []engine.CardID {
	return hand[:n]
}

// A subgame still going at the turn cap is an error rather than a guessed
// outcome: libraries of 510 cards outlast 1000 turns.
func TestSubgameTurnCapIsAnError(t *testing.T) {
	t.Parallel()
	if testing.Short() {
		t.Skip("plays a 1000-turn subgame")
	}

	g, p, other := newTwoPlayerGame(t)
	subgameLands(t, g, p, 510)
	subgameLands(t, g, other, 510)
	c := discardingController{subgameController(p)}
	wantSubgameError(t, subgameCast(t, g, p, c, "DB$ Subgame"), "subgame did not end within 1000 turns")
}
