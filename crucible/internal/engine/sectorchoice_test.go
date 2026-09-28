package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
)

// sectorAsk is one ChooseSector call as the controller saw it.
type sectorAsk struct {
	decider  engine.PlayerID
	assignee engine.CardID
	sectors  []string
}

// sectorRecorder records every ChooseSector ask, then answers from the
// embedded ScriptedController's own queue.
type sectorRecorder struct {
	*engine.ScriptedController
	asks []sectorAsk
}

func (r *sectorRecorder) ChooseSector(g *engine.Game, decider engine.PlayerID, assignee engine.CardID, sectors []string) int {
	r.asks = append(r.asks, sectorAsk{decider, assignee, append([]string(nil), sectors...)})
	return r.ScriptedController.ChooseSector(g, decider, assignee, sectors)
}

// resolveSectorLine puts a host running line on the battlefield under
// hostController, pushes line with activator as the ability's controller,
// and resolves it on c.
func resolveSectorLine(t *testing.T, g *engine.Game, hostController, activator engine.PlayerID, c engine.PlayerController, line string, svars ...string) (engine.CardID, error) {
	t.Helper()
	def := etbChainDef(t, "Test Sector", line, svars...)
	host := g.NewCard(def, hostController, engine.Battlefield)
	face := def.Faces[0]
	for _, sub := range face.Triggers[0].Subs {
		if !strings.EqualFold(sub.Key, "Execute") {
			continue
		}
		g.PushAbility(engine.Ability{API: engine.APIChooseSector, Source: host, Controller: activator, Params: sub.Ability, Amounts: face.Amounts})
		return host, g.ResolveStack(engine.NewRegistry(), c)
	}
	t.Fatal("no Execute$")
	return engine.NoCard, nil
}

func TestChooseSectorRecordsThePickedSectorOnTheHost(t *testing.T) {
	t.Parallel()

	for i, want := range []string{"Alpha", "Beta", "Gamma"} {
		g, p, _ := newTwoPlayerGame(t)
		c := &sectorRecorder{ScriptedController: engine.NewScriptedController()}
		c.QueueSector(i)
		host, err := resolveSectorLine(t, g, p, p, c, "DB$ ChooseSector")
		if err != nil {
			t.Fatalf("pick %d: %v", i, err)
		}
		if got := g.Card(host).Memory.ChosenSector(); got != want {
			t.Errorf("pick %d: chosen sector = %q, want %q", i, got, want)
		}
		if len(c.asks) != 1 {
			t.Fatalf("pick %d: %d ChooseSector asks, want 1", i, len(c.asks))
		}
		ask := c.asks[0]
		if ask.assignee != engine.NoCard {
			t.Errorf("pick %d: assignee = %v, want NoCard (ChooseSectorEffect.java:12 passes null)", i, ask.assignee)
		}
		if !slices.Equal(ask.sectors, []string{"Alpha", "Beta", "Gamma"}) {
			t.Errorf("pick %d: sectors offered = %v, want [Alpha Beta Gamma] (PlayerController.java:255)", i, ask.sectors)
		}
	}
}

// TestChooseSectorAsksTheHostsControllerNotTheActivator proves the decider
// is card.getController() (ChooseSectorEffect.java:12), not the ability's
// activator.
func TestChooseSectorAsksTheHostsControllerNotTheActivator(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := &sectorRecorder{ScriptedController: engine.NewScriptedController()}
	c.QueueSector(1)
	if _, err := resolveSectorLine(t, g, p, other, c, "DB$ ChooseSector"); err != nil {
		t.Fatal(err)
	}
	if len(c.asks) != 1 || c.asks[0].decider != p {
		t.Errorf("asks = %+v, want one ask of the host's controller %v", c.asks, p)
	}
}

func TestChooseSectorLastPickWins(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueSector(2)
	c.QueueSector(0)
	host := resolveLine(t, g, p, c, "DB$ ChooseSector | SubAbility$ Again", "Again", "DB$ ChooseSector")
	if got := g.Card(host).Memory.ChosenSector(); got != "Alpha" {
		t.Errorf("chosen sector = %q, want Alpha (Card.setChosenSector overwrites)", got)
	}
}

