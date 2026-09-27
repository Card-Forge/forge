// The game: the arena every handle indexes into, and the zone table.

// Package engine is the rules core.
//
// It is one Go package on purpose. Java's forge.game splits across 21 packages
// with 82 two-package cycles between them, and Go forbids cycles, so the
// mirrored layout does not compile. What lives here is the set of types whose
// references are genuinely mutual -- Game, Card, Player, Zone, and the stack,
// combat and effect machinery that lands in later slices. Everything that can
// be acyclic is a separate package importing this one, never the other way
// round (ADR-0003).
//
// Inside the package, tools/enginelint enforces the file-group boundaries the
// compiler cannot see, because Go has no sub-package visibility.
package engine

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/pkg/collect"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// Game is one game in progress. It owns every entity in it.
//
// The engine runs a goroutine per game and games share only immutable data, so
// nothing here is guarded and nothing here should be: a mutex in this package
// is a design bug (GO-3, ADR-0005).
type Game struct {
	// cards is the arena. A CardID is an index into it, slot 0 is NoCard, and
	// the slice only ever grows -- handles are never reused (ADR-0009).
	cards []Card
	// players is the arena for players, on the same terms.
	players []Player
	// zones is keyed by type and owner. Ownerless zones -- the stack, and the
	// shared exile Java models per-player -- use NoPlayer.
	zones map[zoneKey]*Zone

	// db is the compiled card database, shared read-only by every game in the
	// process (ADR-0005, ADR-0007).
	db *compile.DB
	// rand is this game's own stream, seeded per game so a run is
	// reproducible from its seed alone. There is no package-level RNG
	// (GO-2, ADR-0006).
	rand *javarand.Rand
	// registry is the Registry this game resolves through. NewGame sets it
	// to NewRegistry() -- an array of stateless effect values, immutable
	// and shareable like db (ADR-0020) -- so a static trigger fired where
	// no Registry is passed in (TapLandForMana, ActivateManaAbility) has one
	// (statictrigger.go). Registry.Resolve re-records the table it runs
	// on, so an effect resolving one of its own AdditionalAbility SVars
	// (TrueSubAbility$, RepeatSubAbility$, Choices$) uses the same dispatch
	// as the stack object around it (additional.go).
	registry *Registry
	// pendingErr is a static trigger's error from a site with no error
	// return (resolveStaticTriggers, statictrigger.go), waiting for the
	// nearest boundary that has one -- Registry.Resolve, ResolveStack,
	// PassPriority, or a caller's own TakePendingError (ADR-0020 decision
	// 4, GO-7). The first error is kept; nil when none is waiting.
	pendingErr error

	// timestamp is the monotonic counter behind Card.Timestamp. It only ever
	// increases, so an ordering never repeats within a game.
	timestamp uint64
	// nextStackItemID is the monotonic counter behind Ability.ID (ADR-0018),
	// on the same terms as timestamp above: PushAbility increments it and
	// hands out the new value, so an ID is never reused within a game.
	nextStackItemID StackItemID

	// over is set once CheckStateBasedActions decides the game has ended --
	// a win, a loss, or a draw. Nothing unsets it: a game that has ended
	// stays ended (action.go).
	over bool

	// turn, activePlayer and activePhase are the turn structure: whose turn
	// it is, what step or phase it is in, and how many turns have passed.
	// Zero-valued (0, NoPlayer, Untap) until StartTurn, which is also a
	// coherent "the game has not started its turn structure yet" reading --
	// nothing downstream treats an unstarted game's phase as meaningful
	// without also checking activePlayer (turn.go).
	turn         int
	activePlayer PlayerID
	activePhase  PhaseType

	// sink is where this game's events go. DiscardSink by default: most
	// callers -- every test, fixture loading -- have nothing listening and
	// should not have to construct a sink just to build a game.
	sink Sink

	// stack is CR 405's stack of resolvable abilities, last on first off (the
	// last element is the top). Distinct from the Stack ZoneType a spell's
	// own card occupies -- Zone(Stack, NoPlayer) tracks where the card is,
	// this tracks resolution order, and nothing yet moves a card there or
	// pushes an ability here (stack.go).
	stack []Ability

	// combat is CR 506-510's combat state, starting with CR 508's declared
	// attackers (combat.go, attack.go). Zero-valued when no combat is in
	// progress.
	combat Combat

	// pumps is every Pump effect (pumpeffect.go) currently in force --
	// cleanupStep's own former "duration tracking this port does not have"
	// gap. Unlike Card.PT/Card.KeywordMod's own static-ability contributions,
	// a Pump's own contribution is not rederivable from a card script each
	// pass (there is no S: line behind it to re-read), so it is kept here and
	// re-added into its target's own PT/KeywordMod every
	// CheckStateBasedActions pass by applyPumpEffects (continuous.go) instead
	// -- cleared down to its own Permanent-only remainder every cleanupStep
	// (turn.go), CR 514.2's "until end of turn" effects wearing off.
	pumps []pumpRecord
	// animates is every Animate-shaped effect in force (animate.go):
	// Animate, AnimateAll, Debuff, Protection, ProtectionAll.
	animates []animateRecord
	// delayed is every registered delayed trigger (delayedtrigger.go), in
	// registration order.
	delayed []delayedTrigger
	// extraPhases is PhaseHandler.extraPhases: for each phase, the stack of
	// phases an AddPhase effect queued to follow it instead of the normal
	// next one (last entry first). Cleared when the turn ends.
	extraPhases [numPhaseTypes][]PhaseType
	// skips is every SkipPhase effect in force (skipphaseeffect.go).
	skips []skipPhase
	// combatsThisTurn is PhaseHandler.nCombatsThisTurn: combat phases begun
	// this turn, counted at CombatBegin and reset as the turn ends -- what
	// FirstCombat$ reads once AddPhase can add a second combat.
	combatsThisTurn int
	// skipDamageSteps is PhaseHandler.skipDamageSteps: set by a driven
	// entry into DeclareBlockers when no creature attacked, read by the
	// damage steps that follow (drivenSkips, turn.go; ADR-0026).
	skipDamageSteps bool

	// turnOrderReversed is Game.turnOrder flipped to Direction.Right by
	// ReverseTurnOrder: nextPlayerAfter walks the seats backwards.
	turnOrderReversed bool

	// preventShields are PreventDamage's "prevent the next N damage"
	// shields, oldest first, all ending at cleanup.
	preventShields []preventShield

	// exileGrants are Airbend's and Heist's "may cast it from exile"
	// permissions (ExilePlayGrant), in the order they were made.
	exileGrants []ExilePlayGrant

	// dayTime is Game.daytime: DayNeither until something makes it day or
	// night (DayTime, CR 726.2), then Day or Night.
	dayTime DayTime
	// previousPlayer and previousPlayerSpells are the last turn's active
	// player and the spells they cast that turn -- what CR 726.3a's untap
	// step check reads (Untap.doDayTime).
	previousPlayer       PlayerID
	previousPlayerSpells int
	// extraTurns is Java PhaseHandler's own extra-turn stack (AddTurn): the
	// last entry is the next turn taken. The bottom entry, pushed with the
	// first extra turn, is the player whose normal turn comes next, so
	// normal turn order resumes once the stack drains.
	extraTurns []PlayerID
	// combatDamagePrevented is Fog's "prevent all combat damage this turn",
	// cleared at cleanup (damagePrevented/damagePreventedPlayer read it).
	combatDamagePrevented bool

	// lki is CR 603.6d's "look back in time": each CardID's own frozen copy
	// of itself from the instant before Move's own battlefield-leaving branch
	// (below) reset its Counters/PT/TypeMod/ColorMod/KeywordMod, so
	// checkDiesTriggers/otherDiesTriggerMatches (trigger.go) still see the
	// dying card's own power, toughness, type, color, keyword and counter
	// state as it stood on the battlefield an instant earlier, not the
	// printed-only state Move has already reset it to by the time either
	// function runs. Ported from CardCopyService.getLKICopy() at the one
	// subset of its several dozen copied fields the real corpus's own dies
	// triggers actually read: 116 of 7,574 real Mode$ ChangesZone lines whose
	// Destination$ permits Graveyard also name a ValidCard$ checking a
	// power/toughness/counter/keyword property of the dying card itself
	// (port-log/game-state.md's "Last-known-information" section) --
	// Card.Def/Card.Controller() need no such lookup (Move's own doc comment
	// already established why), so the LKI copy is a plain struct copy rather
	// than Java's own field-by-field reconstruction. Overwritten whole, never
	// merged, on every subsequent trip off the battlefield -- the identical
	// "one frozen copy, replaced whole" contract getLKICopy() itself has, one
	// CardID absent here simply never having left the battlefield yet.
	lki map[CardID]*Card

	// monarch is the player who is the monarch (CR 724, Game.monarch),
	// NoPlayer while nobody is. monarchBeginTurn is who was the monarch as
	// the current turn began (Game.monarchBeginTurn, set by PhaseHandler at
	// each new turn), read by Mode$ BecomeMonarch's BeginTurn$.
	monarch, monarchBeginTurn PlayerID
	// initiative is the player who has the initiative (CR 725,
	// Game.hasInitiative), NoPlayer while nobody does.
	initiative PlayerID
}

