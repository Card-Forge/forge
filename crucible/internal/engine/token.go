// Tokens: creating one from a token script and CR 704.5d's "a token
// anywhere but the battlefield ceases to exist."
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/
// TokenEffectBase.java's makeTokenTable and GameAction.java's
// stateBasedAction704_5d.

package engine

import (
	"fmt"
	"sort"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// tokenSpec is one token to create: the script, who creates (owns) it, and
// the per-token adjustments TokenEffectBase applies before it enters.
type tokenSpec struct {
	Def                    *compile.Card
	Owner                  PlayerID
	Tapped                 bool
	Power, Toughness       int
	HasPower, HasToughness bool
	// P1P1 is WithCountersType$ P1P1's amount (Incubate): counters the
	// token enters with.
	P1P1 int
}

// tokenScript looks up a TokenScript$ name. A script the DB does not hold
// is an error, Java's "don't find Token for TokenScript" (PORT-8).
func tokenScript(g *Game, script string) (*compile.Card, error) {
	def, ok := g.db.Token(script)
	if !ok {
		return nil, fmt.Errorf("engine: token script %q not found", script)
	}
	return def, nil
}

// createToken is one pass of makeTokenTable's inner loop: a new card from
// the script, owned by spec.Owner, tapped first when TokenTapped$ says so,
// carrying its enter-with counters, then moved onto the battlefield from
// ZoneType None through moveByEffect -- ETB replacement and triggers
// included, Origin$ None the way Java's triggerList records it. The
// caller fires ChangesZoneAll once for the batch.
func (g *Game) createToken(controller PlayerController, spec tokenSpec) CardID {
	id := g.NewCard(spec.Def, spec.Owner, None)
	c := g.Card(id)
	c.IsToken = true
	c.basePower, c.hasBasePower = spec.Power, spec.HasPower
	c.baseToughness, c.hasBaseToughness = spec.Toughness, spec.HasToughness
	// The counters enter with the token, through AddCounter replacements and a
	// Mode$ CantPutCounter static (TokenEffectBase.java:140-145, the counter
	// table's replaceCounterEffect).
	var enter []enterCounters
	if spec.P1P1 > 0 {
		enter = append(enter, enterCounters{kind: P1P1, n: spec.P1P1, placer: spec.Owner})
	}
	g.moveByEffect(controller, id, Battlefield, 0, NoPlayer, spec.Tapped, enter...)
	return id
}

// removeTokensOffBattlefield is CR 704.5d: a token in any zone but the
// battlefield leaves the game, silently (Zone.remove, no zone-change
// event). It is parked in its owner's None zone, since a CardID is never
// freed (ADR-0009). A prepared spell's copy waiting in exile stays
// (GameAction.java:1922-1924, prepare.go).
func removeTokensOffBattlefield(g *Game) bool {
	performed := false
	for _, pid := range g.Players() {
		for _, kind := range []ZoneType{Hand, Library, Graveyard, Exile, Command, Sideboard} {
			for _, id := range append([]CardID(nil), g.Zone(kind, pid).Cards()...) {
				if c := g.Card(id); c.IsToken && !hiddenPreparedSpell(c) {
					g.Zone(kind, pid).cards.Remove(id)
					g.put(id, None, g.Card(id).Owner)
					performed = true
				}
			}
		}
	}
	return performed
}

// inAPNAPOrder sorts players the way SpellAbilityEffect.getPlayers does
// before returning them: turn order starting with the active player.
func (g *Game) inAPNAPOrder(players []PlayerID) []PlayerID {
	order := g.playersInAPNAPOrder()
	rank := func(p PlayerID) int {
		for i, q := range order {
			if q == p {
				return i
			}
		}
		return len(order)
	}
	out := append([]PlayerID(nil), players...)
	sort.SliceStable(out, func(i, j int) bool { return rank(out[i]) < rank(out[j]) })
	return out
}
