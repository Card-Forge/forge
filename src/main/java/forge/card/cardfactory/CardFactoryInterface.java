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
package forge.card.cardfactory;

import java.util.Iterator;

import forge.Card;
import forge.CardList;
import forge.Player;
import forge.card.spellability.SpellAbility;

/**
 * The Interface CardFactoryInterface.
 */
public interface CardFactoryInterface extends Iterable<Card> {

    /**
     * Iterate over all full-fledged cards in the database; these cards are
     * owned by the human player by default.
     * 
     * @return an Iterator that does NOT support the remove method
     */
    @Override
    Iterator<Card> iterator();

    /**
     * Typical size method.
     * 
     * @return an estimate of the number of items encountered by this object's
     *         iterator
     * 
     * @see #iterator
     */
    int size();

    /**
     * <p>
     * copyCard.
     * </p>
     * 
     * @param in
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    Card copyCard(Card in);

    /**
     * <p>
     * copyCardintoNew.
     * </p>
     * 
     * @param in
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    Card copyCardintoNew(Card in);

    /**
     * <p>
     * copySpellontoStack.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param original
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param bCopyDetails
     *            a boolean.
     */
    void copySpellontoStack(Card source, Card original, SpellAbility sa, boolean bCopyDetails);

    /**
     * <p>
     * copySpellontoStack.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param original
     *            a {@link forge.Card} object.
     * @param bCopyDetails
     *            a boolean.
     */
    void copySpellontoStack(Card source, Card original, boolean bCopyDetails);

    /**
     * <p>
     * getCard.
     * </p>
     * 
     * @param cardName
     *            a {@link java.lang.String} object.
     * @param owner
     *            a {@link forge.Player} object.
     * @return a {@link forge.Card} instance, owned by owner; or the special
     *         blankCard
     */
    Card getCard(String cardName, Player owner);

    /**
     * Fetch a random combination of cards without any duplicates.
     * 
     * This algorithm is reasonably fast if numCards is small. If it is larger
     * than, say, size()/10, it starts to get noticeably slow.
     * 
     * @param numCards
     *            the number of cards to return
     * @return a list of fleshed-out card instances
     */
    CardList getRandomCombinationWithoutRepetition(int numCards);

}
