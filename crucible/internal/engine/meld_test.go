package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// These tests cover MeldEffect.java (CR 712, ADR-0032) through the real
// corpus meld pairs: two cards exiled and returned as one permanent, the
// other card held in no zone's card list, and the pair splitting back apart
// when the permanent leaves the battlefield (CR 712.4c).

const (
	gisela  = "Gisela, the Broken Blade"
	bruna   = "Bruna, the Fading Light"
	brisela = "Brisela, Voice of Nightmares"
)

// meldAbilityOf is the first Meld ability anywhere on def's front face: an
// A: line, or reached from a trigger or ability through its sub-abilities.
func meldAbilityOf(t *testing.T, def *compile.Card) *compile.Ability {
	t.Helper()
	var walk func(a *compile.Ability) *compile.Ability
	walk = func(a *compile.Ability) *compile.Ability {
		if a.Name == "Meld" {
			return a
		}
		for _, s := range a.Subs {
			if found := walk(s.Ability); found != nil {
				return found
			}
		}
		return nil
	}
	face := def.Faces[0]
	for _, list := range [][]*compile.Ability{face.Abilities, face.Triggers} {
		for _, a := range list {
			if found := walk(a); found != nil {
				return found
			}
		}
	}
	t.Fatalf("%s has no Meld ability", def.Name)
	return nil
}

// resolveMeldOf puts host's own Meld ability on the stack as p's and
// resolves the stack.
func resolveMeldOf(t *testing.T, g *engine.Game, p engine.PlayerID, host engine.CardID, c *engine.ScriptedController) error {
	t.Helper()
	def := g.Card(host).Def
	g.PushAbility(engine.Ability{API: engine.APIMeld, Source: host, Controller: p, Params: meldAbilityOf(t, def), Amounts: def.Faces[0].Amounts})
	return g.ResolveStack(engine.NewRegistry(), c)
}

// meldedPair is a two-player game in which p owns and controls primary and
// secondary on the battlefield, and primary's Meld ability has resolved.
func meldedPair(t *testing.T, primary, secondary string) (*engine.Game, engine.PlayerID, engine.PlayerID, engine.CardID, engine.CardID) {
	t.Helper()
	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	prim := g.NewCard(corpusCard(t, primary), p, engine.Battlefield)
	sec := g.NewCard(corpusCard(t, secondary), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{sec})
	if err := resolveMeldOf(t, g, p, prim, c); err != nil {
		t.Fatalf("Meld: %v", err)
	}
	if g.Card(prim).Zone == engine.Battlefield && g.Card(prim).MeldedWith != sec {
		t.Fatalf("%s did not meld with %s", primary, secondary)
	}
	return g, p, other, prim, sec
}

// countValid counts p's battlefield cards matching spec, Count$Valid's own
// walk.
func countValid(g *engine.Game, p engine.PlayerID, spec string) int {
	n := 0
	for _, pid := range g.Players() {
		for _, id := range g.Zone(engine.Battlefield, pid).Cards() {
			if engine.Matches(g, g.Card(id), valid.Parse(spec), p, engine.NoCard) {
				n++
			}
		}
	}
	return n
}

// TestMeldGiselaAndBrunaAtEndStep plays Gisela's own end-step trigger
// through the turn driver: its IsPresent2$ named<Name> condition holds, both
// cards are exiled and Gisela returns as Brisela, the one creature p
// controls, with Bruna folded into it and in no zone's card list.
func TestMeldGiselaAndBrunaAtEndStep(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main2)
	gis := g.NewCard(corpusCard(t, gisela), p, engine.Battlefield)
	bru := g.NewCard(corpusCard(t, bruna), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{bru})

	if err := g.Step(engine.NewRegistry(), c); err != nil {
		t.Fatalf("Step: %v", err)
	}

	gc, bc := g.Card(gis), g.Card(bru)
	if gc.Zone != engine.Battlefield || gc.Def.Name != brisela || gc.MeldedWith != bru {
		t.Fatalf("Gisela zone %v face %q melded with %v, want Brisela on the battlefield melded with Bruna", gc.Zone, gc.Def.Name, gc.MeldedWith)
	}
	if pw, _ := gc.Power(); pw != 9 {
		t.Errorf("Brisela power = %d, want 9", pw)
	}
	if th, _ := gc.Toughness(); th != 10 {
		t.Errorf("Brisela toughness = %d, want 10", th)
	}
	if !bc.Melded || bc.Zone != engine.Battlefield {
		t.Errorf("Bruna melded %v zone %v, want melded on the battlefield", bc.Melded, bc.Zone)
	}
	bf := g.Zone(engine.Battlefield, p)
	if bf.Contains(bru) || bf.Len() != 1 || len(bf.CardsIncludingPhasedOut()) != 1 {
		t.Errorf("battlefield %v, want only Brisela in its card list", bf.Cards())
	}
	if n := countValid(g, p, "Creature.YouCtrl"); n != 1 {
		t.Errorf("Count$Valid Creature.YouCtrl = %d, want 1", n)
	}
	if n := g.Zone(engine.Exile, p).Len(); n != 0 {
		t.Errorf("exile holds %d cards, want 0", n)
	}
}

