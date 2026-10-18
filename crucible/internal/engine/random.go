// Random picks with Java's exact draw sequence, so a seeded game replays
// the oracle's choices (ADR-0006).
//
// Ported from forge-core/src/main/java/forge/util/Aggregates.java's
// random(Iterable, count).

package engine

// randomSample is Aggregates.random(Iterable, count): reservoir sampling
// over indices 0..n-1, answering the sampled indices in reservoir order.
func (g *Game) randomSample(n, count int) []int {
	var out []int
	for i := 1; i <= n; i++ {
		if i <= count {
			out = append(out, i-1)
			continue
		}
		if j := int(g.rand.Int32n(int32(i))); j < count {
			out[j] = i - 1
		}
	}
	return out
}

// randomInt is Aggregates.randomInt (Aggregates.java:191-193): nextInt(hi -
// lo + 1) + lo. The caller guarantees lo <= hi.
func (g *Game) randomInt(lo, hi int) int {
	return int(g.rand.Int32n(int32(hi-lo+1))) + lo
}

// randomIndex is Aggregates.random over a List of n: no draw for zero or one
// element, otherwise nextInt(n). -1 when n is zero.
func (g *Game) randomIndex(n int) int {
	switch n {
	case 0:
		return -1
	case 1:
		return 0
	}
	return int(g.rand.Int32n(int32(n)))
}
