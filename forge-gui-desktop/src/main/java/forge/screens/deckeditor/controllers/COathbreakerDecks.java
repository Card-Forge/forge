package forge.screens.deckeditor.controllers;

import forge.deck.DeckProxy;
import forge.gui.framework.ICDoc;
import forge.screens.deckeditor.views.VOathbreakerDecks;

/**
 * Controls the "Oathbreaker Decks" panel in the deck editor UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum COathbreakerDecks implements ICDoc {
    SINGLETON_INSTANCE;

    private final VOathbreakerDecks view = VOathbreakerDecks.SINGLETON_INSTANCE;

    //========== Overridden methods

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        refresh();
    }

    public void refresh() {
        CAllDecks.refreshDeckManager(view.getLstDecks(), DeckProxy.getAllOathbreakerDecks());
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        CAllDecks.updateDeckManager(view.getLstDecks());
    }
}
