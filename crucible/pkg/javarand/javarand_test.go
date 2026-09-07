package javarand_test

import (
	"bufio"
	"fmt"
	"math"
	"os"
	"strconv"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// TestJavaParity is the M1 exit gate.
//
// testdata/java-random.golden is produced by crucible/oracle-java RandomDumper
// running on a real JVM. Every record here is a value Java actually produced;
// a mismatch means Go and Forge would shuffle differently, which would make
// scenario parity (ADR-0010, Layer 2) impossible.
//
// Regenerate with:
//
//	javac -d /tmp/jout crucible/oracle-java/src/main/java/crucible/oracle/RandomDumper.java
//	java -cp /tmp/jout crucible.oracle.RandomDumper > crucible/pkg/javarand/testdata/java-random.golden
func TestJavaParity(t *testing.T) {
	t.Parallel()

	f, err := os.Open("testdata/java-random.golden")
	if err != nil {
		t.Fatalf("open golden: %v", err)
	}
	defer f.Close()

	// Records for one (kind, seed) sequence arrive consecutively and share a
	// generator, because Java drew them from one. Rebuilding per record would
	// test the seeding and nothing else.
	var (
		cur     *javarand.Rand
		curKey  string
		records int
	)
	rng := func(key string, seed int64) *javarand.Rand {
		if key != curKey {
			cur, curKey = javarand.New(seed), key
		}
		return cur
	}

	sc := bufio.NewScanner(f)
	sc.Buffer(make([]byte, 0, 1<<20), 1<<20)
	for line := 1; sc.Scan(); line++ {
		fields := strings.Split(sc.Text(), "\t")
		if len(fields) < 4 {
			t.Fatalf("golden line %d: want >=4 fields, got %d", line, len(fields))
		}
		kind, seed := fields[0], mustI64(t, fields[1], line)
		records++

		switch kind {
		case "nextInt":
			r := rng(kind+fields[1], seed)
			if got, want := int64(r.Int32()), mustI64(t, fields[3], line); got != want {
				t.Fatalf("line %d: nextInt(seed=%d,i=%s) = %d, want %d", line, seed, fields[2], got, want)
			}
		case "nextLong":
			r := rng(kind+fields[1], seed)
			if got, want := r.Int64(), mustI64(t, fields[3], line); got != want {
				t.Fatalf("line %d: nextLong(seed=%d,i=%s) = %d, want %d", line, seed, fields[2], got, want)
			}
		case "nextBoolean":
			r := rng(kind+fields[1], seed)
			if got, want := r.Bool(), fields[3] == "true"; got != want {
				t.Fatalf("line %d: nextBoolean(seed=%d,i=%s) = %v, want %v", line, seed, fields[2], got, want)
			}
		case "nextDouble":
			r := rng(kind+fields[1], seed)
			// Compared as raw bits: Java emits the exact double, and a float
			// comparison would hide a one-ulp difference that changes a draw.
			if got, want := int64(math.Float64bits(r.Float64())), mustI64(t, fields[3], line); got != want {
				t.Fatalf("line %d: nextDouble(seed=%d,i=%s) bits = %d, want %d", line, seed, fields[2], got, want)
			}
		case "nextFloat":
			r := rng(kind+fields[1], seed)
			if got, want := int64(int32(math.Float32bits(r.Float32()))), mustI64(t, fields[3], line); got != want {
				t.Fatalf("line %d: nextFloat(seed=%d,i=%s) bits = %d, want %d", line, seed, fields[2], got, want)
			}
		case "nextIntBound":
			bound := int32(mustI64(t, fields[2], line))
			r := rng(fmt.Sprintf("%s%s/%d", kind, fields[1], bound), seed)
			if got, want := int64(r.Int32n(bound)), mustI64(t, fields[4], line); got != want {
				t.Fatalf("line %d: nextInt(seed=%d,bound=%d,i=%s) = %d, want %d", line, seed, bound, fields[3], got, want)
			}
		case "percentTrue":
			p := int32(mustI64(t, fields[2], line))
			r := rng(kind+fields[1], seed)
			if got, want := r.PercentTrue(p), fields[3] == "true"; got != want {
				t.Fatalf("line %d: percentTrue(seed=%d,p=%d) = %v, want %v", line, seed, p, got, want)
			}
		case "shuffle":
			size := int(mustI64(t, fields[2], line))
			list := make([]int, size)
			for i := range list {
				list[i] = i
			}
			javarand.New(seed).Shuffle(size, func(i, j int) { list[i], list[j] = list[j], list[i] })
			if got, want := joinInts(list), fields[3]; got != want {
				t.Fatalf("line %d: shuffle(seed=%d,size=%d)\n got %s\nwant %s", line, seed, size, got, want)
			}
		case "bulkInt":
			n, want := int(mustI64(t, fields[2], line)), mustI64(t, fields[3], line)
			r, sum := javarand.New(seed), int64(0)
			for i := 0; i < n; i++ {
				sum = sum*31 + int64(r.Int32())
			}
			if sum != want {
				t.Fatalf("line %d: %d-draw nextInt checksum = %d, want %d", line, n, sum, want)
			}
		case "bulkIntBound60":
			n, want := int(mustI64(t, fields[2], line)), mustI64(t, fields[3], line)
			r, sum := javarand.New(seed), int64(0)
			for i := 0; i < n; i++ {
				sum = sum*31 + int64(r.Int32n(60))
			}
			if sum != want {
				t.Fatalf("line %d: %d-draw nextInt(60) checksum = %d, want %d", line, n, sum, want)
			}
		default:
			t.Fatalf("golden line %d: unknown record kind %q", line, kind)
		}
	}
	if err := sc.Err(); err != nil {
		t.Fatalf("scan golden: %v", err)
	}
	if records < 1000 {
		t.Fatalf("golden held only %d records; it should cover every draw kind", records)
	}
	t.Logf("verified %d records against the Java golden", records)
}

func TestInt32nRejectsNonPositiveBound(t *testing.T) {
	t.Parallel()
	for _, bound := range []int32{0, -1, math.MinInt32} {
		func() {
			defer func() {
				if recover() == nil {
					t.Errorf("Int32n(%d) did not panic; Java throws IllegalArgumentException", bound)
				}
			}()
			javarand.New(1).Int32n(bound)
		}()
	}
}

func TestSetSeedMatchesFreshRand(t *testing.T) {
	t.Parallel()
	a := javarand.New(99)
	b := javarand.New(1)
	b.SetSeed(99)
	for i := range 8 {
		if got, want := b.Int32(), a.Int32(); got != want {
			t.Fatalf("draw %d after SetSeed = %d, want %d", i, got, want)
		}
	}
}

func mustI64(t *testing.T, s string, line int) int64 {
	t.Helper()
	v, err := strconv.ParseInt(strings.TrimSpace(s), 10, 64)
	if err != nil {
		t.Fatalf("golden line %d: parse %q: %v", line, s, err)
	}
	return v
}

func joinInts(xs []int) string {
	var b strings.Builder
	for i, x := range xs {
		if i > 0 {
			b.WriteByte(',')
		}
		b.WriteString(strconv.Itoa(x))
	}
	return b.String()
}
