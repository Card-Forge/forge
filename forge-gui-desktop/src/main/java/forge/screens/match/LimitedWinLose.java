package forge.screens.match;

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

import java.awt.Dimension;

import javax.swing.SwingConstants;

import forge.game.GameView;
import forge.limited.LimitedWinLoseController;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.Colors;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinnedLabel;

/**
 * The Win/Lose handler for 'gauntlet' type tournament
 * games.
 */
public class LimitedWinLose extends ControlWinLose {
    private final LimitedWinLoseController controller;

    /** String constraint parameters. */
    private static final SkinColor FORE_COLOR = FSkin.getColor(Colors.CLR_TEXT);
    private static final String CONSTRAINTS_TITLE = "w 95%!, gap 0 0 20px 10px";
    private static final String CONSTRAINTS_TEXT = "w 95%!,, h 180px!, gap 0 0 0 20px";

    /**
     * Instantiates a new limited mode win/lose handler.
     * 
     * @param view0 {@link forge.screens.match.ViewWinLose}
     * @param match {@link forge.game.Match}
     */
    public LimitedWinLose(final ViewWinLose view0, final GameView game0, final CMatchUI matchUI) {
        super(view0, game0, matchUI);
        controller = new LimitedWinLoseController(view0, game0) {
            @Override
            protected void showOutcome(Runnable runnable) {
                runnable.run(); //just run on GUI thread
            }

            @Override
            protected void showMessage(String message, String title) {
                TitleLabel lblTemp1 = new TitleLabel(title);
                SkinnedLabel lblTemp2 = new SkinnedLabel(message);
                lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
                lblTemp2.setFont(FSkin.getFont(17));
                lblTemp2.setForeground(FORE_COLOR);
                lblTemp2.setIconTextGap(50);
                getView().getPnlCustom().add(lblTemp1, LimitedWinLose.CONSTRAINTS_TITLE);
                getView().getPnlCustom().add(lblTemp2, LimitedWinLose.CONSTRAINTS_TEXT);
            }

            @Override
            protected void saveOptions() {
                LimitedWinLose.this.saveOptions();
            }
        };
    }


    /**
     * <p>
     * populateCustomPanel.
     * </p>
     * @return true
     */
    @Override
    public final boolean populateCustomPanel() {
        controller.showOutcome();
        return true;
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
        controller.actionOnRestart();
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
        controller.actionOnQuit();
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
        if (!controller.actionOnContinue()) {
            super.actionOnContinue();
        }
    }

    /**
     * JLabel header, adapted from QuestWinLose.
     * 
     */
    @SuppressWarnings("serial")
    private class TitleLabel extends SkinnedLabel {
        TitleLabel(final String msg) {
            super(msg);
            setFont(FSkin.getFont(18));
            setPreferredSize(new Dimension(200, 40));
            setHorizontalAlignment(SwingConstants.CENTER);
            setForeground(FORE_COLOR);
            setBorder(new FSkin.MatteSkinBorder(1, 0, 1, 0, FORE_COLOR));
        }
    }

}
