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
package forge.quest.io;

import java.io.File;
import java.io.FilenameFilter;

import forge.deck.io.DeckSerializer;
import forge.item.PreconDeck;
import forge.util.storage.StorageReaderFolder;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class PreconReader extends StorageReaderFolder<PreconDeck> {

    /**
     * TODO: Write javadoc for Constructor.
     *
     * @param deckDir0 the deck dir0
     */
    public PreconReader(final File deckDir0) {
        super(deckDir0, PreconDeck.FN_NAME_SELECTOR);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.deck.io.DeckSerializerBase#read(java.io.File)
     */
    @Override
    protected PreconDeck read(final File file) {
        return new PreconDeck(file);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.deck.io.DeckSerializerBase#getFileFilter()
     */
    @Override
    protected FilenameFilter getFileFilter() {
        return DeckSerializer.DCK_FILE_FILTER;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.util.IItemReader#readAll()
     */

}
