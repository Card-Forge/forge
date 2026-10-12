package compile_test

import (
	"errors"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// A Haunt instant or sorcery has no A:SP$ line: its haunt SVar is the spell
// (CardFactoryUtil.java:3069-3083), with no Cost$ of its own, and the card
// gains the trigger that exiles it after resolving plus the one that runs the
// SVar again when the haunted creature dies.
func TestHauntSpellIsItsSVar(t *testing.T) {
	t.Parallel()

	face := compileScript(t, "Name:X\nManaCost:W\nTypes:Sorcery\nK:Haunt:TrigGain\n"+
		"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ 1\n").Faces[0]
	if len(face.Abilities) != 1 {
		t.Fatalf("%d abilities, want the one spell", len(face.Abilities))
	}
	spell := face.Abilities[0]
	if spell.Record != compile.Spell || spell.Name != "GainLife" || spell.Keyword != "Haunt:TrigGain" {
		t.Errorf("spell is %s %q from %q, want Spell GainLife from the keyword", spell.Record, spell.Name, spell.Keyword)
	}
	if _, ok := spell.Param("Cost"); ok {
		t.Error("spell carries Cost$, want none: the card's mana cost pays for it")
	}
	if len(face.Triggers) != 2 {
		t.Fatalf("%d triggers, want 2", len(face.Triggers))
	}
	for i, want := range []struct{ origin, execute string }{{"Stack", "Haunt"}, {"Battlefield", "GainLife"}} {
		tr := face.Triggers[i]
		origin, _ := tr.Param("Origin")
		if origin != want.origin || len(tr.Subs) != 1 || tr.Subs[0].Ability.Name != want.execute {
			t.Errorf("trigger %d: Origin$ %q, want %q executing %s", i, origin, want.origin, want.execute)
		}
	}
	if fizzle, _ := face.Triggers[0].Param("Fizzle"); fizzle != "False" {
		t.Errorf("haunter trigger Fizzle$ %q, want False", fizzle)
	}
}

// A Haunt creature casts as a creature: no spell is synthesized, and it gets
// an enters trigger running the SVar ahead of the dies-and-exile one.
func TestHauntCreatureEntersAndDies(t *testing.T) {
	t.Parallel()

	face := compileScript(t, "Name:X\nManaCost:W\nTypes:Creature Spirit\nPT:1/1\nK:Haunt:TrigGain\n"+
		"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ 1\n").Faces[0]
	if len(face.Abilities) != 0 {
		t.Errorf("%d abilities, want none", len(face.Abilities))
	}
	if len(face.Triggers) != 3 {
		t.Fatalf("%d triggers, want 3", len(face.Triggers))
	}
	for i, want := range []string{"GainLife", "Haunt", "GainLife"} {
		if got := face.Triggers[i].Subs[0].Ability.Name; got != want {
			t.Errorf("trigger %d executes %s, want %s", i, got, want)
		}
	}
}

// A Haunt keyword naming no SVar, or one the card does not define, is a
// script to fix, not a keyword to skip (PORT-8).
func TestHauntWithoutItsSVarFails(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		line string
		want error
	}{
		{"K:Haunt\n", compile.ErrBadHaunt},
		{"K:Haunt:Missing\n", compile.ErrMissingSVar},
	} {
		card, err := carddb.ParseScript(testRegistry(t), "fixture", []byte("Name:X\nManaCost:W\nTypes:Sorcery\n"+tc.line))
		if err != nil {
			t.Fatalf("ParseScript failed: %v", err)
		}
		if _, err := compile.Compile(card); !errors.Is(err, tc.want) {
			t.Errorf("%q: Compile err %v, want %v", tc.line, err, tc.want)
		}
	}
}
