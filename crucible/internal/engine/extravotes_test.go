package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// These tests cover Layer 8's vote and villainous-choice params
// (StaticAbilityContinuous.java:536-553): AdditionalVote$,
// AdditionalOptionalVote$, ControlVote$ read by Vote (VoteEffect.java:83-104)
// and AdditionalVillainousChoice$ read by VillainousChoice
// (VillainousChoiceEffect.java:22-28), each from a real corpus static line.

// A vote both players take between DBA (the caster gains 1) and DBB (each
// opponent loses 2); with no VoteTiedAbility$, a tie resolves both.
var voteLine = []string{
	"DB$ Vote | Defined$ Player | Choices$ DBA,DBB",
	"DBA", "DB$ GainLife | Defined$ You | LifeAmount$ 1",
	"DBB", "DB$ LoseLife | Defined$ Opponent | LifeAmount$ 2",
}

// staticOn puts an enchantment carrying line onto p's battlefield and runs
// a state-based-action check so Layer 8 folds it in.
func staticOn(t *testing.T, g *engine.Game, p engine.PlayerID, line string) {
	t.Helper()
	g.NewCard(continuousDef(t, "Vote Static", line), p, engine.Battlefield)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
}

// Brago's Representative: "While voting, you get an additional vote." The
// caster's two votes for DBB outweigh the opponent's one for DBA.
func TestAdditionalVoteCastsTwoBallots(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	staticOn(t, g, p, "Mode$ Continuous | Affected$ You | AdditionalVote$ 1")
	c := engine.NewScriptedController()
	c.QueueAbilityChoice([]int{1})
	c.QueueAbilityChoice([]int{1})
	c.QueueAbilityChoice([]int{0})
	resolveLine(t, g, p, c, voteLine[0], voteLine[1:]...)

	if g.Player(p).Life != 20 || g.Player(other).Life != 18 {
		t.Errorf("life = %d/%d, want 20/18: DBB wins 2-1, DBA does not resolve", g.Player(p).Life, g.Player(other).Life)
	}
}

// Ballot Broker: "While voting, you may vote an additional time." The voter picks
// how many optional votes to use (ChooseNumber in [0, 1]).
func TestAdditionalOptionalVoteAsksHowMany(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		extra     int
		wantOther int
		wantMine  int
	}{
		{1, 18, 20}, // 2-1 for DBB
		{0, 18, 21}, // 1-1 tie: both resolve
	} {
		g, p, other := newTwoPlayerGame(t)
		staticOn(t, g, p, "Mode$ Continuous | Affected$ You | AdditionalOptionalVote$ 1")
		c := engine.NewScriptedController()
		c.QueueNumberChoice(tc.extra)
		for range 1 + tc.extra {
			c.QueueAbilityChoice([]int{1})
		}
		c.QueueAbilityChoice([]int{0})
		resolveLine(t, g, p, c, voteLine[0], voteLine[1:]...)
		if g.Player(p).Life != tc.wantMine || g.Player(other).Life != tc.wantOther {
			t.Errorf("extra %d: life = %d/%d, want %d/%d", tc.extra,
				g.Player(p).Life, g.Player(other).Life, tc.wantMine, tc.wantOther)
		}
	}
}

// deciderLog is a ScriptedController that records who is asked each
// ChooseAbilitiesForEffect question: the one decision ControlVote$ moves.
type deciderLog struct {
	*engine.ScriptedController
	asked []engine.PlayerID
}

func (d *deciderLog) ChooseAbilitiesForEffect(g *engine.Game, decider engine.PlayerID, source engine.CardID, options []string, amount int) []int {
	d.asked = append(d.asked, decider)
	return d.ScriptedController.ChooseAbilitiesForEffect(g, decider, source, options, amount)
}

// Illusion of Choice: "You choose how each player votes this turn" (ControlVote$
// on an Effect card, the one real line): the controlling player casts every
// ballot, each still counted as its own player's vote.
func TestControlVoteCastsEveryBallot(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	staticOn(t, g, other, "Mode$ Continuous | Affected$ You | ControlVote$ True")
	c := &deciderLog{ScriptedController: engine.NewScriptedController()}
	c.QueueAbilityChoice([]int{0})
	c.QueueAbilityChoice([]int{0})

	g.Player(p).ManaPool.Add(mana.Green, 1)
	host := g.NewCard(etbChainDef(t, "Test Shape", voteLine[0], voteLine[1:]...), p, engine.Hand)
	if !g.CastSpell(p, host, c) {
		t.Fatal("CastSpell failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}

	if len(c.asked) != 2 || c.asked[0] != other || c.asked[1] != other {
		t.Errorf("deciders = %v, want the ControlVote$ player (%d) twice", c.asked, other)
	}
	if got := g.Player(p).Life; got != 21 {
		t.Errorf("life = %d, want 21: two DBA votes", got)
	}
}

// VoteEffect.java:93-94 removes the controlling player, not the voter, from
// a VotePlayer$ Other ballot (a Forge bug), so the combination fails closed.
func TestControlVoteWithVotePlayerOtherFailsClosed(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	staticOn(t, g, other, "Mode$ Continuous | Affected$ You | ControlVote$ True")
	c := engine.NewScriptedController()
	_, err := castETBChain(t, g, p, etbChainDef(t, "Test Shape",
		"DB$ Vote | Defined$ Player | VotePlayer$ Other | VoteSubAbility$ DBHit",
		"DBHit", "DB$ LoseLife | Defined$ Remembered | LifeAmount$ 1"), c)
	if err == nil || !strings.Contains(err.Error(), "VoteEffect.java:93-94") {
		t.Errorf("err = %v, want the ControlVote$/VotePlayer$ Other refusal", err)
	}
}

// The Valeyard: "If an opponent would face a villainous choice, they face
// that choice an additional time."
func TestAdditionalVillainousChoiceFacesItTwice(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	staticOn(t, g, p, "Mode$ Continuous | Affected$ Opponent | AdditionalVillainousChoice$ 1")
	c := engine.NewScriptedController()
	c.QueueAbilityChoice([]int{0})
	c.QueueAbilityChoice([]int{1})
	resolveLine(t, g, p, c, "DB$ VillainousChoice | Defined$ Opponent | Choices$ DBA,DBB",
		"DBA", "DB$ LoseLife | Defined$ Remembered | LifeAmount$ 4",
		"DBB", "DB$ GainLife | Defined$ You | LifeAmount$ 9")

	if g.Player(other).Life != 16 || g.Player(p).Life != 29 {
		t.Errorf("life = %d/%d, want 29/16: both picks resolve", g.Player(p).Life, g.Player(other).Life)
	}
}
