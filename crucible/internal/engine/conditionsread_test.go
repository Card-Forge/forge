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

// The trigger and sub-ability conditions this port reads instead of skipping:
// the cast's mana (Adamant$, ManaSpent$, ManaNotSpent$ and their Condition
// forms), CheckDefinedPlayer$, the Werewolf conditions, LifeTotal$ against
// the opponents, the SpellAbilityCondition life, player, colour and second
// presence checks -- and the ones it still cannot read, which now fail the
// game instead of reading as unmet (GO-7).

// costedETBDef is a 1/1 creature costing cost whose "when CARDNAME enters"
// trigger carries the extra trigger params and gains its controller 1 life.
func costedETBDef(t *testing.T, cost, params string) *compile.Card {
	t.Helper()
	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: "Test Costed"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Test Costed"
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "1", "1"
	raw.Faces[0].ManaCost = mana.MustParse(cost)
	raw.Faces[0].Triggers = []string{
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | " + params + " | Execute$ Trig",
	}
	raw.Faces[0].SVars.Set("Trig", "DB$ GainLife | Defined$ You | LifeAmount$ 1")
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}
	return c
}

// castCosted casts costedETBDef paying pool and answers the life gained and
// the error the cast's resolution, or a pending one, reported.
func castCosted(t *testing.T, g *engine.Game, p engine.PlayerID, cost, params string, pool ...mana.Colors) (int, error) {
	t.Helper()
	for _, c := range pool {
		g.Player(p).ManaPool.Add(c, 1)
	}
	card := g.NewCard(costedETBDef(t, cost, params), p, engine.Hand)
	if !g.CastSpell(p, card, engine.NewScriptedController()) {
		t.Fatalf("CastSpell %s failed", cost)
	}
	before := g.Player(p).Life
	err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController())
	if err == nil {
		err = g.TakePendingError()
	}
	return g.Player(p).Life - before, err
}

// CardTraitBase.java:355-375, :492-507: the mana the host's cast spent.
func TestTriggerReadsTheCastsMana(t *testing.T) {
	t.Parallel()

	g3 := []mana.Colors{mana.Green, mana.Green, mana.Green}
	for _, tc := range []struct {
		params, cost string
		pool         []mana.Colors
		want         int
	}{
		{"ManaSpent$ G", "G", []mana.Colors{mana.Green}, 1},
		{"ManaSpent$ R", "G", []mana.Colors{mana.Green}, 0},
		{"ManaNotSpent$ G", "G", []mana.Colors{mana.Green}, 0},
		{"ManaNotSpent$ R", "G", []mana.Colors{mana.Green}, 1},
		{"Adamant$ Green", "G G G", g3, 1},
		{"Adamant$ Green", "G", []mana.Colors{mana.Green}, 0},
		{"Adamant$ Any", "G G G", g3, 1},
		{"Adamant$ Any", "G", []mana.Colors{mana.Green}, 0},
	} {
		t.Run(tc.params+" paying "+tc.cost, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			got, err := castCosted(t, g, p, tc.cost, tc.params, tc.pool...)
			if err != nil || got != tc.want {
				t.Errorf("gained %d, err %v, want %d", got, err, tc.want)
			}
		})
	}
	for _, params := range []string{"ManaSpent$ Purple", "Adamant$ Purple"} {
		g, p, _ := newTwoPlayerGame(t)
		if _, err := castCosted(t, g, p, "G", params, mana.Green); err == nil || !strings.Contains(err.Error(), "not resolvable") {
			t.Errorf("%s: err = %v, want not resolvable", params, err)
		}
	}
}

