package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// These tests cover the replacement half of Opposition Agent: an Event$
// Moved line naming FoundSearchingLibrary$ applies only to a card a library
// search found (ChangeZoneEffect.java:1308, ReplaceMoved.java:92-98).

// oppositionAgentDef is Opposition Agent's two lines and the SVars its
// replacement runs, word for word.
func oppositionAgentDef(t *testing.T, replacement string) *compile.Card {
	t.Helper()
	return scriptDef(t, "Test Opposition Agent", "Creature Human",
		"S:Mode$ Continuous | Affected$ Opponent | ControlOpponentsSearchingLibrary$ You | Description$ x",
		"R:"+replacement,
		"SVar:RepExile:DB$ ChangeZone | Hidden$ True | Origin$ All | Destination$ Exile | Defined$ ReplacedCard | SubAbility$ DBEffect",
		"SVar:DBEffect:DB$ Effect | Duration$ Permanent | StaticAbilities$ MayPlay | RememberObjects$ ReplacedCard | ForgetOnMoved$ Exile",
		// RepBad fails before it moves the card (Duration$ is refused).
		"SVar:RepBad:DB$ ChangeZone | Hidden$ True | Origin$ All | Destination$ Exile | Defined$ ReplacedCard | Duration$ Permanent",
		"SVar:MayPlay:Mode$ Continuous | MayPlay$ True | Affected$ Card.IsRemembered | AffectedZone$ Exile | MayPlayIgnoreColor$ True | Description$ x",
	)
}

const agentReplacement = "Event$ Moved | ValidCard$ Card.OppOwn | FoundSearchingLibrary$ True | Origin$ Library | ReplaceWith$ RepExile | ActiveZones$ Battlefield | Description$ x"

