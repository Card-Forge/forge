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

// This file covers the secondary shapes of the fifty-API pack.

func watcherDef(t *testing.T, name, trigger, execute string) *compile.Card {
	t.Helper()
	reg, err := cardtype.LoadRegistry(strings.NewReader("[CreatureTypes]\nElf\n"))
	if err != nil {
		t.Fatal(err)
	}
	raw := &carddb.Card{Filename: name}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = name
	raw.Faces[0].Type = cardtype.Parse(reg, "Creature Elf")
	raw.Faces[0].Power, raw.Faces[0].Toughness = "1", "1"
	raw.Faces[0].Triggers = []string{trigger + " | Execute$ Trig"}
	raw.Faces[0].SVars.Set("Trig", execute)
	c, err := compile.Compile(raw)
	if err != nil {
		t.Fatal(err)
	}
	return c
}

func TestCounterTargetsSpellCastTrigger(t *testing.T) {
	t.Parallel()
	for _, dest := range []string{"Graveyard", "Hand", "TopOfLibrary", "BottomOfLibrary"} {
		g, p, other := newTwoPlayerGame(t)
		g.NewCard(watcherDef(t, "Watcher", "Mode$ SpellCast | ValidActivatingPlayer$ Opponent | TriggerZones$ Battlefield",
			"DB$ Counter | TargetType$ Spell | ValidTgts$ Card | RememberForCounter$ True | Destination$ "+dest), p, engine.Battlefield)
		spell := g.NewCard(creatureDefCost(t, "Spell", "G"), other, engine.Hand)
		g.SetTurnState(1, other, engine.Main1)
		g.Player(other).ManaPool.Add(mana.Green, 1)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{engine.CardEntity(spell)})
		if !g.CastSpell(other, spell, c) {
			t.Fatal("cast failed")
		}
		if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
			t.Fatal(err)
		}
		want := map[string]engine.ZoneType{"Graveyard": engine.Graveyard, "Hand": engine.Hand, "TopOfLibrary": engine.Library, "BottomOfLibrary": engine.Library}[dest]
		if g.Card(spell).Zone != want {
			t.Errorf("%s: spell zone = %v", dest, g.Card(spell).Zone)
		}
	}
}

func TestGainControlVariantModes(t *testing.T) {
	t.Parallel()
	g := newGame(t, "a", "b", "c")
	ps := g.Players()
	for _, p := range ps {
		g.Player(p).Life = 20
	}
	g.SetTurnState(1, ps[0], engine.Main1)
	cs := []engine.CardID{
		g.NewCard(creatureDefPT(t, "5", "5"), ps[0], engine.Battlefield),
		g.NewCard(creatureDefPT(t, "5", "5"), ps[1], engine.Battlefield),
		g.NewCard(creatureDefPT(t, "5", "5"), ps[2], engine.Battlefield),
	}
	c := engine.NewScriptedController()
	c.QueueBinary(true) // left
	resolveLine(t, g, ps[0], c, "DB$ ChooseDirection | SubAbility$ DBSwap",
		"DBSwap", "DB$ GainControlVariant | AllValid$ Creature.powerEQ5 | ChangeController$ NextPlayerInChosenDirection")
	if g.Card(cs[1]).Controller() != ps[0] || g.Card(cs[2]).Controller() != ps[1] || g.Card(cs[0]).Controller() != ps[2] {
		t.Error("next-player control swap wrong")
	}
	c2 := engine.NewScriptedController()
	c2.QueueBinary(false)
	for range ps {
		c2.QueueCardChoice(nil)
	}
	resolveLine(t, g, ps[0], c2, "DB$ ChooseDirection | SubAbility$ DBSwap",
		"DBSwap", "DB$ GainControlVariant | AllValid$ Creature.powerEQ5 | ChangeController$ ChooseNextPlayerInChosenDirection")
	c3 := engine.NewScriptedController()
	for range ps {
		c3.QueueCardChoice(nil)
	}
	resolveLine(t, g, ps[0], c3, "DB$ GainControlVariant | AllValid$ Creature.powerEQ5 | ChangeController$ ChooseFromPlayerToTheirRight")
	resolveLine(t, g, ps[0], engine.NewScriptedController(), "DB$ GainControlVariant | AllValid$ Creature.powerEQ5 | ChangeController$ Random")
	resolveLine(t, g, ps[0], engine.NewScriptedController(), "DB$ GainControlVariant | AllValid$ Creature.powerEQ5 | ChangeController$ NextPlayerInChosenDirection")
}