// CheckDefinedPlayer$/DefinedPlayerCompare$ (CardTraitBase.java:458-468), and
// the isMonarch/hasInitiative player properties it is written with.
func TestTriggerCheckDefinedPlayer(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		params string
		setup  func(g *engine.Game, p, other engine.PlayerID)
		want   int
	}{
		{"CheckDefinedPlayer$ You.isMonarch", func(g *engine.Game, p, _ engine.PlayerID) { g.SetMonarch(p) }, 1},
		{"CheckDefinedPlayer$ You.isMonarch", nil, 0},
		{"CheckDefinedPlayer$ You.isMonarch | DefinedPlayerCompare$ EQ0", nil, 1},
		{"CheckDefinedPlayer$ Opponent.hasInitiative", func(g *engine.Game, _, other engine.PlayerID) { g.SetInitiative(other) }, 1},
		{"CheckDefinedPlayer$ You.hasInitiative", func(g *engine.Game, _, other engine.PlayerID) { g.SetInitiative(other) }, 0},
	} {
		g, p, other := newTwoPlayerGame(t)
		if tc.setup != nil {
			tc.setup(g, p, other)
		}
		if got, err := castCosted(t, g, p, "G", tc.params, mana.Green); err != nil || got != tc.want {
			t.Errorf("%s: gained %d, err %v, want %d", tc.params, got, err, tc.want)
		}
	}
	for _, tc := range []struct{ params, want string }{
		{"CheckDefinedPlayer$ You.bogusProperty", "CheckDefinedPlayer$"},
		{"CheckDefinedPlayer$ You | DefinedPlayerCompare$ GE", "not a comparison"},
		{"CheckDefinedPlayer$ You | DefinedPlayerCompare$ GEZ", "not resolvable"},
	} {
		g, p, _ := newTwoPlayerGame(t)
		if _, err := castCosted(t, g, p, "G", tc.params, mana.Green); err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want one containing %q", tc.params, err, tc.want)
		}
	}
}

// LifeTotal$ OpponentSmallest/OpponentGreatest (CardTraitBase.java:385-390),
// EnduringStory$ and DayTime$; ClassLevel$ and an unreadable LifeTotal$ fail
// the game.
func TestTriggerCommonRequirementsRead(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		params string
		want   int
	}{
		{"LifeTotal$ OpponentSmallest | LifeAmount$ LE15", 1},
		{"LifeTotal$ OpponentGreatest | LifeAmount$ GE21", 0},
		{"EnduringStory$ True", 0},
		{"EnduringStory$ False", 1},
		{"DayTime$ Wibble", 1},
		{"DayTime$ night", 0},
	} {
		g, p, other := newTwoPlayerGame(t)
		g.Player(other).Life = 10
		if got, err := castCosted(t, g, p, "G", tc.params, mana.Green); err != nil || got != tc.want {
			t.Errorf("%s: gained %d, err %v, want %d", tc.params, got, err, tc.want)
		}
	}
	for _, tc := range []struct{ params, want string }{
		{"LifeTotal$ Bogus", "LifeTotal$"},
		{"LifeTotal$ You | LifeAmount$ GE", "not a comparison"},
		{"LifeTotal$ You | LifeAmount$ GEZ", "not resolvable"},
		{"ClassLevel$ 2", "ClassLevel$"},
		{"IsPresent$ Card | PresentZone$ Nowhere", "zone"},
	} {
		g, p, _ := newTwoPlayerGame(t)
		if _, err := castCosted(t, g, p, "G", tc.params, mana.Green); err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want one containing %q", tc.params, err, tc.want)
		}
	}
}

