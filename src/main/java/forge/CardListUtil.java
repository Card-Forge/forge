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
package forge;

import java.util.Collections;
import java.util.Comparator;

import forge.card.cardfactory.CardFactoryUtil;
import forge.util.closures.Predicate;

/**
 * <p>
 * CardListUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardListUtil {
    /**
     * <p>
     * filterToughness.
     * </p>
     * 
     * @param in
     *            a {@link forge.CardList} object.
     * @param atLeastToughness
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public static CardList filterToughness(final CardList in, final int atLeastToughness) {
        return in.filter(
            new Predicate<Card>() {
                @Override
                public boolean isTrue(Card c) {
                    return c.getNetDefense() <= atLeastToughness;
                }
            }
        );
    }

    public static final Comparator<Card> DefenseComparator = new Comparator<Card>() {
        @Override
        public int compare(final Card a, final Card b) {
            return b.getNetDefense() - a.getNetDefense();
        }
    };    
    public static final Comparator<Card> AttackComparator = new Comparator<Card>() {
        @Override
        public int compare(final Card a, final Card b) {
            return b.getNetCombatDamage() - a.getNetCombatDamage();
        }
    };    
    public static final Comparator<Card> EvaluateCreatureComparator = new Comparator<Card>() {
        @Override
        public int compare(final Card a, final Card b) {
            return CardFactoryUtil.evaluateCreature(b) - CardFactoryUtil.evaluateCreature(a);
        }
    };

    public static final Comparator<Card> CmcComparator = new Comparator<Card>() {
        @Override
        public int compare(final Card a, final Card b) {
            return b.getCMC() - a.getCMC();
        }
    };    
    
    public static final Comparator<Card> TextLenReverseComparator = new Comparator<Card>() {
        @Override
        public int compare(final Card a, final Card b) {
            final int aLen = a.getText().length();
            final int bLen = b.getText().length();
            return aLen - bLen;
        }
    };

    public static final Comparator<Card> getKeywordComparator(final String kw) {
        return new Comparator<Card>() {
            @Override
            public int compare(final Card a, final Card b) {
                int aV = a.hasKeyword(kw) ? 1 : 0;
                int bV = b.hasKeyword(kw) ? 1 : 0;
                return bV - aV;
            }
        };
    }
    
    // the higher the attack the better
    /**
     * <p>
     * sortAttack.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortAttack(final CardList list) {
        Collections.sort(list, AttackComparator);
    } // sortAttack()

    /**
     * <p>
     * Sorts a CardList by "best" using the EvaluateCreature function.
     * the best creatures will be first in the list.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortByEvaluateCreature(final CardList list) {
        Collections.sort(list, EvaluateCreatureComparator);
    } // sortByEvaluateCreature()

    /**
     * <p>
     * Sorts a CardList by converted mana cost, putting highest first.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortByMostExpensive(final CardList list) {
        Collections.sort(list, CmcComparator);
    } // sortByMostExpensive()

    // the lower the attack the better
    /**
     * <p>
     * sortAttackLowFirst.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortAttackLowFirst(final CardList list) {
        Collections.sort(list, Collections.reverseOrder(AttackComparator));
    } // sortAttackLowFirst()

    /**
     * <p>
     * Sorts a CardList, putting creatures without Flying first.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortNonFlyingFirst(final CardList list) {
        CardListUtil.sortFlying(list);
        list.reverse();
    } // sortNonFlyingFirst

    /**
     * <p>
     * Sorts a CardList, putting creatures with Flying first.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortFlying(final CardList list) {
        Collections.sort(list, getKeywordComparator("Flying"));
    } // sortFlying()

    /**
     * <p>
     * Sorts a CardList from highest converted mana cost to lowest.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortCMC(final CardList list) {
        Collections.sort( list, CmcComparator );
    } // sortCMC

    /**
     * <p>
     * getColor.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param color
     *            a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getColor(final CardList list, final String color) {
        return list.filter(new Predicate<Card>() {
            @Override
            public boolean isTrue(final Card c) {
                return CardUtil.getColors(c).contains(color);
            }
        });
    } // getColor()

    /**
     * <p>
     * getGoldCards.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getGoldCards(final CardList list) {
        return list.filter(new Predicate<Card>() {
            @Override
            public boolean isTrue(final Card c) {
                return CardUtil.getColors(c).size() >= 2;
            }
        });
    }

    // Get the total converted mana cost of a card list
    /**
     * <p>
     * Gets the total converted mana cost of a card list.
     * </p>
     * 
     * @param c
     *            a {@link forge.CardList} object.
     * @return a int.
     */
    public static int sumCMC(final CardList c) {
        return CardPredicates.Presets.All.sum(c, CardPredicates.Accessors.fnGetCmc);
    } // sumCMC

    /**
     * <p>
     * Gets the average converted mana cost of a card list.
     * </p>
     * 
     * @param c
     *            a {@link forge.CardList} object.
     * @return a float.
     */
    public static float getAverageCMC(final CardList c) {

        return sumCMC(c) / c.size();

    }

    /**
     * 
     * Given a CardList c, return a CardList that contains a random amount of cards from c.
     * 
     * @param c
     *            CardList
     * @param amount
     *            int
     * @return CardList
     */
    public static CardList getRandomSubList(final CardList c, final int amount) {
        if (c.size() < amount) {
            return null;
        }

        final CardList subList = new CardList();
        while (subList.size() < amount) {
            c.shuffle();
            subList.add(c.get(0));
            c.remove(0);
        }
        return subList;
    }
}
