package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// This file covers the secondary shapes of the token, Animate-shaped,
// trigger, dispatch and phase effects -- the params past each one's
// dominant line that real corpus lines still name.

func resolveLine(t *testing.T, g *engine.Game, p engine.PlayerID, c *engine.ScriptedController, trig string, svars ...string) engine.CardID {
	t.Helper()
	host, err := castETBChain(t, g, p, etbChainDef(t, "Test Shape", trig, svars...), c)
	if err != nil {
		t.Fatalf("%q: ResolveStack: %v", trig, err)
	}
	return host
}

func TestConditionPhasesAndPlayerTurn(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ GainLife | Defined$ You | LifeAmount$ 1 | ConditionPhases$ Main1->EndCombat | SubAbility$ DBA",
		"DBA", "DB$ GainLife | Defined$ You | LifeAmount$ 10 | ConditionPhases$ Upkeep,Draw | SubAbility$ DBB",
		"DBB", "DB$ GainLife | Defined$ You | LifeAmount$ 100 | ConditionPlayerTurn$ False | SubAbility$ DBC",
		"DBC", "DB$ GainLife | Defined$ You | LifeAmount$ 1000 | ConditionPlayerTurn$ True | ConditionPhases$ Main | SubAbility$ DBD",
		"DBD", "DB$ GainLife | Defined$ You | LifeAmount$ 5 | ConditionPhases$ Bogus")
	if got := g.Player(p).Life; got != 1021 {
		t.Errorf("life = %d, want 1021", got)
	}
}

func TestTokenEffectTargetedOwnerAndScripts(t *testing.T) {
	t.Parallel()

	g, p, other := newTokenGame(t)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	host := resolveLine(t, g, p, c,
		"DB$ Token | ValidTgts$ Player | TokenScript$ w_1_1_soldier,c_a_clue_draw | ImprintTokens$ True | RememberSource$ True | SubAbility$ DBAll",
		"DBAll", "DB$ Token | TokenOwner$ Player | TokenScript$ c_a_clue_draw")
	if n := len(tokensOn(g, other, "Soldier Token")); n != 1 {
		t.Errorf("other's soldiers = %d, want 1", n)
	}
	if n := len(tokensOn(g, other, "Clue Token")); n != 2 {
		t.Errorf("other's clues = %d, want 2", n)
	}
	if n := len(tokensOn(g, p, "Clue Token")); n != 1 {
		t.Errorf("p's clues = %d, want 1", n)
	}
	if n := len(g.Card(host).Memory.Imprinted()); n != 2 {
		t.Errorf("host imprinted %d, want 2", n)
	}
	soldier := tokensOn(g, other, "Soldier Token")[0]
	if r := g.Card(soldier).Memory.Remembered(); len(r) != 1 || r[0] != engine.CardEntity(host) {
		t.Errorf("soldier remembers %v, want the host", r)
	}
}

func TestInvestigateAndIncubateRepeat(t *testing.T) {
	t.Parallel()

	g, p, _ := newTokenGame(t)
	host := resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ Investigate | RememberInvestigatingPlayers$ True | SubAbility$ DBInc",
		"DBInc", "DB$ Incubate | Times$ 2")
	if n := len(tokensOn(g, p, "Incubator Token")); n != 2 {
		t.Errorf("incubators = %d, want 2", n)
	}
	if r := g.Card(host).Memory.Remembered(); len(r) != 1 || r[0] != engine.PlayerEntity(p) {
		t.Errorf("host remembers %v, want the investigator", r)
	}
}