// pumpRecord is one resolved Pump effect's own contribution -- Defined$'s
// card, the amount and/or keywords it grants, and whether Duration$ named
// the literal value "Permanent" rather than the default "until end of turn."
type pumpRecord struct {
	Card             CardID
	Timestamp        uint64
	Power, Toughness int
	Keywords         []string
	Permanent        bool
}

// clearPumps drops every pumpRecord naming id, called from Move (above)
// alongside c.PT.Clear()/c.KeywordMod.Clear() the moment a card leaves the
// battlefield, for the identical reason: CardID is stable across zone
// changes here (ADR-0009, Move's own doc comment), so without this a
// Permanent Pump (or one recorded earlier this turn) would silently survive
// a trip to the graveyard and reapply the moment a Raise Dead-style effect
// returns the same CardID to the battlefield -- Java's own applyPump avoids
// this by checking the target's game timestamp at apply time
// (`applyTo.equalsWithGameTimestamp(gameCard)`), a per-instance identity
// check this port has no equivalent of; dropping the record on exit gets
// the same real-world answer without one.
func (g *Game) clearPumps(id CardID) {
	kept := g.pumps[:0]
	for _, p := range g.pumps {
		if p.Card != id {
			kept = append(kept, p)
		}
	}
	g.pumps = kept
}

