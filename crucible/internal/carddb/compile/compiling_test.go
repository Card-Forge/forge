package compile_test

import (
	"errors"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/expr"
)

func compileScript(t *testing.T, script string) *compile.Card {
	t.Helper()

	card, err := carddb.ParseScript(testRegistry(t), "fixture", []byte(script))
	if err != nil {
		t.Fatalf("ParseScript failed: %v", err)
	}
	out, err := compile.Compile(card)
	if err != nil {
		t.Fatalf("Compile failed: %v", err)
	}
	return out
}

// A compiled card carries its own printed name. Nothing else it holds can
// answer "what card is this" -- the script string is gone by this stage, and
// Filename is a snake_case file stem, not a printed name -- so a caller that
// only has a *Card (fixture.Dump, a report) still needs it.
func TestCardCarriesItsName(t *testing.T) {
	t.Parallel()

	out := compileScript(t, "Name:Grizzly Bears\nManaCost:1 G\nTypes:Creature Bear\nPT:2/2\nOracle:\n")
	if out.Name != "Grizzly Bears" {
		t.Errorf("Name %q, want %q", out.Name, "Grizzly Bears")
	}
}

// The first key of a line decides what the line is, and the line it sits on
// decides only whether a `Mode$` is a trigger or a continuous effect.
func TestRecordTypes(t *testing.T) {
	t.Parallel()

	card := compileScript(t, "Name:X\nManaCost:R\nTypes:Instant\n"+
		"A:SP$ Draw | NumCards$ 1\n"+
		"A:AB$ Pump | Cost$ T | NumAtt$ 1\n"+
		"T:Mode$ Attacks | Execute$ TrigDraw | TriggerDescription$ x\n"+
		"S:Mode$ Continuous | Affected$ Creature | AddPower$ 1\n"+
		"R:Event$ Moved | ReplaceWith$ TrigDraw | Destination$ Graveyard\n"+
		"SVar:TrigDraw:DB$ Draw | NumCards$ 1\n")

	face := card.Faces[0]
	for _, tt := range []struct {
		got  *compile.Ability
		want compile.Record
		name string
	}{
		{face.Abilities[0], compile.Spell, "Draw"},
		{face.Abilities[1], compile.Activated, "Pump"},
		{face.Triggers[0], compile.Trigger, "Attacks"},
		{face.Statics[0], compile.StaticEffect, "Continuous"},
		{face.Replacements[0], compile.Replacement, "Moved"},
	} {
		if tt.got.Record != tt.want || tt.got.Name != tt.name {
			t.Errorf("got %s %q, want %s %q", tt.got.Record, tt.got.Name, tt.want, tt.name)
		}
	}
}

// A chain is resolved into direct references, however deep it goes: ADR-0007's
// whole point is that nothing looks a name up during a game.
func TestSubAbilityChainResolves(t *testing.T) {
	t.Parallel()

	card := compileScript(t, "Name:X\nManaCost:R\nTypes:Instant\n"+
		"A:SP$ Token | TokenScript$ x | RememberTokens$ True | SubAbility$ DBPump\n"+
		"SVar:DBPump:DB$ Pump | Defined$ Remembered | SubAbility$ DBCleanup\n"+
		"SVar:DBCleanup:DB$ Cleanup | ClearRemembered$ True\n")

	top := card.Faces[0].Abilities[0]
	if len(top.Subs) != 1 || top.Subs[0].SVar != "DBPump" {
		t.Fatalf("top-level subs = %+v, want one naming DBPump", top.Subs)
	}
	pump := top.Subs[0].Ability
	if pump.Record != compile.SubAbility || pump.Name != "Pump" {
		t.Errorf("DBPump = %s %q, want SubAbility \"Pump\"", pump.Record, pump.Name)
	}
	if len(pump.Subs) != 1 || pump.Subs[0].Ability.Name != "Cleanup" {
		t.Fatalf("DBPump subs = %+v, want one Cleanup", pump.Subs)
	}
	if got := pump.Subs[0].Ability.SVar; got != "DBCleanup" {
		t.Errorf("SVar = %q, want %q -- the name is kept for diagnostics", got, "DBCleanup")
	}
}

// A trigger reaches its ability through Execute$, which is one of Java's
// additional-ability keys rather than a special case.
func TestTriggerExecuteResolves(t *testing.T) {
	t.Parallel()

	card := compileScript(t, "Name:X\nManaCost:R\nTypes:Creature Elf\nPT:1/1\n"+
		"T:Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | Execute$ TrigDraw | TriggerDescription$ x\n"+
		"SVar:TrigDraw:DB$ Draw | NumCards$ 1\n")

	trigger := card.Faces[0].Triggers[0]
	if len(trigger.Subs) != 1 || trigger.Subs[0].Key != "Execute" || trigger.Subs[0].Ability.Name != "Draw" {
		t.Errorf("trigger subs = %+v, want one Execute naming Draw", trigger.Subs)
	}
}

