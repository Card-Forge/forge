package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TokenAttacking$ is SpellAbilityEffect.addToCombat's attacking half
// (SpellAbilityEffect.java:758-793), run per token (TokenEffectBase.java:188):
// during combat, a creature token the attacking player controls attacks the
// defender the activator picks among "True" (every defender) or the Defined$
// entities named.

// pushTokenLine resolves line as p's ability in phase.
func pushTokenLine(t *testing.T, g *engine.Game, p engine.PlayerID, phase engine.PhaseType, c *engine.ScriptedController, line string) (engine.CardID, error) {
	t.Helper()
	g.SetTurnState(1, p, phase)
	host := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	def := etbChainDef(t, "Token Maker", line)
	g.PushAbility(engine.Ability{API: engine.APIToken, Source: host, Controller: p, Params: def.Faces[0].Triggers[0].Subs[0].Ability})
	return host, g.ResolveStack(engine.NewRegistry(), c)
}

func TestTokenAttackingJoinsCombat(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		name, value string
	}{{"every defender", "True"}, {"a defined defender", "Opponent"}} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
			c := engine.NewScriptedController()
			c.QueueAttackTarget(engine.PlayerEntity(other))
			host, err := pushTokenLine(t, g, p, engine.DeclareAttackers, c,
				"DB$ Token | TokenScript$ w_1_1_soldier | TokenTapped$ True | RememberTokens$ True | TokenAttacking$ "+tc.value)
			if err != nil {
				t.Fatalf("ResolveStack: %v", err)
			}
			made := g.Card(host).Memory.Remembered()
			if len(made) != 1 {
				t.Fatalf("made %d tokens, want 1", len(made))
			}
			tok, _ := made[0].AsCard()
			if !slices.Contains(g.Attackers(), tok) {
				t.Fatalf("the token is not attacking; attackers %v", g.Attackers())
			}
			if got := g.AttackTarget(tok); got != engine.PlayerEntity(other) {
				t.Errorf("the token attacks %v, want the opponent", got)
			}
		})
	}
}

// Outside combat the token just enters (SpellAbilityEffect.java:761).
func TestTokenAttackingOutsideCombatOnlyEnters(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	host, err := pushTokenLine(t, g, p, engine.Main1, engine.NewScriptedController(),
		"DB$ Token | TokenScript$ w_1_1_soldier | RememberTokens$ True | TokenAttacking$ True")
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if len(g.Card(host).Memory.Remembered()) != 1 || len(g.Attackers()) != 0 {
		t.Errorf("remembered %v, attackers %v: want one token, no attacker", g.Card(host).Memory.Remembered(), g.Attackers())
	}
}

func TestTokenAttackingUnreadableShapesFail(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct{ line, want string }{
		{"DB$ Token | TokenScript$ w_1_1_soldier | TokenAttacking$ Bogus", "TokenAttacking$"},
		{"DB$ Token | TokenScript$ w_1_1_soldier | TokenAttacking$ Opponent | ForEach$ Player", "ForEach$"},
	} {
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		_, err := pushTokenLine(t, g, p, engine.DeclareAttackers, engine.NewScriptedController(), tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%q: ResolveStack = %v, want an error naming %s", tc.line, err, tc.want)
		}
	}
}