func TestAmassChoosesAmongArmies(t *testing.T) {
	t.Parallel()

	g, p, _ := newTokenGame(t)
	c := engine.NewScriptedController()
	armies := []engine.CardID{
		g.NewCard(tokenDefT(t, "Zombie Army Token", "Creature Zombie Army", "1", "1"), p, engine.Battlefield),
		g.NewCard(tokenDefT(t, "Zombie Army Token", "Creature Zombie Army", "1", "1"), p, engine.Battlefield),
	}
	c.QueueCardChoice([]engine.CardID{armies[1]})
	host := resolveLine(t, g, p, c, "DB$ Amass | Type$ Zombie | Num$ 2 | RememberAmass$ True")
	if n := g.Card(armies[1]).Counters.Count(engine.P1P1); n != 2 {
		t.Errorf("chosen army counters = %d, want 2", n)
	}
	if r := g.Card(host).Memory.Remembered(); len(r) != 1 || r[0] != engine.CardEntity(armies[1]) {
		t.Errorf("host remembers %v, want the chosen army", r)
	}
}

func TestAnimateColorsAndRemember(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueColorChoice(mana.Blue)
	host := resolveLine(t, g, p, c,
		"DB$ ChooseColor | Defined$ You | SubAbility$ DBAnim",
		"DBAnim", "DB$ Animate | Defined$ Self | Colors$ ChosenColor | OverwriteColors$ True | RememberAnimated$ True | RemoveKeywords$ Flying | SubAbility$ DBAll",
		"DBAll", "DB$ Animate | Defined$ Self | Colors$ green,Colorless")
	hc := g.Card(host)
	if hc.Colors() != mana.Blue|mana.Green {
		t.Errorf("host colors = %v, want blue and green", hc.Colors())
	}
	if len(hc.Memory.Remembered()) != 1 {
		t.Errorf("host remembers %v, want itself", hc.Memory.Remembered())
	}
	resolveLine(t, g, p, c, "DB$ Animate | Defined$ Self | Colors$ All | Types$ Artifact | RemoveSuperTypes$ True | RemoveSubTypes$ True")
}

func TestDebuffRefusesProtectionSplit(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	def := etbChainDef(t, "Test Debuff Split", "DB$ Protection | Defined$ Self | Gains$ Choice | Choices$ CardType | SubAbility$ DBDebuff",
		"DBDebuff", "DB$ Debuff | Defined$ Self | Keywords$ Protection from red")
	c.QueueProtectionChoice(1)
	host, err := castETBChain(t, g, p, def, c)
	if err == nil {
		t.Fatalf("Debuff of Protection from red over %v succeeded, want an error", g.Card(host).KeywordLines())
	}
}

func TestProtectionChosenColorAndDurationPermanent(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueColorChoice(mana.Green)
	host := resolveLine(t, g, p, c, "DB$ ChooseColor | Defined$ You | SubAbility$ DBProt",
		"DBProt", "DB$ Protection | Defined$ Self | Gains$ ChosenColor | Duration$ Permanent")
	advanceToCleanup(g, c)
	if !strings.Contains(strings.Join(g.Card(host).KeywordLines(), "|"), "Protection from green") {
		t.Error("permanent protection from green gone after cleanup")
	}
}

func TestFlipCoinNoCallAmountAndUntilLose(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c,
		"DB$ FlipCoin | NoCall$ True | Amount$ 3 | HeadsSubAbility$ DBHeads | TailsSubAbility$ DBTails",
		"DBHeads", "DB$ GainLife | Defined$ You | LifeAmount$ X",
		"DBTails", "DB$ LoseLife | Defined$ You | LifeAmount$ X")
	r := javarand.New(1)
	heads := 0
	for i := 0; i < 3; i++ {
		if r.Bool() {
			heads++
		}
	}
	if got, want := g.Player(p).Life, 20+heads-(3-heads); got != want {
		t.Errorf("life = %d, want %d (%d heads)", got, want, heads)
	}

	g2, p2, _ := newTwoPlayerGame(t)
	c2 := engine.NewScriptedController()
	for i := 0; i < 20; i++ {
		c2.QueueCoinCall(true)
	}
	host := resolveLine(t, g2, p2, c2,
		"DB$ FlipCoin | FlipUntilYouLose$ True | RememberWinner$ True | RememberLoser$ True | WinSubAbility$ DBWin",
		"DBWin", "DB$ GainLife | Defined$ You | LifeAmount$ Wins")
	r2 := javarand.New(1)
	wins := 0
	for r2.Bool() {
		wins++
	}
	if got := g2.Player(p2).Life; got != 20+wins {
		t.Errorf("life = %d, want %d (%d wins)", got, 20+wins, wins)
	}
	if len(g2.Card(host).Memory.Remembered()) == 0 {
		t.Error("host remembers nobody")
	}
}

