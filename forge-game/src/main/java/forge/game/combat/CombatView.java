package forge.game.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty.CombatProp;


public class CombatView extends TrackableObject<CombatProp> {
    public CombatView() {
        super(-1, CombatProp.class); //ID not needed
    }
    private Map<CardView, GameEntityView<?>> getAttackersWithDefenders() {
        return get(CombatProp.AttackersWithDefenders);
    }
    private Map<CardView, Iterable<CardView>> getAttackersWithBlockers() {
        return get(CombatProp.AttackersWithBlockers);
    }
    private Map<Iterable<CardView>, GameEntityView<?>> getBandsWithDefenders() {
        return get(CombatProp.BandsWithDefenders);
    }
    private Map<Iterable<CardView>, Iterable<CardView>> getBandsWithBlockers() {
        return get(CombatProp.BandsWithBlockers);
    }
    private Map<CardView, Iterable<CardView>> getAttackersWithPlannedBlockers() {
        return get(CombatProp.AttackersWithPlannedBlockers);
    }
    private Map<Iterable<CardView>, Iterable<CardView>> getBandsWithPlannedBlockers() {
        return get(CombatProp.BandsWithPlannedBlockers);
    }

    public int getNumAttackers() {
        return getAttackersWithDefenders().size();
    }

    public boolean isAttacking(final CardView card) {
        return getAttackersWithDefenders().containsKey(card);
    }

    public Iterable<CardView> getAttackers() {
        return getAttackersWithDefenders().keySet();
    }

    public Iterable<GameEntityView<?>> getDefenders() {
        return Sets.newHashSet(getAttackersWithDefenders().values());
    }

    public GameEntityView<?> getDefender(final CardView attacker) {
        return getAttackersWithDefenders().get(attacker);
    }

    public boolean isBlocking(final CardView card) {
        for (final Iterable<CardView> blockers : getAttackersWithBlockers().values()) {
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
        return getAttackersWithBlockers().get(attacker);
    }

    /**
     * @param attacker
     * @return the blockers associated with an attacker, or {@code null} if the
     *         attacker is unblocked (planning stage, for targeting overlay).
     */
    public Iterable<CardView> getPlannedBlockers(final CardView attacker) {
        return getAttackersWithPlannedBlockers().get(attacker);
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
        return getBandsWithBlockers().get(attackingBand);
    }

    /**
     * Get an {@link Iterable} of the blockers of the specified band, or
     * {@code null} if that band is unblocked (planning stage, for targeting overlay).
     * 
     * @param attackingBand
     *            an {@link Iterable} representing an attacking band.
     * @return an {@link Iterable} of {@link CardView} objects, or {@code null}.
     */
    public Iterable<CardView> getPlannedBlockers(final Iterable<CardView> attackingBand) {
        return getBandsWithPlannedBlockers().get(attackingBand);
    }

    public Iterable<CardView> getAttackersOf(final GameEntityView<?> defender) {
        ArrayList<CardView> views = new ArrayList<CardView>();
        for (Entry<CardView, GameEntityView<?>> entry : getAttackersWithDefenders().entrySet()) {
            if (entry.getValue().equals(defender)) {
                views.add(entry.getKey());
            }
        }
        return views;
    }
    public Iterable<Iterable<CardView>> getAttackingBandsOf(final GameEntityView<?> defender) {
        ArrayList<Iterable<CardView>> views = new ArrayList<Iterable<CardView>>();
        for (Entry<Iterable<CardView>, GameEntityView<?>> entry : getBandsWithDefenders().entrySet()) {
            if (entry.getValue().equals(defender)) {
                views.add(entry.getKey());
            }
        }
        return views;
    }

    public void addAttackingBand(final Iterable<CardView> attackingBand, final GameEntityView<?> defender, final Iterable<CardView> blockers, final Iterable<CardView> plannedBlockers) {
        final List<CardView> attackingBandCopy = Lists.newArrayList(attackingBand),
                blockersCopy, plannedBlockersCopy;

        blockersCopy = blockers == null ? null : Lists.newArrayList(blockers);
        plannedBlockersCopy = plannedBlockers == null ? null : Lists.newArrayList(plannedBlockers);

        for (final CardView attacker : attackingBandCopy) {
            this.getAttackersWithDefenders().put(attacker, defender);
            this.getAttackersWithBlockers().put(attacker, blockersCopy);
            this.getAttackersWithPlannedBlockers().put(attacker, plannedBlockersCopy);
        }
        this.getBandsWithDefenders().put(attackingBandCopy, defender);
        this.getBandsWithBlockers().put(attackingBandCopy, blockersCopy);
        this.getBandsWithPlannedBlockers().put(attackingBandCopy, plannedBlockersCopy);
    }
}
