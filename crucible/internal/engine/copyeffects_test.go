package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// These tests cover Clone (CloneEffect.java): Layer 1 copy effects (CR
// 613.2a, 707.2) -- what a copy takes, what its "except" params change,
// how copies stack and end, and how every other layer still folds over the
// copied values.

// copyTestDef compiles a one-face card named name. Each line is a script
// line: "K:..." a keyword, "S:..." a static, "A:..." an ability,
// "SVar:Name:..." an SVar, "Cost:..." the mana cost (default 2 G).
func copyTestDef(t *testing.T, name, typeLine, power, toughness string, lines ...string) *compile.Card {
	t.Helper()
	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\nGiant\nShapeshifter\nZombie\nWall\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: name}
	f := &raw.Faces[0]
	f.Present = true
	f.Name = name
	f.Type = cardtype.Parse(reg, typeLine)
	f.Power, f.Toughness = power, toughness
	f.ManaCost = mana.MustParse("2 G")
	for _, l := range lines {
		head, body, _ := strings.Cut(l, ":")
		switch head {
		case "K":
			f.Keywords = append(f.Keywords, body)
		case "S":
			f.Statics = append(f.Statics, body)
		case "A":
			f.Abilities = append(f.Abilities, body)
		case "T":
			f.Triggers = append(f.Triggers, body)
		case "R":
			f.Replacements = append(f.Replacements, body)
		case "SVar":
			k, v, _ := strings.Cut(body, ":")
			f.SVars.Set(k, v)
		case "Cost":
			f.ManaCost = mana.MustParse(body)
		default:
			t.Fatalf("copyTestDef: unknown line %q", l)
		}
	}
	def, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return def
}

// giantDef is the usual card to copy: a green 4/5 Giant with reach.
func giantDef(t *testing.T) *compile.Card {
	t.Helper()
	return copyTestDef(t, "Copied Giant", "Creature Giant", "4", "5", "K:Reach")
}

// shifter puts a 1/1 Shapeshifter onto p's battlefield whose one ability is
// "AB$ <line>", with svars as "Name:body" pairs.
func shifter(t *testing.T, g *engine.Game, p engine.PlayerID, line string, svars ...string) engine.CardID {
	t.Helper()
	lines := []string{"A:AB$ " + line}
	for _, s := range svars {
		lines = append(lines, "SVar:"+s)
	}
	return g.NewCard(copyTestDef(t, "Shifter", "Creature Shapeshifter", "1", "1", lines...), p, engine.Battlefield)
}

// activate pushes host's first printed ability as p's, with targets, and
// resolves the stack.
func activate(g *engine.Game, p engine.PlayerID, c engine.PlayerController, host engine.CardID, targets ...engine.CardID) error {
	def := g.Card(host).UncopiedDef()
	ab := def.Faces[0].Abilities[0]
	api, _ := engine.APIByName(ab.Name)
	var ts []engine.EntityID
	for _, id := range targets {
		ts = append(ts, engine.CardEntity(id))
	}
	g.PushAbility(engine.Ability{API: api, Source: host, Controller: p, Params: ab, Amounts: def.Faces[0].Amounts, Targets: ts})
	return g.ResolveStack(engine.NewRegistry(), c)
}

func mustActivate(t *testing.T, g *engine.Game, p engine.PlayerID, c engine.PlayerController, host engine.CardID, targets ...engine.CardID) {
	t.Helper()
	if err := activate(g, p, c, host, targets...); err != nil {
		t.Fatal(err)
	}
}

// wantPT fails the test unless id's current power and toughness are p/tough.
func wantPT(t *testing.T, g *engine.Game, id engine.CardID, p, tough int) {
	t.Helper()
	c := g.Card(id)
	gotP, okP := c.Power()
	gotT, okT := c.Toughness()
	if !okP || !okT || gotP != p || gotT != tough {
		t.Errorf("%s is %d/%d (ok %v/%v), want %d/%d", c.Def.Name, gotP, gotT, okP, okT, p, tough)
	}
}

// TestCloneHostBecomesPermanentCopyOfTarget proves the plain targeted shape
// (Mizzium Transreliquat without Duration$): the host takes the target's
// name, types, power/toughness, keywords and color, keeps its controller,
// and with no Duration$ keeps the copy past cleanup.
func TestCloneHostBecomesPermanentCopyOfTarget(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature")
	mustActivate(t, g, p, c, host, giant)

	h := g.Card(host)
	if h.Def.Name != "Copied Giant" || !h.Type().HasSubtype("Giant") || h.Type().HasSubtype("Shapeshifter") ||
		!h.HasKeyword("Reach") || !h.Colors().Has(mana.Green) || !h.IsCopy() {
		t.Errorf("host is %q %v reach=%v, want a copy of Copied Giant", h.Def.Name, h.Type(), h.HasKeyword("Reach"))
	}
	if h.UncopiedDef().Name != "Shifter" {
		t.Errorf("uncopied name = %q, want Shifter", h.UncopiedDef().Name)
	}
	wantPT(t, g, host, 4, 5)
	if h.Controller() != p {
		t.Error("a copy effect changed the copy's controller")
	}
	advanceToCleanup(g, c)
	if g.Card(host).Def.Name != "Copied Giant" {
		t.Errorf("a copy with no Duration$ ended at cleanup: host is %q", g.Card(host).Def.Name)
	}
}

