package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// Forsworn Paladin's Count$TotalManaSpent Treasure reads the mana its
// activation paid (AbilityUtils.java:1856-1865, the root ability's
// getPayingMana): a Treasure's mana gives the target deathtouch, other mana
// does not.
func TestTotalManaSpentReadsTheActivationsPayingMana(t *testing.T) {
	t.Parallel()
	for _, withTreasure := range []bool{true, false} {
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		paladin := g.NewCard(corpusCard(t, "Forsworn Paladin"), p, engine.Battlefield)
		bear := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
		c := engine.NewScriptedController()
		g.Player(p).ManaPool.Add(mana.Black, 2)
		if withTreasure {
			treasure := g.NewCard(manaSourceDef(t, "Test Treasure", "Artifact Treasure",
				"AB$ Mana | Cost$ T Sac<1/CARDNAME> | Produced$ Any"), p, engine.Battlefield)
			c.QueueManaColor(mana.Black)
			if !g.ActivateManaAbility(p, treasure, 0, c) {
				t.Fatal("could not float the Treasure's mana")
			}
		} else {
			g.Player(p).ManaPool.Add(mana.Black, 1)
		}
		c.QueueTargets([]engine.EntityID{engine.CardEntity(bear)})
		queueXPayGeneric(c, mana.ShardB, 2)
		if !g.ActivateAbility(p, paladin, 1, c) {
			t.Fatal("could not activate the Paladin's pump")
		}
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatalf("ResolveStack: %v", err)
		}
		if got := g.Card(bear).HasKeyword("Deathtouch"); got != withTreasure {
			t.Errorf("Treasure mana %v: deathtouch = %v", withTreasure, got)
		}
		if pw, _ := g.Card(bear).Power(); pw != 4 {
			t.Errorf("Treasure mana %v: power = %d, want 4", withTreasure, pw)
		}
	}
}

// Outside a spell or an activation the root's paying mana is not recorded:
// unresolved, not 0 (GO-7); a spell's root reads its cast.
func TestTotalManaSpentOutsideAnActivation(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	if err := measureErr(t, g, p, "Count$TotalManaSpent Treasure"); err == nil {
		t.Error("a trigger's TotalManaSpent: no error, want unresolved")
	}
	got := spentFromCountHead(t, "Count$TotalManaSpent Cave")
	if got != 1 {
		t.Errorf("a spell's TotalManaSpent Cave = %d, want 1", got)
	}
}

// spentFromCountHead casts an instant costing {R} paid with a Cave's mana that
// gains X life, X the given head, and returns the life gained.
func spentFromCountHead(t *testing.T, head string) int {
	t.Helper()
	g, p, _ := newTwoPlayerGame(t)
	spell := g.NewCard(spellDefWith(t, "Test Sourced Spend", "Instant", "R",
		"SP$ GainLife | Defined$ You | LifeAmount$ X", "X", head), p, engine.Hand)
	cave := g.NewCard(manaSourceDef(t, "Test Cave", "Land Cave", "AB$ Mana | Cost$ T | Produced$ R"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	if !g.ActivateManaAbility(p, cave, 0, c) {
		t.Fatal("could not float the Cave's mana")
	}
	before := g.Player(p).Life
	castThenResolve(t, g, p, spell, c)
	return g.Player(p).Life - before
}

// Plumb the Forbidden's CastSA>Count$xPaid is the X its Sac<X/Creature> cost
// announced (AbilityUtils.java:1890-1891 over the cast SpellAbility): two
// creatures sacrificed copy it twice, none skips the trigger.
func TestCastSAxPaidCountsTheSacrificedCreatures(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		x         int
		wantDrawn int
	}{
		{2, 3},
		{0, 1},
	} {
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		libraryCards(t, g, p, 5)
		var fodder []engine.CardID
		for range tc.x {
			fodder = append(fodder, g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield))
		}
		plumb := g.NewCard(corpusCard(t, "Plumb the Forbidden"), p, engine.Hand)
		g.Player(p).ManaPool.Add(mana.Black, 2)
		c := engine.NewScriptedController()
		c.QueuePayX(tc.x)
		c.QueuePayGeneric(mana.ShardB)
		if tc.x > 0 {
			c.QueueSacrificeChoice(fodder)
		}
		castThenResolve(t, g, p, plumb, c)
		if got := 5 - g.Zone(engine.Library, p).Len(); got != tc.wantDrawn {
			t.Errorf("X=%d: drew %d, want %d", tc.x, got, tc.wantDrawn)
		}
		if got := g.Player(p).Life; got != 20-tc.wantDrawn {
			t.Errorf("X=%d: life = %d, want %d", tc.x, got, 20-tc.wantDrawn)
		}
	}
}