func TestChooseSectorResolvesUltimateAndAILogicLines(t *testing.T) {
	t.Parallel()

	// Space Beleren's -5 line carries both: AILogic$ is an AI hint and
	// Ultimate$ feeds only AchievementTracker.java:23.
	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueSector(1)
	host := resolveLine(t, g, p, c, "DB$ ChooseSector | Planeswalker$ True | Ultimate$ True | AILogic$ Destroy")
	if got := g.Card(host).Memory.ChosenSector(); got != "Beta" {
		t.Errorf("chosen sector = %q, want Beta", got)
	}
}

func TestChooseSectorOutOfRangeAnswerIsAnError(t *testing.T) {
	t.Parallel()

	for _, i := range []int{-1, 3} {
		g, p, _ := newTwoPlayerGame(t)
		c := engine.NewScriptedController()
		c.QueueSector(i)
		host, err := resolveNow(t, g, p, c, nil, "DB$ ChooseSector")
		if err == nil || !strings.Contains(err.Error(), "out of range") {
			t.Errorf("answer %d: err = %v, want an out of range error", i, err)
		}
		if got := g.Card(host).Memory.ChosenSector(); got != "" {
			t.Errorf("answer %d: chosen sector = %q, want none recorded", i, got)
		}
	}
}

func TestChooseSectorRejectsCondition(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ ChooseSector | Condition$ Kicked")
	if err == nil || !strings.Contains(err.Error(), "Condition$ not resolvable yet") {
		t.Errorf("err = %v, want Condition$ rejected", err)
	}
}

func TestChooseSectorSkippedWhenItsConditionFails(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	// No sector is queued: asking would panic the scripted controller.
	host := resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ ChooseSector | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1", "X", "0")
	if got := g.Card(host).Memory.ChosenSector(); got != "" {
		t.Errorf("chosen sector = %q, want none -- X is 0", got)
	}
}

func TestChosenSectorSurvivesCloneIndependently(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueSector(2)
	host := resolveLine(t, g, p, c, "DB$ ChooseSector")
	clone := g.Clone()
	g.Card(host).Memory.SetChosenSector("Alpha")
	if got := clone.Card(host).Memory.ChosenSector(); got != "Gamma" {
		t.Errorf("clone chosen sector = %q, want Gamma", got)
	}
}

// spaceBelerenDef compiles Space Beleren's own ability lines verbatim from
// forge-gui/res/cardsfolder/s/space_beleren.txt (abilities 0: +1, 1: -1,
// 2: -5).
func spaceBelerenDef(t *testing.T) *compile.Card {
	t.Helper()
	raw := &carddb.Card{Filename: "space_beleren"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Space Beleren"
	raw.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), "Legendary Planeswalker Jace")
	raw.Faces[0].InitialLoyalty = "3"
	raw.Faces[0].Abilities = []string{
		"AB$ Effect | Cost$ AddCounter<1/LOYALTY> | Planeswalker$ True | StaticAbilities$ SectorBlock | SpellDescription$ Creatures in each sector can be blocked this turn only by creatures in the same sector.",
		"AB$ ChooseSector | Cost$ SubCounter<1/LOYALTY> | Planeswalker$ True | SubAbility$ DBPutCounterAll | AILogic$ Pump | SpellDescription$ Put a +1/+1 counter on each creature in the sector of your choice.",
		"AB$ ChooseSector | Cost$ SubCounter<5/LOYALTY> | Planeswalker$ True | Ultimate$ True | SubAbility$ DBDestroyAll | AILogic$ Destroy | SpellDescription$ Destroy all creatures in the sector of your choice.",
	}
	raw.Faces[0].SVars.Set("SectorBlock", "Mode$ CantBlockBy | ValidAttacker$ Creature | ValidBlockerRelative$ Creature.DifferentSector | Description$ Creatures in each sector can be blocked this turn only by creatures in the same sector.")
	raw.Faces[0].SVars.Set("DBPutCounterAll", "DB$ PutCounterAll | ValidCards$ Creature.ChosenSector | CounterType$ P1P1 | StackDescription$ None")
	raw.Faces[0].SVars.Set("DBDestroyAll", "DB$ DestroyAll | ValidCards$ Creature.ChosenSector | StackDescription$ None")
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile Space Beleren: %v", err)
	}
	return c
}

