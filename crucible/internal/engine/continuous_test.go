package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// continuousDef builds a *compile.Card for a non-creature permanent carrying
// one real S:Mode$ Continuous line, compiled through the real pipeline the
// same reason cantBlockByAuraDef (staticability_test.go) is. applyContinuousPT
// is unexported, so every case here is driven through CheckStateBasedActions,
// its only caller (TEST-1).
// artifactDefT is Metalcraft's own test fixture: a bare artifact permanent,
// creatureDefPT's own struct-literal shape (action_test.go) rather than
// continuousDef's raw-text pipeline, since it carries no Statics of its own.
func artifactDefT(t *testing.T) *compile.Card {
	t.Helper()
	def := &compile.Card{Name: "Test Artifact"}
	def.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), "Artifact")
	return def
}

func continuousDef(t *testing.T, name, static string) *compile.Card {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(reg, "Enchantment")
	raw.Faces[0].Statics = []string{static}

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// TestApplyContinuousPTAppliesAnthemToMatchingCreatures proves the
// corpus-frequent anthem shape (Affected$ Creature.YouCtrl, a blanket
// valid-string match) actually boosts every matching creature's own
// Power/Toughness once CheckStateBasedActions recomputes it.
func TestApplyContinuousPTAppliesAnthemToMatchingCreatures(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 3 {
		t.Errorf("Power() = (%d, %v), want (3, true)", pw, ok)
	}
	if tg, ok := g.Card(creature).Toughness(); !ok || tg != 3 {
		t.Errorf("Toughness() = (%d, %v), want (3, true)", tg, ok)
	}
}

// TestApplyContinuousPTDoesNotAffectNonMatchingCreatures proves the
// Affected$ restriction is actually checked, not applied blanket to every
// creature in the game: an opponent's creature is untouched by a "creatures
// you control" anthem.
func TestApplyContinuousPTDoesNotAffectNonMatchingCreatures(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(theirs).Power(); !ok || pw != 2 {
		t.Errorf("Power() = (%d, %v), want (2, true) -- an opponent's anthem must not affect it", pw, ok)
	}
}

// TestApplyContinuousPTRecomputesWhenSourceLeaves proves continuous effects
// are recalculated fresh every pass, not pushed once and left to persist:
// once the anthem itself leaves the battlefield, the creature it used to
// boost reverts to its printed stats on the very next check.
func TestApplyContinuousPTRecomputesWhenSourceLeaves(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	anthem := g.NewCard(continuousDef(t, "Test Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if pw, _ := g.Card(creature).Power(); pw != 3 {
		t.Fatalf("setup: Power() = %d, want 3", pw)
	}

	g.Move(anthem, engine.Graveyard, p)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 2 {
		t.Errorf("Power() after the anthem left = (%d, %v), want (2, true)", pw, ok)
	}
}

// TestApplyContinuousPTSetPowerToughnessPartial proves a real corpus
// SetPower/SetToughness line naming only one dimension leaves the other
// exactly as it was -- PTEffect's own HasPower/HasToughness (pt.go), now
// exercised by a real caller for the first time.
func TestApplyContinuousPTSetPowerToughnessPartial(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test SetPower", "Mode$ Continuous | Affected$ Creature.YouCtrl | SetPower$ 0"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "4", "4"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 0 {
		t.Errorf("Power() = (%d, %v), want (0, true)", pw, ok)
	}
	if tg, ok := g.Card(creature).Toughness(); !ok || tg != 4 {
		t.Errorf("Toughness() = (%d, %v), want (4, true) -- SetPower alone must not touch Toughness", tg, ok)
	}
}

// TestApplyContinuousPTSkipsConditionParam proves a Condition$ value this
// port has no player-state for (MaxSpeed -- Alchemy's own speed counter,
// tracked nowhere in this port) is skipped entirely rather than treated as
// always active -- continuousConditionMet's own default case.
func TestApplyContinuousPTSkipsConditionParam(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Conditional Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Condition$ MaxSpeed"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 2 {
		t.Errorf("Power() = (%d, %v), want (2, true) -- Condition$ MaxSpeed cannot resolve, so this must not apply", pw, ok)
	}
}

// TestApplyContinuousPTAppliesWhenPlayerTurnConditionMet proves the
// resolvable Condition$ values now gate a Layer 7b/7c line rather than
// blanket-skip it: PlayerTurn (141 of the corpus's 317 real Mode$ Continuous
// | Condition$ lines, the single most common value) lets the anthem through
// once it is host's own controller's turn.
func TestApplyContinuousPTAppliesWhenPlayerTurnConditionMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Conditional Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Condition$ PlayerTurn"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.StartTurn(p, engine.NewScriptedController())

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 3 {
		t.Errorf("Power() = (%d, %v), want (3, true) -- Condition$ PlayerTurn is met, so the anthem must apply", pw, ok)
	}
}

// TestApplyContinuousPTSkipsWhenPlayerTurnConditionNotMet proves the other
// direction: the same anthem does not apply on the opponent's turn.
func TestApplyContinuousPTSkipsWhenPlayerTurnConditionNotMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Conditional Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Condition$ PlayerTurn"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.StartTurn(other, engine.NewScriptedController())

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 2 {
		t.Errorf("Power() = (%d, %v), want (2, true) -- Condition$ PlayerTurn is not met on the opponent's turn", pw, ok)
	}
}

