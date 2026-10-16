package keyword_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/keyword"
)

func TestExpand(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		line     string
		ok       bool
		contains string // a fragment of the first expanded line
	}{
		{"Equip:1", true, "AB$ Attach | Cost$ 1 | ValidTgts$ Creature.YouCtrl | TgtPrompt$ Select target creature you control | SorcerySpeed$ True"},
		{"Equip:3:Creature.Legendary+YouCtrl:legendary creature", true, "ValidTgts$ Creature.Legendary+YouCtrl | TgtPrompt$ Select target legendary creature you control"},
		{"Equip:0:::ActivationLimit$ 1:Activate only once each turn", true, "| ActivationLimit$ 1"},
		{"Equip:2:Flavor Spirit of the Whalaqee", true, "Cost$ 2 |"},
		{"Equip:3:::ReduceCost$ X:This ability costs {X} less", false, ""},
		{"Equip:3:::AlternateCost$ Discard<1/Card>", false, ""},
		{"Equip", false, ""},
		{"Cycling:2", true, "AB$ Draw | Cost$ 2 Discard<1/CARDNAME> | ActivationZone$ Hand"},
		{"Cycling", false, ""},
		{"Prowess", true, "Mode$ SpellCast | ValidCard$ Card.nonCreature"},
		{"Exalted", true, "Mode$ Attacks | ValidCard$ Creature.YouCtrl | Alone$ True"},
		{"Prowess:1", false, ""},
		{"Annihilator:2", true, "Mode$ Attacks | ValidCard$ Card.Self"},
		{"Annihilator", false, ""},
		{"Bushido:1", true, "Mode$ Blocks | ValidCard$ Card.Self"},
		{"Bushido:x", false, ""},
		{"Afterlife:2", true, "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard"},
		{"TypeCycling:Plains:2", true, "Cost$ 2 Discard<1/CARDNAME> | ActivationZone$ Hand | Origin$ Library | Destination$ Hand | ChangeType$ Plains"},
		{"TypeCycling:Basic", false, ""},
		{"Crew:2", true, "AB$ Animate | Cost$ tapXType<Any/Creature.Other+withTotalPowerGE2> | Defined$ Self | Types$ Artifact,Creature"},
		{"Crew", false, ""},
		{"Affinity:Artifact", true, ""},
		{"Affinity", false, ""},
		{"Persist", true, "counters_EQ0_M1M1"},
		{"Undying", true, "counters_EQ0_P1P1"},
		{"Undying:1", false, ""},
		{"Battle cry", true, "Mode$ Attacks | ValidCard$ Card.Self"},
		{"Battle cry:1", false, ""},
		{"Dethrone", true, "Attacked$ Player.withMostLife"},
		{"Flanking", true, "ValidBlocker$ Creature.withoutFlanking"},
		{"Afflict:2", true, "Mode$ AttackerBlocked | ValidCard$ Card.Self"},
		{"Afflict", false, ""},
		{"Soulshift:3", true, "OptionalDecider$ You"},
		{"Soulbond", true, "IsPresent$ Creature.Other+YouCtrl+!Paired"},
		{"Soulbond:1", false, ""},
		{"Soulshift:x", false, ""},
		{"Mentor", true, "Mode$ Attacks | ValidCard$ Card.Self"},
		{"Training", true, "IsPresent$ Creature.attacking+Other+powerGTKWPower"},
		{"Evolve", true, "Condition$ Evolve"},
		{"Chapter:3:A,B,C", true, "Chapter$ 1 | CounterType$ LORE | CounterAmount$ EQ1 | Execute$ A"},
		{"Chapter:3:A,B", false, ""},
		{"Chapter:x:A", false, ""},
		{"Chapter:2:A,", false, ""},
		{"Chapter", false, ""},
		{"Modular:1", true, "OptionalDecider$ You"},
		{"Modular:Sunburst", false, ""},
		{"Modular", false, ""},
		{"Renown:2", true, "IsPresent$ Card.Self+!IsRenowned"},
		{"Renown", false, ""},
		{"Dethrone:1", false, ""},
		{"Flanking:1", false, ""},
		{"Mentor:1", false, ""},
		{"Training:1", false, ""},
		{"Evolve:1", false, ""},
		{"Extort:1", false, ""},
		{"Fabricate:2", true, "Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self"},
		{"Fabricate", false, ""},
		{"Extort", true, "Mode$ SpellCast | ValidActivatingPlayer$ You"},
		{"Echo:1 R", true, "IsPresent$ Card.Self+cameUnderControlSinceLastUpkeep"},
		{"Echo", false, ""},
		{"Cumulative upkeep:PayLife<1>", true, "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You"},
		{"Cumulative upkeep", false, ""},
		{"Cascade", true, "Mode$ SpellCast | ValidCard$ Card.Self | TriggerZones$ Stack"},
		{"Cascade:1", false, ""},
		{"Storm", true, "Mode$ SpellCast | ValidCard$ Card.Self | TriggerZones$ Stack"},
		{"Storm:1", false, ""},
		{"Exploit", true, "Mode$ ChangesZone | ValidCard$ Card.Self | Destination$ Battlefield"},
		{"Exploit:1", false, ""},
		{"Flying", false, ""},
	} {
		t.Run(tc.line, func(t *testing.T) {
			t.Parallel()

			exp, ok := keyword.Expand(keyword.Parse(tc.line))
			if ok != tc.ok {
				t.Fatalf("Expand ok = %v, want %v", ok, tc.ok)
			}
			if !ok {
				return
			}
			lines := append(append(append([]string(nil), exp.Abilities...), exp.Triggers...), exp.Statics...)
			if len(lines) == 0 || !strings.Contains(lines[0], tc.contains) {
				t.Errorf("first line %q does not contain %q", lines, tc.contains)
			}
		})
	}
}