// TestMeldedPermanentSplitsIntoGraveyardOnDestroy is CR 712.4c: destroyed,
// the melded permanent puts both cards into their owner's graveyard, the
// melded card first; each is its own front face again, and its last-known
// information is still Brisela's for its dies triggers.
func TestMeldedPermanentSplitsIntoGraveyardOnDestroy(t *testing.T) {
	t.Parallel()

	g, p, _, gis, bru := meldedPair(t, gisela, bruna)
	if _, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(gis)}, "DB$ Destroy | ValidTgts$ Creature"); err != nil {
		t.Fatalf("Destroy: %v", err)
	}

	grave := g.Zone(engine.Graveyard, p).Cards()
	if len(grave) != 2 || grave[0] != gis || grave[1] != bru {
		t.Fatalf("graveyard %v, want [Gisela Bruna]", grave)
	}
	gc, bc := g.Card(gis), g.Card(bru)
	if gc.Def.Name != gisela || gc.MeldedWith != engine.NoCard || bc.Melded {
		t.Errorf("after the split: Gisela face %q melded with %v, Bruna melded %v", gc.Def.Name, gc.MeldedWith, bc.Melded)
	}
	if lki := g.LKI(gis); lki == nil || lki.Def.Name != brisela || lki.MeldedWith != bru {
		t.Errorf("Gisela's last-known information %+v, want Brisela melded with Bruna", lki)
	}
	if bf := g.Zone(engine.Battlefield, p); bf.Contains(gis) || bf.Contains(bru) {
		t.Errorf("battlefield %v still holds Gisela or Bruna", bf.Cards())
	}
}

// TestMeldedPermanentSplitsOntoLibraryTop: put on top of its owner's
// library, the melded card lands over the melded permanent's own card --
// Java's changeZone at the same position, after it (GameAction.java:640).
func TestMeldedPermanentSplitsOntoLibraryTop(t *testing.T) {
	t.Parallel()

	g, p, _, gis, bru := meldedPair(t, gisela, bruna)
	g.MoveToLibraryTop(gis, p)

	lib := g.Zone(engine.Library, p).Cards()
	if len(lib) < 2 || lib[0] != bru || lib[1] != gis {
		t.Fatalf("library %v, want Bruna then Gisela on top", lib)
	}
	if g.Card(gis).Def.Name != gisela || g.Card(bru).Melded {
		t.Error("the pair did not split back into Gisela and Bruna")
	}
}

// TestMeldedPermanentSplitsIntoHand: bounced, both cards go to their
// owner's hand.
func TestMeldedPermanentSplitsIntoHand(t *testing.T) {
	t.Parallel()

	g, p, _, gis, bru := meldedPair(t, gisela, bruna)
	g.Move(gis, engine.Hand, p)

	hand := g.Zone(engine.Hand, p).Cards()
	if len(hand) != 2 || hand[0] != gis || hand[1] != bru {
		t.Fatalf("hand %v, want [Gisela Bruna]", hand)
	}
	if g.Card(bru).Zone != engine.Hand {
		t.Errorf("Bruna zone %v, want Hand", g.Card(bru).Zone)
	}
}

// TestMeldedCardMovedAloneIsNotMovedAgain: a melded card some stale
// reference moves on its own is no longer melded, so the split leaves it
// where it went.
func TestMeldedCardMovedAloneIsNotMovedAgain(t *testing.T) {
	t.Parallel()

	g, p, _, gis, bru := meldedPair(t, gisela, bruna)
	g.Move(bru, engine.Exile, p)
	g.Move(gis, engine.Graveyard, p)

	if g.Card(bru).Zone != engine.Exile || g.Card(gis).Zone != engine.Graveyard {
		t.Errorf("Bruna in %v, Gisela in %v, want Exile and Graveyard", g.Card(bru).Zone, g.Card(gis).Zone)
	}
}

