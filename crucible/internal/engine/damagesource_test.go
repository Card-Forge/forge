package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// This file covers DealDamage's DamageSource$: the damage is dealt by the
// card it names, so that card's keywords and colors -- not the spell's or the
// trigger host's -- decide deathtouch, lifelink, infect and protection
// (DamageDealEffect.java:135, :217-268).

// coloredCreatureDef is a creature of the color cost names, with keywords.
func coloredCreatureDef(t *testing.T, cost, power, toughness string, keywords ...string) *compile.Card {
	t.Helper()
	def := creatureDefPTKeywords(t, power, toughness, keywords...)
	def.Faces[0].ManaCost = mana.MustParse(cost)
	return def
}

// biteLine is Rabid Bite's chain: the Pump root targets a creature its
// controller controls, the DealDamage link a creature they do not, dealt by
// the root's target.
func biteLine(link string) (string, []string) {
	return "DB$ Pump | ValidTgts$ Creature.YouCtrl | SubAbility$ Bite",
		[]string{"Bite", "DB$ DealDamage | ValidTgts$ Creature.YouDontCtrl | NumDmg$ X | DamageSource$ ParentTarget" + link,
			"X", "ParentTargeted$CardPower"}
}

func TestDamageSourceParentTargetDealsWithTheBitersLifelink(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	biter := g.NewCard(coloredCreatureDef(t, "R", "3", "3", "Lifelink"), p, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "2", "4"), other, engine.Battlefield)
	var sink recordingSink
	g.SetSink(&sink)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(biter)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	root, svars := biteLine("")
	host := resolveLine(t, g, p, c, root, svars...)

	if got := g.Card(victim).Damage.Marked; got != 3 {
		t.Errorf("victim damage = %d, want 3 (the biter's power)", got)
	}
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23 (the biter's lifelink)", got)
	}
	for _, e := range sink.events {
		if e.Kind == engine.DamageDealt && e.Source != biter {
			t.Errorf("DamageDealt source = %v, want the biter %v, not the host %v", e.Source, biter, host)
		}
	}
}

// TestDamageSourcePreventionIsCheckedAgainstTheSource proves a "prevent all
// damage blue sources would deal" replacement prevents a blue biter's damage
// and not a red one's: ValidSource$ reads the damage source, not the (green)
// host whose ability it is.
func TestDamageSourcePreventionIsCheckedAgainstTheSource(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		cost string
		want int
	}{{"R", 3}, {"U", 0}} {
		g, p, other := newTwoPlayerGame(t)
		g.NewCard(replacementEnchantmentDef(t, "Test Blue Shield",
			"Event$ DamageDone | ValidSource$ Card.Blue | Prevent$ True | Description$ Prevent all damage blue sources would deal."), other, engine.Battlefield)
		biter := g.NewCard(coloredCreatureDef(t, tc.cost, "3", "3"), p, engine.Battlefield)
		victim := g.NewCard(creatureDefPT(t, "2", "4"), other, engine.Battlefield)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.CardEntity(biter)})
		c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
		root, svars := biteLine("")
		resolveLine(t, g, p, c, root, svars...)
		if got := g.Card(victim).Damage.Marked; got != tc.want {
			t.Errorf("%s biter: victim damage = %d, want %d", tc.cost, got, tc.want)
		}
	}
}

