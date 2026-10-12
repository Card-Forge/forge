package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// discardWatcher is a ScriptedController that records who decides each
// discard pick and what they were offered, and, at every pick, which of
// watch are still in a hand -- so a test can prove the chooser, the pool and
// that every pick happens before anything is discarded.
type discardWatcher struct {
	*engine.ScriptedController
	watch    []engine.CardID
	deciders []engine.PlayerID
	offers   [][]engine.CardID
	inHand   [][]engine.CardID
}

func (w *discardWatcher) record(g *engine.Game, p engine.PlayerID, offered []engine.CardID) {
	w.deciders = append(w.deciders, p)
	w.offers = append(w.offers, slices.Clone(offered))
	var held []engine.CardID
	for _, id := range w.watch {
		if g.Card(id).Zone == engine.Hand {
			held = append(held, id)
		}
	}
	w.inHand = append(w.inHand, held)
}

func (w *discardWatcher) ChooseCardsForEffect(g *engine.Game, p engine.PlayerID, src engine.CardID, options []engine.CardID, lo, hi int) []engine.CardID {
	w.record(g, p, options)
	return w.ScriptedController.ChooseCardsForEffect(g, p, src, options, lo, hi)
}

func (w *discardWatcher) ChooseCardsToDiscard(g *engine.Game, p engine.PlayerID, hand []engine.CardID, count int) []engine.CardID {
	w.record(g, p, hand)
	return w.ScriptedController.ChooseCardsToDiscard(g, p, hand, count)
}

func newDiscardWatcher(watch ...engine.CardID) *discardWatcher {
	return &discardWatcher{ScriptedController: engine.NewScriptedController(), watch: watch}
}

func wantZone(t *testing.T, g *engine.Game, id engine.CardID, want engine.ZoneType, what string) {
	t.Helper()
	if got := g.Card(id).Zone; got != want {
		t.Errorf("%s zone = %v, want %v", what, got, want)
	}
}

// discardNow is resolveNow (batch1effects_test.go) for any controller: it
// pushes trig's Execute$ ability for a fresh host p controls, with targets,
// and resolves the stack.
func discardNow(t *testing.T, g *engine.Game, p engine.PlayerID, c engine.PlayerController, targets []engine.EntityID, trig string) (engine.CardID, error) {
	t.Helper()
	def := etbChainDef(t, "Test Discard", trig)
	host := g.NewCard(def, p, engine.Battlefield)
	face := def.Faces[0]
	for _, sub := range face.Triggers[0].Subs {
		if strings.EqualFold(sub.Key, "Execute") {
			g.PushAbility(engine.Ability{API: engine.APIDiscard, Source: host, Controller: p, Params: sub.Ability, Amounts: face.Amounts, Targets: targets})
			return host, g.ResolveStack(engine.NewRegistry(), c)
		}
	}
	t.Fatal("no Execute$")
	return engine.NoCard, nil
}

// Mind Rot's shape: the target player, not the caster, picks and discards.
func TestDiscardTargetPlayerDiscardsTheirOwnPick(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	mine := g.NewCard(nil, p, engine.Hand)
	keep := g.NewCard(nil, other, engine.Hand)
	toss := g.NewCard(nil, other, engine.Hand)
	c := newDiscardWatcher()
	c.QueueDiscardChoice([]engine.CardID{toss})

	if _, err := discardNow(t, g, p, c.ScriptedController, []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ Discard | ValidTgts$ Player | NumCards$ 1 | Mode$ TgtChoose"); err != nil {
		t.Fatalf("Discard: %v", err)
	}
	wantZone(t, g, toss, engine.Graveyard, "target's pick")
	wantZone(t, g, keep, engine.Hand, "target's other card")
	wantZone(t, g, mine, engine.Hand, "caster's card")
}

