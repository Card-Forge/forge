package engine_test

import (
	"fmt"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// TestChangeZoneEffectBouncesTargetToHand proves the known-origin path's
// dominant shape: a targeted permanent moves from the battlefield to its
// owner's hand.
func TestChangeZoneEffectBouncesTargetToHand(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	target := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})
	def := etbChainDef(t, "Test Bounce", "DB$ ChangeZone | ValidTgts$ Creature.OppCtrl | Origin$ Battlefield | Destination$ Hand")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z, o := g.Card(target).Zone, g.Card(target).ZoneOwner; z != engine.Hand || o != other {
		t.Errorf("target zone = %v of %v, want Hand of %v", z, o, other)
	}
}

// TestChangeZoneEffectReanimatesUnderYourControlTapped proves the
// battlefield destination's entering modifiers: Tapped$ and GainControl$
// put another player's graveyard creature onto the battlefield tapped and
// under the activator's control, summoning sick.
func TestChangeZoneEffectReanimatesUnderYourControlTapped(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	dead := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Graveyard)

	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{dead})
	def := etbChainDef(t, "Test Reanimate",
		"DB$ ChooseCard | ChoiceZone$ Graveyard | Choices$ Creature | Mandatory$ True | RememberChosen$ True | SubAbility$ DBReturn",
		"DBReturn", "DB$ ChangeZone | Defined$ Remembered | Origin$ Graveyard | Destination$ Battlefield | Tapped$ True | GainControl$ True")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	card := g.Card(dead)
	if card.Zone != engine.Battlefield || !card.Tapped || card.Controller() != p || !card.SummonSick {
		t.Errorf("reanimated: zone %v tapped %v controller %v sick %v, want Battlefield true %v true",
			card.Zone, card.Tapped, card.Controller(), card.SummonSick, p)
	}
}

// TestChangeZoneEffectTutorsFromLibrary proves the hidden-origin path: a
// library search offers only ChangeType$ matches and moves the pick.
func TestChangeZoneEffectTutorsFromLibrary(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(landDef(t, "Test Land", "Basic Land Plains"), p, engine.Library)
	wanted := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Library)
	g.NewCard(landDef(t, "Test Land", "Basic Land Plains"), p, engine.Library)

	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{wanted})
	def := etbChainDef(t, "Test Tutor", "DB$ ChangeZone | Origin$ Library | Destination$ Hand | ChangeType$ Creature")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(wanted).Zone; z != engine.Hand {
		t.Errorf("searched card zone = %v, want Hand", z)
	}
	if n := len(g.Zone(engine.Library, p).Cards()); n != 2 {
		t.Errorf("library size = %d, want 2", n)
	}
}

// TestChangeZoneEffectTutorToTopShufflesFirst proves a search whose
// destination is the library itself shuffles before the move, so the found
// card stays on top.
func TestChangeZoneEffectTutorToTopShufflesFirst(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	libraryCards(t, g, p, 4)
	wanted := g.NewCard(landDef(t, "Test Land", "Basic Land Plains"), p, engine.Library)

	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{wanted})
	def := etbChainDef(t, "Test Tutor Top", "DB$ ChangeZone | Origin$ Library | Destination$ Library | LibraryPosition$ 0 | ChangeType$ Land")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if top := g.Zone(engine.Library, p).Cards()[0]; top != wanted {
		t.Errorf("library top = %v, want %v", top, wanted)
	}
}

// TestChangeZoneEffectOrdersCardsIntoLibrary proves CR 401.4: two cards
// going to the same library are ordered by their owner, and with
// LibraryPosition$ 0 the last one moved ends on top.
func TestChangeZoneEffectOrdersCardsIntoLibrary(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{a, b})
	c.QueueCardOrder([]engine.CardID{b, a})
	def := etbChainDef(t, "Test Tuck",
		"DB$ ChooseCard | Choices$ Creature.OppCtrl | Amount$ 2 | Mandatory$ True | RememberChosen$ True | SubAbility$ DBTuck",
		"DBTuck", "DB$ ChangeZone | Defined$ Remembered | Origin$ Battlefield | Destination$ Library")
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	lib := g.Zone(engine.Library, other).Cards()
	if len(lib) != 2 || lib[0] != a || lib[1] != b {
		t.Errorf("library = %v, want [%v %v]", lib, a, b)
	}
}

