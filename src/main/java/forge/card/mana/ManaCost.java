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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import forge.Constant;
import forge.card.CardManaCost;
import forge.control.input.InputPayManaCostUtil;

/**
 * <p>
 * ManaCost class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ManaCost {
    // holds Mana_Part objects
    // ManaPartColor is stored before ManaPartColorless
    private final HashMap<ManaCostShard, Integer> unpaidShards = new HashMap<ManaCostShard, Integer>();
    private final HashMap<String, Integer> sunburstMap = new HashMap<String, Integer>();
    private final ArrayList<String> manaNeededToAvoidNegativeEffect = new ArrayList<String>();
    private final ArrayList<String> manaPaidToAvoidNegativeEffect = new ArrayList<String>();

    // manaCost can be like "0", "3", "G", "GW", "10", "3 GW", "10 GW"
    // or "split hybrid mana" like "2/G 2/G", "2/B 2/B 2/B"
    // "GW" can be paid with either G or W

    /**
     * <p>
     * Constructor for ManaCost.
     * </p>
     * 
     * @param manaCost
     *            a {@link java.lang.String} object.
     */
    public ManaCost(String sCost) {
        if ( "0".equals(sCost) || "C".equals(sCost) ) 
            return;

        final CardManaCost manaCost = new CardManaCost(new ManaCostParser(sCost));
        for(ManaCostShard shard : manaCost.getShards()) {
            increaseShard(shard, 1);
        }
        int generic = manaCost.getGenericCost();
        if( generic > 0 )
            unpaidShards.put(ManaCostShard.COLORLESS, Integer.valueOf(generic));
    }

    /**
     * <p>
     * getSunburst.
     * </p>
     * 
     * @return a int.
     */
    public final int getSunburst() {
        final int ret = this.sunburstMap.size();
        this.sunburstMap.clear();
        return ret;
    }

    /**
     * <p>
     * getColorsPaid.
     * </p>
     * 
     * @return a String.
     */
    public final String getColorsPaid() {
        String s = "";
        for (final String key : this.sunburstMap.keySet()) {
            if (key.equalsIgnoreCase("black") || key.equalsIgnoreCase("B")) {
                s += "B";
            }
            if (key.equalsIgnoreCase("blue") || key.equalsIgnoreCase("U")) {
                s += "U";
            }
            if (key.equalsIgnoreCase("green") || key.equalsIgnoreCase("G")) {
                s += "G";
            }
            if (key.equalsIgnoreCase("red") || key.equalsIgnoreCase("R")) {
                s += "R";
            }
            if (key.equalsIgnoreCase("white") || key.equalsIgnoreCase("W")) {
                s += "W";
            }
        }
        return s;
    }

    /**
     * <p>
     * getUnpaidPhyrexianMana.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    private List<ManaCostShard> getUnpaidPhyrexianMana() {
        ArrayList<ManaCostShard> res = new ArrayList<ManaCostShard>();
        for(final Entry<ManaCostShard, Integer> part : this.unpaidShards.entrySet() )
        {
            if( !part.getKey().isPhyrexian() ) continue;
            for(int i = 0; i < part.getValue(); i++)
                res.add(part.getKey());
        }
        return res;
    }

    /**
     * <p>
     * containsPhyrexianMana.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean containsPhyrexianMana() {
        for(ManaCostShard shard : unpaidShards.keySet()) {
            if ( shard.isPhyrexian() ) return true; 
        }
        return false;
    }

    /**
     * <p>
     * payPhyrexian.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean payPhyrexian() {
        final List<ManaCostShard> phy = this.getUnpaidPhyrexianMana();

        if (phy.size() > 0) {
            Integer cnt = unpaidShards.get(phy.get(0));
            if( cnt <= 1 ) unpaidShards.remove(phy.get(0));
            else unpaidShards.put(phy.get(0), Integer.valueOf(cnt - 1));

            return true;
        }

        return false;
    }

    // takes a Short Color and returns true if it exists in the mana cost.
    // Easier for split costs
    /**
     * <p>
     * isColor.
     * </p>
     * 
     * @param color
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean isColor(final String color) {
        if ( "1".equals(color) ) return getColorlessManaAmount() > 0;
        
        for(ManaCostShard shard : unpaidShards.keySet())
        {
            String ss = shard.toString(); 
            if (ss.contains(color))
                return true;
        }
        return false;
    }

    // isNeeded(String) still used by the Computer, might have problems
    // activating Snow abilities
    /**
     * <p>
     * isNeeded.
     * </p>
     * 
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean isNeeded(String mana) {
        if (this.manaNeededToAvoidNegativeEffect.size() != 0) {
            for (final String s : this.manaNeededToAvoidNegativeEffect) {
                if ((s.equalsIgnoreCase(mana) || s.substring(0, 1).equalsIgnoreCase(mana))
                        && !this.manaPaidToAvoidNegativeEffect.contains(mana)) {
                    return true;
                }
            }
        }
        if (mana.length() > 1) {
            mana = InputPayManaCostUtil.getShortColorString(mana);
        }
        for(ManaCostShard shard : unpaidShards.keySet()) {
            if (canBePaidWith(shard, mana)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * isNeeded.
     * </p>
     * 
     * @param paid
     *            a {@link forge.card.mana.ManaPaid} object.
     * @return a boolean.
     */
    public final boolean isNeeded(final ManaPaid paid) {
        for (ManaCostShard shard : unpaidShards.keySet()) {
            
            if (canBePaidWith(shard, paid)) {
                return true;
            }
            
            if (shard.isSnow() && paid.isSnow()) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * isPaid.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isPaid() {
        return unpaidShards.isEmpty();
    } // isPaid()

    /**
     * <p>
     * payMana.
     * </p>
     * 
     * @param mana
     *            a {@link forge.card.mana.ManaPaid} object.
     * @return a boolean.
     */
    public final boolean payMana(final ManaPaid mana) {
        return this.addMana(mana);
    }

    /**
     * <p>
     * payMana.
     * </p>
     * 
     * @param color
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean payMana(String color) {
        if (this.manaNeededToAvoidNegativeEffect.contains(color) && !this.manaPaidToAvoidNegativeEffect.contains(color)) {
            this.manaPaidToAvoidNegativeEffect.add(color);
        }
        color = InputPayManaCostUtil.getShortColorString(color);
        return this.addMana(color);
    }

    /**
     * <p>
     * increaseColorlessMana.
     * </p>
     * 
     * @param manaToAdd
     *            a int.
     */
    public final void increaseColorlessMana(final int manaToAdd) {
        increaseShard(ManaCostShard.COLORLESS, manaToAdd);
    }

    public final void increaseShard(final ManaCostShard shard, final int toAdd) {
        if (toAdd <= 0) {
            return;
        }
        
        Integer cnt = unpaidShards.get(shard);
        unpaidShards.put(shard, Integer.valueOf(cnt == null || cnt == 0 ? toAdd : toAdd + cnt ));
    }
    
    
    /**
     * <p>
     * decreaseColorlessMana
     * </p>
     * .
     * 
     * @param manaToSubtract
     *            an int. The amount of colorless mana to subtract from the
     *            cost.Used by Delve.
     */
    public final void decreaseColorlessMana(final int manaToSubtract) {
        decreaseShard(ManaCostShard.COLORLESS, manaToSubtract);
    }

    
    public final void decreaseShard(final ManaCostShard shard, final int manaToSubtract) {
        if (manaToSubtract <= 0) {
            return;
        }

        Integer genericCnt = unpaidShards.get(shard);
        if( null == genericCnt || genericCnt - manaToSubtract <= 0 )
            unpaidShards.remove(shard);
        else
            unpaidShards.put(shard, Integer.valueOf(genericCnt - manaToSubtract));
    }
    

    /**
     * <p>
     * getColorlessManaAmount
     * </p>
     * Returns how much colorless mana must be paid to pay the cost.Used by
     * Delve AI.
     * 
     * @return an int.
     */
    public final int getColorlessManaAmount() {
        Integer genericCnt = unpaidShards.get(ManaCostShard.COLORLESS);
        return genericCnt == null ? 0 : genericCnt;
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
    public final boolean addMana(final String mana) {
        if (!this.isNeeded(mana)) {
            System.out.println("ManaCost : addMana() error, mana not needed - " + mana);
            //throw new RuntimeException("ManaCost : addMana() error, mana not needed - " + mana);
        }

        ManaCostShard choice = null;
        for(ManaCostShard toPay : unpaidShards.keySet())
        {
            if (canBePaidWith(toPay, mana)) {
                // if m is a better to pay than choice
                if (choice == null) {
                    choice = toPay;
                    continue;
                }
                if (isFisrtChoiceBetter(toPay, choice)) {
                    choice = toPay;
                }
            }
        } // for
        if (choice == null) {
            return false;
        }

        decreaseShard(choice, 1);
        if (!mana.equals(Constant.Color.COLORLESS)) {
            if (this.sunburstMap.containsKey(mana)) {
                this.sunburstMap.put(mana, this.sunburstMap.get(mana) + 1);
            } else {
                this.sunburstMap.put(mana, 1);
            }
        }
        return true;
    }

    private boolean isFisrtChoiceBetter(ManaCostShard s1, ManaCostShard s2 ) {
        return getPayPriority(s1) > getPayPriority(s2);
    }
    
    private int getPayPriority(ManaCostShard s1) {
        if ( s1 == ManaCostShard.COLORLESS ) return 0;

        if( s1.isMonoColor() ) {
            if ( s1.isOr2Colorless() ) return 9;
            if ( !s1.isPhyrexian() ) return 10;
            return 8;
        }
        
        return 5;
    }
    
    private boolean canBePaidWith(ManaCostShard shard, ManaPaid mana) {
        //System.err.println(String.format("ManaPaid: paying for %s with %s" , shard, mana));
        // debug here even more;
        return canBePaidWith(shard, InputPayManaCostUtil.getShortColorString(mana.getColor()) );
    }

    private boolean canBePaidWith(ManaCostShard shard, String mana) {
        // most debug here!!
        String sShard = shard.toString();
        boolean res = "1".equals(sShard) || sShard.contains(mana); 
        //System.out.println(String.format("Str: paying for %s with %s => %d" , shard, mana, res ? 1 : 0));
        return res;
    }    
    
    /**
     * <p>
     * addMana.
     * </p>
     * 
     * @param mana
     *            a {@link forge.card.mana.ManaPaid} object.
     * @return a boolean.
     */
    public final boolean addMana(final ManaPaid mana) {
        if (!this.isNeeded(mana)) {
            throw new RuntimeException("ManaCost : addMana() error, mana not needed - " + mana);
        }

        ManaCostShard choice = null;
        for(ManaCostShard toPay : unpaidShards.keySet())
        {
            if (canBePaidWith(toPay, mana)) {
                // if m is a better to pay than choice
                if (choice == null) {
                    choice = toPay;
                    continue;
                }
                if (isFisrtChoiceBetter(toPay, choice)) {
                    choice = toPay;
                }
            }
        } // for
        if (choice == null) {
            return false;
        }

        decreaseShard(choice, 1);

        if (!mana.isColor(Constant.Color.COLORLESS)) {
            if (this.sunburstMap.containsKey(mana.getColor())) {
                this.sunburstMap.put(mana.getColor(), this.sunburstMap.get(mana.getColor()) + 1);
            } else {
                this.sunburstMap.put(mana.getColor(), 1);
            }
        }
        return true;
    }

    /**
     * <p>
     * combineManaCost.
     * </p>
     * 
     * @param extra
     *            a {@link java.lang.String} object.
     */
    public final void combineManaCost(final String extra) {
        final CardManaCost manaCost = new CardManaCost(new ManaCostParser(extra));
        for(ManaCostShard shard : manaCost.getShards()) {
            Integer cnt = unpaidShards.get(shard);
            unpaidShards.put(shard, cnt == null ? Integer.valueOf(1) : Integer.valueOf(cnt + 1));
        }
        int generic = manaCost.getGenericCost() + this.getColorlessManaAmount();
        if( generic > 0)
            unpaidShards.put(ManaCostShard.COLORLESS, Integer.valueOf(generic));
    }

    /**
     * To string.
     * 
     * @param addX
     *            the add x
     * @return the string
     */
    public final String toString(final boolean addX) {
        // Boolean addX used to add Xs into the returned value
        final StringBuilder sb = new StringBuilder();

        if (addX) {
            for (int i = 0; i < this.getXcounter(); i++) {
                sb.append("X").append(" ");
            }
        }

        int nGeneric = getColorlessManaAmount();
        if( nGeneric > 0 )
            sb.append(nGeneric).append(" ");
            
        for( Entry<ManaCostShard, Integer> s : unpaidShards.entrySet() ) {
            if ( s.getKey() == ManaCostShard.COLORLESS) continue;
            for (int i = 0; i < s.getValue(); i++) {
                sb.append(s.getKey().toString()).append(" ");
            }
        }

        final String str = sb.toString().trim();

        if (str.equals("")) {
            return "0";
        }

        return str;
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return this.toString(true);
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
        
        for (final Entry<ManaCostShard, Integer> s : this.unpaidShards.entrySet()) {
            cmc += s.getKey().getCmc() * s.getValue();
        }
        return cmc;
    }

    /**
     * <p>
     * Getter for the field <code>xcounter</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getXcounter() {
        Integer x = unpaidShards.get(ManaCostShard.X);
        return x == null ? 0 : x;
    }

    /**
     * <p>
     * removeColorlessMana.
     * </p>
     * 
     * @since 1.0.15
     */
    public final void removeColorlessMana() {
        unpaidShards.remove(ManaCostShard.COLORLESS);
    }

    /**
     * Sets the mana needed to avoid negative effect.
     * 
     * @param manaCol
     *            the new mana needed to avoid negative effect
     */
    public final void setManaNeededToAvoidNegativeEffect(final String[] manaCol) {
        for (final String s : manaCol) {
            this.manaNeededToAvoidNegativeEffect.add(s);
        }
    }

    /**
     * Gets the mana needed to avoid negative effect.
     * 
     * @return the mana needed to avoid negative effect
     */
    public final ArrayList<String> getManaNeededToAvoidNegativeEffect() {
        return this.manaNeededToAvoidNegativeEffect;
    }

    /**
     * Gets the mana paid so far to avoid negative effect.
     * 
     * @return the mana paid to avoid negative effect
     */
    public final ArrayList<String> getManaPaidToAvoidNegativeEffect() {
        return this.manaPaidToAvoidNegativeEffect;
    }
}