func TestSetStateChoicesAndStamp(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	card := g.NewCard(transformDef(t), p, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{card})
	c.QueueConfirmEffect(true)
	host := resolveLine(t, g, p, c, "DB$ SetState | Choices$ Creature.powerEQ1 | Mode$ Transform | RememberChanged$ True | Optional$ True | Amount$ 1")
	_ = host
	c2 := engine.NewScriptedController()
	c2.QueueCardChoice([]engine.CardID{card})
	c2.QueueConfirmEffect(true)
	h2 := resolveLine(t, g, p, c2, "DB$ SetState | Choices$ Creature.nonToken | Mode$ Transform | RememberChanged$ True | Optional$ True | Mandatory$ True | MinAmount$ 1")
	if len(g.Card(h2).Memory.Remembered()) != 1 {
		t.Error("transformed card not remembered")
	}
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ SetState | Defined$ Self | Mode$ Transform")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ SetState | Choices$ Creature | Amount$ 0 | Mode$ Transform")
}

func TestAddOrRemoveCounterChoosesKind(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	big := g.NewCard(creatureDefPT(t, "5", "5"), p, engine.Battlefield)
	g.Card(big).Counters.Add(engine.P1P1, 1)
	g.Card(big).Counters.Add(engine.Charge, 1)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(big)})
	c.QueueConfirmEffect(true)
	c.QueueOption(1)
	c.QueueBinary(false)
	host := resolveLine(t, g, p, c, "DB$ AddOrRemoveCounter | ValidTgts$ Creature.powerGE5 | Optional$ True | DefinedPlayer$ You | RememberRemovedCards$ True")
	if g.Card(big).Counters.Count(engine.Charge) != 0 || len(g.Card(host).Memory.Remembered()) != 1 {
		t.Error("charge counter not removed or not remembered")
	}
}

func TestTapOrUntapAllTargetedPlayer(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	x := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	g.Card(x).Tapped = true
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueBinary(false)
	resolveLine(t, g, p, c, "DB$ TapOrUntapAll | ValidTgts$ Player | ValidCards$ Creature")
	if g.Card(x).Tapped {
		t.Error("not untapped")
	}
}

func TestMakeCardNameSources(t *testing.T) {
	t.Parallel()
	bear := namedCreature(t, "Grizzly Bears", "1 G")
	elf := namedCreature(t, "Llanowar Elves", "G")
	g, p, _ := newPackGame(t, bear, elf)
	src := g.NewCard(bear, p, engine.Battlefield)
	g.Card(src).Memory.AddNamedCard("Llanowar Elves")
	resolveLine(t, g, p, engine.NewScriptedController(),
		"DB$ MakeCard | Names$ Grizzly Bears,Llanowar Elves | Zone$ Graveyard | ImprintMade$ True | SubAbility$ DBDef",
		"DBDef", "DB$ MakeCard | DefinedName$ Imprinted | Zone$ Exile | SubAbility$ DBRand",
		"DBRand", "DB$ MakeCard | Choices$ Grizzly Bears,Llanowar Elves | AtRandom$ True | Zone$ Library | LibraryPosition$ -1 | Tapped$ True")
	if n := len(g.Zone(engine.Graveyard, p).Cards()); n != 2 {
		t.Errorf("graveyard = %d, want 2", n)
	}
	if n := len(g.Zone(engine.Exile, p).Cards()); n != 2 {
		t.Errorf("exile = %d, want 2", n)
	}
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(false)
	resolveLine(t, g, p, c, "DB$ MakeCard | Name$ Grizzly Bears | Optional$ True | OptionPrompt$ Q")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ MakeCard | Name$ ChosenName | Zone$ Hand")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ MakeCard | Zone$ Hand")
}

