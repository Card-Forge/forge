package engine_test

import (
	"strings"
	"testing"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
)

// chaosTrigger is the corpus's only chaos-ability shape (all 158 real lines).
const chaosTrigger = "Mode$ ChaosEnsues | TriggerZones$ Command | Execute$ Chaos"

// gainOnChaos is a Plane whose chaos ability has its controller gain n life.
func gainOnChaos(t *testing.T, name, n string) *compile.Card {
	t.Helper()
	return planarDef(t, name, "Plane", []string{chaosTrigger}, nil, "Chaos", "DB$ GainLife | Defined$ You | LifeAmount$ "+n)
}

// resolveChaos resolves line from a fresh host p controls, its Remembered
// list seeded with remembered first (Defined$ Remembered), and returns the
// stack's error.
func resolveChaos(t *testing.T, g *engine.Game, p engine.PlayerID, c *engine.ScriptedController, remembered []engine.CardID, line string) error {
	t.Helper()
	def := etbChainDef(t, "Chaos Source", line)
	host := g.NewCard(def, p, engine.Battlefield)
	for _, id := range remembered {
		g.Card(host).Memory.Remember(engine.CardEntity(id))
	}
	face := def.Faces[0]
	for _, sub := range face.Triggers[0].Subs {
		if strings.EqualFold(sub.Key, "Execute") {
			g.PushAbility(engine.Ability{API: engine.APIChaosEnsues, Source: host, Controller: p, Params: sub.Ability, Amounts: face.Amounts})
			return g.ResolveStack(engine.NewRegistry(), c)
		}
	}
	t.Fatal("no Execute$")
	return nil
}

// TestChaosEnsuesIsANoOpOutsidePlanechase proves ChaosEnsuesEffect.java:33:
// with no planar deck seated nothing triggers, even a chaos ability sitting
// face up in the Command zone -- Missy's "you draw a card and chaos ensues"
// in a normal game.
func TestChaosEnsuesIsANoOpOutsidePlanechase(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	g.NewCard(gainOnChaos(t, "Here", "5"), p, engine.Command)
	for _, line := range []string{"DB$ ChaosEnsues", "DB$ ChaosEnsues | Defined$ Bogus"} {
		if err := resolveChaos(t, g, p, engine.NewScriptedController(), nil, line); err != nil {
			t.Fatalf("%s: %v", line, err)
		}
	}
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("life = %d, want 20", got)
	}
}

// TestChaosEnsuesFiresTheActivePlanesChaosAbility proves the default path:
// the active plane's chaos ability fires under its planar controller -- the
// other player here, whose Command zone holds it -- and records the player
// chaos ensued for as TriggeredPlayer (TriggerChaosEnsues.setTriggeringObjects).
func TestChaosEnsuesFiresTheActivePlanesChaosAbility(t *testing.T) {
	t.Parallel()

	g, p, other := newTwoPlayerGame(t)
	faceUpPlane(g, other, planarDef(t, "Theirs", "Plane", []string{chaosTrigger}, nil,
		"Chaos", "DB$ GainLife | Defined$ You | LifeAmount$ 3 | SubAbility$ Hit",
		"Hit", "DB$ LoseLife | Defined$ TriggeredPlayer | LifeAmount$ 2"))
	planarDeck(g, other, plainPlane(t, "Their Next"))

	if err := resolveChaos(t, g, p, engine.NewScriptedController(), nil, "DB$ ChaosEnsues"); err != nil {
		t.Fatalf("ChaosEnsues: %v", err)
	}
	if got := g.Player(other).Life; got != 23 {
		t.Errorf("planar controller's life = %d, want 23", got)
	}
	if got := g.Player(p).Life; got != 18 {
		t.Errorf("activator's life = %d, want 18 (TriggeredPlayer loses 2)", got)
	}
}

