package engine

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// TestWasAppliedDistinguishesSharedReplacementPointers is an internal test
// (TEST-2: an invariant not observable through the public API) for the gen
// field on copyReplacement: the DB shares one *compile.Card per name
// (compile/db.go's byName), so two cards with the same name -- an entering
// Body Double and a Body Double already in a graveyard, or an uncopied
// Vesuva copying another uncopied Vesuva -- carry the identical
// *compile.Ability pointer for their own ETBReplacement:Copy line. Without
// gen, wasApplied would treat a pointer it has already run for moved's own
// generation 0 as still "applied" once moved's Def is swapped by a copy and
// happens to carry that same shared pointer again, silently skipping a
// replacement Java would offer afresh (the copied state is a distinct
// ReplacementEffect with hasRun=false).
func TestWasAppliedDistinguishesSharedReplacementPointers(t *testing.T) {
	t.Parallel()

	shared := &compile.Ability{Name: "Continuous"}
	applied := []copyReplacement{{host: 1, r: shared, gen: 0}}

	if !wasApplied(applied, copyReplacement{host: 1, r: shared, gen: 0}) {
		t.Error("same host, pointer and generation must read as already applied")
	}
	if wasApplied(applied, copyReplacement{host: 1, r: shared, gen: 1}) {
		t.Error("a later generation carrying the identical shared pointer must not read as already applied")
	}
	if wasApplied(applied, copyReplacement{host: 2, r: shared, gen: 0}) {
		t.Error("a different host must never read as already applied off another host's entry")
	}
}
