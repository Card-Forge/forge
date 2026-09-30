package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// These tests cover Camouflage (ADR-0035): an Event$ DeclareBlocker
// replacement on an effect card whose ReplaceWith$ declares the defending
// player's blocks as piles assigned to the attackers at random, repaired
// by Java's steady-state loop rather than validated.

// camouflageLine is camouflage.txt's replacement and its ReplaceWith$, with
// the ReplaceWith$ line swappable for the declarer/defender variants.
const (
	camouflageReplacement = "Event$ DeclareBlocker | ValidPlayer$ Opponent | ReplaceWith$ DBCamouflage | Description$ Camouflage."
	camouflageReplaceWith = "DB$ Camouflage | Defined$ ReplacedPlayer | Defender$ ReplacedDefendingPlayer | AILogic$ BestBlocker"
)

// camouflageController answers ChooseCardsForEffect from its queue and
// records who was asked and what was offered; being asked to declare
// blockers normally fails the test, since the replacement takes its place.
type camouflageController struct {
	*engine.ScriptedController
	t        *testing.T
	deciders []engine.PlayerID
	offers   [][]engine.CardID
	bounds   [][2]int
}

func newCamouflageController(t *testing.T) *camouflageController {
	return &camouflageController{ScriptedController: engine.NewScriptedController(), t: t}
}

func (c *camouflageController) DeclareCombatBlockers(_ *engine.Game, _ engine.PlayerID, _, _ []engine.CardID) []engine.Block {
	c.t.Error("DeclareCombatBlockers asked: the DeclareBlocker replacement should have taken its place")
	return nil
}

func (c *camouflageController) ChooseCardsForEffect(g *engine.Game, p engine.PlayerID, src engine.CardID, choices []engine.CardID, lo, hi int) []engine.CardID {
	c.deciders = append(c.deciders, p)
	c.offers = append(c.offers, append([]engine.CardID(nil), choices...))
	c.bounds = append(c.bounds, [2]int{lo, hi})
	return c.ScriptedController.ChooseCardsForEffect(g, p, src, choices, lo, hi)
}

// camouflageGame is a two-player combat game in a's first main phase with
// a Camouflage effect card in a's Command zone, its ReplaceWith$ line
// replaceWith and its replacement line replacement.
func camouflageGameWith(t *testing.T, replacement, replaceWith string, svars ...string) (*engine.Game, engine.PlayerID, engine.PlayerID) {
	t.Helper()
	g, a, b := combatGame(t)
	resolveLine(t, g, a, engine.NewScriptedController(),
		"DB$ Effect | ReplacementEffects$ RDeclareBlocker",
		append([]string{"RDeclareBlocker", replacement, "DBCamouflage", replaceWith}, svars...)...)
	return g, a, b
}

func camouflageGame(t *testing.T) (*engine.Game, engine.PlayerID, engine.PlayerID) {
	t.Helper()
	return camouflageGameWith(t, camouflageReplacement, camouflageReplaceWith)
}

// blocksOf is the blockers of attacker in blocks, in order.
func blocksOf(blocks []engine.Block, attacker engine.CardID) []engine.CardID {
	var out []engine.CardID
	for _, b := range blocks {
		if b.Attacker == attacker {
			out = append(out, b.Blocker)
		}
	}
	return out
}

// TestCamouflageReplacesTheBlockerDeclaration proves the replacement takes
// the normal declaration's place: the controller is never asked to declare
// blockers, the defender (ReplacedPlayer, no redirect in this port) is
// asked for one pile of its own creatures, and the pile blocks the only
// attacker.
func TestCamouflageReplacesTheBlockerDeclaration(t *testing.T) {
	t.Parallel()
	g, a, b := camouflageGame(t)
	attacker := g.NewCard(creatureDefPT(t, "3", "3"), a, engine.Battlefield)
	x := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	y := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	declareAttacking(t, g, attacker)

	c := newCamouflageController(t)
	c.QueueCardChoice([]engine.CardID{x, y})
	got := declareBlockers(t, g, c)

	if want := []engine.CardID{x, y}; !slices.Equal(blocksOf(got, attacker), want) {
		t.Errorf("blockers = %v, want %v", blocksOf(got, attacker), want)
	}
	if !slices.Equal(g.Blocks(), got) {
		t.Errorf("combat blocks = %v, want the returned %v", g.Blocks(), got)
	}
	if len(c.deciders) != 1 || c.deciders[0] != b {
		t.Errorf("deciders = %v, want the defender %v once", c.deciders, b)
	}
	if want := []engine.CardID{x, y}; len(c.offers) != 1 || !slices.Equal(c.offers[0], want) || c.bounds[0] != [2]int{0, 2} {
		t.Errorf("offers = %v bounds %v, want %v between 0 and 2", c.offers, c.bounds, want)
	}
}