// SetSink replaces the game's event sink. The zero Game has a DiscardSink,
// so this is opt-in for whatever eventually reads the stream (a recorder,
// M8) rather than a constructor parameter every existing caller would have
// had to grow one to keep compiling.
func (g *Game) SetSink(s Sink) { g.sink = s }

// Over reports whether the game has ended, per the last call to
// CheckStateBasedActions.
func (g *Game) Over() bool { return g.over }

// SetOver writes Over directly, the same relationship SetTurnState has to
// StartTurn/AdvancePhase: a state injection for fixture loading, not
// something real play calls. CheckStateBasedActions is the only thing that
// sets it as a side effect of actually deciding a game has ended.
func (g *Game) SetOver(over bool) { g.over = over }

// Turn is the current turn number, per the last player whose turn ended.
func (g *Game) Turn() int { return g.turn }

// ActivePlayer is whose turn it is. NoPlayer before StartTurn.
func (g *Game) ActivePlayer() PlayerID { return g.activePlayer }

// ActivePhase is the step or phase in progress.
func (g *Game) ActivePhase() PhaseType { return g.activePhase }

// zoneKey identifies a zone. Ownerless zones carry NoPlayer.
type zoneKey struct {
	kind  ZoneType
	owner PlayerID
}

// NewGame builds an empty game with the given players.
//
// The database and the RNG are injected rather than reached for, which is what
// keeps this package free of the singletons Java uses -- StaticData, FModel
// and MyRandom all become parameters (GO-2).
func NewGame(db *compile.DB, rng *javarand.Rand, names []string) *Game {
	g := &Game{
		// Slot 0 is NoCard and NoPlayer, so a zero-valued handle field means
		// "none" instead of aliasing the first entity created.
		cards:    make([]Card, 1, 128),
		players:  make([]Player, 1, len(names)+1),
		zones:    make(map[zoneKey]*Zone, len(names)*8),
		db:       db,
		rand:     rng,
		registry: NewRegistry(),
		sink:     DiscardSink{},
		lki:      make(map[CardID]*Card),
	}
	for _, name := range names {
		id := PlayerID(len(g.players))
		g.players = append(g.players, Player{ID: id, Name: name, CrankCounter: 3})
		for _, z := range []ZoneType{Hand, Library, Graveyard, Battlefield, Exile, Command, Sideboard} {
			g.zones[zoneKey{z, id}] = &Zone{Type: z, Owner: id, cards: collect.NewOrderedSet[CardID](0)}
		}
	}
	g.zones[zoneKey{Stack, NoPlayer}] = &Zone{Type: Stack, cards: collect.NewOrderedSet[CardID](0)}
	return g
}

// DB is the compiled card database this game reads from.
func (g *Game) DB() *compile.DB { return g.db }

// Rand is this game's random stream.
func (g *Game) Rand() *javarand.Rand { return g.rand }

// TakePendingError returns the error a static trigger raised at a site with
// no error return (a mana ability's tap), and clears it, so
// each such error reaches exactly one caller (ADR-0020 decision 4, GO-7).
// ResolveStack, PassPriority and Registry.Resolve take it themselves; a
// driver that calls a bool-returning entry point such as TapLandForMana
// directly, like the fixture action runner, takes it after each call. nil
// when nothing is waiting.
func (g *Game) TakePendingError() error {
	err := g.pendingErr
	g.pendingErr = nil
	return err
}

