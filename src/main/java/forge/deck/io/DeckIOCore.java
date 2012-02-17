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
package forge.deck.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.filechooser.FileFilter;

import org.apache.commons.lang3.StringUtils;

import forge.deck.Deck;
import forge.game.GameType;
import forge.gui.deckeditor.elements.TableSorter;
import forge.item.CardPrinted;
import forge.item.ItemPoolView;
import forge.util.FileUtil;
import forge.util.SectionUtil;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class DeckIOCore {

    private static final String NAME = "Name";
    private static final String DECK_TYPE = "Deck Type";
    private static final String COMMENT = "Comment";
    /** Constant <code>DCKFileFilter</code>. */
    public static final FilenameFilter DCK_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
            return name.endsWith(".dck");
        }
    };
    /** The Constant DCK_FILTER. */
    public static final FileFilter DCK_FILTER = new FileFilter() {
        @Override
        public boolean accept(final File f) {
            return f.getName().endsWith(".dck") || f.isDirectory();
        }

        @Override
        public String getDescription() {
            return "Simple Deck File .dck";
        }
    };
    /** The Constant HTML_FILTER. */
    public static final FileFilter HTML_FILTER = new FileFilter() {
        @Override
        public boolean accept(final File f) {
            return f.getName().endsWith(".html") || f.isDirectory();
        }

        @Override
        public String getDescription() {
            return "Simple Deck File .html";
        }
    };


    public static DeckFileHeader readDeckMetadata(final Map<String,List<String>> map) {
        List<String> lines = map.get("metadata");
        if (lines == null) {
            return null;
        }
        DeckFileHeader d = new DeckFileHeader(SectionUtil.parseKvPairs(lines, "="));
        

        return d;
    }


    // Precondition: iterator should point at the first line of cards list
    public static List<String> readCardList(final Iterable<String> lines) {
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

    public static String deriveFileName(final String deckName) {
        // skips all but the listed characters
        return deckName.replaceAll("[^-_$#@.{[()]} a-zA-Z0-9]", "");
    }

    // only accepts numbers, letters or dashes up to 20 characters in length
    /**
     * 
     * Clean deck name.
     * 
     * @param in
     *            a String
     * @return a String
     */
    public static String cleanDeckName(final String in) {
        final char[] c = in.toCharArray();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; (i < c.length) && (i < 20); i++) {
            if (Character.isLetterOrDigit(c[i]) || (c[i] == '-')) {
                sb.append(c[i]);
            }
        }
        return sb.toString();
    }

    public static List<String> writeCardPool(final ItemPoolView<CardPrinted> pool) {
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
     * readDeck.
     * </p>
     * 
     * @param deckFile
     *            a {@link java.io.File} object.
     * @return a {@link forge.deck.Deck} object.
     */
    
    public static Deck readDeck(final File deckFile) {
        return readDeck(FileUtil.readFile(deckFile));
    }


    /**
     * Read deck.
     *
     * @param deckFileLines the deck file lines
     * @return the deck
     */
    public static Deck readDeck(final List<String> deckFileLines) {
        final Map<String, List<String>> sections = SectionUtil.parseSections(deckFileLines);
        if (sections.isEmpty()) {
            return null;
        }
        
        DeckFileHeader dh = readDeckMetadata(sections);
        
        final Deck d = new Deck(dh.getName());
        d.setComment(dh.getComment());
        d.getMain().set(readCardList(sections.get("main")));
        d.getSideboard().set(readCardList(sections.get("sideboard")));
    
        return d;
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
    public static List<String> saveDeck(final Deck d) {
        final List<String> out = new ArrayList<String>();
        out.add(String.format("[metadata]"));
    
        out.add(String.format("%s=%s", NAME, d.getName().replaceAll("\n", "")));
        // these are optional
        if (d.getComment() != null) {
            out.add(String.format("%s=%s", COMMENT, d.getComment().replaceAll("\n", "")));
        }
    
        out.add(String.format("%s", "[main]"));
        out.addAll(writeCardPool(d.getMain()));
    
        out.add(String.format("%s", "[sideboard]"));
        out.addAll(writeCardPool(d.getSideboard()));
        return out;
    }



}
