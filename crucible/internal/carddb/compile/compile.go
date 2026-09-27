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
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/mana"
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

//go:generate go run ../../../tools/genparams -table ../../../tools/apiscan/testdata/param-kinds.golden

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
	// Name is the face's own printed name -- a back face's differs from
	// the card's (Card.Name is the front face's).
	Name string

	// Type is the face's printed card type line, carried through unchanged
	// from carddb.Face -- the engine's only way to ask "is this an Aura"
	// today. It is the printed value only: nothing that would change it
	// (the layer system) exists yet, so a card whose type a continuous
	// effect currently changes reports the one on the card, not the one in
	// play (game-state.md's "Not ported yet").
	Type cardtype.Line

	// Power and Toughness are carried through unchanged from carddb.Face,
	// printed values only -- either can be "*", "1+*" or a Count$
	// reference, which is why they stay text here rather than becoming an
	// int at compile time (carddb.Face's own doc comment). Resolving one to
	// a number, when it is a plain integer, is engine.Card.BasePower/
	// BaseToughness's job (game-state.md's "Layer 0" section).
	Power, Toughness string

	// Loyalty is a planeswalker's printed starting loyalty, carried through
	// the same way and for the same reason: it can be a plain integer or an
	// SVar reference, and resolving a plain one is engine.Card.BaseLoyalty's
	// job.
	Loyalty string

	// Defense is a Battle's printed starting defense -- the same shape as
	// Loyalty, and for the same reason: CR 704.5v checks the Defense counter
	// count directly, not a layered value, so engine.Card.BaseDefense is
	// only ever "how many counters would it enter with."
	Defense string

	// ManaCost, Colors and HasColors are carried through unchanged from
	// carddb.Face, the same "printed value only" shape Type/Power/Toughness
	// already use. Colors is only meaningful when HasColors is true (a
	// script's explicit `Colors:` override); absent that, a card's color is
	// derived from ManaCost, which is engine.Card.Colors' job -- the exact
	// logic carddb.Face.dumpColors already verifies against Forge's own
	// dump (M2's P1 gate), not a new derivation invented here.
	ManaCost  mana.Cost
	Colors    mana.Colors
	HasColors bool

	// Keywords are the face's `K:` lines, carried through unchanged from
	// carddb.Face -- keyword.Parse resolves one to a name at read time
	// (engine.Card.HasKeyword's job), the same "carry the printed text,
	// interpret it downstream" split Type/Power/Toughness/Loyalty already
	// use. Expanding a keyword into the triggers, statics and abilities it
	// stands for is a different job entirely (keyword.go's own doc
	// comment), done here for two keywords only, each because it names an
	// SVar nothing else would compile: `Dungeon` (its rooms, added to
	// Triggers) and `ETBReplacement` (its replacement, added to
	// Replacements). The keyword line itself stays here too.
	Keywords []string

	Abilities    []*Ability
	Triggers     []*Ability
	Statics      []*Ability
	Replacements []*Ability

	// Amounts is every SVar this face defines that is NOT itself an ability
	// (one of the four slices above, reached through a Sub reference when a
	// param names it) -- most commonly a `SVar:X:Count$...` line, the value
	// a param like `AddPower$ X` or `SetToughness$ Y` names. Parsed once
	// here, the same "compile once, never re-interpret a script string at
	// runtime" reason every other Face field is (PORT-2) -- an engine
	// resolver (resolveAmount, internal/engine) reads this map by name
	// rather than re-parsing the raw SVar text itself. Keyed by the SVar's
	// own name, folded to lower case (SVars.Get's own case-insensitive
	// contract, carddb/card.go) since a param value naming it may not match
	// its declared spelling exactly. nil when the face defines no such SVar.
	Amounts map[string]expr.Amount
}

// Card is a compiled card: one [Face] per face the script filled.
type Card struct {
	Filename string
	// Name is the primary face's printed name -- the same string DB.Card
	// looks compiled cards up by. It is carried on the compiled value itself
	// because nothing else a *Card holds can answer "what card is this": the
	// script string is gone by this stage, and Filename is a snake_case file
	// stem, not a printed name (card.go:196).
	Name  string
	Faces [carddb.NumFaces]Face
	// SplitType is how the faces relate (AlternateMode:), what decides
	// whether the card can transform.
	SplitType carddb.SplitType
}

