package fixture_test

import (
	"math"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/fixture"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

func runActions(t *testing.T, l *fixture.Loaded, c *engine.ScriptedController, log string) error {
	t.Helper()
	return fixture.RunActions(strings.NewReader(log), l, c)
}

// landDB builds a one-card database like testDB, except this card carries a
// real type line. tapformana needs Card.Type() to answer "does this have a
// basic land type", which testDB's own vanilla cards never set -- testDB's
// own doc comment says Load never reads it, so building it there would test
// something Load itself does not care about.
func landDB(t *testing.T, name, typeLine string) *compile.DB {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{
		Filename: name,
		Faces:    [carddb.NumFaces]carddb.Face{{Present: true, Name: name, Type: cardtype.Parse(reg, typeLine)}},
	}
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return compile.NewDB(map[string]*compile.Card{name: c})
}

// permanentDB is landDB's own shape, plus a real mana cost -- castspell
// needs both Card.Type() (castableAsPermanent) and Def.Faces[0].ManaCost
// (PayManaCost), neither of which testDB's vanilla cards carry.
func permanentDB(t *testing.T, name, typeLine, cost string) *compile.DB {
	t.Helper()
	return castTestDB(t, permanentCard{name: name, typeLine: typeLine, cost: cost})
}

// permanentCard is one castTestDB entry: a type line and a mana cost, the
// two fields castspell reads off a card that landDB's own one-field shape
// (type line only) does not carry.
type permanentCard struct {
	name, typeLine, cost string
	keywords             []string
}

// castTestDB builds a database of real-type-line, real-mana-cost cards --
// landDB's own shape, extended to more than one card at once, since a
// castspell scenario needs the spell and the land(s) paying for it in the
// same database.
func castTestDB(t *testing.T, cards ...permanentCard) *compile.DB {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	db := make(map[string]*compile.Card, len(cards))
	for _, card := range cards {
		raw := &carddb.Card{
			Filename: card.name,
			Faces: [carddb.NumFaces]carddb.Face{{
				Present:  true,
				Name:     card.name,
				Type:     cardtype.Parse(reg, card.typeLine),
				ManaCost: mana.MustParse(card.cost),
				Keywords: card.keywords,
			}},
		}
		c, err := compile.Compile(raw)
		if err != nil {
			t.Fatalf("compile %q: %v", card.name, err)
		}
		db[card.name] = c
	}
	return compile.NewDB(db)
}

func TestRunActionsStartTurnAndAdvance(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "startturn human\nadvance\nadvance 2\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if l.Game.Turn() != 1 {
		t.Errorf("turn %d, want 1", l.Game.Turn())
	}
	// Untap (startturn) -> Upkeep (advance) -> Draw -> Main1 (advance 2).
	if l.Game.ActivePhase() != engine.Main1 {
		t.Errorf("phase %v, want Main1", l.Game.ActivePhase())
	}
}

// Comments and blank lines are noise, the same convention setup.state uses.
func TestRunActionsSkipsCommentsAndBlankLines(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\n")
	c := engine.NewScriptedController()

	err := runActions(t, l, c, "# a comment\n\nstartturn human\n\n# another\n")
	if err != nil {
		t.Fatalf("RunActions: %v", err)
	}
	if l.Game.Turn() != 1 {
		t.Errorf("turn %d, want 1", l.Game.Turn())
	}
}

