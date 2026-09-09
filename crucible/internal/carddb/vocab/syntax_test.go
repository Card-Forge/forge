package vocab_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
)

func TestSplitParams(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name  string
		value string
		want  []string // "Key=Value"
	}{
		{
			name:  "a plain map",
			value: "DB$ Draw | Defined$ You | NumCards$ 1",
			want:  []string{"DB=Draw", "Defined=You", "NumCards=1"},
		},
		{
			name:  "the value keeps every $ after the first",
			value: "DB$ Pump | NumAtt$ Count$CardsInYourHand",
			want:  []string{"DB=Pump", "NumAtt=Count$CardsInYourHand"},
		},
		{
			name:  "free text with punctuation stays one value",
			value: `SP$ Effect | SpellDescription$ Draw a card. Then, if you have $5, win.`,
			want:  []string{"SP=Effect", `SpellDescription=Draw a card. Then, if you have $5, win.`},
		},
		{
			name:  "a segment with no $ is not a param",
			value: "DB$ Draw | nonsense | Defined$ You",
			want:  []string{"DB=Draw", "Defined=You"},
		},
		{
			name:  "a key that is not an identifier is not a param",
			value: "DB$ Draw | 2 damage$ no | Defined$ You",
			want:  []string{"DB=Draw", "Defined=You"},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			var got []string
			for _, p := range vocab.SplitParams(tt.value) {
				got = append(got, p.Key+"="+p.Value)
			}
			if diff := diffStrings(got, tt.want); diff != "" {
				t.Error(diff)
			}
		})
	}
}

// The finding that sizes the cost parser: 12% of `<...>` bodies contain a
// space, so splitting on whitespace produces halves of a cost part.
func TestCostPartsKeepBracketedBodiesWhole(t *testing.T) {
	t.Parallel()

	tests := []struct {
		cost string
		want []string
	}{
		{"2 R T", []string{"2", "R", "T"}},
		{"Sac<1/Creature.Other/another creature>", []string{"Sac<1/Creature.Other/another creature>"}},
		{
			"1 Sac<2/Blood.token/Blood token> Discard<1/Card.OppOwn/card an opponent owns>",
			[]string{"1", "Sac<2/Blood.token/Blood token>", "Discard<1/Card.OppOwn/card an opponent owns>"},
		},
		{"PayLife<3> or 2 U", []string{"PayLife<3>", "or", "2", "U"}},
		{"  T   Q  ", []string{"T", "Q"}},
	}

	for _, tt := range tests {
		t.Run(tt.cost, func(t *testing.T) {
			t.Parallel()

			if diff := diffStrings(vocab.CostParts(tt.cost), tt.want); diff != "" {
				t.Error(diff)
			}
		})
	}
}

func TestCostPartName(t *testing.T) {
	t.Parallel()

	for _, tt := range []struct{ part, want string }{
		{"T", "T"},
		{"Sac<1/Creature.Other/another creature>", "Sac<>"},
		{"Sac<2/Land/two lands>", "Sac<>"},
		{"PayLife<3>", "PayLife<>"},
	} {
		if got := vocab.CostPartName(tt.part); got != tt.want {
			t.Errorf("CostPartName(%q) = %q, want %q", tt.part, got, tt.want)
		}
	}
}