// TestMeldLegendRuleSeesOnlyTheMeldedPermanent: a second Bruna next to the
// melded Brisela shares no name with any permanent, so the legend rule
// (CR 704.5j) asks nothing -- the scripted controller has no answer queued.
func TestMeldLegendRuleSeesOnlyTheMeldedPermanent(t *testing.T) {
	t.Parallel()

	g, p, _, gis, _ := meldedPair(t, gisela, bruna)
	second := g.NewCard(corpusCard(t, bruna), p, engine.Battlefield)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if g.Card(second).Zone != engine.Battlefield || g.Card(gis).Zone != engine.Battlefield {
		t.Errorf("second Bruna in %v, Brisela in %v, want both on the battlefield", g.Card(second).Zone, g.Card(gis).Zone)
	}
}

// TestMeldUrzaEntersWithMeldFaceLoyalty: Urza, Lord Protector's AB$ Meld
// with SecondaryType$ Artifact returns Urza, Planeswalker with its own
// starting loyalty (CR 306.5b), and a creature-typed filter would have found
// nothing.
func TestMeldUrzaEntersWithMeldFaceLoyalty(t *testing.T) {
	t.Parallel()

	g, _, _, urza, stones := meldedPair(t, "Urza, Lord Protector", "The Mightstone and Weakstone")
	uc := g.Card(urza)
	if uc.Def.Name != "Urza, Planeswalker" || uc.Counters.Count(engine.Loyalty) != 7 {
		t.Errorf("Urza face %q loyalty %d, want Urza, Planeswalker with 7", uc.Def.Name, uc.Counters.Count(engine.Loyalty))
	}
	if !g.Card(stones).Melded {
		t.Error("The Mightstone and Weakstone is not melded")
	}
}

// TestMeldUrzaThroughActivation activates Urza, Lord Protector's own
// {7} AB$ Meld at sorcery speed: the paid Cost$ is the host's own A: line,
// so the meld resolves rather than being rejected as an unpaid cost.
func TestMeldUrzaThroughActivation(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	urza := g.NewCard(corpusCard(t, "Urza, Lord Protector"), p, engine.Battlefield)
	stones := g.NewCard(corpusCard(t, "The Mightstone and Weakstone"), p, engine.Battlefield)
	g.Player(p).ManaPool.AddColorless(7)
	c := engine.NewScriptedController()
	for i := 0; i < 7; i++ {
		c.QueuePayGeneric(mana.ShardC)
	}
	c.QueueCardChoice([]engine.CardID{stones})

	index := -1
	for i, ab := range g.Card(urza).Def.Faces[0].Abilities {
		if ab.Name == "Meld" {
			index = i
		}
	}
	if !g.ActivateAbility(p, urza, index, c) {
		t.Fatal("ActivateAbility returned false, want true")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if uc := g.Card(urza); uc.Def.Name != "Urza, Planeswalker" || uc.MeldedWith != stones || g.Player(p).ManaPool.Total() != 0 {
		t.Errorf("Urza face %q melded with %v, pool %d, want Urza, Planeswalker melded with the stones and {7} spent",
			uc.Def.Name, uc.MeldedWith, g.Player(p).ManaPool.Total())
	}
}

// TestMeldTitaniaTakesALandSecondary: SecondaryType$ Land finds Argoth, and
// Titania, Gaea Incarnate's power and toughness -- the number of lands p
// controls -- count p's two Forests but not the melded Argoth.
func TestMeldTitaniaTakesALandSecondary(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	titania := g.NewCard(corpusCard(t, "Titania, Voice of Gaea"), p, engine.Battlefield)
	argoth := g.NewCard(corpusCard(t, "Argoth, Sanctum of Nature"), p, engine.Battlefield)
	for i := 0; i < 2; i++ {
		g.NewCard(corpusCard(t, "Forest"), p, engine.Battlefield)
	}
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{argoth})
	if err := resolveMeldOf(t, g, p, titania, c); err != nil {
		t.Fatalf("Meld: %v", err)
	}
	tc := g.Card(titania)
	if tc.Def.Name != "Titania, Gaea Incarnate" || tc.MeldedWith != argoth || !g.Card(argoth).Melded {
		t.Fatalf("Titania face %q melded with %v, Argoth melded %v", tc.Def.Name, tc.MeldedWith, g.Card(argoth).Melded)
	}
	if n := countValid(g, p, "Land.YouCtrl"); n != 2 {
		t.Errorf("Count$Valid Land.YouCtrl = %d, want 2: the melded Argoth is no land p controls", n)
	}
	if pw, _ := tc.Power(); pw != 2 {
		t.Errorf("Titania, Gaea Incarnate power = %d, want 2", pw)
	}
}

