package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// pushPlayLine puts a host for trig on p's battlefield (no ETB fired) and
// pushes trig as p's ability with targets, leaving the caller to set up the
// host's memory before ResolveStack.
func pushPlayLine(t *testing.T, g *engine.Game, p engine.PlayerID, targets []engine.EntityID, trig string, svars ...string) engine.CardID {
	t.Helper()
	def := etbChainDef(t, "Test Play Host", trig, svars...)
	host := g.NewCard(def, p, engine.Battlefield)
	face := def.Faces[0]
	for _, sub := range face.Triggers[0].Subs {
		if strings.EqualFold(sub.Key, "Execute") {
			api, ok := engine.APIByName(sub.Ability.Name)
			if !ok {
				t.Fatalf("unknown API %q", sub.Ability.Name)
			}
			g.PushAbility(engine.Ability{API: api, Source: host, Controller: p, Params: sub.Ability, Amounts: face.Amounts, Targets: targets})
			return host
		}
	}
	t.Fatal("no Execute$")
	return engine.NoCard
}

// gainInstant is an instant gaining its caster n life, costing cost.
func gainInstant(t *testing.T, name, cost, n string) *compile.Card {
	t.Helper()
	return instantDefWithAbility(t, name, cost, "SP$ GainLife | Defined$ You | LifeAmount$ "+n)
}

