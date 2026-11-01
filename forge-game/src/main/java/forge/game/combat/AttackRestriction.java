package forge.game.combat;

import java.util.*;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityAttackBlockRestrict;
import forge.game.staticability.StaticAbilityMode;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;

public class AttackRestriction {

    private final Card attacker;
    private boolean cantAttack;
    private final FCollectionView<GameEntity> cantAttackDefender;
    private final List<StaticAbility> othersRestrictions;

    public AttackRestriction(final Card attacker, final FCollectionView<GameEntity> possibleDefenders) {
        this.attacker = attacker;

        final FCollection<GameEntity> cantAttackDefender = new FCollection<>();
        for (final GameEntity defender : possibleDefenders) {
            if (!CombatUtil.canAttack(attacker, defender)) {
                cantAttackDefender.add(defender);
            }
        }
        this.cantAttackDefender = cantAttackDefender;

        if (cantAttackDefender.size() == possibleDefenders.size()) {
            cantAttack = true;
        }
        othersRestrictions = cantAttack ? Collections.emptyList()
                : StaticAbilityAttackBlockRestrict.getRestrictions(StaticAbilityMode.AttackRestrict, attacker);
    }

    /**
     * Restrictions that depend on the other attackers.
     */
    public List<StaticAbility> getOthersRestrictions() {
        return othersRestrictions;
    }

    public boolean isRestrictedByOthers() {
        return !othersRestrictions.isEmpty();
    }

    public boolean canAttack(final GameEntity defender) {
        return !cantAttack && !cantAttackDefender.contains(defender);
    }

    public List<StaticAbility> getStaticViolations(final Map<Card, GameEntity> attackers) {
        if (othersRestrictions.isEmpty()) {
            return Collections.emptyList();
        }
        final List<StaticAbility> violations = new ArrayList<>(othersRestrictions);
        violations.removeIf(stAb -> !StaticAbilityAttackBlockRestrict.isViolated(stAb, attacker, attackers.keySet()));
        return violations;
    }

    public boolean canAttack(final GameEntity defender, final Map<Card, GameEntity> attackers) {
        if (!canAttack(defender)) {
            return false;
        }

        return getStaticViolations(attackers).isEmpty();
    }
}
