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

import java.util.List;

import forge.Card;

import forge.CardLists;
import forge.Command;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.CostReturn;
import forge.card.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

//if cost is paid, Command.execute() is called

/**
 * <p>
 * Input_PayManaCost_Ability class.
 * </p>
 * 
 * @author Forge
 * @version $Id: InputPayManaCostAbility.java 15673 2012-05-23 14:01:35Z ArsenalNut $
 */
public class InputPayReturnCost extends Input {
    /**
     * Constant <code>serialVersionUID=2685832214529141991L</code>.
     */
    private static final long serialVersionUID = 8701146064257627671L;

    private int numChosen = 0;
    private int numRequired = 0;
    private List<Card> choiceList;
    private CostReturn returnCost;
    private SpellAbility ability;
    private Command paid;
    private Command unpaid;

    /**
     * <p>
     * Constructor for InputPayReturnCost.
     * </p>
     * 
     * @param cost
     *            a {@link forge.card.cost.CostSacrifice} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param paidCommand
     *            a {@link forge.Command} object.
     * @param unpaidCommand
     *            a {@link forge.Command} object.
     */
    public InputPayReturnCost(final CostReturn cost, final SpellAbility sa, final Command paidCommand,
            final Command unpaidCommand) {
        final Card source = sa.getSourceCard();

        this.ability = sa;
        this.returnCost = cost;
        this.choiceList = CardLists.getValidCards(Singletons.getControl().getPlayer().getCardsIn(ZoneType.Battlefield), cost.getType().split(";"), Singletons.getControl().getPlayer(), source);
        String amountString = cost.getAmount();
        this.numRequired = amountString.matches("[0-9][0-9]?") ? Integer.parseInt(amountString)
                : CardFactoryUtil.xCount(source, source.getSVar(amountString));
        this.paid = paidCommand;
        this.unpaid = unpaidCommand;
    }

    /** {@inheritDoc} */
    @Override
    public void showMessage() {
        final StringBuilder msg = new StringBuilder("Return ");
        final int nLeft = this.numRequired - this.numChosen;
        msg.append(nLeft).append(" ");
        msg.append(this.returnCost.getDescriptiveType());
        if (nLeft > 1) {
            msg.append("s");
        }
        msg.append(" to your hand");
        if (!this.returnCost.getList().isEmpty()) {
            msg.append("\r\nSelected:\r\n");
            for (Card selected : this.returnCost.getList()) {
                msg.append(selected + "\r\n");
            }
        }

        CMatchUI.SINGLETON_INSTANCE.showMessage(msg.toString());
        if (nLeft > 0) {
            ButtonUtil.enableOnlyCancel();
        }
        else {
            ButtonUtil.enableAll();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void selectButtonCancel() {
        this.cancel();
    }

    /** {@inheritDoc} */
    @Override
    public void selectButtonOK() {
        this.done();
    }

    /** {@inheritDoc} */
    @Override
    public void selectCard(final Card card) {
        if (this.choiceList.contains(card) && this.numChosen < numRequired) {
            this.numChosen++;
            this.returnCost.addToList(card);
            card.setUsedToPay(true);
            this.choiceList.remove(card);
            this.showMessage();
        }
    }

    /**
     * <p>
     * unselectCard.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param zone
     *            a {@link forge.game.zone.PlayerZone} object.
     */
    public void unselectCard(final Card card, final PlayerZone zone) {
        if (this.returnCost.getList().contains(card)) {
            this.numChosen--;
            card.setUsedToPay(false);
            this.returnCost.getList().remove(card);
            this.choiceList.add(card);
            this.showMessage();
        }
    }

    /**
     * <p>
     * executes paid commmand.
     * </p>
     * 
     */
    public void done() {
        this.stop();
        // actually sacrifice the cards
        for (Card selected : this.returnCost.getList()) {
            selected.setUsedToPay(false);
            Singletons.getModel().getGame().getAction().moveTo(ZoneType.Hand, selected);
        }
        this.returnCost.addListToHash(ability, "Returned");
        this.paid.execute();
    }

    /**
     * <p>
     * executes unpaid commmand.
     * </p>
     * 
     */
    public void cancel() {
        this.stop();
        for (Card selected : this.returnCost.getList()) {
            selected.setUsedToPay(false);
        }
        this.unpaid.execute();
    }

    @Override public void isClassUpdated() {
    }

}