// castLine casts a sorcery for pid whose spell ability is line and resolves
// the stack on c.
func castLine(t *testing.T, g *engine.Game, pid engine.PlayerID, c *engine.ScriptedController, line string, svars ...string) error {
	t.Helper()
	lines := []string{"A:SP$ " + strings.TrimPrefix(line, "DB$ ")}
	for i := 0; i+1 < len(svars); i += 2 {
		lines = append(lines, "SVar:"+svars[i]+":"+svars[i+1])
	}
	def := scriptDef(t, "Test Sorcery", "Sorcery", lines...)
	def.Faces[0].ManaCost = mana.MustParse("G")
	g.Player(pid).ManaPool.Add(mana.Green, 1)
	card := g.NewCard(def, pid, engine.Hand)
	if !g.CastSpell(pid, card, c) {
		t.Fatal("CastSpell failed")
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

func TestOppositionAgentExilesWhatAnOpponentFindsAndLetsItsControllerPlayIt(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(oppositionAgentDef(t, agentReplacement), p, engine.Battlefield)
	land := g.NewCard(landDef(t, "Forest", "Basic Land Forest"), other, engine.Library)
	g.SetTurnState(1, other, engine.Main1)

	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{land})
	if err := castLine(t, g, other, c, "DB$ ChangeZone | Origin$ Library | Destination$ Battlefield | ChangeType$ Land"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(land).Zone; z != engine.Exile {
		t.Fatalf("found land zone = %v, want Exile", z)
	}
	if got := len(g.Zone(engine.Command, p).Cards()); got != 1 {
		t.Fatalf("effects in the Agent controller's command zone = %d, want 1", got)
	}

	g.SetTurnState(2, p, engine.Main1)
	if !g.PlayLand(p, land, c) {
		t.Fatal("Agent controller could not play the exiled land")
	}
	if card := g.Card(land); card.Zone != engine.Battlefield || card.Controller() != p {
		t.Errorf("played land zone/controller = %v/%v, want Battlefield/%v", card.Zone, card.Controller(), p)
	}
}

// TestOppositionAgentLeavesMovesThatFoundNothingAlone proves the run param
// gate: a library card moved without a search (a Defined$ ChangeZone, a
// NoLooking$ one, a Searched$ one -- Java turns its flag back off at
// ChangeZoneEffect.java:1090-1092) or found by the Agent's own controller is
// not exiled.
func TestOppositionAgentLeavesMovesThatFoundNothingAlone(t *testing.T) {
	t.Parallel()

	for name, tc := range map[string]struct {
		line     string
		ownerIsP bool
		choose   bool
	}{
		"defined imprinted":         {line: "DB$ ChooseCard | ChoiceZone$ Library | Choices$ Land | Mandatory$ True | ImprintChosen$ True | SubAbility$ DBMove", choose: true},
		"no looking":                {line: "DB$ ChangeZone | Origin$ Library | Destination$ Battlefield | ChangeType$ Land | NoLooking$ True", choose: true},
		"searched param":            {line: "DB$ ChangeZone | Origin$ Library | Destination$ Battlefield | ChangeType$ Land | Searched$ True", choose: true},
		"the controller's own find": {line: "DB$ ChangeZone | Origin$ Library | Destination$ Battlefield | ChangeType$ Land", ownerIsP: true, choose: true},
	} {
		t.Run(name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGame(t)
			g.NewCard(oppositionAgentDef(t, agentReplacement), p, engine.Battlefield)
			searcher := other
			if tc.ownerIsP {
				searcher = p
			}
			land := g.NewCard(landDef(t, "Forest", "Basic Land Forest"), searcher, engine.Library)
			g.SetTurnState(1, searcher, engine.Main1)
			c := engine.NewScriptedController()
			if tc.choose {
				c.QueueCardChoice([]engine.CardID{land})
			}
			if err := castLine(t, g, searcher, c, tc.line,
				"DBMove", "DB$ ChangeZone | Defined$ Imprinted | Origin$ Library | Destination$ Battlefield"); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if z := g.Card(land).Zone; z != engine.Battlefield {
				t.Errorf("land zone = %v, want Battlefield", z)
			}
		})
	}
}

// TestFoundSearchingLibraryLinesTheEngineCannotRun covers the refusals: a
// param foundSearchReplaced does not read, and a chain that does not move the
// replaced card, each record a pending error and leave the search's own move
// in place; a Destination$ that is not the search's skips the line quietly.
func TestFoundSearchingLibraryLinesTheEngineCannotRun(t *testing.T) {
	t.Parallel()

	for name, tc := range map[string]struct {
		line      string
		wantError bool
	}{
		"unread param":      {line: strings.Replace(agentReplacement, "| Origin$", "| ValidLKI$ Card | Origin$", 1), wantError: true},
		"chain keeps card":  {line: strings.Replace(agentReplacement, "RepExile", "DBEffect", 1), wantError: true},
		"chain fails early": {line: strings.Replace(agentReplacement, "RepExile", "RepBad", 1), wantError: true},
		"other destination": {line: strings.Replace(agentReplacement, "| Origin$", "| Destination$ Graveyard | Origin$", 1)},
		"other layer":       {line: strings.Replace(agentReplacement, "| Origin$", "| Layer$ Control | Origin$", 1), wantError: true},
		"requirements fail": {line: strings.Replace(agentReplacement, "| Origin$", "| IsPresent$ Card.Nonexistent | Origin$", 1)},
	} {
		t.Run(name, func(t *testing.T) {
			t.Parallel()

			g, p, other := newTwoPlayerGame(t)
			g.NewCard(oppositionAgentDef(t, tc.line), p, engine.Battlefield)
			land := g.NewCard(landDef(t, "Forest", "Basic Land Forest"), other, engine.Library)
			g.SetTurnState(1, other, engine.Main1)
			c := engine.NewScriptedController()
			c.QueueCardChoice([]engine.CardID{land})
			err := castLine(t, g, other, c, "DB$ ChangeZone | Origin$ Library | Destination$ Battlefield | ChangeType$ Land")
			if (err != nil) != tc.wantError {
				t.Errorf("ResolveStack error = %v, want error %v", err, tc.wantError)
			}
			if z := g.Card(land).Zone; z != engine.Battlefield {
				t.Errorf("land zone = %v, want Battlefield", z)
			}
		})
	}
}

// TestOppositionAgentExileKeepsTheSearchsMemory proves Java goes on with the
// replaced card (GameAction.java:368): RememberChanged$ still remembers it,
// and an exiling search marks it exiled with the searching spell.
func TestOppositionAgentExileKeepsTheSearchsMemory(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(oppositionAgentDef(t, agentReplacement), p, engine.Battlefield)
	wanted := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Library)
	g.SetTurnState(1, other, engine.Main1)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{wanted})
	if err := castLine(t, g, other, c, "DB$ ChangeZone | Origin$ Library | Destination$ Exile | ChangeType$ Creature | RememberChanged$ True | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 1 | ConditionDefined$ Remembered | ConditionPresent$ Card | ConditionCompare$ GE1"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(wanted).Zone; z != engine.Exile {
		t.Fatalf("found card zone = %v, want Exile", z)
	}
	if got := g.Player(other).Life; got != 21 {
		t.Errorf("searcher life = %d, want 21 (the exiled card is remembered)", got)
	}
}
