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

import com.google.common.collect.Lists;

import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;

/**
 * The Class CostRemoveCounter.
 */
public class CostRemoveCounter extends CostPart {
    // SubCounter<Num/Counter/{Type/TypeDescription/Zone}>

    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;
    public final CounterType counter;
    public final List<ZoneType> zone;
    public final Boolean oneOrMore;

    /**
     * Instantiates a new cost remove counter.
     *
     * @param amount
     *            the amount
     * @param counter
     *            the counter
     * @param type
     *            the type
     * @param description
     *            the description
     * @param zone the zone.
     */
    public CostRemoveCounter(final String amount, final CounterType counter, final String type, final String description, final List<ZoneType> zone, final boolean oneOrMore) {
        super(amount, type, description);

        this.counter = counter;
        this.zone = zone;
        this.oneOrMore = oneOrMore;
    }

    @Override
    public int paymentOrder() { return 8; }

    @Override
    public Integer getMaxAmountX(final SpellAbility ability, final Player payer, final boolean effect) {
        final CounterType cntrs = this.counter;
        final Card source = ability.getHostCard();
        final String type = this.getType();

        if (this.payCostFromSource()) {
            return source.getCounters(cntrs);
        }

        List<Card> typeList;
        if (type.equals("OriginalHost")) {
            typeList = Lists.newArrayList(ability.getOriginalHost());
        } else {
            typeList = CardLists.getValidCards(payer.getCardsIn(this.zone), type.split(";"), payer, source, ability);
        }

        // Single Target
        int maxcount = 0;
        for (Card c : typeList) {
            maxcount = Math.max(maxcount, c.getCounters(cntrs));
        }
        return maxcount;
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        if (this.counter.is(CounterEnumType.LOYALTY) && payCostFromSource()) {
            sb.append("-").append(this.getAmount());
        } else {
            sb.append("Remove ");
            if (this.getAmount().equals("X")) {
                if (oneOrMore) {
                    sb.append("one or more ");
                } else {
                    sb.append("any number of ");
                }
                sb.append(this.counter.getName().toLowerCase()).append(" counters");
            } else if (this.getAmount().equals("All")) {
                sb.append("all ").append(this.counter.getName().toLowerCase()).append(" counters");
            } else {
                sb.append(Lang.nounWithNumeralExceptOne(this.getAmount(),
                        this.counter.getName().toLowerCase() + " counter"));
            }

            sb.append(" from ");

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
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        final CounterType cntrs = this.counter;
        final Card source = ability.getHostCard();
        final String type = this.getType();

        final int amount;
        if (getAmount().equals("All")) {
            amount = source.getCounters(cntrs);
        } else {
            amount = getAbilityAmount(ability);
        }
        if (this.payCostFromSource()) {
            return !source.isPhasedOut() && (source.getCounters(cntrs) - amount) >= 0;
        }

        List<Card> typeList;
        if (type.equals("OriginalHost")) {
            typeList = Lists.newArrayList(ability.getOriginalHost());
        } else {
            typeList = CardLists.getValidCards(payer.getCardsIn(this.zone), type.split(";"), payer, source, ability);
        }

        // (default logic) remove X counters from a single permanent
        for (Card c : typeList) {
            if (c.getCounters(cntrs) - amount >= 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean payAsDecided(Player ai, PaymentDecision decision, SpellAbility ability, final boolean effect) {
        int removed = 0;
        final int toRemove = decision.c;

        // for this cost, the list should be only one
        for (Card c : decision.cards) {
            removed += toRemove;
            c.subtractCounter(counter, toRemove, ai);
            c.getGame().updateLastStateForCard(c);
        }

        ability.setSVar("CostCountersRemoved", Integer.toString(removed));
        return true;
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
