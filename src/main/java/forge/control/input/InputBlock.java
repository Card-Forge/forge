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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import forge.Card;

import forge.Singletons;
import forge.game.phase.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.Zone;
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
    private final HashMap<Card, List<Card>> allBlocking = new HashMap<Card, List<Card>>();
    private final Player defender;

    /**
     * TODO: Write javadoc for Constructor.
     * @param priority
     */
    public InputBlock(Player priority) {
        defender = priority;
    }

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
        if (CombatUtil.finishedMandatoryBlocks(Singletons.getModel().getGame().getCombat(), defender)) {
            // Done blocking
            ButtonUtil.reset();
            CombatUtil.orderMultipleCombatants(Singletons.getModel().getGame().getCombat());

            Singletons.getModel().getGame().getPhaseHandler().passPriority();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void selectCard(final Card card) {
        // is attacking?
        boolean reminder = true;

        if (Singletons.getModel().getGame().getCombat().getAttackers().contains(card)) {
            this.currentAttacker = card;
            reminder = false;
        } else {
            Zone zone = Singletons.getModel().getGame().getZoneOf(card);
            // Make sure this card is valid to even be a blocker
            if (this.currentAttacker != null && card.isCreature() && card.getController().equals(defender)
                    && zone.is(ZoneType.Battlefield, defender)) {
                // Create a new blockedBy list if it doesn't exist
                if (!this.allBlocking.containsKey(card)) {
                    this.allBlocking.put(card, new ArrayList<Card>());
                }

                List<Card> attackersBlocked = this.allBlocking.get(card);
                if (!attackersBlocked.contains(this.currentAttacker)
                        && CombatUtil.canBlock(this.currentAttacker, card, Singletons.getModel().getGame().getCombat())) {
                    attackersBlocked.add(this.currentAttacker);
                    Singletons.getModel().getGame().getCombat().addBlocker(this.currentAttacker, card);
                    reminder = false;
                }
            }
        }

        if (reminder) {
            SDisplayUtil.remind(VMessage.SINGLETON_INSTANCE);
        }

        this.showMessage();
    } // selectCard()

    /* (non-Javadoc)
     * @see forge.control.input.Input#isClassUpdated()
     */
    @Override
    public void isClassUpdated() {
    }
}
