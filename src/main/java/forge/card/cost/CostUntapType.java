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
 * The Class CostUntapType.
 */
public class CostUntapType extends CostPartWithList {

    /**
     * Instantiates a new cost untap type.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostUntapType(final String amount, final String type, final String description) {
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
        sb.append("Untap ");

        final Integer i = this.convertAmount();
        final String desc = this.getDescription();

        sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), " tapped " + desc));

        if (this.getType().contains("YouDontCtrl")) {
            sb.append(" an opponent controls");
        } else if (this.getType().contains("YouCtrl")) {
            sb.append(" you control");
        }

        return sb.toString();
    }

    /**
     * Adds the card to untapped list.
     * 
     * @param c
     *            the card
     */
    public final void addToUntappedList(final Card c) {
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
            c.setTapped(true);
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
        List<Card> typeList = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);

        typeList = CardLists.getValidCards(typeList, this.getType().split(";"), activator, source);

        if (cost.getUntap()) {
            typeList.remove(source);
        }
        typeList = CardLists.filter(typeList, Presets.TAPPED);

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
            c.untap();
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
        final boolean untap = payment.getCost().getUntap();
        List<Card> typeList = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        typeList = CardLists.getValidCards(typeList, this.getType().split(";"), ability.getActivatingPlayer(), ability.getSourceCard());
        typeList = CardLists.filter(typeList, Presets.TAPPED);
        if (untap) {
            typeList.remove(source);
        }
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

        CostUtil.setInput(CostUntapType.inputUntapYCost(this, typeList, ability, payment, c));
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
        final boolean untap = payment.getCost().getUntap();
        final String amount = this.getAmount();
        Integer c = this.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            if (sVar.equals("XChoice")) {
                List<Card> typeList = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
                typeList = CardLists.getValidCards(typeList, this.getType().split(";"), ai, ability.getSourceCard());
                if (untap) {
                    typeList.remove(source);
                }
                typeList = CardLists.filter(typeList, Presets.TAPPED);
                c = typeList.size();
                source.setSVar("ChosenX", "Number$" + Integer.toString(c));
            } else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }

        this.setList(ComputerUtil.chooseUntapType(ai, this.getType(), source, untap, c));

        if (this.getList() == null) {
            System.out.println("Couldn't find a valid card to untap for: " + source.getName());
            return false;
        }

        return true;
    }

    // Inputs

    /**
     * <p>
     * input_untapYCost.
     * </p>
     * 
     * @param untapType
     *            the untap type
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
    public static Input inputUntapYCost(final CostUntapType untapType, final List<Card> cardList, final SpellAbility sa,
            final CostPayment payment, final int nCards) {
        final Input target = new Input() {

            private static final long serialVersionUID = -7151144318287088542L;
            private int nUntapped = 0;

            @Override
            public void showMessage() {
                if (nCards == 0) {
                    this.done();
                }

                if (cardList.size() == 0) {
                    this.stop();
                }

                final int left = nCards - this.nUntapped;
                CMatchUI.SINGLETON_INSTANCE
                        .showMessage("Select a " + untapType.getDescription() + " to untap (" + left + " left)");
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.cancel();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(ZoneType.Battlefield) && cardList.contains(card) && card.isTapped()) {
                    // send in List<Card> for Typing
                    card.untap();
                    untapType.addToList(card);
                    cardList.remove(card);

                    this.nUntapped++;

                    if (this.nUntapped == nCards) {
                        this.done();
                    } else if (cardList.size() == 0) {
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
                untapType.addListToHash(sa, "Untapped");
                payment.paidCost(untapType);
            }
        };

        return target;
    } // input_untapYCost()
}
