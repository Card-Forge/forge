package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// This file covers the amounts that read a record of the cast or of the turn:
// a replacement's replacing object (ReplaceCount$), the turn's departures
// from the battlefield (LeftBattlefieldThisTurn, Void, Revolt), the descend
// and crime counters, the integers a card remembers (RememberedNumber), the
// snow mana a cast spent, and the Bargain and Foretell cast records.

// A damage replacement whose ReplaceWith$ ability is an ordinary effect reads
// "that much damage" through ReplaceCount$DamageAmount, and the damaged player
// as Defined$ ReplacedTarget (Angel of Suffering: prevent the damage, mill
// twice that many cards).
func TestDamageReplacementChainReadsTheDamageAndItsTarget(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, a, engine.Main1)
	g.NewCard(replacementEnchantmentDefWithSVars(t, "Test Angel of Suffering",
		"Event$ DamageDone | ValidTarget$ You | ReplaceWith$ DoubleMill | PreventionEffect$ True | Description$ Mill twice that many instead.",
		map[string]string{
			"DoubleMill": "DB$ Mill | Defined$ ReplacedTarget | NumCards$ X",
			"X":          "ReplaceCount$DamageAmount/Twice",
		}), b, engine.Battlefield)
	addN(t, g, b, engine.Library, 10, "Artifact")
	attacker := g.NewCard(creatureDefPT(t, "3", "3"), a, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, ac)
	bc := engine.NewScriptedController()
	bc.QueueBlocks(nil)
	declareBlockers(t, g, bc)
	g.DealCombatDamage(engine.NewScriptedController())
	if err := g.TakePendingError(); err != nil {
		t.Fatalf("pending error: %v", err)
	}

	if got := g.Player(b).Life; got != 20 {
		t.Errorf("defender life = %d, want 20: the damage is replaced", got)
	}
	if got := len(g.Zone(engine.Graveyard, b).Cards()); got != 6 {
		t.Errorf("defender graveyard = %d cards, want 6: twice the 3 damage", got)
	}
}

// A damaged creature is no player, so Defined$ ReplacedTarget names nobody
// (AbilityUtils.java:1104-1106 adds only a Player); outside a replacement the
// key is an error.
func TestReplacedTargetNamesOnlyADamagedPlayer(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, a, engine.Main1)
	g.NewCard(replacementEnchantmentDefWithSVars(t, "Test Creature Mill",
		"Event$ DamageDone | ValidTarget$ Creature.YouCtrl | ReplaceWith$ DoubleMill | PreventionEffect$ True | Description$ Mill instead.",
		map[string]string{
			"DoubleMill": "DB$ Mill | Defined$ ReplacedTarget | NumCards$ X",
			"X":          "ReplaceCount$DamageAmount",
		}), b, engine.Battlefield)
	addN(t, g, b, engine.Library, 5, "Artifact")
	attacker := g.NewCard(creatureDefPT(t, "3", "3"), a, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "5", "5"), b, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, ac)
	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: attacker}})
	declareBlockers(t, g, bc)
	g.DealCombatDamage(engine.NewScriptedController())
	if err := g.TakePendingError(); err != nil {
		t.Fatalf("pending error: %v", err)
	}
	if got := g.Card(blocker).Damage.Marked; got != 0 {
		t.Errorf("blocker damage = %d, want 0: the damage is replaced", got)
	}
	if got := len(g.Zone(engine.Graveyard, b).Cards()); got != 0 {
		t.Errorf("graveyard = %d cards, want 0: a damaged creature is no player to mill", got)
	}

	g2, p, _ := newTwoPlayerGame(t)
	def := etbChainDef(t, "Test Outside", "DB$ Mill | Defined$ ReplacedTarget | NumCards$ 1")
	if _, err := castETBChain(t, g2, p, def, engine.NewScriptedController()); err == nil ||
		!strings.Contains(err.Error(), "outside a replacement") {
		t.Errorf("err = %v, want ReplacedTarget outside a replacement", err)
	}
}

