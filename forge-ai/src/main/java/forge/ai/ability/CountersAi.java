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
package forge.ai.ability;

import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.*;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

import java.util.List;


/**
 * <p>
 * AbilityFactory_Counters class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public abstract class CountersAi extends SpellAbilityAi {
    // An AbilityFactory subclass for Putting or Removing Counters on Cards.

    /**
     * <p>
     * chooseCursedTarget.
     * </p>
     *
     * @param list
     *            a {@link CardCollectionView} object.
     * @param type
     *            a {@link String} object.
     * @param amount
     *            a int.
     * @param ai a {@link Player} object.
     * @return a {@link Card} object.
     */
    public static Card chooseCursedTarget(final CardCollectionView list, final String type, final int amount, final Player ai) {
        Card choice;

        // opponent can always order it so that he gets 0
        if (amount == 1 && ai.getOpponents().getCardsIn(ZoneType.Battlefield).anyMatch(CardPredicates.nameEquals("Vorinclex, Monstrous Raider"))) {
            return null;
        }

        if (type.equals("M1M1")) {
            // try to kill the best killable creature, or reduce the best one
            // but try not to target a Undying Creature
            final List<Card> killable = CardLists.getNotKeyword(CardLists.filterToughness(list, amount), Keyword.UNDYING);
            if (!killable.isEmpty()) {
                choice = ComputerUtilCard.getBestCreatureAI(killable);
            } else {
                choice = ComputerUtilCard.getBestCreatureAI(list);
            }
        } else {
            // improve random choice here
            choice = Aggregates.random(list);
        }
        return choice;
    }

    /**
     * <p>
     * chooseBoonTarget.
     * </p>
     *
     * @param list
     *            a {@link CardCollectionView} object.
     * @param type
     *            a {@link String} object.
     * @return a {@link Card} object.
     */
    public static Card chooseBoonTarget(final CardCollectionView list, final String type) {
        Card choice = null;

        if (type.equals("P1P1")) {
            // TODO look for modified
            choice = ComputerUtilCard.getBestCreatureAI(list);

            if (choice == null) {
                // We'd only get here if list isn't empty, maybe we're trying to animate a land?
                choice = ComputerUtilCard.getBestLandToAnimate(list);
            }
        } else if (type.equals("DIVINITY")) {
            final CardCollection boon = CardLists.filter(list, c -> c.getCounters(CounterEnumType.DIVINITY) == 0);
            choice = ComputerUtilCard.getMostExpensivePermanentAI(boon);
        } else if (CounterType.getType(type).isKeywordCounter()) {
            choice = ComputerUtilCard.getBestCreatureAI(CardLists.getNotKeyword(list, type));
        } else {
            // The AI really should put counters on cards that can use it.
            // Charge counters on things with Charge abilities, etc. Expand
            // these above
            choice = Aggregates.random(list);
        }
        return choice;
    }

}