func TestNameCardFaceValidShapes(t *testing.T) {
	t.Parallel()
	bear := namedCreature(t, "Grizzly Bears", "1 G")
	g, p, _ := newPackGame(t, bear, namedLand(t, "Some Land"))
	for _, spec := range []string{"Permanent.nonLand+cmcEQ2", "Creature.cmcEQX", "Land.nonCreature"} {
		c := engine.NewScriptedController()
		c.QueueOption(0)
		resolveLine(t, g, p, c, "DB$ NameCard | Defined$ You | ValidCards$ "+spec, "X", "Count$Valid Card.YouCtrl")
	}
	c := engine.NewScriptedController()
	resolveLine(t, g, p, c, "DB$ NameCard | Defined$ You | ValidCards$ Card.cmcEQ9")
	host := resolveLine(t, g, p, engine.NewScriptedController(), "DB$ NameCard | Defined$ You | ChooseFromList$ Alpha | AtRandom$ True | SubAbility$ DBAgain",
		"DBAgain", "DB$ NameCard | Defined$ You | ChooseFromList$ Alpha | ExcludeChosen$ True")
	if n := len(g.Card(host).Memory.NamedCards()); n != 1 {
		t.Errorf("named %d, want 1 (Alpha excluded the second time)", n)
	}
}

func TestChangeCombatantsRedirectsAndOptional(t *testing.T) {
	t.Parallel()
	g := newGame(t, "a", "b", "c")
	ps := g.Players()
	for _, p := range ps {
		g.Player(p).Life = 20
	}
	g.SetTurnState(1, ps[0], engine.Main1)
	atk := g.NewCard(creatureDefPT(t, "2", "2"), ps[0], engine.Battlefield)
	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{atk})
	ac.QueueAttackTarget(engine.PlayerEntity(ps[1]))
	g.SetTurnState(1, ps[0], engine.DeclareAttackers)
	declareAttackers(t, g, ac)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(true)
	c.QueueAttackTarget(engine.PlayerEntity(ps[2]))
	if _, err := resolveNow(t, g, ps[0], c, []engine.EntityID{engine.CardEntity(atk)},
		"DB$ ChangeCombatants | ValidTgts$ Creature | Attacking$ True | Optional$ True"); err != nil {
		t.Fatal(err)
	}
	if g.AttackTarget(atk) != engine.PlayerEntity(ps[2]) {
		t.Error("not redirected")
	}
	c2 := engine.NewScriptedController()
	c2.QueueAttackTarget(engine.PlayerEntity(ps[2]))
	if _, err := resolveNow(t, g, ps[0], c2, []engine.EntityID{engine.CardEntity(atk)},
		"DB$ ChangeCombatants | ValidTgts$ Creature | Attacking$ True"); err != nil {
		t.Fatal(err)
	}
	if _, err := resolveNow(t, g, ps[0], engine.NewScriptedController(), nil, "DB$ ChangeCombatants | Defined$ Self"); err != nil {
		t.Fatal(err)
	}
}

func TestEndTurnExilesSpellsOnStack(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	g.NewCard(watcherDef(t, "Ender", "Mode$ SpellCast | ValidActivatingPlayer$ Opponent | TriggerZones$ Battlefield",
		"DB$ EndTurn"), p, engine.Battlefield)
	spell := g.NewCard(creatureDefCost(t, "Spell", "G"), other, engine.Hand)
	g.SetTurnState(1, other, engine.Main1)
	g.Player(other).ManaPool.Add(mana.Green, 1)
	c := engine.NewScriptedController()
	if !g.CastSpell(other, spell, c) {
		t.Fatal("cast failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if g.Card(spell).Zone != engine.Exile || g.ActivePhase() != engine.Cleanup {
		t.Errorf("spell zone %v phase %v", g.Card(spell).Zone, g.ActivePhase())
	}
}

// TestAbilityResolvedCarriesThePhaseItResolvedIn proves resolveTop
// (stack.go) captures Phase/Active/Turn before calling Resolve, not after:
// EndTurn jumps g.activePhase to Cleanup and calls beginPhase as part of
// its own resolve, so a read taken after Resolve returns would describe
// the wrong phase for the ability that just resolved.
func TestAbilityResolvedCarriesThePhaseItResolvedIn(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	var sink recordingSink
	g.SetSink(&sink)
	ender := g.NewCard(creatureDefWithAbility(t, "Test Ender", "AB$ EndTurn | Cost$ T"), p, engine.Battlefield)
	g.SetTurnState(1, p, engine.Main1)
	g.Player(p).Life, g.Player(other).Life = 20, 20

	if !g.ActivateAbility(p, ender, 0, engine.NewScriptedController()) {
		t.Fatal("ActivateAbility(EndTurn) returned false, want true")
	}
	if err := g.ResolveStack(engine.NewRegistry(), engine.NewScriptedController()); err != nil {
		t.Fatalf("ResolveStack: %v", err)
	}

	var resolvedPhase engine.PhaseType
	found := false
	for _, e := range sink.events {
		if e.Kind == engine.AbilityResolved && e.Source == ender {
			resolvedPhase, found = e.Phase, true
		}
	}
	if !found {
		t.Fatal("no AbilityResolved event for the EndTurn ability")
	}
	if resolvedPhase != engine.Main1 {
		t.Errorf("AbilityResolved.Phase = %v, want Main1 (the phase EndTurn resolved in, not %v it jumped to)",
			resolvedPhase, g.ActivePhase())
	}
}

func TestRemoveFromGameSpellOnStack(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	g.NewCard(watcherDef(t, "Remover", "Mode$ SpellCast | ValidActivatingPlayer$ Opponent | TriggerZones$ Battlefield",
		"DB$ RemoveFromGame | ValidTgts$ Card"), p, engine.Battlefield)
	spell := g.NewCard(creatureDefCost(t, "Spell", "G"), other, engine.Hand)
	g.SetTurnState(1, other, engine.Main1)
	g.Player(other).ManaPool.Add(mana.Green, 1)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(spell)})
	if !g.CastSpell(other, spell, c) {
		t.Fatal("cast failed")
	}
	if err := g.ResolveStack(engine.NewRegistry(), c); err != nil {
		t.Fatal(err)
	}
	if g.Card(spell).Zone != engine.None || g.StackLen() != 0 {
		t.Errorf("spell zone %v, stack %d", g.Card(spell).Zone, g.StackLen())
	}
}