// Count$LeftBattlefieldThisTurn <valid> counts the turn's departures from the
// battlefield as last known there; Void holds once a nonland permanent left,
// Revolt once a permanent its controller controlled did (Game.java:1432,
// Player.java:2095).
func TestTurnDeparturesFeedLeftBattlefieldVoidAndRevolt(t *testing.T) {
	t.Parallel()

	creatureLeft := func(owner bool) func(g *engine.Game, p, other engine.PlayerID) {
		return func(g *engine.Game, p, other engine.PlayerID) {
			who := other
			if owner {
				who = p
			}
			g.Move(addN(t, g, who, engine.Battlefield, 1, "Creature Elf")[0], engine.Graveyard, who)
		}
	}
	landLeft := func(g *engine.Game, p, _ engine.PlayerID) {
		g.Move(addN(t, g, p, engine.Battlefield, 1, "Land")[0], engine.Hand, p)
	}
	handDiscard := func(g *engine.Game, p, _ engine.PlayerID) {
		g.Move(addN(t, g, p, engine.Hand, 1, "Creature Elf")[0], engine.Graveyard, p)
	}
	for _, tc := range []struct {
		name  string
		setup func(g *engine.Game, p, other engine.PlayerID)
		x     string
		want  int
	}{
		{"own creature counted", creatureLeft(true), "Count$LeftBattlefieldThisTurn Creature.YouCtrl", 1},
		{"opponent's creature not yours", creatureLeft(false), "Count$LeftBattlefieldThisTurn Creature.YouCtrl", 0},
		{"any card", landLeft, "Count$LeftBattlefieldThisTurn Card", 1},
		{"a discard never left the battlefield", handDiscard, "Count$LeftBattlefieldThisTurn Card", 0},
		{"void after a creature", creatureLeft(false), "Count$Void.3.2", 3},
		{"no void after a land", landLeft, "Count$Void.3.2", 2},
		{"no void on a quiet turn", nil, "Count$Void.3.2", 2},
		{"revolt after your permanent", landLeft, "Count$Revolt.7.1", 7},
		{"no revolt after the opponent's", creatureLeft(false), "Count$Revolt.7.1", 1},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			if got := gainedFrom(t, tc.setup, tc.x); got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
		})
	}
	if err := gainErr(t, nil, "Count$LeftBattlefieldThisTurn"); err == nil {
		t.Error("LeftBattlefieldThisTurn without a valid string resolved")
	}
}

// Count$YouDescendedThisTurn is Player.getDescended, a count of permanent
// cards put into the player's graveyard this turn (tokens and instants
// excluded), back to 0 after cleanup.
func TestDescendedCountsPermanentCardsAndResetsAtCleanup(t *testing.T) {
	t.Parallel()

	twoAndExtras := func(g *engine.Game, p, _ engine.PlayerID) {
		for _, id := range addN(t, g, p, engine.Library, 2, "Creature Elf") {
			g.Move(id, engine.Graveyard, p)
		}
		token := addN(t, g, p, engine.Battlefield, 1, "Creature Elf")[0]
		g.Card(token).IsToken = true
		g.Move(token, engine.Graveyard, p)
		g.Move(addN(t, g, p, engine.Library, 1, "Instant")[0], engine.Graveyard, p)
	}
	if got := gainedFrom(t, twoAndExtras, "Count$YouDescendedThisTurn"); got != 2 {
		t.Errorf("gained %d, want 2: two permanent cards, the token and the instant not", got)
	}

	g, p, _ := newTwoPlayerGame(t)
	twoAndExtras(g, p, engine.NoPlayer)
	g.SetTurnState(1, p, engine.EndOfTurn)
	g.AdvancePhase(engine.NewScriptedController())
	g.SetTurnState(2, p, engine.Main1)
	before := g.Player(p).Life
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ GainLife | Defined$ You | LifeAmount$ X | SubAbility$ DBMore",
		"DBMore", "DB$ GainLife | Defined$ You | LifeAmount$ 1", "X", "Count$YouDescendedThisTurn")
	if got := g.Player(p).Life - before; got != 1 {
		t.Errorf("gained %d after cleanup, want 1: the count resets", got)
	}
}

