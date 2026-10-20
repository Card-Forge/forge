package engine

//enginelint:allow ability additional card condition control defined effecthelpers game id parts player zone

import "fmt"

// clashEffect is ClashEffect.java (CR 701.30): the host's controller clashes
// with Defined$'s first player or, without it, an opponent the activator
// picks. Each reveals the top card of their library; the higher mana value
// wins (no winner on a tie or two empty libraries); then each, clasher
// first, puts their card back on top or on the bottom (willPutCardOnTop).
// WinSubAbility$ resolves if the host's controller won, else
// OtherwiseSubAbility$. RememberClasher$ remembers the opponent. Mode$
// Clashed triggers are not ported. Java moves a card within its own library
// with zone-change events suppressed; this port reorders it in place.
type clashEffect struct{}

func (clashEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "Clash"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	player := source.Controller()
	var opponent PlayerID
	if raw, ok := a.Params.Param("Defined"); ok {
		ps, err := definedPlayers(g, a.Controller, a.Source, raw, a.refs())
		if err != nil {
			return fmt.Errorf("engine: Clash: %w", err)
		}
		if len(ps) == 0 {
			return nil
		}
		opponent = ps[0]
	} else {
		var opps []PlayerID
		for _, p := range g.Players() {
			if p != player && !g.Player(p).Lost {
				opps = append(opps, p)
			}
		}
		switch len(opps) {
		case 0:
			return nil
		case 1:
			opponent = opps[0]
		default:
			opponent = controller.ChoosePlayerForEffect(g, a.Controller, a.Source, opps)
			if err := checkChoice([]PlayerID{opponent}, opps, 1, 1); err != nil {
				return fmt.Errorf("engine: Clash: %w", err)
			}
		}
	}
	if hasParam(a, "RememberClasher") {
		source.Memory.Remember(PlayerEntity(opponent))
	}

	winner := NoPlayer
	pCard, pMV := topOfLibrary(g, player)
	oCard, oMV := topOfLibrary(g, opponent)
	if (pCard != NoCard || oCard != NoCard) && pMV != oMV {
		winner = opponent
		if pMV > oMV {
			winner = player
		}
	}
	for _, pc := range [...]struct {
		p PlayerID
		c CardID
	}{{player, pCard}, {opponent, oCard}} {
		if pc.c != NoCard && !controller.WillPutCardOnTop(g, pc.p, pc.c) {
			lib := g.Zone(Library, pc.p)
			lib.cards.Remove(pc.c)
			lib.cards.Add(pc.c)
		}
	}
	key := "OtherwiseSubAbility"
	if winner == player {
		key = "WinSubAbility"
	}
	if subs := additionalAbilities(a.Params, key); len(subs) > 0 {
		return g.resolveAdditional(a, controller, subs[0])
	}
	return nil
}

// topOfLibrary is p's top library card and its mana value, or NoCard and -1.
func topOfLibrary(g *Game, p PlayerID) (CardID, int) {
	lib := g.Zone(Library, p).Cards()
	if len(lib) == 0 {
		return NoCard, -1
	}
	return lib[0], g.Card(lib[0]).CMC()
}
