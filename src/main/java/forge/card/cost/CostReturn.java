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
package forge.card.cost;

import java.util.List;

import javax.swing.JOptionPane;

import forge.AllZoneUtil;
import forge.Card;

import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/**
 * The Class CostReturn.
 */
public class CostReturn extends CostPartWithList {
    // Return<Num/Type{/TypeDescription}>

    /**
     * Instantiates a new cost return.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostReturn(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Return ");

        final Integer i = this.convertAmount();
        String pronoun = "its";

        if (this.getThis()) {
            sb.append(this.getType());
        } else {
            final String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();
            if (i != null) {
                sb.append(Cost.convertIntAndTypeToWords(i, desc));
                if (i > 1) {
                    pronoun = "their";
                }
            } else {
                sb.append(Cost.convertAmountTypeToWords(this.getAmount(), desc));
            }

            sb.append(" you control");
        }
        sb.append(" to ").append(pronoun).append(" owner's hand");
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#refund(forge.Card)
     */
    @Override
    public void refund(final Card source) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability, final Card source, final Player activator, final Cost cost) {
        if (!this.getThis()) {
            List<Card> typeList = activator.getCardsIn(ZoneType.Battlefield);
            typeList = CardLists.getValidCards(typeList, this.getType().split(";"), activator, source);

            final Integer amount = this.convertAmount();
            if ((amount != null) && (typeList.size() < amount)) {
                return false;
            }
        } else if (!AllZoneUtil.isCardInPlay(source)) {
            return false;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#payAI(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final void payAI(final Player ai, final SpellAbility ability, final Card source, final CostPayment payment) {
        for (final Card c : this.getList()) {
            Singletons.getModel().getGameAction().moveToHand(c);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payHuman(final SpellAbility ability, final Card source, final CostPayment payment) {
        final String amount = this.getAmount();
        Integer c = this.convertAmount();
        final Player activator = ability.getActivatingPlayer();
        final List<Card> list = activator.getCardsIn(ZoneType.Battlefield);
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, ability, list.size());
            } else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }
        if (this.getThis()) {
            CostUtil.setInput(CostReturn.returnThis(ability, payment, this));
        } else {
            CostUtil.setInput(CostReturn.returnType(ability, this.getType(), payment, this, c));
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#decideAIPayment(forge.card.spellability.SpellAbility
     * , forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean decideAIPayment(final Player ai, final SpellAbility ability, final Card source, final CostPayment payment) {
        this.resetList();
        if (this.getThis()) {
            this.getList().add(source);
        } else {
            Integer c = this.convertAmount();
            if (c == null) {
                c = AbilityFactory.calculateAmount(source, this.getAmount(), ability);
            }

            this.setList(ComputerUtil.chooseReturnType(ai, this.getType(), source, ability.getTargetCard(), c));
            if (this.getList() == null) {
                return false;
            }
        }
        return true;
    }

    // Inputs

    /**
     * <p>
     * returnType.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @param payment
     *            a {@link forge.card.cost.CostPayment} object.
     * @param part
     *            TODO
     * @param nNeeded
     *            the n needed
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input returnType(final SpellAbility sa, final String type, final CostPayment payment,
            final CostReturn part, final int nNeeded) {
        final Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            private List<Card> typeList;
            private int nReturns = 0;

            @Override
            public void showMessage() {
                if (nNeeded == 0) {
                    this.done();
                }

                final StringBuilder msg = new StringBuilder("Return ");
                final int nLeft = nNeeded - this.nReturns;
                msg.append(nLeft).append(" ");
                msg.append(type);
                if (nLeft > 1) {
                    msg.append("s");
                }

                this.typeList = sa.getActivatingPlayer().getCardsIn(ZoneType.Battlefield);
                this.typeList = CardLists.getValidCards(this.typeList, type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());
                CMatchUI.SINGLETON_INSTANCE.showMessage(msg.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.cancel();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (this.typeList.contains(card)) {
                    this.nReturns++;
                    part.addToList(card);
                    Singletons.getModel().getGameAction().moveToHand(card);
                    this.typeList.remove(card);
                    // in case nothing else to return
                    if (this.nReturns == nNeeded) {
                        this.done();
                    } else if (this.typeList.size() == 0) {
                        // happen
                        this.cancel();
                    } else {
                        this.showMessage();
                    }
                }
            }

            public void done() {
                this.stop();
                part.addListToHash(sa, "Returned");
                payment.paidCost(part);
            }

            public void cancel() {
                this.stop();
                payment.cancelCost();
            }
        };

        return target;
    } // returnType()

    /**
     * <p>
     * returnThis.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param payment
     *            a {@link forge.card.cost.CostPayment} object.
     * @param part
     *            TODO
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input returnThis(final SpellAbility sa, final CostPayment payment, final CostReturn part) {
        final Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;

            @Override
            public void showMessage() {
                final Card card = sa.getSourceCard();
                if (card.getController().isHuman() && AllZoneUtil.isCardInPlay(card)) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(card.getName());
                    sb.append(" - Return to Hand?");
                    final Object[] possibleValues = { "Yes", "No" };
                    final Object choice = JOptionPane.showOptionDialog(null, sb.toString(), card.getName() + " - Cost",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, possibleValues,
                            possibleValues[0]);
                    if (choice.equals(0)) {
                        part.addToList(card);
                        Singletons.getModel().getGameAction().moveToHand(card);
                        this.stop();
                        part.addListToHash(sa, "Returned");
                        payment.paidCost(part);
                    } else {
                        this.stop();
                        payment.cancelCost();
                    }
                }
            }
        };

        return target;
    } // input_sacrifice()
}