// crimeSpell is an instant that deals 1 damage to any target, then gains its
// caster X life, X a Count$CommittedCrimeThisTurn branch read as it resolves.
func crimeSpell(t *testing.T, g *engine.Game, p engine.PlayerID) engine.CardID {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Red, 1)
	return g.NewCard(spellDefWith(t, "Test Crime", "Instant", "R",
		"SP$ DealDamage | ValidTgts$ Any | NumDmg$ 1 | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ X | SubAbility$ DBAll",
		"DBAll", "DB$ GainLife | Defined$ Player.committedCrimeThisTurn | LifeAmount$ 100",
		"X", "Count$CommittedCrimeThisTurn.5.1"), p, engine.Hand)
}

// A spell or ability put on the stack targeting an opponent, or a permanent
// an opponent controls, is a crime (MagicStack.commitCrimeCheck); targeting
// your own creature is not.
func TestTargetingAnOpponentCommitsACrime(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name   string
		target func(g *engine.Game, p, other engine.PlayerID) engine.EntityID
		want   int
	}{
		{"the opponent", func(_ *engine.Game, _, other engine.PlayerID) engine.EntityID {
			return engine.PlayerEntity(other)
		}, 105},
		{"the opponent's creature", func(g *engine.Game, _, other engine.PlayerID) engine.EntityID {
			return engine.CardEntity(g.NewCard(creatureDefPT(t, "4", "4"), other, engine.Battlefield))
		}, 105},
		{"your own creature", func(g *engine.Game, p, _ engine.PlayerID) engine.EntityID {
			return engine.CardEntity(g.NewCard(creatureDefPT(t, "4", "4"), p, engine.Battlefield))
		}, 1},
		{"yourself", func(_ *engine.Game, p, _ engine.PlayerID) engine.EntityID {
			return engine.PlayerEntity(p)
		}, 0},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			spell := crimeSpell(t, g, p)
			c := engine.NewScriptedController()
			c.QueueTargets([]engine.EntityID{tc.target(g, p, other)})
			before := g.Player(p).Life
			castThenResolve(t, g, p, spell, c)
			if got := g.Player(p).Life - before; got != tc.want {
				t.Errorf("caster gained %d, want %d", got, tc.want)
			}
		})
	}
}