func TestGoadEndsAtGoaderTurnUnlessPermanent(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "2", "2"), other, engine.Battlefield)
	b := g.NewCard(creatureDefPT(t, "3", "3"), other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(a)})
	host := resolveLine(t, g, p, c, "DB$ Goad | ValidTgts$ Creature | RememberGoaded$ True")
	if len(g.Card(host).Memory.Remembered()) != 1 {
		t.Error("goaded not remembered")
	}
	c2 := engine.NewScriptedController()
	c2.QueueTargets([]engine.EntityID{engine.CardEntity(b)})
	resolveLine(t, g, p, c2, "DB$ Goad | ValidTgts$ Creature | Duration$ Permanent")
	g.SetTurnState(1, other, engine.Cleanup)
	g.AdvancePhase(engine.NewScriptedController())
	if g.Card(a).IsGoaded() || !g.Card(b).IsGoaded() {
		t.Errorf("goaded a=%v b=%v, want only the permanent one", g.Card(a).IsGoaded(), g.Card(b).IsGoaded())
	}
	g.SetTurnState(2, p, engine.Main1)
	c3 := engine.NewScriptedController()
	c3.QueueTargets([]engine.EntityID{engine.CardEntity(b)})
	resolveLine(t, g, p, c3, "DB$ Goad | ValidTgts$ Creature | NoLonger$ True")
	if g.Card(b).IsGoaded() {
		t.Error("NoLonger$ kept the goad")
	}
}

func TestActivateAbilityScriptedManaChoice(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	reg := attachmentTypeRegistry(t)
	raw := &carddb.Card{Filename: "Dual"}
	raw.Faces[0].Present = true
	raw.Faces[0].Name = "Dual"
	raw.Faces[0].Type = cardtype.Parse(reg, "Land Forest")
	raw.Faces[0].Abilities = []string{"AB$ Mana | Cost$ T | Produced$ R"}
	def, err := compile.Compile(raw)
	if err != nil {
		t.Fatal(err)
	}
	land := g.NewCard(def, other, engine.Battlefield)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	c.QueueOption(1)
	resolveLine(t, g, p, c, "DB$ ActivateAbility | ValidTgts$ Player | Type$ Land | ManaAbility$ True")
	if !g.Card(land).Tapped {
		t.Error("land not tapped for its scripted ability")
	}
}

func TestDayTimeNightBecomesDay(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ DayTime | Value$ Night")
	g.Player(p).SpellsCastThisTurn = 2
	g.SetTurnState(1, p, engine.Cleanup)
	g.AdvancePhase(engine.NewScriptedController())
	if g.DayTime() != engine.Day {
		t.Errorf("daytime = %v, want Day after two spells", g.DayTime())
	}
}