// An SVar a param names as a plain value, not an ability -- AddPower$ X, the
// real corpus shape a characteristic-defining or anthem-shaped Mode$
// Continuous line writes -- is parsed as an amount expression
// (internal/expr) rather than left as inert text, keyed by name folded to
// lower case (Face.Amounts' own doc comment).
func TestAmountsParsesNonAbilitySVars(t *testing.T) {
	t.Parallel()

	card := compileScript(t, "Name:X\nManaCost:1 G\nTypes:Creature Elf\nPT:0/0\n"+
		"S:Mode$ Continuous | CharacteristicDefining$ True | SetPower$ X | SetToughness$ X\n"+
		"SVar:X:Count$Valid Elf\n")

	amt, ok := card.Faces[0].Amounts["x"]
	if !ok {
		t.Fatalf("Amounts[%q] missing, want an entry -- SVar:X is not an ability", "x")
	}
	if amt.Kind != expr.Expression || amt.Head != "Count" || amt.Body != "Valid Elf" {
		t.Errorf("Amounts[%q] = %+v, want Kind=Expression Head=Count Body=%q", "x", amt, "Valid Elf")
	}
}

// A PlayerCount amount's life<op> operand written as an expression (Great
// Unclean One's `HasPropertylifeLTCount$YourLifeTotal`) is compiled too, keyed
// by its own folded text; a number or SVar operand, or another property, adds
// nothing.
func TestAmountsCompileALifePropertyExpressionOperand(t *testing.T) {
	t.Parallel()

	card := compileScript(t, "Name:X\nManaCost:R\nTypes:Instant\nA:SP$ Draw | NumCards$ X\n"+
		"SVar:X:PlayerCountOpponents$HasPropertylifeLTCount$YourLifeTotal\n"+
		"SVar:Y:PlayerCountOpponents$HasPropertylifeLTZ\n"+
		"SVar:Z:PlayerCountOpponents$HasPropertyLostLifeThisTurn\n")
	amounts := card.Faces[0].Amounts
	amt, ok := amounts["count$yourlifetotal"]
	if !ok || amt.Kind != expr.Expression || amt.Head != "Count" {
		t.Errorf("Amounts[%q] = %+v, %v, want the compiled Count$ expression", "count$yourlifetotal", amt, ok)
	}
	if len(amounts) != 4 {
		t.Errorf("Amounts has %d entries, want 4: X, Y, Z and X's operand", len(amounts))
	}
}

// An SVar that IS an ability -- referenced through Execute$/SubAbility$ and
// compiled into a Sub above -- is not duplicated into Amounts: its own body
// starts with a recognized record key (DB$ here), not a Count$/Number$/
// SVar$ amount shape.
func TestAmountsSkipsAbilityShapedSVars(t *testing.T) {
	t.Parallel()

	card := compileScript(t, "Name:X\nManaCost:R\nTypes:Instant\n"+
		"A:SP$ Pump | SubAbility$ DBCleanup\n"+
		"SVar:DBCleanup:DB$ Cleanup | ClearRemembered$ True\n")

	if _, ok := card.Faces[0].Amounts["dbcleanup"]; ok {
		t.Errorf("Amounts[%q] present, want absent -- DBCleanup is an ability, already reached through Subs", "dbcleanup")
	}
}

// Java reads a param map into a TreeMap with CASE_INSENSITIVE_ORDER, so a
// repeated key keeps its last value and one spelled differently is the same
// key. Twenty-nine corpus lines repeat a key and one writes SubABility$.
func TestParamMapSemantics(t *testing.T) {
	t.Parallel()

	card := compileScript(t, "Name:X\nManaCost:R\nTypes:Instant\n"+
		"A:SP$ Draw | NumCards$ 1 | SubAbility$ DBMissing | SubAbility$ DBReal\n"+
		"A:SP$ Draw | NumCards$ 1 | SubABility$ DBReal\n"+
		"SVar:DBReal:DB$ Cleanup | ClearRemembered$ True\n")

	repeated := card.Faces[0].Abilities[0]
	if len(repeated.Subs) != 1 || repeated.Subs[0].SVar != "DBReal" {
		t.Errorf("subs = %+v, want only the last SubAbility$ to count", repeated.Subs)
	}
	if got, _ := repeated.Param("subability"); got != "DBReal" {
		t.Errorf("Param(subability) = %q, want %q -- lookup ignores case", got, "DBReal")
	}

	misspelled := card.Faces[0].Abilities[1]
	if len(misspelled.Subs) != 1 || misspelled.Subs[0].SVar != "DBReal" {
		t.Errorf("subs = %+v, want SubABility$ to resolve like SubAbility$", misspelled.Subs)
	}
}