// A card in an opponent's graveyard is theirs to protect too: targeting it is
// a crime (a graveyard card's controller is its owner).
func TestTargetingAnOpponentsGraveyardCardCommitsACrime(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := addN(t, g, other, engine.Graveyard, 1, "Creature Elf")[0]
	g.Player(p).ManaPool.Add(mana.Black, 1)
	spell := g.NewCard(spellDefWith(t, "Test Grave Crime", "Instant", "B",
		"SP$ ChangeZone | ValidTgts$ Card | TgtZone$ Graveyard | Origin$ Graveyard | Destination$ Exile | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ X",
		"X", "Count$CommittedCrimeThisTurn.5.1"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	before := g.Player(p).Life
	castThenResolve(t, g, p, spell, c)
	if got := g.Player(p).Life - before; got != 5 {
		t.Errorf("gained %d, want 5: exiling an opponent's graveyard card is a crime", got)
	}
	if got := g.Card(victim).Zone; got != engine.Exile {
		t.Errorf("victim in %v, want Exile", got)
	}
}

// An Aura spell enchanting an opponent's creature targets it, and a counter
// spell aimed at an opponent's spell targets something they control on the
// stack: both are crimes.
func TestAuraAndStackTargetsCommitACrime(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	victim := g.NewCard(corpusCard(t, "Grizzly Bears"), other, engine.Battlefield)
	aura := g.NewCard(corpusCard(t, "Pacifism"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.White, 2)
	c := engine.NewScriptedController()
	c.QueueEnchantTarget(victim)
	c.QueuePayGeneric(mana.ShardW)
	castThenResolve(t, g, p, aura, c)
	before := g.Player(p).Life
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ GainLife | Defined$ You | LifeAmount$ X",
		"X", "Count$CommittedCrimeThisTurn.5.1")
	if got := g.Player(p).Life - before; got != 5 {
		t.Errorf("after enchanting the opponent's creature gained %d, want 5", got)
	}

	g2, p2, other2 := newTwoPlayerGameOn(t, scenarioDB(t))
	g2.SetTurnState(1, other2, engine.Main1)
	bears := g2.NewCard(corpusCard(t, "Grizzly Bears"), other2, engine.Hand)
	g2.Player(other2).ManaPool.Add(mana.Green, 2)
	oc := engine.NewScriptedController()
	oc.QueuePayGeneric(mana.ShardG)
	if !g2.CastSpell(other2, bears, oc) {
		t.Fatal("the opponent could not cast Grizzly Bears")
	}
	counter := g2.NewCard(spellDefWith(t, "Test Stack Crime", "Instant", "U",
		"SP$ Counter | TargetType$ Spell | ValidTgts$ Card | TgtPrompt$ Select target spell | SubAbility$ DBGain",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ X",
		"X", "Count$CommittedCrimeThisTurn.5.1"), p2, engine.Hand)
	g2.Player(p2).ManaPool.Add(mana.Blue, 1)
	pc := engine.NewScriptedController()
	pc.QueueTargets([]engine.EntityID{engine.CardEntity(bears)})
	before = g2.Player(p2).Life
	castThenResolve(t, g2, p2, counter, pc)
	if got := g2.Player(p2).Life - before; got != 5 {
		t.Errorf("after targeting the opponent's spell gained %d, want 5", got)
	}
	if got := g2.Card(bears).Zone; got != engine.Graveyard {
		t.Errorf("countered Grizzly Bears in %v, want Graveyard", got)
	}
}

// PutCounter's RememberAmount$ remembers the total counters put as an
// integer, which Count$RememberedNumber sums and Count$RememberedSize counts;
// equal integers collapse as in Java's set, and Cleanup clears them.
func TestRememberAmountFeedsRememberedNumber(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name  string
		first string
		want  int
	}{
		{"two counters", "2", 2 + 1 + 0},
		{"none placed", "0", 0 + 0 + 0},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			before := g.Player(p).Life
			resolveLine(t, g, p, engine.NewScriptedController(),
				"DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ "+tc.first+" | RememberAmount$ True | SubAbility$ DBNumber",
				"DBNumber", "DB$ GainLife | Defined$ You | LifeAmount$ X | SubAbility$ DBSize",
				"DBSize", "DB$ GainLife | Defined$ You | LifeAmount$ Y | SubAbility$ DBClean",
				"DBClean", "DB$ Cleanup | ClearRemembered$ True | SubAbility$ DBAfter",
				"DBAfter", "DB$ GainLife | Defined$ You | LifeAmount$ X",
				"X", "Count$RememberedNumber", "Y", "Count$RememberedSize")
			if got := g.Player(p).Life - before; got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
		})
	}

	g, p, _ := newTwoPlayerGame(t)
	before := g.Player(p).Life
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ 1 | RememberAmount$ True | SubAbility$ DBAgain",
		"DBAgain", "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ 1 | RememberAmount$ True | SubAbility$ DBNumber",
		"DBNumber", "DB$ GainLife | Defined$ You | LifeAmount$ X",
		"X", "Count$RememberedNumber")
	if got := g.Player(p).Life - before; got != 1 {
		t.Errorf("gained %d, want 1: remembering 1 twice holds one 1", got)
	}
}

