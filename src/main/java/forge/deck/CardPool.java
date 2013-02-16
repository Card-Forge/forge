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

import java.util.List;
import java.util.Map.Entry;

import forge.Card;

import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPool;

/**
 * Deck section.
 * 
 */
public class CardPool extends ItemPool<CardPrinted> {

    /**
     * Instantiates a new deck section.
     */
    public CardPool() {
        super(CardPrinted.class);
    }

    /**
     * Instantiates a new deck section.
     *
     * @param cards the cards
     */
    public CardPool(final Iterable<Entry<CardPrinted, Integer>> cards) {
        this();
        this.addAll(cards);
    }

    /**
     * Sets the.
     * 
     * @param cardNames
     *            the card names
     */
    public void set(final Iterable<String> cardNames) {
        this.clear();
        for (final String name : cardNames) {
            this.add(name);
        }
    }

    /**
     * Adds the.
     * 
     * @param card
     *            the card
     */
    public void add(final Card card) {
        this.add(CardDb.getCard(card));
    }

    /**
     * Adds the.
     * 
     * @param cardName
     *            the card name
     * @param setCode
     *            the set code
     */
    public void add(final String cardName, final String setCode) {
        this.add(cardName, setCode, 1);
    }

    /**
     * Adds the.
     *
     * @param cardName the card name
     * @param setCode the set code
     * @param amount the amount
     */
    public void add(final String cardName, final String setCode, final int amount) {
        CardPrinted cp = CardDb.instance().tryGetCard(cardName, setCode);
        if ( cp == null )
            cp = CardDb.variants().tryGetCard(cardName, setCode);

        if ( cp != null)
            this.add(cp, amount);
        else
            throw new RuntimeException(String.format("Card %s from %s is not supported by Forge, as it's neither a known common card nor one of casual variants' card.", cardName, setCode ));
    }

    /**
     * Adds the.
     * 
     * @param cardList
     *            the card list
     */
    public void add(final List<Card> cardList) {
        for (final Card c : cardList) {
            this.add(c);
        }
    }

    /**
     * Add all from a List of CardPrinted.
     * 
     * @param list
     *            CardPrinteds to add
     */
    public void add(final Iterable<CardPrinted> list) {
        for (CardPrinted cp : list) {
            this.add(cp);
        }
    }

    /**
     * TODO: Write javadoc for this method.
     *
     * @param cardName the card name
     */
    public void add(final String cardName) {
        CardPrinted cp = CardDb.instance().tryGetCard(cardName);
        if ( cp == null )
            cp = CardDb.variants().tryGetCard(cardName);

        if ( cp != null)
            this.add(cp);
        else
            throw new RuntimeException(String.format("Card %s is not supported by Forge, as it's neither a known common card nor one of casual variants' card.", cardName));
    }

    /**
     * returns n-th card from this DeckSection. LINEAR time. No fixed order between changes
     * @param i
     * @return
     */
    public CardPrinted get(int n) {
        for(Entry<CardPrinted, Integer> e : this)
        {
            n -= e.getValue();
            if ( n <= 0 ) return e.getKey();
        }
        return null;
    }
    
    @Override
    public String toString() {
        if (this.isEmpty()) return "[]";

        boolean isFirst = true;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Entry<CardPrinted, Integer> e : this) {
            if ( isFirst ) isFirst = false; 
            else sb.append(", ");

            sb.append(e.getValue()).append(" x ").append(e.getKey().getName());
        }
        return sb.append(']').toString();
    }
}
