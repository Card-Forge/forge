package carddb_test

import (
	"strings"
	"testing"
)

// The canonical form is a contract with crucible/oracle-java's CardRulesDumper:
// the two write the same bytes for the same card, and the P1 gate diffs them
// across the whole corpus. These cases pin the parts of it that are decisions
// rather than transcription.
func TestCanonicalJSON(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name   string
		script string
		want   string
	}{
		{
			name:   "a minimal card",
			script: "Name:Bolt\nManaCost:R\nTypes:Instant\nOracle:Deals 3 damage.\n",
			want: `{"file":"fixture","split":"None","partnerWith":"","meldWith":"","remAI":false,` +
				`"remRandom":false,"remNonCommander":false,"tokens":[],"placeholders":[],"faces":[` +
				`{"i":0,"name":"Bolt","flavorName":"","type":"Instant","manaCost":"{R}","colors":8,` +
				`"power":"","toughness":"","intPower":2147483647,"intToughness":2147483647,` +
				`"loyalty":"","defense":"","lights":"","text":"",` +
				`"oracle":"Deals 3 damage.","abilities":[],"keywords":[],"triggers":[],"statics":[],` +
				`"replacements":[],"deckRules":[],"draftActions":[],"svars":[],"variants":[]}]}`,
		},
		{
			name:   "colour is derived from the mana cost when no Colors line declares it",
			script: "Name:X\nManaCost:W U\nTypes:Instant\n",
			want:   `"colors":3,`,
		},
		{
			name:   "a declared Colors line wins over the mana cost",
			script: "Name:X\nManaCost:W U\nTypes:Instant\nColors:green\n",
			want:   `"colors":16,`,
		},
		{
			name:   "a face with no ManaCost line has no cost, which is not zero",
			script: "Name:X\nTypes:Land\n",
			want:   `"manaCost":"no cost",`,
		},
		{
			name:   "quotes and backslashes are escaped, and UTF-8 is not",
			script: "Name:X\nManaCost:R\nTypes:Instant\nOracle:say \"hi\"\\nand café\n",
			want:   `"oracle":"say \"hi\"\\nand café",`,
		},
		{
			name:   "lights are sorted numerically, because Java holds a set",
			script: "Name:X\nManaCost:2\nTypes:Artifact\nLights:6 4 5\n",
			want:   `"lights":"4 5 6",`,
		},
		{
			name:   "svars print in Java's case-insensitive sorted order",
			script: "Name:X\nManaCost:R\nTypes:Instant\nSVar:Zed:1\nSVar:apple:2\n",
			want:   `"svars":[["apple","2"],["Zed","1"]],`,
		},
		{
			name:   "variant names print sorted",
			script: "Name:X\nManaCost:2\nTypes:Artifact\nVariant:B:K:Two\nVariant:A:K:One\n",
			want:   `"variants":["A","B"]`,
		},
		{
			name:   "tokens are dumped on the card in script order, not sorted",
			script: "Name:X\nManaCost:R\nTypes:Instant\nA:SP$ Token | TokenScript$ zombie,soldier | Cost$ R\n",
			want:   `"tokens":["zombie","soldier"],`,
		},
		{
			name:   "a star power is dumped as the number it reduces to, next to the text it was written as",
			script: "Name:X\nManaCost:R\nTypes:Creature Elf\nPT:1+*/*\n",
			want:   `"power":"1+*","toughness":"*","intPower":1,"intToughness":0,`,
		},
		{
			name:   "a placeholder face is named and left unfilled, as Forge's reader leaves it",
			script: "Name:X\nManaCost:R\nTypes:Instant\nALTERNATE\nCopyFaceFrom:Other Card\n",
			want:   `"placeholders":["Other Card"],`,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			got := string(parse(t, tt.script).CanonicalJSON())
			if !strings.Contains(got, tt.want) {
				t.Errorf("CanonicalJSON() = %s\nwant it to contain %s", got, tt.want)
			}
		})
	}
}

// A control character has no shorthand escape and must not be emitted raw: the
// dump is line-oriented, so a stray newline would split one card into two.
func TestCanonicalJSONEscapesControlCharacters(t *testing.T) {
	t.Parallel()

	card := parse(t, "Name:X\nManaCost:R\nTypes:Instant\nOracle:before\x01after\n")
	got := string(card.CanonicalJSON())
	if !strings.Contains(got, `"oracle":"before\u0001after"`) {
		t.Errorf("CanonicalJSON() = %s, want the control character escaped", got)
	}
	if strings.Contains(got, "\n") {
		t.Error("CanonicalJSON() contains a raw newline, which would split the dump line")
	}
}

// Every present face appears, in index order, and absent ones do not.
func TestCanonicalJSONFaceOrder(t *testing.T) {
	t.Parallel()

	card := parse(t, "Name:Base\nManaCost:2\nTypes:Creature Human\nSPECIALIZE:GREEN\nName:Green\nALTERNATE\nName:Back\n")
	got := string(card.CanonicalJSON())
	for _, want := range []string{`"i":0`, `"i":1`, `"i":6`} {
		if !strings.Contains(got, want) {
			t.Errorf("CanonicalJSON() = %s, want a face %s", got, want)
		}
	}
	if strings.Index(got, `"i":1`) > strings.Index(got, `"i":6`) {
		t.Error("faces are not in index order")
	}
	if strings.Contains(got, `"i":2`) {
		t.Error("an absent face was written")
	}
}
