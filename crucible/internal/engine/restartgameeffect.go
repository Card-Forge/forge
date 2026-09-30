package engine

//enginelint:allow id card game player ability condition control zone zonemove parts phase combat effecthelpers effecteffect ventureeffect subgameeffect daytimeeffect mulligan scheduledaction

import "fmt"

// restartZones is RestartGameEffect.java's own restartZones list
// (RestartGameEffect.java:29-30), in its order: the zones whose cards every
// player shuffles into a fresh library. Ante is left out, as in Java; the
// Stack and Command zones are handled separately (restartGameEffect's own
// doc comment).
var restartZones = [...]ZoneType{Battlefield, Library, Graveyard, Hand, Exile}

// restartVariantDecks are the variant zones Player.initVariantsZones
// (Player.java:2918) rebuilds from the registered deck after a restart --
// no registered-deck model here to rebuild them from.
var restartVariantDecks = [...]ZoneType{SchemeDeck, PlanarDeck, AttractionDeck, ContraptionDeck}

// restartGameEffect is RestartGameEffect.java (CR 727, Karn Liberated's
// ultimate, karn_liberated.txt:7 -- the corpus's one real line): every
// player's cards in the restart zones, less a RestrictFromZone$/
// RestrictFromValid$ carve-out, go into their owners' libraries, each
// library is shuffled, the stack, delayed triggers, monarch, initiative,
// day/night and every per-turn and per-game player count reset, and the
// game is marked restarted (Game.Restarted, ADR-0034). The turn driver
// returns on that mark and its caller runs ResumeAfterRestart (driver.go):
// opening hands, mulligans and the activator's first turn. SubAbility$
// (ReturnFromExile, "put those cards onto the battlefield under your
// control") resolves after this against the reset state in the same stack
// resolution -- AbilityUtils.resolveApiAbility's resolve then
// resolveSubAbilities -- so the reset is the last thing this does.
//
// Order is Java's: per player, in seat order (game.getPlayers), the
// player's counts reset, then the cards they control on the battlefield,
// their library, graveyard and hand (and exile, when not the carve-out
// zone), then the carve-out zone's cards RestrictFromValid$ matches with
// that player as the valid string's controller, each moved to the top of
// its owner's library (moveToLibrary(c, 0), RestartGameEffect.java:94),
// then that player's library shuffled. The carve-out filter of a later
// player therefore runs after an earlier player's cards -- the host
// included -- have moved; ExiledWithSource reads a moved host as its last
// battlefield object (hostObjectStamp, game.go), as Java's own
// ability-host object does.
//
// Triggers: Java suppresses ChangesZone and Shuffled triggers for the
// whole reset (RestartGameEffect.java:42-44, 101-102). Moves here go through
// Game.Move and Game.Shuffle directly, which fire no trigger -- the
// effect-driven path that does (moveByEffect, zonemove.go) is not used --
// and every Command-zone effect card is removed before any card moves, so
// no effect card's own zone watch (effectCardsSeeMove) sees one either.
//
// Rejected before anything changes (PORT-8, GO-7):
//   - a card on the Stack zone: restartZones has no Stack and
//     MagicStack.reset (MagicStack.java:97-109) clears only the stack's
//     entries, never the spell cards in its zone; a RestartGame resolving
//     over another spell -- or as a spell -- leaves Java nothing to put
//     that card anywhere.
//   - a Command-zone card that is neither an effect card nor a dungeon
//     (commander, vanguard, scheme, plane, conspiracy): Java puts a
//     commander into the library and rebuilds the variant cards through
//     Player.initVariantsZones, neither ported; NewGame triggers
//     (TriggerZones$ Command on every real line) live only on such cards,
//     so refusing them is also what keeps ResumeAfterRestart's skipped
//     NewGame trigger from dropping anything.
//   - a Planechase game or a card in a variant deck, for the same
//     initVariantsZones reason.
//   - RestrictFromZone$ naming a zone outside restartZones.
//
// Not rejected: Ultimate$ feeds only AchievementTracker
// (AchievementTracker.java:23), no game state.
type restartGameEffect struct{}