// Devour's type and multiplier land in the sacrifice and the counter amount
// (CardFactoryUtil.java:2211-2238).
func TestExpandDevourTypeAndMultiplier(t *testing.T) {
	t.Parallel()

	exp, ok := keyword.Expand(keyword.Parse("Devour:3:Land"))
	if !ok {
		t.Fatal("Devour:3:Land did not expand")
	}
	if len(exp.SVars) != 3 || !strings.Contains(exp.SVars[0].Value, "SacValid$ Land.Other") ||
		!strings.Contains(exp.SVars[0].Value, "Optional$ True") {
		t.Errorf("SVars %v: want a Sacrifice of Land.Other, optional", exp.SVars)
	}
	want := map[string]string{
		"KWDevourSacX{n}": "Count$Valid Land.YouCtrl+Other",
		"KWDevourX{n}":    "Count$RememberedSize/Times.3",
	}
	for _, a := range exp.Amounts {
		if want[a.Name] != a.Value {
			t.Errorf("amount %s = %q, want %q", a.Name, a.Value, want[a.Name])
		}
	}
	if len(exp.Amounts) != len(want) {
		t.Errorf("amounts %v, want %v", exp.Amounts, want)
	}
}

func TestExpandReplacements(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		line string
		ok   bool
		want string // the replacement line
	}{
		{"etbCounter:P1P1:2", true, "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | Secondary$ True | ReplacementResult$ Updated | ReplaceWith$ KWEtbCounter{n}"},
		{"etbCounter:P1P1:X:no Condition:CARDNAME enters with X counters.", true, "ReplaceWith$ KWEtbCounter{n}"},
		{"etbCounter:P1P1:X:CheckSVar$ WasKicked:desc", true, "ReplaceWith$ KWEtbCounter{n} | CheckSVar$ WasKicked"},
		{"etbCounter:EACH :1", false, ""},
		{"Bloodthirst:2", true, "Bloodthirst$ True"},
		{"Bloodthirst:X", true, "ReplaceWith$ KWBloodthirst{n} | Bloodthirst$ True"},
		{"Bloodthirst:y", false, ""},
		{"Bloodthirst", false, ""},
		{"etbCounter:P1P1", false, ""},
		{"Devour:2", true, "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | ReplacementResult$ Updated | ReplaceWith$ KWDevour{n}"},
		{"Devour:3:Land", true, "ReplaceWith$ KWDevour{n}"},
		{"Devour:X::, where X is the number of creatures devoured this way", true, "ReplaceWith$ KWDevour{n}"},
		{"Devour:y", false, ""},
		{"Devour", false, ""},
	} {
		t.Run(tc.line, func(t *testing.T) {
			t.Parallel()

			exp, ok := keyword.Expand(keyword.Parse(tc.line))
			if ok != tc.ok {
				t.Fatalf("Expand ok = %v, want %v", ok, tc.ok)
			}
			if !ok {
				return
			}
			if len(exp.Replacements) != 1 || !strings.Contains(exp.Replacements[0], tc.want) {
				t.Errorf("replacements %q do not contain %q", exp.Replacements, tc.want)
			}
		})
	}
}