// TestMeldTitaniaWithNoOtherLandDies: with no land but the melded Argoth,
// Titania, Gaea Incarnate is 0/0 and dies (CR 704.5f); both cards go to the
// graveyard, and its enters trigger then returns Argoth, a land card there.
func TestMeldTitaniaWithNoOtherLandDies(t *testing.T) {
	t.Parallel()

	g, p, _, titania, argoth := meldedPair(t, "Titania, Voice of Gaea", "Argoth, Sanctum of Nature")
	if g.Card(titania).Zone != engine.Graveyard || g.Card(titania).Def.Name != "Titania, Voice of Gaea" {
		t.Errorf("Titania in %v as %q, want Titania, Voice of Gaea in the graveyard", g.Card(titania).Zone, g.Card(titania).Def.Name)
	}
	if ac := g.Card(argoth); ac.Zone != engine.Battlefield || ac.Melded || !g.Zone(engine.Battlefield, p).Contains(argoth) {
		t.Errorf("Argoth in %v melded %v, want back on the battlefield on its own", ac.Zone, ac.Melded)
	}
}

// TestMeldMishraEntersTappedAndAttacking plays Mishra, Claimed by Gix's
// attack trigger: both attackers are exiled and leave combat, and Mishra,
// Lost to Phyrexia enters tapped and attacking (Tapped$, Attacking$ True).
// Only it deals combat damage: the melded Phyrexian Dragon Engine is no
// attacker.
func TestMeldMishraEntersTappedAndAttacking(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.CombatBegin)
	mishra := g.NewCard(corpusCard(t, "Mishra, Claimed by Gix"), p, engine.Battlefield)
	dragon := g.NewCard(corpusCard(t, "Phyrexian Dragon Engine"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{mishra, dragon})
	c.QueueCardChoice([]engine.CardID{dragon})
	// Mishra, Lost to Phyrexia's enters trigger: pump, curse and powerstones.
	c.QueueModeChoice([]int{3, 4, 5})
	c.QueueAttackTarget(engine.PlayerEntity(other))
	reg := engine.NewRegistry()

	if err := g.Step(reg, c); err != nil {
		t.Fatalf("Step to declare attackers: %v", err)
	}
	mc := g.Card(mishra)
	if mc.Def.Name != "Mishra, Lost to Phyrexia" || !mc.Tapped || !g.Card(dragon).Melded {
		t.Fatalf("Mishra face %q tapped %v, Dragon Engine melded %v", mc.Def.Name, mc.Tapped, g.Card(dragon).Melded)
	}
	if atk := g.Attackers(); len(atk) != 1 || atk[0] != mishra || g.AttackTarget(mishra) != engine.PlayerEntity(other) {
		t.Fatalf("attackers %v, want only Mishra attacking the opponent", atk)
	}
	// Each opponent lost 2 (two attackers as the trigger resolved).
	if got := g.Player(other).Life; got != 18 {
		t.Fatalf("opponent life = %d, want 18", got)
	}
	for g.ActivePhase() != engine.CombatEnd {
		if err := g.Step(reg, c); err != nil {
			t.Fatalf("Step to combat end: %v", err)
		}
	}
	if got := g.Player(other).Life; got != 9 {
		t.Errorf("opponent life after combat = %d, want 9 (Mishra's 9 alone)", got)
	}
}

// TestMeldTokenSecondaryStaysExiled: a token is exiled with the host and
// neither returns (MeldEffect.java's c.isToken() check).
func TestMeldTokenSecondaryStaysExiled(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	gis := g.NewCard(corpusCard(t, gisela), p, engine.Battlefield)
	tok := g.NewCard(corpusCard(t, bruna), p, engine.Battlefield)
	g.Card(tok).IsToken = true
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{tok})
	if err := resolveMeldOf(t, g, p, gis, c); err != nil {
		t.Fatalf("Meld: %v", err)
	}
	if g.Card(gis).Zone != engine.Exile || g.Card(gis).MeldedWith != engine.NoCard || g.Card(tok).Melded {
		t.Errorf("Gisela in %v melded with %v, token melded %v, want both left in exile", g.Card(gis).Zone, g.Card(gis).MeldedWith, g.Card(tok).Melded)
	}
}