// dealopeninghands deals a real seven-card hand from a real library --
// queue startingplayer answers ChooseStartingPlayer's own CR 103.2 coin
// flip, the one decision DealOpeningHands asks before shuffling and
// dealing.
func TestRunActionsDealOpeningHandsDealsSevenCards(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Mountain")
	l := load(t, db, "humanlife=20\nailife=20\n"+
		"humanlibrary=Mountain|Id:1;Mountain|Id:2;Mountain|Id:3;Mountain|Id:4;"+
		"Mountain|Id:5;Mountain|Id:6;Mountain|Id:7;Mountain|Id:8;Mountain|Id:9;Mountain|Id:10\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue startingplayer human\ndealopeninghands\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	human := l.Game.Players()[0]
	if got := l.Game.Zone(engine.Hand, human).Len(); got != 7 {
		t.Errorf("human's hand has %d cards, want 7", got)
	}
	if got := l.Game.Zone(engine.Library, human).Len(); got != 3 {
		t.Errorf("human's library has %d cards, want 3", got)
	}
}

// A full mulligan exchange, scripted end to end: queue the decisions and the
// tuck before the action that consumes them, the same order
// ScriptedController expects. Id: on every hand card is what lets
// `queue tuck` name one without knowing which CardID a shuffle puts where --
// the hand is a closed pool of seven known ids the whole time.
func TestRunActionsMulliganWithQueuedKeepHand(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Mountain")
	l := load(t, db, "humanlife=20\nailife=20\n"+
		"humanhand=Mountain|Id:1;Mountain|Id:2;Mountain|Id:3;Mountain|Id:4;"+
		"Mountain|Id:5;Mountain|Id:6;Mountain|Id:7\n")
	c := engine.NewScriptedController()

	err := runActions(t, l, c, ""+
		"queue keephand false\n"+ // human: mulligan
		"queue keephand true\n"+ // ai: keep (asked in the same round)
		"queue tuck 1\n"+ // the one card human's mulligan will tuck
		"queue keephand true\n"+ // human: keep the redrawn hand
		"mulligan human\n")
	if err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if got := l.Game.Zone(engine.Hand, l.Game.Players()[0]).Len(); got != 6 {
		t.Errorf("human's hand has %d cards, want 6 (one non-free mulligan)", got)
	}
	if id, ok := l.CardByFixtureID[1]; !ok || l.Game.Card(id).Zone != engine.Library {
		t.Error("the tucked card is not in the library")
	}
}

// queue tuck resolves setup.state's Id: numbers to the CardIDs Load actually
// assigned, so a scenario can name a specific card without knowing its
// handle ahead of time.
func TestRunActionsQueueTuckResolvesFixtureID(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Mountain")
	l := load(t, db, "humanlife=20\nailife=20\nhumanbattlefield=Mountain|Id:7\n")
	want, ok := l.CardByFixtureID[7]
	if !ok {
		t.Fatal("setup: Id:7 did not resolve to a CardID")
	}
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue tuck 7\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	got := c.TuckCardsViaMulligan(l.Game, l.Game.Players()[0], nil, 1)
	if len(got) != 1 || got[0] != want {
		t.Errorf("tucked %v, want [%v]", got, want)
	}
}

// queue tuck accepts more than one id, comma-separated.
func TestRunActionsQueueTuckMultipleIDs(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Mountain", "Forest")
	l := load(t, db, "humanlife=20\nailife=20\nhumanbattlefield=Mountain|Id:1;Forest|Id:2\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue tuck 1,2\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	got := c.TuckCardsViaMulligan(l.Game, l.Game.Players()[0], nil, 2)
	want := []engine.CardID{l.CardByFixtureID[1], l.CardByFixtureID[2]}
	if len(got) != 2 || got[0] != want[0] || got[1] != want[1] {
		t.Errorf("tucked %v, want %v", got, want)
	}
}

// queue legendarykeep resolves setup.state's Id: number the same way tuck
// does, for the one card ChooseLegendaryToKeep should return.
func TestRunActionsQueueLegendaryKeepResolvesFixtureID(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Mountain")
	l := load(t, db, "humanlife=20\nailife=20\nhumanbattlefield=Mountain|Id:7\n")
	want, ok := l.CardByFixtureID[7]
	if !ok {
		t.Fatal("setup: Id:7 did not resolve to a CardID")
	}
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue legendarykeep 7\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	got := c.ChooseLegendaryToKeep(l.Game, l.Game.Players()[0], nil)
	if got != want {
		t.Errorf("legendary to keep = %v, want %v", got, want)
	}
}

// queue legendarykeep takes exactly one id -- unlike tuck, there is only
// ever one permanent to keep.
func TestRunActionsQueueLegendaryKeepWantsExactlyOneID(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Mountain", "Forest")
	l := load(t, db, "humanlife=20\nailife=20\nhumanbattlefield=Mountain|Id:1;Forest|Id:2\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue legendarykeep 1,2\n"); err == nil {
		t.Error("two ids did not error")
	}
}

func TestRunActionsQueueLegendaryKeepBadIDErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue legendarykeep abc\n"); err == nil {
		t.Error("a non-numeric id did not error")
	}
}

// declareattackers runs with nothing on the battlefield without error --
// there is nothing eligible, so Game.DeclareCombatAttackers never touches
// the controller's queue at all.
func TestRunActionsDeclareAttackersWithNothingOnTheBattlefield(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "startturn human\ndeclareattackers\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}
}

// queue attackers resolves setup.state's Id: numbers the same way tuck and
// legendarykeep do.
func TestRunActionsQueueAttackersResolvesFixtureIDs(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Mountain", "Forest")
	l := load(t, db, "humanlife=20\nailife=20\nhumanbattlefield=Mountain|Id:1;Forest|Id:2\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue attackers 1,2\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	got := c.DeclareCombatAttackers(l.Game, l.Game.Players()[0], nil)
	want := []engine.CardID{l.CardByFixtureID[1], l.CardByFixtureID[2]}
	if len(got) != 2 || got[0] != want[0] || got[1] != want[1] {
		t.Errorf("attackers %v, want %v", got, want)
	}
}

// queue attackers none is how a scenario queues "decline to attack" --
// there being no ids is not the same as the line being absent, since every
// other queue kind also requires a value.
func TestRunActionsQueueAttackersNoneDeclines(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue attackers none\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	got := c.DeclareCombatAttackers(l.Game, l.Game.Players()[0], nil)
	if got != nil {
		t.Errorf("attackers = %v, want nil", got)
	}
}

func TestRunActionsQueueAttackersBadIDErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue attackers abc\n"); err == nil {
		t.Error("a non-numeric id did not error")
	}
}

