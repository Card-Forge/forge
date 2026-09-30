package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// typedDef builds just enough of a *compile.Card for Card.Type() to answer
// with typeLine -- the generic version of action_test.go's auraDef/
// equipmentDef, for a type shape neither of those already covers.
func typedDef(t *testing.T, typeLine string) *compile.Card {
	t.Helper()
	def := &compile.Card{Name: "Test " + typeLine}
	def.Faces[0].Type = cardtype.Parse(attachmentTypeRegistry(t), typeLine)
	return def
}

// A bare color name matches a card carrying that color, derived from its
// mana cost (Card.Colors' own doc comment) -- and not one it lacks. All
// five, not just one, since colorMatches names each independently.
func TestMatchesColor(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]

	for _, tc := range []struct{ cost, color, other string }{
		{"1 W", "White", "Blue"},
		{"1 U", "Blue", "Black"},
		{"1 B", "Black", "Red"},
		{"1 R", "Red", "Green"},
		{"1 G", "Green", "White"},
	} {
		id := g.NewCard(creatureDefManaCost(t, tc.cost), p, engine.Battlefield)
		if !engine.Matches(g, g.Card(id), valid.Parse("Creature."+tc.color), p, engine.NoCard) {
			t.Errorf("a %s creature did not match Creature.%s", tc.color, tc.color)
		}
		if engine.Matches(g, g.Card(id), valid.Parse("Creature."+tc.other), p, engine.NoCard) {
			t.Errorf("a %s creature matched Creature.%s", tc.color, tc.other)
		}
	}
}

// The "non" form of a color negates it -- "nonBlack" matches anything that
// is not black, including a card of a different color entirely.
func TestMatchesNonColor(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	red := g.NewCard(creatureDefManaCost(t, "1 R"), p, engine.Battlefield)
	black := g.NewCard(creatureDefManaCost(t, "1 B"), p, engine.Battlefield)

	if !engine.Matches(g, g.Card(red), valid.Parse("Creature.nonBlack"), p, engine.NoCard) {
		t.Error("a red creature did not match Creature.nonBlack")
	}
	if engine.Matches(g, g.Card(black), valid.Parse("Creature.nonBlack"), p, engine.NoCard) {
		t.Error("a black creature matched Creature.nonBlack")
	}
}

// Colorless and nonColorless are each other's opposite, and neither is a
// color name -- a colored card is not Colorless, and a colorless one is not
// nonColorless.
func TestMatchesColorlessAndNonColorless(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	colorless := g.NewCard(creatureDefManaCost(t, "3"), p, engine.Battlefield)
	red := g.NewCard(creatureDefManaCost(t, "1 R"), p, engine.Battlefield)

	if !engine.Matches(g, g.Card(colorless), valid.Parse("Creature.Colorless"), p, engine.NoCard) {
		t.Error("a colorless creature did not match Creature.Colorless")
	}
	if engine.Matches(g, g.Card(red), valid.Parse("Creature.Colorless"), p, engine.NoCard) {
		t.Error("a red creature matched Creature.Colorless")
	}
	if !engine.Matches(g, g.Card(red), valid.Parse("Creature.nonColorless"), p, engine.NoCard) {
		t.Error("a red creature did not match Creature.nonColorless")
	}
	if engine.Matches(g, g.Card(colorless), valid.Parse("Creature.nonColorless"), p, engine.NoCard) {
		t.Error("a colorless creature matched Creature.nonColorless")
	}
}

// MultiColor matches two or more colors, not one and not zero.
func TestMatchesMultiColor(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	mono := g.NewCard(creatureDefManaCost(t, "1 R"), p, engine.Battlefield)
	multi := g.NewCard(creatureDefManaCost(t, "R G"), p, engine.Battlefield)

	if engine.Matches(g, g.Card(mono), valid.Parse("Creature.MultiColor"), p, engine.NoCard) {
		t.Error("a monocolored creature matched Creature.MultiColor")
	}
	if !engine.Matches(g, g.Card(multi), valid.Parse("Creature.MultiColor"), p, engine.NoCard) {
		t.Error("a two-color creature did not match Creature.MultiColor")
	}
}

// TestMatchesSharesColorWith proves the bare SharesColorWith property
// (staticability.go's Intimidate synthesis is its real caller): source's own
// color, not sourceController's, is what c is compared against, and a
// colorless c never shares a color with anything.
func TestMatchesSharesColorWith(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	source := g.NewCard(creatureDefManaCost(t, "R"), p, engine.Battlefield)
	sameColor := g.NewCard(creatureDefManaCost(t, "R"), p, engine.Battlefield)
	offColor := g.NewCard(creatureDefManaCost(t, "U"), p, engine.Battlefield)
	colorless := g.NewCard(creatureDefManaCost(t, "3"), p, engine.Battlefield)

	if !engine.Matches(g, g.Card(sameColor), valid.Parse("Creature.SharesColorWith"), p, source) {
		t.Error("a same-color creature did not match Creature.SharesColorWith")
	}
	if engine.Matches(g, g.Card(offColor), valid.Parse("Creature.SharesColorWith"), p, source) {
		t.Error("an off-color creature matched Creature.SharesColorWith")
	}
	if engine.Matches(g, g.Card(colorless), valid.Parse("Creature.SharesColorWith"), p, source) {
		t.Error("a colorless creature matched Creature.SharesColorWith")
	}
	if engine.Matches(g, g.Card(sameColor), valid.Parse("Creature.!SharesColorWith"), p, source) {
		t.Error("a same-color creature matched Creature.!SharesColorWith")
	}
	if !engine.Matches(g, g.Card(offColor), valid.Parse("Creature.!SharesColorWith"), p, source) {
		t.Error("an off-color creature did not match Creature.!SharesColorWith")
	}
}

// YouDontCtrl is YouCtrl's simple negation.
func TestMatchesYouDontCtrl(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	id := g.NewCard(nil, a, engine.Battlefield)

	if engine.Matches(g, g.Card(id), valid.Parse("Card.YouDontCtrl"), a, engine.NoCard) {
		t.Error("a's own card matched YouDontCtrl from a's perspective")
	}
	if !engine.Matches(g, g.Card(id), valid.Parse("Card.YouDontCtrl"), b, engine.NoCard) {
		t.Error("a's card did not match YouDontCtrl from b's perspective")
	}
}