// TestCloneUntilEndOfTurnEndsAtCleanup proves Duration$ UntilEndOfTurn (46
// of the corpus's 61 Duration$ Clone lines): the host returns to its own
// characteristics in the cleanup step.
func TestCloneUntilEndOfTurnEndsAtCleanup(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature | Duration$ UntilEndOfTurn")
	mustActivate(t, g, p, c, host, giant)
	wantPT(t, g, host, 4, 5)
	advanceToCleanup(g, c)
	h := g.Card(host)
	if h.Def.Name != "Shifter" || h.IsCopy() || h.HasKeyword("Reach") {
		t.Errorf("after cleanup host is %q copy=%v, want Shifter", h.Def.Name, h.IsCopy())
	}
	wantPT(t, g, host, 1, 1)
}

// TestCloneCopiesStackByTimestamp proves a permanent copy and a later
// until-end-of-turn one coexisting: while both apply the latest wins
// (Card.getLastClonedState); once it ends the card is the first copy again,
// not its printed self. CloneTarget$ Valid picks the card becoming a copy.
func TestCloneCopiesStackByTimestamp(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(giantDef(t), other, engine.Battlefield)
	b := g.NewCard(copyTestDef(t, "Copied Wall", "Creature Wall", "0", "7", "K:Defender"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host := g.NewCard(copyTestDef(t, "Target Shifter", "Creature Shapeshifter", "1", "1"), p, engine.Battlefield)
	permanent := g.NewCard(copyTestDef(t, "Source A", "Artifact", "", "",
		"A:AB$ Clone | ValidTgts$ Creature | Defined$ Targeted | CloneTarget$ Valid Shapeshifter.YouCtrl"), p, engine.Battlefield)
	temporary := g.NewCard(copyTestDef(t, "Source B", "Artifact", "", "",
		"A:AB$ Clone | ValidTgts$ Creature | Defined$ Targeted | CloneTarget$ Valid Giant.YouCtrl | Duration$ UntilEndOfTurn"),
		p, engine.Battlefield)

	mustActivate(t, g, p, c, permanent, a)
	mustActivate(t, g, p, c, temporary, b)
	if h := g.Card(host); h.Def.Name != "Copied Wall" || !h.HasKeyword("Defender") {
		t.Fatalf("host is %q, want the later copy, Copied Wall", h.Def.Name)
	}
	wantPT(t, g, host, 0, 7)
	advanceToCleanup(g, c)
	if h := g.Card(host); h.Def.Name != "Copied Giant" || !h.IsCopy() {
		t.Errorf("after cleanup host is %q, want the permanent copy of Copied Giant", h.Def.Name)
	}
	wantPT(t, g, host, 4, 5)
}

// TestCloneLaterLayersApplyOverCopiedValues proves Layer 1 sits under the
// others (CR 613.1): a +1/+1 counter, a +2/+2 pump (7c) and a Layer 4 type
// grant on the host before it becomes a copy all still apply to the copied
// values.
func TestCloneLaterLayersApplyOverCopiedValues(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{giant})
	host := resolveLine(t, g, p, c,
		"DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ 1 | SubAbility$ DBPump",
		"DBPump", "DB$ Pump | Defined$ Self | NumAtt$ +2 | NumDef$ +2 | SubAbility$ DBAnimate",
		"DBAnimate", "DB$ Animate | Defined$ Self | Types$ Artifact | SubAbility$ DBClone",
		"DBClone", "DB$ Clone | Choices$ Creature.Other")
	wantPT(t, g, host, 4+2+1, 5+2+1)
	h := g.Card(host)
	if !h.Type().Has(cardtype.Artifact) || !h.Type().HasSubtype("Giant") {
		t.Errorf("host type = %v, want the copied Giant plus the animated Artifact", h.Type())
	}
}

// TestCloneChoicesCopyOntoTarget proves the Cytoshape shape: Choices$ picks
// the card to copy among the matching battlefield cards, and the targeted
// creature -- not the host -- becomes the copy.
func TestCloneChoicesCopyOntoTarget(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	legend := g.NewCard(copyTestDef(t, "Legend", "Legendary Creature Elf", "9", "9"), other, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	sc := engine.NewScriptedController()
	sc.QueueCardChoice([]engine.CardID{giant})
	rec := &offerRecorder{ScriptedController: sc}
	host := shifter(t, g, p, "Clone | Choices$ Creature.nonLegendary | ValidTgts$ Creature | Duration$ UntilEndOfTurn")
	mustActivate(t, g, p, rec, host, victim)

	if g.Card(victim).Def.Name != "Copied Giant" {
		t.Errorf("target is %q, want a copy of Copied Giant", g.Card(victim).Def.Name)
	}
	if g.Card(host).IsCopy() || g.Card(legend).IsCopy() {
		t.Error("something other than the target became a copy")
	}
	if len(rec.offers) != 1 || containsID(rec.offers[0], legend) || !containsID(rec.offers[0], giant) {
		t.Errorf("offered %v, want every nonlegendary creature and not %d", rec.offers, legend)
	}
}

func containsID(ids []engine.CardID, id engine.CardID) bool {
	for _, x := range ids {
		if x == id {
			return true
		}
	}
	return false
}

// TestCloneExceptParamsChangeTheCopiedValues proves getCloneStates' own
// "except" edits: a new name, added types and keywords, set power and
// toughness, a non-legendary copy, an added color, an SVar carried over,
// and the copy entering tapped.
func TestCloneExceptParamsChangeTheCopiedValues(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	legend := g.NewCard(copyTestDef(t, "Legend", "Legendary Creature Elf", "9", "9", "K:Flying", "Cost:W",
		"SVar:X:Number$1"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host := shifter(t, g, p,
		"Clone | ValidTgts$ Creature | NewName$ Renamed | AddTypes$ Artifact & Zombie | AddKeywords$ IfNew Flying & Haste | "+
			"SetPower$ 3 | SetToughness$ Y | NonLegendary$ True | AddColors$ Black | AddSVars$ X,Missing | IntoPlayTapped$ True",
		"Y:Count$Valid Creature", "X:Number$7")
	mustActivate(t, g, p, c, host, legend)

	h := g.Card(host)
	if h.Def.Name != "Renamed" {
		t.Errorf("name = %q, want Renamed", h.Def.Name)
	}
	ty := h.Type()
	if !ty.Has(cardtype.Artifact) || !ty.HasSubtype("Zombie") || !ty.HasSubtype("Elf") ||
		ty.HasSupertype(cardtype.Legendary) {
		t.Errorf("type = %v, want a non-legendary Artifact Creature Elf Zombie", ty)
	}
	flying := 0
	for _, k := range h.KeywordLines() {
		if k == "Flying" {
			flying++
		}
	}
	if flying != 1 || !h.HasKeyword("Haste") {
		t.Errorf("keywords = %v, want one Flying (IfNew) and Haste", h.KeywordLines())
	}
	wantPT(t, g, host, 3, 2)
	if cs := h.Colors(); !cs.Has(mana.White) || !cs.Has(mana.Black) {
		t.Errorf("colors = %v, want white and black", cs)
	}
	if !h.Tapped {
		t.Error("IntoPlayTapped$ did not tap the copy")
	}
	if n, ok := h.Def.Faces[0].Amounts["x"]; !ok || n.String() != "Number$7" {
		t.Errorf("SVar X = %v (%v), want the host's Number$7", n, ok)
	}
	if g.Card(legend).Def.Faces[0].Amounts["x"].String() != "Number$1" {
		t.Error("AddSVars$ wrote through into the copied card's own definition")
	}
}

// TestCloneSetColorAndKeepName proves SetColor$ (which also drops devoid and
// a characteristic-defining color) and KeepName$, and that SetPower$ drops a
// characteristic-defining power ability.
func TestCloneSetColorAndKeepName(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	odd := g.NewCard(copyTestDef(t, "Odd", "Creature Elf", "*", "3", "K:Devoid",
		"S:Mode$ Continuous | EffectZone$ All | Affected$ Card.Self | CharacteristicDefining$ True | SetPower$ X",
		"SVar:X:Count$Valid Creature"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature | SetColor$ Blue | KeepName$ True | SetPower$ 2")
	mustActivate(t, g, p, c, host, odd)
	h := g.Card(host)
	if h.Def.Name != "Shifter" || h.Def.Faces[0].Name != "Shifter" {
		t.Errorf("name = %q, want the kept name Shifter", h.Def.Name)
	}
	if cs := h.Colors(); cs != mana.Blue {
		t.Errorf("colors = %v, want blue only", cs)
	}
	if h.HasKeyword("Devoid") || len(h.Def.Faces[0].Statics) != 0 {
		t.Errorf("devoid=%v statics=%d, want both gone", h.HasKeyword("Devoid"), len(h.Def.Faces[0].Statics))
	}
	wantPT(t, g, host, 2, 3)
	if len(g.Card(odd).Def.Faces[0].Statics) != 1 {
		t.Error("the copy's edits reached the copied card's own definition")
	}
}

// TestCloneOptionalAsksTheHostsController proves Optional$ asks the host's
// controller, and that declining copies nothing.
func TestCloneOptionalAsksTheHostsController(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(false)
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature | Optional$ True")
	mustActivate(t, g, p, c, host, giant)
	if g.Card(host).IsCopy() {
		t.Error("declined Optional$ still copied")
	}
	c.QueueConfirmEffect(true)
	mustActivate(t, g, p, c, host, giant)
	if !g.Card(host).IsCopy() {
		t.Error("accepted Optional$ did not copy")
	}
}

// TestCloneMemoryIsClearedAndRestored proves CloneEffect's memory handling:
// the copy forgets what it remembered and imprinted, RememberCloneOrigin$
// remembers the copied card, and a Duration$ copy's end restores the old
// memory -- minus cards whose owner has left the game.
func TestCloneMemoryIsClearedAndRestored(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	kept := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)
	c := engine.NewScriptedController()
	target := g.NewCard(copyTestDef(t, "Target", "Creature Shapeshifter", "1", "1"), p, engine.Battlefield)
	tm := &g.Card(target).Memory
	tm.Remember(engine.CardEntity(kept))
	tm.Remember(engine.PlayerEntity(other))
	tm.Imprint(kept)
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature | Defined$ Targeted | CloneTarget$ Valid Shapeshifter.Other+YouCtrl | "+
		"RememberCloneOrigin$ True | Duration$ UntilEndOfTurn")
	g.Card(host).Memory.Remember(engine.CardEntity(kept))
	mustActivate(t, g, p, c, host, giant)

	if got := g.Card(target).Memory.Remembered(); len(got) != 1 || got[0] != engine.CardEntity(giant) {
		t.Errorf("copy remembers %v, want only the copied card", got)
	}
	if len(g.Card(target).Memory.Imprinted()) != 0 {
		t.Error("copy still imprints its old card")
	}
	if len(g.Card(host).Memory.Remembered()) != 1 {
		t.Error("the host, not a copy target, lost its memory")
	}
	advanceToCleanup(g, c)
	got := g.Card(target).Memory.Remembered()
	if len(got) != 2 || got[0] != engine.PlayerEntity(other) || got[1] != engine.CardEntity(kept) {
		t.Errorf("after the copy ended target remembers %v, want [player, card]", got)
	}
	if im := g.Card(target).Memory.Imprinted(); len(im) != 1 || im[0] != kept {
		t.Errorf("after the copy ended target imprints %v, want [%d]", im, kept)
	}
}

// TestCloneSelfCopyClearsMemoryBeforeTheSnapshot pins CloneEffect's own
// order: the host copying itself clears its memory before the Duration$
// snapshot, so the copy's end restores nothing -- unless
// ImprintRememberedNoCleanup$ skips that clear, and a card whose owner lost
// is not restored.
func TestCloneSelfCopyClearsMemoryBeforeTheSnapshot(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name      string
		extra     string
		remembers int
	}{
		{"cleared", "", 0},
		{"no cleanup", " | ImprintRememberedNoCleanup$ True", 1},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			giant := g.NewCard(giantDef(t), other, engine.Battlefield)
			mine := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)
			theirs := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Graveyard)
			c := engine.NewScriptedController()
			host := shifter(t, g, p, "Clone | ValidTgts$ Creature | Duration$ UntilEndOfTurn"+tc.extra)
			g.Card(host).Memory.Remember(engine.CardEntity(mine))
			g.Card(host).Memory.Remember(engine.CardEntity(theirs))
			g.Card(host).Memory.Imprint(theirs)
			mustActivate(t, g, p, c, host, giant)
			g.Player(other).Lost = true
			for g.ActivePhase() != engine.Cleanup {
				g.AdvancePhase(c)
			}
			if got := len(g.Card(host).Memory.Remembered()); got != tc.remembers {
				t.Errorf("host remembers %d cards after the copy ended, want %d", got, tc.remembers)
			}
			if got := len(g.Card(host).Memory.Imprinted()); got != 0 {
				t.Errorf("host imprints %d cards of a player who lost, want 0", got)
			}
		})
	}
}

