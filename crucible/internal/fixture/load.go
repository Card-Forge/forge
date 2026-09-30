// Building a *engine.Game from a parsed fixture.

package fixture

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/engine"
	"github.com/jczastkiewicz/crucible/internal/mana"
	"github.com/jczastkiewicz/crucible/pkg/javarand"
)

// Loaded is what Load produces: the engine.Game, plus the one directive that
// still has no home in it. Turn, ActivePlayer and ActivePhase used to live
// here too -- they moved onto Game itself once turn.go gave them one
// (porting/port-log/game-state.md's "turn structure" section); read them
// from Game.Turn, Game.ActivePlayer and Game.ActivePhase.
type Loaded struct {
	Game *engine.Game

	// ActivePhaseAdvance and PhaseAdvanced are a fixture-only convenience --
	// a second phase to fast-forward to after setup -- not real game state,
	// so they have no Game equivalent and are not applied by Load yet.
	ActivePhaseAdvance engine.PhaseType
	PhaseAdvanced      bool

	// CardByFixtureID is the same fixture-declared Id: a card carried into
	// AttachedTo:/RememberedCards:/Imprinting: resolution, kept around after
	// Load returns. A scenario's actions.log needs it for the same reason
	// those annotations do: it has to name a specific card, and CardID is
	// not something a fixture author can predict ahead of a shuffle.
	CardByFixtureID map[int]engine.CardID

	// Unapplied records every value Load recognised the shape of but had
	// nothing to apply it to: a card annotation for a mechanic that is not
	// modeled yet (Renowned, ChosenColor, ...), or a player-level field with
	// no corresponding engine.Player field (PersistentMana, Speed, ...). Silently dropping these would make a fixture that names,
	// say, a Monstrous creature pass while testing something other than what
	// it says.
	Unapplied []string
}

// Load builds a game from a parsed fixture.
//
// Ported from GameState.applyToGame and processCardsForZone, with one
// structural difference: Java applies fixture state onto a Game a Match
// already built, with its players already seated. Crucible has no Match yet,
// so Load builds the Game itself, seating one player per Named slot in slot
// order -- human, ai, p2..p9 -- which is the same seating order every fixture
// already writes in (PORT-1).
func Load(st *State, db *compile.DB, rng *javarand.Rand) (*Loaded, error) {
	var names []string
	var slots []int
	for i, p := range st.Players {
		if p.Named {
			names = append(names, slotName(i))
			slots = append(slots, i)
		}
	}
	g := engine.NewGame(db, rng, names)

	var slotToID [MaxPlayers]engine.PlayerID
	for i, slot := range slots {
		slotToID[slot] = g.Players()[i]
	}

	l := &Loaded{
		Game:               g,
		ActivePhaseAdvance: st.ActivePhaseAdvance,
		PhaseAdvanced:      st.PhaseAdvanced,
	}
	active := engine.NoPlayer
	if st.ActivePlayer != "" {
		slot, ok := playerSlot(st.ActivePlayer)
		if !ok || slotToID[slot] == engine.NoPlayer {
			return nil, fmt.Errorf("activeplayer %q: no such player", st.ActivePlayer)
		}
		active = slotToID[slot]
	}
	g.SetTurnState(st.Turn, active, st.ActivePhase)
	g.SetOver(st.Over)

	ld := &loader{game: g, slotToID: slotToID, idToCard: map[int]engine.CardID{}}
	for _, slot := range slots {
		ps := &st.Players[slot]
		pid := slotToID[slot]
		g.Player(pid).Life = ps.Life
		g.Player(pid).Lost = ps.Lost
		g.Player(pid).Won = ps.Won
		if ps.Counters != "" {
			if err := applyCounters(&g.Player(pid).Counters, ps.Counters); err != nil {
				return nil, fmt.Errorf("%s counters: %w", slotName(slot), err)
			}
		}
		if ps.ManaPool != "" {
			if err := applyManaPool(&g.Player(pid).ManaPool, ps.ManaPool); err != nil {
				return nil, fmt.Errorf("%s manapool: %w", slotName(slot), err)
			}
		}
		if ps.PersistentMana != "" {
			l.Unapplied = append(l.Unapplied, fmt.Sprintf("%s: persistent mana -- engine.Pool has no persistence tracking yet (CR 500.4's own emptying applies to every kind of floating mana this port has)", slotName(slot)))
		}
		g.Player(pid).LandsPlayed = ps.LandsPlayed
		g.Player(pid).LandsPlayedLastTurn = ps.LandsPlayedLastTurn
		for _, z := range []struct {
			text string
			kind engine.ZoneType
		}{
			{ps.Battlefield, engine.Battlefield},
			{ps.Hand, engine.Hand},
			{ps.Graveyard, engine.Graveyard},
			{ps.Library, engine.Library},
			{ps.Exile, engine.Exile},
			{ps.Command, engine.Command},
			{ps.Sideboard, engine.Sideboard},
		} {
			if err := ld.zone(z.text, z.kind, pid); err != nil {
				return nil, err
			}
		}
		// GameState.setupPlayerState, after the player's cards: the count,
		// then "The Ring" with every level up to it -- no Ring card when the
		// count is zero.
		g.SetRingTemptedYou(pid, ps.NumRingTemptedYou)
	}

	if err := ld.resolveRefs(); err != nil {
		return nil, err
	}
	for _, d := range []struct {
		key, name string
		set       func(engine.PlayerID)
	}{
		{"monarch", st.Monarch, g.SetMonarch},
		{"initiative", st.Initiative, g.SetInitiative},
	} {
		if d.name == "" {
			continue
		}
		slot, ok := playerSlot(d.name)
		if !ok || slotToID[slot] == engine.NoPlayer {
			return nil, fmt.Errorf("%s %q: no such player", d.key, d.name)
		}
		d.set(slotToID[slot])
	}
	if st.RemoveSummoningSickness {
		for _, p := range slots {
			for _, id := range g.Zone(engine.Battlefield, slotToID[p]).Cards() {
				g.Card(id).SummonSick = false
			}
		}
	}
	l.Unapplied = append(l.Unapplied, ld.unapplied...)
	l.CardByFixtureID = ld.idToCard
	return l, nil
}