// YouOwn/YouDontOwn/OppOwn mirror YouCtrl/YouDontCtrl/OppCtrl exactly, but
// against Owner rather than Controller -- distinct once something steals
// control, which nothing here needs to for this test.
func TestMatchesYouOwnOppOwn(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	id := g.NewCard(nil, a, engine.Battlefield) // owned and controlled by a

	if !engine.Matches(g, g.Card(id), valid.Parse("Card.YouOwn"), a, engine.NoCard) {
		t.Error("a's own card did not match YouOwn from a's perspective")
	}
	if engine.Matches(g, g.Card(id), valid.Parse("Card.YouOwn"), b, engine.NoCard) {
		t.Error("a's card matched YouOwn from b's perspective")
	}
	if engine.Matches(g, g.Card(id), valid.Parse("Card.YouDontOwn"), a, engine.NoCard) {
		t.Error("a's own card matched YouDontOwn from a's perspective")
	}
	if !engine.Matches(g, g.Card(id), valid.Parse("Card.OppOwn"), b, engine.NoCard) {
		t.Error("a's card did not match OppOwn from b's perspective")
	}
	if engine.Matches(g, g.Card(id), valid.Parse("Card.OppOwn"), a, engine.NoCard) {
		t.Error("a's own card matched OppOwn from a's own perspective")
	}
}

// Other is Self's negation: the source card does not match, any other card
// does.
func TestMatchesOther(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	source := g.NewCard(nil, p, engine.Battlefield)
	other := g.NewCard(nil, p, engine.Battlefield)

	if engine.Matches(g, g.Card(source), valid.Parse("Card.Other"), p, source) {
		t.Error("the source card matched Other")
	}
	if !engine.Matches(g, g.Card(other), valid.Parse("Card.Other"), p, source) {
		t.Error("a different card did not match Other")
	}
	// StrictlyOther has no game-timestamp tracking to distinguish from
	// Other with (game-state.md's "Not ported yet"), so it reads the same.
	if !engine.Matches(g, g.Card(other), valid.Parse("Card.StrictlyOther"), p, source) {
		t.Error("a different card did not match StrictlyOther")
	}
}

// tapped and untapped read Card.Tapped directly.
func TestMatchesTappedUntapped(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	id := g.NewCard(nil, p, engine.Battlefield)

	if engine.Matches(g, g.Card(id), valid.Parse("Card.tapped"), p, engine.NoCard) {
		t.Error("an untapped card matched tapped")
	}
	if !engine.Matches(g, g.Card(id), valid.Parse("Card.untapped"), p, engine.NoCard) {
		t.Error("an untapped card did not match untapped")
	}

	g.Card(id).Tapped = true
	if !engine.Matches(g, g.Card(id), valid.Parse("Card.tapped"), p, engine.NoCard) {
		t.Error("a tapped card did not match tapped")
	}
	if engine.Matches(g, g.Card(id), valid.Parse("Card.untapped"), p, engine.NoCard) {
		t.Error("a tapped card matched untapped")
	}
}

// with<Keyword>/without<Keyword>/hasKeyword<Keyword> all reduce to the same
// HasKeyword check -- three spellings the corpus uses for the same
// question.
func TestMatchesKeywordProperties(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	flier := g.NewCard(creatureDefPTKeywords(t, "2", "2", "Flying"), p, engine.Battlefield)
	grounded := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	for _, spec := range []string{"Creature.withFlying", "Creature.hasKeywordFlying"} {
		if !engine.Matches(g, g.Card(flier), valid.Parse(spec), p, engine.NoCard) {
			t.Errorf("a flier did not match %s", spec)
		}
		if engine.Matches(g, g.Card(grounded), valid.Parse(spec), p, engine.NoCard) {
			t.Errorf("a grounded creature matched %s", spec)
		}
	}
	if !engine.Matches(g, g.Card(grounded), valid.Parse("Creature.withoutFlying"), p, engine.NoCard) {
		t.Error("a grounded creature did not match Creature.withoutFlying")
	}
	if engine.Matches(g, g.Card(flier), valid.Parse("Creature.withoutFlying"), p, engine.NoCard) {
		t.Error("a flier matched Creature.withoutFlying")
	}
}

// A generic "non<Type>" property, not one of the five named colors, falls
// to the type-negation fallback -- "nonLand" is "!HasStringType(Land)".
func TestMatchesNonTypeFallback(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	creature := g.NewCard(creatureDef(t), p, engine.Battlefield)

	if !engine.Matches(g, g.Card(creature), valid.Parse("Card.nonLand"), p, engine.NoCard) {
		t.Error("a creature did not match Card.nonLand")
	}
	if engine.Matches(g, g.Card(creature), valid.Parse("Card.nonCreature"), p, engine.NoCard) {
		t.Error("a creature matched Card.nonCreature")
	}
}

// A bare type word as a Base matches by type, ignoring properties entirely
// when there are none.
func TestMatchesBaseCoreType(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	aura := g.NewCard(auraDef(t), p, engine.Battlefield)
	equipment := g.NewCard(equipmentDef(t), p, engine.Battlefield)

	if !engine.Matches(g, g.Card(aura), valid.Parse("Enchantment"), p, engine.NoCard) {
		t.Error("an Aura did not match its own core type")
	}
	if engine.Matches(g, g.Card(aura), valid.Parse("Creature"), p, engine.NoCard) {
		t.Error("an Aura matched Creature")
	}
	if !engine.Matches(g, g.Card(equipment), valid.Parse("Artifact"), p, engine.NoCard) {
		t.Error("an Equipment did not match its own core type")
	}
}

// A subtype word, and the vocabulary fallthrough case-sensitivity
// HasStringType already covers, both reach the same code path as a core
// type.
func TestMatchesBaseSubtype(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	aura := g.NewCard(auraDef(t), p, engine.Battlefield)

	if !engine.Matches(g, g.Card(aura), valid.Parse("Aura"), p, engine.NoCard) {
		t.Error("an Aura did not match its own subtype")
	}
}

