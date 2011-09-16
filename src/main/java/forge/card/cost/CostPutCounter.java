package forge.card.cost;

import forge.Card;
import forge.Counters;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;

public class CostPutCounter extends CostPart {
	// Put Counter doesn't really have a "Valid" portion of the cost 
    private Counters counter;
    private int lastPaidAmount = 0;

    public Counters getCounter() {
        return counter;
    }
    
    public void setLastPaidAmount(int paidAmount){
    	lastPaidAmount = paidAmount;
    }
    
    public CostPutCounter(String amount, Counters counter){
        this.type = "CARDNAME";
    	isReusable = true;
    	this.amount = amount;
    	this.counter = counter;
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        if (counter.getName().equals("Loyalty"))
        	sb.append("+").append(amount);
        else {
        	sb.append("Put ");
        	Integer i = convertAmount();
        	sb.append(Cost.convertAmountTypeToWords(i, amount, counter.getName()));

            sb.append(" on ").append(type);
        }
        return sb.toString();
	}

	@Override
	public void refund(Card source) {
		source.subtractCounter(counter, lastPaidAmount);
	}

    @Override
    public boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost) {

        if(source.hasKeyword("CARDNAME can't have counters placed on it.")) {
            return false;
        }
        if (source.hasKeyword("CARDNAME can't have -1/-1 counters placed on it.") && counter.equals(Counters.M1M1)) {
            return false;
        }
        
        return true;
    }

    @Override
    public void payAI(SpellAbility ability, Card source, Cost_Payment payment) {
        Integer c = convertAmount();
        if (c == null){
            c = AbilityFactory.calculateAmount(source, amount, ability);
        }
        source.addCounterFromNonEffect(getCounter(), c);
    }

    @Override
    public boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment) {
        Integer c = convertAmount();
        if (c == null){
            c = AbilityFactory.calculateAmount(source, amount, ability);
        }
        
        source.addCounterFromNonEffect(getCounter(), c);
        payment.setPaidManaPart(this, true);
        return true;
    }

    @Override
    public boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment) {
        return true;
    }
}
