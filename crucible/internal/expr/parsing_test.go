package expr_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/expr"
)

// calculateAmount tests three things in order: a plain number, a `$` past the
// first character, and otherwise an SVar name.
func TestParseKinds(t *testing.T) {
	t.Parallel()

	for _, tt := range []struct {
		amount string
		kind   expr.Kind
	}{
		{"3", expr.Literal},
		{"+3", expr.Literal},
		{"-1", expr.Literal},
		{"0", expr.Literal},
		{"X", expr.Reference},
		{"IronclawX", expr.Reference},
		{"Count$CardsInYourHand", expr.Expression},
		{"Remembered$CardPower", expr.Expression},
		{"PlayerCountOpponents$Amount", expr.Expression},
	} {
		if got := expr.Parse(tt.amount).Kind; got != tt.kind {
			t.Errorf("Parse(%q).Kind = %s, want %s", tt.amount, got, tt.kind)
		}
	}
}

func TestLiteralSigns(t *testing.T) {
	t.Parallel()

	for _, tt := range []struct {
		amount string
		value  int
	}{
		{"3", 3},
		{"+3", 3},
		{"-1", -1},
		{"0", 0},
		{"10", 10},
	} {
		got := expr.Parse(tt.amount)
		if got.Kind != expr.Literal || got.Value != tt.value {
			t.Errorf("Parse(%q) = %+v, want the literal %d", tt.amount, got, tt.value)
		}
	}
}

// A `$` makes the value a raw expression, but only past the first character:
// Java tests indexOf('$') > 0, so a leading one is a name that will not resolve.
func TestDollarMustNotLead(t *testing.T) {
	t.Parallel()

	if got := expr.Parse("$Weird").Kind; got != expr.Reference {
		t.Errorf("Parse(\"$Weird\").Kind = %s, want Reference -- indexOf('$') > 0", got)
	}
}

func TestExpressionParts(t *testing.T) {
	t.Parallel()

	for _, tt := range []struct {
		amount   string
		head     string
		body     string
		operator string
		operand  string
	}{
		{"Count$CardsInYourHand", "Count", "CardsInYourHand", "", ""},
		{"Count$CardsInYourHand/Twice", "Count", "CardsInYourHand", "Twice", ""},
		{"Count$CardsInYourHand/Plus.2", "Count", "CardsInYourHand", "Plus", "2"},
		{"Count$Valid Creature.YouCtrl/LimitMax.1", "Count", "Valid Creature.YouCtrl", "LimitMax", "1"},
		{"Remembered$CardToughness/Minus.1", "Remembered", "CardToughness", "Minus", "1"},
		{"SVar$X/Times.Y", "SVar", "X", "Times", "Y"},
	} {
		got := expr.Parse(tt.amount)
		if got.Head != tt.head || got.Body != tt.body {
			t.Errorf("Parse(%q) head/body = %q/%q, want %q/%q", tt.amount, got.Head, got.Body, tt.head, tt.body)
		}
		if tt.operator == "" {
			if got.Op != nil {
				t.Errorf("Parse(%q).Op = %+v, want none", tt.amount, *got.Op)
			}
			continue
		}
		if got.Op == nil {
			t.Errorf("Parse(%q).Op is nil, want %s.%s", tt.amount, tt.operator, tt.operand)
			continue
		}
		if got.Op.Name != tt.operator || got.Op.Operand != tt.operand {
			t.Errorf("Parse(%q).Op = %+v, want %s.%s", tt.amount, *got.Op, tt.operator, tt.operand)
		}
	}
}

// Only the first operator applies. extractOperators takes l[1] from a split on
// "/" and nothing else, so a second is dropped -- reproduced, not fixed,
// because no corpus expression writes one and evaluating more than Forge does
// is a disagreement with the oracle (PORT-7).
func TestOnlyTheFirstOperatorApplies(t *testing.T) {
	t.Parallel()

	got := expr.Parse("Count$CardsInYourHand/Twice/Plus.3")
	if got.Op == nil || got.Op.Name != "Twice" || got.Op.Operand != "" {
		t.Fatalf("Op = %+v, want Twice with no operand", got.Op)
	}
	if got.Body != "CardsInYourHand" {
		t.Errorf("Body = %q, want %q", got.Body, "CardsInYourHand")
	}
}

// doXMath matches the operator by containment, in its own order. NMinus has to
// be tested before Minus, or the operands come out the wrong way round.
func TestOperatorMatching(t *testing.T) {
	t.Parallel()

	for _, tt := range []struct {
		written string
		want    string
		ok      bool
	}{
		{"Plus", "Plus", true},
		{"NMinus", "NMinus", true},
		{"Minus", "Minus", true},
		{"LimitMax", "LimitMax", true},
		{"DivideEvenlyDown", "DivideEvenlyDown", true},
		{"Wibble", "", false},
	} {
		got, ok := expr.Operator(tt.written)
		if got != tt.want || ok != tt.ok {
			t.Errorf("Operator(%q) = %q, %v; want %q, %v", tt.written, got, ok, tt.want, tt.ok)
		}
	}
}

