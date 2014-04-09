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

import forge.deck.Deck;
import forge.util.storage.StorageBase;

import java.util.Map;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class QuestDeckMap extends StorageBase<Deck> {

    /**
     * Instantiates a new quest deck map.
     */
    public QuestDeckMap(Map<String, Deck> in) {
        super("Quest decks", in);
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

}
