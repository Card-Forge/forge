package fixture_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/fixture"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// testDB builds a database of vanilla cards -- no abilities, no type line --
// because Load never reads either: a compiled card's only field it looks at
// is Def.Name. Building carddb.Card values directly, rather than through
// ParseScript, keeps these tests independent of the corpus and TypeLists.txt.
func testDB(t *testing.T, names ...string) *compile.DB {
	t.Helper()

	cards := make(map[string]*compile.Card, len(names))
	for _, name := range names {
		raw := &carddb.Card{
			Filename: name,
			Faces:    [carddb.NumFaces]carddb.Face{{Present: true, Name: name}},
		}
		c, err := compile.Compile(raw)
		if err != nil {
			t.Fatalf("compile %q: %v", name, err)
		}
		cards[name] = c
	}
	return compile.NewDB(cards)
}

func load(t *testing.T, db *compile.DB, text string) *fixture.Loaded {
	t.Helper()

	st, err := fixture.Parse(strings.NewReader(text))
	if err != nil {
		t.Fatalf("Parse: %v", err)
	}
	l, err := fixture.Load(st, db, javarand.New(1))
	if err != nil {
		t.Fatalf("Load: %v", err)
	}
	return l
}

func TestLoadSeatsOnlyNamedPlayers(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=17\n")

	if got := len(l.Game.Players()); got != 2 {
		t.Fatalf("seated %d players, want 2", got)
	}
	if got := l.Game.Player(l.Game.Players()[0]).Name; got != "human" {
		t.Errorf("first player %q, want human", got)
	}
	if got := l.Game.Player(l.Game.Players()[1]).Name; got != "ai" {
		t.Errorf("second player %q, want ai", got)
	}
}

func TestLoadPutsCardsInZones(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears", "Llanowar Elves", "Mountain")
	l := load(t, db, "humanbattlefield=Grizzly Bears;Llanowar Elves\nhumanhand=Mountain\n")

	human := l.Game.Players()[0]
	bf := l.Game.Zone(engine.Battlefield, human).Cards()
	if len(bf) != 2 {
		t.Fatalf("battlefield has %d cards, want 2", len(bf))
	}
	if got := l.Game.Card(bf[0]).Def.Name; got != "Grizzly Bears" {
		t.Errorf("first battlefield card %q, want Grizzly Bears", got)
	}
	if got := l.Game.Card(bf[1]).Def.Name; got != "Llanowar Elves" {
		t.Errorf("second battlefield card %q, want Llanowar Elves", got)
	}
	hand := l.Game.Zone(engine.Hand, human).Cards()
	if len(hand) != 1 || l.Game.Card(hand[0]).Def.Name != "Mountain" {
		t.Errorf("hand %v, want one Mountain", hand)
	}
}

func TestLoadUnknownCardErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	st, err := fixture.Parse(strings.NewReader("humanbattlefield=Not A Real Card\n"))
	if err != nil {
		t.Fatalf("Parse: %v", err)
	}
	if _, err := fixture.Load(st, db, javarand.New(1)); err == nil {
		t.Error("a card not in the database loaded without error")
	}
}

func TestLoadTappedAndSummonSick(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears", "Llanowar Elves")
	l := load(t, db, "humanbattlefield=Grizzly Bears|Tapped;Llanowar Elves|SummonSick\n")

	human := l.Game.Players()[0]
	bf := l.Game.Zone(engine.Battlefield, human).Cards()
	bears, elves := l.Game.Card(bf[0]), l.Game.Card(bf[1])

	if !bears.Tapped || bears.SummonSick {
		t.Errorf("Grizzly Bears tapped=%v summonsick=%v, want true/false", bears.Tapped, bears.SummonSick)
	}
	if elves.Tapped || !elves.SummonSick {
		t.Errorf("Llanowar Elves tapped=%v summonsick=%v, want false/true", elves.Tapped, elves.SummonSick)
	}
}