// Blaster Hulk's inline Amount$ Count$CountersRemovedThisTurn ENERGY You takes
// {1} off per energy paid this turn (CostAdjustment.java:479); a ReduceCost
// Amount$ that does not resolve refuses the cast and says why (GO-7), where it
// was skipped.
func TestReduceCostInlineAmountAndUnresolvableAmount(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.Player(p).Counters.Add(engine.Energy, 5)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
		"DB$ RemoveCounter | Defined$ You | CounterType$ ENERGY | CounterNum$ 4"); err != nil {
		t.Fatalf("paying energy: %v", err)
	}
	hulk := g.NewCard(corpusCard(t, "Blaster Hulk"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Red, 4)
	c := engine.NewScriptedController()
	queueXPayGeneric(c, mana.ShardR, 2)
	if !g.CastSpell(p, hulk, c) {
		t.Fatal("could not cast Blaster Hulk for {2}{R}{R} after paying four energy")
	}

	g2, p2, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g2.NewCard(copyTestDef(t, "Bad Discount", "Artifact", "", "",
		"S:Mode$ ReduceCost | ValidCard$ Creature | Type$ Spell | Activator$ You | Amount$ Count$BogusHead"), p2, engine.Battlefield)
	bears := g2.NewCard(corpusCard(t, "Grizzly Bears"), p2, engine.Hand)
	g2.Player(p2).ManaPool.Add(mana.Green, 2)
	c2 := engine.NewScriptedController()
	c2.QueuePayGeneric(mana.ShardG)
	if g2.CastSpell(p2, bears, c2) {
		t.Error("cast Grizzly Bears under an unresolvable reduction, want refused")
	}
	if err := g2.TakePendingError(); err == nil || !strings.Contains(err.Error(), "ReduceCost") {
		t.Errorf("pending error = %v, want the ReduceCost Amount$ refusal", err)
	}
}

// A Relative$ reduction read off the X a Sac<X/…> part announces (Extus,
// Oriq Overlord's "{2} less for each creature sacrificed this way") cannot
// resolve before the announce: the probe that only asks whether an X is left
// skips it, and the payment applies it (CostAdjustment.java:473-477).
func TestReduceCostReadsTheAnnouncedXOnlyAtPayment(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	fodder := []engine.CardID{
		g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield),
		g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield),
	}
	spell := g.NewCard(copyTestDef(t, "Test Blood Price", "Sorcery", "", "", "Cost:4 B",
		"A:SP$ GainLife | Cost$ 4 B Sac<X/Creature> | Defined$ You | LifeAmount$ 1",
		"S:Mode$ ReduceCost | ValidCard$ Card.Self | Type$ Spell | Amount$ Y | Relative$ True | EffectZone$ All",
		"SVar:X:Count$xPaid", "SVar:Y:SVar$X/Times.2"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Black, 1)
	c := engine.NewScriptedController()
	c.QueuePayX(2)
	c.QueueSacrificeChoice(fodder)
	castThenResolve(t, g, p, spell, c)
	if got := g.Player(p).Life; got != 21 {
		t.Errorf("life = %d, want 21: cast for {B} after sacrificing two", got)
	}
	if err := g.TakePendingError(); err != nil {
		t.Errorf("pending error %v, want none: the probe skips the unresolved reduction", err)
	}
}

// A modal card's other face is not its state: Extus, Oriq Overlord is cast
// without its back face's Sac<X> reduction being read at all.
func TestReduceCostSkipsAModalCardsOtherFace(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	extus := g.NewCard(corpusCard(t, "Extus, Oriq Overlord"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.White, 1)
	g.Player(p).ManaPool.Add(mana.Black, 3)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardB)
	castThenResolve(t, g, p, extus, c)
	if got := g.Card(extus).Zone; got != engine.Battlefield {
		t.Errorf("Extus is in %v, want the battlefield", got)
	}
	if err := g.TakePendingError(); err != nil {
		t.Errorf("pending error %v, want none", err)
	}
}

// Count$SearchedLibrary.<players> counts the searches of a player's own
// library this turn (ChangeZoneEffect.java:1067-1079, AbilityUtils.java:
// 1941-1948), reset at cleanup (Player.java:2499).
func TestSearchedLibraryCountsOwnLibrarySearches(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	lib := libraryCards(t, g, other, 3)
	c := engine.NewScriptedController()
	c.QueueCardChoice(lib[:1])
	if _, err := resolveNow(t, g, other, c, nil,
		"DB$ ChangeZone | Origin$ Library | Destination$ Hand | ChangeType$ Card | ChangeNum$ 1 | Mandatory$ True"); err != nil {
		t.Fatalf("search: %v", err)
	}
	for _, tc := range []struct {
		x    string
		want int
	}{
		{"Count$SearchedLibrary.Opponent", 1},
		{"Count$SearchedLibrary.You", 0},
		{"Count$SearchedLibrary.Player", 1},
	} {
		if got := measure(t, g, p, tc.x); got != tc.want {
			t.Errorf("%s = %d, want %d", tc.x, got, tc.want)
		}
	}
	for _, x := range []string{"Count$SearchedLibrary", "Count$SearchedLibrary.Bogus"} {
		if err := measureErr(t, g, p, x); err == nil {
			t.Errorf("%s: no error, want unresolved", x)
		}
	}
	clone := g.Clone()
	g.SetTurnState(1, p, engine.EndOfTurn)
	g.AdvancePhase(c)
	if got := measure(t, g, p, "Count$SearchedLibrary.Opponent"); got != 0 {
		t.Errorf("after cleanup = %d, want 0", got)
	}
	if got := measure(t, clone, p, "Count$SearchedLibrary.Opponent"); got != 1 {
		t.Errorf("clone = %d, want 1", got)
	}
}

