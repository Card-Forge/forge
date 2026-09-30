package engine

//enginelint:allow card game ability defined condition control parts effecthelpers id zone valid amount copypermanenteffect effecteffect player room

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// copyEffect is one Layer 1 copy effect on one permanent (CR 613.2a): Java's
// CardCloneStates, stored under its timestamp in Card.clonedStates.
//
// def is the whole set of copiable values the permanent has while the effect
// is the latest one on it, built once as the effect begins
// (CardFactory.getCloneStates) and never written again, so clones of the game
// and last-known-information snapshots share it the way they share any Def.
//
// A copy effect with no Duration$ is permanent: it ends only when the
// permanent leaves the battlefield. One with a Duration$ ends by life, and
// remembered/imprinted are the permanent's Memory as it began -- what
// CloneEffect's unclone command restores.
type copyEffect struct {
	Timestamp  uint64
	def        *compile.Card
	permanent  bool
	life       effectLifetime
	remembered []EntityID
	imprinted  []CardID
}

// UncopiedDef is c's definition under every copy effect and any Layer 3
// text change (preTextDef, card.go) -- Def itself when none applies.
// Java's GameState writes a copied permanent under its own paper card's
// name, which is what the fixture dumper reads this for.
func (c *Card) UncopiedDef() *compile.Card {
	if len(c.copies) == 0 {
		return c.preTextDef()
	}
	return c.uncopiedDef
}

// IsCopy reports whether a copy effect applies to c (Card.isCloned).
func (c *Card) IsCopy() bool { return len(c.copies) > 0 }

// copiableValues is what copying c copies (CR 707.2): its current definition
// -- a copy effect's own, when one applies (CR 707.3), and the face-down
// characteristics of a face-down permanent (CR 708.5) -- plus the base power
// and toughness a token's creating effect gave it, which a copy effect on c
// hides (BasePower).
//
// A Room permanent's copiable values are its whole printed card, both
// halves (CardFactory.java:535-542 copies LeftSplit, RightSplit and the
// empty room); which doors are unlocked is not copied.
func (c *Card) copiableValues() copyOriginal {
	o := copyOriginal{def: c.preTextDef()}
	if c.IsRoomPermanent() && len(c.copies) == 0 && !c.IsFaceDown() {
		o.def = c.roomDef
	}
	if len(c.copies) == 0 {
		o.power, o.hasPower = c.basePower, c.hasBasePower
		o.toughness, o.hasToughness = c.baseToughness, c.hasBaseToughness
	}
	return o
}

// addCopy is Card.addCloneState: e becomes the latest copy effect on c and
// its definition c's. The slice is rebuilt rather than appended in place: a
// last-known-information snapshot shares the old backing array.
func (c *Card) addCopy(e copyEffect) {
	c.clearTextChange()
	if len(c.copies) == 0 {
		c.uncopiedDef = c.Def
	}
	c.copies = append(append([]copyEffect(nil), c.copies...), e)
	c.Def = e.def
}

// removeCopy is Card.removeCloneState(timestamp): the copy effect under ts
// ends and c takes the latest remaining one's definition, or its own.
func (c *Card) removeCopy(ts uint64) bool {
	for i, e := range c.copies {
		if e.Timestamp != ts {
			continue
		}
		rest := append(append([]copyEffect(nil), c.copies[:i]...), c.copies[i+1:]...)
		c.setCopies(rest)
		return true
	}
	return false
}

func (c *Card) setCopies(rest []copyEffect) {
	if len(rest) == 0 {
		c.Def, c.copies, c.uncopiedDef = c.uncopiedDef, nil, nil
		return
	}
	c.copies = rest
	c.Def = rest[len(rest)-1].def
}

// endCopiesOnLeave ends every copy effect id leaving the battlefield takes
// with it: its own (Card.removeChangedState's removeCloneStates -- Java's
// card becomes a new object), and every UntilHostLeavesPlay one it hosts
// (Card.addLeavesPlayCommand). Game.Move calls it before turning the card
// face up or front face up, so those restore the card's own faces, not the
// copied one.
func (g *Game) endCopiesOnLeave(id CardID) {
	if c := g.Card(id); len(c.copies) > 0 {
		c.setCopies(nil)
	}
	g.endCopiesWhere(func(e copyEffect) bool {
		return e.life.duration == effectUntilHostLeavesPlay && e.life.host == id
	})
}

