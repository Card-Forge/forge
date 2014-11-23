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
package forge.planarconquest;

import com.google.common.eventbus.Subscribe;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.game.event.GameEvent;
import forge.util.storage.IStorage;

public class ConquestController {
    private ConquestData model;
    private CardPool cardPool;
    private transient IStorage<Deck> decks;

    public ConquestController() {
    }

    public String getName() {
        return model == null ? null : model.getName();
    }

    public String getCurrentPlane() {
        return model == null ? null : model.getCurrentPlane().getName();
    }

    public CardPool getCardPool() {
        return cardPool;
    }

    public IStorage<Deck> getDecks() {
        return decks;
    }

    public void load(final ConquestData model0) {
        model = model0;
        cardPool = model == null ? null : model.getCardPool();
        decks = model == null ? null : model.getDeckStorage();
    }

    public void save() {
        if (model != null) {
            model.saveData();
        }
    }

    @Subscribe
    public void receiveGameEvent(GameEvent ev) { // Receives events only during planar conquest games

    }
}