// TargetMin$/TargetMax$ allow several target players: each picks, every pick
// comes before any discard (DiscardEffect.java's discardedMap), and the
// discards then land in player order.
func TestDiscardSeveralTargetsAllPickBeforeAnyDiscards(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	mine := g.NewCard(nil, p, engine.Hand)
	theirs := g.NewCard(nil, other, engine.Hand)
	c := newDiscardWatcher(mine, theirs)
	c.QueueDiscardChoice([]engine.CardID{mine})
	c.QueueDiscardChoice([]engine.CardID{theirs})
	targets := []engine.EntityID{engine.PlayerEntity(p), engine.PlayerEntity(other)}

	if _, err := discardNow(t, g, p, c, targets,
		"DB$ Discard | ValidTgts$ Player | TargetMin$ 0 | TargetMax$ 2 | NumCards$ 1 | Mode$ TgtChoose"); err != nil {
		t.Fatalf("Discard: %v", err)
	}
	if !slices.Equal(c.deciders, []engine.PlayerID{p, other}) {
		t.Errorf("deciders = %v, want [%d %d]", c.deciders, p, other)
	}
	if len(c.inHand) != 2 || len(c.inHand[1]) != 2 {
		t.Errorf("cards in hand at each pick = %v, want both still in hand at the second pick", c.inHand)
	}
	wantZone(t, g, mine, engine.Graveyard, "first target's pick")
	wantZone(t, g, theirs, engine.Graveyard, "second target's pick")
}

// Mode$ Hand discards every card, ordered into the graveyard by their owner.
func TestDiscardHandModeDiscardsEverythingInOwnerOrder(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(nil, other, engine.Hand)
	b := g.NewCard(nil, other, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueCardOrder([]engine.CardID{b, a})

	if _, err := discardNow(t, g, p, c, []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ Discard | ValidTgts$ Opponent | Mode$ Hand"); err != nil {
		t.Fatalf("Discard: %v", err)
	}
	if got := g.Zone(engine.Graveyard, other).Cards(); !slices.Equal(got, []engine.CardID{b, a}) {
		t.Errorf("graveyard = %v, want [%d %d], the owner's order", got, b, a)
	}
}

// Optional$ on Mode$ Hand asks the discarder; a no keeps the hand, and
// RememberDiscardingPlayers$ remembers only players that went ahead --
// including one whose hand was empty (DiscardEffect.java:155-176).
func TestDiscardOptionalHandAndRememberDiscardingPlayers(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	kept := g.NewCard(nil, other, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(true)  // p, empty hand
	c.QueueConfirmEffect(false) // other declines

	host, err := discardNow(t, g, p, c, nil,
		"DB$ Discard | Defined$ Player | Mode$ Hand | Optional$ True | RememberDiscardingPlayers$ True")
	if err != nil {
		t.Fatalf("Discard: %v", err)
	}
	wantZone(t, g, kept, engine.Hand, "declining player's card")
	if got := g.Card(host).Memory.Remembered(); !slices.Equal(got, []engine.EntityID{engine.PlayerEntity(p)}) {
		t.Errorf("remembered = %v, want only the player who accepted", got)
	}
}

// Mode$ Random draws with Aggregates.random's reservoir on the game's own
// stream, so a seeded game picks what the oracle picks.
func TestDiscardRandomModeFollowsTheGameStream(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	hand := []engine.CardID{g.NewCard(nil, other, engine.Hand), g.NewCard(nil, other, engine.Hand), g.NewCard(nil, other, engine.Hand)}

	r := javarand.New(1)
	want := 0
	for i := int32(2); i <= 3; i++ {
		if r.Int32n(i) < 1 {
			want = int(i) - 1
		}
	}

	if _, err := discardNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ Discard | ValidTgts$ Opponent | NumCards$ 1 | Mode$ Random"); err != nil {
		t.Fatalf("Discard: %v", err)
	}
	for i, id := range hand {
		wantIn := engine.Hand
		if i == want {
			wantIn = engine.Graveyard
		}
		wantZone(t, g, id, wantIn, "hand card")
	}
}

// Mode$ Random draws only among DiscardValid$ cards, and Optional$ asks first.
func TestDiscardRandomModeFiltersAndAsks(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	land := g.NewCard(landDef(t, "Test Land", "Land"), other, engine.Hand)
	spell := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(true)

	if _, err := discardNow(t, g, p, c, []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ Discard | ValidTgts$ Opponent | Mode$ Random | DiscardValid$ Card.nonLand | Optional$ True"); err != nil {
		t.Fatalf("Discard: %v", err)
	}
	wantZone(t, g, spell, engine.Graveyard, "nonland card")
	wantZone(t, g, land, engine.Hand, "land card")

	c.QueueConfirmEffect(false)
	if _, err := discardNow(t, g, p, c, []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ Discard | ValidTgts$ Opponent | Mode$ Random | Optional$ True"); err != nil {
		t.Fatalf("Discard: %v", err)
	}
	wantZone(t, g, land, engine.Hand, "land card after a declined random discard")
}

// Amnesia's shape: Mode$ RevealDiscardAll discards every matching card.
func TestDiscardRevealDiscardAllTakesEveryMatch(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	land := g.NewCard(landDef(t, "Test Land", "Land"), other, engine.Hand)
	s1 := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Hand)
	s2 := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueCardOrder([]engine.CardID{s1, s2})

	host, err := discardNow(t, g, p, c, []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ Discard | ValidTgts$ Player | Mode$ RevealDiscardAll | DiscardValid$ Card.nonLand | RememberDiscarded$ True")
	if err != nil {
		t.Fatalf("Discard: %v", err)
	}
	if got, want := g.Card(host).Memory.Remembered(), []engine.EntityID{engine.CardEntity(s1), engine.CardEntity(s2)}; !slices.Equal(got, want) {
		t.Errorf("remembered = %v, want %v", got, want)
	}
	wantZone(t, g, s1, engine.Graveyard, "first nonland")
	wantZone(t, g, s2, engine.Graveyard, "second nonland")
	wantZone(t, g, land, engine.Hand, "land")

	// An empty hand is skipped outright.
	if _, err := discardNow(t, g, p, c, []engine.EntityID{engine.PlayerEntity(p)},
		"DB$ Discard | ValidTgts$ Player | Mode$ RevealDiscardAll"); err != nil {
		t.Fatalf("Discard on an empty hand: %v", err)
	}
}

// Duress's shape: Mode$ RevealYouChoose -- the caster picks out of the
// target's DiscardValid$ cards, and the target discards the pick.
func TestDiscardRevealYouChooseCasterPicksFromValidCards(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	land := g.NewCard(landDef(t, "Test Land", "Land"), other, engine.Hand)
	creature := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Hand)
	c := newDiscardWatcher()
	c.QueueCardChoice([]engine.CardID{creature})

	if _, err := discardNow(t, g, p, c, []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ Discard | ValidTgts$ Opponent | NumCards$ 1 | Mode$ RevealYouChoose | DiscardValid$ Card.nonLand"); err != nil {
		t.Fatalf("Discard: %v", err)
	}
	if !slices.Equal(c.deciders, []engine.PlayerID{p}) {
		t.Errorf("deciders = %v, want the caster %d", c.deciders, p)
	}
	if len(c.offers) != 1 || !slices.Equal(c.offers[0], []engine.CardID{creature}) {
		t.Errorf("offered %v, want only the nonland card", c.offers)
	}
	wantZone(t, g, creature, engine.Graveyard, "picked card")
	wantZone(t, g, land, engine.Hand, "land")
}

