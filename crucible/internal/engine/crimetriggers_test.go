package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// Mode$ CommitCrime (TriggerCommitCrime, Player.commitCrime): a player who
// puts a spell or ability on the stack that targets an opponent, something an
// opponent controls or a card in an opponent's graveyard commits a crime, and
// the triggers whose ValidPlayer$ admits them fire.

// Deepmuck Desperado mills each opponent three cards the first time its
// controller commits a crime each turn (ActivationLimit$ 1); a spell aimed at
// its controller's own creature is no crime.
func TestCommitCrimeTriggersOncePerTurn(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGameOn(t, scenarioDB(t))
	g.SetTurnState(1, p, engine.Main1)
	g.NewCard(corpusCard(t, "Deepmuck Desperado"), p, engine.Battlefield)
	addN(t, g, other, engine.Library, 10, "Artifact")
	mine := g.NewCard(creatureDefPT(t, "4", "4"), p, engine.Battlefield)

	cast := func(target engine.EntityID) {
		t.Helper()
		spell := crimeSpell(t, g, p)
		c := engine.NewScriptedController()
		c.QueueTargets([]engine.EntityID{target})
		castThenResolve(t, g, p, spell, c)
	}
	cast(engine.CardEntity(mine))
	if got := len(g.Zone(engine.Graveyard, other).Cards()); got != 0 {
		t.Fatalf("after targeting your own creature the opponent milled %d, want 0", got)
	}
	cast(engine.PlayerEntity(other))
	if got := len(g.Zone(engine.Graveyard, other).Cards()); got != 3 {
		t.Fatalf("after the first crime the opponent milled %d, want 3", got)
	}
	cast(engine.PlayerEntity(other))
	if got := len(g.Zone(engine.Graveyard, other).Cards()); got != 3 {
		t.Errorf("after a second crime the opponent milled %d, want still 3: once each turn", got)
	}
}

// ValidPlayer$ names whose crime counts, from the trigger host's controller's
// side; the trigger may wait in a graveyard (TriggerZones$, Forsaken Miner's
// shape). The criminal is TriggeredPlayer.
func TestCommitCrimeMatchesTheCriminal(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, valid string
		zone        engine.ZoneType
		zones       string
		criminal    bool // the trigger host's controller commits the crime
		fires       bool
	}{
		{"your crime, you", "You", engine.Battlefield, "Battlefield", true, true},
		{"the opponent's crime, you", "You", engine.Battlefield, "Battlefield", false, false},
		{"the opponent's crime, opponent", "Opponent", engine.Battlefield, "Battlefield", false, true},
		{"from the graveyard", "You", engine.Graveyard, "Graveyard", true, true},
		{"graveyard host, battlefield trigger", "You", engine.Graveyard, "Battlefield", true, false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			g, p, other := newTwoPlayerGame(t)
			g.SetTurnState(1, p, engine.Main1)
			watcher := g.NewCard(triggerWatcherDef(t, "Test Crime Watcher",
				"Mode$ CommitCrime | ValidPlayer$ "+tc.valid+" | TriggerZones$ "+tc.zones+" | Execute$ TrigGain",
				"TrigGain", "DB$ GainLife | Defined$ TriggeredPlayer | LifeAmount$ 7"), p, tc.zone)
			criminal, victim := p, other
			if !tc.criminal {
				criminal, victim = other, p
			}
			spell := crimeSpell(t, g, criminal)
			c := engine.NewScriptedController()
			c.QueueTargets([]engine.EntityID{engine.PlayerEntity(victim)})
			before := g.Player(criminal).Life
			castThenResolve(t, g, criminal, spell, c)
			// crimeSpell's own chain gains its caster 5 for the crime and 100
			// for committedCrimeThisTurn; the watcher adds 7 when it fires.
			want := 105
			if tc.fires {
				want += 7
			}
			if got := g.Player(criminal).Life - before; got != want {
				t.Errorf("criminal gained %d, want %d (watcher in %v)", got, want, g.Card(watcher).Zone)
			}
		})
	}
}

// A triggered ability whose target is an opponent is a crime too: its
// controller's CommitCrime triggers fire once it is on the stack.
func TestTriggeredAbilityTargetingAnOpponentIsACrime(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	g.SetTurnState(1, p, engine.Main1)
	g.NewCard(triggerWatcherDef(t, "Test Crime Watcher",
		"Mode$ CommitCrime | ValidPlayer$ You | TriggerZones$ Battlefield | Execute$ TrigGain",
		"TrigGain", "DB$ GainLife | Defined$ You | LifeAmount$ 7"), p, engine.Battlefield)
	def := etbChainDef(t, "Test Pinger", "DB$ DealDamage | ValidTgts$ Player | NumDmg$ 1")
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	before := g.Player(p).Life
	if _, err := castETBChain(t, g, p, def, c); err != nil {
		t.Fatalf("castETBChain: %v", err)
	}
	if got := g.Player(p).Life - before; got != 7 {
		t.Errorf("gained %d, want 7: the ETB trigger aimed at the opponent is a crime", got)
	}
	if got := g.Player(other).Life; got != 19 {
		t.Errorf("opponent at %d, want 19", got)
	}
}