// "Permanent" and "Card" are Java's own special Base cases, not a type
// lookup.
func TestMatchesSpecialBases(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	aura := g.NewCard(auraDef(t), p, engine.Battlefield) // Enchantment: a permanent

	if !engine.Matches(g, g.Card(aura), valid.Parse("Permanent"), p, engine.NoCard) {
		t.Error("an Aura (a permanent) did not match Permanent")
	}
	if !engine.Matches(g, g.Card(aura), valid.Parse("Card"), p, engine.NoCard) {
		t.Error("an ordinary card did not match Card")
	}
}

// "Any" is Java's own shorthand for "a creature, a planeswalker or a
// battle" -- not a type lookup, and not literally every card.
func TestMatchesBaseAny(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	creature := g.NewCard(creatureDef(t), p, engine.Battlefield)
	aura := g.NewCard(auraDef(t), p, engine.Battlefield)

	if !engine.Matches(g, g.Card(creature), valid.Parse("Any"), p, engine.NoCard) {
		t.Error("a creature did not match Any")
	}
	if engine.Matches(g, g.Card(aura), valid.Parse("Any"), p, engine.NoCard) {
		t.Error("an Aura (not a creature, planeswalker or battle) matched Any")
	}
}

// A bare type word used as a property, not a base -- "Permanent.Creature"
// -- reaches the same HasStringType fallthrough a Base does.
func TestMatchesPropertyTypeWordFallthrough(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	creature := g.NewCard(creatureDef(t), p, engine.Battlefield)

	if !engine.Matches(g, g.Card(creature), valid.Parse("Permanent.Creature"), p, engine.NoCard) {
		t.Error("a creature did not match Permanent.Creature")
	}
	if engine.Matches(g, g.Card(creature), valid.Parse("Permanent.Land"), p, engine.NoCard) {
		t.Error("a creature matched Permanent.Land")
	}
}

// Effect, Emblem and Boon are coverage gaps, not matches; Spell is
// Card.isSpell, which an Aura on the battlefield is not
// (TestSpellBaseMatchesInstantsSorceriesAndOffBattlefieldAuras has the
// cases that match).
func TestMatchesUnbuiltBasesNeverMatch(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	aura := g.NewCard(auraDef(t), p, engine.Battlefield)

	for _, base := range []string{"Spell", "Effect", "Emblem", "Boon"} {
		if engine.Matches(g, g.Card(aura), valid.Parse(base), p, engine.NoCard) {
			t.Errorf("an ordinary permanent matched Base %q", base)
		}
	}
}

// YouCtrl and OppCtrl compare the card's controller against sourceController,
// the perspective Matches is called from -- not the card's owner, and not
// some fixed player.
func TestMatchesYouCtrlOppCtrl(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	id := g.NewCard(nil, a, engine.Battlefield) // controlled by a

	if !engine.Matches(g, g.Card(id), valid.Parse("Card.YouCtrl"), a, engine.NoCard) {
		t.Error("a's own card did not match YouCtrl from a's perspective")
	}
	if engine.Matches(g, g.Card(id), valid.Parse("Card.YouCtrl"), b, engine.NoCard) {
		t.Error("a's card matched YouCtrl from b's perspective")
	}
	if !engine.Matches(g, g.Card(id), valid.Parse("Card.OppCtrl"), b, engine.NoCard) {
		t.Error("a's card did not match OppCtrl from b's perspective")
	}
	if engine.Matches(g, g.Card(id), valid.Parse("Card.OppCtrl"), a, engine.NoCard) {
		t.Error("a's own card matched OppCtrl from a's own perspective")
	}
}

// Self compares the card's own handle against source, not its controller.
func TestMatchesSelf(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	source := g.NewCard(nil, p, engine.Battlefield)
	other := g.NewCard(nil, p, engine.Battlefield)

	if !engine.Matches(g, g.Card(source), valid.Parse("Card.Self"), p, source) {
		t.Error("the source card did not match Self")
	}
	if engine.Matches(g, g.Card(other), valid.Parse("Card.Self"), p, source) {
		t.Error("a different card matched Self")
	}
}

// Properties within one alternative are AND: all of them have to match.
func TestMatchesPropertiesAreConjunctive(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	aura := g.NewCard(auraDef(t), a, engine.Battlefield)

	if !engine.Matches(g, g.Card(aura), valid.Parse("Enchantment.YouCtrl"), a, engine.NoCard) {
		t.Error("an Aura controlled by a did not match Enchantment.YouCtrl from a's perspective")
	}
	if engine.Matches(g, g.Card(aura), valid.Parse("Enchantment.YouCtrl"), b, engine.NoCard) {
		t.Error("an Aura controlled by a matched Enchantment.YouCtrl from b's perspective")
	}
	if engine.Matches(g, g.Card(aura), valid.Parse("Creature.YouCtrl"), a, engine.NoCard) {
		t.Error("an Aura matched Creature.YouCtrl even though the base does not match")
	}
}

// Alternatives are OR: matching either is enough.
func TestMatchesAlternativesAreDisjunctive(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	aura := g.NewCard(auraDef(t), p, engine.Battlefield)

	if !engine.Matches(g, g.Card(aura), valid.Parse("Creature,Enchantment"), p, engine.NoCard) {
		t.Error("an Aura did not match the second alternative of Creature,Enchantment")
	}
	if engine.Matches(g, g.Card(aura), valid.Parse("Creature,Land"), p, engine.NoCard) {
		t.Error("an Aura matched neither alternative but Matches still reported true")
	}
}

// A `!` on a property negates only that property -- the simple, De Morgan
// case (Card.hasProperty's own wrapper).
func TestMatchesPropertyNegation(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	id := g.NewCard(nil, a, engine.Battlefield)

	if !engine.Matches(g, g.Card(id), valid.Parse("Card.!OppCtrl"), a, engine.NoCard) {
		t.Error("a's own card did not match Card.!OppCtrl from a's perspective")
	}
	if engine.Matches(g, g.Card(id), valid.Parse("Card.!OppCtrl"), b, engine.NoCard) {
		t.Error("a's card matched Card.!OppCtrl from b's perspective, where OppCtrl itself holds")
	}
}

