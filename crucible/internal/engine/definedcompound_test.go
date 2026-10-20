package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// Defined$ wrapping another Defined$: a "Base.Prop" filter keeps the base's
// cards that match Card.Prop, and "A & B" joins both lists, each card once.
func TestDefinedCardsFilterAndJoin(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name      string
		defined   string
		wantPower int
	}{
		{"filter keeps the creature", "Targeted.Creature", 3},
		{"filter drops everything", "Targeted.Land", 1},
		{"join lists a card once", "Targeted.Creature & Targeted", 3},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGame(t)
			creature := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
			// a non-creature second target, so the filter has something to drop
			other := g.NewCard(artifactDefT(t), p, engine.Battlefield)
			if _, err := resolveNow(t, g, p, engine.NewScriptedController(),
				[]engine.EntityID{engine.CardEntity(creature), engine.CardEntity(other)},
				"DB$ Pump | Defined$ "+tc.defined+" | NumAtt$ +2 | Duration$ Permanent"); err != nil {
				t.Fatal(err)
			}
			if got, _ := g.Card(creature).Power(); got != tc.wantPower {
				t.Errorf("creature power = %d, want %d", got, tc.wantPower)
			}
		})
	}
}

// Defined$ "AttachedBy <x>" names what each of x is attached to, and
// "AttachedTo <x>" what is attached to each of x (AbilityUtils.java:96-117).
func TestDefinedAttachedByAndAttachedTo(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name    string
		defined string
		target  func(creature, aura engine.CardID) engine.CardID
		pumped  string
	}{
		{"the creature an aura enchants", "AttachedBy Targeted", func(_, aura engine.CardID) engine.CardID { return aura }, "creature"},
		{"what is attached to a creature", "AttachedTo Targeted", func(creature, _ engine.CardID) engine.CardID { return creature }, "aura"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()

			g, p, _ := newTwoPlayerGame(t)
			creature := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
			aura := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
			g.Attach(aura, creature)
			if _, err := resolveNow(t, g, p, engine.NewScriptedController(),
				[]engine.EntityID{engine.CardEntity(tc.target(creature, aura))},
				"DB$ Pump | Defined$ "+tc.defined+" | NumAtt$ +2 | Duration$ Permanent"); err != nil {
				t.Fatal(err)
			}
			sba(g)
			pumped, other := creature, aura
			if tc.pumped == "aura" {
				pumped, other = aura, creature
			}
			if got, _ := g.Card(pumped).Power(); got != 3 {
				t.Errorf("%s power = %d, want 3", tc.pumped, got)
			}
			if got, _ := g.Card(other).Power(); got != 1 {
				t.Errorf("the other card's power = %d, want 1", got)
			}
		})
	}
}

// Defined$ Caster is the player who cast the host (AbilityUtils.java:1165).
func TestDefinedCasterNamesWhoCastTheHost(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	def := scriptDef(t, "Caster Reader", "Creature Elf",
		"T:Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigGain",
		"SVar:TrigGain:DB$ GainLife | Defined$ Caster | LifeAmount$ 3")
	card := g.NewCard(def, p, engine.Hand)
	if err := castEntrant(t, g, p, card, engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23 (the caster gained 3)", got)
	}
}