// TestCloneTokenBaseValues proves a token's creating effect's power and
// toughness are copiable (CR 707.2): copying a 5/5 Soldier token gives
// 5/5, and a token becoming a copy loses its own 5/5 until the copy ends.
func TestCloneTokenBaseValues(t *testing.T) {
	t.Parallel()

	g, p, _ := newTokenGame(t)
	c := engine.NewScriptedController()
	src := resolveLine(t, g, p, c,
		"DB$ Token | TokenScript$ w_1_1_soldier | TokenPower$ 5 | TokenToughness$ 5 | RememberTokens$ True")
	soldier := g.Card(src).Memory.Remembered()[0]
	tok, _ := soldier.AsCard()
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature | Duration$ UntilEndOfTurn")
	mustActivate(t, g, p, c, host, tok)
	wantPT(t, g, host, 5, 5)

	giant := g.NewCard(giantDef(t), p, engine.Battlefield)
	copier := g.NewCard(copyTestDef(t, "Token Copier", "Artifact", "", "",
		"A:AB$ Clone | ValidTgts$ Creature | Defined$ Targeted | CloneTarget$ Valid Soldier | Duration$ UntilEndOfTurn"),
		p, engine.Battlefield)
	mustActivate(t, g, p, c, copier, giant)
	wantPT(t, g, tok, 4, 5)
	advanceToCleanup(g, c)
	wantPT(t, g, tok, 5, 5)
	wantPT(t, g, host, 1, 1)
}