// recordPendingError keeps err for TakePendingError unless an earlier error
// is already waiting: the first failure is the one that explains the rest.
func (g *Game) recordPendingError(err error) {
	if g.pendingErr == nil {
		g.pendingErr = err
	}
}

// Card resolves a handle. It panics on an out-of-range or absent handle,
// because that is an engine invariant breach rather than anything a card
// script can cause -- the game boundary recovers it so one bad card cannot
// take down a batch (GO-7).
func (g *Game) Card(id CardID) *Card {
	if id == NoCard || int(id) >= len(g.cards) {
		panic("engine: card handle out of range")
	}
	return &g.cards[id]
}

// Player resolves a player handle, on the same terms as [Game.Card].
func (g *Game) Player(id PlayerID) *Player {
	if id == NoPlayer || int(id) >= len(g.players) {
		panic("engine: player handle out of range")
	}
	return &g.players[id]
}

// Players returns every player, in seating order.
func (g *Game) Players() []PlayerID {
	out := make([]PlayerID, 0, len(g.players)-1)
	for i := 1; i < len(g.players); i++ {
		out = append(out, PlayerID(i))
	}
	return out
}

// NumCards is how many handles have been allocated, NoCard excluded. It is the
// arena's high-water mark, not a count of cards in play.
func (g *Game) NumCards() int { return len(g.cards) - 1 }

// NewCard allocates a card and puts it in a zone.
//
// The handle it returns is stable for the life of the game even after the card
// changes zone, is destroyed, or leaves the game entirely.
func (g *Game) NewCard(def *compile.Card, owner PlayerID, zone ZoneType) CardID {
	id := CardID(len(g.cards))
	g.cards = append(g.cards, Card{
		ID:         id,
		Def:        def,
		Owner:      owner,
		controller: owner,
	})
	g.put(id, zone, owner)
	return id
}

// Zone returns a zone by type and owner, creating it on first use. Ownerless
// zones are addressed with NoPlayer.
func (g *Game) Zone(kind ZoneType, owner PlayerID) *Zone {
	key := zoneKey{kind, owner}
	z, ok := g.zones[key]
	if !ok {
		z = &Zone{Type: kind, Owner: owner, cards: collect.NewOrderedSet[CardID](0)}
		g.zones[key] = z
	}
	return z
}

// traitHosts is every card of pid's whose static abilities and triggers are
// active: the battlefield's permanents, then pid's effect cards in the
// Command zone, whose traits EffectEffect.java makes active there alone
// (setActiveZone(EnumSet.of(ZoneType.Command))). With no effect card it is
// the battlefield's own slice, so the common case allocates nothing.
func (g *Game) traitHosts(pid PlayerID) []CardID {
	bf := g.Zone(Battlefield, pid).Cards()
	cmd := g.Zone(Command, pid).Cards()
	effects := 0
	for _, id := range cmd {
		if g.cards[id].IsEffect {
			effects++
		}
	}
	if effects == 0 {
		return bf
	}
	out := make([]CardID, 0, len(bf)+effects)
	out = append(out, bf...)
	for _, id := range cmd {
		if g.cards[id].IsEffect {
			out = append(out, id)
		}
	}
	return out
}

// LKI returns id's frozen last-known-information snapshot -- Move's own
// battlefield-leaving branch, below -- or nil if id has never left the
// battlefield. checkDiesTriggers/otherDiesTriggerMatches (trigger.go) are its
// only callers today.
func (g *Game) LKI(id CardID) *Card {
	return g.lki[id]
}

