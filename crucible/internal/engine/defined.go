// Defined$ resolution shared across M6's own script-driven effects --
// AbilityUtils.getDefinedPlayers's/getDefinedCards's own real corpus shapes
// this port can resolve. Of the ability-context Triggered* vocabulary only
// the keys a ported trigger mode records resolve (Ability.triggered:
// TriggeredPlayer, TriggeredSource, TriggeredSourceController,
// TriggeredCardController, TriggeredActivator); the rest (TriggeredCard,
// TriggeredController, ...) stay in game-state.md's "Not ported yet". The host-Memory references -- Remembered, Imprinted,
// ChosenCard, ChosenPlayer -- read memory.go. No single effect owns this
// outright, the identical "shared, so neither" reason amount.go's own
// resolveAmount lives apart from its first two callers.

package engine

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// definedPlayers resolves Defined$ to the players it names: "You" (the
// ability's own controller), "Opponent"/"Player.Opponent" (every opponent),
// "Player" (every player in the game, unfiltered -- AbilityUtils.
// getDefinedPlayers's own fallthrough `else` branch, `game.
// getPlayersInTurnOrder()`, reached because a bare "Player" matches none of
// its named cases; "Player.Opponent" does not fall into this branch at all,
// since it is Java's dotted-suffix filter applied to that same fallthrough
// set -- the identical opponents-only result "Opponent" gets directly,
// which is why both are one case here) and "TargetedPlayer"/"Targeted"
// (every PlayerEntity in targets -- resolveTargets's own answer,
// targeting.go -- filtered from a mixed EntityID slice even though no real
// ValidTgts$ line this port evaluates ever actually mixes cards and players
// in one target set, since nothing about Defined$'s own reading enforces
// that), "TargetedController" (the controller of each targeted card),
// "ChosenPlayer" (the host's Memory.ChosenPlayer), "Remembered"/
// "RememberedController"/"RememberedOwner" (rememberedPlayers, below) and
// "DelayTriggerRemembered"/"DelayTriggerRememberedController", the same
// reading applied to what a delayed trigger remembered (Ability.
// TriggerRemembered), and "TriggeredPlayer"/"TriggeredSource"/
// "TriggeredSourceController"/"TriggeredCardController"/
// "TriggeredActivator", what the trigger that made the ability recorded
// (Ability.triggered) -- an error, "the trigger recorded no ...",
// when it recorded no such key, so a trigger mode that never learned to
// set one fails loudly (GO-7). Any other "Player.<property>" is Java's
// fallthrough: every player matching that one property
// (matchesPlayerProperty, valid.go) -- "Player.IsRemembered", the players
// the host remembers, is the corpus's 153 real lines of it. A player no
// longer in the game is skipped, matching Java's own
// `if (!p.isInGame()) continue`.
func definedPlayers(g *Game, controller PlayerID, host CardID, defined string, refs abilityRefs) ([]PlayerID, error) {
	var candidates []PlayerID
	switch defined {
	case "You":
		candidates = []PlayerID{controller}
	case "Player":
		candidates = g.playersInTurnOrder()
	case "Opponent", "Player.Opponent":
		for _, pid := range g.playersInTurnOrder() {
			if pid != controller {
				candidates = append(candidates, pid)
			}
		}
	case "TargetedController":
		for _, e := range refs.targets {
			if id, ok := e.AsCard(); ok {
				candidates = append(candidates, g.Card(id).Controller())
			}
		}
	case "TargetedPlayer", "Targeted", "ThisTargetedPlayer":
		for _, e := range refs.targets {
			if pid, ok := e.AsPlayer(); ok {
				candidates = append(candidates, pid)
			}
		}
	case "ChosenPlayer":
		if pid := g.Card(host).Memory.ChosenPlayer(); pid != NoPlayer {
			candidates = []PlayerID{pid}
		}
	case "Remembered", "RememberedController", "RememberedOwner":
		candidates = rememberedPlayers(g, defined, g.Card(host).Memory.Remembered(), true)
	case "DelayTriggerRemembered":
		candidates = rememberedPlayers(g, "", refs.triggerRemembered, false)
	case "DelayTriggerRememberedController":
		candidates = rememberedPlayers(g, "RememberedController", refs.triggerRemembered, false)
	case "TriggeredPlayer":
		if refs.triggered.player == NoPlayer {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no player", defined)
		}
		candidates = []PlayerID{refs.triggered.player}
	case "TriggeredSource", "TriggeredSourceController":
		// AbilityUtils.getDefinedPlayers' "Triggered" branch over
		// AbilityKey.Source: a player source is itself, a card source
		// answers its controller for the ...Controller spelling. Unset --
		// a trigger mode that does not record Source -- is an error.
		src := refs.triggered.source
		if src == NoEntity {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no source", defined)
		}
		if _, isCard := src.AsCard(); isCard && defined == "TriggeredSource" {
			return nil, fmt.Errorf("engine: Defined$ %q naming a card as players not resolvable yet", defined)
		}
		candidates = []PlayerID{refs.triggered.sourceController}
	case "TriggeredCardController":
		// AbilityUtils.getDefinedPlayers' "...Controller" branch over
		// AbilityKey.Card (AbilityUtils.java:1017-1027): the triggering
		// card's controller now, which for a static trigger is the moment
		// it was tapped (ADR-0020).
		if refs.triggered.card == NoCard {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no card", defined)
		}
		candidates = []PlayerID{g.Card(refs.triggered.card).Controller()}
	case "TriggeredActivator":
		if refs.triggered.activator == NoPlayer {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no activator", defined)
		}
		candidates = []PlayerID{refs.triggered.activator}
	default:
		// getDefinedPlayers' fallthrough (AbilityUtils.java:1186-1198):
		// every player, filtered by the dotted restriction. Only a single
		// property matchesPlayerProperty recognizes resolves; a comma list
		// or an unrecognized property stays an error.
		prop, ok := strings.CutPrefix(defined, "Player.")
		if !ok || strings.Contains(prop, ",") {
			return nil, fmt.Errorf("engine: Defined$ %q not resolvable yet", defined)
		}
		for _, pid := range g.playersInTurnOrder() {
			matched, recognized := matchesPlayerSpec(g, pid, controller, host, defined)
			if !recognized {
				return nil, fmt.Errorf("engine: Defined$ %q not resolvable yet", defined)
			}
			if matched {
				candidates = append(candidates, pid)
			}
		}
	}
	var players []PlayerID
	for _, pid := range candidates {
		if !g.Player(pid).Lost {
			players = append(players, pid)
		}
	}
	return players, nil
}

