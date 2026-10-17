package smoke

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
)

// staging is game state a staged variant plays into being, through the
// engine, before the card is cast: a target spec that reads what happened
// this turn (an Aura attached, a creature that entered or died, a spell cast
// from a graveyard, a second spell) finds nothing on a board that was only
// placed.
type staging uint8

const (
	stageNone staging = iota
	// stageAura casts and resolves an Aura (wornAura) on the first creature
	// the controller picks: "Aura attached to a permanent", "enchanted
	// permanent".
	stageAura
	// stageEntered casts and resolves a removal spell (stageKill) on the first
	// creature offered, which dies, then a creature (bears): a creature that
	// entered this turn, a creature card put into a graveyard from the
	// battlefield this turn, a nonland card that entered a graveyard this turn.
	stageEntered
	// stageSpells casts two instants (bolt) and leaves both on the stack: the
	// second spell cast this turn.
	stageSpells
	// stageFlashback casts an instant (stageFlashbackSpell) from the graveyard
	// for its Flashback cost and leaves it on the stack: a spell cast from a
	// graveyard.
	stageFlashback
	// stageDamaged casts and resolves a spell dealing 1 damage to a creature
	// the probe player does not control (stagePing): a creature that was dealt
	// damage this turn and survived it.
	stageDamaged
	// stageToken casts and resolves a spell creating creature tokens
	// (stageTokens): a token the probe player controls.
	stageToken
	// stageCounter casts and resolves a spell putting a +1/+1 counter on the
	// first creature offered (stageGrow), the probe player's own bear.
	stageCounter
	// stageFaceDown casts and resolves a spell manifesting the top card of
	// the caster's library (stageManifest): a face-down creature. Played on
	// the opponent's turn, it is a face-down creature an opponent controls.
	stageFaceDown
)

// The corpus cards the stages are played with.
const (
	stageKill           = "Murder"
	stageFlashbackSpell = "Think Twice"
	stagePing           = "Electrickery"
	stageTokens         = "Raise the Alarm"
	stageGrow           = "Battlegrowth"
	stageManifest       = "Soul Summons"
)

// stagedVariants is tried, in order, for a card no earlier variant could cast.
// The non-combat stages are played in the probe player's first main phase,
// the face-down one in the opponent's. The combat ones walk a turn
// (timing.go's walk) with the defender blocking every attacker it can
// (Controller.blockEvery) or not at all, after the state-free part of the
// card's target specs has been placed on both sides (stateTailored): own turn
// in the declare blockers step and the second main phase after combat damage
// (an attacker that was blocked, a creature that dealt or was dealt damage, a
// creature that blocked; unblocked, a creature of the probe player's that
// dealt damage and lived), the opponent's turn in the declare blockers step (a
// creature of the probe player's blocking, an unblocked attacker, a creature
// attacking the probe player) and its second main phase (a creature that
// dealt damage to the probe player).
var stagedVariants = [...]variant{
	{stage: stageAura},
	{stage: stageEntered},
	{stage: stageSpells},
	{stage: stageFlashback},
	{stage: stageDamaged},
	{stage: stageToken},
	{stage: stageCounter},
	{timed: true, at: moment{opponents: true, step: engine.Main1}, stage: stageFaceDown},
	{timed: true, at: moment{step: engine.DeclareBlockers}, blocks: true, stateTailor: true},
	{timed: true, at: moment{step: engine.Main2}, blocks: true, stateTailor: true},
	{timed: true, at: moment{step: engine.Main2}, stateTailor: true},
	{timed: true, at: moment{opponents: true, step: engine.DeclareBlockers}, blocks: true, stateTailor: true},
	{timed: true, at: moment{opponents: true, step: engine.DeclareBlockers}, stateTailor: true},
	{timed: true, at: moment{opponents: true, step: engine.Main2}, blocks: true, stateTailor: true},
	{timed: true, at: moment{opponents: true, step: engine.Main2}, stateTailor: true},
}

// playStage plays s for the active player, in its main phase, leaving the
// game there with priority. An error means the staged spells could not be
// cast or resolved, which says nothing about the card: the caller reports the
// variant inconclusive with it.
func (p *Prober) playStage(g *engine.Game, ctl *Controller, s staging) error {
	pid := g.ActivePlayer()
	switch s {
	case stageAura:
		return p.castStaged(g, ctl, pid, wornAura, engine.Hand, true)
	case stageEntered:
		if err := p.castStaged(g, ctl, pid, stageKill, engine.Hand, true); err != nil {
			return err
		}
		return p.castStaged(g, ctl, pid, bears, engine.Hand, true)
	case stageSpells:
		if err := p.castStaged(g, ctl, pid, bolt, engine.Hand, false); err != nil {
			return err
		}
		return p.castStaged(g, ctl, pid, bolt, engine.Hand, false)
	case stageFlashback:
		return p.castStaged(g, ctl, pid, stageFlashbackSpell, engine.Graveyard, false)
	case stageDamaged:
		return p.castStaged(g, ctl, pid, stagePing, engine.Hand, true)
	case stageToken:
		return p.castStaged(g, ctl, pid, stageTokens, engine.Hand, true)
	case stageCounter:
		return p.castStaged(g, ctl, pid, stageGrow, engine.Hand, true)
	case stageFaceDown:
		return p.castStaged(g, ctl, pid, stageManifest, engine.Hand, true)
	}
	return nil
}

