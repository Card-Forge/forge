// Effect: the effect card, a Command-zone object carrying triggers,
// continuous effects and replacement effects for a Duration$.

package engine

//enginelint:allow id zone card game player ability defined condition control parts amount effecthelpers controlcommands ateot untilcommand

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
)

// effectUnresolvedParams are EffectEffect params this port does not model,
// each its own further mechanic: granted activated abilities (Abilities$),
// remembering a spell, a last-known copy or keywords rather than objects
// (RememberSpell$/RememberLKI$/RememberKeywords$ and its SharedKeywordsZone$/
// SharedRestrictions$), the counter-driven forget/exile triggers
// (ForgetCounter$/ExileOnCounter$/NoteCounterDefined$), the loses-the-game
// exile trigger (ExileOnLost$), a
// standalone forget-on-cast (ForgetOnCast$ without RememberObjects$), the
// Boon one-shot (Boon$, TriggerHandler removes the effect after its first
// trigger), imprinting the effect
// on its host (ImprintOnHost$), the Adventure name, and the
// SpellAbilityCondition shapes subAbilityConditionMet does not cover
// (ConditionZone$, and a Condition$ value rejectUnreadCondition refuses).
var effectUnresolvedParams = [...]string{
	"Abilities", "RememberSpell", "RememberLKI", "RememberKeywords", "SharedKeywordsZone", "SharedRestrictions",
	"ForgetCounter", "ExileOnCounter", "NoteCounterDefined", "ExileOnLost",
	"Boon", "ImprintOnHost", "Adventure",
	"ConditionZone",
}

// effectEffect is EffectEffect.java: for each EffectOwner$ player (default
// the activator) it creates an effect card in that player's Command zone
// carrying the StaticAbilities$, Triggers$ and ReplacementEffects$ SVars,
// active there alone, remembering RememberObjects$ and imprinting
// ImprintCards$, until its Duration$ ends or its ExileOnMoved$/
// ForgetOnMoved$/ForgetOnPhasedIn$ watch exiles it.
//
// The traits are the compiled abilities the carddb compiler resolved from
// those three params (compile.effectTraitKeys) -- nothing is parsed here
// (PORT-2). The effect card's own definition is built from them at
// resolution; its SVars are the host's (createEffect's
// eff.setSVars(sa.getSVars())), so a trait's amounts resolve as they would
// on the host.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/EffectEffect.java's
// resolve, and SpellAbilityEffect.java's createEffect, checkValidDuration,
// addUntilCommand, addForgetOnMovedTrigger, addExileOnMovedTrigger,
// addForgetOnCastTrigger and addForgetOnPhasedInTrigger (phasing.go's
// effectCardsSeePhaseIn).
type effectEffect struct{}

