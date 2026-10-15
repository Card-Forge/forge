package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// This file covers the layer rebuild before each sub-ability
// (AbilityUtils.resolveSubAbilities' checkStaticAbilities,
// AbilityUtils.java:1366-1367): a link reads what the link before it changed.

// TestSubAbilityReadsThePumpedPower proves a Pump's +2/+0 is on the host when
// the next link counts its power.
func TestSubAbilityReadsThePumpedPower(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ Pump | Defined$ Self | NumAtt$ +2 | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ X",
		"X", "Count$CardPower")
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23 (the 1/1 host pumped to 3 power)", got)
	}
}

// TestSubAbilityInsideStateBasedActionsKeepsTheHold proves a replacement's
// chain that a state-based action reaches still runs every link: the rebuild
// is held while the pass runs (GameAction.java:1588-1618), the chain is not.
func TestSubAbilityInsideStateBasedActionsKeepsTheHold(t *testing.T) {
	t.Parallel()

	g, p, opp := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(scriptDef(t, "Warden", "Creature Spirit",
		"R:Event$ Moved | ActiveZones$ Battlefield | Origin$ Battlefield | Destination$ Graveyard | ValidLKI$ Creature.OppCtrl | ReplaceWith$ Exile",
		"SVar:Exile:DB$ ChangeZone | Origin$ Battlefield | Destination$ Exile | Defined$ ReplacedCard | SubAbility$ DBGain",
		"SVar:DBGain:DB$ GainLife | Defined$ You | LifeAmount$ 1 | SubAbility$ DBGainMore",
		"SVar:DBGainMore:DB$ GainLife | Defined$ You | LifeAmount$ 2"), opp, engine.Battlefield)
	victim := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
	g.Card(victim).Damage.Mark(2, false)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if err := g.TakePendingError(); err != nil {
		t.Fatalf("pending error: %v", err)
	}
	if z := g.Card(victim).Zone; z != engine.Exile {
		t.Errorf("victim zone = %v, want Exile", z)
	}
	if got := g.Player(opp).Life; got != 23 {
		t.Errorf("Warden's controller life = %d, want 23", got)
	}
}
