package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
)

// A Copy-layer replacement whose ReplaceWith$ is not itself a Clone
// (ReplacementHandler runs any ReplaceWith$ ability on the spot): Primal
// Clay's GenericChoice of three Clone modes, and Molten Sentry's FlipCoin.

func primalClayDef(t *testing.T) *compile.Card {
	t.Helper()
	return copyTestDef(t, "Test Primal Clay", "Artifact Creature Shapeshifter", "0", "0", "Cost:G",
		"R:Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | Layer$ Copy | ReplacementResult$ Updated | ReplaceWith$ MoldChoice | Description$ As CARDNAME enters, it becomes your choice of a 3/3, 2/2 flier or 1/6 Wall.",
		"SVar:MoldChoice:DB$ GenericChoice | Defined$ You | Choices$ GroundMold,AirMold,WallMold",
		"SVar:GroundMold:DB$ Clone | Defined$ Self | SetPower$ 3 | SetToughness$ 3",
		"SVar:AirMold:DB$ Clone | Defined$ Self | SetPower$ 2 | SetToughness$ 2 | AddKeywords$ Flying",
		"SVar:WallMold:DB$ Clone | Defined$ Self | SetPower$ 1 | SetToughness$ 6 | AddTypes$ Wall | AddKeywords$ Defender")
}

func TestEntersAsCopyPrimalClayBecomesTheChosenMold(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name         string
		pick         int
		power, tough int
		keyword      string
		wall         bool
	}{
		{"ground", 0, 3, 3, "", false},
		{"air", 1, 2, 2, "Flying", false},
		{"wall", 2, 1, 6, "Defender", true},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			clay := g.NewCard(primalClayDef(t), p, engine.Hand)
			sc := engine.NewScriptedController()
			sc.QueueAbilityChoice([]int{tc.pick})
			mustCastAndResolve(t, g, p, sc, clay)
			wantPT(t, g, clay, tc.power, tc.tough)
			c := g.Card(clay)
			if tc.keyword != "" && !c.HasKeyword(tc.keyword) {
				t.Errorf("missing %s", tc.keyword)
			}
			if c.Type().HasSubtype("Wall") != tc.wall {
				t.Errorf("Wall subtype = %v, want %v", !tc.wall, tc.wall)
			}
		})
	}
}

func TestEntersAsCopyMoltenSentryFlipsForItsShape(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	sentry := g.NewCard(copyTestDef(t, "Test Sentry", "Creature Elf", "0", "0", "Cost:G",
		"R:Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | Layer$ Copy | ReplacementResult$ Updated | ReplaceWith$ TrigFlip | Description$ Flip a coin.",
		"SVar:TrigFlip:DB$ FlipCoin | NoCall$ True | HeadsSubAbility$ DBAttacker | TailsSubAbility$ DBDefender",
		"SVar:DBAttacker:DB$ Clone | Defined$ Self | SetPower$ 5 | SetToughness$ 2 | AddKeywords$ Haste",
		"SVar:DBDefender:DB$ Clone | Defined$ Self | SetPower$ 2 | SetToughness$ 5 | AddKeywords$ Defender"), p, engine.Hand)
	mustCastAndResolve(t, g, p, engine.NewScriptedController(), sentry)

	c := g.Card(sentry)
	pw, _ := c.Power()
	switch {
	case pw == 5 && c.HasKeyword("Haste") && !c.HasKeyword("Defender"):
	case pw == 2 && c.HasKeyword("Defender") && !c.HasKeyword("Haste"):
	default:
		t.Errorf("sentry is power %d, haste %v, defender %v, want 5/2 haste or 2/5 defender", pw, c.HasKeyword("Haste"), c.HasKeyword("Defender"))
	}
}

// A ReplaceWith$ whose API this port has no effect for stops the entry as a
// pending error, not silently entering as itself.
func TestEntersAsCopyUnimplementedReplaceWithIsAnError(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	card := g.NewCard(copyTestDef(t, "Test Mimeo", "Creature Shapeshifter", "0", "0", "Cost:G",
		"K:ETBReplacement:Copy:DBText:Optional",
		"SVar:DBText:DB$ Encode | Defined$ Self"), p, engine.Hand)
	sc := engine.NewScriptedController()
	sc.QueueConfirmEffect(true)
	err := castAndResolve(t, g, p, sc, card)
	if err == nil || !strings.Contains(err.Error(), "copy replacement") {
		t.Fatalf("err = %v, want a copy replacement error", err)
	}
}

// Living Lore's K:ETBReplacement:Copy: the ReplaceWith$ is a ChangeZone that
// exiles a card from the graveyard and remembers it, run before the permanent
// is looked at again.
func TestEntersAsCopyChangeZoneExilesAsItEnters(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	fodder := g.NewCard(giantDef(t), p, engine.Graveyard)
	lore := g.NewCard(copyTestDef(t, "Test Lore", "Creature Elf", "2", "2", "Cost:G",
		"K:ETBReplacement:Copy:ChooseSpell",
		"SVar:ChooseSpell:DB$ ChangeZone | ChangeType$ Creature.YouOwn | ChangeNum$ 1 | Hidden$ True | Origin$ Graveyard | Destination$ Exile | RememberChanged$ True | Mandatory$ True"), p, engine.Hand)
	sc := engine.NewScriptedController()
	sc.QueueCardChoice([]engine.CardID{fodder})
	mustCastAndResolve(t, g, p, sc, lore)

	if z := g.Card(fodder).Zone; z != engine.Exile {
		t.Errorf("graveyard card zone = %v, want Exile", z)
	}
	if g.Card(lore).Zone != engine.Battlefield {
		t.Errorf("lore zone = %v, want Battlefield", g.Card(lore).Zone)
	}
}
