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
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.gui.GuiDisplayUtil;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.CMatchUI;
import forge.gui.match.views.VMessage;
import forge.view.ButtonUtil;

/**
 * <p>
 * Input_PassPriority class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputPassPriority extends Input implements java.io.Serializable {
    /** Constant <code>serialVersionUID=-581477682214137181L</code>. */
    private static final long serialVersionUID = -581477682214137181L;

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        GuiDisplayUtil.updateGUI();
        ButtonUtil.enableOnlyOK();

        final PhaseType phase = Singletons.getModel().getGame().getPhaseHandler().getPhase();
        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPriorityPlayer();

        if (player.isComputer()) {
            System.out.println(phase + ": Computer in passpriority");
        }

        final StringBuilder sb = new StringBuilder();

        sb.append("Turn : ").append(Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn()).append("\n");
        sb.append("Phase: ").append(phase.Name).append("\n");
        sb.append("Stack: ");
        if (Singletons.getModel().getGame().getStack().size() != 0) {
            sb.append(Singletons.getModel().getGame().getStack().size()).append(" to Resolve.");
        } else {
            sb.append("Empty");
        }
        sb.append("\n");
        sb.append("Priority: ").append(player);

        CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonOK() {
        Singletons.getModel().getGame().getPhaseHandler().passPriority();
        //GuiDisplayUtil.updateGUI();
//        final Input in = Singletons.getModel().getMatch().getInput().getInput();
//        if ((in == this) || (in == null)) {
//            Singletons.getModel().getMatch().getInput().resetInput();
//            // Clear out PassPriority after clicking button
//        }
    }

    /** {@inheritDoc} */
    @Override
    public final void selectCard(final Card card, final PlayerZone zone) {
        if (Singletons.getModel().getGame().getAction().playCard(card)) {
            Singletons.getModel().getGame().getPhaseHandler().setPriority(Singletons.getControl().getPlayer());
        }
        else {
            SDisplayUtil.remind(VMessage.SINGLETON_INSTANCE);
        }
    } // selectCard()
}
