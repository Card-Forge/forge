package forge.screens.deckeditor.controllers;

import forge.deck.DeckProxy;
import forge.gui.framework.ICDoc;
import forge.screens.deckeditor.views.VDandanDecks;

/**
 * Controls the "DanDan Decks" panel in the deck editor UI.
 */
public enum CDandanDecks implements ICDoc {
    SINGLETON_INSTANCE;

    private final VDandanDecks view = VDandanDecks.SINGLETON_INSTANCE;

    @Override
    public void register() {
    }

    @Override
    public void initialize() {
        refresh();
    }

    public void refresh() {
        CAllDecks.refreshDeckManager(view.getLstDecks(), DeckProxy.getAllDanDanDecks());
    }

    @Override
    public void update() {
        CAllDecks.updateDeckManager(view.getLstDecks());
    }
}
