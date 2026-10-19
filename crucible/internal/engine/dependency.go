// CR 613.8 dependency ordering within a layer, ported from
// GameAction.findStaticAbilityToApply (GameAction.java:1273-1381) and the
// loop that calls it (:1129-1165). ADR-0025, Decision 2.

package engine

import (
	"maps"
	"slices"
	"strings"
)

// layerOps is one dependency-checked layer as the search sees it: apply is
// the layer's own applier for one static, affected is what that static
// would apply to now (StaticAbilityContinuous.getAffectedCards), and
// mark/undo bracket a trial application of another static.
type layerOps struct {
	apply    func(ls layerStatic)
	affected func(ls layerStatic) ([]CardID, bool)
	mark     func() layerMark
	undo     func(mark layerMark)
}

// applyInDependencyOrder applies statics -- one layer's, already in
// effectOrder (continuousStatics) -- one at a time, each time choosing the
// next with findStaticToApply (CR 613.8c: the order is re-evaluated after
// every application). A characteristic-defining line is applied in its
// effectOrder place with no search, as Java's loop does
// (GameAction.java:1131-1133).
func applyInDependencyOrder(g *Game, statics []layerStatic, ops layerOps) {
	remaining := slices.Clone(statics)
	for len(remaining) > 0 {
		i := 0
		if !hasParamOn(remaining[0].s, "CharacteristicDefining") {
			i = findStaticToApply(g, remaining, ops)
		}
		next := remaining[i]
		remaining = slices.Delete(remaining, i, i+1)
		// A static whose host lost its printed abilities to an effect
		// already applied in this layer no longer exists
		// (applyContinuousAbilityBefore returns null for it).
		if staticExists(g, next) {
			ops.apply(next)
		}
	}
}

// findStaticToApply is findStaticAbilityToApply: the index in remaining of
// the static to apply next.
//
// CR 613.8a, as Java tests it: a static depends on another when applying the
// other, on trial, changes whether the first still exists (its host losing
// its printed abilities, CR 305.7: Blood Moon on Urborg) or what it applies
// to (its affected cards, compared in order, as Iterators.elementsEqual
// does). Java's third test, "changes what it does", is for GainControl$
// player lists only, where this port resolves only You. RemoveAllAbilities$
// removes keywords here, not statics, so it never changes existence.
//
// The edges are S -> O for "S depends on O". Every edge on a cycle is
// dropped (CR 613.8b: a dependency loop is ignored; Java removes the edges
// of every simple cycle, which is exactly the edges whose endpoints share a
// strongly connected component). Of the statics left with no outgoing edge,
// the earliest timestamp wins, ties in effectOrder.
//
// Java's two shortcuts are kept, since this runs every state-based-action
// pass: one static left, or the first one being from a resolved effect
// (CR 611.2c, an IsEffect host that is not an emblem), returns it at once;
// and when the first static depends on nothing, it is returned before the
// rest of the graph is built.
func findStaticToApply(g *Game, remaining []layerStatic, ops layerOps) int {
	if len(remaining) == 1 || resolvedStatic(g, remaining[0]) {
		return 0
	}
	n := len(remaining)
	deps := make([][]bool, n)
	for i := range deps {
		deps[i] = make([]bool, n)
	}
	anyEdge := false
	for i, s := range remaining {
		if resolvedStatic(g, s) {
			continue
		}
		existed := staticExists(g, s)
		before, hasAffected := ops.affected(s)
		for j, o := range remaining {
			if i == j {
				continue
			}
			mark := ops.mark()
			carried := maps.Clone(g.layerAffected)
			ops.apply(o)
			dependent := existed != staticExists(g, s)
			if !dependent && hasAffected {
				after, ok := ops.affected(s)
				dependent = ok && !slices.Equal(before, after)
			}
			ops.undo(mark)
			g.layerAffected = carried
			if dependent {
				deps[i][j] = true
				anyEdge = true
			}
		}
		if i == 0 && !anyEdge {
			return 0
		}
	}

	// CR 613.8b: drop every edge whose endpoints reach each other.
	reach := transitiveClosure(deps)
	best := -1
	for i := range remaining {
		dependent := false
		for j := range remaining {
			if deps[i][j] && !reach[j][i] {
				dependent = true
				break
			}
		}
		if dependent {
			continue
		}
		if best < 0 || g.Card(remaining[i].host).Timestamp < g.Card(remaining[best].host).Timestamp {
			best = i
		}
	}
	if best < 0 {
		// Unreachable: once cycle edges are gone the graph is acyclic and
		// has a sink. Fall back to effectOrder rather than panic (GO-7).
		return 0
	}
	return best
}