// TestApplyContinuousPTAppliesWhenThresholdConditionMet proves Threshold (61
// real lines): seven or more cards in host's own controller's graveyard.
func TestApplyContinuousPTAppliesWhenThresholdConditionMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	for i := 0; i < 7; i++ {
		g.NewCard(nil, p, engine.Graveyard)
	}
	g.NewCard(continuousDef(t, "Test Threshold Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Condition$ Threshold"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 3 {
		t.Errorf("Power() = (%d, %v), want (3, true) -- seven graveyard cards meets Threshold", pw, ok)
	}
}

// TestApplyContinuousPTSkipsWhenThresholdConditionNotMet proves the other
// direction: six graveyard cards does not meet Threshold.
func TestApplyContinuousPTSkipsWhenThresholdConditionNotMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	for i := 0; i < 6; i++ {
		g.NewCard(nil, p, engine.Graveyard)
	}
	g.NewCard(continuousDef(t, "Test Threshold Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Condition$ Threshold"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 2 {
		t.Errorf("Power() = (%d, %v), want (2, true) -- six graveyard cards does not meet Threshold", pw, ok)
	}
}

// TestApplyContinuousPTAppliesWhenHellbentConditionMet proves Hellbent (8
// real lines): host's own controller has an empty hand.
func TestApplyContinuousPTAppliesWhenHellbentConditionMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Hellbent Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Condition$ Hellbent"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 3 {
		t.Errorf("Power() = (%d, %v), want (3, true) -- an empty hand meets Hellbent", pw, ok)
	}
}

