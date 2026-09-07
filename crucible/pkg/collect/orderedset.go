// Package collect holds the ordered collections the port needs and the standard
// library does not provide.
//
// It exists because iteration order is load-bearing in Forge. Trigger ordering,
// simultaneous-ability ordering and replacement selection all depend on it, and
// Go randomises map iteration deliberately. Substituting a plain map for one of
// Forge's ordered collections produces games that differ between runs from the
// same seed, which destroys replay parity (GO-12, ADR-0006).
package collect

// OrderedSet is a set that iterates in insertion order.
//
// The Java counterpart is Guava-backed FCollection. Not safe for concurrent
// use: each game owns its own state and nothing is shared (ADR-0005).
type OrderedSet[T comparable] struct {
	items []T
	index map[T]int
}

// NewOrderedSet returns an empty set, optionally sized.
func NewOrderedSet[T comparable](capacity int) *OrderedSet[T] {
	return &OrderedSet[T]{
		items: make([]T, 0, capacity),
		index: make(map[T]int, capacity),
	}
}

// Add appends v if absent and reports whether it was added. Re-adding an
// existing element does NOT move it: Forge's ordering is first-insertion, and
// a re-add that reordered would change trigger order.
func (s *OrderedSet[T]) Add(v T) bool {
	if s.index == nil {
		s.index = make(map[T]int)
	}
	if _, ok := s.index[v]; ok {
		return false
	}
	s.index[v] = len(s.items)
	s.items = append(s.items, v)
	return true
}

// Remove deletes v and reports whether it was present. Order of the remaining
// elements is preserved, so this is O(n) — the alternative, swapping the last
// element into the hole, is O(1) and would reorder.
func (s *OrderedSet[T]) Remove(v T) bool {
	i, ok := s.index[v]
	if !ok {
		return false
	}
	s.items = append(s.items[:i], s.items[i+1:]...)
	delete(s.index, v)
	for j := i; j < len(s.items); j++ {
		s.index[s.items[j]] = j
	}
	return true
}

// Contains reports membership.
func (s *OrderedSet[T]) Contains(v T) bool {
	_, ok := s.index[v]
	return ok
}

// Len returns the number of elements.
func (s *OrderedSet[T]) Len() int { return len(s.items) }

// All returns the elements in insertion order. The slice aliases the set's
// storage; callers must not mutate it.
func (s *OrderedSet[T]) All() []T { return s.items }

// Clone returns an independent copy. Used on the game-state clone path
// (ADR-0009), so it allocates exactly twice and copies nothing else.
func (s *OrderedSet[T]) Clone() *OrderedSet[T] {
	out := &OrderedSet[T]{
		items: make([]T, len(s.items)),
		index: make(map[T]int, len(s.index)),
	}
	copy(out.items, s.items)
	for k, v := range s.index {
		out.index[k] = v
	}
	return out
}