// rememberedPlayers is AbilityUtils.addPlayer for the three "Remembered"
// Defined$ spellings: a remembered player is taken as-is; a remembered card
// contributes its controller ("RememberedController"), its owner
// ("RememberedOwner"), or -- for plain "Remembered", Java's own
// def.endsWith("Remembered") branch -- the players that card itself
// remembers, one level deep (recurse false), which is Java's own
// skipRemembered guard against Riveteers Overlook's StackOverflow.
func rememberedPlayers(g *Game, defined string, remembered []EntityID, recurse bool) []PlayerID {
	var out []PlayerID
	for _, e := range remembered {
		if pid, ok := e.AsPlayer(); ok {
			out = append(out, pid)
			continue
		}
		cid, ok := e.AsCard()
		if !ok {
			continue
		}
		c := g.Card(cid)
		switch defined {
		case "RememberedController":
			out = append(out, c.Controller())
		case "RememberedOwner":
			out = append(out, c.Owner)
		default:
			if recurse {
				out = append(out, rememberedPlayers(g, defined, c.Memory.Remembered(), false)...)
			}
		}
	}
	return out
}

// definedCards resolves Defined$ to the cards it names, relative to the
// ability's own host card rather than its controller (pumpEffect's first
// caller): "Self" (the host itself, 1,094 of pumpEffect's own 1,147 real
// resolvable lines), "Enchanted"/"Equipped" (what the host -- an Aura or
// an Equipment -- is currently attached to, Card.AttachedTo, empty rather
// than an error when nothing is, matching Java's own
// AbilityUtils.getDefinedCards returning an empty list for an unattached
// Aura/Equipment rather than failing the ability), and "Targeted"/
// "ThisTargetedCard" (every CardEntity in targets -- resolveTargets's own
// answer, targeting.go), the host's own Memory lists: "Remembered"/
// "RememberedCard" (its card entries only), "Imprinted", "ChosenCard", and
// "DelayTriggerRemembered"/"DelayTriggerRememberedLKI" -- the cards a
// delayed or reflexive trigger remembered (Ability.TriggerRemembered) --
// and "TriggeredBlocker"/"TriggeredBlockerLKICopy", what Mode$
// AttackerBlockedByCreature recorded, and "TriggeredAttacker"/
// "TriggeredAttackerLKICopy", what Mode$ Attacks recorded (an error when
// the trigger recorded none), and "ReplacedCard", the card whose entry a
// Moved replacement is replacing (an error anywhere else). A CardID is stable across zone changes, so each pair of spellings
// names the same card; Java's LKI form differs only in which snapshot it
// reads.
func definedCards(host *Card, defined string, refs abilityRefs) ([]CardID, error) {
	switch defined {
	case "Self":
		return []CardID{host.ID}, nil
	case "Enchanted", "Equipped":
		if id, ok := host.AttachedTo(); ok {
			return []CardID{id}, nil
		}
		return nil, nil
	case "Targeted", "ThisTargetedCard":
		var cards []CardID
		for _, e := range refs.targets {
			if id, ok := e.AsCard(); ok {
				cards = append(cards, id)
			}
		}
		return cards, nil
	case "Remembered", "RememberedCard":
		var cards []CardID
		for _, e := range host.Memory.Remembered() {
			if id, ok := e.AsCard(); ok {
				cards = append(cards, id)
			}
		}
		return cards, nil
	case "DelayTriggerRemembered", "DelayTriggerRememberedLKI":
		var cards []CardID
		for _, e := range refs.triggerRemembered {
			if id, ok := e.AsCard(); ok {
				cards = append(cards, id)
			}
		}
		return cards, nil
	case "TriggeredBlocker", "TriggeredBlockerLKICopy":
		if refs.triggered.blocker == NoCard {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no blocker", defined)
		}
		return []CardID{refs.triggered.blocker}, nil
	case "TriggeredAttacker", "TriggeredAttackerLKICopy":
		if refs.triggered.attacker == NoCard {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no attacker", defined)
		}
		return []CardID{refs.triggered.attacker}, nil
	case "ReplacedCard":
		if refs.replaced == NoCard {
			return nil, fmt.Errorf("engine: Defined$ %q outside a replacement of a card's entry", defined)
		}
		return []CardID{refs.replaced}, nil
	case "Imprinted":
		return append([]CardID(nil), host.Memory.Imprinted()...), nil
	case "ChosenCard":
		return append([]CardID(nil), host.Memory.Chosen()...), nil
	default:
		return nil, fmt.Errorf("engine: Defined$ %q not resolvable yet", defined)
	}
}

