// Excess damage (CR 120.10): how much of a batch's damage to a permanent was
// more than lethal, the ExcessSVar$ that hands it to the rest of the ability
// chain, the per-turn excess record and the ExcessDamage/ExcessDamageAll
// trigger modes.
//
// Ported from forge-game/src/main/java/forge/game/card/Card.java's
// getExcessDamageValue (:6127-6151), GameAction.dealDamage's lethal-damage
// snapshot (GameAction.java:2738-2752) and CardDamageTable.triggerExcessDamage
// (CardDamageTable.java:113-163), TriggerExcessDamage.java and
// TriggerExcessDamageAll.java.

package engine

//enginelint:allow id card parts game ability valid trigger effecthelpers control

import (
	"fmt"
	"slices"
	"strings"

	"github.com/jczastkiewicz/crucible/internal/carddb/compile"
	"github.com/jczastkiewicz/crucible/internal/cardtype"
	"github.com/jczastkiewicz/crucible/internal/valid"
)

// excessDamageValue is Card.getExcessDamageValue (Card.java:6127-6151, CR
// 120.10): the least of what is lethal to c as a creature (1 from a
// deathtouch source while any is lethal), its loyalty as a planeswalker and
// its defense as a battle; 0 for none of the three.
func excessDamageValue(c *Card, deathtouch bool) int {
	values := make([]int, 0, 3)
	t := c.Type()
	if t.Has(cardtype.Creature) {
		lethal := 0
		if tough, ok := c.Toughness(); ok {
			lethal = tough - c.Damage.Marked
		}
		if deathtouch && lethal > 0 {
			lethal = 1
		}
		values = append(values, max(0, lethal))
	}
	if t.Has(cardtype.Planeswalker) {
		values = append(values, c.Counters.Count(Loyalty))
	}
	if t.Has(cardtype.Battle) {
		values = append(values, c.Counters.Count(Defense))
	}
	if len(values) == 0 {
		return 0
	}
	return slices.Min(values)
}

// damageable reports whether c can be dealt damage at all (CR 120.1a,
// Card.java:6325-6327): a creature, a planeswalker or a battle.
func damageable(c *Card) bool {
	t := c.Type()
	return t.Has(cardtype.Creature) || t.Has(cardtype.Planeswalker) || t.Has(cardtype.Battle)
}

// defaultExcessSVar binds an ability's ExcessSVar$ name to 0 when the chain
// does not already define it, before the effect deals anything. Java sets the
// SVar only once GameAction.dealDamage runs for the ability
// (CardDamageTable.java:153-155); an ability that returns before dealing (no
// source left, an OptionalDecider$ who declined, an unmet condition, fewer than
// two fighters) leaves it unset, and AbilityUtils.calculateAmount reads an
// unset SVar as 0 (AbilityUtils.java:415-423). Binding the 0 reproduces that
// read (PORT-7): without it, Windswift Slice whose biter left the battlefield
// would fail its TokenAmount$ Excess instead of making no token. A name the
// card itself defines keeps the card's value, as Java's fallback to the card's
// SVar does.
func defaultExcessSVar(a *Ability) {
	name, ok := a.Params.Param("ExcessSVar")
	if !ok {
		return
	}
	if _, defined := a.Amounts[strings.ToLower(name)]; defined {
		return
	}
	a.Amounts = withAmount(a.Amounts, name, 0)
}

// bindExcessSVar is CardDamageTable.triggerExcessDamage's ExcessSVar$ half
// (CardDamageTable.java:113-155): for every permanent table damaged, the
// damage it was dealt across every source of the batch minus what was lethal
// to it before any of it (damageEntry.Lethal, GameAction.java:2750-2752) -- 1
// instead when deathtouch damage was dealt to it -- summed over the damaged
// permanents that match ExcessSVarCondition$, if named, and bound as the
// ExcessSVar$ name on a's amounts (cause.setSVar). The sub-abilities a chains
// inherit the amounts (resolveSubAbility), Java's SpellAbility.getSVarFallback
// reading the parent's SVars. The sum is bound even when it is 0.
//
// ExcessSVarCondition$ is matched against the damaged permanent, not the
// source (CardDamageTable.java:137). Its targetedBy property is
// CardProperty.java:271-277, the root ability targeting the card
// (SpellAbility.isTargeting walks the whole chain): chainTargeted.
func bindExcessSVar(g *Game, a *Ability, table damageTable) {
	name, ok := a.Params.Param("ExcessSVar")
	if !ok {
		return
	}
	var cond *valid.Spec
	if spec, ok := a.Params.Param("ExcessSVarCondition"); ok {
		parsed := valid.Parse(spec)
		cond = &parsed
	}
	stored := 0
	for _, d := range batchExcess(g, table) {
		if d.excess <= 0 {
			continue
		}
		if cond != nil && !matchesWithTargetedBy(g, a, g.Card(d.card), *cond) {
			continue
		}
		stored += d.excess
	}
	a.Amounts = withAmount(a.Amounts, name, stored)
}

