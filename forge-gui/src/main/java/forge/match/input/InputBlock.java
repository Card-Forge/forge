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
package forge.match.input;

import forge.GuiBase;
import forge.events.UiEventBlockerAssigned;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.ITriggerEvent;
import forge.util.gui.SGuiDialog;

/**
 * <p>
 * Input_Block class.
 * </p>
 * 
 * @author Forge
 * @version $Id: InputBlock.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class InputBlock extends InputSyncronizedBase {
    /** Constant <code>serialVersionUID=6120743598368928128L</code>. */
    private static final long serialVersionUID = 6120743598368928128L;

    private Card currentAttacker = null;
    // some cards may block several creatures at a time. (ex:  Two-Headed Dragon, Vanguard's Shield)
    private final Combat combat;
    private final Player defender;
    private final Player declarer;

    /**
     * TODO: Write javadoc for Constructor.
     * @param priority
     */
    public InputBlock(Player whoDeclares, Player whoDefends, Combat combat) {
        defender = whoDefends;
        declarer = whoDeclares;
        this.combat = combat;
    }

    /** {@inheritDoc} */
    @Override
    protected final void showMessage() {
        // could add "Reset Blockers" button
        ButtonUtil.enableOnlyOk();

        String prompt = declarer == defender ? "declare blockers." : "declare blockers for " + defender.getName();

        final StringBuilder sb = new StringBuilder(declarer.getName());
        sb.append(", ").append(prompt).append("\n\n");

        if (this.currentAttacker == null) {
            sb.append("To Block, click on your opponent's attacker first, then your blocker(s).\n");
            sb.append("To cancel a block right-click on your blocker");
        }
        else {
            final String attackerName = this.currentAttacker.isFaceDown() ? "Morph" : this.currentAttacker.getName();
            sb.append("Select a creature to block ").append(attackerName).append(" (");
            sb.append(this.currentAttacker.getUniqueNumber()).append("). ");
            sb.append("To cancel a block right-click on your blocker");
        }

        showMessage(sb.toString());
        GuiBase.getInterface().showCombat(combat);
    }

    /** {@inheritDoc} */
    @Override
    public final void onOk() {
        String blockErrors = CombatUtil.validateBlocks(combat, defender);
        if( null == blockErrors ) {
            // Done blocking
            ButtonUtil.reset();
            setCurrentAttacker(null);
            stop();
        } else {
            SGuiDialog.message(blockErrors);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean onCardSelected(final Card card, final ITriggerEvent triggerEvent) {
        boolean isCorrectAction = false;
        if (triggerEvent != null && triggerEvent.getButton() == 3 && card.getController() == defender) {
            combat.removeFromCombat(card);
            GuiBase.getInterface().fireEvent(new UiEventBlockerAssigned(card, (Card)null));
            isCorrectAction = true;
        }
        else {
            // is attacking?
            if (combat.isAttacking(card)) {
                setCurrentAttacker(card);
                isCorrectAction = true;
            }
            else {
                // Make sure this card is valid to even be a blocker
                if (this.currentAttacker != null && card.isCreature() && defender.getZone(ZoneType.Battlefield).contains(card)) {
                    isCorrectAction = CombatUtil.canBlock(this.currentAttacker, card, combat);
                    if (isCorrectAction) {
                        combat.addBlocker(this.currentAttacker, card);
                        GuiBase.getInterface().fireEvent(new UiEventBlockerAssigned(card, currentAttacker));
                    }
                }
            }

            if (!isCorrectAction) {
                flashIncorrectAction();
            }
        }
        this.showMessage();
        return isCorrectAction;
    }

    private void setCurrentAttacker(Card card) {
        currentAttacker = card;
        for(Card c : combat.getAttackers()) {
            GuiBase.getInterface().setUsedToPay(c, card == c);
        }
    }
}
