package engine_test

import (
	"strconv"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// cdaCorpusFloor is how many of the real corpus's CDA power/toughness
// dimensions (a SetPower$/SetToughness$ on a CharacteristicDefining$ line of a
// card whose printed value is not a plain number) resolve on an otherwise
// empty battlefield. It only ever goes up; port-log/game-state/
// layer7a-cda-amounts.md lists what the remainder needs.
const cdaCorpusFloor = 363

// Every real CDA creature, alone on the battlefield: counts how many of its
// "*" dimensions Layer 7a resolves, so a regression in any amount head shows
// up as a lower count, and a panic on any real shape fails the test outright.
// A +0/+50 anthem keeps a creature whose CDA resolves to 0 toughness on the
// battlefield to be counted; it adds nothing to a dimension Layer 7a left
// unresolved (foldPT only adds to a resolved value).
func TestCharacteristicDefiningCorpusFloor(t *testing.T) {
	t.Parallel()

	db := scenarioDB(t)
	anthem := amountDef(t, "Test Toughness Anthem", "Enchantment", "", "", "",
		[]string{"Mode$ Continuous | Affected$ Creature | AddToughness$ 50"})
	resolved, total := 0, 0
	var missing []string
	for _, name := range db.Names() {
		def, ok := db.Card(name)
		if !ok {
			continue
		}
		face := def.Faces[0]
		var hasP, hasT bool
		for _, s := range face.Statics {
			if _, cda := s.Param("CharacteristicDefining"); !cda {
				continue
			}
			_, p := s.Param("SetPower")
			_, tg := s.Param("SetToughness")
			hasP, hasT = hasP || p, hasT || tg
		}
		_, errP := strconv.Atoi(face.Power)
		_, errT := strconv.Atoi(face.Toughness)
		hasP, hasT = hasP && errP != nil, hasT && errT != nil
		if !hasP && !hasT {
			continue
		}
		g := engine.NewGame(db, javarand.New(1), []string{"a", "b"})
		p := g.Players()[0]
		for _, pid := range g.Players() {
			g.Player(pid).Life = 20
		}
		g.SetTurnState(1, p, engine.Main1)
		g.NewCard(anthem, p, engine.Battlefield)
		id := g.NewCard(def, p, engine.Battlefield)
		engine.CheckStateBasedActions(g, engine.NewScriptedController())
		for _, dim := range []struct {
			has bool
			get func() (int, bool)
		}{{hasP, g.Card(id).Power}, {hasT, g.Card(id).Toughness}} {
			if !dim.has {
				continue
			}
			total++
			if _, ok := dim.get(); ok {
				resolved++
			} else {
				missing = append(missing, name)
			}
		}
	}
	t.Logf("CDA dimensions resolved: %d of %d; unresolved: %s", resolved, total, strings.Join(missing, ", "))
	if resolved < cdaCorpusFloor {
		t.Errorf("CDA dimensions resolved = %d of %d, want at least %d", resolved, total, cdaCorpusFloor)
	}
}

// svarLine is one `SVar:Name:Body` line, in script order.
type svarLine struct{ name, body string }

// amountDef compiles a card through the real pipeline, so compile.Face.
// Amounts and Statics are built exactly as a card script's would be.
func amountDef(t *testing.T, name, typeLine, cost, power, toughness string, statics []string, svars ...svarLine) *compile.Card {
	t.Helper()
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), typeLine)
	if cost != "" {
		raw.Faces[0].ManaCost = mana.MustParse(cost)
	}
	raw.Faces[0].Power, raw.Faces[0].Toughness = power, toughness
	raw.Faces[0].Statics = statics
	for _, s := range svars {
		raw.Faces[0].SVars.Set(s.name, s.body)
	}
	def, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return def
}

// newLiveGame is a two-player game with both players at 20 life, so the
// state-based-action pass these tests drive does not end it first.
func newLiveGame(t *testing.T) *engine.Game {
	t.Helper()
	g := newGame(t, "a", "b")
	for _, p := range g.Players() {
		g.Player(p).Life = 20
	}
	return g
}

