// DealDamage: CR 119/120.1, M6's second script-driven effect. The damage is
// dealt to CR 115's own "any target" (or a narrower ValidTgts$) chosen at
// cast/activation time (Ability.Targets, resolveTargets, targeting.go), or
// to what Defined$ names, by each card DamageSource$ names -- the ability's
// own host when it names none.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/DamageDealEffect.java's
// resolve and internalDamageDeal, and the RememberDamaged$ half of
// GameAction.dealDamage (GameAction.java:2787-2794).

package engine

//enginelint:allow id card game player ability combatdamage defined amount condition control trigger effecthelpers damageresolveeffect zone valid parts excessdamage replacedying

import (
	"fmt"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/valid"
)

// dealDamageEffect resolves Mode$/DB$/AB$ DealDamage the way
// DamageDealEffect.resolve does: the sources first (DamageSource$,
// DamageDealEffect.java:135-138 -- none left, no damage), then the targets,
// then every source's damage to every target recorded into one
// CardDamageTable (:217-268) and dealt as one batch (GameAction.dealDamage,
// :269-271). The batch is a pendingDamage (damageresolveeffect.go): the one a
// DamageMap$ chain shares, dealt later by DamageResolve, or a local one dealt
// here by dealPendingDamage. Dealing reuses combat's machinery
// (dealPermanentDamage/dealPlayerDamage, combatdamage.go), which reads the
// source's own lifelink, infect, wither and protection-relevant qualities off
// the source CardID it is given -- so a "target creature you control deals
// damage equal to its power" bite has that creature's deathtouch and
// lifelink, not the spell's. checkDamageTableTriggers then checks the whole
// batch's damage-done-once triggers (combatdamage.go's damageTable doc).
//
// DamageSource$ resolves through definedCards (ParentTarget -- Rabid Bite's
// "target creature you control", 88 of the corpus's 190 real lines --
// Targeted, Enchanted, Remembered, Imprinted, ChosenCard, Self, the
// Triggered* card keys, ...) and "Valid <spec>", every battlefield card
// matching spec (AbilityUtils.java:287-303). The source is read as it is
// now, not as a last-known copy (sourceLKI, :218): a source still on the
// battlefield is the same either way, and a ParentTarget or Targeted source
// that left was dropped as an illegal target already (CR 608.2b,
// dropIllegalTargets).
//
// A card target no longer on the battlefield, or phased out, is skipped
// (:253-256); Java's equalsWithGameTimestamp half of that check is not made
// for a Defined$ card, which this port does not stamp.
//
// DividedAsYouChoose$ (Arc Lightning's "3 damage divided as you choose among
// one, two, or three targets"): the division was announced with the targets
// (CR 601.2d, divideAmongTargets) and each target takes its own share from
// each source (:248), NumDmg$ unread for it. A target dropped under CR 608.2b
// or gone from the battlefield takes its share with it; the others keep
// theirs.
//
// Not ported (each fails loudly by name rather than dealing the wrong amount
// to the wrong thing, PORT-8/GO-7): see dealDamageUnresolvedParams.
type dealDamageEffect struct{}

// dealDamageUnresolvedParams are DamageDealEffect params this port does not
// evaluate:
//
//   - Condition$ (SpellAbilityCondition's flag switch; subAbilityConditionMet
//     would otherwise silently no-op it), TriggeredSpellAbility$, CounterNum$,
//     Optional$ -- each its own further mechanic;
//   - NoPrevention$/NoRedirection$ -- damagePrevented would apply where
//     AbilityKey.NoPreventDamage says not to;
//   - DivideEvenly$/DividerOnResolution$ -- Fireball's even split and
//     Master of the Wild Hunt's assignCombatDamage at resolution
//     (:188-193, :220-241). DividedAsYouChoose$ resolves: each target takes
//     the share announced with it (perTargetAmounts, dividedtargets.go);
//   - CardChoices$/PlayerChoices$/Random$ -- a choice outside Defined$/
//     targets (:150-176);
//   - RelativeTarget$ -- Defined$ re-read relative to each source (:243-245);
//   - RememberAmount$/RememberRedirectedExcess$ -- the dealt total or the
//     redirected excess remembered on the host.
//
// ReplaceDyingDefined$/ReplaceDyingCondition$ resolve through replaceDying
// (replacedying.go), after the damage (:272) -- or, under DamageMap$, before
// the later DamageResolve deals it, as Java's own order has it.
// ExcessSVar$/ExcessSVarCondition$ resolve through bindExcessSVar
// (excessdamage.go); under DamageMap$ they are refused, since the SVar would
// be set on the DamageResolve that deals the batch (GameAction.dealDamage's
// cause), not on this ability.
var dealDamageUnresolvedParams = [...]string{
	"Condition", "TriggeredSpellAbility", "CounterNum", "Optional",
	"NoPrevention", "NoRedirection",
	"DivideEvenly", "DividerOnResolution",
	"CardChoices", "PlayerChoices", "Random",
	"RelativeTarget",
	"RememberAmount", "RememberRedirectedExcess",
}

