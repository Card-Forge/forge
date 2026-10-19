// SacrificeAll: Sacrifice's own blanket sibling -- CR 701.20 applied to a
// valid-string-matched set across every player rather than a single
// Defined$/targeted card, `pumpAllEffect`'s own shape (pumpalleffect.go)
// reused for a second blanket effect.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/SacrificeAllEffect.java's
// resolve; sacrificeCards (sacrificeeffect.go) does the actual sacrificing,
// shared with sacrificeEffect's own two branches outright.

package engine

import (
	"fmt"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// sacrificeAllUnresolvedParams names SacrificeAllEffect.resolve's own
// params this port does not evaluate. Every one fails the whole line
// loudly rather than sacrificing the wrong set (PORT-8/GO-7):
// ConditionDefined$ (3) -- SpellAbilityCondition's own shape
// subAbilityConditionMet does not cover, the identical GainLife/LoseLife/
// Sacrifice-shaped gap; Activator$ (1) -- a restriction on who activated the
// ability rather than on what it affects, a further mechanic; SorcerySpeed$
// (1) -- a cost-restriction flag with no cost-payment site to attach to;
// ImprintSacrificed$ (1) -- Card.Memory has an Imprint writer (memory.go)
// but no caller yet, not worth building for the one real line naming it.
//
// SubAbility$ chains through resolveSubAbility (subability.go,
// Registry.Resolve, effect.go) once this effect's own body finishes,
// whether or not subAbilityConditionMet let it run at all, the identical
// shape every other M6 effect already has -- not named here because it
// never blocks.
//
// UnlessCost$/UnlessPayer$ no longer block either: resolveUnlessCost
// (effect.go) gates the whole ability before Registry.Resolve ever reaches
// it, the identical gap Sacrifice's own already documented. 0 of the
// corpus's own 6 real SacrificeAll lines naming UnlessCost$ resolve,
// though: ashling_the_limitless.txt's own real pure-mana "{W}{U}{B}{R}{G}"
// is the only one clearing resolveUnlessCost's own pure-mana-cost/
// resolvable-payer filter, and its own Defined$ DelayTriggerRememberedLKI
// is reached only through DB$ DelayedTrigger, a general delayed-trigger
// mechanic this port does not build; every other real line names a
// PayEnergy<.../DefinedCost_.../X-shard UnlessCost$ or a controller-derived
// UnlessPayer$ (EnchantedController) this port cannot resolve.
var sacrificeAllUnresolvedParams = [...]string{
	"Activator", "ImprintSacrificed",
}

type sacrificeAllEffect struct{}

// Resolve gathers the affected cards two ways, matching
// SacrificeAllEffect.resolve's own branch: Defined$ present names specific
// cards (definedCards, defined.go -- Self/Enchanted/Equipped/Targeted, an
// unrecognized value failing loudly rather than silently sacrificing
// nothing); Defined$ absent scans every battlefield in the game,
// ValidCards$-filtered if present (Java's own
// `game.getCardsIn(Battlefield)` then an optional
// `AbilityUtils.filterListByType`) -- 72 of the corpus's own 140 real
// SacrificeAll lines, the corpus's own dominant real shape, name no
// Defined$ at all. Controller$, when present, narrows either set further
// to cards controlled by one of its own resolved players (definedPlayers,
// defined.go), Java's own "do the controller check after LKI got updated"
// step reordered here since this port takes no LKI snapshot until the
// actual sacrifice happens (sacrificeCards, sacrificeeffect.go). 92 of the
// corpus's own 140 real lines resolve, the sole real line naming
// Planeswalker$ among them now that it no longer blocks (CR 606.3's own
// loyalty-ability restriction is a cost-side gate, activateability.go,
// never a restriction on how the effect it pays for resolves).
func (sacrificeAllEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range sacrificeAllUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: SacrificeAll: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}

	var cards []CardID
	if defined, ok := a.Params.Param("Defined"); ok {
		var err error
		cards, err = definedCards(source, defined, a.refs())
		if err != nil {
			return fmt.Errorf("engine: SacrificeAll: %w", err)
		}
	} else {
		for _, pid := range g.Players() {
			cards = append(cards, g.Zone(Battlefield, pid).Cards()...)
		}
		if validCards, ok := a.Params.Param("ValidCards"); ok {
			spec := valid.Parse(validCards)
			var filtered []CardID
			for _, cid := range cards {
				if Matches(g, g.Card(cid), spec, source.Controller(), a.Source) {
					filtered = append(filtered, cid)
				}
			}
			cards = filtered
		}
	}

	if controllerParam, ok := a.Params.Param("Controller"); ok {
		players, err := definedPlayers(g, a.Controller, a.Source, controllerParam, a.refs())
		if err != nil {
			return fmt.Errorf("engine: SacrificeAll: %w", err)
		}
		allowed := make(map[PlayerID]bool, len(players))
		for _, pid := range players {
			allowed[pid] = true
		}
		var filtered []CardID
		for _, cid := range cards {
			if allowed[g.Card(cid).Controller()] {
				filtered = append(filtered, cid)
			}
		}
		cards = filtered
	}

	sacrificeCards(g, controller, a, cards)
	return nil
}
