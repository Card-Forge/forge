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
import forge.Counters;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/**
 * The Class CostRemoveCounter.
 */
public class CostRemoveCounter extends CostPartWithList {
    // SubCounter<Num/Counter/{Type/TypeDescription/Zone}>

    // Here are the cards that have RemoveCounter<Type>
    // Ion Storm, Noviken Sages, Ghave, Guru of Spores, Power Conduit (any
    // Counter is tough),
    // Quillspike, Rift Elemental, Sage of Fables, Spike Rogue

    private final Counters counter;
    private int lastPaidAmount = 0;
    private ZoneType zone;

    /**
     * Gets the counter.
     * 
     * @return the counter
     */
    public final Counters getCounter() {
        return this.counter;
    }

    /**
     * @return the zone
     */
    public ZoneType getZone() {
        return zone;
    }

    /**
     * @param zone the zone to set
     */
    public void setZone(ZoneType zone) {
        this.zone = zone;
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
    public CostRemoveCounter(final String amount, final Counters counter, final String type, final String description, ZoneType zone) {
        super(amount, type, description);
        this.setReusable(true);

        this.counter = counter;
        this.setType(type);
        this.setTypeDescription(description);
        this.setZone(zone);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        if (this.counter.getName().equals("Loyalty")) {
            sb.append("-").append(this.getAmount());
        } else {
            sb.append("Remove ");
            final Integer i = this.convertAmount();
            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), this.counter.getName() + " counter"));

            if (this.getAmount().equals("All")) {
                sb.append("s");
            }

            sb.append(" from ");

