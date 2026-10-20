// Cards. Identity and location here; the mutable detail is split out by
// concern (ADR-0009).

package engine

import (
	"slices"
	"sort"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb"
	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/pkg/collect"
)

// castSnapshot is the card IDs on the battlefield and in the graveyards at the
// moment a spell was cast.
type castSnapshot struct {
	set                    bool
	battlefield, graveyard []CardID
}

// untapNextStepKeyword is the hidden line Card.canUntap reads
// (Card.java:4817) and Untap.doUntap removes once read (Untap.java:160).
const untapNextStepKeyword = "This card doesn't untap during your next untap step."

// Card is one card in one game.
//
// It holds no back-reference: no *Game, no *Player. Its controller is a
// PlayerID and its attachments are CardIDs, so a game copies with a slice copy
// and nothing has to be remapped (ADR-0009). Every operation that needs the
// rest of the game takes *Game as a parameter.
type Card struct {
	// ID is this card's own handle, so a Card passed by value still knows what
	// it is.
	ID CardID
	// Def is the compiled script, shared and immutable across every game in
	// the process (ADR-0007). Never nil for a real card.
	Def *compile.Card
	// Owner never changes. Controller (the method below) does, and the two
	// differ whenever something has taken control of the card.
	Owner      PlayerID
	controller PlayerID
	// IsToken marks a card a Token effect created (CR 111.1): it ceases to
	// exist once it is anywhere but the battlefield (CR 704.5d, action.go).
	IsToken bool

	// flashbackCast marks a spell cast from a graveyard for its flashback cost:
	// any time it would leave the stack it is exiled instead (CR 702.34a,
	// Game.Move). Cleared when it leaves.
	flashbackCast bool
	// graveyardToExile marks a spell cast through a MayPlay$ grant with
	// ReplaceGraveyard$ Exile: leaving the stack for a graveyard it is exiled
	// instead. Cleared when it leaves the stack.
	graveyardToExile bool
	// castFrom is the zone the card was last cast from (castrecord.go), and
	// castBy who cast it (getCastSA().getActivatingPlayer(), read by
	// Count$wasCastFrom...ByYou); both meaningful only while wasCast holds.
	castFrom ZoneType
	castBy   PlayerID
	// wasCast is Card.wasCast (castFrom != null): the card was cast and has not
	// changed zones since, except from the stack onto the battlefield. A cast
	// that failed clears it; Game.Move clears it on every other zone change
	// (CR 400.7: the card is a new object). castManaSpent is the mana paid for
	// that cast (SpellAbility.getTotalManaSpent), 0 for a free one.
	wasCast bool
	// castLast is the battlefield and graveyards as the spell was put on the
	// stack (SpellAbility.getLastStateBattlefield/Graveyard of its cast
	// ability), what Count$LastState...WithFallback counts. The slices are
	// never changed once set.
	castLast castSnapshot
	// damageDealtThisTurn is the damage this card dealt this turn after
	// prevention and replacement (CardDamageHistory.getDamageDoneThisTurn, read
	// by Count$TotalDamageDoneByThisTurn); cleared with every card's turn counters.
	damageDealtThisTurn int
	castManaSpent       int
	// castPaid is the colored mana that cast spent, white to green
	// (SpellAbility.getPayingMana by color), read by Count$Converge and
	// Count$Adamant; meaningful while wasCast holds. castSnow is how much of
	// that cast's mana was snow mana (xAnnounced.snow), read by
	// Count$CastTotalManaSpent Snow; cleared with castManaSpent.
	castPaid [5]int
	castSnow int
	// castColorless is the colorless mana ({C}) that cast spent, castPaid's
	// sixth bucket kept apart so Count$Converge, which counts castPaid's
	// colors, never counts it: Count$Adamant.Colorless and
	// Count$EachSpentToCast.C read it.
	castColorless int
	// castPaidSources is the source of each unit of mana that cast spent from
	// the caster's pool (xAnnounced.sources, ADR-0042), read by
	// Count$CastTotalManaSpent <valid>; cleared with castManaSpent. Replaced,
	// never written in place, so clones may share it.
	castPaidSources []manaSource
	// prepared is the exiled copy of this creature's prepare spell while it is
	// prepared (Card.preparedEffect's remembered card, prepare.go), NoCard
	// otherwise. preparedFrom, on that copy, is the creature it was made for:
	// the copy is in its PreparedSpell state for as long as it exists.
	prepared     CardID
	preparedFrom CardID
	// foretold marks a card exiled by the Foretell special action, and
	// foretoldTurn the turn it happened (foretell.go).
	foretold     bool
	foretoldTurn int

	// kicker is which of the card's kicker costs were paid casting it (kicker.go),
	// read while it is on the stack or the battlefield; Move clears it when the
	// card leaves either for another zone.
	kicker uint8
	// castX is the X its caster announced casting it (Card.getXManaCostPaid
	// over getCastSA), kept while it is on the stack or the battlefield, 0
	// for a card cast without an X or never cast; Move clears it with kicker.
	castX int
	// sneaked is getCastSA().isSneak(): the card was cast for its Sneak cost
	// (sneak.go), read as Card.sneaked while it is on the stack or the
	// battlefield. sneakDefender is what the returned attacker was attacking,
	// which a sneaked creature attacks as it enters (PermanentEffect.java:55-63).
	// Move clears both with kicker.
	sneaked       bool
	sneakDefender EntityID
	// castOpt is what the card's cast paid of its optional costs beyond Kicker
	// (optionalcost.go): the OptionalCost kinds and the paid lists its effects
	// read back. Kept and cleared with kicker.
	castOpt castOptional
	// cameUnderControl is Card.cameUnderControlSinceLastUpkeep (Echo): true
	// from entering the battlefield or changing controller until the end of
	// its controller's next upkeep. enteredTurn is the turn it entered the
	// battlefield (Card.getTurnInZone), which keeps that upkeep from clearing
	// the flag on the turn the permanent arrived.
	cameUnderControl bool
	enteredTurn      int
	// embalmed is Card.isEmbalmed: the token Embalm makes. Embalm is not
	// ported, so nothing sets it; Clone's Embalm$ reads it (CardFactory.java:555).
	embalmed bool
	// zoneEntered, zoneEntryTurn and zoneEntryFrom record how the card last
	// entered the zone it sits in by a move: the turn and the zone it came from
	// (Zone.cardsAddedThisTurn and enteredFromThisTurn, Zone.java:120-127), read
	// by the ThisTurnEnteredFrom_<Zone> property. A card NewCard placed has
	// none.
	zoneEntered   bool
	zoneEntryTurn int
	zoneEntryFrom ZoneType
	// renowned is Card.isRenowned (CR 702.112): a Renown creature that has
	// had its counters put on it; Move clears it when the card leaves the
	// battlefield.
	renowned bool
	// monstrous is Card.isMonstrous (CR 701.31): a Monstrosity ability has
	// resolved for it; cleared when the card leaves the battlefield like
	// renowned.
	monstrous bool
	// IsCopiedSpell marks the card a copy of a spell lives on
	// (GamePieceType.COPIED_SPELL, CardFactory.copySpellHost): it exists
	// only on the stack and ceases to exist the moment anything would move
	// it (CR 707.10a, ceaseCopiedSpell, game.go) -- unless it is a
	// permanent spell resolving, which makes it a token instead (CR 111.11,
	// permanentEffect, castspell.go).
	IsCopiedSpell bool
	// IsEffect marks an effect card (GamePieceType.EFFECT): the Command-zone
	// object an Effect ability creates to carry its triggers, continuous
	// effects and replacement effects (effecteffect.go). Its traits are
	// active in the Command zone only, whatever zones their own script text
	// names (EffectEffect.java's setActiveZone(EnumSet.of(ZoneType.Command))).
	IsEffect bool
	// effectLife is when an effect card stops existing: its Duration$ and
	// its ExileOnMoved$/ForgetOnMoved$ watch. Zero on every other card.
	effectLife effectLifetime
	// isBoon is Card.isBoon: a Boon$ effect card, which leaves the Command zone
	// after its triggers have fired; boons holds the BoonAmount$ ones.
	isBoon bool
	boons  []boonTrigger
	// CurrentRoom is the room a dungeon's venture marker is on (CR 309.4,
	// Card.currentRoom), its RoomName$; empty before the first venture and
	// on every card that is not a dungeon (ventureeffect.go).
	CurrentRoom string
	// basePower/baseToughness replace the printed value when set --
	// TokenPower$/TokenToughness$ (TokenInfo.getProtoType's setBasePower).
	basePower, baseToughness       int
	hasBasePower, hasBaseToughness bool
	// svars are SVars an effect set at runtime (StoreSVar's
	// Card.setSVar(key, "Number$N")), keyed lower-case; each shadows the
	// script's own SVar of that name for resolveNamedAmount. A card's SVars
	// survive its zone changes, as Java's do.
	svars map[string]int
	// Zone is where the card is. The zone's own list is the ordering
	// authority; this is the reverse index, kept in step by the move
	// operations.
	Zone      ZoneType
	ZoneOwner PlayerID
	// Timestamp orders continuous effects and is what breaks ties between
	// otherwise simultaneous events. Assigned from the game's counter on every
	// zone change, never reused.
	Timestamp uint64
	// zoneStamp is Java's gameTimestamp (Card.java:274): set only as the
	// card enters a zone (put/putFront, game.go; GameAction.java:370), so
	// unlike Timestamp a transform does not change it. It is the object
	// identity CR 608.2b's target re-check compares (targetStillLegal,
	// targeting.go; CR 400.7).
	zoneStamp uint64

	// The mutable detail, split by concern rather than flattened onto Card:
	// 8,105 lines of Java's Card has to land somewhere, and these are the
	// parts with their own invariants (ADR-0009).
	Counters   Counters
	Damage     Damage
	Memory     Memory
	PT         PT
	TypeMod    TypeMod
	ColorMod   ColorMod
	KeywordMod KeywordMod
	ControlMod ControlMod

	// Tapped, SummonSick and Exerted are battlefield state every permanent
	// carries that is not "how much of something" -- everything else that
	// shape (Renowned, Monstrous, and the rest of GameState's per-card
	// annotation grammar) waits on the mechanic that reads it, which is
	// card-type-specific and not built yet
	// (porting/port-log/game-state-fixture.md). PhasedOut is phasedOut,
	// below.
	Tapped     bool
	SummonSick bool

	// phasedOut is Card.phasedOut (CR 702.26): NoPlayer while the permanent
	// is phased in, else the player whose untap step phases it back in --
	// its controller as it phased out (Card.java:5645), read back by
	// isPhasedOut(Player) (Card.java:5575). Game.setPhasedOut is the only
	// writer and keeps the zone's own phased-out subset in step with it
	// (zone.go, ADR-0021). directlyPhasedOut is false for a permanent that
	// phased out only because what it is attached to did (CR 702.26g,
	// Card.phase's direct flag), and wontPhaseInNormal is Phases'
	// WontPhaseInNormal$: the untap step does not phase it back in
	// (Card.switchPhaseState's first check).
	phasedOut         PlayerID
	directlyPhasedOut bool
	wontPhaseInNormal bool
	// Exerted is CR 701.42a's own marker (Card.exertedByPlayer in Java,
	// collapsed from a per-player set to a single bool -- this port's own
	// activation-cost caller is always the card's own controller, and
	// control does not realistically change between exerting and the
	// exerting player's own next untap step for any real corpus card).
	// untapStep (turn.go) reads it to skip untapping (Card.untap's own
	// "isExertedBy(phase)" early return) and clears it unconditionally
	// every untap step regardless, the identical unconditional-clear
	// Untap.java's own separate "remove exerted flags from all things in
	// play" pass already has.
	Exerted bool

	// Sprocket is the Contraption's own dial (Unfinity's "assemble the
	// Contraption," CR 725): 1, 2 or 3, chosen (AssembleContraptionEffect.java's
	// own chooseSprocket) the moment the Contraption enters the battlefield
	// via assembleContraptionEffect.go, and read by nothing else this port
	// builds yet -- the Crank/sprocket-triggered abilities a Contraption's own
	// Sprocket$ line names are a further mechanic (AssembleContraption$ lands,
	// AdvanceCrank$ does not). Zero for every card that has never been
	// assembled, cleared on leaving the battlefield (Game.Move) exactly as
	// Tapped/Exerted are, since a Contraption reassembled later rolls a fresh
	// dial rather than remembering its last one (CR 725.4a).
	Sprocket int

	// Sector is Card.sector (Card.java:321): the "Alpha", "Beta" or "Gamma"
	// sector CR 704.5u's state-based action (assignSectors, action.go) gives
	// a creature while some permanent has Space sculptor, "" until then.
	// Cleared on leaving the battlefield (Game.Move): Java builds a new Card
	// object for the new zone and never copies the field.
	Sector string

	// AttacksThisTurn is CardDamageHistory.getCreatureAttacksThisTurn's own
	// per-card counter -- incremented once per combat this card is declared
	// an attacker in (DeclareCombatAttackers, attack.go), reset every cleanup
	// (cleanupStep, turn.go) alongside Damage/LandsPlayed/CardsDrawnThisTurn.
	// TriggerAttacks' own FirstAttack$ (trigger.go) is the one reader: "this
	// is the first time this creature has attacked this turn," true exactly
	// when this reads 1 at the moment its own trigger fires, immediately
	// after the increment.
	AttacksThisTurn int

	// attackedThisTurn is CardDamageHistory.attackedThisTurn
	// (CardDamageHistory.java:28, :59-72): the entity -- player, planeswalker
	// or battle -- this card was declared attacking, once per declaration
	// (recordAttacked, castrecord.go). Cleared every cleanup with
	// AttacksThisTurn (CardDamageHistory.newTurn, :287-288, from
	// Card.onCleanupPhase) and on leaving the battlefield, where Java builds a
	// new Card. Read by the attackedYouThisTurn property (CardProperty.java:
	// 1193-1196, valid.go).
	attackedThisTurn []EntityID

	// BecameTargetThisTurn is Card.hasBecomeTargetThisTurn's own flag --
	// AttacksThisTurn's own boolean sibling, since Mode$ BecomesTarget's own
	// FirstTime$ (trigger.go's checkBecomesTargetTriggers) only ever asks
	// "has anyone targeted this card yet this turn," never how many times or
	// by whom (Java's own targetedFromThisTurn is a Player set, but nothing
	// this port resolves reads which players are in it, only whether it is
	// empty). Set the moment this card is first targeted
	// (checkBecomesTargetTriggers), reset every cleanup (cleanupStep,
	// turn.go) alongside AttacksThisTurn.
	BecameTargetThisTurn bool

	// blockedByThisTurn is Card.getBlockedByThisTurn (Card.java:112,
	// 1658-1666): every creature that blocked this card this turn, in the
	// order recorded. Written where Java writes it -- blockers declared
	// (DeclareCombatBlockers, block.go; PhaseHandler.java:804-805) and a
	// Block effect adding a blocker (blockeffect.go; BlockEffect.java:60-61)
	// -- and nowhere else: SwitchBlock's re-added blocks are not recorded,
	// as SwitchBlockEffect.java's own addBlocker calls do not record them.
	// Cleared every cleanup (cleanupStep, turn.go; Card.onCleanupPhase,
	// Card.java:7152) and on leaving the battlefield (Game.Move), where
	// Java's card becomes a new object. Read by SwitchBlock's
	// `blockedByValidThisTurn Targeted` (switchblockeffect.go).
	blockedByThisTurn []CardID
	// blockedThisTurn is its mirror, Card.getBlockedThisTurn (Card.java:116,
	// 1657-1665): every attacker this card blocked this turn, written by the
	// same two calls (PhaseHandler.java:804, BlockEffect.java:60) and cleared
	// with it. Read by the blockedThisTurn and blockedValidThisTurn properties
	// (validhistory.go).
	blockedThisTurn []CardID
	// regeneratedThisTurn is Card.regeneratedThisTurn (Card.java:3516-3523):
	// how often the card regenerated this turn (regenerateBody), zeroed at
	// cleanup (Card.java:7275) and on a zone change. Count$RegeneratedThisTurn
	// reads it.
	regeneratedThisTurn int

	// LoyaltyAbilityActivated is CR 606.3's own once-per-turn marker
	// (Card.planeswalkerAbilityActivated in Java, collapsed from an int to a
	// bool -- StaticAbilityNumLoyaltyAct's own limit-raising static ability,
	// the only real reason Java counts past one, is a further mechanic this
	// port does not build). ActivateAbility/ActivateManaAbility
	// (activateability.go/activatemanaability.go) refuse any ability naming
	// Planeswalker$ (SpellAbility.isPwAbility's own bare hasParam check, the
	// identical presence-only contract compile.Ability.Param already
	// returns) once this reads true, and set it the moment such an ability
	// actually commits -- CR 606.3 restricts the whole loyalty ability, not
	// only the shape that pays for it, so an AddCounter<0/...> "+0" ability
	// (a real corpus shape, addCounterN's own doc comment) sets it exactly
	// the same as any other. Reset every cleanup (cleanupStep, turn.go)
	// alongside AttacksThisTurn/BecameTargetThisTurn.
	LoyaltyAbilityActivated bool

	// activations counts how often each of the card's A: lines was
	// activated: Card.numberTurnActivations/numberGameActivations
	// (Card.java:363-364), read by ActivationLimit$/GameActivationLimit$
	// (SpellAbilityRestriction.canPlay) and reset like Java's: the turn count
	// every cleanup (Game.java:1239-1241), both on any zone change, which
	// makes the card a new object (CR 400.7).
	activations activationCounts
	// trigActs is the same for the card's triggers, keyed by Execute$ ability.
	trigActs triggerActivations
	// abilityRemoval is this pass's Layer 6 removal of the card's own text.
	abilityRemoval abilityRemoval
	// trigResolved counts, this turn, how often each of the card's triggers
	// resolved (Card.numberAbilityResolved, ResolvedLimit$).
	trigResolved triggerActivations

	// ProtectingPlayer is CR 122.1/704.5w's protector: the opponent
	// defending a Battle. NoPlayer for anything that is not a Battle, or a
	// Battle that has not been assigned one yet (assignBattleProtector,
	// action.go).
	ProtectingPlayer PlayerID

	// HasNonLegendaryCreatureNames is Card.hasNonLegendaryCreatureNames's own
	// flag -- Layer 3's own AddNames$ AllNonLegendaryCreatureNames
	// (applyContinuousNames, continuous.go), recomputed fresh every
	// CheckStateBasedActions pass the identical way TypeMod/ColorMod/
	// KeywordMod already are, since it is a plain "does any current line
	// grant this" question with no timestamp-ordering to fold (unlike those
	// three, nothing else ever reads more than one source's own contribution
	// at once). resolveLegendRule (action.go) reads it for CR 704.5j's own
	// corner case: two legendary permanents that share every non-legendary
	// creature name -- Spy Kit's own real shape -- clash with each other even
	// when their printed names differ.
	HasNonLegendaryCreatureNames bool

	// changedName is Layer 3's SetName$ result (Card.changedCardNames,
	// Card.getName): the last overwrite in timestamp order, "" for none.
	// Rebuilt every pass by applyContinuousNames; read through Name.
	changedName string
	// stickerName is the name the card's Name stickers make of its printed one
	// (Card.recomputeStickerName, Card.java:4677-4693), "" with none. Kept
	// apart from changedName because a sticker's name holds in every public
	// zone, a SetName$ result only on the battlefield. Rebuilt every pass by
	// applyStickerNames.
	stickerName string

	// text is Layer 3's own text-changing effect on this permanent (CR
	// 613.1c): GainTextOf$ (applyContinuousText, continuous.go). While it
	// applies, Def is the composite text definition and text.base is the
	// definition under it, Layer 1's own result -- the same "swap Def, every
	// later layer folds over it unaware" shape copies already use.
	text textChange

	// hiddenKeywords is Card.hiddenExtrinsicKeywords (Card.java:103): whole
	// keyword lines a Mode$ Continuous AddHiddenKeyword$ static grants this
	// pass (applyOneContinuousHiddenKeyword, continuous.go), and a Pump/PumpAll
	// KW$ "HIDDEN " line while its record lives (pumpHiddenKeywords). Unlike
	// KeywordMod's AddKeyword$ grants they are not part of the card's
	// keyword list, so nothing that removes or counts keywords sees them;
	// only Card.hasKeyword's exact-text and hasStartOfKeyword's prefix reads
	// do (hasKeywordText/hasKeywordTextPrefix, blockvalidation.go).
	hiddenKeywords []string

	// RegenShields counts the regeneration shields Regenerate gave this
	// permanent this turn (CR 701.15): each replaces one destruction
	// (Game.regenerate) and all of them end at cleanup or when it leaves the
	// battlefield -- Java's one-shot Regeneration effect per Regenerate call.
	RegenShields int

	// detainedBy is Card.detainedBy (CR 701.35): the players who detained
	// this permanent. While any remain it can't attack or block and its
	// activated abilities can't be activated; each ends when that player's
	// next turn begins.
	detainedBy []PlayerID

	// Intensity is Card.intensity (Alchemy's intensify): starts at zero and
	// is raised by Intensify, read through Count$Intensity.
	Intensity int

	// exiledWith is Card.exiledWith (Card.java:326): which host object
	// exiled this card, read by the ExiledWithSource valid property
	// (valid.go). The zero value means exiled with nothing. Every zone
	// entry but one to the Stack clears it (put's own "kind != Stack"
	// guard, game.go, Card.cleanupExiledWith's own reset on a move,
	// GameAction.java:576-579), and markExiledWith (zonemove.go) sets it
	// after an effect's own exile move, ADR-0034.
	exiledWith exiledWithMark
	// delved is Card.delvedCards (Card.java:111): the cards the last delve
	// payment of this card exiled, read by the "delved" valid property
	// (Soulflayer). Cleared only by the next delve cast (CostAdjustment.java:
	// 262), never by a zone change: the host is one object from stack to
	// battlefield. Held by ID, as a card keeps its ID through Exile.
	delved []CardID
	// stickers is Card.stickers (Card.java:4617): what has been placed on this
	// card, in placement order (ADR-0044). A move into a hidden zone clears it
	// (put); one into a public zone keeps it (GameAction.java:268-270).
	stickers []appliedSticker
	// newPTs is Card.addNewPT's list (Card.java:4390-4398): set-P/T effects
	// a dice exchange wrote, in timestamp order (ADR-0045). Rebuilt into the
	// P/T layers every pass (newPTEffects); any zone change drops it
	// (Card.clearNewPT via removeChangedState, GameAction.java:363).
	newPTs []newPT
	// haunting is Card.haunting (Card.java:1392, CR 702.55): the creature
	// this exiled card haunts, set by Haunt (haunteffect.go) and read by the
	// HauntedBy valid property. The zero value haunts nothing.
	haunting hauntMark
	// battlefieldStamp is the zoneStamp this card had as its most recent
	// battlefield object, set as it leaves the battlefield (Move,
	// MoveToLibraryTop, game.go); zero while it never has. It stands in for
	// the old object an ability of this card keeps once the card has moved
	// on (hostObjectStamp, game.go) -- Java's own LKI copy keeps that
	// object's gameTimestamp (CardCopyService.java:375).
	battlefieldStamp uint64

	// Suspected, Solved, Harnessed and Plotted are Card's designations of
	// those names (AlterAttribute). suspectedTS is the timestamp of the
	// menace a suspected card has.
	Suspected, Solved, Harnessed, Plotted bool
	suspectedTS                           uint64

	// faceUpDef is the card's own definition while it is face down (CR
	// 708): Def then holds the face-down characteristics. Manifested and
	// Cloaked record how it turned face down.
	faceUpDef           *compile.Card
	Manifested, Cloaked bool

	// frontDef is the card's own (front face) definition while it is
	// transformed: Def then holds the back face. Transforms counts its
	// transformations, Java's transformedTimestamp.
	frontDef   *compile.Card
	Transforms int
	// flipped is Card.flipped (Card.java:1170): the permanent has flipped
	// (CR 709.4, one-way). For a flip card Def then holds the flipped face
	// and frontDef the unflipped one, the transform mechanism's own swap;
	// it also holds for a card with no flipped face, and for a face-down or
	// copying one, whose Def does not change. Cleared with the swap as the
	// card leaves the battlefield (turnFrontFaceUp).
	flipped bool

	// MeldedWith is Card.meldedWith (Card.java:1400) on a melded
	// permanent (CR 712.4a, meldeffect.go): the other card it represents,
	// NoCard for anything not melded. Def is then the meld face and frontDef
	// this card's own front face, turnFrontFaceUp's revert. Leaving the
	// battlefield splits the pair back apart (Game.unmeld, CR 712.4c).
	MeldedWith CardID
	// Melded marks the other card of a melded pair: PlayerZoneBattlefield.
	// addToMelded (PlayerZoneBattlefield.java:45-49) points its Zone at the
	// battlefield but adds it to no zone's card list, so no enumeration of
	// the battlefield (Zone.Cards, CardsIncludingPhasedOut, Len, Contains)
	// ever sees it; only its melded permanent's MeldedWith reaches it.
	Melded bool

	// roomDef is a Room's printed split card while it is a permanent or a
	// spell cast as one of its halves (room.go, CR 709.5): its own
	// characteristics (Def, or faceUpDef/uncopiedDef under a face-down or
	// copy effect) then hold the view of its unlocked doors, or of the half
	// it was cast as. doors is Card.unlockedRooms; castDoor is the half a
	// Room spell was cast as, which unlocks as it enters (permanentEffect).
	roomDef  *compile.Card
	doors    doorSet
	castDoor Door

	// copies are the Layer 1 copy effects on this permanent (CR 613.2a,
	// 707.2), ascending Timestamp: Java's Card.clonedStates. The last one's
	// definition is Def; uncopiedDef is what Def was before the first of
	// them and what it returns to once the last one ends (cloneeffect.go).
	// Every other layer already folds over Def, so a copy effect sits under
	// all of them without any of them knowing.
	copies      []copyEffect
	uncopiedDef *compile.Card

	// pendingEnter are the counters an entry replacement's chain put on this
	// card as part of the entry (a PutCounter with ETB$ inside a Copy-layer
	// chain: Altered Ego, The Mimeoplasm), before the card is on the
	// battlefield. applyEnterCounters adds them with the entry's other
	// counters, through the AddCounter replacements, once it has moved. The
	// slice is replaced, never appended in place: a last-known-information
	// snapshot shares it.
	pendingEnter []enterPile

	// devoured are the permanents this one's Devour sacrificed as it entered
	// (Card.devouredCards, Card.java:1186-1194), in the order sacrificed,
	// each read from its last-known information. Ends when the permanent
	// leaves the battlefield (Java's next zone's object is a new Card). The
	// slice is replaced, never appended in place.
	devoured []CardID

	// goadedBy are this creature's goads (CR 701.15).
	goadedBy []goad

	// mustBlock are the attackers this creature must block if able
	// (Card.mustBlockCards, MustBlockEffect.java:76/:79), in the order the
	// effects resolved. Every entry ends at cleanup (Card.onCleanupPhase's
	// clearMustBlockCards); a Duration$ UntilEndOfCombat one at end of
	// combat. A creature leaving the battlefield loses them all (CR 400.7).
	mustBlock []mustBlockReq

	// tempControllers are one-shot control changes (GainControl,
	// ExchangeControl): Java's Card.addTempController. Controller merges them
	// with ControlMod's continuous effects by timestamp, the latest winning.
	tempControllers []ControlEffect

	// leavesPlayCmds, untapCmds, changeControllerCmds and phaseOutCmds are
	// Java's per-card GameCommand lists (Card.addLeavesPlayCommand,
	// addUntapCommand, addChangeControllerCommand, addPhaseOutCommand), kept
	// as data (controlcommands.go): each runs once when its event happens
	// to this card and is then cleared, in registration order.
	leavesPlayCmds, untapCmds, changeControllerCmds, phaseOutCmds []cardCommand

	// unattachCmds is Card.unattachCommandList: commands run when this
	// Aura/Equipment becomes unattached (Card.unattachFromEntity), and
	// staticCmds is Card.staticCommandList: commands checked at every
	// state-based pass (GameAction.java:1180-1198).
	unattachCmds []cardCommand
	staticCmds   []staticCheck

	// pairedWith is the creature this one is Soulbond-paired with, NoCard
	// for none (Card.getPairedWith, soulbond.go).
	pairedWith CardID

	// grants is this card's overlay of granted triggers (ADR-0023 decision
	// 2), ascending id: the Layer-6 rows of Java's changedCardTraits
	// (Card.java:141-142) this port builds, one per Animate/AnimateAll
	// Duration$ Perpetual resolution that granted Triggers$ to the card
	// (PerpetualAbilities, AnimateEffectBase.java:221-233). Perpetual is the
	// only duration built, and it survives every zone change -- Java re-applies
	// the card's perpetual list to the new object (GameAction.java:265-266) --
	// so Move leaves it alone. Every trigger scan reads it through
	// triggerFaces (trigger.go). Replaced, never edited in place: a
	// last-known-information snapshot shares the backing array.
	grants []grantedTriggers

	// traitGrants is the Layer 6 trait overlay Mode$ Continuous statics write
	// (AddTrigger$, AddAbility$; ADR-0023): rebuilt from scratch every
	// state-based pass by applyContinuousTraits, like every other continuous
	// effect, so a grant ends the pass its source stops granting. Triggers are
	// read through triggerFaces, abilities through abilityAt.
	traitGrants []traitGrant

	// attachedTo is the card this one is attached to, and attachments is the
	// reverse. Both are unexported because they are two representations of one
	// fact and only Game.Attach and Game.Unattach may write either.
	attachedTo  CardID
	attachments *collect.OrderedSet[CardID]
	// attachedPlayer is the player this card is attached to (an Aura with
	// "Enchant player", CR 303.4h), NoPlayer when none. At most one of
	// attachedTo and attachedPlayer is set (Card.getEntityAttachedTo is one
	// field in Java); Game.Attach, AttachToPlayer and Unattach are the only
	// writers.
	attachedPlayer PlayerID
}

