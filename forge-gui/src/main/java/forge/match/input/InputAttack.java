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

import com.google.common.collect.Iterables;

import forge.GuiBase;
import forge.events.UiEventAttackerDeclared;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.ITriggerEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * InputAttack class.
 * </p>
 * 
 * @author Forge
 * @version $Id: InputAttack.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class InputAttack extends InputSyncronizedBase {
    /** Constant <code>serialVersionUID=7849903731842214245L</code>. */
    private static final long serialVersionUID = 7849903731842214245L;

    private final Combat combat;
    private final List<GameEntity> defenders;
    private GameEntity currentDefender;
    private final Player playerAttacks;
    private AttackingBand activeBand = null;

    public InputAttack(Player attacks0, Combat combat0) {
        playerAttacks = attacks0;
        combat = combat0;
        defenders = combat.getDefenders();
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        // TODO still seems to have some issues with multiple planeswalkers
        setCurrentDefender(defenders.isEmpty() ? null : defenders.get(0));

        if (null == currentDefender) {
            System.err.println("InputAttack has no potential defenders!");
            updatePrompt();
            return; // should even throw here!
        }

        List<Card> possibleAttackers = playerAttacks.getCardsIn(ZoneType.Battlefield);
        for (Card c : Iterables.filter(possibleAttackers, CardPredicates.Presets.CREATURES)) {
            if (c.hasKeyword("CARDNAME attacks each turn if able.")) {
                for (GameEntity def : defenders) {
                    if (CombatUtil.canAttack(c, def, combat)) {
                        combat.addAttacker(c, def);
                        GuiBase.getInterface().fireEvent(new UiEventAttackerDeclared(c, currentDefender));
                        break;
                    }
                }
            }
            else if (c.hasStartOfKeyword("CARDNAME attacks specific player each combat if able")) {
                final int i = c.getKeywordPosition("CARDNAME attacks specific player each combat if able");
                final String defined = c.getKeyword().get(i).split(":")[1];
                final Player player = AbilityUtils.getDefinedPlayers(c, defined, null).get(0);
                if (player != null && CombatUtil.canAttack(c, player, combat)) {
                    combat.addAttacker(c, player);
                    GuiBase.getInterface().fireEvent(new UiEventAttackerDeclared(c, player));
                }
            }
        }
        updateMessage();
    }

    //determine whether currently attackers can be called back (undeclared)
    private boolean canCallBackAttackers() {
        for (Card c : combat.getAttackers()) {
            if (canUndeclareAttacker(c)) {
                return true;
            }
        }
        return false;
    }

    private void updatePrompt() {
        if (canCallBackAttackers()) {
            ButtonUtil.update("OK", "Call Back", true, true, true);
        }
        else {
            ButtonUtil.update("OK", "Alpha Strike", true, true, true);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected final void onOk() {
        // TODO Add check to see if each must attack creature is attacking
        // Propaganda costs could have been paid here.
        setCurrentDefender(null); // remove highlights
        activateBand(null);
        stop();
    }

    /** {@inheritDoc} */
    @Override
    protected final void onCancel() {
        //either alpha strike or undeclare all attackers based on whether any attackers have been declared
        if (canCallBackAttackers()) {
            //undeclare all attackers
            List<Card> attackers = new ArrayList<Card>(combat.getAttackers()); //must copy list since it will be modified
            for (Card c : attackers) {
                undeclareAttacker(c);
            }
        }
        else {
            //alpha strike
            List<Player> defenders = playerAttacks.getOpponents();
    
            for (Card c : CardLists.filter(playerAttacks.getCardsIn(ZoneType.Battlefield), Presets.CREATURES)) {
                if (combat.isAttacking(c)) {
                    continue;
                }
    
                for (Player defender : defenders) {
                    if (CombatUtil.canAttack(c, defender, combat)) {
                        combat.addAttacker(c, defender);
                        break;
                    }
                }
            }
        }
        updateMessage();
    }

    @Override
    protected final void onPlayerSelected(Player selected, final ITriggerEvent triggerEvent) {
        if (defenders.contains(selected)) {
            setCurrentDefender(selected);
        }
        else {
            flashIncorrectAction(); // cannot attack that player
        }
    }

    /** {@inheritDoc} */
    @Override
    protected final boolean onCardSelected(final Card card, final ITriggerEvent triggerEvent) {
        final List<Card> att = combat.getAttackers();
        if (triggerEvent != null && triggerEvent.getButton() == 3 && att.contains(card)) {
            if (undeclareAttacker(card)) {
                updateMessage();
                return true;
            }
        }

        if (combat.isAttacking(card, currentDefender)) {
            boolean validAction = true;
            if (isBandingPossible()) {
                // Activate band by selecting/deselecting a band member
                if (activeBand == null) {
                    activateBand(combat.getBandOfAttacker(card));
                }
                else if (activeBand.getAttackers().contains(card)) {
                    activateBand(null);
                }
                else { // Join a band by selecting a non-active band member after activating a band
                    if (activeBand.canJoinBand(card)) {
                        combat.removeFromCombat(card);
                        declareAttacker(card);
                    }
                    else {
                        flashIncorrectAction();
                        validAction = false;
                    }
                }
            }
            else {
                //if banding not possible, just undeclare attacker
                undeclareAttacker(card);
            }

            updateMessage();
            return validAction;
        }

        if (card.getController().isOpponentOf(playerAttacks)) {
            if (defenders.contains(card)) { // planeswalker?
                setCurrentDefender(card);
                return true;
            }
        }

        if (playerAttacks.getZone(ZoneType.Battlefield).contains(card) && CombatUtil.canAttack(card, currentDefender, combat)) {
            if (activeBand != null && !activeBand.canJoinBand(card)) {
                activateBand(null);
                updateMessage();
                flashIncorrectAction();
                return false;
            }

            if (combat.isAttacking(card)) {
                combat.removeFromCombat(card);
            }

            declareAttacker(card);
            updateMessage();
            return true;
        }

        flashIncorrectAction();
        return false;
    }

    private void declareAttacker(final Card card) {
        combat.addAttacker(card, currentDefender, activeBand);
        activateBand(activeBand);

        GuiBase.getInterface().fireEvent(new UiEventAttackerDeclared(card, currentDefender));
    }

    private boolean canUndeclareAttacker(Card card) {
        return !card.hasKeyword("CARDNAME attacks each turn if able.") &&
               !card.hasStartOfKeyword("CARDNAME attacks specific player each combat if able");
    }

    private boolean undeclareAttacker(Card card) {
        if (canUndeclareAttacker(card)) {
            // TODO Is there no way to attacks each turn cards to attack Planeswalkers?
            combat.removeFromCombat(card);
            GuiBase.getInterface().setUsedToPay(card, false);
            // When removing an attacker clear the attacking band
            activateBand(null);

            GuiBase.getInterface().fireEvent(new UiEventAttackerDeclared(card, null));
            return true;
        }
        return false;
    }

    private final void setCurrentDefender(GameEntity def) {
        currentDefender = def;
        for (GameEntity ge : defenders) {
            if (ge instanceof Card) {
                GuiBase.getInterface().setUsedToPay((Card)ge, ge == def);
            }
            else if (ge instanceof Player) {
                GuiBase.getInterface().setHighlighted((Player) ge, ge == def);
            }
        }

        updateMessage();
    }

    private final void activateBand(AttackingBand band) {
        if (activeBand != null) {
            for (Card card : activeBand.getAttackers()) {
                GuiBase.getInterface().setUsedToPay(card, false);
            }
        }
        activeBand = band;

        if (activeBand != null) {
            for(Card card : activeBand.getAttackers()) {
                GuiBase.getInterface().setUsedToPay(card, true);
            }
        }
    }

    //only enable banding message and actions if a creature that can attack has banding
    private boolean isBandingPossible() {
        List<Card> possibleAttackers = playerAttacks.getCardsIn(ZoneType.Battlefield);
        for (Card c : Iterables.filter(possibleAttackers, CardPredicates.hasKeyword("Banding"))) {
            if (c.isCreature() && CombatUtil.canAttack(c, currentDefender, combat)) {
                return true;
            }
        }
        return false;
    }

    private void updateMessage() {
        String message = "Select creatures to attack " + currentDefender + " or select player/planeswalker you wish to attack.";
        if (isBandingPossible()) {
            message += " To attack as a band, select an attacking creature to activate its 'band' then select another to join it.";
        }
        showMessage(message);

        updatePrompt();
        GuiBase.getInterface().showCombat(combat); // redraw sword icons
    }
}
