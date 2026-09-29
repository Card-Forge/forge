package engine

//enginelint:allow ability amount card castspell condition control defined effecthelpers game id land parts player trigger valid zone zonemove

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// playEffect is PlayEffect.java: the caster (Controller$, default the
// activator) plays up to Amount$ (default 1; All = every candidate) of the
// candidate cards, one at a time, choosing which (Optional$ lets them stop)
// -- casting each during this resolution with timing ignored (CR 608.2g),
// without paying its mana cost under WithoutManaCost$, or playing a land when
// it is their turn and they have a land drop left (CR 305.3). A cast spell
// goes on the stack above whatever is resolving, through the same cast path
// CastSpell uses (castSpell, castspell.go), so it resolves next.
//
// Candidates are Valid$ in ValidZone$ (default Hand) across every player, or
// else the ability's own targets / Defined$ cards (default Self). ValidSA$
// keeps a card only if one of its play options matches (validSAMatches).
// CopyCard$ casts a token copy of the chosen card, made in the chosen card's
// zone, instead. RememberPlayed$/ImprintPlayed$/ForgetPlayed$/
// ForgetRemembered$ record a successful play on the host.
// ShowCardToActivator$ is display-only in Java (revealTo) and changes no
// state.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/PlayEffect.java's
// resolve (PlayEffect.java:84-493) and AbilityUtils.getSpellsFromPlayEffect
// (AbilityUtils.java:2910-2979).
type playEffect struct{}

// playUnresolvedParams are PlayEffect params this port does not resolve,
// each rejected before anything is played (PORT-8, GO-7):
// ReplaceGraveyard$/ReplaceGraveyardValid$ (35 lines; "exile it instead of
// putting it anywhere else" is an effect-card replacement on the spell's own
// trip off the stack, and the Stack -> Graveyard move has no replacement
// hook, ADR-0018 Decision point 3); a card built from outside the game
// (CopyFromChosenName$, AnySupportedCard$ and its RandomCopied$/RandomNum$/
// ChoiceNum$); alternate states (CastFaceDown$, CastTransformed$,
// ReplaceIlluMask$); alternative or modified costs (PlayCost$,
// PlayReduceCost$, PlayRaiseCost$, ManaConversion$); ControlledByPlayer$
// (Mindslaver control of the caster), WithTotalCMC$ (a running mana-value
// budget), ShowCards$ (a second, display-only list), ZoneRegardless$
// (equalsWithGameTimestamp, which this port cannot tell apart);
// TgtZone$ (resolveTargets scans the battlefield only, so the targets were
// chosen from the wrong zone); Condition$/ConditionDefined$, which
// subAbilityConditionMet reads as "never met" rather than evaluating.
var playUnresolvedParams = [...]string{
	"ReplaceGraveyard", "ReplaceGraveyardValid", "CopyFromChosenName", "AnySupportedCard", "RandomCopied",
	"RandomNum", "ChoiceNum", "CastFaceDown", "CastTransformed", "ReplaceIlluMask", "PlayCost", "PlayReduceCost",
	"PlayRaiseCost", "ManaConversion", "ControlledByPlayer", "WithTotalCMC", "ShowCards", "ZoneRegardless",
	"TgtZone", "Condition", "ConditionDefined",
}

// playUnportedProperties are card-property prefixes in the corpus's Play
// valid strings that Matches (valid.go) has no case for and would read as
// false for every card, silently emptying the pool (GO-7): ExiledWith[Source]
// (23 lines), TargetedPlayerCtrl (6), OwnedBy/ControlledBy <Defined> (8),
// sharesNameWith/sharesCardTypeWith, faceUp, NotedFor..., IsCommander,
// CanPayManaCost.
var playUnportedProperties = [...]string{
	"ExiledWith", "TargetedPlayerCtrl", "OwnedBy", "ControlledBy", "sharesNameWith", "sharesCardTypeWith",
	"faceUp", "NotedFor", "IsCommander", "CanPayManaCost",
}

