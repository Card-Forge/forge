package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// This file covers the amount expressions an effect's numeric param names
// (NumCards$, NumDmg$, LifeAmount$, ...): Count$Compare, the branch heads,
// the scalar Count$ heads, and the card and player list heads whose operator
// applies inside the measurement (AbilityUtils.calculateAmount/xCount).

// fillerDef is a vanilla card of typeLine, the shape the tests below put in
// zones to change what an amount measures.
func fillerDef(t *testing.T, name, typeLine, power, toughness string) *compile.Card {
	t.Helper()
	return amountDef(t, name, typeLine, "", power, toughness, nil)
}

// gainedFrom resolves "gain X life" with X's SVar body (and any extra SVars),
// after setup, and answers the life the host's controller gained.
func gainedFrom(t *testing.T, setup func(g *engine.Game, p, other engine.PlayerID), x string, svars ...string) int {
	t.Helper()
	g, p, other := newTwoPlayerGame(t)
	if setup != nil {
		setup(g, p, other)
	}
	before := g.Player(p).Life
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ GainLife | Defined$ You | LifeAmount$ X",
		append([]string{"X", x}, svars...)...)
	return g.Player(p).Life - before
}

// gainErr resolves the same line and answers the resolution's error.
func gainErr(t *testing.T, setup func(g *engine.Game, p, other engine.PlayerID), x string, svars ...string) error {
	t.Helper()
	g, p, other := newTwoPlayerGame(t)
	if setup != nil {
		setup(g, p, other)
	}
	def := etbChainDef(t, "Test Amount", "DB$ GainLife | Defined$ You | LifeAmount$ X", append([]string{"X", x}, svars...)...)
	_, err := castETBChain(t, g, p, def, engine.NewScriptedController())
	return err
}

func addN(t *testing.T, g *engine.Game, p engine.PlayerID, zone engine.ZoneType, n int, typeLine string) []engine.CardID {
	t.Helper()
	var ids []engine.CardID
	for i := 0; i < n; i++ {
		ids = append(ids, g.NewCard(fillerDef(t, "Filler", typeLine, "2", "2"), p, zone))
	}
	return ids
}

// Count$Compare <lhs> <op><rhs>.<then>.<else> (xCount, AbilityUtils.java:
// 1608-1614): each part is itself an amount, and the comparator is read by
// containment.
func TestCountCompareChoosesItsBranch(t *testing.T) {
	t.Parallel()

	twoCreatures := func(g *engine.Game, p, _ engine.PlayerID) { addN(t, g, p, engine.Battlefield, 1, "Creature Elf") }
	for _, tc := range []struct {
		name  string
		setup func(g *engine.Game, p, other engine.PlayerID)
		x     string
		svars []string
		want  int
	}{
		{"then branch", twoCreatures, "Count$Compare Y GE2.10.1", []string{"Y", "Count$Valid Creature.YouCtrl"}, 10},
		{"else branch", nil, "Count$Compare Y GE2.10.1", []string{"Y", "Count$Valid Creature.YouCtrl"}, 1},
		{"branch is an SVar", nil, "Count$Compare Y LT1.1.Z", []string{"Y", "Count$Valid Creature.YouCtrl", "Z", "Count$YourLifeTotal"}, 20},
		{"rhs is an SVar", twoCreatures, "Count$Compare Y EQTwo.4.3", []string{"Y", "Count$Valid Creature.YouCtrl", "Two", "2"}, 4},
		{"operator after the branch", nil, "Count$Compare Y GE1.3.1/Twice", []string{"Y", "Count$Valid Creature.YouCtrl"}, 6},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			if got := gainedFrom(t, tc.setup, tc.x, tc.svars...); got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
		})
	}
}

