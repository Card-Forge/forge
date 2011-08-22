package forge.card.cost;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.ComputerUtil;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;

public class CostReturn extends CostPartWithList {
	// Return<Num/Type{/TypeDescription}>
    
    public CostReturn(String amount, String type, String description){
    	this.amount = amount;
    	this.type = type;
    	this.typeDescription = description;
    }

	@Override
	public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Return ");

        Integer i = convertAmount();
        String pronoun = "its";
        
        if (getThis())
            sb.append(type);
        else {
        	String desc = typeDescription == null ? type : typeDescription;
	        if (i != null){
	        	sb.append(Cost.convertIntAndTypeToWords(i, desc));
	        	if (i > 1)
            		pronoun = "their"; 	
	        }
	        else
	        	sb.append(Cost.convertAmountTypeToWords(amount, desc));
        	
            sb.append(" you control");
        }
        sb.append(" to ").append(pronoun).append(" owner's hand");
        return sb.toString();
	}

	@Override
	public void refund(Card source) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost) {
        if (!getThis()) {
            CardList typeList = AllZoneUtil.getPlayerCardsInPlay(activator);
            typeList = typeList.getValidCards(getType().split(";"), activator, source);
            
            Integer amount = convertAmount();   
            if (amount != null && typeList.size() < amount)
                return false;
        } else if (!AllZoneUtil.isCardInPlay(source))
            return false;
        
        return true;
    }

    @Override
    public void payAI(SpellAbility ability, Card source, Cost_Payment payment) {
        for (Card c : list)
            AllZone.getGameAction().moveToHand(c);
    }

    @Override
    public boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment) {
        String amount = getAmount();
        Integer c = convertAmount();
        Player activator = ability.getActivatingPlayer();
        CardList list = AllZoneUtil.getPlayerCardsInPlay(activator);
        if (c == null) {
            String sVar = source.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, list.size());
            } else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }
        if (getThis())
            CostUtil.setInput(Cost_Input.returnThis(ability, payment, this));
        else
            CostUtil.setInput(Cost_Input.returnType(ability, getType(), payment, this, c));
        return false;
    }

    @Override
    public boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment) {
        resetList();
        if (getThis())
            list.add(source);
        else{
            Integer c = convertAmount();
            if (c == null){
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
            
            list = ComputerUtil.chooseReturnType(getType(), source, ability.getTargetCard(), c);
            if (list == null)
                return false;
        }
        return true;
    }
}
