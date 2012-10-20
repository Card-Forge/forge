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
 * The Class CostSacrifice.
 */
public class CostSacrifice extends CostPartWithList {

    /**
     * Instantiates a new cost sacrifice.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostSacrifice(final String amount, final String type, final String description) {
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
        sb.append("Sacrifice ");

        final Integer i = this.convertAmount();

        if (this.getThis()) {
            sb.append(this.getType());
        } else {
            final String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();
            if (i != null) {
                sb.append(Cost.convertIntAndTypeToWords(i, desc));
            } else {
                sb.append(Cost.convertAmountTypeToWords(this.getAmount(), desc));
            }
        }
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
        // You can always sac all
        if (!this.getThis()) {
            List<Card> typeList = activator.getCardsIn(ZoneType.Battlefield);
            typeList = CardLists.getValidCards(typeList, this.getType().split(";"), activator, source);

            final Integer amount = this.convertAmount();

            if (activator.hasKeyword("You can't sacrifice creatures to cast spells or activate abilities.")) {
                typeList = CardLists.getNotType(typeList, "Creature");
            }

            if ((amount != null) && (typeList.size() < amount)) {
                return false;
            }

            // If amount is null, it's either "ALL" or "X"
            // if X is defined, it needs to be calculated and checked, if X is
            // choice, it can be Paid even if it's 0
        }
        else {
            if (!source.isInPlay()) {
                return false;
            }
            else if (source.isCreature() && activator.hasKeyword("You can't sacrifice creatures to cast spells or activate abilities.")) {
                return false;
            }
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
        this.addListToHash(ability, "Sacrificed");
        for (final Card c : this.getList()) {
            Singletons.getModel().getGame().getAction().sacrifice(c, ability);
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
        final String type = this.getType();
        final Player activator = ability.getActivatingPlayer();
        List<Card> list = activator.getCardsIn(ZoneType.Battlefield);
        list = CardLists.getValidCards(list, type.split(";"), activator, source);
        if (activator.hasKeyword("You can't sacrifice creatures to cast spells or activate abilities.")) {
            list = CardLists.getNotType(list, "Creature");
        }

        if (this.getThis()) {
            final Input inp = CostSacrifice.sacrificeThis(ability, payment, this);
            Singletons.getModel().getMatch().getInput().setInputInterrupt(inp);
        } else if (amount.equals("All")) {
            this.setList(list);
            CostSacrifice.sacrificeAll(ability, payment, this, list);
            //this.addListToHash(ability, "Sacrificed");
            return true;
        } else {
            Integer c = this.convertAmount();
            if (c == null) {
                final String sVar = ability.getSVar(amount);
                // Generalize this
                if (sVar.equals("XChoice")) {
                    c = CostUtil.chooseXValue(source, ability, list.size());
                } else {
                    c = AbilityFactory.calculateAmount(source, amount, ability);
                }
            }
            if (c != null && 0 == c.intValue()) {
                payment.setPaidManaPart(this);
                return true;
            }
            final Input inp = CostSacrifice.sacrificeFromList(ability, payment, this, list, c);
            Singletons.getModel().getMatch().getInput().setInputInterrupt(inp);
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
        final Player activator = ability.getActivatingPlayer();
        if (this.getThis()) {
            this.getList().add(source);
        } else if (this.getAmount().equals("All")) {
            List<Card> typeList = activator.getCardsIn(ZoneType.Battlefield);
            typeList = CardLists.getValidCards(typeList, this.getType().split(";"), activator, source);
            if (activator.hasKeyword("You can't sacrifice creatures to cast spells or activate abilities.")) {
                typeList = CardLists.getNotType(typeList, "Creature");
            }
            // Does the AI want to use Sacrifice All?
            return false;
        } else {
            Integer c = this.convertAmount();
            if (c == null) {
                if (ability.getSVar(this.getAmount()).equals("XChoice")) {
                    return false;
                }

                c = AbilityFactory.calculateAmount(source, this.getAmount(), ability);
            }
            this.setList(ComputerUtil.chooseSacrificeType(activator, this.getType(), source, ability.getTargetCard(), c));
            if (this.getList() == null) {
                return false;
            }
        }
        return true;
    }

    // Inputs

    /**
     * <p>
     * sacrificeAllType.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param payment
     *            a {@link forge.card.cost.CostPayment} object.
     * @param part
     *            TODO
     * @param typeList
     *            TODO
     */
    public static void sacrificeAll(final SpellAbility sa, final CostPayment payment, final CostPart part,
            final List<Card> typeList) {
        // TODO Ask First
        for (final Card card : typeList) {
            payment.getAbility().addCostToHashList(card, "Sacrificed");
            Singletons.getModel().getGame().getAction().sacrifice(card, sa);
        }

        payment.setPaidManaPart(part);
    }

    /**
     * <p>
     * sacrificeFromList.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param payment
     *            a {@link forge.card.cost.CostPayment} object.
     * @param part
     *            TODO
     * @param typeList
     *            TODO
     * @param nNeeded
     *            the n needed
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input sacrificeFromList(final SpellAbility sa, final CostPayment payment, final CostSacrifice part,
            final List<Card> typeList, final int nNeeded) {
        final Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            private int nSacrifices = 0;

            @Override
            public void showMessage() {
                if (nNeeded == 0) {
                    this.done();
                }

                final StringBuilder msg = new StringBuilder("Sacrifice ");
                final int nLeft = nNeeded - this.nSacrifices;
                msg.append(nLeft).append(" ");
                msg.append(part.getDescriptiveType());
                if (nLeft > 1) {
                    msg.append("s");
                }

                CMatchUI.SINGLETON_INSTANCE.showMessage(msg.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.cancel();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (typeList.contains(card)) {
                    this.nSacrifices++;
                    part.addToList(card);
                    Singletons.getModel().getGame().getAction().sacrifice(card, sa);
                    typeList.remove(card);
                    // in case nothing else to sacrifice
                    if (this.nSacrifices == nNeeded) {
                        this.done();
                    } else if (typeList.size() == 0) {
                        // happen
                        this.cancel();
                    } else {
                        this.showMessage();
                    }
                }
            }

            public void done() {
                this.stop();
                part.addListToHash(sa, "Sacrificed");
                payment.paidCost(part);
            }

            public void cancel() {
                this.stop();

                payment.cancelCost();
            }
        };

        return target;
    } // sacrificeType()

    /**
     * <p>
     * sacrificeThis.
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
    public static Input sacrificeThis(final SpellAbility sa, final CostPayment payment, final CostSacrifice part) {
        final Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;

            @Override
            public void showMessage() {
                final Card card = sa.getSourceCard();
                if (card.getController().isHuman() && card.isInPlay()) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(card.getName());
                    sb.append(" - Sacrifice?");
                    final Object[] possibleValues = { "Yes", "No" };
                    final Object choice = JOptionPane.showOptionDialog(null, sb.toString(), card.getName() + " - Cost",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, possibleValues,
                            possibleValues[0]);
                    if (choice.equals(0)) {
                        part.addToList(card);
                        part.addListToHash(sa, "Sacrificed");
                        Singletons.getModel().getGame().getAction().sacrifice(card, sa);
                        this.stop();
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