// Java matches "Tapped" and "SummonSick" on prefix, no colon required
// (PORT-7): a fixture written either way loads the same in both engines.
func TestLoadTappedMatchesOnBarePrefix(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears")
	l := load(t, db, "humanbattlefield=Grizzly Bears|Tapped:True\n")

	id := l.Game.Zone(engine.Battlefield, l.Game.Players()[0]).Cards()[0]
	if !l.Game.Card(id).Tapped {
		t.Error("Tapped:True did not tap the card")
	}
}

func TestLoadRemoveSummoningSicknessOverridesEveryCard(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears")
	l := load(t, db, "removesummoningsickness=true\nhumanbattlefield=Grizzly Bears|SummonSick\n")

	id := l.Game.Zone(engine.Battlefield, l.Game.Players()[0]).Cards()[0]
	if l.Game.Card(id).SummonSick {
		t.Error("removesummoningsickness did not override the per-card annotation")
	}
}

func TestLoadCounters(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears")
	l := load(t, db, "humanbattlefield=Grizzly Bears|Counters:P1P1=3,STUN=1\n")

	id := l.Game.Zone(engine.Battlefield, l.Game.Players()[0]).Cards()[0]
	c := l.Game.Card(id)
	if got := c.Counters.Count(engine.P1P1); got != 3 {
		t.Errorf("P1P1 %d, want 3", got)
	}
	if got := c.Counters.Count(engine.Stun); got != 1 {
		t.Errorf("STUN %d, want 1", got)
	}
}

func TestLoadDamage(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears")
	l := load(t, db, "humanbattlefield=Grizzly Bears|Damage:2\n")

	id := l.Game.Zone(engine.Battlefield, l.Game.Players()[0]).Cards()[0]
	if got := l.Game.Card(id).Damage.Marked; got != 2 {
		t.Errorf("damage %d, want 2", got)
	}
}

// AttachedTo resolves after every card in the fixture exists, so a forward
// reference -- the aura declared before the creature it names -- has to work
// the same as a backward one.
func TestLoadAttachedToResolvesForwardReferences(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Rancor", "Grizzly Bears")
	l := load(t, db, "humanbattlefield=Rancor|Id:1|AttachedTo:2;Grizzly Bears|Id:2\n")

	human := l.Game.Players()[0]
	bf := l.Game.Zone(engine.Battlefield, human).Cards()
	aura, bears := bf[0], bf[1]

	host, ok := l.Game.Card(aura).AttachedTo()
	if !ok || host != bears {
		t.Errorf("Rancor attached to %v ok=%v, want %v true", host, ok, bears)
	}
	if got := l.Game.Card(bears).Attachments(); len(got) != 1 || got[0] != aura {
		t.Errorf("Grizzly Bears' attachments %v, want [Rancor]", got)
	}
}

func TestLoadAttachedToMissingHostErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Rancor")
	st, err := fixture.Parse(strings.NewReader("humanbattlefield=Rancor|AttachedTo:99\n"))
	if err != nil {
		t.Fatalf("Parse: %v", err)
	}
	if _, err := fixture.Load(st, db, javarand.New(1)); err == nil {
		t.Error("attaching to a nonexistent id loaded without error")
	}
}

func TestLoadOwnerDiffersFromController(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears")
	l := load(t, db, "humanbattlefield=Grizzly Bears|Owner:ai\nailife=20\n")

	human, ai := l.Game.Players()[0], l.Game.Players()[1]
	id := l.Game.Zone(engine.Battlefield, human).Cards()[0]
	c := l.Game.Card(id)
	if c.Controller() != human {
		t.Errorf("controller %v, want human -- Owner: must not change control", c.Controller())
	}
	if c.Owner != ai {
		t.Errorf("owner %v, want ai", c.Owner)
	}
}

func TestLoadProtector(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Invasion of Amonkhet")
	l := load(t, db, "humanbattlefield=Invasion of Amonkhet|Protector:ai\nailife=20\n")

	ai := l.Game.Players()[1]
	id := l.Game.Zone(engine.Battlefield, l.Game.Players()[0]).Cards()[0]
	if got := l.Game.Card(id).ProtectingPlayer; got != ai {
		t.Errorf("ProtectingPlayer = %v, want %v", got, ai)
	}
}

func TestLoadBadProtectorNameErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Invasion of Amonkhet")
	st, err := fixture.Parse(strings.NewReader("humanbattlefield=Invasion of Amonkhet|Protector:nobody\n"))
	if err != nil {
		t.Fatalf("Parse: %v", err)
	}
	if _, err := fixture.Load(st, db, javarand.New(1)); err == nil {
		t.Error("an unseated protector name loaded without error")
	}
}

func TestLoadRememberedAndImprintedCards(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears", "Llanowar Elves", "Mountain")
	l := load(t, db, "humanbattlefield=Grizzly Bears|Id:1|RememberedCards:2,3;Llanowar Elves|Id:2;Mountain|Id:3\n")

	bf := l.Game.Zone(engine.Battlefield, l.Game.Players()[0]).Cards()
	bears, elves, mountain := bf[0], bf[1], bf[2]

	remembered := l.Game.Card(bears).Memory.Remembered()
	if len(remembered) != 2 {
		t.Fatalf("remembered %v, want 2 entries", remembered)
	}
	if got, _ := remembered[0].AsCard(); got != elves {
		t.Errorf("first remembered %v, want elves", got)
	}
	if got, _ := remembered[1].AsCard(); got != mountain {
		t.Errorf("second remembered %v, want mountain", got)
	}
}

func TestLoadUnappliedAnnotationsAreRecorded(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears")
	l := load(t, db, "humanbattlefield=Grizzly Bears|Renowned\n")

	found := false
	for _, u := range l.Unapplied {
		if strings.Contains(u, "Renowned") {
			found = true
		}
	}
	if !found {
		t.Errorf("Unapplied %v does not mention the dropped Renowned annotation", l.Unapplied)
	}
}

// TestLoadReportsIsCommanderUnapplied pins what the engine's commander-cast
// count rests on (engine.Game.commanderCasts): |IsCommander
// (GameState.java:1377, Player.addCommander) designates no commander here --
// no Commander setup is ported -- so it is reported, never silently taken.
// Supporting it means porting the Commander Effect and the cast record with it.
func TestLoadReportsIsCommanderUnapplied(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears")
	l := load(t, db, "humancommand=Grizzly Bears|IsCommander\n")
	if len(l.Unapplied) != 1 || !strings.Contains(l.Unapplied[0], "IsCommander") {
		t.Errorf("Unapplied %v, want the one IsCommander annotation", l.Unapplied)
	}
}

// manapool= applies to engine.Player.ManaPool for real -- GameState.java's
// own space-separated-letters shape, not a mana cost's "2W" shorthand.
func TestLoadAppliesManaPool(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nhumanmanapool=R R W\n")

	p := l.Game.Players()[0]
	if got, want := l.Game.Player(p).ManaPool.Breakdown(), [6]int{1, 0, 0, 2, 0, 0}; got != want {
		t.Errorf("mana pool = %v, want %v (one white, two red)", got, want)
	}
}

// A token manapool= does not recognise fails the load outright -- the same
// "fail loud on a fixture-authoring mistake" reasoning every other malformed
// value here already gets, not a silently empty pool.
func TestLoadManaPoolRejectsUnknownToken(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	st, err := fixture.Parse(strings.NewReader("humanlife=20\nhumanmanapool=Q\n"))
	if err != nil {
		t.Fatalf("Parse: %v", err)
	}
	if _, err := fixture.Load(st, db, javarand.New(1)); err == nil {
		t.Error("an unknown mana pool token loaded without error")
	}
}

// persistentmana= has nowhere to go yet -- engine.Pool tracks no
// persistence, CR 500.4's own emptying applies to every kind of floating
// mana this port has -- so it is still reported through Unapplied rather
// than silently dropped.
func TestLoadUnappliedPersistentManaIsRecorded(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nhumanpersistentmana=R R\n")

	found := false
	for _, u := range l.Unapplied {
		if strings.Contains(u, "persistent mana") {
			found = true
		}
	}
	if !found {
		t.Errorf("Unapplied %v does not mention the dropped persistent mana", l.Unapplied)
	}
}

