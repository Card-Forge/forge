package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// This file covers the player properties PlayerProperty.playerHasProperty
// answers from the turn's records, combat and zone counts (LostLifeThisTurn,
// wasDealt...ThisTurn[By], Defending, HasCardsIn..., withMore..., ...), read
// through PlayerCount...$HasProperty<p> and Property<p> amounts and through
// Defined$ player specs, plus the playerXProperty measures over the new
// per-player records (OpponentsAttackedThisTurn/ThisCombat, SacrificedThisTurn,
// NumPowerSurgeLands).

// propertyMeter puts an artifact on p's battlefield whose one ability is
// "AB$ <line>", X's SVar body x, with extra "Name:body" SVars.
func propertyMeter(t *testing.T, g *engine.Game, p engine.PlayerID, line, x string, svars ...string) engine.CardID {
	t.Helper()
	lines := []string{"A:AB$ " + line, "SVar:X:" + x}
	for _, s := range svars {
		lines = append(lines, "SVar:"+s)
	}
	return g.NewCard(copyTestDef(t, "Meter", "Artifact", "", "", lines...), p, engine.Battlefield)
}

// measure resolves "gain X life" from a fresh meter p controls and answers
// the life p gained.
func measure(t *testing.T, g *engine.Game, p engine.PlayerID, x string, svars ...string) int {
	t.Helper()
	host := propertyMeter(t, g, p, "GainLife | Defined$ You | LifeAmount$ X", x, svars...)
	before := g.Player(p).Life
	if err := activate(g, p, engine.NewScriptedController(), host); err != nil {
		t.Fatalf("%s: %v", x, err)
	}
	return g.Player(p).Life - before
}

// measureErr is measure's refusal half: the activation's error.
func measureErr(t *testing.T, g *engine.Game, p engine.PlayerID, x string, svars ...string) error {
	t.Helper()
	host := propertyMeter(t, g, p, "GainLife | Defined$ You | LifeAmount$ X", x, svars...)
	return activate(g, p, engine.NewScriptedController(), host)
}

// attackOpponent declares attackers for p, made the active player in
// DeclareAttackers, at its one opponent.
func attackOpponent(t *testing.T, g *engine.Game, p engine.PlayerID, attackers ...engine.CardID) {
	t.Helper()
	g.SetTurnState(1, p, engine.DeclareAttackers)
	for _, id := range attackers {
		g.Card(id).SummonSick = false
	}
	ac := engine.NewScriptedController()
	ac.QueueAttackers(attackers)
	declareAttackers(t, g, ac)
}

func TestLostLifeThisTurnCountsPlayersWhoLostLife(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name          string
		lost, oppLost int
		x             string
		svars         []string
		want          int
	}{
		{"opponent lost life", 0, 1, "PlayerCountOpponents$HasPropertyLostLifeThisTurn", nil, 1},
		{"nobody lost life", 0, 0, "PlayerCountPlayers$HasPropertyLostLifeThisTurn", nil, 0},
		{"both lost life", 2, 4, "PlayerCountPlayers$HasPropertyLostLifeThisTurn", nil, 2},
		{"operator applies after the count", 0, 1, "PlayerCountOpponents$HasPropertyLostLifeThisTurn/Twice", nil, 2},
		{"literal comparison", 2, 4, "PlayerCountPlayers$HasPropertyLostLifeThisTurn GE3", nil, 1},
		{"SVar comparison", 2, 4, "PlayerCountPlayers$HasPropertyLostLifeThisTurn GEY", []string{"Y:2"}, 2},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			g.Player(p).LifeLostThisTurn, g.Player(other).LifeLostThisTurn = tc.lost, tc.oppLost
			if got := measure(t, g, p, tc.x, tc.svars...); got != tc.want {
				t.Errorf("%s = %d, want %d", tc.x, got, tc.want)
			}
		})
	}
}