// WerewolfTransformCondition$ and WerewolfUntransformCondition$
// (CardTraitBase.java:509-520) read the spells cast last turn: none for a
// transform, two or more by one player for an untransform.
func TestWerewolfConditionsReadLastTurnsSpells(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, cond string
		casts      int
		want       int
	}{
		{"transform after a quiet turn", "WerewolfTransformCondition$ True", 0, 1},
		{"no transform after a spell", "WerewolfTransformCondition$ True", 1, 0},
		{"no untransform after one spell", "WerewolfUntransformCondition$ True", 1, 0},
		{"untransform after two spells", "WerewolfUntransformCondition$ True", 2, 1},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			g.NewCard(watcherDef(t, "Wolf Watcher", "Mode$ Phase | Phase$ Upkeep | "+tc.cond+" | TriggerZones$ Battlefield",
				"DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
			for range tc.casts {
				if _, err := castETBChain(t, g, p, etbChainDef(t, "Cast Filler", "DB$ Cleanup"), engine.NewScriptedController()); err != nil {
					t.Fatalf("cast: %v", err)
				}
			}
			advanceToTurn(t, g, 2)
			before := g.Player(p).Life
			g.AdvancePhase(engine.NewScriptedController())
			if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Player(p).Life - before; got != tc.want {
				t.Errorf("gained %d at turn 2's upkeep, want %d", got, tc.want)
			}
		})
	}
}

// subGained resolves line off a cast host (castETBChain: it was cast paying
// one G) and answers the life gained and any error.
func subGained(t *testing.T, g *engine.Game, p engine.PlayerID, line string, svars ...string) (int, error) {
	t.Helper()
	before := g.Player(p).Life
	_, err := castETBChain(t, g, p, etbChainDef(t, "Test Condition", line, svars...), engine.NewScriptedController())
	if err == nil {
		err = g.TakePendingError()
	}
	return g.Player(p).Life - before, err
}

// SpellAbilityCondition's ConditionManaSpent$/NotSpent$, ConditionLifeTotal$,
// ConditionPlayerContains$, ConditionPresent2$, ConditionChosenColor$,
// ConditionNoDifferentColors$ and OrOtherConditionSVarCompare$
// (SpellAbilityCondition.java:286-298, :342-346, :376-429, :476-507).
func TestSubAbilityConditionsRead(t *testing.T) {
	t.Parallel()

	const gain = "DB$ GainLife | Defined$ You | LifeAmount$ 1 | "
	oppCreature := func(g *engine.Game, _, other engine.PlayerID) {
		addN(t, g, other, engine.Battlefield, 1, "Creature Elf")
	}
	for _, tc := range []struct {
		cond  string
		svars []string
		setup func(g *engine.Game, p, other engine.PlayerID)
		want  int
	}{
		{"ConditionManaSpent$ G", nil, nil, 1},
		{"ConditionManaSpent$ G U", nil, nil, 0},
		{"ConditionManaNotSpent$ U", nil, nil, 1},
		{"ConditionManaNotSpent$ Green", nil, nil, 0},
		{"ConditionLifeTotal$ You | ConditionLifeAmount$ GE20", nil, nil, 1},
		{"ConditionLifeTotal$ You | ConditionLifeAmount$ LT20", nil, nil, 0},
		{"ConditionLifeTotal$ Opponent | ConditionLifeAmount$ LTX", []string{"X", "Number$30"}, nil, 1},
		{"ConditionLifeTotal$ You", nil, nil, 1},
		{"ConditionPlayerDefined$ You | ConditionPlayerContains$ You", nil, nil, 1},
		{"ConditionPlayerDefined$ You | ConditionPlayerContains$ Opponent", nil, nil, 0},
		{"ConditionPlayerContains$ You", nil, nil, 0},
		{"ConditionPlayerDefined$ You", nil, nil, 1},
		{"ConditionPresent$ Creature.YouCtrl | ConditionPresent2$ Creature.OppCtrl", nil, nil, 0},
		{"ConditionPresent$ Creature.YouCtrl | ConditionPresent2$ Creature.OppCtrl", nil, oppCreature, 1},
		{"ConditionChosenColor$ white", nil, nil, 0},
		{"ConditionNoDifferentColors$ Self", nil, nil, 1},
		{"ConditionCheckSVar$ X | ConditionSVarCompare$ EQ1 | OrOtherConditionSVarCompare$ GE4", []string{"X", "Number$5"}, nil, 1},
		{"ConditionCheckSVar$ X | ConditionSVarCompare$ EQ1 | OrOtherConditionSVarCompare$ GE4", []string{"X", "Number$2"}, nil, 0},
		{"ConditionCheckSVar$ X | ConditionSVarCompare$ EQ1 | OrOtherConditionSVarCompare$ GE4", []string{"X", "Number$1"}, nil, 1},
		{"ConditionCheckSVar$ X | ConditionSVarCompare$ EQ1 | OrConditionCheckSVar$ Y | OrOtherConditionSVarCompare$ GE4",
			[]string{"X", "Number$2", "Y", "Number$9"}, nil, 1},
	} {
		t.Run(tc.cond, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			if tc.setup != nil {
				tc.setup(g, p, other)
			}
			if got, err := subGained(t, g, p, gain+tc.cond, tc.svars...); err != nil || got != tc.want {
				t.Errorf("gained %d, err %v, want %d", got, err, tc.want)
			}
		})
	}
}

