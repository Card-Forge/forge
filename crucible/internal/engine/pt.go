// PT: Layer 7's continuous power/toughness effects, the mutable part of a
// card Card.Power/Toughness (card.go) folds against the printed value.

package engine

// PT is the continuous power/toughness effects currently affecting one
// card -- CHARACTERISTIC, SETPT and MODIFYPT, Layer 7's own three
// sub-layers (CR 613.4). Counters are not here: CR 613.4 applies them
// after Layer 7, not as a layer of their own, and Card.Counters already
// tracks them -- Card.Power/Toughness reads both.
type PT struct {
	effects []PTEffect
	// switches counts the Layer 7d (CR 613.4d) "power and toughness are
	// switched" effects in force. Forge has no layer value for it
	// (StaticAbilityLayer's SWITCHPT is commented out): Card.getNetPower
	// swaps the whole unswitched total when the keyword's amount is odd
	// (Card.java:4448), so two switches cancel and only the parity matters.
	switches int
}

// PTEffect is one continuous effect's contribution to a card's power
// and/or toughness. Layer decides how it combines with whatever came
// before: LayerCharacteristic and LayerSetPT replace it, LayerModifyPT
// adds to it (CR 613.4) -- Card.Power/Toughness is what applies that rule,
// this only records the effect.
//
// HasPower and HasToughness matter only for LayerCharacteristic/LayerSetPT:
// a real corpus SetPower/SetToughness line sets just one dimension far more
// often than both together (68 SetPower-only, 9 SetToughness-only lines,
// the removed port log's "Continuous effects" section), and the
// dimension it leaves alone must stay exactly what came before it, not
// reset to zero -- foldPT (card.go) only overwrites a dimension one of
// these two flags is true for. LayerModifyPT needs neither flag: adding
// zero to a dimension an effect does not mention is already a no-op, the
// reason every ModifyPT-only caller (pt_test.go's own) can leave both
// false.
type PTEffect struct {
	Layer                  StaticAbilityLayer
	Timestamp              uint64
	Power, Toughness       int
	HasPower, HasToughness bool
}

// Add records one continuous effect. Order does not matter here: folding
// sorts by Layer then Timestamp, not insertion order.
func (pt *PT) Add(e PTEffect) { pt.effects = append(pt.effects, e) }

// Clear removes every effect, which Move calls when a card leaves the
// battlefield: an effect that only applied there stops the moment it does.
// This port has no duration tracking yet (an "until end of turn" pump
// wearing off on its own is a separate, unbuilt mechanic,
// game-state.md's "Not ported yet"), so leaving the battlefield is the one
// case this clears today.
func (pt *PT) Clear() { pt.effects, pt.switches = nil, 0 }

// AddSwitch records one Layer 7d switch of power and toughness.
func (pt *PT) AddSwitch() { pt.switches++ }

// Switched reports whether power and toughness are currently swapped: an odd
// number of switch effects.
func (pt PT) Switched() bool { return pt.switches%2 != 0 }

// size and truncate bracket the dependency search's trial application of a
// Layer 7b static (typeLayerOps' own reasoning).
func (pt *PT) size() int { return len(pt.effects) }

// truncate drops every effect added since size returned n.
func (pt *PT) truncate(n int) { pt.effects = pt.effects[:n] }

// clone is PT's half of Game.Clone: a shared backing array would let a
// push on the clone alias the original, the same reasoning stack.go's own
// clone has.
func (pt PT) clone() PT {
	return PT{effects: append([]PTEffect(nil), pt.effects...), switches: pt.switches}
}
