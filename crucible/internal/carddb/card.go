// Package carddb holds the static card database: one card script becomes one
// immutable [Card], read once at load and shared by every game (PORT-2).
//
// Ability lines are kept as text here. Nothing in this package interprets an
// `A:`, `T:`, `S:`, `R:` or `K:` line; compiling those into a typed AST is M3's
// job, and mixing the two would put string interpretation back on the hot path,
// which is the single largest waste this port exists to remove.
//
// Ported from forge-core/src/main/java/forge/card/CardRules.java (its Reader),
// CardFace.java and CardSplitType.java.
// Deviations recorded in docs/crucible/porting/port-log/card-rules-reader.md.
package carddb

import (
	"sort"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// NumFaces is how many faces one script can fill: the primary, the ALTERNATE
// face, and one per colour for a Specialize card.
const NumFaces = 7

// Face indices. A script addresses them through `ALTERNATE` and
// `SPECIALIZE:<COLOUR>`, never by number.
const (
	FacePrimary         = 0
	FaceAlternate       = 1
	FaceSpecializeWhite = 2
	FaceSpecializeBlue  = 3
	FaceSpecializeBlack = 4
	FaceSpecializeRed   = 5
	FaceSpecializeGreen = 6
)

// SplitType says how a card's faces relate to each other.
//
// Values and names are Java's CardSplitType. The script spelling `DoubleFaced`
// maps to [SplitTransform], which is Java's one special case in smartValueOf.
type SplitType uint8

// The ten split types, in Java's declaration order.
const (
	SplitNone SplitType = iota
	SplitTransform
	SplitMeld
	SplitSplit
	SplitFlip
	SplitAdventure
	SplitOmen
	SplitModal
	SplitPrepare
	SplitSpecialize

	numSplitTypes = int(SplitSpecialize) + 1
)

var splitTypeNames = [numSplitTypes]string{
	SplitNone:       "None",
	SplitTransform:  "Transform",
	SplitMeld:       "Meld",
	SplitSplit:      "Split",
	SplitFlip:       "Flip",
	SplitAdventure:  "Adventure",
	SplitOmen:       "Omen",
	SplitModal:      "Modal",
	SplitPrepare:    "Prepare",
	SplitSpecialize: "Specialize",
}

// String returns the name Java's enum uses, which is also what a script writes
// -- except for Transform, which a script spells `DoubleFaced`.
func (t SplitType) String() string { return splitTypeNames[t] }

// SplitTypeFromScript reads an `AlternateMode:` value.
func SplitTypeFromScript(value string) (SplitType, bool) {
	if value == "DoubleFaced" {
		return SplitTransform, true
	}
	for i, name := range splitTypeNames {
		if name == value {
			return SplitType(i), true
		}
	}
	return SplitNone, false
}

// SVars are a face's script variables.
//
// Java holds them in a TreeMap with CASE_INSENSITIVE_ORDER, which decides two
// things worth reproducing exactly: `SVar:X` and `SVar:x` are the same
// variable, and iteration is sorted rather than in script order. The canonical
// JSON dump depends on that order, so it is not an implementation detail.
type SVars struct {
	byFolded map[string]svar
}

type svar struct {
	name  string // first spelling seen, as TreeMap keeps its existing key
	value string
}

// Set stores a variable, replacing any previous value for the same name
// compared case-insensitively. The first spelling of the name is kept.
func (s *SVars) Set(name, value string) {
	if s.byFolded == nil {
		s.byFolded = make(map[string]svar, 8)
	}
	folded := strings.ToLower(name)
	if existing, ok := s.byFolded[folded]; ok {
		s.byFolded[folded] = svar{name: existing.name, value: value}
		return
	}
	s.byFolded[folded] = svar{name: name, value: value}
}

// Get returns a variable's value, matched case-insensitively.
func (s SVars) Get(name string) (string, bool) {
	v, ok := s.byFolded[strings.ToLower(name)]
	return v.value, ok
}

// Len returns the number of variables.
func (s SVars) Len() int { return len(s.byFolded) }

// Names returns the variable names in Java's iteration order: sorted, ignoring
// case, each in the spelling the script used first.
func (s SVars) Names() []string {
	out := make([]string, 0, len(s.byFolded))
	for _, v := range s.byFolded {
		out = append(out, v.name)
	}
	sort.Slice(out, func(i, j int) bool {
		li, lj := strings.ToLower(out[i]), strings.ToLower(out[j])
		if li != lj {
			return li < lj
		}
		return out[i] < out[j]
	})
	return out
}

// Face is one printed face of a card.
//
// The zero value is an absent face: a script fills faces by index, and a card
// with no ALTERNATE line leaves six of the seven empty. Present says which is
// which, because "name is empty" and "face does not exist" are different
// things (GO-11).
type Face struct {
	Present bool

	Name       string
	FlavorName string
	Type       cardtype.Line
	ManaCost   mana.Cost
	Oracle     string

	// Colors is a script's explicit `Colors:` override. HasColors distinguishes
	// "declared colorless" from "not declared, derive from the mana cost".
	Colors    mana.Colors
	HasColors bool

	// Power and Toughness are kept as written: either can be `*`, `1+*`, or a
	// Count$ reference, and resolving those needs a game (M3).
	Power     string
	Toughness string

	InitialLoyalty   string
	Defense          string
	AttractionLights string
	NonAbilityText   string

	// Script lines kept verbatim for M3 to compile. Order is script order,
	// which is load-bearing: it decides the order abilities appear on the card
	// and, for triggers, the order they are put on the stack (GO-12).
	Abilities    []string
	Keywords     []string
	Triggers     []string
	Statics      []string
	Replacements []string
	DeckRules    []string
	DraftActions []string

	SVars SVars

	// Variants are alternative rules for a card of the same name, introduced by
	// `Variant:<name>:<line>`. Attractions and some Un-cards use them.
	Variants map[string]*Face
}

// Card is one card script, parsed.
type Card struct {
	// Filename is the script this came from, without directory or extension.
	// It is the identity used by the corpus dump, because two cards can share
	// a printed name.
	Filename string

	Faces     [NumFaces]Face
	SplitType SplitType

	PartnerWith      string
	PartnerType      string
	MeldWith         string
	HandLifeModifier string
	SetColorID       int

	// Tokens the card can create, harvested from any `TokenScript$` parameter
	// wherever it appears in the script.
	Tokens []string

	// Deck-building metadata, kept as written. Nothing in the engine reads it;
	// the deck generator will.
	DeckHints string
	DeckNeeds string
	DeckHas   string

	RemovedFromAIDecks           bool
	RemovedFromRandomDecks       bool
	RemovedFromNonCommanderDecks bool

	// PlaceholderFaces maps a face index to the card name it is copied from.
	// Resolving them needs every card, so a single-card parse is deliberately
	// incomplete until [ResolvePlaceholders] runs over the whole corpus.
	PlaceholderFaces map[int]string

	// SupportedVariants lists every variant name any face declared.
	SupportedVariants []string
}

// Primary returns the face a card is identified by.
func (c *Card) Primary() *Face { return &c.Faces[FacePrimary] }

// Name returns the primary face's name, which is the card's name.
func (c *Card) Name() string { return c.Faces[FacePrimary].Name }

// PresentFaces returns the indices of the faces the script filled, in order.
func (c *Card) PresentFaces() []int {
	var out []int
	for i := range c.Faces {
		if c.Faces[i].Present {
			out = append(out, i)
		}
	}
	return out
}
