package forge.game.combat;

import java.util.*;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;

public class AttackRestriction {

    private final Card attacker;
    // the keyword causing each restriction
    private final Map<AttackRestrictionType, String> restrictions = new EnumMap<>(AttackRestrictionType.class);
    private boolean cantAttack;
    private final FCollectionView<GameEntity> cantAttackDefender;

    public AttackRestriction(final Card attacker, final FCollectionView<GameEntity> possibleDefenders) {
        this.attacker = attacker;
        setRestrictions();

        final FCollection<GameEntity> cantAttackDefender = new FCollection<>();
        for (final GameEntity defender : possibleDefenders) {
            if (!CombatUtil.canAttack(attacker, defender)) {
                cantAttackDefender.add(defender);
            }
        }
        this.cantAttackDefender = cantAttackDefender;

        if ((restrictions.containsKey(AttackRestrictionType.ONLY_ALONE) && (
                restrictions.containsKey(AttackRestrictionType.NEED_GREATER_POWER) ||
                restrictions.containsKey(AttackRestrictionType.NEED_BLACK_OR_GREEN) ||
                restrictions.containsKey(AttackRestrictionType.NOT_ALONE) ||
                restrictions.containsKey(AttackRestrictionType.NEED_TWO_OTHERS))
                ) || (
                        restrictions.containsKey(AttackRestrictionType.NEVER)
                ) || (
                        cantAttackDefender.size() == possibleDefenders.size())) {
            cantAttack = true;
        }
    }

    public boolean canAttack(final GameEntity defender) {
        return !cantAttack && !cantAttackDefender.contains(defender);
    }

    public Set<AttackRestrictionType> getViolation(final Map<Card, GameEntity> attackers) {
        final Set<AttackRestrictionType> violations = EnumSet.noneOf(AttackRestrictionType.class);
        final int nAttackers = attackers.size();
        if (restrictions.containsKey(AttackRestrictionType.ONLY_ALONE) && nAttackers > 1) {
            violations.add(AttackRestrictionType.ONLY_ALONE);
        }
        if (restrictions.containsKey(AttackRestrictionType.NEED_GREATER_POWER)
                && attackers.keySet().stream().noneMatch(AttackRestrictionType.NEED_GREATER_POWER.getPredicate(attacker))) {
            violations.add(AttackRestrictionType.NEED_GREATER_POWER);
        }
        if (restrictions.containsKey(AttackRestrictionType.NEED_BLACK_OR_GREEN)
                && attackers.keySet().stream().noneMatch(AttackRestrictionType.NEED_BLACK_OR_GREEN.getPredicate(attacker))) {
            violations.add(AttackRestrictionType.NEED_BLACK_OR_GREEN);
        }
        if (restrictions.containsKey(AttackRestrictionType.NOT_ALONE) && nAttackers <= 1) {
            violations.add(AttackRestrictionType.NOT_ALONE);
        }
        if (restrictions.containsKey(AttackRestrictionType.NEED_TWO_OTHERS) && nAttackers <= 2) {
            violations.add(AttackRestrictionType.NEED_TWO_OTHERS);
        }
        return violations;
    }

    public boolean canAttack(final GameEntity defender, final Map<Card, GameEntity> attackers) {
        if (!canAttack(defender)) {
            return false;
        }

        return getViolation(attackers).isEmpty();
    }

    public Set<AttackRestrictionType> getTypes() {
        return Collections.unmodifiableSet(restrictions.keySet());
    }

    /**
     * @return the keyword text causing the restriction, or null if the attacker doesn't have it
     */
    public String getKeyword(final AttackRestrictionType type) {
        return restrictions.get(type);
    }

    private void setRestrictions() {
        addRestriction(AttackRestrictionType.ONLY_ALONE, "CARDNAME can only attack alone.");
        addRestriction(AttackRestrictionType.NEED_GREATER_POWER, "CARDNAME can't attack unless a creature with greater power also attacks.");
        addRestriction(AttackRestrictionType.NEED_BLACK_OR_GREEN, "CARDNAME can't attack unless a black or green creature also attacks.");
        addRestriction(AttackRestrictionType.NOT_ALONE, "CARDNAME can't attack or block alone.", "CARDNAME can't attack alone.");
        addRestriction(AttackRestrictionType.NEED_TWO_OTHERS, "CARDNAME can't attack unless at least two other creatures attack.");
    }

    private void addRestriction(final AttackRestrictionType type, final String... keywords) {
        for (final String keyword : keywords) {
            if (attacker.hasKeyword(keyword)) {
                restrictions.put(type, keyword);
                return;
            }
        }
    }

}