// grantedTriggers is one row of a card's grant overlay: the triggers one
// granting resolution gave it. id is that resolution's timestamp, Java's row
// key (game.getNextTimestamp, AnimateEffect.java:57), shared by every card the
// one resolution granted to and unique per resolution, so two grants of the
// same compiled SVar stay apart even though they hold the same
// *compile.Ability. amounts is the granting face's SVars: a granted trigger's
// Execute$ chain was compiled in that card's namespace
// (AbilityUtils.getSVar(sa, s), AnimateEffectBase.java:173) and reads its
// amounts there.
type grantedTriggers struct {
	id           uint64
	triggers     []*compile.Ability
	replacements []*compile.Ability
	amounts      map[string]expr.Amount
}

// traitGrant is the traits one continuous static granted to a card, compiled
// from the SVars its AddTrigger$/AddAbility$ named; amounts is the granting
// face's, where those traits' SVar amounts live.
type traitGrant struct {
	triggers     []*compile.Ability
	abilities    []*compile.Ability
	statics      []*compile.Ability
	replacements []*compile.Ability
	amounts      map[string]expr.Amount
}

// Name is Card.getName: the printed name, or the one a Layer 3 SetName$ gave a
// permanent (changedName; only a battlefield card is ever renamed). A card
// without a definition has none.
func (c *Card) Name() string {
	if c.changedName != "" && c.Zone == Battlefield {
		return c.changedName
	}
	if c.stickerName != "" {
		return c.stickerName
	}
	if c.Def == nil {
		return ""
	}
	return c.Def.Name
}

