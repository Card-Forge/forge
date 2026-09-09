// The five card-script sub-grammars, as scanners rather than compilers: each
// splits a value into the tokens the vocabulary is counted in, and none of them
// interprets what a token means. Compiling these into a typed AST is the rest
// of M3.
//
// Grammars in docs/crucible/porting/dsl/. Java counterparts named per function.

package vocab

import "strings"

// Param is one `Key$ Value` entry of a param map.
type Param struct {
	Key   string
	Value string
}

// SplitParams reads a param map: `Key$ Value | Key$ Value`.
//
// Split on `|`, then on the *first* `$` only, which is what
// AbilityFactory.getMapParams does. Values routinely contain a second `$`: a
// SubAbility$ naming an SVar whose own body holds a Count$ is ordinary, and
// SpellDescription$ holds free text with any punctuation at all.
//
// A segment with no `$`, or whose key is not an identifier, is skipped rather
// than reported: free text after an unescaped `|` lands here, and it is not a
// param.
func SplitParams(value string) []Param {
	var out []Param
	for _, segment := range strings.Split(value, "|") {
		key, val, ok := strings.Cut(segment, "$")
		if !ok {
			continue
		}
		key = strings.TrimSpace(key)
		if !isIdentifier(key) {
			continue
		}
		out = append(out, Param{Key: key, Value: strings.TrimSpace(val)})
	}
	return out
}

// CostParts splits a cost string into its parts.
//
// Bracket-aware, because a `<...>` body's third field is display text and 12%
// of them contain spaces: `Sac<1/Creature.Other/another creature>` is one part,
// and strings.Fields would make it two. The `or` separating alternative
// payments is returned as its own part, as written.
func CostParts(cost string) []string {
	var (
		out   []string
		start = -1
		depth int
	)
	for i := 0; i < len(cost); i++ {
		switch cost[i] {
		case '<':
			depth++
		case '>':
			if depth > 0 {
				depth--
			}
		case ' ', '\t':
			if depth == 0 {
				if start >= 0 {
					out = append(out, cost[start:i])
					start = -1
				}
				continue
			}
		}
		if start < 0 {
			start = i
		}
	}
	if start >= 0 {
		out = append(out, cost[start:])
	}
	return out
}

// CostPartName is the part with its `<...>` body replaced by `<>`, which is the
// shape the vocabulary is counted in: `Sac<1/Creature.Other/another creature>`
// and `Sac<2/Land/two lands>` are one cost part, not two.
func CostPartName(part string) string {
	open := strings.IndexByte(part, '<')
	if open < 0 {
		return part
	}
	return part[:open] + "<>"
}

// CountExpr is one `Count$` expression: what is counted, and the arithmetic
// applied to it.
type CountExpr struct {
	// Head is the name alone, without the `.` parameters that follow it, and is
	// the literal "Valid" when the expression embeds a valid string.
	Head string
	// Valid is the embedded valid string, empty unless the head is one of the
	// Valid family.
	Valid string
	// Argument is a space-separated argument that is not a valid string:
	// `Compare Y GE1`, `xColorPaid B`.
	Argument string
	// Operators are the `/`-introduced suffixes, without their operands.
	Operators []string
}

const countMarker = "Count$"

// CountExprs finds every count expression in a value.
//
// A value can hold more than one, and a count expression is not a token: the
// `Valid` head is followed by a space and then a whole valid string, so a
// scanner that stops at whitespace loses the half that matters
// (docs/crucible/porting/dsl/04-count-expression-grammar.md).
//
// Java counterpart: AbilityUtils.calculateAmount and xCount.
func CountExprs(value string) []CountExpr {
	var out []CountExpr
	rest := value
	for {
		at := strings.Index(rest, countMarker)
		if at < 0 {
			return out
		}
		rest = rest[at+len(countMarker):]
		out = append(out, parseCount(rest))
	}
}

func parseCount(s string) CountExpr {
	// A leading space is written by 30 corpus values -- `Count$ 2`, `Count$ X`
	// -- and the head is what follows it.
	body, ops := cutOperators(strings.TrimLeft(s, " "))

	// A head can take a space-separated argument, and for the whole Valid
	// family -- Valid, ValidHand, ValidGraveyard, ValidLibrary and the rest --
	// that argument is a valid string. 172 heads in the corpus take one, so
	// this is a family rather than the single `Count$Valid ` case.
	if head, arg, ok := strings.Cut(body, " "); ok {
		if strings.HasPrefix(head, "Valid") {
			return CountExpr{Head: head, Valid: arg, Operators: ops}
		}
		return CountExpr{Head: head, Argument: arg, Operators: ops}
	}

	// A simple head carries `.` parameters -- TypeInYourGraveyard.Creature --
	// which belong to the head's own vocabulary, not to this one.
	head, _, _ := strings.Cut(body, ".")
	return CountExpr{Head: head, Operators: ops}
}

// cutOperators splits the head from its `/`-introduced operator chain, dropping
// each operator's `.operand`.
func cutOperators(s string) (head string, operators []string) {
	fields := strings.Split(s, "/")
	head = fields[0]
	for _, field := range fields[1:] {
		name, _, _ := strings.Cut(field, ".")
		if name != "" {
			operators = append(operators, name)
		}
	}
	return head, operators
}

// ValidAlt is one alternative of a valid string: a base and the properties
// conjoined to it.
type ValidAlt struct {
	Base       string
	Properties []string
}

// ParseValid reads a valid string: `Creature.Green+attacking,Land.YouCtrl`.
//
// `,` is OR and `+` is AND, and the property list does not distribute across
// alternatives. A `!` prefix stays on the property it negates, because Java's
// table holds `nonLand` as its own entry and the two negation forms are not
// interchangeable.
//
// Java counterpart: CardProperty.cardHasProperty and its callers.
func ParseValid(value string) []ValidAlt {
	var out []ValidAlt
	for _, alt := range strings.Split(value, ",") {
		alt = strings.TrimSpace(alt)
		if alt == "" {
			continue
		}
		base, rest, hasProps := strings.Cut(alt, ".")
		v := ValidAlt{Base: base}
		if hasProps {
			for _, property := range strings.Split(rest, "+") {
				if property != "" {
					v.Properties = append(v.Properties, property)
				}
			}
		}
		out = append(out, v)
	}
	return out
}

// Keyword is one `K:` line: a head and its positional arguments.
//
// The head can contain spaces -- `First Strike`, `Cumulative Upkeep` -- so it
// is what precedes the first colon, not the first word. Arguments are
// positional and heterogeneous: `Ward:2` is an amount and `Hexproof:White` is a
// colour, in the same shape.
type Keyword struct {
	Head string
	Args []string
}

// ParseKeyword reads a `K:` line's value.
func ParseKeyword(value string) Keyword {
	head, rest, ok := strings.Cut(value, ":")
	k := Keyword{Head: head}
	if ok {
		k.Args = strings.Split(rest, ":")
	}
	return k
}

// isIdentifier reports whether s is a param key: a letter, then letters,
// digits or underscores.
func isIdentifier(s string) bool {
	if s == "" {
		return false
	}
	for i := 0; i < len(s); i++ {
		c := s[i]
		switch {
		case c >= 'a' && c <= 'z', c >= 'A' && c <= 'Z':
		case i > 0 && (c >= '0' && c <= '9' || c == '_'):
		default:
			return false
		}
	}
	return true
}
