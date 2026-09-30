// Subgame: CR 720's "players play a Magic subgame" -- 3 real SP$ Subgame
// lines (Shahrazad, Enter the Dungeon, The Countdown Is at One), every one
// naming RememberPlayers$ and two naming StartingLife$.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/SubgameEffect.java's
// resolve, createSubGame, setCardsInZone and prepareAllZonesSubgame, with
// GameAction.startGame (GameAction.java:2322-2382) as startSubgame. The
// subgame is a second, wholly separate *Game -- its own arena, zones, turn
// structure and SBA state -- driven to completion by the ordinary turn
// driver (Game.Run, ADR-0026) from inside this Resolve call. Nothing of the
// main game's own turn loop is re-entered: the main game is frozen while
// the subgame plays, exactly as Java's synchronous startGame call freezes
// it (SubgameEffect.java:149).

package engine

//enginelint:allow id card game player ability condition control zone parts mulligan turn driver effecthelpers effect

import (
	"fmt"
	"strconv"
)

// subgameTurnCap bounds the subgame's own turn driver. Java has none
// (PhaseHandler.java:1032-1037); Game.Run needs one, and a subgame that
// has not ended by it is an error rather than a guessed outcome (GO-7): the
// main game's own driver can only stop between its steps, and the whole
// subgame is one step of it. A subgame nobody wins ends by decking in about
// one library's size in turns per seat (each seat draws once per its own
// turn), so an N-seat subgame needs roughly N times its largest library's
// size; the cap sits well past a 2-seat, 250-card Battle of Wits library, and
// should scale with len(seats) if a real corpus card ever runs Subgame with
// more players and larger decks than that.
const subgameTurnCap = 1000

// subgameOutsideZones is prepareAllZonesSubgame's outsideZones
// (SubgameEffect.java:88-89), in its order: the main-game zones whose cards
// are outside the subgame (CR 720.4) and become its Sideboard.
var subgameOutsideZones = [...]ZoneType{Hand, Battlefield, Graveyard, Exile, Stack, Sideboard, Ante, Merged}

// subgameExtraDecks are the variant decks prepareAllZonesSubgame copies
// after the Sideboard (SubgameEffect.java:111-121) and shuffles after the
// library (:127-130), and resolve shuffles again once the subgame ends
// (:241-244), in that order.
var subgameExtraDecks = [...]ZoneType{SchemeDeck, PlanarDeck, AttractionDeck, ContraptionDeck}

// subgameEffect resolves SP$ Subgame.
//
// Resolved: StartingLife$ (every seat's starting life, default
// startingLife), RememberPlayers$ Win / NotWin (the main-game players who
// won, or did not win, the subgame, remembered on the host -- a draw makes
// every player NotWin, GameOutcome.isWinner being false for all).
//
// Rejected before anything is built, each a shape the real three lines never
// reach:
//
//   - Condition$/ConditionDefined$: subAbilityConditionMet would otherwise
//     silently pass them (dealDamageEffect's reasoning).
//   - RememberPlayers$ naming anything but Win/NotWin: Java ignores it
//     silently (SubgameEffect.java:174-181), which is a script bug to name,
//     not a shape to reproduce.
//   - A main-game player with a SchemeDeck or PlanarDeck: GameAction.startGame
//     puts the archenemy first (determineFirstTurnPlayer, :2393-2400) and
//     turns a plane face up (initPlane, :2358-2363) for those variants, and
//     this port's game start has neither.
//
// Rejected while the subgame is built, before it plays: a card with
// Companion among a player's outside-the-game cards. Player.assignCompanion
// (Player.java:3109) checks each companion's deck restriction and asks the
// controller; this port has no companion setup. Earlier seats' shuffles have
// already advanced the shared random stream by then; the error ends the
// game regardless.
//
// Rejected after the subgame, before the main game is touched: a card that
// left the subgame's Sideboard (a Wish or Learn fetching it). CR 720.4a moves
// the main-game card it stands for into the Subgame zone and then the
// library (GameAction.java:815-824, SubgameEffect.java:205-212), which needs
// identity across two games' arenas this port does not track.
//
// Nothing to port: Vanguard avatars and Commanders
// (initVariantsZonesSubgame) -- this port has neither, so the main game's
// Command zone never holds one, and the post-game commander move
// (SubgameEffect.java:214-238) and Subgame-zone return (:205-212) always
// find nothing. Merged is scanned but always empty (Mutate is not ported).
// The GameEventSubgameStart/End, DayTimeChanged and Zone view events are UI
// refreshes with no rules content.
type subgameEffect struct{}

