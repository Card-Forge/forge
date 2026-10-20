package engine

//enginelint:allow game control ability effecthelpers card condition id defined player token parts zone valid amount changecombatantseffect ateot

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// copyPermanentEffect is CopyPermanentEffect.java: for each Controller$
// player (the activator by default), NumCopies$ (default 1) token copies of
// each card to copy are created under that player's control. The cards are
// the Defined$ or targeted ones; or, with Choices$, one the Chooser$ (the
// activator) picks among the matching Defined$ cards or permanents; or the
// DB card named by DefinedName$ (NamedCard: the host's named card). An
// instant or sorcery is never copied (CR 111.5).
//
// A copy takes the original's copiable values (CR 707.2): its card
// definition and any power/toughness its creating effect set, since this
// port has no other copy effects to fold in. SetPower$/SetToughness$ change
// the copy's own base values (CR 707.9b), TokenTapped$ taps it and
// RememberTokens$ remembers it on the host and TokenAttacking$/TokenBlocking$
// put it into combat attacking or blocking (addToCombatAttacking,
// addToCombatBlocking); the other copy exceptions and
// end-of-turn cleanup are not resolved.
type copyPermanentEffect struct{}

func (copyPermanentEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "CopyPermanent", "Condition", "AtEOTTrig", "AddTypes", "PumpKeywords",
		"AddKeywords", "NonLegendary", "Populate", "SetColor", "SetCreatureTypes",
		"PumpDuration", "AddSVars", "RemoveCardTypes", "AttachedTo", "AddTriggers", "ValidSupportedCopy",
		"RemoveSubTypes", "RandomNum", "RandomCopied", "ForEach", "OptionalForEach", "WithDifferentNames",
		"ChangeZoneTable", "AddAbilities", "AddStaticAbilities", "AddColors", "ChosenMapIndex",
		"RemoveCreatureTypes", "SetCardTypes", "GainThisAbility", "NewName", "AddTypesTo",
		"CopyIsColor", "SetManaCost", "SetImageKey"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	if def, _ := a.Params.Param("Defined"); def == "ChosenMap" {
		return fmt.Errorf("engine: CopyPermanent: Defined$ ChosenMap not resolvable yet")
	}
	if hasParam(a, "Optional") && !controller.ConfirmEffect(g, a.Controller, a.Source) {
		return nil
	}
	num, err := optionalAmount(g, a, "CopyPermanent", "NumCopies", 1)
	if err != nil {
		return err
	}
	controllers := []PlayerID{a.Controller}
	if raw, ok := a.Params.Param("Controller"); ok {
		ps, err := definedPlayers(g, a.Controller, a.Source, raw, a.refs())
		if err != nil {
			return fmt.Errorf("engine: CopyPermanent: %w", err)
		}
		if len(ps) > 0 {
			controllers = ps
		}
	}
	setPower, hasSetPower, err := copyBaseValue(g, a, source, "SetPower")
	if err != nil {
		return err
	}
	setTough, hasSetTough, err := copyBaseValue(g, a, source, "SetToughness")
	if err != nil {
		return err
	}
	var created []CardID
	for _, owner := range controllers {
		if g.Player(owner).Lost {
			continue
		}
		originals, err := copyPermanentOriginals(g, a, controller, source)
		if err != nil {
			return err
		}
		for _, orig := range originals {
			if orig.def == nil {
				continue
			}
			t := orig.def.Faces[0].Type
			if t.Has(cardtype.Instant) || t.Has(cardtype.Sorcery) {
				continue
			}
			spec := tokenSpec{Def: orig.def, Owner: owner, Tapped: hasParam(a, "TokenTapped"),
				Power: orig.power, HasPower: orig.hasPower, Toughness: orig.toughness, HasToughness: orig.hasToughness}
			if hasSetPower {
				spec.Power, spec.HasPower = setPower, true
			}
			if hasSetTough {
				spec.Toughness, spec.HasToughness = setTough, true
			}
			for i := 0; i < num; i++ {
				id := g.createToken(controller, spec)
				created = append(created, id)
				// TokenEffectBase.makeTokenTable's addToCombat (TokenEffectBase.java:188),
				// which CopyPermanentEffect inherits.
				if err := g.addToCombatAttacking(controller, a, id, "TokenAttacking"); err != nil {
					return fmt.Errorf("engine: CopyPermanent: %w", err)
				}
				if err := g.addToCombatBlocking(controller, a, id, "TokenBlocking"); err != nil {
					return fmt.Errorf("engine: CopyPermanent: %w", err)
				}
				if hasParam(a, "RememberTokens") {
					source.Memory.Remember(CardEntity(id))
				}
			}
		}
	}
	if len(created) > 0 {
		g.checkCreatedZoneAllTriggers(controller, created)
	}
	return registerAtEOT(g, a, created)
}

