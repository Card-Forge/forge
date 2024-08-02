package forge.screens.home.quest;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import forge.deck.DeckGroup;
import forge.gamemodes.quest.QuestTournamentController;
import forge.gui.UiCommand;
import forge.gui.framework.ICDoc;

/**
 * Controls the quest draft submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuQuestDraft implements ICDoc {
    SINGLETON_INSTANCE;

    private QuestTournamentController controller;

    @Override
    public void register() {
    }

    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        final VSubmenuQuestDraft view = VSubmenuQuestDraft.SINGLETON_INSTANCE;
        controller = new QuestTournamentController(view);

        view.getBtnStartDraft().addActionListener(selectTournamentStart);
        view.getBtnStartTournament().addActionListener(prepareDeckStart);
        view.getBtnStartMatch().addActionListener(nextMatchStart);

        view.getBtnStartMatchSmall().setCommand((UiCommand) () -> controller.startNextMatch());
        view.getBtnSpendToken().setCommand((UiCommand) () -> controller.spendToken());
        view.getBtnEditDeck().setCommand((UiCommand) () -> view.editDeck(true));
        view.getBtnLeaveTournament().setCommand((UiCommand) () -> controller.endTournamentAndAwardPrizes());
    }

    private final ActionListener selectTournamentStart = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent event) {
            controller.startDraft();
        }
    };

    private final ActionListener prepareDeckStart = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent event) {
            controller.startTournament();
        }
    };

    private final ActionListener nextMatchStart = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent event) {
            controller.startNextMatch();
        }
    };

    @Override
    public void update() {
        controller.update();
    }

    public void setCompletedDraft(final DeckGroup finishedDraft) {
        controller.setCompletedDraft(finishedDraft);
    }

    public boolean cancelDraft() {
        return controller.cancelDraft();
    }
}