// damagedExcess is one permanent a damage batch dealt damage to and how much
// of it was past lethal (0 or less for none).
type damagedExcess struct {
	card   CardID
	excess int
}

// batchExcess is CardDamageTable.triggerExcessDamage's per-permanent sum
// (CardDamageTable.java:117-126): for every permanent table dealt damage, the
// damage it was dealt across every source of the batch minus what was lethal
// to it before any of it (damageEntry.Lethal, GameAction.java:2750-2752), 1
// instead when deathtouch damage has been dealt to it. In first-damaged
// order; Java iterates the lethal-damage snapshot as a HashMap
// (GameAction.java:2738), whose order no game rule fixes. A card none of
// creature, planeswalker, battle is left out: Java's addDamageAfterPrevention
// deals it 0 (Card.java:6325-6327).
func batchExcess(g *Game, table damageTable) []damagedExcess {
	type damaged struct {
		card        CardID
		sum, lethal int
	}
	var order []damaged
	for _, e := range table {
		cid, ok := e.Target.AsCard()
		if !ok || e.Amount <= 0 {
			continue
		}
		i := slices.IndexFunc(order, func(d damaged) bool { return d.card == cid })
		if i < 0 {
			order = append(order, damaged{card: cid, lethal: e.Lethal})
			i = len(order) - 1
		}
		order[i].sum += e.Amount
	}
	out := make([]damagedExcess, 0, len(order))
	for _, d := range order {
		c := g.Card(d.card)
		if !damageable(c) {
			continue
		}
		threshold := d.lethal
		if c.Damage.Deathtouch {
			threshold = 1
		}
		out = append(out, damagedExcess{card: d.card, excess: d.sum - threshold})
	}
	return out
}

// excessSoFar is the excess damage c has been dealt in the batch table is
// recording, amount -- dealt now, with lethal the threshold before it --
// included: batchExcess for one card while the batch is still being dealt.
// dealPermanentDamage sets Damage.ExcessThisTurn from it before checking the
// DamageDone triggers of this exchange: Java tests those only after the whole
// batch (runWaitingTriggers after triggerExcessDamage, GameAction.java:2766-2779),
// so Zangief's ValidTarget$ Creature.wasDealtExcessDamageThisTurn already sees
// the excess the same damage dealt.
func excessSoFar(c *Card, table *damageTable, amount, lethal int) int {
	if !damageable(c) {
		return 0
	}
	sum, threshold, seen := amount, lethal, false
	if table != nil {
		for _, e := range *table {
			if e.Target != CardEntity(c.ID) || e.Amount <= 0 {
				continue
			}
			if !seen {
				threshold, seen = e.Lethal, true
			}
			sum += e.Amount
		}
	}
	if c.Damage.Deathtouch {
		threshold = 1
	}
	return sum - threshold
}

// triggerExcessDamage is CardDamageTable.triggerExcessDamage's record and
// trigger half (CardDamageTable.java:126-162), run once per damage batch
// (checkDamageTableTriggers): every permanent dealt excess damage is marked
// (setHasBeenDealtExcessDamageThisTurn), Mode$ ExcessDamage fires once for
// it, and Mode$ ExcessDamageAll fires once for the batch when any permanent
// was. Card.logExcessDamage's running total is not kept: its one reader,
// Count$ExcessDamageReceivedThisTurn, is read only through Overclocked
// Electromancer's CardTriggeredTarget$ head, which this port does not
// resolve.
//
// ExcessDamage (TriggerExcessDamage.performTest, :50-62) matches ValidTarget$
// against the damaged permanent and CombatDamage$ against isCombat, and
// records AbilityKey.Target and DamageAmount, the excess (:66-69): Defined$
// TriggeredTarget and TriggerCount$DamageAmount (Fall of Cair Andros, Toralf,
// God of Fury). ExcessDamageAll (TriggerExcessDamageAll.performTest, :18-29)
// keeps the permanents ValidTarget$ matches, needs one, and matches
// CombatDamage$ (Magmatic Galleon); its AbilityKey.Targets list is not
// recorded, since no corpus line reads it.
func (g *Game) triggerExcessDamage(controller PlayerController, table damageTable, isCombat bool) {
	var damaged []CardID
	var matches []Ability
	for _, d := range batchExcess(g, table) {
		if d.excess <= 0 {
			continue
		}
		c := g.Card(d.card)
		c.Damage.ExcessThisTurn = true
		damaged = append(damaged, d.card)
		objs := triggeredObjects{target: CardEntity(d.card), counts: triggerCounts{damage: d.excess, set: countDamage}}
		matches = append(matches, g.onceTriggerMatches("ExcessDamage", func(h *Card, t *compile.Ability) bool {
			return combatDamageMatches(t, isCombat) && validTargetMatches(g, h, t, c)
		}, objs)...)
	}
	g.pushTriggeredAbilities(controller, matches)
	if len(damaged) == 0 {
		return
	}
	g.pushTriggeredAbilities(controller, g.onceTriggerMatches("ExcessDamageAll", func(h *Card, t *compile.Ability) bool {
		if !combatDamageMatches(t, isCombat) {
			return false
		}
		return slices.ContainsFunc(damaged, func(id CardID) bool { return validTargetMatches(g, h, t, g.Card(id)) })
	}, triggeredObjects{}))
}

