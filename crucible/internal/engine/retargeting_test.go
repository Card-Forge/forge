package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// targetOfferRecorder records every candidate list ChooseTargets is offered.
type targetOfferRecorder struct {
	*engine.ScriptedController
	offers [][]engine.EntityID
}

func (r *targetOfferRecorder) ChooseTargets(g *engine.Game, p engine.PlayerID, valid []engine.EntityID, lo, hi int) []engine.EntityID {
	r.offers = append(r.offers, append([]engine.EntityID(nil), valid...))
	return r.ScriptedController.ChooseTargets(g, p, valid, lo, hi)
}

// seatRetargeter puts a watcher for p on the battlefield whose "whenever
// you cast a spell" trigger resolves changeLine.
func seatRetargeter(t *testing.T, g *engine.Game, p engine.PlayerID, changeLine string, svars ...string) engine.CardID {
	t.Helper()
	def := triggerWatcherDef(t, "Retargeter",
		"Mode$ SpellCast | ValidCard$ Card | ValidActivatingPlayer$ You | Execute$ TrigChange",
		append([]string{"TrigChange", changeLine}, svars...)...)
	return g.NewCard(def, p, engine.Battlefield)
}

// castW casts card from p's hand paying one W, then resolves the stack.
func castW(t *testing.T, g *engine.Game, p engine.PlayerID, card engine.CardID, c engine.PlayerController) error {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.White, 1)
	if !g.CastSpell(p, card, c) {
		t.Fatal("CastSpell failed")
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

func boltAt(t *testing.T, g *engine.Game, p engine.PlayerID) engine.CardID {
	t.Helper()
	return g.NewCard(instantDefWithAbility(t, "Bolt", "W", "SP$ DealDamage | ValidTgts$ Player | NumDmg$ 3"), p, engine.Hand)
}

// TestChangeTargetsRetargetsASpellWithASingleTarget proves the dominant
// shape (Misdirection, Swerve, Shunt: TargetType$ Spell.singleTarget |
// ValidTgts$ Card) end to end: the retargeting trigger targets the Bolt on
// the stack as it is pushed, then its controller chooses the Bolt's new
// target, and the Bolt resolves against it.
func TestChangeTargetsRetargetsASpellWithASingleTarget(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	seatRetargeter(t, g, p, "DB$ ChangeTargets | TargetType$ Spell.singleTarget | ValidTgts$ Card")
	bolt := boltAt(t, g, p)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bolt)})
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(p)})
	r := &targetOfferRecorder{ScriptedController: c}
	if err := castW(t, g, p, bolt, r); err != nil {
		t.Fatal(err)
	}
	if g.Player(p).Life != 17 || g.Player(other).Life != 20 {
		t.Errorf("life %d/%d, want 17/20: the Bolt was redirected at its caster", g.Player(p).Life, g.Player(other).Life)
	}
	if len(r.offers) != 3 || len(r.offers[1]) != 1 || r.offers[1][0] != engine.CardEntity(bolt) {
		t.Fatalf("offers = %v, want the Bolt alone as the retargeter's candidate", r.offers)
	}
	if len(r.offers[2]) != 2 {
		t.Errorf("new-target offer = %v, want both players", r.offers[2])
	}
}