// A sub-ability condition this port cannot read fails the game (GO-7):
// subAbilityUnresolvedParams, an unreadable value of a read one, and a
// presence check whose compare or zone does not parse.
func TestSubAbilityConditionsFailLoudly(t *testing.T) {
	t.Parallel()

	const gain = "DB$ GainLife | Defined$ You | LifeAmount$ 1 | "
	for _, tc := range []struct {
		cond  string
		svars []string
		want  string
	}{
		{"ConditionSorcerySpeed$ True", nil, "ConditionSorcerySpeed$"},
		{"ConditionTargetsSingleTarget$ True", nil, "ConditionTargetsSingleTarget$"},
		{"ConditionManaSpent$ Purple", nil, "not resolvable"},
		{"ConditionLifeTotal$ Player.bogusProperty", nil, "names no player"},
		{"ConditionLifeTotal$ You | ConditionLifeAmount$ GEY", nil, "not resolvable"},
		{"ConditionLifeTotal$ You | ConditionLifeAmount$ GE", nil, "not a comparison"},
		{"ConditionPlayerContains$ Player.bogusProperty", nil, "ConditionPlayerContains$"},
		{"ConditionPlayerDefined$ Player.bogusProperty | ConditionPlayerContains$ You", nil, "ConditionPlayerDefined$"},
		{"ConditionChosenColor$ purple", nil, "ConditionChosenColor$"},
		{"ConditionNoDifferentColors$ Bogus", nil, "ConditionNoDifferentColors$"},
		{"ConditionCheckSVar$ X | ConditionSVarCompare$ EQ1 | OrOtherConditionSVarCompare$ GEZ", []string{"X", "Number$2"}, "not resolvable"},
		{"ConditionCheckSVar$ X | ConditionSVarCompare$ EQ1 | OrConditionCheckSVar$ Missing | OrOtherConditionSVarCompare$ GE4", []string{"X", "Number$2"}, "not resolvable"},
		{"ConditionCheckSVar$ X | ConditionSVarCompare$ EQ1 | OrOtherConditionSVarCompare$ GE", []string{"X", "Number$2"}, "not a comparison"},
		{"ConditionPresent$ Card | ConditionCompare$ GE", nil, "not a comparison"},
		{"ConditionPresent$ Card | ConditionCompare$ GEZ", nil, "not resolvable"},
	} {
		t.Run(tc.cond, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			got, err := subGained(t, g, p, gain+tc.cond, tc.svars...)
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Errorf("err = %v, want one containing %q", err, tc.want)
			}
			if got != 0 {
				t.Errorf("gained %d, want 0 (the line did not run)", got)
			}
		})
	}
}

