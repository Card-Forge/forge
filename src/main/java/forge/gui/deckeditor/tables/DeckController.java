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
package forge.gui.deckeditor.tables;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Supplier;

import forge.deck.DeckBase;
import forge.gui.deckeditor.controllers.ACEditorBase;
import forge.gui.deckeditor.controllers.CProbabilities;
import forge.gui.deckeditor.controllers.CStatistics;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.util.storage.IStorage;

/**
 * TODO: Write javadoc for this type.
 *
 * @param <T> the generic type
 */
public class DeckController<T extends DeckBase> {

    private T model;
    private boolean saved;
    private boolean modelInStore;
    private final IStorage<T> folder;
    private final ACEditorBase<?, T> view;
    private final Supplier<T> newModelCreator;

    /**
     * Instantiates a new deck controller.
     *
     * @param folder0 the folder0
     * @param view0 the view0
     * @param newModelCreator0 the new model creator0
     */
    public DeckController(final IStorage<T> folder0, final ACEditorBase<?, T> view0,
            final Supplier<T> newModelCreator0) {
        this.folder = folder0;
        this.view = view0;
        this.model = null;
        this.saved = true;
        this.modelInStore = false;
        this.newModelCreator = newModelCreator0;
    }

    /**
     * Gets the model.
     *
     * @return the document
     */
    public T getModel() {
        return this.model;
    }

    /**
     * Sets the model.
     *
     * @param document the new model
     */
    public void setModel(final T document) {
        this.setModel(document, false);
    }

    /**
     * Sets the model.
     *
     * @param document the document
     * @param isStored the is stored
     */
    public void setModel(final T document, final boolean isStored) {
        this.modelInStore = isStored;
        this.model = document;
        this.view.resetTables();

        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setText(model.getName());
        CStatistics.SINGLETON_INSTANCE.update();
        CProbabilities.SINGLETON_INSTANCE.update();

        if (this.isModelInSyncWithFolder()) {
            _setSaved(true);
        } else {
            this.notifyModelChanged();
        }
    }

    private boolean isModelInSyncWithFolder() {
        if (model.getName().isEmpty()) {
            return true;
        }
        
        final T modelStored = this.folder.get(this.model.getName());
        // checks presence in dictionary only.
        if (modelStored == this.model) {
            return true;
        }
        if (null == modelStored) {
            return false;
        }

        return modelStored.equals(this.model);
    }

    /**
     * Gets the view.
     *
     * @return the view
     */
    public ACEditorBase<?, T> getView() {
        return this.view;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#notifyModelChanged()
     */
    /**
     * Notify model changed.
     */
    public void notifyModelChanged() {
        _setSaved(false);
    }

    private void _setSaved(boolean val) {
        saved = val;
        VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText((saved ? "" : "*") + "Current Deck");
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#getSavedModelNames()
     */
    /**
     * Gets the saved names.
     *
     * @return the saved names
     */
    public ArrayList<String> getSavedNames() {
        return new ArrayList<String>(this.folder.getNames());
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#load(java.lang.String)
     */
    /**
     * Load.
     *
     * @param name the name
     */
    @SuppressWarnings("unchecked")
    public void load(final String name) {
        T newModel = this.folder.get(name);
        if (null != newModel) {
            this.setModel((T) newModel.copyTo(name), true);
        }
        _setSaved(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#save()
     */
    /**
     * Save.
     */
    @SuppressWarnings("unchecked")
    public void save() {
        if (null == model) {
            return;
        }

        this.folder.add(this.model);
        // copy to new instance which will be edited and left if unsaved
        this.setModel((T) this.model.copyTo(this.model.getName()), true);
        this.modelInStore = true;
        _setSaved(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#rename(java.lang.String)
     */
    /**
     * Save as.
     *
     * @param name0 the name0
     */
    @SuppressWarnings("unchecked")
    public void saveAs(final String name0) {
        this.setModel((T) this.model.copyTo(name0), false);
        this.save();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#isSaved()
     */

    /**
     * Checks if is saved.
     *
     * @return true, if is saved
     */
    public boolean isSaved() {
        return this.saved;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#delete()
     */

    /**
     * Delete.
     */
    public void delete() {
        if (StringUtils.isNotBlank(this.model.getName())) {
            this.folder.delete(this.model.getName());
        }
        this.modelInStore = false;
        this.newModel();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#isGoodName(java.lang.String)
     */

    /**
     * File exists.
     *
     * @param deckName the deck name
     * @return true, if successful
     */
    public boolean fileExists(final String deckName) {
        return !this.folder.isUnique(deckName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#importDeck(forge.deck.Deck)
     */

    /**
     * Import deck.
     *
     * @param newDeck the new deck
     */
    public void importDeck(final T newDeck) {
        this.setModel(newDeck);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#isModelInStore()
     */

    /**
     * Checks if is model in store.
     *
     * @return true, if is model in store
     */
    public boolean isModelInStore() {
        return this.modelInStore;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#newModel()
     */

    /**
     * New model.
     */
    public void newModel() {
        this.model = this.newModelCreator.get();
        _setSaved(true);
        this.view.resetTables();
    }
}