// Compile resolves every ability line of every face.
//
// It returns an error rather than dropping a bad reference. Java prints
// "SubAbility 'X' not found" to stdout and carries on with a null child, which
// is how a card can ship with an ability that silently does half of what its
// text says; a script that cannot be compiled is a script to fix (PORT-8).
func Compile(card *carddb.Card) (*Card, error) {
	out := &Card{Filename: card.Filename, Name: card.Faces[0].Name, SplitType: card.SplitType}
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

	out := Face{
		Name: face.Name,
		Type: face.Type, Power: face.Power, Toughness: face.Toughness,
		Loyalty: face.InitialLoyalty, Defense: face.Defense, Keywords: face.Keywords,
		ManaCost: face.ManaCost, Colors: face.Colors, HasColors: face.HasColors,
	}
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
	for _, kw := range face.Keywords {
		if rest, ok := strings.CutPrefix(kw, "ETBReplacement:"); ok {
			r, err := c.etbReplacement(rest)
			if err != nil {
				return Face{}, err
			}
			if r != nil {
				out.Replacements = append(out.Replacements, r)
			}
			continue
		}
		rooms, ok := strings.CutPrefix(kw, "Dungeon:")
		if !ok {
			continue
		}
		triggers, err := c.dungeonRooms(splitTrim(rooms, ","))
		if err != nil {
			return Face{}, err
		}
		out.Triggers = append(out.Triggers, triggers...)
	}
	out.Amounts = compileAmounts(face)
	out.Amounts = addInlineAmounts(out.Amounts, out.Statics)
	return out, nil
}

// addInlineAmounts parses a static ability's power/toughness value written
// inline as a raw expression rather than as an SVar name --
// nethergoyf.txt's own `SetPower$ Count$ValidGraveyard Card.YouOwn$CardTypes`,
// which calculateAmount reads as a raw value because `amount.indexOf('$') > 0`
// -- into amounts, keyed by the value's own text folded to lower case: the
// key an evaluator already looks a param value up by. An SVar name never
// contains `$`, so no such key can shadow a real SVar. Parsed here, once,
// for the same reason every other amount is (PORT-2).
//
// Limited to the four Layer 7 keys (SetPower$, SetToughness$, AddPower$,
// AddToughness$) a Mode$ Continuous line's own amount resolution reads;
// every other key keeps reading an SVar name only.
func addInlineAmounts(amounts map[string]expr.Amount, statics []*Ability) map[string]expr.Amount {
	for _, s := range statics {
		for _, key := range [...]string{"SetPower", "SetToughness", "AddPower", "AddToughness"} {
			v, ok := s.Param(key)
			if !ok || strings.Index(v, "$") <= 0 {
				continue
			}
			amt := expr.Parse(v)
			if amt.Kind != expr.Expression {
				continue
			}
			if amounts == nil {
				amounts = map[string]expr.Amount{}
			}
			amounts[strings.ToLower(v)] = amt
		}
	}
	return amounts
}

// ErrBadRoom is a dungeon room ability missing its RoomName$, or naming a
// NextRoom$ SVar its own K:Dungeon line does not list.
var ErrBadRoom = errors.New("bad dungeon room")

