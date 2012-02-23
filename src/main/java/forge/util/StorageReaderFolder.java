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
package forge.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;

import forge.deck.io.OldDeckFileFormatException;
import forge.error.ErrorViewer;

/**
 * This class treats every file in the given folder as a source for a named object.
 * The descendant should implement read method to deserialize a single item.
 * So that readAll will return a map of Name => Object as read from disk
 * 
 */
public abstract class StorageReaderFolder<T extends IHasName> implements IItemReader<T> {

    private final File directory;

    protected final File getDirectory() {
        return directory;
    }


    public StorageReaderFolder(File deckDir0) {

        directory = deckDir0;

        if (directory == null) {
            throw new IllegalArgumentException("No deck directory specified");
        }
        try {
            if (directory.isFile()) {
                throw new IOException("Not a directory");
            } else {
                directory.mkdirs();
                if (!directory.isDirectory()) {
                    throw new IOException("Directory can't be created");
                }
            }
        } catch (final IOException ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("DeckManager : writeDeck() error, " + ex.getMessage());
        }
    }

    @Override
    public Map<String, T> readAll() {
        final Map<String, T> result = new TreeMap<String, T>();
        final List<String> decksThatFailedToLoad = new ArrayList<String>();
        final File[] files = directory.listFiles(getFileFilter());
        boolean hasWarnedOfOldFormat = false;
        for (final File file : files) {
            try {
                final T newDeck = read(file);
                if (null == newDeck) {
                    String msg =  "An object stored in " + file.getPath() + " failed to load.\nPlease submit this as a bug with the mentioned file/directory attached.";
                    JOptionPane.showMessageDialog(null, msg);
                    continue;
                }
                result.put(newDeck.getName(), newDeck);
            } catch (final OldDeckFileFormatException ex ) {
                if( !hasWarnedOfOldFormat ) {
                    JOptionPane.showMessageDialog(null, "Found a deck in old fileformat in the storage.\nMoving this file and all similiar ones to parent folder.\n\nForge will try to convert them in a second.");
                    hasWarnedOfOldFormat = true;
                }
                file.renameTo(new File(directory.getParentFile(), file.getName()));
            } catch (final NoSuchElementException ex) {
                final String message = String.format("%s failed to load because ---- %s", file.getName(),
                        ex.getMessage());
                decksThatFailedToLoad.add(message);
            }
        }

        if (!decksThatFailedToLoad.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    StringUtils.join(decksThatFailedToLoad, System.getProperty("line.separator")),
                    "Some of your objects were not loaded.", JOptionPane.WARNING_MESSAGE);
        }

        return result;
    }


    /**
     * Read the object from file.
     * @param file
     * @return the object deserialized by inherited class
     */
    protected abstract T read(File file);


    /**
     * TODO: Write javadoc for this method.
     * @return FilenameFilter to pick only relevant objects for deserialization
     */
    protected abstract FilenameFilter getFileFilter();

}
