package compile

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
)

// StickerKind is StickerKind.java's enum: what a sticker does once placed.
type StickerKind uint8

// The four kinds of sticker (CR 123.2).
const (
	StickerName StickerKind = iota
	StickerArt
	StickerAbility
	StickerPT
)

var stickerKindNames = [...]string{StickerName: "Name", StickerArt: "Art", StickerAbility: "Ability", StickerPT: "PT"}

// String is the kind as a script spells it.
func (k StickerKind) String() string { return stickerKindNames[k] }

// StickerKindByName is StickerKind.smartValueOf: case-insensitive, false for a
// name that is none.
func StickerKindByName(s string) (StickerKind, bool) {
	for k, name := range stickerKindNames {
		if strings.EqualFold(strings.TrimSpace(s), name) {
			return StickerKind(k), true
		}
	}
	return 0, false
}

// Sticker is one slot of a sticker sheet: Sticker.java's parse of the SVar the
// sheet's `K:StickerSheet:` keyword names. Compiled once at load (ADR-0044,
// PORT-2); a placed sticker is identified by its sheet and Slot.
type Sticker struct {
	// Slot is the SVar's own name (STK1), which with the sheet is the
	// sticker's identity (StickerSheet.java:268-271).
	Slot string
	Kind StickerKind
	// Tickets is the cost in ticket counters; a malformed value reads 0
	// (Sticker.intParam, Sticker.java:50-60).
	Tickets int
	// Word is a Name sticker's word, Text an Ability or PT sticker's rules text.
	Word, Text string
	// Power and Toughness are a PT sticker's numbers (0 when malformed).
	Power, Toughness int
	// Keywords are an Ability sticker's `Keywords$` lines; Abilities, Triggers
	// and Statics the traits its `Ability$`, `Triggers$` and `Statics$` SVars
	// compile to, in the order listed.
	Keywords  []string
	Abilities []*Ability
	Triggers  []*Ability
	Statics   []*Ability
}

// Implemented is Sticker.isImplemented (Sticker.java:125-128): an Ability
// sticker with no body of any kind is hidden from the choice.
func (s *Sticker) Implemented() bool {
	if s.Kind != StickerAbility {
		return true
	}
	return len(s.Keywords) > 0 || len(s.Abilities) > 0 || len(s.Triggers) > 0 || len(s.Statics) > 0
}

// stickerSheet compiles `K:StickerSheet:STK1,STK2,...` (StickerSheet.readStickers,
// StickerSheet.java:122-150, :294-299): a slot whose SVar is missing or whose
// Kind$ names no kind is skipped without a word, as Java does.
func (c *faceCompiler) stickerSheet(slots string) ([]Sticker, error) {
	var out []Sticker
	for _, slot := range splitTrim(slots, ",") {
		body, ok := c.face.SVars.Get(slot)
		if !ok {
			continue
		}
		st := Sticker{Slot: slot}
		haveKind := false
		var abilities, triggers, statics []string
		for _, p := range vocab.SplitParams(body) {
			switch strings.ToLower(p.Key) {
			case "kind":
				st.Kind, haveKind = StickerKindByName(p.Value)
			case "tickets":
				st.Tickets = stickerInt(p.Value)
			case "word":
				st.Word = p.Value
			case "text":
				st.Text = p.Value
			case "power":
				st.Power = stickerInt(p.Value)
			case "toughness":
				st.Toughness = stickerInt(p.Value)
			case "keywords":
				st.Keywords = splitTrim(p.Value, ",")
			case "ability":
				abilities = splitTrim(p.Value, ",")
			case "triggers":
				triggers = splitTrim(p.Value, ",")
			case "statics":
				statics = splitTrim(p.Value, ",")
			}
		}
		if !haveKind {
			continue
		}
		for _, group := range []struct {
			names  []string
			record Record
			target *[]*Ability
		}{
			{abilities, Spell, &st.Abilities},
			{triggers, Trigger, &st.Triggers},
			{statics, StaticEffect, &st.Statics},
		} {
			for _, name := range group.names {
				ab, err := c.stickerTrait(name, group.record)
				if err != nil {
					return nil, fmt.Errorf("sticker %s: %w", slot, err)
				}
				*group.target = append(*group.target, ab)
			}
		}
		out = append(out, st)
	}
	return out, nil
}

// stickerTrait compiles one SVar a sticker grants (AppliedSticker.getGrantedTraits,
// AppliedSticker.java:110+): an ability, a trigger or a static, with the
// sub-abilities it names followed as for any other line.
func (c *faceCompiler) stickerTrait(name string, want Record) (*Ability, error) {
	if c.open[name] {
		return nil, &cycleError{key: "Sticker", name: name}
	}
	body, ok := c.face.SVars.Get(name)
	if !ok {
		return nil, fmt.Errorf("%w: sticker names %q", ErrMissingSVar, name)
	}
	c.open[name] = true
	defer delete(c.open, name)
	ab, err := c.line(body, want)
	if err != nil {
		return nil, fmt.Errorf("in %q: %w", name, err)
	}
	ab.SVar = name
	return ab, nil
}

// stickerInt is Sticker.intParam: a malformed number reads 0.
func stickerInt(s string) int {
	n, err := strconv.Atoi(strings.TrimSpace(s))
	if err != nil {
		return 0
	}
	return n
}