// TestCloneCopyOfACopyTakesTheCopiedValues proves CR 707.3: copying a
// permanent that is itself a copy copies what it copied, with its "except"
// changes -- the copied values, not the printed ones.
func TestCloneCopyOfACopyTakesTheCopiedValues(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	first := shifter(t, g, p, "Clone | ValidTgts$ Creature | NewName$ Middle")
	mustActivate(t, g, p, c, first, giant)
	second := shifter(t, g, p, "Clone | ValidTgts$ Creature")
	mustActivate(t, g, p, c, second, first)
	if n := g.Card(second).Def.Name; n != "Middle" {
		t.Errorf("copy of a copy is named %q, want Middle", n)
	}
	wantPT(t, g, second, 4, 5)
}

// TestCloneEndsWhenTheCopyLeavesTheBattlefield proves a copy effect ends
// with the permanent's zone change: in the graveyard it is its own card
// again, and last-known information still sees the copy it died as.
func TestCloneEndsWhenTheCopyLeavesTheBattlefield(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature")
	mustActivate(t, g, p, c, host, giant)
	g.Move(host, engine.Graveyard, p)
	h := g.Card(host)
	if h.Def.Name != "Shifter" || h.IsCopy() {
		t.Errorf("in the graveyard the card is %q copy=%v, want Shifter", h.Def.Name, h.IsCopy())
	}
	if lki := g.LKI(host); lki == nil || lki.Def.Name != "Copied Giant" {
		t.Error("last-known information lost the copy the card died as")
	}
	g.Move(host, engine.Battlefield, p)
	if g.Card(host).IsCopy() {
		t.Error("the copy came back with the card")
	}
}

