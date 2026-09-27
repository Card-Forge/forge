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

// Unbound Flourishing's own trigger lines, verbatim from
// forge-gui/res/cardsfolder/u/unbound_flourishing.txt. The second trigger
// (Mode$ SpellAbilityCast, copy an instant/sorcery/ability with {X}) is a
// mode checkSpellCastTriggers does not walk, so it never fires here.
const (
	unboundFlourishingDouble = "Mode$ SpellCast | ValidCard$ Permanent | ValidActivatingPlayer$ You | Execute$ TrigDouble | " +
		"TriggerZones$ Battlefield | HasXManaCost$ True | TriggerDescription$ Whenever you cast a permanent spell with a " +
		"mana cost that contains {X}, double the value of X."
	unboundFlourishingTrigDouble = "DB$ ChangeX | Defined$ TriggeredSpellAbility | Value$ TriggeredSpellAbility>Count$xPaid/Twice"
	unboundFlourishingCopy       = "Mode$ SpellAbilityCast | ValidSA$ Instant,Sorcery,Activated | ValidActivatingPlayer$ You | " +
		"Execute$ TrigCopySpell | HasXManaCost$ True | TriggerZones$ Battlefield"
	unboundFlourishingTrigCopy = "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | MayChooseTarget$ True | AILogic$ Always"
)

// watcherDef compiles an Enchantment carrying the given T: lines and SVars,
// the way a card script writes them (TEST-1).
func xWatcherDef(t *testing.T, name string, triggers []string, svars ...string) *compile.Card {
	t.Helper()
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), "Enchantment")
	raw.Faces[0].Triggers = triggers
	for i := 0; i+1 < len(svars); i += 2 {
		raw.Faces[0].SVars.Set(svars[i], svars[i+1])
	}
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// changeXWatcher is a "whenever you cast a spell with {X}" watcher running
// DB$ ChangeX | Defined$ TriggeredSpellAbility with the given Value$.
func changeXWatcher(t *testing.T, value string, svars ...string) *compile.Card {
	t.Helper()
	svars = append([]string{"TrigX", "DB$ ChangeX | Defined$ TriggeredSpellAbility | Value$ " + value}, svars...)
	return xWatcherDef(t, "Test ChangeX Watcher",
		[]string{"Mode$ SpellCast | ValidActivatingPlayer$ You | Execute$ TrigX"}, svars...)
}

// stackXRecorder passes priority like its ScriptedController, recording the
// X the stack's top item carries whenever it is api -- the only way a
// module test sees a spell's X once the trigger above it has resolved.
type stackXRecorder struct {
	*engine.ScriptedController
	api       engine.APIType
	seen      bool
	x         int
	announced bool
}

func (r *stackXRecorder) TakeAction(g *engine.Game, pid engine.PlayerID) engine.Action {
	if top, ok := g.StackTop(); ok && top.API == r.api {
		r.seen = true
		r.x, r.announced = top.XManaCostPaid()
	}
	return r.ScriptedController.TakeAction(g, pid)
}

// castXCreature casts a {X}{G} creature for p with the given X (0 or more;
// -1 casts a plain {G} creature instead), then passes priority until the
// stack is empty, recording the creature spell's X as it sits on top.
func castXCreature(t *testing.T, g *engine.Game, p engine.PlayerID, x int) (*stackXRecorder, error) {
	t.Helper()
	r := &stackXRecorder{ScriptedController: engine.NewScriptedController(), api: engine.APIPermanentCreature}
	cost := "G"
	if x >= 0 {
		cost = "X G"
		r.QueuePayX(x)
		queueXPayGeneric(r.ScriptedController, mana.ShardG, x)
	}
	g.Player(p).ManaPool.Add(mana.Green, max(x, 0)+1)
	creature := g.NewCard(creatureDefManaCost(t, cost), p, engine.Hand)
	if !g.CastSpell(p, creature, r) {
		t.Fatalf("CastSpell failed casting {%s} with X=%d", cost, x)
	}
	err := g.PassPriority(engine.NewRegistry(), r)
	if err == nil && g.Card(creature).Zone != engine.Battlefield {
		t.Errorf("creature zone = %v, want Battlefield", g.Card(creature).Zone)
	}
	return r, err
}

// wantRecordedX fails t unless r saw the spell on top with X = want.
func wantRecordedX(t *testing.T, r *stackXRecorder, want int, announced bool) {
	t.Helper()
	if !r.seen {
		t.Fatal("never saw the spell on top of the stack")
	}
	if r.x != want || r.announced != announced {
		t.Errorf("spell XManaCostPaid() after the trigger = (%d, %v), want (%d, %v)", r.x, r.announced, want, announced)
	}
}

// Unbound Flourishing's real lines: casting a permanent spell with X = 3
// fires its HasXManaCost$ trigger, whose ChangeX doubles the spell's X to
// 6 (TriggeredSpellAbility>Count$xPaid/Twice) before the spell resolves.
func TestUnboundFlourishingDoublesX(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(xWatcherDef(t, "Unbound Flourishing", []string{unboundFlourishingDouble, unboundFlourishingCopy},
		"TrigDouble", unboundFlourishingTrigDouble, "TrigCopySpell", unboundFlourishingTrigCopy), p, engine.Battlefield)

	r, err := castXCreature(t, g, p, 3)
	if err != nil {
		t.Fatal(err)
	}
	wantRecordedX(t, r, 6, true)
}