// TestCamouflageDeclarerAndDefenderAreDistinctValues proves Defined$ and
// Defender$ are read as two values: with Defined$ absent the declarer is
// Java's default "You", the caster, while Defender$
// ReplacedDefendingPlayer still names the defending player whose creatures
// make up the pool.
func TestCamouflageDeclarerAndDefenderAreDistinctValues(t *testing.T) {
	t.Parallel()
	g, a, b := camouflageGameWith(t, camouflageReplacement, "DB$ Camouflage | Defender$ ReplacedDefendingPlayer")
	attacker := g.NewCard(creatureDefPT(t, "3", "3"), a, engine.Battlefield)
	x := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	declareAttacking(t, g, attacker)

	c := newCamouflageController(t)
	c.QueueCardChoice([]engine.CardID{x})
	got := declareBlockers(t, g, c)

	if len(c.deciders) != 1 || c.deciders[0] != a {
		t.Errorf("deciders = %v, want the caster %v", c.deciders, a)
	}
	if len(c.offers) != 1 || !slices.Equal(c.offers[0], []engine.CardID{x}) {
		t.Errorf("offers = %v, want the defender's creature %v", c.offers, x)
	}
	if !slices.Equal(blocksOf(got, attacker), []engine.CardID{x}) {
		t.Errorf("blocks = %v, want %v blocking", got, x)
	}
}

// TestCamouflagePilesAreAssignedToAttackersAtRandom proves one pile per
// attacker, a chosen creature leaving the pool, and each pile going whole
// to one attacker -- which one is g's shuffle, deterministic for its seed.
func TestCamouflagePilesAreAssignedToAttackersAtRandom(t *testing.T) {
	t.Parallel()
	g, a, b := camouflageGame(t)
	first := g.NewCard(creatureDefPT(t, "3", "3"), a, engine.Battlefield)
	second := g.NewCard(creatureDefPT(t, "3", "3"), a, engine.Battlefield)
	x := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	y := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	z := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	declareAttacking(t, g, first, second)
	replay := g.Clone()

	run := func(g *engine.Game) ([]engine.Block, *camouflageController) {
		c := newCamouflageController(t)
		c.QueueCardChoice([]engine.CardID{x})
		c.QueueCardChoice([]engine.CardID{y, z})
		return declareBlockers(t, g, c), c
	}
	got, c := run(g)

	if want := [][]engine.CardID{{x, y, z}, {y, z}}; len(c.offers) != 2 || !slices.Equal(c.offers[0], want[0]) || !slices.Equal(c.offers[1], want[1]) {
		t.Errorf("offers = %v, want %v", c.offers, want)
	}
	one, two := blocksOf(got, first), blocksOf(got, second)
	straight := slices.Equal(one, []engine.CardID{x}) && slices.Equal(two, []engine.CardID{y, z})
	swapped := slices.Equal(one, []engine.CardID{y, z}) && slices.Equal(two, []engine.CardID{x})
	if !straight && !swapped {
		t.Errorf("first blocked by %v, second by %v: want one pile each", one, two)
	}
	if again, _ := run(replay); !slices.Equal(again, got) {
		t.Errorf("replay from the same state = %v, want %v", again, got)
	}
}