// resolvePlay remembers each card on host, then resolves the stack.
func resolvePlay(t *testing.T, g *engine.Game, host engine.CardID, c engine.PlayerController, remembered ...engine.CardID) error {
	t.Helper()
	for _, id := range remembered {
		g.Card(host).Memory.Remember(engine.CardEntity(id))
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

// TestPlayCastsRememberedInstantWithoutPaying proves the dominant shape:
// Defined$ Remembered | ValidSA$ Spell | WithoutManaCost$ | Optional$ casts
// the exiled instant as a real spell on the stack above the resolving Play,
// which then resolves and goes to its owner's graveyard; RememberPlayed$
// records it, and the cast counts as a cast (SpellsCastThisTurn).
func TestPlayCastsRememberedInstantWithoutPaying(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	spell := g.NewCard(gainInstant(t, "Gain", "4 W", "4"), p, engine.Exile)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(true)
	host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | ValidSA$ Spell | WithoutManaCost$ True | Optional$ True | RememberPlayed$ True")
	if err := resolvePlay(t, g, host, c, spell); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(p).Life; got != 24 {
		t.Errorf("life = %d, want 24", got)
	}
	if got := g.Card(spell).Zone; got != engine.Graveyard {
		t.Errorf("spell zone = %v, want Graveyard", got)
	}
	if got := g.Player(p).SpellsCastThisTurn; got != 1 {
		t.Errorf("SpellsCastThisTurn = %d, want 1: Play casts", got)
	}
	if rem := g.Card(host).Memory.Remembered(); len(rem) != 1 || rem[0] != engine.CardEntity(spell) {
		t.Errorf("remembered = %v, want the played spell", rem)
	}
}

// TestPlayCastSpellWaitsOnTheStack proves the cast spell is a stack object
// of its own (ADR-0018): resolving only the Play leaves the spell on the
// stack, unresolved, with a SpellCast trigger above it.
func TestPlayCastSpellWaitsOnTheStack(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(spellCastWatcherDef(t, "Watcher", "You"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Library)
	spell := g.NewCard(gainInstant(t, "Gain", "W", "4"), p, engine.Exile)
	host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | WithoutManaCost$ True")
	g.Card(host).Memory.Remember(engine.CardEntity(spell))
	top, _ := g.StackTop()
	if err := engine.NewRegistry().Resolve(g, &top, engine.NewScriptedController()); err != nil {
		t.Fatal(err)
	}
	if got := g.Card(spell).Zone; got != engine.Stack {
		t.Fatalf("spell zone = %v, want Stack", got)
	}
	if got := g.StackLen(); got != 3 {
		t.Fatalf("StackLen = %d, want 3: the Play, the spell, the SpellCast trigger", got)
	}
	if top, _ := g.StackTop(); top.API != engine.APIDraw {
		t.Errorf("top = %v, want the Watcher's Draw trigger", top.API)
	}
}

// TestPlayOptionalDeclined proves a declined single option casts nothing.
func TestPlayOptionalDeclined(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	spell := g.NewCard(gainInstant(t, "Gain", "W", "4"), p, engine.Exile)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(false)
	host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | WithoutManaCost$ True | Optional$ True")
	if err := resolvePlay(t, g, host, c, spell); err != nil {
		t.Fatal(err)
	}
	if g.Card(spell).Zone != engine.Exile || g.Player(p).Life != 20 {
		t.Errorf("zone %v life %d: a declined Play must do nothing", g.Card(spell).Zone, g.Player(p).Life)
	}
}

// TestPlayOptionalChoiceStops proves an optional pick among several can
// choose nothing, ending the loop.
func TestPlayOptionalChoiceStops(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(gainInstant(t, "A", "W", "1"), p, engine.Exile)
	b := g.NewCard(gainInstant(t, "B", "W", "2"), p, engine.Exile)
	c := engine.NewScriptedController()
	c.QueueCardChoice(nil)
	host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | WithoutManaCost$ True | Optional$ True")
	if err := resolvePlay(t, g, host, c, a, b); err != nil {
		t.Fatal(err)
	}
	if g.Player(p).Life != 20 {
		t.Errorf("life = %d, want 20", g.Player(p).Life)
	}
}

// TestPlayValidZoneFiltersByValidSACmc proves Valid$/ValidZone$ candidates
// narrowed by ValidSA$ Spell.cmcLEX (X an SVar on the host, 2): the mana value
// 4 instant is never offered, and the chosen one is cast paying its cost.
func TestPlayValidZoneFiltersByValidSACmc(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	cheap := g.NewCard(gainInstant(t, "Cheap", "R", "1"), p, engine.Hand)
	g.NewCard(gainInstant(t, "Dear", "3 R", "9"), p, engine.Hand)
	other := g.NewCard(gainInstant(t, "Other", "1 R", "2"), p, engine.Hand)
	g.Player(p).ManaPool.Add(mana.Red, 1)
	// X counts Elves: the Play host and this one.
	g.NewCard(etbChainDef(t, "Other Elf", "DB$ BlankLine"), p, engine.Battlefield)
	rec := &offerRecorder{ScriptedController: engine.NewScriptedController()}
	rec.QueueCardChoice([]engine.CardID{cheap})
	pushPlayLine(t, g, p, nil, "DB$ Play | Valid$ Card.nonLand+YouOwn | ValidZone$ Hand | ValidSA$ Spell.cmcLEX | Optional$ True",
		"X", "Count$Valid Elf")
	if err := g.ResolveStack(engine.NewRegistry(), rec); err != nil {
		t.Fatal(err)
	}
	if len(rec.offers) != 1 || len(rec.offers[0]) != 2 || rec.offers[0][0] != cheap || rec.offers[0][1] != other {
		t.Fatalf("offers = %v, want [[cheap other]]", rec.offers)
	}
	if g.Player(p).Life != 21 || g.Player(p).ManaPool.Total() != 0 {
		t.Errorf("life %d pool %d, want 21 and the R paid", g.Player(p).Life, g.Player(p).ManaPool.Total())
	}
}

// TestPlayValidZonesScanZoneByZone proves Valid$ over a zone list offers
// cards zone-major, each zone across every player (Game.getCardsIn).
func TestPlayValidZonesScanZoneByZone(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	oppGrave := g.NewCard(gainInstant(t, "OppGrave", "W", "1"), other, engine.Graveyard)
	myExile := g.NewCard(gainInstant(t, "MyExile", "W", "1"), p, engine.Exile)
	myGrave := g.NewCard(gainInstant(t, "MyGrave", "W", "1"), p, engine.Graveyard)
	rec := &offerRecorder{ScriptedController: engine.NewScriptedController()}
	rec.QueueCardChoice(nil)
	pushPlayLine(t, g, p, nil, "DB$ Play | Valid$ Instant | ValidZone$ Graveyard,Exile | Optional$ True")
	if err := g.ResolveStack(engine.NewRegistry(), rec); err != nil {
		t.Fatal(err)
	}
	want := []engine.CardID{myGrave, oppGrave, myExile}
	if len(rec.offers) != 1 || len(rec.offers[0]) != 3 {
		t.Fatalf("offers = %v, want one offer of %v", rec.offers, want)
	}
	for i, id := range want {
		if rec.offers[0][i] != id {
			t.Errorf("offer[%d] = %v, want %v", i, rec.offers[0][i], id)
		}
	}
}

// TestPlayCopyCardCastsATokenCopy proves CopyCard$: a token copy of the
// chosen card is cast from its zone, the original stays put, and the token
// copy -- an instant, in the graveyard after resolving -- ceases to exist
// at the next state-based check (CR 704.5d).
func TestPlayCopyCardCastsATokenCopy(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	spell := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Exile)
	host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | ValidSA$ Spell | WithoutManaCost$ True | CopyCard$ True | RememberPlayed$ True")
	if err := resolvePlay(t, g, host, engine.NewScriptedController(), spell); err != nil {
		t.Fatal(err)
	}
	if g.Player(p).Life != 23 || g.Card(spell).Zone != engine.Exile {
		t.Errorf("life %d original %v, want 23 and the original still exiled", g.Player(p).Life, g.Card(spell).Zone)
	}
	if n := len(g.Zone(engine.Graveyard, p).Cards()); n != 0 {
		t.Errorf("graveyard holds %d cards, want 0: the token copy ceases to exist", n)
	}
	rem := g.Card(host).Memory.Remembered()
	if len(rem) != 2 || rem[1] == engine.CardEntity(spell) {
		t.Errorf("remembered = %v, want the original then the played token copy", rem)
	}
}

// TestPlayPlaysALandOnlyWithALandDropLeft proves the land branch: on the
// caster's own turn with an unused land drop the land is played
// (LandsPlayed counts it); with the drop spent, or on another player's
// turn, the land is not an option.
func TestPlayPlaysALandOnlyWithALandDropLeft(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		name         string
		spent, other bool
		want         engine.ZoneType
	}{
		{"drop left", false, false, engine.Battlefield},
		{"drop spent", true, false, engine.Exile},
		{"their turn", false, true, engine.Exile},
	} {
		g, p, opp := newTwoPlayerGame(t)
		land := g.NewCard(landDef(t, "Forest", "Basic Land Forest"), p, engine.Exile)
		if tc.spent {
			g.Player(p).LandsPlayed = 1
		}
		if tc.other {
			g.SetTurnState(1, opp, engine.Main1)
		}
		host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | ImprintPlayed$ True")
		if err := resolvePlay(t, g, host, engine.NewScriptedController(), land); err != nil {
			t.Fatal(err)
		}
		if got := g.Card(land).Zone; got != tc.want {
			t.Errorf("%s: land zone %v, want %v", tc.name, got, tc.want)
		}
		if tc.want == engine.Battlefield && (g.Player(p).LandsPlayed != 1 || len(g.Card(host).Memory.Imprinted()) != 1) {
			t.Errorf("%s: LandsPlayed %d imprinted %v, want 1 and the land", tc.name, g.Player(p).LandsPlayed, g.Card(host).Memory.Imprinted())
		}
	}
}

// TestPlayAmountAllCastsEach proves Amount$ All: every candidate is offered
// in turn and cast, ImprintPlayed$ records each and ForgetPlayed$ forgets
// each from the remembered list.
func TestPlayAmountAllCastsEach(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	a := g.NewCard(gainInstant(t, "A", "W", "1"), p, engine.Exile)
	b := g.NewCard(gainInstant(t, "B", "W", "2"), p, engine.Exile)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{b})
	host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | Amount$ All | WithoutManaCost$ True | ImprintPlayed$ True | ForgetPlayed$ True")
	if err := resolvePlay(t, g, host, c, a, b); err != nil {
		t.Fatal(err)
	}
	if g.Player(p).Life != 23 {
		t.Errorf("life = %d, want 23", g.Player(p).Life)
	}
	if len(g.Card(host).Memory.Imprinted()) != 2 || len(g.Card(host).Memory.Remembered()) != 0 {
		t.Errorf("imprinted %v remembered %v, want both imprinted and forgotten",
			g.Card(host).Memory.Imprinted(), g.Card(host).Memory.Remembered())
	}
}

