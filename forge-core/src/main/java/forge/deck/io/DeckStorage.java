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

import forge.deck.Deck;
import forge.util.FileSection;
import forge.util.FileUtil;
import forge.util.IItemReader;
import forge.util.IItemSerializer;
import forge.util.storage.StorageReaderFolder;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Map;

/**
 * This class knows how to make a file out of a deck object and vice versa.
 */
public class DeckStorage extends StorageReaderFolder<Deck> implements IItemSerializer<Deck> {
    private final boolean moveWronglyNamedDecks;
    public static final String FILE_EXTENSION = ".dck";

    /** Constant <code>DCKFileFilter</code>. */
    public static final FilenameFilter DCK_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
            return name.endsWith(FILE_EXTENSION);
        }
    };

    public DeckStorage(final File deckDir0) {
        this(deckDir0, false);
    }

    public DeckStorage(final File deckDir0, boolean moveWrongDecks) {
        super(deckDir0, Deck.FN_NAME_SELECTOR);
        moveWronglyNamedDecks = moveWrongDecks;
    }

    /* (non-Javadoc)
     * @see forge.util.storage.StorageReaderBase#getReaderForFolder(java.io.File)
     */
    @Override
    public IItemReader<Deck> getReaderForFolder(File subfolder) {
        if ( !subfolder.getParentFile().equals(directory) )
            throw new UnsupportedOperationException("Only child folders of " + directory + " may be processed");
        return new DeckStorage(subfolder, false);
    }

    @Override
    public void save(final Deck unit) {
        DeckSerializer.writeDeck(unit, this.makeFileFor(unit));
    }
    
    @Override
    public void erase(final Deck unit) {
        this.makeFileFor(unit).delete();
    }

    public File makeFileFor(final Deck deck) {
        return new File(this.directory, deck.getBestFileName() + FILE_EXTENSION);
    }

    @Override
    protected Deck read(final File file) {
        final Map<String, List<String>> sections = FileSection.parseSections(FileUtil.readFile(file));
        Deck result = DeckSerializer.fromSections(sections);

        if (moveWronglyNamedDecks) {
            adjustFileLocation(file, result);
        }
        return result;
    }

    private static void adjustFileLocation(final File file, final Deck result) {
        if (result == null) {
            file.delete();
        } else {
            String destFilename = result.getBestFileName() + FILE_EXTENSION;
            if (!file.getName().equals(destFilename)) {
                file.renameTo(new File(file.getParentFile().getParentFile(), destFilename));
            }
        }
    }

    @Override
    protected FilenameFilter getFileFilter() {
        return DCK_FILE_FILTER;
    }
}