// staticExists is Java's `stAb.getHostCard().getStaticAbilities()
// .contains(stAb)`: every static a layer walks is printed or text-gained
// (traitDef), and those go away when the host's printed traits do.
func staticExists(g *Game, ls layerStatic) bool {
	return g.staticLive(g.Card(ls.host), ls.s)
}

// resolvedStatic is Java's isResolved: a static on an effect card (an
// immutable host that is not an emblem; this port creates no emblems, see
// baseMatches) always affects the same objects the same way (CR
// 611.2c), so it never depends on anything.
func resolvedStatic(g *Game, ls layerStatic) bool {
	return g.Card(ls.host).IsEffect
}

// transitiveClosure is reach[i][j]: j is reachable from i over one or more
// edges of deps (Floyd-Warshall; a layer holds a handful of statics).
func transitiveClosure(deps [][]bool) [][]bool {
	n := len(deps)
	reach := make([][]bool, n)
	for i := range deps {
		reach[i] = slices.Clone(deps[i])
	}
	for k := 0; k < n; k++ {
		for i := 0; i < n; i++ {
			if !reach[i][k] {
				continue
			}
			for j := 0; j < n; j++ {
				if reach[k][j] {
					reach[i][j] = true
				}
			}
		}
	}
	return reach
}

// staticsWithAny keeps the statics naming at least one of keys -- the layer
// a Mode$ Continuous line belongs to is decided by which params it carries
// (StaticAbility.generateLayer, StaticAbility.java:139-187).
func staticsWithAny(statics []layerStatic, keys ...string) []layerStatic {
	var out []layerStatic
	for _, ls := range statics {
		if !strings.EqualFold(ls.s.Name, "Continuous") {
			continue
		}
		for _, k := range keys {
			if hasParamOn(ls.s, k) {
				out = append(out, ls)
				break
			}
		}
	}
	return out
}

// typeLayerKeys, keywordLayerKeys and controlLayerKeys are the params that
// put a line in Layer 4, the keyword half of Layer 6, and Layer 2
// (StaticAbility.java:139-168).
var (
	typeLayerKeys = []string{
		"AddType", "RemoveType", "AddAllCreatureTypes", "RemoveCardTypes", "RemoveSubTypes",
		"RemoveSuperTypes", "RemoveLandTypes", "RemoveCreatureTypes", "RemoveArtifactTypes",
		"RemoveEnchantmentTypes",
	}
	keywordLayerKeys = []string{"AddKeyword", "RemoveKeyword", "CantHaveKeyword", "RemoveAllAbilities", "RemoveNonManaAbilities", "AddTrigger", "AddAbility", "AddStaticAbility", "AddReplacementEffect", "GainsStickerAbilitiesOf", "GainsStickerAbilitiesOfDefined"}
	controlLayerKeys = []string{"GainControl"}
)

// layerMark is what a trial application can change and must give back: the
// effect-list sizes of every card (and player), and the trait grants and
// removal state of every card.
type layerMark struct {
	sizes   []int
	grants  [][]traitGrant
	removal []abilityRemoval
}

// markCards records size(c) for every card in the arena.
func markCards(g *Game, size func(c *Card) int) []int {
	out := make([]int, len(g.cards))
	for i := 1; i < len(g.cards); i++ {
		out[i] = size(&g.cards[i])
	}
	return out
}

// typeLayerOps, keywordLayerOps and controlLayerOps are layerOps for the
// three appliers that order their statics by dependency.
func typeLayerOps(g *Game) layerOps {
	return layerOps{
		apply: func(ls layerStatic) { applyOneContinuousType(g, g.Card(ls.host), ls.amounts, ls.s) },
		affected: func(ls layerStatic) ([]CardID, bool) {
			return g.staticPeekAffected(g.Card(ls.host), ls.s)
		},
		mark: func() layerMark { return layerMark{sizes: markCards(g, func(c *Card) int { return c.TypeMod.size() })} },
		undo: func(m layerMark) {
			for i := 1; i < len(m.sizes); i++ {
				g.cards[i].TypeMod.truncate(m.sizes[i])
			}
		},
	}
}