// slotName is the inverse of playerSlot: the name Load seats a player under.
func slotName(slot int) string {
	switch slot {
	case 0:
		return "human"
	case 1:
		return "ai"
	default:
		return fmt.Sprintf("p%d", slot)
	}
}

// loader carries the state that spans every zone and every player while
// Load builds the game: the fixture's declared card IDs, and the
// cross-references that can only resolve once every card exists.
type loader struct {
	game     *engine.Game
	slotToID [MaxPlayers]engine.PlayerID
	idToCard map[int]engine.CardID

	// Resolved after every card in the fixture has been created, and in the
	// order the fixture declared them: two auras naming the same host must
	// attach in that order, because it is what breaks a tie between their
	// continuous effects when both share a timestamp (GO-12).
	attaches  []attachRef
	remembers []refList
	imprints  []refList
	// exiledWith pairs a card with the fixture id of the host it was
	// exiled with (ExiledWith:), resolved with the rest.
	exiledWith []attachRef
	unapplied  []string
}

type attachRef struct {
	card   engine.CardID
	hostID int
}

type refList struct {
	card engine.CardID
	ids  []int
}

// zone creates every card a zone's raw text names, in the order written.
func (ld *loader) zone(text string, kind engine.ZoneType, owner engine.PlayerID) error {
	if text == "" {
		return nil
	}
	for _, entry := range strings.Split(text, ";") {
		entry = strings.TrimSpace(entry)
		if entry == "" {
			continue
		}
		if err := ld.card(entry, kind, owner); err != nil {
			return err
		}
	}
	return nil
}