// A count expression is not a token: the Valid family takes a whole valid
// string after a space, and the arithmetic suffixes come after that.
func TestCountExprs(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name       string
		value      string
		head       string
		valid      string
		argument   string
		operators  []string
		exprsFound int
	}{
		{
			name:       "a simple head",
			value:      "Count$CardsInYourHand",
			head:       "CardsInYourHand",
			exprsFound: 1,
		},
		{
			name:       "a head with parameters keeps only its name",
			value:      "Count$TypeInYourGraveyard.Creature",
			head:       "TypeInYourGraveyard",
			exprsFound: 1,
		},
		{
			name:       "an embedded valid string with an operator after it",
			value:      "Count$Valid Creature.YouCtrl+powerGE1/LimitMax.1",
			head:       "Valid",
			valid:      "Creature.YouCtrl+powerGE1",
			operators:  []string{"LimitMax"},
			exprsFound: 1,
		},
		{
			name:       "the whole Valid family takes one, not just Valid itself",
			value:      "Count$ValidGraveyard Creature.YouOwn/Twice",
			head:       "ValidGraveyard",
			valid:      "Creature.YouOwn",
			operators:  []string{"Twice"},
			exprsFound: 1,
		},
		{
			name:       "a non-Valid head can still take an argument",
			value:      "Count$Compare Y GE1",
			head:       "Compare",
			argument:   "Y GE1",
			exprsFound: 1,
		},
		{
			name:       "a leading space is not part of the head",
			value:      "Count$ 2",
			head:       "2",
			exprsFound: 1,
		},
		{
			name:       "two expressions in one value",
			value:      "Count$CardsInYourHand Count$CardsInYourGraveyard",
			head:       "CardsInYourHand",
			argument:   "Count$CardsInYourGraveyard",
			exprsFound: 2,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			got := vocab.CountExprs(tt.value)
			if len(got) != tt.exprsFound {
				t.Fatalf("CountExprs(%q) found %d expressions, want %d: %+v", tt.value, len(got), tt.exprsFound, got)
			}
			if got[0].Head != tt.head {
				t.Errorf("Head = %q, want %q", got[0].Head, tt.head)
			}
			if got[0].Valid != tt.valid {
				t.Errorf("Valid = %q, want %q", got[0].Valid, tt.valid)
			}
			if got[0].Argument != tt.argument {
				t.Errorf("Argument = %q, want %q", got[0].Argument, tt.argument)
			}
			if diff := diffStrings(got[0].Operators, tt.operators); diff != "" {
				t.Errorf("Operators: %s", diff)
			}
		})
	}
}

// "," is OR and "+" is AND, and the property list does not distribute across
// alternatives -- which is the rule that decides thousands of targeting
// restrictions.
func TestParseValid(t *testing.T) {
	t.Parallel()

	got := vocab.ParseValid("Instant.YouCtrl+nonToken,Sorcery,Card.!token")
	want := []struct {
		base  string
		props []string
	}{
		{"Instant", []string{"YouCtrl", "nonToken"}},
		{"Sorcery", nil},
		{"Card", []string{"!token"}},
	}
	if len(got) != len(want) {
		t.Fatalf("ParseValid returned %d alternatives, want %d: %+v", len(got), len(want), got)
	}
	for i := range want {
		if got[i].Base != want[i].base {
			t.Errorf("alternative %d base = %q, want %q", i, got[i].Base, want[i].base)
		}
		if diff := diffStrings(got[i].Properties, want[i].props); diff != "" {
			t.Errorf("alternative %d properties: %s", i, diff)
		}
	}
}

// A head can contain spaces, so it is what precedes the first colon rather than
// the first word.
func TestParseKeyword(t *testing.T) {
	t.Parallel()

	for _, tt := range []struct {
		value string
		head  string
		args  []string
	}{
		{"Flying", "Flying", nil},
		{"First Strike", "First Strike", nil},
		{"Ward:2", "Ward", []string{"2"}},
		{"Dash:4 R W", "Dash", []string{"4 R W"}},
		{"Awaken:3:4 U", "Awaken", []string{"3", "4 U"}},
	} {
		got := vocab.ParseKeyword(tt.value)
		if got.Head != tt.head {
			t.Errorf("ParseKeyword(%q).Head = %q, want %q", tt.value, got.Head, tt.head)
		}
		if diff := diffStrings(got.Args, tt.args); diff != "" {
			t.Errorf("ParseKeyword(%q).Args: %s", tt.value, diff)
		}
	}
}

func diffStrings(got, want []string) string {
	if len(got) == len(want) {
		same := true
		for i := range got {
			if got[i] != want[i] {
				same = false
				break
			}
		}
		if same {
			return ""
		}
	}
	return "got [" + strings.Join(got, " ") + "], want [" + strings.Join(want, " ") + "]"
}
