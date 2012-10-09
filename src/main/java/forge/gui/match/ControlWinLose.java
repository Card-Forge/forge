package forge.gui.match;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import forge.AllZone;
import forge.Card;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.Deck;
import forge.game.GameNew;
import forge.game.GameSummary;
import forge.game.GameType;
import forge.game.PlayerStartsGame;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.SOverlayUtils;
import forge.gui.match.nonsingleton.VHand;
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

    /** @param v &emsp; ViewWinLose */
    public ControlWinLose(final ViewWinLose v) {
        this.view = v;
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
        startNextRound();
    }

    /** Action performed when "restart" button is pressed in default win/lose UI. */
    public void actionOnRestart() {
        Singletons.getModel().getMatchState().reset();
        SOverlayUtils.hideOverlay();
        startNextRound();
    }

    /** Action performed when "quit" button is pressed in default win/lose UI. */
    public void actionOnQuit() {
        // Clear cards off of playing areas
        final List<VHand> hands = VMatchUI.SINGLETON_INSTANCE.getHandViews();
        for (VHand h : hands) {
            h.getPlayer().getZone(ZoneType.Battlefield).reset();
            h.getPlayer().getZone(ZoneType.Hand).reset();
        }

        // Reset other stuff
        Singletons.getModel().getMatchState().reset();
        Singletons.getModel().savePrefs();
        Singletons.getControl().changeState(FControl.HOME_SCREEN);
        SOverlayUtils.hideOverlay();
    }

    /**
     * Either continues or restarts a current game. May be overridden for use
     * with other game modes.
     */
    public void startNextRound() {
        boolean isAnte = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ANTE);
        GameType gameType = Singletons.getModel().getMatchState().getGameType();

        //This is called from QuestWinLoseHandler also.  If we're in a quest, this is already handled elsewhere
        if (isAnte && !gameType.equals(GameType.Quest)) {
            executeAnte();
        }
        Singletons.getModel().savePrefs();
        CMatchUI.SINGLETON_INSTANCE.initMatch(null);
        GameNew.newGame( new PlayerStartsGame(AllZone.getHumanPlayer(), AllZone.getHumanPlayer().getDeck()), 
                         new PlayerStartsGame(AllZone.getComputerPlayer(), AllZone.getComputerPlayer().getDeck()));

    }

    /**
     * TODO: Write javadoc for this method.
     * @param hDeck
     * @param cDeck
     */
    private void executeAnte() {
        List<GameSummary> games = Singletons.getModel().getMatchState().getGamesPlayed();
        if ( games.isEmpty() ) return;
        GameSummary lastGame = games.get(games.size()-1);
        for (Player p: Singletons.getModel().getGameState().getPlayers()) {
            if (!p.getName().equals(lastGame.getWinner())) continue; // not a loser
            
            // remove all the lost cards from owners' decks
            List<CardPrinted> losses = new ArrayList<CardPrinted>();
            for (Player loser: Singletons.getModel().getGameState().getPlayers()) {
                if( loser.equals(p)) continue; // not a loser
                
                List<Card> compAntes = loser.getCardsIn(ZoneType.Ante);
                Deck cDeck = loser.getDeck(); 

                for (Card c : compAntes) {
                    CardPrinted toRemove = CardDb.instance().getCard(c);
                    cDeck.getMain().remove(toRemove);
                }
            }
            
            // offer to winner, if he is human
            if( p.isHuman() ) {
                List<CardPrinted> o = GuiChoose.noneOrMany("Select cards to add to your deck", losses);
                if (null != o) {
                    for (CardPrinted c : o) {
                        p.getDeck().getMain().add(c);
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