func TestFlipCoinRefusesCoinModifier(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	thumb := &compile.Card{Name: "Test Thumb"}
	thumb.Faces[0].Statics = []*compile.Ability{{Name: "FlipCoinDoubler"}}
	g.NewCard(thumb, p, engine.Battlefield)
	_, err := castETBChain(t, g, p, etbChainDef(t, "Test Coin", "DB$ FlipCoin | NoCall$ True"), engine.NewScriptedController())
	if err == nil {
		t.Error("flip with a FlipCoinDoubler in play succeeded, want an error")
	}
}

func TestRollDiceSecondaryShapes(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	host := resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ RollDice | Amount$ 3 | Sides$ 4 | IgnoreLower$ 1 | Modifier$ 1 | EvenOddResults$ True | DifferentResults$ True | MaxRollsResults$ True | NoteDoubles$ True | SubsForEach$ True | RememberHighestPlayer$ True | ResultSubAbilities$ 99:DBNever | Else$ DBElse | SubAbility$ DBCount",
		"DBNever", "DB$ LoseLife | Defined$ You | LifeAmount$ 50",
		"DBElse", "DB$ GainLife | Defined$ You | LifeAmount$ 1",
		"DBCount", "DB$ GainLife | Defined$ You | LifeAmount$ EvenResults")
	r := javarand.New(1)
	var rolls []int
	for i := 0; i < 3; i++ {
		rolls = append(rolls, int(r.Int32n(4))+1)
	}
	lo := 0
	for i, v := range rolls {
		if v < rolls[lo] {
			lo = i
		}
	}
	even := 0
	for i, v := range rolls {
		if i != lo && (v+1)%2 == 0 {
			even++
		}
	}
	if got, want := g.Player(p).Life, 20+2+even; got != want {
		t.Errorf("life = %d, want %d (rolls %v)", got, want, rolls)
	}
	if len(g.Card(host).Memory.Remembered()) != 1 {
		t.Error("host does not remember the highest roller")
	}

	g2, p2, _ := newTwoPlayerGame(t)
	g2.NewCard(creatureDefPTKeywords(t, "1", "1", "After you roll a die, you may pay 1 life. If you do, increase or decrease the result by 1. Do this only once each turn."), p2, engine.Battlefield)
	if _, err := castETBChain(t, g2, p2, etbChainDef(t, "Test Die", "DB$ RollDice | Sides$ 6 | UseDifferenceBetweenRolls$ True"), engine.NewScriptedController()); err == nil {
		t.Error("roll with an increment card in play succeeded, want an error")
	}
}

func TestClashTieAndDefinedOpponent(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	host := resolveLine(t, g, p, c,
		"DB$ Clash | Defined$ Opponent | RememberClasher$ True | WinSubAbility$ DBWin | OtherwiseSubAbility$ DBLose",
		"DBWin", "DB$ GainLife | Defined$ You | LifeAmount$ 4",
		"DBLose", "DB$ LoseLife | Defined$ You | LifeAmount$ 4")
	if got := g.Player(p).Life; got != 16 {
		t.Errorf("life = %d, want 16 (empty libraries, no winner)", got)
	}
	if r := g.Card(host).Memory.Remembered(); len(r) != 1 || r[0] != engine.PlayerEntity(other) {
		t.Errorf("host remembers %v, want the opponent", r)
	}
}

func TestBalanceHandsDiscard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Hand)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueDiscardChoice([]engine.CardID{a})
	g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Hand)
	resolveLine(t, g, p, c, "DB$ Balance | Zone$ Hand")
	if z := g.Card(a).Zone; z != engine.Graveyard {
		t.Errorf("discarded card zone = %v, want Graveyard", z)
	}
}

