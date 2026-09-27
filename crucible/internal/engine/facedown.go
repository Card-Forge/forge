// Face-down permanents (CR 708): manifested and cloaked cards.
//
// Ported from forge-game/src/main/java/forge/game/card/Card.java's
// manifest, cloak and turnFaceDown, and CardFactoryUtil's face-down state.

package engine

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
)

// faceDownDef is a face-down permanent's characteristics (CR 708.2a): a
// 2/2 creature with no name, types, color, mana cost or abilities -- plus
// ward 2 for a cloaked one (CR 701.58a).
func faceDownDef(cloaked bool) *compile.Card {
	def := &compile.Card{}
	def.Faces[0].Type = cardtype.ParseToken("Creature")
	def.Faces[0].Power, def.Faces[0].Toughness = "2", "2"
	if cloaked {
		def.Faces[0].Keywords = []string{"Ward:2"}
	}
	return def
}

// IsFaceDown reports whether c is a face-down permanent.
func (c *Card) IsFaceDown() bool { return c.faceUpDef != nil }

// manifest is Card.manifest/cloak: id turns face down, comes under p's
// control and enters p's battlefield face down, answering whether it did.
func (g *Game) manifest(controller PlayerController, id CardID, p PlayerID, cloaked bool) {
	c := g.Card(id)
	c.clearTextChange()
	if c.faceUpDef == nil {
		c.faceUpDef = c.Def
	}
	c.Def = faceDownDef(cloaked)
	c.Manifested, c.Cloaked = !cloaked, cloaked
	g.moveByEffect(controller, id, Battlefield, 0, p, c.Tapped)
}

// turnFaceUp restores id's own characteristics as it leaves the
// battlefield or turns face up (CR 708.9).
func (c *Card) turnFaceUp() {
	if c.faceUpDef == nil {
		return
	}
	c.Def = c.faceUpDef
	c.faceUpDef = nil
	c.Manifested, c.Cloaked = false, false
}

// turnFrontFaceUp returns a transformed card to its front face as it leaves
// the battlefield (CR 711.8).
func (c *Card) turnFrontFaceUp() {
	if c.frontDef == nil {
		return
	}
	c.Def = c.frontDef
	c.frontDef = nil
}

// manifestEffect is ManifestEffect.java (and, with cloak, CloakEffect.java)
// through ManifestBaseEffect: for each DefinedPlayer$ (the activator by
// default), Amount$ (default 1) cards are manifested -- the top of their
// library (Defined$ TopOfLibrary, the default), the Defined$/targeted
// cards, or cards they pick from ChoiceZone$ (default Hand) matching
// Choices$. Library cards go one at a time; others together. Shuffle$
// randomizes the order; Tapped$ (cloak) taps them first. The Remember$
// param remembers each one on the host.
//
//crucible:register Manifest manifestEffect{api: "Manifest", remember: "RememberManifested"}
//crucible:register Cloak manifestEffect{api: "Cloak", cloak: true, remember: "RememberCloaked"}
type manifestEffect struct {
	api      string
	cloak    bool
	remember string
}

func (e manifestEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, e.api, "Condition"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	amount, err := optionalAmount(g, a, e.api, "Amount", 1)
	if err != nil {
		return err
	}
	players, err := manifestPlayers(g, a)
	if err != nil {
		return fmt.Errorf("engine: %s: %w", e.api, err)
	}
	for _, p := range players {
		cards, err := e.cards(g, a, controller, source, p, amount)
		if err != nil {
			return err
		}
		if hasParam(a, "Shuffle") {
			g.rand.Shuffle(len(cards), func(i, j int) { cards[i], cards[j] = cards[j], cards[i] })
		}
		var moved []CardID
		for _, id := range cards {
			c := g.Card(id)
			origin := c.Zone
			if e.cloak && hasParam(a, "Tapped") {
				c.Tapped = true
			}
			g.manifest(controller, id, p, e.cloak)
			if c.Zone != Battlefield {
				continue
			}
			if hasParam(a, e.remember) {
				source.Memory.Remember(CardEntity(id))
			}
			if origin == Library {
				g.checkChangesZoneAllTriggers(controller, []CardID{id}, Library, Battlefield)
			} else {
				moved = append(moved, id)
			}
		}
		if len(moved) > 0 {
			g.checkChangesZoneAllTriggers(controller, moved, Hand, Battlefield)
		}
	}
	return nil
}