// The total remembered is the table after the AddCounter replacements: with
// Hardened Scales out, one +1/+1 counter becomes two.
func TestRememberAmountCountsReplacedCounters(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.NewCard(corpusCard(t, "Hardened Scales"), p, engine.Battlefield)
	before := g.Player(p).Life
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ 1 | RememberAmount$ True | SubAbility$ DBNumber",
		"DBNumber", "DB$ GainLife | Defined$ You | LifeAmount$ X",
		"X", "Count$RememberedNumber")
	if got := g.Player(p).Life - before; got != 2 {
		t.Errorf("gained %d, want 2: Hardened Scales' extra counter is in the total", got)
	}
}

// Count$CastTotalManaSpent Snow counts the snow mana the cast spent (the
// pool's snow buckets; other restrictions read each unit's source,
// castmanasources_test.go).
func TestCastTotalManaSpentCountsSnowMana(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name string
		snow int
		want int
	}{
		{"one snow", 1, 1},
		{"two snow", 2, 2},
		{"no snow", 0, 0},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			spell := g.NewCard(spellDefWith(t, "Test Snow Spent", "Instant", "1 G",
				"SP$ GainLife | Defined$ You | LifeAmount$ X", "X", "Count$CastTotalManaSpent Snow"), p, engine.Hand)
			g.Player(p).ManaPool.AddSnow(mana.Green, tc.snow)
			g.Player(p).ManaPool.Add(mana.Green, 2-tc.snow)
			c := engine.NewScriptedController()
			c.QueuePayGeneric(mana.ShardG)
			before := g.Player(p).Life
			castThenResolve(t, g, p, spell, c)
			if got := g.Player(p).Life - before; got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
		})
	}
	if got := gainedFrom(t, nil, "Count$CastTotalManaSpent Snow"); got != 0 {
		t.Errorf("an ETB read of a creature cast with plain mana gained %d, want 0", got)
	}
}

// Bargain is an optional additional cost (GameActionUtil.java:479-481):
// sacrificing an artifact, enchantment or token as the spell is cast, read
// back by Count$Bargained and Condition$ Bargain.
func TestBargainIsAnOptionalCostTheSpellRecords(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name    string
		bargain bool
		want    int
	}{
		{"bargained", true, 5 + 10},
		{"declined", false, 3},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGame(t)
			def := spellDefWith(t, "Test Bargain", "Sorcery", "G",
				"SP$ GainLife | Defined$ You | LifeAmount$ X | SubAbility$ DBBonus",
				"DBBonus", "DB$ GainLife | Defined$ You | LifeAmount$ 10 | Condition$ Bargain",
				"X", "Count$Bargained.5.3")
			def.Faces[0].Keywords = append(def.Faces[0].Keywords, "Bargain")
			spell := g.NewCard(def, p, engine.Hand)
			fodder := addN(t, g, p, engine.Battlefield, 1, "Artifact")[0]
			g.Player(p).ManaPool.Add(mana.Green, 1)
			c := engine.NewScriptedController()
			c.QueueConfirmPayCost(tc.bargain)
			if tc.bargain {
				c.QueueSacrificeChoice([]engine.CardID{fodder})
			}
			before := g.Player(p).Life
			castThenResolve(t, g, p, spell, c)
			if got := g.Player(p).Life - before; got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
			wantZone := engine.Battlefield
			if tc.bargain {
				wantZone = engine.Graveyard
			}
			if got := g.Card(fodder).Zone; got != wantZone {
				t.Errorf("artifact in %v, want %v", got, wantZone)
			}
		})
	}
}