// Mode$ RevealYouChoose with no valid card asks nobody.
func TestDiscardChooseModesSkipWhenNothingIsValid(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	land := g.NewCard(landDef(t, "Test Land", "Land"), other, engine.Hand)
	// No queued choice: asking would panic on an exhausted queue.
	if _, err := discardNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ Discard | ValidTgts$ Opponent | NumCards$ 1 | Mode$ RevealYouChoose | DiscardValid$ Card.nonLand"); err != nil {
		t.Fatalf("Discard: %v", err)
	}
	wantZone(t, g, land, engine.Hand, "land")
}

// Extortion's shape: LookYouChoose with Optional$ lets the caster pick up to
// NumCards$, none included.
func TestDiscardLookYouChooseOptionalAllowsNone(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(nil, other, engine.Hand)
	b := g.NewCard(nil, other, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueCardChoice(nil)

	if _, err := discardNow(t, g, p, c, []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ Discard | ValidTgts$ Opponent | Mode$ LookYouChoose | Optional$ True | NumCards$ 2"); err != nil {
		t.Fatalf("Discard: %v", err)
	}
	wantZone(t, g, a, engine.Hand, "first card")
	wantZone(t, g, b, engine.Hand, "second card")
}

// AnyNumber$ lets the chooser take every valid card.
func TestDiscardAnyNumberTakesAsManyAsChosen(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(nil, p, engine.Hand)
	b := g.NewCard(nil, p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{a, b})
	c.QueueCardOrder([]engine.CardID{a, b})

	if _, err := discardNow(t, g, p, c, nil, "DB$ Discard | Defined$ You | Mode$ TgtChoose | AnyNumber$ True"); err != nil {
		t.Fatalf("Discard: %v", err)
	}
	wantZone(t, g, a, engine.Graveyard, "first card")
	wantZone(t, g, b, engine.Graveyard, "second card")
}

// Rakdos Augermage's shape: Mode$ RevealTgtChoose -- Defined$ You discards,
// the target opponent picks.
func TestDiscardRevealTgtChooseTargetPicksForDefined(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	mine := g.NewCard(nil, p, engine.Hand)
	theirs := g.NewCard(nil, other, engine.Hand)
	c := newDiscardWatcher()
	c.QueueCardChoice([]engine.CardID{mine})

	if _, err := discardNow(t, g, p, c, []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ Discard | Defined$ You | ValidTgts$ Opponent | NumCards$ 1 | Mode$ RevealTgtChoose"); err != nil {
		t.Fatalf("Discard: %v", err)
	}
	if !slices.Equal(c.deciders, []engine.PlayerID{other}) {
		t.Errorf("deciders = %v, want the target %d", c.deciders, other)
	}
	wantZone(t, g, mine, engine.Graveyard, "caster's card")
	wantZone(t, g, theirs, engine.Hand, "target's card")
}

// RevealNumber$ has the discarder pick which cards the chooser sees first.
func TestDiscardRevealNumberNarrowsThePool(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(nil, other, engine.Hand)
	b := g.NewCard(nil, other, engine.Hand)
	c := newDiscardWatcher()
	c.QueueCardChoice([]engine.CardID{b}) // other reveals b
	c.QueueCardChoice([]engine.CardID{b}) // p picks b

	if _, err := discardNow(t, g, p, c, []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ Discard | ValidTgts$ Opponent | NumCards$ 1 | RevealNumber$ 1 | Mode$ RevealYouChoose"); err != nil {
		t.Fatalf("Discard: %v", err)
	}
	if !slices.Equal(c.deciders, []engine.PlayerID{other, p}) {
		t.Errorf("deciders = %v, want the revealer then the caster", c.deciders)
	}
	if len(c.offers) != 2 || !slices.Equal(c.offers[1], []engine.CardID{b}) {
		t.Errorf("offers = %v, want the caster offered only the revealed card", c.offers)
	}
	wantZone(t, g, b, engine.Graveyard, "revealed and picked card")
	wantZone(t, g, a, engine.Hand, "unrevealed card")
}

// Mode$ Defined discards DefinedCards$, skipping any no longer in a hand
// (Card.canBeDiscardedBy), and RememberDiscarded$ remembers only the ones
// that moved.
func TestDiscardDefinedCardsSkipsCardsOutOfHand(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	inHand := g.NewCard(nil, p, engine.Hand)
	onField := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := engine.NewScriptedController()

	def := etbChainDef(t, "Test Defined Cards", "DB$ Discard | Mode$ Defined | DefinedCards$ Remembered")
	host := g.NewCard(def, p, engine.Battlefield)
	g.Card(host).Memory.Remember(engine.CardEntity(inHand))
	g.Card(host).Memory.Remember(engine.CardEntity(onField))
	c.QueueCardOrder([]engine.CardID{inHand, onField})
	for _, sub := range def.Faces[0].Triggers[0].Subs {
		if strings.EqualFold(sub.Key, "Execute") {
			g.PushAbility(engine.Ability{API: engine.APIDiscard, Source: host, Controller: p, Params: sub.Ability})
		}
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("Discard: %v", err)
	}
	wantZone(t, g, inHand, engine.Graveyard, "defined card in hand")
	wantZone(t, g, onField, engine.Battlefield, "defined card on the battlefield")
}

// Mode$ Defined with Optional$: a no discards nothing.
func TestDiscardDefinedOptionalDeclined(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(false)
	if _, err := discardNow(t, g, p, c, nil, "DB$ Discard | Mode$ Defined | DefinedCards$ Self | Optional$ True"); err != nil {
		t.Fatalf("Discard: %v", err)
	}
}

// A player who already lost is skipped (DiscardEffect.java:136).
func TestDiscardSkipsAPlayerWhoLost(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	card := g.NewCard(nil, other, engine.Hand)
	g.Player(other).Lost = true
	if _, err := discardNow(t, g, p, engine.NewScriptedController(), nil, "DB$ Discard | Defined$ Opponent | Mode$ Hand"); err != nil {
		t.Fatalf("Discard: %v", err)
	}
	wantZone(t, g, card, engine.Hand, "lost player's card")
}

// Every shape this port cannot resolve fails before anything moves.
func TestDiscardRejectsUnportedShapes(t *testing.T) {
	t.Parallel()

	cases := []struct {
		line    string
		targets bool
		want    string
	}{
		{"DB$ Discard | Defined$ You | Mode$ TgtChoose | NumCards$ 2 | UnlessType$ Land", false, "UnlessType"},
		{"DB$ Discard | Defined$ You | Mode$ Bogus", false, `Mode$ "Bogus"`},
		{"DB$ Discard | Defined$ You | Mode$ RevealDiscardAll | DiscardValid$ Card.SharesColorWith Sacrificed", false, "SharesColorWith"},
		{"DB$ Discard | Defined$ You | Mode$ RevealDiscardAll | DiscardValid$ Card.cmcEQZ", false, "operand"},
		{"DB$ Discard | Defined$ You | Mode$ Defined", false, "DefinedCards"},
		{"DB$ Discard | Defined$ You | Mode$ Random | NumCards$ Bogus", false, "NumCards"},
		{"DB$ Discard | Defined$ You | Mode$ TgtChoose | RevealNumber$ Bogus", false, "RevealNumber"},
		{"DB$ Discard | ValidTgts$ Opponent | Mode$ RevealTgtChoose | Defined$ You", false, "no target"},
		{"DB$ Discard | Defined$ Bogus | Mode$ Hand", false, "Bogus"},
		{"DB$ Discard | Defined$ TriggeredTarget | Mode$ Hand", false, "recorded no target"},
		{"DB$ Discard | Defined$ Bogus | ValidTgts$ Opponent | Mode$ RevealTgtChoose", true, "Bogus"},
	}
	for _, tc := range cases {
		g, p, other := newTwoPlayerGame(t)
		g.NewCard(nil, p, engine.Hand)
		var targets []engine.EntityID
		if tc.targets {
			targets = []engine.EntityID{engine.PlayerEntity(other)}
		}
		_, err := discardNow(t, g, p, engine.NewScriptedController(), targets, tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%q: err = %v, want one naming %s", tc.line, err, tc.want)
		}
	}
}

// DiscardValid$ Card.ChosenColor reads the first chosen color; with two
// chosen, which was first is not kept, so the line fails rather than guess.
func TestDiscardChosenColorOverTwoColorsIsRefused(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(nil, other, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueColorChoice(mana.Red | mana.Green)
	_, err := castETBChain(t, g, p, etbChainDef(t, "Test Two Colors",
		"DB$ ChooseColor | Defined$ You | TwoColors$ True | SubAbility$ DBDiscard",
		"DBDiscard", "DB$ Discard | Defined$ Opponent | Mode$ RevealYouChoose | DiscardValid$ Card.ChosenColor"), c)
	if err == nil || !strings.Contains(err.Error(), "ChosenColor") {
		t.Errorf("err = %v, want one naming ChosenColor", err)
	}
}

// A chooser's answer outside the offered pool or count is an error (GO-7).
func TestDiscardRejectsABadPick(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(nil, other, engine.Hand)
	stray := g.NewCard(nil, p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{stray})
	if _, err := discardNow(t, g, p, c, []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ Discard | ValidTgts$ Opponent | NumCards$ 1 | Mode$ YouChoose"); err == nil {
		t.Error("a pick from outside the pool resolved, want an error")
	}

	c.QueueCardChoice(nil)
	if _, err := discardNow(t, g, p, c, []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ Discard | ValidTgts$ Opponent | NumCards$ 1 | RevealNumber$ 1 | Mode$ YouChoose"); err == nil {
		t.Error("a short reveal resolved, want an error")
	}
}
