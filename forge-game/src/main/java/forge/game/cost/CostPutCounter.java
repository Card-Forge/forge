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

import com.google.common.collect.Sets;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityKey;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementType;
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
            source.subtractCounter(this.counter, this.lastPaidAmount, null);
        else {
            final Integer i = this.convertAmount();
            for (final Card c : this.getCardList()) {
                c.subtractCounter(this.counter, i, null);
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
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        final Card source = ability.getHostCard();
        final Game game = source.getGame();
        if (this.payCostFromSource()) {
            if (isETBReplacement(ability, effect)) {
                final Card copy = CardCopyService.getLKICopy(source);
                copy.setLastKnownZone(payer.getZone(ZoneType.Battlefield));

                // check state it would have on the battlefield
                CardCollection preList = new CardCollection(copy);
                game.getAction().checkStaticAbilities(false, Sets.newHashSet(copy), preList);
                // reset again?
                game.getAction().checkStaticAbilities(false);
                if (copy.canReceiveCounters(getCounter())) {
                    return true;
                }
            } else {
                if (!source.isInPlay()) {
                    return false;
                }
                if (source.canReceiveCounters(getCounter())) {
                    return true;
                }
            }
            return getAbilityAmount(ability) == 0;
        }

        // 3 Cards have Put a -1/-1 Counter on a Creature you control.
        List<Card> typeList = CardLists.getValidCards(source.getGame().getCardsIn(ZoneType.Battlefield),
                this.getType().split(";"), payer, source, ability);

        typeList = CardLists.filter(typeList, CardPredicates.canReceiveCounters(getCounter()));

        return !typeList.isEmpty();
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.card.cost.CostPart#payAI(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public boolean payAsDecided(Player payer, PaymentDecision decision, SpellAbility ability, final boolean effect) {
        if (this.payCostFromSource()) {
            executePayment(payer, ability, ability.getHostCard(), effect);
        } else {
            executePayment(payer, ability, decision.cards, effect);
        }
        triggerCounterPutAll(ability, effect);
        return true;
    }

    @Override
    protected Card doPayment(Player payer, SpellAbility ability, Card targetCard, final boolean effect) {
        final int i = getAbilityAmount(ability);
        if (isETBReplacement(ability, effect)) {
            targetCard.addEtbCounter(getCounter(), i, payer);
        } else {
            targetCard.addCounter(getCounter(), i, payer, counterTable);
        }
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

    protected void triggerCounterPutAll(final SpellAbility ability, final boolean effect) {
        if (counterTable.isEmpty()) {
            return;
        }

        GameEntityCounterTable tempTable = new GameEntityCounterTable(counterTable);
        tempTable.replaceCounterEffect(ability.getHostCard().getGame(), ability, effect);
    }

    /* (non-Javadoc)
     * @see forge.game.cost.CostPartWithList#resetLists()
     */
    @Override
    public void resetLists() {
        super.resetLists();
        counterTable.clear();
    }

    protected boolean isETBReplacement(final SpellAbility ability, final boolean effect) {
       if (!effect) {
           return false;
       }
       // only for itself?
       if (!payCostFromSource()) {
           return false;
       }
       if (ability == null) {
           return false;
       }
       if (!ability.isReplacementAbility()) {
           return false;
       }
       ReplacementEffect re = ability.getReplacementEffect();
       if (re.getMode() != ReplacementType.Moved) {
           return false;
       }
       if (!ability.getHostCard().equals(ability.getReplacingObject(AbilityKey.Card))) {
           return false;
       }
       return true;
   }
}