// TestCloneUntilYourNextTurn proves Duration$ UntilYourNextTurn: the copy
// survives the activator's own cleanup and the opponent's turn, and ends as
// the activator's next turn begins.
func TestCloneUntilYourNextTurn(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature | Duration$ UntilYourNextTurn")
	mustActivate(t, g, p, c, host, giant)
	g.SetTurnState(1, p, engine.Cleanup)
	g.AdvancePhase(c)
	if !g.Card(host).IsCopy() {
		t.Fatal("copy ended at its own turn's cleanup")
	}
	g.SetTurnState(2, other, engine.Cleanup)
	g.AdvancePhase(c)
	if g.Card(host).IsCopy() {
		t.Error("copy did not end as the activator's next turn began")
	}
}

// TestCloneUntilHostLeavesPlay proves Duration$ UntilHostLeavesPlay: the
// copy on another permanent ends when the host leaves, and a host already
// gone resolves to nothing (checkValidDuration).
func TestCloneUntilHostLeavesPlay(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	target := g.NewCard(copyTestDef(t, "Target", "Creature Shapeshifter", "1", "1"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	host := g.NewCard(copyTestDef(t, "Host", "Artifact", "", "",
		"A:AB$ Clone | ValidTgts$ Creature | Defined$ Targeted | CloneTarget$ Valid Shapeshifter | Duration$ UntilHostLeavesPlay"),
		p, engine.Battlefield)
	mustActivate(t, g, p, c, host, giant)
	if !g.Card(target).IsCopy() {
		t.Fatal("target did not become a copy")
	}
	g.Move(host, engine.Graveyard, p)
	if g.Card(target).IsCopy() {
		t.Error("copy outlived its host leaving the battlefield")
	}
	mustActivate(t, g, p, c, host, giant)
	if g.Card(target).IsCopy() {
		t.Error("a host off the battlefield still made a copy")
	}
}

// TestCloneCopiesOnlyTheCurrentFaceOfATransformingCard proves
// getCloneStates' current-state branch: a copy of a transformed permanent is
// a single-faced copy of the face it shows, and a copied permanent cannot
// transform.
func TestCloneCopiesOnlyTheCurrentFaceOfATransformingCard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	front := copyTestDef(t, "Front", "Creature Elf", "1", "1")
	back := copyTestDef(t, "Back", "Creature Giant", "6", "6")
	dfc := &compile.Card{Filename: "dfc", Name: "Front", SplitType: carddb.SplitTransform}
	dfc.Faces[0], dfc.Faces[1] = front.Faces[0], back.Faces[0]
	moon := g.NewCard(dfc, other, engine.Battlefield)
	c := engine.NewScriptedController()
	if _, err := resolveNow(t, g, other, c, []engine.EntityID{engine.CardEntity(moon)},
		"DB$ SetState | Defined$ Targeted | Mode$ Transform"); err != nil {
		t.Fatal(err)
	}
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature")
	mustActivate(t, g, p, c, host, moon)
	h := g.Card(host)
	if h.Def.Name != "Back" || h.Def.SplitType != carddb.SplitNone {
		t.Errorf("copy is %q split=%v, want single-faced Back", h.Def.Name, h.Def.SplitType)
	}
	wantPT(t, g, host, 6, 6)

	// The copied DFC itself transforming back is still allowed; a card
	// under a copy effect is not.
	copier := g.NewCard(dfc, p, engine.Battlefield)
	onto := g.NewCard(copyTestDef(t, "Copier", "Artifact", "", "",
		"A:AB$ Clone | ValidTgts$ Creature | Defined$ Targeted | CloneTarget$ Valid Elf.YouCtrl"), p, engine.Battlefield)
	mustActivate(t, g, p, c, onto, host)
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(copier)},
		"DB$ SetState | Defined$ Targeted | Mode$ Transform"); err != nil {
		t.Fatal(err)
	}
	if g.Card(copier).Def.Name != "Back" || g.Card(copier).Transforms != 0 {
		t.Errorf("copied DFC is %q after %d transforms, want Back and no transform", g.Card(copier).Def.Name, g.Card(copier).Transforms)
	}
	g.Move(copier, engine.Graveyard, p)
	if g.Card(copier).Def.Name != "Front" {
		t.Errorf("in the graveyard the DFC is %q, want its front face", g.Card(copier).Def.Name)
	}
}

