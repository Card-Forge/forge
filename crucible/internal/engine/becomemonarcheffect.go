// BecomeMonarch: the monarch designation (CR 724), its "The Monarch"
// Command-zone effect card and the Mode$ BecomeMonarch trigger.

package engine

//enginelint:allow id zone card game player ability defined condition control parts valid trigger effecteffect effecthelpers earthbendeffect

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/carddb/vocab"
)

// becomeMonarchEffect is BecomeMonarchEffect.java: each targeted or
// Defined$ player (default the activator) still in the game, and allowed to
// by every Mode$ CantBecomeMonarch static, becomes the monarch
// (GameAction.becomeMonarch).
//
// ConditionDefined$ is rejected: subAbilityConditionMet reads a line naming
// it as unmet, which would silently skip the effect.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/BecomeMonarchEffect.java's
// resolve.
type becomeMonarchEffect struct{}

func (becomeMonarchEffect) Resolve(g *Game, a *Ability, c PlayerController) error {
	if err := rejectParams(a, "BecomeMonarch", "ConditionDefined"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: BecomeMonarch: %w", err)
	}
	for _, pid := range players {
		if g.Player(pid).Lost {
			continue
		}
		can, err := g.canBecomeMonarch(pid)
		if err != nil {
			return fmt.Errorf("engine: BecomeMonarch: %w", err)
		}
		if !can {
			continue
		}
		if err := g.becomeMonarch(c, pid); err != nil {
			return fmt.Errorf("engine: BecomeMonarch: %w", err)
		}
	}
	return nil
}

// Monarch is the player who is the monarch (CR 724), NoPlayer while nobody
// is.
func (g *Game) Monarch() PlayerID { return g.monarch }

// SetMonarch makes p the monarch as a restored game state would have it:
// the designation and p's "The Monarch" card in the Command zone, with no
// trigger run and no CantBecomeMonarch check -- the fixture loader's
// counterpart of SetTurnState. NoPlayer clears the designation.
func (g *Game) SetMonarch(p PlayerID) {
	if g.monarch != NoPlayer {
		g.removeDesignationCard(g.Player(g.monarch).monarchEffect)
	}
	g.monarch = p
	if p != NoPlayer {
		pl := g.Player(p)
		pl.monarchEffect = g.putDesignationCard(pl.monarchEffect, p, monarchEffectDef)
	}
}

// IsDesignationCard reports whether id is some player's designation effect
// card ("The Monarch", "The Initiative", "The Ring", "Planar Dice"): state the
// designation itself implies, which a fixture writes as monarch=/initiative=/
// numringtemptedyou= rather than as a Command-zone card no database holds.
// "Planar Dice" is written as nothing: Java makes one for every player of a
// Planechase game (Player.createPlanechaseEffects), so no fixture line carries it.
func (g *Game) IsDesignationCard(id CardID) bool {
	if id == NoCard {
		return false
	}
	if g.isRingCard(id) {
		return true
	}
	for _, pid := range g.Players() {
		if pl := g.Player(pid); pl.monarchEffect == id || pl.initiativeEffect == id || pl.planarDie == id {
			return true
		}
	}
	return false
}

// becomeMonarch is GameAction.becomeMonarch: p takes the monarchy from
// whoever held it. The previous monarch's effect card leaves the Command
// zone first; then, if a CantBecomeMonarch static stops p, nobody gets a
// new effect card and Game.monarch is left naming the previous monarch --
// Java's own order, reproduced as written. Otherwise p's "The Monarch" card
// enters p's Command zone, p is the monarch, and Mode$ BecomeMonarch
// triggers run.
func (g *Game) becomeMonarch(c PlayerController, p PlayerID) error {
	previous := g.monarch
	if p == NoPlayer || p == previous {
		return nil
	}
	if previous != NoPlayer {
		g.removeDesignationCard(g.Player(previous).monarchEffect)
	}
	can, err := g.canBecomeMonarch(p)
	if err != nil {
		return err
	}
	if !can {
		return nil
	}
	pl := g.Player(p)
	pl.monarchEffect = g.putDesignationCard(pl.monarchEffect, p, monarchEffectDef)
	g.monarch = p
	g.checkBecomeMonarchTriggers(c, p)
	return nil
}