// TestPlayValidSAExcludesOtherKinds proves ValidSA$ Instant,Sorcery keeps
// a creature out of the pool, so nothing is played.
func TestPlayValidSAExcludesOtherKinds(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	bear := g.NewCard(creatureDefCost(t, "Bear", "G"), p, engine.Hand)
	pushPlayLine(t, g, p, nil, "DB$ Play | Valid$ Card.YouOwn | ValidSA$ Instant,Sorcery | WithoutManaCost$ True")
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatal(err)
	}
	if g.Card(bear).Zone != engine.Hand {
		t.Errorf("bear zone %v, want Hand", g.Card(bear).Zone)
	}
}

// TestPlayCastsAnOpponentsCardUnderTheCaster proves CR 110.2: a spell Play
// casts from an opponent's exile is controlled by the caster, so a
// permanent resolves onto the caster's battlefield under the caster, and
// an instant returns to its owner's graveyard and control (CR 108.4a).
func TestPlayCastsAnOpponentsCardUnderTheCaster(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	bear := g.NewCard(creatureDefCost(t, "Bear", "G"), other, engine.Exile)
	gain := g.NewCard(gainInstant(t, "Gain", "W", "3"), other, engine.Exile)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{bear})
	host := pushPlayLine(t, g, p, nil, "DB$ Play | Valid$ Card.IsRemembered | ValidZone$ Exile | Controller$ You | WithoutManaCost$ True | Amount$ All")
	if err := resolvePlay(t, g, host, c, bear, gain); err != nil {
		t.Fatal(err)
	}
	if g.Card(bear).Zone != engine.Battlefield || g.Card(bear).Controller() != p {
		t.Errorf("bear zone %v controller %v, want battlefield under %v", g.Card(bear).Zone, g.Card(bear).Controller(), p)
	}
	if g.Player(p).Life != 23 || g.Player(other).Life != 20 {
		t.Errorf("life %d/%d, want the caster to gain 3", g.Player(p).Life, g.Player(other).Life)
	}
	if g.Card(gain).Zone != engine.Graveyard || g.Card(gain).ZoneOwner != other || g.Card(gain).Controller() != other {
		t.Errorf("instant zone %v/%v controller %v, want its owner's graveyard and control", g.Card(gain).Zone, g.Card(gain).ZoneOwner, g.Card(gain).Controller())
	}
}

