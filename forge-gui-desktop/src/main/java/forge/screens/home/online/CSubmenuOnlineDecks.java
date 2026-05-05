package forge.screens.home.online;

import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.SEditorIO;
import forge.screens.deckeditor.controllers.CEditorLimited;

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
        final VSubmenuOnlineDecks view = VSubmenuOnlineDecks.SINGLETON_INSTANCE;
        // Override the default editDeck command so it uses getNetworkEventDecks()
        // instead of the default getSealed() that DeckManager would pick
        view.getLstDecks().setItemActivateCommand(() -> {
            final DeckProxy deck = view.getLstDecks().getSelectedItem();
            final FScreen screen = FScreen.DECK_EDITOR_SEALED;
            final CEditorLimited<Deck> editorCtrl = new CEditorLimited<>(
                    FModel.getDecks().getNetworkEventDecks(), Deck::new, screen,
                    CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());

            if (!Singletons.getControl().ensureScreenActive(screen)) {
                return;
            }
            // Confirm before installing the new controller so a Cancel doesn't
            // clobber the previous editor's unsaved state.
            if (!SEditorIO.confirmSaveChanges(screen, true)) {
                return;
            }
            CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(editorCtrl);
            if (deck != null) {
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController()
                        .getDeckController().load(deck.getPath(), deck.getName());
            }
        });
    }

    @Override
    public void update() {
        final VSubmenuOnlineDecks view = VSubmenuOnlineDecks.SINGLETON_INSTANCE;
        view.getLstDecks().setPool(DeckProxy.getAllNetworkEventDecks());
        view.getLstDecks().setup(ItemManagerConfig.NET_EVENT_DECKS);
    }
}