func (dealDamageEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	if err := rejectParams(a, "DealDamage", dealDamageUnresolvedParams[:]...); err != nil {
		return err
	}
	if tgts, ok := a.Params.Param("ValidTgts"); ok && strings.Contains(tgts, "NotDefined") {
		// CardProperty's NotDefined<Defined> (CardProperty.java:2064-2068)
		// needs the targeting ability's own references, which Matches has
		// no access to: it would read false for every card (GO-7).
		return fmt.Errorf("engine: DealDamage: ValidTgts$ %q: NotDefined property not resolvable yet", tgts)
	}
	if err := checkReplaceDying(g, a, "DealDamage"); err != nil {
		return err
	}
	if err := checkExcessSVar(a, "DealDamage"); err != nil {
		return err
	}
	defaultExcessSVar(a)
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	sources, err := damageSources(g, a)
	if err != nil {
		return err
	}
	if len(sources) == 0 {
		return nil
	}
	numDmg, ok := a.Params.Param("NumDmg")
	if !ok {
		return fmt.Errorf("engine: DealDamage: NumDmg$ missing")
	}
	dmg, ok := resolveNamedAmount(g, a.Amounts, source, numDmg)
	if !ok {
		return fmt.Errorf("engine: DealDamage: NumDmg$ %q is not resolvable", numDmg)
	}
	if hasParam(a, "DamageMap") && a.damageMap == nil {
		a.damageMap = &pendingDamage{}
	}
	if a.damageMap != nil && hasParam(a, "RememberDamaged") {
		// GameAction.dealDamage remembers for the ability that deals the
		// batch, the later DamageResolve, not this one.
		return fmt.Errorf("engine: DealDamage: RememberDamaged$ under DamageMap$ not resolvable yet")
	}
	targets, err := dealDamageTargets(g, a)
	if err != nil {
		return err
	}
	// DamageDealEffect.java:248: a DividedAsYouChoose$ target takes its own
	// share, every other target NumDmg$.
	amounts, err := a.perTargetAmounts("DealDamage", targets, dmg)
	if err != nil {
		return err
	}
	if spec, ok := a.Params.Param("OptionalDecider"); ok {
		// DamageDealEffect.java:180-185: the first player OptionalDecider$
		// names may decline the whole damage; nobody named, nobody asked.
		deciders, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs())
		if err != nil {
			return fmt.Errorf("engine: DealDamage: OptionalDecider$: %w", err)
		}
		if len(deciders) > 0 && !controller.ConfirmEffect(g, deciders[0], a.Source) {
			return nil
		}
	}
	radiant := g.radianceCards(a)

	batch := a.damageMap
	if batch == nil {
		batch = &pendingDamage{}
	}
	for _, src := range sources {
		deathtouch := g.Card(src).HasKeyword("Deathtouch")
		for i, e := range targets {
			if cid, ok := e.AsCard(); ok {
				if !damageTargetInPlay(g.Card(cid)) {
					continue
				}
				if err := recordCardDamage(g, a, batch, src, cid, amounts[i], deathtouch); err != nil {
					return err
				}
				continue
			}
			if _, ok := e.AsPlayer(); ok {
				batch.add(src, e, amounts[i])
			}
		}
		// Radiance$: the permanents sharing a color with a target take the
		// same damage (DamageDealEffect.java:195, :263-267).
		for _, cid := range radiant {
			if g.Card(cid).Zone != Battlefield {
				continue
			}
			if err := recordCardDamage(g, a, batch, src, cid, dmg, deathtouch); err != nil {
				return err
			}
		}
	}
	if a.damageMap != nil {
		// DamageMap$: dealt all at once by a later DamageResolve; the
		// ReplaceDying effect is made now (DamageDealEffect.java:269-272).
		return g.replaceDying(a, "DealDamage")
	}
	table := g.dealPendingDamage(controller, batch.entries)
	if hasParam(a, "RememberDamaged") {
		rememberDamaged(g, a.Source, table)
	}
	bindExcessSVar(g, a, table)
	g.checkDamageTableTriggers(controller, table, false)
	return g.replaceDying(a, "DealDamage")
}