// grantFaces is faces plus one pseudo-face per trait grant that carries statics
// or replacement effects, holding exactly those lines and the granting face's
// amounts: Java's Card.getStaticAbilities/getReplacementEffects read the printed
// lines and the changedCardTraits rows through one list (Card.java), and every
// walk over a host's statics or replacements here reads traitFaces, so one
// seam serves them all (AddStaticAbility$, AddReplacementEffect$). The result
// shares faces' backing array only when no grant adds either.
func (c *Card) grantFaces(faces []compile.Face) []compile.Face {
	var out []compile.Face
	for _, grant := range c.traitGrants {
		if len(grant.statics) == 0 && len(grant.replacements) == 0 {
			continue
		}
		if out == nil {
			out = append(make([]compile.Face, 0, len(faces)+len(c.traitGrants)), faces...)
		}
		out = append(out, compile.Face{Statics: grant.statics, Replacements: grant.replacements, Amounts: grant.amounts})
	}
	if out == nil {
		return faces
	}
	return out
}

// printedTraitsRemoved is CR 305.7 (Card.hasRemoveIntrinsic): an effect
// that sets a land's subtype -- RemoveLandTypes$, Blood Moon's "nonbasic
// lands are Mountains" -- takes away every ability the land's own text gave
// it. Java clears them in Layer 4 (CardState.LandTraitChanges): the printed
// and Layer 3 text-gained statics, triggers, replacement effects, activated
// abilities and keywords, while Layer 6 grants (traitGrants, KeywordMod)
// still apply on top and the basic land type's own mana ability comes from
// the new subtype (TapLandForMana).
func (c *Card) printedTraitsRemoved() bool {
	return c.TypeMod.removesLandAbilities() || c.abilityRemoval != removalNone
}

