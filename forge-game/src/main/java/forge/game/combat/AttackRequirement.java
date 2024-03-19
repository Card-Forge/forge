package forge.game.combat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import forge.game.staticability.StaticAbilityMustAttack;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.util.collect.FCollectionView;
import forge.util.maps.LinkedHashMapToAmount;
import forge.util.maps.MapToAmount;
import forge.util.maps.MapToAmountUtil;

public class AttackRequirement {

    private final MapToAmount<GameEntity> defenderSpecific;
    private final MapToAmount<Card> causesToAttack;
    private final Card attacker;

    public AttackRequirement(final Card attacker, final MapToAmount<Card> causesToAttack, final FCollectionView<GameEntity> possibleDefenders) {
        this.defenderSpecific = new LinkedHashMapToAmount<>();
        this.attacker = attacker;
        this.causesToAttack = causesToAttack;

        final Game game = attacker.getGame();
        int nAttackAnything = 0;

        if (attacker.isGoaded()) {
            // Goad has two requirements but the other is handled by CombatUtil currently
            nAttackAnything += attacker.getGoaded().size();
        }

        //MustAttack static check
        final List<GameEntity> mustAttack = StaticAbilityMustAttack.entitiesMustAttack(attacker);
        nAttackAnything += Collections.frequency(mustAttack, attacker);
        for (GameEntity e : mustAttack) {
            if (e.equals(attacker)) continue;
            defenderSpecific.add(e);
        }

        for (final GameEntity defender : possibleDefenders) {
            // use put here because we want to always put it, even if the value is 0
            defenderSpecific.put(defender, Integer.valueOf(defenderSpecific.count(defender) + nAttackAnything));
        }

        // Remove GameEntities that are no longer on an opposing battlefield or are
        // related to Players who have lost the game
        final List<GameEntity> toRemove = Lists.newArrayListWithCapacity(defenderSpecific.size());
        for (final GameEntity entity : defenderSpecific.keySet()) {
            boolean removeThis = false;
            if (entity instanceof Player) {
                if (!((Player) entity).isInGame()) {
                    removeThis = true;
                }
            } else if (entity instanceof Card) {
                final Card reqPW = (Card) entity;
                final Card gamePW = game.getCardState(reqPW, null);
                if (gamePW == null || !gamePW.getController().isInGame() || !gamePW.equalsWithGameTimestamp(reqPW)
                        || (!gamePW.isBattle() && !gamePW.getController().isOpponentOf(attacker.getController()))) {
                    removeThis = true;
                }
            }
            if (removeThis) {
                toRemove.add(entity);
            }
        }
        defenderSpecific.keySet().removeAll(toRemove);
    }

    public Card getAttacker() {
        return attacker;
    }

    public boolean hasRequirement() {
        return defenderSpecific.countAll() > 0 || causesToAttack.countAll() > 0;
    }

    public final MapToAmount<Card> getCausesToAttack() {
        return causesToAttack;
    }

    public int countViolations(final GameEntity defender, final Map<Card, GameEntity> attackers) {
        if (!hasRequirement()) {
            return 0;
        }

        final boolean isAttacking = defender != null;
        int violations = defenderSpecific.countAll() - (isAttacking ? defenderSpecific.count(defender) : 0);
        if (isAttacking) {
            final Combat combat = defender.getGame().getCombat();
            final Map<Card, AttackRestriction> constraints = combat.getAttackConstraints().getRestrictions();

            // check if a restriction will apply such that the requirement is no longer relevant
            if (attackers.size() != 1 || !constraints.get(attackers.entrySet().iterator().next().getKey()).getTypes().contains(AttackRestrictionType.ONLY_ALONE)) {
                for (final Map.Entry<Card, Integer> mustAttack : causesToAttack.entrySet()) {
                    if (constraints.get(mustAttack.getKey()).getTypes().contains(AttackRestrictionType.ONLY_ALONE)) continue;
                    int max = GlobalAttackRestrictions.getGlobalRestrictions(mustAttack.getKey().getController(), combat.getDefenders()).getMax();
                    if (max == -1) max = Integer.MAX_VALUE;

                    // only count violations if the forced creature can actually attack and has no cost incurred for doing so
                    if (attackers.size() < max && !attackers.containsKey(mustAttack.getKey()) && CombatUtil.canAttack(mustAttack.getKey()) && CombatUtil.getAttackCost(defender.getGame(), mustAttack.getKey(), defender) == null) {
                        violations += mustAttack.getValue().intValue();
                    }
                }
            }
        }
        return violations;
    }

    public List<Pair<GameEntity, Integer>> getSortedRequirements() {
        return MapToAmountUtil.sort(defenderSpecific);
    }
    public static final Function<AttackRequirement, List<Pair<GameEntity, Integer>>> SORT = new Function<AttackRequirement, List<Pair<GameEntity,Integer>>>() {
        @Override
        public List<Pair<GameEntity,Integer>> apply(final AttackRequirement input) {
            return input.getSortedRequirements();
        }
    };

}