// targetedOrDefinedCards is SpellAbilityEffect.getTargetCards(sa)'s own
// either/or contract (getCards(false, "Defined", sa), forge-game's own
// SpellAbilityEffect.java): an ability that carries ValidTgts$ itself uses
// its own chosen targets -- a's own Targets field, resolveTargets's own
// answer (targeting.go) against these same Params, already populated before
// Resolve is ever called (pushTriggeredAbilities, trigger.go) -- and Defined$
// is not consulted at all, even if also present on the same line (destroy_
// evil.txt-shaped SubAbility$ chains sometimes carry both, the outer half
// dead). No ValidTgts$ at all falls back to Defined$, defaulting to "Self"
// the same way Java's own getParamOrDefault(definedParam, "Self") does --
// destroyEffect's/tapEffect's/untapEffect's own first caller, DestroyEffect.
// java/TapEffect.java/UntapEffect.java each calling the identical
// getTargetCards(sa) with no definedParam override.
//
// DestroyEffect.java's own dominant real shape (782 of 986 non-DestroyAll
// (AB|DB)$ Destroy lines) names ValidTgts$ alone; TapEffect.java's (413 of
// 577 non-ETB (AB|DB)$ Tap lines) does too -- the first two effects in this
// port to read a's own Targets field directly rather than only through
// definedCards's own "Targeted"/"ThisTargetedCard" case, Ability.Targets's
// own doc comment updated to match (ability.go).
// targetedOrDefinedPlayers is getTargetPlayers(sa)'s own mirror-image
// contract (getPlayers(false, "Defined", sa)), the player-shaped twin of
// targetedOrDefinedCards, above -- millEffect's own first caller
// (millEffect.java calling the identical getTargetPlayers(sa) with no
// definedParam override). Defaults to "You" rather than "Self" when neither
// ValidTgts$ nor Defined$ is present, Java's own
// getParamOrDefault(definedParam, "You") for a player list.
//
// Not ported: getPlayers' own trailing APNAP sort (StartingWith$/the active
// player). This port's own definedPlayers already returns "Player"'s/
// "Opponent"'s own candidates in Players()' own fixed seat order rather than
// turn order -- an established simplification every other effect naming
// Defined$ Player/Opponent already carries (scryEffect's/discardEffect's
// own doc comments), not a new gap Mill introduces.
func targetedOrDefinedPlayers(g *Game, controller PlayerID, host CardID, a *compile.Ability, refs abilityRefs) ([]PlayerID, error) {
	if _, ok := a.Param("ValidTgts"); ok {
		var players []PlayerID
		for _, e := range refs.targets {
			if pid, ok := e.AsPlayer(); ok {
				players = append(players, pid)
			}
		}
		return players, nil
	}
	defined, ok := a.Param("Defined")
	if !ok {
		defined = "You"
	}
	return definedPlayers(g, controller, host, defined, refs)
}

func targetedOrDefinedCards(host *Card, a *compile.Ability, refs abilityRefs) ([]CardID, error) {
	if _, ok := a.Param("ValidTgts"); ok {
		var cards []CardID
		for _, e := range refs.targets {
			if id, ok := e.AsCard(); ok {
				cards = append(cards, id)
			}
		}
		return cards, nil
	}
	defined, ok := a.Param("Defined")
	if !ok {
		defined = "Self"
	}
	return definedCards(host, defined, refs)
}

// definedEntities is AbilityUtils.getDefinedEntities: the players def names,
// then the cards. A spelling only one of the two readers knows yields that
// side alone; one neither knows is an error.
func definedEntities(g *Game, controller PlayerID, host *Card, def string, refs abilityRefs) ([]EntityID, error) {
	var out []EntityID
	players, perr := definedPlayers(g, controller, host.ID, def, refs)
	for _, p := range players {
		out = append(out, PlayerEntity(p))
	}
	cards, cerr := definedCards(host, def, refs)
	for _, c := range cards {
		out = append(out, CardEntity(c))
	}
	if perr != nil && cerr != nil {
		return nil, cerr
	}
	return out, nil
}