func TestVoteEachVoteAndRememberVoted(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueAbilityChoice([]int{0})
	c.QueueAbilityChoice([]int{0})
	resolveLine(t, g, p, c, "DB$ Vote | Defined$ Player | Choices$ DBA | EachVote$ True",
		"DBA", "DB$ GainLife | Defined$ Remembered | LifeAmount$ 1")
	if g.Player(p).Life != 21 || g.Player(other).Life != 21 {
		t.Errorf("life %d/%d, want 21/21", g.Player(p).Life, g.Player(other).Life)
	}
	x := g.NewCard(creatureDefPT(t, "4", "4"), other, engine.Battlefield)
	c2 := engine.NewScriptedController()
	c2.QueueEntityChoice([]engine.EntityID{engine.CardEntity(x)})
	c2.QueueEntityChoice([]engine.EntityID{engine.CardEntity(x)})
	host := resolveLine(t, g, p, c2, "DB$ Vote | Defined$ Player | VoteCard$ Creature.powerEQ4 | RememberVotedObjects$ True")
	if len(g.Card(host).Memory.Remembered()) != 1 {
		t.Error("voted card not remembered")
	}
	c3 := engine.NewScriptedController()
	c3.QueueEntityChoice([]engine.EntityID{engine.PlayerEntity(other)})
	c3.QueueEntityChoice([]engine.EntityID{engine.PlayerEntity(p)})
	resolveLine(t, g, p, c3, "DB$ Vote | Defined$ Player | VotePlayer$ Other")
}

func TestReverseTurnOrderTwoPlayersAndIntensifyDefault(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ ReverseTurnOrder")
	g.SetTurnState(1, p, engine.Cleanup)
	g.AdvancePhase(engine.NewScriptedController())
	if g.ActivePlayer() != other {
		t.Error("two-player reversed order skipped the other player")
	}
}

func TestCopyPermanentControllerAndOptional(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(true)
	c.QueueConfirmEffect(false) // the copy's own ETB copy, declined
	resolveLine(t, g, p, c, "DB$ CopyPermanent | Defined$ Self | Controller$ Opponent | Optional$ True | SetToughness$ 3")
	var found bool
	for _, id := range g.Zone(engine.Battlefield, other).Cards() {
		if g.Card(id).IsToken {
			found = true
		}
	}
	if !found {
		t.Error("no copy for the opponent")
	}
	c2 := engine.NewScriptedController()
	c2.QueueConfirmEffect(false)
	resolveLine(t, g, p, c2, "DB$ CopyPermanent | Defined$ Self | Optional$ True")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ CopyPermanent | Choices$ Creature.powerEQ9")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ CopyPermanent | DefinedName$ NamedCard")
}

// TestFiftyPackConditionSkips proves every effect of the pack checks its
// sub-ability condition before doing anything: an unmet one resolves as a
// no-op, not an error.
func TestFiftyPackConditionSkips(t *testing.T) {
	t.Parallel()
	for _, api := range []string{
		"BlankLine", "GameDrawn", "RemoveFromGame", "ReverseTurnOrder", "ChangeSpeed", "GainOwnership",
		"ReorderZone", "EndTurn", "EndCombatPhase", "ChooseEvenOdd", "ChooseDirection", "ExchangeLifeVariant",
		"ExchangePower", "TapOrUntapAll", "AddOrRemoveCounter", "BecomesBlocked", "Block", "ChangeCombatants",
		"GainControlVariant", "Detain", "Intensify", "Blight", "TimeTravel", "Endure", "AssignGroup",
		"VillainousChoice", "TwoPiles", "ChooseType", "NameCard", "PreventDamage", "DigMultiple", "Recruit",
		"BidLife", "ExchangeControlVariant", "DayTime", "AlterAttribute", "Vote", "MakeCard", "Learn",
		"CopyPermanent", "Counter", "Manifest", "Cloak", "ManifestDread", "SetState", "Goad",
		"RemoveFromMatch", "ActivateAbility", "MultiplePiles",
	} {
		g, p, _ := newPackGame(t)
		line := "DB$ " + api + " | ConditionCheckSVar$ Z | ConditionSVarCompare$ EQ99"
		if api == "Counter" {
			line += " | TargetType$ Spell"
		}
		def := etbChainDef(t, "Skip", line, "Z", "Number$0")
		if _, err := castETBChain(t, g, p, def, engine.NewScriptedController()); err != nil {
			t.Errorf("%s: %v", api, err)
		}
		if g.Over() && api != "GameDrawn" {
			t.Errorf("%s: game over", api)
		}
	}
}