// TestCamouflageMenaceNeedsAPileOfTwo proves the minimum-blocker branch
// (CombatUtil.getMinNumBlockersForAttacker): a Menace attacker's pile of
// one blocks nothing, a pile of two blocks.
func TestCamouflageMenaceNeedsAPileOfTwo(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		name string
		pile int
		want int
	}{
		{"pile of one", 1, 0},
		{"pile of two", 2, 2},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, a, b := camouflageGame(t)
			attacker := g.NewCard(creatureDefPTKeywords(t, "3", "3", "Menace"), a, engine.Battlefield)
			x := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
			y := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
			declareAttacking(t, g, attacker)

			c := newCamouflageController(t)
			c.QueueCardChoice([]engine.CardID{x, y}[:tc.pile])
			if got := blocksOf(declareBlockers(t, g, c), attacker); len(got) != tc.want {
				t.Errorf("blockers = %v, want %d", got, tc.want)
			}
		})
	}
}

// TestCamouflageMaxBlockerHasTheDeclarerPickOne proves the maximum-blocker
// branch: a pile larger than a Mode$ MinMaxBlocker Max$ has the declarer
// pick exactly one of it to block.
func TestCamouflageMaxBlockerHasTheDeclarerPickOne(t *testing.T) {
	t.Parallel()
	g, a, b := camouflageGame(t)
	attacker := g.NewCard(combatCreatureDef(t, "Lone Duelist",
		[]string{"Mode$ MinMaxBlocker | ValidCard$ Card.Self | Max$ 1 | Description$ One blocker at most."}, nil), a, engine.Battlefield)
	x := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	y := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	declareAttacking(t, g, attacker)

	c := newCamouflageController(t)
	c.QueueCardChoice([]engine.CardID{x, y})
	c.QueueCardChoice([]engine.CardID{y})
	got := declareBlockers(t, g, c)

	if !slices.Equal(blocksOf(got, attacker), []engine.CardID{y}) {
		t.Errorf("blockers = %v, want only the picked %v", blocksOf(got, attacker), y)
	}
	if len(c.bounds) != 2 || c.bounds[1] != [2]int{1, 1} || !slices.Equal(c.offers[1], []engine.CardID{x, y}) {
		t.Errorf("second ask = %v %v, want exactly one of %v", c.offers, c.bounds, []engine.CardID{x, y})
	}
}

// TestCamouflageRepairsInsteadOfValidating proves ADR-0035's exception to
// ADR-0024: a replaced declaration breaking a can't-block rule is repaired
// by PhaseHandler.java's steady-state loop -- the offending blocker
// silently dropped, no IllegalDeclarationError -- and one ignoring a lure
// requirement stands, since Java's replaced path never validates.
func TestCamouflageRepairsInsteadOfValidating(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		name     string
		keywords []string
		power    string
		// pile picks from the defender's creatures: 0 is the keyword
		// creature, 1 and 2 plain 2/2s.
		pile []int
		want []int
	}{
		{"alone", []string{"CARDNAME can't block alone."}, "2", []int{0}, nil},
		{"attack or block alone", []string{"CARDNAME can't attack or block alone."}, "2", []int{0}, nil},
		{"not alone", []string{"CARDNAME can't block alone."}, "2", []int{0, 1}, []int{0, 1}},
		{"two others missing", []string{"CARDNAME can't block unless at least two other creatures block."}, "2", []int{0, 1}, []int{1}},
		{"two others present", []string{"CARDNAME can't block unless at least two other creatures block."}, "2", []int{0, 1, 2}, []int{0, 1, 2}},
		{"no greater power", []string{"CARDNAME can't block unless a creature with greater power also blocks."}, "2", []int{0, 1}, []int{1}},
		{"greater power", []string{"CARDNAME can't block unless a creature with greater power also blocks."}, "1", []int{0, 1}, []int{0, 1}},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, a, b := camouflageGame(t)
			attacker := g.NewCard(creatureDefPT(t, "5", "5"), a, engine.Battlefield)
			army := []engine.CardID{
				g.NewCard(creatureDefPTKeywords(t, tc.power, "2", tc.keywords...), b, engine.Battlefield),
				g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield),
				g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield),
			}
			declareAttacking(t, g, attacker)

			var pile, want []engine.CardID
			for _, i := range tc.pile {
				pile = append(pile, army[i])
			}
			for _, i := range tc.want {
				want = append(want, army[i])
			}
			c := newCamouflageController(t)
			c.QueueCardChoice(pile)
			if got := blocksOf(declareBlockers(t, g, c), attacker); !slices.Equal(got, want) {
				t.Errorf("blockers = %v, want %v", got, want)
			}
		})
	}

	t.Run("lure ignored", func(t *testing.T) {
		t.Parallel()
		g, a, b := camouflageGame(t)
		attacker := g.NewCard(creatureDefPTKeywords(t, "3", "3", "All creatures able to block CARDNAME do so."), a, engine.Battlefield)
		g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
		declareAttacking(t, g, attacker)

		c := newCamouflageController(t)
		c.QueueCardChoice(nil)
		if got := declareBlockers(t, g, c); len(got) != 0 {
			t.Errorf("blocks = %v, want none: the replaced path never checks the lure", got)
		}
	})
}