// TestApplyContinuousPTSkipsWhenHellbentConditionNotMet proves the other
// direction: a nonempty hand does not meet Hellbent.
func TestApplyContinuousPTSkipsWhenHellbentConditionNotMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(nil, p, engine.Hand)
	g.NewCard(continuousDef(t, "Test Hellbent Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Condition$ Hellbent"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 2 {
		t.Errorf("Power() = (%d, %v), want (2, true) -- a nonempty hand does not meet Hellbent", pw, ok)
	}
}

// TestApplyContinuousPTAppliesWhenFatefulHourConditionMet proves FatefulHour
// (3 real lines): host's own controller is at 5 life or less.
func TestApplyContinuousPTAppliesWhenFatefulHourConditionMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 5, 20
	g.NewCard(continuousDef(t, "Test Fateful Hour Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Condition$ FatefulHour"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 3 {
		t.Errorf("Power() = (%d, %v), want (3, true) -- 5 life meets FatefulHour", pw, ok)
	}
}

// TestApplyContinuousPTSkipsWhenFatefulHourConditionNotMet proves the other
// direction: 6 life does not meet FatefulHour.
func TestApplyContinuousPTSkipsWhenFatefulHourConditionNotMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 6, 20
	g.NewCard(continuousDef(t, "Test Fateful Hour Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Condition$ FatefulHour"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 2 {
		t.Errorf("Power() = (%d, %v), want (2, true) -- 6 life does not meet FatefulHour", pw, ok)
	}
}

// TestApplyContinuousPTAppliesWhenMetalcraftConditionMet proves Metalcraft
// (18 real lines): three or more artifacts host's own controller controls.
func TestApplyContinuousPTAppliesWhenMetalcraftConditionMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	for i := 0; i < 3; i++ {
		g.NewCard(artifactDefT(t), p, engine.Battlefield)
	}
	g.NewCard(continuousDef(t, "Test Metalcraft Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Condition$ Metalcraft"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 3 {
		t.Errorf("Power() = (%d, %v), want (3, true) -- three artifacts meets Metalcraft", pw, ok)
	}
}

// TestApplyContinuousPTSkipsWhenMetalcraftConditionNotMet proves the other
// direction: two artifacts does not meet Metalcraft.
func TestApplyContinuousPTSkipsWhenMetalcraftConditionNotMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	for i := 0; i < 2; i++ {
		g.NewCard(artifactDefT(t), p, engine.Battlefield)
	}
	g.NewCard(continuousDef(t, "Test Metalcraft Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Condition$ Metalcraft"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 2 {
		t.Errorf("Power() = (%d, %v), want (2, true) -- two artifacts does not meet Metalcraft", pw, ok)
	}
}

// TestApplyContinuousPTAppliesWhenDeliriumConditionMet proves Delirium (23
// real lines): four or more distinct core types among cards in host's own
// controller's graveyard -- Creature, Instant, Sorcery and Land here.
func TestApplyContinuousPTAppliesWhenDeliriumConditionMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	for _, typeLine := range []string{"Creature", "Instant", "Sorcery", "Land"} {
		def := &compile.Card{Name: "Test Graveyard Card"}
		def.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), typeLine)
		g.NewCard(def, p, engine.Graveyard)
	}
	g.NewCard(continuousDef(t, "Test Delirium Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Condition$ Delirium"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 3 {
		t.Errorf("Power() = (%d, %v), want (3, true) -- four distinct graveyard core types meets Delirium", pw, ok)
	}
}

// TestApplyContinuousPTSkipsWhenDeliriumConditionNotMet proves the other
// direction: three distinct core types does not meet Delirium.
func TestApplyContinuousPTSkipsWhenDeliriumConditionNotMet(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	for _, typeLine := range []string{"Creature", "Instant", "Sorcery"} {
		def := &compile.Card{Name: "Test Graveyard Card"}
		def.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), typeLine)
		g.NewCard(def, p, engine.Graveyard)
	}
	g.NewCard(continuousDef(t, "Test Delirium Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Condition$ Delirium"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 2 {
		t.Errorf("Power() = (%d, %v), want (2, true) -- three distinct graveyard core types does not meet Delirium", pw, ok)
	}
}

// TestApplyContinuousPTSkipsNonNumericValue proves an SVar-driven AddPower$
// (X, Y, a named SVar) is skipped rather than resolved to zero or crashing.
func TestApplyContinuousPTSkipsNonNumericValue(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test X Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ X | AddToughness$ X"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 2 {
		t.Errorf("Power() = (%d, %v), want (2, true) -- a non-numeric AddPower$ must not apply", pw, ok)
	}
}

// continuousDefWithSVar is continuousDef plus one SVar the static line's own
// AddPower$/SetPower$/etc names -- resolveAmount's own real corpus shape
// (amount.go), compiled through the real pipeline so compile.Face.Amounts is
// actually built, not hand-constructed.
func continuousDefWithSVar(t *testing.T, name, static, svarName, svarBody string) *compile.Card {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(reg, "Enchantment")
	raw.Faces[0].Statics = []string{static}
	raw.Faces[0].SVars.Set(svarName, svarBody)

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// TestApplyContinuousPTResolvesNamedSVarCountValid proves resolveAmount
// (amount.go) closes the gap TestApplyContinuousPTSkipsNonNumericValue
// documents, for the one shape it actually can: AddPower$/AddToughness$
// naming an SVar whose own body is Count$Valid <spec> -- here, the number
// of Elves on the battlefield, which includes the anthem's own host (an
// Enchantment, not an Elf, so it does not count itself) and the one
// creature.
func TestApplyContinuousPTResolvesNamedSVarCountValid(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDefWithSVar(t, "Test X Anthem",
		"Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ X | AddToughness$ X",
		"X", "Count$Valid Elf"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 3 {
		t.Errorf("Power() = (%d, %v), want (3, true) -- AddPower$ X should resolve to 1, the one Elf on the battlefield", pw, ok)
	}
	if tg, ok := g.Card(creature).Toughness(); !ok || tg != 3 {
		t.Errorf("Toughness() = (%d, %v), want (3, true)", tg, ok)
	}
}

// TestApplyContinuousCharacteristicDefiningSetsFromCountValid proves Layer
// 7a: a CharacteristicDefining$ True line's own SetPower$/SetToughness$,
// naming an SVar whose body is Count$Valid <spec>, sets the host's own
// power/toughness to the number of matches -- reckless_one.txt's own real
// shape ("CARDNAME's power and toughness are each equal to the number of
// Goblins on the battlefield"), Elf standing in for Goblin here. Applies to
// the host itself only, not blanket to every Elf -- a second Elf on the
// battlefield is unaffected, proving Affected$ is not read for this shape
// (applyOneCharacteristicDefiningPT's own doc comment).
func TestApplyContinuousCharacteristicDefiningSetsFromCountValid(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: "Test CDA Elf"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Test CDA Elf"
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "0", "0"
	raw.Faces[0].Statics = []string{"Mode$ Continuous | CharacteristicDefining$ True | SetPower$ X | SetToughness$ X"}
	raw.Faces[0].SVars.Set("X", "Count$Valid Elf")
	def, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}
	cda := g.NewCard(def, p, engine.Battlefield)
	other2 := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(cda).Power(); !ok || pw != 2 {
		t.Errorf("Power() = (%d, %v), want (2, true) -- 2 Elves on the battlefield (CDA host + creatureDefPT)", pw, ok)
	}
	if tg, ok := g.Card(cda).Toughness(); !ok || tg != 2 {
		t.Errorf("Toughness() = (%d, %v), want (2, true)", tg, ok)
	}
	if pw, ok := g.Card(other2).Power(); !ok || pw != 1 {
		t.Errorf("other Elf's Power() = (%d, %v), want (1, true) -- CharacteristicDefining only ever describes its own host", pw, ok)
	}
}

// TestApplyContinuousPTResolvesChainedSVarReference proves resolveAmount's
// own Reference case: AddPower$ X, where X's own body is just "Y" (another
// SVar name, no Count$ of its own) and Y's is Count$Valid Elf -- one level
// of SVar-to-SVar indirection on top of the Expression case
// TestApplyContinuousPTResolvesNamedSVarCountValid already proves.
func TestApplyContinuousPTResolvesChainedSVarReference(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: "Test Chained SVar Anthem"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Test Chained SVar Anthem"
	raw.Faces[0].Type = cardtype.Parse(reg, "Enchantment")
	raw.Faces[0].Statics = []string{"Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ X | AddToughness$ X"}
	raw.Faces[0].SVars.Set("X", "Y")
	raw.Faces[0].SVars.Set("Y", "Count$Valid Elf")
	def, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}
	g.NewCard(def, p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 3 {
		t.Errorf("Power() = (%d, %v), want (3, true) -- AddPower$ X should chain X->Y->Count$Valid Elf to 1", pw, ok)
	}
}

// TestApplyContinuousPTResolvesCountWithOperator proves a Count$ expression
// carrying an operator suffix (/Plus.1) applies it after counting --
// doXMath, resolveAmount's own applyOperator: one Elf plus one is 2.
func TestApplyContinuousPTResolvesCountWithOperator(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDefWithSVar(t, "Test Operator Anthem",
		"Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ X | AddToughness$ X",
		"X", "Count$Valid Elf/Plus.1"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 4 {
		t.Errorf("Power() = (%d, %v), want (4, true) -- X = one Elf, Plus.1", pw, ok)
	}
}

// TestApplyContinuousPTSkipsUnresolvableOperand proves an operator whose own
// operand names an SVar the card does not define leaves the whole amount
// unresolved rather than treating the operand as 0 (Java's own stderr-and-
// zero fallback, which this port does not reproduce, GO-7).
func TestApplyContinuousPTSkipsUnresolvableOperand(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDefWithSVar(t, "Test Missing Operand Anthem",
		"Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ X | AddToughness$ X",
		"X", "Count$Valid Elf/Plus.Z"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 2 {
		t.Errorf("Power() = (%d, %v), want (2, true) -- an undefined operand SVar must not resolve", pw, ok)
	}
}

// TestApplyContinuousPTResolvesMultiZoneCount proves a Count$Valid<Zone1>,
// <Zone2> head (a real corpus shape, 40-some lines naming more than one
// zone) counts across every zone it names, not just the first: one matching
// card on the battlefield and one in the graveyard both count.
func TestApplyContinuousPTResolvesMultiZoneCount(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDefWithSVar(t, "Test Multi-Zone Anthem",
		"Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ X | AddToughness$ X",
		"X", "Count$ValidGraveyard,Battlefield Creature.YouOwn"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(creature).Power(); !ok || pw != 4 {
		t.Errorf("Power() = (%d, %v), want (4, true) -- AddPower$ X should count both the battlefield creature and the graveyard one", pw, ok)
	}
}

// TestApplyContinuousCharacteristicDefiningSkipsUnresolvableSVar proves a
// CharacteristicDefining$ line whose own SetPower$/SetToughness$ SVar is not
// a shape resolveAmount evaluates (Count$CardPower: another permanent's
// power is mid-rebuild while Layer 7 applies) is skipped entirely -- the host's own printed "*/*" base stays
// unresolvable, not silently zero.
func TestApplyContinuousCharacteristicDefiningSkipsUnresolvableSVar(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: "Test CDA Unresolvable"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Test CDA Unresolvable"
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "*", "*"
	raw.Faces[0].Statics = []string{"Mode$ Continuous | CharacteristicDefining$ True | SetPower$ X | SetToughness$ X"}
	raw.Faces[0].SVars.Set("X", "Count$CardPower")
	def, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}
	cda := g.NewCard(def, p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if _, ok := g.Card(cda).Power(); ok {
		t.Error("Power() resolved, want unresolvable -- Count$CardPower is not a head resolveAmount evaluates")
	}
}

// TestApplyContinuousCharacteristicDefiningSkipsDistinctPropertyCount proves
// a Valid family argument carrying a `$`-suffixed property this port does
// not measure (`$CreatureType`: distinct creature types, which needs the
// subtype vocabulary to tell a creature type from any other subtype) is
// skipped, not silently resolved as a plain match count against `Card` --
// expr.Count.DistinctProperty's own doc comment; a regression test for a
// real bug caught after the fact, kept for the properties still unported.
func TestApplyContinuousCharacteristicDefiningSkipsDistinctPropertyCount(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nLhurgoyf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: "Test Tarmogoyf"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Test Tarmogoyf"
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Lhurgoyf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "*", "*"
	raw.Faces[0].Statics = []string{"Mode$ Continuous | CharacteristicDefining$ True | SetPower$ X | SetToughness$ X"}
	raw.Faces[0].SVars.Set("X", "Count$ValidGraveyard Card$CreatureType")
	def, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile: %v", err)
	}
	goyf := g.NewCard(def, p, engine.Battlefield)
	g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Graveyard)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if pw, ok := g.Card(goyf).Power(); ok {
		t.Errorf("Power() = (%d, true), want unresolvable -- Card$CreatureType is a distinct-value count this port does not evaluate, and must not silently resolve to a plain match count against \"Card\"", pw)
	}
}

// TestApplyContinuousTypeAddsTypeToMatchingCreatures proves Layer 4's own
// AddType$, the type-line counterpart to TestApplyContinuousPTAppliesAnthemToMatchingCreatures:
// a blanket "creatures you control are also Zombies" effect adds Zombie
// without displacing the creature's own printed Elf.
func TestApplyContinuousTypeAddsTypeToMatchingCreatures(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Type Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddType$ Zombie"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	typ := g.Card(creature).Type()
	if !typ.HasSubtype("Zombie") {
		t.Errorf("Type() = %q, want it to carry the added Zombie subtype", typ)
	}
	if !typ.HasSubtype("Elf") {
		t.Errorf("Type() = %q, want it to still carry its own printed Elf subtype", typ)
	}
}

// TestApplyContinuousTypeDoesNotAffectNonMatchingCreatures mirrors
// TestApplyContinuousPTDoesNotAffectNonMatchingCreatures: an opponent's
// creature is untouched by a "creatures you control" type-granting anthem.
func TestApplyContinuousTypeDoesNotAffectNonMatchingCreatures(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Type Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddType$ Zombie"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if typ := g.Card(theirs).Type(); typ.HasSubtype("Zombie") {
		t.Errorf("Type() = %q, an opponent's anthem must not add Zombie to it", typ)
	}
}

// TestApplyContinuousTypeRecomputesWhenSourceLeaves mirrors
// TestApplyContinuousPTRecomputesWhenSourceLeaves: once the type-granting
// source itself leaves the battlefield, the added type is gone on the very
// next check.
func TestApplyContinuousTypeRecomputesWhenSourceLeaves(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	anthem := g.NewCard(continuousDef(t, "Test Type Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddType$ Zombie"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if typ := g.Card(creature).Type(); !typ.HasSubtype("Zombie") {
		t.Fatalf("setup: Type() = %q, want it to carry Zombie", typ)
	}

	g.Move(anthem, engine.Graveyard, p)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if typ := g.Card(creature).Type(); typ.HasSubtype("Zombie") {
		t.Errorf("Type() after the anthem left = %q, want Zombie gone", typ)
	}
}

// TestApplyContinuousTypeRemovesNamedType proves RemoveType$'s own plain
// literal-token shape: a real corpus "loses all creature types" line spelled
// as a single named RemoveType$ (not the bulk RemoveCreatureTypes$ flag)
// clears exactly that subtype and nothing else.
func TestApplyContinuousTypeRemovesNamedType(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Type Remover", "Mode$ Continuous | Affected$ Creature.YouCtrl | RemoveType$ Elf"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	typ := g.Card(creature).Type()
	if typ.HasSubtype("Elf") {
		t.Errorf("Type() = %q, want Elf removed", typ)
	}
	if !typ.Has(cardtype.Creature) {
		t.Errorf("Type() = %q, want the Creature core type left untouched", typ)
	}
}

// TestApplyContinuousTypeSkipsDynamicValue proves an AddType$ token this
// port cannot resolve at runtime (ChosenType, a chosen-type reference) skips
// the whole line rather than adding a literal subtype named "ChosenType".
func TestApplyContinuousTypeSkipsDynamicValue(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Chosen Type Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddType$ ChosenType"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if typ := g.Card(creature).Type(); typ.HasSubtype("ChosenType") {
		t.Errorf("Type() = %q, an unresolvable ChosenType token must not be added as a literal subtype", typ)
	}
}

// TestApplyContinuousTypeSkipsBulkRemovalFlag proves a line pairing AddType$
// with a bulk RemoveCreatureTypes$ flag (the real "becomes a Turtle" shape,
// StaticAbilityContinuous.java:425-448) is skipped whole: applying AddType$
// alone, without the wipe RemoveCreatureTypes$ asks for, would leave the
// creature with both its old and new creature types -- an actively wrong
// answer this port refuses to give rather than shipping half of a line.
func TestApplyContinuousTypeSkipsBulkRemovalFlag(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Turtle Aura", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddType$ Turtle | RemoveCreatureTypes$ True"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	typ := g.Card(creature).Type()
	if typ.HasSubtype("Turtle") {
		t.Errorf("Type() = %q, want the whole line skipped (RemoveCreatureTypes$ is not evaluated), not just partially applied", typ)
	}
	if !typ.HasSubtype("Elf") {
		t.Errorf("Type() = %q, want the creature's own printed Elf left untouched by the skipped line", typ)
	}
}

// TestApplyContinuousColorAddsColorToMatchingCreatures proves Layer 5's own
// AddColor$: a blanket "creatures you control are also blue" effect unions
// Blue in without displacing the creature's own printed Red.
func TestApplyContinuousColorAddsColorToMatchingCreatures(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Color Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddColor$ Blue"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefManaCost(t, "R"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	colors := g.Card(creature).Colors()
	if !colors.Has(mana.Blue) {
		t.Errorf("Colors() = %v, want it to carry the added Blue", colors)
	}
	if !colors.Has(mana.Red) {
		t.Errorf("Colors() = %v, want it to still carry its own printed Red", colors)
	}
}

// TestApplyContinuousColorSetColorOverwrites proves SetColor$'s own
// "replace outright" semantics (Java's overwriteColors): the creature's own
// printed Red is gone, not merely joined by Black.
func TestApplyContinuousColorSetColorOverwrites(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Color Setter", "Mode$ Continuous | Affected$ Creature.YouCtrl | SetColor$ Black"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefManaCost(t, "R"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	colors := g.Card(creature).Colors()
	if !colors.Has(mana.Black) {
		t.Errorf("Colors() = %v, want Black", colors)
	}
	if colors.Has(mana.Red) {
		t.Errorf("Colors() = %v, want the printed Red replaced, not joined", colors)
	}
}

// TestApplyContinuousColorDoesNotAffectNonMatchingCreatures mirrors the
// Layer 7/Layer 4 non-matching tests: an opponent's creature is untouched.
func TestApplyContinuousColorDoesNotAffectNonMatchingCreatures(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Color Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddColor$ Blue"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefManaCost(t, "R"), other, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if colors := g.Card(theirs).Colors(); colors.Has(mana.Blue) {
		t.Errorf("Colors() = %v, an opponent's anthem must not add Blue to it", colors)
	}
}

// TestApplyContinuousColorRecomputesWhenSourceLeaves mirrors the Layer
// 7/Layer 4 leave tests: once the color-granting source itself leaves the
// battlefield, the added color is gone on the very next check.
func TestApplyContinuousColorRecomputesWhenSourceLeaves(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	anthem := g.NewCard(continuousDef(t, "Test Color Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddColor$ Blue"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefManaCost(t, "R"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if colors := g.Card(creature).Colors(); !colors.Has(mana.Blue) {
		t.Fatalf("setup: Colors() = %v, want it to carry Blue", colors)
	}

	g.Move(anthem, engine.Graveyard, p)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if colors := g.Card(creature).Colors(); colors.Has(mana.Blue) {
		t.Errorf("Colors() after the anthem left = %v, want Blue gone", colors)
	}
}

// TestApplyContinuousColorSetColorAll proves the fixed "All" token Java's
// own getColorsFromParam special-cases: SetColor$ All resolves to every
// color (mana.AllColors), not a literal color named "All".
func TestApplyContinuousColorSetColorAll(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test All Colors", "Mode$ Continuous | Affected$ Creature.YouCtrl | SetColor$ All"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefManaCost(t, "R"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if got := g.Card(creature).Colors(); got != mana.AllColors {
		t.Errorf("Colors() = %v, want mana.AllColors", got)
	}
}

// TestApplyContinuousColorSetColorColorless proves the other fixed token:
// SetColor$ Colorless resolves to no color at all, replacing the creature's
// own printed Red rather than leaving it untouched.
func TestApplyContinuousColorSetColorColorless(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Colorless", "Mode$ Continuous | Affected$ Creature.YouCtrl | SetColor$ Colorless"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefManaCost(t, "R"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if got := g.Card(creature).Colors(); got != 0 {
		t.Errorf("Colors() = %v, want colorless (0)", got)
	}
}

// TestApplyContinuousColorSkipsChosenColor proves an AddColor$/SetColor$
// token this port cannot resolve at runtime (ChosenColor) skips the whole
// line rather than crashing or resolving to no color.
func TestApplyContinuousColorSkipsChosenColor(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Chosen Color Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddColor$ ChosenColor"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefManaCost(t, "R"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if colors := g.Card(creature).Colors(); colors != mana.Red {
		t.Errorf("Colors() = %v, want unchanged Red -- an unresolvable ChosenColor token must not apply", colors)
	}
}

// TestApplyContinuousKeywordGrantsKeywordToMatchingCreatures proves Layer
// 6's own AddKeyword$: a blanket "creatures you control have flying" effect
// grants it, read back through HasKeyword the same way a printed keyword
// already is.
func TestApplyContinuousKeywordGrantsKeywordToMatchingCreatures(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Keyword Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddKeyword$ Flying"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if !g.Card(creature).HasKeyword("Flying") {
		t.Error("HasKeyword(\"Flying\") = false, want true -- the anthem's own AddKeyword$ should have granted it")
	}
}

// TestApplyContinuousKeywordGrantsBothTokens proves the " & "-separated
// multi-keyword shape (5 of 256 real AddKeyword$ lines carry more than one
// core-type token for AddType$; AddKeyword$'s own corpus carries the
// identical separator, "Flying & Haste" among the real samples) grants
// every token, not just the first.
func TestApplyContinuousKeywordGrantsBothTokens(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Keyword Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddKeyword$ Flying & Haste"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	c := g.Card(creature)
	if !c.HasKeyword("Flying") || !c.HasKeyword("Haste") {
		t.Errorf("HasKeyword: Flying = %v, Haste = %v, want true and true", c.HasKeyword("Flying"), c.HasKeyword("Haste"))
	}
}

// TestApplyContinuousKeywordDoesNotAffectNonMatchingCreatures mirrors the
// Layer 7/4/5 non-matching tests: an opponent's creature is untouched by a
// "creatures you control" keyword-granting anthem.
func TestApplyContinuousKeywordDoesNotAffectNonMatchingCreatures(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Keyword Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddKeyword$ Flying"), p, engine.Battlefield)
	theirs := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if g.Card(theirs).HasKeyword("Flying") {
		t.Error("HasKeyword(\"Flying\") = true, an opponent's anthem must not grant it")
	}
}

// TestApplyContinuousKeywordRecomputesWhenSourceLeaves mirrors the Layer
// 7/4/5 leave tests: once the keyword-granting source itself leaves the
// battlefield, the granted keyword is gone on the very next check.
func TestApplyContinuousKeywordRecomputesWhenSourceLeaves(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	anthem := g.NewCard(continuousDef(t, "Test Keyword Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddKeyword$ Flying"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if !g.Card(creature).HasKeyword("Flying") {
		t.Fatal("setup: HasKeyword(\"Flying\") = false, want true")
	}

	g.Move(anthem, engine.Graveyard, p)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if g.Card(creature).HasKeyword("Flying") {
		t.Error("HasKeyword(\"Flying\") after the anthem left = true, want false")
	}
}

// TestApplyContinuousKeywordSkipsDynamicValue proves an AddKeyword$ token
// this port cannot resolve at runtime (a ChosenColor-qualified Protection
// grant) skips the whole line rather than granting a literal, wrong keyword.
func TestApplyContinuousKeywordSkipsDynamicValue(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Chosen Protection", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddKeyword$ Protection:Card.ChosenColor:chosenColor"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if g.Card(creature).HasKeyword("Protection") {
		t.Error("HasKeyword(\"Protection\") = true, an unresolvable ChosenColor token must not apply")
	}
}

// TestApplyContinuousKeywordSkipsRemoveKeywordCombo proves a line pairing
// AddKeyword$ with RemoveKeyword$ (a real "gains X, loses Y" shape) is
// skipped whole: applying AddKeyword$ alone would leave the creature with
// both the old and new keyword, an actively wrong answer this port refuses
// to give rather than shipping half of a line.
func TestApplyContinuousKeywordSkipsRemoveKeywordCombo(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Swap Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddKeyword$ Reach | RemoveKeyword$ Flying"), p, engine.Battlefield)
	creature := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Flying"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	c := g.Card(creature)
	if c.HasKeyword("Reach") {
		t.Error("HasKeyword(\"Reach\") = true, want the whole line skipped (RemoveKeyword$ is not evaluated), not just partially applied")
	}
	if !c.HasKeyword("Flying") {
		t.Error("HasKeyword(\"Flying\") = false, want the creature's own printed Flying left untouched by the skipped line")
	}
}

// TestApplyContinuousKeywordGrantedFlyingAffectsCanBlock proves the fold
// reaches further than HasKeyword alone: a continuously-granted Flying
// makes a creature unblockable by a grounded creature the identical way a
// printed Flying keyword already does (cantBlockByKeywords, staticability.go)
// -- Layer 6's own first real caller reaching all the way into block
// legality, not just a bare HasKeyword check.
func TestApplyContinuousKeywordGrantedFlyingAffectsCanBlock(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.Player(a).Life, g.Player(b).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Flying Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddKeyword$ Flying"), a, engine.Battlefield)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	ground := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if g.CanBlock(attacker, ground) {
		t.Error("CanBlock(continuously-flying attacker, grounded blocker) = true, want false")
	}
}

// TestApplyContinuousRulesSetsUnlimitedHandSize proves Layer 8's own
// SetMaxHandSize$ Unlimited: Thought Vessel's own real shape.
func TestApplyContinuousRulesSetsUnlimitedHandSize(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Unlimited Hand", "Mode$ Continuous | Affected$ You | SetMaxHandSize$ Unlimited"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if limit, hasLimit := g.Player(p).HandSizeLimit(engine.MaxHandSize); hasLimit {
		t.Errorf("HandSizeLimit() = (%d, true), want (_, false) -- SetMaxHandSize$ Unlimited grants no maximum", limit)
	}
}

// TestApplyContinuousRulesSetsFixedHandSize proves the plain-integer case:
// SetMaxHandSize$ 10 replaces the printed default of 7 outright.
func TestApplyContinuousRulesSetsFixedHandSize(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Fixed Hand", "Mode$ Continuous | Affected$ You | SetMaxHandSize$ 10"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if limit, hasLimit := g.Player(p).HandSizeLimit(engine.MaxHandSize); !hasLimit || limit != 10 {
		t.Errorf("HandSizeLimit() = (%d, %v), want (10, true)", limit, hasLimit)
	}
}

// TestApplyContinuousRulesRaisesHandSize proves RaiseMaxHandSize$ ADDS to
// the running limit rather than replacing it.
func TestApplyContinuousRulesRaisesHandSize(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Raised Hand", "Mode$ Continuous | Affected$ You | RaiseMaxHandSize$ 2"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if limit, hasLimit := g.Player(p).HandSizeLimit(engine.MaxHandSize); !hasLimit || limit != 9 {
		t.Errorf("HandSizeLimit() = (%d, %v), want (9, true) -- 7 plus RaiseMaxHandSize$ 2", limit, hasLimit)
	}
}

// TestApplyContinuousRulesAdjustsLandPlays proves AdjustLandPlays$ adds to
// the printed default of one land per turn.
func TestApplyContinuousRulesAdjustsLandPlays(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Extra Land", "Mode$ Continuous | Affected$ You | AdjustLandPlays$ 1"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if limit, unlimited := g.Player(p).LandPlayLimit(1); unlimited || limit != 2 {
		t.Errorf("LandPlayLimit() = (%d, %v), want (2, false) -- 1 plus AdjustLandPlays$ 1", limit, unlimited)
	}
}