// TestDamageSourceTargetedInfectCreatureDamagesItself is Justice Strike's
// shape with an infect target: its damage to itself is -1/-1 counters.
func TestDamageSourceTargetedInfectCreatureDamagesItself(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPTKeywords(t, "2", "3", "Infect"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	resolveLine(t, g, p, c, "DB$ DealDamage | ValidTgts$ Creature | DamageSource$ Targeted | NumDmg$ X", "X", "Targeted$CardPower")
	if got := g.Card(victim).Counters.Count(engine.M1M1); got != 2 {
		t.Errorf("-1/-1 counters = %d, want 2", got)
	}
	if got := g.Card(victim).Damage.Marked; got != 0 {
		t.Errorf("marked damage = %d, want 0 (infect deals counters)", got)
	}
}

// TestDamageSourceNoParentTargetLeftDealsNothing proves a targeting ancestor
// holding no target -- TargetMin$ 0 chose none, the same answer as a target
// dropped under CR 608.2b -- names no source: no damage, no error.
func TestDamageSourceNoParentTargetLeftDealsNothing(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "2", "4"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets(nil)
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	resolveLine(t, g, p, c, "DB$ Pump | ValidTgts$ Creature.YouCtrl | TargetMin$ 0 | TargetMax$ 1 | SubAbility$ Bite",
		"Bite", "DB$ DealDamage | ValidTgts$ Creature.YouDontCtrl | NumDmg$ X | DamageSource$ ParentTarget",
		"X", "ParentTargeted$CardPower")
	if got := g.Card(victim).Damage.Marked; got != 0 {
		t.Errorf("victim damage = %d, want 0", got)
	}
}

// TestDamageSourceParentTargetWithoutTargetingAncestorFails proves a
// ParentTarget reference with no targeting ancestor at all -- one this port
// does not carry -- fails by name, as a source and as an amount.
func TestDamageSourceParentTargetWithoutTargetingAncestorFails(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ line, want string }{
		{"DB$ DealDamage | Defined$ Opponent | NumDmg$ 1 | DamageSource$ ParentTarget", "ParentTarget"},
		{"DB$ DealDamage | Defined$ Opponent | NumDmg$ X", "NumDmg$"},
	} {
		g, p, other := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, tc.line, "X", "ParentTargeted$CardPower")
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%q: err = %v, want one naming %s", tc.line, err, tc.want)
		}
		if g.Player(other).Life != 20 {
			t.Errorf("%q: other's life = %d, want 20", tc.line, g.Player(other).Life)
		}
	}
}

