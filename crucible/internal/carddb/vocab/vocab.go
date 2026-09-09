// Package vocab measures the card-script vocabulary: every distinct param key,
// ability API, mode, count head, cost part, valid-string token and keyword head
// the corpus uses, with how often each appears.
//
// It is the measurement half of M3's P2 gate. Compiling a script into a typed
// AST needs a type for every token in it, and the only way to know that set is
// closed is to enumerate the corpus and diff the result against what the
// compiler supports. Nothing here interprets a token; it counts them.
//
// The grammars are specified in docs/crucible/porting/dsl/, derived from the
// corpus rather than from reading Java, and this package is what keeps them
// honest: a token appearing here with no entry there is a hole in the spec.
package vocab

import (
	"sort"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb"
)

// Kind is a vocabulary: one namespace of tokens with its own grammar.
type Kind uint8

// The vocabularies. Each is counted separately because each is a different
// namespace: `Attacks` as a trigger mode and `Attacks` as a card property are
// unrelated tokens that happen to share a spelling.
const (
	ParamKey Kind = iota
	API
	Mode
	ReplacementEvent
	CountHead
	CountOperator
	CostPart
	ValidBase
	ValidProperty
	KeywordHead
	SVarHead
	SVarProperty
	AIHintKey

	numKinds = int(AIHintKey) + 1
)

var kindNames = [numKinds]string{
	ParamKey:         "paramKey",
	API:              "api",
	Mode:             "mode",
	ReplacementEvent: "replacementEvent",
	CountHead:        "countHead",
	CountOperator:    "countOperator",
	CostPart:         "costPart",
	ValidBase:        "validBase",
	ValidProperty:    "validProperty",
	KeywordHead:      "keywordHead",
	SVarHead:         "svarHead",
	SVarProperty:     "svarProperty",
	AIHintKey:        "aiHintKey",
}

// String returns the vocabulary's name as the report writes it.
func (k Kind) String() string { return kindNames[k] }

// Kinds returns every vocabulary, in declaration order.
func Kinds() []Kind {
	out := make([]Kind, numKinds)
	for i := range out {
		out[i] = Kind(i)
	}
	return out
}

// apiKeys are the param keys whose value names an ability API. Which one leads
// a param map decides the record type -- spell, activated ability, sub-ability,
// static, replacement -- but all five name an API the same way.
var apiKeys = map[string]bool{"SP": true, "AB": true, "DB": true, "ST": true, "RE": true}

// Vocabulary is what a set of cards uses, by kind.
//
// Counts are occurrences, not cards: a key used twice on one card counts twice,
// because the question a compiler asks is how much of the corpus a missing
// token would break.
type Vocabulary struct {
	counts [numKinds]map[string]int
}

// New returns an empty vocabulary.
func New() *Vocabulary {
	v := &Vocabulary{}
	for i := range v.counts {
		v.counts[i] = make(map[string]int, 256)
	}
	return v
}

// Add records one occurrence of a token. Blank tokens are dropped: a trailing
// separator is not a name.
func (v *Vocabulary) Add(kind Kind, name string) {
	name = strings.TrimSpace(name)
	if name == "" {
		return
	}
	v.counts[kind][name]++
}

// Names returns the distinct tokens of a kind, sorted.
func (v *Vocabulary) Names(kind Kind) []string {
	out := make([]string, 0, len(v.counts[kind]))
	for name := range v.counts[kind] {
		out = append(out, name)
	}
	sort.Strings(out)
	return out
}

// Count returns how many times a token occurs.
func (v *Vocabulary) Count(kind Kind, name string) int { return v.counts[kind][name] }

// Distinct returns how many different tokens a kind holds.
func (v *Vocabulary) Distinct(kind Kind) int { return len(v.counts[kind]) }

// Occurrences returns the total across every token of a kind.
func (v *Vocabulary) Occurrences(kind Kind) int {
	total := 0
	for _, n := range v.counts[kind] {
		total += n
	}
	return total
}

// AddCard records everything one card uses, across every face it has and every
// functional variant of those faces.
func (v *Vocabulary) AddCard(c *carddb.Card) {
	for _, i := range c.PresentFaces() {
		v.addFace(&c.Faces[i])
	}
}

func (v *Vocabulary) addFace(f *carddb.Face) {
	for _, line := range f.Keywords {
		v.Add(KeywordHead, ParseKeyword(line).Head)
	}
	for _, group := range [][]string{f.Abilities, f.Triggers, f.Statics, f.Replacements} {
		for _, line := range group {
			v.addParamMap(line)
		}
	}
	// An SVar body is a param map when it defines a sub-ability, and a count
	// expression when it defines a number. Both are scanned: which one it is
	// depends on how the name is referenced, and a scanner does not follow
	// references.
	for _, name := range f.SVars.Names() {
		value, _ := f.SVars.Get(name)
		v.addSVar(name, value)
	}
	for _, variant := range f.Variants {
		v.addFace(variant)
	}
}