func TestHasCardsInComparesAZoneCountWithAnOperand(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name  string
		x     string
		svars []string
		want  int
	}{
		// Consuming Tide/Wojek Investigator: opponents with more cards in hand
		// than you.
		{"more than you", "PlayerCountOpponents$HasPropertyHasCardsInHand_Card_GTY", []string{"Y:Count$CardsInYourHand"}, 1},
		{"at least four", "PlayerCountOpponents$HasPropertyHasCardsInHand_Card_GE4", nil, 0},
		{"valid string filters", "PlayerCountPlayers$HasPropertyHasCardsInGraveyard_Creature_GE1", nil, 1},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			addN(t, g, p, engine.Hand, 1, "Artifact")
			addN(t, g, other, engine.Hand, 3, "Artifact")
			addN(t, g, other, engine.Graveyard, 1, "Creature Elf")
			addN(t, g, p, engine.Graveyard, 2, "Artifact")
			if got := measure(t, g, p, tc.x, tc.svars...); got != tc.want {
				t.Errorf("%s = %d, want %d", tc.x, got, tc.want)
			}
		})
	}
}

// Noncombat damage dealt by the measuring card itself: the plain ThisTurn
// forms read its kind, BySource the amount the source dealt, and ThisTurnBy
// the distinct sources that match the valid string as they were.
func TestWasDealtDamageThisTurnReadsTheTurnsDamage(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		x    string
		want int
	}{
		{"PlayerCountOpponents$HasPropertywasDealtDamageThisTurn", 1},
		{"PlayerCountOpponents$HasPropertywasDealtNonCombatDamageThisTurn", 1},
		{"PlayerCountOpponents$HasPropertywasDealtCombatDamageThisTurn", 0},
		{"PlayerCountPlayers$HasPropertywasDealtDamageThisTurn", 1},
		{"PlayerCountOpponents$HasPropertywasDealtDamageThisTurnBySource GE5", 1},
		{"PlayerCountOpponents$HasPropertywasDealtDamageThisTurnBySource GE6", 0},
		{"PlayerCountOpponents$HasPropertywasDealtDamageThisTurnBySourceTimes EQ2", 1},
		{"PlayerCountOpponents$HasPropertywasDealtDamageThisTurnBy Card.Self GE1", 1},
		{"PlayerCountOpponents$HasPropertywasDealtDamageThisTurnBy Card.Self GE2", 0},
		{"PlayerCountOpponents$HasPropertywasDealtDamageThisTurnBy Creature GE1", 0},
		{"PlayerCountOpponents$HasPropertywasDealtDamageThisTurnBy Artifact", 1},
	} {
		t.Run(tc.x, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			host := propertyMeter(t, g, p, "DealDamage | Defined$ Opponent | NumDmg$ 2 | SubAbility$ DBAgain", tc.x,
				"DBAgain:DB$ DealDamage | Defined$ Opponent | NumDmg$ 3 | SubAbility$ DBGain",
				"DBGain:DB$ GainLife | Defined$ You | LifeAmount$ X")
			before := g.Player(p).Life
			if err := activate(g, p, engine.NewScriptedController(), host); err != nil {
				t.Fatal(err)
			}
			if got := g.Player(p).Life - before; got != tc.want {
				t.Errorf("%s = %d, want %d", tc.x, got, tc.want)
			}
		})
	}
}

// Combat damage: Estinien Varlineau's "dealt combat damage this turn by this
// creature or a Dragon" counts the distinct matching sources, each read as
// it was when it dealt the damage.
func TestWasDealtCombatDamageThisTurnByCountsMatchingSources(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	small := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	big := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	attackOpponent(t, g, p, small, big)
	bc := engine.NewScriptedController()
	bc.QueueBlocks(nil)
	declareBlockers(t, g, bc)
	g.DealCombatDamage(engine.NewScriptedController())
	if g.Player(other).Life != 16 {
		t.Fatalf("defender life = %d, want 16", g.Player(other).Life)
	}

	for _, tc := range []struct {
		x    string
		want int
	}{
		{"PlayerCountOpponents$HasPropertywasDealtCombatDamageThisTurn", 1},
		{"PlayerCountPlayers$HasPropertywasDealtCombatDamageThisTurn", 1},
		{"PlayerCountOpponents$HasPropertywasDealtNonCombatDamageThisTurn", 0},
		{"PlayerCountRegisteredOpponents$HasPropertywasDealtCombatDamageThisTurnBy Creature GE2", 1},
		{"PlayerCountRegisteredOpponents$HasPropertywasDealtCombatDamageThisTurnBy Creature.powerGE3 GE1", 1},
		{"PlayerCountRegisteredOpponents$HasPropertywasDealtCombatDamageThisTurnBy Creature.powerGE3 GE2", 0},
	} {
		if got := measure(t, g, p, tc.x); got != tc.want {
			t.Errorf("%s = %d, want %d", tc.x, got, tc.want)
		}
	}
}

