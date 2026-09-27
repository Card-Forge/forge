// KeywordMod: Layer 6's own continuous ability-granting effects, the
// keyword counterpart to TypeMod's/ColorMod's own type-line/color layers.

package engine

import (
	"sort"
	"strings"
)

// KeywordMod is the continuous keyword changes currently affecting one
// entity -- a Card or a Player alike, CR 613.4's Layer 6, folded in
// Timestamp order (fold, below) the way Java's KeywordsChange.applyKeywords
// runs over its TreeMap: an effect that removes a keyword only removes what
// the effects before it left. Card.KeywordLines folds onto its own printed
// face; Player.KeywordLines (player.go) has no face to fold onto, since
// every line a player carries came from a continuous effect -- Leyline of
// Sanctity's own `Affected$ You | AddKeyword$ Hexproof`, applyOneContinuous
// Keyword's own player branch (continuous.go).
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
// applyKeywords order. A resolved Animate/Debuff line is the only source of
// a removal today: applyOneContinuousKeyword (continuous.go) still skips a
// static line naming RemoveKeyword$/RemoveAllAbilities$.
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

// Clear removes every effect -- Move calls it when a card leaves the
// battlefield, applyContinuousKeyword calls it for every card and player at
// the top of every Layer 6 pass before rebuilding it fresh (PT.Clear's own
// reason applies identically here).
func (km *KeywordMod) Clear() { km.effects = nil }

// clone is KeywordMod's half of Game.Clone -- PT.clone's own reasoning: a
// shared backing array would let a push on the clone alias the original.
func (km KeywordMod) clone() KeywordMod {
	return KeywordMod{effects: append([]KeywordEffect(nil), km.effects...)}
}

// fold applies every one of km's own effects on top of base, in Timestamp
// order, the way Java's KeywordsChange.applyKeywords runs over its own
// TreeMap: each effect's removals apply before its own additions. Card.
// KeywordLines calls this with its printed face's own lines as base;
// Player.KeywordLines calls it with nil, since a player has no printed
// face to start from -- every line it carries is Layer 6's doing.
func (km KeywordMod) fold(base []string) []string {
	lines := base
	if len(km.effects) == 0 {
		return lines
	}
	effects := append([]KeywordEffect(nil), km.effects...)
	sort.SliceStable(effects, func(i, j int) bool { return effects[i].Timestamp < effects[j].Timestamp })
	for _, e := range effects {
		switch {
		case e.RemoveAll:
			lines = nil
		case len(e.RemoveKeywords) > 0:
			kept := lines[:0:0]
			for _, line := range lines {
				if !hasAnyPrefix(line, e.RemoveKeywords) {
					kept = append(kept, line)
				}
			}
			lines = kept
		}
		lines = append(lines, e.AddKeywords...)
	}
	return lines
}

// hasAnyPrefix reports whether s starts with any of prefixes --
// KeywordCollection.remove's own startsWith match (Java), moved here from
// card.go once fold's only caller of it stopped being Card.KeywordLines
// alone.
func hasAnyPrefix(s string, prefixes []string) bool {
	for _, p := range prefixes {
		if strings.HasPrefix(s, p) {
			return true
		}
	}
	return false
}