// TestPlayCastsATargetedInstantAndAnAura proves the cast path's own
// choices run for a Play cast: an instant's ValidTgts$ asks ChooseTargets,
// an Aura its enchant target.
func TestPlayCastsATargetedInstantAndAnAura(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	bolt := g.NewCard(instantDefWithAbility(t, "Bolt", "R", "SP$ DealDamage | ValidTgts$ Player | NumDmg$ 3"), p, engine.Exile)
	bear := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	aura := g.NewCard(auraDefWithEnchant(t, "Creature"), p, engine.Exile)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{bolt})
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueEnchantTarget(bear)
	host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | Amount$ All | WithoutManaCost$ True")
	if err := resolvePlay(t, g, host, c, bolt, aura); err != nil {
		t.Fatal(err)
	}
	if g.Player(other).Life != 17 {
		t.Errorf("opponent life = %d, want 17", g.Player(other).Life)
	}
	if host, ok := g.Card(aura).AttachedTo(); g.Card(aura).Zone != engine.Battlefield || !ok || host != bear {
		t.Errorf("aura zone %v attached %v, want on the battlefield on the bear", g.Card(aura).Zone, host)
	}
}

// TestPlayRejectsUnbuiltShapes pins the fail-closed contract: each
// unresolved param or valid-string property, and a chosen card whose spell
// this port cannot cast, errors before anything moves.
func TestPlayRejectsUnbuiltShapes(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		line, want string
	}{
		{"DB$ Play | Defined$ Remembered | ReplaceGraveyard$ Exile", "ReplaceGraveyard$"},
		{"DB$ Play | Defined$ Remembered | TgtZone$ Graveyard", "TgtZone$"},
		{"DB$ Play | Defined$ Remembered | ConditionDefined$ Remembered | ConditionPresent$ Card", "ConditionDefined$"},
		{"DB$ Play | Valid$ Card.ExiledWithSource | ValidZone$ Exile", "ExiledWithSource"},
		{"DB$ Play | Valid$ Card.cmcLEX | ValidZone$ Exile", "cmcLEX"},
		{"DB$ Play | Valid$ Card | ValidZone$ Nowhere", "ValidZone$"},
		{"DB$ Play | Defined$ Remembered | ValidSA$ Spell.faceUp", "faceUp"},
		{"DB$ Play | Defined$ Remembered | ValidSA$ Spell.cmcLEZ", "cmcLEZ"},
		{"DB$ Play | Defined$ Bogus", "Bogus"},
		{"DB$ Play | Defined$ Remembered | Controller$ Bogus", "Controller$"},
		{"DB$ Play | Defined$ Remembered | Amount$ Bogus", "Amount"},
	} {
		g, p, _ := newTwoPlayerGame(t)
		spell := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Exile)
		host := pushPlayLine(t, g, p, nil, tc.line)
		err := resolvePlay(t, g, host, engine.NewScriptedController(), spell)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want one naming %s", tc.line, err, tc.want)
		}
		if g.Card(spell).Zone != engine.Exile {
			t.Errorf("%s: the card moved before the rejection", tc.line)
		}
	}
}

