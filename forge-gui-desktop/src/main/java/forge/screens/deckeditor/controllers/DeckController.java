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
import forge.properties.ForgeConstants;
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
        rootFolder = folder0;
        currentFolder = rootFolder;
        view = view0;
        model = null;
        saved = true;
        modelInStorage = false;
        modelPath = "";
        newModelCreator = newModelCreator0;
    }

    /**
     * Gets the model.
     *
     * @return the document
     */
    public T getModel() {
        return model;
    }

    public String getModelPath() {
        return modelPath;
    }

    public boolean isEmpty() {
        return model == null || model.isEmpty();
    }

    /**
     * Sets the model.
     *
     */
    public void setModel(final T document) {
        setModel(document, false);
    }
    public void setModel(final T document, final boolean isStored) {
        modelInStorage = isStored;
        model = document;
        view.resetTables();

        CStatistics.SINGLETON_INSTANCE.update();
        CProbabilities.SINGLETON_INSTANCE.update();

        if (isStored) {
            if (isModelInSyncWithFolder()) {
                setSaved(true);
            }
            else {
                notifyModelChanged();
            }
        } else { //TODO: Make this smarter
            currentFolder = rootFolder;
            modelPath = "";
            setSaved(true);
        }
    }

    private boolean isModelInSyncWithFolder() {
        if (model.getName().isEmpty()) {
            return true;
        }

        final T modelStored = currentFolder.get(model.getName());
        // checks presence in dictionary only.
        if (modelStored == model) {
            return true;
        }
        if (modelStored == null) {
            return false;
        }

        return modelStored.equals(model);
    }

    /**
     * Gets the view.
     *
     * @return the view
     */
    public ACEditorBase<?, T> getView() {
        return view;
    }

    /**
     * Notify model changed.
     */
    public void notifyModelChanged() {
        if (saved) {
            setSaved(false);
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
        final String name = getModelName();
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
        final T newModel = currentFolder.get(name);
        if (newModel != null) {
            setModel((T) newModel.copyTo(name), true);
        }
        else {
            setSaved(true);
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
        currentFolder.add((T) model.copyTo(model.getName()));
        model.setDirectory(currentFolder.getFullPath().substring(ForgeConstants.DECK_BASE_DIR.length()));
        modelInStorage = true;
        setSaved(true);

        VSubmenuConstructed.SINGLETON_INSTANCE.getLobby().updateDeckPanel();
    }

    /**
     * Save as.
     *
     * @param name0 the name0
     */
    @SuppressWarnings("unchecked")
    public void saveAs(final String name0) {
        model = (T)model.copyTo(name0);
        modelInStorage = false;
        save();
        view.resetTables(); //ensure pool updated in CCurrentDeck
    }

    /**
     * Checks if is saved.
     *
     * @return true, if is saved
     */
    public boolean isSaved() {
        return saved;
    }

    /**
     * File exists.
     *
     * @param deckName the deck name
     * @return true, if successful
     */
    public boolean fileExists(final String deckName) {
        return currentFolder.contains(deckName);
    }

    /**
     * Refresh current model or create new one if none
     */
    public void refreshModel() {
        if (model == null) {
            newModel();
        }
        else {
            setModel(model, modelInStorage);
        }
    }

    /**
     * New model.
     */
    public void newModel() {
        model = newModelCreator.get();
        setSaved(true);
        view.resetTables();
    }

    public String getModelName() {
        return model != null ? model.getName() : "";
    }

    public void updateCaptions() {
        String tabCaption = "Current Deck";
        final String title = getModelName();
        String itemManagerCaption = title.isEmpty() ? "[Untitled]" : title;

        if (!saved) {
            tabCaption = "*" + tabCaption;
            itemManagerCaption = "*" + itemManagerCaption;
        }
        itemManagerCaption += " - " + view.getSectionMode().name();

        VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText(tabCaption);
        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setText(title);
        VCurrentDeck.SINGLETON_INSTANCE.getItemManager().setCaption(itemManagerCaption);
        DeckFileMenu.updateSaveEnabled();
    }
}
