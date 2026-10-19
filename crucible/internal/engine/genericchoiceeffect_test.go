package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestGenericChoiceEffectResolvesChosenMode proves the chooser's pick
// among Choices$ is the one that resolves.
func TestGenericChoiceEffectResolvesChosenMode(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueAbilityChoice([]int{1})
	def := etbChainDef(t, "Test Choice", "DB$ GenericChoice | Defined$ You | Choices$ DBA,DBB",
		"DBA", "DB$ GainLife | Defined$ You | LifeAmount$ 1",
		"DBB", "DB$ GainLife | Defined$ You | LifeAmount$ 5")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 25 {
		t.Errorf("life = %d, want 25", got)
	}
}

// TestGenericChoiceEffectEachPlayerTempRemember proves each Defined$ player
// chooses, and TempRemember$ makes that chooser the Remembered player the
// chosen mode reads.
func TestGenericChoiceEffectEachPlayerTempRemember(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueAbilityChoice([]int{0})
	c.QueueAbilityChoice([]int{1})
	def := etbChainDef(t, "Test Choice Each", "DB$ GenericChoice | Defined$ Player | TempRemember$ Chooser | Choices$ DBA,DBB",
		"DBA", "DB$ LoseLife | Defined$ Remembered | LifeAmount$ 1",
		"DBB", "DB$ LoseLife | Defined$ Remembered | LifeAmount$ 4")
	host, err := castETBChain(t, g, p, def, c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 19 {
		t.Errorf("p life = %d, want 19", got)
	}
	if got := g.Player(other).Life; got != 16 {
		t.Errorf("other life = %d, want 16", got)
	}
	if n := len(g.Card(host).Memory.Remembered()); n != 0 {
		t.Errorf("remembered after = %d, want 0", n)
	}
}

// TestGenericChoiceEffectRejectsBadShapes proves AtRandom$ Urza, an
// out-of-range answer, and a choice whose UnlessCost$ is past the cost reader fail closed.
func TestGenericChoiceEffectRejectsBadShapes(t *testing.T) {
	t.Parallel()

	cases := []struct {
		line string
		pick []int
	}{
		{"DB$ GenericChoice | Defined$ You | AtRandom$ Urza | Choices$ DBA,DBB", nil},
		{"DB$ GenericChoice | Defined$ You | Choices$ DBA,DBB", []int{2}},
		{"DB$ GenericChoice | Defined$ You | Choices$ DBA,DBC", nil},
	}
	for _, tc := range cases {
		g, p, _ := newTwoPlayerGame(t)
		c := engine.NewScriptedController()
		if tc.pick != nil {
			c.QueueAbilityChoice(tc.pick)
		}
		def := etbChainDef(t, "Test Choice Bad", tc.line,
			"DBA", "DB$ GainLife | Defined$ You | LifeAmount$ 1",
			"DBB", "DB$ GainLife | Defined$ You | LifeAmount$ 5",
			"DBC", "DB$ GainLife | Defined$ You | LifeAmount$ 5 | UnlessCost$ T")
		if _, err := castETBChain(t, g, p, def, c); err == nil {
			t.Errorf("%q: ResolveStack succeeded, want an error", tc.line)
		}
	}
}

// TestGenericChoiceEffectDropsUnpayableUnlessChoices proves a choice whose
// UnlessCost$ the chooser cannot pay is not offered (ChooseGenericEffect.java:
// 69-80), so the only payable one resolves, and with none payable the
// FallbackAbility$ resolves (:128-137); a mana part is never filtered.
func TestGenericChoiceEffectDropsUnpayableUnlessChoices(t *testing.T) {
	t.Parallel()

	cases := []struct {
		name string
		line string
		a, b string
		life int
	}{
		{"only payable is offered", "DB$ GenericChoice | Defined$ You | Choices$ DBA,DBB",
			"DB$ GainLife | Defined$ You | LifeAmount$ 7 | UnlessCost$ Discard<1/Card> | UnlessPayer$ You | UnlessSwitched$ True",
			"DB$ GainLife | Defined$ You | LifeAmount$ 5", 25},
		{"none payable falls back", "DB$ GenericChoice | Defined$ You | Choices$ DBA,DBB | FallbackAbility$ DBF",
			"DB$ GainLife | Defined$ You | LifeAmount$ 7 | UnlessCost$ Discard<1/Card> | UnlessPayer$ You | UnlessSwitched$ True",
			"DB$ GainLife | Defined$ You | LifeAmount$ 5 | UnlessCost$ Sac<1/Artifact> | UnlessPayer$ You | UnlessSwitched$ True", 23},
		{"none payable, no fallback", "DB$ GenericChoice | Defined$ You | Choices$ DBA,DBB",
			"DB$ GainLife | Defined$ You | LifeAmount$ 7 | UnlessCost$ Discard<1/Card> | UnlessPayer$ You | UnlessSwitched$ True",
			"DB$ GainLife | Defined$ You | LifeAmount$ 5 | UnlessCost$ Sac<1/Artifact> | UnlessPayer$ You | UnlessSwitched$ True", 20},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			c := engine.NewScriptedController()
			c.QueueAbilityChoice([]int{0})
			def := etbChainDef(t, "Test Choice Unless", tc.line,
				"DBA", tc.a, "DBB", tc.b, "DBF", "DB$ GainLife | Defined$ You | LifeAmount$ 3")
			if _, err := castETBChain(t, g, p, def, c); err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			if got := g.Player(p).Life; got != tc.life {
				t.Errorf("life = %d, want %d", got, tc.life)
			}
		})
	}
}
