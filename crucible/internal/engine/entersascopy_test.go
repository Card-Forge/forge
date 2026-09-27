package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// These tests cover "enters as a copy" (CR 614.1c, 707.2): a Copy-layer
// replacement of a card's entry onto the battlefield, from a
// K:ETBReplacement:Copy keyword or an R: line, applied before every other
// replacement and before any ETB trigger looks at the card.

// testCloneDef is Clone itself: a 0/0 that may enter as a copy of another
// creature on the battlefield. choices is its Choices$.
func testCloneDef(t *testing.T, name, choices string, extra ...string) *compile.Card {
	t.Helper()
	lines := append([]string{
		"Cost:G",
		"K:ETBReplacement:Copy:DBCopy:Optional",
		"SVar:DBCopy:DB$ Clone | Choices$ " + choices,
	}, extra...)
	return copyTestDef(t, name, "Creature Shapeshifter", "0", "0", lines...)
}

// castAndResolve casts id from p's hand with one green mana and resolves
// the stack, returning ResolveStack's error.
func castAndResolve(t *testing.T, g *engine.Game, p engine.PlayerID, c engine.PlayerController, id engine.CardID) error {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.Green, 1)
	if !g.CastSpell(p, id, c) {
		t.Fatalf("CastSpell(%d) failed", id)
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

func mustCastAndResolve(t *testing.T, g *engine.Game, p engine.PlayerID, c engine.PlayerController, id engine.CardID) {
	t.Helper()
	if err := castAndResolve(t, g, p, c, id); err != nil {
		t.Fatal(err)
	}
}

// TestEntersAsCopyCloneCopiesTheChosenCreature proves the dominant shape
// (Clone, Phyrexian Metamorph, 60-odd cards): the controller agrees, picks
// among the other creatures on the battlefield -- never the entering card
// itself -- and the card enters as a permanent copy under its caster's
// control.
func TestEntersAsCopyCloneCopiesTheChosenCreature(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	clone := g.NewCard(testCloneDef(t, "Test Clone", "Creature.Other"), p, engine.Hand)
	sc := engine.NewScriptedController()
	sc.QueueConfirmEffect(true)
	sc.QueueCardChoice([]engine.CardID{giant})
	rec := &offerRecorder{ScriptedController: sc}
	mustCastAndResolve(t, g, p, rec, clone)

	c := g.Card(clone)
	if c.Zone != engine.Battlefield || !c.IsCopy() || c.Def.Name != "Copied Giant" || !c.HasKeyword("Reach") {
		t.Fatalf("clone is %q in %v (copy %v), want a copy of Copied Giant on the battlefield", c.Def.Name, c.Zone, c.IsCopy())
	}
	if c.UncopiedDef().Name != "Test Clone" || c.Controller() != p {
		t.Errorf("clone's own name %q, controller %v; want Test Clone under %v", c.UncopiedDef().Name, c.Controller(), p)
	}
	wantPT(t, g, clone, 4, 5)
	if len(rec.offers) != 1 || !containsID(rec.offers[0], giant) || containsID(rec.offers[0], clone) {
		t.Errorf("offered %v, want the giant and never the entering clone", rec.offers)
	}
}

// TestEntersAsCopyDeclinedEntersAsItself proves Optional$: declining leaves
// the card entering as itself -- a 0/0 Clone, which the next state-based
// action check puts into its owner's graveyard.
func TestEntersAsCopyDeclinedEntersAsItself(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(giantDef(t), other, engine.Battlefield)
	clone := g.NewCard(testCloneDef(t, "Test Clone", "Creature.Other"), p, engine.Hand)
	sc := engine.NewScriptedController()
	sc.QueueConfirmEffect(false)
	mustCastAndResolve(t, g, p, sc, clone)

	if g.Card(clone).IsCopy() {
		t.Fatal("a declined copy replacement still copied")
	}
	engine.CheckStateBasedActions(g, sc)
	if z := g.Card(clone).Zone; z != engine.Graveyard {
		t.Errorf("uncopied 0/0 clone is in %v, want Graveyard", z)
	}
}

// TestEntersAsCopyNeverOffersTheEnteringCard proves Java's last-battlefield
// filter (CloneEffect's isReplacementAbility branch): with Choices$
// Creature and no Other, and no other creature in play, nothing is offered
// at all -- the entering card is not yet on the battlefield to be copied.
func TestEntersAsCopyNeverOffersTheEnteringCard(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	clone := g.NewCard(testCloneDef(t, "Test Mirror", "Creature"), p, engine.Hand)
	sc := engine.NewScriptedController()
	sc.QueueConfirmEffect(true)
	rec := &offerRecorder{ScriptedController: sc}
	mustCastAndResolve(t, g, p, rec, clone)

	if g.Card(clone).IsCopy() || len(rec.offers) != 0 {
		t.Errorf("copy %v, offers %v: want no offer and no copy", g.Card(clone).IsCopy(), rec.offers)
	}
}

// TestEntersAsCopyGetsTheCopiedETBTrigger proves the copy applies before
// the ETB trigger check (CR 614.12 before CR 603.2): a Clone entering as a
// copy of a creature with "when this enters, you gain 3 life" triggers it.
func TestEntersAsCopyGetsTheCopiedETBTrigger(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	healer := g.NewCard(copyTestDef(t, "Test Healer", "Creature Elf", "2", "2",
		"T:Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigGain | TriggerDescription$ When CARDNAME enters, you gain 3 life.",
		"SVar:TrigGain:DB$ GainLife | LifeAmount$ 3"), other, engine.Battlefield)
	clone := g.NewCard(testCloneDef(t, "Test Clone", "Creature.Other"), p, engine.Hand)
	sc := engine.NewScriptedController()
	sc.QueueConfirmEffect(true)
	sc.QueueCardChoice([]engine.CardID{healer})
	mustCastAndResolve(t, g, p, sc, clone)

	if got := g.Player(p).Life; got != 23 {
		t.Errorf("caster's life = %d, want 23: the copied ETB trigger did not fire for the clone", got)
	}
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("healer's controller's life = %d, want 20", got)
	}
}

