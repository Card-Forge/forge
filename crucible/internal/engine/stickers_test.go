package engine_test

import (
	"slices"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
)

// stickerRecorder records the options each ChooseSticker call was offered and
// answers with the next scripted index.
type stickerRecorder struct {
	*engine.ScriptedController
	offered [][]engine.StickerOption
}

func (r *stickerRecorder) ChooseSticker(g *engine.Game, d engine.PlayerID, src, target engine.CardID, options []engine.StickerOption, optional bool) int {
	r.offered = append(r.offered, slices.Clone(options))
	return r.ScriptedController.ChooseSticker(g, d, src, target, options, optional)
}

// stickerGame is a two-player game where p owns the real Unstable Robot Dragon
// sheet (ten stickers: three Name, three Art, two Ability, two PT costing 2 and
// 3 tickets) with the given tickets, and a vanilla creature on the battlefield.
func stickerGame(t *testing.T, tickets int) (g *engine.Game, p engine.PlayerID, creature engine.CardID) {
	t.Helper()
	g, p, _ = newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Unstable Robot Dragon"), p, engine.StickerSheets)
	g.Player(p).Counters.Add(engine.Ticket, tickets)
	creature = g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	return g, p, creature
}

const putPTSticker = "DB$ PutSticker | Choices$ Creature.YouOwn | Optional$ True | Kind$ PT"

// A PT sticker sets the creature's power and toughness, costs its tickets at
// resolution, and still applies after later state-based passes rebuild the
// continuous effects (ADR-0044).
func TestPutStickerPTSetsBaseStatsAndPaysTickets(t *testing.T) {
	t.Parallel()

	g, p, creature := stickerGame(t, 5)
	c := &stickerRecorder{ScriptedController: engine.NewScriptedController()}
	c.QueueCardChoice([]engine.CardID{creature})
	c.QueueSticker(1) // STK10, 2/7 for 3 tickets
	if err := resolveWith(t, g, p, c, putPTSticker); err != nil {
		t.Fatal(err)
	}
	sba(g)
	sba(g)

	if pw, _ := g.Card(creature).Power(); pw != 2 {
		t.Errorf("power = %d, want 2", pw)
	}
	if tg, _ := g.Card(creature).Toughness(); tg != 7 {
		t.Errorf("toughness = %d, want 7", tg)
	}
	if got := g.Player(p).Counters.Count(engine.Ticket); got != 2 {
		t.Errorf("tickets = %d, want 2", got)
	}
	if got := g.Card(creature).Stickers(); len(got) != 1 || got[0].Slot != "STK10" {
		t.Errorf("stickers = %v, want STK10 only", got)
	}
}

func slots(options []engine.StickerOption) []string {
	var out []string
	for _, o := range options {
		out = append(out, o.Slot)
	}
	return out
}

// The offered list is the implemented, unused stickers the tickets cover, of
// the asked kind, in sheet order; a sticker already placed on a card in play
// is gone from it (CR 123.3a, StickerSheet.collectAvailable).
func TestPutStickerOffersOnlyAffordableUnusedStickers(t *testing.T) {
	t.Parallel()

	g, p, creature := stickerGame(t, 2)
	c := &stickerRecorder{ScriptedController: engine.NewScriptedController()}
	c.QueueCardChoice([]engine.CardID{creature})
	c.QueueCardChoice([]engine.CardID{creature})
	c.QueueSticker(0)
	c.QueueSticker(-1)
	if err := resolveWith(t, g, p, c, putPTSticker); err != nil {
		t.Fatal(err)
	}
	if err := resolveWith(t, g, p, c, putPTSticker); err != nil {
		t.Fatal(err)
	}
	if got := slots(c.offered[0]); !slices.Equal(got, []string{"STK9"}) {
		t.Errorf("first offer = %v, want STK9 only (STK10 costs 3 of 2 tickets)", got)
	}
	if len(c.offered) != 1 {
		t.Errorf("ChooseSticker called %d times, want 1: no PT sticker is affordable once the tickets are spent", len(c.offered))
	}
	if got := g.Player(p).Counters.Count(engine.Ticket); got != 0 {
		t.Errorf("tickets = %d, want 0", got)
	}
}

