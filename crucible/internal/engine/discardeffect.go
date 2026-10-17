// Discard: CR 701.8 -- a player moves cards from their own hand to their
// graveyard. Ported from
// forge-game/src/main/java/forge/game/ability/effects/DiscardEffect.java's
// resolve (lines 119-294) and SpellAbilityEffect.discard (lines 917-946).
//
// Who discards (DiscardEffect.java:125-132): getTargetPlayers -- the ability's
// own target players when it carries ValidTgts$ (TargetMin$/TargetMax$ are
// read by resolveTargets, targeting.go, before the ability ever resolves),
// else Defined$, default "You" (targetedOrDefinedPlayers, defined.go). Mode$
// RevealTgtChoose alone reads Defined$ first (getDefinedPlayersOrTargeted): the
// discarders are Defined$ and the first target player chooses.
//
// What each discards, by Mode$ (exact match, as Java's mode.equals):
//
//   - Hand: the whole hand. Optional$ asks the discarder (ConfirmEffect).
//   - Defined: DefinedCards$ (definedCards, defined.go). Optional$ as Hand.
//   - Random: min(NumCards$, hand) cards (1 with no NumCards$) out of the hand
//     cards matching DiscardValid$, drawn with Aggregates.random's reservoir on
//     the game's stream (randomSample, random.go). Optional$ as Hand.
//   - RevealDiscardAll: every hand card matching DiscardValid$.
//   - TgtChoose, YouChoose, LookYouChoose, RevealYouChoose, RevealTgtChoose:
//     the chooser -- the discarder, the activator for *YouChoose, the first
//     target for RevealTgtChoose -- picks from the hand cards matching
//     DiscardValid$ (DiscardEffect.java:226-275): min(valid, NumCards$) of
//     them, 0..that with Optional$, 0..all valid with AnyNumber$. RevealNumber$
//     first has the discarder pick which cards of their hand the chooser may
//     choose from (chooseCardsToRevealFromHand, clamped to the hand the way
//     PlayerControllerHuman.java:1804-1811 clamps it).
//
// Reveal and Look are information only: this engine has no hidden zone, so
// game.getAction().reveal/revealTo have nothing to change (revealhandeffect.go
// carries the same reading).
//
// Every player's cards are collected first and discarded after (Java's
// discardedMap, DiscardEffect.java:134-291), so a later player's choice sees
// the earlier players' hands as they were. Each player's cards are ordered
// for the graveyard by their owner (orderCardsByTheirOwners, zonemove.go;
// GameActionUtil.orderCardsByTheirOwners). discardedMap is a HashMap keyed
// by Player, whose hashCode is its id (GameEntity.java:407), so Java walks it
// in ascending player id; this port sorts by PlayerID to match. A card no
// longer in a hand when its turn comes is skipped (Card.canBeDiscardedBy,
// Card.java:6940-6946). RememberDiscarded$ remembers each card discarded
// (Player.java:1445-1447), RememberDiscardingPlayers$ every player that
// reached discardedMap (DiscardEffect.java:279-281).
//
// Not ported, each failing the whole line before anything moves (PORT-8,
// GO-7): UnlessType$ -- chooseCardsToDiscardUnlessType's "N cards, or one of
// the type" selection, a constraint ChooseCardsForEffect's lo..hi contract
// cannot express; RememberDiscardingPlayer$ (0 corpus lines, Java never reads
// it); a DiscardValid$ property Matches has no case for, or a comparison
// operand it cannot resolve, since either reads false for every card and
// would silently discard nothing (discardValidResolvable). Player.canDiscardBy
// (StaticAbilityCantDiscard) is not consulted: no Mode$ CantDiscard static is
// ported, so no card can make it false.
//
// SubAbility$ chains through resolveSubAbility (subability.go) and
// UnlessCost$ gates the whole ability through resolveUnlessCost (effect.go),
// both outside this effect.

package engine

//enginelint:allow id card game player ability defined amount condition control effecthelpers zone valid parts