// A param value written as an expression rather than an SVar name (Akroma's
// Will's `CharmNum$ Count$Compare Y GE1.2.1`, Edgar's `NumCards$ Count$Valid
// Artifact.YouCtrl`) is compiled at load and resolves like an SVar would.
func TestInlineExpressionParamResolves(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	addN(t, g, p, engine.Battlefield, 2, "Artifact")
	before := g.Player(p).Life
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ GainLife | Defined$ You | LifeAmount$ Count$Valid Artifact.YouCtrl | SubAbility$ DBMore",
		"DBMore", "DB$ GainLife | Defined$ You | LifeAmount$ Count$Compare Y GE2.10.0",
		"Y", "Count$Valid Artifact.YouCtrl")
	if got := g.Player(p).Life - before; got != 12 {
		t.Errorf("gained %d, want 12: two artifacts, then the Compare's 10", got)
	}
}

// The player-state branch heads pick their first branch while the flag holds
// (AbilityUtils.java:2256-2357, :2693-2697).
func TestBranchHeadsReadPlayerState(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		head  string
		setup func(t *testing.T, g *engine.Game, p, other engine.PlayerID)
	}{
		{"Hellbent", nil},
		{"Metalcraft", func(t *testing.T, g *engine.Game, p, _ engine.PlayerID) {
			addN(t, g, p, engine.Battlefield, 3, "Artifact")
		}},
		{"Threshold", func(t *testing.T, g *engine.Game, p, _ engine.PlayerID) {
			addN(t, g, p, engine.Graveyard, 7, "Artifact")
		}},
		{"Delirium", func(t *testing.T, g *engine.Game, p, _ engine.PlayerID) {
			for _, typ := range []string{"Artifact", "Creature Elf", "Land", "Instant"} {
				addN(t, g, p, engine.Graveyard, 1, typ)
			}
		}},
		{"FatefulHour", func(_ *testing.T, g *engine.Game, p, _ engine.PlayerID) { g.Player(p).Life = 5 }},
		{"Monarch", func(_ *testing.T, g *engine.Game, p, _ engine.PlayerID) { g.SetMonarch(p) }},
		{"MaxSpeed", func(_ *testing.T, g *engine.Game, p, _ engine.PlayerID) { g.Player(p).Speed = 4 }},
		{"Landfall", func(t *testing.T, g *engine.Game, p, _ engine.PlayerID) {
			g.Move(addN(t, g, p, engine.Hand, 1, "Land")[0], engine.Battlefield, p)
		}},
		{"Morbid", func(t *testing.T, g *engine.Game, _, other engine.PlayerID) {
			g.Move(addN(t, g, other, engine.Battlefield, 1, "Creature Elf")[0], engine.Graveyard, other)
		}},
	} {
		t.Run(tc.head, func(t *testing.T) {
			t.Parallel()
			x := "Count$" + tc.head + ".7.2"
			yes := func(g *engine.Game, p, other engine.PlayerID) {
				if tc.setup != nil {
					tc.setup(t, g, p, other)
				}
			}
			if got := gainedFrom(t, yes, x); got != 7 {
				t.Errorf("with the flag: gained %d, want 7", got)
			}
			if tc.setup == nil {
				// Hellbent holds on the bare game: a card in hand turns it off.
				no := func(g *engine.Game, p, _ engine.PlayerID) { addN(t, g, p, engine.Hand, 1, "Artifact") }
				if got := gainedFrom(t, no, x); got != 2 {
					t.Errorf("with a card in hand: gained %d, want 2", got)
				}
				return
			}
			if got := gainedFrom(t, nil, x); got != 2 {
				t.Errorf("without the flag: gained %d, want 2", got)
			}
		})
	}
}

// Night (Game.isNight), cast-zone and own-main-phase branches.
func TestBranchHeadsReadTheCastAndTheGame(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	before := g.Player(p).Life
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ DayTime | Value$ Night | SubAbility$ DBNight",
		"DBNight", "DB$ GainLife | Defined$ You | LifeAmount$ N | SubAbility$ DBCast",
		"DBCast", "DB$ GainLife | Defined$ You | LifeAmount$ C | SubAbility$ DBGrave",
		"DBGrave", "DB$ GainLife | Defined$ You | LifeAmount$ G | SubAbility$ DBMain",
		"DBMain", "DB$ GainLife | Defined$ You | LifeAmount$ M",
		"N", "Count$Night.100.0",
		"C", "Count$wasCastFromYourHandByYou.20.0",
		"G", "Count$wasCastFromGraveyard.0.3",
		"M", "Count$IfCastInOwnMainPhase.4.0")
	if got := g.Player(p).Life - before; got != 127 {
		t.Errorf("gained %d, want 127: night 100, cast from its owner's hand 20, not from a graveyard 3, own main phase 4", got)
	}
}

