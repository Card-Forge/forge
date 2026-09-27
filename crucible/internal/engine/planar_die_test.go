package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// Planar die faces by PlanarDice.roll's raw nextInt(6): 0 the planeswalker
// symbol, 1 chaos, 2-5 blank.
const (
	dieWalk  = 0
	dieChaos = 1
	dieBlank = 2
)

// dieFace folds a raw nextInt(6) draw onto its face.
func dieFace(draw int32) int32 {
	if draw >= dieBlank {
		return dieBlank
	}
	return draw
}

// planarDieGame is a two-player game in p's first main phase whose random
// stream rolls faces in order: its first nextInt(6) draw lands on faces[0],
// its second on faces[1], and so on.
func planarDieGame(t *testing.T, faces ...int32) (*engine.Game, engine.PlayerID, engine.PlayerID) {
	t.Helper()
	for seed := int64(1); seed < 100000; seed++ {
		r := javarand.New(seed)
		ok := true
		for _, f := range faces {
			if dieFace(r.Int32n(6)) != f {
				ok = false
				break
			}
		}
		if !ok {
			continue
		}
		g := engine.NewGame(nil, javarand.New(seed), []string{"a", "b"})
		p, other := g.Players()[0], g.Players()[1]
		g.SetTurnState(1, p, engine.Main1)
		g.Player(p).Life, g.Player(other).Life = 20, 20
		return g, p, other
	}
	t.Fatalf("no seed rolls %v", faces)
	return nil, engine.NoPlayer, engine.NoPlayer
}

// rollDie resolves line (a RollPlanarDice ability) for p from a fresh
// battlefield host, directly rather than through the stack, so whatever the
// roll triggers is left on the stack for the test to inspect.
func rollDie(t *testing.T, g *engine.Game, p engine.PlayerID, c *engine.ScriptedController, line string) error {
	t.Helper()
	def := etbChainDef(t, "Die Roller", line)
	host := g.NewCard(def, p, engine.Battlefield)
	face := def.Faces[0]
	for _, sub := range face.Triggers[0].Subs {
		if strings.EqualFold(sub.Key, "Execute") {
			a := engine.Ability{API: engine.APIRollPlanarDice, Source: host, Controller: p, Params: sub.Ability, Amounts: face.Amounts}
			return engine.NewRegistry().Resolve(g, &a, c)
		}
	}
	t.Fatal("no Execute$")
	return nil
}