// damageTargetInPlay is DamageDealEffect's per-card check (:253-256): on the
// battlefield and not phased out.
func damageTargetInPlay(c *Card) bool {
	return c.Zone == Battlefield && !c.IsPhasedOut()
}

// damageSources is DamageSource$ (DamageDealEffect.java:135):
// AbilityUtils.getDefinedCards, "Self" when absent. "Valid <spec>" is every
// battlefield card matching spec for the ability's controller (AbilityUtils
// .java:287-303); every other spelling goes through definedCards.
func damageSources(g *Game, a *Ability) ([]CardID, error) {
	spec, ok := a.Params.Param("DamageSource")
	if !ok {
		return []CardID{a.Source}, nil
	}
	if rest, ok := strings.CutPrefix(spec, "Valid "); ok {
		parsed := valid.Parse(rest)
		var out []CardID
		for _, pid := range g.Players() {
			for _, cid := range g.Zone(Battlefield, pid).Cards() {
				if Matches(g, g.Card(cid), parsed, a.Controller, a.Source) {
					out = append(out, cid)
				}
			}
		}
		return out, nil
	}
	cards, err := definedCards(g.Card(a.Source), spec, a.refs())
	if err != nil {
		return nil, fmt.Errorf("engine: DealDamage: DamageSource$: %w", err)
	}
	return cards, nil
}

// dealDamageTargets is SpellAbilityEffect.getTargetEntities
// (SpellAbilityEffect.java:374-383): the chosen targets when the ability
// names ValidTgts$ (already re-checked under CR 608.2b by resolveTop), else
// AbilityUtils.getDefinedEntities over each " & " part of Defined$ --
// players, then cards -- defaulting to "Self".
func dealDamageTargets(g *Game, a *Ability) ([]EntityID, error) {
	if _, ok := a.Params.Param("ValidTgts"); ok {
		return a.Targets, nil
	}
	defined, ok := a.Params.Param("Defined")
	if !ok {
		defined = "Self"
	}
	var out []EntityID
	for _, part := range strings.Split(defined, " & ") {
		part = strings.TrimSpace(part)
		players, perr := definedPlayers(g, a.Controller, a.Source, part, a.refs())
		cards, cerr := definedCards(g.Card(a.Source), part, a.refs())
		if perr != nil && cerr != nil {
			// Neither reader knows it. The player reader's reason is the
			// one this effect reported before it read cards too, and the
			// more specific one for a Triggered* player key.
			return nil, fmt.Errorf("engine: DealDamage: %w", perr)
		}
		for _, p := range players {
			out = append(out, PlayerEntity(p))
		}
		for _, c := range cards {
			out = append(out, CardEntity(c))
		}
	}
	return out, nil
}

// recordCardDamage is internalDamageDeal (DamageDealEffect.java:275-302):
// dmg from src to card cid into batch. Under ExcessDamage$ -- when the source
// meets ExcessDamageCondition$, if named -- only what is lethal goes to cid
// (Card.getExcessDamageValue) and the rest to the first entity ExcessDamage$
// names: Flame Spill's "excess damage is dealt to that creature's controller
// instead".
func recordCardDamage(g *Game, a *Ability, batch *pendingDamage, src, cid CardID, dmg int, deathtouch bool) error {
	spec, ok := a.Params.Param("ExcessDamage")
	if ok {
		if cond, has := a.Params.Param("ExcessDamageCondition"); has && !Matches(g, g.Card(src), valid.Parse(cond), a.Controller, a.Source) {
			ok = false
		}
	}
	if !ok {
		batch.add(src, CardEntity(cid), dmg)
		return nil
	}
	toTarget := min(excessDamageValue(g.Card(cid), deathtouch), dmg)
	batch.add(src, CardEntity(cid), toTarget)
	rest, err := definedEntities(g, a.Controller, g.Card(a.Source), spec, a.refs())
	if err != nil {
		return fmt.Errorf("engine: DealDamage: ExcessDamage$: %w", err)
	}
	if len(rest) > 0 {
		batch.add(src, rest[0], dmg-toTarget)
	}
	return nil
}