// copyOriginal is what a token copy copies: the card definition and the
// base power/toughness its creating effect gave it, if any.
type copyOriginal struct {
	def                    *compile.Card
	power, toughness       int
	hasPower, hasToughness bool
}

func copyPermanentOriginals(g *Game, a *Ability, controller PlayerController, source *Card) ([]copyOriginal, error) {
	fromCard := func(id CardID) copyOriginal { return g.Card(id).copiableValues() }
	if name, ok := a.Params.Param("DefinedName"); ok {
		if name == "NamedCard" {
			named := source.Memory.NamedCards()
			if len(named) == 0 {
				return nil, nil
			}
			name = named[len(named)-1]
		}
		def, ok := g.db.Card(name)
		if !ok {
			return nil, nil
		}
		return []copyOriginal{{def: def}}, nil
	}
	if spec, ok := a.Params.Param("Choices"); ok {
		chooser := a.Controller
		if raw, ok := a.Params.Param("Chooser"); ok {
			ps, err := definedPlayers(g, a.Controller, a.Source, raw, a.refs())
			if err != nil || len(ps) == 0 {
				return nil, fmt.Errorf("engine: CopyPermanent: Chooser$ %q not resolvable", raw)
			}
			chooser = ps[0]
		}
		var pool []CardID
		if hasParam(a, "Defined") || hasParam(a, "ValidTgts") {
			var err error
			if pool, err = targetedOrDefinedCards(source, a.Params, a.refs()); err != nil {
				return nil, fmt.Errorf("engine: CopyPermanent: %w", err)
			}
		} else {
			for _, pid := range g.Players() {
				pool = append(pool, g.Zone(Battlefield, pid).Cards()...)
			}
		}
		parsed := valid.Parse(spec)
		var choices []CardID
		for _, id := range pool {
			if Matches(g, g.Card(id), parsed, a.Controller, a.Source) {
				choices = append(choices, id)
			}
		}
		if len(choices) == 0 {
			return nil, nil
		}
		chosen := controller.ChooseCardsForEffect(g, chooser, a.Source, choices, 1, 1)
		if err := checkChoice(chosen, choices, 1, 1); err != nil {
			return nil, fmt.Errorf("engine: CopyPermanent: %w", err)
		}
		return []copyOriginal{fromCard(chosen[0])}, nil
	}
	cards, err := targetedOrDefinedCards(source, a.Params, a.refs())
	if err != nil {
		return nil, fmt.Errorf("engine: CopyPermanent: %w", err)
	}
	out := make([]copyOriginal, len(cards))
	for i, id := range cards {
		out[i] = fromCard(id)
	}
	return out, nil
}

// copyBaseValue resolves a copy's SetPower$/SetToughness$ override.
func copyBaseValue(g *Game, a *Ability, source *Card, key string) (int, bool, error) {
	raw, ok := a.Params.Param(key)
	if !ok {
		return 0, false, nil
	}
	n, ok := resolveNamedAmount(g, a.Amounts, source, raw)
	if !ok {
		return 0, false, fmt.Errorf("engine: CopyPermanent: %s$ %q not resolvable", key, raw)
	}
	return n, true, nil
}