// Brave the Wilds: bargained, its land becomes a 3/3 creature (Animate's
// Condition$ Bargain holds, and Count$Bargained.1.0 asks for one target);
// unbargained it asks for none and the Animate does nothing. A Condition$
// value no reader evaluates stays an error.
func TestBargainGatesAnAnimate(t *testing.T) {
	t.Parallel()

	for _, bargain := range []bool{true, false} {
		g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
		g.SetTurnState(1, p, engine.Main1)
		spell := g.NewCard(corpusCard(t, "Brave the Wilds"), p, engine.Hand)
		land := g.NewCard(corpusCard(t, "Forest"), p, engine.Battlefield)
		fodder := g.NewCard(corpusCard(t, "Ornithopter"), p, engine.Battlefield)
		g.Player(p).ManaPool.Add(mana.Green, 1)
		c := engine.NewScriptedController()
		c.QueueConfirmPayCost(bargain)
		if bargain {
			c.QueueSacrificeChoice([]engine.CardID{fodder})
			c.QueueTargets([]engine.EntityID{engine.CardEntity(land)})
		}
		castThenResolve(t, g, p, spell, c)
		power, _ := g.Card(land).Power()
		if bargain && power != 3 {
			t.Errorf("bargained: the Forest's power = %d, want 3", power)
		}
		if !bargain && strings.Contains(g.Card(land).Type().String(), "Creature") {
			t.Errorf("unbargained: the Forest is %q, want no creature", g.Card(land).Type().String())
		}
	}

	g, p, _ := newTwoPlayerGame(t)
	def := etbChainDef(t, "Test Animate", "DB$ Animate | Defined$ Self | Power$ 4 | Condition$ Teamwork")
	if _, err := castETBChain(t, g, p, def, engine.NewScriptedController()); err == nil ||
		!strings.Contains(err.Error(), "Condition$") {
		t.Errorf("err = %v, want Condition$ not resolvable", err)
	}
}

// With nothing to sacrifice the Bargain cost is not offered, so the spell is
// cast without asking.
func TestBargainIsNotOfferedWithoutFodder(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	def := spellDefWith(t, "Test Bargain", "Sorcery", "G",
		"SP$ GainLife | Defined$ You | LifeAmount$ X", "X", "Count$Bargained.5.3")
	def.Faces[0].Keywords = append(def.Faces[0].Keywords, "Bargain")
	spell := g.NewCard(def, p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Green, 1)
	before := g.Player(p).Life
	castThenResolve(t, g, p, spell, engine.NewScriptedController())
	if got := g.Player(p).Life - before; got != 3 {
		t.Errorf("gained %d, want 3", got)
	}
}

// Escape (CR 702.138, GameActionUtil.java:145-169): the owner casts the card
// from the graveyard for its escape mana, exiling the other cards the cost
// names; the cast is recorded, so "Card.Self+escaped" lets Underworld
// Rage-Hound escape with its +1/+1 counter. Without enough other cards the
// escape is not offered.
func TestEscapeCastsFromTheGraveyardAndIsRecorded(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	hound := g.NewCard(corpusCard(t, "Underworld Rage-Hound"), p, engine.Graveyard)
	others := addN(t, g, p, engine.Graveyard, 2, "Artifact")
	g.Player(p).ManaPool.Add(mana.Red, 4)
	c := engine.NewScriptedController()
	for i := 0; i < 3; i++ {
		c.QueuePayGeneric(mana.ShardR)
	}
	if g.CastSpell(p, hound, c) {
		t.Fatal("escaped with two other cards in the graveyard; the cost exiles three")
	}
	others = append(others, addN(t, g, p, engine.Graveyard, 1, "Artifact")...)
	c.QueueCardChoice(others)
	castThenResolve(t, g, p, hound, c)
	if got := g.Card(hound).Zone; got != engine.Battlefield {
		t.Fatalf("hound in %v, want Battlefield", got)
	}
	if got := g.Card(hound).Counters.Count(engine.P1P1); got != 1 {
		t.Errorf("hound has %d +1/+1 counters, want 1: it escaped", got)
	}
	for _, id := range others {
		if got := g.Card(id).Zone; got != engine.Exile {
			t.Errorf("escape fodder in %v, want Exile", got)
		}
	}

	g2, p2, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g2.SetTurnState(1, p2, engine.Main1)
	fromHand := g2.NewCard(corpusCard(t, "Underworld Rage-Hound"), p2, engine.Hand)
	g2.Player(p2).ManaPool.Add(mana.Red, 2)
	c2 := engine.NewScriptedController()
	c2.QueuePayGeneric(mana.ShardR)
	castThenResolve(t, g2, p2, fromHand, c2)
	if got := g2.Card(fromHand).Counters.Count(engine.P1P1); got != 0 {
		t.Errorf("hound cast from hand has %d counters, want 0", got)
	}
}

