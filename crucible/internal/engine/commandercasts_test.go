package engine_test

import (
	"testing"

	"github.com/jczastkiewicz/crucible/internal/engine"
)

// Player.getTotalCommanderCast / getCommanderCast count the commanders a
// player cast from the Command zone (MagicStack.java:346-349). No card in a
// game this engine runs is a commander -- no Commander setup designates one
// -- so every count is 0, Java's own answer for a game without commanders,
// with the amount's operator still applied to it.
func TestCommanderCastCountsReadZeroWithoutCommanders(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		x    string
		want int
	}{
		// Echo Storm, Skull Storm, Jirina Kudro: the controller's total.
		{"Count$TotalCommanderCastFromCommandZone", 0},
		// The Swarmlord's /Twice and a Plus, applied to the 0.
		{"Count$TotalCommanderCastFromCommandZone/Plus.2", 2},
		// Opal Palace: the host's own count.
		{"Count$CommanderCastFromCommandZone/Plus.1", 1},
		// playerXCount sums every player's total, then operates the sum
		// (AbilityUtils.java:3366-3372).
		{"PlayerCountPlayers$TotalCommanderCastFromCommandZone/Plus.3", 3},
	} {
		t.Run(tc.x, func(t *testing.T) {
			t.Parallel()
			if got := gainedFrom(t, nil, tc.x); got != tc.want {
				t.Errorf("gained %d, want %d", got, tc.want)
			}
		})
	}
}

// TestCommandersInsightCountsTheTargetsCommanderCasts proves the
// TargetedPlayer$ spelling (Commander's Insight's Y): the targeted player's
// total, 0, under the SVar$Y/Plus.X sum the card draws by.
func TestCommandersInsightCountsTheTargetsCommanderCasts(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	addN(t, g, other, engine.Library, 3, "Artifact")
	c := engine.NewScriptedController()
	c.QueueTargets([]engine.EntityID{engine.PlayerEntity(other)})
	resolveLine(t, g, p, c, "DB$ Draw | ValidTgts$ Player | NumCards$ Z",
		"Y", "TargetedPlayer$TotalCommanderCastFromCommandZone", "Z", "SVar$Y/Plus.2")
	if got := len(g.Zone(engine.Hand, other).Cards()); got != 2 {
		t.Errorf("target drew %d, want 2 -- no commander casts plus 2", got)
	}
}