func (restartGameEffect) Resolve(g *Game, a *Ability, _ PlayerController) error {
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	leaveZone, hasLeave, err := restartLeaveZone(a)
	if err != nil {
		return err
	}
	leaveValid := "Card"
	if raw, ok := a.Params.Param("RestrictFromValid"); ok {
		leaveValid = raw
	}
	if err := restartRefusals(g); err != nil {
		return err
	}

	activator := a.Controller
	g.resetForRestart()
	for _, pid := range g.Players() {
		if g.Player(pid).Lost {
			continue
		}
		g.resetPlayerForRestart(pid)
		var newLibrary []CardID
		for _, kind := range restartZones {
			if hasLeave && kind == leaveZone {
				continue
			}
			if kind == Battlefield {
				newLibrary = append(newLibrary, g.Zone(Battlefield, pid).CardsIncludingPhasedOut()...)
				continue
			}
			newLibrary = append(newLibrary, g.Zone(kind, pid).Cards()...)
		}
		if hasLeave {
			newLibrary = append(newLibrary, filterValid(g, g.Zone(leaveZone, pid).Cards(), leaveValid, pid, a.Source)...)
		}
		for _, id := range newLibrary {
			g.restartToLibrary(id)
		}
		g.Shuffle(Library, pid)
	}

	// Game.resetTurnOrder, then GameStage.RestartedByKarn and
	// setPlayerTurn(activator) (RestartGameEffect.java:104-107).
	g.turnOrderReversed = false
	g.turn, g.activePlayer, g.activePhase = 0, activator, Untap
	g.restarted, g.restartedBy = true, activator
	return nil
}

// restartLeaveZone reads RestrictFromZone$, ZoneType.smartValueOf's zone:
// ok false when the line names none.
func restartLeaveZone(a *Ability) (ZoneType, bool, error) {
	raw, ok := a.Params.Param("RestrictFromZone")
	if !ok {
		return None, false, nil
	}
	z, found := ZoneByName(raw)
	if !found || !zoneIn(z, restartZones[:]) {
		return None, false, fmt.Errorf("engine: RestartGame: RestrictFromZone$ %q not resolvable yet", raw)
	}
	return z, true, nil
}

// restartRefusals is restartGameEffect's reject-before-acting list (its own
// doc comment has each reason).
func restartRefusals(g *Game) error {
	if g.planechaseActive {
		return fmt.Errorf("engine: RestartGame: a Planechase game's planar deck (Player.initVariantsZones) not resolvable yet")
	}
	for _, pid := range g.Players() {
		if z := existingZone(g, Stack, pid); z != nil && z.Len() > 0 {
			return fmt.Errorf("engine: RestartGame: a card on the stack (restartZones has no Stack, RestartGameEffect.java:29-30) not resolvable yet")
		}
		for _, kind := range restartVariantDecks {
			if z := existingZone(g, kind, pid); z != nil && z.Len() > 0 {
				return fmt.Errorf("engine: RestartGame: a card in the %v (Player.initVariantsZones) not resolvable yet", kind)
			}
		}
		for _, id := range g.Zone(Command, pid).CardsIncludingPhasedOut() {
			if c := g.Card(id); !c.IsEffect && !isDungeon(c) {
				return fmt.Errorf("engine: RestartGame: Command-zone card %q (commander or variant, Player.initVariantsZones) not resolvable yet", c.Def.Name)
			}
		}
	}
	return nil
}

