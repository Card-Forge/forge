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
import forge.game.MatchController;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.SOverlayUtils;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.properties.ForgePreferences.FPref;

/** 
 * Default controller for a ViewWinLose object. This class can
 * be extended for various game modes to populate the custom
 * panel in the win/lose screen.
 *
 */
public class ControlWinLose {
    private final ViewWinLose view;
    protected final MatchController match;

    /** @param v &emsp; ViewWinLose 
     * @param match */
    public ControlWinLose(final ViewWinLose v, MatchController match) {
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
        match.replay();
    }

    /** Action performed when "quit" button is pressed in default win/lose UI. */
    public void actionOnQuit() {
        // Reset other stuff
        saveOptions();
        Singletons.getControl().changeState(FControl.HOME_SCREEN);
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
        

        GameOutcome lastGame = match.getLastGameOutcome();
        if ( games.isEmpty() ) return;
        
        for (Player p: Singletons.getModel().getGame().getPlayers()) {
            if (!p.getName().equals(lastGame.getWinner())) continue; // not a loser
            
            // remove all the lost cards from owners' decks
            List<CardPrinted> losses = new ArrayList<CardPrinted>();
            for (Player loser : Singletons.getModel().getGame().getPlayers()) {
                if (loser.equals(p)) {
                    continue; // not a loser
                }

                List<Card> compAntes = loser.getCardsIn(ZoneType.Ante);
                Deck cDeck = match.getPlayersDeck(loser.getLobbyPlayer());

                for (Card c : compAntes) {
                    CardPrinted toRemove = CardDb.instance().getCard(c);
                    cDeck.getMain().remove(toRemove);
                }
            }

            // offer to winner, if he is human
            if (p.isHuman()) {
                List<CardPrinted> chosen = GuiChoose.noneOrMany("Select cards to add to your deck", losses);
                if (null != chosen) {
                    Deck d = match.getPlayersDeck(p.getLobbyPlayer());
                    for (CardPrinted c : chosen) {
                        d.getMain().add(c);
                    }
                }
            }

            break; // expect no other winners
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
