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
import forge.CounterType;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.control.input.InputSynchronized;
import forge.game.GameState;
import forge.game.ai.ComputerUtilCard;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/**
 * The Class CostPutCounter.
 */
public class CostPutCounter extends CostPartWithList {
    /** 
     * TODO: Write javadoc for this type.
     *
     */
    public static final class InputPayCostPutCounter extends InputPayCostBase {
        private final String type;
        private final CostPutCounter costPutCounter;
        private final int nNeeded;
        private final SpellAbility sa;
        private static final long serialVersionUID = 2685832214519141903L;
        private List<Card> typeList;
        private int nPut = 0;

        /**
         * TODO: Write javadoc for Constructor.
         * @param type
         * @param costPutCounter
         * @param nNeeded
         * @param payment
         * @param sa
         */
        public InputPayCostPutCounter(String type, CostPutCounter costPutCounter, int nNeeded, CostPayment payment, SpellAbility sa) {
            super(payment);
            this.type = type;
            this.costPutCounter = costPutCounter;
            this.nNeeded = nNeeded;
            this.sa = sa;
        }

        @Override
        public void showMessage() {
            if ((nNeeded == 0) || (nNeeded == this.nPut)) {
                this.done();
            }

            final StringBuilder msg = new StringBuilder("Put ");
            final int nLeft = nNeeded - this.nPut;
            msg.append(nLeft).append(" ");
            msg.append(costPutCounter.getCounter()).append(" on ");

            msg.append(costPutCounter.getDescriptiveType());
            if (nLeft > 1) {
                msg.append("s");
            }

            this.typeList = CardLists.getValidCards(sa.getActivatingPlayer().getCardsIn(ZoneType.Battlefield), type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());
            CMatchUI.SINGLETON_INSTANCE.showMessage(msg.toString());
            ButtonUtil.enableOnlyCancel();
        }

        @Override
        public void selectCard(final Card card) {
            if (this.typeList.contains(card)) {
                this.nPut++;
                costPutCounter.addToList(card);
                card.addCounter(costPutCounter.getCounter(), 1, false);

                if (nNeeded == this.nPut) {
                    this.done();
                } else {
                    this.showMessage();
                }
            }
        }
    }

    // Put Counter doesn't really have a "Valid" portion of the cost
    private final CounterType counter;
    private int lastPaidAmount = 0;

    /**
     * Gets the counter.
     * 
     * @return the counter
     */
    public final CounterType getCounter() {
        return this.counter;
    }

    /**
     * Sets the last paid amount.
     * 
     * @param paidAmount
     *            the new last paid amount
     */
    public final void setLastPaidAmount(final int paidAmount) {
        this.lastPaidAmount = paidAmount;
    }

    /**
     * Instantiates a new cost put counter.
     * 
     * @param amount
     *            the amount
     * @param cntr
     *            the cntr
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostPutCounter(final String amount, final CounterType cntr, final String type, final String description) {
        super(amount, type, description);
        this.counter = cntr;
    }
    
    @Override
    public boolean isReusable() { return true; }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        if (this.counter == CounterType.LOYALTY) {
            sb.append("+").append(this.getAmount());
        } else {
            sb.append("Put ");
            final Integer i = this.convertAmount();
            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), this.counter.getName() + " counter"));

            sb.append(" on ");
            if (this.payCostFromSource()) {
                sb.append(this.getType());
            } else {
                final String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();
                sb.append(desc);
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
    public final void refund(final Card source) {
        for (final Card c : this.getList()) {
            c.subtractCounter(this.counter, this.lastPaidAmount);
        }
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
        if (this.payCostFromSource()) {
            if (source.hasKeyword("CARDNAME can't have counters placed on it.")) {
                return false;
            }
            if (source.hasKeyword("CARDNAME can't have -1/-1 counters placed on it.")
                    && this.counter.equals(CounterType.M1M1)) {
                return false;
            }
        } else {
            // 3 Cards have Put a -1/-1 Counter on a Creature you control.
            final List<Card> typeList = CardLists.getValidCards(activator.getCardsIn(ZoneType.Battlefield), this.getType().split(";"), activator, source);

            if (typeList.size() == 0) {
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
    public final void payAI(final AIPlayer ai, final SpellAbility ability, final Card source, final CostPayment payment, final GameState game) {
        Integer c = this.convertAmount();
        if (c == null) {
            c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }

        if (this.payCostFromSource()) {
            source.addCounter(this.getCounter(), c, false);
        } else {
            // Put counter on chosen card
            for (final Card card : this.getList()) {
                card.addCounter(this.getCounter(), 1, false);
            }
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
    public final void payHuman(final SpellAbility ability, final Card source, final CostPayment payment, final GameState game) {
        Integer c = this.convertAmount();
        if (c == null) {
            c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }

        if (this.payCostFromSource()) {
            source.addCounter(this.getCounter(), c, false);
            this.addToList(source);
        } else {
            InputSynchronized inp = new InputPayCostPutCounter(this.getType(), this, c, payment, ability);
            inp.awaitLatchRelease();
        }
        if ( !payment.isCanceled())
            addListToHash(ability, "CounterPut");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#decideAIPayment(forge.card.spellability.SpellAbility
     * , forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean decideAIPayment(final AIPlayer ai, final SpellAbility ability, final Card source, final CostPayment payment) {
        this.resetList();
        if (this.payCostFromSource()) {
            this.addToList(source);
            return true;
        } else {
            Integer c = this.convertAmount();
            if (c == null) {
                c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
            }

            final List<Card> typeList =
                    CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), this.getType().split(";"), ai, source);

            Card card = null;
            if (this.getType().equals("Creature.YouCtrl")) {
                card = ComputerUtilCard.getWorstCreatureAI(typeList);
            } else {
                card = ComputerUtilCard.getWorstPermanentAI(typeList, false, false, false, false);
            }
            this.addToList(card);
        }
        return true;
    }
}