// putDesignationCard is Player.createMonarchEffect's zone half: the
// player's one effect card, made on first use, goes (back) into the
// player's Command zone. It is permanent -- the designation, not a
// Duration$, decides when it leaves.
func (g *Game) putDesignationCard(id CardID, p PlayerID, def func() *compile.Card) CardID {
	if id == NoCard {
		id = g.NewCard(def(), p, Command)
		eff := g.Card(id)
		eff.IsEffect = true
		eff.effectLife = effectLifetime{duration: effectPermanent, player: p}
		return id
	}
	if c := g.Card(id); c.Zone != Command {
		g.Zone(c.Zone, c.ZoneOwner).cards.Remove(id)
		g.put(id, Command, p)
	}
	return id
}

// removeDesignationCard is Player.removeMonarchEffect: the effect card
// leaves the Command zone, parked in None (exileEffect) for the next time.
func (g *Game) removeDesignationCard(id CardID) {
	if id != NoCard && g.Card(id).Zone == Command {
		g.exileEffect(id)
	}
}

// monarchEffectDef is the card Player.createMonarchEffect builds: "The
// Monarch", with its two triggers. Java parses both from strings at
// creation; this port builds the same trees directly (the way
// earthbendReturnTrigger does), so nothing is parsed at runtime (PORT-2):
//
//	Mode$ Phase | Phase$ End of Turn | TriggerZones$ Command | ValidPlayer$ You
//	  -> DB$ Draw | Defined$ You
//	Mode$ DamageDone | ValidSource$ Creature | ValidTarget$ You | CombatDamage$ True | TriggerZones$ Command
//	  -> DB$ BecomeMonarch | Defined$ TriggeredSourceController
func monarchEffectDef() *compile.Card {
	draw := designationTrigger("Phase", []vocab.Param{
		{Key: "Phase", Value: "End of Turn"}, {Key: "TriggerZones", Value: "Command"}, {Key: "ValidPlayer", Value: "You"},
	}, "Draw", []vocab.Param{{Key: "Defined", Value: "You"}})
	steal := designationTrigger("DamageDone", []vocab.Param{
		{Key: "ValidSource", Value: "Creature"}, {Key: "ValidTarget", Value: "You"},
		{Key: "CombatDamage", Value: "True"}, {Key: "TriggerZones", Value: "Command"},
	}, "BecomeMonarch", []vocab.Param{{Key: "Defined", Value: "TriggeredSourceController"}})
	return designationDef("The Monarch", draw, steal)
}

// designationDef is an effect card definition named name carrying
// triggers, and nothing else: no type line, cost or power.
func designationDef(name string, triggers ...*compile.Ability) *compile.Card {
	def := &compile.Card{Name: name}
	def.Faces[0].Name = name
	def.Faces[0].Triggers = triggers
	return def
}

// designationTrigger builds a "Mode$ <mode> | <params>" trigger whose
// overriding ability is "DB$ <api> | <execParams>" -- Java's
// TriggerHandler.parseTrigger plus setOverridingAbility, as the Execute$ sub
// this port's trigger checks read (triggerEffectAPI).
func designationTrigger(mode string, params []vocab.Param, api string, execParams []vocab.Param) *compile.Ability {
	exec := &compile.Ability{Record: compile.SubAbility, Name: api,
		Params: append([]vocab.Param{{Key: "DB", Value: api}}, execParams...)}
	return &compile.Ability{Record: compile.Trigger, Name: mode,
		Params: append([]vocab.Param{{Key: "Mode", Value: mode}}, params...),
		Subs:   []compile.SubRef{{Key: "Execute", Ability: exec}}}
}

// canBecomeMonarch is Player.canBecomeMonarch: no Mode$ CantBecomeMonarch
// static in play names p (StaticAbilityCantBecomeMonarch). Its one corpus
// line, Jared Carthalion's, reaches play through an Effect card, which
// traitHosts covers. A static carrying anything past ValidPlayer$ (a
// checkConditions shape) or a ValidPlayer$ matchesPlayerSpec cannot read is
// an error, not a guess (GO-7).
func (g *Game) canBecomeMonarch(p PlayerID) (bool, error) {
	return noPlayerStatic(g, p, "CantBecomeMonarch")
}

// noPlayerStatic reports whether no Mode$ <mode> static in play applies to
// p through its ValidPlayer$ (absent: every player) -- the shape
// StaticAbilityCantBecomeMonarch and StaticAbilityCantVenture share.
func noPlayerStatic(g *Game, p PlayerID, mode string) (bool, error) {
	for _, pid := range g.Players() {
		for _, host := range g.traitHosts(pid) {
			h := g.Card(host)
			if h.Def == nil {
				continue
			}
			for _, face := range h.Def.Faces {
				for _, s := range face.Statics {
					if !strings.EqualFold(s.Name, mode) {
						continue
					}
					for _, prm := range s.Params {
						switch strings.ToLower(prm.Key) {
						case "mode", "validplayer", "description":
						default:
							return false, fmt.Errorf("%s static %s$ not resolvable yet", mode, prm.Key)
						}
					}
					spec, ok := s.Param("ValidPlayer")
					if !ok {
						return false, nil
					}
					matched, recognized := matchesPlayerSpec(g, p, h.Controller(), host, spec)
					if !recognized {
						return false, fmt.Errorf("%s static ValidPlayer$ %q not resolvable yet", mode, spec)
					}
					if matched {
						return false, nil
					}
				}
			}
		}
	}
	return true, nil
}