// cdaLine is the Layer 7a line every real CDA creature writes.
var cdaLine = []string{"Mode$ Continuous | CharacteristicDefining$ True | SetPower$ X | SetToughness$ Y"}

// cdaDef is a */* creature whose power is X and toughness Y.
func cdaDef(t *testing.T, x, y string, extra ...svarLine) *compile.Card {
	t.Helper()
	svars := append([]svarLine{{"X", x}, {"Y", y}}, extra...)
	return amountDef(t, "Test CDA", "Creature Lhurgoyf", "", "*", "*", cdaLine, svars...)
}

func wantCDAPT(t *testing.T, g *engine.Game, id engine.CardID, power, toughness int, why string) {
	t.Helper()
	c := g.Card(id)
	pw, pok := c.Power()
	tg, tok := c.Toughness()
	if !pok || !tok || pw != power || tg != toughness {
		t.Errorf("P/T = %d/%d (resolved %v/%v), want %d/%d -- %s", pw, tg, pok, tok, power, toughness, why)
	}
}

// Tarmogoyf: power is the number of card types among cards in all
// graveyards, toughness that plus one -- tarmogoyf.txt's own
// `Count$ValidGraveyard Card$CardTypes` and `SVar$X/Plus.1`. Empty graveyards
// make it 0/1 (handlePaid's empty-list 0, then the outer Plus.1); a creature
// in one graveyard and an artifact creature plus an instant in the other make
// three distinct types, counted once each.
func TestCharacteristicDefiningTarmogoyfCountsCardTypesAcrossGraveyards(t *testing.T) {
	t.Parallel()

	g := newLiveGame(t)
	p, opp := g.Players()[0], g.Players()[1]
	goyf := g.NewCard(cdaDef(t, "Count$ValidGraveyard Card$CardTypes", "SVar$X/Plus.1"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	wantCDAPT(t, g, goyf, 0, 1, "no cards in any graveyard")

	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)
	g.NewCard(amountDef(t, "Test Construct", "Artifact Creature Construct", "", "1", "1", nil), opp, engine.Graveyard)
	g.NewCard(amountDef(t, "Test Bolt", "Instant", "R", "", "", nil), opp, engine.Graveyard)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	wantCDAPT(t, g, goyf, 3, 4, "creature, artifact and instant across both graveyards")
}

// Devotion counts colored symbols among the mana costs of permanents the
// controller controls: a hybrid symbol counts, generic mana does not, an
// opponent's permanents do not, and a Mode$ Devotion static (Altar of the
// Pantheon) adds its own Value$ (default 1) for the player it names.
func TestCharacteristicDefiningDevotionCountsSymbolsAndDevotionMod(t *testing.T) {
	t.Parallel()

	g := newLiveGame(t)
	p, opp := g.Players()[0], g.Players()[1]
	god := g.NewCard(cdaDef(t, "Count$Devotion.Black", "Count$Devotion.Black"), p, engine.Battlefield)
	g.NewCard(creatureDefManaCost(t, "2 B B"), p, engine.Battlefield)
	g.NewCard(creatureDefManaCost(t, "B/G G"), p, engine.Battlefield)
	g.NewCard(creatureDefManaCost(t, "B B B"), opp, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	wantCDAPT(t, g, god, 3, 3, "{B}{B} and the {B/G} hybrid; the opponent's symbols do not count")

	altar := []string{"Mode$ Devotion | ValidPlayer$ You | Description$ Your devotion is increased by one."}
	g.NewCard(amountDef(t, "Test Altar", "Artifact", "3", "", "", altar), p, engine.Battlefield)
	g.NewCard(amountDef(t, "Test Opposing Altar", "Artifact", "3", "", "", altar), opp, engine.Battlefield)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	wantCDAPT(t, g, god, 4, 4, "the controller's own altar adds one; the opponent's names only its own controller")
}

// Domain counts the basic land types among the controller's lands, each
// type once however many lands carry it.
func TestCharacteristicDefiningDomainCountsBasicLandTypes(t *testing.T) {
	t.Parallel()

	g := newLiveGame(t)
	p, opp := g.Players()[0], g.Players()[1]
	kavu := g.NewCard(cdaDef(t, "Count$Domain", "Count$Domain"), p, engine.Battlefield)
	g.NewCard(landDef(t, "Plains", "Basic Land Plains"), p, engine.Battlefield)
	g.NewCard(landDef(t, "Plains", "Basic Land Plains"), p, engine.Battlefield)
	g.NewCard(landDef(t, "Test Dual", "Land Island Swamp"), p, engine.Battlefield)
	g.NewCard(landDef(t, "Forest", "Basic Land Forest"), opp, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	wantCDAPT(t, g, kavu, 3, 3, "Plains, Island, Swamp; the opponent's Forest does not count")
}

// The player- and host-measuring heads real CDA creatures write, one table.
func TestCharacteristicDefiningPlayerAndHostHeads(t *testing.T) {
	t.Parallel()

	for _, tt := range []struct {
		name        string
		x, y        string
		extra       []svarLine
		setup       func(g *engine.Game, p, opp engine.PlayerID, host engine.CardID)
		power, tghn int
	}{
		{
			name: "YourLifeTotal", x: "Count$YourLifeTotal", y: "Count$YourLifeTotal",
			setup: func(g *engine.Game, p, opp engine.PlayerID, _ engine.CardID) {
				g.Player(p).Life, g.Player(opp).Life = 17, 5
			},
			power: 17, tghn: 17,
		},
		{
			// roiling_horror.txt: YourLifeTotal/Minus.Z, Z itself an SVar
			// naming OppGreatestLifeTotal -- the operand resolved in turn.
			name: "Roiling Horror's life difference", x: "Count$YourLifeTotal/Minus.Z", y: "Count$YourLifeTotal/Minus.Z",
			extra: []svarLine{{"Z", "Count$OppGreatestLifeTotal"}},
			setup: func(g *engine.Game, p, opp engine.PlayerID, _ engine.CardID) {
				g.Player(p).Life, g.Player(opp).Life = 23, 18
			},
			power: 5, tghn: 5,
		},
		{
			name: "CardCounters on the host", x: "Count$CardCounters.TIME", y: "Count$CardCounters.TIME/Twice",
			setup: func(g *engine.Game, _, _ engine.PlayerID, host engine.CardID) {
				g.Card(host).Counters.Add(engine.Time, 3)
			},
			power: 3, tghn: 6,
		},
		{
			name: "NumInAllHands", x: "Count$NumInAllHands", y: "Count$NumInAllHands",
			setup: func(g *engine.Game, p, opp engine.PlayerID, _ engine.CardID) {
				g.NewCard(creatureDef(t), p, engine.Hand)
				g.NewCard(creatureDef(t), opp, engine.Hand)
				g.NewCard(creatureDef(t), opp, engine.Hand)
			},
			power: 3, tghn: 3,
		},
		{
			name: "Chroma and ChromaInGrave", x: "Count$Chroma.Green", y: "Count$ChromaInGrave.Green",
			setup: func(g *engine.Game, p, opp engine.PlayerID, _ engine.CardID) {
				g.NewCard(creatureDefManaCost(t, "G G"), p, engine.Battlefield)
				g.NewCard(creatureDefManaCost(t, "G G G"), p, engine.Graveyard)
				g.NewCard(creatureDefManaCost(t, "G"), opp, engine.Graveyard)
			},
			power: 2, tghn: 3,
		},
		{
			name: "Shapeshifter's ChosenNumber and Number$7/Minus.X", x: "Count$ChosenNumber", y: "Number$7/Minus.X",
			setup: func(g *engine.Game, _, _ engine.PlayerID, host engine.CardID) {
				g.Card(host).Memory.SetChosenNumber(5)
			},
			power: 5, tghn: 2,
		},
		{
			name: "HighestCardsInHand among opponents", x: "PlayerCountOpponents$HighestCardsInHand", y: "Number$1",
			setup: func(g *engine.Game, p, opp engine.PlayerID, _ engine.CardID) {
				g.NewCard(creatureDef(t), p, engine.Hand)
				g.NewCard(creatureDef(t), p, engine.Hand)
				g.NewCard(creatureDef(t), p, engine.Hand)
				g.NewCard(creatureDef(t), opp, engine.Hand)
			},
			power: 1, tghn: 1,
		},
		{
			name: "GreatestCardManaCost", x: "Count$Valid Creature.YouCtrl$GreatestCardManaCost", y: "Count$Valid Creature.YouCtrl$CardManaCost",
			setup: func(g *engine.Game, p, opp engine.PlayerID, _ engine.CardID) {
				g.NewCard(creatureDefManaCost(t, "3 G"), p, engine.Battlefield)
				g.NewCard(creatureDefManaCost(t, "1 G"), p, engine.Battlefield)
				g.NewCard(creatureDefManaCost(t, "7"), opp, engine.Battlefield)
			},
			power: 4, tghn: 6,
		},
		{
			name: "CardCounters.P1P1 summed over lands", x: "Count$Valid Land.YouCtrl$CardCounters.P1P1", y: "Count$Valid Land.YouCtrl$Amount",
			setup: func(g *engine.Game, p, _ engine.PlayerID, _ engine.CardID) {
				a := g.NewCard(landDef(t, "Forest", "Basic Land Forest"), p, engine.Battlefield)
				b := g.NewCard(landDef(t, "Forest", "Basic Land Forest"), p, engine.Battlefield)
				g.Card(a).Counters.Add(engine.P1P1, 2)
				g.Card(b).Counters.Add(engine.P1P1, 1)
			},
			power: 3, tghn: 2,
		},
	} {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()
			g := newLiveGame(t)
			p, opp := g.Players()[0], g.Players()[1]
			g.Player(p).Life, g.Player(opp).Life = 20, 20
			host := g.NewCard(cdaDef(t, tt.x, tt.y, tt.extra...), p, engine.Battlefield)
			tt.setup(g, p, opp, host)
			engine.CheckStateBasedActions(g, engine.NewScriptedController())
			wantCDAPT(t, g, host, tt.power, tt.tghn, tt.x+" / "+tt.y)
		})
	}
}

// Every doXMath operator, against Number$7 so the arithmetic is the only
// thing measured. Operand Z is an SVar (the calculateAmount fallback), 2.
// HalfUp/HalfDown round a negative the way Java's Math.ceil/floor do, which
// is not Go's truncating division.
func TestAmountOperatorsMatchDoXMath(t *testing.T) {
	t.Parallel()

	for _, tt := range []struct {
		body string
		want int
	}{
		{"Number$7/Plus.2", 9},
		{"Number$7/Plus.Z", 9},
		{"Number$7/NMinus.10", 3},
		{"Number$7/Minus.Z", 5},
		{"Number$7/Twice", 14},
		{"Number$7/Thrice", 21},
		{"Number$7/HalfUp", 4},
		{"Number$7/HalfDown", 3},
		{"Number$-7/HalfUp", -3},
		{"Number$-7/HalfDown", -4},
		{"Number$7/ThirdUp", 3},
		{"Number$7/ThirdDown", 2},
		{"Number$7/Negative", -7},
		{"Number$7/Times.Z", 14},
		{"Number$7/Pow.Z", 49},
		{"Number$7/DivideEvenlyUp.2", 4},
		{"Number$7/DivideEvenlyDown.2", 3},
		{"Number$7/DivideEvenlyDown.0", 0},
		{"Number$7/Mod.4", 3},
		{"Number$-7/Abs", 7},
		{"Number$7/LimitMax.5", 5},
		{"Number$7/LimitMin.9", 9},
		{"Number$7/Wibble", 7},
		{"Number$7/Plus.1.2", 7},
		{"-Number$7/Plus.1", -8},
		{"SVar$Z/Twice", 4},
	} {
		t.Run(tt.body, func(t *testing.T) {
			t.Parallel()
			g := newLiveGame(t)
			p := g.Players()[0]
			host := g.NewCard(amountDef(t, "Test Math", "Creature Elf", "", "*", "*",
				[]string{"Mode$ Continuous | CharacteristicDefining$ True | SetPower$ X | SetToughness$ Z"},
				svarLine{"X", tt.body}, svarLine{"Z", "2"}), p, engine.Battlefield)
			engine.CheckStateBasedActions(g, engine.NewScriptedController())
			if pw, ok := g.Card(host).Power(); !ok || pw != tt.want {
				t.Errorf("Power() = (%d, %v), want (%d, true)", pw, ok, tt.want)
			}
		})
	}
}

// Shapes that must stay unresolved rather than produce a number: Mod by zero
// (Java throws), a head no evaluator exists for, a context prefix, a Mode$
// Devotion line carrying a condition this does not evaluate, and an
// unrecognized distinct property.
func TestAmountUnresolvableShapesStaySkipped(t *testing.T) {
	t.Parallel()

	for _, tt := range []struct {
		name   string
		body   string
		static string
	}{
		{name: "Mod by zero", body: "Number$7/Mod.0"},
		{name: "Party", body: "Count$Party"},
		{name: "YourTurns", body: "Count$YourTurns"},
		{name: "context prefix", body: "CastSA>Count$YourLifeTotal"},
		{name: "ExiledWith", body: "ExiledWith$CardPower"},
		{name: "DifferentCardNames", body: "Count$Valid Land.YouCtrl$DifferentCardNames"},
		{name: "Greatest CardPower", body: "Count$Valid Creature$GreatestCardPower"},
		{name: "colorless devotion", body: "Count$Devotion.Colorless"},
		{
			name:   "conditional Mode$ Devotion",
			body:   "Count$Devotion.Black",
			static: "Mode$ Devotion | ValidPlayer$ You | Condition$ PlayerTurn",
		},
	} {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()
			g := newLiveGame(t)
			p := g.Players()[0]
			host := g.NewCard(cdaDef(t, tt.body, tt.body), p, engine.Battlefield)
			if tt.static != "" {
				g.NewCard(amountDef(t, "Test Static", "Artifact", "", "", "", []string{tt.static}), p, engine.Battlefield)
			}
			engine.CheckStateBasedActions(g, engine.NewScriptedController())
			if pw, ok := g.Card(host).Power(); ok {
				t.Errorf("Power() = (%d, true), want unresolvable", pw)
			}
		})
	}
}

