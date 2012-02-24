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
package forge.gui.deckeditor;

import java.awt.Component;
import java.util.List;

/**
 * TODO: Write javadoc for this type.
 *
 * @param <T> the generic type
 */
public interface IDeckController<T> {

    /**
     * New model.
     */
    void newModel();

    /**
     * Sets the model.
     *
     * @param model the new model
     */
    void setModel(T model);

    /**
     * Gets the model.
     *
     * @return the model
     */
    T getModel();

    /** Call this anytime model becomes different from the saved on disk state. */
    void notifyModelChanged();

    /**
     * Gets the owner window.
     *
     * @return the owner window
     */
    Component getOwnerWindow();

    /**
     * Gets the view.
     *
     * @return the view
     */
    DeckEditorBase<?, T> getView();

    /**
     * Gets names of saved models in folder / questData.
     *
     * @return the saved names
     */
    List<String> getSavedNames();

    /**
     * Load.
     *
     * @param name the name
     */
    void load(String name);

    /**
     * Save.
     */
    void save();

    /**
     * Save as.
     *
     * @param name0 the name0
     */
    void saveAs(String name0);

    /**
     * Checks if is saved.
     *
     * @return true, if is saved
     */
    boolean isSaved();

    /**
     * Delete.
     */
    void delete();

    /**
     * Returns true if no object exists with that name.
     *
     * @param deckName the deck name
     * @return true, if is good name
     */
    boolean isGoodName(String deckName);

    /**
     * Import in quest adds add cards to pool, unlike constructed.
     *
     * @param newDeck the new deck
     */
    void importDeck(T newDeck);

    /**
     * Tells if this deck was already saved to disk / questData.
     *
     * @return true, if is model in store
     */
    boolean isModelInStore();

    /**
     * TODO: Write javadoc for this method.
     *
     * @param deckName the deck name
     * @return true, if successful
     */
    boolean fileExists(String deckName);

    /*
     * // IMPORT DECK CODE this.questData.addDeck(newDeck);
     * 
     * final ItemPool<CardPrinted> cardpool =
     * ItemPool.createFrom(this.questData.getCards().getCardpool(),
     * CardPrinted.class); final ItemPool<CardPrinted> decklist = new
     * ItemPool<CardPrinted>(CardPrinted.class); for (final Entry<CardPrinted,
     * Integer> s : newDeck.getMain()) { final CardPrinted cp = s.getKey();
     * decklist.add(cp, s.getValue()); cardpool.add(cp, s.getValue());
     * this.questData.getCards().getCardpool().add(cp, s.getValue()); }
     * this.controller.showItems(cardpool, decklist);
     */
}
