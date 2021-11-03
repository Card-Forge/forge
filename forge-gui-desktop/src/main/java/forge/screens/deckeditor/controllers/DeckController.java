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

import com.google.common.base.Supplier;
import forge.StaticData;
import forge.card.CardEdition;
import forge.deck.*;
import forge.item.PaperCard;
import forge.screens.deckeditor.menus.DeckFileMenu;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.screens.home.gauntlet.VSubmenuGauntletBuild;
import forge.screens.home.gauntlet.VSubmenuGauntletContests;
import forge.screens.home.gauntlet.VSubmenuGauntletQuick;
import forge.screens.home.sanctioned.VSubmenuConstructed;
import forge.util.ItemPool;
import forge.util.Localizer;
import forge.util.storage.IStorage;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class DeckController<T extends DeckBase> {
    private T model;
    private boolean saved;
    private boolean modelInStorage;
    private final IStorage<T> rootFolder;
    private IStorage<T> currentFolder;
    private String modelPath;
    private final CDeckEditor<T> view;
    private final Supplier<T> newModelCreator;

    /**
     * Instantiates a new deck controller.
     *
     * @param folder0 the folder0
     * @param view0 the view0
     * @param newModelCreator0 the new model creator0
     */
    public DeckController(final IStorage<T> folder0, final CDeckEditor<T> view0, final Supplier<T> newModelCreator0) {
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
     * Load deck from file or clipboard
     */
    public void loadDeck(Deck deck) {
        this.loadDeck(deck, true);
    }
    public void loadDeck(Deck deck, boolean substituteCurrentDeck) {
        boolean isStored;
        if (view.getCatalogManager().isInfinite()) {
            Deck currentDeck = view.getHumanDeck();
            if (substituteCurrentDeck || currentDeck.isEmpty()) {
                newModel();
                isStored = false;
            } else
                isStored = !this.modelPath.equals("");
        } else {
            CardPool catalogClone = new CardPool(view.getInitialCatalog());
            deck = pickFromCatalog(deck, catalogClone);
            ItemPool<PaperCard> catalogPool = view.getCatalogManager().getPool();
            catalogPool.clear();
            catalogPool.addAll(catalogClone);
            isStored = false;
        }

        Deck currentDeck = view.getHumanDeck();
        for (DeckSection section: EnumSet.allOf(DeckSection.class)) {
            if (view.isSectionImportable(section)) {
                CardPool sectionCards = currentDeck.getOrCreate(section);
                sectionCards.addAll(deck.getOrCreate(section));
            }
        }
        // Allow to specify the name of Deck in DeckImporter
        if (deck.hasName())
            currentDeck.setName(deck.getName());
        this.setModel((T) currentDeck, isStored);
    }

    public Deck getCurrentDeckInEditor(){
        try{
            return this.getModel().getHumanDeck();
        } catch (NullPointerException npe){
            return null;
        }
    }

    private Deck pickFromCatalog(Deck deck, CardPool catalog) {
        // Getting Latest among the earliest editions in catalog!
        CardEdition referenceEdition = StaticData.instance().getEditions().getTheLatestOfAllTheOriginalEditionsOfCardsIn(catalog);
        Date referenceReleaseDate = referenceEdition.getDate();
        Deck result = new Deck();
        for (DeckSection section: EnumSet.allOf(DeckSection.class)) {
            if (view.isSectionImportable(section)) {
                CardPool cards = pickSectionFromCatalog(catalog, deck.getOrCreate(section), referenceReleaseDate);
                result.putSection(section, cards);
            }
        }

        return result;
    }

    private CardPool pickSectionFromCatalog(CardPool catalog, CardPool sourceSection, Date referenceReleaseDate) {
        Map<String, Integer> countByName = groupByName(sourceSection);
        Map<String, PaperCard> basicLandsByName = getBasicLandsByName(sourceSection);

        CardPool targetSection = new CardPool();
        pickFromCatalog(countByName, catalog, targetSection);
        importBasicLands(countByName, basicLandsByName, referenceReleaseDate, targetSection);

        return targetSection;
    }

    private Map<String, Integer> groupByName(CardPool section) {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<PaperCard, Integer> entry : section) {
            PaperCard importedCard = entry.getKey();
            Integer previousCount = result.getOrDefault(importedCard.getName(), 0);
            int countToAdd = entry.getValue();
            result.put(importedCard.getName(), countToAdd + previousCount);
        }
        return result;
    }

    private void pickFromCatalog(Map<String, Integer> countByName, CardPool catalog, CardPool targetSection) {
        CardPool catalogClone = new CardPool(catalog); // clone to iterate modified collection
        for (Map.Entry<PaperCard, Integer> entry : catalogClone) {
            PaperCard availableCard = entry.getKey();
            if (availableCard.getRules().getType().isBasicLand())  // basic lands are added regardless of catalog cards
                continue;
            Integer availableCount = entry.getValue();
            int toAddByName = countByName.getOrDefault(availableCard.getName(), 0);
            int toAdd = Math.min(availableCount, toAddByName);

            if (toAdd > 0) {
                targetSection.add(availableCard, toAdd);
                countByName.put(availableCard.getName(), toAddByName - toAdd);
                catalog.remove(availableCard, toAdd);
            }
        }
    }

    private void importBasicLands(Map<String, Integer> countByName, Map<String, PaperCard> basicLandsByName,
                                  Date referenceReleaseDate, CardPool targetSection) {
        for (String cardName : countByName.keySet()) {
            PaperCard card = basicLandsByName.getOrDefault(cardName, null);
            if (card == null)
                continue;
            int countToAdd = countByName.get(cardName);
            card = StaticData.instance().getAlternativeCardPrint(card, referenceReleaseDate);
            if (card != null)
                targetSection.add(card.getName(), card.getEdition(), countToAdd);
        }
    }

    private HashMap<String, PaperCard> getBasicLandsByName(CardPool sourceSection) {
        HashMap<String, PaperCard> result = new HashMap<>();

        for (Map.Entry<PaperCard, Integer> entry : sourceSection) {
            PaperCard card = entry.getKey();

            if (!card.getRules().getType().isBasicLand()) {
                continue;
            }

            if (result.containsKey(card.getName())) {
                continue;
            }

            result.put(card.getName(), card);
        }

        return result;
    }

    /**
     * Sets the model.
     *
     */
    public void setModel(final T document) {
        setModel(document, false);
    }
    private void setModel(final T document, final boolean isStored) {
        model = document;
        onModelChanged(isStored);
    }

    private void onModelChanged(boolean isStored) {
        modelInStorage = isStored;
        view.resetTables();

        CStatistics.SINGLETON_INSTANCE.update();
        CProbabilities.SINGLETON_INSTANCE.update();

        if (isStored) {
            if (isModelInSyncWithFolder()) {
                setSaved(true);
            } else {
                notifyModelChanged();
            }
        } else { //TODO: Make this smarter
            currentFolder = rootFolder;
            modelPath = "";
            setSaved(this.model.isEmpty());
        }
    }

    private Boolean isInSyncCacheResult = null;
    private T syncModelCache = null;
    private boolean isModelInSyncWithFolder() {
        if (syncModelCache != null && model == syncModelCache)
            return isInSyncCacheResult;

        if (model.getName().isEmpty())
            return true;

        final T modelStored = currentFolder.get(model.getName());
        // checks presence in dictionary only.
        if (modelStored == model) {
            return true;
        }
        if (modelStored == null) {
            return false;
        }
        syncModelCache = model;
        isInSyncCacheResult = modelStored.equals(model);
        return isInSyncCacheResult;
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
        } else {
            load(name);
        }
    }

    public void load(final String path, final String name) {
        if (StringUtils.isBlank(path)) {
            currentFolder = rootFolder;
        } else {
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
        } else {
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
        model.setDirectory(DeckProxy.getDeckDirectory(currentFolder));
        modelInStorage = true;
        setSaved(true);

        VSubmenuConstructed.SINGLETON_INSTANCE.getLobby().updateDeckPanel();
        VSubmenuGauntletBuild.SINGLETON_INSTANCE.updateDeckPanel();
        VSubmenuGauntletQuick.SINGLETON_INSTANCE.updateDeckPanel();
        VSubmenuGauntletContests.SINGLETON_INSTANCE.updateDeckPanel();
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
        } else {
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
        final Localizer localizer = Localizer.getInstance();
        String tabCaption = localizer.getMessage("lblCurrentDeck2");
        final String title = getModelName();
        String itemManagerCaption = title.isEmpty() ? "[" + localizer.getMessage("lblUntitled") +"]" : title;

        if (!saved) {
            tabCaption = "*" + tabCaption;
            itemManagerCaption = "*" + itemManagerCaption;
        }
        itemManagerCaption += " - ";

        VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText(tabCaption);
        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setText(title);
        VCurrentDeck.SINGLETON_INSTANCE.getItemManager().setCaption(itemManagerCaption);
        DeckFileMenu.updateSaveEnabled();
    }
}
