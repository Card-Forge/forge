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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardLists;
import forge.CounterType;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.control.input.InputSelectCards;
import forge.game.GameState;
import forge.game.ai.ComputerUtilCard;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The Class CostPutCounter.
 */
public class CostPutCounter extends CostPartWithList {
    /** 
     * TODO: Write javadoc for this type.
     *
     */
    public static final class InputSelectCardToPutCounter extends InputSelectCards {

        private static final long serialVersionUID = 2685832214519141903L;
        private List<Card> validChoices;
        private final Map<Card,Integer> cardsChosen;

        /**
         * TODO: Write javadoc for Constructor.
         * @param type
         * @param costPutCounter
         * @param nNeeded
         * @param payment
         * @param sa
         */
        public InputSelectCardToPutCounter(int cntCounters, List<Card> targets) {
            super(cntCounters, cntCounters);
            validChoices = targets;
            cardsChosen = cntCounters > 1 ? new HashMap<Card, Integer>() : null;
        }
        
        protected String getMessage() {
            return max == Integer.MAX_VALUE
                ? String.format(message, getDistibutedCounters())
                : String.format(message, max - getDistibutedCounters());
        }
        
        private int getDistibutedCounters() {
            int sum = 0;
            for(Card c : selected) {
                sum += max == 1 || cardsChosen.get(c) == null ? 1 : cardsChosen.get(c).intValue();
            }
            return sum;
        }


        @Override
        protected boolean selectEntity(Card c) {
            if (!isValidChoice(c)) {
                return false;
            }

            int tc = getTimesSelected(c);
            if( tc == 0)
                selected.add(c);
            else
                cardsChosen.put(c, tc+1);
            
            onSelectStateChanged(c, true);
            return true;
        }

        @Override
        protected boolean hasEnoughTargets() {
            return hasAllTargets();
        }

        @Override
        protected boolean hasAllTargets() {
            int sum = getDistibutedCounters();
            return sum >= max;
        }

        @Override
        protected final boolean isValidChoice(Card choice) {
            return validChoices.contains(choice);
        }

        public int getTimesSelected(Card c) {
            return selected.contains(c) ? max == 1 || cardsChosen.get(c) == null ? 1 : cardsChosen.get(c).intValue() : 0;
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
    public final boolean canPay(final SpellAbility ability) {
        final Player activator = ability.getActivatingPlayer();
        final Card source = ability.getSourceCard();
        if (this.payCostFromSource()) {
            if (!source.canReceiveCounters(this.counter)) {
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
    public void payAI(PaymentDecision decision, Player ai, SpellAbility ability, Card source) {
        Integer c = getNumberOfCounters(ability);

        if (this.payCostFromSource()) {
            executePayment(ability, source, c);
        } else {
            // Put counter on chosen card
            executePayment(ability, decision.cards);
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
    public final boolean payHuman(final SpellAbility ability, final GameState game) {
        final Card source = ability.getSourceCard();
        Integer c = getNumberOfCounters(ability);

        if (this.payCostFromSource()) {
            executePayment(ability, source, c);
            lastPaidAmount = c; 
            return true;
        } else {
            // Cards to use this branch: Scarscale Ritual, Wandering Mage - each adds only one counter 
            final Player actor = ability.getActivatingPlayer();
            List<Card> typeList = CardLists.getValidCards(actor.getCardsIn(ZoneType.Battlefield), getType().split(";"), actor, ability.getSourceCard());
            
            InputSelectCardToPutCounter inp = new InputSelectCardToPutCounter(c, typeList);
            inp.setMessage("Put %d " + getCounter().getName() + " counter on " + getDescriptiveType());
            inp.setCancelAllowed(true);
            Singletons.getControl().getInputQueue().setInputAndWait(inp);
            
            if(inp.hasCancelled())
                return false;

            int sum = 0;
            for(Card crd : inp.getSelected()) {
                int added = inp.getTimesSelected(crd);
                sum += added;
                executePayment(ability, crd, added);
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
        executePayment(ability, targetCard);
    }


    @Override
    public String getHashForList() {
        return "CounterPut";
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#decideAIPayment(forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public PaymentDecision decideAIPayment(Player ai, SpellAbility ability, Card source) {

        if (this.payCostFromSource()) {
            return new PaymentDecision(source);

        }

        final List<Card> typeList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), this.getType().split(";"), ai, source);

        Card card = null;
        if (this.getType().equals("Creature.YouCtrl")) {
            card = ComputerUtilCard.getWorstCreatureAI(typeList);
        } else {
            card = ComputerUtilCard.getWorstPermanentAI(typeList, false, false, false, false);
        }
        return new PaymentDecision(card);
    }
}
