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
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardUtil;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.spellability.SpellAbility;
import forge.card.staticability.StaticAbility;
import forge.game.GameState;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.util.TextUtil;
import forge.util.maps.EnumMapToAmount;
import forge.util.maps.MapToAmount;

/**
 * <p>
 * ManaCost class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ManaCostBeingPaid {
    private class ManaCostBeingPaidIterator implements IParserManaCost, Iterator<ManaCostShard> {
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
            if (remainingShards == 0)
                throw new UnsupportedOperationException("All shards were depleted, call hasNext()");
            remainingShards--;
            return nextShard;
        }
        
        @Override
        public boolean hasNext() {
            if ( remainingShards > 0 ) return true;
            if ( !hasSentX ) {
                if ( nextShard != ManaCostShard.X && cntX > 0) {
                    nextShard = ManaCostShard.X;
                    remainingShards = cntX;
                    return true;
                } else 
                    hasSentX = true;
            }
            if ( !mch.hasNext() ) return false;
            
            nextShard = mch.next();
            if ( nextShard == ManaCostShard.COLORLESS )
                return this.hasNext(); // skip colorless
            remainingShards = unpaidShards.get(nextShard);
            
            return true;
        }
        
        @Override
        public int getTotalColorlessCost() {
            Integer c = unpaidShards.get(ManaCostShard.COLORLESS);
            return c == null ? 0 : c.intValue();
        }
    }

    // holds Mana_Part objects
    // ManaPartColor is stored before ManaPartColorless
    private final MapToAmount<ManaCostShard> unpaidShards = new EnumMapToAmount<ManaCostShard>(ManaCostShard.class);
    private byte sunburstMap = 0;
    private int cntX = 0;
    private final String sourceRestriction;
    
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
    public ManaCostBeingPaid(String sCost) {
        this("0".equals(sCost) || "C".equals(sCost) || sCost.isEmpty() ? ManaCost.ZERO : new ManaCost(new ManaCostParser(sCost)));
    }

    public ManaCostBeingPaid(ManaCost manaCost) {
        this(manaCost, null);
    }
    public ManaCostBeingPaid(ManaCost manaCost, String srcRestriction) {
        sourceRestriction = srcRestriction;
        if( manaCost == null ) return;
        for (ManaCostShard shard : manaCost.getShards()) {
            if (shard == ManaCostShard.X) {
                cntX++;
            } else {
                unpaidShards.add(shard);
            }
        }
        increaseColorlessMana(manaCost.getGenericCost());
    }

    /**
     * <p>
     * getSunburst.
     * </p>
     * 
     * @return a int.
     */
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
        for(ManaCostShard mcs : unpaidShards.keySet()) {
            if( mcs.isPhyrexian() ) {
                phy = mcs;
                break;
            }
        }

        if (phy == null )
            return false;

        decreaseShard(phy, 1);
        return true;
    }
    

    // takes a Short Color and returns true if it exists in the mana cost.
    // Easier for split costs
    public final boolean needsColor(final byte colorMask) {
        for (ManaCostShard shard : unpaidShards.keySet()) {
            if (shard == ManaCostShard.COLORLESS)
                continue;
            if (shard.isOr2Colorless()) {
                if ((shard.getColorMask() & colorMask) != 0 )
                    return true;
            } else if (shard.canBePaidWithManaOfColor(colorMask))
                return true;
        }
        return false;
    }

    // isNeeded(String) still used by the Computer, might have problems activating Snow abilities
    public final boolean isAnyPartPayableWith(byte colorMask) {
        for (ManaCostShard shard : unpaidShards.keySet()) {
            if (shard.canBePaidWithManaOfColor(colorMask)) {
                return true;
            }
        }
        return false;
    }

    public final boolean isNeeded(final Mana paid) {
        for (ManaCostShard shard : unpaidShards.keySet()) {
            if (canBePaidWith(shard, paid)) {
                return true;
            }
        }
        return false;
    }


    public final boolean isPaid() {
        return unpaidShards.isEmpty();
    } // isPaid()

    /**
     * <p>
     * payMultipleMana.
     * </p>
     * 
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final String payMultipleMana(String mana) {
        List<String> unused = new ArrayList<>(4);
        for (String manaPart : TextUtil.split(mana, ' ')) {
            if (StringUtils.isNumeric(manaPart)) {
                for(int i = Integer.parseInt(manaPart); i > 0; i--) {
                    boolean wasNeeded = this.payMana("1");
                    if(!wasNeeded) {
                        unused.add(Integer.toString(i));
                        break;
                    }
                }
            } else {
                String color = MagicColor.toShortString(manaPart);
                boolean wasNeeded = this.payMana(color);
                if(!wasNeeded)
                    unused.add(color);
            }
        }
        return unused.isEmpty() ? null : StringUtils.join(unused, ' ');
    }

    public final void increaseColorlessMana(final int manaToAdd) {
        increaseShard(ManaCostShard.COLORLESS, manaToAdd);
    }

    public final void increaseShard(final ManaCostShard shard, final int toAdd) {
        unpaidShards.add(shard, toAdd);
    }

    public final void decreaseColorlessMana(final int manaToSubtract) {
        decreaseShard(ManaCostShard.COLORLESS, manaToSubtract);
    }

    public final void decreaseShard(final ManaCostShard shard, final int manaToSubtract) {
        if (manaToSubtract <= 0) {
            return;
        }

        if (!unpaidShards.containsKey(shard)) {
            System.err.println("Tried to substract a " + shard.toString() + " shard that is not present in this ManaCostBeingPaid");
            return;
        }
        unpaidShards.substract(shard, manaToSubtract);
    }

    public final int getColorlessManaAmount() {
        return unpaidShards.count(ManaCostShard.COLORLESS);
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
    public final boolean payMana(final String mana) {
        final byte colorMask = MagicColor.fromName(mana);
        if (!this.isAnyPartPayableWith(colorMask)) {
            //System.out.println("ManaCost : addMana() error, mana not needed - " + mana);
            return false;
            //throw new RuntimeException("ManaCost : addMana() error, mana not needed - " + mana);
        }

        Predicate<ManaCostShard> predCanBePaid = new Predicate<ManaCostShard>() {
            @Override public boolean apply(ManaCostShard ms) { 
                return ms.canBePaidWithManaOfColor(colorMask);
            }
        };

        return tryPayMana(colorMask, Iterables.filter(unpaidShards.keySet(), predCanBePaid));
    }

    /**
     * <p>
     * addMana.
     * </p>
     * 
     * @param mana
     *            a {@link forge.card.mana.Mana} object.
     * @return a boolean.
     */
    public final boolean payMana(final Mana mana) {
        if (!this.isNeeded(mana)) {
            throw new RuntimeException("ManaCost : addMana() error, mana not needed - " + mana);
        }

        Predicate<ManaCostShard> predCanBePaid = new Predicate<ManaCostShard>() {
            @Override public boolean apply(ManaCostShard ms) { 
                return canBePaidWith(ms, mana);
            }
        };
        
        return tryPayMana(mana.getColorCode(), Iterables.filter(unpaidShards.keySet(), predCanBePaid));
    }

    private boolean tryPayMana(final byte colorMask, Iterable<ManaCostShard> payableShards) {
        ManaCostShard choice = null;
        for (ManaCostShard toPay : payableShards) {
            // if m is a better to pay than choice
            if (choice == null) {
                choice = toPay;
                continue;
            }
            if (isFirstChoiceBetter(toPay, choice, colorMask)) {
                choice = toPay;
            }
        } // for
        if (choice == null) {
            return false;
        }
    
        decreaseShard(choice, 1);
        if (choice.isOr2Colorless() && choice.getColorMask() != colorMask ) {
            this.increaseColorlessMana(1);
        }
    
        this.sunburstMap |= colorMask;
        return true;
    }

    private boolean isFirstChoiceBetter(ManaCostShard s1, ManaCostShard s2, byte colorMask) {
        return getPayPriority(s1, colorMask) > getPayPriority(s2, colorMask);
    }

    private int getPayPriority(ManaCostShard bill, byte paymentColor) {
        if (bill == ManaCostShard.COLORLESS) {
            return 0;
        }

        if (bill.isMonoColor()) {
            if (bill.isOr2Colorless()) {
                return bill.getColorMask() == paymentColor ? 9 : 4;
            }
            if (!bill.isPhyrexian()) {
                return 10;
            }
            return 8;
        }
        return 5;
    }

    private boolean canBePaidWith(ManaCostShard shard, Mana mana) {
        if (shard.isSnow() && !mana.isSnow()) {
            return false;
        }
        
        byte color = mana.getColorCode();
        return shard.canBePaidWithManaOfColor(color);
    }

    public final void combineManaCost(final ManaCost extra) {
        for (ManaCostShard shard : extra.getShards()) {
            if (shard == ManaCostShard.X) {
                cntX++;
            } else {
                increaseShard(shard, 1);
            }
        }
        increaseColorlessMana(extra.getGenericCost());
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
        if (nGeneric > 0) {
            sb.append(nGeneric).append(" ");
        }

        for (Entry<ManaCostShard, Integer> s : unpaidShards.entrySet()) {
            if (s.getKey() == ManaCostShard.COLORLESS) {
                continue;
            }
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
    
    public ManaCost toManaCost() {
        return new ManaCost(new ManaCostBeingPaidIterator());
    }

    public final int getXcounter() {
        return cntX;
    }

    
    public final List<ManaCostShard> getUnpaidShards() { 
        List<ManaCostShard> result = new ArrayList<ManaCostShard>();
        for(Entry<ManaCostShard, Integer> kv : unpaidShards.entrySet()) {
           for(int i = kv.getValue().intValue(); i > 0; i--) {
               result.add(kv.getKey());
           }
        }
        for(int i = cntX; i > 0; i--) {
            result.add(ManaCostShard.X);
        }
        return result;
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

    public final void applySpellCostChange(final SpellAbility sa) {
        final GameState game = sa.getActivatingPlayer().getGame();
        // Beached
        final Card originalCard = sa.getSourceCard();
        final SpellAbility spell = sa;


        if (sa.isXCost() && !originalCard.isCopiedSpell()) {
            originalCard.setXManaCostPaid(0);
        }

        if (sa.isTrigger()) {
            return;
        }

        if (spell.isSpell()) {
            if (spell.isDelve()) {
                final Player pc = originalCard.getController();
                final List<Card> mutableGrave = Lists.newArrayList(pc.getZone(ZoneType.Graveyard).getCards());
                final List<Card> toExile = pc.getController().chooseCardsToDelve(this.getColorlessManaAmount(), mutableGrave);
                for (final Card c : toExile) {
                    pc.getGame().getAction().exile(c);
                    decreaseColorlessMana(1);
                }
            } else if (spell.getSourceCard().hasKeyword("Convoke")) {
                adjustCostByConvoke(sa, spell);
            }
        } // isSpell

        List<Card> cardsOnBattlefield = Lists.newArrayList(game.getCardsIn(ZoneType.Battlefield));
        cardsOnBattlefield.addAll(game.getCardsIn(ZoneType.Stack));
        cardsOnBattlefield.addAll(game.getCardsIn(ZoneType.Command));
        if (!cardsOnBattlefield.contains(originalCard)) {
            cardsOnBattlefield.add(originalCard);
        }
        final ArrayList<StaticAbility> raiseAbilities = new ArrayList<StaticAbility>();
        final ArrayList<StaticAbility> reduceAbilities = new ArrayList<StaticAbility>();
        final ArrayList<StaticAbility> setAbilities = new ArrayList<StaticAbility>();

        // Sort abilities to apply them in proper order
        for (Card c : cardsOnBattlefield) {
            final ArrayList<StaticAbility> staticAbilities = c.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                if (stAb.getMapParams().get("Mode").equals("RaiseCost")) {
                    raiseAbilities.add(stAb);
                } else if (stAb.getMapParams().get("Mode").equals("ReduceCost")) {
                    reduceAbilities.add(stAb);
                } else if (stAb.getMapParams().get("Mode").equals("SetCost")) {
                    setAbilities.add(stAb);
                }
            }
        }
        // Raise cost
        for (final StaticAbility stAb : raiseAbilities) {
            stAb.applyAbility("RaiseCost", spell, this);
        }

        // Reduce cost
        for (final StaticAbility stAb : reduceAbilities) {
            stAb.applyAbility("ReduceCost", spell, this);
        }

        // Set cost (only used by Trinisphere) is applied last
        for (final StaticAbility stAb : setAbilities) {
            stAb.applyAbility("SetCost", spell, this);
        }
    } // GetSpellCostChange

    private void adjustCostByConvoke(final SpellAbility sa, final SpellAbility spell) {
        
        List<Card> untappedCreats = CardLists.filter(spell.getActivatingPlayer().getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);
        untappedCreats = CardLists.filter(untappedCreats, CardPredicates.Presets.UNTAPPED);

        while (!untappedCreats.isEmpty() && getConvertedManaCost() > 0) {
            Card workingCard = null;
            String chosenColor = null;
            if (sa.getActivatingPlayer().isHuman()) {
                workingCard = GuiChoose.oneOrNone("Tap for Convoke? " + toString(), untappedCreats);
                if( null == workingCard )
                    break; // that means "I'm done"

                List<String> usableColors = getConvokableColors(workingCard);
                if ( !usableColors.isEmpty() ) {
                    chosenColor = usableColors.size() == 1 ? usableColors.get(0) : GuiChoose.one("Convoke for which color?", usableColors);
                } 
            } else {
                // TODO: AI to choose a creature to tap would go here
                // Probably along with deciding how many creatures to tap
                break;

            }
            untappedCreats.remove(workingCard);


            if ( null == chosenColor )
                continue;
            else if (chosenColor.equals("colorless")) {
                decreaseColorlessMana(1);
            } else {
                decreaseShard(ManaCostShard.valueOf(MagicColor.fromName(chosenColor)), 1);
            }

            sa.addTappedForConvoke(workingCard);
        }

        // Convoked creats are tapped here with triggers
        // suppressed,
        // Then again when payment is done(In
        // InputPayManaCost.done()) with suppression cleared.
        // This is to make sure that triggers go off at the
        // right time
        // AND that you can't use mana tapabilities of convoked
        // creatures
        // to pay the convoked cost.
        for (final Card c : sa.getTappedForConvoke()) {
            c.setTapped(true);
        }

    }

    /**
     * Gets the convokable colors.
     * 
     * @param cardToConvoke
     *            the card to convoke
     * @param cost
     *            the cost
     * @return the convokable colors
     */
    private List<String> getConvokableColors(final Card cardToConvoke) {
        final ArrayList<String> usableColors = new ArrayList<String>();
    
        if (getColorlessManaAmount() > 0) {
            usableColors.add("colorless");
        }
        ColorSet cs = CardUtil.getColors(cardToConvoke);
        for(byte color : MagicColor.WUBRG) {
            if( cs.hasAnyColor(color))
                usableColors.add(MagicColor.toLongString(color));
        }
        return usableColors;
    }

    public String getSourceRestriction() {
        return sourceRestriction;
    }

    public Iterable<ManaCostShard> getDistinctShards() {
        return unpaidShards.keySet();
    }

    public int getUnpaidShards(ManaCostShard key) {
        return unpaidShards.count(key);
    }
}