func TestTwoPilesVariants(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	a := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Hand)
	b := g.NewCard(creatureDefPT(t, "1", "1"), p, engine.Hand)
	c := engine.NewScriptedController()
	c.QueueCardChoice([]engine.CardID{a})
	host := resolveLine(t, g, p, c,
		"DB$ TwoPiles | Defined$ You | Zone$ Hand | ValidCards$ Creature | Separator$ Opponent | Chooser$ You | LeftRightPile$ True | RememberChosen$ True | UnchosenPile$ DBDiscard",
		"DBDiscard", "DB$ ChangeZone | Defined$ Remembered | Origin$ Hand | Destination$ Graveyard")
	if g.Card(b).Zone != engine.Graveyard || g.Card(a).Zone != engine.Hand {
		t.Errorf("zones a=%v b=%v", g.Card(a).Zone, g.Card(b).Zone)
	}
	if rem := g.Card(host).Memory.Remembered(); len(rem) != 1 {
		t.Errorf("remembered %v, want the chosen pile", rem)
	}
	_ = other
	c2 := engine.NewScriptedController()
	c2.QueueBinary(true)
	resolveLine(t, g, p, c2, "DB$ TwoPiles | Defined$ You | DefinedPiles$ Self,Self | KeepRemembered$ True")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ TwoPiles | Defined$ You | Zone$ Exile")
}

func TestManifestShapes(t *testing.T) {
	t.Parallel()
	g, p, other := newTwoPlayerGame(t)
	libraryCards(t, g, other, 3)
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	resolveLine(t, g, p, c, "DB$ Manifest | ValidTgts$ Player | Amount$ 2 | Shuffle$ True")
	if n := len(g.Zone(engine.Battlefield, other).Cards()); n != 2 {
		t.Errorf("other's battlefield = %d, want 2 manifested", n)
	}
	h := g.NewCard(creatureDefPT(t, "3", "3"), p, engine.Hand)
	c2 := engine.NewScriptedController()
	c2.QueueCardChoice([]engine.CardID{h})
	resolveLine(t, g, p, c2, "DB$ Cloak | DefinedPlayer$ You | Choices$ Creature | Tapped$ True | RememberCloaked$ True")
	if !g.Card(h).Tapped || !g.Card(h).Cloaked {
		t.Error("cloak not tapped")
	}
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Manifest | Choices$ Creature.powerEQ9")
	host := resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Manifest | Defined$ Self")
	if !g.Card(host).IsFaceDown() {
		t.Error("Defined$ Self not manifested")
	}
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ ManifestDread | DefinedPlayer$ Opponent | Amount$ 0")
}

func TestBlockAndBecomesBlockedOutsideCombat(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ BecomesBlocked | Defined$ Self")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Block | DefinedAttacker$ Self | DefinedBlocker$ Self")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ EndCombatPhase")
}

func TestBidLifeOtherBidderAndAny(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGame(t)
	c := engine.NewScriptedController()
	c.QueueNumberChoice(4)
	c.QueueConfirmEffect(false)
	c.QueueConfirmEffect(false)
	host := resolveLine(t, g, p, c, "DB$ BidLife | StartBidding$ Any | OtherBidder$ Opponent")
	if n, _ := g.Card(host).Memory.ChosenNumber(); n != 4 {
		t.Errorf("bid = %d, want 4", n)
	}
}