// Choices$ names sub-abilities only for the APIs that read it as a list. On any
// other API it is a valid string, and resolving it would fail on correct cards.
func TestChoicesOnlyForTheAPIsThatReadThem(t *testing.T) {
	t.Parallel()

	card := compileScript(t, "Name:X\nManaCost:R\nTypes:Instant\n"+
		"A:SP$ Charm | Choices$ DBOne,DBTwo\n"+
		"A:SP$ ChooseCard | Choices$ Creature.YouCtrl\n"+
		"A:SP$ RollDice | ResultSubAbilities$ 1:DBOne,2:DBTwo\n"+
		"SVar:DBOne:DB$ Draw | NumCards$ 1\n"+
		"SVar:DBTwo:DB$ Draw | NumCards$ 2\n")

	if got := len(card.Faces[0].Abilities[0].Subs); got != 2 {
		t.Errorf("Charm resolved %d choices, want 2", got)
	}
	if got := len(card.Faces[0].Abilities[1].Subs); got != 0 {
		t.Errorf("ChooseCard resolved %d choices, want 0 -- its Choices$ is a valid string", got)
	}
	if got := len(card.Faces[0].Abilities[2].Subs); got != 2 {
		t.Errorf("RollDice resolved %d result abilities, want 2", got)
	}
}

// Both failures name the card and the reference, because the fix is always an
// edit to one script (GO-7).
func TestBrokenChains(t *testing.T) {
	t.Parallel()

	for _, tt := range []struct {
		name   string
		script string
		want   error
	}{
		{
			name:   "a reference to an SVar that does not exist",
			script: "A:SP$ Draw | NumCards$ 1 | SubAbility$ DBNope\n",
			want:   compile.ErrMissingSVar,
		},
		{
			name:   "a chain that returns to itself",
			script: "A:SP$ Draw | SubAbility$ DBLoop\nSVar:DBLoop:DB$ Pump | SubAbility$ DBLoop\n",
			want:   compile.ErrCycle,
		},
		{
			name: "a longer cycle",
			script: "A:SP$ Draw | SubAbility$ DBOne\n" +
				"SVar:DBOne:DB$ Pump | SubAbility$ DBTwo\nSVar:DBTwo:DB$ Pump | SubAbility$ DBOne\n",
			want: compile.ErrCycle,
		},
		{
			name:   "a line whose first key is not a record",
			script: "A:Wibble$ Draw | NumCards$ 1\n",
			want:   compile.ErrNoRecord,
		},
	} {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			card, err := carddb.ParseScript(testRegistry(t), "fixture",
				[]byte("Name:X\nManaCost:R\nTypes:Instant\n"+tt.script))
			if err != nil {
				t.Fatalf("ParseScript failed: %v", err)
			}
			out, err := compile.Compile(card)
			if err == nil {
				t.Fatalf("Compile accepted the script, got %+v", out)
			}
			if !errors.Is(err, tt.want) {
				t.Errorf("error = %v, want one wrapping %v", err, tt.want)
			}
			if !strings.Contains(err.Error(), "fixture") {
				t.Errorf("error = %v, want the card named", err)
			}
		})
	}
}

// One SVar reached twice by different chains is not a cycle: only a chain that
// revisits a name it already holds is.
func TestSharedSubAbilityIsNotACycle(t *testing.T) {
	t.Parallel()

	card := compileScript(t, "Name:X\nManaCost:R\nTypes:Instant\n"+
		"A:SP$ Draw | SubAbility$ DBOne\n"+
		"SVar:DBOne:DB$ Pump | SubAbility$ DBShared\n"+
		"A:SP$ Draw | SubAbility$ DBShared\n"+
		"SVar:DBShared:DB$ Cleanup | ClearRemembered$ True\n")

	if got := len(card.Faces[0].Abilities); got != 2 {
		t.Fatalf("compiled %d abilities, want 2", got)
	}
}

// Every face compiles, not only the first: an ALTERNATE face has its own SVar
// namespace and its own chains.
func TestEveryFaceCompiles(t *testing.T) {
	t.Parallel()

	card := compileScript(t, "Name:Front\nManaCost:R\nTypes:Creature Elf\nPT:1/1\n"+
		"A:SP$ Draw | SubAbility$ DBFront\nSVar:DBFront:DB$ Cleanup | ClearRemembered$ True\n"+
		"AlternateMode:DoubleFaced\nALTERNATE\n"+
		"Name:Back\nTypes:Creature Elf\nPT:2/2\n"+
		"A:SP$ Draw | SubAbility$ DBBack\nSVar:DBBack:DB$ Cleanup | ClearRemembered$ True\n")

	if got := len(card.Faces[carddb.FacePrimary].Abilities); got != 1 {
		t.Errorf("front face compiled %d abilities, want 1", got)
	}
	back := card.Faces[carddb.FaceAlternate].Abilities
	if len(back) != 1 || len(back[0].Subs) != 1 || back[0].Subs[0].SVar != "DBBack" {
		t.Errorf("back face = %+v, want one ability resolving DBBack", back)
	}
}
