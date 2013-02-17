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
package forge.util.storage;

import forge.util.IItemSerializer;

//reads and writeDeck Deck objects
/**
 * <p>
 * DeckManager class.
 * </p>
 *
 * @param <T> the generic type
 * @author Forge
 * @version $Id$
 */
public class StorageImmediatelySerialized<T> extends StorageView<T> implements IStorage<T> {

    private final IItemSerializer<T> serializer;
    /**
     * <p>
     * Constructor for DeckManager.
     * </p>
     *
     * @param io the io
     */
    public StorageImmediatelySerialized(final IItemSerializer<T> io) {
        super(io);
        this.serializer = io;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.deck.IFolderMap#add(T)
     */
    @Override
    public final void add(final T deck) {
        String name = serializer.getItemKey(deck);
        this.getMap().put(name, deck);
        this.serializer.save(deck);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.deck.IFolderMap#delete(java.lang.String)
     */
    @Override
    public final void delete(final String deckName) {
        this.serializer.erase(this.getMap().remove(deckName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.deck.IFolderMapView#isUnique(java.lang.String)
     */
    @Override
    public final boolean isUnique(final String name) {
        return !this.getMap().containsKey(name);
    }

}
