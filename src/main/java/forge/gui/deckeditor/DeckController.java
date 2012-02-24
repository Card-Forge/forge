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
import java.util.ArrayList;

import net.slightlymagic.braids.util.lambda.Lambda0;

import org.apache.commons.lang3.StringUtils;

import forge.deck.DeckBase;
import forge.util.IFolderMap;

/**
 * TODO: Write javadoc for this type.
 *
 * @param <T> the generic type
 */
public class DeckController<T extends DeckBase> implements IDeckController<T> {

    private T model;
    private boolean saved;
    private boolean modelInStore;
    private final IFolderMap<T> folder;
    private final DeckEditorBase<?, T> view;
    private final Lambda0<T> newModelCreator;

    /**
     * Instantiates a new deck controller.
     *
     * @param folder0 the folder0
     * @param view0 the view0
     * @param newModelCreator0 the new model creator0
     */
    public DeckController(final IFolderMap<T> folder0, final DeckEditorBase<?, T> view0,
            final Lambda0<T> newModelCreator0) {
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
    @Override
    public T getModel() {
        return this.model;
    }

    /**
     * Sets the model.
     *
     * @param document the new model
     */
    @Override
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
        this.view.updateView();
        this.saved = true; // unless set to false in notify
        if (!this.isModelInSyncWithFolder()) {
            this.notifyModelChanged();
        }
    }

    private boolean isModelInSyncWithFolder() {
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
    @Override
    public DeckEditorBase<?, T> getView() {
        return this.view;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#notifyModelChanged()
     */
    @Override
    public void notifyModelChanged() {
        this.saved = false;
        // view.setTitle();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#getOwnerWindow()
     */
    @Override
    public Component getOwnerWindow() {
        return this.getView();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#getSavedModelNames()
     */
    @Override
    public ArrayList<String> getSavedNames() {
        return new ArrayList<String>(this.folder.getNames());
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#load(java.lang.String)
     */
    @Override
    public void load(final String name) {
        this.setModel(this.folder.get(name), true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#save()
     */
    @Override
    public void save() {
        this.folder.add(this.model);
        this.saved = true;
        this.modelInStore = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#rename(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void saveAs(final String name0) {
        this.setModel((T) this.model.copyTo(name0), false);
        this.save();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#isSaved()
     */
    @Override
    public boolean isSaved() {
        return this.saved;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#delete()
     */
    @Override
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

    @Override
    public boolean fileExists(final String deckName) {
        return !this.folder.isUnique(deckName);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckController#isGoodName(java.lang.String)
     */
    @Override
    public boolean isGoodName(final String deckName) {
        return StringUtils.isNotBlank(deckName) && this.folder.isUnique(deckName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#importDeck(forge.deck.Deck)
     */
    @Override
    public void importDeck(final T newDeck) {
        this.setModel(newDeck);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#isModelInStore()
     */
    @Override
    public boolean isModelInStore() {
        return this.modelInStore;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.IDeckController#newModel()
     */
    @Override
    public void newModel() {
        this.model = this.newModelCreator.apply();
        this.saved = true;
        this.view.updateView();
    }
}
