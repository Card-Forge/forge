package forge.screens.deckeditor.controllers;

import forge.UiCommand;
import forge.deck.DeckProxy;
import forge.gui.framework.ICDoc;
import forge.itemmanager.ItemManagerConfig;
import forge.screens.deckeditor.views.VAllDecks;

/** 
 * Controls the "all decks" panel in the deck editor UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CAllDecks implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        refresh();
    }

    public void refresh() {
        VAllDecks.SINGLETON_INSTANCE.getLstDecks().setPool(DeckProxy.getAllConstructedDecks());
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        VAllDecks.SINGLETON_INSTANCE.getLstDecks().setup(ItemManagerConfig.CONSTRUCTED_DECKS);
    }
}
