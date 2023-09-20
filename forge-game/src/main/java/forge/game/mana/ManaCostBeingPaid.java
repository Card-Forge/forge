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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.IParserManaCost;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;

/**
 * <p>
 * ManaCostBeingPaid class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ManaCostBeingPaid {
    private class ManaCostBeingPaidIterator implements IParserManaCost {
        private Iterator<ManaCostShard> mch;
        private ManaCostShard nextShard = null;
        private int remainingShards = 0;
        private boolean hasSentX = false;

        public ManaCostBeingPaidIterator() {
            mch = unpaidShards.keySet().iterator();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ManaCostShard next() {
            if (remainingShards == 0) {
                throw new UnsupportedOperationException("All shards were depleted, call hasNext()");
            }
            remainingShards--;
            return nextShard;
        }

        @Override
        public boolean hasNext() {
            if (remainingShards > 0) { return true; }
            if (!hasSentX) {
                if (nextShard != ManaCostShard.X && cntX > 0) {
                    nextShard = ManaCostShard.X;
                    remainingShards = cntX;
                    return true;
                }
                else {
                    hasSentX = true;
                }
            }
            if (!mch.hasNext()) { return false; }

            nextShard = mch.next();
            if (nextShard == ManaCostShard.GENERIC) {
                return this.hasNext(); // skip generic
            }
            remainingShards = unpaidShards.get(nextShard).totalCount;
            return true;
        }

        @Override
        public int getTotalGenericCost() {
            ShardCount c = unpaidShards.get(ManaCostShard.GENERIC);
            return c == null ? 0 : c.totalCount;
        }
    }

    private class ShardCount {
        private int xCount;
        private int totalCount;

        private ShardCount() {
        }
        private ShardCount(ShardCount copy) {
            xCount = copy.xCount;
            totalCount = copy.totalCount;
        }

        @Override
        public String toString() {
            return "{x=" + xCount + " total=" + totalCount + "}";
        }
    }

    // holds Mana_Part objects
    // ManaPartColor is stored before ManaPartGeneric
    private final Map<ManaCostShard, ShardCount> unpaidShards = Maps.newHashMap();
    private Map<String, Integer> xManaCostPaidByColor;
    private byte sunburstMap = 0;
    private int cntX = 0;

    /**
     * Copy constructor
     * @param manaCostBeingPaid
     */
    public ManaCostBeingPaid(ManaCostBeingPaid manaCostBeingPaid) {
        for (Entry<ManaCostShard, ShardCount> m : manaCostBeingPaid.unpaidShards.entrySet()) {
            unpaidShards.put(m.getKey(), new ShardCount(m.getValue()));
        }
        if (manaCostBeingPaid.xManaCostPaidByColor != null) {
            xManaCostPaidByColor = Maps.newHashMap(manaCostBeingPaid.xManaCostPaidByColor);
        }
        sunburstMap = manaCostBeingPaid.sunburstMap;
        cntX = manaCostBeingPaid.cntX;
    }

    public ManaCostBeingPaid(ManaCost manaCost) {
        if (manaCost == null) { return; }
        for (ManaCostShard shard : manaCost) {
            if (shard == ManaCostShard.X) {
                cntX++;
            } else {
                increaseShard(shard, 1, false);
            }
        }
        increaseGenericMana(manaCost.getGenericCost());
    }

    public Map<String, Integer> getXManaCostPaidByColor() {
        return xManaCostPaidByColor;
    }

    public final int getSunburst() {
        return ColorSet.fromMask(sunburstMap).countColors();
    }

    public final byte getColorsPaid() {
        return sunburstMap;
    }

    public final boolean containsPhyrexianMana() {
        for (ManaCostShard shard : unpaidShards.keySet()) {
            if (shard.isPhyrexian()) {
                return true;
            }
        }
        return false;
    }

    public final boolean containsOnlyPhyrexianMana() {
        for (ManaCostShard shard : unpaidShards.keySet()) {
            if (!shard.isPhyrexian()) {
                return false;
            }
        }
        return true;
    }

    public final boolean payPhyrexian() {
        ManaCostShard phy = null;
        for (ManaCostShard mcs : unpaidShards.keySet()) {
            if (mcs.isPhyrexian()) {
                phy = mcs;
                break;
            }
        }

        if (phy == null) {
            return false;
        }

        decreaseShard(phy, 1);
        return true;
    }

    // takes a Short Color and returns true if it exists in the mana cost.
    // Easier for split costs
    public final boolean needsColor(final byte colorMask, final ManaPool pool) {
        for (ManaCostShard shard : unpaidShards.keySet()) {
            if (shard == ManaCostShard.GENERIC) {
                continue;
            }
            if (shard.isOr2Generic()) {
                if ((shard.getColorMask() & colorMask) != 0) {
                    return true;
                }
            }
            else if (pool.canPayForShardWithColor(shard, colorMask)) {
                return true;
            }
        }
        return false;
    }

    // isNeeded(String) still used by the Computer, might have problems activating Snow abilities
    public final boolean isAnyPartPayableWith(byte colorMask, final ManaPool pool) {
        for (ManaCostShard shard : unpaidShards.keySet()) {
            if (pool.canPayForShardWithColor(shard, colorMask)) {
                return true;
            }
        }
        return false;
    }

    public final boolean isNeeded(final Mana paid, final ManaPool pool) {
        for (ManaCostShard shard : unpaidShards.keySet()) {
            if (canBePaidWith(shard, paid, pool, xManaCostPaidByColor)) {
                return true;
            }
        }
        return false;
    }

    public final boolean isPaid() {
        return unpaidShards.isEmpty();
    }

    public final void setXManaCostPaid(final int xPaid, final String xColor) {
        int xCost = xPaid * cntX;
        cntX = 0;

        ManaCostShard shard;
        if (StringUtils.isEmpty(xColor)) {
            shard = ManaCostShard.GENERIC;
        } else {
            shard = ManaCostShard.parseNonGeneric(xColor);
        }
        increaseShard(shard, xCost, true);
    }

    public final void increaseGenericMana(final int toAdd) {
        increaseShard(ManaCostShard.GENERIC, toAdd, false);
    }
    public final void increaseShard(final ManaCostShard shard, final int toAdd) {
        increaseShard(shard, toAdd, false);
    }
    private final void increaseShard(final ManaCostShard shard, final int toAdd, final boolean forX) {
        if (toAdd <= 0) { return; }

        ShardCount sc = unpaidShards.get(shard);
        if (sc == null) {
            sc = new ShardCount();
            unpaidShards.put(shard, sc);
        }
        if (forX) {
            sc.xCount += toAdd;
        }
        sc.totalCount += toAdd;
    }

    public final void decreaseGenericMana(final int manaToSubtract) {
        decreaseShard(ManaCostShard.GENERIC, manaToSubtract);
    }

    public final void decreaseShard(final ManaCostShard shard, final int manaToSubtract) {
        if (manaToSubtract <= 0) {
            return;
        }

        ShardCount sc = unpaidShards.get(shard);
        if (sc == null) {
            // only special rules for Mono Color Shards and for Generic
            if (!shard.isMonoColor() && shard != ManaCostShard.GENERIC) {
                return;
            }
            int otherSubtract = manaToSubtract;
            List<ManaCostShard> toRemove = Lists.newArrayList();

            //TODO move that for parts into extra function if able

            // try to remove multicolored hybrid shards
            // for that, this shard need to be mono colored
            if (shard.isMonoColor()) {
                for (Entry<ManaCostShard, ShardCount> e : unpaidShards.entrySet()) {
                    final ManaCostShard eShard = e.getKey();
                    sc = e.getValue();
                    if (eShard != ManaCostShard.COLORED_X && eShard.isOfKind(shard.getShard()) && !eShard.isMonoColor()) {
                        if (otherSubtract >= sc.totalCount) {
                            otherSubtract -= sc.totalCount;
                            sc.xCount = sc.totalCount = 0;
                            toRemove.add(eShard);
                        } else {
                            sc.totalCount -= otherSubtract;
                            if (sc.xCount > sc.totalCount) {
                                sc.xCount = sc.totalCount;
                            }
                            // nothing more left in otherSubtract
                            return;
                        }
                    }
                }

                // try to remove 2 generic hybrid shards with colored shard
                for (Entry<ManaCostShard, ShardCount> e : unpaidShards.entrySet()) {
                    final ManaCostShard eShard = e.getKey();
                    sc = e.getValue();
                    if (eShard.isOfKind(shard.getShard()) && eShard.isOr2Generic()) {
                        if (otherSubtract >= sc.totalCount) {
                            otherSubtract -= sc.totalCount;
                            sc.xCount = sc.totalCount = 0;
                            toRemove.add(eShard);
                        } else {
                            sc.totalCount -= otherSubtract;
                            if (sc.xCount > sc.totalCount) {
                                sc.xCount = sc.totalCount;
                            }
                            // nothing more left in otherSubtract
                            return;
                        }
                    }
                }

                // try to remove phyrexian shards with colored shard
                for (Entry<ManaCostShard, ShardCount> e : unpaidShards.entrySet()) {
                    final ManaCostShard eShard = e.getKey();
                    sc = e.getValue();
                    if (eShard.isOfKind(shard.getShard()) && eShard.isPhyrexian()) {
                        if (otherSubtract >= sc.totalCount) {
                            otherSubtract -= sc.totalCount;
                            sc.xCount = sc.totalCount = 0;
                            toRemove.add(eShard);
                        } else {
                            sc.totalCount -= otherSubtract;
                            if (sc.xCount > sc.totalCount) {
                                sc.xCount = sc.totalCount;
                            }
                            // nothing more left in otherSubtract
                            return;
                        }
                    }
                }
            } else if (shard == ManaCostShard.GENERIC) {
                // try to remove 2 generic hybrid shards WITH generic shard
                int shardAmount = otherSubtract / 2;
                for (Entry<ManaCostShard, ShardCount> e : unpaidShards.entrySet()) {
                    final ManaCostShard eShard = e.getKey();
                    sc = e.getValue();
                    if (eShard.isOr2Generic()) {
                        if (shardAmount >= sc.totalCount) {
                            shardAmount -= sc.totalCount;
                            otherSubtract -= sc.totalCount * 2;
                            sc.xCount = sc.totalCount = 0;
                            toRemove.add(eShard);
                        } else {
                            sc.totalCount -= shardAmount;
                            if (sc.xCount > sc.totalCount) {
                                sc.xCount = sc.totalCount;
                            }
                            // nothing more left in otherSubtract
                            return;
                        }
                    } else if (sc.xCount > 0) { // X part that can only be paid by specific color
                        if (otherSubtract >= sc.xCount) {
                            otherSubtract -= sc.xCount;
                            sc.totalCount -= sc.xCount;
                            sc.xCount = 0;
                            if (sc.totalCount == 0) {
                                toRemove.add(eShard);
                            }
                        } else {
                            sc.totalCount -= otherSubtract;
                            sc.xCount -= otherSubtract;
                            // nothing more left in otherSubtract
                            return;
                        }
                    }
                }
            }

            unpaidShards.keySet().removeAll(toRemove);
            //System.out.println("Tried to substract a " + shard.toString() + " shard that is not present in this ManaCostBeingPaid");
            return;
        }

        int difference = manaToSubtract - sc.totalCount;

        if (manaToSubtract >= sc.totalCount) {
            sc.xCount = 0;
            sc.totalCount = 0;
            unpaidShards.remove(shard);
            // try to remove difference from the rest
            this.decreaseShard(shard, difference);
            return;
        }

        sc.totalCount -= manaToSubtract;
        if (sc.xCount > sc.totalCount) {
            sc.xCount = sc.totalCount; //only decrease xCount if it would otherwise be greater than totalCount
        }
    }

    public final int getGenericManaAmount() {
        ShardCount sc = unpaidShards.get(ManaCostShard.GENERIC);
        if (sc != null) {
            return sc.totalCount;
        }
        return 0;
    }

    /**
     * <p>
     * addMana.
     * </p>
     * 
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean ai_payMana(final String mana, final ManaPool pool) {
        final byte colorMask = ManaAtom.fromName(mana);
        if (!this.isAnyPartPayableWith(colorMask, pool)) {
            //System.out.println("ManaCost : addMana() error, mana not needed - " + mana);
            return false;
            //throw new RuntimeException("ManaCost : addMana() error, mana not needed - " + mana);
        }

        Predicate<ManaCostShard> predCanBePaid = new Predicate<ManaCostShard>() {
            @Override
            public boolean apply(ManaCostShard ms) {
                // Check Colored X and see if the color is already used
                if (ms == ManaCostShard.COLORED_X && !canColoredXShardBePaidByColor(MagicColor.toShortString(colorMask), xManaCostPaidByColor)) {
                    return false;
                }
                return pool.canPayForShardWithColor(ms, colorMask);
            }
        };

        return tryPayMana(colorMask, Iterables.filter(unpaidShards.keySet(), predCanBePaid), pool.getPossibleColorUses(colorMask)) != null;
    }

    /**
     * <p>
     * addMana.
     * </p>
     * 
     * @param mana
     *            a {@link forge.game.mana.Mana} object.
     * @return a boolean.
     */
    public final boolean payMana(final Mana mana, final ManaPool pool) {
        if (!this.isNeeded(mana, pool)) {
            throw new RuntimeException("ManaCost : addMana() error, mana not needed - " + mana);
        }

        Predicate<ManaCostShard> predCanBePaid = new Predicate<ManaCostShard>() {
            @Override
            public boolean apply(ManaCostShard ms) {
                return canBePaidWith(ms, mana, pool, xManaCostPaidByColor);
            }
        };

        byte inColor = mana.getColor();
        byte outColor = pool.getPossibleColorUses(inColor);
        return tryPayMana(inColor, Iterables.filter(unpaidShards.keySet(), predCanBePaid), outColor) != null;
    }
    
    public final ManaCostShard payManaViaConvoke(final byte color) {
        Predicate<ManaCostShard> predCanBePaid = new Predicate<ManaCostShard>() {
            @Override
            public boolean apply(ManaCostShard ms) {
                return ms.canBePaidWithManaOfColor(color);
            }
        };
        return tryPayMana(color, Iterables.filter(unpaidShards.keySet(), predCanBePaid), (byte)0xFF);
    }

    public ManaCostShard getShardToPayByPriority(Iterable<ManaCostShard> payableShards, byte possibleUses) {
        Set<ManaCostShard> choice = EnumSet.noneOf(ManaCostShard.class);
        int priority = Integer.MIN_VALUE;
        for (ManaCostShard toPay : payableShards) {
            // if m is a better to pay than choice
            int toPayPriority = getPayPriority(toPay, possibleUses);
            if (toPayPriority > priority) {
                priority = toPayPriority;
                choice.clear();
            }
            if (toPayPriority == priority) {
                choice.add(toPay);
            }
        }
        if (choice.isEmpty()) {
            return null;
        }

       return Iterables.getFirst(choice, null);
    }

    private ManaCostShard tryPayMana(final byte colorMask, Iterable<ManaCostShard> payableShards, byte possibleUses) {
        ManaCostShard chosenShard = getShardToPayByPriority(payableShards, possibleUses);
        if (chosenShard == null) {
            return null;
        }
        ShardCount sc = unpaidShards.get(chosenShard);
        if (sc != null && sc.xCount > 0) {
            //if there's any X part of the cost for the chosen shard, pay it off first and track what color was spent to pay X
            sc.xCount--;
            String color = MagicColor.toShortString(colorMask);
            if (xManaCostPaidByColor == null) {
                xManaCostPaidByColor = Maps.newHashMap();
            }
            Integer xColor = xManaCostPaidByColor.get(color);
            if (xColor == null) {
                xColor = 0;
            }
            xManaCostPaidByColor.put(color, xColor + 1);
        }

        decreaseShard(chosenShard, 1);
        if (chosenShard.isOr2Generic() && ( 0 == (chosenShard.getColorMask() & possibleUses) )) {
            this.increaseGenericMana(1);
        }

        this.sunburstMap |= colorMask;
        return chosenShard;
    }

    private static int getPayPriority(final ManaCostShard bill, final byte paymentColor) {
        if (bill == ManaCostShard.GENERIC) {
            return 2;
        }

        if (bill.isMonoColor()) {
            if (bill.isOr2Generic()) {
                // The generic portion of a 2/Colored mana, should be lower priority than generic mana
                return !ColorSet.fromMask(bill.getColorMask() & paymentColor).isColorless() ? 9 : 1;
            }
            if (!bill.isPhyrexian()) {
                return 10;
            }
            return 8;
        }
        return 5;
    }

    public static boolean canColoredXShardBePaidByColor(String color, Map<String, Integer> xManaCostPaidByColor) {
        if (xManaCostPaidByColor != null && xManaCostPaidByColor.get(color) != null) {
            return false;
        }
        return true;
    }

    private static boolean canBePaidWith(final ManaCostShard shard, final Mana mana, final ManaPool pool, Map<String, Integer> xManaCostPaidByColor) {
        if (shard.isSnow() && !mana.isSnow()) {
            return false;
        }
        if (mana.isRestricted() && !mana.getManaAbility().meetsManaShardRestrictions(shard, mana.getColor())) {
        	return false;
        }

        // snow can be paid for any color
        if (shard.getColorMask() != 0 && mana.isSnow() && pool.isSnowForColor()) {
            return true;
        }

        // Check Colored X and see if the color is already used
        if (shard == ManaCostShard.COLORED_X && !canColoredXShardBePaidByColor(MagicColor.toShortString(mana.getColor()), xManaCostPaidByColor)) {
            return false;
        }

        byte color = mana.getColor();
        return pool.canPayForShardWithColor(shard, color);
    }

    public final void addManaCost(final ManaCost extra) {
        for (ManaCostShard shard : extra) {
            if (shard == ManaCostShard.X) {
                cntX++;
            } else {
                increaseShard(shard, 1, false);
            }
        }
        increaseGenericMana(extra.getGenericCost());
    }

    public final void subtractManaCost(final ManaCost subThisManaCost) {
        for (ManaCostShard shard : subThisManaCost) {
            if (shard == ManaCostShard.X) {
                cntX--;
            }
            else if (unpaidShards.containsKey(shard)) {
                decreaseShard(shard, 1);
            }
            else {
                decreaseGenericMana(1);
            }
        }
        decreaseGenericMana(subThisManaCost.getGenericCost());
    }

    /**
     * To string.
     * 
     * @param addX
     *            the add x
     * @return the string
     */
    public final String toString(final boolean addX, final ManaPool pool) {
        // Boolean addX used to add Xs into the returned value
        final StringBuilder sb = new StringBuilder();

        // TODO Prepend a line about paying with any type/color if available
        if (addX) {
            for (int i = 0; i < this.getXcounter(); i++) {
                sb.append("{X}");
            }
        }

        int nGeneric = getGenericManaAmount();
        List<ManaCostShard> shards = Lists.newArrayList(unpaidShards.keySet());

        if (nGeneric > 0) {
            if (nGeneric <= 20) {
                sb.append("{").append(nGeneric).append("}");
            }
            else { //if no mana symbol exists for generic amount, use combination of symbols for each digit
                String genericStr = String.valueOf(nGeneric);
                for (int i = 0; i < genericStr.length(); i++) {
                    sb.append("{").append(genericStr.charAt(i)).append("}");
                }
            }
        }

        // Sort the keys to get a deterministic ordering.
        Collections.sort(shards);
        for (ManaCostShard shard : shards) {
            if (shard == ManaCostShard.GENERIC) {
                continue;
            }
            
            final String str = shard.toString();
            final int count = unpaidShards.get(shard).totalCount;
            for (int i = 0; i < count; i++) {
                sb.append(str);
            }
        }

        return sb.length() == 0 ? "0" : sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return this.toString(true, null);
    }

    /**
     * <p>
     * getConvertedManaCost.
     * </p>
     * 
     * @return a int.
     */
    public final int getConvertedManaCost() {
        int cmc = 0;

        for (final Entry<ManaCostShard, ShardCount> s : this.unpaidShards.entrySet()) {
            cmc += s.getKey().getCmc() * s.getValue().totalCount;
        }
        return cmc;
    }

    public ManaCost toManaCost() {
        return new ManaCost(new ManaCostBeingPaidIterator());
    }

    public final int getXcounter() {
        return cntX;
    }

    public final List<ManaCostShard> getUnpaidShards() {
        List<ManaCostShard> result = new ArrayList<>();
        for (Entry<ManaCostShard, ShardCount> kv : unpaidShards.entrySet()) {
           for (int i = kv.getValue().totalCount; i > 0; i--) {
               result.add(kv.getKey());
           }
        }
        for (int i = cntX; i > 0; i--) {
            result.add(ManaCostShard.X);
        }
        return result;
    }

    public final void removeGenericMana() {
        unpaidShards.remove(ManaCostShard.GENERIC);
    }

    public Iterable<ManaCostShard> getDistinctShards() {
        return unpaidShards.keySet();
    }

    public int getUnpaidShards(ManaCostShard key) {
        ShardCount sc = unpaidShards.get(key);
        if (sc != null) {
            return sc.totalCount;
        }
        return 0;
    }

    public final byte getUnpaidColors() {
        byte result = 0;
        for (ManaCostShard s : unpaidShards.keySet()) {
            result |= s.getColorMask();
        }
        return result;
    }

    public boolean hasAnyKind(int kind) {
        for (Map.Entry<ManaCostShard, ShardCount> e : unpaidShards.entrySet()) {
            if (e.getKey().isOfKind(kind) && e.getValue().totalCount > e.getValue().xCount) {
                return true;
            }
        }
        return false;
    }
}