// TestMeldWithoutSecondaryDoesNothing: no permanent p owns and controls is
// named Secondary$, so nothing is exiled -- Bruna under the opponent's
// control, or owned by them, does not count.
func TestMeldWithoutSecondaryDoesNothing(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	gis := g.NewCard(corpusCard(t, gisela), p, engine.Battlefield)
	theirs := g.NewCard(corpusCard(t, bruna), other, engine.Battlefield)
	if err := resolveMeldOf(t, g, p, gis, engine.NewScriptedController()); err != nil {
		t.Fatalf("Meld: %v", err)
	}
	if g.Card(gis).Zone != engine.Battlefield || g.Card(theirs).Zone != engine.Battlefield {
		t.Error("a card moved with no Secondary$ to meld with")
	}
}

// TestMeldRejectsUnresolvedShapes pins the fail-closed contract: each
// shape errors before anything moves.
func TestMeldRejectsUnresolvedShapes(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ line, want string }{
		{"DB$ Meld | Primary$ A | Secondary$ B | Blocking$ Defined", "Blocking$"},
		{"DB$ Meld | Primary$ A | Secondary$ B | Attacking$ Defined", "Attacking$"},
		{"DB$ Meld | Secondary$ B", "Primary$"},
		{"DB$ Meld | Primary$ A", "Secondary$"},
	} {
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want one naming %s", tc.line, err, tc.want)
		}
	}
}

// TestMeldHanweirActivatedAbility: Hanweir Battlements' own A: line, a land
// primary whose ConditionCheckSVar$ counts a Hanweir Garrison p owns and
// controls through named<Name>, melds into Hanweir, the Writhing Township.
func TestMeldHanweirActivatedAbility(t *testing.T) {
	t.Parallel()

	g, _, _, hanweir, garrison := meldedPair(t, "Hanweir Battlements", "Hanweir Garrison")
	hc := g.Card(hanweir)
	if hc.Def.Name != "Hanweir, the Writhing Township" || hc.Zone != engine.Battlefield || !g.Card(garrison).Melded {
		t.Fatalf("Hanweir in %v as %q, Garrison melded %v", hc.Zone, hc.Def.Name, g.Card(garrison).Melded)
	}
	if pw, _ := hc.Power(); pw != 7 {
		t.Errorf("Hanweir, the Writhing Township power = %d, want 7", pw)
	}
}

// TestMeldHanweirConditionNeedsGarrisonYouOwn: a Hanweir Garrison the
// opponent controls fails ConditionCheckSVar$, so nothing is exiled.
func TestMeldHanweirConditionNeedsGarrisonYouOwn(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	hanweir := g.NewCard(corpusCard(t, "Hanweir Battlements"), p, engine.Battlefield)
	garrison := g.NewCard(corpusCard(t, "Hanweir Garrison"), other, engine.Battlefield)
	if err := resolveMeldOf(t, g, p, hanweir, engine.NewScriptedController()); err != nil {
		t.Fatalf("Meld: %v", err)
	}
	if g.Card(hanweir).Zone != engine.Battlefield || g.Card(garrison).Zone != engine.Battlefield {
		t.Error("a card moved though p owns and controls no Hanweir Garrison")
	}
}

// TestMeldRejectsVanillesUnpaidTriggeredCost: Vanille, Cheerful l'Cie's
// trigger executes AB$ Meld | Cost$ 3 B G, a cost no triggered ability asks
// for yet; resolving it would meld for free, so it errors before anything
// moves.
func TestMeldRejectsVanillesUnpaidTriggeredCost(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	vanille := g.NewCard(corpusCard(t, "Vanille, Cheerful l'Cie"), p, engine.Battlefield)
	fang := g.NewCard(corpusCard(t, "Fang, Fearless l'Cie"), p, engine.Battlefield)
	err := resolveMeldOf(t, g, p, vanille, engine.NewScriptedController())
	if err == nil || !strings.Contains(err.Error(), "Cost$") {
		t.Errorf("err = %v, want one naming Cost$", err)
	}
	if g.Card(vanille).Zone != engine.Battlefield || g.Card(fang).Zone != engine.Battlefield {
		t.Error("a card moved before the rejection")
	}
}