func (effectEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	if err := rejectParams(a, "Effect", effectUnresolvedParams[:]...); err != nil {
		return err
	}
	if err := rejectUnreadCondition(a, "Effect"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	life, ok, err := effectDurationOf(g, a)
	if err != nil || !ok {
		return err
	}
	if err := effectMoveWatch(a, &life); err != nil {
		return err
	}

	var remember []EntityID
	hasRemember := false
	if spec, ok := a.Params.Param("RememberObjects"); ok {
		hasRemember = true
		for _, part := range strings.Split(spec, " & ") {
			objs, err := definedEntities(g, a.Controller, source, strings.TrimSpace(part), a.refs())
			if err != nil {
				return fmt.Errorf("engine: Effect: RememberObjects$: %w", err)
			}
			remember = append(remember, objs...)
		}
		// Java creates no effect when nothing is left to watch
		// (EffectEffect.java:94).
		if len(remember) == 0 && (hasParam(a, "ForgetOnMoved") || hasParam(a, "ExileOnMoved") || hasParam(a, "ForgetOnPhasedIn")) {
			return nil
		}
		// Java arms the phase-in watch only on a remembered list
		// (EffectEffect.java:247-249).
		life.forgetOnPhasedIn = hasParam(a, "ForgetOnPhasedIn")
	}
	if !hasRemember && (hasParam(a, "ForgetOnMoved") || hasParam(a, "ExileOnMoved")) {
		// Java only arms either watch on a remembered list.
		life.exileOnMoved, life.forgetOnMoved, life.forgetOnCast = 0, 0, false
	}

	var imprint []CardID
	if spec, ok := a.Params.Param("ImprintCards"); ok {
		if imprint, err = definedCards(source, spec, a.refs()); err != nil {
			return fmt.Errorf("engine: Effect: ImprintCards$: %w", err)
		}
	}
	chosenNumber, hasChosenNumber := 0, false
	if hasParam(a, "SetChosenNumber") {
		if chosenNumber, err = optionalAmount(g, a, "Effect", "SetChosenNumber", 0); err != nil {
			return err
		}
		hasChosenNumber = true
	}

	name, ok := a.Params.Param("Name")
	if !ok {
		name = effectHostName(source) + "'s Effect"
	}
	owners := []PlayerID{a.Controller}
	if spec, ok := a.Params.Param("EffectOwner"); ok {
		if owners, err = definedPlayers(g, a.Controller, a.Source, spec, a.refs()); err != nil {
			return fmt.Errorf("engine: Effect: EffectOwner$: %w", err)
		}
	}
	if hasParam(a, "Unique") {
		kept := owners[:0:0]
		for _, pid := range owners {
			if !hasEffectNamed(g, pid, name) {
				kept = append(kept, pid)
			}
		}
		owners = kept
	}

	def := effectCardDef(a, name)
	for _, pid := range owners {
		id := g.NewCard(def, pid, Command)
		eff := g.Card(id)
		eff.IsEffect = true
		life.player = pid
		if life.duration == effectUntilEndOfYourNextTurn {
			// addUntilCommand's registerUntilEnd: made during the owner's own
			// turn, the effect outlives that turn's cleanup.
			life.armed = g.ActivePlayer() == pid
		}
		eff.effectLife = life
		// EffectEffect.java:312-314: AtEOT$ acts on the host card, once per
		// effect made.
		if err := registerAtEOT(g, a, []CardID{a.Source}); err != nil {
			return err
		}
		if life.until != "" {
			// EffectEffect.java:318-320, keyed to the effect's owner.
			if err := g.addUntilCommandFor(a, life.until, cardCommand{Kind: commandEndEffect, Target: id}, pid); err != nil {
				return err
			}
		}
		for _, e := range remember {
			eff.Memory.Remember(e)
		}
		for _, c := range imprint {
			eff.Memory.Imprint(c)
		}
		// Re-read: NewCard may have grown the arena under source.
		eff.Memory.copyChoicesFrom(&g.Card(a.Source).Memory)
		if hasChosenNumber {
			eff.Memory.SetChosenNumber(chosenNumber)
		}
	}
	return nil
}

// effectCardDef is the effect card's definition: name, the host's SVars,
// and the compiled traits a names under StaticAbilities$, Triggers$ and
// ReplacementEffects$, in script order. It carries no type line, cost or
// power: an effect card is none of card, permanent or spell.
func effectCardDef(a *Ability, name string) *compile.Card {
	def := &compile.Card{Name: name}
	face := &def.Faces[0]
	face.Name = name
	face.Amounts = a.Amounts
	for _, sub := range a.Params.Subs {
		switch strings.ToLower(sub.Key) {
		case "staticabilities":
			face.Statics = append(face.Statics, sub.Ability)
		case "triggers":
			face.Triggers = append(face.Triggers, sub.Ability)
		case "replacementeffects":
			face.Replacements = append(face.Replacements, sub.Ability)
		}
	}
	return def
}

// effectHostName is the host's name for the default "<host>'s Effect"
// (Card.toString), empty for a definition-less test card.
func effectHostName(c *Card) string {
	if c.Def == nil {
		return ""
	}
	return c.Def.Name
}

// hasEffectNamed is Player.isCardInCommand(name): Unique$'s "one per
// player" check.
func hasEffectNamed(g *Game, pid PlayerID, name string) bool {
	for _, id := range g.Zone(Command, pid).Cards() {
		if c := g.Card(id); c.Def != nil && c.Def.Name == name {
			return true
		}
	}
	return false
}

// effectDuration is which of addUntilCommand's branches ends an effect card.
type effectDuration uint8

const (
	// effectUntilEndOfTurn is addUntilCommand's default branch: gone at the
	// next cleanup step (EndOfTurn.executeUntil at CLEANUP).
	effectUntilEndOfTurn effectDuration = iota
	// effectPermanent never ends by duration (Duration$ Permanent).
	effectPermanent
	// effectUntilYourNextTurn ends as its controller's next turn begins
	// (Cleanup.executeUntil(playerTurn) in handleNextTurn).
	effectUntilYourNextTurn
	// effectUntilEndOfYourNextTurn ends at the cleanup step of its
	// controller's next turn -- not the current one, when created during it
	// (registerUntilEnd versus addUntilEnd).
	effectUntilEndOfYourNextTurn
	// effectUntilEndOfCombat ends when combat ends (EndOfCombat.executeUntil).
	effectUntilEndOfCombat
	// effectUntilHostLeavesPlay ends when the host leaves the battlefield
	// (Card.addLeavesPlayCommand).
	effectUntilHostLeavesPlay
	// effectUntilHostLeavesPlayOrEOT ends at whichever comes first: the host
	// leaving the battlefield or the next cleanup step.
	effectUntilHostLeavesPlayOrEOT
	// effectUntilFacedown ends when the host is turned face down
	// (Card.addFacedownCommand, run by Card.turnFaceDown). Only a Clone copy
	// effect takes it (Vesuvan Shapeshifter).
	effectUntilFacedown
)

// zoneMask is a set of zones, one bit per ZoneType.
type zoneMask uint32

func (m zoneMask) has(z ZoneType) bool { return m&(1<<z) != 0 }

// effectLifetime is what ends an effect card: its Duration$ (for player's
// turns, or host's leaving play) and the ExileOnMoved$/ForgetOnMoved$
// watch on the cards it remembers, and forgetOnPhasedIn its
// ForgetOnPhasedIn$ watch. armed marks an
// effectUntilEndOfYourNextTurn created during player's own turn, which
// survives that turn's cleanup.
type effectLifetime struct {
	duration effectDuration
	player   PlayerID
	armed    bool
	host     CardID
	// until is a Duration$ addUntilCommand puts the ending command on
	// (AsLongAsControl, UntilUntaps, UntilYourNextEndStep, ...): the effect is
	// permanent until that command runs, not a lifetime this struct's
	// duration tracks.
	until         string
	exileOnMoved  zoneMask
	forgetOnMoved zoneMask
	forgetOnCast  bool
	// forgetOnPhasedIn forgets a remembered card as it phases in
	// (effectCardsSeePhaseIn, phasing.go).
	forgetOnPhasedIn bool
}

// effectDurationOf reads Duration$ and applies checkValidDuration: a
// duration whose host or target is not in the state it needs creates no effect
// at all (ok false). The durations this struct tracks itself are the first
// cases; every other value addUntilCommand names rides a command (until), and
// one it does not name ends at cleanup, its final else.
func effectDurationOf(g *Game, a *Ability) (life effectLifetime, ok bool, err error) {
	d, has := a.Params.Param("Duration")
	life.host = a.Source
	switch {
	case !has:
		life.duration = effectUntilEndOfTurn
	case d == "Permanent":
		life.duration = effectPermanent
	case d == "UntilYourNextTurn":
		life.duration = effectUntilYourNextTurn
	case d == "UntilTheEndOfYourNextTurn":
		life.duration = effectUntilEndOfYourNextTurn
	case d == "UntilEndOfCombat":
		life.duration = effectUntilEndOfCombat
	case d == "UntilHostLeavesPlay":
		life.duration = effectUntilHostLeavesPlay
	case d == "UntilHostLeavesPlayOrEOT":
		life.duration = effectUntilHostLeavesPlayOrEOT
	default:
		if err := durationRefused("Effect", d); err != nil {
			return life, false, err
		}
		if endsAtCleanup(d) {
			life.duration = effectUntilEndOfTurn
		} else {
			life.duration, life.until = effectPermanent, d
		}
	}
	if has && !g.checkValidDuration(a, d) {
		return life, false, nil
	}
	return life, true, nil
}

// effectMoveWatch reads ExileOnMoved$ and ForgetOnMoved$ (each a comma list
// of zones) onto life. ForgetOnMoved$ also forgets a remembered card that is
// cast (addForgetOnCastTrigger), unless it names Stack or ForgetOnCast$
// False.
func effectMoveWatch(a *Ability, life *effectLifetime) error {
	var err error
	if v, ok := a.Params.Param("ExileOnMoved"); ok {
		if life.exileOnMoved, err = parseZoneMask(v); err != nil {
			return err
		}
	}
	if v, ok := a.Params.Param("ForgetOnMoved"); ok {
		if life.forgetOnMoved, err = parseZoneMask(v); err != nil {
			return err
		}
		onCast, _ := a.Params.Param("ForgetOnCast")
		life.forgetOnCast = v != "Stack" && !strings.EqualFold(onCast, "False")
	}
	return nil
}

// parseZoneMask reads a comma list of zone names; an unknown name is an
// error (forget_on_moved's one corpus "True" among them).
func parseZoneMask(v string) (zoneMask, error) {
	var m zoneMask
	for _, name := range strings.Split(v, ",") {
		z, ok := ZoneByName(strings.TrimSpace(name))
		if !ok {
			return 0, fmt.Errorf("engine: Effect: zone %q not resolvable", name)
		}
		m |= 1 << z
	}
	return m, nil
}

// effectCards is every effect card currently in a Command zone, copied: the
// callers below exile some of them while walking.
func (g *Game) effectCards() []CardID {
	var out []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Command, pid).Cards() {
			if g.Card(id).IsEffect {
				out = append(out, id)
			}
		}
	}
	return out
}

