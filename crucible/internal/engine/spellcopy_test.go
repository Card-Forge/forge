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

// spellDefWith is an instant or sorcery (typeLine) carrying one A:SP$ line
// and the SVars it names.
func spellDefWith(t *testing.T, name, typeLine, cost, sp string, svars ...string) *compile.Card {
	t.Helper()
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), typeLine)
	raw.Faces[0].ManaCost = mana.MustParse(cost)
	raw.Faces[0].Abilities = []string{sp}
	for i := 0; i+1 < len(svars); i += 2 {
		raw.Faces[0].SVars.Set(svars[i], svars[i+1])
	}
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatalf("compile %q: %v", name, err)
	}
	return c
}

// copyWatcher is an enchantment whose "whenever you cast a <validCard>
// spell" trigger resolves copyLine.
func copyWatcher(t *testing.T, validCard, copyLine string) *compile.Card {
	t.Helper()
	return triggerWatcherDef(t, "Copy Watcher",
		"Mode$ SpellCast | ValidCard$ "+validCard+" | ValidActivatingPlayer$ You | Execute$ TrigCopy",
		"TrigCopy", copyLine)
}

// castWithCopyWatcher seats a copyWatcher for p, casts card from p's hand
// with one W, and resolves the stack.
func castWithCopyWatcher(t *testing.T, g *engine.Game, p engine.PlayerID, card engine.CardID, c engine.PlayerController, validCard, copyLine string) error {
	t.Helper()
	g.NewCard(copyWatcher(t, validCard, copyLine), p, engine.Battlefield)
	g.Player(p).ManaPool.Add(mana.White, 1)
	if !g.CastSpell(p, card, c) {
		t.Fatal("CastSpell failed")
	}
	return g.ResolveStack(engine.NewRegistry(), c)
}

// TestCopySpellCopiesTheTriggeringSpell proves the dominant shape, Defined$
// TriggeredSpellAbility under a Mode$ SpellCast trigger: the copy is a
// second stack object that resolves first, is not cast, and ceases to exist
// instead of going to a graveyard (CR 707.10a); the original resolves after
// it and goes to the graveyard as usual.
func TestCopySpellCopiesTheTriggeringSpell(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	spell := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Hand)
	var sink recordingSink
	g.SetSink(&sink)
	c := engine.NewScriptedController()
	if err := castWithCopyWatcher(t, g, p, spell, c, "Instant", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility"); err != nil {
		t.Fatal(err)
	}
	if got := g.Player(p).Life; got != 26 {
		t.Errorf("life = %d, want 26: original and copy each gain 3", got)
	}
	if got := g.Player(p).SpellsCastThisTurn; got != 1 {
		t.Errorf("SpellsCastThisTurn = %d, want 1: a copy is not cast", got)
	}
	if got := g.Zone(engine.Graveyard, p).Cards(); len(got) != 1 || got[0] != spell {
		t.Errorf("graveyard = %v, want only the original", got)
	}
	copies := g.Zone(engine.None, p).Cards()
	if len(copies) != 1 || !g.Card(copies[0]).IsCopiedSpell || g.Card(copies[0]).Def != g.Card(spell).Def {
		t.Fatalf("None zone = %v, want the ceased copy of the spell", copies)
	}
	casts := 0
	for _, e := range sink.events {
		if e.Kind == engine.SpellCast {
			casts++
		}
		if e.Kind == engine.ZoneChanged && e.Source == copies[0] {
			t.Errorf("copy emitted %+v, want no zone change as it ceases", e)
		}
	}
	if casts != 1 {
		t.Errorf("SpellCast events = %d, want 1", casts)
	}
}

// TestCopySpellMayChooseNewTargets proves CR 707.10c through two existing
// decisions: ConfirmEffect("new targets?") false keeps the original's
// target, true then ChooseTargets picks a new one from the same scan.
func TestCopySpellMayChooseNewTargets(t *testing.T) {
	t.Parallel()
	for _, retarget := range []bool{false, true} {
		g, p, other := newTwoPlayerGame(t)
		bolt := g.NewCard(instantDefWithAbility(t, "Bolt", "W", "SP$ DealDamage | ValidTgts$ Player | NumDmg$ 3"), p, engine.Hand)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
		c.QueueConfirmEffect(retarget)
		if retarget {
			c.QueueTargets([]engine.EntityID{engine.PlayerEntity(p)})
		}
		if err := castWithCopyWatcher(t, g, p, bolt, c, "Instant", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | MayChooseTarget$ True"); err != nil {
			t.Fatal(err)
		}
		wantMine, wantTheirs := 20, 14
		if retarget {
			wantMine, wantTheirs = 17, 17
		}
		if g.Player(p).Life != wantMine || g.Player(other).Life != wantTheirs {
			t.Errorf("retarget=%v: life %d/%d, want %d/%d", retarget, g.Player(p).Life, g.Player(other).Life, wantMine, wantTheirs)
		}
	}
}

