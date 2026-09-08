// Ported alongside crucible/oracle-java CardRulesDumper: the two write the same
// bytes for the same card, which is what the P1 gate diffs (ADR-0010).
// Deviations recorded in docs/crucible/porting/port-log/card-rules-reader.md.

package carddb

import (
	"sort"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/mana"
)

// DumpVersion is stamped on nothing, and exists so a change to the canonical
// form is a deliberate edit rather than a surprise diff. Bump it when a field
// is added, removed or reordered, and regenerate both goldens.
const DumpVersion = 1

// CanonicalJSON writes the card as one line of JSON.
//
// The format is hand-written rather than produced by encoding/json, because the
// Java side has to emit the same bytes and no two JSON libraries agree on
// escaping, key order and number formatting. Everything here is explicit: field
// order is the order below, strings escape only what JSON requires, and absent
// values are written as empty rather than omitted.
//
// Deliberately not dumped, because the two sides model them differently and the
// P1 gate is about rules parity rather than deck-builder metadata: DeckHints,
// DeckNeeds, DeckHas, the token list, and the derived integer power and
// toughness. Each is listed in the port log.
func (c *Card) CanonicalJSON() []byte {
	var b strings.Builder
	b.Grow(1024)

	b.WriteString(`{"file":`)
	writeJSONString(&b, c.Filename)
	b.WriteString(`,"split":`)
	writeJSONString(&b, c.SplitType.String())
	b.WriteString(`,"partnerWith":`)
	writeJSONString(&b, c.PartnerWith)
	b.WriteString(`,"meldWith":`)
	writeJSONString(&b, c.MeldWith)
	b.WriteString(`,"remAI":`)
	writeJSONBool(&b, c.RemovedFromAIDecks)
	b.WriteString(`,"remRandom":`)
	writeJSONBool(&b, c.RemovedFromRandomDecks)
	b.WriteString(`,"remNonCommander":`)
	writeJSONBool(&b, c.RemovedFromNonCommanderDecks)

	// Placeholder faces are still unfilled here, exactly as Forge's Reader
	// leaves them: resolution needs every card and happens in CardDb. Naming
	// them keeps the information in the diff.
	b.WriteString(`,"placeholders":[`)
	for i, name := range sortedPlaceholderNames(c.PlaceholderFaces) {
		if i > 0 {
			b.WriteByte(',')
		}
		writeJSONString(&b, name)
	}
	b.WriteString(`],"faces":[`)
	first := true
	for i := range c.Faces {
		if !c.Faces[i].Present {
			continue
		}
		if !first {
			b.WriteByte(',')
		}
		first = false
		c.Faces[i].writeCanonical(&b, i)
	}
	b.WriteString("]}")
	return []byte(b.String())
}

// dumpColors is the colour Forge ends up with: the declared override, or the
// mana cost's profile when a script declares none.
func (f *Face) dumpColors() mana.Colors {
	if f.HasColors {
		return f.Colors
	}
	return f.ManaCost.Colors()
}