// TestPlayRejectsSpellsItCannotCast proves a chosen card castSpell cannot
// cast the way Java would -- a split card's choice of spells, an instant
// with an additional Cost$ or two A:SP$ lines -- is an error, not a guess.
func TestPlayRejectsSpellsItCannotCast(t *testing.T) {
	t.Parallel()
	split := gainInstant(t, "Split", "W", "1")
	split.SplitType = carddb.SplitSplit
	costed := instantDefWithAbility(t, "Costed", "W", "SP$ GainLife | Cost$ W Sac<1/Creature> | Defined$ You | LifeAmount$ 1")
	twoSpells := gainInstant(t, "Twice", "W", "1")
	twoSpells.Faces[0].Abilities = append(twoSpells.Faces[0].Abilities, twoSpells.Faces[0].Abilities[0])
	noSpell := gainInstant(t, "None", "W", "1")
	noSpell.Faces[0].Abilities = nil
	for _, def := range []*compile.Card{split, costed, twoSpells, noSpell} {
		g, p, _ := newTwoPlayerGame(t)
		card := g.NewCard(def, p, engine.Exile)
		host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | ValidSA$ Spell | WithoutManaCost$ True")
		err := resolvePlay(t, g, host, engine.NewScriptedController(), card)
		if err == nil || !strings.Contains(err.Error(), "not resolvable yet") {
			t.Errorf("%s: err = %v, want not resolvable yet", def.Name, err)
		}
		if g.Card(card).Zone != engine.Exile {
			t.Errorf("%s: moved before the rejection", def.Name)
		}
	}
}

// TestPlayAllowRepeatsCopiesAndRefusesToLoop proves Mnemonic Deluge's shape
// -- CopyCard$ | Amount$ 3 | AllowRepeats$ -- casts three token copies of
// the one remembered instant, and that a repeatable card with nothing to
// play is an error rather than Java's endless re-offer (PlayEffect.java:312).
func TestPlayAllowRepeatsCopiesAndRefusesToLoop(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	spell := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Exile)
	host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | CopyCard$ True | Amount$ 3 | AllowRepeats$ True | WithoutManaCost$ True | ValidSA$ Spell")
	if err := resolvePlay(t, g, host, engine.NewScriptedController(), spell); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(p).Life; got != 29 {
		t.Errorf("life = %d, want 29 from three copies", got)
	}

	for _, tc := range []struct {
		name string
		def  *compile.Card
	}{
		{"land with no drop", landDef(t, "Forest", "Basic Land Forest")},
		{"no mana cost", func() *compile.Card {
			d := gainInstant(t, "Free", "W", "1")
			d.Faces[0].ManaCost = mana.NoCost()
			return d
		}()},
	} {
		g, p, _ := newTwoPlayerGame(t)
		g.Player(p).LandsPlayed = 1
		card := g.NewCard(tc.def, p, engine.Exile)
		host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | Amount$ 3 | AllowRepeats$ True")
		err := resolvePlay(t, g, host, engine.NewScriptedController(), card)
		if err == nil || !strings.Contains(err.Error(), "AllowRepeats$") {
			t.Errorf("%s: err = %v, want the AllowRepeats$ loop refused", tc.name, err)
		}
	}
}

