package forge.card.cost;

import forge.AllZone;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.Constant.Zone;
import forge.Counters;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.gui.input.Input;

/**
 * The Class CostPutCounter.
 */
public class CostPutCounter extends CostPartWithList {
    // Put Counter doesn't really have a "Valid" portion of the cost
    private final Counters counter;
    private int lastPaidAmount = 0;

    /**
     * Gets the counter.
     * 
     * @return the counter
     */
    public final Counters getCounter() {
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
    public CostPutCounter(final String amount, final Counters cntr, final String type, final String description) {
        this.setReusable(true);
        this.setAmount(amount);
        this.counter = cntr;

        this.setType(type);
        this.setTypeDescription(description);
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
            sb.append("+").append(this.getAmount());
        } else {
            sb.append("Put ");
            final Integer i = this.convertAmount();
            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), this.counter.getName() + " counter"));

            sb.append(" on ");
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
    public final boolean canPay(final SpellAbility ability, final Card source, final Player activator, final Cost cost) {
        if (this.getThis()) {
            if (source.hasKeyword("CARDNAME can't have counters placed on it.")) {
                return false;
            }
            if (source.hasKeyword("CARDNAME can't have -1/-1 counters placed on it.")
                    && this.counter.equals(Counters.M1M1)) {
                return false;
            }
        } else {
            // 3 Cards have Put a -1/-1 Counter on a Creature you control.
            final CardList typeList = activator.getCardsIn(Zone.Battlefield).getValidCards(this.getType().split(";"),
                    activator, source);

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
    public final void payAI(final SpellAbility ability, final Card source, final CostPayment payment) {
        Integer c = this.convertAmount();
        if (c == null) {
            c = AbilityFactory.calculateAmount(source, this.getAmount(), ability);
        }

        if (this.getThis()) {
            source.addCounterFromNonEffect(this.getCounter(), c);
        } else {
            // Put counter on chosen card
            for (final Card card : this.getList()) {
                card.addCounterFromNonEffect(this.getCounter(), 1);
            }
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
    public final boolean payHuman(final SpellAbility ability, final Card source, final CostPayment payment) {
        Integer c = this.convertAmount();
        if (c == null) {
            c = AbilityFactory.calculateAmount(source, this.getAmount(), ability);
        }

        if (this.getThis()) {
            source.addCounterFromNonEffect(this.getCounter(), c);
            payment.setPaidManaPart(this, true);
            this.addToList(source);
            return true;
        } else {
            CostUtil.setInput(CostPutCounter.putCounterType(ability, this.getType(), payment, this, c));
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#decideAIPayment(forge.card.spellability.SpellAbility
     * , forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean decideAIPayment(final SpellAbility ability, final Card source, final CostPayment payment) {
        this.resetList();
        if (this.getThis()) {
            this.addToList(source);
            return true;
        } else {
            final Player activator = ability.getActivatingPlayer();
            Integer c = this.convertAmount();
            if (c == null) {
                c = AbilityFactory.calculateAmount(source, this.getAmount(), ability);
            }

            final CardList typeList = activator.getCardsIn(Zone.Battlefield).getValidCards(this.getType().split(";"),
                    activator, source);

            Card card = null;
            if (this.getType().equals("Creature.YouCtrl")) {
                card = CardFactoryUtil.getWorstCreatureAI(typeList);
            } else {
                card = CardFactoryUtil.getWorstPermanentAI(typeList, false, false, false, false);
            }
            this.addToList(card);
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
     * @param costPutCounter
     *            TODO
     * @param nNeeded
     *            the n needed
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input putCounterType(final SpellAbility sa, final String type, final CostPayment payment,
            final CostPutCounter costPutCounter, final int nNeeded) {
        final Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            private CardList typeList;
            private int nPut = 0;

            @Override
            public void showMessage() {
                if ((nNeeded == 0) || (nNeeded == this.nPut)) {
                    this.done();
                }

                final StringBuilder msg = new StringBuilder("Put ");
                final int nLeft = nNeeded - this.nPut;
                msg.append(nLeft).append(" ");
                msg.append(costPutCounter.getCounter()).append(" on ");

                msg.append(costPutCounter.getDescriptiveType());
                if (nLeft > 1) {
                    msg.append("s");
                }

                this.typeList = sa.getActivatingPlayer().getCardsIn(Zone.Battlefield);
                this.typeList = this.typeList.getValidCards(type.split(";"), sa.getActivatingPlayer(),
                        sa.getSourceCard());
                AllZone.getDisplay().showMessage(msg.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.cancel();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (this.typeList.contains(card)) {
                    this.nPut++;
                    costPutCounter.addToList(card);
                    card.addCounterFromNonEffect(costPutCounter.getCounter(), 1);

                    if (nNeeded == this.nPut) {
                        this.done();
                    } else {
                        this.showMessage();
                    }
                }
            }

            public void done() {
                this.stop();
                payment.paidCost(costPutCounter);
            }

            public void cancel() {
                this.stop();
                costPutCounter.addListToHash(sa, "CounterPut");
                payment.cancelCost();
            }
        };

        return target;
    }
}