func TestStoreSVarShapes(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ StoreSVar | SVar$ Y | Type$ Calculate | Expression$ 3 | SubAbility$ DBAdd",
		"DBAdd", "DB$ StoreSVar | SVar$ Y | Type$ AdditiveForEach | Expression$ 2 | SubAbility$ DBTwice",
		"DBTwice", "DB$ StoreSVar | SVar$ Z | Type$ CountSVar | Expression$ Y/Twice | SubAbility$ DBMinus",
		"DBMinus", "DB$ StoreSVar | SVar$ Z | Type$ CountSVar | Expression$ Z/Minus.3 | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ Z")
	if got := g.Player(p).Life; got != 27 {
		t.Errorf("life = %d, want 27", got)
	}
}

func TestSeekTypesAndImprint(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	land := g.NewCard(landDef(t, "Test Forest", "Land Forest"), p, engine.Library)
	creature := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Library)
	host := resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Seek | Types$ Creature,Land | ImprintFound$ True")
	if g.Card(land).Zone != engine.Hand || g.Card(creature).Zone != engine.Hand {
		t.Error("seek by two types did not find both cards")
	}
	if n := len(g.Card(host).Memory.Imprinted()); n != 2 {
		t.Errorf("host imprinted %d, want 2", n)
	}
}

func TestCharmRandomAndMinimum(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ Charm | Random$ True | Choices$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 2")
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22", got)
	}

	g2, p2, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueModeChoice([]int{0, 0})
	def := etbChainDef(t, "Test Bad Charm", "DB$ Charm | CharmNum$ 2 | MinCharmNum$ 1 | Choices$ DBGain,DBLose",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 2",
		"DBLose", "DB$ LoseLife | Defined$ You | LifeAmount$ 2")
	if _, err := castETBChain(t, g2, p2, def, c); err == nil {
		t.Error("a repeated mode was accepted, want an error")
	}
}

func TestDelayedTriggerForPlayer(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	libraryCards(t, g, p, 5)
	libraryCards(t, g, other, 5)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c,
		"DB$ DelayedTrigger | Mode$ Phase | Phase$ Upkeep | DelayedTriggerDefinedPlayer$ Opponent | RememberObjects$ Opponent | Execute$ TrigLose",
		"TrigLose", "DB$ LoseLife | Defined$ DelayTriggerRemembered | LifeAmount$ 2")
	advanceUntil(t, g, c, func() bool { return g.ActivePlayer() == other && g.ActivePhase() == engine.Draw })
	if got := g.Player(other).Life; got != 18 {
		t.Errorf("other life = %d, want 18", got)
	}
}

func TestSkipPhaseCombatEachTurn(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c, "DB$ SkipPhase | Defined$ You | Phase$ BeginCombat | Duration$ EndOfTurn | SubAbility$ DBNext",
		"DBNext", "DB$ SkipPhase | Defined$ You | Step$ Upkeep | Duration$ NextThisTurn")
	g.AdvancePhase(c)
	if got := g.ActivePhase(); got != engine.Main2 {
		t.Errorf("phase after Main1 = %v, want Main2 (combat skipped)", got)
	}
}

// TestDefinedPlayersTrailingFilterOnRegisteredAndRemembered proves the
// trailing valid filter on a list base (AbilityUtils.java:1192-1198):
// Registered.Other (Ludevic) is every player but the controller, and
// RememberedOwner.Opponent (The Second Doctor) the owners of remembered cards
// who are opponents.
func TestDefinedPlayersTrailingFilterOnRegisteredAndRemembered(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Library)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ GainLife | Defined$ Registered.Other | LifeAmount$ 3 | SubAbility$ DBDraw",
		"DBDraw", "DB$ Draw | Defined$ Opponent | RememberDrawn$ True | SubAbility$ DBOwner",
		"DBOwner", "DB$ GainLife | Defined$ RememberedOwner.Opponent | LifeAmount$ 5")
	if got := g.Player(other).Life; got != 20+3+5 {
		t.Errorf("opponent life = %d, want 28 (Registered.Other, then RememberedOwner.Opponent)", got)
	}
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("controller life = %d, want 20", got)
	}
}
