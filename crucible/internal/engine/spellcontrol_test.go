package engine_test

import (
	"slices"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// aethersnatchDef is aethersnatch.txt at one U: gain control of target
// spell, then you may choose new targets for it.
func aethersnatchDef(t *testing.T) *compile.Card {
	t.Helper()
	return spellDefWith(t, "Aethersnatch", "Instant", "U",
		"SP$ ControlSpell | ValidTgts$ Card | TargetType$ Spell | Mode$ Gain | SubAbility$ DBChooseTargets",
		"DBChooseTargets", "DB$ ChangeTargets | Defined$ Targeted | Optional$ True")
}

// chimeraDef is perplexing_chimera.txt: whenever an opponent casts a
// spell, its controller may exchange control of it and that spell, then
// choose new targets for the spell.
func chimeraDef(t *testing.T) *compile.Card {
	t.Helper()
	return scriptDef(t, "Perplexing Chimera", "Enchantment Creature",
		"T:Mode$ SpellCast | ValidCard$ Card | ValidActivatingPlayer$ Opponent | OptionalDecider$ You | Execute$ ExchangeControlSpell | TriggerZones$ Battlefield",
		"SVar:ExchangeControlSpell:DB$ ControlSpell | Defined$ TriggeredSpellAbility | Mode$ Exchange | Remember$ True | SubAbility$ DBChooseTargets",
		"SVar:DBChooseTargets:DB$ ChangeTargets | Defined$ TriggeredSpellAbility | Optional$ True | ConditionDefined$ Remembered | ConditionPresent$ Card | ConditionCompare$ GE2 | SubAbility$ DBCleanup",
		"SVar:DBCleanup:DB$ Cleanup | ClearRemembered$ True")
}

// counterWatcherDef counters the spell its controller casts, as a trigger
// that lands above that spell's other cast triggers when its controller is
// the non-active player (CR 603.3b).
func counterWatcherDef(t *testing.T) *compile.Card {
	t.Helper()
	return triggerWatcherDef(t, "Counter Watcher",
		"Mode$ SpellCast | ValidCard$ Card | ValidActivatingPlayer$ You | Execute$ TrigCounter",
		"TrigCounter", "DB$ Counter | TargetType$ Spell | ValidTgts$ Card")
}

// snatch casts Aethersnatch for thief at spell, which must already be on
// the stack.
func snatch(t *testing.T, g *engine.Game, thief engine.PlayerID, spell engine.CardID, c *engine.ScriptedController) {
	t.Helper()
	card := g.NewCard(aethersnatchDef(t), thief, engine.Hand)
	g.Player(thief).ManaPool.Add(mana.Blue, 1)
	c.QueueTargets([]engine.EntityID{engine.CardEntity(spell)})
	if !g.CastSpell(thief, card, c) {
		t.Fatal("CastSpell(Aethersnatch) failed")
	}
}

// castOn casts card for p paying one W, leaving it on the stack.
func castOn(t *testing.T, g *engine.Game, p engine.PlayerID, card engine.CardID, c *engine.ScriptedController) {
	t.Helper()
	g.Player(p).ManaPool.Add(mana.White, 1)
	if !g.CastSpell(p, card, c) {
		t.Fatal("CastSpell failed")
	}
}

// TestControlSpellStolenCreatureEntersUnderTheThief proves CR 608.3a
// through ControlSpell's dominant shape (Aethersnatch): the creature spell
// resolves onto the thief's battlefield, controlled by the thief, still
// owned by its caster.
func TestControlSpellStolenCreatureEntersUnderTheThief(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	creature := g.NewCard(creatureDefManaCost(t, "W"), p, engine.Hand)
	c := engine.NewScriptedController()
	castOn(t, g, p, creature, c)
	snatch(t, g, other, creature, c)
	c.QueueConfirmEffect(true)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	card := g.Card(creature)
	if card.Zone != engine.Battlefield || card.ZoneOwner != other {
		t.Fatalf("creature in %v of player %d, want the thief's battlefield", card.Zone, card.ZoneOwner)
	}
	if card.Controller() != other || card.Owner != p {
		t.Errorf("controller %d owner %d, want thief %d and caster %d", card.Controller(), card.Owner, other, p)
	}
}

// TestControlSpellThiefResolvesTheSpellAsYou proves the stack item's
// controller is what "you" reads at resolution, and that the card goes back
// to its owner's control once it leaves the stack (GameAction.java:651-654).
func TestControlSpellThiefResolvesTheSpellAsYou(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	gain := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Hand)
	c := engine.NewScriptedController()
	castOn(t, g, p, gain, c)
	snatch(t, g, other, gain, c)
	c.QueueConfirmEffect(false)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if g.Player(p).Life != 20 || g.Player(other).Life != 23 {
		t.Errorf("life %d/%d, want 20/23: the thief gains", g.Player(p).Life, g.Player(other).Life)
	}
	card := g.Card(gain)
	if card.Zone != engine.Graveyard || card.ZoneOwner != p || card.Controller() != p {
		t.Errorf("gain in %v of %d controlled by %d, want its owner's graveyard under its owner", card.Zone, card.ZoneOwner, card.Controller())
	}
}