// checkBecomeMonarchTriggers runs Mode$ BecomeMonarch (TriggerBecomeMonarch)
// for p: ValidPlayer$ against p, BeginTurn$ against who was the monarch as
// the turn began (nobody matches nothing, as Java's matchesValid on null).
// Static$ True lines -- resolved at once, off the stack -- are skipped: no
// trigger mode here runs one that way.
func (g *Game) checkBecomeMonarchTriggers(c PlayerController, p PlayerID) {
	g.pushPlayerTriggers(c, p, g.playerActionTriggerMatches(p, func(h *Card, t *compile.Ability) bool {
		if hasAnyParam(t, "Static") {
			return false
		}
		spec, ok := t.Param("BeginTurn")
		if !ok {
			return true
		}
		if g.monarchBeginTurn == NoPlayer {
			return false
		}
		matched, recognized := matchesPlayerSpec(g, g.monarchBeginTurn, h.Controller(), h.ID, spec)
		return recognized && matched
	}, "BecomeMonarch"))
}

// pushPlayerTriggers pushes matches, each recording p as its
// AbilityKey.Player (Defined$ TriggeredPlayer) -- setTriggeringObjectsFrom(
// runParams, AbilityKey.Player) in TriggerBecomeMonarch,
// TriggerTakesInitiative and TriggerCompletedDungeon.
func (g *Game) pushPlayerTriggers(c PlayerController, p PlayerID, matches []Ability) {
	for i := range matches {
		matches[i].triggered.player = p
	}
	g.pushTriggeredAbilities(c, matches)
}

// onPlayersLost is the designation half of Game.onPlayerLost
// (Game.java:988-1006), run once per player as CheckStateBasedActions
// awards the loss (GameAction.checkGameOverCondition), in seat order.
// CR 724.4 and 725.4: a monarch, or the player with the initiative, who
// leaves the game passes it to the active player -- or, when the leaver is
// the active player, to the next player in the game.
//
// Java calls becomeMonarch here without the effect's CantBecomeMonarch
// pre-check; becomeMonarch's own check still applies, and a static it
// cannot read keeps the monarchy where it is rather than guessing (the SBA
// has no error channel). lossHandled is set once the player is processed:
// until then Java still counts them among ingamePlayers (designationSuccessor).
func (g *Game) onPlayersLost(c PlayerController) {
	for _, pid := range g.Players() {
		pl := g.Player(pid)
		if !pl.Lost || pl.lossHandled {
			continue
		}
		if g.monarch == pid {
			_ = g.becomeMonarch(c, g.designationSuccessor(pid))
		}
		if g.initiative == pid {
			g.takeInitiative(c, g.designationSuccessor(pid))
		}
		pl.lossHandled = true
	}
}

// designationSuccessor is who a leaving player's designation passes to:
// the active player, or, when the leaver is the active player, the next
// player in the game after them.
func (g *Game) designationSuccessor(leaver PlayerID) PlayerID {
	if leaver != g.activePlayer {
		return g.activePlayer
	}
	return g.nextInGameAfter(leaver)
}

// nextInGameAfter is Game.getNextPlayerAfter during onPlayerLost: the next
// seat, in turn order, whose player Java still holds in ingamePlayers --
// every player not yet processed as a loser, including losers of this same
// pass still waiting their turn (Player.lossHandled unset). Unlike
// nextPlayerAfter, which skips every player who has lost. NoPlayer when
// nobody is left.
func (g *Game) nextInGameAfter(p PlayerID) PlayerID {
	ids := g.Players()
	start := 0
	for i, id := range ids {
		if id == p {
			start = i
			break
		}
	}
	step := 1
	if g.turnOrderReversed {
		step = len(ids) - 1
	}
	for i := 1; i <= len(ids); i++ {
		next := ids[(start+i*step)%len(ids)]
		if pl := g.Player(next); !pl.Lost || !pl.lossHandled {
			return next
		}
	}
	return NoPlayer
}