// TestSpaceBelerenMinusOneRejectsWhileSectorReadIsNotPorted pins where Space
// Beleren's -1 stands: Creature.ChosenSector (CardProperty.java:119-122) has
// no case in valid.go, so PutCounterAll would put its counter on nothing --
// resolving anyway would silently turn a real effect into a no-op (GO-7).
// chooseSectorReadUnbuilt catches this ahead of time (choosesectoreffect.go):
// the whole ability fails closed, the sector is never asked or recorded, and
// loyalty is never paid (the cost is paid before Resolve runs; the failed
// resolve does not refund it, matching how any other rejected effect leaves
// its already-paid cost spent). This test must change when the sector read
// side lands (port-log effects-choosesector.md).
func TestSpaceBelerenMinusOneRejectsWhileSectorReadIsNotPorted(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Main1)
	pw := g.NewCard(spaceBelerenDef(t), p, engine.Battlefield)
	g.Card(pw).Counters.Add(engine.Loyalty, 3)
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	c := &sectorRecorder{ScriptedController: engine.NewScriptedController()}
	if !g.ActivateAbility(p, pw, 1, c) {
		t.Fatal("ActivateAbility(-1) returned false, want true")
	}
	err := g.ResolveStack(engine.NewRegistry(), c)
	if err == nil || !strings.Contains(err.Error(), "not resolvable yet") {
		t.Fatalf("ResolveStack = %v, want a not-resolvable-yet error", err)
	}
	if got := g.Card(pw).Counters.Count(engine.Loyalty); got != 2 {
		t.Errorf("loyalty = %d, want 2 (cost already paid before the reject)", got)
	}
	if len(c.asks) != 0 {
		t.Errorf("asks = %+v, want none: the sector is never asked for a chain that can't use it", c.asks)
	}
	if got := g.Card(pw).Memory.ChosenSector(); got != "" {
		t.Errorf("chosen sector = %q, want none recorded", got)
	}
	for _, id := range []engine.CardID{mine, theirs} {
		if n := g.Card(id).Counters.Count(engine.P1P1); n != 0 {
			t.Errorf("creature %v P1P1 = %d, want 0", id, n)
		}
	}
}

// TestSpaceBelerenMinusFiveRejectsWhileSectorReadIsNotPorted is the -5's twin
// of the -1 test above: DestroyAll with Creature.ChosenSector is rejected the
// same way, before anything is destroyed. Must change when the sector read
// side lands.
func TestSpaceBelerenMinusFiveRejectsWhileSectorReadIsNotPorted(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Main1)
	pw := g.NewCard(spaceBelerenDef(t), p, engine.Battlefield)
	g.Card(pw).Counters.Add(engine.Loyalty, 6)
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	if !g.ActivateAbility(p, pw, 2, c) {
		t.Fatal("ActivateAbility(-5) returned false, want true")
	}
	err := g.ResolveStack(engine.NewRegistry(), c)
	if err == nil || !strings.Contains(err.Error(), "not resolvable yet") {
		t.Fatalf("ResolveStack = %v, want a not-resolvable-yet error", err)
	}
	if got := g.Card(pw).Memory.ChosenSector(); got != "" {
		t.Errorf("chosen sector = %q, want none recorded", got)
	}
	for _, id := range []engine.CardID{mine, theirs} {
		if z := g.Card(id).Zone; z != engine.Battlefield {
			t.Errorf("creature %v zone = %v, want Battlefield", id, z)
		}
	}
}
