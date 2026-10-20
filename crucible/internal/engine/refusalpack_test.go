package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestDestinationAlternativeLetsTheDeciderChoose proves handleAltDest
// (ChangeZoneEffect.java:1659-1703): the targeted card's owner decides
// (AlternativeDecider$ TargetedOwner); confirming keeps the destination,
// declining takes DestinationAlternative$ (Condemn-style top or bottom).
func TestDestinationAlternativeLetsTheDeciderChoose(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name    string
		confirm bool
		wantPos int
	}{{"confirm keeps the top", true, 0}, {"decline takes the bottom", false, -1}} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			g.NewCard(creatureDef(t), p, engine.Library)
			victim := g.NewCard(creatureDef(t), p, engine.Battlefield)
			c := engine.NewScriptedController()
			c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
			c.QueueConfirmEffect(tc.confirm)
			resolveLine(t, g, p, c,
				"DB$ ChangeZone | ValidTgts$ Creature | Origin$ Battlefield | Destination$ Library | DestinationAlternative$ Library | LibraryPositionAlternative$ -1 | AlternativeDecider$ TargetedOwner")
			lib := g.Zone(engine.Library, p).Cards()
			if g.Card(victim).Zone != engine.Library {
				t.Fatalf("victim zone = %v, want Library", g.Card(victim).Zone)
			}
			if got := lib[0] == victim; got != (tc.wantPos == 0) {
				t.Errorf("victim on top = %v, want %v", got, tc.wantPos == 0)
			}
		})
	}
}

// A hidden origin runs handleAltDest after the search has chosen
// (ChangeZoneEffect.java:1252-1256): the searched creature goes to the hand on
// a confirm and to the graveyard on a decline.
func TestDestinationAlternativeOnASearch(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		confirm bool
		want    engine.ZoneType
	}{{true, engine.Hand}, {false, engine.Graveyard}} {
		g, p, _ := newTwoPlayerGame(t)
		fetched := g.NewCard(creatureDef(t), p, engine.Library)
		c := engine.NewScriptedController()
		c.QueueCardChoice([]engine.CardID{fetched})
		c.QueueConfirmEffect(tc.confirm)
		resolveLine(t, g, p, c,
			"DB$ ChangeZone | Origin$ Library | Destination$ Hand | DestinationAlternative$ Graveyard | ChangeType$ Creature | ChangeNum$ 1")
		if got := g.Card(fetched).Zone; got != tc.want {
			t.Errorf("confirm %v: creature in %v, want %v", tc.confirm, got, tc.want)
		}
	}
}

// TestRememberLKIKeepsTheDestroyedCreaturesController proves RememberLKI$ on
// Destroy (DestroyEffect.java:102-104): RememberedController reads the
// controller the creature had on the battlefield.
func TestRememberLKIKeepsTheDestroyedCreaturesController(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDef(t), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	resolveLine(t, g, p, c,
		"DB$ Destroy | ValidTgts$ Creature | RememberLKI$ True | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ RememberedController | LifeAmount$ 3")
	if g.Card(victim).Zone != engine.Graveyard || g.Player(other).Life != 23 {
		t.Errorf("victim %v, its controller's life %d, want Graveyard and 23", g.Card(victim).Zone, g.Player(other).Life)
	}
}

// TestPumpAllUntilYourNextTurnEndsAtTheUntap proves PumpAll's Duration$
// UntilYourNextTurn (SpellAbilityEffect.java:980-981) is accepted and outlasts
// the turn it was made in.
func TestPumpAllUntilYourNextTurnIsAccepted(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	bear := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ PumpAll | ValidCards$ Creature.YouCtrl | NumAtt$ 1 | NumDef$ 1 | Duration$ UntilYourNextTurn")
	if pw, _ := g.Card(bear).Power(); pw != 3 {
		t.Errorf("power = %d, want 3", pw)
	}
	for g.ActivePhase() != engine.Cleanup {
		g.AdvancePhase(engine.NewScriptedController())
	}
	g.AdvancePhase(engine.NewScriptedController())
	if pw, _ := g.Card(bear).Power(); pw != 3 {
		t.Errorf("power after cleanup = %d, want 3: the pump lasts until the activator's next turn", pw)
	}
}