import (
	"cmp"
	"fmt"
	"slices"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// discardUnresolvedParams names the params this port rejects (doc comment
// above).
var discardUnresolvedParams = [...]string{"UnlessType", "RememberDiscardingPlayer"}

type discardEffect struct{}

// discardPick is one discardedMap entry: a player and the cards they are to
// discard, in graveyard order.
type discardPick struct {
	player PlayerID
	cards  []CardID
}

func (discardEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range discardUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: Discard: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	mode, ok := a.Params.Param("Mode")
	if !ok {
		return fmt.Errorf("engine: Discard: Mode$ missing")
	}
	switch mode {
	case "Hand", "Defined", "Random", "RevealDiscardAll",
		"TgtChoose", "YouChoose", "LookYouChoose", "RevealYouChoose", "RevealTgtChoose":
	default:
		return fmt.Errorf("engine: Discard: Mode$ %q not resolvable yet", mode)
	}

	validSpec, filtered := a.Params.Param("DiscardValid")
	if !filtered {
		validSpec = "Card"
	}
	if err := discardValidResolvable(g, validSpec, a.Controller, a.Source); err != nil {
		return fmt.Errorf("engine: Discard: DiscardValid$ %q: %w", validSpec, err)
	}

	targets, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Discard: %w", err)
	}
	discarders := targets
	if mode == "RevealTgtChoose" {
		if defined, ok := a.Params.Param("Defined"); ok {
			if discarders, err = definedPlayers(g, a.Controller, a.Source, defined, a.refs()); err != nil {
				return fmt.Errorf("engine: Discard: %w", err)
			}
		}
	}

	picks := make([]discardPick, 0, len(discarders))
	for _, p := range discarders {
		if g.Player(p).Lost {
			continue
		}
		cards, keep, err := discardChoice(g, a, controller, mode, p, targets, validSpec)
		if err != nil {
			return fmt.Errorf("engine: Discard: %w", err)
		}
		if !keep {
			continue
		}
		if cards, err = g.orderCardsByTheirOwners(controller, cards, Graveyard); err != nil {
			return fmt.Errorf("engine: Discard: %w", err)
		}
		picks = append(picks, discardPick{player: p, cards: cards})
	}
	slices.SortStableFunc(picks, func(x, y discardPick) int { return cmp.Compare(x.player, y.player) })

	if _, ok := a.Params.Param("RememberDiscardingPlayers"); ok {
		for _, pick := range picks {
			source.Memory.Remember(PlayerEntity(pick.player))
		}
	}

	_, remember := a.Params.Param("RememberDiscarded")
	discarded := make([][]CardID, len(picks))
	for i, pick := range picks {
		for _, id := range pick.cards {
			if g.Card(id).Zone != Hand {
				continue
			}
			g.discardToGraveyard(controller, id, discardCause{known: true, effect: true, controller: a.Controller})
			g.checkDiscardedTriggers(controller, id, pick.player)
			if remember {
				source.Memory.Remember(CardEntity(id))
			}
			discarded[i] = append(discarded[i], id)
		}
	}
	for i, pick := range picks {
		g.checkDiscardedAllTriggers(controller, pick.player, discarded[i])
	}
	return nil
}