// TestControlSpellThenChooseNewTargets proves Aethersnatch's whole chain:
// the thief may retarget the stolen Bolt (at its caster), or decline and
// let it hit the original target.
func TestControlSpellThenChooseNewTargets(t *testing.T) {
	t.Parallel()
	for _, retarget := range []bool{true, false} {
		g, p, other := newTwoPlayerGame(t)
		bolt := boltAt(t, g, p)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
		castOn(t, g, p, bolt, c)
		snatch(t, g, other, bolt, c)
		c.QueueConfirmEffect(retarget)
		if retarget {
			c.QueueTargets([]engine.EntityID{engine.PlayerEntity(p)})
		}
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatal(err)
		}
		wantP, wantOther := 20, 17
		if retarget {
			wantP, wantOther = 17, 20
		}
		if g.Player(p).Life != wantP || g.Player(other).Life != wantOther {
			t.Errorf("retarget %v: life %d/%d, want %d/%d", retarget, g.Player(p).Life, g.Player(other).Life, wantP, wantOther)
		}
	}
}

// TestControlSpellNonCreatureRestrictionFiltersTheSpell proves Commandeer's
// ValidTgts$ Card.nonCreature: with only a creature spell on the stack
// there is no legal target, so the spell cannot be cast (CR 601.2c).
func TestControlSpellNonCreatureRestrictionFiltersTheSpell(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	creature := g.NewCard(creatureDefManaCost(t, "W"), p, engine.Hand)
	c := engine.NewScriptedController()
	castOn(t, g, p, creature, c)
	commandeer := g.NewCard(spellDefWith(t, "Commandeer", "Instant", "U",
		"SP$ ControlSpell | ValidTgts$ Card.nonCreature | TargetType$ Spell | Mode$ Gain"), other, engine.Hand)
	g.Player(other).ManaPool.Add(mana.Blue, 1)
	if g.CastSpell(other, commandeer, c) {
		t.Error("Commandeer cast with only a creature spell to target")
	}
}