// card creates one card from a zone entry -- the card's name, then its
// `|`-separated annotations -- and files any cross-references it makes for
// resolveRefs to apply once every card exists.
func (ld *loader) card(entry string, kind engine.ZoneType, owner engine.PlayerID) error {
	fields := strings.Split(entry, "|")
	name := fields[0]
	if strings.HasPrefix(name, "t:") || strings.HasPrefix(name, "T:") {
		ld.unapplied = append(ld.unapplied, fmt.Sprintf("%s: token cards are not loaded yet", name))
		return nil
	}
	def, ok := ld.game.DB().Card(name)
	if !ok {
		return fmt.Errorf("card %q: not in the database", name)
	}
	id := ld.game.NewCard(def, owner, kind)
	c := ld.game.Card(id)

	var remembered, imprinted []int
	for _, info := range fields[1:] {
		switch {
		// Set: and Art: pick a printing. Crucible plays cards, not printings
		// (porting/port-log/deck-serializer.md), so both parse and are
		// dropped rather than reported -- that is an established decision,
		// not a gap.
		case strings.HasPrefix(info, "Set:"), strings.HasPrefix(info, "Art:"):
		// Tapped and SummonSick match on prefix with no colon required,
		// which is Java's own info.startsWith("Tapped") -- reproduced
		// because a fixture written either way has to load the same in both
		// engines (PORT-7).
		case strings.HasPrefix(info, "Tapped"):
			c.Tapped = true
		case strings.HasPrefix(info, "SummonSick"):
			c.SummonSick = true
		case strings.HasPrefix(info, "PhasedOut"):
			if err := ld.phasedOut(id, kind, info); err != nil {
				return fmt.Errorf("%s: %w", name, err)
			}
		case strings.HasPrefix(info, "Counters:"):
			if err := applyCounters(&c.Counters, strings.TrimPrefix(info, "Counters:")); err != nil {
				return fmt.Errorf("%s: %w", name, err)
			}
		case strings.HasPrefix(info, "Damage:"):
			n, err := strconv.Atoi(strings.TrimPrefix(info, "Damage:"))
			if err != nil {
				return fmt.Errorf("%s: damage %q: %w", name, info, err)
			}
			c.Damage.Mark(n, false)
		case strings.HasPrefix(info, "Id:"):
			n, err := strconv.Atoi(strings.TrimPrefix(info, "Id:"))
			if err != nil {
				return fmt.Errorf("%s: id %q: %w", name, info, err)
			}
			ld.idToCard[n] = id
		case strings.HasPrefix(info, "AttachedTo:"), strings.HasPrefix(info, "Attaching:"):
			n, err := strconv.Atoi(info[strings.Index(info, ":")+1:])
			if err != nil {
				return fmt.Errorf("%s: attach %q: %w", name, info, err)
			}
			ld.attaches = append(ld.attaches, attachRef{id, n})
		case strings.HasPrefix(info, "Owner:"):
			slot, ok := playerSlot(strings.ToLower(strings.TrimSpace(strings.TrimPrefix(info, "Owner:"))))
			if !ok {
				return fmt.Errorf("%s: owner %q: no such player", name, info)
			}
			c.Owner = ld.slotToID[slot]
		case strings.HasPrefix(info, "Protector:"):
			// Crucible-only, the same category as lost=/won=/over= -- CR
			// 704.5w's protector (Card.ProtectingPlayer, game-state.md's
			// "Combat") has no equivalent key in Java's own GameState text
			// format at all, not even one this port chose to diverge from.
			slot, ok := playerSlot(strings.ToLower(strings.TrimSpace(strings.TrimPrefix(info, "Protector:"))))
			if !ok {
				return fmt.Errorf("%s: protector %q: no such player", name, info)
			}
			c.ProtectingPlayer = ld.slotToID[slot]
		case strings.HasPrefix(info, "UnlockedRoom:"):
			// GameState.java:1419: the door unlocks with every trigger
			// suppressed (GameState.java:617). Only a Room on the
			// battlefield has doors.
			d, ok := engine.DoorByName(strings.TrimPrefix(info, "UnlockedRoom:"))
			if !ok {
				return fmt.Errorf("%s: %q: not LeftSplit or RightSplit", name, info)
			}
			if !ld.game.LoadUnlockedDoor(id, d) {
				ld.unapplied = append(ld.unapplied, fmt.Sprintf("%s: %s off a Room on the battlefield", name, info))
			}
		case strings.HasPrefix(info, "IsRingBearer"):
			// GameState.java: player.setRingBearer(c), the player whose
			// zone the entry is in.
			ld.game.SetRingBearer(owner, id)
		case strings.HasPrefix(info, "RememberedCards:"):
			ids, err := parseIDList(strings.TrimPrefix(info, "RememberedCards:"))
			if err != nil {
				return fmt.Errorf("%s: %w", name, err)
			}
			remembered = ids
		case strings.HasPrefix(info, "ExiledWith:"):
			// GameState.java:1398/771-781: resolved once every card exists.
			hostID, err := strconv.Atoi(strings.TrimPrefix(info, "ExiledWith:"))
			if err != nil {
				return fmt.Errorf("%s: %q: %w", name, info, err)
			}
			ld.exiledWith = append(ld.exiledWith, attachRef{id, hostID})
		case strings.HasPrefix(info, "Imprinting:"):
			ids, err := parseIDList(strings.TrimPrefix(info, "Imprinting:"))
			if err != nil {
				return fmt.Errorf("%s: %w", name, err)
			}
			imprinted = ids
		default:
			ld.unapplied = append(ld.unapplied, fmt.Sprintf("%s: %s", name, info))
		}
	}
	if remembered != nil {
		ld.remembers = append(ld.remembers, refList{id, remembered})
	}
	if imprinted != nil {
		ld.imprints = append(ld.imprints, refList{id, imprinted})
	}
	return nil
}

