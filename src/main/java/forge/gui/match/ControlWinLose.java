package forge.gui.match;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import forge.Card;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.Deck;
import forge.game.GameOutcome;
import forge.game.GameType;
import forge.game.Match;
import forge.game.RegisteredPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.SOverlayUtils;
import forge.item.CardDb;
import forge.item.PaperCard;
import forge.properties.ForgePreferences.FPref;

/** 
 * Default controller for a ViewWinLose object. This class can
 * be extended for various game modes to populate the custom
 * panel in the win/lose screen.
 *
 */
public class ControlWinLose {
    private final ViewWinLose view;
    protected final Match match;

    /** @param v &emsp; ViewWinLose
     * @param match */
    public ControlWinLose(final ViewWinLose v, Match match) {
        this.view = v;
        this.match = match;
        addListeners();
    }

    /** */
    public void addListeners() {
        view.getBtnContinue().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                actionOnContinue();
            }
        });

        view.getBtnRestart().addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                actionOnRestart();
            }
        });

        view.getBtnQuit().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                actionOnQuit();
                ((JButton) e.getSource()).setEnabled(false);
            }
        });
    }

    /** Action performed when "continue" button is pressed in default win/lose UI. */
    public void actionOnContinue() {
        SOverlayUtils.hideOverlay();
        saveOptions();

        boolean isAnte = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ANTE);

        //This is called from QuestWinLose also.  If we're in a quest, this is already handled elsewhere
        if (isAnte && match.getGameType() != GameType.Quest) {
            executeAnte();
        }

        match.startRound();
    }

    /** Action performed when "restart" button is pressed in default win/lose UI. */
    public void actionOnRestart() {
        SOverlayUtils.hideOverlay();
        saveOptions();
        match.clearGamesPlayed();
        match.startRound();
    }

    /** Action performed when "quit" button is pressed in default win/lose UI. */
    public void actionOnQuit() {
        // Reset other stuff
        saveOptions();
        Singletons.getControl().changeState(FControl.Screens.HOME_SCREEN);
        SOverlayUtils.hideOverlay();
    }

    /**
     * Either continues or restarts a current game. May be overridden for use
     * with other game modes.
     */
    public void saveOptions() {
        Singletons.getModel().getPreferences().writeMatchPreferences();
        Singletons.getModel().getPreferences().save();
    }

    /**
     * TODO: Write javadoc for this method.
     * @param hDeck
     * @param cDeck
     */
    private void executeAnte() {
        List<GameOutcome> games = match.getPlayedGames();

        if (games.isEmpty()) {
            return;
        }

        // remove all the lost cards from owners' decks
        List<PaperCard> losses = new ArrayList<PaperCard>();
        int cntPlayers = match.getPlayers().size();
        for (int i = 0; i < cntPlayers; i++ ) {
            Player fromGame = match.getCurrentGame().getRegisteredPlayers().get(i);
            if( !fromGame.hasLost()) continue; // not a loser
            

            List<Card> compAntes = new ArrayList<Card>(fromGame.getCardsIn(ZoneType.Ante));
            RegisteredPlayer psc = match.getPlayers().get(i);
            Deck cDeck = psc.getCurrentDeck();
            Deck oDeck = psc.getOriginalDeck();

            for (Card c : compAntes) {
                PaperCard toRemove = CardDb.getCard(c);
                cDeck.getMain().remove(toRemove);
                if ( cDeck != oDeck )
                    oDeck.getMain().remove(toRemove);
                losses.add(toRemove);
            }
        }

        for (int i = 0; i < cntPlayers; i++ ) {
            Player fromGame = match.getCurrentGame().getRegisteredPlayers().get(i);
            if( !fromGame.hasWon()) continue; // not a loser
            
            // offer to winner, if he is local human
            if (fromGame.isHuman()) {
                List<PaperCard> chosen = GuiChoose.noneOrMany("Select cards to add to your deck", losses);
                if (null != chosen) {
                    RegisteredPlayer psc = match.getPlayers().get(i);
                    Deck cDeck = psc.getCurrentDeck();
                    //Deck oDeck = psc.getOriginalDeck();
                    for (PaperCard c : chosen) {
                        cDeck.getMain().add(c);
                        //oDeck.getMain().add(c);
                    }
                }
            }
        }

    }

    /**
     * <p>
     * populateCustomPanel.
     * </p>
     * May be overridden as required by controllers for various game modes
     * to show custom information in center panel. Default configuration is empty.
     * 
     * @return boolean, panel has contents or not.
     */
    public boolean populateCustomPanel() {
        return false;
    }

    /** @return ViewWinLose object this controller is in charge of */
    public ViewWinLose getView() {
        return this.view;
    }
}
