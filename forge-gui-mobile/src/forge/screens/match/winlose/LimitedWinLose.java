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

import forge.game.GameView;
import forge.gamemodes.limited.LimitedWinLoseController;

/**
 * The Win/Lose handler for 'gauntlet' type tournament
 * games.
 */
public class LimitedWinLose extends ControlWinLose {
    private final LimitedWinLoseController controller;

    /**
     * Instantiates a new limited mode win/lose handler.
     * 
     * @param view0 {@link forge.gui.match.ViewWinLose}
     * @param match {@link forge.game.Match}
     */
    public LimitedWinLose(final ViewWinLose view0, GameView lastGame) {
        super(view0, lastGame);
        controller = new LimitedWinLoseController(view0, lastGame) {
            @Override
            protected void showOutcome(Runnable runnable) {
                //invoke reward logic in background thread so dialogs can be shown
                //FThreads.invokeInBackgroundThread(runnable);
                runnable.run(); //background thread only needed if message shown
            }

            @Override
            protected void showMessage(String message, String title) {
                //avoid showing unnecessary message for mobile game
                //TODO: Consider a better way to do this
                //SOptionPane.showMessageDialog(message, title);
            }

            @Override
            protected void saveOptions() {
                LimitedWinLose.this.saveOptions();
            }
        };
    }

    @Override
    public final void showRewards() {
        controller.showOutcome();
    }

    @Override
    public final void actionOnRestart() {
        controller.actionOnRestart();
        super.actionOnRestart();
    }

    @Override
    public final void actionOnQuit() {
        controller.actionOnQuit();
        super.actionOnQuit();
    }

    @Override
    public final void actionOnContinue() {
        if (!controller.actionOnContinue()) {
            super.actionOnContinue();
        }
    }
}