// abilityRemoval is how much of a card's own text a Layer 6
// RemoveAllAbilities$/RemoveNonManaAbilities$ effect has taken away this pass
// (StaticAbilityContinuous.java:327-331, CardTraitChanges' remove predicate):
// every static, trigger, replacement effect, activated ability and keyword,
// or all of them but the mana abilities. It is rebuilt each pass
// (applyContinuousLayers) and read by printedTraitsRemoved and abilityAt.
type abilityRemoval uint8

const (
	removalNone abilityRemoval = iota
	removalNonMana
	removalAll
)

// hidesPrintedAbility reports whether ab, one of c's own printed A: lines, is
// not an ability c has: gone to a land subtype setter (CR 305.7) or to a
// RemoveAllAbilities$ effect, or, under RemoveNonManaAbilities$, not a mana
// ability.
func (c *Card) hidesPrintedAbility(ab *compile.Ability) bool {
	if c.TypeMod.removesLandAbilities() || c.abilityRemoval == removalAll {
		return true
	}
	return c.abilityRemoval == removalNonMana && !strings.EqualFold(ab.Name, "Mana")
}

// traitDef is c's definition as a source of its own printed traits, nil
// while printedTraitsRemoved. Every scan for a host's static abilities,
// replacement effects, triggers, activated abilities and keywords reads its
// printed lines through this (or traitFaces), never Def directly.
func (c *Card) traitDef() *compile.Card {
	if c.printedTraitsRemoved() {
		return nil
	}
	return c.Def
}

