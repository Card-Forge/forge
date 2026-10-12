// DealDamage: CR 119/120.1, M6's second script-driven effect. Two shapes:
// a plain-or-named-SVar NumDmg$ dealt to a Defined$ player or the ability's
// own host, and CR 115's own "any target" (or a narrower ValidTgts$)
// resolved at cast/activation time (Ability.Targets, resolveTargets,
// targeting.go). 2,068 real corpus lines combine DealDamage with ValidTgts$
// (`grep -c`, forge-gui/res/cardsfolder) -- the single largest unresolved
// DealDamage shape until now (`ValidTgts$ Any` alone: 615, the dominant one
// -- Lightning Bolt, Shock); 234 of the 2,068 also name a param still in
// dealDamageUnresolvedParams below and stay unresolved, leaving roughly
// 1,834 newly resolvable. Sourced from the ability's own host either way.
// 3 of the Defined$ shape's own real lines also name UnlessCost$ --
// resolveUnlessCost's own gate (effect.go) runs ahead of this file entirely
// now, below.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/DamageDealEffect.java's
// resolve.

package engine

//enginelint:allow id card game player ability combatdamage defined amount condition control trigger effecthelpers damageresolveeffect zone

import "fmt"

// dealDamageEffect resolves Mode$/DB$/AB$ DealDamage. Reuses the exact
// damage machinery combat already built: dealPermanentDamage/
// dealPlayerDamage (combatdamage.go) mark the damage, check CR 614's own
// "prevent all of this damage" replacement effects and CR 603's own "deals
// damage" trigger identically whether the source is a blocker or a script --
// isCombat threaded through as false is the one thing that tells the two
// apart (FlagCombat's own doc comment, event.go). A local damageTable
// accumulates every dealPlayerDamage/dealPermanentDamage call this one
// resolution makes (more than one when Defined$ names several players at
// once, or ValidTgts$'s own TargetMax$ lets more than one be chosen),
// consumed by checkDamageDoneOnceTriggers (trigger.go) once the whole
// resolution's own damage is dealt -- combatdamage.go's own doc comment on
// damageTable has the reason a single script-driven ability's own damage is
// one batch too, not just a combat damage step's.
//
// Not ported (every one fails loudly rather than dealing the wrong amount to
// the wrong thing, PORT-8/GO-7): DamageSource$ (a source other than the
// ability's own host, needing a reference vocabulary this file does not
// have); TriggeredSpellAbility$/CounterNum$/Optional$ (each its own further
// mechanic); DividedAsYouChoose$ (CR 601.2c's own Forked Bolt shape --
// forked_bolt.txt -- splits one NumDmg$ unevenly across several targets, an
// allocation Java records at target-choosing time, `SpellAbility
// .addDividedAllocation`, that this port's own resolveTargets/ChooseTargets
// has nowhere to carry; every target would otherwise wrongly take the
// ability's own full NumDmg$); NoPrevention$ (this port's own
// damagePrevented/damagePreventedPlayer would otherwise apply where Java's
// own AbilityKey.NoPreventDamage says not to, a wrong answer rather than a
// missing one). TgtPrompt$ is display prose, not gated here the same as
// Destroy's own identical param is not (destroyeffect.go). SubAbility$ no
// longer blocks: resolveSubAbility
// (subability.go) chains it through Registry.Resolve (effect.go) once this
// effect's own body finishes, whether or not subAbilityConditionMet below
// let it run at all -- sword_of_fire_and_ice_and_war_and_peace.txt's own
// real DealDamage-chaining-into-GainLife shape is why. 9 of the corpus's
// own 316 real SVar-defined DealDamage lines naming SubAbility$ chain to an
// already-built leaf ability and resolve end to end. UnlessPayer$/
// UnlessCost$/UnlessResolveSubs$ no longer block either: resolveUnlessCost
// (effect.go) gates the whole ability, this effect's own body included,
// before Registry.Resolve ever reaches it -- force_of_nature.txt's own real
// "deals 8 damage to you unless you pay {G}{G}{G}{G}" is the shape.
//
// ConditionPresent$/ConditionCompare$/ConditionCheckSVar$/
// ConditionSVarCompare$ -- SpellAbilityCondition's own gate on the ability
// itself, distinct from CardTraitBase's own meetsCommonRequirements a
// trigger already has -- are resolved now too (subAbilityConditionMet,
// condition.go), 5 more of the 822 real Defined$ lines: a met condition lets
// this run as normal, an unmet one is not an error, the ability simply does
// nothing (SpellAbilityCondition.areMet's own contract), the identical
// "declined by the rules" outcome PlayLand/PayManaCost already report as a
// plain false/nil rather than an error. Condition$ itself (SpellAbilityCondition's
// own separate Threshold/Metalcraft/... flag switch) and ConditionDefined$
// (an arbitrary reference this port has no Defined$-to-objects resolver for)
// stay in dealDamageUnresolvedParams below, failing loudly the identical way
// DamageSource$ already does -- condition.go's own generic
// subAbilityConditionMet would otherwise silently no-op a card naming either,
// which this file's own established contract (every unresolvable param fails
// loudly by name, never silently) does not allow.
//
// ValidTgts$ (dealDamageTargets, below) reads Ability.Targets directly --
// resolveTargets (targeting.go) already resolved it at cast/activation time,
// the identical answer Destroy/Tap already read the same way. Unlike those,
// this file adds its own per-target liveness check right before applying
// damage: DamageDealEffect.java's own resolve loop skips a card target that
// is no longer on the battlefield, still damaging every other target, on
// top of the shared CR 608.2b re-check resolveTop already ran
// (targetsStillLegal, targeting.go). It matters for a target the shared
// check keeps -- one chosen outside the battlefield, such as a spell on the
// stack (`TestRemoveFromGameSpellOnStack`'s shape). No such check exists for a player
// target in Java's own loop, so none is added here either -- a player who
// has since lost the game is not filtered out.
type dealDamageEffect struct{}

