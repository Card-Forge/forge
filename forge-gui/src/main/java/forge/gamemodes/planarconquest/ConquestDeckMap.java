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
package forge.gamemodes.planarconquest;

import java.util.Map;

import forge.deck.Deck;
import forge.util.storage.StorageBase;

public class ConquestDeckMap extends StorageBase<Deck> {
    public ConquestDeckMap(Map<String, Deck> in) {
        super("Conquest decks", null, in);
    }

    @Override
    public void add(final Deck deck) {
        map.put(deck.getName(), deck);
    }

    @Override
    public void delete(final String deckName) {
        map.remove(deckName);
    }
}