// A branch head missing a branch, an unknown cast zone, a non-numeric
// InOwnMainPhase branch, a branch naming no SVar and a Compare missing its
// comparator stay errors naming the value (GO-7).
func TestBranchHeadsRefuseMalformedShapes(t *testing.T) {
	t.Parallel()

	for _, x := range []string{
		"Count$Hellbent",
		"Count$wasCastFromNowhere.1.2",
		"Count$IfCastInOwnMainPhase.Y.1",
		"Count$Hellbent.Missing.1",
		"Count$Compare Y.1.2",
		"Count$Compare Missing GE1.1.2",
	} {
		t.Run(x, func(t *testing.T) {
			t.Parallel()
			err := gainErr(t, nil, x)
			if err == nil || !strings.Contains(err.Error(), "not resolvable") {
				t.Errorf("err = %v, want LifeAmount$ not resolvable", err)
			}
		})
	}
}

// The scalar Count$ heads read the player or the cast record directly.
func TestScalarCountHeads(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		x     string
		setup func(g *engine.Game, p, other engine.PlayerID)
		want  int
	}{
		{"Count$CardsInYourHand", func(g *engine.Game, p, _ engine.PlayerID) { addN(t, g, p, engine.Hand, 2, "Artifact") }, 2},
		{"Count$YourCountersExperience", func(g *engine.Game, p, _ engine.PlayerID) {
			g.Player(p).Counters.Add(engine.CounterType("EXPERIENCE"), 3)
		}, 3},
		{"Count$TotalOppPoisonCounters", func(g *engine.Game, _, other engine.PlayerID) {
			g.Player(other).Counters.Add(engine.Poison, 4)
		}, 4},
		{"Count$LifeOppsLostThisTurn", func(g *engine.Game, _, other engine.PlayerID) { g.Player(other).LifeLostThisTurn = 5 }, 5},
		{"Count$LifeYouLostThisTurn", func(g *engine.Game, p, _ engine.PlayerID) { g.Player(p).LifeLostThisTurn = 6 }, 6},
		{"Count$YourStartingLife", nil, 20},
		{"Count$YourSpeed", func(g *engine.Game, p, _ engine.PlayerID) { g.Player(p).Speed = 3 }, 3},
		{"Count$TopOfLibraryCMC", func(g *engine.Game, p, _ engine.PlayerID) {
			g.NewCard(amountDef(t, "Costly", "Artifact", "3 R", "", "", nil), p, engine.Library)
		}, 4},
		{"Count$TopOfLibraryCMC", nil, 0},
		{"Count$CastTotalManaSpent", nil, 1},
		{"Count$Converge", nil, 1},
		{"Count$YourLifeTotal/Minus.Remembered$Amount", nil, 20},
		{"Number$5/Minus.Remembered$Valid Card.Creature", nil, 5},
		{"Count$RememberedSize", nil, 0},
		{"Count$ImprintedSize", nil, 0},
		{"Count$ChosenSize", nil, 0},
	} {
		t.Run(tc.x, func(t *testing.T) {
			t.Parallel()
			if got := gainedFrom(t, tc.setup, tc.x); got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
		})
	}
	if got := gainedFrom(t, nil, "Count$CastTotalManaSpent Treasure"); got != 0 {
		t.Errorf("Count$CastTotalManaSpent Treasure over sourceless mana gained %d, want 0", got)
	}
}