// Move takes a card out of the zone it is in and appends it to another,
// stamping it on the way.
//
// Every zone change gets a new timestamp, which is what continuous effects
// order by and what makes a card that left and came back a different object to
// the layer system.
//
// Leaving the battlefield freezes a copy of the card into [Game.lki] before
// clearing Counters, Damage, Tapped, PT/TypeMod/ColorMod/KeywordMod and any
// attachment; entering it sets SummonSick and, for a planeswalker or a
// Battle, its printed starting loyalty/defense as counters (CR 121.5,
// 704.5v). Java gets all three for free: GameAction.changeZone builds a new
// Card object for the destination zone (CardCopyService.copyCard), so a
// field simply is not copied onto it, a freshly-built permanent starts sick
// unless something says otherwise, and a planeswalker's loyalty is set once
// at that same construction -- CR 121.5 puts loyalty nowhere near Layer 7,
// so it has no computed accessor the way Power/Toughness do; it is exactly
// its Loyalty counter count from the moment of entry, full stop
// (game-state.md's "Loyalty is not a layer" section has the full citation).
// A CardID is stable across zone changes here instead (ADR-0009) — the same
// struct persists, so a creature that dies with three +1/+1 counters would return
// from the graveyard still carrying them unless this clears them, a Raise
// Dead'd creature would enter without summoning sickness unless this sets
// it, and a planeswalker cast a second time would enter with whatever
// loyalty its last trip to the battlefield ended at unless this sets a
// fresh value every time.
//
// [Game.NewCard] does none of the three: it is the arena-allocation
// primitive fixture loading uses to seat a board mid-game, where a
// battlefield permanent's starting Tapped/SummonSick/loyalty-or-defense is
// exactly what the fixture says, not a rule this port applies for it. Move
// is real play transitioning a card between zones; NewCard is "this card
// already exists here."
//
// Every call emits a ZoneChanged event, for the same reason NewCard does
// not: this is real play, and NewCard is setup nothing downstream should
// see as something happening.
func (g *Game) Move(id CardID, kind ZoneType, owner PlayerID) {
	if g.ceaseCopiedSpell(id) {
		return
	}
	c := g.Card(id)
	from := c.Zone
	if from == Stack && kind != Battlefield {
		// CR 108.4a: only a permanent or a spell has a controller. A spell
		// leaving the stack for anywhere but the battlefield goes back to
		// its owner's control -- the other half of castSpell's CR 110.2
		// "the caster controls it" (castspell.go).
		c.controller = c.Owner
	}
	isPermanent := c.Type().IsPermanent()
	g.Zone(c.Zone, c.ZoneOwner).remove(id)
	g.put(id, kind, owner)

	// A card exiled face down (Heist) turns face up as it leaves exile.
	if from == Exile && kind != Exile {
		c.turnFaceUp()
	}
	switch {
	case from == Battlefield && kind != Battlefield:
		snap := *c
		g.lki[id] = &snap
		// After the snapshot: last-known information keeps a phased-out
		// card's own phasing (GameAction.java:977 reads lki.isPhasedOut()).
		c.phasedOut, c.directlyPhasedOut, c.wontPhaseInNormal = NoPlayer, false, false
		c.Counters = Counters{}
		c.Damage.Clear()
		c.PT.Clear()
		c.TypeMod.Clear()
		c.ColorMod.Clear()
		c.KeywordMod.Clear()
		c.Tapped = false
		c.SummonSick = false
		c.Exerted = false
		c.LoyaltyAbilityActivated = false
		c.ProtectingPlayer = NoPlayer
		c.controller = c.Owner
		c.tempControllers = nil
		c.RegenShields = 0
		c.detainedBy = nil
		c.goadedBy = nil
		c.mustBlock = nil
		c.Suspected, c.Solved, c.Harnessed = false, false, false
		// Layer 3's text change ends with the object (CR 400.7), before the
		// copy and face-down restores below read or replace Def.
		c.clearTextChange()
		g.endCopiesOnLeave(id)
		c.Sprocket = 0
		c.turnFaceUp()
		c.turnFrontFaceUp()
		g.dropPreventShields(id)
		g.Unattach(id)
		g.clearPumps(id)
		g.clearAnimates(id)
		g.loseRingBearer(id)
	case from != Battlefield && kind == Battlefield:
		c.SummonSick = true
		if loyalty, ok := c.BaseLoyalty(); ok && c.Type().Has(cardtype.Planeswalker) {
			c.Counters.Add(Loyalty, loyalty)
			emitCounterChanged(g.sink, id, CardEntity(id), Loyalty, loyalty)
		}
		if defense, ok := c.BaseDefense(); ok && c.Type().Has(cardtype.Battle) {
			c.Counters.Add(Defense, defense)
			emitCounterChanged(g.sink, id, CardEntity(id), Defense, defense)
		}
	}

	// CR's own "descend" tracker (Zone.add's own "!rollback" branch, Java):
	// a permanent card put into a graveyard from anywhere this turn marks its
	// owner (not its controller -- every real Move-to-Graveyard call site in
	// this port already passes owner as c.Owner itself) as having descended,
	// read back by matchesPlayerProperty's own "descended" case (valid.go).
	// Java's own check also excludes a token.
	if kind == Graveyard && isPermanent && !c.IsToken {
		g.Player(owner).DescendedThisTurn = true
	}

	g.sink.Emit(Event{
		Kind:   ZoneChanged,
		Phase:  g.activePhase,
		Active: g.activePlayer,
		Actor:  owner,
		Turn:   uint16(g.turn),
		Source: id,
		From:   from,
		To:     kind,
	})
	g.effectCardsSeeMove(id, from, kind)
}

