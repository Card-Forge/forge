package forge.screens.deckeditor.controllers;

import forge.deck.DeckProxy;
import forge.gui.framework.ICDoc;
import forge.screens.deckeditor.views.VPauperCommanderDecks;

/**
 * Controls the "Pauper Commander Decks" panel in the deck editor UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CPauperCommanderDecks implements ICDoc {
    SINGLETON_INSTANCE;

    private final VPauperCommanderDecks view = VPauperCommanderDecks.SINGLETON_INSTANCE;

    //========== Overridden methods

    @Override
    public void register() {
    }

    @Override
    public void initialize() {
        refresh();
    }

    public void refresh() {
        CAllDecks.refreshDeckManager(view.getLstDecks(), DeckProxy.getAllPauperCommanderDecks());
    }

    @Override
    public void update() {
        CAllDecks.updateDeckManager(view.getLstDecks());
    }
}
