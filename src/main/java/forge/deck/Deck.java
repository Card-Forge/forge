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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import forge.deck.io.DeckFileHeader;
import forge.deck.io.DeckSerializer;
import forge.gui.deckeditor.elements.TableSorter;
import forge.item.CardPrinted;
import forge.item.ItemPoolView;
import forge.util.FileUtil;
import forge.util.IHasName;
import forge.util.SectionUtil;

/**
 * <p>
 * Deck class.
 * </p>
 * 
 * The set of MTG legal cards that become player's library when the game starts.
 * Any other data is not part of a deck and should be stored elsewhere. Current
 * fields allowed for deck metadata are Name, Title, Description and Deck Type.
 * 
 * @author Forge
 * @version $Id$
 */
public class Deck extends DeckBase implements Serializable, IHasName {
    /**
     *
     */
    private static final long serialVersionUID = -7478025567887481994L;

    private final DeckSection main;
    private final DeckSection sideboard;

    // gameType is from Constant.GameType, like GameType.Regular
    /**
     * <p>
     * Decks have their named finalled.
     * </p>
     */
    public Deck() { this(""); }

    public Deck(String name0) {
        super(name0);
        this.main = new DeckSection();
        this.sideboard = new DeckSection();
    }

    /**
     * <p>
     * hashCode.
     * </p>
     * 
     * @return a int.
     */
    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getName();
    }


    /**
     * <p>
     * Getter for the field <code>main</code>.
     * </p>
     * 
     * @return a {@link java.util.List} object.
     */
    public DeckSection getMain() {
        return this.main;
    }

    /**
     * <p>
     * Getter for the field <code>sideboard</code>.
     * </p>
     * 
     * @return a {@link java.util.List} object.
     */
    public DeckSection getSideboard() {
        return this.sideboard;
    }

    /* (non-Javadoc)
     * @see forge.item.CardCollectionBase#getCardPool()
     */
    @Override
    public ItemPoolView<CardPrinted> getCardPool() {
        return main;
    }

    protected void cloneFieldsTo(DeckBase clone) {
        super.cloneFieldsTo(clone);
        Deck result = (Deck) clone;
        result.main.addAll(this.main);
        result.sideboard.addAll(this.sideboard);
    }

    /* (non-Javadoc)
     * @see forge.deck.DeckBase#newInstance(java.lang.String)
     */
    @Override
    protected DeckBase newInstance(String name0) {
        return new Deck(name0);
    }


    public static Deck fromFile(final File deckFile) {
        return fromLines(FileUtil.readFile(deckFile));
    }

    public static Deck fromLines(final List<String> deckFileLines) {
        return Deck.fromSections(SectionUtil.parseSections(deckFileLines));
    }

    public static Deck fromSections(Map<String, List<String>> sections) {
        if (sections.isEmpty()) {
            return null;
        }

        DeckFileHeader dh = DeckSerializer.readDeckMetadata(sections);

        final Deck d = new Deck(dh.getName());
        d.setComment(dh.getComment());

        d.getMain().set(readCardList(sections.get("main")));
        d.getSideboard().set(readCardList(sections.get("sideboard")));

        return d;
    }

    private static List<String> readCardList(final List<String> lines) {
        final List<String> result = new ArrayList<String>();
        final Pattern p = Pattern.compile("((\\d+)\\s+)?(.*?)");

        if (lines == null) {
            return result;
        }

        final Iterator<String> lineIterator = lines.iterator();
        while (lineIterator.hasNext()) {
            final String line = lineIterator.next();
            if (line.startsWith("[")) {
                break;
            } // there comes another section

            final Matcher m = p.matcher(line.trim());
            m.matches();
            final String sCnt = m.group(2);
            final String cardName = m.group(3);
            if (StringUtils.isBlank(cardName)) {
                continue;
            }

            final int count = sCnt == null ? 1 : Integer.parseInt(sCnt);
            for (int i = 0; i < count; i++) {
                result.add(cardName);
            }
        }
        return result;
    }

    private static List<String> writeCardPool(final ItemPoolView<CardPrinted> pool) {
        final List<Entry<CardPrinted, Integer>> main2sort = pool.getOrderedList();
        Collections.sort(main2sort, TableSorter.BY_NAME_THEN_SET);
        final List<String> out = new ArrayList<String>();
        for (final Entry<CardPrinted, Integer> e : main2sort) {
            final CardPrinted card = e.getKey();
            final boolean hasBadSetInfo = "???".equals(card.getSet()) || StringUtils.isBlank(card.getSet());
            if (hasBadSetInfo) {
                out.add(String.format("%d %s", e.getValue(), card.getName()));
            } else {
                out.add(String.format("%d %s|%s", e.getValue(), card.getName(), card.getSet()));
            }
        }
        return out;
    }



    /**
     * <p>
     * writeDeck.
     * </p>
     * 
     * @param d
     *            a {@link forge.deck.Deck} object.
     * @param out
     *            a {@link java.io.BufferedWriter} object.
     * @throws java.io.IOException
     *             if any.
     */
    public List<String> save() {


        final List<String> out = new ArrayList<String>();
        out.add(String.format("[metadata]"));

        out.add(String.format("%s=%s", DeckFileHeader.NAME, getName().replaceAll("\n", "")));
        // these are optional
        if (getComment() != null) {
            out.add(String.format("%s=%s", DeckFileHeader.COMMENT, getComment().replaceAll("\n", "")));
        }

        out.add(String.format("%s", "[main]"));
        out.addAll(writeCardPool(getMain()));

        out.add(String.format("%s", "[sideboard]"));
        out.addAll(writeCardPool(getSideboard()));
        return out;
    }
}