// NoTicketCost places any sticker without paying; MaxTickets caps the cost of
// what is offered (PutStickerEffect.java:80-84, :124).
func TestPutStickerNoTicketCostAndMaxTickets(t *testing.T) {
	t.Parallel()

	g, p, creature := stickerGame(t, 0)
	c := &stickerRecorder{ScriptedController: engine.NewScriptedController()}
	c.QueueCardChoice([]engine.CardID{creature})
	c.QueueSticker(0)
	if err := resolveWith(t, g, p, c, putPTSticker+" | NoTicketCost$ True"); err != nil {
		t.Fatal(err)
	}
	if got := slots(c.offered[0]); !slices.Equal(got, []string{"STK9", "STK10"}) {
		t.Errorf("free offer = %v, want both PT stickers", got)
	}
	if pw, _ := g.Card(creature).Power(); pw != 3 {
		t.Errorf("power = %d, want 3", pw)
	}

	g, p, creature = stickerGame(t, 9)
	c = &stickerRecorder{ScriptedController: engine.NewScriptedController()}
	c.QueueCardChoice([]engine.CardID{creature})
	c.QueueSticker(-1)
	if err := resolveWith(t, g, p, c, "DB$ PutSticker | Choices$ Creature.YouOwn | Optional$ True | MaxTickets$ 2"); err != nil {
		t.Fatal(err)
	}
	for _, o := range c.offered[0] {
		if o.Tickets > 2 {
			t.Errorf("offered %s at %d tickets past MaxTickets 2", o.Slot, o.Tickets)
		}
	}
	if len(g.Card(creature).Stickers()) != 0 {
		t.Error("a declined optional sticker was placed")
	}
}

// A mandatory placement refuses an answer of none, and a bad index is an
// error, never a panic (GO-7).
func TestPutStickerRejectsAnAnswerOutsideTheOffer(t *testing.T) {
	t.Parallel()

	for _, answer := range []int{-1, 99} {
		g, p, creature := stickerGame(t, 5)
		c := engine.NewScriptedController()
		c.QueueCardChoice([]engine.CardID{creature})
		c.QueueSticker(answer)
		if err := resolveWith(t, g, p, c, "DB$ PutSticker | Choices$ Creature.YouOwn | Kind$ PT"); err == nil {
			t.Errorf("answer %d was accepted", answer)
		}
	}
}

// Stickers stay on a card that moves between public zones and drop off one that
// moves into a hidden zone (GameAction.java:268-270).
func TestStickersSurvivePublicZonesAndDropInHiddenOnes(t *testing.T) {
	t.Parallel()

	g, p, creature := stickerGame(t, 5)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{creature})
	c.QueueSticker(0)
	if err := resolveWith(t, g, p, c, putPTSticker); err != nil {
		t.Fatal(err)
	}
	g.Move(creature, engine.Graveyard, p)
	if len(g.Card(creature).Stickers()) != 1 {
		t.Error("the graveyard dropped the sticker")
	}
	g.Move(creature, engine.Battlefield, p)
	sba(g)
	if pw, _ := g.Card(creature).Power(); pw != 3 {
		t.Errorf("power back on the battlefield = %d, want 3", pw)
	}
	g.Move(creature, engine.Hand, p)
	if len(g.Card(creature).Stickers()) != 0 {
		t.Error("the hand kept the sticker")
	}
}

