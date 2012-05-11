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
package forge.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



import forge.card.BoosterGenerator;
import forge.card.CardRules;
import forge.card.UnOpenedProduct;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.util.MyRandom;
import forge.util.closures.Lambda1;
import forge.util.closures.Predicate;

// The BoosterPack generates cards for the Card Pool in Quest Mode
/**
 * <p>
 * QuestBoosterPack class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class BoosterUtils {

    /**
     * Gets the quest starter deck.
     * 
     * @param filter
     *            the filter
     * @param numCommon
     *            the num common
     * @param numUncommon
     *            the num uncommon
     * @param numRare
     *            the num rare
     * @return the quest starter deck
     */
    public static List<CardPrinted> getQuestStarterDeck(final Predicate<CardPrinted> filter, final int numCommon,
            final int numUncommon, final int numRare) {
        final ArrayList<CardPrinted> cards = new ArrayList<CardPrinted>();

        // Each color should have around the same amount of monocolored cards
        // There should be 3 Colorless cards for every 4 cards in a single color
        // There should be 1 Multicolor card for every 4 cards in a single color

        final List<Predicate<CardRules>> colorFilters = new ArrayList<Predicate<CardRules>>();
        colorFilters.add(CardRules.Predicates.Presets.IS_MULTICOLOR);

        for (int i = 0; i < 4; i++) {
            if (i != 2) {
                colorFilters.add(CardRules.Predicates.Presets.IS_COLORLESS);
            }

            colorFilters.add(CardRules.Predicates.Presets.IS_WHITE);
            colorFilters.add(CardRules.Predicates.Presets.IS_RED);
            colorFilters.add(CardRules.Predicates.Presets.IS_BLUE);
            colorFilters.add(CardRules.Predicates.Presets.IS_BLACK);
            colorFilters.add(CardRules.Predicates.Presets.IS_GREEN);
        }

        final Iterable<CardPrinted> cardpool = CardDb.instance().getAllUniqueCards();

        cards.addAll(BoosterUtils.generateDefinetlyColouredCards(cardpool,
                Predicate.and(filter, CardPrinted.Predicates.Presets.IS_COMMON), numCommon, colorFilters));
        cards.addAll(BoosterUtils.generateDefinetlyColouredCards(cardpool,
                Predicate.and(filter, CardPrinted.Predicates.Presets.IS_UNCOMMON), numUncommon, colorFilters));

        int nRares = numRare, nMythics = 0;
        final Predicate<CardPrinted> filterMythics = Predicate.and(filter,
                CardPrinted.Predicates.Presets.IS_MYTHIC_RARE);
        final boolean haveMythics = filterMythics.any(cardpool);
        for (int iSlot = 0; haveMythics && (iSlot < numRare); iSlot++) {
            if (MyRandom.getRandom().nextInt(10) < 1) {
                // 10% chance of upgrading a Rare into a Mythic
                nRares--;
                nMythics++;
            }
        }

        cards.addAll(BoosterUtils.generateDefinetlyColouredCards(cardpool,
                Predicate.and(filter, CardPrinted.Predicates.Presets.IS_RARE), nRares, colorFilters));
        if (nMythics > 0) {
            cards.addAll(BoosterUtils.generateDefinetlyColouredCards(cardpool, filterMythics, nMythics, colorFilters));
        }
        return cards;
    }

    /**
     * Create the list of card names at random from the given pool.
     * 
     * @param source
     *            an Iterable<CardPrinted>
     * @param filter
     *            Predicate<CardPrinted>
     * @param cntNeeded
     *            an int
     * @param allowedColors
     *            a List<Predicate<CardRules>>
     * @return a list of card names
     */
    private static ArrayList<CardPrinted> generateDefinetlyColouredCards(final Iterable<CardPrinted> source,
            final Predicate<CardPrinted> filter, final int cntNeeded, final List<Predicate<CardRules>> allowedColors) {
        // If color is null, use colorOrder progression to grab cards
        final ArrayList<CardPrinted> result = new ArrayList<CardPrinted>();

        final int size = allowedColors == null ? 0 : allowedColors.size();
        Collections.shuffle(allowedColors);

        int cntMade = 0, iAttempt = 0;

        // This will prevent endless loop @ wh
        int allowedMisses = (2 + size + 2) * cntNeeded; // lol, 2+2 is not magic
                                                        // constant!

        while ((cntMade < cntNeeded) && (allowedMisses > 0)) {
            CardPrinted card = null;

            if (size > 0) {
                final Predicate<CardRules> color2 = allowedColors.get(iAttempt % size);
                if (color2 != null) {
                    card = Predicate.and(filter, color2, CardPrinted.FN_GET_RULES).random(source);
                }
            }

            if (card == null) {
                // We can't decide on a color, so just pick a card.
                card = filter.random(source);
            }

            if ((card != null) && !result.contains(card)) {
                result.add(card);
                cntMade++;
            } else {
                allowedMisses--;
            }
            iAttempt++;
        }

        return result;
    }

    /**
     * Generate distinct cards.
     * 
     * @param filter
     *            the filter
     * @param cntNeeded
     *            the cnt needed
     * @return the list
     */
    public static List<CardPrinted> generateDistinctCards(final Predicate<CardPrinted> filter, final int cntNeeded) {
        return BoosterUtils.generateDistinctCards(CardDb.instance().getAllCards(), filter, cntNeeded);
    }

    /**
     * Generate distinct cards.
     * 
     * @param source
     *            the source
     * @param filter
     *            the filter
     * @param cntNeeded
     *            the cnt needed
     * @return the list
     */
    public static List<CardPrinted> generateDistinctCards(final Iterable<CardPrinted> source,
            final Predicate<CardPrinted> filter, final int cntNeeded) {
        final ArrayList<CardPrinted> result = new ArrayList<CardPrinted>();
        int cntMade = 0;

        // This will prevent endless loop @ while
        int allowedMisses = (2 + 2) * cntNeeded; // lol, 2+2 is not magic
                                                 // constant!

        while ((cntMade < cntNeeded) && (allowedMisses > 0)) {
            final CardPrinted card = filter.random(source);

            if ((card != null) && !result.contains(card)) {
                result.add(card);
                cntMade++;
            } else {
                allowedMisses--;
            }
        }

        return result;
    }

    /**
     * <p>
     * generateCardRewardList.
     * </p>
     * Takes a reward list string, parses, and returns list of cards rewarded.
     * 
     * @param s
     *            Properties string of reward (97 multicolor rares)
     * @return CardList
     */
    public static UnOpenedProduct generateCardRewardList(final String s) {
        final String[] temp = s.split(" ");

        final int qty = Integer.parseInt(temp[0]);
        // Determine rarity
        Predicate<CardPrinted> rar = CardPrinted.Predicates.Presets.IS_UNCOMMON;
        if (temp[2].equalsIgnoreCase("rare") || temp[2].equalsIgnoreCase("rares")) {
            rar = CardPrinted.Predicates.Presets.IS_RARE_OR_MYTHIC;
        }

        // Determine color ("random" defaults to null color)
        Predicate<CardRules> col = Predicate.getTrue(CardRules.class);
        if (temp[1].equalsIgnoreCase("black")) {
            col = CardRules.Predicates.Presets.IS_BLACK;
        } else if (temp[1].equalsIgnoreCase("blue")) {
            col = CardRules.Predicates.Presets.IS_BLUE;
        } else if (temp[1].equalsIgnoreCase("colorless")) {
            col = CardRules.Predicates.Presets.IS_COLORLESS;
        } else if (temp[1].equalsIgnoreCase("green")) {
            col = CardRules.Predicates.Presets.IS_GREEN;
        } else if (temp[1].equalsIgnoreCase("multicolor")) {
            col = CardRules.Predicates.Presets.IS_MULTICOLOR;
        } else if (temp[1].equalsIgnoreCase("red")) {
            col = CardRules.Predicates.Presets.IS_RED;
        } else if (temp[1].equalsIgnoreCase("white")) {
            col = CardRules.Predicates.Presets.IS_WHITE;
        }

        Lambda1<List<CardPrinted>, BoosterGenerator> openWay = new Lambda1<List<CardPrinted>, BoosterGenerator>() {
            @Override
            public List<CardPrinted> apply(BoosterGenerator arg1) {
                return arg1.getSingletonBoosterPack(qty);
            }
        };
        return new UnOpenedProduct(openWay, new BoosterGenerator(Predicate.and(rar, col, CardPrinted.FN_GET_RULES))); // qty))
    }
}
