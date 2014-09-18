package forge.view;

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Representation of a {@link forge.game.combat.Combat}, containing only the
 * information relevant to a user interface.
 * 
 * Conversion from and to Combat happens through {@link LocalGameView}.
 * 
 * @author elcnesh
 */
public class CombatView {

    private Map<CardView, GameEntityView> attackersWithDefenders = Maps.newHashMap();
    private Map<CardView, Iterable<CardView>> attackersWithBlockers = Maps.newHashMap();
    private Map<Iterable<CardView>, GameEntityView> bandsWithDefenders = Maps.newHashMap();
    private Map<Iterable<CardView>, Iterable<CardView>> bandsWithBlockers = Maps.newHashMap();

    public CombatView() {
    }

    public int getNumAttackers() {
        return attackersWithDefenders.size();
    }

    public boolean isAttacking(final CardView card) {
        return attackersWithDefenders.containsKey(card);
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

    public boolean isBlocking(final CardView card) {
        for (final Iterable<CardView> blockers : attackersWithBlockers.values()) {
            if (blockers == null) {
                continue;
            }
            if (Iterables.contains(blockers, card)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param attacker
     * @return the blockers associated with an attacker, or {@code null} if the
     *         attacker is unblocked.
     */
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
        final List<CardView> attackingBandCopy = Lists.newArrayList(attackingBand),
                blockersCopy;
        if (blockers == null) {
            blockersCopy = null;
        } else {
            blockersCopy = Lists.newArrayList(blockers);
        }

        for (final CardView attacker : attackingBandCopy) {
            this.attackersWithDefenders.put(attacker, defender);
            this.attackersWithBlockers.put(attacker, blockersCopy);
        }
        this.bandsWithDefenders.put(attackingBandCopy, defender);
        this.bandsWithBlockers.put(attackingBandCopy, blockersCopy);
    }

}
