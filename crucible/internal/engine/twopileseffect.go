package engine

//enginelint:allow game ability control effecthelpers card condition defined id player zone valid parts additional

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// twoPilesEffect is TwoPilesEffect.java: for each target player, the
// Separator$ (the host's controller by default; the activator picks among
// several) splits a pool -- DefinedCards$, else the player's Zone$ cards,
// filtered by ValidCards$ -- into two piles (the cards they pick, and the
// rest), or DefinedPiles$ names both. The Chooser$ (the first target
// player by default) picks one; with LeftRightPile$ the first pile is
// always "chosen". The chosen pile is remembered with RememberChosen$, and
// ChosenPile$/UnchosenPile$ resolve with only that pile remembered. The
// host forgets everything afterwards unless KeepRemembered$ or
// RememberChosen$.
type twoPilesEffect struct{}

func (twoPilesEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "TwoPiles"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: TwoPiles: %w", err)
	}
	if len(players) == 0 {
		return nil
	}
	pickPlayer := func(key string, def PlayerID) (PlayerID, error) {
		raw, ok := a.Params.Param(key)
		if !ok {
			return def, nil
		}
		options, err := definedPlayers(g, a.Controller, a.Source, raw, a.refs())
		if err != nil {
			return NoPlayer, fmt.Errorf("engine: TwoPiles: %w", err)
		}
		switch len(options) {
		case 0:
			return def, nil
		case 1:
			return options[0], nil
		}
		p := controller.ChoosePlayerForEffect(g, a.Controller, a.Source, options)
		if err := checkChoice([]PlayerID{p}, options, 1, 1); err != nil {
			return NoPlayer, fmt.Errorf("engine: TwoPiles: %w", err)
		}
		return p, nil
	}
	separator, err := pickPlayer("Separator", source.Controller())
	if err != nil {
		return err
	}
	chooser, err := pickPlayer("Chooser", players[0])
	if err != nil {
		return err
	}
	spec, ok := a.Params.Param("ValidCards")
	if !ok {
		spec = "Card"
	}
	parsed := valid.Parse(spec)
	_, leftRight := a.Params.Param("LeftRightPile")
	_, rememberChosen := a.Params.Param("RememberChosen")
	for _, p := range players {
		if g.Player(p).Lost {
			continue
		}
		var pile1, pile2 []CardID
		if raw, ok := a.Params.Param("DefinedPiles"); ok {
			defs := strings.SplitN(raw, ",", 2)
			if len(defs) != 2 {
				return fmt.Errorf("engine: TwoPiles: DefinedPiles$ %q needs two", raw)
			}
			if pile1, err = definedCards(source, defs[0], a.refs()); err != nil {
				return fmt.Errorf("engine: TwoPiles: %w", err)
			}
			if pile2, err = definedCards(source, defs[1], a.refs()); err != nil {
				return fmt.Errorf("engine: TwoPiles: %w", err)
			}
		} else {
			var pool0 []CardID
			if raw, ok := a.Params.Param("DefinedCards"); ok {
				if pool0, err = definedCards(source, raw, a.refs()); err != nil {
					return fmt.Errorf("engine: TwoPiles: %w", err)
				}
			} else {
				raw, _ := a.Params.Param("Zone")
				zone, ok := ZoneByName(raw)
				if !ok {
					return fmt.Errorf("engine: TwoPiles: Zone$ %q not resolvable", raw)
				}
				pool0 = g.Zone(zone, p).Cards()
			}
			var pool []CardID
			for _, id := range pool0 {
				if Matches(g, g.Card(id), parsed, source.Controller(), a.Source) {
					pool = append(pool, id)
				}
			}
			if len(pool) == 0 {
				return nil
			}
			pile1 = controller.ChooseCardsForEffect(g, separator, a.Source, pool, 0, len(pool))
			if err := checkChoice(pile1, pool, 0, len(pool)); err != nil {
				return fmt.Errorf("engine: TwoPiles: %w", err)
			}
			pile2 = withoutCards(pool, pile1)
		}
		firstChosen := true
		if !leftRight {
			firstChosen = controller.ChooseBinary(g, chooser, a.Source, Pile1OrPile2)
		}
		chosen, unchosen := pile1, pile2
		if !firstChosen {
			chosen, unchosen = pile2, pile1
		}
		if rememberChosen {
			for _, id := range chosen {
				source.Memory.Remember(CardEntity(id))
			}
		}
		for _, run := range []struct {
			key  string
			pile []CardID
		}{{"ChosenPile", chosen}, {"UnchosenPile", unchosen}} {
			subs := additionalAbilities(a.Params, run.key)
			if len(subs) == 0 {
				continue
			}
			saved := source.Memory.Remembered()
			saved = append([]EntityID(nil), saved...)
			source.Memory.ClearRemembered()
			for _, id := range run.pile {
				source.Memory.Remember(CardEntity(id))
			}
			if err := g.resolveAdditional(a, controller, subs[0]); err != nil {
				return err
			}
			for _, id := range run.pile {
				source.Memory.Forget(CardEntity(id))
			}
			for _, e := range saved {
				source.Memory.Remember(e)
			}
		}
	}
	if !hasParam(a, "KeepRemembered") && !rememberChosen {
		source.Memory.ClearRemembered()
	}
	return nil
}