func TestLoadSeatsPNSlots(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\np2life=20\np3life=20\n")

	if got := len(l.Game.Players()); got != 4 {
		t.Fatalf("seated %d players, want 4", got)
	}
	if got := l.Game.Player(l.Game.Players()[2]).Name; got != "p2" {
		t.Errorf("third player %q, want p2", got)
	}
	if got := l.Game.Player(l.Game.Players()[3]).Name; got != "p3" {
		t.Errorf("fourth player %q, want p3", got)
	}
}

func TestLoadUnknownActivePlayerErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	st, err := fixture.Parse(strings.NewReader("humanlife=20\nactiveplayer=ai\n"))
	if err != nil {
		t.Fatalf("Parse: %v", err)
	}
	if _, err := fixture.Load(st, db, javarand.New(1)); err == nil {
		t.Error("activeplayer naming an unseated player loaded without error")
	}
}

// Player-level Counters applies to engine.Player.Counters -- poison chief
// among them, which is what CR 704.5c reads -- and landsplayed/
// landsplayedlastturn apply straight to engine.Player.LandsPlayed/
// LandsPlayedLastTurn (PlayLand's own per-turn limit, land.go), the same as
// ManaPool since M5's mana-payment work.
func TestLoadPlayerCountersAndLandsPlayedApply(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nhumancounters=POISON=3\nhumanlandsplayed=1\nhumanlandsplayedlastturn=2\n")

	p := l.Game.Player(l.Game.Players()[0])
	if got := p.Counters.Count(engine.Poison); got != 3 {
		t.Errorf("poison counters %d, want 3", got)
	}
	if got := p.LandsPlayed; got != 1 {
		t.Errorf("LandsPlayed = %d, want 1", got)
	}
	if got := p.LandsPlayedLastTurn; got != 2 {
		t.Errorf("LandsPlayedLastTurn = %d, want 2", got)
	}
}

func TestLoadMalformedPlayerCountersErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	st, err := fixture.Parse(strings.NewReader("humanlife=20\nhumancounters=POISON\n"))
	if err != nil {
		t.Fatalf("Parse: %v", err)
	}
	if _, err := fixture.Load(st, db, javarand.New(1)); err == nil {
		t.Error("a malformed player counters value loaded without error")
	}
}

// A trailing separator or repeated `;` produces an empty entry, which is
// dropped rather than treated as a card named "".
func TestLoadSkipsEmptyZoneEntries(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears")
	l := load(t, db, "humanbattlefield=Grizzly Bears;;\n")

	bf := l.Game.Zone(engine.Battlefield, l.Game.Players()[0]).Cards()
	if len(bf) != 1 {
		t.Errorf("battlefield has %d cards, want 1 (empty entries dropped)", len(bf))
	}
}

// Set: and Art: pick a printing Crucible does not model; they parse and are
// silently dropped, the same established decision as internal/deck's Edition
// and Extra (porting/port-log/deck-serializer.md).
func TestLoadSetAndArtAreDroppedNotReported(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears")
	l := load(t, db, "humanbattlefield=Grizzly Bears|Set:M10|Art:2\n")

	if len(l.Unapplied) != 0 {
		t.Errorf("Unapplied %v, want none -- Set/Art are a deliberate no-op", l.Unapplied)
	}
}

func TestLoadJavaTokenSnapshotsAreReportedNotLoaded(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanbattlefield=t:1/1 G Insect\n")

	if got := len(l.Game.Zone(engine.Battlefield, l.Game.Players()[0]).Cards()); got != 0 {
		t.Errorf("battlefield has %d cards, want 0 -- a t: snapshot is not loaded", got)
	}
	if len(l.Unapplied) != 1 {
		t.Errorf("Unapplied %v, want one entry naming the dropped token", l.Unapplied)
	}
}

func TestLoadMalformedCardAnnotationsFail(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears")
	cases := []string{
		"Grizzly Bears|Counters:P1P1",
		"Grizzly Bears|Counters:P1P1=x",
		"Grizzly Bears|Damage:x",
		"Grizzly Bears|Id:x",
		"Grizzly Bears|AttachedTo:x",
		"Grizzly Bears|Owner:nobody",
		"Grizzly Bears|RememberedCards:x",
		"Grizzly Bears|Imprinting:x",
	}
	for _, entry := range cases {
		t.Run(entry, func(t *testing.T) {
			st, err := fixture.Parse(strings.NewReader("humanbattlefield=" + entry + "\n"))
			if err != nil {
				t.Fatalf("Parse: %v", err)
			}
			if _, err := fixture.Load(st, db, javarand.New(1)); err == nil {
				t.Errorf("%q loaded without error", entry)
			}
		})
	}
}

func TestLoadRememberedCardsMissingIDErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears")
	st, err := fixture.Parse(strings.NewReader("humanbattlefield=Grizzly Bears|RememberedCards:99\n"))
	if err != nil {
		t.Fatalf("Parse: %v", err)
	}
	if _, err := fixture.Load(st, db, javarand.New(1)); err == nil {
		t.Error("RememberedCards naming a nonexistent id loaded without error")
	}
}

func TestLoadImprintingMissingIDErrors(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears")
	st, err := fixture.Parse(strings.NewReader("humanbattlefield=Grizzly Bears|Imprinting:99\n"))
	if err != nil {
		t.Fatalf("Parse: %v", err)
	}
	if _, err := fixture.Load(st, db, javarand.New(1)); err == nil {
		t.Error("Imprinting naming a nonexistent id loaded without error")
	}
}

func TestLoadLostWonOver(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=0\nhumanlost=true\nailife=20\naiwon=true\nover=true\n")

	human, ai := l.Game.Players()[0], l.Game.Players()[1]
	if !l.Game.Player(human).Lost {
		t.Error("human.Lost did not apply")
	}
	if !l.Game.Player(ai).Won {
		t.Error("ai.Won did not apply")
	}
	if !l.Game.Over() {
		t.Error("Game.Over() did not apply")
	}
}

func TestLoadActivePlayerAndPhase(t *testing.T) {
	t.Parallel()

	db := testDB(t)
	l := load(t, db, "humanlife=20\nailife=20\nactiveplayer=ai\nactivephase=Main1\nturn=3\n")

	if l.Game.Turn() != 3 {
		t.Errorf("turn %d, want 3", l.Game.Turn())
	}
	if l.Game.ActivePlayer() != l.Game.Players()[1] {
		t.Errorf("active player %v, want ai", l.Game.ActivePlayer())
	}
	if l.Game.ActivePhase() != engine.Main1 {
		t.Errorf("phase %v, want Main1", l.Game.ActivePhase())
	}
}

// tokenDB is testDB plus one compiled token script, named by file stem.
func tokenDB(t *testing.T, script string) *compile.DB {
	t.Helper()

	raw := &carddb.Card{
		Filename: script,
		Faces:    [carddb.NumFaces]carddb.Face{{Present: true, Name: "Soldier Token"}},
	}
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", script, err)
	}
	return testDB(t).WithTokens(map[string]*compile.Card{script: c})
}

func TestLoadTokenByScriptNameMakesAToken(t *testing.T) {
	t.Parallel()

	db := tokenDB(t, "w_1_1_soldier")
	l := load(t, db, "humanbattlefield=T:w_1_1_soldier|Id:1|Tapped\n")

	human := l.Game.Players()[0]
	ids := l.Game.Zone(engine.Battlefield, human).Cards()
	if len(ids) != 1 {
		t.Fatalf("battlefield has %d cards, want the one token", len(ids))
	}
	c := l.Game.Card(ids[0])
	if !c.IsToken || !c.Tapped {
		t.Errorf("IsToken = %v, Tapped = %v, want both true", c.IsToken, c.Tapped)
	}
	if len(l.Unapplied) != 0 {
		t.Errorf("Unapplied %v, want none", l.Unapplied)
	}
}

func TestLoadTokenNotInTheDatabaseFails(t *testing.T) {
	t.Parallel()

	st, err := fixture.Parse(strings.NewReader("humanbattlefield=T:no_such_token\n"))
	if err != nil {
		t.Fatalf("Parse: %v", err)
	}
	if _, err := fixture.Load(st, tokenDB(t, "w_1_1_soldier"), javarand.New(1)); err == nil {
		t.Error("an unknown token script loaded without error")
	}
}