// TestChangeZoneEffectOptionalDeclineKeepsCard proves Optional$: a declined
// move leaves the card where it was.
func TestChangeZoneEffectOptionalDeclineKeepsCard(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(false)
	host, err := castETBChain(t, g, p, etbChainDef(t, "Test Optional", "DB$ ChangeZone | Defined$ Self | Origin$ Battlefield | Destination$ Hand | Optional$ True"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(host).Zone; z != engine.Battlefield {
		t.Errorf("host zone = %v, want Battlefield", z)
	}
}

// TestChangeZoneLibraryPositionCountsDown proves LibraryPosition$ N puts the
// card N cards below the top (zone.add(card, N)), and a position past the end
// of the library is the bottom.
func TestChangeZoneLibraryPositionCountsDown(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		pos, want int
	}{{1, 1}, {2, 2}, {9, 3}} {
		g, p, _ := newTwoPlayerGame(t)
		lib := libraryCards(t, g, p, 3)
		victim := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
		line := fmt.Sprintf("DB$ ChangeZone | ValidTgts$ Creature | Origin$ Battlefield | Destination$ Library | LibraryPosition$ %d", tc.pos)
		if _, err := castETBChain(t, g, p, etbChainDef(t, "Test Second From Top", line), c); err != nil {
			t.Fatalf("pos %d: ResolveStack: %v", tc.pos, err)
		}
		cards := g.Zone(engine.Library, p).Cards()
		if len(cards) != 4 || cards[tc.want] != victim {
			t.Errorf("pos %d: library = %v, want %d at index %d (original %v)", tc.pos, cards, victim, tc.want, lib)
		}
	}
}

// TestChangeZoneEffectRejectsUnresolvedShapes proves the fail-closed
// params and zones (PORT-8).
func TestChangeZoneEffectRejectsUnresolvedShapes(t *testing.T) {
	t.Parallel()

	for _, line := range []string{
		"DB$ ChangeZone | Defined$ Self | Origin$ Battlefield | Destination$ Hand | Transformed$ True",
		"DB$ ChangeZone | Defined$ Self | Origin$ Flashback | Destination$ Hand",
		"DB$ ChangeZone | Defined$ Self | Origin$ Battlefield | Destination$ Command",
		"DB$ ChangeZone | Defined$ Self | Origin$ Battlefield | Destination$ Library | LibraryPosition$ Bogus",
		"DB$ ChangeZone | Origin$ Library | Destination$ Hand | ChangeType$ EACH Creature",
	} {
		g, p, _ := newTwoPlayerGame(t)
		c := engine.NewScriptedController()
		if _, err := castETBChain(t, g, p, etbChainDef(t, "Test Reject", line), c); err == nil {
			t.Errorf("%q: ResolveStack succeeded, want an error", line)
		}
	}
}

// TestChangeZoneEffectMemoryAndShuffle proves the known-origin Memory
// params (ForgetOtherRemembered$, Unimprint$, RememberChanged$, Imprint$)
// and Shuffle$ True shuffling each moved card's owner's library.
func TestChangeZoneEffectMemoryAndShuffle(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	target := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	libraryCards(t, g, other, 3)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})
	def := etbChainDef(t, "Test Tuck Shuffle",
		"DB$ ChangeZone | ValidTgts$ Creature.OppCtrl | Origin$ Battlefield | Destination$ Library | Shuffle$ True | ForgetOtherRemembered$ True | Unimprint$ True | RememberChanged$ True | Imprint$ True")
	host, err := castETBChain(t, g, p, def, c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(target).Zone; z != engine.Library {
		t.Errorf("target zone = %v, want Library", z)
	}
	m := g.Card(host).Memory
	if len(m.Remembered()) != 1 || len(m.Imprinted()) != 1 {
		t.Errorf("remembered %v imprinted %v, want the target in both", m.Remembered(), m.Imprinted())
	}
}

// TestChangeZoneEffectForgetChangedAndGainControlPlayer proves
// ForgetChanged$ and GainControl$ naming a Defined$ player.
func TestChangeZoneEffectForgetChangedAndGainControlPlayer(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	card := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Graveyard)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{card})
	def := etbChainDef(t, "Test Gift",
		"DB$ ChooseCard | ChoiceZone$ Graveyard | Choices$ Creature | Mandatory$ True | RememberChosen$ True | SubAbility$ DBGive",
		"DBGive", "DB$ ChangeZone | Defined$ Remembered | Origin$ Graveyard | Destination$ Battlefield | GainControl$ Opponent | ForgetChanged$ True")
	host, err := castETBChain(t, g, p, def, c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(card).Controller(); got != other {
		t.Errorf("controller = %v, want %v", got, other)
	}
	if n := len(g.Card(host).Memory.Remembered()); n != 0 {
		t.Errorf("remembered = %d, want 0 after ForgetChanged$", n)
	}
}

// TestChangeZoneEffectShuffleNonMandatoryDecline proves a declined
// ShuffleNonMandatory$ skips the whole move.
func TestChangeZoneEffectShuffleNonMandatoryDecline(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(false)
	host, err := castETBChain(t, g, p, etbChainDef(t, "Test SNM", "DB$ ChangeZone | Defined$ Self | Origin$ Battlefield | Destination$ Library | ShuffleNonMandatory$ True"), c)
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if z := g.Card(host).Zone; z != engine.Battlefield {
		t.Errorf("host zone = %v, want Battlefield", z)
	}
}

