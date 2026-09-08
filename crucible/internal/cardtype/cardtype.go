// Package cardtype holds the type line: supertypes, core types, and subtypes.
//
// A card script writes its type line as one space-separated string, and the
// engine has to answer "is this a creature", "is this an Equipment", "does this
// share a creature type with that" millions of times per batch. So the string
// is parsed once, at load, into the value type here (PORT-2), and the answers
// become bit tests.
//
// Ported from forge-core/src/main/java/forge/card/CardType.java.
// Deviations recorded in docs/crucible/porting/port-log/card-type.md.
package cardtype

import "strings"

// Line is a parsed type line.
//
// The zero value is the empty type line, which is what a card gets while an
// effect is stripping its types. Supertypes and core types are sets and print
// in the fixed order the rules use; subtypes keep the order the card printed
// them in, because "Elf Warrior" and "Warrior Elf" are different type lines
// even though they mean the same thing.
type Line struct {
	supertypes uint8  // bit per Supertype
	coreTypes  uint16 // bit per CoreType
	subtypes   []string
}

// Separator is what a printed type line puts between core types and subtypes.
// Card scripts leave it out; Forge's own output and one card script include it.
const Separator = " - "

// Parse reads a type line: "Legendary Creature Elf Warrior".
//
// A "-" is an ordinary word and becomes a subtype, which is what Java does and
// therefore what the card database holds: `gandalf_shadows_foe` writes its type
// line in printed form, "Legendary Creature - Avatar Wizard", and Forge stores
// a literal "-" subtype for it. Parse(l.String()) is consequently not a fixed
// point for a line with subtypes, and matching the oracle is worth more than a
// property this parser was never required to have (PORT-7).
//
// Parse returns no error, because no type line can fail. Every word that is not
// a core type or a supertype becomes a subtype, including one the vocabulary has
// never heard of: Java's CardType.parse adds each word with add(), which never
// runs sanisfySubtypes, so Forge's own card database holds Contraption and
// Killbot exactly as the script wrote them.
//
// Dropping a subtype is something Java does only on the type-changing path
// (addAll, removeAll, getTypeWithChanges), which belongs to the layer system at
// M4. Doing it here would make 77 cards differ from the oracle.
//
// [UnknownTypes] reports the words the vocabulary does not contain, which is
// what the P2 vocabulary gate consumes.
//
// A nil registry is a programming error, not card data, so it panics (GO-7).
func Parse(reg *Registry, text string) Line {
	if reg == nil {
		panic("cardtype.Parse: nil registry")
	}

	var out Line
	var subtypes []string
	for _, word := range splitTypes(reg, strings.TrimSpace(text)) {
		if core, ok := CoreTypeFromName(word); ok {
			out.coreTypes |= 1 << uint(core)
			continue
		}
		if super, ok := SupertypeFromName(word); ok {
			out.supertypes |= 1 << uint(super)
			continue
		}
		subtypes = append(subtypes, word)
	}

	out.subtypes = subtypes
	return out
}

// UnknownTypes returns the words in a type line that are in no category of the
// vocabulary, in the order they appear.
//
// These are the words Forge silently discards: joke-set types such as Killbot
// and Clamfolk, and types newer than the vocabulary file, such as Omenpath.
// Reporting them is what turns a silent data gap into a reviewable list, and is
// the hook the P2 vocabulary gate will use.
func UnknownTypes(reg *Registry, text string) []string {
	if reg == nil {
		panic("cardtype.UnknownTypes: nil registry")
	}

	var out []string
	for _, word := range splitTypes(reg, strings.TrimSpace(text)) {
		if _, ok := CoreTypeFromName(word); ok {
			continue
		}
		if _, ok := SupertypeFromName(word); ok {
			continue
		}
		if !reg.known(word) {
			out = append(out, word)
		}
	}
	return out
}

