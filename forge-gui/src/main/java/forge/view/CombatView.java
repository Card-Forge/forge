package forge.view;

import java.util.Map;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class CombatView {

    private Map<CardView, GameEntityView> attackersWithDefenders;
    private Map<CardView, Iterable<CardView>> attackersWithBlockers;
    private Map<Iterable<CardView>, GameEntityView> bandsWithDefenders;
    private Map<Iterable<CardView>, Iterable<CardView>> bandsWithBlockers;

    public CombatView() {
    }

    public int getNumAttackers() {
        return attackersWithDefenders.size();
    }

    public Iterable<CardView> getAttackers() {
        return attackersWithDefenders.keySet();
    }

    public Iterable<GameEntityView> getDefenders() {
        return Sets.newHashSet(attackersWithDefenders.values());
    }

    public GameEntityView getDefender(final CardView attacker) {
        return attackersWithDefenders.get(attacker);
    }

    public Iterable<CardView> getBlockers(final CardView attacker) {
        return attackersWithBlockers.get(attacker);
    }

    /**
     * Get an {@link Iterable} of the blockers of the specified band, or
     * {@code null} if that band is unblocked.
     * 
     * @param attackingBand
     *            an {@link Iterable} representing an attacking band.
     * @return an {@link Iterable} of {@link CardView} objects, or {@code null}.
     */
    public Iterable<CardView> getBlockers(final Iterable<CardView> attackingBand) {
        return bandsWithBlockers.get(attackingBand);
    }

    public Iterable<CardView> getAttackersOf(final GameEntityView defender) {
        return Maps.filterValues(attackersWithDefenders, Predicates.equalTo(defender)).keySet();
    }
    public Iterable<Iterable<CardView>> getAttackingBandsOf(final GameEntityView defender) {
        return Maps.filterValues(bandsWithDefenders, Predicates.equalTo(defender)).keySet();
    }

    public void addAttackingBand(final Iterable<CardView> attackingBand, final GameEntityView defender, final Iterable<CardView> blockers) {
        for (final CardView attacker : attackingBand) {
            this.attackersWithDefenders.put(attacker, defender);
            this.attackersWithBlockers.put(attacker, blockers);
        }
        this.bandsWithDefenders.put(attackingBand, defender);
        this.bandsWithBlockers.put(attackingBand, blockers);
    }

    public void reset() {
        this.attackersWithDefenders = Maps.newHashMap();
        this.attackersWithBlockers = Maps.newHashMap();
        this.bandsWithDefenders = Maps.newHashMap();
        this.bandsWithBlockers = Maps.newHashMap();
    }
}
