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

import com.google.common.base.Predicate;

import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Map;

/**
 * The Class CostRemoveAnyCounter.
 */
public class CostRemoveAnyCounter extends CostPartWithList {
    // RemoveAnyCounter<Num/Type/{TypeDescription}>
    // Power Conduit and Chisei, Heart of Oceans
    // Both cards have "Remove a counter from a permanent you control"
    private CounterType counterType;
    /**
     * @param counterType the counterType to set
     */
    public void setCounterType(CounterType counterType) {
        this.counterType = counterType;
    }

    /**
     * Instantiates a new cost CostRemoveAnyCounter.
     *
     * @param amount
     *            the amount
     */
    public CostRemoveAnyCounter(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#getHashForList()
     */
    @Override
    public String getHashForLKIList() {
        return "CounterRemove";
    }
    @Override
    public String getHashForCardList() {
    	return "CounterRemoveCards";
    }

    /**
     * Gets the counter.
     *
     * @return the counter
     */
    public CounterType getCounter() {
        return this.counterType;
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
        final Card source = ability.getHostCard();
        CardCollectionView validCards = activator.getCardsIn(ZoneType.Battlefield);
        validCards = CardLists.getValidCards(validCards, this.getType().split(";"), activator, source, ability);
        validCards = CardLists.filter(validCards, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.hasCounters();
            }
        });
        if (validCards.isEmpty()) {
            return false;
        }
        Integer i = this.convertAmount();

        if (i == null) {
            i = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }
        int allCounters = 0;
        for (Card c : validCards) {
            final Map<CounterType, Integer> tgtCounters = c.getCounters();
            for (Integer value : tgtCounters.values()) {
                allCounters += value;
            }
        }

        return i <= allCounters;
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("Remove ");
        sb.append(Cost.convertIntAndTypeToWords(this.convertAmount(), "counter"));
        final String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();
        sb.append(" from " + desc);

        return sb.toString();
    }

    @Override
    public boolean payAsDecided(Player ai, PaymentDecision decision, SpellAbility ability) {
        final String amount = this.getAmount();
        final Card source = ability.getHostCard();
        Integer c = this.convertAmount();
        if (c == null) {
            c = AbilityUtils.calculateAmount(source, amount, ability);
        }
        Card valid = decision.cards.get(0);
        counterType = decision.ct;
        for (int i = 0; i < c; i++) {
            executePayment(ability, valid);
        }
        source.setSVar("CostCountersRemoved", Integer.toString(c));
        return true;
    }

    @Override
    protected Card doPayment(SpellAbility ability, Card targetCard){
        targetCard.subtractCounter(this.getCounter(), 1);
        return targetCard;
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
