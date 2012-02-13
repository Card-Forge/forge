package forge.control.home;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import forge.AllZone;
import forge.Constant;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.Deck;
import forge.game.GameType;
import forge.game.limited.BoosterDraft;
import forge.game.limited.CardPoolLimitation;
import forge.gui.GuiUtils;
import forge.gui.deckeditor.DeckEditorDraft;
import forge.view.home.ViewDraft;
import forge.view.toolbox.FSkin;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ControlDraft {
    private final ViewDraft view;
    private final MouseListener madBuildDeck, madDirections, madStartGame;

    /** @param v0 &emsp; ViewDraft */
    public ControlDraft(ViewDraft v0) {
        this.view = v0;
        updateHumanDecks();
        view.getLstAIDecks().setSelectedIndex(0);

        // Action listeners
        madDirections = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                view.showDirections();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                view.getLblDirections().setForeground(FSkin.getColor(FSkin.Colors.CLR_HOVER));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                view.getLblDirections().setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            }
        };

        madBuildDeck = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { setupDraft(); }
        };

        madStartGame = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                final Thread t = new Thread() {
                    @Override
                    public void run() {
                        startGame();
                    }
                };
                t.start();
            }
        };

        addListeners();
    }

    private void addListeners() {
        view.getBtnBuildDeck().removeMouseListener(madBuildDeck);
        view.getBtnBuildDeck().addMouseListener(madBuildDeck);

        view.getLblDirections().removeMouseListener(madDirections);
        view.getLblDirections().addMouseListener(madDirections);

        view.getBtnStart().removeMouseListener(madStartGame);
        view.getBtnStart().addMouseListener(madStartGame);
    }

    /** */
    private void startGame() {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "ControlDraft() > startGame() must be accessed from outside the event dispatch thread.");
        }

        Deck human = view.getLstHumanDecks().getSelectedDeck();
        int aiIndex = view.getLstAIDecks().getSelectedIndex();

        if (human == null) {
            JOptionPane.showMessageDialog(null,
                    "No deck selected for human!\r\n(You may need to build a new deck.)",
                    "No deck", JOptionPane.ERROR_MESSAGE);
            return;
        }
        else if (human.getMain().countAll() < 40) {
            JOptionPane.showMessageDialog(null,
                    "The selected deck doesn't have enough cards to play (minimum 40)."
                    + "\r\nUse the deck editor to choose the cards you want before starting.",
                    "No deck", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // If everything is OK, show progress bar and start inits.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view.getBarProgress().setMaximum(2);
                view.getBarProgress().reset();
                view.getBarProgress().setShowETA(false);
                view.getBarProgress().setShowCount(false);
                view.getBarProgress().setDescription("Starting New Game");
                view.getBarProgress().setVisible(true);
                view.getBtnStart().setVisible(false);
            }
        });

        Deck[] opponentDecks = AllZone.getDeckManager().getDraftDeck(human.getName());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view.getBarProgress().increment();
            }
         });

        Constant.Runtime.HUMAN_DECK[0] = human;
        Constant.Runtime.COMPUTER_DECK[0] = opponentDecks[aiIndex + 1]; //zero is human deck, so it must be +1

        if (Constant.Runtime.COMPUTER_DECK[0] == null) {
            throw new IllegalStateException("startButton() error - computer deck is null");
        }

        Constant.Runtime.setGameType(GameType.Draft);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view.getBtnStart().setVisible(true);
                view.getBarProgress().setVisible(false);

                Singletons.getControl().changeState(FControl.MATCH_SCREEN);
                Singletons.getControl().getMatchControl().initMatch();
                AllZone.getGameAction().newGame(Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0]);
            }
        });
    }

    /** */
    public void setupDraft() {
        final DeckEditorDraft draft = new DeckEditorDraft();

        // Determine what kind of booster draft to run
        final ArrayList<String> draftTypes = new ArrayList<String>();
        draftTypes.add("Full Cardpool");
        draftTypes.add("Block / Set");
        draftTypes.add("Custom");

        final String prompt = "Choose Draft Format:";
        final Object o = GuiUtils.getChoice(prompt, draftTypes.toArray());

        if (o.toString().equals(draftTypes.get(0))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.Full));
        }

        else if (o.toString().equals(draftTypes.get(1))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.Block));
        }

        else if (o.toString().equals(draftTypes.get(2))) {
            draft.showGui(new BoosterDraft(CardPoolLimitation.Custom));
        }

    }

    /** Updates deck list in view. */
    public void updateHumanDecks() {
        Collection<Deck[]> temp = AllZone.getDeckManager().getDraftDecks().values();
        List<Deck> human = new ArrayList<Deck>();
        for (Deck[] d : temp) { human.add(d[0]); }
        view.getLstHumanDecks().setDecks(human.toArray(new Deck[0]));
    }
}
