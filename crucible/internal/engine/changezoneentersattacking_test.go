package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// resolveFromZone is resolveNow with the host placed in zone instead of on
// the battlefield: a "return CARDNAME from your graveyard" line.
func resolveFromZone(t *testing.T, g *engine.Game, p engine.PlayerID, zone engine.ZoneType, c *engine.ScriptedController, targets []engine.EntityID, trig string) (engine.CardID, error) {
	t.Helper()
	def := etbChainDef(t, "Test Zone", trig)
	host := g.NewCard(def, p, zone)
	face := def.Faces[0]
	for _, sub := range face.Triggers[0].Subs {
		if !strings.EqualFold(sub.Key, "Execute") {
			continue
		}
		api, ok := engine.APIByName(sub.Ability.Name)
		if !ok {
			t.Fatalf("unknown API %q", sub.Ability.Name)
		}
		g.PushAbility(engine.Ability{API: api, Source: host, Controller: p, Params: sub.Ability, Amounts: face.Amounts, Targets: targets})
		return host, g.ResolveStack(engine.NewRegistry(), c)
	}
	t.Fatal("no Execute$")
	return 0, nil
}

// Warcry Phoenix's shape (a known origin, ChangeZoneEffect.java:703): the
// card returns tapped and attacking the defender its controller picks, and
// after blockers are declared it is an unblocked attacker
// (SpellAbilityEffect.java:786-792).
func TestChangeZoneAttackingReturnsTheCardAttacking(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.DeclareBlockers)
	c := engine.NewScriptedController()
	c.QueueAttackTarget(engine.PlayerEntity(other))
	host, err := resolveFromZone(t, g, p, engine.Graveyard, c, nil,
		"DB$ ChangeZone | Defined$ Self | Origin$ Graveyard | Destination$ Battlefield | Tapped$ True | Attacking$ True")
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	h := g.Card(host)
	if h.Zone != engine.Battlefield || !h.Tapped {
		t.Fatalf("host zone = %v tapped = %v, want Battlefield, tapped", h.Zone, h.Tapped)
	}
	if got := g.AttackTarget(host); got != engine.PlayerEntity(other) {
		t.Errorf("AttackTarget = %v, want %v", got, engine.PlayerEntity(other))
	}
	if !slicesContains(g.Attackers(), host) {
		t.Errorf("attackers = %v, want the returned card among them", g.Attackers())
	}
}

// A search from hand (ChangeZoneEffect.java:1391, before moveToPlay): the
// card is attacking as it enters, so an "enters attacking" watcher (Arni
// Metalbrow's ValidCard$ Creature.YouCtrl+attacking) triggers off it.
func TestChangeZoneAttackingFromHandIsAttackingAsItEnters(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.DeclareAttackers)
	fetched := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Hand)
	g.NewCard(watcherDef(t, "Arni Watcher", "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Creature.YouCtrl+attacking | TriggerZones$ Battlefield",
		"DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{fetched})
	c.QueueAttackTarget(engine.PlayerEntity(other))
	if _, err := resolveNow(t, g, p, c, nil,
		"DB$ ChangeZone | Origin$ Hand | Destination$ Battlefield | ChangeType$ Creature | Tapped$ True | Attacking$ True"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.AttackTarget(fetched); got != engine.PlayerEntity(other) {
		t.Errorf("AttackTarget = %v, want %v", got, engine.PlayerEntity(other))
	}
	if got := g.Player(p).Life; got != 21 {
		t.Errorf("life = %d, want 21 (the enters-attacking watcher triggered)", got)
	}
}

// GainControl$ (Chorale of the Void): an opponent's card enters under the
// attacking player's control, so it attacks; Java's search sets the
// controller before addToCombat (ChangeZoneEffect.java:1322-1330).
func TestChangeZoneAttackingWithGainControlAttacksForTheNewController(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.DeclareAttackers)
	stolen := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Graveyard)
	c := engine.NewScriptedController()
	c.QueueAttackTarget(engine.PlayerEntity(other))
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(stolen)},
		"DB$ ChangeZone | Origin$ Graveyard | Destination$ Battlefield | ValidTgts$ Creature | GainControl$ True | Tapped$ True | Attacking$ True"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(stolen).Controller(); got != p {
		t.Fatalf("controller = %v, want %v", got, p)
	}
	if got := g.AttackTarget(stolen); got != engine.PlayerEntity(other) {
		t.Errorf("AttackTarget = %v, want %v", got, engine.PlayerEntity(other))
	}
}