// mustRoll is rollDie failing the test on error, then the stack resolved.
func mustRoll(t *testing.T, g *engine.Game, p engine.PlayerID) {
	t.Helper()
	c := engine.NewScriptedController()
	if err := rollDie(t, g, p, c, "DB$ RollPlanarDice"); err != nil {
		t.Fatalf("RollPlanarDice: %v", err)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
}

// planarDice is every "Planar Dice" card in p's Command zone.
func planarDice(g *engine.Game, p engine.PlayerID) []engine.CardID {
	var dice []engine.CardID
	for _, id := range g.Zone(engine.Command, p).Cards() {
		if g.Card(id).Def.Name == "Planar Dice" {
			dice = append(dice, id)
		}
	}
	return dice
}

// rollTriggerPlane is a Plane whose Mode$ PlanarDice line, with extra
// params, has its controller gain n life.
func rollTriggerPlane(t *testing.T, name, extra, n string) *compile.Card {
	t.Helper()
	return planarDef(t, name, "Plane", []string{"Mode$ PlanarDice | TriggerZones$ Command | Execute$ Rolled" + extra}, nil,
		"Rolled", "DB$ GainLife | Defined$ You | LifeAmount$ "+n)
}

// TestRollingThePlanarDieIsANoOpOutsidePlanechase proves
// RollPlanarDiceEffect.java:25, Fractured Powerstone's usual real path:
// with no planar deck seated nothing is rolled, nothing triggers and no
// Planar Dice card appears.
func TestRollingThePlanarDieIsANoOpOutsidePlanechase(t *testing.T) {
	t.Parallel()

	g, p, _ := planarDieGame(t, dieBlank)
	g.NewCard(rollTriggerPlane(t, "Stairs Test", "", "1"), p, engine.Command)
	if err := rollDie(t, g, p, engine.NewScriptedController(), "DB$ RollPlanarDice"); err != nil {
		t.Fatalf("RollPlanarDice: %v", err)
	}
	if n := g.StackLen(); n != 0 {
		t.Errorf("stack holds %d, want 0", n)
	}
	if dice := planarDice(g, p); len(dice) != 0 {
		t.Errorf("Planar Dice cards %v, want none", dice)
	}
}

// TestRollingThePlaneswalkerSymbolPlaneswalks proves the Planeswalk face
// does not walk inline: the roller's "Planar Dice" effect card
// (Player.createPlanechaseEffects) triggers, its Planeswalk goes on the
// stack under the roller's control, and resolving it walks to the top of
// the roller's planar deck.
func TestRollingThePlaneswalkerSymbolPlaneswalks(t *testing.T) {
	t.Parallel()

	g, p, _ := planarDieGame(t, dieWalk)
	here := faceUpPlane(g, p, plainPlane(t, "Here"))
	deck := planarDeck(g, p, plainPlane(t, "There"))
	c := engine.NewScriptedController()
	if err := rollDie(t, g, p, c, "DB$ RollPlanarDice"); err != nil {
		t.Fatalf("RollPlanarDice: %v", err)
	}
	if g.ActivePlane() != here {
		t.Fatalf("walked before the trigger resolved: ActivePlane = %v", g.ActivePlane())
	}
	dice := planarDice(g, p)
	if len(dice) != 1 {
		t.Fatalf("Planar Dice cards %v, want one in p's Command zone", dice)
	}
	if !g.IsDesignationCard(dice[0]) || !g.Card(dice[0]).IsEffect {
		t.Error("Planar Dice card is not an effect card fixtures skip")
	}
	top, ok := g.StackTop()
	if !ok || top.API != engine.APIPlaneswalk || top.Source != dice[0] || top.Controller != p {
		t.Fatalf("stack top = %+v, want p's Planar Dice Planeswalk", top)
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if g.ActivePlane() != deck[0] {
		t.Errorf("ActivePlane = %v, want %v", g.ActivePlane(), deck[0])
	}
}

// TestPlanarDieRollsDrawOneStreamValueEach proves each roll is one
// nextInt(6) on the game's stream, and the roller's Planar Dice card is
// made once and reused: a blank, then the planeswalker symbol.
func TestPlanarDieRollsDrawOneStreamValueEach(t *testing.T) {
	t.Parallel()

	g, p, _ := planarDieGame(t, dieBlank, dieWalk)
	faceUpPlane(g, p, plainPlane(t, "Here"))
	deck := planarDeck(g, p, plainPlane(t, "There"))
	mustRoll(t, g, p)
	if g.ActivePlane() == deck[0] {
		t.Fatal("a blank planeswalked")
	}
	mustRoll(t, g, p)
	if g.ActivePlane() != deck[0] {
		t.Errorf("second roll: ActivePlane = %v, want %v", g.ActivePlane(), deck[0])
	}
	if dice := planarDice(g, p); len(dice) != 1 {
		t.Errorf("Planar Dice cards %v, want exactly one", dice)
	}
}

// TestRollingChaosEnsuesForTheRoller proves the Chaos face runs
// Mode$ ChaosEnsues (PlanarDice.java:90-93) with the roller as
// TriggeredPlayer, alongside every "whenever you roll the planar die" line,
// and does not planeswalk.
func TestRollingChaosEnsuesForTheRoller(t *testing.T) {
	t.Parallel()

	g, p, other := planarDieGame(t, dieChaos)
	faceUpPlane(g, other, planarDef(t, "Theirs", "Plane", []string{
		chaosTrigger,
		"Mode$ PlanarDice | TriggerZones$ Command | Execute$ Rolled",
		"Mode$ PlanarDice | Result$ Blank | TriggerZones$ Command | Execute$ Rolled",
	}, nil,
		"Chaos", "DB$ LoseLife | Defined$ TriggeredPlayer | LifeAmount$ 2",
		"Rolled", "DB$ GainLife | Defined$ You | LifeAmount$ 3"))
	deck := planarDeck(g, other, plainPlane(t, "Their Next"))
	mustRoll(t, g, p)
	if got := g.Player(p).Life; got != 18 {
		t.Errorf("roller's life = %d, want 18 (TriggeredPlayer loses 2)", got)
	}
	if got := g.Player(other).Life; got != 23 {
		t.Errorf("planar controller's life = %d, want 23 (one roll trigger, not the Blank one)", got)
	}
	if g.ActivePlane() == deck[0] {
		t.Error("a chaos roll planeswalked")
	}
}

// TestRollingABlankFiresOnlyBlankAndAnyFaceLines proves Result$
// (TriggerPlanarDice.performTest): Pompeii's Result$ Blank fires on a
// blank, Result$ Chaos and Result$ Planeswalk lines do not, nor does the
// active plane's chaos ability or the Planar Dice card's own walk.
func TestRollingABlankFiresOnlyBlankAndAnyFaceLines(t *testing.T) {
	t.Parallel()

	g, p, _ := planarDieGame(t, dieBlank)
	here := faceUpPlane(g, p, planarDef(t, "Pompeii Test", "Plane", []string{
		chaosTrigger,
		"Mode$ PlanarDice | Result$ blank | TriggerZones$ Command | Execute$ One",
		"Mode$ PlanarDice | Result$ Chaos | TriggerZones$ Command | Execute$ Ten",
		"Mode$ PlanarDice | Result$ Planeswalk | TriggerZones$ Command | Execute$ Ten",
		"Mode$ PlanarDice | TriggerZones$ Command | Execute$ Hundred",
	}, nil,
		"Chaos", "DB$ GainLife | Defined$ You | LifeAmount$ 1000",
		"One", "DB$ GainLife | Defined$ You | LifeAmount$ 1",
		"Ten", "DB$ GainLife | Defined$ You | LifeAmount$ 10",
		"Hundred", "DB$ GainLife | Defined$ You | LifeAmount$ 100"))
	planarDeck(g, p, plainPlane(t, "There"))
	mustRoll(t, g, p)
	if got := g.Player(p).Life; got != 121 {
		t.Errorf("life = %d, want 121", got)
	}
	if g.ActivePlane() != here {
		t.Errorf("ActivePlane = %v, want %v", g.ActivePlane(), here)
	}
}

// TestPlanarDiceValidPlayerIsTheRoller proves ValidPlayer$ is matched
// against the roller: The Drum Mining Facility's ValidPlayer$ You does not
// fire for another player's roll; Stairs to Infinity's line, without one,
// does.
func TestPlanarDiceValidPlayerIsTheRoller(t *testing.T) {
	t.Parallel()

	g, p, other := planarDieGame(t, dieBlank)
	faceUpPlane(g, other, rollTriggerPlane(t, "Drum Test", " | ValidPlayer$ You", "1"))
	g.NewCard(rollTriggerPlane(t, "Stairs Test", "", "10"), other, engine.Command)
	g.NewCard(rollTriggerPlane(t, "Bogus Test", " | ValidPlayer$ Bogus.Spec", "100"), other, engine.Command)
	planarDeck(g, other, plainPlane(t, "Their Next"))
	mustRoll(t, g, p)
	if got := g.Player(other).Life; got != 30 {
		t.Errorf("life = %d, want 30 (Stairs only)", got)
	}
}

// TestPlanarDieTriggersStackInOneAPNAPBatch proves the PlanarDice and
// ChaosEnsues runs of one roll are ordered together, APNAP: the active
// player's chaos ability goes on the stack first and the nonactive
// player's roll trigger over it, as Java's single simultaneous batch
// (MagicStack.addAllTriggeredAbilitiesToStack), not run by run.
func TestPlanarDieTriggersStackInOneAPNAPBatch(t *testing.T) {
	t.Parallel()

	g, p, other := planarDieGame(t, dieChaos)
	faceUpPlane(g, p, gainOnChaos(t, "Mine", "1"))
	planarDeck(g, p, plainPlane(t, "My Next"))
	theirs := g.NewCard(rollTriggerPlane(t, "Their Stairs", "", "10"), other, engine.Command)
	c := engine.NewScriptedController()
	if err := rollDie(t, g, p, c, "DB$ RollPlanarDice"); err != nil {
		t.Fatalf("RollPlanarDice: %v", err)
	}
	if n := g.StackLen(); n != 2 {
		t.Fatalf("stack holds %d, want 2", n)
	}
	if top, _ := g.StackTop(); top.Source != theirs || top.Controller != other {
		t.Errorf("stack top = %+v, want the nonactive player's roll trigger", top)
	}
}

// TestStaticPlanarDiceTriggerResolvesInline proves a Static$ True
// Mode$ PlanarDice line resolves at the roll, off the stack (ADR-0020).
func TestStaticPlanarDiceTriggerResolvesInline(t *testing.T) {
	t.Parallel()

	g, p, _ := planarDieGame(t, dieChaos)
	faceUpPlane(g, p, planarDef(t, "Static Test", "Plane", []string{
		"Mode$ PlanarDice | Static$ True | TriggerZones$ Command | Execute$ Rolled",
		"Mode$ ChaosEnsues | Static$ True | TriggerZones$ Command | Execute$ Rolled",
	}, nil, "Rolled", "DB$ GainLife | Defined$ You | LifeAmount$ 2"))
	planarDeck(g, p, plainPlane(t, "There"))
	if err := rollDie(t, g, p, engine.NewScriptedController(), "DB$ RollPlanarDice"); err != nil {
		t.Fatalf("RollPlanarDice: %v", err)
	}
	if n := g.StackLen(); n != 0 {
		t.Errorf("stack holds %d, want 0", n)
	}
	if got := g.Player(p).Life; got != 24 {
		t.Errorf("life = %d, want 24", got)
	}
}

// TestPlanarDiceBadResultIsAnError proves a Result$ naming no face fails
// the roll, as PlanarDice.smartValueOf throws.
func TestPlanarDiceBadResultIsAnError(t *testing.T) {
	t.Parallel()

	g, p, _ := planarDieGame(t, dieBlank)
	faceUpPlane(g, p, rollTriggerPlane(t, "Odd Test", " | Result$ Sideways", "1"))
	planarDeck(g, p, plainPlane(t, "There"))
	err := rollDie(t, g, p, engine.NewScriptedController(), "DB$ RollPlanarDice")
	if err == nil || !strings.Contains(err.Error(), `Result$ "Sideways"`) {
		t.Errorf("err = %v, want the unknown Result$", err)
	}
}

// TestRollPlanarDiceRefusesUnportedShapes proves every shape this port
// cannot roll past fails before the die is rolled: SpecialAction$, a live
// Event$ RollPlanarDice (Ichor Elixir) or Event$ PlanarDiceResult (Chaotic
// Aether's effect card) replacement, and a live Mode$ RolledDie or RolledDieOnce
// trigger. After each refusal, with the blocker gone, the next roll lands
// on the stream's first draw.
func TestRollPlanarDiceRefusesUnportedShapes(t *testing.T) {
	t.Parallel()

	blockers := []struct {
		name   string
		zone   engine.ZoneType
		effect bool
		def    func(t *testing.T) *compile.Card
		want   string
	}{
		{"Ichor", engine.Battlefield, false, func(t *testing.T) *compile.Card {
			return planarDef(t, "Ichor Test", "Artifact", nil, []string{
				"Event$ RollPlanarDice | ActiveZones$ Battlefield | ValidPlayer$ You | ReplaceWith$ Plus",
			}, "Plus", "DB$ GainLife | Defined$ You | LifeAmount$ 1")
		}, "Event$ RollPlanarDice"},
		{"Aether", engine.Command, true, func(t *testing.T) *compile.Card {
			return planarDef(t, "Aether Test", "Phenomenon", nil, []string{
				"Event$ PlanarDiceResult | ValidRoll$ Blank | ReplaceWith$ Plus",
			}, "Plus", "DB$ GainLife | Defined$ You | LifeAmount$ 1")
		}, "Event$ PlanarDiceResult"},
		{"Rolled", engine.Battlefield, false, func(t *testing.T) *compile.Card {
			return planarDef(t, "Rolled Test", "Creature Elf", []string{
				"Mode$ RolledDie | ValidPlayer$ You | Execute$ Plus",
			}, nil, "Plus", "DB$ GainLife | Defined$ You | LifeAmount$ 1")
		}, "Mode$ RolledDie"},
		{"EffectRolled", engine.Command, true, func(t *testing.T) *compile.Card {
			return planarDef(t, "Effect Rolled Test", "Plane", []string{
				"Mode$ RolledDie | Execute$ Plus",
			}, nil, "Plus", "DB$ GainLife | Defined$ You | LifeAmount$ 1")
		}, "Mode$ RolledDie"},
		{"Once", engine.Graveyard, false, func(t *testing.T) *compile.Card {
			return planarDef(t, "Once Test", "Creature Elf", []string{
				"Mode$ RolledDieOnce | TriggerZones$ Graveyard | Execute$ Plus",
			}, nil, "Plus", "DB$ GainLife | Defined$ You | LifeAmount$ 1")
		}, "Mode$ RolledDieOnce"},
	}
	for _, b := range blockers {
		t.Run(b.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := planarDieGame(t, dieWalk)
			faceUpPlane(g, p, plainPlane(t, "Here"))
			deck := planarDeck(g, p, plainPlane(t, "There"))
			blocker := g.NewCard(b.def(t), p, b.zone)
			// Chaotic Aether's replacement lives on the effect card its
			// DB$ Effect makes, live in the Command zone as any effect's.
			g.Card(blocker).IsEffect = b.effect
			err := rollDie(t, g, p, engine.NewScriptedController(), "DB$ RollPlanarDice")
			if err == nil || !strings.Contains(err.Error(), b.want) {
				t.Fatalf("err = %v, want %s refused", err, b.want)
			}
			if n := g.StackLen(); n != 0 || len(planarDice(g, p)) != 0 {
				t.Fatalf("refused roll left stack %d, dice %v", n, planarDice(g, p))
			}
			g.Move(blocker, engine.Exile, p)
			mustRoll(t, g, p)
			if g.ActivePlane() != deck[0] {
				t.Errorf("roll after refusal did not land the stream's first draw")
			}
		})
	}

	t.Run("SpecialAction", func(t *testing.T) {
		t.Parallel()

		g, p, _ := planarDieGame(t, dieWalk)
		planarDeck(g, p, plainPlane(t, "There"))
		err := rollDie(t, g, p, engine.NewScriptedController(), "DB$ RollPlanarDice | SpecialAction$ True")
		if err == nil || !strings.Contains(err.Error(), "SpecialAction$ not resolvable yet") {
			t.Errorf("err = %v, want SpecialAction$ refused", err)
		}
	})
}

// TestChaoticAethersEffectRefusesTheRoll proves the PlanarDiceResult
// refusal reaches Chaotic Aether's real replacement, which is live only on
// the effect card its DB$ Effect makes (chaotic_aether.txt:5,7-8).
func TestChaoticAethersEffectRefusesTheRoll(t *testing.T) {
	t.Parallel()

	g, p, _ := planarDieGame(t, dieBlank)
	planarDeck(g, p, plainPlane(t, "There"))
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ Effect | ReplacementEffects$ BlankIsChaos | Duration$ Permanent",
		"BlankIsChaos", "Event$ PlanarDiceResult | ValidRoll$ Blank | ReplaceWith$ REChaos | Description$ Each blank roll of the planar die is a {CHAOS} roll until a player planeswalks away from a plane.",
		"REChaos", "DB$ ReplaceEffect | VarName$ Result | VarValue$ Chaos | VarType$ PlanarDice")
	err := rollDie(t, g, p, engine.NewScriptedController(), "DB$ RollPlanarDice")
	if err == nil || !strings.Contains(err.Error(), "Event$ PlanarDiceResult") {
		t.Errorf("err = %v, want Chaotic Aether's replacement refused", err)
	}
}

