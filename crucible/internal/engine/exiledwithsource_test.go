package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// exiledWithSource reports whether card matches Card.ExiledWithSource from
// host's point of view, the way an ability of host evaluates it.
func exiledWithSource(g *engine.Game, card, host engine.CardID) bool {
	return engine.Matches(g, g.Card(card), valid.Parse("Card.ExiledWithSource"), g.Card(host).Controller(), host)
}

// TestExiledWithSourceReturnsWhatTheHostExiled proves the write/read pair
// ADR-0034 builds for Karn Liberated's ReturnFromExile: a ChangeZoneAll exile
// marks each card as exiled with its host, and a later ChangeType$
// Card.ExiledWithSource of that same host brings back exactly those cards --
// not one another host exiled.
func TestExiledWithSourceReturnsWhatTheHostExiled(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	bear := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	elsewhere := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Exile)
	otherHost := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	g.SetExiledWith(elsewhere, otherHost)

	host := resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ ChangeZoneAll | ChangeType$ Creature.OppCtrl+powerEQ2 | Origin$ Battlefield | Destination$ Exile | SubAbility$ DBBack",
		"DBBack", "DB$ ChangeZoneAll | ChangeType$ Card.ExiledWithSource | Origin$ Exile | Destination$ Battlefield | GainControl$ True")

	if c := g.Card(bear); c.Zone != engine.Battlefield || c.Controller() != p {
		t.Errorf("bear zone %v controller %v, want back on the battlefield under %v", c.Zone, c.Controller(), p)
	}
	if got := g.Card(bear).ExiledWith(); got != engine.NoCard {
		t.Errorf("returned bear still exiled with %v, want the mark cleared by its zone change", got)
	}
	if z := g.Card(elsewhere).Zone; z != engine.Exile {
		t.Errorf("card exiled with another host moved to %v, want it left in exile", z)
	}
	if exiledWithSource(g, elsewhere, host) {
		t.Error("a card exiled with another host reads as exiled with this one")
	}
}

// TestExiledWithSourceFollowsTheHostObject pins the object identity Java's
// equalsWithGameTimestamp comparison gives the property: while the host is
// on the battlefield it matches; once the host has left, an ability of it
// still sees its last battlefield object (a leaves-the-battlefield trigger,
// RestartGame's own host shuffled away mid-resolution); once it has come
// back it is a new object that exiled nothing. A card leaving exile loses
// its mark.
func TestExiledWithSourceFollowsTheHostObject(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	bear := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	host := resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ ChangeZoneAll | ChangeType$ Creature.OppCtrl | Origin$ Battlefield | Destination$ Exile")

	if got := g.Card(bear).ExiledWith(); got != host {
		t.Fatalf("bear exiled with %v, want the host %v", got, host)
	}
	if !exiledWithSource(g, bear, host) {
		t.Error("bear does not read as exiled with its host on the battlefield")
	}
	g.Move(host, engine.Graveyard, p)
	if !exiledWithSource(g, bear, host) {
		t.Error("bear does not read as exiled with its host's last battlefield object")
	}
	g.Move(host, engine.Battlefield, p)
	if exiledWithSource(g, bear, host) {
		t.Error("bear reads as exiled with a host that has since left and come back")
	}

	g.Move(bear, engine.Hand, other)
	g.Move(bear, engine.Exile, other)
	if got := g.Card(bear).ExiledWith(); got != engine.NoCard {
		t.Errorf("bear exiled again by nothing still exiled with %v", got)
	}
}

// TestExiledWithSourceMarksEveryJavaExilePath covers the other effects Java
// calls handleExiledWith from (SpellAbilityEffect.java:1079-1116's callers):
// ChangeZone's known and hidden origins, Dig and DigUntil.
func TestExiledWithSourceMarksEveryJavaExilePath(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, line string
		zone       engine.ZoneType
	}{
		{"ChangeZone known", "DB$ ChangeZone | ValidTgts$ Creature | Origin$ Battlefield | Destination$ Exile", engine.Battlefield},
		{"ChangeZone hidden", "DB$ ChangeZone | Origin$ Library | Destination$ Exile | ChangeType$ Creature | ChangeNum$ 1 | Mandatory$ True", engine.Library},
		{"Dig", "DB$ Dig | DigNum$ 1 | ChangeNum$ All | DestinationZone$ Exile", engine.Library},
		{"DigUntil", "DB$ DigUntil | Valid$ Creature | FoundDestination$ Exile | RevealedDestination$ Graveyard", engine.Library},
	} {
		g, p, _ := newTwoPlayerGame(t)
		target := g.NewCard(creatureDefPT(t, "2", "2"), p, tc.zone)
		c := engine.NewScriptedController()
		c.QueueCardChoice([]engine.CardID{target})
		host, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(target)}, tc.line)
		if err != nil {
			t.Fatalf("%s: %v", tc.name, err)
		}
		if z := g.Card(target).Zone; z != engine.Exile {
			t.Fatalf("%s: target in %v, want exile", tc.name, z)
		}
		if got := g.Card(target).ExiledWith(); got != host {
			t.Errorf("%s: exiled with %v, want host %v", tc.name, got, host)
		}
	}
}