// TestCloneChoiceZoneOptionalAndExclude proves ChoiceZone$ (a graveyard
// card), ChoiceOptional$ (picking none copies nothing), ExcludeChosen$ and
// CloneZone$.
func TestCloneChoiceZoneOptionalAndExclude(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	dead := g.NewCard(giantDef(t), other, engine.Graveyard)
	c := engine.NewScriptedController()
	host := shifter(t, g, p, "Clone | Choices$ Creature | ChoiceZone$ Graveyard | ChoiceOptional$ True")
	c.QueueCardChoice(nil)
	mustActivate(t, g, p, c, host)
	if g.Card(host).IsCopy() {
		t.Fatal("picking none still copied")
	}
	c.QueueCardChoice([]engine.CardID{dead})
	mustActivate(t, g, p, c, host)
	if g.Card(host).Def.Name != "Copied Giant" {
		t.Errorf("host is %q, want a copy of the graveyard's Copied Giant", g.Card(host).Def.Name)
	}

	g2, p2, _ := newTwoPlayerGame(t)
	tokenA := g2.NewCard(copyTestDef(t, "Myr", "Creature Elf", "2", "1"), p2, engine.Battlefield)
	tokenB := g2.NewCard(creatureDefPT(t, "1", "1"), p2, engine.Battlefield)
	c2 := engine.NewScriptedController()
	c2.QueueCardChoice([]engine.CardID{tokenA})
	brudiclad := g2.NewCard(copyTestDef(t, "Brudiclad", "Artifact", "", "",
		"A:AB$ Clone | Choices$ Creature.Elf+YouCtrl | ExcludeChosen$ True | CloneTarget$ Valid Creature.Elf+YouCtrl | CloneZone$ Battlefield"),
		p2, engine.Battlefield)
	mustActivate(t, g2, p2, c2, brudiclad)
	if g2.Card(tokenA).IsCopy() || g2.Card(tokenB).Def.Name != "Myr" {
		t.Errorf("chosen copy=%v, other is %q, want the other to become Myr and the chosen one untouched",
			g2.Card(tokenA).IsCopy(), g2.Card(tokenB).Def.Name)
	}
	wantPT(t, g2, tokenB, 2, 1)
}

// TestCloneCopyFromChosenName proves CopyFromChosenName$: the host becomes a
// copy of the card its named card names, straight from the database.
func TestCloneCopyFromChosenName(t *testing.T) {
	t.Parallel()

	giant := giantDef(t)
	g := engine.NewGame(compile.NewDB(map[string]*compile.Card{"Copied Giant": giant}), javarand.New(1), []string{"a", "b"})
	p, other := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20
	c := engine.NewScriptedController()
	host := shifter(t, g, p, "Clone | CopyFromChosenName$ True")
	mustActivate(t, g, p, c, host)
	if g.Card(host).IsCopy() {
		t.Fatal("no named card, yet the host copied something")
	}
	g.Card(host).Memory.AddNamedCard("Copied Giant")
	mustActivate(t, g, p, c, host)
	if g.Card(host).Def.Name != "Copied Giant" {
		t.Errorf("host is %q, want Copied Giant", g.Card(host).Def.Name)
	}
	g.Card(host).Memory.AddNamedCard("No Such Card")
	if err := activate(g, p, c, host); err == nil {
		t.Error("an unknown name resolved")
	}
}

// TestCloneCopyPermanentOfACopiedTokenUsesTheCopiedValues proves
// CopyPermanent reads a copied token's copied values, not the token's own
// hidden base power and toughness.
func TestCloneCopyPermanentOfACopiedTokenUsesTheCopiedValues(t *testing.T) {
	t.Parallel()

	g, p, _ := newTokenGame(t)
	c := engine.NewScriptedController()
	src := resolveLine(t, g, p, c,
		"DB$ Token | TokenScript$ w_1_1_soldier | TokenPower$ 5 | TokenToughness$ 5 | RememberTokens$ True")
	tok, _ := g.Card(src).Memory.Remembered()[0].AsCard()
	giant := g.NewCard(giantDef(t), p, engine.Battlefield)
	copier := g.NewCard(copyTestDef(t, "Token Copier", "Artifact", "", "",
		"A:AB$ Clone | ValidTgts$ Creature | Defined$ Targeted | CloneTarget$ Valid Soldier"), p, engine.Battlefield)
	mustActivate(t, g, p, c, copier, giant)
	populate := g.NewCard(copyTestDef(t, "Populate", "Artifact", "", "",
		"A:AB$ CopyPermanent | ValidTgts$ Creature | RememberTokens$ True"), p, engine.Battlefield)
	mustActivate(t, g, p, c, populate, tok)
	made, _ := g.Card(populate).Memory.Remembered()[0].AsCard()
	if g.Card(made).Def.Name != "Copied Giant" {
		t.Errorf("token copy is %q, want Copied Giant", g.Card(made).Def.Name)
	}
	wantPT(t, g, made, 4, 5)
}

