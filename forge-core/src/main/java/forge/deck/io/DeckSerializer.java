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
import java.util.Map;


import org.apache.commons.lang3.StringUtils;

import forge.deck.Deck;
import forge.util.FileSection;
import forge.util.FileSectionManual;
import forge.util.FileUtil;
import forge.util.IItemReader;
import forge.util.IItemSerializer;
import forge.util.storage.StorageReaderFolder;

/**
 * This class knows how to make a file out of a deck object and vice versa.
 */
public class DeckSerializer extends StorageReaderFolder<Deck> implements IItemSerializer<Deck> {
    private final boolean moveWronglyNamedDecks;
    public static final String FILE_EXTENSION = ".dck";

    public DeckSerializer(final File deckDir0) {
        this(deckDir0, false);
    }

    public DeckSerializer(final File deckDir0, boolean moveWrongDecks) {
        super(deckDir0, Deck.FN_NAME_SELECTOR);
        moveWronglyNamedDecks = moveWrongDecks;
    }

    /** Constant <code>DCKFileFilter</code>. */
    public static final FilenameFilter DCK_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
            return name.endsWith(FILE_EXTENSION);
        }
    };
public static void writeDeck(final Deck d, final File f) {
        FileUtil.writeFile(f, d.save());
    }

    @Override
    public void save(final Deck unit) {
        FileUtil.writeFile(this.makeFileFor(unit), unit.save());
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
        Deck result = Deck.fromSections(sections, true);

        if (moveWronglyNamedDecks) {
            adjustFileLocation(file, result);
        }
        return result;
    }

    private void adjustFileLocation(final File file, final Deck result) {
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
        return DeckSerializer.DCK_FILE_FILTER;
    }

    public static DeckFileHeader readDeckMetadata(final Map<String, List<String>> map, final boolean canThrow) {
        if (map == null) {
            return null;
        }
        final List<String> metadata = map.get("metadata");
        if (metadata != null) {
            return new DeckFileHeader(FileSection.parse(metadata, "="));
        }
        final List<String> general = map.get("general");
        if (general != null) {
            if (canThrow) {
                throw new OldDeckFileFormatException();
            }
            final FileSectionManual fs = new FileSectionManual();
            fs.put(DeckFileHeader.NAME, StringUtils.join(map.get(""), " "));
            fs.put(DeckFileHeader.DECK_TYPE, StringUtils.join(general, " "));
            return new DeckFileHeader(fs);
        }

        return null;
    }
    
    /* (non-Javadoc)
     * @see forge.util.storage.StorageReaderBase#getReaderForFolder(java.io.File)
     */
    @Override
    public IItemReader<Deck> getReaderForFolder(File subfolder) {
        if ( !subfolder.getParentFile().equals(directory) )
            throw new UnsupportedOperationException("Only child folders of " + directory + " may be processed");
        return new DeckSerializer(subfolder, false);
    }
}