// Pulse of the Forge's Defined$ Parent with Origin$ Stack returns the
// resolving spell itself to its owner's hand (ChangeZoneEffect.java:496-506,
// AbilityUtils.java:1232-1233) when the target has more life than its caster.
func TestChangeZoneDefinedParentReturnsTheResolvingSpell(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		casterLife int
		want       engine.ZoneType
	}{
		{10, engine.Hand},
		{20, engine.Graveyard},
	} {
		g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
		g.Player(p).Life = tc.casterLife
		pulse := g.NewCard(corpusCard(t, "Pulse of the Forge"), p, engine.Hand)
		g.Player(p).ManaPool.Add(mana.Red, 3)
		c := engine.NewScriptedController()
		c.QueuePayGeneric(mana.ShardR)
		c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
		castThenResolve(t, g, p, pulse, c)
		if got := g.Player(other).Life; got != 16 {
			t.Errorf("caster at %d: target life = %d, want 16", tc.casterLife, got)
		}
		if got := g.Card(pulse).Zone; got != tc.want {
			t.Errorf("caster at %d: Pulse of the Forge is in %v, want %v", tc.casterLife, got, tc.want)
		}
	}
}

// Beacon of Creation's Defined$ Parent shuffles the resolving spell into its
// owner's library (removeFromStack's moveToLibrary then shuffle,
// ChangeZoneEffect.java:1625-1629), not the graveyard.
func TestChangeZoneDefinedParentShufflesTheSpellIntoTheLibrary(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	libraryCards(t, g, p, 3)
	beacon := g.NewCard(corpusCard(t, "Beacon of Creation"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Green, 4)
	c := engine.NewScriptedController()
	queueXPayGeneric(c, mana.ShardG, 3)
	castThenResolve(t, g, p, beacon, c)
	if got := g.Card(beacon).Zone; got != engine.Library {
		t.Errorf("Beacon of Creation is in %v, want its owner's library", got)
	}
	if got := g.Zone(engine.Library, p).Len(); got != 4 {
		t.Errorf("library holds %d cards, want 4", got)
	}
}

// A Charm mode and a Branch result are sub-abilities of the spell in Java
// (getRootAbility is the spell): Defined$ Parent there returns the spell too.
func TestChangeZoneDefinedParentFromAModeOrABranch(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		name  string
		line  string
		svars []string
		queue func(*engine.ScriptedController)
	}{
		{"charm mode", "A:SP$ Charm | Choices$ DBGain,DBReturn", nil,
			func(c *engine.ScriptedController) { c.QueueModeChoice([]int{1}) }},
		{"branch result", "A:SP$ Branch | BranchConditionSVar$ X | BranchConditionSVarCompare$ GE0 | TrueSubAbility$ DBReturn",
			[]string{"SVar:X:Number$0"}, func(*engine.ScriptedController) {}},
	} {
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		lines := append([]string{"Cost:G", tc.line,
			"SVar:DBGain:DB$ GainLife | Defined$ You | LifeAmount$ 1",
			"SVar:DBReturn:DB$ ChangeZone | Defined$ Parent | Origin$ Stack | Destination$ Hand"}, tc.svars...)
		spell := g.NewCard(copyTestDef(t, "Test Rebound", "Instant", "", "", lines...), p, engine.Hand)
		g.Player(p).ManaPool.Add(mana.Green, 1)
		c := engine.NewScriptedController()
		tc.queue(c)
		castThenResolve(t, g, p, spell, c)
		if got := g.Card(spell).Zone; got != engine.Hand {
			t.Errorf("%s: the spell is in %v, want its owner's hand", tc.name, got)
		}
	}
}

// A Defined$ Parent whose root is not a spell moves nothing (Java's
// getTargetSpells skips a non-spell, ChangeZoneEffect.java:497); without
// Origin$ Stack the line is refused.
func TestChangeZoneDefinedParentOutsideASpell(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	host, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
		"DB$ ChangeZone | Defined$ Parent | Origin$ Stack | Destination$ Hand")
	if err != nil {
		t.Fatalf("a trigger's Defined$ Parent: %v", err)
	}
	if got := g.Card(host).Zone; got != engine.Battlefield {
		t.Errorf("host is in %v, want the battlefield", got)
	}
	_, err = resolveNow(t, g, p, engine.NewScriptedController(), nil,
		"DB$ ChangeZone | Defined$ Parent | Origin$ Graveyard | Destination$ Hand")
	if err == nil || !strings.Contains(err.Error(), "Defined$ Parent") {
		t.Errorf("Defined$ Parent from the graveyard: err = %v, want the refusal", err)
	}
}
