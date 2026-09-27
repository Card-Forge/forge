// KeywordMod: Layer 6's own continuous ability-granting effects, the
// keyword counterpart to TypeMod's/ColorMod's own type-line/color layers.

package engine

// KeywordMod is the continuous keyword changes currently affecting one card
// -- CR 613.4's Layer 6, folded in Timestamp order (Card.KeywordLines) the
// way Java's KeywordsChange.applyKeywords runs over its TreeMap: an effect
// that removes a keyword only removes what the effects before it left.
type KeywordMod struct {
	effects []KeywordEffect
}

// KeywordEffect is one continuous effect's own keyword change: every
// keyword line it grants, verbatim -- exactly the form a real K: line would
// carry ("Ward:2", "First Strike", "Protection:..."), keyword.Parse's own
// job to split further at HasKeyword's own query time -- and what it takes
// away first. RemoveKeywords drops every line starting with one of its
// entries (KeywordCollection.remove's own startsWith match); RemoveAll drops
// every line. Removal applies before the effect's own additions, Java's
// applyKeywords order. A resolved Animate/Debuff line and a static
// RemoveKeyword$/RemoveAllAbilities$ line (applyOneContinuousKeyword,
// continuous.go) are the sources of a removal.
type KeywordEffect struct {
	Timestamp      uint64
	AddKeywords    []string
	RemoveKeywords []string
	RemoveAll      bool
}

// Add records one continuous effect. Order does not matter here, the same
// as it never has for PT.Add/TypeMod.Add/ColorMod.Add -- folding (or, for
// this one, plain membership) does not depend on insertion order.
func (km *KeywordMod) Add(e KeywordEffect) { km.effects = append(km.effects, e) }

// Clear removes every effect, which Move calls when a card leaves the
// battlefield -- PT.Clear's own reason applies identically here.
func (km *KeywordMod) Clear() { km.effects = nil }

// clone is KeywordMod's half of Game.Clone -- PT.clone's own reasoning: a
// shared backing array would let a push on the clone alias the original.
func (km KeywordMod) clone() KeywordMod {
	return KeywordMod{effects: append([]KeywordEffect(nil), km.effects...)}
}
