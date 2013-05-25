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
import forge.game.GameState;
import forge.game.phase.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.view.ButtonUtil;

/**
 * <p>
 * Input_Block class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputBlock extends InputPassPriorityBase {
    /** Constant <code>serialVersionUID=6120743598368928128L</code>. */
    private static final long serialVersionUID = 6120743598368928128L;

    private Card currentAttacker = null;
    private final HashMap<Card, List<Card>> allBlocking = new HashMap<Card, List<Card>>();
    private final GameState game; 
    
    /**
     * TODO: Write javadoc for Constructor.
     * @param priority
     */
    public InputBlock(Player human, GameState game) {
        super(human);
        this.game = game;
    }

    private  final void removeFromAllBlocking(final Card c) {
        this.allBlocking.remove(c);
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        // could add "Reset Blockers" button
        ButtonUtil.enableOnlyOk();

        if (this.currentAttacker == null) {

            final StringBuilder sb = new StringBuilder();
            sb.append("To Block, click on your Opponents attacker first, then your blocker(s). ");
            sb.append("To cancel a block right-click on your blocker");
            showMessage(sb.toString());
        } else {
            final String attackerName = this.currentAttacker.isFaceDown() ? "Morph" : this.currentAttacker.getName();
            final StringBuilder sb = new StringBuilder();
            sb.append("Select a creature to block ").append(attackerName).append(" (");
            sb.append(this.currentAttacker.getUniqueNumber()).append("). ");
            sb.append("To cancel a block right-click on your blocker");
            showMessage(sb.toString());
        }

        CombatUtil.showCombat(game);
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonOK() {
        if (CombatUtil.finishedMandatoryBlocks(game.getCombat(), player)) {
            // Done blocking
            ButtonUtil.reset();
            CombatUtil.orderMultipleCombatants(game);
            currentAttacker = null;
            allBlocking.clear();

            pass();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void selectCard(final Card card, boolean isMetaDown) {


        if (isMetaDown) {
            if (card.getController() == player ) {
                game.getCombat().removeFromCombat(card);
            }
            removeFromAllBlocking(card);
            CombatUtil.showCombat(game);
            return;
        }
        
        // is attacking?
        boolean reminder = true;

        if (game.getCombat().getAttackers().contains(card)) {
            this.currentAttacker = card;
            reminder = false;
        } else {
            Zone zone = game.getZoneOf(card);
            // Make sure this card is valid to even be a blocker
            if (this.currentAttacker != null && card.isCreature() && card.getController().equals(player)
                    && zone.is(ZoneType.Battlefield, player)) {
                // Create a new blockedBy list if it doesn't exist
                if (!this.allBlocking.containsKey(card)) {
                    this.allBlocking.put(card, new ArrayList<Card>());
                }

                List<Card> attackersBlocked = this.allBlocking.get(card);
                if (!attackersBlocked.contains(this.currentAttacker)
                        && CombatUtil.canBlock(this.currentAttacker, card, game.getCombat())) {
                    attackersBlocked.add(this.currentAttacker);
                    game.getCombat().addBlocker(this.currentAttacker, card);
                    reminder = false;
                }
            }
        }

        if (reminder) {
            flashIncorrectAction();
        }

        this.showMessage();
    } // selectCard()
}