// TestEntersAsCopyOfATaplandEntersTapped proves the land-play site and CR
// 614.12's re-scan: Vesuva entering as a copy of a land that enters tapped
// has that land's "enters tapped" replacement, and IntoPlayTapped$ is not
// needed for it.
func TestEntersAsCopyOfATaplandEntersTapped(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	tapland := g.NewCard(copyTestDef(t, "Test Tapland", "Land", "", "",
		"R:Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | ReplaceWith$ ETBTapped | Description$ CARDNAME enters tapped.",
		"SVar:ETBTapped:DB$ Tap | Defined$ Self | ETB$ True"), p, engine.Battlefield)
	vesuva := g.NewCard(copyTestDef(t, "Test Vesuva", "Land", "", "",
		"K:ETBReplacement:Copy:DBCopy:Optional",
		"SVar:DBCopy:DB$ Clone | Choices$ Land.Other"), p, engine.Hand)
	sc := engine.NewScriptedController()
	sc.QueueConfirmEffect(true)
	sc.QueueCardChoice([]engine.CardID{tapland})
	if !g.PlayLand(p, vesuva, sc) {
		t.Fatal("PlayLand failed")
	}
	if err := g.TakePendingError(); err != nil {
		t.Fatal(err)
	}
	v := g.Card(vesuva)
	if v.Def.Name != "Test Tapland" || !v.Tapped {
		t.Errorf("vesuva is %q, tapped %v; want a tapped copy of Test Tapland", v.Def.Name, v.Tapped)
	}
}