// dungeonRooms expands a dungeon's `K:Dungeon:<svar>,<svar>,...` keyword
// into one room trigger per SVar, in keyword order -- the order
// VentureEffect reads (its first trigger is the entrance) -- the way
// CardFactoryUtil.java:1955-1989 does at card creation:
//
//	Mode$ RoomEntered | TriggerZones$ Command | ValidCard$ Card.Self | ValidRoom$ <RoomName>
//
// with the room's own ability as the trigger's Execute$. A room naming
// NextRoom$ SVars also gets NextRoomName$, their RoomName$ values joined
// with ",", which is what VentureEffect.chooseNextRoom offers. Nothing
// references these SVars through a param, so without this they would
// compile nowhere and a dungeon would have no rooms (PORT-2).
//
// Java dereferences a missing RoomName$ or an unlisted NextRoom$ SVar
// (NullPointerException); both are ErrBadRoom here (PORT-8).
func (c *faceCompiler) dungeonRooms(svars []string) ([]*Ability, error) {
	rooms := make([]SubRef, 0, len(svars))
	index := make(map[string]*Ability, len(svars))
	for _, name := range svars {
		ref, err := c.reference("Dungeon", name)
		if err != nil {
			return nil, err
		}
		if _, ok := ref.Ability.Param("RoomName"); !ok {
			return nil, fmt.Errorf("%w: %q has no RoomName$", ErrBadRoom, name)
		}
		rooms = append(rooms, ref)
		index[name] = ref.Ability
	}
	triggers := make([]*Ability, 0, len(rooms))
	for _, ref := range rooms {
		room := ref.Ability
		roomName, _ := room.Param("RoomName")
		if next, ok := room.Param("NextRoom"); ok {
			var names []string
			for _, svar := range splitTrim(next, ",") {
				target, ok := index[svar]
				if !ok {
					return nil, fmt.Errorf("%w: %q leads to %q, not a room of this dungeon", ErrBadRoom, ref.SVar, svar)
				}
				n, _ := target.Param("RoomName")
				names = append(names, n)
			}
			room.Params = append(room.Params, vocab.Param{Key: "NextRoomName", Value: strings.Join(names, ",")})
		}
		triggers = append(triggers, &Ability{
			Record: Trigger,
			Name:   "RoomEntered",
			Params: []vocab.Param{
				{Key: "Mode", Value: "RoomEntered"}, {Key: "TriggerZones", Value: "Command"},
				{Key: "ValidCard", Value: "Card.Self"}, {Key: "ValidRoom", Value: roomName},
			},
			Subs: []SubRef{{Key: "Execute", SVar: ref.SVar, Ability: room}},
		})
	}
	return triggers, nil
}

// ErrBadETBReplacement is a `K:ETBReplacement` keyword too short to name
// its SVar, or naming a replacement layer Forge does not have.
var ErrBadETBReplacement = errors.New("bad ETBReplacement keyword")

// replacementLayers are ReplacementLayer.java's values, the only ones
// ReplacementLayer.smartValueOf accepts (case-insensitively).
var replacementLayers = [...]string{"CantHappen", "Control", "Copy", "Transform", "Other"}

