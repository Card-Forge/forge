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
package forge.gui.deckeditor.controllers;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Supplier;

import forge.deck.DeckBase;
import forge.gui.deckeditor.menus.DeckFileMenu;
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
    private boolean modelInStorage;
    private final IStorage<T> rootFolder;
    private IStorage<T> folder;
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
        this.rootFolder = folder0;
        this.folder = rootFolder;
        this.view = view0;
        this.model = null;
        this.saved = true;
        this.modelInStorage = false;
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
     */
    public void setModel(final T document) {
        this.setModel(document, false);
    }
    public void setModel(final T document, final boolean isStored) {
        this.modelInStorage = isStored;
        this.model = document;
        this.view.resetTables();

        CStatistics.SINGLETON_INSTANCE.update();
        CProbabilities.SINGLETON_INSTANCE.update();

        if (this.isModelInSyncWithFolder()) {
            _setSaved(true);
        }
        else {
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
        if (modelStored == null) {
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

    /**
     * Notify model changed.
     */
    public void notifyModelChanged() {
        _setSaved(false);
    }

    private void _setSaved(boolean val) {
        saved = val;
        updateCaptions();
    }

    /**
     * Gets the saved names.
     *
     * @return the saved names
     */
    public ArrayList<String> getSavedNames() {
        return new ArrayList<String>(this.folder.getItemNames());
    }

    /**
     * Reload current model
     */
    public void reload() {
        String name = this.getModelName();
        if (name.isEmpty()) {
            newModel();
        }
        else {
            load(name);
        }
    }

    public void load(final String path, final String name) {
        if ( StringUtils.isBlank(path))
            folder = rootFolder;
        else
            folder = rootFolder.tryGetFolder(path);
        load(name);
    }
    
    /**
     * Load.
     *
     * @param name the name
     */
    @SuppressWarnings("unchecked") private void load(final String name) {
        T newModel = this.folder.get(name);
        if (newModel != null) {
            this.setModel((T) newModel.copyTo(name), true);
        }
        else {
            _setSaved(true);
        }
    }

    /**
     * Save.
     */
    @SuppressWarnings("unchecked")
    public void save() {
        if (model == null) {
            return;
        }

        this.folder.add(this.model);
        // copy to new instance which will be edited and left if unsaved
        this.model = (T)this.model.copyTo(this.model.getName());
        this.modelInStorage = true;
        _setSaved(true);
    }

    /**
     * Save as.
     *
     * @param name0 the name0
     */
    @SuppressWarnings("unchecked")
    public void saveAs(final String name0) {
        this.model = (T)this.model.copyTo(name0);
        this.modelInStorage = false;
        this.save();
    }

    /**
     * Checks if is saved.
     *
     * @return true, if is saved
     */
    public boolean isSaved() {
        return this.saved;
    }

    /**
     * Delete.
     */
    public void delete() {
        if (StringUtils.isNotBlank(this.model.getName())) {
            this.folder.delete(this.model.getName());
        }
        this.modelInStorage = false;
        this.newModel();
    }

    /**
     * File exists.
     *
     * @param deckName the deck name
     * @return true, if successful
     */
    public boolean fileExists(final String deckName) {
        return this.folder.contains(deckName);
    }

    /**
     * Import deck.
     *
     * @param newDeck the new deck
     */
    public void importDeck(final T newDeck) {
        this.setModel(newDeck);
    }

    /**
     * Refresh current model or create new one if none
     */
    public void refreshModel() {
        if (this.model == null) {
            newModel();
        }
        else {
            setModel(this.model, this.modelInStorage);
        }
    }

    /**
     * New model.
     */
    public void newModel() {
        this.model = this.newModelCreator.get();
        _setSaved(true);
        this.view.resetTables();
    }

    public String getModelName() {
        return this.model != null ? this.model.getName() : "";
    }

    public void updateCaptions() {
        String tabCaption = "Current Deck";
        String title = this.model.getName();
        String itemManagerCaption = title.isEmpty() ? "[Untitled]" : title;

        if (!saved) {
            tabCaption = "*" + tabCaption;
            itemManagerCaption = "*" + itemManagerCaption;
        }
        itemManagerCaption += " - " + this.view.getSectionMode().name();

        VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText(tabCaption);
        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setText(title);
        VCurrentDeck.SINGLETON_INSTANCE.getItemManager().setCaption(itemManagerCaption);
        DeckFileMenu.updateSaveEnabled();
    }
}
