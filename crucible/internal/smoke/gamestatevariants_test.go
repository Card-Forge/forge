package smoke_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/smoke"
)

// TestProbePlaysTheGameStateATargetReads proves the staged variants: a card
// whose target reads what happened this turn is cast once the probe has played
// that state into being through the engine, and so is judged. Each case names
// the state it needs.
func TestProbePlaysTheGameStateATargetReads(t *testing.T) {
	t.Parallel()

	p := smoke.NewProber(corpusDB(t))
	for _, tc := range []struct {
		card string
		want smoke.Outcome
	}{
		// An Aura attached to a permanent (Pacifism cast and resolved).
		{"Cut the Earthly Bond", smoke.Plays},
		{"Venomous Vines", smoke.Plays},
		{"Enchantment Alteration", smoke.Plays},
		// A creature that entered this turn, one that died this turn, a
		// nonland card put into a graveyard this turn (Murder, then a bear).
		{"Cradle to Grave", smoke.Plays},
		{"Premature Burial", smoke.Plays},
		{"Grim Return", smoke.Plays},
		{"Reenact the Crime", smoke.Plays},
		// The second spell cast this turn; a spell cast from a graveyard.
		{"Second Guess", smoke.Plays},
		{"Laquatus's Disdain", smoke.Plays},
		// A creature dealt damage that lived (Electrickery), a creature with a
		// +1/+1 counter (Battlegrowth), an opponent's face-down creature (Soul
		// Summons on its own turn).
		{"Fatal Blow", smoke.Plays},
		{"You Are Already Dead", smoke.Plays},
		{"Mutant's Prey", smoke.Plays},
		{"Break Open", smoke.Plays},
		// Combat played through the turn: a creature that dealt damage, that
		// blocked, a blocking creature of the probe player's, an unblocked
		// attacker, an attacker at the probe player, a creature that dealt it
		// damage, a creature of its own that dealt damage and lived.
		{"Avenging Arrow", smoke.Plays},
		{"Sizzling Barrage", smoke.Plays},
		{"Aang's Defense", smoke.Plays},
		{"Mirror Strike", smoke.Plays},
		{"Confront the Assault", smoke.Plays},
		{"Reciprocate", smoke.Plays},
		{"Treacherous Greed", smoke.Plays},
		// The state-free part of the target placed first: a red or black
		// creature in combat, a creature that blocked or was blocked by a
		// Zombie, by a legendary creature.
		{"Surge of Righteousness", smoke.Plays},
		{"Time to Reflect", smoke.Plays},
		{"You Cannot Pass!", smoke.Plays},
	} {
		t.Run(tc.card, func(t *testing.T) {
			t.Parallel()

			if got := p.Probe(tc.card); got.Outcome != tc.want {
				t.Errorf("Probe(%q) = %v (%s: %s), want %v", tc.card, got.Outcome, got.Stage, got.Detail, tc.want)
			}
		})
	}
}

// TestProbeReportsWhatTheStagedBoardReveals: once the state is there and the
// card casts, an engine refusal behind it is the outcome, not hidden as
// inconclusive (GO-7). A card whose target the engine still cannot read stays
// inconclusive.
func TestProbeReportsWhatTheStagedBoardReveals(t *testing.T) {
	t.Parallel()

	p := smoke.NewProber(corpusDB(t))
	for _, tc := range []struct {
		card string
		want smoke.Outcome
	}{
		// Aura Graft's ReEnchant: Attach Move$ with Object$ ParentTarget.
		{"Aura Graft", smoke.Refused},
		// A Wall that blocks: Pump AtEOT$ and the Wall's own pump resolve.
		{"Glyph of Destruction", smoke.Plays},
		// PumpAll AtEOT$ resolves.
		{"Tears of Rage", smoke.Plays},
		// Its second target, "blockedByValidThisTurn ParentTarget", is a
		// Defined$ list the valid evaluator is not given.
		{"Glyph of Delusion", smoke.Inconclusive},
	} {
		t.Run(tc.card, func(t *testing.T) {
			t.Parallel()

			if got := p.Probe(tc.card); got.Outcome != tc.want {
				t.Errorf("Probe(%q) = %v (%s: %s), want %v", tc.card, got.Outcome, got.Stage, got.Detail, tc.want)
			}
		})
	}
}
