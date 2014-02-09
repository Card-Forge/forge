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
import com.google.common.collect.Lists;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.IParserManaCost;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.zone.ZoneType;
import forge.util.maps.EnumMapToAmount;
import forge.util.maps.MapToAmount;

import java.util.*;
import java.util.Map.Entry;

/**
 * <p>
 * ManaCostBeingPaid class.
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
            if (nextShard == ManaCostShard.COLORLESS) {
                return this.hasNext(); // skip colorless
            }
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
    private final MapToAmount<ManaCostShard> unpaidShards;
    private final String sourceRestriction;
    private byte sunburstMap = 0;
    private int cntX = 0;

    /**
     * Copy constructor
     * @param manaCostBeingPaid
     */
    public ManaCostBeingPaid(ManaCostBeingPaid manaCostBeingPaid) {
        unpaidShards = new EnumMapToAmount<ManaCostShard>(manaCostBeingPaid.unpaidShards);
        sourceRestriction = manaCostBeingPaid.sourceRestriction;
        sunburstMap = manaCostBeingPaid.sunburstMap;
        cntX = manaCostBeingPaid.cntX;
    }

    public ManaCostBeingPaid(ManaCost manaCost) {
        this(manaCost, null);
    }

    public ManaCostBeingPaid(ManaCost manaCost, String srcRestriction) {
        unpaidShards = new EnumMapToAmount<ManaCostShard>(ManaCostShard.class);
        sourceRestriction = srcRestriction;
        if (manaCost == null) { return; }
        for (ManaCostShard shard : manaCost) {
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
            if (shard == ManaCostShard.COLORLESS) {
                continue;
            }
            if (shard.isOr2Colorless()) {
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
    public final boolean ai_payMana(final String mana, final ManaPool pool) {
        final byte colorMask = MagicColor.fromName(mana);
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

        return tryPayMana(colorMask, Iterables.filter(unpaidShards.keySet(), predCanBePaid), pool.getPossibleColorUses(colorMask));
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

        byte inColor = mana.getColorCode();
        byte outColor = pool.getPossibleColorUses(inColor);
        return tryPayMana(inColor, Iterables.filter(unpaidShards.keySet(), predCanBePaid), outColor);
    }

    private boolean tryPayMana(final byte colorMask, Iterable<ManaCostShard> payableShards, byte possibleUses) {
        Set<ManaCostShard> choice = EnumSet.noneOf(ManaCostShard.class);
        int priority = Integer.MIN_VALUE;
        for (ManaCostShard toPay : payableShards) {
            // if m is a better to pay than choice
            int toPayPriority = getPayPriority(toPay, possibleUses);
            if (toPayPriority > priority) {
                priority = toPayPriority;
                choice.clear();
            }
            if ( toPayPriority == priority )
                choice.add(toPay);
        } // for
        if (choice.isEmpty()) {
            return false;
        }

        ManaCostShard chosenShard = Iterables.getFirst(choice, null);
        decreaseShard(chosenShard, 1);
        if (chosenShard.isOr2Colorless() && ( 0 == (chosenShard.getColorMask() & possibleUses) )) {
            this.increaseColorlessMana(1);
        }

        this.sunburstMap |= colorMask;
        return true;
    }


    private int getPayPriority(ManaCostShard bill, byte paymentColor) {
        if (bill == ManaCostShard.COLORLESS) {
            return 0;
        }

        if (bill.isMonoColor()) {
            if (bill.isOr2Colorless()) {
                return (bill.getColorMask() & paymentColor & MagicColor.ALL_COLORS) != 0? 9 : 4;
            }
            if (!bill.isPhyrexian()) {
                return 10;
            }
            return 8;
        }
        return 5;
    }

    private boolean canBePaidWith(ManaCostShard shard, Mana mana, ManaPool pool) {
        if (shard.isSnow() && !mana.isSnow()) {
            return false;
        }

        byte color = mana.getColorCode();
        return pool.canPayForShardWithColor(shard, color);
    }

    
    public final void addManaCost(final ManaCost extra) {
        for (ManaCostShard shard : extra) {
            if (shard == ManaCostShard.X) {
                cntX++;
            }
            else {
                increaseShard(shard, 1);
            }
        }
        increaseColorlessMana(extra.getGenericCost());
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
                decreaseColorlessMana(1);
            }
        }
        decreaseColorlessMana(subThisManaCost.getGenericCost());
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
                sb.append("{X}");
            }
        }

        int nGeneric = getColorlessManaAmount();
        if (nGeneric > 0) {
        	if (nGeneric <= 20) {
                sb.append("{" + nGeneric + "}");
        	}
        	else { //if no mana symbol exists for colorless amount, use combination of symbols for each digit
        		String genericStr = String.valueOf(nGeneric);
        		for (int i = 0; i < genericStr.length(); i++) {
        			sb.append("{" + genericStr.charAt(i) + "}");
        		}
        	}
        }

        for (Entry<ManaCostShard, Integer> s : unpaidShards.entrySet()) {
            if (s.getKey() == ManaCostShard.COLORLESS) {
                continue;
            }
            for (int i = 0; i < s.getValue(); i++) {
                sb.append(s.getKey().toString());
            }
        }

        final String str = sb.toString();

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
        for (Entry<ManaCostShard, Integer> kv : unpaidShards.entrySet()) {
           for (int i = kv.getValue().intValue(); i > 0; i--) {
               result.add(kv.getKey());
           }
        }
        for (int i = cntX; i > 0; i--) {
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

    public final void applySpellCostChange(final SpellAbility sa, boolean test) {
        final Game game = sa.getActivatingPlayer().getGame();
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
                final List<Card> mutableGrave = new ArrayList<Card>(pc.getCardsIn(ZoneType.Graveyard));
                final List<Card> toExile = pc.getController().chooseCardsToDelve(this.getColorlessManaAmount(), mutableGrave);
                for (final Card c : toExile) {
                    decreaseColorlessMana(1);
                    if (!test) {
                        pc.getGame().getAction().exile(c);
                    }
                }
            }
            else if (spell.getSourceCard().hasKeyword("Convoke")) {
                adjustCostByConvoke(sa);
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
                }
                else if (stAb.getMapParams().get("Mode").equals("ReduceCost")) {
                    reduceAbilities.add(stAb);
                }
                else if (stAb.getMapParams().get("Mode").equals("SetCost")) {
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
        if (spell.isSpell() && spell.isOffering()) { // cost reduction from offerings
            adjustCostByOffering(sa, spell);
        }

        // Set cost (only used by Trinisphere) is applied last
        for (final StaticAbility stAb : setAbilities) {
            stAb.applyAbility("SetCost", spell, this);
        }
    } // GetSpellCostChange

    private void adjustCostByConvoke(final SpellAbility sa) {

        List<Card> untappedCreats = CardLists.filter(sa.getActivatingPlayer().getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);
        untappedCreats = CardLists.filter(untappedCreats, CardPredicates.Presets.UNTAPPED);

        Map<Card, ManaCostShard> convokedCards = sa.getActivatingPlayer().getController().chooseCardsForConvoke(sa, this.toManaCost(), untappedCreats);
        
        // Convoked creats are tapped here with triggers suppressed,
        // Then again when payment is done(In InputPayManaCost.done()) with suppression cleared.
        // This is to make sure that triggers go off at the right time
        // AND that you can't use mana tapabilities of convoked creatures to pay the convoked cost.
        for (final Entry<Card, ManaCostShard> conv : convokedCards.entrySet()) {
            sa.addTappedForConvoke(conv.getKey());
            this.decreaseShard(conv.getValue(), 1);
            conv.getKey().setTapped(true);
        }
    }

    private void adjustCostByOffering(final SpellAbility sa, final SpellAbility spell) {
        String offeringType = "";
        for (String kw : sa.getSourceCard().getKeyword()) {
            if (kw.endsWith(" offering")) {
                offeringType = kw.split(" ")[0];
                break;
            }
        }

        Card toSac = null;
        List<Card> canOffer = CardLists.filter(spell.getActivatingPlayer().getCardsIn(ZoneType.Battlefield),
                CardPredicates.isType(offeringType));

        final List<Card> toSacList = sa.getSourceCard().getController().getController().choosePermanentsToSacrifice(spell, 0, 1, canOffer,
                offeringType);

        if (!toSacList.isEmpty()) {
            toSac = toSacList.get(0);
        }
        else {
            return;
        }

        subtractManaCost(toSac.getManaCost());

        sa.setSacrificedAsOffering(toSac);
        toSac.setUsedToPay(true); //stop it from interfering with mana input
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
