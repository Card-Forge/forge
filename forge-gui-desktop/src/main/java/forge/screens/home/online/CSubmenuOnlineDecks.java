package forge.screens.home.online;

import forge.deck.DeckProxy;
import forge.gui.framework.ICDoc;
import forge.itemmanager.ItemManagerConfig;

/**
 * Controls the online draft/sealed decks submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CSubmenuOnlineDecks implements ICDoc {
    SINGLETON_INSTANCE;

    @Override
    public void register() {
    }

    @Override
    public void initialize() {
    }

    @Override
    public void update() {
        final VSubmenuOnlineDecks view = VSubmenuOnlineDecks.SINGLETON_INSTANCE;
        view.getLstDecks().setPool(DeckProxy.getAllNetworkEventDecks());
        view.getLstDecks().setup(ItemManagerConfig.NET_EVENT_DECKS);
    }
}