// Defending (Pack Attack, Iron Mastiff) and OpponentsAttackedThisCombat
// (Amber Gristle O'Maul) read the current combat; OpponentsAttackedThisTurn
// (Militant Angel) the attacking player's record of the turn.
func TestAttackedPlayersAreCountedThisCombatAndThisTurn(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	for _, tc := range []struct {
		x    string
		want int
	}{
		{"PlayerCountPlayers$HasPropertyDefending", 0},
		{"PlayerCountPropertyYou$OpponentsAttackedThisCombat", 0},
	} {
		if got := measure(t, g, p, tc.x); got != tc.want {
			t.Errorf("before combat: %s = %d, want %d", tc.x, got, tc.want)
		}
	}
	attackOpponent(t, g, p, a, b)
	for _, tc := range []struct {
		x    string
		who  engine.PlayerID
		want int
	}{
		{"PlayerCountPlayers$HasPropertyDefending", p, 1},
		{"PlayerCountOpponents$HasPropertyDefending", p, 1},
		{"PlayerCountPropertyYou$OpponentsAttackedThisCombat", p, 1},
		{"PlayerCountPropertyYou$OpponentsAttackedThisCombat", other, 0},
		{"PlayerCountPropertyYou$OpponentsAttackedThisTurn", p, 1},
		{"PlayerCountPlayers$HasPropertyattackedWithCreaturesThisTurn", p, 1},
		{"PlayerCountPlayers$HasPropertyattackedYouTheirCurrentTurn", other, 1},
		{"PlayerCountPlayers$HasPropertyattackedYouTheirCurrentTurn", p, 0},
	} {
		if got := measure(t, g, tc.who, tc.x); got != tc.want {
			t.Errorf("during combat: %s for %v = %d, want %d", tc.x, tc.who, got, tc.want)
		}
	}
}

// The attack record outlives the turn for the player who attacked
// (attackedYouTheirLastTurn) and the turn's damage, sacrifice and
// land-for-mana records end at cleanup.
func TestCleanupRotatesAndClearsTheTurnsRecords(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	attackOpponent(t, g, p, a)
	bc := engine.NewScriptedController()
	bc.QueueBlocks(nil)
	declareBlockers(t, g, bc)
	g.DealCombatDamage(engine.NewScriptedController())
	forest := g.NewCard(landDef(t, "Forest", "Basic Land Forest"), p, engine.Battlefield)
	if !g.TapLandForMana(p, forest, mana.Green, engine.NewScriptedController()) {
		t.Fatal("TapLandForMana failed")
	}
	if got := measure(t, g, p, "PlayerCountPlayers$HasPropertyTappedLandForManaThisTurn"); got != 1 {
		t.Errorf("TappedLandForManaThisTurn before cleanup = %d, want 1", got)
	}

	g.SetTurnState(1, p, engine.EndOfTurn)
	g.AdvancePhase(engine.NewScriptedController()) // Cleanup
	for _, tc := range []struct {
		x    string
		who  engine.PlayerID
		want int
	}{
		{"PlayerCountRegisteredOpponents$HasPropertywasDealtCombatDamageThisTurnBy Creature GE1", p, 0},
		{"PlayerCountPlayers$HasPropertyTappedLandForManaThisTurn", p, 0},
		{"PlayerCountPropertyYou$OpponentsAttackedThisTurn", p, 0},
		{"PlayerCountPlayers$HasPropertyattackedYouTheirLastTurn", other, 1},
		{"PlayerCountPlayers$HasPropertyattackedYouTheirLastTurn", p, 0},
	} {
		if got := measure(t, g, tc.who, tc.x); got != tc.want {
			t.Errorf("after cleanup: %s for %v = %d, want %d", tc.x, tc.who, got, tc.want)
		}
	}
}