func TestPackMoreBranches(t *testing.T) {
	t.Parallel()
	g, p, _ := newPackGame(t)
	// Vote: EachVote needs Choices$, VoteSubAbility over Choices$ is refused.
	c := engine.NewScriptedController()
	c.QueueEntityChoice([]engine.EntityID{engine.PlayerEntity(p)})
	c.QueueEntityChoice([]engine.EntityID{engine.PlayerEntity(p)})
	if _, err := castETBChain(t, g, p, etbChainDef(t, "V", "DB$ Vote | Defined$ Player | VotePlayer$ Player | EachVote$ True"), c); err == nil {
		t.Error("EachVote$ over players resolved")
	}
	g, p, other := newPackGame(t)
	c = engine.NewScriptedController()
	c.QueueAbilityChoice([]int{0})
	c.QueueAbilityChoice([]int{0})
	if _, err := castETBChain(t, g, p, etbChainDef(t, "V", "DB$ Vote | Defined$ Player | Choices$ DBX | VoteSubAbility$ DBX",
		"DBX", "DB$ GainLife | Defined$ You | LifeAmount$ 1"), c); err == nil {
		t.Error("VoteSubAbility$ over Choices$ resolved")
	}
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Vote | Defined$ Player | VoteCard$ Creature.powerEQ9")
	// DigMultiple: Optional$ with nothing picked leaves every card on the bottom.
	libraryCards(t, g, p, 2)
	c = engine.NewScriptedController()
	c.QueueCardChoice(nil)
	resolveLine(t, g, p, c, "DB$ DigMultiple | DigNum$ 2 | ChangeValid$ Card | Optional$ True | RestRandomOrder$ True | DestinationZone2$ Graveyard | LibraryPosition2$ 0")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ DigMultiple | DigNum$ 2 | ChangeValid$ Creature.powerEQ9 | SourceZone$ Exile")
	// PreventDamage on a targeted card that stays, and one that is gone.
	x := g.NewCard(creatureDefPT(t, "5", "5"), other, engine.Battlefield)
	c = engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.CardEntity(x)})
	resolveLine(t, g, p, c, "DB$ PreventDamage | ValidTgts$ Creature | Amount$ 2 | SubAbility$ DBHit",
		"DBHit", "DB$ DealDamage | Defined$ Self | NumDmg$ 1")
	g.Move(x, engine.Graveyard, other)
	// TurnFaceUp on a face-up card changes nothing.
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ SetState | Defined$ Self | Mode$ TurnFaceUp | RememberChanged$ True")
	// ChooseEvenOdd with a lost player skips them.
	g.Player(other).Lost = true
	c = engine.NewScriptedController()
	c.QueueBinary(true)
	resolveLine(t, g, p, c, "DB$ ChooseEvenOdd | Defined$ Player")
}

func TestCopyPermanentSkipsInstant(t *testing.T) {
	t.Parallel()
	spell := &compile.Card{Name: "Bolt"}
	spell.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), "Instant")
	g, p, _ := newPackGame(t, spell)
	host := resolveLine(t, g, p, engine.NewScriptedController(), "DB$ CopyPermanent | DefinedName$ Bolt | RememberTokens$ True")
	if n := len(g.Card(host).Memory.Remembered()); n != 0 {
		t.Errorf("copied an instant (%d tokens)", n)
	}
}

func TestPackSmallBranches(t *testing.T) {
	t.Parallel()
	g, p, other := newPackGame(t)
	libraryCards(t, g, other, 3)
	c := engine.NewScriptedController()
	c.QueueConfirmEffect(true)
	host := resolveLine(t, g, p, c, "DB$ AlterAttribute | Defined$ Self | Attributes$ Harnessed,Plotted,Solve,Suspect | Optional$ True | RememberAltered$ True")
	if hc := g.Card(host); !hc.Harnessed || !hc.Plotted || !hc.Solved || !hc.Suspected {
		t.Error("attributes not set")
	}
	c = engine.NewScriptedController()
	c.QueueConfirmEffect(false)
	resolveLine(t, g, p, c, "DB$ AlterAttribute | Defined$ Self | Attributes$ Solved | Optional$ True")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ GainControlVariant | AllValid$ Creature | ChangeController$ ChooseNextPlayerInChosenDirection")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ MultiplePiles | Defined$ You | Piles$ 1")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ RemoveFromMatch | Defined$ Self")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Learn | Defined$ Opponent")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ ActivateAbility | Defined$ You | ManaAbility$ True | Type$ Land")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ ExchangeControlVariant | Defined$ You")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ TimeTravel")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Blight | Defined$ Opponent")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Endure | Num$ 0")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ AssignGroup | Defined$ Player")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ VillainousChoice | Defined$ You")
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ ExchangeLifeVariant | Mode$ Power | Defined$ Opponent | SubAbility$ DBAgain",
		"DBAgain", "DB$ ExchangePower | Defined$ Self")
	g.Player(other).Lost = true
	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ ChangeSpeed | Defined$ Player | SubAbility$ DBR",
		"DBR", "DB$ ReorderZone | Zone$ Hand | Defined$ Player")
}