// TestEntersAsCopyFromAnotherPermanent proves the watcher shape (Essence of
// the Wild): a Mandatory replacement on a permanent already in play makes
// each other creature its controller casts enter as a copy of it, asking
// nothing, through Defined$ Self and CloneTarget$ ReplacedCard. An
// opponent's creature is untouched (YouCtrl).
func TestEntersAsCopyFromAnotherPermanent(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(copyTestDef(t, "Test Essence", "Creature Elf", "6", "6",
		"K:ETBReplacement:Copy:EssenceClone:Mandatory:Battlefield:Creature.Other+YouCtrl",
		"SVar:EssenceClone:DB$ Clone | Defined$ Self | CloneTarget$ ReplacedCard"), p, engine.Battlefield)
	bear := g.NewCard(copyTestDef(t, "Test Bear", "Creature Elf", "2", "2", "Cost:G"), p, engine.Hand)
	sc := engine.NewScriptedController()
	mustCastAndResolve(t, g, p, sc, bear)

	if b := g.Card(bear); b.Def.Name != "Test Essence" || !b.IsCopy() {
		t.Errorf("bear entered as %q (copy %v), want a copy of Test Essence", b.Def.Name, b.IsCopy())
	}
	wantPT(t, g, bear, 6, 6)

	theirs := g.NewCard(copyTestDef(t, "Their Bear", "Creature Elf", "2", "2", "Cost:G"), other, engine.Hand)
	g.SetTurnState(1, other, engine.Main1)
	mustCastAndResolve(t, g, other, sc, theirs)
	if g.Card(theirs).IsCopy() {
		t.Error("an opponent's creature entered as a copy of a YouCtrl watcher's host")
	}
}

// TestEntersAsCopyReappliesForTheCopiedReplacement proves the Copy layer
// gathers its candidates again after each copy (ReplacementHandler.run's
// Updated re-run): a Body Double entering as a copy of a Clone card in a
// graveyard gains Clone's own "enters as a copy" replacement, which then
// applies too, so it ends up a copy of the giant.
func TestEntersAsCopyReappliesForTheCopiedReplacement(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	deadClone := g.NewCard(testCloneDef(t, "Test Clone", "Creature.Other"), other, engine.Graveyard)
	double := g.NewCard(testCloneDef(t, "Test Body Double", "Creature.Other | ChoiceZone$ Graveyard"), p, engine.Hand)
	sc := engine.NewScriptedController()
	sc.QueueConfirmEffect(true)
	sc.QueueCardChoice([]engine.CardID{deadClone})
	sc.QueueConfirmEffect(true)
	sc.QueueCardChoice([]engine.CardID{giant})
	mustCastAndResolve(t, g, p, sc, double)

	d := g.Card(double)
	if d.Def.Name != "Copied Giant" || d.UncopiedDef().Name != "Test Body Double" {
		t.Errorf("body double is %q (own %q), want a copy of Copied Giant", d.Def.Name, d.UncopiedDef().Name)
	}
	wantPT(t, g, double, 4, 5)
}