// TestChangeTargetsTargetTypeFiltersStackItems proves the TargetType$
// properties (SpellAbilityProperty.hasProperty): a Bolt with one player
// target is offered or not by each shape, judged from the retargeter's
// host and controller. When nothing qualifies, the trigger never goes on
// the stack and the Bolt hits its original target.
func TestChangeTargetsTargetTypeFiltersStackItems(t *testing.T) {
	t.Parallel()
	cases := []struct {
		targetType string
		atCaster   bool // the Bolt's original target is its own caster
		offered    bool
	}{
		{"Spell", false, true},
		{"SpellAbility.singleTarget", false, true},
		{"Spell.numTargets EQ1", false, true},
		{"Spell.numTargets GE2", false, false},
		{"Spell.singleTarget+IsTargeting You", true, true},
		{"Spell.singleTarget+IsTargeting You", false, false},
		{"Spell.IsTargeting Self", false, false},
		{"Instant.singleTarget,Sorcery.singleTarget", false, true},
		{"Sorcery", false, false},
		{"Spell.YouCtrl", false, true},
		{"Spell.OppCtrl", false, false},
		{"Ability", false, false},
	}
	for _, tc := range cases {
		g, p, other := newTwoPlayerGame(t)
		seatRetargeter(t, g, p, "DB$ ChangeTargets | TargetType$ "+tc.targetType+" | ValidTgts$ Card")
		bolt := boltAt(t, g, p)
		original, redirected := other, p
		if tc.atCaster {
			original, redirected = p, other
		}
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.PlayerEntity(original)})
		if tc.offered {
			c.QueueTargets([]engine.EntityID{engine.CardEntity(bolt)})
			c.QueueTargets([]engine.EntityID{engine.PlayerEntity(redirected)})
		}
		if err := castW(t, g, p, bolt, c); err != nil {
			t.Fatalf("%s: %v", tc.targetType, err)
		}
		hit := original
		if tc.offered {
			hit = redirected
		}
		if got := g.Player(hit).Life; got != 17 {
			t.Errorf("%s (at caster %v): life of player hit = %d, want 17", tc.targetType, tc.atCaster, got)
		}
	}
}

// TestChangeTargetsTargetValidTargetingAndRestriction proves Rebound's
// shape: TargetValidTargeting$ Player admits a Bolt the caster aimed at
// themself, and TargetRestriction$ narrows the new-target offer, judged
// from the retargeter's controller -- here to the opponent alone.
func TestChangeTargetsTargetValidTargetingAndRestriction(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	seatRetargeter(t, g, p, "DB$ ChangeTargets | TargetType$ Spell.numTargets EQ1 | TargetValidTargeting$ Player | ValidTgts$ Card | TargetRestriction$ Player.Opponent")
	bolt := boltAt(t, g, p)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(p)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bolt)})
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	r := &targetOfferRecorder{ScriptedController: c}
	if err := castW(t, g, p, bolt, r); err != nil {
		t.Fatal(err)
	}
	if len(r.offers) != 3 || len(r.offers[2]) != 1 || r.offers[2][0] != engine.PlayerEntity(other) {
		t.Fatalf("offers = %v, want only the opponent as the new target", r.offers)
	}
	if g.Player(other).Life != 17 {
		t.Errorf("opponent life = %d, want 17", g.Player(other).Life)
	}
}

// TestChangeTargetsRestrictionOtherIsTheOldTarget proves Meddle's
// TargetRestriction$ Creature.Other: "Other" is read against the part's
// current card target (ChangeTargetsEffect.java:166-169), so the creature
// the spell already targets is not offered again.
func TestChangeTargetsRestrictionOtherIsTheOldTarget(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	seatRetargeter(t, g, p, "DB$ ChangeTargets | TargetType$ Spell | ValidTgts$ Card | TargetRestriction$ Creature.Other")
	a := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	spell := g.NewCard(instantDefWithAbility(t, "Destroy", "W", "SP$ Destroy | ValidTgts$ Creature"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(spell)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(b)})
	r := &targetOfferRecorder{ScriptedController: c}
	if err := castW(t, g, p, spell, r); err != nil {
		t.Fatal(err)
	}
	if len(r.offers) != 3 || len(r.offers[2]) != 1 || r.offers[2][0] != engine.CardEntity(b) {
		t.Fatalf("offers = %v, want only the other creature", r.offers)
	}
	if g.Card(a).Zone != engine.Battlefield || g.Card(b).Zone != engine.Graveyard {
		t.Errorf("a in %v, b in %v, want b destroyed instead of a", g.Card(a).Zone, g.Card(b).Zone)
	}
}