// Each numeric-comparison field reads the value CardProperty.java names for
// it, not the accessor its own name might suggest -- basePower/baseToughness
// measure Layer 7 folded in (layer7Power's own doc comment), not the printed
// BasePower/BaseToughness.
func TestMatchesNumericComparisons(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]

	for _, tc := range []struct {
		name string
		def  *compile.Card
		spec string
		want bool
	}{
		{"power current", creatureDefPT(t, "3", "2"), "Creature.powerGE3", true},
		{"power current miss", creatureDefPT(t, "3", "2"), "Creature.powerGE4", false},
		{"power folds counters", creatureDefPT(t, "3", "2"), "Creature.powerGE5", true /* +2 counters below */},
		{"toughness LT", creatureDefPT(t, "3", "2"), "Creature.toughnessLT3", true},
		{"toughness EQ", creatureDefPT(t, "3", "2"), "Creature.toughnessEQ2", true},
		{"cmc GE", creatureDefManaCost(t, "2 R"), "Creature.cmcGE3", true},
		{"cmc NE", creatureDefManaCost(t, "2 R"), "Creature.cmcNE3", false},
		{"numColors EQ", creatureDefManaCost(t, "R G"), "Creature.numColorsEQ2", true},
		{"numTypes GE", creatureDef(t), "Creature.numTypesGE1", true},
		{"totalPT GE", creatureDefPT(t, "3", "2"), "Creature.totalPT_GE5", true},
		{"totalPT LT", creatureDefPT(t, "3", "2"), "Creature.totalPT_LT5", false},
		{"M2 even", creatureDefPT(t, "4", "2"), "Creature.powerM20", true},
		{"M2 odd", creatureDefPT(t, "3", "2"), "Creature.powerM20", false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			id := g.NewCard(tc.def, p, engine.Battlefield)
			if tc.name == "power folds counters" {
				g.Card(id).Counters.Add(engine.P1P1, 2)
			}
			if got := engine.Matches(g, g.Card(id), valid.Parse(tc.spec), p, engine.NoCard); got != tc.want {
				t.Errorf("%s: Matches(%s) = %v, want %v", tc.name, tc.spec, got, tc.want)
			}
		})
	}
}

// basePower/baseToughness measure Java's getCurrentPower/getCurrentToughness
// -- base folded with Layer 7, but counters excluded -- distinct from the
// printed-only BasePower/BaseToughness accessors of the same name.
func TestMatchesNumericComparisonsExcludeCountersFromBaseForms(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	id := g.NewCard(creatureDefPT(t, "3", "2"), p, engine.Battlefield)
	g.Card(id).Counters.Add(engine.P1P1, 5)

	if !engine.Matches(g, g.Card(id), valid.Parse("Creature.basePowerEQ3"), p, engine.NoCard) {
		t.Error("basePowerEQ3 did not match a printed-3-power creature with +1/+1 counters piled on")
	}
	if engine.Matches(g, g.Card(id), valid.Parse("Creature.basePowerEQ8"), p, engine.NoCard) {
		t.Error("basePowerEQ8 matched -- counters must not have leaked into the base form")
	}
	if !engine.Matches(g, g.Card(id), valid.Parse("Creature.powerEQ8"), p, engine.NoCard) {
		t.Error("powerEQ8 did not match -- the full form must fold counters in")
	}
}

// An unresolvable printed value ("*") is a coverage gap, not a wrong answer:
// the comparison matches nothing rather than guessing.
func TestMatchesNumericComparisonUnresolvableFieldIsGap(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	id := g.NewCard(creatureDefPT(t, "*", "2"), p, engine.Battlefield)

	if engine.Matches(g, g.Card(id), valid.Parse("Creature.powerGE0"), p, engine.NoCard) {
		t.Error("a Creature.powerGE0 matched a card with unresolvable (\"*\") power")
	}
}

// A non-numeric operand ("X", "Chosen", an SVar name) is a coverage gap this
// port cannot resolve without an ability-context evaluator -- the property
// matches nothing rather than guessing.
func TestMatchesNumericComparisonNonNumericOperandIsGap(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	id := g.NewCard(creatureDefPT(t, "3", "2"), p, engine.Battlefield)

	for _, spec := range []string{"Creature.powerGEX", "Creature.powerGEChosen", "Creature.powerGEY"} {
		if engine.Matches(g, g.Card(id), valid.Parse(spec), p, engine.NoCard) {
			t.Errorf("%s matched despite a non-numeric operand", spec)
		}
	}
}

// IsRemembered matches only the card source has remembered, not any other
// card -- membership in Memory.Remembered, keyed by entity handle rather
// than card handle (Remembered holds players too).
func TestMatchesIsRemembered(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	source := g.NewCard(nil, p, engine.Battlefield)
	remembered := g.NewCard(nil, p, engine.Battlefield)
	forgotten := g.NewCard(nil, p, engine.Battlefield)
	g.Card(source).Memory.Remember(engine.CardEntity(remembered))

	if !engine.Matches(g, g.Card(remembered), valid.Parse("Card.IsRemembered"), p, source) {
		t.Error("a remembered card did not match Card.IsRemembered")
	}
	if engine.Matches(g, g.Card(forgotten), valid.Parse("Card.IsRemembered"), p, source) {
		t.Error("an unremembered card matched Card.IsRemembered")
	}
}

// IsImprinted mirrors IsRemembered, against Memory.Imprinted.
func TestMatchesIsImprinted(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	source := g.NewCard(nil, p, engine.Battlefield)
	imprinted := g.NewCard(nil, p, engine.Battlefield)
	other := g.NewCard(nil, p, engine.Battlefield)
	g.Card(source).Memory.Imprint(imprinted)

	if !engine.Matches(g, g.Card(imprinted), valid.Parse("Card.IsImprinted"), p, source) {
		t.Error("an imprinted card did not match Card.IsImprinted")
	}
	if engine.Matches(g, g.Card(other), valid.Parse("Card.IsImprinted"), p, source) {
		t.Error("a card that was not imprinted matched Card.IsImprinted")
	}
}