// declareblockers runs with no attackers declared without error -- there is
// nothing to block, so Game.DeclareCombatBlockers never touches the
// controller's queue at all.
func TestRunActionsDeclareBlockersWithNoAttackers(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "startturn human\ndeclareblockers\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}
}

// queue blocks resolves each blocker=attacker pair's setup.state Id:
// numbers the same way tuck and legendarykeep resolve theirs.
func TestRunActionsQueueBlocksResolvesFixtureIDs(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Mountain", "Forest")
	l := load(t, db, "humanlife=20\nailife=20\nhumanbattlefield=Mountain|Id:1;Forest|Id:2\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue blocks 1=2\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	got := c.DeclareCombatBlockers(l.Game, l.Game.Players()[0], nil, nil)
	want := engine.Block{Blocker: l.CardByFixtureID[1], Attacker: l.CardByFixtureID[2]}
	if len(got) != 1 || got[0] != want {
		t.Errorf("blocks %v, want [%v]", got, want)
	}
}

// queue blocks accepts more than one pair, comma-separated -- gang blocking
// (CR 509.1c).
func TestRunActionsQueueBlocksMultiplePairs(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Mountain", "Forest", "Island")
	l := load(t, db, "humanlife=20\nailife=20\nhumanbattlefield=Mountain|Id:1;Forest|Id:2;Island|Id:3\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue blocks 1=3,2=3\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	got := c.DeclareCombatBlockers(l.Game, l.Game.Players()[0], nil, nil)
	want := []engine.Block{
		{Blocker: l.CardByFixtureID[1], Attacker: l.CardByFixtureID[3]},
		{Blocker: l.CardByFixtureID[2], Attacker: l.CardByFixtureID[3]},
	}
	if len(got) != 2 || got[0] != want[0] || got[1] != want[1] {
		t.Errorf("blocks %v, want %v", got, want)
	}
}

// queue blocks none is how a scenario queues "decline to block" -- the same
// convention queue attackers none uses.
func TestRunActionsQueueBlocksNoneDeclines(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue blocks none\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	got := c.DeclareCombatBlockers(l.Game, l.Game.Players()[0], nil, nil)
	if got != nil {
		t.Errorf("blocks = %v, want nil", got)
	}
}

func TestRunActionsQueueBlocksBadIDErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue blocks abc=def\n"); err == nil {
		t.Error("non-numeric ids did not error")
	}
}

func TestRunActionsQueueBlocksMissingEqualsErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue blocks 1\n"); err == nil {
		t.Error("a pair with no attacker half did not error")
	}
}

// firststrikedamage runs with no attackers declared without error --
// there is nothing to deal, so Game.DealFirstStrikeDamage never touches the
// controller's queue at all.
func TestRunActionsFirstStrikeDamageWithNoAttackers(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "startturn human\nfirststrikedamage\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}
}

// combatdamage runs with no attackers declared without error -- there is
// nothing to deal, so Game.DealCombatDamage never touches the controller's
// queue at all.
func TestRunActionsCombatDamageWithNoAttackers(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "startturn human\ncombatdamage\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}
}

// queue damage resolves each blocker=amount pair's setup.state Id: number
// the same way queue blocks resolves its pairs, keeping the pair order the
// line was written in.
func TestRunActionsQueueDamageResolvesFixtureIDs(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Mountain", "Forest")
	l := load(t, db, "humanlife=20\nailife=20\nhumanbattlefield=Mountain|Id:1;Forest|Id:2\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue damage 1=3,2=2\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	got := c.AssignCombatDamage(l.Game, l.Game.Players()[0], 0, nil)
	want := []engine.DamageAssignment{
		{Blocker: l.CardByFixtureID[1], Amount: 3},
		{Blocker: l.CardByFixtureID[2], Amount: 2},
	}
	if len(got) != 2 || got[0] != want[0] || got[1] != want[1] {
		t.Errorf("damage assignment %v, want %v", got, want)
	}
}

func TestRunActionsQueueDamageBadIDErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue damage abc=3\n"); err == nil {
		t.Error("a non-numeric id did not error")
	}
}

func TestRunActionsQueueDamageBadAmountErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Mountain")
	l := load(t, db, "humanlife=20\nhumanbattlefield=Mountain|Id:1\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue damage 1=xyz\n"); err == nil {
		t.Error("a non-numeric amount did not error")
	}
}

func TestRunActionsQueueDamageMissingEqualsErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue damage 1\n"); err == nil {
		t.Error("a pair with no amount half did not error")
	}
}