// Count$Converge and Count$Adamant read the colored mana the spell's cast
// spent (SpellAbility.getPayingMana); Adamant only for the spell resolving.
func TestCastPaidColors(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, cost, x string
		pay           []mana.Colors
		generic       []mana.Shard
		want          int
	}{
		{"converge two colors", "1 G", "Count$Converge", []mana.Colors{mana.Green, mana.Red}, []mana.Shard{mana.ShardR}, 2},
		{"adamant held", "G G G", "Count$Adamant.Green.5.1", []mana.Colors{mana.Green, mana.Green, mana.Green}, nil, 5},
		{"adamant missed", "1 G", "Count$Adamant.Green.5.1", []mana.Colors{mana.Green, mana.Green}, []mana.Shard{mana.ShardG}, 1},
		{"adamant two", "1 G", "Count$Adamant_2.Green.5.1", []mana.Colors{mana.Green, mana.Green}, []mana.Shard{mana.ShardG}, 5},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			spell := g.NewCard(spellDefWith(t, "Test Paid", "Instant", tc.cost,
				"SP$ GainLife | Defined$ You | LifeAmount$ X", "X", tc.x), p, engine.Hand)
			for _, col := range tc.pay {
				g.Player(p).ManaPool.Add(col, 1)
			}
			c := engine.NewScriptedController()
			for _, s := range tc.generic {
				c.QueuePayGeneric(s)
			}
			before := g.Player(p).Life
			castThenResolve(t, g, p, spell, c)
			if got := g.Player(p).Life - before; got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
		})
	}
	for _, x := range []string{"Count$Adamant.Green.5.1", "Count$Adamant_X.Green.5.1", "Count$AdamantWibble.Green.5.1", "Count$Adamant.Green.5"} {
		if err := gainErr(t, nil, x); err == nil {
			t.Errorf("%s resolved off a triggered ability, want an error", x)
		}
	}
}

// handlePaid outside a layer pass reads power and toughness
// (Count$Valid ...$GreatestCardPower, Essence Harvest's shape).
func TestValidCountMeasuresLivePowerAndToughness(t *testing.T) {
	t.Parallel()

	withBears := func(g *engine.Game, p, _ engine.PlayerID) { addN(t, g, p, engine.Battlefield, 2, "Creature Elf") }
	for _, tc := range []struct {
		x    string
		want int
	}{
		{"Count$Valid Creature.YouCtrl$GreatestCardPower", 2},
		{"Count$Valid Creature.YouCtrl$LeastCardToughness", 1},
		{"Count$Valid Creature.YouCtrl$CardToughness", 5},
		{"Count$Valid Creature.YouCtrl$CardSumPT", 10},
		{"Count$Valid Creature.YouCtrl$DifferentCardPower", 2},
		{"Count$Valid Creature.YouCtrl$CardPower/Plus.1", 6},
		{"Count$ValidSelf Creature$CardPower", 1},
	} {
		t.Run(tc.x, func(t *testing.T) {
			t.Parallel()
			if got := gainedFrom(t, withBears, tc.x); got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
		})
	}
}

// Remembered$ is handlePaid over the host's remembered cards, the operator
// applied inside: to the filtered count for Valid, to each card for a
// per-card measure (AbilityUtils.java:3742-3851).
func TestRememberedListAppliesItsOperatorInside(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		x    string
		want int
	}{
		{"Remembered$Valid Creature/Plus.1", 3},
		{"Remembered$Amount", 2},
		{"RememberedLKI$CardPower/Plus.1", 6},
		{"Remembered$Valid Creature$CardToughness", 4},
		{"Remembered$CardTypes", 1},
		{"Remembered$Colors", 0},
		{"Remembered$DifferentColorPair", 0},
		{"Remembered$DifferentCardNames", 1},
	} {
		t.Run(tc.x, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			addN(t, g, other, engine.Battlefield, 2, "Creature Elf")
			before := g.Player(p).Life
			resolveLine(t, g, p, engine.NewScriptedController(),
				"DB$ DestroyAll | ValidCards$ Creature.OppCtrl | RememberDestroyed$ True | SubAbility$ DBGain",
				"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ X", "X", tc.x)
			if got := g.Player(p).Life - before; got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
		})
	}
}