// abilitiesLayerOps is the whole of Layer 6: keyword grants and removals,
// trait grants (AddTrigger$, AddAbility$) and RemoveAllAbilities$ /
// RemoveNonManaAbilities$, applied together so one timestamp order governs
// them (StaticAbility.generateLayer's ABILITIES set).
func abilitiesLayerOps(g *Game) layerOps {
	return layerOps{
		apply: func(ls layerStatic) {
			h := g.Card(ls.host)
			applyOneContinuousKeyword(g, h, ls.amounts, ls.s)
			// A static this one granted applies in this layer too, and so does
			// anything it grants in turn.
			for work := applyOneContinuousTraits(g, h, ls.amounts, ls.s); len(work) > 0; work = work[1:] {
				gs := work[0]
				gh := g.Card(gs.host)
				applyOneContinuousKeyword(g, gh, gs.amounts, gs.s)
				work = append(work, applyOneContinuousTraits(g, gh, gs.amounts, gs.s)...)
			}
		},
		affected: func(ls layerStatic) ([]CardID, bool) {
			return g.staticPeekAffected(g.Card(ls.host), ls.s)
		},
		mark: func() layerMark {
			m := layerMark{sizes: markCards(g, func(c *Card) int { return c.KeywordMod.size() })}
			for _, pid := range g.Players() {
				m.sizes = append(m.sizes, g.Player(pid).KeywordMod.size())
			}
			m.grants = make([][]traitGrant, len(g.cards))
			m.removal = make([]abilityRemoval, len(g.cards))
			for i := 1; i < len(g.cards); i++ {
				m.grants[i], m.removal[i] = g.cards[i].traitGrants, g.cards[i].abilityRemoval
			}
			return m
		},
		undo: func(m layerMark) {
			for i := 1; i < len(g.cards); i++ {
				g.cards[i].KeywordMod.truncate(m.sizes[i])
				g.cards[i].traitGrants, g.cards[i].abilityRemoval = m.grants[i], m.removal[i]
			}
			for k, pid := range g.Players() {
				g.Player(pid).KeywordMod.truncate(m.sizes[len(g.cards)+k])
			}
		},
	}
}

func controlLayerOps(g *Game) layerOps {
	return layerOps{
		apply: func(ls layerStatic) { applyOneContinuousControl(g, g.Card(ls.host), ls.s) },
		affected: func(ls layerStatic) ([]CardID, bool) {
			return g.staticPeekAffected(g.Card(ls.host), ls.s)
		},
		mark: func() layerMark {
			return layerMark{sizes: markCards(g, func(c *Card) int { return c.ControlMod.size() })}
		},
		undo: func(m layerMark) {
			for i := 1; i < len(m.sizes); i++ {
				g.cards[i].ControlMod.truncate(m.sizes[i])
			}
		},
	}
}

// setPTLayerOps is Layer 7b, the only P/T sublayer in
// CONTINUOUS_LAYERS_WITH_DEPENDENCY (StaticAbilityLayer.java:49): a line
// whose Affected$ reads power or toughness (Andrios, Roaming Explorer's
// basePowerEQ4+baseToughnessEQ3) depends on a setter that changes either.
func setPTLayerOps(g *Game) layerOps {
	return layerOps{
		apply: func(ls layerStatic) {
			applyOneContinuousPT(g, g.Card(ls.host), ls.amounts, ls.s, LayerSetPT)
		},
		affected: func(ls layerStatic) ([]CardID, bool) {
			return g.staticPeekAffected(g.Card(ls.host), ls.s)
		},
		mark: func() layerMark { return layerMark{sizes: markCards(g, func(c *Card) int { return c.PT.size() })} },
		undo: func(m layerMark) {
			for i := 1; i < len(m.sizes); i++ {
				g.cards[i].PT.truncate(m.sizes[i])
			}
		},
	}
}
