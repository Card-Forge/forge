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

import forge.AllZone;
import forge.ButtonUtil;
import forge.Card;
import forge.CombatUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.PlayerZone;

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
        if (AllZone.getPhase().getPlayerTurn().isComputer()) {
            this.aiCleanupDiscard();
            return;
        }

        ButtonUtil.disableAll();
        final int n = AllZone.getHumanPlayer().getCardsIn(Zone.Hand).size();

        // MUST showMessage() before stop() or it will overwrite the next
        // Input's message
        final StringBuffer sb = new StringBuffer();
        sb.append("Cleanup Phase: You can only have a maximum of ").append(AllZone.getHumanPlayer().getMaxHandSize());
        sb.append(" cards, you currently have ").append(n).append(" cards in your hand - select a card to discard");
        AllZone.getDisplay().showMessage(sb.toString());

        // goes to the next phase
        if ((n <= AllZone.getHumanPlayer().getMaxHandSize()) || (AllZone.getHumanPlayer().getMaxHandSize() == -1)) {
            CombatUtil.removeAllDamage();

            AllZone.getPhase().setNeedToNextPhase(true);
            AllZone.getPhase().nextPhase(); // TODO keep an eye on this code,
                                            // see if we can get rid of it.
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void selectCard(final Card card, final PlayerZone zone) {
        if (zone.is(Constant.Zone.Hand, AllZone.getHumanPlayer())) {
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
        final int size = AllZone.getComputerPlayer().getCardsIn(Zone.Hand).size();

        if (AllZone.getComputerPlayer().getMaxHandSize() != -1) {
            final int numDiscards = size - AllZone.getComputerPlayer().getMaxHandSize();
            AllZone.getComputerPlayer().discard(numDiscards, null, false);
        }
        CombatUtil.removeAllDamage();

        AllZone.getPhase().setNeedToNextPhase(true);
    }
}