// endCopiesAtCleanup ends every until-end-of-turn copy effect
// (EndOfTurn.addUntil, run at cleanup).
func (g *Game) endCopiesAtCleanup() {
	g.endCopiesWhere(func(e copyEffect) bool { return e.life.duration == effectUntilEndOfTurn })
}

// endCopiesAtTurnStart ends every until-your-next-turn copy effect of
// active, whose turn is beginning (Cleanup.addUntil(player)).
func (g *Game) endCopiesAtTurnStart(active PlayerID) {
	g.endCopiesWhere(func(e copyEffect) bool {
		return e.life.duration == effectUntilYourNextTurn && e.life.player == active
	})
}

// endCopiesWhere ends each non-permanent copy effect on the battlefield that
// ends reports true for, in battlefield order.
func (g *Game) endCopiesWhere(ends func(copyEffect) bool) {
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			// uncopy replaces the card's copies slice rather than editing
			// it, so this range keeps walking the one it started on.
			for _, e := range g.Card(id).copies {
				if !e.permanent && ends(e) {
					g.uncopy(id, e)
				}
			}
		}
	}
}

// uncopy is CloneEffect's unclone command: e ends on id and, if it was
// still there, id's Memory returns to what it was as e began -- imprinted
// cards and remembered cards whose owner is still in the game, and every
// remembered player, players first (IterableUtil.filter by Player.class,
// then by Card.class).
func (g *Game) uncopy(id CardID, e copyEffect) {
	c := g.Card(id)
	if !c.removeCopy(e.Timestamp) {
		return
	}
	c.Memory.ClearImprinted()
	c.Memory.ClearRemembered()
	for _, im := range e.imprinted {
		if !g.Player(g.Card(im).Owner).Lost {
			c.Memory.Imprint(im)
		}
	}
	for _, r := range e.remembered {
		if _, ok := r.AsPlayer(); ok {
			c.Memory.Remember(r)
		}
	}
	for _, r := range e.remembered {
		if cid, ok := r.AsCard(); ok && !g.Player(g.Card(cid).Owner).Lost {
			c.Memory.Remember(r)
		}
	}
}

// cloneEffect is CloneEffect.java: a permanent becomes a copy of another
// card (CR 707.2) -- a Layer 1 copy effect (CR 613.2a), under every other
// continuous effect on it.
//
// The card to copy is the one the activator picks among Choices$ (in
// ChoiceZone$, the battlefield by default; ChoiceOptional$ lets them pick
// none), else the first Defined$ card, else the first targeted card, else a
// card named by the host's named card (CopyFromChosenName$). The card
// becoming a copy is each CloneTarget$ card, else the targeted card when
// Choices$ picked the one to copy, else the host. Optional$ asks the host's
// controller first; ExcludeChosen$ spares the copied card itself;
// CloneZone$ skips targets elsewhere. With a Duration$ the copy ends by it;
// without, it is permanent.
//
// The copied values are the copied card's own copiable values with the
// "except" params applied (CardFactory.getCloneStates): NewName$/KeepName$,
// AddColors$/SetColor$, NonLegendary$, AddTypes$, AddKeywords$ (IfNew
// filtering), SetPower$/SetToughness$, AddSVars$ (numeric SVars; an
// ability SVar is already compiled into the ability that names it, PORT-2),
// AddTriggers$/AddAbilities$/AddStaticAbilities$ (the traits their SVars
// hold, compiled at load into the line's own Subs) and GainThisAbility$
// (the copy keeps the trigger or ability this line resolves under).
// IntoPlayTapped$ taps the copy; RememberCloneOrigin$ remembers the copied
// card.
//
// The same effect runs as a Copy-layer replacement's ReplaceWith$ ("enters
// as a copy", entersascopy.go), where Choices$ never offers the entering
// card and Defined$/CloneTarget$ ReplacedCard name it.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/
// CloneEffect.java's resolve and forge-game/src/main/java/forge/game/card/
// CardFactory.java's getCloneStates.
type cloneEffect struct{}

