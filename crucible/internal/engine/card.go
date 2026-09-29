// Cards. Identity and location here; the mutable detail is split out by
// concern (ADR-0009).

package engine

import (
	"sort"
	"strconv"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/expr"
	"github.com/jczastkiewicz/crucible/internal/keyword"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/pkg/collect"
)

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

	// AttacksThisTurn is CardDamageHistory.getCreatureAttacksThisTurn's own
	// per-card counter -- incremented once per combat this card is declared
	// an attacker in (DeclareCombatAttackers, attack.go), reset every cleanup
	// (cleanupStep, turn.go) alongside Damage/LandsPlayed/CardsDrawnThisTurn.
	// TriggerAttacks' own FirstAttack$ (trigger.go) is the one reader: "this
	// is the first time this creature has attacked this turn," true exactly
	// when this reads 1 at the moment its own trigger fires, immediately
	// after the increment.
	AttacksThisTurn int

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

	// text is Layer 3's own text-changing effect on this permanent (CR
	// 613.1c): GainTextOf$ (applyContinuousText, continuous.go). While it
	// applies, Def is the composite text definition and text.base is the
	// definition under it, Layer 1's own result -- the same "swap Def, every
	// later layer folds over it unaware" shape copies already use.
	text textChange

	// hiddenKeywords is Card.hiddenExtrinsicKeywords (Card.java:103): whole
	// keyword lines a Mode$ Continuous AddHiddenKeyword$ static grants this
	// pass (applyOneContinuousHiddenKeyword, continuous.go). Unlike
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
	// is raised by Intensify, read through Count$CardIntensity.
	Intensity int

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

	// attachedTo is the card this one is attached to, and attachments is the
	// reverse. Both are unexported because they are two representations of one
	// fact and only Game.Attach and Game.Unattach may write either.
	attachedTo  CardID
	attachments *collect.OrderedSet[CardID]
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
	id       uint64
	triggers []*compile.Ability
	amounts  map[string]expr.Amount
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
	if c.Def != nil {
		lines = append(lines, c.Def.Faces[0].Keywords...)
	}
	return c.KeywordMod.fold(lines)
}

// BasePower and BaseToughness are the card's printed power and toughness --
// CR 613's Layer 0, before anything in Layer 7 or a counter has applied.
// Java calls these getBasePower/getBaseToughness for the same reason:
// "base" is a named concept in the rules, distinct from "current"
// (getNetPower) -- Power/Toughness, below, is this port's getNetPower.
//
// ok is false for anything that is not a plain integer: "*", "1+*", a
// Count$ reference, or a card with no printed toughness at all (an
// instant, a nil Def). Resolving those needs `internal/expr` and a game,
// neither of which this reaches yet -- a coverage gap, not a wrong answer,
// the same category CheckStateBasedActions's own gaps are in.
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
	n, err := strconv.Atoi(c.Def.Faces[0].Power)
	return n, err == nil
}

// BaseToughness is BasePower's counterpart; see its doc comment.
func (c *Card) BaseToughness() (int, bool) {
	if c.hasBaseToughness && len(c.copies) == 0 {
		return c.baseToughness, true
	}
	if c.Def == nil {
		return 0, false
	}
	n, err := strconv.Atoi(c.Def.Faces[0].Toughness)
	return n, err == nil
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
	v, ok := c.layer7Power()
	if !ok {
		return 0, false
	}
	return v + c.Counters.Count(P1P1) - c.Counters.Count(M1M1), true
}

// Toughness is Power's counterpart; see its doc comment.
func (c *Card) Toughness() (int, bool) {
	v, ok := c.layer7Toughness()
	if !ok {
		return 0, false
	}
	return v + c.Counters.Count(P1P1) - c.Counters.Count(M1M1), true
}

// layer7Power and layer7Toughness are Power/Toughness stopped one step
// early: base folded with Layer 7, counters not yet added. This is Java's
// own getCurrentPower/getCurrentToughness (Card.java:4407,4450) -- a
// distinct, more confusingly-named thing than getBasePower/getBaseToughness
// (this port's BasePower/BaseToughness) -- and it is what
// CardProperty.java's "basePower"/"baseToughness" valid-string properties
// actually measure (valid.go's compareFieldValue), not the printed value
// the names suggest. Neither this port nor Java's own getNetPower folds in
// the "CARDNAME's power and toughness are switched" keyword here; Power and
// Toughness don't either, so a switched creature's valid-string comparisons
// share the same gap every other switch-blind read on this type already has
// (game-state.md's "Not ported yet").
func (c *Card) layer7Power() (int, bool) {
	base, ok := c.BasePower()
	return foldPT(base, ok, c.PT.effects, func(e PTEffect) (int, bool) { return e.Power, e.HasPower })
}

func (c *Card) layer7Toughness() (int, bool) {
	base, ok := c.BaseToughness()
	return foldPT(base, ok, c.PT.effects, func(e PTEffect) (int, bool) { return e.Toughness, e.HasToughness })
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

// foldPT applies Layer 7's own sub-layers in order (CR 613.4):
// LayerCharacteristic and LayerSetPT each replace the running value --
// unless pick's own bool reports this effect does not set this particular
// dimension at all (PTEffect's own HasPower/HasToughness doc comment has
// the reason), in which case the running value is left exactly as it was --
// LayerModifyPT adds to it, using pick's value regardless of its bool since
// adding zero is always safe. Ties within a layer break by Timestamp,
// ascending -- CR 613.7's own tiebreak once dependency reordering (CR
// 613.8) is not in play, which it cannot be: nothing here has more than one
// continuous effect on the same card yet to depend on another.
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
			if ok {
				value += v
			}
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

// Attachments returns what is attached to this card, in the order it was
// attached. Order decides which Aura's continuous effect applies first when
// two share a timestamp.
func (c *Card) Attachments() []CardID {
	if c.attachments == nil {
		return nil
	}
	return c.attachments.All()
}
