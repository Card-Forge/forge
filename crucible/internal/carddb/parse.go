// Ported from forge-core/src/main/java/forge/card/CardRules.java (Reader).
// Deviations recorded in docs/crucible/porting/port-log/card-rules-reader.md.

package carddb

import (
	"errors"
	"fmt"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// ErrBadScript reports a card script the parser could not read. Card scripts
// are data, so this is an error and never a panic (GO-7).
var ErrBadScript = errors.New("bad card script")

// alternateKey is the one line in the corpus with no colon and no value.
const alternateKey = "ALTERNATE"

// tokenMarker introduces the token a card can create, wherever it appears in a
// value.
const tokenMarker = "TokenScript$"

// ignoredKeys are corpus defects Forge silently drops, listed here so the
// corpus still loads while a third one is an error rather than a shrug. Each
// names the card it appears on, so the list can be re-checked after a sync.
var ignoredKeys = map[string]string{
	"ODeckHints": "spirit_of_resilience: DeckHints, mistyped",
	"DBCleanup":  "the_dawning_archaic: an SVar body that lost its SVar: prefix",
}

// specializeFaces maps a SPECIALIZE colour to the face it switches to.
var specializeFaces = map[string]int{
	"WHITE": FaceSpecializeWhite,
	"BLUE":  FaceSpecializeBlue,
	"BLACK": FaceSpecializeBlack,
	"RED":   FaceSpecializeRed,
	"GREEN": FaceSpecializeGreen,
}

// parser holds the state a script builds up as it is read. Java reuses one
// Reader across every card and calls reset(); this is per script, so nothing
// can leak from one card to the next (GO-2).
type parser struct {
	reg  *cardtype.Registry
	card *Card
	cur  int // face index subsequent entries land on
}

// ParseScript reads one card script.
//
// The result is deliberately incomplete for a card with `CopyFaceFrom:`: that
// face is filled from another card, which needs the whole corpus, so
// [ResolvePlaceholders] finishes the job once every script is read.
//
// filename identifies the card in errors and in the corpus dump. Pass the
// script's base name without its extension.
func ParseScript(reg *cardtype.Registry, filename string, script []byte) (*Card, error) {
	if reg == nil {
		panic("carddb.ParseScript: nil type registry")
	}

	p := &parser{reg: reg, card: &Card{Filename: filename}}
	for n, raw := range strings.Split(string(script), "\n") {
		line := strings.TrimRight(raw, "\r")
		if line == "" || line[0] == '#' {
			continue
		}
		if err := p.line(line, &p.card.Faces[p.cur]); err != nil {
			return nil, fmt.Errorf("%w %s:%d: %w", ErrBadScript, filename, n+1, err)
		}
	}
	return p.card, nil
}

// line parses one entry onto the given face. face is a parameter rather than
// p.cur because `Variant:` re-parses the rest of its line onto a variant face.
func (p *parser) line(text string, face *Face) error {
	key, value, hasValue := cut(text)
	if hasValue {
		p.harvestTokens(value)
	}

	// Entries that build the card rather than a face, and so do not need one to
	// exist yet.
	switch key {
	case alternateKey:
		p.cur = FaceAlternate
		return nil
	case "SPECIALIZE":
		index, ok := specializeFaces[value]
		if !ok {
			return fmt.Errorf("SPECIALIZE: unknown colour %q", value)
		}
		p.cur = index
		return nil
	case "AlternateMode":
		mode, ok := SplitTypeFromScript(value)
		if !ok {
			return fmt.Errorf("AlternateMode: unknown mode %q", value)
		}
		p.card.SplitType = mode
		return nil
	case "CopyFaceFrom":
		if p.card.PlaceholderFaces == nil {
			p.card.PlaceholderFaces = make(map[int]string, 2)
		}
		p.card.PlaceholderFaces[p.cur] = value
		return nil
	case "Name":
		// A face with no ManaCost: line has no cost, which is not {0}: Java
		// coalesces its null to NO_COST, and the two print differently.
		*face = Face{Present: true, Name: value, ManaCost: mana.NoCost()}
		return nil
	case "AI":
		p.aiHint(value)
		return nil
	case "DeckHints":
		p.card.DeckHints = value
		return nil
	case "DeckNeeds":
		p.card.DeckNeeds = value
		return nil
	case "DeckHas":
		p.card.DeckHas = value
		return nil
	case "MeldPair":
		p.card.MeldWith = value
		return nil
	case "HandLifeModifier":
		p.card.HandLifeModifier = value
		return nil
	case "SETCOLORID":
		id, err := strconv.Atoi(value)
		if err != nil {
			return fmt.Errorf("SETCOLORID: %w", err)
		}
		p.card.SetColorID = id
		return nil
	}

	// Forge ignores these because its switch matches nothing. Crucible ignores
	// the same two knowingly, and the map's values say which card each is on.
	if _, ok := ignoredKeys[key]; ok {
		return nil
	}

	if !face.Present {
		return fmt.Errorf("%q before any Name: line", key)
	}
	return p.faceEntry(key, value, face)
}

// faceEntry parses the keys that belong to a face.
func (p *parser) faceEntry(key, value string, face *Face) error {
	switch key {
	case "ManaCost":
		cost, err := mana.Parse(value)
		if err != nil {
			return err
		}
		face.ManaCost = cost
	case "Types":
		face.Type = cardtype.Parse(p.reg, value)
	case "Oracle":
		face.Oracle = value
	case "PT":
		power, toughness, ok := strings.Cut(value, "/")
		if !ok || strings.Contains(toughness, "/") {
			return fmt.Errorf("PT: %q is not power/toughness", value)
		}
		// Both halves are normalised to a number here rather than on first
		// read, because Java normalises in setPtText and throws there. A card
		// whose P/T does not reduce to a number is a broken script, and it has
		// to fail while the script is in hand to say which one.
		if _, err := parsePT(power); err != nil {
			return fmt.Errorf("PT: power %q: %w", power, err)
		}
		if _, err := parsePT(toughness); err != nil {
			return fmt.Errorf("PT: toughness %q: %w", toughness, err)
		}
		face.Power, face.Toughness = power, toughness
	case "Colors":
		face.Colors, face.HasColors = parseColors(value), true
	case "Loyalty":
		face.InitialLoyalty = value
	case "Defense":
		face.Defense = value
	case "Lights":
		face.AttractionLights = value
	case "Text":
		face.NonAbilityText = value
	case "FlavorName":
		face.FlavorName = value
	case "A":
		face.Abilities = append(face.Abilities, value)
	case "K":
		face.Keywords = append(face.Keywords, value)
		p.partnerFrom(value)
	case "T":
		face.Triggers = append(face.Triggers, value)
	case "S":
		face.Statics = append(face.Statics, value)
	case "R":
		face.Replacements = append(face.Replacements, value)
	case "DeckRule":
		face.DeckRules = append(face.DeckRules, value)
	case "Draft":
		face.DraftActions = append(face.DraftActions, value)
	case "SVar":
		name, body, ok := strings.Cut(value, ":")
		if !ok {
			return fmt.Errorf("bad SVar %q: a name but no value", value)
		}
		face.SVars.Set(name, body)
	case "Variant":
		return p.variant(value, face)
	default:
		return fmt.Errorf("unknown key %q", key)
	}
	return nil
}

// variant re-parses the rest of the line onto a named variant of face. This is
// the parser's only recursion, and it is why line takes a face rather than
// reading p.cur.
func (p *parser) variant(value string, face *Face) error {
	name, rest, ok := strings.Cut(value, ":")
	if !ok || name == "" {
		return fmt.Errorf("bad Variant %q: no variant name", value)
	}
	if face.Variants == nil {
		face.Variants = make(map[string]*Face, 2)
	}
	target, ok := face.Variants[name]
	if !ok {
		target = &Face{Present: true, Name: face.Name, ManaCost: mana.NoCost()}
		face.Variants[name] = target
	}
	if err := p.line(rest, target); err != nil {
		return fmt.Errorf("in variant %q: %w", name, err)
	}
	if !containsString(p.card.SupportedVariants, name) {
		p.card.SupportedVariants = append(p.card.SupportedVariants, name)
	}
	return nil
}

// aiHint reads an `AI:` line. Only RemoveDeck is understood; Java ignores the
// rest just as quietly, and the values are additive across lines.
func (p *parser) aiHint(value string) {
	variable, rest, _ := strings.Cut(value, ":")
	if variable != "RemoveDeck" {
		return
	}
	for _, want := range strings.Split(rest, ",") {
		switch strings.TrimSpace(want) {
		case "All":
			p.card.RemovedFromAIDecks = true
		case "Random":
			p.card.RemovedFromRandomDecks = true
		case "NonCommander":
			p.card.RemovedFromNonCommanderDecks = true
		}
	}
}

// partnerFrom picks the partner out of a keyword line, which is where Java
// keeps it: `Partner with:<name>` and `Partner:<type>`.
func (p *parser) partnerFrom(keyword string) {
	// Java reads value.split(":")[1], so a keyword carrying a third segment --
	// "Partner with:Rory Williams:Rory" -- names only the second.
	if rest, ok := strings.CutPrefix(keyword, "Partner with:"); ok {
		name, _, _ := strings.Cut(rest, ":")
		p.card.PartnerWith = name
		return
	}
	if rest, ok := strings.CutPrefix(keyword, "Partner:"); ok {
		kind, _, _ := strings.Cut(rest, ":")
		p.card.PartnerType = kind
	}
}

// harvestTokens collects the token scripts named anywhere in a value.
//
// Java looks for the marker with indexOf(...) > 0, so a value that begins with
// it is missed. No corpus value does -- a param map always starts with its API
// key -- and the bound is reproduced rather than fixed, because this is the
// parser the oracle diff compares against (PORT-7).
func (p *parser) harvestTokens(value string) {
	at := strings.Index(value, tokenMarker)
	if at <= 0 {
		return
	}
	rest := strings.TrimSpace(value[at+len(tokenMarker):])
	if end := strings.Index(rest, "|"); end > 0 {
		rest = strings.TrimSpace(rest[:end])
	}
	p.card.Tokens = append(p.card.Tokens, strings.Split(rest, ",")...)
}

// parseColors reads a `Colors:` override: comma-separated colour words.
//
// Words are not trimmed and an unrecognised one contributes nothing, both of
// which are Java's ColorSet.fromNames. That combination is a trap and it has
// already caught a card: `flamewar_brash_veteran_flamewar_streetwise_operative`
// writes "black, red", and the leading space on " red" makes Forge read the
// face as mono-black. Reproduced rather than fixed, because the static database
// has to match the oracle before anything can be trusted to diverge from it
// (PORT-7).
func parseColors(value string) mana.Colors {
	var out mana.Colors
	for _, word := range strings.Split(value, ",") {
		if colour, ok := colorFromWord(word); ok {
			out |= colour
		}
	}
	return out
}

func colorFromWord(word string) (mana.Colors, bool) {
	switch strings.ToLower(word) {
	case "white":
		return mana.White, true
	case "blue":
		return mana.Blue, true
	case "black":
		return mana.Black, true
	case "red":
		return mana.Red, true
	case "green":
		return mana.Green, true
	case "colorless":
		return 0, true
	}
	return 0, false
}

// cut splits a line into key and value. A line with no colon is a bare key,
// which is what ALTERNATE is. The value is trimmed and the key is not, which
// is what Java does.
func cut(line string) (key, value string, hasValue bool) {
	at := strings.Index(line, ":")
	if at <= 0 {
		return line, "", false
	}
	return line[:at], strings.TrimSpace(line[at+1:]), true
}

func containsString(xs []string, want string) bool {
	for _, x := range xs {
		if x == want {
			return true
		}
	}
	return false
}

// IndexByFaceName indexes cards by every face name they carry, which is what a
// `CopyFaceFrom:` value refers to: `start_fire` copies from "Start", a face of
// the split card whose own name is "Start // Fire".
func IndexByFaceName(cards []*Card) map[string]*Card {
	out := make(map[string]*Card, len(cards)*2)
	for _, card := range cards {
		for _, name := range card.FaceNames() {
			if _, taken := out[name]; !taken {
				out[name] = card
			}
		}
	}
	return out
}

// ResolvePlaceholders fills every placeholder face. See [IndexByFaceName].
func ResolvePlaceholders(cards []*Card, byName map[string]*Card) error {
	for _, card := range cards {
		for index, from := range card.PlaceholderFaces {
			source, ok := byName[from]
			if !ok {
				return fmt.Errorf("%w %s: CopyFaceFrom: no card named %q", ErrBadScript, card.Filename, from)
			}
			copied := *source.Primary()
			copied.Variants = nil // a copied face does not inherit variants
			card.Faces[index] = copied
		}
	}
	return nil
}
