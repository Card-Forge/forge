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

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.card.CardDb;
import forge.item.PaperCard;
import forge.item.ItemPool;

/**
 * Deck section.
 * 
 */
public class CardPool extends ItemPool<PaperCard> {

    /**
     * Instantiates a new deck section.
     */
    public CardPool() {
        super(PaperCard.class);
    }

    /**
     * Instantiates a new deck section.
     *
     * @param cards the cards
     */
    public CardPool(final Iterable<Entry<PaperCard, Integer>> cards) {
        this();
        this.addAll(cards);
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
        PaperCard cp = CardDb.instance().tryGetCard(cardName, setCode);
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
    public void add(final Iterable<PaperCard> list) {
        for (PaperCard cp : list) {
            this.add(cp);
        }
    }

    /**
     * TODO: Write javadoc for this method.
     *
     * @param cardName the card name
     */
    public void add(final String cardName, int cnt) {
        PaperCard cp = CardDb.instance().tryGetCard(cardName);
        if ( cp == null )
            cp = CardDb.variants().tryGetCard(cardName);

        if ( cp != null)
            this.add(cp, cnt);
        else
            throw new NoSuchElementException(String.format("Card %s is not supported by Forge, as it's neither a known common card nor one of casual variants' card.", cardName));
    }

    /**
     * returns n-th card from this DeckSection. LINEAR time. No fixed order between changes
     * @param i
     * @return
     */
    public PaperCard get(int n) {
        for(Entry<PaperCard, Integer> e : this)
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
        for (Entry<PaperCard, Integer> e : this) {
            if ( isFirst ) isFirst = false; 
            else sb.append(", ");

            sb.append(e.getValue()).append(" x ").append(e.getKey().getName());
        }
        return sb.append(']').toString();
    }
    
    
    public static CardPool fromCardList(final Iterable<String> lines) {
        CardPool pool = new CardPool();
        final Pattern p = Pattern.compile("((\\d+)\\s+)?(.*?)");

        if (lines == null) {
            return pool;
        }

        final Iterator<String> lineIterator = lines.iterator();
        while (lineIterator.hasNext()) {
            final String line = lineIterator.next();
            if (line.startsWith(";") || line.startsWith("#")) { continue; } // that is a comment or not-yet-supported card

            final Matcher m = p.matcher(line.trim());
            m.matches();
            final String sCnt = m.group(2);
            final String cardName = m.group(3);
            if (StringUtils.isBlank(cardName)) {
                continue;
            }

            final int count = sCnt == null ? 1 : Integer.parseInt(sCnt);
            pool.add(cardName, count);
        }
        return pool;
    }
}
