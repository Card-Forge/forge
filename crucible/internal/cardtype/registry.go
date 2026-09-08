// Ported from forge-core/src/main/java/forge/card/CardType.java (Constant and
// Helper.parseTypes) plus the caller in
// forge-gui/src/main/java/forge/model/FModel.java (loadDynamicGamedata).
// Deviations recorded in docs/crucible/porting/port-log/card-type.md.

package cardtype

import (
	"bufio"
	"errors"
	"fmt"
	"io"
	"sort"
	"strings"
)

// Category is one section of the subtype vocabulary. A subtype is legal on a
// card only if its category matches one of the card's core types: Equipment on
// an artifact, Elf on a creature, Aura on an enchantment.
type Category uint8

// The ten subtype categories, named after the sections of TypeLists.txt.
const (
	CategoryBasic Category = iota
	CategoryLand
	CategoryCreature
	CategorySpell
	CategoryEnchantment
	CategoryArtifact
	CategoryPlaneswalker
	CategoryDungeon
	CategoryBattle
	CategoryPlanar

	numCategories = int(CategoryPlanar) + 1
)

var categorySections = [numCategories]string{
	CategoryBasic:        "BasicTypes",
	CategoryLand:         "LandTypes",
	CategoryCreature:     "CreatureTypes",
	CategorySpell:        "SpellTypes",
	CategoryEnchantment:  "EnchantmentTypes",
	CategoryArtifact:     "ArtifactTypes",
	CategoryPlaneswalker: "WalkerTypes",
	CategoryDungeon:      "DungeonTypes",
	CategoryBattle:       "BattleTypes",
	CategoryPlanar:       "PlanarTypes",
}

// String returns the section name this category is loaded from.
func (c Category) String() string { return categorySections[c] }

// Registry is the subtype vocabulary, loaded from Forge's TypeLists.txt.
//
// It is built once at load and never written afterwards, so one instance is
// shared by every game (GO-2). Java keeps the same data in mutable static sets
// on CardType.Constant, guarded by a "have I loaded yet" flag; that singleton
// is exactly what this port does not reproduce.
type Registry struct {
	members  [numCategories]map[string]bool
	multi    []string // subtypes containing a space, longest first
	plural   map[string]string
	singular map[string]string
}

// ErrBadTypeList reports a malformed type-list file.
var ErrBadTypeList = errors.New("bad type list")

// LoadRegistry reads Forge's TypeLists.txt.
//
// The format is sections introduced by "[SectionName]", one type per line,
// optionally "Singular:Plural". Sections the loader does not know are skipped,
// matching Java, which switches on the section name and ignores the rest.
func LoadRegistry(r io.Reader) (*Registry, error) {
	reg := &Registry{
		plural:   make(map[string]string, 512),
		singular: make(map[string]string, 512),
	}
	for i := range reg.members {
		reg.members[i] = make(map[string]bool, 64)
	}
	for t := CoreType(0); int(t) < numCoreTypes; t++ {
		reg.plural[t.String()] = t.Plural()
		reg.singular[t.Plural()] = t.String()
	}

	current, known := Category(0), false
	sc := bufio.NewScanner(r)
	for line := 1; sc.Scan(); line++ {
		text := strings.TrimSpace(sc.Text())
		if text == "" {
			continue
		}
		if name, ok := sectionHeader(text); ok {
			current, known = categoryFromSection(name)
			continue
		}
		if !known {
			// Either a stray line before the first section, or a section this
			// port has no use for.
			continue
		}

		singular, plural, hasPlural := strings.Cut(text, ":")
		if singular == "" || (hasPlural && plural == "") {
			return nil, fmt.Errorf("%w: line %d: %q", ErrBadTypeList, line, text)
		}
		reg.members[current][singular] = true
		if hasPlural {
			reg.plural[singular] = plural
			reg.singular[plural] = singular
		}
		if strings.Contains(singular, " ") {
			reg.multi = append(reg.multi, singular)
		}
	}
	if err := sc.Err(); err != nil {
		return nil, fmt.Errorf("%w: %w", ErrBadTypeList, err)
	}
	if len(reg.members[CategoryCreature]) == 0 {
		return nil, fmt.Errorf("%w: no creature types; wrong file?", ErrBadTypeList)
	}

	// Longest first, so "Time Lord" wins over a hypothetical "Time". Java scans
	// a HashSet and takes the first prefix match, which makes the result depend
	// on hash order; deciding by length is deterministic (GO-12).
	sort.Slice(reg.multi, func(i, j int) bool {
		if len(reg.multi[i]) != len(reg.multi[j]) {
			return len(reg.multi[i]) > len(reg.multi[j])
		}
		return reg.multi[i] < reg.multi[j]
	})
	return reg, nil
}

func sectionHeader(line string) (string, bool) {
	if len(line) >= 2 && line[0] == '[' && line[len(line)-1] == ']' {
		return line[1 : len(line)-1], true
	}
	return "", false
}

func categoryFromSection(name string) (Category, bool) {
	for i, section := range categorySections {
		if section == name {
			return Category(i), true
		}
	}
	return 0, false
}

// Is reports whether name is a subtype of the given category.
func (r *Registry) Is(c Category, name string) bool { return r.members[c][name] }

// IsLandType reports whether name is a land subtype. Basic land types count,
// which is why this is not a plain [Registry.Is] call.
func (r *Registry) IsLandType(name string) bool {
	return r.members[CategoryLand][name] || r.members[CategoryBasic][name]
}

// IsCreatureType reports whether name is a creature subtype.
func (r *Registry) IsCreatureType(name string) bool { return r.members[CategoryCreature][name] }

// Plural returns the plural of a type name, or the name unchanged if the
// vocabulary has no plural for it.
func (r *Registry) Plural(name string) string {
	if p, ok := r.plural[name]; ok {
		return p
	}
	return name
}

// Singular reverses [Registry.Plural].
func (r *Registry) Singular(name string) string {
	if s, ok := r.singular[name]; ok {
		return s
	}
	return name
}

// Count returns how many subtypes a category holds. Used by tests and by the
// load-time report; the engine has no reason to ask.
func (r *Registry) Count(c Category) int { return len(r.members[c]) }

// multiwordPrefix returns the longest multiword subtype that starts text at a
// word boundary, so "Time Lord Rogue" yields "Time Lord" and not "Time".
func (r *Registry) multiwordPrefix(text string) (string, bool) {
	for _, candidate := range r.multi {
		if !strings.HasPrefix(text, candidate) {
			continue
		}
		if len(text) == len(candidate) || text[len(candidate)] == ' ' {
			return candidate, true
		}
	}
	return "", false
}
