package forge.screens.match.winlose;

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

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.GuiBase;
import forge.assets.FSkinFont;
import forge.game.Game;
import forge.limited.GauntletMini;
import forge.model.FModel;
import forge.toolbox.FLabel;

/**
 * The Win/Lose handler for 'gauntlet' type tournament
 * games.
 */
public class LimitedWinLose extends ControlWinLose {

    private final boolean wonMatch;
    private ViewWinLose view;
    private GauntletMini gauntlet;
    private boolean nextRound = false;

    /**
     * Instantiates a new limited mode win/lose handler.
     * 
     * @param view0 {@link forge.gui.match.ViewWinLose}
     * @param match {@link forge.game.Match}
     */
    public LimitedWinLose(final ViewWinLose view0, Game lastGame) {
        super(view0, lastGame);
        this.view = view0;
        gauntlet = FModel.getGauntletMini();
        this.wonMatch = lastGame.getMatch().isWonBy(GuiBase.getInterface().getGuiPlayer());
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

        if (lastGame.getOutcome().isWinner(GuiBase.getInterface().getGuiPlayer())) {
            gauntlet.addWin();
        }
        else {
            gauntlet.addLoss();
        }

        view.getBtnRestart().setText("Restart Round");

        if (!lastGame.getMatch().isMatchOver()) {
            showTournamentInfo("Tournament Info");
            return true;
        }
        else {
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
            }
            else {
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
        final FLabel lblTitle = new FLabel.Builder().text(newTitle).font(FSkinFont.get(18)).align(HAlignment.CENTER).build();
        final FLabel lblSubTitle = new FLabel.Builder().text("Round: " + gauntlet.getCurrentRound() + "/" + gauntlet.getRounds())
                .align(HAlignment.CENTER).font(FSkinFont.get(17)).build();
        this.getView().getPnlCustom().add(lblTitle);
        this.getView().getPnlCustom().add(lblSubTitle);
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
            view.hide();
            saveOptions();
            gauntlet.nextRound();
        }
        else { // noone will get here - if round is lost, the button is inivisible anyway
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
}