// phasedOut applies PhasedOut:<player> (GameState.java:1302-1304): the card
// is phased out, to phase back in on that player's untap step. The player is
// parsePlayerString's own vocabulary (GameState.java:240-249) -- HUMAN and AI
// for the first and second seat, P<digit> for a seat index, all indexes into
// the seated players, not fixture slots -- except that an unrecognized value is an
// error here rather than Java's silent fallback to the first player: a
// fixture that relies on the fallback has a typo, and loading it as seat 0
// would hide it. Java writes the key only for a Battlefield card; anywhere
// else it is reported unapplied.
func (ld *loader) phasedOut(id engine.CardID, kind engine.ZoneType, info string) error {
	if kind != engine.Battlefield {
		ld.unapplied = append(ld.unapplied, fmt.Sprintf("%s: phasing outside the battlefield", info))
		return nil
	}
	_, value, _ := strings.Cut(info, ":")
	seat := -1
	switch {
	case strings.EqualFold(value, "HUMAN"):
		seat = 0
	case strings.EqualFold(value, "AI"):
		seat = 1
	case len(value) >= 2 && value[0] == 'P' && value[1] >= '0' && value[1] <= '9':
		seat = int(value[1] - '0')
	}
	players := ld.game.Players()
	if seat < 0 || seat >= len(players) {
		return fmt.Errorf("phased out %q: no such player", info)
	}
	return ld.game.SetPhasedOut(id, players[seat])
}

// resolveRefs applies every cross-reference collected while cards were being
// created. It runs once every card in the fixture exists, because a card can
// reference one declared later in the file.
func (ld *loader) resolveRefs() error {
	for _, a := range ld.attaches {
		host, ok := ld.idToCard[a.hostID]
		if !ok {
			return fmt.Errorf("attachedto %d: no card has that id", a.hostID)
		}
		ld.game.Attach(a.card, host)
	}
	for _, r := range ld.remembers {
		for _, refID := range r.ids {
			target, ok := ld.idToCard[refID]
			if !ok {
				return fmt.Errorf("rememberedcards %d: no card has that id", refID)
			}
			ld.game.Card(r.card).Memory.Remember(engine.CardEntity(target))
		}
	}
	for _, r := range ld.imprints {
		for _, refID := range r.ids {
			target, ok := ld.idToCard[refID]
			if !ok {
				return fmt.Errorf("imprinting %d: no card has that id", refID)
			}
			ld.game.Card(r.card).Memory.Imprint(target)
		}
	}
	for _, e := range ld.exiledWith {
		host, ok := ld.idToCard[e.hostID]
		if !ok {
			return fmt.Errorf("exiledwith %d: no card has that id", e.hostID)
		}
		ld.game.SetExiledWith(e.card, host)
	}
	return nil
}

// applyCounters parses Counters:'s "TYPE=n,TYPE=n" value, the same format
// Player-level counters use.
// applyManaPool is GameState.java's own manapool= shape (processManaPool/
// updateManaPool): one space-separated token per floating mana, "W"/"U"/"B"
// /"R"/"G" for the five colors and "C" for colorless (MagicColor.Color's own
// short names) -- "W W U" is two white and one blue, not a mana cost's "2W"
// shorthand, so this reads each token as a bare color letter rather than
// handing the whole value to mana.Parse.
func applyManaPool(pool *engine.Pool, value string) error {
	for _, tok := range strings.Fields(value) {
		switch tok {
		case "W":
			pool.Add(mana.White, 1)
		case "U":
			pool.Add(mana.Blue, 1)
		case "B":
			pool.Add(mana.Black, 1)
		case "R":
			pool.Add(mana.Red, 1)
		case "G":
			pool.Add(mana.Green, 1)
		case "C":
			pool.AddColorless(1)
		default:
			return fmt.Errorf("mana pool token %q: not one of W/U/B/R/G/C", tok)
		}
	}
	return nil
}

func applyCounters(counters *engine.Counters, value string) error {
	for _, pair := range strings.Split(value, ",") {
		typ, n, ok := strings.Cut(pair, "=")
		if !ok {
			return fmt.Errorf("counter %q: missing =", pair)
		}
		count, err := strconv.Atoi(strings.TrimSpace(n))
		if err != nil {
			return fmt.Errorf("counter %q: %w", pair, err)
		}
		counters.Add(engine.CounterType(strings.TrimSpace(typ)), count)
	}
	return nil
}

// parseIDList parses a comma-separated list of fixture card IDs.
func parseIDList(value string) ([]int, error) {
	parts := strings.Split(value, ",")
	ids := make([]int, len(parts))
	for i, p := range parts {
		n, err := strconv.Atoi(strings.TrimSpace(p))
		if err != nil {
			return nil, fmt.Errorf("id list %q: %w", value, err)
		}
		ids[i] = n
	}
	return ids, nil
}