// ChosenCard and ChosenCardStrict read the same Memory.Chosen membership --
// this port has no game-timestamp tracking to tell the "Strict" form apart
// (Self/StrictlyOther's own precedent). nonChosenCard is the plain
// negation, its own named property in the corpus rather than a `!` prefix.
func TestMatchesChosenCard(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	source := g.NewCard(nil, p, engine.Battlefield)
	chosen := g.NewCard(nil, p, engine.Battlefield)
	unchosen := g.NewCard(nil, p, engine.Battlefield)
	g.Card(source).Memory.Choose(chosen)

	for _, spec := range []string{"Card.ChosenCard", "Card.ChosenCardStrict"} {
		if !engine.Matches(g, g.Card(chosen), valid.Parse(spec), p, source) {
			t.Errorf("a chosen card did not match %s", spec)
		}
		if engine.Matches(g, g.Card(unchosen), valid.Parse(spec), p, source) {
			t.Errorf("an unchosen card matched %s", spec)
		}
	}
	if engine.Matches(g, g.Card(chosen), valid.Parse("Card.nonChosenCard"), p, source) {
		t.Error("a chosen card matched Card.nonChosenCard")
	}
	if !engine.Matches(g, g.Card(unchosen), valid.Parse("Card.nonChosenCard"), p, source) {
		t.Error("an unchosen card did not match Card.nonChosenCard")
	}
}

// A memory-based property with no source card at all (NoCard) matches
// nothing, the same "false for every card" answer any other unresolvable
// property gives -- not a panic, even though Game.Card itself panics on
// NoCard everywhere else it is called.
func TestMatchesMemoryPropertiesWithoutASourceCard(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	id := g.NewCard(nil, p, engine.Battlefield)

	for _, spec := range []string{"Card.IsRemembered", "Card.IsImprinted", "Card.ChosenCard", "Card.nonChosenCard"} {
		if engine.Matches(g, g.Card(id), valid.Parse(spec), p, engine.NoCard) {
			t.Errorf("%s matched with no source card", spec)
		}
	}
}

// EnchantedBy, EquippedBy, AttachedBy and FortifiedBy are one check in this
// port regardless of which of the four names is written: is source among
// the things attached to c. Direction matters -- the attachment does not
// match from the host's own perspective, only the other way around.
func TestMatchesAttachmentProperties(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	host := g.NewCard(nil, p, engine.Battlefield)
	attachment := g.NewCard(nil, p, engine.Battlefield)
	other := g.NewCard(nil, p, engine.Battlefield)
	g.Attach(attachment, host)

	for _, spec := range []string{"Card.EnchantedBy", "Card.EquippedBy", "Card.AttachedBy", "Card.FortifiedBy"} {
		if !engine.Matches(g, g.Card(host), valid.Parse(spec), p, attachment) {
			t.Errorf("the attached host did not match %s from the attachment's own perspective", spec)
		}
		if engine.Matches(g, g.Card(host), valid.Parse(spec), p, other) {
			t.Errorf("the attached host matched %s from an unrelated card's perspective", spec)
		}
		if engine.Matches(g, g.Card(attachment), valid.Parse(spec), p, host) {
			t.Errorf("the attachment itself matched %s from its own host's perspective -- direction reversed", spec)
		}
	}
}

// A restricted form ("EnchantedBy Aura.YouCtrl") is not equal to any of the
// four bare names, so it is a coverage gap: it matches nothing rather than
// evaluating the nested restriction.
func TestMatchesAttachmentPropertyRestrictedFormIsGap(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	host := g.NewCard(nil, p, engine.Battlefield)
	attachment := g.NewCard(nil, p, engine.Battlefield)
	g.Attach(attachment, host)

	if engine.Matches(g, g.Card(host), valid.Parse("Card.EnchantedBy Aura.YouCtrl"), p, attachment) {
		t.Error("a restricted EnchantedBy form matched despite the restriction never being evaluated")
	}
}

// inZone reads c.Zone directly, matching only the zone actually named.
func TestMatchesInZone(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	onBattlefield := g.NewCard(nil, p, engine.Battlefield)
	inGraveyard := g.NewCard(nil, p, engine.Graveyard)

	if !engine.Matches(g, g.Card(onBattlefield), valid.Parse("Card.inZoneBattlefield"), p, engine.NoCard) {
		t.Error("a battlefield card did not match Card.inZoneBattlefield")
	}
	if engine.Matches(g, g.Card(onBattlefield), valid.Parse("Card.inZoneGraveyard"), p, engine.NoCard) {
		t.Error("a battlefield card matched Card.inZoneGraveyard")
	}
	if !engine.Matches(g, g.Card(inGraveyard), valid.Parse("Card.inZoneGraveyard"), p, engine.NoCard) {
		t.Error("a graveyard card did not match Card.inZoneGraveyard")
	}
}

// inRealZone reads the same c.Zone, no LKI to distinguish it from inZone in
// this port (YouCtrl's own LKI-collapse precedent).
func TestMatchesInRealZone(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	id := g.NewCard(nil, p, engine.Stack)

	if !engine.Matches(g, g.Card(id), valid.Parse("Card.inRealZoneStack"), p, engine.NoCard) {
		t.Error("a stack card did not match Card.inRealZoneStack")
	}
	if engine.Matches(g, g.Card(id), valid.Parse("Card.inRealZoneExile"), p, engine.NoCard) {
		t.Error("a stack card matched Card.inRealZoneExile")
	}
}

// A zone name ZoneByName does not recognize is a coverage gap: the property
// matches nothing rather than guessing.
func TestMatchesInZoneUnknownZoneNameIsGap(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	id := g.NewCard(nil, p, engine.Battlefield)

	if engine.Matches(g, g.Card(id), valid.Parse("Card.inZoneNotAZone"), p, engine.NoCard) {
		t.Error("Card.inZoneNotAZone matched despite naming no real zone")
	}
}

// attacking matches only a declared attacker, not a creature that could
// have attacked but did not, and not the defending creature.
func TestMatchesAttacking(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	stayedHome := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	defender := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, ac)

	if !engine.Matches(g, g.Card(attacker), valid.Parse("Card.attacking"), a, engine.NoCard) {
		t.Error("the declared attacker did not match Card.attacking")
	}
	if engine.Matches(g, g.Card(stayedHome), valid.Parse("Card.attacking"), a, engine.NoCard) {
		t.Error("a creature that did not attack matched Card.attacking")
	}
	if engine.Matches(g, g.Card(defender), valid.Parse("Card.attacking"), a, engine.NoCard) {
		t.Error("the defending creature matched Card.attacking")
	}
}

