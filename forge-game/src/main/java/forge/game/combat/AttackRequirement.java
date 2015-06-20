package forge.game.combat;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;
import forge.util.maps.MapToAmountUtil;
import forge.util.maps.LinkedHashMapToAmount;
import forge.util.maps.MapToAmount;

public class AttackRequirement {

    private final MapToAmount<GameEntity> defenderSpecific;
    private final MapToAmount<Card> causesToAttack;

    public AttackRequirement(final Card attacker, final MapToAmount<Card> causesToAttack, final FCollectionView<GameEntity> possibleDefenders) {
        this.defenderSpecific = new LinkedHashMapToAmount<GameEntity>();
        this.causesToAttack = causesToAttack;

        final GameEntity mustAttack = attacker.getController().getMustAttackEntity();
        if (mustAttack != null) {
            defenderSpecific.add(mustAttack);
        }

        int nAttackAnything = 0;
        for (final String keyword : attacker.getKeywords()) {
            if (keyword.startsWith("CARDNAME attacks specific player each combat if able")) {
                final String defined = keyword.split(":")[1];
                final GameEntity mustAttack2 = AbilityUtils.getDefinedPlayers(attacker, defined, null).get(0);
                defenderSpecific.add(mustAttack2);
            } else if (keyword.equals("CARDNAME attacks each combat if able.") || 
                    (keyword.equals("CARDNAME attacks each turn if able.")
                            && !attacker.getDamageHistory().getCreatureAttackedThisTurn())) {
                nAttackAnything++;
            }
        }
        final GameEntity mustAttack3 = attacker.getMustAttackEntity();
        if (mustAttack3 != null) {
            defenderSpecific.add(mustAttack3);
        }

        final Game game = attacker.getGame();
        for (final GameEntity defender : possibleDefenders) {
            if (CombatUtil.getAttackCost(game, attacker, defender) == null) {
                // use put here because we want to always put it, even if the value is 0
                defenderSpecific.put(defender, Integer.valueOf(defenderSpecific.count(defender) + nAttackAnything));
            } else {
                defenderSpecific.remove(defender);
            }
        }

        // Remove GameEntities that are no longer on the battlefield or are
        // related to Players who have lost the game
        final List<GameEntity> toRemove = Lists.newArrayListWithCapacity(defenderSpecific.size());
        for (final GameEntity entity : defenderSpecific.keySet()) {
            boolean removeThis = false;
            if (entity instanceof Player) {
                if (((Player) entity).hasLost()) {
                    removeThis = true;
                }
            } else if (entity instanceof Card) {
                final Player controller = ((Card) entity).getController();
                if (controller.hasLost() || !controller.getCardsIn(ZoneType.Battlefield).contains(entity)) {
                    removeThis = true;
                }
            }
            if (removeThis) {
                toRemove.add(entity);
            }
        }
        for (final GameEntity entity : toRemove) {
            defenderSpecific.remove(entity);
        }
    }

    public boolean hasRequirement() {
        return !defenderSpecific.isEmpty() || !causesToAttack.isEmpty();
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
            for (final Map.Entry<Card, Integer> mustAttack : causesToAttack.entrySet()) {
                // only count violations if the forced creature can actually attack and has no cost incurred for doing so
                if (CombatUtil.canAttack(mustAttack.getKey()) && !attackers.containsKey(mustAttack.getKey()) && CombatUtil.getAttackCost(defender.getGame(), mustAttack.getKey(), defender) == null) {
                    violations += mustAttack.getValue().intValue();
                }
            }
        }
        return violations;
    }

    public List<Pair<GameEntity, Integer>> getSortedRequirements() {
        final List<Pair<GameEntity, Integer>> result = Lists.newArrayListWithExpectedSize(defenderSpecific.size());
        result.addAll(MapToAmountUtil.sort(defenderSpecific));

        for (int i = 0; i < result.size(); i++) {
            final Pair<GameEntity, Integer> def = result.get(i);
            result.set(i, Pair.of(def.getLeft(), def.getRight()));
        }

        return result;
    }
    public static final Function<AttackRequirement, List<Pair<GameEntity, Integer>>> SORT = new Function<AttackRequirement, List<Pair<GameEntity,Integer>>>() {
        @Override
        public List<Pair<GameEntity,Integer>> apply(final AttackRequirement input) {
            return input.getSortedRequirements();
        }
    };

}