// cloneUnresolvedParams are the CloneEffect/getCloneStates params this port
// does not resolve: gaining another card's text (GainTextAbilities$,
// GainTextOf$), the pump keywords' own until-command, Embalm's condition,
// mana cost and card/creature type rewriting, keyword removal, loyalty, and
// RemoveCreatureTypes$ -- which getCloneStates never reads (PORT-8,
// effects-clone.md).
var cloneUnresolvedParams = [...]string{
	"GainTextAbilities", "GainTextOf",
	"PumpKeywords", "PumpDuration", "Embalm", "RemoveCost", "SetManaCost", "SetColorByManaCost",
	"RemoveCardTypes", "RemoveSubTypes", "RemoveCreatureTypes", "SetCreatureTypes", "RemoveKeywords",
	"SetLoyalty", "Condition",
}

func (cloneEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "Clone", cloneUnresolvedParams[:]...); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	template, ok, err := cloneDuration(a, source)
	if err != nil || !ok {
		return err
	}
	origin, found, err := cloneOriginOf(g, a, controller, source)
	if err != nil || !found {
		return err
	}
	if isRoomDef(origin.vals.def) {
		// CloneEffect.java:146 gives a permanent that becomes a copy of a
		// Room its own locked/unlocked doors (updateRooms); this port's
		// copy effects have no door state of their own yet.
		return fmt.Errorf("engine: Clone: becoming a copy of a Room not resolvable yet")
	}
	if hasParam(a, "Optional") && !controller.ConfirmEffect(g, source.Controller(), a.Source) {
		return nil
	}
	targets, err := cloneTargets(g, a, source)
	if err != nil {
		return err
	}
	if hasParam(a, "ExcludeChosen") && origin.card != NoCard {
		targets = withoutCards(targets, []CardID{origin.card})
	}
	if hasParam(a, "RememberCloneOrigin") && origin.card == NoCard {
		return fmt.Errorf("engine: Clone: RememberCloneOrigin$ of a card outside the game not resolvable yet")
	}
	cloneZone, hasCloneZone := None, false
	if raw, ok := a.Params.Param("CloneZone"); ok {
		if cloneZone, hasCloneZone = ZoneByName(raw); !hasCloneZone {
			return fmt.Errorf("engine: Clone: CloneZone$ %q not resolvable", raw)
		}
	}

	// Every target is checked and every definition built before any card
	// changes: a shape this port cannot resolve fails the whole line, never
	// half of it (GO-7).
	type pending struct {
		id  CardID
		def *compile.Card
	}
	var todo []pending
	for _, id := range targets {
		t := g.Card(id)
		if hasCloneZone && t.Zone != cloneZone {
			continue
		}
		if t.Zone != Battlefield {
			return fmt.Errorf("engine: Clone: a copy outside the battlefield (%v) not resolvable yet", t.Zone)
		}
		if t.IsFaceDown() {
			return fmt.Errorf("engine: Clone: a copy on a face-down permanent not resolvable yet")
		}
		def, err := cloneDef(g, a, origin, t)
		if err != nil {
			return err
		}
		todo = append(todo, pending{id, def})
	}

	g.timestamp++
	ts := g.timestamp
	for _, p := range todo {
		t := g.Card(p.id)
		// Java adds the copy, then clears the host's own memory, then
		// snapshots what the unclone command restores; only the memory
		// steps depend on each other, so the snapshot is taken first here
		// and stored on the effect.
		if p.id == a.Source && !hasParam(a, "ImprintRememberedNoCleanup") {
			t.Memory.ClearImprinted()
			t.Memory.ClearRemembered()
		}
		e := template
		e.Timestamp, e.def = ts, p.def
		if !e.permanent {
			e.imprinted = append([]CardID(nil), t.Memory.Imprinted()...)
			e.remembered = append([]EntityID(nil), t.Memory.Remembered()...)
		}
		t.addCopy(e)
		if hasParam(a, "IntoPlayTapped") {
			t.Tapped = true
		}
		t.Memory.ClearRemembered()
		t.Memory.ClearImprinted()
		if hasParam(a, "RememberCloneOrigin") {
			t.Memory.Remember(CardEntity(origin.card))
		}
	}
	return nil
}

