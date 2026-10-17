// The replacement Event$ values past Moved, Untap, DamageDone, Draw, GainLife,
// AddCounter, CreateToken, ProduceMana, DeclareBlocker, GameLoss and GameWin
// (replacement.go, gameloss.go), by the corpus's real R: line count:
//
//   - Counter (118): "can't be countered", Layer$ CantHappen. counterCantHappen.
//   - BeginPhase (21): "skip your draw step", Skip$ True. beginPhaseSkipped.
//   - LifeReduced (8): "reduces it to 1 instead", Worship's family, and
//     Bloodletter of Aclazotz's doubling. lifeReduced.
//   - LoseMana (5): Kruphix and Horizon Stone's "that mana becomes colorless
//     instead". loseManaConversion.
//   - BeginTurn (5): "that player skips that extra turn instead", Skip$ True
//     with ExtraTurn$ True. beginTurnSkipped.
//
// Ported from forge-game/src/main/java/forge/game/replacement/{ReplaceCounter,
// ReplaceBeginPhase,ReplaceLifeReduced,ReplaceLoseMana,ReplaceBeginTurn}.java
// and the call sites in CounterEffect.removeFromStack, PhaseHandler
// (advanceToNextPhase, getNextActivePlayer), Player.loseLife and
// ManaPool.clearPool. A line whose shape is not resolved (a Transform
// substitution) records a pending error when it applies rather
// than being ignored (GO-7).

package engine

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// replacementIsCantHappen is "Layer$ CantHappen" with no ReplaceWith$: the
// line says the event does not happen, with nothing to run.
func replacementIsCantHappen(r *compile.Ability) bool {
	layer, ok := r.Param("Layer")
	return ok && strings.EqualFold(layer, "CantHappen") && replaceWithSub(r) == nil
}

// counterCantHappen is ReplacementHandler.cantHappenCheck over Event$ Counter
// for the stack item about to be countered, called by
// CounterEffect.removeFromStack before it leaves the stack: a Layer$
// CantHappen line whose ValidCard$ matches spell -- the spell's card, or an
// ability's host (Affected is tgtSA.getHostCard(), CounterEffect.java:250-252)
// -- and whose ValidSA$ matches the item as kind (causeSpell, causeActivated
// or causeTriggered; stackValidSA) stops the counter: Spider-Punk's "spells
// and abilities can't be countered".
//
// ActiveZones$ names the zones the host must be in; with none, the line
// applies from anywhere (TriggerReplacementBase.zonesCheck), which is how a
// spell's own "this spell can't be countered" works from the stack. Hosts are
// the spell itself and everything on the battlefield or in the command zone.
// A line this cannot resolve (Guile's ReplaceWith$, a ValidCause$) is an
// error when no resolvable line stopped the counter: the spell could be
// countered wrongly otherwise.
func (g *Game) counterCantHappen(spell CardID, kind string) (bool, error) {
	spellCard := g.Card(spell)
	hosts := []CardID{spell}
	for _, pid := range g.Players() {
		for _, z := range replacementZones {
			for _, id := range g.Zone(z, pid).Cards() {
				if id != spell {
					hosts = append(hosts, id)
				}
			}
		}
	}
	unresolved := ""
	for _, id := range hosts {
		h := g.Card(id)
		if h.Def == nil {
			continue
		}
		for _, face := range h.liveTraitFaces() {
			for _, r := range face.Replacements {
				if !strings.EqualFold(r.Name, "Counter") {
					continue
				}
				if _, ok := r.Param("ActiveZones"); ok && !hostInActiveZones(h, r, h.Zone) {
					continue
				}
				if replaceWithSub(r) != nil && counterReplaceResolvable(r) {
					continue // counterReplaced's
				}
				if !replacementIsCantHappen(r) || !onlyParams(r, "layer", "validcard", "validsa") {
					unresolved = h.Def.Name
					continue
				}
				if v, ok := r.Param("ValidCard"); ok && !Matches(g, spellCard, valid.Parse(v), h.Controller(), h.ID) {
					continue
				}
				if v, ok := r.Param("ValidSA"); ok {
					matched, recognized := stackValidSA(g, v, spellCard, h, kind)
					if !recognized {
						unresolved = h.Def.Name
						continue
					}
					if !matched {
						continue
					}
				}
				if !replacementRequirementsCheck(g, h, face.Amounts, r) {
					continue
				}
				return true, nil
			}
		}
	}
	if unresolved != "" {
		return false, fmt.Errorf("engine: Counter: %q's Event$ Counter replacement is not resolvable yet", unresolved)
	}
	return false, nil
}