// Obsessive Pursuit's "permanents you sacrificed this turn": each sacrifice
// is kept as it was on the battlefield, filtered by the valid string after
// the space.
func TestSacrificedThisTurnCountsTheTurnsSacrifices(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	host := propertyMeter(t, g, p, "Sacrifice | Defined$ You | SacValid$ Creature | SubAbility$ DBGain",
		"PlayerCountPropertyYou$SacrificedThisTurn Creature",
		"DBGain:DB$ GainLife | Defined$ You | LifeAmount$ X")
	c := engine.NewScriptedController()
	c.QueueSacrificeChoice([]engine.CardID{victim})
	before := g.Player(p).Life
	if err := activate(g, p, c, host); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(p).Life - before; got != 1 {
		t.Errorf("SacrificedThisTurn Creature = %d, want 1", got)
	}
	for _, tc := range []struct {
		x    string
		who  engine.PlayerID
		want int
	}{
		{"PlayerCountPropertyYou$SacrificedThisTurn", p, 1},
		{"PlayerCountPropertyYou$SacrificedThisTurn Artifact", p, 0},
		{"PlayerCountPropertyYou$SacrificedThisTurn", other, 0},
		{"PlayerCountPropertyYou$SacrificedThisTurn/Twice", p, 2},
	} {
		if got := measure(t, g, tc.who, tc.x); got != tc.want {
			t.Errorf("%s for %v = %d, want %d", tc.x, tc.who, got, tc.want)
		}
	}
	g.SetTurnState(1, p, engine.EndOfTurn)
	g.AdvancePhase(engine.NewScriptedController())
	if got := measure(t, g, p, "PlayerCountPropertyYou$SacrificedThisTurn"); got != 0 {
		t.Errorf("SacrificedThisTurn after cleanup = %d, want 0", got)
	}
}

// Power Surge: the untapped lands the active player controlled as the turn
// began, before the untap step untapped the rest.
func TestNumPowerSurgeLandsIsTakenAsTheTurnBegins(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	for i := 0; i < 3; i++ {
		g.NewCard(landDef(t, "Forest", "Basic Land Forest"), p, engine.Battlefield)
	}
	tapped := g.NewCard(landDef(t, "Forest", "Basic Land Forest"), p, engine.Battlefield)
	g.Card(tapped).Tapped = true
	g.StartTurn(p, engine.NewScriptedController())
	if g.Card(tapped).Tapped {
		t.Fatal("the untap step did not untap the tapped Forest")
	}
	if got := measure(t, g, p, "PlayerCountPropertyYou$NumPowerSurgeLands"); got != 3 {
		t.Errorf("NumPowerSurgeLands = %d, want 3", got)
	}
}