// A creature does not match attacking before any attack is declared --
// Combat's zero value (no combat in progress) reads the same as combat
// having happened with zero attackers, and both mean "not attacking".
func TestMatchesAttackingBeforeCombat(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	id := g.NewCard(creatureDefPT(t, "2", "2"), p, engine.Battlefield)

	if engine.Matches(g, g.Card(id), valid.Parse("Card.attacking"), p, engine.NoCard) {
		t.Error("a creature matched Card.attacking with no combat declared at all")
	}
}

// blocking matches a declared blocker, not the attacker it blocks and not
// an eligible-but-undeclared blocker.
func TestMatchesBlocking(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)
	stayedHome := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, ac)

	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: attacker}})
	declareBlockers(t, g, bc)

	if !engine.Matches(g, g.Card(blocker), valid.Parse("Card.blocking"), a, engine.NoCard) {
		t.Error("the declared blocker did not match Card.blocking")
	}
	if engine.Matches(g, g.Card(attacker), valid.Parse("Card.blocking"), a, engine.NoCard) {
		t.Error("the attacker matched Card.blocking")
	}
	if engine.Matches(g, g.Card(stayedHome), valid.Parse("Card.blocking"), a, engine.NoCard) {
		t.Error("an undeclared creature matched Card.blocking")
	}
}

// blocked matches an attacker with a declared blocker, not an unblocked
// attacker and not the blocker itself (CardProperty.java:1591).
func TestMatchesBlocked(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, a, engine.Main1)
	blockedAttacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	freeAttacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)
	blocker := g.NewCard(creatureDefPT(t, "2", "2"), b, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{blockedAttacker, freeAttacker})
	declareAttackers(t, g, ac)
	bc := engine.NewScriptedController()
	bc.QueueBlocks([]engine.Block{{Blocker: blocker, Attacker: blockedAttacker}})
	declareBlockers(t, g, bc)

	spec := valid.Parse("Creature.attacking+blocked")
	if !engine.Matches(g, g.Card(blockedAttacker), spec, a, engine.NoCard) {
		t.Error("the blocked attacker did not match Creature.attacking+blocked")
	}
	if engine.Matches(g, g.Card(freeAttacker), spec, a, engine.NoCard) {
		t.Error("the unblocked attacker matched Creature.attacking+blocked")
	}
	if engine.Matches(g, g.Card(blocker), valid.Parse("Card.blocked"), a, engine.NoCard) {
		t.Error("the blocker itself matched Card.blocked")
	}
}

// A suffixed attacking/blocking form ("attackingYou", "blockingSource") is
// not equal to the bare name, so it is a coverage gap: it matches nothing
// rather than evaluating the suffix.
func TestMatchesAttackingBlockingSuffixedFormIsGap(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a := g.Players()[0]
	g.SetTurnState(1, a, engine.Main1)
	attacker := g.NewCard(creatureDefPT(t, "2", "2"), a, engine.Battlefield)

	ac := engine.NewScriptedController()
	ac.QueueAttackers([]engine.CardID{attacker})
	declareAttackers(t, g, ac)

	if engine.Matches(g, g.Card(attacker), valid.Parse("Card.attackingYou"), a, engine.NoCard) {
		t.Error("Card.attackingYou matched despite the suffix never being evaluated")
	}
}

// HasCounters matches any card with a counter of any kind, and nothing
// without one.
func TestMatchesHasCounters(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	countered := g.NewCard(nil, p, engine.Battlefield)
	bare := g.NewCard(nil, p, engine.Battlefield)
	g.Card(countered).Counters.Add(engine.P1P1, 1)

	if !engine.Matches(g, g.Card(countered), valid.Parse("Card.HasCounters"), p, engine.NoCard) {
		t.Error("a countered card did not match Card.HasCounters")
	}
	if engine.Matches(g, g.Card(bare), valid.Parse("Card.HasCounters"), p, engine.NoCard) {
		t.Error("a card with no counters matched Card.HasCounters")
	}
}

// counters_<op><n>_<type> compares one counter kind's count, the same
// operators compareOp already covers for power/toughness/cmc.
func TestMatchesCounters(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	id := g.NewCard(nil, p, engine.Battlefield)
	g.Card(id).Counters.Add(engine.P1P1, 3)

	for _, tc := range []struct {
		spec string
		want bool
	}{
		{"Card.counters_GE1_P1P1", true},
		{"Card.counters_GE9_P1P1", false},
		{"Card.counters_LT4_P1P1", true},
		{"Card.counters_EQ3_P1P1", true},
		{"Card.counters_GE1_M1M1", false}, // a different counter kind entirely
	} {
		if got := engine.Matches(g, g.Card(id), valid.Parse(tc.spec), p, engine.NoCard); got != tc.want {
			t.Errorf("Matches(%s) = %v, want %v", tc.spec, got, tc.want)
		}
	}
}

// A non-numeric operand ("counters_LTX_P1P1") is a coverage gap, the same as
// the plain numeric comparisons' own X/Chosen/SVar gap -- it matches nothing
// rather than guessing. The four-part "ReceivedThisTurn" form is a separate
// gap this port has no per-turn counter tracking for at all.
func TestMatchesCountersGaps(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	id := g.NewCard(nil, p, engine.Battlefield)
	g.Card(id).Counters.Add(engine.P1P1, 3)

	for _, spec := range []string{"Card.counters_LTX_P1P1", "Card.countersReceivedThisTurn_GE1_P1P1_You"} {
		if engine.Matches(g, g.Card(id), valid.Parse(spec), p, engine.NoCard) {
			t.Errorf("%s matched despite being an unhandled form", spec)
		}
	}
}

// enchanted matches a host with an Aura attached, not one with an Equipment
// attached and not a bare host -- the subtype of the attachment is what
// this checks, not mere presence.
func TestMatchesEnchanted(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	host := g.NewCard(nil, p, engine.Battlefield)
	aura := g.NewCard(auraDef(t), p, engine.Battlefield)
	g.Attach(aura, host)

	bare := g.NewCard(nil, p, engine.Battlefield)
	equippedHost := g.NewCard(nil, p, engine.Battlefield)
	equipment := g.NewCard(equipmentDef(t), p, engine.Battlefield)
	g.Attach(equipment, equippedHost)

	if !engine.Matches(g, g.Card(host), valid.Parse("Card.enchanted"), p, engine.NoCard) {
		t.Error("a host with an Aura attached did not match Card.enchanted")
	}
	if engine.Matches(g, g.Card(bare), valid.Parse("Card.enchanted"), p, engine.NoCard) {
		t.Error("a bare host matched Card.enchanted")
	}
	if engine.Matches(g, g.Card(equippedHost), valid.Parse("Card.enchanted"), p, engine.NoCard) {
		t.Error("a host with only an Equipment attached matched Card.enchanted")
	}
}

