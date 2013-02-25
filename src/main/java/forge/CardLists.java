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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.card.spellability.SpellAbility;
import forge.game.ai.ComputerUtilCard;
import forge.game.player.Player;
import forge.util.MyRandom;


/**
 * <p>
 * CardListUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardLists {
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
    public static List<Card> filterToughness(final List<Card> in, final int atLeastToughness) {
        return CardLists.filter(in, new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.getNetDefense() <= atLeastToughness;
            }
        });
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
    public static void sortAttack(final List<Card> list) {
        Collections.sort(list, AttackComparator);
    } // sortAttack()

    /**
     * <p>
     * Sorts a List<Card> by "best" using the EvaluateCreature function.
     * the best creatures will be first in the list.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortByEvaluateCreature(final List<Card> list) {
        Collections.sort(list, ComputerUtilCard.EvaluateCreatureComparator);
    } // sortByEvaluateCreature()

    /**
     * <p>
     * Sorts a List<Card> by converted mana cost, putting highest first.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortByMostExpensive(final List<Card> list) {
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
    public static void sortAttackLowFirst(final List<Card> list) {
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
    public static void sortNonFlyingFirst(final List<Card> list) {
        CardLists.sortFlying(list);
        Collections.reverse(list);
    } // sortNonFlyingFirst

    /**
     * <p>
     * Sorts a CardList, putting creatures with Flying first.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortFlying(final List<Card> list) {
        Collections.sort(list, getKeywordComparator("Flying"));
    } // sortFlying()

    /**
     * <p>
     * Sorts a List<Card> from highest converted mana cost to lowest.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     */
    public static void sortCMC(final List<Card> list) {
        Collections.sort(list, CmcComparator);
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
    public static List<Card> getColor(final List<Card> list, final String color) {
        return CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return CardUtil.getColors(c).contains(color);
            }
        });
    } // getColor()


    /**
     * 
     * Given a List<Card> c, return a List<Card> that contains a random amount of cards from c.
     * 
     * @param c
     *            CardList
     * @param amount
     *            int
     * @return CardList
     */
    public static List<Card> getRandomSubList(final List<Card> c, final int amount) {
        if (c.size() < amount) {
            return null;
        }

        final List<Card> cs = Lists.newArrayList(c);

        final List<Card> subList = new ArrayList<Card>();
        while (subList.size() < amount) {
            CardLists.shuffle(cs);
            subList.add(cs.remove(0));
        }
        return subList;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param cardList
     */
    public static void shuffle(List<Card> list) {
        // reseed Random each time we want to Shuffle
        // MyRandom.random = MyRandom.random;
        Collections.shuffle(list, MyRandom.getRandom());
        Collections.shuffle(list, MyRandom.getRandom());
        Collections.shuffle(list, MyRandom.getRandom());
    }

    public static List<Card> filterControlledBy(Iterable<Card> cardList, Player player) {
        return CardLists.filter(cardList, CardPredicates.isController(player));
    }

    public static List<Card> filterControlledBy(Iterable<Card> cardList, List<Player> player) {
        return CardLists.filter(cardList, CardPredicates.isControlledByAnyOf(player));
    }

    public static List<Card> getValidCards(Iterable<Card> cardList, String[] restrictions, Player sourceController, Card source) {
        return CardLists.filter(cardList, CardPredicates.restriction(restrictions, sourceController, source));
    }

    public static List<Card> getValidCards(Iterable<Card> cardList, String restriction, Player sourceController, Card source) {
        return CardLists.filter(cardList, CardPredicates.restriction(restriction.split(","), sourceController, source));
    }

    public static List<Card> getTargetableCards(Iterable<Card> cardList, SpellAbility source) {
        return CardLists.filter(cardList, CardPredicates.isTargetableBy(source));
    }

    public static List<Card> getKeyword(Iterable<Card> cardList, String keyword) {
        return CardLists.filter(cardList, CardPredicates.hasKeyword(keyword));
    }

    public static List<Card> getNotKeyword(Iterable<Card> cardList, String keyword) {
        return CardLists.filter(cardList, Predicates.not(CardPredicates.hasKeyword(keyword)));
    }

    // cardType is like "Land" or "Goblin", returns a new ArrayList<Card> that is a
    // subset of current CardList
    public static List<Card> getNotType(Iterable<Card> cardList, String cardType) {
        return CardLists.filter(cardList, Predicates.not(CardPredicates.isType(cardType)));
    }

    public static List<Card> getType(Iterable<Card> cardList, String cardType) {
        return CardLists.filter(cardList, CardPredicates.isType(cardType));
    }

    /**
     * Create a new list of cards by applying a filter to this one.
     * 
     * @param filt
     *            determines which cards are present in the resulting list
     * 
     * @return a subset of this List<Card> whose items meet the filtering
     *         criteria; may be empty, but never null.
     */
    public static List<Card> filter(Iterable<Card> cardList, Predicate<Card> filt) {
        return Lists.newArrayList(Iterables.filter(cardList, filt));
    }

    public static List<Card> createCardList(Card c) {
        List<Card> res = new ArrayList<Card>();
        res.add(c);
        return res;
    }
}