// combatDamageMatches is a damage trigger's CombatDamage$ check: absent
// passes, otherwise "True" must agree with isCombat.
func combatDamageMatches(t *compile.Ability, isCombat bool) bool {
	v, ok := t.Param("CombatDamage")
	return !ok || strings.EqualFold(v, "True") == isCombat
}

// validTargetMatches is a trigger's ValidTarget$ against card c, from the
// trigger host h (Trigger.matchesValidParam); absent passes.
func validTargetMatches(g *Game, h *Card, t *compile.Ability, c *Card) bool {
	spec, ok := t.Param("ValidTarget")
	return !ok || Matches(g, c, valid.Parse(spec), h.Controller(), h.ID)
}

// checkExcessSVar refuses, before the effect acts, an ExcessSVar$ shape
// bindExcessSVar does not evaluate: under DamageMap$ (Java's cause is the
// later DamageResolve that deals the batch, so this ability's SVar is never
// set), and an ExcessSVarCondition$ naming !targetedBy (no corpus line).
func checkExcessSVar(a *Ability, api string) error {
	if !hasParam(a, "ExcessSVar") {
		return nil
	}
	if hasParam(a, "DamageMap") || a.damageMap != nil {
		return fmt.Errorf("engine: %s: ExcessSVar$ under DamageMap$ not resolvable yet", api)
	}
	spec, ok := a.Params.Param("ExcessSVarCondition")
	if !ok {
		return nil
	}
	for _, alt := range valid.Parse(spec).Alternatives {
		for _, p := range alt.Properties {
			if p.Name == "targetedBy" && p.Negated {
				return fmt.Errorf("engine: %s: ExcessSVarCondition$ %q: !targetedBy not resolvable yet", api, spec)
			}
		}
	}
	return nil
}

// matchesWithTargetedBy is Matches for a valid string that may name the
// targetedBy property (CardProperty.java:271-277), which needs the ability:
// the property holds when the root ability's chain targets c
// (chainTargeted), and the rest of the alternative is matched as usual.
// checkExcessSVar has refused a negated !targetedBy.
func matchesWithTargetedBy(g *Game, a *Ability, c *Card, spec valid.Spec) bool {
	for _, alt := range spec.Alternatives {
		props := make([]valid.Property, 0, len(alt.Properties))
		needsTarget := false
		for _, p := range alt.Properties {
			if p.Name == "targetedBy" && !p.Negated {
				needsTarget = true
				continue
			}
			props = append(props, p)
		}
		if needsTarget && !chainTargeted(a, c.ID) {
			continue
		}
		rest := valid.Spec{Alternatives: []valid.Alternative{{Base: alt.Base, Properties: props}}}
		if Matches(g, c, rest, a.Controller, a.Source) {
			return true
		}
	}
	return false
}

// chainTargeted is SpellAbility.isTargeting on the root ability
// (SpellAbility.java:2112-2118) as far as a carries it: its own targets, the
// nearest targeting ancestor's (parentTargets) and every chain link's
// (chainTargets). An ancestor above the nearest targeting one is not carried;
// no ExcessSVarCondition$ line in the corpus sits under two targeting
// ancestors.
func chainTargeted(a *Ability, id CardID) bool {
	want := CardEntity(id)
	if slices.Contains(a.Targets, want) || slices.Contains(a.parentTargets, want) {
		return true
	}
	for _, ct := range a.chainTargets {
		if slices.Contains(ct.Targets, want) {
			return true
		}
	}
	return false
}
