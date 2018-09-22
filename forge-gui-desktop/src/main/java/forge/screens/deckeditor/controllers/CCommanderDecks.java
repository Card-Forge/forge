package forge.screens.deckeditor.controllers;

import forge.deck.DeckProxy;
import forge.gui.framework.ICDoc;
import forge.itemmanager.ItemManagerConfig;
import forge.screens.deckeditor.views.VCommanderDecks;

/**
 * Controls the "Commander Decks" panel in the deck editor UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CCommanderDecks implements ICDoc {
    SINGLETON_INSTANCE;

    private final VCommanderDecks view = VCommanderDecks.SINGLETON_INSTANCE;

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
        view.getLstDecks().setPool(DeckProxy.getAllCommanderDecks());
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        view.getLstDecks().setup(ItemManagerConfig.CONSTRUCTED_DECKS);
    }
}