// counterReplaceResolvable is whether r, an Event$ Counter line with a
// ReplaceWith$ ability, names only params counterReplaced reads.
func counterReplaceResolvable(r *compile.Ability) bool {
	return onlyParams(r, "validcard", "validsa", "validcause")
}

// counterReplaced is ReplacementType.Counter's run for spell, about to be
// countered by cause (CounterEffect.removeFromStack): a line with a
// ReplaceWith$ ability whose ValidCard$/ValidSA$ match the spell and whose
// ValidCause$ matches the countering ability replaces the counter with that
// ability (Guile: "instead exile that spell and you may play that card
// without paying its mana cost"). The spell leaves the stack first, as
// ChangeZone's Origin$ Stack move does in Java; the ability then acts on it
// as the replaced card. ValidCause$ is "SpellAbility", optionally with
// ".YouCtrl" or ".OppCtrl" read against the host's controller; any other form
// records a pending error and skips the line (GO-7). Reports whether a line
// replaced the counter. kind is the countered item's (counterCantHappen): a
// line matching an ability records a pending error and replaces nothing, as
// the replacement's ability acting on an ability's host is not ported (no
// corpus ReplaceWith$ line's ValidSA$ names an ability).
func (g *Game) counterReplaced(controller PlayerController, cause *Ability, spell CardID, kind string) bool {
	spellCard := g.Card(spell)
	res := g.runReplacements(controller, spellCard.Controller(), func() []replacementCandidate {
		var out []replacementCandidate
		g.eachReplacementRule(func(h *Card, z ZoneType, amounts map[string]expr.Amount, r *compile.Ability) {
			sub := replaceWithSub(r)
			if sub == nil || !strings.EqualFold(r.Name, "Counter") || !hostInActiveZones(h, r, z) {
				return
			}
			if !counterReplaceResolvable(r) {
				g.recordPendingError(fmt.Errorf("engine: %q: Event$ Counter: a param is not resolvable yet", h.Def.Name))
				return
			}
			if v, ok := r.Param("ValidCard"); ok && !Matches(g, spellCard, valid.Parse(v), h.Controller(), h.ID) {
				return
			}
			if v, ok := r.Param("ValidSA"); ok {
				matched, recognized := stackValidSA(g, v, spellCard, h, kind)
				if !recognized {
					g.recordPendingError(fmt.Errorf("engine: %q: Event$ Counter: ValidSA$ %q is not resolvable yet", h.Def.Name, v))
					return
				}
				if !matched {
					return
				}
			}
			if kind != causeSpell {
				g.recordPendingError(fmt.Errorf("engine: %q: Event$ Counter: a ReplaceWith$ over a countered ability not resolvable yet", h.Def.Name))
				return
			}
			if v, ok := r.Param("ValidCause"); ok {
				base, rel, _ := strings.Cut(v, ".")
				if base != "SpellAbility" || (rel != "" && rel != "YouCtrl" && rel != "OppCtrl") {
					g.recordPendingError(fmt.Errorf("engine: %q: Event$ Counter: ValidCause$ %q is not resolvable yet", h.Def.Name, v))
					return
				}
				if rel != "" && (rel == "YouCtrl") != (cause.Controller == h.Controller()) {
					return
				}
			}
			if !replacementRequirementsCheck(g, h, amounts, r) {
				return
			}
			out = append(out, replacementCandidate{host: h, rule: r, apply: func() replacementResult {
				kept := g.stack[:0]
				for _, s := range g.stack {
					if s.Source != spell {
						kept = append(kept, s)
					}
				}
				g.stack = kept
				if err := g.runReplacementChain(controller, h, amounts, sub, &replacementEvent{result: replacementReplaced, card: spell}); err != nil {
					g.recordPendingError(err)
				}
				return replacementReplaced
			}})
		})
		return out
	})
	return res == replacementReplaced
}

