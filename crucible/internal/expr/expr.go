// Package expr parses Forge's amount expressions -- the numbers an ability
// computes rather than states.
//
// `NumDmg$ 3` is a literal, `NumDmg$ X` names an SVar, and
// `SVar:X:Count$CardsInYourHand/Twice` is an expression with an operator. All
// three arrive through the same param, so all three are one type here.
//
// Parsing only. Every head ultimately measures something about a game, so
// evaluation lands with the engine (ADR-0003).
//
// Ported from forge-game/src/main/java/forge/game/ability/AbilityUtils.java
// (calculateAmount, doXMath) and forge/game/card/CardFactoryUtil.java
// (extractOperators).
// Deviations recorded in docs/crucible/porting/port-log/count-expressions.md.
package expr

import (
	"strconv"
	"strings"
)

// Kind is what an amount turns out to be.
type Kind uint8

// The three forms calculateAmount distinguishes, in the order it tests them.
const (
	// Literal is a plain number, sign included: `3`, `+3`, `-1`.
	Literal Kind = iota
	// Reference names an SVar to look up. `X` is the common one.
	Reference
	// Expression is a `Head$Body` measurement, optionally with an operator.
	Expression
)

var kindNames = [...]string{Literal: "Literal", Reference: "Reference", Expression: "Expression"}

// String returns the kind's name.
func (k Kind) String() string { return kindNames[k] }

// Amount is a parsed amount expression.
type Amount struct {
	Kind Kind

	// Value is the number, when the amount is a [Literal]. The sign is already
	// applied. It is also a `Number$N` expression's own N when Numeric is set;
	// a `-` written before `Number$` stays in Negative, as for any expression.
	Value int
	// Numeric reports a `Number$` expression whose body is an integer
	// (xCount's own Integer.parseInt), read into Value at load so the
	// evaluator never converts script text (PORT-2).
	Numeric bool

	// Name is the SVar named by a [Reference].
	Name string

	// Head is the object an [Expression] measures: `Count`, `Number`, `SVar`,
	// `Remembered`, `PlayerCountOpponents`, and eighty more. A context prefix,
	// if any, has been removed.
	Head string
	// Context is a `>`-terminated prefix that switches which ability the
	// measurement is taken against, empty when there is none.
	Context string
	// Body is everything after the first `$`, with the operator removed.
	Body string
	// Op is the arithmetic applied to the result, nil when there is none.
	Op *Op
	// Count is Body already read by [ParseCount], set when Head is `Count`
	// (case-folded, the engine's own dispatch) and nil otherwise. Parsing it
	// here rather than in the evaluator is what keeps a compiled card's
	// amounts free of runtime re-parsing (PORT-2): Parse runs once per SVar
	// at card load. Shared and read-only once built, like the rest of a
	// compiled card.
	Count *Count

	// Negative records a `-` prefix on an expression or a reference, which
	// Java strips before it does anything else and applies as a multiplier at
	// the end.
	Negative bool

	// Text is the amount exactly as written.
	Text string
}

// Op is the arithmetic suffix: `/Twice`, `/Plus.2`, `/LimitMax.X`.
type Op struct {
	// Name is the operator as written. Java matches it by containment, so the
	// written form is what a caller must keep.
	Name string
	// Operand is the value after the `.`, empty for the operators that take
	// none. A number or an SVar name; resolving the latter needs a game.
	Operand string
	// Value is Operand as an integer when Numeric is set: doXMath's own
	// Integer.parseInt(s[1]), read at load (PORT-2). An operand that is not
	// an integer is an SVar name the evaluator looks up instead.
	Value   int
	Numeric bool
}