// exileEffect is GameAction.exileEffect: an effect card moving to exile
// just leaves the Command zone -- changeZone removes it from its zone and
// adds it nowhere, with no zone-change event or trigger. It is parked in
// its owner's None zone, since a CardID is never freed (ADR-0009), the way
// removeTokensOffBattlefield parks a token.
func (g *Game) exileEffect(id CardID) {
	c := g.Card(id)
	g.Zone(c.Zone, c.ZoneOwner).cards.Remove(id)
	g.put(id, None, c.Owner)
}

// effectCardsSeeMove runs every effect card's watch on one zone change:
// UntilHostLeavesPlay* ends when the host leaves the battlefield,
// ExileOnMoved$ exiles the effect when a remembered card leaves a named
// zone, and ForgetOnMoved$ forgets it -- exiling the effect once it
// remembers no card (getForgetSpellAbility's ConditionCompare$ EQ0).
//
// The three trigger shapes addForgetOnMovedTrigger builds are folded into
// one check: leaving a named zone for anywhere but the stack or exile, being
// exiled from anywhere (the Mode$ Exiled trigger), and, with forgetOnCast,
// being cast (moved to the stack -- casting is this port's only move there).
// The Exiled trigger's ValidCause$ SpellAbility.!EffectSourceAbility
// exception, an exile by the very ability that made the effect, is not
// modeled: the corpus's ForgetOnMoved$ chains exile before they create the
// effect, so the exception has no card to apply to.
func (g *Game) effectCardsSeeMove(moved CardID, from, to ZoneType) {
	if from == to || g.Card(moved).IsEffect {
		return
	}
	if from == Battlefield {
		// GameAction.java:533/960: the card's leaves-play commands run as it
		// goes, before anything else reacts to the move. No controller is at
		// hand here, so a lose-control command ends its control change and
		// leaves the zone correction to the next state-based pass.
		g.runLeavesPlayCommands(nil, moved)
	}
	for _, id := range g.effectCards() {
		e := g.Card(id)
		if e.Zone != Command {
			continue
		}
		life := e.effectLife
		if from == Battlefield && life.host == moved &&
			(life.duration == effectUntilHostLeavesPlay || life.duration == effectUntilHostLeavesPlayOrEOT) {
			g.exileEffect(id)
			continue
		}
		if !effectRemembers(e, moved) {
			continue
		}
		if life.exileOnMoved.has(from) {
			g.exileEffect(id)
			continue
		}
		forget := life.forgetOnMoved.has(from) && to != Stack && to != Exile ||
			life.forgetOnMoved != 0 && to == Exile ||
			life.forgetOnCast && to == Stack
		if !forget {
			continue
		}
		e.Memory.Forget(CardEntity(moved))
		if !effectRemembersAnyCard(e) {
			g.exileEffect(id)
		}
	}
}

