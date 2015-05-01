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
package forge.screens.deckeditor.controllers;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Supplier;

import forge.deck.DeckBase;
import forge.screens.deckeditor.menus.DeckFileMenu;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.screens.home.sanctioned.VSubmenuConstructed;
import forge.util.storage.IStorage;

public class DeckController<T extends DeckBase> {
    private T model;
    private boolean saved;
    private boolean modelInStorage;
    private final IStorage<T> rootFolder;
    private IStorage<T> currentFolder;
    private String modelPath;
    private final ACEditorBase<?, T> view;
    private final Supplier<T> newModelCreator;

    /**
     * Instantiates a new deck controller.
     *
     * @param folder0 the folder0
     * @param view0 the view0
     * @param newModelCreator0 the new model creator0
     */
    public DeckController(final IStorage<T> folder0, final ACEditorBase<?, T> view0, final Supplier<T> newModelCreator0) {
        this.rootFolder = folder0;
        this.currentFolder = rootFolder;
        this.view = view0;
        this.model = null;
        this.saved = true;
        this.modelInStorage = false;
        this.modelPath = "";
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

    public String getModelPath() {
        return this.modelPath;
    }

    public boolean isEmpty() {
        return model == null || model.isEmpty();
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

        if (isStored) {
            if (this.isModelInSyncWithFolder()) {
                this.setSaved(true);
            }
            else {
                this.notifyModelChanged();
            }
        } else { //TODO: Make this smarter
            this.currentFolder = this.rootFolder;
            this.modelPath = "";
            this.setSaved(true);
        }
    }

    private boolean isModelInSyncWithFolder() {
        if (model.getName().isEmpty()) {
            return true;
        }

        final T modelStored = this.currentFolder.get(this.model.getName());
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
        if (saved) {
            this.setSaved(false);
        }
    }

    private void setSaved(final boolean val) {
        saved = val;
        updateCaptions();
    }

    /**
     * Reload current model
     */
    public void reload() {
        final String name = this.getModelName();
        if (name.isEmpty()) {
            newModel();
        }
        else {
            load(name);
        }
    }

    public void load(final String path, final String name) {
        if (StringUtils.isBlank(path)) {
            currentFolder = rootFolder;
        }
        else {
            currentFolder = rootFolder.tryGetFolder(path);
        }
        modelPath = path;
        load(name);
    }

    /**
     * Load.
     *
     * @param name the name
     */
    @SuppressWarnings("unchecked")
    private void load(final String name) {
        final T newModel = this.currentFolder.get(name);
        if (newModel != null) {
            this.setModel((T) newModel.copyTo(name), true);
        }
        else {
            this.setSaved(true);
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

        // copy to new instance before adding to current folder so further changes are auto-saved
        this.currentFolder.add((T) this.model.copyTo(this.model.getName()));
        this.modelInStorage = true;
        this.setSaved(true);

        VSubmenuConstructed.SINGLETON_INSTANCE.getLobby().updateDeckPanel();
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
        this.view.resetTables(); //ensure pool updated in CCurrentDeck
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
     * File exists.
     *
     * @param deckName the deck name
     * @return true, if successful
     */
    public boolean fileExists(final String deckName) {
        return this.currentFolder.contains(deckName);
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
        this.setSaved(true);
        this.view.resetTables();
    }

    public String getModelName() {
        return this.model != null ? this.model.getName() : "";
    }

    public void updateCaptions() {
        String tabCaption = "Current Deck";
        final String title = this.getModelName();
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
