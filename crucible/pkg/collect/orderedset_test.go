package collect_test

import (
	"slices"
	"testing"

	"github.com/jczastkiewicz/crucible/pkg/collect"
)

func TestOrderedSet(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name string
		ops  func(*collect.OrderedSet[int])
		want []int
	}{
		{"empty", func(*collect.OrderedSet[int]) {}, nil},
		{"insertion order preserved", func(s *collect.OrderedSet[int]) {
			s.Add(3)
			s.Add(1)
			s.Add(2)
		}, []int{3, 1, 2}},
		{"duplicate does not reorder", func(s *collect.OrderedSet[int]) {
			s.Add(1)
			s.Add(2)
			s.Add(1)
		}, []int{1, 2}},
		{"remove preserves order of the rest", func(s *collect.OrderedSet[int]) {
			for _, v := range []int{1, 2, 3, 4} {
				s.Add(v)
			}
			s.Remove(2)
		}, []int{1, 3, 4}},
		{"remove then re-add appends at the end", func(s *collect.OrderedSet[int]) {
			for _, v := range []int{1, 2, 3} {
				s.Add(v)
			}
			s.Remove(1)
			s.Add(1)
		}, []int{2, 3, 1}},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			s := collect.NewOrderedSet[int](0)
			tc.ops(s)
			if got := s.All(); !slices.Equal(got, tc.want) && (len(got) != 0 || len(tc.want) != 0) {
				t.Errorf("order = %v, want %v", got, tc.want)
			}
			if got, want := s.Len(), len(tc.want); got != want {
				t.Errorf("Len = %d, want %d", got, want)
			}
		})
	}
}

func TestOrderedSetAddRemoveReport(t *testing.T) {
	t.Parallel()
	s := collect.NewOrderedSet[string](0)
	if !s.Add("a") {
		t.Error(`Add("a") on empty set = false, want true`)
	}
	if s.Add("a") {
		t.Error(`Add("a") when present = true, want false`)
	}
	if !s.Contains("a") {
		t.Error(`Contains("a") = false, want true`)
	}
	if !s.Remove("a") {
		t.Error(`Remove("a") when present = false, want true`)
	}
	if s.Remove("a") {
		t.Error(`Remove("a") when absent = true, want false`)
	}
	if s.Contains("a") {
		t.Error(`Contains("a") after Remove = true, want false`)
	}
}

// Index integrity after a removal is the failure mode worth pinning: a stale
// index makes Contains and Remove disagree with All, silently.
func TestOrderedSetIndexSurvivesRemoval(t *testing.T) {
	t.Parallel()
	s := collect.NewOrderedSet[int](0)
	for i := range 10 {
		s.Add(i)
	}
	for _, v := range []int{0, 5, 9} {
		s.Remove(v)
	}
	for _, v := range s.All() {
		if !s.Contains(v) {
			t.Errorf("element %d present in All but Contains says no", v)
		}
		if !s.Remove(v) {
			t.Errorf("element %d present in All but Remove says absent", v)
		}
		s.Add(v)
	}
}

func TestOrderedSetCloneIsIndependent(t *testing.T) {
	t.Parallel()
	s := collect.NewOrderedSet[int](0)
	for _, v := range []int{1, 2, 3} {
		s.Add(v)
	}
	c := s.Clone()
	c.Add(4)
	c.Remove(1)
	if got := s.All(); !slices.Equal(got, []int{1, 2, 3}) {
		t.Errorf("original mutated by clone: %v", got)
	}
	if got := c.All(); !slices.Equal(got, []int{2, 3, 4}) {
		t.Errorf("clone = %v, want [2 3 4]", got)
	}
}

func BenchmarkOrderedSetClone(b *testing.B) {
	s := collect.NewOrderedSet[int](256)
	for i := range 256 {
		s.Add(i)
	}
	b.ReportAllocs()
	for b.Loop() {
		_ = s.Clone()
	}
}