// discardChoice is one pass of DiscardEffect.java's per-player loop: the
// cards p is to discard under mode, and whether p enters discardedMap at all
// (false on each of Java's `continue`s).
func discardChoice(g *Game, a *Ability, controller PlayerController, mode string, p PlayerID, targets []PlayerID, validSpec string) ([]CardID, bool, error) {
	source := g.Card(a.Source)
	hand := slices.Clone(g.Zone(Hand, p).Cards())
	_, optional := a.Params.Param("Optional")

	switch mode {
	case "Defined":
		if optional && !controller.ConfirmEffect(g, p, a.Source) {
			return nil, false, nil
		}
		def, ok := a.Params.Param("DefinedCards")
		if !ok {
			return nil, false, fmt.Errorf("no DefinedCards$ for Mode$ Defined")
		}
		cards, err := definedCards(source, def, a.refs())
		return cards, true, err
	case "Hand":
		if optional && !controller.ConfirmEffect(g, p, a.Source) {
			return nil, false, nil
		}
		return hand, true, nil
	}

	numCards := 1
	if raw, ok := a.Params.Param("NumCards"); ok {
		n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
		if !ok {
			return nil, false, fmt.Errorf("NumCards$ %q is not resolvable", raw)
		}
		numCards = min(n, len(hand))
	}

	switch mode {
	case "Random":
		if optional && !controller.ConfirmEffect(g, p, a.Source) {
			return nil, false, nil
		}
		list := filterValid(g, hand, validSpec, a.Controller, a.Source)
		var cards []CardID
		for _, i := range g.randomSample(len(list), numCards) {
			cards = append(cards, list[i])
		}
		return cards, true, nil
	case "RevealDiscardAll":
		if len(hand) == 0 {
			return nil, false, nil
		}
		return filterValid(g, hand, validSpec, a.Controller, a.Source), true, nil
	}

	// The *YouChoose / *TgtChoose family.
	if len(hand) == 0 {
		return nil, false, nil
	}
	shown := hand
	_, revealNumber := a.Params.Param("RevealNumber")
	if revealNumber {
		raw, _ := a.Params.Param("RevealNumber")
		n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
		if !ok {
			return nil, false, fmt.Errorf("RevealNumber$ %q is not resolvable", raw)
		}
		n = max(0, min(n, len(hand)))
		shown = controller.ChooseCardsForEffect(g, p, a.Source, hand, n, n)
		if err := checkChoice(shown, hand, n, n); err != nil {
			return nil, false, fmt.Errorf("RevealNumber$: %w", err)
		}
	}

	chooser := p
	switch {
	case strings.HasSuffix(mode, "YouChoose"):
		chooser = a.Controller
	case mode == "RevealTgtChoose":
		if len(targets) == 0 {
			return nil, false, fmt.Errorf("no target player to choose for Mode$ RevealTgtChoose")
		}
		chooser = targets[0]
	}

	_, filtered := a.Params.Param("DiscardValid")
	_, anyNumber := a.Params.Param("AnyNumber")
	cands := filterValid(g, shown, validSpec, a.Controller, a.Source)
	hi := min(len(cands), numCards)
	lo := hi
	if anyNumber {
		hi = len(cands)
	}
	if anyNumber || optional {
		lo = 0
	}
	if hi == 0 {
		return nil, false, nil
	}
	var chosen []CardID
	if mode == "TgtChoose" && !filtered && !optional && !anyNumber && !revealNumber {
		// The discarder picks an exact count out of their whole hand:
		// ChooseCardsToDiscard's own contract (control.go).
		chosen = controller.ChooseCardsToDiscard(g, p, hand, hi)
	} else {
		chosen = controller.ChooseCardsForEffect(g, chooser, a.Source, cands, lo, hi)
		if err := checkChoice(chosen, cands, lo, hi); err != nil {
			return nil, false, err
		}
	}
	if len(chosen) == 0 {
		return nil, false, nil
	}
	return chosen, true, nil
}

// discardValidResolvable fails a DiscardValid$ string Matches would answer
// wrongly. Every property the corpus's DiscardValid$ values name has a case
// in Matches (valid.go) except CardProperty's suffixed "SharesColorWith
// <defined>" (Mind Extraction), which reads false for every card. A numeric
// comparison whose operand (cmcEQX's X) does not resolve reads false too, and
// ChosenColor is exact only for a single chosen color (valid.go's own case).
func discardValidResolvable(g *Game, spec string, controller PlayerID, source CardID) error {
	for _, alt := range valid.Parse(spec).Alternatives {
		for _, p := range alt.Properties {
			switch {
			case p.Compare != nil:
				if _, ok := compareOperand(g, p.Compare.Operand, controller, source); !ok {
					return fmt.Errorf("operand %q not resolvable yet", p.Compare.Operand)
				}
			case strings.HasPrefix(p.Name, "SharesColorWith "):
				return fmt.Errorf("property %q not resolvable yet", p.Name)
			case strings.HasPrefix(p.Name, "ChosenColor"):
				if g.Card(source).Memory.ChosenColors().Count() > 1 {
					return fmt.Errorf("property %q over more than one chosen color not resolvable yet", p.Name)
				}
			}
		}
	}
	return nil
}

// discardCards moves each of ids from its owner's hand to their graveyard
// and fires Mode$ Discarded (checkDiscardedTriggers), CR 701.8's own "move
// directly from hand to graveyard" -- a Discard<N/Card> activation cost
// (ActivateAbility, activateability.go) and the other cost sites discard
// through it; the effect above runs the same steps across several players.
// A discard replacement may send a card elsewhere (discardToGraveyard,
// Madness); its cause is not known here, so a line that asks for it is
// refused there.
func discardCards(g *Game, controller PlayerController, ids []CardID, pid PlayerID) {
	for _, id := range ids {
		g.discardToGraveyard(controller, id, discardCause{})
		g.checkDiscardedTriggers(controller, id, pid)
	}
	g.checkDiscardedAllTriggers(controller, pid, ids)
}
