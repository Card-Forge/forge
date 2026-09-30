// Camouflage: a DeclareBlocker replacement's ReplaceWith$ that declares a
// defending player's blocks as random piles (ADR-0035).

package engine

//enginelint:allow id card game player ability defined condition control combat block blockvalidation replaceeffect effecthelpers

import "fmt"

// camouflageEffect is CamouflageEffect.java: instead of declaring blockers,
// the declarer divides the defending player's creatures into one pile per
// attacking creature, the piles are assigned to the attackers at random,
// and each creature in a pile blocks its attacker if it can.
//
// It resolves only as a DeclareBlocker replacement's ReplaceWith$ ability
// (declareBlockersReplaced, replacement.go), adding its blocks to that
// event's combat; anywhere else is an error. Java would add them to the live
// Combat, but this port only holds a declaration being built on the event.
//
// Declarer and defender are read as two values, both from the replacing
// objects in the one real line: Defined$ ReplacedPlayer declares (default
// "You", getDefinedPlayersOrTargeted), Defender$ ReplacedDefendingPlayer
// defends (default "You", AbilityUtils.getDefinedPlayers(null)). Either
// resolving to nobody is an error, Java's .get(0) on an empty list.
//
// Java's AI branch (CamouflageEffect.java:64-74, declarer.isAI()) is a
// controller-implementation split, not a rules difference: this port's
// PlayerController has no "is AI" and runs the human branch for every
// controller, one ChooseCardsForEffect per attacker. AILogic$ is that
// branch's hint and is not read.
//
// Ported from forge-game/src/main/java/forge/game/ability/effects/CamouflageEffect.java's
// resolve and randomizeBlockers.
type camouflageEffect struct{}

func (camouflageEffect) Resolve(g *Game, a *Ability, c PlayerController) error {
	source := g.Card(a.Source)
	if !subAbilityConditionMet(g, source, a.Amounts, a.Params) {
		return nil
	}
	ev := a.replacing
	if ev == nil || ev.defendingPlayer == NoPlayer {
		return fmt.Errorf("engine: Camouflage: not resolving as a DeclareBlocker replacement")
	}
	declarers, err := targetedOrDefinedPlayers(g, a.Controller, a.Source, a.Params, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Camouflage: %w", err)
	}
	if len(declarers) == 0 {
		return fmt.Errorf("engine: Camouflage: Defined$ names no declarer")
	}
	declarer := declarers[0]
	spec, ok := a.Params.Param("Defender")
	if !ok {
		spec = "You"
	}
	defenders, err := definedPlayers(g, a.Controller, a.Source, spec, a.refs())
	if err != nil {
		return fmt.Errorf("engine: Camouflage: Defender$: %w", err)
	}
	if len(defenders) == 0 {
		return fmt.Errorf("engine: Camouflage: Defender$ %q names no player", spec)
	}
	defender := defenders[0]

	piles, err := camouflagePiles(g, a, c, declarer, defender)
	if err != nil {
		return err
	}
	// combat.getAttackers() is a fresh list: shuffling it leaves the
	// combat's own attacker order alone.
	attackers := append([]CardID(nil), g.combat.Attackers...)
	g.rand.Shuffle(len(attackers), func(i, j int) { attackers[i], attackers[j] = attackers[j], attackers[i] })
	blocks, err := g.randomizeBlockers(c, a.Source, declarer, defender, attackers, piles, ev.blocks)
	if err != nil {
		return err
	}
	ev.blocks = blocks
	return nil
}

// camouflagePiles is CamouflageEffect.java:75-101, the human branch: one
// ChooseCardsForEffect per attacker (every attacker in combat, not only
// defender's), 0 to all of what is left in the pool. A chosen creature
// leaves the pool unless it can block more attackers than it has been piled
// for; nothing in this port grants "can block an additional creature"
// (blockvalidation.go's absent defaults), so every chosen creature leaves.
// Once the pool is empty the remaining piles are empty without asking, the
// only answer the declarer could give.
//
// The pool is defender's creatures (Player.getCreaturesInPlay), less those
// CombatUtil.canBlock(blocker) rejects (canBlockAtAll: tapped, "can't
// block", ...). Java's own filter (:78-80) removes from the list it is
// iterating: ConcurrentModificationException for any such creature except
// one sitting second-to-last, which is removed while the last one goes
// unchecked (forge-java-defects.md). Failing closed per ADR-0035, a pool
// holding any such creature is an error before anything is chosen, so the
// declarer is never offered a creature that cannot block, and Crucible
// never silently plays a game state Java crashes on.
func camouflagePiles(g *Game, a *Ability, c PlayerController, declarer, defender PlayerID) ([][]CardID, error) {
	pool := g.creaturesInPlay(defender)
	for _, id := range pool {
		if !g.canBlockAtAll(id) {
			return nil, fmt.Errorf("engine: Camouflage: defender's creature %d cannot block: "+
				"CamouflageEffect.java:78-80's pool filter throws ConcurrentModificationException here, not resolvable", id)
		}
	}
	piles := make([][]CardID, len(g.combat.Attackers))
	for i := range piles {
		if len(pool) == 0 {
			continue
		}
		chosen := c.ChooseCardsForEffect(g, declarer, a.Source, pool, 0, len(pool))
		if err := checkChoice(chosen, pool, 0, len(pool)); err != nil {
			return nil, fmt.Errorf("engine: Camouflage: pile %d: %w", i+1, err)
		}
		piles[i] = append([]CardID(nil), chosen...)
		pool = withoutCards(pool, chosen)
	}
	return piles, nil
}

// randomizeBlockers is CamouflageEffect.randomizeBlockers
// (CamouflageEffect.java:21-50): attackers is already shuffled, so pile i
// goes to attackers[i]. Each pile first loses every creature
// CombatUtil.canBlock(attacker, blocker, combat) rejects against the combat
// so far (canBlockCombat, blockvalidation.go -- lure and blocker-count
// limits included). A pile left below the attacker's minimum (Menace, Mode$
// MinMaxBlocker Min$) blocks nothing; one above its maximum (Max$) has the
// declarer pick exactly one of it to block, whatever the maximum is, as
// Java does; otherwise every creature in it blocks. Returns blocks with the
// new ones appended.
func (g *Game) randomizeBlockers(c PlayerController, source CardID, declarer, defender PlayerID, attackers []CardID, piles [][]CardID, blocks []Block) ([]Block, error) {
	v, err := g.newBlockCheck(blocks, g.creaturesInPlay(defender))
	if err != nil {
		return nil, fmt.Errorf("engine: Camouflage: %w", err)
	}
	for i, attacker := range attackers {
		var pile []CardID
		for _, b := range piles[i] {
			if v.canBlockCombat(attacker, b) {
				pile = append(pile, b)
			}
		}
		lo, hi := v.minMax[attacker][0], v.minMax[attacker][1]
		if len(pile) < lo {
			continue
		}
		if hi < len(pile) {
			chosen := c.ChooseCardsForEffect(g, declarer, source, pile, 1, 1)
			if err := checkChoice(chosen, pile, 1, 1); err != nil {
				return nil, fmt.Errorf("engine: Camouflage: blocker for attacker %d: %w", attacker, err)
			}
			pile = chosen
		}
		for _, b := range pile {
			v.blocks = append(v.blocks, Block{Blocker: b, Attacker: attacker})
		}
	}
	return v.blocks, nil
}