// TestChaosAbilitiesInThePlanarDeckDoNotFire proves the default path keeps
// every chaos ability's TriggerZones$ Command: a plane still in the planar
// deck stays silent, and so does a chaos ability on a battlefield card that
// names Command.
func TestChaosAbilitiesInThePlanarDeckDoNotFire(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	faceUpPlane(g, p, gainOnChaos(t, "Here", "1"))
	planarDeck(g, p, gainOnChaos(t, "Buried", "100"))
	g.NewCard(planarDef(t, "Onlooker", "Creature Elf", []string{chaosTrigger}, nil,
		"Chaos", "DB$ GainLife | Defined$ You | LifeAmount$ 1000"), p, engine.Battlefield)

	if err := resolveChaos(t, g, p, engine.NewScriptedController(), nil, "DB$ ChaosEnsues"); err != nil {
		t.Fatalf("ChaosEnsues: %v", err)
	}
	if got := g.Player(p).Life; got != 21 {
		t.Errorf("life = %d, want 21 (active plane only)", got)
	}
}

// TestChaosEnsuesForARevealedPlane proves the Defined$ path, The Fertile
// Lands of Saulvinia's "chaos ensues on that plane" (CR 311.7's last case):
// the remembered plane's chaos ability fires from inside the planar deck,
// and the active plane's own does not (TriggerChaosEnsues.java:36-50's
// Affected check). A remembered card without a chaos ability is not
// affected, and naming the same plane twice is still one plane.
func TestChaosEnsuesForARevealedPlane(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	faceUpPlane(g, p, gainOnChaos(t, "Here", "1"))
	deck := planarDeck(g, p, plainPlane(t, "Blank"), gainOnChaos(t, "Revealed", "10"))

	remembered := []engine.CardID{deck[0], deck[1], deck[1]}
	if err := resolveChaos(t, g, p, engine.NewScriptedController(), remembered, "DB$ ChaosEnsues | Defined$ Remembered"); err != nil {
		t.Fatalf("ChaosEnsues: %v", err)
	}
	if got := g.Player(p).Life; got != 30 {
		t.Errorf("life = %d, want 30 (revealed plane only)", got)
	}
	if c := g.Card(deck[1]); c.Zone != engine.PlanarDeck {
		t.Errorf("revealed plane moved to %v", c.Zone)
	}
}

// TestChaosEnsuesForNoChaosAbilityFiresNothing proves
// ChaosEnsuesEffect.java:54-56: when no Defined$ card has a chaos ability,
// not even the active plane's fires.
func TestChaosEnsuesForNoChaosAbilityFiresNothing(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	faceUpPlane(g, p, gainOnChaos(t, "Here", "1"))
	deck := planarDeck(g, p, plainPlane(t, "Blank"))

	for _, remembered := range [][]engine.CardID{nil, deck} {
		if err := resolveChaos(t, g, p, engine.NewScriptedController(), remembered, "DB$ ChaosEnsues | Defined$ Remembered"); err != nil {
			t.Fatalf("ChaosEnsues: %v", err)
		}
	}
	if got := g.Player(p).Life; got != 20 {
		t.Errorf("life = %d, want 20", got)
	}
}

// TestChaosEnsuesRejectsUnportedDefinedShapes proves two Defined$ shapes fail
// closed before anything fires: two distinct planes (performTest's Iterable
// branch would fire neither, TriggerChaosEnsues.java:43-48; no real line
// reaches it) and a Defined$ spelling definedCards does not resolve.
func TestChaosEnsuesRejectsUnportedDefinedShapes(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct{ name, line, want string }{
		{"two planes", "DB$ ChaosEnsues | Defined$ Remembered", "naming more than one plane not resolvable yet"},
		{"unknown spelling", "DB$ ChaosEnsues | Defined$ Bogus", `Defined$ "Bogus" not resolvable yet`},
	} {
		g, p, _ := newTwoPlayerGame(t)
		faceUpPlane(g, p, gainOnChaos(t, "Here", "1"))
		deck := planarDeck(g, p, gainOnChaos(t, "One", "10"), gainOnChaos(t, "Two", "100"))
		err := resolveChaos(t, g, p, engine.NewScriptedController(), deck, tc.line)
		if err == nil || !strings.Contains(err.Error(), tc.want) {
			t.Errorf("%s: err = %v, want %q", tc.name, err, tc.want)
		}
		if got := g.Player(p).Life; got != 20 {
			t.Errorf("%s: life = %d, want 20", tc.name, got)
		}
	}
}

