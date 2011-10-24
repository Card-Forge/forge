package forge.card.cost;

import forge.Card;
import forge.Counters;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;

public class CostRemoveCounter extends CostPart {
	// SubCounter<Num/Counter/{Type/TypeDescription}>
    
    // Here are the cards that have RemoveCounter<Type>
    // Ion Storm, Noviken Sages, Ghave, Guru of Spores, Power Conduit (any Counter is tough), 
    // Quillspike, Rift Elemental, Sage of Fables, Spike Rogue

    private Counters counter;
    private int lastPaidAmount = 0;
    
    public Counters getCounter() {
        return counter;
    }

    public void setLastPaidAmount(int paidAmount){
    	lastPaidAmount = paidAmount;
    }
    
    public CostRemoveCounter(String amount, Counters counter, String type, String description){
        super(amount, type, description);
    	isReusable = true;

    	this.counter = counter;
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        if (counter.getName().equals("Loyalty"))
        	sb.append("-").append(amount);
        else {
        	sb.append("Remove ");
        	Integer i = convertAmount();
        	sb.append(Cost.convertAmountTypeToWords(i, amount, counter.getName()+" counter"));
        	
        	if (amount.equals("All")) {
        	    sb.append("s");
        	}

            sb.append(" from ").append(typeDescription == null ? type : typeDescription);
        }
        return sb.toString();
	}

	@Override
	public void refund(Card source) {
		source.addCounterFromNonEffect(counter, lastPaidAmount);
	}

    @Override
    public boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost) {
        Counters c = getCounter();
        
        Integer amount = convertAmount();
        if (amount != null && source.getCounters(c) - amount < 0) {
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
        source.subtractCounter(getCounter(), c);
    }

    @Override
    public boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment) {
        String amount = getAmount();
        Counters type = getCounter();
        Integer c = convertAmount();
        int maxCounters = source.getCounters(type);
        
        if (amount.equals("All")){
            c = maxCounters;
        }
        else{
            if (c == null){
                String sVar = source.getSVar(amount);
                // Generalize this
                if (sVar.equals("XChoice")){
                    c = CostUtil.chooseXValue(source, maxCounters);
                }
                else{
                    c = AbilityFactory.calculateAmount(source, amount, ability);
                }
            }
        }
       
        if (maxCounters >= c) {
            source.setSVar("CostCountersRemoved", "Number$"+Integer.toString(c));
            source.subtractCounter(type, c);
            setLastPaidAmount(c);
            payment.setPaidManaPart(this, true);
        } else {
            payment.setCancel(true);
            payment.getRequirements().finishPaying();
            return false;
        }
        return true;
    }

    @Override
    public boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment) {
        Integer c = convertAmount();
        if (c == null){
            String sVar = source.getSVar(amount);
            if (sVar.equals("XChoice")){
                return false;
            }
            
            c = AbilityFactory.calculateAmount(source, amount, ability);
        }
        if (c > source.getCounters(getCounter())) {
            System.out.println("Not enough " + getCounter() + " on " + source.getName());
            return false;
        }
        return true;
    }
}