var dealDamageUnresolvedParams = [...]string{
	"DamageSource", "Condition", "TriggeredSpellAbility", "CounterNum",
	"NoPrevention", "Optional", "DividedAsYouChoose",
}

func (dealDamageEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range dealDamageUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: DealDamage: %s$ not resolvable yet", key)
		}
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
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
	deathtouch := source.HasKeyword("Deathtouch")
	if hasParam(a, "DamageMap") && a.damageMap == nil {
		a.damageMap = &pendingDamage{}
	}

	if _, ok := a.Params.Param("ValidTgts"); ok {
		return dealDamageTargets(g, controller, a, dmg, deathtouch)
	}

	defined, _ := a.Params.Param("Defined")
	if a.damageMap != nil {
		// DamageMap$: the damage is recorded, and dealt all at once by a
		// later DamageResolve (DamageDealEffect's usedDamageMap).
		if defined == "Self" {
			a.damageMap.add(a.Source, CardEntity(a.Source), dmg)
			return nil
		}
		players, err := definedPlayers(g, a.Controller, a.Source, defined, a.refs())
		if err != nil {
			return fmt.Errorf("engine: DealDamage: %w", err)
		}
		for _, pid := range players {
			a.damageMap.add(a.Source, PlayerEntity(pid), dmg)
		}
		return nil
	}
	if defined == "Self" {
		var table damageTable
		g.dealPermanentDamage(controller, a.Source, a.Source, dmg, deathtouch, false, &table)
		g.checkDamageTableTriggers(controller, table, false)
		return nil
	}
	players, err := definedPlayers(g, a.Controller, a.Source, defined, a.refs())
	if err != nil {
		return fmt.Errorf("engine: DealDamage: %w", err)
	}
	var table damageTable
	for _, pid := range players {
		g.dealPlayerDamage(controller, a.Source, pid, dmg, false, &table)
	}
	g.checkDamageTableTriggers(controller, table, false)
	return nil
}

// dealDamageTargets is dealDamageEffect's own ValidTgts$ shape -- CR 115's
// own "any target" (or a narrower spec), dmg to each already-chosen target
// (a.Targets, resolveTargets, targeting.go). DamageDealEffect.java's own
// resolve reads its own already-resolved target list once, a single loop
// with an instanceof check (DamageDealEffect.java) rather than separate
// card/player passes -- ported the same shape here rather than through
// targetedOrDefinedCards/targetedOrDefinedPlayers (defined.go), which exist
// for the Defined$ shape's own different resolution and would need to be
// called twice for no reason once a.Targets already holds the mixed answer.
func dealDamageTargets(g *Game, controller PlayerController, a *Ability, dmg int, deathtouch bool) error {
	var table damageTable
	radiant := g.radianceCards(a)
	for _, e := range a.Targets {
		if cid, ok := e.AsCard(); ok {
			// CR 608.2b-adjacent, DealDamage's own (DamageDealEffect.java):
			// a target that left the battlefield between targeting and
			// resolution is skipped, the rest of the targets still take
			// their damage.
			if g.Card(cid).Zone != Battlefield {
				continue
			}
			if a.damageMap != nil {
				a.damageMap.add(a.Source, e, dmg)
				continue
			}
			g.dealPermanentDamage(controller, a.Source, cid, dmg, deathtouch, false, &table)
			continue
		}
		pid, ok := e.AsPlayer()
		if !ok {
			continue
		}
		if a.damageMap != nil {
			a.damageMap.add(a.Source, e, dmg)
			continue
		}
		g.dealPlayerDamage(controller, a.Source, pid, dmg, false, &table)
	}
	// Radiance$: the permanents sharing a color with a target take the same
	// damage (DamageDealEffect.java:195, :263-267).
	for _, cid := range radiant {
		if g.Card(cid).Zone != Battlefield {
			continue
		}
		if a.damageMap != nil {
			a.damageMap.add(a.Source, CardEntity(cid), dmg)
			continue
		}
		g.dealPermanentDamage(controller, a.Source, cid, dmg, deathtouch, false, &table)
	}
	if a.damageMap == nil {
		g.checkDamageTableTriggers(controller, table, false)
	}
	return nil
}