// splitTypes breaks a type line into words, keeping multiword subtypes such as
// "Time Lord" and "Bolas's Meditation Realm" whole.
func splitTypes(reg *Registry, text string) []string {
	var out []string
	for i := 0; i < len(text); {
		if text[i] == ' ' {
			i++
			continue
		}
		if multi, ok := reg.multiwordPrefix(text[i:]); ok {
			out = append(out, multi)
			i += len(multi)
			continue
		}
		j := strings.IndexByte(text[i:], ' ')
		if j < 0 {
			out = append(out, text[i:])
			break
		}
		out = append(out, text[i:i+j])
		i += j
	}
	return out
}

// known reports whether a word appears in any subtype category.
func (r *Registry) known(name string) bool {
	for c := Category(0); int(c) < numCategories; c++ {
		if r.members[c][name] {
			return true
		}
	}
	return false
}

// Has reports whether the line carries a core type.
func (l Line) Has(t CoreType) bool { return l.coreTypes&(1<<uint(t)) != 0 }

// HasSupertype reports whether the line carries a supertype.
func (l Line) HasSupertype(t Supertype) bool { return l.supertypes&(1<<uint(t)) != 0 }

// HasSubtype reports whether the line carries a subtype, compared exactly as
// printed.
func (l Line) HasSubtype(name string) bool {
	for _, s := range l.subtypes {
		if s == name {
			return true
		}
	}
	return false
}

// IsPermanent reports whether a card with this type line stays on the
// battlefield. A card with several core types is a permanent if any of them is.
func (l Line) IsPermanent() bool {
	for t := CoreType(0); int(t) < numCoreTypes; t++ {
		if l.Has(t) && t.IsPermanent() {
			return true
		}
	}
	return false
}

// IsEmpty reports whether the line carries no types at all.
func (l Line) IsEmpty() bool {
	return l.coreTypes == 0 && l.supertypes == 0 && len(l.subtypes) == 0
}

// CoreTypes returns the core types in print order.
func (l Line) CoreTypes() []CoreType {
	var out []CoreType
	for t := CoreType(0); int(t) < numCoreTypes; t++ {
		if l.Has(t) {
			out = append(out, t)
		}
	}
	return out
}

// Supertypes returns the supertypes in print order.
func (l Line) Supertypes() []Supertype {
	var out []Supertype
	for t := Supertype(0); int(t) < numSupertypes; t++ {
		if l.HasSupertype(t) {
			out = append(out, t)
		}
	}
	return out
}

// Subtypes returns the subtypes in printed order. The slice aliases the line's
// storage; callers must not modify it.
func (l Line) Subtypes() []string { return l.subtypes }

// CreatureTypes returns the subtypes that are creature types, which is only
// meaningful on a creature or a Kindred card.
func (l Line) CreatureTypes(reg *Registry) []string {
	if !l.Has(Creature) && !l.Has(Kindred) {
		return nil
	}
	var out []string
	for _, s := range l.subtypes {
		if reg.IsCreatureType(s) {
			out = append(out, s)
		}
	}
	return out
}

// Equal reports whether two type lines are the same, subtype order included.
// Order is part of the identity here because it is part of what the card
// printed and what the oracle dump compares.
func (l Line) Equal(other Line) bool {
	if l.coreTypes != other.coreTypes || l.supertypes != other.supertypes {
		return false
	}
	if len(l.subtypes) != len(other.subtypes) {
		return false
	}
	for i, s := range l.subtypes {
		if s != other.subtypes[i] {
			return false
		}
	}
	return true
}

// String prints the line the way a card does: supertypes, then core types, then
// the separator and the subtypes.
func (l Line) String() string {
	var b strings.Builder
	for _, t := range l.Supertypes() {
		if b.Len() > 0 {
			b.WriteByte(' ')
		}
		b.WriteString(t.String())
	}
	for _, t := range l.CoreTypes() {
		if b.Len() > 0 {
			b.WriteByte(' ')
		}
		b.WriteString(t.String())
	}
	if len(l.subtypes) > 0 {
		b.WriteString(Separator)
		b.WriteString(strings.Join(l.subtypes, " "))
	}
	return b.String()
}
