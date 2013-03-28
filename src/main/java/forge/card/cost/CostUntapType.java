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
import forge.FThreads;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.control.input.InputPayment;
import forge.game.GameState;
import forge.game.ai.ComputerUtil;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.view.ButtonUtil;

/**
 * The Class CostUntapType.
 */
public class CostUntapType extends CostPartWithList {

    /** 
     * TODO: Write javadoc for this type.
     *
     */
    public static final class InputPayCostUntapY extends InputPayCostBase {
        private final int nCards;
        private final List<Card> cardList;
        private final CostUntapType untapType;
        private static final long serialVersionUID = -7151144318287088542L;
        private int nUntapped = 0;
        private final SpellAbility sa;


        /**
         * TODO: Write javadoc for Constructor.
         * @param nCards
         * @param cardList
         * @param untapType
         * @param ability 
         * @param sa
         * @param payment
         */
        public InputPayCostUntapY(int nCards, List<Card> cardList, CostUntapType untapType, SpellAbility ability) {
            this.nCards = nCards;
            this.cardList = cardList;
            this.untapType = untapType;
            this.sa = ability;
        }

        @Override
        public void showMessage() {
            if (nCards == 0) {
                this.done();
            }

            if (cardList.size() == 0) {
                this.stop();
            }

            final int left = nCards - this.nUntapped;
            showMessage("Select a " + untapType.getDescription() + " to untap (" + left + " left)");
            ButtonUtil.enableOnlyCancel();
        }


        @Override
        public void selectCard(final Card card) {
            Zone zone = Singletons.getModel().getGame().getZoneOf(card);
            if (zone.is(ZoneType.Battlefield) && cardList.contains(card) && card.isTapped()) {
                // send in List<Card> for Typing
                untapType.executePayment(sa, card);
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
    }

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
    }

    @Override
    public boolean isReusable() { return true; }

    
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
    public final boolean canPay(final SpellAbility ability, final Card source, final Player activator, final Cost cost, final GameState game) {
        List<Card> typeList = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);

        typeList = CardLists.getValidCards(typeList, this.getType().split(";"), activator, source);

        if (cost.hasUntapCost()) {
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
     * @see
     * forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payHuman(final SpellAbility ability, final GameState game) {
        final boolean canUntapSource = false; // payment.getCost().hasUntapCost(); - only Crackleburr uses this
        List<Card> typeList = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        typeList = CardLists.getValidCards(typeList, this.getType().split(";"), ability.getActivatingPlayer(), ability.getSourceCard());
        typeList = CardLists.filter(typeList, Presets.TAPPED);
        final Card source = ability.getSourceCard();
        if (canUntapSource) {
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
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
        InputPayment inp = new InputPayCostUntapY(c, typeList, this, ability);
        FThreads.setInputAndWait(inp);
        return inp.isPaid();
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected void doPayment(SpellAbility ability, Card targetCard) {
        targetCard.untap();
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#getHashForList()
     */
    @Override
    public String getHashForList() {
        return "Untapped";
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#decideAIPayment(forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public PaymentDecision decideAIPayment(AIPlayer ai, SpellAbility ability, Card source) {
        boolean untap = false; // payment.getCost().hasUntapCost(); 
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
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
    
        List<Card> list = ComputerUtil.chooseUntapType(ai, this.getType(), source, untap, c);
    
        if (list == null) {
            System.out.println("Couldn't find a valid card to untap for: " + source.getName());
            return null;
        }
    
        return new PaymentDecision(list);
    }

    // Inputs

}
