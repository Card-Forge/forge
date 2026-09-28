package engine

//enginelint:allow ability card castspell condition control defined earthbendeffect effecthelpers event game id parts player trigger zone zonemove room

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// discoverEffect is DiscoverEffect.java (CR 701.57): each target or Defined$
// player (default You) exiles cards from the top of their library, one at a
// time, until a nonland card with mana value Num$ (default 1) or less. They
// cast it without paying its mana cost (ConfirmEffect true, Java's "Cast"
// option) or put it into their hand; the rest go to the bottom in a random
// order (CardLists.shuffle on the game's stream). Then their Mode$ Discover
// triggers run. RememberDiscovered$ remembers the found card on the host.
//
// Casting reaches only what CastSpell casts: a non-Aura permanent spell,
// cast from exile with no mana paid (castWithoutPaying). The library is
// peeked and the player asked before anything moves, so choosing to cast an
// instant, sorcery or Aura fails the line closed before acting (PORT-8,
// GO-7) -- Java's confirm comes after the exile, a choice that sees the same
// cards either way.
type discoverEffect struct{}

func (discoverEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "Discover", "Condition", "ConditionDefined"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	players, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Discover: %w", err)
	}
	num, err := optionalAmount(g, a, "Discover", "Num", 1)
	if err != nil {
		return err
	}
	for _, p := range players {
		if g.Player(p).Lost {
			return nil
		}
		var exiled []CardID
		found := NoCard
		for _, id := range g.Zone(Library, p).Cards() {
			exiled = append(exiled, id)
			c := g.Card(id)
			if !c.Type().Has(cardtype.Land) && c.CMC() <= num {
				found = id
				break
			}
		}
		cast := false
		if found != NoCard {
			cast = controller.ConfirmEffect(g, p, a.Source)
			// A Room offers a choice of halves to cast (CR 709.3) this
			// effect has no decision for.
			if cast && (!castableAsPermanent(g.Card(found)) || isRoomDef(g.Card(found).Def)) {
				return fmt.Errorf("engine: Discover: casting %q not resolvable yet", g.Card(found).Def.Name)
			}
			if hasParam(a, "RememberDiscovered") {
				source.Memory.Remember(CardEntity(found))
			}
		}

		for _, id := range exiled {
			g.moveByEffect(controller, id, Exile, 0, NoPlayer, false)
			g.checkChangesZoneAllTriggers(controller, []CardID{id}, Library, Exile)
		}
		if found != NoCard {
			if cast {
				g.castWithoutPaying(controller, p, found)
			} else {
				g.moveByEffect(controller, found, Hand, 0, NoPlayer, false)
				g.checkChangesZoneAllTriggers(controller, []CardID{found}, Exile, Hand)
			}
		}

		rest := exiled
		if found != NoCard {
			rest = exiled[:len(exiled)-1]
		}
		rest = append([]CardID(nil), rest...)
		g.rand.Shuffle(len(rest), func(x, y int) { rest[x], rest[y] = rest[y], rest[x] })
		for _, id := range rest {
			g.moveByEffect(controller, id, Library, libraryBottom, NoPlayer, false)
		}
		g.checkChangesZoneAllTriggers(controller, rest, Exile, Library)

		g.checkPlayerActionTriggers(controller, p, "Discover")
	}
	return nil
}

// castWithoutPaying is CastSpell's tail for a card cast during a resolving
// effect with no mana paid (Java's copyWithNoManaCost then
// playSaFromPlayEffect): card goes from wherever it is to the stack under
// pid, its permanent spell ability is pushed, SpellCast is emitted, and the
// cast and zone-change triggers run. Timing is not checked -- an effect's
// "cast it" ignores it (CR 608.2g). The caller has checked castableAsPermanent.
func (g *Game) castWithoutPaying(controller PlayerController, pid PlayerID, card CardID) {
	origin := g.Card(card).Zone
	// A non-Aura permanent spell with no mana to pay has nothing left to
	// decline (castSpell's permanent branch), so this always casts.
	g.castSpell(controller, pid, card, castOpts{withoutManaCost: true})
	g.checkChangesZoneAllTriggers(controller, []CardID{card}, origin, Stack)
}