// traitFaces is traitDef's faces, nil when it is nil.
func (c *Card) traitFaces() []compile.Face {
	var faces []compile.Face
	if d := c.traitDef(); d != nil {
		faces = d.Faces[:]
	}
	return c.grantFaces(faces)
}

// liveTraitFaces is traitFaces cut to liveFaces.
func (c *Card) liveTraitFaces() []compile.Face {
	var faces []compile.Face
	if d := c.traitDef(); d != nil {
		faces = d.Faces[:liveFaces(d)]
	}
	return c.grantFaces(faces)
}

// abilityAt is the index'th activated ability c has: its printed A: lines,
// then the abilities continuous statics grant it (traitGrants), with the
// amounts those lines read their SVars from.
func (c *Card) abilityAt(index int) (*compile.Ability, map[string]expr.Amount, bool) {
	if c.Def == nil || index < 0 {
		return nil, nil, false
	}
	printed := c.Def.Faces[0].Abilities
	if index < len(printed) {
		// CR 305.7 and RemoveAllAbilities$ hide the printed lines without
		// renumbering the granted ones after them.
		if c.hidesPrintedAbility(printed[index]) {
			return nil, nil, false
		}
		return printed[index], c.Def.Faces[0].Amounts, true
	}
	index -= len(printed)
	for _, grant := range c.traitGrants {
		if index < len(grant.abilities) {
			return grant.abilities[index], grant.amounts, true
		}
		index -= len(grant.abilities)
	}
	return nil, nil, false
}

// abilityCount is how many indices abilityAt answers for, printed lines and
// granted ones alike. Under CR 305.7 the printed indices answer not-found
// while the granted ones after them still answer, so a walk over c's
// abilities runs to abilityCount and skips a not-found index rather than
// stopping at it.
func (c *Card) abilityCount() int {
	if c.Def == nil {
		return 0
	}
	n := len(c.Def.Faces[0].Abilities)
	for _, grant := range c.traitGrants {
		n += len(grant.abilities)
	}
	return n
}

// abilityAmounts is the amounts table ability reads its SVars from: the
// granting face's for a granted ability, else c's own printed face's.
func (c *Card) abilityAmounts(ability *compile.Ability) map[string]expr.Amount {
	for _, grant := range c.traitGrants {
		if slices.Contains(grant.abilities, ability) {
			return grant.amounts
		}
	}
	return c.Def.Faces[0].Amounts
}

// withGrant returns c's grants plus g, in a fresh slice (grants' own
// doc comment has the reason).
func (c *Card) withGrant(g grantedTriggers) []grantedTriggers {
	out := make([]grantedTriggers, 0, len(c.grants)+1)
	return append(append(out, c.grants...), g)
}

// withoutGrant returns c's grants less the row keyed id, in a fresh slice,
// and whether that row was there (Card.removePerpetual, Card.java:4595-4604).
func (c *Card) withoutGrant(id uint64) ([]grantedTriggers, bool) {
	out := make([]grantedTriggers, 0, len(c.grants))
	found := false
	for _, g := range c.grants {
		if g.id == id {
			found = true
			continue
		}
		out = append(out, g)
	}
	return out, found
}

// Controller is this card's current controller: Layer 2's own GainControl$
// effects (ControlMod, controlmod.go) folded onto the card's own base
// controller -- Type()'s/HasKeyword()'s own Layer 4/6 counterpart. The fold
// picks the highest-Timestamp ControlEffect if any exist, else falls back to
// the base -- Java's own Card.getController() (Card.java) reading its
// tempControllers NavigableMap the identical way, ControlEffect's own doc
// comment has the reason Java's extra base-timestamp guard collapses away
// here.
func (c *Card) Controller() PlayerID {
	best := c.controller
	var bestTS uint64
	found := false
	for _, e := range c.ControlMod.effects {
		if !found || e.Timestamp > bestTS {
			best, bestTS, found = e.Controller, e.Timestamp, true
		}
	}
	for _, e := range c.tempControllers {
		if !found || e.Timestamp > bestTS {
			best, bestTS, found = e.Controller, e.Timestamp, true
		}
	}
	return best
}

// Type is the card's current type line: its own primary face's printed type
// (CardState -- which face is current for a transformed, flipped or melded
// card -- is not modeled yet, game-state.md's "Not ported yet") with Layer
// 4's own continuous type-changing effects (TypeMod, typemod.go) folded in --
// Power/Toughness's own Layer 7 counterpart (card.go's own doc comment on
// foldPT). A synthetic card built with a nil Def (most engine tests) reports
// the zero Line, which matches nothing -- consistent with "no Def" already
// meaning "no name" elsewhere, and skips the fold entirely: TypeMod on a
// nil-Def card is never populated by anything real.
func (c *Card) Type() cardtype.Line {
	if c.Def == nil {
		return cardtype.Line{}
	}
	return foldType(c.Def.Faces[0].Type, c.TypeMod.effects)
}

// isSick is Card.isSick (Card.java:3651-3653): summoning sickness
// restricts only a creature (CR 302.6), and haste lifts it
// (Card.hasSickness). A land or artifact that entered this turn can pay a
// {T} cost.
func (c *Card) isSick() bool {
	return c.SummonSick && c.Type().Has(cardtype.Creature) && !c.HasKeyword("Haste")
}

// HasKeyword reports whether the card currently carries the named keyword:
// its own printed face, exact match against keyword.Parse's own Name (the
// head as written -- "Indestructible" for a bare line, "Ward" for
// "Ward:2"), so a keyword written with arguments is still found by its bare
// name, folded with Layer 6's own continuous keyword changes (KeywordMod,
// keywordmod.go) -- Type()'s/Colors()'s own Layer 4/5 counterpart.
func (c *Card) HasKeyword(name string) bool {
	for _, line := range c.KeywordLines() {
		if keyword.Parse(line).Name == name {
			return true
		}
	}
	return false
}

// KeywordLines is every keyword line c currently carries: the printed face
// with every Layer 6 change (KeywordMod) applied in Timestamp order -- each
// one's removals, then its additions (KeywordsChange.applyKeywords). A
// keyword Layer 6 grants is exactly as real a source for landwalk or
// protection (staticability.go) as a printed one.
func (c *Card) KeywordLines() []string {
	var lines []string
	if d := c.traitDef(); d != nil {
		lines = append(lines, d.Faces[0].Keywords...)
	}
	return c.KeywordMod.fold(lines)
}