// MoveToLibraryTop moves id to the top of owner's library -- library index
// 0, the position [Game.DrawCards] reads from and the one CR 701.19's own
// scry action needs for the cards it puts back rather than to the bottom
// (scryeffect.go, its first real caller). [Game.Move] always appends to a
// zone's own end, which for the library is the bottom (mulligan.go's own
// tuck already relies on exactly that); this is Move's mirror for the one
// case a card needs the opposite end instead, sharing its every other
// behavior -- including the Battlefield-transition cleanup below, so a
// future caller that moves a card onto the top of a library from anywhere
// other than the library itself (a tutor effect's own
// "Destination$ Library | LibraryPosition$ 0" shape, not built yet) gets
// that cleanup for free rather than a scry-only shortcut that silently
// skips it.
func (g *Game) MoveToLibraryTop(id CardID, owner PlayerID) {
	if g.ceaseCopiedSpell(id) {
		return
	}
	c := g.Card(id)
	from := c.Zone
	if from == Stack {
		c.controller = c.Owner
	}
	g.Zone(c.Zone, c.ZoneOwner).remove(id)
	g.putFront(id, owner)

	if from == Battlefield {
		c.phasedOut, c.directlyPhasedOut, c.wontPhaseInNormal = NoPlayer, false, false
		c.Counters = Counters{}
		c.Damage.Clear()
		c.PT.Clear()
		c.TypeMod.Clear()
		c.ColorMod.Clear()
		c.KeywordMod.Clear()
		c.Tapped = false
		c.SummonSick = false
		c.Exerted = false
		c.LoyaltyAbilityActivated = false
		c.ProtectingPlayer = NoPlayer
		c.controller = c.Owner
		c.tempControllers = nil
		c.RegenShields = 0
		c.detainedBy = nil
		c.goadedBy = nil
		c.mustBlock = nil
		c.Suspected, c.Solved, c.Harnessed = false, false, false
		// Layer 3's text change ends with the object (CR 400.7), before the
		// copy and face-down restores below read or replace Def.
		c.clearTextChange()
		g.endCopiesOnLeave(id)
		c.Sprocket = 0
		c.turnFaceUp()
		c.turnFrontFaceUp()
		g.dropPreventShields(id)
		g.Unattach(id)
		g.clearPumps(id)
		g.clearAnimates(id)
		g.loseRingBearer(id)
	}

	g.sink.Emit(Event{
		Kind:   ZoneChanged,
		Phase:  g.activePhase,
		Active: g.activePlayer,
		Actor:  owner,
		Turn:   uint16(g.turn),
		Source: id,
		From:   from,
		To:     Library,
	})
	g.effectCardsSeeMove(id, from, Library)
}

// ceaseCopiedSpell is GameAction.changeZone's copied-spell early return
// (GameAction.java:100-105, CR 707.10a): a copy of a spell that would move
// anywhere is removed from its zone instead -- no timestamp, no ZoneChanged,
// no zone-change trigger. It is parked in its owner's None zone, since a
// CardID is never freed (ADR-0009), the same place removeTokensOffBattlefield
// (token.go) parks a token. A permanent spell's copy that resolves is not
// ceased: permanentEffect/attachEffect (castspell.go) turn it into a token
// first (CR 111.11, GameAction.java:96). Reports whether id was ceased, so
// the caller stops.
func (g *Game) ceaseCopiedSpell(id CardID) bool {
	c := g.Card(id)
	if !c.IsCopiedSpell {
		return false
	}
	g.Zone(c.Zone, c.ZoneOwner).cards.Remove(id)
	c.Zone, c.ZoneOwner = None, c.Owner
	g.Zone(None, c.Owner).cards.Add(id)
	return true
}

// Shuffle randomises one zone's order, in place, using the game's own random
// stream. Ported from Player.shuffle (Collections.shuffle(list,
// MyRandom.getRandom())): javarand.Rand.Shuffle reproduces that algorithm
// exactly, which is what makes a shuffled library replay identically from
// the same seed (pkg/javarand's P0 gate).
//
// A shuffle stamps no timestamp and fires no zone-change: order within a zone
// is not itself a zone change, and nothing reads a card's Timestamp to learn
// where it sits in its own library.
func (g *Game) Shuffle(kind ZoneType, owner PlayerID) {
	z := g.Zone(kind, owner)
	g.rand.Shuffle(z.cards.Len(), z.cards.Swap)
}

