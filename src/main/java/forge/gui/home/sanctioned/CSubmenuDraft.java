package forge.gui.home.sanctioned;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.tuple.Pair;

import forge.Command;
import forge.FThreads;
import forge.Singletons;
import forge.control.FControl;
import forge.control.Lobby;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameType;
import forge.game.MatchController;
import forge.game.PlayerStartConditions;
import forge.game.limited.BoosterDraft;
import forge.game.limited.LimitedPoolType;
import forge.game.player.LobbyPlayer;
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
        public void run() {
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
                    public void run() { setupDraft(); } });

        view.getBtnStart().addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) { startGame(GameType.Draft); } });
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

        final VSubmenuDraft view = VSubmenuDraft.SINGLETON_INSTANCE;
        final JButton btnStart = view.getBtnStart();
        
        view.getLstDecks().setDecks(human);

        if (human.size() > 1) {
            btnStart.setEnabled(true);
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (btnStart.isEnabled()) {
                    view.getBtnStart().requestFocusInWindow();
                } else {
                    view.getBtnBuildDeck().requestFocusInWindow();
                }
            }
        });
    }

    private void startGame(final GameType gameType) {
        final boolean gauntlet = !VSubmenuDraft.SINGLETON_INSTANCE.isSingleSelected();
        final Deck humanDeck = VSubmenuDraft.SINGLETON_INSTANCE.getLstDecks().getSelectedDeck();
        final int aiIndex = (int) Math.floor(Math.random() * 8);

        if (humanDeck == null) {
            JOptionPane.showMessageDialog(null,
                    "No deck selected for human!\r\n(You may need to build a new deck.)",
                    "No deck", JOptionPane.ERROR_MESSAGE);
            return;
        } 
        String errorMessage = gameType.getDecksFormat().getDeckConformanceProblem(humanDeck);
        if (null != errorMessage) {
            JOptionPane.showMessageDialog(null, "Your deck " + errorMessage +  " Please edit or choose a different deck.", "Invalid deck", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Singletons.getModel().getGauntletMini().resetGauntletDraft();

        if (gauntlet) {
            int rounds = Singletons.getModel().getDecks().getDraft().get(humanDeck.getName()).getAiDecks().size();
            Singletons.getModel().getGauntletMini().launch(rounds, humanDeck, gameType);
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

 
        DeckGroup opponentDecks = Singletons.getModel().getDecks().getDraft().get(humanDeck.getName());
        Deck aiDeck = opponentDecks.getAiDecks().get(aiIndex);
        if (aiDeck == null) {
            throw new IllegalStateException("Draft: Computer deck is null!");
        }

        List<Pair<LobbyPlayer, PlayerStartConditions>> starter = new ArrayList<Pair<LobbyPlayer, PlayerStartConditions>>();
        Lobby lobby = Singletons.getControl().getLobby();
        starter.add(Pair.of(lobby.getGuiPlayer(), PlayerStartConditions.fromDeck(humanDeck)));
        starter.add(Pair.of(lobby.getAiPlayer(), PlayerStartConditions.fromDeck(aiDeck)));

        final MatchController mc = new MatchController(GameType.Draft, starter);
        FThreads.invokeInEdtLater(new Runnable(){
            @Override
            public void run() {
                mc.startRound();
                SOverlayUtils.hideOverlay();
            }
        });
    }

    /** */
    private void setupDraft() {
        

        // Determine what kind of booster draft to run
        final String prompt = "Choose Draft Format:";
        final LimitedPoolType o = GuiChoose.oneOrNone(prompt, LimitedPoolType.values());
        if ( o == null ) return;
        
        final CEditorDraftingProcess draft = new CEditorDraftingProcess();
        draft.showGui(new BoosterDraft(o));

        FControl.SINGLETON_INSTANCE.changeState(FControl.Screens.DRAFTING_PROCESS);
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