// An empty list reads the operator over 0 (handlePaid's first line); a
// property this port does not measure, or an equals-matched property under an
// operator, stays an error.
func TestListMeasuresRefuseWhatTheyCannotRead(t *testing.T) {
	t.Parallel()

	if got := gainedFrom(t, nil, "Remembered$Amount/Plus.4"); got != 4 {
		t.Errorf("empty Remembered$Amount/Plus.4 gained %d, want 4", got)
	}
	for _, x := range []string{"Remembered$Colors/Twice", "Remembered$TapPowerValue", "Remembered$CreatureType", "Remembered$Valid", "Convoked$Amount"} {
		t.Run(x, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			addN(t, g, other, engine.Battlefield, 1, "Creature Elf")
			def := etbChainDef(t, "Test Refuse",
				"DB$ DestroyAll | ValidCards$ Creature.OppCtrl | RememberDestroyed$ True | SubAbility$ DBGain",
				"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ X", "X", x)
			if _, err := castETBChain(t, g, p, def, engine.NewScriptedController()); err == nil {
				t.Errorf("%s resolved, want an error", x)
			}
		})
	}
}

// PlayerCount<hType>$: the player set, then playerXCount -- the operator per
// player for a summed property, once for Amount, never for no players.
func TestPlayerCountAppliesItsOperatorPerPlayer(t *testing.T) {
	t.Parallel()

	hands := func(g *engine.Game, p, other engine.PlayerID) {
		addN(t, g, p, engine.Hand, 1, "Artifact")
		addN(t, g, other, engine.Hand, 2, "Artifact")
	}
	for _, tc := range []struct {
		x    string
		want int
	}{
		{"PlayerCountPlayers$CardsInHand/Plus.1", 5},
		{"PlayerCountOpponents$CardsInHand", 2},
		{"PlayerCountPropertyYou$CardsInHand", 1},
		{"PlayerCountYourTeam$Amount", 1},
		{"PlayerCountOther$Amount", 1},
		{"PlayerCountNonActive$Amount", 1},
		{"PlayerCountHasLost$Amount/Plus.3", 0},
		{"PlayerCountRegisteredOpponents$Amount/Twice", 2},
		{"PlayerCountRemembered$CardsInHand/Plus.5", 0},
		{"PlayerCountPlayers$HighestCardsInHand", 2},
		{"PlayerCountPlayers$LowestCardsInHand", 1},
		{"PlayerCountPlayers$TiedForHighestLife", 2},
		{"PlayerCountPlayers$TiedForLowestLife/Plus.1", 3},
		{"PlayerCountPlayers$ConditionGE2 CardsInHand", 1},
		{"PlayerCountPlayers$Condition CardsInHand/Twice", 4},
		{"PlayerCountPlayers$DamageThisTurn", 0},
		{"PlayerCountDefinedOpponent$CardsInLibrary", 0},
		{"PlayerCountPlayers$Valid Artifact.YouCtrl", 0},
		{"PlayerCountPlayers$ValidHand Artifact.YouOwn", 3},
		{"PlayerCountPropertyYou$LifeTotal", 20},
		{"PlayerCountPropertyYou$Counters.Poison", 0},
		{"PlayerCountPropertyYou$StartingLife", 20},
		{"PlayerCountPropertyYou$DomainPlayer", 0},
		{"PlayerCountPropertyYou$CreaturesInPlay", 1},
		{"PlayerCountPropertyYou$CardsInPlay", 1},
		{"PlayerCountPropertyYou$LandsInGraveyard", 0},
		{"PlayerCountPropertyYou$CardsInGraveyard", 0},
		{"PlayerCountPropertyYou$TopOfLibraryCMC", 0},
		{"PlayerCountPropertyYou$CardsDrawn", 0},
		{"PlayerCountPropertyYou$CardsDiscardedThisTurn", 0},
		{"PlayerCountPropertyYou$AttackersDeclared", 0},
		{"PlayerCountPropertyYou$LandsPlayed", 0},
		{"PlayerCountPropertyYou$SpellsCastThisTurn", 1},
		{"PlayerCountPropertyYou$LifeLostThisTurn", 0},
		{"PlayerCountPropertyYou$LifeGainedThisTurn", 0},
		{"PlayerCountPropertyYou$Speed", 0},
		{"PlayerCountPropertyYou$NonCombatDamageDealtThisTurn", 0},
		{"PlayerCountPropertyYou$RingTemptedYou", 0},
		{"PlayerCountPropertyYou$AttractionsVisitedThisTurn", 0},
		{"PlayerCountPropertyYou$Counters.ALL", 0},
		{"PlayerCountPropertyYou$ThisTurnEntered_Battlefield_Creature", 1},
	} {
		t.Run(tc.x, func(t *testing.T) {
			t.Parallel()
			if got := gainedFrom(t, hands, tc.x); got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
		})
	}
}