func (playEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "Play", playUnresolvedParams[:]...); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	caster := a.Controller
	if raw, ok := a.Params.Param("Controller"); ok {
		players, err := definedPlayers(g, a.Controller, a.Source, raw, a.refs())
		if err != nil {
			return fmt.Errorf("engine: Play: Controller$: %w", err)
		}
		if len(players) == 0 {
			return fmt.Errorf("engine: Play: Controller$ %q names no player", raw)
		}
		caster = players[0]
	}
	cards, err := playCandidates(g, a)
	if err != nil {
		return err
	}
	validSA, hasValidSA := a.Params.Param("ValidSA")
	if hasValidSA {
		var kept []CardID
		for _, id := range cards {
			keep, err := cardHasPlayOption(g, a, caster, id, validSA)
			if err != nil {
				return err
			}
			if keep {
				kept = append(kept, id)
			}
		}
		cards = kept
	}
	if len(cards) == 0 {
		return nil
	}

	var amount int
	if raw, ok := a.Params.Param("Amount"); ok && raw == "All" {
		amount = len(cards)
	} else if amount, err = optionalAmount(g, a, "Play", "Amount", 1); err != nil {
		return err
	}
	optional := hasParam(a, "Optional")
	singleOption := len(cards) == 1 && amount == 1 && optional
	allowRepeats := hasParam(a, "AllowRepeats")
	withoutMana := hasParam(a, "WithoutManaCost")

	for len(cards) > 0 && amount > 0 {
		// chooseSingleEntityForEffect, optional unless this is the single
		// option (which is confirmed below instead).
		lo := 1
		if optional && !singleOption {
			lo = 0
		}
		pick := cards[0]
		if lo == 0 || len(cards) > 1 {
			chosen := controller.ChooseCardsForEffect(g, caster, a.Source, cards, lo, 1)
			if err := checkChoice(chosen, cards, lo, 1); err != nil {
				return fmt.Errorf("engine: Play: %w", err)
			}
			if len(chosen) == 0 {
				break
			}
			pick = chosen[0]
		}
		if singleOption && !controller.ConfirmEffect(g, caster, a.Source) {
			break
		}
		if !allowRepeats {
			cards = withoutCards(cards, []CardID{pick})
		}
		chosenCard := pick
		if hasParam(a, "CopyCard") {
			orig := g.Card(pick)
			origZone, origZoneOwner := orig.Zone, orig.ZoneOwner
			// PlayEffect.java:273-281 adds the token to the original card's
			// own Zone object (zone.add(tgtCard)), not the caster's: Owner is
			// caster, but the zone it lands in is origZoneOwner's whenever
			// the two differ (e.g. copying an opponent's exiled card).
			pick = g.NewCard(orig.Def, caster, origZone)
			if origZoneOwner != caster {
				g.Zone(origZone, caster).cards.Remove(pick)
				g.put(pick, origZone, origZoneOwner)
			}
			g.Card(pick).IsToken = true
		}

		opt, ok, err := playOptionFor(g, a, caster, pick, validSA, hasValidSA)
		if err != nil {
			return err
		}
		if !ok {
			// PlayEffect.java:312's `continue`: nothing to play, and amount is
			// not spent.
			if err := playRepeatLoop(allowRepeats, g.Card(chosenCard), "has no play option"); err != nil {
				return err
			}
			continue
		}
		origin := g.Card(pick).Zone
		if opt.land {
			g.playLandNow(controller, caster, pick)
			playRecord(g, a, pick, chosenCard)
			g.checkChangesZoneAllTriggers(controller, []CardID{pick}, origin, Battlefield)
			amount--
			continue
		}
		if !withoutMana && g.Card(pick).Def.Faces[0].ManaCost.IsNoCost() {
			// PlayEffect.java:387-389: a spell with no mana cost cannot be
			// paid for, so it is skipped, amount not spent.
			if err := playRepeatLoop(allowRepeats, g.Card(chosenCard), "has no mana cost to pay"); err != nil {
				return err
			}
			continue
		}
		if g.castSpell(controller, caster, pick, castOpts{withoutManaCost: withoutMana}) {
			playRecord(g, a, pick, chosenCard)
			g.checkChangesZoneAllTriggers(controller, []CardID{pick}, origin, Stack)
		}
		amount--
	}
	return nil
}

// playRepeatLoop is the error for a card Play cannot play under
// AllowRepeats$: Java's `continue` at PlayEffect.java:312/:389 leaves amount
// unspent and, with AllowRepeats$, the card still in the pool, so Java would
// offer it again forever (forge-java-defects.md). Neither real AllowRepeats$
// line reaches it; this refuses rather than looping or silently diverging.
func playRepeatLoop(allowRepeats bool, c *Card, why string) error {
	if !allowRepeats {
		return nil
	}
	return fmt.Errorf("engine: Play: AllowRepeats$ with %q, which %s, would repeat forever (PlayEffect.java:312)", c.Def.Name, why)
}

// playRecord is PlayEffect's bookkeeping after a successful play of played
// (the chosen card, or its CopyCard$ token): RememberPlayed$ and
// ImprintPlayed$ record what was played, ForgetRemembered$ clears the host's
// remembered list, ForgetPlayed$ forgets the chosen card (tgtCard in Java,
// PlayEffect.java:473 -- the original, not the copy).
func playRecord(g *Game, a *Ability, played, chosen CardID) {
	// Re-read: a CopyCard$ token's NewCard call may have grown the arena and
	// moved g.cards, so a *Card taken before it can point into a stale
	// backing array whose lazily-allocated Memory.remembered writes are lost
	// (effecteffect.go:143's own convention).
	source := g.Card(a.Source)
	if hasParam(a, "RememberPlayed") {
		source.Memory.Remember(CardEntity(played))
	}
	if hasParam(a, "ImprintPlayed") {
		source.Memory.Imprint(played)
	}
	if hasParam(a, "ForgetRemembered") {
		source.Memory.ClearRemembered()
	}
	if hasParam(a, "ForgetPlayed") {
		source.Memory.Forget(CardEntity(chosen))
	}
}

