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
package forge.screens.match.winlose;

import java.util.List;

import forge.FThreads;
import forge.assets.FSkinProp;
import forge.game.GameView;
import forge.item.PaperCard;
import forge.quest.QuestWinLoseController;
import forge.util.gui.SGuiChoose;
import forge.util.gui.SOptionPane;

/**
 * <p>
 * QuestWinLose.
 * </p>
 * Processes win/lose presentation for Quest events. This presentation is
 * displayed by WinLoseFrame. Components to be added to pnlCustom in
 * WinLoseFrame should use MigLayout.
 * 
 */
public class QuestWinLose extends ControlWinLose {
    private final QuestWinLoseController controller;

    /**
     * Instantiates a new quest win lose handler.
     * 
     * @param view0 ViewWinLose object
     * @param match2
     */
    public QuestWinLose(final ViewWinLose view0, GameView lastGame) {
        super(view0, lastGame);
        controller = new QuestWinLoseController(lastGame) {
            @Override
            protected void showRewards(Runnable runnable) {
                //invoke reward logic in background thread so dialogs can be shown
                FThreads.invokeInBackgroundThread(runnable);
            }

            @Override
            protected void showCards(String title, List<PaperCard> cards) {
                SGuiChoose.reveal(title, cards);
            }

            @Override
            protected void showMessage(String message, String title, FSkinProp icon) {
                SOptionPane.showMessageDialog(message, title, icon);
            }
        };
    }

    @Override
    public final void showRewards() {
        controller.showRewards(getView());
    }

    /**
     * <p>
     * actionOnQuit.
     * </p>
     * When "quit" button is pressed, this method adjusts quest data as
     * appropriate and saves.
     * 
     */
    @Override
    public final void actionOnQuit() {
        controller.actionOnQuit();
        super.actionOnQuit();
    }
}