// Glava's Value$ 5: a literal replaces the announced X outright.
func TestChangeXLiteralValueReplacesX(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(changeXWatcher(t, "5"), p, engine.Battlefield)

	r, err := castXCreature(t, g, p, 1)
	if err != nil {
		t.Fatal(err)
	}
	wantRecordedX(t, r, 5, true)
}

// A named SVar resolves through resolveNamedAmount, and a leading `-`
// negates it (calculateAmount's sign multiplier).
func TestChangeXNamedSVarValue(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(changeXWatcher(t, "-Y", "Y", "2"), p, engine.Battlefield)

	r, err := castXCreature(t, g, p, 4)
	if err != nil {
		t.Fatal(err)
	}
	wantRecordedX(t, r, -2, true)
}

// An operator with an operand (Plus.N) and a leading `-` both apply to the
// triggering spell's xPaid.
func TestChangeXTriggeredXPaidWithOperandAndSign(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(changeXWatcher(t, "-TriggeredSpellAbility>Count$xPaid/Plus.2"), p, engine.Battlefield)

	r, err := castXCreature(t, g, p, 3)
	if err != nil {
		t.Fatal(err)
	}
	wantRecordedX(t, r, -5, true)
}

// A spell whose X was never announced keeps none (ChangeXEffect.java:24,
// :27's null checks), even when Value$ reads its xPaid as 0.
func TestChangeXLeavesUnannouncedXAlone(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(changeXWatcher(t, "TriggeredSpellAbility>Count$xPaid"), p, engine.Battlefield)

	r, err := castXCreature(t, g, p, -1)
	if err != nil {
		t.Fatal(err)
	}
	wantRecordedX(t, r, 0, false)
}

// A copy of a spell keeps its X (CR 707.10): the copy CopySpellAbility puts
// on the stack carries the doubled value ChangeX already wrote.
func TestCopiedSpellKeepsChangedX(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(xWatcherDef(t, "Copier", []string{"Mode$ SpellCast | ValidActivatingPlayer$ You | Execute$ TrigCopy"},
		"TrigCopy", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility"), p, engine.Battlefield)
	g.NewCard(changeXWatcher(t, "TriggeredSpellAbility>Count$xPaid/Twice"), p, engine.Battlefield)
	g.Player(p).ManaPool.Add(mana.White, 3)
	spell := g.NewCard(gainInstant(t, "Gain X", "X W", "1"), p, engine.Hand)
	r := &stackXRecorder{ScriptedController: engine.NewScriptedController(), api: engine.APIGainLife}
	r.QueuePayX(2)
	queueXPayGeneric(r.ScriptedController, mana.ShardW, 2)

	if !g.CastSpell(p, spell, r) {
		t.Fatal("CastSpell failed casting {X}{W} with X=2 and three white")
	}
	if got := g.StackLen(); got != 3 {
		t.Fatalf("StackLen = %d, want 3: the spell and both watchers' triggers", got)
	}
	if top, _ := g.StackTop(); top.API != engine.APIChangeX {
		t.Fatalf("top = %v, want ChangeX resolving before the Copier's trigger", top.API)
	}
	if err := g.PassPriority(engine.NewRegistry(), r); err != nil {
		t.Fatal(err)
	}
	wantRecordedX(t, r, 4, true)
}

// A spell already gone from the stack gets nothing, and nothing fails:
// Java writes to an orphaned SpellAbility no later reader sees.
func TestChangeXAfterSpellLeftStackIsNoOp(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(changeXWatcher(t, "5"), p, engine.Battlefield)
	g.Player(p).ManaPool.Add(mana.Green, 2)
	creature := g.NewCard(creatureDefManaCost(t, "X G"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueuePayX(1)
	queueXPayGeneric(c, mana.ShardG, 1)
	if !g.CastSpell(p, creature, c) {
		t.Fatal("CastSpell failed casting {X}{G} with X=1")
	}
	trigger, _ := g.StackTop()
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if err := engine.NewRegistry().Resolve(g, &trigger, c); err != nil {
		t.Errorf("ChangeX after its spell resolved: %v, want nil", err)
	}
}

// Every shape this port does not resolve is an error before acting (GO-7).
func TestChangeXRejectsUnresolvedShapes(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		name, line, want string
	}{
		{"defined targeted", "DB$ ChangeX | Defined$ Targeted | Value$ 5", `Defined$ "Targeted" not resolvable yet`},
		{"valid tgts", "DB$ ChangeX | ValidTgts$ Card | TgtPrompt$ Select | Value$ 5", "ValidTgts$ not resolvable yet"},
		{"no triggering spell", "DB$ ChangeX | Defined$ TriggeredSpellAbility | Value$ 5", "no triggering spell recorded"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, tc.line)
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Errorf("err = %v, want one containing %q", err, tc.want)
			}
		})
	}
}

// A Value$ shape changeXValue does not evaluate fails the ability rather
// than writing 0 (GO-7).
func TestChangeXRejectsUnresolvedValues(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		name, value, want string
		svars             []string
	}{
		{"other count head", "Count$CardsInYourHand", "not resolvable yet", nil},
		{"unknown operator", "TriggeredSpellAbility>Count$xPaid/Pow.2", "operator not resolvable yet", nil},
		{"unresolvable svar", "Y", "not resolvable yet", []string{"Y", "Count$xPaid"}},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			g.NewCard(changeXWatcher(t, tc.value, tc.svars...), p, engine.Battlefield)
			_, err := castXCreature(t, g, p, 1)
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Errorf("err = %v, want one containing %q", err, tc.want)
			}
		})
	}
}
