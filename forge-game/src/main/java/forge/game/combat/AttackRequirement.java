package forge.game.combat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;
import forge.util.maps.LinkedHashMapToAmount;
import forge.util.maps.MapToAmount;
import forge.util.maps.MapToAmountUtil;

public class AttackRequirement {

    private final MapToAmount<GameEntity> defenderSpecific;
    private final MapToAmount<GameEntity> defenderOrPWSpecific;
    private final Map<GameEntity, List<GameEntity>> defenderSpecificAlternatives;
    private final MapToAmount<Card> causesToAttack;

    public AttackRequirement(final Card attacker, final MapToAmount<Card> causesToAttack, final FCollectionView<GameEntity> possibleDefenders) {
        this.defenderSpecific = new LinkedHashMapToAmount<>();
        this.defenderOrPWSpecific = new LinkedHashMapToAmount<>();
        this.defenderSpecificAlternatives = new HashMap<>();

        this.causesToAttack = causesToAttack;

        final GameEntity mustAttack = attacker.getController().getMustAttackEntity();
        if (mustAttack != null) {
            defenderSpecific.add(mustAttack);
        }
        final GameEntity mustAttackThisTurn = attacker.getController().getMustAttackEntityThisTurn();
        if (mustAttackThisTurn != null) {
            defenderSpecific.add(mustAttackThisTurn);
        }

        int nAttackAnything = 0;

        if (attacker.isGoaded()) {
            nAttackAnything += attacker.getGoaded().size();
        }

        // remove it when all of them are HIDDEN or static
        for (final KeywordInterface inst : attacker.getKeywords()) {
            final String keyword = inst.getOriginal();
            if (keyword.startsWith("CARDNAME attacks specific player each combat if able")) {
                final String defined = keyword.split(":")[1];
                final GameEntity mustAttack2 = AbilityUtils.getDefinedPlayers(attacker, defined, null).get(0);
                defenderSpecific.add(mustAttack2);
            } else if (keyword.equals("CARDNAME attacks each combat if able.")) {
                nAttackAnything++;
            }
        }
        for (final String keyword : attacker.getHiddenExtrinsicKeywords()) {
            if (keyword.startsWith("CARDNAME attacks specific player each combat if able")) {
                final String defined = keyword.split(":")[1];
                final GameEntity mustAttack2 = AbilityUtils.getDefinedPlayers(attacker, defined, null).get(0);
                defenderSpecific.add(mustAttack2);
            } else if (keyword.equals("CARDNAME attacks each combat if able.")) {
                nAttackAnything++;
            }
        }
        
        final GameEntity mustAttack3 = attacker.getMustAttackEntity();
        if (mustAttack3 != null) {
            defenderSpecific.add(mustAttack3);
        }
        final GameEntity mustAttackThisTurn3 = attacker.getMustAttackEntityThisTurn();
        if (mustAttackThisTurn3 != null) {
            defenderSpecific.add(mustAttackThisTurn3);
        }

        final Game game = attacker.getGame();

        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if (c.hasKeyword("Each opponent must attack you or a planeswalker you control with at least one creature each combat if able.")) {
                if (attacker.getController().isOpponentOf(c.getController()) && !defenderOrPWSpecific.containsKey(c.getController())) {
                    defenderOrPWSpecific.put(c.getController(), 1);
                    for (Card pw : c.getController().getPlaneswalkersInPlay()) {
                        // Add the attack alternatives that suffice (planeswalkers that can be attacked instead of the player)
                        if (!defenderSpecificAlternatives.containsKey(c.getController())) {
                            defenderSpecificAlternatives.put(c.getController(), Lists.newArrayList());
                        }
                        defenderSpecificAlternatives.get(c.getController()).add(pw);
                    }
                }
            }
        }

        for (final GameEntity defender : possibleDefenders) {
            if (CombatUtil.getAttackCost(game, attacker, defender) == null) {
                // use put here because we want to always put it, even if the value is 0
                defenderSpecific.put(defender, Integer.valueOf(defenderSpecific.count(defender) + nAttackAnything));
                if (defenderOrPWSpecific.containsKey(defender)) {
                    defenderOrPWSpecific.put(defender, Integer.valueOf(defenderOrPWSpecific.count(defender) + nAttackAnything));
                }
            } else {
                defenderSpecific.remove(defender);
                defenderOrPWSpecific.remove(defender);
            }
        }

        // Remove GameEntities that are no longer on the battlefield or are
        // related to Players who have lost the game
        final MapToAmount<GameEntity> combinedDefMap = new LinkedHashMapToAmount<>();
        combinedDefMap.putAll(defenderSpecific);
        combinedDefMap.putAll(defenderOrPWSpecific);

        final List<GameEntity> toRemove = Lists.newArrayListWithCapacity(combinedDefMap.size());
        for (final GameEntity entity : combinedDefMap.keySet()) {
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
            defenderOrPWSpecific.remove(entity);
        }
    }

    public boolean hasRequirement() {
        return !defenderSpecific.isEmpty() || !causesToAttack.isEmpty() || !defenderOrPWSpecific.isEmpty();
    }

    public final MapToAmount<Card> getCausesToAttack() {
        return causesToAttack;
    }

    public int countViolations(final GameEntity defender, final Map<Card, GameEntity> attackers) {
        if (!hasRequirement()) {
            return 0;
        }

        final boolean isAttacking = defender != null;
        int violations = 0;

        // first. check to see if "must attack X or Y with at least one creature" requirements are satisfied
        //List<GameEntity> toRemoveFromDefSpecific = Lists.newArrayList();
        if (!defenderOrPWSpecific.isEmpty()) {
            for (GameEntity def : defenderOrPWSpecific.keySet()) {
                if (defenderSpecificAlternatives.containsKey(def)) {
                    boolean isAttackingDefender = false;
                    outer: for (Card atk : attackers.keySet()) {
                        // is anyone attacking this defender or any of the alternative defenders?
                        if (attackers.get(atk).equals(def)) {
                            isAttackingDefender = true;
                            break;
                        }
                        for (GameEntity altDef : defenderSpecificAlternatives.get(def)) {
                            if (attackers.get(atk).equals(altDef)) {
                                isAttackingDefender = true;
                                break outer;
                            }
                        }
                    }
                    if (!isAttackingDefender) {
                        violations++; // no one is attacking that defender or any of his PWs
                    }
                }
            }
        }

        // now, count everything else
        violations += defenderSpecific.countAll() - (isAttacking ? (defenderSpecific.count(defender)) : 0);
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
        result.addAll(MapToAmountUtil.sort(defenderOrPWSpecific));

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