// Mode$ StickerPlaced fires for the owner with the stickered card, once per
// sticker, filtered by ValidCard$ and StickerKind$ (TriggerStickerPlaced).
func TestStickerPlacedTriggerFiltersByKind(t *testing.T) {
	t.Parallel()

	g, p, creature := stickerGame(t, 5)
	g.NewCard(scriptDef(t, "Watcher", "Enchantment",
		"T:Mode$ StickerPlaced | ValidPlayer$ You | ValidCard$ Creature | StickerKind$ PT,Ability | Execute$ TrigGain | TriggerZones$ Battlefield",
		"SVar:TrigGain:DB$ GainLife | Defined$ You | LifeAmount$ 3"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{creature})
	c.QueueCardChoice([]engine.CardID{creature})
	c.QueueSticker(0)
	c.QueueSticker(0)
	if err := resolveWith(t, g, p, c, putPTSticker); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life after a PT sticker = %d, want 23", got)
	}
	// An Art sticker is not in the trigger's StickerKind$ list.
	if err := resolveWith(t, g, p, c, "DB$ PutSticker | Choices$ Creature.YouOwn | Optional$ True | Kind$ Art"); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life after an Art sticker = %d, want still 23", got)
	}
}

// Defined$ targets only cards the activator owns, and ChoiceZone$ reads
// another zone.
func TestPutStickerChoiceZoneAndOwnership(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Unstable Robot Dragon"), p, engine.StickerSheets)
	g.Player(p).Counters.Add(engine.Ticket, 5)
	mine := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)
	g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Graveyard)
	c := &stickerRecorder{ScriptedController: engine.NewScriptedController()}
	c.QueueCardChoice([]engine.CardID{mine})
	c.QueueSticker(0)
	if err := resolveWith(t, g, p, c, "DB$ PutSticker | ChoiceZone$ Graveyard | Choices$ Creature | Optional$ True | Kind$ PT"); err != nil {
		t.Fatal(err)
	}
	if len(g.Card(mine).Stickers()) != 1 {
		t.Error("the graveyard creature got no sticker")
	}
}

// Name stickers apply in timestamp order: each fills the leftmost blank still
// empty (CR 123.6a), with none left it goes in after the chosen number of words
// (Card.addStickerWord, Card.java:4699-4709). The name is the same after the
// next pass rebuilt it.
func TestNameStickersFillBlanksThenInsertAtThePosition(t *testing.T) {
	t.Parallel()

	g, p, _ := stickerGame(t, 0)
	bear := g.NewCard(scriptDef(t, "Bear", "Creature"), p, engine.Battlefield)
	blank := g.NewCard(scriptDef(t, "Gary __ Smith", "Creature"), p, engine.Battlefield)
	place := func(target engine.CardID, answer, position int) {
		t.Helper()
		c := engine.NewScriptedController()
		c.QueueCardChoice([]engine.CardID{target})
		c.QueueSticker(answer)
		c.QueueStickerNamePosition(position)
		if err := resolveWith(t, g, p, c, "DB$ PutSticker | Choices$ Creature.YouOwn | Optional$ True | Kind$ Name"); err != nil {
			t.Fatal(err)
		}
		sba(g)
		sba(g)
	}
	place(bear, 0, 1) // Unstable after "Bear"
	if got := g.Card(bear).Name(); got != "Bear Unstable" {
		t.Errorf("name = %q, want %q", got, "Bear Unstable")
	}
	place(bear, 0, 0) // Robot first of the remaining two, at the front
	if got := g.Card(bear).Name(); got != "Robot Bear Unstable" {
		t.Errorf("name = %q, want %q", got, "Robot Bear Unstable")
	}
	place(blank, 0, 5) // Dragon is the first of the stickers left: fills the blank
	if got := g.Card(blank).Name(); got != "Gary Dragon Smith" {
		t.Errorf("name = %q, want %q", got, "Gary Dragon Smith")
	}
}