// TestDamageSourceValidEachMatchingCardDeals proves "Valid <spec>" makes every
// matching battlefield card a source: each deals the damage, and each
// lifelinker's controller gains its share.
func TestDamageSourceValidEachMatchingCardDeals(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.NewCard(coloredCreatureDef(t, "W", "1", "1", "Lifelink"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ DealDamage | Defined$ Opponent | NumDmg$ 1 | DamageSource$ Valid Creature.YouCtrl")
	if got := g.Player(other).Life; got != 17 {
		t.Errorf("other's life = %d, want 17 (the host and two creatures each deal 1)", got)
	}
	if got := g.Player(p).Life; got != 21 {
		t.Errorf("life = %d, want 21 (one lifelinker)", got)
	}
}

// TestDamageSourceValidMatchingNothingDealsNothing proves an empty source
// list deals nothing and still chains the sub-ability.
func TestDamageSourceValidMatchingNothingDealsNothing(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ DealDamage | Defined$ Opponent | NumDmg$ 1 | DamageSource$ Valid Artifact.YouCtrl | SubAbility$ Gain",
		"Gain", "DB$ GainLife | Defined$ You | LifeAmount$ 2")
	if got := g.Player(other).Life; got != 20 {
		t.Errorf("other's life = %d, want 20", got)
	}
	if got := g.Player(p).Life; got != 22 {
		t.Errorf("life = %d, want 22 (the sub-ability still runs)", got)
	}
}

// TestDamageSourceTargetedDealsBackToTheHost is Stalking Yeti's shape: the
// host deals 1 to a creature, then that creature deals damage equal to its
// power to the host (Defined$ Self | DamageSource$ Targeted).
func TestDamageSourceTargetedDealsBackToTheHost(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	host := resolveLine(t, g, p, c, "DB$ DealDamage | ValidTgts$ Creature.YouDontCtrl | NumDmg$ 1 | SubAbility$ Back",
		"Back", "DB$ DealDamage | Defined$ Self | DamageSource$ Targeted | NumDmg$ Y", "Y", "Targeted$CardPower")
	if z := g.Card(host).Zone; z != engine.Graveyard {
		t.Errorf("host in %v, want the graveyard (3 damage from the 3/3)", z)
	}
	if got := g.Card(victim).Damage.Marked; got != 1 {
		t.Errorf("victim damage = %d, want 1", got)
	}
}

// TestDealDamageDefinedNamesCardsAndDefaultsToSelf proves Defined$ is
// getDefinedEntities: a targeted card under Defined$ Targeted is dealt the
// damage, and no Defined$ at all is the host.
func TestDealDamageDefinedNamesCardsAndDefaultsToSelf(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "4"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	resolveLine(t, g, p, c, "DB$ Pump | ValidTgts$ Creature | SubAbility$ Hit", "Hit", "DB$ DealDamage | Defined$ Targeted | NumDmg$ 2")
	if got := g.Card(victim).Damage.Marked; got != 2 {
		t.Errorf("victim damage = %d, want 2", got)
	}

	host := resolveLine(t, g, p, engine.NewScriptedController(), "DB$ DealDamage | NumDmg$ 1")
	if z := g.Card(host).Zone; z != engine.Graveyard {
		t.Errorf("host in %v, want the graveyard (no Defined$ is Self)", z)
	}

	_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ DealDamage | Defined$ Bogus | NumDmg$ 1")
	if err == nil || !strings.Contains(err.Error(), "Bogus") {
		t.Errorf("Defined$ Bogus: err = %v, want one naming it", err)
	}
}

// TestDealDamageDefinedSkipsACardNotInPlay proves a Defined$ card off the
// battlefield is dealt nothing (DamageDealEffect.java:253-256).
func TestDealDamageDefinedSkipsACardNotInPlay(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	def := etbChainDef(t, "Test Gone", "DB$ DealDamage | Defined$ Self | NumDmg$ 1")
	host := g.NewCard(def, p, engine.Graveyard)
	for _, sub := range def.Faces[0].Triggers[0].Subs {
		if !strings.EqualFold(sub.Key, "Execute") {
			continue
		}
		g.PushAbility(engine.Ability{API: engine.APIDealDamage, Source: host, Controller: p, Params: sub.Ability, Amounts: def.Faces[0].Amounts})
	}
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Card(host).Damage.Marked; got != 0 {
		t.Errorf("graveyard host damage = %d, want 0", got)
	}
}

// TestDamageSourceTriggeredSourceDealsToItself proves DamageSource$
// TriggeredSource names the card a DamageDone trigger recorded.
func TestDamageSourceTriggeredSourceDealsToItself(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: "Test Triggered Source"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Test Triggered Source"
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "1", "3"
	raw.Faces[0].ManaCost = mana.MustParse("G")
	raw.Faces[0].Triggers = []string{
		"Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ Ping",
		"Mode$ DamageDone | ValidSource$ Card.Self | ValidTarget$ Player | Execute$ Back",
	}
	raw.Faces[0].SVars.Set("Ping", "DB$ DealDamage | Defined$ Opponent | NumDmg$ 1")
	raw.Faces[0].SVars.Set("Back", "DB$ DealDamage | DamageSource$ TriggeredSource | Defined$ TriggeredSource | NumDmg$ 2")
	def, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}
	host, err := castETBChain(t, g, p, def, engine.NewScriptedController())
	if err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 19 {
		t.Errorf("other's life = %d, want 19", got)
	}
	if got := g.Card(host).Damage.Marked; got != 2 {
		t.Errorf("host damage = %d, want 2 (dealt by itself as the trigger's source)", got)
	}
}

