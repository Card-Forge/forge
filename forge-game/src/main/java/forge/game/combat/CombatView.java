package forge.game.combat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Iterables;

import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.util.FCollection;


public class CombatView extends TrackableObject {
    public CombatView() {
        super(-1); //ID not needed
        set(TrackableProperty.AttackersWithDefenders, new HashMap<CardView, GameEntityView>());
        set(TrackableProperty.AttackersWithBlockers, new HashMap<CardView, FCollection<CardView>>());
        set(TrackableProperty.BandsWithDefenders, new HashMap<FCollection<CardView>, GameEntityView>());
        set(TrackableProperty.BandsWithBlockers, new HashMap<FCollection<CardView>, FCollection<CardView>>());
        set(TrackableProperty.AttackersWithPlannedBlockers, new HashMap<CardView, FCollection<CardView>>());
        set(TrackableProperty.BandsWithPlannedBlockers, new HashMap<FCollection<CardView>, FCollection<CardView>>());
    }
    private Map<CardView, GameEntityView> getAttackersWithDefenders() {
        return get(TrackableProperty.AttackersWithDefenders);
    }
    private Map<CardView, FCollection<CardView>> getAttackersWithBlockers() {
        return get(TrackableProperty.AttackersWithBlockers);
    }
    private Map<FCollection<CardView>, GameEntityView> getBandsWithDefenders() {
        return get(TrackableProperty.BandsWithDefenders);
    }
    private Map<FCollection<CardView>, FCollection<CardView>> getBandsWithBlockers() {
        return get(TrackableProperty.BandsWithBlockers);
    }
    private Map<CardView, FCollection<CardView>> getAttackersWithPlannedBlockers() {
        return get(TrackableProperty.AttackersWithPlannedBlockers);
    }
    private Map<FCollection<CardView>, FCollection<CardView>> getBandsWithPlannedBlockers() {
        return get(TrackableProperty.BandsWithPlannedBlockers);
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

    public Iterable<GameEntityView> getDefenders() {
        return new HashSet<GameEntityView>(getAttackersWithDefenders().values());
    }

    public GameEntityView getDefender(final CardView attacker) {
        return getAttackersWithDefenders().get(attacker);
    }

    public boolean isBlocking(final CardView card) {
        for (final FCollection<CardView> blockers : getAttackersWithBlockers().values()) {
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
    public FCollection<CardView> getBlockers(final CardView attacker) {
        return getAttackersWithBlockers().get(attacker);
    }

    /**
     * @param attacker
     * @return the blockers associated with an attacker, or {@code null} if the
     *         attacker is unblocked (planning stage, for targeting overlay).
     */
    public FCollection<CardView> getPlannedBlockers(final CardView attacker) {
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
    public FCollection<CardView> getBlockers(final FCollection<CardView> attackingBand) {
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
    public FCollection<CardView> getPlannedBlockers(final FCollection<CardView> attackingBand) {
        return getBandsWithPlannedBlockers().get(attackingBand);
    }

    public FCollection<CardView> getAttackersOf(final GameEntityView defender) {
        FCollection<CardView> views = new FCollection<CardView>();
        for (Entry<CardView, GameEntityView> entry : getAttackersWithDefenders().entrySet()) {
            if (entry.getValue().equals(defender)) {
                views.add(entry.getKey());
            }
        }
        return views;
    }
    public Iterable<FCollection<CardView>> getAttackingBandsOf(final GameEntityView defender) {
        ArrayList<FCollection<CardView>> views = new ArrayList<FCollection<CardView>>();
        for (Entry<FCollection<CardView>, GameEntityView> entry : getBandsWithDefenders().entrySet()) {
            if (entry.getValue().equals(defender)) {
                views.add(entry.getKey());
            }
        }
        return views;
    }

    public void addAttackingBand(final Iterable<CardView> attackingBand, final GameEntityView defender, final Iterable<CardView> blockers, final Iterable<CardView> plannedBlockers) {
        final FCollection<CardView> attackingBandCopy = new FCollection<CardView>();
        final FCollection<CardView> blockersCopy = new FCollection<CardView>();
        final FCollection<CardView> plannedBlockersCopy = new FCollection<CardView>();

        attackingBandCopy.addAll(attackingBand);
        if (blockers != null) {
            blockersCopy.addAll(blockers);
        }
        if (plannedBlockers != null) {
            plannedBlockersCopy.addAll(plannedBlockers);
        }

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