// Winter, Misanthropic Guide's Layer 8 line shares the amount path: each
// opponent's maximum hand size is seven minus the card types in its
// controller's graveyard (Number$7/Minus.X over Count$ValidGraveyard
// Card.YouOwn$CardTypes), once Delirium holds.
func TestAmountDrivesWinterMaximumHandSize(t *testing.T) {
	t.Parallel()

	g := newLiveGame(t)
	p, opp := g.Players()[0], g.Players()[1]
	g.NewCard(amountDef(t, "Test Winter", "Legendary Creature Human Warlock", "", "3", "4",
		[]string{"Mode$ Continuous | Condition$ Delirium | Affected$ Opponent | SetMaxHandSize$ Y"},
		svarLine{"X", "Count$ValidGraveyard Card.YouOwn$CardTypes"}, svarLine{"Y", "Number$7/Minus.X"}), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)
	g.NewCard(amountDef(t, "Test Relic", "Artifact", "1", "", "", nil), p, engine.Graveyard)
	g.NewCard(amountDef(t, "Test Bolt", "Instant", "R", "", "", nil), p, engine.Graveyard)
	g.NewCard(landDef(t, "Forest", "Basic Land Forest"), p, engine.Graveyard)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if limit, ok := g.Player(opp).HandSizeLimit(7); !ok || limit != 3 {
		t.Errorf("opponent HandSizeLimit = (%d, %v), want (3, true) -- seven minus four card types", limit, ok)
	}
	if limit, ok := g.Player(p).HandSizeLimit(7); !ok || limit != 7 {
		t.Errorf("controller HandSizeLimit = (%d, %v), want (7, true) -- Affected$ Opponent only", limit, ok)
	}
}