// TestCopySpellCopiesGoOnTheStackBeforeTheirTargetTriggers proves every
// copy of one resolution is pushed before any BecomesTarget trigger its
// targets raise (MagicStack.add hands those to the trigger handler, which
// puts them on the stack when a player next receives priority): under
// Amount$ 2 both watcher triggers resolve before either copy, not
// interleaved with them.
func TestCopySpellCopiesGoOnTheStackBeforeTheirTargetTriggers(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	watcher := g.NewCard(becomesTargetCreatureDef(t, "Target Watcher", "Card.Self", ""), p, engine.Battlefield)
	spell := g.NewCard(instantDefWithAbility(t, "Pump", "W", "SP$ Tap | ValidTgts$ Creature"), p, engine.Hand)
	var sink recordingSink
	g.SetSink(&sink)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(watcher)})
	if err := castWithCopyWatcher(t, g, p, spell, c, "Instant", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | Amount$ 2"); err != nil {
		t.Fatal(err)
	}
	var order []string
	for _, e := range sink.events {
		if e.Kind != engine.AbilityResolved {
			continue
		}
		switch {
		case e.Source == watcher:
			order = append(order, "watcher")
		case g.Card(e.Source).IsCopiedSpell:
			order = append(order, "copy")
		case e.Source == spell:
			order = append(order, "original")
		default:
			order = append(order, g.Card(e.Source).Def.Name)
		}
	}
	got := strings.Join(order, ",")
	// The original's own BecomesTarget trigger and the copy trigger go on
	// the stack together at cast; only what follows the copy trigger is
	// under test.
	i := strings.Index(got, "Copy Watcher,")
	if i < 0 || got[i+len("Copy Watcher,"):] != "watcher,watcher,copy,copy,original" {
		t.Errorf("resolution order = %s, want the copy trigger followed by watcher,watcher,copy,copy,original", got)
	}
	if got := g.Player(p).Life; got != 35 {
		t.Errorf("life = %d, want 35: the watcher gains 5 for the original and each copy", got)
	}
}