// Parse reads an amount.
//
// It never fails, because Java's calculateAmount never does: a name it cannot
// resolve prints to stderr and yields zero. What can be told apart statically
// is told apart here, and the rest is left as text for the evaluator.
func Parse(amount string) Amount {
	out := Amount{Text: amount}
	if strings.TrimSpace(amount) == "" {
		return out
	}

	// The sign is stripped first and applied at the end as a multiplier, so
	// `-Count$...` is the negation of the whole expression rather than part of
	// its head.
	body := amount
	switch body[0] {
	case '+':
		body = body[1:]
	case '-':
		body, out.Negative = body[1:], true
	}

	if n, err := strconv.Atoi(body); err == nil && isDigits(body) {
		out.Kind, out.Value = Literal, n
		if out.Negative {
			out.Value = -n
		}
		return out
	}

	// A `$` makes it a raw expression rather than an SVar name -- but only
	// past the first character, because Java tests indexOf('$') > 0. A value
	// starting with `$` is looked up as a name and will not be found.
	if at := strings.Index(body, "$"); at > 0 {
		out.Kind = Expression
		out.Context, out.Head = cutContext(body[:at])
		out.Body, out.Op = cutOperator(body[at+1:])
		switch {
		case strings.EqualFold(out.Head, "Count"):
			count := ParseCount(out.Body)
			out.Count = &count
		case out.Head == "Number":
			if n, err := strconv.Atoi(out.Body); err == nil {
				out.Value, out.Numeric = n, true
			}
		}
		return out
	}

	out.Kind, out.Name = Reference, body
	return out
}

// cutOperator splits the arithmetic suffix from the body.
//
// Java takes `l[1]` from a split on `/` and nothing else, so **only the first
// operator applies** and a second is silently dropped
// (CardFactoryUtil.extractOperators). No corpus expression writes two, so the
// limit is reproduced rather than fixed: it is unobservable today, and a port
// that quietly evaluates more than Forge does is a port that disagrees with
// the oracle (PORT-7).
func cutOperator(body string) (string, *Op) {
	head, rest, ok := strings.Cut(body, "/")
	if !ok {
		return body, nil
	}

	// The operator itself ends at the next `/`, which is where Java stops.
	operator, _, _ := strings.Cut(rest, "/")
	name, operand, _ := strings.Cut(operator, ".")
	if name == "" {
		return head, nil
	}
	op := &Op{Name: name, Operand: operand}
	if n, err := strconv.Atoi(operand); err == nil {
		op.Value, op.Numeric = n, true
	}
	return head, op
}

// Contexts are the head prefixes that change which ability the measurement is
// taken against, from AbilityUtils.adjustTriggerContext. `CastSA>Returned` is
// what the spell that cast this card returned, not what this ability did.
var Contexts = []string{"Spawner>", "TriggeredSpellAbility>", "CastSA>"}

// cutContext removes a context prefix from a head.
//
// At most one: each branch of adjustTriggerContext returns as soon as it
// strips, so a head carrying two keeps the second as part of its name.
func cutContext(head string) (context, rest string) {
	for _, prefix := range Contexts {
		if after, ok := strings.CutPrefix(head, prefix); ok {
			return prefix, after
		}
	}
	return "", head
}

// Operators are doXMath's, in the order it tests them. The order is the rule:
// the test is containment, so `NMinus` has to be checked before `Minus` or
// every `NMinus` would be read as a subtraction with its operands the wrong way
// round.
var Operators = []string{
	"Plus", "NMinus", "Minus", "Twice", "Thrice", "HalfUp", "HalfDown",
	"ThirdUp", "ThirdDown", "Negative", "Times", "Pow",
	"DivideEvenlyUp", "DivideEvenlyDown", "Mod", "Abs", "LimitMax", "LimitMin",
}

// Operator returns the operator doXMath would apply to a written name, and
// whether any matches. A name matching none leaves the number unchanged, which
// is doXMath's final else.
func Operator(written string) (string, bool) {
	for _, operator := range Operators {
		if strings.Contains(written, operator) {
			return operator, true
		}
	}
	return "", false
}

// String writes the amount back as it was read.
func (a Amount) String() string { return a.Text }

// isDigits reports whether every byte is a digit, which is what
// StringUtils.isNumeric tests -- it rejects a sign, and the sign is already
// gone by the time this is asked.
func isDigits(s string) bool {
	if s == "" {
		return false
	}
	for i := 0; i < len(s); i++ {
		if s[i] < '0' || s[i] > '9' {
			return false
		}
	}
	return true
}
