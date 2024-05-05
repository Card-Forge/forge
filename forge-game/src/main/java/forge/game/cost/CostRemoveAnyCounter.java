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

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Optional;

import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Class CostRemoveAnyCounter.
 */
public class CostRemoveAnyCounter extends CostPart {
    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;
    // RemoveAnyCounter<Num/Type/{TypeDescription}>
    // things like "Remove a counter from a permanent you control"
    // or "Remove one or more +1/+1 counters from among artifacts you control"

    public final CounterType counter;
    public final Boolean oneOrMore;

    /**
     * Instantiates a new cost CostRemoveAnyCounter.
     *
     * @param amount
     *            the amount
     */
    public CostRemoveAnyCounter(final String amount, final CounterType counter, final String type, final String description, final boolean oneOrMore) {
        super(amount, type, description);
        this.counter = counter;
        this.oneOrMore = oneOrMore;
    }

    @Override
    public int paymentOrder() { return 8; }

    @Override
    public Integer getMaxAmountX(final SpellAbility ability, final Player payer, final boolean effect) {
        final Card source = ability.getHostCard();

        CardCollectionView validCards = CardLists.getValidCards(payer.getCardsIn(ZoneType.Battlefield), this.getType().split(";"), payer, source, ability);
        int allCounters = 0;
        for (Card c : validCards) {
            if (this.counter != null) {
                allCounters += c.getCounters(this.counter);
            } else {
                for (Integer value : c.getCounters().values()) {
                    allCounters += value;
                }
            }
        }
        return allCounters;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        return AbilityUtils.calculateAmount(ability.getHostCard(), this.getAmount(), ability) <= getMaxAmountX(ability, payer, effect);
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();

        final String counters =  this.counter == null ? "counter" : this.counter.getName().toLowerCase() + " counter";
        final String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();

        sb.append("Remove ");
        if (oneOrMore) {
            sb.append("one or more ").append(counters).append("s");
        } else {
            sb.append(Cost.convertAmountTypeToWords(this.convertAmount(), this.getAmount(), counters));
            sb.append(this.getAmount().equals("1") ? "" : "s");
        }
        sb.append(" from ").append(desc).append(" you control");

        return sb.toString();
    }

    @Override
    public boolean payAsDecided(Player ai, PaymentDecision decision, SpellAbility ability, final boolean effect) {
        int removed = 0;
        for (Entry<GameEntity, Map<CounterType, Integer>> e : decision.counterTable.row(Optional.absent()).entrySet()) {
            for (Entry<CounterType, Integer> v : e.getValue().entrySet()) {
                removed += v.getValue();
                e.getKey().subtractCounter(v.getKey(), v.getValue(), ai);
            }
            if (e.getKey() instanceof Card) {
                e.getKey().getGame().updateLastStateForCard((Card) e.getKey());
            }
        }

        ability.setSVar("CostCountersRemoved", Integer.toString(removed));
        return true;
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