// The comparisons against another player (PlayerProperty.java:319-439).
func TestPlayerComparisonProperties(t *testing.T) {
	t.Parallel()

	setup := func(t *testing.T) (*engine.Game, engine.PlayerID, engine.PlayerID) {
		g, p, other := newTwoPlayerGame(t)
		addN(t, g, p, engine.Battlefield, 1, "Creature Elf")
		addN(t, g, other, engine.Battlefield, 3, "Creature Elf")
		for i := 0; i < 4; i++ {
			g.NewCard(landDef(t, "Forest", "Basic Land Forest"), other, engine.Battlefield)
		}
		g.NewCard(landDef(t, "Forest", "Basic Land Forest"), p, engine.Battlefield)
		addN(t, g, p, engine.Hand, 2, "Artifact")
		addN(t, g, other, engine.Hand, 2, "Artifact")
		addN(t, g, p, engine.Graveyard, 1, "Creature Elf")
		g.Player(other).Life = 25
		return g, p, other
	}
	for _, tc := range []struct {
		x    string
		want int
	}{
		{"PlayerCountOpponents$HasPropertywithMoreCreaturesThanYou", 1},
		{"PlayerCountOpponents$HasPropertywithMoreLandsThanYou", 1},
		{"PlayerCountOpponents$HasPropertyhasFewerCreaturesInPlayThanYou", 0},
		{"PlayerCountOpponents$HasPropertyhasFewerCreaturesInYardThanYou", 1},
		{"PlayerCountPropertywithAtLeast2MoreLandsThanYou$Amount", 1},
		{"PlayerCountPropertywithAtLeast4MoreLandsThanYou$Amount", 0},
		{"PlayerCountOpponents$HasPropertyhasMoreLife", 1},
		{"PlayerCountOpponents$HasPropertyhasMoreCardsInHand", 0},
		{"PlayerCountPlayers$HasPropertywithMostTypeCreature", 1},
		// The meter itself is p's one artifact.
		{"PlayerCountPlayers$HasPropertywithMostTypeArtifactOnly", 1},
		// Nobody has one: Java's running maximum starts at 0, so all tie.
		{"PlayerCountPlayers$HasPropertywithMostTypeEnchantment", 2},
		{"PlayerCountPlayers$HasPropertywithMostTypeEnchantmentOnly", 0},
		{"PlayerCountPlayers$HasPropertywithMostPermanentInPlay", 1},
		// A tie for the largest hand goes to the first seat (Java's strict ">").
		{"PlayerCountPropertyYou$HasPropertywithMostCardsInHand", 1},
		{"PlayerCountPlayers$HasPropertywithLowestLife", 1},
		{"PlayerCountPlayers$HasPropertyOpponentToActive", 1},
		{"PlayerCountOpponents$HasPropertylifeGTY", 1},
		// Great Unclean One's operand is an expression, compiled at load.
		{"PlayerCountOpponents$HasPropertylifeLTCount$YourLifeTotal", 0},
		{"PlayerCountOpponents$HasPropertylifeGTCount$YourLifeTotal", 1},
	} {
		t.Run(tc.x, func(t *testing.T) {
			t.Parallel()
			g, p, _ := setup(t)
			if got := measure(t, g, p, tc.x, "Y:Count$YourLifeTotal"); got != tc.want {
				t.Errorf("%s = %d, want %d", tc.x, got, tc.want)
			}
		})
	}
}

// The other turn records: castSpellThisTurn and CardsInHandAtBeginningOfTurn
// (zero for the whole first turn, as Java leaves it).
func TestTurnRecordProperties(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.Player(other).SpellsCastThisTurn = 1
	if got := measure(t, g, p, "PlayerCountPlayers$HasPropertycastSpellThisTurn"); got != 1 {
		t.Errorf("castSpellThisTurn = %d, want 1", got)
	}
	addN(t, g, p, engine.Hand, 2, "Artifact")
	if got := measure(t, g, p, "PlayerCountPlayers$HasPropertyCardsInHandAtBeginningOfTurn"); got != 0 {
		t.Errorf("CardsInHandAtBeginningOfTurn on the first turn = %d, want 0", got)
	}
	g.SetTurnState(1, p, engine.EndOfTurn)
	g.AdvancePhase(engine.NewScriptedController())
	if got := measure(t, g, p, "PlayerCountPlayers$HasPropertyCardsInHandAtBeginningOfTurn"); got != 1 {
		t.Errorf("CardsInHandAtBeginningOfTurn after cleanup = %d, want 1 (only p held cards)", got)
	}
}

// Defined$ You.<p> and Opponent.<p> filter the You/Opponent players by the
// property (AbilityUtils.java:1173-1176, :1194-1200): Davros's "each opponent
// who lost 3 or more life this turn", Mindstorm Crown's "you, if you had
// cards in hand as the turn began".
func TestDefinedPlayerSpecsFilterByProperty(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.Player(p).LifeLostThisTurn, g.Player(other).LifeLostThisTurn = 5, 3
	host := propertyMeter(t, g, p, "LoseLife | Defined$ Opponent.LostLifeThisTurn GE3 | LifeAmount$ 1 | SubAbility$ DBYou", "1",
		"DBYou:DB$ LoseLife | Defined$ You.CardsInHandAtBeginningOfTurn | LifeAmount$ 10 | SubAbility$ DBNot",
		"DBNot:DB$ GainLife | Defined$ You.!CardsInHandAtBeginningOfTurn | LifeAmount$ 3")
	if err := activate(g, p, engine.NewScriptedController(), host); err != nil {
		t.Fatal(err)
	}
	if g.Player(other).Life != 19 || g.Player(p).Life != 23 {
		t.Errorf("life = %d/%d, want 23/19: the opponent lost 1, you had no cards in hand (gain 3, not lose 10)",
			g.Player(p).Life, g.Player(other).Life)
	}
	// Player.hasProperty's "!" negates in an amount too.
	if got := measure(t, g, p, "PlayerCountPlayers$HasProperty!LostLifeThisTurn GE5"); got != 1 {
		t.Errorf("HasProperty!LostLifeThisTurn GE5 = %d, want 1 (only the opponent, at 4, lost less than 5)", got)
	}
	if err := measureErr(t, g, p, "PlayerCountPlayers$HasProperty!NotedDefender"); err == nil {
		t.Error("HasProperty!NotedDefender resolved, want an unresolved amount")
	}
}

