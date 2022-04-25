package forge.game.combat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.game.staticability.StaticAbilityMustAttack;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardLists;
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
    private final Card attacker;

    public AttackRequirement(final Card attacker, final MapToAmount<Card> causesToAttack, final FCollectionView<GameEntity> possibleDefenders) {
        this.defenderSpecific = new LinkedHashMapToAmount<>();
        this.defenderOrPWSpecific = new LinkedHashMapToAmount<>();
        this.defenderSpecificAlternatives = new HashMap<>();
        this.attacker = attacker;

        this.causesToAttack = causesToAttack;

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
            // use put here because we want to always put it, even if the value is 0
            defenderSpecific.put(defender, Integer.valueOf(defenderSpecific.count(defender) + nAttackAnything));
            if (defenderOrPWSpecific.containsKey(defender)) {
                defenderOrPWSpecific.put(defender, Integer.valueOf(defenderOrPWSpecific.count(defender) + nAttackAnything));
            }
        }

        // Remove GameEntities that are no longer on an opposing battlefield or are
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
                final Card reqPW = (Card) entity;
                final List<Card> actualPW = CardLists.getValidCards(attacker.getController().getOpponents().getCardsIn(ZoneType.Battlefield), "Card.StrictlySelf", null, reqPW, null);
                if (reqPW.getController().hasLost() || actualPW.isEmpty()) {
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
                    if (!isAttackingDefender && CombatUtil.getAttackCost(attacker.getGame(), attacker, def) == null) {
                        violations++; // no one is attacking that defender or any of his PWs
                    }
                }
            }
        }

        // now, count everything else
        violations += defenderSpecific.countAll() - (isAttacking ? defenderSpecific.count(defender) : 0);
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