// A count head can take a space-separated argument, and for the Valid family
// that argument is a whole valid string.
func TestParseCount(t *testing.T) {
	t.Parallel()

	simple := expr.ParseCount("TypeInYourGraveyard.Creature")
	if simple.Head != "TypeInYourGraveyard" || len(simple.Parameters) != 1 || simple.Parameters[0] != "Creature" {
		t.Errorf("simple head = %+v, want TypeInYourGraveyard with one parameter", simple)
	}

	embedded := expr.ParseCount("ValidGraveyard Creature.YouOwn")
	if embedded.Head != "ValidGraveyard" {
		t.Errorf("head = %q, want ValidGraveyard", embedded.Head)
	}
	if got := len(embedded.Valid.Alternatives); got != 1 {
		t.Fatalf("parsed %d alternatives from the embedded valid string, want 1", got)
	}
	if base := embedded.Valid.Alternatives[0].Base.Name; base != "Creature" {
		t.Errorf("embedded base = %q, want Creature", base)
	}

	other := expr.ParseCount("Compare Y GE1")
	if other.Head != "Compare" || other.Argument != "Y GE1" {
		t.Errorf("Compare = %+v, want the argument kept as text", other)
	}
	if len(other.Valid.Alternatives) != 0 {
		t.Error("Compare's argument was read as a valid string")
	}

	spaced := expr.ParseCount(" 2")
	if spaced.Head != "2" {
		t.Errorf("head = %q, want 2 -- a leading space is not part of it", spaced.Head)
	}
}

// TestParseCountDistinctProperty proves a Valid family argument carrying a
// `$`-suffixed distinct-value operator (Tarmogoyf's own
// `ValidGraveyard Card$CardTypes`) splits Valid and DistinctProperty apart
// rather than feeding the whole `Card$CardTypes` string into valid.Parse as
// one base name -- xCount's own `paidparts = l[0].split("\\$", 2)` cuts on
// the first `$`, so the valid string is only `Card` (matches every object)
// and `CardTypes` is a separate operator (Count.DistinctProperty's own doc
// comment).
func TestParseCountDistinctProperty(t *testing.T) {
	t.Parallel()

	goyf := expr.ParseCount("ValidGraveyard Card$CardTypes")
	if goyf.Head != "ValidGraveyard" {
		t.Errorf("head = %q, want ValidGraveyard", goyf.Head)
	}
	if goyf.DistinctProperty != "CardTypes" {
		t.Errorf("DistinctProperty = %q, want CardTypes", goyf.DistinctProperty)
	}
	if got := len(goyf.Valid.Alternatives); got != 1 || goyf.Valid.Alternatives[0].Base.Name != "Card" {
		t.Errorf("Valid = %+v, want a single Card alternative -- the part of the argument before the $", goyf.Valid)
	}

	plain := expr.ParseCount("ValidGraveyard Creature.YouOwn")
	if plain.DistinctProperty != "" {
		t.Errorf("DistinctProperty = %q, want empty -- no $ in this argument", plain.DistinctProperty)
	}
}

// A Count expression carries its body already read by ParseCount, so an
// evaluator never parses script text at game time (PORT-2); every other head
// carries none.
func TestParseCarriesCompiledCount(t *testing.T) {
	t.Parallel()

	goyf := expr.Parse("Count$ValidGraveyard Card.OppOwn$CardTypes/Plus.1")
	if goyf.Count == nil {
		t.Fatal("Count = nil, want the body parsed at Parse time")
	}
	if goyf.Count.Head != "ValidGraveyard" || goyf.Count.DistinctProperty != "CardTypes" {
		t.Errorf("Count = %+v, want ValidGraveyard with DistinctProperty CardTypes", *goyf.Count)
	}
	if goyf.Op == nil || goyf.Op.Name != "Plus" || goyf.Op.Operand != "1" {
		t.Errorf("Op = %+v, want Plus.1 cut before the body is read", goyf.Op)
	}

	devotion := expr.Parse("Count$Devotion.Black")
	if devotion.Count == nil || devotion.Count.Head != "Devotion" || len(devotion.Count.Parameters) != 1 {
		t.Errorf("Count = %+v, want Devotion with one parameter", devotion.Count)
	}

	for _, amount := range []string{"SVar$X/Plus.1", "Number$0", "X", "3"} {
		if got := expr.Parse(amount); got.Count != nil {
			t.Errorf("Parse(%q).Count = %+v, want nil -- not a Count head", amount, *got.Count)
		}
	}
}

func TestEmpty(t *testing.T) {
	t.Parallel()

	for _, amount := range []string{"", " "} {
		if got := expr.Parse(amount); got.Kind != expr.Literal || got.Value != 0 {
			t.Errorf("Parse(%q) = %+v, want a zero literal", amount, got)
		}
	}
}

// A `>`-terminated prefix on the head switches which ability the measurement
// is taken against. adjustTriggerContext strips one and returns immediately, so
// a head carrying two keeps the second as part of its name.
func TestContextPrefix(t *testing.T) {
	t.Parallel()

	got := expr.Parse("CastSA>Returned$CardManaCost")
	if got.Context != "CastSA>" || got.Head != "Returned" {
		t.Errorf("context/head = %q/%q, want %q/%q", got.Context, got.Head, "CastSA>", "Returned")
	}
	if got.Body != "CardManaCost" {
		t.Errorf("body = %q, want CardManaCost", got.Body)
	}

	plain := expr.Parse("Count$CardsInYourHand")
	if plain.Context != "" || plain.Head != "Count" {
		t.Errorf("context/head = %q/%q, want no context and Count", plain.Context, plain.Head)
	}

	twice := expr.Parse("CastSA>Spawner>Count$X")
	if twice.Context != "CastSA>" || twice.Head != "Spawner>Count" {
		t.Errorf("context/head = %q/%q, want only the first stripped", twice.Context, twice.Head)
	}
}
