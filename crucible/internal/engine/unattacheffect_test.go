package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
)

// etbUnattachAuraDef builds an Aura whose own "when CARDNAME enters"
// trigger runs DB$ Unattach with the given params string appended, so
// casting it both attaches it (APIAttach, CastSpell's own Aura shape) and
// then detaches it again in the same ResolveStack call.
func etbUnattachAuraDef(t *testing.T, name, extraParams string) *compile.Card {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(reg, "Enchantment Aura")
	raw.Faces[0].Keywords = []string{"Enchant:Creature"}
	raw.Faces[0].Triggers = []string{
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigUnattach",
	}
	raw.Faces[0].SVars.Set("TrigUnattach", "DB$ Unattach | "+extraParams)

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// TestUnattachEffectDetachesTheDefinedCard proves the corpus's own only real
// shape: Defined$ Self detaches the ability's own host (Game.Unattach,
// game.go) itself -- Unattach does not move the card, though a
// now-unattached Aura is cleaned up to the graveyard on the very next
// state-based-action pass regardless (CR 704.5m, cleanupDanglingAttachments,
// action.go), which is why this test observes the graveyard rather than the
// battlefield: Unattach's own real corpus use always pairs it with a
// SubAbility$ that reattaches to a new host before that pass ever runs.
func TestUnattachEffectDetachesTheDefinedCard(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	target := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	aura := g.NewCard(etbUnattachAuraDef(t, "Test Unattach Aura", "Defined$ Self"), p, engine.Hand)

	c := engine.NewScriptedController()
	if !g.CastSpell(p, aura, c) {
		t.Fatal("CastSpell failed casting an Aura with exactly one legal target")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if _, attached := g.Card(aura).AttachedTo(); attached {
		t.Error("aura still attached after its own Unattach resolved, want detached")
	}
	if len(g.Card(target).Attachments()) != 0 {
		t.Error("target still lists the aura as an attachment, want none")
	}
	if g.Card(aura).Zone != engine.Graveyard {
		t.Errorf("aura zone = %v, want Graveyard (cleanupDanglingAttachments' own next state-based-action pass)", g.Card(aura).Zone)
	}
}

// TestUnattachEffectRejectsUnresolvedParam proves unattachUnresolvedParams'
// own fail-loud contract (PORT-8/GO-7).
func TestUnattachEffectRejectsUnresolvedParam(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p := g.Players()[0]
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(g.Players()[1]).Life = 20, 20
	g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	aura := g.NewCard(etbUnattachAuraDef(t, "Test Unattach Condition", "Defined$ Self | Condition$ Teamwork"), p, engine.Hand)

	c := engine.NewScriptedController()
	if !g.CastSpell(p, aura, c) {
		t.Fatal("CastSpell failed casting an Aura with exactly one legal target")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err == nil {
		t.Fatal("ResolveStack succeeded, want an error for unresolved Condition$")
	}
}
