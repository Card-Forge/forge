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
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;

import forge.StaticData;
import forge.item.PaperCard;
import forge.util.ItemPool;
import forge.util.MyRandom;

/**
 * Deck section.
 * 
 */
public class CardPool extends ItemPool<PaperCard> {

    public CardPool() {
        super(PaperCard.class);
    }

    public CardPool(final Iterable<Entry<PaperCard, Integer>> cards) {
        this();
        this.addAll(cards);
    }

    public void add(final String cardName, final String setCode) {
        this.add(cardName, setCode, -1, 1);
    }

    public void add(final String cardName, final String setCode, final int amount) {
        this.add(cardName, setCode, -1, amount);
    }

    public void add(final String cardName, final String setCode, final int artIndex, final int amount) {
        boolean isCommonCard = true;
        PaperCard cp = StaticData.instance().getCommonCards().tryGetCard(cardName, setCode, artIndex);
        if ( cp == null ) {
            cp = StaticData.instance().getVariantCards().tryGetCard(cardName, setCode, artIndex);
            isCommonCard = false;
        }

        int artCount = isCommonCard ? StaticData.instance().getCommonCards().getArtCount(cardName, setCode) : StaticData.instance().getVariantCards().getArtCount(cardName, setCode);

        if ( cp != null) {
            if (artIndex >= 0 || artCount <= 1) {
                // either a specific art index is specified, or there is only one art, so just add the card
                this.add(cp, amount);
            } else {
                // random art index specified, make sure we get different groups of cards with different art
                int[] artGroups = MyRandom.splitIntoRandomGroups(amount, artCount);
                for (int i = 0; i < artGroups.length; i++) {
                    PaperCard cp_random = isCommonCard ? StaticData.instance().getCommonCards().tryGetCard(cardName, setCode, i) : StaticData.instance().getVariantCards().tryGetCard(cardName, setCode, i);
                    this.add(cp_random, artGroups[i]);
                }
            }
        }
        else {
            throw new RuntimeException(String.format("Card %s from %s is not supported by Forge, as it's neither a known common card nor one of casual variants' card.", cardName, setCode ));
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
        // in order to account for art index randomization we have to add cards one by one instead of in a batch
        // TODO: somehow optimize this algorithm?...
        for (int i = 0; i < cnt; i++) {
            PaperCard cp = StaticData.instance().getCommonCards().tryGetCard(cardName);
            if ( cp == null )
                cp = StaticData.instance().getVariantCards().tryGetCard(cardName);
    
            if ( cp != null)
                this.add(cp);
            else
                throw new NoSuchElementException(String.format("Card %s is not supported by Forge, as it's neither a known common card nor one of casual variants' card.", cardName));
        }
    }

    /**
     * returns n-th card from this DeckSection. LINEAR time. No fixed order between changes
     * @param i
     * @return
     */
    public PaperCard get(int n) {
        for (Entry<PaperCard, Integer> e : this) {
            n -= e.getValue();
            if ( n <= 0 ) return e.getKey();
        }
        return null;
    }

    @Override
    public String toString() {
        if (this.isEmpty()) { return "[]"; }

        boolean isFirst = true;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Entry<PaperCard, Integer> e : this) {
            if (isFirst) {
                isFirst = false;
            }
            else {
                sb.append(", ");
            }
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