// TestChangeTargetsRejectsAnAnswerNotOffered proves a controller answer
// outside the new-target offer is an error, not a silent retarget.
func TestChangeTargetsRejectsAnAnswerNotOffered(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	seatRetargeter(t, g, p, "DB$ ChangeTargets | Defined$ TriggeredSpellAbility")
	bolt := boltAt(t, g, p)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bolt)})
	if err := castW(t, g, p, bolt, c); err == nil || !strings.Contains(err.Error(), "new targets") {
		t.Errorf("err = %v, want the new-targets choice error", err)
	}
}

// TestChangeTargetsTargetValidTargetingRefusesOtherTargets proves Muck
// Drubb's filter: a spell targeting a player is not a candidate for
// TargetValidTargeting$ Creature.inRealZoneBattlefield.
func TestChangeTargetsTargetValidTargetingRefusesOtherTargets(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	seatRetargeter(t, g, p, "DB$ ChangeTargets | TargetType$ Spell.numTargets EQ1 | TargetValidTargeting$ Creature.inRealZoneBattlefield | ValidTgts$ Card | DefinedMagnet$ Self")
	bolt := boltAt(t, g, p)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	if err := castW(t, g, p, bolt, c); err != nil {
		t.Fatal(err)
	}
	if g.Player(other).Life != 17 {
		t.Errorf("life = %d, want 17: no retargeting trigger", g.Player(other).Life)
	}
}

// TestChangeTargetsKeepsTargetsWithTooFewCandidates proves
// TargetSelection.chooseTargets' refusal: a part needs as many new targets
// as it had, and with fewer legal ones it keeps its own.
func TestChangeTargetsKeepsTargetsWithTooFewCandidates(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	seatRetargeter(t, g, p, "DB$ ChangeTargets | TargetType$ Spell | ValidTgts$ Card | TargetRestriction$ Creature")
	bolt := boltAt(t, g, p)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bolt)})
	if err := castW(t, g, p, bolt, c); err != nil {
		t.Fatal(err)
	}
	if g.Player(other).Life != 17 {
		t.Errorf("life = %d, want 17: no creature to move the Bolt to", g.Player(other).Life)
	}
}

// TestChangeTargetsOptionalDeclineAndTriggeredSpell proves Speedball's
// shape, Defined$ TriggeredSpellAbility | Optional$ True: declining keeps
// the target, accepting lets the activator choose a new one.
func TestChangeTargetsOptionalDeclineAndTriggeredSpell(t *testing.T) {
	t.Parallel()
	for _, accept := range []bool{false, true} {
		g, p, other := newTwoPlayerGame(t)
		seatRetargeter(t, g, p, "DB$ ChangeTargets | Defined$ TriggeredSpellAbility | Optional$ True")
		bolt := boltAt(t, g, p)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
		c.QueueConfirmEffect(accept)
		if accept {
			c.QueueTargets([]engine.EntityID{engine.PlayerEntity(p)})
		}
		if err := castW(t, g, p, bolt, c); err != nil {
			t.Fatal(err)
		}
		hit := other
		if accept {
			hit = p
		}
		if g.Player(hit).Life != 17 {
			t.Errorf("accept=%v: life of player hit = %d, want 17", accept, g.Player(hit).Life)
		}
	}
}

// TestChangeTargetsDefinedTargetedReadsTheParentsTarget proves Commandeer's
// and Aethersnatch's chain link, Defined$ Targeted: the spell the ability
// was put on the stack against.
func TestChangeTargetsDefinedTargetedReadsTheParentsTarget(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	bolt := boltAt(t, g, p)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	g.Player(p).ManaPool.Add(mana.White, 1)
	if !g.CastSpell(p, bolt, c) {
		t.Fatal("CastSpell failed")
	}
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(p)})
	if _, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(bolt)}, "DB$ ChangeTargets | Defined$ Targeted"); err != nil {
		t.Fatal(err)
	}
	if g.Player(p).Life != 17 || g.Player(other).Life != 20 {
		t.Errorf("life %d/%d, want 17/20", g.Player(p).Life, g.Player(other).Life)
	}
}

