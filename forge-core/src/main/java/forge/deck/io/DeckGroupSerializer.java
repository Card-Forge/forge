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
package forge.deck.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;

import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.util.IItemSerializer;
import forge.util.storage.StorageReaderFolder;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class DeckGroupSerializer extends StorageReaderFolder<DeckGroup> implements IItemSerializer<DeckGroup> {
    private static final String humanDeckFile = "human.dck";

    private final String rootDir;

    /**
     * Instantiates a new deck group serializer.
     *
     * @param deckDir0 the deck dir0
     */
    public DeckGroupSerializer(final File deckDir0, String rootDir0) {
        super(deckDir0, DeckGroup.FN_NAME_SELECTOR);
        rootDir = rootDir0;
    }

    /** The Constant MAX_DRAFT_PLAYERS. */
    public static final int MAX_DRAFT_PLAYERS = 8;

    /**
     * Write draft Decks.
     *
     * @param unit the unit
     */
    @Override
    public void save(final DeckGroup unit) {
        final File f = makeFileFor(unit);
        f.mkdir();
        DeckSerializer.writeDeck(unit.getHumanDeck(), new File(f, humanDeckFile));
        final List<Deck> aiDecks = unit.getAiDecks();
        for (int i = 1; i <= aiDecks.size(); i++) {
            DeckSerializer.writeDeck(aiDecks.get(i - 1), new File(f, "ai-" + i + ".dck"));
        }
    }

    /* (non-Javadoc)
     * @see forge.util.StorageReaderFolder#read(java.io.File)
     */
    @Override
    protected final DeckGroup read(final File file) {
        final Deck humanDeck = DeckSerializer.fromFile(new File(file, humanDeckFile));
        if (humanDeck == null) { return null; }

        final DeckGroup d = new DeckGroup(humanDeck.getName());
        d.setDirectory(file.getParent().substring(rootDir.length()));
        d.setHumanDeck(humanDeck);
        for (int i = 1; i < DeckGroupSerializer.MAX_DRAFT_PLAYERS; i++) {
            final File theFile = new File(file, "ai-" + i + ".dck");
            if (!theFile.exists()) {
                break;
            }
            d.addAiDeck(DeckSerializer.fromFile(theFile));
        }
        return d;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.deck.IDeckSerializer#erase(forge.item.CardCollectionBase,
     * java.io.File)
     */
    @Override
    public void erase(final DeckGroup unit) {
        final File dir = makeFileFor(unit);
        final File[] files = dir.listFiles();
        for (final File f : files) {
            f.delete();
        }
        dir.delete();
    }

    /**
     * Make file for.
     *
     * @param decks the decks
     * @return the file
     */
    public File makeFileFor(final DeckGroup decks) {
        return new File(directory, decks.getBestFileName());
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.deck.io.DeckSerializerBase#getFileFilter()
     */
    @Override
    protected FilenameFilter getFileFilter() {
        return new FilenameFilter() {

            @Override
            public boolean accept(final File dir, final String name) {
                final File testSubject = new File(dir, name);
                final boolean isVisibleFolder = testSubject.isDirectory() && !testSubject.isHidden();
                final boolean hasGoodName = StringUtils.isNotEmpty(name) && !name.startsWith(".");
                final File fileHumanDeck = new File(testSubject, DeckGroupSerializer.humanDeckFile);
                return isVisibleFolder && hasGoodName && fileHumanDeck.exists();
            }
        };
    }

    @Override
    public Iterable<File> getSubFolders() {
        // Sealed decks are kept in separate folders, no further drilling possible
        return ImmutableList.of();
    }

}