// cloneDuration is checkValidDuration plus the copy effect's lifetime: no
// Duration$ is permanent; the corpus's other Clone durations
// (UntilUnattached, UntilFacedown, UntilTargetedUntaps, UntilNextEndStep)
// need an ending this port does not track. ok is false when a host-bound
// duration's host is neither on the battlefield nor on the stack, which
// resolves to nothing at all.
func cloneDuration(a *Ability, source *Card) (copyEffect, bool, error) {
	var e copyEffect
	d, has := a.Params.Param("Duration")
	if !has {
		e.permanent = true
		return e, true, nil
	}
	e.life.player, e.life.host = a.Controller, a.Source
	switch d {
	case "UntilEndOfTurn", "EndOfTurn":
		e.life.duration = effectUntilEndOfTurn
	case "UntilYourNextTurn":
		e.life.duration = effectUntilYourNextTurn
	case "UntilHostLeavesPlay":
		e.life.duration = effectUntilHostLeavesPlay
		if source.Zone != Battlefield && source.Zone != Stack {
			return e, false, nil
		}
	default:
		return e, false, fmt.Errorf("engine: Clone: Duration$ %q not resolvable yet", d)
	}
	return e, true, nil
}

// cloneOrigin is the card being copied: its copiable values, its own
// uncopied definition (getOriginalState, for SetPower$'s printed
// power/toughness test), and its CardID -- NoCard for a card built from a
// name, which is in no zone.
type cloneOrigin struct {
	card    CardID
	vals    copyOriginal
	printed *compile.Card
}

func cloneOriginOf(g *Game, a *Ability, controller PlayerController, source *Card) (cloneOrigin, bool, error) {
	var id CardID
	switch {
	case hasParam(a, "Choices"):
		chosen, ok, err := cloneChoice(g, a, controller)
		if err != nil || !ok {
			return cloneOrigin{}, false, err
		}
		id = chosen
	case hasParam(a, "Defined"):
		raw, _ := a.Params.Param("Defined")
		cards, err := cloneDefinedCards(g, a, source, raw)
		if err != nil || len(cards) == 0 {
			return cloneOrigin{}, false, err
		}
		id = cards[0]
	case hasParam(a, "ValidTgts"):
		ok := false
		for _, t := range a.Targets {
			if id, ok = t.AsCard(); ok {
				break
			}
		}
		if !ok {
			return cloneOrigin{}, false, nil
		}
	case hasParam(a, "CopyFromChosenName"):
		named := source.Memory.NamedCards()
		if len(named) == 0 {
			return cloneOrigin{}, false, nil
		}
		name := named[len(named)-1]
		if g.db == nil {
			return cloneOrigin{}, false, fmt.Errorf("engine: Clone: CopyFromChosenName$ needs a card database")
		}
		def, ok := g.db.Card(name)
		if !ok {
			return cloneOrigin{}, false, fmt.Errorf("engine: Clone: no card named %q to copy", name)
		}
		return cloneOrigin{card: NoCard, vals: copyOriginal{def: def}, printed: def}, true, nil
	default:
		return cloneOrigin{}, false, nil
	}
	c := g.Card(id)
	vals := c.copiableValues()
	if vals.def == nil {
		return cloneOrigin{}, false, fmt.Errorf("engine: Clone: card %d has no definition to copy", id)
	}
	printed := c.UncopiedDef()
	if c.IsFaceDown() {
		printed = c.faceUpDef
	}
	return cloneOrigin{card: id, vals: vals, printed: printed}, true, nil
}