// What stays refused: records this port does not keep, shapes Java throws
// on or answers without comparing anything, and operands that do not resolve.
func TestPlayerPropertiesRefuseWhatTheyCannotRead(t *testing.T) {
	t.Parallel()

	for _, x := range []string{
		"PlayerCountOpponents$HasPropertywasDealtDamageThisGameBy Self",
		"PlayerCountOpponents$HasPropertywasDealtCombatDamageThisCombatBy Self",
		"PlayerCountOpponents$HasPropertywasDealtDamageLastTurn GE2",
		"PlayerCountOpponents$HasPropertywasDealtDamage",
		"PlayerCountOpponents$HasPropertywasDealtDamageThisTurnBy",
		"PlayerCountOpponents$HasPropertywasDealtDamageThisTurnBySource G",
		"PlayerCountOpponents$HasPropertywasDealtDamageThisTurnBySource GEZ",
		"PlayerCountOpponents$HasPropertyLostLifeThisTurn GEZ",
		"PlayerCountOpponents$HasPropertyLostLifeThisTurn G",
		"PlayerCountOpponents$HasPropertyLostLifeThisTurnX",
		"PlayerCountOpponents$HasPropertyHasCardsInHand_Card",
		"PlayerCountOpponents$HasPropertyHasCardsInNowhere_Card_GE1",
		"PlayerCountOpponents$HasPropertyHasCardsInHand_Card_GEZ",
		"PlayerCountOpponents$HasPropertylifeGTZ",
		"PlayerCountOpponents$HasPropertywithMoreCreatures",
		"PlayerCountOpponents$HasPropertywithAtLeastXMoreLandsThanYou",
		"PlayerCountOpponents$HasPropertywithAtLeast2",
		"PlayerCountOpponents$HasPropertyhasMoreArtifacts",
		"PlayerCountOpponents$HasPropertyhasFewerCreatures",
		"PlayerCountOpponents$HasPropertywithMostColors",
		"PlayerCountOpponents$HasPropertyNotedDefender",
		"PlayerCountPropertyYou$SacrificedPermanentTypesThisTurn",
	} {
		g, p, _ := newTwoPlayerGame(t)
		if err := measureErr(t, g, p, x); err == nil {
			t.Errorf("%s resolved, want an unresolved amount", x)
		}
	}
}

// A clone's records are its own: damage dealt and a sacrifice made on the
// clone leave the original's counts where they were.
func TestCloneKeepsItsOwnTurnRecords(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	clone := g.Clone()
	host := propertyMeter(t, clone, p, "DealDamage | Defined$ Opponent | NumDmg$ 2 | SubAbility$ DBSac", "1",
		"DBSac:DB$ Sacrifice | Defined$ You | SacValid$ Creature")
	c := engine.NewScriptedController()
	c.QueueSacrificeChoice([]engine.CardID{victim})
	if err := activate(clone, p, c, host); err != nil {
		t.Fatal(err)
	}
	if got := measure(t, clone, p, "PlayerCountPropertyYou$SacrificedThisTurn"); got != 1 {
		t.Errorf("clone SacrificedThisTurn = %d, want 1", got)
	}
	for _, x := range []string{
		"PlayerCountPropertyYou$SacrificedThisTurn",
		"PlayerCountOpponents$HasPropertywasDealtDamageThisTurnBy Artifact",
	} {
		if got := measure(t, g, p, x); got != 0 {
			t.Errorf("original %s = %d, want 0", x, got)
		}
	}
}
