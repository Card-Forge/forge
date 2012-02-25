package forge.gui.home.quest;

import java.util.ArrayList;

import forge.AllZone;
import forge.Command;
import forge.Singletons;
import forge.deck.Deck;
import forge.gui.deckeditor.DeckEditorQuest;
import forge.gui.home.ICSubmenu;
import forge.quest.data.QuestData;
import forge.quest.data.QuestPreferences.QPref;

/** 
 * TODO: Write javadoc for this type.
 */
@SuppressWarnings("serial")
public enum CSubmenuQuestDecks implements ICSubmenu {
    /** */
    SINGLETON_INSTANCE;

    private Deck currentDeck;

    private final Command cmdDeckExit = new Command() {
        @Override
        public void execute() {
            AllZone.getQuestData().saveData();
            update();
            Singletons.getView().getViewHome().getBtnQuest().grabFocus();
        }
    };

    private final Command cmdDeckSelect = new Command() {
        @Override
        public void execute() {
            currentDeck = VSubmenuQuestDecks.SINGLETON_INSTANCE.getLstDecks().getSelectedDeck();
            Singletons.getModel().getQuestPreferences().setPreference(QPref.CURRENT_DECK, currentDeck.toString());
            VSubmenuDuels.SINGLETON_INSTANCE.setCurrentDeckStatus();
        }
    };

    private final Command cmdDeckDelete = new Command() { @Override
        public void execute() { update(); } };

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        VSubmenuQuestDecks.SINGLETON_INSTANCE.populate();
        CSubmenuQuestDecks.SINGLETON_INSTANCE.update();

        VSubmenuQuestDecks.SINGLETON_INSTANCE.getBtnNewDeck()
            .setCommand(new Command() { @Override
                public void execute() {
                    final DeckEditorQuest editor = new DeckEditorQuest(AllZone.getQuestData());
                    editor.show(cmdDeckExit);
                    editor.setVisible(true);
                }
            });
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#getCommand()
     */
    @Override
    public Command getMenuCommand() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final VSubmenuQuestDecks view = VSubmenuQuestDecks.SINGLETON_INSTANCE;
        final QuestData qData = AllZone.getQuestData();
        // Retrieve and set all decks
        view.getLstDecks().setDecks(qData != null ? qData.getMyDecks() : new ArrayList<Deck>());

        // Look through list for preferred deck from prefs
        currentDeck = null;

        if (qData != null) {
            final String cd = Singletons.getModel().getQuestPreferences().getPreference(QPref.CURRENT_DECK);
            for (Deck d : qData.getMyDecks()) {
                if (d.getName().equals(cd)) {
                    currentDeck = d;
                    view.getLstDecks().setSelectedDeck(d);
                    break;
                }
            }
        }

        // Not found? Set first one. Still not found? OK, throw to setCurrentDeckStatus().
        if (currentDeck == null) { view.getLstDecks().setSelectedIndex(0); }

        view.getLstDecks().setSelectCommand(cmdDeckSelect);
        view.getLstDecks().setDeleteCommand(cmdDeckDelete);
        view.getLstDecks().setExitCommand(cmdDeckExit);

        VSubmenuDuels.SINGLETON_INSTANCE.setCurrentDeckStatus();
    }

    /** @return forge.deck.Deck */
    public Deck getCurrentDeck() {
        return this.currentDeck;
    }
}