// An Ability sticker's keywords are on the card for as long as the sticker is,
// through every rebuild of the keyword layer (AppliedSticker.grantAbility).
func TestAbilityStickerGrantsItsKeywords(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Unsanctioned Ancient Juggler"), p, engine.StickerSheets)
	g.Player(p).Counters.Add(engine.Ticket, 4)
	creature := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	c := &stickerRecorder{ScriptedController: engine.NewScriptedController()}
	c.QueueCardChoice([]engine.CardID{creature})
	c.QueueSticker(1)
	if err := resolveWith(t, g, p, c, "DB$ PutSticker | Choices$ Creature.YouOwn | Optional$ True | Kind$ Ability"); err != nil {
		t.Fatal(err)
	}
	if got := slots(c.offered[0]); !slices.Equal(got, []string{"STK7", "STK8"}) {
		t.Fatalf("offered %v, want STK7 and STK8", got)
	}
	sba(g)
	sba(g)
	if !g.Card(creature).HasKeyword("Indestructible") {
		t.Errorf("keywords = %v, want Indestructible", g.Card(creature).KeywordLines())
	}
	g.Move(creature, engine.Hand, p)
	sba(g)
	if g.Card(creature).HasKeyword("Indestructible") {
		t.Error("the keyword outlived the sticker")
	}
}

// place puts the sticker at index answer of the offered list on target with a
// Choices$ PutSticker of the given kind, then runs the layers twice.
func placeSticker(t *testing.T, g *engine.Game, p engine.PlayerID, target engine.CardID, kind string, answer int, svars ...string) {
	t.Helper()
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{target})
	c.QueueSticker(answer)
	c.QueueStickerNamePosition(0)
	line := "DB$ PutSticker | Choices$ Creature.YouOwn | Optional$ True | Kind$ " + kind
	if err := resolveWith(t, g, p, c, line, svars...); err != nil {
		t.Fatal(err)
	}
	sba(g)
	sba(g)
}

// Count$CardStickers reads the host's own stickers: all of them, a kind, or the
// Name stickers by word (AbilityUtils.countStickers).
func TestCardStickersCountReadsTheHostsStickers(t *testing.T) {
	t.Parallel()

	g, p, _ := stickerGame(t, 0)
	host := g.NewCard(scriptDef(t, "Counter", "Creature",
		"S:Mode$ Continuous | Affected$ Card.Self | AddPower$ X | AddToughness$ Y",
		"SVar:X:Count$CardStickers.NameMinLetters.6",
		"SVar:Y:Count$CardStickers.Name"), p, engine.Battlefield)
	for range 3 {
		placeSticker(t, g, p, host, "Name", 0)
	}
	// Unstable and Dragon have six letters or more, Robot has five.
	if pw, _ := g.Card(host).Power(); pw != 3 {
		t.Errorf("power = %d, want 1 + 2", pw)
	}
	if tg, _ := g.Card(host).Toughness(); tg != 4 {
		t.Errorf("toughness = %d, want 1 + 3 Name stickers", tg)
	}
}

// Count$Valid ...$StickerPower sums the PT stickers over the matching cards.
func TestStickerPowerSumsPTStickersOverMatchingCards(t *testing.T) {
	t.Parallel()

	g, p, creature := stickerGame(t, 5)
	host := g.NewCard(scriptDef(t, "Summer", "Creature",
		"S:Mode$ Continuous | Affected$ Card.Self | AddPower$ X",
		"SVar:X:Count$Valid Creature.Other+YouCtrl$StickerPower"), p, engine.Battlefield)
	placeSticker(t, g, p, creature, "PT", 0) // 3/2 for 2 tickets
	if pw, _ := g.Card(host).Power(); pw != 4 {
		t.Errorf("power = %d, want 1 + 3", pw)
	}
}