// stackValidSA is ValidSA$ matched against the stack item about to be
// countered, of kind (causeSpell, causeActivated, causeTriggered), whose card
// is spell (an ability's host): each comma-separated alternative is "Spell"
// with optional "+"-joined properties read off the spell's card as a Card
// valid string, "SpellAbility" (any), or a bare ability kind ("Activated",
// "Triggered") matching an ability of that kind. Properties on an ability
// kind are not read (recognized false).
func stackValidSA(g *Game, spec string, spell, host *Card, kind string) (matched, recognized bool) {
	recognized = true
	for _, alt := range strings.Split(spec, ",") {
		base, props, _ := strings.Cut(alt, ".")
		switch base {
		case "Spell":
			if kind == causeSpell && (props == "" || Matches(g, spell, valid.Parse("Card."+props), host.Controller(), host.ID)) {
				matched = true
			}
		case "SpellAbility":
			if props != "" {
				recognized = false
				continue
			}
			matched = true
		case causeActivated, causeTriggered:
			if props != "" {
				recognized = false
				continue
			}
			matched = matched || base == kind
		default:
			recognized = false
		}
	}
	return matched, recognized
}

// beginPhaseSkipped is ReplacementType.BeginPhase's run for the active
// player at the start of phase (PhaseHandler.advanceToNextPhase): a line
// whose ValidPlayer$ matches the player whose turn it is (absent: every
// player) and whose Phase$ names phase replaces the step. The step is not
// begun at all, the same as a SkipPhase effect (consumeSkip). "Skip$ True"
// is a plain skip; a ReplaceWith$ ability (Fasting's "you gain 2 life") runs
// first, after the line's Optional$ "you may" was confirmed
// (confirmOptionalReplacement).
func (g *Game) beginPhaseSkipped(controller PlayerController, phase PhaseType) bool {
	active := g.ActivePlayer()
	return g.runBeginReplacements(controller, active, "BeginPhase", []string{"validplayer", "phase", "skip", "layer", "hellbent", "optional", "optionaldecider"},
		func(h *Card, r *compile.Ability) bool {
			if v, ok := r.Param("ValidPlayer"); ok {
				if matched, recognized := matchesPlayerSpec(g, active, h.Controller(), h.ID, v); !recognized || !matched {
					return false
				}
			}
			if _, ok := r.Param("Phase"); ok && !phaseTriggerMatches(r, "Phase", phase) {
				return false
			}
			return true
		})
}

// beginTurnSkipped is ReplacementType.BeginTurn's run for pid, who is about
// to take a turn (PhaseHandler.getNextActivePlayer): a line whose
// ValidPlayer$ matches pid, and whose ExtraTurn$ (when present) is true only
// for an extra turn, replaces the turn: "Skip$ True", or a ReplaceWith$
// ability after the Optional$ confirmation (Time Vault's "you may skip that
// turn instead. If you do, untap this artifact"). isExtra is Java's
// "!extraTurns.isEmpty()" after the pop: the entry at the bottom of the
// extra-turn stack is the normal turn the extra ones interrupted.
func (g *Game) beginTurnSkipped(controller PlayerController, pid PlayerID, isExtra bool) bool {
	return g.runBeginReplacements(controller, pid, "BeginTurn", []string{"validplayer", "extraturn", "skip", "optional", "optionaldecider"},
		func(h *Card, r *compile.Ability) bool {
			if v, ok := r.Param("ValidPlayer"); ok {
				if matched, recognized := matchesPlayerSpec(g, pid, h.Controller(), h.ID, v); !recognized || !matched {
					return false
				}
			}
			if v, ok := r.Param("ExtraTurn"); ok && strings.EqualFold(v, "True") && !isExtra {
				return false
			}
			return true
		})
}

// runBeginReplacements is the CR 616 walk shared by BeginPhase and
// BeginTurn, whose affected player is who. matches is the event's own
// ValidPlayer$/Phase$/ExtraTurn$ test; a line naming a param outside allowed
// records a pending error instead (GO-7). A line is a "Skip$ True" or names a
// ReplaceWith$ ability; either replaces the event.
func (g *Game) runBeginReplacements(controller PlayerController, who PlayerID, event string, allowed []string, matches func(h *Card, r *compile.Ability) bool) bool {
	res := g.runReplacements(controller, who, func() []replacementCandidate {
		var out []replacementCandidate
		g.eachReplacementRule(func(h *Card, z ZoneType, amounts map[string]expr.Amount, r *compile.Ability) {
			if !strings.EqualFold(r.Name, event) || !hostInActiveZones(h, r, z) || !matches(h, r) {
				return
			}
			if !replacementRequirementsCheck(g, h, amounts, r) {
				return
			}
			sub := replaceWithSub(r)
			skip, hasSkip := r.Param("Skip")
			plainSkip := hasSkip && strings.EqualFold(skip, "True")
			if !onlyParams(r, allowed...) || (!plainSkip && sub == nil) {
				g.recordPendingError(fmt.Errorf("engine: %q: Event$ %s: only a Skip$ True or ReplaceWith$ line is resolvable yet", h.Def.Name, event))
				return
			}
			out = append(out, replacementCandidate{host: h, rule: r, apply: func() replacementResult {
				if sub != nil {
					if err := g.runReplacementChain(controller, h, amounts, sub, &replacementEvent{result: replacementReplaced}); err != nil {
						g.recordPendingError(err)
						return replacementNotReplaced
					}
				}
				return replacementReplaced
			}})
		})
		return out
	})
	return res == replacementReplaced
}