// TestControlSpellMovesEveryCharmMode proves a stolen Charm's chosen modes
// resolve for the thief (setActivatingPlayer trickles into sub-instances),
// and that a clone taken before keeps the caster's modes.
func TestControlSpellMovesEveryCharmMode(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	charm := g.NewCard(spellDefWith(t, "Charm", "Instant", "W", "SP$ Charm | Choices$ DBGain,DBLose",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 2",
		"DBLose", "DB$ LoseLife | Defined$ You | LifeAmount$ 1"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueModeChoice([]int{0})
	castOn(t, g, p, charm, c)
	before := g.Clone()
	if _, err := resolveNow(t, g, other, c, []engine.EntityID{engine.CardEntity(charm)},
		"DB$ ControlSpell | ValidTgts$ Card | TargetType$ Spell | Mode$ Gain"); err != nil {
		t.Fatal(err)
	}
	if g.Player(p).Life != 20 || g.Player(other).Life != 22 {
		t.Errorf("life %d/%d, want 20/22: the thief resolves the mode", g.Player(p).Life, g.Player(other).Life)
	}
	top, ok := before.StackTop()
	if !ok || len(top.Modes) != 1 || top.Modes[0].Controller != p || top.Controller != p {
		t.Errorf("clone's Charm = %+v, want the caster's own item and mode", top)
	}
}

// TestControlSpellNewControllerAndRemember proves NewController$ names who
// gains the spell (Opponent, You), a targeted
// player without NewController$ is that player, Defined$ Targeted reads the
// ability's targets, and Remember$ remembers the spell on the host.
func TestControlSpellNewControllerAndRemember(t *testing.T) {
	t.Parallel()
	cases := []struct {
		line    string
		byOther bool // the ControlSpell ability is the opponent's
		player  bool // a player target joins the spell
	}{
		{"DB$ ControlSpell | ValidTgts$ Card | TargetType$ Spell | NewController$ Opponent | Mode$ Gain | Remember$ True", false, false},
		{"DB$ ControlSpell | ValidTgts$ Card | TargetType$ Spell | NewController$ You | Mode$ Gain | Remember$ True", true, false},
		{"DB$ ControlSpell | Defined$ Targeted | Mode$ Gain | Remember$ True", true, false},
		{"DB$ ControlSpell | ValidTgts$ Card,Player | Mode$ Gain | Remember$ True", false, true},
	}
	for _, tc := range cases {
		g, p, other := newTwoPlayerGame(t)
		gain := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Hand)
		c := engine.NewScriptedController()
		castOn(t, g, p, gain, c)
		actor := p
		if tc.byOther {
			actor = other
		}
		targets := []engine.EntityID{engine.CardEntity(gain)}
		if tc.player {
			targets = append(targets, engine.PlayerEntity(other))
		}
		host, err := resolveNow(t, g, actor, c, targets, tc.line)
		if err != nil {
			t.Fatalf("%s: %v", tc.line, err)
		}
		if g.Player(other).Life != 23 {
			t.Errorf("%s: opponent life %d, want 23", tc.line, g.Player(other).Life)
		}
		if rem := g.Card(host).Memory.Remembered(); !slices.Equal(rem, []engine.EntityID{engine.CardEntity(gain)}) {
			t.Errorf("%s: remembered %v, want the spell", tc.line, rem)
		}
	}
}

// exchangerDef is Perplexing Chimera's trigger and ControlSpell line alone,
// without the ConditionDefined$-gated ChangeTargets that fails closed.
func exchangerDef(t *testing.T) *compile.Card {
	t.Helper()
	return scriptDef(t, "Exchanger", "Enchantment Creature",
		"T:Mode$ SpellCast | ValidCard$ Card | ValidActivatingPlayer$ Opponent | OptionalDecider$ You | Execute$ ExchangeControlSpell | TriggerZones$ Battlefield",
		"SVar:ExchangeControlSpell:DB$ ControlSpell | Defined$ TriggeredSpellAbility | Mode$ Exchange | Remember$ True")
}

// TestControlSpellExchangeSwapsHostAndSpell proves Mode$ Exchange: the
// host goes to the spell's caster, the spell to the host's controller, who
// then resolves it, and Remember$ remembers the host, then the spell.
func TestControlSpellExchangeSwapsHostAndSpell(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	host := g.NewCard(exchangerDef(t), other, engine.Battlefield)
	gain := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueConfirmOptionalTrigger(true)
	castOn(t, g, p, gain, c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if got := g.Card(host).Controller(); got != p {
		t.Errorf("host controller %d, want the caster %d", got, p)
	}
	if g.Player(p).Life != 20 || g.Player(other).Life != 23 {
		t.Errorf("life %d/%d, want 20/23: the host's controller resolves the spell", g.Player(p).Life, g.Player(other).Life)
	}
	want := []engine.EntityID{engine.CardEntity(host), engine.CardEntity(gain)}
	if rem := g.Card(host).Memory.Remembered(); !slices.Equal(rem, want) {
		t.Errorf("remembered %v, want %v", rem, want)
	}
}

// TestControlSpellPerplexingChimeraRetargetFailsClosed proves the real
// card: the exchange happens, then its ChangeTargets, gated on
// ConditionDefined$ Remembered, fails closed rather than skipping.
func TestControlSpellPerplexingChimeraRetargetFailsClosed(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	chimera := g.NewCard(chimeraDef(t), other, engine.Battlefield)
	bolt := boltAt(t, g, p)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueConfirmOptionalTrigger(true)
	castOn(t, g, p, bolt, c)
	err := g.ResolveStack(engine.NewRegistry(), c)
	if err == nil || !strings.Contains(err.Error(), "ChangeTargets: ConditionDefined$ not resolvable yet") {
		t.Errorf("err = %v, want ChangeTargets' ConditionDefined$ error", err)
	}
	if got := g.Card(chimera).Controller(); got != p {
		t.Errorf("Chimera controller %d, want the Bolt's caster %d", got, p)
	}
}

// TestControlSpellExchangeSkipsASpellThatLeftTheStack proves
// ControlSpellEffect.java:74-77: countered before the exchanging trigger
// resolves, the spell has no stack instance, so nothing is exchanged or
// remembered.
func TestControlSpellExchangeSkipsASpellThatLeftTheStack(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	chimera := g.NewCard(exchangerDef(t), p, engine.Battlefield)
	g.NewCard(counterWatcherDef(t), other, engine.Battlefield)
	bolt := boltAt(t, g, other)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(p)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bolt)})
	c.QueueConfirmOptionalTrigger(true)
	castOn(t, g, other, bolt, c)
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if got := g.Card(chimera).Controller(); got != p {
		t.Errorf("Chimera controller %d, want still %d", got, p)
	}
	if g.Card(bolt).Zone != engine.Graveyard || g.Player(p).Life != 20 {
		t.Errorf("bolt in %v, life %d: want it countered", g.Card(bolt).Zone, g.Player(p).Life)
	}
	if rem := g.Card(chimera).Memory.Remembered(); len(rem) != 0 {
		t.Errorf("remembered %v, want nothing", rem)
	}
}