func (e manifestEffect) cards(g *Game, a *Ability, controller PlayerController, source *Card, p PlayerID, amount int) ([]CardID, error) {
	if hasParam(a, "Choices") || hasParam(a, "ChoiceZone") {
		zone := Hand
		if raw, ok := a.Params.Param("ChoiceZone"); ok {
			z, ok := ZoneByName(raw)
			if !ok {
				return nil, fmt.Errorf("engine: %s: ChoiceZone$ %q not resolvable", e.api, raw)
			}
			zone = z
		}
		choices := g.Zone(zone, p).Cards()
		if spec, ok := a.Params.Param("Choices"); ok {
			choices = filterValid(g, choices, spec, a.Controller, a.Source)
		}
		if len(choices) == 0 {
			return nil, nil
		}
		n := amount
		if n > len(choices) {
			n = len(choices)
		}
		chosen := controller.ChooseCardsForEffect(g, p, a.Source, choices, n, n)
		if err := checkChoice(chosen, choices, n, n); err != nil {
			return nil, fmt.Errorf("engine: %s: %w", e.api, err)
		}
		return chosen, nil
	}
	if def, _ := a.Params.Param("Defined"); def == "" || def == "TopOfLibrary" {
		lib := g.Zone(Library, p).Cards()
		if amount > len(lib) {
			amount = len(lib)
		}
		return append([]CardID(nil), lib[:amount]...), nil
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return nil, fmt.Errorf("engine: %s: %w", e.api, err)
	}
	return cards, nil
}

// manifestPlayers is getTargetPlayers(sa, "DefinedPlayer"): the targeted
// players, else DefinedPlayer$, else the activator.
func manifestPlayers(g *Game, a *Ability) ([]PlayerID, error) {
	if hasParam(a, "ValidTgts") {
		var out []PlayerID
		for _, t := range a.Targets {
			if p, ok := t.AsPlayer(); ok {
				out = append(out, p)
			}
		}
		return out, nil
	}
	if def, ok := a.Params.Param("DefinedPlayer"); ok {
		return definedPlayers(g, a.Controller, a.Source, def, a.refs())
	}
	return []PlayerID{a.Controller}, nil
}

// manifestDreadEffect is ManifestDreadEffect.java (CR 701.60): Amount$
// times, each DefinedPlayer$ looks at their top two cards, manifests one of
// them and puts the rest into their graveyard.
type manifestDreadEffect struct{}

func (manifestDreadEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "ManifestDread", "Condition"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	amount, err := optionalAmount(g, a, "ManifestDread", "Amount", 1)
	if err != nil {
		return err
	}
	players, err := manifestPlayers(g, a)
	if err != nil {
		return fmt.Errorf("engine: ManifestDread: %w", err)
	}
	for _, p := range players {
		for i := 0; i < amount; i++ {
			lib := g.Zone(Library, p).Cards()
			n := 2
			if n > len(lib) {
				n = len(lib)
			}
			top := append([]CardID(nil), lib[:n]...)
			if len(top) == 0 {
				continue
			}
			chosen := controller.ChooseCardsForEffect(g, p, a.Source, top, 1, 1)
			if err := checkChoice(chosen, top, 1, 1); err != nil {
				return fmt.Errorf("engine: ManifestDread: %w", err)
			}
			g.manifest(controller, chosen[0], p, false)
			rest := withoutCards(top, chosen)
			if g.Card(chosen[0]).Zone != Battlefield {
				rest = append(rest, chosen[0])
			} else if hasParam(a, "RememberManifested") {
				source.Memory.Remember(CardEntity(chosen[0]))
			}
			for _, id := range rest {
				g.moveByEffect(controller, id, Graveyard, 0, NoPlayer, false)
			}
			g.checkChangesZoneAllTriggers(controller, []CardID{chosen[0]}, Library, Battlefield)
			if len(rest) > 0 {
				g.checkChangesZoneAllTriggers(controller, rest, Library, Graveyard)
			}
		}
	}
	return nil
}