// A card that will not be the attacking player's enters without attacking
// (SpellAbilityEffect.java:768), and outside combat nothing joins at all
// (:761).
func TestChangeZoneAttackingSkipsANonAttackerAndOutsideCombat(t *testing.T) {
	t.Parallel()

	t.Run("not the attacking player's", func(t *testing.T) {
		t.Parallel()
		g, p, other := newTwoPlayerGame(t)
		g.SetTurnState(1, other, engine.DeclareBlockers)
		host, err := resolveFromZone(t, g, p, engine.Graveyard, engine.NewScriptedController(), nil,
			"DB$ ChangeZone | Defined$ Self | Origin$ Graveyard | Destination$ Battlefield | Attacking$ True")
		if err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if g.Card(host).Zone != engine.Battlefield || slicesContains(g.Attackers(), host) {
			t.Errorf("zone = %v attackers = %v, want on the battlefield and not attacking", g.Card(host).Zone, g.Attackers())
		}
	})
	t.Run("outside combat", func(t *testing.T) {
		t.Parallel()
		g, p, _ := newTwoPlayerGame(t)
		host, err := resolveFromZone(t, g, p, engine.Graveyard, engine.NewScriptedController(), nil,
			"DB$ ChangeZone | Defined$ Self | Origin$ Graveyard | Destination$ Battlefield | Attacking$ True")
		if err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if g.Card(host).Zone != engine.Battlefield || len(g.Attackers()) != 0 {
			t.Errorf("zone = %v attackers = %v, want on the battlefield, no combat", g.Card(host).Zone, g.Attackers())
		}
	})
}

// Aetherplasm's Blocking$ (ChangeZoneEffect.java:1391,
// SpellAbilityEffect.java:795-829): the creature put onto the battlefield
// from hand blocks the named attacker when its controller defends against
// it, and the attacker becomes blocked.
func TestChangeZoneBlockingPutsTheCardIntoTheBlock(t *testing.T) {
	t.Parallel()

	g, p, other, attacker := attackingCombat(t)
	g.SetTurnState(1, other, engine.DeclareBlockers)
	bc := engine.NewScriptedController()
	bc.QueueBlocks(nil)
	declareBlockers(t, g, bc)
	blocker := g.NewCard(creatureDefPT(t, "1", "4"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{blocker})
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(attacker)},
		"DB$ ChangeZone | Origin$ Hand | Destination$ Battlefield | ChangeType$ Creature | Blocking$ Targeted"); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	found := false
	for _, b := range g.Blocks() {
		if b.Blocker == blocker && b.Attacker == attacker {
			found = true
		}
	}
	if !found {
		t.Errorf("blocks = %v, want %v blocking %v", g.Blocks(), blocker, attacker)
	}
}

// An Attacking$ Defined$ this port cannot resolve (Kaalia's
// TriggeredDefender outside a trigger) fails the line before the card moves.
func TestChangeZoneAttackingUnresolvableDefinedErrors(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.DeclareAttackers)
	host, err := resolveFromZone(t, g, p, engine.Graveyard, engine.NewScriptedController(), nil,
		"DB$ ChangeZone | Defined$ Self | Origin$ Graveyard | Destination$ Battlefield | Attacking$ TriggeredDefender")
	if err == nil || !strings.Contains(err.Error(), "Attacking$") {
		t.Errorf("err = %v, want an Attacking$ error", err)
	}
	if z := g.Card(host).Zone; z != engine.Graveyard {
		t.Errorf("zone = %v, want Graveyard (nothing moved)", z)
	}
}

func slicesContains(ids []engine.CardID, id engine.CardID) bool {
	for _, x := range ids {
		if x == id {
			return true
		}
	}
	return false
}
