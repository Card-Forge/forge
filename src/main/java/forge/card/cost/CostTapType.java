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

import java.util.ArrayList;
import java.util.List;

import forge.Card;

import forge.CardLists;
import forge.CardPredicates.Presets;
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
 * The Class CostTapType.
 */
public class CostTapType extends CostPartWithList {

    /**
     * Instantiates a new cost tap type.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostTapType(final String amount, final String type, final String description) {
        super(amount, type, description);
        this.setReusable(true);
    }

    /**
     * Gets the description.
     * 
     * @return the description
     */
    public final String getDescription() {
        return this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Tap ");

        final Integer i = this.convertAmount();
        final String desc = this.getDescription();

        sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), "untapped " + desc));

        sb.append(" you control");

        return sb.toString();
    }

    /**
     * Adds the to tapped list.
     * 
     * @param c
     *            the c
     */
    public final void addToTappedList(final Card c) {
        this.getList().add(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#refund(forge.Card)
     */
    @Override
    public final void refund(final Card source) {
        for (final Card c : this.getList()) {
            c.setTapped(false);
        }

        this.getList().clear();
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
        List<Card> typeList = new ArrayList<Card>(activator.getCardsIn(ZoneType.Battlefield));

        typeList = CardLists.getValidCards(typeList, this.getType().split(";"), activator, source);

        if (cost.getTap()) {
            typeList.remove(source);
        }
        typeList = CardLists.filter(typeList, Presets.UNTAPPED);

        final Integer amount = this.convertAmount();
        if ((typeList.size() == 0) || ((amount != null) && (typeList.size() < amount))) {
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
            c.tap();
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
        List<Card> typeList = new ArrayList<Card>(ability.getActivatingPlayer().getCardsIn(ZoneType.Battlefield));
        typeList = CardLists.getValidCards(typeList, this.getType().split(";"), ability.getActivatingPlayer(), ability.getSourceCard());
        typeList = CardLists.filter(typeList, Presets.UNTAPPED);
        final String amount = this.getAmount();
        Integer c = this.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, ability, typeList.size());
            } else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }

        final Input inp = CostTapType.inputTapXCost(this, typeList, ability, payment, c);
        Singletons.getModel().getMatch().getInput().setInputInterrupt(inp);
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
        final boolean tap = payment.getCost().getTap();
        final String amount = this.getAmount();
        Integer c = this.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            if (sVar.equals("XChoice")) {
                List<Card> typeList = ai.getCardsIn(ZoneType.Battlefield);
                typeList = CardLists.getValidCards(typeList, this.getType().split(";"), ability.getActivatingPlayer(), ability.getSourceCard());
                typeList = CardLists.filter(typeList, Presets.UNTAPPED);
                c = typeList.size();
                source.setSVar("ChosenX", "Number$" + Integer.toString(c));
            } else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }

        this.setList(ComputerUtil.chooseTapType(ai, this.getType(), source, tap, c));

        if (this.getList() == null) {
            System.out.println("Couldn't find a valid card to tap for: " + source.getName());
            return false;
        }

        return true;
    }

    // Inputs

    /**
     * <p>
     * input_tapXCost.
     * </p>
     * 
     * @param tapType
     *            the tap type
     * @param cardList
     *            a {@link forge.CardList} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param payment
     *            a {@link forge.card.cost.CostPayment} object.
     * @param nCards
     *            a int.
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input inputTapXCost(final CostTapType tapType, final List<Card> cardList, final SpellAbility sa,
            final CostPayment payment, final int nCards) {
        final Input target = new Input() {

            private static final long serialVersionUID = 6438988130447851042L;
            private int nTapped = 0;

            @Override
            public void showMessage() {
                if (nCards == 0) {
                    this.done();
                }

                /*if (cardList.size() == 0) {
                    this.cancel();
                }*/

                final int left = nCards - this.nTapped;
                CMatchUI.SINGLETON_INSTANCE
                        .showMessage("Select a " + tapType.getDescription() + " to tap (" + left + " left)");
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.cancel();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(ZoneType.Battlefield) && cardList.contains(card) && card.isUntapped()) {
                    // send in List<Card> for Typing
                    card.tap();
                    tapType.addToList(card);
                    cardList.remove(card);

                    this.nTapped++;

                    if (this.nTapped == nCards) {
                        this.done();
                    } else if (cardList.size() == 0) {
                        // happen
                        this.cancel();
                    } else {
                        this.showMessage();
                    }
                }
            }

            public void cancel() {
                this.stop();
                payment.cancelCost();
            }

            public void done() {
                this.stop();
                tapType.addListToHash(sa, "Tapped");
                payment.paidCost(tapType);
            }
        };

        return target;
    } // input_tapXCost()
}
