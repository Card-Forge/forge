/*
 * Forge: Play Magic: the Gathering.
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
package forge.gui.input;

import forge.game.player.Player;
import forge.view.ButtonUtil;
 /**
  * <p>
  * InputMulligan class.
  * </p>
  * 
  * @author Forge
  * @version $Id: InputConfirmMulligan.java 21647 2013-05-24 22:31:11Z Max mtg $
  */
public class InputPlayOrDraw extends InputSyncronizedBase {
    /** Constant <code>serialVersionUID=-8112954303001155622L</code>. */
    private static final long serialVersionUID = -8112954303001155622L;
    
    private final Player startingPlayer;
    private final boolean firstGame;
    private boolean willPlayFirst = true;
    
    public InputPlayOrDraw(Player startsGame, boolean isFirstGame) {
        startingPlayer = startsGame;
        firstGame = isFirstGame;
    }
    
    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        ButtonUtil.setButtonText("Play", "Draw");
        ButtonUtil.enableAllFocusOk();

        StringBuilder sb = new StringBuilder();
        sb.append(startingPlayer.getName()).append(", you ").append(firstGame ? " have won the coin toss." : " lost the last game.");
        sb.append("\n\n");
        sb.append("Would you like to play or draw?");
        showMessage(sb.toString());
    }

    /** {@inheritDoc} */
    @Override
    protected final void onOk() {
        willPlayFirst = true;
        done();
    }

    /** {@inheritDoc} */
    @Override
    protected final void onCancel() {
        willPlayFirst = false;
        done();
    }

    private void done() {
        ButtonUtil.reset();
        stop();
    }

    public final boolean isPlayingFirst() {
        return willPlayFirst;
    }
}