// TestCopySpellIllegalNewTargetIsAnError proves a new target outside the
// legal candidates fails the line (checkChoice), never lands on the copy.
func TestCopySpellIllegalNewTargetIsAnError(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	bolt := g.NewCard(instantDefWithAbility(t, "Bolt", "W", "SP$ DealDamage | ValidTgts$ Player | NumDmg$ 3"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueConfirmEffect(true)
	c.QueueTargets([]engine.EntityID{engine.CardEntity(bolt)})
	err := castWithCopyWatcher(t, g, p, bolt, c, "Instant", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | MayChooseTarget$ True")
	if err == nil || !strings.Contains(err.Error(), "new targets") {
		t.Errorf("err = %v, want the new-targets choice rejected", err)
	}
}

// TestCopySpellAmountControllerAndOptional proves Amount$ copies per
// Controller$ player, each copy under that player, and Optional$ asking
// each copier once per spell.
func TestCopySpellAmountControllerAndOptional(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	spell := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(true)
	host := engine.CardID(0)
	if err := castWithCopyWatcher(t, g, p, spell, c, "Instant",
		"DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | Amount$ 2 | Controller$ Opponent | Optional$ True | RememberNewCard$ True"); err != nil {
		t.Fatal(err)
	}
	if g.Player(other).Life != 26 || g.Player(p).Life != 23 {
		t.Errorf("life %d/%d, want the opponent's two copies to gain them 6", g.Player(p).Life, g.Player(other).Life)
	}
	for _, id := range g.Zone(engine.Battlefield, p).Cards() {
		if g.Card(id).Def.Name == "Copy Watcher" {
			host = id
		}
	}
	rem := g.Card(host).Memory.Remembered()
	if len(rem) != 2 {
		t.Fatalf("remembered = %v, want both copies' cards", rem)
	}
	for _, e := range rem {
		id, _ := e.AsCard()
		if g.Card(id).Owner != other {
			t.Errorf("copy %v owned by %v, want the copier %v", id, g.Card(id).Owner, other)
		}
	}

	g, p, _ = newTwoPlayerGame(t)
	spell = g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Hand)
	c = engine.NewScriptedController()
	c.QueueConfirmEffect(false)
	if err := castWithCopyWatcher(t, g, p, spell, c, "Instant", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | Optional$ True"); err != nil {
		t.Fatal(err)
	}
	if g.Player(p).Life != 23 {
		t.Errorf("declined: life = %d, want 23", g.Player(p).Life)
	}
}

// TestCopySpellOfAPermanentBecomesAToken proves CR 111.11: a copy of a
// creature spell resolves into a token creature under the copier, and the
// original resolves into the real card.
func TestCopySpellOfAPermanentBecomesAToken(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	bear := g.NewCard(creatureDefCost(t, "Bear", "W"), p, engine.Hand)
	c := engine.NewScriptedController()
	if err := castWithCopyWatcher(t, g, p, bear, c, "Creature", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility"); err != nil {
		t.Fatal(err)
	}
	var tokens, nontokens int
	for _, id := range g.Zone(engine.Battlefield, p).Cards() {
		cd := g.Card(id)
		if cd.Def.Name != "Bear" {
			continue
		}
		if cd.IsCopiedSpell {
			t.Errorf("card %v still marked a copied spell on the battlefield", id)
		}
		if cd.IsToken {
			tokens++
		} else {
			nontokens++
		}
	}
	if tokens != 1 || nontokens != 1 {
		t.Errorf("bears on the battlefield: %d token, %d nontoken, want 1 and 1", tokens, nontokens)
	}
}

// TestCopySpellAbilityRejectsCopyingARoomSpell proves a Room spell on the
// stack cannot be copied: its own Def, while cast, is doorView's single-half
// projection (room.go), not the split card the copy's own roomDef/Room-ness
// would need. Copying it as-is builds a permanent copy of one bare half that
// can never become a Room again -- rejected instead (rules-review finding on
// the merged UnlockDoor commit; GO-7/PORT-8, the same discipline Clone and
// Discover already apply to casting/copying a Room).
func TestCopySpellAbilityRejectsCopyingARoomSpell(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	room := g.NewCard(testRoomDef(t), p, engine.Hand)
	g.NewCard(copyWatcher(t, "Card", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility"), p, engine.Battlefield)
	g.Player(p).ManaPool.Add(mana.Blue, 1)
	c := engine.NewScriptedController()
	if !g.CastSpell(p, room, c) {
		t.Fatal("CastSpell failed")
	}
	err := g.ResolveStack(engine.NewRegistry(), c)
	if err == nil || !strings.Contains(err.Error(), "not resolvable yet") {
		t.Fatalf("ResolveStack = %v, want a not-resolvable-yet error", err)
	}
}

// TestCopySpellTargetsASpellOnTheStack proves the ValidTgts$ shapes: with
// TargetType$ Spell, and without it (Mischievous Quanar's ValidTgts$
// Instant, which CopySpellAbilityEffect.buildSpellAbility points at the
// stack) -- the trigger targets the spell as it is pushed, then copies it.
func TestCopySpellTargetsASpellOnTheStack(t *testing.T) {
	t.Parallel()
	for _, line := range []string{
		"DB$ CopySpellAbility | ValidTgts$ Instant | TargetType$ Spell",
		"DB$ CopySpellAbility | ValidTgts$ Instant",
	} {
		g, p, _ := newTwoPlayerGame(t)
		spell := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Hand)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.CardEntity(spell)})
		if err := castWithCopyWatcher(t, g, p, spell, c, "Instant", line); err != nil {
			t.Fatalf("%s: %v", line, err)
		}
		if got := g.Player(p).Life; got != 26 {
			t.Errorf("%s: life = %d, want 26", line, got)
		}
	}
}

// TestCopySpellKeepsCharmModes proves a copied Charm keeps the modes chosen
// for the original (CR 707.10): they are not chosen again.
func TestCopySpellKeepsCharmModes(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	libraryCards(t, g, p, 4)
	charm := g.NewCard(spellDefWith(t, "Charm", "Instant", "W", "SP$ Charm | Choices$ DBGain,DBDraw",
		"DBGain", "DB$ GainLife | Defined$ You | LifeAmount$ 2",
		"DBDraw", "DB$ Draw | Defined$ You | NumCards$ 1"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueModeChoice([]int{1})
	if err := castWithCopyWatcher(t, g, p, charm, c, "Instant", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | MayChooseTarget$ True"); err != nil {
		t.Fatal(err)
	}
	if got := len(g.Zone(engine.Hand, p).Cards()); got != 2 || g.Player(p).Life != 20 {
		t.Errorf("hand %d life %d, want 2 cards drawn and no life: the draw mode twice", got, g.Player(p).Life)
	}
}

// TestCopySpellCopyCeasesWhenMoved proves CR 707.10a at the move
// primitives: a copied spell's card moved anywhere -- a graveyard, the top
// of a library -- is removed silently to its owner's None zone.
func TestCopySpellCopyCeasesWhenMoved(t *testing.T) {
	t.Parallel()
	for _, toLibrary := range []bool{false, true} {
		g, p, _ := newTwoPlayerGame(t)
		spell := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Hand)
		g.NewCard(copyWatcher(t, "Instant", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | RememberNewCard$ True"), p, engine.Battlefield)
		g.Player(p).ManaPool.Add(mana.White, 1)
		c := engine.NewScriptedController()
		if !g.CastSpell(p, spell, c) {
			t.Fatal("CastSpell failed")
		}
		trig, _ := g.StackTop()
		if err := engine.NewRegistry().Resolve(g, &trig, c); err != nil {
			t.Fatal(err)
		}
		rem := g.Card(trig.Source).Memory.Remembered()
		if len(rem) != 1 {
			t.Fatalf("remembered = %v, want the copy's card", rem)
		}
		cp, _ := rem[0].AsCard()
		if g.Card(cp).Zone != engine.Stack {
			t.Fatalf("copy zone = %v, want Stack", g.Card(cp).Zone)
		}
		if top, _ := g.StackTop(); top.Source != cp || top.ID == trig.ID {
			t.Fatalf("top = %+v, want the copy with its own StackItemID", top)
		}
		var sink recordingSink
		g.SetSink(&sink)
		if toLibrary {
			g.MoveToLibraryTop(cp, p)
		} else {
			g.Move(cp, engine.Graveyard, p)
		}
		if g.Card(cp).Zone != engine.None || len(sink.events) != 0 {
			t.Errorf("toLibrary=%v: zone %v events %v, want None and no event", toLibrary, g.Card(cp).Zone, sink.events)
		}
	}
}

// TestCopySpellRetargetsAnAuraCopy proves an Aura spell's copy may move to a
// new enchant target (CR 707.10c over ChooseEnchantTarget).
func TestCopySpellRetargetsAnAuraCopy(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	first := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	second := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)
	auraDef := auraDefWithEnchant(t, "Creature")
	auraDef.Faces[0].ManaCost = mana.MustParse("W")
	aura := g.NewCard(auraDef, p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueEnchantTarget(first)
	c.QueueConfirmEffect(true)
	c.QueueEnchantTarget(second)
	if err := castWithCopyWatcher(t, g, p, aura, c, "Aura", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | MayChooseTarget$ True"); err != nil {
		t.Fatal(err)
	}
	if host, _ := g.Card(aura).AttachedTo(); host != first {
		t.Errorf("original on %v, want %v", host, first)
	}
	var copyHost engine.CardID
	for _, id := range g.Zone(engine.Battlefield, p).Cards() {
		if cd := g.Card(id); cd.IsToken && cd.Def == auraDef {
			copyHost, _ = cd.AttachedTo()
		}
	}
	if copyHost != second {
		t.Errorf("token Aura on %v, want %v", copyHost, second)
	}
}

// TestCopySpellDropsCantCopyAndSkipsGoneTargets proves CantCopy$ on the
// original spell drops it, and that a targeted card no longer a spell on
// the stack is not copied.
func TestCopySpellDropsCantCopyAndSkipsGoneTargets(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	spell := g.NewCard(instantDefWithAbility(t, "Uncopyable", "W", "SP$ GainLife | Defined$ You | LifeAmount$ 3 | CantCopy$ True"), p, engine.Hand)
	if err := castWithCopyWatcher(t, g, p, spell, engine.NewScriptedController(), "Instant", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility"); err != nil {
		t.Fatal(err)
	}
	if g.Player(p).Life != 23 {
		t.Errorf("life = %d, want 23: CantCopy$ spell not copied", g.Player(p).Life)
	}

	g, p, _ = newTwoPlayerGame(t)
	gone := g.NewCard(gainInstant(t, "Gone", "W", "3"), p, engine.Graveyard)
	pushPlayLine(t, g, p, []engine.EntityID{engine.CardEntity(gone), engine.PlayerEntity(p)}, "DB$ CopySpellAbility | Defined$ Targeted")
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatal(err)
	}
	if g.StackLen() != 0 || g.Player(p).Life != 20 {
		t.Errorf("stack %d life %d, want nothing copied", g.StackLen(), g.Player(p).Life)
	}
}

// TestCopySpellRejectsUnbuiltShapes pins the fail-closed contract: each
// unresolved param, an unbuilt Defined$ shape, a triggering spell that has
// left the stack, and a board with a copy trigger, CopySpell replacement or
// CantBeCopied static to honor all error before anything is copied.
func TestCopySpellRejectsUnbuiltShapes(t *testing.T) {
	t.Parallel()
	for _, tc := range []struct {
		line, want string
	}{
		{"DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | CopyForEachCanTarget$ Creature", "CopyForEachCanTarget$"},
		{"DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | NonLegendary$ True", "NonLegendary$"},
		{"DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | ConditionDefined$ Self | ConditionPresent$ Card", "ConditionDefined$"},
		{"DB$ CopySpellAbility | Defined$ Parent", "Parent"},
		{"DB$ CopySpellAbility", `Defined$ ""`},
		{"DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | Amount$ Bogus", "Amount"},
		{"DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | Controller$ Bogus", "Controller$"},
	} {
		g, p, _ := newTwoPlayerGame(t)
		spell := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Hand)
		err := castWithCopyWatcher(t, g, p, spell, engine.NewScriptedController(), "Instant", tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want one naming %s", tc.line, err, tc.want)
		}
		if len(g.Zone(engine.None, p).Cards()) != 0 {
			t.Errorf("%s: a copy was made before the rejection", tc.line)
		}
	}

	// A Defined$ TriggeredSpellAbility on a trigger that records none.
	g, p, _ := newTwoPlayerGame(t)
	pushPlayLine(t, g, p, nil, "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility")
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err == nil || !strings.Contains(err.Error(), "no triggering spell") {
		t.Errorf("no recorded spell: err = %v", err)
	}

	// A targeted spell whose TargetType$ names an ability.
	g, p, _ = newTwoPlayerGame(t)
	pushPlayLine(t, g, p, nil, "DB$ CopySpellAbility | ValidTgts$ Card | TargetType$ Activated.YouCtrl")
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err == nil || !strings.Contains(err.Error(), "TargetType$") {
		t.Errorf("ability copy: err = %v", err)
	}

	for _, tc := range []struct {
		name, trigger, want string
	}{
		{"copy trigger", "Mode$ SpellCopy | ValidCard$ Card | Execute$ TrigGain", "SpellCopy"},
		{"cast-or-copy trigger", "Mode$ SpellCastOrCopy | ValidCard$ Card | Execute$ TrigGain", "SpellCopy"},
	} {
		g, p, _ := newTwoPlayerGame(t)
		g.NewCard(triggerWatcherDef(t, tc.name, tc.trigger, "TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ 1"), p, engine.Battlefield)
		spell := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Hand)
		err := castWithCopyWatcher(t, g, p, spell, engine.NewScriptedController(), "Instant", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility")
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want one naming %s", tc.name, err, tc.want)
		}
	}

	g, p, _ = newTwoPlayerGame(t)
	g.NewCard(continuousDef(t, "No Copies", "Mode$ CantBeCopied | ValidCard$ Card"), p, engine.Battlefield)
	spell := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Hand)
	if err := castWithCopyWatcher(t, g, p, spell, engine.NewScriptedController(), "Instant", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility"); err == nil || !strings.Contains(err.Error(), "CantBeCopied") {
		t.Errorf("CantBeCopied static: err = %v", err)
	}
}

// TestCopySpellOfASpellThatLeftIsAnError proves a triggering spell no
// longer on the stack (countered above its trigger) is an error, not a
// silent no-op: Java would copy it from last-known information.
func TestCopySpellOfASpellThatLeftIsAnError(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	spell := g.NewCard(gainInstant(t, "Gain", "W", "3"), p, engine.Hand)
	g.NewCard(copyWatcher(t, "Instant", "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility"), p, engine.Battlefield)
	g.Player(p).ManaPool.Add(mana.White, 1)
	c := engine.NewScriptedController()
	if !g.CastSpell(p, spell, c) {
		t.Fatal("CastSpell failed")
	}
	pushPlayLine(t, g, p, []engine.EntityID{engine.CardEntity(spell)}, "DB$ Counter | TargetType$ Spell")
	err := g.ResolveStack(engine.NewRegistry(), c)
	if err == nil || !strings.Contains(err.Error(), "no longer on the stack") {
		t.Errorf("err = %v, want the gone spell rejected", err)
	}
}
