// Players.

package engine

import (
	"sort"

	"github.com/jczastkiewicz/crucible/internal/keyword"
)

// Player is one player in one game.
//
// Like Card it holds no back-reference. Its library, hand and graveyard live
// in the game's zone table rather than on the player, because a zone is
// addressed by (type, owner) from a dozen places that have the pair and not
// the player.
type Player struct {
	// ID is this player's own handle.
	ID PlayerID
	// Name is for reports and event logs, never for identity (GO-9).
	Name string
	// Life is the current total. It goes negative before the state-based
	// action that ends the game runs, so it is signed.
	Life int
	// Turn is how many turns this player has taken, which several card scripts
	// count.
	Turn int
	// Lost records that the player has left the game, and Won that they won
	// it. Both can be false at once; both true is an engine invariant breach.
	Lost bool
	Won  bool
	// Speed is Player.speed (CR 702.179): 0 until "Start your engines!"
	// gives the player one, then 1-4, changed by ChangeSpeed.
	Speed int
	// NamedCard is Player.namedCard: the name this player last named with
	// NameCard (read by NamedByRememberedPlayer).
	NamedCard string
	// SpellsCastThisTurn counts the spells this player cast this turn
	// (MagicStack.getSpellsCastThisTurn); day/night reads the count the
	// previous turn's active player ended with (CR 726.3a).
	SpellsCastThisTurn int
	// Counters is player-level counters -- poison chief among them, which is
	// what CR 704.5c checks. The same type as a card's, because nothing about
	// "a count that is never stored at zero" is specific to what holds it.
	Counters Counters
	// DrewFromEmptyLibrary records an attempted draw with nothing to draw.
	// CheckStateBasedActions reads it for CR 704.5b and clears it either way
	// -- a one-shot check, the same as Java's triedToDrawFromEmptyLibrary --
	// so a player who survives this check (because a replacement effect
	// intervenes, once one exists) does not lose on the next one for a draw
	// that already happened.
	DrewFromEmptyLibrary bool
	// ManaPool is this player's own floating mana (CR 106.4, mana.go).
	// Emptied every phase/step transition (CR 500.4, emptyManaPools,
	// turn.go), not something a card ability triggers.
	ManaPool Pool
	// LandsPlayed is how many lands this player has played this turn (CR
	// 305.2), read by PlayLand's own per-turn limit. LandsPlayedLastTurn is
	// last turn's count, Java's own landsPlayedLastTurn -- no card in scope
	// reads it yet (a replacement effect keyed on "if you've played a land
	// this turn" would), but cleanupStep (turn.go) rolls it forward every
	// turn regardless, the same "reset land-bearing state whether or not a
	// reader exists yet" position CR 500.4's own mana-pool emptying is in.
	// Both reset for every player at cleanup, not just the active one (CR
	// 305.2's own scope: any player who played a land this turn, and every
	// game player's Game.onCleanupPhase in Java runs the same reset over
	// every registered player).
	LandsPlayed         int
	LandsPlayedLastTurn int
	// CardsDrawnThisTurn is how many cards this player has drawn this turn
	// (Java's own numDrawnThisTurn, Player.java), read by checkDrawnTriggers
	// (trigger.go) for Mode$ Drawn's own Number$ param -- "whenever you draw
	// your Nth card each turn." Incremented per card in DrawCards (below),
	// the identical per-turn-counter shape LandsPlayed already has, reset for
	// every player at cleanup (cleanupStep, turn.go) the same way.
	CardsDrawnThisTurn int
	// DescendedThisTurn is CR's own "descend" tracker (Java's own
	// Player.descended, Player.java) -- whether a permanent card has been put
	// into this player's graveyard from anywhere this turn, read by
	// matchesPlayerProperty's own "descended" case (valid.go) for
	// ValidPlayer$ You.descended (Mode$ Phase's own real corpus shape).
	// Java's own field is an int count (getDescended() < 1); this port only
	// ever asks whether it happened at all, so a bool is enough. Set in
	// Game.Move (game.go) whenever a permanent, non-token card is moved into
	// Graveyard, reset for every player at cleanup (cleanupStep, turn.go) the
	// identical way LandsPlayed/CardsDrawnThisTurn already are.
	DescendedThisTurn bool
	// DrawnThisDrawStep is how many cards this player has drawn while the
	// game's current phase is Draw (Java's own numDrawnThisDrawStep,
	// Player.java) -- distinct from CardsDrawnThisTurn's whole-turn count:
	// reset for every player at the start of each Draw step (drawStep,
	// turn.go, ReplaceDraw's own PhaseHandler.java:268-271 reset), never at
	// cleanup, and incremented for whichever player actually draws (drawOneCard,
	// turn.go) regardless of whose turn it is -- a non-active player who draws
	// an extra card while the active player's own Draw step is still current
	// counts too, the identical scope Java's own unconditional
	// game.getPhaseHandler().is(PhaseType.DRAW) check has. Read by
	// notFirstCardInDrawStepExempts (replacement.go) for
	// NotFirstCardInDrawStep$'s own "except the first one they draw in each
	// of their draw steps" exemption.
	DrawnThisDrawStep int
	// LifeGainedTimesThisTurn is how many separate times this player has
	// gained life this turn (Java's own lifeGainedTimesThisTurn, Player.java)
	// -- a count of EVENTS, not amount (LifeGainedThisTurn's own shape, not
	// tracked here since no real corpus line needs it) -- read by
	// checkLifeGainedTriggers (trigger.go) for Mode$ LifeGained's own
	// FirstTime$: "whenever you gain life for the first time each turn."
	// Incremented once per gainLifeEffect resolution (gainlifeeffect.go),
	// the only life-gain call site this port has, reset for every player at
	// cleanup (cleanupStep, turn.go) the identical way LandsPlayed/
	// CardsDrawnThisTurn already are.
	LifeGainedTimesThisTurn int

	// TurnsToSkip is how many of this player's next turns are skipped
	// (SkipTurn): Java's BeginTurn replacement effect that counts itself down.
	TurnsToSkip int
	// Rules is Layer 8's own continuous effects currently affecting this
	// player (rulesmod.go), recomputed fresh every CheckStateBasedActions
	// pass (applyContinuousRules, continuous.go) -- HandSizeLimit/
	// LandPlayLimit, below, are what folds it against the printed defaults.
	Rules RulesMod

	// VenturedThisTurn counts this player's ventures into the dungeon this
	// turn (Player.venturedThisTurn), read by the VenturedThisTurn player
	// property (Keen-Eared Sentry's CantVenture); reset for every player at
	// cleanup (Player.onCleanupPhase).
	VenturedThisTurn int
	// CrankCounter is Player.crankCounter (Java's own field, default 3): the
	// CRANK! counter's own sprocket position (1, 2 or 3), advanced by
	// AdvanceCrank (advancecrankeffect.go). Set to 3 at game creation
	// (NewGame, game.go) the same way Java's own field initializer does, so
	// the first AdvanceCrank ever resolved moves it to 1 -- CR 725's own
	// Contraption subtype rules.
	CrankCounter int

	// completedDungeons is every dungeon this player has completed, in
	// completion order (Player.completedDungeons, CR 309.7). The cards
	// themselves have ceased to exist, parked in None.
	completedDungeons []CardID

	// monarchEffect is this player's "The Monarch" effect card
	// (Player.monarchEffect), NoCard until they first become the monarch.
	// One card per player for the whole game, as in Java: losing the
	// monarchy parks it in None and becoming the monarch again puts the same
	// card back in the Command zone (becomemonarcheffect.go).
	monarchEffect CardID
	// initiativeEffect is this player's "The Initiative" effect card
	// (Player.initiativeEffect), reused the way monarchEffect is
	// (takeinitiativeeffect.go).
	initiativeEffect CardID
	// theRing is this player's "The Ring" effect card (Player.theRing),
	// NoCard until the Ring first tempts them; it stays in the Command zone
	// for the rest of the game (ringtemptsyoueffect.go).
	theRing CardID
	// ringTempted is how many times the Ring has tempted this player
	// (Player.numRingTemptedYou); The Ring carries the abilities of every
	// level up to it (Player.setRingLevel, at most four).
	ringTempted int
	// ringBearer is this player's chosen Ring-bearer (Player.ringBearer,
	// CR 701.54a), NoCard for none. It is cleared as the creature leaves
	// the battlefield or changes controller (loseRingBearer); Game.RingBearer
	// also answers NoCard for a bearer another player controls.
	ringBearer CardID
	// planarDie is this player's "Planar Dice" effect card
	// (Player.createPlanechaseEffects), NoCard until they first roll the
	// planar die. Java creates one per player at the start of a Planechase
	// game; this port has no match setup, so the roller's is made at their
	// first roll, before its trigger could first fire -- and only the
	// roller's, whose ValidPlayer$ You is the only one a roll can match
	// (rollplanardiceeffect.go).
	planarDie CardID
	// lossHandled records that Game.onPlayerLost has run for this player:
	// Java runs it once, as the loss is awarded (GameAction.
	// checkGameOverCondition), passing the monarchy and the initiative on
	// (CR 724.4, 725.4, becomemonarcheffect.go), then drops the player from
	// ingamePlayers.
	lossHandled bool
	// KeywordMod is CR 613's Layer 6 for a Player entity -- the same
	// continuous ability-granting type a Card's own KeywordMod
	// (keywordmod.go) already is, reused unchanged: nothing about "which
	// keywords currently apply, folded in timestamp order" is specific to a
	// card. Java's own PlayerFactoryUtil.addStaticAbility-shaped grant
	// (`Affected$ You | AddKeyword$ Hexproof`, Leyline of Sanctity's own
	// line) writes here through applyContinuousKeyword (continuous.go),
	// cleared and rebuilt every Layer 6 pass the identical way a card's own
	// is.
	KeywordMod KeywordMod
}

