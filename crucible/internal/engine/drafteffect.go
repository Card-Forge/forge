package engine

//enginelint:allow ability card condition control defined effecthelpers game id parts zone zonemove

import (
	"fmt"
	"strings"
)

// draftEffect is DraftEffect.java (Alchemy's "draft a card from CARDNAME's
// spellbook"): DraftNum$ times (default 1), the Spellbook$ name list is
// shuffled on the game's stream (Collections.shuffle, which Java applies to
// the same list each round), its first three names are made as cards owned
// by the first Defined$ player (default You), and that player picks one.
// The picks go to their hand together, ChangesZoneAll firing once;
// RememberDrafted$ records them on the host.
//
// A name whose "A-" rebalanced version exists is made as that version
// (PaperCard.isUnRebalanced, DraftEffect.java:52-55). Java asks the
// editions' [rebalanced] sections; this port has no edition data and asks
// the DB for the "A-" card. The nine spellbook names with an "A-" card in the
// corpus are each listed in an edition's [rebalanced] section, so the two
// agree on every real line. The two cards not picked are made and left
// outside the game, as Card.fromPaperCard's are.
type draftEffect struct{}

func (draftEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	if err := rejectParams(a, "Draft"); err != nil {
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
		return fmt.Errorf("engine: Draft: %w", err)
	}
	if len(players) == 0 {
		return nil
	}
	p := players[0]
	raw, ok := a.Params.Param("Spellbook")
	if !ok {
		return fmt.Errorf("engine: Draft: no Spellbook$")
	}
	spellbook := strings.Split(raw, ",")
	if len(spellbook) < 3 {
		return fmt.Errorf("engine: Draft: Spellbook$ has %d names, need 3", len(spellbook))
	}
	num, err := optionalAmount(g, a, "Draft", "DraftNum", 1)
	if err != nil {
		return err
	}

	var drafted []CardID
	for i := 0; i < num; i++ {
		g.rand.Shuffle(len(spellbook), func(x, y int) { spellbook[x], spellbook[y] = spellbook[y], spellbook[x] })
		options := make([]CardID, 0, 3)
		for _, name := range spellbook[:3] {
			// Card names with a comma are written with ";" in Spellbook$.
			name = strings.ReplaceAll(name, ";", ",")
			cardDef, ok := g.db.Card("A-" + name)
			if !ok {
				if cardDef, ok = g.db.Card(name); !ok {
					return fmt.Errorf("engine: Draft: no card named %q", name)
				}
			}
			options = append(options, g.NewCard(cardDef, p, None))
		}
		picked := controller.ChooseCardsForEffect(g, p, a.Source, options, 1, 1)
		if err := checkChoice(picked, options, 1, 1); err != nil {
			return fmt.Errorf("engine: Draft: %w", err)
		}
		drafted = append(drafted, picked[0])
	}

	for _, id := range drafted {
		g.moveByEffect(controller, id, Hand, 0, NoPlayer, false)
		if hasParam(a, "RememberDrafted") {
			source.Memory.Remember(CardEntity(id))
		}
	}
	g.checkChangesZoneAllTriggers(controller, drafted, None, Hand)
	return nil
}
