package forge.card.cost;

import forge.AllZone;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.Counters;
import forge.Player;
import forge.PlayerZone;
import forge.Constant.Zone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.gui.input.Input;

public class CostPutCounter extends CostPartWithList {
	// Put Counter doesn't really have a "Valid" portion of the cost
    private Counters counter;
    private int lastPaidAmount = 0;

    public Counters getCounter() {
        return counter;
    }

    public void setLastPaidAmount(int paidAmount) {
    	lastPaidAmount = paidAmount;
    }

    public CostPutCounter(String amount, Counters cntr, String type, String description) {
    	isReusable = true;
    	this.amount = amount;
    	this.counter = cntr;

        this.type = type;
        this.typeDescription = description;
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        if (counter.getName().equals("Loyalty")) {
        	sb.append("+").append(amount);
        }
        else {
        	sb.append("Put ");
        	Integer i = convertAmount();
        	sb.append(Cost.convertAmountTypeToWords(i, amount, counter.getName()));

        	sb.append(" on ");
            if (getThis()){
                sb.append(type);
            }
            else {
                String desc = typeDescription == null ? type : typeDescription;
                sb.append(desc);
            }
        }
        return sb.toString();
	}

	@Override
	public void refund(Card source) {
	    for(Card c : list) {
	        c.subtractCounter(counter, lastPaidAmount);
	    }
	}

    @Override
    public boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost) {
        if (getThis()) {
            if (source.hasKeyword("CARDNAME can't have counters placed on it.")) {
                return false;
            }
            if (source.hasKeyword("CARDNAME can't have -1/-1 counters placed on it.")
                    && counter.equals(Counters.M1M1)) {
                return false;
            }
        }
        else {
            // 3 Cards have Put a -1/-1 Counter on a Creature you control.
            CardList typeList = activator.getCardsIn(Zone.Battlefield)
                    .getValidCards(getType().split(";"), activator, source);

            if (typeList.size() == 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void payAI(SpellAbility ability, Card source, Cost_Payment payment) {
        Integer c = convertAmount();
        if (c == null) {
            c = AbilityFactory.calculateAmount(source, amount, ability);
        }

        if (getThis()) {
            source.addCounterFromNonEffect(getCounter(), c);
        }
        else {
            // Put counter on chosen card
            for(Card card : list) {
                card.addCounterFromNonEffect(getCounter(), 1);
            }
        }
    }

    @Override
    public boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment) {
        Integer c = convertAmount();
        if (c == null) {
            c = AbilityFactory.calculateAmount(source, amount, ability);
        }

        if (getThis()) {
            source.addCounterFromNonEffect(getCounter(), c);
            payment.setPaidManaPart(this, true);
            addToList(source);
            return true;
        }
        else {
            CostUtil.setInput(putCounterType(ability, getType(), payment, this, c));
            return false;
        }
    }

    @Override
    public boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment) {
        resetList();
        if (getThis()) {
            addToList(source);
            return true;
        }
        else {
            Player activator = ability.getActivatingPlayer();
            Integer c = convertAmount();
            if (c == null) {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }

            CardList typeList = activator.getCardsIn(Zone.Battlefield)
                    .getValidCards(getType().split(";"), activator, source);

            Card card = null;
            if (type.equals("Creature.YouCtrl")) {
                card = CardFactoryUtil.AI_getWorstCreature(typeList);
            }
            else {
                card = CardFactoryUtil.AI_getWorstPermanent(typeList, false, false, false, false);
            }
            addToList(card);
        }
        return true;
    }

    /**
     * <p>returnType.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param type a {@link java.lang.String} object.
     * @param payment a {@link forge.card.cost.Cost_Payment} object.
     * @param costPutCounter TODO
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input putCounterType(final SpellAbility sa, final String type, final Cost_Payment payment, final CostPutCounter costPutCounter, final int nNeeded) {
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
                    }
                    else{
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
