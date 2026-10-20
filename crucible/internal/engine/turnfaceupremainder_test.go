package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/engine"
)

// These tests cover the Event$ TurnFaceUp lines that need more than a plain
// ReplaceWith$: Unable to Scream's Card.EnchantedBy+faceDown gate, Vesuvan
// Shapeshifter's "copy until turned face down" (Clone Duration$
// UntilFacedown, SetState Mode$ TurnFaceDown) and Gift of Doom's Attach |
// Object$ Self | Choices$ (attachObjectToChoice).

// setStateOn resolves `DB$ SetState | ValidTgts$ Creature | Mode$ <mode>`
// targeting id, as p's ability, on c.
func setStateOn(t *testing.T, g *engine.Game, p engine.PlayerID, c engine.PlayerController, id engine.CardID, mode string) error {
	t.Helper()
	def := etbChainDef(t, "Test Turner", "DB$ SetState | ValidTgts$ Creature | Mode$ "+mode)
	host := g.NewCard(def, p, engine.Battlefield)
	sub := def.Faces[0].Triggers[0].Subs[0].Ability
	g.PushAbility(engine.Ability{API: engine.APISetState, Source: host, Controller: p, Params: sub,
		Targets: []engine.EntityID{engine.CardEntity(id)}})
	return g.ResolveStack(engine.NewRegistry(), c)
}

// manifestTop manifests the top card of p's library and returns it.
func manifestTop(t *testing.T, g *engine.Game, p engine.PlayerID) engine.CardID {
	t.Helper()
	top := g.Zone(engine.Library, p).Cards()[0]
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Manifest")
	if !g.Card(top).IsFaceDown() {
		t.Fatal("manifested card is not face down")
	}
	return top
}

func TestUnableToScreamKeepsTheEnchantedCreatureFaceDown(t *testing.T) {
	t.Parallel()

	for name, enchanted := range map[string]bool{"enchanted": true, "not enchanted": false} {
		t.Run(name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Library)
			card := manifestTop(t, g, p)
			aura := g.NewCard(scriptDef(t, "Test Scream", "Enchantment Aura", "K:Enchant:Creature",
				"R:Event$ TurnFaceUp | ValidCard$ Card.EnchantedBy+faceDown | Layer$ CantHappen | ActiveZones$ Battlefield | Description$ x"),
				p, engine.Battlefield)
			if enchanted {
				g.Attach(aura, card)
			}
			if err := setStateOn(t, g, p, engine.NewScriptedController(), card, "TurnFaceUp"); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Card(card).IsFaceDown(); got != enchanted {
				t.Errorf("face down = %v, want %v", got, enchanted)
			}
		})
	}
}