// cloneChoice is the Choices$ branch: the activator picks one card among
// every player's cards in ChoiceZone$ (the battlefield by default) matching
// Choices$, or -- with ChoiceOptional$ -- none. An empty pool picks nothing.
func cloneChoice(g *Game, a *Ability, controller PlayerController) (CardID, bool, error) {
	zone := Battlefield
	if raw, ok := a.Params.Param("ChoiceZone"); ok {
		z, ok := ZoneByName(raw)
		if !ok {
			return NoCard, false, fmt.Errorf("engine: Clone: ChoiceZone$ %q not resolvable", raw)
		}
		zone = z
	}
	raw, _ := a.Params.Param("Choices")
	spec, err := cloneSpec(raw)
	if err != nil {
		return NoCard, false, err
	}
	// As a replacement of a card's entry ("enters as a copy"), Java keeps
	// only the last battlefield state's cards (CloneEffect.java's
	// isReplacementAbility branch): the card entering is not yet there to be
	// copied. This port has already moved it, so it is left out by hand.
	// Java's last graveyard state still holds a card entering from the
	// graveyard, which this port has moved out of it. No corpus line can
	// tell: each choosing there says Other, except Lazotep Convert's, a
	// battle's back face, which enters cast from exile.
	entering := a.replacedCard()
	var choices []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(zone, pid).Cards() {
			if zone == Battlefield && id == entering {
				continue
			}
			if Matches(g, g.Card(id), spec, a.Controller, a.Source) {
				choices = append(choices, id)
			}
		}
	}
	if len(choices) == 0 {
		return NoCard, false, nil
	}
	lo := 1
	if hasParam(a, "ChoiceOptional") {
		lo = 0
	}
	chosen := controller.ChooseCardsForEffect(g, a.Controller, a.Source, choices, lo, 1)
	if err := checkChoice(chosen, choices, lo, 1); err != nil {
		return NoCard, false, fmt.Errorf("engine: Clone: %w", err)
	}
	if len(chosen) == 0 {
		return NoCard, false, nil
	}
	return chosen[0], true, nil
}

// cloneTargets is who becomes a copy: CloneTarget$'s cards, the targeted
// card when Choices$ picked what to copy, else the host.
func cloneTargets(g *Game, a *Ability, source *Card) ([]CardID, error) {
	if raw, ok := a.Params.Param("CloneTarget"); ok {
		return cloneDefinedCards(g, a, source, raw)
	}
	if hasParam(a, "Choices") && hasParam(a, "ValidTgts") {
		for _, t := range a.Targets {
			if id, ok := t.AsCard(); ok {
				return []CardID{id}, nil
			}
		}
		return nil, nil
	}
	return []CardID{source.ID}, nil
}

// cloneDefinedCards is AbilityUtils.getDefinedCards for Clone's Defined$
// and CloneTarget$: "Valid <spec>" is every battlefield card matching spec,
// anything else definedCards' own vocabulary.
func cloneDefinedCards(g *Game, a *Ability, source *Card, raw string) ([]CardID, error) {
	rest, ok := strings.CutPrefix(raw, "Valid ")
	if !ok {
		cards, err := definedCards(source, raw, a.refs())
		if err != nil {
			return nil, fmt.Errorf("engine: Clone: %w", err)
		}
		return cards, nil
	}
	spec, err := cloneSpec(rest)
	if err != nil {
		return nil, err
	}
	var out []CardID
	for _, pid := range g.Players() {
		for _, id := range g.Zone(Battlefield, pid).Cards() {
			if Matches(g, g.Card(id), spec, a.Controller, a.Source) {
				out = append(out, id)
			}
		}
	}
	return out, nil
}

// cloneUnportedProperties are valid-string properties Clone's corpus names
// that Matches has no case for: it would read each as a type and match
// nothing, silently (GO-7).
var cloneUnportedProperties = [...]string{"token", "NotDefinedTargeted", "TriggeredCards"}

// cloneSpec parses a Choices$/Valid spec, rejecting the properties
// Matches cannot answer: cloneUnportedProperties, ThisTurnEntered*, and a
// comparison against anything but a literal number.
func cloneSpec(raw string) (valid.Spec, error) {
	spec := valid.Parse(raw)
	for _, alt := range spec.Alternatives {
		for _, p := range alt.Properties {
			unported := strings.HasPrefix(p.Name, "ThisTurnEntered")
			for _, name := range cloneUnportedProperties {
				unported = unported || p.Name == name
			}
			if p.Compare != nil {
				if _, err := strconv.Atoi(p.Compare.Operand); err != nil {
					unported = true
				}
			}
			if unported {
				return spec, fmt.Errorf("engine: Clone: valid property %q in %q not resolvable yet", p.Name, raw)
			}
		}
	}
	return spec, nil
}