// TestMeldRejectsHostOffTheBattlefield: Gisela's trigger resolving after
// Gisela died in response would, in Java, fail its IsPresent$ recheck (CR
// 603.4); this port has no recheck, so it errors rather than melding Gisela
// out of the graveyard.
func TestMeldRejectsHostOffTheBattlefield(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	gis := g.NewCard(corpusCard(t, gisela), p, engine.Graveyard)
	bru := g.NewCard(corpusCard(t, bruna), p, engine.Battlefield)
	err := resolveMeldOf(t, g, p, gis, engine.NewScriptedController())
	if err == nil || !strings.Contains(err.Error(), "603.4") {
		t.Errorf("err = %v, want one naming CR 603.4", err)
	}
	if g.Card(gis).Zone != engine.Graveyard || g.Card(bru).Zone != engine.Battlefield {
		t.Error("a card moved before the rejection")
	}
}

// TestMeldFaceTriggersAreNotLiveOnTheFrontFace: Mishra, Claimed by Gix
// attacking alone fires its own attack trigger, whose Meld finds no Dragon
// Engine -- and not Mishra, Lost to Phyrexia's "whenever attacks" charm,
// printed on the meld face it is not (liveFaces, trigger.go). The scripted
// controller has no mode choice queued, so a fired charm would panic.
func TestMeldFaceTriggersAreNotLiveOnTheFrontFace(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.CombatBegin)
	mishra := g.NewCard(corpusCard(t, "Mishra, Claimed by Gix"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueAttackers([]engine.CardID{mishra})

	if err := g.Step(engine.NewRegistry(), c); err != nil {
		t.Fatalf("Step to declare attackers: %v", err)
	}
	if got := g.Player(other).Life; got != 19 {
		t.Errorf("opponent life = %d, want 19 (one attacker)", got)
	}
	if mc := g.Card(mishra); mc.Def.Name != "Mishra, Claimed by Gix" || mc.Zone != engine.Battlefield {
		t.Errorf("Mishra in %v as %q, want Mishra, Claimed by Gix still on the battlefield", mc.Zone, mc.Def.Name)
	}
}

// TestFlipFaceTriggersAreNotLiveOnTheFrontFace proves liveFaces (trigger.go)
// restricts a Flip card the identical way it restricts Meld, Transform,
// Modal and Specialize: Nezumi Shortfang unflipped controls no printed
// upkeep trigger, so its opponent's upkeep beginning must not fire Stabwhisker
// the Odious's own "each opponent's upkeep, that player loses life" --
// printed on the flip face Nezumi Shortfang is not (rules review on the
// merged Meld commit: liveFaces originally restricted Transform/Meld/Modal/
// Specialize only, leaving every Flip card's back-face triggers live on the
// front face, unflipped).
func TestFlipFaceTriggersAreNotLiveOnTheFrontFace(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Nezumi Shortfang"), p, engine.Battlefield)
	g.SetTurnState(1, other, engine.Untap)

	g.AdvancePhase(engine.NewScriptedController())

	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("opponent life = %d, want 20 -- Stabwhisker the Odious's trigger must not fire off the front face", got)
	}
}

// TestRemoveFromMatchSkipsAMeldedSecondary proves RemoveType$'s own raw
// CardID scan (removefrommatcheffect.go), which does not walk a zone's own
// Cards() set the way most effects do, skips a melded secondary by its own
// Melded flag instead of sweeping it in -- rules review on the merged Meld
// commit: Java's own scan reads each zone's own getCards(), which
// PlayerZoneBattlefield.addToMelded already excludes it from.
func TestRemoveFromMatchSkipsAMeldedSecondary(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	secondary := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.Card(secondary).Melded = true

	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ RemoveFromMatch | RemoveType$ Creature.YouOwn")

	if g.Card(secondary).Zone != engine.Battlefield {
		t.Errorf("melded secondary zone = %v, want Battlefield (untouched)", g.Card(secondary).Zone)
	}
}

// TestIntensifySkipsAMeldedSecondary is TestRemoveFromMatchSkipsAMeldedSecondary's
// twin for AllDefined$ (intensifyeffect.go), the same raw-scan pattern.
func TestIntensifySkipsAMeldedSecondary(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	secondary := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.Card(secondary).Melded = true

	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Intensify | AllDefined$ Creature.YouOwn")

	if got := g.Card(secondary).Intensity; got != 0 {
		t.Errorf("melded secondary Intensity = %d, want 0 (untouched)", got)
	}
}