// TestVesuvanShapeshifterCopiesUntilTurnedFaceDown runs Vesuvan
// Shapeshifter's TurnFaceUp line and SVars word for word.
func TestVesuvanShapeshifterCopiesUntilTurnedFaceDown(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	bear := g.NewCard(scriptDef(t, "Test Bear", "Creature Bear"), p, engine.Battlefield)
	g.NewCard(scriptDef(t, "Test Shapeshifter", "Creature Shapeshifter",
		"R:Event$ TurnFaceUp | ValidCard$ Card.Self | Optional$ True | ReplaceWith$ DBCopy | ActiveZones$ Battlefield | Secondary$ True | ReplacementResult$ Updated | Description$ x",
		"SVar:DBCopy:DB$ Clone | Choices$ Creature.Other | AddTriggers$ VesShapeUpkeepTrig | AddSVars$ VesShapeTurn,VesShapeUpkeepTrig | Duration$ UntilFacedown",
		"SVar:VesShapeUpkeepTrig:Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Battlefield | Execute$ VesShapeTurn | OptionalDecider$ You | TriggerDescription$ x",
		"SVar:VesShapeTurn:DB$ SetState | Defined$ Self | Mode$ TurnFaceDown"),
		p, engine.Library)
	ves := manifestTop(t, g, p)

	c := engine.NewScriptedController()
	c.QueueConfirmReplacementEffect(true)
	c.QueueCardChoice([]engine.CardID{bear})
	if err := setStateOn(t, g, p, c, ves, "TurnFaceUp"); err != nil {
		t.Fatalf("turn face up: %v", err)
	}
	if got := g.Card(ves).Name(); got != "Test Bear" || !g.Card(ves).IsCopy() {
		t.Fatalf("name = %q copy = %v, want a copy of Test Bear", got, g.Card(ves).IsCopy())
	}

	if err := setStateOn(t, g, p, engine.NewScriptedController(), ves, "TurnFaceDown"); err != nil {
		t.Fatalf("turn face down: %v", err)
	}
	if card := g.Card(ves); !card.IsFaceDown() || card.IsCopy() {
		t.Fatalf("face down = %v copy = %v, want face down and no copy", card.IsFaceDown(), card.IsCopy())
	}

	// Turned up again, the "you may" is declined: it is itself.
	c = engine.NewScriptedController()
	c.QueueConfirmReplacementEffect(false)
	if err := setStateOn(t, g, p, c, ves, "TurnFaceUp"); err != nil {
		t.Fatalf("turn face up again: %v", err)
	}
	if got := g.Card(ves).Name(); got != "Test Shapeshifter" {
		t.Errorf("name = %q, want Test Shapeshifter", got)
	}
}

// TestTurnFaceDownShapes covers SetState Mode$ TurnFaceDown's no-ops and
// refusals: an already face-down or double-faced permanent does not turn; a
// card off the battlefield and a permanent under a copy effect it does not
// end are refused before anything turns.
func TestTurnFaceDownShapes(t *testing.T) {
	t.Parallel()

	t.Run("face up creature turns", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		bear := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
		if err := setStateOn(t, g, p, engine.NewScriptedController(), bear, "TurnFaceDown"); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if !g.Card(bear).IsFaceDown() {
			t.Error("not face down")
		}
	})

	t.Run("double faced stays", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		def := scriptDef(t, "Test DFC", "Creature Bear")
		def.SplitType = carddb.SplitTransform
		dfc := g.NewCard(def, p, engine.Battlefield)
		if err := setStateOn(t, g, p, engine.NewScriptedController(), dfc, "TurnFaceDown"); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if g.Card(dfc).IsFaceDown() {
			t.Error("a double-faced permanent turned face down")
		}
	})

	t.Run("under a permanent copy", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		g.NewCard(scriptDef(t, "Test Bear", "Creature Bear"), p, engine.Battlefield)
		host := resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Clone | Defined$ Valid Creature.namedTest_Bear")
		if !g.Card(host).IsCopy() {
			t.Fatal("the host is not a copy")
		}
		err := setStateOn(t, g, p, engine.NewScriptedController(), host, "TurnFaceDown")
		if err == nil || !strings.Contains(err.Error(), "another copy effect") {
			t.Errorf("err = %v, want the copy refusal", err)
		}
		if g.Card(host).IsFaceDown() {
			t.Error("turned face down despite the refusal")
		}
	})

	t.Run("off the battlefield", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		// Turning the host down twice: the second is a face-down no-op.
		host, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
			"DB$ SetState | Defined$ Self | Mode$ TurnFaceDown | SubAbility$ DBAgain",
			"DBAgain", "DB$ SetState | Defined$ Self | Mode$ TurnFaceDown")
		if err != nil || !g.Card(host).IsFaceDown() {
			t.Fatalf("ResolveStack: %v, face down %v", err, g.Card(host).IsFaceDown())
		}
		hand := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Hand)
		def := etbChainDef(t, "Test Hand Turner", "DB$ SetState | Defined$ Targeted | Mode$ TurnFaceDown")
		src := g.NewCard(def, p, engine.Battlefield)
		g.PushAbility(engine.Ability{API: engine.APISetState, Source: src, Controller: p,
			Params: def.Faces[0].Triggers[0].Subs[0].Ability, Targets: []engine.EntityID{engine.CardEntity(hand)}})
		err = g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController())
		if err == nil || !strings.Contains(err.Error(), "outside the battlefield") {
			t.Errorf("err = %v, want the zone refusal", err)
		}
	})
}