// lifeReduced is Player.loseLife's replacement run (ReplacementType
// .LifeReduced): amount is the life pid is about to lose, isDamage whether
// damage causes it. It returns what is lost instead, 0 for nothing.
//
// A Layer$ CantHappen line (Archon of Coronation: "damage doesn't cause you
// to lose life") stops the loss before any choice, as the CantHappen layer
// runs first. The rest are CR 616 candidates: Worship and its family's
// "reduces it to 1 instead" (a ReplaceEffect on Amount limited to life minus
// 1) and Bloodletter of Aclazotz's "twice that much". A ValidPlayer$ such as
// "You.lifeGE1" and a Result$ such as "LT1" (the life left after the loss,
// ReplaceLifeReduced.canReplace) both gate a line.
func (g *Game) lifeReduced(controller PlayerController, pid PlayerID, amount int, isDamage bool) int {
	if amount <= 0 {
		return amount
	}
	matches := func(h *Card, z ZoneType, amounts map[string]expr.Amount, r *compile.Ability, amount int) bool {
		if !strings.EqualFold(r.Name, "LifeReduced") || !hostInActiveZones(h, r, z) {
			return false
		}
		if !onlyParams(r, "validplayer", "isdamage", "result", "layer", "monarch") {
			g.recordPendingError(fmt.Errorf("engine: %q: Event$ LifeReduced: a param is not resolvable yet", h.Def.Name))
			return false
		}
		if v, ok := r.Param("ValidPlayer"); ok {
			if matched, recognized := matchesPlayerSpec(g, pid, h.Controller(), h.ID, v); !recognized || !matched {
				return false
			}
		}
		if v, ok := r.Param("IsDamage"); ok && strings.EqualFold(v, "True") != isDamage {
			return false
		}
		if v, ok := r.Param("Result"); ok {
			n, err := strconv.Atoi(v[min(2, len(v)):])
			if len(v) < 3 || err != nil {
				g.recordPendingError(fmt.Errorf("engine: %q: Event$ LifeReduced: Result$ %q is not resolvable", h.Def.Name, v))
				return false
			}
			if !compareOp(g.Player(pid).Life-amount, v[:2], n) {
				return false
			}
		}
		return replacementRequirementsCheck(g, h, amounts, r)
	}
	cant := false
	g.eachReplacementRule(func(h *Card, z ZoneType, amounts map[string]expr.Amount, r *compile.Ability) {
		if replacementIsCantHappen(r) && matches(h, z, amounts, r, amount) {
			cant = true
		}
	})
	if cant {
		return 0
	}
	g.runReplacements(controller, pid, func() []replacementCandidate {
		var out []replacementCandidate
		g.eachReplacementRule(func(h *Card, z ZoneType, amounts map[string]expr.Amount, r *compile.Ability) {
			sub := replaceWithSub(r)
			if sub == nil || !matches(h, z, amounts, r, amount) {
				return
			}
			out = append(out, replacementCandidate{host: h, rule: r, apply: func() replacementResult {
				ev := replacementEvent{amountName: "Amount", amount: amount}
				if !g.runReplaceWith(controller, h, amounts, sub, &ev) {
					g.recordPendingError(fmt.Errorf("engine: %q: Event$ LifeReduced: ReplaceWith$ %s is not resolvable yet", h.Def.Name, sub.Name))
					return replacementNotReplaced
				}
				amount = ev.amount
				return ev.result
			}})
		})
		return out
	})
	return amount
}