// TestChangeTargetsSingleTargetToMagnet proves Spellskite's shape,
// ChangeSingleTarget$ with DefinedMagnet$ Self: of a two-target spell, the
// activator picks one target, which becomes the magnet; the other stays.
func TestChangeTargetsSingleTargetToMagnet(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	skite := seatRetargeter(t, g, p, "DB$ ChangeTargets | TargetType$ Spell,Activated,Triggered | ValidTgts$ Card,Emblem | DefinedMagnet$ Self | ChangeSingleTarget$ True")
	a := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	spell := g.NewCard(instantDefWithAbility(t, "Double Destroy", "W", "SP$ Destroy | ValidTgts$ Permanent | TargetMin$ 2 | TargetMax$ 2"), p, engine.Hand)
	var sink recordingSink
	g.SetSink(&sink)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a), engine.CardEntity(b)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(spell)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a)})
	if err := castW(t, g, p, spell, c); err != nil {
		t.Fatal(err)
	}
	if g.Card(a).Zone != engine.Battlefield {
		t.Errorf("a in %v, want Battlefield: its target moved to the magnet", g.Card(a).Zone)
	}
	if g.Card(b).Zone != engine.Graveyard || g.Card(skite).Zone != engine.Graveyard {
		t.Errorf("b in %v, magnet in %v, want both destroyed", g.Card(b).Zone, g.Card(skite).Zone)
	}
}

// TestChangeTargetsSingleTargetKeepsAMagnetAlreadyTargeted proves the
// CR 115.3 guard: a part already targeting the magnet is left alone, and a
// single target is picked without asking.
func TestChangeTargetsSingleTargetKeepsAMagnetAlreadyTargeted(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	skite := seatRetargeter(t, g, p, "DB$ ChangeTargets | TargetType$ Spell | ValidTgts$ Card | DefinedMagnet$ Self | ChangeSingleTarget$ True")
	spell := g.NewCard(instantDefWithAbility(t, "Destroy", "W", "SP$ Destroy | ValidTgts$ Permanent"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(skite)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(spell)})
	if err := castW(t, g, p, spell, c); err != nil {
		t.Fatal(err)
	}
	if g.Card(skite).Zone != engine.Graveyard {
		t.Errorf("magnet in %v, want Graveyard", g.Card(skite).Zone)
	}
}

// TestChangeTargetsMagnetReplacesEveryTarget proves Muck Drubb's shape,
// DefinedMagnet$ without ChangeSingleTarget$: the part targets the magnet
// alone, and a magnet the part cannot target leaves it unchanged.
func TestChangeTargetsMagnetReplacesEveryTarget(t *testing.T) {
	t.Parallel()
	for _, spec := range []string{"Permanent", "Creature"} {
		g, p, other := newTwoPlayerGame(t)
		magnet := seatRetargeter(t, g, p, "DB$ ChangeTargets | TargetType$ Spell.numTargets EQ1 | TargetValidTargeting$ Creature.inRealZoneBattlefield | ValidTgts$ Card | DefinedMagnet$ Self")
		victim := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
		spell := g.NewCard(instantDefWithAbility(t, "Destroy", "W", "SP$ Destroy | ValidTgts$ "+spec), p, engine.Hand)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
		c.QueueTargets([]engine.EntityID{engine.CardEntity(spell)})
		if err := castW(t, g, p, spell, c); err != nil {
			t.Fatal(err)
		}
		movable := spec == "Permanent"
		if (g.Card(magnet).Zone == engine.Graveyard) != movable || (g.Card(victim).Zone == engine.Graveyard) == movable {
			t.Errorf("%s: magnet in %v, victim in %v", spec, g.Card(magnet).Zone, g.Card(victim).Zone)
		}
	}
}

