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

import java.util.HashMap;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.Singletons;
import forge.game.phase.CombatUtil;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.CMatchUI;
import forge.gui.match.views.VMessage;
import forge.view.ButtonUtil;

/**
 * <p>
 * Input_Block class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputBlock extends Input {
    /** Constant <code>serialVersionUID=6120743598368928128L</code>. */
    private static final long serialVersionUID = 6120743598368928128L;

    private Card currentAttacker = null;
    private final HashMap<Card,CardList> allBlocking = new HashMap<Card, CardList>();

    /**
     * <p>
     * removeFromAllBlocking.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void removeFromAllBlocking(final Card c) {
        this.allBlocking.remove(c);
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        // could add "Reset Blockers" button
        ButtonUtil.enableOnlyOK();

        if (this.currentAttacker == null) {

            final StringBuilder sb = new StringBuilder();
            sb.append("To Block, click on your Opponents attacker first, then your blocker(s). ");
            sb.append("To cancel a block right-click on your blocker");
            CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());
        } else {
            final String attackerName = this.currentAttacker.isFaceDown() ? "Morph" : this.currentAttacker.getName();
            final StringBuilder sb = new StringBuilder();
            sb.append("Select a creature to block ").append(attackerName).append(" (");
            sb.append(this.currentAttacker.getUniqueNumber()).append("). ");
            sb.append("To cancel a block right-click on your blocker");
            CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());
        }

        CombatUtil.showCombat();
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonOK() {
        if (CombatUtil.finishedMandatoryBlocks(AllZone.getCombat())) {
            // Done blocking
            ButtonUtil.reset();
            
            CombatUtil.orderMultipleCombatants(AllZone.getCombat());

            Singletons.getModel().getGameState().getPhaseHandler().setNeedToNextPhase(true);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void selectCard(final Card card, final PlayerZone zone) {
        // is attacking?
        boolean reminder = true;
        
        if (AllZone.getCombat().getAttackers().contains(card)) {
            this.currentAttacker = card;
            reminder = false;
        } else {
            // Make sure this card is valid to even be a blocker
            if (this.currentAttacker != null && card.isCreature() && card.getController().isHuman() 
                    && zone.is(ZoneType.Battlefield, AllZone.getHumanPlayer())) {
                // Create a new blockedBy list if it doesn't exist
                if (!this.allBlocking.containsKey(card)) {
                    this.allBlocking.put(card, new CardList());
                }
                
                CardList attackersBlocked = this.allBlocking.get(card);
                if (!attackersBlocked.contains(this.currentAttacker) && 
                        CombatUtil.canBlock(this.currentAttacker, card, AllZone.getCombat())) {
                    attackersBlocked.add(this.currentAttacker);
                    AllZone.getCombat().addBlocker(this.currentAttacker, card);
                    reminder = false;
                }
            } 
        }
        
        if (reminder) {
            SDisplayUtil.remind(VMessage.SINGLETON_INSTANCE);
        }
        
        this.showMessage();
    } // selectCard()
}