// castStaged puts name in pid's zone, gives pid the mana for it, casts it
// from there and, when resolve holds, resolves the stack.
func (p *Prober) castStaged(g *engine.Game, ctl *Controller, pid engine.PlayerID, name string, zone engine.ZoneType, resolve bool) error {
	def, ok := p.db.Card(name)
	if !ok {
		return fmt.Errorf("the staged %s is missing from the database", name)
	}
	pool := &g.Player(pid).ManaPool
	for _, c := range [...]mana.Colors{mana.White, mana.Blue, mana.Black, mana.Red, mana.Green} {
		pool.Add(c, 3)
	}
	if !g.CastSpell(pid, g.NewCard(def, pid, zone), ctl) {
		if err := g.TakePendingError(); err != nil {
			return fmt.Errorf("the staged %s could not be cast: %w", name, err)
		}
		return fmt.Errorf("the staged %s could not be cast", name)
	}
	if !resolve {
		return nil
	}
	if err := g.ResolveStack(p.registry, ctl); err != nil {
		return fmt.Errorf("the staged %s did not resolve: %w", name, err)
	}
	if err := g.TakePendingError(); err != nil {
		return fmt.Errorf("the staged %s did not resolve: %w", name, err)
	}
	return nil
}

// stateProps are the valid properties that read game state a placed card
// does not have: combat, damage, zone entry and block history
// (CardProperty.java). A target spec is searched for (stateSpecs) with them
// stripped, so that the creature the state is then played onto has the rest.
var stateProps = [...]string{
	"attacking", "blocking", "blocked", "unblocked", "dealtDamage", "wasDealtDamage", "gotBlocked",
	"ThisTurnEntered", "EnteredSince", "attackedThisTurn", "attackedYouThisTurn",
}

// definedNames are the Defined$ forms a blockedValidThisTurn names in the
// corpus instead of a valid spec: no card is searched for them.
var definedNames = map[string]bool{"Targeted": true, "ParentTarget": true}

// controlProps are the properties a placed bear already satisfies for one
// side or the other: a stripped spec left with only these needs no search.
var controlProps = map[string]bool{"YouCtrl": true, "OppCtrl": true, "YouDontCtrl": true, "YouOwn": true, "OppOwn": true}

// stateSpecs lists, for each target spec of def that names a state property,
// the spec with the state properties stripped, and the nested spec of a
// blockedValidThisTurn or blockedByValidThisTurn ("Zombie", "Creature.Legendary":
// what the creature blocked or was blocked by). A spec left with nothing a
// bear lacks is not listed.
func stateSpecs(def *compile.Card) []targetSpec {
	var out []targetSpec
	seen := map[string]bool{}
	add := func(spec string, zone engine.ZoneType) {
		if spec == "" || seen[spec] {
			return
		}
		seen[spec] = true
		out = append(out, targetSpec{alt: spec, zone: zone})
	}
	for _, ts := range targetSpecs(def) {
		if ts.spell || ts.ability || ts.zone != engine.Battlefield {
			continue
		}
		base, props, _ := strings.Cut(ts.alt, ".")
		var kept []string
		stripped, specific := false, false
		for _, prop := range strings.Split(props, "+") {
			if prop == "" {
				continue
			}
			for _, key := range [...]string{"blockedValidThisTurn ", "blockedByValidThisTurn "} {
				if nested, ok := strings.CutPrefix(prop, key); ok && !definedNames[nested] {
					add(nested, engine.Battlefield)
				}
			}
			if isStateProp(prop) {
				stripped = true
				continue
			}
			kept = append(kept, prop)
			specific = specific || !controlProps[prop]
		}
		if !stripped {
			continue
		}
		switch base {
		case "Creature", "Card", "Permanent":
		default:
			specific = true
		}
		if specific {
			add(strings.Join(append([]string{base}, kept...), "."), engine.Battlefield)
		}
	}
	return out
}

func isStateProp(prop string) bool {
	prop = strings.TrimPrefix(prop, "!")
	for _, s := range stateProps {
		if strings.HasPrefix(prop, s) {
			return true
		}
	}
	return false
}

// addStateTailored puts, for each spec stateSpecs lists for def, a card the
// spec accepts on each side that has one: the combat a staged variant then
// plays gives it the state the stripped properties asked for.
func (p *Prober) addStateTailored(g *engine.Game, pid, opp engine.PlayerID, def *compile.Card) {
	for _, ts := range stateSpecs(def) {
		for _, mine := range [...]bool{true, false} {
			name := p.candidate(ts, mine)
			if name == "" {
				continue
			}
			owner := opp
			if mine {
				owner = pid
			}
			if d, ok := p.db.Card(name); ok {
				g.NewCard(d, owner, ts.zone)
			}
		}
	}
}