// resetForRestart is RestartGameEffect.java:39-57's game-wide half, and
// the Command-zone removal every player's loop does (:88): delayed
// triggers, the phase handler's extra phases and turns, the untap/upkeep/
// end-of-turn commands (here the effect-card-backed pieces this port
// keeps as Game fields instead: skips, prevention shields, exile play
// grants), the stack, monarch, initiative and day/night. Every effect and
// designation card and every dungeon leaves its Command zone for None
// (exileEffect), Java's removeAllCards.
//
// g.scheduled (ADR-0030's ControlPlayer, landed after this effect's own
// original design) is Java's own getCleanup()/getEndOfCombat() command
// lists in this port's shape -- both cleared here, matching
// RestartGameEffect.java:47-51's clearCommands calls. getBeginOfCombat()'s
// own list is not among those five clearCommands calls -- a Forge bug
// (forge-java-defects.md): a pending Combat$ ControlPlayer grant survives
// a restart in Java, so a boundaryBeginCombat entry is left in g.scheduled
// here too, reproduced rather than fixed (PORT-8).
func (g *Game) resetForRestart() {
	for _, pid := range g.Players() {
		for _, id := range append([]CardID(nil), g.Zone(Command, pid).CardsIncludingPhasedOut()...) {
			g.exileEffect(id)
		}
	}
	g.delayed = nil
	g.extraPhases = [numPhaseTypes][]PhaseType{}
	g.extraTurns = nil
	g.skips = nil
	g.preventShields = nil
	g.exileGrants = nil
	g.combatDamagePrevented = false
	g.combat = Combat{}
	g.combatsThisTurn = 0
	g.skipDamageSteps = false
	g.stack = nil
	g.monarch, g.monarchBeginTurn = NoPlayer, NoPlayer
	g.initiative = NoPlayer
	g.dayTime = DayNeither
	g.previousPlayer, g.previousPlayerSpells = NoPlayer, 0
	kept := g.scheduled[:0]
	for _, sa := range g.scheduled {
		if sa.At == boundaryBeginCombat {
			kept = append(kept, sa)
		}
	}
	g.scheduled = kept
}

// resetPlayerForRestart is RestartGameEffect.java:61-74's per-player half:
// starting life, player counters, spells cast this game, the per-turn
// counts onCleanupPhase clears and the "last turn" ones, completed
// dungeons, the Ring's temptation count and bearer, and skipped turns (an
// effect card in Java). p.controlledBy = nil is p.clearController() (`:74`,
// ADR-0030's ControlPlayer, landed after this effect's own original
// design): every grant naming pid as the controlled player ends. Called
// once per non-lost player in the caller's own loop (restartRefusals'
// !g.Player(pid).Lost check, matching Java's own ingamePlayers,
// Game.java:397-399), so by the time every one of them has been reset, no
// grant naming a non-lost player as controller or controlled remains --
// releaseControlBy (scheduledaction.go) needs no separate call here. A
// lost player's own stale controlledBy, if any, is left as is: Java never
// resets a player who has already left the game either. Commander stats
// and blessing have no counterpart here.
func (g *Game) resetPlayerForRestart(pid PlayerID) {
	p := g.Player(pid)
	p.Life = startingLife
	p.Counters = Counters{}
	p.SpellsCastThisTurn = 0
	p.LandsPlayed, p.LandsPlayedLastTurn = 0, 0
	p.CardsDrawnThisTurn = 0
	p.DrawnThisDrawStep = 0
	p.DescendedThisTurn = false
	p.VenturedThisTurn = 0
	p.LifeGainedTimesThisTurn = 0
	p.TurnsToSkip = 0
	p.completedDungeons = nil
	p.ringTempted = 0
	p.ringBearer = NoCard
	p.controlledBy = nil
}

// restartToLibrary is RestartGameEffect.java:90-94's per-card step: the
// card's intensity resets and it goes to the top of its owner's library
// (moveToLibrary(c, 0)). A card already in that library is moved to its
// top without a zone change, as Java's same-zone move is.
func (g *Game) restartToLibrary(id CardID) {
	c := g.Card(id)
	c.Intensity = 0
	owner := c.Owner
	lib := g.Zone(Library, owner)
	if c.Zone != Library || c.ZoneOwner != owner {
		g.Move(id, Library, owner)
		if c.Zone != Library {
			// A copied spell ceased to exist instead (Move's own
			// ceaseCopiedSpell).
			return
		}
	}
	lib.cards.Remove(id)
	lib.cards.Prepend(id)
}
