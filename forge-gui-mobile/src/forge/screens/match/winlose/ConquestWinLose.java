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

import forge.game.GameView;
import forge.model.FModel;

public class ConquestWinLose extends ControlWinLose {
    public ConquestWinLose(final ViewWinLose view0, GameView lastGame0) {
        super(view0, lastGame0);
        view0.getBtnContinue().setVisible(false);
        view0.getBtnRestart().setVisible(false);
    }

    @Override
    public final void showRewards() {
        FModel.getConquest().showGameOutcome(lastGame, getView());
    }

    @Override
    public final void actionOnQuit() {
        FModel.getConquest().finishEvent();
        super.actionOnQuit();
    }
}
