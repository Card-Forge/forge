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
package forge.game.cost;

import java.util.List;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gui.input.InputSelectCardsFromList;
import forge.util.Lang;

/**
 * The Class CostPutCounter.
 */
public class CostPutCounter extends CostPartWithList {
     // Put Counter doesn't really have a "Valid" portion of the cost
    private final CounterType counter;
    private int lastPaidAmount = 0;

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
            if (this.getAmount().equals("0")) {
                sb.append("0");
            }
            else {
                sb.append("+").append(this.getAmount());
            }
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
    public final boolean canPay(final SpellAbility ability) {
        final Player activator = ability.getActivatingPlayer();
        final Card source = ability.getSourceCard();
        if (this.payCostFromSource()) {
            if (!source.canReceiveCounters(this.counter)) {
                return false;
            }
        } else {
            // 3 Cards have Put a -1/-1 Counter on a Creature you control.
            final List<Card> typeList = CardLists.getValidCards(activator.getGame().getCardsIn(ZoneType.Battlefield), this.getType().split(";"), activator, source);

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
    public boolean payAI(PaymentDecision decision, Player ai, SpellAbility ability, Card source) {
        Integer c = getNumberOfCounters(ability);

        if (this.payCostFromSource()) {
            executePayment(ability, source, c);
        } else {
            // Put counter on chosen card
        	for (int i = 0; i < c; i++) {
        		executePayment(ability, decision.cards);
        	}
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
    public final boolean payHuman(final SpellAbility ability, final Player activator) {
        final Card source = ability.getSourceCard();
        Integer c = getNumberOfCounters(ability);

        if (this.payCostFromSource()) {
            executePayment(ability, source, c);
            lastPaidAmount = c; 
            return true;
        } else {
            // Cards to use this branch: Scarscale Ritual, Wandering Mage - each adds only one counter 
            List<Card> typeList = CardLists.getValidCards(activator.getCardsIn(ZoneType.Battlefield), getType().split(";"), activator, ability.getSourceCard());
            
            InputSelectCardsFromList inp = new InputSelectCardsFromList(1, 1, typeList);
            inp.setMessage("Put " + Lang.nounWithAmount(c, getCounter().getName() + " counter") + " on " + getDescriptiveType());
            inp.setCancelAllowed(true);
            inp.showAndWait();

            if(inp.hasCancelled())
                return false;

            int sum = 0;
            for(Card crd : inp.getSelected()) {
                sum++;
                executePayment(ability, crd, 1);
            }
            source.setSVar("CostCountersAdded", Integer.toString(sum));
            lastPaidAmount = sum;
            return true;
        }
    }

    private Integer getNumberOfCounters(final SpellAbility ability) {
        Integer c = this.convertAmount();
        if (c == null) {
            c = AbilityUtils.calculateAmount(ability.getSourceCard(), this.getAmount(), ability);
        }
        return c;
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected void doPayment(SpellAbility ability, Card targetCard){
        targetCard.addCounter(this.getCounter(), 1, false);
    }
    
    protected void executePayment(SpellAbility ability, Card targetCard, int c) {
        CounterType counterType = this.getCounter();
        if( c > 1 ) {
            Integer oldValue = targetCard.getCounters().get(counterType);
            int newValue = c + (oldValue == null ? 0 : oldValue.intValue()) - 1;
            targetCard.getCounters().put(counterType, Integer.valueOf(newValue));
        }
        // added c - 1 without firing triggers, the last counter added should fire trigger.
        if (c > 0) {
            executePayment(ability, targetCard);
        }
    }


    @Override
    public String getHashForList() {
        return "CounterPut";
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