// BasePower and BaseToughness are the card's printed power and toughness --
// CR 613's Layer 0, before anything in Layer 7 or a counter has applied.
// Java calls these getBasePower/getBaseToughness for the same reason:
// "base" is a named concept in the rules, distinct from "current"
// (getNetPower) -- Power/Toughness, below, is this port's getNetPower.
//
// A written "*" counts as zero and the sign binding it goes with it ("1+*"
// is 1, "7-*" is 7), CardFace.parsePT's rule (carddb.PrintedPT): the
// characteristic-defining ability that gives such a creature its real value
// is Layer 7a's job (PT), so a "*" creature with none (or whose amount is
// not resolvable) reads 0 and dies to CR 704.5f, as in Java. ok is false only
// for a card with no printed P/T at all (an instant, a nil Def).
//
// A token's own override (hasBasePower) is part of its own copiable values,
// so a copy effect on the token hides it: the copied definition carries the
// copied object's own power instead (CR 707.2).
func (c *Card) BasePower() (int, bool) {
	if c.hasBasePower && len(c.copies) == 0 {
		return c.basePower, true
	}
	if c.Def == nil {
		return 0, false
	}
	return carddb.PrintedPT(c.Def.Faces[0].Power)
}

// BaseToughness is BasePower's counterpart; see its doc comment.
func (c *Card) BaseToughness() (int, bool) {
	if c.hasBaseToughness && len(c.copies) == 0 {
		return c.baseToughness, true
	}
	if c.Def == nil {
		return 0, false
	}
	return carddb.PrintedPT(c.Def.Faces[0].Toughness)
}

// BaseLoyalty is a planeswalker's printed starting loyalty -- CR 121.5's
// own words, since unlike power and toughness a planeswalker's loyalty is
// not something Layer 7 recomputes on every check. It exists once, the
// moment the permanent enters the battlefield, as that many loyalty
// counters (Loyalty, counters.go); everything after that -- gaining,
// losing, paying loyalty costs -- is ordinary counter addition and
// removal, which Card.Counters already handles. ok is false on the same
// terms as BasePower/BaseToughness: anything past a plain printed integer,
// or a nil Def.
func (c *Card) BaseLoyalty() (int, bool) {
	if c.Def == nil {
		return 0, false
	}
	n, err := strconv.Atoi(c.Def.Faces[0].Loyalty)
	return n, err == nil
}

// BaseDefense is BaseLoyalty's counterpart for a Battle: its printed
// starting defense, entered as that many Defense counters (counters.go)
// the same way a planeswalker's loyalty is, not a Layer 7 value.
func (c *Card) BaseDefense() (int, bool) {
	if c.Def == nil {
		return 0, false
	}
	n, err := strconv.Atoi(c.Def.Faces[0].Defense)
	return n, err == nil
}

// Colors is the card's current color identity for rules purposes (CR 105,
// valid.go's White/Blue/Black/Red/Green/Colorless/MultiColor properties): a
// script's explicit `Colors:` override, or (absent one) whatever its mana
// cost's own colored symbols say -- the exact "override, else derive" logic
// carddb.Face.dumpColors already carries out and M2's P1 gate already
// verifies byte-identical to Forge's own dump, not a new derivation --
// with Layer 5's own continuous color-changing effects (ColorMod,
// colormod.go) folded in, Type()'s own Layer 4 counterpart (card.go's own
// doc comment on foldType). A nil Def reports the zero value, mana.Colors'
// own "colorless" -- consistent with every other Def-derived accessor here,
// and skips the fold entirely: ColorMod on a nil-Def card is never
// populated by anything real.
func (c *Card) Colors() mana.Colors {
	if c.Def == nil {
		return 0
	}
	f := c.Def.Faces[0]
	base := f.ManaCost.Colors()
	if f.HasColors {
		base = f.Colors
	}
	return foldColor(base, c.ColorMod.effects)
}

// textChange is one permanent's Layer 3 text change: Java's
// changedCardNames/changedCardManaCost/changedCardColorsByText/
// changedCardTypesByText/changedCardTraitsByText/changedCardKeywordsByText/
// newPTText (Card.java:128-279), every one of which GainTextOf$ writes at
// once, collapsed into the one composite definition they describe together.
//
// base is non-nil exactly while the change applies. def and its key outlive
// it: they cache the composite built from source definition from by the
// static at Faces[face].Statics[static] of definition owner, so a pass whose
// graveyard top is unchanged reuses it rather than building a new one. The
// key names the static by position, not by pointer, since card.go's own
// enginelint group may not name the static's type. Every pointer here is
// immutable compiled data, so Game.Clone and a last-known-information
// snapshot share them like any Def.
type textChange struct {
	base         *compile.Card
	def          *compile.Card
	from, owner  *compile.Card
	face, static int
}

// preTextDef is c's definition under any Layer 3 text change -- Def itself
// when none applies. CR 707.2 leaves text-changing effects out of an
// object's copiable values, so copying reads this, not Def.
func (c *Card) preTextDef() *compile.Card {
	if c.text.base != nil {
		return c.text.base
	}
	return c.Def
}

// clearTextChange ends c's Layer 3 text change, if any: Def returns to what
// it was under the change. A writer that replaced Def since the change
// applied (a copy effect ending, a face turning up) has already said what
// Def is, so its value stands and only the record is dropped.
func (c *Card) clearTextChange() {
	if c.text.base == nil {
		return
	}
	if c.Def == c.text.def {
		c.Def = c.text.base
	}
	c.text.base = nil
}

// setTextChange makes t.def c's definition as a Layer 3 text change,
// recording t's key for the next pass's cache check. A second change in the
// same pass stacks over the first, as Java's timestamp-ordered tables do;
// base stays the definition under both.
func (c *Card) setTextChange(t textChange) {
	if c.text.base == nil {
		c.text.base = c.Def
	}
	t.base = c.text.base
	c.text = t
	c.Def = t.def
}

// Power and Toughness are the card's current power and toughness: Layer 0
// (BasePower/BaseToughness) with Layer 7's continuous effects (PT) folded
// in, plus +1/+1 and -1/-1 counters, in CR 613.4's own order -- counters
// apply after every layer, not as one themselves.
//
// ok is false wherever BasePower/BaseToughness's own ok is, unless a
// LayerCharacteristic effect supplies a value of its own: a
// characteristic-defining ability's whole point is replacing an
// unresolvable printed value ("*") with a computed one, so PT can turn an
// unresolvable base into a resolvable current value, never the reverse.
func (c *Card) Power() (int, bool) {
	if c.PT.Switched() {
		return c.unswitchedToughness()
	}
	return c.unswitchedPower()
}

// Toughness is Power's counterpart; see its doc comment.
func (c *Card) Toughness() (int, bool) {
	if c.PT.Switched() {
		return c.unswitchedPower()
	}
	return c.unswitchedToughness()
}

// unswitchedPower and unswitchedToughness are Java's getUnswitchedPower and
// getUnswitchedToughness: Layer 7a-7c and counters, before Layer 7d (CR
// 613.4d) swaps the two totals. The swap takes the whole total, counters
// included, as getNetPower does (Card.java:4453).
func (c *Card) unswitchedPower() (int, bool) {
	v, ok := c.layer7Power()
	if !ok {
		return 0, false
	}
	return v + c.modifyPT(func(e PTEffect) int { return e.Power }) + c.Counters.Count(P1P1) - c.Counters.Count(M1M1), true
}

func (c *Card) unswitchedToughness() (int, bool) {
	v, ok := c.layer7Toughness()
	if !ok {
		return 0, false
	}
	return v + c.modifyPT(func(e PTEffect) int { return e.Toughness }) + c.Counters.Count(P1P1) - c.Counters.Count(M1M1), true
}

