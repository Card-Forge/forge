package forge.view;

import java.util.Map;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

public class CombatView {

    private Map<CardView, GameEntityView> attackersWithDefenders;
    private Map<CardView, Iterable<CardView>> attackersWithBlockers;

    public CombatView() {
    }

    public Iterable<CardView> getAttackers() {
        return attackersWithDefenders.keySet();
    }

    public GameEntityView getDefender(final CardView attacker) {
        return attackersWithDefenders.get(attacker);
    }

    public Iterable<CardView> getBlockers(final CardView attacker) {
        return attackersWithBlockers.get(attacker);
    }

    public Iterable<CardView> getAttackersOf(final GameEntityView defender) {
        return Maps.filterValues(attackersWithDefenders, Predicates.equalTo(defender)).keySet();
    }

    public void addAttacker(final CardView attacker, final GameEntityView defender, final Iterable<CardView> blockers) {
        this.attackersWithDefenders.put(attacker, defender);
        this.attackersWithBlockers.put(attacker, blockers);
    }

}
