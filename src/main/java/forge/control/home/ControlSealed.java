package forge.control.home;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;

import net.slightlymagic.braids.util.UtilFunctions;
import forge.AllZone;
import forge.Command;
import forge.Constant;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.Deck;
import forge.deck.DeckSet;
import forge.game.GameType;
import forge.game.limited.SealedDeck;
import forge.gui.GuiUtils;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.view.home.ViewSealed;

/** 
 * Controls behavior of swing components in "sealed" mode menu.
 *
 */
public class ControlSealed {
    private ViewSealed view;
    private Map<String, Deck> aiDecks;
    private final MouseListener madBuildDeck, madStartGame;

    /**
     * Controls behavior of swing components in "sealed" mode menu.
     * 
     * @param v0 &emsp; ViewSealed
     */
    public ControlSealed(ViewSealed v0) {
        view = v0;
        Constant.Runtime.setGameType(GameType.Sealed);

        madBuildDeck = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { setupSealed(); }
        };

        // Game start logic must happen outside of the EDT.
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
    }

    /** */
    public void addListeners() {
        view.getBtnBuild().removeMouseListener(madBuildDeck);
        view.getBtnBuild().addMouseListener(madBuildDeck);

        view.getBtnStart().removeMouseListener(madStartGame);
        view.getBtnStart().addMouseListener(madStartGame);
    }

    /** Start button has been pressed. */
    public void startGame() {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "ControlSealed() > startGame() must be accessed from outside the event dispatch thread.");
        }

        Deck human = view.getLstHumanDecks().getSelectedDeck();

        if (human == null) {
            JOptionPane.showMessageDialog(null,
                    "Please build and/or select a deck for yourself.",
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

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view.getBarProgress().increment();
            }
         });

        Constant.Runtime.HUMAN_DECK[0] = human;
        Constant.Runtime.COMPUTER_DECK[0] = AllZone.getDecks().getSealed().get(human.getName()).getAiDecks().get(0);

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
    public void updateDeckLists() {
        List<Deck> humanDecks = new ArrayList<Deck>();
        aiDecks = new HashMap<String, Deck>();

        // Since AI decks are tied directly to the human choice,
        // they're just mapped in a parallel map and grabbed when the game starts.
        for (DeckSet d : AllZone.getDecks().getSealed()) {
            aiDecks.put(d.getName(), d.getAiDecks().get(0));
            humanDecks.add(d.getHumanDeck());
        }

        view.getLstHumanDecks().setDecks(humanDecks);
    }

    /** Build button has been pressed. */
    public void setupSealed() {
        ArrayList<String> sealedTypes = new ArrayList<String>();
        sealedTypes.add("Full Cardpool");
        sealedTypes.add("Block / Set");
        sealedTypes.add("Custom");

        final String prompt = "Choose Sealed Deck Format:";
        final Object o = GuiUtils.getChoice(prompt, sealedTypes.toArray());

        SealedDeck sd = null;

        if (o.toString().equals(sealedTypes.get(0))) {
            sd = new SealedDeck("Full");
        }

        else if (o.toString().equals(sealedTypes.get(1))) {
            sd = new SealedDeck("Block");
        }

        else if (o.toString().equals(sealedTypes.get(2))) {
            sd = new SealedDeck("Custom");
        }
        else {
            throw new IllegalStateException("choice <<" + UtilFunctions.safeToString(o)
                    + ">> does not equal any of the sealedTypes.");
        }

        if (sd.getCardpool().isEmpty()) {
            return;
        }

        final String sDeckName = JOptionPane.showInputDialog(null,
                ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.SAVE_SEALED_MSG),
                ForgeProps.getLocalized(NewConstants.Lang.OldGuiNewGame.NewGameText.SAVE_SEALED_TTL),
                JOptionPane.QUESTION_MESSAGE);

        if (StringUtils.isBlank(sDeckName)) {
            return;
        }

        // May check for name uniqueness here

        final ItemPool<CardPrinted> sDeck = sd.getCardpool();

        Deck deck = new Deck(sDeckName);
        deck.getSideboard().addAll(sDeck);

        for (final String element : Constant.Color.BASIC_LANDS) {
            deck.getSideboard().add(element, sd.getLandSetCode()[0], 18);
        }

        DeckSet sealed = new DeckSet(sDeckName);
        sealed.setHumanDeck(deck);
        sealed.addAiDeck(sd.buildAIDeck(sDeck.toForgeCardList()));
        AllZone.getDecks().getSealed().add(sealed);

        view.getParentView().getUtilitiesController().showDeckEditor(GameType.Sealed, sealed);

    }

    /** @return {@link forge.Command} What to do when the deck editor exits. */
    public Command getExitCommand() {
        Command exit = new Command() {
            private static final long serialVersionUID = -9133358399503226853L;

            @Override
            public void execute() {
                updateDeckLists();
            }
        };

        return exit;
    }
}
