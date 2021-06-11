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

import forge.game.GameEntityCounterTable;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Class CostPutCounter.
 */
public class CostPutCounter extends CostPartWithList {
     /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;
    // Put Counter doesn't really have a "Valid" portion of the cost
    private final CounterType counter;
    private int lastPaidAmount = 0;

    private final GameEntityCounterTable counterTable = new GameEntityCounterTable();

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
    public int paymentOrder() { return 6; }

    @Override
    public boolean isReusable() {
        return !counter.is(CounterEnumType.M1M1);
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        if (this.counter.is(CounterEnumType.LOYALTY)) {
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
        if(this.payCostFromSource())
            source.subtractCounter(this.counter, this.lastPaidAmount);
        else {
            final Integer i = this.convertAmount();
            for (final Card c : this.getCardList()) {
                c.subtractCounter(this.counter, i);
            }
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
    public final boolean canPay(final SpellAbility ability, final Player payer) {
        final Card source = ability.getHostCard();
        if (this.payCostFromSource()) {
            return source.canReceiveCounters(this.counter);
        } else {
            // 3 Cards have Put a -1/-1 Counter on a Creature you control.
            List<Card> typeList = CardLists.getValidCards(source.getGame().getCardsIn(ZoneType.Battlefield),
                    this.getType().split(";"), payer, source, ability);

            typeList = CardLists.filter(typeList, CardPredicates.canReceiveCounters(this.counter));

            return !typeList.isEmpty();
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see forge.card.cost.CostPart#payAI(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public boolean payAsDecided(Player ai, PaymentDecision decision, SpellAbility ability) {
        if (this.payCostFromSource()) {
            executePayment(ability, ability.getHostCard());
        } else {
            executePayment(ai, ability, decision.cards);
        }
        triggerCounterPutAll(ability);
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected Card doPayment(SpellAbility ability, Card targetCard){
        final Integer i = this.convertAmount();
        targetCard.addCounter(this.getCounter(), i, ability.getActivatingPlayer(), null, ability.getRootAbility().isTrigger(), counterTable);
        return targetCard;
    }

    @Override
    public String getHashForLKIList() {
        return "CounterPut";
    }
    @Override
    public String getHashForCardList() {
    	return "CounterPutCards";
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

    protected void triggerCounterPutAll(final SpellAbility ability) {
        if (counterTable.isEmpty()) {
            return;
        }

        GameEntityCounterTable tempTable = new GameEntityCounterTable();
        tempTable.putAll(counterTable);
        tempTable.triggerCountersPutAll(ability.getHostCard().getGame());
    }


    /* (non-Javadoc)
     * @see forge.game.cost.CostPartWithList#resetLists()
     */
    @Override
    public void resetLists() {
        super.resetLists();
        counterTable.clear();
    }

}