// A player set or property this port keeps no state for is an error, never
// Java's silent 0.
func TestPlayerCountRefusesWhatItCannotRead(t *testing.T) {
	t.Parallel()

	for _, x := range []string{
		"PlayerCountPlayers$StartingLibrarySize",
		"PlayerCountPlayers$Bogus",
		"PlayerCountPlayers$SacrificedPermanentTypesThisTurn",
		"PlayerCountPlayers$HighestCardsInHand/Twice",
		"PlayerCountPlayers$Counters",
		"PlayerCountPlayers$ConditionGE",
		"PlayerCountPlayers$ValidNowhere Card",
		"PlayerCountPlayers$ValidHand",
		"PlayerCountWibble$Amount",
		"PlayerCountRememberedBogus$Amount",
		"PlayerCountDefinedNobody$Amount",
		"TriggeredPlayer$CardsInHand",
	} {
		t.Run(x, func(t *testing.T) {
			t.Parallel()
			if err := gainErr(t, nil, x); err == nil {
				t.Errorf("%s resolved, want an error", x)
			}
		})
	}
}

// TargetedPlayer$, ThisTargetedPlayer$ and TargetedController$ measure the
// players the resolving ability's targets name (AbilityUtils.java:574-614).
func TestTargetedPlayerAmounts(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		line, x string
		target  func(g *engine.Game, p, other engine.PlayerID) engine.EntityID
		want    int
	}{
		{"DB$ GainLife | ValidTgts$ Player | LifeAmount$ X", "TargetedPlayer$CardsInHand/Plus.1",
			func(_ *engine.Game, _, other engine.PlayerID) engine.EntityID { return engine.PlayerEntity(other) }, 3},
		{"DB$ GainLife | ValidTgts$ Player | LifeAmount$ X", "ThisTargetedPlayer$Valid Artifact.YouCtrl",
			func(_ *engine.Game, _, other engine.PlayerID) engine.EntityID { return engine.PlayerEntity(other) }, 1},
		{"DB$ GainLife | ValidTgts$ Player | LifeAmount$ X", "TargetedController$CardsInHand",
			func(_ *engine.Game, _, other engine.PlayerID) engine.EntityID { return engine.PlayerEntity(other) }, 0},
	} {
		t.Run(tc.x, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			addN(t, g, other, engine.Hand, 2, "Artifact")
			addN(t, g, other, engine.Battlefield, 1, "Artifact")
			c := engine.NewScriptedController()
			c.QueueTargets([]engine.EntityID{tc.target(g, p, other)})
			before := g.Player(other).Life
			resolveLine(t, g, p, c, tc.line, "X", tc.x)
			if got := g.Player(other).Life - before; got != tc.want {
				t.Errorf("target gained %d, want %d", got, tc.want)
			}
		})
	}
}

// No ability in the chain targeting a player is Java's null targeting
// ability: TargetedPlayer$ reads no players, 0. A parent that targets only a
// card may hide a player-targeting grandparent, so that one is unresolved.
func TestTargetedPlayerWithoutAPlayerTarget(t *testing.T) {
	t.Parallel()

	if got := gainedFrom(t, nil, "TargetedPlayer$CardsInHand/Plus.2"); got != 0 {
		t.Errorf("no targeting ability: gained %d, want 0", got)
	}
	g, p, other := newTwoPlayerGame(t)
	bear := addN(t, g, other, engine.Battlefield, 1, "Creature Elf")[0]
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bear)})
	def := etbChainDef(t, "Test Parent Card",
		"DB$ Pump | ValidTgts$ Creature | NumAtt$ +1 | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ X", "X", "TargetedPlayer$CardsInHand")
	if _, err := castETBChain(t, g, p, def, c); err == nil {
		t.Error("TargetedPlayer$ under a card-targeting parent resolved, want an error")
	}
}
