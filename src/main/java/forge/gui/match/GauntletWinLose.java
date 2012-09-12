package forge.gui.match;

/** Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import forge.AllZone;
import forge.Singletons;
import javax.swing.JLabel;

import forge.gui.toolbox.FSkin;
import forge.model.FMatchState;
import forge.game.limited.GauntletMini;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.SwingConstants;

/**
 * The Win/Lose handler for 'gauntlet' type tournament
 * games.
 */
public class GauntletWinLose extends ControlWinLose {

    private final transient boolean wonMatch;
    private transient ViewWinLose view;
    private GauntletMini gauntlet;
    private boolean nextRound = false;

    /** String constraint parameters. */
    private static final String CONSTRAINTS_TITLE = "w 95%!, gap 0 0 20px 10px";
    private static final String CONSTRAINTS_TEXT = "w 95%!,, h 180px!, gap 0 0 0 20px";

    private transient JLabel lblTemp1;
    private transient JLabel lblTemp2;
    private final transient FMatchState matchState;

    /**
     * Instantiates a new gauntlet win/lose handler.
     * 
     * @param view0 ViewWinLose object
     */
    public GauntletWinLose(final ViewWinLose view0) {
        super(view0);
        this.view = view0;
        gauntlet = AllZone.getGauntlet();
        matchState = Singletons.getModel().getMatchState();
        this.wonMatch = matchState.isMatchWonBy(AllZone.getHumanPlayer().getName());
    }


    /**
     * <p>
     * populateCustomPanel.
     * </p>
     * @return true
     */
    @Override
    public final boolean populateCustomPanel() {

        // view.getBtnRestart().setVisible(false);
        // Deliberate; allow replaying bad tournaments

        //TODO: do per-game actions like ante here...

        resetView();
        nextRound = false;



        if (Singletons.getModel().getMatchState().hasWonLastGame(AllZone.getHumanPlayer().getName())) {
            gauntlet.addWin();
        }
        else {
            gauntlet.addLoss();
        }

        view.getBtnRestart().setText("Restart Round");

        if (!matchState.isMatchOver()) {
            showTournamentInfo("Tournament Info");
            return true;
        } else {
            if (this.wonMatch) {
                if (gauntlet.getCurrentRound() < gauntlet.getRounds()) {
                    view.getBtnContinue().setText("Next Round (" + (gauntlet.getCurrentRound() + 1)
                            + "/" + gauntlet.getRounds() + ")");
                    nextRound = true;
                    view.getBtnContinue().setEnabled(true);
                    showTournamentInfo("YOU HAVE WON ROUND " + gauntlet.getCurrentRound() + "/"
                            + gauntlet.getRounds());
                }
                else {
                    showTournamentInfo("***CONGRATULATIONS! YOU HAVE WON THE TOURNAMENT!***");
                }
            } else {
                showTournamentInfo("YOU HAVE LOST ON ROUND " + gauntlet.getCurrentRound() + "/"
                        + gauntlet.getRounds());
                view.getBtnContinue().setVisible(false);
            }
        }



        return true;
    }

    /**
     * <p>
     * Shows some tournament info in the custom panel.
     * </p>
     * @param String - the title to be displayed
     */
    private void showTournamentInfo(final String newTitle) {

        this.lblTemp1 = new TitleLabel(newTitle);
        this.lblTemp2 = new JLabel("Round: " + gauntlet.getCurrentRound() + "/" + gauntlet.getRounds()
                + "      Total Wins: " + gauntlet.getWins()
                + "      Total Losses: " + gauntlet.getLosses());
        this.lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
        this.lblTemp2.setFont(FSkin.getFont(17));
        this.lblTemp2.setForeground(Color.white);
        this.lblTemp2.setIconTextGap(50);
        this.getView().getPnlCustom().add(this.lblTemp1, GauntletWinLose.CONSTRAINTS_TITLE);
        this.getView().getPnlCustom().add(this.lblTemp2, GauntletWinLose.CONSTRAINTS_TEXT);
    }

    /**
     * <p>
     * actionOnRestart.
     * </p>
     * When "restart" button is pressed, this method restarts the current round.
     * 
     */
    @Override
    public final void actionOnRestart() {
        resetView();
        // gauntlet.resetCurrentRound();
        super.actionOnRestart();
    }

    /**
     * <p>
     * actionOnQuit.
     * </p>
     * When "quit" button is pressed, we exit the tournament.
     * 
     */
    @Override
    public final void actionOnQuit() {
        resetView();
        gauntlet.resetCurrentRound();
        super.actionOnQuit();
    }

    /**
     * <p>
     * actionOnContinue.
     * </p>
     * When "continue / next round" button is pressed, we either continue
     * to the next game in the current match or (next round) proceed to
     * the next round in the mini tournament.
     * 
     */
    @Override
    public final void actionOnContinue() {
        resetView();

        if (nextRound) {
            gauntlet.nextRound();
            super.actionOnRestart();
        }
        else {
            super.actionOnContinue();
        }
    }

    /**
     * <p>
     * ResetView
     * </p>
     * Restore the default texts to the win/lose panel buttons.
     * 
     */
    private void resetView() {
        view.getBtnQuit().setText("Quit");
        view.getBtnContinue().setText("Continue");
        view.getBtnRestart().setText("Restart");
    }

    /**
     * JLabel header, adapted from QuestWinLoseHandler.
     * 
     */
    @SuppressWarnings("serial")
    private class TitleLabel extends JLabel {
        TitleLabel(final String msg) {
            super(msg);
            this.setFont(FSkin.getFont(18));
            this.setPreferredSize(new Dimension(200, 40));
            this.setHorizontalAlignment(SwingConstants.CENTER);
            this.setForeground(Color.white);
            this.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.white));
        }
    }

}