// queue attacktarget accepts a seated player's name.
func TestRunActionsQueueAttackTargetResolvesAPlayerName(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue attacktarget ai\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	got := c.ChooseAttackTarget(l.Game, l.Game.Players()[0], 0, nil)
	want := engine.PlayerEntity(l.Game.Players()[1])
	if got != want {
		t.Errorf("attack target = %v, want %v", got, want)
	}
}

// queue attacktarget accepts a setup.state Id: number, resolving to the
// CardID Load assigned it, for a planeswalker or battle target.
func TestRunActionsQueueAttackTargetResolvesFixtureID(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Mountain")
	l := load(t, db, "humanlife=20\nailife=20\nhumanbattlefield=Mountain|Id:7\n")
	want, ok := l.CardByFixtureID[7]
	if !ok {
		t.Fatal("setup: Id:7 did not resolve to a CardID")
	}
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue attacktarget 7\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	got := c.ChooseAttackTarget(l.Game, l.Game.Players()[0], 0, nil)
	if got != engine.CardEntity(want) {
		t.Errorf("attack target = %v, want CardEntity(%v)", got, want)
	}
}

func TestRunActionsQueueAttackTargetBadPlayerNameErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue attacktarget nobody\n"); err == nil {
		t.Error("an unseated player name did not error")
	}
}

func TestRunActionsQueueAttackTargetUnknownFixtureIDErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue attacktarget 99\n"); err == nil {
		t.Error("an Id: with no matching card did not error")
	}
}

// queue discard resolves setup.state's Id: numbers the same way tuck does.
func TestRunActionsQueueDiscardResolvesFixtureIDs(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Mountain", "Forest")
	l := load(t, db, "humanlife=20\nailife=20\nhumanhand=Mountain|Id:1;Forest|Id:2\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue discard 1,2\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	got := c.DiscardToHandSize(l.Game, l.Game.Players()[0], nil, 2)
	want := []engine.CardID{l.CardByFixtureID[1], l.CardByFixtureID[2]}
	if len(got) != 2 || got[0] != want[0] || got[1] != want[1] {
		t.Errorf("discard %v, want %v", got, want)
	}
}

func TestRunActionsQueueDiscardBadIDErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue discard abc\n"); err == nil {
		t.Error("a non-numeric id did not error")
	}
}

// queue cardchoice answers an effect's ChooseCardsForEffect with the cards
// setup.state's Id: numbers name.
func TestRunActionsQueueCardChoiceResolvesFixtureIDs(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Mountain", "Forest")
	l := load(t, db, "humanlife=20\nailife=20\nhumanlibrary=Mountain|Id:1;Forest|Id:2\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue cardchoice 2,1\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	got := c.ChooseCardsForEffect(l.Game, l.Game.Players()[0], engine.NoCard, nil, 0, 2)
	want := []engine.CardID{l.CardByFixtureID[2], l.CardByFixtureID[1]}
	if len(got) != 2 || got[0] != want[0] || got[1] != want[1] {
		t.Errorf("card choice %v, want %v", got, want)
	}
	if err := runActions(t, l, c, "queue cardchoice abc\n"); err == nil {
		t.Error("a non-numeric id did not error")
	}
}

// queue targets answers a triggered ability's ChooseTargets with the cards
// setup.state's Id: numbers name.
func TestRunActionsQueueTargetsResolvesFixtureIDs(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Mountain", "Forest")
	l := load(t, db, "humanlife=20\nailife=20\nhumanbattlefield=Mountain|Id:1;Forest|Id:2\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue targets 2,1\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	got := c.ChooseTargets(l.Game, l.Game.Players()[0], nil, 1, 2)
	want := []engine.EntityID{engine.CardEntity(l.CardByFixtureID[2]), engine.CardEntity(l.CardByFixtureID[1])}
	if len(got) != 2 || got[0] != want[0] || got[1] != want[1] {
		t.Errorf("targets %v, want %v", got, want)
	}
}

func TestRunActionsQueueTargetsBadIDErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue targets abc\n"); err == nil {
		t.Error("a non-numeric id did not error")
	}
}

// queue battleprotector accepts a seated player's name, the same vocabulary
// queue startingplayer uses.
func TestRunActionsQueueBattleProtectorResolvesAPlayerName(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue battleprotector ai\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	got := c.ChooseBattleProtector(l.Game, l.Game.Players()[0], 0, nil)
	want := l.Game.Players()[1]
	if got != want {
		t.Errorf("battle protector = %v, want %v", got, want)
	}
}

func TestRunActionsQueueBattleProtectorBadPlayerNameErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue battleprotector nobody\n"); err == nil {
		t.Error("an unseated player name did not error")
	}
}