// TestApplyContinuousRulesGrantsUnlimitedLandPlays proves AdjustLandPlays$
// Unlimited (Exploration-adjacent shapes' own dominant "Unlimited" form).
func TestApplyContinuousRulesGrantsUnlimitedLandPlays(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Unlimited Lands", "Mode$ Continuous | Affected$ You | AdjustLandPlays$ Unlimited"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if _, unlimited := g.Player(p).LandPlayLimit(1); !unlimited {
		t.Error("LandPlayLimit() unlimited = false, want true -- AdjustLandPlays$ Unlimited")
	}
}

// TestApplyContinuousRulesAffectedOpponentSkipsTheHostsOwnController proves
// Affected$ Opponent (matchesPlayerSpec, valid.go) applies to the OTHER
// player, not the host's own controller -- a "each opponent's maximum hand
// size is reduced" shape.
func TestApplyContinuousRulesAffectedOpponentSkipsTheHostsOwnController(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Opponent Hand", "Mode$ Continuous | Affected$ Opponent | SetMaxHandSize$ 3"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if limit, hasLimit := g.Player(other).HandSizeLimit(engine.MaxHandSize); !hasLimit || limit != 3 {
		t.Errorf("other's HandSizeLimit() = (%d, %v), want (3, true)", limit, hasLimit)
	}
	if limit, hasLimit := g.Player(p).HandSizeLimit(engine.MaxHandSize); !hasLimit || limit != engine.MaxHandSize {
		t.Errorf("host's own HandSizeLimit() = (%d, %v), want (%d, true) -- Affected$ Opponent must not affect the host's own controller", limit, hasLimit, engine.MaxHandSize)
	}
}

