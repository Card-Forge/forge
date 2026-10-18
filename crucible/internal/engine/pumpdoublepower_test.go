package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// NumAtt$ Double gives the target +X/+0 where X is its own power
// (PumpEffect.java:468-470): Overclocked Electromancer's "double its power".
func TestPumpDoubleNumAttDoublesTheTargetsPower(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	target := g.NewCard(creatureDefPT(t, "3", "2"), p, engine.Battlefield)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(target)},
		"DB$ Pump | ValidTgts$ Creature | NumAtt$ Double"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	pw, _ := g.Card(target).Power()
	tg, _ := g.Card(target).Toughness()
	if pw != 6 || tg != 2 {
		t.Errorf("P/T = %d/%d, want 6/2", pw, tg)
	}
}

// Triple adds the stat twice (PumpEffect.java:475-480), Double on NumDef$
// doubles toughness, and each target is read on its own.
func TestPumpTripleAndDoubleReadEachTarget(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	first := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	second := g.NewCard(creatureDefPT(t, "1", "3"), other, engine.Battlefield)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(),
		[]engine.EntityID{engine.CardEntity(first), engine.CardEntity(second)},
		"DB$ Pump | ValidTgts$ Creature | TargetMin$ 2 | TargetMax$ 2 | NumAtt$ Triple | NumDef$ Double"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	for _, c := range []struct {
		id           engine.CardID
		wantP, wantT int
	}{{first, 6, 4}, {second, 3, 6}} {
		pw, _ := g.Card(c.id).Power()
		tg, _ := g.Card(c.id).Toughness()
		if pw != c.wantP || tg != c.wantT {
			t.Errorf("card %d P/T = %d/%d, want %d/%d", c.id, pw, tg, c.wantP, c.wantT)
		}
	}
}

// A plain NumDef$ beside NumAtt$ Double still adds its own amount.
func TestPumpDoubleKeepsAPlainSecondAmount(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	target := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(target)},
		"DB$ Pump | ValidTgts$ Creature | NumAtt$ Double | NumDef$ 1"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	pw, _ := g.Card(target).Power()
	tg, _ := g.Card(target).Toughness()
	if pw != 4 || tg != 3 {
		t.Errorf("P/T = %d/%d, want 4/3", pw, tg)
	}
}

// The refusals: a target whose power does not resolve, Radiance$ beside a
// Double (Java reuses the last target's amount for the radiance cards), and
// PumpAll, which has no per-target Double at all.
func TestPumpDoubleRefusals(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, line, power, want string
	}{
		{"unresolvable power", "DB$ Pump | ValidTgts$ Creature | NumAtt$ Double", "X", "not resolvable"},
		{"radiance", "DB$ Pump | ValidTgts$ Creature | NumAtt$ Double | Radiance$ True", "2", "Radiance"},
		{"pumpall", "DB$ PumpAll | ValidCards$ Creature | NumAtt$ Double", "2", "NumAtt"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			target := g.NewCard(creatureDefPT(t, tc.power, "2"), p, engine.Battlefield)
			_, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(target)}, tc.line)
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Errorf("err = %v, want one containing %q", err, tc.want)
			}
		})
	}
}
