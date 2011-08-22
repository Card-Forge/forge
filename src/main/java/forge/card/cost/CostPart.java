package forge.card.cost;

import forge.Card;
import forge.Player;
import forge.card.spellability.SpellAbility;

public abstract class CostPart {
	protected boolean isReusable = false;
	protected boolean isUndoable = false;
	protected boolean optional = false;
	protected String optionalType = null;    
	protected String amount = "1";
	protected String type = "Card";
	protected String typeDescription = null;

    public String getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public boolean getThis() {
        return type.equals("CARDNAME");
    }
    
    public String getTypeDescription() {
        return typeDescription;
    }

    public String getDescriptiveType(){
        return typeDescription == null ? type : typeDescription;
    }
    
	public boolean isReusable(){
		return isReusable;
	}
	
	public boolean isUndoable(){
		return isUndoable;
	}
	
	public String getOptionalType() {
		return optionalType;
	}

	public void setOptionalType(String optionalType) {
		this.optionalType = optionalType;
	}
	
	public Integer convertAmount(){
		Integer i = null;
        try{
        	i = Integer.parseInt(amount);
        }catch(NumberFormatException e){}
        return i;
	}
	
	public abstract boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost);
	
	public abstract boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment);
	public abstract void payAI(SpellAbility ability, Card source, Cost_Payment payment);
	public abstract boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment);
	
	public abstract String toString();
	public abstract void refund(Card source);
}