// put appends a card to a zone and records the reverse index on the card. It
// does not remove the card from wherever it was, so only [Game.Move] and
// [Game.NewCard] may call it.
func (g *Game) put(id CardID, kind ZoneType, owner PlayerID) {
	c := &g.cards[id]
	c.Zone, c.ZoneOwner = kind, owner
	g.timestamp++
	c.Timestamp, c.zoneStamp = g.timestamp, g.timestamp
	g.Zone(kind, owner).cards.Add(id)
}

// putFront is put's mirror for the library's own top instead of a zone's
// end: it does not remove the card from wherever it was, so only
// [Game.MoveToLibraryTop] may call it.
func (g *Game) putFront(id CardID, owner PlayerID) {
	c := &g.cards[id]
	c.Zone, c.ZoneOwner = Library, owner
	g.timestamp++
	c.Timestamp, c.zoneStamp = g.timestamp, g.timestamp
	g.Zone(Library, owner).cards.Prepend(id)
}

// Attach attaches one card to another, moving it off whatever it was attached
// to first.
//
// The two sides -- the attachment's own pointer and the host's list -- are one
// fact stored twice, so this and [Game.Unattach] are the only writers. A card
// cannot be attached to itself, and cannot be attached to a card that does not
// exist; both are invariant breaches rather than rules questions (GO-7).
func (g *Game) Attach(attachment, host CardID) {
	if attachment == host {
		panic("engine: card attached to itself")
	}
	a, h := g.Card(attachment), g.Card(host)
	g.Unattach(attachment)
	a.attachedTo = host
	if h.attachments == nil {
		h.attachments = collect.NewOrderedSet[CardID](2)
	}
	h.attachments.Add(attachment)
}

// Unattach detaches a card from whatever it is attached to. Detaching an
// unattached card is a no-op, because the callers that clean up after a zone
// change do not track whether there was anything to clean.
func (g *Game) Unattach(attachment CardID) {
	a := g.Card(attachment)
	if a.attachedTo == NoCard {
		return
	}
	if h := g.Card(a.attachedTo); h.attachments != nil {
		h.attachments.Remove(attachment)
	}
	a.attachedTo = NoCard
}

// setPhasedOut is Card.setPhasedOut plus the zone's half of the same fact:
// p NoPlayer phases id in, anything else phases it out on p's behalf. It is
// the only writer of Card.phasedOut and Zone.phasedOut, which is what keeps
// the battlefield enumeration (Zone.Cards) and the card agreeing (ADR-0021,
// decision 5). The card keeps its place in the zone's order either way
// (GO-12): phasing is not a zone change (CR 702.26d).
func (g *Game) setPhasedOut(id CardID, p PlayerID) {
	c := g.Card(id)
	z := g.Zone(c.Zone, c.ZoneOwner)
	c.phasedOut = p
	if p == NoPlayer {
		if z.phasedOut != nil {
			z.phasedOut.Remove(id)
		}
		return
	}
	if z.phasedOut == nil {
		z.phasedOut = collect.NewOrderedSet[CardID](1)
	}
	z.phasedOut.Add(id)
}

// SetPhasedOut writes a battlefield permanent's phased-out state directly --
// p NoPlayer for phased in -- with no trigger, event or attachment
// following: fixture loading's tool (GameState.java's own PhasedOut: key,
// c.setPhasedOut at :1302-1304), the same relationship SetTurnState has to
// StartTurn. Real play phases through Game.phase (phasing.go). A card not on
// the battlefield is an error: only a permanent can phase (CR 702.26a).
func (g *Game) SetPhasedOut(id CardID, p PlayerID) error {
	if g.Card(id).Zone != Battlefield {
		return fmt.Errorf("engine: SetPhasedOut: card %d is not on the battlefield", id)
	}
	g.setPhasedOut(id, p)
	g.Card(id).directlyPhasedOut = p != NoPlayer
	return nil
}