// stickered and stickeredWith <kind> select cards by what is on them.
func TestStickeredValidPropertiesSelectByKind(t *testing.T) {
	t.Parallel()

	g, p, bare := stickerGame(t, 5)
	marked := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	placeSticker(t, g, p, marked, "PT", 0)
	for _, tc := range []struct {
		spec   string
		pick   engine.CardID
		placed bool // a sticker went on marked
		err    bool // the pick was outside the pool
	}{
		{"Creature.stickered", marked, true, false},
		{"Creature.stickered", bare, false, true},
		{"Creature.stickeredWith PT", marked, true, false},
		// The pool is empty (marked carries no Name sticker): nothing is asked.
		{"Creature.stickeredWith Name", marked, false, false},
	} {
		before := len(g.Card(marked).Stickers())
		c := engine.NewScriptedController()
		c.QueueCardChoice([]engine.CardID{tc.pick})
		c.QueueSticker(0)
		err := resolveWith(t, g, p, c, "DB$ PutSticker | Choices$ "+tc.spec+" | Kind$ Art")
		if (err != nil) != tc.err {
			t.Errorf("%s: err = %v, want error %v", tc.spec, err, tc.err)
		}
		if placed := len(g.Card(marked).Stickers()) > before; placed != tc.placed {
			t.Errorf("%s: sticker placed = %v, want %v", tc.spec, placed, tc.placed)
		}
	}
}

// StickersPlaced and StickerUniqueVowels are readable by the sub-abilities of the
// PutSticker that wrote them (PutStickerEffect.java:100-101, :131, :134).
func TestPutStickerWritesItsPlacementSVars(t *testing.T) {
	t.Parallel()

	g, p, creature := stickerGame(t, 0)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{creature})
	c.QueueSticker(0) // Unstable: U, A, E
	c.QueueStickerNamePosition(0)
	if err := resolveWith(t, g, p, c,
		"DB$ PutSticker | Choices$ Creature.YouOwn | Optional$ True | Kind$ Name | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ StickerUniqueVowels | ConditionCheckSVar$ StickersPlaced | ConditionSVarCompare$ GE1"); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23", got)
	}
}

// GainsStickerAbilitiesOf$ gives the host the keywords of the Ability stickers on
// the cards it names (Clandestine Chameleon, Pin Collection).
func TestGainsStickerAbilitiesOfTakesKeywordsFromOtherCards(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Unsanctioned Ancient Juggler"), p, engine.StickerSheets)
	g.Player(p).Counters.Add(engine.Ticket, 4)
	host := g.NewCard(scriptDef(t, "Chameleon", "Creature",
		"S:Mode$ Continuous | Affected$ Card.Self | GainsStickerAbilitiesOf$ Creature.YouOwn+Other"), p, engine.Battlefield)
	other := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	sba(g)
	if g.Card(host).HasKeyword("Indestructible") {
		t.Fatal("the host has the keyword before any sticker exists")
	}
	placeSticker(t, g, p, other, "Ability", 1)
	if !g.Card(host).HasKeyword("Indestructible") {
		t.Errorf("host keywords = %v, want Indestructible from the other creature's sticker", g.Card(host).KeywordLines())
	}
}

// A player keeps three of the deck's sheets, a reservoir sample of the game's own
// generator (Aggregates.random): the same seed seats the same sheets, and a deck
// with three or fewer keeps them all in order.
func TestSeatStickerSheetsKeepsThreeAtRandomFromTheGameSeed(t *testing.T) {
	t.Parallel()

	names := []string{"Unstable Robot Dragon", "Unsanctioned Ancient Juggler", "Unglued Pea-Brained Dinosaur", "Ancestral Hot Dog Minotaur", "Urza's Dark Cannonball"}
	seat := func(n int) []string {
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		var defs []*compile.Card
		for _, name := range names[:n] {
			defs = append(defs, corpusCard(t, name))
		}
		var got []string
		for _, id := range g.SeatStickerSheets(p, defs) {
			if g.Card(id).Zone != engine.StickerSheets {
				t.Errorf("sheet %v is in %v, want StickerSheets", id, g.Card(id).Zone)
			}
			got = append(got, g.Card(id).Name())
		}
		return got
	}
	first, second := seat(5), seat(5)
	if len(first) != 3 || !slices.Equal(first, second) {
		t.Errorf("seated %v then %v, want the same three", first, second)
	}
	if got := seat(2); !slices.Equal(got, names[:2]) {
		t.Errorf("seated %v, want %v", got, names[:2])
	}
}
