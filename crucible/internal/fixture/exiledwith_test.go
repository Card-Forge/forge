package fixture_test

import (
	"strconv"
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/fixture"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// TestLoadAndDumpExiledWith proves GameState's ExiledWith:<id> annotation
// (GameState.java:407-410 dump, :771-781/1398 load): the exiled card is
// marked as exiled with the card that id names, and dumping writes the
// mark back out on the exiled card's own entry.
func TestLoadAndDumpExiledWith(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Grizzly Bears", "Llanowar Elves")
	l := load(t, db, "humanbattlefield=Grizzly Bears|Id:1\nhumanexile=Llanowar Elves|Id:2|ExiledWith:1\n")
	human := l.Game.Players()[0]
	bears := l.Game.Zone(engine.Battlefield, human).Cards()[0]
	elves := l.Game.Zone(engine.Exile, human).Cards()[0]
	if got := l.Game.Card(elves).ExiledWith(); got != bears {
		t.Fatalf("elves exiled with %v, want bears %v", got, bears)
	}
	st := fixture.Dump(l)
	if want := "|ExiledWith:" + strconv.FormatUint(uint64(bears), 10); !strings.Contains(st.Players[0].Exile, want) {
		t.Errorf("exile %q does not carry %q", st.Players[0].Exile, want)
	}
}

// TestLoadExiledWithRejectsABadHost pins the loader's error on an
// ExiledWith: naming no card, or no number at all.
func TestLoadExiledWithRejectsABadHost(t *testing.T) {
	t.Parallel()

	db := testDB(t, "Llanowar Elves")
	for _, text := range []string{
		"humanexile=Llanowar Elves|ExiledWith:9\n",
		"humanexile=Llanowar Elves|ExiledWith:x\n",
	} {
		st, err := fixture.Parse(strings.NewReader(text))
		if err != nil {
			t.Fatalf("Parse: %v", err)
		}
		if _, err := fixture.Load(st, db, javarand.New(1)); err == nil {
			t.Errorf("%q loaded, want an error", text)
		}
	}
}
