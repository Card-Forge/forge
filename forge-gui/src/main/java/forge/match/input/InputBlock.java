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

import forge.FThreads;
import forge.events.UiEventBlockerAssigned;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.gui.SGuiDialog;
import forge.view.CardView;

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

    public InputBlock(final PlayerControllerHuman controller, final Player defender0, final Combat combat0) {
        super(controller);
        defender = defender0;
        combat = combat0;

        //auto-select first attacker to declare blockers for
        for (final Card attacker : combat.getAttackers()) {
            for (final Card c : CardLists.filter(defender.getCardsIn(ZoneType.Battlefield), Presets.CREATURES)) {
                if (CombatUtil.canBlock(attacker, c, combat)) {
                    FThreads.invokeInEdtNowOrLater(getGui(), new Runnable() { //must set current attacker on EDT
                        @Override
                        public void run() {
                            setCurrentAttacker(attacker);
                        }
                    });
                    return;
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected final void showMessage() {
        // could add "Reset Blockers" button
        ButtonUtil.update(getGui(), true, false, true);

        if (currentAttacker == null) {
            showMessage("Select another attacker to declare blockers for.");
        }
        else {
            String attackerName = currentAttacker.isFaceDown() ? "Morph" : currentAttacker.getName() + " (" + currentAttacker.getUniqueNumber() + ")";
            String message = "Select creatures to block " + attackerName + " or select another attacker to declare blockers for.";
            showMessage(message);
        }

        getGui().showCombat(getController().getCombat(combat));
    }

    /** {@inheritDoc} */
    @Override
    public final void onOk() {
        String blockErrors = CombatUtil.validateBlocks(combat, defender);
        if (blockErrors == null) {
            // Done blocking
            setCurrentAttacker(null);
            stop();
        }
        else {
            SGuiDialog.message(getGui(), blockErrors);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean onCardSelected(final Card card, final ITriggerEvent triggerEvent) {
        boolean isCorrectAction = false;
        if (triggerEvent != null && triggerEvent.getButton() == 3 && card.getController() == defender) {
            combat.removeFromCombat(card);
            getGui().fireEvent(new UiEventBlockerAssigned(
                    getController().getCardView(card), (CardView) null));
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
                if (currentAttacker != null && card.isCreature() && defender.getZone(ZoneType.Battlefield).contains(card)) {
                    if (combat.isBlocking(card, currentAttacker)) {
                        //if creature already blocking current attacker, remove blocker from combat
                        combat.removeBlockAssignment(currentAttacker, card);
                        getGui().fireEvent(new UiEventBlockerAssigned(
                                getController().getCardView(card), (CardView) null));
                        isCorrectAction = true;
                    }
                    else {
                        isCorrectAction = CombatUtil.canBlock(currentAttacker, card, combat);
                        if (isCorrectAction) {
                            combat.addBlocker(currentAttacker, card);
                            getGui().fireEvent(new UiEventBlockerAssigned(
                                    getController().getCardView(card),
                                    getController().getCardView(currentAttacker)));
                        }
                    }
                }
            }

            if (!isCorrectAction) {
                flashIncorrectAction();
            }
        }
        showMessage();
        return isCorrectAction;
    }

    private void setCurrentAttacker(final Card card) {
        currentAttacker = card;
        for (final Card c : combat.getAttackers()) {
            getGui().setUsedToPay(getController().getCardView(c), card == c);
        }
    }
}