// paymanacost pays straight from the pool manapool= already loaded, with one
// queue paygeneric per unit of the cost's generic amount.
func TestRunActionsPayManaCostSucceeds(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nhumanmanapool=W W\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue paygeneric W\nqueue paygeneric W\npaymanacost human 2\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	p := l.Game.Players()[0]
	if got, want := l.Game.Player(p).ManaPool.Breakdown(), [6]int{}; got != want {
		t.Errorf("mana pool = %v, want %v (both white spent on generic)", got, want)
	}
}

// A payment the pool cannot cover fails silently, the same as calling
// Game.PayManaCost directly does -- the verb does not assert success, and
// the pool is left exactly as manapool= loaded it.
func TestRunActionsPayManaCostFailureLeavesPoolUnchanged(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	// A pure colored pip, no generic amount -- PayManaCost never reaches
	// ChoosePayGeneric for this cost, so no queue paygeneric is needed to
	// reach the pool's own insufficient-mana failure.
	if err := runActions(t, l, c, "paymanacost human W\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	p := l.Game.Players()[0]
	if got, want := l.Game.Player(p).ManaPool.Breakdown(), [6]int{}; got != want {
		t.Errorf("mana pool = %v, want %v (a failed payment spends nothing)", got, want)
	}
}

func TestRunActionsPayManaCostMalformedCostErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "paymanacost human not-a-cost\n"); err == nil {
		t.Error("a malformed cost did not error")
	}
}

func TestRunActionsPayManaCostTooFewArgsErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "paymanacost human\n"); err == nil {
		t.Error("a paymanacost with no cost did not error")
	}
}

func TestRunActionsQueuePayGenericResolvesAShard(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue paygeneric C\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if got, want := c.ChoosePayGeneric(l.Game, l.Game.Players()[0]), mana.ShardC; got != want {
		t.Errorf("paygeneric answer = %v, want %v", got, want)
	}
}

func TestRunActionsQueuePayGenericBadShardErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue paygeneric ZZ\n"); err == nil {
		t.Error("an unparseable shard did not error")
	}
}

func TestRunActionsQueuePayXResolvesAnInt(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue payx 3\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if got, want := c.ChoosePayX(l.Game, l.Game.Players()[0], mana.Cost{}), 3; got != want {
		t.Errorf("payx answer = %d, want %d", got, want)
	}
}

func TestRunActionsQueuePayXBadIntErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue payx abc\n"); err == nil {
		t.Error("an unparseable int did not error")
	}
}

func TestRunActionsQueuePaySnowResolvesAShard(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue paysnow W\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if got, want := c.ChoosePaySnow(l.Game, l.Game.Players()[0]), mana.ShardW; got != want {
		t.Errorf("paysnow answer = %v, want %v", got, want)
	}
}

func TestRunActionsQueuePaySnowBadShardErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue paysnow ZZ\n"); err == nil {
		t.Error("an unparseable shard did not error")
	}
}

func TestRunActionsQueueHybridManaColorResolvesAColor(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue hybridmanacolor U\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if got, want := c.ChooseHybridManaColor(l.Game, l.Game.Players()[0], 0), mana.Blue; got != want {
		t.Errorf("hybridmanacolor answer = %v, want %v", got, want)
	}
}

func TestRunActionsQueueHybridManaColorBadColorErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue hybridmanacolor ZZ\n"); err == nil {
		t.Error("an unparseable color did not error")
	}
}

func TestRunActionsQueuePayMonocoloredHybridResolvesABool(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue paymonocoloredhybrid true\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if got := c.ChoosePayMonocoloredHybrid(l.Game, l.Game.Players()[0], 0, 2); !got {
		t.Error("paymonocoloredhybrid answer = false, want true")
	}
}

func TestRunActionsQueuePayMonocoloredHybridBadBoolErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue paymonocoloredhybrid maybe\n"); err == nil {
		t.Error("an unparseable bool did not error")
	}
}

func TestRunActionsQueuePayColorlessHybridResolvesABool(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue paycolorlesshybrid false\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if got := c.ChoosePayColorlessHybrid(l.Game, l.Game.Players()[0], 0); got {
		t.Error("paycolorlesshybrid answer = true, want false")
	}
}

func TestRunActionsQueuePayColorlessHybridBadBoolErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue paycolorlesshybrid maybe\n"); err == nil {
		t.Error("an unparseable bool did not error")
	}
}

func TestRunActionsQueuePayPhyrexianResolvesABool(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue payphyrexian false\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if got := c.ChoosePayPhyrexian(l.Game, l.Game.Players()[0], 0); got {
		t.Error("payphyrexian answer = true, want false")
	}
}

func TestRunActionsQueuePayPhyrexianBadBoolErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue payphyrexian maybe\n"); err == nil {
		t.Error("an unparseable bool did not error")
	}
}