// TestApplyContinuousRulesSkipsLineWithCondition proves a Condition$ value
// this port has no player-state for (MaxSpeed) gates a SetMaxHandSize$ line
// the same way it gates PT (continuousConditionMet's own default case,
// shared by every applier).
func TestApplyContinuousRulesSkipsLineWithCondition(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	g.NewCard(continuousDef(t, "Test Conditional Hand", "Mode$ Continuous | Condition$ MaxSpeed | Affected$ Opponent | SetMaxHandSize$ 3"), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if limit, hasLimit := g.Player(other).HandSizeLimit(engine.MaxHandSize); !hasLimit || limit != engine.MaxHandSize {
		t.Errorf("HandSizeLimit() = (%d, %v), want (%d, true) -- Condition$ MaxSpeed cannot resolve, so the line must not apply", limit, hasLimit, engine.MaxHandSize)
	}
}

// TestApplyContinuousControlGrantsControlOfEnchantedCreature proves the
// corpus's own dominant real shape, Control Magic's own line: an Aura-like
// permanent (host) enchants a creature and hands its own controller control
// of it -- Affected$ Card.EnchantedBy | GainControl$ You (43 of the
// corpus's 44 real S:Mode$ Continuous lines naming GainControl$).
func TestApplyContinuousControlGrantsControlOfEnchantedCreature(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.Player(a).Life, g.Player(b).Life = 20, 20
	creature := g.NewCard(creatureDef(t), b, engine.Battlefield)
	host := g.NewCard(continuousDef(t, "Test Control Magic", "Mode$ Continuous | Affected$ Card.EnchantedBy | GainControl$ You"), a, engine.Battlefield)
	g.Attach(host, creature)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if got := g.Card(creature).Controller(); got != a {
		t.Errorf("Controller() = %v, want %v (host's own controller, via GainControl$ You)", got, a)
	}
	if got := g.Card(creature).Owner; got != b {
		t.Errorf("Owner = %v, want %v -- Owner must not change control", got, b)
	}
}

// TestApplyContinuousControlLeavesUnenchantedCreaturesAlone proves
// Affected$ Card.EnchantedBy only reaches the one creature host actually
// enchants, not every creature on the battlefield.
func TestApplyContinuousControlLeavesUnenchantedCreaturesAlone(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.Player(a).Life, g.Player(b).Life = 20, 20
	enchanted := g.NewCard(creatureDef(t), b, engine.Battlefield)
	bystander := g.NewCard(creatureDef(t), b, engine.Battlefield)
	host := g.NewCard(continuousDef(t, "Test Control Magic", "Mode$ Continuous | Affected$ Card.EnchantedBy | GainControl$ You"), a, engine.Battlefield)
	g.Attach(host, enchanted)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if got := g.Card(bystander).Controller(); got != b {
		t.Errorf("bystander Controller() = %v, want %v -- unenchanted, must keep its own controller", got, b)
	}
}

// TestApplyContinuousControlSkipsUnresolvedGainControlValue proves a
// qualified GainControl$ value (Player.isMonarch, 1 of the corpus's 44 real
// lines) is skipped rather than guessed at -- this port has no monarch
// mechanic to resolve it against (PORT-8/GO-7).
func TestApplyContinuousControlSkipsUnresolvedGainControlValue(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.Player(a).Life, g.Player(b).Life = 20, 20
	creature := g.NewCard(creatureDef(t), b, engine.Battlefield)
	host := g.NewCard(continuousDef(t, "Test Monarch Steal", "Mode$ Continuous | Affected$ Card.EnchantedBy | GainControl$ Player.isMonarch"), a, engine.Battlefield)
	g.Attach(host, creature)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if got := g.Card(creature).Controller(); got != b {
		t.Errorf("Controller() = %v, want %v -- GainControl$ Player.isMonarch cannot resolve, so control must not change", got, b)
	}
}

// TestApplyContinuousControlSkipsLineWithCondition proves a Condition$ value
// this port has no player-state for (MaxSpeed) gates a GainControl$ line the
// same way it gates PT -- continuousConditionMet's own default case, shared
// by every applier, ported here for Layer 2.
func TestApplyContinuousControlSkipsLineWithCondition(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.Player(a).Life, g.Player(b).Life = 20, 20
	creature := g.NewCard(creatureDef(t), b, engine.Battlefield)
	host := g.NewCard(continuousDef(t, "Test Conditional Steal", "Mode$ Continuous | Condition$ MaxSpeed | Affected$ Card.EnchantedBy | GainControl$ You"), a, engine.Battlefield)
	g.Attach(host, creature)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if got := g.Card(creature).Controller(); got != b {
		t.Errorf("Controller() = %v, want %v -- Condition$ MaxSpeed cannot resolve, so the line must not apply", got, b)
	}
}

// TestApplyContinuousControlRunsBeforeKeywordSoYouCtrlSeesTheNewController
// proves Layer 2 applies before Layer 6: an anthem-style keyword grant whose
// own Affected$ reads Creature.YouCtrl must see THIS pass's new controller,
// not the previous pass's -- applyContinuousControl's own doc comment
// (continuous.go) has the CR 613.1 ordering reason.
func TestApplyContinuousControlRunsBeforeKeywordSoYouCtrlSeesTheNewController(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.Player(a).Life, g.Player(b).Life = 20, 20
	creature := g.NewCard(creatureDef(t), b, engine.Battlefield)
	host := g.NewCard(continuousDef(t, "Test Control Magic", "Mode$ Continuous | Affected$ Card.EnchantedBy | GainControl$ You"), a, engine.Battlefield)
	g.Attach(host, creature)
	g.NewCard(continuousDef(t, "Test Anthem", "Mode$ Continuous | Affected$ Creature.YouCtrl | AddKeyword$ Flying"), a, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if !g.Card(creature).HasKeyword("Flying") {
		t.Error("HasKeyword(Flying) = false, want true -- the anthem's own Creature.YouCtrl must see this pass's new controller (a), not the stale one (b)")
	}
}

// equipmentDefWithStatic is continuousDef's own Equipment sibling -- Spy
// Kit's own real shape needs the Equipment type (an Enchantment can never be
// equipped) alongside a static line, compiled through the real pipeline so
// s.Param reads back exactly what a real corpus card would parse to.
func equipmentDefWithStatic(t *testing.T, name, static string) *compile.Card {
	t.Helper()

	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatalf("LoadRegistry: %v", err)
	}
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(reg, "Artifact Equipment")
	raw.Faces[0].Statics = []string{static}

	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// spyKitStatic is spy_kit.txt's own real S: line, minus the AddPower$/
// AddToughness$ half applyContinuousNames does not read (a separate,
// pre-existing gap: applyOneContinuousPT refuses on AffectedDefined$
// outright, so Spy Kit's own +1/+1 does not resolve today either -- out of
// scope for this test, which is only about the legend-rule flag).
const spyKitStatic = "Mode$ Continuous | AffectedDefined$ Equipped | Affected$ Creature | AddNames$ AllNonLegendaryCreatureNames"

// TestApplyContinuousNamesGrantsFlagToEquippedCreature proves
// applyContinuousNames (continuous.go) resolves Spy Kit's own real line:
// AffectedDefined$ Equipped reads host's own AttachedTo() directly, and the
// equipped creature ends this pass with HasNonLegendaryCreatureNames true.
func TestApplyContinuousNamesGrantsFlagToEquippedCreature(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	creature := g.NewCard(creatureDef(t), p, engine.Battlefield)
	host := g.NewCard(equipmentDefWithStatic(t, "Test Spy Kit", spyKitStatic), p, engine.Battlefield)
	g.Attach(host, creature)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if !g.Card(creature).HasNonLegendaryCreatureNames {
		t.Error("HasNonLegendaryCreatureNames = false, want true -- Spy Kit's own AddNames$ must reach the creature it equips")
	}
}

// TestApplyContinuousNamesDoesNotGrantFlagWhenUnattached is the regression
// half: Spy Kit sitting on the battlefield unattached grants nothing to
// anyone -- AttachedTo() reports false, so the line skips entirely.
func TestApplyContinuousNamesDoesNotGrantFlagWhenUnattached(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	creature := g.NewCard(creatureDef(t), p, engine.Battlefield)
	g.NewCard(equipmentDefWithStatic(t, "Test Spy Kit", spyKitStatic), p, engine.Battlefield)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if g.Card(creature).HasNonLegendaryCreatureNames {
		t.Error("HasNonLegendaryCreatureNames = true, want false -- an unattached Spy Kit grants nothing")
	}
}

// TestApplyContinuousNamesClearsFlagOnceUnattached proves the flag is
// recomputed fresh every pass, not stuck once granted: unattaching Spy Kit
// and rechecking must clear it, the identical "no timestamp fold, no
// leftover state" contract applyContinuousPT's own doc comment already
// gives every other layer.
func TestApplyContinuousNamesClearsFlagOnceUnattached(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	creature := g.NewCard(creatureDef(t), p, engine.Battlefield)
	host := g.NewCard(equipmentDefWithStatic(t, "Test Spy Kit", spyKitStatic), p, engine.Battlefield)
	g.Attach(host, creature)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())
	if !g.Card(creature).HasNonLegendaryCreatureNames {
		t.Fatal("setup: flag should be true while attached")
	}

	g.Unattach(host)
	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if g.Card(creature).HasNonLegendaryCreatureNames {
		t.Error("HasNonLegendaryCreatureNames = true, want false -- unattaching Spy Kit must clear the flag on the next pass")
	}
}

// TestApplyContinuousNamesIgnoresUnrecognizedAddNamesValue proves
// AllNonLegendaryCreatureNames is the only AddNames$ value this applier
// resolves (the only one the corpus carries at all): a different value skips
// the whole line rather than guessing (GO-7).
func TestApplyContinuousNamesIgnoresUnrecognizedAddNamesValue(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	p, other := g.Players()[0], g.Players()[1]
	g.Player(p).Life, g.Player(other).Life = 20, 20
	creature := g.NewCard(creatureDef(t), p, engine.Battlefield)
	host := g.NewCard(equipmentDefWithStatic(t, "Test Odd Namer",
		"Mode$ Continuous | AffectedDefined$ Equipped | Affected$ Creature | AddNames$ ChosenName"), p, engine.Battlefield)
	g.Attach(host, creature)

	engine.CheckStateBasedActions(g, engine.NewScriptedController())

	if g.Card(creature).HasNonLegendaryCreatureNames {
		t.Error("HasNonLegendaryCreatureNames = true, want false -- AddNames$ ChosenName is not the resolved value")
	}
}
