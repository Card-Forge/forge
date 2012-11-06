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
package forge.card.abilityfactory.ai;

import java.util.List;

import com.google.common.base.Predicate;

import forge.Card;

import forge.CardLists;
import forge.Counters;
import forge.card.cardfactory.CardFactoryUtil;


/**
 * <p>
 * AbilityFactory_Counters class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class CountersAi {
    // An AbilityFactory subclass for Putting or Removing Counters on Cards.

    /**
     * <p>
     * chooseCursedTarget.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @param amount
     *            a int.
     * @return a {@link forge.Card} object.
     */
    public static Card chooseCursedTarget(final List<Card> list, final String type, final int amount) {
        Card choice;
        if (type.equals("M1M1")) {
            // try to kill the best killable creature, or reduce the best one
            final List<Card> killable = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.getNetDefense() <= amount;
                }
            });
            if (killable.size() > 0) {
                choice = CardFactoryUtil.getBestCreatureAI(killable);
            } else {
                choice = CardFactoryUtil.getBestCreatureAI(list);
            }
        } else {
            // improve random choice here
            choice = CardFactoryUtil.getRandomCard(list);
        }
        return choice;
    }

    /**
     * <p>
     * chooseBoonTarget.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @return a {@link forge.Card} object.
     */
    public static Card chooseBoonTarget(final List<Card> list, final String type) {
        Card choice;
        if (type.equals("P1P1")) {
            choice = CardFactoryUtil.getBestCreatureAI(list);
        } else if (type.equals("DIVINITY")) {
            final List<Card> boon = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.getCounters(Counters.DIVINITY) == 0;
                }
            });
            choice = CardFactoryUtil.getMostExpensivePermanentAI(boon, null, false);
        } else {
            // The AI really should put counters on cards that can use it.
            // Charge counters on things with Charge abilities, etc. Expand
            // these above
            choice = CardFactoryUtil.getRandomCard(list);
        }
        return choice;
    }

}
