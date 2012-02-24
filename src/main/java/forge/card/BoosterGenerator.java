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

import net.slightlymagic.braids.util.lambda.Lambda1;
import net.slightlymagic.maxmtg.Closure1;
import net.slightlymagic.maxmtg.Predicate;
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
    public static final Lambda1<List<CardPrinted>, BoosterGenerator> IDENTITY_PICK = new Lambda1<List<CardPrinted>, BoosterGenerator>() {
        @Override
        public List<CardPrinted> apply(final BoosterGenerator arg1) {
            return arg1.getBoosterPack();
        }
    };

    // Closure which will hold both the booster and the way we want to pick from
    // it - holds default options
    /**
     * Gets the simple picker.
     * 
     * @param source
     *            the source
     * @return the simple picker
     */
    public static Closure1<List<CardPrinted>, BoosterGenerator> getSimplePicker(final BoosterGenerator source) {
        return new Closure1<List<CardPrinted>, BoosterGenerator>(BoosterGenerator.IDENTITY_PICK, source);
    }

    // These lists are to hold cards grouped by rarity in advance.

    private final List<CardPrinted> allButLands = new ArrayList<CardPrinted>();

    private final Map<CardRarity, List<CardPrinted>> cardsByRarity = new EnumMap<CardRarity, List<CardPrinted>>(CardRarity.class);
    private final Map<CardRarity, List<CardPrinted>> twoFacedByRarity = new EnumMap<CardRarity, List<CardPrinted>>(CardRarity.class);

    // private List<CardPrinted> commonCreatures;
    // private List<CardPrinted> commonNonCreatures;

    private static final List<CardPrinted> EMPTY_LIST = Collections.unmodifiableList(new ArrayList<CardPrinted>(0));

    // Modern boosters contain 10 commons, 3 uncommmons, 1 rare/mythic
    // They also contain 1 land and 1 token/rules, but we don't pick them now.
    private int numCommons = 10;
    private int numUncommons = 3;
    private int numRareSlots = 1;
    private int numDoubleFaced = 0;
    private int numSpecials = 0;

    private BoosterGenerator() {

        for (CardRarity v : CardRarity.values()) {
            cardsByRarity.put(v, new ArrayList<CardPrinted>());
            twoFacedByRarity.put(v, new ArrayList<CardPrinted>());
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
    }

    /**
     * <p>
     * Constructor for BoosterGenerator.
     * </p>
     * 
     * @param cardSet
     *            the card set
     */
    public BoosterGenerator(BoosterData booster) {
        this();

        this.numCommons = booster.getCommon();
        this.numUncommons = booster.getUncommon();
        this.numRareSlots = booster.getRare();
        this.numSpecials = booster.getSpecial();
        this.numDoubleFaced = booster.getDoubleFaced();

        final Predicate<CardPrinted> filter = booster.getEditionFilter();
        final List<CardPrinted> cardsInThisSet = filter.select(CardDb.instance().getAllCards());

        for (final CardPrinted c : cardsInThisSet) {
            this.addToRarity(c);
            // System.out.println(c);
        }
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
     * Gets the booster pack.
     * 
     * @return the booster pack
     */
    public final List<CardPrinted> getBoosterPack() {
        return this.getBoosterPack(this.numCommons, this.numUncommons, this.numRareSlots, 0, 0, this.numSpecials,
                this.numDoubleFaced, 0, 0);
    }

    /**
     * Gets the singleton booster pack.
     * 
     * @param nAnyCard
     *            the n any card
     * @return the singleton booster pack
     */
    public final List<CardPrinted> getSingletonBoosterPack(final int nAnyCard) {
        final List<CardPrinted> temp = new ArrayList<CardPrinted>();

        temp.addAll(this.pickRandomCards(this.allButLands, nAnyCard, true));

        return temp;
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

        temp.addAll(this.pickRandomCards(cardsByRarity.get(CardRarity.Common), nCom));
        temp.addAll(this.pickRandomCards(cardsByRarity.get(CardRarity.Uncommon), nUnc));

        if (nRareSlots > 0) {
            temp.addAll(this.pickRandomRaresOrMythics(cardsByRarity.get(CardRarity.Rare),
                    cardsByRarity.get(CardRarity.MythicRare), nRareSlots));
        }
        if ((nRares > 0) || (nMythics > 0)) {
            temp.addAll(this.pickRandomCards(cardsByRarity.get(CardRarity.Rare), nRares));
            temp.addAll(this.pickRandomCards(cardsByRarity.get(CardRarity.MythicRare), nMythics));
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

        temp.addAll(this.pickRandomCards(cardsByRarity.get(CardRarity.Special), nSpecs));

        temp.addAll(this.pickRandomCards(this.allButLands, nAnyCard));

        temp.addAll(this.pickRandomCards(cardsByRarity.get(CardRarity.BasicLand), nLands));

        return temp;
    }

    private void addToRarity(final CardPrinted c) {
        if (c.getCard().isAltState()) {
            return;
        }

        CardRarity rarity = c.getRarity();
        if (c.getCard().isDoubleFaced() && (this.numDoubleFaced > 0)) {
            twoFacedByRarity.get(rarity).add(c);
        } else {
            cardsByRarity.get(rarity).add(c);
        }

        if (!c.getCard().getType().isBasicLand()) {
            this.allButLands.add(c);
        }
    }

}
