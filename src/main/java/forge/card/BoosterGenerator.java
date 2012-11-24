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
package forge.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPoolView;
import forge.util.MyRandom;

/**
 * <p>
 * BoosterGenerator class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class BoosterGenerator {
    private static final int BOOSTERS_TO_FIND_MYTHIC = 8;

    // Function to open a booster as it is.
    /** The Constant IDENTITY_PICK. */
    public static final Function<BoosterGenerator, List<CardPrinted>> IDENTITY_PICK = new Function<BoosterGenerator, List<CardPrinted>>() {
        @Override
        public List<CardPrinted> apply(final BoosterGenerator arg1) {
            return arg1.getBoosterPack(10, 3, 1, 0, 0, 0, 0, 0, 1);
        }
    };

    // These lists are to hold cards grouped by rarity in advance.

    private final List<CardPrinted> allButLands = new ArrayList<CardPrinted>();

    private final Map<CardRarity, List<CardPrinted>> cardsByRarity = new EnumMap<CardRarity, List<CardPrinted>>(CardRarity.class);
    private final Map<CardRarity, List<CardPrinted>> twoFacedByRarity = new EnumMap<CardRarity, List<CardPrinted>>(CardRarity.class);
    private final Map<CardRarity, List<CardPrinted>> singleFacedByRarity = new EnumMap<CardRarity, List<CardPrinted>>(CardRarity.class);

    // private List<CardPrinted> commonCreatures;
    // private List<CardPrinted> commonNonCreatures;

    private static final List<CardPrinted> EMPTY_LIST = Collections.unmodifiableList(new ArrayList<CardPrinted>(0));

    private BoosterGenerator() {
        for (CardRarity v : CardRarity.values()) {
            twoFacedByRarity.put(v, new ArrayList<CardPrinted>());
            singleFacedByRarity.put(v, new ArrayList<CardPrinted>());
        }
    }

    private void mergeAllFacedCards() {
        for (CardRarity v : CardRarity.values()) {
            List<CardPrinted> cp = new ArrayList<CardPrinted>(singleFacedByRarity.get(v));
            cp.addAll(twoFacedByRarity.get(v));
            cardsByRarity.put(v, cp);
        }
    }

    /**
     * <p>
     * Constructor for BoosterGenerator.
     * </p>
     * 
     * @param cards
     *            the cards
     */
    public BoosterGenerator(final Iterable<CardPrinted> cards) {
        this();
        for (final CardPrinted c : cards) {
            this.addToRarity(c);
        }
        mergeAllFacedCards();
    }

    /**
     * Instantiates a new booster generator.
     * 
     * @param dPool
     *            the d pool
     */
    public BoosterGenerator(final ItemPoolView<CardPrinted> dPool) {
        this();
        for (final Entry<CardPrinted, Integer> e : dPool) {
            this.addToRarity(e.getKey());
        }
        mergeAllFacedCards();
    }

    /**
     * <p>
     * Constructor for BoosterGenerator.
     * </p>
     * 
     * @param filter
     *            the card set
     */
    public BoosterGenerator(Predicate<CardPrinted> filter) {
        this();

        for (final CardPrinted c : Iterables.filter(CardDb.instance().getTraditionalCards(), filter)) {
            this.addToRarity(c);
            // System.out.println(c);
        }
        mergeAllFacedCards();
        // System.out.println("done");
    }

    private List<CardPrinted> pickRandomCards(final List<CardPrinted> source, final int count) {
        return this.pickRandomCards(source, count, false);
    }

    private List<CardPrinted> pickRandomCards(final List<CardPrinted> source, final int count, final boolean singleton) {
        int listSize = source == null ? 0 : source.size();
        if ((count <= 0) || (listSize == 0)) {
            return BoosterGenerator.EMPTY_LIST;
        }
        final List<CardPrinted> result = new ArrayList<CardPrinted>(count);

        int index = Integer.MAX_VALUE;
        for (int iCard = 0; iCard < count; iCard++) {
            if (index >= listSize) {
                Collections.shuffle(source, MyRandom.getRandom());
                index = 0;
            }
            result.add(source.get(index));

            if (!singleton) {
                index++;
            } else {
                source.remove(index);
                listSize--;
            }
        }
        return result;
    }

    private List<CardPrinted> pickRandomRaresOrMythics(final List<CardPrinted> rares, final List<CardPrinted> mythics,
            final int count) {
        final int raresSize = rares == null ? 0 : rares.size();
        final int mythicsSize = mythics == null ? 0 : mythics.size();
        if ((count <= 0) || (raresSize == 0)) {
            return BoosterGenerator.EMPTY_LIST;
        }

        final List<CardPrinted> result = new ArrayList<CardPrinted>(count);

        int indexRares = Integer.MAX_VALUE;
        int indexMythics = Integer.MAX_VALUE;
        for (int iCard = 0; iCard < count; iCard++) {
            final int rollD8 = MyRandom.getRandom().nextInt(BOOSTERS_TO_FIND_MYTHIC);
            final boolean takeMythic = (mythicsSize > 0) && (rollD8 < 1);
            if (takeMythic) {
                if (indexMythics >= mythicsSize) {
                    Collections.shuffle(mythics, MyRandom.getRandom());
                    indexMythics = 0;
                }
                result.add(mythics.get(indexMythics));
                indexMythics++;
            } else {
                if (indexRares >= raresSize) {
                    Collections.shuffle(rares, MyRandom.getRandom());
                    indexRares = 0;
                }
                result.add(rares.get(indexRares));
                indexRares++;
            }
        }
        return result;
    }

    /**
     * Gets the singleton booster pack.
     * Singleton means that every card in every booster in whole draft is unique!
     * First arg in pickRandomCards can't be copy, because picker must remove card from pool to ensure uniqueness.
     * 
     * @param nAnyCard
     *            the n any card
     * @return the singleton booster pack
     */
    public final List<CardPrinted> getSingletonBoosterPack(final int nAnyCard) {
        return this.pickRandomCards(allButLands, nAnyCard, true);
    }

    /**
     * Gets the booster pack.
     * 
     * @return the booster pack
     */
    public final List<CardPrinted> getBoosterPack(BoosterData booster) {
        return this.getBoosterPack(booster.getCommon(), booster.getUncommon(), booster.getRare(), 0, 0, booster.getSpecial(),
                booster.getDoubleFaced(), 0, booster.getLand());
    }

    /**
     * Gets the booster pack.
     *
     * @param numbers the numbers
     * @param nRareSlots the n rare slots
     * @param nDoubls the n doubls
     * @param nAnyCard the n any card
     * @return the booster pack
     */
    public final List<CardPrinted> getBoosterPack(final Map<CardRarity, Integer> numbers,
            final int nRareSlots, final int nDoubls, final int nAnyCard) {
        return getBoosterPack(numbers.get(CardRarity.Common), numbers.get(CardRarity.Uncommon), nRareSlots,
                numbers.get(CardRarity.Rare), numbers.get(CardRarity.MythicRare), numbers.get(CardRarity.Special),
                nDoubls, nAnyCard, numbers.get(CardRarity.BasicLand));
    }


    /**
     * So many parameters are needed for custom limited cardpools,.
     * 
     * @param nCom
     *            the n com
     * @param nUnc
     *            the n unc
     * @param nRareSlots
     *            the n rare slots
     * @param nRares
     *            the n rares
     * @param nMythics
     *            the n mythics
     * @param nSpecs
     *            the n specs
     * @param nDoubls
     *            the n doubls
     * @param nAnyCard
     *            the n any card
     * @param nLands
     *            the n lands
     * @return the booster pack
     */
    public final List<CardPrinted> getBoosterPack(final int nCom, final int nUnc, final int nRareSlots,
            final int nRares, final int nMythics, final int nSpecs, final int nDoubls, final int nAnyCard,
            final int nLands) {

        final List<CardPrinted> temp = new ArrayList<CardPrinted>();
        final Map<CardRarity, List<CardPrinted>> commonCardsMap = nDoubls != 0 ? singleFacedByRarity : cardsByRarity;

        temp.addAll(this.pickRandomCards(commonCardsMap.get(CardRarity.Common), nCom));
        temp.addAll(this.pickRandomCards(commonCardsMap.get(CardRarity.Uncommon), nUnc));

        if (nRareSlots > 0) {
            temp.addAll(this.pickRandomRaresOrMythics(commonCardsMap.get(CardRarity.Rare),
                    cardsByRarity.get(CardRarity.MythicRare), nRareSlots));
        }
        if ((nRares > 0) || (nMythics > 0)) {
            if (nMythics == 0) {
                temp.addAll(this.pickRandomRaresOrMythics(commonCardsMap.get(CardRarity.Rare),
                        cardsByRarity.get(CardRarity.MythicRare), nRares));
            } else {
                temp.addAll(this.pickRandomCards(commonCardsMap.get(CardRarity.Rare), nRares));
                temp.addAll(this.pickRandomCards(commonCardsMap.get(CardRarity.MythicRare), nMythics));
            }
        }
        if (nDoubls > 0) {
            final int dblFacedRarity = MyRandom.getRandom().nextInt(nCom + nUnc + nRareSlots);
            CardRarity rarityInSlot = CardRarity.MythicRare;
            if (dblFacedRarity < nCom) {
                rarityInSlot = CardRarity.Common;
            } else if (dblFacedRarity < nCom + nUnc) {
                rarityInSlot = CardRarity.Uncommon;
            } else if (MyRandom.getRandom().nextInt(BOOSTERS_TO_FIND_MYTHIC) != 0) {
                rarityInSlot = CardRarity.Rare;
            }

            temp.addAll(this.pickRandomCards(twoFacedByRarity.get(rarityInSlot), nDoubls));
        }

        temp.addAll(this.pickRandomCards(commonCardsMap.get(CardRarity.Special), nSpecs));
        temp.addAll(this.pickRandomCards(this.allButLands, nAnyCard));
        temp.addAll(this.pickRandomCards(commonCardsMap.get(CardRarity.BasicLand), nLands));

        return temp;
    }

    private void addToRarity(final CardPrinted c) {
        if (c.getCard().isAltState()) {
            return;
        }

        Map<CardRarity, List<CardPrinted>> targetList = c.getCard().isDoubleFaced() ? twoFacedByRarity : singleFacedByRarity;
        targetList.get(c.getRarity()).add(c);

        if (!c.getCard().getType().isBasicLand()) {
            this.allButLands.add(c);
        }
    }

}