// TestChangeTargetsRetargetsEachCharmMode proves the sub-instance walk: a
// Charm's chosen mode is its own targeting part, and changing it leaves a
// clone taken before untouched (Game.Clone shares the stack's Modes): the
// clone declines the change and its Charm still hits the original target.
func TestChangeTargetsRetargetsEachCharmMode(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	seatRetargeter(t, g, p, "DB$ ChangeTargets | Defined$ TriggeredSpellAbility | Optional$ True")
	charm := g.NewCard(spellDefWith(t, "Charm Bolt", "Instant", "W", "SP$ Charm | Choices$ DBDmg",
		"DBDmg", "DB$ DealDamage | ValidTgts$ Player | NumDmg$ 2"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueModeChoice([]int{0})
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	g.Player(p).ManaPool.Add(mana.White, 1)
	if !g.CastSpell(p, charm, c) {
		t.Fatal("CastSpell failed")
	}
	before := g.Clone()
	c.QueueConfirmEffect(true)
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(p)})
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if g.Player(p).Life != 18 || g.Player(other).Life != 20 {
		t.Errorf("life %d/%d, want 18/20", g.Player(p).Life, g.Player(other).Life)
	}
	declined := engine.NewScriptedController()
	declined.QueueConfirmEffect(false)
	if err := before.ResolveStack(engine.NewRegistry(), declined); err != nil {
		t.Fatal(err)
	}
	if before.Player(p).Life != 20 || before.Player(other).Life != 18 {
		t.Errorf("clone life %d/%d, want 20/18: the retarget leaked into the clone", before.Player(p).Life, before.Player(other).Life)
	}
}

// TestChangeTargetsRetargetsAnAbilityOnTheStack proves a triggered ability
// with a single target is a candidate for TargetType$
// SpellAbility.singleTarget, offered as its stack item (ADR-0041) above the
// Bolt, and retargeting it moves its damage. Spell.singleTarget over the same
// stack offers the Bolt alone.
func TestChangeTargetsRetargetsAnAbilityOnTheStack(t *testing.T) {
	t.Parallel()
	for _, targetType := range []string{"SpellAbility.singleTarget", "Spell.singleTarget"} {
		g, p, other := newTwoPlayerGame(t)
		// Seated first, so its trigger is on the stack when the
		// retargeter's own targets are chosen.
		g.NewCard(triggerWatcherDef(t, "Pinger",
			"Mode$ SpellCast | ValidCard$ Card | ValidActivatingPlayer$ You | Execute$ TrigPing",
			"TrigPing", "DB$ DealDamage | ValidTgts$ Player | NumDmg$ 1"), p, engine.Battlefield)
		seatRetargeter(t, g, p, "DB$ ChangeTargets | TargetType$ "+targetType+" | ValidTgts$ Card,Emblem")
		bolt := boltAt(t, g, p)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
		c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
		// Pushes in order: the Bolt (stack item 1), the Pinger trigger (2).
		pinger := engine.StackItemEntity(2)
		spellOnly := targetType == "Spell.singleTarget"
		if spellOnly {
			c.QueueTargets([]engine.EntityID{engine.CardEntity(bolt)})
		} else {
			c.QueueTargets([]engine.EntityID{pinger})
		}
		c.QueueTargets([]engine.EntityID{engine.PlayerEntity(p)})
		r := &targetOfferRecorder{ScriptedController: c}
		if err := castW(t, g, p, bolt, r); err != nil {
			t.Fatalf("%s: %v", targetType, err)
		}
		want := []engine.EntityID{pinger, engine.CardEntity(bolt)}
		if spellOnly {
			want = want[1:]
		}
		if len(r.offers) < 3 || !slices.Equal(r.offers[2], want) {
			t.Errorf("%s: retargeter offered %v, want %v", targetType, r.offers, want)
		}
		wantP, wantOther := 17, 19
		if !spellOnly {
			wantP, wantOther = 19, 17
		}
		if g.Player(p).Life != wantP || g.Player(other).Life != wantOther {
			t.Errorf("%s: life %d/%d, want %d/%d", targetType, g.Player(p).Life, g.Player(other).Life, wantP, wantOther)
		}
	}
}