func TestRunActionsQueuePayHybridPhyrexianResolvesAColor(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue payhybridphyrexian G\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if got, want := c.ChoosePayHybridPhyrexian(l.Game, l.Game.Players()[0], 0), mana.Green; got != want {
		t.Errorf("payhybridphyrexian answer = %v, want %v", got, want)
	}
}

// "life" is the zero mana.Colors answer -- the third option a color letter
// cannot spell, the same convention ChoosePayHybridPhyrexian's own doc
// comment (control.go) already uses.
func TestRunActionsQueuePayHybridPhyrexianLifeAnswersZero(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue payhybridphyrexian life\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if got := c.ChoosePayHybridPhyrexian(l.Game, l.Game.Players()[0], 0); got != 0 {
		t.Errorf("payhybridphyrexian life answer = %v, want the zero mana.Colors", got)
	}
}

func TestRunActionsQueuePayHybridPhyrexianBadColorErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue payhybridphyrexian ZZ\n"); err == nil {
		t.Error("an unparseable color did not error")
	}
}

func TestRunActionsUnknownVerbErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "castspell human\n"); err == nil {
		t.Error("an unknown verb ran without error")
	}
}

func TestRunActionsUnknownQueueKindErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue targetplayer human\n"); err == nil {
		t.Error("an unknown queue kind ran without error")
	}
}

func TestRunActionsUnknownPlayerErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "startturn nobody\n"); err == nil {
		t.Error("an unseated player name ran without error")
	}
}

func TestRunActionsMulliganUnknownPlayerErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "mulligan nobody\n"); err == nil {
		t.Error("mulligan naming an unseated player ran without error")
	}
}

func TestRunActionsQueueStartingPlayerUnknownPlayerErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue startingplayer nobody\n"); err == nil {
		t.Error("queue startingplayer naming an unseated player ran without error")
	}
}

func TestRunActionsMalformedAdvanceCountErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "advance many\n"); err == nil {
		t.Error("a non-numeric advance count ran without error")
	}
}

func TestRunActionsMalformedKeepHandErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue keephand maybe\n"); err == nil {
		t.Error("a non-boolean keephand value ran without error")
	}
}

func TestRunActionsMalformedStartingHandErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue startinghand first\n"); err == nil {
		t.Error("a non-numeric startinghand index ran without error")
	}
}

func TestRunActionsTuckMalformedIDErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue tuck abc\n"); err == nil {
		t.Error("a non-numeric tuck id ran without error")
	}
}

func TestRunActionsTuckUnknownIDErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue tuck 99\n"); err == nil {
		t.Error("a tuck id naming no card ran without error")
	}
}

func TestRunActionsQueueTooFewArgsErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "queue keephand\n"); err == nil {
		t.Error("a queue line with no value ran without error")
	}
}

func TestRunActionsQueueStartingPlayerAndHand(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\n")
	c := engine.NewScriptedController()

	err := runActions(t, l, c, "queue startingplayer ai\nqueue startinghand 1\n")
	if err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if got := c.ChooseStartingPlayer(l.Game, l.Game.Players()[0], true); got != l.Game.Players()[1] {
		t.Errorf("starting player %v, want ai", got)
	}
	if got := c.ChooseStartingHand(l.Game, l.Game.Players()[0], nil); got != 1 {
		t.Errorf("starting hand index %d, want 1", got)
	}
}

// A line naming too few fields for its verb is a fixture-authoring error --
// the same as everywhere else in this package, that has to fail loud rather
// than panic on a short slice or silently do nothing.
func TestRunActionsStartTurnWithNoPlayerErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "startturn\n"); err == nil {
		t.Error("startturn with no player ran without error")
	}
}

