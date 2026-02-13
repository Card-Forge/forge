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
package forge.card.mana;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * CardManaCost class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardManaCost.java 9708 2011-08-09 19:34:12Z jendave $
 */

public final class ManaCost implements Comparable<ManaCost>, Iterable<ManaCostShard>, Serializable, Cloneable {
    private static final long serialVersionUID = -2477430496624149226L;

    private static final char DELIM = (char)6;

    private List<ManaCostShard> shards;
    private final int genericCost;
    private final boolean hasNoCost; // lands cost
    private String stringValue; // precalculated for toString;

    private Float compareWeight = null;

    public static final ManaCost NO_COST = new ManaCost(-1);
    public static final ManaCost ZERO = new ManaCost(0);
    public static final ManaCost ONE = new ManaCost(1);
    public static final ManaCost TWO = new ManaCost(2);
    public static final ManaCost THREE = new ManaCost(3);
    public static final ManaCost FOUR = new ManaCost(4);

    public static ManaCost get(int cntGeneric) {
        switch (cntGeneric) {
            case 0: return ZERO;
            case 1: return ONE;
            case 2: return TWO;
            case 3: return THREE;
            case 4: return FOUR;
        }
        return cntGeneric > 0 ? new ManaCost(cntGeneric) : NO_COST;
    }

    // pass mana cost parser here
    private ManaCost(int cmc) {
        this.hasNoCost = cmc < 0;
        this.genericCost = cmc < 0 ? 0 : cmc;
        sealClass(Lists.newArrayList());
    }
    
    private ManaCost(int cmc, List<ManaCostShard> shards0) {
        this.hasNoCost = cmc < 0;
        this.genericCost = cmc < 0 ? 0 : cmc;
        sealClass(shards0);
    }

    private void sealClass(List<ManaCostShard> shards0) {
        this.shards = Collections.unmodifiableList(shards0);
        this.stringValue = this.getSimpleString();
    }

    // public ctor, should give it a mana parser
    /**
     * Instantiates a new card mana cost.
     * 
     * @param parser
     *            the parser
     */
    public ManaCost(final IParserManaCost parser) {
        final List<ManaCostShard> shardsTemp = Lists.newArrayList();
        boolean xMana = false;
        while (parser.hasNext()) {
            final ManaCostShard shard = parser.next();
            if (shard != null && shard != ManaCostShard.GENERIC) {
                if (shard == ManaCostShard.X) {
                    xMana = true;
                }
                shardsTemp.add(shard);
            } // null is OK - that was generic mana
        }
        int generic = parser.getTotalGenericCost(); // collect generic mana here
        this.hasNoCost = !xMana && generic == -1;
        this.genericCost = hasNoCost ? 0 : generic;
        sealClass(shardsTemp);
    }

    public ManaCost(final String str) {
        this(new ManaCostParser(str));
    }

    public String getSimpleString() {
        if (this.hasNoCost) {
            return "no cost";
        }
        if (this.shards.isEmpty()) {
            return "{" + this.genericCost + "}";
        }

        final StringBuilder sb = new StringBuilder();
        if (this.genericCost > 0) {
            sb.append("{").append(this.genericCost).append("}");
        }
        for (final ManaCostShard s : this.shards) {
            if (s == ManaCostShard.X) {
                sb.insert(0, s);
            } else {
                sb.append(s.toString());
            }
        }
        // If the generic cost has been reduced below 0, display the reduction. (Only set for X cost spells)
        if (this.genericCost < 0) {
            sb.append(' ').append(this.genericCost);
        }
        return sb.toString();
    }

    /**
     * Gets the cMC.
     * 
     * @return the cMC
     */
    public int getCMC() {
        int sum = 0;
        for (final ManaCostShard s : this.shards) {
            sum += s.getCmc();
        }
        return sum + this.genericCost;
    }

    /**
     * Gets the color profile.
     * 
     * @return the color profile
     */
    public byte getColorProfile() {
        byte result = 0;
        for (final ManaCostShard s : this.shards) {
            result |= s.getColorMask();
        }
        return result;
    }

    public int getShardCount(ManaCostShard which) {
        if (which == ManaCostShard.GENERIC) {
            return genericCost;
        }

        int res = 0;
        for (ManaCostShard shard : shards) {
            if (shard == which) {
                res++;
            }
        }
        return res;
    }

    /**
     * Gets the amount of color shards in the card's mana cost.
     * 
     * @return an array of five integers containing the amount of color shards in the card's mana cost in WUBRG order 
     */
    public int[] getColorShardCounts() {
        int[] counts = new int[6]; // in WUBRGC order

        for (int i = 0; i < stringValue.length(); i++) {
            char symbol = stringValue.charAt(i);
            switch (symbol) {
                case 'W': 
                case 'U':
                case 'B':
                case 'R':
                case 'G':
                case 'C':
                    counts[ManaAtom.getIndexOfFirstManaType(ManaAtom.fromName(symbol))]++;
                    break;
            }
            
        }

        return counts;
    }