// TestChangeZoneEffectHiddenShapes proves the hidden-origin variants:
// Defined$ takes the named cards without a choice; ChooseFromDefined$ offers
// them; a non-hidden Hidden$ origin searches every player's zone; a targeted
// player's library is searched; Optional$ may decline; Mandatory$ demands
// the full count.
func TestChangeZoneEffectHiddenShapes(t *testing.T) {
	t.Parallel()

	t.Run("Defined", func(t *testing.T) {
		g, p, _ := newTwoPlayerGame(t)
		c := engine.NewScriptedController()
		host, err := castETBChain(t, g, p, etbChainDef(t, "Test Hidden Defined", "DB$ ChangeZone | Hidden$ True | Defined$ Self | Origin$ Battlefield | Destination$ Hand"), c)
		if err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if z := g.Card(host).Zone; z != engine.Hand {
			t.Errorf("host zone = %v, want Hand", z)
		}
	})
	t.Run("ChooseFromDefined", func(t *testing.T) {
		g, p, _ := newTwoPlayerGame(t)
		c := engine.NewScriptedController()
		c.QueueCardChoice(nil)
		host, err := castETBChain(t, g, p, etbChainDef(t, "Test Hidden Choose", "DB$ ChangeZone | Hidden$ True | ChooseFromDefined$ Self | Origin$ Battlefield | Destination$ Exile"), c)
		if err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if z := g.Card(host).Zone; z != engine.Battlefield {
			t.Errorf("host zone = %v, want Battlefield (nothing chosen)", z)
		}
	})
	t.Run("EveryGraveyard", func(t *testing.T) {
		g, p, other := newTwoPlayerGame(t)
		dead := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Graveyard)
		c := engine.NewScriptedController()
		c.QueueCardChoice([]engine.CardID{dead})
		if _, err := castETBChain(t, g, p, etbChainDef(t, "Test Hidden Yard", "DB$ ChangeZone | Hidden$ True | Origin$ Graveyard | Destination$ Exile | ChangeType$ Creature | Mandatory$ True"), c); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if z := g.Card(dead).Zone; z != engine.Exile {
			t.Errorf("zone = %v, want Exile", z)
		}
	})
	t.Run("TargetedPlayer", func(t *testing.T) {
		g, p, other := newTwoPlayerGame(t)
		theirs := libraryCards(t, g, other, 2)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
		c.QueueCardChoice([]engine.CardID{theirs[0]})
		if _, err := castETBChain(t, g, p, etbChainDef(t, "Test Hidden Tgt", "DB$ ChangeZone | ValidTgts$ Opponent | Origin$ Library | Destination$ Graveyard | ChangeType$ Card"), c); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if z := g.Card(theirs[0]).Zone; z != engine.Graveyard {
			t.Errorf("zone = %v, want Graveyard", z)
		}
	})
	t.Run("OptionalDecline", func(t *testing.T) {
		g, p, _ := newTwoPlayerGame(t)
		libraryCards(t, g, p, 2)
		c := engine.NewScriptedController()
		c.QueueConfirmEffect(false)
		if _, err := castETBChain(t, g, p, etbChainDef(t, "Test Hidden Opt", "DB$ ChangeZone | Origin$ Library | Destination$ Hand | ChangeType$ Card | Optional$ True"), c); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if n := len(g.Zone(engine.Hand, p).Cards()); n != 0 {
			t.Errorf("hand = %d, want 0", n)
		}
	})
	t.Run("DefinedPlayerShuffleTrue", func(t *testing.T) {
		g, p, _ := newTwoPlayerGame(t)
		lib := libraryCards(t, g, p, 3)
		c := engine.NewScriptedController()
		c.QueueCardChoice([]engine.CardID{lib[0], lib[1]})
		if _, err := castETBChain(t, g, p, etbChainDef(t, "Test Hidden Two", "DB$ ChangeZone | DefinedPlayer$ You | Origin$ Library | Destination$ Battlefield | ChangeType$ Creature | ChangeNum$ 2 | Mandatory$ True | Tapped$ True | Shuffle$ True"), c); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if !g.Card(lib[0]).Tapped || g.Card(lib[1]).Zone != engine.Battlefield {
			t.Error("fetched creatures not on the battlefield tapped")
		}
	})
	t.Run("MandatoryShort", func(t *testing.T) {
		g, p, _ := newTwoPlayerGame(t)
		lib := libraryCards(t, g, p, 1)
		c := engine.NewScriptedController()
		c.QueueCardChoice(nil)
		if _, err := castETBChain(t, g, p, etbChainDef(t, "Test Hidden Mand", "DB$ ChangeZone | Origin$ Library | Destination$ Hand | ChangeType$ Card | ChangeNum$ 2 | Mandatory$ True"), c); err == nil {
			t.Errorf("empty answer to a Mandatory$ search accepted; library %v", lib)
		}
	})
}