// rollReplaced is ReplacementType.RollDice's run for player rolling amount
// dice of sides sides with ignore lowest rolls set aside (RollDiceEffect.
// rollAction, RollDiceEffect.java:403-421): each applying line's
// ReplaceWith$ chain edits the dice count (VarName$ Number) and the lowest
// rolls ignored (VarName$ Ignore) -- Pixie Guide, Wyll and Barbarian Class's
// "roll one more die and ignore the lowest" -- and the edited values are what
// is rolled. ValidPlayer$ and ValidSides$ are ReplaceRollDice.canReplace's.
func (g *Game) rollReplaced(controller PlayerController, player PlayerID, amount, sides, ignore int) (int, int) {
	ev := replacementEvent{amountName: "Number", amount: amount, vars: map[string]int{"ignore": ignore}}
	g.runReplacements(controller, player, func() []replacementCandidate {
		var out []replacementCandidate
		g.eachReplacementRule(func(h *Card, z ZoneType, amounts map[string]expr.Amount, r *compile.Ability) {
			if !strings.EqualFold(r.Name, "RollDice") || !hostInActiveZones(h, r, z) || !rollReplacementResolvable(r) {
				return
			}
			if v, ok := r.Param("ValidPlayer"); ok {
				if matched, recognized := matchesPlayerSpec(g, player, h.Controller(), h.ID, v); !recognized || !matched {
					return
				}
			}
			if v, ok := r.Param("ValidSides"); ok {
				if n, err := strconv.Atoi(v); err != nil || n != sides {
					return
				}
			}
			if !replacementRequirementsCheck(g, h, amounts, r) {
				return
			}
			out = append(out, replacementCandidate{host: h, rule: r, apply: func() replacementResult {
				if err := g.runReplacementChain(controller, h, amounts, replaceWithSub(r), &ev); err != nil {
					g.recordPendingError(err)
					return replacementNotReplaced
				}
				return replacementUpdated
			}})
		})
		return out
	})
	return ev.amount, ev.vars["ignore"]
}

// rollReplacementResolvable reports whether r, an Event$ RollDice line, only
// edits the dice count and the ignored rolls: its ReplaceWith$ chain is
// ReplaceEffect abilities naming VarName$ Number or Ignore. Vedalken Squirrel-
// Whacker's DicePTExchanges (VarType$ CardSet) is not one.
func rollReplacementResolvable(r *compile.Ability) bool {
	if !onlyParams(r, "event", "validplayer", "validsides", "replacewith", "description", "activezones", "secondary") {
		return false
	}
	sub := replaceWithSub(r)
	if sub == nil {
		return false
	}
	for a := sub; a != nil; a = chainNext(a) {
		if !strings.EqualFold(a.Name, "ReplaceEffect") {
			return false
		}
		if _, typed := a.Param("VarType"); typed {
			return false
		}
		if name, _ := a.Param("VarName"); !strings.EqualFold(name, "Number") && !strings.EqualFold(name, "Ignore") {
			return false
		}
	}
	return true
}

// loseManaConversion is ReplacementType.LoseMana's run for pid's unspent mana
// as a step or phase ends (ManaPool.clearPool): a line with ReplaceWith$ a
// ReplaceMana naming ReplaceType$ turns the mana into that type instead of
// emptying it (Kruphix, God of Horizons: colorless; Ozai: red). ok is false
// when no line applies.
func (g *Game) loseManaConversion(controller PlayerController, pid PlayerID) (color mana.Colors, colorless, ok bool) {
	res := g.runReplacements(controller, pid, func() []replacementCandidate {
		var out []replacementCandidate
		g.eachReplacementRule(func(h *Card, z ZoneType, amounts map[string]expr.Amount, r *compile.Ability) {
			if !strings.EqualFold(r.Name, "LoseMana") || !hostInActiveZones(h, r, z) {
				return
			}
			if v, ok := r.Param("ValidPlayer"); ok {
				if matched, recognized := matchesPlayerSpec(g, pid, h.Controller(), h.ID, v); !recognized || !matched {
					return
				}
			}
			sub := replaceWithSub(r)
			if sub == nil || !replacementRequirementsCheck(g, h, amounts, r) {
				return
			}
			out = append(out, replacementCandidate{host: h, rule: r, apply: func() replacementResult {
				// Java asks with the mana "C" and reads back what ReplaceMana made of it.
				ev := replacementEvent{amountName: "Mana", mana: producedMana{colorless: true, amount: 1}}
				if !g.runReplaceWith(controller, h, amounts, sub, &ev) {
					g.recordPendingError(fmt.Errorf("engine: %q: Event$ LoseMana: ReplaceWith$ %s is not resolvable yet", h.Def.Name, sub.Name))
					return replacementNotReplaced
				}
				color, colorless = ev.mana.color, ev.mana.colorless
				return replacementReplaced
			}})
		})
		return out
	})
	return color, colorless, res == replacementReplaced
}
