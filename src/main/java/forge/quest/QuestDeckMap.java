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
package forge.quest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import forge.deck.Deck;
import forge.util.storage.IStorage;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class QuestDeckMap implements IStorage<Deck> {

    /**
     * Instantiates a new quest deck map.
     */
    public QuestDeckMap() {
        this.map = new HashMap<String, Deck>();
    }

    /**
     * Instantiates a new quest deck map.
     *
     * @param inMap the in map
     */
    public QuestDeckMap(final Map<String, Deck> inMap) {
        this.map = inMap;
    }

    private final Map<String, Deck> map;

    /*
     * (non-Javadoc)
     * 
     * @see forge.util.IFolderMapView#get(java.lang.String)
     */
    @Override
    public Deck get(final String name) {
        return this.map.get(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.util.IFolderMapView#getNames()
     */
    @Override
    public Collection<String> getNames() {
        return this.map.keySet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Deck> iterator() {
        return this.map.values().iterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.util.IFolderMap#add(forge.util.IHasName)
     */
    @Override
    public void add(final Deck deck) {
        this.map.put(deck.getName(), deck);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.util.IFolderMap#delete(java.lang.String)
     */
    @Override
    public void delete(final String deckName) {
        this.map.remove(deckName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.util.IFolderMap#isUnique(java.lang.String)
     */
    @Override
    public boolean isUnique(final String name) {
        return !this.map.containsKey(name);
    }

    /* (non-Javadoc)
     * @see forge.util.IFolderMapView#any(java.lang.String)
     */
    @Override
    public boolean contains(String name) {
        return map.containsKey(name);
    }

    /* (non-Javadoc)
     * @see forge.util.IStorageView#getCount()
     */
    @Override
    public int getCount() {
        return map.size();
    }

}
