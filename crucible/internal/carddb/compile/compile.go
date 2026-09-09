// Package compile turns a card's ability lines into a tree.
//
// This is the front end of ADR-0007: definitions compile once at load, and
// `SubAbility$` chains become direct references rather than names, so nothing
// resolves a string during a game. What it does not do yet is type the
// parameter values -- that is the generated layer above it, and doing both at
// once would mean writing the generator against a moving target.
//
// Ported from forge-game/src/main/java/forge/game/ability/AbilityFactory.java
// (getAbility, getSubAbility, additionalAbilityKeys).
// Deviations recorded in docs/crucible/porting/port-log/ability-factory.md.
package compile

import (
	"errors"
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
)

// Errors a card script can cause. Each names the card and the reference that
// failed, because the answer is always an edit to one script (GO-7).
var (
	// ErrMissingSVar is a reference to an SVar the face does not define.
	ErrMissingSVar = errors.New("sub-ability names no SVar")
	// ErrCycle is a reference chain that returns to an SVar it already holds.
	ErrCycle = errors.New("sub-ability chain is cyclic")
	// ErrNoRecord is an ability line whose first key names no record type.
	ErrNoRecord = errors.New("line has no record key")
)

// Record is what an ability line declares itself to be, taken from its first
// param key. Java's AbilityRecordType covers four of these; triggers and
// replacements lead with `Mode$` and `Event$` instead and are records here for
// the same reason -- the first key decides how the rest is read.
type Record uint8

// The record types, in the order the grammar lists them.
const (
	Spell Record = iota
	Activated
	SubAbility
	Static
	Replacement
	Trigger
	StaticEffect
)

var recordNames = [...]string{
	Spell:        "Spell",
	Activated:    "Activated",
	SubAbility:   "SubAbility",
	Static:       "Static",
	Replacement:  "Replacement",
	Trigger:      "Trigger",
	StaticEffect: "StaticEffect",
}

// String returns the record type's name.
func (r Record) String() string { return recordNames[r] }

// recordKeys maps a leading param key to what it declares. `Mode$` is two
// records, told apart by the line that carries it: a `T:` line is a trigger and
// an `S:` line is a continuous effect, and no SVar body says which it is until
// something references it.
// Keys are folded, because the map they are looked up in ignores case.
var recordKeys = map[string]Record{
	"sp":    Spell,
	"ab":    Activated,
	"db":    SubAbility,
	"st":    Static,
	"re":    Replacement,
	"mode":  Trigger,
	"event": Replacement,
}

// Ability is one compiled ability line.
type Ability struct {
	Record Record
	// Name is the value of the record key: the API for SP/AB/DB/ST/RE, the
	// mode for a trigger or static, the event for a replacement.
	Name string
	// SVar is the variable this was defined in, empty for a line written
	// directly as `A:`, `T:`, `S:` or `R:`.
	SVar string
	// Params are the line's `Key$ Value` entries after Java's map semantics:
	// keys match without case and a repeat replaces the earlier value. Order is
	// the script's, which Java's TreeMap loses and nothing should depend on.
	// Values are still text -- typing them is the generated layer's job (GO-8).
	Params []vocab.Param
	// Subs are the abilities this one names, resolved. Order is the order the
	// params appear in, so it is the script's.
	Subs []SubRef
}

// SubRef is a resolved reference from one ability to another.
type SubRef struct {
	// Key is the param that named it: `SubAbility`, `Execute`, `Choices`, one
	// of Java's additional-ability keys.
	Key string
	// SVar is the name it was written as, kept because a trigger's description
	// and the port log both refer to abilities by their SVar name.
	SVar string
	// Ability is the compiled target. Never nil.
	Ability *Ability
}

// Param returns a param's value, and whether the line has it. Keys match
// without case, because Java holds them in a CASE_INSENSITIVE_ORDER TreeMap and
// the corpus writes `staticAbilities$` and `StaticAbilities$` for one key.
func (a *Ability) Param(key string) (string, bool) {
	for _, p := range a.Params {
		if strings.EqualFold(p.Key, key) {
			return p.Value, true
		}
	}
	return "", false
}