// playCandidates is PlayEffect's tgtCards: Valid$ filtered over ValidZone$
// (default Hand), zone by zone and each zone across every player
// (Game.getCardsIn(Iterable<ZoneType>)), with the ability's own activator as
// the valid string's "You" (AbilityUtils.filterListByType); or else the
// ability's targets / Defined$ cards (getTargetCards, default Self).
func playCandidates(g *Game, a *Ability) ([]CardID, error) {
	if spec, ok := a.Params.Param("Valid"); ok {
		if prop, gap := playSpecGap(spec); gap {
			return nil, fmt.Errorf("engine: Play: Valid$ property %q not resolvable yet", prop)
		}
		zones := []ZoneType{Hand}
		if raw, ok := a.Params.Param("ValidZone"); ok {
			parsed, err := parseZoneList(raw)
			if err != nil {
				return nil, fmt.Errorf("engine: Play: ValidZone$: %w", err)
			}
			zones = parsed
		}
		var all []CardID
		for _, zone := range zones {
			for _, pid := range g.Players() {
				all = append(all, g.Zone(zone, pid).Cards()...)
			}
		}
		return filterValid(g, all, spec, a.Controller, a.Source), nil
	}
	cards, err := targetedOrDefinedCards(g.Card(a.Source), a.Params, a.refs())
	if err != nil {
		return nil, fmt.Errorf("engine: Play: %w", err)
	}
	return cards, nil
}

// playSpecGap names the first property of a card valid string Matches
// cannot answer: a numeric comparison against a non-literal operand
// (compareMatches reads only a plain integer) or a playUnportedProperties
// prefix.
func playSpecGap(spec string) (string, bool) {
	for _, alt := range valid.Parse(spec).Alternatives {
		for _, p := range alt.Properties {
			if p.Compare != nil {
				if _, err := strconv.Atoi(p.Compare.Operand); err != nil {
					return p.Name, true
				}
				continue
			}
			for _, prefix := range playUnportedProperties {
				if strings.HasPrefix(p.Name, prefix) {
					return p.Name, true
				}
			}
		}
	}
	return "", false
}

// playOption is one of a card's play options under Play: its land play, or
// its one spell.
type playOption struct {
	land bool
}

// playOptionsOf is AbilityUtils.getSpellsFromPlayEffect for card: a land's
// play when caster could play a land now (their turn, a land drop left, CR
// 305.3), else its one spell. ok is false for a card whose spell this port
// cannot cast honestly (playCastGap): Java would offer it, so the ValidSA$
// pre-filter keeps it and choosing it is an error.
func playOptionsOf(g *Game, caster PlayerID, card CardID) ([]playOption, bool) {
	c := g.Card(card)
	if playCastGap(c) != "" {
		return nil, false
	}
	t := c.Type()
	switch {
	case t.Has(cardtype.Land):
		if g.activePlayer == caster && g.hasLandDrop(caster) {
			return []playOption{{land: true}}, true
		}
		return nil, true
	case t.HasSubtype("Aura") || castableAsPermanent(c) || castableAsInstantOrSorcery(c):
		return []playOption{{}}, true
	}
	return nil, true
}

// playCastGap is why castSpell cannot cast c the way Java's Play would, or
// "" when it can: a split, adventure, omen, modal or prepare card offers a
// second spell to choose between (getAbilityToPlay, no PlayerController
// decision here); an instant or sorcery with no A:SP$ line, more than one, or
// a Cost$ on it (an additional cost castInstantOrSorcery does not pay).
func playCastGap(c *Card) string {
	switch c.Def.SplitType {
	case carddb.SplitNone, carddb.SplitTransform, carddb.SplitMeld, carddb.SplitFlip, carddb.SplitSpecialize:
	default:
		return "a " + c.Def.SplitType.String() + " card's choice of spells"
	}
	if c.Type().Has(cardtype.Land) || !castableAsInstantOrSorcery(c) {
		return ""
	}
	sp := firstSpellAbility(c)
	if sp == nil {
		return "an instant or sorcery with no A:SP$ line"
	}
	n := 0
	for _, ab := range c.Def.Faces[0].Abilities {
		if ab.Record == compile.Spell {
			n++
		}
	}
	if n > 1 {
		return "a choice between its A:SP$ lines"
	}
	if _, ok := sp.Param("Cost"); ok {
		return "its A:SP$ Cost$ additional cost"
	}
	return ""
}

