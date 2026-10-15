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
	"errors"
	"fmt"
	"maps"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/pkg/collect"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// activeReplacement names one replacement effect: its host and its line.
// Game.replacing holds the ones whose ReplaceWith$ ability is resolving.
type activeReplacement struct {
	host CardID
	rule *compile.Ability
}

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
	// relativeFace is the face whose SVars a CantBlockBy static's
	// ValidAttackerRelative$/ValidBlockerRelative$ Compare operand names
	// (Creature.powerGEIronclawX), set only while relativeMatches evaluates it.
	// Java reads the operand through the static ability, whose SVars are its
	// host's, while the amount's own card is the property's source
	// (CardProperty.java's calculateAmount(source, ..., spellAbility)); the
	// valid-string evaluator has no ability to carry that, so it rides here.
	// Never copied by Clone: nothing is mid-evaluation across a clone.
	relativeFace *compile.Face
	// relativeAmounts is relativeFace's counterpart for a trigger's
	// TargetRelativeToSource$ (Eriette, the Beguiler's `cmcLEX`): the SVars of
	// the trigger host, set only while checkAttachedTriggers evaluates it.
	relativeAmounts map[string]expr.Amount
	// endTurnCleanup is set when an EndTurn resolution began a Cleanup that
	// grants priority (CR 514.3a: a state-based action or trigger fired in
	// it), for Step to play that priority window and repeat the Cleanup
	// (PhaseHandler.java:447-449 sets bRepeatCleanup and givePriorityToPlayer
	// from endTurnByEffect's own onPhaseBegin). Step consumes it.
	endTurnCleanup bool
	// xctx is the X of the ability Registry.resolve is running, the root
	// ability's xManaCostPaid that Count$xPaid reads first
	// (AbilityUtils.java:1631); hasX false reads the source card's castX.
	xctx xContext
	// resolving is the ability in context: the one Registry.resolve is
	// running, or the one whose targets are being chosen
	// (inAbilityContext, targeting.go). The amounts and valid properties
	// that name its targets, triggering objects and paid cards read it
	// (amountcontext.go, valid.go); nil outside both.
	resolving *Ability
	// layering counts the continuous-effect passes running now
	// (applyContinuousLayers, which a refresh can enter again): while it is
	// non-zero a permanent's power is mid-rebuild, so handlePaid
	// (amountpaid.go) does not read power or toughness. Always zero between
	// calls, so Game.Clone leaves it zero rather than copying it.
	layering int
	// staticsHeld counts the state-based-action passes running now
	// (checkStateBasedActionsPass): while it is non-zero a sub-ability that a
	// replacement chain reaches mid-pass does not refresh the layers, Java's
	// setHoldCheckingStaticAbilities around the SBA's destruction
	// (GameAction.java:1588-1618, checked at :1100). Always zero between calls,
	// so Game.Clone leaves it zero rather than copying it.
	staticsHeld int
	// castPending is the spell whose cast is in progress and whose X has not
	// been announced yet, NoCard otherwise: Count$xPaid is unresolved for it.
	castPending CardID
	// preX is the X a spell announced before its targets (castspell.go
	// announceX), waiting for payManaCostX.
	preX    int
	hasPreX bool
	// holdingCastExits is set while a spell is being cast, and castExits
	// collects the cards that left a graveyard meanwhile -- the spell itself
	// cast from there, a Delve or ExileFromGrave cost -- so their
	// graveyard-origin ChangesZone triggers go on the stack with the spell's
	// SpellCast batch, above the spell, as Java holds them until the cast is
	// done (TriggerHandler.java:264, the stack frozen). Both are transient,
	// empty outside castSpellChosen, so Clone does not carry them (castPending
	// and preX neither).
	holdingCastExits bool
	castExits        []CardID
	// pendingErr is a static trigger's error from a site with no error
	// return (resolveStaticTriggers, statictrigger.go), waiting for the
	// nearest boundary that has one -- Registry.Resolve, ResolveStack,
	// PassPriority, or a caller's own TakePendingError (ADR-0020 decision
	// 4, GO-7). The first error is kept; nil when none is waiting.
	pendingErr error
	// replacing lists the replacement effects whose ReplaceWith$ ability is
	// resolving right now (ReplacementEffect.hasRun, ReplacementHandler.java:
	// 227-274): an event the substitute ability raises does not offer them
	// again. Never copied by Clone: nothing is mid-replacement across a clone.
	replacing []activeReplacement

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

	// restarted is Java's GameStage.RestartedByKarn (RestartGameEffect.java:105):
	// a RestartGame effect has reset the game mid-resolution and the turn
	// driver has stopped on it (ADR-0034). restartedBy is the activator,
	// whose first turn the restarted game begins with
	// (RestartGameEffect.java:107). Both cleared by ResumeAfterRestart
	// (driver.go), the caller's own obligation.
	restarted   bool
	restartedBy PlayerID

	// turn, activePlayer and activePhase are the turn structure: whose turn
	// it is, what step or phase it is in, and how many turns have passed.
	// Zero-valued (0, NoPlayer, Untap) until StartTurn, which is also a
	// coherent "the game has not started its turn structure yet" reading --
	// nothing downstream treats an unstarted game's phase as meaningful
	// without also checking activePlayer (turn.go).
	turn         int
	activePlayer PlayerID
	activePhase  PhaseType

	// castThisTurn is every spell cast this turn, in order, with who cast it
	// and from where (Count$ThisTurnCast_<valid>, castrecord.go); cleared when
	// a new turn starts.
	castThisTurn []castRecord
	// enteredThisTurn is every zone change this turn, as the card stood in
	// the zone it entered (a battlefield entry) or left (any other), and
	// countersAddedThisTurn every counter placement (castrecord.go); both
	// cleared at cleanup.
	enteredThisTurn       []zoneEntry
	countersAddedThisTurn []counterAddition
	// playerDamageThisTurn is every instance of damage dealt to a player this
	// turn, in order, with a last-known copy of its source (the player-target
	// half of Game.globalDamageHistory/damageThisTurnLKI, Game.java:1357-1360);
	// the wasDealt...ThisTurnBy player properties read it (playerproperty.go).
	// Cleared at cleanup (Game.clearGlobalDamageHistory).
	playerDamageThisTurn []playerDamage
	// sacrificedThisTurn is every player's Player.sacrificedThisTurn in one
	// list: a last-known copy of each permanent sacrificed this turn, in
	// order, as it stood on the battlefield (GameAction.sacrifice's
	// LastStateBattlefield copy), with the player who sacrificed it. Read by
	// the SacrificedThisTurn player measure; cleared at cleanup.
	sacrificedThisTurn []sacrificeRecord
	// snowAnyColor is set while a cast through MayPlaySnowIgnoreColor$ pays
	// its mana cost: snow mana pays a colored pip of any color
	// (ManaConversionMatrix.snowForColor, PlaySpellAbility.java:648-649).
	snowAnyColor bool

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
	// CheckStateBasedActions pass by pumpPT/pumpLayerKeywords (continuous.go) instead
	// -- cleared down to its own Permanent-only remainder every cleanupStep
	// (turn.go), CR 514.2's "until end of turn" effects wearing off.
	pumps []pumpRecord
	// endOfTurnCmds, endOfCombatCmds and endOfNextTurnCmds are the phase
	// command lists Java's EndOfTurn.addUntil, EndOfCombat.addUntil and
	// EndOfTurn.addUntilEnd/registerUntilEnd hold (controlcommands.go).
	endOfTurnCmds, endOfCombatCmds []cardCommand
	endOfNextTurnCmds              []playerCommand
	// pendingReturns are the Duration$ UntilHostLeavesPlay returns a host's
	// leaving has run but no controller has carried out yet
	// (returnUntilCards, controlcommands.go). Empty between steps.
	pendingReturns []cardCommand
	// layerAffected is CR 613.6's carried set for the layer pass in progress:
	// the cards each continuous static applied to in the first layer it did,
	// which later layers reuse (affectedPerAbility, GameAction.java:1119). A
	// key's presence also says the static has started applying.
	layerAffected map[layerKey][]CardID
	// animates is every Animate-shaped effect in force (animate.go):
	// Animate, AnimateAll, Debuff, Protection, ProtectionAll.
	animates []animateRecord
	// textWords and textBoxes are the ChangeText word changes and
	// ExchangeTextBox exchanges in force (textwords.go, ADR-0039); textMaps is
	// this pass's folded word map per card, and textMemo the rewritten
	// definitions built so far. The memo is a pure cache of immutable values,
	// rebuilt on demand, so Clone leaves it empty rather than share a map.
	textWords []textWordRecord
	textBoxes []textBoxRecord
	textMaps  map[CardID]wordMap
	textMemo  map[textMemoKey]*compile.Card
	// everyCreatureType is the type line holding every creature type of the
	// DB's vocabulary, built on first use (allCreatureTypes); a pure cache of
	// the immutable registry, so Clone leaves it unbuilt.
	everyCreatureType      cardtype.Line
	everyCreatureTypeBuilt bool
	// delayed is every registered delayed trigger (delayedtrigger.go), in
	// registration order.
	delayed []delayedTrigger
	// scheduled is every engine action waiting on a phase boundary
	// (scheduledaction.go, ADR-0030) -- ControlPlayer's grants and
	// revokes -- in scheduling order.
	scheduled []scheduledAction
	// ignores is every resolved IgnoreEffectCost$ ability: the players who
	// ignore a static until end of turn (ignoreeffect.go). Cleared at cleanup.
	ignores []ignoredEffect
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

	// mayPlay are this pass's MayPlay$ permissions (mayPlayGrant,
	// rulesmod.go), rebuilt from scratch by applyContinuousRules
	// (continuous.go) every state-based-action pass the way every other
	// continuous effect is, and read by CastSpell/PlayLand through
	// mayPlayOption.
	mayPlay []mayPlayGrant
	// mayPlayUses counts the casts this turn through each MayPlayLimit$
	// grant (castoptions.go).
	mayPlayUses map[mayPlayLimitKey]mayPlayUse

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

	// counterKinds interns the script-written CounterTypes a CounterChanged
	// event carried, in first-use order (counterDetailFor, event.go).
	counterKinds []CounterType
	// initiative is the player who has the initiative (CR 725,
	// Game.hasInitiative), NoPlayer while nobody does.
	initiative PlayerID

	// activePlane is Game.activePlanes (Game.java:80) narrowed to the one
	// card every real Java path drives it to (ADR-0029): the face-up plane
	// or phenomenon, NoCard while there is none. Its planar controller is
	// the player whose Command zone holds it (Card.ZoneOwner), so no Player
	// field mirrors Java's Player.currentPlanes.
	activePlane CardID
	// planechaseActive stands in for Java's activePlanes != null ("this is
	// a Planechase game", PlaneswalkEffect.java:23): NewCard sets it the
	// first time setup puts a card into any player's PlanarDeck, and
	// nothing unsets it (ADR-0029).
	planechaseActive bool
}

