package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// castGraveyardAura casts an Aura whose Enchant type is spec from p's hand and
// reports the Aura and whether the cast went through. The Aura's
// cost is free, so nothing but the host choice is under test.
func castGraveyardAura(t *testing.T, g *engine.Game, p engine.PlayerID, spec string, c *engine.ScriptedController) (engine.CardID, bool) {
	t.Helper()
	aura := g.NewCard(auraDefWithEnchant(t, spec), p, engine.Hand)
	return aura, g.CastSpell(p, aura, c)
}

// An Aura whose Enchant type names a zone ("creature card in a graveyard",
// Animate Dead) is cast onto a graveyard card, enters attached to it, and
// stays there through state-based actions (Card.java:6986: a host outside play
// is legal when the Enchant type contains "inZone"; CardState.java:665 searches
// the graveyard). When the host leaves the graveyard the Aura no longer
// enchants a legal object and goes to its owner's graveyard.
func TestAuraEnchantsACreatureCardInAGraveyard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	host := g.NewCard(creatureDef(t), other, engine.Graveyard)
	c := engine.NewScriptedController()

	aura, ok := castGraveyardAura(t, g, p, "Creature.inZoneGraveyard", c)
	if !ok {
		t.Fatal("CastSpell refused an Aura with a creature card in a graveyard")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(aura).Zone; z != engine.Battlefield {
		t.Fatalf("aura zone = %v, want Battlefield", z)
	}
	if got, attached := g.Card(aura).AttachedTo(); !attached || got != host {
		t.Fatalf("aura attached to %v (%v), want the graveyard creature %v", got, attached, host)
	}
	if g.Card(host).Zone != engine.Graveyard {
		t.Fatalf("host zone = %v, want Graveyard", g.Card(host).Zone)
	}

	engine.CheckStateBasedActions(g, c)
	if z := g.Card(aura).Zone; z != engine.Battlefield {
		t.Errorf("aura zone after state-based actions = %v, want Battlefield", z)
	}

	g.Move(host, engine.Exile, other)
	engine.CheckStateBasedActions(g, c)
	if z := g.Card(aura).Zone; z != engine.Graveyard {
		t.Errorf("aura zone after its host left the graveyard = %v, want Graveyard", z)
	}
}

// The host is a choice across every graveyard (Dance of the Dead: "a
// graveyard", not "your graveyard").
func TestGraveyardAuraChoosesAmongEveryGraveyard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(creatureDef(t), p, engine.Graveyard)
	theirs := g.NewCard(creatureDef(t), other, engine.Graveyard)
	c := engine.NewScriptedController()
	c.QueueEnchantTarget(theirs)

	aura, ok := castGraveyardAura(t, g, p, "Creature.inZoneGraveyard", c)
	if !ok {
		t.Fatal("CastSpell refused an Aura with two graveyard hosts")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got, attached := g.Card(aura).AttachedTo(); !attached || got != theirs {
		t.Errorf("aura attached to %v (%v), want the chosen opposing graveyard creature %v", got, attached, theirs)
	}
}

// Only an Enchant type naming a zone reaches the graveyard, and only that
// zone: "Creature" (Pacifism) has no legal host when the only creature card is
// in a graveyard, and "inZoneExile" matches nothing in the battlefield and
// graveyard TgtZone$ list.
func TestAuraWithoutAGraveyardZoneHasNoGraveyardHost(t *testing.T) {
	t.Parallel()

	for _, spec := range []string{"Creature", "Creature.inZoneExile"} {
		g, p, other := newTwoPlayerGame(t)
		g.NewCard(creatureDef(t), other, engine.Graveyard)
		if _, ok := castGraveyardAura(t, g, p, spec, engine.NewScriptedController()); ok {
			t.Errorf("Enchant:%s was cast with only a graveyard creature to enchant", spec)
		}
	}
}

// CR 608.2b: the Aura spell fizzles when its graveyard host is gone by the time
// it resolves, and goes to the graveyard instead of the battlefield.
func TestGraveyardAuraFizzlesWhenItsHostLeavesTheGraveyard(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	host := g.NewCard(creatureDef(t), other, engine.Graveyard)
	c := engine.NewScriptedController()
	aura, ok := castGraveyardAura(t, g, p, "Creature.inZoneGraveyard", c)
	if !ok {
		t.Fatal("CastSpell refused an Aura with a creature card in a graveyard")
	}
	g.Move(host, engine.Exile, other)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(aura).Zone; z != engine.Graveyard {
		t.Errorf("aura zone = %v, want Graveyard (fizzled)", z)
	}
}

// Attach with Defined$ and no ValidTgts$ (Animate Dead's re-attach) attaches
// the source itself; what it cannot decide stays an error (GO-7).
func TestAttachDefinedRefusals(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ line, want string }{
		{"DB$ Attach | Defined$ Self | Chooser$ Opponent", "Chooser$"},
		{"DB$ Attach | Defined$ Player", "choosing among 2"},
		{"DB$ Attach | Defined$ You", "player host"},
	} {
		g, p, _ := newTwoPlayerGame(t)
		g.NewCard(creatureDef(t), p, engine.Battlefield)
		g.NewCard(creatureDef(t), p, engine.Battlefield)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%q: err = %v, want one naming %q", tc.line, err, tc.want)
		}
	}
}

