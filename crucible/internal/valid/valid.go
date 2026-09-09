// Package valid parses Forge's valid strings -- the predicates that decide
// which objects an ability may target, affect or count.
//
// `Creature.Green+attacking,Land.YouCtrl` is two alternatives joined by OR,
// each a base with properties joined by AND. This package builds that
// structure; deciding whether a given object matches needs a game and lands
// with the engine (ADR-0003).
//
// Parsing is deliberately as literal as Java's. Alternatives are split on `,`
// with no trimming, because CardTraitBase splits the param that way and a
// script that writes `Player, Planeswalker` really does produce an alternative
// no object can match. Tidying the input here would hide the bug rather than
// find it.
//
// Ported from forge-game/src/main/java/forge/game/card/CardProperty.java and
// the callers that split the param (CardTraitBase.java:261, CardLists.java).
// Deviations recorded in docs/crucible/porting/port-log/valid-strings.md.
package valid

import "strings"

// Spec is a parsed valid string: alternatives joined by OR.
type Spec struct {
	Alternatives []Alternative
}

// Alternative is one `Base.Property+Property` term. The properties are joined
// by AND, and they do not distribute across alternatives: `Instant.YouCtrl,
// Sorcery` is "(an instant you control) or (any sorcery)".
type Alternative struct {
	Base       Base
	Properties []Property
}

// Base is what an alternative selects before its properties narrow it: a card
// type, a supertype, a card name, or a player selector.
type Base struct {
	// Negated is a `!` prefix, which Java consumes before the lookup, so the
	// sign is not part of the name.
	Negated bool
	Name    string
}

// Property is one predicate applied to the base.
type Property struct {
	// Negated is a `!` prefix. It is not the same as a `non` prefix baked into
	// the name: `nonLand` is its own entry in Java's table, and only one of the
	// pair may exist for a given property.
	Negated bool
	// Name is the property as written, without the `!`.
	Name string
	// Compare is set when the name encodes a numeric comparison, so a caller
	// reads a field, an operator and an operand instead of re-parsing the
	// token. Nil otherwise.
	Compare *Compare
}

// Parse reads a valid string.
//
// It never fails. Java has no parse step to fail in: a base it does not
// recognise simply matches nothing, which is behaviour a port has to keep
// (GO-7 applies to scripts that cannot be represented, and every valid string
// can be).
func Parse(value string) Spec {
	if value == "" {
		return Spec{}
	}

	alternatives := strings.Split(value, ",")
	spec := Spec{Alternatives: make([]Alternative, 0, len(alternatives))}
	for _, alt := range alternatives {
		spec.Alternatives = append(spec.Alternatives, parseAlternative(alt))
	}
	return spec
}

func parseAlternative(text string) Alternative {
	// Only the first `.` separates the base from its properties, which is
	// Java's split("\\.", 2). A property may contain further dots.
	base, rest, hasProperties := strings.Cut(text, ".")

	alt := Alternative{Base: Base{Name: base}}
	if negated, ok := strings.CutPrefix(alt.Base.Name, "!"); ok {
		alt.Base.Negated, alt.Base.Name = true, negated
	}
	if !hasProperties {
		return alt
	}

	for _, name := range strings.Split(rest, "+") {
		property := Property{Name: name}
		if negated, ok := strings.CutPrefix(name, "!"); ok {
			property.Negated, property.Name = true, negated
		}
		if c, ok := parseCompare(property.Name); ok {
			property.Compare = &c
		}
		alt.Properties = append(alt.Properties, property)
	}
	return alt
}

// String writes the spec back in the form it was parsed from.
func (s Spec) String() string {
	parts := make([]string, len(s.Alternatives))
	for i, alt := range s.Alternatives {
		parts[i] = alt.String()
	}
	return strings.Join(parts, ",")
}

// String writes one alternative back.
func (a Alternative) String() string {
	var b strings.Builder
	b.WriteString(a.Base.String())
	for i, property := range a.Properties {
		if i == 0 {
			b.WriteByte('.')
		} else {
			b.WriteByte('+')
		}
		b.WriteString(property.String())
	}
	return b.String()
}

// String writes the base, sign included.
func (b Base) String() string {
	if b.Negated {
		return "!" + b.Name
	}
	return b.Name
}

// String writes the property, sign included.
func (p Property) String() string {
	if p.Negated {
		return "!" + p.Name
	}
	return p.Name
}