func (subgameEffect) Resolve(g *Game, a *Ability, c PlayerController) error {
	if err := rejectParams(a, "Subgame", "Condition", "ConditionDefined"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	life := startingLife
	if raw, ok := a.Params.Param("StartingLife"); ok {
		n, err := strconv.Atoi(raw)
		if err != nil {
			return fmt.Errorf("engine: Subgame: StartingLife$ %q is not an integer", raw)
		}
		life = n
	}
	remember, _ := a.Params.Param("RememberPlayers")
	if remember != "" && remember != "Win" && remember != "NotWin" {
		return fmt.Errorf("engine: Subgame: RememberPlayers$ %q not resolvable yet", remember)
	}

	// createSubGame's maingame.getPlayers(): the players still in the game,
	// in seating order. Subgame seat i is seats[i].
	var seats []PlayerID
	for _, pid := range g.Players() {
		if !g.Player(pid).Lost {
			seats = append(seats, pid)
		}
	}
	for _, pid := range seats {
		for _, kind := range [...]ZoneType{SchemeDeck, PlanarDeck} {
			if z := existingZone(g, kind, pid); z != nil && z.Len() > 0 {
				return fmt.Errorf("engine: Subgame: %v not resolvable yet (no game-start variant setup)", kind)
			}
		}
	}

	sub, sideboard, err := buildSubgame(g, seats, life)
	if err != nil {
		return err
	}
	if err := startSubgame(sub, g.registry, c); err != nil {
		return fmt.Errorf("engine: Subgame: %w", err)
	}
	for _, s := range sideboard {
		// A zone change restamps zoneStamp (put, game.go), so an unchanged
		// stamp is a card that never left, even for a move back.
		if card := sub.Card(s.id); card.Zone != Sideboard || card.zoneStamp != s.stamp {
			return fmt.Errorf("engine: Subgame: a card taken from the subgame's Sideboard (CR 720.4a) not resolvable yet")
		}
	}

	subSeats := sub.Players()
	for i, pid := range seats {
		won := sub.Player(subSeats[i]).Won
		if (remember == "Win" && won) || (remember == "NotWin" && !won) {
			source.Memory.Remember(PlayerEntity(pid))
		}
	}
	// SubgameEffect.java:240-244: player.shuffle(sa), then each variant deck.
	for _, pid := range seats {
		g.Shuffle(Library, pid)
		for _, kind := range subgameExtraDecks {
			if z := existingZone(g, kind, pid); z != nil {
				g.Shuffle(kind, pid)
			}
		}
	}
	return nil
}

// sideboardCard is one card buildSubgame put in a subgame Sideboard, with
// the zoneStamp it was created with.
type sideboardCard struct {
	id    CardID
	stamp uint64
}

// existingZone is g.zones' own entry, nil when the zone was never created:
// Game.Zone creates one on first use, and reading the main game's rarely
// used zones (Ante, Merged, the variant decks) must not grow its zone table.
func existingZone(g *Game, kind ZoneType, owner PlayerID) *Zone {
	return g.zones[zoneKey{kind, owner}]
}

// buildSubgame is createSubGame plus prepareAllZonesSubgame: a fresh Game
// seating seats' players by name, each at life, holding copies of their
// library, their outside-the-game cards (as the Sideboard, CR 720.4) and
// their Attraction and Contraption decks, each shuffled in Java's order. It
// returns the subgame and every Sideboard card it created.
//
// The subgame shares the main game's database and its random stream: Java's
// MyRandom is one stream across both games, so the subgame's shuffles and
// coin flip advance the same sequence the main game continues from. That is
// a pointer held by two *Game values on one goroutine for the length of
// this call, not package-level state (GO-2). Its sink is NewGame's
// DiscardSink: the subgame's CardIDs index its own arena, and an event
// naming one in the main game's stream would name the wrong card.
func buildSubgame(g *Game, seats []PlayerID, life int) (*Game, []sideboardCard, error) {
	names := make([]string, len(seats))
	for i, pid := range seats {
		names[i] = g.Player(pid).Name
	}
	sub := NewGame(g.db, g.rand, names)
	sub.registry = g.registry
	subSeats := sub.Players()
	var sideboard []sideboardCard
	for i, pid := range seats {
		sp := subSeats[i]
		// Player.setStartingLife (Player.java:423-428) sets life too.
		sub.Player(sp).Life = life
		copyCardsInto(g, sub, g.Zone(Library, pid).CardsIncludingPhasedOut(), sp, Library)

		// CR 720.4: getCardsInOwnedBy(outsideZones, p) (Game.java:645-651),
		// each zone's cards across every player still in the game, phased
		// out included (getCardsIncludePhasingIn, Game.java:648), filtered
		// to those p owns.
		var outside []CardID
		for _, kind := range subgameOutsideZones {
			holders := seats
			if kind == Stack {
				holders = []PlayerID{NoPlayer}
			}
			for _, holder := range holders {
				z := existingZone(g, kind, holder)
				if z == nil {
					continue
				}
				for _, id := range z.CardsIncludingPhasedOut() {
					if g.Card(id).Owner == pid {
						outside = append(outside, id)
					}
				}
			}
		}
		created := copyCardsInto(g, sub, outside, sp, Sideboard)
		for _, id := range created {
			if sub.Card(id).HasKeyword("Companion") {
				return nil, nil, fmt.Errorf("engine: Subgame: companion %q outside the subgame not resolvable yet", sub.Card(id).Def.Name)
			}
		}
		for _, id := range created {
			sideboard = append(sideboard, sideboardCard{id, sub.Card(id).zoneStamp})
		}

		for _, kind := range subgameExtraDecks {
			if z := existingZone(g, kind, pid); z != nil {
				copyCardsInto(g, sub, z.Cards(), sp, kind)
			}
		}
		sub.Shuffle(Library, sp)
		for _, kind := range subgameExtraDecks {
			if existingZone(sub, kind, sp) != nil {
				sub.Shuffle(kind, sp)
			}
		}
	}
	return sub, sideboard, nil
}

// copyCardsInto is setCardsInZone (SubgameEffect.java:41-57) without the
// maingame mapping: a fresh subgame card from each main-game card's printed
// script (Card.fromPaperCard), owned by the subgame seat, in order. Tokens
// and copied spells have no paper card and are skipped; so are effect
// cards, which never sit in a zone this reads (they live in Command) and
// have no paper card either. Returns the new cards.
func copyCardsInto(g, sub *Game, from []CardID, owner PlayerID, kind ZoneType) []CardID {
	var out []CardID
	for _, id := range from {
		card := g.Card(id)
		if card.IsToken || card.IsCopiedSpell || card.IsEffect {
			continue
		}
		out = append(out, sub.NewCard(card.Def, owner, kind))
	}
	return out
}

// startSubgame is GameAction.startGame(null, null) for a game with no
// previous outcome: CR 103.2's coin flip and the starting-player choice
// (determineFirstTurnPlayer's isFirstGame branch, Aggregates.random over the
// players), opening hands drawn with no further shuffle (buildSubgame
// already shuffled), London mulligans, then the whole game through the turn
// driver. runOpeningHandActions and the NewGame trigger have no port in the
// main game's own start either (DealOpeningHands), so neither runs here.
func startSubgame(sub *Game, reg *Registry, c PlayerController) error {
	players := sub.Players()
	decider := players[sub.rand.Int32n(int32(len(players)))]
	first := c.ChooseStartingPlayer(sub, decider, true)
	if first == NoPlayer || int(first) >= len(sub.players) {
		return fmt.Errorf("starting player %d is not a subgame seat", first)
	}
	for _, pid := range players {
		drawOpeningHand(sub, pid)
	}
	// Java breaks out here for a concession during the mulligan prompt;
	// nothing in this port concedes, and an ended game's turn driver
	// returns at once anyway.
	PerformMulligans(sub, c, first)
	sub.StartTurn(first, c)
	// GameAction.startGame's own do-while(RestartedByKarn): a RestartGame
	// resolved inside the subgame restarts the subgame (ADR-0034).
	for {
		if err := sub.Run(reg, c, subgameTurnCap); err != nil {
			return err
		}
		if !sub.Restarted() {
			break
		}
		if err := sub.ResumeAfterRestart(c); err != nil {
			return err
		}
	}
	if !sub.Over() {
		return fmt.Errorf("subgame did not end within %d turns", subgameTurnCap)
	}
	return nil
}
