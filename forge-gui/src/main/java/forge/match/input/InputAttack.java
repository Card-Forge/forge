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
import forge.game.card.CardPredicates;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.ITriggerEvent;

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
    private final Player playerDeclares;
    private AttackingBand activeBand = null;

    public InputAttack(Player attacks, Player declares, Combat combat) {
        this.playerAttacks = attacks;
        this.playerDeclares = declares;
        this.combat = combat;
        this.defenders = combat.getDefenders();
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        // TODO still seems to have some issues with multiple planeswalkers

        ButtonUtil.enableOnlyOk();

        setCurrentDefender(defenders.isEmpty() ? null : defenders.get(0));

        if (null == currentDefender) {
            System.err.println("InputAttack has no potential defenders!");
            return; // should even throw here!
        }

        List<Card> possibleAttackers = playerAttacks.getCardsIn(ZoneType.Battlefield);
        for (Card c : Iterables.filter(possibleAttackers, CardPredicates.Presets.CREATURES)) {
            if (c.hasKeyword("CARDNAME attacks each turn if able.")) {
                for(GameEntity def : defenders) {
                    if(CombatUtil.canAttack(c, def, combat)) {
                        combat.addAttacker(c, currentDefender);
                        GuiBase.getInterface().fireEvent(new UiEventAttackerDeclared(c, currentDefender));
                        break;
                    }
                }
            } else if (c.hasStartOfKeyword("CARDNAME attacks specific player each combat if able")) {
                final int i = c.getKeywordPosition("CARDNAME attacks specific player each combat if able");
                final String defined = c.getKeyword().get(i).split(":")[1];
                final Player player = AbilityUtils.getDefinedPlayers(c, defined, null).get(0);
                if (player != null && CombatUtil.canAttack(c, player, combat)) {
                    combat.addAttacker(c, player);
                    GuiBase.getInterface().fireEvent(new UiEventAttackerDeclared(c, player));
                }
            }
        }
    }

    private void showCombat() {
        // redraw sword icons
        GuiBase.getInterface().showCombat(combat);
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

    @Override
    protected final void onPlayerSelected(Player selected, final ITriggerEvent triggerEvent) {
        if(defenders.contains(selected)) {
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
        if (triggerEvent != null && triggerEvent.getButton() == 3 && att.contains(card) && !card.hasKeyword("CARDNAME attacks each turn if able.")
                && !card.hasStartOfKeyword("CARDNAME attacks specific player each combat if able")) {
            // TODO Is there no way to attacks each turn cards to attack Planeswalkers?
            combat.removeFromCombat(card);
            GuiBase.getInterface().setUsedToPay(card, false);
            showCombat();
            // When removing an attacker clear the attacking band
            this.activateBand(null);

            GuiBase.getInterface().fireEvent(new UiEventAttackerDeclared(card, null));
            return true;
        }

        if (combat.isAttacking(card, currentDefender)) {
            // Activate band by selecting/deselecting a band member
            boolean validAction = true;
            if (this.activeBand == null) {
                this.activateBand(combat.getBandOfAttacker(card));
            }
            else if (this.activeBand.getAttackers().contains(card)) {
                this.activateBand(null);
            }
            else { // Join a band by selecting a non-active band member after activating a band
                if (this.activeBand.canJoinBand(card)) {
                    combat.removeFromCombat(card);
                    declareAttacker(card);
                }
                else {
                    flashIncorrectAction();
                    validAction = false;
                }
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
            if (this.activeBand != null && !this.activeBand.canJoinBand(card)) {
                this.activateBand(null);
                updateMessage();
                flashIncorrectAction();
                return false;
            }

            if(combat.isAttacking(card)) {
                combat.removeFromCombat(card);
            }

            declareAttacker(card);
            showCombat();
            return true;
        }

        flashIncorrectAction();
        return false;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param card
     */
    private void declareAttacker(final Card card) {
        combat.addAttacker(card, currentDefender, this.activeBand);
        this.activateBand(this.activeBand);
        updateMessage();

        GuiBase.getInterface().fireEvent(new UiEventAttackerDeclared(card, currentDefender));
    }

    private final void setCurrentDefender(GameEntity def) {
        currentDefender = def;
        for(GameEntity ge: defenders) {
            if (ge instanceof Card) {
                GuiBase.getInterface().setUsedToPay((Card)ge, ge == def);
            }
            else if (ge instanceof Player) {
                GuiBase.getInterface().setHighlighted((Player) ge, ge == def);
            }
        }

        updateMessage();

        // update UI
    }

    private final void activateBand(AttackingBand band) {
        if (this.activeBand != null) {
            for(Card card : this.activeBand.getAttackers()) {
                GuiBase.getInterface().setUsedToPay(card, false);
            }
        }
        this.activeBand = band;

        if (this.activeBand != null) {
            for(Card card : this.activeBand.getAttackers()) {
                GuiBase.getInterface().setUsedToPay(card, true);
            }
        }

        // update UI
    }

    private void updateMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(playerDeclares.getName()).append(", ");
        sb.append(playerAttacks == playerDeclares ? "declare attackers." : "declare attackers for " + playerAttacks.getName()).append("\n");
        sb.append("Selecting Creatures to Attack ").append(currentDefender).append("\n\n");
        sb.append("To change the current defender, click on the player or planeswalker you wish to attack.\n");
        sb.append("To attack as a band, click an attacking creature to activate its 'band', select another to join the band.");

        showMessage(sb.toString());
    }
}
