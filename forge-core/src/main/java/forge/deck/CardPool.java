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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardDb;
import forge.item.PaperCard;
import forge.util.ItemPool;
import forge.util.ItemPoolSorter;
import forge.util.MyRandom;


public class CardPool extends ItemPool<PaperCard> {
    private static final long serialVersionUID = -5379091255613968393L;

    public CardPool() {
        super(PaperCard.class);
    }

    public CardPool(final Iterable<Entry<PaperCard, Integer>> cards) {
        this();
        this.addAll(cards);
    }
    
    public void add(final String cardName, final int amount) {
        this.add(cardName, null, -1, amount);
    }    

    public void add(final String cardName, final String setCode) {
        this.add(cardName, setCode, -1, 1);
    }

    public void add(final String cardName, final String setCode, final int amount) {
        this.add(cardName, setCode, -1, amount);
    }

    // NOTE: ART indices are "1" -based
    public void add(String cardName, String setCode, final int artIndex, final int amount) {
        PaperCard cp = StaticData.instance().getCommonCards().getCard(cardName, setCode, artIndex);
        boolean isCommonCard = cp != null;
        if (!isCommonCard) {
            cp = StaticData.instance().getVariantCards().getCard(cardName, setCode);
        }

        boolean artIndexExplicitlySet = artIndex > 0 || Character.isDigit(cardName.charAt(cardName.length()-1)) && cardName.charAt(cardName.length()-2) == CardDb.NameSetSeparator;
        int artCount = 1;

        if (cp != null ) {
            setCode = cp.getEdition();
            cardName = cp.getName();
            artCount = isCommonCard ? StaticData.instance().getCommonCards().getArtCount(cardName, setCode) : 1;
        }
        else {
            cp = StaticData.instance().getCommonCards().createUnsuportedCard(cardName); 
        }

        if (artIndexExplicitlySet || artCount <= 1) {
            // either a specific art index is specified, or there is only one art, so just add the card
            this.add(cp, amount);
        } else {
            // random art index specified, make sure we get different groups of cards with different art
            int[] artGroups = MyRandom.splitIntoRandomGroups(amount, artCount);
            for (int i = 1; i <= artGroups.length; i++) {
                int cnt = artGroups[i-1];
                if (cnt <= 0)
                    continue;
                PaperCard cp_random = isCommonCard 
                        ? StaticData.instance().getCommonCards().getCard(cardName, setCode, i) 
                        : StaticData.instance().getVariantCards().getCard(cardName, setCode, i);
                this.add(cp_random, cnt);
            }
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
     * returns n-th card from this DeckSection. LINEAR time. No fixed order between changes
     * @param n
     * @return
     */
    public PaperCard get(int n) {
        for (Entry<PaperCard, Integer> e : this) {
            n -= e.getValue();
            if ( n <= 0 ) return e.getKey();
        }
        return null;
    }

    public int countByName(String cardName, boolean isCommonCard) {
        PaperCard pc = isCommonCard
                ? StaticData.instance().getCommonCards().getCard(cardName)
                : StaticData.instance().getVariantCards().getCard(cardName);

        return this.count(pc);
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

    private final static Pattern p = Pattern.compile("((\\d+)\\s+)?(.*?)");
    public static CardPool fromCardList(final Iterable<String> lines) {
        CardPool pool = new CardPool();


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
    
    public String toCardList(String separator) {
        List<Entry<PaperCard, Integer>> main2sort = Lists.newArrayList(this);
        Collections.sort(main2sort, ItemPoolSorter.BY_NAME_THEN_SET);
        final CardDb commonDb = StaticData.instance().getCommonCards();
        StringBuilder sb = new StringBuilder();
        
        boolean isFirst = true;
        
        for (final Entry<PaperCard, Integer> e : main2sort) {
            if(!isFirst)
                sb.append(separator);
            else
                isFirst = false;
            
            CardDb db = !e.getKey().getRules().isVariant() ? commonDb : StaticData.instance().getVariantCards();
            sb.append(e.getValue()).append(" ");
            db.appendCardToStringBuilder(e.getKey(), sb);
            
        }
        return sb.toString();
    }
}
