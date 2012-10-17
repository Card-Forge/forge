package forge.gui.home.sanctioned;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import forge.AllZone;
import forge.Command;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameNew;
import forge.game.GameType;
import forge.game.PlayerStartsGame;
import forge.game.limited.BoosterDraft;
import forge.game.limited.CardPoolLimitation;
import forge.gui.GuiChoose;
import forge.gui.SOverlayUtils;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorDraftingProcess;
import forge.gui.framework.ICDoc;
import forge.gui.match.CMatchUI;

/** 
 * Controls the draft submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuDraft implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final Command cmdDeckSelect = new Command() {
        @Override
        public void execute() {
            VSubmenuDraft.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);
        }
    };

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        final VSubmenuDraft view = VSubmenuDraft.SINGLETON_INSTANCE;

        view.getLstDecks().setSelectCommand(cmdDeckSelect);

        view.getBtnBuildDeck().setCommand(new Command() { @Override
                    public void execute() { setupDraft(); } });

        view.getBtnStart().addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(final MouseEvent e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                startGame();
                            }
                        });
                    }
                });
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        List<Deck> human = new ArrayList<Deck>();
        for (DeckGroup d : Singletons.getModel().getDecks().getDraft()) {
            human.add(d.getHumanDeck());
        }

        VSubmenuDraft.SINGLETON_INSTANCE.getLstDecks().setDecks(human);

        if (human.size() > 1) {
            VSubmenuDraft.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);
        }
    }

    private void startGame() {
        final boolean gauntlet = VSubmenuDraft.SINGLETON_INSTANCE.getRadSingle().isSelected() ? false : true;

        final Deck human = VSubmenuDraft.SINGLETON_INSTANCE.getLstDecks().getSelectedDeck();
        final int aiIndex = (int) Math.floor(Math.random() * 8);

        if (human == null) {
            JOptionPane.showMessageDialog(null,
                    "No deck selected for human!\r\n(You may need to build a new deck.)",
                    "No deck", JOptionPane.ERROR_MESSAGE);
            return;
        } else if (!human.meetsGameTypeRequirements(GameType.Draft)) {
            JOptionPane.showMessageDialog(null,
                    "The selected deck doesn't have enough cards to play (minimum 40)."
                    + "\r\nUse the deck editor to choose the cards you want before starting.",
                    "Invalid deck", JOptionPane.ERROR_MESSAGE);
            return;
        }

        AllZone.getGauntlet().resetGauntletDraft();

        if (gauntlet) {
            int rounds = Singletons.getModel().getDecks().getDraft().get(human.getName()).getAiDecks().size();
            AllZone.getGauntlet().launch(rounds, human, GameType.Draft);
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        final SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {
            @Override
            public Object doInBackground() {
                DeckGroup opponentDecks = Singletons.getModel().getDecks().getDraft().get(human.getName());

                Deck aiDeck = opponentDecks.getAiDecks().get(aiIndex);
                if (aiDeck == null) {
                    throw new IllegalStateException("Draft: Computer deck is null!");
                }

                CMatchUI.SINGLETON_INSTANCE.initMatch(null);
                Singletons.getModel().getMatchState().setGameType(GameType.Draft);
                GameNew.newGame(new PlayerStartsGame(AllZone.getHumanPlayer(), human),
                                 new PlayerStartsGame(AllZone.getComputerPlayer(), aiDeck));
                return null;
            }

            @Override
            public void done() {
                SOverlayUtils.hideOverlay();
            }
        };
        worker.execute();
    }

    /** */
    private void setupDraft() {
        final CEditorDraftingProcess draft = new CEditorDraftingProcess();

        // Determine what kind of booster draft to run
        final ArrayList<String> draftTypes = new ArrayList<String>();
        draftTypes.add("Full Cardpool");
        draftTypes.add("Block / Set");
        draftTypes.add("Fantasy Block");
        draftTypes.add("Custom");

        final String prompt = "Choose Draft Format:";
        final Object o = GuiChoose.one(prompt, draftTypes);

        if (o.toString().equals(draftTypes.get(0))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.Full));
        }

        else if (o.toString().equals(draftTypes.get(1))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.Block));
        }

        else if (o.toString().equals(draftTypes.get(2))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.FantasyBlock));
        }

        else if (o.toString().equals(draftTypes.get(3))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.Custom));
        }

        FControl.SINGLETON_INSTANCE.changeState(FControl.DECK_EDITOR_LIMITED);
        CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(draft);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