// TestCamouflageFailsClosedOnACreatureThatCannotBlock proves the mitigation
// for CamouflageEffect.java:78-80's ConcurrentModificationException
// (forge-java-defects.md): a defender whose creatures include one that
// cannot block (here tapped) is an error before the declarer is asked
// anything, and no block is applied.
func TestCamouflageFailsClosedOnACreatureThatCannotBlock(t *testing.T) {
	t.Parallel()
	g, a, b := camouflageGame(t)
	attacker := g.NewCard(creatureDefPT(t, "3", "3"), a, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	tapped := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	g.Card(tapped).Tapped = true
	declareAttacking(t, g, attacker)

	c := newCamouflageController(t)
	_, err := g.DeclareCombatBlockers(c)
	if err == nil || !strings.Contains(err.Error(), "ConcurrentModificationException") {
		t.Fatalf("err = %v, want the fail-closed pool error", err)
	}
	if len(c.offers) != 0 || len(g.Blocks()) != 0 {
		t.Errorf("offers %v, blocks %v: want nothing asked or applied", c.offers, g.Blocks())
	}
}

// TestCamouflageSkipsADefenderThatCannotBlock proves Java's
// CombatUtil.canBlock(p, combat) guard: a defender with an untapped
// creature that can block no attacker (a flier it cannot reach) is neither
// asked for piles nor asked to declare.
func TestCamouflageSkipsADefenderThatCannotBlock(t *testing.T) {
	t.Parallel()
	g, a, b := camouflageGame(t)
	attacker := g.NewCard(creatureDefPTKeywords(t, "3", "3", "Flying"), a, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	declareAttacking(t, g, attacker)

	c := newCamouflageController(t)
	if got := declareBlockers(t, g, c); len(got) != 0 || len(c.offers) != 0 {
		t.Errorf("blocks %v offers %v, want neither", got, c.offers)
	}
}

// TestCamouflageDoesNotReplaceItsCastersOwnDeclaration proves ValidPlayer$
// Opponent: when the caster defends, the replacement does not apply and
// the normal declaration (and its validation) runs.
func TestCamouflageDoesNotReplaceItsCastersOwnDeclaration(t *testing.T) {
	t.Parallel()
	g, a, b := camouflageGame(t)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	attacker := g.NewCard(creatureDefPT(t, "3", "3"), b, engine.Battlefield)
	g.SetTurnState(2, b, engine.Main1)
	declareAttacking(t, g, attacker)

	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: attacker}})
	if got := blocksOf(declareBlockers(t, g, bc), attacker); !slices.Equal(got, []engine.CardID{blocker}) {
		t.Errorf("blockers = %v, want the normally declared %v", got, blocker)
	}
}

