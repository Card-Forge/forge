package engine

//enginelint:allow id card game ability defined condition control amount zone effecthelpers trigger valid

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// eachDamageUnresolvedParams: EachDamage's own batched DamageMap$ context
// (a parent RepeatEach/GenericChoice collecting damage) and the planeswalker
// ultimate gate.
var eachDamageUnresolvedParams = [...]string{
	"Ultimate",
}

// eachDamageEffect is DamageEachEffect.java: every damage source --
// DefinedDamagers$, or else the battlefield permanents matching ValidCards$
// (all of them without it) -- deals NumDmg$ (default X, evaluated with that
// source as the card, so Count$CardPower reads each source's own power)
// to each target: the ability's targets or Defined$ entities, or with
// EachToItself$ to itself, or with ToEachOther$ to every other card that
// Defined$-style list names. Damage goes through the same
// dealPermanentDamage/dealPlayerDamage pipeline as DealDamage, one shared
// damageTable, with each source's own deathtouch. 27 real lines.
type eachDamageEffect struct{}

func (eachDamageEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectUnreadCondition(a, a.Params.Name); err != nil {
		return err
	}
	for _, key := range eachDamageUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: EachDamage: %s$ not resolvable yet", key)
		}
	}
	host := g.Card(a.Source)
	if !subAbilityConditionMet(g, host, a.Amounts, a.Params) {
		return nil
	}
	num, ok := a.Params.Param("NumDmg")
	if !ok {
		num = "X"
	}
	var sources []CardID
	if spec, ok := a.Params.Param("DefinedDamagers"); ok {
		var err error
		sources, err = definedCards(host, spec, a.refs())
		if err != nil {
			return fmt.Errorf("engine: EachDamage: DefinedDamagers$: %w", err)
		}
	} else {
		spec, hasValid := a.Params.Param("ValidCards")
		parsed := valid.Parse(spec)
		for _, pid := range g.Players() {
			for _, id := range g.Zone(Battlefield, pid).Cards() {
				if !hasValid || Matches(g, g.Card(id), parsed, a.Controller, a.Source) {
					sources = append(sources, id)
				}
			}
		}
	}
	amount := func(src CardID) (int, error) {
		n, ok := resolveNamedAmount(g, a.Amounts, g.Card(src), num)
		if !ok {
			return 0, fmt.Errorf("engine: EachDamage: NumDmg$ %q not resolvable yet", num)
		}
		return n, nil
	}
	var table damageTable
	hit := func(src CardID, target EntityID) error {
		dmg, err := amount(src)
		if err != nil {
			return err
		}
		deathtouch := g.Card(src).HasKeyword("Deathtouch")
		if pid, ok := target.AsPlayer(); ok {
			g.dealPlayerDamage(controller, src, pid, dmg, false, &table)
			return nil
		}
		id, _ := target.AsCard()
		if g.Card(id).Zone == Battlefield {
			g.dealPermanentDamage(controller, src, id, dmg, deathtouch, false, &table)
		}
		return nil
	}

	switch {
	case hasParam(a, "EachToItself"):
		for _, src := range sources {
			if err := hit(src, CardEntity(src)); err != nil {
				return err
			}
		}
	case hasParam(a, "ToEachOther"):
		spec, _ := a.Params.Param("ToEachOther")
		cards, err := definedCards(host, spec, a.refs())
		if err != nil {
			return fmt.Errorf("engine: EachDamage: ToEachOther$: %w", err)
		}
		for _, damager := range cards {
			for _, c := range cards {
				if c != damager {
					if err := hit(damager, CardEntity(c)); err != nil {
						return err
					}
				}
			}
		}
	default:
		targets, err := eachDamageTargets(g, a, host)
		if err != nil {
			return err
		}
		for _, t := range targets {
			for _, src := range sources {
				if err := hit(src, t); err != nil {
					return err
				}
			}
		}
	}
	g.checkDamageTableTriggers(controller, table, false)
	return nil
}

// eachDamageTargets is getTargetEntities: the ability's own targets when it
// names ValidTgts$; for a Defined$ "Targeted" reference, every entity
// among the targets, cards and players alike (getDefinedEntities); for any
// other Defined$, players when it is a player reference and cards otherwise.
func eachDamageTargets(g *Game, a *Ability, host *Card) ([]EntityID, error) {
	if hasParam(a, "ValidTgts") {
		return a.Targets, nil
	}
	spec, ok := a.Params.Param("Defined")
	if !ok {
		spec = "Self"
	}
	switch spec {
	case "Targeted", "TargetedPlayer", "ThisTargetedCard":
		return a.Targets, nil
	}
	if players, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs()); err == nil {
		out := make([]EntityID, len(players))
		for i, p := range players {
			out[i] = PlayerEntity(p)
		}
		return out, nil
	}
	cards, err := definedCards(host, spec, a.refs())
	if err != nil {
		return nil, fmt.Errorf("engine: EachDamage: %w", err)
	}
	out := make([]EntityID, len(cards))
	for i, c := range cards {
		out[i] = CardEntity(c)
	}
	return out, nil
}
