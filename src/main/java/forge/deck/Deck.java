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
import forge.gui.deckeditor.tables.TableSorter;
import forge.item.CardPrinted;
import forge.item.ItemPoolView;
import forge.game.limited.ReadDraftRankings;
import forge.util.FileSection;
import forge.util.FileUtil;
import forge.util.closures.Lambda1;

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
public class Deck extends DeckBase implements Serializable {
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
    public Deck() {
        this("");
    }

    /**
     * Instantiates a new deck.
     *
     * @param name0 the name0
     */
    public Deck(final String name0) {
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

    /*
     * (non-Javadoc)
     * 
     * @see forge.item.CardCollectionBase#getCardPool()
     */
    @Override
    public ItemPoolView<CardPrinted> getCardPool() {
        return this.main;
    }

    /* (non-Javadoc)
     * @see forge.deck.DeckBase#cloneFieldsTo(forge.deck.DeckBase)
     */
    @Override
    protected void cloneFieldsTo(final DeckBase clone) {
        super.cloneFieldsTo(clone);
        final Deck result = (Deck) clone;
        result.main.addAll(this.main);
        result.sideboard.addAll(this.sideboard);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.deck.DeckBase#newInstance(java.lang.String)
     */
    @Override
    protected DeckBase newInstance(final String name0) {
        return new Deck(name0);
    }

    /**
     * From file.
     *
     * @param deckFile the deck file
     * @return the deck
     */
    public static Deck fromFile(final File deckFile) {
        return Deck.fromSections(FileSection.parseSections(FileUtil.readFile(deckFile)));
    }

    /**
     * From sections.
     *
     * @param sections the sections
     * @return the deck
     */
    public static Deck fromSections(final Map<String, List<String>> sections) {
        return Deck.fromSections(sections, false);
    }

    /**
     * From sections.
     *
     * @param sections the sections
     * @param canThrowExtendedErrors the can throw extended errors
     * @return the deck
     */
    public static Deck fromSections(final Map<String, List<String>> sections, final boolean canThrowExtendedErrors) {
        if ((sections == null) || sections.isEmpty()) {
            return null;
        }

        final DeckFileHeader dh = DeckSerializer.readDeckMetadata(sections, canThrowExtendedErrors);
        if (dh == null) {
            return null;
        }

        final Deck d = new Deck(dh.getName());
        d.setComment(dh.getComment());

        d.getMain().set(Deck.readCardList(sections.get("main")));
        d.getSideboard().set(Deck.readCardList(sections.get("sideboard")));

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
            if (line.startsWith(";")) { continue; } // that is a comment or not-yet-supported card
            if (line.startsWith("[")) { break; } // there comes another section

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
            final boolean hasBadSetInfo = "???".equals(card.getEdition()) || StringUtils.isBlank(card.getEdition());
            if (hasBadSetInfo) {
                out.add(String.format("%d %s", e.getValue(), card.getName()));
            } else {
                out.add(String.format("%d %s|%s", e.getValue(), card.getName(), card.getEdition()));
            }
        }
        return out;
    }

    /**
     * <p>
     * writeDeck.
     * </p>
     *
     * @return the list
     */
    public List<String> save() {

        final List<String> out = new ArrayList<String>();
        out.add(String.format("[metadata]"));

        out.add(String.format("%s=%s", DeckFileHeader.NAME, this.getName().replaceAll("\n", "")));
        // these are optional
        if (this.getComment() != null) {
            out.add(String.format("%s=%s", DeckFileHeader.COMMENT, this.getComment().replaceAll("\n", "")));
        }

        out.add(String.format("%s", "[main]"));
        out.addAll(Deck.writeCardPool(this.getMain()));

        out.add(String.format("%s", "[sideboard]"));
        out.addAll(Deck.writeCardPool(this.getSideboard()));
        return out;
    }

    /**
     * <p>
     * getDraftValue.
     * </p>
     *
     * @return the combined draft values of cards in the main deck
     */
    public double getDraftValue() {

        double value = 0;
        double divider = 0;

        ReadDraftRankings ranker = new ReadDraftRankings();

        if (this.getMain().isEmpty()) {
            return 0;
        }

        double best = 1.0;

        for( Entry<CardPrinted, Integer> kv : this.getMain()) {
            CardPrinted evalCard = kv.getKey();
            int count = kv.getValue();
            if (ranker.getRanking(evalCard.getName(), evalCard.getEdition()) != null) {
                double add = ranker.getRanking(evalCard.getName(), evalCard.getEdition());
                // System.out.println(evalCard.getName() + " is worth " + add);
                value += add * count;
                divider += count;
                if (best > add) {
                    best = add;
                }
            }
        }

        if (divider == 0 || value == 0) {
            return 0;
        }

        value /= divider;

        return (20.0 / (best + (2 * value)));
    }


    public static final Lambda1<String, Deck> FN_NAME_SELECTOR = new Lambda1<String, Deck>() {
        @Override
        public String apply(Deck arg1) {
            return arg1.getName();
        }
    };
}
