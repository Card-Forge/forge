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
import forge.FThreads;
import forge.game.GameState;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.view.ButtonUtil;

/**
 * <p>
 * Input_Cleanup class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputCleanup extends InputBase {
    /** Constant <code>serialVersionUID=-4164275418971547948L</code>. */
    private static final long serialVersionUID = -4164275418971547948L;
    private final GameState game;
    final Player activePlayer;
    final Player turnOwner;
    
    /**
     * TODO: Write javadoc for Constructor.
     * @param game
     */
    public InputCleanup(GameState game) {
        super();
        this.game = game;
        activePlayer = game.getPhaseHandler().getPriorityPlayer();
        turnOwner = game.getPhaseHandler().getPlayerTurn();
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {


        final int n = activePlayer.getCardsIn(ZoneType.Hand).size();
        final int max = activePlayer.getMaxHandSize();
        // goes to the next phase
        if (activePlayer.isUnlimitedHandSize() || n <= max || n <= 0 || activePlayer != turnOwner) {
            activePlayer.getController().passPriority();
            return;
        }
        ButtonUtil.disableAll();

        // MUST showMessage() before stop() or it will overwrite the next
        // Input's message
        final StringBuffer sb = new StringBuffer();
        sb.append("Cleanup Phase: You can only have a maximum of ").append(max);
        sb.append(" cards, you currently have ").append(n).append(" cards in your hand - select a card to discard");
        showMessage(sb.toString());
    }

    /** {@inheritDoc} */
    @Override
    public final void selectCard(final Card card) {
        Zone zone = game.getZoneOf(card);
        if (!zone.is(ZoneType.Hand, turnOwner))
            return;
            
        FThreads.invokeInNewThread(new Runnable() {
            @Override
            public void run() {
                turnOwner.discard(card, null);
            }
        }, true);
    }
}