// layer7Power and layer7Toughness are Java's getCurrentPower and
// getCurrentToughness (Card.java:4417,4460): the base folded with the
// characteristic-defining and setting effects (Layers 7a and 7b) only. Layer
// 7c's modifiers (Java's temp boost) and counters are not in them, so a
// +1/+1 anthem does not change what "basePowerEQ3" measures
// (CardProperty.java's "basePower"/"baseToughness"). This is a distinct, more
// confusingly-named thing than getBasePower/getBaseToughness (this port's
// BasePower/BaseToughness). Neither value is switched (Layer 7d).
func (c *Card) layer7Power() (int, bool) {
	if c.starUnresolved(false) {
		return 0, false
	}
	base, ok := c.BasePower()
	return foldPT(base, ok, c.PT.effects, func(e PTEffect) (int, bool) { return e.Power, e.HasPower })
}

func (c *Card) layer7Toughness() (int, bool) {
	if c.starUnresolved(true) {
		return 0, false
	}
	base, ok := c.BaseToughness()
	return foldPT(base, ok, c.PT.effects, func(e PTEffect) (int, bool) { return e.Toughness, e.HasToughness })
}

// modifyPT sums the Layer 7c effects' pick, Java's getTempPowerBoost and
// getTempToughnessBoost.
func (c *Card) modifyPT(pick func(PTEffect) int) int {
	total := 0
	for _, e := range c.PT.effects {
		if e.Layer == LayerModifyPT {
			total += pick(e)
		}
	}
	return total
}

// starUnresolved reports a printed "*" power or toughness (toughness when
// toughness is true) whose characteristic-defining static exists but produced
// no Layer 7a value -- an amount this port cannot evaluate (BasePower's doc
// comment). The base then reads 0 only by CardFace.parsePT's convention, and
// the real value is whatever that amount is, so the creature is left
// unresolved rather than killed or sized by a guess (GO-7). A "*" with no
// such static at all is a plain 0, as Java reads it.
func (c *Card) starUnresolved(toughness bool) bool {
	if c.Def == nil || len(c.copies) != 0 {
		return false
	}
	printed := c.Def.Faces[0].Power
	if toughness {
		printed = c.Def.Faces[0].Toughness
	}
	if !strings.Contains(printed, "*") {
		return false
	}
	for _, e := range c.PT.effects {
		if e.Layer == LayerCharacteristic && (toughness && e.HasToughness || !toughness && e.HasPower) {
			return false
		}
	}
	for _, face := range c.traitFaces() {
		for _, st := range face.Statics {
			if _, cda := st.Param("CharacteristicDefining"); cda && strings.EqualFold(st.Name, "Continuous") {
				return true
			}
		}
	}
	return false
}

// CMC is the card's printed mana value (CR 202.3), the sum of its mana
// cost's own symbols -- compile.Face's own ManaCost (Colors' own doc
// comment carries the same "printed value only" limit). A nil Def reports
// 0, mana.Cost's own zero value.
func (c *Card) CMC() int {
	if c.Def == nil {
		return 0
	}
	return c.Def.Faces[0].ManaCost.CMC()
}

// stackX is the mana value the X its cast paid adds (Card.getXManaCostPaid
// times the cost's X count, Card.java:7351-7353), 0 for a card not cast with
// an X: castX is kept only on the stack and the battlefield.
func (c *Card) stackX() int {
	if c.Def == nil {
		return 0
	}
	return c.castX * c.Def.Faces[0].ManaCost.CountX()
}

// isCommander is Card.isCommander: only Player.addCommander sets it, from a
// Commander game setup or GameState's |IsCommander option, neither ported
// (commanderCasts, castrecord.go), so no card here is a commander.
func (*Card) isCommander() bool { return false }

// shortManaCost is Card.getManaCost().getShortString(), what a granted
// keyword's CardManaCost stands for (mana.Cost.ShortString).
func (c *Card) shortManaCost() string {
	if c.Def == nil {
		return ""
	}
	return c.Def.Faces[0].ManaCost.ShortString()
}

// foldPT applies Layers 7a and 7b in order (CR 613.4a-b): each effect
// replaces the running value, unless pick's own bool reports this effect
// does not set this particular dimension at all (PTEffect's own
// HasPower/HasToughness doc comment has the reason), in which case the
// running value is left exactly as it was. Layer 7c effects are
// Card.modifyPT's. Ties within a layer break by Timestamp, ascending (CR
// 613.7). Dependency (CR 613.8) changes which cards a Layer 7b static
// applies to, never this fold: Java keys its own tables by the static's
// timestamp whatever order the statics were applied in.
func foldPT(base int, baseOK bool, effects []PTEffect, pick func(PTEffect) (int, bool)) (int, bool) {
	sorted := append([]PTEffect(nil), effects...)
	sort.Slice(sorted, func(i, j int) bool {
		if sorted[i].Layer != sorted[j].Layer {
			return sorted[i].Layer < sorted[j].Layer
		}
		return sorted[i].Timestamp < sorted[j].Timestamp
	})
	value, ok := base, baseOK
	for _, e := range sorted {
		v, has := pick(e)
		switch e.Layer {
		case LayerCharacteristic, LayerSetPT:
			if has {
				value, ok = v, true
			}
		case LayerModifyPT:
			// Layer 7c is Card.modifyPT's: it adds after the base is
			// known, whatever the base is.
		}
	}
	return value, ok
}

// IsPhasedOut is Card.isPhasedOut (CR 702.26b). A phased-out permanent is
// still in the battlefield zone -- c.Zone reads Battlefield, Java's own
// isInPlay() split (ADR-0021, decision 3) -- but every battlefield
// enumeration (Zone.Cards) leaves it out.
func (c *Card) IsPhasedOut() bool { return c.phasedOut != NoPlayer }

// PhasedOutFor is Card.getPhasedOut: the player whose untap step phases c
// back in, NoPlayer while c is phased in.
func (c *Card) PhasedOutFor() PlayerID { return c.phasedOut }

// AttachedTo is what this card is attached to, and whether it is attached at
// all. Auras, Equipment and Fortifications all use it.
func (c *Card) AttachedTo() (CardID, bool) {
	if c.attachedTo == NoCard {
		return NoCard, false
	}
	return c.attachedTo, true
}

// AttachedToPlayer is the player this card is attached to, and whether it is
// attached to one at all (Card.getPlayerAttachedTo). An Aura with "Enchant
// player" -- a Curse -- is the one real shape.
func (c *Card) AttachedToPlayer() (PlayerID, bool) {
	return c.attachedPlayer, c.attachedPlayer != NoPlayer
}

// Attachments returns what is attached to this card, in the order it was
// attached. Order decides which Aura's continuous effect applies first when
// two share a timestamp.
func (c *Card) Attachments() []CardID {
	if c.attachments == nil {
		return nil
	}
	return c.attachments.All()
}

// exiledWithMark is one card's Card.exiledWith: the host that exiled it,
// as the host object it was then. stamp is that host object's zoneStamp
// (Java compares with equalsWithGameTimestamp, CardProperty.java:410, so a
// host that has since left and come back is a new object that exiled
// nothing). listed is whether Java would also have put the card on that
// host's own exiledCards list, which it does only for a host in play, on
// the stack or in the Command zone (SpellAbilityEffect.java:1100-1104) --
// the ExiledWithSource property's other half, source.hasExiledCard.
type exiledWithMark struct {
	host   CardID
	stamp  uint64
	listed bool
}

// ExiledWith is the card that exiled c, NoCard if nothing did since c last
// changed zones (Card.getExiledWith).
func (c *Card) ExiledWith() CardID { return c.exiledWith.host }

// hauntMark is one card's Card.haunting with both objects pinned: self is the
// haunter's own exile zoneStamp, card the haunted creature and stamp that
// creature's battlefield zoneStamp, all as Haunt exiled the haunter. Java
// keeps the link right with two Static$ triggers that undo it when the haunter
// leaves exile or the creature leaves the battlefield (CardFactoryUtil.java:
// 1213-1237); the stamps answer the same question by object identity (CR
// 400.7), so a moved card stops matching without a trigger.
type hauntMark struct {
	self  uint64
	card  CardID
	stamp uint64
}

// haunts reports whether c, still the exiled object Haunt made it, haunts
// target -- CardProperty.java:553's HauntedBy, from the haunter's side. A
// target on the battlefield must be the object that was haunted; one that
// has left it is matched by its last battlefield object (battlefieldStamp),
// which is how a dies trigger's last-known information still sees the
// creature the haunter haunted as it died.
func (c *Card) haunts(target *Card) bool {
	m := c.haunting
	if m.card == NoCard || m.card != target.ID || c.Zone != Exile || c.zoneStamp != m.self {
		return false
	}
	if target.Zone == Battlefield {
		return target.zoneStamp == m.stamp
	}
	return target.battlefieldStamp == m.stamp
}