// equipped mirrors enchanted, against Equipment instead of Aura.
func TestMatchesEquipped(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	host := g.NewCard(nil, p, engine.Battlefield)
	equipment := g.NewCard(equipmentDef(t), p, engine.Battlefield)
	g.Attach(equipment, host)

	bare := g.NewCard(nil, p, engine.Battlefield)
	enchantedHost := g.NewCard(nil, p, engine.Battlefield)
	aura := g.NewCard(auraDef(t), p, engine.Battlefield)
	g.Attach(aura, enchantedHost)

	if !engine.Matches(g, g.Card(host), valid.Parse("Card.equipped"), p, engine.NoCard) {
		t.Error("a host with an Equipment attached did not match Card.equipped")
	}
	if engine.Matches(g, g.Card(bare), valid.Parse("Card.equipped"), p, engine.NoCard) {
		t.Error("a bare host matched Card.equipped")
	}
	if engine.Matches(g, g.Card(enchantedHost), valid.Parse("Card.equipped"), p, engine.NoCard) {
		t.Error("a host with only an Aura attached matched Card.equipped")
	}
}

// modified is CR 707.9's three-way OR: a counter, an Equipment, or an Aura
// controlled by the same player who controls the modified card.
func TestMatchesModified(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]

	countered := g.NewCard(nil, a, engine.Battlefield)
	g.Card(countered).Counters.Add(engine.P1P1, 1)

	equippedHost := g.NewCard(nil, a, engine.Battlefield)
	equipment := g.NewCard(equipmentDef(t), a, engine.Battlefield)
	g.Attach(equipment, equippedHost)

	ownAura := g.NewCard(nil, a, engine.Battlefield)
	auraFromOwner := g.NewCard(auraDef(t), a, engine.Battlefield)
	g.Attach(auraFromOwner, ownAura)

	oppAura := g.NewCard(nil, a, engine.Battlefield)
	auraFromOpponent := g.NewCard(auraDef(t), b, engine.Battlefield)
	g.Attach(auraFromOpponent, oppAura)

	bare := g.NewCard(nil, a, engine.Battlefield)

	for _, tc := range []struct {
		name string
		id   engine.CardID
		want bool
	}{
		{"counter", countered, true},
		{"equipment", equippedHost, true},
		{"own-controlled aura", ownAura, true},
		{"opponent-controlled aura", oppAura, false},
		{"bare", bare, false},
	} {
		if got := engine.Matches(g, g.Card(tc.id), valid.Parse("Card.modified"), a, engine.NoCard); got != tc.want {
			t.Errorf("%s: Matches(Card.modified) = %v, want %v", tc.name, got, tc.want)
		}
	}

	// The opponent-controlled Aura still makes the host "enchanted" -- only
	// "modified" cares who controls it.
	if !engine.Matches(g, g.Card(oppAura), valid.Parse("Card.enchanted"), a, engine.NoCard) {
		t.Error("a host enchanted by an opponent's Aura did not match Card.enchanted")
	}
}

// RememberedPlayerCtrl matches a card whose controller source has
// remembered, not one whose owner has been remembered instead (a
// distinction only visible once control changes, which this test does not
// need to exercise to prove the two fields are read separately).
func TestMatchesRememberedPlayerCtrl(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	source := g.NewCard(nil, a, engine.Battlefield)
	g.Card(source).Memory.Remember(engine.PlayerEntity(a))

	yours := g.NewCard(nil, a, engine.Battlefield)
	theirs := g.NewCard(nil, b, engine.Battlefield)

	if !engine.Matches(g, g.Card(yours), valid.Parse("Card.RememberedPlayerCtrl"), a, source) {
		t.Error("a card controlled by the remembered player did not match Card.RememberedPlayerCtrl")
	}
	if engine.Matches(g, g.Card(theirs), valid.Parse("Card.RememberedPlayerCtrl"), a, source) {
		t.Error("a card controlled by an unremembered player matched Card.RememberedPlayerCtrl")
	}
}

// RememberedPlayerOwn is RememberedPlayerCtrl's own counterpart against
// Owner instead of Controller.
func TestMatchesRememberedPlayerOwn(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	source := g.NewCard(nil, a, engine.Battlefield)
	g.Card(source).Memory.Remember(engine.PlayerEntity(b))

	ownedByB := g.NewCard(nil, b, engine.Battlefield)
	ownedByA := g.NewCard(nil, a, engine.Battlefield)

	if !engine.Matches(g, g.Card(ownedByB), valid.Parse("Card.RememberedPlayerOwn"), a, source) {
		t.Error("a card owned by the remembered player did not match Card.RememberedPlayerOwn")
	}
	if engine.Matches(g, g.Card(ownedByA), valid.Parse("Card.RememberedPlayerOwn"), a, source) {
		t.Error("a card owned by an unremembered player matched Card.RememberedPlayerOwn")
	}
}

// A `$`-suffixed RememberedPlayerCtrl/RememberedPlayerOwn form is a
// coverage gap -- neither name is an exact match once anything follows it,
// so it falls through rather than guessing which player field to read.
func TestMatchesRememberedPlayerSuffixedFormIsGap(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	source := g.NewCard(nil, p, engine.Battlefield)
	g.Card(source).Memory.Remember(engine.PlayerEntity(p))
	id := g.NewCard(nil, p, engine.Battlefield)

	for _, spec := range []string{
		"Card.RememberedPlayerCtrl$GreatestCardManaCost",
		"Card.RememberedPlayerOwn$GreatestCardManaCost",
	} {
		if engine.Matches(g, g.Card(id), valid.Parse(spec), p, source) {
			t.Errorf("%s matched despite the suffix never being evaluated", spec)
		}
	}
}

