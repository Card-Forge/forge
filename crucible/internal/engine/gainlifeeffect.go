// GainLife: CR 119.1, M6's third script-driven effect, and the corpus's
// single largest resolvable slice past DealDamage -- a plain-or-named-SVar
// LifeAmount$ granted to a Defined$ player, no target -- 862 of the corpus's
// 1,700 real (AB|DB)$ GainLife lines that also name Defined$
// You/Player.Opponent and carry no other unresolved param. A line naming no
// Defined$ at all -- a chosen ValidTgts$ player, or the activator by
// default (gainLifePlayers, below) -- is 796 of the corpus's 1,804
// (SP|AB|DB)$ GainLife lines, 774 of them naming no unresolved param. 5 of
// the Defined$ lines
// naming Planeswalker$ too, Ajani Goldmane's own real "[+1]: You gain 2
// life" among them, no longer blocked (below): CR 606.3's own loyalty
// ability restriction is a cost-side gate (ActivateAbility/
// ActivateManaAbility, activateability.go), never a restriction on how the
// effect it pays for actually resolves.
//
// Ported from
// forge-game/src/main/java/forge/game/ability/effects/LifeGainEffect.java's
// resolve.

package engine

//enginelint:allow id card game player ability event trigger defined amount condition control

import "fmt"

// gainLifeEffect resolves Mode$/DB$/AB$ GainLife. Player.Life is a plain
// field; CR 119's own "life gain replacement" family (Event$ GainLife, 21
// real replacement lines) has real content now too, both checked per player
// before Life is touched at all: gainLifePrevented (replacement.go) resolves
// sulfuric_vortex.txt's own bare Prevent$ True, the only one of the 21
// naming Prevent$ at all; gainLifeReplaced (replacement.go) carries CR 616's
// own two outcomes for the other 20, a full substitution (19 of them --
// Draw/LoseLife/ReplaceEffect target shapes, gainLifeReplaced's own doc
// comment has the full corpus accounting and the 1 that stays unresolved) or
// a resized gain, the same "reduce or replace the amount, keep going through
// the normal path" contract damagePrevented's/damageReplaced's own split
// already has for a different Event$ -- gain, below, is the number
// gainLifeReplaced itself says should actually be granted, already run
// through both outcomes. LifeChanged, below, is the identical event
// dealPlayerDamage already emits for a life LOSS, reused here for a gain.
//
// Not ported (every one fails loudly rather than granting the wrong amount
// to the wrong player, PORT-8/GO-7): a Condition$ value subAbilityConditionMet
// does not read (rejectUnreadCondition; Kicked and OptionalCost with its
// ConditionOptionalPaid$ resolve) and ConditionZone$.
//
// UnlessCost$/UnlessPayer$ no longer block either: resolveUnlessCost
// (effect.go) gates the whole ability before Registry.Resolve ever reaches
// it. 0 of the corpus's own 3 real GainLife lines naming UnlessCost$ clear
// that gate's own pure-mana-cost/resolvable-payer filter at all (each
// names a Sac<.../Reveal<... cost part or a controller-derived UnlessPayer$
// this port cannot resolve).
//
// ConditionPresent$/ConditionCompare$/ConditionCheckSVar$/ConditionSVarCompare$
// are resolved through subAbilityConditionMet (condition.go) the identical
// way DealDamage's own do. SubAbility$ chains through resolveSubAbility
// (subability.go, effect.go's own Registry.Resolve) once this effect's own
// body finishes -- CR's own "then" (Sphinx Sovereign, Rogue Refiner, ...)
// -- and runs whether or not subAbilityConditionMet above let this effect's
// own body run at all, resolveApiAbility's own unconditional
// resolveSubAbilities pairing (subability.go's own doc comment). 18 of the
// corpus's own 253 real SVar-defined GainLife lines naming SubAbility$
// chain to an already-built leaf ability (no further SubAbility$ of its
// own) and resolve end to end; a chain more than one deep, or one whose
// target is not built yet, is not counted here -- each of those effects'
// own count already tracks that half of the question.
type gainLifeEffect struct{}

var gainLifeUnresolvedParams = [...]string{
	"ConditionZone",
}

func (gainLifeEffect) Resolve(g *Game, a *Ability, controller PlayerController) error {
	for _, key := range gainLifeUnresolvedParams {
		if _, ok := a.Params.Param(key); ok {
			return fmt.Errorf("engine: GainLife: %s$ not resolvable yet", key)
		}
	}
	if err := rejectUnreadCondition(a, "GainLife"); err != nil {
		return err
	}
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	lifeAmount, ok := a.Params.Param("LifeAmount")
	if !ok {
		return fmt.Errorf("engine: GainLife: LifeAmount$ missing")
	}
	amount, ok := resolveNamedAmount(g, a.Amounts, source, lifeAmount)
	if !ok {
		return fmt.Errorf("engine: GainLife: LifeAmount$ %q is not resolvable", lifeAmount)
	}
	players, err := gainLifePlayers(g, a)
	if err != nil {
		return fmt.Errorf("engine: GainLife: %w", err)
	}
	for _, pid := range players {
		g.gainLife(controller, pid, amount, a.Source, a)
	}
	return nil
}

// gainLife is the step LifeGainEffect.resolve and lifelink (GameAction.java
// dealDamage, CR 702.15e) share, Player.gainLife: a player no longer in the
// game or under a CantGainLife static gains nothing, a prevention or
// replacement can stop or change the amount, and a gain that survives updates the life total, the event and
// the LifeGained triggers. cause is the spell or ability making the player
// gain (Java's SourceSA), nil for lifelink and the SetLife/ExchangeLife paths,
// which do not carry one here; a GainLife replacement's ValidSource$ reads it.
func (g *Game) gainLife(controller PlayerController, pid PlayerID, amount int, source CardID, cause *Ability) {
	if g.cantGainLife(pid) || g.gainLifePrevented(pid) {
		return
	}
	gain := g.gainLifeReplaced(controller, pid, amount, cause)
	if gain <= 0 {
		return
	}
	firstGain := g.Player(pid).LifeGainedTimesThisTurn == 0
	g.Player(pid).LifeGainedTimesThisTurn++
	g.Player(pid).LifeGainedThisTurn += gain
	g.Player(pid).Life += gain
	g.sink.Emit(Event{Kind: LifeChanged, Source: source, Target: PlayerEntity(pid), Amount: int32(gain)})
	g.checkLifeGainedTriggers(controller, pid, gain, firstGain)
}

// gainLifePlayers is LifeGainEffect.resolve's getTargetPlayersWithDuplicates
// (true, "Defined", sa) (SpellAbilityEffect.java:326-345): Defined$ first
// when the line names it, else the chosen player targets (ValidTgts$,
// Ability.Targets), else "You" -- the default Java's own
// getParamOrDefault(definedParam, "You") supplies (SpellAbilityEffect.java:339).
// Nourish's own bare "SP$ GainLife | LifeAmount$ 6" is that last shape.
func gainLifePlayers(g *Game, a *Ability) ([]PlayerID, error) {
	if defined, ok := a.Params.Param("Defined"); ok {
		return definedPlayers(g, a.Controller, a.Source, defined, a.refs())
	}
	if _, ok := a.Params.Param("ValidTgts"); ok {
		var players []PlayerID
		for _, e := range a.Targets {
			if pid, ok := e.AsPlayer(); ok {
				players = append(players, pid)
			}
		}
		return players, nil
	}
	return []PlayerID{a.Controller}, nil
}
