package forge.gui.home.quest;

import java.util.ArrayList;

import forge.AllZone;
import forge.Command;
import forge.Singletons;
import forge.deck.Deck;
import forge.gui.OverlayUtils;
import forge.gui.deckeditor.DeckEditorQuest;
import forge.gui.home.EMenuItem;
import forge.gui.home.ICSubmenu;
import forge.gui.home.VHomeUI;
import forge.quest.QuestController;
import forge.quest.data.QuestPreferences.QPref;

/** 
 * Controls the quest decks submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
@SuppressWarnings("serial")
public enum CSubmenuQuestDecks implements ICSubmenu {
    /** */
    SINGLETON_INSTANCE;

    private Deck currentDeck;

    private final Command cmdDeckExit = new Command() {
        @Override
        public void execute() {
            AllZone.getQuest().save();
            OverlayUtils.hideOverlay();
            update();
        }
    };

    private final Command cmdDeckSelect = new Command() {
        @Override
        public void execute() {
            currentDeck = VSubmenuQuestDecks.SINGLETON_INSTANCE.getLstDecks().getSelectedDeck();
            Singletons.getModel().getQuestPreferences().setPreference(QPref.CURRENT_DECK, currentDeck.toString());
            Singletons.getModel().getQuestPreferences().save();
            VSubmenuDuels.SINGLETON_INSTANCE.updateCurrentDeckStatus();
            VSubmenuChallenges.SINGLETON_INSTANCE.updateCurrentDeckStatus();
        }
    };

    private final Command cmdDeckDelete = new Command() { @Override
        public void execute() { update(); } };

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#getCommand()
     */
    @Override
    public Command getMenuCommand() {
        final QuestController qc = AllZone.getQuest();
        return new Command() {
            @Override
            public void execute() {
                if (qc.getAchievements() == null) {
                    VHomeUI.SINGLETON_INSTANCE.itemClick(EMenuItem.QUEST_DATA);
                }
            }
        };
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        VSubmenuQuestDecks.SINGLETON_INSTANCE.populate();
        CSubmenuQuestDecks.SINGLETON_INSTANCE.update();

        VSubmenuQuestDecks.SINGLETON_INSTANCE.getBtnNewDeck().setCommand(new Command() {
            @Override
            public void execute() {
                final DeckEditorQuest editor =
                        new DeckEditorQuest(Singletons.getView().getFrame(), AllZone.getQuest());
                editor.show(cmdDeckExit);
                OverlayUtils.showOverlay();
                editor.setVisible(true);
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final VSubmenuQuestDecks view = VSubmenuQuestDecks.SINGLETON_INSTANCE;
        final QuestController qData = AllZone.getQuest();
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
        view.getLstDecks().setExitCommand(cmdDeckExit);

        if (view.getLstDecks().getSelectedDeck() != null) {
            Singletons.getModel().getQuestPreferences().setPreference(QPref.CURRENT_DECK, view.getLstDecks().getSelectedDeck().getName());
        }
        else {
            Singletons.getModel().getQuestPreferences().setPreference(QPref.CURRENT_DECK, QPref.CURRENT_DECK.getDefault());
        }

        VSubmenuDuels.SINGLETON_INSTANCE.updateCurrentDeckStatus();
        VSubmenuChallenges.SINGLETON_INSTANCE.updateCurrentDeckStatus();
    }

    /** @return forge.deck.Deck */
    public Deck getCurrentDeck() {
        return this.currentDeck;
    }
}