// Kicker1 and Kicker2 are the bits Card.kicker records: which of a card's two
// kicker costs were paid (Java's OptionalCost.Kicker1/Kicker2).
const (
	kicker1 uint8 = 1 << iota
	kicker2
)

// optionalKinds is the slice of Java's OptionalCost enum a cast records past
// Kicker1/Kicker2 (SpellAbility.optionalCosts, an EnumSet): Generic for a
// Mode$ OptionalCost static without ReduceColor$, Teamwork and Bargain for
// the keywords. A set, so two Generic costs paid read the same as one.
type optionalKinds uint8

const (
	optGeneric optionalKinds = 1 << iota
	optTeamwork
	optBargain
	// optAltCost is OptionalCost.AltCost: the card's own Mode$ AlternativeCost
	// static paid the cast (StaticAbilityAlternativeCost.java:84-86).
	optAltCost
)

// castOptional is a card's record of its cast's optional costs: Java keeps
// them on Card.getCastSA, read by isOptionalCostPaid and the paid lists
// (SpellAbility.getPaidList). collected is "Collected" (CostCollectEvidence),
// revealed is "Revealed" (CostReveal and CostBehold). The lists are replaced,
// never appended to in place, so a copied Card may share them. foretold is
// getCastSA().isForetold() (SpellAbility.java:743-745): the card was cast for
// its foretell cost from exile (castFromHand's foretellCost way); escaped is
// getCastSA().isEscape() (SpellAbility.isEscape): cast from the graveyard for
// its Escape cost (escapeCost).
type castOptional struct {
	kinds     optionalKinds
	collected []CardID
	revealed  []CardID
	// revealedLKI is the "Revealed" list as Java keeps it: each revealed or
	// chosen card frozen as it was paid (CostPartWithList.java:136-138,
	// CostReveal's getHashForLKIList), what Revealed$CardPower reads. Parallel
	// to revealed.
	revealedLKI []*Card
	// exiled are the cards the optional costs exiled (CostExile's "Exiled"),
	// marked as exiled with the spell once it is on the stack (putSpellOnStack:
	// Java moves the spell there before paying, PlaySpellAbility.java:624, and
	// CostExile.java:294 marks them then).
	exiled   []CardID
	foretold bool
	escaped  bool
	// madness is SpellAbility.isMadness (AlternativeCost.Madness): the spell
	// was cast by Madness's trigger for its madness cost (Count$Madness).
	madness bool
	// emerged is getSacrificedAsEmerge (SpellAbility, PlaySpellAbility.java:
	// 553): the permanent sacrificed for the Emerge cost, read as it last was
	// (emerge.go); NoCard for a cast that did not emerge.
	emerged CardID
}

// has reports whether the cast paid an optional cost of kind k.
func (o castOptional) has(k optionalKinds) bool { return o.kinds&k != 0 }

// castEscaped is getCastSA() != null && getCastSA().isEscape(): c was cast
// for its Escape cost and still carries that cast (on the stack, or the
// permanent it became).
func (c *Card) castEscaped() bool { return c.wasCast && c.castOpt.escaped }

// spellCollected is the evidence c's cast collected while c is that spell on
// the stack, nil anywhere else (Defined$ Collected).
func (c *Card) spellCollected() []CardID {
	if c.Zone != Stack {
		return nil
	}
	return c.castOpt.collected
}

// frozenCopy is a copy of the snapshot c that shares no mutable state with it:
// what Game.Clone gives every last-known-information card, and what a cost's
// paid list keeps of a card as it was paid (CardCopyService.getLKICopy).
func (c *Card) frozenCopy() *Card {
	s := *c
	s.Counters = c.Counters.clone()
	s.Memory = c.Memory.clone()
	s.PT = c.PT.clone()
	s.TypeMod = c.TypeMod.clone()
	s.ColorMod = c.ColorMod.clone()
	s.KeywordMod = c.KeywordMod.clone()
	s.ControlMod = c.ControlMod.clone()
	s.copies = append([]copyEffect(nil), c.copies...)
	s.grants = append([]grantedTriggers(nil), c.grants...)
	if c.attachments != nil {
		s.attachments = c.attachments.Clone()
	}
	return &s
}

// clone is o with its own lists, for Game.Clone.
func (o castOptional) clone() castOptional {
	o.collected = append([]CardID(nil), o.collected...)
	o.revealed = append([]CardID(nil), o.revealed...)
	o.exiled = append([]CardID(nil), o.exiled...)
	if o.revealedLKI != nil {
		lki := make([]*Card, len(o.revealedLKI))
		for i, c := range o.revealedLKI {
			lki[i] = c.frozenCopy()
		}
		o.revealedLKI = lki
	}
	return o
}

// kickerMagnitude is Card.getKickerMagnitude for a kicker (not multikicker)
// card: how many kicker costs were paid, 0 to 2 -- one when exactly one of the
// two was, as Java computes it.
func (c *Card) kickerMagnitude() int {
	k1, k2 := c.kicker&kicker1 != 0, c.kicker&kicker2 != 0
	switch {
	case k1 && k2:
		return 2
	case k1 || k2:
		return 1
	}
	return 0
}

// IsFlipped is Card.isFlipped.
func (c *Card) IsFlipped() bool { return c.flipped }

// InFlippedState is Card.getCurrentStateName() == Flipped: a flip card
// showing its flipped face, which GameState writes as `|Flipped`.
func (c *Card) InFlippedState() bool { return c.flipped && c.frontDef != nil }

// InTransformedState reports whether c shows the back face of a transforming
// double-faced card (CardStateName.Backside, GameState's `|Transformed`).
func (c *Card) InTransformedState() bool {
	return !c.flipped && c.frontDef != nil && c.frontDef.SplitType == carddb.SplitTransform && len(c.copies) == 0
}

// liveFaces is how many of def's faces are its current state's. A
// transforming, flipping, melded, modal or specialize card's other face is a
// state it is not in (Card.getTriggers reads currentState alone), and this
// port's current face of such a card is always Faces[0]: a transformed or
// melded card's Def is its back face, no path flips a flip card yet
// (setstateeffect.go's own "Flip... not resolved" gap -- an unflipped card's
// Def still carries both faces' real data, so its back face's triggers must
// stay unread the identical way an untransformed DFC's did before this fix),
// and no path puts a modal card's back face into play (playCastGap rejects
// its choice of spells). cloneDef (cloneeffect.go) reads faces the same way.
// Prepare joins the restricted set for the identical reason and for
// consistency with Java's own classification (CardSplitType.java:7-16 groups
// Flip and Prepare under USE_ACTIVE_FACE with Transform/Meld/Modal/
// Specialize), though 0 real corpus Prepare lines carry a trigger on their
// alternate face today -- corpus-inert, not exempted on principle. Every
// other definition keeps all its faces: a split, adventure or omen card's,
// and a Room permanent's both-doors view (roomView, room.go).
func liveFaces(def *compile.Card) int {
	switch def.SplitType {
	case carddb.SplitTransform, carddb.SplitFlip, carddb.SplitMeld, carddb.SplitModal, carddb.SplitSpecialize,
		carddb.SplitPrepare:
		return 1
	}
	return len(def.Faces)
}

// evolvedBy is Trigger.meetsRequirementsOnTriggeredObjects' CR 702.100c check
// (Trigger.java:377): entering is a creature with greater power or greater
// toughness than c, which is a creature too. False when either value does not
// resolve (GO-7).
func (c *Card) evolvedBy(entering *Card) bool {
	if !entering.Type().Has(cardtype.Creature) || !c.Type().Has(cardtype.Creature) {
		return false
	}
	ep, okEP := entering.Power()
	et, okET := entering.Toughness()
	cp, okCP := c.Power()
	ct, okCT := c.Toughness()
	if !okEP || !okET || !okCP || !okCT {
		return false
	}
	return ep > cp || et > ct
}

// enterPile is n counters of kind ct an entering permanent receives, put by
// source's replacement and placed by placer (the controller the AddCounter
// replacements read).
type enterPile struct {
	ct     CounterType
	n      int
	source CardID
	placer PlayerID
}
