package forge.screens.home.quest;

import javax.swing.SwingUtilities;

import forge.Singletons;
import forge.UiCommand;
import forge.deck.DeckProxy;
import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.QuestUtil;
import forge.gamemodes.quest.data.QuestPreferences.QPref;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorQuest;
import forge.util.Localizer;

/**
 * Controls the quest decks submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
@SuppressWarnings("serial")
public enum CSubmenuQuestDecks implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final UiCommand cmdDeckSelect = new UiCommand() {
        @Override
        public void run() {
            final DeckProxy deck = VSubmenuQuestDecks.SINGLETON_INSTANCE.getLstDecks().getSelectedItem();
            if (deck != null) {
                FModel.getQuest().setCurrentDeck(deck.toString());
            }
            else {
                FModel.getQuest().setCurrentDeck(QPref.CURRENT_DECK.getDefault());
            }
            FModel.getQuest().save();
        }
    };

    private final UiCommand cmdDeckDelete = new UiCommand() {
        @Override
        public void run() {
            update();
        }
    };

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        final Localizer localizer = Localizer.getInstance();

        VSubmenuQuestDecks.SINGLETON_INSTANCE.getBtnNewDeck().setCommand(new UiCommand() {
            @Override
            public void run() {
                if (!QuestUtil.checkActiveQuest(localizer.getMessage("lblCreateaDeck"))) {
                    return;
                }
                Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_QUEST);
                CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorQuest(FModel.getQuest(), CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture()));
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final VSubmenuQuestDecks view = VSubmenuQuestDecks.SINGLETON_INSTANCE;

        view.getLstDecks().setSelectCommand(null); //set to null temporarily
        view.getLstDecks().setDeleteCommand(null);

        final QuestController qData = FModel.getQuest();
        final boolean hasQuest = qData.getAssets() != null;
        // Retrieve and set all decks
        view.getLstDecks().setPool(DeckProxy.getAllQuestDecks(hasQuest ? qData.getMyDecks() : null));
        view.getLstDecks().setup(ItemManagerConfig.QUEST_DECKS);

        // Look through list for preferred deck from prefs
        final DeckProxy deck = hasQuest ? view.getLstDecks().stringToItem(FModel.getQuest().getCurrentDeck()) : null;
        if (deck != null) {
            view.getLstDecks().setSelectedItem(deck);
        }

        view.getLstDecks().setSelectCommand(cmdDeckSelect);
        view.getLstDecks().setDeleteCommand(cmdDeckDelete);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view.getBtnNewDeck().requestFocusInWindow();
            }
        });
    }

}