// tapformana resolves a player, a setup.state Id: number, and a bare color
// letter, and hands them straight to Game.TapLandForMana -- a real Plains
// from the corpus, not a synthetic def, so this exercises the actual basic
// land type on the actual card the compiler produced.
func TestRunActionsTapForManaAddsColorAndTaps(t *testing.T) {
	t.Parallel()

	db := landDB(t, "Plains", "Basic Land Plains")
	l := load(t, db, "humanlife=20\nailife=20\nhumanbattlefield=Plains|Id:1\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "tapformana human 1 W\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	p := l.Game.Players()[0]
	if got, want := l.Game.Player(p).ManaPool.Breakdown(), [6]int{1, 0, 0, 0, 0, 0}; got != want {
		t.Errorf("mana pool = %v, want one white", got)
	}
	if !l.Game.Card(l.CardByFixtureID[1]).Tapped {
		t.Error("Plains not tapped after tapformana")
	}
}

// A failed tap -- here, asking a Plains for blue -- is declined by the
// rules, not a fixture error: the verb does not assert success, the same
// as paymanacost.
func TestRunActionsTapForManaFailureLeavesPoolAndCardUnchanged(t *testing.T) {
	t.Parallel()

	db := landDB(t, "Plains", "Basic Land Plains")
	l := load(t, db, "humanlife=20\nailife=20\nhumanbattlefield=Plains|Id:1\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "tapformana human 1 U\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	p := l.Game.Players()[0]
	if got, want := l.Game.Player(p).ManaPool.Total(), 0; got != want {
		t.Errorf("mana pool total = %d, want %d (a failed tap adds nothing)", got, want)
	}
	if l.Game.Card(l.CardByFixtureID[1]).Tapped {
		t.Error("Plains tapped despite the failed request")
	}
}

func TestRunActionsTapForManaUnknownCardIDErrors(t *testing.T) {
	t.Parallel()

	db := landDB(t, "Plains", "Basic Land Plains")
	l := load(t, db, "humanlife=20\nailife=20\nhumanbattlefield=Plains|Id:1\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "tapformana human 99 W\n"); err == nil {
		t.Error("an id absent from setup.state did not error")
	}
}

func TestRunActionsTapForManaBadColorErrors(t *testing.T) {
	t.Parallel()

	db := landDB(t, "Plains", "Basic Land Plains")
	l := load(t, db, "humanlife=20\nailife=20\nhumanbattlefield=Plains|Id:1\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "tapformana human 1 ZZ\n"); err == nil {
		t.Error("an unparseable color did not error")
	}
}

func TestRunActionsTapForManaTooFewArgsErrors(t *testing.T) {
	t.Parallel()

	db := landDB(t, "Plains", "Basic Land Plains")
	l := load(t, db, "humanlife=20\nailife=20\nhumanbattlefield=Plains|Id:1\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "tapformana human 1\n"); err == nil {
		t.Error("tapformana with no color did not error")
	}
}

// playland resolves a player and a setup.state Id: number the same way
// tapformana does, and hands them straight to Game.PlayLand.
func TestRunActionsPlayLandMovesCardToBattlefield(t *testing.T) {
	t.Parallel()

	db := landDB(t, "Plains", "Basic Land Plains")
	l := load(t, db, "humanlife=20\nailife=20\nhumanhand=Plains|Id:1\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "startturn human\nadvance 3\nplayland human 1\n"); err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if l.Game.Card(l.CardByFixtureID[1]).Zone != engine.Battlefield {
		t.Error("Plains not on the battlefield after playland")
	}
	if got := l.Game.Player(l.Game.Players()[0]).LandsPlayed; got != 1 {
		t.Errorf("LandsPlayed = %d, want 1", got)
	}
}

// A declined play -- here, a second land the same turn -- is not a fixture
// error: the verb does not assert success, the same as tapformana.
func TestRunActionsPlayLandFailureLeavesCardInHand(t *testing.T) {
	t.Parallel()

	db := landDB(t, "Plains", "Basic Land Plains")
	l := load(t, db, "humanlife=20\nailife=20\nhumanhand=Plains|Id:1;Plains|Id:2\n")
	c := engine.NewScriptedController()

	err := runActions(t, l, c, "startturn human\nadvance 3\nplayland human 1\nplayland human 2\n")
	if err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if l.Game.Card(l.CardByFixtureID[2]).Zone != engine.Hand {
		t.Error("second Plains left hand despite the declined play")
	}
}

func TestRunActionsPlayLandUnknownCardIDErrors(t *testing.T) {
	t.Parallel()

	db := landDB(t, "Plains", "Basic Land Plains")
	l := load(t, db, "humanlife=20\nailife=20\nhumanhand=Plains|Id:1\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "playland human 99\n"); err == nil {
		t.Error("an id absent from setup.state did not error")
	}
}

func TestRunActionsPlayLandTooFewArgsErrors(t *testing.T) {
	t.Parallel()

	db := landDB(t, "Plains", "Basic Land Plains")
	l := load(t, db, "humanlife=20\nailife=20\nhumanhand=Plains|Id:1\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "playland human\n"); err == nil {
		t.Error("playland with no card id did not error")
	}
}

// castspell resolves a player and a setup.state Id: number the same way
// playland does, and hands them straight to Game.CastSpell; resolvestack
// then resolves it through Game.ResolveStack(engine.NewRegistry(), controller).
// The mana comes from a real Forest tapped after reaching Main1, not
// setup.state's own manapool= -- emptyManaPools (CR 500.4) clears any
// preloaded pool on the very first startturn/advance, the same trap a
// scenario would hit, so the fixture-level test exercises the same path a
// scenario needs to.
func TestRunActionsCastSpellAndResolveStackMovesCardToBattlefield(t *testing.T) {
	t.Parallel()

	db := castTestDB(t,
		permanentCard{name: "Grizzly Bears", typeLine: "Creature Bear", cost: "G"},
		permanentCard{name: "Forest", typeLine: "Basic Land Forest", cost: "no cost"},
	)
	l := load(t, db, "humanlife=20\nailife=20\nhumanhand=Grizzly Bears|Id:1\nhumanbattlefield=Forest|Id:2\n")
	c := engine.NewScriptedController()

	err := runActions(t, l, c, "startturn human\nadvance 3\ntapformana human 2 G\ncastspell human 1\nresolvestack\n")
	if err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if l.Game.Card(l.CardByFixtureID[1]).Zone != engine.Battlefield {
		t.Error("Grizzly Bears not on the battlefield after castspell/resolvestack")
	}
}

// castspell's own Aura branch: a lone eligible target needs no
// `queue enchanttarget` at all (CastSpell's own "nothing meaningful to
// decide" reasoning), and resolvestack attaches it through attachEffect.
func TestRunActionsCastSpellAuraAttachesToChosenTarget(t *testing.T) {
	t.Parallel()

	db := castTestDB(t,
		permanentCard{name: "Test Ward", typeLine: "Enchantment Aura", cost: "W", keywords: []string{"Enchant:Creature"}},
		permanentCard{name: "Plains", typeLine: "Basic Land Plains", cost: "no cost"},
		permanentCard{name: "Silvercoat Lion", typeLine: "Creature Cat", cost: "1 W"},
	)
	l := load(t, db,
		"humanlife=20\nailife=20\nhumanhand=Test Ward|Id:1\n"+
			"humanbattlefield=Plains|Id:2;Silvercoat Lion|Id:3\n")
	c := engine.NewScriptedController()

	err := runActions(t, l, c, "startturn human\nadvance 3\ntapformana human 2 W\ncastspell human 1\nresolvestack\n")
	if err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	aura := l.Game.Card(l.CardByFixtureID[1])
	if aura.Zone != engine.Battlefield {
		t.Fatal("Test Ward not on the battlefield after castspell/resolvestack")
	}
	host, ok := aura.AttachedTo()
	if !ok || host != l.CardByFixtureID[3] {
		t.Errorf("Test Ward attached to %v, %v, want %v, true", host, ok, l.CardByFixtureID[3])
	}
}

// A declined cast -- here, no mana at all -- is not a fixture error: the
// verb does not assert success, the same as playland.
func TestRunActionsCastSpellFailureLeavesCardInHand(t *testing.T) {
	t.Parallel()

	db := permanentDB(t, "Grizzly Bears", "Creature Bear", "G")
	l := load(t, db, "humanlife=20\nailife=20\nhumanhand=Grizzly Bears|Id:1\n")
	c := engine.NewScriptedController()

	err := runActions(t, l, c, "startturn human\nadvance 3\ncastspell human 1\n")
	if err != nil {
		t.Fatalf("RunActions: %v", err)
	}

	if l.Game.Card(l.CardByFixtureID[1]).Zone != engine.Hand {
		t.Error("Grizzly Bears left hand despite the declined cast")
	}
}

func TestRunActionsCastSpellUnknownCardIDErrors(t *testing.T) {
	t.Parallel()

	db := permanentDB(t, "Grizzly Bears", "Creature Bear", "1 G")
	l := load(t, db, "humanlife=20\nailife=20\nhumanhand=Grizzly Bears|Id:1\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "castspell human 99\n"); err == nil {
		t.Error("an id absent from setup.state did not error")
	}
}

func TestRunActionsCastSpellTooFewArgsErrors(t *testing.T) {
	t.Parallel()

	db := permanentDB(t, "Grizzly Bears", "Creature Bear", "1 G")
	l := load(t, db, "humanlife=20\nailife=20\nhumanhand=Grizzly Bears|Id:1\n")
	c := engine.NewScriptedController()

	if err := runActions(t, l, c, "castspell human\n"); err == nil {
		t.Error("castspell with no card id did not error")
	}
}

// resolvestack surfaces ResolveStack's own error rather than swallowing it
// -- an unimplemented API is a real gap, not a declined decision, the same
// GO-7 "a bad card fails its game" reasoning the registry itself documents.
//
// The pushed Ability names an APIType past every registered (and every
// corpus-known) one, rather than relying on the zero value: APIType's own
// generated order is alphabetical by name (apitype_gen.go), so the zero
// value is whichever API sorts first, and that has been a real, registered
// one (APIAbandon) since abandoneffect.go landed. math.MaxUint16 always sits
// past numAPITypes (apitype_gen.go), so Registry.Resolve's own
// int(a.API) >= numAPITypes bound (effect.go) is the only way this name
// stays unregistered on purpose, not by alphabetical accident.
func TestRunActionsResolveStackSurfacesAnUnimplementedAPI(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\n")
	c := engine.NewScriptedController()
	l.Game.PushAbility(engine.Ability{API: engine.APIType(math.MaxUint16)})

	if err := runActions(t, l, c, "resolvestack\n"); err == nil {
		t.Error("resolvestack with an unregistered API did not error")
	}
}