func (f *Face) writeCanonical(b *strings.Builder, index int) {
	b.WriteString(`{"i":`)
	b.WriteString(strconv.Itoa(index))
	b.WriteString(`,"name":`)
	writeJSONString(b, f.Name)
	b.WriteString(`,"flavorName":`)
	writeJSONString(b, f.FlavorName)
	b.WriteString(`,"type":`)
	writeJSONString(b, f.Type.String())
	b.WriteString(`,"manaCost":`)
	writeJSONString(b, f.ManaCost.String())
	b.WriteString(`,"colors":`)
	// A colour mask, not a name: Java's ColorSet spells the same set "GW" where
	// this package spells it "WG", and a mask has no spelling to disagree on.
	//
	// Absent a Colors: line the colour is derived from the mana cost, which is
	// what CardFace does when it seals a face (CardFace.java:174). Keeping the
	// undeclared case as an empty mask here would make every coloured card
	// differ from the oracle.
	b.WriteString(strconv.Itoa(int(f.dumpColors())))
	b.WriteString(`,"power":`)
	writeJSONString(b, f.Power)
	b.WriteString(`,"toughness":`)
	writeJSONString(b, f.Toughness)
	b.WriteString(`,"loyalty":`)
	writeJSONString(b, f.InitialLoyalty)
	b.WriteString(`,"defense":`)
	writeJSONString(b, f.Defense)
	b.WriteString(`,"lights":`)
	writeJSONString(b, canonicalLights(f.AttractionLights))
	b.WriteString(`,"text":`)
	writeJSONString(b, f.NonAbilityText)
	b.WriteString(`,"oracle":`)
	writeJSONString(b, f.Oracle)

	writeJSONStrings(b, "abilities", f.Abilities)
	writeJSONStrings(b, "keywords", f.Keywords)
	writeJSONStrings(b, "triggers", f.Triggers)
	writeJSONStrings(b, "statics", f.Statics)
	writeJSONStrings(b, "replacements", f.Replacements)
	writeJSONStrings(b, "deckRules", f.DeckRules)
	writeJSONStrings(b, "draftActions", f.DraftActions)

	b.WriteString(`,"svars":[`)
	for i, name := range f.SVars.Names() {
		if i > 0 {
			b.WriteByte(',')
		}
		value, _ := f.SVars.Get(name)
		b.WriteByte('[')
		writeJSONString(b, name)
		b.WriteByte(',')
		writeJSONString(b, value)
		b.WriteByte(']')
	}
	b.WriteString(`],"variants":[`)
	for i, name := range sortedVariantNames(f.Variants) {
		if i > 0 {
			b.WriteByte(',')
		}
		writeJSONString(b, name)
	}
	b.WriteString("]}")
}

// canonicalLights normalises an attraction's lights. Java parses them into a
// set of integers, so the order a script wrote them in is not recoverable;
// sorting numerically is what both sides can agree on.
func canonicalLights(raw string) string {
	if raw == "" {
		return ""
	}
	fields := strings.Fields(raw)
	numbers := make([]int, 0, len(fields))
	for _, field := range fields {
		n, err := strconv.Atoi(field)
		if err != nil {
			// Not a light list at all. Return it unchanged so the diff shows
			// the real content rather than a swallowed parse failure.
			return raw
		}
		numbers = append(numbers, n)
	}
	sort.Ints(numbers)
	parts := make([]string, len(numbers))
	for i, n := range numbers {
		parts[i] = strconv.Itoa(n)
	}
	return strings.Join(parts, " ")
}

func sortedPlaceholderNames(placeholders map[int]string) []string {
	if len(placeholders) == 0 {
		return nil
	}
	out := make([]string, 0, len(placeholders))
	for _, name := range placeholders {
		out = append(out, name)
	}
	sort.Strings(out)
	return out
}

func sortedVariantNames(variants map[string]*Face) []string {
	if len(variants) == 0 {
		return nil
	}
	out := make([]string, 0, len(variants))
	for name := range variants {
		out = append(out, name)
	}
	sort.Strings(out)
	return out
}

func writeJSONStrings(b *strings.Builder, key string, values []string) {
	b.WriteString(`,"`)
	b.WriteString(key)
	b.WriteString(`":[`)
	for i, v := range values {
		if i > 0 {
			b.WriteByte(',')
		}
		writeJSONString(b, v)
	}
	b.WriteByte(']')
}

func writeJSONBool(b *strings.Builder, v bool) {
	if v {
		b.WriteString("true")
		return
	}
	b.WriteString("false")
}

// writeJSONString escapes exactly what JSON requires and nothing else: no
// \u escaping of non-ASCII, because both sides emit UTF-8, and no HTML
// escaping, which is encoding/json's own habit and not part of the format.
func writeJSONString(b *strings.Builder, s string) {
	b.WriteByte('"')
	for i := 0; i < len(s); i++ {
		switch c := s[i]; c {
		case '"':
			b.WriteString(`\"`)
		case '\\':
			b.WriteString(`\\`)
		case '\n':
			b.WriteString(`\n`)
		case '\r':
			b.WriteString(`\r`)
		case '\t':
			b.WriteString(`\t`)
		default:
			if c < 0x20 {
				b.WriteString(`\u00`)
				const hex = "0123456789abcdef"
				b.WriteByte(hex[c>>4])
				b.WriteByte(hex[c&0xf])
				continue
			}
			b.WriteByte(c)
		}
	}
	b.WriteByte('"')
}
