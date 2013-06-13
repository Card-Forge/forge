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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import forge.Card;
import forge.CardLists;
import forge.CounterType;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.input.InputSelectCards;
import forge.gui.input.InputSelectCardsFromList;

/**
 * The Class CostRemoveAnyCounter.
 */
public class CostRemoveAnyCounter extends CostPartWithList {
    // RemoveAnyCounter<Num/Type/{TypeDescription}>
    // Power Conduit and Chisei, Heart of Oceans
    // Both cards have "Remove a counter from a permanent you control"
    private CounterType counterType;
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
    public String getHashForList() {
        return "CounterRemove";
    }

    /**
     * Gets the counter.
     *
     * @return the counter
     */
    private CounterType getCounter() {
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
        final Card source = ability.getSourceCard();
        List<Card> validCards = new ArrayList<Card>(activator.getCardsIn(ZoneType.Battlefield));
        validCards = CardLists.getValidCards(validCards, this.getType().split(";"), activator, source);
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

    @Override
    public final boolean payHuman(final SpellAbility ability, final Game game) {
        final Card source = ability.getSourceCard();
        Integer c = this.convertAmount();
        final String type = this.getType();
        final Player activator = ability.getActivatingPlayer();

        if (c == null) {
            c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }

        List<Card> list = new ArrayList<Card>(activator.getCardsIn(ZoneType.Battlefield));
        list = CardLists.getValidCards(list, type.split(";"), activator, source);
        int nNeed = c.intValue();
        while (nNeed > 0) {
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card card) {
                    return card.hasCounters();
                }
            });
            InputSelectCards inp = new InputSelectCardsFromList(1, 1, list);
            inp.setMessage("Select " + this.getDescriptiveType() + " to remove a counter");
            inp.setCancelAllowed(false);
            Singletons.getControl().getInputQueue().setInputAndWait(inp);
            Card selected = inp.getSelected().get(0);
            final Map<CounterType, Integer> tgtCounters = selected.getCounters();
            final ArrayList<CounterType> typeChoices = new ArrayList<CounterType>();
            for (CounterType key : tgtCounters.keySet()) {
                if (tgtCounters.get(key) > 0) {
                    typeChoices.add(key);
                }
            }
            if (typeChoices.size() > 1) {
                String prompt = "Select type counters to remove";
                counterType = GuiChoose.one(prompt, typeChoices);
            } else {
                counterType = typeChoices.get(0);
            }
            executePayment(ability, selected);
            nNeed--;
        }
        source.setSVar("CostCountersRemoved", Integer.toString(c));
        return true;
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

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#payAI(forge.card.cost.PaymentDecision, forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public void payAI(PaymentDecision decision, Player ai, SpellAbility ability, Card source) {
        final String amount = this.getAmount();
        Integer c = this.convertAmount();
        if (c == null) {
            c = AbilityUtils.calculateAmount(source, amount, ability);
        }
        Card valid = decision.cards.get(0);
        for (CounterType c1 : valid.getCounters().keySet()) {
            if (valid.getCounters(c1) >= c && c1.isNegativeCounter()) {
                counterType = c1;
                break;
            }
        }
        for (int i = 0; i < c; i++) {
            executePayment(ability, valid);
        }
        source.setSVar("CostCountersRemoved", Integer.toString(c));
    }

    @Override
    protected void doPayment(SpellAbility ability, Card targetCard){
        targetCard.subtractCounter(this.getCounter(), 1);
    }



    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#decideAIPayment(forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public PaymentDecision decideAIPayment(Player ai, SpellAbility ability, Card source) {
        final String amount = this.getAmount();
        final int c = AbilityUtils.calculateAmount(source, amount, ability);
        final String type = this.getType();

        List<Card> typeList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split(";"), ai, source);
        List<Card> hperms = CardLists.filter(typeList, new Predicate<Card>() {
            @Override
            public boolean apply(final Card crd) {
                for (final CounterType c1 : CounterType.values()) {
                    if (crd.getCounters(c1) >= c  && c1.isNegativeCounter()) {
                        return true;
                    }
                }
                return false;
            }
        });
        // Only find cards with enough negative counters
        // TODO: add ai for Chisei, Heart of Oceans
        return hperms.isEmpty() ? null : new PaymentDecision(hperms);
    }
}