    /**
     * Gets the generic cost.
     * 
     * @return the generic cost
     */
    public int getGenericCost() {
        return this.genericCost;
    }

    /**
     * Checks if is empty.
     * 
     * @return true, if is empty
     */
    public boolean isNoCost() {
        return this.hasNoCost;
    }

    /**
     * Checks if is pure generic.
     * 
     * @return true, if is pure generic
     */
    public boolean isPureGeneric() {
        return this.shards.isEmpty() && !this.isNoCost();
    }

    public boolean isZero() {
        return genericCost == 0 && isPureGeneric();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final ManaCost o) {
        return this.getCompareWeight().compareTo(o.getCompareWeight());
    }

    private Float getCompareWeight() {
        if (this.compareWeight == null) {
            float weight = this.genericCost;
            for (final ManaCostShard s : this.shards) {
                weight += s.getCmpc();
            }
            if (this.hasNoCost) {
                weight = -1; // for those who doesn't even have a 0 sign on card
            }
            this.compareWeight = weight;
        }
        return this.compareWeight;
    }

    public static String serialize(ManaCost mc) {
        StringBuilder builder = new StringBuilder();
        builder.append(mc.hasNoCost ? -1 : mc.genericCost);
        for (ManaCostShard shard : mc.shards) {
            builder.append(DELIM).append(shard.name());
        }
        return builder.toString();
    }
    public static ManaCost deserialize(String value) {
        String[] pieces = StringUtils.split(value, DELIM);
        ManaCost mc = new ManaCost(Integer.parseInt(pieces[0]));
        List<ManaCostShard> sh = Lists.newArrayList();
        for (int i = 1; i < pieces.length; i++) {
            sh.add(ManaCostShard.valueOf(pieces[i]));
        }
        mc.sealClass(sh);
        return mc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.stringValue;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public String getShortString() {
        if (isNoCost()) {
            return "-1";
        }
    	StringBuilder sb = new StringBuilder();
        int generic = getGenericCost();
        if (this.isZero()) {
            sb.append('0');
        }
        if (generic > 0) {
            sb.append(generic);
        }
        for (ManaCostShard s : this.shards) {
            sb.append(' ');
            sb.append(s);
        }
        // If the generic cost has been reduced below 0, display the reduction. (Only set for X cost spells)
        if (generic < 0) {
            sb.append(' ').append(generic);
        }
        return sb.toString().trim();
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public boolean hasPhyrexian() {
        for (ManaCostShard shard : shards) {
            if (shard.isPhyrexian()) {
                return true;
            }
        }
        return false;
    }
    
    public int getPhyrexianCount() {
        int i = 0;
        for (ManaCostShard shard : shards) {
            if (shard.isPhyrexian()) {
                i++;
            }
        }
        return i;
    }
    
    public boolean hasMultiColor() {
        for (ManaCostShard shard : shards) {
            if (shard.isMultiColor()) {
                return true;
            }
        }
        return false;
    }

    /**
     * works for Phyrexian Mana and 2Half mana, not for Hybrid mana
     * @return
     */
    public ManaCost getNormalizedMana() {
        List<ManaCostShard> list = Lists.newArrayList();
        for (ManaCostShard shard : shards) {
            list.add(ManaCostShard.valueOf(shard.getColorMask()));
        }
        
        return new ManaCost(this.genericCost, list);
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public int countX() {
        return getShardCount(ManaCostShard.X);
    }

    /**
     * Can this mana cost be paid with unlimited mana of given color set.
     * @param colorCode
     * @return
     */
    public boolean canBePaidWithAvailable(byte colorCode) {
        for (ManaCostShard shard : shards) {
            if (!shard.isPhyrexian() && !shard.canBePaidWithManaOfColor(colorCode)) {
                return false;
            }
        }
        return true;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param a
     * @param b
     * @return
     */
    public static ManaCost combine(ManaCost a, ManaCost b) {
        ManaCost res = new ManaCost(a.genericCost + b.genericCost);
        List<ManaCostShard> sh = Lists.newArrayList();
        sh.addAll(a.shards);
        sh.addAll(b.shards);
        res.sealClass(sh);
        return res;
    }

    @Override
    public Iterator<ManaCostShard> iterator() {
        return this.shards.iterator();
    }

    public int getGlyphCount() { // counts all colored shards or 1 for {0} costs 
        int width = shards.size();
        if (genericCost > 0 || (genericCost == 0 && width == 0)) {
            width++;
        }
        // If the generic cost has been reduced below 0 (due to perpetual cost decrease effects)
        // and there is an X cost (so the below 0 generic cost actually does something) then
        // add space for an additional symbol to display the extra cost reduction.
        if (genericCost < 0 && countX() > 0) {
            width++;
        }
        return width;
    }
}
