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
package forge.control.input;

import forge.Card;
import forge.Singletons;
import forge.game.player.Player;
import forge.gui.match.CMatchUI;

/**
 * <p>
 * Abstract Input class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class InputBase implements java.io.Serializable, Input {
    /** Constant <code>serialVersionUID=-6539552513871194081L</code>. */
    private static final long serialVersionUID = -6539552513871194081L;

    // showMessage() is always the first method called
    @Override
    public abstract void showMessage();

    @Override
    public void selectCard(final Card c) {   }
    @Override
    public void selectPlayer(final Player player) {    }
    @Override
    public void selectButtonOK() {    }
    @Override
    public void selectButtonCancel() {    }

    // to remove need for CMatchUI dependence
    protected void showMessage(String message) { 
        CMatchUI.SINGLETON_INSTANCE.showMessage(message);
    }
    
    // called by input to cleanup.
    protected final void stop() {
        // clears a "temp" Input like Input_PayManaCost if there is one
        Singletons.getModel().getMatch().getInput().resetInput();
        afterStop(); // sync inputs will release their latch there
    }
    
    protected void afterStop() { }
}