// Defined$ Registered (every seated player, those who left included),
// ActivePlayer and DefendingPlayer (AbilityUtils.java:1110-1111, :1129-1130,
// :1171-1172).
func TestDefinedRegisteredActiveAndDefendingPlayers(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.Player(other).Lost = true
	if got, err := gainedNow(t, g, p, "PlayerCountDefinedRegistered$Amount"); err != nil || got != 2 {
		t.Errorf("Registered: gained %d, err %v, want 2", got, err)
	}

	g, p, _ = newTwoPlayerGame(t)
	g.Player(p).Life = 7
	if got, err := gainedNow(t, g, p, "PlayerCountDefinedActivePlayer$LifeTotal"); err != nil || got != 7 {
		t.Errorf("ActivePlayer: gained %d, err %v, want 7", got, err)
	}
	if _, err := gainedNow(t, g, p, "PlayerCountDefinedDefendingPlayer$Amount"); err == nil || !strings.Contains(err.Error(), "not resolvable") {
		t.Errorf("DefendingPlayer outside combat: err = %v, want not resolvable", err)
	}

	g, p, other, attacker := attackingCombat(t)
	if got, err := gainedNow(t, g, p, "PlayerCountDefinedDefendingPlayer$Amount"); err != nil || got != 0 {
		t.Errorf("DefendingPlayer of a host not attacking: gained %d, err %v, want 0", got, err)
	}
	def := etbChainDef(t, "Defending Reader", "DB$ GainLife | Defined$ You | LifeAmount$ X", "X", "PlayerCountDefinedDefendingPlayer$Amount")
	exec := def.Faces[0].Triggers[0].Subs[0].Ability
	g.PushAbility(engine.Ability{API: engine.APIGainLife, Source: attacker, Controller: other, Params: exec, Amounts: def.Faces[0].Amounts})
	before := g.Player(other).Life
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life - before; got != 1 {
		t.Errorf("DefendingPlayer of the attacker: gained %d, want 1", got)
	}
}

// The ControlledBy property reads a spec Player.isValid cannot read as a
// Defined$ list (CardProperty.java:310-317): Zara, Renegade Recruiter's
// "Planeswalker.ControlledBy DefendingPlayer", here over the attacker's own
// defending player.
func TestControlledByReadsADefinedPlayer(t *testing.T) {
	t.Parallel()

	g, p, other, attacker := attackingCombat(t)
	addN(t, g, p, engine.Battlefield, 2, "Creature Elf")
	for _, tc := range []struct {
		spec string
		want int
	}{
		{"Count$Valid Creature.ControlledBy DefendingPlayer", 2},
		{"Count$Valid Creature.ControlledBy Bogus", 0},
	} {
		def := etbChainDef(t, "ControlledBy Reader", "DB$ GainLife | Defined$ You | LifeAmount$ X", "X", tc.spec)
		exec := def.Faces[0].Triggers[0].Subs[0].Ability
		g.PushAbility(engine.Ability{API: engine.APIGainLife, Source: attacker, Controller: other, Params: exec, Amounts: def.Faces[0].Amounts})
		before := g.Player(other).Life
		if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
			t.Fatalf("%s: ResolveStack: %v", tc.spec, err)
		}
		if got := g.Player(other).Life - before; got != tc.want {
			t.Errorf("%s: gained %d, want %d", tc.spec, got, tc.want)
		}
	}
}

// Attacking$ TriggeredDefender off an ability no Attacks trigger made is an
// error, never an empty answer (GO-7).
func TestTriggeredDefenderNeedsAnAttacksTrigger(t *testing.T) {
	t.Parallel()

	g, _, other, _ := attackingCombat(t)
	angel := g.NewCard(creatureDefPT(t, "4", "4"), other, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{angel})
	_, err := resolveNow(t, g, other, c, nil,
		"DB$ ChangeZone | Origin$ Hand | Destination$ Battlefield | ChangeType$ Creature | Attacking$ TriggeredDefender")
	if err == nil {
		err = g.TakePendingError()
	}
	if err == nil || !strings.Contains(err.Error(), "recorded no attacker") {
		t.Errorf("err = %v, want the trigger recorded no attacker", err)
	}
}
