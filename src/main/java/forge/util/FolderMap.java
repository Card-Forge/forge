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
package forge.util;


//reads and writeDeck Deck objects
/**
 * <p>
 * DeckManager class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class FolderMap<T extends IHasName> extends FolderMapView<T> implements IFolderMap<T> {

    private IItemSerializer<T> serializer;
    /**
     * <p>
     * Constructor for DeckManager.
     * </p>
     * 
     * @param deckDir
     *            a {@link java.io.File} object.
     */
    public FolderMap(IItemSerializer<T> io ) {
        super(io);
        serializer = io;
    }



    /* (non-Javadoc)
     * @see forge.deck.IFolderMap#add(T)
     */
    @Override
    public final void add(final T deck) {
        this.getMap().put(deck.getName(), deck);
        serializer.save(deck);
    }

    /* (non-Javadoc)
     * @see forge.deck.IFolderMap#delete(java.lang.String)
     */
    @Override
    public final void delete(final String deckName) {
        serializer.erase(this.getMap().remove(deckName));
    }
    
    


}