// cloneDef is CardFactory.getCloneStates for one card becoming a copy (out)
// of origin: origin's copiable values, then the ability's "except" changes
// to every state copied.
//
// Which states are copied follows getCloneStates' own branches: a flip,
// split, adventure, omen or prepare card copies all its faces; anything
// else -- a transforming, modal or melded card included, since Clone is not
// CopyPermanent -- copies its current face alone, as a single-faced card.
// This port's current face is always Faces[0] (a transformed card's Def is
// its back face).
func cloneDef(g *Game, a *Ability, origin cloneOrigin, out *Card) (*compile.Card, error) {
	base := origin.vals.def
	def := &compile.Card{Filename: base.Filename, Name: base.Name}
	switch base.SplitType {
	case carddb.SplitFlip, carddb.SplitSplit, carddb.SplitAdventure, carddb.SplitOmen, carddb.SplitPrepare:
		def.SplitType = base.SplitType
		def.Faces = base.Faces
	default:
		def.Faces[0] = base.Faces[0]
	}
	if origin.vals.hasPower {
		def.Faces[0].Power = strconv.Itoa(origin.vals.power)
	}
	if origin.vals.hasToughness {
		def.Faces[0].Toughness = strconv.Itoa(origin.vals.toughness)
	}
	ch, err := readCloneChanges(g, a)
	if err != nil {
		return nil, err
	}
	if hasParam(a, "GainThisAbility") {
		if ch.gain, ch.gainKind, err = cloneRoot(g.Card(a.Source), a); err != nil {
			return nil, err
		}
	}
	for i := range def.Faces {
		f := &def.Faces[i]
		if i > 0 && f.Name == "" && f.Type.IsEmpty() {
			continue
		}
		ch.apply(f, faceAt(out.Def, i), faceAt(origin.printed, i))
	}
	// Java adds the gained traits to every copied state, but only the
	// current one is ever active. This port's trigger scans walk every face
	// of Def, so they go on the current face alone -- on an adventurer's
	// second face too a trigger would fire twice. Java's order is
	// AddTriggers$, AddAbilities$, AddStaticAbilities$, then
	// GainThisAbility$ (CardFactory.java:633-682).
	f := &def.Faces[0]
	for _, sub := range a.Params.Subs {
		switch {
		case strings.EqualFold(sub.Key, "AddTriggers"):
			f.Triggers = append(append([]*compile.Ability(nil), f.Triggers...), sub.Ability)
		case strings.EqualFold(sub.Key, "AddAbilities"):
			f.Abilities = append(append([]*compile.Ability(nil), f.Abilities...), sub.Ability)
		case strings.EqualFold(sub.Key, "AddStaticAbilities"):
			f.Statics = append(append([]*compile.Ability(nil), f.Statics...), sub.Ability)
		}
	}
	if ch.gain != nil {
		switch ch.gainKind {
		case compile.Trigger:
			f.Triggers = append(append([]*compile.Ability(nil), f.Triggers...), ch.gain)
		case compile.Replacement:
			f.Replacements = append(append([]*compile.Ability(nil), f.Replacements...), ch.gain)
		default:
			f.Abilities = append(append([]*compile.Ability(nil), f.Abilities...), ch.gain)
		}
	}
	switch {
	case ch.keepName:
		def.Name = out.Def.Name
	case ch.hasNewName:
		def.Name = ch.newName
	}
	return def, nil
}

func faceAt(def *compile.Card, i int) *compile.Face {
	if def == nil {
		return nil
	}
	return &def.Faces[i]
}