// TestChangeTargetsRefusesUnbuiltShapes proves each shape this port does
// not resolve fails by name instead of acting on a guess (PORT-8, GO-7).
func TestChangeTargetsRefusesUnbuiltShapes(t *testing.T) {
	t.Parallel()
	cases := []struct{ line, want string }{
		{"DB$ ChangeTargets | Defined$ Targeted | Chooser$ Opponent", "Chooser$ not resolvable yet"},
		{"DB$ ChangeTargets | Defined$ TriggeredSpellAbility | RandomTarget$ True", "RandomTarget$ not resolvable yet"},
		{"DB$ ChangeTargets | Defined$ Remembered", `Defined$ "Remembered" not resolvable yet`},
		{"DB$ ChangeTargets | Defined$ TriggeredSourceSA", `Defined$ "TriggeredSourceSA" not resolvable yet`},
		{"DB$ ChangeTargets | Defined$ TriggeredSpellAbility | ModeCost$ 1", "ModeCost$ not resolvable yet"},
		{"DB$ ChangeTargets | Defined$ Targeted | ConditionTargetsSingleTarget$ True", "ConditionTargetsSingleTarget$ not resolvable yet"},
		{"DB$ ChangeTargets | Defined$ Targeted | ChangeSingleTarget$ True", "ChangeSingleTarget$ without DefinedMagnet$"},
		{"DB$ ChangeTargets | Defined$ TriggeredSpellAbility", "no triggering spell recorded"},
	}
	for _, tc := range cases {
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%q: err = %v, want %q", tc.line, err, tc.want)
		}
	}
}

// TestChangeTargetsRefusesUnreadableTargetType proves a TargetType$ kind or
// property this port does not read is an error at resolution, not a
// silently empty candidate set.
func TestChangeTargetsRefusesUnreadableTargetType(t *testing.T) {
	t.Parallel()
	for _, targetType := range []string{"!Spell", "Spell.Kicked", "Spell.numTargets EQ", "Spell.numTargets EQx", "Spell.IsTargeting Enchanted"} {
		g, p, other := newTwoPlayerGame(t)
		seatRetargeter(t, g, p, "DB$ ChangeTargets | TargetType$ "+targetType+" | ValidTgts$ Card")
		bolt := boltAt(t, g, p)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
		if err := castW(t, g, p, bolt, c); err == nil || !strings.Contains(err.Error(), "not resolvable yet") {
			t.Errorf("%s: err = %v, want not resolvable yet", targetType, err)
		}
	}
}

// TestChangeTargetsRefusesSpellsItCannotRewrite proves the spell-side
// refusals: an Aura spell's attach target, a SubAbility$ naming its own
// targets (never targeted separately in this port) and a divided target.
func TestChangeTargetsRefusesSpellsItCannotRewrite(t *testing.T) {
	t.Parallel()
	cases := []struct {
		name  string
		spell func(t *testing.T, g *engine.Game, p engine.PlayerID) engine.CardID
		want  string
	}{
		{"sub-ability targets", func(t *testing.T, g *engine.Game, p engine.PlayerID) engine.CardID {
			return g.NewCard(spellDefWith(t, "Bolt And Tap", "Instant", "W",
				"SP$ DealDamage | ValidTgts$ Player | NumDmg$ 3 | SubAbility$ DBTap",
				"DBTap", "DB$ Tap | ValidTgts$ Creature"), p, engine.Hand)
		}, "SubAbility$ targets not resolvable yet"},
		{"divided", func(t *testing.T, g *engine.Game, p engine.PlayerID) engine.CardID {
			return g.NewCard(instantDefWithAbility(t, "Split Bolt", "W",
				"SP$ DealDamage | ValidTgts$ Player | NumDmg$ 3 | DividedAsYouChoose$ 3"), p, engine.Hand)
		}, "divided target not resolvable yet"},
	}
	for _, tc := range cases {
		g, p, other := newTwoPlayerGame(t)
		seatRetargeter(t, g, p, "DB$ ChangeTargets | Defined$ TriggeredSpellAbility")
		spell := tc.spell(t, g, p)
		// The sub-ability's own target is chosen at cast time now
		// (resolveChainTargets), so a creature must exist for it.
		victim := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
		c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
		if err := castW(t, g, p, spell, c); err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want %q", tc.name, err, tc.want)
		}
	}
}

