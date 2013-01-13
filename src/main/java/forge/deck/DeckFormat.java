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
package forge.deck;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.math.IntRange;

import forge.card.CardCoreType;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.util.Aggregates;

/**
 * GameType is an enum to determine the type of current game. :)
 */
public enum DeckFormat {

    //            Main board: allowed size             SB: restriction    Max distinct non basic cards
    Constructed ( new IntRange(60, Integer.MAX_VALUE), new IntRange(15),    4),
    Limited     ( new IntRange(40, Integer.MAX_VALUE), null,                Integer.MAX_VALUE),
    Commander   ( new IntRange(99),                    new IntRange(0, 10), 1),
    Vanguard    ( new IntRange(60, Integer.MAX_VALUE), new IntRange(0),     4),
    Planechase  ( new IntRange(60, Integer.MAX_VALUE), new IntRange(0),     4),
    Archenemy   ( new IntRange(60, Integer.MAX_VALUE), new IntRange(0),     4);

    private final IntRange mainRange;
    private final IntRange sideRange; // null => no check
    private final int maxCardCopies;


    /**
     * Instantiates a new game type.
     * 
     * @param isLimited
     *            the is limited
     */
    DeckFormat(IntRange main, IntRange side, int maxCopies) {
        mainRange = main;
        sideRange = side;
        maxCardCopies = maxCopies;
    }

    /**
     * Smart value of.
     *
     * @param value the value
     * @param defaultValue the default value
     * @return the game type
     */
    public static DeckFormat smartValueOf(final String value, DeckFormat defaultValue) {
        if (null == value) {
            return defaultValue;
        }

        final String valToCompate = value.trim();
        for (final DeckFormat v : DeckFormat.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }

        throw new IllegalArgumentException("No element named " + value + " in enum GameType");
    }


    /**
     * @return the sideRange
     */
    public IntRange getSideRange() {
        return sideRange;
    }


    /**
     * @return the mainRange
     */
    public IntRange getMainRange() {
        return mainRange;
    }


    /**
     * @return the maxCardCopies
     */
    public int getMaxCardCopies() {
        return maxCardCopies;
    }



    @SuppressWarnings("incomplete-switch")
    public String getDeckConformanceProblem(Deck deck) {
        int deckSize = deck.getMain().countAll();

        int min = getMainRange().getMinimumInteger();
        int max = getMainRange().getMaximumInteger();

        if (deckSize < min) {
            return String.format("should have a minimum of %d cards", min);
        }

        if (deckSize > max) {
            return String.format("should not exceed a maximum of %d cards", max);
        }

        switch(this) {
            case Commander: //Must contain exactly 1 legendary Commander and no sideboard.

                //TODO:Enforce color identity
                if (null == deck.getCommander()) {
                    return "is missing a commander";
                }
                if (!deck.getCommander().getCard().getType().isLegendary()) {
                    return "has a commander that is not a legendary creature";
                }
                
                break;

            case Planechase: //Must contain at least 10 planes/phenomenons, but max 2 phenomenons. Singleton.
                if (deck.getSideboard().countAll() < 10) {
                    return "should gave at least 10 planes";
                }
                int phenoms = 0;
                for (Entry<CardPrinted, Integer> cp : deck.getSideboard()) {

                    if (cp.getKey().getCard().getType().typeContains(CardCoreType.Phenomenon)) {
                        phenoms++;
                    }
                    if (cp.getValue() > 1) {
                        return "must not contain multiple copies of any Phenomena";
                    }

                }
                if (phenoms > 2) {
                    return "must not contain more than 2 Phenomena";
                }
                break;

            case Archenemy:  //Must contain at least 20 schemes, max 2 of each.
                if (deck.getSideboard().countAll() < 20) {
                    return "must contain at least 20 schemes";
                }

                for (Entry<CardPrinted, Integer> cp : deck.getSideboard()) {
                    if (cp.getValue() > 2) {
                        return "must not contain more than 2 copies of any Scheme";
                    }
                }
                break;
        }

        int maxCopies = getMaxCardCopies();
        if (maxCopies < Integer.MAX_VALUE) {
            //Must contain no more than 4 of the same card
            //shared among the main deck and sideboard, except
            //basic lands and Relentless Rats

            DeckSection tmp = new DeckSection(deck.getMain());
            tmp.addAll(deck.getSideboard());
            if (null != deck.getCommander() && this == Commander) {
                tmp.add(deck.getCommander());
            }

            List<String> limitExceptions = Arrays.asList("Relentless Rats");

            // should group all cards by name, so that different editions of same card are really counted as the same card
            for (Entry<String, Integer> cp : Aggregates.groupSumBy(tmp, CardPrinted.FN_GET_NAME)) {

                CardPrinted simpleCard = CardDb.instance().getCard(cp.getKey());
                boolean canHaveMultiple = simpleCard.getCard().getType().isBasicLand() || limitExceptions.contains(cp.getKey());

                if (!canHaveMultiple && cp.getValue() > maxCopies) {
                    return String.format("must not contain more than %d of '%s' card", maxCopies, cp.getKey());
                }
            }

            // The sideboard must contain either 0 or 15 cards
            int sideboardSize = deck.getSideboard().countAll();
            IntRange sbRange = getSideRange();
            if (sbRange != null && sideboardSize > 0 && !sbRange.containsInteger(sideboardSize)) {
                return sbRange.getMinimumInteger() == sbRange.getMaximumInteger()
                ? String.format("must have a sideboard of %d cards or no sideboard at all", sbRange.getMaximumInteger())
                : String.format("must have a sideboard of %d to %d cards or no sideboard at all", sbRange.getMinimumInteger(), sbRange.getMaximumInteger());
            }

        }

        return null;
    }
}