// TestCloneGameCloneKeepsCopiesApart proves Game.Clone copies Layer 1
// state: ending a copy in the clone leaves the original's in place.
func TestCloneGameCloneKeepsCopiesApart(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature | Duration$ UntilEndOfTurn")
	mustActivate(t, g, p, c, host, giant)
	cl := g.Clone()
	advanceToCleanup(cl, c)
	if cl.Card(host).IsCopy() || !g.Card(host).IsCopy() {
		t.Errorf("clone copy=%v original copy=%v, want false/true", cl.Card(host).IsCopy(), g.Card(host).IsCopy())
	}
}

// TestCloneNoCardToCopyDoesNothing proves every branch that finds no card
// to copy resolves to nothing: no Choices$ match, an empty Defined$, no
// card target, no source param at all, and an empty CloneTarget$.
func TestCloneNoCardToCopyDoesNothing(t *testing.T) {
	t.Parallel()

	for _, line := range []string{
		"Clone | Choices$ Creature.Other+OppCtrl",
		"Clone | Defined$ Remembered",
		"Clone | ValidTgts$ Creature",
		"Clone",
		"Clone | Defined$ Self | CloneTarget$ Remembered",
	} {
		g, p, _ := newTwoPlayerGame(t)
		c := engine.NewScriptedController()
		host := shifter(t, g, p, line)
		mustActivate(t, g, p, c, host)
		if g.Card(host).IsCopy() {
			t.Errorf("%q: host became a copy", line)
		}
	}
}

// TestCloneRejectsUnportedShapes proves each shape this port does not
// resolve fails with an error before anything changes (PORT-8, GO-7).
func TestCloneRejectsUnportedShapes(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ line, want string }{
		{"Clone | ValidTgts$ Creature | PumpKeywords$ Haste", "PumpKeywords$ not resolvable yet"},
		{"Clone | ValidTgts$ Creature | RemoveCreatureTypes$ True", "RemoveCreatureTypes$ not resolvable yet"},
		{"Clone | ValidTgts$ Creature | Duration$ UntilUnattached", `Duration$ "UntilUnattached" not resolvable yet`},
		{"Clone | Choices$ Card.token+YouCtrl", `valid property "token"`},
		{"Clone | Choices$ Creature.cmcLEX", `valid property "cmcLEX"`},
		{"Clone | Choices$ Creature.ThisTurnEntered", `valid property "ThisTurnEntered"`},
		{"Clone | ValidTgts$ Creature | CloneTarget$ Valid Creature.NotDefinedTargeted", `"NotDefinedTargeted"`},
		{"Clone | ValidTgts$ Creature | AddColors$ Blue & Black", `AddColors$ "Blue & Black" names no color`},
		{"Clone | ValidTgts$ Creature | SetColor$ Plaid", `SetColor$ "Plaid" names no color`},
		{"Clone | ValidTgts$ Creature | SetPower$ Z", `SetPower$ "Z" is not resolvable`},
		{"Clone | ValidTgts$ Creature | SetToughness$ Z", `SetToughness$ "Z" is not resolvable`},
		{"Clone | ValidTgts$ Creature | CloneTarget$ ParentTarget", `Defined$ "ParentTarget" not resolvable yet`},
		{"Clone | Defined$ TriggeredCardLKICopy", `Defined$ "TriggeredCardLKICopy" not resolvable yet`},
		{"Clone | Choices$ Creature | ChoiceZone$ Nowhere", `ChoiceZone$ "Nowhere" not resolvable`},
		{"Clone | ValidTgts$ Creature | CloneZone$ Nowhere", `CloneZone$ "Nowhere" not resolvable`},
		{"Clone | ValidTgts$ Creature | CloneTarget$ Remembered", "outside the battlefield"},
		{"Clone | ValidTgts$ Creature | CloneTarget$ Imprinted", "face-down"},
	} {
		g, p, other := newTwoPlayerGame(t)
		giant := g.NewCard(giantDef(t), other, engine.Battlefield)
		c := engine.NewScriptedController()
		c.QueueCardChoice([]engine.CardID{giant})
		host := shifter(t, g, p, tc.line)
		inHand := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Hand)
		g.Card(host).Memory.Remember(engine.CardEntity(inHand))
		faceDown := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)
		manifest := g.NewCard(copyTestDef(t, "Manifester", "Artifact", "", "", "A:AB$ Manifest | Defined$ Remembered"), p, engine.Battlefield)
		g.Card(manifest).Memory.Remember(engine.CardEntity(faceDown))
		mustActivate(t, g, p, c, manifest)
		g.Card(host).Memory.Imprint(faceDown)
		err := activate(g, p, c, host, giant)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%q: err = %v, want one containing %q", tc.line, err, tc.want)
		}
		if g.Card(host).IsCopy() {
			t.Errorf("%q: host became a copy despite the error", tc.line)
		}
	}
}