// cardHasPlayOption is PlayEffect's ValidSA$ pre-filter (PlayEffect.java:198):
// whether card has an option validSA matches. A card this port cannot build
// options for is kept -- Java would offer it -- and fails only if chosen.
func cardHasPlayOption(g *Game, a *Ability, caster PlayerID, card CardID, validSA string) (bool, error) {
	opts, ok := playOptionsOf(g, caster, card)
	if !ok {
		return true, nil
	}
	for _, opt := range opts {
		matched, err := validSAMatches(g, a, caster, card, opt, validSA)
		if err != nil || matched {
			return matched, err
		}
	}
	return false, nil
}

// playOptionFor is the option Play uses for the chosen card, and whether it
// has one; a spell this port cannot cast is an error (GO-7).
func playOptionFor(g *Game, a *Ability, caster PlayerID, card CardID, validSA string, hasValidSA bool) (playOption, bool, error) {
	opts, ok := playOptionsOf(g, caster, card)
	if !ok {
		c := g.Card(card)
		return playOption{}, false, fmt.Errorf("engine: Play: casting %q (%s) not resolvable yet", c.Def.Name, playCastGap(c))
	}
	for _, opt := range opts {
		if !hasValidSA {
			return opt, true, nil
		}
		matched, err := validSAMatches(g, a, caster, card, opt, validSA)
		if err != nil {
			return playOption{}, false, err
		}
		if matched {
			return opt, true, nil
		}
	}
	return playOption{}, false, nil
}

// validSAMatches is SpellAbility.isValid (SpellAbility.java:2206-2274) for
// one of card's play options against ValidSA$ spec, each comma alternative
// OR'd. The head is the ability kind: Spell (not the land play),
// SpellAbility (anything), LandAbility/Ability/Static (the land play: Java's
// LandAbility is an AbilityStatic), Instant/Sorcery (the card's type);
// Triggered/Activated and anything else never name a play option. A `!` on
// the head negates the whole alternative. Each "+" property is
// validSAPropertyMatches'.
func validSAMatches(g *Game, a *Ability, caster PlayerID, card CardID, opt playOption, spec string) (bool, error) {
	c := g.Card(card)
	for _, alt := range strings.Split(spec, ",") {
		head, rest, hasRest := strings.Cut(alt, ".")
		negated := strings.HasPrefix(head, "!")
		head = strings.TrimPrefix(head, "!")
		var kind bool
		switch {
		case head == "Spell":
			kind = !opt.land
		case head == "SpellAbility":
			kind = true
		case head == "Ability", head == "Static", strings.Contains(head, "LandAbility"):
			kind = opt.land
		case head == "Instant":
			kind = c.Type().Has(cardtype.Instant)
		case head == "Sorcery":
			kind = c.Type().Has(cardtype.Sorcery)
		}
		matched := kind
		if kind && hasRest {
			for _, prop := range strings.Split(rest, "+") {
				ok, err := validSAPropertyMatches(g, a, caster, c, prop)
				if err != nil {
					return false, err
				}
				if !ok {
					matched = false
					break
				}
			}
		}
		if matched != negated {
			return true, nil
		}
	}
	return false, nil
}

// validSAPropertyMatches is one SpellAbilityProperty.hasProperty check
// (SpellAbilityProperty.java:245-320): YouCtrl/OppCtrl against the caster,
// who is also the valid string's "You" (so YouCtrl always holds);
// cmc<op><n> against the spell's mana value (the card's, since it is not on
// the stack), the operand resolved through the Play host's SVars (X, Y, ...
// -- resolveNamedAmount); anything else the card's own property (Matches),
// rejected when Matches has no case for it.
func validSAPropertyMatches(g *Game, a *Ability, caster PlayerID, c *Card, prop string) (bool, error) {
	if rest, ok := strings.CutPrefix(prop, "!"); ok {
		matched, err := validSAPropertyMatches(g, a, caster, c, rest)
		return !matched, err
	}
	switch {
	case prop == "YouCtrl":
		return true, nil
	case prop == "OppCtrl":
		return false, nil
	case strings.HasPrefix(prop, "cmc") && len(prop) > 5:
		operand := prop[5:]
		n, ok := resolveNamedAmount(g, a.Amounts, g.Card(a.Source), operand)
		if !ok {
			return false, fmt.Errorf("engine: Play: ValidSA$ %q: operand not resolvable", prop)
		}
		return compareOp(c.CMC(), prop[3:5], n), nil
	}
	if name, gap := playSpecGap("Card." + prop); gap {
		return false, fmt.Errorf("engine: Play: ValidSA$ property %q not resolvable yet", name)
	}
	return Matches(g, c, valid.Parse("Card."+prop), caster, a.Source), nil
}