// etbReplacement expands one `K:ETBReplacement:<Layer>:<SVar>[:<Optional>
// [:<Zone>[:<Valid>]]]` keyword (rest is what follows the prefix) into the
// replacement it stands for, the way CardFactoryUtil.java:2595-2606 and
// createETBReplacement (CardFactoryUtil.java:515-543) do at card creation:
//
//	Event$ Moved | ValidCard$ <Valid> | Destination$ Battlefield | ReplacementResult$ Updated | Layer$ <Layer>
//
// plus Optional$ True when the fourth field contains "Optional", ActiveZones$
// <Zone> when the fifth is present, and Description$ from the SVar's own
// SpellDescription$. The SVar is the replacement's ReplaceWith$ ability.
// Valid defaults to Card.Self: the keyword's own card entering. Layer$ is a
// param here rather than a field because that is how an R: line spells the
// same thing, and ReplacementEffect's constructor reads it the same way
// (ReplacementEffect.java:109-111). Nothing references the SVar through a
// param, so without this it would compile nowhere (PORT-2).
//
// Only the Copy layer (68 cards, "enters as a copy") is expanded; any other
// layer returns nil. The engine dispatches no other ETBReplacement layer
// yet, and compiling the Other layer's 353 SVars surfaces two dead params
// (ListTitle$ on ChooseEvenOdd, ashlings_prerogative.txt and
// gollum_riddle_master.txt) that the tools/apiscan -api gate would fail on:
// those belong to whoever ports the Other layer, not to Layer 1.
func (c *faceCompiler) etbReplacement(rest string) (*Ability, error) {
	fields := strings.Split(rest, ":")
	if len(fields) < 2 {
		return nil, fmt.Errorf("%w: %q names no SVar", ErrBadETBReplacement, rest)
	}
	layer := ""
	for _, l := range replacementLayers {
		if strings.EqualFold(l, strings.TrimSpace(fields[0])) {
			layer = l
		}
	}
	if layer == "" {
		return nil, fmt.Errorf("%w: %q names no replacement layer", ErrBadETBReplacement, rest)
	}
	if layer != "Copy" {
		return nil, nil
	}
	ref, err := c.reference("ReplaceWith", fields[1])
	if err != nil {
		return nil, err
	}
	// fields is Java's splitkw without its leading "ETBReplacement", so
	// splitkw[n] is fields[n-1].
	validCard := "Card.Self"
	if len(fields) >= 5 {
		validCard = fields[4]
	}
	params := []vocab.Param{
		{Key: "Event", Value: "Moved"}, {Key: "ValidCard", Value: validCard},
		{Key: "Destination", Value: "Battlefield"}, {Key: "ReplacementResult", Value: "Updated"},
		{Key: "Layer", Value: layer}, {Key: "ReplaceWith", Value: fields[1]},
	}
	if len(fields) >= 3 && strings.Contains(fields[2], "Optional") {
		params = append(params, vocab.Param{Key: "Optional", Value: "True"})
	}
	if len(fields) >= 4 && fields[3] != "" {
		params = append(params, vocab.Param{Key: "ActiveZones", Value: fields[3]})
	}
	if desc, ok := ref.Ability.Param("SpellDescription"); ok {
		params = append(params, vocab.Param{Key: "Description", Value: desc})
	}
	return &Ability{Record: Replacement, Name: "Moved", Params: params, Subs: []SubRef{ref}}, nil
}

// compileAmounts parses every SVar face defines that is NOT itself an
// ability -- Face.Amounts' own doc comment has the reason and the shape --
// via internal/expr, keyed by name folded to lower case.
//
// An SVar whose body IS an ability (its own head, up to the first `$`,
// folds to one of recordKeys -- `DB$`, `AB$`, `SP$`, `ST$`, `RE$`,
// `Mode$`/`Event$`) is skipped: it already compiles through
// reference/SubRef above when a param names it, and running it through
// expr.Parse too would store a meaningless Amount for it (Head would be
// "DB" or "Mode", never a real Count$ family resolveAmount evaluates).
// expr.Parse never fails (its own doc comment), so every other SVar gets an
// entry even when its body is not a shape resolveAmount can use yet --
// exactly the same "record what recognizing it needs, evaluator decides
// whether it can" split Ability.Params already has, GO-8's reason params
// stay text at this layer.
func compileAmounts(face *carddb.Face) map[string]expr.Amount {
	names := face.SVars.Names()
	if len(names) == 0 {
		return nil
	}
	out := make(map[string]expr.Amount, len(names))
	for _, name := range names {
		body, _ := face.SVars.Get(name)
		amt := expr.Parse(body)
		if amt.Kind == expr.Expression {
			if _, isAbility := recordKeys[strings.ToLower(amt.Head)]; isAbility {
				continue
			}
		}
		out[strings.ToLower(name)] = amt
	}
	return out
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
	// which is what `Execute$` on a delayed trigger relies on. The one
	// exception is Effect's `StaticAbilities$`: its SVars lead with `Mode$`
	// like a trigger's, and only the naming key says they are continuous
	// effects (EffectEffect.java adds them through addStaticAbility).
	want := SubAbility
	if strings.EqualFold(key, "StaticAbilities") || strings.EqualFold(key, "AddStaticAbilities") {
		want = StaticEffect
	}
	ability, err := c.line(body, want)
	if err != nil {
		return SubRef{}, fmt.Errorf("in %q: %w", name, err)
	}
	ability.SVar = name
	return SubRef{Key: key, SVar: name, Ability: ability}, nil
}