// TestDamageSourceTriggeredSourceUnrecordedFails proves TriggeredSource off
// a trigger that records none fails by name.
func TestDamageSourceTriggeredSourceUnrecordedFails(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil,
		"DB$ DealDamage | Defined$ Opponent | NumDmg$ 1 | DamageSource$ TriggeredSource")
	if err == nil || !strings.Contains(err.Error(), "TriggeredSource") {
		t.Errorf("err = %v, want one naming TriggeredSource", err)
	}
}

// TestDealDamageRememberDamagedRemembersWhatTheHostDamaged proves
// RememberDamaged$ (GameAction.java:2790-2794): the host remembers each
// entity its damage reached, and a fully prevented one is not.
func TestDealDamageRememberDamagedRemembersWhatTheHostDamaged(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "4"), other, engine.Battlefield)
	g.NewCard(replacementEnchantmentDef(t, "Test Shield",
		"Event$ DamageDone | ValidTarget$ You | Prevent$ True | Description$ Prevent all damage that would be dealt to you."), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim), engine.PlayerEntity(other)})
	host := resolveLine(t, g, p, c, "DB$ DealDamage | ValidTgts$ Any | TargetMax$ 2 | NumDmg$ 1 | RememberDamaged$ True")
	got := g.Card(host).Memory.Remembered()
	if !slices.Equal(got, []engine.EntityID{engine.CardEntity(victim)}) {
		t.Errorf("remembered = %v, want only the damaged creature %v", got, victim)
	}
}

// TestDealDamageExcessDamageGoesToTheController covers ExcessDamage$
// (DamageDealEffect.java:280-298): lethal damage to a creature, the rest to
// its controller; a planeswalker's lethal is its loyalty; a noncreature
// nonplaneswalker takes none of it; ExcessDamageCondition$ unmet sends it all
// to the target.
func TestDealDamageExcessDamageGoesToTheController(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, line string
		target     func(*engine.Game, engine.PlayerID) engine.CardID
		wantLife   int
	}{
		{"creature", "DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 5 | ExcessDamage$ TargetedController",
			func(g *engine.Game, o engine.PlayerID) engine.CardID {
				return g.NewCard(creatureDefPT(t, "2", "2"), o, engine.Battlefield)
			}, 17},
		{"planeswalker", "DB$ DealDamage | ValidTgts$ Planeswalker | NumDmg$ 5 | ExcessDamage$ TargetedController",
			func(g *engine.Game, o engine.PlayerID) engine.CardID {
				id := g.NewCard(planeswalkerDefLoyalty(t, "3"), o, engine.Battlefield)
				g.Card(id).Counters.Add(engine.Loyalty, 3)
				return id
			}, 18},
		{"artifact", "DB$ DealDamage | ValidTgts$ Artifact | NumDmg$ 5 | ExcessDamage$ TargetedController",
			func(g *engine.Game, o engine.PlayerID) engine.CardID {
				def := &compile.Card{Name: "Test Rock"}
				def.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), "Artifact")
				return g.NewCard(def, o, engine.Battlefield)
			}, 15},
		{"condition unmet", "DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 5 | ExcessDamage$ TargetedController | ExcessDamageCondition$ Card.withTrample",
			func(g *engine.Game, o engine.PlayerID) engine.CardID {
				return g.NewCard(creatureDefPT(t, "2", "2"), o, engine.Battlefield)
			}, 20},
	} {
		g, p, other := newTwoPlayerGame(t)
		target := tc.target(g, other)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.CardEntity(target)})
		resolveLine(t, g, p, c, tc.line)
		if got := g.Player(other).Life; got != tc.wantLife {
			t.Errorf("%s: controller life = %d, want %d", tc.name, got, tc.wantLife)
		}
	}
}