func TestGiftOfDoomAttachObjectToAChoice(t *testing.T) {
	t.Parallel()

	for name, tc := range map[string]struct {
		typeLine, keyword, choices string
		hostLand, decline          bool
		wantAttached               bool
	}{
		"aura onto a creature":            {typeLine: "Enchantment Aura", keyword: "K:Enchant:Creature", choices: "Creature", wantAttached: true},
		"aura declined":                   {typeLine: "Enchantment Aura", keyword: "K:Enchant:Creature", choices: "Creature", decline: true},
		"aura enchanting players":         {typeLine: "Enchantment Aura", keyword: "K:Enchant:Player", choices: "Creature"},
		"aura whose enchant does not fit": {typeLine: "Enchantment Aura", keyword: "K:Enchant:Land", choices: "Creature"},
		"equipment onto a creature":       {typeLine: "Artifact Equipment", choices: "Creature", wantAttached: true},
		"equipment onto a land":           {typeLine: "Artifact Equipment", choices: "Land", hostLand: true},
		"fortification onto a land":       {typeLine: "Artifact Fortification", choices: "Land", hostLand: true, wantAttached: true},
		"fortification onto a creature":   {typeLine: "Artifact Fortification", choices: "Creature"},
		"a creature attaches nothing":     {typeLine: "Creature Bear", choices: "Creature"},
		"aura without an enchant keyword": {typeLine: "Enchantment Aura", choices: "Creature"},
		"fortification that is a land":    {typeLine: "Land Fortification", choices: "Land", hostLand: true},
	} {
		t.Run(name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			var lines []string
			if tc.keyword != "" {
				lines = append(lines, tc.keyword)
			}
			att := g.NewCard(scriptDef(t, "Test Attachment", tc.typeLine, lines...), p, engine.Battlefield)
			var host engine.CardID
			if tc.hostLand {
				host = g.NewCard(landDef(t, "Forest", "Basic Land Forest"), p, engine.Battlefield)
			} else {
				host = g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
			}
			c := engine.NewScriptedController()
			c.QueueCardChoice([]engine.CardID{host})
			c.QueueConfirmEffect(!tc.decline)
			_, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(att)},
				"DB$ Attach | Object$ Targeted | Choices$ "+tc.choices+".Other+nonEnchantment | Optional$ True")
			if err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			got, ok := g.Card(att).AttachedTo()
			if attached := ok && got == host; attached != tc.wantAttached {
				t.Errorf("attached to the host = %v, want %v", attached, tc.wantAttached)
			}
		})
	}
}

// TestAttachObjectToAChoiceRemembersTheAttachment covers RememberAttached$
// (AttachEffect.java:149-151).
func TestAttachObjectToAChoiceRemembersTheAttachment(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	att := g.NewCard(scriptDef(t, "Test Sword", "Artifact Equipment"), p, engine.Battlefield)
	host := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{host})
	src, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(att)},
		"DB$ Attach | Object$ Targeted | Choices$ Creature.Other+nonEnchantment | RememberAttached$ True")
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	found := false
	for _, e := range g.Card(src).Memory.Remembered() {
		found = found || e == engine.CardEntity(att)
	}
	if !found {
		t.Error("the attached Equipment is not remembered")
	}
}

func TestAttachObjectRefusals(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ line, want string }{
		{"DB$ Attach | Object$ Self | Choices$ Creature | ChoiceZone$ Graveyard", "ChoiceZone$"},
		{"DB$ Attach | Object$ Self | Choices$ Creature | Chooser$ Opponent", "Chooser$"},
	} {
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%q: err = %v, want one naming %s", tc.line, err, tc.want)
		}
	}
}
