package compile_test

import (
	"errors"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// Animate's Triggers$ names the SVars of the triggers it grants; each compiles
// into a Trigger record with its own Execute$ chain, so the engine never parses
// a granted trigger at resolution (PORT-2, ADR-0023 decision 1).
func TestAnimateTriggersCompileToGrantedTriggers(t *testing.T) {
	t.Parallel()

	card := compileScript(t, "Name:X\nManaCost:R\nTypes:Instant\n"+
		"A:SP$ Animate | Defined$ Self | Triggers$ CastTrig,DamageTrig | Duration$ Perpetual\n"+
		"SVar:CastTrig:Mode$ SpellCast | ValidCard$ Card.Self | Execute$ TrigDraw\n"+
		"SVar:DamageTrig:Mode$ DamageDone | ValidSource$ Card.Self | Execute$ TrigDraw\n"+
		"SVar:TrigDraw:DB$ Draw | NumCards$ 1\n")

	subs := card.Faces[0].Abilities[0].Subs
	if len(subs) != 2 {
		t.Fatalf("Animate resolved %d grants, want 2", len(subs))
	}
	for i, want := range []string{"SpellCast", "DamageDone"} {
		got := subs[i].Ability
		if subs[i].Key != "Triggers" || got.Record != compile.Trigger || got.Name != want {
			t.Errorf("grant %d = %s$ %v %q, want Triggers$ Trigger %q", i, subs[i].Key, got.Record, got.Name, want)
		}
		if len(got.Subs) != 1 || got.Subs[0].Key != "Execute" || got.Subs[0].Ability.Name != "Draw" {
			t.Errorf("grant %d's Execute$ = %+v, want the compiled Draw", i, got.Subs)
		}
	}
}

// A granted trigger whose own chain grants it again (Snarlfang Vermin: "It
// perpetually gains this ability") is a loop Java closes only at apply time.
// The card still compiles; the regrant is left unfollowed for the engine to
// refuse.
func TestSelfRegrantingTriggerIsLeftUnfollowed(t *testing.T) {
	t.Parallel()

	card := compileScript(t, "Name:X\nManaCost:B\nTypes:Creature\nPT:2/1\n"+
		"T:Mode$ DamageDone | ValidSource$ Card.Self | Execute$ TrigSuspect\n"+
		"SVar:TrigSuspect:DB$ AlterAttribute | Attributes$ Suspected | SubAbility$ DBAnimate\n"+
		"SVar:DBAnimate:DB$ Animate | Triggers$ CombatTrig | Duration$ Perpetual\n"+
		"SVar:CombatTrig:Mode$ DamageDone | ValidSource$ Card.Self | Execute$ TrigSuspect\n")

	animate := card.Faces[0].Triggers[0].Subs[0].Ability.Subs[0].Ability
	if animate.Name != "Animate" {
		t.Fatalf("chain reached %q, want Animate", animate.Name)
	}
	if len(animate.Subs) != 0 {
		t.Errorf("Animate followed %d grants, want the regrant left unfollowed", len(animate.Subs))
	}
}

// A cycle wholly inside a granted trigger's own chain stays an error: only a
// granted trigger coming back to the chain that granted it is exempt.
func TestCycleInsideAGrantedTriggerStillFails(t *testing.T) {
	t.Parallel()

	card, err := carddb.ParseScript(testRegistry(t), "fixture", []byte("Name:X\nManaCost:R\nTypes:Instant\n"+
		"A:SP$ Animate | Triggers$ Trig\n"+
		"SVar:Trig:Mode$ Attacks | Execute$ DBLoop\n"+
		"SVar:DBLoop:DB$ Pump | SubAbility$ DBLoop\n"))
	if err != nil {
		t.Fatalf("ParseScript failed: %v", err)
	}
	if out, err := compile.Compile(card); !errors.Is(err, compile.ErrCycle) {
		t.Errorf("Compile = %+v, %v, want an error wrapping %v", out, err, compile.ErrCycle)
	}
}
