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

import forge.deck.CustomLimited;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class CubeSerializer extends DeckSerializerBase<CustomLimited> {

    public CubeSerializer(File deckDir0)
    {
        super(deckDir0);
    }
    

    /* (non-Javadoc)
     * @see forge.deck.IDeckSerializer#save(forge.item.CardCollectionBase, java.io.File)
     */
    @Override
    public void save(CustomLimited unit) {
       
    }

    /* (non-Javadoc)
     * @see forge.deck.IDeckSerializer#erase(forge.item.CardCollectionBase, java.io.File)
     */
    @Override
    public void erase(CustomLimited unit) {
    }



    /**
     * 
     * Make file name.
     * 
     * @param deckName
     * @param deckType
     *            a GameType
     * @return a File
     */
    public File makeFileFor(final CustomLimited deck) {
        return new File(getDirectory(), deriveFileName(cleanDeckName(deck.getName())) + ".cub");
    }

    /* (non-Javadoc)
     * @see forge.deck.io.DeckSerializerBase#read(java.io.File)
     */
    @Override
    protected CustomLimited read(File file) {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.deck.io.DeckSerializerBase#getFileFilter()
     */
    @Override
    protected FilenameFilter getFileFilter() {
        return new FilenameFilter() {
            
            @Override
            public boolean accept(File dir, String name) {
                return dir.getPath().endsWith(".cub");
            }
        };
    }

}
