/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.mana;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;

import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.cost.CostPayment;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventManaPool;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.replacement.ReplacementLayer;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityUnspentMana;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>
 * ManaPool class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class ManaPool extends ManaConversionMatrix implements Iterable<Mana> {
    private final Player owner;

    private final Multiset<Mana> floatingMana = HashMultiset.create();

    public ManaPool(final Player player) {
        owner = player;
        restoreColorReplacements();
    }

    public final int getAmountOfColor(final byte color) {
        return Multisets.filter(this.floatingMana, m -> m.getColor() == color).size();
    }

    public void addManaNoEvent(final Mana mana) {
        this.floatingMana.add(mana);
    }

    public final void addMana(final Mana... manaList) {
        addMana(Arrays.asList(manaList));
    }
    public final void addMana(final Iterable<Mana> manaList) {
        Set<MagicColor.Color> colors;

        if (manaList instanceof Multiset<Mana> manaSet) {
            colors = manaSet.elementSet().stream().map(m -> MagicColor.Color.fromByte(m.getColor())).collect(Collectors.toSet());
            floatingMana.addAll(manaSet);
        } else {
            colors = EnumSet.noneOf(MagicColor.Color.class);
            for (final Mana m : manaList) {
                floatingMana.add(m);
                colors.add(MagicColor.Color.fromByte(m.getColor()));
            }
        }
        owner.updateManaForView();
        owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Added, colors));
    }

    /**
     * <p>
     * willManaBeLostAtEndOfPhase.
     *
     * @return - whether floating mana will be lost if the current phase ended right now
     * </p>
     */
    public final boolean willManaBeLostAtEndOfPhase() {
        if (floatingMana.isEmpty()) {
            return false;
        }

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromAffected(owner);
        if (!owner.getGame().getReplacementHandler().getReplacementList(ReplacementType.LoseMana, runParams, ReplacementLayer.Other).isEmpty()) {
            return false;
        }

        Collection<Byte> safeColors = StaticAbilityUnspentMana.getManaToKeep(owner);
        int safeMana = Multisets.filter(this.floatingMana, m -> safeColors.contains(m.getColor())).size();

        // TODO isPersistentMana

        return totalMana() != safeMana; //won't lose floating mana if all mana is of colors that aren't going to be emptied
    }

    public final boolean hasBurn() {
        final Game game = owner.getGame();
        return game.getRules().hasManaBurn() || StaticAbilityUnspentMana.hasManaBurn(owner);
    }

    public final void resetPool() {
        // This should only be used to reset the pool to empty by things like restores.
        floatingMana.clear();
    }

    public final List<Mana> clearPool(boolean isEndOfPhase) {
        // isEndOfPhase parameter: true = end of phase, false = mana drain effect
        List<Mana> cleared = Lists.newArrayList();
        if (floatingMana.isEmpty()) { return cleared; }

        Byte convertTo = null;

        // TODO move this lower in case all mana would be persistent
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromAffected(owner);
        runParams.put(AbilityKey.Mana, "C");
        switch (owner.getGame().getReplacementHandler().run(ReplacementType.LoseMana, runParams)) {
        case NotReplaced:
            break;
        case Skipped:
            return cleared;
        default:
            convertTo = ManaAtom.fromName((String) runParams.get(AbilityKey.Mana));
            break;

        }

        final Set<Byte> safeKeys = Sets.newHashSet();

        if (isEndOfPhase) {
            safeKeys.addAll(StaticAbilityUnspentMana.getManaToKeep(owner));
        }
        if (convertTo != null) {
            safeKeys.add(convertTo);
        }

        Predicate<Mana> retain = m -> safeKeys.contains(m.getColor());

        if (isEndOfPhase && !owner.getGame().getPhaseHandler().is(PhaseType.CLEANUP)) {
            retain.or(m -> {
                if (m.isPersistentMana()) {
                    return true;
                }
                if (m.isCombatMana() && !owner.getGame().getPhaseHandler().is(PhaseType.COMBAT_END)) {
                    return true;
                }
                return false;
            });
        }

        Multiset<Mana> convertedMana = null;
        if (convertTo != null) {
            convertedMana = HashMultiset.create();
            for (Multiset.Entry<Mana> e : Multisets.filter(floatingMana, Predicate.not(retain)::test).entrySet()) {
                convertedMana.add(e.getElement().convertColor(convertTo), e.getCount());
            }
        }
        floatingMana.removeIf(Predicate.not(retain)::test);
        if (convertedMana != null) {
            floatingMana.addAll(convertedMana);
        }

        owner.updateManaForView();
        owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Cleared, null));
        return cleared;
    }

    public boolean removeManaNoEvent(final Mana mana) {
        return floatingMana.remove(mana);
    }

    public boolean removeMana(Mana... manaList) {
        return removeMana(Arrays.asList(manaList));
    }

    public boolean removeMana(final Iterable<Mana> manaList) {
        Set<MagicColor.Color> colors;
        if (manaList instanceof Multiset<Mana> manaSet) {
            colors = manaSet.elementSet().stream().map(m -> MagicColor.Color.fromByte(m.getColor())).collect(Collectors.toSet());
            Multisets.removeOccurrences(floatingMana, manaSet);
        } else {
            colors = EnumSet.noneOf(MagicColor.Color.class);
            for (Mana m : manaList) {
                if (floatingMana.remove(m)) {
                    colors.add(MagicColor.Color.fromByte(m.getColor()));
                }
            }
        }
        owner.updateManaForView();
        owner.getGame().fireEvent(new GameEventManaPool(owner, EventValueChangeType.Removed, colors));
        return !colors.isEmpty();
    }

    public final void payManaFromAbility(final SpellAbility saPaidFor, ManaCostBeingPaid manaCost, final SpellAbility saPayment) {
        // Mana restriction must be checked before this method is called
        final List<SpellAbility> paidAbs = saPaidFor.getPayingManaAbilities();

        paidAbs.add(saPayment); // assumes some part on the mana produced by the ability will get used

        // need to get all mana from all ManaAbilities of the SpellAbility
        for (AbilityManaPart mp : saPayment.getAllManaParts()) {
            for (final Mana mana : mp.getLastManaProduced()) {
                if (!saPaidFor.allowsPayingWithShard(mp.getSourceCard(), mana.getColor())) {
                    continue;
                }
                if (tryPayCostWithMana(saPaidFor, manaCost, mana, false)) {
                    saPaidFor.getPayingMana().add(mana);
                }
            }
        }
    }

    public boolean tryPayCostWithColor(byte colorCode, SpellAbility saPaidFor, ManaCostBeingPaid manaCost, List<Mana> manaSpentToPay) {
        Mana manaFound = null;

        for (final Mana mana : Multisets.filter(this.floatingMana, m -> m.getColor() == colorCode)) {
            if (!mana.meetsManaRestrictions(saPaidFor)) {
                continue;
            }

            if (!saPaidFor.allowsPayingWithShard(mana.getSourceCard(), colorCode)) {
                continue;
            }

            manaFound = mana;
            break;
        }

        if (manaFound != null && tryPayCostWithMana(saPaidFor, manaCost, manaFound, false)) {
            manaSpentToPay.add(manaFound);
            return true;
        }
        return false;
    }

    public boolean tryPayCostWithMana(final SpellAbility sa, ManaCostBeingPaid manaCost, final Mana mana, boolean test) {
        if (!manaCost.isNeeded(mana, this)) {
            return false;
        }
        // only pay mana into manaCost when the Mana could be removed from the Mana pool
        // if the mana wasn't in the mana pool then something is wrong
        if (!removeMana(mana)) {
            return false;
        }
        manaCost.payMana(mana, this);

        return true;
    }

    public final boolean isEmpty() {
        return floatingMana.isEmpty();
    }

    public final int totalMana() {
        return floatingMana.size();
    }

    public final Map<Byte, Integer> getView() {
        return floatingMana.entrySet().stream().collect(Collectors.groupingBy(e -> e.getElement().getColor(), Collectors.summingInt(Multiset.Entry::getCount)));
    }

    public final Multiset<Mana> filter(final Predicate<Mana> predicate) {
        return Multisets.filter(this.floatingMana, predicate::test);
    }

    //Account for mana part of ability when undoing it
    public boolean accountFor(final AbilityManaPart ma) {
        if (ma == null) {
            return false;
        }
        if (floatingMana.isEmpty()) {
            return false;
        }

        // loop over mana produced by mana ability
        Multiset<Mana> produced = HashMultiset.create(ma.getLastManaProduced());

        if (Multisets.containsOccurrences(floatingMana, produced)) {
            removeMana(produced);
            return true;
        }
        return false;
    }

    public void refundMana(List<Mana> manaSpent) {
        addMana(manaSpent);
        manaSpent.clear();
    }

    public boolean canPayForShardWithColor(ManaCostShard shard, byte color) {
        if (shard.isOfKind(ManaAtom.COLORLESS) && color == ManaAtom.GENERIC) {
            return false; // FIXME: testing Colorless against Generic is a recipe for disaster, but probably there should be a better fix.
        }

        byte line = getPossibleColorUses(color);

        for (byte outColor : ManaAtom.MANATYPES) {
            if ((line & outColor) != 0 && shard.canBePaidWithManaOfColor(outColor)) {
                return true;
            }
        }

        return shard.canBePaidWithManaOfColor((byte)0);
    }

    /**
     * Checks if the given mana cost can be paid from floating mana.
     * @param cost mana cost to pay for
     * @param sa ability to pay for
     * @param test actual payment is made if this is false
     * @param manaSpentToPay list of mana spent
     * @return whether the floating mana is sufficient to pay the cost fully
     */
    public boolean payManaCostFromPool(final ManaCostBeingPaid cost, final SpellAbility sa, final boolean test, List<Mana> manaSpentToPay) {
        final boolean hasConverge = sa.getHostCard().hasConverge();
        List<ManaCostShard> unpaidShards = cost.getUnpaidShards();
        Collections.sort(unpaidShards); // most difficult shards must come first
        for (ManaCostShard part : unpaidShards) {
            if (part != ManaCostShard.X) {
                if (cost.isPaid()) {
                    continue;
                }

                // get a mana of this type from floating, bail if none available
                final Mana mana = CostPayment.getMana(owner, part, sa, hasConverge ? cost.getColorsPaid() : -1, cost.getXManaCostPaidByColor());
                if (mana != null) {
                    if (tryPayCostWithMana(sa, cost, mana, test)) {
                        manaSpentToPay.add(mana);
                    }
                }
            }
        }

        if (cost.isPaid()) {
            // refund any mana taken from mana pool when test
            if (test) {
                refundMana(manaSpentToPay);
            }
            return true;
        }
        return false;
    }

    @Override
    public Iterator<Mana> iterator() {
        return floatingMana.iterator();
    }

}
