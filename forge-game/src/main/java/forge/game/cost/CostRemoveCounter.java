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

import com.google.common.collect.Lists;

import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

/**
 * The Class CostRemoveCounter.
 */
public class CostRemoveCounter extends CostPartWithList {
    // SubCounter<Num/Counter/{Type/TypeDescription/Zone}>

    // Here are the cards that have RemoveCounter<Type>
    // Ion Storm, Noviken Sages, Ghave, Guru of Spores, Power Conduit (any
    // Counter is tough),
    // Quillspike, Rift Elemental, Sage of Fables, Spike Rogue


    public final CounterType counter;
    public final ZoneType zone;
    private int cntRemoved;

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
    public CostRemoveCounter(final String amount, final CounterType counter, final String type, final String description, ZoneType zone) {
        super(amount, type, description);

        this.counter = counter;
        this.zone = zone;
    }

    @Override
    public int paymentOrder() { return 8; }

    @Override
    public boolean isUndoable() { return true; }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        if (this.counter == CounterType.LOYALTY) {
            sb.append("-").append(this.getAmount());
        } else {
            sb.append("Remove ");
            final Integer i = this.convertAmount();
            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), this.counter.getName() + " counter"));

            if (this.getAmount().equals("All")) {
                sb.append("s");
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
     * @see forge.card.cost.CostPart#refund(forge.Card)
     */
    @Override
    public final void refund(final Card source) {
        int refund = this.getCardList().size() == 1 ? this.cntRemoved : 1; // is wrong for Ooze Flux and Novijen Sages
        for (final Card c : this.getCardList()) {
            c.addCounter(this.counter, refund, false);
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
        final CounterType cntrs = this.counter;
        final Player activator = ability.getActivatingPlayer();
        final Card source = ability.getHostCard();
        final String type = this.getType();

        final Integer amount = this.convertAmount();
        if (this.payCostFromSource()) {
            if ((amount != null) && ((source.getCounters(cntrs) - amount) < 0)) {
                return false;
            }
        }
        else {
            List<Card> typeList;
            if (type.equals("OriginalHost")) {
                typeList = Lists.newArrayList(ability.getOriginalHost());
            } else {
                typeList = CardLists.getValidCards(activator.getCardsIn(this.zone), type.split(";"), activator, source, ability);
            }
            if (amount != null) {
                if (this.getTypeDescription().equals("among creatures you control")) {
                    // remove X counters from among creatures you control
                    int totalCounters = 0;
                    for (Card c : typeList) {
                        totalCounters += c.getCounters(cntrs);
                    }
                    if (totalCounters >= amount) {
                        return true;
                    }
                    
                } else {
                    // (default logic) remove X counters from a single permanent
                    for (Card c : typeList) {
                        if (c.getCounters(cntrs) - amount >= 0) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean payAsDecided(Player ai, PaymentDecision decision, SpellAbility ability) {
        Card source = ability.getHostCard();
        cntRemoved = decision.c;
        for (final Card card : decision.cards) {
            executePayment(ability, card);
        }
        source.setSVar("CostCountersRemoved", Integer.toString(cntRemoved));
        return true;
    }

    @Override
    protected Card doPayment(SpellAbility ability, Card targetCard){
        targetCard.subtractCounter(this.counter, cntRemoved);
        return targetCard;
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

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