// TestChaosEnsuesValidPlayerMatchesWhomChaosEnsuedFor proves
// TriggerChaosEnsues.performTest's ValidPlayer$ check against
// AbilityKey.Player: "Opponent" of the plane's controller fires for the
// other player's chaos only, and a spelling matchesPlayerSpec does not know
// never fires.
func TestChaosEnsuesValidPlayerMatchesWhomChaosEnsuedFor(t *testing.T) {
	t.Parallel()

	for _, tc := range []struct {
		name, spec  string
		byOther     bool
		wantPlanars int
	}{
		{"own chaos", "Opponent", false, 20},
		{"opponent's chaos", "Opponent", true, 24},
		{"unknown spelling", "Bogus", true, 20},
	} {
		g, p, other := newTwoPlayerGame(t)
		faceUpPlane(g, p, planarDef(t, "Here", "Plane", []string{
			"Mode$ ChaosEnsues | ValidPlayer$ " + tc.spec + " | TriggerZones$ Command | Execute$ Chaos",
		}, nil, "Chaos", "DB$ GainLife | Defined$ You | LifeAmount$ 4"))
		planarDeck(g, p, plainPlane(t, "There"))
		activator := p
		if tc.byOther {
			activator = other
		}
		if err := resolveChaos(t, g, activator, engine.NewScriptedController(), nil, "DB$ ChaosEnsues"); err != nil {
			t.Fatalf("%s: %v", tc.name, err)
		}
		if got := g.Player(p).Life; got != tc.wantPlanars {
			t.Errorf("%s: life = %d, want %d", tc.name, got, tc.wantPlanars)
		}
	}
}

// TestPlaneswalkingToOteclanEnsuesChaos proves the corpus's dominant chain
// end to end (Oteclán, Norn's Seedcore): the plane's own "when you
// planeswalk to CARDNAME, chaos ensues" resolves DB$ ChaosEnsues, whose run
// finds that same plane's chaos ability now live in the Command zone.
func TestPlaneswalkingToOteclanEnsuesChaos(t *testing.T) {
	t.Parallel()

	g, p, _ := newTwoPlayerGame(t)
	planarDeck(g, p, planarDef(t, "Oteclan Test", "Plane", []string{
		"Mode$ PlaneswalkedTo | ValidCard$ Card.Self | Execute$ TrigChaos",
		chaosTrigger,
	}, nil,
		"TrigChaos", "DB$ ChaosEnsues",
		"Chaos", "DB$ GainLife | Defined$ You | LifeAmount$ 3"))

	resolveLine(t, g, p, engine.NewScriptedController(), "DB$ Planeswalk")
	if got := g.Player(p).Life; got != 23 {
		t.Errorf("life = %d, want 23", got)
	}
}

// TestChaosEnsuesStopsOnAStaticTriggerError proves a Static$ True chaos
// ability that cannot resolve fails the effect at its trigger site (GO-7),
// on both the default and the Defined$ path.
func TestChaosEnsuesStopsOnAStaticTriggerError(t *testing.T) {
	t.Parallel()

	for _, line := range []string{"DB$ ChaosEnsues", "DB$ ChaosEnsues | Defined$ Remembered"} {
		g, p, _ := newTwoPlayerGame(t)
		bad := faceUpPlane(g, p, planarDef(t, "Bad Plane", "Plane", []string{
			"Mode$ ChaosEnsues | TriggerZones$ Command | Execute$ Hit | Static$ True",
		}, nil, "Hit", "DB$ DealDamage | ValidTgts$ Player | NumDmg$ 1"))
		planarDeck(g, p, plainPlane(t, "There"))
		err := resolveChaos(t, g, p, engine.NewScriptedController(), []engine.CardID{bad}, line)
		if err == nil || !strings.Contains(err.Error(), "ValidTgts$ not resolvable yet") {
			t.Errorf("%s: err = %v, want the static trigger's refusal", line, err)
		}
	}
}