// mergeParams applies the semantics of FileSection.parseToMap: the params go
// into a TreeMap with CASE_INSENSITIVE_ORDER, so a repeated key keeps only its
// last value and two spellings of one key are one entry.
//
// Twenty-nine corpus lines repeat a key, and one writes `SubABility$`. Reading
// the params as a list instead would chain a sub-ability Forge never chains,
// and miss one it does.
func mergeParams(params []vocab.Param) []vocab.Param {
	out := make([]vocab.Param, 0, len(params))
	at := make(map[string]int, len(params))
	for _, p := range params {
		folded := strings.ToLower(p.Key)
		if i, ok := at[folded]; ok {
			out[i].Value = p.Value
			continue
		}
		at[folded] = len(out)
		out = append(out, p)
	}
	return out
}

// Face is one printed face's compiled definitions, in script order.
type Face struct {
	Abilities    []*Ability
	Triggers     []*Ability
	Statics      []*Ability
	Replacements []*Ability
}

// Card is a compiled card: one [Face] per face the script filled.
type Card struct {
	Filename string
	Faces    [carddb.NumFaces]Face
}

// Compile resolves every ability line of every face.
//
// It returns an error rather than dropping a bad reference. Java prints
// "SubAbility 'X' not found" to stdout and carries on with a null child, which
// is how a card can ship with an ability that silently does half of what its
// text says; a script that cannot be compiled is a script to fix (PORT-8).
func Compile(card *carddb.Card) (*Card, error) {
	out := &Card{Filename: card.Filename}
	for _, i := range card.PresentFaces() {
		face, err := compileFace(&card.Faces[i])
		if err != nil {
			return nil, fmt.Errorf("%s face %d: %w", card.Filename, i, err)
		}
		out.Faces[i] = face
	}
	return out, nil
}

func compileFace(face *carddb.Face) (Face, error) {
	c := &faceCompiler{face: face, open: map[string]bool{}}

	var out Face
	for _, group := range []struct {
		lines  []string
		target *[]*Ability
		record Record
	}{
		{face.Abilities, &out.Abilities, Spell},
		{face.Triggers, &out.Triggers, Trigger},
		{face.Statics, &out.Statics, StaticEffect},
		{face.Replacements, &out.Replacements, Replacement},
	} {
		for _, line := range group.lines {
			ability, err := c.line(line, group.record)
			if err != nil {
				return Face{}, err
			}
			*group.target = append(*group.target, ability)
		}
	}
	return out, nil
}

// faceCompiler holds the SVar namespace one face's references resolve in, and
// the chain currently being resolved.
type faceCompiler struct {
	face *carddb.Face
	open map[string]bool // SVar names on the current chain, for cycle detection
}

// line compiles one ability line. The declared record is what the line's own
// key says; want is what the line's position implies, and decides only whether
// a `Mode$` line is a trigger or a continuous effect.
func (c *faceCompiler) line(text string, want Record) (*Ability, error) {
	params := mergeParams(vocab.SplitParams(text))
	if len(params) == 0 {
		return nil, fmt.Errorf("%w: %q", ErrNoRecord, text)
	}

	record, ok := recordKeys[strings.ToLower(params[0].Key)]
	if !ok {
		return nil, fmt.Errorf("%w: %q leads with %q", ErrNoRecord, text, params[0].Key)
	}
	if record == Trigger && want == StaticEffect {
		record = StaticEffect
	}

	ability := &Ability{Record: record, Name: params[0].Value, Params: params}
	for _, p := range params {
		names, ok := c.references(ability, p)
		if !ok {
			continue
		}
		for _, name := range names {
			sub, err := c.reference(p.Key, name)
			if err != nil {
				return nil, err
			}
			ability.Subs = append(ability.Subs, sub)
		}
	}
	return ability, nil
}

