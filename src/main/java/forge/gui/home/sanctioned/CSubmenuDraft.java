package forge.gui.home.sanctioned;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import forge.Command;
import forge.Singletons;
import forge.control.FControl;
import forge.control.Lobby;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameType;
import forge.game.MatchController;
import forge.game.MatchStartHelper;
import forge.game.limited.BoosterDraft;
import forge.game.limited.CardPoolLimitation;
import forge.game.player.LobbyPlayer;
import forge.game.player.PlayerType;
import forge.gui.GuiChoose;
import forge.gui.SOverlayUtils;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorDraftingProcess;
import forge.gui.framework.ICDoc;

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
    private LobbyPlayer[] opponents;

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
        final Deck humanDeck = VSubmenuDraft.SINGLETON_INSTANCE.getLstDecks().getSelectedDeck();
        final int aiIndex = (int) Math.floor(Math.random() * 8);

        if (humanDeck == null) {
            JOptionPane.showMessageDialog(null,
                    "No deck selected for human!\r\n(You may need to build a new deck.)",
                    "No deck", JOptionPane.ERROR_MESSAGE);
            return;
        } else if (null != humanDeck.meetsGameTypeRequirements(GameType.Draft)) {
            JOptionPane.showMessageDialog(null,
                    "The selected deck doesn't have enough cards to play (minimum 40)."
                    + "\r\nUse the deck editor to choose the cards you want before starting.",
                    "Invalid deck", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Singletons.getModel().getGauntletMini().resetGauntletDraft();

        if (gauntlet) {
            int rounds = Singletons.getModel().getDecks().getDraft().get(humanDeck.getName()).getAiDecks().size();
            Singletons.getModel().getGauntletMini().launch(rounds, humanDeck, GameType.Draft);
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
                DeckGroup opponentDecks = Singletons.getModel().getDecks().getDraft().get(humanDeck.getName());
                Deck aiDeck = opponentDecks.getAiDecks().get(aiIndex);
                if (aiDeck == null) {
                    throw new IllegalStateException("Draft: Computer deck is null!");
                }

                MatchStartHelper starter = new MatchStartHelper();
                starter.addPlayer(Singletons.getControl().getLobby().findLocalPlayer(PlayerType.HUMAN), humanDeck);
                starter.addPlayer(opponents[aiIndex], aiDeck);

                MatchController mc = Singletons.getModel().getMatch();
                mc.initMatch(GameType.Draft, starter.getPlayerMap());
                mc.startRound();

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
        opponents = generatePlayers();
    }

    private LobbyPlayer[] generatePlayers() {
        Lobby lobby = Singletons.getControl().getLobby();
        LobbyPlayer[] ai = {
                lobby.findLocalPlayer(PlayerType.COMPUTER),
                lobby.findLocalPlayer(PlayerType.COMPUTER),
                lobby.findLocalPlayer(PlayerType.COMPUTER),
                lobby.findLocalPlayer(PlayerType.COMPUTER),
                lobby.findLocalPlayer(PlayerType.COMPUTER),
                lobby.findLocalPlayer(PlayerType.COMPUTER),
                lobby.findLocalPlayer(PlayerType.COMPUTER)
        };

        return ai;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
