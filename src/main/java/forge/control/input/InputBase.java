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
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.CMatchUI;
import forge.gui.match.views.VMessage;

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
    private InputQueue queue;
    private boolean finished = false;
    protected final boolean isFinished() { return finished; }
    protected final void setFinished() { finished = true; }
    
    // showMessage() is always the first method called
    @Override
    public final void showMessage(InputQueue iq) {
        queue = iq;
        finished = false;
        showMessage();
    }
    
    protected abstract void showMessage();

    @Override
    public void selectCard(final Card c, boolean isMetaDown) {   }
    @Override
    public void selectPlayer(final Player player) {    }
    @Override
    public void selectButtonOK() {    }
    @Override
    public void selectButtonCancel() {    }

    // to remove need for CMatchUI dependence
    protected final void showMessage(String message) { 
        CMatchUI.SINGLETON_INSTANCE.showMessage(message);
    }
    

    protected void afterStop() { }

    protected final void flashIncorrectAction() {
        SDisplayUtil.remind(VMessage.SINGLETON_INSTANCE);
    }

    protected InputQueue getQueue() {
        return queue;
    }

    protected String getTurnPhasePriorityMessage(Player player) {
        final PhaseHandler ph = player.getGame().getPhaseHandler();
        final StringBuilder sb = new StringBuilder();
    
        sb.append("Priority: ").append(player).append("\n").append("\n");
        sb.append("Turn : ").append(ph.getPlayerTurn()).append("\n");
        sb.append("Phase: ").append(ph.getPhase().NameForUi).append("\n");
        sb.append("Stack: ");
        if (!player.getGame().getStack().isEmpty()) {
            sb.append(player.getGame().getStack().size()).append(" to Resolve.");
        } else {
            sb.append("Empty");
        }
        sb.append("\n");
        String message = sb.toString();
        return message;
    }
}