// cloneChanges are an ability's "except" params, read once.
type cloneChanges struct {
	keepName, hasNewName bool
	newName              string
	addColors, setColor  bool
	colors               mana.Colors
	nonLegendary         bool
	addTypes             []string
	addKeywords          []string
	keywordsIfNew        bool
	setPower, setTough   bool
	power, toughness     int
	addAmounts           map[string]expr.Amount
	addAmountNames       []string
	gain                 *compile.Ability
	gainKind             compile.Record
}

func readCloneChanges(g *Game, a *Ability) (cloneChanges, error) {
	var ch cloneChanges
	ch.keepName = hasParam(a, "KeepName")
	ch.newName, ch.hasNewName = a.Params.Param("NewName")
	parseColors := func(key string) (mana.Colors, error) {
		raw, _ := a.Params.Param(key)
		var out mana.Colors
		// ColorSet.fromNames(split(",")): Java reads a name it does not know
		// as no color at all; this port refuses it instead (PORT-8,
		// effects-clone.md).
		for _, name := range strings.Split(raw, ",") {
			name = strings.TrimSpace(name)
			if name == "" {
				continue
			}
			c, ok := colorFromName(strings.ToUpper(name[:1]) + strings.ToLower(name[1:]))
			if !ok {
				return 0, fmt.Errorf("engine: Clone: %s$ %q names no color", key, raw)
			}
			out |= c
		}
		return out, nil
	}
	var err error
	if ch.addColors = hasParam(a, "AddColors"); ch.addColors {
		if ch.colors, err = parseColors("AddColors"); err != nil {
			return ch, err
		}
	}
	if ch.setColor = hasParam(a, "SetColor"); ch.setColor {
		if ch.colors, err = parseColors("SetColor"); err != nil {
			return ch, err
		}
	}
	ch.nonLegendary = hasParam(a, "NonLegendary")
	if raw, ok := a.Params.Param("AddTypes"); ok {
		ch.addTypes = strings.Split(raw, " & ")
	}
	if raw, ok := a.Params.Param("AddKeywords"); ok {
		raw, ch.keywordsIfNew = strings.CutPrefix(raw, "IfNew ")
		ch.addKeywords = strings.Split(raw, " & ")
	}
	if ch.setPower = hasParam(a, "SetPower"); ch.setPower {
		if ch.power, err = optionalAmount(g, a, "Clone", "SetPower", 0); err != nil {
			return ch, err
		}
	}
	if ch.setTough = hasParam(a, "SetToughness"); ch.setTough {
		if ch.toughness, err = optionalAmount(g, a, "Clone", "SetToughness", 0); err != nil {
			return ch, err
		}
	}
	if raw, ok := a.Params.Param("AddSVars"); ok {
		for _, name := range strings.Split(raw, ",") {
			key := strings.ToLower(strings.TrimSpace(name))
			if amt, ok := a.Amounts[key]; ok {
				if ch.addAmounts == nil {
					ch.addAmounts = map[string]expr.Amount{}
				}
				ch.addAmounts[key] = amt
				ch.addAmountNames = append(ch.addAmountNames, key)
			}
		}
	}
	return ch, nil
}