// effectRemembers reports whether e remembers card id.
func effectRemembers(e *Card, id CardID) bool {
	for _, r := range e.Memory.Remembered() {
		if c, ok := r.AsCard(); ok && c == id {
			return true
		}
	}
	return false
}

// effectRemembersAnyCard reports whether e remembers at least one card.
func effectRemembersAnyCard(e *Card) bool {
	for _, r := range e.Memory.Remembered() {
		if _, ok := r.AsCard(); ok {
			return true
		}
	}
	return false
}

// endEffectsAtCleanup ends every effect whose duration runs out at this
// cleanup step: end-of-turn ones, host-or-end-of-turn ones, and the active
// player's until-the-end-of-your-next-turn ones not created this turn.
func (g *Game) endEffectsAtCleanup() {
	for _, id := range g.effectCards() {
		e := g.Card(id)
		switch e.effectLife.duration {
		case effectUntilEndOfTurn, effectUntilHostLeavesPlayOrEOT:
			g.exileEffect(id)
		case effectUntilEndOfYourNextTurn:
			if e.effectLife.player != g.activePlayer {
				continue
			}
			if e.effectLife.armed {
				e.effectLife.armed = false
				continue
			}
			g.exileEffect(id)
		}
	}
}

// endEffectsAtTurnStart ends every until-your-next-turn effect of active,
// whose turn is beginning.
func (g *Game) endEffectsAtTurnStart(active PlayerID) {
	for _, id := range g.effectCards() {
		e := g.Card(id)
		if e.effectLife.duration == effectUntilYourNextTurn && e.effectLife.player == active {
			g.exileEffect(id)
		}
	}
}

// endEffectsAtEndOfCombat ends every until-end-of-combat effect.
func (g *Game) endEffectsAtEndOfCombat() {
	for _, id := range g.effectCards() {
		if g.Card(id).effectLife.duration == effectUntilEndOfCombat {
			g.exileEffect(id)
		}
	}
}