// TestDealDamageExcessDamageReadsTheSourcesDeathtouch proves the lethal
// amount is 1 from a deathtouch source (Card.getExcessDamageValue).
func TestDealDamageExcessDamageReadsTheSourcesDeathtouch(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	biter := g.NewCard(creatureDefPTKeywords(t, "4", "4", "Deathtouch"), p, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "2", "5"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(biter)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	root, svars := biteLine(" | ExcessDamage$ TargetedController")
	resolveLine(t, g, p, c, root, svars...)
	if z := g.Card(victim).Zone; z != engine.Graveyard {
		t.Errorf("victim in %v, want the graveyard (1 deathtouch damage)", z)
	}
	if got := g.Player(other).Life; got != 17 {
		t.Errorf("controller life = %d, want 17 (the excess 3)", got)
	}
}

func TestDealDamageExcessDamageUnresolvableFails(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.CardEntity(victim)},
		"DB$ DealDamage | ValidTgts$ Creature | NumDmg$ 5 | ExcessDamage$ Bogus")
	if err == nil || !strings.Contains(err.Error(), "ExcessDamage$") {
		t.Errorf("err = %v, want one naming ExcessDamage$", err)
	}
}

// TestDealDamageOptionalDeciderMayDecline proves OptionalDecider$
// (DamageDealEffect.java:180-185): declined, no damage; accepted, damage.
func TestDealDamageOptionalDeciderMayDecline(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		confirm bool
		want    int
	}{{false, 20}, {true, 17}} {
		g, p, other := newTwoPlayerGame(t)
		c := engine.NewScriptedController()
		c.QueueConfirmEffect(tc.confirm)
		resolveLine(t, g, p, c, "DB$ DealDamage | Defined$ Opponent | NumDmg$ 3 | OptionalDecider$ You")
		if got := g.Player(other).Life; got != tc.want {
			t.Errorf("confirm %v: other's life = %d, want %d", tc.confirm, got, tc.want)
		}
	}

	g, p, _ := newTwoPlayerGame(t)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ DealDamage | Defined$ Opponent | NumDmg$ 3 | OptionalDecider$ Bogus")
	if err == nil || !strings.Contains(err.Error(), "OptionalDecider$") {
		t.Errorf("err = %v, want one naming OptionalDecider$", err)
	}
}

// TestDamageSourceTargetUniqueLinkCannotRetargetTheBiter proves Fall of the
// Hammer's "another target creature": a TargetUnique$ link is not offered
// what its ancestor targets (SpellAbility.java:1401).
func TestDamageSourceTargetUniqueLinkCannotRetargetTheBiter(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	biter := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Battlefield)
	victim := g.NewCard(creatureDefPT(t, "2", "4"), other, engine.Battlefield)
	r := &targetOfferRecorder{ScriptedController: engine.NewScriptedController()}
	r.QueueTargets([]engine.EntityID{engine.CardEntity(biter)})
	r.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	def := etbChainDef(t, "Test Hammer", "DB$ Pump | ValidTgts$ Creature.YouCtrl | SubAbility$ Bite",
		"Bite", "DB$ DealDamage | ValidTgts$ Creature | TargetUnique$ True | NumDmg$ X | DamageSource$ ParentTarget",
		"X", "ParentTargeted$CardPower")
	g.Player(p).ManaPool.Add(mana.Green, 1)
	host := g.NewCard(def, p, engine.Hand)
	if !g.CastSpell(p, host, r) {
		t.Fatal("CastSpell failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), r); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if len(r.offers) != 2 {
		t.Fatalf("offered %d target lists, want 2", len(r.offers))
	}
	if slices.Contains(r.offers[1], engine.CardEntity(biter)) {
		t.Errorf("link offered %v, want the biter excluded", r.offers[1])
	}
	if !slices.Contains(r.offers[1], engine.CardEntity(host)) {
		t.Errorf("link offered %v, want the host still offered", r.offers[1])
	}
	if got := g.Card(victim).Damage.Marked; got != 3 {
		t.Errorf("victim damage = %d, want 3", got)
	}
}