// TestCloneGainThisAbilityKeepsTheActivatedAbility proves GainThisAbility$
// on an activated line (Dimir Doppelganger, Likeness Looter): the copy has
// the copied card's abilities plus the one that made it, which can copy
// again.
func TestCloneGainThisAbilityKeepsTheActivatedAbility(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(copyTestDef(t, "Copied Giant", "Creature Giant", "4", "5",
		"A:AB$ GainLife | LifeAmount$ 1"), other, engine.Battlefield)
	wall := g.NewCard(copyTestDef(t, "Copied Wall", "Creature Wall", "0", "7"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host := shifter(t, g, p, "Clone | ValidTgts$ Creature | GainThisAbility$ True")
	own := g.Card(host).Def.Faces[0].Abilities[0]
	mustActivate(t, g, p, c, host, giant)

	abs := g.Card(host).Def.Faces[0].Abilities
	if len(abs) != 2 || abs[0].Name != "GainLife" || abs[1] != own {
		t.Fatalf("copy abilities = %d, want the Giant's GainLife then the Clone ability", len(abs))
	}
	if len(g.Card(giant).Def.Faces[0].Abilities) != 1 {
		t.Error("gaining the ability wrote into the copied card's definition")
	}
	// Activating the gained ability from the copy copies again, keeping it.
	def := g.Card(host).Def
	g.PushAbility(engine.Ability{API: engine.APIClone, Source: host, Controller: p, Params: abs[1],
		Amounts: def.Faces[0].Amounts, Targets: []engine.EntityID{engine.CardEntity(wall)}})
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if h := g.Card(host); h.Def.Name != "Copied Wall" || len(h.Def.Faces[0].Abilities) != 1 || h.Def.Faces[0].Abilities[0] != own {
		t.Errorf("second copy is %q with %d abilities, want Copied Wall keeping the Clone ability", h.Def.Name, len(h.Def.Faces[0].Abilities))
	}
}

// TestCloneGainThisAbilityKeepsTheTrigger proves GainThisAbility$ on a
// triggered line (Cryptoplasm, Artisan of Forms): the root is the trigger
// whose Execute$ holds the line, found through a sub-ability chain.
func TestCloneGainThisAbilityKeepsTheTrigger(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	host, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(giant)},
		"DB$ GainLife | Defined$ You | LifeAmount$ 1 | SubAbility$ DBCopy", "DBCopy", "DB$ Clone | ValidTgts$ Creature | GainThisAbility$ True")
	if err != nil {
		t.Fatal(err)
	}
	h := g.Card(host)
	trigs := h.Def.Faces[0].Triggers
	if h.Def.Name != "Copied Giant" || len(trigs) != 1 || trigs[0] != h.UncopiedDef().Faces[0].Triggers[0] {
		t.Errorf("copy %q has %d triggers, want Copied Giant with the host's own trigger", h.Def.Name, len(trigs))
	}
}

// TestCloneGainThisAbilityFromElsewhereIsRejected proves a line whose root
// is not among the host's own abilities fails rather than gaining nothing.
func TestCloneGainThisAbilityFromElsewhereIsRejected(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	host := g.NewCard(copyTestDef(t, "Plain", "Creature Elf", "1", "1"), p, engine.Battlefield)
	elsewhere := copyTestDef(t, "Elsewhere", "Artifact", "", "", "A:AB$ Clone | ValidTgts$ Creature | GainThisAbility$ True")
	g.PushAbility(engine.Ability{API: engine.APIClone, Source: host, Controller: p, Params: elsewhere.Faces[0].Abilities[0],
		Targets: []engine.EntityID{engine.CardEntity(giant)}})
	err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController())
	if err == nil || !strings.Contains(err.Error(), "GainThisAbility$ from outside") {
		t.Errorf("err = %v, want the GainThisAbility$ rejection", err)
	}
}

// TestCloneAdventurerKeepsBothFacesAndGainsOnTheCurrentOne proves
// getCloneStates' multi-state branch (an adventurer copies both faces) and
// that a gained trigger lands on the current face alone: this port's
// trigger scans walk every face, so a second copy would fire twice.
func TestCloneAdventurerKeepsBothFacesAndGainsOnTheCurrentOne(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	creature := copyTestDef(t, "Adventurer", "Creature Giant", "4", "3")
	story := copyTestDef(t, "Stomp", "Instant Adventure", "", "")
	adv := &compile.Card{Filename: "adventurer", Name: "Adventurer", SplitType: carddb.SplitAdventure}
	adv.Faces[0], adv.Faces[1] = creature.Faces[0], story.Faces[0]
	source := g.NewCard(adv, other, engine.Battlefield)
	c := engine.NewScriptedController()
	host, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(source)},
		"DB$ Clone | ValidTgts$ Creature | GainThisAbility$ True | AddTypes$ Zombie")
	if err != nil {
		t.Fatal(err)
	}
	d := g.Card(host).Def
	if d.SplitType != carddb.SplitAdventure || d.Faces[1].Name != "Stomp" {
		t.Fatalf("copy split=%v second face %q, want the adventurer's both faces", d.SplitType, d.Faces[1].Name)
	}
	if len(d.Faces[0].Triggers) != 1 || len(d.Faces[1].Triggers) != 0 {
		t.Errorf("gained trigger on faces: %d/%d, want 1/0", len(d.Faces[0].Triggers), len(d.Faces[1].Triggers))
	}
	if !d.Faces[1].Type.HasSubtype("Zombie") {
		t.Error("AddTypes$ skipped the second copied state")
	}
}