// Count$Escaped.<then>.<else> reads the same record: the escaped spell's
// own branch.
func TestEscapedBranchReadsTheCast(t *testing.T) {
	t.Parallel()

	for _, escape := range []bool{true, false} {
		g, p, _ := newTwoPlayerGame(t)
		def := spellDefWith(t, "Test Escaper", "Sorcery", "1 G",
			"SP$ GainLife | Defined$ You | LifeAmount$ X", "X", "Count$Escaped.9.2")
		def.Faces[0].Keywords = append(def.Faces[0].Keywords, "Escape:G ExileFromGrave<1/Card.Other/other>")
		zone := engine.Hand
		if escape {
			zone = engine.Graveyard
		}
		spell := g.NewCard(def, p, zone)
		fodder := addN(t, g, p, engine.Graveyard, 1, "Artifact")
		g.Player(p).ManaPool.Add(mana.Green, 2)
		c := engine.NewScriptedController()
		if escape {
			c.QueueCardChoice(fodder)
		} else {
			c.QueuePayGeneric(mana.ShardG)
		}
		before := g.Player(p).Life
		castThenResolve(t, g, p, spell, c)
		want := 2
		if escape {
			want = 9
		}
		if got := g.Player(p).Life - before; got != want {
			t.Errorf("escape=%v: gained %d, want %d", escape, got, want)
		}
	}
}

// A spell cast for its foretell cost is foretold (Card.isForetold through its
// cast SpellAbility): Count$Foretold and Condition$ Foretold read it; the same
// card cast from hand is not.
func TestForetoldCastIsRecorded(t *testing.T) {
	t.Parallel()

	foretoldSpell := func(t *testing.T, g *engine.Game, p engine.PlayerID) engine.CardID {
		t.Helper()
		def := spellDefWith(t, "Test Foretold", "Sorcery", "2 G",
			"SP$ GainLife | Defined$ You | LifeAmount$ X | SubAbility$ DBBonus",
			"DBBonus", "DB$ GainLife | Defined$ You | LifeAmount$ 10 | Condition$ Foretold",
			"X", "Count$Foretold.5.1")
		def.Faces[0].Keywords = append(def.Faces[0].Keywords, "Foretell:G")
		return g.NewCard(def, p, engine.Hand)
	}

	g, p, other := newTwoPlayerGame(t)
	spell := foretoldSpell(t, g, p)
	c := engine.NewScriptedController()
	g.Player(p).ManaPool.Add(mana.Green, 2)
	c.QueuePayGeneric(mana.ShardG)
	c.QueuePayGeneric(mana.ShardG)
	if !g.Foretell(p, spell, c) {
		t.Fatal("could not foretell")
	}
	g.SetTurnState(2, other, engine.Main1)
	g.SetTurnState(3, p, engine.Main1)
	g.Player(p).ManaPool.Add(mana.Green, 1)
	before := g.Player(p).Life
	castThenResolve(t, g, p, spell, c)
	if got := g.Player(p).Life - before; got != 15 {
		t.Errorf("foretold cast gained %d, want 15", got)
	}

	g2, p2, _ := newTwoPlayerGame(t)
	fromHand := foretoldSpell(t, g2, p2)
	g2.Player(p2).ManaPool.Add(mana.Green, 3)
	c2 := engine.NewScriptedController()
	c2.QueuePayGeneric(mana.ShardG)
	c2.QueuePayGeneric(mana.ShardG)
	before = g2.Player(p2).Life
	castThenResolve(t, g2, p2, fromHand, c2)
	if got := g2.Player(p2).Life - before; got != 1 {
		t.Errorf("cast from hand gained %d, want 1", got)
	}
}
