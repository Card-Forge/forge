// Package javarand reproduces java.util.Random bit-for-bit.
//
// It exists for one reason: differential testing against the Forge engine
// (ADR-0010). Scenario fixtures run the same seed through both engines and must
// shuffle identically, which is only possible if Go reproduces Java's generator
// exactly.
//
// It is NOT the production generator. Its 48-bit state reaches 2^48 values
// against 60! deck permutations, which is a needless ceiling on a tool whose
// output is distributional claims about card draw. Production uses
// math/rand/v2 (ADR-0006).
//
// The algorithm is fully specified by the java.util.Random javadoc, which is
// what makes an exact port possible at all.
package javarand

const (
	multiplier = 0x5DEECE66D
	addend     = 0xB
	mask       = (1 << 48) - 1
)

// Rand is a java.util.Random. The zero value is not usable; call New.
//
// Not safe for concurrent use, deliberately: each game owns its own streams and
// nothing is shared between games (ADR-0005).
type Rand struct {
	seed int64
}

// New returns a Rand seeded as java.util.Random's constructor does.
func New(seed int64) *Rand {
	return &Rand{seed: (seed ^ multiplier) & mask}
}

// SetSeed matches java.util.Random.setSeed.
func (r *Rand) SetSeed(seed int64) {
	r.seed = (seed ^ multiplier) & mask
}

// next matches the protected java.util.Random.next(int bits).
func (r *Rand) next(bits uint) int32 {
	r.seed = (r.seed*multiplier + addend) & mask
	return int32(r.seed >> (48 - bits))
}

// Int32 matches nextInt().
func (r *Rand) Int32() int32 { return r.next(32) }

// Int32n matches nextInt(int bound), including its power-of-two special case
// and its rejection loop. Panics on bound <= 0, as Java throws.
func (r *Rand) Int32n(bound int32) int32 {
	if bound <= 0 {
		panic("javarand: bound must be positive")
	}
	// Power of two: Java takes the high bits rather than the remainder.
	if bound&-bound == bound {
		return int32((int64(bound) * int64(r.next(31))) >> 31)
	}
	// Rejection loop, rejecting the values that would bias the modulus.
	for {
		bits := r.next(31)
		val := bits % bound
		if bits-val+(bound-1) >= 0 {
			return val
		}
	}
}

// Int64 matches nextLong(). Java composes two 32-bit draws, and the high word
// is signed, so the shift must be arithmetic.
func (r *Rand) Int64() int64 {
	hi := int64(r.next(32)) << 32
	return hi + int64(r.next(32))
}

// Bool matches nextBoolean().
func (r *Rand) Bool() bool { return r.next(1) != 0 }

// Float64 matches nextDouble().
func (r *Rand) Float64() float64 {
	hi := int64(r.next(26)) << 27
	return float64(hi+int64(r.next(27))) / float64(int64(1)<<53)
}

// Float32 matches nextFloat().
func (r *Rand) Float32() float32 {
	return float32(r.next(24)) / float32(1<<24)
}

// PercentTrue matches Forge's MyRandom.percentTrue, which is
// `percent > getRandom().nextInt(100)`.
//
// Ported here rather than in the engine because its exact call into the
// generator is what has to match, not its meaning.
func (r *Rand) PercentTrue(percent int32) bool {
	return percent > r.Int32n(100)
}

// Shuffle reorders n elements by calling swap, matching
// java.util.Collections.shuffle(List, Random) for a random-access list.
//
// Java walks from the end down to index 1, swapping each element with one at a
// random index at or below it. The direction and the bound are both load-bearing:
// walking upward, or using nextInt(n), produces a different permutation from the
// same seed.
func (r *Rand) Shuffle(n int, swap func(i, j int)) {
	for i := n; i > 1; i-- {
		swap(i-1, int(r.Int32n(int32(i))))
	}
}