// TestControlSpellInvertPolarityWinsTheSpell proves the Defined$ Targeted
// shape through invert_polarity.txt: whichever call wins the flip (the
// game's RNG decides; both calls run on clones of one game) steals the
// targeted spell for the flipper; the losing call reaches Counter's
// Defined$ Targeted, which fails closed.
func TestControlSpellInvertPolarityWinsTheSpell(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	gain := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Hand)
	c := engine.NewScriptedController()
	castOn(t, g, p, gain, c)
	invert := g.NewCard(spellDefWith(t, "Invert Polarity", "Instant", "U",
		"SP$ FlipCoin | TargetType$ Spell | TgtZone$ Stack | WinSubAbility$ GainControl | LoseSubAbility$ CounterIt | ValidTgts$ Card",
		"GainControl", "DB$ ControlSpell | Defined$ Targeted | Mode$ Gain | SubAbility$ DBChooseTargets",
		"DBChooseTargets", "DB$ ChangeTargets | Defined$ Targeted | Optional$ True",
		"CounterIt", "DB$ Counter | Defined$ Targeted"), other, engine.Hand)
	g.Player(other).ManaPool.Add(mana.Blue, 1)
	c.QueueTargets([]engine.EntityID{engine.CardEntity(gain)})
	if !g.CastSpell(other, invert, c) {
		t.Fatal("CastSpell(Invert Polarity) failed")
	}
	wins := 0
	for _, heads := range []bool{true, false} {
		gg := g.Clone()
		cc := engine.NewScriptedController()
		cc.QueueCoinCall(heads)
		cc.QueueConfirmEffect(false)
		err := gg.ResolveStack(engine.NewRegistry(), cc)
		if err != nil {
			if !strings.Contains(err.Error(), "Counter: Defined$ not resolvable yet") {
				t.Errorf("heads %v: err = %v, want Counter's Defined$ error", heads, err)
			}
			continue
		}
		wins++
		if gg.Player(p).Life != 20 || gg.Player(other).Life != 23 {
			t.Errorf("heads %v: life %d/%d, want 20/23", heads, gg.Player(p).Life, gg.Player(other).Life)
		}
	}
	if wins != 1 {
		t.Errorf("won %d of the two calls, want exactly 1", wins)
	}
}

