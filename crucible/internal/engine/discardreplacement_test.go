package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// The discard replacement walk (ReplaceMoved's Discard$ True, Player.discard)
// refuses what it cannot order or check rather than guessing (GO-7).

// A discard paid as a cost carries no cause the walk knows: Loxodon Smiter's
// EffectOnly$/ValidCause$ line cannot be decided, and is refused.
func TestDiscardReplacementWithUnknownCauseIsAnError(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	voice := g.NewCard(corpusCard(t, "Tormenting Voice"), p, engine.Hand)
	smiter := g.NewCard(corpusCard(t, "Loxodon Smiter"), p, engine.Hand)
	g.NewCard(corpusCard(t, "Island"), p, engine.Library)
	g.NewCard(corpusCard(t, "Island"), p, engine.Library)
	g.Player(p).ManaPool.Add(mana.Red, 2)
	c := engine.NewScriptedController()
	c.QueuePayGeneric(mana.ShardR)
	c.QueueDiscardChoice([]engine.CardID{smiter})
	if !g.CastSpell(p, voice, c) {
		t.Fatal("could not cast Tormenting Voice")
	}
	err := g.ResolveStack(engine.NewRegistry(), c)
	if err == nil || !strings.Contains(err.Error(), "cause is not known") {
		t.Errorf("err = %v, want the Smiter's cause check refused", err)
	}
}

// Madness and Rest in Peace both apply to the discard: CR 616.1's order is
// the player's, which the walk does not ask, so it is refused.
func TestMadnessBesideAGraveyardReplacementIsAnError(t *testing.T) {
	t.Parallel()
	g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
	g.NewCard(corpusCard(t, "Rest in Peace"), p, engine.Battlefield)
	g.NewCard(corpusCard(t, "Fiery Temper"), p, engine.Hand)
	_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ Discard | Defined$ You | Mode$ Hand")
	if err == nil || !strings.Contains(err.Error(), "graveyard replacement both apply") {
		t.Errorf("err = %v, want the two replacements refused", err)
	}
}

// Two discard replacements on one card (two Madness keywords) are refused,
// and so is a ValidCause$ the walk does not read.
func TestDiscardReplacementShapesRefused(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name  string
		lines []string
		want  string
	}{
		{"two madness lines", []string{"K:Madness:R", "K:Madness:B"}, "two discard replacements"},
		{"unread ValidCause", []string{
			"R:Event$ Moved | ActiveZones$ Hand | ValidCard$ Card.Self | ValidCause$ SpellAbility.YouCtrl | Discard$ True | ReplaceWith$ DBExile",
			"SVar:DBExile:DB$ ChangeZone | Defined$ ReplacedCard | Origin$ Hand | Destination$ Exile",
		}, "ValidCause$"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, _ := newTwoPlayerGameOn(t, scenarioDB(t))
			g.NewCard(scriptDef(t, "Test Discardee", "Sorcery", tc.lines...), p, engine.Hand)
			_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ Discard | Defined$ You | Mode$ Hand")
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Errorf("err = %v, want %q", err, tc.want)
			}
		})
	}
}

// Play's PlayCost$ is plain mana; anything else is refused before a card is
// chosen.
func TestPlayCostPastManaIsRefused(t *testing.T) {
	t.Parallel()
	for _, raw := range []string{"Discard<1/Card>", "ManaCost"} {
		g, p, _ := newTwoPlayerGame(t)
		_, err := resolveNow(t, g, p, engine.NewScriptedController(), nil, "DB$ Play | Defined$ Self | PlayCost$ "+raw)
		if err == nil || !strings.Contains(err.Error(), "PlayCost$") {
			t.Errorf("PlayCost$ %s: err = %v, want refused", raw, err)
		}
	}
}
