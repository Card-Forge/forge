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
import forge.deck.DeckBase;
import forge.util.FileSection;
import forge.util.FileUtil;
import forge.util.IItemReader;
import forge.util.IItemSerializer;
import forge.util.storage.StorageReaderFolder;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class knows how to make a file out of a deck object and vice versa.
 */
public class DeckStorage extends StorageReaderFolder<Deck> implements IItemSerializer<Deck> {
    public static final String FILE_EXTENSION = ".dck";

    private final String rootDir;
    private final boolean moveWronglyNamedDecks;

    /** Constant <code>DCKFileFilter</code>. */
    public static final FilenameFilter DCK_FILE_FILTER = (dir, name) -> name.endsWith(FILE_EXTENSION);

    public DeckStorage(final File deckDir0, final String rootDir0) {
        this(deckDir0, rootDir0, false);
    }

    public DeckStorage(final File deckDir0, final String rootDir0, boolean moveWrongDecks) {
        super(deckDir0, DeckBase::getName);
        rootDir = rootDir0;
        moveWronglyNamedDecks = moveWrongDecks;
    }

    /* (non-Javadoc)
     * @see forge.util.storage.StorageReaderBase#getReaderForFolder(java.io.File)
     */
    @Override
    public IItemReader<Deck> getReaderForFolder(File subfolder) {
        if ( !subfolder.getParentFile().equals(directory) )
            throw new UnsupportedOperationException("Only child folders of " + directory + " may be processed");
        return new DeckStorage(subfolder, rootDir, false);
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

    public boolean saveMetadata(final Deck deck) {
        final File file = makeFileFor(deck);
        if (!file.exists()) {
            return false;
        }
        try {
            final List<String> lines = FileUtil.readFile(file);
            final int metadataStart = findMetadataSection(lines);
            if (metadataStart < 0) {
                return false;
            }

            final List<String> updatedLines = new ArrayList<>(lines);
            int insertAt = metadataStart + 1;
            for (int i = metadataStart + 1; i < updatedLines.size(); i++) {
                final String line = updatedLines.get(i).trim();
                if (line.startsWith("[") && line.endsWith("]")) {
                    break;
                }
                if (isMetadataLine(line, DeckFileHeader.DECK_HASH) || isMetadataLine(line, DeckFileHeader.COMMANDER_BRACKET)) {
                    updatedLines.remove(i--);
                    continue;
                }
                if (isMetadataLine(line, DeckFileHeader.NAME) || isMetadataLine(line, DeckFileHeader.COMMENT)) {
                    insertAt = i + 1;
                }
            }
            if (StringUtils.isNotBlank(deck.getDeckHash()) && deck.getCommanderBracket() != null) {
                updatedLines.add(insertAt, DeckFileHeader.DECK_HASH + "=" + deck.getDeckHash());
                updatedLines.add(insertAt + 1, DeckFileHeader.COMMANDER_BRACKET + "=" + deck.getCommanderBracket());
            }
            FileUtil.writeFile(file, updatedLines);
            return true;
        }
        catch (final RuntimeException ignored) {
            return false;
        }
    }

    @Override
    protected Deck read(final File file) {
        final Map<String, List<String>> sections = FileSection.parseSections(FileUtil.readFile(file));
        Deck result = DeckSerializer.fromSections(sections);

        if (moveWronglyNamedDecks) {
            adjustFileLocation(file, result);
        }

        if (result != null) {
            result.setDirectory(file.getParent().substring(rootDir.length()));
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

    private static int findMetadataSection(final List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if ("[metadata]".equalsIgnoreCase(lines.get(i).trim())) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isMetadataLine(final String line, final String key) {
        return line.regionMatches(true, 0, key + "=", 0, key.length() + 1);
    }
}