// TestControlSpellGainOnASpellThatLeftTheStackFails proves the Gain
// branch's missing stack instance -- a null dereference in Java
// (ControlSpellEffect.java:99) -- is an error rather than a silent skip.
func TestControlSpellGainOnASpellThatLeftTheStackFails(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	g.NewCard(triggerWatcherDef(t, "Thief",
		"Mode$ SpellCast | ValidCard$ Card | ValidActivatingPlayer$ Opponent | Execute$ TrigSteal",
		"TrigSteal", "DB$ ControlSpell | Defined$ TriggeredSpellAbility | Mode$ Gain"), p, engine.Battlefield)
	g.NewCard(counterWatcherDef(t), other, engine.Battlefield)
	bolt := boltAt(t, g, other)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(p)})
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bolt)})
	castOn(t, g, other, bolt, c)
	err := g.ResolveStack(engine.NewRegistry(), c)
	if err == nil || !strings.Contains(err.Error(), "no longer on the stack") {
		t.Errorf("err = %v, want the missing-stack-instance error", err)
	}
}

// TestControlSpellFailsClosed proves each shape ControlSpell does not
// resolve is an error before anything changes (PORT-8, GO-7).
func TestControlSpellFailsClosed(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct{ line, want string }{
		{"DB$ ControlSpell | ValidTgts$ Card | TargetType$ Spell | Mode$ Gain | RememberTargets$ True", "RememberTargets$ not resolvable yet"},
		{"DB$ ControlSpell | ValidTgts$ Card | TargetType$ Spell | Mode$ Gain | TargetValidTargeting$ Player", "TargetValidTargeting$ not resolvable yet"},
		{"DB$ ControlSpell | Defined$ Targeted | Mode$ Exchange | DefinedExchange$ Self", "DefinedExchange$ not resolvable yet"},
		{"DB$ ControlSpell | Defined$ Targeted", "Mode$ is required"},
		{"DB$ ControlSpell | Defined$ Targeted | Mode$ Lose", `Mode$ "Lose" not resolvable yet`},
		{"DB$ ControlSpell | Defined$ Remembered | Mode$ Gain", `Defined$ "Remembered" not resolvable yet`},
		{"DB$ ControlSpell | Defined$ TriggeredSpellAbility | Mode$ Gain", "no triggering spell recorded"},
		{"DB$ ControlSpell | Defined$ Targeted | NewController$ Player.withMostLife | Mode$ Gain", `NewController$: engine: Defined$ "Player.withMostLife"`},
		{"DB$ ControlSpell | Defined$ Targeted | Mode$ Gain | ConditionDefined$ Remembered | ConditionPresent$ Card", "ConditionDefined$ not resolvable yet"},
		{"DB$ ControlSpell | Defined$ Targeted | Mode$ Gain | Condition$ Kicked", "Condition$ not resolvable yet"},
	} {
		g, p, _ := newTwoPlayerGame(t)
		gain := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Hand)
		c := engine.NewScriptedController()
		castOn(t, g, p, gain, c)
		_, err := resolveNow(t, g, p, c, []engine.EntityID{engine.CardEntity(gain)}, tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want %q", tc.line, err, tc.want)
		}
	}
}

// TestControlSpellExchangeFailsClosedOnCantGainControl proves
// canBeControlledBy's static half is refused, not ignored.
func TestControlSpellExchangeFailsClosedOnCantGainControl(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	g.NewCard(chimeraDef(t), other, engine.Battlefield)
	g.NewCard(continuousDef(t, "Guard", "Mode$ CantGainControl | ValidCard$ Card.YouCtrl"), other, engine.Battlefield)
	bolt := boltAt(t, g, p)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueConfirmOptionalTrigger(true)
	castOn(t, g, p, bolt, c)
	err := g.ResolveStack(engine.NewRegistry(), c)
	if err == nil || !strings.Contains(err.Error(), "CantGainControl statics not resolvable yet") {
		t.Errorf("err = %v, want the CantGainControl error", err)
	}
}
