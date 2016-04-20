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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.IParserManaCost;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;

import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

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
    }

    // holds Mana_Part objects
    // ManaPartColor is stored before ManaPartGeneric
    private final Map<ManaCostShard, ShardCount> unpaidShards = new HashMap<ManaCostShard, ShardCount>();
    private Map<String, Integer> xManaCostPaidByColor;
    private final String sourceRestriction;
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
            xManaCostPaidByColor = new HashMap<String, Integer>(manaCostBeingPaid.xManaCostPaidByColor);
        }
        sourceRestriction = manaCostBeingPaid.sourceRestriction;
        sunburstMap = manaCostBeingPaid.sunburstMap;
        cntX = manaCostBeingPaid.cntX;
    }

    public ManaCostBeingPaid(ManaCost manaCost) {
        this(manaCost, null);
    }

    public ManaCostBeingPaid(ManaCost manaCost, String srcRestriction) {
        sourceRestriction = srcRestriction;
        if (manaCost == null) { return; }
        for (ManaCostShard shard : manaCost) {
            if (shard == ManaCostShard.X) {
                cntX++;
            }
            else {
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
            if (canBePaidWith(shard, paid, pool)) {
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
        }
        else {
            shard = ManaCostShard.valueOf(ManaAtom.fromName(xColor));
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
            //System.out.println("Tried to substract a " + shard.toString() + " shard that is not present in this ManaCostBeingPaid");
            return;
        }
        if (manaToSubtract >= sc.totalCount) {
            sc.xCount = 0;
            sc.totalCount = 0;
            unpaidShards.remove(shard);
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
                return canBePaidWith(ms, mana, pool);
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
                xManaCostPaidByColor = new HashMap<String, Integer>();
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

    private static boolean canBePaidWith(final ManaCostShard shard, final Mana mana, final ManaPool pool) {
        if (shard.isSnow() && !mana.isSnow()) {
            return false;
        }

        byte color = mana.getColor();
        return pool.canPayForShardWithColor(shard, color);
    }

    public final void addManaCost(final ManaCost extra) {
        for (ManaCostShard shard : extra) {
            if (shard == ManaCostShard.X) {
                cntX++;
            }
            else {
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

        if (addX) {
            for (int i = 0; i < this.getXcounter(); i++) {
                sb.append("{X}");
            }
        }

        int nGeneric = getGenericManaAmount();
        List<ManaCostShard> shards = new ArrayList<ManaCostShard>(unpaidShards.keySet());

        if (pool != null) { //replace shards with generic mana if they can be paid with any color mana
            for (int i = 0; i < shards.size(); i++) {
                ManaCostShard shard = shards.get(i);
                if (shard != ManaCostShard.GENERIC && pool.getPossibleColorUses(shard.getColorMask()) == MagicColor.ALL_COLORS) {
                    nGeneric += unpaidShards.get(shard).totalCount;
                    shards.remove(i);
                    i--;
                }
            }
        }

        if (nGeneric > 0) {
            if (nGeneric <= 20) {
                sb.append("{" + nGeneric + "}");
            }
            else { //if no mana symbol exists for generic amount, use combination of symbols for each digit
                String genericStr = String.valueOf(nGeneric);
                for (int i = 0; i < genericStr.length(); i++) {
                    sb.append("{" + genericStr.charAt(i) + "}");
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
        List<ManaCostShard> result = new ArrayList<ManaCostShard>();
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

    public String getSourceRestriction() {
        return sourceRestriction;
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
}