// Attach with Choices$ and Defined$ has the activator pick the attachment among
// the matching permanents and attach it to the defined host; Optional$ asks
// first (AttachEffect.java:45-65, :144).
func TestAttachChoicesWithDefinedAndOptional(t *testing.T) {
	t.Parallel()

	for _, confirm := range []bool{true, false} {
		g, p, _ := newTwoPlayerGame(t)
		creature := g.NewCard(creatureDef(t), p, engine.Battlefield)
		equipment := g.NewCard(equipmentDef(t), p, engine.Battlefield)
		c := engine.NewScriptedController()
		c.QueueCardChoice([]engine.CardID{equipment})
		c.QueueConfirmEffect(confirm)
		if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(creature)},
			"DB$ Attach | Choices$ Equipment | Defined$ Targeted | Optional$ True"); err != nil {
			t.Fatal(err)
		}
		host, attached := g.Card(equipment).AttachedTo()
		if attached != confirm || (confirm && host != creature) {
			t.Errorf("confirm=%v: equipment attached to %v (%v), want attached=%v to the creature", confirm, host, attached, confirm)
		}
	}
}

// "Creature.EnchantedBy Aura.Other" (Daybreak Coronet) names a creature that
// already wears an Aura other than the one being cast
// (CardProperty.java:482-488); Imprinted and Targeted forms match nothing.
func TestEnchantedByNestedSpec(t *testing.T) {
	t.Parallel()

	for name, tc := range map[string]struct {
		spec     string
		hasAura  bool
		wantCast bool
	}{
		"another aura":    {spec: "Creature.EnchantedBy Aura.Other", hasAura: true, wantCast: true},
		"no aura at all":  {spec: "Creature.EnchantedBy Aura.Other"},
		"imprinted form":  {spec: "Creature.EnchantedBy Imprinted", hasAura: true},
		"targeted form":   {spec: "Creature.EnchantedBy Targeted", hasAura: true},
		"aura of no type": {spec: "Creature.EnchantedBy Land", hasAura: true},
	} {
		t.Run(name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			host := g.NewCard(creatureDef(t), p, engine.Battlefield)
			if tc.hasAura {
				g.Attach(g.NewCard(auraDef(t), p, engine.Battlefield), host)
			}
			if _, ok := castGraveyardAura(t, g, p, tc.spec, engine.NewScriptedController()); ok != tc.wantCast {
				t.Errorf("cast = %v, want %v", ok, tc.wantCast)
			}
		})
	}
}

// An Aura whose enters trigger attaches it to a chosen creature with
// RememberAttached$ remembers itself (AttachEffect.java:149-151). Cast onto
// the same creature, the attach is a no-op and the remember still happens.
func TestAttachDefinedRemembersTheAttachment(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	host := g.NewCard(creatureDef(t), p, engine.Battlefield)
	def := scriptDef(t, "Test Remembering Aura", "Enchantment Aura",
		"K:Enchant:Creature",
		"T:Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigChoose",
		"SVar:TrigChoose:DB$ ChooseCard | Defined$ You | Choices$ Creature | SubAbility$ DBAttach",
		"SVar:DBAttach:DB$ Attach | Defined$ ChosenCard | RememberAttached$ True")
	aura := g.NewCard(def, p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{host})
	if !g.CastSpell(p, aura, c) {
		t.Fatal("CastSpell refused the Aura")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got, ok := g.Card(aura).AttachedTo(); !ok || got != host {
		t.Fatalf("aura attached to %v (%v), want %v", got, ok, host)
	}
	found := false
	for _, e := range g.Card(aura).Memory.Remembered() {
		found = found || e == engine.CardEntity(aura)
	}
	if !found {
		t.Error("the attached Aura did not remember itself")
	}
}

// With no host to name, or a source that is no attachment, Attach with
// Defined$ does nothing and is not an error.
func TestAttachDefinedWithNothingToAttach(t *testing.T) {
	t.Parallel()

	for _, line := range []string{
		"DB$ Attach | Defined$ Remembered",
		"DB$ Attach | Defined$ Self",
	} {
		g, p, _ := newTwoPlayerGame(t)
		creature := g.NewCard(creatureDef(t), p, engine.Battlefield)
		src, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, line)
		if err != nil {
			t.Fatalf("%q: ResolveStack: %v", line, err)
		}
		if _, attached := g.Card(src).AttachedTo(); attached {
			t.Errorf("%q: a non-attachment source was attached", line)
		}
		if g.Card(creature).Attachments() != nil && len(g.Card(creature).Attachments()) != 0 {
			t.Errorf("%q: the creature gained an attachment", line)
		}
	}
}