// TestChangeTargetsCountingASubAbilityTargetIsAnError proves the
// candidate-side refusal: singleTarget cannot count a spell whose
// SubAbility$ names its own targets.
func TestChangeTargetsCountingASubAbilityTargetIsAnError(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	seatRetargeter(t, g, p, "DB$ ChangeTargets | TargetType$ Spell.singleTarget | ValidTgts$ Card")
	spell := g.NewCard(spellDefWith(t, "Bolt And Tap", "Instant", "W",
		"SP$ DealDamage | ValidTgts$ Player | NumDmg$ 3 | SubAbility$ DBTap",
		"DBTap", "DB$ Tap | ValidTgts$ Creature"), p, engine.Hand)
	victim := g.NewCard(creatureDefPT(t, "1", "1"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(victim)})
	if err := castW(t, g, p, spell, c); err == nil || !strings.Contains(err.Error(), "SubAbility$ targets not resolvable yet") {
		t.Errorf("err = %v, want the SubAbility$ targets error", err)
	}
}

// TestChangeTargetsCountsAnAuraAndACharmMode proves singleTarget counts an
// Aura spell's attach target and a Charm mode's targets: both are offered.
// A Charm mode is then retargeted; an Aura's attach target is refused.
func TestChangeTargetsCountsAnAuraAndACharmMode(t *testing.T) {
	t.Parallel()
	t.Run("aura", func(t *testing.T) {
		t.Parallel()
		g, p, other := newTwoPlayerGame(t)
		seatRetargeter(t, g, p, "DB$ ChangeTargets | TargetType$ Spell.singleTarget | ValidTgts$ Card")
		g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
		aura := g.NewCard(auraDefWithEnchant(t, "Creature"), p, engine.Hand)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.CardEntity(aura)})
		if err := castW(t, g, p, aura, c); err == nil || !strings.Contains(err.Error(), "Aura spell's target not resolvable yet") {
			t.Errorf("err = %v, want the Aura refusal", err)
		}
	})
	t.Run("charm", func(t *testing.T) {
		t.Parallel()
		g, p, other := newTwoPlayerGame(t)
		seatRetargeter(t, g, p, "DB$ ChangeTargets | TargetType$ Spell.singleTarget | ValidTgts$ Card")
		charm := g.NewCard(spellDefWith(t, "Charm Bolt", "Instant", "W", "SP$ Charm | Choices$ DBDmg",
			"DBDmg", "DB$ DealDamage | ValidTgts$ Player | NumDmg$ 2"), p, engine.Hand)
		c := engine.NewScriptedController()
		c.QueueModeChoice([]int{0})
		c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
		c.QueueTargets([]engine.EntityID{engine.CardEntity(charm)})
		c.QueueTargets([]engine.EntityID{engine.PlayerEntity(p)})
		if err := castW(t, g, p, charm, c); err != nil {
			t.Fatal(err)
		}
		if g.Player(p).Life != 18 {
			t.Errorf("caster life = %d, want 18", g.Player(p).Life)
		}
	})
}

