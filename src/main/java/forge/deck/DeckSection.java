/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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

import forge.Card;
import forge.CardList;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPool;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class DeckSection extends ItemPool<CardPrinted> {

    /**
     * Instantiates a new deck section.
     */
    public DeckSection() {
        super(CardPrinted.class);
    }

    /**
     * Clear main.
     */
    public void clearMain() {
        this.clear();

    }

    /**
     * Sets the.
     *
     * @param cardNames the card names
     */
    public void set(final Iterable<String> cardNames) {
        this.clear();
        this.addAllCards(CardDb.instance().getCards(cardNames));
    }

    /**
     * Adds the.
     *
     * @param card the card
     */
    public void add(final Card card) {
        this.add(CardDb.instance().getCard(card));
    }

    /**
     * Adds the.
     *
     * @param cardName the card name
     * @param setCode the set code
     */
    public void add(final String cardName, final String setCode) {
        this.add(CardDb.instance().getCard(cardName, setCode));
    }

    /**
     * Adds the.
     *
     * @param cardList the card list
     */
    public void add(final CardList cardList) {
        for (final Card c : cardList) {
            this.add(c);
        }
    }

}