// references returns the SVar names a param holds, and whether the param names
// abilities at all.
//
// Five shapes, four of them AbilityFactory's:
//
//   - `SubAbility$ X` and the additional-ability keys name one SVar.
//   - `Choices$ A,B,C` names a list, and only for the five APIs that read it.
//   - `StaticAbilities$`/`Triggers$`/`ReplacementEffects$ A,B` name a list,
//     and only for Effect (EffectEffect.java).
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
	case effectTraitKeys[strings.ToLower(p.Key)] && a.Name == "Effect":
		return splitTrim(p.Value, ","), true
	case cloneTraitKeys[strings.ToLower(p.Key)] && a.Name == "Clone":
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
// `SubAbility`, `PreventionSubAbility`, Java's additionalAbilityKeys list
// verbatim (AbilityFactory.java:51), and the four keys the handlers resolve
// themselves without going through that list. Folded, because the lookup
// ignores case.
//
// The list is not the whole story on its own: `additionalAbilityKeys` is what
// AbilityFactory attaches to a SpellAbility, while a replacement's own ability
// is fetched by ReplacementHandler and an extra turn's by the effect that
// grants it. Reading only the list leaves 1,592 references unresolved, and a
// reference the compiler never follows is one the corpus gate can never find
// dangling.
var subAbilityKeys = map[string]bool{
	"subability":           true,
	"preventionsubability": true,

	// Resolved by the handlers rather than by AbilityFactory.
	// ReplacementHandler.java:843, RollDiceEffect.java:474,
	// AddPhaseEffect.java:72, AddTurnEffect.java:50. The misspelling in
	// `ExtraPhaseDelayedTriggerExcute` is Forge's and is load-bearing: the
	// param map is keyed on it.
	"replacewith":                    true,
	"else":                           true,
	"extraphasedelayedtriggerexcute": true,
	"extraturndelayedtriggerexecute": true,

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

	// StaticAbilityCantAttackBlock's own Mode$ OptionalAttackCost (CR
	// 508.1c's "you may exert this as it attacks"): the "when you do" payoff
	// this static ability's own Trigger$ param names, 23 of the corpus's 28
	// real Cost$ Exert<1/CARDNAME> OptionalAttackCost lines (5 name none at
	// all -- ahn_crop_crasher.txt's own K:Haste sits above a bare S: line
	// with no Trigger$; resolute_survivors.txt's own payoff is a separate,
	// ordinary T:Mode$ Exerted line instead, checkExertedTriggers'
	// (exertcost.go) own shape already covers that one). Not the same key
	// as `T:`'s own `Trigger$` -- CR 603's own trigger-mode grammar has no
	// param of this name; StaticAbilityCantAttackBlock.java's own
	// `getAttackCost`/`getSSTrigger` is the only Java reader.
	"trigger": true,
}

// effectTraitKeys are the params through which an Effect names the SVars
// holding the triggers, continuous effects and replacement effects its
// effect card carries: EffectEffect.java splits each on "," and parses every
// name with AbilityUtils.getSVar. Compiling them here is what lets the
// engine build an effect card from compiled abilities instead of reparsing
// script text at resolution (PORT-2). Gated on the Effect API for the same
// reason `Choices$` is gated: Animate and others write `Triggers$` too, and
// their values are not all SVar lists this compiler can follow yet.
var effectTraitKeys = map[string]bool{
	"staticabilities":    true,
	"triggers":           true,
	"replacementeffects": true,
}

// cloneTraitKeys are the params through which a Clone names the SVars
// holding the triggers, activated abilities and continuous effects its copy
// gains: CardFactory.getCloneStates splits each on "," and parses every name
// out of the host's SVars (CardFactory.java:633-665). Compiled here so the
// engine adds compiled abilities to the copy instead of reparsing script
// text at resolution (PORT-2). Gated on Clone for the reason effectTraitKeys
// is gated on Effect.
var cloneTraitKeys = map[string]bool{
	"addtriggers":        true,
	"addabilities":       true,
	"addstaticabilities": true,
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