// TestChangeTargetsWithoutAMagnetOrATarget proves the no-op edges: a
// DefinedMagnet$ naming no card changes nothing, and ChangeSingleTarget$
// over a spell with no target ends the resolution quietly.
func TestChangeTargetsWithoutAMagnetOrATarget(t *testing.T) {
	t.Parallel()
	for _, line := range []string{
		"DB$ ChangeTargets | Defined$ TriggeredSpellAbility | DefinedMagnet$ Enchanted",
		"DB$ ChangeTargets | Defined$ TriggeredSpellAbility | DefinedMagnet$ Enchanted | ChangeSingleTarget$ True",
	} {
		g, p, other := newTwoPlayerGame(t)
		seatRetargeter(t, g, p, line)
		bolt := boltAt(t, g, p)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
		if err := castW(t, g, p, bolt, c); err != nil {
			t.Fatalf("%s: %v", line, err)
		}
		if g.Player(other).Life != 17 {
			t.Errorf("%s: life = %d, want 17", line, g.Player(other).Life)
		}
	}
	g, p, _ := newTwoPlayerGame(t)
	seatRetargeter(t, g, p, "DB$ ChangeTargets | Defined$ TriggeredSpellAbility | DefinedMagnet$ Self | ChangeSingleTarget$ True")
	gain := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Hand)
	if err := castW(t, g, p, gain, engine.NewScriptedController()); err != nil {
		t.Fatal(err)
	}
	if g.Player(p).Life != 23 {
		t.Errorf("life = %d, want 23", g.Player(p).Life)
	}
}

// TestChangeTargetsIsTargetingSelf proves Torchling's shape, TargetType$
// Spell.numTargets EQ1+IsTargeting Self: a spell aimed at the retargeter's
// own host is offered and moved to another permanent.
func TestChangeTargetsIsTargetingSelf(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	host := seatRetargeter(t, g, p, "DB$ ChangeTargets | TargetType$ Spell.numTargets EQ1+IsTargeting Self | ValidTgts$ Card")
	decoy := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	spell := g.NewCard(instantDefWithAbility(t, "Destroy", "W", "SP$ Destroy | ValidTgts$ Permanent"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(host)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(spell)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(decoy)})
	if err := castW(t, g, p, spell, c); err != nil {
		t.Fatal(err)
	}
	if g.Card(host).Zone != engine.Battlefield || g.Card(decoy).Zone != engine.Graveyard {
		t.Errorf("host in %v, decoy in %v, want the decoy destroyed instead", g.Card(host).Zone, g.Card(decoy).Zone)
	}
}

// TestCharmOffersAStackTargetingModeOnlyWithACandidate proves
// makePossibleOptions' CR 603.3c filter for a mode naming TargetType$
// (Insidious Will, Untimely Malfunction): over an empty stack the
// ChangeTargets mode is not offered, so the one remaining option, index 0,
// is the GainLife mode and the Charm is cast.
func TestCharmOffersAStackTargetingModeOnlyWithACandidate(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	// A battlefield card: the battlefield scan would have offered the
	// ChangeTargets mode for it.
	g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	charm := g.NewCard(spellDefWith(t, "Will Charm", "Instant", "W", "SP$ Charm | Choices$ DBChange,DBGain",
		"DBChange", "DB$ ChangeTargets | TargetType$ Spell | ValidTgts$ Card",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 4"), p, engine.Hand)
	var offered []string
	c := &modeRecorder{ScriptedController: engine.NewScriptedController(), offered: &offered}
	c.QueueModeChoice([]int{0})
	if err := castW(t, g, p, charm, c); err != nil {
		t.Fatal(err)
	}
	if len(offered) != 1 || offered[0] != "DBGain" {
		t.Errorf("modes offered = %v, want only DBGain", offered)
	}
	if g.Player(p).Life != 24 {
		t.Errorf("life = %d, want 24", g.Player(p).Life)
	}
}

// modeRecorder records the mode names ChooseModesForAbility is offered.
type modeRecorder struct {
	*engine.ScriptedController
	offered *[]string
}

func (r *modeRecorder) ChooseModesForAbility(g *engine.Game, p engine.PlayerID, src engine.CardID, options []string, lo, hi int) []int {
	*r.offered = append(*r.offered, options...)
	return r.ScriptedController.ChooseModesForAbility(g, p, src, options, lo, hi)
}
