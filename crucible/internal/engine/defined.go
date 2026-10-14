// Defined$ resolution shared across M6's own script-driven effects --
// AbilityUtils.getDefinedPlayers's/getDefinedCards's own real corpus shapes
// this port can resolve. Of the ability-context Triggered* vocabulary only
// the keys a ported trigger mode records resolve (Ability.triggered, whose
// field comments name the modes recording each key); a key the trigger did
// not record is an error. The host-Memory references -- Remembered, Imprinted,
// ChosenCard, ChosenPlayer -- read memory.go. No single effect owns this
// outright, the identical "shared, so neither" reason amount.go's own
// resolveAmount lives apart from its first two callers.

package engine

import (
	"fmt"
	"slices"
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
// "TriggeredSourceController"/"TriggeredCardController"/"TriggeredCardOwner"/
// "TriggeredActivator", what the trigger that made the ability recorded
// (Ability.triggered) -- an error, "the trigger recorded no ...",
// when it recorded no such key, so a trigger mode that never learned to
// set one fails loudly (GO-7). "ReplacedPlayer"/"ReplacedDefendingPlayer"
// are a DeclareBlocker replacement's two replacing objects (ADR-0035), an
// error the same way outside one. Any other "Player.<property>" is Java's
// fallthrough: every player matching that one property
// (matchesPlayerProperty, valid.go) -- "Player.IsRemembered", the players
// the host remembers, is the corpus's 153 real lines of it. A player no
// longer in the game is skipped, matching Java's own
// `if (!p.isInGame()) continue`.
func definedPlayers(g *Game, controller PlayerID, host CardID, defined string, refs abilityRefs) ([]PlayerID, error) {
	// AbilityUtils.getDefinedPlayers splits the string on "&" and gathers
	// the players of each part ("You & ReplacedPlayer"), each once.
	if strings.Contains(defined, "&") {
		var all []PlayerID
		for _, part := range strings.Split(defined, "&") {
			ps, err := definedPlayers(g, controller, host, strings.TrimSpace(part), refs)
			if err != nil {
				return nil, err
			}
			for _, p := range ps {
				if !slices.Contains(all, p) {
					all = append(all, p)
				}
			}
		}
		return all, nil
	}
	if base, filter, ok := strings.Cut(defined, "."); ok && strings.HasPrefix(base, "Triggered") {
		return triggeredPlayersFiltered(g, controller, host, base, filter, refs)
	}
	// AbilityUtils.java:1104-1106: "Non<spec>" is every player but those spec names
	// (NonTriggeredCardController, Confusion in the Ranks).
	if rest, ok := strings.CutPrefix(defined, "Non"); ok && rest != "" {
		excluded, err := definedPlayers(g, controller, host, rest, refs)
		if err != nil {
			return nil, err
		}
		var out []PlayerID
		for _, pid := range g.playersInTurnOrder() {
			if !slices.Contains(excluded, pid) {
				out = append(out, pid)
			}
		}
		return out, nil
	}
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
	case "TargetedController", "ThisTargetedController":
		// ThisTargetedController (AbilityUtils.java:976-982) reads
		// ThisTargetedCard, which definedCards answers from the same
		// targets as Targeted.
		for _, e := range refs.targets {
			if id, ok := e.AsCard(); ok {
				candidates = append(candidates, g.Card(id).Controller())
			}
		}
	case "ParentTargetedController":
		// AbilityUtils.java:987-993: the controller of each card the nearest
		// targeting ancestor targets (definedCards' ParentTarget), plus the
		// activators of targeted stack items, which the engine reads off
		// the card targets of a spell. An ancestor that targets only players
		// names no card, so Java's list is empty and so is this one.
		if len(refs.parentTargets) == 0 {
			return nil, fmt.Errorf("engine: Defined$ %q: no ancestor ability chose targets", defined)
		}
		for _, e := range refs.parentTargets {
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
	case "ParentTarget":
		// AbilityUtils.java:947-951: the players the nearest ancestor that
		// targets a player targets (SpellAbility.java:2024-2032,
		// getParentTargetingPlayer). parentTargetsOf stops at the first
		// ancestor targeting anything -- the same one whenever it targets a
		// player (Dichotomancy's DBShuffle). One targeting only cards may hide
		// a player-targeting grandparent Java would reach, so it is an error,
		// as is no targeting ancestor at all (definedCards' own ParentTarget).
		if len(refs.parentTargets) == 0 {
			return nil, fmt.Errorf("engine: Defined$ %q: no ancestor ability chose targets", defined)
		}
		for _, e := range refs.parentTargets {
			if pid, ok := e.AsPlayer(); ok {
				candidates = append(candidates, pid)
			}
		}
		if len(candidates) == 0 {
			return nil, fmt.Errorf("engine: Defined$ %q: the nearest targeting ancestor targets no player, not resolvable yet", defined)
		}
	case "EnchantedPlayer":
		// AbilityUtils.java:1109: the player the host enchants.
		if pid, ok := g.Card(host).AttachedToPlayer(); ok {
			candidates = []PlayerID{pid}
		}
	case "Enchanted", "EnchantedController", "EnchantedOwner":
		// AbilityUtils.java:1113 (addPlayer): what the host is attached to,
		// as a player -- itself, or the controller/owner of an enchanted
		// card. An enchanted card under plain "Enchanted" names no player.
		if pid, ok := g.Card(host).AttachedToPlayer(); ok {
			candidates = []PlayerID{pid}
		} else if id, ok := g.Card(host).AttachedTo(); ok {
			switch defined {
			case "EnchantedController":
				candidates = []PlayerID{g.Card(id).Controller()}
			case "EnchantedOwner":
				candidates = []PlayerID{g.Card(id).Owner}
			}
		}
	case "AttackingPlayer":
		// AbilityUtils.java:1122-1125: the combat's attacking player, nobody
		// outside combat (a static reads this on every recompute, so outside
		// combat is an empty answer, not an error).
		if g.activePhase.IsCombat() {
			candidates = []PlayerID{g.activePlayer}
		}
	case "ChosenPlayer":
		if pid := g.Card(host).Memory.ChosenPlayer(); pid != NoPlayer {
			candidates = []PlayerID{pid}
		}
	case "Remembered", "RememberedController", "RememberedOwner":
		candidates = rememberedPlayers(g, defined, g.Card(host).Memory.Remembered(), true)
	case "ImprintedController", "ImprintedOwner":
		// AbilityUtils.java:996-997: addPlayer over the imprinted cards, the
		// Remembered branch's own reading of each card.
		var imprinted []EntityID
		for _, id := range g.Card(host).Memory.Imprinted() {
			imprinted = append(imprinted, CardEntity(id))
		}
		candidates = rememberedPlayers(g, "Remembered"+strings.TrimPrefix(defined, "Imprinted"), imprinted, false)
	case "DelayTriggerRemembered":
		candidates = rememberedPlayers(g, "", refs.triggerRemembered, false)
	case "DelayTriggerRememberedController":
		candidates = rememberedPlayers(g, "RememberedController", refs.triggerRemembered, false)
	case "TriggeredPlayer":
		if refs.triggered.player == NoPlayer {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no player", defined)
		}
		candidates = []PlayerID{refs.triggered.player}
	case "TriggeredTarget":
		// AbilityUtils.getDefinedPlayers' "Triggered" branch over
		// AbilityKey.Target: a player target is itself; a card target names
		// no player (Java adds only a Player object here). Unset is an error.
		if refs.triggered.target == NoEntity {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no target", defined)
		}
		if pid, ok := refs.triggered.target.AsPlayer(); ok {
			candidates = []PlayerID{pid}
		}
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
	case "TriggeredCardOwner":
		// The "...Owner" branch over AbilityKey.Card (AbilityUtils.java:1053-1059):
		// the triggering card's owner. Java answers no player for an unset key;
		// here that is an error, as for every Triggered key (GO-7).
		if refs.triggered.card == NoCard {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no card", defined)
		}
		candidates = []PlayerID{g.Card(refs.triggered.card).Owner}
	case "TriggeredDefendingPlayer":
		// AbilityKey.DefendingPlayer of Mode$ Attacks: the player defending
		// against the attacker the trigger recorded (defenderOf).
		if refs.triggered.attacker == NoCard {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no attacker", defined)
		}
		candidates = []PlayerID{g.defenderOf(refs.triggered.attacker)}
	case "TriggeredOriginalController":
		// AbilityUtils.java:1022-1027 (the "OrController"/"OriginalController"
		// suffix): a Player recorded under OriginalController is itself.
		if refs.triggered.originalController == NoPlayer {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no original controller", defined)
		}
		candidates = []PlayerID{refs.triggered.originalController}
	case "TriggeredAttackingPlayer":
		if refs.triggered.attackingPlayer == NoPlayer {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no attacking player", defined)
		}
		candidates = []PlayerID{refs.triggered.attackingPlayer}
	case "TriggeredActivator":
		if refs.triggered.activator == NoPlayer {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no activator", defined)
		}
		candidates = []PlayerID{refs.triggered.activator}
	case "ReplacedPlayer", "ReplacedDefendingPlayer":
		// AbilityUtils.getDefinedPlayers' "Replaced" branch
		// (AbilityUtils.java:1097-1103): the replacing object of that key.
		// Only a DeclareBlocker replacement (ADR-0035) records either here;
		// the per-event dispatches in replacement.go that read "the player
		// who would have drawn/gained" do so on their own, never through
		// this function. Unset is an error, never an empty answer (GO-7).
		pid := refs.replacedPlayer
		if defined == "ReplacedDefendingPlayer" {
			pid = refs.replacedDefendingPlayer
		}
		if pid == NoPlayer {
			return nil, fmt.Errorf("engine: Defined$ %q outside a replacement that records it", defined)
		}
		candidates = []PlayerID{pid}
	default:
		// getDefinedPlayers' fallthrough (AbilityUtils.java:1186-1198):
		// every player, filtered by the dotted restriction; "You.<p>" and
		// "Opponent.<p>" are the You and Opponent players filtered the same
		// way (:1173-1176, :1194-1200), which matchesPlayerSpec's own base
		// check gives. Only a single property matchesPlayerProperty
		// recognizes resolves; a comma list or an unrecognized property stays
		// an error.
		base, prop, dotted := strings.Cut(defined, ".")
		if !dotted || base != "Player" && base != "You" && base != "Opponent" || strings.Contains(prop, ",") {
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
		// getDefinedPlayers returns a PlayerCollection: each player once, so two
		// targeted cards of one controller ask that player once.
		if !g.Player(pid).Lost && !slices.Contains(players, pid) {
			players = append(players, pid)
		}
	}
	return players, nil
}

// triggeredPlayersFiltered is getDefinedPlayers' trailing valid filter
// (AbilityUtils.java:1186-1196) applied to a Triggered<Key> player: the
// text after the first "." is a comma list of restrictions, each prefixed
// "Player." and kept through PlayerPredicates.restriction, so
// "TriggeredAttackingPlayer.Opponent+controlsCreature.attacking
// Player.EnchantedBy" (Curse of Vitality and two more) keeps the triggering
// attacking player only while it is an opponent of the ability's controller
// controlling a creature that attacks the enchanted player. A filter
// matchesPlayerSpec cannot evaluate is an error, never an empty answer
// (GO-7).
func triggeredPlayersFiltered(g *Game, controller PlayerID, host CardID, base, filter string, refs abilityRefs) ([]PlayerID, error) {
	players, err := definedPlayers(g, controller, host, base, refs)
	if err != nil {
		return nil, err
	}
	alts := strings.Split(filter, ",")
	for i, alt := range alts {
		alts[i] = "Player." + alt
	}
	spec := strings.Join(alts, ",")
	var out []PlayerID
	for _, pid := range players {
		matched, recognized := matchesPlayerSpec(g, pid, controller, host, spec)
		if !recognized {
			return nil, fmt.Errorf("engine: Defined$ %q.%q: filter not resolvable yet", base, filter)
		}
		if matched {
			out = append(out, pid)
		}
	}
	return out, nil
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
// the trigger recorded none), "TriggeredSource"/"TriggeredSourceLKICopy",
// a DamageDone trigger's card source (none for a player source), and
// "ReplacedCard", the card whose entry a
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
	case "ParentTarget":
		var cards []CardID
		for _, e := range refs.parentTargets {
			if id, ok := e.AsCard(); ok {
				cards = append(cards, id)
			}
		}
		// An ancestor that names ValidTgts$ but holds no target now -- its
		// targets dropped as illegal (CR 608.2b), or TargetMin$ 0 chose none --
		// is Java's null getParentTargetingCard: no cards
		// (AbilityUtils.java:162-166). No targeting ancestor at all is a
		// reference this port does not carry, an error.
		if len(refs.parentTargets) == 0 && !refs.ancestorTargeting {
			return nil, fmt.Errorf("engine: Defined$ %q: no ancestor ability chose targets", defined)
		}
		return cards, nil
	case "Sacrificed":
		if !refs.paid.recorded {
			return nil, fmt.Errorf("engine: Defined$ %q: no cost recorded what it sacrificed", defined)
		}
		return append([]CardID(nil), refs.paid.sacrificed...), nil
	case "Collected":
		// The evidence the spell's own cast collected (CostCollectEvidence's
		// paid list): only a spell is its cast SpellAbility (spellOptional).
		return append([]CardID(nil), host.spellCollected()...), nil
	case "CastSA>Collected":
		// The same list read off the permanent's cast SpellAbility
		// (AbilityUtils.adjustTriggerContext); none for a card not cast.
		if !host.wasCast {
			return nil, nil
		}
		return append([]CardID(nil), host.castOpt.collected...), nil
	case "Remembered", "RememberedCard", "RememberedLKI", "DirectRemembered":
		// DirectRemembered (AbilityUtils.java:231-246) is the host's remembered
		// cards plus those of its current state; a card here has one Memory.
		var cards []CardID
		for _, e := range host.Memory.Remembered() {
			if id, ok := e.AsCard(); ok {
				cards = append(cards, id)
			}
		}
		if defined == "DirectRemembered" && len(cards) == 0 {
			// A delayed trigger whose host already left and cleared its Memory
			// (Dance of the Dead's "leaves the battlefield" sacrifice): Java reads
			// the host's last-known remembered list, which this port does not
			// keep; the delayed trigger's own RememberObjects$ is that list for
			// every real DirectRemembered user that registers one.
			for _, e := range refs.triggerRemembered {
				if id, ok := e.AsCard(); ok {
					cards = append(cards, id)
				}
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
	case "TriggeredTarget", "TriggeredTargetLKICopy":
		// AbilityKey.Target (Mode$ Attached, DamageDone, DamageDoneOnce,
		// BecomesTarget: triggeredObjects.target). A player is not a card, so
		// it yields none here.
		if refs.triggered.target == NoEntity {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no target", defined)
		}
		if id, ok := refs.triggered.target.AsCard(); ok {
			return []CardID{id}, nil
		}
		return nil, nil
	case "TriggeredCards", "TriggeredCardsLKICopy":
		if len(refs.triggered.cards) == 0 {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no cards", defined)
		}
		return slices.Clone(refs.triggered.cards), nil
	case "TriggeredCard", "TriggeredCardLKICopy", "TriggeredNewCard", "TriggeredNewCardLKICopy":
		// AbilityKey.NewCard of a zone-change trigger: the card as it is now.
		// A CardID is stable across the move, so it is the card that moved.
		// TriggeredCard and TriggeredCardLKICopy are AbilityKey.Card, which
		// differs from NewCard only for an Origin$ Battlefield trigger
		// (TriggerChangesZone.java:192-197, the pre-move copy) -- one CardID
		// here. Soulbond's Bond and Krovikan Vampire's Pump read them.
		if refs.triggered.card == NoCard {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no card", defined)
		}
		return []CardID{refs.triggered.card}, nil
	case "TriggeredSource", "TriggeredSourceLKICopy":
		// AbilityKey.Source as a card (AbilityUtils.java:167-199): a player
		// source names no card. Unset is an error, as for the other keys.
		if refs.triggered.source == NoEntity {
			return nil, fmt.Errorf("engine: Defined$ %q: the trigger recorded no source", defined)
		}
		if id, ok := refs.triggered.source.AsCard(); ok {
			return []CardID{id}, nil
		}
		return nil, nil
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
