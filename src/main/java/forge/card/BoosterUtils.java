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
import java.util.List;

import net.slightlymagic.maxmtg.Predicate;

import org.apache.commons.lang3.StringUtils;

import forge.Constant;
import forge.MyRandom;
import forge.item.CardDb;
import forge.item.CardPrinted;

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
            if (MyRandom.getRandom().nextInt(7) < 1) { // a bit higher chance to
                                                       // get
                // a mythic
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

    // Left if only for backwards compatibility
    /**
     * Generate cards.
     * 
     * @param num
     *            the num
     * @param rarity
     *            the rarity
     * @param color
     *            the color
     * @return the list
     */
    public static List<CardPrinted> generateCards(final int num, final CardRarity rarity, final String color) {
        final Predicate<CardPrinted> whatYouWant = BoosterUtils.getPredicateForConditions(rarity, color);
        return BoosterUtils.generateDistinctCards(CardDb.instance().getAllUniqueCards(), whatYouWant, num);
    }

    /**
     * Generate cards.
     * 
     * @param filter
     *            the filter
     * @param num
     *            the num
     * @param rarity
     *            the rarity
     * @param color
     *            the color
     * @return the list
     */
    public static List<CardPrinted> generateCards(final Predicate<CardPrinted> filter, final int num,
            final CardRarity rarity, final String color) {
        final Predicate<CardPrinted> whatYouWant = Predicate.and(filter,
                BoosterUtils.getPredicateForConditions(rarity, color));
        return BoosterUtils.generateDistinctCards(CardDb.instance().getAllUniqueCards(), whatYouWant, num);
    }

    private static List<CardPrinted> generateDistinctCards(final Iterable<CardPrinted> source,
            final Predicate<CardPrinted> filter, final int cntNeeded) {
        final ArrayList<CardPrinted> result = new ArrayList<CardPrinted>();
        int cntMade = 0;

        // This will prevent endless loop @ wh
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

    private static Predicate<CardPrinted> getPredicateForConditions(final CardRarity rarity, final String color) {
        Predicate<CardPrinted> rFilter;
        switch (rarity) {
        case Rare:
            rFilter = CardPrinted.Predicates.Presets.IS_RARE_OR_MYTHIC;
            break;
        case Common:
            rFilter = CardPrinted.Predicates.Presets.IS_COMMON;
            break;
        case Uncommon:
            rFilter = CardPrinted.Predicates.Presets.IS_UNCOMMON;
            break;
        default:
            rFilter = Predicate.getTrue(CardPrinted.class);
        }

        Predicate<CardRules> colorFilter;
        if (StringUtils.isBlank(color)) {
            colorFilter = Predicate.getTrue(CardRules.class);
        } else {
            final String col = color.toLowerCase();
            if (col.startsWith("wh")) {
                colorFilter = CardRules.Predicates.Presets.IS_WHITE;
            } else if (col.startsWith("bla")) {
                colorFilter = CardRules.Predicates.Presets.IS_BLACK;
            } else if (col.startsWith("blu")) {
                colorFilter = CardRules.Predicates.Presets.IS_BLUE;
            } else if (col.startsWith("re")) {
                colorFilter = CardRules.Predicates.Presets.IS_RED;
            } else if (col.startsWith("col")) {
                colorFilter = CardRules.Predicates.Presets.IS_COLORLESS;
            } else if (col.startsWith("gre")) {
                colorFilter = CardRules.Predicates.Presets.IS_GREEN;
            } else if (col.startsWith("mul")) {
                colorFilter = CardRules.Predicates.Presets.IS_MULTICOLOR;
            } else {
                colorFilter = Predicate.getTrue(CardRules.class);
            }
        }
        return Predicate.and(rFilter, colorFilter, CardPrinted.FN_GET_RULES);
    }

    // return List<CardPrinted> of 5 or 6 cards, one for each color and maybe an
    // artifact
    /**
     * Gets the variety.
     * 
     * @param in
     *            the in
     * @return the variety
     */
    public static List<CardPrinted> getVariety(final List<CardPrinted> in) {
        final List<CardPrinted> out = new ArrayList<CardPrinted>();
        Collections.shuffle(in, MyRandom.getRandom());

        for (int i = 0; i < Constant.Color.COLORS.length; i++) {
            final CardPrinted check = BoosterUtils.findCardOfColor(in, i);
            if (check != null) {
                out.add(check);
            }
        }

        return out;
    } // getVariety()

    /**
     * Find card of color.
     * 
     * @param in
     *            the in
     * @param color
     *            the color
     * @return the card printed
     */
    public static CardPrinted findCardOfColor(final List<CardPrinted> in, final int color) {
        final Predicate<CardRules> filter = CardRules.Predicates.Presets.COLORS.get(color);
        if (null == filter) {
            return null;
        }
        return filter.first(in, CardPrinted.FN_GET_RULES);
    }
}