            if (this.getThis()) {
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
            c.addCounterFromNonEffect(this.counter, this.lastPaidAmount);
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
    public final boolean canPay(final SpellAbility ability, final Card source, final Player activator, final Cost cost) {
        final Counters cntrs = this.getCounter();

        final Integer amount = this.convertAmount();
        if (this.getThis()) {
            if ((amount != null) && ((source.getCounters(cntrs) - amount) < 0)) {
                return false;
            }
        }
        else {
            final List<Card> typeList = CardLists.getValidCards(activator.getCardsIn(this.getZone()), this.getType().split(";"), activator, source);
            if (amount != null) {
                for (Card c : typeList) {
                    if (c.getCounters(cntrs) - amount >= 0) {
                        return true;
                    }
                }
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
    public final void payAI(final Player ai, final SpellAbility ability, final Card source, final CostPayment payment) {
        final String amount = this.getAmount();
        Integer c = this.convertAmount();
        if (c == null) {
            if (amount.equals("All")) {
                c = source.getCounters(this.counter);
            } else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }

        if (this.getThis()) {
            source.subtractCounter(this.counter, c);
        } else {
            for (final Card card : this.getList()) {
                card.subtractCounter(this.counter, c);
            }
        }
        source.setSVar("CostCountersRemoved", "Number$" + Integer.toString(c));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payHuman(final SpellAbility ability, final Card source, final CostPayment payment) {
        final String amount = this.getAmount();
        Integer c = this.convertAmount();
        int maxCounters = 0;

        if (!this.getThis()) {
            if (this.getZone().equals(ZoneType.Battlefield)) {
                final Input inp = CostRemoveCounter.removeCounterType(ability, this.getType(), payment, this, c);
                Singletons.getModel().getMatch().getInput().setInputInterrupt(inp);
            }
            else {
                final Input inp = CostRemoveCounter.removeCounterTypeFrom(ability, this.getType(), payment, this, c);
                Singletons.getModel().getMatch().getInput().setInputInterrupt(inp);
            }
            return false;
        }

        maxCounters = source.getCounters(this.counter);
        if (amount.equals("All")) {
            c = maxCounters;
        } else {
            if (c == null) {
                final String sVar = ability.getSVar(amount);
                // Generalize this
                if (sVar.equals("XChoice")) {
                    c = CostUtil.chooseXValue(source, ability, maxCounters);
                } else {
                    c = AbilityFactory.calculateAmount(source, amount, ability);
                }
            }
        }

        if (maxCounters >= c) {
            this.addToList(source);
            source.setSVar("CostCountersRemoved", "Number$" + Integer.toString(c));
            source.subtractCounter(this.counter, c);
            this.setLastPaidAmount(c);
            payment.setPaidManaPart(this);
        } else {
            payment.setCancel(true);
            payment.getRequirements().finishPaying();
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#decideAIPayment(forge.card.spellability.SpellAbility
     * , forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean decideAIPayment(final Player ai, final SpellAbility ability, final Card source, final CostPayment payment) {
        final String amount = this.getAmount();
        Integer c = this.convertAmount();


        if (c == null) {
            final String sVar = ability.getSVar(amount);
            if (sVar.equals("XChoice")) {
                return false;
            }
            if (amount.equals("All")) {
                c = source.getCounters(this.counter);
            } else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }

        if (!this.getThis()) {
            this.getList().clear();
            final List<Card> typeList = 
                    CardLists.getValidCards(ai.getCardsIn(this.getZone()), this.getType().split(";"), ai, source);
            for (Card card : typeList) {
                if (card.getCounters(this.getCounter()) >= c) {
                    this.addToList(card);
                    return true;
                }
            }
            return false;
        }
        if (c > source.getCounters(this.getCounter())) {
            System.out.println("Not enough " + this.counter + " on " + source.getName());
            return false;
        }
        return true;
    }

    /**
     * <p>
     * returnType.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @param payment
     *            a {@link forge.card.cost.CostPayment} object.
     * @param costRemoveCounter
     *            TODO
     * @param nNeeded
     *            the n needed
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input removeCounterType(final SpellAbility sa, final String type, final CostPayment payment,
            final CostRemoveCounter costRemoveCounter, final int nNeeded) {
        final Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            private List<Card> typeList;
            private int nRemove = 0;

            @Override
            public void showMessage() {
                if ((nNeeded == 0) || (nNeeded == this.nRemove)) {
                    this.done();
                }

                final StringBuilder msg = new StringBuilder("Remove ");
                final int nLeft = nNeeded - this.nRemove;
                msg.append(nLeft).append(" ");
                msg.append(costRemoveCounter.getCounter()).append(" from ");

                msg.append(costRemoveCounter.getDescriptiveType());
                if (nLeft > 1) {
                    msg.append("s");
                }

                this.typeList = CardLists.getValidCards(sa.getActivatingPlayer().getCardsIn(costRemoveCounter.getZone()), type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());
                CMatchUI.SINGLETON_INSTANCE.showMessage(msg.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.cancel();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (this.typeList.contains(card)) {
                    if (card.getCounters(costRemoveCounter.getCounter()) > 0) {
                        this.nRemove++;
                        costRemoveCounter.addToList(card);
                        card.subtractCounter(costRemoveCounter.getCounter(), 1);

                        if (nNeeded == this.nRemove) {
                            this.done();
                        } else {
                            this.showMessage();
                        }
                    }
                }
            }

            public void done() {
                this.stop();
                costRemoveCounter.addListToHash(sa, "CounterPut");
                payment.paidCost(costRemoveCounter);
            }

            public void cancel() {
                this.stop();
                costRemoveCounter.addListToHash(sa, "CounterPut");
                payment.cancelCost();
            }
        };

        return target;
    }
    
    /**
     * <p>
     * returnType.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @param payment
     *            a {@link forge.card.cost.CostPayment} object.
     * @param costRemoveCounter
     *            TODO
     * @param nNeeded
     *            the n needed
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input removeCounterTypeFrom(final SpellAbility sa, final String type, final CostPayment payment,
            final CostRemoveCounter costRemoveCounter, final int nNeeded) {
        final Input target = new Input() {
            private static final long serialVersionUID = 734256837615635021L;
            private List<Card> typeList;
            private int nRemove = 0;

            @Override
            public void showMessage() {
                if (nNeeded == 0) {
                    this.done();
                }

                this.typeList = sa.getActivatingPlayer().getCardsIn(costRemoveCounter.getZone());
                this.typeList = CardLists.getValidCards(this.typeList, type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());

                for (int i = 0; i < nNeeded; i++) {
                    if (this.typeList.size() == 0) {
                        this.cancel();
                    }

                    final Card o = GuiChoose
                            .oneOrNone("Remove counter(s) from a card in " + costRemoveCounter.getZone(), this.typeList);

                    if (o != null) {
                        final Card card = o;
                    
                        if (card.getCounters(costRemoveCounter.getCounter()) > 0) {
                            this.nRemove++;
                            costRemoveCounter.addToList(card);
                            card.subtractCounter(costRemoveCounter.getCounter(), 1);
    
                            if (card.getCounters(costRemoveCounter.getCounter()) == 0) {
                                this.typeList.remove(card);
                            }
                            
                            if (nNeeded == this.nRemove) {
                                this.done();
                            } else {
                                this.showMessage();
                            }
                        }
                    } else {
                        this.cancel();
                        break;
                    }
                }
            }

            @Override
            public void selectButtonCancel() {
                this.cancel();
            }

            public void done() {
                this.stop();
                costRemoveCounter.addListToHash(sa, "CounterPut");
                payment.paidCost(costRemoveCounter);
            }

            public void cancel() {
                this.stop();
                costRemoveCounter.addListToHash(sa, "CounterPut");
                payment.cancelCost();
            }
        };
        return target;
    }
}