package forge.gui.home.quest;

import java.util.ArrayList;

import forge.Command;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.Deck;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorQuest;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.CHomeUI;
import forge.quest.QuestController;
import forge.quest.data.QuestPreferences.QPref;

/** 
 * Controls the quest decks submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
@SuppressWarnings("serial")
public enum CSubmenuQuestDecks implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private Deck currentDeck;

    private final Command cmdDeckSelect = new Command() {
        @Override
        public void execute() {
            currentDeck = VSubmenuQuestDecks.SINGLETON_INSTANCE.getLstDecks().getSelectedDeck();
            Singletons.getModel().getQuestPreferences().setPreference(QPref.CURRENT_DECK, currentDeck.toString());
            Singletons.getModel().getQuestPreferences().save();
        }
    };

    private final Command cmdDeckDelete = new Command() { @Override
        public void execute() { update(); } };

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        VSubmenuQuestDecks.SINGLETON_INSTANCE.getBtnNewDeck().setCommand(new Command() {
            @Override
            public void execute() {
                CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(new CEditorQuest(Singletons.getModel().getQuest()));
                FControl.SINGLETON_INSTANCE.changeState(FControl.DECK_EDITOR_QUEST);
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final VSubmenuQuestDecks view = VSubmenuQuestDecks.SINGLETON_INSTANCE;
        final QuestController qData = Singletons.getModel().getQuest();
        boolean hasQuest = qData.getAssets() != null;
        // Retrieve and set all decks
        view.getLstDecks().setDecks(hasQuest ? qData.getMyDecks() : new ArrayList<Deck>());

        // Look through list for preferred deck from prefs
        currentDeck = null;

        if (hasQuest) {
            final String cd = Singletons.getModel().getQuestPreferences().getPreference(QPref.CURRENT_DECK);

            for (Deck d : qData.getMyDecks()) {
                if (d.getName() != null && d.getName().equals(cd)) {
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

        if (view.getLstDecks().getSelectedDeck() != null) {
            Singletons.getModel().getQuestPreferences().setPreference(QPref.CURRENT_DECK, view.getLstDecks().getSelectedDeck().getName());
        }
        else {
            Singletons.getModel().getQuestPreferences().setPreference(QPref.CURRENT_DECK, QPref.CURRENT_DECK.getDefault());
        }
    }

    /** @return forge.deck.Deck */
    public Deck getCurrentDeck() {
        return this.currentDeck;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        final QuestController qc = Singletons.getModel().getQuest();
        return new Command() {
            @Override
            public void execute() {
                if (qc.getAchievements() == null) {
                    CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_QUESTDATA);
                }
            }
        };
    }
}
