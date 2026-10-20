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
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ ChooseSector | Condition$ Teamwork")
	if err == nil || !strings.Contains(err.Error(), `Condition$ "Teamwork" not resolvable yet`) {
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
	raw.Faces[0].Keywords = []string{"Space sculptor"}
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

// assignSectorsTo runs the state-based actions with c answering CR 704.5u's
// sector assignment: sectors[i] for the i'th creature asked, opponents first.
func assignSectorsTo(g *engine.Game, c *sectorRecorder, sectors ...int) {
	for _, i := range sectors {
		c.QueueSector(i)
	}
	engine.CheckStateBasedActions(g, c)
}

// TestSpaceSculptorAssignsEveryCreatureOpponentsFirst pins CR 704.5u's order
// (GameAction.java:1531-1558): the sculptor's controller assigns last, and
// each creature is offered the three sectors in Alpha, Beta, Gamma order.
func TestSpaceSculptorAssignsEveryCreatureOpponentsFirst(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Main1)
	g.Card(g.NewCard(spaceBelerenDef(t), p, engine.Battlefield)).Counters.Add(engine.Loyalty, 3)
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	c := &sectorRecorder{ScriptedController: engine.NewScriptedController()}
	assignSectorsTo(g, c, 2, 1)
	if len(c.asks) != 2 {
		t.Fatalf("%d ChooseSector asks, want 2", len(c.asks))
	}
	if c.asks[0].assignee != theirs || c.asks[0].decider != other {
		t.Errorf("first ask = %+v, want the opponent's creature %v decided by %v", c.asks[0], theirs, other)
	}
	if c.asks[1].assignee != mine || c.asks[1].decider != p {
		t.Errorf("second ask = %+v, want the sculptor's own creature %v decided by %v", c.asks[1], mine, p)
	}
	if !slices.Equal(c.asks[0].sectors, []string{"Alpha", "Beta", "Gamma"}) {
		t.Errorf("sectors offered = %v", c.asks[0].sectors)
	}
	if got := g.Card(theirs).Sector; got != "Gamma" {
		t.Errorf("opponent's creature sector = %q, want Gamma", got)
	}
	if got := g.Card(mine).Sector; got != "Beta" {
		t.Errorf("own creature sector = %q, want Beta", got)
	}

	// A creature that is already assigned is never asked again.
	assignSectorsTo(g, c)
	if len(c.asks) != 2 {
		t.Errorf("%d asks after a second pass, want still 2", len(c.asks))
	}
}

// TestSectorIsForgottenWhenACreatureLeavesTheBattlefield: Java builds a new
// Card object per zone change and never copies the sector.
func TestSectorIsForgottenWhenACreatureLeavesTheBattlefield(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Main1)
	g.Card(g.NewCard(spaceBelerenDef(t), p, engine.Battlefield)).Counters.Add(engine.Loyalty, 3)
	bear := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	assignSectorsTo(g, &sectorRecorder{ScriptedController: engine.NewScriptedController()}, 1)
	if got := g.Card(bear).Sector; got != "Beta" {
		t.Fatalf("sector = %q, want Beta", got)
	}
	g.Move(bear, engine.Graveyard, p)
	if got := g.Card(bear).Sector; got != "" {
		t.Errorf("sector after leaving = %q, want none", got)
	}
}

// TestNoSculptorNoSectors: with no Space sculptor in play CR 704.5u assigns
// nothing and asks nothing.
func TestNoSculptorNoSectors(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Main1)
	bear := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := &sectorRecorder{ScriptedController: engine.NewScriptedController()}
	assignSectorsTo(g, c)
	if len(c.asks) != 0 || g.Card(bear).Sector != "" {
		t.Errorf("asks = %v, sector = %q, want none", c.asks, g.Card(bear).Sector)
	}
}

// TestSpaceBelerenPlusOneBlocksOnlyWithinASector runs the +1's Effect static
// (CantBlockBy, ValidBlockerRelative$ Creature.DifferentSector): the
// attacker can be blocked only by a creature in its own sector.
func TestSpaceBelerenPlusOneBlocksOnlyWithinASector(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Main1)
	pw := g.NewCard(spaceBelerenDef(t), p, engine.Battlefield)
	g.Card(pw).Counters.Add(engine.Loyalty, 3)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	sameSector := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	otherSector := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	c := &sectorRecorder{ScriptedController: engine.NewScriptedController()}
	// Opponent's two creatures first (Beta, Alpha), then the attacker (Beta).
	assignSectorsTo(g, c, 1, 0, 1)
	if !g.CanBlock(attacker, otherSector) {
		t.Fatal("before the +1, a creature in another sector can block")
	}
	if !g.ActivateAbility(p, pw, 0, c) {
		t.Fatal("ActivateAbility(+1) returned false")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if !g.CanBlock(attacker, sameSector) {
		t.Error("a creature in the attacker's own sector can still block")
	}
	if g.CanBlock(attacker, otherSector) {
		t.Error("a creature in a different sector cannot block (ValidBlockerRelative$ Creature.DifferentSector)")
	}
}

// TestSpaceBelerenMinusOneCountersOnlyTheChosenSector and the -5 below run
// the chained PutCounterAll/DestroyAll against Creature.ChosenSector.
func TestSpaceBelerenMinusOneCountersOnlyTheChosenSector(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Main1)
	pw := g.NewCard(spaceBelerenDef(t), p, engine.Battlefield)
	g.Card(pw).Counters.Add(engine.Loyalty, 3)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := &sectorRecorder{ScriptedController: engine.NewScriptedController()}
	assignSectorsTo(g, c, 0, 1)

	c.QueueSector(0)
	if !g.ActivateAbility(p, pw, 1, c) {
		t.Fatal("ActivateAbility(-1) returned false")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if n := g.Card(theirs).Counters.Count(engine.P1P1); n != 1 {
		t.Errorf("Alpha creature P1P1 = %d, want 1", n)
	}
	if n := g.Card(mine).Counters.Count(engine.P1P1); n != 0 {
		t.Errorf("Beta creature P1P1 = %d, want 0", n)
	}
}

func TestSpaceBelerenMinusFiveDestroysOnlyTheChosenSector(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Main1)
	pw := g.NewCard(spaceBelerenDef(t), p, engine.Battlefield)
	g.Card(pw).Counters.Add(engine.Loyalty, 6)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	mine := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	c := &sectorRecorder{ScriptedController: engine.NewScriptedController()}
	assignSectorsTo(g, c, 0, 1)

	c.QueueSector(1)
	if !g.ActivateAbility(p, pw, 2, c) {
		t.Fatal("ActivateAbility(-5) returned false")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(theirs).Zone; z != engine.Battlefield {
		t.Errorf("Alpha creature zone = %v, want Battlefield", z)
	}
	if z := g.Card(mine).Zone; z != engine.Graveyard {
		t.Errorf("Beta creature zone = %v, want Graveyard", z)
	}
}