// TestExiledWithSourceNeedsAListingHost proves the property's
// source.hasExiledCard half: a host that has never been on the battlefield
// and exiles from its graveyard marks the card but does not list it
// (SpellAbilityEffect.java:1100-1104), so the card never reads as exiled
// with it.
func TestExiledWithSourceNeedsAListingHost(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	bear := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	def := etbChainDef(t, "Graveyard Exiler", "DB$ ChangeZoneAll | ChangeType$ Creature.OppCtrl | Origin$ Battlefield | Destination$ Exile")
	host := g.NewCard(def, p, engine.Graveyard)
	sub := def.Faces[0].Triggers[0].Subs[0].Ability
	g.PushAbility(engine.Ability{API: engine.APIChangeZoneAll, Source: host, Controller: p, Params: sub, Amounts: def.Faces[0].Amounts})
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatal(err)
	}
	if got := g.Card(bear).ExiledWith(); got != host {
		t.Fatalf("bear exiled with %v, want host %v", got, host)
	}
	if exiledWithSource(g, bear, host) {
		t.Error("bear reads as exiled with a host that was never in play to list it")
	}
}

// TestExiledWithSourceSkipsTokens proves handleExiledWith's isToken early
// return: a token exiled by its host is never marked.
func TestExiledWithSourceSkipsTokens(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	token := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	g.Card(token).IsToken = true
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(token)},
		"DB$ ChangeZone | ValidTgts$ Creature | Origin$ Battlefield | Destination$ Exile"); err != nil {
		t.Fatal(err)
	}
	if got := g.Card(token).ExiledWith(); got != engine.NoCard {
		t.Errorf("token exiled with %v, want unmarked", got)
	}
}

// TestChangeZoneExilingAMeldedPermanentMarksBothHalves is
// ChangeZoneEffect.java's own handleExiledWith(meld, sa) call alongside its
// primary one: exiling a melded permanent moves its other card along
// (unmeld, game.go), and Java marks that card too, not only the one the
// effect explicitly chose.
func TestChangeZoneExilingAMeldedPermanentMarksBothHalves(t *testing.T) {
	t.Parallel()

	g, p, _, gis, bru := meldedPair(t, gisela, bruna)
	host, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(gis)},
		"DB$ ChangeZone | ValidTgts$ Creature | Origin$ Battlefield | Destination$ Exile")
	if err != nil {
		t.Fatal(err)
	}
	if got := g.Card(gis).ExiledWith(); got != host {
		t.Errorf("Gisela exiled with %v, want host %v", got, host)
	}
	if got := g.Card(bru).ExiledWith(); got != host {
		t.Errorf("Bruna (unmeld's own partner move) exiled with %v, want host %v too", got, host)
	}
}

// TestExiledWithSourceSurvivesOntoTheStack pins GameAction.java:576-579's
// own "if (!zoneTo.is(Stack))" guard on cleanupExiledWith: a card cast or
// activated straight out of the exile a host put it in keeps the mark while
// its own spell or ability sits on the stack -- the one zone a card leaves
// exile for without losing it.
func TestExiledWithSourceSurvivesOntoTheStack(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	host := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	card := g.NewCard(gainInstant(t, "Gain", "W", "1"), p, engine.Exile)
	g.SetExiledWith(card, host)

	g.Move(card, engine.Stack, p)
	if got := g.Card(card).ExiledWith(); got != host {
		t.Errorf("card on the stack exiled with %v, want the mark kept (%v)", got, host)
	}

	g.Move(card, engine.Graveyard, p)
	if got := g.Card(card).ExiledWith(); got != engine.NoCard {
		t.Errorf("card exiled with %v after leaving the stack, want the mark cleared", got)
	}
}

// TestChangeZoneRejectsExiledWithEffectSource proves the one exiler
// override this port does not honour errors before anything moves.
func TestChangeZoneRejectsExiledWithEffectSource(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	host, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
		"DB$ ChangeZone | Defined$ Self | Origin$ Battlefield | Destination$ Exile | ExiledWithEffectSource$ True")
	if err == nil {
		t.Fatal("ExiledWithEffectSource$ resolved, want a not-resolvable error")
	}
	if z := g.Card(host).Zone; z != engine.Battlefield {
		t.Errorf("host moved to %v before the rejection", z)
	}
}

// TestSpellBaseMatchesInstantsSorceriesAndOffBattlefieldAuras proves
// Card.isSpell (Card.java:5500-5502) as a valid-string base: an instant
// anywhere, an Aura everywhere but the battlefield, never a creature.
func TestSpellBaseMatchesInstantsSorceriesAndOffBattlefieldAuras(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	spell := valid.Parse("Spell")
	instant := g.NewCard(gainInstant(t, "Gain", "W", "1"), p, engine.Exile)
	exiledAura := g.NewCard(auraDef(t), p, engine.Exile)
	bear := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	fieldAura := g.NewCard(auraDef(t), p, engine.Battlefield)
	for _, tc := range []struct {
		id   engine.CardID
		want bool
	}{{instant, true}, {exiledAura, true}, {bear, false}, {fieldAura, false}} {
		if got := engine.Matches(g, g.Card(tc.id), spell, p, engine.NoCard); got != tc.want {
			t.Errorf("%s in %v: Spell = %v, want %v", g.Card(tc.id).Def.Name, g.Card(tc.id).Zone, got, tc.want)
		}
	}
}