// reference compiles the SVar a param names.
func (c *faceCompiler) reference(key, name string) (SubRef, error) {
	if c.open[name] {
		return SubRef{}, fmt.Errorf("%w: %s revisits %q", ErrCycle, key, name)
	}
	body, ok := c.face.SVars.Get(name)
	if !ok {
		return SubRef{}, fmt.Errorf("%w: %s names %q", ErrMissingSVar, key, name)
	}

	c.open[name] = true
	defer delete(c.open, name)

	// A referenced SVar is a sub-ability unless its own key says otherwise,
	// which is what `Execute$` on a delayed trigger relies on.
	ability, err := c.line(body, SubAbility)
	if err != nil {
		return SubRef{}, fmt.Errorf("in %q: %w", name, err)
	}
	ability.SVar = name
	return SubRef{Key: key, SVar: name, Ability: ability}, nil
}

// references returns the SVar names a param holds, and whether the param names
// abilities at all.
//
// Four shapes, all of them AbilityFactory's:
//
//   - `SubAbility$ X` and the additional-ability keys name one SVar.
//   - `Choices$ A,B,C` names a list, and only for the five APIs that read it.
//   - `ResultSubAbilities$ 1:A,2:B` names `key:svar` pairs, and only for
//     RollDice.
//
// The API gate is not decoration. `Choices$` on any other API is a valid
// string, and resolving it as a list of SVars would fail on cards that are
// correct.
func (c *faceCompiler) references(a *Ability, p vocab.Param) ([]string, bool) {
	switch {
	case subAbilityKeys[strings.ToLower(p.Key)]:
		return []string{p.Value}, true
	case strings.EqualFold(p.Key, "Choices") && choiceAPIs[a.Name]:
		return splitTrim(p.Value, ","), true
	case strings.EqualFold(p.Key, "ResultSubAbilities") && a.Name == "RollDice":
		var out []string
		for _, pair := range splitTrim(p.Value, ",") {
			_, name, ok := strings.Cut(pair, ":")
			if !ok {
				continue
			}
			out = append(out, strings.TrimSpace(name))
		}
		return out, true
	}
	return nil, false
}

// subAbilityKeys are the params whose value is an SVar holding an ability:
// `SubAbility`, `PreventionSubAbility`, and Java's additionalAbilityKeys list
// verbatim (AbilityFactory.java:51). Folded, because the lookup ignores case.
var subAbilityKeys = map[string]bool{
	"subability":             true,
	"preventionsubability":   true,
	"winsubability":          true,
	"otherwisesubability":    true,
	"bidsubability":          true,
	"choosenumbersubability": true,
	"lowest":                 true,
	"highest":                true,
	"notlowest":              true,
	"guesscorrect":           true,
	"guesswrong":             true,
	"matchedability":         true,
	"unmatchedability":       true,
	"headssubability":        true,
	"tailssubability":        true,
	"losesubability":         true,
	"truesubability":         true,
	"falsesubability":        true,
	"chosenpile":             true,
	"unchosenpile":           true,
	"repeatsubability":       true,
	"execute":                true,
	"fallbackability":        true,
	"choosesubability":       true,
	"cantchoosesubability":   true,
	"regenerationability":    true,
	"returnability":          true,
	"giftability":            true,
	"votesubability":         true,
	"votetiedability":        true,
}

// choiceAPIs are the APIs whose `Choices$` names sub-abilities.
var choiceAPIs = map[string]bool{
	"Charm":            true,
	"GenericChoice":    true,
	"AssignGroup":      true,
	"VillainousChoice": true,
	"Vote":             true,
}

func splitTrim(value, sep string) []string {
	fields := strings.Split(value, sep)
	out := make([]string, 0, len(fields))
	for _, f := range fields {
		if f = strings.TrimSpace(f); f != "" {
			out = append(out, f)
		}
	}
	return out
}