// pumpRecord is one resolved Pump effect's own contribution -- Defined$'s
// card, the amount and/or keywords it grants, and whether Duration$ named
// the literal value "Permanent" rather than the default "until end of turn."
type pumpRecord struct {
	Card             CardID
	Timestamp        uint64
	Power, Toughness int
	Keywords         []string
	// Hidden is KW$'s "HIDDEN " lines, prefix stripped
	// (Card.addHiddenExtrinsicKeywords), put on the card every rules pass
	// (pumpHiddenKeywords). Never edited in place: Clone shares it.
	Hidden    []string
	Switched  bool // KW$ HIDDEN switchPTKeyword: a Layer 7d switch
	Permanent bool
	// OnPlayer marks a keyword pump of a player (PumpEffect.applyPump(player),
	// Player.addChangedKeywords): Player holds the keywords, Card is NoCard.
	OnPlayer bool
	Player   PlayerID
	// UntilTurnOf ends the record as that player's next turn begins
	// (Duration$ UntilYourNextTurn); set only with HasUntil. Such a record is
	// Permanent so cleanup keeps it.
	HasUntil    bool
	UntilTurnOf PlayerID
}

// endPumpsUntilTurnOf ends every Duration$ UntilYourNextTurn pump p made, as
// p's turn begins (the untap-phase until list, PhaseHandler.java:252).
func (g *Game) endPumpsUntilTurnOf(p PlayerID) {
	kept := g.pumps[:0]
	for _, r := range g.pumps {
		if !r.HasUntil || r.UntilTurnOf != p {
			kept = append(kept, r)
		}
	}
	g.pumps = kept
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

// errRestartPending is the GO-7 stop for driving a game a RestartGame
// effect has restarted before its caller ran ResumeAfterRestart
// (driver.go, ADR-0034): the reset broke every invariant the driver,
// priority and stack loops read (turn count, opening hands, whose turn it
// is).
var errRestartPending = errors.New("engine: game restarted (RestartGame); call ResumeAfterRestart before driving it again")

// startingLife is CR 103.3's twenty: RegisteredPlayer.startingLife's own
// default (RegisteredPlayer.java:25), what Game's constructor hands every
// seat when no StartingLife was forced (Game.java:347-351). The variants
// that raise it (Commander, Archenemy, Vanguard, RegisteredPlayer.java:137-171)
// have no game setup in this port, so it is a constant for the same reason
// startingHandSize is. Subgame's seats start at it (subgameeffect.go).
const startingLife = 20

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
		g.players = append(g.players, Player{ID: id, Name: name, Life: startingLife, CrankCounter: 3})
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

// mayPlayLand is castOptions for a land outside pid's hand: a land has no
// cost or timing option to choose between, so any live grant with the zone
// permission lets pid play it.
func (g *Game) mayPlayLand(pid PlayerID, card CardID) bool {
	ts := g.Card(card).Timestamp
	for _, gr := range g.mayPlay {
		if gr.CardID == card && gr.Grantee == pid && gr.Timestamp == ts && gr.ZonePermission {
			return true
		}
	}
	return false
}

// recordPendingError keeps err for TakePendingError unless an earlier error
// is already waiting: the first failure is the one that explains the rest.
func (g *Game) recordPendingError(err error) {
	if g.pendingErr == nil {
		g.pendingErr = err
	}
}

// spellOptional is the optional-cost record a measurement taken for source
// reads without a CastSA> context: SpellAbility.isOptionalCostPaid on the
// root of the ability evaluating, which is the cast SpellAbility only while
// source is that spell -- being cast (castPending) or on the stack. A
// permanent's own abilities are never its cast SpellAbility and read nothing;
// CastSA> reaches the record there (castSARecord).
func (g *Game) spellOptional(source CardID) castOptional {
	if source == NoCard {
		return castOptional{}
	}
	if c := g.Card(source); source == g.castPending || c.Zone == Stack {
		return c.castOpt
	}
	return castOptional{}
}

// castSARecord is Card.getCastSA's optional-cost record for a CastSA>
// context (AbilityUtils.adjustTriggerContext): ok is false for a card that
// was not cast, whose context Java leaves unswitched, and the whole amount
// then reads 0 (AbilityUtils.java:4036-4040, :729).
func (g *Game) castSARecord(source CardID) (castOptional, bool) {
	if source == NoCard || !g.Card(source).wasCast {
		return castOptional{}, false
	}
	return g.Card(source).castOpt, true
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

// playersInTurnOrder is Java's getPlayersInTurnOrder(): Players() reversed
// when turn order has been reversed (ReverseTurnOrder), forward otherwise.
// Read by definedPlayers' own "Player"/"Opponent"/"Player.<property>"
// fallthrough cases (AbilityUtils.java:1188) -- rotation to start from the
// active player, if Java's own callers ever depend on that rather than just
// the direction, is not modeled here; no real corpus caller found needing it
// (defined.go).
func (g *Game) playersInTurnOrder() []PlayerID {
	out := g.Players()
	if !g.turnOrderReversed {
		return out
	}
	for i, j := 0, len(out)-1; i < j; i, j = i+1, j-1 {
		out[i], out[j] = out[j], out[i]
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
		// Card.java:202: every card starts as new to its controller, and
		// entered its zone this turn.
		cameUnderControl: true,
		enteredTurn:      g.turn,
	})
	g.put(id, zone, owner)
	if zone == Battlefield {
		// A fixture seating a Room on the battlefield: both doors locked
		// until the loader unlocks the ones it names.
		g.cards[id].enterRoom()
	}
	if zone == PlanarDeck {
		// Setup seating a planar deck is what makes this a Planechase
		// game (ADR-0029); real play only ever moves cards there.
		g.planechaseActive = true
	}
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

// counterDetailFor is the Detail CounterChanged carries for t in this game: the
// named constant, else t's interned open number. The first script-written type
// a game meets gets CounterDetailOpenBase, the next one CounterDetailOpenBase+1,
// and so on (Game.counterKinds, ADR-0009's arena pattern: interned per game in
// first-use order, so a replay of the same game numbers them the same).
func (g *Game) counterDetailFor(t CounterType) CounterDetail {
	if d, ok := counterDetail(t); ok {
		return d
	}
	for i, k := range g.counterKinds {
		if k == t {
			return CounterDetailOpenBase + CounterDetail(i)
		}
	}
	g.counterKinds = append(g.counterKinds, t)
	return CounterDetailOpenBase + CounterDetail(len(g.counterKinds)-1)
}

// CounterTypeOf is the CounterType a CounterChanged Detail names in this game,
// false for a Detail it never carried. The named constants decode too.
func (g *Game) CounterTypeOf(d CounterDetail) (CounterType, bool) {
	if d >= CounterDetailOpenBase {
		i := int(d - CounterDetailOpenBase)
		if i < len(g.counterKinds) {
			return g.counterKinds[i], true
		}
		return "", false
	}
	for _, t := range [...]CounterType{P1P1, M1M1, Loyalty, Defense, Charge, Stun, Shield, Poison, Energy} {
		if got, _ := counterDetail(t); got == d {
			return t, true
		}
	}
	return "", false
}

// emitCounterChanged emits CounterChanged for a change of delta counters of
// kind t on target, sourced from source. A script-written kind is interned
// (counterDetailFor), so no kind is dropped and none is mislabeled.
func emitCounterChanged(g *Game, source CardID, target EntityID, t CounterType, delta int) {
	g.sink.Emit(Event{Kind: CounterChanged, Source: source, Target: target, Amount: int32(delta), Detail: uint32(g.counterDetailFor(t))})
}

// traitHosts is every card of pid's whose static abilities and triggers are
// active: the battlefield's permanents, then pid's Command-zone cards that
// carry live traits -- effect cards, whose traits EffectEffect.java makes
// active there alone (setActiveZone(EnumSet.of(ZoneType.Command))), and
// schemes (CR 904.7's ongoing scheme stays face up in Command), whose
// printed S: lines and T: lines all name Command (EffectZone$/TriggerZones$,
// 21 real ongoing schemes). A plane or phenomenon is not walked: every mode
// planar play fires has its own planeswalkTriggerZones walk. With no such
// card it is the battlefield's own slice, so the common case allocates
// nothing.
func (g *Game) traitHosts(pid PlayerID) []CardID {
	bf := g.Zone(Battlefield, pid).Cards()
	cmd := g.Zone(Command, pid).Cards()
	live := 0
	for _, id := range cmd {
		if g.cards[id].isCommandTraitHost() {
			live++
		}
	}
	if live == 0 {
		return bf
	}
	out := make([]CardID, 0, len(bf)+live)
	out = append(out, bf...)
	for _, id := range cmd {
		if g.cards[id].isCommandTraitHost() {
			out = append(out, id)
		}
	}
	return out
}

// isCommandTraitHost reports whether c, sitting in the Command zone, has
// traits traitHosts must walk: an effect card or a scheme.
func (c *Card) isCommandTraitHost() bool {
	return c.IsEffect || (c.Def != nil && c.Type().Has(cardtype.Scheme))
}

// hostObjectStamp is the zoneStamp of host as the object an ability of
// it sees, and whether that object counts as in play, on the stack or in
// the Command zone (SpellAbilityEffect.java:1100-1104's own zone test).
// While host is on the battlefield, the stack or in Command that is its
// current object. Once it has left the battlefield it is its last
// battlefield object (Card.battlefieldStamp): Java's ability keeps the host
// object it was created on (CardProperty.java:401-407 reads the ability's
// getHostCard), which for a host that has since moved is the old object --
// a leaves-the-battlefield trigger's host, and RestartGame's own host once
// the restart has shuffled it into its library (restartgameeffect.go,
// ADR-0034). A host that never was on the battlefield is its current
// object, not in play.
//
// An Ability carries no host object of its own, so an ability a moved
// card's new object activates from its graveyard or hand reads the old
// battlefield object too, where Java would compare its new one: no real
// ExiledWithSource line reads the property from such an ability.
func (g *Game) hostObjectStamp(host CardID) (uint64, bool) {
	h := g.Card(host)
	switch h.Zone {
	case Battlefield, Stack, Command:
		return h.zoneStamp, true
	}
	if h.battlefieldStamp != 0 {
		return h.battlefieldStamp, true
	}
	return h.zoneStamp, false
}

// SetExiledWith records card as exiled with host's current object, the
// way GameState's own "ExiledWith:<id>" annotation does
// (GameState.java:771-781, which also lists card on host whatever zone
// host is in) -- fixture loading's tool, the same relationship
// SetTurnState has to real play. Real play sets it through an effect's
// own exile move (markExiledWith, zonemove.go).
func (g *Game) SetExiledWith(card, host CardID) {
	stamp, _ := g.hostObjectStamp(host)
	g.Card(card).exiledWith = exiledWithMark{host: host, stamp: stamp, listed: true}
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
//
// Returns the melded permanent's other card, NoCard if id was not one --
// the same card [Game.unmeld] then follows id into kind. A caller that
// marks id afterward (markExiledWith, zonemove.go) checks this return
// for the same reason SpellAbilityEffect.handleExiledWith takes an explicit
// "meld" card alongside its own moved one (SpellAbilityEffect.java:1087):
// unmeld's own move fires no effect-driven path of its own for the caller
// to hang a second mark on.
func (g *Game) Move(id CardID, kind ZoneType, owner PlayerID) CardID {
	if g.ceaseCopiedSpell(id) {
		return NoCard
	}
	c := g.Card(id)
	from := c.Zone
	before := *c
	g.clearTextRecords(id)
	if from == Exile && kind != Exile {
		c.foretold = false
	}
	if kind != Stack && kind != Battlefield {
		c.wasCast, c.castManaSpent, c.castSnow = false, 0, 0
	}
	if from == Stack && c.flashbackCast {
		// CR 702.34a, Flashback's replacement (CardFactoryUtil.java:2252:
		// Event$ Moved | Origin$ Stack | ExcludeDestination$ Exile): a spell
		// cast for its flashback cost is exiled wherever it would go.
		c.flashbackCast = false
		if kind != Exile {
			kind, owner = Exile, c.Owner
		}
	}
	if from == Stack && c.graveyardToExile {
		c.graveyardToExile = false
		if kind == Graveyard {
			kind, owner = Exile, c.Owner
		}
	}
	if (from == Stack && kind != Battlefield) || (from == Battlefield && kind != Battlefield) {
		c.kicker, c.castX, c.castOpt = 0, 0, castOptional{}
		c.sneaked, c.sneakDefender = false, NoEntity
	}
	if from == Stack && kind != Battlefield {
		// CR 108.4a: only a permanent or a spell has a controller. A spell
		// leaving the stack for anywhere but the battlefield goes back to
		// its owner's control -- the other half of castSpell's CR 110.2
		// "the caster controls it" (castspell.go).
		c.controller = c.Owner
		// ControlSpell's temporary controller goes too: GameAction.java:651-654
		// clears every controller of a card leaving for a zone without one.
		c.tempControllers = nil
	}
	isPermanent := c.Type().IsPermanent()
	leftStamp := c.zoneStamp
	g.Zone(c.Zone, c.ZoneOwner).remove(id)
	g.put(id, kind, owner)
	if from != kind {
		c.zoneEntered, c.zoneEntryTurn, c.zoneEntryFrom = true, g.turn, from
	}

	// A card exiled face down (Heist) turns face up as it leaves exile.
	if from == Exile && kind != Exile {
		c.turnFaceUp()
	}
	// A zone change always drops continuous-effect grants from the zone left
	// behind: Java gives every zone's own card fresh characteristics
	// (Layers 4/5/6, continuouslayers.go's own AffectedZone$ reach can now
	// write these off the battlefield too). Leaving the battlefield already
	// clears these below as part of its own broader reset; this covers
	// entering it (a reanimated permanent must not keep its graveyard's
	// grants) and every other-zone-to-other-zone move (an AffectedZone$ Hand
	// grant must not ride along into the graveyard or stack).
	if from != kind {
		c.TypeMod.Clear()
		c.ColorMod.Clear()
		c.KeywordMod.Clear()
	}
	// melded is the other card of a melded permanent leaving the battlefield
	// (CR 712.4c), split off after this move's own event, below.
	melded := NoCard
	switch {
	case from == Battlefield && kind != Battlefield:
		c.battlefieldStamp = leftStamp
		snap := *c
		g.lki[id] = &snap
		// After the snapshot: last-known information keeps a phased-out
		// card's own phasing (GameAction.java:977 reads lki.isPhasedOut()).
		c.phasedOut, c.directlyPhasedOut, c.wontPhaseInNormal = NoPlayer, false, false
		melded = c.leaveMeld()
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
		c.blockedByThisTurn = nil
		c.Suspected, c.Solved, c.Harnessed = false, false, false
		c.renowned = false
		// Layer 3's text change ends with the object (CR 400.7), before the
		// copy and face-down restores below read or replace Def.
		c.clearTextChange()
		g.endCopiesOnLeave(id)
		c.Sprocket = 0
		c.Sector = ""
		c.turnFaceUp()
		c.turnFrontFaceUp()
		c.leaveRoom()
		g.dropPreventShields(id)
		g.Unattach(id)
		g.clearPumps(id)
		g.clearAnimates(id)
		g.loseRingBearer(id)
		g.unpairOnLeave(id)
	case from == Stack && kind != Stack && kind != Battlefield:
		// A Room spell that does not resolve into a permanent goes back to
		// its printed split card.
		c.leaveRoom()
	case from != Battlefield && kind == Battlefield:
		c.enterRoom()
		c.SummonSick = true
		c.cameUnderControl, c.enteredTurn = true, g.turn
		// A Mode$ CantPutCounter static stops the printed loyalty and defense
		// counters too (Card.canReceiveCounters, the "etbCounter" replacement's
		// own placement).
		if loyalty, ok := c.BaseLoyalty(); ok && c.Type().Has(cardtype.Planeswalker) && !g.cantPutCounter(CardEntity(id), Loyalty) {
			c.Counters.Add(Loyalty, loyalty)
			emitCounterChanged(g, id, CardEntity(id), Loyalty, loyalty)
		}
		if defense, ok := c.BaseDefense(); ok && c.Type().Has(cardtype.Battle) && !g.cantPutCounter(CardEntity(id), Defense) {
			c.Counters.Add(Defense, defense)
			emitCounterChanged(g, id, CardEntity(id), Defense, defense)
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
		g.Player(owner).descended++
	}
	g.recordEntered(c, &before, from, kind)

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
	if melded != NoCard {
		g.unmeld(melded, kind, owner, false)
	}
	return melded
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
// skips it. Returns the melded partner it unmelds along with id, same as
// [Game.Move].
func (g *Game) MoveToLibraryTop(id CardID, owner PlayerID) CardID {
	if g.ceaseCopiedSpell(id) {
		return NoCard
	}
	c := g.Card(id)
	from := c.Zone
	before := *c
	g.clearTextRecords(id)
	if from == Stack {
		c.controller = c.Owner
		c.tempControllers = nil
	}
	leftStamp := c.zoneStamp
	g.Zone(c.Zone, c.ZoneOwner).remove(id)
	g.putFront(id, owner)
	if from != Library {
		c.zoneEntered, c.zoneEntryTurn, c.zoneEntryFrom = true, g.turn, from
	}

	melded := NoCard
	if from == Battlefield {
		c.battlefieldStamp = leftStamp
		// Last-known information, as Move freezes it: a leaves trigger
		// (checkLeftToTriggers) reads the state the permanent left in, never
		// an earlier departure's snapshot.
		snap := *c
		g.lki[id] = &snap
		c.phasedOut, c.directlyPhasedOut, c.wontPhaseInNormal = NoPlayer, false, false
		melded = c.leaveMeld()
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
		c.blockedByThisTurn = nil
		c.Suspected, c.Solved, c.Harnessed = false, false, false
		// Layer 3's text change ends with the object (CR 400.7), before the
		// copy and face-down restores below read or replace Def.
		c.clearTextChange()
		g.endCopiesOnLeave(id)
		c.Sprocket = 0
		c.Sector = ""
		c.turnFaceUp()
		c.turnFrontFaceUp()
		c.leaveRoom()
		g.dropPreventShields(id)
		g.Unattach(id)
		g.clearPumps(id)
		g.clearAnimates(id)
		g.loseRingBearer(id)
		g.unpairOnLeave(id)
	}
	if from == Stack {
		c.leaveRoom()
	}
	g.recordEntered(c, &before, from, Library)

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
	if melded != NoCard {
		g.unmeld(melded, Library, owner, true)
	}
	return melded
}

// leaveMeld clears c's side of a meld as c leaves the battlefield and
// returns the other card its melded permanent represented, NoCard if c was
// not one. A secondary moved on its own drops its Melded mark here too, so
// the unmeld that follows its melded permanent finds it gone and leaves it
// where it is.
func (c *Card) leaveMeld() CardID {
	c.Melded = false
	melded := c.MeldedWith
	c.MeldedWith = NoCard
	return melded
}

// unmeld is GameAction.changeZone's "Spin off Melded card"
// (GameAction.java:629-642, CR 712.4c): a melded permanent just left the
// battlefield for kind, so the other card it represented follows it to the
// same zone, after it -- appended after it, or with top put on the library's
// top over it, where Java's changeZone(null, zoneTo, unmeld, position, ...)
// with the melded permanent's own position puts it. Java moves it from no
// zone at all (zoneFrom null): it never was in the battlefield's card list
// (PlayerZoneBattlefield.addToMelded), so it gets no leave-the-battlefield
// cleanup, no last-known information and no effect-card watch -- its state
// was already cleared when meldeffect.go exiled it -- only the zone change
// itself and its event.
func (g *Game) unmeld(secondary CardID, kind ZoneType, owner PlayerID, top bool) {
	s := g.Card(secondary)
	if !s.Melded {
		return
	}
	s.Melded = false
	if top {
		g.putFront(secondary, owner)
	} else {
		g.put(secondary, kind, owner)
	}
	g.sink.Emit(Event{
		Kind:   ZoneChanged,
		Phase:  g.activePhase,
		Active: g.activePlayer,
		Actor:  owner,
		Turn:   uint16(g.turn),
		Source: secondary,
		From:   Battlefield,
		To:     kind,
	})
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
// does not remove the card from wherever it was, so only [Game.Move],
// [Game.NewCard] and unmeld (a melded pair splitting apart, ADR-0032) may
// call it. The ExiledWithSource mark clears on every move but one to the
// Stack (GameAction.java:576-579's own "if (!zoneTo.is(Stack))" guard on its
// cleanupExiledWith call) -- a card cast or activated straight out of the
// exile an ability put it in keeps the mark while its own spell/ability is
// on the stack, the one zone this port's callers ever move an exiled,
// marked card into without going through this clearing branch first
// (castspell.go's own g.Move(card, Stack, pid)).
func (g *Game) put(id CardID, kind ZoneType, owner PlayerID) {
	c := &g.cards[id]
	c.Zone, c.ZoneOwner = kind, owner
	c.activations = nil
	c.trigActs = nil
	c.trigResolved = nil
	g.timestamp++
	c.Timestamp, c.zoneStamp = g.timestamp, g.timestamp
	if kind != Stack {
		c.exiledWith = exiledWithMark{}
	}
	g.Zone(kind, owner).cards.Add(id)
}

// putFront is put's mirror for the library's own top instead of a zone's
// end: it does not remove the card from wherever it was, so only
// [Game.MoveToLibraryTop] and unmeld may call it.
func (g *Game) putFront(id CardID, owner PlayerID) {
	c := &g.cards[id]
	c.Zone, c.ZoneOwner = Library, owner
	c.activations = nil
	c.trigActs = nil
	c.trigResolved = nil
	g.timestamp++
	c.Timestamp, c.zoneStamp = g.timestamp, g.timestamp
	c.exiledWith = exiledWithMark{}
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
	// CR 613.7e: an Aura, Equipment or Fortification gets a new timestamp each
	// time it becomes attached (Card.java:3938), so its continuous effects
	// order after what existed before.
	g.timestamp++
	a.Timestamp = g.timestamp
	a.attachedTo = host
	if h.attachments == nil {
		h.attachments = collect.NewOrderedSet[CardID](2)
	}
	h.attachments.Add(attachment)
}

// AttachToPlayer is Attach for a player host (Card.attachToEntity with a
// Player, CR 303.4h): an Aura with "Enchant player" enchants the player until
// it leaves the battlefield or the player leaves the game. Like Attach it
// detaches from the previous host first and restamps (CR 613.7e).
func (g *Game) AttachToPlayer(attachment CardID, host PlayerID) {
	a := g.Card(attachment)
	g.Unattach(attachment)
	g.timestamp++
	a.Timestamp = g.timestamp
	a.attachedPlayer = host
	p := g.Player(host)
	if p.attachments == nil {
		p.attachments = collect.NewOrderedSet[CardID](2)
	}
	p.attachments.Add(attachment)
}

// Unattach detaches a card from whatever it is attached to. Detaching an
// unattached card is a no-op, because the callers that clean up after a zone
// change do not track whether there was anything to clean.
func (g *Game) Unattach(attachment CardID) {
	a := g.Card(attachment)
	if a.attachedPlayer != NoPlayer {
		if p := g.Player(a.attachedPlayer); p.attachments != nil {
			p.attachments.Remove(attachment)
		}
		a.attachedPlayer = NoPlayer
		g.runUnattachCommands(nil, attachment)
		return
	}
	if a.attachedTo == NoCard {
		return
	}
	if h := g.Card(a.attachedTo); h.attachments != nil {
		h.attachments.Remove(attachment)
	}
	a.attachedTo = NoCard
	// Card.unattachFromEntity ends with runUnattachCommands (Card.java:3995).
	g.runUnattachCommands(nil, attachment)
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
		restarted:    g.restarted,
		restartedBy:  g.restartedBy,
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
		nextStackItemID:   g.nextStackItemID,
		combat:            g.combat.clone(),
		pumps:             append([]pumpRecord(nil), g.pumps...),
		endOfTurnCmds:     append([]cardCommand(nil), g.endOfTurnCmds...),
		endOfCombatCmds:   append([]cardCommand(nil), g.endOfCombatCmds...),
		endOfNextTurnCmds: append([]playerCommand(nil), g.endOfNextTurnCmds...),
		pendingReturns:    append([]cardCommand(nil), g.pendingReturns...),

		animates:  append([]animateRecord(nil), g.animates...),
		textWords: append([]textWordRecord(nil), g.textWords...),
		textBoxes: append([]textBoxRecord(nil), g.textBoxes...),
		textMaps:  maps.Clone(g.textMaps),
		delayed:   append([]delayedTrigger(nil), g.delayed...),
		scheduled: append([]scheduledAction(nil), g.scheduled...),
		ignores:   cloneIgnores(g.ignores),
		skips:     append([]skipPhase(nil), g.skips...),

		extraTurns:            append([]PlayerID(nil), g.extraTurns...),
		combatDamagePrevented: g.combatDamagePrevented,
		combatsThisTurn:       g.combatsThisTurn,
		skipDamageSteps:       g.skipDamageSteps,
		turnOrderReversed:     g.turnOrderReversed,
		preventShields:        append([]preventShield(nil), g.preventShields...),
		exileGrants:           append([]ExilePlayGrant(nil), g.exileGrants...),
		mayPlay:               append([]mayPlayGrant(nil), g.mayPlay...),
		mayPlayUses:           cloneMayPlayUses(g.mayPlayUses),
		castThisTurn:          append([]castRecord(nil), g.castThisTurn...),
		enteredThisTurn:       append([]zoneEntry(nil), g.enteredThisTurn...),
		countersAddedThisTurn: append([]counterAddition(nil), g.countersAddedThisTurn...),
		playerDamageThisTurn:  append([]playerDamage(nil), g.playerDamageThisTurn...),
		sacrificedThisTurn:    append([]sacrificeRecord(nil), g.sacrificedThisTurn...),
		snowAnyColor:          g.snowAnyColor,
		dayTime:               g.dayTime,
		previousPlayer:        g.previousPlayer,
		previousPlayerSpells:  g.previousPlayerSpells,
		lki:                   make(map[CardID]*Card, len(g.lki)),
		monarch:               g.monarch,
		counterKinds:          append([]CounterType(nil), g.counterKinds...),
		monarchBeginTurn:      g.monarchBeginTurn,
		initiative:            g.initiative,
		activePlane:           g.activePlane,
		planechaseActive:      g.planechaseActive,
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
		if g.players[i].attachments != nil {
			out.players[i].attachments = g.players[i].attachments.Clone()
		}
		out.players[i].Rules = g.players[i].Rules.clone()
		out.players[i].KeywordMod = g.players[i].KeywordMod.clone()
		if g.players[i].completedDungeons != nil {
			out.players[i].completedDungeons = append([]CardID(nil), g.players[i].completedDungeons...)
		}
		out.players[i].controlledBy = append([]controlGrant(nil), g.players[i].controlledBy...)
		out.players[i].attackedThisTurn = append([]CardID(nil), g.players[i].attackedThisTurn...)
		out.players[i].attackedPlayersThisTurn = append([]PlayerID(nil), g.players[i].attackedPlayersThisTurn...)
		out.players[i].attackedPlayersLastTurn = append([]PlayerID(nil), g.players[i].attackedPlayersLastTurn...)
	}

	copy(out.cards, g.cards)
	for i := range out.cards {
		c := &out.cards[i]
		c.Counters = g.cards[i].Counters.clone()
		c.Memory = g.cards[i].Memory.clone()
		c.detainedBy = append([]PlayerID(nil), g.cards[i].detainedBy...)
		c.goadedBy = append([]goad(nil), g.cards[i].goadedBy...)
		c.mustBlock = append([]mustBlockReq(nil), g.cards[i].mustBlock...)
		c.hiddenKeywords = append([]string(nil), g.cards[i].hiddenKeywords...)
		c.blockedByThisTurn = append([]CardID(nil), g.cards[i].blockedByThisTurn...)
		c.activations = g.cards[i].activations.clone()
		c.trigActs = g.cards[i].trigActs.clone()
		c.trigResolved = g.cards[i].trigResolved.clone()
		c.PT = g.cards[i].PT.clone()
		c.TypeMod = g.cards[i].TypeMod.clone()
		c.ColorMod = g.cards[i].ColorMod.clone()
		c.KeywordMod = g.cards[i].KeywordMod.clone()
		c.ControlMod = g.cards[i].ControlMod.clone()
		c.tempControllers = append([]ControlEffect(nil), g.cards[i].tempControllers...)
		c.Damage.Sources = append([]CardID(nil), g.cards[i].Damage.Sources...)
		c.leavesPlayCmds = append([]cardCommand(nil), g.cards[i].leavesPlayCmds...)
		c.untapCmds = append([]cardCommand(nil), g.cards[i].untapCmds...)
		c.changeControllerCmds = append([]cardCommand(nil), g.cards[i].changeControllerCmds...)
		c.phaseOutCmds = append([]cardCommand(nil), g.cards[i].phaseOutCmds...)
		c.unattachCmds = append([]cardCommand(nil), g.cards[i].unattachCmds...)
		c.staticCmds = append([]staticCheck(nil), g.cards[i].staticCmds...)
		c.copies = append([]copyEffect(nil), g.cards[i].copies...)
		c.grants = append([]grantedTriggers(nil), g.cards[i].grants...)
		c.castOpt = g.cards[i].castOpt.clone()
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
		s.grants = append([]grantedTriggers(nil), snap.grants...)
		if snap.attachments != nil {
			s.attachments = snap.attachments.Clone()
		}
		out.lki[id] = &s
	}
	return out
}

// xContext is the X announced for the ability being resolved: the value and
// whether one was announced (SpellAbility.getXManaCostPaid's null).
type xContext struct {
	value int
	has   bool
	// source is the card the ability belongs to: the X is that ability's, not
	// that of a card an effect of it puts onto the battlefield.
	source CardID
}

// castRecord is one spell cast: the card, its caster (who controlled it on
// the stack) and the zone it was cast from.
type castRecord struct {
	card       CardID
	controller PlayerID
	from       ZoneType
}

// zoneEntry is one card's zone change this turn (Zone.cardsAddedThisTurn): the
// card as a last-known copy, so its controller and characteristics are those
// it had in the zone it entered (a battlefield entry) or left (any other,
// Zone.add's latestState), and the zones it moved between.
type zoneEntry struct {
	card     Card
	from, to ZoneType
}

// playerDamage is one instance of damage dealt to a player
// (CardDamageHistory.registerDamage's Pair plus Game.damageThisTurnLKI's
// source copy): the source as it stood when it dealt the damage, the player
// dealt it, how much and whether it was combat damage.
type playerDamage struct {
	source Card
	target PlayerID
	amount int
	combat bool
}

// sacrificeRecord is one entry of Game.sacrificedThisTurn: who sacrificed
// the permanent and the permanent as last known on the battlefield.
type sacrificeRecord struct {
	player PlayerID
	card   Card
}

// counterAddition is one placement in Game.countersAddedThisTurn: who put how
// many counters of which kind on which card, the card as a last-known copy.
type counterAddition struct {
	counter CounterType
	putter  PlayerID
	card    Card
	n       int
}

// cloneMayPlayUses copies the counter map for Game.Clone.
func cloneMayPlayUses(m map[mayPlayLimitKey]mayPlayUse) map[mayPlayLimitKey]mayPlayUse {
	if m == nil {
		return nil
	}
	out := make(map[mayPlayLimitKey]mayPlayUse, len(m))
	for k, v := range m {
		out[k] = v
	}
	return out
}

// Kinds of ability a payment is made for, what a static's ValidCause$ is matched
// against (causeNone: not a spell or ability this port classifies).
const (
	causeNone      = ""
	causeSpell     = "Spell"
	causeActivated = "Activated"
	causeManaAbil  = "ManaAbility"
	causeTriggered = "Triggered"
)

// causeKind is what kind of spell or ability a is, the vocabulary a static's
// ValidSA$/ValidCause$ is matched against: a spell on the stack, a trigger
// (isTrigger), or otherwise an activated ability.
func (a *Ability) causeKind() string {
	switch {
	case a.spell:
		return causeSpell
	case a.isTrigger:
		return causeTriggered
	default:
		return causeActivated
	}
}

// layerKey names one static ability on one host within a layer pass.
type layerKey struct {
	host CardID
	s    *compile.Ability
}