// TestCamouflageRejectsWhatItCannotResolve proves every unresolvable shape
// is an error, not a silent normal declaration: a bad controller answer,
// a Defined$ naming nobody, an unknown replacement param, a chained
// SubAbility$, and Camouflage resolved outside a DeclareBlocker
// replacement.
func TestCamouflageRejectsWhatItCannotResolve(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		name        string
		replacement string
		replaceWith string
		answer      bool
		want        string
	}{
		{"unoffered answer", camouflageReplacement, camouflageReplaceWith, true, "not offered"},
		{"no declarer", camouflageReplacement, "DB$ Camouflage | Defined$ ChosenPlayer | Defender$ ReplacedDefendingPlayer", false, "names no declarer"},
		{"no defender", camouflageReplacement, "DB$ Camouflage | Defined$ ReplacedPlayer | Defender$ ChosenPlayer", false, "names no player"},
		{"bad defender", camouflageReplacement, "DB$ Camouflage | Defined$ ReplacedPlayer | Defender$ Bogus", false, "Defender$"},
		{"bad declarer", camouflageReplacement, "DB$ Camouflage | Defined$ Bogus", false, "Bogus"},
		{"unknown param", "Event$ DeclareBlocker | ValidPlayer$ Opponent | Optional$ True | ReplaceWith$ DBCamouflage", camouflageReplaceWith, false, "param not resolvable yet"},
		{"unknown player spec", "Event$ DeclareBlocker | ValidPlayer$ Player.Bogus | ReplaceWith$ DBCamouflage", camouflageReplaceWith, false, "ValidPlayer$"},
		{"no ReplaceWith", "Event$ DeclareBlocker | ValidPlayer$ Opponent", camouflageReplaceWith, false, "without ReplaceWith$"},
		{"chained", camouflageReplacement, camouflageReplaceWith + " | SubAbility$ DBNoop", false, "SubAbility$"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, a, b := camouflageGameWith(t, tc.replacement, tc.replaceWith, "DBNoop", "DB$ BlankLine")
			attacker := g.NewCard(creatureDefPT(t, "3", "3"), a, engine.Battlefield)
			g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
			declareAttacking(t, g, attacker)

			c := newCamouflageController(t)
			if tc.answer {
				c.QueueCardChoice([]engine.CardID{attacker})
			}
			_, err := g.DeclareCombatBlockers(c)
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Fatalf("err = %v, want one mentioning %q", err, tc.want)
			}
			if len(g.Blocks()) != 0 {
				t.Errorf("blocks = %v, want none applied", g.Blocks())
			}
		})
	}

	t.Run("outside a replacement", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, camouflageReplaceWith)
		if err == nil || !strings.Contains(err.Error(), "not resolving as a DeclareBlocker replacement") {
			t.Fatalf("err = %v, want the outside-a-replacement error", err)
		}
	})
}

// TestCamouflageRejectsABadMaxBlockerPick proves the maximum-blocker pick is
// checked like every other controller answer: a creature outside the pile
// is an error, nothing applied.
func TestCamouflageRejectsABadMaxBlockerPick(t *testing.T) {
	t.Parallel()
	g, a, b := camouflageGame(t)
	attacker := g.NewCard(combatCreatureDef(t, "Lone Duelist",
		[]string{"Mode$ MinMaxBlocker | ValidCard$ Card.Self | Max$ 1 | Description$ One blocker at most."}, nil), a, engine.Battlefield)
	x := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	y := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	declareAttacking(t, g, attacker)

	c := newCamouflageController(t)
	c.QueueCardChoice([]engine.CardID{x, y})
	c.QueueCardChoice([]engine.CardID{attacker})
	if _, err := g.DeclareCombatBlockers(c); err == nil || !strings.Contains(err.Error(), "not offered") {
		t.Fatalf("err = %v, want the unoffered pick rejected", err)
	}
	if len(g.Blocks()) != 0 {
		t.Errorf("blocks = %v, want none applied", g.Blocks())
	}
}

// TestCamouflageSurfacesAnUnresolvableBlockerLimit proves a Mode$
// MinMaxBlocker static this port cannot evaluate stops the replaced
// declaration with an error rather than guessing a limit (GO-7).
func TestCamouflageSurfacesAnUnresolvableBlockerLimit(t *testing.T) {
	t.Parallel()
	g, a, b := camouflageGame(t)
	attacker := g.NewCard(combatCreatureDef(t, "Odd Duelist",
		[]string{"Mode$ MinMaxBlocker | ValidCard$ Card.Self | Max$ Bogus | Description$ Unreadable."}, nil), a, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	declareAttacking(t, g, attacker)

	if _, err := g.DeclareCombatBlockers(newCamouflageController(t)); err == nil || !strings.Contains(err.Error(), "Max$") {
		t.Fatalf("err = %v, want the unresolvable Max$ surfaced", err)
	}
}