// TestDamageSourceDamageMapRecordsEachSource is Self-Destruct's chain: the
// targeted creature deals X to itself and X to another target, both recorded
// under DamageMap$ and dealt by DamageResolve.
func TestDamageSourceDamageMapRecordsEachSource(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	bomb := g.NewCard(creatureDefPT(t, "3", "5"), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bomb)})
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	resolveLine(t, g, p, c,
		"DB$ DealDamage | ValidTgts$ Creature.YouCtrl | NumDmg$ X | DamageMap$ True | DamageSource$ ThisTargetedCard | SubAbility$ Two",
		"Two", "DB$ DealDamage | ValidTgts$ Any | TargetUnique$ True | NumDmg$ Y | DamageSource$ ParentTarget | SubAbility$ Resolve",
		"Resolve", "DB$ DamageResolve",
		"X", "Targeted$CardPower", "Y", "ParentTargeted$CardPower")
	if got := g.Card(bomb).Damage.Marked; got != 3 {
		t.Errorf("bomb damage = %d, want 3", got)
	}
	if got := g.Player(other).Life; got != 17 {
		t.Errorf("other's life = %d, want 17", got)
	}
}

// TestDealDamageImprintedControllerIsDamaged is Enchanter's Bane's
// Defined$ ImprintedController: the controller of each imprinted card.
func TestDealDamageImprintedControllerIsDamaged(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	def := etbChainDef(t, "Test Imprint", "DB$ DealDamage | Defined$ ImprintedController | DamageSource$ Imprinted | NumDmg$ 2")
	host := g.NewCard(def, p, engine.Battlefield)
	g.Card(host).Memory.Imprint(theirs)
	for _, sub := range def.Faces[0].Triggers[0].Subs {
		if strings.EqualFold(sub.Key, "Execute") {
			g.PushAbility(engine.Ability{API: engine.APIDealDamage, Source: host, Controller: p, Params: sub.Ability, Amounts: def.Faces[0].Amounts})
		}
	}
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}
	if got := g.Player(other).Life; got != 18 {
		t.Errorf("other's life = %d, want 18", got)
	}
}

// TestDealDamageRejectsUnportedParams proves each DamageDealEffect param this
// port does not evaluate fails by name before any damage (PORT-8/GO-7).
func TestDealDamageRejectsUnportedParams(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ extra, want string }{
		{"DamageMap$ True | ExcessSVar$ Excess", "ExcessSVar$"},
		{"ReplaceDyingDefined$ Targeted | ReplaceDyingExiledWith$ True", "ReplaceDyingExiledWith$"},
		{"ReplaceDyingDefined$ Targeted | ReplaceDyingCondition$ Foretold", "ReplaceDyingCondition$"},
		{"ReplaceDyingDefined$ Bogus", "ReplaceDyingDefined$"},
		{"RelativeTarget$ True", "RelativeTarget$"},
		{"DividerOnResolution$ You", "DividerOnResolution$"},
		{"DivideEvenly$ RoundedDown", "DivideEvenly$"},
		{"CardChoices$ Creature", "CardChoices$"},
		{"RememberAmount$ True", "RememberAmount$"},
		{"NoRedirection$ True", "NoRedirection$"},
		{"DamageSource$ Valid Card.IsCommander+YouCtrl", "IsCommander"},
		{"DamageMap$ True | RememberDamaged$ True", "RememberDamaged$"},
	} {
		g, p, other := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ DealDamage | Defined$ Opponent | NumDmg$ 1 | "+tc.extra)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want one naming %s", tc.extra, err, tc.want)
		}
		if g.Player(other).Life != 20 {
			t.Errorf("%s: other's life = %d, want 20", tc.extra, g.Player(other).Life)
		}
	}

	g, p, other := newTwoPlayerGame(t)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), []engine.EntityID{engine.PlayerEntity(other)},
		"DB$ DealDamage | ValidTgts$ Any.NotDefinedParentTarget,Player | NumDmg$ 1")
	if err == nil || !strings.Contains(err.Error(), "NotDefined") {
		t.Errorf("NotDefined ValidTgts$: err = %v, want one naming it", err)
	}
}