// TestRolledDieTriggersNotLiveDoNotRefuse proves the RolledDie refusal is
// for live lines only: a creature card in hand, and a battlefield line
// whose TriggerZones$ is elsewhere, cannot fire and leave the roll alone.
func TestRolledDieTriggersNotLiveDoNotRefuse(t *testing.T) {
	t.Parallel()

	g, p, _ := planarDieGame(t, dieBlank)
	planarDeck(g, p, plainPlane(t, "There"))
	g.NewCard(planarDef(t, "Held Test", "Creature Elf", []string{"Mode$ RolledDie | Execute$ Plus"}, nil,
		"Plus", "DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Hand)
	g.NewCard(planarDef(t, "Grave Test", "Creature Elf", []string{"Mode$ RolledDie | TriggerZones$ Graveyard | Execute$ Plus"}, nil,
		"Plus", "DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	mustRoll(t, g, p)
}

// TestPlanarDieReplacementOutsidePlanechaseIsInert proves the Planechase
// gate runs before the refusals: Ichor Elixir in a normal game rolls
// nothing and errors nothing.
func TestPlanarDieReplacementOutsidePlanechaseIsInert(t *testing.T) {
	t.Parallel()

	g, p, _ := planarDieGame(t, dieBlank)
	g.NewCard(planarDef(t, "Ichor Test", "Artifact", nil, []string{
		"Event$ RollPlanarDice | ActiveZones$ Battlefield | ValidPlayer$ You | ReplaceWith$ Plus",
	}, "Plus", "DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	if err := rollDie(t, g, p, engine.NewScriptedController(), "DB$ RollPlanarDice"); err != nil {
		t.Errorf("RollPlanarDice outside Planechase: %v", err)
	}
}

// TestClonePlanarDie proves Game.Clone carries the roller's Planar Dice
// card: the copy still knows it as a designation card and reuses it.
func TestClonePlanarDie(t *testing.T) {
	t.Parallel()

	g, p, _ := planarDieGame(t, dieBlank, dieBlank)
	planarDeck(g, p, plainPlane(t, "There"))
	mustRoll(t, g, p)
	dice := planarDice(g, p)
	if len(dice) != 1 {
		t.Fatalf("Planar Dice cards %v, want one", dice)
	}
	cl := g.Clone()
	if !cl.IsDesignationCard(dice[0]) {
		t.Error("clone lost the Planar Dice card")
	}
	mustRoll(t, cl, p)
	if got := planarDice(cl, p); len(got) != 1 || got[0] != dice[0] {
		t.Errorf("clone's Planar Dice cards %v, want %v", got, dice)
	}
}
