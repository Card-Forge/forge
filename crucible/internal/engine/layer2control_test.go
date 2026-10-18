package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestControlMagicShapeGainsControlThroughAffectedDefined proves Layer 2's
// GainControl$ You reaches the enchanted creature through AffectedDefined$
// Enchanted, the shape 35 of the corpus's 42 real Mode$ Continuous
// GainControl$ lines write (Control Magic), and nothing else: a bystander
// stays with its owner, and control reverts when the Aura leaves.
func TestControlMagicShapeGainsControlThroughAffectedDefined(t *testing.T) {
	t.Parallel()

	g, p, other := layer456Game(t)
	aura := g.NewCard(layer456Def(t, "Test Control Magic", "Enchantment Aura", "", nil,
		"Mode$ Continuous | AffectedDefined$ Enchanted | GainControl$ You"), p, engine.Battlefield)
	stolen := g.NewCard(layer456Creature(t, "G"), other, engine.Battlefield)
	bystander := g.NewCard(layer456Creature(t, "G"), other, engine.Battlefield)
	g.Attach(aura, stolen)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if got := g.Card(stolen).Controller(); got != p {
		t.Errorf("enchanted creature controller = %v, want the Aura's controller %v", got, p)
	}
	if got := g.Card(bystander).Controller(); got != other {
		t.Errorf("bystander controller = %v, want its owner %v", got, other)
	}

	g.Move(aura, engine.Graveyard, p)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if got := g.Card(stolen).Controller(); got != other {
		t.Errorf("controller after the Aura left = %v, want control back with the owner %v", got, other)
	}
}

// TestFealtyShapeTheMonarchControlsTheEnchantedCreature proves Fealty to the
// Realm's GainControl$ Player.isMonarch (StaticAbilityContinuous.java:597-600):
// the first defined player gains control, so the creature follows the monarch,
// and with no monarch nobody gains it.
func TestFealtyShapeTheMonarchControlsTheEnchantedCreature(t *testing.T) {
	t.Parallel()

	g, p, other := layer456Game(t)
	aura := g.NewCard(layer456Def(t, "Test Fealty", "Enchantment Aura", "", nil,
		"Mode$ Continuous | AffectedDefined$ Enchanted | GainControl$ Player.isMonarch"), p, engine.Battlefield)
	creature := g.NewCard(layer456Creature(t, "G"), other, engine.Battlefield)
	g.Attach(aura, creature)
	for _, tc := range []struct {
		name          string
		monarch, want engine.PlayerID
	}{
		{"no monarch", engine.NoPlayer, other},
		{"aura controller", p, p},
		{"owner", other, other},
	} {
		g.SetMonarch(tc.monarch)
		engine.CheckStateBasedActions(g, engine.NewScriptedController())
		if got := g.Card(creature).Controller(); got != tc.want {
			t.Errorf("%s: controller = %v, want %v", tc.name, got, tc.want)
		}
	}
	if err := g.TakePendingError(); err != nil {
		t.Fatal(err)
	}
}

// TestContinuousGainControlUnreadableSpecIsLoud proves a GainControl$ spec
// definedPlayers cannot read is a pending error, never a silent no-op (GO-7).
func TestContinuousGainControlUnreadableSpecIsLoud(t *testing.T) {
	t.Parallel()

	g, p, other := layer456Game(t)
	aura := g.NewCard(layer456Def(t, "Test Bogus Control", "Enchantment Aura", "", nil,
		"Mode$ Continuous | AffectedDefined$ Enchanted | GainControl$ Player.Bogus"), p, engine.Battlefield)
	creature := g.NewCard(layer456Creature(t, "G"), other, engine.Battlefield)
	g.Attach(aura, creature)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if err := g.TakePendingError(); err == nil || !strings.Contains(err.Error(), "GainControl$") {
		t.Errorf("pending error = %v, want the GainControl$ refusal", err)
	}
}
