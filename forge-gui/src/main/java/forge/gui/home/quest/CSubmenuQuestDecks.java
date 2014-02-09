package forge.gui.home.quest;

import forge.Command;
import forge.Singletons;
import forge.deck.Deck;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.DeckProxy;
import forge.gui.deckeditor.controllers.CEditorQuest;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.gui.home.CHomeUI;
import forge.quest.QuestController;
import forge.quest.data.QuestPreferences.QPref;

import javax.swing.*;
import java.util.Map.Entry;

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
        public void run() {
            currentDeck = VSubmenuQuestDecks.SINGLETON_INSTANCE.getLstDecks().getSelectedItem().getDeck();
            Singletons.getModel().getQuestPreferences().setPref(QPref.CURRENT_DECK, currentDeck.toString());
            Singletons.getModel().getQuestPreferences().save();
        }
    };

    private final Command cmdDeckDelete = new Command() { @Override
        public void run() { update(); } };

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        VSubmenuQuestDecks.SINGLETON_INSTANCE.getBtnNewDeck().setCommand(new Command() {
            @Override
            public void run() {
                if (!SSubmenuQuestUtil.checkActiveQuest("Create a Deck.")) {
                    return;
                }
                Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_QUEST);
                CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorQuest(Singletons.getModel().getQuest()));
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
        view.getLstDecks().setPool(DeckProxy.getAllQuestDecks(hasQuest ? qData.getMyDecks() : null));
        view.getLstDecks().update();

        // Look through list for preferred deck from prefs
        currentDeck = null;

        if (hasQuest) {
            final String cd = Singletons.getModel().getQuestPreferences().getPref(QPref.CURRENT_DECK);

            for (Entry<DeckProxy, Integer> d : view.getLstDecks().getPool() )
                if( d.getKey().getName().equals(cd) ) {
                    view.getLstDecks().setSelectedItem(d.getKey());
                    break;
                }
        }

        // Not found? Set first one. Still not found? OK, throw to setCurrentDeckStatus().
        if (currentDeck == null) { view.getLstDecks().setSelectedIndex(0); }

        view.getLstDecks().setSelectCommand(cmdDeckSelect);
        view.getLstDecks().setDeleteCommand(cmdDeckDelete);

        if (view.getLstDecks().getSelectedItem() != null) {
            Singletons.getModel().getQuestPreferences().setPref(QPref.CURRENT_DECK, view.getLstDecks().getSelectedItem().getName());
        }
        else {
            Singletons.getModel().getQuestPreferences().setPref(QPref.CURRENT_DECK, QPref.CURRENT_DECK.getDefault());
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { view.getBtnNewDeck().requestFocusInWindow(); }
        });
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
            public void run() {
                if (qc.getAchievements() == null) {
                    CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_QUESTDATA);
                }
            }
        };
    }
}