// TestEntersAsCopyRejectsWhatItCannotRun proves the fail-closed half: each
// shape this port cannot resolve is an error from the resolution that put
// the card onto the battlefield, and the card is not half copied.
func TestEntersAsCopyRejectsWhatItCannotRun(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name  string
		setup func(t *testing.T, g *engine.Game, p engine.PlayerID) *compile.Card
		want  string
	}{
		{
			name: "two copy replacements at once need CR 616.1's choice",
			setup: func(t *testing.T, g *engine.Game, p engine.PlayerID) *compile.Card {
				g.NewCard(copyTestDef(t, "Test Essence", "Creature Elf", "6", "6",
					"K:ETBReplacement:Copy:EssenceClone:Mandatory:Battlefield:Creature.Other+YouCtrl",
					"SVar:EssenceClone:DB$ Clone | Defined$ Self | CloneTarget$ ReplacedCard"), p, engine.Battlefield)
				return testCloneDef(t, "Test Clone", "Creature.Other")
			},
			want: "CR 616.1",
		},
		{
			name: "a ReplaceWith$ other than Clone",
			setup: func(t *testing.T, _ *engine.Game, _ engine.PlayerID) *compile.Card {
				return copyTestDef(t, "Test Mimeo", "Creature Shapeshifter", "0", "0", "Cost:G",
					"K:ETBReplacement:Copy:DBChoose:Optional",
					"SVar:DBChoose:DB$ ChooseCard | Choices$ Creature | ChoiceZone$ Graveyard")
			},
			want: "ReplaceWith$ ChooseCard not resolvable yet",
		},
		{
			name: "an Effect replacing the same entry",
			setup: func(t *testing.T, _ *engine.Game, _ engine.PlayerID) *compile.Card {
				return testCloneDef(t, "Test Spark", "Creature.Other | SubAbility$ DBEffect",
					"SVar:DBEffect:DB$ Effect | ReplacementEffects$ ETBCounter",
					"SVar:ETBCounter:Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | ReplaceWith$ DBCounter | Description$ x",
					"SVar:DBCounter:DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | ETB$ True")
			},
			want: "an Effect replacing the entry",
		},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			g.NewCard(giantDef(t), other, engine.Battlefield)
			card := g.NewCard(tc.setup(t, g, p), p, engine.Hand)
			err := castAndResolve(t, g, p, engine.NewScriptedController(), card)
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Fatalf("err = %v, want one containing %q", err, tc.want)
			}
			if g.Card(card).IsCopy() {
				t.Error("a rejected copy replacement still copied")
			}
		})
	}
}

// TestEntersAsCopyGainsTheAddedTraits proves AddTriggers$, AddAbilities$ and
// AddStaticAbilities$ (Phantasmal Image, Evil Twin, Sakashima): the traits
// the named SVars hold join the copied values, so an added ETB trigger
// fires for the entering copy, an added static applies to it, and an added
// activated ability is on it -- while the copied card's own definition
// gains none of them.
func TestEntersAsCopyGainsTheAddedTraits(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	giant := g.NewCard(giantDef(t), other, engine.Battlefield)
	clone := g.NewCard(testCloneDef(t, "Test Twin",
		"Creature.Other | AddTriggers$ TrigETB | AddAbilities$ ABPump | AddStaticAbilities$ STBig",
		"SVar:TrigETB:Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigGain | TriggerDescription$ When this enters, you gain 1 life.",
		"SVar:TrigGain:DB$ GainLife | LifeAmount$ 1",
		"SVar:ABPump:AB$ Pump | Cost$ G | Defined$ Self | NumAtt$ +1 | SpellDescription$ This gets +1/+0.",
		"SVar:STBig:Mode$ Continuous | Affected$ Card.Self | AddPower$ 1 | AddToughness$ 1 | Description$ This gets +1/+1."),
		p, engine.Hand)
	sc := engine.NewScriptedController()
	sc.QueueConfirmEffect(true)
	sc.QueueCardChoice([]engine.CardID{giant})
	mustCastAndResolve(t, g, p, sc, clone)
	engine.CheckStateBasedActions(g, sc)

	c := g.Card(clone)
	if c.Def.Name != "Copied Giant" {
		t.Fatalf("clone is %q, want a copy of Copied Giant", c.Def.Name)
	}
	if got := g.Player(p).Life; got != 21 {
		t.Errorf("life = %d, want 21: the added ETB trigger did not fire", got)
	}
	wantPT(t, g, clone, 5, 6)
	abilities := c.Def.Faces[0].Abilities
	if len(abilities) != 1 || abilities[0].Name != "Pump" {
		t.Errorf("copy's abilities = %v, want the added Pump", abilities)
	}
	if gf := g.Card(giant).Def.Faces[0]; len(gf.Abilities)+len(gf.Triggers)+len(gf.Statics) != 0 {
		t.Error("the added traits reached the copied card's own definition")
	}
}