// addSVar records an SVar body, which is one of three things.
//
// A param map when a record key leads it, which is how a sub-ability is
// written. An amount expression otherwise: `Count$CardsInYourHand` is the
// common head, and `Remembered$`, `TriggerCount$`, `Targeted$`, `Number$` and
// two dozen others take the same shape -- a head, a property, and the same
// `/Operator.operand` arithmetic. Anything with no `$` at all is a literal and
// carries no vocabulary.
//
// The distinction matters because the heads are not param keys: counting
// `Enchanted$CardPower` as a param named `Enchanted` would put a token in the
// param vocabulary that no ability ever accepts.
func (v *Vocabulary) addSVar(name, value string) {
	params := SplitParams(value)
	if len(params) == 0 {
		return
	}

	// An `AI`-prefixed name is a hint to the AI, not an ability: 29 such names
	// hold their own `Key$ Value` maps -- AIPreference, AICastPreference,
	// AIRollPlanarDieParams. Their keys are counted apart, because M3 compiles
	// abilities and the AI lands at M7, so the two vocabularies are closed at
	// different times.
	if strings.HasPrefix(name, "AI") {
		for _, p := range params {
			v.Add(AIHintKey, p.Key)
		}
		return
	}

	head := params[0].Key
	if apiKeys[head] || head == "Mode" || head == "Event" {
		v.addParamMap(value)
		return
	}

	v.Add(SVarHead, head)
	if head == "Count" {
		for _, count := range CountExprs(value) {
			v.addCount(count)
		}
		return
	}
	property, operators := cutOperators(params[0].Value)
	v.Add(SVarProperty, property)
	for _, op := range operators {
		v.Add(CountOperator, op)
	}
}

// addParamMap records a param map's keys and everything their values embed.
//
// Segment by segment, because a value ends at the next `|`. Scanning the whole
// line for count expressions instead lets one that sits in front of a
// SpellDescription$ swallow the prose behind it, and prose contains slashes.
func (v *Vocabulary) addParamMap(value string) {
	for _, segment := range strings.Split(value, "|") {
		for _, count := range CountExprs(segment) {
			v.addCount(count)
		}
	}
	for _, p := range SplitParams(value) {
		// `Count$` leading a segment is a count expression, not a param: an
		// SVar body defining a number is written `Count$CardsInYourHand`, and
		// counting "Count" as a param key would invent one.
		if p.Key == "Count" {
			continue
		}
		v.Add(ParamKey, p.Key)
		switch {
		case apiKeys[p.Key]:
			v.Add(API, p.Value)
		case p.Key == "Mode":
			// One vocabulary for trigger and static modes together. 2,483 of
			// them are on an SVar body, where nothing local says which the
			// referencing line is, so splitting them here would be a guess.
			v.Add(Mode, p.Value)
		case p.Key == "Event":
			v.Add(ReplacementEvent, p.Value)
		case p.Key == "Cost":
			for _, part := range CostParts(p.Value) {
				v.Add(CostPart, CostPartName(part))
			}
		}
		if isValidKey(p.Key) {
			for _, alt := range ParseValid(p.Value) {
				v.Add(ValidBase, alt.Base)
				for _, property := range alt.Properties {
					v.Add(ValidProperty, property)
				}
			}
		}
	}
}

func (v *Vocabulary) addCount(c CountExpr) {
	v.Add(CountHead, c.Head)
	for _, op := range c.Operators {
		v.Add(CountOperator, op)
	}
	for _, alt := range ParseValid(c.Valid) {
		v.Add(ValidBase, alt.Base)
		for _, property := range alt.Properties {
			v.Add(ValidProperty, property)
		}
	}
}

// notValidStrings are the `Valid`-prefixed keys whose value is something else:
// a zone, a counter type, a keyword, a comparison. Naming them is unavoidable
// -- the prefix says nothing about the value's grammar, and letting `Hand` or
// `ENERGY` into the valid-string vocabulary would put tokens there that no
// property table will ever hold.
var notValidStrings = map[string]bool{
	"ValidAmountEach":      true,
	"ValidAttackersAmount": true,
	"ValidBlockerAmount":   true,
	"ValidCounterType":     true,
	"ValidCrewAmount":      true,
	"ValidDestination":     true,
	"ValidKeyword":         true,
	"ValidLoseReason":      true,
	"ValidMode":            true,
	"ValidResult":          true,
	"ValidRoll":            true,
	"ValidSides":           true,
	"ValidTypes":           true,
	"ValidZone":            true,
}

// isValidKey reports whether a param key's value is a valid string.
//
// A prefix rule with exceptions rather than a fixed list of the keys that are:
// 70 keys start with `Valid`, upstream adds more, and a list of the ones that
// hold a valid string would go stale silently while a list of the ones that do
// not fails loudly the moment a name lands in the wrong vocabulary.
//
// Description keys are excluded by suffix. `ValidTgtsDesc$` holds prose, and so
// does `ValidTgtsDes$` -- the corpus carries both spellings, one of them a typo
// nobody noticed because nothing parses either.
func isValidKey(key string) bool {
	if !strings.HasPrefix(key, "Valid") && key != "Affected" {
		return false
	}
	if notValidStrings[key] {
		return false
	}
	for _, suffix := range []string{"Desc", "Des", "Description", "Message"} {
		if strings.HasSuffix(key, suffix) {
			return false
		}
	}
	return true
}