// Clone returns an independent copy of the game.
//
// This is what the AI's lookahead runs on, so it is on a hot path and its cost
// decides search depth (GO-16). Handles are indices, so nothing has to be
// remapped: there is no equivalent of Java's CopiedGameObjectMap, and that is
// the point of addressing entities by handle (ADR-0009).
//
// "A slice copy" is the shape but not the whole job. A Card owns collections
// behind pointers -- its counters, its three memory lists, its attachments --
// and copying the slice alone would leave the clone and the original writing
// to the same ones. Each is copied when it exists and left nil when it does
// not, which is most cards most of the time; a zone's phased-out subset
// (Zone.clone, zone.go) the same. lki gets the identical
// treatment, one frozen snapshot at a time -- a card that already left the
// battlefield needs its own independent copy exactly as much as one still on
// it does.
//
// The database is shared, because it is immutable (ADR-0005). The random
// stream is copied by value, so the clone continues from where the original
// is rather than replaying it or advancing it.
func (g *Game) Clone() *Game {
	out := &Game{
		cards:   make([]Card, len(g.cards)),
		players: append([]Player(nil), g.players...),
		zones:   make(map[zoneKey]*Zone, len(g.zones)),
		db:      g.db,
		// Shared like db: a Registry holds only stateless effect values.
		registry:     g.registry,
		pendingErr:   g.pendingErr,
		timestamp:    g.timestamp,
		over:         g.over,
		turn:         g.turn,
		activePlayer: g.activePlayer,
		activePhase:  g.activePhase,
		// Always DiscardSink, whatever the original's sink is: the AI's
		// lookahead explores lines that never happened, and a clone holding
		// the real sink would record imagined casts as real.
		sink:  DiscardSink{},
		stack: append([]Ability(nil), g.stack...),
		// Carried so the clone's next push gets an ID no item already on
		// its stack has (ADR-0018: a StackItemID is never reused within a
		// game, and CopySpellAbility tells a copy from its original by it).
		nextStackItemID: g.nextStackItemID,
		combat:          g.combat.clone(),
		pumps:           append([]pumpRecord(nil), g.pumps...),

		animates: append([]animateRecord(nil), g.animates...),
		delayed:  append([]delayedTrigger(nil), g.delayed...),
		skips:    append([]skipPhase(nil), g.skips...),

		extraTurns:            append([]PlayerID(nil), g.extraTurns...),
		combatDamagePrevented: g.combatDamagePrevented,
		combatsThisTurn:       g.combatsThisTurn,
		skipDamageSteps:       g.skipDamageSteps,
		turnOrderReversed:     g.turnOrderReversed,
		preventShields:        append([]preventShield(nil), g.preventShields...),
		exileGrants:           append([]ExilePlayGrant(nil), g.exileGrants...),
		dayTime:               g.dayTime,
		previousPlayer:        g.previousPlayer,
		previousPlayerSpells:  g.previousPlayerSpells,
		lki:                   make(map[CardID]*Card, len(g.lki)),
		monarch:               g.monarch,
		monarchBeginTurn:      g.monarchBeginTurn,
		initiative:            g.initiative,
	}
	for i := range g.extraPhases {
		out.extraPhases[i] = append([]PhaseType(nil), g.extraPhases[i]...)
	}
	if g.rand != nil {
		r := *g.rand
		out.rand = &r
	}

	for i := range out.players {
		out.players[i].Counters = g.players[i].Counters.clone()
		out.players[i].Rules = g.players[i].Rules.clone()
		if g.players[i].completedDungeons != nil {
			out.players[i].completedDungeons = append([]CardID(nil), g.players[i].completedDungeons...)
		}
	}

	copy(out.cards, g.cards)
	for i := range out.cards {
		c := &out.cards[i]
		c.Counters = g.cards[i].Counters.clone()
		c.Memory = g.cards[i].Memory.clone()
		c.detainedBy = append([]PlayerID(nil), g.cards[i].detainedBy...)
		c.goadedBy = append([]goad(nil), g.cards[i].goadedBy...)
		c.mustBlock = append([]mustBlockReq(nil), g.cards[i].mustBlock...)
		c.PT = g.cards[i].PT.clone()
		c.TypeMod = g.cards[i].TypeMod.clone()
		c.ColorMod = g.cards[i].ColorMod.clone()
		c.KeywordMod = g.cards[i].KeywordMod.clone()
		c.ControlMod = g.cards[i].ControlMod.clone()
		c.tempControllers = append([]ControlEffect(nil), g.cards[i].tempControllers...)
		c.copies = append([]copyEffect(nil), g.cards[i].copies...)
		if g.cards[i].svars != nil {
			c.svars = make(map[string]int, len(g.cards[i].svars))
			for k, v := range g.cards[i].svars {
				c.svars[k] = v
			}
		}
		if g.cards[i].attachments != nil {
			c.attachments = g.cards[i].attachments.Clone()
		}
	}
	for k, z := range g.zones {
		out.zones[k] = z.clone()
	}
	for id, snap := range g.lki {
		s := *snap
		s.Counters = snap.Counters.clone()
		s.Memory = snap.Memory.clone()
		s.PT = snap.PT.clone()
		s.TypeMod = snap.TypeMod.clone()
		s.ColorMod = snap.ColorMod.clone()
		s.KeywordMod = snap.KeywordMod.clone()
		s.ControlMod = snap.ControlMod.clone()
		s.copies = append([]copyEffect(nil), snap.copies...)
		if snap.attachments != nil {
			s.attachments = snap.attachments.Clone()
		}
		out.lki[id] = &s
	}
	return out
}