// TestPlayValidSAVocabulary pins validSAMatches' heads and properties
// against an instant in exile: which ValidSA$ values let it be played.
func TestPlayValidSAVocabulary(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		validSA string
		cast    bool
	}{
		{"Spell.YouCtrl", true}, {"Spell.OppCtrl", false}, {"SpellAbility.Instant", true},
		{"Sorcery", false}, {"!Spell", false}, {"Spell.!Creature", true}, {"LandAbility", false},
		{"Activated,Instant.cmcEQ1", true}, {"Spell.cmcGE2", false}, {"Spell.White+YouOwn", true},
	} {
		g, p, _ := newTwoPlayerGame(t)
		spell := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Exile)
		host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | WithoutManaCost$ True | ValidSA$ "+tc.validSA)
		if err := resolvePlay(t, g, host, engine.NewScriptedController(), spell); err != nil {
			t.Fatalf("%s: %v", tc.validSA, err)
		}
		if cast := g.Card(spell).Zone == engine.Graveyard; cast != tc.cast {
			t.Errorf("ValidSA$ %s: cast = %v, want %v", tc.validSA, cast, tc.cast)
		}
	}
	g, p, _ := newTwoPlayerGame(t)
	land := g.NewCard(landDef(t, "Forest", "Basic Land Forest"), p, engine.Exile)
	host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | ValidSA$ SpellAbility.Land")
	if err := resolvePlay(t, g, host, engine.NewScriptedController(), land); err != nil {
		t.Fatal(err)
	}
	if g.Card(land).Zone != engine.Battlefield {
		t.Errorf("land zone %v, want Battlefield under SpellAbility.Land", g.Card(land).Zone)
	}
}

// TestPlayPaysAndSkipsUnpayable proves a Play without WithoutManaCost$
// pays the spell's cost, declining when the pool cannot, and skips a "no
// cost" card entirely (PlayEffect.java:387-389).
func TestPlayPaysAndSkipsUnpayable(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	spell := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Exile)
	host := pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | RememberPlayed$ True | ForgetRemembered$ True")
	if err := resolvePlay(t, g, host, engine.NewScriptedController(), spell); err != nil {
		t.Fatal(err)
	}
	if g.Card(spell).Zone != engine.Exile || len(g.Card(host).Memory.Remembered()) != 1 {
		t.Error("cast a spell nobody could pay for, or recorded a failed play")
	}

	free := gainInstant(t, "Free", "W", "3")
	free.Faces[0].ManaCost = mana.NoCost()
	nocost := g.NewCard(free, p, engine.Exile)
	host = pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered")
	if err := resolvePlay(t, g, host, engine.NewScriptedController(), nocost); err != nil {
		t.Fatal(err)
	}
	if g.Card(nocost).Zone != engine.Exile {
		t.Error("cast a no-cost card by paying")
	}

	g.Player(p).ManaPool.Add(mana.White, 1)
	host = pushPlayLine(t, g, p, nil, "DB$ Play | Defined$ Remembered | RememberPlayed$ True | ForgetRemembered$ True")
	if err := resolvePlay(t, g, host, engine.NewScriptedController(), spell); err != nil {
		t.Fatal(err)
	}
	if g.Card(spell).Zone != engine.Graveyard || g.Player(p).Life != 23 {
		t.Errorf("zone %v life %d, want the paid spell resolved", g.Card(spell).Zone, g.Player(p).Life)
	}
	if rem := g.Card(host).Memory.Remembered(); len(rem) != 0 {
		t.Errorf("remembered = %v, want cleared by ForgetRemembered$", rem)
	}
}
