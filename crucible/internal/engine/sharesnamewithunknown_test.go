package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// `sharesNameWith <restriction>` names a Defined$ list a valid string cannot
// answer without the ability that carries it (CardProperty.java:823-869). Such
// a filter matches nothing, and says so: a card whose filter was silently
// ignored would otherwise play as if the filter were satisfied by no card.
func TestSharesNameWithUnevaluableRestrictionLeavesAPendingError(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	bears := g.NewCard(corpusCard(t, "Grizzly Bears"), p, engine.Battlefield)
	source := g.NewCard(corpusCard(t, "Colossal Dreadmaw"), p, engine.Battlefield)

	if engine.Matches(g, g.Card(bears), valid.Parse("Card.sharesNameWith Targeted"), p, source) {
		t.Error("an unevaluable sharesNameWith restriction matched")
	}
	if err := g.TakePendingError(); err == nil || !strings.Contains(err.Error(), "sharesNameWith Targeted") {
		t.Errorf("pending error = %v, want one naming sharesNameWith Targeted", err)
	}
}
