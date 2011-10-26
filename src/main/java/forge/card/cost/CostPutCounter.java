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
    private Counters counter;
    private int lastPaidAmount = 0;

    /**
     * Gets the counter.
     * 
     * @return the counter
     */
    public final Counters getCounter() {
        return counter;
    }

    /**
     * Sets the last paid amount.
     * 
     * @param paidAmount
     *            the new last paid amount
     */
    public final void setLastPaidAmount(final int paidAmount) {
        lastPaidAmount = paidAmount;
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
        isReusable = true;
        this.amount = amount;
        this.counter = cntr;

        this.type = type;
        this.typeDescription = description;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        if (counter.getName().equals("Loyalty")) {
            sb.append("+").append(amount);
        } else {
            sb.append("Put ");
            Integer i = convertAmount();
            sb.append(Cost.convertAmountTypeToWords(i, amount, counter.getName() + " counter"));

            sb.append(" on ");
            if (getThis()) {
                sb.append(type);
            } else {
                String desc = typeDescription == null ? type : typeDescription;
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
        for (Card c : list) {
            c.subtractCounter(counter, lastPaidAmount);
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
        if (getThis()) {
            if (source.hasKeyword("CARDNAME can't have counters placed on it.")) {
                return false;
            }
            if (source.hasKeyword("CARDNAME can't have -1/-1 counters placed on it.") && counter.equals(Counters.M1M1)) {
                return false;
            }
        } else {
            // 3 Cards have Put a -1/-1 Counter on a Creature you control.
            CardList typeList = activator.getCardsIn(Zone.Battlefield).getValidCards(getType().split(";"), activator,
                    source);

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
    public final void payAI(final SpellAbility ability, final Card source, final Cost_Payment payment) {
        Integer c = convertAmount();
        if (c == null) {
            c = AbilityFactory.calculateAmount(source, amount, ability);
        }

        if (getThis()) {
            source.addCounterFromNonEffect(getCounter(), c);
        } else {
            // Put counter on chosen card
            for (Card card : list) {
                card.addCounterFromNonEffect(getCounter(), 1);
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
    public final boolean payHuman(final SpellAbility ability, final Card source, final Cost_Payment payment) {
        Integer c = convertAmount();
        if (c == null) {
            c = AbilityFactory.calculateAmount(source, amount, ability);
        }

        if (getThis()) {
            source.addCounterFromNonEffect(getCounter(), c);
            payment.setPaidManaPart(this, true);
            addToList(source);
            return true;
        } else {
            CostUtil.setInput(putCounterType(ability, getType(), payment, this, c));
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
    public final boolean decideAIPayment(final SpellAbility ability, final Card source, final Cost_Payment payment) {
        resetList();
        if (getThis()) {
            addToList(source);
            return true;
        } else {
            Player activator = ability.getActivatingPlayer();
            Integer c = convertAmount();
            if (c == null) {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }

            CardList typeList = activator.getCardsIn(Zone.Battlefield).getValidCards(getType().split(";"), activator,
                    source);

            Card card = null;
            if (type.equals("Creature.YouCtrl")) {
                card = CardFactoryUtil.AI_getWorstCreature(typeList);
            } else {
                card = CardFactoryUtil.AI_getWorstPermanent(typeList, false, false, false, false);
            }
            addToList(card);
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
     *            a {@link forge.card.cost.Cost_Payment} object.
     * @param costPutCounter
     *            TODO
     * @param nNeeded
     *            the n needed
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input putCounterType(final SpellAbility sa, final String type, final Cost_Payment payment,
            final CostPutCounter costPutCounter, final int nNeeded) {
        Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            private CardList typeList;
            private int nPut = 0;

            @Override
            public void showMessage() {
                if (nNeeded == 0 || nNeeded == nPut) {
                    done();
                }

                StringBuilder msg = new StringBuilder("Put ");
                int nLeft = nNeeded - nPut;
                msg.append(nLeft).append(" ");
                msg.append(costPutCounter.getCounter()).append(" on ");

                msg.append(costPutCounter.getDescriptiveType());
                if (nLeft > 1) {
                    msg.append("s");
                }

                typeList = sa.getActivatingPlayer().getCardsIn(Zone.Battlefield);
                typeList = typeList.getValidCards(type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());
                AllZone.getDisplay().showMessage(msg.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                cancel();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (typeList.contains(card)) {
                    nPut++;
                    costPutCounter.addToList(card);
                    card.addCounterFromNonEffect(costPutCounter.getCounter(), 1);

                    if (nNeeded == nPut) {
                        done();
                    } else {
                        showMessage();
                    }
                }
            }

            public void done() {
                stop();
                payment.paidCost(costPutCounter);
            }

            public void cancel() {
                stop();
                costPutCounter.addListToHash(sa, "CounterPut");
                payment.cancelCost();
            }
        };

        return target;
    }
}
