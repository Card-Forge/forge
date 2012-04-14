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

import forge.AllZone;
import forge.Card;
import forge.Singletons;
import forge.game.phase.CombatUtil;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/**
 * <p>
 * Input_Cleanup class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputCleanup extends Input {
    /** Constant <code>serialVersionUID=-4164275418971547948L</code>. */
    private static final long serialVersionUID = -4164275418971547948L;

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        if (Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn().isComputer()) {
            this.aiCleanupDiscard();
            return;
        }

        final int n = AllZone.getHumanPlayer().getCardsIn(ZoneType.Hand).size();
        final int max = AllZone.getHumanPlayer().getMaxHandSize();
        // goes to the next phase
        if (n <= max || max <= -1) {
            CombatUtil.removeAllDamage();

            Singletons.getModel().getGameState().getPhaseHandler().setNeedToNextPhase(true);
            Singletons.getModel().getGameState().getPhaseHandler().nextPhase(); // TODO keep an eye on this code,
                                            // see if we can get rid of it.
            return;
        }
        ButtonUtil.disableAll();

        // MUST showMessage() before stop() or it will overwrite the next
        // Input's message
        final StringBuffer sb = new StringBuffer();
        sb.append("Cleanup Phase: You can only have a maximum of ").append(max);
        sb.append(" cards, you currently have ").append(n).append(" cards in your hand - select a card to discard");
        CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());
    }

    /** {@inheritDoc} */
    @Override
    public final void selectCard(final Card card, final PlayerZone zone) {
        if (zone.is(ZoneType.Hand, AllZone.getHumanPlayer())) {
            card.getController().discard(card, null);
            if (AllZone.getStack().size() == 0) {
                this.showMessage();
            }
        }
    } // selectCard()

    /**
     * <p>
     * AI_CleanupDiscard.
     * </p>
     */
    public void aiCleanupDiscard() {
        final int size = AllZone.getComputerPlayer().getCardsIn(ZoneType.Hand).size();

        if (AllZone.getComputerPlayer().getMaxHandSize() != -1) {
            final int numDiscards = size - AllZone.getComputerPlayer().getMaxHandSize();
            AllZone.getComputerPlayer().discard(numDiscards, null, false);
        }
        CombatUtil.removeAllDamage();

        Singletons.getModel().getGameState().getPhaseHandler().setNeedToNextPhase(true);
    }
}
