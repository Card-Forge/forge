package engine

//enginelint:allow game control ability effecthelpers card condition defined zone id parts zonemove event putcountereffect

import (
	"fmt"
	"strconv"
	"strings"
)

// makeCardEffect is MakeCardEffect.java: for each Defined$ player (the
// activator by default), cards are made from outside the game -- by Name$
// (ChosenName: the host's named cards), Names$, the printed names of the
// DefinedName$ cards, or one picked from a Spellbook$/Choices$ name list
// (SpellbookAmount$ times; AtRandom$ picks with Aggregates.random). Each
// name is made Amount$ times (default 1), owned by that player, and put
// into Zone$ (default Library; at LibraryPosition$, else the top and then
// the library is shuffled), tapped with Tapped$, entering with WithCounter$
// counters. RememberMade$/ImprintMade$ record them on the host.
type makeCardEffect struct{}

func (makeCardEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "MakeCard", "Booster", "Filter", "AttachedTo", "TokenCard", "FaceDown"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	def, ok := a.Params.Param("Defined")
	if !ok {
		def = "You"
	}
	players, err := definedPlayers(g, a.Controller, a.Source, def, a.refs())
	if err != nil {
		return fmt.Errorf("engine: MakeCard: %w", err)
	}
	amount, err := optionalAmount(g, a, "MakeCard", "Amount", 1)
	if err != nil {
		return err
	}
	zone := Library
	if raw, ok := a.Params.Param("Zone"); ok {
		z, ok := ZoneByName(raw)
		if !ok {
			return fmt.Errorf("engine: MakeCard: Zone$ %q not resolvable", raw)
		}
		zone = z
	}
	libPos := 0
	if raw, ok := a.Params.Param("LibraryPosition"); ok {
		n, err := strconv.Atoi(raw)
		if err != nil || (n != 0 && n != -1) {
			return fmt.Errorf("engine: MakeCard: LibraryPosition$ %q not resolvable", raw)
		}
		libPos = n
	}
	for _, p := range players {
		if hasParam(a, "Optional") && hasParam(a, "OptionPrompt") && !controller.ConfirmEffect(g, p, a.Source) {
			return nil
		}
		names, err := makeCardNames(g, a, controller, source, p)
		if err != nil {
			return err
		}
		var made []CardID
		for _, name := range names {
			if name == "" {
				continue
			}
			cardDef, ok := g.db.Card(name)
			if !ok {
				return fmt.Errorf("engine: MakeCard: no card named %q", name)
			}
			for i := 0; i < amount; i++ {
				id := g.NewCard(cardDef, p, None)
				if hasParam(a, "Tapped") {
					g.Card(id).Tapped = true
				}
				made = append(made, id)
			}
		}
		counterKind, withCounter := a.Params.Param("WithCounter")
		counterNum := 0
		if withCounter {
			if counterNum, err = optionalAmount(g, a, "MakeCard", "WithCounterNum", 1); err != nil {
				return err
			}
		}
		counterType := CounterType(strings.ToUpper(counterKind))
		for _, id := range made {
			// The counters go through AddCounter replacements and a Mode$
			// CantPutCounter static either way (GameEntityCounterTable
			// .replaceCounterEffect): entering with them on the battlefield, put
			// on the card where it landed anywhere else.
			var enter []enterCounters
			if withCounter && zone == Battlefield {
				enter = append(enter, enterCounters{kind: counterType, n: counterNum, placer: p})
			}
			g.moveByEffect(controller, id, zone, libPos, NoPlayer, g.Card(id).Tapped, enter...)
			if withCounter && zone != Battlefield {
				// made.addCounter(..., player, counterTable), placed by
				// replaceCounterEffect (MakeCardEffect.java:203-206, :211):
				// CounterAdded fires for them too.
				if n := g.countersReplaced(controller, p, CardEntity(id), counterType, counterNum); n > 0 {
					g.addCardCounters(controller, p, id, id, counterType, n)
				}
			}
			if hasParam(a, "RememberMade") {
				source.Memory.Remember(CardEntity(id))
			}
			if hasParam(a, "ImprintMade") {
				source.Memory.Imprint(id)
			}
		}
		g.checkChangesZoneAllTriggers(controller, made, None, zone)
		if zone == Library && !hasParam(a, "LibraryPosition") {
			g.Shuffle(Library, p)
		}
	}
	return nil
}

// makeCardNames is MakeCardEffect's name-gathering half for player p.
func makeCardNames(g *Game, a *Ability, controller PlayerController, source *Card, p PlayerID) ([]string, error) {
	if n, ok := a.Params.Param("Name"); ok {
		if n == "ChosenName" {
			return append([]string(nil), source.Memory.NamedCards()...), nil
		}
		return []string{n}, nil
	}
	if raw, ok := a.Params.Param("Names"); ok {
		var out []string
		for _, s := range strings.Split(raw, ",") {
			out = append(out, strings.ReplaceAll(s, ";", ","))
		}
		return out, nil
	}
	if def, ok := a.Params.Param("DefinedName"); ok {
		cards, err := definedCards(source, def, a.refs())
		if err != nil {
			return nil, fmt.Errorf("engine: MakeCard: %w", err)
		}
		var out []string
		for _, id := range cards {
			if d := g.Card(id).Def; d != nil {
				out = append(out, d.Name)
			}
		}
		return out, nil
	}
	key := "Spellbook"
	raw, ok := a.Params.Param(key)
	if !ok {
		key = "Choices"
		if raw, ok = a.Params.Param(key); !ok {
			return nil, nil
		}
	}
	var faces []string
	for _, s := range strings.Split(raw, ",") {
		faces = append(faces, strings.ReplaceAll(strings.TrimSpace(s), ";", ","))
	}
	times, err := optionalAmount(g, a, "MakeCard", "SpellbookAmount", 1)
	if err != nil {
		return nil, err
	}
	var out []string
	for ; times > 0 && len(faces) > 0; times-- {
		var i int
		if hasParam(a, "AtRandom") {
			i = g.randomIndex(len(faces))
		} else {
			i = controller.ChooseOption(g, p, a.Source, faces)
			if i < 0 || i >= len(faces) {
				return nil, fmt.Errorf("engine: MakeCard: %s$ choice %d out of range", key, i)
			}
		}
		out = append(out, faces[i])
		faces = append(faces[:i:i], faces[i+1:]...)
	}
	return out, nil
}
