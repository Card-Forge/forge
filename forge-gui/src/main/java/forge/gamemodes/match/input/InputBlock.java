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
package forge.gamemodes.match.input;

import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventCombatUpdate;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.FThreads;
import forge.gui.events.UiEventBlockerAssigned;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import forge.util.ThreadUtil;

import java.util.List;

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
            for (final Card c : defender.getCreaturesInPlay()) {
                if (CombatUtil.canBlock(attacker, c, combat)) {
                    //must set current attacker on EDT
                    FThreads.invokeInEdtNowOrLater(() -> setCurrentAttacker(attacker));
                    return;
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected final void showMessage() {
        // could add "Reset Blockers" button
        Localizer localizer = Localizer.getInstance();
        getController().getGui().updateButtons(getOwner(), true, false, true);

        if (currentAttacker == null) {
            showMessage(localizer.getMessage("lblSelectBlockTarget"));
        } else {
            String attackerName = currentAttacker.isFaceDown() ? localizer.getMessage("lblMorph") : currentAttacker.getDisplayName() + " (" + currentAttacker.getId() + ")";
            String message = localizer.getMessage("lblSelectBlocker") + attackerName + " " + localizer.getMessage("lblOrSelectBlockTarget");
            showMessage(message);
        }

        if (combat != null)
            getController().getGame().fireEvent(new GameEventCombatUpdate(combat.getAttackers(), combat.getAllBlockers()));

        getController().getGui().showCombat();
    }

    /** {@inheritDoc} */
    @Override
    public final void onOk() {
        final String blockErrors = CombatUtil.validateBlocks(combat, defender);
        if (blockErrors == null) {
            // Done blocking
            setCurrentAttacker(null);
            stop();
        } else {
            //must run in game thread to prevent problems for mobile game
            ThreadUtil.invokeInGameThread(() -> getController().getGui().message(blockErrors));
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean onCardSelected(final Card card, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        boolean isCorrectAction = false;
        if (triggerEvent != null && triggerEvent.getButton() == 3 && card.getController() == defender) {
            combat.removeFromCombat(card);
            card.getGame().getMatch().fireEvent(new UiEventBlockerAssigned(CardView.get(card), null));
            if (otherCardsToSelect != null) {
                for (Card c : otherCardsToSelect) {
                    if (c.getController() == defender) {
                        combat.removeFromCombat(c);
                        c.getGame().getMatch().fireEvent(new UiEventBlockerAssigned(CardView.get(c), null));
                    }
                }
            }
            isCorrectAction = true;
        } else {
            // is attacking?
            if (combat.isAttacking(card)) {
                setCurrentAttacker(card);
                isCorrectAction = true;
            } else {
                // Make sure this card is valid to even be a blocker
                if (currentAttacker != null && card.isCreature() && defender.getZone(ZoneType.Battlefield).contains(card)) {
                    if (combat.isBlocking(card, currentAttacker)) {
                        //if creature already blocking current attacker, remove blocker from combat
                        combat.removeBlockAssignment(currentAttacker, card);
                        card.getGame().getMatch().fireEvent(new UiEventBlockerAssigned(CardView.get(card), null));
                        if (otherCardsToSelect != null) {
                            for (Card c : otherCardsToSelect) {
                                if (combat.isBlocking(c, currentAttacker)) {
                                    combat.removeBlockAssignment(currentAttacker, c);
                                    c.getGame().getMatch().fireEvent(new UiEventBlockerAssigned(CardView.get(c), null));
                                }
                            }
                        }
                        isCorrectAction = true;
                    } else {
                        isCorrectAction = CombatUtil.canBlock(currentAttacker, card, combat);
                        if (isCorrectAction) {
                            combat.addBlocker(currentAttacker, card);
                            card.getGame().getMatch().fireEvent(new UiEventBlockerAssigned(
                                    CardView.get(card),
                                    CardView.get(currentAttacker)));
                            if (otherCardsToSelect != null) {
                                for (Card c : otherCardsToSelect) {
                                    if (CombatUtil.canBlock(currentAttacker, c, combat)) {
                                        combat.addBlocker(currentAttacker, c);
                                        c.getGame().getMatch().fireEvent(new UiEventBlockerAssigned(
                                                CardView.get(c),
                                                CardView.get(currentAttacker)));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isCorrectAction) {
            card.getGame().fireEvent(new GameEventCombatChanged());
        }

        showMessage();
        return isCorrectAction;
    }

    @Override
    public String getActivateAction(Card card) {
        if (combat.isAttacking(card)) {
            return Localizer.getInstance().getMessage("lblDeclareBlockersForCard");
        }
        if (currentAttacker != null && card.isCreature() && defender.getZone(ZoneType.Battlefield).contains(card)) {
            if (combat.isBlocking(card, currentAttacker)) {
                return Localizer.getInstance().getMessage("lblRemoveFromCombat");
            }
            if (CombatUtil.canBlock(currentAttacker, card, combat)) {
                return Localizer.getInstance().getMessage("lblBlockWithCard");
            }
        }
        return null;
    }

    private void setCurrentAttacker(final Card card) {
        currentAttacker = card;
        for (final Card c : combat.getAttackers()) {
            getController().getGui().setUsedToPay(CardView.get(c), card == c);
        }
    }
}
