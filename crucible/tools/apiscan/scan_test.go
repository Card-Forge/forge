package main

import (
	"flag"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

var updateKinds = flag.Bool("update", false, "rewrite the golden files")

// Internal test, deliberately. apiscan is a command, so readParams and
// readExclusions are unexported and there is no public API to test through
// (TEST-2). Both are worth pinning: readParams is the whole premise -- a shape
// it misses becomes a card falsely reported as writing a dead param -- and
// readExclusions decides what the gate is allowed to stay quiet about.

func TestReadParams(t *testing.T) {
	t.Parallel()

	got, err := readParams(filepath.Join("testdata", "Reads.java"))
	if err != nil {
		t.Fatalf("readParams: %v", err)
	}

	want := []string{
		"Accessor", "Presence", "Defaulted", // accessors
		"RawContains", "RawGet", // raw map
		"OneHelperKey", "FirstOfTwo", "SecondOfTwo", // helpers
		"BoundToVariable", // key in a variable
		"ValidMatched",    // trigger and replacement matching
	}
	for _, key := range want {
		if !got[key] {
			t.Errorf("readParams missed %q", key)
		}
	}

	// A string literal that is not a param must not be collected, or the known
	// set grows and the gate stops finding anything.
	for _, key := range []string{"NotAParam", "AlsoNotAParam"} {
		if got[key] {
			t.Errorf("readParams collected %q, which no accessor reads", key)
		}
	}

	if len(got) != len(want) {
		t.Errorf("readParams found %d keys, want %d: %v", len(got), len(want), got)
	}
}

func TestReadExclusions(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name  string
		file  string
		want  []string
		avoid []string
	}{
		{
			name:  "param key rows only",
			file:  "exclusions.md",
			want:  []string{"excludedkey", "plainexcludedkey"},
			avoid: []string{"wrongkind", "—"},
		},
		{
			name: "no table",
			file: "empty.md",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			got, err := readExclusions(filepath.Join("testdata", tt.file))
			if err != nil {
				t.Fatalf("readExclusions: %v", err)
			}
			for _, key := range tt.want {
				if !got[key] {
					t.Errorf("readExclusions missed %q", key)
				}
			}
			// A row of another kind must not silence a param key of the same
			// name, which is why the gate matches on both columns.
			for _, key := range tt.avoid {
				if got[key] {
					t.Errorf("readExclusions took %q, which is not a param-key row", key)
				}
			}
			if len(got) != len(tt.want) {
				t.Errorf("readExclusions found %d rows, want %d: %v", len(got), len(tt.want), got)
			}
		})
	}
}

// TestParamKinds pins the type evidence for every param key the corpus writes.
//
// The classification is the input to M3's generated param structs (ADR-0007),
// and it is derived from two weak signals rather than a declaration, so a
// change in either -- an upstream refactor moving a getParam call, a new card
// writing a key in a new shape -- has to be a diff someone reads rather than a
// silent shift in what a generated field's type would be.
//
//	go test ./tools/apiscan -run TestParamKinds -update
func TestParamKinds(t *testing.T) {
	t.Parallel()

	f, err := os.CreateTemp(t.TempDir(), "kinds")
	if err != nil {
		t.Fatalf("temp: %v", err)
	}
	defer func() { _ = f.Close() }()

	if err := writeTypes("../../..", "../../../forge-gui/res/cardsfolder",
		"../../../forge-gui/res/lists/TypeLists.txt", f); err != nil {
		t.Fatalf("writeTypes: %v", err)
	}
	got, err := os.ReadFile(f.Name())
	if err != nil {
		t.Fatalf("read back: %v", err)
	}

	path := filepath.Join("testdata", "param-kinds.golden")
	if *updateKinds {
		if err := os.WriteFile(path, got, 0o644); err != nil {
			t.Fatalf("write %s: %v", path, err)
		}
		return
	}
	want, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("read %s: %v (regenerate with -update)", path, err)
	}
	if string(got) != string(want) {
		gl, wl := strings.Split(string(got), "\n"), strings.Split(string(want), "\n")
		for i := 0; i < len(gl) && i < len(wl); i++ {
			if gl[i] != wl[i] {
				t.Fatalf("param-kinds.golden line %d:\n got %s\nwant %s\n(regenerate with -update and read the diff)",
					i+1, gl[i], wl[i])
			}
		}
		t.Fatalf("param-kinds.golden has %d lines, generated %d", len(wl), len(gl))
	}
}

// An effect that reads a param through another effect class's static helper
// reads that param: CloneEffect reads PumpDuration$ only through
// TokenEffectBase.addPumpUntil. Only the called method's body counts, not the
// rest of the helper's class.
func TestReadEffectParamsFollowsStaticHelpers(t *testing.T) {
	t.Parallel()

	dir := t.TempDir()
	files := map[string]string{
		"CallerEffect.java": `public class CallerEffect extends SpellAbilityEffect {
    public void resolve(SpellAbility sa) {
        sa.getParam("Own");
        HelperEffectBase.addThing(sa, c);
    }
}`,
		"HelperEffectBase.java": `public abstract class HelperEffectBase extends SpellAbilityEffect {
    public static void addThing(SpellAbility sa, final Card c) {
        if (!sa.hasParam("Helped")) {
            return;
        }
    }
    public static void other(SpellAbility sa) {
        sa.getParam("NotCalled");
    }
}`,
	}
	for name, body := range files {
		if err := os.WriteFile(filepath.Join(dir, name), []byte(body), 0o600); err != nil {
			t.Fatal(err)
		}
	}
	got, err := readEffectParams(dir, "CallerEffect")
	if err != nil {
		t.Fatalf("readEffectParams: %v", err)
	}
	for _, key := range []string{"Own", "Helped"} {
		if !got[key] {
			t.Errorf("readEffectParams missed %q", key)
		}
	}
	if got["NotCalled"] {
		t.Error("readEffectParams collected a param from a helper method the effect never calls")
	}
}