// ActivePlayerCtrl matches a card controlled by whoever's turn it is, not by
// sourceController -- it reads Game.ActivePlayer, not the perspective
// Matches was called from.
func TestMatchesActivePlayerCtrl(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	g.SetTurnState(1, a, engine.Main1)
	active := g.NewCard(nil, a, engine.Battlefield)
	inactive := g.NewCard(nil, b, engine.Battlefield)

	if !engine.Matches(g, g.Card(active), valid.Parse("Card.ActivePlayerCtrl"), b, engine.NoCard) {
		t.Error("the active player's card did not match Card.ActivePlayerCtrl from the other player's perspective")
	}
	if engine.Matches(g, g.Card(inactive), valid.Parse("Card.ActivePlayerCtrl"), b, engine.NoCard) {
		t.Error("the inactive player's card matched Card.ActivePlayerCtrl")
	}
}

// Historic matches a Legendary permanent, an Artifact, or a Saga -- CR's
// own umbrella, three different type chains, any one of which is enough --
// and nothing else.
func TestMatchesHistoric(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	legendary := g.NewCard(legendaryCreatureDef(t, "Test Legend"), p, engine.Battlefield)
	artifact := g.NewCard(equipmentDef(t), p, engine.Battlefield)
	saga := g.NewCard(typedDef(t, "Enchantment Saga"), p, engine.Battlefield)
	plain := g.NewCard(creatureDef(t), p, engine.Battlefield)

	for _, tc := range []struct {
		name string
		id   engine.CardID
		want bool
	}{
		{"legendary", legendary, true},
		{"artifact", artifact, true},
		{"saga", saga, true},
		{"plain creature", plain, false},
	} {
		if got := engine.Matches(g, g.Card(tc.id), valid.Parse("Card.Historic"), p, engine.NoCard); got != tc.want {
			t.Errorf("%s: Matches(Card.Historic) = %v, want %v", tc.name, got, tc.want)
		}
	}
}

// Outlaw and Party each match a Creature (or Kindred) with one of a fixed
// set of creature types -- Assassin/Mercenary/Pirate/Rogue/Warlock for
// Outlaw, Cleric/Rogue/Warrior/Wizard for Party -- and nothing else, not
// even a noncreature permanent of the same subtype.
func TestMatchesOutlawAndParty(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	pirate := g.NewCard(typedDef(t, "Creature Pirate"), p, engine.Battlefield)
	wizard := g.NewCard(typedDef(t, "Creature Wizard"), p, engine.Battlefield)
	rogue := g.NewCard(typedDef(t, "Creature Rogue"), p, engine.Battlefield) // both Outlaw and Party
	elf := g.NewCard(creatureDef(t), p, engine.Battlefield)                  // neither

	if !engine.Matches(g, g.Card(pirate), valid.Parse("Creature.Outlaw"), p, engine.NoCard) {
		t.Error("a Pirate did not match Creature.Outlaw")
	}
	if engine.Matches(g, g.Card(pirate), valid.Parse("Creature.Party"), p, engine.NoCard) {
		t.Error("a Pirate matched Creature.Party")
	}
	if !engine.Matches(g, g.Card(wizard), valid.Parse("Creature.Party"), p, engine.NoCard) {
		t.Error("a Wizard did not match Creature.Party")
	}
	if engine.Matches(g, g.Card(wizard), valid.Parse("Creature.Outlaw"), p, engine.NoCard) {
		t.Error("a Wizard matched Creature.Outlaw")
	}
	if !engine.Matches(g, g.Card(rogue), valid.Parse("Creature.Outlaw"), p, engine.NoCard) {
		t.Error("a Rogue did not match Creature.Outlaw")
	}
	if !engine.Matches(g, g.Card(rogue), valid.Parse("Creature.Party"), p, engine.NoCard) {
		t.Error("a Rogue did not match Creature.Party")
	}
	if engine.Matches(g, g.Card(elf), valid.Parse("Creature.Outlaw"), p, engine.NoCard) {
		t.Error("an Elf matched Creature.Outlaw")
	}
	if engine.Matches(g, g.Card(elf), valid.Parse("Creature.Party"), p, engine.NoCard) {
		t.Error("an Elf matched Creature.Party")
	}
}

// A permanent with an Outlaw/Party subtype but no Creature or Kindred type
// does not match -- CardType.isOutlaw/isParty both require one of those two
// core types first.
func TestMatchesOutlawRequiresCreatureOrKindred(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a")
	p := g.Players()[0]
	// A Rogue-typed Instant is not realistic, but the property's own
	// definition does not care what kind of permanent (or non-permanent)
	// carries the subtype -- only whether Creature/Kindred is also present.
	id := g.NewCard(typedDef(t, "Instant Rogue"), p, engine.Battlefield)

	if engine.Matches(g, g.Card(id), valid.Parse("Card.Outlaw"), p, engine.NoCard) {
		t.Error("a non-Creature, non-Kindred Rogue matched Card.Outlaw")
	}
}

// A `!` on the base negates the whole alternative -- base AND every
// property -- not just the base by itself (Card.isValid's testFailed
// short-circuit). "!Creature.YouCtrl" matches everything that is not a
// creature you control, including a creature an opponent controls, not
// only "a non-creature you control".
func TestMatchesBaseNegationCoversTheWholeAlternative(t *testing.T) {
	t.Parallel()

	g := newGame(t, "a", "b")
	a, b := g.Players()[0], g.Players()[1]
	yours := g.NewCard(auraDef(t), a, engine.Battlefield)
	theirs := g.NewCard(auraDef(t), b, engine.Battlefield)

	spec := valid.Parse("!Permanent.YouCtrl")

	if engine.Matches(g, g.Card(yours), spec, a, engine.NoCard) {
		t.Error("a permanent a controls matched !Permanent.YouCtrl from a's own perspective")
	}
	// theirs is a permanent OppCtrl-relative to a, so "Permanent.YouCtrl" is
	// false for it from a's perspective -- and the negated form must then be
	// true, not "false because theirs is still a Permanent".
	if !engine.Matches(g, g.Card(theirs), spec, a, engine.NoCard) {
		t.Error("a permanent a does not control did not match !Permanent.YouCtrl from a's perspective")
	}
}
