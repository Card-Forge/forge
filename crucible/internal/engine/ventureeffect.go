// Venture: venturing into the dungeon (CR 701.49), the dungeon card in the
// Command zone, its RoomEntered triggers and dungeon completion (CR 309).

package engine

//enginelint:allow id zone card game player ability defined condition control parts valid trigger effecteffect effecthelpers earthbendeffect becomemonarcheffect

import (
	"fmt"
	"sort"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// ventureEffect is VentureEffect.java: each targeted or Defined$ player
// (default the activator) still in the game ventures into the dungeon --
// into Dungeon$'s dungeon type when the ability names one (the initiative's
// Undercity).
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/VentureEffect.java's
// resolve, getDungeonCard, chooseNextRoom and ventureIntoDungeon.
type ventureEffect struct{}

func (ventureEffect) Resolve(g *Game, a *Ability, c PlayerController) error {
	if err := rejectParams(a, "Venture", "ConditionDefined"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Venture: %w", err)
	}
	dungeonType, _ := a.Params.Param("Dungeon")
	for _, pid := range players {
		if g.Player(pid).Lost {
			continue
		}
		if err := g.ventureIntoDungeon(c, a.Source, pid, dungeonType); err != nil {
			return fmt.Errorf("engine: Venture: %w", err)
		}
	}
	return nil
}

// ventureIntoDungeon is VentureEffect.ventureIntoDungeon: unless a
// CantVenture static stops p, the venture marker moves -- onto the
// entrance of a dungeon p enters, or onto a room the current one leads to,
// p choosing between two -- the RoomEntered triggers run, and p's ventures
// this turn count up.
func (g *Game) ventureIntoDungeon(c PlayerController, source CardID, p PlayerID, dungeonType string) error {
	can, err := noPlayerStatic(g, p, "CantVenture")
	if err != nil || !can {
		return err
	}
	dungeon, err := g.dungeonToVenture(c, source, p, dungeonType)
	if err != nil {
		return err
	}
	d := g.Card(dungeon)
	var next string
	if d.CurrentRoom == "" {
		rooms := dungeonRooms(d)
		if len(rooms) == 0 {
			return fmt.Errorf("dungeon %q has no rooms", d.Def.Name)
		}
		next = rooms[0].name
	} else if next, err = chooseNextRoom(g, c, p, d); err != nil {
		return err
	}
	d.CurrentRoom = next
	g.checkRoomEnteredTriggers(c, p, dungeon, next)
	g.Player(p).VenturedThisTurn++
	return nil
}

// dungeonToVenture is VentureEffect.getDungeonCard: p's dungeon in the
// Command zone, unless its marker is on its last room -- then that dungeon
// is completed first -- and otherwise a new one p chooses, entering p's
// Command zone. The choice is among the database's dungeon token scripts
// of dungeonType, or, with none named, every dungeon p may enter; offered
// sorted by name, as Java's TreeMap of card faces is.
//
// CardRules.isEnterableDungeon reads the oracle text for "You can't enter
// this dungeon unless"; compile.Face carries no oracle text, and the one
// script with that sentence (Undercity) also carries it as its first K:
// line, which this reads instead.
func (g *Game) dungeonToVenture(c PlayerController, source CardID, p PlayerID, dungeonType string) (CardID, error) {
	for _, id := range g.Zone(Command, p).Cards() {
		d := g.Card(id)
		if !isDungeon(d) {
			continue
		}
		if !isInLastRoom(d) {
			return id, nil
		}
		g.completeDungeon(c, p, id)
		break
	}

	type candidate struct {
		name string
		def  *compile.Card
	}
	var options []candidate
	for _, script := range g.db.TokenScripts() {
		def, _ := g.db.Token(script)
		face := &def.Faces[0]
		if !face.Type.Has(cardtype.Dungeon) {
			continue
		}
		if dungeonType != "" {
			if !face.Type.HasSubtype(dungeonType) {
				continue
			}
		} else if !enterableDungeon(face) {
			continue
		}
		options = append(options, candidate{def.Name, def})
	}
	if len(options) == 0 {
		return NoCard, fmt.Errorf("no dungeon %q in the token database to venture into", dungeonType)
	}
	sort.SliceStable(options, func(i, j int) bool { return options[i].name < options[j].name })
	names := make([]string, len(options))
	for i, o := range options {
		names[i] = o.name
	}
	pick := c.ChooseOption(g, p, source, names)
	if pick < 0 || pick >= len(options) {
		return NoCard, fmt.Errorf("dungeon choice %d out of range [0,%d)", pick, len(options))
	}
	return g.NewCard(options[pick].def, p, Command), nil
}

// enterableDungeon is CardRules.isEnterableDungeon without the dungeon
// type check its caller already made (dungeonToVenture has the reading).
func enterableDungeon(face *compile.Face) bool {
	for _, kw := range face.Keywords {
		if strings.HasPrefix(kw, "You can't enter this dungeon unless") {
			return false
		}
	}
	return true
}

// isDungeon reports whether c is a dungeon card (CardType.isDungeon).
func isDungeon(c *Card) bool {
	return c.Def != nil && c.Def.Faces[0].Type.Has(cardtype.Dungeon)
}

// dungeonRoom is one room of a dungeon: its RoomEntered trigger's room
// ability, as compile.dungeonRooms built it from K:Dungeon.
type dungeonRoom struct {
	name string
	// next is NextRoomName$: the rooms this one leads to, by name.
	next    []string
	hasNext bool
}

// dungeonRooms is d's rooms in keyword order -- Card.getTriggers' room
// triggers, whose overriding abilities carry RoomName$/NextRoom$.
func dungeonRooms(d *Card) []dungeonRoom {
	var out []dungeonRoom
	// The dungeon's printed room table, not a trigger scan: a grant adds no
	// room, so this reads Def directly rather than triggerFaces (ADR-0023
	// decision 3 is about the scans).
	for _, t := range d.Def.Faces[0].Triggers {
		if !strings.EqualFold(t.Name, "RoomEntered") {
			continue
		}
		for _, sub := range t.Subs {
			if !strings.EqualFold(sub.Key, "Execute") {
				continue
			}
			name, _ := sub.Ability.Param("RoomName")
			r := dungeonRoom{name: name}
			if _, ok := sub.Ability.Param("NextRoom"); ok {
				r.hasNext = true
				v, _ := sub.Ability.Param("NextRoomName")
				r.next = strings.Split(v, ",")
			}
			out = append(out, r)
		}
	}
	return out
}

// isInLastRoom is Card.isInLastRoom: the marker is on a room that leads
// nowhere (no NextRoom$).
func isInLastRoom(d *Card) bool {
	for _, r := range dungeonRooms(d) {
		if r.name == d.CurrentRoom && !r.hasNext {
			return true
		}
	}
	return false
}

// chooseNextRoom is VentureEffect.chooseNextRoom: the one room the current
// room leads to, or the controller's pick of several (Java's
// chooseSingleSpellForEffect over the rooms' abilities, offered here by
// room name).
func chooseNextRoom(g *Game, c PlayerController, p PlayerID, d *Card) (string, error) {
	var next []string
	for _, r := range dungeonRooms(d) {
		if r.name == d.CurrentRoom {
			next = r.next
			break
		}
	}
	switch len(next) {
	case 0:
		return "", fmt.Errorf("dungeon %q: room %q leads nowhere", d.Def.Name, d.CurrentRoom)
	case 1:
		return next[0], nil
	}
	picked := c.ChooseAbilitiesForEffect(g, p, d.ID, next, 1)
	if len(picked) != 1 || picked[0] < 0 || picked[0] >= len(next) {
		return "", fmt.Errorf("dungeon %q: room choice %v, want one index into %v", d.Def.Name, picked, next)
	}
	return next[picked[0]], nil
}

// checkRoomEnteredTriggers runs Mode$ RoomEntered (TriggerEnteredRoom) for
// dungeon entering room: ValidCard$ against the dungeon, ValidRoom$ (a
// comma list, matched exactly as matchesValid matches a String) against
// the room.
func (g *Game) checkRoomEnteredTriggers(c PlayerController, p PlayerID, dungeon CardID, room string) {
	g.pushTriggeredAbilities(c, g.playerActionTriggerMatches(p, func(h *Card, t *compile.Ability) bool {
		if spec, ok := t.Param("ValidCard"); ok && !Matches(g, g.Card(dungeon), valid.Parse(spec), h.Controller(), h.ID) {
			return false
		}
		if spec, ok := t.Param("ValidRoom"); ok {
			for _, v := range strings.Split(spec, ",") {
				if v == room {
					return true
				}
			}
			return false
		}
		return true
	}, "RoomEntered"))
}

// completeFinishedDungeons is GameAction.stateBasedAction_Dungeon, the SBA
// half of CR 309.7: a dungeon whose marker is on its last room, with no
// ability from it left on the stack, is completed.
func (g *Game) completeFinishedDungeons(c PlayerController) {
	for _, pid := range g.Players() {
		for _, id := range append([]CardID(nil), g.Zone(Command, pid).Cards()...) {
			d := g.Card(id)
			if !isDungeon(d) || !isInLastRoom(d) || g.hasSourceOnStack(id) {
				continue
			}
			g.completeDungeon(c, d.Controller(), id)
		}
	}
}

// hasSourceOnStack is MagicStack.hasSourceOnStack with no predicate: some
// stack item comes from source.
func (g *Game) hasSourceOnStack(source CardID) bool {
	for i := range g.stack {
		if g.stack[i].Source == source {
			return true
		}
	}
	return false
}

// completeDungeon is GameAction.completeDungeon: p completes the dungeon,
// which ceases to exist (parked in None, as exileEffect parks an effect
// card), and Mode$ DungeonCompleted triggers run.
func (g *Game) completeDungeon(c PlayerController, p PlayerID, dungeon CardID) {
	pl := g.Player(p)
	pl.completedDungeons = append(pl.completedDungeons, dungeon)
	g.exileEffect(dungeon)
	g.pushPlayerTriggers(c, p, g.playerActionTriggerMatches(p, nil, "DungeonCompleted"))
}

// CompletedDungeons is every dungeon p has completed, in order.
func (g *Game) CompletedDungeons(p PlayerID) []CardID {
	return g.Player(p).completedDungeons
}