// apply is getCloneStates' per-state loop body on one copied face f. out is
// the same face of the card becoming the copy (KeepName$), printed the same
// face of the copied card's own uncopied definition (SetPower$'s CR 208.3
// test). Every slice and map f shares with the database is replaced, never
// written through: the definition is shared by every game in the process.
func (ch *cloneChanges) apply(f, out, printed *compile.Face) {
	switch {
	case ch.keepName && out != nil:
		f.Name = out.Name
	case ch.hasNewName:
		f.Name = ch.newName
	}
	if ch.addColors {
		current := f.ManaCost.Colors()
		if f.HasColors {
			current = f.Colors
		}
		f.Colors, f.HasColors = current|ch.colors, true
	}
	if ch.setColor {
		f.Colors, f.HasColors = ch.colors, true
	}
	if ch.nonLegendary {
		f.Type = f.Type.Without(cardtype.ParseToken("Legendary"))
	}
	for _, t := range ch.addTypes {
		f.Type = f.Type.Union(cardtype.ParseToken(t))
	}
	if len(ch.addKeywords) > 0 {
		kws := append([]string(nil), f.Keywords...)
		for _, k := range ch.addKeywords {
			if ch.keywordsIfNew && hasKeywordLine(f.Keywords, keyword.Parse(k).Name) {
				continue
			}
			kws = append(kws, k)
		}
		f.Keywords = kws
	}
	// CR 208.3: a noncreature object not on the battlefield has power or
	// toughness only if it has them printed.
	if (ch.setPower || ch.setTough) &&
		(f.Type.Has(cardtype.Creature) || out != nil && printed != nil && (printed.Power != "" || printed.Toughness != "")) {
		if ch.setPower {
			f.Power = strconv.Itoa(ch.power)
		}
		if ch.setTough {
			f.Toughness = strconv.Itoa(ch.toughness)
		}
	}
	if len(ch.addAmounts) > 0 {
		amounts := make(map[string]expr.Amount, len(f.Amounts)+len(ch.addAmounts))
		for k, v := range f.Amounts {
			amounts[k] = v
		}
		for _, k := range ch.addAmountNames {
			amounts[k] = ch.addAmounts[k]
		}
		f.Amounts = amounts
	}
	// A characteristic-defining ability setting what an "except" sets is
	// gone from the copy, and SetColor$ removes devoid.
	if ch.setPower || ch.setTough || ch.setColor {
		var statics []*compile.Ability
		for _, s := range f.Statics {
			if _, cda := s.Param("CharacteristicDefining"); cda && cloneOverridesCDA(ch, s) {
				continue
			}
			statics = append(statics, s)
		}
		f.Statics = statics
	}
	if ch.setColor {
		var kws []string
		for _, k := range f.Keywords {
			if keyword.Parse(k).Name != "Devoid" {
				kws = append(kws, k)
			}
		}
		f.Keywords = kws
	}
}

func cloneOverridesCDA(ch *cloneChanges, s *compile.Ability) bool {
	_, power := s.Param("SetPower")
	_, tough := s.Param("SetToughness")
	_, color := s.Param("SetColor")
	return ch.setPower && power || ch.setTough && tough || ch.setColor && color
}

func hasKeywordLine(lines []string, name string) bool {
	for _, l := range lines {
		if keyword.Parse(l).Name == name {
			return true
		}
	}
	return false
}

// cloneRoot is GainThisAbility$'s "this ability": the trigger, activated
// ability or replacement on the host whose compiled tree holds the line
// resolving now -- SpellAbility.getRootAbility, and through an immediate or
// delayed trigger its spawning ability (Aurora Shifter), since the spawned
// trigger's Execute$ is compiled inside the ability that spawns it. The
// host's current definition is searched first, then its own and each copy
// effect's, so a copy that already gained the ability finds it again. kind
// is the list the root came from.
func cloneRoot(host *Card, a *Ability) (*compile.Ability, compile.Record, error) {
	defs := []*compile.Card{host.Def, host.UncopiedDef()}
	for _, e := range host.copies {
		defs = append(defs, e.def)
	}
	for _, def := range defs {
		if def == nil {
			continue
		}
		for i := range def.Faces {
			f := &def.Faces[i]
			for _, list := range []struct {
				roots []*compile.Ability
				kind  compile.Record
			}{{f.Triggers, compile.Trigger}, {f.Abilities, compile.Spell}, {f.Replacements, compile.Replacement}} {
				for _, r := range list.roots {
					if abilityTreeHolds(r, a.Params) {
						return r, list.kind, nil
					}
				}
			}
		}
	}
	return nil, 0, fmt.Errorf("engine: Clone: GainThisAbility$ from outside the host's own abilities not resolvable yet")
}

// abilityTreeHolds reports whether target is root or one of the abilities
// root's params reference, at any depth. A compiled tree is acyclic
// (compile's ErrCycle) and every reference is its own *Ability, so pointer
// identity names exactly one place.
func abilityTreeHolds(root, target *compile.Ability) bool {
	if root == target {
		return true
	}
	for _, sub := range root.Subs {
		if abilityTreeHolds(sub.Ability, target) {
			return true
		}
	}
	return false
}
