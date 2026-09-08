// Ported from forge-core/src/main/java/forge/card/CardType.java (CoreType and
// Supertype).
// Deviations recorded in docs/crucible/porting/port-log/card-type.md.

package cardtype

// CoreType is a card type proper: Creature, Instant, Land. The set is closed —
// only the rules add to it — so it is an enum rather than a string.
//
// Declaration order matches Java's and decides print order: Kindred first,
// because a card that is both Kindred and Creature prints "Kindred Creature".
type CoreType uint8

// The fifteen core types, in Java's CardType.CoreType order.
const (
	Kindred CoreType = iota
	Artifact
	Battle
	Conspiracy
	Enchantment
	Creature
	Dungeon
	Instant
	Land
	Phenomenon
	Plane
	Planeswalker
	Scheme
	Sorcery
	Vanguard

	numCoreTypes = int(Vanguard) + 1
)

type coreTypeInfo struct {
	name      string
	plural    string
	permanent bool
}

var coreTypeTable = [numCoreTypes]coreTypeInfo{
	Kindred:      {"Kindred", "kindreds", false},
	Artifact:     {"Artifact", "artifacts", true},
	Battle:       {"Battle", "battles", true},
	Conspiracy:   {"Conspiracy", "conspiracies", false},
	Enchantment:  {"Enchantment", "enchantments", true},
	Creature:     {"Creature", "creatures", true},
	Dungeon:      {"Dungeon", "dungeons", false},
	Instant:      {"Instant", "instants", false},
	Land:         {"Land", "lands", true},
	Phenomenon:   {"Phenomenon", "phenomenons", false},
	Plane:        {"Plane", "planes", false},
	Planeswalker: {"Planeswalker", "planeswalkers", true},
	Scheme:       {"Scheme", "schemes", false},
	Sorcery:      {"Sorcery", "sorceries", false},
	Vanguard:     {"Vanguard", "vanguards", false},
}

// String returns the type as a card prints it: "Creature".
func (t CoreType) String() string { return coreTypeTable[t].name }

// Plural returns the form used by rules text and valid strings: "creatures".
func (t CoreType) Plural() string { return coreTypeTable[t].plural }

// IsPermanent reports whether a card of this type stays on the battlefield.
func (t CoreType) IsPermanent() bool { return coreTypeTable[t].permanent }

// CoreTypeFromName looks up a core type by its printed name, which is
// case-sensitive because card scripts are.
func CoreTypeFromName(name string) (CoreType, bool) {
	for i, info := range coreTypeTable {
		if info.name == name {
			return CoreType(i), true
		}
	}
	return 0, false
}

// Supertype is the word before the card type: Legendary, Basic, Snow.
type Supertype uint8

// The seven supertypes, in Java's CardType.Supertype order, which is the order
// they print in.
const (
	Basic Supertype = iota
	Elite
	Host
	Legendary
	Snow
	Ongoing
	World

	numSupertypes = int(World) + 1
)

var supertypeNames = [numSupertypes]string{
	Basic:     "Basic",
	Elite:     "Elite",
	Host:      "Host",
	Legendary: "Legendary",
	Snow:      "Snow",
	Ongoing:   "Ongoing",
	World:     "World",
}

// String returns the supertype as a card prints it: "Legendary".
func (t Supertype) String() string { return supertypeNames[t] }

// SupertypeFromName looks up a supertype by its printed name.
func SupertypeFromName(name string) (Supertype, bool) {
	for i, known := range supertypeNames {
		if known == name {
			return Supertype(i), true
		}
	}
	return 0, false
}