// HasKeyword reports whether the player currently carries the named
// keyword -- Card.HasKeyword's own player-entity counterpart, Java's own
// Player.hasKeyword(String). A player has no printed face to fold onto,
// unlike a card: every line here came from a continuous effect.
func (p *Player) HasKeyword(name string) bool {
	for _, line := range p.KeywordLines() {
		if keyword.Parse(line).Name == name {
			return true
		}
	}
	return false
}

// KeywordLines is every keyword line p currently carries -- Card.
// KeywordLines' own player-entity counterpart, folded in Timestamp order
// the identical way (KeywordsChange.applyKeywords). No printed face to
// start from: a player's own keyword lines are entirely Layer 6's doing.
func (p *Player) KeywordLines() []string {
	return p.KeywordMod.fold(nil)
}

// HandSizeLimit folds Layer 8's own SetMaxHandSize$/RaiseMaxHandSize$
// effects on top of base (CR 103.4's default maximum hand size, MaxHandSize,
// turn.go -- passed in rather than read directly so this file need not
// depend on turn.go's own group, `player`'s allow-list staying acyclic) --
// Player.setMaxHandSize/setUnlimitedHandSize ported: a SetMaxHandSize$
// effect REPLACES the running limit (and the unlimited flag with it), a
// RaiseMaxHandSize$ effect ADDS to whatever the running limit already is --
// both applied in Timestamp order, foldPT's own combine convention (card.go)
// reused here for the one other layer this port's own effects can conflict
// within. Order matters only for the rare case of two SetMaxHandSize$
// effects active on the same player at once; Java's own iteration order for
// that case is not itself pinned down anywhere doc.go can cite, so
// Timestamp is this port's own deliberate, documented choice, not a
// rediscovery of Java's.
//
// hasLimit is false once the LAST effect touching hand size at all set
// `Unlimited` (`SetMaxHandSize$ Unlimited`, 33 of the corpus's 43 real
// SetMaxHandSize$ lines) -- cleanupStep's own hand-size check (turn.go)
// skips the discard entirely when this is false, CR 103.4a's "no maximum
// hand size."
func (p *Player) HandSizeLimit(base int) (limit int, hasLimit bool) {
	sorted := append([]RulesEffect(nil), p.Rules.effects...)
	sort.Slice(sorted, func(i, j int) bool { return sorted[i].Timestamp < sorted[j].Timestamp })
	limit, hasLimit = base, true
	for _, e := range sorted {
		if e.HasSetHandSize {
			hasLimit = !e.SetHandSizeUnlimited
			if hasLimit {
				limit = e.SetHandSize
			}
		}
		if e.HasRaiseHandSize {
			limit += e.RaiseHandSize
		}
	}
	return limit, hasLimit
}

// LandPlayLimit folds Layer 8's own AdjustLandPlays$ effects on top of base
// (CR 305.2's default one land per turn, land.go's own maxLandPlays --
// passed in for the identical acyclic-dependency reason HandSizeLimit's own
// base parameter is) -- Player.getMaxLandPlays's own unconditional sum
// ported: every AdjustLandPlays$ effect adds to the running limit
// regardless of order (unlike HandSizeLimit's own Set/Raise interaction,
// there is no "replace" form here to make order matter), and
// Player.getMaxLandPlaysInfinite's own "any one active effect makes it
// unlimited" OR -- an `Unlimited` effect does not need to be the last one
// applied, or the only one, to win.
func (p *Player) LandPlayLimit(base int) (limit int, unlimited bool) {
	limit = base
	for _, e := range p.Rules.effects {
		if !e.HasAdjustLandPlays {
			continue
		}
		if e.AdjustLandPlaysUnlimited {
			unlimited = true
			continue
		}
		limit += e.AdjustLandPlays
	}
	return limit, unlimited
}
